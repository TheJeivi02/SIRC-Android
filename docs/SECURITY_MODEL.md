# SIRC — Modelo de Seguridad (v2)

> Documento formal de seguridad, suscripción y modelo de confianza de SIRC.
> Resultado del **Roadmap Gate** (LOOP de consistencia: estrategia competitiva +
> informe ejecutivo + arquitectura + roadmap + modelo comercial de suscripción +
> seguridad) y **ampliado por el LOOP Backend Supabase** (16-ago-2026: T15–T20,
> API de Play v2 - `subscriptionsv2`, backend basado en Supabase) y **por el
> LOOP Modelo Free** (16-ago-2026: entitlement `FREE`/`PREMIUM`, seguridad del
> Free, regla D15.3).
> **No implementa nada**: define la arquitectura objetivo y las
> mitigaciones para el futuro.
>
> - Estado del repositorio al momento de escribir: **v1.0.0-rc1 (Sprint 11)**,
>   sin backend, sin suscripción, sin Play Integrity.
> - Fuente de datos de mercado: `docs/PRODUCT_COMPETITIVE_ANALYSIS.md` y
>   `docs/PRODUCT_STRATEGY.md`.
> - Principio fundamental: **el APK debe considerarse potencialmente manipulable**.
>   Nada de lo que existe localmente puede, por sí solo, probar un entitlement.

## 1. Principio de producto: LOCAL-FIRST (no "100 % local")

Tras el análisis, **resolvemos el dilema "100 % local" vs "LOCAL-FIRST"** en
favor de **LOCAL-FIRST** como modelo comercial-seguro, sin renunciar al pilar de
privacidad de los datos de las ofertas.

> **Procesamiento local (se mantiene 100 % en el dispositivo):**
> OCR, parsing, evaluación, cálculo de rentabilidad, recomendación, overlay,
> historial de ofertas y estadísticas. **Ninguna oferta capturada sale del
> dispositivo.** Esto es coherente con la FUENTE 1 (100 % local en datos de
> pantalla) y con Google Play (accesibilidad para parsing, no para telemetría).

> **Servicios remotos mínimos (nuevos, acotados al modelo de negocio):**
> autenticación/cuenta, suscripción, **entitlement**, verificación de compra,
> Play Integrity, RTDN y recuperación de cuenta. Solo datos de cuenta/suscripción;
> **jamás** contenido de pantalla ni ofertas.

Justificación:

- Implementar una app de pago por suscripción **sin** verificación de entitlement
  en servidor es inviable comercialmente (C el APK es manipulable).
- La arquitectura actual ya es multi-módulo con `:domain` Kotlin puro: añadir un
  cliente remoto acotado **no** contamina el pipeline de captura.

**Riesgo de confusión**: "local" pasa a significar "la privacidad de pantalla
permanece local", NO "la app funciona sin red". Lo documentamos explícitamente.

## 2. Modelo de confianza (trust model)

```
                    ┌──────────────────────────────┐
                    │       GOOGLE PLAY / GCP       │
                    │  (fuente de verdad comercial) │
                    └───────┬───────────────▲──────┘
                            │ 1. tokens/billing      │ 4. RTDN / API verifican
                            ▼                       │
┌──────────────┐    ┌───────────────┐    ┌───────────────┐
│   SIRC App   │───▶│   BACKEND     │───▶│   ENTITLEMENT │
│ (no confiable│2.  │ (no confiable │3.  │    (confiable)│
│  por sí sola)│  purchase token + │    │  verifica con │
│              │◀── integrity token│◀───│  Play APIs    │
└──────────────┘5.  └───────────────┘    └───────────────┘
   |
   └──► Se puede CONFIAR al cliente (nunca autorización):
        OCR, parsing, cálculos, UI, caché, UX offline (mejor esfuerzo).
```

### Qué puede confiarse al cliente

| Dominio | Confiable | Por qué |
|---|---|---|
| Procesamiento OCR/parsing | ✅ | No autoriza nada; degradación visible. |
| Cálculos de rentabilidad | ✅ | Local, derivado, sin red. |
| UI + overlay | ✅ | Solo presenta. |
| Caché de historial/dashboard | ✅ | Datos del conductor. |
| Experiencia offline del pipeline | ✅ | El pipeline no gana/revoca productividad alguna. |

### Qué NO debe decidir el cliente (decisión crítica de autorización)

| Dominio | NO confiable | Mecanismo requerido |
|---|---|---|
| Suscripción activa | ❌ | Verificación servidor (token → Play). |
| Entitlement/estado premium | ❌ | Backend + RTDN + TTL. |
| Autorización de features premium | ❌ | Gate server-verificado (o cached por TTL con degradación). |
| Identidad de cuenta | ❌ | Autenticación gestionada (Play/Google + sesión backend). |
| Revocación | ❌ | RTDN + reintento del cliente al volver online. |
| Estado comercial (restore, grace, hold) | ❌ | Play API, no lecturas locales. |

**Consecuencia de arquitectura**: `EntitlementRepository` (contrato en `:domain`)
tiene dos implementaciones: `ServerEntitlementRepository` (fuente de verdad) y
`CachedEntitlementRepository` (caché por TTL **corto**). La **decisión de UI
premium** se toma sobre el estado verificado SIEMPRE que haya red; el caché solo
mitiga offline breve.

## 3. Threat Model (T1–T20) para SIRC

Metodología: cada amenaza con impacto (I) y probabilidad (P) en Alto/Medio/Bajo,
capa responsable y limitaciones. **No prometemos protección absoluta.**

| # | Amenaza | I | P | Mitigación | Capa | Limitaciones |
|---|---|---|---|---|---|---|
| T1 | APK repackaged/firmado de nuevo | Alto | Medio | Play Integrity `appIntegrity` (`PLAY_RECOGNIZED` + `certificateSha256Digest`) verificado en backend con `requestHash`/`nonce`. | Cliente→Backend | Detecta en servidor al operar; no evita instalación sin red. |
| T2 | Eliminación de comprobaciones premium | Alto | Medio | Entitlement verificado en servidor; la app gateway sobre estado server, no sobre strings locales; R8/ProGuard. | Backend | Rooting completo con instrumentación puede evadir la UI local. |
| T3 | Falsificación de entitlement local | Alto | Medio | Never trust client: backend + `purchases.subscriptionsv2.get` + TTL + firmas server HMAC corto. | Backend | Un APK totalmente reescrito puede omitir el gate (ver T2). |
| T4 | Manipulación de Room/DataStore/prefs | Medio | Alto | Separar datos no sensibles (Room ofertas) de datos sensibles (sesión/entitlement cache) que van cifrados con key del Keystore; firmtas del caché de entitlement. | Cliente | Un dispositivo rooteado puede leer datos en claro del pipeline (no es autorización). |
| T5 | Replay de credenciales/tokens | Alto | Medio | Tokens de sesión cortos, rotación, `nonce`/`requestHash` en Play Integrity, `messageId` único en RTDN. | Backend | El robo del session token permite uso hasta su expiración. |
| T6 | Clonación de instalación (copia de datos) | Medio | Medio | Vincular sesión a instalación (install ID cifrado), invalidar en el servidor al detectar duplicado. | Backend | No contraindica copias offline de datos de historial. |
| T7 | Root / entorno comprometido | Medio | Alto | Play Integrity `deviceIntegrity` (STRENGHT opcional, tiered enforcement), tolerancia degradada, no penalizar demasiado (root ≠ fraude). | Backend | Root con Xposed puede falsear señales; enforcement demasiado duro dañaría UX. |
| T8 | Debug builds distribuidas ilegalmente | Medio | Medio | Release-signed builds, Play Integrity `appRecognition` (rechaza versiones UNRECOGNIZED), control de `versionCode`. | Cliente+Backend | No evita que una release legítima sea manipulada (apunta a T1). |
| T9 | Hooking/instrumentación (Frida/Xposed) | Alto | Medio | Play Integrity `appAccessRisk` (opcional), tiered enforcement; **no depositar la seguridad en anti-tamper local**. | Backend | La instrumentación en runtime puede falsear localmente; la señal remota lo detecta con retraso. |
| T10 | Interceptación/modificación de tráfico | Medio | Medio | TLS 1.2+ con certificate pinning del backend; validar `requestHash`/`nonce`; no enviar contenido de pantalla. | Cliente+Backend | MitM avanzado con root puede bypasear; los datos viajan son comerciales, no pantalla. |
| T11 | Cuenta válida en app modificada | Alto | Alto | Entitlement **siempre** verificado en servidor + Play Integrity en acciones sensibles; la app modificada queda bloqueada al validar. | Backend | Existe ventana entre revisión del token y verificación (por diseño con TTL). |
| T12 | Compartir APK premium modificado multiusuario | Alto | Medio | Cada instalación tiene su sesión; el entitlement se valida **por cuenta de Google Play**, no por binario: una copia no da cuenta pagada. | Backend | Si el atacante usa CUENTAS pagadas legítimas, es abuso de cuentas (mitigado por límites/rate limiting). |
| T13 | Abuso de endpoints de backend | Alto | Medio | Rate limiting por IP/cuenta, autenticación obligatoria, auditoría mínima, `nonce` por request. | Backend | El rate limiting balancea UX vs abuso. |
| T14 | Uso prolongado offline tras expirar | Alto | Alto | Entitlement cacheado con **TTL corto** (documentado en §7) + challenge/response + revocación por RTDN en cuanto vuelve online. | Backend+Cliente | Mientras dure el TTL, se tolera un consumo limitado (tradeoff offline vs fraude). |
| T15 | APK premium modificado (repackaged con entitlement forzado) | Alto | Medio | Entitlement verificado en servidor (`subscriptionsv2.get`); Play Integrity `appIntegrity` como señal (`PLAY_RECOGNIZED` + `certificateSha256Digest`); R8/ProGuard; gate premium sobre estado server, no strings locales. | Backend+Cliente | Un APK reescrito puede esquivar la UI local, pero no obtiene entitlement válido SIN verificación server; queda bloqueado al operar online. |
| T16 | Eliminación de la pantalla/login de suscripción | Alto | Medio | El pago real crea el token en Play; el entitlement se otorga solo tras verificar; sin pantalla de prueba local hay que pagar en Play por el flujo oficial. Un "código" de activación local no existe (diseño). | Backend | Frameworks de purchase entirely local (sin verificar) no afectan al entitlement; solo la UI podría parecer activa pero el gate server la bloquea. |
| T17 | Modificación del entitlement local (cache) | Alto | Alto | Caché **cifrado + firmado por el servidor** (HMAC) con TTL 24–72 h; cualquier manipulación rompe la firma → sin premium; comparar contra `serverUtc`; al conectar se revalida. | Cliente+Backend | Un atacante con raíz puede re-firmar SI poseyera la clave HMAC (que solo guarda el servidor); por diseño no la tiene. |
| T18 | Compartir APK premium modificado entre múltiples usuarios | Alto | Medio | Entitlement ligado a la **cuenta de pago** y al `purchase_token` (único): copiar el APK no copia la cuenta pagada. Desde el servidor: rate limiting por cuenta, detección de patterns por dispositivo. | Backend | Si el atacante usa cuentas pagadas legítimas múltiples, es abuso de cuentas (mitigable por rate limiting/auditoría). |
| T19 | Uso de cuenta válida con cliente manipulado (el cliente miente su estado) | Alto | Alto | El gate premium consulta `EntitlementRepository` con estado **server-verificado**; el cliente no decide. Sesión vinculada a install (`sessions`/`devices`); verificación on-demand en acciones sensibles. | Backend | Ventana de TTL/offline inherente al modelo (tradeoff documentado). |
| T20 | Manipulación del reloj para prolongar acceso offline | Alto | Alto | Caché firma el `expiry` (clave server); la validación usa `serverUtc` (recuperado al verificar), nunca el reloj local como autoridad; TTL absoluto acotado. | Backend+Cliente | Con TTL ≤72 h, manipular el reloj solo gana hasta el TTL; no se puede extender indefinidamente sin validación server. |

**Conclusión del threat model (T1–T20)**: la mayor superficie de fraude comercial
se mitiga **exclusivamente** en el servidor (entitlement, Play APIs, rate
limiting). El cliente contribuye señales (Integrity, tokens) pero **jamás es
autoridad**. Esto define el rol de Play Integrity §4: **no es por sí solo la
barrera de suscripción**, es una señal adicional. No prometemos protección
absoluta: el objetivo es elevar el coste de la manipulación y evitar **premium
indefinido sin pago** (T14/T15/T17/T20).

## 4. Play Integrity — análisis y rol (investigación oficial, 16-ago-2026)

Fuentes: `developer.android.com/google/play/integrity/*` (overview, classic,
standard, verdicts). Saltos entre recomendación oficial y decisión propia.

### 4.1 Modos de request

| Id | Modo | Latencia | Frecuencia recomendada | Protección anti-replay |
|---|---|---|---|---|
| S | Standard | cientos de ms (promedio) | Frecuente / on-demand | **Automática** (Google Play) + `requestHash` opcional (content binding) |
| C | Classic | varios segundos | Infrecuente / one-off (acciones de alto valor) | Manual vía `nonce` (server-side) |

**Recomendación oficial**: Standard para la mayoría; Classic solo para acciones
de alto valor/raras; no cachear veredictos (riesgo de proxying); `requestHash`
para binding de contenido y `nonce` único del servidor para Classic.

### 4.2 Veredictos disponibles (payload JSON)

- `requestDetails`: valida `requestHash`/`nonce` y ventana de tiempo.
- `accountDetails.appLicensingVerdict`: `LICENSED`/`UNLICENSED`/`UNEVALUATED`.
  **NO hay campo de "subscription" aquí**: la licencia es de la app, no del
  producto de suscripción.
- `appIntegrity.appRecognitionVerdict`: `PLAY_RECOGNIZED`/`UNRECOGNIZED_VERSION`
  (+ `packageName`, `certificateSha256Digest`, `versionCode`).
- `deviceIntegrity`: `MEETS_STRONG_INTEGRITY` (hardware-backed, Android 13+),
  `MEETS_DEVICE_INTEGRITY` (hardware-backed, con fallback software pre-13),
  `MEETS_BASIC_INTEGRITY`, `MEETS_VIRTUAL_INTEGRITY`.
- Opcionales opt-in: `appAccessRiskVerdict`, `playProtectVerdict`,
  `recentDeviceActivity`, `deviceRecall` (beta).

### 4.3 Requisitos de configuración

- **Play Console**: activar la API en *Test and release > App integrity*.
- **Google Cloud**: vincular un proyecto GCP a la app en Play Console (número de
  proyecto cloud requerido por Standard).
- **Config de firmas**: SHA-256 de los certificados (Play App Signing) correctos.
- **Testers**: la API funciona en tracks de test (internal/closed/open) con
  testers licenciados.
- **Límite**: **10.000 solicitudes/día** por defecto (console: aumentar con ~7
  días de proceso). Classic y Standard comparten cuota de decodificación en
  servidor (≈10k decodes/día por defecto).
- **Comportamiento offline**: los veredictos **no** se pueden obtener offline;
  las respuestas no deben cachearse (recomendado). Por tanto, la estrategia
  offline (§7) no puede apoyarse en Integrity cacheada.

### 4.4 Rol en la arquitectura de SIRC (Veredicto del gate)

**Play Integrity NO es la barrera de suscripción.** Es una SEÑAL de entorno que
el backend combina con entitlement/Play Billing. Uso recomendado:

- **`accountDetails.appLicensingVerdict == LICENSED`**: condición mínima de
  "instalación legítima de Play". **No equivale a suscripción activa.**
- **`deviceIntegrity` tiered enforcement**: no bloquear a nivel BASIC; degradar
  (p. ej. limitar acciones Premium si el dispositivo no supera STRONG mientras
  dure un incidente de abuso). Evitar penalizar root a priori.
- **`appRecognition`**: detectar APK repackaged/debug.
- Estrategia recomendada por la doc: **medición previa sin enforcement**
  (observar qué veredictos da la base instalada antes de endurecer).

**Decisión (S1)**: Implementar Play Integrity en **modo Standard por defecto**,
con `requestHash` de acciones sensibles (suscribirse, validar entitlement) y
Classic solo para "restaurar compra" (acción rara de alto valor). El enforcement
se hace en backend con tiered strategy. **Fuera de alcance de Sprint 12** (ver
roadmap revisado): se instrumenta en E1b junto a backend.

## 5. Google Play Licensing / Suscripciones y Billing — flujo recomendado

Fuente: `developer.android.com/google/play/billing/*` y Play Console help.

> **Actualización (LOOP Backend, 16-ago-2026)**: la API v3
> `purchases.subscriptions.get` está **DEPRECADA**. El endpoint oficial actual
> es **`purchases.subscriptionsv2.get`**
> (`androidpublisher/v3/applications/{packageName}/purchases/subscriptionsv2/tokens/{token}`).
> Las referencias a "subscriptions.get" en versiones anteriores de este
> documento quedan obsoletas. Estados modernos de `SubscriptionPurchaseV2`:
> `SUBSCRIPTION_STATE_PENDING`, `ACTIVE`, `PAUSED`, `IN_GRACE_PERIOD`, `ON_HOLD`,
> `CANCELED`, `EXPIRED`, `PENDING_PURCHASE_CANCELED` (no existen estados propios
> "GRACE PERIOD"/"ACCOUNT HOLD"/"DISMISSED"). `paymentState` fue eliminado (se
> deriva del `subscriptionState`).

### 5.1 Verificación de compra (purchase token)

Flujo oficial recomendado para suscripciones:

```
APP ──▶ Google Play checkout ──▶ purchase token
APP ──▶ BACKEND: POST /verify-purchase {purchaseToken, productId}
BACKEND ──▶ Google Play Developer API (purchases.subscriptionsv2.get)
       ◀── subscriptionState, lineItems[].expiryTime, autoRenewEnabled, ...
BACKEND ──▶ grant entitlement (Supabase: subscriptions + entitlements)
BACKEND ──▶ APP: entitlement activo (con TTL en cliente)
```

Detalles técnicos del flujo:

- El servidor queda **obligado a llamar a la API de Play** con el `purchaseToken`
  (nunca confiar en el token sin validar).
- El entitlement se concede si `subscriptionState ∈ {ACTIVE, IN_GRACE_PERIOD}`
  (o `CANCELED` con `lineItems[].expiryTime` futuro). `PENDING`, `ON_HOLD`,
  `PAUSED`, `EXPIRED`, `PENDING_PURCHASE_CANCELED` → **no** entitled.
- `expiryTime` (por lineItem) define cuándo revocar.
- `linkedPurchaseToken`: al renovar/upgrade, el token anterior queda inválido
  (evita dobles entitlements).
- **Acknowledgment** de compras (backend) dentro de la ventana de 3 días en
  prod (test más corto) para evitar reembolsos automáticos.

### 5.2 Estados de suscripción (Play v2 → entitlement SIRC)

| Estado SIRC | Estado Play (`subscriptionsv2`) | Acceso | Acción backend |
|---|---|---|---|
| ACTIVE | ACTIVE | Sí | Entitlement activo. |
| GRACE | IN_GRACE_PERIOD | Sí (hasta fin de periodo) | Mantener acceso; avisar UI. |
| ON_HOLD | ON_HOLD (ex "ACCOUNT HOLD") | **No** | Revocar / bloquear premium. |
| CANCELED | CANCELED (billing period ends) | No tras fin de periodo | Revocar al vencer (`expiryTime`). |
| EXPIRED | EXPIRED | No | Revocar. |
| PAUSED | PAUSED | No | Revocar (reanuda en `autoResumeTime`). |
| RECOVERED | RECOVERED (mismo token) | Según Play | Restaurar entitlement. |
| PENDING_PURCHASE | PENDING | No (hasta completar) | No otorgar. |

### 5.3 Entitlement server (fuente de verdad)

- Backend mantiene: `account_id`, `purchase_tokens[]`, `subscription_state`,
  `expiry_time`, `plan_id`, `linked_purchase_token`, `acknowledged`, `revoked`;
  y el entitlement derivado con `tier` (`FREE`/`PREMIUM`).
- **Entitlement FREE**: se adjudica en el alta de cuenta (0 €), sin compra, con
  `server_issued_at`/`server_expires_at` y revocación posible por servidor.
  No es "premium desbloqueado con botones ocultos": es un estado server-side
  igual de gestionable (ver "Seguridad del Free" más abajo).
- **RTDN**: Google Cloud Pub/Sub notifica cambios (`SUBSCRIPTION_PURCHASED`,
  `RENEWED`, `CANCELED`, `ON_HOLD`, `IN_GRACE_PERIOD`, `RECOVERED`, `EXPIRED`,
  `REVOKED`, `voidedPurchaseNotification`, ...). El handler:
  1. Verifica autenticación del push (JWT OIDC de Pub/Sub en el header).
  2. Dedupe por `messageId`.
  3. **SIEMPRE consulta la API** (`subscriptionsv2.get`) con el token,
     **nunca** toma la notificación como estado sin contrastarla (regla de oro
     de la documentación) y actualiza la DB.
- **Restore purchases**: `queryPurchasesAsync` en cliente + reconciliación con
  el backend (`subscriptionsv2.get` es autoridad; `queryPurchaseHistory`
  deprecado en PBL 7).
- La implementación propuesta: **Supabase** (Auth + RLS + Edge Functions +
  Postgres) como servicio de identidad/suscripción/entitlement — detalle en
  `docs/BACKEND_ARCHITECTURE.md`.

### 5.4 Qué NO debe pasar por el backend

- **Contenido de ofertas/snapshots/OCR**: prohibido subir (también por privacidad).
- Monto/detalles de viajes (siempre derivado local; sin envio).
- La app funciona sin backend para el pipeline: el backend solo gana en
  verificaciones de cuenta/suscripción.

### 5.5 Seguridad del Free (D15.3)

El modelo **SIRC FREE** (descarga gratuita + cuenta gratuita) **no relaja el
modelo de seguridad**:

- El Free es `entitlement = FREE` adjudicado **server-side**; el límite del Free
  (`FREE_LIMITS = TBD`) se decide server-side, no por ocultar botones en el APK.
- Manipular/clonar el APK para "desbloquear" features premium **no produce
  premium indefinido**: el gate consulta `EntitlementRepository` (online o caché
  firmado con TTL); el backend conserva revocación y gestión por cuenta.
- La clave de firma del caché y los secretos server-only **nunca** viven en el
  APK (`BACKEND_ARCHITECTURE.md` §2.5).
- T15–T20 (APK modificado/repackaged) aplican igualmente a la fase Free:
  Integridad, RLS, RTDN y TTL son las mismas barreras.

## 6. Suscripción offline (crítico para SIRC)

### 6.1 Preguntas y respuestas

- **¿Debe funcionar SIRC completamente offline?** El **núcleo de captura sí** y
  sin backend (privacidad). La **suscripción premium** requiere conectividad
  periódica (ver TTL). El **entitlement FREE** también se cachea con el mismo
  TTL y se revalida online: el estado local nunca es autoridad sobre el tier.
- **¿Cuánto puede funcionar sin renovar entitlement?** Definimos un **TTL
  máximo de 24–72 h** (a configurar, decisión S2) para el caché de entitlement.
- **¿Cómo se protege el periodo offline?** El caché está cifrado y **firmado por
  el servidor** con clave en Keystore; cualquier manipulación rompe la firma →
  se pierde entitlement. Al volver online se revalida contra el backend.
- **¿Cómo se revoca una cuenta?** RTDN al instante + validación online; al
  conectar, el caché queda invalidado y la UI muestra "suscripción vencida".
- **¿Qué ocurre si el reloj del dispositivo se manipula?** El caché firma el
  `expiry` y se compara con un `serverUtc` remoto (o con NTP ajustado), y la
  lógica de TTL usa tiempo del servidor en la revalidación; el reloj local solo
  sirve como referencia provisional, nunca como autoridad para extender.
- **¿Cómo evitar un entitlement cacheado indefinido?** TTL + refresh (al
  conectarse), + invalidación por RTDN, + firma server HMAC (caduca).

### 6.2 Alternativas evaluadas

| Estrategia | Pros | Contras | Veredicto para SIRC |
|---|---|---|---|
| Online-only premium | Seguro, sin fraude offline | CX rota (conductor sin señal; core del mercado) | ❌ descartada |
| Entitlement cacheado con TTL corto | Equilibrio offline/seguridad | Ventana de fraude acotada | ✅ **elegida** |
| Grace period (Play) | Sincronizado con cobro | Se aplica en Play, no evita cache largo | Parcialmente (combinado) |
| Renovación periódica obligatoria | Reduce ventana | Requiere red; molesto offline | Usada como complemento |
| Challenge/response (retos) | Alto coste para atacante | Complejidad y UX | Para acciones premium raras |

## 7. Seguridad criptográfica (sin criptografía propia)

Regla: **no diseñar criptografía propia**. Usar mecanismos oficiales:

- **Android Keystore** (hardware-backed cuando disponible, `isInsideSecureHardware`,
  StrongBox opcional). Generar claves **dentro** del Keystore; nunca hardcodear.
- **Cifrado de datos sensibles en reposo**: caché de entitlement/sesión con clave
  AES del Keystore (o usar `EncryptedSharedPreferences` **solo si procede**; la
  versión actual de `androidx.security` la ha dejado en desuso para nuevos
  desarrollos — se usarán primitivas Keystore directamente si es necesario).
  Datos no sensibles (historial de ofertas) pueden quedar en Room en claro
  (no exponen autorización).
- **Tokens de sesión de corta duración** con rotación (JWT sobre algo, firmado
  por nuestro servidor, preferiblemente opaco).
- **Attestation**: Key Attestation para verificar que las claves son
  hardware-backed cuando se gane acceso a features premium sensibles.
- **Protección de secretos**: nada de API keys hardcodeadas; el backend usa
  cuentas de servicio GCP para las APIs de Play.

**¿Qué debe existir localmente?** Únicamente: install/session id cifrados,
caché de entitlement cifrado+FIFO firmado, pref de UI no sensibles. **No**
deben existir localmente: estado de suscripción en claro, firmas ni claves que
desbloqueen premium sin servidor.

## 8. Backend conceptual (sin implementación)

**Decisión del LOOP Backend (16-ago-2026)**: el backend inicial se construirá
sobre **Supabase** (Auth + PostgreSQL + RLS + Edge Functions), ver
`docs/BACKEND_ARCHITECTURE.md`. Las funciones de negocio se ejecutan en **Edge
Functions** (server-side, con secretos a salvo); el cliente solo lleva la
publishable key de Supabase y URL del proyecto.

**Lo mínimo que el backend debe ofrecer** (ordenado por prioridad):

1. **Autenticación de cuenta** (Supabase Auth; flujo Google Sign-In nativo +
   sesión JWT corta + refresh token de un solo uso).
2. **Verificación de suscripción** (**Edge Function `verify-purchase`**) contra
   Google Play Developer API (`purchases.subscriptionsv2.get`).
3. **Entitlement service** (fuente de verdad en Supabase): estado, TTL,
   `linked_purchase_token`, revocación.
4. **RTDN receptor** (Pub/Sub → Edge Function `on-play-rtdn`) → actualiza
   entitlement.
5. **Gestión de dispositivos/sesiones**: token de instalación, detectar clonación
   (tablas `devices`/`sessions`).
6. **Rate limiting** + auditoría mínima (replay, abuso de endpoints).
7. **Recuperación de cuenta** (re-vincular al Play; restore purchases).

**Lo que NO debe almacenarse**:

- Contenido de pantalla, snapshots, OCR, ofertas, rutas, montos.
- Nada que derive de datos de viajes. La API del backend es **exclusivamente
  comercial/de cuenta**.

> Previsto: si Supabase quedara insuficiente o PPro-resultara inviable, se
> re-evaluaría un micro-backend propio (Go/Node) cubriendo los mismos 2 flujos
> esenciales (verify-purchase + RTDN). Decisión documentada, diferida a E1b.

## 9. Privacidad (cruce seguridad × sensitive surfaces)

SIRC usa: Accessibility, MediaProjection, OCR, capturas de pantalla, datos de
viajes (montos, distancia, tiempo, ubicación de recorrido) y config financiera
del conductor (costos, metas).

| Dato | Procesamiento | Puede salir | Nunca sale | Consentimiento |
|---|---|---|---|---|
| Texto de pantalla (Accessibility) | Local (parsing) | ❌ | ✅ | Accesibilidad declarada en Play; no compartir como telemetría. |
| Frames MediaProjection | Local (OCR) | ❌ | ✅ | Permiso temporal de captura; eliminar frame tras OCR. |
| Datos derivados de ofertas (ganancia, $/km, $/h) | Local + historial Room | ❌ | ✅ | No subir. |
| Métricas de rendimiento teléfono (integrity signals) | Local → solo al servidor de verificación, agregado | Solo aggregation | ❌ | Play Integrity ToS; no vinculado a perfil del conductor. |
| Config del conductor (costos, moneda, metas) | Local Room | ❌ | ✅ | Solo local; esencial para privacidad. |
| Identidad/cuenta + suscripción | Cliente → Backend | ✅ (mínimo: email/GoogleID + tokens) | Datos de viaje | Consentimiento de cuenta (términos); Google Sign-In. |
| Ubicación GPS (futuro) | Local si alguna vez existe | ❌ (ideal) | ✅ | Permiso runtime + reselección tras cada uso si se implementa. |

**Principios** (para la hoja de ruta de privacidad, alinear con
`GOOGLE_PLAY_COMPLIANCE.md`):

- Los datos de la oferta **nunca** salen: reafirma "LOCAL-FIRST" para el núcleo.
- El mínimo que viaja es identidad + tokens de compra + señales de integridad.
- Todo lo que el backend toca debe tener TTL/borrado y auditoría.

## 10. Limitaciones (declaración honesta)

- **El fraude telefónico no tiene solución 100 %** en un dispositivo gestionado
  por el atacante (root/Xposed/instrumentación). El modelo local-first + servidor
  reduce el abuso económico, no lo elimina.
- **Play Integrity no es infalible**: es una señal con tasa de error, por eso se
  usa con tiered enforcement, no como permiso binario global.
- **La brecha offline (TTL)** es un tradeoff deliberado por UX del conductor;
  se documenta como riesgo aceptado (T14).
- La app no funciona premium **sin conectividad periódica**: los conductores más
  desconectados (sin señal prolongada) quedarán fuera de features premium, por
  diseño, para proteger el modelo comercial.
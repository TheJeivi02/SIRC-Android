# SIRC — Modelo de Seguridad (v1)

> Documento formal de seguridad, suscripción y modelo de confianza de SIRC.
> Resultado del **Roadmap Gate** (LOOP de consistencia: estrategia competitiva +
> informe ejecutivo + arquitectura + roadmap + modelo comercial de suscripción +
> seguridad). **No implementa nada**: define la arquitectura objetivo y las
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

## 3. Threat Model (T1–T14) para SIRC

Metodología: cada amenaza con impacto (I) y probabilidad (P) en Alto/Medio/Bajo,
capa responsable y limitaciones. **No prometemos protección absoluta.**

| # | Amenaza | I | P | Mitigación | Capa | Limitaciones |
|---|---|---|---|---|---|---|
| T1 | APK repackaged/firmado de nuevo | Alto | Medio | Play Integrity `appIntegrity` (`PLAY_RECOGNIZED` + `certificateSha256Digest`) verificado en backend con `requestHash`/`nonce`. | Cliente→Backend | Detecta en servidor al operar; no evita instalación sin red. |
| T2 | Eliminación de comprobaciones premium | Alto | Medio | Entitlement verificado en servidor; la app gateway sobre estado server, no sobre strings locales; R8/ProGuard. | Backend | Rooting completo con instrumentación puede evadir la UI local. |
| T3 | Falsificación de entitlement local | Alto | Medio | Never trust client: backend + `purchases.subscriptions.get` + TTL + firmas server HMAC corto. | Backend | Un APK totalmente reescrito puede omitir el gate (ver T2). |
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

**Conclusión del threat model**: la mayor superficie de fraude comercial se
mitiga **exclusivamente** en el servidor (entitlement, Play APIs, rate limiting).
El cliente contribuye señales (Integrity, tokens) pero **jamás es autoridad**.
Esto define el rol de Play Integrity §4: **no es por sí solo la barrera de
suscripción**, es una señal adicional.

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

### 5.1 Verificación de compra (purchase token)

Flujo oficial recomendado para suscripciones:

```
APP ──▶ Google Play checkout ──▶ purchase token
APP ──▶ BACKEND: POST /verify {purchaseToken, productId}
BACKEND ──▶ Google Play Developer API (purchases.subscriptions.get)
       ◀── estado (expiryTimeMillis, paymentState, cancelReason, ...)
BACKEND ──▶ grant entitlement (base de datos)
BACKEND ──▶ APP: entitlement activo (con TTL en cliente)
```

Detalles técnicos del flujo:

- El servidor queda **obligado a llamar a la API de Play** con el `purchaseToken`
  (nunca confiar en el token sin validar).
- `expiryTimeMillis` define cuándo revocar (revocar antes de esa fecha).
- `linkedPurchaseToken`: al renovar/upgrade, el token anterior queda inválido
  (evita dobles entitlements).
- **Acknowledgment** de compras (dentro de la ventana de 3 días en prod; test
  más corto) para evitar reembolsos automáticos.

### 5.2 Estados de suscripción (Play)

| Estado | Acceso | Acción backend |
|---|---|---|
| ACTIVE | Sí | Entitlement activo. |
| GRACE PERIOD | Sí (hasta fin de periodo) | Mantener acceso; avisar UI. |
| ACCOUNT HOLD | **No** | Revocar / bloquear premium. |
| CANCELLED (billing period ends) | No tras fin de periodo | Revocar al vencer. |
| EXPIRED | No | Revocar. |
| PAUSED | No | Revocar. |
| RESTORED/MIGRATED | Según Play | Recalcular entitlement. |
| PENDING_PURCHASE | No (hasta completar) | No otorgar. |

### 5.3 Entitlement server (fuente de verdad)

- Backend mantiene: `account_id`, `purchase_tokens[]`, `subscription_state`,
  `expiryTimeMillis`, `plan_id`, `linked_products`.
- **RTDN**: Google Cloud Pub/Sub notifica cambios (`SUBSCRIPTION_RENEWED`,
  `CANCELED`, `ON_HOLD`, `EXPIRED`, `REVOKED`, ...). El handler **siempre
  consulta la API** (`purchases.subscriptions.get`) con el token, verifica
  `messageId` único (idempotencia) y actualiza la DB. **Nunca** tomar la
  notificación como estado sin contrastar (regla de oro de la documentación).
- **Restore purchases**: `queryPurchasesAsync` en cliente + reconciliación con
  el backend (la API de Play es autoridad).

### 5.4 Qué NO debe pasar por el backend

- **Contenido de ofertas/snapshots/OCR**: prohibido subir (también por privacidad).
- Monto/detalles de viajes (siempre derivado local; sin envio).
- La app funciona sin backend para el pipeline: el backend solo gana en
  verificaciones de cuenta/suscripción.

## 6. Suscripción offline (crítico para SIRC)

### 6.1 Preguntas y respuestas

- **¿Debe funcionar SIRC completamente offline?** El **núcleo de captura sí** y
  sin backend (privacidad). La **suscripción premium** requiere conectividad
  periódica (ver TTL).
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

**Lo mínimo que el backend debe ofrecer** (ordenado por prioridad):

1. **Autenticación de cuenta** (recomendado: flujo Google Sign-In + session
   token opaco; alternativa: anónima vinculada a Play license).
2. **Verificación de suscripción** (`/verify`) contra Google Play Developer API.
3. **Entitlement service** (fuente de verdad): estado, TTL, `linkedPurchaseToken`,
   revocación.
4. **RTDN receptor** (Pub/Sub) → actualiza entitlement.
5. **Gestión de dispositivos/sesiones**: token de instalación, detectar clonación.
6. **Rate limiting** + auditoría mínima (replay, abuso de endpoints).
7. **Recuperación de cuenta** (re-vincular al Sí Play).

**Lo que NO debe almacenarse**:

- Contenido de pantalla, snapshots, OCR, ofertas, rutas, montos.
- Nada que derive de datos de viajes. La API del backend es **exclusivamente
  comercial/de cuenta**.

También se evaluará, al implementar, si un servicio de suscripción gestionado
(RevenueCat-like) es más eficiente; decisión diferida a implementación.

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
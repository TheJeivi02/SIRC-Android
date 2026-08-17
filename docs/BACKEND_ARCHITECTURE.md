# SIRC — Arquitectura de Backend (conceptual, Supabase)

> Diseño conceptual del backend de SIRC para el modelo de **app de pago por
> suscripción** (LOOP ENGINEERING — Backend Supabase, 16-ago-2026),
> **actualizado por el LOOP Modelo Free + Supabase Account Gate** (16-ago-2026).
>
> **ESTADO: documentación de diseño SOLO.** No existe proyecto Supabase, no hay
> tablas, Edge Functions, SDK, Billing, Auth ni Play Integrity implementados, y
> **no se implementarán** hasta abrir la tarea **E1b** (regla 9f / R16). El
> núcleo de captura/evaluación permanece **100 % local** (LOCAL-FIRST).
>
> Fuentes: documentación oficial de Supabase y Google Play (citada en cada
> sección con `[DOC]` = documentación oficial, `[INF]` = inferencia razonada).
> Relación con otros docs: `SECURITY_MODEL.md` (threat model y trust model),
> `SUBSCRIPTION_MODEL.md` (planes/lifecycle), `PRODUCT_STRATEGY.md` (roadmap).

## 0. Supabase ACCOUNT GATE (regla operativa)

> **Regla (D15.4)**: si durante el trabajo se determina que para implementar o
> probar Supabase realmente necesitamos crear/configurar recursos reales
> (cuenta, proyecto, Project URL, publishable/anon key, Authentication, base de
> datos, políticas RLS, etc.):

1. **NO inventar valores ni usar credenciales ficticias.**
2. **NO crear una cuenta en nombre del usuario.**
3. **NO continuar** como si el backend estuviera configurado.
4. **DETENERSE y solicitar al usuario**: *"Necesitamos crear/configurar la
   cuenta de Supabase para continuar."*
5. Proporcionar entonces la guía paso a paso (§0.1).

### 0.1 Guía de configuración (para cuando el usuario la necesite)

| Paso | Acción del usuario | Notas |
|---|---|---|
| 1 | Crear / iniciar sesión en Supabase (`supabase.com`) | Cuenta personal (email u OAuth). |
| 2 | Crear el proyecto **SIRC** | Elegir región cercana al público objetivo (LATAM/MX recomendada, p. ej. `us-east` o `southamerica-east1` según disponibilidad `[INF]`). |
| 3 | Configurar región/organización | Crear organización si no existe; asignar propietario. |
| 4 | Obtener credenciales públicas | **Project URL** (`https://<ref>.supabase.co`) + **publishable key** (la antigua anon key; `sb_publishable_…`). |
| 5 | Configurarlas de forma segura en el proyecto | En `local.properties`/inject con ocultación (NO en git) o como BuildConfig para debug; jamás commitear secretos. |

**Importante**: NUNCA pedir al usuario que pegue en archivos públicos, GitHub o
chat: `service_role` / secret key, claves privadas, secretos de webhook o
credenciales de servidor (§10 secrets).

## 1. Principio: LOCAL-FIRST (el camino crítico NO pasa por Supabase)

El camino crítico de producto permanece 100 % en el dispositivo:

```
CAPTURA → OCR → DETECCIÓN → PARSING → RENTABILIDAD → RECOMENDACIÓN → OVERLAY
```

**Prohibido diseñar** `CAPTURA → INTERNET → SUPABASE → EVALUACIÓN → OVERLAY`
porque degradaría velocidad (objetivo UX de decisión **<1 s**; `<3 s` es el
límite técnico/E2E), disponibilidad (sin red), batería
(websockets/cifrado), privacidad (fuga de pantalla) y resiliencia.

Lo que **sí** pasa por Supabase (solo datos de cuenta/comercial):

```
IDENTIDAD → SUSCRIPCIÓN → ENTITLEMENT → INTEGRIDAD → DISPOSITIVOS/SESIONES → SOPORTE
```

**Ninguna oferta capturada, frame, OCR ni historial de viajes sale del
dispositivo** por defecto (§9 SECURITY_MODEL, regla R9e).

## 2. Evaluación de Supabase para SIRC (veredicto)

Tras revisar la documentación oficial de Supabase (16-ago-2026), **Supabase es
ADEcuado como backend inicial de identidad + suscripción/entitlement**, con un
perfil de uso mínimo y disciplinado. No es adecuado (ni necesario) para el
pipeline de ofertas.

### 2.1 Qué usar inicialmente (`[DOC]` salvo marca)

| Componente | Uso | Justificación |
|---|---|---|
| **Auth** | Autenticación de cuenta | OAuth Google nativo (Credential Manager, `signInWithIdToken` con nonce), sesiones JWT + refresh token de un solo uso, MFA opcional. Claims de autorización en `app_metadata` (no editable por el usuario). |
| **PostgreSQL + PostgREST** | Persistencia de perfiles/planes/suscripciones/entitlements/dispositivos/sesiones | CRUD REST auto-generado; las **Auth** tablas expuestas vía REST se protegen SIEMPRE con RLS habilitado. |
| **Row Level Security (RLS)** | Barrera por-fila sobre datos de cuenta | `auth.uid()` como base; cada tabla del esquema público expuesto debe tener RLS on - política `IS NOT NULL` implícita con `auth.uid()`. Evitar `user_metadata` en políticas. |
| **Edge Functions (Deno)** | Verificación de compra (`verify-purchase`) y reconciliación de RTDN | Único lugar con acceso a secretos (service account de Play). `verify_jwt=true` por defecto (autenticado). Frío medio ~42 ms (P99 ~460 ms `[DOC]`, blog jul-2025); correcto para acciones puntuales, no para el overlay. |
| **Secrets** | Service account de Google Play (OAuth JWT) | En `supabase secrets`, JAMÁS en el APK. En el cliente solo URL del proyecto + publishable key (la antigua anon key). |
| **Webhooks de Billing** | Notificar cambios de entitlement | Los eventos de Google Play llegan por RTDN (Pub/Sub), no por webhooks Supabase; se procesan en Edge Function, no en un DB webhook. |

### 2.2 Qué NO usar inicialmente (y por qué)

| Componente | Decisión | Por qué |
|---|---|---|
| **Realtime (postgres_changes)** | ❌ NO | Procesar entitlement online vía websocket 24/7 consume batería y conexión; los cambios de entitlement son raros. Suficiente: verificar al abrir app + en acciones premium + on-demand. |
| **Storage** | ❌ NO | SIRC no sube capturas por defecto. Si nunca hay sincronización de evidencia opcional, no se añade. |
| **Database Webhooks** | ❌ NO | No hay receptores externos de eventos Supabase que necesiten disparo de DB. |
| **PostgREST para datos de ofertas** | ❌ NO | Los datos de ofertas son locales (Room). Prohibido subirlos (privacy + R9e). |
| **custom claims por `user_metadata`** | ❌ NO | Es editable por el usuario; rompe RLS/autorización. |
| **service_role / secret key en el cliente** | ❌ NO | Solo Edge Functions (server-side). La publishable key (anon) es la única que vive en el APK. |

### 2.3 Riesgos y advertencias conocidos

- **Tabla sin RLS + publishable key pública = fuga total** (`[DOC]`: RLS debe estar activa en todo esquema expuesto).
- **JWT de acceso no fresco**: el claim de rol tarda en refrescar; revocaciones se reflejan en el siguiente refresh token.
- **Pausa de proyectos Free por inactividad** (7 días `[DOC]`, 2026): inaceptable para producción → **Pro** desde el momento en que exista backend en uso real.
- **SDK Kotlin** (`supabase-kt`) es **community-maintained** (`[INF]`): maduro pero sin SLA de Google; considerar envolverlo detrás de contratos `:domain` (`AuthRepository`, `EntitlementRepository`) para poder sustituirlo.
- **Supabase ≠ garantía de purchase validation**: la verificación real la hace la Google Play Developer API desde nuestra Edge Function; Supabase solo aloja/ejecuta.

### 2.4 Veredicto

- **Sí a Supabase como backend inicial** (Auth + RLS + Edge Functions + Postgres) para identidad/suscripción/entitlement.
- **Plan `[INF]`**: desarrollo en **Free** ok para E1b-bootstrap; **Pro ($25/mes)** antes de producción (sin pausa, backups, 8 GB).
- **Importante**: este backend solo trata datos de cuenta; el valor que protege (habilidad premium de SIRC) se decide siempre contra Google, no contra lo que el APK diga.

### 2.5 Secretos: client-safe vs server-only (D15.5)

Regla operativa para NO filtrar material sensible:

| Clase | Qué incluye | Dónde vive | Nunca en… |
|---|---|---|---|
| **Client-safe** | Project URL (`https://<ref>.supabase.co`), publishable key (anon/new). | APK / `local.properties` (gitignore) / BuildConfig de debug oculto | — |
| **Server-only** | `service_role` (secret key), service account de Google Play (OAuth JWT), claves privadas, secretos de webhooks/RTDN, credenciales de firmado (keystore), Google Play Developer API credentials. | Edge Functions (`supabase secrets`), CI secret store | APK, git, GitHub, chat, `local.properties` |

Reglas: (1) el APK **solo** transporta client-safe; (2) cualquier rotación de
clave se documenta; (3) auditoría de quién accede a secretos server-only
(regla R9g); (4) si un secreto aparece en git/chat/APK → revocar y rotar.

### 2.6 Desarrollo local sin backend (dev sin Supabase)

- **El núcleo LOCAL-FIRST funciona sin cuenta Supabase**: desarrollar y probar
  el pipeline CAPTURA→OCR→PARSER→PROFIT→RECOMMENDATION→OVERLAY **no requiere
  proyecto, credenciales ni red**.
- El backend se aísla detrás de contratos `:domain` (`AuthRepository`,
  `EntitlementRepository`); en ausencia de backend real se usa una
  implementación _no-op/local_ (dev) que devuelve `state = TRIAL_ACTIVE` local
  (solo dev/test; en producción el valor lo da el servidor).
- **Account Gate**: insertar credenciales/URL **solo si el usuario las aporta**
  (§0); no se bloquea el dev local ni los tests por no existir Supabase.
- Las pruebas de `:domain`/`:core:*`/`:feature:overlay` siguen en verde sin
  backend (dependencias hacia adentro; Kotlin puro).

### 2.7 Trial anti-abuso (backend)

El trial de 14 días se controla **en backend**, nunca en el cliente
(`SECURITY_MODEL.md` §6.1bis, `SUBSCRIPTION_MODEL.md` §1.3):

- Fuente de verdad: tabla `trial` por `user_id` (`trial_start`/`trial_end`/
  `status`), adjudicada en el alta de cuenta.
- Reinstalación / borrado de datos / cambio de dispositivo: el trial persiste en
  la cuenta (`auth.uid()`), no en el dispositivo.
- Múltiples cuentas / abuso: mitigado por control de dispositivos/sesiones
  (`devices`/`sessions`, rate limiting, RLS) y políticas de abuso en Edge
  Function; sin bloquear usuarios legítimos (UX equilibrada).
- Reloj manipulado: el trial se compara contra `serverUtc`/`trial_end` del
  backend, nunca contra el reloj local como autoridad.

## 3. Modelo de datos conceptual (entidades)

> No implementar. Propietario, campos principales, estado, lifecycle, relaciones, RLS y privacidad mínima.

```
users (Auth gestionado por Supabase Auth)
profiles ──1:1── users
plans ────────── groups of entitlements (trial/weekly/monthly/annual)
trial ────────── 1:1 users · control anti-abuso (server-side)
subscriptions ── N:1 users · 1:1 plans · purchase lifecycle
entitlements ─── 1:1 user+plan · N active · revocable · TTL · state conceptual
devices ──────── N:1 users · install/session binding
sessions ─────── N:1 devices · tokens · rotations
```

### Arquitectura de cuenta (lado cliente, D15.6)

Cadena conceptual de la cuenta SIRC en el dispositivo:

```
SIRC ACCOUNT (identidad) → PROFILE (preferencias) → SUBCRIPCIÓN/ENTITLEMENT
→ CACHE LOCAL ENTITLEMENT (firmado, TTL) → gate de feature
```

- **User/Identity**: lo aporta Supabase Auth (`auth.uid()`); el cliente guarda
  solo referencias de sesión seguras.
- **Profile**: display_name, moneda preferida (opcional); config del conductor
  permanece local.
- **Subscription/Entitlement**: estado derivado por el backend; `state` =
  `TRIAL_ACTIVE`/`PREMIUM_ACTIVE`/etc.
- **Local entitlement cache**: reflejo firmado con TTL (§7) que alimenta el
  gate offline; nunca es autoridad.

Gates de feature en el cliente consultan únicamente ese caché autorizado:
`entitlement == FREE` → alcance Free; `entitlement == PREMIUM` → features
premium; ante duda/expiración → bloquear y revalidar online.

### users
- Fuente: **Supabase Auth** (`auth.users` gestionada por Supabase, no nuestro esquema `public`).
- Propietario: sistema (Auth). No contiene datos de ofertas.
- Solo referencia por `auth.uid()` en RLS.

### profiles
- Nuestro esquema, `user_id uuid references auth.users primary key`.
- Campos: `user_id`, `display_name`, `preferred_currency` (opcional, mejora UX local; opcional, NO obligatorio), `created_at`, `updated_at`.
- Estado: 1 fila por usuario (auto-creada en sign-in). No guardar config del conductor (eso es local).

### plans
- Nuestro esquema. Catálogo de planes (conceptualmente **Trial 14 días →
  suscripción Weekly/Monthly/Annual**; ver `SUBSCRIPTION_MODEL.md` §2).
- Campos: `id`, `code` (único, ej. `sirc_weekly`, `sirc_monthly`,
  `sirc_annual`, `sirc_trial`), `name`, `base_plan_id_play` (código del BasePlan
  de Play; `sirc_trial` es un estado de cuenta conceptual, no un plan de Play
  facturable), `offer_id_play` (nullable), `entitlements` (JSON de features),
  `active`.
- Propietario: SIRC (administración). RLS: lectura pública (catálogo), escritura
  solo admin (service role).
- **Trial (14 días)**: NO requiere compra; el backend adjudica el estado
  `TRIAL_ACTIVE` en el alta/momento de la cuenta (`trial_start`/`trial_end`/
  `trial_status`), con `server_issued_at`/`server_expires_at` y revocación/
  bloqueo por servidor (anti-abuso §2.7). No relaja la seguridad
  (`SECURITY_MODEL.md` §6.1bis).

### trial (control anti-abuso)
- Nuestro esquema. `user_id` (único), `trial_start`, `trial_end`, `status`
  (`ACTIVE`/`EXPIRED`/`RESTRICTED`), `source` (`account`/`play_offer` nullable),
  `revoked_at` nullable.
- Propietario: Edge Function / backend (adjudicación en alta de cuenta).
- **El trial es server-side**: NO se confía en fecha local, almacenamiento local
  ni instalación (reinstalación/borrado de datos/cambio de dispositivo/múltiples
  cuentas/reloj manipulado se controlan contra `auth.uid()` y sesiones).

### subscriptions
- Nuestro esquema. Estado operativo por compra.
- Campos: `id`, `user_id`, `purchase_token` (**clave primaria natural, globalmente único** `[DOC Play]`), `plan_id` → plans, `subscription_state` (enum mapa del `SubscriptionState` moderno de Play §5), `expiry_time` (server UTC), `auto_renew_enabled`, `linked_purchase_token` (nullable), `region_code`, `created_at`, `updated_at`, `acknowledged` (bool), `revoked` (bool).
- Relations: `user_id` auth.uid(); `plan_id`; `linked_purchase_token` apunta a `purchase_token` anterior (para invalidarlo).
- RLS: `user_id = auth.uid()` (escribir solo vía Edge Function con service_role; leer por el dueño).

### entitlements
- Nuestro esquema. `user_id`, `plan_id`, `status` (ACTIVE/SUSPENDED/GRACE),
  `tier` (`TRIAL`/`PREMIUM`), `state` (uno de los estados conceptuales
  `TRIAL_ACTIVE`/`TRIAL_EXPIRED`/`PREMIUM_ACTIVE`/`SUBSCRIPTION_EXPIRED`/
  `ACCOUNT_RESTRICTED`/`ACCOUNT_UNKNOWN`), `server_issued_at`,
  `server_expires_at` (TTL), `cache_signature_server` (firma HMAC del estado),
  `revoked_at` nullable.
- Propietario: Edge Function. El cliente **jamás** escribe entitlements.
- RLS: `user_id = auth.uid()` para cache server-leído; escritura solo service role.
- `state = TRIAL_ACTIVE` se adjudica server-side en el alta de cuenta (14 días,
  `trial` tabla); `state = PREMIUM_ACTIVE` por suscripción verificada
  (`subscriptionsv2.get`). Todo estado es revocable/bloqueable por servidor.

### devices
- `id uuid`, `user_id`, `install_id` (cifrado local, referencial para el servidor), `platform` (android), `app_version`, `integrity_last_verdict` (nullable, resumen), `is_revoked`.
- Propietario: Edge Function (alta/registro con token). Binding a instalación: mitigación T6 (clonación).
- RLS: `user_id = auth.uid()`.

### sessions
- `id uuid`, `user_id`, `device_id → devices`, `token_oid` (hash, nunca el token), `issued_at`, `expires_at`, `revoked_at`.
- Propietario: Edge Function. Rotación de tokens; reutilización de refresh → revocar (Supabase ya aplica reuse detection `[DOC]`).

### Relaciones y seguridad (`[INF]` salvo `[DOC]`)
- `subscriptions.user_id` y `entitlements.user_id`, `sessions.user_id` SIEMPRE = `auth.uid()` en RLS impuesto por Supabase.
- Clave única `subscriptions.purchase_token` para idempotencia (evita dobles grants).
- `entitlements.status` solo cambia desde Edge Function tras verificar contra Play (`subscriptionsv2.get`), nunca por PostgREST público.

## 4. Integración de Google Play Billing (`[DOC]` investigado 16-ago-2026)

> Actualización crítica respecto a `SECURITY_MODEL.md` v1: la API
> `purchases.subscriptions.get` (v3) está **DEPRECADA**; el endpoint oficial
> actual es `purchases.subscriptionsv2.get`.

Flujo conceptual (sin implementar):

```
USER → GOOGLE PLAY checkout → purchase token
APP  → POST /verify-purchase {purchaseToken, productId}  (Edge Function, autenticada)
EDGE → Google Play Developer API: purchases.subscriptionsv2.get
       (OAuth service account JWT, scope androidpublisher)
EDGE → adjudica entitlement + acknowledge
EDGE → Grant/update entitlement en Supabase
APP  → entitlement activo (cache firmado, TTL)
```

### 4.1 Quién verifica: el backend (Edge Function), NUNCA el cliente
- El `purchaseToken` lo genera Play; el backend lo **re-verifica** llamando
  `subscriptionsv2.get`. (`[DOC]` security + lifecycle: "server-side verification").
- Idempotencia: `purchase_token` como PK. Rechazar tokens ya vistos.
- **Acknowledgment**: el backend llama `acknowledge` (o lo completa vía API del
  publisher) dentro de la ventana (3 días prod) para evitar auto-refund
  (`[DOC]` security).

### 4.2 Dónde se verifica
En la Edge Function `verify-purchase` (entorno server-side con el secreto del
service account). Solo URL+publishable key en el APK.

### 4.3 Dónde se guarda el estado
En Supabase: `subscriptions` (operativo) + `entitlements` (derivado, fuente para
el TTL del caché) + `sessions`/`devices` (seguridad).

### 4.4 Cómo se actualiza
- **RTDN (Pub/Sub)** → Edge Function `on-play-rtdn`. El payload trae
  `subscriptionNotification` con `notificationType` y `purchaseToken`. **Regla de
  oro**: RTDN es una **señal**, no el estado; el handler **SIEMPRE consulta
  `subscriptionsv2.get`** antes de mutar (`[DOC]` lifecycle). Dedupe por
  `messageId` (Pub/Sub es at-least-once). Verificar autenticación del push: JWT
  OIDC de Pub/Sub en el header Authorization `[DOC]`.
- Verificación síncrona on-demand al abrir la app / gateway de feature premium.
- Confirmación de renovación por consulta al estado (`subscriptionState` +
  `expiry_time`), no por confiar en el notificationType.

### 4.5 Renovación
Auto-renew lo gestiona Play; RTDN `RENEWED` dispara re-consulta; el backend
actualiza `expiry_time` y firma un nuevo entitlement. No se programan polls por
`expiryTime` (RTDN cubre; la API es fuente de verdad `[INF]`).

### 4.6 Cancelación
RTDN `CANCELED`/`EXPIRING` → plan sigue hasta `expiry_time` (Play valida);
`EXPIRED` → revocar entitlement ya.

### 4.7 Expiración
`EXPIRED`/`expiry_time` pasado → entitlement revocado (ONLINE inmediato, TTL en cache). El token sigue siendo válido para consultas hasta 60 días post-expiración (`[DOC Play]`), útil para restore/re-engage.

### 4.8 Grace period
`IN_GRACE_PERIOD`: el usuario conserva **entitlement** (espacio de pago). Backend: mantener acceso, avisar en UI. Total gracia + account hold ≥30 días (`[DOC]` play-help).

### 4.9 Account hold
`ON_HOLD`: **sin** entitlement. Backend: bloquear premium (`subscriptionState == ON_HOLD`), conceder recuperación por `RECOVERED`.

### 4.10 Refund
- RTDN `voidedPurchaseNotification` (full/partial) → revocar + marcar `revoked`.
- Voided Purchases API (pull) como segundo canal (requiere "View financial reports").
- `PendingRefundReviewNotification` → respuesta `Orders.reviewrefund` (`[DOC]`).

### 4.11 Cambio de plan
- `linkedPurchaseToken` presente → invalidar token anterior / entitlement antiguo.
- Modos: prorrateo / full / sin prorrateo / diferido (downgrade). Diferido: el
  switch ocurre en la siguiente renovación (RTDN del token nuevo + EXPIRED del
  viejo). La cadena de tokens vía `linkedPurchaseToken` da el historial de upgrades.

### 4.12 Restore purchase
- Cliente: `queryPurchasesAsync`.
- Backend re-verifica tokens guardados (llamando `subscriptionsv2.get`) y re-emite
  entitlement. `queryPurchaseHistory` deprecado en PBL 7 (`[DOC]`); usar el
  catálogo actual + tokens persistidos.

### 4.13 Billing alterno / user choice
- Hoy: MX no elegible (Rest of World llega 2027). Sin impacto en SIRC ahora
  (`[INF]`). Solo documentado por si en el futuro se monetiza en regiones
  elegibles.

## 5. Play Integrity como señal (mantiene decisión del gate)

Play Integrity **no es la barrera de suscripción**. Es una señal de entorno que
el backend combina con entitlement y Play Billing (§4 SECURITY_MODEL):

- `appIntegrity.appRecognitionVerdict`: detecta repackage/builds no reconocidas.
- `accountDetails.appLicensingVerdict == LICENSED`: instalación de Play legítima (no es suscripción activa).
- `deviceIntegrity`: tiered enforcement (no penalizar BASIC/root a priori).
- `requestHash` para ligar contenido y `nonce` server-side en acciones de alto valor (S1/S2 del SECURITY_MODEL).

Uso en Supabase: la Edge Function que recibe Integrity verdict lo valida y
responde SOLO si `appRecognition` + `licensing` son coherentes, manteniendo los
veredictos fuera del control del cliente. No cachear veredictos (`[DOC]`).

## 6. Source of truth (qué significa aquí)

| Capa | Rol |
|---|---|
| **Google Play** | Fuente de verdad de la **transacción** (token, estado, renovación, refund). |
| **Backend (Edge Functions)** | Fuente de verdad **operativa** del entitlement: adjudica/revoca combinando Play API + la política de TTL/plan de SIRC. |
| **Supabase** | **Persistencia/servicio** de identidad, suscripción y entitlement (Auth + RLS + Postgres + funciones). |
| **Cliente (APK)** | **Caché local** del estado autorizado; nunca autoridad. Su "entitlement" es un reflejo firmado con TTL, siempre re-validable. |

`[INF]` — la doc de Supabase y el ecosistema (WatermelonDB/RxDB/PowerSync) usan
el patrón "Postgres = database of record; cliente = caché"; para SIRC ese patrón
aplica a identidad/entitlement y es coherente con R9e (nunca ofertas en servidor).

## 7. Offline (mantiene §6 de SECURITY_MODEL)

- Entitlement cacheado, **cifrado y firmado por el servidor** (HMAC corto) con
  **TTL 24–72 h** (decisión S2).
- **Nunca** permitir `ENTITLEMENT CACHEADO + SIN INTERNET → PREMIUM INDEFINIDO`:
  el TTL caduca y al volver online se revalida (`subscriptionsv2.get`).
- **Reloj manipulado**: la lógica de TTL compara contra `serverUtc` (recuperado
  en la verificación), nunca contra el reloj local como autoridad.
- **Reinstalación / cambio de dispositivo**: restore de compra (4.12) +
  `devices`/`sessions` (incluye device binding opcional T6/T18).
- **Logout/login**: sesión corta, revocación por logout; el caché de entitlement
  se limpia al logout.

## 8. Seguridad contra APK modificado/repackaged (T15–T20)

Mitigaciones por capas (detalle en `SECURITY_MODEL.md` §3 ampliado):

| Capa | Mitigación |
|---|---|
| **Play Integrity** | Detectar repackage (T15/T18), app no reconocida, tiers de device (señal, no barrera). |
| **Backend/Entitlement** | Entitlement verificado server-side; revocación por RTDN; TTL corto; `purchase_token` PK idempotente. |
| **Play Billing** | Verificación con `subscriptionsv2.get`; acknowledge; gestión de refund. |
| **Keystore** | Cache de entitlement cifrado + firma server; secrets del backend NUNCA en APK. |
| **Tokens/sesiones** | Session cortas, rotación, reuse detection (revocación de refresh reutilizado), binding a install. |
| **Rate limiting** | Abuso de endpoints (T19) mitigado en Edge Function/edge. |
| **Device binding** | `devices.install_id` vinculado a sesión (opcional, no romper UX offline). |

**Promesa honesta (T-prologue)**: ninguna capa aísla el fraude 100 %. El
objetivo es elevar el costo de manipular y evitar premium indefinido.

## 9. Privacidad (mantiene §9 de SECURITY_MODEL)

- Las **ofertas capturadas NO se envían a Supabase** (ni por defecto ni por
  opción de usuario salvo justificación futura).
- El backend conoce SOLO: identidad, cuenta, suscripción, entitlement,
  dispositivos/sesiones, integridad y datos de soporte.
- No subir capturas, OCR, historial ni rutas por defecto. Cualquier futura
  sincronización de datos de conductor debe justificarse por una decisión de
  producto explícita.
- Todo dato del backend tiene TTL/borrado y auditoría (regla R9g).

## 10. Qué NO es esto

- No es el pipeline de análisis (ese es local).
- No es autenticación de la de la app de la plataforma (Uber/DiDi): SIRC nunca
  tiene credenciales del conductor.
- No es un backend de datos de viajes.
- No es una promesa de que "Supabase resolverá la piratería": es una pieza cuyo
  trabajo real es ejecutar la verificación de Play y almacenar el mínimo
  necesario, correctamente protegido con RLS/secretos.
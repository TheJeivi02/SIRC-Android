# SIRC — Modelo de Suscripción (planes y entitlement conceptual)

> Estructura conceptual de planes, precios de referencia y lifecycle de
> entitlement de SIRC (LOOP ENGINEERING — Backend Supabase, 16-ago-2026;
> **actualizado por el LOOP Modelo Free**, 16-ago-2026; **revisado y definido
> por el LOOP Modelo Comercial — TRIAL + SUSCRIPCIÓN**, 16-ago-2026).
>
> **ESTADO: diseño SOLO.** No se definen precios finales ni se crean productos
> en Play. Sin asociación de planes reales todavía. Referencia de precios de
> mercado verificada (matriz de competidores) y este documento.
>
> Regla: **no crear planes artificiales**; cada nivel debe corresponder a valor
> real para un conductor. El precio final considerará mercado, poder adquisitivo
> objetivo, competencia, costo de infraestructura, comisión de Google Play,
> margen y conversión (ver §5bis Matriz de decisión de precio).

## 1. Modelo comercial definitivo: DESCARGA GRATUITA → TRIAL 14 DÍAS → SUSCRIPCIÓN

**DECISIÓN (D16.1)**: SIRC es una aplicación de:

- **descarga gratuita**;
- **registro de cuenta gratuito**;
- **acceso inicial completamente gratuito**;
- **prueba Premium completa de 14 días**;
- posteriormente **basada en suscripción** (plans semanal, mensual y anual).

No existe inicialmente un "Free permanente" con una versión reducida del
producto. Flujo:

```
FREE DOWNLOAD
   ↓
CREACIÓN DE CUENTA
   ↓
14 DÍAS DE TRIAL PREMIUM COMPLETO  (todas las funciones disponibles)
   ↓
FIN DEL TRIAL
   ↓
FUNCIONES PREMIUM DESACTIVADAS
   ↓
PAYWALL / SUSCRIPCIÓN  (SEMANA / MES / AÑO)
   ↓
TODAS LAS FUNCIONES PREMIUM RESTAURADAS
```

Reemplaza el modelo anterior `FREE_INITIAL_MODEL = ENABLED` /
`FREE_LIMITS = TBD`. Definición correcta:

```
FREE_TRIAL = 14 DAYS
TRIAL_ACCESS = FULL_PREMIUM
POST_TRIAL = SUBSCRIPTION_REQUIRED
```

### 1.1 Objetivos del trial (adquisición + validación)

El trial de 14 días tiene dos objetivos simultáneos: **adquisición** (que el
conductor experimente el producto completo sin pagar) y **validación** (usuarios
reales que permitan detectar errores, validar OCR/captura/rendimiento, detectar
problemas por plataforma, obtener feedback, observar comportamiento, mejorar UX
e identificar funciones de mayor valor).

Estrategia buscada:

```
PRODUCTO ÚTIL → PRUEBA COMPLETA → VALOR PERCIBIDO → RECOMENDACIÓN
→ NUEVOS USUARIOS → MÁS DATOS/FEEDBACK → MEJOR PRODUCTO → CONVERSIÓN A PREMIUM
```

La comunicación favorece el crecimiento orgánico y boca a boca, sin técnicas
engañosas ni spam.

### 1.2 La cuenta SIRC (obligatoria; D16.6)

El usuario debe crear una cuenta para usar SIRC. La cuenta permite: identificar
al usuario, asociar el trial, asociar compras, restaurar entitlement, sincronizar
estado, controlar abuso (anti-trial), obtener feedback y dar continuidad entre
dispositivos. La cuenta **no** ralentiza el análisis de ofertas (LOCAL-FIRST;
regla 9e; objetivo de decisión <1 s, §8).

### 1.3 Trial anti-abuso

El trial de 14 días se asocia **principalmente a la cuenta**; el backend determina
`trial_start`, `trial_end` y `trial_status`. No se confía exclusivamente en fecha
local, almacenamiento local, instalación ni identificadores manipulables. El
modelo anti-abuso contempla reinstalación, borrado de datos, cambio de
dispositivo, múltiples cuentas y reloj manipulado, **sin bloquear usuarios
legítimos innecesariamente** (seguridad equilibrada con UX). Detalle en
`SECURITY_MODEL.md` §6.1bis.

## 2. Estructura conceptual de planes (tres periodicidades)

Propuesta alineada con el modelo Trial→Suscripción (decisión <1 s local, solo
lectura, overlay). Tres periodicidades de suscripción Premium (D16.3):

| Nivel | Qué puede hacer el usuario | Objetivo principal | Estado |
|---|---|---|---|
| **Trial Premium (14 días)** | **Acceso completo a todas las funciones Premium** durante 14 días tras el registro. No requiere pago. | **Adquisición + validación**: probar el producto completo, feedback, recomendación, conversión. | `TRIAL_ACTIVE` (entitlement completo, server-side). |
| **SIRC Weekly** (suscripción semanal) | Funciones Premium completas (overlay + evaluación + multi-plataforma + umbrales dinámicos + nocturno + AHU + SOC…). | Usuarios que quieren probar Premium tras el trial; uso ocasional; menor barrera inicial. | Entitlement `PREMIUM_ACTIVE` (server-side). |
| **SIRC Monthly** (suscripción mensual) | ídem Premium completo. | **Opción principal**: equilibrio entre precio y compromiso. | Entitlement `PREMIUM_ACTIVE` (server-side). |
| **SIRC Annual** (suscripción anual) | ídem Premium completo. | Usuarios recurrentes; **mejor valor por período**; mayor retención. Debe **presentar claramente el ahorro** respecto al pago mensual. | Entitlement `PREMIUM_ACTIVE` (server-side). |
| **Futuro** | Ecosistema Lite/Pro compartido; alertas anti-fatiga; niveles intermedios si el mercado lo exige. | Profesionalización y retención (E3/E4). | Requiere producto evolucionado (E3/E4). |

> Se **retiran del modelo activo** los niveles "SIRC FREE permanente" y
> "Basic/Pro" del diseño anterior: el arranque es **Trial completo → suscripción
> Weekly/Monthly/Annual**. No se definen todavía descuentos concretos de la
> versión anual (se presentará el ahorro al fijar pricing; decisión posterior).

### Qué hace que el usuario pague (evidencia de mercado)
- El valor central es **decidir antes de aceptar** (semáforo <1 s, $/km + $/hora,
  lucro neto). Ese valor es lo que encapsulará la **suscripción Premium**
  (Weekly/Monthly/Annual).
- Lo que mejor monetiza en el nicho (evidencia de `PRODUCT_STRATEGY.md`): el
  overlay + métricas duales + lucro neto; el auto-aceptar (multi-app) es el
  diferenciador de precio de competidores con automatización — **SIRC NO lo
  ofrecerá** (regla R9b).

## 2bis. Precios de referencia (competencia verificada, 16-ago-2026)

Referencias competitivas **documentadas, NO precios de SIRC** (los precios SIRC
no se fijan todavía; ver §2ter y §5bis). Conversión a USD **solo para análisis
comparativo**; no se inventan precios de otros países. Formato: `VERIFIED`
(fecha) · `SOURCE`; sin precio público → `PRICE NOT PUBLICLY DISCLOSED`. Detalle
completo y fuentes en `docs/PRODUCT_COMPETITIVE_ANALYSIS.md`.

| Producto | Plan / Periodicidad | Precio | Región | Trial | Verificado (fecha) |
|---|---|---|---|---|---|
| Motorista One | Mensual | R$29,90 (~USD 5,5) | BR | — | VERIFIED (16-ago-2026) |
| Motorista One | Anual | R$169,90 (~USD 31) | BR | — | VERIFIED (16-ago-2026) |
| GigU | Monthly | USD 6,95 | Global/US + BR | Prueba inicial | VERIFIED (16-ago-2026) |
| GigU | Anual | USD 49,95 (~USD 4,2/mes) | Global/US | — | VERIFIED (16-ago-2026) |
| DecideRider | Mensual | CLP $3.490 (~USD 3,6) | Global/LATAM | **~14 días** | VERIFIED (16-ago-2026) |
| Ruta Rentable | — | `PRICE NOT PUBLICLY DISCLOSED` | LATAM | **~3 días (in-app)** | VERIFIED (16-ago-2026) |
| Maxymo | Mensual | USD 4,99 | Global | — | VERIFIED (16-ago-2026) |
| Mystro | Mensual | USD 18,99 | US | — | VERIFIED (16-ago-2026) |
| Mystro | Anual | USD 139,99 | US | — | VERIFIED (16-ago-2026) |
| Viaje Rentable | Mensual | USD 4,99 (consumible/Play) | AR | freemium | VERIFIED (16-ago-2026) |
| Operdrive | Mensual / Anual | R$19,90 / R$199,00 | BR | — | VERIFIED (16-ago-2026) |

**Rango típico del nicho:** ~USD 2,40–6,95/mes en LATAM/BR; USD 4,99–18,99/mes
en EE.UU. Pruebas gratuitas observadas: **3 días (Ruta Rentable)**, **14 días
(DecideRider)**. Referencia `[INF]`, no precios SIRC.

### Cómo se incorporará este análisis (sin inventar)
1. Convalidar con datos de la fase trial/beta (adquisición, uso real,
   conversión) y con investigaciones posteriores antes de fijar precios.
2. Definir precio inicial en **USD** y periodicidades (weekly/monthly/annual)
   mediante la matriz de decisión (§5bis); Play muestra el precio regional.
3. Publicar planes en Play Console (BasePlans/Offers) cuando exista monetización;
   desde Supabase se mapeará `plan_id` ↔ `basePlanId_play`/`offerId_play`.

## 2ter. Moneda y precios SIRC (D16.4): USD como referencia + regionalización Play

SIRC usa una **referencia internacional en USD**:

```
SIRC PRICE DEFINITION → USD BASE PRICE → GOOGLE PLAY REGIONAL PRICING → MONEDA LOCAL
```

- Los precios internos del producto se definen **inicialmente en USD** y se
  muestran al usuario en la moneda correspondiente cuando Google Play localice
  la oferta (Ecuador puede ver USD; otros países su moneda local).
- **NO crear conversiones manuales dentro de la app.** NO usar el tipo de cambio
  del dispositivo ni el reloj local para determinar precios. **El precio mostrado
  por Google Play es la autoridad comercial final** para la compra.
- **NO se fijan todavía los valores definitivos** de los planes semanales/
  mensuales/anuales (la estrategia de pricing requiere validación con datos de
  la fase trial). Queda documentado que: existe precio base USD, existen tres
  periodicidades, Google Play maneja la presentación regional y el precio podrá
  evolucionar conforme aumente el valor del producto (§5).

## 3. Entitlement (política de acceso)

El entitlement es el **permiso derivado**: qué features tiene el usuario AHORA
(`TRIAL_ACTIVE`, `PREMIUM_ACTIVE`, etc.). No es la suscripción en bruto (Play),
es el estado que el backend deriva de ella.

### 3.1 Estados conceptuales (D16)

| Estado | Significado | Acceso a features Premium |
|---|---|---|
| `TRIAL_ACTIVE` | Trial Premium completo de 14 días en curso | **Todas** (overlay + evaluación completas). |
| `TRIAL_EXPIRED` | Trial finalizado sin suscripción | **Bloqueado** → paywall (flujo local, sin espera de red). |
| `PREMIUM_ACTIVE` | Suscripción vigente (weekly/monthly/annual) | **Todas**. |
| `SUBSCRIPTION_EXPIRED` | Suscripción expirada/cancelada sin renovación | **Bloqueado** → paywall. |
| `ACCOUNT_RESTRICTED` | Cuenta suspendida/limitada por abuso o seguridad | **Bloqueado** (no Premium). |
| `ACCOUNT_UNKNOWN` | Estado no verificado (sin red, sesión invalidada, error) | **Bloqueado** hasta revalidación (offline seguro: no otorgar Premium). |

El entitlement es **server-authoritative** (D15.3/9j, `SECURITY_MODEL.md`): la
app **no decide por sí sola que un usuario tiene Premium indefinidamente**. El
cliente solo puede usar caché firmada/cifrada para operar **offline** con las
reglas de TTL existentes (`SECURITY_MODEL.md` §6).

### 3.2 Features y gate

| Feature | Plan mínimo | Gate |
|---|---|---|
| Overlay + evaluación completa (núcleo) | `TRIAL_ACTIVE` o `PREMIUM_ACTIVE` | `EntitlementRepository` (server + caché TTL firmado) |
| Multi-plataforma (DiDi/InDrive/Cabify) | ídem | ídem |
| Umbrales dinámicos / modo nocturno / AHU / SOC | ídem | ídem |

### 3.3 Reglas del entitlement

- **El cliente no decide entitlement.** El gate consulta `EntitlementRepository`
  con estado verificado (online) o caché firmado offline (TTL). El cliente
  **nunca** es autoridad sobre Premium.
- El caché lleva `server_issued_at`/`server_expires_at` (decisión S2: TTL 24–72 h)
  explicado en `SECURITY_MODEL.md` §6.
- Cambios de plan (trial→suscripción, weekly→monthly, renovación, refund) se
  reflejan vía RTDN + verificación `purchases.subscriptionsv2.get`
  (`BACKEND_ARCHITECTURE.md` §4). El trial es un estado de cuenta gestionado por
  el backend (`trial_start`/`trial_end`/`trial_status`), no un estado de Play.
- **Paywall sin espera en el camino crítico**: si el estado es
  `TRIAL_EXPIRED`/`SUBSCRIPTION_EXPIRED`/`ACCOUNT_RESTRICTED`/`ACCOUNT_UNKNOWN`,
  la experiencia de bloqueo es **local y rápida** (el entitlement ya está
  disponible por la arquitectura definida; NO se hace `OCR → backend → esperar →
  mostrar` para decidir que el usuario no tiene Premium). Objetivo de decisión
  `<1 s` cuando el usuario tiene acceso (ver §8).

## 4. Lifecycle de estados (entitlement ↔ Play)

Mapeo del entitlement de SIRC con el `SubscriptionState` moderno de Play
(API `subscriptionsv2`, 2026):

| Estado SIRC en UI | Estado Play (source of truth) | Acción backend | Qué ve el usuario |
|---|---|---|---|
| `TRIAL_ACTIVE` | — (estado de **cuenta**, no de Play) | Backend adjudica trial completo 14 días (`trial_start/end/status`); caché firmado TTL | Todo Premium activo |
| `TRIAL_EXPIRED` | — (cuenta) | Backend revoca acceso; paywall local | "Tu prueba ha terminado" → suscripción |
| `PREMIUM_ACTIVE` | `ACTIVE` | Entitlement activo (TTL) | Premium |
| `GRACE` | `IN_GRACE_PERIOD` | Mantener acceso; aviso de pago pendiente | Premium sigue, con aviso |
| `RECOVERED` | `RECOVERED` (mismo token) | Restaurar entitlement | Premium de nuevo |
| `ON_HOLD` | `ON_HOLD` | **Bloquear** premium | "Pago pendiente / cuenta en espera" |
| `PAUSED` | `PAUSED` (pendiente renew) | Bloquear | "Pausado" |
| `CANCELED` | `CANCELED` (billing ends) | Entitlement hasta `expiry_time`; luego revocar | "Se cancela al final del periodo" |
| `SUBSCRIPTION_EXPIRED` | `EXPIRED` | **Revocar** | "Suscripción vencida" |
| `PENDING_PURCHASE` | `PENDING` | No otorgar aún | "Esperando confirmación de pago" |
| `REVOKED/REFUNDED` | `REVOKED`/voided | **Revocar + clawback** | "Suspendida" |
| `ACCOUNT_RESTRICTED` | — (cuenta) | Bloquear por abuso/seguridad | "Cuenta restringida" |
| `ACCOUNT_UNKNOWN` | — (cuenta) | Sin verificar → bloquear hasta revalidar | "Sesión no verificada" |

> Nota de terminología: en la API v2 **no existen** estados `GRACE_PERIOD`/
> `ACCOUNT_HOLD` / `DISMISSED` como propios: son `IN_GRACE_PERIOD` y `ON_HOLD`
> (`[DOC]` API subscriptionsv2). Ajuste sobre la tabla del `SECURITY_MODEL.md`
> v1.

### Acciones del backend por evento RTDN (resumen)
| evento | acción |
|---|---|
| `SUBSCRIPTION_PURCHASED` → nuevo token | verificar + grant + acknowledge |
| `RENEWED` | actualizar `expiry_time`, re-firmar TTL |
| `RECOVERED` | restaurar (mismo token) |
| `IN_GRACE_PERIOD` | mantener acceso |
| `ON_HOLD` | bloquear |
| `CANCELED` / `EXPIRING` | hasta `expiry_time` |
| `EXPIRED` | revocar |
| `REVOKED` / `voidedPurchaseNotification` | revocar + clawback |
| `ITEM_CHANGED` (plan change) | `linkedPurchaseToken` → invalidar antiguo |
| `PENDING_PURCHASE_CANCELED` | sin grant; revisar linkage |

## 5. Cambio de plan y restore

- **Upgrade/downgrade**: crea nuevo token; `linkedPurchaseToken` apunta al viejo
  → invalidar token antiguo para no duplicar entitlement. Downgrade diferido
  (DEFERRED): switch en la siguiente renovación.
- **Restore purchase**: `queryPurchasesAsync` + re-verificación backend
  (`subscriptionsv2.get`). Olvidado: el caché stale se descarta y se vuelve a
  comprobar.
- **Multi-dispositivo / reinstalación**: restore cubre re-enlace; `sessions`/
  `devices` dan trazabilidad (mitigación T6/T18).

## 5bis. Estrategia de pricing evolutivo (D16.5)

SIRC **no intenta maximizar el precio desde el lanzamiento**. La evolución del
precio se justifica por **evolución real del producto** (no por el paso del
tiempo). Fases conceptuales:

| Fase | Valor agregado al producto | Precio |
|---|---|---|
| 1 | Producto inicial | **Precio de entrada competitivo** |
| 2 | Más plataformas | mayor valor |
| 3 | Modo nocturno | mayor valor |
| 4 | AHU / análisis avanzado | mayor valor |
| 5 | Funciones financieras | mayor valor |
| 6 | IA / funciones avanzadas | mayor valor |

Reglas:
- **NO aumentar precios simplemente porque pasó el tiempo.** Solo cuando se
  agreguen capacidades que mejoren significativamente el resultado económico o
  la experiencia del conductor.
- **Grandfathering**: los usuarios existentes deben recibir una política de
  protección de precio, **definida posteriormente antes de implementar billing**
  (no se inventa ahora la política exacta).

### Matriz de decisión del precio inicial en USD (§20 del LOOP)

Permite decidir posteriormente el precio inicial **sin fijarlo todavía**:

| Criterio | Consideración para el precio |
|---|---|
| Costo competitivo | Rango del nicho (USD 2,40–18,99/mes según región y automatización) |
| Valor funcional | Overlay <1 s, lucro neto, $/h y $/km, solo lectura |
| Cantidad de plataformas | 1 (Uber) → 4 (Uber/DiDi/InDrive/Cabify) |
| Velocidad | Decisión <1 s (objetivo UX), <3 s (límite técnico E2E) |
| Overlay | Contributor principal de valor |
| Funciones financieras | AHU/tendencias (E3) → mayor valor |
| Funciones Premium futuras | Nocturno, SOC, anti-fatiga |
| Costo de infraestructura | Supabase (plan Pro en producción) |
| Comisión de Google Play | ~15–30 % sobre suscripción (por configurar al fijar precio) |
| Conversión esperada | Dato de la fase trial (14 días) |
| Retención | Dato de la fase trial; anual mejora retención |
| Feedback de usuarios | Señal de la fase trial (reporte de errores/valor percibido) |

**Objetivo de pricing:** no ser necesariamente el más barato; **mejor relación
valor/precio para el conductor**.

## 6. Decisiones de diseño (registradas en `.ai/DECISIONS.md`)

- **D14.1 — Supabase como backend inicial** de identidad/suscripción/entitlement
  (Auth + RLS + Edge Functions + Postgres), plan Pro antes de producción.
- **D14.3 — Entitlement TTL 24–72 h** (mantiene decisión S2); `subscriptionsv2.get`
  (no `subscriptions.get`, deprecado); integración RTDN + verificación on-demand.
- **D15.1 — Descarga gratuita + cuenta gratuita** para adquisición/validación
  (base del modelo Trial→Suscripción posterior).
- **D15.3 — El entitlement NO relaja la seguridad**: server-side, revocable, sin
  premium indefinido por manipulación local.
- **D16.1 — Modelo comercial Trial → Premium** (descarga/cuenta gratuitas +
  trial 14 días completo + suscripción): `FREE_TRIAL=14 DAYS`,
  `TRIAL_ACCESS=FULL_PREMIUM`, `POST_TRIAL=SUBSCRIPTION_REQUIRED`.
- **D16.2 — Trial Premium completo de 14 días** (adquisición + validación).
- **D16.3 — Suscripciones Weekly / Monthly / Annual** (tres periodicidades;
  anual con ahorro claro; descuentos concretos por decisión posterior).
- **D16.4 — USD como referencia de pricing + regionalización por Google Play**
  (sin conversión manual, sin reloj local; Play = autoridad comercial).
- **D16.5 — Pricing evolutivo ligado al valor agregado** + grandfathering.
- **D16.6 — Cuenta obligatoria** para controlar trial y entitlement.
- Planes activos: **Trial 14 días → Weekly/Monthly/Annual** (sin Free
  permanente).

## 7. NO implementar (riesgo de confusión)

Este documento **no** crea: productos en Play, precio en la UI, planes en
Supabase, tablas, `EntitlementRepository` de producción, banner de suscripción,
paywall ni ningún gate premium. El **trial**, la **cuenta**, el **backend**,
**Billing** y el **entitlement runtime** tampoco se implementan todavía:
pertenecen a E1b (cuenta + Supabase + Billing + entitlement + trial) y requieren
abrir tarea explícita (regla 9f/R16). E1a (validación del núcleo) precede a
E1b: **no adelantar la monetización si E1a aún no está validado**.
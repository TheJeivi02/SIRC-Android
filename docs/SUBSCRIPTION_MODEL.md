# SIRC — Modelo de Suscripción (planes y entitlement conceptual)

> Estructura conceptual de planes, precios de referencia y lifecycle de
> entitlement de SIRC (LOOP ENGINEERING — Backend Supabase, 16-ago-2026),
> **actualizada por el LOOP Modelo Free + Supabase Account Gate** (16-ago-2026).
>
> **ESTADO: diseño SOLO.** No se definen precios finales ni se crean productos
> en Play. Sin asociación de planes reales todavía. Referencia de precios de
> mercado verificada (matriz de competidores) y este documento.
>
> Regla: **no crear planes artificiales**; cada nivel debe corresponder a valor
> real para un conductor. El precio final considerará mercado, poder adquisitivo
> objetivo, competencia, costo de infraestructura, margen y conversión.

## 1. Modelo comercial inicial: DESCARGA GRATUITA + PLAN FREE

**DECISIÓN (D15.1)**: SIRC se distribuye **gratuitamente** (sin precio de
descarga) y arranca con un nivel **SIRC FREE** basado en cuenta. **La
monetización Premium es posterior y progresiva** (E3), no inmediata.

Objetivos de la fase inicial (en orden):

1. Conseguir usuarios.     6. Validar rendimiento.
2. Conseguir datos de uso. 7. Validar UX.
3. Detectar errores reales. 8. Obtener feedback.
4. Validar OCR.             9. Mejorar el producto.
5. Validar captura.        10. Construir base real antes de monetizar.

La **cuenta SIRC** (FREE) sirve para: identificar al usuario, sincronizar
configuración cuando corresponda, gestionar entitlement, soporte, recuperación
de cuenta, futuras suscripciones y **métricas agregadas estrictamente
necesarias**. La cuenta **no** es dependencia del camino crítico de análisis de
ofertas (LOCAL-FIRST; regla 9e).

> **¿Existe límite en el Free?** `FREE_INITIAL_MODEL = ENABLED`, `FREE_LIMITS =
> TBD`. La interpretación del requisito ("dos 3 free") no es clara —puede ser 3
> días, 3 meses, 3 funciones, 3 análisis, 3 plataformas u otro—. **NO se inventa
> ningún límite aquí**: se definirá con una decisión explícita posterior,
> basada en datos de la beta. (Decisión D15.2.)

## 2. Estructura conceptual de planes

Propuesta en **dos niveles + futuro**, alineada con la fase de adquisición y los
pilares del producto (decisión <3 s local, solo lectura, overlay).

| Nivel | Qué puede hacer el usuario | Objetivo principal | Estado |
|---|---|---|---|
| **SIRC FREE** | Descarga gratuita + cuenta gratuita. Uso del núcleo (overlay, evaluación, historial) con alcance aún por definir (`FREE_LIMITS = TBD`). | **Adquisición, beta pública/controlada, pruebas reales, feedback, detección de errores, validación de funcionalidades, crecimiento inicial.** | **Es el modelo de arranque.** Entitlement = `FREE` (server-side, no "todo desbloqueado localmente"). |
| **SIRC PRO** (suscripción) | Features premium completas (alcance aún TBD; núcleo completo, multi-plataforma, umbrales dinámicos, nocturno, AHU, SOC). | Monetización **posterior y progresiva** (E3), sobre una base de usuarios que ya validó el valor. | Entitlement = `PREMIUM`, verificada server-side. Precios/límites **TBD**. |
| **Futuro** | Ecosistema Lite/Pro compartido; alertas anti-fatiga; niveles intermedios si el mercado lo exige. | Profesionalización y retención. | Requiere producto evolucionado (E3/E4). |

> Se **retiran del modelo activo** los roles intermedios "Basic/Pro" del diseño
> anterior: ahora el arranque es **FREE → PREMIUM** y cualquier nivel adicional
> queda **TBD** (no fijar precios, límites, cantidad de análisis, duración ni
> número de plataformas hasta completar la investigación comercial).

### SIRC FREE — matices importantes
- La **descarga es gratuita** y **no hay precio inicial**. La existencia de una
  cuenta **no** implica pago.
- El objetivo del Free **no es maximizar ingreso inmediato** sino adquirir
  usuarios y validar el producto.
- **Seguridad del Free (regla)**: el Free NO es "todo desbloqueado localmente y
  escondemos botones". Aunque sea gratuito, las capacidades premium se diseñan
  para: validarse, respaldarse server-side, no otorgar premium indefinido por
  manipulación local, usar Play Integrity como señal, poder revocarse desde el
  backend y gestionarse por cuenta (ver `SECURITY_MODEL.md` §5.5 y
  `BACKEND_ARCHITECTURE.md` §3).
- El entitlement FREE se sirve igual que el premium (server + caché TTL), con
  el plan `free` en la base (no solo lógica local).

### Qué hace que el usuario pague (evidencia de mercado)
- El valor central es **decidir antes de aceptar** (semáforo <1 s, $/km + $/hora,
  lucro neto). Ese valor es lo que encapsulará **SIRC PRO**.
- Lo que mejor monetiza en el nicho (evidencia de `PRODUCT_STRATEGY.md`): el
  overlay + métricas duales + lucro neto; el auto-aceptar (multi-app) es el
  diferenciador de precio de competidores con automatización — **SIRC NO lo
  ofrecerá** (regla R9b).

## 2bis. Precios de referencia (competencia verificada, 16-ago-2026)

Matriz resumida (detalle y fuentes en `docs/PRODUCT_STRATEGY.md` §precios):

| Producto | Plan | Precio | Periodicidad | Región | Verificado |
|---|---|---|---|---|---|
| Motorista One | Mensual | R$29,90 (~USD 5,5) | /mes | BR | ✅ |
| Motorista One | Semestral | R$109,90 (~USD 20) | /6 meses | BR | ✅ |
| Motorista One | Anual | R$169,90 (~USD 31) | /año | BR | ✅ |
| GigU | Monthly | USD 6,95 | /mes | Global/US + BR | ✅ |
| GigU | Anual | USD 49,95 (~USD 4,2/mes) | /año | Global/US | ✅ |
| GigU (BR) | Mensual | R$12,90 (~USD 2,4) | /mes | BR | ✅ (precio regional publicado) |
| Maxymo | Mensual | USD 4,99 | /mes | Global | ✅ (IAP publicado) |
| Mystro | Mensual | USD 18,99 | /mes | US | ✅ |
| Mystro | Anual | USD 139,99 | /año | US | ✅ |
| Viaje Rentable | Mensual | USD 4,99 (consumible/Play) | /mes | AR | ✅ (freemium + pago) |
| Operdrive | Mensual | R$19,90 | /mes | BR | ✅ |
| Operdrive | Anual | R$199,00 | /año | BR | ✅ |
| Ruta Rentable | — | **no publicado** (solo in-app; prueba 3 días) | — | LATAM | ⚠️/❌ |

**Rango típico del nicho:** ~USD 2,40–6,95/mes en LATAM/BR; USD 4,99–18,99/mes
en EE.UU. Punto medio competitivo razonable para SIRC: **USD 4–7/mes** con
descuento anual (~USD 2,5–4,2/mes) y prueba gratuita 3–15 días `[INF]`.

### Cómo se incorporará este análisis (sin inventar)
1. Convalidar con datos de la fase FREE/beta (adquisición, uso real) y con
   investigaciones posteriores antes de fijar precios.
2. Definir precio final **en E3** (monetización) con estos rangos como informed
   prior. En la fase inicial (E1a/Free) **no hay cobro**.
3. Publicar planes en Play Console (BasePlans/Offers) cuando exista monetización;
   desde Supabase se mapeará `plan_id` ↔ `basePlanId_play`/`offerId_play`.

## 3. Entitlement (política de acceso)

El entitlement es el **permiso derivado**: qué features tiene el usuario AHORA
(FREE o PREMIUM). No es la suscripción en bruto (Play), es el estado que el
backend deriva de ella.

| Feature | Plan mínimo | Gate |
|---|---|---|
| Núcleo del Free (alcance por definir, `FREE_LIMITS = TBD`) | **FREE** | `EntitlementRepository` (server + caché TTL) — entitlement `FREE` |
| Features premium (multi-plataforma, umbrales dinámicos, nocturno, AHU, SOC…) | **PRO** (premium) | ídem — entitlement `PREMIUM` |

### Reglas del entitlement
- **El cliente no decide entitlement.** El gate premium consulta `EntitlementRepository`
  con estado verificado (online) o caché firmado offline (TTL). El usuario Free
  tiene `entitlement = FREE`; el suscriptor `entitlement = PREMIUM`; el cliente
  nunca es autoridad sobre PREMIUM.
- El caché lleva `server_issued_at`/`server_expires_at` (decisión S2: TTL 24–72 h)
  explicado en `SECURITY_MODEL.md` §6.
- Cambios de plan/renovación/refund se reflejan vía RTDN + verificación
  `purchases.subscriptionsv2.get` (ver `BACKEND_ARCHITECTURE.md` §4).
- **El modelo FREE no relaja la seguridad**: una manipulación local no otorga
  premium indefinido; el backend conserva capacidad de revocación y gestión por
  cuenta (regla D15.3, `SECURITY_MODEL.md` §seguridad del Free).

## 4. Lifecycle de estados (entitlement ↔ Play)

Mapeo del entitlement de SIRC con el `SubscriptionState` moderno de Play
(API `subscriptionsv2`, 2026):

| Estado SIRC en UI | Estado Play (source of truth) | Acción backend | Qué ve el usuario |
|---|---|---|---|
| `GRACE` | `IN_GRACE_PERIOD` | Mantener acceso; aviso de pago pendiente | Premium sigue, con aviso |
| `ACTIVE` | `ACTIVE` | Entitlement activo (TTL) | Premium |
| `ON_HOLD` | `ON_HOLD` | **Bloquear** premium | "Pago pendiente / cuenta en espera" |
| `RECOVERED` | `RECOVERED` (mismo token) | Restaurar entitlement | Premium de nuevo |
| `PAUSED` | `PAUSED` (pendiente renew) | Bloquear | "Pausado" |
| `CANCELED` | `CANCELED` (billing ends) | Entitlement hasta `expiry_time`; luego revocar | "Se cancela al final del periodo" |
| `EXPIRED` | `EXPIRED` | **Revocar** | "Suscripción vencida" |
| `PENDING_PURCHASE` | `PENDING` | No otorgar aún | "Esperando confirmación de pago" |
| `REVOKED/REFUNDED` | `REVOKED`/voided | **Revocar + clawback** | "Suspendida" |

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

## 6. Decisiones de diseño (registradas en `.ai/DECISIONS.md`)

- **D14.1 — Supabase como backend inicial** de identidad/suscripción/entitlement
  (Auth + RLS + Edge Functions + Postgres), plan Pro antes de producción.
- **D14.3 — Entitlement TTL 24–72 h** (mantiene decisión S2); `subscriptionsv2.get`
  (no `subscriptions.get`, deprecado); integración RTDN + verificación on-demand.
- **D15.1 — Descarga gratuita + SIRC FREE** con cuenta gratuita para
  adquisición/validación; monetización Premium posterior y progresiva (E3).
- **D15.2 — `FREE_INITIAL_MODEL = ENABLED`, `FREE_LIMITS = TBD`**: no se
  inventa ningún límite del Free; se definirá con decisión explícita posterior.
- **D15.3 — El Free NO relaja la seguridad**: entitlement server-side,
  revocable, sin premium indefinido por manipulación local.
- Planes activos: **FREE → PREMIUM**; niveles adicionales **TBD**.

## 7. NO implementar (riesgo de confusión)

Este documento **no** crea: productos en Play, precio en la UI, planes en
Supabase, tablas, `EntitlementRepository` de producción, banner de suscripción,
ni ningún gate premium. El **Free** tampoco se implementa todavía: pertenece a
las fases E1a/Free (validación) y E1b/E3 (cuenta/entitlement/monetización) y
requiere abrir tarea explícita (regla 9f/R16).
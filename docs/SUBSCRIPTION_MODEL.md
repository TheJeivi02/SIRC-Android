# SIRC — Modelo de Suscripción (planes y entitlement conceptual)

> Estructura conceptual de planes, precios de referencia y lifecycle de
> entitlement de SIRC (LOOP ENGINEERING — Backend Supabase, 16-ago-2026).
>
> **ESTADO: diseño SOLO.** No se definen precios finales ni se crean productos
> en Play. Sin asociación de planes reales todavía. Referencia de precios de
> mercado verificada en `docs/PRODUCT_STRATEGY.md` §… (matriz de competidores)
> y este documento.
>
> Regla: **no crear planes artificiales**; cada nivel debe corresponder a valor
> real para un conductor. El precio final considerará mercado, poder adquisitivo
> objetivo, competencia, costo de infraestructura, margen y conversión.

## 1. Estructura conceptual de planes

Propuesta en **tres niveles** + futuro, alineada con los pilares del producto
(decisión <3 s local, solo lectura, overlay).

| Nivel | Qué puede hacer el usuario | Valor que justifica el pago | Estado |
|---|---|---|---|
| **Free / Trial** | Evaluación de ofertas con información derivada básica, overlay, historial limitado en el tiempo, acceso a fetching setup de arranque. | Validar el valor real de "decisión en <3 s". Prueba limitada en número de ofertas/días (diseño de trial por confirmar). | En el roadmap de onboarding (E1b), no fija de precio todavía. |
| **SIRC Basic** (plan base de pago) | Uso completo del núcleo: overlay <3 s, OCR, evaluación de rentabilidad completa, historial, dashboard básico, soporte de 1 plataforma (Uber). | **El valor central**: no perder dinero en decisiones; rentabilidad calculada local con costos reales del conductor. | Plan de pago mínimo. Definir en E1b. |
| **SIRC Pro** (nivel superior) | Multi-plataforma (DiDi/InDrive/Cabify), umbrales dinámicos, modo nocturno, dashboard AHU y ahorro de energía SOC. | Capacidades que extienden la decisión a la cohorte multi-app y a la eficiencia energética. | Se desbloquea por features con `EntitlementRepository` server-side. |
| **Futuro** | Ecosistema Lite/Pro compartido; alertas anti-fatiga; transferencia de datos entre dispositivos (con justificación explícita de producto). | Profesionalización y retención. | Requiere producto evolucionado (E3/E4). |

### Free/Trial — matices importantes
- **No debe ser un "free forever" que invita a no pagar.** Propuesta: trial de
  tiempo (p. ej. 7–14 días `[INF]`, como el mercado: pruebas 3–15 días) con
  features básicas. No inventar duración fija: se ajustará con datos de beta.
- El trial se valida con la misma vía de entitlement (es un plan `trial` con
  `expires_at`), no con lógica local.

### Qué hace que el usuario pague (evidencia de mercado)
- El valor central es **decidir antes de aceptar** (semáforo <1 s, $/km + $/hora,
  lucro neto). Ese valor es lo que encapsula **SIRC Basic**.
- Lo que mejor monetiza en el nicho (evidencia de `PRODUCT_STRATEGY.md`): el
  overlay + métricas duales + lucro neto; el auto-aceptar (multi-app) es el
  diferenciador de precio de competidores con automatización — **SIRC NO lo
  ofrecerá** (regla R9b).

## 2. Precios de referencia (competencia verificada, 16-ago-2026)

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
1. Convalidar con datos de beta (disposición a pagar, región).
2. Definir precio final al abrir E1b con estos rangos como informed prior.
3. Publicar planes en Play Console (BasePlans/Offers); desde Supabase se
   mapeará `plan_id` ↔ `basePlanId_play`/`offerId_play`.

## 3. Entitlement (política de acceso)

El entitlement es el **permiso derivado**: qué features premium tiene el
usuario AHORA. No es la suscripción en bruto (Play), es el estado que el backend
deriva de ella.

| Feature | Plan mínimo | Gate |
|---|---|---|
| Overlay + evaluación completa (núcleo) | **Basic** | `EntitlementRepository` (server + caché TTL) |
| Multi-plataforma (DiDi/InDrive/Cabify) | **Pro** | ídem |
| Umbrales dinámicos / modo nocturno | **Pro** | ídem |
| Dashboard AHU / SOC ahorro (E3) | **Pro** | ídem |
| Free/Trial | — | plan trial con `expires_at` |

### Reglas del entitlement
- **El cliente no decide entitlement.** El gate premium consulta `EntitlementRepository`
  con estado verificado (online) o caché firmado offline (TTL).
- El caché lleva `server_issued_at`/`server_expires_at` (decisión S2: TTL 24–72 h)
  explicado en `SECURITY_MODEL.md` §6.
- Cambios de plan/renovación/refund se reflejan vía RTDN + verificación
  `purchases.subscriptionsv2.get` (ver `BACKEND_ARCHITECTURE.md` §4).

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

## 6. Decisión de diseño (para registrar en `.ai/DECISIONS.md`)

- **D14.x — Supabase como backend inicial** de identidad/suscripción/entitlement
  (Auth + RLS + Edge Functions + Postgres), plan Pro antes de producción.
- **D14.y — Planes en 3 niveles** (Free/Trial, Basic, Pro) + futuro; sin precios
  finales; rangos de referencia de mercado registrados.
- **D14.z — Entitlement TTL 24–72 h** (mantiene decisión S2); `subscriptionsv2.get`
  (no `subscriptions.get`, deprecado); integración RTDN + verificación on-demand.

## 7. NO implementar (riesgo de confusión)

Este documento **no** crea: productos en Play, precio en la UI, planes en
Supabase, tablas, `EntitlementRepository` de producción, banner de suscripción,
ni ningún gate premium. Todo eso pertenece a la fase E1b (o E3) y requiere
abrir tarea explícita (regla 9f/R16).
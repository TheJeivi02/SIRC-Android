# SIRC — Criterios de Beta (BETA_READINESS)

> Checklist formal, verificable y específica de SIRC para abrir una **beta
> cerrada**. Se escribió en paralelo con `docs/SECURITY_MODEL.md` (modelo de
> seguridad y suscripción) y `docs/PRODUCT_STRATEGY.md` (roadmap E0–E4).
>
> **Decisión de Sprint 12 (Roadmap Gate)**: Sprint 12 NO introduce ni backend ni
> suscripción ni Play Integrity (ver `docs/SECURITY_MODEL.md` y el ROADMAP
> revisado). La beta cerrada se abre sobre el núcleo de producto (captura) con
> una **superficie de suscripción/seguridad planificada, no implementada**, y
> dados los criterios de esta checklist. El objetivo de la beta es **validar el
> núcleo de producto** (overlay <3 s, OCR, estabilidad) con conductores reales,
> antes de construir la capa comercial (E1b).
>
> Estado de partida: v1.0.0-rc1 (Sprint 11) con el pipeline de captura completado.

## 1. Resumen ejecutivo

La beta cerrada es la **etapa E1a** del roadmap revisado. Tiene 2 objetivos:

1. Validar el núcleo de producto en dispositivos reales (Android 10–15) con
   conductores de Uber/DiDi/InDrive/Cabify.
2. Recolectar métricas locales de decisión (100 % local) para confirmar que el
   overlay se mantiene <3 s y es usable en ruta.

**NO incluye** (por decisión del gate): backend, suscripción, cobro, Play
Integrity con enforcement, RTDN, entitlement verificado. Eso es **E1b**. La beta
sirve para medir el producto; la monetización se diseña sobre datos reales.

## 2. Criterios de ENTRADA a la beta (todo debe pasar)

### 2.1 Producto (núcleo)

| # | Criterio | Verificación | Estado |
|---|---|---|---|
| P1 | El overlay muestra la decisión **<3 s** tras la oferta | Test en dispositivo real con cronómetro; N≥50 ofertas | ⬜ |
| P2 | **OCR aceptable**: tasa de parse en pantallas reales de Uber ≥ umbral definido | Métricas del panel de depuración (tasa éxito/fallo) | ⬜ |
| P3 | Sin crashes: la app aguanta 1 jornada (≥8 h) de uso continuo | Prueba manual + Crashlytics/ADB sin crash | ⬜ |
| P4 | Rendimiento: pico de consumo de CPU/RAM aceptable en gama media | `PERFORMANCE_REPORT.md` + medición en dispositivo real | ⬜ |
| P5 | Overlay estable: no se pierde, no se reposiciona solo, respeta config | Validación manual de overlay (posición/opacidad/TTL) | ⬜ |
| P6 | Pipeline sin errores de validación persistentes | `ValidationRecorder` ≤ umbral de fallos | ⬜ |

### 2.2 Plataforma (soporte mínimo por app)

| # | Plataforma | Nivel mínimo exigido | Verificación |
|---|---|---|---|
| PL1 | **Uber** | Detección + parse + overlay correctos en los tipos de oferta soportados (REQUEST/Radar/Moto/XL/Reserva) | Dataset + dispositivo real |
| PL2 | **DiDi** | Detección de pantalla + parse básico de oferta + overlay | Mínimo: un dispositivo real validado |
| PL3 | **Cabify** | Detección + parse básico + overlay | Mínimo: un dispositivo real validado |
| PL4 | **InDrive** | Detección + parse básico + overlay | Mínimo: un dispositivo real validado |

> En el roadmap revisado, el soporte multi-plataforma por descriptor es parte de
> **E2**. La beta puede abrirse con Uber sólido y DiDi/InDrive/Cabify en ALPHA
> (tolerancia a fallos), nunca en un estado sin detección. Se marca el estado de
> cada plataforma en la ficha de tester.

### 2.3 Seguridad (superficie mínima para la beta)

La beta NO tiene modelo comercial aún, pero la seguridad del pipeline sigue
vigente:

| # | Criterio | Verificación |
|---|---|---|
| S1 | Accessibility Service **solo lectura** verificado (sin `performAction`/gestos) | Auditoría de código + revisión política Play |
| S2 | `GOOGLE_PLAY_COMPLIANCE.md` al día (accesibilidad, mediaProjection, specialUse) | Revisión documental |
| S3 | Los frames de MediaProjection se liberan tras OCR (sin persistencia) | Revisión de `:core:capture:android` |
| S4 | Sin telemetría de contenido de pantalla; logs deshabilitados en release | Verificación de `SircLogger`/flags |

### 2.4 UX

| # | Criterio | Verificación |
|---|---|---|
| U1 | Onboarding completo sin fricción en dispositivo real | Prueba manual |
| U2 | Configuración de costos/umbrales guardada y reflejada en el overlay | Prueba manual |
| U3 | Manejo de errores claro (permisos faltantes, captura revocada, sin GPS) | Plan de pruebas manual |
| U4 | El overlay no interfiere con la conducción (no bloquea velocímetro/AOI) | Validación con conductor en ruta |
| U5 | Feedback del conductor: encuesta simple dentro de la beta | Formulario/encuesta definida |

### 2.5 Legal / Compliance

| # | Criterio | Verificación |
|---|---|---|
| L1 | Permisos declarados y justificados (Accessibility, MediaProjection, FGS) | Play Console draft completo |
| L2 | Data safety form completo (sin recolección de pantalla) | Play Console |
| L3 | Declaración de propósito de Accessibility Service correcta | `accessibility_service_config.xml` |
| L4 | Términos/privacidad de la beta (informativa; sin cuenta) | Documento legal simple |

### 2.6 Infraestructura de testing

| # | Criterio | Verificación |
|---|---|---|
| T1 | Lista de testers y opt-in (Google Play: closed track) | Play Console configurado |
| T2 | Licencia de testers / app para test purchase NO necesaria (sin Billing en beta) | — (se documenta como futuro en E1b) |
| T3 | Canal para reporte de bugs de los testers | Formulario/correo definido |
| T4 | Plan de rollout incremental (5 → 20 → 50 testers) | Documento de lanzamiento |

## 3. Criterios de SALIDA (fin de beta)

- El 90 % de los objetivos P1–P5 se cumplen en ≥3 dispositivos distintos por
  cada Android (10, 11, 12/12L, 13, 14, 15) representativos.
- La tasa de éxito de parseo en Uber alcanza el umbral definido.
- No hay crash report pendiente de severidad alta sin resolver 5 días.
- Feedback de testers: ≥80 % de respuestas "usable/rentable" en encuesta.
- Documentación del pipeline actualizada (overlay <3 s verificado con datos).

## 4. Decisiones del gate sobre Sprint 12

| Aspecto | Decisión |
|---|---|
| ¿Sprint 12 es beta cerrada + Play Integrity? | **No en su forma original.** Reestructurado: beta **sin** monetización (E1a) y Play Integrity+backend+suscripción en **E1b** (posterior, sobre datos de la beta). |
| ¿Qué se valida en Sprint 12? | Núcleo de producto en campo (P1–P5, PL1–PL4, UX, legal). |
| ¿Qué queda para después? | Entitlement, backend, Billing, RTDN, Integrity enforcement (E1b). |
| Pruebas obligatorias en Sprint 12 | Overlay <3 s cronometrado en ruta; OCR en dispositivos variados; estabilidad 8 h; cada plataforma soportada ≥1 dispositivo real. |
| ¿Superficie de riesgo? | Sin cobro en beta ⇒ sin fraude comercial; el núcleo de captura ya cumplía solo lectura.
# Roadmap

Plan de desarrollo del proyecto. El estado de cada sprint refleja el repositorio
actual.

## Sprint 1 — Proyecto base ✅

Scaffolding Gradle multi-módulo con Version Catalog, wrapper, `.editorconfig`,
`.gitignore`, CI (GitHub Actions), ktlint y lint. Arquitectura Clean +
MVVM + modularización en 8 módulos (`:app`, `:domain`, `:data`, `:core:ui`,
`:core:platform`, `:feature:overlay`, `:feature:settings`, `:feature:history`).

- Estado: **completado**.

## Sprint 2 — Design System + Overlay Foundation ✅

Base visual reutilizable (Material 3) y el overlay flotante sobre las apps de
transporte.

**Design System (`:core:ui`):**

- `SircTheme`, `SircColors` (semáforo), `SircTypography`, `SircSpacing` y
  `SircElevations`.
- `ProfitState` + `ProfitIndicator` (insignia semáforo), `DecisionBadge`,
  `StatusDot`, `SectionCard`, `LabeledValue`, `MetricCell`/`MetricValue`,
  `OverlayCard`/`OverlayCardContent`.
- `@Preview` y KDoc en todos los componentes; pruebas unitarias de paleta y
  estados.

**Overlay:**

- `OverlayService` (Foreground Service) con `TYPE_APPLICATION_OVERLAY`.
- `OverlayContent`: insignia de decisión y hasta 4 indicadores, consumiendo los
  componentes de `:core:ui`.
- `OverlayConfig`: indicadores visibles, modo compacto, opacidad, TTL, posición.
- `OverlayDataSource`/`SimulatedOverlayDataSource`: datos simulados evaluados
  con el `ProfitEngine` real.
- `PermissionManager`, `OverlayManager` y `OverlayController` (control del
  servicio).
- `OverlayViewModel` (`@HiltViewModel`) y `OverlayModule` (`@Binds`).
- Pantalla Diagnóstico con 5 indicadores y vista previa simulada.

- Estado: **completado**.

## Sprint 3 — Configuración Inicial del Conductor ✅

Flujo de onboarding que alimenta al motor de rentabilidad con datos reales.

- **Perfil**: nombre (opcional), país, ciudad, moneda.
- **Vehículo**: nombre, marca, modelo, año, tipo de combustible, consumo (km/L).
- **Costos básicos**: precio del combustible, mantenimiento por km y otros
  costos configurables (arquitectura lista para ampliarlos).
- **Plataformas**: selección múltiple (Uber, DiDi, InDrive, Cabify).
- **Objetivos**: ganancia mínima por km y por hora (indicadores del MVP).
- Persistencia local en Room (`driver_config` v2, migración 1→2) y exposición
  vía casos de uso.
- Gating en el arranque: onboarding solo la primera vez; app principal cuando
  el conductor ya está configurado.
- Sin OCR, sin IA, sin extracción de datos, sin fórmulas complejas.

- Estado: **completado**.

## Sprint 4 — Plataforma de Captura (Infrastructure First) ✅

Infraestructura de captura que servirá de base para analizar ofertas reales en
el futuro. NO se implementó OCR, ML Kit, IA, regex ni interpretación de texto.

- Nuevo módulo **`:core:capture`** (Kotlin puro): `WindowObserver`,
  `OfferCaptureSession`, `OfferSnapshot` (inmutable, simulado), `OfferParser` +
  `FakeParser`, `CaptureRepository` (en memoria, lista para persistencia),
  `OfferCaptureCoordinator` (desacoplado, orquesta captura de extremo a
  extremo).
- **Feature Flags** configurables en caliente: `ACCESSIBILITY`, `OVERLAY`,
  `CAPTURE`, `PARSER`, `DEBUG_PANEL`.
- **Logging centralizado** (`SircLogger`), deshabilitado fuera de debug.
- `AccessibilityWindowObserver` + `AndroidSircLogger` en `:feature:overlay`;
  `SircAccessibilityService` reenvía los cambios de ventana al pipeline sin
  interpretar y sin alterar el flujo existente.
- **Panel de depuración** en `:app` (destino Debug): estado de accesibilidad/
  overlay/captura/parser, toggles de flags, último snapshot, tiempo de
  procesamiento, memoria aproximada y eventos recientes.
- Pruebas unitarias de `:core:capture`; CI actualizado para correr `test`.

- Estado: **completado**.

## Sprint 5 — Primer Pipeline de Captura + OCR ✅

Primer pipeline completo de captura de ofertas (Uber Driver) preparado para
OCR, con el servicio de accesibilidad desacoplado de la UI.

- **`CaptureAccessibilityService`** dedicado a la captura (solo lectura,
  desacoplado de la UI), reutilizando la config de accesibilidad existente.
- **`OverlayState`** (`:core:capture`): `DISABLED`, `WAITING`, `CAPTURING`,
  `PROCESSING`, `ERROR`.
- **`CapturePipeline`** (`:core:capture`, puro): `CaptureRequest` →
  `ScreenCapture` → OCR (`OcrEngine`) → `OfferParser` → `CaptureRepository`,
  exponiendo el `OverlayState`. `OverlayService`/`OverlayManager` mantienen su
  arquitectura independiente.
- **ML Kit OCR** integrado bajo la abstracción `OcrEngine` (`MlKitOcrEngine`),
  sustituible y testeable; flag `OCR` nuevo.
- **Imágenes de prueba** (`core/capture/src/test/resources/test-images/`) y
  pruebas unitarias del pipeline y del parser.
- Panel de depuración: `Estado del pipeline` (`OverlayState`) y fila `OCR`.
- Corrección de las incidencias de ktlint de `DebugPanelScreen.kt` (resueltas
  en v0.5.0; verificadas sin incidencias).

- Estado: **completado**.

## Sprint 6 — Captura de pantalla real con MediaProjection ✅

Captura de pantalla real (MediaProjection) integrada con el pipeline de
extremo a extremo: accesibilidad → debounce → captura de frame → OCR → parser →
repositorio → estado del overlay, con caché por hash y métricas de rendimiento.

- **Nuevo módulo `:core:capture:android`**: `ScreenCaptureProvider` +
  `MediaProjectionScreenCaptureProvider` (token MediaProjection, `VirtualDisplay`
  + `ImageReader`, último frame), `MediaProjectionService` (FGS tipo
  `mediaProjection` + `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`),
  `MediaProjectionScreenCapture` (ScreenCapture real), `DebugCaptureMetrics`
  (solo debug) y test instrumentado de humo.
- **Pipeline real**: `CapturePipeline` expone `snapshots` (SharedFlow) y
  `lastMetrics` (StateFlow con captura/OCR/parseo/total);
  `DefaultCapturePipeline` con caché de frames por hash (`CaptureFrameCache` +
  `InMemoryCaptureFrameCache`, LRU 32) y métricas por etapa.
- **Debounce de accesibilidad**: `DebounceCaptureScheduler` (400 ms) consumido
  por `CaptureAccessibilityService` para no ejecutar OCR en cada evento.
- **Overlay conectado al pipeline**: `PipelineOverlayDataSource` (estado real +
  evaluación con el motor real) sustituye a `SimulatedOverlayDataSource`;
  `OverlayContent` muestra `StatusLabel` (Esperando/Capturando/Analizando/Error);
  `OverlayService` se muestra según `visible`.
- **UI**: permiso de captura en Home (lanzador del sistema → `startProjection`,
  `projectionActive`, stop) y métricas de rendimiento en el panel de depuración.
- **Tests**: `DebounceCaptureSchedulerTest`, `InMemoryCaptureFrameCacheTest`,
  `PipelineOverlayDataSourceTest` y ampliación de `DefaultCapturePipelineTest`.
- Verificación: `ktlintCheck`, `testDebugUnitTest`, `:core:capture:test`,
  `lintDebug`, `assembleDebug` y `assembleDebugAndroidTest` en verde.

- Estado: **completado**.

## Sprint 7 — Evaluación en tiempo real con recomendación ✅

La oferta capturada se evalúa con el motor real y el overlay muestra una
recomendación accionable con motivo y confianza, con métricas de rendimiento por
oferta en el panel de depuración.

- **Motor de evaluación detallada** (`ProfitEvaluationEngine`): reutiliza
  `ProfitEngine` (delega) y deriva los costos desde `DriverConfig`
  (combustible + mantenimiento + adicionales por km); los umbrales de decisión
  vienen **solo de `DriverConfig.thresholds`** (O1/O6).
- **`RecommendationEngine`** (O2): `ACCEPT`/`REJECT`/`WARNING` con motivo
  principal, métricas usadas y % de confianza.
- **Historial en memoria** (O4): `OfferEvaluationRepository` +
  `InMemoryOfferEvaluationRepository` (100 ofertas, sin Room).
- **Métricas por oferta** (O8): `OfferTiming` + `OfferPerformanceTracker`
  (últimas 100, promedio de 20) alimentados por el pipeline y el overlay.
- **Overlay con recomendación** (O3): `RecommendationBadge`, precio, ganancia,
  $/hora, $/km, costo estimado, motivo y confianza; `OverlayUiState.recommendation`.
- **Panel de depuración** (O5): sección "Última oferta" (recomendación + texto
  OCR) y "Rendimiento (promedio últimas 20 ofertas)".
- **Tests** (O7): engines, tracker, historial, overlay data source y pipeline.
- Verificación en verde: ktlint, unit tests (todos los módulos), lint,
  assembleDebug y assembleDebugAndroidTest.

- Estado: **completado**.

## Sprint 8 — Motor de análisis de pantallas reales ✅

El parser se convierte en un motor de análisis robusto para pantallas reales de
Uber Driver: detección de pantalla → parsers especializados → validación →
reglas → confianza → overlay con tipo de oferta, confianza y veredicto de reglas.

- **Detección de pantalla** (O1, `:core:platform`): `OfferDetectionEngine`
  clasifica el texto visible en `ScreenType` (HOME/REQUEST/TRIP/NAVIGATION/
  OFFLINE/ERROR/UNKNOWN) con keywords ponderadas; solo `REQUEST` produce oferta.
- **Parsers especializados + orquestador** (O2, `:core:platform`): `OfferType`,
  `OfferTypeParser`/`BaseOfferTypeParser` con `UberRequestParser`,
  `UberRadarParser`, `UberReservationParser`, `UberMotoParser`, `UberXlParser`;
  `OfferParserOrchestrator` detecta → especializados → extractor genérico.
- **Motor de Confianza** (O3, `:domain`): `ConfidenceEngine` → nivel HIGH/
  MEDIUM/LOW con % y razones; LOW = "Información insuficiente".
- **Validación cruzada** (O4, `:domain`): `OfferValidator` con `ValidationIssue`.
- **Motor de Reglas** (O5, `:domain`): `RuleEngine` + 6 reglas
  (`MinimumProfit`, `MinimumProfitPerKm`, `MinimumProfitPerHour`,
  `MaximumDistance`, `MaximumPickup`, `MaximumTripTime`) con `RuleVerdict`
  PASS/WARNING/FAIL y umbrales desde `DriverConfig`.
- **Overlay con análisis real** (O7): `OverlayUiState.offerType`/`confidence`/
  `ruleEvaluation`; `OverlayContent` muestra tipo + confianza e
  "Información insuficiente" cuando no es accionable.
- **Panel de depuración ampliado** (O8): sección "Análisis" con tipo, confianza
  (razones) y veredicto de cada regla.
- **Dataset** (O6): `test-images/` con 9 escenarios Uber + README.
- **Métricas por etapa** (O10): `detectionMillis`/`rulesMillis` en
  `OfferTiming`/`ProcessingMetrics`; regex del parser precompiladas.
- **Tests** (O9): `OfferDetectionEngineTest` (12), `OfferParserOrchestratorTest`
  (9), `RuleEngineTest`, `ConfidenceEngineTest`, `OfferValidatorTest` y
  ampliación de `PipelineOverlayDataSourceTest`.
- Verificación en verde: ktlint (todos), unit tests, `lintDebug`, `assembleDebug`.

- Estado: **completado**.

## Sprint 9 — Preparación beta cerrada (v1.0.0-beta) ✅

Preparación de la app para una beta cerrada en dispositivos reales: sesión de
captura, persistencia e historial completos, dashboard de estadísticas,
optimización y estabilidad, modo beta con diagnóstico exportable y un overlay
más estable y claro.

- **Sesión de captura** (O1, `:domain`): `CaptureSessionManager` (iniciar,
  pausar, reanudar, detener, reset) + `SessionStatus` + `SessionStats`
  (duración activa en vivo, ofertas procesadas/aceptadas/rechazadas, errores);
  reloj inyectable para pruebas. El pipeline y el overlay alimentan la sesión.
- **Persistencia completa del historial** (O2): `OfferHistoryEntry` ampliado
  (tipo de oferta, confianza, reglas, razones, recomendación y tiempos);
  Room v3 con migración 1→3 (nuevas columnas y `overlay_config.historyLimit`),
  DAO con `trimToLimit`/`count`; `OverlayConfig.historyLimit` (default 500)
  configura el límite desde Ajustes.
- **Pantalla Historial** (O3): filtros por plataforma/decisión/fecha/prescencia,
  búsqueda de texto, presets de fecha y detalle en diálogo
  (`HistoryFilters`/`HistoryFilter` en `:domain`).
- **Dashboard de estadísticas** (O4): `HistoryStats` +
  `HistoryStatsCalculator` (aceptación, ganancia/hora, ganancia/km,
  procesamiento, confianza, agrupación diaria) y pantalla con Canvas
  (barras diarias + donut de decisiones) en `:feature:history`.
- **Optimización** (O5): OCR recicla el bitmap y cancela la corrutina;
  MediaProjection recrea el virtual display ante cambios de configuración
  (`onDisplayConfigChanged`) y libera/reintenta de forma idempotente.
- **Estabilidad** (O6): `OverlayService` con vista persistente y
  `FLAG_NOT_TOUCHABLE`; `onConfigurationChanged` reclama tamaño/posición;
  manejo de rotación, split screen y revocación de permisos.
- **Modo Beta** (O8): feature flags `RULES`, `DETAILED_LOGS`, `METRICS`;
  `AndroidSircLogger.debug` apagado con `DETAILED_LOGS`; **Exportar
  diagnóstico** (share del estado de sesión, rendimiento y flags).
- **Overlay mejorado** (O9): `OverlayContent` con `animateFloatAsState`
  (escala/alpha) y `AnimatedContent` (crossfade estado↔evaluación);
  `StatusLabel` con mensajes claros.
- **Tests de integración** (O7): `CaptureSessionManagerTest`,
  `HistoryFilterTest`, `HistoryStatsCalculatorTest`,
  `PipelineOverlayDataSourceTest` (sesión + persistencia),
  `OfferHistoryDaoTest` y `SircDatabaseMigrationTest` (v1→v3) en Room.
- Verificación en verde: ktlint (todos), unit tests, `lintDebug`,
  `assembleDebug`, `assembleDebugAndroidTest` y `:data:connectedDebugAndroidTest`.

- Estado: **completado**.

## Sprint 10 — Hardening y Release Candidate (v1.0.0-rc1) ✅

Endurecimiento de la beta para pruebas intensivas en dispositivos reales
(Android 10–15). No añade funcionalidades de producto: solo estabilidad,
observabilidad y compatibilidad.

- **Auditoría** (O1): sin `TODO`/`FIXME`/`XXX`/`HACK`; revisión de manifests,
  strings y recursos; eliminación del flujo legacy `OfferEvaluator`/
  `OfferEventBus` (persistía historial **duplicado**; el pipeline moderno es la
  única fuente). `SircAccessibilityService` conserva el reenvío de eventos.
- **Modo de validación** (O3): `ValidationRecorder` (`:core:capture`, puro,
  buffer 500) con `CaptureError`/`OcrFailed`/`ParseFailed`/`FrameDiscarded`
  (`CAPTURE_FAILED`/`DUPLICATE`/`NO_TEXTS`/`UNSUPPORTED_PLATFORM`)/
  `RuleFailed`/`OfferRejected`; sección en el Panel de depuración con contadores,
  **Exportar informe de validación** y Limpiar; el informe se adjunta a
  "Exportar diagnóstico".
- **Crash recovery** (O6): OCR degrada a textos de accesibilidad (ya no entra en
  `ERROR`); fallos no controlados registran `CaptureError`; MediaProjection
  registra token no disponible / proyección interrumpida.
- **Logs por niveles** (O7): `ERROR`/`WARNING` siempre (también Release);
  `INFO`/`DEBUG` solo en desarrollo; `DEBUG` requiere el flag `DETAILED_LOGS`.
- **Compatibilidad Android 15** (O4): `screenBounds()` con
  `WindowManager.getCurrentWindowMetrics()` (API 30+) y fallback API 24–29.
- **Rendimiento/consumo** (O2/O5): `docs/PERFORMANCE_REPORT.md`; buffers
  acotados verificados por el test de stress (tracker ≤ 100, repositorio ≤ 50,
  validación ≤ 500).
- **Tests** (O9): `ValidationRecorderTest`, pipeline de validación y stress de
  200 solicitudes.
- **Documentación** (O10): `RELEASE_NOTES_RC1.md`, `KNOWN_ISSUES.md`,
  `PERFORMANCE_REPORT.md`, `TEST_REPORT.md`; CHANGELOG/ROADMAP/CONTEXT/DECISIONS
  actualizados.
- Verificación en verde: ktlint (todos), `lintDebug`, `assembleDebug`,
  `assembleDebugAndroidTest` y todos los tests unitarios.

- Estado: **completado**.

## Sprint 11 — Auditoría, remediación y consolidación (WP-E1/E2/E3) ✅

Ejecución de la remediación planificada en `docs/remediation/` (basada en las
9 auditorías de `docs/audit/`). Sin funcionalidades de producto nuevas:
eliminación de código dual, endurecimiento del pipeline y auditoría interna de
arquitectura.

- **WP-E1 — Eliminación y consolidación**: `FakeParser` fuera de la ruta de
  producción (WP-E1-01); `ProfitEngine` como único motor de decisión, sin rama
  `RuleEngine` (WP-E1-02); unificación en un único `CaptureAccessibilityService`
  (WP-E1-03).
- **WP-E2 — Endurecimiento de captura**: limpieza determinista de recursos en
  `MediaProjectionService` (WP-E2-01) y máquina de estados endurecida en
  `ScreenCaptureProvider` (WP-E2-02).
- **WP-E3 — Detección de plataforma y auditoría**: detección descriptor-driven
  (WP-E3-01), `PlatformDetectionEngine`/`DetectionMatcher`/`DetectionResult` en
  `:core:platform` (WP-E3-02), Unified Capture Source con `CaptureInput` único
  (WP-E3-03), auditoría de arquitectura (WP-E3-04) y resolución de hallazgos de
  severidad Alta/Media/Baja + limpieza documental (WP-E3-05A a 05E). Se elimina
  el paquete de reglas legacy (`RuleEngine`, `OfferRule`, `OfferValidator`,
  `RuleContext`, `RuleThresholds`, `ValidationResult`, `ValidationIssue`).
- **Overlay**: correcciones de crash (`SavedStateRegistry`,
  `ViewTreeLifecycleOwner`) sin cambios de funcionalidad.
- Verificación en verde: ktlint, unit tests, `lintDebug`, `assembleDebug`.

- Estado: **completado** (cierre en `456ca67`).

## Ruta estratégica de producto (etapas) — REVISADA (Roadmap Gate + LOOP Modelo Free)

> Consolidación de las tres fuentes + **Roadmap Gate** (coherencia con modelo de
> suscripción y seguridad; ver `docs/SECURITY_MODEL.md` y
> `docs/BETA_READINESS.md`) + **LOOP Modelo Free** (16-ago-2026).
>
> **Reestructuración introducida por el gate**: la etapa E1 se divide en
> **E1a (beta de validación del núcleo, sin monetización)** y **E1b
> (integración comercial: backend de cuenta, entitlement, Play Integrity)**. La
> seguridad comercial NO queda al final del roadmap (se incorpora desde E1b),
> pero tampoco desplaza la validación del núcleo de producto.
>
> **Reestructuración introducida por el LOOP Modelo Free (D15.x)**: entre E1a y
> E1b se incorpora la etapa **FREE / BETA ABIERTA — adquisición**: descarga
> gratuita + cuenta gratuita + plan FREE + usuarios reales + feedback +
> telemetría mínima y privada + corrección de errores → madurez → monetización
> **progresiva** en E3. No se cobra en la fase inicial.

| Etapa | Alcance | Objetivo | Valor | Dependencias | Riesgo | Seguridad | Criterio de salida |
|---|---|---|---|---|---|---|---|
| E0 — Cierre técnico | Remediación + RC1 + auditorías (Sprints 4–11) | Estabilidad y arquitectura | Base para beta | — | Bajo | Solo lectura intacto | ✅ completado |
| **E1a — Beta controlada (núcleo de producto)** | Beta cerrada sin monetización; Play Console internal/closed track; validación overlay <3 s, OCR, estabilidad Android 10–15, multi-plataforma en ALPHA | Validar el producto real **antes** de construir cobro | Datos reales de campo | E0 | Medio (crash/calidad en campo) | Solo lectura + compliance ✓ (sin billing) | Checklist `BETA_READINESS.md` §2 (P1–P5, PL1–PL4, U1–U5, L1–L4) |
| **FREE / BETA ABIERTA — adquisición** | **Descarga gratuita + cuenta gratuita + plan FREE** (sin monetización); usuarios reales, feedback, telemetría mínima y privada, corrección de errores, afinamiento del núcleo | Adquirir usuarios y validar producto/mercado **antes** de monetizar | Base de usuarios y feedback | E1a | Medio | Entitlement `FREE` server-side (no relaja seguridad, D15.3) | Señales de retención/valor; decisión comercial de fijar `FREE_LIMITS` y abrir monetización |
| **E1b — Integración comercial** | **Backend de cuenta (Supabase: Auth/RLS/Edge Functions)** + entitlement server + Play Billing (suscripción verifiable) + Play Integrity (Standard + tiered) + RTDN | Cuenta y entitlement seguros; la suscripción Premium NO es barrera de adquisición | Cuenta/entitlement seguros | FREE (usuarios reales) | Alto (fraude/piratería) | **Ver `SECURITY_MODEL.md`** (threat model T1–T20, TTL offline, trust model) y `docs/BACKEND_ARCHITECTURE.md` (Supabase, Account Gate) | End-to-end: alta cuenta→entitlement FREE→(futuro) compra→verificación→revocación por RTDN probado |
| E2 — Crecimiento multi-plataforma | Descriptores DiDi/InDrive/Cabify en producción + umbrales dinámicos + modo nocturno | Escalar cohorte multi-app | Crecimiento | E1 (base estable) | Medio | Integridad en features premium por E1b | 4 plataformas con overlay verificado en prod |
| E3 — Diferenciación | Dashboard AHU/tendencias + ahorro energía SOC-aware + modo anti-fatiga | Profesionalización | Retención | E2 | Medio | Entitlement por features premium | Métricas AHU visibles y mejora de batería |
| E4 — Expansión | Ecosistema Lite/Pro + Android 16 + LATAM | Mercado ampliado | Escala | E3 | Medio | Key Sharing API (JSSEC 5.6) entre Lite/Pro | Lite/Pro lanzado y compartiendo entitlement |

### Decisiones que guían la ruta

- **Prohibido cualquier automatización de clics/gestos** (auto-aceptar,
  contra-ofertas, `performAction`): riesgo de baneo y violación de Play. El
  diferenciador de SIRC es "solo lectura + <3 s + local-first".
- **Modelo comercial**: **apk de pago por suscripción** (nueva restricción
  formal). El APK se considera manipulable; la autorización de features premium
  se decide en backend, no en cliente (`docs/SECURITY_MODEL.md`).
- **LOCAL-FIRST** (reemplaza "100 % local"): el procesamiento de ofertas (OCR,
  parsing, evaluación, overlay) permanece 100 % local y sin salida de datos de
  pantalla; identidad/suscripción/entitlement/integridad usan **servicios
  remotos mínimos**.
- **Backend inicial (E1b) = Supabase** (Auth + RLS + Edge Functions + Postgres;
  plan **Pro** en producción) para identidad/suscripción/entitlement; ver
  `docs/BACKEND_ARCHITECTURE.md`. Verificación de compra con Play API **v2**
  (`subscriptionsv2.get`). **No implementar antes de E1b** (regla 9f).
- **Modelo de suscripción**: estructura conceptual **FREE → PREMIUM** (+ futuro
  TBD) en `docs/SUBSCRIPTION_MODEL.md`; `FREE_INITIAL_MODEL = ENABLED`,
  `FREE_LIMITS = TBD`; el Free se adjudica server-side con la cuenta; la
  monetización Premium es **progresiva (E3)**, con decisión explícita para fijar
  límites/precios.
- **Modelo comercial inicial (LOOP Modelo Free)**: **descarga gratuita + cuenta
  gratuita + plan FREE** para adquisición y validación; ver
  `docs/SUBSCRIPTION_MODEL.md` §1.
- **Prioridades P0–P3** revisadas en `docs/PRODUCT_STRATEGY.md`; ninguna
  feature fuera de ellas entra al roadmap sin aprobación explícita.
- **Herramientas**: OpenCode principal + Antigravity complementario
  (`docs/ANTIGRAVITY_EVALUATION.md`); prohibido doble agente simultáneo en el
  mismo branch (regla R17).

## Sprint 12 — Beta controlada: validación núcleo de producto (E1a) 🔵 Planificado

**Reemplaza la propuesta anterior** ("Sprint 12 = beta + Play Integrity").
Decisión del Roadmap Gate (§12 de `docs/SECURITY_MODEL.md` y
`docs/BETA_READINESS.md` §4): Play Integrity, backend, suscripción y RTDN NO
entran en el Sprint 12; se desplazan a **E1b** (se construyen sobre los datos de
la beta).

Respondiendo al gate:

- **A. ¿Es el siguiente Sprint?** Sí, pero redefinido: la beta cerrada SE
  mantiene como próximo paso (el RC1 permite validar en campo), pero **sin**
  Play Integrity ni billing. Añadir monetización antes de validar el núcleo
  arriesga construir cobro sobre un producto aún no probado con conductores reales.
- **B. Qué debe existir ANTES**: E0 completado (✅), checklist `BETA_READINESS.md`
  §2 cumplida, Plan de testers y opt-in de Play Console, panel de métricas local.
- **C. Qué se desarrolla en Sprint 12**: instrumentación de beta (métricas de
  decisión local, encuesta, reporte de bugs), pulido de UX de errores, ajustes de
  estabilidad según primer lote de testers, rellenado de la checklist. La
  **cuenta/registro SIRC** (fase FREE) se introduce por decisión explícita cuando
  la beta lo justifique (LOOP Modelo Free; no se implementa en este sprint).
- **D. Qué queda para después**: entitlement, backend, Billing, RTDN, Play
  Integrity con enforcement, modo anti-fatiga, AHU (E1b, E3).
- **E. Pruebas obligatorias (mínimo)**: overlay <3 s cronometrado en ruta con
  conductor, OCR en ≥3 dispositivos/Android 10–15, jornada ≥8 h sin crash,
  cada plataforma soportada en ≥1 dispositivo real, revocación de permisos
  (MediaProjection) y vuelta al servicio sin crash.

- Estado: **planificado — sin implementar** (regla R16: no implementar sin
  instrucción explícita).

## Sprint 3 — Accessibility

Canal de lectura del contenido visible de las plataformas.

- `SircAccessibilityService` (solo lectura, filtrado por paquete).
- Traversal con límites duros y deduplicación por huella de texto.
- `OfferEventBus` como puente hacia el evaluador.

- Estado: **implementado en el MVP**.

## Sprint 4 — Motor de Rentabilidad

Decisión de rentabilidad en menos de 3 segundos.

- `ProfitEngine` (función pura): ganancia, ganancia/hora, ganancia/km, margen.
- `DecisionThresholds` (ganancia mínima y mínima por hora) y `Decision`
  (`PROFITABLE`, `MARGINAL`, `NOT_PROFITABLE`).
- `OfferTextParser` + extractores por plataforma en `:core:platform`.
- Pruebas unitarias del motor y del parser.

- Estado: **implementado en el MVP**.

## Sprint 5 — Persistencia

Persistencia local de configuración e historial.

- Room (`SircDatabase`): `driver_config`, `overlay_config`, `offer_history`.
- Repositorios `Default*` y mappers, con valores por defecto.
- Historial de ofertas evaluadas (límite 100) en `:feature:history`.

- Estado: **implementado en el MVP**.

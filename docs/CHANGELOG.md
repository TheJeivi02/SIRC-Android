# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
Las versiones siguen [SemVer](https://semver.org/lang/es/).

## [v1.0.0-rc1] — 2026-08-01

Hardening y preparación de la Release Candidate. Sin funcionalidades nuevas de
producto: estabilidad y observabilidad.

### Añadido

- **Modo de validación** (O3): `ValidationRecorder` (`:core:capture`, puro) con
  buffer acotado (500 eventos) que acumula `CaptureError`, `OcrFailed`,
  `ParseFailed`, `FrameDiscarded` (`CAPTURE_FAILED`/`DUPLICATE`/`NO_TEXTS`/
  `UNSUPPORTED_PLATFORM`), `RuleFailed` (FAIL) y `OfferRejected`. Sección
  **"Modo validación"** en el Panel de depuración con contadores, **Exportar
  informe de validación** (share) y Limpiar eventos; el informe también se
  adjunta al final de "Exportar diagnóstico". `ValidationRecorderTest`.
- **Recuperación ante fallos** (O6): el pipeline degrada a textos de
  accesibilidad si **el OCR falla** (registra `OcrFailed`, ya no entra en
  `ERROR`); los fallos no controlados registran `CaptureError`. La
  infraestructura de MediaProjection registra incidentes (token no disponible,
  proyección interrumpida por el sistema).
- **Logs por niveles** (O7): `AndroidSircLogger` emite **ERROR/WARNING siempre**
  (también en Release), **INFO** solo en desarrollo y **DEBUG** solo en
  desarrollo con el flag `DETAILED_LOGS`.
- **Compatibilidad Android 15** (O4): `OverlayService.screenBounds()` usa
  `WindowManager.getCurrentWindowMetrics()` (API 30+) con fallback para API
  24–29 (elimina las APIs deprecadas `defaultDisplay.getRealMetrics()`).
- **Tests de hardening** (O9): `ValidationRecorderTest`, pipeline de validación
  (OCR degradado, descartes `CAPTURE_FAILED`/`DUPLICATE`, `CaptureError`) y
  **stress de 200 solicitudes** que verifica buffers acotados (tracker ≤ 100,
  repositorio ≤ 50, validación ≤ 500).
- **Documentación RC1** (O10): `docs/RELEASE_NOTES_RC1.md`,
  `docs/KNOWN_ISSUES.md`, `docs/PERFORMANCE_REPORT.md` y `docs/TEST_REPORT.md`.

### Cambiado

- Eliminados `OfferEvaluator` y `OfferEventBus` (flujo legacy que persistía un
  historial básico **duplicado**); `SircAccessibilityService` conserva solo su
  rol de reenvío de eventos de ventana al pipeline/panel de depuración;
  `OverlayService` deja de inyectar el evaluador. **Fin del historial
  duplicado** (el pipeline moderno es la única fuente).
- `ValidationRecorder` inyectado en `DefaultCapturePipeline`,
  `PipelineOverlayDataSource` y `MediaProjectionScreenCaptureProvider`.
- `ValidationSummary` incluye `captureErrors`.

### Notas técnicas

- Auditoría O1: sin `TODO`/`FIXME`/`XXX`/`HACK`; recursos y tokens de tema
  verificados en uso.
- Verificación en verde: `ktlintCheck`, `lintDebug`, `assembleDebug`,
  `assembleDebugAndroidTest` y todos los tests unitarios.

## [v1.0.0-beta] — 2026-08-01

### Añadido

- **Sesión de captura** (O1, `:domain`): `CaptureSessionManager` controla el
  ciclo de vida (iniciar/pausar/reanudar/detener/reset) y acumula
  `SessionStats` (duración activa en vivo, ofertas procesadas/aceptadas/
  rechazadas, errores); `SessionStatus` con `IDLE`/`ACTIVE`/`PAUSED`. El reloj
  es inyectable para pruebas deterministas. `PipelineOverlayDataSource` la
  alimenta (inicia en cada snapshot, registra decisión o error).
- **Persistencia completa del historial** (O2): `OfferHistoryEntry` ampliado
  con tipo de oferta, confianza (%, nivel), resumen de reglas, motivos,
  recomendación y tiempos (procesamiento/evaluación/reglas). Room pasa a
  **v3** con migración 1→3: 9 columnas nuevas en `offer_history` y
  `historyLimit` (default 500) en `overlay_config`; `OfferHistoryDao` con
  `trimToLimit(limit)`/`count()`. El repositorio recorta automáticamente al
  insertar usando `OverlayConfig.historyLimit`.
- **Pantalla Historial completa** (O3, `:feature:history`): `HistoryFilters`/
  `HistoryFilter` en `:domain`; barra de búsqueda, dropdowns de
  plataforma/decisión, presets de fecha (Hoy/7/30 días), borrado y **detalle**
  en diálogo (precio, distancia, duración, ganancia, tipo, confianza,
  recomendación, reglas, motivos). Ajustes: campo **Límite de registros**.
- **Dashboard de estadísticas** (O4, `:feature:history`): `HistoryStats` +
  `HistoryStatsCalculator` en `:domain` (aceptación, ganancia total, $/hora,
  $/km, procesamiento medio, confianza media, agrupación por día) y
  `StatsScreen` con gráficos **Canvas** (barras diarias + donut de decisiones);
  nuevo destino de navegación `STATS`.
- **Modo Beta** (O8): feature flags `RULES` (gatea `RuleEngine`), `DETAILED_LOGS`
  (apaga `AndroidSircLogger.debug`) y `METRICS`. **Exportar diagnóstico** en
  Debug comparte un informe con estado de sesión, promedios, última oferta y
  estado de cada flag.
- **Overlay mejorado** (O9): `OverlayContent` con animaciones
  (`animateFloatAsState` escala/alpha y `AnimatedContent` crossfade
  estado↔evaluación) y `StatusLabel` con mensajes claros por estado.
- **Tests de integración** (O7): `CaptureSessionManagerTest`,
  `HistoryFilterTest`, `HistoryStatsCalculatorTest`,
  `PipelineOverlayDataSourceTest` ampliado (sesión + persistencia),
  `OfferHistoryDaoTest` y `SircDatabaseMigrationTest` (migración v1→v3).

### Cambiado

- `OverlayService` reescrito: **una sola vista persistente** (sin
  agregar/quitar en cada oferta), ocultar = `FLAG_NOT_TOUCHABLE`,
  `onConfigurationChanged` reclama tamaño/posición, `START_STICKY`.
- `MediaProjectionService` con `onConfigurationChanged` → el provider recrea el
  virtual display; `MediaProjectionScreenCaptureProvider` con
  `onDisplayConfigChanged`/`releaseVirtualDisplay`/`drainFrames` idempotentes.
- `MlKitOcrEngine` recicla el bitmap y cancela la corrutina si se aborta.
- `DebugPanelViewModel`/`DebugPanelScreen`: nueva sección **Sesión de captura**
  (estado, duración, contadores y botones Iniciar/Pausar/Reanudar/Detener) y
  botón **Exportar diagnóstico**.
- `OfferHistoryDao.trimToLimit` reformateado; `SircDatabase` versión 3;
  `DatabaseModule` registra `MIGRATION_2_3`.
- `SessionStats.activeSeconds` se calcula en vivo con reloj inyectable (sin
  afectar la igualdad estructural del data class).
- Dependencias de test: `room-testing`, `androidx.arch.core:core-testing` y
  `androidx.test.ext:junit` en `:data` (androidTest).

## [v0.9.0] — 2026-07-31

### Añadido

- **Detección de pantalla** (O1, `:core:platform`): `OfferDetectionEngine`
  clasifica el texto visible (OCR/accesibilidad) en `ScreenType`
  (`UNKNOWN`/`HOME`/`REQUEST`/`TRIP`/`NAVIGATION`/`OFFLINE`/`ERROR`) usando
  palabras clave ponderadas; solo `REQUEST` produce ofertas evaluables.
  `ScreenDetection` expone tipo, keywords matcheadas, confianza (peso ×
  aciertos / 8.0) e `isRequest`. Normaliza texto a minúsculas sin acentos.
  Incluye keywords de variantes Uber (radar, moto, xl, reservado, programado).
- **Parsers especializados + orquestador** (O2, `:core:platform`):
  `OfferType` (`UBER_REQUEST`/`UBER_RADAR`/`UBER_RESERVATION`/`UBER_MOTO`/
  `UBER_XL`/`GENERIC`), `OfferTypeParser` (interfaz) y `BaseOfferTypeParser`
  con 5 parsers concretos; `OfferParserOrchestrator` detecta → prueba parsers
  especializados (específicos primero, Uber solo) → extractor genérico por
  plataforma. `ParsedOffer` incluye tiempos internos de detección/parsing
  (Debug).
- **Motor de Confianza** (O3, `:domain`): `ConfidenceEngine.assess` combina
  completitud de datos, coherencia de métricas (precio/km ≤ 500 y precio/hora
  ≤ 5000), moneda y resultados de reglas → `ConfidenceResult` con nivel
  `HIGH`/`MEDIUM`/`LOW`, % y razones; `LOW` = "Información insuficiente".
- **Validación cruzada de ofertas** (O4, `:domain`): `OfferValidator` detecta
  montos inválidos, distancias/duraciones faltantes, precios por km/hora
  irracionales y recogidas más lejanas que el viaje (`ValidationIssue`).
- **Motor de Reglas** (O5, `:domain`): `RuleEngine` ejecuta las 6 reglas del
  MVP (`MinimumProfit`, `MinimumProfitPerKm`, `MinimumProfitPerHour`,
  `MaximumDistance`, `MaximumPickup`, `MaximumTripTime`) y agrega
  `RuleEvaluation` con `RuleVerdict` PASS/WARNING/FAIL. `RuleThresholds`
  deriva los umbrales de rentabilidad desde `DriverConfig`.
- **Overlay con análisis real** (O7): `PipelineOverlayDataSource` ejecuta
  reglas + confianza tras evaluar cada snapshot y expone en `OverlayUiState`
  `offerType`, `confidence` y `ruleEvaluation`; `OverlayContent` muestra el
  tipo de oferta, el % de confianza y "Información insuficiente" cuando la
  confianza no es accionable.
- **Panel de depuración ampliado** (O8): sección "Análisis" con tipo de
  oferta, confianza (%, nivel y razones) y veredicto de cada regla con color
  semáforo; filas Detección/Reglas en captura y promedios.
- **Métricas de rendimiento por etapa** (O10): `detectionMillis` en
  `ProcessingMetrics`/`OfferTiming` y `rulesMillis` en `OfferTiming`
  (promedios del tracker); regex del parser ya compiladas una sola vez en el
  companion object.
- **Dataset de prueba** (O6): `core/capture/src/test/resources/test-images/`
  con 9 placeholders de escenarios Uber (uberx, comfort, moto, xl,
  reservation, radar, bonus, night, invalid) + `README.md` del dataset.
- **Tests**: `OfferDetectionEngineTest` (12), `OfferParserOrchestratorTest`
  (9), `RuleEngineTest`, `ConfidenceEngineTest`, `OfferValidatorTest`,
  ampliado `PipelineOverlayDataSourceTest` (8, incluye tipo/confianza/reglas).

### Cambiado

- `:core:capture` depende de `:core:platform`; `CaptureModule` liga
  `PlatformOfferParser` (reemplaza a `FakeParser` en el flujo real). El
  snapshot transporta `detectionMillis` y `rawData = "type={OfferType}"`.
- `TripOffer` gana `pickupDistanceKm: Double?` para la regla de recogida.
- `PlatformModule` (nuevo en `:feature:overlay`) provee `OfferDetectionEngine`,
  los parsers (`List<@JvmSuppressWildcards OfferTypeParser>`), el orquestador
  y el `RuleEngine` (resuelve el multibinding de `List<OfferRule>`).
- `RuleEngine` pierde el `@Inject` con default args (doble constructor en
  Dagger) y se provee explícitamente, siguiendo el patrón de O1/O2.
- `DebugPanelViewModel` reestructura los combines para observar
  `OverlayDataSource.uiState`.

### Notas técnicas

- Reglas con datos faltantes devuelven PASS "no disponible" sin bloquear la
  oferta; la confianza LOW no debe traducirse en ACCEPT/REJECT.
- Los parsers especializados solo se aplican a Uber; otras plataformas caen al
  extractor genérico.
- Verificación en verde: `ktlintCheck` (todos los módulos), tests unitarios de
  `:domain`/`:core:platform`/`:core:capture`/`:feature:overlay`/`:app` y
  `lintDebug`/`assembleDebug`.

## [v0.8.0] — 2026-07-31

### Añadido

- **Evaluación detallada con motor real en `:domain`** (O1/O6):
  `ProfitEvaluationEngine` reutiliza `ProfitEngine` (delega, no duplica
  fórmulas) y deriva los costos desde `DriverConfig`: `costPerKm` =
  combustible + mantenimiento + costos adicionales, `costPerMinute` y
  `costPerTrip` pasan tal cual de la configuración. Los umbrales de decisión se
  toman **solo de `DriverConfig.thresholds`** (sin constantes), garantizando que
  la decisión respete los objetivos del conductor.
- **`RecommendationEngine`** (O2): decisión accionable
  `ACCEPT`/`REJECT`/`WARNING` a partir de `Decision` +
  `ProfitMetrics`, con `OfferRecommendation` que incluye motivo principal,
  métricas usadas (ganancia/km, ganancia/hora) y **% de confianza**
  (`(50 + margen/3).coerceIn(50, 98)`, WARNING = 50).
- **Modelos de evaluación**: `ProfitBreakdown` (costo total y por
  componente), `ProfitEvaluationDetailed` (evaluación + desglose),
  `OfferEvaluationResult` (evaluación + desglose + recomendación) y
  `OfferEvaluationRecord` (registro del historial).
- **`OfferEvaluationRepository`** (O4): contrato en `:domain` +
  `InMemoryOfferEvaluationRepository` (O(1), sincronizado, retiene las últimas
  100 ofertas con ids incrementales, sin Room).
- **Historial conectado al flujo real** (`PipelineOverlayDataSource` reescrito):
  cada snapshot capturado se mapea a `TripOffer` (con `texts` de OCR),
  se evalúa con `EvaluateDetailedOfferUseCase`, se persiste en
  `OfferEvaluationRepository` y se publica en el overlay con su recomendación.
  `stop()` limpia el estado y cancela el TTL.
- **Overlay con recomendación** (O3): `OverlayContent` muestra
  `RecommendationBadge` (ACEPTAR/RECHAZAR/REVISAR con color semáforo), precio,
  ganancia, $/hora, $/km, resumen del viaje, costo estimado y motivo principal
  con % de confianza. `OverlayUiState.recommendation` y
  `ProfitState.fromRecommendation`/`recommendationLabel` en `:core:ui`.
- **Métricas de rendimiento por oferta** (O8): `OfferTiming`
  (captura/OCR/parseo/evaluación/overlay/total) registrado por
  `DefaultCapturePipeline` (tres primeras etapas) y por
  `PipelineOverlayDataSource` (evaluación/overlay); `OfferPerformanceTracker`
  + `InMemoryOfferPerformanceTracker` retiene las últimas 100 ofertas y expone
  **promedios de las últimas 20**.
- **Panel de depuración ampliado** (O5): sección "Última oferta" (recomendación,
  plataforma, precio, distancia, duración, motivo, texto OCR truncado a 200
  chars, parser y timestamp) y "Rendimiento (promedio últimas 20 ofertas)" con
  captura/OCR/parseo/evaluación/overlay/total en ms.
- `OfferSnapshot` ahora transporta los **textos OCR** (`texts: List<String>`) y
  `FakeParser` los puebla desde el evento de captura.
- **Tests**: `ProfitEvaluationEngineTest` (7), `RecommendationEngineTest` (4),
  `InMemoryOfferPerformanceTrackerTest` (6, incluye promedio de ventana),
  `InMemoryOfferEvaluationRepositoryTest` (5, límite 100), reescritura de
  `PipelineOverlayDataSourceTest` (7, con el caso de uso detallado) y
  ampliación de `DefaultCapturePipelineTest` (registro de tiempos en el tracker).

### Cambiado

- `PipelineOverlayDataSource` pasa a depender de `EvaluateDetailedOfferUseCase`,
  `OfferEvaluationRepository` y `OfferPerformanceTracker` (bindings nuevos en
  `CaptureModule`).
- `DebugPanelViewModel` reconstruido con combines anidados para agregar
  `performanceTracker.averages` e `historial (última oferta)` sin superar los 5
  flows por `combine`.
- Verificación en verde: `ktlintCheck`, `testDebugUnitTest`, `:core:capture:test`,
  `:domain:test`, `lintDebug`, `assembleDebug` y
  `:core:capture:android:assembleDebugAndroidTest`.

### Notas técnicas

- Dos historiales conviven: `OfferHistoryRepository` (Room, persistente, para
  `:feature:history`) y el nuevo `OfferEvaluationRepository` (memoria, para el
  overlay y el panel de depuración). El motor de evaluación detallada es el que
  alimenta al overlay en tiempo real.
- `EvaluateDetailedOfferUseCase` usa `DriverConfig.default()` si el conductor
  aún no está configurado (evita el `require` del `ProfitEngine`).

## [v0.7.0] — 2026-07-31

### Añadido

- **Nuevo módulo `:core:capture:android`** (Android library, Hilt): captura de
  pantalla real con MediaProjection, desacoplada de la UI.
  - `ScreenCaptureProvider` (interfaz) + `MediaProjectionScreenCaptureProvider`:
    posee el token de proyección, el `VirtualDisplay` + `ImageReader` (RGBA_8888)
    y el último frame; la UI solo entrega el resultado del permiso y lee
    `isProjecting: StateFlow<Boolean>`.
  - `MediaProjectionService`: **Foreground Service tipo `mediaProjection`**
    (Android 14+) con `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`; arranca desde la
    Activity tras el consentimiento y completa la proyección.
  - `MediaProjectionScreenCapture` (implementación de `ScreenCapture`): captura
    el frame real y lo comprime a PNG para el OCR; degrada a `request.texts`
    cuando no hay proyección o frame.
  - `DebugCaptureMetrics` (solo loguea en builds `FLAG_DEBUGGABLE`).
  - Test instrumentado de humo: sin permiso no proyecta ni captura frames.
- **Captura de pantalla real integrada en el pipeline**: `CapturePipeline`
  ahora expone `snapshots: SharedFlow<OfferSnapshot>` y
  `lastMetrics: StateFlow<ProcessingMetrics>` (tiempos por etapa:
  captura/OCR/parseo/total).
- **Caché de frames por hash de contenido**
  (`CaptureFrameCache` + `InMemoryCaptureFrameCache`, LRU de 32 entradas):
  el pipeline omite capturas idénticas y no repite OCR.
- **Debounce de requests de accesibilidad**
  (`DebounceCaptureScheduler`, `:core:capture`): coalesce los
  `CaptureRequest` de los eventos de accesibilidad (muy frecuentes) y emite
  solo el último tras 400 ms de silencio; `CaptureAccessibilityService` lo usa.
- **Overlay conectado al estado real del pipeline**
  (`PipelineOverlayDataSource`): consume `pipeline.state` y `pipeline.snapshots`,
  evalúa con `EvaluateOfferUseCase` (motor real) y expone
  `OverlayUiState.status` (`OverlayState`) + `visible` + `evaluation`. Sustituye
  a `SimulatedOverlayDataSource`.
- **Estados del overlay en la UI** (`OverlayContent`): `StatusLabel` para
  WAITING/CAPTURING/PROCESSING/ERROR (y resultado evaluado cuando existe);
  `OverlayService` muestra la tarjeta de estado mientras `visible`.
- **Permiso de captura en Home** (`HomeViewModel`/`HomeScreen`): botón "Permitir
  captura de pantalla" (lanzador `StartActivityForResult` → `startProjection`)
  y "Detener captura"; `OverlayManager` expone `projectionActive`,
  `createScreenCaptureIntent()`, `startProjection(...)`, `stopProjection()`.
- **Métricas de rendimiento en el panel de depuración**
  (`DebugPanelViewModel`/`DebugPanelScreen`): filas Captura/OCR/Parseo/Total
  desde `pipeline.lastMetrics`.
- Pruebas unitarias nuevas: `DebounceCaptureSchedulerTest`,
  `InMemoryCaptureFrameCacheTest`, `PipelineOverlayDataSourceTest`; ampliado
  `DefaultCapturePipelineTest` (caché, snapshots y métricas).

### Cambiado

- `DefaultCapturePipeline` ahora requiere `cache: CaptureFrameCache` y
  `metrics: CaptureMetrics`; transiciona `WAITING→CAPTURING→PROCESSING→WAITING/ERROR`.
- `CaptureAccessibilityService` (`:feature:overlay`) reescrito para pasar por el
  `DebounceCaptureScheduler` (collector en `onCreate`, cancel en `onDestroy`;
  límites 400 nodos / 200 chars / 80 textos).
- `CaptureModule` (`:feature:overlay`): elimina el binding de `ScreenCapture`
  (ahora lo provee `:core:capture:android`) y añade el binding de
  `CaptureFrameCache`.
- Eliminados `AccessibilityScreenCapture` y `SimulatedOverlayDataSource`
  (sustituidos por la captura real y `PipelineOverlayDataSource`).
- `settings.gradle.kts`: registrado `:core:capture:android` (11 módulos);
  `gradle/libs.versions.toml`: añadido `kotlinx-coroutines-test`.

### Notas técnicas

- `:core:capture` sigue siendo Kotlin puro: la caché, el scheduler, las métricas
  y el pipeline no dependen de Android; las piezas Android viven en
  `:core:capture:android`.
- Todo el análisis es local; la captura de pantalla solo ocurre tras el
  consentimiento explícito del sistema (MediaProjection) y no sale nada del
  dispositivo.

## [v0.6.0] — 2026-07-31

### Añadido

- **`CaptureAccessibilityService`** en `:feature:overlay`: servicio de
  accesibilidad dedicado a la captura, completamente desacoplado de la UI (no
  publica en el overlay ni conoce estados de interfaz). Solo observa, construye
  `CaptureRequest` y los envía al pipeline. Reutiliza la configuración de
  accesibilidad existente (solo lectura, `canPerformGestures=false`).
- **`OverlayState`** (`:core:capture`): estados mínimos del ciclo de vida —
  `DISABLED`, `WAITING`, `CAPTURING`, `PROCESSING`, `ERROR`.
- **`CapturePipeline`** (`:core:capture`): pipeline de captura de extremo a
  extremo desacoplado de Android.
  - `CaptureRequest` / `ScreenFrame`: modelos de entrada y contenido capturado.
  - `ScreenCapture` (contrato): captura el frame; `AccessibilityScreenCapture`
    (Android) usa el texto observado; el contrato queda listo para
    MediaProjection.
  - `OcrEngine` (contrato) + `MlKitOcrEngine` (Android): reconocimiento óptico
    con **ML Kit** (`com.google.mlkit:text-recognition:16.0.1`) a través de una
    abstracción sustituible y testeable.
  - `DefaultCapturePipeline`: ScreenCapture → OCR (si hay imagen) → OfferParser
    → CaptureRepository; expone `OverlayState` y Feature Flag `OCR`.
- **Imágenes de prueba** en `core/capture/src/test/resources/test-images/` y
  pruebas unitarias del pipeline (con falso OCR/captura/parser): flujo con
  texto, con imagen + OCR, flags `CAPTURE`/`OCR`, fallos de captura/OCR/parser
  y carga de imagen real desde recursos.
- **Panel de depuración** (`:app`): fila `OCR` en infraestructura y `Estado del
  pipeline` (`OverlayState`); el flag `OCR` aparece en los toggles.
- Dependencia **ML Kit OCR** en `gradle/libs.versions.toml` y
  `feature/overlay/build.gradle.kts`.

### Corregido

- Las incidencias de ktlint en `DebugPanelScreen.kt` (import sin usar y líneas
  > 120) ya quedaron resueltas en v0.5.0; `ktlintCheck` pasa sin incidencias.


### Añadido

- **Nuevo módulo `:core:capture`** (Kotlin puro): Plataforma de Captura
  (infraestructura) lista para conectar en el futuro el parser real, OCR y el
  `ProfitEngine`. NO implementa OCR, ML Kit, IA, regex ni interpretación de
  pantallas.
  - `WindowObserver`: contrato de observación de ventanas que emite eventos
    mediante Flow (`CaptureWindowEvent`).
  - `OfferParser` (interfaz) + `FakeParser`: genera snapshots simulados para
    validar el flujo completo; no interpreta pantallas reales.
  - `OfferSnapshot` (inmutable) con `SnapshotSource.FAKE`/`REAL` y campo
    `rawData` reservado; `OfferCaptureSession` con estado ACTIVE/CLOSED.
  - `CaptureRepository` (interfaz) + `InMemoryCaptureRepository` (buffer de 50
    snapshots), preparado para una futura implementación persistente.
  - `OfferCaptureCoordinator`: coordina captura de extremo a extremo
    (eventos → sesión → parser → repositorio), completamente desacoplado de
    Android; expone `CaptureState` (sesión activa, último snapshot, tiempo de
    procesamiento, eventos recientes).
  - **Feature Flags** (`FeatureFlag`, `FeatureFlags` + `InMemoryFeatureFlags`):
    `ACCESSIBILITY`, `OVERLAY`, `CAPTURE`, `PARSER`, `DEBUG_PANEL`, todos
    configurables en caliente.
  - **Logging centralizado** (`SircLogger`), deshabilitable en producción.
- **Implementación Android de captura** en `:feature:overlay`:
  `AccessibilityWindowObserver` (publica los eventos del servicio como Flow) y
  `AndroidSircLogger` (solo emite en builds de desarrollo). El
  `SircAccessibilityService` reenvía cada cambio de ventana relevante al
  pipeline de captura sin interpretar nada y sin tocar el flujo existente.
  DI en `CaptureModule` (`@Binds`).
- **Panel de depuración** en `:app`: destino `Debug` (icono de llave) con
  estado de Accessibility/Overlay/Captura/Parser, toggles de Feature Flags,
  último snapshot, tiempo de procesamiento, memoria aproximada y eventos
  recientes. Visible solo si `DEBUG_PANEL` está habilitado.
- `OfferCaptureCoordinator.start()` se ejecuta al arrancar la app
  (`SircApplication`); el panel puede iniciar/detener/limpiar la captura.
- Pruebas unitarias de `:core:capture` (12): coordinador (sesión, flags,
  reset, start/stop), parser simulado, repositorio en memoria y feature flags.
- CI: el paso "Unit tests" ahora corre `./gradlew test` (incluye los módulos
  JVM puros).

### Cambiado

- `settings.gradle.kts`: se registra `:core:capture` (10 módulos).
- `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `.ai/CONTEXT.md` y
  `.ai/DECISIONS.md` actualizados.
- Nuevo `docs/testing/SPRINT_04_MANUAL_TEST.md` con el manual de prueba.

### Notas técnicas

- `:core:capture` es Kotlin puro (depende de `:domain`, coroutines y
  `javax.inject`), igual que `:domain` y `:core:platform`.
- Los flags default a `true` en memoria (entorno de desarrollo); la
  implementación queda lista para deshabilitar piezas en producción.

## [v0.4.0] — 2026-07-31

### Añadido

- **Flujo de configuración inicial (onboarding)** en nuevo módulo
  `:feature:onboarding`: 6 pasos (Perfil, Vehículo, Costos, Plataformas,
  Objetivos, Resumen) que capturan perfil, vehículo, combustible, otros costos,
  plataformas activas y ganancias mínimas por km/hora.
- **Modelos de dominio**: `DriverConfig` (agregado completo), `DriverProfile`,
  `DriverVehicle`, `FuelType`, `AdditionalCost`.
- **Persistencia completa en Room**: `driver_config` (v2) almacena toda la
  configuración en una fila; migración 1→2 con reconstrucción de tabla
  (compatible con SQLite antiguo). Codecs de texto para plataformas y costos
  adicionales en `Mappers.kt`.
- **Gating de onboarding**: `RootViewModel` + `SircRoot` en `:app` muestran el
  onboarding en la primera apertura y la app principal solo cuando el conductor
  ya está configurado (`observeIsConfigured()`).
- Use cases ampliados: `GetDriverConfigUseCase` (config completa +
  `observeIsConfigured`) y `SaveDriverConfigUseCase.save(config)`.
- Objetivo de ganancia mínima por **km** como segundo indicador del MVP
  (`DecisionThresholds.minProfitPerKm`).
- Pruebas del codec de persistencia (`DriverConfigCodecTest` en `:data`) y del
  nuevo umbral por km (`ProfitEngineTest`).

### Cambiado

- `DecisionThresholds.minProfit` → `minProfitPerKm`; `ProfitEngine` decide con
  ganancia/km y ganancia/hora.
- `DriverCosts` deja de guardar `currency` (única fuente: `DriverProfile`).
- `SettingsViewModel` opera sobre el `DriverConfig` completo (preserva perfil,
  vehículo y plataformas al guardar costos/umbrales); Ajustes edita moneda
  desde el perfil.
- `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `.ai/CONTEXT.md` y
  `.ai/DECISIONS.md` actualizados.

## [v0.3.0] — 2026-07-31

### Añadido

- **Tokens de Design System** en `:core:ui`: `SircSpacing` (escala 4dp:
  4/8/12/16/24/32) y `SircElevations` (Card 2dp, CardProminent 4dp, Overlay 8dp).
- **`ProfitState`** (`theme/ProfitState.kt`): semántica de decisión → estado
  visual (etiqueta `CONVIENE`/`DUDOSO`/`NO CONVIENE` + color semáforo) vía
  `fromDecision`.
- **Componentes overlay** en `:core:ui`: `ProfitIndicator` (píldora semáforo),
  `OverlayCard` (contenedor presentacional con opacidad/compacto/borde/
  elevación) y `OverlayCardContent` (cabecera + cierre opcional); `MetricCell`
  y `MetricValue` para métricas del overlay.
- **`@Preview` en todos los componentes** de `:core:ui` (dark scheme) y KDoc.
- **Pruebas unitarias de `:core:ui`**: mapeo `ProfitState.fromDecision`,
  etiquetas/colores de semáforo y valores de paleta (`ProfitStateTest`).
- Dependencia `testImplementation(libs.junit)` en `:core:ui`.

### Cambiado

- `DecisionBadge` delega en `ProfitIndicator` (misma API, fuente única de
  estilo semáforo).
- `OverlayContent` (overlay) ahora compone los componentes de `:core:ui`
  (`OverlayCard` + `OverlayCardContent` + `MetricCell`) en lugar de duplicar
  tarjetas/métricas.
- `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `.ai/CONTEXT.md` y
  `.ai/DECISIONS.md` actualizados.

### Notas técnicas

- `OverlayCard` es presentacional: no conoce de dominio; la plataforma,
  decisión y métricas entran como contenido/slot.
- El icono de cierre usa `Icons.Filled.Close` (core icons, ya disponible
  vía Material 3); `:core:ui` no depende de `material-icons-extended`.

## [v0.2.0] — 2026-07-31

### Añadido

- **`PermissionManager`** (interfaz) + **`AndroidPermissionManager`**:
  detección de permisos (overlay, accesibilidad, notificaciones en Android 13+,
  optimización de batería) y apertura de los ajustes correspondientes.
- **`OverlayManager`** (interfaz) + **`AndroidOverlayManager`**: fachada de alto
  nivel con estado de ejecución (`isRunning`), `start()`/`stop()` y acceso a
  permisos; consumida por las ViewModels.
- **`OverlayDataSource`** (interfaz) + **`SimulatedOverlayDataSource`**: emite
  ofertas simuladas por plataforma cada 20 s, evaluadas con el `ProfitEngine`
  real (`EvaluateOfferUseCase`); se oculta tras `OverlayConfig.ttlSeconds`.
  No persiste en el historial.
- **`OverlayViewModel`** (`@HiltViewModel`) y **`OverlayModule`** (`@Binds`
  para `PermissionManager`, `OverlayManager` y `OverlayDataSource`).
- **Pantalla Diagnóstico** (`DiagnosisViewModel` + `DiagnosisScreen`, ruta
  `diagnosis`): indicadores 🟢/🔴 de overlay, accesibilidad, servicio en
  ejecución, notificaciones y batería, más vista previa con datos simulados.

### Cambiado

- **`OverlayService`** ya no depende de `OfferEvaluator`: consume
  `OverlayDataSource` (datos simulados); arranca la simulación en
  `onStartCommand` y la detiene en `onDestroy`.
- **`OverlayController`** pasa a controlar únicamente el servicio
  (arranque/parada + estado de ejecución); la lógica de permisos se extrae a
  `PermissionManager`.
- **`HomeViewModel`/`HomeScreen`** usan `PermissionManager` + `OverlayManager` y
  muestran estado de notificaciones, batería y ejecución del overlay.
- Navegación con 4 destinos (agregado Diagnóstico).
- `docs/ARCHITECTURE.md`, `docs/ROADMAP.md`, `.ai/CONTEXT.md` y
  `.ai/DECISIONS.md` actualizados.

### Notas técnicas

- `OfferEvaluator.uiState` queda sin consumidor hasta reconectar el análisis
  real de ofertas; la persistencia de historial se mantiene intacta.

## [v0.1.0] — 2026-07-31

### Añadido

- **Scaffolding Gradle**: proyecto multi-módulo con Version Catalog
  (`gradle/libs.versions.toml`), wrapper Gradle 8.11.1, `.editorconfig` y
  `.gitignore`.
- **Módulo `:domain`** (Kotlin puro): modelos de dominio (`TripOffer`,
  `ProfitEvaluation`, `ProfitMetrics`, `DriverCosts`, `DecisionThresholds`,
  `OverlayConfig`, `OfferHistoryEntry`, `Decision`, `RidePlatform`), motor de
  rentabilidad `ProfitEngine` (función pura), use cases y contratos de
  repositorio.
- **Módulo `:core:platform`** (Kotlin puro): `OfferTextParser` (heurística de
  montos, distancias y duraciones), `PlatformExtractor`,
  `GenericPlatformExtractor`, `PlatformDescriptors` y `ExtractorRegistry` para
  Uber, DiDi, Cabify e InDrive.
- **Módulo `:core:ui`**: tema SIRC (`SircTheme`, `SircColors`, `SircTypography`)
  y componentes (`DecisionBadge`, `StatusDot`, `SectionCard`, `LabeledValue`).
- **Módulo `:data`**: Room (`SircDatabase` con `driver_config`,
  `overlay_config`, `offer_history`), DAOs, mappers, repositorios concretos
  `Default*Repository` y módulos Hilt (`DatabaseModule`, `RepositoryModule`).
- **Módulo `:feature:overlay`**: `SircAccessibilityService` (solo lectura),
  `OverlayService` (Foreground Service con `TYPE_APPLICATION_OVERLAY`),
  `OfferEvaluator`, `OfferEventBus`, `OverlayController`, `OverlayUiState` y
  `OverlayContent`.
- **Módulo `:feature:settings`**: `SettingsViewModel` y `SettingsScreen`
  (costos, umbrales, configuración del overlay).
- **Módulo `:feature:history`**: `HistoryViewModel` y `HistoryScreen`
  (historial Room de ofertas evaluadas).
- **Módulo `:app`**: `SircApplication`, `MainActivity`, navegación (`SircApp`),
  `HomeViewModel` y `HomeScreen`.
- **Pruebas unitarias**: `ProfitEngineTest` (6) y `OfferTextParserTest` (8).
- **CI**: workflow de GitHub Actions (`ktlintCheck`, `testDebugUnitTest`,
  `lintDebug`, `assembleDebug` + artefactos APK y reporte de lint).
- **Documentación**: `README.md`, `docs/ARCHITECTURE.md`,
  `docs/GOOGLE_PLAY_COMPLIANCE.md`.

### Notas técnicas

- SDK compile/target 35, minSdk 24, Java/Kotlin 17.
- Persistencia local con Room (`exportSchema = true`, esquemas en `data/schemas/`).
- Cumplimiento de la política de accesibilidad de Google Play (solo lectura).

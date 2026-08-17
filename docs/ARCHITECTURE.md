# Arquitectura

> Documentación derivada del código existente. Solo describe lo que el
> proyecto realmente implementa.

## Principios

- **Clean Architecture**: separación estricta Presentation / Domain / Data / Core.
- **MVVM**: la UI observa `StateFlow` expuestos por ViewModels; el overlay
  consume su estado desde `OverlayDataSource` (`StateFlow`).
- **Modularización por feature y por capa**.
- **Dependencias siempre hacia adentro**: `domain` es Kotlin puro y no depende
  de nada Android.
- SOLID, DRY, KISS. Código limpio y testeable.
- Bajo consumo de batería y rendimiento (límites duros y deduplicación en el
  flujo de accesibilidad).

> **Principio de producto LOCAL-FIRST (Roadmap Gate, 16-ago-2026; aún sin
> cambios de código):** el pipeline de ofertas es 100 % local (sin salida de
> datos de pantalla). En la etapa **E1b** (ver `SECURITY_MODEL.md`, ROADMAP,
> `BACKEND_ARCHITECTURE.md`) se añadirán componentes remotos de
> cuenta/suscripción/entitlement/integridad implementados sobre **Supabase**
> (Auth + RLS + Edge Functions) y expuestos como **contratos de `:domain`**
> (p. ej. `EntitlementRepository`, `AuthRepository`), implementados en módulos
> Android, sin contaminar `:domain`/`:core:platform`/`:core:capture` (que deben
> seguir Kotlin puro). La verificación de compra usa la Google Play Developer
> API v2 (`purchases.subscriptionsv2.get`) desde las Edge Functions;
> `purchases.subscriptions.get` está deprecada. Decisiones registradas:
> D14.1–D14.4 en `.ai/DECISIONS.md`.

## Estructura de módulos

| Módulo | Capa | Tipo | Responsabilidad |
|---|---|---|---|
| `:app` | Presentation (entrada) | `com.android.application` | `SircApplication`, `MainActivity`, navegación (`SircApp`), Home. |
| `:domain` | Domain | `kotlin("jvm")` | Modelos, `ProfitEngine`, motores de evaluación y recomendación, use cases, contratos de repositorio. **Sin Android.** |
| `:data` | Data | `com.android.library` | Room (`SircDatabase`, entidades, DAOs), repositorios concretos, DI de datos. |
| `:core:platform` | Core | `kotlin("jvm")` | Parser de texto y extractores por plataforma. **Sin Android.** |
| `:core:capture` | Core | `kotlin("jvm")` | Plataforma de captura: pipeline de extremo a extremo (screen capture, OCR, parser, repositorio), observador de ventanas, sesión/snapshot, estados del overlay, coordinador, caché de frames, debounce, métricas, feature flags y logging. **Sin Android.** |
| `:core:capture:android` | Core | `com.android.library` | Implementación Android de la captura de pantalla: MediaProjection (provider + FGS `mediaProjection`), entrada de captura `MediaProjectionCaptureInput`, métricas de depuración. |
| `:core:ui` | Core | `com.android.library` | Design system Compose: tema y componentes. |
| `:feature:overlay` | Feature | `com.android.library` | Accessibility Service, Overlay Service, pipeline de evaluación y UI del overlay. |
| `:feature:settings` | Feature | `com.android.library` | Configuración de costos, umbrales e indicadores. |
| `:feature:history` | Feature | `com.android.library` | Historial de ofertas evaluadas. |
| `:feature:onboarding` | Feature | `com.android.library` | Flujo de configuración inicial del conductor. |

## Grafo de dependencias

```
app ──► feature:overlay ──► core:platform ─► domain
  │          │                 │
  │          └──────► data ────┘  (data implementa contratos de domain)
  ├──► feature:settings ─► data
  ├──► feature:history  ─► data
  ├──► feature:onboarding ─► data
  ├──► core:capture ─────► domain
  ├──► core:capture:android ──► core:capture ─► domain
  │         (y feature:overlay dep. de core:capture:android)
  └──► core:ui ──────────► domain (solo tipos)
```

Reglas derivadas del grafo real:

- `domain` es dependencia común de todos los módulos.
- `data` implementa los contratos de `domain` con Room; ningún módulo depende de
  `data` por encima de su capa (los features sí usan `data` directamente).
- `core:platform` (puro Kotlin) desacopla las plataformas del producto.
- `core:capture` (puro Kotlin) desacopla la infraestructura de captura de
  Android: el servicio de accesibilidad solo alimenta un `WindowObserver`.
- `core:ui` provee el design system Compose y depende de `domain` solo para
  tipos (`Decision`).
- La UI de cada feature depende de su ViewModel; los ViewModels dependen de
  `domain` (use cases) — nunca de `data` directamente.

## Capas y responsabilidades

### Domain (`:domain`) — Kotlin puro

- `model/` — `TripOffer`, `RidePlatform`, `ProfitMetrics`, `ProfitEvaluation`,
  `Decision`, `DriverCosts`, `DecisionThresholds`, `OverlayConfig`,
  `OfferHistoryEntry`, `DriverConfig`, `DriverProfile`, `DriverVehicle`,
  `FuelType`, `AdditionalCost`, y los de evaluación (SPRINT 7):
  `Recommendation` (ACCEPT/REJECT/WARNING), `ProfitBreakdown` (costo total y por
  componente), `ProfitEvaluationDetailed` (evaluación + desglose),
  `OfferRecommendation` (recomendación + motivo principal + métricas usadas +
  % de confianza), `OfferEvaluationResult` y `OfferEvaluationRecord` (registro
  del historial en memoria).
- `engine/ProfitEngine` — función pura: oferta + costos + umbrales → evaluación.
  Sin estado ni I/O; 100 % testeable. Decide con ganancia/km y ganancia/hora
  (`DecisionThresholds`).
- `engine/ProfitEvaluationEngine` — motor de evaluación detallada (SPRINT 7):
  **delega en `ProfitEngine`** (no duplica fórmulas) y deriva los costos desde
  `DriverConfig`: `costPerKm` = combustible + mantenimiento + costos
  adicionales; `costPerMinute` y `costPerTrip` pasan de la configuración. Los
  umbrales de decisión se toman **solo de `DriverConfig.thresholds`** (sin
  constantes), respetando los objetivos del conductor.
- `engine/RecommendationEngine` — de `Decision` + `ProfitMetrics` obtiene la
  recomendación accionable `ACCEPT`/`REJECT`/`WARNING` con motivo principal,
  métricas usadas y % de confianza (`(50 + margen/3).coerceIn(50, 98)`;
  WARNING = 50).
- `repository/` — contratos: `DriverConfigRepository`, `OverlayConfigRepository`,
  `OfferHistoryRepository` y `OfferEvaluationRepository` (SPRINT 7).
- `session/` (SPRINT 9) — `CaptureSessionManager` (`@Singleton`, máquina de
  estados pura): iniciar/pausar/reanudar/detener/reset + `SessionStatus`
  (IDLE/ACTIVE/PAUSED) + `SessionStats` (duración activa en vivo con reloj
  inyectable, ofertas procesadas/aceptadas/rechazadas, errores).
- `usecase/` — orquestación fina: `EvaluateOfferUseCase`,
  `EvaluateDetailedOfferUseCase` (evalúa una oferta completa con motores + costos
  derivados y devuelve `OfferEvaluationResult`),
  `Get/SaveOverlayConfigUseCase`, `Get/SaveDriverConfigUseCase`,
  `Observe/Clear/AddOfferHistoryUseCase`; SPRINT 9: `HistoryFilters`/
  `HistoryFilter` (filtros del Historial) y `HistoryStatsCalculator`
  (estadísticas del Dashboard: aceptación, $/hora, $/km, procesamiento,
  confianza y agrupación diaria).

### Data (`:data`)

- Room: `SircDatabase` (tablas `driver_config`, `overlay_config`,
  `offer_history`), entidades, DAOs y mappers (`Mappers.kt`). **Versión 3**
  (SPRINT 9) con migraciones 1→2 (reconstrucción de tabla) y **2→3**
  (`offer_history` +9 columnas de análisis detallado; `overlay_config` +
  `historyLimit` default 500).
- `driver_config` guarda el agregado `DriverConfig` en una fila: perfil,
  vehículo, costos (combustible, mantenimiento, adicionales codificados),
  plataformas (codificadas) y umbrales. La existencia de la fila = conductor
  configurado.
- `offer_history` (SPRINT 9) guarda el análisis detallado de cada oferta:
  tipo, confianza (%, nivel), resumen de reglas, motivos, recomendación y
  tiempos (procesamiento/evaluación/reglas). `OfferHistoryDao` con
  `trimToLimit(limit)`/`count()`; `DefaultOfferHistoryRepository` recorta
  automáticamente al insertar según `OverlayConfig.historyLimit`.
- Repositorios concretos `Default*Repository` implementan los contratos de
  dominio y aplican valores por defecto si no existe fila.
- DI: `DatabaseModule` (DB + DAOs + migraciones) y `RepositoryModule` (`@Binds`).
- Esquema Room versionado en `data/schemas/` (`exportSchema = true`).

### Core:platform (`:core:platform`) — Kotlin puro

Motor de análisis de pantallas (SPRINT 1 extractores + SPRINT 8 detección y
parsers + WP-E3-01 descriptores por plataforma):

- `OfferTextParser` — heurística pura: extrae candidatos de monto, distancias y
  duraciones con regex precompiladas y límites de rango.
- **Detección de pantalla (SPRINT 8)**: `OfferDetectionEngine` clasifica el texto
  visible en `ScreenType` (HOME/REQUEST/TRIP/NAVIGATION/OFFLINE/ERROR/UNKNOWN)
  con keywords ponderadas; solo `REQUEST` produce oferta. `ScreenDetection`
  expone tipo, keywords, confianza e `isRequest`. `defaultRules()` y
  `normalize()` alimentan los descriptores y el orquestador.
- **`OfferTypeVariant` (WP-E3-01)** — variante de oferta `(type, keywords,
  refine?)`; semilla del futuro subdescriptor `OfferTypeDescriptor`.
- **`PlatformDescriptor` (WP-E3-01)** — descriptor declarativo por plataforma:
  `platform`, `packageNames`, `detectionRules`, `offerTypes`,
  `extractorKeywords` y `defaultCurrency`. Estructura preparada para evolucionar
  a subdescriptores (`IdentityDescriptor`, `DetectionDescriptor`,
  `OfferTypeDescriptor`, `ExtractionDescriptor`, `LocalizationDescriptor`) sin
  romper la API pública; no se implementan todavía.
- **`PlatformDescriptorRegistry` (WP-E3-01)** — único validador y punto de
  entrada: asocia descriptores por plataforma y packageName y precompila en
  `init` los motores de detección, parsers de variantes y extractores (sin
  recálculo por frame). Valida en construcción (`IllegalArgumentException`,
  nunca en parseo): reglas/keywords vacías, plataformas/aliases duplicados,
  monedas inválidas (`[A-Z]{3}`), descriptores sin regla `ScreenType.REQUEST` y
  tipos de oferta duplicados. `PlatformDescriptors.all()` registra
  UBER/DIDI/CABIFY/INDRIVE.
- `PlatformExtractor` (interfaz) + `GenericPlatformExtractor` — estrategia por
  plataforma basada en las keywords del descriptor.
- **`OfferParserOrchestrator` (SPRINT 8 + WP-E3-01)**: 100 % descriptor-driven,
  sin referencias a plataformas concretas. Detecta (motor del descriptor) →
  prueba las variantes del descriptor en orden de especificidad → cae al
  extractor genérico; devuelve `ParsedOffer.none()` si la plataforma no está
  registrada o la pantalla no es `REQUEST`. `ParsedOffer` incluye tiempos de
  detección/parsing (Debug).
- **Agregar una plataforma** = registrar un `PlatformDescriptor`; sin ramas, sin
  parsers especializados por plataforma y sin tocar el núcleo.

### Core:capture (`:core:capture`) — Kotlin puro

Plataforma de captura (SPRINT 4 infraestructura + SPRINT 5 pipeline): observa los
cambios de ventana de las plataformas, captura contenido, aplica OCR si hay
imagen y produce snapshots; desacoplado por completo de Android.

Flujo de captura (coordinador, SPRINT 4):

```
CaptureAccessibilityService (solo lectura, no interpreta)
      ▼ emite cambios de ventana
AccessibilityWindowObserver (:feature:overlay, Flow)
      ▼
OfferCaptureCoordinator ──► WindowObserver.windowEvents (Flow)
      · mantiene la OfferCaptureSession activa
      · consume Pipeline.snapshots (no parsea ni guarda)
      · CaptureRepository (InMemoryCaptureRepository)
      · CaptureState: sesión, último snapshot, tiempo de procesamiento,
        eventos recientes
```

Pipeline de captura (SPRINT 5 + SPRINT 6 + WP-E3-03, captura de pantalla real):

```
CaptureAccessibilityService (solo lectura, desacoplado de la UI)
      ▼ CaptureRequest (packageName + textos + imagen)
DebounceCaptureScheduler (400 ms: coalesce los eventos de accesibilidad)
      ▼
CaptureInput (MediaProjectionCaptureInput: enriquece el request con la
      imagen PNG si la proyección está activa; degrada a textos si no)
      ▼
CapturePipeline (DefaultCapturePipeline)
      1. caché de frames (InMemoryCaptureFrameCache, LRU 32) → si es repetido, se omite
      2. resolveTexts: si hay imagen → OcrEngine → textos (ML Kit, abstraído)
      3. detect → OfferParser (PlatformOfferParser) → OfferSnapshot
      4. CaptureRepository
      ▼
OverlayState (StateFlow): DISABLED → WAITING → PROCESSING → ERROR
snapshots (SharedFlow) · lastMetrics (StateFlow: OCR/detección/parseo/total)
```

- `model/` — `CaptureWindowEvent`, `WindowEventType`, `OfferCaptureSession`
  (ACTIVE/CLOSED), `OfferSnapshot` (inmutable, `SnapshotSource.FAKE`/`REAL`),
  `CaptureState`, `CaptureRequest`, `OverlayState`
  (DISABLED/WAITING/PROCESSING/ERROR).
- `observer/WindowObserver` — contrato: expone `windowEvents: Flow`; la
  implementación Android (`AccessibilityWindowObserver`) vive en
  `:feature:overlay`; `WindowEventPublisher` (puro) emite los eventos desde el
  servicio accesibilidad.
- `input/CaptureInput` (WP-E3-03) — contrato de entrada de captura: recibe la
  corriente base de `CaptureRequest` y la enriquece; `CaptureInputType`
  (ACCESSIBILITY/MEDIA_PROJECTION/UNKNOWN) identifica el origen del texto. La
  implementación real (`MediaProjectionCaptureInput`) vive en
  `:core:capture:android` y degrada a textos de accesibilidad si no hay
  proyección o frame.
- `cache/` — `CaptureFrameCache` + `InMemoryCaptureFrameCache` (LRU de 32
  entradas, clave por hash de contenido de la imagen): evita reprocesar
  capturas idénticas.
- `scheduler/DebounceCaptureScheduler` — coalesce los `CaptureRequest` y emite
  solo el último tras el debounce (400 ms por defecto).
- `metrics/` — `CaptureMetrics` (interfaz, implementada por
  `DebugCaptureMetrics` en `:core:capture:android`) y `ProcessingMetrics`
  (tiempos por etapa, incluye `detectionMillis`) expuestos por el pipeline;
  `OfferTiming`
  (OCR/detección/parseo/reglas/evaluación/overlay/total) y
  `OfferPerformanceTracker` + `InMemoryOfferPerformanceTracker` (SPRINT 7):
  retiene las últimas 100 ofertas y expone promedios de las últimas 20.
- `ocr/OcrEngine` — reconoce texto en una imagen; `MlKitOcrEngine` (Android)
  implementa ML Kit; el pipeline solo lo invoca si la solicitud lleva imagen.
- `parser/OfferParser` + `PlatformOfferParser` (SPRINT 8): parser real que usa
  el `OfferParserOrchestrator` de `:core:platform`; devuelve `null` si la
  pantalla no es una solicitud.
- `repository/CaptureRepository` + `InMemoryCaptureRepository` — guardado
  temporal (buffer 50); la interfaz admite una implementación persistente.
- `coordinator/OfferCaptureCoordinator` — orquesta la captura vía observer;
  sin dependencias de Android (solo interfaces + modelos).
- `pipeline/CapturePipeline` + `DefaultCapturePipeline` — orquesta el flujo
  Cache → OCR → detección → OfferParser → CaptureRepository, con caché y
  métricas; expone `OverlayState`, `snapshots` y `lastMetrics`.
- `flag/` — `FeatureFlag` (OVERLAY, CAPTURE, PARSER, OCR, DETAILED_LOGS,
  DEBUG_PANEL) y `FeatureFlags`/`InMemoryFeatureFlags` (configurables).
- `log/SircLogger` — logging centralizado; `AndroidSircLogger` solo emite en
  builds de desarrollo.

### Core:capture:android (`:core:capture:android`) — captura de pantalla real

Implementación Android de la captura con MediaProjection (SPRINT 6), aislada de
la UI y del pipeline puro:

- `provider/ScreenCaptureProvider` — contrato: `isProjecting: StateFlow<Boolean>`,
  `onProjectionPermissionGranted(resultCode, data)`, `stopProjection()`,
  `captureFrame(): Bitmap?`.
- `provider/MediaProjectionScreenCaptureProvider` — `@Singleton`: posee el token
  de proyección, el `VirtualDisplay` + `ImageReader` (RGBA_8888) y el último
  frame (canal CONFLATED, timeout 400 ms). `initializeProjection` lo invoca el
  servicio tras el consentimiento.
- `projection/MediaProjectionService` — **FGS tipo `mediaProjection`** (Android
  14+, `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`): arranca desde la Activity con el
  resultado del permiso y completa la proyección; canal de notificación
  `sirc_capture`.
- `input/MediaProjectionCaptureInput` — implementa `CaptureInput`: observa la
  corriente base de `CaptureRequest` (accesibilidad, ya debounced) y, si la
  proyección está activa, captura el frame (`captureFrame(): Bitmap?`) y lo
  codifica a PNG para el OCR (`request.copy(imageData = png, origin =
  MEDIA_PROJECTION)`); degrada a `request.texts` si no proyecta.
- `metrics/DebugCaptureMetrics` — `CaptureMetrics` que solo loguea en builds
  `FLAG_DEBUGGABLE`.
- `CaptureAndroidModule` — `@Binds`: `ScreenCaptureProvider` y `CaptureMetrics`.
- Test instrumentado: `MediaProjectionScreenCaptureProviderTest` (sin permiso no
  proyecta ni captura frames).

### Core:ui (`:core:ui`)

- Tema SIRC: `SircTheme`, paleta `SircColors` (semáforo), `SircTypography`,
  `SircSpacing` (escala 4dp), `SircElevations`.
- Estados: `ProfitState` (decisión → etiqueta/color) en `theme/ProfitState.kt`.
- Componentes: `ProfitIndicator` (píldora semáforo, fuente única de estilo),
  `DecisionBadge` (delega en `ProfitIndicator`), `StatusDot`, `SectionCard`,
  `LabeledValue`, `MetricCell`/`MetricValue`, `OverlayCard`/`OverlayCardContent`
  (presentacionales, slots de contenido). Todos con `@Preview` y KDoc.

### Feature:overlay (`:feature:overlay`) — el corazón del MVP

Flujo de datos del overlay (SPRINT 6 + SPRINT 7 — estado y evaluación reales):

```
OverlayDataSource (interfaz: StateFlow<OverlayUiState>, start/stop)
      │
      └── PipelineOverlayDataSource (Singleton)
            · consume CapturePipeline.state → OverlayUiState.status (OverlayState)
            · consume CapturePipeline.snapshots → mapea OfferSnapshot → TripOffer
              (texts de OCR; rawData como respaldo) → EvaluateDetailedOfferUseCase
              → OverlayUiState.evaluation + recommendation
            · registra OfferTiming (evaluación/overlay) en OfferPerformanceTracker
            · persiste OfferEvaluationRecord en OfferEvaluationRepository (memoria,
              últimas 100) — historial "Última oferta" del panel de depuración
            · SPRINT 9: alimenta la sesión (CaptureSessionManager: start por
              snapshot, recordOffer/recordError) y persiste OfferHistoryEntry
              ampliado (tipo, confianza, resumen de reglas, motivos, tiempos)
              en Room
            · visible = status != DISABLED || evaluation != null
            · oculta el resultado tras OverlayConfig.ttlSeconds (MIN 10 s)
      ▼
OverlayService (Foreground Service, TYPE_APPLICATION_OVERLAY)
      └── ComposeView liviano: máximo 4 indicadores
          (recomendación, precio, ganancia, ganancia/hora, ganancia/km,
           costo estimado, resumen, motivo + % de confianza)
          · StatusLabel: Esperando / Capturando / Analizando / Error
          · arrastrable · ocultable · TTL configurable
```

Pipeline de análisis real — persiste historial y alimenta el overlay
(única fuente desde SPRINT 7/8; eliminado el flujo legacy
`SircAccessibilityService → OfferEventBus → OfferEvaluator` en RC1):

```
CaptureAccessibilityService (solo lectura) → DebounceCaptureScheduler (400 ms)
      → CapturePipeline → OfferSnapshot → PipelineOverlayDataSource
      → EvaluateDetailedOfferUseCase (ProfitEvaluationEngine + RecommendationEngine)
      → ConfidenceEngine → OverlayUiState → OverlayService
      → OfferEvaluationRepository (memoria) + OfferHistoryRepository (Room)
      → ValidationRecorder (modo validación RC1)
```

Captura (SPRINT 4, aditiva y sin interpretar):

```
CaptureAccessibilityService → AccessibilityWindowObserver (Flow)
      → OfferCaptureCoordinator → espejo de snapshots del Pipeline
        (no parsea ni guarda; mantiene la sesión y el CaptureState)
```

Pipeline de captura (SPRINT 5 + SPRINT 6, desacoplado de la UI):

```
CaptureAccessibilityService (solo lectura)
      → DebounceCaptureScheduler (400 ms) → CaptureRequest
      → CaptureInput (MediaProjectionCaptureInput: imagen si proyecta)
      → CapturePipeline
      → caché de frames → (OCR si hay imagen) → detección → parser
      → CaptureRepository
      → OverlayState · snapshots · lastMetrics
```

El `OverlayService` y el `OverlayManager` tienen arquitectura independiente del
pipeline: el servicio (FGS `specialUse`) dibuja el `ComposeView`, y el manager
controla su ciclo de vida vía `OverlayController` + `PermissionManager`. El
estado del pipeline (`OverlayState`) es consumido por `PipelineOverlayDataSource`
y observado por el panel de depuración.

Permisos y control:

```
PermissionManager (interfaz) ← AndroidPermissionManager
      overlay · accesibilidad · notificaciones · batería (detección + ajustes)
OverlayManager (interfaz) ← AndroidOverlayManager: fachada para ViewModels
      (isRunning, start/stop, permisos, projectionActive, createScreenCaptureIntent,
       startProjection, stopProjection) ──► OverlayController (arranca/para
      OverlayService) ──► OverlayViewModel (@HiltViewModel) ──► Compose Overlay
      └──► ScreenCaptureProvider (:core:capture:android): consentimiento de
           captura de pantalla y control de la proyección
```

Detalles de diseño del overlay:

- **Una sola vista persistente** (SPRINT 9): el `ComposeView` se agrega una vez;
  ocultar/mostrar usa `FLAG_NOT_TOUCHABLE` en lugar de re-agregar/retirar de
  `WindowManager`. `onConfigurationChanged` reclama tamaño/posición y
  `START_STICKY` ayuda al reinicio. `OverlayContent` anima visibilidad
  (`animateFloatAsState`) y hace crossfade estado↔evaluación
  (`AnimatedContent`).
- El servicio arranca/observa en `onStartCommand` (`dataSource.start()`) y
  detiene en `onDestroy` (`dataSource.stop()`).
- Traversal del árbol de accesibilidad con límites duros (400 nodos, 80 textos,
  textos ≤200 chars) para minimizar memoria y batería.
- Deduplicación por huella del frame y **caché de frames por hash de contenido**
  en el pipeline: no re-evalúa el mismo contenido.
- `OverlayConfig` define indicadores visibles, modo compacto, opacidad, TTL,
  posición y `historyLimit`; máxima velocidad de lectura.
- `PermissionManager` es la única fuente de verdad de permisos y ajustes; la
  consume `OverlayManager`, `HomeViewModel` y `DiagnosisViewModel`.
- `OverlayDataSource` es la única fuente de verdad del estado del overlay; la
  comparten `OverlayService` y `OverlayViewModel` (vista previa en pantallas).

### Feature:settings (`:feature:settings`)

- `SettingsViewModel` (`@HiltViewModel`) combina los Flows de `DriverCosts`,
  `DecisionThresholds` y `OverlayConfig`; `save()` persiste los tres.
- `SettingsScreen`: tarjetas "Costos del conductor", "Umbrales de decisión" y
  "Overlay" (indicadores, modo compacto, opacidad); campo editable
  **Límite de registros del historial** (`OverlayConfig.historyLimit`, SPRINT 9).

### Feature:history (`:feature:history`)

- `HistoryViewModel` (`@HiltViewModel`) expone el historial filtrado
  (`HistoryUiState` con `HistoryFilters`), `select`/`dismissDetail` y
  `clearHistory()`.
- `HistoryScreen`: lista `LazyColumn` con insignia de decisión, resumen,
  ganancia formateada y timestamp; barra de **búsqueda** y **filtros**
  (plataforma, decisión, presets de fecha Hoy/7/30 días); **detalle** en
  diálogo (precio, distancia, duración, ganancia, tipo de oferta, confianza,
  recomendación, reglas, motivos); estado vacío.
- **Dashboard (SPRINT 9)**: `StatsViewModel` + `StatsScreen` con gráficos
  `Canvas` (barras diarias + donut de decisiones) alimentados por
  `HistoryStatsCalculator` (`:domain`).

### Feature:onboarding (`:feature:onboarding`)

- `OnboardingViewModel` (`@HiltViewModel`): mantiene el borrador `DriverConfig`
  y un índice de paso; `save()` persiste el agregado completo.
- `OnboardingScreen`: 6 pasos (Perfil, Vehículo, Costos, Plataformas, Objetivos,
  Resumen) con validación por paso y barra de progreso. Los "otros costos" se
  editan como lista (arquitectura extensible).
- Solo es visible en la primera apertura (ver gating en `:app`).

### App (`:app`)

- `SircApplication` (`@HiltAndroidApp`, arranca `OfferCaptureCoordinator`),
  `MainActivity` (`@AndroidEntryPoint`), `SircRoot` (gating de onboarding) y
  navegación con `Scaffold` + `TopAppBar` + `NavigationBar` (6 destinos: Home,
  Historial, **Estadísticas**, Ajustes, Diagnóstico, Debug) y `NavHost`.
- `RootViewModel`: expone `observeIsConfigured()` como `StateFlow<Boolean?>`;
  `SircRoot` muestra spinner → `OnboardingScreen` → `SircApp`.
- `HomeViewModel` (`@HiltViewModel`) expone estado de permisos (overlay,
  accesibilidad, notificaciones, batería), ejecución del overlay y
  `projectionActive`; acciones para iniciar/detener el overlay, abrir ajustes,
  solicitar el consentimiento de captura de pantalla
  (`createScreenCaptureIntent()` → `startProjection(resultCode, data)`) y
  detener la proyección.
- `DiagnosisViewModel` + `DiagnosisScreen`: indicadores 🟢/🔴 de los 5
  requisitos del overlay y vista previa con datos simulados
  (`OverlayViewModel`).
- `DebugPanelViewModel` + `DebugPanelScreen` (desarrollo): estado de la
  infraestructura de captura, toggles de Feature Flags (el destino Debug se
  oculta si `DEBUG_PANEL` está desactivado), estado del pipeline
  (`OverlayState`), último snapshot, **métricas por etapa**
  (`pipeline.lastMetrics`: Captura/OCR/Parseo/Total), **"Última oferta"**
  (recomendación con confianza, plataforma, precio, distancia, duración,
  motivo, texto OCR truncado a 200 chars, parser y timestamp desde
  `OfferEvaluationRepository`), **"Rendimiento (promedio últimas 20 ofertas)"**
  (captura/OCR/parseo/evaluación/overlay/total desde
  `OfferPerformanceTracker.averages`), **"Sesión de captura"** (estado,
  duración, contadores y controles Iniciar/Pausar/Reanudar/Detener vía
  `CaptureSessionManager`), **Exportar diagnóstico** (share del informe con
  sesión, promedios, última oferta y flags), tiempo de procesamiento, memoria
  aproximada y eventos recientes.

## Decisiones técnicas registradas

| Decisión | Justificación |
|---|---|
| Overlay en `TYPE_APPLICATION_OVERLAY` | API estándar, compatible con Play con permiso explícito; overlay nunca intercepta toques (`FLAG_NOT_FOCUSABLE`). |
| FGS tipo `specialUse` | Android 14+ exige tipo para servicios de solapamiento; se declara subtipo descriptivo (`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`). |
| Parseo heurístico por keywords | No hay API pública de las apps de transporte; el reconocimiento es configurable y mejorable sin tocar el núcleo. |
| `domain` y `core:platform` puros Kotlin | Velocidad de pruebas, cero dependencias Android en la lógica crítica. |
| Historial en Room (no en memoria) | Sobrevive reinicios y es la base de futuros reportes. |
| Indicadores ≤ 4 | Restricción de producto: el conductor decide en <3 s. |
| `ValidationRecorder` en memoria (buffer 500) | Modo de validación RC1: registra `CaptureError`/`OcrFailed`/`ParseFailed`/`FrameDiscarded`/`RuleFailed`/`OfferRejected` y exporta un informe; efímero por diseño (sin desgaste de almacenamiento). |
| `OverlayDataSource` como única fuente del estado del overlay | `OverlayService` y pantallas comparten el mismo estado; FGS no depende de un ViewModel con ciclo de vida. |
| `DriverConfig` agregado en una fila | Perfil/vehículo/costos/plataformas/umbrales persisten atómicos; fila existente = configurado. |
| Listas codificadas como texto | Sin TypeConverters ni JSON; funciones puras probadas en `DriverConfigCodecTest`. |
| Migración Room con reconstrucción de tabla | Compatible con SQLite < 3.25 (minSdk 24). |
| Gating de onboarding en la raíz | Onboarding solo la primera vez; app principal cuando la fila existe. |
| `minProfitPerKm` como indicador del MVP | Ganancia mínima por km y por hora: los dos umbrales principales. |
| Design System y tokens en `:core:ui` | Única fuente de colores/espaciados/elevaciones/estados; componentes con `@Preview` y KDoc. |
| `OverlayCard`/`OverlayCardContent` presentacionales | Reutilizables en overlay y pantallas; no conocen de dominio (slots). |
| `ProfitState` como semántica de decisión | `Decision` → etiqueta/color única para overlay, historial y diagnóstico. |
| Overlay simulado con `ProfitEngine` real | Valida la UI completa (métricas y decisión verdaderas) sin conectar datos reales aún. |
| Permisos centralizados en `PermissionManager` | Home y Diagnóstico leen el mismo estado; se elimina duplicación en `OverlayController`. |
| `OverlayManager` fachada → `OverlayController` control del servicio | La UI depende de interfaces, nunca de Android/`Settings` directo. |
| `ksp.useKSP2=false` | Estabilidad del toolchain con la versión actual de KSP. |
| Plataforma de captura en `:core:capture` (puro) | Infraestructura de captura desacoplada de Android: el servicio solo alimenta un `WindowObserver`; parser/repositorio/coordinador se prueban con JUnit puro. |
| `OfferParser` `PlatformOfferParser` | Parser único real (SPRINT 8) que usa `OfferParserOrchestrator`; el `FakeParser` del flujo simulado se eliminó al llegar el parser real (WP-E1-01). |
| `CaptureRepository` en memoria | Guardado temporal de snapshots (buffer 50); la interfaz admite una implementación persistente futura. |
| Feature Flags configurables | `OVERLAY`, `CAPTURE`, `PARSER`, `OCR`, `DETAILED_LOGS`, `DEBUG_PANEL` con toggles en el panel; listos para desactivarse en producción. |
| Logging centralizado `SircLogger` | `AndroidSircLogger` solo emite en builds de desarrollo; cero logs en producción. |
| `AccessibilityWindowObserver` en `:feature:overlay` | La implementación Android del observer vive junto al servicio; el coordinador (puro) solo ve el Flow. |
| `CapturePipeline` en `:core:capture` (puro) | Orquesta caché → OCR → detección → Parser → Repository sin conocer Android; los falsos de prueba validan cada etapa con JUnit puro. |
| `OcrEngine` como abstracción de ML Kit | Facilita sustituciones y pruebas (falso OCR) sin tocar el pipeline; el motor real (`MlKitOcrEngine`) vive en `:feature:overlay`. |
| `CaptureInput` como contrato | Implementado por `MediaProjectionCaptureInput` (`:core:capture:android`); degrada a textos de accesibilidad si no hay proyección o frame. |
| `CaptureAccessibilityService` dedicado | Desacopla la captura de la UI: no publica en el overlay ni conoce estados de interfaz; reutiliza la config de accesibilidad (mismos paquetes, solo lectura, `canPerformGestures=false`). |
| `OverlayState` en el pipeline | Estados mínimos (Disabled/Waiting/Processing/Error) observables desde el panel de depuración. |
| Test images en recursos de prueba | `core/capture/src/test/resources/test-images/` alimentan el pipeline con una imagen real en pruebas unitarias. |
| Captura de pantalla en `:core:capture:android` | MediaProjection (Android) aislado de la UI y del pipeline puro: `ScreenCaptureProvider` posee el token/`VirtualDisplay`; la UI solo entrega el resultado del permiso. |
| FGS tipo `mediaProjection` | Android 14+ exige tipo `mediaProjection` (más `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`); el FGS mantiene viva la proyección. |
| `ScreenCaptureProvider` desacoplado de la UI | La UI no toca `MediaProjectionManager`/`VirtualDisplay`; el servicio completa la proyección al recibir el consentimiento. |
| Caché de frames por hash de contenido | `InMemoryCaptureFrameCache` (LRU 32) evita reprocesar capturas idénticas (mismo contenido → mismo OCR). |
| `DebounceCaptureScheduler` | Coalesce los `CaptureRequest` de accesibilidad (muy frecuentes) y emite solo el último tras 400 ms; el OCR no se ejecuta por cada evento. |
| Métricas por etapa en el pipeline | `ProcessingMetrics` (captura/OCR/parseo/total) expuestas para el panel de depuración; `CaptureMetrics` solo loguea en debug. |
| `PipelineOverlayDataSource` conecta el pipeline al overlay | El overlay muestra el estado real (WAITING/PROCESSING/ERROR) y el resultado evaluado con el motor real; sustituye a los datos simulados. |
| `ProfitEvaluationEngine` delega en `ProfitEngine` | Reutiliza el motor probado (decisión/margen/umbrales) y solo añade la derivación de costos desde `DriverConfig`; sin duplicar fórmulas. |
| Umbrales solo desde `DriverConfig.thresholds` | La decisión respeta siempre los objetivos del conductor; el motor no define constantes propias. |
| `ConfidenceEngine` con niveles | Combina completitud, coherencia de métricas, moneda y coherencia de reglas (`RuleEvaluation` opcional, sin fallos); `LOW` = "Información insuficiente" y nunca ACCEPT/REJECT. |
| Recomendación `ACCEPT`/`REJECT`/`WARNING` | El conductor decide en <3 s: el overlay muestra qué hacer, el motivo principal y el % de confianza derivado del margen. |
| Historial en memoria `OfferEvaluationRepository` | `InMemoryOfferEvaluationRepository` (100 ofertas) alimenta el overlay y el panel de depuración en tiempo real, sin I/O de Room en el camino crítico; el historial persistente de `:feature:history` se mantiene aparte. |
| `OfferPerformanceTracker` con promedio de 20 | Retiene las últimas 100 ofertas (memoria acotada) y expone promedios móviles por etapa para medir el rendimiento real de captura/OCR/parseo/evaluación/overlay. |
| `OfferSnapshot.texts` | El snapshot transporta los textos OCR además de la imagen cruda, para que el overlay evalúe el contenido visible real de la oferta. |
| `OfferDetectionEngine` clasifica la pantalla | Keywords ponderadas por pantalla → `ScreenType`; solo `REQUEST` produce oferta, evitando parsear Home/navegación/error. |
| Parser orquestado por descriptores | `OfferParserOrchestrator` es 100 % descriptor-driven: resuelve motor/parsers/extractor desde el `PlatformDescriptorRegistry`; los descriptores expresan variantes (Moto/XL/Radar/Reserva antes que Request) sin código por plataforma. |
| `PlatformDescriptor` + `PlatformDescriptorRegistry` | El conocimiento de plataforma (detección, extracción, tipos de oferta, moneda) vive en descriptores declarativos que se validan una sola vez en construcción; el orquestador y el pipeline no contienen ramas por plataforma. |
| `@JvmSuppressWildcards` en listas inyectadas | Kotlin declara `List<? extends T>`; Dagger lo trataría como multibinding; el provider usa `List<@JvmSuppressWildcards T>` explícito. |
| Constructores con default args sin `@Inject` | Un default arg genera un segundo constructor que Dagger rechaza ("may only contain one injected constructor"); los engines se proveen explícitamente. |
| Tiempos de detección y reglas por oferta | `detectionMillis` en `ProcessingMetrics`/`OfferTiming` y `rulesMillis` en `OfferTiming`; regex del parser precompiladas (sin recompilar por frame). |
| `CaptureSessionManager` como máquina de estados pura | La sesión (duración, ofertas, errores) se mide en `:domain` sin Android; el reloj inyectable hace deterministas las pruebas. |
| Historial persistente ampliado en Room v3 | `offer_history` conserva el análisis detallado (tipo, confianza, reglas, motivos, tiempos) y sobrevive reinicios; migración 1→3. |
| `OverlayConfig.historyLimit` + `trimToLimit` | El límite de registros es configurable desde Ajustes; el repositorio recorta los más antiguos al insertar. |
| `HistoryStatsCalculator` + gráficos Canvas | Dashboard sin librería de charting: barras diarias y donut dibujados con `Canvas` de Compose. |
| `SessionStats.clock` excluido de `equals` | `activeSeconds` en vivo con reloj inyectable sin romper la igualdad estructural del data class. |
| `OverlayService` de vista única persistente | Oculta con `FLAG_NOT_TOUCHABLE` en vez de re-agregar el `ComposeView` (menos parpadeo y consumo); `onConfigurationChanged` reclama tamaño/posición. |
| MediaProjection resiliente a configuración | `onDisplayConfigChanged()` recrea el `VirtualDisplay` en rotación/cambio de resolución; liberación idempotente. |
| Flags `DETAILED_LOGS`/`DEBUG_PANEL` | El Modo Beta controla logs detallados y visibilidad del panel de depuración en caliente; el diagnóstico exportable permite reportar a soporte sin leer logcat. |

## Cumplimiento

La política de Accessibility de Google Play y el detalle de permisos están en
`docs/GOOGLE_PLAY_COMPLIANCE.md`.

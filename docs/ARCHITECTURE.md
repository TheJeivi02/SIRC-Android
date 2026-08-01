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

## Estructura de módulos

| Módulo | Capa | Tipo | Responsabilidad |
|---|---|---|---|
| `:app` | Presentation (entrada) | `com.android.application` | `SircApplication`, `MainActivity`, navegación (`SircApp`), Home. |
| `:domain` | Domain | `kotlin("jvm")` | Modelos, `ProfitEngine`, motores de evaluación y recomendación, use cases, contratos de repositorio. **Sin Android.** |
| `:data` | Data | `com.android.library` | Room (`SircDatabase`, entidades, DAOs), repositorios concretos, DI de datos. |
| `:core:platform` | Core | `kotlin("jvm")` | Parser de texto y extractores por plataforma. **Sin Android.** |
| `:core:capture` | Core | `kotlin("jvm")` | Plataforma de captura: pipeline de extremo a extremo (screen capture, OCR, parser, repositorio), observador de ventanas, sesión/snapshot, estados del overlay, coordinador, caché de frames, debounce, métricas, feature flags y logging. **Sin Android.** |
| `:core:capture:android` | Core | `com.android.library` | Implementación Android de la captura de pantalla: MediaProjection (provider + FGS `mediaProjection`), `ScreenCapture` real, métricas de depuración. |
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
- `usecase/` — orquestación fina: `EvaluateOfferUseCase`,
  `EvaluateDetailedOfferUseCase` (evalúa una oferta completa con motores + costos
  derivados y devuelve `OfferEvaluationResult`),
  `Get/SaveOverlayConfigUseCase`, `Get/SaveDriverConfigUseCase`,
  `Observe/Clear/AddOfferHistoryUseCase`.

### Data (`:data`)

- Room: `SircDatabase` (tablas `driver_config`, `overlay_config`,
  `offer_history`), entidades, DAOs y mappers (`Mappers.kt`). Versión 2 con
  migración 1→2 (reconstrucción de tabla, compatible con SQLite antiguo).
- `driver_config` guarda el agregado `DriverConfig` en una fila: perfil,
  vehículo, costos (combustible, mantenimiento, adicionales codificados),
  plataformas (codificadas) y umbrales. La existencia de la fila = conductor
  configurado.
- Repositorios concretos `Default*Repository` implementan los contratos de
  dominio y aplican valores por defecto si no existe fila.
- DI: `DatabaseModule` (DB + DAOs + migraciones) y `RepositoryModule` (`@Binds`).
- Esquema Room versionado en `data/schemas/` (`exportSchema = true`).

### Core:platform (`:core:platform`) — Kotlin puro

- `OfferTextParser` — heurística pura: extrae candidatos de monto, distancias y
  duraciones con regex y límites de rango.
- `PlatformExtractor` (interfaz) + `GenericPlatformExtractor` — estrategia por
  plataforma basada en palabras clave (`PlatformKeywords`, `PlatformDescriptors`).
- `ExtractorRegistry` — resuelve el extractor por `RidePlatform`.
- **Agregar una plataforma** = agregar `RidePlatform` + descriptor de palabras
  clave; no requiere tocar el núcleo.

### Core:capture (`:core:capture`) — Kotlin puro

Plataforma de captura (SPRINT 4 infraestructura + SPRINT 5 pipeline): observa los
cambios de ventana de las plataformas, captura contenido, aplica OCR si hay
imagen y produce snapshots; desacoplado por completo de Android.

Flujo de captura (coordinador, SPRINT 4):

```
SircAccessibilityService (solo lectura, no interpreta)
      ▼ emite cambios de ventana
AccessibilityWindowObserver (:feature:overlay, Flow)
      ▼
OfferCaptureCoordinator ──► WindowObserver.windowEvents (Flow)
      · mantiene la OfferCaptureSession activa
      · OfferParser (FakeParser hoy) → OfferSnapshot (FAKE)
      · CaptureRepository (InMemoryCaptureRepository)
      · CaptureState: sesión, último snapshot, tiempo de procesamiento,
        eventos recientes
```

Pipeline de captura (SPRINT 5 + SPRINT 6, captura de pantalla real):

```
CaptureAccessibilityService (solo lectura, desacoplado de la UI)
      ▼ CaptureRequest (textos + imagen)
DebounceCaptureScheduler (400 ms: coalesce los eventos de accesibilidad)
      ▼
CapturePipeline (DefaultCapturePipeline)
      1. caché de frames (InMemoryCaptureFrameCache, LRU 32) → si es repetido, se omite
      2. ScreenCapture → ScreenFrame            (MediaProjection: imagen real;
                                                 degrada a texto si no hay frame)
      3. si hay imagen → OcrEngine → textos     (ML Kit, abstraído)
      4. OfferParser → OfferSnapshot
      5. CaptureRepository
      ▼
OverlayState (StateFlow): DISABLED → WAITING → CAPTURING → PROCESSING → ERROR
snapshots (SharedFlow) · lastMetrics (StateFlow: captura/OCR/parseo/total)
```

- `model/` — `CaptureWindowEvent`, `WindowEventType`, `OfferCaptureSession`
  (ACTIVE/CLOSED), `OfferSnapshot` (inmutable, `SnapshotSource.FAKE`/`REAL`),
  `CaptureState`, `CaptureRequest`, `ScreenFrame`, `OverlayState`
  (DISABLED/WAITING/CAPTURING/PROCESSING/ERROR).
- `observer/WindowObserver` — contrato: expone `windowEvents: Flow`; la
  implementación Android (`AccessibilityWindowObserver`) vive en
  `:feature:overlay`.
- `screen/ScreenCapture` — contrato de captura del frame; la implementación real
  (`MediaProjectionScreenCapture`) vive en `:core:capture:android`; degrada a
  texto de accesibilidad si no hay proyección.
- `cache/` — `CaptureFrameCache` + `InMemoryCaptureFrameCache` (LRU de 32
  entradas, clave por hash de contenido de la imagen): evita reprocesar
  capturas idénticas.
- `scheduler/DebounceCaptureScheduler` — coalesce los `CaptureRequest` y emite
  solo el último tras el debounce (400 ms por defecto).
- `metrics/` — `CaptureMetrics` (interfaz, `NoOpCaptureMetrics`) y
  `ProcessingMetrics` (tiempos por etapa) expuestos por el pipeline;
  `OfferTiming` (captura/OCR/parseo/evaluación/overlay/total) y
  `OfferPerformanceTracker` + `InMemoryOfferPerformanceTracker` (SPRINT 7):
  retiene las últimas 100 ofertas y expone promedios de las últimas 20.
- `ocr/OcrEngine` — reconoce texto en una imagen; `MlKitOcrEngine` (Android)
  implementa ML Kit; el pipeline solo lo invoca si la solicitud lleva imagen.
- `parser/OfferParser` + `FakeParser` — el fake genera snapshots simulados para
  validar el flujo completo; la interfaz está lista para un parser real.
- `repository/CaptureRepository` + `InMemoryCaptureRepository` — guardado
  temporal (buffer 50); la interfaz admite una implementación persistente.
- `coordinator/OfferCaptureCoordinator` — orquesta la captura vía observer;
  sin dependencias de Android (solo interfaces + modelos).
- `pipeline/CapturePipeline` + `DefaultCapturePipeline` — orquesta el flujo
  ScreenCapture → OCR → OfferParser → CaptureRepository, con caché y métricas;
  expone `OverlayState`, `snapshots` y `lastMetrics`.
- `flag/` — `FeatureFlag` (ACCESSIBILITY, OVERLAY, CAPTURE, PARSER, OCR,
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
- `MediaProjectionScreenCapture` — implementa `ScreenCapture`: captura el frame
  y lo comprime a PNG para el OCR; degrada a `request.texts` si no proyecta.
- `metrics/DebugCaptureMetrics` — `CaptureMetrics` que solo loguea en builds
  `FLAG_DEBUGGABLE`.
- `CaptureAndroidModule` — `@Binds`: `ScreenCaptureProvider`,
  `ScreenCapture` y `CaptureMetrics`.
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

Pipeline real (Accessibility) — persiste historial:

```
SircAccessibilityService (solo lectura) → OfferEventBus → OfferEvaluator
      → EvaluateOfferUseCase → ProfitEngine
      → AddOfferHistoryUseCase → Room offer_history
```

Captura (SPRINT 4, aditiva y sin interpretar):

```
SircAccessibilityService → AccessibilityWindowObserver (Flow)
      → OfferCaptureCoordinator → OfferSnapshot (FakeParser) → CaptureRepository
```

Pipeline de captura (SPRINT 5 + SPRINT 6, desacoplado de la UI):

```
CaptureAccessibilityService (solo lectura)
      → DebounceCaptureScheduler (400 ms) → CaptureRequest
      → CapturePipeline
      → caché de frames → ScreenCapture (MediaProjection) → (OCR si hay imagen)
      → OfferParser → CaptureRepository
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

- **Un solo `ComposeView`** agregado/retirado de `WindowManager` según estado.
- El servicio arranca/observa en `onStartCommand` (`dataSource.start()`) y
  detiene en `onDestroy` (`dataSource.stop()`).
- Traversal del árbol de accesibilidad con límites duros (400 nodos, 80 textos,
  textos ≤200 chars) para minimizar memoria y batería.
- Deduplicación por huella del frame y **caché de frames por hash de contenido**
  en el pipeline: no re-evalúa el mismo contenido.
- `OverlayConfig` define indicadores visibles, modo compacto, opacidad, TTL y
  posición; máxima velocidad de lectura.
- `PermissionManager` es la única fuente de verdad de permisos y ajustes; la
  consume `OverlayManager`, `HomeViewModel` y `DiagnosisViewModel`.
- `OverlayDataSource` es la única fuente de verdad del estado del overlay; la
  comparten `OverlayService` y `OverlayViewModel` (vista previa en pantallas).

### Feature:settings (`:feature:settings`)

- `SettingsViewModel` (`@HiltViewModel`) combina los Flows de `DriverCosts`,
  `DecisionThresholds` y `OverlayConfig`; `save()` persiste los tres.
- `SettingsScreen`: tarjetas "Costos del conductor", "Umbrales de decisión" y
  "Overlay" (indicadores, modo compacto, opacidad).

### Feature:history (`:feature:history`)

- `HistoryViewModel` (`@HiltViewModel`) expone `Flow<List<OfferHistoryEntry>>`
  (límite 100) y `clearHistory()`.
- `HistoryScreen`: lista `LazyColumn` con insignia de decisión, resumen,
  ganancia formateada y timestamp; estado vacío.

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
  navegación con `Scaffold` + `TopAppBar` + `NavigationBar` (5 destinos: Home,
  Historial, Ajustes, Diagnóstico, Debug) y `NavHost`.
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
  `OfferPerformanceTracker.averages`), tiempo de procesamiento, memoria
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
| `OfferEventBus` en memoria | Puente simple entre servicios sin persistencia ni I/O innecesaria. |
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
| `OfferParser` + `FakeParser` | El fake valida el flujo completo sin interpretar pantallas reales; la interfaz queda lista para parser/OCR real. |
| `CaptureRepository` en memoria | Guardado temporal de snapshots (buffer 50); la interfaz admite una implementación persistente futura. |
| Feature Flags configurables | `ACCESSIBILITY`, `OVERLAY`, `CAPTURE`, `PARSER`, `OCR`, `DEBUG_PANEL` con toggles en el panel; listos para desactivarse en producción. |
| Logging centralizado `SircLogger` | `AndroidSircLogger` solo emite en builds de desarrollo; cero logs en producción. |
| `AccessibilityWindowObserver` en `:feature:overlay` | La implementación Android del observer vive junto al servicio; el coordinador (puro) solo ve el Flow. |
| `CapturePipeline` en `:core:capture` (puro) | Orquesta ScreenCapture → OCR → Parser → Repository sin conocer Android; los falsos de prueba validan cada etapa con JUnit puro. |
| `OcrEngine` como abstracción de ML Kit | Facilita sustituciones y pruebas (falso OCR) sin tocar el pipeline; el motor real vive en `:feature:overlay`. |
| `ScreenCapture` como contrato | Implementado por `MediaProjectionScreenCapture` (`:core:capture:android`); degrada a texto de accesibilidad si no hay proyección o frame. |
| `CaptureAccessibilityService` dedicado | Desacopla la captura de la UI: no publica en el overlay ni conoce estados de interfaz; reutiliza la config de accesibilidad (mismos paquetes, solo lectura, `canPerformGestures=false`). |
| `OverlayState` en el pipeline | Estados mínimos (Disabled/Waiting/Capturing/Processing/Error) observables desde el panel de depuración. |
| Test images en recursos de prueba | `core/capture/src/test/resources/test-images/` alimentan el pipeline con una imagen real en pruebas unitarias. |
| Captura de pantalla en `:core:capture:android` | MediaProjection (Android) aislado de la UI y del pipeline puro: `ScreenCaptureProvider` posee el token/`VirtualDisplay`; la UI solo entrega el resultado del permiso. |
| FGS tipo `mediaProjection` | Android 14+ exige tipo `mediaProjection` (más `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`); el FGS mantiene viva la proyección. |
| `ScreenCaptureProvider` desacoplado de la UI | La UI no toca `MediaProjectionManager`/`VirtualDisplay`; el servicio completa la proyección al recibir el consentimiento. |
| Caché de frames por hash de contenido | `InMemoryCaptureFrameCache` (LRU 32) evita reprocesar capturas idénticas (mismo contenido → mismo OCR). |
| `DebounceCaptureScheduler` | Coalesce los `CaptureRequest` de accesibilidad (muy frecuentes) y emite solo el último tras 400 ms; el OCR no se ejecuta por cada evento. |
| Métricas por etapa en el pipeline | `ProcessingMetrics` (captura/OCR/parseo/total) expuestas para el panel de depuración; `CaptureMetrics` solo loguea en debug. |
| `PipelineOverlayDataSource` conecta el pipeline al overlay | El overlay muestra el estado real (WAITING/CAPTURING/PROCESSING/ERROR) y el resultado evaluado con el motor real; sustituye a los datos simulados. |
| `ProfitEvaluationEngine` delega en `ProfitEngine` | Reutiliza el motor probado (decisión/margen/umbrales) y solo añade la derivación de costos desde `DriverConfig`; sin duplicar fórmulas. |
| Umbrales solo desde `DriverConfig.thresholds` | La decisión respeta siempre los objetivos del conductor; el motor no define constantes propias. |
| Recomendación `ACCEPT`/`REJECT`/`WARNING` | El conductor decide en <3 s: el overlay muestra qué hacer, el motivo principal y el % de confianza derivado del margen. |
| Historial en memoria `OfferEvaluationRepository` | `InMemoryOfferEvaluationRepository` (100 ofertas) alimenta el overlay y el panel de depuración en tiempo real, sin I/O de Room en el camino crítico; el historial persistente de `:feature:history` se mantiene aparte. |
| `OfferPerformanceTracker` con promedio de 20 | Retiene las últimas 100 ofertas (memoria acotada) y expone promedios móviles por etapa para medir el rendimiento real de captura/OCR/parseo/evaluación/overlay. |
| `OfferSnapshot.texts` | El snapshot transporta los textos OCR además de la imagen cruda, para que el overlay evalúe el contenido visible real de la oferta. |

## Cumplimiento

La política de Accessibility de Google Play y el detalle de permisos están en
`docs/GOOGLE_PLAY_COMPLIANCE.md`.

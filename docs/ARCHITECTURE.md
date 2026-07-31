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
| `:domain` | Domain | `kotlin("jvm")` | Modelos, `ProfitEngine`, use cases, contratos de repositorio. **Sin Android.** |
| `:data` | Data | `com.android.library` | Room (`SircDatabase`, entidades, DAOs), repositorios concretos, DI de datos. |
| `:core:platform` | Core | `kotlin("jvm")` | Parser de texto y extractores por plataforma. **Sin Android.** |
| `:core:capture` | Core | `kotlin("jvm")` | Plataforma de captura: pipeline de extremo a extremo (screen capture, OCR, parser, repositorio), observador de ventanas, sesión/snapshot, estados del overlay, coordinador, feature flags y logging. **Sin Android.** |
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
  `FuelType`, `AdditionalCost`.
- `engine/ProfitEngine` — función pura: oferta + costos + umbrales → evaluación.
  Sin estado ni I/O; 100 % testeable. Decide con ganancia/km y ganancia/hora
  (`DecisionThresholds`).
- `repository/` — contratos: `DriverConfigRepository`, `OverlayConfigRepository`,
  `OfferHistoryRepository`.
- `usecase/` — orquestación fina: `EvaluateOfferUseCase`,
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

Pipeline de captura (SPRINT 5, preparado para OCR):

```
CaptureAccessibilityService (solo lectura, desacoplado de la UI)
      ▼ CaptureRequest (textos y, en el futuro, imagen)
CapturePipeline (DefaultCapturePipeline)
      1. ScreenCapture → ScreenFrame            (hoy texto de accesibilidad;
                                                 futuro MediaProjection + imagen)
      2. si hay imagen → OcrEngine → textos     (ML Kit, abstraído)
      3. OfferParser → OfferSnapshot
      4. CaptureRepository
      ▼
OverlayState (StateFlow): DISABLED → WAITING → CAPTURING → PROCESSING → ERROR
```

- `model/` — `CaptureWindowEvent`, `WindowEventType`, `OfferCaptureSession`
  (ACTIVE/CLOSED), `OfferSnapshot` (inmutable, `SnapshotSource.FAKE`/`REAL`),
  `CaptureState`, `CaptureRequest`, `ScreenFrame`, `OverlayState`
  (DISABLED/WAITING/CAPTURING/PROCESSING/ERROR).
- `observer/WindowObserver` — contrato: expone `windowEvents: Flow`; la
  implementación Android (`AccessibilityWindowObserver`) vive en
  `:feature:overlay`.
- `screen/ScreenCapture` — captura el frame; `AccessibilityScreenCapture`
  (Android) usa el texto observado; el contrato admite una futura captura de
  imagen (MediaProjection).
- `ocr/OcrEngine` — reconoce texto en una imagen; `MlKitOcrEngine` (Android)
  implementa ML Kit; el pipeline solo lo invoca si la solicitud lleva imagen.
- `parser/OfferParser` + `FakeParser` — el fake genera snapshots simulados para
  validar el flujo completo; la interfaz está lista para un parser real.
- `repository/CaptureRepository` + `InMemoryCaptureRepository` — guardado
  temporal (buffer 50); la interfaz admite una implementación persistente.
- `coordinator/OfferCaptureCoordinator` — orquesta la captura vía observer;
  sin dependencias de Android (solo interfaces + modelos).
- `pipeline/CapturePipeline` + `DefaultCapturePipeline` — orquesta el flujo
  ScreenCapture → OCR → OfferParser → CaptureRepository y expone `OverlayState`.
- `flag/` — `FeatureFlag` (ACCESSIBILITY, OVERLAY, CAPTURE, PARSER, OCR,
  DEBUG_PANEL) y `FeatureFlags`/`InMemoryFeatureFlags` (configurables).
- `log/SircLogger` — logging centralizado; `AndroidSircLogger` solo emite en
  builds de desarrollo.

### Core:ui (`:core:ui`)

- Tema SIRC: `SircTheme`, paleta `SircColors` (semáforo), `SircTypography`,
  `SircSpacing` (escala 4dp), `SircElevations`.
- Estados: `ProfitState` (decisión → etiqueta/color) en `theme/ProfitState.kt`.
- Componentes: `ProfitIndicator` (píldora semáforo, fuente única de estilo),
  `DecisionBadge` (delega en `ProfitIndicator`), `StatusDot`, `SectionCard`,
  `LabeledValue`, `MetricCell`/`MetricValue`, `OverlayCard`/`OverlayCardContent`
  (presentacionales, slots de contenido). Todos con `@Preview` y KDoc.

### Feature:overlay (`:feature:overlay`) — el corazón del MVP

Flujo de datos del overlay (SPRINT 2 — datos simulados):

```
OverlayDataSource (interfaz: StateFlow<OverlayUiState>, start/stop)
      │
      └── SimulatedOverlayDataSource (Singleton)
            · emite una oferta simulada por plataforma cada 20 s
            · evalúa con EvaluateOfferUseCase (ProfitEngine real, datos simulados)
            · oculta tras OverlayConfig.ttlSeconds; NO persiste historial
      ▼
OverlayService (Foreground Service, TYPE_APPLICATION_OVERLAY)
      └── ComposeView liviano: máximo 4 indicadores
          (decisión, ganancia, ganancia/hora, ganancia/km, resumen)
          · arrastrable · ocultable · TTL configurable
```

Pipeline real (Accessibility) — persiste historial; aún sin UI conectada:

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

Pipeline de captura (SPRINT 5, desacoplado de la UI):

```
CaptureAccessibilityService (solo lectura)
      → CaptureRequest → CapturePipeline
      → ScreenCapture → (OCR si hay imagen) → OfferParser → CaptureRepository
      → OverlayState
```

El `OverlayService` y el `OverlayManager` tienen arquitectura independiente del
pipeline: el servicio (FGS `specialUse`) dibuja el `ComposeView`, y el manager
controla su ciclo de vida vía `OverlayController` + `PermissionManager`. El
estado del pipeline (`OverlayState`) es observado por el panel de depuración.

Permisos y control:

```
PermissionManager (interfaz) ← AndroidPermissionManager
      overlay · accesibilidad · notificaciones · batería (detección + ajustes)
OverlayManager (interfaz) ← AndroidOverlayManager: fachada para ViewModels
      (isRunning, start/stop, permisos) ──► OverlayController (arranca/para
      OverlayService) ──► OverlayViewModel (@HiltViewModel) ──► Compose Overlay
```

Detalles de diseño del overlay:

- **Un solo `ComposeView`** agregado/retirado de `WindowManager` según estado.
- El servicio arranca la simulación en `onStartCommand` (`dataSource.start()`) y
  la detiene en `onDestroy` (`dataSource.stop()`).
- Traversal del árbol de accesibilidad con límites duros (400 nodos, 80 textos,
  textos ≤200 chars) para minimizar memoria y batería.
- Deduplicación por huella del frame: no re-evalúa el mismo texto.
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
  accesibilidad, notificaciones, batería) y ejecución del overlay; acciones para
  iniciar/detener el overlay y abrir ajustes.
- `DiagnosisViewModel` + `DiagnosisScreen`: indicadores 🟢/🔴 de los 5
  requisitos del overlay y vista previa con datos simulados
  (`OverlayViewModel`).
- `DebugPanelViewModel` + `DebugPanelScreen` (desarrollo): estado de la
  infraestructura de captura, toggles de Feature Flags (el destino Debug se
  oculta si `DEBUG_PANEL` está desactivado), estado del pipeline
  (`OverlayState`), último snapshot, tiempo de procesamiento, memoria
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
| `ScreenCapture` como contrato | Hoy usa el texto de accesibilidad; en el futuro MediaProjection solo añadirá la imagen (el pipeline ya la procesa con OCR). |
| `CaptureAccessibilityService` dedicado | Desacopla la captura de la UI: no publica en el overlay ni conoce estados de interfaz; reutiliza la config de accesibilidad (mismos paquetes, solo lectura, `canPerformGestures=false`). |
| `OverlayState` en el pipeline | Estados mínimos (Disabled/Waiting/Capturing/Processing/Error) observables desde el panel de depuración. |
| Test images en recursos de prueba | `core/capture/src/test/resources/test-images/` alimentan el pipeline con una imagen real en pruebas unitarias. |

## Cumplimiento

La política de Accessibility de Google Play y el detalle de permisos están en
`docs/GOOGLE_PLAY_COMPLIANCE.md`.

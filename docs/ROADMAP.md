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

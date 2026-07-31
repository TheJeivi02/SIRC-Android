# Changelog

Formato basado en [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/).
Las versiones siguen [SemVer](https://semver.org/lang/es/).

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

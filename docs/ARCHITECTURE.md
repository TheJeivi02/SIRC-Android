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
| `:core:ui` | Core | `com.android.library` | Design system Compose: tema y componentes. |
| `:feature:overlay` | Feature | `com.android.library` | Accessibility Service, Overlay Service, pipeline de evaluación y UI del overlay. |
| `:feature:settings` | Feature | `com.android.library` | Configuración de costos, umbrales e indicadores. |
| `:feature:history` | Feature | `com.android.library` | Historial de ofertas evaluadas. |

## Grafo de dependencias

```
app ──► feature:overlay ──► core:platform ─► domain
  │          │                 │
  │          └──────► data ────┘  (data implementa contratos de domain)
  ├──► feature:settings ─► data
  ├──► feature:history  ─► data
  └──► core:ui ──────────► domain (solo tipos)
```

Reglas derivadas del grafo real:

- `domain` es dependencia común de todos los módulos.
- `data` implementa los contratos de `domain` con Room; ningún módulo depende de
  `data` por encima de su capa (los features sí usan `data` directamente).
- `core:platform` (puro Kotlin) desacopla las plataformas del producto.
- `core:ui` provee el design system Compose y depende de `domain` solo para
  tipos (`Decision`).
- La UI de cada feature depende de su ViewModel; los ViewModels dependen de
  `domain` (use cases) — nunca de `data` directamente.

## Capas y responsabilidades

### Domain (`:domain`) — Kotlin puro

- `model/` — `TripOffer`, `RidePlatform`, `ProfitMetrics`, `ProfitEvaluation`,
  `Decision`, `DriverCosts`, `DecisionThresholds`, `OverlayConfig`,
  `OfferHistoryEntry`.
- `engine/ProfitEngine` — función pura: oferta + costos + umbrales → evaluación.
  Sin estado ni I/O; 100 % testeable.
- `repository/` — contratos: `DriverConfigRepository`, `OverlayConfigRepository`,
  `OfferHistoryRepository`.
- `usecase/` — orquestación fina: `EvaluateOfferUseCase`,
  `Get/SaveOverlayConfigUseCase`, `Get/SaveDriverConfigUseCase`,
  `Observe/Clear/AddOfferHistoryUseCase`.

### Data (`:data`)

- Room: `SircDatabase` (tablas `driver_config`, `overlay_config`,
  `offer_history`), entidades, DAOs y mappers (`Mappers.kt`).
- Repositorios concretos `Default*Repository` implementan los contratos de
  dominio y aplican valores por defecto si no existe fila.
- DI: `DatabaseModule` (DB + DAOs) y `RepositoryModule` (`@Binds`).
- Esquema Room versionado en `data/schemas/` (`exportSchema = true`).

### Core:platform (`:core:platform`) — Kotlin puro

- `OfferTextParser` — heurística pura: extrae candidatos de monto, distancias y
  duraciones con regex y límites de rango.
- `PlatformExtractor` (interfaz) + `GenericPlatformExtractor` — estrategia por
  plataforma basada en palabras clave (`PlatformKeywords`, `PlatformDescriptors`).
- `ExtractorRegistry` — resuelve el extractor por `RidePlatform`.
- **Agregar una plataforma** = agregar `RidePlatform` + descriptor de palabras
  clave; no requiere tocar el núcleo.

### Core:ui (`:core:ui`)

- Tema SIRC: `SircTheme`, paleta `SircColors` (semáforo), `SircTypography`.
- Componentes: `DecisionBadge`, `StatusDot`, `SectionCard`, `LabeledValue`.

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

### App (`:app`)

- `SircApplication` (`@HiltAndroidApp`), `MainActivity` (`@AndroidEntryPoint`),
  navegación con `Scaffold` + `TopAppBar` + `NavigationBar` (4 destinos: Home,
  Historial, Ajustes, Diagnóstico) y `NavHost`.
- `HomeViewModel` (`@HiltViewModel`) expone estado de permisos (overlay,
  accesibilidad, notificaciones, batería) y ejecución del overlay; acciones para
  iniciar/detener el overlay y abrir ajustes.
- `DiagnosisViewModel` + `DiagnosisScreen`: indicadores 🟢/🔴 de los 5
  requisitos del overlay y vista previa con datos simulados
  (`OverlayViewModel`).

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
| Overlay simulado con `ProfitEngine` real | Valida la UI completa (métricas y decisión verdaderas) sin conectar datos reales aún. |
| Permisos centralizados en `PermissionManager` | Home y Diagnóstico leen el mismo estado; se elimina duplicación en `OverlayController`. |
| `OverlayManager` fachada → `OverlayController` control del servicio | La UI depende de interfaces, nunca de Android/`Settings` directo. |
| `ksp.useKSP2=false` | Estabilidad del toolchain con la versión actual de KSP. |

## Cumplimiento

La política de Accessibility de Google Play y el detalle de permisos están en
`docs/GOOGLE_PLAY_COMPLIANCE.md`.

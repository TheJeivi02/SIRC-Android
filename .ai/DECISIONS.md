# Decisiones técnicas

> Registro de decisiones de diseño relevantes. Cada entrada indica contexto,
> opción elegida, alternativas descartadas y consecuencias. Se actualiza en
> cada sprint.

## SPRINT 3 — Configuración Inicial del Conductor

### D4.1 — `DriverConfig` es un agregado único persistido en una fila

**Contexto:** el sprint captura perfil, vehículo, costos, plataformas y
objetivos. Antes existían `DriverCosts` y `DecisionThresholds` en la misma fila
`driver_config`, pero sin perfil/vehículo ni "configurado" (una fila existía con
valores por defecto siempre).

**Decisión:** se crea `DriverConfig` (agregado con `DriverProfile`,
`DriverVehicle`, `DriverCosts`, `AdditionalCost`, `Set<RidePlatform>` y
`DecisionThresholds`) persistido en una única fila de `driver_config`. "Estar
configurado" = existe la fila (`observeIsConfigured()`). `DriverConfig.blank()`
inicia el onboarding y `DriverConfig.default()` es el respaldo legado.

**Alternativas descartadas:** una tabla por concepto (sobredimensiona el
problema y complica la transacción del onboarding); mantener costos/umbrales
fuera del agregado (dificulta guardar todo en un solo `save`).

### D4.2 — Persistencia codificada de listas sin TypeConverters

**Contexto:** `platforms` (Set) y `additionalCosts` (List) no son tipos nativos
de Room.

**Decisión:** se persisten como texto: plataformas separadas por coma
(nombres estables del enum) y costos adicionales con separadores de control
`\u001F`/`\u001E` (seguros frente a texto libre). La codificación vive en
`Mappers.kt` como funciones puras, cubiertas por `DriverConfigCodecTest`, sin
dependencias nuevas ni JSON.

**Consecuencia:** ampliar costos configurables es solo agregar más filas a la
lista; el motor futuro sumará `fuelPrice`, `maintenanceCostPerKm` y cada
`AdditionalCost`.

### D4.3 — Migración Room 1→2 con reconstrucción de tabla

**Contexto:** `driver_config` cambia de esquema (nuevas columnas y
`minProfit` → `minProfitPerKm`). SQLite < 3.25 (API < 30) no soporta
`RENAME COLUMN`.

**Decisión:** migración manual con tabla nueva + `INSERT ... SELECT` + `DROP` +
`RENAME TO`, compatible con minSdk 24. `minProfit` se repurposa a
`minProfitPerKm` conservando el valor anterior.

### D4.4 — Gating de onboarding en la raíz de la app

**Contexto:** el criterio de aceptación exige mostrar el onboarding solo la
primera vez.

**Decisión:** `RootViewModel` (`:app`) expone `observeIsConfigured()` como
`StateFlow<Boolean?>` (null = cargando). `SircRoot` muestra spinner,
`OnboardingScreen` o `SircApp`. Al guardar, la fila existe y el flujo cambia a la
app principal sin navegación manual.

### D4.5 — `DriverCosts` deja de guardar moneda

**Contexto:** la moneda se captura en el perfil; `DriverCosts.currency` duplicaba
ese dato con riesgo de desincronización.

**Decisión:** la única fuente de moneda es `DriverProfile.currency`. Ajustes la
edita desde el perfil y el onboarding la captura al inicio.

## SPRINT 2 — Design System + Overlay Foundation

### D3.1 — Design System en `:core:ui` con tokens propios

**Contexto:** cada pantalla usaba valores sueltos (dp, sp, colores). El overlay
replicaba tarjetas y métricas que luego se necesitaron en pantallas.

**Decisión:** `:core:ui` es la única fuente del design system: `SircColors`,
`SircTypography`, `SircSpacing` (escala 4dp) y `SircElevations`. Todo componente
público lleva KDoc y `@Preview`. `OverlayCard`/`OverlayCardContent` son
presentacionales (slots de contenido) y no conocen de dominio; `ProfitIndicator`
es la fuente única del estilo semáforo y `DecisionBadge` delega en él.

**Alternativas descartadas:** crear componentes en cada feature (duplica
estilos); atar `OverlayCard` a modelos de dominio (rompe la reutilización en
vista previa y pantallas).

### D3.2 — Estado semántico `ProfitState`

**Contexto:** la decisión del motor es un enum de dominio (`Decision`); la UI
necesita etiqueta + color consistentes en overlay, historial y diagnóstico.

**Decisión:** `ProfitState` (`theme/ProfitState.kt`) mapea `Decision` →
etiqueta/color de semáforo. Única fuente de la semántica visual; los
componentes lo consumen y las pruebas cubren el mapeo.

### D3.3 — El icono de cierre sin dependencia extra

**Contexto:** `:core:ui` no quería arrastrar `material-icons-extended` solo por
un `Close`.

**Decisión:** se usa `Icons.Filled.Close`, incluido en el conjunto core de
Material Icons que Material 3 ya aporta transitivamente.

## SPRINT 2 — Overlay desacoplado con datos simulados

### D2.1 — `PermissionManager` centraliza permisos y ajustes

**Contexto:** `OverlayController` duplicaba la lógica de permisos
(overlay/accesibilidad). Diagnóstico y Home necesitan los mismos chequeos
(+notificaciones y batería).

**Decisión:** Se crea `PermissionManager` (interfaz) + `AndroidPermissionManager`
(singleton Hilt) como única fuente de verdad de permisos y apertura de ajustes.
`OverlayController`, `OverlayManager`, `HomeViewModel` y `DiagnosisViewModel` lo
consumen. Se elimina la duplicación en `OverlayController`.

**Alternativas descartadas:** mantener la lógica en cada ViewModel (duplica
código y estado de permisos en dos pantallas).

### D2.2 — `OverlayManager` es la fachada; `OverlayController` pasa a controlar el servicio

**Contexto:** la cadena objetivo es
`OverlayManager → OverlayController → OverlayViewModel → Compose Overlay`.

**Decisión:** `OverlayManager` (interfaz + `AndroidOverlayManager`) es la fachada
que consumen las ViewModels: estado de ejecución (`isRunning`), `start()`,
`stop()` y accesos de permisos (delegados a `PermissionManager`).
`OverlayController` se refactoriza a un controlador de bajo nivel: inicia/para
`OverlayService` y expone `isRunning`; ya no contiene lógica de permisos
(extraída a `PermissionManager`).

**Consecuencia:** `HomeViewModel` y `DiagnosisViewModel` dependen de
`PermissionManager` + `OverlayManager`, nunca de `data` ni de Android directamente.

### D2.3 — Fuente de datos simulada con la misma UI

**Contexto:** en SPRINT 2 no hay análisis de ofertas reales conectado; el overlay
debe mostrarse con datos plausibles para validar el flujo visual completo.

**Decisión:** `OverlayDataSource` (interfaz) + `SimulatedOverlayDataSource`
(singleton). Expone `uiState: StateFlow<OverlayUiState>` y `start()/stop()`.
Emite una oferta simulada por plataforma (Uber, DiDi, Cabify, InDrive) cada
~20 s; la evaluación la produce el `ProfitEngine` real vía
`EvaluateOfferUseCase` (motor real, datos de entrada simulados), con decisión
`PROFITABLE` (insignia verde "CONVIENE"). El TTL de ocultado respeta
`OverlayConfig.ttlSeconds`. Las simulaciones NO se persisten en historial.

**Consecuencia:** `OverlayService` consume `OverlayDataSource` directamente (el
Foreground Service no debe depender de un ViewModel con ciclo de vida).
`OverlayViewModel` (`@HiltViewModel`) envuelve la misma fuente y sirve a las
pantallas (vista previa en Diagnóstico). `OfferEvaluator.uiState` queda sin
consumidor hasta reconectar el análisis real (sprint futuro); su persistencia en
historial se mantiene intacta.

### D2.4 — OverlayService se desacopla de `OfferEvaluator`

**Contexto:** el servicio dibujaba según `OfferEvaluator.uiState`.

**Decisión:** `OverlayService` inyecta `OverlayDataSource`; en
`onStartCommand` llama a `start()` (arranca la simulación) y en `onDestroy` a
`stop()`. Conserva `ProfitEngine` solo para formateo de valores en la UI.
Sigue con un único `ComposeView` y la estrategia add/remove por estado.

### D2.5 — Pantalla Diagnóstico

**Contexto:** se necesita verificar en un vistazo el estado de los 5
requisitos del overlay (overlay, accesibilidad, servicio/overlay en ejecución,
notificaciones, batería) con indicadores 🟢/🔴.

**Decisión:** `DiagnosisViewModel` + `DiagnosisScreen` en `:app`, ruta
`diagnosis` en `SircApp` (cuarto ítem de la barra inferior). Cada requisito se
muestra con `StatusDot` (Activo/Inactivo) y acción para abrir su ajuste cuando
corresponde. La pantalla incluye una vista previa del estado simulado del
overlay vía `OverlayViewModel`.

### D2.6 — El Overlay NO interactúa con la plataforma

**Decisión:** la cadena `OverlayManager → OverlayController → OverlayViewModel →
Compose Overlay` es de datos/estado únicamente. El Accessibility Service sigue
siendo solo lectura (ver `.ai/RULES.md` regla 9).

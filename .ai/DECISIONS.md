# Decisiones técnicas

> Registro de decisiones de diseño relevantes. Cada entrada indica contexto,
> opción elegida, alternativas descartadas y consecuencias. Se actualiza en
> cada sprint.

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

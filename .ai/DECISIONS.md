# Decisiones técnicas

> Registro de decisiones de diseño relevantes. Cada entrada indica contexto,
> opción elegida, alternativas descartadas y consecuencias. Se actualiza en
> cada sprint.

## SPRINT 5 — Primer Pipeline de Captura + OCR

### D6.1 — `CaptureAccessibilityService` dedicado y desacoplado de la UI

**Contexto:** el objetivo es un servicio de captura "desacoplado de la UI". El
`SircAccessibilityService` existente sirve al overlay (`OfferEventBus`) y además
reenvía eventos al pipeline de Sprint 4; tocarlo era un riesgo de regresión.

**Decisión:** se crea `CaptureAccessibilityService` (segundo servicio de
accesibilidad, `:feature:overlay`) que solo construye `CaptureRequest` y los
envía al `CapturePipeline`. No publica en el overlay ni conoce estados de
interfaz. Reutiliza la misma `accessibility_service_config.xml` (mismos
paquetes, `canRetrieveWindowContent=true`, `canPerformGestures=false`).

**Consecuencia:** dos servicios conviven sin regresión; la captura queda lista
para reemplazar al reenvío del `SircAccessibilityService` en un sprint futuro.

### D6.2 — `OverlayState` con estados mínimos en el pipeline

**Contexto:** el objetivo pide estados `Disabled`, `Waiting`, `Capturing`,
`Processing`, `Error` para el ciclo de vida del overlay/captura.

**Decisión:** enum `OverlayState` en `:core:capture` (puro). `CapturePipeline`
lo expone como `StateFlow` y lo transiciona a lo largo del proceso; el panel de
depuración lo observa. `OverlayService`/`OverlayManager` conservan su
arquitectura independiente (Sprint 2).

### D6.3 — `CapturePipeline` como orquestador puro en `:core:capture`

**Contexto:** el flujo objetivo es AccessibilityEvent → CaptureRequest →
ScreenCapture → OCR → OfferParser → OfferRepository.

**Decisión:** `CapturePipeline`/`DefaultCapturePipeline` en `:core:capture`
(puro) orquesta `ScreenCapture` → OCR (solo si hay imagen) → `OfferParser` →
`CaptureRepository`, reutilizando `OfferParser` y `CaptureRepository`
existentes. Falsos de prueba cubren cada etapa con JUnit puro.

**Alternativas descartadas:** orquestar en `:feature:overlay` (no testeable en
JVM puro); introducir un parser paralelo (duplicaba `OfferParser`).

### D6.4 — OCR bajo la abstracción `OcrEngine` (ML Kit sustituible)

**Contexto:** integrar ML Kit pero permitir sustituciones y pruebas.

**Decisión:** `OcrEngine` (interfaz, `:core:capture`) con `MlKitOcrEngine`
(`:feature:overlay`, `com.google.mlkit:text-recognition:16.0.1`, texto latino).
El pipeline solo invoca OCR si la solicitud lleva `imageData`; hoy la
accesibilidad aporta texto, así que la ruta OCR queda validada por pruebas con
falso OCR y por las imágenes de prueba.

**Consecuencia:** añadir captura de imagen (MediaProjection) no toca el
pipeline; la dependencia Android queda aislada en `:feature:overlay`.

### D6.5 — Flag `OCR` añadido a los Feature Flags

**Contexto:** igual que `PARSER`, el OCR debe poder desactivarse en caliente.

**Decisión:** nuevo valor `FeatureFlag.OCR` (default habilitado en dev). El
panel de depuración lo lista automáticamente (se itera sobre `entries`).

### D6.6 — Imágenes de prueba en recursos y tests del pipeline

**Contexto:** el objetivo pide un conjunto inicial de imágenes de prueba y
pruebas unitarias para el parser y el pipeline.

**Decisión:** PNGs por plataforma en `core/capture/src/test/resources/test-images/`
(Uber, DiDi, Cabify, InDrive). `DefaultCapturePipelineTest` los carga como
`ByteArray` reales y los hace recorrer el pipeline (falso OCR), validando
también el caso con imagen real + OCR, flags y fallos (captura/OCR/parser).

### D6.7 — `DebugPanelScreen.kt`: incidencias de ktlint ya resueltas

**Contexto:** el sprint pedía corregir un import sin usar y líneas >120 chars.

**Decisión:** quedaron resueltas en v0.5.0; `ktlintCheck` verifica que no
quedan incidencias. En este sprint solo se añade la fila `OCR` y el `Estado del
pipeline` al panel.

## SPRINT 4 — Plataforma de Captura (Infrastructure First)

### D5.1 — La infraestructura de captura vive en `:core:capture` (Kotlin puro)

**Contexto:** la captura de ofertas necesita observación, sesiones, snapshots,
parser, repositorio temporal, coordinación, feature flags y logging. Si viviera
en `:feature:overlay`, la lógica quedaría atada a Android y difícil de probar.

**Decisión:** nuevo módulo `kotlin("jvm")` `:core:capture` con el mismo patrón
que `:domain`/`:core:platform` (coroutines + `javax.inject`, sin Android). La
única pieza Android es `AccessibilityWindowObserver` y `AndroidSircLogger`, que
viven en `:feature:overlay` y se inyectan por Hilt (`CaptureModule` `@Binds`).

**Alternativas descartadas:** implementar todo en `:feature:overlay` (lógica
crítica no testeable en JVM puro); una capa `data` persistente desde ya (el
requisito es guardado temporal).

### D5.2 — El Accessibility Service solo observa y reenvía, sin interpretar

**Contexto:** el sprint prohíbe OCR, regex, IA y toda interpretación; el
servicio existente ya parsea para el flujo legacy (no se modifica).

**Decisión:** `SircAccessibilityService` reenvía cada cambio de ventana
relevante (evento, paquete, fingerprint, textos acotados) a
`AccessibilityWindowObserver` sin interpretarlo, manteniendo intacto el flujo
existente (`OfferEventBus`). `CaptureWindowEvent` transporta metadatos + texto
visible acotado como materia prima futura.

**Consecuencia:** el flujo legacy y la captura conviven; `FakeParser` ignora el
contenido y solo valida el flujo.

### D5.3 — `FakeParser` y `CaptureRepository` en memoria para validar el flujo

**Contexto:** el sprint exige "guardar temporalmente" y "generar datos simulados
para validar el flujo completo".

**Decisión:** `OfferParser` (interfaz) + `FakeParser` (snapshots con valores
simulados) y `CaptureRepository` (interfaz) + `InMemoryCaptureRepository`
(buffer de 50). Ambas interfaces quedan preparadas para parser/OCR y
persistencia reales.

### D5.4 — Feature Flags configurables con default encendido en dev

**Contexto:** se requieren flags de `ACCESSIBILITY`, `OVERLAY`, `CAPTURE`,
`PARSER` y `DEBUG_PANEL`, todos configurables.

**Decisión:** `FeatureFlag` + `FeatureFlags`/`InMemoryFeatureFlags` (en memoria,
default `true` en desarrollo). `CAPTURE` y `PARSER` gatean el pipeline; el
destino Debug se oculta si `DEBUG_PANEL` está desactivado.

**Consecuencia:** listo para deshabilitar piezas en producción; la persistencia
de flags (p. ej. DataStore) puede añadirse sin romper la interfaz.

### D5.5 — Logging centralizado deshabilitado fuera de debug

**Contexto:** el pipeline de captura loguea progreso; no debe haber logs en
producción.

**Decisión:** `SircLogger` (interfaz) en `:core:capture`; `AndroidSircLogger` en
`:feature:overlay` solo emite si la app es `FLAG_DEBUGGABLE`.

### D5.6 — Panel de depuración como destino extra en `:app`

**Contexto:** el panel debe mostrar estado de accesibilidad/overlay/captura/
parser, último snapshot, tiempo, memoria y eventos.

**Decisión:** `DebugPanelViewModel` + `DebugPanelScreen` en `:app` (destino
`Debug`, 5º ítem) combinando `OfferCaptureCoordinator.state`,
`OverlayManager.isRunning`, `FeatureFlags` y `PermissionManager`. El ítem se
oculta si `DEBUG_PANEL` está desactivado; el coordinador arranca en
`SircApplication.onCreate`.

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

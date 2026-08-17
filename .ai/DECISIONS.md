# Decisiones técnicas

> Registro de decisiones de diseño relevantes. Cada entrada indica contexto,
> opción elegida, alternativas descartadas y consecuencias. Se actualiza en
> cada sprint.

## SPRINT 8 — Motor de análisis de pantallas reales

### D9.1 — Detección de pantalla antes del parsing (`OfferDetectionEngine`)

**Contexto:** el pipeline parseaba todo texto visible, incluso Home, navegación
o pantallas de error, gastando trabajo y produciendo ruido.

**Decisión:** `OfferDetectionEngine` clasifica el texto en `ScreenType`
(HOME/REQUEST/TRIP/NAVIGATION/OFFLINE/ERROR/UNKNOWN) con palabras clave
ponderadas (REQUEST/ERROR peso 3, OFFLINE/NAVIGATION/TRIP peso 2, HOME peso 1);
solo `REQUEST` produce ofertas evaluables. La confianza normaliza
`(peso × aciertos / 8.0)`. Texto normalizado a minúsculas y sin acentos para
tolerar el OCR.

**Alternativas descartadas:** parsear siempre y descartar aguas abajo
(desperdiciaba OCR/parseo en cada cambio de ventana).

### D9.2 — Parsers especializados por tipo, orquestados por especificidad

**Contexto:** las pantallas de Uber varían (solicitud estándar, moto, XL,
reservado, radar) y un solo parser genérico no las distingue.

**Decisión:** `OfferType` + `OfferTypeParser`/`BaseOfferTypeParser` con 5 parsers
de Uber; `OfferParserOrchestrator` detecta la pantalla, prueba los especializados
**específicos primero** (Moto/XL/Radar/Reserva antes que Request, que es el más
greedy) y solo si la plataforma es Uber; si ninguno extrae, cae al extractor
genérico por plataforma. El tipo se reporta al pipeline como
`rawData = "type={OfferType.name}"`.

**Consecuencias:** las keywords de variantes (radar, moto, xl, reservado,
programado) se añadieron también a la detección `REQUEST` para que esas pantallas
produzcan oferta.

### D9.3 — Motor de Reglas con umbrales del conductor (`RuleEngine`)

**Contexto:** la decisión de rentabilidad necesitaba reglas explícitas y
verificables (ganancia, por km, por hora, distancia máx., recogida, duración).

**Decisión:** `RuleEngine` ejecuta 6 `OfferRule` y agrega `RuleEvaluation` con
`RuleVerdict` PASS/WARNING/FAIL. `RuleThresholds.from(DriverConfig)` deriva los
umbrales de rentabilidad del conductor; los límites operativos usan defaults.
Reglas con datos faltantes devuelven PASS "no disponible" sin bloquear la oferta.

**Consecuencias:** `TripOffer` ganó `pickupDistanceKm: Double?` para la regla de
recogida; `RuleEngine` se provee vía Hilt con `List<@JvmSuppressWildcards
OfferRule>` (ver D9.5).

### D9.4 — Confianza explícita con niveles (`ConfidenceEngine`)

**Contexto:** no bastaba con recomendar; había que señalar cuándo la señal es
poco fiable (datos incompletos, métricas incoherentes) para no engañar al
conductor.

**Decisión:** `ConfidenceEngine.assess` parte de 80 y penaliza datos faltantes
(−40), métricas incoherentes (−35, precio/km > 500 o precio/hora > 5000),
moneda ausente (−5), fallos de reglas (−25) o warnings (−15); reglas limpias
suman +10. Resulta `ConfidenceResult` con nivel HIGH/MEDIUM/LOW, % y razones.
LOW = "Información insuficiente" y nunca ACCEPT/REJECT.

### D9.5 — Listas inyectadas con `@JvmSuppressWildcards` y constructores sin default args

**Contexto:** Dagger fallaba de dos maneras: `List<OfferTypeParser>` se compila
como `List<? extends OfferTypeParser>` (Dagger lo interpreta como multibinding)
y un constructor `@Inject` con default args genera un segundo constructor
("may only contain one injected constructor").

**Decisión:** los providers usan `List<@JvmSuppressWildcards T>` explícito
(`PlatformModule` provee `OfferDetectionEngine`, los parsers, el orquestador y
el `RuleEngine`); los engines que reciben listas con default (`OfferDetectionEngine`,
`OfferParserOrchestrator`, `RuleEngine`) no llevan `@Inject` y se proveen
explícitamente. `ConfidenceEngine` conserva su `@Inject()` de un solo constructor.

### D9.6 — Confianza y reglas dentro de `PipelineOverlayDataSource`

**Contexto:** el overlay necesitaba exponer tipo de oferta, confianza y
veredicto de reglas sin tocar el pipeline puro.

**Decisión:** tras evaluar cada snapshot, el data source ejecuta
`RuleEngine.evaluate(RuleContext(offer, metrics, RuleThresholds.from(driverConfig)))`
y `ConfidenceEngine.assess` y publica `offerType`, `confidence` y
`ruleEvaluation` en `OverlayUiState`; `OverlayContent` muestra tipo + % de
confianza (e "Información insuficiente" en rojo cuando no es accionable) y el
panel de depuración añade la sección "Análisis". `DriverConfigRepository` se
inyecta para leer los umbrales reales del conductor.

### D9.7 — Tiempos de detección y reglas por oferta

**Contexto:** las etapas internas del motor (detección, reglas) no estaban
cronometradas.

**Decisión:** `OfferParserOrchestrator` mide detección y parsing y las reporta
en `ParsedOffer`; el pipeline las lleva a `ProcessingMetrics`/`OfferTiming`
(`detectionMillis`); el data source cronometra `rulesMillis`. Las regex del
`OfferTextParser` ya viven en el companion object (se compilan una sola vez).
El panel de depuración muestra Detección y Reglas.

## SPRINT 9 — Preparación beta cerrada (v1.0.0-beta)

### D10.1 — `CaptureSessionManager` como máquina de estados pura en `:domain`

**Contexto:** la beta necesita medir sesiones (duración, ofertas, errores) sin
atar el estado a Android ni a la UI.

**Decisión:** `CaptureSessionManager` (`@Singleton`, `:domain`) controla
`SessionStatus` (IDLE/ACTIVE/PAUSED) y acumula `SessionStats`; el reloj es un
parámetro inyectable (`clock`) que `copy()` conserva y que se excluye de la
igualdad estructural (`equals`/`hashCode` reescritos) para que los data class
comparen solo el estado. `PipelineOverlayDataSource` inicia la sesión en cada
snapshot y registra decisión/error.

**Alternativas descartadas:** reloj global mutable en el companion (estado
compartido frágil); `activeSeconds` con `System.currentTimeMillis()` directo
(no determinista en tests).

### D10.2 — Historial persistente ampliado en Room v3 (migración 1→3)

**Contexto:** el historial beta debe conservar el análisis detallado (tipo,
confianza, reglas, motivos, tiempos) y permitir un límite configurable.

**Decisión:** `OfferHistoryEntry` gana `offerType`, `confidencePercent`/`Level`,
`ruleSummary`, `reasons`, `recommendation` y tiempos; Room sube a **v3** con
`MIGRATION_2_3` (ALTER TABLE con 9 columnas) encadenada a la 1→2 existente.
`overlay_config.historyLimit` (default 500) limita el historial vía
`OfferHistoryDao.trimToLimit`, que el repositorio invoca tras cada inserción.

**Consecuencia:** el histórico sobrevive a reinicios (Room) y sirve de
diagnóstico sin depender del panel; `DriverConfigCodecTest` y los tests de
Room (`OfferHistoryDaoTest`, `SircDatabaseMigrationTest`) cubren la migración.

### D10.3 — Dashboard con gráficos Canvas, sin librería de charting

**Contexto:** el Dashboard necesita visualizar aceptación diaria y distribución
de decisiones sin añadir dependencias pesadas.

**Decisión:** `HistoryStatsCalculator` (función pura en `:domain`) produce
`HistoryStats` con `daily: List<DayStat>` y el `StatsScreen` dibuja barras y
donut con `Canvas`/`drawScope` del propio Compose. Sin librería externa.

### D10.4 — `SessionStats.clock` inyectable sin participar en `equals`

**Contexto:** `activeSeconds` se calcula en vivo, pero en pruebas debe ser
determinista y los data class deben comparar solo el estado.

**Decisión:** `SessionStats` lleva un `clock: (() -> Long)?` (constructor, lo
conserva `copy()`, default `System.currentTimeMillis`) y reescribe
`equals`/`hashCode` excluyéndolo. `CaptureSessionManager.setClockForTesting`
inyecta el reloj en las instancias que crea.

### D10.5 — `OverlayService` de vista única persistente

**Contexto:** el overlay parpadeaba y consumía recursos al agregar/quitar el
`ComposeView` en cada oferta.

**Decisión:** una sola vista persistente; ocultar = `FLAG_NOT_TOUCHABLE`
(cambios de tipo de ventana) y `visible` viaja en el estado. `onConfigurationChanged`
reclama tamaño/posición y `START_STICKY` ayuda al reinicio tras muerte del
proceso. `OverlayContent` anima visibilidad con `animateFloatAsState` y hace
crossfade estado↔evaluación con `AnimatedContent`.

### D10.6 — MediaProjection resiliente a cambios de configuración

**Contexto:** rotación/cambio de resolución podían invalidar el virtual display
y romper la captura.

**Decisión:** `MediaProjectionService.onConfigurationChanged` llama a
`provider.onDisplayConfigChanged()`, que recrea el `VirtualDisplay`; el provider
tiene `releaseVirtualDisplay()`/`drainFrames()` idempotentes. El OCR recicla el
bitmap y cancela la corrutina al abortar (`invokeOnCancellation`).

### D10.7 — Modo Beta: flags `RULES`/`DETAILED_LOGS`/`METRICS` y diagnóstico exportable

**Contexto:** la beta necesita apagar piezas (reglas, logs, métricas) en caliente
y enviar un informe a soporte.

**Decisión:** nuevos `FeatureFlag.RULES`/`DETAILED_LOGS`/`METRICS`.
`PipelineOverlayDataSource` gatea `RuleEngine` con `RULES`;
`AndroidSircLogger.debug` se apaga con `DETAILED_LOGS`. DebugPanel gana la
sección "Sesión de captura" y **Exportar diagnóstico** (share vía `Intent.ACTION_SEND`
con estado de sesión, promedios, última oferta y flags). Los flags siguen en
memoria (DataStore queda documentado como evolución sin romper la interfaz).

## SPRINT 10 — Hardening y Release Candidate (v1.0.0-rc1)

### D11.1 — Eliminación del flujo legacy `OfferEvaluator`/`OfferEventBus`

**Contexto:** desde el Sprint 7/8 el overlay y el historial los alimenta el
pipeline moderno (`PipelineOverlayDataSource` → Room). El flujo legacy
(`SircAccessibilityService` → `OfferEventBus` → `OfferEvaluator`) persistía un
historial **básico y duplicado** (mismo viaje escrito dos veces) y su
`uiState` no tenía consumidores.

**Decisión:** eliminar `OfferEvaluator` y `OfferEventBus`; `SircAccessibilityService`
conserva únicamente el reenvío de eventos de ventana (`AccessibilityWindowObserver`
→ coordinador/panel de depuración) y `OverlayService` deja de inyectar el
evaluador. El pipeline moderno es la única fuente de historial.

**Consecuencias:** fin del historial duplicado; `OfferTextParser` sigue vivo y
el `OfferParserOrchestrator` quedó 100 % descriptor-driven en WP-E3-01
(`ExtractorRegistry` y parsers especializados eliminados); sin cambios de API en
`:domain`.

### D11.2 — Modo de validación con `ValidationRecorder` (buffer acotado + informe)

**Contexto:** RC1 necesita observar qué ocurre en campo (errores OCR/parseo,
descartes, reglas fallidas, rechazos) y poder exportarlo.

**Decisión:** `ValidationRecorder` (`:core:capture`, puro, `@Singleton`) acumula
eventos tipados (`CaptureError`, `OcrFailed`, `ParseFailed`, `FrameDiscarded`
con `DiscardReason`, `RuleFailed`, `OfferRejected`) en un buffer de 500 eventos
recientes; `summary()` y `buildReport()` producen un informe legible. Se inyecta
en `DefaultCapturePipeline`, `PipelineOverlayDataSource` y
`MediaProjectionScreenCaptureProvider`. El DebugPanel muestra contadores,
exporta el informe y permite limpiarlo.

**Alternativas descartadas:** persistir eventos en Room (complejidad y desgaste
de almacenamiento para un diagnóstico efímero).

### D11.3 — El pipeline degrada a textos si el OCR falla (en lugar de ERROR)

**Contexto:** un frame con imagen que ML Kit no reconoce rompía toda la
solicitud (`OverlayState.ERROR`) aunque existieran textos de accesibilidad.

**Decisión:** en `resolveTexts`, si el OCR lanza una excepción se registra
`OcrFailed` y se usan `frame.texts`; los fallos no controlados del pipeline
registran `CaptureError` y se auto-recuperan en la siguiente solicitud.

**Consecuencia:** más ofertas analizadas en dispositivos reales y menos estados
de error transitorios; el informe de validación da visibilidad al fallo.

### D11.4 — Logs por niveles con `ERROR`/`WARNING` siempre activos

**Contexto:** la beta apagaba todos los logs en Release, impidiendo diagnosticar
incidencias de campo.

**Decisión:** `AndroidSircLogger` emite `ERROR`/`WARNING` siempre; `INFO` solo en
builds de desarrollo; `DEBUG` solo en desarrollo con el flag `DETAILED_LOGS`.
El modo se determina por `ApplicationInfo.FLAG_DEBUGGABLE` (más fiable que
`BuildConfig.DEBUG` en librerías).

**Consecuencia:** Release sigue sin logs verbosos (batería/I/O) pero conserva
errores y advertencias en logcat.

### D11.5 — `screenBounds()` con `WindowMetrics` para Android 15

**Contexto:** `WindowManager.defaultDisplay.getRealMetrics()` está deprecado en
Android 15 (targetSdk 35).

**Decisión:** `OverlayService.screenBounds()` usa
`WindowManager.getCurrentWindowMetrics().bounds` en API 30+ y el fallback
clásico (con `@Suppress("DEPRECATION")`) para API 24–29. `reclampOverlay`,
`moveOverlay` y `buildWindowParams` lo consumen.

**Consecuencia:** sin warnings de deprecación y preparado para Android 15.

## ESTRATEGIA DE PRODUCTO — Ruta consolidada (Sprint 12 planificación)

Decisión de dirección de producto a partir de las tres fuentes (auditoría
técnica, informe estratégico, análisis competitivo). Sin cambios de código;
solo dirección. Documentación: `docs/PRODUCT_STRATEGY.md` y
`docs/PRODUCT_COMPETITIVE_ANALYSIS.md`.

### D12.1 — Diferenciador: "solo lectura + <3 s + 100 % local"

**Contexto:** la investigación de mercado (FUENTE 3) confirmó que ninguna app
verificada combina decisión <3 s, solo lectura estricto, 100 % local y
multi-plataforma real; varias compiten con automatización de clics
(DecideRider, Autoindrive/Maxymo/Mystro, algunas no verificadas).

**Decisión:** el posicionamiento de SIRC es la herramienta **legalista**:
jamás automatizar interacción (auto-clic, auto-accept, gestos). Eso es a la vez
un diferenciador de marca y un requisito de Google Play.

**Consecuencias:** se añadieron reglas 9b/9c a `.ai/RULES.md`; la
automatización queda en la matriz EVITAR de `PRODUCT_STRATEGY.md`.

### D12.2 — El próximo hito es "Lanzamiento controlado" (beta cerrada + Play Integrity)

**Contexto:** el RC1 está endurecido y en verde; el informe estratégico
prescribe Play Integrity (Strong) como Hito 1; el mercado premia la narrativa
"Iegal, solo lectura".

**Decisión:** Sprint 12 = etapa E1 (beta cerrada real + Play Integrity + política
de Play al día). Las prioridades P0–P3 quedan fijadas en `PRODUCT_STRATEGY.md`.

**Consecuencias:** nueva sección "Ruta estratégica de producto" y "Sprint 12"
en `docs/ROADMAP.md`. No se implementa código hasta abrir la tarea (regla R16).

## ROADMAP GATE — Seguridad y Suscripción (revisión de coherencia, 16-ago-2026)

Revisión de coherencia entre estrategia competitiva, informe ejecutivo,
arquitectura, roadmap E0–E4 y el nuevo modelo comercial (app de pago por
suscripción). Solo documentación; sin cambios de código.
Documentación: `docs/SECURITY_MODEL.md` y `docs/BETA_READINESS.md`.

### D13.1 — Modelo LOCAL-FIRST (no "100 % local")

**Contexto:** el modelo de negocio de pago por suscripción exige servicios
remotos (identidad, suscripción, verificación de compra, integridad), lo que
contradecía el principio literal "100 % local".

**Decisión:** SIRC queda definido como **LOCAL-FIRST**. El procesamiento de
ofertas (OCR, parsing, evaluación, overlay, historial) es 100 % local y
**ninguna oferta/dato de pantalla sale del dispositivo**; los servicios remotos
se limitan a estado comercial/de cuenta (autenticación, entitlement, verificación
Play, Play Integrity, RTDN, recuperación).

**Consecuencias:** reglas 9d/9e/9f y secciones "1bis" de
`PRODUCT_STRATEGY.md`; el pipeline de captura no se contamina; la privacidad de
pantalla se mantiene.

### D13.2 — El APK se considera manipulable; la autorización vive en backend

**Contexto:** como app de pago, el APK publicado puede ser repackaged/reescrito;
confiar en chequeos locales de suscripción es inseguro.

**Decisión:** **nunca confiar en el cliente para decisiones críticas de
autorización.** El entitlement (suscripción activa, revocación) lo decide un
backend que verifica con Google Play Developer APIs
(`purchases.subscriptions.get`). Play Integrity es una **señal de entorno**
(Standard + tiered por defecto, Classic con nonce solo para restaurar compra),
no la barrera de suscripción. Offline: caché servidor firmada con TTL 24–72 h
(el reloj local nunca es autoridad); RTDN para revocación.

**Consecuencias:** threat model T1–T14 y trust model en `docs/SECURITY_MODEL.md`.

### D13.3 — Sprint 12 = E1a (beta cerrada del núcleo, SIN monetización); E1b comercial después

**Contexto:** la propuesta original "Sprint 12 = beta + Play Integrity (Strong)"
mezclaba la validación de producto con la infraestructura de cobro/fraude.

**Decisión:** **se rechaza la propuesta original y se reestructura.** Sprint 12 =
**E1a**: beta cerrada de validación del núcleo (overlay <3 s, OCR, estabilidad
10–15) **sin** Play Integrity con enforcement, sin billing, sin backend.
Play Integrity + backend + suscripción + RTDN + entitlement = **E1b**, etapa
posterior construida sobre los datos reales de la beta. La seguridad comercial
no queda al final del roadmap (E1b precede a E2), pero tampoco se despliega
antes de validar el producto.

**Consecuencias:** `docs/ROADMAP.md` (tabla E1a/E1b + Sprint 12), §7 y
guardrails 2–5 de `PRODUCT_STRATEGY.md`, P1 reordenada, `docs/BETA_READINESS.md`
(checklist de entrada + criterios de salida) y regla 9f.

### D13.4 — `EncryptedSharedPreferences` deprecada → primitivas Keystore directas

**Contexto:** androidx.security `EncryptedSharedPreferences` está **deprecada**
en 2026; no usarla en la cache de entitlement offline (E1b).

**Decisión:** si hace falta cifrado local en E1b, usar **Keystore directamente**
(hardware-backed, `isInsideSecureHardware`, StrongBox donde exista) para los
secretos de cache; la autoridad de autorización sigue siendo el backend (T3).

**Consecuencias:** sin dependencia deprecada; decisión aplicable solo al abrir
E1b (no se implementa en Sprint 12).

## SPRINT 11 — Eliminación de FakeParser (WP-E1-01)

### D11.6 — Eliminación de `FakeParser` de la ruta de producción

**Contexto:** `FakeParser` (injectable vía `@Inject @Singleton` en `src/main` de
`:core:capture`) permanecía disponible en el grafo de producción pese a que
`CaptureModule` ya bindea `PlatformOfferParser` como la implementación oficial de
`OfferParser`. El parser real conecta con `OfferParserOrchestrator`
(`:core:platform`) que ejecuta detección de pantalla + parsers especializados.

**Decisión:** eliminar completamente `FakeParser` de producción: se borra la
clase (`FakeParser.kt`), su test (`FakeParserTest.kt`) y se actualiza la KDoc de
`OfferParser` y `OfferSnapshot`. En tests, el `OfferCaptureCoordinatorTest` pasa
a usar un `FakeOfferParser` local (como ya hace `DefaultCapturePipelineTest`).
`PlatformOfferParser` es la única fuente de análisis oficial; el enum
`SnapshotSource` conserva `FAKE` para uso futuro de tests si se requiere, pero la
producción solo emite `REAL`.

**Consecuencias:** un único parser en la ruta de producción; no hay referencias
a `FakeParser` desde `src/main`; la interfaz de usuario no percibe cambios.

### D11.7 — Consolidación del motor de decisión: `ProfitEngine` único

**Contexto:** desde SPRINT 8, `PipelineOverlayDataSource` ejecutaba `RuleEngine`
(como brazo de análisis condicional) y `ProfitEngine` (como motor de decisión)
en paralelo. `RuleEngine` producía `RuleEvaluation` usada para afinar la
confianza y registrar `RuleFailed` en validación, duplicando lógicamente los
umbrales de `ProfitEngine` (MinimumProfit, MinimumProfitPerKm, etc.).

**Decisión:** eliminar `RuleEngine` de la ruta de producción.
`PipelineOverlayDataSource.analyze()` deja de recibir `ruleEngine`, el feature
flag `FeatureFlag.RULES` se elimina de `FeatureFlag`, y los providers
`provideRuleEngine`/`provideOfferRules` se eliminan de `PlatformModule`.
`ConfidenceEngine.assess` se invoca sin `ruleEvaluation` (parameter opcional
por defecto `null`). `ruleEvaluation` se expone como `RuleEvaluation(emptyList())`
en `OverlayUiState` para mantener compatibilidad de UI (WP: NO modificar UI).
`RecordValidationEvents` ya no registra `RuleFailed` (no hay rules ejecutadas).
`RuleEngine` se marca como LEGACY en KDoc para uso en tests futuros.

**Consecuencias:** `ProfitEngine` (via `ProfitEvaluationEngine` +
`RecommendationEngine`) es el único motor de decisión; no hay alternancia
entre motores; `PipelineOverlayDataSourceTest` verifica `ruleEvaluation` vacío.

### D11.8 — Consolidación de un único AccessibilityService

**Contexto:** existían dos servicios de accesibilidad registrados en
`AndroidManifest.xml`: `SircAccessibilityService` (legacy, reenvía eventos al
`AccessibilityWindowObserver` → `OfferCaptureCoordinator`) y
`CaptureAccessibilityService` (moderno, envía `CaptureRequest` a
`DebounceCaptureScheduler` → `DefaultCapturePipeline`). La duplicidad implicaba
dos servicios activos, configuración separada y riesgo de eventos duplicados.

**Decisión:** eliminar `SircAccessibilityService` y dejar
`CaptureAccessibilityService` como el único servicio. La funcionalidad de
observación para el panel de depuración se integra en `CaptureAccessibilityService`
inyectando `WindowEventPublisher` (nueva interfaz) que publica
`CaptureWindowEvent` a `AccessibilityWindowObserver` (que implementa
`WindowObserver` + `WindowEventPublisher`). `CaptureModule` añade el binding
`@Binds fun bindWindowEventPublisher(impl): WindowEventPublisher`.
`PermissionManager` referencia `CaptureAccessibilityService` para la detección de
permiso.

**Consecuencias:** un único servicio de accesibilidad en producción; no hay eventos
duplicados; el flujo único es `AccessibilityEvent → CaptureAccessibilityService →
DebounceCaptureScheduler → DefaultCapturePipeline → Overlay`; el panel de
depuración preserva su observación.

### D11.9 — Limpieza determinista de MediaProjection en `onDestroy()` (WP-E2-01)

**Contexto:** `MediaProjectionService` no implementaba `onDestroy()`; si el
servicio finalizaba sin pasar por `provider.stopProjection()` (muerte del
proceso FGS, `stopService` externo o interrupción del sistema), el
`MediaProjection`, el `VirtualDisplay`, el `ImageReader` y el callback quedaban
vivos (fuga). Además, un callback de `ImageReader` encolado tras `close()` podía
lanzar `IllegalStateException` (carrera callback/destrucción).

**Decisión:** `MediaProjectionService.onDestroy()` delega en
`provider.onServiceDestroyed()`, que ejecuta la secuencia idempotente de
liberación ya existente (`releaseResources()`: virtual display → unregister
callback → `MediaProjection.stop()`) y pone `_isProjecting = false`. El listener
del `ImageReader` valida que su reader siga siendo el actual antes de
`acquireLatestImage()` (descarta callbacks obsoletos tras el cierre). No se
reutiliza `stopProjection()` desde `onDestroy()` (evita un `stopService`
recursivo e innecesario durante la destrucción).

**Consecuencias:** sin fugas detectables cuando el servicio finaliza; `stop()` y
`onDestroy()` son seguros ante invocaciones múltiples; no hay callbacks
posteriores a la destrucción; el servicio puede reiniciarse (el provider vuelve a
`releaseResources()` antes de recrear la proyección). El módulo
`:core:capture:android` configura `testInstrumentationRunner =
"androidx.test.runner.AndroidJUnitRunner"` para que sus tests instrumentados
JUnit4 sean descubiertos (el runner legacy `android.test.InstrumentationTestRunner`
no los encontraba).

### D11.10 — Fortalecimiento del ciclo de vida de `ScreenCaptureProvider` (WP-E2-02)

**Contexto:** `MediaProjectionScreenCaptureProvider` mantenía banderas dispersas
y carecía de una máquina de estados explícita. Excepciones durante la creación del
`VirtualDisplay` o `ImageReader` podían dejar recursos parcialmente inicializados
(proyección adquirida sin display). Además, callbacks asíncronos (`onStop`) de una
sesión anterior cancelada podían interrumpir una nueva sesión tras una reinicialización.

**Decisión:**
1. `ProjectionLifecycle` (`internal class`) en `:core:capture:android` es la **única
   fuente de verdad** del estado interno (`IDLE`, `INITIALIZING`, `ACTIVE`). `isProjecting`
   se deriva estrictamente de `lifecycle.isActive` como proyección pública para la UI/pipeline.
2. `initializeProjection()` es **completamente atómico**: en un bloque `try/catch`,
   cualquier fallo realiza rollback inmediato (`releaseResources()`, `lifecycle.abort(token)`,
   `MediaProjectionService.stop()`) e informa a `ValidationRecorder` (`CaptureError`).
3. **Generation Token**: cada sesión incrementa un token entero. Los callbacks de
   `MediaProjection.Callback.onStop` e inicialización validan `lifecycle.isCurrent(token)`.
   Si el token es de una sesión obsoleta, la acción es ignorada.
4. Pruebas unitarias JVM puros (`ProjectionLifecycleTest`) cubren todas las transiciones,
   idempotencia, abordo y rechazo de tokens obsoletos.

**Consecuencias:**
Garantía de atomicidad sin estados parcialmente inicializados; re-inicialización segura
ante reinicios del servicio; callbacks obsoletos ignorados; sin regresiones funcionales.

### D11.11 — Motor de análisis descriptor-driven (WP-E3-01)

**Contexto:** el orquestador de parsing contenía conocimiento de plataforma
acoplado al código: parsers especializados solo para Uber
(`SpecializedParsers.kt`: `UberRequestParser`, `UberRadarParser`, etc.), un
`ExtractorRegistry` keyed por `RidePlatform` y un objeto `PlatformDescriptors`
con keywords. Añadir una plataforma o una variante de oferta obligaba a tocar
el núcleo del motor.

**Decisión:** modelar cada plataforma como `PlatformDescriptor` declarativo
(`platform`, `packageNames`, `detectionRules`, `offerTypes` como
`OfferTypeVariant` (type, keywords, refine?), `extractorKeywords`,
`defaultCurrency`) con estructura preparada para subdescriptores futuros
(`IdentityDescriptor`, `DetectionDescriptor`, `OfferTypeDescriptor`,
`ExtractionDescriptor`, `LocalizationDescriptor`). `PlatformDescriptorRegistry`
es el único validador — falla en construcción con `IllegalArgumentException`
(reglas/keywords vacías, plataformas o aliases duplicados, monedas inválidas
`[A-Z]{3}`, descriptor sin regla `ScreenType.REQUEST`, tipos de oferta
duplicados) y nunca en parseo — y precompila en `init` los motores/parsers/
extractores. Se eliminan `SpecializedParsers.kt` y `ExtractorRegistry`; el
objeto `PlatformDescriptors` se conserva como fuente de descriptores.
`OfferParserOrchestrator` resuelve todo desde el
registry (devuelve `ParsedOffer.none()` si la plataforma no está registrada).
`PlatformDescriptors.all()` mantiene los descriptores de UBER/DIDI/CABIFY/
INDRIVE (idénticos en comportamiento; Cabify conserva EUR por diseño).

**Alternativas descartadas:** interfaz `PlatformParser` por plataforma (crea N
clases y ramas); catálogo con `when(RidePlatform)` (acopla el núcleo); registro
que valida en parseo (fallaría en caliente, no en construcción).

**Consecuencias:** agregar una plataforma o variante = datos en un descriptor
(compatible con el sprint "Platform Agnostic Detection"); el orquestador y el
pipeline no contienen ramas por plataforma; validación en arranque; `:domain` y
`:core:platform` siguen Kotlin puro. Los 42 tests de `:core:platform` en verde
(incluidos 13 nuevos de registro/validación).

### D11.12 — Framework Genérico de Detección descriptor-driven (WP-E3-02)

**Contexto:** la resolución de plataforma dependía de `RidePlatform.fromPackageName`
(mapeo del enum) y la detección de pantalla vivía dentro de `OfferParserOrchestrator`;
WP-E3-02 busca consolidar el framework genérico de detección ya iniciado en WP-E3-01.

**Decisión:** `PlatformDetectionEngine` (servicio independiente que consume el
`PlatformDescriptorRegistry`) ejecuta la estrategia por etapas en una sola pasada
(O(n) descriptores): 1) `PACKAGE_MATCH` por coincidencia de packageName normalizado;
2) candidatos por keywords de detección (pantalla ≠ UNKNOWN); 3) único candidato →
`KEYWORD_CANDIDATE`; 4) empate por mayor `matchScore` → `AMBIGUOUS` (sin elegir);
5) sin candidatos → `NONE`. `DetectionMatcher` es una función pura sin estado;
`DetectionResult` es inmutable y autocontenido (descriptor, `ScreenDetection`,
`origin` con `DetectionOrigin`, diagnóstico). El registry solo expone una
`Collection<PlatformDescriptor>` de solo lectura. `OfferParserOrchestrator` añade
un overload `parse(texts, ts, packageName)` sin tocar el método por `RidePlatform`.

**Alternativas descartadas:** matchers componibles (sobreingeniería); scoring
heurístico con umbrales (rompe determinismo); callbacks de eventos dentro del
motor (rompe pureza de `:core:platform`).

**Consecuencias:** agregar una plataforma = datos en su descriptor; sin ramas por
plataforma; detección determinista y testeable; el motor no conoce el origen de
los textos (`DetectionOrigin` deja preparado el framework para galería/laboratorios).

### D11.13 — Unified Capture Source (WP-E3-03)

**Contexto:** la captura se repartía entre `ScreenCapture`/`ScreenFrame`/
`MediaProjectionScreenCapture` (frames), un pipeline que resolvía plataforma en
dos sitios (coordinador por `RidePlatform` y pipeline por detection engine) y un
coordinador que parseaba y guardaba snapshots, duplicando el guardado.

**Decisión:** `CaptureInput` es la única abstracción de entrada de captura
(`AccessibilityCaptureInput` en `:feature:overlay` y
`MediaProjectionCaptureInput` en `:core:capture:android`), expuestas como flujos
`@AccessibilityRequests`/`@CaptureRequests` fusionados por Hilt. El pipeline
único `DefaultCapturePipeline` consume `CaptureRequest` (dedup por `imageData` →
OCR si hay imagen → `PlatformDetectionEngine.detect(texts, ts, packageName,
origin)` → `OfferParser`/`OfferParserOrchestrator`) y emite `OfferSnapshot`.
`DetectionOrigin` se renombra a `CaptureInputType` (valores legacy aditivos +
ACCESSIBILITY/MEDIA_PROJECTION/SHARE) y `CaptureRequest`/`OfferSnapshot` llevan
`origin`. Se eliminan `ScreenCapture`/`ScreenFrame`/`MediaProjectionScreenCapture`
y `bindScreenCapture`; `CaptureFrameCache` queda solo como dedup por request.
`MediaProjectionCaptureInput` solo enriquece `imageData` si proyecta (degrade a
textos). El coordinador deja de parsear/guardar (consuma `pipeline.snapshots`)
y `OfferParser.parse(request, result, detectionMillis)` no depende de
`CaptureWindowEvent`. `CaptureAccessibilityService` queda como adaptador delgado.

**Alternativas descartadas:** mantener dos pipelines (duplica detección y
guardado); hacer de MediaProjection un "superframe" que re-emplee `ScreenFrame`
(legacy); resolver el origen de captura dentro del coordinador (mezcla de capas).

**Consecuencias:** una sola fuente de captura y de guardado de snapshots
(corrige doble escritura); la plataforma se resuelve una única vez en el
pipeline; agregar una fuente (Gallery/Share) = nuevo `CaptureInput` en el merge,
sin tocar el pipeline. `:core:capture` y `:core:platform` siguen Kotlin puro.

### D11.14 — Auditoría Sprint 11 y limpieza de severidad Alta (WP-E3-05A)

**Contexto:** aprobada `docs/audit/architecture/ARCHITECTURE_AUDIT_SPRINT11.md`
(29 hallazgos; 0 críticos, 3 altos, 9 medios, 10 bajos, 7 observaciones). La
corrección se dividió en Work Packages pequeños e independientes; WP-E3-05A
resuelve solo los tres hallazgos Altos.

**Decisión (A-1):** `OfferParserOrchestrator` queda con un **único** método de
parseo `parse(result, texts, ts, detectionMillis)`. Se eliminan los overloads
legacy `parse(texts, ts, RidePlatform)` y `parse(texts, ts, packageName)` y la
instancia interna `PlatformDetectionEngine`; el orquestador no ejecuta detección
(ni la duplica). `OfferParserOrchestratorTest` migra sus 14 escenarios a la API
definitiva construyendo `DetectionResult` vía `PlatformDetectionEngine`.

**Decisión (A-2):** las docs internas se corrigieron: el objeto
`PlatformDescriptors` **no fue eliminado** en WP-E3-01; se conserva como fuente
de descriptores (`PlatformDescriptors.all()`). Solo se eliminaron
`SpecializedParsers.kt` y `ExtractorRegistry`.

**Decisión (A-3):** `PlatformDetectionEngine` es la **única** lógica de
resolución de plataforma por paquete en todo el proyecto (mismo algoritmo,
normalización y fuente de verdad que el pipeline). `OfferCaptureCoordinator`
(`:core:capture`) y `AccessibilityCaptureInput` (`:feature:overlay`) inyectan el
motor y resuelven con `detect(emptyList(), ts, packageName)` →
`descriptor?.platform` / `isRecognized`. `PlatformDescriptorRegistry` permanece
como repositorio de datos y descriptores precompilados, **no** como segundo
mecanismo de resolución. `RidePlatform.fromPackageName` se deprecia con
`@Deprecated`.

**Alternativas descartadas:** conservar los overloads con `@VisibleForTesting`
(compatibilidad artificial solo para tests); resolver con
`PlatformDescriptorRegistry.descriptorForPackageName` en coordinador/input
(segunda lógica, divergente de la normalización del pipeline).

**Consecuencias:** un único camino de parseo y una única lógica de detección en
producción y tests; futuras fuentes de captura (Gallery/Share) reutilizan el
mismo motor; comportamiento observable idéntico para el conductor.

## SPRINT 7 — Evaluación en tiempo real con recomendación

### D8.1 — `ProfitEvaluationEngine` delega en `ProfitEngine` (no duplica fórmulas)

**Contexto:** el sprint pide evaluar la oferta detallada (costos derivados del
perfil del conductor) y decidir con umbrales; ya existe `ProfitEngine` con la
fórmula de ganancia/margen/decisión probada.

**Decisión:** `ProfitEvaluationEngine` reutiliza `ProfitEngine` (delegación) y
solo deriva los costos: `costPerKm` = combustible + mantenimiento + costos
adicionales (todo desde `DriverConfig`); `costPerMinute` y `costPerTrip` pasan
de la configuración. No se duplica ninguna fórmula ni umbral.

**Alternativas descartadas:** reescribir el cálculo en un motor nuevo
(duplicaba lógica crítica probada); tocar `ProfitEngine` para que calcule
costos (rompía su contrato puro oferta+costos+umbrales).

### D8.2 — Umbrales de decisión SOLO desde `DriverConfig.thresholds`

**Contexto:** el objetivo exige que los umbrales vengan solo de la configuración
del conductor (sin constantes).

**Decisión:** `ProfitEvaluationEngine` entrega los umbrales configurados
(`config.thresholds`) a `ProfitEngine`; no define ningún valor por defecto. Si no
hay conductor configurado, `EvaluateDetailedOfferUseCase` usa
`DriverConfig.default()` para no romper el flujo.

### D8.3 — Recomendación accionable con motivo y confianza

**Contexto:** el conductor decide en <3 s; la decisión técnica (`Decision`) debe
traducirse a una acción clara.

**Decisión:** `RecommendationEngine` produce `OfferRecommendation`:
`ACCEPT`/`REJECT`/`WARNING` según la decisión, motivo principal, métricas usadas
y % de confianza = `(50 + |margen|/3).coerceIn(50, 98)` (WARNING = 50). La UI lo
muestra como insignia semáforo con el motivo y el %.

### D8.4 — Historial en memoria para el overlay/Debug (Room se mantiene aparte)

**Contexto:** el objetivo pide historial de 100 ofertas sin Room, para el flujo
en tiempo real del overlay.

**Decisión:** `OfferEvaluationRepository` (contrato `:domain`) +
`InMemoryOfferEvaluationRepository` (sincronizado, ids incrementales, retiene 100)
en `:feature:overlay`. `PipelineOverlayDataSource` persiste cada oferta evaluada
y `DebugPanelViewModel` lee la última. El historial persistente de
`:feature:history` (Room) no se toca: dos fuentes con distinto propósito.

### D8.5 — `OfferTiming` + `OfferPerformanceTracker` para medir por oferta

**Contexto:** se necesitan tiempos por etapa (captura/OCR/parseo/evaluación/
overlay/total) y promedios de las últimas 20 ofertas.

**Decisión:** `OfferTiming` en `:core:capture` (puro) y
`OfferPerformanceTracker`/`InMemoryOfferPerformanceTracker` (retiene 100,
promedio de 20, `merge` completa la última entrada cuando falta overlay).
`DefaultCapturePipeline` registra captura/OCR/parseo/total; el data source
registra evaluación/overlay. Panel de depuración: "Última oferta" +
"Rendimiento (promedio últimas 20 ofertas)".

### D8.6 — `OfferSnapshot.texts` transporta los textos OCR

**Contexto:** para evaluar la oferta real el overlay necesita el contenido
visible (textos OCR), no solo la imagen cruda.

**Decisión:** `OfferSnapshot` añade `texts: List<String> = emptyList()`;
`FakeParser` los puebla desde el evento de captura y
`PipelineOverlayDataSource` los mapea al `TripOffer` (con `rawData` como
respaldo).

### D8.7 — Combines anidados en `DebugPanelViewModel`

**Contexto:** al añadir `performanceTracker.averages` e historial (última
oferta) se superaban los 5 flows que soporta `Flow.combine`.

**Decisión:** se agrupa el estado base en `PipelineSnapshot` (combine de 5) y el
par promedio+última en un `performance` (combine de 2), anidando los resultados.
Sin estados globales ni flows intermedios visibles al resto de la app.

## SPRINT 6 — Captura de pantalla real con MediaProjection

### D7.1 — MediaProjection vive en `:core:capture:android` (nuevo módulo Android)

**Contexto:** la captura de imagen real requiere APIs Android
(`MediaProjectionManager`, `VirtualDisplay`, `ImageReader`) y un Foreground
Service, pero `:core:capture` debe seguir siendo Kotlin puro.

**Decisión:** nuevo módulo `:core:capture:android` (Android library, Hilt) que
implementa `ScreenCaptureProvider`/`MediaProjectionScreenCaptureProvider` (token,
virtual display, último frame), `MediaProjectionService` (FGS tipo
`mediaProjection`), `MediaProjectionScreenCapture` (implementa `ScreenCapture`)
y `DebugCaptureMetrics`. `:feature:overlay` y `:app` dependen de él; el pipeline
puro solo ve las interfaces de `:core:capture`.

**Alternativas descartadas:** implementar la captura en `:feature:overlay`
(junto al servicio, pero repetía el problema de lógica no testeable y mezclaba
responsabilidades); en `:core:capture` (rompía su pureza JVM).

### D7.2 — `ScreenCaptureProvider` desacoplado de la UI

**Contexto:** la UI debe entregar el resultado del consentimiento y controlar la
proyección, pero no debe tocar `MediaProjectionManager`/`VirtualDisplay`.

**Decisión:** `ScreenCaptureProvider` (interfaz) es el único que posee el token,
el `VirtualDisplay` y el frame; `OverlayManager` solo expone
`createScreenCaptureIntent()`, `startProjection(resultCode, data)` y
`stopProjection()` (delega en el provider) y `projectionActive`. En Android 14+
el FGS tipo `mediaProjection` debe arrancar antes de pedir el token: el provider
lo arranca con el `resultCode`/`data` y el servicio completa la proyección.

### D7.3 — `MediaProjectionService` como FGS tipo `mediaProjection`

**Contexto:** Android 14+ exige que la creación del `MediaProjection` ocurra
dentro de un FGS declarado con `foregroundServiceType="mediaProjection"` (más
`PROPERTY_SPECIAL_USE_FGS_SUBTYPE`).

**Decisión:** el servicio arranca vía `startForegroundService` desde la
Activity tras el consentimiento, inicia la proyección en `onStartCommand` y se
detiene con `stopService` al terminar. Canal de notificación de importancia
baja (`sirc_capture`).

### D7.4 — Caché de frames por hash de contenido

**Contexto:** la accesibilidad genera eventos muy frecuentes y capturas
idénticas del mismo frame; reprocesarlas desperdicia OCR, CPU y batería.

**Decisión:** `CaptureFrameCache` + `InMemoryCaptureFrameCache` (LRU de 32
entradas, clave `img-<contentHashCode>`) en `:core:capture` (puro). El
`DefaultCapturePipeline` consulta `isNew(frame)` y omite el procesamiento de
frames repetidos (se devuelve `idle()`).

### D7.5 — `DebounceCaptureScheduler` para coalescer requests de accesibilidad

**Contexto:** cada cambio de ventana de accesibilidad genera un `CaptureRequest`;
disparar OCR en cada uno es inviable.

**Decisión:** `DebounceCaptureScheduler` (`:core:capture`, puro) con
`MutableSharedFlow` (DROP_OLDEST, buffer 64) y `debounce(400 ms)`;
`CaptureAccessibilityService` registra requests y el pipeline se suscribe al
flujo debounced.

### D7.6 — El overlay consume el estado real del pipeline

**Contexto:** el overlay se alimentaba de `SimulatedOverlayDataSource`; el
pipeline ya produce estado y snapshots reales.

**Decisión:** `PipelineOverlayDataSource` (`@Singleton`) consume
`pipeline.state` y `pipeline.snapshots`, evalúa con `EvaluateOfferUseCase`
(motor real) y expone `OverlayUiState` con `status` (`OverlayState`) y
`visible` (= status != DISABLED o hay evaluación). `OverlayContent` muestra un
`StatusLabel` (Esperando oferta…/Capturando pantalla…/Analizando oferta…/Error al
analizar). Se eliminan `SimulatedOverlayDataSource` y `AccessibilityScreenCapture`.

### D7.7 — Métricas por etapa expuestas por el pipeline

**Contexto:** se necesita medir el rendimiento por etapa (captura/OCR/parseo)
para cumplir el objetivo de decisión en <3 s.

**Decisión:** `ProcessingMetrics` (tiempos por etapa) en
`CapturePipeline.lastMetrics: StateFlow` y `CaptureMetrics` (interfaz, solo
loguea en debug vía `DebugCaptureMetrics`). El panel de depuración muestra las
filas Captura/OCR/Parseo/Total.

### D7.8 — El permiso de captura se pide en Home y las métricas se ven en Debug

**Contexto:** el usuario debe otorgar el consentimiento de MediaProjection y el
desarrollador debe poder inspeccionar el rendimiento.

**Decisión:** `HomeViewModel`/`HomeScreen` añaden la sección "Captura de
pantalla" (lanzador `StartActivityForResult` → `startProjection`,
`projectionActive`, "Detener captura"); `DebugPanelViewModel`/`DebugPanelScreen`
consumen `pipeline.lastMetrics`.

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

# WP-E3-03 — Unified Capture Source (spec)

> Fecha: 2026-08-06 · Estado: borrador (listo para revisión del usuario) · Epic: EPIC-03 (Platform Agnostic Detection)

## Objetivo

Eliminar el acoplamiento entre MediaProjection y el pipeline de análisis.
Cualquier origen de captura (Accessibility, MediaProjection, futuros Gallery/Share)
recorre exactamente el mismo flujo:

```
CaptureInput → (OCR si hay imagen) → PlatformDetectionEngine → OfferParserOrchestrator → OfferSnapshot → Overlay
```

Un solo pipeline, un solo punto de resolución de plataforma, sin parsers ni
pipelines paralelos. **Refactor genérico**: no se implementan aún Gallery ni Share
(queda soportado por la arquitectura; la UI y los permisos son un WP posterior).

## Contexto — problemas actuales (confirmados en el código)

1. **Acoplamiento MediaProjection ↔ pipeline.** `DefaultCapturePipeline` invoca
   `ScreenCapture.capture(request)` internamente (hoy `MediaProjectionScreenCapture`).
   El pipeline no debería conocer el origen de la captura.
2. **Resolución de plataforma duplicada 4-5 veces.** `RidePlatform.fromPackageName`
   se llama en: `CaptureAccessibilityService`, `DefaultCapturePipeline`
   (`UNSUPPORTED_PLATFORM`), `PlatformOfferParser` y `OfferCaptureCoordinator`,
   además de la resolución interna del `OfferParserOrchestrator`. Además existe la
   vía `DetectionMatcher`/keywords de WP-E3-02, muerta en producción.
3. **`PlatformDetectionEngine` infrautilizado.** Su etapa keywords solo se ejerce en
   tests; `timestampMillis` nunca se usa. Es el único punto de resolución previsto
   por WP-E3-02 y no está conectado al pipeline real.
4. **`OfferParser` acoplado a `CaptureWindowEvent`.** `OfferParser.parse(event,
   session)` obliga a construir un evento de ventana dentro del pipeline solo para
   parsear.
5. **`OfferParserOrchestrator` re-resuelve la plataforma.** Su overload por
   `platform` vuelve a buscar el descriptor; el pipeline ya resolvió.
6. **Dos pipelines paralelos.** `OfferCaptureCoordinator` (debug) parsea Y guarda
   snapshots por su cuenta; el pipeline también los guarda. Ambos comparten el mismo
   `CaptureRepository` singleton → **guardado duplicado** de snapshots.
7. **`ScreenFrame` como intermediario redundante.** Solo existe para conectar
   `ScreenCapture` → pipeline; no aporta información que no esté en `CaptureRequest`.
8. **Sin etiqueta de origen.** `CaptureRequest` y `OfferSnapshot` no indican de dónde
   vino la captura (accesibilidad vs proyección vs futuro galería), lo que impide
   diagnóstico y lógica condicional por fuente.

## Decisiones aprobadas por el usuario (renames)

- **`CaptureSource` → `CaptureInput`.** No toda entrada "captura": Gallery/Share
  solo alimentan el pipeline.
- **`DetectionOrigin` → `CaptureInputType`.** Se eligió `CaptureInputType` por
  coherencia con el prefijo `Capture*` existente (`CaptureRequest`,
  `CapturePipeline`, `CaptureModule`, etc.). Los valores pasan a representar la
  **fuente de entrada**, no el origen de detección.
- El resto del diseño (Enfoque 1) queda aprobado sin cambios.

## Arquitectura objetivo

```
AccessibilityEvent (sistema)
        ↓  CaptureAccessibilityService (adaptador Android delgado)
AccessibilityCaptureInput.onAccessibilityEvent(event, root)
        ↓  filtro por plataforma · collectTexts (400/80/200) · dedup fingerprint
CaptureRequest(texts, packageName, origin=ACCESSIBILITY) → scheduler (debounce 400 ms)
        ↓  AccessibilityCaptureInput.requests()
MediaProjectionCaptureInput.requests()  (enrich: si proyecta → imageData, origin=MEDIA_PROJECTION)
        ↓  @CaptureRequests Flow<CaptureRequest>  (compuesto; futuro: merge de Gallery/Share)
DefaultCapturePipeline.process(request)
        ↓  dedup por imageData → OCR (si imagen) → PlatformDetectionEngine.detect(texts, ts, packageName, origin)
DetectionResult
        ↓
OfferParser.parse(request, result, detectionMillis)  →  OfferParserOrchestrator.parse(result, texts, ts, detectionMillis)
OfferSnapshot(sessionId, platform, ..., origin, detectionMillis)
        ↓  repository.save + _snapshots (único escritor)
OfferCaptureCoordinator (consumidor debug: snapshots + windowEvents → CaptureState)
PipelineOverlayDataSource (overlay real)
```

## Componentes

### `CaptureInputType` (`:core:platform`) — rename de `DetectionOrigin`

- Renombrado in-place: `core/platform/.../DetectionOrigin.kt` → `CaptureInputType.kt`.
  Se queda en `:core:platform` porque `DetectionResult.origin` y
  `PlatformDetectionEngine.detect(origin=...)` lo usan, y `:core:capture` ya depende
  de `:core:platform` (moverlo a `:core:capture` crearía un ciclo).
- Valores **aditivos** (aprobado): se conservan `PACKAGE`, `OCR`, `GALLERY`, `TEST`,
  `UNKNOWN` (semántica legacy de origen de detección, hoy solo usados en tests) y se
  añaden `ACCESSIBILITY`, `MEDIA_PROJECTION`, `SHARE`.
- `DetectionResult.origin: CaptureInputType` y
  `PlatformDetectionEngine.detect(origin: CaptureInputType = UNKNOWN)`. Mismo
  contrato, mismo comportamiento.

### `CaptureInput` (`:core:capture`, Kotlin puro)

`core/capture/src/main/kotlin/com/sirc/capture/input/CaptureInput.kt`

```kotlin
interface CaptureInput {
    val origin: CaptureInputType
    fun requests(): Flow<CaptureRequest>
}
```

- `origin` identifica la fuente; los `CaptureRequest` emitidos llevan su propio
  `origin` por request.
- `:core:capture` ya depende de `kotlinx.coroutines.flow` y de `:core:platform`.

### `AccessibilityCaptureInput` (`:feature:overlay`)

`feature/overlay/src/main/kotlin/com/sirc/feature/overlay/AccessibilityCaptureInput.kt`

- **Extrae la lógica completa de `CaptureAccessibilityService`**: filtro por
  `RidePlatform.fromPackageName`, filtro de tipos de evento, `collectTexts` con los
  límites duros 400 nodos / 80 textos / ≤200 chars, dedup por fingerprint, armado de
  `CaptureRequest(texts, packageName, origin = ACCESSIBILITY)`, encolado en el
  `DebounceCaptureScheduler` y reenvío del `CaptureWindowEvent` al
  `WindowEventPublisher` (para el panel debug, sin duplicación).
- `@Singleton`, `@Inject constructor(scheduler: DebounceCaptureScheduler,
  windowEventPublisher: WindowEventPublisher)`.
- API: `fun onAccessibilityEvent(event: AccessibilityEvent, root:
  AccessibilityNodeInfo?)` y `override fun requests() = scheduler.debouncedRequests()`.
- `CaptureAccessibilityService` queda como adaptador delgado Android que delega en
  este input y colecciona el flujo compuesto en el pipeline.

### `MediaProjectionCaptureInput` (`:core:capture:android`)

`core/capture/android/src/main/kotlin/com/sirc/capture/android/MediaProjectionCaptureInput.kt`

- Reemplaza a `MediaProjectionScreenCapture` (se elimina).
- `@Singleton`, `@Inject constructor(@AccessibilityRequests baseRequests:
  Flow<CaptureRequest>, provider: ScreenCaptureProvider, logger: SircLogger)`.
- `requests()` mapea la corriente base (ya debounced) request a request:
  - Si `provider.isProjecting.value`: captura el frame (`provider.captureFrame()`),
    lo codifica a PNG y emite `request.copy(imageData = png, origin =
    MEDIA_PROJECTION)`.
  - Si no proyecta, o el frame es `null` (degrade): emite el request sin cambios
    (origin queda `ACCESSIBILITY`, se usan los textos).
- `origin = MEDIA_PROJECTION`.
- **Nueva dependencia:** `:core:capture:android/build.gradle.kts` añade
  `implementation(project(":core:platform"))` (necesita `CaptureInputType`; no hay
  ciclo porque `:core:platform` solo depende de `:domain`).

### `CaptureRequest` (`:core:capture`) — DTO universal enriquecido

- Añade `val origin: CaptureInputType = CaptureInputType.UNKNOWN` (default → los
  constructores existentes y tests siguen compilando).
- Sigue siendo el único DTO de entrada del pipeline; ya transporta `texts`,
  `packageName`, `timestampMillis`, `imageData` y ahora `origin`.

### `OfferSnapshot` (`:core:capture`)

- Añade `val origin: CaptureInputType = CaptureInputType.UNKNOWN`.

### `DefaultCapturePipeline` (`:core:capture`)

- **Constructor:** pierde `ScreenCapture`, gana `PlatformDetectionEngine`
  (proporcionado por `PlatformModule`).
- `process(request)`:
  1. Flag `CAPTURE` (igual).
  2. Dedup con `CaptureFrameCache` **keyed en `request.imageData`** (misma clave que
     hoy; request sin imagen siempre se considera nuevo).
  3. `resolveTexts(request)`: si hay `imageData` y flag `OCR` → OCR (fallback a
     `request.texts`); si no → `request.texts`. Igual que hoy con frame→request.
  4. Textos vacíos → `NO_TEXTS`.
  5. **Resolución única:** `platformDetectionEngine.detect(texts,
     request.timestampMillis, request.packageName, request.origin)`. Si
     `!result.isRecognized` → `UNSUPPORTED_PLATFORM`.
  6. Flag `PARSER` (nuevo en el pipeline; hoy lo checa el coordinador, y el
     coordinador ya no parsea). Si está desactivada → sin snapshot.
  7. `parser.parse(request, result, detectionMillis)` dentro de try/catch →
     `ParseFailed`.
  8. Si hay snapshot: `cache.markProcessed(request)`, `repository.save(snapshot)`,
     `performanceTracker.record(...)`, `_snapshots.tryEmit(snapshot)`.
- **Métricas:** se elimina `metrics.onCapture` (la captura ocurre aguas arriba en
  `MediaProjectionCaptureInput`). `ProcessingMetrics.captureMillis` y
  `OfferTiming.captureMillis` quedan en `0.0` (detalle solo de Debug). `detectionMillis`
  se mide en el pipeline y fluye por el parser → snapshot → métricas.
- No se construye más `CaptureWindowEvent` en el pipeline (el parser ya no lo usa).
- La sesión del snapshot pasa a `sessionId = "pipeline-${request.id}"` (igual que hoy,
  pero calculada por el parser a partir de `request`).

### `CaptureFrameCache` (`:core:capture`)

- `isNew(frame: ScreenFrame)` / `markProcessed(frame)` →
  `isNew(request: CaptureRequest)` / `markProcessed(request)`. Clave
  `"img-${imageData.contentHashCode()}"` (idéntica). `ScreenFrame` se elimina.

### `OfferParser` (`:core:capture`)

```kotlin
interface OfferParser {
    fun parse(request: CaptureRequest, result: DetectionResult, detectionMillis: Double): OfferSnapshot?
}
```

- Ya no depende de `CaptureWindowEvent` ni de `OfferCaptureSession`. Solo usa datos
  contenidos en `CaptureRequest` + `DetectionResult` (+ el tiempo de detección medido
  por el pipeline, para preservar la métrica).

### `PlatformOfferParser` (`:core:capture`)

- `val platform = result.descriptor?.platform ?: return null` (sin
  `RidePlatform.fromPackageName`).
- `if (request.texts.isEmpty()) return null`.
- `val parsed = orchestrator.parse(result, request.texts, request.timestampMillis,
  detectionMillis)`.
- Construye `OfferSnapshot` con `sessionId = "pipeline-${request.id}"`, `platform`
  desde el `DetectionResult`, `origin = request.origin` y `detectionMillis =
  parsed.detectionMillis`.

### `OfferParserOrchestrator` (`:core:platform`) — nuevo overload

```kotlin
fun parse(result: DetectionResult, texts: List<String>, timestampMillis: Long, detectionMillis: Double = 0.0): ParsedOffer {
    if (!result.isRecognized || !result.screenDetection.isRequest) return ParsedOffer.none()
    val descriptor = result.descriptor ?: return ParsedOffer.none()
    return parseWith(descriptor, result.screenDetection, texts, timestampMillis, System.nanoTime(), detectionMillis)
}
```

- Consume el `DetectionResult` tal cual: no re-resuelve ni re-detecta.
- Los overloads existentes (`platform` y `packageName`) se conservan intactos
  (compatibilidad con WP-E3-02 y sus tests).

### `OfferCaptureCoordinator` (`:core:capture`) — unificado como consumidor debug

- **Constructor:** pierde `parser`, gana `pipeline: CapturePipeline`. Conserva
  `windowObserver`, `captureRepository` (solo para `reset()`), `featureFlags`,
  `logger`.
- `start()` colecciona (a) `windowObserver.windowEvents` → `onWindowEvent` y
  (b) `pipeline.snapshots` → `onSnapshot`.
- `onWindowEvent(event)`: si flag `CAPTURE` off → ignore; mantiene el ciclo de vida
  de sesión (`ensureSession`/`closeActiveSession` por `RidePlatform.fromPackageName`
  — metadato de sesión, no parsing) y actualiza `eventsProcessed`/`recentEvents`.
- `onSnapshot(snapshot)`: actualiza `lastSnapshot`, incrementa
  `capturedSnapshotCount` de la sesión activa y setea `lastProcessingTimeMillis`
  desde `pipeline.lastMetrics.value.totalMillis`.
- **Ya no guarda snapshots** → se corrige el guardado duplicado. `CaptureState`
  intacto (contrato con `DebugPanelViewModel`).

### DI (`:feature:overlay`)

- `CaptureModule`: providers con qualifier `@AccessibilityRequests
  Flow<CaptureRequest>` (= `accessibilityInput.requests()`) y `@CaptureRequests
  Flow<CaptureRequest>` (= `mediaProjectionInput.requests()`). El service inyecta
  `@CaptureRequests` y colecciona → `pipeline.process`.
- `CaptureAndroidModule`: elimina `bindScreenCapture` (ya no existe `ScreenCapture`).
- `PlatformModule`: nuevo `@Provides fun providePlatformDetectionEngine(registry) =
  PlatformDetectionEngine(registry)` para el pipeline.
- El qualifier `@AccessibilityRequests` se define en `:core:capture`
  (`com.sirc.capture.di`) para ser visible desde `:core:capture:android` y
  `:feature:overlay`.

## Eliminaciones

- `core/capture/.../screen/ScreenCapture.kt`
- `core/capture/.../model/ScreenFrame.kt`
- `core/capture/android/.../MediaProjectionScreenCapture.kt`
- `bindScreenCapture` en `CaptureAndroidModule`
- `RidePlatform.fromPackageName` en pipeline y parser (solo queda en el filtro de
  entrada del servicio y en el ciclo de vida de sesión del coordinador).

## Flujo resultante (resumen)

Accesibilidad → `AccessibilityCaptureInput` → debounce → `MediaProjectionCaptureInput`
(imagen si proyecta) → **un solo pipeline** (dedup → OCR → detección única → parser
→ snapshot) → coordinador (debug) + overlay. Gallery/Share futuros = un `CaptureInput`
más en el merge `@CaptureRequests`, sin tocar el pipeline.

## Tests (TDD, RED → GREEN)

- `:core:platform`
  - Rename de `DetectionOrigin` → `CaptureInputType` en `DetectionResultTest` y
    `PlatformDetectionEngineTest` (valores conservados + nuevos).
  - `OfferParserOrchestratorTest`: nuevo overload `parse(result, texts, ts, det)` —
    resultado reconocido → `ParsedOffer` con oferta; no reconocido → `none()`;
    `screenDetection.isRequest == false` → `none()`.
- `:core:capture`
  - `CaptureRequest` y `OfferSnapshot`: `origin` default `UNKNOWN`.
  - `InMemoryCaptureFrameCacheTest`: keyed en `CaptureRequest.imageData` (mismos
    escenarios que hoy).
  - `DefaultCapturePipelineTest` (reescrito): sin `FakeScreenCapture`; `FakeDetectionEngine`
    con `isRecognized` configurable → plataforma/`UNSUPPORTED_PLATFORM`; flag `PARSER`
    gate; dedup por `imageData`; `origin` propagado al snapshot; OCR degrada a textos;
    métricas (sin `onCapture`, con `detectionMillis`).
  - `OfferCaptureCoordinatorTest` (reescrito): consume `pipeline.snapshots` +
    `windowObserver.windowEvents`; `eventsProcessed`/`recentEvents`; ciclo de sesión;
    flag `CAPTURE`; `reset`; `start/stop`. Ya no parsea ni guarda.
- `:core:capture:android`
  - `MediaProjectionCaptureInputTest`: proyecta → `imageData` + `origin =
    MEDIA_PROJECTION`; no proyecta → passthrough con `origin = ACCESSIBILITY`; frame
    null → degrade. Con `FakeScreenCaptureProvider` y `FakeBaseRequests`.
- `:feature:overlay`
  - `AccessibilityCaptureInput` se verifica por integración/regresión (usa tipos
    Android; sin tests unitarios nuevos, igual que hoy el service).

## Documentación

- `docs/CHANGELOG.md` (WP-E3-03: pipeline unificado, origen en request/snapshot,
  corrección del doble guardado).
- `.ai/CONTEXT.md` (actualizar mención a `DetectionOrigin` y al flujo).
- `.ai/DECISIONS.md` (decisión D11.x: `CaptureInput` + `CaptureInputType`).
- `TASK.md` (estado en vivo).

## Verificación

- `.\gradlew.bat :core:platform:test :core:capture:test :domain:test
  :feature:overlay:testDebugUnitTest :core:capture:android:testDebugUnitTest
  testDebugUnitTest --console=plain`
- `.\gradlew.bat lintDebug assembleDebug --console=plain`
- `.\gradlew.bat ktlintCheck --console=plain` (documentando solo fallos preexistentes,
  si los hubiera).

## Fuera de alcance

- Implementación de Gallery (`READ_MEDIA_IMAGES`, Photo Picker) y Share (intent),
  y la UI "Probar con captura de pantalla" → WP posterior (solo queda preparado el
  punto de extensión: otro `CaptureInput` en el merge).
- Eliminar `RidePlatform.fromPackageName` del filtro de entrada ni del ciclo de
  sesión del coordinador (siguen siendo útiles y baratos).
- Cambios en `OverlayService`, `PipelineOverlayDataSource`, `DebugPanelViewModel` o
  `SircApplication` (contratos intactos).
- Telemetría/logging nuevos desde `:core:platform`.

## Riesgos y mitigaciones

- **Orden del OCR:** se preserva (el OCR corre en el pipeline y solo si hay imagen;
  los textos de accesibilidad son el fallback exacto de hoy).
- **Dedup:** mismo hash de `imageData`, ahora sobre `CaptureRequest`; request sin
  imagen nunca se deduplica (igual que hoy).
- **MediaProjection disparado por accesibilidad:** el servicio sigue siendo el
  disparador; `MediaProjectionCaptureInput` solo añade la imagen si proyecta.
- **Coordinador debug:** se conserva `CaptureState` y todos los métodos públicos
  (`start/stop/reset/state`) → `DebugPanelViewModel` y `SircApplication` sin cambios.
- **`CaptureWindowEvent`:** NO se elimina; sigue en `WindowObserver`/coordinador/
  debug. Solo deja de ser input del parser.
- **Doble guardado:** se elimina (el coordinador deja de escribir); verificar en
  tests que `repository.snapshots()` recibe una sola entrada por oferta.
- **DI:** cambios acotados a `CaptureModule`, `CaptureAndroidModule` y
  `PlatformModule`; verificación con `assembleDebug`.

# PERFORMANCE_AUDIT.md — Auditoría de Rendimiento

**Rol:** Performance Engineer
**Objetivo:** Auditar CPU, RAM, batería, OCR, bitmap, parser, Compose, GC, leaks, cache, scheduler y coroutines. Sin optimizar.
**Proyecto:** SIRC (captura de pantalla + OCR + overlay en tiempo real)
**Fecha:** 2026-08-01 · **Commit auditado:** `e3460a5`

---

## Resumen ejecutivo

La arquitectura del pipeline es correcta a alto nivel (debounce 400 ms, channel CONFLATED, buffers acotados, bitmaps reciclados, DAOs suspend). El costo dominante es **el OCR a pantalla completa ejecutado sin gate de detección previo**, junto con **dos FGS y dos servicios de accesibilidad siempre activos** que duplican trabajo en el hilo principal. Hay varios puntos de ineficiencia estructural, ninguno es crash por rendimiento, pero la batería y CPU en uso continuo (toda la jornada del conductor) son el riesgo real.

---

## 1. OCR (`MlKitOcrEngine`)

### P-P01 — OCR a pantalla completa sin gate de detección previo
- **Resumen:** El pipeline captura el frame completo (resolución nativa, 1080×2400 típica) y ejecuta ML Kit OCR **antes** de comprobar si la pantalla es una oferta. La detección de plataforma (`RidePlatform.fromPackageName`) ocurre **después** del OCR (`DefaultCapturePipeline.kt:116`) y la clasificación de pantalla (`OfferDetectionEngine`) solo dentro del parser (`:145`). Si el conductor está en HOME/TRIP/NAVIGATION/ERROR, se ejecuta un OCR completo inútil.
- **Impacto:** CPU y batería dominantes. Con debounce de 400 ms (`DebounceCaptureScheduler.kt:38`) el peor caso son ~2.5 OCR/s a pantalla completa en cualquier cambio de texto de la app de la plataforma. Horas de conducción → sobrecalentamiento y drenaje de batería.
- **Severidad:** ALTA
- **Evidencia:** `DefaultCapturePipeline.kt:107` (`resolveTexts`) → `:116` (check plataforma) → `:145` (parser). `MlKitOcrEngine.kt:28` decodifica el PNG completo.
- **Prioridad:** P1

### P-P02 — Sin downscaling ni crop al ROI de la tarjeta de oferta
- **Resumen:** ML Kit recibe la imagen completa sin reducir (`MediaProjectionScreenCaptureProvider.kt:146-155` crea el VirtualDisplay a resolución completa; `MlKitOcrEngine.kt:28-29` la decodifica íntegra). No hay crop a la zona de la tarjeta de oferta ni `setTargetRotation` ni redimensionado.
- **Impacto:** Latencia OCR de cientos de ms por frame y más memoria; el pipeline entero queda limitado por esta etapa.
- **Severidad:** ALTA
- **Evidencia:** `MlKitOcrEngine.kt:27-29`; `MediaProjectionScreenCaptureProvider.kt:149-155`.
- **Prioridad:** P1

### P-P03 — Formato intermedio PNG (doble encode/decode)
- **Resumen:** Cada frame: `Image`→`Bitmap` (`toBitmap`), `Bitmap`→**PNG calidad 100** (`MediaProjectionScreenCapture.kt:47-51`), luego el OCR vuelve a decodificar el PNG a `Bitmap` (`MlKitOcrEngine.kt:28`). PNG lossless de pantalla completa son ~10 MB por frame y el encode es caro.
- **Impacto:** CPU/memoria adicional por frame (encode + decode); el OCR nunca ve el RGBA directo.
- **Severidad:** MEDIA
- **Evidencia:** `MediaProjectionScreenCapture.kt:47-51`; `MlKitOcrEngine.kt:28`.
- **Prioridad:** P2

### P-P04 — Frames cerrados por `acquireLatestImage()` bajo carga (degradación silenciosa)
- **Resumen:** `acquireLatestImage()` cierra (según doc de Android) las imágenes adquiridas previamente aún no cerradas — incluidas las que están esperando en el `Channel(CONFLATED)`. Si el OCR es más lento que la captura, el consumidor puede recibir una `Image` ya cerrada y `Image.toBitmap()` lanza `IllegalStateException` → se captura y devuelve `null` (`captureFrame` retorna null) → el pipeline degrada silenciosamente a textos de accesibilidad sin señalarlo como error de captura.
- **Impacto:** La vía OCR falla en ráfagas bajo carga sin diagnóstico; el overlay puede operar con datos de accesibilidad (menos precisos) sin aviso.
- **Severidad:** MEDIA (robustez + pérdida de funcionalidad de OCR)
- **Evidencia:** `MediaProjectionScreenCaptureProvider.kt:156-162` (listener) + `:49` (canal CONFLATED) + `:128-143` (consume/close).
- **Prioridad:** P2

---

## 2. Captura MediaProjection

### P-P05 — VirtualDisplay continuo a resolución completa sin límite de frame rate
- **Resumen:** El VirtualDisplay espeja la pantalla y el `ImageReader` produce frames a la tasa de refresco del display (60–120 Hz) aunque el OCR consuma 1 de cada N. `MAX_IMAGES=2` + CONFLATED acotan la memoria, **no el costo de render/composición del sistema**.
- **Impacto:** Batería: la GPU/display pipeline trabaja continuamente durante toda la jornada de conducción.
- **Severidad:** MEDIA-ALTA
- **Evidencia:** `MediaProjectionScreenCaptureProvider.kt:145-177`; `MAX_IMAGES = 2` (`:209`).
- **Prioridad:** P1/P2

### P-P06 — Pipeline captura→OCR serializado con timeout de captura 400 ms
- **Resumen:** El `collect` en `CaptureAccessibilityService` es secuencial: cada request bloquea hasta 400 ms esperando un frame (`CAPTURE_TIMEOUT_MS`). Si el OCR es más lento, la latencia total por oferta acumula captura+OCR.
- **Impacto:** Latencia de respuesta del overlay (el conductor decide en segundos). Requests excedentes se dropean (DROP_OLDEST).
- **Severidad:** MEDIA
- **Evidencia:** `CaptureAccessibilityService.kt:38-42`; `MediaProjectionScreenCaptureProvider.kt:132` (`withTimeoutOrNull(400)`).
- **Prioridad:** P2

---

## 3. Scheduler / pipeline

### P-P07 — Dos scopes singleton nunca cancelados (falta de structured concurrency)
- **Resumen:** `PipelineOverlayDataSource` y `OfferCaptureCoordinator` crean `CoroutineScope(SupervisorJob() + Dispatchers.Default)` de proceso que nunca se cancela. Trabajan mientras la app viva (FGS activo).
- **Impacto:** La evaluación + persistencia Room + observación de config siguen corriendo en background; no hay punto de control global. Sin leak de Context (usa `@ApplicationContext`), pero sin shutdown limpio.
- **Severidad:** MEDIA
- **Evidencia:** `PipelineOverlayDataSource.kt:75`; `OfferCaptureCoordinator.kt:46`; `SircApplication.kt:14` (arranque del coordinator).
- **Prioridad:** P2

### P-P08 — Evaluación duplicada de la misma oferta (coordinador legacy + pipeline OCR)
- **Resumen:** `SircAccessibilityService` alimenta `OfferCaptureCoordinator` (parseo sin OCR) y `CaptureAccessibilityService` alimenta `DefaultCapturePipeline` (con OCR). La misma oferta se procesa dos veces en paralelo sin dedup cruzado.
- **Impacto:** Doble CPU, doble parseo y posible doble persistencia por oferta.
- **Severidad:** MEDIA
- **Evidencia:** `OfferCaptureCoordinator.kt:49-57`; `DefaultCapturePipeline.kt:83-186`.
- **Prioridad:** P2

### P-P09 — `snapshotInFlight` no sincronizado (race benigno)
- **Resumen:** `snapshotInFlight` es un `Boolean` plano leído/escrito desde corrutinas del mismo scope multi-hilo de `Dispatchers.Default`.
- **Impacto:** Evaluación doble ocasional de un snapshot; bajo impacto real pero es una race no controlada.
- **Severidad:** BAJA
- **Evidencia:** `PipelineOverlayDataSource.kt:77,120-121`.
- **Prioridad:** P3

---

## 4. Parser de texto (core:platform)

### P-P10 — Triple normalización del mismo texto
- **Resumen:** El texto se normaliza (lowercase + strip acentos, alocando un StringBuilder+String por texto) en `OfferDetectionEngine.detect` (`:25`), luego en `OfferParserOrchestrator.kt:40` (`texts.map(OfferDetectionEngine::normalize)`), y de nuevo en `BaseOfferTypeParser.matches` (`SpecializedParsers.kt:22`) por cada uno de los 5 parsers especializados.
- **Impacto:** CPU innecesaria y allocs por request. Menor frente al OCR, pero trivial de evitar.
- **Severidad:** BAJA-MEDIA
- **Evidencia:** `OfferParserOrchestrator.kt:40`; `SpecializedParsers.kt:22`; `OfferDetectionEngine.kt:25`.
- **Prioridad:** P3

### P-P11 — Detección de pantalla ejecutada 2 veces por request (O(r×k×t))
- **Resumen:** `OfferDetectionEngine` recorre 6 reglas × ~100 keywords × hasta 80 textos con `contains()`, y los parsers especializados vuelven a hacer `matches()` equivalentes.
- **Impacto:** Costo cuadrático en textos; sigue siendo menor que el OCR.
- **Severidad:** BAJA
- **Evidencia:** `OfferDetectionEngine.kt:27-36`; `SpecializedParsers.kt:22`.
- **Prioridad:** P3

---

## 5. Coroutines

- Sin `GlobalScope` (verificado). Uso correcto de `StateFlow`/`SharedFlow`.
- `Dispatchers.IO` no se usa en ningún lugar; Room usa sus propios executors suspend (sin bloqueo de Main).
- `withContext(Dispatchers.Default)` en `captureFrame` (`MediaProjectionScreenCaptureProvider.kt:130`) es un hop redundante (ya se está en Default) — sin impacto real.
- Los scopes de servicios (`CaptureAccessibilityService.kt:33`, `OverlayService.kt:54`) se cancelan en `onDestroy` — correcto.
- **Hallazgo:** los scopes singleton (P-P07) son el único patrón débil.

---

## 6. Compose

### P-P12 — `DebugPanelViewModel` coleccionado en la raíz (trabajo continuo oculto)
- **Resumen:** `SircApp.kt:53-54` instancia y colecciona `DebugPanelViewModel` en la raíz de la app (no solo en el tab Debug). `DebugPanelViewModel.build()` (`DebugPanelViewModel.kt:267-324`) aloca un `UiState` de ~30 campos **en cada cambio del pipeline** (WAITING→CAPTURING→PROCESSING→ERROR), esté o no abierta la pantalla.
- **Impacto:** Recomposición y trabajo de fondo permanente aunque el usuario nunca abra Debug; consumo de CPU/GC en toda la sesión.
- **Severidad:** MEDIA
- **Evidencia:** `SircApp.kt:53-54`; `DebugPanelViewModel.kt:113-164` (`combine` + `stateIn`) y `:267-324` (`build()`).
- **Prioridad:** P2

### P-P13 — Cero `derivedStateOf` y estado dependiente no memoizado
- **Resumen:** No se usa `derivedStateOf` en ningún composable; el estado dependiente (p. ej. visibilidad desde `status`+`evaluation`) se recalcula en cada cambio de estado de bajo nivel.
- **Impacto:** Recomposiciones más frecuentes que lo necesario; el overlay es pequeño (impacto acotado).
- **Severidad:** BAJA
- **Evidencia:** grep de `derivedStateOf` sin resultados; `OverlayContent.kt` maneja `animateFloatAsState`/`AnimatedContent` (recomposiciones acotadas).
- **Prioridad:** P3

### Compose — puntos correctos
- `LazyColumn` con `key = { it.id }` (`HistoryScreen.kt:91-102`) — correcto.
- `StateFlow` inmutables en ViewModels con `stateIn` — correcto.
- `collectAsStateWithLifecycle` usado en `SircApp` (menos en el overlay, que usa `collectAsState` dentro del Service, aceptable).

---

## 7. Memoria / GC / leaks

- **Sin leaks de Context:** todos los singletons usan `@ApplicationContext` (verificado en `MediaProjectionScreenCaptureProvider.kt:41`, `OverlayController.kt:20`, `PermissionManager.kt:46`, `AndroidSircLogger.kt:24`).
- **Bitmaps reciclados correctamente:** captura (`MediaProjectionScreenCapture.kt:30`), OCR (`MlKitOcrEngine.kt:33,37,42`), `Image.close()` en `finally` (`MediaProjectionScreenCaptureProvider.kt:140`).
- **Caches acotadas (bien):** frame cache 32, capture repository 50, evaluación 100, validation 500, performance 100, eventos recientes 20.
- **Room:** DAOs suspend, sin consultas en Main. `trimToLimit` O(n) por insert con límite 500 (`DefaultOfferHistoryRepository.kt:23-26`), aceptable.

### P-P14 — Posible corrupción de bitmap al reciclar `padded` compartido
- **Resumen:** `toBitmap()` crea `padded`, luego `cropped = Bitmap.createBitmap(padded, ...)` (que puede compartir el buffer de píxeles) y recicla `padded` (`:229-231`). Si el bitmap devuelto comparte el buffer con el reciclado, sus píxeles quedan inválidos.
- **Impacto:** Frame corrupto bajo carga (píxeles basura) → OCR con resultados erróneos o fallo silencioso. Depende de la implementación interna de `createBitmap`.
- **Severidad:** MEDIA (correctness bajo carga)
- **Evidencia:** `MediaProjectionScreenCaptureProvider.kt:226-231`.
- **Prioridad:** P2

### P-P15 — `contentHashCode()` sobre el PNG completo por request
- **Resumen:** `InMemoryCaptureFrameCache.isNew` calcula el hash del PNG completo (varios MB) en cada request para deduplicar.
- **Impacto:** Costo de hash de MB por request; la dedup ya es limitada (si la pantalla se anima, el hash cambia y no deduplica).
- **Severidad:** BAJA
- **Evidencia:** `InMemoryCaptureFrameCache.kt:35-36`.
- **Prioridad:** P3

### P-P16 — Sin dedup cuando no hay imagen
- **Resumen:** `isNew` devuelve `true` si el frame no tiene imagen (`:20-22`). Con proyección apagada, cada evento debounced re-parsea y re-persiste.
- **Impacto:** Trabajo repetido sin proyección (vía accesibilidad solamente).
- **Severidad:** BAJA
- **Evidencia:** `InMemoryCaptureFrameCache.kt:20-22`.
- **Prioridad:** P3

---

## 8. Batería

### P-P17 — Dos FGS + dos servicios de accesibilidad activos de forma permanente
- **Resumen:** Mientras la función está encendida corren 2 FGS (`OverlayService` specialUse + `MediaProjectionService` mediaProjection) y 2 AccessibilityServices con la misma config (`notificationTimeout=100`). Cada evento de ventana recorre el árbol de accesibilidad **dos veces en el hilo principal** (MAX 400 nodos c/u) — `CaptureAccessibilityService.kt:76-98` y `SircAccessibilityService.kt:66-88`.
- **Impacto:** Jank + doble CPU de accesibilidad + 2 notificaciones permanentes. Sumado a P-P01/P-P05, la batería es el costo principal de usar la app toda la jornada.
- **Severidad:** MEDIA-ALTA
- **Evidencia:** `feature\overlay\src\main\AndroidManifest.xml:11-35` (2 servicios) ; `accessibility_service_config.xml:10` (`notificationTimeout="100"`).
- **Prioridad:** P1/P2

### Batería — puntos correctos
- Sin wakelock / `FLAG_KEEP_SCREEN_ON` / `keepScreenOn` (verificado).
- Sin tráfico de red (todo local).
- La app pide exención de optimización de batería vía UI (`PermissionManager.kt:65-68`, `HomeScreen.kt:126-143`).

---

## 9. Puntos fuertes

- Reciclaje sistemático de bitmaps e `Image.close()` en `finally`.
- Todos los buffers de memoria están acotados (32/50/100/500/100/20).
- DAOs suspend → sin bloqueo de Main en queries Room.
- Sin `GlobalScope`, sin wakelocks, sin red.
- Keys de `LazyColumn` correctas; `StateFlow` inmutables.
- R8 + shrink activo en release (`app\build.gradle.kts:22-31`).
- Debounce de 400 ms bien implementado con `MutableSharedFlow(DROP_OLDEST)`.

---

## 10. Matriz de prioridad

| Ref | Severidad | Prioridad |
|---|---|---|
| P-P01 OCR sin gate previo | ALTA | P1 |
| P-P02 Sin downscaling/crop | ALTA | P1 |
| P-P05 VirtualDisplay sin límite de fps | MEDIA-ALTA | P1/P2 |
| P-P17 Doble a11y + 2 FGS | MEDIA-ALTA | P1/P2 |
| P-P03 PNG intermedio | MEDIA | P2 |
| P-P04 Frames cerrados (degradación) | MEDIA | P2 |
| P-P06 Pipeline serializado 400 ms | MEDIA | P2 |
| P-P07 Scopes singleton | MEDIA | P2 |
| P-P08 Doble procesamiento de oferta | MEDIA | P2 |
| P-P12 DebugPanelViewModel en raíz | MEDIA | P2 |
| P-P14 Bitmap `padded` reciclado | MEDIA | P2 |
| P-P09 Race `snapshotInFlight` | BAJA | P3 |
| P-P10/P-P11 Normalización ×3 | BAJA-MEDIA | P3 |
| P-P13 Sin derivedStateOf | BAJA | P3 |
| P-P15/P-P16 Dedup limitado | BAJA | P3 |

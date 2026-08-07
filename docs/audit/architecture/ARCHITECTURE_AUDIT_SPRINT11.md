# ARCHITECTURE_AUDIT_SPRINT11

> Auditoría arquitectónica completa del repositorio tras WP-E3-01 → WP-E3-03
> (motor descriptor-driven, framework de detección, Unified Capture Source).
> **Documento de solo lectura: no se ha modificado código.**
> El plan de corrección se definirá por separado (WP-E3-05 y siguientes) solo
> después de que esta auditoría sea aprobada.

## 1. Alcance y metodología

- **Alcance**: 8 módulos — `app`, `core/capture`, `core/capture/android`,
  `core/platform`, `core/ui`, `data`, `domain`, `feature/overlay`,
  `feature/history`, `feature/settings`, `feature/onboarding`.
- **Metodología**: lectura de todos los archivos Kotlin (main + test),
  grep repo-wide de cada identificador sospechoso, verificación manual de los
  hallazgos de mayor señal, revisión de `build.gradle.kts` y `AndroidManifest.xml`
  de cada módulo, y del grafo de dependencias entre módulos.
- **Criterio**: se distingue "sin uso en producción", "solo uso en tests" y
  "write-only" (se escribe pero nunca se lee). Cada afirmación fue verificada
  con búsqueda global.
- **Reglas respetadas**: sin cambios funcionales, sin optimizaciones prematuras,
  sin refactors por gusto.

## 2. Conclusión ejecutiva

El refactor WP-E3-01 → WP-E3-03 es **estructuralmente sólido**:

- **0 hallazgos críticos** (no hay defectos de runtime, bindings Hilt rotos,
  dependencias circulares ni violaciones de Clean Architecture).
- **Caminos únicos confirmados en producción**:
  - *Captura*: una sola cadena `AccessibilityCaptureInput + MediaProjectionCaptureInput`
    → merge `@CaptureRequests` → `DefaultCapturePipeline`.
  - *Detección*: una sola en el pipeline (`PlatformDetectionEngine` inyectado).
  - *Parseo*: una sola (`PlatformOfferParser` → `OfferParserOrchestrator.parse(result, …)`).
  - *Persistencia*: una sola (pipeline guarda `OfferSnapshot`; `PipelineOverlayDataSource`
    persiste evaluación/historial). El coordinador ya no guarda.
- **Matices** (no en la ruta de producción, pero sí código vivo o público):
  el coordinador aún resuelve plataforma con `RidePlatform.fromPackageName`
  (solo sesiones/contadores de debug) y el orquestador conserva dos overloads
  de parseo solo usados por tests que ejecutan **detección paralela**.

La deuda restante es **limpieza**: código muerto, API pública sin consumidores,
documentación desactualizada/contradictoria y dependencias no usadas.

## 3. Verificación de caminos únicos

| Flujo | ¿Único? | Detalle |
|---|---|---|
| Captura | ✅ | Único merge `@CaptureRequests` (capture/capture:android → overlay). |
| Detección | ⚠️ | En producción único (`DefaultCapturePipeline`). Pero `OfferParserOrchestrator` tiene 2 overloads test-only que ejecutan detección paralela y el coordinador usa `RidePlatform.fromPackageName`. |
| Parseo | ✅ | Único: `PlatformOfferParser.parse(request, result, detectionMillis)` → `orchestrator.parse(result, …)`. |
| Persistencia | ✅ | Único guardado de snapshot en el pipeline; `PipelineOverlayDataSource` persiste evaluación/historial. Coordinador no guarda. |
| Rutas paralelas heredadas | ⚠️ | Sin rutas en producción; quedan API pública sin uso y resolución de plataforma duplicada (ver A-1, A-3). |

## 4. Hallazgos por severidad

Formato por hallazgo: **Problema · Impacto · Propuesta · Riesgo de modificarlo**.

### 4.1 Crítico

**Ninguno.** No se detectó ningún hallazgo que pueda romper el runtime, el
overlay, la captura o la persistencia.

### 4.2 Alto

---

**A-1 — Overloads de parseo con detección paralela (solo tests, pero API pública)**
- **Problema**: `OfferParserOrchestrator` expone 3 overloads:
  `parse(texts, ts, RidePlatform)` (`OfferParserOrchestrator.kt:32`) y
  `parse(texts, ts, packageName)` (`:65`). El primero resuelve el descriptor y
  ejecuta `OfferDetectionEngine` directamente vía `registry.detectionEngineFor`
  (`:38-42`) **bypaseando** `PlatformDetectionEngine`; el segundo crea una
  **segunda instancia** de `PlatformDetectionEngine` interna (`:22`, usada solo
  ahí). Solo el overload `parse(result, texts, ts, detectionMillis)` (`:96`)
  tiene consumidor en producción (`PlatformOfferParser.kt:34`).
- **Impacto**: riesgo de divergencia futura entre dos caminos de detección;
  `detectionMillis = 0.0` hardcodeado en el overload por packageName (`:82`) a
  pesar de ejecutar detección (timing inconsistente). Confusión para futuros
  consumidores de la API.
- **Propuesta**: tras aprobar, eliminar o marcar `@VisibleForTesting` los dos
  overloads legacy y unificar en `parse(result, …)`; retirar la instancia
  interna `detectionEngine` del orquestador.
- **Riesgo**: bajo (solo afecta tests; los tests se migrarían al overload nuevo).

---

**A-2 — Documentación interna contradice el código: `PlatformDescriptors` no fue eliminado**
- **Problema**: `.ai/CONTEXT.md:112-113,198-199`, `.ai/DECISIONS.md:384-385,399`
  y `docs/CHANGELOG.md:72` afirman que el objeto `PlatformDescriptors` fue
  eliminado en WP-E3-01. En realidad el archivo existe y es el **seed de
  producción** del registry: `PlatformModule.kt:33` llama
  `PlatformDescriptorRegistry(PlatformDescriptors.all())`, y lo usan 15 sitios
  de test.
- **Impacto**: desinformación que puede inducir a agentes/futuros desarrolladores
  a borrar código vivo o a creer que hay una fuente de descriptores distinta.
- **Propuesta**: corregir `.ai/CONTEXT.md`, `.ai/DECISIONS.md` y `docs/CHANGELOG.md`
  para reflejar que `PlatformDescriptors.all()` es la fuente de descriptores.
- **Riesgo**: nulo (solo docs).

---

**A-3 — Resolución de plataforma paralela: `RidePlatform.fromPackageName` aún en producción**
- **Problema**: `RidePlatform.fromPackageName()` (`domain/.../RidePlatform.kt:20`,
  coincidencia exacta) se usa en producción en `OfferCaptureCoordinator.kt:84` y
  `AccessibilityCaptureInput.kt:46`, mientras el pipeline resuelve la plataforma
  de forma descriptor-driven con normalización (`DetectionMatcher.matchesPackage`).
- **Impacto**: dos mapeos package→plataforma que deben mantenerse sincronizados a
  mano; riesgo de que el coordinador abra/cierre sesiones con una plataforma
  distinta a la que detecta el pipeline en casos límite.
- **Propuesta**: migrar el coordinador y `AccessibilityCaptureInput` a
  descriptor-driven (usar `PlatformDescriptorRegistry`/`PlatformDetectionEngine`
  o un helper derivado) y deprecar `RidePlatform.fromPackageName`.
- **Riesgo**: medio (toca lógica de sesión de debug y el filtro de accesibilidad;
  requiere cobertura de tests antes de migrar).

---

### 4.3 Medio

---

**M-1 — API muerta en `:domain` (use cases y repos)**
- **Problema**: sin consumidores en todo el repo:
  `EvaluateOfferUseCase.kt:12`, `AddOfferHistoryUseCase.kt:20`,
  `SaveDriverConfigUseCase.saveDriverCosts/saveDecisionThresholds` (`:14,16`),
  `GetDriverConfigUseCase.observeDriverCosts/observeDecisionThresholds` (`:23,25`)
  y los métodos homónimos de `DriverConfigRepository.kt:21-31` (consumidos solo
  por el use case muerto; la ruta viva usa `DriverConfig` directamente).
- **Impacto**: superficie de API que sugiere capacidad inexistente; el contrato
  de `DriverConfigRepository` obliga a `DefaultDriverConfigRepository` a
  implementar 6 métodos sin uso real.
- **Propuesta**: tras aprobar, eliminar los use cases muertos y recortar la
  interfaz + implementación + fakes de test (`PipelineOverlayDataSourceTest.kt:259-269`).
- **Riesgo**: medio (cambia una interfaz pública de `:domain`; requiere migrar
  los fakes de test y Room impl de `data`).

---

**M-2 — Interfaz `PlatformExtractor` sin implementación referenciada por su tipo**
- **Problema**: `PlatformExtractor` (`PlatformExtractor.kt:13`) tiene una única
  implementación (`GenericPlatformExtractor`) que el registry almacena/retorna
  **como tipo concreto** (`PlatformDescriptorRegistry.kt:24,69`); solo KDoc la
  menciona como interfaz.
- **Impacto**: abstracción sin valor (un solo impl, nunca usada como interfaz).
- **Propuesta**: eliminar la interfaz o hacerla la firma real del registry.
- **Riesgo**: bajo.

---

**M-3 — `OfferTypeVariant.refine` nunca se setea**
- **Problema**: campo `refine` (`OfferTypeVariant.kt:18`) es invocado por
  `GenericOfferTypeParser` (`:28`) pero ningún descriptor ni constructor del repo
  le asigna valor (grep `refine =` sin resultados).
- **Impacto**: código de parseo que ramifica sobre un valor siempre nulo.
- **Propuesta**: eliminar el campo y la rama, o documentarlo como futuro.
- **Riesgo**: bajo.

---

**M-4 — Flags de depuración muertos expuestos como toggles**
- **Problema**: `FeatureFlag.ACCESSIBILITY` y `FeatureFlag.METRICS`
  (`FeatureFlag.kt:11,17`) no se leen en ningún `.kt` (grep global vacío); el
  panel de debug los lista/alterna vía `FeatureFlag.entries`
  (`DebugPanelViewModel.kt:257,277`) sin efecto real.
- **Impacto**: toggles que no hacen nada confunden al usuario de debug y sugieren
  funcionalidad inexistente.
- **Propuesta**: eliminar ambos valores del enum (el panel los muestra por
  iteración, así que desaparecen solos) o cablearlos a algo real.
- **Riesgo**: bajo.

---

**M-5 — `CaptureMetrics.onCapture` muerto (interfaz + override)**
- **Problema**: `CaptureMetrics.onCapture` (`CaptureMetrics.kt:10`) y su override
  en `DebugCaptureMetrics.kt:22-24` no tienen llamador en producción (el pipeline
  solo usa `onOcr/onParse/onTotal`). El único llamador es un test double
  (`DefaultCapturePipelineTest.kt:347`). La spec WP-E3-03 ya planeó retirarlo.
- **Impacto**: método fantasma en una interfaz inyectada; el binding Hilt
  `bindCaptureMetrics` sigue exponiendo un método sin uso.
- **Propuesta**: eliminar `onCapture` de la interfaz y del override.
- **Riesgo**: bajo (ajustar el test double).

---

**M-6 — Campo/datos write-only del framework de detección**
- **Problema**: se producen pero nunca se leen en producción:
  `DetectionResult.candidates` + `DetectionCandidate` (`DetectionResult.kt:16`),
  `DetectionResult.sourcePackage` (`:17`), `DetectionResult.origin` (`:13`),
  `ParsedOffer.parsingMillis` (`OfferTypeParser.kt:14`, escrito en
  `OfferParserOrchestrator.kt:134,145`), `ScreenDetection.matchedKeywords/confidence`
  (`ScreenDetection.kt:12-13`).
- **Impacto**: diagnóstico y telemetría de detección que nadie consume; peso
  conceptual en `:core:platform` (Kotlin puro).
- **Propuesta**: o bien retirar los campos no leídos, o bien consumirlos en el
  panel de debug/`ValidationRecorder` (decisión a tomar; **no** es obligatorio
  eliminar si se quiere diagnóstico futuro).
- **Riesgo**: medio si se elimina (campos públicos del framework, usados en
  tests); bajo si solo se documenta.

---

**M-7 — `CaptureInputType` con valores sin uso en producción**
- **Problema**: `SHARE`/`GALLERY`/`TEST` (`CaptureInputType.kt:34,19,22`) no se
  referencian en ningún `.kt`; `OCR` y `PACKAGE` solo en tests.
- **Impacto**: API de un futuro (Gallery/Share) declarada antes de existir el
  consumidor (diseño aditivo intencional de WP-E3-03).
- **Propuesta**: conservar como plan (documentar en `.ai/DECISIONS.md`) o retirar
  hasta que exista el `CaptureInput` correspondiente. Dejar constancia explícita
  de que son aditivos e intencionales.
- **Riesgo**: nulo si se conserva; bajo si se retira (rename aditivo).

---

**M-8 — Bundle LEGACY de reglas en `:domain` (solo tests)**
- **Problema**: `RuleEngine` + 6 reglas + `RuleContext` + `RuleMessages` +
  `OfferRule` (`domain/.../engine/rules/*`), `RuleThresholds` y
  `OfferValidator` + `ValidationResult` + `ValidationIssue` solo se usan desde
  sus propios tests. `RuleEvaluation.resultFor` (`RuleEvaluation.kt:28`) solo en
  test. `TripOffer.pickupDistanceKm` solo lo leen estos componentes muertos.
- **Impacto**: 8+ clases mantenidas solo por tests, con `@VisibleForTesting`
  implícito; deuda intencional registrada en WP-E1-02 ("para uso en tests futuros").
- **Propuesta**: decidir si se eliminan (junto a sus tests) o se consolidan en un
  único archivo LEGACY marcado. Requiere confirmación del equipo (fue decisión
  explícita del WP-E1-02).
- **Riesgo**: alto de decisión (estaban marcados LEGACY a propósito); bajo
  técnico si se elimina (nada de producción los usa).

---

**M-9 — Moneda duplicada en dos fuentes**
- **Problema**: `GenericPlatformExtractor.DEFAULT_CURRENCY`
  (`PlatformExtractors.kt:79-85`) duplica los valores `defaultCurrency` que ya
  trae cada descriptor (`PlatformDescriptors.kt:57,70,83,96`) y que el registry
  siempre pasa (`PlatformDescriptorRegistry.kt:48`). Solo se usa en tests que
  construyen el extractor directo.
- **Impacto**: dos lugares que pueden divergir si cambia la moneda de una
  plataforma.
- **Propuesta**: eliminar el map y hacer que el extractor exija `defaultCurrency`.
- **Riesgo**: bajo.

---

### 4.4 Bajo

---

**B-1 — Objetos/constantes sin uso**
- `NoOpCaptureMetrics` (`CaptureMetrics.kt:20`) — sin referencias.
- `OfferCaptureCoordinator.NANOS_PER_MILLI` (`OfferCaptureCoordinator.kt:151`) —
  constante privada sin usar.
- `MediaProjectionService.TAG` (`MediaProjectionService.kt:116`) — sin usar.
- `capture_service_label` (`core/capture/android/.../strings.xml:3`) — resource
  sin referencia.
- **Propuesta**: eliminar. **Riesgo**: nulo.

---

**B-2 — API pública usada solo por tests**
- `CaptureRepository.latestSnapshot()` (`:14`) y `snapshots()` (`:16`),
  `CaptureFrameCache.clear()` (`:19`), `OfferPerformanceTracker.clear()` (`:29`),
  `SnapshotSource.FAKE` (`OfferSnapshot.kt:28`), `OfferHistoryDao.count()`,
  `PlatformDescriptorRegistry.descriptorForPackageName()` (`:65`),
  `OfferDetectionEngine.keywordsFor()` (`:44-45`).
- **Propuesta**: conservar si tienen valor de test, o eliminar + ajustar tests.
- **Riesgo**: bajo.

---

**B-3 — `OverlayState.CAPTURING` nunca emitido**
- **Problema**: `OverlayState.kt:15` no lo produce el pipeline (solo
  DISABLED/WAITING/PROCESSING/ERROR); la UI lo mapea (`OverlayContent.kt:229`) y
  un test lo fuerza.
- **Propuesta**: eliminar el valor o documentar como futuro. **Riesgo**: bajo.

---

**B-4 — `DiscardReason.CAPTURE_FAILED` muerto con KDoc legacy**
- **Problema**: `ValidationEvent.kt:54`, sin referencias, y su KDoc describe la
  "captura de pantalla" eliminada.
- **Propuesta**: eliminar el valor y corregir el KDoc del enum. **Riesgo**: nulo.

---

**B-5 — `PipelineOverlayDataSource.start()` no-op con llamadores reales**
- **Problema**: `PipelineOverlayDataSource.kt:93` (`= Unit`) es invocado desde
  `OverlayService.kt:122` y `OverlayViewModel.kt:20`, pero la colección real
  ocurre en `init{}` (`:79-91`).
- **Impacto**: API engañosa (el ciclo de vida llama start/stop pero start no hace
  nada; stop sí limpia).
- **Propuesta**: eliminar `start()` de la interfaz o hacer que delegue en el
  arranque real y armonizar `init{}`.
- **Riesgo**: bajo (revisar que no haya reinicio de colección duplicado).

---

**B-6 — `detect(timestampMillis)` sin uso en el cuerpo**
- **Problema**: parámetro `timestampMillis` de `PlatformDetectionEngine.detect`
  (`PlatformDetectionEngine.kt:16`) declarado y documentado pero nunca leído; el
  pipeline le pasa un valor real que se descarta.
- **Propuesta**: eliminar el parámetro (y ajustar llamadores/tests) o usarlo.
- **Riesgo**: bajo (firma pública de `:core:platform`; ajustar tests).

---

**B-7 — Dependencias de Gradle sin uso**
- `core/capture/android`: `implementation(project(":domain"))` (`build.gradle.kts:29`)
  y `implementation(libs.kotlinx.coroutines.android)` (`:34`) — sin imports.
- `feature/overlay`: `implementation(project(":data"))` (`:34`),
  `implementation(libs.compose.material.icons)` (`:43`),
  `implementation(libs.compose.ui.tooling.preview)` (`:44`) y
  `debugImplementation(libs.compose.ui.tooling)` (`:45`) — sin uso (0 `Icons.*`,
  0 `@Preview`, 0 imports `com.sirc.data.*`).
- **Impacto**: infla el grafo de dependencias y el build.
- **Propuesta**: eliminar. **Riesgo**: nulo (verificado por imports).

---

**B-8 — Referencias legacy en KDoc / log tags**
- `CaptureAccessibilityService.kt:21` y `AccessibilityWindowObserver.kt:17`
  mencionan `SircAccessibilityService` ("fue eliminado").
- `MediaProjectionCaptureInput.kt:19` menciona `MediaProjectionScreenCapture`.
- `PlatformModule.kt:22-25` menciona `provideOfferDetectionEngine`/
  `provideOfferTypeParsers` (eliminados).
- `MediaProjectionScreenCaptureProvider.kt:272` usa `TAG = "ScreenCaptureProvider"`
  (nombre legacy).
- **Propuesta**: actualizar KDoc y tag. **Riesgo**: nulo.

---

**B-9 — 12 imágenes de test sin referencia**
- **Problema**: `core/capture/src/test/resources/test-images/` contiene 13 PNGs;
  solo `offer_uber_1.png` se referencia en `DefaultCapturePipelineTest.kt`.
- **Propuesta**: eliminar las no usadas o documentarlas. **Riesgo**: nulo.

---

**B-10 — (Confirmación, no hallazgo)** `FeatureFlag.OVERLAY/CAPTURE/PARSER/OCR/
DETAILED_LOGS/DEBUG_PANEL` sí se leen vía `isEnabled` en producción; solo
`ACCESSIBILITY` y `METRICS` son muertos (ver M-4).

---

### 4.5 Observación

**O-1** — `android.permission.FOREGROUND_SERVICE` declarado en dos manifests
(`feature/overlay` y `core/capture/android`). Inofensivo (merge) pero redundante.

**O-2** — `SircApplication.onCreate` (`SircApplication.kt:14`) arranca
`OfferCaptureCoordinator.start()` incondicionalmente; su scope corre toda la vida
del proceso aunque el consumidor sea el panel de debug.

**O-3** — `repository-tree.txt` (raíz, untracked) es un volcado stale de build que
lista clases eliminadas (`SircAccessibilityService`, `ScreenFrame`,
`MediaProjectionScreenCapture`). No es fuente, pero confunde greps.

**O-4** — `DetectionResolution.AMBIGUOUS` se produce (`PlatformDetectionEngine.kt:70`)
pero `isRecognized` lo colapsa con `NONE`; no hay rama de producción que lo
distinga. Decidir si se consume o se simplifica.

**O-5** — `CaptureInput.origin`, `CaptureRequest.origin`, `OfferSnapshot.origin` y
`DetectionResult.origin` son write-only en producción (se propagan pero no se
ramifica sobre ellos); solo tests los leen. Diseño intencional de WP-E3-03 para
telemetría/uso futuro.

**O-6** — Menciones históricas de `DetectionOrigin`, `SpecializedParsers`,
`ExtractorRegistry`, `UberRequestParser` solo en docs (`.ai`, CHANGELOG,
ROADMAP, WORK_PACKAGE_PLAN, PERFORMANCE_AUDIT, PRE_BETA_BACKLOG). Código limpio.

**O-7** — `OfferCaptureCoordinator` y `PipelineOverlayDataSource` ya no
duplican parsing/persistencia; el split es correcto (confirmado).

## 5. Resumen cuantitativo

| Severidad | Cantidad | Naturaleza |
|---|---|---|
| Crítico | 0 | — |
| Alto | 3 | Rutas paralelas de detección (A-1), docs falsas (A-2), `fromPackageName` paralelo (A-3) |
| Medio | 9 | API muerta domain (M-1), interfaz sin tipo (M-2), `refine` (M-3), flags muertos (M-4), `onCapture` (M-5), write-only framework (M-6), `CaptureInputType` sin uso (M-7), bundle LEGACY reglas (M-8), moneda duplicada (M-9) |
| Bajo | 10 | Objetos/consts sin uso (B-1), API solo-tests (B-2), `CAPTURING` (B-3), `CAPTURE_FAILED` (B-4), `start()` no-op (B-5), `timestampMillis` (B-6), deps Gradle (B-7), KDoc legacy (B-8), imágenes (B-9), flags vivos verificados (B-10) |
| Observación | 7 | O-1 … O-7 |
| **Total** | **29** | |

## 6. Confirmaciones positivas (no hay problema)

- **Hilt**: los 7 módulos Hilt del repo (`CaptureModule`, `CaptureFlowsModule`,
  `CaptureAndroidModule`, `PlatformModule`, `RepositoryModule`, `DatabaseModule`,
  `FeatureModule`/equivalentes de features) no tienen bindings duplicados,
  huérfanos ni faltantes. Todos los qualifiers (`@AccessibilityRequests`,
  `@CaptureRequests`) resuelven.
- **Clean Architecture**: `:domain` y `:core:platform` son Kotlin puro (sin
  imports de Android en main); `core:ui` solo depende de modelos de `domain`; las
  features no se importan entre sí; `app` es el único agregador.
- **Manifests**: sin servicios ni permisos muertos (`CaptureAccessibilityService`,
  `OverlayService`, `MediaProjectionService` están declarados y son consumidos).
- **Tests**: ningún test referencia API eliminada/renombrada.
- **Overlay**: el <3 s se mantiene (`PipelineOverlayDataSource.onSnapshot` con
  `snapshotInFlight` y `Dispatchers.Default`).

## 7. Siguientes pasos (NO ejecutados)

1. Aprobación de esta auditoría por el equipo.
2. Definición del plan de corrección (WP-E3-05 y siguientes) priorizando los
   hallazgos Alto → Medio → Bajo, con TDD donde aplique y verificación completa
   (`ktlintCheck`, `lintDebug`, `assembleDebug`, tests unitarios) en cada paso.
3. Cierre oficial del Sprint 11 (Architecture / Performance / Technical Debt /
   Sprint Review).

## 8. Verificación aplicada

- Lectura de todos los archivos Kotlin de los 8 módulos (main + test).
- Grep repo-wide de cada identificador sospechoso (ScreenCapture, ScreenFrame,
  MediaProjectionScreenCapture, DetectionOrigin, SircAccessibilityService,
  OfferEventBus, OfferEvaluator, RuleEngine, SpecializedParsers, etc.).
- Revisión manual de: `OfferParserOrchestrator`, `DefaultCapturePipeline`,
  `PlatformOfferParser`, `OfferCaptureCoordinator`, `PipelineOverlayDataSource`,
  `PlatformModule`, `CaptureModule`/`CaptureFlowsModule`, `CaptureAndroidModule`,
  `FeatureFlag`, `PlatformDescriptors`, `RidePlatform`.
- Revisión de `build.gradle.kts` y `AndroidManifest.xml` de cada módulo.

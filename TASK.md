# TASK — Estado en vivo

> Actualiza este archivo al iniciar y terminar cada tarea relevante. Es el
> estado que cualquier agente lee primero para saber qué sigue sin re-leer el
> proyecto.

## Tarea actual

**WP-E3-05D completado** — Resuelven los hallazgos de severidad Baja
B-1…B-10 de la auditoría Sprint 11 (solo código confirmado muerto; se conservó
todo lo que tiene uso real o de test):

- **B-1**: eliminados 4 objetos/consts muertos: `NoOpCaptureMetrics`,
  `OfferCaptureCoordinator.NANOS_PER_MILLI`, `MediaProjectionService.TAG`,
  string `capture_service_label`.
- **B-2 (decidido: conservar)**: API solo-tests de `latestSnapshot/snapshots`,
  `CaptureFrameCache.clear()`, `OfferPerformanceTracker.clear()`,
  `SnapshotSource.FAKE`, `OfferHistoryDao.count()`, `descriptorForPackageName`,
  `keywordsFor` — tienen consumidores de test, la auditoría permite conservarlas.
- **B-3**: eliminado `OverlayState.CAPTURING` (el pipeline solo emite
  DISABLED/WAITING/PROCESSING/ERROR); quitada su rama en `OverlayContent` y
  ajustado `PipelineOverlayDataSourceTest`.
- **B-4**: eliminado `DiscardReason.CAPTURE_FAILED`.
- **B-5 (decidido: conservar)**: `PipelineOverlayDataSource.start()` no-op —
  tiene llamadores funcionales (`OverlayService`/`OverlayViewModel`).
- **B-6**: eliminado el parámetro sin uso `timestampMillis` de
  `PlatformDetectionEngine.detect(texts, packageName, origin)`; actualizados
  `DefaultCapturePipeline`, `OfferCaptureCoordinator`, `AccessibilityCaptureInput`
  y los tests (`PlatformDetectionEngineTest`, `OfferParserOrchestratorTest`).
- **B-7**: quitadas dependencias Gradle sin imports: en
  `core:capture:android` `:domain` y `kotlinx.coroutines.android`; en
  `feature:overlay` `:data`, `compose.material.icons`,
  `compose.ui.tooling.preview` y `debugImplementation(compose.ui.tooling)`.
- **B-8**: corregidos KDoc/log tags legacy (`CaptureAccessibilityService`,
  `AccessibilityWindowObserver`, `MediaProjectionCaptureInput`, `PlatformModule`,
  `PipelineOverlayDataSource`, `RuleResult` — pues el `[OfferRule]` referido se
  eliminó en 05C) y `TAG = "ScreenCaptureProvider"` →
  `"MediaProjectionCapture"`.
- **B-9 (decidido: conservar)**: las 13 PNG de `core:capture` test-resources son
  dataset documentado; solo `offer_uber_1.png` se referencia en código.
- **B-10**: verificación de confirmación de flags vivos — sin hallazgo.

**Siguiente: (pausa)** se entrega informe final para aprobación. No se inicia
WP-E3-05E sin aprobación explícita.

## Antecedentes

- **WP-E3-03 completado** (commit `f1675fb`): Unified Capture Source. Pipeline
  único `CaptureInput → CaptureRequest → (OCR) → PlatformDetectionEngine →
  OfferParserOrchestrator → OfferSnapshot → Repository → Overlay`. Se eliminaron
  `ScreenCapture`/`ScreenFrame`/`MediaProjectionScreenCapture` y la resolución
  de plataforma duplicada del coordinador; `DetectionOrigin` → `CaptureInputType`.
- **WP-E3-02 completado** (`ced6249`): framework genérico de detección
  (`PlatformDetectionEngine`/`DetectionMatcher`/`DetectionResult`).
- **WP-E3-01 completado** (`a79c55a`): motor descriptor-driven
  (`PlatformDescriptor`/`PlatformDescriptorRegistry`).

## Progreso WP-E3-04 (auditoría)

- [x] Documento `docs/audit/architecture/ARCHITECTURE_AUDIT_SPRINT11.md`
      redactado, verificado y **aprobado** por el usuario (29 hallazgos).
- [x] Commit `93a1b57` (auditoría) + `f185d85` (TASK).

## Progreso WP-E3-05A (severidad Alta)

- [x] **A-1**: eliminados los overloads `parse(texts, ts, RidePlatform)` y
      `parse(texts, ts, packageName)` y la instancia interna de
      `PlatformDetectionEngine` en `OfferParserOrchestrator`; único camino
      `parse(result, texts, ts, detectionMillis)`. `OfferParserOrchestratorTest`
      migrado (14 escenarios) a la API definitiva.
- [x] **A-3**: `OfferCaptureCoordinator` y `AccessibilityCaptureInput` inyectan
      `PlatformDetectionEngine` (única fuente de resolución); 
      `RidePlatform.fromPackageName` deprecado con `@Deprecated`;
      `OfferCaptureCoordinatorTest` actualizado.
- [x] **A-2**: corregidos `.ai/CONTEXT.md`, `.ai/DECISIONS.md` (D11.14) y
      `docs/CHANGELOG.md` — `PlatformDescriptors` se conserva como fuente de
      descriptores (solo se eliminaron `SpecializedParsers.kt` y
      `ExtractorRegistry`).
- [x] Verificación completa en verde (ktlintCheck, lintDebug, assembleDebug,
      tests unitarios JVM + instrumentados).
- [x] Docs del WP actualizadas (CHANGELOG WP-E3-05A, DECISIONS D11.14, TASK).

## Progreso WP-E3-05B (severidad Media)

- [x] **M-1**: eliminados `EvaluateOfferUseCase.kt` y `AddOfferHistoryUseCase`;
      recortados `SaveDriverConfigUseCase` (solo `save(config)`) y
      `GetDriverConfigUseCase` (solo `observeDriverConfig`/`observeIsConfigured`);
      `DriverConfigRepository` pasa de 10 a 4 métodos, con su impl
      `DefaultDriverConfigRepository` y el fake de `PipelineOverlayDataSourceTest`
      alineados.
- [x] **M-2**: eliminada la interfaz `PlatformExtractor` (YAGNI);
      `GenericPlatformExtractor` es la clase concreta única; KDoc de
      `OfferTextParser` corregido.
- [x] **M-3**: eliminados `OfferTypeVariant.refine` y la rama muerta en
      `GenericOfferTypeParser`.
- [x] **M-4**: eliminados `FeatureFlag.ACCESSIBILITY` y `FeatureFlag.METRICS`
      (el Debug Panel los listaba por iteración → desaparecen solos).
- [x] **M-5**: eliminado `CaptureMetrics.onCapture` (interfaz +
      `DebugCaptureMetrics` + test double `RecordingCaptureMetrics`).
- [x] **M-6 (parcial, decisión usuario)**: eliminados `ParsedOffer.parsingMillis`
      (más el timing muerto del orquestador) y `ScreenDetection.matchedKeywords`;
      **conservados** `DetectionResult.origin/candidates/sourcePackage`
      (diagnóstico para futuras fuentes de captura y Debug Panel).
- [x] Verificación completa en verde (ktlintCheck, lintDebug, assembleDebug,
      tests).
- [ ] Docs descriptivas (CHANGELOG/DECISIONS/CONTEXT) → diferidas a un WP
      posterior (regla: no tocar documentación en 05B).

## Hallazgos clave (resumen)

- **Alto (3)**: A-1 overloads de parseo test-only con detección paralela en
  `OfferParserOrchestrator` (2ª instancia de `PlatformDetectionEngine`);
  A-2 docs internas contradicen el código (`PlatformDescriptors` NO fue
  eliminado, es el seed de producción); A-3 `RidePlatform.fromPackageName`
  paralelo a la detección descriptor-driven.
- **Medio (9)**: API muerta en `:domain` (use cases + métodos de
  `DriverConfigRepository`), `PlatformExtractor` sin tipo, `refine` nunca
  seteado, `FeatureFlag.ACCESSIBILITY/METRICS` muertos (toggles sin efecto),
  `CaptureMetrics.onCapture` muerto, write-only del framework de detección,
  `CaptureInputType` SHARE/GALLERY/TEST sin uso (intencional aditivo), bundle
  LEGACY de reglas en `:domain`, moneda duplicada.
- **Bajo (10)**: objetos/consts sin uso, API solo-tests, `OverlayState.CAPTURING`,
  `DiscardReason.CAPTURE_FAILED`, `start()` no-op, `timestampMillis` sin uso,
  dependencias Gradle sin uso, KDoc legacy, imágenes de test sin usar.
- **Observación (7)**: O-1…O-7.
- **Crítico (0)**: sin defectos de runtime/Hilt/Clean Architecture.

## Verificación (línea base del Sprint 11)

- `.\gradlew.bat ktlintCheck --console=plain` → BUILD SUCCESSFUL.
- `.\gradlew.bat lintDebug assembleDebug testDebugUnitTest :domain:test :core:platform:test :core:capture:test :core:capture:android:testDebugUnitTest :feature:overlay:testDebugUnitTest --console=plain` → BUILD SUCCESSFUL.

## Próximos pasos

1. Esperar aprobación del documento de auditoría.
2. Tras la aprobación, escribir el plan de corrección (WP-E3-05 y siguientes)
   con TDD donde aplique y verificación completa en cada paso.
3. Cierre oficial del Sprint 11 (Architecture / Performance / Technical Debt /
   Sprint Review).

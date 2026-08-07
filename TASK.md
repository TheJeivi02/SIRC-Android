# TASK — Estado en vivo

> Actualiza este archivo al iniciar y terminar cada tarea relevante. Es el
> estado que cualquier agente lee primero para saber qué sigue sin re-leer el
> proyecto.

## Tarea actual

**WP-E3-05A completado** — Limpieza de severidad Alta (A-1, A-2, A-3) de la
auditoría del Sprint 11. `OfferParserOrchestrator` con un único camino de parseo
`parse(result, …)` (overloads legacy y engine interno eliminados); docs internas
corregidas (`PlatformDescriptors` se conserva); resolución de plataforma unificada
en `PlatformDetectionEngine` (coordinador + input), `RidePlatform.fromPackageName`
deprecado. Verificación completa en verde. **Siguiente: WP-E3-05B** (Medios:
API muerta, interfaces sin uso, callbacks sin consumidores, flags muertos).

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

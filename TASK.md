# TASK — Estado en vivo

> Actualiza este archivo al iniciar y terminar cada tarea relevante. Es el
> estado que cualquier agente lee primero para saber qué sigue sin re-leer el
> proyecto.

## Tarea actual

**WP-E3-02 completado** — Framework Genérico de Detección 100 % descriptor-driven
en `:core:platform`. `PlatformDetectionEngine` → `DetectionMatcher` →
`DetectionResult`; overload `parse(texts, ts, packageName)` en
`OfferParserOrchestrator`; vista de solo lectura de descriptores en el registry.
Sin cambios funcionales, sin plataformas nuevas, `:core:platform` sigue Kotlin puro.

## Objetivo

1. Consolidar el framework genérico de detección iniciado en WP-E3-01 sin tocar
   OCR, Overlay, Capture, ProfitEngine ni `PlatformDescriptor` público.
2. No implementar funcionalidades nuevas: exclusivamente el framework de detección.
3. Dejar todas las verificaciones en verde.

## Archivos involucrados

Creados (en `core/platform/src/main/kotlin/com/sirc/core/platform/`):
- `DetectionResolution.kt`, `DetectionOrigin.kt`, `DetectionCandidate.kt`,
  `DetectionResult.kt`, `DetectionMatcher.kt`, `PlatformDetectionEngine.kt`

Modificados:
- `core/platform/.../PlatformDescriptorRegistry.kt` (vista `val descriptors`)
- `core/platform/.../OfferParserOrchestrator.kt` (overload por `packageName` +
  `parseWith` compartido; método por `RidePlatform` intacto)
- `docs/CHANGELOG.md`, `.ai/CONTEXT.md`, `.ai/DECISIONS.md` (D11.12)

Tests: `DetectionResultTest.kt`, `DetectionMatcherTest.kt`,
`PlatformDetectionEngineTest.kt`, + 2 en `PlatformDescriptorRegistryTest.kt`,
+ 3 en `OfferParserOrchestratorTest.kt`.

## Progreso

- [x] Spec WP-E3-02 aprobado y commiteado (`ef74c8e`).
- [x] Plan de implementación (7 tareas TDD) en
      `docs/superpowers/plans/2026-08-06-wp-e3-02-detection-framework.md`.
- [x] Task 1: value types + tests (commit `cf9419e`).
- [x] Task 2: vista de solo lectura del registry (commit `34bc1a2`).
- [x] Task 3: `DetectionMatcher` puro + tests (commit `18c9e8a`).
- [x] Task 4: `PlatformDetectionEngine` + tests 5 etapas (commit `59520b3`).
- [x] Task 5: overload `parse(packageName)` + `parseWith` (commit `92580d0`).
- [x] Task 6: docs + verificación completa en verde (commit `ced6249`).
- [x] Task 7: `git status` limpio (solo untracked preexistentes ajenos al WP);
      `TASK.md` actualizado.

## Notas / decisiones

- Estrategia determinista por etapas: 1) `PACKAGE_MATCH` (packageName normalizado);
  2) candidatos por keywords (pantalla ≠ UNKNOWN); 3) único → `KEYWORD_CANDIDATE`;
  4) empate por mayor `matchScore` → `AMBIGUOUS` (sin elegir); 5) sin candidatos →
  `NONE`. Sin scoring heurístico.
- `DetectionResult` es autocontenido (`resolution`, `origin`, `descriptor?`,
  `screenDetection`, `candidates`, `sourcePackage`, `isRecognized`). El
  orquestador no re-recorre descriptores.
- `DetectionOrigin` (PACKAGE/OCR/GALLERY/TEST/UNKNOWN) se añade ya, sin cambiar
  comportamiento.
- `:core:platform` sigue Kotlin puro (sin logging/I/O/callbacks). El motor no
  conoce el origen de los textos.
- ktlint: `:core:platform` en verde; `ktlintCheck` global en verde (las
  violaciones preexistentes de `OverlayService.kt` y `ProjectionLifecycleTest.kt`
  se corrigieron en el WP del overlay).
- Empate AMBIGUOUS en tests requiere dos descriptores con keywords idénticas
  (la keyword completa debe aparecer en el texto).

## Verificación

- `.\gradlew.bat :core:platform:test :core:platform:ktlintCheck --console=plain` → en verde.
- `.\gradlew.bat :core:platform:test :core:capture:test :domain:test :feature:overlay:testDebugUnitTest testDebugUnitTest lintDebug assembleDebug --console=plain` → BUILD SUCCESSFUL (428 tareas).
- `.\gradlew.bat ktlintCheck --console=plain` → BUILD SUCCESSFUL.

## Próximos pasos

1. Sin tareas pendientes del WP-E3-02. El siguiente WP candidato es WP-E3-03
   (ver `docs/remediation/WORK_PACKAGE_PLAN.md`) o el que el usuario indique.

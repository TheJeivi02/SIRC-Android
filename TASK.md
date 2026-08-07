# TASK — Estado en vivo

> Actualiza este archivo al iniciar y terminar cada tarea relevante. Es el
> estado que cualquier agente lee primero para saber qué sigue sin re-leer el
> proyecto.

## Tarea actual

**WP-E3-03 implementado** — Unified Capture Source. Pipeline único
`CaptureInput → CaptureRequest → (OCR) → PlatformDetectionEngine →
OfferParserOrchestrator → OfferSnapshot → Repository → Overlay`, eliminando
`ScreenCapture`/`ScreenFrame`/`MediaProjectionScreenCapture` y la resolución de
plataforma duplicada en el coordinador. Pendiente: verificación final + commit
+ informe estándar de 12 puntos.

## Objetivo

1. Unificar la captura en un solo pipeline de `CaptureRequest` con `origin`,
   usando `CaptureInput` como única abstracción de entrada.
2. Corregir el doble guardado de snapshots (coordinador ya no guarda).
3. TDD obligatorio (RED→GREEN→REFACTOR); sin cambios de comportamiento
   observable; dejar las verificaciones en verde.

## Archivos involucrados

Creados:
- `feature/overlay/.../AccessibilityCaptureInput.kt` (lógica del servicio extraída)
- `core/capture/android/.../MediaProjectionCaptureInput.kt` (enriquece con imagen si proyecta)
- `feature/overlay/.../CaptureAccessibilityService.kt` (adaptador delgado reescrito)

Modificados:
- `core/capture/.../coordinator/OfferCaptureCoordinator.kt` (consume `pipeline.snapshots`,
  no guarda; restaurados `closeActiveSession()` + `record()` para `platform == null`)
- `core/capture/.../pipeline/DefaultCapturePipeline.kt` (dedup → OCR → detection engine → parser)
- `core/capture/.../cache/CaptureFrameCache.kt` + `InMemoryCaptureFrameCache.kt` (API solo `request`)
- `core/capture/.../model/` (`CaptureRequest.origin`, `OfferSnapshot.origin`)
- `core/platform/.../DetectionOrigin.kt` → renombrado a `CaptureInputType.kt`
- `core/platform/.../OfferParser.kt` + `OfferParserOrchestrator.kt` (firma con `result`+`detectionMillis`)
- `feature/overlay/.../CaptureModule.kt` (+ `CaptureFlowsModule`), `PlatformModule.kt`
- `core/capture/android/.../CaptureAndroidModule.kt` (sin `bindScreenCapture`)
- `docs/CHANGELOG.md`, `.ai/CONTEXT.md`, `.ai/DECISIONS.md` (D11.13)

Eliminados:
- `core/capture/.../model/ScreenFrame.kt`, `screen/ScreenCapture.kt`
- `core/capture/android/.../MediaProjectionScreenCapture.kt`

## Progreso

- [x] Spec WP-E3-03 aprobado y commiteado (`5598f6d`).
- [x] Tasks 1-12 TDD completadas (rename `CaptureInputType`, overload
      orchestrator, `origin` en models, `CaptureInput` + qualifiers, cache
      solo-request, firma parser, refactor pipeline, tests reescritos, inputs
      Accessibility/MediaProjection, DI completa, eliminación de ScreenCapture/
      ScreenFrame/MediaProjectionScreenCapture).
- [x] `OfferCaptureCoordinator` con `closeActiveSession()` + `record()` restaurados
      (test "paquete no soportado" en verde).
- [x] Docs actualizadas: CHANGELOG (WP-E3-03), CONTEXT (nota + flujo + módulos),
      DECISIONS (D11.13), TASK.
- [ ] Verificación completa final (ktlintCheck, lintDebug, assembleDebug, tests).
- [ ] Commit + informe estándar de 12 puntos.

## Notas / decisiones

- `CaptureInputType` (renombrado de `DetectionOrigin`): legacy PACKAGE/OCR/GALLERY/
  TEST/UNKNOWN + ACCESSIBILITY/MEDIA_PROJECTION/SHARE; valores aditivos.
- DI dual: `@AccessibilityRequests` → `AccessibilityCaptureInput.requests()`,
  `@CaptureRequests` → `MediaProjectionCaptureInput.requests()` (merge Hilt).
- `MediaProjectionCaptureInput`: si proyecta y hay frame → `imageData` PNG +
  `origin = MEDIA_PROJECTION`; si no → pasa el request tal cual (ACCESSIBILITY).
- El coordinador solo agrega: plataforma conocida → `ensureSession` + contadores;
  `platform == null` → `closeActiveSession()` + `record(event)`.
- Sin Robolectric: la lógica de Bitmap se extrajo en `enrichWithImage` (internal
  pura testeable en JVM); el resto de tests del input usan fake del provider.
- ktlint: `kotlin.code.style=official`; `kotlinx.*` antes que `java`/`javax`.
- `PlatformDetectionEngine` es `final`; tests usan instancia real con
  `PlatformDescriptorRegistry`. `DetectionResult` no tiene `detectionMillis`.

## Verificación

- `.\gradlew.bat ktlintCheck --console=plain` → BUILD SUCCESSFUL.
- `.\gradlew.bat lintDebug assembleDebug testDebugUnitTest :domain:test :core:platform:test :core:capture:test :core:capture:android:testDebugUnitTest :feature:overlay:testDebugUnitTest --console=plain` → BUILD SUCCESSFUL.

## Próximos pasos

1. Re-ejecutar la verificación completa final y revisar `git status --short`.
2. Commit (mensaje WP) y entregar el informe estándar de 12 puntos.
3. WP candidato siguiente: ver `docs/remediation/WORK_PACKAGE_PLAN.md`.

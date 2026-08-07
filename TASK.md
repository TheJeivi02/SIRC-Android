# TASK — Estado en vivo

> Actualiza este archivo al iniciar y terminar cada tarea relevante. Es el
> estado que cualquier agente lee primero para saber qué sigue sin re-leer el
> proyecto.

## Tarea actual

**WP-E3-04 — Auditoría final (en curso, fase DOCUMENTO)**. Auditoría
arquitectónica completa de los 8 módulos tras WP-E3-01→E3-03. Documento
`docs/audit/architecture/ARCHITECTURE_AUDIT_SPRINT11.md` generado (29 hallazgos:
0 críticos, 3 altos, 9 medios, 10 bajos, 7 observaciones). **Pendiente de
aprobación** del usuario; tras aprobarlo se define el plan de corrección
(WP-E3-05 y siguientes). Sin cambios de código aún (regla del WP).

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

## Progreso WP-E3-04

- [x] Exploración de contexto (git, estructura de módulos).
- [x] Auditoría profunda con exploradores en paralelo (4 frentes: core:capture
      + android, core:platform, feature:overlay, domain/data/app/ui).
- [x] Verificación manual de los hallazgos de mayor señal (orquestador dual
      engine, `PipelineOverlayDataSource.start()`, coordinador, `FeatureFlag`,
      `PlatformDescriptors`, overloads de parseo en producción).
- [x] Documento `docs/audit/architecture/ARCHITECTURE_AUDIT_SPRINT11.md`
      redactado (hallazgos con problema/impacto/propuesta/riesgo).
- [ ] **Aprobación del usuario** del documento.
- [ ] Definir plan de corrección (WP-E3-05 y siguientes) priorizando
      Alto → Medio → Bajo.

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

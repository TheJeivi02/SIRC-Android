# TASK — Estado en vivo

> Actualiza este archivo al iniciar y terminar cada tarea relevante. Es el
> estado que cualquier agente lee primero para saber qué sigue sin re-leer el
> proyecto.

## Tarea actual

**LOOP ENGINEERING — BACKEND SUPABASE + EVALUACIÓN ANTIGRAVITY + ARQUITECTURA
DE MONETIZACIÓN (16-ago-2026). Solo documentación; sin código.**
Se decidió el backend inicial, el modelo de suscripción, se amplió el threat
model y se redefinió el rol de las herramientas de agente.

Decisiones registradas (ver `.ai/DECISIONS.md` D14.1–D14.4):

- **D14.1 — Supabase como backend inicial**: Auth + RLS + Edge Functions +
  Postgres para identidad/suscripción/entitlement; camino crítico de oferta
  100 % local; plan **Pro** en producción (Free pausa proyectos); sin Realtime
  ni Storage; ninguna oferta/pantalla se sube.
- **D14.2 — Play API v2**: verificación server-side con
  `purchases.subscriptionsv2.get` (la `subscriptions.get` está **deprecada**);
  RTDN = señal (re-consultar API, dedupe `messageId`, JWT OIDC del push).
- **D14.3 — Entitlement + offline**: TTL 24–72 h (S2); source of truth por
  capas (Play=transacción, Backend=operativa, Supabase=persistencia,
  Cliente=caché); threat model ampliado a **T15–T20**.
- **D14.4 — Herramientas**: **OpenCode principal + Antigravity complementario**
  (no sustituye); regla **R17** (prohibido doble-agente simultáneo en el mismo
  branch).

Entregables:

- **`docs/BACKEND_ARCHITECTURE.md`** (nuevo) — Supabase (Auth/RLS/Edge
  Functions/secrets), modelo de datos (users/profiles/plans/subscriptions/
  entitlements/devices/sessions), flujo Play Billing→backend, RTDN, source of
  truth, offline, T15–T20, privacidad.
- **`docs/SUBSCRIPTION_MODEL.md`** (nuevo) — estructura conceptual de planes
  (Free/Trial · Basic · Pro + futuro), matriz de precios de competencia
  verificada (Motorista One, GigU, Maxymo, Mystro, Viaje Rentable, Operdrive…),
  entitlement por features, lifecycle/estados y acción por evento RTDN.
- **`docs/ANTIGRAVITY_EVALUATION.md`** (nuevo) — estado Antigravity 2.0/IDE/CLI,
  comparativa OpenCode vs Antigravity y decisión OPCIÓN C.
- **`docs/SECURITY_MODEL.md`** (v1→v2) — T15–T20 añadidos, T1–T14 referencias
  corregidas, API de Play v2 y estados modernos, backend conceptual = Supabase.
- **`docs/PRODUCT_STRATEGY.md`** — §1ter (Supabase), P1 actualizada (#1 backend,
  #2 Play Billing v2), referencia a planes.
- **`docs/ROADMAP.md`** — decisiones de la ruta actualizadas (Supabase, planes,
  v2, herramientas).
- **`.ai/RULES.md`** — reglas 9g (Supabase), 9h (verificación v2 server-side) y
  R17 (multi-agente). `.ai/CONTEXT.md` — decisión de backend/planes/herramientas.
  `.ai/DECISIONS.md` — D14.1–D14.4. `docs/ARCHITECTURE.md`, `docs/PROJECT.md`.

**Siguiente: (en curso)** verificar `git status` y commit (solo docs) + push.
Luego entregar reporte final del LOOP (A–Q). No implementar nada de E1b/E1a sin
abrir la tarea (regla R16/9f).

## Tarea anterior

**WP-E3-05E completado** — Limpieza documental final del Sprint 11 derivada
exclusivamente de los refactors WP-E3-02 → WP-E3-05D. Solo se corrigió
documentación que describía el estado actual como si las piezas eliminadas
siguieran existiendo; los registros históricos (decisiones, auditorías,
informes por sprint, roadmap, specs) se conservan íntegros. Sin cambios de
código ni de comportamiento. Commit único + informe final entregado.

- **`docs/ARCHITECTURE.md`**: diagramas y listas de `:core:capture` y
  `:core:capture:android` actualizados a la arquitectura real (pipeline
  `CaptureInput → OCR → detección → parser`, `CaptureInputType`, estados
  `WAITING/PROCESSING/ERROR`, `MediaProjectionCaptureInput`, `CaptureAndroidModule`
  sin `ScreenCapture`); diagrama de análisis sin `RuleEngine`; tabla de
  decisiones sin `RuleEngine`/`ScreenCapture`/`FakeParser`/`CAPTURING`/
  flags `RULES`/`METRICS`/`ACCESSIBILITY`.
- **`.ai/CONTEXT.md`**: flujo real (paso 4) sin `RuleEngine` (explica el
  `RuleEvaluation` vacío por compat UI); resumen de arquitectura sin
  `RuleEngine`/`OfferValidator`/`RuleThresholds`/`RuleContext`/`OfferRule`/
  `ValidationResult`/parsers especializados.
- **`.ai/AGENTS.md`**: el rol Accessibility Engineer es dueño del
  `CaptureAccessibilityService` (no `SircAccessibilityService`).
- **`docs/KNOWN_ISSUES.md`**: servicio único de accesibilidad (WP-E1-03);
  terminología de parsers actualizada; backlog sin "un solo servicio".
- **`docs/CHANGELOG.md`**: entradas WP-E3-05B, 05C, 05D y 05E añadidas.

**Siguiente: (pausa)** se entrega informe final para aprobación. No se inicia
otro WP tras WP-E3-05E sin aprobación explícita.

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
- [x] Docs descriptivas (CHANGELOG/DECISIONS/CONTEXT) → diferidas a un WP
      posterior (regla: no tocar documentación en 05B).

## Progreso WP-E3-05C (severidad Media restante)

- [x] **M-7**: eliminados `CaptureInputType.SHARE/GALLERY/TEST` (se mantienen
      `UNKNOWN/ACCESSIBILITY/MEDIA_PROJECTION/OCR/PACKAGE`).
- [x] **M-8**: eliminado el bundle LEGACY de reglas (`RuleEngine`, 6 reglas,
      `OfferValidator`, `ValidationResult`, `ValidationIssue`, `RuleContext`,
      `RuleThresholds`, `OfferRule` y helpers/tests); se conserva solo la API
      viva del overlay (`RuleEvaluation`, `RuleResult`, `RuleVerdict`);
      eliminados `resultFor()` y `TripOffer.pickupDistanceKm`.
- [x] **M-9**: `defaultCurrency` obligatorio no nulo en
      `GenericPlatformExtractor`; eliminado `DEFAULT_CURRENCY`.
- [x] Verificación completa en verde; commit `c1f57c4`.

## Progreso WP-E3-05D (severidad Baja)

- [x] **B-1/B-3/B-4/B-6/B-7/B-8** resueltos; **B-2/B-5/B-9** conservados con
      justificación; **B-10** verificado (detalle en "Tarea actual" histórica).
- [x] Verificación completa en verde; commit `008e792` + informe final.

## Progreso WP-E3-05E (limpieza documental)

- [x] Búsqueda repo-wide de referencias obsoletas y clasificación (histórica /
      actual-incorrecta / muerta).
- [x] Corregidos `docs/ARCHITECTURE.md`, `.ai/CONTEXT.md`, `.ai/AGENTS.md`,
      `docs/KNOWN_ISSUES.md`; entradas WP-E3-05B/C/D/E en `docs/CHANGELOG.md`.
- [x] Sin cambios de código de producción (verificado con `git diff`).
- [x] Commit único + informe final; **pausa** esperando aprobación.

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

1. Cierre oficial del Sprint 11 tras la aprobación del informe de WP-E3-05E
   (Architecture / Performance / Technical Debt / Sprint Review).
2. No iniciar ningún otro WP sin aprobación explícita.

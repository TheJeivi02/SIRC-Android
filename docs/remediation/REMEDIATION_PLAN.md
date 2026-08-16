# REMEDIATION PLAN

**Project:** SIRC v1.0.0-rc1  
**Purpose:** Convert all 191 findings from the 9 audits into an actionable implementation plan organized by Epics.  
**Source Documents:** 9 audit documents listed in `docs/audit/`  
**Prepared by:** Principal Engineering Manager / TPM / Release Planning Lead  
**Date:** 2026-08-02  

---

## Executive Summary

### Consolidated Metrics

| Metric | Value |
|---|---|
| **Total Epics** | 12 |
| **Total Sprints** | 7 (2-week duration each) |
| **Total Effort** | 136 developer-days |
| **Total Findings** | 191 |
| **Findings Before Beta** | 69 (all CRÍTICO, ALTO, and P0/P1) |
| **Findings for v1.1** | 79 (P2) |
| **Findings for v2.0** | 43 (P3) |

### Time Estimation by Sprint

| Sprint | Phase | Duration | Team Effort | Key Milestone |
|---|---|---|---|---|
| Sprint 1 | Phase 1 | 2 weeks | 13 dias | S-S14 device verification, debug panel gated, pipeline unified |
| Sprint 2 | Phase 1 | 2 weeks | 10 dias | Security complete (SQLCipher, Data Safety), contrast fixed |
| Sprint 3 | Phase 2 | 2 weeks | 14 dias | MediaProjection lifecycle safe, platform detection ready |
| Sprint 4 | Phase 2/3 | 2 weeks | 13 dias | RC2 candidate — architecture epics complete |
| Sprint 5 | Phase 3/4 | 2 weeks | 15 dias | Beta candidate — UX complete, testing infra established |
| Sprint 6 | Phase 4/5 | 2 weeks | 12 dias | Google Play compliance, performance baseline |
| Sprint 7 | Phase 6/7 | 2 weeks | 11 dias | Final optimization, repository cleanup |

### Recommended Implementation Order

1. **EPIC-11 (DOC-2.1)** — Zero-risk, immediate clarity (Day 1)
2. **EPIC-05 (critical)** — Play compliance gate, parallel to pipeline work
3. **EPIC-01** — Root cause: dual pipeline (Sprints 1-2)
4. **EPIC-02** — Lifecycle safety on unified pipeline (Sprint 3)
5. **EPIC-03** — Platform detection on single pipeline (Sprint 3)
6. **EPIC-04** — Decision engine consolidation (Sprint 4)
7. **EPIC-07** — Presentation layer after domain is stable (Sprint 5)
8. **EPIC-08** — Design system after ViewModel refactoring (Sprint 5-6)
9. **EPIC-06** — Drive-safe UX after design system (Sprint 6)
10. **EPIC-10** — Tests after code is refactored (Sprint 5-6, parallel)
11. **EPIC-09** — Performance after stable architecture (Sprint 6-7)
12. **EPIC-12** — Edge case stability last (Sprint 7)

Full technical justification for this order: see `IMPLEMENTATION_ORDER.md`.

### Principal Risks

| Risk | Epic | Probability | Impact | Mitigation |
|---|---|---|---|---|
| S-S14: `exported="false"` breaks AccessibilityService binding | EPIC-01 | High | **Critical** (invalidates capture) | **Device verification Sprint 1, Day 1** |
| Pipeline regression during legacy removal | EPIC-01 | Medium | High (capture failure) | Parallel testing with both services active during transition |
| SQLCipher introduces 16KB alignment crash | EPIC-05 | Low | High (Play rejection) | Verify all `.so` libraries with `zipalign -c -P 16` |
| OCR accuracy degrades with resolution reduction | EPIC-09 | Medium | Medium (missed offers) | Validate against 15 test images; conservative gating |
| UX changes cause layout breakage on small screens | EPIC-06 | Medium | Low (cosmetic) | Test on multiple screen sizes and densities |
| Test flakiness blocks CI pipeline | EPIC-10 | Medium | Medium (development velocity) | Virtual time in tests; dedicated stability week |

### Beta Blockers (CRÍTICO findings)

| ID | Finding | Required By | Epic |
|---|---|---|---|
| MPR-6.1 | MediaProjectionService without onDestroy | Sprint 3 | EPIC-02 |
| S-S14 | `exported="false"` on AccessibilityService | Sprint 1 (device verification) | EPIC-01 |
| SCA-11.1 | Platform detection tied to Spanish+Uber | Sprint 3 | EPIC-03 |
| GP-1 | False Data Safety declaration | Sprint 2 | EPIC-05 |
| UX-2.1 | ProfitIndicator contrast <4.5:1 | Sprint 2 | EPIC-06 |
| DOC-2.1 | Missing CHANGELOG v0.5.0 header | Sprint 1 | EPIC-11 |

Additionally, all 38 ALTO findings should be resolved before Beta per the Executive Summary's recommendation.

### Final Recommendation: **Advance to RC2, then Beta**

**Stage 1 — RC2 (End of Sprint 4):** Approve RC2 after EPIC-01, EPIC-02, EPIC-03, EPIC-04, and EPIC-11 (P1) are complete. This delivers:
- Unified capture pipeline
- Safe MediaProjection lifecycle
- Platform-agnostic detection
- Single decision engine
- Accurate documentation

**Stage 2 — Beta (End of Sprint 5):** Publish Beta after EPIC-05 (complete), EPIC-06 (complete), EPIC-07, and EPIC-08 are complete. This delivers:
- Full Google Play compliance (Data Safety, Privacy Policy, Prominent Disclosure)
- WCAG AA drive-safe UX
- Secure database with SQLCipher
- Comprehensive test coverage for core features

**Stage 3 — RC3 / Release (End of Sprint 7):** All 191 findings resolved, performance optimized, repository clean.

**Do NOT remain in RC1** — the CRÍTICO findings (especially GP-1 Data Safety and S-S14 AccessibilityService binding) create unacceptable compliance and safety risk. **Do NOT publish Beta immediately** — the 6 CRÍTICO + 38 ALTO findings must be resolved first.

---

## Epics Overview

| Epic ID | Name | Findings | Auditorías | Priority | Est. Effort |
|---|---|---|---|---|---|
| EPIC-01 | Unify & Optimize Capture Pipeline | 21 | Architecture, Android, Perf, Stability, Play | P0/P1 | 18 días |
| EPIC-02 | MediaProjection Lifecycle Safety | 9 | Android, Stability, Perf | P0/P1 | 8 días |
| EPIC-03 | Platform-Agnostic Detection Framework | 12 | Architecture, Stability | P0/P1 | 15 días |
| EPIC-04 | Consolidate Decision Engine | 12 | Architecture, Android, QA | P0/P1 | 12 días |
| EPIC-05 | Security, Privacy & Data Protection | 8 | Security, Google Play | P0/P1 | 10 días |
| EPIC-06 | Drive-Safe UX & Accessibility | 15 | UX, Stability, Play | P0/P1 | 14 días |
| EPIC-07 | Presentation Layer Architecture | 16 | Architecture, Android, QA | P1/P2 | 16 días |
| EPIC-08 | Localization & Design System Unification | 10 | UX, Stability | P1/P2 | 12 días |
| EPIC-09 | Performance Optimization | 15 | Performance, Android | P1/P2 | 14 días |
| EPIC-10 | Testing Infrastructure & Coverage | 13 | QA | P1/P2 | 16 días |
| EPIC-11 | Documentation Integrity | 13 | Documentation | P1/P2 | 8 días |
| EPIC-12 | Stability & Lifecycle Hardening | 14 | Stability, Android, UX | P2/P3 | 10 días |

---

## Epic Definitions

### EPIC-01 — Unify & Optimize Capture Pipeline

| Attribute | Value |
|---|---|
| **Objective** | Eliminate duplicate capture pipeline and dual AccessibilityService; establish a single source of truth for screen capture. |
| **Description** | Two complete, parallel capture architectures run simultaneously in the same process: the legacy `SircAccessibilityService → AccessibilityWindowObserver → OfferCaptureCoordinator` and the modern `CaptureAccessibilityService → DebounceCaptureScheduler → DefaultCapturePipeline`. Both share the same config, same filter, and same parser. This causes double tree traversal (up to 400 nodes/event), double OCR/parse, double persistence, battery drain, and inconsistent states. The legacy coordinator also ignores `FeatureFlag.PARSER` in the production path. |
| **Findings Included** | ARC-1.1, DUP-10.1, DUP-10.3, PKG-7.1, ACC-7.1, ACC-7.2, ACC-7.3, ACC-7.4, ACC-7.5, LIF-3.1, S-S15, S-S16, GP-4, P-P08, P-P17 |
| **Auditorías** | Arquitectura, Android, Performance, Stability, Google Play |
| **Root Cause** | Incomplete migration: the "legacy eliminated" claim in docs/ARCHITECTURE.md:293 is false. Both services remain declared and active in AndroidManifest.xml. |
| **Affected Modules** | `:app`, `:feature:overlay`, `:core:capture`, `:core:capture:android`, `:core:platform` |
| **Dependencies** | None (this is the first epic to execute) |
| **Risks** | Service fails to receive events if both removed without proper replacement. Debug features tied to legacy coordinator will break. |
| **Regression Risk** | High — this is the primary capture path. Must keep the modern pipeline fully functional before removing legacy. |
| **Verification** | Device test: single AccessibilityService receives events, single pipeline processes them, no duplicate history entries. |
| **Complexity** | High (core architectural change) |
| **Expected Benefit** | 50% CPU/battery reduction in capture path; single source of truth; eliminates all dual-write data inconsistency. |
| **Estimated Effort** | 18 developer-days |
| **Priority** | P0 |

---

### EPIC-02 — MediaProjection Lifecycle Safety

| Attribute | Value |
|---|---|
| **Objective** | Eliminate MediaProjection resource leaks; ensure proper lifecycle cleanup on service death, process kill, and Doze. |
| **Description** | `MediaProjectionService` has no `onDestroy()` override, meaning if the system kills the FGS (via Doze, low memory, or task removal), the VirtualDisplay, ImageReader, and MediaProjection token remain leaked. The projection stays "active" in the system, draining battery and causing the UI to falsely report capture state. Additionally, channels use CONFLATED with `acquireLatestImage()` which can cause closed-image exceptions under load, and `getMediaProjection()` lacks try/catch for `SecurityException`. |
| **Findings Included** | MPR-6.1, MPR-6.2, MPR-6.3, MPR-6.5, BRT-12.3, S-S10, S-S20, S-S28 |
| **Auditorías** | Android, Stability, Performance |
| **Root Cause** | Incomplete lifecycle management in the MediaProjection service. Missing cleanup hook on service destruction. |
| **Affected Modules** | `:core:capture:android`, `:feature:overlay` |
| **Dependencies** | EPIC-01 (unified pipeline first, so cleanup logic is in one place) |
| **Risks** | Fixing the lifecycle could break the existing START_NOT_STICKY behavior. Must coordinate with overlay service state management. |
| **Regression Risk** | Medium — changes the lifecycle behavior of the capture provider. Need thorough testing on Android 12-15. |
| **Verification** | Kill process / Doze simulation: overlay should correctly report non-capturing state; no battery drain from leaked projection. |
| **Complexity** | High (concurrent resource management) |
| **Expected Benefit** | Zero battery drain from leaked projection; correct state reporting; compliance with Android FGS lifecycle. |
| **Estimated Effort** | 8 developer-days |
| **Priority** | P0 |

---

### EPIC-03 — Platform-Agnostic Detection Framework

| Attribute | Value |
|---|---|
| **Objective** | Decouple screen detection from Spanish+Uber vocabulary; enable correct parsing of DiDi, Bolt, Lyft, Cabify, InDrive. |
| **Description** | The detection engine uses a single global keyword list hardcoded in Spanish with Uber-specific terms. Screens from other platforms with different text are classified as UNKNOWN and never parsed, even though the generic extractor exists. Additionally, parser selection is hardcoded with `if (platform == UBER)`, the parser base class defaults to Uber extractor, the enum lacks Lyft/Bolt, and currency is assigned per-platform rather than per-user-profile. History stats sum profits across platforms without currency normalization. |
| **Findings Included** | SCA-11.1, SCA-11.2, SCA-11.3, SCA-11.4, SCA-11.5, SCA-11.6, SCA-11.7, SCA-11.8, SCA-11.9, SCA-11.10, SCA-11.12, S-S08 |
| **Auditorías** | Arquitectura, Stability |
| **Root Cause** | Platform integration was designed as a single-platform solution. The abstraction layer exists but is not utilized. |
| **Affected Modules** | `:core:platform`, `:core:capture`, `:domain`, `:data`, `:feature:overlay` |
| **Dependencies** | EPIC-01 (pipeline must be unified before changing detection wiring) |
| **Risks** | Changing the detection rules could cause false negatives for the existing Uber use case. Must add detection rules incrementally with tests. |
| **Regression Risk** | High — Uber detection is the only currently working platform. Any change must be validated against existing test cases. |
| **Verification** | Detection tests for each platform's keywords; integration tests with sample texts. |
| **Complexity** | High (multi-platform abstraction) |
| **Expected Benefit** | App becomes truly multi-platform ready; enables future expansion to DiDi/Bolt/Lyft/Cabify/InDrive. |
| **Estimated Effort** | 15 developer-days |
| **Priority** | P0 |

---

### EPIC-04 — Consolidate Decision Engine

| Attribute | Value |
|---|---|
| **Objective** | Unify the dual decision engines (RuleEngine + ProfitEngine) into a single source of truth; eliminate dead use-cases and threshold duplication. |
| **Description** | There are two competing decision subsystems: `DecisionThresholds.default()` (4.0, 120.0) and `RuleThresholds.DEFAULT_MIN_PROFIT_PER_KM/HOUR` with duplicated limits (500/5000). Both `ProfitEngine.decide()` and `RuleEngine.allPassed` exist but `RuleEvaluation.allPassed` never gates the decision. Two use-cases are orphaned (zero call sites): `EvaluateOfferUseCase` and `AddOfferHistoryUseCase`. The `isConfigured()` method contradicts the domain definition, allowing partial saves to skip onboarding. A `FakeParser` is injectable in production. |
| **Findings Included** | ARC-1.3, ARC-1.4, DUP-10.6, DEAD-9.1, DEAD-9.2, DEAD-9.3, ABS-8.2, DEAD-9.4 |
| **Auditorías** | Arquitectura, Android |
| **Root Cause** | Incremental additions created parallel systems without removing the old ones. Use-case layer was started but abandoned. |
| **Affected Modules** | `:domain`, `:data`, `:core:capture`, `:feature:overlay` |
| **Dependencies** | EPIC-01 (pipeline must be consolidated first to know which thresholds authority wins) |
| **Risks** | Removing dead use-cases could break indirect dependencies not visible in static analysis. Must verify with grep before removal. |
| **Regression Risk** | Medium — changes the decision logic. Must validate all 26 existing domain tests pass. |
| **Verification** | All domain engine tests pass; single threshold source verified; no orphaned use-cases. |
| **Complexity** | Medium-High (business logic) |
| **Expected Benefit** | Single decision authority; no conflicting verdicts possible; dead code eliminated. |
| **Estimated Effort** | 12 developer-days |
| **Priority** | P1 |

---

### EPIC-05 — Security, Privacy & Data Protection

| Attribute | Value |
|---|---|
| **Objective** | Resolve all CRÍTICO and ALTO security findings: DB encryption, debug panel gating, backup protection, Data Safety correction, privacy policy, and prominent disclosure. |
| **Description** | Room database stores PII (driver name, city, vehicle) unencrypted. `allowBackup="true"` without exclusion rules exposes this in backups. The debug panel (available in release builds) shows raw OCR text captured from other apps. The Data Safety declaration falsely states "no data collected." No privacy policy exists. No prominent in-app disclosure about Accessibility/MediaProjection usage. |
| **Findings Included** | S-1, S-2, S-3, S-4, S-5, GP-1, GP-2, GP-3 |
| **Auditorías** | Security, Google Play |
| **Root Cause** | Security hardening was not addressed during development; release was built with debug infrastructure enabled. |
| **Affected Modules** | `:app`, `:data`, `:feature:overlay`, `:core:capture` |
| **Dependencies** | None (security is independent) |
| **Risks** | SQLCipher adds native library size. Must verify 16KB alignment. Debug panel removal may hide features that debug tests rely on. |
| **Regression Risk** | Low-Medium — these are production-hardening changes that don't affect core functionality. |
| **Verification** | DB encrypted (SQLCipher verified); debug panel absent in release build; Data Safety form corrected; privacy policy published. |
| **Complexity** | Medium (infrastructure changes) |
| **Expected Benefit** | Full Google Play compliance; no PII exposure risk; Beta approvable from security standpoint. |
| **Estimated Effort** | 10 developer-days |
| **Priority** | P0 |

---

### EPIC-06 — Drive-Safe UX & Accessibility

| Attribute | Value |
|---|---|
| **Objective** | Fix all CRÍTICO and ALTO UX findings: contrast for driving, readable overlay text, TalkBack support, touch targets, and destructive action confirmations. |
| **Description** | The `ProfitIndicator` uses white text on low-luminance backgrounds (2.0–3.9:1 contrast), failing WCAG AA and creating a safety risk during driving. The overlay is not focusable by TalkBack. Text sizes are 9–12sp (legibility threshold for driving is 14sp+). Touch targets are below 48dp. The history deletion has no confirmation dialog. Currency field has no ISO validation. |
| **Findings Included** | UX-2.1, UX-2.2, UX-2.3, UX-2.4, UX-4.1, UX-6.1, UX-11.1, UX-11.2, UX-11.3, UX-11.4 |
| **Auditorías** | UX, Stability |
| **Root Cause** | UI was optimized for visual design, not driving-context legibility or accessibility compliance. |
| **Affected Modules** | `:core:ui`, `:feature:overlay`, `:feature:history`, `:feature:settings` |
| **Dependencies** | EPIC-08 (design system must be unified to apply consistent typography) |
| **Risks** | Changing overlay contrast and text size may reduce information density. Must balance safety with usability. |
| **Regression Risk** | Low — visual changes only, no behavioral regressions. |
| **Verification** | Contrast ≥4.5:1 on ProfitIndicator; text ≥14sp in overlay; TalkBack reads decision; delete confirmation dialog. |
| **Complexity** | Medium |
| **Expected Benefit** | WCAG AA compliance; safe driving experience; accessibility support for visually impaired drivers. |
| **Estimated Effort** | 14 developer-days |
| **Priority** | P0 |

---

### EPIC-07 — Presentation Layer Architecture

| Attribute | Value |
|---|---|
| **Objective** | Decompose god-classes, fix ViewModel lifecycle/scoping, establish clean presentation layer conventions. |
| **Description** | `PipelineOverlayDataSource` is a 329-line god-class with 13 dependencies handling pipeline translation, evaluation, persistence, session metrics, and TTL. `DebugPanelViewModel` (331 lines) is instantiated at the NavHost root, running continuously. `OverlayService` mixes Service lifecycle, notification, WindowManager, and Compose rendering in one file. ViewModels lack SavedStateHandle and have no tests. The `OnboardingViewModel.save()` has no error handling. `HistoryViewModel` exposes `ProfitEngine` directly to Compose. |
| **Findings Included** | SOL-2.1, SOL-2.2, SOL-2.3, SOL-2.4, SOL-2.5, CMP-1.1, CMP-1.3, CMP-1.5, MIX-6.1, MIX-6.2, MOD-3.1, VM-9.1, VM-9.2, VM-9.3, SIZ-5.1 |
| **Auditorías** | Arquitectura, Android |
| **Root Cause** | Rushed implementation during sprints; presentation layer conventions not enforced. |
| **Affected Modules** | `:app`, `:feature:overlay`, `:feature:history`, `:feature:settings`, `:feature:onboarding`, `:domain` |
| **Dependencies** | EPIC-04 (decision engine consolidation before extracting analyzer collaborator) |
| **Risks** | Decomposing god-classes could introduce subtle behavioral changes. Must preserve exact behavior with tests. |
| **Regression Risk** | Medium — significant refactoring of the presentation layer. |
| **Verification** | All existing tests pass; new ViewModels have tests; DebugPanel scoped to its route; god-classes decomposed. |
| **Complexity** | High (extensive refactoring) |
| **Expected Benefit** | Testable, maintainable presentation layer; reduced crash risk; cleaner separation of concerns. |
| **Estimated Effort** | 16 developer-days |
| **Priority** | P1 |

---

### EPIC-08 — Localization & Design System Unification

| Attribute | Value |
|---|---|
| **Objective** | Extract all hardcoded strings to resources, complete the design system tokens, and fix internationalization issues. |
| **Description** | All screen text is hardcoded in Kotlin (Spanish only) — `strings.xml` only has `app_name`. The theme's light color scheme is incomplete (only primary/secondary defined). Typography defines only 5 of 15 Material styles. `SircSpacing` tokens exist but are barely used. Currency formatting is inconsistent across screens with 5 different formatters. |
| **Findings Included** | UX-12.1, UX-1.1, UX-1.2, UX-1.3, UX-3.1, UX-3.2, UX-5.1, UX-6.2, UX-7.2, UX-8.1, UX-9.1, UX-2.5 |
| **Auditorías** | UX, Stability |
| **Root Cause** | No localization or design system strategy was established before implementation. |
| **Affected Modules** | `:app`, `:feature:*`, `:core:ui`, all modules with UI |
| **Dependencies** | EPIC-07 (ViewModel changes may affect formatting locations) |
| **Risks** | String extraction is error-prone; one missed string could crash the app. Must be done carefully with tests. |
| **Regression Risk** | Low-Medium — text changes only, though formatting logic could have edge cases. |
| **Verification** | No hardcoded strings in .kt files; complete light theme; all screens use SircSpacing/SircTypography. |
| **Complexity** | Medium |
| **Expected Benefit** | Foundation for multi-language support; consistent UI; easier maintenance. |
| **Estimated Effort** | 12 developer-days |
| **Priority** | P1 |

---

### EPIC-09 — Performance Optimization

| Attribute | Value |
|---|---|
| **Objective** | Reduce CPU, memory, and battery consumption by optimizing the OCR pipeline and coroutine usage. |
| **Description** | OCR runs on full-resolution screen captures (1080×2400) without pre-detection gating — it runs even when the screen is not a ride offer. The pipeline creates a PNG round-trip (compress then decode) adding hundreds of ms. Text normalization happens 3 times per request. The `DebugPanelViewModel` runs continuously at the NavHost root. VirtualDisplay runs at full refresh rate with no frame rate limiting. |
| **Findings Included** | P-P01, P-P02, P-P03, P-P04, P-P05, P-P06, P-P07, P-P08, P-P09, P-P10, P-P11, P-P12, P-P13, P-P14, P-P15, P-P16 |
| **Auditorías** | Performance, Android |
| **Root Cause** | No performance budget was established; optimizations deferred to later sprints. |
| **Affected Modules** | `:core:capture`, `:core:capture:android`, `:feature:overlay`, `:app` |
| **Dependencies** | EPIC-01 (pipeline unification first) |
| **Risks** | Reducing OCR resolution could miss small text. Must balance accuracy vs performance. |
| **Regression Risk** | Medium — performance changes could affect OCR accuracy and timing. |
| **Verification** | OCR latency <500ms; memory usage reduced; no duplicate processing; debug panel scoped to route. |
| **Complexity** | High |
| **Expected Benefit** | 40% reduction in capture pipeline latency; reduced battery drain; smoother overlay performance. |
| **Estimated Effort** | 14 developer-days |
| **Priority** | P1 |

---

### EPIC-10 — Testing Infrastructure & Coverage

| Attribute | Value |
|---|---|
| **Objective** | Establish comprehensive test coverage for the presentation layer, OCR engine, instrumented services, and edge cases. |
| **Description** | Zero tests exist for ViewModels (9/9), Compose UI, the OCR engine, or instrumented service flows. The CI doesn't run instrumented tests. Edge cases for division by zero, NaN, multi-currency, and multi-language are untested. Room migration testing only covers v1→v3 with 1 row. |
| **Findings Included** | H-QA-01, H-QA-02, H-QA-03, B-QA-08, B-QA-09, M-QA-04, M-QA-05, M-QA-06, M-QA-07 |
| **Auditorías** | QA |
| **Root Cause** | Testing infrastructure was not established; presentation layer was built without testability in mind. |
| **Affected Modules** | All modules (new test files) |
| **Dependencies** | EPIC-07 (ViewModels must be refactored for testability first) |
| **Risks** | Adding tests takes time away from features. Must prioritize critical path coverage. |
| **Regression Risk** | None — adding tests only |
| **Verification** | 80%+ of new code has tests; CI runs instrumented tests; edge cases covered. |
| **Complexity** | Medium-High |
| **Expected Benefit** | Regression safety net; confidence in future changes; CI catches bugs before release. |
| **Estimated Effort** | 16 developer-days |
| **Priority** | P1 |

---

### EPIC-11 — Documentation Integrity

| Attribute | Value |
|---|---|
| **Objective** | Correct all documentation inconsistencies that cause confusion or incorrect decisions. |
| **Description** | CHANGELOG is missing the v0.5.0 header (content merged into v0.6.0). README, AGENTS.md, and ROADMAP declare 8 modules while the project has 11. PROJECT.md references v0.1.0 while the project is at v1.0.0-rc1. ARCHITECTURE.md falsely claims the legacy pipeline was eliminated. ProfitEngine lacks formula documentation in KDoc. |
| **Findings Included** | DOC-2.1, DOC-2.2, DOC-3.1, DOC-3.2, DOC-4.1, DOC-5.1, DOC-5.2, DOC-7.1, DOC-8.1, DOC-9.1, DOC-9.2, DOC-9.3, DOC-9.4 |
| **Auditorías** | Documentation |
| **Root Cause** | Documentation was not kept in sync with code evolution. |
| **Affected Modules** | All documentation files (`.md` and `.kt` KDoc) |
| **Dependencies** | None (can run in parallel) |
| **Risks** | Low — documentation only, no code changes. |
| **Regression Risk** | None |
| **Verification** | All docs consistent: module count, version references, pipeline status, formulas documented. |
| **Complexity** | Low |
| **Expected Benefit** | Accurate documentation prevents onboarding confusion and incorrect architectural decisions. |
| **Estimated Effort** | 8 developer-days |
| **Priority** | P1 |

---

### EPIC-12 — Stability & Lifecycle Hardening

| Attribute | Value |
|---|---|
| **Objective** | Fix lifecycle issues, configuration change handling, process death state management, and edge case robustness. |
| **Description** | `accessibility_service_config.xml` has `exported="false"` which may prevent system binding (CRÍTICO, requires device verification). MediaProjectionService uses `START_NOT_STICKY` causing capture loss after kill. The overlay captures itself (OCR reads SIRC's own text). No `onMultiWindowModeChanged` implementation. Clamp logic broken with `WRAP_CONTENT` heights. No `SavedStateHandle` on ViewModels. `getMediaProjection()` can throw `SecurityException` without try/catch. No API 24-25 fallback for `TYPE_APPLICATION_OVERLAY`. |
| **Findings Included** | S-S02, S-S03, S-S04, S-S05, S-S06, S-S07, S-S08, S-S09, S-S10, S-S11, S-S12, S-S13, S-S18, S-S20, S-S22, S-S24, S-S28, CMP-1.1, CMP-1.2, CMP-1.4, CMP-1.6, LIF-3.2, FLW-4.1, FLW-4.2, FLW-4.3, CO-5.1, FGS-8.2, FGS-8.3, FGS-8.4, WKM-10.1, BRT-12.1 |
| **Auditorías** | Stability, Android, UX |
| **Root Cause** | Android lifecycle nuances were not fully addressed during v1.0 implementation. |
| **Affected Modules** | `:app`, `:feature:overlay`, `:core:capture:android`, `:data`, `:domain` |
| **Dependencies** | EPIC-01, EPIC-02 (core capture services must be unified first) |
| **Risks** | Device-specific behavior means some fixes can only be validated on hardware. |
| **Regression Risk** | Medium — lifecycle changes can introduce subtle bugs. |
| **Verification** | All device verification checklist items pass: rotation, split-screen, process death, permission revocation, locale change. |
| **Complexity** | High |
| **Expected Benefit** | Robust operation across all Android versions and lifecycle events; no state loss or crashes. |
| **Estimated Effort** | 10 developer-days |
| **Priority** | P1 |

---

## Blocking Findings Summary (Before Beta)

| Epic | Blocking Findings | Severity |
|---|---|---|
| EPIC-01 | ARC-1.1, ACC-7.4, GP-4 | ALTO/ALTO/ALTO |
| EPIC-02 | MPR-6.1, S-S10 | **CRÍTICO** / ALTO |
| EPIC-03 | SCA-11.1 | **CRÍTICO** |
| EPIC-04 | (consolidation prevents conflicts) | — |
| EPIC-05 | S-3, S-2, S-1, GP-1, GP-2, GP-3 | ALTO/ALTO/ALTO/**CRÍTICO**/**CRÍTICO**/**CRÍTICO** |
| EPIC-06 | UX-2.1 | **CRÍTICO** |
| EPIC-11 | DOC-2.1, DOC-3.1, DOC-4.1, DOC-5.1 | **CRÍTICO** / ALTO / ALTO / ALTO |
| EPIC-12 | S-S14 | **CRÍTICO** (requires device verification) |

---

## Implementation Notes

### Root Cause Themes

1. **Incomplete migration** — ARC-1.1: Legacy and modern pipelines coexist
2. **Missing lifecycle hooks** — MPR-6.1, S-S10: No cleanup on service death
3. **Single-platform design** — SCA-11.x: Architecture assumed Uber-only
4. **Parallel decision systems** — DUP-10.6: Two engines with conflicting thresholds
5. **Debug infrastructure in release** — S-3: Debug panel available to end users
6. **No localization strategy** — UX-12.1: All strings hardcoded in Spanish
7. **No testing strategy for presentation layer** — H-QA-01: Zero ViewModel/UI tests

### Risk Mitigation

- **EPIC-01 and EPIC-02** must be completed together (pipeline unification + lifecycle safety are tightly coupled)
- **EPIC-12 (S-S14)** requires immediate device verification — this finding alone could invalidate the entire capture architecture
- **EPIC-05 (GP-1)** requires immediate correction of the Data Safety declaration before any Beta distribution
- All CRÍTICO findings must be resolved before Beta; ALTO findings should also be resolved before Beta per the Executive Summary

### Dependencies Summary

```
EPIC-01 → EPIC-02 (lifecycle cleanup on unified pipeline)
EPIC-01 → EPIC-03 (detection framework wiring)
EPIC-01 → EPIC-04 (consolidate thresholds within unified pipeline)
EPIC-01 → EPIC-09 (performance optimization on unified pipeline)
EPIC-01 + EPIC-02 → EPIC-12 (lifecycle hardening)
EPIC-04 → EPIC-07 (presentation layer refactoring after decision engine)
EPIC-07 → EPIC-10 (testability after ViewModel refactoring)
EPIC-07 → EPIC-08 (design system after presentation layer refactor)
EPIC-08 → EPIC-06 (drive-safe UX after design system)
```

*End of REMEDIATION_PLAN.md*

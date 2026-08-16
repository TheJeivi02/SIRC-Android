# SUCCESS METRICS

This document defines objective metrics to validate the completion of each Epic in the SIRC Remediation Plan. A metric must be measurable and verifiable.

---

## Metric Framework

| Metric Type | Definition | Validation Method |
|---|---|---|
| **Pass/Fail** | Binary: requirement met or not met | Code inspection, test result, lint check |
| **Threshold** | Value must be within a defined range | Benchmark, profiler, tool output |
| **Count** | Exact number of items must match | Grep, code analysis |

---

## Epic Success Metrics

### EPIC-01 — Unify & Optimize Capture Pipeline

| Metric | Target | Type | Validation |
|---|---|---|---|
| Single AccessibilityService declared | 1 service in manifest | Count | `adb shell dumpsys accessibility \| grep SIRC` |
| No duplicate history entries | 0 duplicates in 10 captures | Count | Integration test: capture 10 offers, check DB |
| Legacy pipeline code removed | 0 references to `SircAccessibilityService` in production | Count | Grep `SircAccessibilityService` in `src/main` |
| `hasAccessibilityPermission()` checks correct service | 1 service checked | Pass/Fail | Code inspection |
| FeatureFlag.PARSER honored in production path | Pipeline respects all flags | Pass/Fail | Unit test coverage |
| No dual text collection | Single `collectTexts()` method | Count | Grep in production code |

### EPIC-02 — MediaProjection Lifecycle Safety

| Metric | Target | Type | Validation |
|---|---|---|---|
| `onDestroy()` present in MediaProjectionService | ✓ | Pass/Fail | Code inspection |
| No projection leak after process kill + Doze | 0 leaked VirtualDisplay | Pass/Fail | Device test + `dumpsys media.projection` |
| `getMediaProjection()` wrapped in try/catch | ✓ | Pass/Fail | Code inspection |
| Single image closure policy | No double-close, no leak | Pass/Fail | Unit test + device test |
| PNG round-trip eliminated | 0 PNG compress/encode in capture path | Count | Grep for `compress()` in media path |
| `isProjecting` state accurate after kill | Matches system state | Pass/Fail | Device test after Doze |

### EPIC-03 — Platform-Agnostic Detection Framework

| Metric | Target | Type | Validation |
|---|---|---|---|
| Platform-specific detection rules | ≥4 platforms with keywords | Count | Test: Uber, DiDi, Cabify, InDrive |
| No `if (platform == UBER)` hardcoding | 0 instances | Count | Grep in production code |
| `BaseOfferTypeParser` has no default platform | Constructor requires platform | Pass/Fail | Code inspection |
| Lyft/Bolt added to enum | 2 new entries | Count | `RidePlatform.entries` |
| Currency from driver profile, not platform | 0 `DEFAULT_CURRENCY` map | Count | Grep in production |
| Multi-platform tests | ≥20 new test cases | Count | Test report |

### EPIC-04 — Consolidate Decision Engine

| Metric | Target | Type | Validation |
|---|---|---|---|
| No orphaned use-cases | 0 references to `EvaluateOfferUseCase` | Count | Grep in production code |
| Single threshold source | `RuleThresholds` derived from `DecisionThresholds` | Pass/Fail | Code inspection |
| `FakeParser` in test sources only | 0 in `src/main` | Count | Grep + file path check |
| `isConfigured()` matches domain definition | All integration tests pass | Pass/Fail | Test suite |
| No double persistence | 1 history entry per offer | Count | Integration test |
| Decision verdict consistency | 100% match between engines | Threshold | Before/after comparison |

### EPIC-05 — Security, Privacy & Data Protection

| Metric | Target | Type | Validation |
|---|---|---|---|
| Database encrypted (SQLCipher) | ✓ | Pass/Fail | Query `PRAGMA cipher_version` |
| `allowBackup` secured | `false` or `dataExtractionRules` present | Pass/Fail | Manifest inspection |
| Debug panel absent in release | 0 Debug routes in release nav | Count | `assembleRelease` + code inspection |
| PII minimized in DB | No `name`/`city`/`vehicle` unless required | Count | Entity schema review |
| Data Safety form corrected | GP-1 text updated | Pass/Fail | Document review |
| Privacy policy published | File exists at `docs/PRIVACY_POLICY.md` | Pass/Fail | File existence |
| Prominent disclosure dialog | Present before accessibility activation | Pass/Fail | Manual walkthrough |
| No PII in debug logs | 0 PII patterns in log output | Count | Grep logcat output |

### EPIC-06 — Drive-Safe UX & Accessibility

| Metric | Target | Type | Validation |
|---|---|---|---|
| ProfitIndicator contrast ≥4.5:1 | All 3 states ≥4.5:1 | Threshold | Color contrast analyzer |
| Overlay text ≥14sp | 0 instances <14sp in overlay | Count | Grep + manual verification |
| TalkBack reads decision | Announced on new offer | Pass/Fail | Device test with TalkBack |
| Interactive touch targets ≥48dp | 0 targets <48dp | Count | Layout inspection |
| Delete confirmation dialog | AlertDialog on delete | Pass/Fail | UI walkthrough |
| Currency field validated | ISO 4217 only | Pass/Fail | Test: invalid code rejected |
| Accessibility semantics | `contentDescription` on all icons | Count | Grep count / lint check |
| Navigation transitions | All screens have transitions | Count | Visual walkthrough |
| Reduce motion respected | Animations disabled with ` animator.duration_scale=0` | Pass/Fail | Developer option test |

### EPIC-07 — Presentation Layer Architecture

| Metric | Target | Type | Validation |
|---|---|---|---|
| God-class decomposition | `PipelineOverlayDataSource` <350 lines | Threshold | Line count |
| DebugPanelViewModel scoped to route | Not in NavHost root | Pass/Fail | Code inspection |
| `SavedStateHandle` on ViewModels | ≥8 ViewModels use it | Count | Grep |
| One-shot events via Channel/SharedFlow | `saved` state removed | Count | Grep for `UiState.saved` |
| `OverlayService` decomposed | Separate `OverlayWindowController` | Pass/Fail | Code structure |
| `OnboardingViewModel.save()` error handling | try/finally present | Pass/Fail | Code inspection |

### EPIC-08 — Localization & Design System Unification

| Metric | Target | Type | Validation |
|---|---|---|---|
| No hardcoded strings in .kt | 0 (excluding debug/test) | Count | Grep `"` in production .kt files |
| `lightColorScheme` complete | 10+ properties defined | Count | Code inspection |
| `dynamicColor` supported | Conditional in Theme.kt | Pass/Fail | Code inspection |
| `SircTypography` complete | ≥10 styles defined | Count | Code inspection |
| SircSpacing adopted | ≥5 screens use tokens | Count | Grep usage |
| Consistent formatting | 1 formatter per currency type | Count | Grep `formatMoney` |

### EPIC-09 — Performance Optimization

| Metric | Target | Type | Validation |
|---|---|---|---|
| OCR latency (avg) | <500ms | Threshold | Benchmark on test images |
| OCR gating effectiveness | ≥60% reduction in OCR calls | Threshold | Pipeline metrics before/after |
| Memory under sustained use | <20MB growth over 50 captures | Threshold | Memory profiler |
| PNG encode eliminated from capture path | 0 occurrences | Count | Grep in capture path |
| Duplicate processing eliminated | 1 evaluation per offer | Count | Integration test |
| DebugPanel not collecting in background | 0 background collections | Count | Profiler trace |
| Limited parallelism for OCR | `limitedParallelism(N)` present | Pass/Fail | Code inspection |

### EPIC-10 — Testing Infrastructure & Coverage

| Metric | Target | Type | Validation |
|---|---|---|---|
| ViewModel test files | ≥5 files | Count | Glob test files |
| ViewModel test cases | ≥30 total | Count | Test report |
| OCR engine tests | ≥10 cases | Count | Test report |
| Instrumented service tests | ≥5 cases | Count | Test report |
| `kotlinx-coroutines-test` in test deps | ✓ in all modules | Pass/Fail | Dependency check |
| `MockK` or `Robolectric` used | ≥3 test files | Count | Grep imports |
| Division-by-zero tests | ≥5 cases | Count | Test report |
| Multi-currency tests | ≥5 currencies | Count | Test report |
| Migration tests | ≥3 test cases | Count | Test report |

### EPIC-11 — Documentation Integrity

| Metric | Target | Type | Validation |
|---|---|---|---|
| Module count consistent | 11 in all docs | Count | Grep "modules" in all .md |
| Version reference consistent | v1.0.0-rc1 | Pass/Fail | Grep version strings |
| Pipeline status accurate | 2 pipelines stated | Count | ARCHITECTURE.md review |
| CHANGELOG complete | v0.5.0 header present | Pass/Fail | CHANGELOG inspection |
| ProfitEngine formula documented | ✓ in KDoc | Pass/Fail | Code inspection |
| KDoc coverage ≥80% | 14/14 in :data, 5/5 in settings/onboarding | Threshold | KDoc count |
| Privacy policy linked | In GOOGLE_PLAY_COMPLIANCE.md | Pass/Fail | Grep |

### EPIC-12 — Stability & Lifecycle Hardening

| Metric | Target | Type | Validation |
|---|---|---|---|
| No crash on rotation | 0 crashes in 20 rotations | Count | Manual test |
| Process death recovery | Overlay+capture state restored | Pass/Fail | Device test |
| Split-screen correct position | 0 position errors | Count | Device test |
| Overlay doesn't capture itself | 0 SIRC text in OCR output | Count | Test capture with visible overlay |
| API 24-25 overlay fallback | Fallback path present | Pass/Fail | Code inspection |
| `isRunning` synced with service | Matches actual state | Pass/Fail | State inspection |
| Overlay permission revocation detected | Service stops correctly | Pass/Fail | Device test |
| No ANR in accessibility | 0 ANRs in 1000 events | Count | Stress test |
| `SavedStateHandle` tests | ≥3 ViewModels tested | Count | Test report |

---

## Overall Beta Readiness Metrics

| Metric | Target | Type | Validation |
|---|---|---|---|
| Critical findings resolved | 6/6 | Count | Audit cross-check |
| High findings resolved before Beta | ≥35/38 | Count | Audit cross-check |
| Total findings resolved before Beta | ≥68/69 | Count | PRE_BETA_BACKLOG cross-check |
| All tests passing | 100% | Threshold | CI green |
| No crashes in 100 test sessions | 0 | Count | Beta test report |
| Memory stable under sustained use | <10% growth | Threshold | Profiler over 10 min |
| WCAG AA compliant | ✓ | Pass/Fail | Accessibility audit |
| Google Play compliant | ✓ | Pass/Fail | Compliance checklist |
| Documentation accurate | 12/13 P1/P0 docs fixed | Count | Doc review |
| Device verification complete | All S-S14, S-S10, S-S04 verified | Count | Device test log |

---

## Phase Gate Metrics

### RC2 Gate (End of Sprint 4)
| Metric | Target |
|---|---|
| CRÍTICO findings resolved | 6/6 |
| EPIC-01, EPIC-02, EPIC-03, EPIC-04, EPIC-11 complete | ✓ |
| CI green (all unit tests) | ✓ |
| Device verification of S-S14 | Complete |
| CHANGELOG v0.5.0 restored | ✓ |
| Documentation module count fixed | 11 modules in all docs |

### Beta Gate (End of Sprint 5)
| Metric | Target |
|---|---|
| All CRÍTICO + all ALTO findings resolved | ✓ |
| EPIC-05, EPIC-06, EPIC-07, EPIC-08 complete | ✓ |
| WCAG AA compliant (contrast, text, a11y) | ✓ |
| Data Safety corrected | ✓ |
| Privacy policy published | ✓ |
| Prominent disclosure in-app | ✓ |
| ViewModel tests exist | ≥3 ViewModels |
| No crashes in 50 test sessions | 0 |

### Final Release Gate (End of Sprint 7)
| Metric | Target |
|---|---|
| All 191 findings addressed | 191/191 |
| Performance baseline met | OCR <500ms, memory stable |
| All tests passing + instrumented | 100% CI green |
| No dead code in production | 0 references |
| Documentation complete | 13/13 docs accurate |

*End of SUCCESS_METRICS.md*

# SPRINT PLAN

**Project:** SIRC v1.0.0-rc1 Remediation  
**Sprint Duration:** 2 weeks (10 working days)  
**Team Composition:** 3 Android Engineers, 1 Domain Engineer, 1 QA Engineer, 1 UX Designer  
**Total Sprints:** 7  

---

## Sprint Allocation

| Sprint | Objective | Epics | Duration | Team Effort |
|---|---|---|---|---|
| Sprint 1 | Capture Unification & Critical Blockers | EPIC-01 (P0), EPIC-05 (critical), EPIC-11 (DOC-2.1) | 2 weeks | 13 días |
| Sprint 2 | Security Hardening & UX Fixes | EPIC-05 (rest), EPIC-06 (critical), EPIC-01 (complete) | 2 weeks | 10 días |
| Sprint 3 | Lifecycle Safety & Platform Detection | EPIC-02, EPIC-03 (partial), EPIC-04 (partial) | 2 weeks | 14 días |
| Sprint 4 | Decision Engine & Architecture Cleanup | EPIC-04 (complete), EPIC-11 (complete), EPIC-02 (complete) | 2 weeks | 13 días |
| Sprint 5 | Testing Infrastructure & Design System | EPIC-10 (partial), EPIC-07, EPIC-08 (partial) | 2 weeks | 15 días |
| Sprint 6 | UX Completion & Performance | EPIC-06 (complete), EPIC-08 (complete), EPIC-09 (partial) | 2 weeks | 12 días |
| Sprint 7 | Optimization & Cleanup | EPIC-09 (complete), EPIC-12, EPIC-11 (P3) | 2 weeks | 11 días |

---

## Sprint 1 — Capture Unification & Critical Blockers

### Objective
Unify the capture pipeline to a single AccessibilityService and resolve all CRÍTICO blockers that prevent Beta consideration.

### Epics
- EPIC-01 (partial): Remove legacy pipeline, single service
- EPIC-05 (critical): Data Safety, Debug panel gating, Privacy policy, allowBackup
- EPIC-11 (DOC-2.1): FIX CHANGELOG v0.5.0 header

### Dependencies
- None — this is the first sprint

### Risks
- **Device Verification Required:** S-S14 (`exported="false"` on AccessibilityService) must be tested on physical devices immediately. If it fails, the entire capture architecture must be redesigned.
- **Pipeline Regression:** Removing the legacy pipeline could break capture if the modern pipeline has gaps.

### Expected Outcome
- Single `CaptureAccessibilityService` active and receiving events
- Legacy pipeline code removed (or marked with compile-time flag)
- Debug panel absent from release builds
- Data Safety declaration corrected
- Privacy policy published
- CHANGELOG v0.5.0 header restored

### Acceptance Criteria
1. `adb shell dumpsys accessibility` shows only one SIRC service
2. Release build has no accessible Debug screen
3. Data Safety form text corrected in `docs/GOOGLE_PLAY_COMPLIANCE.md`
4. Privacy policy published at `docs/PRIVACY_POLICY.md`
5. CHANGELOG has v0.5.0 header with Sprint 4 content
6. No duplicate entries in offer history after 5 consecutive captures

### Verification
- Device test: activate single service, verify capture works
- Build variant: assembleRelease + check Debug not in nav
- Manual: review Data Safety text against audit findings
- Manual: review Privacy Policy content
- File inspection: CHANGELOG headers
- Integration test: capture 5 offers, verify no duplicates

---

## Sprint 2 — Security Hardening & UX Criticals

### Objective
Complete security hardening, fix the CRÍTICO UX contrast issue, and finish pipeline unification.

### Epics
- EPIC-05 (complete): SQLCipher, S-1, S-2, S-3, S-4, S-5, GP-1..GP-3
- EPIC-06 (critical): UX-2.1 (ProfitIndicator contrast)

### Dependencies
- Sprint 1 complete (pipeline unified, debug panel gated)

### Risks
- SQLCipher native library size and 16KB alignment
- Contrast change may affect overlay visual balance
- Debug panel removal may impact debugging workflow

### Expected Outcome
- Room database encrypted with SQLCipher
- `allowBackup="false"` or `dataExtractionRules` in place
- Debug panel fully gated behind `BuildConfig.DEBUG`
- Memory retention of captured text reduced
- ProfitIndicator contrast ≥4.5:1 on all states
- Prominent disclosure dialog in onboarding

### Acceptance Criteria
1. Database encryption verified (SQLCipher)
2. `adb backup` does not include `sirc.db`
3. No Debug tab visible in release build variant
4. ProfitIndicator contrast measured ≥4.5:1 for all 3 states
5. Prominent disclosure dialog appears before accessibility activation
6. No PII leaked in debug logs

### Verification
- Code inspection: SQLCipher integration
- Device test: backup protection
- Screenshot: release build nav (no Debug)
- Color tool: contrast measurements
- Manual: walkthrough disclosure dialog
- Log inspection: no PII in debug output

---

## Sprint 3 — Lifecycle Safety & Platform Detection

### Objective
Fix MediaProjection lifecycle leaks and establish the platform-agnostic detection framework.

### Epics
- EPIC-02: MediaProjection Lifecycle Safety (all findings)
- EPIC-03 (partial): Platform detection rules, parser framework

### Dependencies
- Sprint 1 complete (single pipeline in place)
- Device verification of S-S14 (if failed, architecture redesign needed)

### Risks
- `onDestroy` may not be called in all termination scenarios
- Platform detection changes could break existing Uber parsing
- Thread safety in image resource cleanup

### Expected Outcome
- `MediaProjectionService.onDestroy()` properly releases all resources
- No projection leaks after process kill or Doze
- Single source of truth for image closure in channels
- Platform-specific detection rules defined for Uber, DiDi, Cabify, InDrive
- Parser selection no longer hardcoded to `if (platform == UBER)`

### Acceptance Criteria
1. `onDestroy` override present in `MediaProjectionService`
2. Process kill + Doze → no leaked projection, overlay reports correct state
3. `acquireLatestImage()` replaced with safe image handling
4. PNG round-trip eliminated (pass Bitmap directly to OCR)
5. Detection tests pass for all 4 existing platforms
6. `BaseOfferTypeParser` no longer defaults to Uber extractor

### Verification
- Code inspection: lifecycle methods
- Device test: kill/Doze simulation
- Unit tests: image closure safety
- Benchmark: OCR latency improvement
- Test suite: platform detection tests

---

## Sprint 4 — Decision Engine Consolidation & Architecture Cleanup

### Objective
Eliminate the dual decision engine, remove dead code, and complete all documentation fixes.

### Epics
- EPIC-04: Consolidate Decision Engine (all findings)
- EPIC-11: Documentation Integrity (all P1 items)

### Dependencies
- Sprint 1-3 complete (pipeline unified, lifecycle safe)

### Risks
- Removing dead use-cases could expose hidden dependencies
- Changing decision thresholds could affect user experience
- Documentation changes must match actual code state

### Expected Outcome
- Single decision authority (RuleEngine or ProfitEngine, not both)
- `EvaluateOfferUseCase` and `AddOfferHistoryUseCase` removed
- `isConfigured()` respects domain definition
- `FakeParser` moved to test sources only
- All documentation matches reality (11 modules, current version, accurate pipeline status)
- ProfitEngine formula documented in KDoc

### Acceptance Criteria
1. No orphaned use-cases in production code
2. Single threshold source (`RuleThresholds` derived from `DecisionThresholds`)
3. `isConfigured()` test passes with domain definition
4. `FakeParser` only in `src/test` or `src/debug`
5. README/AGENTS/ROADMAP show 11 modules
6. PROJECT.md references v1.0.0-rc1
7. ARCHITECTURE.md states 2 pipelines accurately (or references audit)
8. ProfitEngine KDoc contains formula

### Verification
- Grep: no references to removed use-cases
- Unit tests: threshold consistency
- Code inspection: FakeParser location
- Documentation review: all 3 module count references
- KDoc inspection: formula present

---

## Sprint 5 — Testing Infrastructure & Design System Foundation

### Objective
Establish testing infrastructure for ViewModels and Compose UI, begin design system unification.

### Epics
- EPIC-10 (partial): ViewModel tests, OCR engine tests, test infrastructure
- EPIC-07 (partial): ViewModel refactoring for testability
- EPIC-08 (partial): Begin string extraction, design tokens

### Dependencies
- Sprint 4 complete (decision engine consolidated, presentation layer ready for testing)

### Risks
- Adding test infrastructure takes time from feature work
- String extraction must be thorough to avoid missing strings

### Expected Outcome
- `kotlinx-coroutines-test` and `MockK`/`Robolectric` dependencies added
- `SavedStateHandle` added to ViewModels
- One-shot events implemented (Channel/SharedFlow)
- Tests for at least 2 ViewModels
- Test infrastructure for instrumented tests
- 50% of hardcoded strings extracted to resources
- `SircTypography` expanded (complete 10 styles)

### Acceptance Criteria
1. Test dependencies declared in module build files
2. `SavedStateHandle` in `OnboardingViewModel`, `SettingsViewModel`
3. Snackbar events use `Channel` instead of state
4. 2 ViewModel test files exist with >10 test cases total
5. `MockK`/`Robolectric` in test classpath
6. 50%+ of hardcoded strings in root-level module extracted
7. Typography covers all used styles

### Verification
- Build: test dependencies compile
- Unit tests pass
- Code inspection: SavedStateHandle usage
- Grep: hardcoded strings count before/after
- Visual: typography consistency check

---

## Sprint 6 — UX Completion & Google Play Compliance

### Objective
Complete all UX and accessibility fixes, finalize Google Play compliance.

### Epics
- EPIC-06 (complete): All UX findings
- EPIC-08 (complete): Full localization setup
- EPIC-09 (partial): Core performance optimizations
- EPIC-05 (completion): Final security hardening, GP justification

### Dependencies
- Sprints 1-5 complete

### Risks
- UX changes require careful validation in driving context
- Localization may introduce layout issues

### Expected Outcome
- WCAG AA compliant overlay
- All strings extracted
- TalkBack support for overlay decision
- Touch targets ≥48dp
- Navigation transitions in place
- `dynamicColor` support (Android 12+)
- Theme toggle in Settings
- Google Play FGS justification text prepared
- OCR gated with pre-detection

### Acceptance Criteria
1. All `ProfitIndicator` states ≥4.5:1 contrast
2. TalkBack reads decision when overlay appears
3. All interactive elements ≥48dp touch target
4. Zero hardcoded strings in `.kt` files
5. Theme toggle works (System/Claro/Oscuro)
6. FGS subtypes have English justification text
7. OCR only runs when platform detected

### Verification
- Accessibility tool: contrast check
- Device test: TalkBack walkthrough
- Layout inspection: touch target sizes
- Grep: no hardcoded strings in .kt
- Device test: theme toggling
- Play Console: FGS form text
- Benchmark: OCR gating effectiveness

---

## Sprint 7 — Optimization & Final Cleanup

### Objective
Complete performance optimization, remove remaining dead code, finalize all documentation.

### Epics
- EPIC-09 (complete): All performance findings
- EPIC-12 (remaining): Final stability items
- EPIC-11 (P3): Final documentation polish
- EPIC-04 (P2/P3): Remaining dead code cleanup

### Dependencies
- Sprints 1-6 complete

### Risks
- Performance changes near release could introduce instability
- Scope creep into v1.1 features

### Expected Outcome
- OCR latency <500ms average
- Memory stable under sustained use
- No dead code in production
- All documentation P3 items addressed
- `fallbackToDestructiveMigration` strategy in place

### Acceptance Criteria
1. Average OCR latency <500ms (benchmark)
2. No memory growth over 100 captures
3. Zero dead code references in production (grep verified)
4. CHANGELOG follows Keep a Changelog format
5. DECISIONS.md has revision date
6. ROADMAP updated to 11 modules
7. All KDoc gaps in data/settings/onboarding filled

### Verification
- Performance benchmark
- Memory profiler over extended session
- Grep: no dead code identifiers
- Documentation review: format consistency
- KDoc coverage: all modules >80%

---

## Cross-Sprint Concerns

### Device Verification (S-S14)
- **When:** Must be done by end of Sprint 1
- **Who:** Lead Android Engineer
- **Impact:** If S-S14 fails, EPIC-01 must be redesigned (can't remove legacy pipeline)

### Security Compliance (GP-1, GP-2, GP-3)
- **When:** Must be complete by end of Sprint 2
- **Who:** Security Engineer + Legal/Compliance
- **Impact:** Cannot enter closed Beta without accurate Data Safety declaration

### Test Stability
- **When:** Throughout, but especially Sprints 3-5
- **Who:** QA Engineer
- **Impact:** All fixes must be regression-tested before sprint completion

### Performance Baseline
- **When:** Sprint 4 (baseline) and Sprint 7 (final verification)
- **Who:** Performance Engineer
- **Impact:** Must demonstrate improvement from Sprint 1 baseline

*End of SPRINT_PLAN.md*

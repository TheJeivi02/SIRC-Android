# IMPLEMENTATION ROADMAP

**Project:** SIRC v1.0.0-rc1  
**Total Epics:** 12  
**Total Estimated Effort:** 136 developer-days  
**Target:** RC2 / Beta Ready  

---

## Phase Overview

| Phase | Name | Epics | Duration | Focus |
|---|---|---|---|---|
| Phase 1 | Critical Blockers | EPIC-01, EPIC-05, EPIC-06, DOC fixes | 4 weeks (Sprint 1-2) | Beta blockers |
| Phase 2 | Architecture Remediation | EPIC-02, EPIC-03, EPIC-04, EPIC-11 | 4 weeks (Sprint 3-4) | Core quality |
| Phase 3 | Quality & Stability | EPIC-12, EPIC-09, EPIC-10 (partial) | 4 weeks (Sprint 4-5) | Robustness |
| Phase 4 | UX & Accessibility | EPIC-08, EPIC-06 (continue), EPIC-10 | 3 weeks (Sprint 5-6) | Product polish |
| Phase 5 | Google Play Compliance | EPIC-05 completion, GP fixes, privacy policy | 2 weeks (Sprint 6) | Release compliance |
| Phase 6 | Optimization | EPIC-09 completion, EPIC-12 completion | 2 weeks (Sprint 6-7) | Performance |
| Phase 7 | Repository Cleanup | EPIC-12 (dead code), EPIC-11 (final docs) | 1 week (Sprint 7) | Technical debt |

---

## Phase 1 — Critical Blockers (Sprints 1-2)

### Goal
Resolve all 6 CRÍTICO findings and all ALTO/ CRÍTICO Security & Google Play findings to unblock Beta consideration.

### Epics in Scope
- **EPIC-01:** Unify & Optimize Capture Pipeline (P0 portions)
  - Remove legacy `SircAccessibilityService`, `AccessibilityWindowObserver`, `OfferCaptureCoordinator`
  - Unify in `DefaultCapturePipeline` / `CaptureAccessibilityService`
  - Eliminate `collectTexts()` duplication (DUP-10.1)
  - Fix `hasAccessibilityPermission()` to check correct service (ACC-7.5)
- **EPIC-05:** Security, Privacy & Data Protection (CRÍTICO/ALTO)
  - Fix Data Safety declaration (GP-1)
  - Draft and publish Privacy Policy (GP-2)
  - Add prominent disclosure in-app (GP-3)
  - Gate debug panel to `BuildConfig.DEBUG` (S-3)
  - Fix `allowBackup` / add `dataExtractionRules` (S-2)
  - Begin SQLCipher migration (S-1)
- **EPIC-06:** Drive-Safe UX (CRÍTICO)
  - Fix `ProfitIndicator` contrast — darken backgrounds (UX-2.1)
  - Begin extraction of hardcoded strings (UX-12.1)

### Deliverables
- Single AccessibilityService active
- Debug panel absent in release builds
- Data Safety form corrected
- Privacy policy published
- ProfitIndicator passes WCAG AA contrast
- CHANGELOG v0.5.0 header restored (DOC-2.1)

### Risk
High — changing the capture pipeline is the most critical path. Must maintain capture functionality throughout.

### Dependency on Device Verification
**S-S14** (`exported="false"` on AccessibilityService) requires immediate device testing. This finding alone could invalidate the entire capture architecture.

---

## Phase 2 — Architecture Remediation (Sprints 3-4)

### Goal
Remove dead code, establish unified decision engine, fix architectural inconsistencies.

### Epics in Scope
- **EPIC-02:** MediaProjection Lifecycle Safety
  - Add `onDestroy()` to `MediaProjectionService`
  - Fix CONFLATED channel + `acquireLatestImage()` (MPR-6.2)
  - Remove PNG round-trip (MPR-6.3)
- **EPIC-03:** Platform-Agnostic Detection Framework
  - Platform-specific detection rules (SCA-11.1)
  - Remove `if (platform == UBER)` hardcoding (SCA-11.2)
  - Fix parser base class default (SCA-11.3)
  - Add Lyft/Bolt to enum (SCA-11.4)
- **EPIC-04:** Consolidate Decision Engine
  - Eliminate `EvaluateOfferUseCase`, `AddOfferHistoryUseCase` (DEAD-9.1, DEAD-9.2)
  - Unify thresholds (DUP-10.6)
  - Fix `isConfigured()` (ARC-1.3)
  - Remove `FakeParser` from production (ABS-8.2)
- **EPIC-11:** Documentation Integrity (P1 items)
  - Fix module counts (DOC-3.1)
  - Fix PROJECT.md version (DOC-4.1)
  - Fix ARCHITECTURE.md legacy claim (DOC-5.1)
  - Document ProfitEngine formula (DOC-9.4)

### Deliverables
- No MediaProjection resource leaks
- Multi-platform detection ready
- Single decision engine with unified thresholds
- No dead code in production
- Documentation accurate

### Risk
Medium-High — architectural changes could affect the entire capture flow.

### Dependencies
- EPIC-01 must be complete (single pipeline to modify)
- Device verification of S-S14 completed in Phase 1

---

## Phase 3 — Quality & Stability (Sprints 4-5)

### Goal
Establish robustness across edge cases, lifecycle events, and performance baselines.

### Epics in Scope
- **EPIC-12:** Stability & Lifecycle Hardening
  - Fix `START_NOT_STICKY` in MediaProjectionService (S-S10)
  - Fix overlay self-capture feedback (S-S04)
  - Add `onMultiWindowModeChanged` (S-S03)
  - Fix clamp with `WRAP_CONTENT` (S-S05)
  - Memory optimization for large screens (S-S06, MPR-6.5)
  - Try/catch around `getMediaProjection` (S-S20)
  - API 24-25 fallback for overlay (S-S22)
  - Sync `isRunning` with service (S-S23, FGS-8.3)
  - Detect overlay permission revocation (S-S24, FGS-8.4)
  - Add `SavedStateHandle` to ViewModels (VM-9.1)
  - Fix one-shot events (VM-9.2)
  - Process death state management (LIF-3.2)
  - Fix collectors without try/catch (FLW-4.1)
  - Fix `snapshotInFlight` discard (FLW-4.2)
  - Fix snapshot SharedFlow buffer (FLW-4.3)
  - Limit OCR parallelism (CO-5.1)
  - Cancel ML Kit tasks (CO-5.2)
  - Add WorkManager for history trim (WKM-10.1)
  - Fix pipeline pause (BRT-12.1)
  - Add `fallbackToDestructiveMigration` safety (S-S28)
- **EPIC-09:** Performance Optimization (partial)
  - Gate OCR with pre-detection (P-P01)
  - Add downscaling/crop to ROI (P-P02)
  - Reduce VirtualDisplay refresh rate (P-P05)
  - Fix overlay collection when hidden (CMP-1.1)
  - Scope DebugPanel to its route (CMP-1.3)
  - Limit OCR parallelism (CO-5.1)

### Deliverables
- No lifecycle-related crashes
- Proper resource cleanup on all paths
- Performance baselines established
- Memory usage reduced
- Overlay doesn't capture itself

### Risk
Medium — lifecycle changes are device-specific and hard to test exhaustively in CI.

### Dependencies
- EPIC-01, EPIC-02 complete

---

## Phase 4 — UX & Accessibility (Sprints 5-6)

### Goal
Achieve WCAG AA compliance and drive-safe UX; unify design system.

### Epics in Scope
- **EPIC-08:** Localization & Design System Unification
  - Complete `lightColorScheme` (UX-1.1)
  - Add `dynamicColor` + theme toggle (UX-1.2)
  - Complete typography (UX-1.3)
  - Extract all hardcoded strings (UX-12.1)
  - Fix currency formatting (UX-6.1, S-S08)
  - Adopt `SircSpacing` tokens (UX-8.1)
  - Fix typography hardcodes (UX-9.1)
- **EPIC-06:** Drive-Safe UX (continue)
  - TalkBack support for overlay (UX-2.2)
  - Increase overlay text sizes (UX-2.3)
  - Fix opacity/visibility controls (UX-2.4)
  - Add delete confirmation (UX-4.1)
  - Fix touch targets (UX-11.2, UX-2.5)
  - Add accessibility semantics (UX-11.1)
  - Fix onboarding feedback (UX-5.1, UX-5.2, UX-5.3)
  - Fix settings feedback (UX-6.2)
  - Fix home dashboard (UX-3.1)
  - Fix StatusDot contrast (UX-3.2)
  - Fix charts for daltonism (UX-7.2)

### Deliverables
- WCAG AA compliant (contrast, text size, semantics)
- All strings localized-ready (extracted to resources)
- Complete design system tokens adopted
- Drive-safe overlay with proper sizing
- Confirmation dialogs for destructive actions

### Risk
Medium — UX changes must be validated with real users driving.

### Dependencies
- EPIC-08 depends on EPIC-07 (presentation layer refactor) for clean formatting
- EPIC-06 depends on EPIC-08 (design system for consistent colors/typography)

---

## Phase 5 — Google Play Compliance & Security Completion (Sprint 6)

### Goal
Complete all remaining compliance items for Play Store Beta approval.

### Epics in Scope
- **EPIC-05:** Security completion
  - Complete SQLCipher migration (S-1)
  - Minimize PII in database (S-1)
  - Clean up memory retention (S-4, S-5)
  - Fix export for sharing intents (S-3)
- **EPIC-01:** Google Play completion
  - Justify `specialUse` FGS in Play Console (GP-6)
  - Address overlay size concern (GP-7)
  - Fix notification timeout (GP-8)

### Deliverables
- DB encrypted with SQLCipher
- All PII minimized or properly protected
- Data Safety form 100% accurate
- Privacy policy published and linked
- Prominent disclosure in-app
- Single AccessibilityService (not two redundant ones)
- Play Console FGS justification ready

### Risk
Low — compliance and hardening only.

### Dependencies
- EPIC-01 Phase 1 (single service)
- EPIC-05 Phase 1 (debug panel gating, Data Safety)

---

## Phase 6 — Optimization & Performance Completion (Sprint 6-7)

### Goal
Complete all performance optimizations and establish performance budgets.

### Epics in Scope
- **EPIC-09:** Performance Optimization completion
  - Pass Bitmap directly to OCR, eliminate PNG round-trip (P-P03)
  - Fix bitmap corruption (P-P14)
  - Fix frame closure under load (P-P04)
  - Fix snapshot coordination (P-P09)
  - Optimize text normalization (P-P10, P-P11)
  - Add `derivedStateOf` (P-P13)
  - Improve frame cache hashing (P-P15, P-P16)
  - Limit coroutine parallelism (CO-5.1)
  - Fix DebugPanel memory (P-P12)

### Deliverables
- OCR latency <500ms average
- Memory usage stable under sustained use
- No duplicate processing
- Battery impact reduced by 40%+

### Risk
Medium — performance changes could affect OCR accuracy.

### Dependencies
- All capture pipeline epics complete

---

## Phase 7 — Repository Cleanup (Sprint 7)

### Goal
Remove remaining dead code, fix minor issues, finalize documentation.

### Epics in Scope
- **EPIC-04:** Architecture cleanup (P2/P3 items)
  - Remove `AddOfferHistoryUseCase` dead code (DEAD-9.2)
  - Clean dead DriverConfig methods (DEAD-9.3)
  - Clean other dead elements (DEAD-9.4)
  - Fix `core:ui → domain` dependency (MOD-3.4)
- **EPIC-12:** Final stability
  - Add `onMultiWindowModeChanged` (S-S03)
  - Fix notification translation (S-S09)
  - Add BOOT_COMPLETED option (S-S11)
  - Process death state persistence (S-S12)
  - MediaProjection token management (S-S13)
  - Accessibility disconnect retry (S-S18)
- **EPIC-11:** Documentation final
  - Fix CHANGELOG format (DOC-2.2)
  - Add audit references in ARCHITECTURE.md (DOC-5.2)
  - Add DECISIONS.md revision date (DOC-7.1)
  - Fix ROADMAP module count (DOC-8.1)
  - Add KDoc to settings/onboarding/data (DOC-9.1, DOC-9.2, DOC-9.3)
  - Add test KDoc (DOC-9.6)

### Deliverables
- No dead code in production
- Complete documentation
- All module dependencies clean
- Repository ready for v1.1 development

### Risk
Low — cleanup and polishing only.

### Dependencies
- All other epics complete

---

## Sprint-to-Epic Mapping

| Sprint | Phase | Epics | Primary Focus | Beta Ready? |
|---|---|---|---|---|
| Sprint 1 | Phase 1 | EPIC-01 (partial), EPIC-05 (critical), EPIC-06 (critical), EPIC-11 (DOC-2.1) | Pipeline unification + security blockers | No |
| Sprint 2 | Phase 1 | EPIC-01 (complete), EPIC-05 (critical), EPIC-06 (critical) | Capture unified + debug gated + contrast fixed | No |
| Sprint 3 | Phase 2 | EPIC-02, EPIC-03 (partial), EPIC-04 (partial), EPIC-11 (P1 docs) | Lifecycle safety + detection framework | No |
| Sprint 4 | Phase 2/3 | EPIC-02, EPIC-03, EPIC-04, EPIC-11 (complete) | Decision engine + docs complete | **Yes** (RC2 candidate) |
| Sprint 5 | Phase 3/4 | EPIC-12 (partial), EPIC-07, EPIC-08 | ViewModel tests + design system | Yes (Beta ready) |
| Sprint 6 | Phase 4/5/6 | EPIC-06 (complete), EPIC-08, EPIC-09 | UX/Play compliance finish | Yes |
| Sprint 7 | Phase 7 | EPIC-12 (remaining), EPIC-11 (P3), EPIC-09 (remaining) | Cleanup + final docs | RC2/Beta approved |

---

## Recommended Release Cadence

1. **RC2** — End of Sprint 4 — Architecture epics + docs complete, security-critical items resolved
2. **Beta** — End of Sprint 5 — All UX/accessibility blockers resolved, full testing infrastructure
3. **RC3** — End of Sprint 7 — All 191 findings resolved, repository clean

## Key Milestones

| Milestone | Target | Criteria |
|---|---|---|
| Capture Pipeline Unified | Sprint 2 | Single AccessibilityService, legacy removed |
| Google Play Blockers Resolved | Sprint 2 | Data Safety, Privacy Policy, Debug Panel |
| Device Verification Complete | Sprint 1 | S-S14 confirmed working with `exported="false"` |
| RC2 Release | Sprint 4 | Architecture epics done, no CRÍTICO remaining |
| Beta Release | Sprint 5 | All ALTO resolved, UX compliant, tests passing |
| Full Remediation Complete | Sprint 7 | All 191 findings addressed |

*End of IMPLEMENTATION_ROADMAP.md*

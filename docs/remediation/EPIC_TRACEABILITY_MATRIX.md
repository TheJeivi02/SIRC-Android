# EPIC TraceABILITY MATRIX

This matrix traces every Epic to its covered findings, source audits, affected modules, assigned sprint, and status progression.

## Matrix

| Epic ID | Epic Name | Findings Count | Findings Covered (IDs) | Audits Source | Modules Affected | Sprint Assigned | Initial State | Expected State |
|---|---|---|---|---|---|---|---|---|
| EPIC-01 | Unify & Optimize Capture Pipeline | 15 | ARC-1.1, DUP-10.1, DUP-10.3, PKG-7.1, ACC-7.1, ACC-7.2, ACC-7.3, ACC-7.4, ACC-7.5, LIF-3.1, S-S15, S-S16, GP-4, P-P08, P-P17 | ARCHITECTURE, ANDROID, PERFORMANCE, STABILITY, GOOGLE_PLAY | `:app`, `:feature:overlay`, `:core:capture`, `:core:capture:android`, `:core:platform` | Sprint 1-2 | 2 active services, 2 pipelines, legacy active | 1 service, 1 pipeline, legacy removed |
| EPIC-02 | MediaProjection Lifecycle Safety | 9 | MPR-6.1, MPR-6.2, MPR-6.3, MPR-6.5, BRT-12.3, S-S10, S-S20, S-S28 | ANDROID, STABILITY, PERFORMANCE | `:core:capture:android`, `:feature:overlay` | Sprint 3 | No onDestroy, channel leaks, PNG round-trip | onDestroy present, clean image handling, no PNG round-trip |
| EPIC-03 | Platform-Agnostic Detection Framework | 12 | SCA-11.1, SCA-11.2, SCA-11.3, SCA-11.4, SCA-11.5, SCA-11.6, SCA-11.7, SCA-11.8, SCA-11.9, SCA-11.10, SCA-11.12, S-S08 | ARCHITECTURE, STABILITY | `:core:platform`, `:core:capture`, `:domain`, `:data`, `:feature:overlay` | Sprint 3 | Detection in Spanish+Uber only | Platform-specific detection, multi-currency |
| EPIC-04 | Consolidate Decision Engine | 12 | ARC-1.3, ARC-1.4, DUP-10.6, DEAD-9.1, DEAD-9.2, DEAD-9.3, ABS-8.2, DEAD-9.4, ARC-1.2, DEAD-9.4 | ARCHITECTURE, QA | `:domain`, `:data`, `:core:capture`, `:feature:overlay` | Sprint 4 | Dual engines, orphaned use-cases, FakeParser in prod | Single engine, no dead code, FakeParser in test |
| EPIC-05 | Security, Privacy & Data Protection | 8 | S-1, S-2, S-3, S-4, S-5, GP-1, GP-2, GP-3 | SECURITY, GOOGLE_PLAY | `:app`, `:data`, `:feature:overlay`, `:core:capture` | Sprint 1-2 | DB plaintext, debug panel in release, false Data Safety | DB encrypted, debug gated, accurate Data Safety |
| EPIC-06 | Drive-Safe UX & Accessibility | 15 | UX-2.1, UX-2.2, UX-2.3, UX-2.4, UX-4.1, UX-6.1, UX-11.1, UX-11.2, UX-11.3, UX-11.4, UX-1.1, UX-1.2, UX-1.3, UX-3.1, UX-3.2, UX-5.1, UX-5.2, UX-5.3, UX-6.2, UX-7.2, UX-8.1, UX-9.1, UX-2.5 | UX, STABILITY | `:core:ui`, `:feature:overlay`, `:feature:history`, `:feature:settings` | Sprint 2, 6 | 9sp text, 2.0:1 contrast, no TalkBack | ≥14sp text, ≥4.5:1 contrast, TalkBack support |
| EPIC-07 | Presentation Layer Architecture | 15 | SOL-2.1, SOL-2.2, SOL-2.3, SOL-2.4, SOL-2.5, CMP-1.1, CMP-1.3, CMP-1.5, MIX-6.1, MIX-6.2, MOD-3.1, VM-9.1, VM-9.2, VM-9.3, SIZ-5.1 | ARCHITECTURE, ANDROID | `:app`, `:feature:overlay`, `:feature:history`, `:feature:settings`, `:feature:onboarding`, `:domain` | Sprint 5 | God-classes, DebugPanel at root, no SavedStateHandle | Decomposed classes, scoped ViewModels, saved state |
| EPIC-08 | Localization & Design System | 15 | UX-12.1, UX-1.1, UX-1.2, UX-1.3, UX-3.1, UX-3.2, UX-5.1, UX-6.2, UX-7.2, UX-8.1, UX-9.1, UX-2.5, PKG-7.2, S-S07, S-S08 | UX, STABILITY, ARCHITECTURE | All UI modules | Sprint 5-6 | Strings hardcoded, light theme incomplete | All strings in resources, complete theme |
| EPIC-09 | Performance Optimization | 16 | P-P01, P-P02, P-P03, P-P04, P-P05, P-P06, P-P07, P-P08, P-P09, P-P10, P-P11, P-P12, P-P13, P-P14, P-P15, P-P16 | PERFORMANCE, ANDROID | `:core:capture`, `:core:capture:android`, `:feature:overlay`, `:app` | Sprint 6-7 | Full-res OCR always on, PNG round-trip | Gated OCR, reduced resolution, no duplicate work |
| EPIC-10 | Testing Infrastructure & Coverage | 9 | H-QA-01, H-QA-02, H-QA-03, B-QA-08, B-QA-09, M-QA-04, M-QA-05, M-QA-06, M-QA-07 | QA | All modules (new test files) | Sprint 5-6 | 0 ViewModel tests, 0 UI tests, 0 OCR tests | ≥5 ViewModel tests, OCR tests, instrumented tests |
| EPIC-11 | Documentation Integrity | 13 | DOC-2.1, DOC-2.2, DOC-3.1, DOC-3.2, DOC-4.1, DOC-5.1, DOC-5.2, DOC-7.1, DOC-8.1, DOC-9.1, DOC-9.2, DOC-9.3, DOC-9.4 | DOCUMENTATION | All documentation (`.md` and `.kt` KDoc) | Sprint 1, 4, 7 | 8 modules in docs, v0.1.0 in PROJECT.md | 11 modules correct, v1.0.0-rc1, formula documented |
| EPIC-12 | Stability & Lifecycle Hardening | 28 | S-S02, S-S03, S-S04, S-S05, S-S06, S-S07, S-S08, S-S09, S-S10, S-S11, S-S12, S-S13, S-S18, S-S20, S-S22, S-S23, S-S24, S-S28, CMP-1.1, CMP-1.2, CMP-1.4, CMP-1.6, LIF-3.2, FLW-4.1, FLW-4.2, FLW-4.3, CO-5.1, CO-5.2, FGS-8.2, FGS-8.3, FGS-8.4, WKM-10.1, BRT-12.1 | STABILITY, ANDROID, QA, UX, PERFORMANCE | `:app`, `:feature:overlay`, `:core:capture:android`, `:data`, `:domain` | Sprint 3-7 | No lifecycle hooks, no process death recovery | All lifecycle events handled, edge cases covered |

---

## Audit Source Coverage per Epic

| Epic | Arquitectura | Android | QA | Performance | Stability | Security | Google Play | UX | Documentation |
|---|---|---|---|---|---|---|---|---|---|
| EPIC-01 | ✓ (ARC-1.1, DUP-10.1, DUP-10.3, PKG-7.1, DEAD-9.4) | ✓ (ACC-7.1..7.5, CMP-1.x, LIF-3.1, FLW-4.x, CO-5.1, WKM-10.1, BRT-12.x) |  | ✓ (P-P08, P-P17) | ✓ (S-S15, S-S16) |  | ✓ (GP-4) |  |  |
| EPIC-02 |  | ✓ (MPR-6.1..6.5) |  | ✓ (P-P04) | ✓ (S-S10, S-S20) |  |  |  |  |
| EPIC-03 | ✓ (SCA-11.1..11.12) |  |  |  | ✓ (S-S08) |  |  |  |  |
| EPIC-04 | ✓ (ARC-1.2..1.4, SOL-2.x, MOD-3.x, ABS-8.2, SIZ-5.x, DEAD-9.x) |  | ✓ |  |  |  |  |  |  |
| EPIC-05 |  |  |  |  |  | ✓ (S-1..S-5) | ✓ (GP-1..GP-3) |  |  |
| EPIC-06 |  |  |  |  |  |  |  | ✓ (UX-2.x, UX-3.x, UX-4.x, UX-6.x, UX-7.x, UX-8.x, UX-9.x, UX-11.x, UX-12.x) | ✓ |
| EPIC-07 | ✓ (SOL-2.1..2.5, MIX-6.x, MOD-3.1) | ✓ (CMP-1.1, CMP-1.3, CMP-1.5, VM-9.1..9.3) |  |  |  |  |  |  |  |
| EPIC-08 |  |  |  |  | ✓ (S-S07, S-S08) |  |  | ✓ (UX-1.x, UX-3.x, UX-5.x, UX-6.x, UX-8.x, UX-9.x, UX-11.x, UX-12.x) |  |
| EPIC-09 |  | ✓ (CO-5.1, CO-5.2) |  | ✓ (P-P01..P-P16) |  |  |  |  |  |
| EPIC-10 |  |  | ✓ (H-QA-01..03, M-QA-04..07, B-QA-08, B-QA-09) |  |  |  |  |  |  |
| EPIC-11 |  |  |  |  |  |  |  |  | ✓ (DOC-2.x, DOC-3.x, DOC-4.x, DOC-5.x, DOC-7.x, DOC-8.x, DOC-9.x) |
| EPIC-12 |  | ✓ (S-S02..S-S28, CMP-1.x, LIF-3.2, FLW-4.x, FGS-8.x) |  | ✓ (BRT-12.1) | ✓ |  |  | ✓ (UX-11.x, UX-2.x) | ✓ |

---

## Sprint-to-Module Impact Matrix

| Sprint | Modules | Epics | Critical Path |
|---|---|---|---|
| Sprint 1 | `:app`, `:data`, `:feature:overlay`, docs | EPIC-01 (start), EPIC-05 (critical), EPIC-11 (DOC-2.1) | Pipeline unification + security gating |
| Sprint 2 | `:core:ui`, `:feature:overlay`, `:data` | EPIC-01 (complete), EPIC-05 (complete), EPIC-06 (critical) | Security complete + contrast fix |
| Sprint 3 | `:core:capture`, `:core:capture:android`, `:core:platform` | EPIC-02, EPIC-03 | Lifecycle safety + platform detection |
| Sprint 4 | `:domain`, `:core:capture`, `:data` | EPIC-04, EPIC-11 (complete) | Decision engine + docs done |
| Sprint 5 | `:app`, `:feature:*`, `:core:ui` | EPIC-07, EPIC-08 (start), EPIC-10 (start) | Presentation layer refactored |
| Sprint 6 | All UI modules, `:core:capture` | EPIC-06 (complete), EPIC-08 (complete), EPIC-09 (start) | UX complete + performance start |
| Sprint 7 | All modules | EPIC-09 (complete), EPIC-12 (remaining), EPIC-11 (P3) | Final optimization + cleanup |

---

## Root Cause Themes Coverage

| Root Cause | Epics Addressing It | Findings Count |
|---|---|---|
| Incomplete migration (dual pipeline) | EPIC-01 | 21 findings across 5 audits |
| Missing lifecycle hooks | EPIC-02, EPIC-12 | 14 findings |
| Single-platform design | EPIC-03 | 12 findings |
| Parallel decision systems | EPIC-04 | 12 findings |
| Debug infrastructure in release | EPIC-05 | 8 findings |
| No internationalization strategy | EPIC-08 | 15 findings |
| No testing strategy for presentation | EPIC-10, EPIC-07 | 24 findings |
| No documentation sync process | EPIC-11 | 13 findings |
| No performance budget | EPIC-09 | 16 findings |
| No accessibility compliance | EPIC-06 | 15 findings |
| No architecture conventions | EPIC-07, EPIC-12 | 15+ findings |

---

## Status Legend

| Initial State | Status | Description |
|---|---|---|
| 🟥 Red | Active | In progress, not yet verified |
| 🟨 Yellow | Blocked | Waiting on dependency |
| ⚪ White | Pending | Not started |
| 🟩 Green | Complete | Verified and accepted |

### Current Status (Sprint 0 — Planning)

| Epic | Status | Notes |
|---|---|---|
| EPIC-01 | 🟥 Red | Ready to execute (pending S-S14 device verification) |
| EPIC-02 | ⚪ White | Blocked by EPIC-01 |
| EPIC-03 | ⚪ White | Blocked by EPIC-01 |
| EPIC-04 | ⚪ White | Blocked by EPIC-01, EPIC-03 |
| EPIC-05 | ⚪ White | Not blocked — can start immediately |
| EPIC-06 | ⚪ White | Partially blocked by EPIC-08 |
| EPIC-07 | ⚪ White | Blocked by EPIC-04 |
| EPIC-08 | ⚪ White | Blocked by EPIC-07 |
| EPIC-09 | ⚪ White | Blocked by EPIC-01, EPIC-02, EPIC-04 |
| EPIC-10 | ⚪ White | Blocked by EPIC-07 |
| EPIC-11 | ⚪ White | Not blocked — can start immediately |
| EPIC-12 | ⚪ White | Partially blocked by EPIC-01, EPIC-02 |

---

## Sprint Status Tracking Template

```
SPRINT 1 STATUS
===============
EPIC-01: 🟨 (in progress — pipeline analysis done, awaiting S-S14 verification)
EPIC-05: 🟨 (in progress — debug panel gated, Data Safety text corrected)
EPIC-11: 🟩 (DOC-2.1: CHANGELOG v0.5.0 restored)

Key Metric: Single service active = [0/1]
Key Metric: Debug panel in release = [FAIL]
Key Metric: Data Safety accurate = [FAIL → in progress]
```

*End of EPIC_TRACEABILITY_MATRIX.md*

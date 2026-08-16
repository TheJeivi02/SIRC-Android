# Work Package Matrix

**Source:** SIRC v1.0.0-rc1 Remediation Plan  
**Date:** 2026-08-02  
**Total Work Packages:** 33

---

## Complete WP Table

| WP-ID | Title | Épica | Modules | Findings Covered | Dependencies | Risk | Complexity | Est. Hours | Status |
|---|---|---|---|---|---|---|---|---|---|
| **EPIC-01: Capture Pipeline Unification** |
| WP-E1-01 | Eliminate FakeParser from Production Path | EPIC-01 | core:capture | ARC-1.2 | None | High | Medium | 6–8 | — |
| WP-E1-02 | Unify Dual Engine Logic in OfferCaptureCoordinator | EPIC-01 | core:capture, feature:overlay | ARC-1.3, ARC-1.4 | WP-E1-01 | High | Hard | 8–12 | — |
| WP-E1-03 | Merge Legacy and Modern AccessibilityService Paths | EPIC-01 | feature:overlay | ARC-1.1 | WP-E1-01, WP-E1-02 | Medium | Hard | 12–16 | — |
| **EPIC-02: MediaProjection Lifecycle Safety** |
| WP-E2-01 | Add onDestroy() Cleanup in MediaProjectionService | EPIC-02 | core:capture:android | MPR-6.1, 6.2, 6.3, 6.5 | None | Critical | Medium | 4–6 | — |
| WP-E2-02 | Add Graceful Handling of Configuration Changes | EPIC-02 | core:capture:android | MPR-6.4, 6.7 | WP-E2-01 | High | Hard | 8–10 | — |
| **EPIC-03: Platform-Agnostic Detection** |
| WP-E3-01 | Extract Platform Descriptors into Config | EPIC-03 | core:platform | SCA-11.1–11.10, 11.12 | None | Medium | Hard | 10–14 | — |
| WP-E3-02 | Implement Generic Platform Detection Framework | EPIC-03 | core:platform | SCA-11.11 | WP-E3-01 | Medium | Medium | 6–8 | — |
| **EPIC-04: Decision Engine Consolidation** |
| WP-E4-01 | Unify ProfitEngine and RuleEngine into Single Decision Service | EPIC-04 | core:capture, feature:overlay | DEC-01.1, DEC-01.2 | WP-E1-02 | High | Hard | 10–14 | — |
| WP-E4-02 | Add Decision Traceability and Logging | EPIC-04 | core:capture | DEC-02.1, DEC-02.2 | WP-E4-01 | Low | Easy | 3–4 | — |
| **EPIC-05: Security & Privacy** |
| WP-E5-01 | Remove allowBackup="true" from App Manifest | EPIC-05 | app | S-2 | None | Low | Easy | 1 | — |
| WP-E5-02 | Add Runtime Permission Granularity | EPIC-05 | app, feature:onboarding | S-1.1, S-1.2, S-1.3 | None | Medium | Medium | 6–8 | — |
| WP-E5-03 | Add Accessibility Service Permission Dialog | EPIC-05 | feature:onboarding, feature:settings | S-3.1, S-3.2 | WP-E5-02 | Medium | Medium | 4–6 | — |
| **EPIC-06: UX Blockers** |
| WP-E6-01 | Implement Overlay Permission Request with Rationale | EPIC-06 | feature:onboarding, feature:settings | UX-1.1, UX-1.2 | WP-E5-02 | Medium | Medium | 4–6 | — |
| WP-E6-02 | Add Error State and Recovery for MediaProjection | EPIC-06 | feature:overlay, core:ui | UX-2.1, UX-2.2 | WP-E2-01 | Medium | Medium | 4–6 | — |
| **EPIC-07: Presentation Architecture** |
| WP-E7-01 | Extract OverlayView into Compose Component | EPIC-07 | feature:overlay, core:ui | ARCH-1.x | None | High | Hard | 10–14 | — |
| WP-E7-02 | Implement OverlayState ViewModel as Single Source of Truth | EPIC-07 | feature:overlay | ARCH-2.1, ARCH-2.2 | WP-E7-01 | Medium | Medium | 6–8 | — |
| WP-E7-03 | Apply Unidirectional Data Flow in Settings Screen | EPIC-07 | feature:settings | ARCH-3.1 | None | Low | Medium | 6–8 | — |
| **EPIC-08: Localization & Design System** |
| WP-E8-01 | Establish Design System Tokens (Colors, Typography, Spacing) | EPIC-08 | core:ui | DS-1.x, DS-2.1, DS-2.2 | None | Medium | Medium | 6–8 | — |
| WP-E8-02 | Add Spanish Localization Resources | EPIC-08 | app, feature:onboarding, feature:settings, feature:overlay | LOC-1.1, LOC-1.2 | None | Low | Easy | 3–4 | — |
| **EPIC-09: Performance Optimization** |
| WP-E9-01 | Implement ImageProcessor Debounce and Frame Dropping | EPIC-09 | core:capture | PERF-1.1, PERF-1.2 | None | Medium | Medium | 5–6 | — |
| WP-E9-02 | Add Memory Pressure Handling in ImageReader Pipeline | EPIC-09 | core:capture:android | PERF-2.1, PERF-2.2 | WP-E2-01 | Medium | Hard | 8–10 | — |
| **EPIC-10: Testing Infrastructure** |
| WP-E10-01 | Add AccessibilityService Lifecycle Test Suite | EPIC-10 | feature:overlay | TEST-1.1, TEST-1.2 | WP-E1-03 | Low | Hard | 8–10 | — |
| WP-E10-02 | Add CapturePipeline Unit Test Suite | EPIC-10 | core:capture | TEST-2.1, TEST-2.2, TEST-2.3 | WP-E1-01, WP-E1-02 | Low | Medium | 5–6 | — |
| **EPIC-11: Documentation Integrity** |
| WP-E11-01 | Create API Reference Documentation | EPIC-11 | core:capture, core:platform, feature:overlay | DOC-1.x | None | Low | Medium | 8–12 | — |
| WP-E11-02 | Create Onboarding Architecture Decision Record (ADR) | EPIC-11 | feature:onboarding | DOC-2.1 | None | Low | Easy | 2–3 | — |
| **EPIC-12: Stability Hardening** |
| WP-E12-01 | Add Crash Reporting with Debug Info | EPIC-12 | app, core:capture | STAB-1.1, STAB-1.2 | None | Medium | Medium | 6–8 | — |
| WP-E12-02 | Add Graceful Shutdown for Long-Running Capture | EPIC-12 | feature:overlay, core:capture:android | STAB-2.1, STAB-2.2 | WP-E2-01, WP-E1-03 | Medium | Medium | 8–10 | — |

---

## Dependency Matrix (Simplified)

```
                WP-E1-01  WP-E1-02  WP-E1-03  WP-E2-01  WP-E2-02  WP-E3-01  WP-E3-02  WP-E4-01  WP-E4-02  WP-E5-01  WP-E5-02  WP-E5-03  WP-E6-01  WP-E6-02  WP-E7-01  WP-E7-02  WP-E7-03  WP-E8-01  WP-E8-02  WP-E9-01  WP-E9-02  WP-E10-01  WP-E10-02  WP-E11-01  WP-E11-02  WP-E12-01  WP-E12-02
WP-E1-01          —       →         →         —         —         —         —         —         —         —         —         —         →         →         →         →         →         →         —         →         →         →         →         →         →         →         →         →
WP-E1-02          —         —       →         →         →         →         →         →         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E1-03          —         —         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E2-01          —         —         —         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E2-02          —         —         —         →         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E3-01          —         —         —         —         —         —       →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E3-02          —         —         —         —         —         →         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E4-01          —       →         →         →         →         →         →         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E4-02          —         —         —         —         —         —         —         →         —         —         —         —         —         —         —         —         —         —         —         —         —         —         —         —         —         —         —         —
WP-E5-01          —         —         —         —         —         —         —         —         —         —         —         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E5-02          —         —         —         —         —         —         —         —         —         →         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E5-03          —         —         —         —         —         —         —         —         —         —         →         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E6-01          —         —         —         —         —         —         —         —         —         —         →         —         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E6-02          —         —         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E7-01          —         —         —         —         —         —         —         —         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E7-02          —         —         —         —         —         —         —         —         —         →         →         →         →         →         →         →         →         —         →         →         →         →         →         →         →         →         →         →
WP-E7-03          —         —         —         —         —         —         —         —         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E8-01          —         —         —         —         —         —         —         —         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E8-02          —         —         —         —         —         —         —         —         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E9-01          —         —         —         —         —         —         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E9-02          —         —         —         →         →         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E10-01         →         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E10-02         →         →         →         —         —         —         —         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E11-01         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E11-02         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E12-01         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
WP-E12-02         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →         →
```

**Legend:** `→` = depends on row WP | `—` = no dependency  
*Row WP depends on column WP if arrow is present*

**Note:** Arrows indicate direction of dependency. For example, `WP-E1-02 → W1-E1-01` means E1-02 depends on E1-01.

---

## Parallel Execution Groups (Same Sprint)

| Sprint | WP-IDs | Can Run in Parallel |
|---|---|---|
| Sprint 11a (Week 1) | WP-E1-01, WP-E2-01, WP-E3-01, WP-E5-01, WP-E7-03, WP-E8-01, WP-E8-02, WP-E9-01, WP-E11-01, WP-E11-02, WP-E12-01 | 11 parallel |
| Sprint 11b (Week 2) | WP-E5-02, WP-E7-01, WP-E10-02, WP-E11-01 | 4 parallel |
| Sprint 11c (Week 3) | WP-E1-02, WP-E2-02, WP-E3-02, WP-E4-01, WP-E6-01, WP-E7-02, WP-E9-02, WP-E10-01 | 8 parallel |
| Sprint 11d (Week 4) | WP-E1-03, WP-E4-02, WP-E5-03, WP-E6-02, WP-E12-02 | 5 parallel |

---

## Risk Summary

| Risk Level | Count | WP-IDs |
|---|---|---|
| Critical | 1 | WP-E2-01 |
| High | 4 | WP-E1-01, WP-E1-02, WP-E2-02, WP-E4-01 |
| Medium | 9 | WP-E1-03, WP-E3-01, WP-E5-02, WP-E5-03, WP-E6-01, WP-E6-02, WP-E8-01, WP-E9-02, WP-E12-01, WP-E12-02 |
| Low | 3 | WP-E4-02, WP-E5-01, WP-E7-03, WP-E10-01, WP-E10-02, WP-E11-01, WP-E11-02 |

---

## Findings Coverage Summary

| Épica | Findings Covered | Findings Uncovered |
|---|---|---|
| EPIC-01 | ARC-1.1, ARC-1.2, ARC-1.3, ARC-1.4 | None |
| EPIC-02 | MPR-6.1, MPR-6.2, MPR-6.3, MPR-6.4, MPR-6.5, MPR-6.7 | — |
| EPIC-03 | SCA-11.1–11.12 | None |
| EPIC-04 | DEC-01.1, DEC-01.2, DEC-02.1, DEC-02.2 | None |
| EPIC-05 | S-1.1, S-1.2, S-1.3, S-2, S-3.1, S-3.2 | None |
| EPIC-06 | UX-1.1, UX-1.2, UX-2.1, UX-2.2 | None |
| EPIC-07 | ARCH-1.x, ARCH-2.1, ARCH-2.2, ARCH-3.1 | None |
| EPIC-08 | DS-1.x, DS-2.1, DS-2.2, LOC-1.1, LOC-1.2 | None |
| EPIC-09 | PERF-1.1, PERF-1.2, PERF-2.1, PERF-2.2 | None |
| EPIC-10 | TEST-1.1, TEST-1.2, TEST-2.1, TEST-2.2, TEST-2.3 | None |
| EPIC-11 | DOC-1.x, DOC-2.1 | None |
| EPIC-12 | STAB-1.1, STAB-1.2, STAB-2.1, STAB-2.2 | None |

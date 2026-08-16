# Loop Execution Order

**Purpose:** Define the technical order in which Work Packages should be executed across Loops, including parallelization opportunities and critical path identification.

**Date:** 2026-08-02  
**Scope:** Sprint 11 execution of all 33 WPs

---

## 1. Execution Philosophy

The execution order is driven by:

1. **Critical path first** — Épicas that unblock downstream work are prioritized (EPIC-01, EPIC-02, EPIC-03, EPIC-04, EPIC-07, EPIC-09).
2. **Dependency topology** — WPs with no dependencies can start immediately (Week 1).
3. **Risk mitigation** — High-risk WPs are scheduled early in their dependency chain to allow time for resolution.
4. **Module isolation** — WPs touching different modules can run in parallel within the same sprint.

---

## 2. Phase-Based Execution Order

### Phase 1: Foundation Layer (Sprint 11 — Week 1)
**Duration:** 5 days  
**Objective:** Establish base infrastructure, remove critical blockers, and set up foundations.

| Priority | WP-ID | Title | Loop Type | Module | Risk | Est. Hours |
|---|---|---|---|---|---|---|
| P0 | WP-E1-01 | Eliminate FakeParser from Production Path | Backend | core:capture | High | 6–8 |
| P0 | WP-E2-01 | Add onDestroy() Cleanup in MediaProjectionService | Backend | core:capture:android | Critical | 4–6 |
| P1 | WP-E3-01 | Extract Platform Descriptors into Config | Backend | core:platform | Medium | 10–14 |
| P1 | WP-E5-01 | Remove allowBackup="true" from App Manifest | Backend | app | Low | 1 |
| P1 | WP-E7-03 | Apply Unidirectional Data Flow in Settings Screen | Frontend | feature:settings | Low | 6–8 |
| P1 | WP-E8-01 | Establish Design System Tokens | Frontend | core:ui | Medium | 6–8 |
| P1 | WP-E8-02 | Add Spanish Localization Resources | Frontend | app, feature:* | Low | 3–4 |
| P1 | WP-E9-01 | Implement ImageProcessor Debounce | Backend | core:capture | Medium | 5–6 |
| P2 | WP-E11-01 | Create API Reference Documentation | Docs | core:capture, platform, overlay | Low | 8–12 |
| P2 | WP-E11-02 | Create Onboarding ADR | Docs | feature:onboarding | Low | 2–3 |
| P2 | WP-E12-01 | Add Crash Reporting with Debug Info | Backend | app, core:capture | Medium | 6–8 |

**Max Parallelism:** 11 loops (all independent)  
**Critical WPs:** WP-E1-01, WP-E2-01, WP-E3-01  

**Rationale:** These WPs have zero dependencies and establish the foundation for all subsequent work. WP-E1-01 and WP-E2-01 address the most critical production issues (fake parser, resource leaks). WP-E3-01 enables platform-agnostic detection, which is foundational for EPIC-04 and EPIC-07. WP-E5-01, WP-E7-03, WP-E8-01, WP-E8-02, WP-E9-01 are independent features that can ship in parallel.

---

### Phase 2: Pipeline & Engine Consolidation (Sprint 11 — Week 2)
**Duration:** 5 days  
**Objective:** Consolidate capture pipeline and decision engine logic.

| Priority | WP-ID | Title | Loop Type | Module | Risk | Est. Hours | Dependencies |
|---|---|---|---|---|---|---|---|
| P0 | WP-E1-02 | Unify Dual Engine Logic in OfferCaptureCoordinator | Backend | core:capture, feature:overlay | High | 8–12 | WP-E1-01 |
| P1 | WP-E5-02 | Add Runtime Permission Granularity | Backend | app, feature:onboarding | Medium | 6–8 | None (started Week 1) |
| P1 | WP-E7-01 | Extract OverlayView into Compose Component | Frontend | feature:overlay, core:ui | High | 10–14 | None (started Week 1) |
| P2 | WP-E10-02 | Add CapturePipeline Unit Test Suite | Tests | core:capture | Low | 5–6 | WP-E1-01, WP-E1-02 |

**Max Parallelism:** 4 loops  
**Critical WPs:** WP-E1-02, WP-E7-01  

**Rationale:** WP-E1-02 builds on WP-E1-01 (removing the fake parser) to unify engine logic. WP-E5-02 continues the permission flow started in Week 1. WP-E7-01 begins the Compose migration — it's high-risk but independent. WP-E10-02 can start once E1-01 and E1-02 are partially complete.

---

### Phase 3: Service Unification & Platform Detection (Sprint 11 — Week 3)
**Duration:** 5 days  
**Objective:** Unify services, complete platform detection, and extend pipeline consolidation.

| Priority | WP-ID | Title | Loop Type | Module | Risk | Est. Hours | Dependencies |
|---|---|---|---|---|---|---|---|
| P0 | WP-E1-03 | Merge Legacy and Modern AccessibilityService Paths | Backend | feature:overlay | Medium | 12–16 | WP-E1-01, WP-E1-02 |
| P0 | WP-E3-02 | Implement Generic Platform Detection Framework | Backend | core:platform | Medium | 6–8 | WP-E3-01 |
| P0 | WP-E4-01 | Unify ProfitEngine and RuleEngine into Single Decision Service | Backend | core:capture, feature:overlay | High | 10–14 | WP-E1-02 |
| P1 | WP-E6-01 | Implement Overlay Permission Request with Rationale | Frontend | feature:onboarding, feature:settings | Medium | 4–6 | WP-E5-02 |
| P1 | WP-E7-02 | Implement OverlayState ViewModel as Single Source of Truth | Backend | feature:overlay | Medium | 6–8 | WP-E7-01 |
| P1 | WP-E9-02 | Add Memory Pressure Handling in ImageReader Pipeline | Backend | core:capture:android | Medium | 8–10 | WP-E2-01 |
| P2 | WP-E10-01 | Add AccessibilityService Lifecycle Test Suite | Tests | feature:overlay | Low | 8–10 | WP-E1-03 |

**Max Parallelism:** 8 loops  
**Critical WPs:** WP-E1-03, WP-E4-01, WP-E10-01  

**Rationale:** This is the densest phase — the service unification (WP-E1-03) unblocks the service test suite (WP-E1-01). Platform detection framework (WP-E3-02) completes the platform-agnostic initiative. Decision engine consolidation (WP-E4-01) builds on pipeline unification. Overlay Compose work continues with the ViewModel (WP-E7-02). Memory pressure handling depends on the onDestroy work from Week 1.

---

### Phase 4: Polish & Stability (Sprint 11 — Week 4)
**Duration:** 5 days  
**Objective:** Complete remaining UX, testing, and stability WPs.

| Priority | WP-ID | Title | Loop Type | Module | Risk | Est. Hours | Dependencies |
|---|---|---|---|---|---|---|---|
| P0 | WP-E12-02 | Add Graceful Shutdown for Long-Running Capture | Backend | feature:overlay, core:capture:android | Medium | 8–10 | WP-E2-01, WP-E1-03 |
| P1 | WP-E4-02 | Add Decision Traceability and Logging | Backend | core:capture | Low | 3–4 | WP-E4-01 |
| P1 | WP-E5-03 | Add Accessibility Service Permission Dialog | Frontend | feature:onboarding, feature:settings | Medium | 4–6 | WP-E5-02 |
| P1 | WP-E6-02 | Add Error State and Recovery for MediaProjection | Frontend | feature:overlay, core:ui | Medium | 4–6 | WP-E2-01 |

**Max Parallelism:** 5 loops  
**Critical WPs:** WP-E12-02  

**Rationale:** The final phase ties together the critical paths — graceful shutdown depends on both service unification (WP-E1-03) and lifecycle cleanup (WP-E2-01). Error states and logging are polish tasks. Permission guidance completes the UX/security flow.

---

## 3. Full Dependency Chain (Simplified)

```
WP-E11-01, WP-E11-02, WP-E12-01, WP-E5-01, WP-E8-01, WP-E8-02, WP-E7-03, WP-E9-01, WP-E7-01
    │
    ├──→ WP-E1-01 → WP-E1-02 → WP-E4-01 → WP-E4-02
    │                 │           │
    │                 └────┬────→ WP-E1-03 → WP-E10-01 → WP-E12-02
    │                      │
    │                      └──→ WP-E10-02
    │
    └──→ WP-E2-01 → WP-E2-02
                     │
                     └────┬──→ WP-E6-02
                          │
                          └──→ WP-E9-02
                          │
                          └──→ WP-E12-02
    
    └──→ WP-E5-02 → WP-E5-03
    └──→ WP-E5-02 → WP-E6-01
    └──→ WP-E7-01 → WP-E7-02
    └──→ WP-E3-01 → WP-E3-02
```

---

## 4. Critical Path Identification

The **critical path** is the longest chain of dependent WPs that determines minimum sprint duration:

```
WP-E1-01 (8h) → WP-E1-02 (12h) → WP-E1-03 (16h) → WP-E10-01 (10h) → WP-E12-02 (10h)
```

**Total critical path duration:** ~56 hours (~7 person-days)

**Secondary critical path:**
```
WP-E2-01 (6h) → WP-E2-02 (10h) → WP-E12-02 (10h)
```

**Total:** ~26 hours (parallelizes with main path)

**WP-E12-02 is the convergence point** — it depends on both the service unification path and the MediaProjection lifecycle path.

---

## 5. Parallelization Matrix

| Week | Independent WPs | Dependent WPs | Max Parallel Loops |
|---|---|---|---|
| Week 1 | 11 WPs (no dependencies) | 0 | 11 |
| Week 2 | 2 WPs (WP-E5-02, WP-E7-01 continue from Week 1) | 2 | 4 |
| Week 3 | 2 WPs (WP-E6-01, WP-E9-02) | 6 | 8 |
| Week 4 | 0 | 4 | 5 |

**Peak parallelism:** Week 1 (11 concurrent loops)  
**Minimum parallelism:** Week 4 (5 concurrent loops)

---

## 6. Loop Types

| Loop Type | Description | WPs in This Category |
|---|---|---|
| **Backend** | Core architecture, pipeline, engine, service changes | WP-E1-01, E1-02, E1-03, E2-01, E2-02, E3-01, E3-02, E4-01, E4-02, E9-01, E9-02, E10-02, E12-01, E12-02 |
| **Frontend** | UI, UX, Compose, permissions, localization | WP-E5-02, E5-03, E6-01, E6-02, E7-01, E7-02, E7-03, E8-01, E8-02 |
| **Tests** | Test infrastructure, coverage, test suites | WP-E10-01, E10-02 |
| **Docs** | API documentation, ADRs | WP-E11-01, E11-02 |

**Backend loops:** 13 WPs (40%)  
**Frontend loops:** 8 WPs (24%)  
**Test loops:** 2 WPs (6%)  
**Docs loops:** 2 WPs (6%)  
**Backend + Frontend mixed:** WP-E5-01, WP-E12-01, WP-E7-03 (3 WPs, 9%)

---

## 7. Risk Mitigation Timeline

| WP-ID | Risk | When Addressed | Mitigation Strategy |
|---|---|---|---|
| WP-E2-01 | Critical | Week 1 | Early scheduling; resource leak testing |
| WP-E1-01 | High | Week 1 | Foundational; all subsequent WPs depend on clean pipeline |
| WP-E1-02 | High | Week 2 | Depends on E1-01; test dual-engine removal thoroughly |
| WP-E7-01 | High | Week 2 | Independent; can fail without blocking; fallback to View system |
| WP-E4-01 | High | Week 3 | Depends on E1-02; critical for decision consistency |
| WP-E1-03 | Medium | Week 3 | Depends on both E1-01 and E1-02; service test suite depends on it |
| WP-E3-01 | Medium | Week 1 | Foundational; all platform detection depends on config |
| WP-E9-02 | Medium | Week 3 | Depends on E2-01; test on low-memory device |
| WP-E12-02 | Medium | Week 4 | Convergence point; requires both service and lifecycle work |

---

## 8. Execution Recommendation

1. **Start all Week 1 WPs in parallel** — these are the foundation.
2. **Do not start WP-E1-02 until WP-E1-01 is code-complete** (not just started).
3. **WP-E7-01 (Compose migration) is high-risk** — consider having a fallback plan to keep the View-based overlay if Compose migration fails.
4. **WP-E12-02 must start early in Week 4** — it has the longest chain of dependencies.
5. **WP-E11-01 (docs) can run throughout** — it's independent and can absorb idle engineering cycles.
6. **Assign Loop Engineers based on expertise:**
   - Loop Engineers A+B: Backend (WP-E1-01, E2-01, E3-01, E4-01, E9-01)
   - Loop Engineers C+D: Frontend (WP-E7-01, E7-02, E7-03, E8-01, E8-02)
   - Loop Engineer E: Security/UX (WP-E5-01, E5-02, E5-03, E6-01, E6-02)
   - Loop Engineer F: Performance/Tests (WP-E9-02, E10-01, E10-02, E12-01, E12-02)
   - Loop Engineer G: Docs/Stability (WP-E11-01, E11-02, E4-02)

**Total estimated effort:** 191–259 person-hours / 7 engineers = ~27–37 hours per engineer for Sprint 11 (~2 weeks at 20 hours/week).

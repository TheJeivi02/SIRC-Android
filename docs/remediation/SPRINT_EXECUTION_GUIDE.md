# Sprint 11 Execution Guide — Work Packages

**Sprint:** Sprint 11 (2 weeks)  
**Date:** 2026-08-02  
**Objective:** Execute all 33 Work Packages to remediate the 12 Épicas from SIRC v1.0.0-rc1  
**Duration:** 10 working days (5 days/week)  
**Loop Engineers:** 7 (A through G)  

---

## Sprint Overview

| Metric | Value |
|---|---|
| Total WPs | 33 |
| Total Est. Hours | 191–259 |
| Peak Parallel Loops | 11 (Week 1, Day 1) |
| Average per Engineer | 27–37 hours |
| Critical Path Duration | ~56 hours (7 person-days) |
| WPs on Critical Path | 5 (E1-01 → E1-02 → E1-03 → E10-01 → E12-02) |

---

## 1. Week 1 — Foundation Sprint

### Day 1 (Monday) — Kickoff & Parallel Launch

**11 independent WPs start simultaneously:**

| Engineer | WP-ID | Type | Module | Risk | Est. Hours | Notes |
|---|---|---|---|---|---|---|
| A | WP-E1-01 | Backend | core:capture | 🔴 Critical | 6–8 | Highest priority — pipeline hygiene |
| B | WP-E2-01 | Backend | core:capture:android | 🔴 Critical | 4–6 | Resource leak fix |
| C | WP-E3-01 | Backend | core:platform | 🟠 High | 10–14 | Platform config extraction |
| D | WP-E7-03 | Frontend | feature:settings | 🟢 Low | 6–8 | UDF settings |
| E | WP-E5-02 | Backend/Frontend | app, onboarding | 🟡 Medium | 6–8 | Permission granularity |
| F | WP-E7-01 | Frontend | feature:overlay | 🟠 High | 10–14 | Compose migration — high-risk |
| G | WP-E8-01 | Frontend | core:ui | 🟡 Medium | 6–8 | Design tokens |
| A | WP-E8-02 | Frontend | app, features | 🟢 Low | 3–4 | Spanish localization |
| B | WP-E9-01 | Backend | core:capture | 🟡 Medium | 5–6 | ImageProcessor debounce |
| C | WP-E11-01 | Docs | multiple | 🟢 Low | 8–12 | API docs — can absorb idle time |
| D | WP-E11-02 | Docs | onboarding | 🟢 Low | 2–3 | ADR |
| E | WP-E12-01 | Backend | app, capture | 🟡 Medium | 6–8 | Crash reporting |
| F | WP-E5-01 | Backend | app | 🟢 Low | 1 | Quick manifest fix |

**End-of-Day Checkpoint:**
- WP-E1-01: Code-complete (DI updates, FakeParser removed)
- WP-E2-01: Code-complete (onDestroy implemented)
- WP-E3-01: Config schema defined, first 2 platforms migrated
- WP-E7-01: ComposeView integration planned, layout scaffold ready

### Day 2-3 (Tue-Wed) — Foundation Continuation

| Engineer | WP-ID | Type | Module | Status | Notes |
|---|---|---|---|---|---|
| A | WP-E1-01 | Backend | core:capture | Continue | Testing injection guards |
| A | WP-E10-02 | Test | core:capture | Start* | *After E1-01 code-complete |
| B | WP-E2-01 | Backend | core:capture:android | Continue | LeakCanary integration |
| B | WP-E9-02 | Backend | core:capture:android | Start | *Depends on E2-01 |
| C | WP-E3-01 | Backend | core:platform | Continue | Migrate all platforms |
| D | WP-E7-01 | Frontend | feature:overlay | Continue | Compose layout built |
| D | WP-E5-01 | Backend | app | Complete | Verify build |
| E | WP-E5-02 | Backend/Frontend | app, onboarding | Continue | Rationale dialogs |
| F | WP-E7-01 | Frontend | feature:overlay | Continue | Compose rendering working |
| G | WP-E8-01 | Frontend | core:ui | Continue | Color contrast fixes |
| G | WP-E8-02 | Frontend | app, features | Complete | QA review |
| G | WP-E11-01 | Docs | multiple | Continue | KDoc coverage tracking |

**Mid-Week Checkpoint:**
- WP-E1-01: Tests passing, ready for E1-02
- WP-E2-01: No leaks detected, ready for E2-02
- WP-E3-01: Config complete, ready for E3-02
- WP-E7-01: Compose scaffold rendering, ready for E7-02

### Day 4-5 (Thu-Fri) — Transition to Phase 2

| Engineer | WP-ID | Type | Module | Status | Notes |
|---|---|---|---|---|---|
| A | WP-E1-01 | Backend | core:capture | Complete | Acceptance: FakeParser gone |
| A | WP-E1-02 | Backend | core:capture, overlay | Start | Unify engines |
| B | WP-E2-01 | Backend | core:capture:android | Complete | Acceptance: onDestroy works |
| B | WP-E2-02 | Backend | core:capture:android | Start | Config changes |
| C | WP-E3-01 | Backend | core:platform | Complete | Acceptance: config-driven |
| C | WP-E3-02 | Backend | core:platform | Start | Detector framework |
| D | WP-E7-01 | Frontend | feature:overlay | Continue | Ready for E7-02 |
| D | WP-E7-03 | Frontend | feature:settings | Complete | UDF settings done |
| E | WP-E5-02 | Backend/Frontend | app, onboarding | Continue | Permission flow |
| F | WP-E7-01 | Frontend | feature:overlay | Continue | Compose overlay ready |
| G | WP-E8-01 | Frontend | core:ui | Complete | Tokens established |
| G | WP-E11-01 | Docs | multiple | Continue | KDoc in progress |
| G | WP-E11-02 | Docs | onboarding | Complete | ADR written |

**Week 1 Retrospective (Friday EOD):**
- All 11 Week-1 WPs on track or complete
- WP-E1-02 started (depends on E1-01) ✓
- WP-E9-02 started (depends on E2-01) ✓
- WP-E3-02 started (depends on E3-01) ✓

---

## 2. Week 2 — Consolidation & Testing

### Phase 2 Launch (Monday)

**WP-E1-02, WP-E5-02, WP-E7-01, WP-E10-02 active**

| Engineer | WP-ID | Type | Module | Est. Hours |
|---|---|---|---|---|
| A | WP-E1-02 | Backend | core:capture, overlay | 8–12 |
| A | WP-E4-01 | Backend | core:capture | 10–14 |
| B | WP-E2-02 | Backend | core:capture:android | 8–10 |
| C | WP-E3-02 | Backend | core:platform | 6–8 |
| D | WP-E7-01 | Frontend | feature:overlay | 10–14 |
| D | WP-E7-02 | Backend | feature:overlay | 6–8 |
| E | WP-E5-02 | Backend/Frontend | app, onboarding | 6–8 |
| E | WP-E5-03 | Frontend | feature:onboarding | 4–6 |
| E | WP-E6-01 | Frontend | feature:onboarding | 4–6 |
| F | WP-E7-01 | Frontend | feature:overlay | 10–14 |
| G | WP-E9-02 | Backend | core:capture:android | 8–10 |
| G | WP-E10-01 | Test | feature:overlay | 8–10 |

### Phase 3 Launch (Tuesday-Wednesday)

**WP-E1-03, WP-E4-01, WP-E10-01, WP-E10-02 complete or near-complete**

| Engineer | WP-ID | Type | Module | Est. Hours |
|---|---|---|---|---|
| A | WP-E1-03 | Backend | feature:overlay | 12–16 |
| A | WP-E10-02 | Test | core:capture | 5–6 |
| B | WP-E2-02 | Backend | core:capture:android | 8–10 |
| B | WP-E6-02 | Frontend | feature:overlay | 4–6 |
| C | WP-E3-02 | Backend | core:platform | 6–8 |
| D | WP-E7-02 | Backend | feature:overlay | 6–8 |
| E | WP-E4-01 | Backend | core:capture | 10–14 |
| E | WP-E5-03 | Frontend | onboarding | 4–6 |
| F | WP-E6-01 | Frontend | onboarding | 4–6 |
| G | WP-E10-01 | Test | feature:overlay | 8–10 |
| G | WP-E12-01 | Backend | app, capture | 6–8 |

### Phase 4 Launch (Thursday-Friday)

**Convergence on WP-E12-02**

| Engineer | WP-ID | Type | Module | Est. Hours |
|---|---|---|---|---|
| A | WP-E12-02 | Backend | overlay, capture:android | 8–10 |
| B | WP-E10-01 | Test | feature:overlay | 8–10 |
| B | WP-E4-02 | Backend | core:capture | 3–4 |
| C | WP-E3-02 | Backend | core:platform | 6–8 |
| D | WP-E7-02 | Backend | feature:overlay | 6–8 |
| E | WP-E4-01 | Backend | core:capture | 10–14 |
| E | WP-E5-03 | Frontend | onboarding | 4–6 |
| F | WP-E6-02 | Frontend | feature:overlay | 4–6 |
| G | WP-E9-02 | Backend | core:capture:android | 8–10 |

---

## 3. Daily Standup Cadence

### Week 1
- **Monday 9:00 AM** — Kickoff, assign all 11 parallel WPs
- **Tuesday 9:00 AM** — Progress check: E1-01, E2-01, E3-01 status
- **Wednesday 9:00 AM** — Mid-week: any blockers? Reassign if needed
- **Thursday 9:00 AM** — Transition prep: E1-02, E2-02, E3-02 ready?
- **Friday 5:00 PM** — Week 1 retrospective

### Week 2
- **Monday 9:00 AM** — Phase 2 launch
- **Tuesday 9:00 AM** — E1-03 progress check (longest WP at 16h)
- **Wednesday 12:00 PM** — Mid-week checkpoint
- **Thursday 9:00 AM** — Phase 4 launch (E12-02 convergence)
- **Friday 5:00 PM** — Sprint review

---

## 4. Risk Register — Sprint 11

| Risk | Probability | Impact | Mitigation | Owner |
|---|---|---|---|---|
| WP-E7-01 (Compose migration) fails | Medium | High | Fallback to View system; isolated change | Engineer F |
| WP-E1-03 (service merge) breaks accessibility service | Medium | High | Test on physical device before E10-01 | Engineer A |
| WP-E3-01 (platform config) introduces parsing regression | Medium | Medium | Maintain backward compatibility for Uber | Engineer C |
| WP-E12-02 (graceful shutdown) missed deadline | Low | High | Start early Thursday; assign 2 engineers | Engineers A, B |
| WP-E10-01 (service tests) blocked by E1-03 | Low | Medium | Have test fixtures ready in parallel | Engineer G |
| Engineer F overloaded (E7-01 high-risk) | High | Medium | Pair with Engineer D on Compose | Engineering Lead |

---

## 5. Definition of "Done" for Each Loop Type

### Backend Loop (WP-E1-01, E1-02, E1-03, E2-01, E2-02, E3-01, E3-02, E4-01, E4-02, E9-01, E9-02, E12-01, E12-02)
- [ ] Code implemented per acceptance criteria
- [ ] Unit tests: ≥80% coverage on changed files
- [ ] Integration tests: pass on emulator
- [ ] No Lint errors
- [ ] No regressions in dependent modules
- [ ] Code reviewed by 1 senior engineer
- [ ] Merged to `develop` branch

### Frontend Loop (WP-E5-01, E5-02, E5-03, E6-01, E6-02, E7-01, E7-02, E7-03, E8-01, E8-02)
- [ ] UI implemented per spec
- [ ] Manual QA on physical device
- [ ] Accessibility labels present
- [ ] Color contrast ≥4.5:1 (WCAG AA)
- [ ] Strings externalized (no hardcoded text)
- [ ] Code reviewed by 1 senior engineer
- [ ] Merged to `develop` branch

### Test Loop (WP-E10-01, E10-02)
- [ ] Test suite written
- [ ] Tests pass on CI
- [ ] Coverage ≥80% on target classes
- [ ] Flaky test <5% failure rate (run 20x)
- [ ] Test reviewed by 1 engineer
- [ ] Merged to `develop` branch

### Docs Loop (WP-E11-01, E11-02)
- [ ] Documentation complete per template
- [ ] Dokka generates without errors
- [ ] ADR follows MADR format
- [ ] Reviewed by 1 senior engineer
- [ ] Merged to `develop` branch

---

## 6. Tools & Commands

### Build & Test
```bash
# Full build
./gradlew assembleDebug

# Run all unit tests
./gradlew test

# Run instrumentation tests
./gradlew connectedDebugAndroidTest

# Run specific module tests
./gradlew :core:capture:test
./gradlew :feature:overlay:connectedDebugAndroidTest

# Check coverage
./gradlew :core:capture:jacocoTestReport

# Lint
./gradlew lint

# Dokka documentation
./gradlew dokkaHtml
```

### Git Workflow
```bash
# Create feature branch
git checkout -b wp/e1-01-fake-parser-removal

# Commit (use conventional commits)
git commit -m "fix(capture): remove FakeParser from production pipeline"

# Push and create PR
git push origin wp/e1-01-fake-parser-removal
gh pr create --fill
```

### Branch Protection
- `develop` requires PR review + CI pass
- `main` requires PR review + CI pass + 2 approvals

---

## 7. Communication Channels

| Purpose | Channel |
|---|---|
| Daily standups | Zoom 9:00 AM (M-W), 12:00 PM (Thu) |
| Blockers | #sirc-sprint11 Slack channel |
| Code reviews | GitHub PRs |
| Architecture decisions | ADR in `docs/adr/` |
| Documentation | Wiki + Dokka HTML |
| Post-mortems | End of Sprint 11 retrospective |

---

## 8. Success Metrics

| Épica | Metric | Target | Measured By |
|---|---|---|---|
| EPIC-01 | No FakeParser in prod path | 100% | Unit test |
| EPIC-02 | No MediaProjection leaks | 0 leaks | LeakCanary |
| EPIC-03 | Platform config loaded | 100% | Config test |
| EPIC-04 | Single engine invocation | 1 path | Code coverage |
| EPIC-05 | No allowBackup leaks | 0 violations | Lint |
| EPIC-06 | Overlay permission granted | >90% users | Analytics (if available) |
| EPIC-07 | Compose overlay renders | 100% | UI test |
| EPIC-08 | All strings translated | 100% | Lint |
| EPIC-09 | CPU usage <5% during idle | ≤5% | Benchmark |
| EPIC-10 | Engine coverage ≥80% | ≥80% | Jacoco |
| EPIC-11 | Dokka generates cleanly | 0 errors | Dokka build |
| EPIC-12 | Graceful shutdown saves data | 100% | Manual test |

---

## 9. Post-Sprint 11 Deliverables

1. **33 WPs completed** with acceptance criteria verified
2. **12 Épicas remediated** per EPIC_TRACEABILITY_MATRIX
3. **7 Loop Engineers** with individual WP completion reports
4. **Full test coverage** for capture pipeline and accessibility service
5. **Documentation** complete with API reference and ADRs
6. **Performance baselines** recorded (CPU, memory, startup time)

---

## 10. Appendix: WP-Épica Mapping

| Épica | WP Count | WPs |
|---|---|---|
| EPIC-01 | 3 | WP-E1-01, WP-E1-02, WP-E1-03 |
| EPIC-02 | 2 | WP-E2-01, WP-E2-02 |
| EPIC-03 | 2 | WP-E3-01, WP-E3-02 |
| EPIC-04 | 2 | WP-E4-01, WP-E4-02 |
| EPIC-05 | 3 | WP-E5-01, WP-E5-02, WP-E5-03 |
| EPIC-06 | 2 | WP-E6-01, WP-E6-02 |
| EPIC-07 | 3 | WP-E7-01, WP-E7-02, WP-E7-03 |
| EPIC-08 | 2 | WP-E8-01, WP-E8-02 |
| EPIC-09 | 2 | WP-E9-01, WP-E9-02 |
| EPIC-10 | 2 | WP-E10-01, WP-E10-02 |
| EPIC-11 | 2 | WP-E11-01, WP-E11-02 |
| EPIC-12 | 2 | WP-E12-01, WP-E12-02 |
| **Total** | **33** | |

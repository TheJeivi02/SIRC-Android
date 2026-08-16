# IMPLEMENTATION ORDER

This document defines the exact order of implementation for all 12 Epics with technical justification for each decision.

## Execution Order

The order below is mandatory. Deviating from it risks cascading regressions, redundant work, and architectural instability.

---

## 1. EPIC-11 (Documentation Fix: CHANGELOG v0.5.0) — Day 1

**Order: 1st**

**Justification:** This is a zero-risk, high-impact fix that takes <1 hour. The missing v0.5.0 header causes confusion immediately. Since documentation changes don't affect code, this can be done first and verified instantly. It also establishes the documentation accuracy baseline that all subsequent work is measured against.

**Deliverable:** `docs/CHANGELOG.md` has `## [v0.5.0]` header with Sprint 4 content properly placed.

**Verification:** File inspection of CHANGELOG.md headers.

---

## 2. EPIC-05 (Security Critical Blockers) — Sprint 1, Days 2-5

**Order: 2nd (parallel with EPIC-01 setup)**

**Justification:** Google Play compliance is a hard gate that cannot slip. The Data Safety false declaration (GP-1), debug panel in release (S-3), and `allowBackup` (S-2) are CRITICAL/ALTO findings that must be resolved before Beta consideration. These are independent of the pipeline architecture — they're configuration and build-variant changes. Starting them immediately alongside EPIC-01 setup ensures they don't become a late bottleneck.

**Sub-order within EPIC-05:**
1. **Day 2:** Gate debug panel behind `BuildConfig.DEBUG` (S-3) — 1 line change, highest impact
2. **Day 2-3:** Fix Data Safety declaration text (GP-1) + draft privacy policy (GP-2) — documentation
3. **Day 3:** Fix `allowBackup` / add `dataExtractionRules` (S-2) — manifest change
4. **Day 4-5:** Begin SQLCipher integration (S-1) — dependency setup

**Technical Reasoning:** 
- Debug panel gating doesn't depend on which pipeline runs — it's a build concern
- Data Safety and privacy policy are pure documentation — zero code risk
- `allowBackup` fix is a manifest attribute — cannot cause runtime regressions
- SQLCipher integration can begin once we know the DB schema is stable (it is — only encryption changes)

---

## 3. EPIC-11 (Documentation Fix: Module Count + PROJECT.md) — Day 3

**Order: 3rd (parallel)**

**Justification:** Incorrect module counts (8 vs 11) and stale version references cause developer confusion during implementation. Every engineer touching the codebase needs accurate documentation. These must be fixed before the team starts modifying modules in EPIC-01 and EPIC-03.

**Deliverable:** README, AGENTS.md, ROADMAP.md, PROJECT.md all updated to 11 modules and v1.0.0-rc1.

**Verification:** Grep for "8 modules" and "v0.1.0" — should return 0 results.

---

## 4. EPIC-01 (Pipeline Unification) — Sprint 1-2

**Order: 4th**

**Justification:** This is the single most critical architectural change. The dual pipeline is the root cause of:
- Double CPU/battery consumption (P-P08, P-P17, S-S16)
- Duplicate history entries (DUP-10.2)
- Debug complexity affecting multiple ViewModels (SOL-2.2, CMP-1.3)
- Fragmented lifecycle management (MPR-6.1 affects both pipelines)

Every other technical epic depends on having a single pipeline. Starting here means:
- EPIC-02 (lifecycle) targets one service, not two
- EPIC-03 (detection) wires to one pipeline
- EPIC-04 (decision engine) consolidates in one place
- EPIC-09 (performance) optimizes one path

**Critical Sub-Ordering within EPIC-01:**
1. **First:** Verify S-S14 (`exported="false"`) on device — if it fails, stop and redesign
2. **Second:** Add `CaptureAccessibilityService` as the single service (keep legacy temporarily)
3. **Third:** Route legacy events through modern pipeline
4. **Fourth:** Verify capture works with both active (regression check)
5. **Fifth:** Remove legacy service, `AccessibilityWindowObserver`, `OfferCaptureCoordinator`
6. **Sixth:** Fix `hasAccessibilityPermission()` to check correct service

**Risk Mitigation:** Do NOT remove legacy until modern pipeline is verified end-to-end with device testing.

**Verification:** Device test — single service receives events, captures work, no duplicate history.

---

## 5. EPIC-05 (Security Completion) + EPIC-06 (UX Critical) — Sprint 2

**Order: 5th (EPIC-05 continuation: SQLCipher, memory cleanup)**

**Justification:** 
- EPIC-05 remaining items (S-1 DB encryption, S-4/S-5 memory cleanup) can now proceed safely with unified pipeline.
- EPIC-06 (UX-2.1 contrast fix) is CRITICAL for driving safety and must be done before Beta. This is a pure UI/color change with no architectural dependencies.

**Technical Reasoning:**
- SQLCipher integration is independent of pipeline logic
- Contrast fix touches `core:ui` color tokens, which doesn't depend on EPIC-01 changes
- Both can be verified independently

**Verification:** DB encryption verified; contrast ≥4.5:1 measured; TalkBack test.

---

## 6. EPIC-02 (MediaProjection Lifecycle Safety) — Sprint 3

**Order: 6th**

**Justification:** Now that EPIC-01 has unified the pipeline, the MediaProjection lifecycle cleanup targets a single, known code path. Previously, fixing `onDestroy()` would need to handle both legacy and modern cleanup — now there's only one set of resources to manage. This reduces the risk of missing a resource path.

**Dependencies Met:** EPIC-01 unified the pipeline → single `MediaProjectionService` to fix.

**Verification:** Process kill + Doze simulation shows no leaked projection.

---

## 7. EPIC-03 (Platform Detection Framework) — Sprint 3

**Order: 7th**

**Justification:** The platform detection must be wired into the unified pipeline. With the legacy pipeline removed (EPIC-01), we know exactly where detection runs. Changing detection rules while two pipelines exist would require duplicating all rule changes.

**Dependencies Met:** EPIC-01 (single pipeline knows where to inject platform-specific rules).

**Technical Reasoning:** The detection engine sits at the boundary between AccessibilityService (EPIC-01) and ParserOrchestrator. With one service, one parser path, the rules are in exactly one place.

**Verification:** Platform detection tests for Uber, DiDi, Cabify, InDrive all pass.

---

## 8. EPIC-04 (Decision Engine Consolidation) — Sprint 4

**Order: 8th**

**Justification:** The decision engine operates within the capture pipeline. With the pipeline unified (EPIC-01) and detection framework established (EPIC-03), we can now safely consolidate the two competing decision engines. The `if (platform == UBER)` parser selection and `RuleThresholds` vs `DecisionThresholds` conflict can be resolved in a single, known location.

**Dependencies Met:** 
- EPIC-01 (single pipeline processes offers)
- EPIC-03 (detection returns platform, parser selection known)

**Technical Reasoning:** Removing dead use-cases (`EvaluateOfferUseCase`, `AddOfferHistoryUseCase`) requires knowing the single active path. With unified pipeline, we can trace all call sites accurately.

**Verification:** All 26 domain engine tests pass; no orphaned use-cases.

---

## 9. EPIC-11 (Documentation Completion) — Sprint 4

**Order: 9th**

**Justification:** After the major architectural changes (EPIC-01, EPIC-02, EPIC-03, EPIC-04), documentation must be updated to reflect the new reality. ARCHITECTURE.md's claim about "legacy eliminated" can now be corrected (or made accurate). PROJECT.md version can be finalized.

**Dependencies Met:** Core architecture changes complete.

**Verification:** ARCHITECTURE.md references audit documents; version references consistent.

---

## 10. EPIC-07 (Presentation Layer Architecture) — Sprint 5

**Order: 10th**

**Justification:** The presentation layer refactoring (decomposing `PipelineOverlayDataSource`, extracting `OverlayWindowController`, scoping `DebugPanelViewModel`) requires knowing the clean interfaces from the domain and data layers. With EPIC-04 complete (consolidated decision engine, no dead use-cases), the data source has a stable contract to implement.

**Dependencies Met:** 
- EPIC-04 (decision engine defines the contract)
- EPIC-12 (S-S14, MPR-6.1) must be verified first to ensure the presentation layer builds on stable services

**Technical Reasoning:** If EPIC-07 runs before EPIC-04, we might decompose a god-class that's about to change its core logic. Doing them in order means the decomposition targets the final architecture.

**Verification:** `PipelineOverlayDataSource` < 350 lines; ViewModels have SavedStateHandle.

---

## 11. EPIC-08 (Localization & Design System) — Sprint 5-6

**Order: 11th**

**Justification:** The design system (typography, color schemes, spacing tokens) must be built on the final ViewModel interfaces. If ViewModels expose `ProfitEngine` directly (MIX-6.1), the formatting lives in the wrong place. After EPIC-07 refactoring, we know exactly where formatting belongs and can extract it cleanly.

**Dependencies Met:** EPIC-07 (ViewModels have clean interfaces, no engine leaks to UI).

**Technical Reasoning:** String extraction requires touching every screen. If those screens are about to be refactored (EPIC-07), we'd extract strings only to move them again.

**Verification:** 0 hardcoded strings in `.kt` files; complete light theme.

---

## 12. EPIC-06 (UX Completion) — Sprint 6

**Order: 12th**

**Justification:** The drive-safe UX fixes (talkback, touch targets, text sizes) depend on the design system (EPIC-08) being in place. Font sizes, color tokens, and typography styles must all come from the unified system before we can ensure consistency. The contrast fix (UX-2.1) is an exception — it's done early in Sprint 2 because it's safety-critical.

**Dependencies Met:** EPIC-08 (color tokens, typography) for comprehensive UX fixes.

**Technical Reasoning:** You can't fix accessibility semantics properly until you know the final component structure (EPIC-07). You can't fix text sizing until you have the complete typography system (EPIC-08).

**Verification:** WCAG AA audit passes; TalkBack walkthrough complete.

---

## 13. EPIC-10 (Testing Infrastructure) — Sprint 5-6

**Order: 13th (parallel from Sprint 5)**

**Justification:** Tests for ViewModels require the refactored ViewModels from EPIC-07. OCR engine tests and instrumented service tests require the lifecycle-safe MediaProjection service (EPIC-02). These can start in Sprint 5 alongside EPIC-08 because testing infrastructure setup (dependencies, base classes) is independent of implementation.

**Dependencies Met:** 
- EPIC-07 (ViewModels testable)
- EPIC-02 (services safe to test)

**Technical Reasoning:** Writing tests too early means rewriting them when the underlying code changes. Writing them after refactoring captures the final behavior accurately.

**Verification:** ≥5 ViewModel test files; ≥30 ViewModel test cases.

---

## 14. EPIC-09 (Performance Optimization) — Sprint 6-7

**Order: 14th**

**Justification:** Performance optimization requires a stable, consolidated system. OCR gating (P-P01) must know the final detection rules (EPIC-03). Memory optimizations require knowing the final pipeline behavior (EPIC-01). Thread pool changes require knowing the final coroutine scope structure (EPIC-02).

**Dependencies Met:** EPIC-01, EPIC-02, EPIC-03 all complete.

**Technical Reasoning:** Optimizing a system that's still changing wastes effort. The 40% performance budget must be measured on the final architecture.

**Verification:** OCR latency <500ms; memory growth <20MB over 50 captures.

---

## 15. EPIC-12 (Stability & Lifecycle Hardening) — Sprint 7

**Order: 15th (remaining P2 items)**

**Justification:** The remaining stability items (S-S03, S-S09, S-S11, S-S12, S-S13, S-S18, S-S22, S-S23, S-S24, S-S28) are edge cases that require the final architecture to test against. For instance, testing `BOOT_COMPLETED` behavior requires knowing the final service states (EPIC-01), and testing `fallbackToDestructiveMigration` (S-S28) requires knowing the final DB schema (EPIC-05).

**Dependencies Met:** All architecture epics complete.

**Technical Reasoning:** These are polish and robustness items. They can only be validated once the core system is stable. Testing them earlier would mean re-testing after every architectural change.

**Verification:** Device test matrix passes on Android 12-15.

---

## Summary Order Table

| Step | Epic | Sprint | Reasoning |
|---|---|---|---|
| 1 | EPIC-11 (DOC-2.1) | Sprint 1 | Zero-risk, immediate clarity |
| 2 | EPIC-05 (critical) | Sprint 1 | Play compliance gate |
| 3 | EPIC-11 (module count) | Sprint 1 | Accurate docs for team |
| 4 | EPIC-01 | Sprint 1-2 | Root cause of most issues |
| 5 | EPIC-05 (rest) | Sprint 2 | Security hardening |
| 6 | EPIC-06 (contrast) | Sprint 2 | Safety-critical UX |
| 7 | EPIC-02 | Sprint 3 | Lifecycle safety on unified pipeline |
| 8 | EPIC-03 | Sprint 3 | Detection on single pipeline |
| 9 | EPIC-04 | Sprint 4 | Decision consolidation |
| 10 | EPIC-11 (rest) | Sprint 4 | Final docs |
| 11 | EPIC-07 | Sprint 5 | Presentation refactoring |
| 12 | EPIC-08 | Sprint 5-6 | Design system on clean ViewModels |
| 13 | EPIC-06 (rest) | Sprint 6 | UX on final design system |
| 14 | EPIC-10 | Sprint 5-6 | Tests on refactored code |
| 15 | EPIC-09 | Sprint 6-7 | Performance on stable system |
| 16 | EPIC-12 | Sprint 7 | Edge case stability |

---

## Non-Negotiable Ordering Rules

1. **EPIC-01 must precede EPIC-02, EPIC-03, EPIC-04** — all these modify the capture pipeline
2. **EPIC-04 must precede EPIC-07** — presentation layer refactoring requires stable domain interfaces
3. **EPIC-07 must precede EPIC-08** — string extraction requires stable file structure
4. **EPIC-08 must precede EPIC-06 (remainder)** — UX polish requires design system
5. **EPIC-02 must precede EPIC-10** — service tests require lifecycle-safe components
6. **EPIC-05 (GP-1) must precede Beta submission** — non-negotiable Play requirement
7. **S-S14 device verification must precede EPIC-01 execution** — could invalidate the plan

## Parallel Opportunities

- EPIC-05 can run entirely parallel to EPIC-01 (no pipeline dependencies)
- EPIC-11 can run entirely parallel to all epics (documentation only)
- EPIC-10 test setup can begin parallel to EPIC-08 (dependency declaration independent of string extraction)

---

## Technical Decision Log

1. **Why EPIC-01 before EPIC-02?** Fixing lifecycle on two parallel pipelines risks missing resources. Single pipeline = single cleanup path = zero gaps.

2. **Why EPIC-04 after EPIC-03?** Platform detection determines which parser runs, which feeds the decision engine. Consolidation must see all platform paths.

3. **Why EPIC-07 after EPIC-04?** The `PipelineOverlayDataSource` god-class contains both pipeline orchestration AND decision logic. Consolidating the decision engine first means the data source decomposition targets the correct final behavior.

4. **Why EPIC-09 last (among technical)?** Performance optimization on a changing system produces wasted work and misleading benchmarks. The final architecture's performance is what matters.

*End of IMPLEMENTATION_ORDER.md*

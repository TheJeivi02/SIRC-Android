# RISK MATRIX

This matrix classifies each Epic by Impact and Probability to determine priority and mitigation strategy.

## Risk Classification Scale

### Impact
- **High (H):** Can block Beta/Beta approval, cause data loss, or create safety risk
- **Medium (M):** Causes significant quality degradation, user frustration, or compliance risk
- **Low (L):** Minor issue, cosmetic, or low-priority debt

### Probability
- **High (H):** >70% chance of occurrence
- **Medium (M):** 30-70% chance of occurrence
- **Low (L):** <30% chance of occurrence

### Risk Priority
- **CRITICAL:** H×H, H×M, M×H
- **HIGH:** H×L, M×M, L×H
- **MEDIUM:** M×L, L×M
- **LOW:** L×L

---

## Epic Risk Matrix

| Epic | Impact | Probability | Risk Level | Risk Description | Mitigation |
|---|---|---|---|---|---|
| EPIC-01 | High | Medium | **HIGH** | Removing the legacy pipeline could break capture if the modern pipeline has undocumented gaps. The legacy coordinator handles events differently. | Run parallel capture (both services) during transition. Comprehensive integration tests covering all platform combinations before removal. |
| EPIC-02 | High | Medium | **HIGH** | Adding `onDestroy()` and changing channel behavior could introduce new bugs in the critical capture path. | Thorough testing on Android 12-15. Kill/Doze simulation on physical devices. Add recovery logic. |
| EPIC-03 | High | Medium | **HIGH** | Platform detection changes could break existing Uber parsing. Detection rules are tightly coupled with OCR output. | Add platform-specific tests before and after changes. Validate against all existing test images. |
| EPIC-04 | High | Medium | **HIGH** | Changing decision thresholds could cause incorrect accept/reject verdicts for drivers. Business logic is the core value prop. | Preserve exact output behavior with threshold mapping. Extensive unit tests on all engine paths. |
| EPIC-05 | High | High | **CRITICAL** | Security vulnerabilities (unencrypted DB, debug panel) are actively exploitable. Google Play rejection risk is certain if not fixed. | Staged rollout of SQLCipher. Verify release build manually. Legal review of privacy policy. |
| EPIC-06 | High | High | **CRITICAL** | Contrast/text size issues create safety risk while driving. WCAG non-compliance = Play policy risk. Accessibility discrimination risk. | Color contrast analysis tools. Physical device testing under bright light. TalkBack testing with visually impaired users. |
| EPIC-07 | Medium | Medium | **MEDIUM** | ViewModel refactoring could introduce state management regressions. God-class decomposition may miss edge cases. | Preserve exact state transitions. Migrate one ViewModel at a time. Comprehensive regression tests. |
| EPIC-08 | Medium | Medium | **MEDIUM** | String extraction could miss hardcoded strings, causing `Resources$NotFoundException` crashes. | Grep-verified string extraction. Test all screens after extraction. Run lint for hardcoded strings. |
| EPIC-09 | Medium | Low | **MEDIUM** | Performance optimizations (OCR gating, resolution reduction) could reduce OCR accuracy. | Validate OCR accuracy on test image set before/after. Keep full-resolution fallback. A/B test accuracy. |
| EPIC-10 | Low | Medium | **MEDIUM** | Adding test dependencies increases build time and APK size. Test flakiness could block CI. | Incremental test addition. Use virtual time in coroutine tests. Cache test dependencies. |
| EPIC-11 | Low | High | **MEDIUM** | Inaccurate documentation causes onboarding delays and incorrect implementation decisions. | Documentation review checklist. Cross-reference with actual code. |
| EPIC-12 | High | High | **CRITICAL** | Device-specific lifecycle issues (process death, Doze, rotation) cause real user-facing crashes. `exported="false"` on AccessibilityService could invalidate entire capture path. | **IMMEDIATE device verification of S-S14.** Test matrix: Android 12-15, physical devices only. |

---

## Findings-Level Risk Matrix

### CRÍTICO Findings (6)

| ID | Finding | Epic | Impact | Probability | Risk |
|---|---|---|---|---|---|
| MPR-6.1 | MediaProjectionService without onDestroy | EPIC-02 | High | High | **CRITICAL** |
| S-S14 | exported="false" on AccessibilityService | EPIC-01 | High | High | **CRITICAL** |
| SCA-11.1 | Detection tied to Spanish+Uber | EPIC-03 | High | Medium | **HIGH** |
| GP-1 | False Data Safety declaration | EPIC-05 | High | High | **CRITICAL** |
| UX-2.1 | ProfitIndicator contrast 2.0-3.9:1 | EPIC-06 | High | High | **CRITICAL** |
| DOC-2.1 | Missing CHANGELOG v0.5.0 header | EPIC-11 | Medium | High | **HIGH** |

### ALTO Findings Summary (38)

| Epic | Count of ALTO | Top 3 Risks |
|---|---|---|
| EPIC-01 | 7 | Pipeline regression, service not receiving events, dual-write inconsistency |
| EPIC-05 | 3 | DB exposure, backup leak, debug panel in production |
| EPIC-06 | 6 | Unsafe driving UX, TalkBack inaccessibility, touch target errors |
| EPIC-07 | 2 | ViewModel state regression, god-class behavior change |
| EPIC-02 | 2 | Resource leak on specific termination, channel race conditions |
| EPIC-03 | 5 | Platform detection regression, parser selection breakage |
| EPIC-04 | 2 | Decision logic change, false verdicts |
| EPIC-09 | 4 | OCR accuracy degradation, performance regression |
| EPIC-12 | 3 | Lifecycle crash on specific Android versions, state inconsistency |

---

## Key Risks by Category

### 1. Safety & Compliance Risks (Highest Priority)

| Risk | Epic | Mitigation Status |
|---|---|---|
| S-S14 could invalidate entire capture architecture | EPIC-01 | **BLOCKER — device verification required in Sprint 1** |
| GP-1 False Data Safety declaration → Play rejection | EPIC-05 | Text correction required in Sprint 2 |
| UX-2.1 Contrast <4.5:1 → unsafe while driving | EPIC-06 | Color change required in Sprint 2 |
| S-3 Debug panel in release → data exposure | EPIC-05 | Code gating required in Sprint 1 |
| GP-2 No privacy policy → Play rejection | EPIC-05 | Policy draft required in Sprint 2 |

### 2. Architectural Risks

| Risk | Epic | Mitigation |
|---|---|---|
| Removing legacy pipeline breaks capture | EPIC-01 | Parallel testing with both services active until verification |
| Dual decision engine produces conflicting verdicts | EPIC-04 | Map old thresholds to new unified system exactly |
| Platform detection changes break Uber parsing | EPIC-03 | Preserve existing test cases as regression suite |

### 3. Performance Risks

| Risk | Epic | Mitigation |
|---|---|---|
| OCR gating misses offers | EPIC-09 | Conservative gating — fall through to full detection if uncertain |
| Reduced resolution degrades OCR accuracy | EPIC-09 | Validate against all 15 test images before deploy |
| Memory pressure on large screens | EPIC-02 | Configurable resolution scaling |

### 4. UX/Accessibility Risks

| Risk | Epic | Mitigation |
|---|---|---|
| Text size increase causes layout overflow | EPIC-06 | Responsive overlay layout; test on various screen sizes |
| Color contrast fix changes brand colors | EPIC-06 | darken hue, not change hue; preserve brand palette |
| TalkBack implementation conflicts with overlay non-focusable design | EPIC-06 | Use announcement API, not focus API |

---

## Risk Owners

| Risk Category | Owner | Contact Point |
|---|---|---|
| Capture Pipeline Safety | Lead Android Engineer | Sprint 1 daily sync |
| Google Play Compliance | Security/Compliance Lead | Sprint 2 review |
| Drive-Safe UX | UX Designer + Safety Lead | Sprint 2 validation |
| Data Security | Security Engineer | Sprint 2 sign-off |
| Performance | Performance Engineer | Sprint 4 baseline, Sprint 7 final |
| Documentation | Technical Writer | Ongoing, Sprint 2 and Sprint 7 review |
| Device Verification | QA Engineer | Sprint 1 (S-S14), Sprint 3 (full matrix) |

---

## Risk Mitigation Timeline

| Sprint | Key Risk Verifications |
|---|---|
| Sprint 1 | S-S14 device verification; Debug panel absent in release |
| Sprint 2 | Data Safety correction verified; Privacy policy published; Contrast measured |
| Sprint 3 | Lifecycle safety verified on Android 12-15; Platform detection regression tests |
| Sprint 4 | Decision engine thresholds validated; Documentation cross-reference complete |
| Sprint 5 | ViewModel tests pass; String extraction crash-free |
| Sprint 6 | UX compliance verified (contrast, TalkBack, touch targets); Performance baseline |
| Sprint 7 | Final device matrix; Performance improvement demonstrated; Cleanup verified |

---

## Escalation Criteria

| Risk Level | Escalation Threshold | Action |
|---|---|---|
| CRITICAL | S-S14 fails device verification | Halt EPIC-01; Redesign capture architecture |
| CRITICAL | GP-1 not corrected by Sprint 2 | Delay Beta; Engage Play support team |
| HIGH | Pipeline regression in Sprint 1-2 | Revert to dual-service; Extend Sprint 1 |
| HIGH | UX-2.1 cannot achieve 4.5:1 contrast | Engage UX lead; Re-evaluate color palette |
| MEDIUM | Test flakiness blocks CI in Sprint 3+ | Dedicate Sprint to test infra stability |

*End of RISK_MATRIX.md*

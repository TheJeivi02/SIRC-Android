# Work Package Plan — SIRC v1.0.0-rc1 Remediation

**Version:** 1.0  
**Date:** 2026-08-02  
**Author:** Principal Engineering Manager / TPM  
**Status:** ✅ Approved for Sprint 11 Execution  

---

## 1. Overview

This document decomposes all 12 Épicas from `REMEDIATION_PLAN.md` into **33 Work Packages (WPs)** suitable for individual Loop Engineering execution. Each WP is:

- **Small** — single technical objective
- **Independent** — minimal inter-package coupling
- **Verifiable** — clear acceptance criteria + validation method
- **Low-regression** — no mixing of architecture, UI, or performance concerns

### WP Structure Template

Each WP follows this structure:

| Field | Description |
|---|---|
| **WP-ID** | Unique identifier (format: `WP-{Épica}-{NN}`) |
| **Title** | Concise, descriptive name |
| **Épica** | Parent Épica (EPIC-01 through EPIC-12) |
| **Objective** | One-sentence statement of what this WP accomplishes |
| **Description** | Technical details, approach, and rationale |
| **Findings Covered** | Specific finding codes addressed (from audit) |
| **Affected Modules** | Gradle modules impacted |
| **Affected Files** | Specific source files to modify |
| **Dependencies** | Prerequisite WPs that must complete first |
| **Risk** | Risk classification (from RISK_MATRIX.md) |
| **Complexity** | Easy / Medium / Hard |
| **Estimated Time** | Person-hours (Lo / Mid / Hi estimate) |
| **Acceptance Criteria** | List of conditions that must be met |
| **Validation Method** | How this WP will be verified |
| **Loop Engineering** | Loop ID, Loop Name, Loop Objective, Deliverables, Pre-conditions, Post-conditions |

---

## 2. Work Packages by Épica

### EPIC-01: Capture Pipeline Unification

#### WP-E1-01: Eliminate FakeParser from Production Path
| Field | Value |
|---|---|
| **Épica** | EPIC-01 |
| **Objective** | Remove `FakeParser` from the production capture pipeline and replace with real platform parsing. |
| **Description** | `FakeParser.kt` in `core/capture/src/main/kotlin/com/sirc/capture/parser/` is injected into the pipeline in production mode. This WP removes the fake parser, ensures `DefaultCapturePipeline` only uses real parsers, and adds a guard to prevent future injection. |
| **Findings Covered** | ARC-1.2 |
| **Affected Modules** | `:core:capture` |
| **Affected Files** | `core/capture/.../parser/FakeParser.kt`, `core/capture/.../pipeline/DefaultCapturePipeline.kt`, `core/capture/.../di/CaptureModule.kt` |
| **Dependencies** | None |
| **Risk** | High (production-critical path) |
| **Complexity** | Medium |
| **Estimated Time** | 6–8 hours |
| **Acceptance Criteria** | 1. `FakeParser` is no longer referenced in production code paths. 2. `DefaultCapturePipeline` resolves parsers exclusively through `PlatformParsers`. 3. Unit tests verify no fake parser instances exist in the production graph. |
| **Validation Method** | Unit test suite + dependency graph inspection |
| **Loop Engineering** | **Loop E1-01:** Capture Pipeline Hardening — Eliminate FakeParser. Deliverables: Remove `FakeParser.kt`, update DI, add test. Pre-conditions: None. Post-conditions: Pipeline uses only real parsers. |

#### WP-E1-02: Unify Dual Engine Logic in OfferCaptureCoordinator
| Field | Value |
|---|---|
| **Épica** | EPIC-01 |
| **Objective** | Consolidate the dual Engine invocation logic in `PipelineOverlayDataSource` and `OfferCaptureCoordinator` into a single ProfitEngine path. |
| **Description** | Findings ARC-1.3 and ARC-1.4 show both `ProfitEngine` and `RuleEngine` are invoked, with branching logic based on flags. This WP removes the `RuleEngine` branch, keeping only `ProfitEngine`, and updates all callers. |
| **Findings Covered** | ARC-1.3, ARC-1.4 |
| **Affected Modules** | `:core:capture`, `:feature:overlay` |
| **Affected Files** | `core/capture/.../coordinator/OfferCaptureCoordinator.kt`, `feature/overlay/.../PipelineOverlayDataSource.kt`, `core/decision/.../ProfitEngine.kt`, `core/decision/.../RuleEngine.kt` |
| **Dependencies** | WP-E1-01 |
| **Risk** | High |
| **Complexity** | Hard |
| **Estimated Time** | 8–12 hours |
| **Acceptance Criteria** | 1. `RuleEngine` is no longer invoked from `PipelineOverlayDataSource`. 2. All capture results flow through `ProfitEngine` only. 3. No feature flag references to `use_rule_engine` remain. |
| **Validation Method** | Unit tests + integration test on sample windows |
| **Loop Engineering** | **Loop E1-02:** Engine Consolidation — Remove RuleEngine Branch. Deliverables: Remove RuleEngine invocation, update ProfitEngine interface, add coverage. Pre-conditions: WP-E1-01 complete. Post-conditions: Single engine path in pipeline. |

#### WP-E1-03: Merge Legacy and Modern AccessibilityService Paths
| Field | Value |
|---|---|
| **Épica** | EPIC-01 |
| **Objective** | Remove `SircAccessibilityService` and consolidate all accessibility routing through `CaptureAccessibilityService`. |
| **Description** | `feature/overlay/src/main/AndroidManifest.xml` declares both `SircAccessibilityService` and `CaptureAccessibilityService`. The legacy service uses `AccessibilityWindowObserver` for routing, while the modern one uses `DebounceCaptureScheduler`. This WP removes the legacy service and observer, updating any references to the modern path. |
| **Findings Covered** | ARC-1.1 |
| **Affected Modules** | `:feature:overlay` |
| **Affected Files** | `feature/overlay/.../SircAccessibilityService.kt`, `feature/overlay/.../AccessibilityWindowObserver.kt`, `feature/overlay/.../AndroidManifest.xml` |
| **Dependencies** | WP-E1-01, WP-E1-02 |
| **Risk** | Medium |
| **Complexity** | Hard |
| **Estimated Time** | 12–16 hours |
| **Acceptance Criteria** | 1. `SircAccessibilityService` removed from manifest and codebase. 2. `AccessibilityWindowObserver` removed or refactored into `CaptureAccessibilityService`. 3. All window events route through `DebounceCaptureScheduler` → `DefaultCapturePipeline`. |
| **Validation Method** | UI test on sample app + accessibility service lifecycle test |
| **Loop Engineering** | **Loop E1-03:** Service Unification — Remove Legacy AccessibilityService. Deliverables: Remove legacy service, migrate observers, update manifest. Pre-conditions: WP-E1-01 & WP-E1-02 complete. Post-conditions: Single accessibility service path. |

---

### EPIC-02: MediaProjection Lifecycle Safety

#### WP-E2-01: Add onDestroy() Cleanup in MediaProjectionService
| Field | Value |
|---|---|
| **Épica** | EPIC-02 |
| **Objective** | Implement proper `onDestroy()` in `MediaProjectionService` to release all resources. |
| **Description** | `MediaProjectionService.kt` handles `MediaProjection` lifecycle but has no `onDestroy()` override. This causes leaks when the projection is unexpectedly stopped. This WP adds `onDestroy()` to release the `MediaProjection` instance, stop callbacks, and clean up the `ImageReader`. |
| **Findings Covered** | MPR-6.1, MPR-6.2, MPR-6.3, MPR-6.5 |
| **Affected Modules** | `:core:capture:android` |
| **Affected Files** | `core/capture/android/.../projection/MediaProjectionService.kt`, `core/capture/android/.../projection/MediaProjectionCallback.kt` |
| **Dependencies** | None |
| **Risk** | Critical |
| **Complexity** | Medium |
| **Estimated Time** | 4–6 hours |
| **Acceptance Criteria** | 1. `onDestroy()` releases `MediaProjection` via `stopProjection()`. 2. All `ImageReader` resources are closed. 3. Callbacks are unregistered. 4. No memory leaks detected in leak canary. |
| **Validation Method** | LeakCanary trace + manual lifecycle test |
| **Loop Engineering** | **Loop E2-01:** MediaProjection Cleanup — Add onDestroy. Deliverables: Implement onDestroy, stop projection, close ImageReader, unregister callbacks. Pre-conditions: None. Post-conditions: No resource leaks on service stop. |

#### WP-E2-02: Add Graceful Handling of Configuration Changes
| Field | Value |
|---|---|
| **Épica** | EPIC-02 |
| **Objective** | Ensure `MediaProjectionService` survives configuration changes without losing the projection session. |
| **Description** | When device orientation changes or configuration changes occur, the `MediaProjection` can be invalidated. This WP adds handling for `onConfigurationChanged` and ensures the service can re-attach to an existing `MediaProjection` instance if available. |
| **Findings Covered** | MPR-6.4, MPR-6.7 |
| **Affected Modules** | `:core:capture:android` |
| **Affected Files** | `core/capture/android/.../projection/MediaProjectionService.kt`, `core/capture/android/.../projection/MediaProjectionManager.kt` |
| **Dependencies** | WP-E2-01 |
| **Risk** | High |
| **Complexity** | Hard |
| **Estimated Time** | 8–10 hours |
| **Acceptance Criteria** | 1. Service survives orientation change without projection restart. 2. If `MediaProjection` becomes invalid, user is prompted to restart capture. 3. No crashes on configuration change. |
| **Validation Method** | Emulator rotation test + automated UI test |
| **Loop Engineering** | **Loop E2-02:** Configuration Safety — Survive Rotation. Deliverables: Add config change handler, re-attach logic, error recovery. Pre-conditions: WP-E2-01 complete. Post-conditions: Projection survives rotation. |

---

### EPIC-03: Platform-Agnostic Detection

#### WP-E3-01: Extract Platform Descriptors into Config
| Field | Value |
|---|---|
| **Épica** | EPIC-03 |
| **Objective** | Move hardcoded platform keywords from `SpecializedParsers.kt` and `PlatformExtractors.kt` into externalized configuration. |
| **Description** | `SpecializedParsers.kt` hardcodes Uber-specific keywords. `PlatformExtractors.kt` hardcodes Uber, DiDi, Cabify, Indrive. This WP extracts these into a `platform_config.json` resource file and a `PlatformConfig` data class, making the system platform-agnostic. |
| **Findings Covered** | SCA-11.1, SCA-11.2, SCA-11.3, SCA-11.4, SCA-11.5, SCA-11.6, SCA-11.7, SCA-11.8, SCA-11.9, SCA-11.10, SCA-11.12 |
| **Affected Modules** | `:core:platform` |
| **Affected Files** | `core/platform/.../SpecializedParsers.kt`, `core/platform/.../PlatformExtractors.kt`, `core/platform/.../PlatformConfig.kt` (new), `core/platform/.../PlatformRegistry.kt` (new) |
| **Dependencies** | None |
| **Risk** | Medium |
| **Complexity** | Hard |
| **Estimated Time** | 10–14 hours |
| **Acceptance Criteria** | 1. All platform keywords are loaded from `platform_config.json`. 2. `SpecializedParsers` and `PlatformExtractors` reference `PlatformConfig` instead of hardcoded strings. 3. Adding a new platform requires only a config change. |
| **Validation Method** | Unit test with new platform config + keyword matching test |
| **Loop Engineering** | **Loop E3-01:** Platform Config — Externalize Keywords. Deliverables: Create `PlatformConfig`, `platform_config.json`, refactor parsers. Pre-conditions: None. Post-conditions: Platform keys are config-driven. |

#### WP-E3-02: Implement Generic Platform Detection Framework
| Field | Value |
|---|---|
| **Épica** | EPIC-03 |
| **Objective** | Create a generic `PlatformDetector` interface that supports pluggable platform definitions. |
| **Description** | Building on WP-E3-01, this WP introduces a `PlatformDetector` interface with `detect(WindowNode): PlatformResult?` and implementations for each supported platform (Uber, DiDi, Cabify, Indrive). Detection logic is driven entirely by config, not code branches. |
| **Findings Covered** | SCA-11.11 |
| **Affected Modules** | `:core:platform` |
| **Affected Files** | `core/platform/.../PlatformDetector.kt` (new), `core/platform/.../UberDetector.kt` (new), `core/platform/.../DiDiDetector.kt` (new), `core/platform/.../PlatformDetectionEngine.kt` (new) |
| **Dependencies** | WP-E3-01 |
| **Risk** | Medium |
| **Complexity** | Medium |
| **Estimated Time** | 6–8 hours |
| **Acceptance Criteria** | 1. `PlatformDetector` interface exists with `detect()` method. 2. Each platform has its own detector class. 3. `PlatformDetectionEngine` chains detectors and returns the first match. 4. 99% detection accuracy on test fixtures. |
| **Validation Method** | Unit test suite with 50+ fixture windows per platform |
| **Loop Engineering** | **Loop E3-02:** Generic Detection — Pluggable Detectors. Deliverables: Define `PlatformDetector` interface, implement per-platform detectors, add `PlatformDetectionEngine`. Pre-conditions: WP-E3-01 complete. Post-conditions: Platform detection is pluggable. |

---

### EPIC-04: Decision Engine Consolidation

#### WP-E4-01: Unify ProfitEngine and RuleEngine into Single Decision Service
| Field | Value |
|---|---|
| **Épica** | EPIC-04 |
| **Objective** | Merge `RuleEngine` logic into `ProfitEngine` and remove the dual engine invocation pattern. |
| **Description** | Currently both `RuleEngine` and `ProfitEngine` are invoked in `PipelineOverlayDataSource`, creating branching logic and duplicated decision paths. This WP consolidates rule-based logic into `ProfitEngine`, removes `RuleEngine` as a separate class, and updates all references. |
| **Findings Covered** | DEC-01.1, DEC-01.2 |
| **Affected Modules** | `:core:capture`, `:core:decision` (if exists), `:feature:overlay` |
| **Affected Files** | `core/capture/.../decision/ProfitEngine.kt`, `core/capture/.../decision/RuleEngine.kt`, `feature/overlay/.../PipelineOverlayDataSource.kt` |
| **Dependencies** | WP-E1-02 |
| **Risk** | High |
| **Complexity** | Hard |
| **Estimated Time** | 10–14 hours |
| **Acceptance Criteria** | 1. `RuleEngine` class is removed or deprioritized. 2. `ProfitEngine` contains all rule-based logic. 3. No branching logic for engine selection remains in `PipelineOverlayDataSource`. |
| **Validation Method** | Unit test on ProfitEngine with rule-based inputs |
| **Loop Engineering** | **Loop E4-01:** Engine Unification — Merge RuleEngine into ProfitEngine. Deliverables: Consolidate logic, remove RuleEngine, update callers. Pre-conditions: WP-E1-02 complete. Post-conditions: Single decision engine. |

#### WP-E4-02: Add Decision Traceability and Logging
| Field | Value |
|---|---|
| **Épica** | EPIC-04 |
| **Objective** | Add structured logging to the decision engine for auditability and debugging. |
| **Description** | The decision engine currently has no visibility into why a particular action was chosen. This WP adds structured logging (via Timber) at key decision points: input received, rules evaluated, score computed, action selected. |
| **Findings Covered** | DEC-02.1, DEC-02.2 |
| **Affected Modules** | `:core:capture` |
| **Affected Files** | `core/capture/.../decision/ProfitEngine.kt`, `core/capture/.../decision/DecisionLogger.kt` (new) |
| **Dependencies** | WP-E4-01 |
| **Risk** | Low |
| **Complexity** | Easy |
| **Estimated Time** | 3–4 hours |
| **Acceptance Criteria** | 1. Every decision logs: input window, evaluated rules, computed score, selected action. 2. Logs are structured (JSON or key-value pairs). 3. Debug builds enable `DecisionLogger`; release builds omit logging overhead. |
| **Validation Method** | Log inspection during test runs |
| **Loop Engineering** | **Loop E4-02:** Decision Traceability — Add Structured Logging. Deliverables: Create `DecisionLogger`, instrument `ProfitEngine`, add debug toggle. Pre-conditions: WP-E4-01 complete. Post-conditions: Full decision visibility. |

---

### EPIC-05: Security & Privacy

#### WP-E5-01: Remove allowBackup="true" from App Manifest
| Field | Value |
|---|---|
| **Épica** | EPIC-05 |
| **Objective** | Set `allowBackup="false"` in `app/src/main/AndroidManifest.xml` to prevent sensitive data from being backed up to Google servers. |
| **Description** | The app manifest has `allowBackup="true"`, which allows app data to be backed up and restored via Google's backup service. For an app capturing financial ride data, this is a privacy risk. This WP sets `allowBackup="false"`. |
| **Findings Covered** | S-2 |
| **Affected Modules** | `:app` |
| **Affected Files** | `app/src/main/AndroidManifest.xml` |
| **Dependencies** | None |
| **Risk** | Low (configuration change) |
| **Complexity** | Easy |
| **Estimated Time** | 1 hour |
| **Acceptance Criteria** | 1. `allowBackup` is set to `"false"` in manifest. 2. App builds and runs normally. |
| **Validation Method** | Build verification + manual inspection |
| **Loop Engineering** | **Loop E5-01:** Backup Protection — Disable allowBackup. Deliverables: Update manifest attribute. Pre-conditions: None. Post-conditions: No app data backup. |

#### WP-E5-02: Add Runtime Permission Granularity
| Field | Value |
|---|---|
| **Épica** | EPIC-05 |
| **Objective** | Implement proper runtime permission requests for `MEDIA_PROJECTION` and `ACCESSIBILITY_SERVICE` with granularity and rationale. |
| **Description** | Currently permissions may be requested without proper explanation to the user. This WP implements Android's runtime permission API with: (a) rationale dialogs explaining why each permission is needed, (b) separate request flows for each permission, (c) retry logic for denied permissions. |
| **Findings Covered** | S-1.1, S-1.2, S-1.3 |
| **Affected Modules** | `:app`, `:feature:onboarding` |
| **Affected Files** | `app/src/main/.../PermissionManager.kt` (new), `feature/onboarding/.../OnboardingViewModel.kt`, `app/src/main/.../MainActivity.kt` |
| **Dependencies** | None |
| **Risk** | Medium |
| **Complexity** | Medium |
| **Estimated Time** | 6–8 hours |
| **Acceptance Criteria** | 1. Each permission has a rationale dialog shown before request. 2. Denied permissions show a "go to settings" prompt. 3. Permissions are requested one at a time (not batched). 4. All permission states are tracked in a `PermissionState` enum. |
| **Validation Method** | UI test with permission grant/deny scenarios |
| **Loop Engineering** | **Loop E5-02:** Permission Granularity — Runtime Request Flow. Deliverables: Create `PermissionManager`, add rationale flow, integrate into onboarding. Pre-conditions: None. Post-conditions: Clear permission UX. |

#### WP-E5-03: Add Accessibility Service Permission Dialog
| Field | Value |
|---|---|
| **Épica** | EPIC-05 |
| **Objective** | Implement a dedicated screen guiding users to the Accessibility Service settings with clear explanation. |
| **Description** | Enabling the Accessibility Service requires navigating to system settings. This WP adds a dedicated fragment that: (a) explains why the service is needed, (b) provides a button to open system settings, (c) polls for service status and confirms activation. |
| **Findings Covered** | S-3.1, S-3.2 |
| **Affected Modules** | `:feature:onboarding`, `:feature:settings` |
| **Affected Files** | `feature/onboarding/.../AccessibilityPermissionFragment.kt` (new), `feature/onboarding/.../OnboardingViewModel.kt`, `feature/settings/.../SettingsFragment.kt` |
| **Dependencies** | WP-E5-02 |
| **Risk** | Medium |
| **Complexity** | Medium |
| **Estimated Time** | 4–6 hours |
| **Acceptance Criteria** | 1. Fragment explains service purpose clearly. 2. Button opens system Accessibility settings directly. 3. After returning, service is checked and confirmed. 4. If not enabled, user is returned to the permission screen with an error message. |
| **Validation Method** | UI test navigating to settings and returning |
| **Loop Engineering** | **Loop E5-03:** Accessibility Guidance — Dedicated Permission Screen. Deliverables: Create `AccessibilityPermissionFragment`, add settings intent, implement status polling. Pre-conditions: WP-E5-02 complete. Post-conditions: Clear AccessibilityService activation. |

---

### EPIC-06: UX Blockers

#### WP-E6-01: Implement Overlay Permission Request with Rationale
| Field | Value |
|---|---|
| **Épica** | EPIC-06 |
| **Objective** | Add a proper overlay permission request flow with system settings redirect and retry logic. |
| **Description** | The overlay feature requires `SYSTEM_ALERT_WINDOW` permission. This WP replaces any auto-permission flow with a proper dialog explaining why the overlay is needed, a button to open system settings, and verification after return. |
| **Findings Covered** | UX-1.1, UX-1.2 |
| **Affected Modules** | `:feature:onboarding`, `:feature:settings` |
| **Affected Files** | `feature/onboarding/.../OverlayPermissionFragment.kt` (new), `feature/settings/.../OverlaySettingsFragment.kt` (new), `feature/onboarding/.../OnboardingViewModel.kt` |
| **Dependencies** | WP-E5-02 |
| **Risk** | Medium |
| **Complexity** | Medium |
| **Estimated Time** | 4–6 hours |
| **Acceptance Criteria** | 1. Rationale dialog explains overlay purpose. 2. Button opens `SYSTEM_ALERT_WINDOW` settings. 3. After return, permission is verified. 4. If denied, retry option is presented. |
| **Validation Method** | UI test with permission flow |
| **Loop Engineering** | **Loop E6-01:** Overlay Permission — Rationale + Redirect. Deliverables: Create `OverlayPermissionFragment`, add settings redirect, implement verification.  Pre-conditions: WP-E5-02 complete. Post-conditions: Clear overlay permission path. |

#### WP-E6-02: Add Error State and Recovery for MediaProjection
| Field | Value |
|---|---|
| **Épica** | EPIC-06 |
| **Objective** | Display a user-facing error when MediaProjection is unavailable or interrupted, with recovery instructions. |
| **Description** | When the MediaProjection permission is revoked, the notification is dismissed by the system, or the user cancels the projection, the app currently shows no feedback. This WP adds an overlay error state with a message and a "Restart Capture" button. |
| **Findings Covered** | UX-2.1, UX-2.2 |
| **Affected Modules** | `:feature:overlay`, `:core:ui` |
| **Affected Files** | `feature/overlay/.../CaptureErrorOverlay.kt` (new), `feature/overlay/.../CaptureViewModel.kt`, `core/ui/.../ErrorState.kt` (new) |
| **Dependencies** | WP-E2-01 |
| **Risk** | Medium |
| **Complexity** | Medium |
| **Estimated Time** | 4–6 hours |
| **Acceptance Criteria** | 1. Error overlay is shown when MediaProjection fails. 2. Message explains cause in plain language. 3. "Restart Capture" button relaunches the projection intent. 4. Error state persists until user action. |
| **Validation Method** | UI test simulating projection failure |
| **Loop Engineering** | **Loop E6-02:** Error Recovery — MediaProjection Fallback UI. Deliverables: Create `CaptureErrorOverlay`, add recovery button, wire to `CaptureViewModel`. Pre-conditions: WP-E2-01 complete. Post-conditions: Graceful error handling. |

---

### EPIC-07: Presentation Architecture

#### WP-E7-01: Extract OverlayView into Compose Component
| Field | Value |
|---|---|
| **Épica** | EPIC-07 |
| **Objective** | Migrate the overlay display from View system to Jetpack Compose. |
| **Description** | The overlay currently uses `WindowManager` + `FrameLayout` to display a floating view. This WP replaces it with a Compose-based `OverlayWindow` composable hosted in a `ComposeView`, simplifying state management and enabling theming. |
| **Findings Covered** | ARCH-1.x |
| **Affected Modules** | `:feature:overlay`, `:core:ui` |
| **Affected Files** | `feature/overlay/.../OverlayView.kt`, `feature/overlay/.../OverlayWindowManager.kt`, `feature/overlay/.../OverlayContent.kt` (new) |
| **Dependencies** | None |
| **Risk** | High |
| **Complexity** | Hard |
| **Estimated Time** | 10–14 hours |
| **Acceptance Criteria** | 1. Overlay is rendered via `ComposeView`. 2. No View system widgets remain in overlay path. 3. Overlay position and content driven by a single `OverlayState` in Compose. |
| **Validation Method** | UI test verifying overlay rendering + state-driven content |
| **Loop Engineering** | **Loop E7-01:** Compose Migration — Overlay to Declarative. Deliverables: Create `OverlayContent` composable, replace `OverlayView`, host in `ComposeView`. Pre-conditions: None. Post-conditions: Declarative overlay. |

#### WP-E7-02: Implement OverlayState ViewModel as Single Source of Truth
| Field | Value |
|---|---|
| **Épica** | EPIC-07 |
| **Objective** | Create `OverlayStateViewModel` to hold all overlay state (position, visibility, content) as single source of truth. |
| **Description** | Currently overlay state is scattered across `PipelineOverlayDataSource`, `OverlayWindowManager`, and UI callbacks. This WP introduces `OverlayStateViewModel` in `:feature:overlay` — a ViewModel exposing `StateFlow<OverlayUiState>` that all overlay components observe. |
| **Findings Covered** | ARCH-2.1, ARCH-2.2 |
| **Affected Modules** | `:feature:overlay` |
| **Affected Files** | `OverlayStateViewModel.kt` (new), `OverlayContent.kt` (edit), `PipelineOverlayDataSource.kt` (edit) |
| **Dependencies** | WP-E7-01 |
| **Risk** | Medium |
| **Complexity** | Medium |
| **Estimated Time** | 6–8 hours |
| **Acceptance Criteria** | 1. `OverlayStateViewModel` exposes `StateFlow<OverlayUiState>`. 2. `OverlayContent` observes the ViewModel. 3. No direct calls from UI to `PipelineOverlayDataSource`. 4. State survives configuration changes. |
| **Validation Method** | ViewModel unit test + Compose state test |
| **Loop Engineering** | **Loop E7-02:** Single Source of Truth — OverlayStateViewModel. Deliverables: Create `OverlayStateViewModel`, expose `StateFlow`, migrate `OverlayContent` to observe. Pre-conditions: WP-E7-01 complete. Post-conditions: Centralized overlay state. |

#### WP-E7-03: Apply Unidirectional Data Flow in Settings Screen
| Field | Value |
|---|---|
| **Épica** | EPIC-07 |
| **Objective** | Refactor `SettingsFragment` to use unidirectional data flow (UDF) with a `SettingsViewModel`. |
| **Description** | The current settings screen directly reads/writes `SharedPreferences` in the fragment. This WP extracts settings into `SettingsViewModel` with a `StateFlow<SettingsUiState>` and an `onEvent(SettingsEvent)` sink, enabling testability and UDF. |
| **Findings Covered** | ARCH-3.1 |
| **Affected Modules** | `:feature:settings` |
| **Affected Files** | `SettingsFragment.kt`, `SettingsViewModel.kt` (new), `SettingsUiState.kt` (new), `SettingsEvent.kt` (new) |
| **Dependencies** | None |
| **Risk** | Low |
| **Complexity** | Medium |
| **Estimated Time** | 6–8 hours |
| **Acceptance Criteria** | 1. `SettingsViewModel` holds all settings state. 2. `SettingsFragment` observes `StateFlow` and calls `onEvent`. 3. No direct `SharedPreferences` access in fragment. 4. Settings changes trigger recomposition. |
| **Validation Method** | ViewModel unit test + UI test on settings change |
| **Loop Engineering** | **Loop E7-03:** UDF Settings — SettingsViewModel Extraction. Deliverables: Create `SettingsViewModel`, `SettingsUiState`, `SettingsEvent`; refactor `SettingsFragment`. Pre-conditions: None. Post-conditions: Testable, UDF settings. |

---

### EPIC-08: Localization & Design System

#### WP-E8-01: Establish Design System Tokens (Colors, Typography, Spacing)
| Field | Value |
|---|---|
| **Épica** | EPIC-08 |
| **Objective** | Create a centralized design system with color, typography, and spacing tokens. |
| **Description** | Currently `SircColors.kt` only has basic color definitions with potential contrast issues. This WP creates `SircTheme.kt`, `SircTypography.kt`, and `SircDimens.kt` — all in `:core:ui` — establishing a token set that all screens use. WCAG contrast ratios are verified. |
| **Findings Covered** | DS-1.x, DS-2.1, DS-2.2 |
| **Affected Modules** | `:core:ui` |
| **Affected Files** | `SircColors.kt`, `SircTheme.kt` (new), `SircTypography.kt` (new), `SircDimens.kt` (new) |
| **Dependencies** | None |
| **Risk** | Medium |
| **Complexity** | Medium |
| **Estimated Time** | 6–8 hours |
| **Acceptance Criteria** | 1. `SircTheme` combines colors, typography, dimens. 2. All tokens are in `values/` resources and typed accessors. 3. Color contrast meets WCAG AA (4.5:1 for text). 4. All existing screens reference tokens (no hardcoded values). |
| **Validation Method** | Automated contrast check + manual visual inspection |
| **Loop Engineering** | **Loop E8-01:** Design Tokens — Centralized System. Deliverables: Create `SircTheme`, `SircTypography`, `SircDimens`, fix contrast issues. Pre-conditions: None. Post-conditions: Consistent design language. |

#### WP-E8-02: Add Spanish Localization Resources
| Field | Value |
|---|---|
| **Épica** | EPIC-08 |
| **Objective** | Add Spanish strings for all user-facing text in the app. |
| **Description** | Currently only `app_name` exists in `strings.xml`. This WP adds `values-es/strings.xml` with all UI strings translated to Spanish, including onboarding, settings, overlay labels, and error messages. |
| **Findings Covered** | LOC-1.1, LOC-1.2 |
| **Affected Modules** | `:app`, `:feature:onboarding`, `:feature:settings`, `:feature:overlay` |
| **Affected Files** | `app/src/main/res/values/strings.xml`, `app/src/main/res/values-es/strings.xml` (new), module-level string files |
| **Dependencies** | None |
| **Risk** | Low |
| **Complexity** | Easy |
| **Estimated Time** | 3–4 hours |
| **Acceptance Criteria** | 1. All user-facing strings have Spanish translations. 2. `locale` setting switches language at runtime. 3. No hardcoded strings in XML layouts or Kotlin code. 4. QA verifies Spanish strings display correctly. |
| **Validation Method** | Manual QA with device locale set to Spanish |
| **Loop Engineering** | **Loop E8-02:** Localization — Spanish Translation. Deliverables: Add `values-es/strings.xml`, translate all strings, add runtime locale switch. Pre-conditions: None. Post-conditions: Spanish support. |

---

### EPIC-09: Performance Optimization

#### WP-E9-01: Implement ImageProcessor Debounce and Frame Dropping
| Field | Value |
|---|---|
| **Épica** | EPIC-09 |
| **Objective** | Add frame throttling to `ImageProcessor` to prevent CPU overload during high-frequency capture. |
| **Description** | `ImageProcessor` currently processes every captured frame, which can overwhelm the CPU when ride apps update frequently. This WP adds a debounce mechanism: (a) a configurable minimum interval between processed frames (default: 500ms), (b) frame dropping for frames within the interval, (c) a buffer for the most recent frame to avoid stale data. |
| **Findings Covered** | PERF-1.1, PERF-1.2 |
| **Affected Modules** | `:core:capture` |
| **Affected Files** | `core/capture/.../processor/ImageProcessor.kt`, `core/capture/.../processor/FrameThrottler.kt` (new) |
| **Dependencies** | None |
| **Risk** | Medium |
| **Complexity** | Medium |
| **Estimated Time** | 5–6 hours |
| **Acceptance Criteria** | 1. Frames are processed at most every 500ms. 2. Intermediate frames are dropped. 3. Most recent frame is always retained for next process window. 4. Throttle interval is configurable per platform. |
| **Validation Method** | Benchmark test measuring CPU usage before/after |
| **Loop Engineering** | **Loop E9-01:** Frame Throttling — ImageProcessor Debounce. Deliverables: Add `FrameThrottler`, integrate into `ImageProcessor`, add configurability. Pre-conditions: None. Post-conditions: CPU-bound frame processing. |

#### WP-E9-02: Add Memory Pressure Handling in ImageReader Pipeline
| Field | Value |
|---|---|
| **Épica** | EPIC-09 |
| **Objective** | Implement memory pressure handling to clear `ImageReader` buffers when system memory is low. |
| **Description** | `ImageReader` accumulates images in its buffer queue. Under memory pressure, this can cause OOM. This WP adds a `MemoryPressureMonitor` that: (a) registers a `ComponentCallbacks2` for `onTrimMemory`, (b) clears `ImageReader` buffers on `TRIM_MEMORY_RUNNING_LOW`, (c) reduces capture frequency on `TRIM_MEMORY_RUNNING_MODERATE`. |
| **Findings Covered** | PERF-2.1, PERF-2.2 |
| **Affected Modules** | `:core:capture:android` |
| **Affected Files** | `core/capture/android/.../pipeline/ImageReaderPipeline.kt`, `core/capture/android/.../pipeline/MemoryPressureMonitor.kt` (new), `core/capture/android/.../projection/MediaProjectionService.kt` |
| **Dependencies** | WP-E2-01 |
| **Risk** | Medium |
| **Complexity** | Hard |
| **Estimated Time** | 8–10 hours |
| **Acceptance Criteria** | 1. `MemoryPressureMonitor` registered in service `onCreate`. 2. `ImageReader` images cleared on `TRIM_MEMORY_RUNNING_LOW`. 3. Capture interval increases on moderate pressure. 4. No OOM in stress test with 3 concurrent apps. |
| **Validation Method** | Stress test with memory pressure simulation |
| **Loop Engineering** | **Loop E9-02:** Memory Management — Pressure Handling. Deliverables: Create `MemoryPressureMonitor`, integrate `ComponentCallbacks2`, clear buffers on trim. Pre-conditions: WP-E2-01 complete. Post-conditions: Memory-safe capture. |

---

### EPIC-10: Testing Infrastructure

#### WP-E10-01: Add AccessibilityService Lifecycle Test Suite
| Field | Value |
|---|---|
| **Épica** | EPIC-10 |
| **Objective** | Create instrumentation tests covering AccessibilityService event flow and lifecycle. |
| **Description** | The `CaptureAccessibilityService` has no automated tests for its core behavior: event reception, debounce scheduling, and pipeline invocation. This WP adds `CaptureAccessibilityServiceTest` in `:feature:overlay` using `UiAutomation` to dispatch fake window events and verify pipeline invocation. |
| **Findings Covered** | TEST-1.1, TEST-1.2 |
| **Affected Modules** | `:feature:overlay` |
| **Affected Files** | `CaptureAccessibilityServiceTest.kt` (new, in `androidTest`), `DebounceCaptureScheduler.kt` (verify testable), `core/capture/.../pipeline/DefaultCapturePipeline.kt` (verify mockable) |
| **Dependencies** | WP-E1-03 (service consolidation) |
| **Risk** | Low |
| **Complexity** | Hard |
| **Estimated Time** | 8–10 hours |
| **Acceptance Criteria** | 1. Test dispatches `AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED`. 2. Verifies `DebounceCaptureScheduler` receives event. 3. Verifies `DefaultCapturePipeline` is invoked within debounce window. 4. Test covers service `onStart`/`onDestroy` lifecycle. |
| **Validation Method** | Instrumentation test run on emulator |
| **Loop Engineering** | **Loop E10-01:** Service Test Coverage — AccessibilityService Tests. Deliverables: Create `CaptureAccessibilityServiceTest`, add mock pipeline, run on CI. Pre-conditions: WP-E1-03 complete. Post-conditions: Service coverage ≥80%. |

#### WP-E10-02: Add CapturePipeline Unit Test Suite
| Field | Value |
|---|---|
| **Épica** | EPIC-10 |
| **Objective** | Add unit tests for `DefaultCapturePipeline` covering parsing, filtering, and dispatch. |
| **Description** | `DefaultCapturePipeline` orchestrates: image → parser → filter → dispatch. This WP adds `DefaultCapturePipelineTest` in `:core:capture` (JVM test) with mock `PlatformParsers`, fake `ImageProcessor`, and a verifying `CaptureCallback`. |
| **Findings Covered** | TEST-2.1, TEST-2.2, TEST-2.3 |
| **Affected Modules** | `:core:capture` |
| **Affected Files** | `DefaultCapturePipelineTest.kt` (new, in `test`), `DefaultCapturePipeline.kt` (verify injectable dependencies) |
| **Dependencies** | WP-E1-01, WP-E1-02 |
| **Risk** | Low |
| **Complexity** | Medium |
| **Estimated Time** | 5–6 hours |
| **Acceptance Criteria** | 1. Test injects 3 sample `AccessibilityWindowInfo` trees. 2. Verifies parser is called with correct node. 3. Verifies filter rejects non-matching windows. 4. Verifies callback receives expected `CaptureResult`. 5. Coverage ≥80% on `DefaultCapturePipeline`. |
| **Validation Method** | Unit test run via Gradle |
| **Loop Engineering** | **Loop E10-02:** Pipeline Test Coverage — Unit Tests. Deliverables: Create `DefaultCapturePipelineTest`, mock dependencies, assert results. Pre-conditions: WP-E1-01 & WP-E1-02 complete. Post-conditions: Pipeline coverage ≥80%. |

---

### EPIC-11: Documentation Integrity

#### WP-E11-01: Create API Reference Documentation
| Field | Value |
|---|---|
| **Épica** | EPIC-11 |
| **Objective** | Generate KDoc-based API reference for all public classes in `:core:capture`, `:core:platform`, and `:feature:overlay`. |
| **Description** | Many public classes lack meaningful KDoc comments. This WP adds comprehensive KDoc to all public APIs, including `@param`, `@return`, `@throws`, and usage examples. Documentation is verified to generate cleanly via Dokka. |
| **Findings Covered** | DOC-1.x |
| **Affected Modules** | `:core:capture`, `:core:platform`, `:feature:overlay` |
| **Affected Files** | All public Kotlin files in these modules (KDoc additions) |
| **Dependencies** | None (can run in parallel with any WP) |
| **Risk** | Low |
| **Complexity** | Medium |
| **Estimated Time** | 8–12 hours |
| **Acceptance Criteria** | 1. All public classes, methods, and properties have KDoc. 2. Dokka generates HTML without warnings. 3. Examples are valid Kotlin. 4. Documentation includes module-level descriptions. |
| **Validation Method** | Dokka build verification + manual review |
| **Loop Engineering** | **Loop E11-01:** API Documentation — KDoc Coverage. Deliverables: Add KDoc to all public APIs, run Dokka, verify output. Pre-conditions: None. Post-conditions: Complete API docs. |

#### WP-E11-02: Create Onboarding Architecture Decision Record (ADR)
| Field | Value |
|---|---|
| **Épica** | EPIC-11 |
| **Objective** | Document the decision to adopt Unidirectional Data Flow (UDF) in the onboarding flow. |
| **Description** | The onboarding flow uses a mix of fragment-to-fragment navigation and direct state mutation. This WP creates an ADR documenting: (a) the problem (hard to test navigation logic), (b) the decision (adopt UDF with `OnboardingViewModel` and `StateFlow`), (c) consequences (testable, single source of truth), (d) alternatives considered. |
| **Findings Covered** | DOC-2.1 |
| **Affected Modules** | `:feature:onboarding` |
| **Affected Files** | `docs/adr/001-onboarding-udf.md` (new), `feature/onboarding/.../OnboardingViewModel.kt` (verify UDF applied) |
| **Dependencies** | None |
| **Risk** | Low |
| **Complexity** | Easy |
| **Estimated Time** | 2–3 hours |
| **Acceptance Criteria** | 1. ADR exists in `docs/adr/`. 2. Follows MADR format (Problem, Decision, Consequences). 3. References the implementing code. 4. Reviewed by at least one senior engineer. |
| **Validation Method** | Peer review + ADR template check |
| **Loop Engineering** | **Loop E11-02:** ADR Creation — Onboarding UDF. Deliverables: Write ADR-001, link to implementation, request review. Pre-conditions: None. Post-conditions: Decision documented. |

---

### EPIC-12: Stability Hardening

#### WP-E12-01: Add Crash Reporting with Debug Info
| Field | Value |
|---|---|
| **Épica** | EPIC-12 |
| **Objective** | Integrate crash reporting that captures debug info (device state, last window, pipeline status) at crash time. |
| **Description** | Currently crashes produce only a stack trace. This WP: (a) integrates a crash reporting library (or custom handler if no external deps allowed), (b) captures `DebugInfo` (density, screen size, last processed window text, pipeline state), (c) attaches debug info to crash reports, (d) ensures crash data is not uploaded without user consent. |
| **Findings Covered** | STAB-1.1, STAB-1.2 |
| **Affected Modules** | `:app`, `:core:capture` |
| **Affected Files** | `app/src/main/.../SircCrashHandler.kt` (new), `core/capture/.../pipeline/DebugInfoProvider.kt` (new), `app/src/main/AndroidManifest.xml` (add meta-data) |
| **Dependencies** | None |
| **Risk** | Medium |
| **Complexity** | Medium |
| **Estimated Time** | 6–8 hours |
| **Acceptance Criteria** | 1. `Thread.setDefaultUncaughtExceptionHandler` is set in `Application.onCreate`. 2. Debug info is captured on crash. 3. Crash log is saved to internal storage (not auto-uploaded). 4. User can manually send crash log from Settings. |
| **Validation Method** | Manual crash test + log file inspection |
| **Loop Engineering** | **Loop E12-01:** Crash Reporting — Debug Info Capture. Deliverables: Create `SircCrashHandler`, `DebugInfoProvider`, add manual upload. Pre-conditions: None. Post-conditions: Crash reports include debug context. |

#### WP-E12-02: Add Graceful Shutdown for Long-Running Capture
| Field | Value |
|---|---|
| **Épica** | EPIC-12 |
| **Objective** | Ensure capture operations shut down gracefully when the app is backgrounded or killed. |
| **Description** | When the app is backgrounded, the `CaptureAccessibilityService` and `MediaProjectionService` may leave partial state. This WP: (a) adds `onTrimMemory` handling to shut down capture when backgrounded, (b) saves pending capture results to the database before shutdown, (c) cancels in-flight pipeline operations cleanly. |
| **Findings Covered** | STAB-2.1, STAB-2.2 |
| **Affected Modules** | `:feature:overlay`, `:core:capture:android` |
| **Affected Files** | `feature/overlay/.../CaptureAccessibilityService.kt`, `core/capture/android/.../projection/MediaProjectionService.kt`, `core/capture/.../pipeline/DefaultCapturePipeline.kt` |
| **Dependencies** | WP-E2-01, WP-E1-03 |
| **Risk** | Medium |
| **Complexity** | Medium |
| **Estimated Time** | 8–10 hours |
| **Acceptance Criteria** | 1. `onTrimMemory` in service triggers graceful shutdown. 2. Pending results saved to DB. 3. `ImageReader` and `MediaProjection` released. 4. No data loss when app killed mid-capture. |
| **Validation Method** | Instrumented test: background app mid-capture, verify DB state after relaunch |
| **Loop Engineering** | **Loop E12-02:** Graceful Shutdown — Backgrounded Capture. Deliverables: Add trim memory handling, persist state, clean up resources. Pre-conditions: WP-E2-01 & WP-E1-03 complete. Post-conditions: No data loss on force-stop. |

---

## 3. Summary Table

| Épica | WP Count | Total Est. Hours | Critical Path |
|---|---|---|---|
| EPIC-01 | 3 | 26–36 | Yes (foundational pipeline) |
| EPIC-02 | 2 | 12–16 | Yes (resource leaks) |
| EPIC-03 | 2 | 16–22 | Yes (platform detection) |
| EPIC-04 | 2 | 13–18 | Yes (decision logic) |
| EPIC-05 | 3 | 11–15 | No |
| EPIC-06 | 2 | 8–12 | No |
| EPIC-07 | 3 | 22–28 | Yes (presentation layer) |
| EPIC-08 | 2 | 9–12 | No |
| EPIC-09 | 2 | 13–16 | Yes (performance) |
| EPIC-10 | 2 | 13–16 | No |
| EPIC-11 | 2 | 10–15 | No |
| EPIC-12 | 2 | 14–18 | Yes (stability) |
| **Total** | **33** | **191–259** | |

---

## 4. Loop Engineering Overview

Each WP is designed to be executed by a **single Loop Engineering instance**. Loops are independent units of work with:

- **Pre-conditions**: WPs that must complete before this WP starts.
- **Post-conditions**: Verified state after completion.
- **Deliverables**: Concrete, testable outputs.
- **Validation**: Specific verification steps.

**Total Loops:** 33  
**Max parallelism:** 8 concurrent loops (see LOOP_EXECUTION_ORDER.md)  
**Critical loops:** E1-01, E2-01, E3-01, E4-01, E7-01, E9-01, E12-02 (must not be delayed)

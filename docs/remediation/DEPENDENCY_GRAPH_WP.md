# Dependency Graph — Work Packages

**Date:** 2026-08-02  
**Scope:** Dependencies between 33 Work Packages across 12 Épicas  
**Format:** Mermaid.js graph (LR layout for horizontal reading)

---

## Full WP Dependency Graph

```mermaid
graph LR
    subgraph "EPIC-01: Capture Pipeline"
        E1_01["WP-E1-01<br/>Eliminate FakeParser"]
        E1_02["WP-E1-02<br/>Unify Dual Engine Logic"]
        E1_03["WP-E1-03<br/>Merge Accessibility Services"]
    end

    subgraph "EPIC-02: MediaProjection Lifecycle"
        E2_01["WP-E2-01<br/>Add onDestroy() Cleanup"]
        E2_02["WP-E2-02<br/>Handle Config Changes"]
    end

    subgraph "EPIC-03: Platform-Agnostic Detection"
        E3_01["WP-E3-01<br/>Extract Platform Config"]
        E3_02["WP-E3-02<br/>Generic Detection Framework"]
    end

    subgraph "EPIC-04: Decision Engine"
        E4_01["WP-E4-01<br/>Unify ProfitEngine+RuleEngine"]
        E4_02["WP-E4-02<br/>Add Decision Logging"]
    end

    subgraph "EPIC-05: Security & Privacy"
        E5_01["WP-E5-01<br/>Remove allowBackup"]
        E5_02["WP-E5-02<br/>Runtime Permissions"]
        E5_03["WP-E5-03<br/>Accessibility Permission Dialog"]
    end

    subgraph "EPIC-06: UX Blockers"
        E6_01["WP-E6-01<br/>Overlay Permission Request"]
        E6_02["WP-E6-02<br/>MediaProjection Error State"]
    end

    subgraph "EPIC-07: Presentation"
        E7_01["WP-E7-01<br/>Extract OverlayView to Compose"]
        E7_02["WP-E7-02<br/>OverlayState ViewModel"]
        E7_03["WP-E7-03<br/>UDF in Settings"]
    end

    subgraph "EPIC-08: Design System"
        E8_01["WP-E8-01<br/>Design System Tokens"]
        E8_02["WP-E8-02<br/>Spanish Localization"]
    end

    subgraph "EPIC-09: Performance"
        E9_01["WP-E9-01<br/>ImageProcessor Debounce"]
        E9_02["WP-E9-02<br/>Memory Pressure Handling"]
    end

    subgraph "EPIC-10: Testing"
        E10_01["WP-E10-01<br/>AccessibilityService Tests"]
        E10_02["WP-E10-02<br/>CapturePipeline Unit Tests"]
    end

    subgraph "EPIC-11: Documentation"
        E11_01["WP-E11-01<br/>API Reference Docs"]
        E11_02["WP-E11-02<br/>Onboarding ADR"]
    end

    subgraph "EPIC-12: Stability"
        E12_01["WP-E12-01<br/>Crash Reporting"]
        E12_02["WP-E12-02<br/>Graceful Shutdown"]
    end

    %% Dependencies for EPIC-01
    E1_02 -->|depends on| E1_01
    E1_03 -->|depends on| E1_01
    E1_03 -->|depends on| E1_02

    %% Dependencies for EPIC-02
    E2_02 -->|depends on| E2_01

    %% Dependencies for EPIC-03
    E3_02 -->|depends on| E3_01

    %% Dependencies for EPIC-04
    E4_01 -->|depends on| E1_02
    E4_02 -->|depends on| E4_01

    %% Dependencies for EPIC-05
    E5_03 -->|depends on| E5_02
    E6_01 -->|depends on| E5_02

    %% Dependencies for EPIC-06
    E6_02 -->|depends on| E2_01

    %% Dependencies for EPIC-07
    E7_02 -->|depends on| E7_01

    %% Dependencies for EPIC-09
    E9_02 -->|depends on| E2_01

    %% Dependencies for EPIC-10
    E10_01 -->|depends on| E1_03
    E10_02 -->|depends on| E1_01
    E10_02 -->|depends on| E1_02

    %% Dependencies for EPIC-12
    E12_02 -->|depends on| E2_01
    E12_02 -->|depends on| E1_03

    %% Cross-cutting dependencies (E11 docs are independent, E12-01 is independent)
    E11_01 -.->|independent| E1_01
    E11_01 -.->|independent| E1_02
    E11_01 -.->|independent| E3_01
    E11_02 -.->|independent| E7_03
    E12_01 -.->|independent| E7_03
    E5_01 -.->|independent| E1_01
    E8_01 -.->|independent| E7_01
    E8_02 -.->|independent| E8_01
    E9_01 -.->|independent| E1_02

    classDef independent fill:#e8f5e9,stroke:#388e3c
    classDef critical fill:#ffebee,stroke:#d32f2f
    classDef high fill:#fff3e0,stroke:#f57c00
    classDef medium fill:#e3f2fd,stroke:#1976d2
    classDef low fill:#f3e5f5,stroke:#7b1fa2

    class E1_01,E2_01,E3_01,E4_01,E7_01,E9_01 critical
    class E1_02,E2_02,E3_02,E5_02,E7_02,E9_02,E12_02 high
    class E1_03,E5_03,E6_01,E6_02,E7_03,E8_01,E10_01,E10_02,E12_01 medium
    class E4_02,E5_01,E8_02,E11_01,E11_02 independent
```

---

## Critical Path

### Longest Chain (56 hours)

```mermaid
graph LR
    A["WP-E1-01<br/>Eliminate FakeParser<br/>8h"] --> B["WP-E1-02<br/>Unify Dual Engine<br/>12h"]
    B --> C["WP-E1-03<br/>Merge Accessibility Services<br/>16h"]
    C --> D["WP-E10-01<br/>AccessibilityService Tests<br/>10h"]
    D --> E["WP-E12-02<br/>Graceful Shutdown<br/>10h"]

    classDef critical fill:#ffebee,stroke:#d32f2f
    class A,B,C,D,E critical
```

### Secondary Chain (26 hours)

```mermaid
graph LR
    A["WP-E2-01<br/>onDestroy Cleanup<br/>6h"] --> B["WP-E2-02<br/>Config Changes<br/>10h"]
    B --> C["WP-E12-02<br/>Graceful Shutdown<br/>10h"]

    classDef critical fill:#ffebee,stroke:#d32f2f
    class A,B,C critical
```

---

## Convergence Point

```mermaid
graph LR
    A["WP-E1-03<br/>Service Merge<br/>EPIC-01"] --> C["WP-E12-02<br/>Graceful Shutdown<br/>EPIC-12"]
    B["WP-E2-01<br/>onDestroy<br/>EPIC-02"] --> C

    classDef converge fill:#fff3e0,stroke:#f57c00
    class A,B,C converge
```

**WP-E12-02 depends on both WP-E1-03 and WP-E2-01** — this is the convergence point where EPIC-01 and EPIC-02 merge.

---

## Parallelization Opportunities

### Week 1 — 11 Independent WPs (Peak Parallelism)

```mermaid
graph LR
    subgraph "Week 1 — Fully Independent"
        A["WP-E1-01<br/>Core:capture<br/>High"]
        B["WP-E2-01<br/>Core:capture:android<br/>Critical"]
        C["WP-E3-01<br/>Core:platform<br/>Medium"]
        D["WP-E5-01<br/>App<br/>Low"]
        E["WP-E7-03<br/>Feature:settings<br/>Low"]
        F["WP-E8-01<br/>Core:ui<br/>Medium"]
        G["WP-E8-02<br/>Multiple<br/>Low"]
        H["WP-E9-01<br/>Core:capture<br/>Medium"]
        I["WP-E11-01<br/>Docs<br/>Low"]
        J["WP-E11-02<br/>Docs<br/>Low"]
        K["WP-E12-01<br/>App/capture<br/>Medium"]
    end

    subgraph "Week 2 — Dependent on Week 1"
        L["WP-E1-02<br/>Depends on E1-01"]
        M["WP-E5-02<br/>Independent"]
        N["WP-E7-01<br/>Independent"]
        O["WP-E10-02<br/>Depends on E1-01, E1-02"]
    end

    subgraph "Week 3 — Chained Dependencies"
        P["WP-E1-03<br/>Depends on E1-01, E1-02"]
        Q["WP-E2-02<br/>Depends on E2-01"]
        R["WP-E3-02<br/>Depends on E3-01"]
        S["WP-E4-01<br/>Depends on E1-02"]
        T["WP-E6-01<br/>Depends on E5-02"]
        U["WP-E7-02<br/>Depends on E7-01"]
        V["WP-E9-02<br/>Depends on E2-01"]
        W["WP-E10-01<br/>Depends on E1-03"]
    end

    subgraph "Week 4 — Convergence"
        X["WP-E4-02<br/>Depends on E4-01"]
        Y["WP-E5-03<br/>Depends on E5-02"]
        Z["WP-E6-02<br/>Depends on E2-01"]
        AA["WP-E12-02<br/>Depends on E1-03, E2-01"]
    end

    classDef week1 fill:#e8f5e9
    classDef week2 fill:#e3f2fd
    classDef week3 fill:#fff3e5
    classDef week4 fill:#ffebee

    class A,B,C,D,E,F,G,H,I,J,K week1
    class L,M,N,O week2
    class P,Q,R,S,T,U,V,W week3
    class X,Y,Z,AA week4
```

---

## Module-Level Dependency Heatmap

| Module | WPs | Internal Dependencies | External Dependencies |
|---|---|---|---|
| `core:capture` | E1-01, E1-02, E4-01, E4-02, E9-01, E10-02 | E1-02→E1-01, E4-01→E1-02, E4-02→E4-01, E10-02→E1-01+E1-02 | E1-01→app (no) |
| `core:capture:android` | E2-01, E2-02, E9-02, E12-02 | E2-02→E2-01, E9-02→E2-01, E12-02→E2-01 | E12-02→E1-03 (feature:overlay) |
| `core:platform` | E3-01, E3-02 | E3-02→E3-01 | None |
| `core:ui` | E8-01, E6-02 | — | E6-02→E2-01 |
| `feature:overlay` | E1-03, E6-02, E7-01, E7-02, E10-01, E12-02 | E1-03→E1-02, E7-02→E7-01, E10-01→E1-03, E12-02→E1-03 | None |
| `feature:onboarding` | E5-02, E5-03, E6-01, E11-02 | E5-03→E5-02, E6-01→E5-02 | None |
| `feature:settings` | E7-03 | — | None |
| `feature:history` | — | — | None |
| `app` | E5-01, E12-01 | — | None |
| `data` | — | — | None |
| `domain` | — | — | None |

---

## Legend

| Symbol | Meaning |
|---|---|
| `──→` | Direct dependency (must complete before) |
| `-.->` | Soft/independent relationship |
| 🔴 Critical | Must not be delayed — blocks downstream work |
| 🟠 High | Should prioritize after critical |
| 🟡 Medium | Can parallelize with other medium/low |
| 🟢 Low | Independent, can run anytime |
| ⚪ Independent | No dependencies at all |

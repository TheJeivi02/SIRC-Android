# G8 — Auditoría de textos fantasma / obsoletos (20-ago-2026)

> LOOP ENGINEERING G8. Auditoría repo-wide de textos que describen
> comportamiento que ya no existe, en la app o en documentación operativa.
> Estado: **COMPLETADO** — 1 ghost text corregido (clase C), 2 textos obsoletos
> corregidos (clase B), resto clasificado y justificado. Commit + push a `main`.

## Criterio usado

Ghost text = texto accesible (usuario o documento operativo) que:
1. describe un comportamiento que ya no existe, y
2. induce a interpretación incorrecta (promete algo que nunca ocurre).

NO son ghost text los estados reales ("Esperando oferta…", "OCR no disponible",
"Sin snapshots todavía", etc.) ni los registros históricos contextualizados.

## Clasificación

- **A** legítimo (no tocar)
- **B** obsoleto visible (corregir)
- **C** engañoso (corregir)
- **D** histórico contextualizado (conservar)
- **E** pendiente de contexto (ninguno quedó)

## Textos corregidos

### C1 — Ghost text en "Análisis" del Debug Panel (clase C, eliminado)

| Campo | Valor |
|---|---|
| Texto | "Sin reglas evaluadas todavía. Cuando el pipeline analice una oferta real, cada regla (ganancia, por km, por hora, distancia, recogida, duración) aparece con su veredicto." |
| Ubicación | `app/src/main/kotlin/com/sirc/app/DebugPanelScreen.kt` (sección "Análisis") |
| Acción | Eliminado el bloque completo `if (state.ruleResults.isEmpty()) { … } else { … }` (estado vacío + render de filas) y el cableado muerto asociado |
| Motivo | Desde WP-E1-02 (SPRINT 11) `RuleEngine` salió de la ruta de producción y `ruleEvaluation` se expone **siempre** vacío: `PipelineOverlayDataSource.kt:234` → `Analysis(offerTypeFrom(...), RuleEvaluation(emptyList()), confidence)`. El texto prometía veredictos por regla que **no pueden aparecer nunca**. La garantía de vacío ya está cubierta por `PipelineOverlayDataSourceTest.kt:175-177` (`results.isEmpty()`). |

Cableado eliminado (dead code del ghost):
- `DebugPanelScreen.kt`: import `RuleVerdict`, función `verdictColor`.
- `DebugPanelViewModel.kt`: data class `RuleRow`, campo `ruleResults` de
  `UiState`, mapeo `overlayUi.ruleEvaluation?.results?.map { … }` y asignación
  `ruleResults = ruleResults`, import `RuleVerdict`.

### B1 — Fila "Reglas fallidas" del modo validación (clase B, eliminada)

| Campo | Valor |
|---|---|
| Texto | "Reglas fallidas" (fila con contador siempre `0`) |
| Ubicación | `app/src/main/kotlin/com/sirc/app/DebugPanelScreen.kt` (sección "Modo validación") |
| Acción | Eliminada la `LabeledValue` |
| Motivo | Ningún código de producción registra `ValidationEvent.RuleFailed` desde WP-E1-02 (sin productor); el contador es permanentemente `0` y referenciaba al `RuleEngine` eliminado. |

### B2 — Línea "Reglas fallidas" del informe de validación (clase B, eliminada)

| Campo | Valor |
|---|---|
| Texto | "Reglas fallidas: ${summary.ruleFailed}" |
| Ubicación | `core/capture/src/main/kotlin/com/sirc/capture/validation/ValidationRecorder.kt` (`buildReport`) |
| Acción | Eliminada la línea del informe exportado ("Exportar informe de validación") |
| Motivo | Mismo texto obsoleto visible vía export desde la app. Se conserva `ValidationEvent.RuleFailed`/`ValidationSummary.ruleFailed` como API del framework de validación (soportada y testeada en `ValidationRecorderTest`), sin productor en la ruta actual; no se tocó comportamiento. |

## Verificación

- Suite completa AGENTS (ktlintCheck / lintDebug / assembleDebug /
  testDebugUnitTest / `:domain:test` / `:core:platform:test` /
  `:core:capture:test` / `:feature:overlay:testDebugUnitTest`) →
  **BUILD SUCCESSFUL** (tras C1+B1 y tras B2).
- No se modificaron androidTest ni configuración de build → no aplica la
  verificación de compilación androidTest del plan §16.
- **Tests (§13 del plan)**: no se añadió test nuevo. Justificación: (1) la
  garantía de "no hay veredictos de reglas" ya está cubierta por
  `PipelineOverlayDataSourceTest.kt:175-177`; (2) ningún test existente
  referenciaba los textos eliminados; (3) un androidTest de `DebugPanel`
  exigiría un scaffolding de DI desproporcionado (9 dependencias de
  `DebugPanelViewModel`) para una eliminación de texto; no se acopla un test a
  cada string.
- **Riesgo de regresión: no se introdujo ninguno** (solo textos/dead UI). No
  se tocaron ProfitEngine, detección, parser, OCR, CapturePipeline, Overlay
  ni configuración → no aplica batería de regresión física (§14/§15).

## Matriz de clasificación del resto (sin cambios)

| Texto / referencias | Ubicación | Clase | Motivo |
|---|---|---|---|
| "Esperando oferta…" / "Analizando oferta…" / ERROR_MESSAGE overlay | `OverlayContent.kt` | A | Estados reales del pipeline |
| "Aún no hay ofertas evaluadas." | `HistoryScreen.kt` | A | Estado vacío real |
| "token de proyección no disponible" | `MediaProjectionScreenCaptureProvider` | A | Error real |
| "no hay imágenes de prueba en assets" | `DebugImageOcrReceiver` (source set `debug`) | A | Build debug, legítimo |
| "Pantalla simulada" | `core/capture/src/test/resources/test-images/README.md` | A | Fixture de test legítimo |
| `SnapshotSource.FAKE` | pipeline/model | A | Solo tests; producción usa `REAL` |
| "Reglas: X ms" / "Tiempo en reglas" | DebugPanel / HistoryScreen | A | `rulesMillis` se sigue midiendo real (`PipelineOverlayDataSource.kt:144`) |
| "última oferta evaluada" | `DiagnosisScreen.kt` | A | Corregido en FASE 12; verificado intacto |
| `ruleEvaluation` vacío + KDoc "compatibilidad UI" | `PipelineOverlayDataSource.kt`, `OverlayUiState.kt` | A | Documenta el estado real; no es texto de usuario |
| "Reglas fallidas: 0" conservado en… | — | — | (ninguno pendiente; B1/B2 cubrieron los visibles) |
| Referencias `RuleEngine`/`FakeParser`/`SircAccessibilityService`/`SimulatedOverlayDataSource` | `.ai/DECISIONS.md`, `.ai/CONTEXT.md` ("Nota SPRINT x" y "SPRINT 2 completado"), `docs/CHANGELOG.md`, `docs/ROADMAP.md`, `docs/TEST_REPORT.md`, `docs/audit/**`, `docs/remediation/**`, `docs/testing/SPRINT_04/05/10_MANUAL_TEST.md`, `TASK.md` (entradas históricas) | D | Registros históricos/auditoría/planificación contextualizados; ninguno describe la arquitectura como vigente |
| "simulad/datos de prueba/oferta de prueba/dummy/placeholder" | docs históricas (TASK FASE 12 cerrada, DECISIONS SPRINT 2, CONTEXT notas) | D | Histórico contextualizado |
| "cada 20 segundos" | `CONTEXT.md:711` (bloque "SPRINT 2 completado") | D | Histórico; la corrección FASE 12 fue en código |
| `compose-ui-tooling-preview` | `gradle/libs.versions.toml` | A | Dependencia de build legítima |
| "Reglas" fila en "Última oferta" (detalle histórico) | `HistoryScreen.kt:378` (`ruleSummary` con `takeIf { isNotBlank() }`) | A | Guardada por contenido; `ruleSummary` queda vacío hoy (sin falsa promesa visible) |

## Resultado

- **Corregidos**: 1 ghost text (C) + 2 textos obsoletos (B).
- **Conservados**: todos los hallazgos históricos (D) y legítimos (A).
- **Sin cambios de comportamiento**: solo eliminación de texto/UI muerta y
  dead code asociado. 3 archivos, 53 líneas eliminadas.
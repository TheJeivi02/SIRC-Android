# Informe de pruebas — v1.0.0-rc1

> Resultado de la verificación de RC1 (Sprint 10). Se actualiza tras cada ronda
> de pruebas en dispositivos reales (Android 10–15).

## Resumen

| Área | Estado |
|---|---|
| ktlint (todos los módulos) | ✅ En verde |
| Lint Android (`lintDebug`) | ✅ En verde |
| Compilación Debug (`assembleDebug`) | ✅ En verde |
| Compilación de instrumentados (`assembleDebugAndroidTest`) | ✅ En verde |
| Tests unitarios | ✅ En verde |
| Pruebas en dispositivo (rotación, split, batería, memoria) | ⏳ Pendiente (plan manual) |

## Tests unitarios ejecutados

Comandos (Windows/PowerShell):

```powershell
.\gradlew.bat ktlintCheck
.\gradlew.bat lintDebug assembleDebug assembleDebugAndroidTest
.\gradlew.bat :domain:test :core:platform:test :core:capture:test :data:testDebugUnitTest :feature:overlay:testDebugUnitTest :app:testDebugUnitTest
```

### `:core:capture` (nuevos en RC1)

- **`ValidationRecorderTest`**: inicio vacío, resumen por tipo (incluye
  `CaptureError`), buffer acotado a 500 eventos recientes, `clear`, `buildReport`
  con resumen + detalle.
- **`DefaultCapturePipelineTest`** (ampliado):
  - Fallo de OCR degrada a textos de accesibilidad y registra `OcrFailed`
    (comportamiento resiliente nuevo; antes pasaba a `ERROR`).
  - Fallo de captura registra descarte `CAPTURE_FAILED`.
  - Frame duplicado registra descarte `DUPLICATE`.
  - Fallo no controlado registra `CaptureError`.
  - **Stress de 200 solicitudes** mantiene buffers acotados (tracker ≤ 100,
    repositorio ≤ 50, validación ≤ 500).

### `:feature:overlay`

- **`PipelineOverlayDataSourceTest`** (ampliado con `ValidationRecorder`):
  estado inicial, transiciones `WAITING`/`ERROR`, evaluación con
  recomendación ACCEPT, persistencia en historial temporal y Room, tipo/
  confianza/reglas, stop, sesión, flag `OVERLAY`.

### Resto de módulos (regresión, sin cambios funcionales)

- `:domain`: `ProfitEngineTest`, `ProfitEvaluationEngineTest`,
  `RecommendationEngineTest`, `RuleEngineTest`, `ConfidenceEngineTest`,
  `OfferValidatorTest`, `CaptureSessionManagerTest`, `HistoryFilterTest`,
  `HistoryStatsCalculatorTest`.
- `:core:platform`: `OfferTextParserTest`, `OfferParserOrchestratorTest`,
  `OfferDetectionEngineTest`.
- `:core:ui`: `ProfitStateTest`.
- `:data`: `DriverConfigCodecTest` (unitarios); `OfferHistoryDaoTest` y
  `SircDatabaseMigrationTest` (androidTest, migración v1→v3).

## Cobertura de objetivos RC1

| Objetivo | Evidencia |
|---|---|
| O1 Auditoría | Sin TODO/FIXME/XXX/HACK; eliminado flujo legacy (historial duplicado); build verde |
| O2/O5 Performance/consumo | `docs/PERFORMANCE_REPORT.md`; buffers acotados verificados por el test de stress |
| O3 Modo validación | `ValidationRecorderTest` + sección en Debug + exportar informe |
| O4 Compatibilidad | `WindowMetrics` en `OverlayService` (API 30+/24–29); targetSdk 35 |
| O6 Crash recovery | Pipeline resiliente a OCR; `CaptureError` en pipeline/MediaProjection |
| O7 Logs por niveles | Niveles ERROR/WARNING/INFO/DEBUG; apagado automático en Release |
| O9 Tests | Recorder, pipeline de validación, stress |
| O10 Docs | `RELEASE_NOTES_RC1`, `KNOWN_ISSUES`, `PERFORMANCE_REPORT`, `TEST_REPORT` |

## Plan manual en dispositivo (Android 10–15)

Ejecutar los pasos de `docs/testing/BETA_TEST_PLAN.md` y
`docs/testing/SPRINT_09_MANUAL_TEST.md`, y registrar:

1. **Rotación / split screen**: el overlay se reajusta (reclamp) y el virtual
   display se recrea (`onDisplayConfigChanged`) sin duplicar la vista.
2. **Recuperación**: matar la app / revocar accesibilidad → al reactivarla los
   servicios y el overlay se restablecen (accesibilidad la reinicia el sistema;
   overlay `START_STICKY`).
3. **Modo validación**: con ofertas rechazadas y descartes, exportar el informe y
   comprobar que los contadores y el detalle son coherentes.
4. **Batería/memoria**: sesión de 30 min con captura de pantalla; anotar
   consumo y memoria aproximada del panel.
5. **Rendimiento**: 20+ ofertas y comprobar objetivo <3 s
   (`docs/PERFORMANCE_REPORT.md`).

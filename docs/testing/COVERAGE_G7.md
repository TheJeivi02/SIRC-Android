# G7 — Cobertura crítica UI + OCR (20-ago-2026)

> LOOP ENGINEERING G7 + G10. Estado final: **VERIFIED** — la lógica
> determinista queda cubierta con unit tests (G7) y los componentes
> DEVICE_REQUIRED se validan con infraestructura instrumentada en dispositivo
> (G10). Detalle de G10 en `docs/testing/G10_INSTRUMENTED_VALIDATION.md`.

## Clasificación

| COVERED (tests deterministas) | INSTRUMENTED / DEVICE_VALIDATED (G10) | NOT_COVERED |
|---|---|---|
| `mapToOverlayPresentation` (mapper, 26 tests) | `OverlayContent` render Compose: **6 casos** `OverlayContentTest` (device, A–F) | Precisión/rendimiento OCR real multi-dispositivo (Android 10–15) |
| `OverlayController` (start/stop/onServiceRunning, 4) | `OverlayService`: **smoke** FGS + ventana `TYPE_APPLICATION_OVERLAY` + stop (device, `:app`) | — |
| `OverlayConfig.activeIndicatorCount` (3) | `MlKitOcrEngine` real: **13/13 imágenes** procesadas, tiempos 60–238 ms, decode inválido → `emptyList()` | — |
| `PipelineOverlayDataSource` (evaluación, offerType, historial, persistencia) | `OverlayService.kt:247` FLAG_NOT_TOUCHABLE (validación física FASE 15) | — |
| Contrato `OcrEngine` en el pipeline (`DefaultCapturePipelineTest`, 15): imagen→OCR→snapshot, flag OCR off, **OCR vacío→NO_TEXTS sin inventar**, **error OCR→OcrFailed + degradación sin crash** | Overlay real sobre otras apps (físico, FIX-01/FASE 15) | — |

## OCR — `MlKitOcrEngine`

- **Contrato** (`OcrEngine`): `suspend fun recognize(imageData): List<String>`. Consumido por
  `DefaultCapturePipeline.resolveTexts` (try/catch → `OcrFailed` + fallback a textos de
  accesibilidad).
- **Cubierto (JVM, contrato)**: resultado válido (imagen→OCR→snapshot), flag OCR desactivado
  (no se invoca), **resultado vacío** → `NO_TEXTS` sin fabricar datos, **error** → evento
  `OcrFailed` + degradación sin crash (nuevos tests de G7).
- **Device validado (G10, DEVICE-01 / Android 15)**: ML Kit real inicializa y procesa las 13
  imágenes del dataset sin excepción, devolviendo resultado (1–4 líneas por imagen); tiempos
  por imagen 60–238 ms (cold start), total dataset 1124 ms; decode inválido → `emptyList()`.
  Evidencia: logcat `G10OcrTest` (matriz en `docs/testing/G10_INSTRUMENTED_VALIDATION.md`).
- **Precisión = NOT_VALIDATED**: el dataset `core/capture/src/test/resources/test-images/` son
  marcadores del pipeline (README lo declara), no fixtures de precisión OCR. La precisión real
  requiere dataset etiquetado + multi-dispositivo.

## Overlay — estados críticos

| Estado | Test |
|---|---|
| Sin oferta / no disponible | **`sin evaluacion el mapper no fabrica presentacion`** (nuevo, mapper→null) + `OverlayContent` StatusLabel (Compose, device) |
| Oferta válida | `las metricas se agrupan en pares`, `la oferta expone plataforma y monto` |
| showDecision true/false | G6 (sin duplicar): 2 tests del mapper |
| compactMode | `compactMode no altera los indicadores activos` |
| Configuración parcial (toggles) | `las metricas respetan los flags desactivados`, `OverlayConfigTest` |
| Datos faltantes (no inventar) | `sin distancia...`, `sin duracion...`, `oferta solo precio no muestra metricas inventadas`, caso real 5.90/27min |
| Error / no disponible | mapper→null; error de pipeline → `PipelineOverlayDataSourceTest`; `OverlayContent` ERROR (Compose, device) |

## No-invención de métricas (WP-12-CALC-04)

Cubierto por el mapper: solo monto → sin celdas; monto+distancia → GANANCIA/POR KM/COSTO EST.,
sin POR HORA; monto+duración → solo POR HORA; completo → ambas; inválido → no se muestra.

## Límites

- `OverlayContent` (Compose) y `OverlayService` no son testeables en JVM en el módulo
  `:feature:overlay` (solo junit + coroutines-test; sin Compose test ni androidTest).
  G10 añadió la infraestructura instrumentada: render Compose (6 casos), ML Kit real y
  smoke de `OverlayService` — ver `G10_INSTRUMENTED_VALIDATION.md`.
- Precisión/rendimiento OCR real en múltiples dispositivos (Android 10–15) = validación
  física pendiente (BETA_READINESS), no unit tests.
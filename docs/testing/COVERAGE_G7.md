# G7 — Cobertura crítica UI + OCR (20-ago-2026)

> LOOP ENGINEERING G7. Estado final: **PARTIAL / DEVICE_VALIDATION_PENDING**.
> La lógica determinista queda cubierta con unit tests; la precisión OCR real,
> `OverlayService` y el render Compose requieren dispositivo/instrumentado.

## Clasificación

| COVERED (tests deterministas) | DEVICE_REQUIRED (Android/ML Kit/físico) | NOT_COVERED |
|---|---|---|
| `mapToOverlayPresentation` (mapper, 26 tests) | `MlKitOcrEngine` (BitmapFactory + ML Kit real) | `OverlayContent` Compose (sin infra Compose test en el módulo) |
| `OverlayController` (start/stop/onServiceRunning, 4) | `OverlayService` (WindowManager, FGS, lifecycle, notificación) | — |
| `OverlayConfig.activeIndicatorCount` (3) | `OverlayService.kt:247` FLAG_NOT_TOUCHABLE (validación física ya hecha, FASE 15) | — |
| `PipelineOverlayDataSource` (evaluación, offerType, historial, persistencia) | Precisión/rendimiento OCR real (Android 10–15) | — |
| Contrato `OcrEngine` en el pipeline (`DefaultCapturePipelineTest`, 15): imagen→OCR→snapshot, flag OCR off, **OCR vacío→NO_TEXTS sin inventar**, **error OCR→OcrFailed + degradación sin crash** | Overlay real sobre otras apps | — |

## OCR — `MlKitOcrEngine`

- **Contrato** (`OcrEngine`): `suspend fun recognize(imageData): List<String>`. Consumido por
  `DefaultCapturePipeline.resolveTexts` (try/catch → `OcrFailed` + fallback a textos de
  accesibilidad).
- **Cubierto (JVM, contrato)**: resultado válido (imagen→OCR→snapshot), flag OCR desactivado
  (no se invoca), **resultado vacío** → `NO_TEXTS` sin fabricar datos, **error** → evento
  `OcrFailed` + degradación sin crash (nuevos tests de G7).
- **Device required**: precisión real de ML Kit, bitmap real, cancelación de ML Kit,
  recurso inválido/null (decode null → emptyList) — sin seam JVM. **No se declara validada
  por unit tests.** Evidencia física previa: dataset OCR DEVICE-01 + latencias
  (`SPRINT_12_DEVICE_VALIDATION.md`).
- Dataset: `core/capture/src/test/resources/test-images/` (imágenes reales; usado por el
  pipeline en JVM). No se crearon fixtures OCR nuevos.

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
  G10 se encargará de la infraestructura instrumentada.
- Precisión/rendimiento OCR real = validación física (no unit tests).
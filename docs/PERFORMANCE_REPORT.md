# Informe de rendimiento — v1.0.0-rc1

> Mediciones basadas en análisis de código y la infraestructura de métricas
> existente. Los números en **dispositivo real** se recogen con el Panel de
> depuración (sección "Rendimiento") durante las pruebas de RC1; este documento
> no inventa datos de hardware.

## Objetivo

La oferta debe evaluarse en **menos de 3 segundos** desde que aparece en
pantalla, con bajo impacto en batería y memoria. Para RC1 no se optimizó sin
evidencia: primero se instrumenta (ya está), luego se mide en dispositivo y solo
después se optimiza lo medido.

## Infraestructura de medición (ya existente)

- `OfferPerformanceTracker`/`InMemoryOfferPerformanceTracker`
  (`:core:capture`): retiene las **últimas 100 ofertas** y expone el **promedio
  de las últimas 20** por etapa.
- `ProcessingMetrics` (StateFlow `CapturePipeline.lastMetrics`): la última oferta
  con `captureMillis`/`ocrMillis`/`detectionMillis`/`parseMillis`/`totalMillis`.
- Etapas cronometradas:
  - **Captura** (pipeline): adquisición del frame (MediaProjection o textos).
  - **OCR** (pipeline): ML Kit sobre el frame (solo si hay imagen y flag `OCR`).
  - **Detección + parseo** (pipeline): `detectionMillis` (parser orquestador) +
    `parseMillis`.
  - **Evaluación + reglas + overlay** (overlay): `evaluationMillis`,
    `rulesMillis`, `overlayMillis` (completados con `merge`).
- El Panel de depuración muestra promedios y la última oferta por etapa.

## Cuellos de botella conocidos (por análisis de código)

| Etapa | Costo esperado | Notas |
|---|---|---|
| **Captura** | ~1–50 ms | MediaProjection `ImageReader` + conversión a Bitmap. Tiempo de espera del frame: 400 ms. |
| **OCR (ML Kit)** | **300–800 ms** | La etapa más cara; escala con resolución del frame. Mitigado con debounce (400 ms), caché por hash (LRU 32) y degradación a textos. |
| **Detección/Parseo** | <10 ms | Keywords ponderadas + parsers especializados + regex precompiladas. |
| **Evaluación** | <5 ms | Funciones puras en `:domain`. |
| **Reglas** | <5 ms | 6 reglas con umbrales desde `DriverConfig`. |
| **Overlay** | ~2–20 ms | `updateViewLayout` + recomposición Compose (solo cuando cambia la oferta). |

## Controles de consumo ya aplicados

- **Debounce de accesibilidad**: `DebounceCaptureScheduler` (400 ms) coalesce los
  eventos de accesibilidad (muy frecuentes) y ejecuta el pipeline solo con el
  último.
- **Caché de frames por hash**: `InMemoryCaptureFrameCache` (LRU 32) omite
  frames idénticos y **no repite OCR**.
- **Límites duros del árbol de accesibilidad**: 400 nodos, 80 textos, ≤200 chars
  por texto, deduplicación por huella.
- **Buffers acotados**: historial en memoria 50 snapshots; tracker 100 ofertas;
  buffer de validación 500 eventos; `ImageReader` con `MAX_IMAGES = 2`; frames
  pendientes drenados al liberar (`drainFrames`).
- **Logs por niveles**: `DEBUG`/`INFO` apagados fuera de builds de desarrollo;
  solo `ERROR`/`WARNING` en Release (menos I/O de logcat).
- **Reciclado de bitmaps** en `MlKitOcrEngine` y cancelación de la corrutina si
  se aborta.

## Procedimiento de medición en dispositivo (RC1)

1. Activar ambos servicios de accesibilidad, overlay y captura de pantalla.
2. Abrir el Panel de depuración → sección **"Rendimiento (promedio últimas 20
   ofertas)"**.
3. Registrar 20+ ofertas reales y anotar: captura, OCR, detección, parseo,
   reglas, evaluación, overlay, total (promedio y última).
4. Comprobar el objetivo <3 s con total promedio; si OCR supera ~1 s, evaluar
   reducir la resolución del virtual display o bajar la frecuencia de captura.
5. **Batería/memoria**: en el mismo panel, anotar "Memoria aproximada" durante
   capturas prolongadas y el consumo de batería de la app en
   Configuración → Batería (sesión de 30 min).
6. Exportar diagnóstico + informe de validación si hay descartes/errores.

## Resultado esperado

- Con los controles actuales y sin optimizar, el flujo debería completar una
  oferta en <1 s (OCR dominante). Si el OCR en pantallas grandes supera 1,2 s,
  es el principal candidato de optimización en post-RC1 (reducir resolución del
  frame o usar `TextRecognitionOptions` de menor carga).
- Sin captura de pantalla (solo accesibilidad) el flujo es casi instantáneo
  (<50 ms): el costo se limita a detección/parseo/evaluación.

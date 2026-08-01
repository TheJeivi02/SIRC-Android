# Release Notes — v1.0.0-rc1

> Sprint 10 · Hardening y preparación de la Release Candidate.
> Destinada a pruebas intensivas en dispositivos reales (Android 10–15).

## Resumen

v1.0.0-rc1 endurece la beta v1.0.0-beta: elimina código muerto que duplicaba el
historial, introduce un **modo de validación con informe exportable**, completa
la recuperación ante fallos, los **logs por niveles**, la compatibilidad con
Android 15 y documenta rendimiento, incidencias conocidas y el plan de pruebas.
No añade funcionalidades al producto: solo estabilidad y observabilidad.

## Qué hay de nuevo

### Modo de validación (exportable)

- `ValidationRecorder` (`:core:capture`, puro): buffer acotado (500 eventos) que
  acumula incidentes del pipeline y de la evaluación.
- Eventos registrados: errores de **captura** (`CaptureError`), de **OCR**
  (`OcrFailed`), de **parseo** (`ParseFailed`), **capturas descartadas**
  (`FrameDiscarded`: `CAPTURE_FAILED`/`DUPLICATE`/`NO_TEXTS`/
  `UNSUPPORTED_PLATFORM`), **reglas fallidas** (`RuleFailed`, veredicto FAIL) y
  **ofertas rechazadas** (`OfferRejected`).
- Sección **"Modo validación"** en el Panel de depuración con contadores,
  **"Exportar informe de validación"** (share) y "Limpiar eventos".
- El informe de validación también se incluye al final de **"Exportar
  diagnóstico"**.

### Recuperación ante fallos (crash recovery)

- El pipeline degrada correctamente si el **OCR falla**: registra `OcrFailed` y
  usa los textos de accesibilidad en lugar de entrar en `ERROR` (más resiliente
  ante imágenes no reconocibles).
- Los fallos no controlados del pipeline se registran como `CaptureError` y
  quedan visibles en el informe de validación; el pipeline se auto-recupera en la
  siguiente solicitud.
- La infraestructura de MediaProjection registra incidentes: token no disponible
  y proyección interrumpida por el sistema (`onStop`).
- Se conserva lo ya robusto desde la beta: `START_STICKY` + vista única en el
  overlay, reinicio automático de los servicios de accesibilidad por el sistema,
  recreación del virtual display ante cambios de configuración, reciclado de
  bitmaps y cancelación de corrutinas de OCR.

### Logs por niveles

- `AndroidSircLogger` clasifica por nivel:
  - **ERROR/WARNING**: siempre activos (también en Release) para diagnosticar
    incidencias de campo en logcat.
  - **INFO**: solo en builds de desarrollo (debbuggables).
  - **DEBUG**: solo en desarrollo **y** con el flag beta `DETAILED_LOGS`.

### Compatibilidad Android 15

- `OverlayService` sustituye `WindowManager.defaultDisplay.getRealMetrics()`
  (deprecado en Android 15) por `WindowManager.getCurrentWindowMetrics()`
  (API 30+) con fallback para API 24–29 en `screenBounds()`.
- targetSdk 35, compileSdk 35, minSdk 24; compatible Android 10–15.

### Limpieza (código muerto y duplicación)

- Eliminados `OfferEvaluator` y `OfferEventBus` (flujo legacy que persistía un
  historial básico **duplicado**; el overlay y el historial ya los alimenta el
  pipeline moderno). `SircAccessibilityService` conserva únicamente su rol de
  reenvío de eventos de ventana al pipeline/panel de depuración, sin interpretar
  nada. Resultado: **fin del historial duplicado**.
- Auditoría O1: sin `TODO`/`FIXME`/`XXX`/`HACK` en el código; recursos
  (`ic_stat_capture`, `ic_stat_sirc`) y tokens de tema verificados en uso.

## Cómo probar

Ver `docs/testing/SPRINT_10_MANUAL_TEST.md` (hardening RC1), el plan general en
`docs/testing/BETA_TEST_PLAN.md` y la regresión en
`docs/testing/SPRINT_09_MANUAL_TEST.md`.

## Verificación

- `ktlintCheck`, `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` en
  verde.
- Tests unitarios en verde: `:domain`, `:core:platform`, `:core:capture`
  (incluye `ValidationRecorderTest`, pipeline con validación y stress),
  `:feature:overlay` (`PipelineOverlayDataSourceTest` ampliado), `:data`.

## Documentos

- `docs/KNOWN_ISSUES.md` — incidencias conocidas y limitaciones.
- `docs/PERFORMANCE_REPORT.md` — medición y cuellos de botella conocidos.
- `docs/TEST_REPORT.md` — resultados de la verificación de RC1.
- `docs/CHANGELOG.md`, `docs/ROADMAP.md`, `.ai/CONTEXT.md`,
  `.ai/DECISIONS.md`, `docs/ARCHITECTURE.md` actualizados.

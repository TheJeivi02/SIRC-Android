# SPRINT 10 — Prueba manual (Hardening y Release Candidate v1.0.0-rc1)

> Objetivo: validar el hardening de RC1 (Sprint 10): **modo de validación**
> con informe exportable, **recuperación ante fallos**, **logs por niveles**,
> **compatibilidad Android 15** y la **eliminación del historial duplicado**.
> Se recomienda hacer esta prueba además de `SPRINT_09_MANUAL_TEST.md` y
> `BETA_TEST_PLAN.md`.

## Requisitos

- APK de depuración (`app/build/outputs/apk/debug/app-debug.apk`) instalado.
- Dispositivos de prueba en el rango **Android 10 (API 29)** a **Android 15
  (API 35)** (targetSdk 35). Al menos uno en **Android 15**.
- Permisos: **accesibilidad** (ambos servicios SIRC), **captura de pantalla**
  y **overlay**.
- Conductor configurado (onboarding) con costos y umbrales.
- **Uber Driver instalado**.

## 1. Configuración previa

1. Completa el onboarding si no está configurado.
2. Ajustes > Accesibilidad: activa **Análisis de ofertas SIRC** y **Captura de
   ofertas SIRC**.
3. En Home: **Permitir captura de pantalla** e inicia el overlay.
4. Abre **Debug** → sección **Modo validación**.

## 2. Modo de validación (O3)

1. Con una oferta real en Uber Driver, deja que el pipeline la analice.
   Verifica que **Total eventos** puede ser 0 cuando todo va bien.
2. **Descarte por duplicado**: en la misma pantalla de oferta, espera un nuevo
   evento de accesibilidad (cambio de contenido) → `Capturas descartadas`
   puede incrementarse con `DUPLICATE` (la caché omite frames idénticos).
3. **Reglas fallidas / rechazos**: configura umbrales de ganancia altos en
   Ajustes (p. ej. min ganancia/km muy alto) para que la oferta caiga en
   `REJECT` → verifica `Reglas fallidas` y `Ofertas rechazadas` se incrementan.
4. **Sin captura de pantalla** (desactiva el permiso de proyección): el
   pipeline usa textos de accesibilidad; si no llegan textos, `Capturas
   descartadas` refleja `NO_TEXTS`.
5. Pulsa **Exportar informe de validación**: el share incluye resumen
   (errores de captura/OCR/parseo, descartes por motivo, reglas, rechazos) y
   el detalle cronológico.
6. Pulsa **Exportar diagnóstico**: el informe de validación aparece al final.
7. Pulsa **Limpiar eventos** y verifica que los contadores vuelven a 0.

## 3. Recuperación ante fallos (O6)

1. **OCR fallido**: con captura de pantalla activa y una pantalla sin texto
   reconocible, verifica que el pipeline no entra en `Error` permanente y que
   en el informe aparece `OCR_ERROR` (degradación a textos de accesibilidad).
2. **Mata el proceso** (recents) con el overlay activo y vuelve a abrir la app:
   el overlay se restablece (`START_STICKY`) y el historial persiste (Room).
3. **Revoca y rehabilita la accesibilidad**: el sistema reinicia los servicios;
   tras re-activar, la captura y el análisis funcionan de nuevo.
4. **Proyección interrumpida** (algunos fabricantes la detienen en ahorro de
   batería): en el informe de validación aparece `CAPTURE_ERROR · proyección
   interrumpida por el sistema` y la app degrada a textos.

## 4. Rotación y split screen (O4/O6)

1. Con una oferta en pantalla, **rota el dispositivo**: el overlay se reajusta
   (reclamp) sin duplicar la vista y la captura de pantalla continúa
   (el virtual display se recrea).
2. Entra en **split screen**: repite el análisis y verifica que el overlay se
   mantiene dentro de la pantalla.

## 5. Compatibilidad Android 15 (O4)

1. En un dispositivo **Android 15**, repite la prueba completa.
2. Verifica que no hay crashes y que `adb logcat` no muestra deprecaciones de
   `getRealMetrics`/`defaultDisplay` del overlay (se usa `WindowMetrics`).
3. Comprueba las notificaciones FGS: `sirc_overlay` y `sirc_capture` visibles.

## 6. Historial sin duplicados (O1)

1. Con **ambos** servicios de accesibilidad activos, analiza 1 oferta real.
2. Abre **Historial**: debe aparecer **una sola entrada** (la del pipeline
   moderno). Si aparecen dos, es una regresión del flujo legacy eliminado.

## 7. Logs por niveles (O7)

1. Con APK de debug, en logcat: `INFO`/`DEBUG` presentes (tags
   `CapturePipeline`, `OverlayMetrics`).
2. Con un build **release** (o `FLAG_DEBUGGABLE` desactivado): `DEBUG`/`INFO`
   desaparecen y `ERROR`/`WARNING` (p. ej. `CapturePipeline`, `OverlayMetrics`)
   siguen apareciendo.

## 8. Rendimiento y consumo (O2/O5)

1. Abre **Debug** → **Rendimiento (promedio últimas 20 ofertas)**.
2. Registra 20+ ofertas reales y anota el **Total** promedio: objetivo **<3 s**.
3. Anota **Memoria aproximada** durante una sesión de 30 min con captura de
   pantalla y el consumo de batería de la app (Configuración > Batería).
4. Compara con `docs/PERFORMANCE_REPORT.md` y registra los números.

## 9. Registro de resultados

| Caso | Dispositivo (API) | Resultado | Notas |
|---|---|---|---|
| Modo validación / exportar | | | |
| Recuperación tras kill | | | |
| Rotación / split screen | | | |
| Historial sin duplicados | | | |
| Logs (debug vs release) | | | |
| Rendimiento total <3 s | | | |
| Memoria/batería 30 min | | | |

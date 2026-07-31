# SPRINT 05 — Prueba manual (Pipeline de Captura + OCR)

> Objetivo: validar el pipeline de captura (Sprint 5) en dispositivo o emulador.
> La captura de imagen real (MediaProjection) aún no está conectada; el OCR (ML
> Kit) está integrado bajo `OcrEngine` y el pipeline lo aplica cuando la
> solicitud lleva imagen.

## Requisitos

- APK de depuración (`app/build/outputs/apk/debug/app-debug.apk`) instalado.
- Dispositivo/emulador con Android 7.0+ (minSdk 24).
- Permiso de accesibilidad otorgado a **ambos** servicios de SIRC:
  - **Análisis de ofertas SIRC** (`SircAccessibilityService`, overlay).
  - **Captura de ofertas SIRC** (`CaptureAccessibilityService`, pipeline).
- Tener instalada al menos una plataforma soportada (Uber, DiDi, Cabify o
  InDrive).

## 1. Servicios de accesibilidad

1. Abre Ajustes > Accesibilidad y verifica que aparezcan **dos** entradas de
   SIRC ("Análisis de ofertas SIRC" y "Captura de ofertas SIRC").
2. Activa ambos. SIRC no debe fallar al activarse.

## 2. Pipeline en el panel de depuración

1. Abre la app → destino **Debug** (icono de llave).
2. En **Feature Flags** deben aparecer 6 toggles, incluido **OCR** (activo).
3. Togglea `OCR` a OFF y vuelve a ON: la fila **OCR** en *Estado de
   infraestructura* cambia.
4. En **Captura** verifica el **Estado del pipeline**. Sin eventos será
   `DISABLED` o `WAITING`.

## 3. Flujo de captura con accesibilidad

1. Con ambos servicios activos y la captura arrancada, abre **Uber** (o
   DiDi/Cabify/InDrive) y navega por la app.
2. Vuelve a SIRC → **Debug**:
   - **Eventos procesados** debe crecer y **Eventos recientes** mostrar los
     últimos eventos.
   - **Sesión activa** refleja el paquete de la plataforma.
   - **Estado del pipeline** pasa por `CAPTURING`/`PROCESSING` y vuelve a
     `WAITING` (los cambios son rápidos).
3. Cambia de plataforma: se abre una nueva sesión.

## 4. Regresión del overlay (no debe romperse)

1. Inicia el overlay desde Inicio/Diagnóstico y activa los permisos.
2. El indicador simulado aparece cada ~20 s con la insignia de decisión y
   métricas, como en sprints anteriores.
3. El historial se sigue persistiendo al evaluar ofertas por accesibilidad.

## 5. OCR

La ruta OCR no se dispara con accesibilidad (no hay imagen aún). Se valida con:

- Pruebas unitarias de `:core:capture` (`DefaultCapturePipelineTest`), que
  cargan las imágenes de `core/capture/src/test/resources/test-images/` y
  recorren el pipeline con OCR simulado.
- Cuando se conecte la captura de imagen (MediaProjection) en un sprint futuro,
  esta sección se ampliará.

## 6. Logging

- Con `adb logcat -s CapturePipeline:MlKitOcr:OfferCapture`, al capturar deben
  aparecer líneas del pipeline. En un build release no debe haber logs.

## Criterios de aceptación (verificación)

- [ ] Dos servicios de accesibilidad activables sin errores.
- [ ] Flag `OCR` visible y configurable en el panel.
- [ ] `Estado del pipeline` (`OverlayState`) observable en el panel.
- [ ] Abrir una plataforma produce eventos y snapshots (sin regresión de los
      sprints 2/4: overlay simulado e historial).
- [ ] `CAPTURE`/`OCR` en OFF bloquean su etapa correspondiente.
- [ ] `ktlintCheck`, `lintDebug`, tests y `assembleDebug` en verde.

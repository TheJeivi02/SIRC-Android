# SPRINT 06 — Prueba manual (Captura de pantalla real con MediaProjection)

> Objetivo: validar la captura de pantalla real (MediaProjection) y su
> integración con el pipeline de extremo a extremo (Sprint 6): accesibilidad →
> debounce → captura de frame → OCR → parser → repositorio → estado del overlay,
> con caché por hash y métricas de rendimiento.

## Requisitos

- APK de depuración (`app/build/outputs/apk/debug/app-debug.apk`) instalado.
- Dispositivo/emulador con Android 7.0+ (minSdk 24). Recomendado **Android 14+**
  para validar el FGS tipo `mediaProjection`.
- Permiso de accesibilidad otorgado a ambos servicios de SIRC:
  - **Análisis de ofertas SIRC** (`SircAccessibilityService`, overlay).
  - **Captura de ofertas SIRC** (`CaptureAccessibilityService`, pipeline).
- Permiso de **captura de pantalla** concedido desde Home (ver sección 2).
- Tener instalada al menos una plataforma soportada (Uber, DiDi, Cabify o
  InDrive).

## 1. Servicios de accesibilidad

1. Ajustes > Accesibilidad: activa **Análisis de ofertas SIRC** y **Captura de
   ofertas SIRC**.
2. SIRC no debe fallar al activarse.

## 2. Permiso de captura de pantalla (MediaProjection)

1. Abre la app → **Inicio**.
2. En la tarjeta **Captura de pantalla**, pulsa **Permitir captura de pantalla**.
3. El sistema muestra el diálogo de consentimiento de captura (proyección):
   selecciona una app para emular (p. ej. Uber). Acepta.
4. La tarjeta cambia a **"Captura activa"** y ofrece **Detener captura**.
5. Al pulsar **Detener captura**, la tarjeta vuelve al estado inicial y la
   notificación del servicio de captura desaparece.

## 3. Estado del overlay (PipelineOverlayDataSource)

Con la proyección activa:

1. Inicia el overlay desde Inicio/Diagnóstico.
2. Abre **Uber** (o DiDi/Cabify/InDrive). El overlay debe mostrar el estado del
   pipeline:
   - `Esperando oferta…` (WAITING)
   - `Capturando pantalla…` (CAPTURING)
   - `Analizando oferta…` (PROCESSING)
   - Al evaluar: insignia de decisión y métricas (motor real).
3. Sin oferta en pantalla, el overlay vuelve a estado de espera; el resultado
   evaluado se oculta tras el TTL configurado.

## 4. Métricas en el panel de depuración

1. Abre **Debug** (icono de llave).
2. Con la captura activa y una oferta visible, en **Métricas** deben aparecer
   filas **Captura / OCR / Parseo / Total** (ms). El **Total** debe estar muy
   por debajo de 3000 ms (objetivo <3 s).
3. **Estado del pipeline** recorre `CAPTURING → PROCESSING → WAITING`.
4. El **Último snapshot** refleja la oferta capturada (source `REAL`).

## 5. Caché de frames (deduplicación)

1. Con una oferta fija en pantalla, espera varios ciclos de accesibilidad.
2. En Debug, **Eventos procesados** crece, pero el pipeline no re-emite ni
   re-evalúa contenido idéntico (caché por hash): no se repite OCR del mismo
   frame.

## 6. Degradación sin proyección

1. Detén la captura (botón **Detener captura** en Home).
2. El pipeline degrada a los textos de accesibilidad (sin imagen): la ruta OCR
   no se dispara y el sistema sigue funcionando sin la imagen.

## 7. Logging

- `adb logcat -s ScreenCaptureProvider:CapturePipeline:MlKitOcr` al capturar
  muestra líneas del provider y del pipeline en debug. En un build release no
  debe haber logs.

## 8. Regresión del overlay y el historial

1. El overlay sigue persistiendo el historial al evaluar ofertas (sprints 2/4).
2. Diagnóstico mantiene los 5 indicadores (overlay, accesibilidad, servicio,
   notificaciones, batería).

## Criterios de aceptación (verificación)

- [ ] El consentimiento de captura de pantalla se pide en Home y se puede
      detener.
- [ ] El overlay muestra el estado real del pipeline (Esperando/Capturando/
      Analizando/Error) y el resultado evaluado con el motor real.
- [ ] Métricas por etapa (Captura/OCR/Parseo/Total) visibles en Debug con Total
      << 3000 ms.
- [ ] La caché de frames evita reprocesar contenido idéntico.
- [ ] Sin proyección, el sistema degrada a texto de accesibilidad sin romperse.
- [ ] `ktlintCheck`, `testDebugUnitTest`, `:core:capture:test`, `lintDebug`,
      `assembleDebug` y `assembleDebugAndroidTest` en verde.

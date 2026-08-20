# G10 — Validación instrumentada en dispositivo (20-ago-2026)

> LOOP ENGINEERING G10. Infraestructura androidTest para validar en dispositivo
> real los componentes DEVICE_REQUIRED de G7 (`MlKitOcrEngine`, `OverlayContent`,
> `OverlayService`). Estado: **COMPLETADO** — 8/8 overlay + 1/1 smoke en verde
> en DEVICE-01.

## Alcance

- **`MlKitOcrEngine` real** (ML Kit on-device + BitmapFactory): inicializa,
  procesa imágenes reales, devuelve resultado, no crashea; decode inválido →
  `emptyList()`.
- **`OverlayContent`** (render Compose real): casos A–F sobre el Application
  host de la librería.
- **`OverlayService`** (smoke en el Application real con Hilt): el FGS arranca
  bajo las condiciones requeridas, WindowManager recibe la ventana
  (`TYPE_APPLICATION_OVERLAY`) y stop/removal no produce crash.
- Sin cambios de producción. La precisión OCR queda **NOT_VALIDATED** (el README
  del dataset declara que las imágenes son marcadores del pipeline, no fixtures
  de precisión).

## Infraestructura añadida

- `:feature/overlay/build.gradle.kts`: `testInstrumentationRunner` +
  dependencias androidTest (junit, androidx.test, Compose BOM,
  `compose-ui-test-junit4`, `compose-ui-test-manifest` debug) + assets del
  dataset `../../core/capture/src/test/resources/test-images` (13 PNGs + README).
- `:app/build.gradle.kts`: `testInstrumentationRunner` +
  `androidx.test.runner`.
- `gradle/libs.versions.toml`: `compose-ui-test-junit4` y
  `compose-ui-test-manifest` (del BOM existente, sin upgrades).

## Pruebas

### OverlayContentTest (6) — `:feature:overlay`

| Caso | Verificación |
|---|---|
| sin evaluacion | "Esperando oferta…" mostrado, sin ACEPTAR |
| oferta válida | ACEPTAR + InDrive + $125 + 12.4 km · 18 min + GANANCIA/POR HORA/COSTO EST. |
| showDecision=false | ACEPTAR ausente, métricas intactas |
| showDecision=true | ACEPTAR visible |
| compactMode | mismos indicadores, ACEPTAR presente |
| datos faltantes | sin métricas inventadas (GANANCIA/POR HORA/POR KM/COSTO EST./resumen ausentes) |

### MlKitOcrEngineTest (2) — `:feature:overlay`

- 13 imágenes reales (360×640 / 240×240) procesadas sin excepción, todas
  devuelven resultado. Matriz + tiempos en logcat (tag `G10OcrTest`).
- decode inválido → `emptyList()` sin lanzar.

### OverlayServiceSmokeTest (1) — `:app`

- Precondición: `SYSTEM_ALERT_WINDOW` concedido (grant físico `adb appops set
  com.sirc.app SYSTEM_ALERT_WINDOW allow`; AGP desinstala tras cada corrida, el
  appop hay que regrantarlo).
- Flujo: `OverlayService.start` → FGS corriendo (`dumpsys activity services`:
  `isForeground=true`, `foregroundId=9001`, `types=0x40000000` specialUse) →
  ventana registrada en WindowManager (`dumpsys window windows`: `Window #6
  Window{... u0 com.sirc.app}` con `ty=APPLICATION_OVERLAY`, `fmt=TRANSLUCENT`)
  → `OverlayService.stop` → no running, sin crash.
- Detección de ventana por `ty=APPLICATION_OVERLAY` + `u0 com.sirc.app}` (el
  título de la ventana es el package name, no "Application Overlay").
- Fallo instrumentado con `diagnose()` que vuelca services/windows en el
  mensaje de fallo.

## Resultados (DEVICE-01, Infinix X6850 / Android 15 / API 35)

- `:feature:overlay:connectedDebugAndroidTest`: **8/8** (5 corridas, 4
  consecutivas en verde tras los fixes).
- `:app:connectedDebugAndroidTest`: **1/1**.
- Suite completa AGENTS en verde tras la infraestructura:
  `ktlintCheck lintDebug assembleDebug testDebugUnitTest :domain:test
  :core:platform:test :core:capture:test :feature:overlay:testDebugUnitTest`
  → BUILD SUCCESSFUL.

## Evidencia OCR (logcat `G10OcrTest`, 20-ago)

| Imagen | Líneas OCR | Tiempo (ms) |
|---|---|---|
| offer_uberx_1.png | 1 | 238 (cold start) |
| offer_comfort_1.png | 1 | 68 |
| offer_moto_1.png | 1 | 60 |
| offer_xl_1.png | 1 | 63 |
| offer_reservation_1.png | 1 | 80 |
| offer_radar_1.png | 1 | 74 |
| offer_bonus_1.png | 1 | 78 |
| offer_night_1.png | 1 | 64 |
| offer_invalid_1.png | 1 | 67 |
| offer_cabify_1.png | 4 | 96 |
| offer_didi_1.png | 4 | 70 |
| offer_indrive_1.png | 4 | 93 |
| offer_uber_1.png | 4 | 73 |

Total dataset: **1124 ms / 13 imágenes** (~86 ms promedio; ~60–96 ms tras
warm-up). Conclusión: el motor real procesa el dataset completo sin excepción;
**precisión = NOT_VALIDATED** (marcadores, no fixtures de precisión).

## Estabilidad (flakes resueltos, no skips)

1. **Smoke — detección de ventana**: el matcher original buscaba el token
   "Application Overlay" que no existe en el dump de Android 15 (el título es el
   package name). Corregido a `ty=APPLICATION_OVERLAY` + `u0 com.sirc.app}`. El
   diagnóstico confirmó que la ventana sí estaba registrada.
2. **Compose — "No compose hierarchies found"**: carrera intermitente durante el
   relaunch de la Activity entre tests. Corregido con `compose.waitForIdle()`
   tras cada `setContent`. Verificado con 4 corridas consecutivas 8/8.
3. **ktlint**: aplicado `ktlintFormat` (argument-list-wrapping, max-line-length,
   final-newline).

## Clasificación final (frontera de evidencia)

| Componente | Evidencia |
|---|---|
| `MlKitOcrEngine` — comportamiento | **DEVICE_VALIDATED** (13/13 imágenes, tiempos medidos) |
| `MlKitOcrEngine` — precisión | **NOT_VALIDATED** (dataset = marcadores) |
| `OverlayContent` render | **INSTRUMENTED_COVERED** (6 casos Compose en device) |
| `OverlayService` (FGS + ventana + stop) | **DEVICE_VALIDATED** (smoke en Application real) |
| Lógica determinista (mapper, pipeline, motor) | JVM_COVERED (G7/G2/G6) |

## Reproducibilidad

```powershell
# Requerido en DEVICE-01 antes de :app:connectedDebugAndroidTest:
adb install -r app\build\outputs\apk\debug\app-debug.apk
adb shell appops set com.sirc.app SYSTEM_ALERT_WINDOW allow
.\gradlew.bat :feature:overlay:connectedDebugAndroidTest :app:connectedDebugAndroidTest --console=plain
# Evidencia OCR (captura durante la corrida):
adb logcat -s G10OcrTest:I
```

## Estado del dispositivo tras las pruebas

- App reinstalada (`app-debug.apk`), `SYSTEM_ALERT_WINDOW=allow` restaurado.
- La accesibilidad quedó deshabilitada por el uninstall de AGP (Android la
  remueve al desinstalar); se restaura con `adb shell settings put secure
  enabled_accessibility_services com.sirc.app/com.sirc.feature.overlay.CaptureAccessibilityService`
  y `adb shell settings put secure accessibility_enabled 1` si se requiere el
  estado de FASE 15.
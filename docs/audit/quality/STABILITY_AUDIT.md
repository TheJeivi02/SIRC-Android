# STABILITY_AUDIT.md — Auditoría de Estabilidad

**Rol:** Stability Engineer
**Objetivo:** Auditar rotación, pantalla dividida, cambio de resolución, idioma, reinicio, accesibilidad, MediaProjection, overlay y foreground service. Sin implementar.
**Proyecto:** SIRC (minSdk 24, targetSdk/compileSdk 35)
**Fecha:** 2026-08-01 · **Commit auditado:** `e3460a5`

---

## Resumen ejecutivo

El diseño separa correctamente la UI (Activity recreable) del estado de captura/overlay (singletons de proceso + FGS), y los servicios manejan `onConfigurationChanged`. Los riesgos mayores están en: (1) **`exported="false"` en los AccessibilityService** — puede impedir que el sistema los vincule, rompiendo toda la captura; (2) **`START_NOT_STICKY` en `MediaProjectionService`** — tras kill/Doze se pierde la captura sin reintento; (3) **API 24-25 sin fallback de `TYPE_APPLICATION_OVERLAY`** — riesgo de crash/no-show; (4) **feedback del propio overlay en la captura** — el OCR lee el texto de SIRC.

---

## 1. Rotación / cambio de configuración

### S-S01 — Manejo de rotación correcto en servicios, sin `configChanges` (correcto por diseño)
- **Resumen:** No hay `android:configChanges` (la Activity se recrea, correcto para Compose). Los ViewModels sobreviven vía Hilt; los campos de UI se guardan con `rememberSaveable` (`SettingsScreen.kt:170`, `OnboardingScreen.kt:175`, `HistoryScreen.kt:121`). El overlay/captura viven en singletons de proceso y sobreviven.
- **Impacto:** Sin hallazgo negativo. Riesgo menor: durante la recreación del VirtualDisplay hay una ventana con `virtualDisplay`/`imageReader` null en la que `captureFrame()` devuelve null y se omite el OCR de ese frame — degradación aceptable.
- **Severidad:** OK (informativo)
- **Evidencia:** `app\src\main\AndroidManifest.xml:20-30`; `OverlayService.kt:78-81` (reclampOverlay); `MediaProjectionService.kt:32-35` → `MediaProjectionScreenCaptureProvider.kt:120-126`.
- **Prioridad:** —

---

## 2. Pantalla dividida / multi-window

### S-S02 — Bounds del overlay inconsistentes entre versiones en split-screen
- **Resumen:** `screenBounds()` usa `WindowManager.currentWindowMetrics.bounds` en API 30+ (`OverlayService.kt:195-197`), que en pantalla dividida refleja **la ventana de la app**, no el display completo; pero una ventana `TYPE_APPLICATION_OVERLAY` se posiciona sobre **toda** la pantalla. El fallback API 24-29 usa `getRealMetrics()` (display completo). El overlay se posiciona/clampa contra métricas distintas según versión → posición inconsistente.
- **Impacto:** En split-screen el overlay puede quedar desplazado fuera de la mitad visible o mal posicionado; comportamiento distinto entre Android 10 y 11+.
- **Severidad:** MEDIA
- **Evidencia:** `OverlayService.kt:193-201`.
- **Prioridad:** P2

### S-S03 — No hay `onMultiWindowModeChanged`; el overlay puede bloquear la interacción
- **Resumen:** No se implementa `onMultiWindowModeChanged` en ningún componente. El overlay de SIRC se dibuja sobre toda la pantalla en split, incluyendo la otra app.
- **Impacto:** En pantalla dividida el overlay (si está visible y toca la mitad de la otra app) puede interferir; sin lógica específica de multi-window.
- **Severidad:** BAJA-MEDIA
- **Evidencia:** grep de `onMultiWindowModeChanged` sin resultados.
- **Prioridad:** P3

### S-S04 — El propio overlay es capturado por MediaProjection (feedback del OCR)
- **Resumen:** La proyección captura el display completo incluyendo la ventana `TYPE_APPLICATION_OVERLAY` de SIRC. El OCR leerá el texto del overlay del frame anterior ("PRECIO", "GANANCIA", "COSTO EST."), contaminando el parseo de la oferta.
- **Impacto:** Resultados OCR corruptos en ofertas consecutivas (el texto de SIRC se mezcla con el de la app de la plataforma). No hay ocultamiento del overlay durante la captura.
- **Severidad:** MEDIA-ALTA (correctness del dato principal)
- **Evidencia:** `MediaProjectionScreenCaptureProvider.kt:145-177` (captura full display) + `OverlayContent.kt:52-119` (sin ocultar al capturar).
- **Prioridad:** P1

---

## 3. Cambio de resolución / densidad

### S-S05 — Clamp de posición `y` ineficaz por `WRAP_CONTENT`
- **Resumen:** `params.height` es `WRAP_CONTENT` (=-2). En `reclampOverlay()` (`OverlayService.kt:153`) y `moveOverlay()` (`:166`), `bounds.height() - params.height` = `bounds.height() + 2`, por lo que el clamp superior de `y` nunca actúa. Tras rotar a landscape, si el overlay es alto, su borde inferior puede quedar fuera de pantalla.
- **Impacto:** Overlay parcialmente fuera de pantalla tras rotación/cambio de tamaño (contenido recortado o fuera de alcance).
- **Severidad:** MEDIA
- **Evidencia:** `OverlayService.kt:153,166`.
- **Prioridad:** P2

### S-S06 — Presión de memoria en pantallas grandes (imágenes a resolución completa)
- **Resumen:** `ImageReader.newInstance(width, height, RGBA_8888, 2)` (`MediaProjectionScreenCaptureProvider.kt:149-155`) a resolución completa: ~18 MB por imagen en QHD (2× ≈ 36 MB) más el `Bitmap` de conversión, el PNG y el decode del OCR. Sin acotación por resolución ni downsample.
- **Impacto:** Riesgo de `OutOfMemoryError`/GC agresivo en pantallas 4K o dispositivos de gama baja.
- **Severidad:** MEDIA
- **Evidencia:** `MediaProjectionScreenCaptureProvider.kt:149-155,218-233`.
- **Prioridad:** P2

### Densidad — puntos correctos
- El VirtualDisplay usa `metrics.densityDpi` y se recrea en `onConfigurationChanged` si cambia la densidad (`MediaProjectionScreenCaptureProvider.kt:171,120-126`).
- `screenBounds()` usa la API moderna para evitar las APIs deprecadas de `Display` en Android 15 (`OverlayService.kt:193-201`).

---

## 4. Idioma / locale

### S-S07 — UI sin internacionalización (strings hardcodeados en español)
- **Resumen:** La UI usa strings en español directamente en Kotlin (74 coincidencias en `app`): `HomeScreen.kt:84-237`, `SircApp.kt:33-44`, `SettingsScreen.kt:45-161`, `OnboardingScreen.kt:55-149`, `HistoryScreen.kt:326-441`, `OverlayContent.kt:143-217`, `DebugPanelScreen.kt:73-366`, e incluso en reglas de negocio (`domain\...\engine\rules\MinimumProfitRule.kt:16,33-35`). Los recursos XML solo cubren servicios/notificaciones; no hay `values-es` ni otros locales.
- **Impacto:** No hay i18n/l10n; la app no se traduce y los textos del overlay (mensajes de reglas) viajan desde dominio sin localizar.
- **Severidad:** MEDIA (deuda de producto)
- **Evidencia:** grep de strings; `MinimumProfitRule.kt:16`.
- **Prioridad:** P2

### S-S08 — Formato de moneda y números sin `NumberFormat`/`Locale` consistente
- **Resumen:** `ProfitEngine.formatCurrency` (`ProfitEngine.kt:88-102`) usa mapa fijo de símbolos + `Double.toString()`; `StatsScreen.kt:252-256` y `HistoryScreen.kt:438-442` usan `DecimalFormat("#,##0.0#")` con `.replace(',', '.')` forzado; `HistoryScreen.kt:441` usa `Locale.getDefault()` en `SimpleDateFormat` (inconsistente con el resto). `PipelineOverlayDataSource.kt:288,301` y `ValidationRecorder.kt:108` usan `Locale.ROOT` (correcto).
- **Impacto:** Formato inconsistente entre pantallas; en dispositivos con locale no-EN los separadores pueden variar; el cambio de locale en runtime no se refleja coherentemente.
- **Severidad:** MEDIA (datos) / BAJA (crash)
- **Evidencia:** `ProfitEngine.kt:88-102,113-122`; `HistoryScreen.kt:438-442`.
- **Prioridad:** P2

### S-S09 — Notificación FGS no se re-traduce al cambiar locale en runtime
- **Resumen:** `buildNotification()` solo se ejecuta en `onStartCommand`; `onConfigurationChanged` del servicio (`OverlayService.kt:78`) no reconstruye la notificación.
- **Impacto:** La notificación conserva el idioma antiguo hasta reiniciar el servicio.
- **Severidad:** BAJA
- **Evidencia:** `OverlayService.kt:78-81` (no toca notificación) vs `:218-232`.
- **Prioridad:** P3

---

## 5. Reinicio / process death

### S-S10 — `MediaProjectionService` `START_NOT_STICKY`: la captura no se restaura tras kill/Doze
- **Resumen:** El servicio de proyección devuelve `START_NOT_STICKY` (`MediaProjectionService.kt:53,57,60`). Tras un kill del proceso o un ciclo de Doze, la proyección no se restaura; el token de MediaProjection no sobrevive y no hay mecanismo de re-solicitud. El overlay (START_STICKY) vuelve, pero sin OCR.
- **Impacto:** La app queda en estado "mitad encendida": overlay visible pero sin captura/OCR, sin aviso proactivo al usuario.
- **Severidad:** ALTA
- **Evidencia:** `MediaProjectionService.kt:42-61`; `OverlayService.kt:75` (START_STICKY, contrasta).
- **Prioridad:** P1

### S-S11 — Sin `BOOT_COMPLETED`: el overlay no se restaura tras reinicio del dispositivo
- **Resumen:** No hay `RECEIVE_BOOT_COMPLETED` ni reintento programado. Tras un reinicio del teléfono el usuario debe iniciar manualmente.
- **Impacto:** Pérdida de continuidad del servicio tras reboot; es una decisión de producto (seguridad) pero deja el sistema sin auto-recuperación.
- **Severidad:** BAJA (decisión de producto, documentar)
- **Evidencia:** grep de `BOOT_COMPLETED` sin resultados; `OverlayService.kt:244-246` (inicio solo vía UI).
- **Prioridad:** P3

### S-S12 — Estado en memoria perdido tras process death (por diseño)
- **Resumen:** Repositorios en memoria (`InMemoryOfferEvaluationRepository` max 100, `InMemoryCaptureRepository`, `InMemoryCaptureFrameCache`, `PipelineOverlayDataSource`), `CaptureSessionManager` y las estadísticas de sesión se pierden con el proceso. Room (`SircDatabase` v3) conserva config + historial.
- **Impacto:** Pérdida de las últimas ofertas evaluadas y de la sesión de captura tras kill; historial persistente intacto.
- **Severidad:** BAJA (por diseño)
- **Evidencia:** `InMemoryOfferEvaluationRepository.kt:17-42`; `SircDatabase.kt:12-20`.
- **Prioridad:** P3

### S-S13 — Token MediaProjection caduca; sin automatización de re-concesión
- **Resumen:** El token se obtiene de `getMediaProjection(resultCode, data)` (`MediaProjectionScreenCaptureProvider.kt:75-76`) y no es persistible; la parada por el usuario desde la barra (chip de captura) o process death lo invalidan. La UI solo lo re-ofrece cuando `projectionActive == false` al volver a Home (`HomeScreen.kt:194-209`).
- **Impacto:** El usuario debe re-conceder el permiso; sin notificación proactiva de parada.
- **Severidad:** BAJA
- **Evidencia:** `MediaProjectionScreenCaptureProvider.kt:88-100` (onStop) + `HomeScreen.kt:194-209`.
- **Prioridad:** P3

---

## 6. Accessibility

### S-S14 — CRÍTICO: `android:exported="false"` en los AccessibilityService puede impedir el vínculo del sistema
- **Resumen:** Ambos servicios (`SircAccessibilityService`, `CaptureAccessibilityService`) se declaran con `android:exported="false"` (`feature\overlay\src\main\AndroidManifest.xml:11-22,24-35`). Un `AccessibilityService` debe poder ser vinculado por `system_server` (uid distinto). Con `exported="false"`, `AccessibilityManagerService` puede rechazar el enlace (`Permission Denial` en `retrieveServiceLocked`) y el servicio nunca recibiría `onAccessibilityEvent`.
- **Impacto:** **Si se confirma en dispositivo, toda la captura basada en accesibilidad falla** (la app depende de accesibilidad para detectar ofertas). `hasAccessibilityPermission()` (`PermissionManager.kt:50-57`) devolvería false y la UI no arrancaría el flujo. **Requiere verificación en dispositivo inmediata.**
- **Severidad:** CRÍTICA (requiere verificación en hardware)
- **Evidencia:** `feature\overlay\src\main\AndroidManifest.xml:11-22,24-35`.
- **Prioridad:** P0

### S-S15 — `hasAccessibilityPermission()` solo valida un servicio
- **Resumen:** `PermissionManager.kt:50-57` comprueba únicamente `SircAccessibilityService`; no valida `CaptureAccessibilityService`, pese a que ambos son necesarios y aparecen como dos entradas en Ajustes.
- **Impacto:** La UI puede reportar "accesibilidad habilitada" con el servicio de captura desactivado → OCR nunca arranca.
- **Severidad:** MEDIA
- **Evidencia:** `PermissionManager.kt:50-57`.
- **Prioridad:** P2

### S-S16 — Doble recorrido del árbol por evento (2 servicios con la misma config)
- **Resumen:** Ambos servicios usan la misma `accessibility_service_config.xml` (eventos, `notificationTimeout=100`, packageNames) y cada uno recorre `rootInActiveWindow` (max 400 nodos/80 textos) por evento en el hilo principal.
- **Impacto:** Doble CPU de accesibilidad + mayor riesgo de ANR en pantallas complejas. (Ver también PERFORMANCE P-P17.)
- **Severidad:** MEDIA
- **Evidencia:** `accessibility_service_config.xml:3,10,11`; `SircAccessibilityService.kt:66-88`; `CaptureAccessibilityService.kt:76-98`.
- **Prioridad:** P2

### S-S17 — Anti-ANR y dedup correctos
- **Resumen:** Recorrido con límites duros (MAX_NODES=400, MAX_TEXTS=80, MAX_TEXT_LENGTH=200, `CaptureAccessibilityService.kt:107-111`), dedup por `fingerprint` (`.hashCode()`), debounce de 400 ms con buffer 64 DROP_OLDEST (`DebounceCaptureScheduler.kt:22-26`).
- **Impacto:** Bajo riesgo de ANR. Riesgo menor: una oferta repetida idéntica podría quedar ignorada por el dedup de fingerprint.
- **Severidad:** OK (con nota)
- **Evidencia:** `CaptureAccessibilityService.kt:58-60,107-111`; `DebounceCaptureScheduler.kt:20-40`.
- **Prioridad:** —

### S-S18 — Sin reintento/aviso de desconexión de accesibilidad
- **Resumen:** No hay re-bindeo ni aviso si un servicio de accesibilidad se desconecta; solo se detecta en `HomeScreen.refresh()` al volver al foreground.
- **Impacto:** Fallos de accesibilidad silenciosos durante la conducción.
- **Severidad:** BAJA
- **Evidencia:** `HomeScreen.kt:45-52`; `onInterrupt()` no-op (`SircAccessibilityService.kt:90`, `CaptureAccessibilityService.kt:100`).
- **Prioridad:** P3

---

## 7. MediaProjection

### S-S19 — Orden FGS→token correcto para Android 14+
- **Resumen:** `MediaProjectionService` hace `startForeground` (tipo `mediaProjection`) **antes** de `getMediaProjection` (`MediaProjectionService.kt:55-56` → `MediaProjectionScreenCaptureProvider.kt:76`), cumpliendo el requisito de Android 14.
- **Impacto:** Correcto.
- **Severidad:** OK
- **Evidencia:** `MediaProjectionService.kt:47-57`; `core\capture\android\src\main\AndroidManifest.xml:9-16`.
- **Prioridad:** —

### S-S20 — `getMediaProjection` sin try/catch de `SecurityException`
- **Resumen:** `manager.getMediaProjection(resultCode, data)` (`MediaProjectionScreenCaptureProvider.kt:76`) puede lanzar `SecurityException` (token inválido, FGS inactivo); no está envuelto en `runCatching`.
- **Impacto:** Crash del servicio en caso de token inválido/estado inesperado.
- **Severidad:** BAJA-MEDIA
- **Evidencia:** `MediaProjectionScreenCaptureProvider.kt:75-76`.
- **Prioridad:** P2

### S-S21 — `onStop` del `projectionCallback` y liberación correctos
- **Resumen:** `onStop()` registra `ValidationEvent.CaptureError` y llama `stopProjection()` (`MediaProjectionScreenCaptureProvider.kt:88-100`); `releaseResources()`/`releaseVirtualDisplay()`/`drainFrames()` liberan callback, proyección, VirtualDisplay, ImageReader y frames pendientes (`:179-204`).
- **Impacto:** Correcto; cubre parada por el usuario (chip) y fallos del sistema.
- **Severidad:** OK
- **Evidencia:** `MediaProjectionScreenCaptureProvider.kt:88-100,179-204`.
- **Prioridad:** —

---

## 8. OverlayService

### S-S22 — `TYPE_APPLICATION_OVERLAY` sin fallback en API 24-25 (riesgo de crash)
- **Resumen:** `buildWindowParams` usa siempre `TYPE_APPLICATION_OVERLAY` (`OverlayService.kt:177`), que solo existe desde API 26. Con minSdk 24, en Android 7.x el `WindowManager.addView` con este tipo puede lanzar `BadTokenException`. El `runCatching` de `OverlayService.kt:122` lo absorbe, pero el overlay no se muestra en absoluto.
- **Impacto:** La función principal no funciona en Android 7.x (2.5-5% del parque); sin crash visible pero inoperativo.
- **Severidad:** MEDIA-ALTA
- **Evidencia:** `OverlayService.kt:170-179` + `:122`.
- **Prioridad:** P2

### S-S23 — `OverlayController.isRunning` desincronizado del servicio real
- **Resumen:** `OverlayController.isRunning` solo cambia con `start()`/`stop()` explícitos (`OverlayController.kt:23-35`); no refleja muerte o reinicio (START_STICKY) del servicio.
- **Impacto:** La UI puede mostrar "Overlay en ejecución" con el servicio muerto (o viceversa), dando información errónea al conductor.
- **Severidad:** MEDIA
- **Evidencia:** `OverlayController.kt:23-35`; `OverlayService.kt:63-76`.
- **Prioridad:** P2

### S-S24 — Revocación de `SYSTEM_ALERT_WINDOW` en runtime no detectada por el servicio
- **Resumen:** La verificación `Settings.canDrawOverlays` solo ocurre en `onStartCommand` (`OverlayService.kt:69-72`). Si el usuario revoca el permiso mientras corre, el sistema retira la ventana pero el servicio sigue vivo con notificación y `isRunning=true`; no hay BroadcastReceiver de `ACTION_MANAGE_OVERLAY_PERMISSION`.
- **Impacto:** Estado fantasma: notificación permanente sin overlay. La UI se corrige al volver a Home, el servicio no.
- **Severidad:** MEDIA
- **Evidencia:** `OverlayService.kt:63-76`; grep de `ACTION_MANAGE_OVERLAY_PERMISSION` sin resultados.
- **Prioridad:** P2

### S-S25 — `START_STICKY` con auto-restauración correcta (pero sin captura)
- **Resumen:** `OverlayService` es `START_STICKY` (`OverlayService.kt:75`); en restart con intent null re-hace `startForeground`, verifica overlay y reconstruye la ventana (`ensureOverlay`, `:94`). Correcto, pero sin la proyección (S-S10) el overlay vuelve sin datos.
- **Impacto:** Parcial: overlay restaurado, captura no.
- **Severidad:** OK (con nota → ver S-S10)
- **Evidencia:** `OverlayService.kt:63-76,94-129`.
- **Prioridad:** —

---

## 9. Foreground service

### S-S26 — Excepciones FGS mayormente evitadas (diseño correcto)
- **Resumen:** Ambos servicios declaran un único `foregroundServiceType` (`specialUse` con `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` en `feature\overlay\src\main\AndroidManifest.xml:37-44`; `mediaProjection`). Los `startForegroundService` vienen solo de foreground (UI). `OverlayService` llama `startForeground` **antes** de validar overlay y hace `stopSelf()` si no hay permiso (`OverlayService.kt:68-71`) — correcto.
- **Impacto:** Sin `MissingForegroundServiceTypeException` ni `ForegroundServiceStartNotAllowedException` en el flujo normal.
- **Severidad:** OK
- **Evidencia:** `OverlayService.kt:68-72`; `MediaProjectionService.kt:51-56`; manifests citados.
- **Prioridad:** —

### S-S27 — `POST_NOTIFICATIONS` en Android 13+
- **Resumen:** Se solicita `POST_NOTIFICATIONS` en API 33+ (`HomeScreen.kt:55-66`); el FGS funciona aunque se deniegue (la notificación no aparece en el drawer pero sí en el Task Manager). `PermissionManager.kt:60` lo considera concedido < TIRAMISU.
- **Impacto:** Correcto; UX menor si el usuario deniega.
- **Severidad:** OK
- **Evidencia:** `HomeScreen.kt:55-66`; `PermissionManager.kt:59-63`.
- **Prioridad:** —

---

## 10. Versiones de Android / APIs

### S-S28 — Room sin `fallbackToDestructiveMigration` (riesgo futuro)
- **Resumen:** `DatabaseModule.kt:24-29` registra solo `MIGRATION_1_2` y `MIGRATION_2_3`. Una futura v4 sin migración crashea con `IllegalStateException` al abrir la BD. Hoy correcto (v3 con ambas migraciones en `SircMigrations.kt:14-84`).
- **Impacto:** Riesgo futuro de crash en release si se olvida una migración.
- **Severidad:** BAJA
- **Evidencia:** `data\...\di\DatabaseModule.kt:24-29`.
- **Prioridad:** P3

### APIs — puntos correctos
- Chequeos de `Build.VERSION` presentes para: `POST_NOTIFICATIONS` (≥T), WindowMetrics (≥R), canales de notificación (≥O), `getParcelableExtra` (≥T) — `MediaProjectionService.kt:94-102`.
- `createVirtualDisplay` existe desde API 21 (OK con minSdk 24).
- Compose BOM 2024.12.01, Kotlin 2.0.21, Room 2.6.1 — coherentes con targetSdk 35.

---

## 11. Matriz de prioridad

| Ref | Severidad | Prioridad |
|---|---|---|
| S-S14 AccessibilityService `exported="false"` | CRÍTICA (verificar en dispositivo) | P0 |
| S-S10 MediaProjectionService START_NOT_STICKY | ALTA | P1 |
| S-S04 Feedback del overlay en la captura (OCR contamina) | MEDIA-ALTA | P1 |
| S-S02 Bounds inconsistentes split-screen | MEDIA | P2 |
| S-S05 Clamp `y` ineficaz (WRAP_CONTENT) | MEDIA | P2 |
| S-S06 Presión de memoria en pantallas grandes | MEDIA | P2 |
| S-S07 Sin i18n (strings hardcodeados) | MEDIA | P2 |
| S-S08 Formato moneda sin Locale | MEDIA | P2 |
| S-S15 Accesibilidad solo valida 1 servicio | MEDIA | P2 |
| S-S16 Doble recorrido árbol de accesibilidad | MEDIA | P2 |
| S-S20 `getMediaProjection` sin try/catch | BAJA-MEDIA | P2 |
| S-S22 TYPE_APPLICATION_OVERLAY en API 24-25 | MEDIA-ALTA | P2 |
| S-S23 `isRunning` desincronizado | MEDIA | P2 |
| S-S24 Revocación de overlay en runtime no detectada | MEDIA | P2 |
| S-S03 Sin onMultiWindowModeChanged | BAJA-MEDIA | P3 |
| S-S09 Notificación no se re-traduce | BAJA | P3 |
| S-S11 Sin BOOT_COMPLETED | BAJA | P3 |
| S-S12 Estado en memoria perdido | BAJA | P3 |
| S-S13 Token MediaProjection caduca | BAJA | P3 |
| S-S18 Sin aviso de desconexión a11y | BAJA | P3 |
| S-S28 Room sin fallback destructivo | BAJA | P3 |

---

## 12. Verificación urgente en dispositivo (checklist)

1. **P0:** ¿Los AccessibilityService se conectan con `exported="false"`? (Probar en Android 12 y 14; si `hasAccessibilityPermission()` da false o no llegan eventos, cambiar a `exported="true"`.)
2. **P1:** Matar el proceso (o simular Doze) → ¿el overlay vuelve? ¿la captura vuelve? ¿la UI lo reporta?
3. **P1:** Oferta consecutiva con el overlay visible → ¿el OCR se contamina con el texto de SIRC?
4. **P2:** Rotar de portrait→landscape con overlay alto y contenido compacto desactivado → ¿queda fuera de pantalla?
5. **P2:** Android 7.x (API 24-25) → ¿se muestra el overlay?
6. **P2:** Split-screen → ¿posición del overlay correcta? ¿bloquea la otra app?
7. **P2:** Revocar `SYSTEM_ALERT_WINDOW` en runtime → ¿la notificación y el estado de la UI se corrigen?
8. **P2:** Cambiar idioma en runtime → ¿se actualiza la notificación y la UI?

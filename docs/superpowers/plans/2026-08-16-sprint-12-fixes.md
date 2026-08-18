# Sprint 12 / E1a — Plan de correcciones post-validación (WP-12-FIX-01…05)

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans o
> superpowers:subagent-driven-development para ejecutar este plan tarea por tarea
> SOLO cuando el usuario lo autorice. Este documento es SOLO análisis y
> priorización: NO implementar todavía.

**Goal:** Corregir los hallazgos de la validación DEVICE-01 (DVC-01…DVC-04 y K1)
tras diagnosticar la causa raíz, en el orden y alcance aquí definidos.

**Architecture:** Aplican los fixes como WPs atómicos, cada uno con su propia
verificación en verde (AGENTS). Overlay (FIX‑01) y captura (FIX‑02) están
relacionados y deben corregirse en ese orden; parser (FIX‑04) puede probarse con
`DebugImageOcrReceiver` independientemente de la captura E2E; configuración
editable (FIX‑03) es independiente; higiene de artefactos (FIX‑05) es
infraestructura de testing.

**Tech Stack:** Kotlin, Jetpack Compose, Material 3, Hilt, Room, ML Kit,
MediaProjection, AccessibilityService, Gradle multi‑módulo.

## Global Constraints

- Sprint 12/E1a sigue abierto; **NO abrir Sprint 13**.
- NO implementar Supabase/Billing/monetización; NO modificar decisiones comerciales.
- Verificación obligatoria en verde: `.\gradlew.bat ktlintCheck`, `lintDebug`,
  `assembleDebug`, `testDebugUnitTest`, `:domain:test`, `:core:platform:test`,
  `:core:capture:test`, `:feature:overlay:testDebugUnitTest` (más las de módulos
  que se toquen).
- Regla §5.10 del plan de validación: los fixes se implementan SOLO en su WP; no
  mezclar pruebas con correcciones.
- 100 % local; sin telemetría; overlay < 3 s / bajo consumo.
- `DebugImageOcrReceiver` es debug-only y su decisión de mantener/eliminar sigue
  pendiente de usuario (§6.3 dev validation).
- Baseline HEAD: `e57e0a8` (working tree con cambios de prueba previos).

---

## 0. Resumen de priorización

| WP | Hallazgo | Severidad | Afecta | Orden | Complejidad | Estado plan |
|---|---|---|---|---|---|---|
| WP‑12‑FIX‑04 | K1 parser monto (0/3) | Alta | Decisión (dato erróneo) | 1 | B ***(rápido, testeable ya)*** | ✅ Hecho |
| WP‑12‑FIX‑01 | DVC‑03 overlay no aparece | P0 | E2E visible | 2 | Media‑Alta | ✅ Hecho |
| WP‑12‑FIX‑02 | DVC‑04 captura E2E no demostrada | P0 | E2E completo | 3 | Alta | ✅ Hecho |
| WP‑12‑FIX‑03 | DVC‑01 config no editable | Alta | Caso de uso real | 4 | Media | Pendiente |
| WP‑12‑FIX‑05 | Artefactos dispersos | Media | Higiene testing | 5 | Baja | Pendiente |

**Por qué FIX‑04 primero**: el parser es independiente de la captura, tiene
dataset real ya disponible (`DebugImageOcrReceiver` + assets), y su error corrompe
la decisión del conductor (dato más crítico del producto). Aisla el bug en
`:core:platform` sin depender de dispositivos ni de FIX‑01/02.

**Por qué FIX‑01 antes que FIX‑02**: el overlay es el elemento visible central;
FIX‑02 (captura real) requiere reintentos en campo y más instrumentación. Además
el overlay puede validarse de forma controlada (aparece/oculta/arrastre) incluso
con inyección de datos mientras se cierra FIX‑02.

---

## WP‑12‑FIX‑01 — Overlay físico (DVC‑03)

### A. Causa probable (no hay fix sin causa → Fase 1 de depuración ya hecha)

1. **Próxima determinista**: la visibilidad está atada a
   `visibleFor(status, evaluation)` (`PipelineOverlayDataSource.kt:113,256-259`).
   Con el pipeline en `OverlayState.DISABLED` y sin `evaluation`, `visible=false`
   y `OverlayContent.kt:59` no compone contenido → la ventana se añade pero es
   **transparente/vacía** ("no aparece nada").
2. **Sin CaptureRequests → pipeline nunca sale de DISABLED**: el único
   recolector es `CaptureAccessibilityService` (`CaptureAccessibilityService.kt:39-44`)
   que depende de que el servicio esté habilitado Y de que lleguen eventos reales
   de las 4 apps con oferta visible.
3. **Excepciones de `addView` tragadas en silencio**: `OverlayService.kt:202`
   (y `updateViewLayout` en `:221`) envuelven el WM en `runCatching` sin log ni
   estado de error → indetectable.
4. **`_isRunning` optimista**: `OverlayController.kt:28` marca "En ejecución"
   antes de arrancar; si el servicio se autodetiene por permiso/error/XOS, la UI
   miente.
5. **OEM (Infinix XOS)**: Hiber/hiberna apps y puede matar el FGS cuando
   `POST_NOTIFICATIONS` está denegado o sin eximir de batería. Evidencia en
   logcat: `Hiber/stateManager: freeze uid: 10232 com.sirc.app`.

Evidencia de diagnóstico (device): `SYSTEM_ALERT_WINDOW=allow` (concedido);
**no hay `OverlayService` corriendo** (`dumpsys activity services` solo muestra
`CaptureAccessibilityService`); **no hay ventana de overlay** en `dumpsys window`;
sin logs de `OverlayService`/`PipelineOverlay`/`CapturePipeline`.
`docs/testing/evidence/DVC_diagnostics_logcat_dump.txt`.

### B. Evidencia

- `feature/overlay/.../OverlayService.kt:117-124,143-209,202,211-222,250-266`
- `feature/overlay/.../OverlayController.kt:26-30`
- `feature/overlay/.../PipelineOverlayDataSource.kt:107-161,233-259`
- `feature/overlay/.../OverlayContent.kt:59-66`
- `dumpsys activity services` + `dumpsys window` + logcat Hiber (dump DVC).

### C. Severidad

P0 — el overlay es el elemento central del producto; sin él no hay decisión
visible. DVC‑03 → FAIL.

### D. Dependencias

- **Entrada**: `FIX‑02` (captura) alimenta el estado que haría visible el
  overlay con datos reales. No bloqueante: FIX‑01 puede arreglar la mecánica
  del overlay y probarse con inyección de datos (debug receiver).
- **Salida**: habilita OVL‑1…4, DEC‑1…4, VEL‑2/3, PARTE de E2E.

### E. Orden recomendado

Después de FIX‑04 (parser) y antes de FIX‑02 (captura). Independiente de FIX‑03/05.

### F. Alcance

1. Sustituir `runCatching` mudo por log + estado de error visible (WindowManager
   failures): `ensureOverlay()` y `applyVisibility()`.
2. Revertir/devolver `_isRunning` real (no optimista): exponer el estado del
   servicio/proceso, no el intento de arranque.
3. Aseguramiento de arranque: reintento o notificación clara de FGS/XOS
   (exención de batería + notificaciones) en Home/Diagnóstico.
4. Garantizar contenido mínimo visible incluso en `DISABLED` (p. ej. indicador de
   "esperando oferta" vs. ventana vacía) o manejar `visible=false` sin window
   vacía engañosa.
5. Logging de diagnóstico en `onStartCommand` (permiso, addView, reflexión).

> **Nota de ejecución (17-ago-2026)**: al hacer visible el indicador se detectó que
> `OverlayContent` usaba `fillMaxSize()`, haciendo la ventana `885x2436` (pantalla
> completa) y bloqueando los toques sobre la app cuando era touchable. Se quitó el
> `fillMaxSize()` → la ventana es un banner (`WRAP_CONTENT`, `885x223`) y no interfiere.
> También se extrajo `OverlayServiceLauncher` para testear `OverlayController` sin Android.

### G. Riesgos

- Añadir logs sin condición puede inflar latencia (mini). Usar `SircLogger`
  (debug solo si debuggable+DETAILED_LOGS).
- Cambiar estado de running puede romper UI que ya lee `isRunning` — revisar
  usos.
- XOS puede seguir matando el FGS aunque arreglemos código; mitigación es
  UX (pedir exención) no mágica.

### H. Criterios de aceptación (en DEVICE‑01)

- [x] `dumpsys window` muestra la ventana del overlay tras activarlo.
      VERIFICADO: `Window #6 com.sirc.app`, `ty=APPLICATION_OVERLAY`, `Requested 885x223`,
      `isVisible=true`, `Surface shown`, `HAS_DRAWN`.
- [x] El overlay aparece/desaparece según `visibleFor` y TTL.
      VERIFICADO: al activar → banner visible "Esperando oferta…"; al detener → ventana removida.
- [x] Si `addView`/`updateViewLayout` falla, se registra log `error` y la UI
      refleja fallo (no "En ejecución" falso). VERIFICADO por revisión de código
      (`ensureOverlay(): Boolean` → `logger.error` + `stopSelf`; UI se apaga vía
      `onServiceRunning(false)`).
- [x] `isRunning` refleja el estado real del servicio/proceso.
      VERIFICADO: UI "Activo" (servicio corriendo) → "Inactivo" (detenido/muerto).
- [ ] Reinstalar/rearrancar mantiene el servicio (`START_STICKY`).
      PENDIENTE de prueba a fondo; el código sigue devolviendo `START_STICKY` y el
      arranque tras reinstalación fue limpio (estado Inactivo, sin errores).

### I. Pruebas necesarias

Necesarias sí (device): OVL‑1 (aparece/desaparece/arrastre/TTL), OVL‑2 (valores),
OVL‑3 (FLAG_NOT_TOUCHABLE), VEL‑2/3 tras fijar captura. Tests unitarios
target: `OverlayControllerTest`, `OverlayService` logic (si se extrae),
`PipelineOverlayDataSourceTest` ya existente se amplía.

### J. Archivos afectados (proyección)

- `feature/overlay/src/main/kotlin/com/sirc/feature/overlay/OverlayService.kt`
- `feature/overlay/src/main/kotlin/com/sirc/feature/overlay/OverlayController.kt`
- `app/src/main/kotlin/com/sirc/app/HomeScreen.kt` / `DiagnosisScreen.kt`
  (mensajes de estado, opcional)
- Tests: `feature/overlay/src/test/.../OverlayControllerTest.kt` (nuevo),
  `PipelineOverlayDataSourceTest.kt` (ampliar).

### K. Complejidad relativa

Media‑Alta (Android servicios + OEM + estados).

### L. Qué debe hacerse primero

Diagnóstico confirmatorio en campo (no código): con app en primer plano,
`adb shell am start-foreground-service` y logs; verificar si XOS mata el FGS.
Luego el cambio mínimo 1+2 (log + estado real).

### M. Qué NO debe hacerse todavía

Nada de este WP hasta autorización.

---

## WP‑12‑FIX‑02 — Captura E2E (DVC‑04)

### A. Causa probable

1. **El flujo termina antes del pipeline en DEVICE‑01** (cero logs de
   `CapturePipeline`/`MediaProjectionCapture`): la ruta de accesibilidad es
   **silenciosa** — `CaptureAccessibilityService`, `AccessibilityCaptureInput` y
   `DebounceCaptureScheduler` no loguean rechazos (paquete/tipo/textos vacíos/
   dedup) → imposible distinguir "no llegan eventos" de "se descartan".
2. **MediaProjection no está activo** en el físico: `PROJECT_MEDIA = ignore;
   rejectTime` y `dumpsys media_projection` vacío (`null`). Sin proyección, solo
   se degrada a textos (passthrough) y nunca hay `imageData` (CAP‑4).
3. **Selección single‑app vs full‑screen del diálogo del sistema**: SIRC usa
   `createScreenCaptureIntent()` sin `MediaProjectionConfig` (`OverlayManager.kt:68-69`),
   por lo que si el usuario eligió "una sola app" distinta a la plataforma, no
   hay frames de ella. SIRC no guarda/controla esa selección (DVC‑02).
4. **Candidato raíz software** (auditoría STABILITY S‑S14, P0): el
   `android:exported="false"` del `CaptureAccessibilityService` — sin embargo en
   DEVICE‑01 el bind SÍ está establecido (`hasBound=true`, "Bound services" en
   `dumpsys accessibility`), por lo que en esta prueba **no** se manifestó.
5. **Dependencia de oferta real visible**: para que fluya un `CaptureRequest` se
   necesita una ventana real de una de las 4 apps con textos no vacíos.

**Pregunta no resuelta (diagnóstico incompleto)**: ¿fluye el evento de
accesibilidad y se descarta, o no fluye? Requiere instrumentación de logs
(primer cambio mínimo).

### B. Evidencia

- `feature/overlay/.../CaptureAccessibilityService.kt:33-50`
- `feature/overlay/.../AccessibilityCaptureInput.kt:45-87,95-129`
- `core/capture/.../scheduler/DebounceCaptureScheduler.kt:22-38`
- `core/capture/android/.../MediaProjectionCaptureInput.kt:30-46`
- `core/capture/android/.../provider/MediaProjectionScreenCaptureProvider.kt:61-147`
- `core/capture/android/.../projection/MediaProjectionService.kt:53-72`
- `feature/overlay/.../res/xml/accessibility_service_config.xml:3,10-11`
- Device: `settings get secure enabled_accessibility_services` (servicio
  habilitado+bindeado), `appops PROJECT_MEDIA=ignore`, `dumpsys media_projection`
  vacío — dump DVC.

### C. Severidad

P0 — flujo completo (captura→OCR→detección→parser→overlay) no demostrado en
físico; todo el E2E depende de esto. DVC‑04 → INSUFFICIENT_EVIDENCE.

### D. Dependencias

- **Entrada**: idealmente FIX‑01 (overlay) para ver resultado; parser (FIX‑04)
  para que el snapshot tenga monto correcto (aunque la prueba E2E puede contar
  la llegada del request/snapshot aunque parsee mal).
- **Salida**: habilita CAP‑1…5, PLT‑1…4 E2E, VEL‑1/2/3, EV‑1, DEC consecuentes.

### E. Orden recomendado

Después de FIX‑01. FIX‑03/05 independientes.

### F. Alcance

1. **Instrumentación de logs** en la ruta de accesibilidad (cada filtro/descarte
   con `SircLogger.debug`/`warn`): evento recibido → package → tipo → textos →
   dedup → debounce → emit. Y log en `MediaProjectionCaptureInput` (enriquecido
   o degradado).
2. Diagnosticar (con esos logs) si fluyen eventos en DEVICE‑01; con esa
   evidencia decidir el fix real.
3. **PROJECT_MEDIA**: revisar el flujo de consentimiento (RESULT_OK↔denegado),
   manejar `MediaProjection.Callback.onStop`, reintento y mensaje claro; valorar
   forzar `MediaProjectionConfig` para pantalla completa (Android 14+) o guiar
   al usuario a seleccionar la plataforma como single‑app. Persistir la
   preferencia si procede (DVC‑02).
4. Verificar `notificationTimeout`/tipos de evento no bloqueen
   (`accessibility_service_config.xml`).
5. Definir (producto) comportamiento single‑app vs full‑screen (DVC‑02).

### G. Riesgos

- SILENCIO log si no se usa `SircLogger` correctamente (debug solo con
  DETAILED_LOGS).
- Cambios en MediaProjection pueden romper Android 10‑15 (backport).
- La captura de pantalla completa captura datos sensibles de otras apps: se
  debe mantener solo lectura y anonimizar (regla R: solo lectura).
- No garantiza ofertas reales disponibles para probar (depende del usuario).

### H. Criterios de aceptación

- [ ] Con los logs de instrumentación se ve en logcat el recorrido completo de
      un evento real (o se confirma dónde se corta).
- [ ] `CaptureAccessibilityService` produce `CaptureRequest` con origin
      `ACCESSIBILITY` (y `MEDIA_PROJECTION` con frames si proyecta) cuando hay
      oferta real.
- [ ] `MediaProjectionCaptureInput` enriquece con `imageData` cuando proyecta.
- [ ] El pipeline emite snapshot (aunque K1 no corregido → documentar).
- [ ] Revocar/conceder reinicia correctamente (CAP‑5/CVD‑4).

### I. Pruebas necesarias

Unit: `AccessibilityCaptureInputTest` (ampliar filtros con logs), scheduler,
`MediaProjectionScreenCaptureProviderTest`. Device: CAP‑1…5, CAP‑2/3 (dedup/
debounce), CVD‑4.

### J. Archivos afectados (proyección)

- `feature/overlay/.../AccessibilityCaptureInput.kt`
- `feature/overlay/.../CaptureAccessibilityService.kt`
- `feature/overlay/.../CaptureModule.kt` (si se cambia flujo)
- `core/capture/android/.../MediaProjectionCaptureInput.kt`
- `core/capture/android/.../provider/MediaProjectionScreenCaptureProvider.kt`
- `feature/overlay/.../res/xml/accessibility_service_config.xml` (si procede)
- `app/.../HomeScreen.kt` (UX captura) y HomeViewModel

### K. Complejidad relativa

Alta (accesibilidad + MediaProjection + OEM + instrumentación).

### L. Qué debe hacerse primero

Solo instrumentación de logs (paso 1) para obtener evidencia de dónde se corta.
Después decidir fix.

### M. Qué NO debe hacerse todavía

Nada de este WP hasta autorización.

### N. Resultado (18-ago-2026) — COMPLETADO ✅

Causa raíz confirmada: el servicio SÍ estaba bindeado/suscrito
(`eventTypes=[TYPE_WINDOW_STATE_CHANGED, TYPE_WINDOW_CONTENT_CHANGED]`,
`notificationTimeout=100`), pero `android:packageNames` del config XML filtraba a
nivel sistema los eventos del paquete real de InDrive en Ecuador
(**`sinet.startup.inDriver`**; el dispositivo NO tiene `com.ubercab.driver`, solo
`com.ubercab` pasajero). Además el código no logueaba rechazos (ruta silenciosa).

Fix aplicado: `accessibility_service_config.xml` con `packageNames` ampliado
(incluye `sinet.startup.inDriver`) y `PlatformDescriptors.kt` con `packageNames`
para UBER e INDRIVE. Instrumentación con `SircLogger` en `AccessibilityCaptureInput`,
`DebounceCaptureScheduler`, `DefaultCapturePipeline`, `MediaProjectionCaptureInput`
y `PipelineOverlayDataSource` (log "overlay mostrando:").

Evidencia en DEVICE-01 (InDrive Ecuador, usuario en línea, 18-ago):
`detección: INDRIVE / REQUEST` · `snapshot INDRIVE guardado: parse 5.7–13.9 ms ·
total 24.7–40.9 ms` (detección 16.2–28.0 ms) · `PipelineOverlay: overlay mostrando:
INDRIVE · $2.9/$3.1 · REJECT (origen=ACCESSIBILITY · eval 1.3–8.6 ms · reglas
0.0–0.7 ms · overlay 8.8–15.6 ms)` · ventana overlay `Requested 885x280 ·
isVisible=true · HAS_DRAWN` · Room `offer_history` con 74 ofertas reales (incluye
ACCEPT/WARNING de ofertas >$5). MediaProjection: 1 frame real enriquecido (300759
bytes PNG; OCR ~2.5 s en frame completo, pantalla HOME sin oferta); tras denegación
(`PROJECT_MEDIA=ignore`) degrada a textos. Evidencia en `/sdcard/SIRC_TEST/`.

Criterios de aceptación: ✅ logs del recorrido completo · ✅ `CaptureRequest` real
`ACCESSIBILITY` + `MEDIA_PROJECTION` con frame · ✅ enriquecimiento `imageData`
cuando proyecta · ✅ snapshot emitido (K1 ya corregido en FIX-04) · ⚠️ revocar/
conceder: con `PROJECT_MEDIA=ignore` no se proyecta → degrada (documentado; DVC-02
sigue como PENDIENTE).

Tests: `DebounceCaptureSchedulerTest` (4, +1 logging), `PlatformDetectionEngineTest`
(9, +2 seed `sinet.startup.inDriver`→INDRIVE / `com.ubercab.driver`→UBER). Suite
completa AGENTS en verde (ktlintCheck, lintDebug, assembleDebug, tests JVM +
`:core:capture:android`).

Hallazgos para el siguiente WP (no corregidos): sesión/última oferta del panel
Debug en memoria (se pierde al reiniciar SIRC); uiautomator "null root node" en
SIRC tras ciclo de overlay; OCR ~2.5 s en frames MediaProjection completos.

---

## WP‑12‑FIX‑03 — Configuración editable (DVC‑01)

### A. Causa probable (confirmada en código)

- La capa de datos **ya lo permite**: `DriverConfigRepository.save()` es un
  upsert completo (`DefaultDriverConfigRepository.kt:21-23` → `DriverConfigDao.kt:18-19`,
  `@Insert REPLACE` fila única id=1) y `SettingsViewModel.save()` ya persiste todo
  el config. El síntoma es **ausencia de UI**: Settings solo expone moneda,
  costos (`costPerKm/Minute/Trip`), umbrales y overlay (`SettingsScreen.kt:45-81`).
- País, ciudad, nombre, vehículo completo, `fuelPrice`, `maintenanceCostPerKm`,
  `additionalCosts` y `platforms` solo se capturan en el onboarding
  (`OnboardingSteps.kt:39-99,111-139,145-167`) y **no tienen re-entrada**: el
  onboarding se muestra solo si `isConfigured == false` (`SircRoot.kt:12,31`),
  donde `isConfigured` = `config != null` (`DefaultDriverConfigRepository.kt:19`).
- No hay botón de reset/reconfig en la app (Eloquent DebugPanel solo reinicia el
  coordinador, no borra config).

**Impacto en el cálculo**: la evaluación usa config (costos/umbrales/plataformas);
con datos obsoletos, decisiones incorrectas (DVC‑01 impacto Alto).

### B. Evidencia

- `feature/onboarding/.../OnboardingSteps.kt:39-99,111-139,145-167`
- `feature/onboarding/.../OnboardingViewModel.kt:74-80`
- `feature/settings/.../SettingsScreen.kt:45-81,154-159`
- `feature/settings/.../SettingsViewModel.kt:38-76`
- `app/.../SircRoot.kt:12,31`; `DefaultDriverConfigRepository.kt:19-23`
- `domain/.../DriverConfigRepository.kt:8-17`

### C. Severidad

Alta — caso de uso real (cambio de vehículo/ciudad/costos/plataformas) no
termina; decisiones basadas en config obsoleta. DVC‑01 → FAIL.

### D. Dependencias

Independiente de FIX‑01/02/04/05 (solo UI+ViewModel+repositorio ya existente).
Puede probarse sin captura ni overlay.

### E. Orden recomendado

Después de FIX‑01/02 (o en paralelo si se desea; es módulo separado
`feature:settings`/`feature:onboarding`), antes de higiene.

### F. Alcance

1. Añadir edición reutilizable en Settings de: perfil (país/ciudad/nombre),
   vehículo (marca/modelo/año/combustible/consumo), costos
   (`fuelPrice`/`maintenanceCostPerKm`/`additionalCosts`) y plataformas
   (reusar composables del onboarding si es viable, o nueva UI en settings).
2. Persistir con `saveDriverConfig` (upsert completo ya funciona).
3. NO romper Room/Hilt/Domain: el repositorio/de la entidad ya cubren todos los
   campos; solo falta exponerlos.
4. Verificar que la evaluación realmente usa cada campo editado (cálculo de
   rentabilidad) — mapear campo→uso.

### G. Riesgos

- Reutilizar composables del onboarding desde settings aumenta acoplamiento
  (`feature:settings` no depende de `:feature:onboarding` hoy).
- Duplicar UI = mantener dos versiones (riesgo de drift).
- Validar inputs (precios > 0, plataforma al menos 1).

### H. Criterios de aceptación

- [x] Config existing visible y editable en Settings (país/ciudad/vehículo/
      costos/plataformas/objetivos).
- [x] Guardar persiste y segunda apertura refleja cambios.
- [x] Los cambios alteran el cálculo de rentabilidad (comprobado/mapeado).
- [x] Sin migrations de Room (entidad ya tiene los campos) y sin cambios en
      Domain/Hilt.

### I. Pruebas necesarias

Unit: ViewModel de settings (carga/guardado, no pierde campos no editados).
Instrumented/device: INST‑3 (CFG‑1) tras edición. Compose UI tests si existen.

### J. Archivos afectados (proyección)

- `feature/settings/.../SettingsScreen.kt`
- `feature/settings/.../SettingsViewModel.kt`
- (opcional) composables de onboarding reutilizables `feature/onboarding/.../OnboardingSteps.kt`
- Tests de settings.

### K. Complejidad relativa

Media.

### L. Qué debe hacerse primero

Mapear campo→cálculo (qué campos editar afectan la decisión) y decidir UI
(reutilizar vs nueva) con el usuario.

### M. Qué NO debe hacerse todavía

Nada de este WP hasta autorización.

### N. Resultado (18-ago-2026) — COMPLETADO ✅

- **Diseño (Opción A, única fuente de verdad)**: `costPerKm` es DERIVADO
  (`fuelPrice/consumptionKmPerUnit + maintenanceCostPerKm + Σ additionalCosts.costPerKm`);
  la UI lo muestra como "Costo por km (calculado)" SOLO lectura y edita sus
  componentes; el motor ignora `costs.costPerKm` manual; al guardar
  `normalizeCostPerKm()` persiste la columna con el derivado.
- **Cambios**: `SettingsViewModel.kt` reescrito (UiState con `derivedCostPerKm` +
  `reloadTick` + `persistedConfig`; `togglePlatform`; `discard()`; `save()`
  normalizado); `SettingsScreen.kt` reescrito (Perfil/Vehículo/Costos/
  Plataformas/Umbrales/Overlay + Guardar/Descartar); `AccessibilityCaptureInput.kt`
  con gate de plataformas (set vacío = acepta todas; logs info de rechazo);
  `feature/settings/build.gradle.kts` + coroutines-test.
- **Tests nuevos**: `SettingsViewModelTest` (6), `AccessibilityCaptureInputTest`
  (3), `ProfitEvaluationEngineTest` (+3 derivación/manual ignorado/decisión).
- **2 bugs reales en dispositivo corregidos**: (1) campos `rememberSaveable` no
  re-sembrados al cargar config persistida (Año mostraba 2020 con BD=2021) →
  `reloadTick` como resetKey; (2) crash `Parcel: unknown type for value
  CostDraft` → `costDrafts` con `remember(reloadTick)`.
- **Validación física (Infinix X6850, Android 15)**: derivado 0.5417 → fuel
  0.5→1.5 → 0.625 en vivo → +costo "Peaje"/0.3 → 0.925 en vivo; Cabify activado;
  Quito→Guayaquil; guardar → BD `costPerKm=0.925` (normalizado),
  `fuelPrice=1.5`, `additionalCosts='Peaje^_0.3'`,
  `platforms='CABIFY,INDRIVE,UBER'`, `city=Guayaquil`; tras force-stop/reopen
  TODO persiste. Suite AGENTS completa en verde. Evidencia:
  `/sdcard/SIRC_TEST/fix03/`.

---

## WP‑12‑FIX‑04 — Parser de monto (K1)

### A. Causa exacta (confirmada en código)

1. `AMOUNT_RUN` (`OfferTextParser.kt:116-121`) captura **cualquier número** con
   opción de prefijo/sufijo de moneda: `(?:(USD|…)\s*)?(R\$\s*|\$\s*|…)?([0-9][0-9.,]*)`
   → el grupo 3 (números) no exige símbolo ni contexto → captura "479", "090",
   "5", "25.53", "4,50", "USD5", "USD4.5".
2. `parseAmount` normaliza decimales correctamente (4,50 → 4.5), pero **no filtra
   por contexto** — `looksLikeUnit` solo excluye km/min/rating, no direcciones
   ni números de calle.
3. `chooseAmount` de `GenericPlatformExtractor.kt:51-73`: si no hay candidatos
   con score>0, **fallback `maxByOrNull { it.value }`** → elige el número mayor
   (que es el de calle "479", "090" incluido, o "USD5") en lugar del monto real
   marcado con $/monto: 479.0 / 5.0 / 90.0 vs reales 4.50 / 4.50 / 25.53.
4. Las `totalKeywords`/`fareKeywords` por plataforma (`PlatformDescriptors.kt:52-56,
   65-70,79-84,91-96`) NO incluyen "aceptar por"/"urt", por lo que el candidato
   correcto ("Aceptar por $4,50", "$25.53") no gana el scoring cuando el fallback
   entra.

**Resultado**: 3/3 ofertas con monto mal extraído (K1) aunque el OCR leyó bien.

### B. Evidencia

- `core/platform/.../OfferTextParser.kt:28-42,94-121,138-151`
- `core/platform/.../GenericPlatformExtractor.kt:51-73` (chooseAmount fallback)
- `core/platform/.../PlatformDescriptors.kt:52-56,65-70,79-84,91-96`
- Logcat real: `docs/testing/evidence/SIRC_OCR_TEST_logcat_dump.txt`
  (indriver_1 total=479.0 vs "$4,50"; indriver_2 total=5.0 vs USD4.5; uber_2
  total=90.0 vs $25.53).

### C. Severidad

Alta — la decisión del conductor muestra un monto/rentabilidad incorrecta
(dato clave del producto). K1 → FAIL.

### D. Dependencias

**Ninguna** de captura/overlay. Se prueba con `DebugImageOcrReceiver`
(independiente de FIX‑02). Bloquea la corrección de PLT/DEC.

### E. Orden recomendado

**PRIMERO** (puede validarse ya, mucha cobertura de tests en `:core:platform`,
dataset real disponible). No depender de dispositivo para iterar.

### F. Alcance

1. Rediseñar la selección de monto: exigir (i) símbolo/currency O (ii) keyword de
   contexto ("aceptar", "total", "monto", "ganancia", "recibe", "neto") para que
   un candidato gane; eliminar/endurecer el fallback `maxByOrNull`.
2. Ignorar contextos de dirección/calle (números seguidos de "A"/"Pa."/"Mt"
   o con letras mezcladas tipo "479 A 1 Pa."), y evitar `$090` tipo "incluido".
3. Soporte `USD`/`$` completo: `4.7 (21) $4.50, Efectivo` y `Aceptar por USD4.5`
   ya matchean; asegurar parse con sufijo y prefijo.
4. Manejar formato decimal latino `4,50` (ya parsea; verificar no romper).
5. Extender keywords por plataforma si hace falta (datos reales).
6. Tests con los textos reales del dataset (fixtures del dump).

### G. Riesgos

- Sobre‑ajustar al dataset de 5 imágenes (validar contra más capturas reales).
- Regresiones en parsers DiDi/Cabify (mismos regex, keywords distintas).
- Romper casos válidos: "10 km" (distancia) no debe contarse como monto; los
  límites actuales (MAX_AMOUNT 1e6, UNIT_WORDS) deben mantenerse.

### H. Criterios de aceptación

- [ ] Con los 5 textos reales del dataset, `estimatedTotal` es correcto en las
      3 ofertas (4.50 / 4.50 / 25.53) y NULL en las 2 no‑oferta.
- [ ] dist y dur siguen extraidas bien (16.4/13.0, 99.0/128.0, etc.).
- [ ] No regresiona: suite `:core:platform:test` completa en verde (incluidos
      DiDi/Cabify/Uber).
- [ ] El `DebugImageOcrReceiver` reproduce los mismos resultados en DEVICE‑01
      (logcat `SIRC-OCR-TEST`).

### I. Pruebas necesarias

Unit: `OfferTextParserTest` (monto/contexto/dirección/USD/latino), ampliar
`GenericPlatformExtractorTest` / orquestador con fixtures del dataset real.
Device (opcional, reproducible): re-ejecutar `com.sirc.debug.OCR_TEST`.

### J. Archivos afectados (proyección)

- `core/platform/.../OfferTextParser.kt`
- `core/platform/.../GenericPlatformExtractor.kt` (chooseAmount)
- `core/platform/.../PlatformDescriptors.kt` (keywords, si procede)
- Tests: `core/platform/src/test/...`.

### K. Complejidad relativa

Baja (pero requiere cuidado con el dataset).

### L. Qué debe hacerse primero

Redactar el fixture real (textos del dump) como tests que fallan, después fix
mínimo en parser/extractor. Verificable al 100 % sin dispositivo.

### M. Qué NO debe hacerse todavía

Nada de este WP hasta autorización.

---

## WP‑12‑FIX‑05 — Test Artifact Hygiene

### A. Causa probable (hecho, no bug)

Los artefactos de prueba se generaron dispersos en la raíz de `/sdcard` por
`uiautomator dump`/screenshots manales y la carpeta `sirc_test`; no hay una
convención única de raíz de testing en dispositivo.

### B. Evidencia

Catálogo real del device (16-ago-2026):
- `/sdcard/sirc_test/` → `imagen 1 indriver.jpg`, `imagen 1 uber.jpg`,
  `imagen 2 indriver.jpg`, `imagen 2 uber.jpg`, `imagen 3 uber.jpg` (5 JPG).
- `/sdcard/sirc_smoke1.png`, `/sirc_e2e_uber_img.png`, `/sirc_state_checks_ok.png`
  y **29** dumps `sirc_ui*.xml` (`sirc_ui.xml`, `sirc_ui2..30.xml`) en la raíz.
- Download/DCIM contienen archivos personales (NO SIRC): catalogar para NO tocar.
- (Carpeta `docs/testing/evidence/` en repo ya existe para evidencia local.)

### C. Severidad

Media (higiene/testing; no afecta producto pero ensucia el storage y dificulta
auditoría/cleanup).

### D. Dependencias

Ninguna. Puede ir al final (mientras FIX‑04 use el dataset es plagiaño de pruebas
— pero el dataset vive en assets del APK, no en device; no bloquea).

### E. Orden recomendado

Último (después de haber capturado toda la evidencia necesaria). No borrar aún.

### F. Alcance

1. Definir convención: todo SIRC testing bajo `/sdcard/SIRC_TEST/` con
   subdirectorios `images/`, `logs/`, `evidence/`, `exports/`, `tmp/`.
2. Mover (no borrar) los artefactos actuales a esa raíz (operator: mover, salvo
   el dataset de imágenes que podrían quedar como `images/`).
3. Documentar el procedimiento de generación de artefactos para que las
   próximas pruebas escriban allí.
4. Cleanup seguro (cuando se autorice): borrar SOLO artefactos con prefijo
   `sirc_`/carpeta `sirc_test`→`SIRC_TEST`; verificar que NO toca archivos
   personales (nunca Download/DCIM/Pictures personales).

### G. Riesgos

- Borrar algo personal si el prefijo no es seguro → verificar cada archivo antes
  de borrar; nómina `ls` la utilizamos.
- Copias locales (assets `app/src/debug/assets/sirc_test`) duplicadas: decidir si
  el device es la única verdad o si los assets del APK siguen siendo la fuente de
  pruebas automatizadas.

### H. Criterios de aceptación

- [x] `/sdcard/SIRC_TEST/{images,logs,evidence,exports,tmp}/` creado.
- [x] Artefactos existentes movidos (nada en la raíz `/sdcard` con prefijo sirc).
- [x] No se borra nada personal (lista previa verificada).
- [x] Procedimiento documentado (ver "N. Resultado" a continuación).

### I. Pruebas necesarias

Manual en device: `ls` raíz, verificar raíz SIRC_TEST, verificar Download/DCIM
intactos.

### J. Archivos afectados (proyección)

- Device storage únicamente (no código). Documentación si procede.

### K. Complejidad relativa

Baja (operativa).

### L. Qué debe hacerse primero

Catálogo completo (ya hecho arriba + lista de no-tocar) y aprobación de la
convención de rutas.

### M. Qué NO debe hacerse todavía

**No borrar nada.**

### N. Resultado (18-ago-2026) — COMPLETADO ✅

**Procedimiento (convención vigente de artefactos de prueba):** todas las
pruebas de SIRC en dispositivo escriben bajo `/sdcard/SIRC_TEST/` con
subdirectorios por tipo:

- `images/` — screenshots/capturas (PNG/JPG): `debug_panel_offer*.png`,
  `sirc_overlay.png`, `sirc_smoke1.png`, `sirc_state_checks_ok.png`,
  `sirc_e2e_uber_img.png`, imágenes OCR `imagen 1/2/3 *.jpg`.
- `logs/` — logs del pipeline (`e2e_final.log`, `e2e_pipeline.log`).
- `evidence/` — evidencia formal por WP (`README.txt`, `offer_history.txt`,
  `fix03/` con su evidencia).
- `exports/` — exportaciones (vacíía hoy; reservada para exports del panel).
- `tmp/` — artefactos temporales (`uiautomator dump` `sirc_ui*.xml`, `ui.xml`,
  `home_*.xml`, `debug_*.xml`, `sirc5.xml`, `sirc_home.xml`).

**Hecho (18-ago):**
- Catálogo previo real del device: `/sdcard/SIRC_TEST/` con 52 archivos en raíz
  + `fix03/` (2) + 3 sueltos en `/sdcard` (`sirc5.xml`, `sirc_home.xml`,
  `sirc_overlay.png`). Se confirmó que `/sdcard/sirc_test` y `/sdcard/SIRC_TEST`
  son la MISMA carpeta (sdcard case-insensitive): no había duplicados.
- Reubicados **57 archivos** (sin borrar ninguno): 14 → `images/`, 2 → `logs/`,
  3 → `evidence/` (+`fix03/` → `evidence/fix03/`), 38 → `tmp/`.
- Raíz `/sdcard` quedó sin archivos `sirc_*` sueltos (solo la carpeta SIRC_TEST).
- No se tocaron archivos personales: `/sdcard/Download` y `/sdcard/DCIM`
  catalogados como NO-tocar (sin coincidencias `sirc`). Verificado con
  `find` en Download/DCIM/Pictures/Documents/Movies/Music/Android → 0
  coincidencias `sirc`.
- **Integridad**: 57 archivos antes = 57 después; tamaños intactos; evidencias
  clave legibles (`README.txt`, `offer_history.txt`, `e2e_final.log`,
  `fix03_evidence.txt`, `shot_settings_platforms.png`). Nada se eliminó.
- **Sin cambios de código de producción** (solo device storage + docs).
- **Limpieza final del Sprint**: al terminar Sprint 12 bastará borrar la única
  carpeta `/sdcard/SIRC_TEST/`.

---

## 1. Orden de ejecución recomendado

1. **WP‑12‑FIX‑04** (parser) — independiente, testeable ya con dataset real, alto valor.
2. **WP‑12‑FIX‑01** (overlay) — mecánica del overlay; se valida con inyección de datos.
3. **WP‑12‑FIX‑02** (captura E2E) — sobre overlay arreglado; instrumentación primero, fix después.
4. **WP‑12‑FIX‑03** (config editable) — independiente; producto.
5. **WP‑12‑FIX‑05** (higiene) — tras cerrar evidencia.

Dependencias clave con evidencia:
- FIX‑02 → FIX‑01: el overlay necesita estado del pipeline (alimentado por captura) para mostrarse con datos reales.
- FIX‑04 → nada (fallback solo; dataset embebido en assets del APK).
- FIX‑03 → nada. FIX‑05 → nada.

## 2. Criterios de salida del Sprint 12/E1a (a re-evaluar tras fixes)

- CAP‑1…5, OVL‑1…4, PLT‑1…4 E2E, VEL‑1/2/3, DEC‑1…4, CVD‑1…4, BAT‑1/2, EV‑1
  con muestra ≥ mínima (§6.1). Pruebas in-field con cuenta real de Uber/InDriver
  (pendiente usuario). No cerrar Sprint sin autorización explícita.
# GOOGLE_PLAY_AUDIT — SIRC (`com.sirc.app`)

**Rol:** Google Play Compliance Reviewer
**Fecha:** 2026-08-01
**Alcance:** Accessibility, MediaProjection, Foreground Services, overlay, notificaciones, consentimiento, Data Safety.
**Modo:** Solo lectura. No se modificó código.

> App para conductores de Uber/DiDi/Cabify/InDrive: AccessibilityService (solo
> lectura) + MediaProjection (OCR local) + overlay flotante de rentabilidad.
> Categoría de alto escrutinio en Google Play.

---

## Resumen ejecutivo

El **comportamiento técnico en runtime es ejemplar** para esta categoría:
accessibility solo lectura (sin `performAction`/gestos), flujo de MediaProjection
con consentimiento correcto y FGS creado antes de `getMediaProjection()`,
procesamiento 100 % local, sin permiso `INTERNET`, y overlay no bloqueante.

Sin embargo, hay **1 hallazgo CRITICO y 4 ALTOS**, todos en documentación,
consentimiento y ficha de Play (no en el comportamiento). El más grave es la
instrucción interna de declarar "sin datos recolectados" en Data Safety, lo cual
**no es cierto** y constituiría una declaración falsa ante Google.

| Severidad | Cantidad |
|---|---|
| CRITICO | 1 |
| ALTO | 4 |
| MEDIO | 3 |
| BAJO | 3 |

---

## HALLAZGOS

### GP-1 · CRITICO — Declaración de Data Safety "sin datos recolectados" es falsa

| | |
|---|---|
| **Ubicación** | `docs/GOOGLE_PLAY_COMPLIANCE.md:100` — *"Completar la declaración en Play Console (Data safety): **sin datos recolectados**."* |
| **Política** | Google Play Developer Program Policy · User Data / Misrepresentation |

La app **sí recopila** datos según la política de Google Play:

- **Texto de pantalla de otras apps** vía Accessibility
  (`feature/overlay/.../SircAccessibilityService.kt:76-81`, `.../CaptureAccessibilityService.kt:86-91`).
- **Capturas de pantalla** vía MediaProjection, procesadas con OCR on-device
  (`core/capture/android/.../MediaProjectionScreenCaptureProvider.kt:128-143`,
  `feature/overlay/.../MlKitOcrEngine.kt:27-45`).
- **PII del conductor** persistida en Room: `name`, `country`, `city`, `currency`,
  vehículo y costos (`data/.../entity/DriverConfigEntity.kt:20-31`).
- **Historial de ofertas** persistido (`data/.../entity/OfferHistoryEntity.kt:7-26`).

Aunque todo el procesamiento es local y no se transmite, Google exige declarar
estos datos en el formulario de Data Safety (con la nota de que se procesan en el
dispositivo). Declarar "sin datos recolectados" es una **declaración falsa** y
causa directa de rechazo/despublicación.

**Recomendación:** corregir `docs/GOOGLE_PLAY_COMPLIANCE.md:100` y declarar en
Play Console:
- *Screen content / other app content* — texto e imágenes, procesado en
  dispositivo, **no transmitido**.
- *Personal info* (perfil del conductor).
- *Other app activity / App activity* (historial de ofertas).
- Notas: cifrado en tránsito "no aplica" (no hay red), opción de borrado del
  historial disponible (`data/.../DefaultOfferHistoryRepository.kt:28-30`).

---

### GP-2 · ALTO — Sin política de privacidad

| | |
|---|---|
| **Ubicación** | Repo completo (no existe archivo de policy; `docs/` no lo incluye; `README.md:81-91` no la lista) |
| **Política** | Play Console exige URL de política de privacidad para apps que recopilan datos personales o sensibles, aunque no se transmitan. |

La app recopila PII (nombre, ciudad, país, vehículo) y contenido de pantalla.
**No existe política de privacidad** ni archivo enlazable en el repo.

**Recomendación:** publicar una política de privacidad (repo o página) que detalle:
qué datos se recopilan, que el procesamiento es local, que no se transmiten datos,
conservación y borrado (historial), y contacto. Enlazarla en Play Console.

---

### GP-3 · ALTO — Sin "prominent disclosure" in-app sobre Accessibility y captura de pantalla

| | |
|---|---|
| **Ubicación** | `feature/onboarding/src/main/.../OnboardingScreen.kt:35-117` · `app/src/main/kotlin/com/sirc/app/HomeScreen.kt:193-237` |
| **Política** | Accessibility Service Policy + Permissions Policy requieren divulgación destacada in-app: qué se usa, qué datos se leen, por qué y cómo se protegen. |

- El onboarding solo pide perfil/vehículo/costos/plataformas/objetivos; **no hay
  pantalla de consentimiento** sobre accesibilidad, captura de pantalla ni datos.
- En Home hay textos informativos (`HomeScreen.kt:197-199` *"Todo el análisis es
  local, nada sale del dispositivo"*; `:233` *"SIRC solo lee la pantalla: nunca toca
  botones ni decide por ti"*), pero **no constituyen un diálogo de divulgación
  destacada con consentimiento activo**.
- La única descripción formal está en los Ajustes del sistema
  (`feature/overlay/.../res/values/strings.xml:5-19`), que el usuario rara vez lee.

**Recomendación:** añadir una pantalla/diálogo en la activación de la captura que
explique: (1) SIRC usa el servicio de accesibilidad y captura de pantalla, (2) lee
el texto/imágenes visibles de las ofertas, (3) lo procesa localmente y no lo envía
a ningún servidor, (4) no toca botones ni acepta/rechaza viajes. Requerir
confirmación explícita.

---

### GP-4 · ALTO — Dos Accessibility Services redundantes para el mismo fin

| | |
|---|---|
| **Ubicación** | `feature/overlay/src/main/AndroidManifest.xml:11-35` · `SircAccessibilityService.kt` · `CaptureAccessibilityService.kt` |
| **Política** | Accessibility Service Policy exige el acceso mínimo necesario. |

- `SircAccessibilityService` y `CaptureAccessibilityService` usan **el mismo**
  `@xml/accessibility_service_config` y hacen **exactamente lo mismo** (leer
  `node.text`/`contentDescription`), solo difieren en el destino del evento.
- Aparecen como **dos entradas separadas** en los Ajustes de accesibilidad con
  labels distintos (`strings.xml:3-4`): *"Análisis de ofertas SIRC"* y
  *"Captura de ofertas SIRC"*.
- Además `PermissionManager.hasAccessibilityPermission()` solo comprueba
  `SircAccessibilityService` (`PermissionManager.kt:50-57`): el usuario podría
  activar solo uno y romper el otro pipeline.

**Riesgo:** apariencia de acceso de accesibilidad no mínimo necesario y
redundante → escrutinio manual y posible rechazo.

**Recomendación:** consolidar en **un único** AccessibilityService y enrutar los
eventos al coordinador/pipeline según corresponda. Eliminar el segundo servicio,
su manifest y sus strings.

---

### GP-5 · ALTO — FGS `specialUse` siempre encendido + prompt de exención de batería

| | |
|---|---|
| **Ubicación** | `feature/overlay/src/main/kotlin/com/sirc/feature/overlay/OverlayService.kt:75` (`START_STICKY`) · `app/src/main/kotlin/com/sirc/app/HomeScreen.kt:126-143` |
| **Política** | Foreground Services Policy / Battery optimization. |

- `OverlayService` retorna `START_STICKY` y se mantiene ejecutándose
  **indefinidamente** mientras el overlay esté activo. Es el núcleo de la app,
  pero un FGS `specialUse` siempre-encendido es el patrón que Play escruta más.
- La app anima al usuario a eximirla de la optimización de batería
  (`HomeScreen.kt:130-131`). No usa `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS`, solo
  abre `ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS` (`PermissionManager.kt:90-92`),
  lo cual es menos agresivo, pero sigue siendo señal de revisión.

**Recomendación:**
1. Justificar el tipo `specialUse` con claridad en Play Console (formulario FGS):
   el overlay debe permanecer visible durante toda la sesión de conducción; no
   existe un tipo FGS más adecuado.
2. Considerar si la exención de batería es imprescindible; si el overlay funciona
   sin ella, retirar el prompt para reducir fricción con la política.
3. Revisar el impacto del timeout del sistema del FGS `mediaProjection`
   (3 h en Android 14 / 6 h en Android 15) y declarar el caso de uso.

---

### GP-6 · MEDIO — Justificación de `specialUse`/`mediaProjection` en Play Console insuficiente (subjetivo)

| | |
|---|---|
| **Ubicación** | `feature/overlay/src/main/AndroidManifest.xml:41-43` · `core/capture/android/src/main/AndroidManifest.xml:13-15` |

Los `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` están bien redactados en español. El riesgo
es que la justificación en el formulario de Play Console debe coincidir y
demostrar que no hay alternativa. Preparar un texto en inglés con el caso de uso
concreto.

---

### GP-7 · MEDIO — Overlay cubre ~82 % de la pantalla

| | |
|---|---|
| **Ubicación** | `feature/overlay/src/main/kotlin/com/sirc/feature/overlay/OverlayService.kt:170-186` |

El overlay puede tapar parte de la UI de la app de transporte. Está mitigado por
`FLAG_NOT_TOUCHABLE` cuando está oculto y por la opción de arrastre/cierre, pero
el tamaño es revisable. Considerar hacerlo colapsable o más estrecho.

---

### GP-8 · MEDIO — `notificationTimeout=100` + `TYPE_WINDOW_CONTENT_CHANGED` → alta frecuencia de eventos

| | |
|---|---|
| **Ubicación** | `feature/overlay/src/main/res/xml/accessibility_service_config.xml:3,10` |

Consumo energético y de eventos elevado. Mitigado por deduplicación por huella en
los servicios. Considerar subir `notificationTimeout` (p. ej. 300 ms).

---

## ÁREAS CON CUMPLIMIENTO CORRECTO (BAJO / positivo)

| Área | Evaluación | Ubicación |
|---|---|---|
| **Accessibility — configuración** | `canPerformGestures="false"`, `canRequestFilterKeyEvents="false"`, `canRetrieveWindowContent="true"`, `packageNames` restringido a 4 apps, `settingsActivity` → MainActivity. Sin `performAction`/`dispatchGesture` en el repo. Descripción bilingüe correcta. | `accessibility_service_config.xml:3-11` · `strings.xml:5-19` |
| **MediaProjection — consentimiento** | Intent creado con `createScreenCaptureIntent()` desde acción del usuario en Home; `RESULT_OK` validado antes de arrancar; FGS `mediaProjection` creado **antes** de `getMediaProjection()`; cancelación y `onStop` del sistema manejados. | `HomeScreen.kt:69-74,204-205` · `MediaProjectionService.kt:42-61` · `MediaProjectionScreenCaptureProvider.kt:56-65,88-99,108-113` |
| **MediaProjection — local** | Contenido procesado 100 % local (OCR ML Kit on-device); sin persistencia de imágenes; frames en `Channel(CONFLATED)`; **sin INTERNET**. | `MediaProjectionScreenCapture.kt:47-51` · `MlKitOcrEngine.kt:31` |
| **Overlay** | `TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE`, permiso vía `Settings.ACTION_MANAGE_OVERLAY_PERMISSION`. No intercepta toques ajenos. | `OverlayService.kt:177,239-242` · `PermissionManager.kt:70-77` |
| **Notificaciones** | `POST_NOTIFICATIONS` en runtime; canales `IMPORTANCE_LOW` con descripciones; notificaciones FGS `setOngoing(true)`. | `HomeScreen.kt:55-66` · `MediaProjectionService.kt:63-92` · `OverlayService.kt:203-232` |
| **Permisos** | Solo los necesarios; sin ubicación, contactos, cámara ni almacenamiento. | manifiestos |

---

## Posibles motivos de rechazo (priorizados)

1. **Data Safety inexacto** ("sin datos recolectados") → misrepresentation, rechazo/despublicación. **CRITICO**
2. **Sin política de privacidad** → requisito obligatorio al recopilar datos personales. **ALTO**
3. **Sin prominent disclosure in-app** sobre Accessibility/MediaProjection/datos. **ALTO**
4. **Dos Accessibility Services redundantes** → acceso de accesibilidad no mínimo necesario. **ALTO**
5. **FGS `specialUse` siempre encendido + prompt de batería** → escrutinio de política FGS/battery. **ALTO**
6. Justificación `specialUse`/`mediaProjection` en Play Console insuficiente. **MEDIO**
7. Overlay de gran tamaño sobre otras apps. **MEDIO**
8. `notificationTimeout` agresivo (consumo). **BAJO**
9. La ficha/listing debe declarar el uso de Accessibility y que no automatiza acciones; debe coincidir con el código. **MEDIO**

---

## Evaluación global

**Veredicto:** no aprobable en el estado actual. La técnica de permisos y runtime
cumple el espíritu de la política; **los bloqueantes son de documentación y
consentimiento** (GP-1, GP-2, GP-3). GP-4 y GP-5 son altamente recomendables antes
de someter a revisión para evitar escrutinio manual y rechazos. Los hallazgos
positivos (procesamiento local, sin red, cancelaciones correctas) deben usarse
como argumentos de la ficha.

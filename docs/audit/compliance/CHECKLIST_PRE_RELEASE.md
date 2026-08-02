# CHECKLIST PRE-RELEASE — SIRC (`com.sirc.app`)

Lista de verificación derivada de `SECURITY_AUDIT.md` y `GOOGLE_PLAY_AUDIT.md`.
**Bloqueantes** = imprescindibles antes de subir a Google Play. **Recomendados** =
reducen riesgo de rechazo en revisión manual. **Verificación técnica** = gate de
calidad local.

---

## BLOQUEANTES

### B1 · Corregir declaración de Data Safety en Play Console (y en `docs`)
- [ ] Corregir `docs/GOOGLE_PLAY_COMPLIANCE.md:100` — la app **sí recopila** datos
      (texto de pantalla, imágenes de pantalla, perfil del conductor, historial).
- [ ] Declarar en el formulario de Data Safety:
      - *Screen content / other app content* — texto e imágenes, **procesado en
        dispositivo, no transmitido**.
      - *Personal info* (perfil del conductor: nombre, ciudad, país, vehículo).
      - *App activity* (historial de ofertas evaluadas).
      - No indicar "transmisión" (no hay red), indicar conservación con opción de
        borrado del historial.

### B2 · Política de privacidad
- [ ] Crear una política de privacidad (página o repo) con: datos recopilados,
      procesamiento 100 % local, ausencia de transmisión, conservación/borrado del
      historial, contacto.
- [ ] Enlazarla en Play Console (ficha → Política de privacidad).

### B3 · Prominent disclosure in-app (consentimiento destacado)
- [ ] Añadir pantalla/diálogo en la **activación** de la captura/accesibilidad que
      declare: (1) se usa servicio de accesibilidad + captura de pantalla,
      (2) se lee el texto e imágenes visibles de las ofertas, (3) el análisis es
      local y no sale del dispositivo, (4) no se tocan botones ni se aceptan/rechazan
      viajes.
- [ ] Requerir confirmación explícita (no un simple texto informativo).

### B4 · Seguridad: panel de depuración fuera de release
- [ ] Ocultar la pestaña Debug en release con `BuildConfig.DEBUG &&` (hoy solo se
      oculta si `debugPanelEnabled=false`, y el flag default es `true` —
      `FeatureFlags.kt:27`, `SircApp.kt:63`).
- [ ] Eliminar o restringir la visualización de "Texto OCR" (`DebugPanelScreen.kt:223`)
      y los `Intent.ACTION_SEND` de exportación (`DebugPanelScreen.kt:83,295`) a
      builds debuggables y destinatarios fijos.

### B5 · Seguridad: protección de datos en reposo
- [ ] `android:allowBackup="false"` **o** `android:dataExtractionRules` que excluya
      `sirc.db` (`app/src/main/AndroidManifest.xml:13`).
- [ ] Cifrar la BD con SQLCipher (`DatabaseModule.kt:25-29`).

---

## ALTAMENTE RECOMENDADOS (reducción de riesgo de rechazo)

### R1 · Consolidar los dos Accessibility Services en uno
- [ ] Fusionar `SircAccessibilityService` y `CaptureAccessibilityService` en un
      único servicio (`feature/overlay/src/main/AndroidManifest.xml:11-35`).
- [ ] Eliminar strings duplicados (`accessibility_service_label`,
      `capture_accessibility_service_label` — `strings.xml:3-4`) y arreglar
      `PermissionManager.hasAccessibilityPermission()` para verificar el servicio
      único.

### R2 · Justificación FGS en Play Console
- [ ] Completar el formulario de FGS `specialUse` (overlay) y `mediaProjection`
      (captura) con caso de uso concreto en inglés: el indicador debe permanecer
      visible durante toda la sesión de conducción; la captura procesa ofertas en
      local.
- [ ] Revisar si el prompt de exención de batería (`HomeScreen.kt:126-143`) es
      imprescindible; si no, retirarlo.
- [ ] Declarar el impacto del timeout de sistema del FGS `mediaProjection`
      (3 h Android 14 / 6 h Android 15).

### R3 · Minimización y limpieza de datos
- [ ] Borrar `imageData`/`texts` tras la evaluación (retención en memoria:
      `InMemoryCaptureRepository`, `InMemoryOfferEvaluationRepository`).
- [ ] No capturar `contentDescription` salvo que aporte a la oferta.
- [ ] Valorar eliminar `name`/`city`/vehículo de la BD (no son necesarios para el
      cálculo de rentabilidad).
- [ ] Considerar overlay colapsable / más estrecho (cubre ~82 % de la pantalla) y
      subir `notificationTimeout` (hoy 100 ms).

---

## VERIFICACIÓN TÉCNICA (gate de calidad)

- [ ] `./gradlew testDebugUnitTest` — sin fallos.
- [ ] `./gradlew ktlintCheck` — sin errores de estilo.
- [ ] `./gradlew lintDebug` — sin hallazgos de permisos/accesibilidad sin declarar.
- [ ] Confirmar 0 ocurrencias de `performAction` / `dispatchGesture` en el repo.
- [ ] Confirmar que no existe el permiso `INTERNET` en los manifiestos.
- [ ] Confirmar que no hay secretos en `gradle.properties`, `local.properties` ni
      `res/`.
- [ ] Probar en Android 14 y 15: orden FGS-antes-de-`getMediaProjection()`,
      cancelación del diálogo de captura (sin FGS fantasma), detención al finalizar.
- [ ] Probar con notificaciones denegadas (Android 13+): FGS sigue operativo.
- [ ] Verificar comportamiento en Doze/Battery Optimization con y sin exención.

---

## PLAY CONSOLE (ficha)

- [ ] Formulario de **Accessibility Service**: declarar propósito, uso solo lectura,
      adjuntar **video demo** mostrando que no se automatiza interacción.
- [ ] Declarar uso de **MediaProjection** y que el contenido se procesa en local.
- [ ] Declarar los **FGS** con sus tipos y justificación.
- [ ] **Data Safety** (B1) y **política de privacidad** (B2) enlazada.
- [ ] Clasificación de contenido (IARC) y objetivo de edad coherentes con datos de
      terceros en pantalla.
- [ ] `targetSdk = 35` (Android 15) y versión mínima razonable (`minSdk = 24`).
- [ ] Nombre, descripción y capturas sin afirmaciones engañosas sobre las apps de
      transporte (no sugerir automatización ni violar TOS de terceros).

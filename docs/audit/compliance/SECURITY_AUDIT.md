# SECURITY_AUDIT — SIRC (`com.sirc.app`)

**Rol:** Security Engineer (OWASP Mobile)
**Fecha:** 2026-08-01
**Alcance:** Módulos `:app`, `:feature:overlay`, `:core:capture:android`, `:data`, manifiestos y configuración Gradle.
**Modo:** Solo lectura. No se modificó código.

---

## Resumen ejecutivo

La aplicación tiene una **base de seguridad sólida**: es 100 % offline (no declara
`android.permission.INTERNET`), no contiene secretos embebidos, no usa red ni
WebView, sus componentes exportados son mínimos y correctos, y el logger está
correctamente restringido por builds debug. Los riesgos se concentran en **tres
hallazgos ALTOS** relacionados con protección de datos en reposo y exposición de
contenido capturado, más dos MEDIOS de minimización de datos.

| Severidad | Cantidad |
|---|---|
| CRITICO | 0 |
| ALTO | 3 |
| MEDIO | 2 |
| BAJO | 0 |
| INFO | 6 |

---

## HALLAZGOS

### S-1 · ALTO — Base de datos Room sin cifrar con PII del conductor

| | |
|---|---|
| **Ubicación** | `data/src/main/kotlin/com/sirc/data/local/SircDatabase.kt:12-21` · `data/src/main/kotlin/com/sirc/data/di/DatabaseModule.kt:25-29` |
| **CWE** | CWE-311 / CWE-312 (Falta de cifrado de datos sensibles) |

- `Room.databaseBuilder(context, SircDatabase::class.java, "sirc.db")` sin SQLCipher
  ni ningún cifrado (`DatabaseModule.kt:25-29`).
- Datos personales persistidos:
  - `data/src/main/kotlin/com/sirc/data/local/entity/DriverConfigEntity.kt:20-23` —
    `name`, `country`, `city`, `currency` (PII directa).
  - `.../entity/DriverConfigEntity.kt:25-31` — vehículo (`brand`, `model`, `year`, `fuelType`).
  - `.../entity/OfferHistoryEntity.kt:9-25` — historial de ofertas con montos,
    distancias, ganancias, decisiones y motivos (datos económicos y de movilidad).
- Protección actual: solo el sandbox de la app
  (`/data/data/com.sirc.app/databases/sirc.db`).

**Riesgo:** en dispositivo rooteado o build debuggable, o mediante backup (ver S-2),
la BD es legible en claro.

**Recomendación:**
1. Migrar a SQLCipher (`net.zetetic:android-database-sqlcipher` + `SupportOpenHelperFactory`).
2. Reducir al mínimo los datos: `name`/`city`/vehículo no son necesarios para el
   cálculo de rentabilidad; considerar eliminarlos.
3. Como mínimo, proteger la BD con `dataExtractionRules` y desactivar backup.

---

### S-2 · ALTO — `allowBackup="true"` sin `dataExtractionRules` ni `fullBackupContent`

| | |
|---|---|
| **Ubicación** | `app/src/main/AndroidManifest.xml:13` |

- `android:allowBackup="true"` en el `application`.
- No existe `android:dataExtractionRules` ni `android:fullBackupContent` en ningún
  manifiesto.
- En Android 12+ el auto-backup por defecto incluye `databases/sirc.db`, que contiene
  PII del conductor (ver S-1).

**Riesgo:** extracción del historial completo de ofertas y perfil del conductor vía
`adb backup` o copia de seguridad en la nube.

**Recomendación:**
1. `android:allowBackup="false"`, **o**
2. `android:dataExtractionRules="@xml/data_extraction_rules"` excluyendo `sirc.db`
   (y cualquier `sharedpref` futuro) del backup de dispositivo y de la nube,
   **y** cifrar la BD con SQLCipher.

---

### S-3 · ALTO — Panel de depuración con texto OCR capturado, disponible en release

| | |
|---|---|
| **Ubicación** | `core/capture/src/main/kotlin/com/sirc/capture/flag/FeatureFlags.kt:27` · `app/src/main/kotlin/com/sirc/app/SircApp.kt:63` · `app/src/main/kotlin/com/sirc/app/DebugPanelScreen.kt:223,455` |

- `InMemoryFeatureFlags.isEnabled(flag)` devuelve `true` por defecto para **todos**
  los flags (`FeatureFlags.kt:27`), incluido `FeatureFlag.DEBUG_PANEL`.
- La pestaña "Debug" solo se oculta si `debugPanelEnabled == false` (`SircApp.kt:63`);
  no hay gating por `BuildConfig.DEBUG` ni `ApplicationInfo.FLAG_DEBUGGABLE`.
- La pestaña muestra el **"Texto OCR"** capturado de la pantalla de la app de
  transporte (`DebugPanelScreen.kt:223`, formateado en `:455`), poblado en
  `feature/overlay/.../PipelineOverlayDataSource.kt:178-179`.
- No existe variante/manifiesto debug (`app/src/debug`), por lo que el panel está
  disponible en builds de release.

**Riesgo:** exposición del contenido crudo de pantallas de otras apps (que puede
incluir datos de pasajeros) a cualquier usuario de la versión publicada.

**Recomendación:**
1. Ocultar la pestaña Debug en release: `if (BuildConfig.DEBUG && ...)`.
2. Eliminar la visualización de `ocrText`/`parserResult` incluso en debug, o
   restringirla a builds debuggables.
3. Los "Exportar diagnóstico/reporte" usan `Intent.ACTION_SEND` sin restringir
   destinatario (`DebugPanelScreen.kt:83,295`); restringir a un set de paquetes y
   usar `FLAG_IMMUTABLE`.

---

### S-4 · MEDIO — Captura de pantalla íntegra retenida en memoria

| | |
|---|---|
| **Ubicación** | `core/capture/android/src/main/kotlin/com/sirc/capture/android/provider/MediaProjectionScreenCaptureProvider.kt:145-177` · `core/capture/src/main/kotlin/com/sirc/capture/model/ScreenFrame.kt:12` |

- `ImageReader.newInstance(width, height, RGBA_8888)` + `createVirtualDisplay(...)`
  captura el **100 % de la pantalla**, no solo la región de la app de transporte.
- El bitmap/`ByteArray` vive en memoria (`ScreenFrame.imageData`) y los textos
  resultantes se retienen en repositorios en memoria:
  `InMemoryCaptureRepository` (50 snapshots) y `InMemoryOfferEvaluationRepository`
  (100 registros con `ocrText` y `parserResult`).
- No se persiste la imagen en disco, pero el contenido capturado (que puede incluir
  direcciones, nombres y teléfonos de pasajeros) queda retenido mientras dure el
  proceso y es visible en el panel de depuración (S-3).

**Recomendación:**
1. Limpiar `imageData` inmediatamente tras el OCR (no retener el bitmap).
2. Minimizar retención de `texts` tras la evaluación.
3. Cuando sea viable, limitar la proyección a la resolución/región relevante.
4. Documentar el alcance de la captura en la ficha de Play.

---

### S-5 · MEDIO — PII del conductor persistida y contenido de apps de terceros en memoria

| | |
|---|---|
| **Ubicación** | `feature/overlay/src/main/kotlin/com/sirc/feature/overlay/SircAccessibilityService.kt:41-58,76-81` · `.../CaptureAccessibilityService.kt:54-69,86-91` · `data/src/main/kotlin/com/sirc/data/local/entity/DriverConfigEntity.kt:20-31` |

- Dos Accessibility Services con `canRetrieveWindowContent="true"` y
  `flagReportViewIds="true"` (`feature/overlay/src/main/res/xml/accessibility_service_config.xml:5,8`) leen `node.text` y `node.contentDescription` de **toda** la ventana de la app de transporte (límites: `MAX_NODES=400`, `MAX_TEXTS=80`, `MAX_TEXT_LENGTH=200`).
- PII persistida en Room: nombre, ciudad, país, vehículo y costos (ver S-1).
- Mitigaciones presentes: sin INTERNET, OCR on-device, textos acotados, filtrado por
  paquete (`accessibility_service_config.xml:11`), overlay solo muestra métricas
  calculadas (nunca texto crudo).

**Recomendación:**
1. No capturar `contentDescription` salvo que aporte a la oferta (minimización).
2. Borrar `texts`/`imageData` tras la evaluación.
3. Documentar el consentimiento del usuario para estos datos (ver informe de Play).

---

## ÁREAS CON RESULTADO LIMPIO (INFO)

| Área | Hallazgo | Ubicación |
|---|---|---|
| **Logs** | Logger centralizado; `debug`/`info` solo en builds debuggables; sin contenido sensible en logcat. No hay `println`/`System.out`/`Timber`. | `feature/overlay/.../AndroidSircLogger.kt:27-54` |
| **Secrets** | Sin API keys, tokens ni credenciales embebidas. `gradle.properties` y `local.properties` limpios. Sin `google-services.json`. | — |
| **Preferences** | No se usa `SharedPreferences`/`DataStore`; toda la persistencia es vía Room. | — |
| **Exported components** | Único activity exportado es LAUNCHER (`MainActivity`). Todos los servicios `exported="false"` con `BIND_ACCESSIBILITY_SERVICE`. Sin providers, receivers ni deep links. | `app/src/main/AndroidManifest.xml:20-30` |
| **PendingIntent** | No se usa `PendingIntent` (riesgo de `FLAG_IMMUTABLE` inexistente). | — |
| **Red/Criptografía** | Sin `INTERNET`, sin HTTP/OkHttp/Retrofit, sin WebView, sin cleartext, sin `network_security_config` necesario. App 100 % offline; única dependencia cloud es ML Kit on-device. | `gradle/libs.versions.toml:62` |
| **Permisos** | Solo los estrictamente necesarios: `SYSTEM_ALERT_WINDOW`, `FOREGROUND_SERVICE*`, `POST_NOTIFICATIONS`, `BIND_ACCESSIBILITY_SERVICE`. Sin ubicación, contactos, cámara ni almacenamiento. | manifiestos |

---

## Prioridad de remediación

1. **S-3** — Desactivar panel de depuración en release (exposición de datos reales al usuario).
2. **S-2** — Endurecer backup (`allowBackup=false` o `dataExtractionRules`).
3. **S-1** — Cifrar la BD con SQLCipher y minimizar PII.
4. **S-5 / S-4** — Limpiar retención en memoria y minimizar captura.

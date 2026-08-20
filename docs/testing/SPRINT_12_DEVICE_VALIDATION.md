# SIRC — Sprint 12 / E1a: Validación real del núcleo (SPRINT_12_DEVICE_VALIDATION)

> Sprint **PARCIALMENTE DESBLOQUEADO** el 16-ago-2026: llegó 1 dispositivo
> físico (DEVICE-01) y se ejecutó la primera prueba real de OCR sobre 5
> capturas (Uber ×3, InDriver ×2) mediante un receptor **debug-only** que
> injrecta imágenes reales al pipeline de producción (§6.2). El resto de la
> matriz sigue **PENDING / SIN EVIDENCIA**; no se inventan métricas.
>
> **ACTUALIZADO el 18-ago-2026 (estado actual)**: los hallazgos DVC-01…04 y
> K1 fueron CORREGIDOS en WP-12-FIX-01…05 y verificados en DEVICE-01 (E2E
> real con InDrive Ecuador). Las secciones §1–§14 documentan el estado
> histórico de 16-ago (pre-correcciones) y se conservan íntegras como
> evidencia del diagnóstico. Ver §15 para el estado real consolidado.

## 1. Objetivo

Validar en dispositivos Android reales el núcleo SIRC (instalación, captura,
OCR, detección/parsing multi-plataforma, evaluación, overlay, ciclo de vida,
estabilidad, batería, privacidad) **antes** de implementar monetización
(Supabase, autenticación, Trial Premium, Play Billing, entitlement, Play
Integrity, nuevas funciones Premium, ampliación de plataformas).

Partimos EXACTAMENTE del estado actual del repositorio (baseline §2). Ninguna
decisión previa se modifica; no se reconstruye Sprint 11; no se repiten
WP-E1/E2/E3; no se implementa monetización.

**Métrica objetivo**: decisión visible **<1 s** (objetivo UX) / **<3 s** (límite
técnico E2E).

## 2. Baseline (verificado el 16-ago-2026)

| Ítem | Valor |
|---|---|
| HEAD | `75cbac12e5d414172f1208d4764b554e3c8fc25a` |
| Branch | `main` |
| `origin/main` vs HEAD | **Idénticos** (tras `git fetch origin`) |
| Working tree | **Limpio** |
| Último commit | `75cbac1` — docs(loop): modelo comercial TRIAL 14 días + suscripción (D16) |

### 2.1 Verificación local del baseline (evidencia separada, NO sustituye lo físico)

Ejecutada el 16-ago-2026 en el mismo working tree, sin modificar código de
producción (solo build/):

| Comando | Resultado |
|---|---|
| `.\gradlew.bat ktlintCheck --console=plain` | **BUILD SUCCESSFUL** (13 s; 73 tasks) |
| `.\gradlew.bat lintDebug assembleDebug testDebugUnitTest :domain:test :core:platform:test :core:capture:test :feature:overlay:testDebugUnitTest --console=plain` | **BUILD SUCCESSFUL** (45 s; 428 tasks) |
| APK generado | `app/build/outputs/apk/debug/app-debug.apk` (≈62,7 MB) |
| `git status --short` tras build | Vacío (working tree limpio) |

> La verificación de build/unitarios está **en verde**, pero NO constituye
> evidencia física. Es la confirmación de que el baseline es compilable y que
> el APK está listo para instalación cuando exista hardware.

## 3. Dispositivos

Requisito del sprint: **mínimo 2 dispositivos físicos**; ideal ≥3 repartidos en
Android 10–11 / 12–13 / 14–15.

### 3.1 Estado del hardware

| Fecha | Evento |
|---|---|
| 16-ago-2026 | `adb devices -l` → **DEVICE-01 conectado (Infinix X6850, Android 15 / API 35)** |
| 16-ago-2026 | AVD disponibles: `Pixel_7_API_35` (emulador, API 35) — **no sustituye** lo físico |

### 3.2 Registro de dispositivos

| Campo | Dispositivo 1 | Dispositivo 2 | Dispositivo 3 |
|---|---|---|---|
| Fabricante | Infinix | — | — |
| Modelo | X6850 | — | — |
| Android | 15 | — | — |
| API level | 35 | — | — |
| RAM | (por confirmar) | — | — |
| Resolución | (por confirmar) | — | — |
| Versión SIRC | v1.0.0-rc1 + `assembleDebug` (16-ago-2026) | — | — |
| Método de instalación | `adb install -r app-debug.apk` | — | — |
| Fecha de prueba | 16-ago-2026 (OCR) | — | — |

### 3.3 Emulador (evidencia local separada, si se decide ejecutarla)

El AVD `Pixel_7_API_35` (Android 15 / API 35) puede usarse para ejercicios
locales (instalación, ciclo de vida, latencias del pipeline en artificial),
**etiquetados siempre como evidencia de emulador**. NUNCA cuenta como
dispositivo real para los criterios de éxito P0/P1/P2 del sprint.

## 4. Versiones

- **SIRC**: v1.0.0-rc1 (Sprint 11) — es el estado de partida del núcleo.
- **Target**: Android 10–15 (API 29–35); minSdk 24; targetSdk 35.
- No hay versión beta de campo publicada todavía (beta cerrada = E1a).

## 5. Metodología

1. Auditoría pre-validación (esta primera pasada), sin modificar código.
2. Identificación de hardware físico (pendiente — usuario).
3. Instalación limpia + actualización con `adb install` desde
   `app-debug.apk` (o track de Play Console cuando exista).
4. Concesión de permisos: accesibilidad (`CaptureAccessibilityService`),
   overlay (`SYSTEM_ALERT_WINDOW`), captura de pantalla (MediaProjection).
5. Onboarding completo (6 pasos: perfil, vehículo, costos, plataformas,
   objetivos) para activar `DriverConfig`.
6. Registro de evidencia por prueba (screenshots, logs vía
   `adb logcat`, exportar diagnóstico, informe de validación del
   `ValidationRecorder`).
7. Métricas del Panel de depuración: sección **Rendimiento (promedio últimas
   20 ofertas)** + **Modo validación** (contadores + exportar informe).
8. Clasificación: PASS / FAIL / BLOCKED / PENDING / INSUFFICIENT_EVIDENCE.
9. Anonimización estricta de capturas (sin nombres, teléfonos, direcciones,
   tokens, cuentas, datos financieros).
10. No corregir problemas en esta primera pasada salvo defecto que bloquee la
    propia validación (cambio mínimo documentado).

## 6. Matriz de pruebas (estado 16-ago-2026)

Estado general: **PARCIAL** — OCR/detección/parsing reales en DEVICE‑01 (§6.2);
4 hallazgos de validación manual (DVC‑01…DVC‑04, §6.4); resto PENDING.

| # | Área | Prueba | Resultado esperado | Estado |
|---|---|---|---|---|
| INST‑1 | Instalación | Instalación limpia en dispositivo real | Instala y abre sin error | **PASS** (instalado vía `adb install -r`, abre) — DEVICE‑01 |
| INST‑2 | Instalación | Actualización desde versión anterior | No pierde datos ni crashea | PENDING — SIN EVIDENCIA |
| INST‑3 | Instalación | Primera ejecución + onboarding (6 pasos) | `showDecision`/`costPerKm` configurados y reflejados | **PARCIAL** — onboarding completado; **config post-onboarding NO editable** (DVC‑01) |
| INST‑4 | Instalación | Cierre y reapertura | Estado UI persistente | PENDING — SIN EVIDENCIA |
| CAP‑1 | Captura | Textos por accesibilidad (`CaptureAccessibilityService`) | Request con origin `ACCESSIBILITY` | **PARCIAL** — servicio habilitado y bindeado (logcat); **sin evidencia de request real** (silencio total de logs; DVC‑04) |
| CAP‑2 | Captura | CaptureInput → CaptureRequest → dedup | Sin duplicados de snapshot | PENDING — SIN EVIDENCIA |
| CAP‑3 | Captura | Debounce (400 ms) | Requests coalesced | PENDING — SIN EVIDENCIA |
| CAP‑4 | Captura | MediaProjection cuando corresponde | `imageData` presente; degrada a textos si falla | **FAIL/INSUFFICIENT_EVIDENCE** — appop `PROJECT_MEDIA = ignore` (rejectTime), `dumpsys media_projection` vacío; sin frames (DVC‑02/DVC‑04) |
| CAP‑5 | Captura | Recuperación tras error de captura | Sin crash; mensaje claro | PENDING — SIN EVIDENCIA |
| OCR‑1 | OCR (P0) | Dataset real por plataforma | `OCR_FIELD_ACCURACY` y `OFFER_PARSE_ACCURACY` calculados | **PARCIAL (ver §6.2)** — OCR lee bien montos; parser falla monto (0/3); 5 capturas reales (2 InDriver + 3 Uber) |
| OCR‑2 | OCR (P0) | Monto / distancia / duración extraídos | Campos correctos | **FAIL (monto) / PARCIAL (dist, dur)** — ver §6.2 |
| OCR‑3 | OCR (P0) | `test-images/` NO como única fuente | Dataset real de validación | **PASS** — imágenes reales de DEVICE-01 (§6.2) |
| PLT‑1 | Uber | Flujo completo (detección + parse + overlay + decisión) | E2E correcto | PENDING — SIN DISPOSITIVO/CUENTA/OFERTAS REALES |
| PLT‑2 | DiDi | Captura + detección + parsing | Correcto | PENDING — SIN DISPOSITIVO/CUENTA/OFERTAS REALES |
| PLT‑3 | Cabify | Captura + detección + parsing | Correcto | PENDING — SIN DISPOSITIVO/CUENTA/OFERTAS REALES |
| PLT‑4 | InDrive | Captura + detección + parsing | Correcto | **PARCIAL** — 2 capturas reales parseadas (§6.2); sin overlay (pendiente) |
| PLT‑5 | Keywords | Hallazgo previo: keywords ambiguas | Verificar AMBIGUOUS→GENERIC en pantallas reales; registrar, NO corregir | **CORREGIDO (G2, 20-ago)** — la identidad por keywords usa solo identificadores fuertes de marca (`platformKeywords`); sin paquete, la marca única resuelve (UBER/DIDI/CABIFY/INDRIVE) y las palabras genéricas quedan AMBIGUOUS→UNSUPPORTED (no inventan plataforma). Con paquete real sigue PACKAGE_MATCH (§10-K2, matriz G2 en `PlatformDetectionEngineTest`) |
| VEL‑1 | Rendimiento | Latencias por etapa (captura/OCR/detección/parse/eval/overlay/total) | min/max/avg (+p95 si muestra ≥ suficiente) | **PARCIAL** — reales en inyección de imágenes (§6.2); sin overlay/UI |
| VEL‑2 | Rendimiento | Decisión visible <1 s (UX) | Registrado con cronómetro en ruta | PENDING — SIN EVIDENCIA |
| VEL‑3 | Rendimiento | E2E <3 s (límite técnico) | Registrado en dispositivo | PENDING — SIN EVIDENCIA |
| EST‑1 | Estabilidad | Sesión corta 30 min | Sin crash/ANR | PENDING — SIN EVIDENCIA |
| EST‑2 | Estabilidad | Sesión prolongada 8 h | Sin crash/ANR/cierre de servicios | PENDING — SIN EVIDENCIA |
| CVD‑1 | Ciclo de vida | abrir/cerrar, background/foreground | Overlay y captura sobreviven | PENDING — SIN EVIDENCIA |
| CVD‑2 | Ciclo de vida | bloqueo/desbloqueo, rotación, split screen | Overlay se reclampa; virtual display se recrea | PENDING — SIN EVIDENCIA |
| CVD‑3 | Ciclo de vida | matar proceso → reabrir | Servicios se reinician (`STICKY`) | PENDING — SIN EVIDENCIA |
| CVD‑4 | Ciclo de vida | reiniciar dispositivo, revocar/conceder permisos, detener MediaProjection, reinstalar | Recuperación correcta | PENDING — SIN EVIDENCIA |
| BAT‑1 | Batería/memoria | Batería inicial/final, duración, condiciones (pantalla, brillo, uso, plataformas) | Consumo real medido; detectar anomalías | PENDING — SIN EVIDENCIA |
| BAT‑2 | Batería/memoria | Memoria aproximada del Panel de depuración | Registrado | PENDING — SIN EVIDENCIA |
| OVL‑1 | Overlay | Aparece/desaparece/actualización/arrastre/TTL | Correcto | **FAIL** — no aparece en DEVICE‑01 (DVC‑03); en emulador sí |
| OVL‑2 | Overlay | Semáforo, ganancia, $/km, $/hora, confianza | Valores coherentes | PENDING — SIN EVIDENCIA (requiere OVL‑1) |
| OVL‑3 | Overlay | `FLAG_NOT_TOUCHABLE`; no interfiere con la app | Toques pasan a la plataforma | PENDING — SIN EVIDENCIA |
| OVL‑4 | Overlay | Comportamiento oculto, rotación, bloqueo/desbloqueo | Correcto | PENDING — SIN EVIDENCIA |
| DEC‑1 | Decisión | Oferta claramente rentable → ACEPTAR | Recomendación correcta | PENDING — SIN EVIDENCIA |
| DEC‑2 | Decisión | Oferta no rentable → RECHAZAR | Recomendación correcta | PENDING — SIN EVIDENCIA |
| DEC‑3 | Decisión | Oferta ambigua → REVISAR | Recomendación correcta | PENDING — SIN EVIDENCIA |
| DEC‑4 | Decisión | Coherencia métricas/costos/confianza | Sin editar el motor; localizar la causa (captura/OCR/parse/config/eval) | PENDIENTE DE PRUEBA REAL |
| CFG‑1 | Configuración | Onboarding afecta vehículo/combustible/mantenimiento/costos/objetivos/plataformas | `costPerKm` y `showDecision` comprobados en dispositivo | **PARCIAL** — ver DVC‑01: plataformas/vehículo/país/costos básicos NO reeditables post-onboarding |
| ERR‑1 | Errores | OCR fallido, texto incompleto, sin distancia/duración | Sin crash; mensaje apropiado | PENDING — SIN EVIDENCIA |
| ERR‑2 | Errores | Plataforma desconocida, permiso revocado, captura interrumpida, servicio detenido, proceso muerto | Recuperación; sin datos corruptos | PENDING — SIN EVIDENCIA |
| PRV‑1 | Privacidad | Pipeline de oferta sin backend/Supabase/internet/Realtime | 100 % local (auditoría de tráfico en dispositivo) | PENDING — SIN EVIDENCIA (ver §9 / nota estática) |
| SEC‑1 | Seguridad | Sin secretos/API keys/credenciales en APK ni logs | Auditable | Ver §9 (auditoría estática inicial) |
| EV‑1 | Evidencia | Por prueba: ID, dispositivo, fecha, versión, pasos, resultado, evidencia, observaciones | A completar en campo | PENDING |

### 6.1 Muestra

Requisito: mínimo 20 ofertas reales por plataforma (20+ Uber / DiDi / Cabify /
InDrive). Sin dispositivos: **MUESTRA INSUFICIENTE** — no se extrapola nada.

### 6.2 Prueba OCR real (16-ago-2026, DEVICE-01)

Ejecutada vía receptor **debug-only** (`DebugImageOcrReceiver`, source set
`app/src/debug`) que inyecta imágenes reales al pipeline de producción. NO
existe en Release. Ver `app/build.gradle.kts`, `app/src/debug/AndroidManifest.xml`,
`app/src/debug/kotlin/com/sirc/app/debug/DebugImageOcrReceiver.kt` y
`app/src/debug/assets/sirc_test/` (5 JPG).

- Capturas reales tomadas en DEVICE-01 (Infinix X6850, Android 15): Uber ×3
  (`uber_1..3.jpg`) e InDriver ×2 (`indriver_1..2.jpg`), montos USD (Quito).
- Disparo: `adb shell am broadcast -a com.sirc.debug.OCR_TEST`.
- Resultado por imagen (etapas y campos parseados; evidencia:
  `docs/testing/evidence/SIRC_OCR_TEST_logcat_dump.txt`):

| Imagen | ocr(ext) | pipelineOcr | detection | parse | total | detector | pantalla | snapshot | platform | total | dist | dur |
|---|---|---|---|---|---|---|---|---|---|---|---|---|
| indriver_1.jpg | 482.4 ms | 182.1 ms | 36.7 ms | 10.5 ms | 241.6 ms | PACKAGE_MATCH | REQUEST | OK | INDRIVE | 479.0 (mal, $4,50) | 16.4 km ✓ | 13.0 min ✓ |
| indriver_2.jpg | 456.2 ms | 248.3 ms | 51.8 ms | 11.5 ms | 317.4 ms | PACKAGE_MATCH | REQUEST | OK | INDRIVE | 5.0 (mal, $4,50) | 0.0 (faltó 28,7) | 42.0 min ✓ |
| uber_1.jpg | 237.7 ms | 171.7 ms | — | 0.0 ms | 198.6 ms | PACKAGE_MATCH | UNKNOWN | NULL (PANTALLA_NO_REQUEST) | — | — | — | — |
| uber_2.jpg | 171.6 ms | 176.9 ms | 12.5 ms | 7.8 ms | 198.1 ms | PACKAGE_MATCH | REQUEST | OK | UBER | 90.0 (mal, $25.53) | 99.0 km ✓ | 128.0 min ✓ |
| uber_3.jpg | 195.7 ms | 155.0 ms | — | 0.0 ms | 175.5 ms | PACKAGE_MATCH | HOME | NULL (PANTALLA_NO_REQUEST) | — | — | — | — |

Observaciones verificables del volcado (`SIRC-OCR-TEST`, líneas OCR reales):

- **OCR lee bien los montos** (los textos del dataset lo confirman sin visión):
  indriver_1: `Aceptar por $4,50`; indriver_2: `4.7 (21) $4.50, Efectivo` y
  `Aceptar por USD4.5`; uber_2: `$25.53`. El OCR NO es el problema del monto.
- **El parser falla el monto 3/3** ofertas reales: 479.0 (street number
  "479 A 1 Pa." capturado), 5.0 (tomó "USD5" en vez de USD4.5), 90.0 (tomó
  "$090 incluido" en vez de $25.53). Hallazgo K1 (§10) — registrar, NO corregir
  en esta pasada (§5.10).
- **dist 2/3 y dur 3/3 correctas** cuando la cadena está presente; indriver_2
  distancia 0.0 (el texto OCR dice `28,7 kn`, "kn" en vez de "km" → no se
  matchea distancia; OCR de texto, no extracción del parser).
- **Detección**: con `packageName` real → `PACKAGE_MATCH` correcto en 5/5
  (2 INDRIVE, 3 UBER). Sin `packageName` → `AMBIGUOUS` (incidencia K2, §10).
- **Pantallas no-oferta**: uber_1 (UNKNOWN, sin "Aceptar") y uber_3 (HOME,
  `Viaje disponible` no oferta) → cosa correcta: snapshot NULL, no se fuerza.
- **Latencias**: pipeline total (OCR + detección + parse) **175–317 ms** en el
  propio dispositivo; el `ocr(ext)`, medición del receiver re-OCR de la imagen,
  es adicional (155–482 ms); no cuenta en el total de pipeline. Sigue sin
  medirse nada del overlay/UI (PENDIENTE).

**Conclusión parcial del test**: hay **evidencia física real** de que el
pipeline ejecuta OCR en dispositivo, detecta por paquete y rechaza pantallas
no-oferta; latencias por debajo de 1 s (objetivo UX). El defecto a registrar es
la **extracción del monto** (K1). Sample 5 capturas → insuficiente para
`OCR_FIELD_ACCURACY`/`OFFER_PARSE_ACCURACY` declarables (requisito §6.1).

**Impacto en producción**: NINGUNO. Solo `app/build.gradle.kts`
(+`implementation(project(":core:platform"))`, requerida y justificada §K1-build)
y contenido del source set `debug` (nunca en Release). Código de producción en
`main` sin cambios (`git diff` solo muestra el build file y archivos nuevos).

### 6.3 Evaluación: ¿se mantiene o se elimina el mecanismo debug?

Análisis (para decisión del usuario; no se elimina automáticamente):

**Mantener (recomendado)**:
- Es **debug-only** (source set `debug`): nunca entra en Release ni en Play.
  No añade permisos, no abre rutas de galería, no sube datos.
- Es reutilizable: permite re-ejecutar OCR/detección/parsing en DEVICE‑01 con
  más capturas reales sin reconstruir el flujo (basta añadir JPG a
  `app/src/debug/assets/sirc_test/`). Útil para alcanzar la muestra §6.1 y para
  verificar la corrección de K1 tras depurar el parser.
- Costo de mantenimiento bajo (un receptor + manifest + assets).

**Eliminar**:
- Solo recomendable cuando el resto de la matriz §5 se ejecute por el flujo
  real (accesibilidad/MediaProjection) y la corrección de K1 esté validada, o
  si el usuario prefiere no conservar código de prueba en el repo.

**Decisión pendiente** del usuario. Ver `TASK.md`.

### 6.4 Hallazgos manuales DEVICE-01 (DVC-01…DVC-04, 16-ago-2026)

Validación manual en DEVICE-01 (Infinix X6850, Android 15 / API 35). Registro de
evidencia — **NO corregir todavía**. Diagnóstico desde código (cita exacta) +
evidencia de dispositivo (`docs/testing/evidence/DVC_diagnostics_logcat_dump.txt`).

---

#### DVC-01 — CONFIGURACIÓN INICIAL NO EDITABLE tras el onboarding

- **Descripción**: país, ciudad, vehículo (nombre/marca/modelo/año/combustible/
  consumo), precio combustible, mantenimiento/km, otros costos y plataformas
  seleccionadas solo se capturan una vez en el onboarding; no existe pantalla
  posterior para reeditarlos.
- **Pasos para reproducir**: completar onboarding (6 pasos) → abrir Ajustes →
  buscar edición de país/ciudad/vehículo/costos básicos/plataformas.
- **Resultado observado**: Settings solo expone moneda, `costPerKm`,
  `costPerMinute`, `costPerTrip`, umbrales (`minProfitPerKm`, `minProfitPerHour`)
  y config de overlay. **NO editables**: país, ciudad, nombre, vehículo completo,
  `fuelPrice`, `maintenanceCostPerKm`, `additionalCosts`, `platforms`.
- **Resultado esperado**: el conductor debe poder actualizar vehículo, ciudad,
  costos y plataformas cuando cambien (la evaluación usa esa config).
- **Impacto**: la rentabilidad puede basarse en datos obsoletos (cambió de
  vehículo/ciudad/costos/plataformas) sin forma de actualizarlos → decisiones
  incorrectas.
- **Severidad**: **Alta** (afecta corrección de la decisión; bloquea caso de uso
  real post-onboarding).
- **Evidencia / causa (código)**: `feature/onboarding/.../OnboardingSteps.kt:45-48`
  (país), `:49-53` (ciudad), `:63-99` (vehículo), `:111-139` (costos), `:145-167`
  (plataformas) — solo en onboarding. `feature/settings/.../SettingsScreen.kt:45-81`
  solo edita moneda/costos/umbrales; `SettingsViewModel.kt:61-63`. No hay
  re-entrada: `SircRoot.kt:12,31` muestra onboarding solo si `isConfigured == false`
  y `DefaultDriverConfigRepository.kt:19` usa `config != null` como flag (no
  existe botón de reset/reconfig). `DriverConfigRepository.save()` es upsert
  completo (no por campo).
- **Estado**: **FAIL** (no cumple el caso de uso de actualización).

---

#### DVC-02 — CONFIGURACIÓN DE CAPTURA DE PANTALLA (MediaProjection)

- **Descripción**: al activar el permiso de captura, el diálogo del sistema
  ofrece "pantalla completa" vs "una app"; la selección inicial quedó en una
  sola aplicación y SIRC no permite cambiarla posteriormente.
- **Pasos para reproducir**: Home → "Permitir captura de pantalla" → elegir
  "una aplicación" → comprobar después que no hay forma de cambiar la fuente.
- **Resultado observado**: (1) Android ofrece ambas opciones; (2) **SIRC no
  guarda nada** de captura (sin DataStore/SharedPreferences; Room solo tiene
  overlay/driver/history); (3) **no puede cambiar la fuente** salvo detener y
  volver a consentir; (4) la selección **no persiste** (cada sesión re-consiente);
  (5) al cambiarla solo se re-lanza el diálogo del sistema; (6) al revocar →
  `stopProjection()` + parada de FGS (`MediaProjectionScreenCaptureProvider.kt:101-116,142-148`);
  (7) al conceder de nuevo → nuevo diálogo del sistema.
- **Resultado esperado**: definir cómo SIRC debe manejar single-app vs
  full-screen (producto) y persistir/seleccionar la fuente adecuada.
- **Impacto**: si se elige "una app" que no es la plataforma, no hay frames; la
  selección depende 100 % del diálogo del sistema y no es controlable por SIRC.
- **Severidad**: **Alta** (configuración de captura no controlable/persistente;
  relacionada con DVC‑04).
- **Evidencia / causa (código)**: `OverlayManager.kt:68-69` usa
  `createScreenCaptureIntent()` sin `MediaProjectionConfig` (no hay
  `setSingleApp`/extras); `MediaProjectionScreenCaptureProvider.kt:61-70`
  valida RESULT_OK; `MediaProjectionService.kt:60-67,119-121` pasa token al FGS
  en memoria (no persistido). **Dispositivo**: `appops PROJECT_MEDIA = ignore;
  rejectTime` y `dumpsys media_projection` vacío (sin proyección activa).
- **Estado**: **FAIL / INSUFFICIENT_EVIDENCE** (comportamiento registrado; el
  diseño de producto está por definir).

---

#### DVC-03 — OVERLAY NO APARECE en DEVICE-01 (funciona en emulador) [P0]

- **Descripción**: al activar manualmente el overlay en el físico no aparece
  ninguna ventana/información; en el simulador/emulador sí se mostró.
- **Pasos para reproducir**: DEVICE‑01 → Home → "Iniciar overlay" → esperar;
  no aparece. Repetir en AVD → aparece.
- **Resultado observado (dispositivo)**: (a) `SYSTEM_ALERT_WINDOW = allow`
  concedido; (b) **no hay `OverlayService` corriendo** (solo
  `CaptureAccessibilityService` en `dumpsys activity services`); (c) **no hay
  ventana de overlay** en `dumpsys window` (solo MainActivity); (d) sin logs de
  `OverlayService`/`PipelineOverlay`/`CapturePipeline` en logcat.
- **Resultado esperado**: el overlay debe aparecer sobre las apps cuando hay
  oferta procesada.
- **Impacto**: **P0** — el overlay es el elemento central del producto; sin él
  no hay decisión visible.
- **Evidencia / causa probable (código)**: la visibilidad depende de
  `visibleFor(status, evaluation)` (`PipelineOverlayDataSource.kt:113,256-259`):
  con pipeline en `DISABLED` y sin `evaluation`, `visible=false` y
  `OverlayContent.kt:59` no compone nada (ventana transparente). El pipeline
  solo sale de `DISABLED` con un `CaptureRequest` (`DefaultCapturePipeline.kt:51`);
  si no llega ninguno (sin accesibilidad real / sin oferta de las 4 apps), el
  overlay nunca muestra nada. Además `OverlayService.kt:202` traga excepciones de
  `addView` en `runCatching` sin log; `OverlayController.kt:28` marca
  `_isRunning=true` optimista (la UI puede decir "En ejecución" sin ventana);
  `OverlayService.kt:117-120` hace `stopSelf()` silencioso sin permiso;
  fabricantes (XOS) pueden matar el FGS. En emulador funcionó porque sí llegaron
  requests (debug receiver/eventos). No existe rama "simulador" en código.
- **Estado**: **FAIL** (P0 potencial) — causa a confirmar con logs del servicio
  en campo (no corregir aún).

---

#### DVC-04 — CAPTURAS NO LEÍDAS EN EL FLUJO NORMAL (solo Escenario A demostrado)

- **Descripción**: separar (A) `DebugImageOcrReceiver` (debug-only, imágenes
  reales → OCR → pipeline, **ya demostrado**, §6.2) de (B) flujo normal
  (captura del sistema → accesibilidad/MediaProjection → OCR → detección →
  parser → overlay), que **todavía NO está demostrado** en DEVICE‑01.
- **Pasos para reproducir**: con DEVICE‑01 y accesibilidad habilitada, abrir una
  oferta real de una de las 4 apps y comprobar si llega request al pipeline y el
  overlay lo muestra.
- **Resultado observado**: servicio de accesibilidad **habilitado y bindeado**
  (`enabled_accessibility_services` contiene
  `CaptureAccessibilityService:0`; `Bound services` en `dumpsys accessibility`);
  sin embargo **cero logs** de `CapturePipeline`/`MediaProjectionCapture` en
  logcat (la ruta de accesibilidad es silenciosa: no loguea rechazos de
  paquete/tipo/textos vacíos/dedup) y sin proyección activa (`PROJECT_MEDIA
  ignore`, `dumpsys media_projection` vacío). No se pudo comprobar un request
  real con origin `ACCESSIBILITY` ni E2E hasta el overlay.
- **Resultado esperado**: captura real → pipeline → overlay en el flujo normal.
- **Impacto**: **P0** — el producto no demuestra aún el flujo completo en
  dispositivo; el OCR debug NO equivale a captura real.
- **Evidencia / causa probable (código)**: la cadena está cableada
  (`CaptureAccessibilityService.kt:39-44` recolecta `@CaptureRequests` →
  `pipeline.process`), pero depende de que el usuario habilite el servicio
  (SIRC solo abre `ACTION_ACCESSIBILITY_SETTINGS`,
  `PermissionManager.kt:79-81`) y de que exista una oferta real visible.
  Candidatos: servicio habilitado pero sin eventos (`NotificationTimeout`,
  filtros en `accessibility_service_config.xml:3,11`), `DETAILED_LOGS`/tags
  silenciosos antes del pipeline, `PROJECT_MEDIA` rechazado para frames. No hay
  log de entrada para distinguir "no llegan eventos" de "se descartan".
- **Estado**: **INSUFFICIENT_EVIDENCE** (el flujo B no se ha demostrado;
  requiere más instrumentación de logs o prueba con oferta real).

## 7. Resultados actuales

Se ejecutaron dos conjuntos de pruebas reales en DEVICE‑01: (a) OCR + detección
+ parsing sobre 5 capturas vía receptor debug‑only (§6.2) y (b) validación
manual de configuración, captura y overlay (DVC‑01…DVC‑04, §6.4).

| Área | Resultado |
|---|---|
| Baseline Git + build local | **PASS** (compilable, árbol limpio — ver §2.1) |
| Instalación (`adb install -r`) + apertura | **PASS** (DEVICE‑01) |
| OCR en dispositivo (5 imágenes reales) | **PASS** — texto leído correctamente (montos/duración/distancia presentes) |
| Detección de plataforma (`PACKAGE_MATCH`) | **PASS 5/5** con `packageName` real (2 INDRIVE, 3 UBER) |
| Rechazo de pantallas no-oferta | **PASS** — uber_1 (UNKNOWN) y uber_3 (HOME) → NULL, no se fuerza |
| Parsing del monto | **FAIL 0/3** — K1 (§10): 479.0 / 5.0 / 90.0 (reales $4,50 / $4,50 / $25.53) |
| Parsing distancia / duración | **PARCIAL** — dist 2/3, dur 3/3 (indriver_2 dist 0.0 por "kn" de OCR) |
| Latencias del pipeline (OCR→parse) | 175–317 ms en dispositivo (VEL‑1 parcial; overlay/UI sin medir) |
| Accesibilidad habilitada | **PASS** — servicio bindeado (logcat); request real SIN confirmar (DVC‑04) |
| Overlay en físico | **FAIL** — no aparece (DVC‑03, P0) |
| Config post-onboarding editable | **FAIL** — vehículo/país/costos/plataformas no reeditables (DVC‑01) |
| MediaProjection | **FAIL/INSUFFICIENT_EVIDENCE** — appop `PROJECT_MEDIA` ignore (DVC‑02/DVC‑04) |
| Resto de la matriz (vida, batería, privacidad dinámica, decisión con overlay…) | **PENDING — SIN EVIDENCIA** |

## 8. Métricas

Primera medición real en DEVICE-01 (16-ago-2026, §6.2). Muestra 5 capturas →
**no declarable como latencia oficial** (no alcanza el mínimo §6.1), pero es
evidencia física de magnitud:

- Latencias del pipeline (OCR interno + detección + parsing) por imagen:
  **175.5 / 198.1 / 198.6 / 241.6 / 317.4 ms** → todas < 1 s (objetivo UX).
- El `ocr(ext)` del receiver (re-OCR de la imagen) añadió 155–482 ms por
  imagen; es medición del receptor de prueba, no del flujo real de producción
  (en producción el OCR del pipeline es la única OCR sobre el frame).
- `detectionMillis`/`parseMillis` no se registraron en imágenes sin oferta
  (pipeline cortocircuita antes de parsear) — comportamiento correcto.

Infraestructura de métricas ya existente (sin cambios):
`OfferPerformanceTracker`, `ProcessingMetrics`, `ValidationRecorder`.

**No se declara latencia oficial** hasta muestra ≥ mínima (§6.1).

## 9. Privacidad y seguridad (estado estático, sin dispositivo)

- **Privacidad (PRV‑1, estática)**: el diseño actual es LOCAL-FIRST; el
  pipeline de oferta no contiene llamadas a backend ni reproesor de supabase en
  este commit. La confirmación dinámica (auditoría de tráfico real) queda
  **PENDING** hasta tener dispositivo.
- **Seguridad (SEC‑1, estática)**: recorrido en árbol con `git ls-files` no
  muestra claves/credenciales en el repo; no se implementa Play Billing ni
  secretos en este sprint. Verificación dinámica pendiente.
- No se suben datos de pantalla (diseño); sin dispositivo no se demuestra.

## 10. Fallos encontrados

Hallazgos manuales DVC‑01…DVC‑04 registrados en §6.4 (con descripción,
repro, observado/esperado, impacto, severidad, evidencia, componente y estado).
Se resumen aquí por severidad:

| ID | Resumen | Severidad | Estado |
|---|---|---|---|
| DVC‑03 | Overlay no aparece en DEVICE‑01 (sí en emulador) | P0 (Alta) | FAIL |
| DVC‑04 | Flujo normal de captura no demostrado en físico (Escenario B) | P0 (Alta) | INSUFFICIENT_EVIDENCE |
| DVC‑01 | Config post-onboarding (vehículo/país/ciudad/costos/plataformas) no editable | Alta | FAIL |
| DVC‑02 | Captura de pantalla: selección single-app/full no controlable ni persistida | Alta | FAIL / INSUFFICIENT_EVIDENCE |
| K1 | Parser monto FAIL 0/3 en ofertas reales | Alta | FAIL (registrado) |
| K2 | Detección keywords AMBIGUOUS sin packageName (confirmado en campo) | Media | PARCIAL |

### K1 — Parser extrae monto incorrecto en ofertas reales (3/3) [NUEVO]

En las 3 ofertas reales el `estimatedTotal` salió mal aunque el texto OCR
contenía el monto correcto:

| Imagen | Texto OCR (monto real) | `estimatedTotal` | Causa probable |
|---|---|---|---|
| indriver_1.jpg | `Aceptar por $4,50` | 479.0 | Matcheó el número de calle "479 A 1 Pa." como monto |
| indriver_2.jpg | `Aceptar por USD4.5`, `$4.50, Efectivo` | 5.0 | Tomó "USD5" (sugerencia) en vez de 4.5 |
| uber_2.jpg | `$25.53` | 90.0 | Tomó `$090 incluido` en vez de 25.53 |

Registrado para depuración posterior (NO corregir en esta pasada, §5.10).
El problema está en la extracción (parser), no en el OCR.

### K2 — Detección por keywords AMBIGUOUS sin `packageName` [CORREGIDO en G2, 20-ago]

Sin `packageName` (primer intento del test) el detector devolvió `AMBIGUOUS`
(todas las plataformas compartían `defaultRules` y el `matchScore` no distinguía
plataformas); con `packageName` real resolvió `PACKAGE_MATCH` en 5/5.

**Fix G2 (20-ago):** `PlatformDescriptor.platformKeywords` (identificadores
fuertes de marca) es la única señal de identidad por OCR; `matchScore` puntúa
solo por esas marcas y `KEYWORD_CANDIDATE` exige score > 0. Verificado en
`PlatformDetectionEngineTest` (matriz 14 casos + `DetectionMatcherTest` 9):
marca única sin paquete → plataforma; palabras genéricas o dos marcas →
`AMBIGUOUS`→`UNSUPPORTED_PLATFORM`; paquete inequívoco siempre gana. En
producción el paquete llega vía accesibilidad (PACKAGE_MATCH), y la ruta OCR
conserva el fallback determinista.

### K1‑build — Dependencia necesaria para el receptor de prueba

`app/build.gradle.kts` requiere `implementation(project(":core:platform"))`
para resolver `PlatformDetectionEngine`/`CaptureInputType`/`DetectionResolution`
en el source set `debug` (KSP no resolvía transitivamente). Solo afecta a
`app` (debug); no cambia el comportamiento de producción ni de otras
dependencias. Sin este cambio el receptor de prueba no compila.

---

Los problemas conocidos ya documentados (`docs/KNOWN_ISSUES.md`, 10 incidencias)
siguen **sin validar en campo** y NO se convierten en PASS.

## 11. Severidad

| Severidad | Elementos | Estado |
|---|---|---|
| P0 | E2E real, overlay estable, captura real, OCR medido, decisión medida | **PARCIAL** — OCR/detección/parsing reales (§6.2); **overlay FAIL en físico (DVC‑03)**, captura E2E SIN demostrar (DVC‑04) |
| P1 | Uber validado, otra plataforma validada, ciclo de vida, estabilidad prolongada | **PARCIAL** — Uber e InDriver parsing real (§6.2); **config post-onboarding NO editable (DVC‑01)**, captura config FAIL (DVC‑02) |
| P2 | Batería/memoria caracterizadas | PENDING |

## 12. Evidencias

- **Local (no física)**: salidas de Gradle `BUILD SUCCESSFUL` (ktlint, lint,
  assemble, unit tests) y `app-debug.apk` generado — sección §2.1.
- **Física (16-ago-2026, DEVICE-01)**:
  - `docs/testing/evidence/SIRC_OCR_TEST_logcat_dump.txt` — logcat de la prueba
    OCR (136 líneas; 5 imágenes, etapas y textos OCR reales).
  - `docs/testing/evidence/DVC_diagnostics_logcat_dump.txt` — diagnóstico de
    DVC‑01…DVC‑04 (accesibilidad habilitada, ausencia de OverlayService y de
    ventana overlay, `appops PROJECT_MEDIA`, `dumpsys media_projection`).
  - 5 capturas reales empaquetadas en `app/src/debug/assets/sirc_test/`
    (`uber_1..3.jpg`, `indriver_1..2.jpg`).
  - Por prueba (ID, dispositivo, fecha, versión, pasos, resultado, evidencia,
    observaciones): DVC‑01…DVC‑04 en §6.4; resto a completar (EV‑1).
- **Física pendiente**: screenshots anonimizados, overlay en físico, batería,
  ciclo de vida, auditoría de tráfico.

## 13. Limitaciones

- **1 dispositivo físico** conectado (DEVICE-01, Infinix X6850, Android 15) —
  por debajo del mínimo 2; ideal 3. OEM XOS (Transsion) con gestión agresiva
  de batería/hibernación que puede afectar FGS/overlay.
- La prueba OCR (§6.2) inyecta imágenes con el receptor debug-only; **no**
  valida el flujo real de accesibilidad/MediaProjection ni el overlay (DVC‑04).
- **El overlay no se validó en físico** (DVC‑03): no apareció; causa probable
  identificada pero sin confirmar con logs del servicio en campo.
- La verificación del monto real por imagen usa los **textos OCR leídos** como
  referencia (el modelo de IA no tiene visión para comparar con el JPG); la
  confirmación visual por el usuario sigue abierta.
- Sin cuentas/plataformas reales conectadas no se puede validar el flujo de
  ofertas en vivo ni el overlay sobre la app.
- No se ejecutó jornada en ruta, ni batería en sesiones prolongadas, ni
  revocación de permisos en campo.

## 14. Conclusión

**Sprint 12 / E1a avanza parcialmente el 16-ago-2026**: con 1 dispositivo real
hay evidencia física de que el pipeline ejecuta OCR en el propio teléfono,
detecta por paquete (5/5) y rechaza pantallas no-oferta; latencia pipeline real
175–317 ms (< 1 s objetivo UX).

**Lo que funciona**: OCR real, detección por paquete, rechazo de no-ofertas,
instalación. **Lo que NO funciona**: el **overlay no aparece en físico (DVC‑03,
P0)** y la **config post-onboarding no es editable (DVC‑01)**. **Demostrado
solo vía debug**: OCR/parser sobre imágenes (Escenario A). **NO demostrado en
flujo real**: captura por accesibilidad/MediaProjection → OCR → overlay (Escenario
B, DVC‑04), con `PROJECT_MEDIA` rechazado y cero logs del pipeline en el físico.

**Defectos registrados (sin corregir, §5.10)**: K1 (parser monto FAIL 0/3),
K2 (keywords ambiguas), DVC‑01 (config no editable), DVC‑02 (captura no
configurable/persistida), DVC‑03 (overlay no aparece), DVC‑04 (flujo normal sin
demostrar). El resto de la matriz sigue **PENDING / SIN EVIDENCIA** (instalación
completa, ciclo de vida, estabilidad, batería, privacidad dinámica, decisión con
overlay).

**Próximo paso (no abierto)**: diagnosticar la causa exacta de DVC‑03 (logs del
servicio, excepción de `addView`, gestión XOS del FGS) y de DVC‑04 (¿llegan
eventos de accesibilidad? ¿se descartan?) antes de corregir. No se abre Sprint
13 ni otro LOOP sin autorización explícita.

---

## 15. Actualización post-correcciones (18-ago-2026) — verificación FIX-01…05

> **Estado actual.** Las secciones §1–§14 documentan el 16-ago-2026
> (pre-correcciones) y se conservan íntegras como evidencia del diagnóstico.
> Detalle operativo de las correcciones en `TASK.md`, `.ai/CONTEXT.md` y el plan
> `docs/superpowers/plans/2026-08-16-sprint-12-fixes.md`.

### 15.1 Correcciones aplicadas y verificadas (DEVICE-01, Infinix X6850 / Android 15 / API 35)

| FIX | Hallazgo | Corrección | Verificación |
|---|---|---|---|
| FIX-01 | DVC-03 — overlay no aparece | Banner real: `PipelineOverlayDataSource.start()` → `WAITING` + visible ("Esperando oferta…"); `OverlayService` con error visible / `START_NOT_STICKY` si `addView` falla; `isRunning` real vía `onServiceRunning`; `OverlayContent` sin `fillMaxSize` (no bloquea toques) | OverlayService en `dumpsys activity services`; ventana `ty=APPLICATION_OVERLAY`; UI "Activo/Inactivo" real; overlay mostró la oferta real en vivo (`overlay mostrando`, 18-ago 16:37) |
| FIX-02 | DVC-04 — flujo normal sin demostrar | Causa raíz: `android:packageNames` del config XML filtraba el paquete real de InDrive Ecuador (**`sinet.startup.inDriver`**). Ampliado config XML + `PlatformDescriptors.packageNames` + instrumentación `SircLogger` en la ruta | E2E real: `AccessibilityInput request programado: package=sinet.startup.inDriver` → `detección: INDRIVE / REQUEST` → `snapshot INDRIVE guardado` → `overlay mostrando: INDRIVE · $2.9/$3.1 · REJECT` (logcat 18-ago 16:37) |
| FIX-03 | DVC-01 — config post-onboarding no editable | Settings edita perfil/vehículo/combustible/mantenimiento/otros costos/plataformas/umbrales/overlay; **costPerKm DERIVADO** (única fuente de verdad) + gate de plataformas en captura; 2 bugs runtime corregidos (`reloadTick`; crash `Parcel: CostDraft`) | En físico: derivado 0.5417 → 0.625 (fuel 1.5) → 0.925 (+Peaje 0.3); BD final `costPerKm=0.925 fuelPrice=1.5 city=Guayaquil additionalCosts=Peaje^_0.3 platforms=CABIFY,INDRIVE,UBER`; persiste tras force-stop/reopen |
| FIX-04 | K1 — parser monto FAIL 0/3 | `OfferTextParser` exige marcador de moneda, rechaza ceros a la izquierda (`$090`), recorta separadores; extractor sin fallback `maxByOrNull`; keyword "aceptar" en INDRIVE | 479.0 / 5.0 / 90.0 → **4.5 / 4.5 / 25.53** (3/3) en `K1AmountRegressionTest` (fixtures reales del dump); dist/dur sin regresión; suite `:core:platform` verde |
| FIX-05 | Higiene de artefactos | Convención `/sdcard/SIRC_TEST/{images,logs,evidence,exports,tmp}/` | 57 archivos reubicados sin borrar (57 antes = 57 después); raíz `/sdcard` limpia de `sirc_*`; 0 coincidencias `sirc` en carpetas personales |

### 15.2 Matriz Sprint 12 — estado real consolidado (18-ago-2026)

Leyenda: **DEMO** = demostrado en físico; **PARC** = demostrado parcialmente;
**PEND** = pendiente por hardware/cuenta/condiciones; **NV** = no validado.

| ID | Prueba | Estado | Evidencia |
|---|---|---|---|
| INST-1 | Instalación limpia | DEMO | `adb install -r`, abre |
| INST-2 | Actualización previa | PEND | sin versión anterior con datos |
| INST-3 | Onboarding + config | DEMO | onboarding completo; config editable verificada (FIX-03) |
| INST-4 | Cierre y reapertura | PARC | force-stop/reopen → config persiste (FIX-03) |
| CAP-1 | Accesibilidad | DEMO | request real origin=`ACCESSIBILITY` (e2e_pipeline/final.log) |
| CAP-2 | CaptureInput→dedup | PARC | requests coalescidas por debounce (IDs únicos) |
| CAP-3 | Debounce 400 ms | PARC | "request programado… emitido tras debounce"; sin medición aislada del intervalo |
| CAP-4 | MediaProjection | PARC | 1 frame real enriquecido (300759 B PNG, 17-ago); degrada a textos al denegar; **DVC-02 pendiente** |
| CAP-5 | Recuperación errores | PARC | degradación a textos sin crash tras denegación |
| OCR-1 | Dataset real por plataforma | PARC | 5 capturas reales + ofertas en vivo; muestra < mínima (§6.1) |
| OCR-2 | Monto/dist/duración | PARC | monto K1 corregido (JVM con fixtures reales); dist 2/3, dur 3/3 (histórico) |
| OCR-3 | test-images NO única fuente | DEMO | imágenes reales de DEVICE-01 + ofertas en vivo |
| PLT-1 | Uber | PEND | device sin `com.ubercab.driver` (solo pasajero); sin cuenta conductor |
| PLT-2 | DiDi | PEND | sin cuenta |
| PLT-3 | Cabify | PEND | sin cuenta |
| PLT-4 | InDrive | DEMO | E2E completo en vivo (captura→det→parse→eval→overlay) |
| PLT-5 | Keywords | CORR | PACKAGE_MATCH 5/5 con paquete; sin paquete: marca única→plataforma, genéricas/2 marcas→AMBIGUOUS (G2 corregido, 20-ago) |
| VEL-1 | Latencias por etapa | PARC | pipeline 175–317 ms (5 imgs, 16-ago); det 16–28 ms; parse 5.7–13.9 ms; eval 1.3–8.6 ms; overlay 8.8–15.6 ms; OCR full-frame MP ~2.5 s (1 muestra) |
| VEL-2 | Decisión <1 s | PARC | pipeline accesible <1 s; **sin cronómetro en ruta** |
| VEL-3 | E2E <3 s | PARC | trayectoria accesible ≪3 s; OCR full-frame ~2.5 s (dentro de 3 s, 1 muestra); **sin cronómetro en ruta** |
| EST-1 | Sesión 30 min | PEND | — |
| EST-2 | Sesión 8 h | PEND | — |
| CVD-1 | bg/fg | PEND | — |
| CVD-2 | rotación/bloqueo/split | PEND | — |
| CVD-3 | proceso muerto→reabrir | PEND | obs: force-stop deshabilita el servicio de accesibilidad (README.txt) |
| CVD-4 | reinicio/permisos/reinstalar | PEND | — |
| BAT-1 | Batería | PEND | solo snapshot meminfo |
| BAT-2 | Memoria panel | PEND | snapshot meminfo (MemTotal 7.8 GB / MemAvailable 1.6 GB) |
| OVL-1 | Aparece/desaparece | PARC | aparece con oferta real (log + ventana); TTL/arrastre sin validar; artefacto de evidencia incompleto (§15.4) |
| OVL-2 | Semáforo/métricas | PARC | mostró `INDRIVE · $2.9/$3.1 · REJECT`; $/km, $/hora, confianza sin evidencia en vivo |
| OVL-3 | FLAG_NOT_TOUCHABLE | PARC | banner no bloquea toques (verificado manual, FIX-01) |
| OVL-4 | oculto/rotación/bloqueo | PEND | — |
| DEC-1 | rentable→ACCEPT | DEMO | offer_history id 61–63, 65–68 = PROFITABLE/ACCEPT (reales) |
| DEC-2 | no rentable→REJECT | DEMO | offer_history id 4–60, 72–74 = NOT_PROFITABLE/REJECT (74 reales) |
| DEC-3 | ambigua→WARNING | PARC | id 64, 69–71 = MARGINAL/WARNING |
| DEC-4 | coherencia métricas/costos | PEND | sin validación cruzada UI |
| CFG-1 | Config→cálculo | DEMO | costPerKm derivado + gate de plataformas verificados (FIX-03) |
| ERR-1 | OCR fallido/incompleto | PARC | "kn"→dist 0.0 sin crash; degradación |
| ERR-2 | plataforma desconocida/permisos | PARC | degradación a textos al denegar `PROJECT_MEDIA` |
| PRV-1 | 100 % local | PARC | estático: sin backend en el pipeline; dinámico (tráfico) PEND |
| SEC-1 | Sin secretos | PARC | estático: `git ls-files` sin claves; dinámico PEND |
| EV-1 | Evidencia por prueba | PARC | consolidada en `/sdcard/SIRC_TEST/` + repo; no todas con formato EV-1 completo |

### 15.3 Evidencia física consolidada (DEVICE-01)

| Artefacto | Contenido |
|---|---|
| `/sdcard/SIRC_TEST/logs/e2e_pipeline.log` | 17-ago 01:10–01:12: requests de accesibilidad (`sinet.startup.inDriver`), detección 16–28 ms, snapshots 24.7–40.9 ms, 1 request `MEDIA_PROJECTION` |
| `/sdcard/SIRC_TEST/logs/e2e_final.log` | 18-ago 16:37: `overlay mostrando: INDRIVE · $2.9/$3.1 · REJECT (origen=ACCESSIBILITY · eval 8.6/1.3 ms · reglas 0.7/0.0 ms · overlay 15.6/8.8 ms)` |
| `/sdcard/SIRC_TEST/evidence/offer_history.txt` | Room offer_history: 74 ofertas reales INDRIVE (id 4–74) con ACCEPT/WARNING/REJECT |
| `/sdcard/SIRC_TEST/evidence/README.txt` | Resumen FIX-02 + hallazgos de entorno |
| `/sdcard/SIRC_TEST/evidence/fix03/fix03_evidence.txt` | Post force-stop: UI "Costo por km (calculado) = 0.925", fuel 1.5, Peaje 0.3, chips de plataformas; BD final `costPerKm=0.925 … platforms=CABIFY,INDRIVE,UBER` |
| `docs/testing/evidence/sprint12/` | DVC dump, OCR dump, crashcheck, meminfo, overlay window (parcial, §15.4) |
| `core/platform/src/test/.../K1AmountRegressionTest.kt` | Regresión K1: 4.5 / 4.5 / 25.53 con fixtures reales del dump |

### 15.4 Hallazgos abiertos (registrados, NO corregidos)

- **DVC-02 (P1/P2)**: fuente de captura single-app/full-screen no controlable ni
  persistida; `PROJECT_MEDIA=ignore` en el físico. El enriquecimiento de frame
  funciona (1×) pero no es reproducible por diseño. Decisión de producto
  pendiente (usuario).
- **Mecanismo debug (§6.3)**: mantener vs eliminar `DebugImageOcrReceiver` —
  decisión pendiente (usuario).
- **Calidad de evidencia FIX-01 (P2)**: `docs/testing/evidence/sprint12/DEVICE-01_overlay_window.txt`
  no contiene el dump de la ventana banner (solo un fragmento de NotificationShade);
  el estado del banner se documentó en TASK.md/README y se confirmó en logcat
  (`overlay mostrando`), pero el dump exacto (`ty=APPLICATION_OVERLAY`,
  `Requested 885x…`, `isVisible=true`, `HAS_DRAWN`) no quedó capturado.
- **Conocidos no bloqueantes**: panel Debug/sesión en memoria (se pierde al
  reiniciar SIRC); uiautomator "null root node" tras ciclo de overlay; OCR
  full-frame MP ~2.5 s; force-stop deshabilita el servicio de accesibilidad;
  overlay solo iniciable por UI (`am startforegroundservice` falla: no
  exported); "Exportar diagnóstico" abre un chooser (no crea archivo); jank
  inicial en 16-ago (Skipped 377–430 frames, Davey! 6415 ms); `$$` duplicado en
  el log de overlay (cosmético).
- **PENDIENTES por condiciones**: cuentas reales Uber Driver / DiDi / Cabify;
  muestra ≥ 20 por plataforma; jornada en ruta; batería; ciclo de vida;
  auditoría de tráfico dinámica; EV-1 completo.

### 15.5 Conclusión — estado real de E1a

**Sprint 12 / E1a = PASS WITH PENDING** (18-ago-2026):

- **Núcleo validado en físico**: captura por accesibilidad → detección →
  parser → evaluación → overlay **DEMOSTRADO** con ofertas reales de InDrive
  Ecuador en DEVICE-01. Los P0 (DVC-03 overlay, DVC-04 flujo normal) y Alta
  (DVC-01 config, K1 parser) quedaron **CORREGIDOS y verificados**.
- **PENDIENTE (condicionado a hardware/cuenta/condiciones, NO a defectos de
  código)**: validación real de Uber Driver/DiDi/Cabify (sin cuentas), muestra
  ≥ mínima (≥20 por plataforma), jornada en ruta (<1 s cronometrado, estabilidad
  8 h, batería), ciclo de vida, DVC-02 (decisión de producto), PRV-1/SEC-1
  dinámicos, EV-1 completo.
- **No se abre Sprint 13 ni se implementa monetización** (E1b, Supabase,
  Billing, Play Integrity, trial, AHU, anti-fatiga) sin autorización explícita.
# SIRC — Sprint 12 / E1a: Validación real del núcleo (SPRINT_12_DEVICE_VALIDATION)

> Sprint **BLOQUEADO / PENDING** por falta de dispositivos físicos.
> Este documento registra la auditoría pre-validación, la matriz de pruebas
> completa, el procedimiento listo para ejecutar y el estado **SIN EVIDENCIA**
> de cada prueba dependiente de hardware. No se inventan métricas ni resultados.

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
| 16-ago-2026 | `adb devices -l` → **ningún dispositivo físico conectado** |
| 16-ago-2026 | AVD disponibles: `Pixel_7_API_35` (emulador, API 35) — **no sustituye** lo físico |

### 3.2 Registro de dispositivos (a cumplimentar)

| Campo | Dispositivo 1 | Dispositivo 2 | Dispositivo 3 |
|---|---|---|---|
| Fabricante | — | — | — |
| Modelo | — | — | — |
| Android | — | — | — |
| API level | — | — | — |
| RAM | — | — | — |
| Resolución | — | — | — |
| Versión SIRC | — | — | — |
| Método de instalación | — | — | — |
| Fecha de prueba | — | — | — |

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

Estado general: **PENDING — SIN EVIDENCIA** (falta hardware físico).

| # | Área | Prueba | Resultado esperado | Estado |
|---|---|---|---|--|
| INST‑1 | Instalación | Instalación limpia en dispositivo real | Instala y abre sin error | PENDING — SIN EVIDENCIA |
| INST‑2 | Instalación | Actualización desde versión anterior | No pierde datos ni crashea | PENDING — SIN EVIDENCIA |
| INST‑3 | Instalación | Primera ejecución + onboarding (6 pasos) | `showDecision`/`costPerKm` configurados y reflejados | PENDING — SIN EVIDENCIA |
| INST‑4 | Instalación | Cierre y reapertura | Estado UI persistente | PENDING — SIN EVIDENCIA |
| CAP‑1 | Captura | Textos por accesibilidad (`CaptureAccessibilityService`) | Request con origin `ACCESSIBILITY` | PENDING — SIN EVIDENCIA |
| CAP‑2 | Captura | CaptureInput → CaptureRequest → dedup | Sin duplicados de snapshot | PENDING — SIN EVIDENCIA |
| CAP‑3 | Captura | Debounce (400 ms) | Requests coalesced | PENDING — SIN EVIDENCIA |
| CAP‑4 | Captura | MediaProjection cuando corresponde | `imageData` presente; degrada a textos si falla | PENDING — SIN EVIDENCIA |
| CAP‑5 | Captura | Recuperación tras error de captura | Sin crash; mensaje claro | PENDING — SIN EVIDENCIA |
| OCR‑1 | OCR (P0) | Dataset real por plataforma | `OCR_FIELD_ACCURACY` y `OFFER_PARSE_ACCURACY` calculados | PENDING — SIN EVIDENCIA |
| OCR‑2 | OCR (P0) | Monto / distancia / duración extraídos | Campos correctos | PENDING — SIN EVIDENCIA |
| OCR‑3 | OCR (P0) | `test-images/` NO como única fuente | Dataset real de validación | PENDING — SIN EVIDENCIA |
| PLT‑1 | Uber | Flujo completo (detección + parse + overlay + decisión) | E2E correcto | PENDING — SIN DISPOSITIVO/CUENTA/OFERTAS REALES |
| PLT‑2 | DiDi | Captura + detección + parsing | Correcto | PENDING — SIN DISPOSITIVO/CUENTA/OFERTAS REALES |
| PLT‑3 | Cabify | Captura + detección + parsing | Correcto | PENDING — SIN DISPOSITIVO/CUENTA/OFERTAS REALES |
| PLT‑4 | InDrive | Captura + detección + parsing | Correcto | PENDING — SIN DISPOSITIVO/CUENTA/OFERTAS REALES |
| PLT‑5 | Keywords | Hallazgo previo: keywords ambiguas | Verificar AMBIGUOUS→GENERIC en pantallas reales; registrar, NO corregir | PENDING — SIN EVIDENCIA |
| VEL‑1 | Rendimiento | Latencias por etapa (captura/OCR/detección/parse/eval/overlay/total) | min/max/avg (+p95 si muestra ≥ suficiente) | PENDING — SIN EVIDENCIA |
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
| OVL‑1 | Overlay | Aparece/desaparece/actualización/arrastre/TTL | Correcto | PENDING — SIN EVIDENCIA |
| OVL‑2 | Overlay | Semáforo, ganancia, $/km, $/hora, confianza | Valores coherentes | PENDING — SIN EVIDENCIA |
| OVL‑3 | Overlay | `FLAG_NOT_TOUCHABLE`; no interfiere con la app | Toques pasan a la plataforma | PENDING — SIN EVIDENCIA |
| OVL‑4 | Overlay | Comportamiento oculto, rotación, bloqueo/desbloqueo | Correcto | PENDING — SIN EVIDENCIA |
| DEC‑1 | Decisión | Oferta claramente rentable → ACEPTAR | Recomendación correcta | PENDING — SIN EVIDENCIA |
| DEC‑2 | Decisión | Oferta no rentable → RECHAZAR | Recomendación correcta | PENDING — SIN EVIDENCIA |
| DEC‑3 | Decisión | Oferta ambigua → REVISAR | Recomendación correcta | PENDING — SIN EVIDENCIA |
| DEC‑4 | Decisión | Coherencia métricas/costos/confianza | Sin editar el motor; localizar la causa (captura/OCR/parse/config/eval) | PENDIENTE DE PRUEBA REAL |
| CFG‑1 | Configuración | Onboarding afecta vehículo/combustible/mantenimiento/costos/objetivos/plataformas | `costPerKm` y `showDecision` comprobados en dispositivo | PENDING — SIN EVIDENCIA |
| ERR‑1 | Errores | OCR fallido, texto incompleto, sin distancia/duración | Sin crash; mensaje apropiado | PENDING — SIN EVIDENCIA |
| ERR‑2 | Errores | Plataforma desconocida, permiso revocado, captura interrumpida, servicio detenido, proceso muerto | Recuperación; sin datos corruptos | PENDING — SIN EVIDENCIA |
| PRV‑1 | Privacidad | Pipeline de oferta sin backend/Supabase/internet/Realtime | 100 % local (auditoría de tráfico en dispositivo) | PENDING — SIN EVIDENCIA (ver §9 / nota estática) |
| SEC‑1 | Seguridad | Sin secretos/API keys/credenciales en APK ni logs | Auditable | Ver §9 (auditoría estática inicial) |
| EV‑1 | Evidencia | Por prueba: ID, dispositivo, fecha, versión, pasos, resultado, evidencia, observaciones | A completar en campo | PENDING |

### 6.1 Muestra

Requisito: mínimo 20 ofertas reales por plataforma (20+ Uber / DiDi / Cabify /
InDrive). Sin dispositivos: **MUESTRA INSUFICIENTE** — no se extrapola nada.

## 7. Resultados actuales

En esta primera pasada NO se ejecutaron pruebas dependientes de hardware.
Único resultado confirmado:

| Área | Resultado |
|---|---|
| Baseline Git + build local | **PASS** (compilable, árbol limpio — ver §2.1) |
| Todo lo dependiente de dispositivo | **PENDING — SIN EVIDENCIA** |

## 8. Métricas

Sin dispositivo real no hay latencias medibles. La infraestructura para
recolectarlas está lista y verificada en código:

- `OfferPerformanceTracker` (promedio últimas 20, buffer 100) por etapa.
- `ProcessingMetrics` (`captureMillis`/`ocrMillis`/`detectionMillis`/
  `parseMillis`/`totalMillis`).
- `ValidationRecorder` (buffer 500; informes exportables).
- Procedimiento de medición en `docs/PERFORMANCE_REPORT.md` §"Procedimiento de
  medición en dispositivo (RC1)".

**No se declara ningún número de latencia** porque no fue medido.

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

Ninguno en esta pasada (no se ejecutó prueba de dispositivo). Los problemas
conocidos ya documentados (`docs/KNOWN_ISSUES.md`, 10 incidencias) siguen
**sin validar en campo** y NO se convierten en PASS.

## 11. Severidad

| Severidad | Elementos | Estado |
|---|---|---|
| P0 | E2E real, overlay estable, captura real, OCR medido, decisión medida | PENDING |
| P1 | Uber validado, otra plataforma validada, ciclo de vida, estabilidad prolongada | PENDING |
| P2 | Batería/memoria caracterizadas | PENDING |

## 12. Evidencias

- **Local (no física)**: salidas de Gradle `BUILD SUCCESSFUL` (ktlint, lint,
  assemble, unit tests) y `app-debug.apk` generado — sección §2.1.
- **Física**: pendiente. Cuando exista hardware se anexarán por prueba:
  screenshots (anonimizados), `adb logcat`, exportar diagnóstico, informe de
  validación.

## 13. Limitaciones

- **Cero dispositivos físicos disponibles** vía `adb` (bloqueo principal).
- Solo existe AVD `Pixel_7_API_35` (emulador); no sustituye lo físico.
- Sin cuentas/plataformas reales (Uber/DiDi/Cabify/InDrive) no se puede validar
  el flujo real de ofertas ni OCR.
- No se ejecutó jornada en ruta, ni batería en sesiones prolongadas, ni
  revocación de permisos en campo.

## 14. Conclusión

**Sprint 12 / E1a queda BLOQUEADO / PENDING por falta de hardware físico.**
El producto compila y sus unit tests están en verde (evidencia local), pero el
objetivo del sprint es **evidencia física reproducible**: overlay <1 s/<3 s,
OCR medido, captura real, estabilidad, ciclo de vida, batería y plataformas
reales. Nada de eso se ha medido porque no hay dispositivos conectados y el
sprint prohíbe simular éxito o inventar métricas.

**Próximo paso (no abierto)**: cuando el usuario conecte ≥1 dispositivo físico
(ideal 2–3, Android 10–15), ejecutar esta matriz en el orden §5. No se abre
Sprint 13 ni otro LOOP de implementación sin autorización explícita.
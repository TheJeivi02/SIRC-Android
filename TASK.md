# TASK — Estado en vivo

> Actualiza este archivo al iniciar y terminar cada tarea relevante. Es el
> estado que cualquier agente lee primero para saber qué sigue sin re-leer el
> proyecto.

## Tarea actual

**LOOP ENGINEERING G8 (20-ago-2026) — Auditoría de textos fantasma / obsoletos.
COMPLETADO.** Auditoría repo-wide de textos que describen comportamiento que ya
no existe; corrección solo de lo obsoleto/engañoso visible; sin lógica de
negocio; commit + push a `main`:

- **Hallazgo clave**: desde WP-E1-02 `ruleEvaluation` se expone **siempre**
  vacío (`PipelineOverlayDataSource.kt:234` → `RuleEvaluation(emptyList())`;
  KDoc "compatibilidad de la UI"), por lo que la sección "Análisis" del Debug
  Panel mostraba un ghost text que prometía veredictos por regla (ganancia, por
  km, por hora, distancia, recogida, duración) que **no pueden aparecer nunca**.
- **Corregidos**: (C) ghost text de "Análisis" eliminado + dead code asociado
  (`RuleRow`, `ruleResults`, `verdictColor`, imports); (B) fila "Reglas
  fallidas" del modo validación eliminada; (B) línea "Reglas fallidas" del
  informe de validación exportado eliminada. Se conserva `RuleFailed`/
  `ruleFailed` como API del framework (sin productor; testeado en
  `ValidationRecorderTest`). 3 archivos, 53 líneas eliminadas.
- **Sin cambios de comportamiento**: solo textos/UI muerta. No se tocaron
  ProfitEngine/detección/parser/OCR/pipeline/overlay → **no regression risk
  introduced** (no aplica batería física §14/§15).
- **Verificación**: suite completa AGENTS BUILD SUCCESSFUL (ktlintCheck /
  lintDebug / assembleDebug / testDebugUnitTest / :domain:test /
  :core:platform:test / :core:capture:test / :feature:overlay:testDebugUnitTest).
  Sin androidTest/config modificados. Sin tests nuevos (garantía de
  `ruleEvaluation` vacío ya cubierta por `PipelineOverlayDataSourceTest.kt:175-177`).
- **Clasificación del resto**: A legítimos (textos de estado reales, OCR/
  captura, fixture de tests, `rulesMillis` real) y D históricos contextualizados
  (DECISIONS, CHANGELOG, ROADMAP, audits, remediation, manual tests de sprint,
  TASK histórico) — ninguno describe la arquitectura como vigente.
- **Docs**: `docs/testing/G8_GHOST_TEXT_AUDIT.md` (nuevo), `.ai/CONTEXT.md`.
  G2/G3/G5/G6/G7/G10 intactos. **FASE 10 sigue PENDING.**

## Tarea anterior

**LOOP ENGINEERING G10 (20-ago-2026) — Validación instrumentada en dispositivo
(DEVICE-01 / Infinix X6850 / Android 15 / API 35). COMPLETADO.** Infraestructura
androidTest para los componentes DEVICE_REQUIRED de G7, sin cambios de
producción; 2 flakes reales resueltos (no skips); commit + push a `main`:

- **`MlKitOcrEngine` real (DEVICE_VALIDATED comportamiento)**: ML Kit on-device
  procesa las **13 imágenes** del dataset `test-images` sin excepción
  (1–4 líneas/imagen; 60–238 ms; total 1124 ms; decode inválido → `emptyList`).
  **Precisión = NOT_VALIDATED** (el README del dataset declara marcadores, no
  fixtures de precisión). Evidencia: logcat `G10OcrTest`.
- **`OverlayContent` render Compose (INSTRUMENTED_COVERED)**: `OverlayContentTest`
  6 casos (A–F) sobre el Application host de `:feature:overlay`.
- **`OverlayService` smoke (DEVICE_VALIDATED)**: en el Application real (Hilt)
  el FGS arranca (`isForeground=true`, `foregroundId=9001`, specialUse), la
  ventana `ty=APPLICATION_OVERLAY` queda registrada en WindowManager y
  stop/removal no produce crash. Matcher de ventana corregido: el título es el
  package name (`u0 com.sirc.app}`), no "Application Overlay".
- **Infraestructura**: `:feature/overlay` runner + Compose test + assets del
  dataset (ruta `../../core/...`); `:app` runner + androidx.test.runner;
  `libs.versions.toml` + `compose-ui-test-junit4`/`compose-ui-test-manifest`
  (BOM existente, sin upgrades). ktlintFormat aplicado a los tests.
- **Estabilidad**: flake Compose "No compose hierarchies found" (carrera de
  relaunch) resuelto con `compose.waitForIdle()` tras cada `setContent` — 4
  corridas 8/8 consecutivas; smoke 1/1.
- **Verificación**: connected `:feature:overlay` **8/8** + `:app` **1/1** +
  suite completa AGENTS BUILD SUCCESSFUL (ktlintCheck/lintDebug/assembleDebug/
  testDebugUnitTest/:domain:test/:core:platform:test/:core:capture:test/
  :feature:overlay:testDebugUnitTest).
- **Entorno DEVICE-01**: AGP desinstala la app tras cada corrida y Android
  revoca `SYSTEM_ALERT_WINDOW` y DESHABILITA la accesibilidad → re-grant appop +
  re-habilitar accesibilidad (comandos en `docs/testing/G10_INSTRUMENTED_VALIDATION.md`).
  Estado final del dispositivo: app instalada, overlay appop `allow`, accesibilidad
  habilitada.
- **Docs**: `docs/testing/G10_INSTRUMENTED_VALIDATION.md` (nuevo),
  `docs/testing/COVERAGE_G7.md` → VERIFIED, `.ai/CONTEXT.md`. G2/G3/G5/G6
  intactos. **FASE 10 sigue PENDING.**

## Tarea anterior

**LOOP ENGINEERING G7 (20-ago-2026) — Cobertura crítica UI + OCR.
PARTIAL / DEVICE_VALIDATION_PENDING → validada por G10.** (+3 tests JVM;
documentación de frontera).

- **Cobertura OCR (contrato, JVM)**: `DefaultCapturePipelineTest` (13→15)
  añade los dos caminos que el `FakeOcrEngine` ya soportaba pero nadie
  probaba: **`ocr sin texto reconocido descarta sin inventar`** (imagen + OCR
  vacío → snapshot null, `FrameDiscarded(NO_TEXTS)`, `WAITING`, sin datos
  fabricados) y **`error de ocr se registra y degrada sin crashear`** (imagen +
  OCR lanza → `ValidationEvent.OcrFailed` + estado `WAITING` + null). El
  contrato `OcrEngine.recognize` ya estaba cubierto a nivel pipeline (imagen→
  OCR→snapshot, flag OCR off, textos de accesibilidad).
- **Cobertura Overlay (estado crítico)**: `OverlayPresentationMapperTest`
  (25→26) añade **`sin evaluacion el mapper no fabrica presentacion`**
  (evaluation=null → mapper devuelve null; Estado sin oferta/no disponible).
  El resto de estados críticos ya estaban cubiertos (decisiones, oferta,
  resumen, pares de métricas, flags, tonos, compactMode, showDecision,
  no-invención ×3, caso real 5.90/27 min).
- **DEVICE_REQUIRED (documentado en `docs/testing/COVERAGE_G7.md`)**: la
  precisión/rendimiento OCR real (`MlKitOcrEngine`: BitmapFactory + ML Kit, sin
  seam JVM; no se fakesea precisión), `OverlayService` (WindowManager, FGS,
  FLAG_NOT_TOUCHABLE ya validado en físico) y el render Compose
  (`OverlayContent`, sin infra Compose-test en `:feature:overlay`) NO se
  declaran validados por unit tests — la frontera queda explícita en la matriz
  COVERED/DEVICE_REQUIRED/NOT_COVERED.
- **Hallazgo del análisis**: `resolveTexts` degrada a `request.texts` SOLO si
  el OCR lanza excepción (no si devuelve vacío); los requests llevan `texts` o
  `imageData` (XOR), así que OCR vacío + sin textos = descarte `NO_TEXTS`
  correcto (no es defecto; comportamientos documentados en los tests).
- **Suite completa BUILD SUCCESSFUL** (ktlintCheck/lintDebug/assembleDebug/
  testDebugUnitTest/:domain:test/:core:platform:test/:core:capture:test/
  :feature:overlay:testDebugUnitTest). G2/G3/G5/G6 intactos. **FASE 10 sigue
  PENDING.**

## Tarea anterior

**LOOP ENGINEERING G3 (20-ago-2026) — VERIFIED (sin cambios de producción;
+1 test de evidencia).** DiDi/Cabify/InDrive con `offerTypes = emptyList()` NO
bloquean: el contrato existente ya las representa con `OfferType.GENERIC` + el
extractor genérico por plataforma; `OfferParserOrchestrator` cae a GENERIC;
`ProfitEngine` no depende del tipo. NO se inventó `DIDI_REQUEST`/`CABIFY_
REQUEST`/`INDRIVE_REQUEST` (sin evidencia de formatos reales). +1 test
(`OfferParserOrchestratorTest` 14→15: packages reales de Cabify/InDrive →
GENERIC + $120). Commit `34d0e3e` pusheado.

**AUDITORÍA DE CONTINUIDAD (20-ago-2026) — VERIFICADA en código + física en
DEVICE-01. 19 fases ejecutadas; 18/18 verificadas; 1 PENDING explícito
(fase 10). Sin defectos nuevos en el motor/parser/overlay/settings. Cambios
seguros: 4 tests añadidos (activeIndicatorCount + compactMode) y copy
obsoleto corregido en DiagnosisScreen. Fase 15 C/D y D/F validadas en físico
(toggles con persistencia real e ida y vuelta; FLAG_NOT_TOUCHABLE). Suite
AGENTS completa en verde. Evidencia:
`docs/testing/evidence/AUDIT_2026-08-20_continuidad.txt`.
CIERRE FASE 13 (20-ago): objetivo por hora validado en $11/h, configurable y
persistente (10.0 → 11.0 en driver_config del dispositivo; 4 tests nuevos;
suite completa en verde; commit `test: validate configurable hourly target at
11` + push; HEAD==origin/main==`0732e85`, working tree limpio). FASE 10 sigue
PENDING. NO abrir WPs nuevos; esperando confirmación del reporte final.**

### Resultado de la auditoría (sin abrir WPs nuevos)

- [x] **FASE 1-9**: git limpio (HEAD==origin/main==`2aa02a0`); `ProfitEngine`
      casos A-D; métricas (`hasEnoughData`/`hasDistance`/`netGoal` MET|NEAR|
      FAILED); recomendación/confianza; `overlayContent` por-oferta con
      `MetricTone`; `OverlayConfig.activeIndicatorCount`; `ProfitState`;
      `OverlayPresentationMapperTest`; `ProfitEngineTest` 8 casos. VERIFICADO.
- [x] **FASE 6 (complemento tests)**: `OverlayConfigTest.kt` NUEVO (3 tests:
      excluye compactMode, cuenta solo indicadores activos, por defecto cuatro)
      + `OverlayPresentationMapperTest.kt` (+1 test "compactMode no altera los
      indicadores activos", 24 total). `OverlayConfig.kt:22-23` excluye
      compactMode. 0 fallos.
- [x] **FASE 10 (PENDING explícito)**: evidencia parcial — el parser extrae
      16.4 km del fixture real indriver_1; en ruta live por accesibilidad las
      ofertas reales registraron distance=0.0; el OCR leyó "28,7 kn" (unidad
      mal leída → regex km no matchea). Clasificar A-C requiere un dump a11y de
      una oferta real en vivo (no disponible hoy). NO se asume causa.
- [x] **FASE 11 (IMG-REPLAY-01)**: CLASIFICADO — dedup INTENCIONAL
      (`InMemoryCaptureFrameCache`, clave `contentHashCode`, MAX_ENTRIES=32;
      `DefaultCapturePipeline.kt:81`). PARSE_FALLIDO es mislabel del harness
      (motivo real DUPLICATE). Verificado en build actual: re-inyección →
      `snapshot=NULL reason=PARSE_FALLIDO (sin monto/plataforma extraíbles)
      state=WAITING parse=0.0ms`. Límite: force-stop limpia la cache.
- [x] **FASE 12**: copy obsoleto "datos simulados"/"cada 20 segundos"
      CORREGIDO en `DiagnosisScreen.kt` (KDoc + título "Última oferta
      evaluada" + texto vacío real). Verificado en pantalla; grep sin restos.
- [x] **FASE 13**: `DecisionThresholds.default()=(4.0,120.0)`; el 10.0 NO está
      en código main (es valor persistido del usuario); el motor es dinámico.
      **CERRADA (20-ago, autorizado)**: objetivo de validación ajustado a
      $11/h en `driver_config.minProfitPerHour` (persistido, integrity_check
      ok), configurable desde Settings y con cualquier valor (tests usan 15);
      +4 tests (SettingsViewModelTest x3: carga 11.0 / 11→15 persiste /
      recarga recupera 15; ProfitEngineTest x1: 13/h → MET con 11, NEAR con
      15). Commit `0732e85`.
- [x] **FASE 14**: suite completa en verde
      (ktlintCheck/lintDebug/assembleDebug/tests unitarios).
- [x] **FASE 15 (validación física, DEVICE-01)**:
  - OCR TEST build actual: indriver_1 $4.5/16.4km/13min → REJECT (profit −0.42);
    indriver_2 $4.5/0.0km/42min → WARNING (profit 3.0); uber_2 $25.53/99km/128min
    → WARNING (profit 3.41); uber_1/uber_3 → PANTALLA_NO_REQUEST. Latencia
    total 156-260 ms, eval 2.5-11.4 ms, overlay 12-21 ms (<3 s).
  - Dedup re-run verificado (snapshot=NULL).
  - Persistencia Room verificada (db+wal): overlay_config `1|1|1|0|0|1|90|45|50|4|500`,
    driver_config `|0.5|10.0`.
  - Settings toggles ida y vuelta: carga limpia 6/6 coinciden con DB (no hay
    bug de carga; el Switch Compose tiene touch target ≥48dp que excede sus
    bounds visuales); toggle "Mostrar ganancia por km" → Guardar → DB=1 →
    revertir → Guardar → DB=0. Restaurado.
  - `OverlayService.kt:247` FLAG_NOT_TOUCHABLE → no bloquea toques.
  - Estado final del dispositivo restaurado: overlay corriendo
    (isForeground=true), accesibilidad habilitada y bindeada, permiso Activo.
- [x] **FASE 16-17 (docs)**: este TASK.md + `.ai/CONTEXT.md` + evidencia
      `AUDIT_2026-08-20_continuidad.txt`. Cambios en repo (3): 2 tests + copy.
- [x] **FASE 18 (reporte final)**: VERIFICADO/PENDIENTES/DECISIONES abajo.
- [x] **FASE 19 (STOP)**: DETENERSE. Esperando confirmación.

### Siguiente (NO abierto)

- Continuar pendientes condicionados (fase 10 con dump a11y de oferta real en
  vivo; resto = validación E1a según `docs/testing/SPRINT_12_DEVICE_VALIDATION.md` §15).
- NO abrir Sprint 13 ni monetización (E1b) sin autorización explícita.

## Tarea anterior

**SPRINT 12 / WP-12-CALC-04 (19-ago-2026). COMPLETADO + VALIDADO en DEVICE-01
y en verde. Rentabilidad con TODOS los datos disponibles (monto y/o distancia
y/o duración) sin inventar métricas + overlay como SEMÁFORO SIN TEXTO (cada
dato con su color: verde=cumple objetivo, naranja=positivo, rojo=no genera;
ACCEPT solo con distancia Y duración Y ambas metas en verde). Decisión D17.4.**

### WP-12-CALC-04 — Implementación (TDD) + validación física

- [x] **Motor**: `TripOffer.hasEnoughData` = true con monto o distancia o
      duración (un monto solo ya es evaluable); `ProfitEngine` calcula
      `profitPerHour` con solo duración (antes null) y `profitPerKm` con solo
      distancia; `goalOf` por métrica (MET ≥ objetivo / NEAR > 0 / FAILED ≤ 0);
      `ProfitMetrics` con `netGoal`, `profitPerKmGoal`, `profitPerHourGoal`;
      lo no calculable queda null (nunca se inventa).
- [x] **Parser/extractores**: `OfferTextParser` y `PlatformExtractors` ya no
      descartan ofertas sin distancia; el fixture real indriver_2 (solo
      monto+duración) se evalúa.
- [x] **Overlay**: `OverlayPresentation`/`OverlayContent` sin línea secundaria;
      una celda por dato derivado (GANANCIA/POR HORA/POR KM) cada una con su
      propio `MetricTone`; banner REVISAR para WARNING; `AnimatedContent`
      booleano compacto/expandido. El objetivo horario NO se modifica
      (`minProfitPerHour=10.0`, regla).
- [x] **Tests TDD en verde** (FASE 8): `ProfitEngineTest`, `ProfitEvaluation
      EngineTest`, `RecommendationEngineTest`, `ConfidenceEngineTest`,
      `K1AmountRegressionTest` (extractores), `OverlayPresentationMapperTest`.
      `.\gradlew.bat ktlintCheck`, `lintDebug`, `assembleDebug`,
      `testDebugUnitTest`, `:domain:test`, `:core:platform:test`,
      `:feature:overlay:testDebugUnitTest` → BUILD SUCCESSFUL.
- [x] **Validación física DEVICE-01** (OCR TEST 00:15:5x, cache fresco +
      overlay corriendo; APK debug instalado): indriver_1 ($4.5/16.4 km/13 min)
      → REJECT "El viaje no cubre los costos (pierdes dinero)"; **indriver_2
      ($4.5/sin km/42 min) → WARNING "Ganancia/hora menor al objetivo"
      (profitPerHour=4.29/h calculado con solo duración)**; uber_2 ($25.53/99
      km/128 min) → WARNING; uber_1/uber_3 → PANTALLA_NO_REQUEST. DB
      `offer_history` ids 283-285 confirma la persistencia.
- [x] **Render del overlay verificado** (capturas `calc04_t1..t4.png`,
      extracción por OCR Windows + scan de píxeles): muestra **uber_2** =
      Uber · GANANCIA **$3.41 verde** (#1DB954, MET) · REVISAR · $25.53 ·
      POR HORA **$1.6/h naranja** (#F5A623, NEAR); 0 px rojo. Capturas
      posteriores a la corrida (00:16) → el overlay refleja el último estado
      evaluado (no hay bug de refresh; el flujo uiState→ventana funciona).
      TTL: la ventana se oculta tras `ttlSeconds` (`calc04_after_ttl.png`).
- [x] **Evidencia en repo**: `docs/testing/evidence/CALC04_offer_history.txt`,
      `CALC04_overlay_render.txt`. En dispositivo:
      `/sdcard/SIRC_TEST/images/calc04_*.png`.
- [x] **Hallazgo y corrección de entorno**: la ACCESIBILIDAD estaba
      DESACTIVADA en DEVICE-01 (`accessibility_enabled=0`) → la ruta real de
      captura estaba muda y uiautomator daba "null root node". Habilitada via
      adb (`settings put secure enabled_accessibility_services
      com.sirc.app/...CaptureAccessibilityService` + `accessibility_enabled=1`)
      y verificada (Bound services: Captura de ofertas SIRC,
      TYPE_WINDOW_STATE_CHANGED/CONTENT_CHANGED).
- [x] **Validación E2E REAL por accesibilidad (02:47-02:49)**: con InDrive
      abierto se capturaron OFERTAS REALES en pantalla
      (`origin=ACCESSIBILITY`): detección INDRIVE/REQUEST → snapshot →
      `PipelineOverlay: overlay mostrando: INDRIVE · $4.4/$4.7/$5.0 · WARNING`
      (eval 1.5-5.3 ms, overlay 3.9-24.6 ms, pipeline total 17-48 ms). DB ids
      286-291: $5.0/19 min → profitPerHour 11.05/h (MET, POR HORA verde);
      $4.4/19 min → 9.2/h (NEAR, naranja). Sin distancia no hay ACCEPT
      (REVISAR), GANANCIA/POR KM/COSTO EST. ocultas sin km (por diseño,
      probado). Render verificado en `calc04_e2e_real.png` (POR HORA $11.05/h
      VERDE, banner REVISAR, 0 rojo). Evidencia:
      `docs/testing/evidence/CALC04_E2E_real_capture.txt`.

**Siguiente (complemento validado):** commit + push de la evidencia E2E real
(accesibilidad habilitada + captura real de ofertas InDrive) y cierre.
NO abrir WP-12-UI-02, ni Sprint 13, ni monetización.

## Tarea anterior

### Auditoría POST-CALC-03 (sin código, entregable A–N)

- [x] **A. Compact mode**: NO tratado como indicador en conteo/layout; SÍ
  mezclado en la UI (6º toggle en la misma tarjeta). Hallazgo UI-CONFIG-01
  completo (causa, archivo/línea, comportamiento, impacto, propuesta, tests).
- [x] **B/C. Flujo de imágenes históricas**: DebugImageOcrReceiver
  (app/src/debug, action `com.sirc.debug.OCR_TEST`) lee assets/sirc_test (5
  JPG) → OCR + detección + `CaptureRequest(texts, imageData, origin=OCR)` →
  `pipeline.process`. Llega a evaluación+persistencia Room la 1ª vez. Cortes
  exactos identificados (dedup, overlay sin servicio, sesión del coordinador,
  guard snapshotInFlight; config no recomputa evaluación).
- [x] **D. Mecanismos**: captura real (AccessibilityCaptureInput con gate de
  plataformas + MediaProjectionCaptureInput + coordinador con sesión) vs
  receiver debug (sin gate, sin sesión, no arranca overlay). No existe otro
  mecanismo (SimulatedOverlayDataSource no existe; FakeParser eliminado).
- [x] **E. Inventario de indicadores**: banner decisión (showDecision) + 4
  celdas máx. (GANANCIA/POR HORA/POR KM/COSTO EST.) + resumen viaje en oferta
  (showTripSummary) + línea secundaria (no configurable). Opciones de
  presentación: compactMode, opacity, posición, TTL, límite historial.
  "máx. 4" sin enforcement; 0 indicadores = solo oferta+secundaria (sin
  validación).
- [x] **F. Confirmación**: compact mode es opción independiente de
  presentación; debe separarse visualmente de los indicadores.
- [x] **G. Competencia**: Ruta Rentable verificada (semáforo bueno/regular/
  malo por $/km y $/h; metas configurables; análisis por captura de pantalla;
  Ecuador incluido) — patrón "probar oferta desde imagen" es real en
  competidores. Fuentes registradas.
- [x] **H–M**: propuesta mínima en 2 WPs (UI-CONFIG-01 y DEBUG-REPLAY-01),
  tests, riesgos/regresiones, orden, archivos, criterios de aceptación.
- [x] **N. Docs**: solo este TASK.md; sin tocar código. DETENERSE.

### WP-12-CALC-02 (auditoría + propuesta, completada, autorizada)

- [x] **Origen de legacy demostrado**: `costPerMinute`/`costPerTrip` nacieron en
  el MVP (`787fa40`, costos manuales editables en Settings, evidencia DVC-01);
  FIX-03 derivó `costPerKm` y quitó esos editores de la UI pero D8.1 los
  mantuvo en el motor → legacy invisibles dominantes.
- [x] **Mapa de variables** completo (real/objetivo/derivado/legacy/ignorado/
  duplicado) y fórmulas literales.
- [x] **Propuesta económica**: única fuente de costo/km derivado (FIX-03 se
  mantiene); eliminar `costPerMinute` del costo real (su rol lo cubre el
  objetivo horario); `costPerTrip` → "costo fijo por viaje" editable default 0;
  jerarquía de decisión ACCEPT/WARNING/REJECT sin mezclar objetivo con costo;
  dato faltante (distancia/duración) sin cifras falsas + evaluación acotada
  por mejor caso.
- [x] **Tabla de 10 escenarios** con números de referencia (Quito/USD/$11/h/
  $0.50-km no constantes): caso real $5.90/27 min pasa de "pérdida −$11" a
  WARNING "gana, no confirmable sin distancia".
- [x] **Competidores verificados** (Ruta Rentable, Viaje Rentable, DecideRider,
  GigU, Motorista One): estado intermedio "gana pero bajo meta" es el patrón;
  ninguno etiqueta pérdida donde hay ganancia.
- [x] **Impacto** en decisión/Settings/tests/archivos + plan mínimo de
  implementación + riesgos de migración.
- [x] **8 decisiones (Q1-Q8) autorizadas** para el LOOP de implementación.

## Tarea anterior — WP-12-UI-01 (completada)

Overlay rediseñado con jerarquía de 4 niveles (mapper puro `OverlayPresentation`,
22 tests JVM, validado en físico con REJECT/WARNING reales, commit `2b09a72`,
decisión D17.2). Detalle en `.ai/CONTEXT.md`, `.ai/DECISIONS.md`, ROADMAP,
plan WP-12-UI-01 §A–E.

## Tarea anterior — Cierre formal Sprint 12 / E1a (completada)

**LOOP ENGINEERING — CIERRE FORMAL DEL SPRINT 12 / E1a (18-ago-2026).
AUDITORÍA + CONSOLIDACIÓN + DOCUMENTACIÓN (sin código). E1a = PASS WITH
PENDING: núcleo validado en físico (InDrive Ecuador E2E real); P0/Alta
(DVC-01/03/04, K1) corregidos y verificados; pendientes condicionados a
cuentas reales/ruta/batería/ciclo de vida/DVC-02. No se abre Sprint 13 ni
monetización (E1b). Matriz consolidada en
`docs/testing/SPRINT_12_DEVICE_VALIDATION.md` §15.**

### CIERRE DEL SPRINT 12 / E1a (autorizado)

- [x] **Auditoría de cierre ejecutada (18-ago-2026)** — revisión del estado
  real del repo (git limpio, HEAD == origin/main == 7bea473), cruce de
  fuentes (SPRINT_12_DEVICE_VALIDATION, plan FIX-01…05, TASK, CONTEXT,
  ROADMAP, DECISIONS, BETA_READINESS) y verificación de la evidencia física
  en DEVICE-01 (`/sdcard/SIRC_TEST/`): `e2e_pipeline.log`, `e2e_final.log`
  (log "overlay mostrando: INDRIVE · $2.9/$3.1 · REJECT", 18-ago 16:37),
  `offer_history.txt` (74 ofertas reales), `fix03_evidence.txt` (config
  derivada persistida). Evidencia FIX-01 en repo parcial
  (`DEVICE-01_overlay_window.txt` sin dump del banner) → registrado (P2).
- [x] **Estado real de E1a decidido: PASS WITH PENDING** (evidencia, no
  deseado). Criterios de salida completos NO cumplidos (multi-plataforma,
  muestra ≥20, jornada en ruta, batería, ciclo de vida, DVC-02).
- [x] **Docs de cierre actualizadas**: `SPRINT_12_DEVICE_VALIDATION.md` §15
  (matriz consolidada + evidencia + hallazgos abiertos), plan (resultados
  FIX-01/FIX-04 + §3 cierre), ROADMAP (estado Sprint 12), `.ai/CONTEXT.md`,
  `.ai/DECISIONS.md` (D17.1).
- [x] Reporte final A-Z entregado. DETENERSE: esperando autorización.

### PROGRESO DE CORRECCIONES (autorizado)

- **[x] WP-12-FIX-05 — Higiene de artefactos COMPLETADO (18-ago-2026).**
  Sin cambios de código de producción (solo device storage + docs). Convención
  vigente: todo artefacto de prueba bajo `/sdcard/SIRC_TEST/{images,logs,
  evidence,exports,tmp}/`. Se confirmó que `/sdcard/sirc_test` y
  `/sdcard/SIRC_TEST` son la MISMA carpeta (sdcard case-insensitive; sin
  duplicados). Catálogo previo: 52 archivos en raíz de SIRC_TEST + `fix03/` (2)
  + 3 sueltos en `/sdcard` (`sirc5.xml`, `sirc_home.xml`, `sirc_overlay.png`).
  Reubicados **57 archivos** sin borrar nada: 14 → `images/`, 2 → `logs/`,
  3 → `evidence/` (+`fix03/` → `evidence/fix03/`), 38 → `tmp/`. Raíz `/sdcard`
  limpia de `sirc_*` sueltos. No se tocaron archivos personales
  (`find` Download/DCIM/Pictures/Documents/Movies/Music/Android → 0
  coincidencias `sirc`). Integridad verificada: 57 antes = 57 después, tamaños
  intactos, evidencias clave legibles. Al terminar Sprint 12 basta borrar la
  única carpeta `/sdcard/SIRC_TEST/`. Detalle en plan §FIX-05 "N. Resultado".

- **[x] WP-12-FIX-03 — Config editable post-onboarding (DVC-01) COMPLETADO y VERIFICADO en DEVICE-01.**
  Settings ahora edita TODO lo que el onboarding persiste (perfil, vehículo, combustible,
  mantenimiento, otros costos, plataformas, umbrales, overlay) con persistencia real en Room y
  efecto real en el motor. Diseño (Opción A, única fuente de verdad): **costPerKm es DERIVADO**
  (`fuelPrice/consumptionKmPerUnit + maintenanceCostPerKm + Σ additionalCosts.costPerKm`); la UI
  lo muestra como "Costo por km (calculado)" SOLO lectura y edita sus componentes; el motor ignora
  `costs.costPerKm` manual; al guardar `normalizeCostPerKm()` persiste la columna con el derivado.
  Cambios: `SettingsViewModel.kt` reescrito (UiState con `derivedCostPerKm` + `reloadTick` +
  `persistedConfig`; `togglePlatform`; `discard()`; `save()` normalizado); `SettingsScreen.kt`
  reescrito (secciones Perfil/Vehículo/Costos/Plataformas/Umbrales/Overlay + botones
  Guardar/Descartar); `AccessibilityCaptureInput.kt` con **gate de plataformas** (solo procesa
  ofertas de plataformas activas; set vacío = acepta todas, fallback anti-regresión; logs info de
  rechazo). Tests nuevos: `SettingsViewModelTest` (6), `AccessibilityCaptureInputTest` (3),
  `ProfitEvaluationEngineTest` (+3 derivación/manual ignorado/decisión). **2 bugs reales
  encontrados en dispositivo y corregidos**: (1) campos `rememberSaveable` no se re-sembraban al
  cargar la config persistida (el campo "Año" mostraba 2020 con BD=2021 → habría corrupto datos al
  guardar) → fix con `reloadTick` como resetKey; (2) crash
  `IllegalArgumentException: Parcel: unknown type for value CostDraft` → `costDrafts` pasa de
  `rememberSaveable` a `remember(reloadTick)`. Validación física (Infinix X6850, Android 15):
  derivado 0.5417 → editar combustible 0.5→1.5 → **0.625 en vivo**; añadir costo "Peaje"/0.3 →
  **0.925 en vivo**; activar Cabify; ciudad Quito→Guayaquil; guardar → BD
  `costPerKm=0.925` (normalizado al derivado), `fuelPrice=1.5`, `additionalCosts='Peaje^_0.3'`,
  `platforms='CABIFY,INDRIVE,UBER'`, `city=Guayaquil`; tras force-stop/reopen TODO persiste
  (0.925, Peaje 0.3, Guayaquil, 4 chips de plataformas). Suite AGENTS completa en verde.
  Evidencia: `/sdcard/SIRC_TEST/fix03/`.

- **[x] WP-12-FIX-02 — Captura E2E (DVC-04) COMPLETADO y VERIFICADO en DEVICE-01.**
  Flujo real demostrado en físico: Accessibility/MediaProjection → OCR → detección →
  parser → evaluación → **overlay**, con latencias reales y evidencia en `/sdcard/SIRC_TEST/`.
  Causa raíz de la ruta silenciosa: el servicio SÍ estaba bindeado/suscrito, pero
  `android:packageNames` del config XML filtraba a nivel sistema los eventos del paquete
  real de InDrive en Ecuador — **`sinet.startup.inDriver`** (hallazgo clave para soporte
  InDrive Ecuador; el dispositivo NO tiene `com.ubercab.driver`, solo `com.ubercab`
  pasajero); además el código no logueaba rechazos.
  Cambios: config XML `packageNames` ampliado (incluye `sinet.startup.inDriver`);
  `PlatformDescriptors.kt` → UBER e INDRIVE con sus `packageNames`; instrumentación con
  `SircLogger` en `AccessibilityCaptureInput` (info/warn de rechazos, schedule),
  `DebounceCaptureScheduler` (logger obligatorio, log al encolar/emitir),
  `DefaultCapturePipeline` (info de request/detección/snapshot con ms),
  `MediaProjectionCaptureInput` (info enriquece/degrada) y `PipelineOverlayDataSource`
  (info "overlay mostrando: …" con métricas de eval/reglas/overlay).
  Tests nuevos: `DebounceCaptureSchedulerTest` (+1 logging; los 3 existentes pasan
  `TestLogger()` por la trampa Hilt de parámetro por defecto) y `PlatformDetectionEngineTest`
  (+2: seed resuelve `sinet.startup.inDriver`→INDRIVE y `com.ubercab.driver`→UBER).
  Evidencia en físico (18-ago): `detección: INDRIVE / REQUEST`, `snapshot INDRIVE guardado:
  parse 5.7–13.9 ms · total 24.7–40.9 ms` (detección 16.2–28.0 ms), **9+3 snapshots reales**;
  **`PipelineOverlay: overlay mostrando: INDRIVE · $2.9/$3.1 · REJECT (origen=ACCESSIBILITY
  · eval 1.3–8.6 ms · reglas 0.0–0.7 ms · overlay 8.8–15.6 ms)`**; ventana overlay
  `Requested 885x280 · isVisible=true · HAS_DRAWN`; Room `offer_history` con 74 ofertas
  reales (id 72–74 = ofertas de hoy 16:37; id 61–71 = ACCEPT/WARNING de ayer 20:39–20:41).
  MediaProjection: **1 frame real enriquecido (300759 bytes PNG)** en la corrida previa
  (OCR ~2.5 s sobre frame completo; pantalla HOME sin oferta); tras denegación
  (`PROJECT_MEDIA=ignore`) se degrada a textos (camino principal). Uber (pasajero):
  `UBER / HOME`, "sin oferta parseable" (correcto). Verificación completa en verde
  (ktlintCheck, lintDebug, assembleDebug, tests JVM + `:core:capture:android`).
  Evidencia: `/sdcard/SIRC_TEST/` (e2e_pipeline.log, e2e_final.log, offer_history.txt,
  debug_panel_offer*.png, README.txt). Hallazgos registrados para el siguiente WP:
  panel Debug/estadísticas con sesión en memoria (se pierde al reiniciar SIRC);
  uiautomator "null root node" con SIRC tras ciclo de overlay; OCR ~2.5 s en frames
  MediaProjection completos.

- **[x] WP-12-FIX-01 — Overlay físico (DVC-03) COMPLETADO y VERIFICADO en DEVICE-01.**
  Causa raíz confirmada en campo: la ventana SÍ se añadía, pero era transparente/vacía
  (`visibleFor(DISABLED,null)=false` → `OverlayContent.kt:59` no compone → `FLAG_NOT_TOUCHABLE`);
  `PipelineOverlayDataSource.start()` era no-op (el pipeline nunca salía de DISABLED sin una
  oferta real visible); `runCatching` mudos en `addView`/`updateViewLayout` y `_isRunning` optimista
  (la UI podía decir "Activo" con el servicio muerto, p. ej. tras ser matado por XOS).
  Cambios:
  - `PipelineOverlayDataSource.start()` → `status=WAITING` + `visible=true` (indicador real
    "Esperando oferta…", sin datos simulados; la evaluación real sigue mostrándose al llegar).
  - `OverlayService` inyecta `SircLogger` + `OverlayController`; `ensureOverlay(): Boolean` con
    `logger.error` + `stopSelf()` + `START_NOT_STICKY` si `addView` falla (estado de error visible,
    la UI refleja "no en ejecución"); log diagnóstico en `onStartCommand`; `onDestroy` reporta
    `onServiceRunning(false)`. Sin `runCatching` mudos (los `updateViewLayout` pasan a log warn).
  - `OverlayController` mantiene feedback inmediato en `start()`/`stop()` y añade
    `onServiceRunning()` para corregir `isRunning` con el estado real del servicio (S-S23):
    si el sistema termina el FGS, la UI deja de decir "en ejecución".
  - `OverlayContent` sin `fillMaxSize()` → la ventana es un banner (WRAP_CONTENT) y NO bloquea
    los toques sobre la app (antes, visible+touchable ocupaba toda la pantalla y tragaba los taps).
  - Extracción testable: `OverlayServiceLauncher`/`AndroidOverlayServiceLauncher` (+ binding en
    `OverlayModule`) para que `OverlayController` sea 100 % testeable sin Android.
  Tests: `OverlayControllerTest.kt` (nuevo) + `PipelineOverlayDataSourceTest.kt` ampliado
  (start→WAITING/visible, flag OVERLAY off, start+stop). Verificación completa en verde.
  Evidencia device (Infinix X6850): `dumpsys activity services` muestra `OverlayService` corriendo;
  `dumpsys window` muestra la ventana (`ty=APPLICATION_OVERLAY`, `Requested 885x223`, `isVisible=true`,
  `Surface shown`, `HAS_DRAWN`); UI "Overlay en ejecución" pasa a "Activo"/"Inactivo" reales;
  "Detener overlay" funciona (sin bloqueo de toques); logcat sin errores. La ventana es ahora un
  banner pequeño que no interfiere con la app de la plataforma.

- **[x] WP-12-FIX-04 — Parser de monto (K1) COMPLETADO y VERIFICADO en verde.**
  Antes 479.0 / 5.0 / 90.0; ahora 4.5 / 4.5 / 25.53 (K1 3/3, dataset real de
  DEVICE-01). Cambios: `OfferTextParser.kt` (exige marcador de moneda en
  `AMOUNT_RUN`, rechaza ceros a la izquierda tipo `$090`, recorta separadores
  colgantes en `parseAmount`, dedupe conserva el contexto más rico),
  `PlatformExtractors.kt` (score usa `hasCurrencyMarker`; eliminado el fallback
  `maxByOrNull`), `PlatformDescriptors.kt` (keyword "aceptar" en INDRIVE).
  Regresión: `K1AmountRegressionTest.kt` (nuevo, fixtures reales del dump).
  Verificación: ktlintCheck + lintDebug + assembleDebug + tests JVM en verde
  (el único fallo puntual `PipelineOverlayDataSourceTest` re-ejecutado en
  aislamiento → PASS, flaky de timing no relacionado).

### Lo que FUNCIONA en DEVICE-01 (evidencia física)

- **OCR real** en el teléfono: lee bien montos/duración/distancia ($4,50, USD4.5, $25.53…).
- **Detección**: PACKAGE_MATCH 5/5 con `packageName` real; sin él → AMBIGUOUS (PLT‑5/K2).
- **Pantallas no-oferta**: uber_1 y uber_3 → NULL (no fuerza). Correcto.
- **Instalación** (`adb install -r`) + apertura + **accesibilidad habilitada y bindeada** (logcat).
- **Latencia pipeline real**: 175–317 ms (<1 s objetivo UX; overlay/UI sin medir).

### Lo que NO funciona (registrado, NO corregido)

- **~~K1 — Parser monto FAIL 0/3~~** ✅ **CORREGIDO** en FIX-04: 479.0 / 5.0 / 90.0
  → 4.5 / 4.5 / 25.53 (ver "PROGRESO DE CORRECCIONES").
- **~~DVC-01 (FAIL, Alta)~~** ✅ **CORREGIDO** en FIX-03 — config post-onboarding editable:
  Settings edita TODO lo persistido (perfil, vehículo, combustible, mantenimiento, otros costos,
  plataformas, umbrales, overlay) con persistencia real en Room y efecto real en el motor
  (costPerKm derivado, gate de plataformas en captura). Validado en físico (ver "PROGRESO DE
  CORRECCIONES").
- **DVC-02 (FAIL/INSUFFICIENT_EVIDENCE, Alta)** — captura de pantalla: `createScreenCaptureIntent()` sin config (single-app vs full lo decide el sistema); SIRC no persiste selección; `appops PROJECT_MEDIA = ignore` (rejectTime) y `dumpsys media_projection` vacío en el físico.
- **~~DVC-03 (FAIL, P0)~~** ✅ **CORREGIDO** en FIX-01 — overlay visible en DEVICE-01:
  la ventana se añadía pero era transparente/vacía (pipeline DISABLED → `visibleFor=false` →
  `OverlayContent` vacío); `start()` no-op; `runCatching` mudos; `_isRunning` optimista. Ahora el
  overlay muestra "Esperando oferta…" al activarlo, `isRunning` es real, los fallos de ventana se
  loguean y el banner no bloquea toques (ver "PROGRESO DE CORRECCIONES").
- **~~DVC-04 (INSUFFICIENT_EVIDENCE, P0)~~** ✅ **CORREGIDO** en FIX-02 — flujo normal
  (accesibilidad/MediaProjection → OCR → overlay) DEMOSTRADO en físico con ofertas reales
  de InDrive Ecuador: la causa de la ruta silenciosa era `android:packageNames` filtrando
  los eventos del paquete real (`sinet.startup.inDriver`); ahora con log instrumentado se
  ve `REQUEST → snapshot → overlay mostrando` (ver "PROGRESO DE CORRECCIONES").

### Evidencia

- `docs/testing/evidence/SIRC_OCR_TEST_logcat_dump.txt` (OCR, 136 líneas).
- `docs/testing/evidence/DVC_diagnostics_logcat_dump.txt` (diagnóstico DVC-01…04: accesibilidad, ventanas, servicios, appops, media_projection).
- Documento: `docs/testing/SPRINT_12_DEVICE_VALIDATION.md` §3, §6.2–6.4, §7–14.

### Próximos pasos (NO abiertos — esperar autorización)

1. **Sprint 12 / E1a CERRADO (PASS WITH PENDING, 18-ago-2026)**. Para
   completar E1a (validación total) se requieren condiciones del usuario:
   cuenta real de Uber Driver/DiDi/Cabify (validación en vivo), jornada en
   ruta (<1 s cronometrado, estabilidad 8 h, batería), más dispositivos
   (mínimo 2), muestra ≥20 por plataforma. Pendiente de decisión: DVC-02
   (fuente de captura), mecanismo debug (§6.3), calidad de evidencia FIX-01.
2. No abrir Sprint 13 ni monetización (E1b, Supabase, Billing, Play
   Integrity, trial, AHU, anti-fatiga) sin autorización explícita.

## Tarea anterior

**LOOP ENGINEERING — MODELO COMERCIAL: TRIAL 14 DÍAS + SUSCRIPCIÓN
WEEKLY/MONTHLY/ANNUAL + PRECIOS INTERNACIONALES (16-ago-2026). Solo
documentación; sin código.**
Se definió el modelo comercial definitivo (descarga gratuita + cuenta + trial
Premium completo de 14 días → suscripción), reemplazando el modelo FREE anterior.

Decisiones registradas (ver `.ai/DECISIONS.md` D16.1–D16.6):

- **D16.1 — Modelo comercial Trial → Premium**: `FREE_TRIAL = 14 DAYS`,
  `TRIAL_ACCESS = FULL_PREMIUM`, `POST_TRIAL = SUBSCRIPTION_REQUIRED`; **sin
  Free Premium permanente** (`FREE_LIMITS` eliminado; D15.1/D15.2 superadas).
- **D16.2 — Trial Premium completo de 14 días** (adquisición + validación),
  controlado server-side con anti-abuso (reinstalación/borrado/cambio de
  dispositivo/múltiples cuentas/reloj manipulado).
- **D16.3 — Suscripciones Weekly / Monthly / Annual** (anual con ahorro claro;
  descuentos concretos por decisión posterior).
- **D16.4 — USD como referencia de pricing + regionalización por Google Play**
  (sin conversión manual, sin reloj local; Play = autoridad comercial).
- **D16.5 — Pricing evolutivo ligado al valor agregado + grandfathering**
  (no fijar precios aún; matriz de decisión §5bis).
- **D16.6 — Cuenta obligatoria** para controlar trial y entitlement.

Entregables:

- **`docs/SUBSCRIPTION_MODEL.md`** — §1 modelo definitivo Trial→Suscripción,
  §2 planes Weekly/Monthly/Annual, §2ter moneda USD + regionalización, §3
  entitlement por estados (`TRIAL_ACTIVE`…`ACCOUNT_UNKNOWN`), §5bis pricing
  evolutivo + matriz de decisión.
- **`docs/BACKEND_ARCHITECTURE.md`** — planes `sirc_weekly/monthly/annual`,
  tabla `trial` (anti-abuso §2.7), entitlement con `state` conceptual, cuenta.
- **`docs/SECURITY_MODEL.md`** — §5.5 seguridad Trial→Suscripción, §6.1bis
  trial anti-abuso, estados y offline; v3.
- **`docs/PRODUCT_STRATEGY.md`** — pilar <1 s (UX), P1/P1bis trial, roadmap
  con E1b (cuenta+trial+suscripción) sin etapa FREE.
- **`docs/PRODUCT_COMPETITIVE_ANALYSIS.md`** — DecideRider (CLP $3.490/mes,
  trial ~14 días) verificado; Ruta Rentable (trial ~3 días, precio no
  publicado); formato `VERIFIED`/`SOURCE`; §5 precios y trials.
- **`docs/ROADMAP.md`**, **`docs/PROJECT.md`**, **`docs/ARCHITECTURE.md`** —
  objetivo <1 s (UX) / <3 s (E2E), E1b con trial y suscripción, sin etapa FREE.
- **`.ai/RULES.md`** — reglas 9j (trial→suscripción), 9k (precios USD/Play),
  9l (entitlement + <1 s), 9m (trial anti-abuso). `.ai/CONTEXT.md` y
  `.ai/DECISIONS.md` (D16.1–D16.6) actualizados.

**Siguiente: (en curso)** verificar `git status`, commit (solo docs) + push y
entregar reporte final del LOOP (A–R). No implementar cuenta/trial/backend/
Billing/paywall/UI (regla R16/9f/9i/9j). No iniciar el siguiente Sprint sin
autorización.

## Tarea anterior

**LOOP ENGINEERING — MODELO FREE + SUPABASE ACCOUNT GATE (16-ago-2026). Solo
documentación; sin código.**
Se definió el modelo de adquisición inicial (descarga gratuita + cuenta + plan
FREE), el proceso de configuración del backend (Supabase Account Gate) y las
reglas de secretos, eliminando los niveles intermedios propuestos.

Decisiones registradas (ver `.ai/DECISIONS.md` D15.1–D15.6):

- **D15.1 — Descarga gratuita + SIRC FREE**: la fase inicial es descarga
  gratuita + cuenta gratuita + plan FREE (entitlement `FREE` server-side, 0 €);
  monetización Premium **progresiva (E3)**, sobre base validada. Planes
  activos **FREE → PREMIUM** (niveles intermedios Basic/Pro retirados).
- **D15.2 — `FREE_INITIAL_MODEL = ENABLED`, `FREE_LIMITS = TBD`**: NO se
  interpreta "dos 3 free" ni se inventa límite; se fijará por decisión explícita
  posterior con datos de la beta.
- **D15.3 — El Free NO relaja seguridad**: entitlement `FREE` server-side,
  revocable; manipular el APK no da premium indefinido.
- **D15.4 — Supabase ACCOUNT GATE**: si se necesitan credenciales reales de
  Supabase → **DETENERSE y pedir configuración al usuario** (guía §0.1). Nunca
  inventar credenciales ni crear cuentas. Dev local/tests en verde sin backend.
- **D15.5 — Secretos client-safe vs server-only**: en el APK solo Project URL +
  publishable key; service_role/service account/keystore/secretos JAMÁS en
  git/GitHub/APK/chat.
- **D15.6 — Arquitectura de cuenta**: identidad → profile → suscripción/
  entitlement → caché local firmado (TTL) → gate; backend aislado tras
  `AuthRepository`/`EntitlementRepository` (`:domain`).

Entregables:

- **`docs/SUBSCRIPTION_MODEL.md`** — §1 Modelo comercial inicial (descarga
  gratuita + plan FREE) y §2 estructura **FREE → PREMIUM** reescrita; se
  retiran los niveles Basic/Pro; `FREE_LIMITS = TBD`.
- **`docs/BACKEND_ARCHITECTURE.md`** — §0 Supabase ACCOUNT GATE + guía de
  configuración (§0.1); §2.5 secretos client-safe/server-only; §2.6 dev local
  sin backend; §3 arquitectura de cuenta; planes con `sirc_free` y entitlement
  con `tier` FREE/PREMIUM.
- **`docs/SECURITY_MODEL.md`** — §5.5 Seguridad del Free (D15.3); entitlement
  FREE/PREMIUM server-side; nota offline del tier FREE.
- **`docs/PRODUCT_STRATEGY.md`** — P1bis (fase inicial de adquisición) y etapa
  FREE/BETA ABIERTA en el roadmap; P1/E1b ref.

**Siguiente: (en curso)** verificar `git status`, commit (solo docs) + push y
entregar reporte final del LOOP (A–N). No implementar Free/backend/E1b sin abrir
la tarea (regla R16/9f/9i).

## Tarea anterior

**LOOP ENGINEERING — BACKEND SUPABASE + EVALUACIÓN ANTIGRAVITY + ARQUITECTURA
DE MONETIZACIÓN (16-ago-2026). Solo documentación; sin código.**
Se decidió el backend inicial, el modelo de suscripción, se amplió el threat
model y se redefinió el rol de las herramientas de agente.

Decisiones registradas (ver `.ai/DECISIONS.md` D14.1–D14.4):

- **D14.1 — Supabase como backend inicial**: Auth + RLS + Edge Functions +
  Postgres para identidad/suscripción/entitlement; camino crítico de oferta
  100 % local; plan **Pro** en producción (Free pausa proyectos); sin Realtime
  ni Storage; ninguna oferta/pantalla se sube.
- **D14.2 — Play API v2**: verificación server-side con
  `purchases.subscriptionsv2.get` (la `subscriptions.get` está **deprecada**);
  RTDN = señal (re-consultar API, dedupe `messageId`, JWT OIDC del push).
- **D14.3 — Entitlement + offline**: TTL 24–72 h (S2); source of truth por
  capas (Play=transacción, Backend=operativa, Supabase=persistencia,
  Cliente=caché); threat model ampliado a **T15–T20**.
- **D14.4 — Herramientas**: **OpenCode principal + Antigravity complementario**
  (no sustituye); regla **R17** (prohibido doble-agente simultáneo en el mismo
  branch).

Entregables:

- **`docs/BACKEND_ARCHITECTURE.md`** (nuevo) — Supabase (Auth/RLS/Edge
  Functions/secrets), modelo de datos, flujo Play Billing→backend, RTDN, source
  of truth, offline, T15–T20, privacidad.
- **`docs/SUBSCRIPTION_MODEL.md`** (nuevo) — estructura conceptual de planes,
  matriz de precios de competencia verificada, entitlement, lifecycle/estados.
- **`docs/ANTIGRAVITY_EVALUATION.md`** (nuevo) — estado Antigravity y decisión
  OPCIÓN C.
- **`docs/SECURITY_MODEL.md`** (v1→v2) — T15–T20, API de Play v2, Supabase.
- **`docs/PRODUCT_STRATEGY.md`**, **`docs/ROADMAP.md`**, **`.ai/RULES.md`**
  (9g/9h/R17), **`.ai/CONTEXT.md`**, **`.ai/DECISIONS.md`** (D14.1–D14.4),
  **`docs/ARCHITECTURE.md`**, **`docs/PROJECT.md`** — actualizados. Commit
  `2ec1f21` + push.

## Tarea anterior

**WP-E3-05E completado** — Limpieza documental final del Sprint 11 derivada
exclusivamente de los refactors WP-E3-02 → WP-E3-05D. Solo se corrigió
documentación que describía el estado actual como si las piezas eliminadas
siguieran existiendo; los registros históricos (decisiones, auditorías,
informes por sprint, roadmap, specs) se conservan íntegros. Sin cambios de
código ni de comportamiento. Commit único + informe final entregado.

- **`docs/ARCHITECTURE.md`**: diagramas y listas de `:core:capture` y
  `:core:capture:android` actualizados a la arquitectura real (pipeline
  `CaptureInput → OCR → detección → parser`, `CaptureInputType`, estados
  `WAITING/PROCESSING/ERROR`, `MediaProjectionCaptureInput`, `CaptureAndroidModule`
  sin `ScreenCapture`); diagrama de análisis sin `RuleEngine`; tabla de
  decisiones sin `RuleEngine`/`ScreenCapture`/`FakeParser`/`CAPTURING`/
  flags `RULES`/`METRICS`/`ACCESSIBILITY`.
- **`.ai/CONTEXT.md`**: flujo real (paso 4) sin `RuleEngine` (explica el
  `RuleEvaluation` vacío por compat UI); resumen de arquitectura sin
  `RuleEngine`/`OfferValidator`/`RuleThresholds`/`RuleContext`/`OfferRule`/
  `ValidationResult`/parsers especializados.
- **`.ai/AGENTS.md`**: el rol Accessibility Engineer es dueño del
  `CaptureAccessibilityService` (no `SircAccessibilityService`).
- **`docs/KNOWN_ISSUES.md`**: servicio único de accesibilidad (WP-E1-03);
  terminología de parsers actualizada; backlog sin "un solo servicio".
- **`docs/CHANGELOG.md`**: entradas WP-E3-05B, 05C, 05D y 05E añadidas.

**Siguiente: (pausa)** se entrega informe final para aprobación. No se inicia
otro WP tras WP-E3-05E sin aprobación explícita.

## Antecedentes

- **WP-E3-03 completado** (commit `f1675fb`): Unified Capture Source. Pipeline
  único `CaptureInput → CaptureRequest → (OCR) → PlatformDetectionEngine →
  OfferParserOrchestrator → OfferSnapshot → Repository → Overlay`. Se eliminaron
  `ScreenCapture`/`ScreenFrame`/`MediaProjectionScreenCapture` y la resolución
  de plataforma duplicada del coordinador; `DetectionOrigin` → `CaptureInputType`.
- **WP-E3-02 completado** (`ced6249`): framework genérico de detección
  (`PlatformDetectionEngine`/`DetectionMatcher`/`DetectionResult`).
- **WP-E3-01 completado** (`a79c55a`): motor descriptor-driven
  (`PlatformDescriptor`/`PlatformDescriptorRegistry`).

## Progreso WP-E3-04 (auditoría)

- [x] Documento `docs/audit/architecture/ARCHITECTURE_AUDIT_SPRINT11.md`
      redactado, verificado y **aprobado** por el usuario (29 hallazgos).
- [x] Commit `93a1b57` (auditoría) + `f185d85` (TASK).

## Progreso WP-E3-05A (severidad Alta)

- [x] **A-1**: eliminados los overloads `parse(texts, ts, RidePlatform)` y
      `parse(texts, ts, packageName)` y la instancia interna de
      `PlatformDetectionEngine` en `OfferParserOrchestrator`; único camino
      `parse(result, texts, ts, detectionMillis)`. `OfferParserOrchestratorTest`
      migrado (14 escenarios) a la API definitiva.
- [x] **A-3**: `OfferCaptureCoordinator` y `AccessibilityCaptureInput` inyectan
      `PlatformDetectionEngine` (única fuente de resolución); 
      `RidePlatform.fromPackageName` deprecado con `@Deprecated`;
      `OfferCaptureCoordinatorTest` actualizado.
- [x] **A-2**: corregidos `.ai/CONTEXT.md`, `.ai/DECISIONS.md` (D11.14) y
      `docs/CHANGELOG.md` — `PlatformDescriptors` se conserva como fuente de
      descriptores (solo se eliminaron `SpecializedParsers.kt` y
      `ExtractorRegistry`).
- [x] Verificación completa en verde (ktlintCheck, lintDebug, assembleDebug,
      tests unitarios JVM + instrumentados).
- [x] Docs del WP actualizadas (CHANGELOG WP-E3-05A, DECISIONS D11.14, TASK).

## Progreso WP-E3-05B (severidad Media)

- [x] **M-1**: eliminados `EvaluateOfferUseCase.kt` y `AddOfferHistoryUseCase`;
      recortados `SaveDriverConfigUseCase` (solo `save(config)`) y
      `GetDriverConfigUseCase` (solo `observeDriverConfig`/`observeIsConfigured`);
      `DriverConfigRepository` pasa de 10 a 4 métodos, con su impl
      `DefaultDriverConfigRepository` y el fake de `PipelineOverlayDataSourceTest`
      alineados.
- [x] **M-2**: eliminada la interfaz `PlatformExtractor` (YAGNI);
      `GenericPlatformExtractor` es la clase concreta única; KDoc de
      `OfferTextParser` corregido.
- [x] **M-3**: eliminados `OfferTypeVariant.refine` y la rama muerta en
      `GenericOfferTypeParser`.
- [x] **M-4**: eliminados `FeatureFlag.ACCESSIBILITY` y `FeatureFlag.METRICS`
      (el Debug Panel los listaba por iteración → desaparecen solos).
- [x] **M-5**: eliminado `CaptureMetrics.onCapture` (interfaz +
      `DebugCaptureMetrics` + test double `RecordingCaptureMetrics`).
- [x] **M-6 (parcial, decisión usuario)**: eliminados `ParsedOffer.parsingMillis`
      (más el timing muerto del orquestador) y `ScreenDetection.matchedKeywords`;
      **conservados** `DetectionResult.origin/candidates/sourcePackage`
      (diagnóstico para futuras fuentes de captura y Debug Panel).
- [x] Verificación completa en verde (ktlintCheck, lintDebug, assembleDebug,
      tests).
- [x] Docs descriptivas (CHANGELOG/DECISIONS/CONTEXT) → diferidas a un WP
      posterior (regla: no tocar documentación en 05B).

## Progreso WP-E3-05C (severidad Media restante)

- [x] **M-7**: eliminados `CaptureInputType.SHARE/GALLERY/TEST` (se mantienen
      `UNKNOWN/ACCESSIBILITY/MEDIA_PROJECTION/OCR/PACKAGE`).
- [x] **M-8**: eliminado el bundle LEGACY de reglas (`RuleEngine`, 6 reglas,
      `OfferValidator`, `ValidationResult`, `ValidationIssue`, `RuleContext`,
      `RuleThresholds`, `OfferRule` y helpers/tests); se conserva solo la API
      viva del overlay (`RuleEvaluation`, `RuleResult`, `RuleVerdict`);
      eliminados `resultFor()` y `TripOffer.pickupDistanceKm`.
- [x] **M-9**: `defaultCurrency` obligatorio no nulo en
      `GenericPlatformExtractor`; eliminado `DEFAULT_CURRENCY`.
- [x] Verificación completa en verde; commit `c1f57c4`.

## Progreso WP-E3-05D (severidad Baja)

- [x] **B-1/B-3/B-4/B-6/B-7/B-8** resueltos; **B-2/B-5/B-9** conservados con
      justificación; **B-10** verificado (detalle en "Tarea actual" histórica).
- [x] Verificación completa en verde; commit `008e792` + informe final.

## Progreso WP-E3-05E (limpieza documental)

- [x] Búsqueda repo-wide de referencias obsoletas y clasificación (histórica /
      actual-incorrecta / muerta).
- [x] Corregidos `docs/ARCHITECTURE.md`, `.ai/CONTEXT.md`, `.ai/AGENTS.md`,
      `docs/KNOWN_ISSUES.md`; entradas WP-E3-05B/C/D/E en `docs/CHANGELOG.md`.
- [x] Sin cambios de código de producción (verificado con `git diff`).
- [x] Commit único + informe final; **pausa** esperando aprobación.

## Hallazgos clave (resumen)

- **Alto (3)**: A-1 overloads de parseo test-only con detección paralela en
  `OfferParserOrchestrator` (2ª instancia de `PlatformDetectionEngine`);
  A-2 docs internas contradicen el código (`PlatformDescriptors` NO fue
  eliminado, es el seed de producción); A-3 `RidePlatform.fromPackageName`
  paralelo a la detección descriptor-driven.
- **Medio (9)**: API muerta en `:domain` (use cases + métodos de
  `DriverConfigRepository`), `PlatformExtractor` sin tipo, `refine` nunca
  seteado, `FeatureFlag.ACCESSIBILITY/METRICS` muertos (toggles sin efecto),
  `CaptureMetrics.onCapture` muerto, write-only del framework de detección,
  `CaptureInputType` SHARE/GALLERY/TEST sin uso (intencional aditivo), bundle
  LEGACY de reglas en `:domain`, moneda duplicada.
- **Bajo (10)**: objetos/consts sin uso, API solo-tests, `OverlayState.CAPTURING`,
  `DiscardReason.CAPTURE_FAILED`, `start()` no-op, `timestampMillis` sin uso,
  dependencias Gradle sin uso, KDoc legacy, imágenes de test sin usar.
- **Observación (7)**: O-1…O-7.
- **Crítico (0)**: sin defectos de runtime/Hilt/Clean Architecture.

## Verificación (línea base del Sprint 11)

- `.\gradlew.bat ktlintCheck --console=plain` → BUILD SUCCESSFUL.
- `.\gradlew.bat lintDebug assembleDebug testDebugUnitTest :domain:test :core:platform:test :core:capture:test :core:capture:android:testDebugUnitTest :feature:overlay:testDebugUnitTest --console=plain` → BUILD SUCCESSFUL.

## Próximos pasos

1. Cierre oficial del Sprint 11 tras la aprobación del informe de WP-E3-05E
   (Architecture / Performance / Technical Debt / Sprint Review).
2. No iniciar ningún otro WP sin aprobación explícita.

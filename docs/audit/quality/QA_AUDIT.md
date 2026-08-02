# QA_AUDIT.md — Auditoría de Calidad

**Rol:** QA Automation Lead
**Objetivo:** Auditar cobertura, tests, edge cases, regression y tests faltantes. Sin implementar.
**Proyecto:** SIRC (app de ganancias Uber — captura de pantalla, OCR, overlay)
**Fecha:** 2026-08-01 · **Commit auditado:** `e3460a5`

---

## Resumen ejecutivo

| Métrica | Valor |
|---|---|
| Archivos de test | 26 unitarios + 1 helper + 3 instrumentados |
| Casos de test | 181 (173 unitarios + 8 instrumentados) |
| Módulos con cobertura | 7 de 11 |
| Módulos sin tests | `app`, `feature:history`, `feature:settings`, `feature:onboarding` |
| ViewModels sin test | 9 de 9 (0%) |
| Motor OCR real (`MlKitOcrEngine`) | 0 tests |
| Tests de UI/Compose | 0 |
| CI | SÍ (GitHub Actions) — sin tests instrumentados |

La cobertura está concentrada en lógica de dominio pura (engines, session, stats) y en el pipeline con fakes. Las mayores lagunas: capa de presentación (0%), OCR real, capa `data` (DAOs/mappers), servicios Android (accessibility/overlay/MediaProjection) y edge cases numéricos.

---

## 1. Inventario de tests por módulo

### 1.1 `domain` (9 archivos, 64 tests) — cobertura fuerte

| Archivo | Cubre | Casos clave |
|---|---|---|
| `RuleEngineTest.kt` | `RuleEngine` + 6 reglas | rentable→pasa; ganancia negativa→falla; sin datos→reglas cumplidas; personalizadas en orden |
| `RecommendationEngineTest.kt` | `RecommendationEngine` | ACCEPT/REJECT/WARNING; confianza acotada 0–100 |
| `ProfitEvaluationEngineTest.kt` | `ProfitEvaluationEngine` | costo/km derivado; consumo cero sin infinito; extremos de distancia |
| `ProfitEngineTest.kt` | `ProfitEngine` | PROFITABLE/NOT_PROFITABLE/MARGINAL; monedas MXN/BRL |
| `OfferValidatorTest.kt` | `OfferValidator` | montos/distancia/duración; incoherencias precio/km·h; recogida>viaje |
| `ConfidenceEngineTest.kt` | `ConfidenceEngine` | alta/baja/medio; moneda faltante; rango 0–100 |
| `CaptureSessionManagerTest.kt` | `CaptureSessionManager` | reloj inyectado; start/pause/resume/stop/reset |
| `HistoryStatsCalculatorTest.kt` | `HistoryStatsCalculator` | vacío; % aceptación; promedios; agrupación por día |
| `HistoryFilterTest.kt` | `HistoryFilter` | por plataforma/decisión/fechas/búsqueda; case-insensitive |

### 1.2 `data` (1 unitario + 2 instrumentados, 14 tests)

- `DriverConfigCodecTest.kt` (7) — codecs encode/decode platforms y additionalCosts.
- `OfferHistoryDaoTest.kt` (6, androidTest) — insert/observe ordenado, límite, trim, clear, análisis detallado.
- `SircDatabaseMigrationTest.kt` (1, androidTest) — **única** migración v1→v3 con 1 fila por tabla.

### 1.3 `core:ui` (5 tests)

- `ProfitStateTest.kt` — `ProfitState.fromDecision`, semáforo, colores, transparencia overlay.

### 1.4 `core:platform` (3 archivos, 29 tests) — cobertura fuerte

- `OfferTextParserTest.kt` (8) — montos con símbolo/código, distancia km, duración h/m, montos absurdos.
- `OfferParserOrchestratorTest.kt` (9) — detección REQUEST/RADAR/RESERVATION/MOTO/XL; fallback a extractor genérico; encadenamiento de parsers.
- `OfferDetectionEngineTest.kt` (12) — HOME/ERROR/OFFLINE/NAVIGATION/TRIP/REQUEST; acentos; prioridad de request; normalize.

### 1.5 `core:capture` (9 archivos, 52 tests) — cobertura fuerte con fakes

- `DefaultCapturePipelineTest.kt` (17) — el más completo: OCR on/off, fallos de captura/OCR/parseo, dedup por caché, 200 requests en stress, métricas.
- `OfferCaptureCoordinatorTest.kt` (8) — sesión por plataforma, flags, reset, start/stop.
- `ValidationRecorderTest.kt` (5) — buffer acotado a 500, reporte.
- `InMemoryOfferPerformanceTrackerTest.kt` (6) — ventana de 20, tope de 100.
- `DebounceCaptureSchedulerTest.kt` (3) — **único** test con reloj virtual (`runTest`).
- `InMemoryCaptureFrameCacheTest.kt` (5) — dedup por hash de imagen.
- `InMemoryCaptureRepositoryTest.kt` (3), `FakeParserTest.kt` (2), `InMemoryFeatureFlagsTest.kt` (3).

### 1.6 `core:capture:android` (1 instrumentado)

- `MediaProjectionScreenCaptureProviderTest.kt` (1) — solo smoke test: sin permiso no proyecta ni captura. **El flujo real de proyección no tiene test.**

### 1.7 `feature:overlay` (2 archivos, 16 tests)

- `PipelineOverlayDataSourceTest.kt` (11) — integra el pipeline con los engines reales; estados WAITING/ERROR; ACCEPT; historial; flags.
- `InMemoryOfferEvaluationRepositoryTest.kt` (5) — orden, ids, tope 100, clear, limit.

### 1.8 Módulos sin ningún test

- **`app`**, **`feature:history`**, **`feature:settings`**, **`feature:onboarding`** — 0 archivos de test.

---

## 2. Clases de producción sin test (tests faltantes)

### Nivel de riesgo ALTO (núcleo de captura/OCR/servicios)

| Clase | Ruta |
|---|---|
| `MlKitOcrEngine` (motor OCR real) | `feature\overlay\src\main\kotlin\com\sirc\feature\overlay\MlKitOcrEngine.kt:22` |
| `MediaProjectionScreenCapture` | `core\capture\android\src\main\kotlin\com\sirc\capture\android\MediaProjectionScreenCapture.kt:21` |
| `MediaProjectionScreenCaptureProvider` (flujo con token) | `...\provider\MediaProjectionScreenCaptureProvider.kt:40` |
| `MediaProjectionService` | `...\projection\MediaProjectionService.kt:27` |
| `SircAccessibilityService` | `feature\overlay\...\SircAccessibilityService.kt:26` |
| `CaptureAccessibilityService` (solo prueba indirecta del pipeline) | `CaptureAccessibilityService.kt:28` |
| `AccessibilityWindowObserver` | `AccessibilityWindowObserver.kt:18` |
| `OverlayService` | `OverlayService.kt:46` |
| `OverlayController` | `OverlayController.kt:19` |
| `AndroidPermissionManager` | `PermissionManager.kt:45` |
| `AndroidSircLogger` | `AndroidSircLogger.kt:23` |
| `PlatformOfferParser` (parser de producción) | `core\capture\...\parser\PlatformOfferParser.kt:22` |

### Nivel de riesgo MEDIO (capa de datos)

| Clase | Ruta |
|---|---|
| `DefaultOverlayConfigRepository` | `data\...\repository\DefaultOverlayConfigRepository.kt:12` |
| `DefaultOfferHistoryRepository` | `data\...\repository\DefaultOfferHistoryRepository.kt:19` |
| `DefaultDriverConfigRepository` | `data\...\repository\DefaultDriverConfigRepository.kt:14` |
| Mappers entity↔domain (`toDriverConfig`/`toEntity`/`toDomain`) | `data\...\local\mapper\Mappers.kt:22,42,94,109,124,146` |
| `DriverConfigDao`, `OverlayConfigDao` | `data\...\dao\*.kt:11` |
| `EvaluateOfferUseCase`, `EvaluateDetailedOfferUseCase` (sin unit test propio) | `domain\...\usecase\EvaluateOfferUseCase.kt:12`, `EvaluateDetailedOfferUseCase.kt:15` |
| Use cases de config (`Save/Get Driver/Overlay Config`) | `domain\...\usecase\*.kt` |
| Reglas individuales (`MinimumProfitRule`, etc.) | `domain\...\engine\rules\*.kt` |

### Nivel de riesgo MEDIO (presentación — 0% cobertura)

| Clase | Ruta |
|---|---|
| `RootViewModel` | `app\...\RootViewModel.kt:13` |
| `HomeViewModel` | `HomeViewModel.kt:14` |
| `DiagnosisViewModel` | `DiagnosisViewModel.kt:14` |
| `DebugPanelViewModel` | `DebugPanelViewModel.kt:39` |
| `OverlayViewModel` | `feature\overlay\...\OverlayViewModel.kt:15` |
| `SettingsViewModel` | `feature\settings\...\SettingsViewModel.kt:23` |
| `HistoryViewModel` + `DatePreset` | `feature\history\...\HistoryViewModel.kt:41` |
| `StatsViewModel` | `feature\history\...\StatsViewModel.kt:17` |
| `OnboardingViewModel` | `feature\onboarding\...\OnboardingViewModel.kt:21` |

---

## 3. Edge cases no cubiertos

### 3.1 Numéricos / aritmética (riesgo de NaN/Infinity)

1. **División por cero en `ProfitEngine`** — `totalCost == estimatedTotal` → `profitPerKm/profitPerHour` = Infinity/NaN. Solo se testea distancia=0/duración=0, no `total==cost`.
2. **Valores NaN/Infinity** en `estimatedTotal` — sin test.
3. **`HistoryStatsCalculator` con `distanceKm == 0` o `durationMin == 0`** → división por cero en promedios. Sin test.
4. **`estimatedTotal` negativo o cero** sin distancia/duración — sin test.
5. **`OfferValidator` con `estimatedTotal == 0.0`** (no negativo, no null) — sin test.
6. **`DebounceCaptureScheduler` con `debounceMillis <= 0`** — sin test.
7. **`HistoryStatsCalculator`** con `confidencePercent == null` en el promedio — sin test.

### 3.2 Texto OCR malformado

1. **`MlKitOcrEngine`** sin ningún test (texto basura, parcial, números ilegibles).
2. Parsers con caracteres rotos (`$12O.5O`, `B5 km`) — sin test.
3. **Múltiples montos en una línea** (precio original vs. tarifa con descuento) — sin test.
4. OCR devolviendo `emptyList()` en el pipeline (solo se testea excepción de OCR).

### 3.3 Monedas y locales

1. Solo MXN/BRL probados; faltan COP, USD, EUR, `€`, `S/`, `RD$`.
2. Separadores de miles (`$1,200.50`), coma decimal europea (`120,50`), `MXN$`, montos sin decimales — sin test.
3. **Multi-idioma: 0 tests.** Todas las keywords probadas en español; no hay cobertura de inglés/portugués (`New trip`, `Aceitar`) — crítica si la app se usa con conductoras de otras regiones.

### 3.4 Dominio / repositorios

1. `EvaluateDetailedOfferUseCase` cuando `getDriverConfig()` devuelve null (usa `DriverConfig.default()` en `EvaluateDetailedOfferUseCase.kt:21`) — sin test.
2. Repositorios con config null → defaults (`DefaultOfferHistoryRepository.kt:27-40`) — sin test.
3. `trimToLimit` con `limit < MIN_LIMIT` (coerción `coerceAtLeast(50)`, `DefaultOfferHistoryRepository.kt:33`) — sin test.
4. `HistoryFilter` con `dateFrom > dateTo`, solo `dateFrom` sin `dateTo`, `query` en blanco — sin test.
5. `RecommendationEngine` con `reasons` vacío en decisión no rentable — sin test.
6. `RuleEngine` con lista de reglas vacía — sin test.

### 3.5 Migraciones de BD

- Solo 1 test (`migracionV1AV3`, 1 fila por tabla). Faltan: v2→v3 aislada, tablas vacías, datos con NULL/edge cases, fallback ante corrupción de esquema.

### 3.6 Reloj / concurrencia

- `System.currentTimeMillis()` real en pipeline/coordinator/dataSource; solo `DebounceCaptureSchedulerTest` usa reloj virtual. No hay tests de backpressure (SharedFlow sin suscriptores, buffers llenos).

---

## 4. Regression / configuración de test

| Aspecto | Estado |
|---|---|
| **CI** | SÍ — `.github\workflows\ci.yml:44-60`: ktlintCheck, test, lintDebug, assembleDebug. **No ejecuta tests instrumentados** (sin emulador/Test Lab). |
| **ktlint** | SÍ — plugin `org.jlleitschuh.gradle.ktlint` v12.1.2 en todos los módulos + CI. |
| **Lint** | SÍ — `lintDebug` por defecto (sin configuración personalizada). |
| **Proguard** | SÍ — `app\proguard-rules.pro`, `isMinifyEnabled=true` en release. |
| **MockK/Mockito** | NO — ningún módulo. Los tests usan fakes manuales o clases reales. |
| **Robolectric** | NO. |
| **Compose ui-test** | NO — sin `createComposeRule` en ningún archivo. **0 tests de UI.** |
| **`kotlinx-coroutines-test`** | Sí en `core:capture` y `feature:overlay`; solo se usa en `DebounceCaptureSchedulerTest` (los de overlay usan `runBlocking`). |
| **Tests de ViewModel** | NO — 0 tests; no hay `Dispatchers.setMain`/`MainDispatcherRule`/`InstantTaskExecutorRule` en ningún test. |
| **`arch-core-testing`** | Declarado en `data` (androidTest) pero **sin uso**. |

---

## 5. Hallazgos con severidad

### H-QA-01 — Cero tests de capa de presentación y ViewModels
- **Resumen:** Ninguno de los 9 ViewModels ni pantallas Compose tiene test; no existe infraestructura de test de UI (no hay dependencia `ui-test-junit4`, ni `createComposeRule`).
- **Impacto:** El negocio de estados (onboarding→home→overlay, permisos, diagnóstico) no tiene red de seguridad; refactor de UI puede romper flujos sin detectarse. La lógica de `HomeViewModel`/`DebugPanelViewModel` (lógica real de permisos/estado) queda sin cobertura.
- **Severidad:** ALTA
- **Evidencia:** módulos `app`, `feature:history`, `feature:settings`, `feature:onboarding` sin `src/test`; `gradle\libs.versions.toml` sin compose ui-test; ViewModels listados en §2.
- **Prioridad:** P1

### H-QA-02 — Motor OCR real sin tests
- **Resumen:** `MlKitOcrEngine` no tiene ningún test; el pipeline solo se prueba con fakes.
- **Impacto:** El componente más frágil (resultados del OCR alimentan todo el análisis) no está verificado con imágenes reales de prueba, pese a que `core:capture` ya tiene 15 PNGs de muestra (`core\capture\src\test\resources\test-images\`).
- **Severidad:** ALTA
- **Evidencia:** `MlKitOcrEngine.kt:22-50` (0 tests); PNGs en `core\capture\src\test\resources\test-images\`.
- **Prioridad:** P1

### H-QA-03 — Cero tests instrumentados de la cadena MediaProjection + servicios
- **Resumen:** Solo existe 1 smoke test (`MediaProjectionScreenCaptureProviderTest`); los servicios (Overlay, MediaProjection, 2 AccessibilityServices) no tienen tests instrumentados.
- **Impacto:** La funcionalidad crítica de la app (captura real, overlay, accesibilidad) nunca se ejecuta en CI. Regresiones de integración solo se detectan en dispositivo manualmente.
- **Severidad:** ALTA
- **Evidencia:** `.github\workflows\ci.yml:44-60` (sin `connectedCheck`); `core\capture\android\src\androidTest\...\MediaProjectionScreenCaptureProviderTest.kt` (1 test).
- **Prioridad:** P1

### M-QA-04 — Lagunas en edge cases numéricos (división por cero / NaN)
- **Resumen:** `ProfitEngine` con `totalCost==estimatedTotal` y `HistoryStatsCalculator` con distancia/duración 0 producen Infinity/NaN sin test ni guard.
- **Impacto:** El overlay podría mostrar `NaN`/`Infinity` en ofertas límite (recomendación incorrecta al conductor).
- **Severidad:** MEDIA
- **Evidencia:** `domain\src\test\...\ProfitEngineTest.kt` (no cubre `total==cost`); `domain\src\test\...\HistoryStatsCalculatorTest.kt` (no cubre división por cero).
- **Prioridad:** P2

### M-QA-05 — Sin tests multi-idioma ni multi-moneda
- **Resumen:** Todas las keywords y formatos probados en español/MXN/BRL. Sin cobertura de inglés, portugués ni otras monedas.
- **Impacto:** Si se despliega en otras regiones, la detección y el formato fallarían sin que CI lo detecte.
- **Severidad:** MEDIA
- **Evidencia:** `OfferDetectionEngineTest.kt`, `OfferParserOrchestratorTest.kt` (solo español); `ProfitEngineTest.kt` (solo MXN/BRL).
- **Prioridad:** P2

### M-QA-06 — Cobertura insuficiente de migraciones y repositorios Room
- **Resumen:** 1 sola migración probada (v1→v3); DAOs de config y mappers entity↔domain sin test.
- **Impacto:** Corrupción silenciosa de datos en updates de esquema o mappers podría afectar configuraciones del conductor sin detección.
- **Severidad:** MEDIA
- **Evidencia:** `SircDatabaseMigrationTest.kt` (1 test); `data\...\dao\DriverConfigDao.kt:11`, `OverlayConfigDao.kt:11` (sin test).
- **Prioridad:** P2

### M-QA-07 — CI sin tests instrumentados
- **Resumen:** El CI corre solo unit tests + lint + assemble; los androidTests (DAO, migración, provider) nunca se ejecutan.
- **Impacto:** Los 3 tests instrumentados existentes no aportan valor en CI; regresiones de BD/schema pasan desapercibidas.
- **Severidad:** MEDIA
- **Evidencia:** `.github\workflows\ci.yml:44-60`.
- **Prioridad:** P2

### B-QA-08 — Sin pruebas de ViewModel ni de dispatchers
- **Resumen:** No se usa `Dispatchers.setMain`, ni `MainDispatcherRule`, ni `InstantTaskExecutorRule`.
- **Impacto:** Los ViewModels no se pueden testear fácilmente; los flujos de estado (`stateIn`, `combine`) no están verificados.
- **Severidad:** BAJA
- **Evidencia:** grep sin resultados de `setMain`/`MainDispatcherRule` en tests.
- **Prioridad:** P3

### B-QA-09 — `PlatformOfferParser` de producción sin test directo
- **Resumen:** El parser que usa la app en producción (`PlatformOfferParser`) no tiene test; solo se prueba con el fake en `core:capture`.
- **Impacto:** La ruta real OCR→parseo no está cubierta con su parser definitivo.
- **Severidad:** BAJA
- **Evidencia:** `core\capture\...\parser\PlatformOfferParser.kt:22` (sin test).
- **Prioridad:** P3

---

## 6. Puntos fuertes

- Excelente cobertura de la lógica de dominio pura (engines, validación, estadísticas, filtros).
- `DefaultCapturePipelineTest` es un test de integración robusto con 17 casos (flags, fallos, dedup, stress de 200 requests).
- Se prueban los límites de los buffers en memoria (500/100/50/32) — buena disciplina anti-leak en tests.
- ktlint en todos los módulos + CI con lint y assemble.
- Recursos de imágenes de prueba reales ya disponibles (`test-images\`).

---

## 7. Acciones recomendadas (solo análisis, sin implementar)

1. Añadir `MockK`/`Robolectric`/`compose ui-test` y testear los ViewModels de `app`.
2. Test instrumentado del flujo OCR con los PNGs existentes (necesita emulador/Test Lab en CI).
3. Tests unitarios de `MlKitOcrEngine` con imágenes de prueba (a nivel JVM con PNGs + fakes del ML Kit).
4. Cubrir divisiones por cero/NaN en engines y stats.
5. Añadir casos multi-idioma y multi-moneda (es, en, pt).
6. Ampliar migraciones (v2→v3, tablas vacías, datos NULL) y mappers.
7. Ejecutar androidTests en CI (emulador o Firebase Test Lab).

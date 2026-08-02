# Auditoría de Arquitectura — SIRC

> Auditoría técnica completa. Rol: Chief Software Architect.
> Solo evidencia, sin modificaciones de código. Fecha: 2026-08-01.
> Método: lectura de 80+ fuentes Kotlin, manifests, recursos, build files y tests
> de los 11 módulos; verificación de cada hallazgo con `file:line`.

**Convención de severidad**: `CRITICA` = bloquea release / pérdida de datos /
integridad; `ALTA` = defecto real en la ruta productiva; `MEDIA` = riesgo
significativo o deuda mantenible; `BAJA` = mejora.
**Convención de prioridad**: `P0` = inmediato; `P1` = antes del siguiente
release; `P2` = siguiente iteración; `P3` = backlog.

---

## Resumen ejecutivo

| Dimensión | Veredicto |
|---|---|
| Clean Architecture | ✅ Núcleo puro y bien dirigido; ❌ 2 pipelines paralelos y use-cases muertos |
| SOLID | ✅ SRP en engines; ❌ God-classes en la ruta overlay |
| Modularización | ✅ DAG acíclico; ❌ dependencia muerta `:data` en features + DI en feature |
| Acoplamiento | ✅ interfaces limpias; ❌ app acoplada a internos de feature:overlay |
| Cohesión | ⚠️ `PipelineOverlayDataSource` / `DebugPanelViewModel` / `OverlayService` |
| Duplicación | ❌ 2 servicios de accesibilidad idénticos + formatters ×5 + doble persistencia |
| Código muerto | ❌ 2 use-cases + clúster de métodos de repo + `FakeParser` en producción |
| Dependencias circulares | ✅ Ninguna a nivel de módulo (DAG acíclico) |
| Clases/métodos grandes | ⚠️ 9 archivos > 200 líneas, 4 con responsabilidades mezcladas |
| Paquetes/Nombres | ⚠️ DI de infra en paquete feature; servicios con nombres ambiguos |
| Interfaces/Abstracción | ⚠️ sobre-abstracción puntual; `FakeParser` inyectable en producción |
| Escalabilidad multi-plataforma | ❌ Motor de detección es vocabulario español+Uber (eslabón roto) |

**Veredicto global**: arquitectura de base sólida (dominio puro, dirección de
dependencias correcta, Room versionado, memoria acotada, buenos tests de
motores). Los riesgos materiales están en la **ruta de captura duplicada**, el
**mecanismo de detección de pantalla acoplado a Uber/es**, y la **capa de
presentación con god-classes y debug en producción**. Ningún hallazgo requiere
reescritura; todos son correcciones localizadas.

---

## 1. Clean Architecture

### ARC-1.1 — Dos pipelines de captura paralelos y ambos activos
- **Resumen**: Existen dos arquitecturas de captura completas y redundantes corriendo en el mismo proceso.
- **Evidencia**: `SircApplication.kt:14` arranca `OfferCaptureCoordinator` (legacy: `SircAccessibilityService → AccessibilityWindowObserver`); `feature/overlay/src/main/AndroidManifest.xml:24-35` declara `CaptureAccessibilityService` (moderno: → `DebounceCaptureScheduler → DefaultCapturePipeline`). Ambos servicios comparten `accessibility_service_config.xml`, el mismo filtro de paquetes y el mismo `PlatformOfferParser`. `docs/ARCHITECTURE.md:293-294` afirma que el flujo legacy fue "eliminado en RC1" — no fue eliminado.
- **Riesgo**: Alto. Procesamiento duplicado (doble recorrido del árbol de accesibilidad hasta 400 nodos, doble OCR/parseo), doble escritura de historial, consumo de batería, y estados inconsistentes según qué servicio se active. Además, `FeatureFlag.PARSER` solo lo honra el coordinador legacy (`OfferCaptureCoordinator.kt:90`), no el pipeline (`DefaultCapturePipeline.kt:70`), por lo que apagar "Parser" en debug no afecta la vía productiva.
- **Severidad**: ALTA
- **Recomendación**: Elegir la vía del pipeline moderno y eliminar `SircAccessibilityService`, `AccessibilityWindowObserver`, `OfferCaptureCoordinator` y sus bindings (`WindowObserver`, `CaptureRepository`); hacer que el pipeline honre `PARSER`/`ACCESSIBILITY`.
- **Prioridad**: P0

### ARC-1.2 — Capa de use-cases inconsistente: huérfana y a la vez saltada
- **Resumen**: La capa de use-cases se aplica a medias: el flujo crítico (overlay) la salta y dos use-cases están muertos.
- **Evidencia**: `domain/.../usecase/EvaluateOfferUseCase.kt:12` y `OfferHistoryUseCases.kt:20` (`AddOfferHistoryUseCase`) tienen **cero call sites**; la orquestación real inyecta engines y repos directamente en `feature/overlay/.../PipelineOverlayDataSource.kt:57-71` (RuleEngine, ConfidenceEngine, 3 repos) y escribe historial vía `historyRepository.add()` en `:185`. Los use-cases de config son fachadas anémicas multi-método (`GetDriverConfigUseCase.kt:13-25`).
- **Riesgo**: Medio. Dificulta razonar el flujo y permite dos convenciones coexistiendo; el código muerto confunde.
- **Severidad**: MEDIA
- **Recomendación**: Adoptar una única convención: (a) eliminar los use-cases huérfanos y documentar que la orquestación vive en el data source, o (b) crear `AnalyzeOfferUseCase` que posea reglas+confianza y rutear persistencia por use-cases.
- **Prioridad**: P2

### ARC-1.3 — `isConfigured()` contradice la definición de dominio; guardas de onboarding eludibles
- **Resumen**: El gating de onboarding se basa en existencia de fila, no en contenido; guardas parciales crean filas "completas".
- **Evidencia**: `data/.../DefaultDriverConfigRepository.kt:21` → `isConfigured()` = `map { it != null }`; `:33-40` → `save(driverCosts)`/`save(decisionThresholds)` caen a `DriverConfig.default()` (fil a completa con país/ciudad/plataformas) cuando no existe fila. El dominio define `DriverConfig.isConfigured` por contenido (`DriverConfig.kt:20-26`) pero no se usa.
- **Riesgo**: Alto. Guardar costos desde Ajustes antes del onboarding crea fila "completa" → `RootViewModel` salta el onboarding; usuario sin perfil configurado.
- **Severidad**: ALTA
- **Recomendación**: Implementar `isConfigured()` mapeando al `DriverConfig.isConfigured` de dominio; bloquear guardados parciales si no existe fila.
- **Prioridad**: P1

### ARC-1.4 — Dependencia muerta de `:data` en los 4 feature modules
- **Resumen**: Los features declaran `implementation(project(":data"))` sin usar ningún símbolo de `:data`.
- **Evidencia**: `feature/{overlay,settings,history,onboarding}/build.gradle.kts:34`; grep de `com.sirc.data.*` en `feature/*/src` = 0 coincidencias. Los repos llegan vía Hilt agregado desde `:app`.
- **Riesgo**: Alto (habilitador). Permite que una feature inyecte implementaciones concretas de `:data` en vez de contratos de `:domain`, erosionando la regla "feature → solo domain" (`docs/ARCHITECTURE.md:52-53`).
- **Severidad**: MEDIA
- **Recomendación**: Eliminar `:data` de los 4 features; que dependan solo de `:domain`.
- **Prioridad**: P1

---

## 2. SOLID

### SOL-2.1 — God-class: `PipelineOverlayDataSource` (329 líneas, 13 dependencias)
- **Resumen**: Un solo objeto traduce pipeline, evalúa, aplica reglas, calcula confianza, persiste en 2 repos, alimenta sesión/métricas/validación y controla TTL del overlay (SRP violado).
- **Evidencia**: `feature/overlay/.../PipelineOverlayDataSource.kt:57-71` (constructor), `:118-228` (procesamiento), `:229-329` (persistencia/TTL/formateo). Test existente monta 9 fakes (`PipelineOverlayDataSourceTest.kt`).
- **Riesgo**: Alto. Difícil de testear aisladamente; punto único de fallo; cualquier cambio de flujo lo toca.
- **Severidad**: ALTA
- **Recomendación**: Extraer colaboradores `OverlayAnalyzer` (reglas+confianza), `OverlayPersister` (doble escritura) y `OverlayScheduler` (TTL); el data source queda como coordinador fino.
- **Prioridad**: P1

### SOL-2.2 — God-class: `DebugPanelViewModel` (331 líneas, 11 dependencias)
- **Resumen**: ViewModel de diagnóstico que toca ambos pipelines, overlay UI, permisos, flags, métricas, sesión y validación; `build()` produce un UiState de 39 campos.
- **Evidencia**: `app/.../DebugPanelViewModel.kt:39-50` (constructor), `:113-164` (combines), `:267-324` (build). Colectado en `app/.../SircApp.kt:53-54` a nivel Scaffold (siempre activo).
- **Riesgo**: Medio-Alto. Acopla el debug a todo el sistema; CPU/batería en todas las pantallas; cualquier cambio interno exige tocar este archivo.
- **Severidad**: MEDIA
- **Recomendación**: Consumir los StateFlows expuestos por cada subsistema sin inyectarlos todos; extraer la generación del reporte a un helper puro; scope del VM a la ruta Debug.
- **Prioridad**: P2

### SOL-2.3 — `OverlayService`: Service + notificación + WindowManager + Compose + config en un archivo
- **Resumen**: Un solo archivo mezcla ciclo de vida Android, canal de notificación, layout de ventana, renderizado Compose y reclamp.
- **Evidencia**: `feature/overlay/.../OverlayService.kt` — vida (`:46-92`), notificación (`:203-216`), WindowManager (`:170-201`), Compose (`:104-129`), drag (`:145-168`).
- **Riesgo**: Medio. Difícil probar y evolucionar la UI del overlay sin tocar el Service.
- **Severidad**: MEDIA
- **Recomendación**: Extraer `OverlayWindowController` para WindowManager y mantener `OverlayContent` (ya extraído) como única capa de presentación.
- **Prioridad**: P2

### SOL-2.4 — `DefaultCapturePipeline`: 7 responsabilidades en `processInternal`
- **Resumen**: El pipeline real es un mega-método secuencial: captura, dedup, OCR, parseo, persistencia, métricas, validación, estado.
- **Evidencia**: `core/capture/.../pipeline/DefaultCapturePipeline.kt:83-186`.
- **Riesgo**: Medio. Legibilidad y testeabilidad de cada paso degradadas.
- **Severidad**: MEDIA
- **Recomendación**: Extraer deduplicador, extractor de textos y persistor como colaboradores; el pipeline queda como orquestador.
- **Prioridad**: P2

### SOL-2.5 — `ProfitEngine` mezcla cálculo con formateo de presentación
- **Resumen**: El motor de dominio también formatea moneda/horas y contiene símbolos hardcodeados.
- **Evidencia**: `domain/.../engine/ProfitEngine.kt:88-122` (`formatCurrency`/`formatHours` + `CURRENCY_SYMBOLS`). Inyectado en UI: `feature/overlay/.../OverlayService.kt:49`, `feature/history/.../HistoryViewModel.kt:44`.
- **Riesgo**: Medio. Obliga a inyectar el engine en la UI; 3 formateadores de moneda dispares en el código; `"COP"→"$"` y `"ARS"→"$"` muestran dólar en vez de símbolo local.
- **Severidad**: MEDIA
- **Recomendación**: Mover formatters a `core:ui` (util puro); el dominio solo calcula.
- **Prioridad**: P2

### SOL-2.6 — `DefaultOfferHistoryRepository.add()` mezcla persistencia y política
- **Resumen**: El repo aplica la política de retención en la escritura (SRP menor) y re-lee la config de overlay por cada inserción.
- **Evidencia**: `data/.../DefaultOfferHistoryRepository.kt:23-26`.
- **Riesgo**: Bajo-Medio. Una lectura + DELETE extra por oferta en la ruta crítica.
- **Severidad**: BAJA
- **Recomendación**: Recortar solo si `count() > limit`, o delegar a un worker periódico (ver AND-10.1).
- **Prioridad**: P3

---

## 3. Modularización y Acoplamiento

### MOD-3.1 — `app` acoplada a internos de `feature:overlay`
- **Resumen**: La capa app conoce y manipula internos de una feature hermana en vez de solo orquestar navegación.
- **Evidencia**: `app/.../DebugPanelViewModel.kt:24-27` importa `OverlayDataSource`, `OverlayManager`, `OverlayUiState`, `PermissionManager` de `feature.overlay`; `DiagnosisScreen.kt:33` importa `OverlayViewModel`.
- **Riesgo**: Medio. Cambios internos del overlay rompen app; la frontera entre módulos se vuelve cosmética.
- **Severidad**: MEDIA
- **Recomendación**: Mover panel de debug y diagnóstico a `feature:overlay` (o un `feature:debug`) y que app solo componga pantallas.
- **Prioridad**: P2

### MOD-3.2 — DI de infraestructura viviendo en paquete de feature
- **Resumen**: Los bindings de `core:capture` y `core:platform` están en `com.sirc.feature.overlay`.
- **Evidencia**: `feature/overlay/.../CaptureModule.kt:29-67` y `PlatformModule.kt:26-62` bindean `WindowObserver`, `CapturePipeline`, `OcrEngine`, parsers de `core:platform`.
- **Riesgo**: Medio (futuro). Si `core` necesita bindings propios, se crean dependencias invertidas confusas; hoy `CaptureAndroidModule` ya bindea en `core:capture:android`.
- **Severidad**: MEDIA
- **Recomendación**: Mover los módulos Hilt de infraestructura a los core modules o a un `:di` de app.
- **Prioridad**: P2

### MOD-3.3 — Implementación de repositorio en feature, no en `:data`
- **Resumen**: `InMemoryOfferEvaluationRepository` (implementación de un contrato de dominio) vive en `feature:overlay`.
- **Evidencia**: `feature/overlay/.../InMemoryOfferEvaluationRepository.kt:18`; bound en `CaptureModule.kt:67`; consumido desde `app/.../DebugPanelViewModel.kt:24,46`.
- **Riesgo**: Medio. Rompe la convención "contratos en domain, implementaciones en data"; los consumidores dependen de otra feature para persistencia.
- **Severidad**: MEDIA
- **Recomendación**: Mover la implementación (o su binding) a `:data` o `:core:capture`.
- **Prioridad**: P2

### MOD-3.4 — `core:ui` depende de `:domain`
- **Resumen**: Un módulo de UI compartida importa modelos de dominio para mapear colores.
- **Evidencia**: `core/ui/.../components/StatusComponents.kt:23-24` y `theme/ProfitState.kt:4-5` importan `Decision`/`Recommendation`.
- **Riesgo**: Bajo-Medio. Acopla el tema visual a modelos de negocio.
- **Severidad**: BAJA
- **Recomendación**: Mapear enum→color en las features o pasar el enum como parámetro del composable consumidor.
- **Prioridad**: P3

---

## 4. Cohesión y Dependencias Circulares

### COH-4.1 — Sin ciclos a nivel de módulo (positivo)
- **Resumen**: El grafo de módulos es un DAG acíclico; `domain` es hoja pura JVM.
- **Evidencia**: `settings.gradle.kts:25-34` + `build.gradle.kts` de los 11 módulos (leídos todos). Ningún core/feature depende de `:app` ni de otra feature a nivel de módulo.
- **Riesgo**: Bajo.
- **Recomendación**: Mantener el invariante; vigilar que `app → feature-internals` (MOD-3.1) no crezca.
- **Prioridad**: P3

### COH-4.2 — Riesgo latente de ciclo: bindings de infra en feature
- **Resumen**: Si `core:capture` necesita bindings propios mañana, la ubicación en feature crea dependencia invertida.
- **Evidencia**: `CaptureModule.kt`/`PlatformModule.kt` en `com.sirc.feature.overlay` (ver MOD-3.2).
- **Riesgo**: Medio a futuro.
- **Severidad**: MEDIA
- **Recomendación**: Mover bindings de infra a `core`/`app` (ver MOD-3.2).
- **Prioridad**: P2

---

## 5. Tamaño de clases y métodos

### SIZ-5.1 — 9 archivos de producción superan ~200 líneas
- **Resumen**: Los 4 mayores concentran múltiples responsabilidades; los composables de 400+ líneas son difíciles de refactorizar.
- **Evidencia** (líneas contadas):

| Archivo | Líneas | Módulo |
|---|---|---|
| `DebugPanelScreen.kt` | 440 | app |
| `HistoryScreen.kt` | 422 | feature/history |
| `PipelineOverlayDataSource.kt` | 329 | feature/overlay |
| `DebugPanelViewModel.kt` | 331 | app |
| `OnboardingSteps.kt` | 324 | feature/onboarding |
| `OverlayService.kt` | 252 | feature/overlay |
| `HomeScreen.kt` | 239 | app |
| `DefaultCapturePipeline.kt` | 223 | core/capture |
| `DiagnosisScreen.kt` | 179 | app |

- **Riesgo**: Medio. Legibilidad y testabilidad degradadas.
- **Severidad**: MEDIA
- **Recomendación**: Descomponer pantallas en composables por sección y extraer lógica de ViewModels a coordinadores/usecases.
- **Prioridad**: P2

### SIZ-5.2 — Firmas con parámetros excesivos
- **Resumen**: Firmas infladas que señalan falta de objetos de contexto intermedios.
- **Evidencia**: `DebugPanelViewModel.build(...)` (8 params, 39 campos); `PipelineOverlayDataSource.persist(...)` (4 objetos tipados).
- **Riesgo**: Bajo-Medio.
- **Severidad**: BAJA
- **Recomendación**: Agrupar en data classes intermedias.
- **Prioridad**: P3

---

## 6. Responsabilidades mezcladas

### MIX-6.1 — `HistoryViewModel` filtra `ProfitEngine` público a la UI
- **Resumen**: El ViewModel expone un motor de dominio a Compose para formatear números.
- **Evidencia**: `feature/history/.../HistoryViewModel.kt:44` (`val engine: ProfitEngine`); consumido en `HistoryScreen.kt:98,110,326,370,374`.
- **Riesgo**: Medio. Refuerza UI→engine y rompe `UI → ViewModel → usecase → domain` (`docs/CODING_STANDARDS.md:48`).
- **Severidad**: MEDIA
- **Recomendación**: Pre-formatear en el ViewModel/UiState o usar formatters de `core:ui`.
- **Prioridad**: P2

### MIX-6.2 — Duplicación UI↔dominio: textos presentacionales en domain
- **Resumen**: El dominio genera texto en español (motivos de reglas, razones de confianza) y la UI embebe strings literales.
- **Evidencia**: `RuleMessages.kt:11-26`, `RecommendationEngine.kt` (mensajes), y grep `stringResource(R.string` = 0 coincidencias en Compose (`HomeScreen.kt:89-233`, `DiagnosisScreen.kt:67-145`, `OverlayContent.kt:228-231`).
- **Riesgo**: Medio. Localización e i18n bloqueados; consistencia de textos frágil.
- **Severidad**: MEDIA
- **Recomendación**: Mover textos de UI a `values/strings.xml`; mover mensajes de dominio a una capa de presentación con recursos.
- **Prioridad**: P2

---

## 7. Paquetes, Nombres y Convenciones

### PKG-7.1 — Dos servicios de accesibilidad con nombres ambiguos
- **Resumen**: `SircAccessibilityService` vs `CaptureAccessibilityService` no comunican su función real.
- **Evidencia**: Ambos en `feature/overlay`, ambos en manifest (`AndroidManifest.xml:11-35`); `docs/KNOWN_ISSUES.md:9-14` incluso invita a activar ambos.
- **Riesgo**: Medio. Un usuario activa el "equivocado"; confusión de mantenimiento.
- **Severidad**: MEDIA
- **Recomendación**: Eliminar uno (ARC-1.1) o renombrar a `CoordinatorAccessibilityService`/`PipelineAccessibilityService`.
- **Prioridad**: P2

### PKG-7.2 — Formatters con nombres distintos y lógica duplicada en 5 archivos
- **Resumen**: Cada pantalla define su propio formateador con variantes inconsistentes.
- **Evidencia**: `formatNumber` en `SettingsScreen.kt:214`, `OnboardingScreen.kt:209`, `DebugPanelScreen.kt:453`, `HistoryScreen.kt:438`, `StatsScreen.kt:256` (2 variantes: con y sin agrupar miles); `formatMoney` en `StatsScreen.kt:252`, `DiagnosisScreen.kt:172`; `formatTimestamp` en `DebugPanelScreen.kt:462`, `HistoryScreen.kt:440`.
- **Riesgo**: Medio. Formatos inconsistentes visibles al usuario.
- **Severidad**: MEDIA
- **Recomendación**: Crear `core:ui` `Formatters` (`formatNumber`/`formatMoney`/`formatTimestamp`) y usarlos en todas las features.
- **Prioridad**: P1

### PKG-7.3 — `Mappers.kt` mezcla extensiones con funciones top-level
- **Resumen**: Convención inconsistente dentro del mismo archivo de mapeo.
- **Evidencia**: `data/.../Mappers.kt:66-92` expone `encodePlatforms`/`decodePlatforms`/`encodeAdditionalCosts`/`decodeAdditionalCosts` como funciones globales, mientras el resto son extensiones `toEntity()`/`toDomain()`.
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: Unificar como extensiones.
- **Prioridad**: P3

### PKG-7.4 — KDoc desactualizado sobre el parser
- **Resumen**: Documentación que apunta al parser simulado como el único existente.
- **Evidencia**: `core/capture/.../parser/OfferParser.kt:11` ("hoy solo existe la implementación simulada FakeParser") y `docs/ARCHITECTURE.md:164` ("OfferParser (FakeParser hoy)"), pero DI bindea `PlatformOfferParser` (`CaptureModule.kt:39`).
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: Actualizar KDoc y docs.
- **Prioridad**: P3

### PKG-7.5 — `core:capture` con 9 subpaquetes fragmentados
- **Resumen**: Fragmentación alta para un módulo pequeño; la mayoría de archivos < 50 líneas.
- **Evidencia**: `core/capture/src/main/.../capture/{flag,cache,scheduler,observer,metrics,log,pipeline,parser,ocr,coordinator,repository,validation,screen,model}`.
- **Riesgo**: Bajo (ordenado) pero navegación costosa.
- **Severidad**: BAJA
- **Recomendación**: Consolidar paquetes afines (model/, validation/, metrics/).
- **Prioridad**: P3

---

## 8. Interfaces innecesarias y Abstracciones

### ABS-8.1 — Sobre-abstracción puntual en contratos triviales
- **Resumen**: Varias interfaces con una sola implementación bordean el over-engineering.
- **Evidencia**: `OverlayDataSource` (3 miembros) → solo `PipelineOverlayDataSource`; `FeatureFlags` (2 métodos) → solo `InMemoryFeatureFlags`; `CaptureFrameCache` → solo `InMemoryCaptureFrameCache`; `CaptureMetrics` → `NoOp`+`DebugCaptureMetrics`.
- **Riesgo**: Bajo-Medio. Costo de navegación/mantenimiento sin beneficio claro en los triviales.
- **Severidad**: BAJA
- **Recomendación**: Mantener interfaces donde hay impls alternativas o testeo real (OcrEngine, OfferParser, repositorios); considerar concretas donde el contrato no aporta.
- **Prioridad**: P3

### ABS-8.2 — `FakeParser` en código de producción y inyectable por Hilt
- **Resumen**: Un test-double vive en `src/main` y es `@Singleton @Inject`.
- **Evidencia**: `core/capture/src/main/.../parser/FakeParser.kt` (produce `FAKE_ESTIMATED_TOTAL=125.0`); `CaptureModule.kt:39` bindea el parser real, pero un cambio accidental de binding enviaría ofertas falsas a producción.
- **Riesgo**: Alto.
- **Severidad**: ALTA
- **Recomendación**: Mover `FakeParser` a `src/test` (o `src/debug`) y eliminar su `@Inject` de producción.
- **Prioridad**: P1

### ABS-8.3 — `CaptureMetrics` con cuerpo NoOp por defecto
- **Resumen**: Una implementación real que "olvida" una etapa compila sin error.
- **Evidencia**: `core/capture/.../metrics/CaptureMetrics.kt:10-16` (métodos `= Unit`).
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: Interfaz estricta + `NoOpCaptureMetrics` explícito.
- **Prioridad**: P3

---

## 9. Código muerto

### DEAD-9.1 — `EvaluateOfferUseCase` (0 referencias)
- **Resumen**: Use-case completo huérfano.
- **Evidencia**: `domain/.../usecase/EvaluateOfferUseCase.kt:12`; grep global solo encuentra su definición.
- **Riesgo**: Bajo (mantenimiento/confusión).
- **Severidad**: MEDIA
- **Recomendación**: Eliminar.
- **Prioridad**: P1

### DEAD-9.2 — `AddOfferHistoryUseCase` (0 referencias)
- **Resumen**: Use-case de agregar historial nunca usado; el pipeline salta la capa.
- **Evidencia**: `domain/.../usecase/OfferHistoryUseCases.kt:20`; el flujo real escribe vía `historyRepository.add()` en `PipelineOverlayDataSource.kt:185`.
- **Riesgo**: Bajo, pero revela inconsistencia de convenciones (ARC-1.2).
- **Severidad**: MEDIA
- **Recomendación**: Eliminar o hacer que el pipeline lo use para unificar.
- **Prioridad**: P2

### DEAD-9.3 — Clúster de métodos muertos en `DriverConfigRepository`/`GetDriverConfigUseCase`
- **Resumen**: 6 métodos de interface + 4 del usecase + 1 usecase forman un cluster muerto mantenido por tests.
- **Evidencia**: `DriverConfigRepository.kt:21-31` (`getDriverCosts`, `getDecisionThresholds`, `save(costs)`, `save(thresholds)`, `observeDriverCosts`, `observeDecisionThresholds`) y `GetDriverConfigUseCase.kt:19-25` — solo los usan `EvaluateOfferUseCase` (muerto) y tests (`PipelineOverlayDataSourceTest.kt:261-271`).
- **Riesgo**: Medio. Contratos inflados que invitan a usos incorrectos.
- **Severidad**: MEDIA
- **Recomendación**: Reducir el contrato a `DriverConfig` completo; ajustar tests.
- **Prioridad**: P2

### DEAD-9.4 — Otros elementos muertos
- **Resumen**: Varios símbolos sin consumidores productivos.
- **Evidencia**: `OverlayConfig.activeIndicatorCount` (`OverlayConfig.kt:22`); `HomeViewModel.projectionActive` standalone (`HomeViewModel.kt:30`, duplicado en `UiState.projectionActive`); `flagReportViewIds` solicitado en `accessibility_service_config.xml:5` sin uso.
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: Eliminar o documentar.
- **Prioridad**: P3

---

## 10. Duplicación

### DUP-10.1 — `collectTexts()` idéntico en ambos servicios de accesibilidad
- **Resumen**: El 80% de la lógica de los dos servicios de accesibilidad es copia exacta.
- **Evidencia**: `SircAccessibilityService.kt:66-88` vs `CaptureAccessibilityService.kt:76-98` — mismo recorrido, mismos límites `MAX_NODES=400, MAX_TEXT_LENGTH=200, MAX_TEXTS=80`, mismo dedup por fingerprint (`:45-47` vs `:58-60`).
- **Riesgo**: Alto. Corrección divergente (se arregla un límite en uno y no en otro); doble trabajo en cada evento.
- **Severidad**: ALTA
- **Recomendación**: Extraer `AccessibilityTreeTextCollector` compartido, o eliminar un servicio (ARC-1.1).
- **Prioridad**: P1

### DUP-10.2 — Doble persistencia del historial de ofertas
- **Resumen**: Los datos de una oferta se escriben en 3 estructuras (2 en memoria).
- **Evidencia**: `PipelineOverlayDataSource.persist()` escribe `evaluationRepository.add(OfferEvaluationRecord)` (in-memory, con `ocrText`/`parserResult`) y `historyRepository.add(OfferHistoryEntry)` (Room, resumido); `InMemoryCaptureRepository` guarda además los snapshots. El historial Room carece del texto OCR/raw del parser.
- **Riesgo**: Alto (pérdida de datos de depuración tras reinicio; consistencia).
- **Severidad**: ALTA
- **Recomendación**: Persistir un solo registro completo en Room con campos raw opcionales; la capa in-memory pasa a ser caché.
- **Prioridad**: P1

### DUP-10.3 — Filtrado de plataformas redundante en 3 lugares
- **Resumen**: El filtrado por plataforma se hace en XML y en ambos servicios.
- **Evidencia**: `accessibility_service_config.xml:11` (`packageNames`) + `SircAccessibilityService.kt:33` + `CaptureAccessibilityService.kt:46` (ambos `RidePlatform.fromPackageName`).
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: Mantener el XML como fuente de filtrado; los servicios como protección defensiva documentada.
- **Prioridad**: P3

### DUP-10.4 — Lógica de fechas duplicada
- **Resumen**: El cálculo de rangos de fecha (Hoy/7/30 días) se implementa en dos lugares con semántica que puede divergir.
- **Evidencia**: `HistoryViewModel.applyPreset` (`HistoryViewModel.kt:90-100`) vs `HistoryScreen.presetRangeStart` (`HistoryScreen.kt:190-205`); además `HistoryStatsCalculator.startOfDay` (`:74-82`) y `HistoryViewModel.startOfDay` (`:118-124`) usan `Calendar` con timezone por defecto.
- **Riesgo**: Medio. Agrupación por día no determinista entre dispositivos; chips y filtros pueden divergir.
- **Severidad**: MEDIA
- **Recomendación**: Una sola fuente en el VM usando `java.time` con zona explícita.
- **Prioridad**: P2

### DUP-10.5 — Lógica de refresh en 3 pantallas
- **Resumen**: El bloque `DisposableEffect` + `LifecycleEventObserver` "refresh on ON_RESUME" está copiado.
- **Evidencia**: `HomeScreen.kt:45-52`, `DiagnosisScreen.kt:50-57`, `DebugPanelScreen.kt:56-63`.
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: Extraer helper `onResume { viewModel.refresh() }`.
- **Prioridad**: P3

### DUP-10.6 — Umbrales y límites duplicados entre motores
- **Resumen**: Dos subsistemas de decisión con constantes duplicadas y salida que puede discrepar.
- **Evidencia**: `DecisionThresholds.default()` (4.0, 120.0) vs `RuleThresholds.DEFAULT_MIN_PROFIT_PER_KM/HOUR` (`RuleThresholds.kt:41-42`); límites 500/5000 duplicados en `ConfidenceEngine.kt:134-135` y `OfferValidator.kt:75-76`; `ProfitEngine.decide()` (`ProfitEngine.kt:62-68`) duplica el FAIL de las reglas mínimas. El pipeline corre ambos (`PipelineOverlayDataSource.kt:126,216-222`) y `RuleEvaluation.allPassed` nunca gatea la decisión.
- **Riesgo**: Alto. Dos motores pueden mostrar decisiones distintas para la misma oferta.
- **Severidad**: ALTA
- **Recomendación**: `RuleThresholds` derivado de `DecisionThresholds`; un único objeto de límites de cordura; decidir si `RuleEngine` es la autoridad única de decisión.
- **Prioridad**: P1

---

## 11. Escalabilidad: añadir Lyft, DiDi, Bolt, Cabify, InDrive

### SCA-11.1 — [CRITICA] La detección de pantalla es vocabulario español + Uber (eslabón roto)
- **Resumen**: `ScreenType` es genérico, pero las reglas de detección son una única lista global en español con términos Uber; pantallas de DiDi/Bolt/Lyft con otro texto se clasifican `UNKNOWN` y jamás se parsean, aunque su extractor genérico exista.
- **Evidencia**: `core/platform/.../OfferDetectionEngine.kt:48-56` (reglas globales sin plataforma), `:62-85` (`requestKeywords = ["nueva solicitud","toca para aceptar","ganancia estimada","radar","explorar","uber moto","uber xl", ...]`); `OfferParserOrchestrator.kt:36-38` descarta todo lo que no sea `isRequest`. El test de DiDi (`OfferParserOrchestratorTest.kt:135-146`) usa texto "Nueva solicitud" (vocabulario Uber-es), enmascarando el problema.
- **Riesgo**: Crítico. La afirmación "agregar plataformas sin romper nada" es falsa para detección: el extractor genérico de DiDi/Cabify/InDrive nunca se invoca si la pantalla no matchea keywords Uber-es.
- **Severidad**: CRITICA
- **Recomendación**: Reglas de detección por plataforma (o por idioma) alimentadas desde `PlatformDescriptors`; incluir frases reales de cada app ("solicitud entrante", "aceptar servicio", "new order", "accept").
- **Prioridad**: P0

### SCA-11.2 — `OfferParserOrchestrator` bloquea parsers especializados con `if (platform == UBER)`
- **Resumen**: La selección de la familia de parsers especializados está hardcodeada a Uber; ninguna otra plataforma puede usar parsers especializados sin tocar el flujo.
- **Evidencia**: `OfferParserOrchestrator.kt:41` → `if (platform == RidePlatform.UBER) { for (parser in specializedParsers) ... }`; luego `registry.forPlatform(platform)`.
- **Riesgo**: Alto. Para agregar parsers especializados de Bolt/DiDi hay que reescribir el flujo; acoplamiento por valor de enum.
- **Severidad**: ALTA
- **Recomendación**: `Map<RidePlatform, List<OfferTypeParser>>` o que cada parser declare `val platform: RidePlatform`; eliminar el `if (platform == Uber)`.
- **Prioridad**: P1

### SCA-11.3 — `BaseOfferTypeParser` acopla TODOS los parsers especializados a Uber
- **Resumen**: La clase base crea por defecto un extractor Uber; no hay forma de construir un parser especializado para otra plataforma sin violar el diseño.
- **Evidencia**: `core/platform/.../SpecializedParsers.kt:15-18` → `abstract class BaseOfferTypeParser(private val extractor: GenericPlatformExtractor = GenericPlatformExtractor(RidePlatform.UBER, PlatformDescriptors.UBER))`.
- **Riesgo**: Alto. Un futuro `BoltXlParser` sin extractor explícito se contamina silenciosamente con Uber.
- **Severidad**: ALTA
- **Recomendación**: Inyectar el extractor por constructor (requerido) o parametrizar por `platform + keywords` explícitos; eliminar el default Uber.
- **Prioridad**: P1

### SCA-11.4 — Faltan Lyft y Bolt en el enum; un solo package por plataforma
- **Resumen**: El enum solo tiene UBER/DIDI/CABIFY/INDRIVE y asume un único package por plataforma (ignora apps driver separadas).
- **Evidencia**: `domain/.../model/RidePlatform.kt:9-17` (`UBER("com.ubercab")`, `DIDI("com.didiglobal.passenger")`, `CABIFY("com.cabify.rider")`, `INDRIVE("com.leadingsoft.ride.driver")`); `:20` → `entries.firstOrNull { it.packageName == packageName }`. Paquetes driver no cubiertos (p. ej. `ee.mtakso.client.driver`, `com.didiglobal.driver`).
- **Riesgo**: Medio. Conductor usando app driver no captura nada; agregar Lyft/Bolt exige enum + XML + manifest.
- **Severidad**: MEDIA
- **Recomendación**: Añadir `LYFT("com.lyft.android")` y `BOLT("ee.mtakso.client")`; modelar `packageNames: List<String>`.
- **Prioridad**: P1

### SCA-11.5 — `DEFAULT_CURRENCY` fuerza una moneda por plataforma independiente del país
- **Resumen**: El extractor asigna moneda fija por plataforma, incorrecta para la mayoría de mercados.
- **Evidencia**: `core/platform/.../PlatformExtractors.kt:101-107` (`DEFAULT_CURRENCY = mapOf(UBER to "MXN", DIDI to "MXN", CABIFY to "EUR", INDRIVE to "MXN")`) usado en `:62`.
- **Riesgo**: Alto. Uber en India/Egipto daría MXN; ganancia e historial contaminados en silencio.
- **Severidad**: ALTA
- **Recomendación**: Usar `DriverProfile.currency` (ya existe) o inferir del código detectado; eliminar el default por plataforma.
- **Prioridad**: P1

### SCA-11.6 — `OfferType` solo tiene variantes `UBER_*`
- **Resumen**: El enum de tipos de oferta es 100% Uber; otras plataformas quedan siempre en `GENERIC`.
- **Evidencia**: `core/platform/.../OfferType.kt:9-27` (`UBER_REQUEST, UBER_RADAR, UBER_RESERVATION, UBER_MOTO, UBER_XL, GENERIC`).
- **Riesgo**: Medio. Historial de DiDi/Bolt siempre `GENERIC`; se pierde granularidad (radar/reserva/moto).
- **Severidad**: MEDIA
- **Recomendación**: Generalizar a `OfferType(platform, variant)` o prefijos por plataforma.
- **Prioridad**: P2

### SCA-11.7 — `HistoryStats` agrega sin agrupar por plataforma y mezcla monedas
- **Resumen**: El dashboard suma ganancias de todas las plataformas; con Cabify (EUR) y Uber (MXN) suma monedas distintas.
- **Evidencia**: `domain/.../usecase/HistoryStatsCalculator.kt:11-58` → `totalProfit = entries.map { it.estimatedProfit }.sum()`; `HistoryStats.kt:16-28` sin campo por plataforma.
- **Riesgo**: Alto (integridad de datos en multi-plataforma).
- **Severidad**: ALTA
- **Recomendación**: Normalizar a la moneda del perfil antes de sumar; añadir `HistoryStats.byPlatform`.
- **Prioridad**: P1

### SCA-11.8 — Tres fuentes de verdad del package list deben sincronizarse
- **Resumen**: La lista de paquetes vive en enum, XML de accesibilidad y manifest `<queries>`; olvidar una rompe la captura en silencio.
- **Evidencia**: `RidePlatform.kt:13-16`; `feature/overlay/.../accessibility_service_config.xml:11`; `app/src/main/AndroidManifest.xml:4-9`.
- **Riesgo**: Medio. Al agregar Bolt/Lyft, si se toca el enum pero no el XML, el servicio nunca recibe eventos de esa app.
- **Severidad**: MEDIA
- **Recomendación**: Documentar el checklist o generar `packageNames`/`queries` desde el enum en build (placeholder de manifiesto).
- **Prioridad**: P2

### SCA-11.9 — Keywords es/en únicamente; normalización sin soporte a otros scripts
- **Resumen**: Descriptores y detección solo tienen tokens en español (más algo de inglés); `normalize()` solo quita acentos latinos.
- **Evidencia**: `PlatformExtractors.kt:19-40` (`totalKeywords=["total","recibe","neto","cobro","ingreso","pago"]`); `OfferDetectionEngine.kt:152-166` (`STRIP_ACCENTS = {á→a, ñ→n}`); regex de divisas limitado (`OfferTextParser.kt:116-136`).
- **Riesgo**: Medio. Mercados de Bolt (Rusia/África/India) y DiDi (BR) con precisión baja.
- **Severidad**: MEDIA
- **Recomendación**: Estructurar `PlatformKeywords` por idioma (`Map<String, PlatformKeywords>`); normalización con `java.text.Normalizer(NFD)`.
- **Prioridad**: P2

### SCA-11.10 — Checklist de adición de plataformas (alto) → hoy 6-7 puntos manuales
- **Resumen**: Agregar una plataforma con extractor genérico exige tocar enum, descriptor, registry, moneda, XML y manifest; no es auto-registro.
- **Evidencia**: `core/platform/.../PlatformExtractors.kt:111-120` → `mapOf(...)` manual con `getValue(platform)` que **lanza** si falta la entrada.
- **Riesgo**: Medio. `NoSuchElementException` en runtime (no en compilación) si se olvida una entrada.
- **Severidad**: MEDIA
- **Recomendación**: Construir el mapa declarativamente desde `RidePlatform.entries` + `PlatformDescriptors`; fallback a `UNKNOWN` en vez de lanzar.
- **Prioridad**: P2

### SCA-11.11 — UI neutral (positivo)
- **Resumen**: Overlay, historial y detalles usan `platform.displayName`; agregar plataformas se refleja automáticamente sin cambios de UI.
- **Evidencia**: `OverlayContent.kt:91` → `title = evaluation?.offer?.platform?.displayName ?: "SIRC"`; `HistoryScreen.kt:313` → `entry.platform.displayName`.
- **Riesgo**: Ninguno.
- **Severidad**: BAJA (positivo)
- **Recomendación**: Mantener; externalizar `displayName` a recursos si se localiza.
- **Prioridad**: P3

### SCA-11.12 — Texto de Home con lista hardcodeada de plataformas
- **Resumen**: Un texto enumera "Uber, DiDi, Cabify e InDrive" manualmente; quedará desactualizado.
- **Evidencia**: `app/.../HomeScreen.kt:173`.
- **Riesgo**: Bajo (cosmético).
- **Severidad**: BAJA
- **Recomendación**: Generar la lista desde `RidePlatform.entries.map { it.displayName }` o recurso.
- **Prioridad**: P3

---

## 12. FORTALEZAS (evidencia verificada)

1. **Dominio puro**: `:domain` es `kotlin.jvm` sin Android (`domain/build.gradle.kts:1-21`); grep de imports `android.*|androidx.*|com.google.*` en `domain/src` = 0 coincidencias. Motores puros y 100% testeables.
2. **Dirección de dependencias correcta**: `data → domain`, `core/* → domain`, ninguna feature depende de otra feature a nivel de módulo (`build.gradle.kts` de los 11 módulos verificados).
3. **Motor de reglas bien diseñado**: `RuleEngine` con `Rule`/`RuleContext`/`RuleVerdict` y `RuleMessages` centralizado; las reglas sin datos devuelven PASS en vez de penalizar (`MaximumDistanceRule.kt:19`).
4. **Tema centralizado**: `core:ui` con tokens (`SircTheme`, `SircColors`, `SircSpacing`, `SircElevations`) y componentes presentacionales desacoplados del dominio (`OverlayCard`, `MetricCell`, `StatusDot`).
5. **Cobertura de tests notable**: 26 archivos de test unitario (engines, pipeline, parser, scheduler, coordinador, validación, mappers, data source) con dobles limpios.
6. **Protección de batería/rendimiento**: límites duros de accesibilidad (400 nodos/200 chars/80 textos), debounce 400 ms, dedup por fingerprint, caché LRU 32, métricas por etapa.
7. **Room disciplinado**: `exportSchema=true`, esquemas versionados comprometidos (`data/schemas/`), migraciones 1→2 y 2→3 explícitas, test de migración real (`SircDatabaseMigrationTest.kt`).
8. **Identidad de plataforma explícita en toda la cadena**: `packageName → RidePlatform → TripOffer → OfferSnapshot → OfferHistoryEntry → Room` (positivo, SCA-11.x).
9. **Feature flags configurables en runtime** y **logging centralizado** con `AndroidSircLogger` limitado a debug.
10. **Docs de alta calidad** en español (KDoc descriptivo, `docs/ARCHITECTURE.md`, decisión table).

---

## 13. Plan de remediación priorizado

| Prioridad | Acción | Hallazgos |
|---|---|---|
| P0 | Eliminar el pipeline legacy (SircAccessibilityService, coordinator, observer, bindings); unificar en `DefaultCapturePipeline` | ARC-1.1, DUP-10.1, PKG-7.1 |
| P0 | Detección de pantalla por plataforma en `OfferDetectionEngine` (desacoplar de Uber/es) | SCA-11.1 |
| P1 | Fix `isConfigured()` + guardas de onboarding | ARC-1.3 |
| P1 | Gating de Debug por `BuildConfig.DEBUG` y flags por defecto off en release | AND-2.1 (ver ANDROID_AUDIT) |
| P1 | Extraer `Analyzer`/`Persister`/`Scheduler` de `PipelineOverlayDataSource` | SOL-2.1 |
| P1 | `Map<RidePlatform, List<OfferTypeParser>>` + eliminar `if(platform==UBER)` + base sin default Uber | SCA-11.2, SCA-11.3 |
| P1 | Moneda desde perfil (no por plataforma) + normalización de stats | SCA-11.5, SCA-11.7 |
| P1 | Unificar umbrales/límites entre motores | DUP-10.6 |
| P1 | Mover `FakeParser` a test; eliminar use-cases muertos; quitar `:data` de features | ABS-8.2, DEAD-9.x, ARC-1.4 |
| P2 | Formatters en `core:ui`; mover repos in-memory a `:data`; DI de infra a core/app | PKG-7.2, MOD-3.2, MOD-3.3 |

---

*Fin de ARCHITECTURE_AUDIT.md. Documento derivado de la evidencia del código; sin modificaciones realizadas.*

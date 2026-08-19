# SIRC — Contexto para agentes de IA

> Resumen de arranque para cualquier sesión de IA. Léelo junto con
> `docs/PROJECT.md`, `docs/ARCHITECTURE.md` y `.ai/RULES.md` antes de tocar
> código. Actualiza este archivo cuando el proyecto cambie de forma relevante.

## Qué es el proyecto

**SIRC (Sistema Inteligente de Rentabilidad para Conductores)** es una app
Android nativa (Kotlin, Jetpack Compose, Material 3) que ayuda a conductores de
Uber, DiDi, Cabify e InDrive a decidir en **<1 segundo** si una oferta de viaje
es rentable (objetivo UX; `<3 s` es el límite técnico/E2E histórico).

> **Modelo de negocio y localidad (Roadmap Gate, 16-ago-2026)**:
> SIRC pasa a ser **aplicación de pago por suscripción** y el modelo de
> datos pasa de "100 % local" a **LOCAL-FIRST**: el procesamiento de ofertas
> (OCR, parsing, evaluación, overlay, historial) sigue siendo **100 % local** y
> ninguna oferta/dato de pantalla sale del dispositivo; identidad, suscripción,
> entitlement, integridad (Play Integrity) y recuperación usarán **servicios
> remotos mínimos** (etapa E1b, aún no implementados). El APK se considera
> manipulable: nunca confiar en el cliente para autorización premium (ver
> `docs/SECURITY_MODEL.md`, `docs/BETA_READINESS.md`, reglas 9d–9f).
>
> > **LOOP Backend (16-ago-2026)** — detalles en `docs/BACKEND_ARCHITECTURE.md`,
> > `docs/SUBSCRIPTION_MODEL.md`, `docs/ANTIGRAVITY_EVALUATION.md`, reglas
> > 9g–9h y R17, decisiones D14.1–D14.4:
> > - **Backend inicial = Supabase** (Auth + RLS + Edge Functions + Postgres)
> >   para identidad/suscripción/entitlement; **Pro** en producción. Nunca
> >   service keys en el APK. Ninguna oferta/pantalla se sube (local-first).
> > - **Verificación de compra**: Play API **v2** (`purchases.subscriptionsv2.get`,
> >   la `subscriptions.get` está deprecada); RTDN = señal (re-consultar API,
> >   dedupe `messageId`); entitlement offline con TTL 24–72 h; threat model
> >   **T1–T20**.
> > - **Herramientas**: **OpenCode principal + Antigravity complementario**
> >   (no sustituye; sin doble agente simultáneo en el mismo branch — regla R17).
> >
> > > **LOOP Modelo Free (16-ago-2026)** — decisiones **D15.1–D15.6**, reglas
> > > **9i–9j**; detalles en `docs/SUBSCRIPTION_MODEL.md` §1/§2 y
> > > `docs/BACKEND_ARCHITECTURE.md` §0/§2.5/§2.6:
> > > - **Modelo inicial = descarga gratuita + cuenta gratuita + plan FREE**
> > >   (entitlement `FREE` server-side, 0 €, revocable); monetización Premium
> > >   **progresiva (E3)**. Estructura de planes **FREE → PREMIUM** (niveles
> > >   intermedios eliminados; adicionales TBD).
> > > - **`FREE_INITIAL_MODEL = ENABLED`, `FREE_LIMITS = TBD`**: no se inventan
> > >   límites del Free (decisión explícita posterior). El Free **no relaja
> > >   seguridad** (D15.3, `SECURITY_MODEL.md` §5.5).
> > > - **Supabase ACCOUNT GATE (D15.4/9i)**: si se necesita crear/configurar
> > >   recursos reales de Supabase → **DETENERSE y pedir la configuración al
> > >   usuario** (guía §0.1). Nunca inventar credenciales. Dev local y tests en
> > >   verde sin backend (§2.6).
> > > - **Secretos**: client-safe (URL + publishable key) en el APK;
> > >   server-only (service_role, service account Play, claves privadas, keystore)
> > >   jamás en git/GitHub/APK/chat (D15.5, §2.5).
> > >
> > > > **LOOP Modelo Comercial TRIAL → SUSCRIPCIÓN (16-ago-2026)** — decisiones
> > > > **D16.1–D16.6**, reglas **9j–9m**; detalle en `docs/SUBSCRIPTION_MODEL.md`
> > > > §1–§5bis y `docs/PRODUCT_COMPETITIVE_ANALYSIS.md` §5:
> > > > - **Modelo definitivo**: descarga gratuita + **cuenta gratuita** + **TRIAL
> > > >   Premium completo 14 días** → **suscripción Weekly/Monthly/Annual**
> > > >   (`FREE_TRIAL = 14 DAYS`, `TRIAL_ACCESS = FULL_PREMIUM`,
> > > >   `POST_TRIAL = SUBSCRIPTION_REQUIRED`). **No hay Free Premium
> > > >   permanente**; `FREE_LIMITS` **eliminado** (D15.1/D15.2 superadas).
> > > > - **Precios**: USD referencia; **Google Play regionaliza** (autoridad
> > > >   comercial; sin conversión manual/reloj local). Definitivos NO fijados
> > > >   (matriz §5bis); pricing evolutivo + grandfathering (D16.5, regla 9k).
> > > > - **Entitlement**: `TRIAL_ACTIVE`/`TRIAL_EXPIRED`/`PREMIUM_ACTIVE`/
> > > >   `SUBSCRIPTION_EXPIRED`/`ACCOUNT_RESTRICTED`/`ACCOUNT_UNKNOWN`;
> > > >   server-side; **trial anti-abuso** (§6.1bis). Objetivo UX: **<1 s**
> > > >   (`<3 s` límite E2E); paywall local sin espera de red.

**Cómo funciona (flujo real):**

> Nota SPRINT 10: **hardening RC1** (v1.0.0-rc1). **Modo de validación**:
> `ValidationRecorder` (`:core:capture`, puro, buffer 500) registra
> `CaptureError`/`OcrFailed`/`ParseFailed`/`FrameDiscarded`
> (`CAPTURE_FAILED`/`DUPLICATE`/`NO_TEXTS`/`UNSUPPORTED_PLATFORM`)/
> `RuleFailed`/`OfferRejected`; el Panel de depuración tiene la sección **Modo
> validación** con contadores, **Exportar informe de validación** y Limpiar; el
> informe se adjunta a "Exportar diagnóstico". **Recuperación**: si el OCR
> falla el pipeline degrada a textos de accesibilidad (ya no entra en `ERROR`);
> los fallos no controlados registran `CaptureError`. **Logs por niveles**:
> `ERROR`/`WARNING` siempre (también Release), `INFO`/`DEBUG` solo en
> desarrollo (`DEBUG` requiere `DETAILED_LOGS`). **Android 15**:
> `screenBounds()` con `WindowManager.getCurrentWindowMetrics()` (API 30+) y
> fallback 24–29. **Auditoría**: eliminados `OfferEvaluator`/`OfferEventBus`
> (historial duplicado); `SircAccessibilityService` conserva solo el reenvío de
> eventos. Docs: `RELEASE_NOTES_RC1.md`, `KNOWN_ISSUES.md`,
> `PERFORMANCE_REPORT.md`, `TEST_REPORT.md`.
>
> Nota SPRINT 9: preparación de la **beta cerrada** (v1.0.0-beta). Nueva
> **sesión de captura** (`CaptureSessionManager` + `SessionStatus` +
> `SessionStats`, reloj inyectable) alimentada por el pipeline/overlay
> (inicia en cada snapshot, registra decisión/error). El **historial ahora se
> persiste en Room con análisis detallado**: `OfferHistoryEntry` gana tipo de
> oferta, confianza, reglas, motivos, recomendación y tiempos; base v3 con
> migración 1→3 y `overlay_config.historyLimit` (default 500, configurable en
> Ajustes); `OfferHistoryDao.trimToLimit` recorta automáticamente.
> **Historial** en `:feature:history` con filtros (`HistoryFilters`/
> `HistoryFilter` en `:domain`), búsqueda, presets de fecha y detalle en
> diálogo. Nuevo **Dashboard** (`StatsViewModel` + `StatsScreen` con gráficos
> Canvas) alimentado por `HistoryStatsCalculator`. **Modo Beta**: flags
> `RULES`/`DETAILED_LOGS`/`METRICS` y **Exportar diagnóstico** (share).
> **OverlayService** reescrito (vista única persistente, `FLAG_NOT_FOCUSABLE`,
> `onConfigurationChanged`); `OverlayContent` con animaciones y crossfade;
> MediaProjection recrea el virtual display ante cambios de configuración.
> Desde FIX-01 (SPRINT 12): la ventana es un **banner** (`WRAP_CONTENT`, sin
> `fillMaxSize`) que no bloquea los toques de la app, `PipelineOverlayDataSource
> .start()` activa el estado `WAITING` (indicador "Esperando oferta…" real al
> encender el overlay), los fallos de `addView`/`updateViewLayout` se registran
> con `logger.error/warn` y `isRunning` se corrige con el estado real del
> servicio vía `OverlayController.onServiceRunning()` (extraído
> `OverlayServiceLauncher` para testear el controller sin Android).
> Desde WP-12-UI-01 (SPRINT 12): el contenido evaluado del overlay usa una
> **jerarquía de 4 niveles** (decisión dominante en banner semáforo con
> etiqueta, oferta monto+resumen, métricas en filas de 2 columnas con ancho
> repartido y textos con ellipsis, secundaria en una sola línea). La lógica de
> presentación vive en `OverlayPresentation.mapToOverlayPresentation`
> (mapper puro, 22 tests); `OverlayContent` solo renderiza el modelo. Se
> respetan los indicadores de `OverlayConfig` y se conservan animaciones,
> drag y `FLAG_NOT_TOUCHABLE` (oculto).
>
> Nota SPRINT 7: la oferta capturada se evalúa en detalle y el overlay muestra
> una **recomendación accionable**. `PipelineOverlayDataSource` mapea el snapshot
> a `TripOffer` (con los textos OCR), lo evalúa con `EvaluateDetailedOfferUseCase`
> (`ProfitEvaluationEngine` delega en `ProfitEngine` y deriva los costos desde
> `DriverConfig`; los umbrales vienen **solo de `DriverConfig.thresholds`**) y
> publica `evaluation` + `recommendation` (`ACCEPT`/`REJECT`/`WARNING` con motivo
> principal y % de confianza). Cada oferta se persiste en
> `OfferEvaluationRepository` (en memoria, últimas 100) y se cronometra con
> `OfferPerformanceTracker` (promedio de las últimas 20). El panel de depuración
> muestra **Última oferta** y **Rendimiento**.
>
> Nota SPRINT 12 (WP-12-CALC-03, D17.3): el motor separa **ganancia real** de
> **objetivo**. El costo real es `costPerTrip + distance×costPerKm` (sin
> `costPerMinute`, eliminado; `costPerTrip` = "Costo fijo por viaje" editable,
> default 0; `costPerKm` sigue derivado). `profitPerKm`/`profitPerHour` son
> null si falta distancia/duración (nunca se inventan); decisión por jerarquía:
> ganancia < 0 → REJECT; dato faltante → MARGINAL/WARNING; ambas métricas ≥
> umbral → ACCEPT; break-even → MARGINAL. El overlay oculta celdas sin datos y
> usa el tono del estado de decisión.
>
> Nota SPRINT 8: el parser es un **motor de análisis** de pantallas reales de
> Uber. `OfferParserOrchestrator` (`:core:platform`) primero **detecta la
> pantalla** (`OfferDetectionEngine` → `ScreenType`; solo `REQUEST` produce
> oferta) y luego prueba parsers **especializados por tipo** (`UBER_REQUEST`,
> `UBER_MOTO`, `UBER_XL`, `UBER_RESERVATION`, `UBER_RADAR`) antes de caer al
> extractor genérico por plataforma. Tras evaluar, `PipelineOverlayDataSource`
> ejecuta **`RuleEngine`** (6 reglas con umbrales desde `DriverConfig`) y
> **`ConfidenceEngine`** (HIGH/MEDIUM/LOW) y expone en `OverlayUiState`
> `offerType`, `confidence` y `ruleEvaluation`; el overlay muestra tipo +
> confianza y "Información insuficiente" si no es accionable. El panel de
> depuración tiene la sección **Análisis** (tipo, confianza, veredicto por
> regla). Se cronometra detección y reglas (`detectionMillis`/`rulesMillis`).
>
> Nota SPRINT 6: el overlay consume el **estado real del pipeline**.
> `PipelineOverlayDataSource` traduce `CapturePipeline.state` (`OverlayState`)
> y los snapshots a `OverlayUiState` (status + evaluación con el motor real);
> sustituyó a `SimulatedOverlayDataSource`. La captura de pantalla real usa
> **MediaProjection** (`:core:capture:android`): `ScreenCaptureProvider` +
> FGS tipo `mediaProjection`; el pipeline aplica OCR a la imagen real y degrada
> al texto de accesibilidad si no hay proyección. Hay debounce de requests
> (`DebounceCaptureScheduler`, 400 ms), caché de frames por hash de contenido y
> métricas por etapa en el panel de depuración.
>
> Nota SPRINT 2: el overlay ya NO se alimenta de datos simulados (sustituido en
> SPRINT 6 por `PipelineOverlayDataSource`). El flujo de accesibilidad persiste
> el historial y el overlay muestra ahora el estado real de la captura.
>
> Nota SPRINT 4: la captura observa cambios de ventana y produce snapshots
> simulados para validar el flujo; el parser real aún no existía en ese momento
> (el fake se conservaba temporalmente). `SircLogger` solo emite en builds de
> desarrollo. **WP-E1-01 (SPRINT 11)**: `FakeParser` eliminado de producción;
> `PlatformOfferParser` es la única fuente de análisis.
> **WP-E1-02 (SPRINT 11)**: `RuleEngine` eliminado de la ruta de producción;
> `ProfitEngine` es el único motor de decisión. Marcado como LEGACY en `:domain`.
> **WP-E2-02 (SPRINT 11)**: ciclo de vida de `ScreenCaptureProvider` fortalecido
> con `ProjectionLifecycle` como única fuente de verdad interna (`IDLE`, `INITIALIZING`,
> `ACTIVE`). Inicialización atómica con rollback en try/catch y generation tokens en KDoc
> que aíslan callbacks tardíos de sesiones previas. `isProjecting` se deriva estrictamente
> de `lifecycle.isActive`.
> **WP-E2-01 (SPRINT 11)**: limpieza determinista de MediaProjection en
> `MediaProjectionService.onDestroy()` → `provider.onServiceDestroyed()` (release
> idempotente de `MediaProjection`/`VirtualDisplay`/`ImageReader`/callback/frames).
> El listener del `ImageReader` ignora callbacks posteriores al cierre. El módulo
> `:core:capture:android` configura `AndroidJUnitRunner` para ejecutar sus tests
> instrumentados JUnit4 (antes usaba el runner legacy y no los descubría).
> **WP-E3-01 (SPRINT 11)**: el motor de análisis es 100 % descriptor-driven.
> `PlatformDescriptor` reúne por plataforma detección, tipos de oferta
> (`OfferTypeVariant`), keywords de extracción y moneda, con estructura
> preparada para subdescriptores futuros. `PlatformDescriptorRegistry` es el
> único validador (falla en construcción, nunca en parseo) y precompila
> motores/parsers/extractores. Se eliminan los parsers especializados
> (`UberRequestParser`, etc.) y `ExtractorRegistry`; el objeto
> `PlatformDescriptors` se conserva como fuente de descriptores.
> `OfferParserOrchestrator` resuelve todo desde el registry
> sin ramas por plataforma. `PlatformModule` provee
> `PlatformDescriptorRegistry(PlatformDescriptors.all())`.
> **WP-E3-02 (SPRINT 11)**: framework genérico de detección. `PlatformDetectionEngine`
> recorre los descriptores del registry (O(n), una sola pasada) y resuelve la
> plataforma por packageName (`PACKAGE_MATCH`) o por keywords (`KEYWORD_CANDIDATE`;
> `AMBIGUOUS` ante empate; `NONE` sin candidatos). `DetectionMatcher` es puro y sin
> estado; `DetectionResult` encapsula descriptor, `ScreenDetection`, `origin`
> (`DetectionOrigin`) y diagnóstico (`candidates`, `sourcePackage`).
> `OfferParserOrchestrator` añade `parse(texts, ts, packageName)` sin romper el
> método por `RidePlatform`. El registry solo expone una vista de solo lectura.
> Sin cambios de comportamiento; `:core:platform` sigue Kotlin puro.
> **WP-E3-03 (SPRINT 11)**: unified capture source. `CaptureInput` es la única
> abstracción de entrada de captura (`AccessibilityCaptureInput` +
> `MediaProjectionCaptureInput`) y el pipeline único `DefaultCapturePipeline`
> consume el merge `@CaptureRequests Flow<CaptureRequest>`. `DetectionOrigin` se
> renombra a `CaptureInputType` (valores aditivos: legacy
> PACKAGE/OCR/GALLERY/TEST/UNKNOWN + ACCESSIBILITY/MEDIA_PROJECTION/SHARE);
> `CaptureRequest` y `OfferSnapshot` llevan `origin`. Se eliminan
> `ScreenCapture`/`ScreenFrame`/`MediaProjectionScreenCapture`;
> `MediaProjectionCaptureInput` solo enriquece con `imageData` si proyecta
> (degrade a textos si no). `OfferParser.parse(request, result, detectionMillis)`
> y el overload `OfferParserOrchestrator.parse(result, texts, ts, det)` no
> dependen de `CaptureWindowEvent`. El coordinador debug consume
> `pipeline.snapshots` y ya no guarda (corrige el doble guardado de snapshots).
> `CaptureAccessibilityService` queda como adaptador delgado que delega en
> `AccessibilityCaptureInput`. Sin cambios de comportamiento; Gallery/Share
> futuros = otro `CaptureInput` en el merge.
>
> Nota SPRINT 5: `CaptureAccessibilityService` (desacoplado de la UI) alimenta
> `CapturePipeline` (ScreenCapture → OCR → OfferParser → CaptureRepository).
> ML Kit OCR está integrado bajo `OcrEngine`; el pipeline aplica OCR cuando la
> solicitud lleva imagen (SPRINT 6 la aporta vía MediaProjection).

1. `CaptureAccessibilityService` (único Accessibility Service, solo lectura)
   delega en `AccessibilityCaptureInput`: filtra por plataforma, recolecta los
   textos de pantalla (límites duros: 400 nodos, 80 textos, ≤200 chars,
   deduplicación por huella de texto), encola `CaptureRequest` (origin
   `ACCESSIBILITY`) en el `DebounceCaptureScheduler` (400 ms) y reenvía los
   cambios de ventana al panel de depuración sin interpretar nada.
2. El merge `@CaptureRequests` (accesibilidad + `MediaProjectionCaptureInput`,
   que añade `imageData` solo si proyecta y degrada a textos si no) alimenta el
   pipeline único `DefaultCapturePipeline`.
3. `DefaultCapturePipeline` aplica dedup por `imageData` → **OCR** (ML Kit, si
   hay imagen y flag `OCR`; degrada a textos si falla) →
   `PlatformDetectionEngine` (resolución de plataforma única por paquete/
   keywords) → `OfferParser`/`OfferParserOrchestrator` (descriptor-driven) y
   produce un `OfferSnapshot` (con `origin`) que se escribe una sola vez en el
   repositorio.
4. `PipelineOverlayDataSource` mapea el snapshot a `TripOffer`, lo evalúa con
   `EvaluateDetailedOfferUseCase` (`ProfitEvaluationEngine` + `RecommendationEngine`)
   y **confianza** (`ConfidenceEngine`), publica
   el overlay y persiste el historial (Room). Todo queda cronometrado
   (`OfferPerformanceTracker`) y los incidentes se registran en el
   `ValidationRecorder`. El contenedor `RuleEvaluation` se emite vacío
   (`RuleEvaluation(emptyList())`) por compatibilidad con la UI; no existe motor
   de reglas en producción.
5. `OverlayService` (Foreground, `TYPE_APPLICATION_OVERLAY`) dibuja un
   `ComposeView` liviano con la recomendación, métricas y semáforo.
6. Todo el análisis de ofertas es **100 % local** (LOCAL-FIRST): ninguna oferta
   o texto de pantalla sale del dispositivo. Los únicos servicios remotos
   previstos (E1b) son comerciales/de cuenta (suscripción, entitlement,
   integridad), nunca contenido de pantalla.

**Filosofía**: NO repetir la información que ya muestra la plataforma; solo
información derivada (ganancia, métricas) con colores semáforo. El Accessibility
Service **nunca** interactúa con otras apps.

## Dirección de producto (LEER antes de tocar código)

- **Ruta de producto consolidada**: `docs/PRODUCT_STRATEGY.md` (diferenciación
  ADOPTAR/MEJORAR/EVITAR/DIFERENCIAR, prioridades P0–P3, arquitectura futura,
  etapas E0–E4 con **E1a/E1b**). **Sprint 12
  = E1a: beta cerrada de validación del núcleo SIN monetización** (planificado,
  sin implementar); cuenta + trial 14 días + backend de cuenta + entitlement +
  Play Integrity + RTDN quedan en **E1b** (posterior, sobre datos de la beta).
- **Backend**: **Supabase** (Auth + RLS + Edge Functions + Postgres) para
  identidad/suscripción/entitlement — `docs/BACKEND_ARCHITECTURE.md`. Nada de
  ofertas/pantallas se sube. Plan **Pro** en producción. **ACCOUNT GATE**: si se
  necesitan credenciales reales → detenerse y pedir configuración al usuario
  (reglas 9i).
- **Modelo comercial (D16, vigente)**: **descarga gratuita + cuenta gratuita +
  TRIAL Premium completo de 14 días → suscripción Weekly/Monthly/Annual**;
  `FREE_TRIAL = 14 DAYS`, `TRIAL_ACCESS = FULL_PREMIUM`, `POST_TRIAL =
  SUBSCRIPTION_REQUIRED`; el trial se controla server-side (anti-abuso).
  USD referencia de pricing + regionalización por Google Play (precios
  definitivos no fijados). — `docs/SUBSCRIPTION_MODEL.md`.
- **Herramientas**: OpenCode principal + Antigravity complementario (evaluación
  en `docs/ANTIGRAVITY_EVALUATION.md`); prohibido doble-agente simultáneo en el
  mismo branch (regla R17).
- **Modelo comercial**: **apk de pago por suscripción**; el APK es manipulable.
  Autorización premium decide el backend, no el cliente (`docs/SECURITY_MODEL.md`,
  threat model T1–T20; reglas 9d/9e/9f). Modelo vigente: **descarga gratuita +
  trial 14 días completo → suscripción Weekly/Monthly/Annual** (D16, ver
  dirección de producto arriba).
- **LOCAL-FIRST** (reemplaza "100 % local"): procesamiento de ofertas 100 %
  local sin salida de datos de pantalla; servicios remotos mínimos solo para
  estado comercial/de cuenta.
- **Análisis competitivo verificado**: `docs/PRODUCT_COMPETITIVE_ANALYSIS.md`
  (Ruta Rentable, Motorista One, GigU, DecideRider verificados con precios y
  trials documentados; autoindrive/Maxymo/Mystro sin verificación).
- **Regla de hierro**: prohibida cualquier automatización de clics/gestos
  (auto-aceptar, contra-ofertas). SIRC es la herramienta legalista: solo
  lectura + <1 s (UX; <3 s E2E) + local-first (reglas 9b–9f en `.ai/RULES.md`).
- **Nada fuera de las prioridades P0–P3** sin aprobación explícita.

## Estado del proyecto

- **SPRINT 12 WP-12-CALC-03 completado y VALIDADO en DEVICE-01
  (modelo económico real vs objetivo, 18-ago-2026, decisión D17.3)**: los
  costos legacy `costPerMinute=0.30` y `costPerTrip=1.50` (sin UI tras
  FIX-03) dominaban el costo real y etiquetaban como pérdida ofertas que
  ganaban (caso real InDrive $5.90/27 min → NOT_PROFITABLE −$3.70). Con Q1–Q8
  autorizados: `costPerMinute` ELIMINADO del costo real; `costPerTrip` =
  "Costo fijo por viaje" editable (default 0); `costPerKm` sigue DERIVADO
  (FIX-03); objetivo ≠ costo (nunca se resta). `totalCost = costPerTrip +
  distance×costPerKm` (solo con distancia > 0); `profitPerKm`/`profitPerHour`
  null si falta distancia/duración (sin cifras falsas). Jerarquía: ganancia
  real < 0 → REJECT; falta distancia/duración → MARGINAL/WARNING; ambas ≥
  umbral → ACCEPT; break-even → MARGINAL. Dominio 56 tests verdes; DB v3→v4
  (`MIGRATION_3_4` recrea `driver_config` sin `costPerMinute` conservando
  `costPerTrip` y perfil; 8/8 connected tests en físico); Settings con "Costo
  fijo por viaje" editable y sección "Objetivos de ganancia"; overlay oculta
  celdas sin datos y tono por decisión. El caso real $5.90/27 min ya NO da
  pérdida artificial. Evidencia: `/sdcard/SIRC_TEST/evidence/calc03_*` +
  `{logs,images}/calc03_*`.
- **SPRINT 12 / E1a CERRADO como PASS WITH PENDING (18-ago-2026, auditoría
  de cierre; sin cambios de código)**: núcleo validado en físico (DEVICE-01,
  Infinix X6850/Android 15) con E2E real de InDrive Ecuador (captura
  accesibilidad → detección → parser → evaluación → overlay). P0 (DVC-03
  overlay, DVC-04 flujo normal) y Alta (DVC-01 config, K1 parser) CORREGIDOS
  y verificados (WP-12-FIX-01…05). Pendientes condicionados a cuentas reales/
  ruta/otros dispositivos (no a defectos): Uber Driver/DiDi/Cabify en vivo,
  muestra ≥20 por plataforma, <1 s en ruta, estabilidad 8 h, batería, ciclo
  de vida, DVC-02, PRV/SEC dinámicos. Hallazgos abiertos registrados
  (DVC-02, mecanismo debug, evidencia FIX-01 parcial, conocidos no
  bloqueantes) — ver `docs/testing/SPRINT_12_DEVICE_VALIDATION.md` §15 y
  decisión D17.1. NO se abre Sprint 13 ni monetización (E1b).
- **SPRINT 12 WP-12-FIX-05 completado (higiene de artefactos, 18-ago-2026)**:
  sin cambios de código de producción (solo device storage + docs). Convención
  vigente: todo artefacto de prueba en `/sdcard/SIRC_TEST/{images,logs,evidence,
  exports,tmp}/`. Se confirmó que `/sdcard/sirc_test` y `/sdcard/SIRC_TEST` son
  la MISMA carpeta (sdcard case-insensitive; sin duplicados). Reubicados 57
  archivos sin borrar (14→images, 2→logs, 3→evidence + fix03→evidence/fix03,
  38→tmp); raíz `/sdcard` limpia de `sirc_*` sueltos; 0 coincidencias `sirc` en
  carpetas personales; 57 antes = 57 después (integridad). Al terminar Sprint 12
  basta borrar la única carpeta `/sdcard/SIRC_TEST/`.
- **SPRINT 12 WP-12-FIX-03 completado (config editable post-onboarding, DVC-01)**:
  Settings edita TODO lo que el onboarding persiste (perfil, vehículo,
  combustible, mantenimiento, otros costos, plataformas, umbrales, overlay) con
  persistencia real en Room y efecto real en el motor. **costPerKm es DERIVADO**
  (única fuente de verdad: `fuelPrice/consumptionKmPerUnit +
  maintenanceCostPerKm + Σ additionalCosts.costPerKm`); la UI lo muestra como
  "Costo por km (calculado)" SOLO lectura y edita sus componentes; el motor
  ignora `costs.costPerKm` manual; al guardar `normalizeCostPerKm()` persiste la
  columna con el derivado. `AccessibilityCaptureInput` ahora aplica un **gate de
  plataformas** (solo procesa ofertas de plataformas activas; set vacío = acepta
  todas como fallback anti-regresión; logs info de rechazo). Tests nuevos:
  `SettingsViewModelTest` (6), `AccessibilityCaptureInputTest` (3),
  `ProfitEvaluationEngineTest` (+3). **2 bugs reales encontrados y corregidos en
  dispositivo**: (1) campos `rememberSaveable` no re-sembrados al cargar la
  config persistida (el campo "Año" mostraba 2020 con BD=2021) → `reloadTick` como
  resetKey en `NumericField`/`IntField`/`costDrafts`; (2) crash
  `Parcel: unknown type for value CostDraft` → `costDrafts` pasa de
  `rememberSaveable` a `remember(reloadTick)`. Validación física (Infinix X6850):
  derivado 0.5417 → combustible 0.5→1.5 → 0.625 en vivo → +costo "Peaje"/0.3 →
  0.925 en vivo; activar Cabify; ciudad Quito→Guayaquil; guardar → BD
  `costPerKm=0.925` normalizado, `fuelPrice=1.5`, `additionalCosts='Peaje^_0.3'`,
  `platforms='CABIFY,INDRIVE,UBER'`; tras force-stop/reopen TODO persiste. Suite
  AGENTS completa en verde. Evidencia: `/sdcard/SIRC_TEST/fix03/`.
- **SPRINT 12 WP-12-FIX-02 completado (captura E2E, DVC-04)**: el flujo real
  (accesibilidad/MediaProjection → OCR → detección → parser → evaluación →
  overlay) DEMOSTRADO en DEVICE-01 (Infinix X6850) con ofertas reales de InDrive
  Ecuador. Causa raíz de la ruta silenciosa: el servicio SÍ estaba
  bindeado/suscrito, pero `android:packageNames` de
  `accessibility_service_config.xml` filtraba a nivel sistema los eventos del
  paquete real de InDrive en Ecuador — **`sinet.startup.inDriver`** (hallazgo
  clave para el futuro soporte InDrive Ecuador; el dispositivo NO tiene
  `com.ubercab.driver`, solo `com.ubercab` pasajero). Cambios: config XML
  `packageNames` ampliado (Uber/DiDi/Cabify/InDrive + `sinet.startup.inDriver`),
  `PlatformDescriptors.kt` con `packageNames` por plataforma, e instrumentación
  con `SircLogger` (info/warn de rechazos y latencias) en
  `AccessibilityCaptureInput`, `DebounceCaptureScheduler` (logger obligatorio),
  `DefaultCapturePipeline`, `MediaProjectionCaptureInput` y
  `PipelineOverlayDataSource` (log "overlay mostrando:"). Evidencia en físico:
  `detección: INDRIVE / REQUEST`, `snapshot INDRIVE guardado: parse 5.7–13.9 ms ·
  total 24.7–40.9 ms`, `overlay mostrando: INDRIVE · $2.9/$3.1 · REJECT
  (origen=ACCESSIBILITY · eval 1.3–8.6 ms · reglas 0.0–0.7 ms · overlay
  8.8–15.6 ms)`, ventana overlay `Requested 885x280 · isVisible=true ·
  HAS_DRAWN`; Room `offer_history` con 74 ofertas reales (id 61–71 ACCEPT/WARNING
  de ofertas >$5). MediaProjection: 1 frame real enriquecido (300759 bytes PNG;
  OCR ~2.5 s en frame completo) y degradación a textos al denegarse
  (`PROJECT_MEDIA=ignore`). Tests nuevos: `DebounceCaptureSchedulerTest` (+1) y
  `PlatformDetectionEngineTest` (+2 seed `sinet.startup.inDriver`→INDRIVE /
  `com.ubercab.driver`→UBER). Suite AGENTS completa en verde. Evidencia en
  `/sdcard/SIRC_TEST/`. Hallazgos para el siguiente WP: panel Debug con sesión en
  memoria (se pierde al reiniciar SIRC), uiautomator "null root node" tras ciclo
  de overlay, OCR ~2.5 s en frames MediaProjection completos.
- **FIX Overlay (verificación en emulador)**: el overlay crasheaba en
  instalación limpia al iniciarse (regresión de `6d62dba`). Tres causas
  encadenadas: (1) `ViewTreeLifecycleOwner.set` recibía `LifecycleRegistry` en
  lugar de `LifecycleOwner`; (2) Compose 1.7.6 exige además
  `ViewTreeSavedStateRegistryOwner` (FATAL si falta); (3) las clases
  `androidx.savedstate` tampoco se resuelven en compile time desde
  `:feature:overlay` (metadata KMP). Fix en `OverlayService.kt`: `lifecycleOwner`
  como propiedad `lateinit` y propagación por reflexión de
  `ViewTreeLifecycleOwner` y `ViewTreeSavedStateRegistryOwner`; `SavedStateRegistry`
  creado por reflexión con `isRestored=true` (sin `SavedStateRegistryController`).
  Verificado en emulador: overlay inicia/detiene sin FATAL, MainActivity en
  primer plano, teclado OK en Ajustes. `ktlintCheck` global en verde (se
  corrigieron las violaciones preexistentes de `OverlayService.kt` y
  `ProjectionLifecycleTest.kt`).
- **SPRINT 11 WP-E3-01 completado**: Motor de análisis descriptor-driven.
  `PlatformDescriptor` (platform, packageNames, detectionRules, offerTypes,
  extractorKeywords, defaultCurrency) + `PlatformDescriptorRegistry` como único
  validador (construcción con `IllegalArgumentException`, nunca en parseo) y
  precompilador de motores/parsers/extractores. Eliminados parsers
  especializados (`SpecializedParsers.kt`) y `ExtractorRegistry`; el objeto
  `PlatformDescriptors` se conserva como fuente de descriptores.
  `OfferParserOrchestrator` es 100 % descriptor-driven y
  devuelve `ParsedOffer.none()` si la plataforma no está registrada.
  `PlatformModule` provee el registry con `PlatformDescriptors.all()`. Tests:
  13 nuevos de registro/validación; los 42 de `:core:platform` en verde.
  Verificación completa en verde (lintDebug, assembleDebug, unit tests).
- **SPRINT 11 WP-E3-02 completado**: Framework Genérico de Detección
  descriptor-driven en `:core:platform`. `PlatformDetectionEngine` +
  `DetectionMatcher` + `DetectionResult`; overload `parse(packageName)` en el
  orquestador; vista de solo lectura en el registry. Sin plataformas nuevas ni
  cambios de comportamiento.
- **SPRINT 11 WP-E3-03 completado**: Unified Capture Source. Pipeline único
  `CaptureInput → CaptureRequest → (OCR) → PlatformDetectionEngine →
  OfferParserOrchestrator → OfferSnapshot → Repository → Overlay`, eliminando
  `ScreenCapture`/`ScreenFrame`/`MediaProjectionScreenCapture` y la resolución
  de plataforma duplicada en el coordinador. `DetectionOrigin` renombrado a
  `CaptureInputType`; inputs `AccessibilityCaptureInput` (lógica del servicio
  extraída) y `MediaProjectionCaptureInput` (enriquece con imagen si proyecta).
  El coordinador consume `pipeline.snapshots` y ya no guarda. Verificación
  completa en verde (lintDebug, assembleDebug, unit tests, ktlintCheck).
- **SPRINT 11 WP-E2-02 completado**: Fortalecimiento del ciclo de vida de `ScreenCaptureProvider`.
  `ProjectionLifecycle` es la única fuente de verdad interna (`IDLE`, `INITIALIZING`,
  `ACTIVE`); `initializeProjection()` es atómico con rollback atómico y `ValidationEvent.CaptureError`.
  Generation tokens en KDoc garantizan que callbacks obsoletos de sesiones previas no afecten
  nuevas sesiones. Tests JVM puros para la máquina de estados en verde.
- **SPRINT 11 WP-E2-01 completado**: Limpieza determinista de recursos en
  MediaProjection. `MediaProjectionService.onDestroy()` delega en
  `provider.onServiceDestroyed()`, que libera de forma idempotente
  `MediaProjection`, `VirtualDisplay`, `ImageReader`, el callback y los frames
  pendientes; el listener del `ImageReader` descarta callbacks posteriores al
  cierre. El módulo `:core:capture:android` configura `AndroidJUnitRunner`
  (sus tests instrumentados JUnit4 no se ejecutaban con el runner legacy).
  Tests instrumentados nuevos de idempotencia (5/5 en emulador) y verificación
  JVM en verde.
- **SPRINT 11 WP-E1-02 completado**: Consolidación del motor de decisión.
  `RuleEngine` eliminado de la ruta de producción; `ProfitEngine` es el único
  motor de decisión. Eliminado feature flag `RULES`, providers de `RuleEngine`/
  `OfferRule` en `PlatformModule` y la condición `if (RULES)` en
  `PipelineOverlayDataSource`. `RuleEngine` marcado como LEGACY en `:domain`
  para uso en tests futuros. Comportamiento funcional idéntico.
- **SPRINT 10 completado** (v1.0.0-rc1): Hardening RC1 (ver `docs/ROADMAP.md`).
  **Modo de validación** (`ValidationRecorder` + sección en Debug + exportar
  informe). **Recuperación**: OCR degrada a textos, `CaptureError` registrado.
  **Logs por niveles** (ERROR/WARNING siempre; INFO/DEBUG en desarrollo).
  **Android 15**: `WindowMetrics` en `OverlayService`. **Auditoría**: eliminados
  `OfferEvaluator`/`OfferEventBus` (historial duplicado). Tests:
  `ValidationRecorderTest`, pipeline de validación y stress. Docs:
  `RELEASE_NOTES_RC1.md`/`KNOWN_ISSUES.md`/`PERFORMANCE_REPORT.md`/`TEST_REPORT.md`.
  Verificación en verde (ktlint, unit tests, lint, assembleDebug,
  assembleDebugAndroidTest).
- **SPRINT 9 completado** (v1.0.0-beta): Preparación beta cerrada (ver
  `docs/ROADMAP.md`). **Sesión** (`CaptureSessionManager`/`SessionStatus`/
  `SessionStats` en `:domain`). **Room v3** con `OfferHistoryEntry` ampliado
  (tipo/confianza/reglas/recomendación/tiempos), migración 1→3 y
  `overlay_config.historyLimit`; DAO con `trimToLimit`. **Historial** con
  filtros/búsqueda/presets/detalle (`HistoryFilters`/`HistoryFilter`).
  **Dashboard** con `HistoryStatsCalculator` + `StatsScreen` (Canvas).
  **Modo Beta**: flags `RULES`/`DETAILED_LOGS`/`METRICS` y exportar
  diagnóstico. **Overlay mejorado**: `OverlayService` de vista única +
  `OverlayContent` animado. **Estabilidad**: `onConfigurationChanged` en
  overlay y MediaProjection. Tests: `CaptureSessionManagerTest`,
  `HistoryFilterTest`, `HistoryStatsCalculatorTest`,
  `PipelineOverlayDataSourceTest` ampliado, `OfferHistoryDaoTest` y
  `SircDatabaseMigrationTest`. Verificación en verde (ktlint, unit tests,
  lint, assembleDebug, assembleDebugAndroidTest). Documentos:
  `docs/testing/BETA_TEST_PLAN.md` y `docs/testing/SPRINT_09_MANUAL_TEST.md`.
- **SPRINT 8 completado**: Motor de análisis de pantallas reales (ver
  `docs/ROADMAP.md`). En `:core:platform`: `OfferDetectionEngine` (detección de
  pantalla → `ScreenType`, solo `REQUEST` produce oferta), `OfferType` +
  `OfferTypeParser`/`BaseOfferTypeParser` con 5 parsers especializados de Uber
  (`UberRequestParser`, `UberRadarParser`, `UberReservationParser`,
  `UberMotoParser`, `UberXlParser`) y `OfferParserOrchestrator`
  (especializados primero, solo Uber, fallback genérico). En `:domain`:
  `RuleEngine` + 6 reglas (`RuleVerdict` PASS/WARNING/FAIL, `RuleThresholds`
  desde `DriverConfig`), `ConfidenceEngine` (HIGH/MEDIUM/LOW con % y razones;
  LOW = "Información insuficiente") y `OfferValidator`. `:core:capture` depende
  de `:core:platform`; `PlatformOfferParser` conecta el orquestador al pipeline
  (snapshot con `detectionMillis` y `rawData = "type={OfferType}"`).
  `PipelineOverlayDataSource` ejecuta reglas + confianza por oferta y expone
  `offerType`/`confidence`/`ruleEvaluation` en `OverlayUiState`; el overlay
  muestra tipo + confianza. Panel de depuración con sección **Análisis** y filas
  **Detección**/**Reglas** en rendimiento. Dataset `test-images/` con 9 escenarios
  Uber + README. `PlatformModule` provee detección/parsers/orquestador/`RuleEngine`
  (`List<@JvmSuppressWildcards ...>`). Tests: `OfferDetectionEngineTest` (12),
  `OfferParserOrchestratorTest` (9), `RuleEngineTest`, `ConfidenceEngineTest`,
  `OfferValidatorTest` y `PipelineOverlayDataSourceTest` ampliado (8).
  Verificación en verde (ktlint, unit tests, lint, assembleDebug).
- **SPRINT 7 completado**: Evaluación en tiempo real con recomendación (ver
  `docs/ROADMAP.md`). Nuevos motores en `:domain`: `ProfitEvaluationEngine`
  (delega en `ProfitEngine`, deriva costos desde `DriverConfig`, umbrales solo
  de `DriverConfig.thresholds`) y `RecommendationEngine`
  (`ACCEPT`/`REJECT`/`WARNING` con motivo, métricas usadas y % de confianza).
  Modelos de evaluación: `Recommendation`, `ProfitBreakdown`,
  `ProfitEvaluationDetailed`, `OfferRecommendation`, `OfferEvaluationResult`,
  `OfferEvaluationRecord`. **`PipelineOverlayDataSource` reescrito**: mapea el
  snapshot a `TripOffer` (textos OCR), evalúa con `EvaluateDetailedOfferUseCase`,
  persiste en `OfferEvaluationRepository` (`InMemoryOfferEvaluationRepository`,
  100 ofertas) y cronometra con `OfferPerformanceTracker`
  (`InMemoryOfferPerformanceTracker`, promedio de 20) vía `OfferTiming`. Overlay
  con `RecommendationBadge` (ACEPTAR/RECHAZAR/REVISAR), precio, ganancia, $/hora,
  $/km, costo estimado, motivo y confianza (`OverlayUiState.recommendation`,
  `ProfitState.fromRecommendation`). Panel de depuración con **Última oferta**
  (recomendación + texto OCR) y **Rendimiento (promedio últimas 20 ofertas)**.
  `OfferSnapshot` añade `texts`. Tests nuevos: `ProfitEvaluationEngineTest`,
  `RecommendationEngineTest`, `InMemoryOfferPerformanceTrackerTest`,
  `InMemoryOfferEvaluationRepositoryTest`, `PipelineOverlayDataSourceTest`
  reescrito y `DefaultCapturePipelineTest` ampliado. Verificación en verde
  (ktlint, unit tests, lint, assembleDebug, assembleDebugAndroidTest).
- **SPRINT 6 completado**: Captura de pantalla real con MediaProjection (ver
  `docs/ROADMAP.md`). Nuevo módulo **`:core:capture:android`**:
  `ScreenCaptureProvider` + `MediaProjectionScreenCaptureProvider` (token
  MediaProjection, `VirtualDisplay` + `ImageReader`), `MediaProjectionService`
  (FGS tipo `mediaProjection`), `MediaProjectionScreenCapture` (ScreenCapture
  real, degrada a texto) y `DebugCaptureMetrics`. Pipeline real:
  `CapturePipeline` expone `snapshots` y `lastMetrics`; caché de frames por hash
  (`InMemoryCaptureFrameCache`, LRU 32); `DebounceCaptureScheduler` (400 ms)
  usado por `CaptureAccessibilityService`. **`PipelineOverlayDataSource`**
  conecta el pipeline al overlay (estado real + evaluación con el motor real;
  sustituye a `SimulatedOverlayDataSource`); `OverlayContent` muestra
  `StatusLabel` (Esperando/Capturando/Analizando/Error). Home pide el permiso de
  captura (`createScreenCaptureIntent` → `startProjection`, `projectionActive`).
  Panel de depuración con métricas por etapa (Captura/OCR/Parseo/Total). Tests
  nuevos: `DebounceCaptureSchedulerTest`, `InMemoryCaptureFrameCacheTest`,
  `PipelineOverlayDataSourceTest` + `DefaultCapturePipelineTest` ampliado.
- **SPRINT 5 completado**: Primer Pipeline de Captura + OCR (ver
  `docs/ROADMAP.md`). `CaptureAccessibilityService` dedicado y desacoplado de
  la UI; `OverlayState` (DISABLED/WAITING/CAPTURING/PROCESSING/ERROR);
  `CapturePipeline`/`DefaultCapturePipeline` (ScreenCapture → OCR → OfferParser
  → CaptureRepository) en `:core:capture` (puro); `ScreenCapture` +
  `AccessibilityScreenCapture`, `OcrEngine` + `MlKitOcrEngine` (ML Kit
  `text-recognition:16.0.1`); flag `OCR`; imágenes de prueba en
  `core/capture/src/test/resources/test-images/` y tests del pipeline. Panel de
  depuración muestra `Estado del pipeline` y fila `OCR`.
- **SPRINT 4 completado**: Plataforma de Captura (Infrastructure First, ver
  `docs/ROADMAP.md`). Nuevo módulo `:core:capture` (Kotlin puro) con
   `WindowObserver`, `OfferCaptureSession`, `OfferSnapshot`,
   `FakeParser` (eliminado en WP-E1-01), `CaptureRepository` (en memoria), `OfferCaptureCoordinator`
  (desacoplado), Feature Flags (`ACCESSIBILITY`, `OVERLAY`, `CAPTURE`,
  `PARSER`, `DEBUG_PANEL`) y `SircLogger`. Sin OCR/ML/IA/regex/interpretación.
  `SircAccessibilityService` reenvía cambios de ventana sin interpretar;
  `AccessibilityWindowObserver`/`AndroidSircLogger`/`CaptureModule` en
  `:feature:overlay`; Panel de depuración (destino `Debug`) en `:app`.
- **SPRINT 3 completado**: Configuración Inicial del Conductor (ver
  `docs/ROADMAP.md`). Onboarding de 6 pasos (perfil, vehículo, costos,
  plataformas, objetivos, resumen) que persiste `DriverConfig` en Room
  (`driver_config` v2, migración 1→2) y se muestra solo la primera vez
  (`RootViewModel`/`SircRoot` en `:app`).
- **SPRINT 2 completado**: Design System + Overlay Foundation. El overlay se
  alimenta de `SimulatedOverlayDataSource` (ofertas simuladas cada 20 s
  evaluadas con el `ProfitEngine` real); el flujo de accesibilidad persiste
  historial pero su UI no está conectada aún.
- Design system en `:core:ui`: `SircTheme`, `SircColors`, `SircTypography`,
  `SircSpacing`, `SircElevations`, `ProfitState`, `ProfitIndicator`,
  `OverlayCard`/`OverlayCardContent`, `MetricCell`, etc. Todos con `@Preview`,
  KDoc y prueba unitaria de paleta/estados.
- MVP compilable (ver `docs/PROJECT.md`); 11 módulos Gradle (`:core:capture` y
  `:core:capture:android` agregados); `:domain`, `:core:platform` y
  `:core:capture` son **Kotlin puro** (sin Android).
- Dependencias Android nuevas: ML Kit OCR
  (`com.google.mlkit:text-recognition:16.0.1`) en `:feature:overlay` y el
  módulo `:core:capture:android` (MediaProjection).
- Pruebas unitarias JUnit 4: `:domain`, `:core:platform`, `:core:capture`,
  `:core:ui` y `:data`; imágenes de prueba en
  `core/capture/src/test/resources/test-images/`.
- ktlint + lint + CI (GitHub Actions) en verde (CI corre `./gradlew test`).
- Repositorio git en rama `main`, sincronizado con
  `origin https://github.com/TheJeivi02/SIRC-Android.git`.
- Decisiones de diseño registradas en `.ai/DECISIONS.md`.

## Stack y versiones clave

Kotlin 2.0.21 · Compose BOM 2024.12.01 · AGP 8.7.3 · Gradle 8.11.1 · Hilt 2.52 ·
Room 2.6.1 · Coroutines 1.9.0 · Navigation Compose 2.8.5 · KSP 2.0.21-1.0.27 ·
ktlint 12.1.2. compile/targetSdk 35 · minSdk 24 · Java/Kotlin 17.

## Arquitectura (resumen)

```
app ──► feature:overlay ──► core:platform ─► domain
  │          │                 │
  │          └──────► data ────┘
  ├──► feature:settings ─► data
  ├──► feature:history  ─► data
  ├──► feature:onboarding ─► data
  ├──► core:capture ─────► domain
  ├──► core:capture:android ──► core:capture ─► domain
  │         (y feature:overlay dep. de core:capture:android)
  └──► core:ui ──────────► domain (tipos)
```

- `domain`: modelos (incluye `DriverConfig`/`DriverProfile`/`DriverVehicle`/
  `FuelType`/`AdditionalCost` y los de evaluación: `Recommendation`,
  `ProfitBreakdown`, `ProfitEvaluationDetailed`, `OfferRecommendation`,
  `OfferEvaluationResult`, `OfferEvaluationRecord`; y el de análisis:
  `RuleVerdict`/`RuleResult`/`RuleEvaluation`), `ProfitEngine` +
  `ProfitEvaluationEngine` + `RecommendationEngine` + `ConfidenceEngine`,
  use cases (`EvaluateOfferUseCase`/`EvaluateDetailedOfferUseCase`), contratos de
  repositorio (`OfferEvaluationRepository` incluido).
- `data`: Room + repositorios concretos + Hilt (`DatabaseModule` con migración
  1→2, `RepositoryModule`).
- `core:platform`: motor de análisis de pantallas: detección (`OfferDetectionEngine`),
  `OfferParserOrchestrator` (descriptor-driven, 100 % declarativo) y
  extractores multi-plataforma (`OfferTextParser`/`GenericPlatformExtractor`,
  parser de variantes `GenericOfferTypeParser`).
- `core:capture`: plataforma de captura (pipeline único CaptureInput →
  OCR → parser → repositorio, observador, sesión/snapshot, `OverlayState`,
  coordinador, caché de dedup por hash, debounce de requests, métricas por
  etapa, `OfferTiming` + `OfferPerformanceTracker`, feature flags, logging).
- `core:capture:android`: captura de pantalla real (MediaProjection):
  `ScreenCaptureProvider`, `MediaProjectionService` (FGS tipo `mediaProjection`),
  `MediaProjectionCaptureInput`, `DebugCaptureMetrics`, `CaptureAndroidModule`.
- `core:ui`: design system.
- `feature:overlay`: accesibilidad (`CaptureAccessibilityService` delgado +
  `AccessibilityCaptureInput` con debounce), overlay + pipeline de evaluación
  + piezas Android de captura (`AccessibilityWindowObserver`,
  `MlKitOcrEngine`, `AndroidSircLogger`, `CaptureModule`). Estado del overlay
  vía `OverlayDataSource` (`PipelineOverlayDataSource`, estado real del
  pipeline; evalúa + confianza); DI del motor de análisis en
  `PlatformModule` (`OfferDetectionEngine`, `OfferParserOrchestrator`,
  `PlatformDetectionEngine`); permisos y control vía
  `PermissionManager` y `OverlayManager`
  (incluye proyección de pantalla).
- `feature:onboarding`: flujo de configuración inicial (6 pasos) que persiste
  `DriverConfig`; gating en `app` (`RootViewModel`/`SircRoot`).
- `feature:settings` / `feature:history`: UI.
- `app`: entrada, gating de onboarding, navegación (6 destinos, incluido
  Estadísticas, Diagnóstico y Debug) y arranque del `OfferCaptureCoordinator`.

## Comandos de verificación (Windows / PowerShell)

```powershell
.\gradlew.bat ktlintCheck --console=plain
.\gradlew.bat lintDebug assembleDebug testDebugUnitTest :domain:test :core:platform:test :core:capture:test :feature:overlay:testDebugUnitTest --console=plain
```

- JDK 17 (Adoptium) ya es el JVM actual: `.\gradlew.bat` funciona sin prefijar
  `$env:JAVA_HOME`. Prefijar `$env:JAVA_HOME = ...` en una sola línea puede ser
  matado por el harness (usar comandos directos).
- `ktlintFormat` NO desactiva reglas: si rompe `@Inject constructor`, es que
  falta `ktlint_standard_annotation = disabled` en `.editorconfig` (ya presente).

## Qué NO se debe hacer

Ver `.ai/RULES.md` (lista completa). Lo esencial:

- No romper la arquitectura (dependencias hacia adentro).
- El overlay es la prioridad absoluta del producto.
- No agregar dependencias ni funcionalidades que no existan ("documentar solo
  lo real").
- Accessibility Service solo lectura; prohibido `performAction`/gestos.
- Mantener bajo consumo de batería.
- No duplicar información que ya muestra la plataforma.

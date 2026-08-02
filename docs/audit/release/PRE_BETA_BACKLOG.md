# PRE-BETA BACKLOG — SIRC

**Rol:** Release Manager
**Fecha:** 2026-08-01
**Propósito:** Registro único y trazable de los **191 hallazgos accionables** de las
9 auditorías, clasificados por **severidad** (CRÍTICO / ALTO / MEDIO / BAJO) y por
**ventana de corrección** (antes de Beta / v1.1 / v2.0).

**Método:** Consolidación sin nuevas recomendaciones. La severidad es la declarada
por cada auditoría (mapeada a los 4 niveles); la ventana de corrección deriva de la
prioridad declarada por cada auditoría (P0/P1 → antes de Beta; P2 → v1.1; P3 → v2.0;
para auditorías sin prioridad, CRÍTICO/ALTO → antes de Beta, MEDIO → v1.1).

---

## Leyenda de ventanas

| Ventana | Significado | Criterio |
|---|---|---|
| **Antes de Beta** | Bloquea la distribución de la Beta | P0 + P1 (y severidad CRÍTICO/ALTO en Seguridad/Google Play) |
| **v1.1** | Deuda planificada para la primera iteración posterior a Beta | P2 |
| **v2.0** | Backlog de largo plazo | P3 |

---

# 1. DEBE CORREGIRSE ANTES DE BETA (69)

## 1.1 Arquitectura (16)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| ARC-1.1 | Dos pipelines de captura paralelos y ambos activos (legacy + moderno) | ALTO |
| SCA-11.1 | Detección de pantalla es vocabulario español+Uber; otras plataformas nunca se parsean | **CRÍTICO** |
| ARC-1.3 | `isConfigured()` contradice la definición de dominio; guardas de onboarding eludibles | ALTO |
| ARC-1.4 | Dependencia muerta de `:data` en los 4 feature modules | MEDIO |
| SOL-2.1 | God-class: `PipelineOverlayDataSource` (329 líneas, 13 dependencias) | ALTO |
| ABS-8.2 | `FakeParser` en código de producción e inyectable por Hilt | ALTO |
| DUP-10.1 | `collectTexts()` idéntico en ambos servicios de accesibilidad | ALTO |
| DUP-10.2 | Doble persistencia del historial de ofertas (Room + 2 in-memory) | ALTO |
| DUP-10.6 | Umbrales y límites duplicados entre motores de decisión | ALTO |
| SCA-11.2 | `OfferParserOrchestrator` bloquea parsers especializados con `if (platform == UBER)` | ALTO |
| SCA-11.3 | `BaseOfferTypeParser` acopla TODOS los parsers especializados a Uber | ALTO |
| SCA-11.4 | Faltan Lyft y Bolt en el enum; un solo package por plataforma | MEDIO |
| SCA-11.5 | `DEFAULT_CURRENCY` fuerza una moneda por plataforma independiente del país | ALTO |
| SCA-11.7 | `HistoryStats` agrega sin agrupar por plataforma y mezcla monedas | ALTO |
| PKG-7.2 | Formatters con nombres distintos y lógica duplicada en 5 archivos | MEDIO |
| DEAD-9.1 | `EvaluateOfferUseCase` muerto (0 referencias) | MEDIO |

## 1.2 Android (19)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| MPR-6.1 | `MediaProjectionService` sin `onDestroy` → fuga de proyección/VirtualDisplay/ImageReader | **CRÍTICO** |
| CMP-1.1 | Overlay colecta con `collectAsState()` y recompone incluso oculto | MEDIO |
| CMP-1.3 | `DebugPanelViewModel` siempre activo en la raíz del NavHost | MEDIO |
| CMP-1.5 | `costDrafts` del onboarding con `remember` (se pierde en rotación) | MEDIO |
| LIF-3.1 | `SircApplication.onCreate` arranca el coordinador legacy en segundo plano | MEDIO |
| FLW-4.1 | Colectores del `init` de `PipelineOverlayDataSource` sin manejo de errores ni reinicio | MEDIO |
| FLW-4.2 | `snapshotInFlight` descarta la oferta más reciente durante el procesamiento | MEDIO |
| CO-5.1 | La cadena OCR/evaluación/persistencia satura `Dispatchers.Default` | MEDIO |
| MPR-6.2 | Canal `CONFLATED` + `acquireLatestImage`: frame cerrado en cola | MEDIO |
| MPR-6.3 | Ronda PNG innecesaria por captura (doble encode/decode) | MEDIO |
| ACC-7.1 | Dos servicios de accesibilidad con recorridos redundantes y doble parseo | MEDIO |
| ACC-7.2 | `AccessibilityNodeInfo` no reciclado (API < 33) | MEDIO |
| ACC-7.3 | Recorrido síncrono del árbol en el hilo principal por cada `WINDOW_CONTENT_CHANGED` | MEDIO |
| ACC-7.4 | El recorrido del árbol no es exception-safe (crash del servicio) | ALTO |
| ACC-7.5 | `hasAccessibilityPermission()` verifica solo el servicio legacy | MEDIO |
| WKM-10.1 | WorkManager ausente; trim del historial como DELETE síncrono en ruta crítica | MEDIO |
| VM-9.3 | `OnboardingViewModel.save()` sin manejo de errores (puede quedar atascado) | MEDIO |
| BRT-12.2 | PNG full-screen + hash O(n) por captura | MEDIO |
| BRT-12.3 | `MediaProjectionScreenCaptureProvider` no libera la imagen reemplazada | MEDIO |

## 1.3 QA (3)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| H-QA-01 | Cero tests de capa de presentación y ViewModels (9/9 sin test, 0 UI tests) | ALTO |
| H-QA-02 | Motor OCR real (`MlKitOcrEngine`) sin tests pese a tener 15 PNGs de muestra | ALTO |
| H-QA-03 | Cero tests instrumentados de la cadena MediaProjection + servicios | ALTO |

## 1.4 Rendimiento (4)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| P-P01 | OCR a pantalla completa sin gate de detección previo | ALTO |
| P-P02 | Sin downscaling ni crop al ROI de la tarjeta de oferta | ALTO |
| P-P05 | VirtualDisplay continuo a resolución completa sin límite de frame rate | ALTO |
| P-P17 | Dos FGS + dos servicios de accesibilidad activos de forma permanente | ALTO |

## 1.5 Estabilidad (3)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| S-S14 | `android:exported="false"` en AccessibilityServices puede impedir el vínculo del sistema (**verificar en dispositivo**) | **CRÍTICO** |
| S-S10 | `MediaProjectionService` `START_NOT_STICKY`: la captura no se restaura tras kill/Doze | ALTO |
| S-S04 | El propio overlay es capturado por MediaProjection (feedback del OCR contamina) | ALTO |

## 1.6 Seguridad (3)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| S-1 | Base de datos Room sin cifrar con PII del conductor (CWE-311/312) | ALTO |
| S-2 | `allowBackup="true"` sin `dataExtractionRules` ni `fullBackupContent` | ALTO |
| S-3 | Panel de depuración con texto OCR capturado, disponible en release | ALTO |

## 1.7 Google Play (5)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| GP-1 | Declaración de Data Safety "sin datos recolectados" es falsa (misrepresentation) | **CRÍTICO** |
| GP-2 | Sin política de privacidad | ALTO |
| GP-3 | Sin "prominent disclosure" in-app sobre Accessibility y captura de pantalla | ALTO |
| GP-4 | Dos Accessibility Services redundantes para el mismo fin (no mínimo necesario) | ALTO |
| GP-5 | FGS `specialUse` siempre encendido + prompt de exención de batería | ALTO |

## 1.8 UX (11)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| UX-2.1 | Contraste del `ProfitIndicator` (decisión) insuficiente: 2.0–3.9:1, falla AA en conducción | **CRÍTICO** |
| UX-1.1 | `lightColorScheme` incompleto: modo claro roto | ALTO |
| UX-3.2 | `StatusDot` con colores oscuros hardcodeados rompe el modo claro | ALTO |
| UX-2.2 | Overlay no enfocable por TalkBack: sin alternativa accesible | ALTO |
| UX-2.3 | Texto del overlay por debajo del umbral de lectura en marcha (9–12sp) | ALTO |
| UX-11.1 | Ausencia casi total de semántica accesible (3 contentDescription en toda la app) | ALTO |
| UX-12.1 | Todos los textos de pantalla hardcodeados en español en los `.kt` | ALTO |
| UX-2.4 | Ancho fijo 82% + opacidad mínima 20%: contraste incontrolable | MEDIO |
| UX-4.1 | Borrado del historial sin diálogo de confirmación | MEDIO |
| UX-6.1 | Campo de moneda libre sin validación ISO 4217 | MEDIO |
| UX-11.2 | Objetivos táctiles por debajo de 48dp | MEDIO |

## 1.9 Documentación (5)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| DOC-2.1 | Cabecera `## [v0.5.0]` faltante; contenido del Sprint 4 anidado bajo v0.6.0 | **CRÍTICO** |
| DOC-3.1 | README/AGENTS/ROADMAP declaran 8 módulos; el proyecto tiene 11 | ALTO |
| DOC-4.1 | `PROJECT.md` "Fecha de referencia: v0.1.0" estando en v1.0.0-rc1 | ALTO |
| DOC-5.1 | `ARCHITECTURE.md` afirma que el flujo legacy fue "eliminado en RC1" cuando sigue activo | ALTO |
| DOC-9.4 | `ProfitEngine.kt` no documenta la fórmula en KDoc | MEDIO |

---

# 2. PUEDE ESPERAR A v1.1 (79)

## 2.1 Arquitectura (20)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| ARC-1.2 | Capa de use-cases inconsistente: huérfana y a la vez saltada | MEDIO |
| SOL-2.2 | God-class: `DebugPanelViewModel` (331 líneas, 11 dependencias) | MEDIO |
| SOL-2.3 | `OverlayService`: Service + notificación + WindowManager + Compose + config en un archivo | MEDIO |
| SOL-2.4 | `DefaultCapturePipeline`: 7 responsabilidades en `processInternal` | MEDIO |
| SOL-2.5 | `ProfitEngine` mezcla cálculo con formateo de presentación | MEDIO |
| MOD-3.1 | `app` acoplada a internos de `feature:overlay` | MEDIO |
| MOD-3.2 | DI de infraestructura viviendo en paquete de feature | MEDIO |
| MOD-3.3 | Implementación de repositorio en feature, no en `:data` | MEDIO |
| COH-4.2 | Riesgo latente de ciclo: bindings de infra en feature | MEDIO |
| SIZ-5.1 | 9 archivos de producción superan ~200 líneas | MEDIO |
| MIX-6.1 | `HistoryViewModel` filtra `ProfitEngine` público a la UI | MEDIO |
| MIX-6.2 | Duplicación UI↔dominio: textos presentacionales en domain | MEDIO |
| PKG-7.1 | Dos servicios de accesibilidad con nombres ambiguos | MEDIO |
| SCA-11.6 | `OfferType` solo tiene variantes `UBER_*` | MEDIO |
| SCA-11.8 | Tres fuentes de verdad del package list deben sincronizarse | MEDIO |
| SCA-11.9 | Keywords es/en únicamente; normalización sin soporte a otros scripts | MEDIO |
| SCA-11.10 | Checklist de adición de plataformas: 6-7 puntos manuales, `getValue()` lanza | MEDIO |
| DEAD-9.2 | `AddOfferHistoryUseCase` muerto (0 referencias) | MEDIO |
| DEAD-9.3 | Clúster de métodos muertos en `DriverConfigRepository`/`GetDriverConfigUseCase` | MEDIO |
| DUP-10.4 | Lógica de fechas duplicada (puede divergir entre VM y Screen) | MEDIO |

## 2.2 Android (13)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| CMP-1.2 | `applyVisibility` sin `distinctUntilChanged` (relayout por cada emisión) | BAJO |
| LIF-3.2 | Muerte de proceso: sin `SavedStateHandle` ni restauración de sesión | BAJO |
| FLW-4.3 | `snapshots` SharedFlow con buffer pequeño y `tryEmit` (descarta emisiones) | BAJO |
| CO-5.2 | Tarea ML Kit no cancelada al cancelarse la corrutina | BAJO |
| MPR-6.4 | VirtualDisplay dimensionado con `resources.displayMetrics` (contexto app) | BAJO |
| MPR-6.5 | Memoria del ImageReader a resolución completa (~2 buffers full-screen) | MEDIO |
| FGS-8.2 | `POST_NOTIFICATIONS` solicitado automáticamente al abrir Home | BAJO |
| FGS-8.3 | `START_STICKY` con `isRunning` no sincronizado con el servicio real | BAJO |
| FGS-8.4 | `startForeground` y `canDrawOverlays` sin robustez (errores tragados) | MEDIO |
| VM-9.1 | Patrón Hilt correcto; sin `SavedStateHandle` ni tests de ViewModel | MEDIO |
| VM-9.2 | One-shot "saved" modelado en estado persistente (racy) | BAJO |
| CMP-11.2 | 16KB page alignment de ML Kit sin verificar | BAJO |
| BRT-12.1 | El pipeline nunca se pausa si la captura está activa | BAJO |

## 2.3 QA (4)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| M-QA-04 | Lagunas en edge cases numéricos (división por cero / NaN) | MEDIO |
| M-QA-05 | Sin tests multi-idioma ni multi-moneda | MEDIO |
| M-QA-06 | Cobertura insuficiente de migraciones y repositorios Room | MEDIO |
| M-QA-07 | CI sin tests instrumentados | MEDIO |

## 2.4 Rendimiento (7)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| P-P03 | Formato intermedio PNG (doble encode/decode) | MEDIO |
| P-P04 | Frames cerrados por `acquireLatestImage()` bajo carga (degradación silenciosa) | MEDIO |
| P-P06 | Pipeline captura→OCR serializado con timeout de captura 400 ms | MEDIO |
| P-P07 | Dos scopes singleton nunca cancelados (falta de structured concurrency) | MEDIO |
| P-P08 | Evaluación duplicada de la misma oferta (coordinador legacy + pipeline OCR) | MEDIO |
| P-P12 | `DebugPanelViewModel` coleccionado en la raíz (trabajo continuo oculto) | MEDIO |
| P-P14 | Posible corrupción de bitmap al reciclar `padded` compartido | MEDIO |

## 2.5 Estabilidad (11)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| S-S02 | Bounds del overlay inconsistentes entre versiones en split-screen | MEDIO |
| S-S05 | Clamp de posición `y` ineficaz por `WRAP_CONTENT` | MEDIO |
| S-S06 | Presión de memoria en pantallas grandes (imágenes a resolución completa) | MEDIO |
| S-S07 | UI sin internacionalización (strings hardcodeados en español) | MEDIO |
| S-S08 | Formato de moneda y números sin `NumberFormat`/`Locale` consistente | MEDIO |
| S-S15 | `hasAccessibilityPermission()` solo valida un servicio | MEDIO |
| S-S16 | Doble recorrido del árbol por evento (2 servicios con la misma config) | MEDIO |
| S-S20 | `getMediaProjection` sin try/catch de `SecurityException` | MEDIO |
| S-S22 | `TYPE_APPLICATION_OVERLAY` sin fallback en API 24-25 (riesgo de crash) | ALTO |
| S-S23 | `OverlayController.isRunning` desincronizado del servicio real | MEDIO |
| S-S24 | Revocación de `SYSTEM_ALERT_WINDOW` en runtime no detectada por el servicio | MEDIO |

## 2.6 Seguridad (2)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| S-4 | Captura de pantalla íntegra retenida en memoria | MEDIO |
| S-5 | PII del conductor persistida y contenido de apps de terceros en memoria | MEDIO |

## 2.7 Google Play (3)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| GP-6 | Justificación de `specialUse`/`mediaProjection` en Play Console insuficiente | MEDIO |
| GP-7 | Overlay cubre ~82 % de la pantalla | MEDIO |
| GP-8 | `notificationTimeout=100` + `TYPE_WINDOW_CONTENT_CHANGED` → alta frecuencia de eventos | MEDIO |

## 2.8 UX (12)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| UX-1.2 | Sin `dynamicColor` (Material You) ni opción manual de tema | MEDIO |
| UX-2.5 | Objetivo táctil del botón de cerrar: ~28dp en conducción | MEDIO |
| UX-2.7 | Sin respeto por "Quitar animaciones" del sistema | MEDIO |
| UX-3.1 | El Home es un checklist de permisos, no un panel de decisión | MEDIO |
| UX-5.1 | Sin botón de "Saltar"/"Ahora no" en el onboarding | MEDIO |
| UX-6.2 | Sin feedback de guardado robusto (ni error, ni estado de guardando) | MEDIO |
| UX-7.2 | Donut y barras de Stats dependen solo de color | MEDIO |
| UX-8.1 | Tokens `SircSpacing` con adopción casi nula | MEDIO |
| UX-9.1 | Tamaños de texto hardcodeados dispersos fuera del sistema | MEDIO |
| UX-11.3 | Sin gestión de `fontScale`/`TextScaler` | MEDIO |
| UX-11.4 | `notificationTimeout="100"` agresivo para batería | MEDIO |
| UX-1.3 | `SircTypography` solo define 5 de los 15 estilos de Material | MEDIO |

## 2.9 Documentación (7)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| DOC-3.2 | README no indexa 12 documentos existentes | MEDIO |
| DOC-5.2 | ARCHITECTURE no referencia `docs/audit/ARCHITECTURE_AUDIT.md` | MEDIO |
| DOC-7.1 | DECISIONS sin fecha de última revisión ni tabla de decisiones recientes | MEDIO |
| DOC-8.1 | ROADMAP desactualizado en conteo de módulos y sin fecha de corte | MEDIO |
| DOC-9.1 | `:feature:settings` y `:feature:onboarding` sin KDoc | MEDIO |
| DOC-9.2 | `:data` con 10 de 14 archivos sin comentarios | MEDIO |
| DOC-9.3 | 6 archivos de `:domain` sin comentarios (use-cases muertos) | MEDIO |

---

# 3. PUEDE ESPERAR A v2.0 (43)

## 3.1 Arquitectura (12)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| SOL-2.6 | `DefaultOfferHistoryRepository.add()` mezcla persistencia y política | BAJO |
| MOD-3.4 | `core:ui` depende de `:domain` | BAJO |
| SIZ-5.2 | Firmas con parámetros excesivos | BAJO |
| PKG-7.3 | `Mappers.kt` mezcla extensiones con funciones top-level | BAJO |
| PKG-7.4 | KDoc desactualizado sobre el parser | BAJO |
| PKG-7.5 | `core:capture` con 9 subpaquetes fragmentados | BAJO |
| ABS-8.1 | Sobre-abstracción puntual en contratos triviales | BAJO |
| ABS-8.3 | `CaptureMetrics` con cuerpo NoOp por defecto | BAJO |
| DEAD-9.4 | Otros elementos muertos (`activeIndicatorCount`, `projectionActive`, `flagReportViewIds`) | BAJO |
| DUP-10.3 | Filtrado de plataformas redundante en 3 lugares | BAJO |
| DUP-10.5 | Lógica de refresh en 3 pantallas | BAJO |
| SCA-11.12 | Texto de Home con lista hardcodeada de plataformas | BAJO |

## 3.2 Android (4)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| CMP-1.4 | Ausencia de `@Immutable`/`@Stable` en modelos Compose | BAJO |
| CMP-1.6 | Import inconsistente de `LocalLifecycleOwner` | BAJO |
| NAV-2.1 | Rutas por string sin type-safety | BAJO |
| NAV-2.2 | Patrón bottom-nav correcto; sin deep links | BAJO |

## 3.3 QA (2)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| B-QA-08 | Sin pruebas de ViewModel ni de dispatchers | BAJO |
| B-QA-09 | `PlatformOfferParser` de producción sin test directo | BAJO |

## 3.4 Rendimiento (5)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| P-P09 | `snapshotInFlight` no sincronizado (race benigno) | BAJO |
| P-P10/P-P11 | Triple normalización del mismo texto / detección de pantalla 2 veces por request | MEDIO |
| P-P13 | Cero `derivedStateOf` y estado dependiente no memoizado | BAJO |
| P-P15 | `contentHashCode()` sobre el PNG completo por request | BAJO |
| P-P16 | Sin dedup cuando no hay imagen | BAJO |

## 3.5 Estabilidad (7)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| S-S03 | No hay `onMultiWindowModeChanged`; el overlay puede bloquear la interacción | MEDIO |
| S-S09 | Notificación FGS no se re-traduce al cambiar locale en runtime | BAJO |
| S-S11 | Sin `BOOT_COMPLETED`: el overlay no se restaura tras reinicio | BAJO |
| S-S12 | Estado en memoria perdido tras process death (por diseño) | BAJO |
| S-S13 | Token MediaProjection caduca; sin automatización de re-concesión | BAJO |
| S-S18 | Sin reintento/aviso de desconexión de accesibilidad | BAJO |
| S-S28 | Room sin `fallbackToDestructiveMigration` (riesgo futuro) | BAJO |

## 3.6 UX (11)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| UX-2.6 | Cambio de visibilidad instantáneo con animación de opacidad de 220 ms | BAJO |
| UX-4.2 | Estado vacío único para "sin datos" y "sin resultados de filtro" | BAJO |
| UX-4.3 | Lista sin paginación ni rendimiento bajo 500 registros | BAJO |
| UX-5.2 | Objetivo táctil del botón "✕" de costos: ~20dp | BAJO |
| UX-5.3 | Sin feedback de carga al guardar | BAJO |
| UX-6.3 | Nota de "máx. 4 indicadores" hardcodeada en la UI | BAJO |
| UX-7.1 | Las animaciones viven solo en el overlay; sin transiciones de navegación | BAJO |
| UX-8.2 | Padding interior inconsistente entre tarjetas | BAJO |
| UX-9.2 | Sin gestión de `maxLines`/`ellipsis` | BAJO |
| UX-9.3 | `SircTypography` sin `letterSpacing` ni estilos numéricos (tabulación) | BAJO |
| UX-10.2 | Home con dos "Cómo funciona": duplicación de contenido | BAJO |

## 3.7 Documentación (2)

| ID | Hallazgo | Sev. consolidada |
|---|---|---|
| DOC-2.2 | El CHANGELOG no sigue un formato semver/Keep a Changelog estricto | BAJO |
| DOC-9.6 | Cobertura de tests sin KDoc (2 de 29 archivos) | BAJO |

---

# 4. Duplicidades entre auditorías (mismo defecto raíz)

Los siguientes hallazgos son el **mismo problema** reportado por múltiples
auditorías. Al planificar, deben tratarse como una sola corrección.

| Defecto raíz | Hallazgos que lo reportan |
|---|---|
| **Doble pipeline de captura / doble AccessibilityService** | ARC-1.1, DUP-10.1, PKG-7.1, ACC-7.1, LIF-3.1, P-P08, P-P17, S-S16, GP-4 |
| **Accesibilidad: permiso validado contra un solo servicio** | ACC-7.5, S-S15, GP-4 |
| **Fuga de MediaProjection / liberación de recursos** | MPR-6.1, MPR-6.2, BRT-12.3, S-S10 |
| **Ronda PNG + hash del frame completo** | MPR-6.3, P-P03, BRT-12.2, P-P15 |
| **Overlay recompone/trabaja oculto + Debug en la raíz** | CMP-1.1, CMP-1.3, P-P12, SOL-2.2 |
| **Recorrido del árbol de accesibilidad duplicado y no seguro** | DUP-10.1, ACC-7.3, ACC-7.4, S-S16 |
| **Moneda por plataforma incorrecta + stats mezclan monedas** | SCA-11.5, SCA-11.7, S-S08 |
| **Retención de contenido capturado (memoria) en repositorios** | S-4, S-5, DUP-10.2 |
| **`snapshotInFlight` / emisión de snapshots sin coordinación** | FLW-4.2, FLW-4.3, P-P09 |
| **Textos hardcodeados en español sin localización** | UX-12.1, S-S07, MIX-6.2 |
| **`notificationTimeout=100` agresivo para batería** | UX-11.4, GP-8 |
| **Imagen/captura a resolución completa (memoria)** | MPR-6.5, S-S06, P-P02 |
| **Historial trim como DELETE síncrono** | WKM-10.1, SOL-2.6 |
| **Onboarding guarda sin try/finally ni feedback** | VM-9.3, UX-5.3 |

---

# 5. Pendientes de verificación en dispositivo (bloquean la decisión de Beta)

| Hallazgo | Verificación requerida |
|---|---|
| S-S14 | ¿Los AccessibilityService se conectan con `exported="false"`? Probar en Android 12 y 14. Si no llegan eventos, el diseño de captura completo queda invalidado. |
| S-S10 | Matar el proceso / simular Doze → ¿vuelve el overlay y la captura? ¿la UI lo reporta? |
| S-S04 | Oferta consecutiva con overlay visible → ¿el OCR se contamina con el texto de SIRC? |

---

*Fin de PRE_BETA_BACKLOG.md. Documento de consolidación; sin modificaciones de
código y sin nuevas recomendaciones.*

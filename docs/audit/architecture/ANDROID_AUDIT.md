# Auditoría Android — SIRC

> Auditoría técnica completa del stack Android. Rol: Android Lead Engineer.
> Solo evidencia, sin modificaciones de código. Fecha: 2026-08-01.
> Alcance: `compileSdk=35`, `targetSdk=35`, `minSdk=24`; módulos app,
> feature/*, core/ui, core/capture, core/capture/android, data, domain.

**Convención de severidad**: `CRITICA` = fallo en producción / fuga de recursos
del sistema / pérdida de datos; `ALTA` = defecto en ruta productiva;
`MEDIA` = riesgo de robustez/rendimiento; `BAJA` = mejora.
**Convención de prioridad**: `P0` = inmediato; `P1` = antes del próximo
release; `P2` = siguiente iteración; `P3` = backlog.

---

## Resumen ejecutivo

| Área | Veredicto |
|---|---|
| Compose | ✅ buena disciplina general; ❌ overlay recomponiendo oculto + debug siempre activo |
| Navigation | ✅ patrón bottom-nav correcto; ⚠️ rutas por string (no type-safe) |
| Lifecycle | ✅ scopes cancelados en onDestroy; ❌ coordinador legacy arranca en Application.onCreate |
| StateFlow/Flow | ⚠️ colectores sin try/catch; snapshotInFlight descarta ofertas |
| Coroutines | ✅ sin GlobalScope; ⚠️ OCR satura Dispatchers.Default; ML Kit no cancelable |
| MediaProjection | ❌ **sin onDestroy → fuga de proyección**; canal CONFLATED frágil; PNG round-trip |
| Accessibility | ✅ postura read-only correcta; ❌ doble servicio + nodos no reciclados + no exception-safe |
| Foreground Services | ✅ tipos correctos (specialUse/mediaProjection); ⚠️ isRunning no sincronizado |
| VirtualDisplay/ImageReader | ⚠️ dimensiones desde displayMetrics del app; memoria alta (2 buffers fullscreen) |
| ViewModel | ✅ @HiltViewModel idiomático; ⚠️ sin SavedStateHandle ni tests |
| WorkManager | ❌ ausente; trim del historial como DELETE síncrono en ruta crítica |
| Compatibilidad Android 10–15 | ✅ WindowMetrics con fallback, getParcelableExtra tipado; ⚠️ 16KB ML Kit sin verificar |

**Veredicto global**: el stack Android está por encima de la media de un MVP:
FGS correctos para Android 14/15, postura de accesibilidad alineada a Play,
sin GlobalScope, buena gestión de `Image.close()` en la mayoría de rutas. Los
defectos materiales son: **la fuga de proyección al morir el FGS (P0)** y **la
duplicidad de servicios de accesibilidad + recorridos no seguros (P1)**.

---

## 1. Compose

### CMP-1.1 — Overlay colecta con `collectAsState()` y recompone incluso oculto
- **Resumen**: El `ComposeView` del overlay colecta estado sin lifecycle y permanece compuesto/medido aunque la ventana esté oculta.
- **Evidencia**: `feature/overlay/.../OverlayService.kt:110` → `val state by dataSource.uiState.collectAsState()`.
- **Riesgo**: Medio. Cada cambio de pipeline (WAITING→CAPTURING→PROCESSING, snapshot, TTL) fuerza recomposición y layout; en un Service no hay LifecycleOwner, pero la ventana invisibilizada (alpha 0 + `FLAG_NOT_TOUCHABLE`) sigue recomponiendo en segundo plano durante la conducción.
- **Severidad**: MEDIA
- **Recomendación**: No agregar la ventana hasta la primera evaluación, o emitir estado solo cuando `visible==true` desde `PipelineOverlayDataSource`; usar `collectAsStateWithLifecycle` cuando exista owner.
- **Prioridad**: P1

### CMP-1.2 — `applyVisibility` sin `distinctUntilChanged`
- **Resumen**: Se llama a `updateViewLayout` por cada emisión del estado, no solo cuando cambia la visibilidad.
- **Evidencia**: `OverlayService.kt:124-128` (`uiState.collect { state -> applyVisibility(state.visible) }`) y `:131-142` (`runCatching { windowManager?.updateViewLayout(view, params) }`).
- **Riesgo**: Bajo. Relayout de la ventana overlay (costo de binder/GPU) por cada cambio de evaluación aunque la visibilidad no cambie.
- **Severidad**: BAJA
- **Recomendación**: `uiState.map { it.visible }.distinctUntilChanged()` antes de `applyVisibility`.
- **Prioridad**: P2

### CMP-1.3 — `DebugPanelViewModel` siempre activo en la raíz del NavHost
- **Resumen**: El estado del debug se instancia y colecta a nivel de Activity; el `stateIn(WhileSubscribed)` nunca se detiene.
- **Evidencia**: `app/.../SircApp.kt:53-54` → `hiltViewModel()` + `collectAsStateWithLifecycle()` en el Scaffold; `DebugPanelViewModel.kt:113-164,326-330` (combine triple + `approximateMemoryMb()`).
- **Riesgo**: Medio. Cálculo y `Runtime.getRuntime()` corriendo en todas las pantallas; CPU/batería y recomposiciones del Scaffold.
- **Severidad**: MEDIA
- **Recomendación**: Scope del VM a la ruta Debug (dentro de `composable(Destination.DEBUG.route)`); usar `derivedStateOf` solo para la visibilidad de la pestaña.
- **Prioridad**: P1

### CMP-1.4 — Ausencia de `@Immutable`/`@Stable` en modelos Compose
- **Resumen**: Ningún modelo de UI de estado está anotado para estabilidad.
- **Evidencia**: grep `@Immutable|@Stable` = 0; `OverlayUiState.kt:19-28`, `OverlayConfig.kt:9-21`.
- **Riesgo**: Bajo. El compilador puede marcar tipos como inestables → recomposiciones más amplias en listas/overlay.
- **Severidad**: BAJA
- **Recomendación**: Anotar data classes inmutables consumidas en Compose; verificar con report de estabilidad del compilador Compose.
- **Prioridad**: P3

### CMP-1.5 — `costDrafts` del onboarding con `remember` (se pierde en rotación)
- **Resumen**: La lista de costos adicionales no sobrevive recreación de Activity/proceso.
- **Evidencia**: `feature/onboarding/.../OnboardingScreen.kt:39` → `var costDrafts by remember { mutableStateOf(listOf<CostDraft>()) }`.
- **Riesgo**: Medio. Rotación en el paso Costos borra los datos ingresados; `canProceed(step=2)` falla sin explicación.
- **Severidad**: MEDIA
- **Recomendación**: Elevar los borradores al `OnboardingViewModel` o hacer `CostDraft` saveable y usar `rememberSaveable`.
- **Prioridad**: P1

### CMP-1.6 — Import inconsistente de `LocalLifecycleOwner`
- **Resumen**: Dos imports distintos del mismo API, uno deprecado.
- **Evidencia**: `HomeScreen.kt:28` usa `androidx.compose.ui.platform.LocalLifecycleOwner` (deprecado); `DiagnosisScreen.kt:27` usa `androidx.lifecycle.compose.LocalLifecycleOwner`.
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: Estandarizar en el de `lifecycle`.
- **Prioridad**: P3

---

## 2. Navigation

### NAV-2.1 — Rutas por string sin type-safety
- **Resumen**: Rutas como constantes `String` en un enum; sin `@Serializable`/`NavType` tipado.
- **Evidencia**: `app/.../SircApp.kt:33-44` (`HOME("home", ...)`) y `:92-115` (`composable(Destination.HOME.route)`).
- **Riesgo**: Bajo hoy; refactors de rutas y futuras rutas con argumentos serán propensos a errores de runtime.
- **Severidad**: BAJA
- **Recomendación**: Migrar a Navigation Compose type-safe (`@Serializable`) — `navigation-compose 2.8.5` ya está en el classpath.
- **Prioridad**: P3

### NAV-2.2 — Patrón bottom-nav correcto; sin deep links
- **Resumen**: `popUpTo(start){saveState=true} + launchSingleTop + restoreState` es el patrón canónico.
- **Evidencia**: `SircApp.kt:71-79`.
- **Riesgo**: Ninguno funcional; solo limita entrada externa a la app.
- **Severidad**: BAJA
- **Recomendación**: Si se notifica al conductor desde fuera, definir deep links con `onNewIntent` (MainActivity es `singleTask`).
- **Prioridad**: P3

---

## 3. Lifecycle

### LIF-3.1 — `SircApplication.onCreate` arranca el coordinador legacy en segundo plano
- **Resumen**: `OfferCaptureCoordinator.start()` colecta `windowEvents` con `Dispatchers.Default` durante toda la vida del proceso, sin overlay ni permisos.
- **Evidencia**: `app/.../SircApplication.kt:14`; `core/capture/.../coordinator/OfferCaptureCoordinator.kt:46-57` (`CoroutineScope(SupervisorJob() + Dispatchers.Default)` + `windowEvents.collect`).
- **Riesgo**: Medio. Proceso mantiene un colector y parsea eventos aun sin que el usuario active nada; doble pipeline con `CaptureAccessibilityService` (ARC-1.1).
- **Severidad**: MEDIA
- **Recomendación**: Arrancar la captura solo cuando el overlay esté activo (`OverlayService.onStartCommand`), o eliminar el coordinador legacy.
- **Prioridad**: P1

### LIF-3.2 — Muerte de proceso: sin `SavedStateHandle` ni restauración de sesión
- **Resumen**: Ningún ViewModel usa `SavedStateHandle`; sesión de captura/proyección/overlay son en memoria.
- **Evidencia**: grep `SavedStateHandle` = 0; `OverlayController.kt:23-35`, `MediaProjectionScreenCaptureProvider.kt:45-46`.
- **Riesgo**: Bajo-Medio. Si el sistema mata el proceso (común en apps de captura), el usuario debe reactivar manualmente overlay + captura; `isRunning`/`isProjecting` en memoria no reflejan la realidad.
- **Severidad**: BAJA
- **Recomendación**: Persistir el "deseo" de overlay/captura en `DataStore`/Room y re-arrancar el FGS tras recrear la Activity; `SavedStateHandle` para flags efímeros.
- **Prioridad**: P2

### LIF-3.3 — Edge-to-edge correcto (positivo)
- **Resumen**: `enableEdgeToEdge()`, Scaffold con `innerPadding` y `safeDrawingPadding` en onboarding.
- **Evidencia**: `MainActivity.kt:14`, `SircApp.kt:56-95`, `OnboardingScreen.kt:52`.
- **Riesgo**: Ninguno; conforme al enforcement de edge-to-edge en API 35.
- **Severidad**: BAJA
- **Recomendación**: Mantener `Scaffold` + `safeDrawingPadding` en nuevas pantallas.
- **Prioridad**: P3

---

## 4. StateFlow / Flow

### FLW-4.1 — Colectores del `init` de `PipelineOverlayDataSource` sin manejo de errores ni reinicio
- **Resumen**: Los `scope.launch { ...collect }` del `init` no tienen try/catch; una excepción mata el hijo silenciosamente y el overlay se congela para siempre.
- **Evidencia**: `feature/overlay/.../PipelineOverlayDataSource.kt:79-91` (`pipeline.state.collect { ... }`, `snapshots.collect { ... }`).
- **Riesgo**: Medio. Una excepción puntual (p. ej. NPE en un snapshot) deja el overlay congelado sin error visible ni reintento.
- **Severidad**: MEDIA
- **Recomendación**: Envolver cada collector en `try/catch` con `logger.error` y reintento con backoff, o `retryWhen` en los flujos.
- **Prioridad**: P1

### FLW-4.2 — `snapshotInFlight` descarta la oferta más reciente durante el procesamiento
- **Resumen**: Si llega un snapshot mientras se evalúa otro, se descarta silenciosamente sin cola del último pendiente.
- **Evidencia**: `PipelineOverlayDataSource.kt:118-121` → `if (snapshotInFlight) return; snapshotInFlight = true`. Es un `var` no volátil compartido entre corrutinas (`:77,120,158`).
- **Riesgo**: Medio. Dos ofertas consecutivas (rechazo y nueva puja) pueden perder la segunda; además hay una condición de carrera sin barrera de memoria.
- **Severidad**: MEDIA
- **Recomendación**: Guardar el último snapshot pendiente (`pending = snapshot`) y procesarlo al terminar; `@Volatile`/`AtomicBoolean` o un único `Job` cancelable.
- **Prioridad**: P1

### FLW-4.3 — `snapshots` SharedFlow con buffer pequeño y `tryEmit`
- **Resumen**: El pipeline emite snapshots con `tryEmit`; si el consumidor está ocupado, la emisión se descarta silenciosamente.
- **Evidencia**: `core/capture/.../pipeline/DefaultCapturePipeline.kt:59` (`extraBufferCapacity = 8`) y `:173` (`_snapshots.tryEmit(snapshot)`).
- **Riesgo**: Bajo-Medio. Pérdida de ofertas bajo carga; compensado parcialmente por FLW-4.2 pero sin coordinación.
- **Severidad**: BAJA
- **Recomendación**: Coordinar con el mecanismo "último pendiente" o `Channel(CONFLATED)` con un único consumidor; loguear si `tryEmit` falla.
- **Prioridad**: P2

---

## 5. Coroutines

### CO-5.1 — La cadena OCR/evaluación/persistencia satura `Dispatchers.Default`
- **Resumen**: OCR (pesado en CPU), captura de frame y evaluación corren todos en `Dispatchers.Default` sin limitación de paralelismo.
- **Evidencia**: `CaptureAccessibilityService.kt:33` (`CoroutineScope(SupervisorJob() + Dispatchers.Default)`); `PipelineOverlayDataSource.kt:75,123`; `MediaProjectionScreenCaptureProvider.kt:130`.
- **Riesgo**: Medio. El OCR puede saturar el pool y retrasar la conversión de frames o la evaluación, aumentando el tiempo de respuesta del overlay (objetivo <3 s).
- **Severidad**: MEDIA
- **Recomendación**: `Dispatchers.Default.limitedParallelism(N)` para OCR; `Dispatchers.IO` para E/S; aislar Image→Bitmap.
- **Prioridad**: P1

### CO-5.2 — Tarea ML Kit no cancelada al cancelarse la corrutina
- **Resumen**: `suspendCancellableCoroutine` cancela la continuación pero no `recognizer.process(image)`, que sigue consumiendo CPU/memoria.
- **Evidencia**: `feature/overlay/.../MlKitOcrEngine.kt:30-44` (`task.addOnSuccessListener { ... }`; `invokeOnCancellation { bitmap.recycle() }`).
- **Riesgo**: Bajo. Acumulación de tareas OCR huérfanas bajo alta frecuencia.
- **Severidad**: BAJA
- **Recomendación**: `task.addOnCanceledListener { ... }` o `recognizer.close()` bajo contabilidad de tareas activas.
- **Prioridad**: P2

### CO-5.3 — Sin GlobalScope y scopes acotados (positivo)
- **Resumen**: Sin `GlobalScope`; scopes cancelados en `onDestroy`/`viewModelScope`; `SupervisorJob` para aislar fallos.
- **Evidencia**: `OverlayService.kt:54,83-91`; `CaptureAccessibilityService.kt:102-105`.
- **Riesgo**: Ninguno.
- **Severidad**: BAJA
- **Recomendación**: N/A.
- **Prioridad**: P3

---

## 6. MediaProjection + VirtualDisplay + ImageReader

### MPR-6.1 — `MediaProjectionService` sin `onDestroy`: fuga de proyección/VirtualDisplay/ImageReader
- **Resumen**: Solo `stopProjection()` libera recursos; si el sistema detiene el FGS, `onDestroy` no libera la proyección y `isProjecting` queda en `true`.
- **Evidencia**: `core/capture/android/.../projection/MediaProjectionService.kt` — **no hay `override fun onDestroy()`** (verificado línea a línea, 1-130); liberación solo en `MediaProjectionScreenCaptureProvider.kt:179-204`.
- **Riesgo**: Crítico. El espejo de pantalla sigue activo sin FGS que lo justifique → drenaje de batería/GPU y capturas continuas; la UI muestra "captura activa" falsa; riesgo de política Play por captura sin servicio en primer plano.
- **Severidad**: CRITICA
- **Recomendación**: Añadir `override fun onDestroy() { provider.stopProjection() }` (y `_isProjecting=false`); re-evaluar en `onTaskRemoved`/`onTrimMemory`.
- **Prioridad**: P0

### MPR-6.2 — Canal `CONFLATED` + `acquireLatestImage`: frame cerrado en cola
- **Resumen**: `acquireLatestImage()` cierra imágenes previamente adquiridas; si la imagen en el canal no se consumió y llega otra, se puede recibir un `Image` cerrado → excepción en `toBitmap()` (mitigada por catch → null) o timing frágil.
- **Evidencia**: `MediaProjectionScreenCaptureProvider.kt:49` (`Channel<Image>(Channel.CONFLATED)`), `:156-162` (`acquireLatestImage()` + `trySend`). Adicionalmente, la imagen reemplazada nunca se cierra por el productor (~8 MB por frame Full HD).
- **Riesgo**: Medio. Capturas devueltas como null en carreras → caída al texto de accesibilidad; comportamiento dependiente de la frecuencia de frames del fabricante.
- **Severidad**: MEDIA
- **Recomendación**: `acquireNextImage()` con un único pendiente y cierre garantizado al reemplazarlo, o mutex de ciclo de vida de la imagen.
- **Prioridad**: P1

### MPR-6.3 — Ronda PNG innecesaria por captura
- **Resumen**: Cada captura comprime el frame completo como PNG (calidad 100) solo para que OCR lo decodifique de nuevo a Bitmap.
- **Evidencia**: `core/capture/android/.../MediaProjectionScreenCapture.kt:47-51` (`compress(Bitmap.CompressFormat.PNG, 100, output)`); `MlKitOcrEngine.kt:28-29` (decode).
- **Riesgo**: Medio. PNG full-screen (1080×2400) añade cientos de ms y presión de memoria/GC en la ruta crítica <3 s.
- **Severidad**: MEDIA
- **Recomendación**: Pasar el `Bitmap`/`Image` directamente a `OcrEngine` (cambiar contrato `ScreenFrame`), o JPEG ~80.
- **Prioridad**: P1

### MPR-6.4 — VirtualDisplay dimensionado con `resources.displayMetrics` (contexto app)
- **Resumen**: `startVirtualDisplay` usa las métricas del contexto app, no del display real proyectado.
- **Evidencia**: `MediaProjectionScreenCaptureProvider.kt:145-177` → `val width = metrics.widthPixels` (de `context.resources.displayMetrics`).
- **Riesgo**: Bajo. En plegables/multiventana, capturas recortadas o escaladas incorrectamente.
- **Severidad**: BAJA
- **Recomendación**: Usar `WindowMetrics`/`Display` de la proyección real y verificar `densityDpi`.
- **Prioridad**: P2

### MPR-6.5 — Memoria del ImageReader a resolución completa
- **Resumen**: `ImageReader.newInstance(ancho, alto, RGBA_8888, 2)` reserva ~2 buffers full-screen (~20 MB en Full HD).
- **Evidencia**: `MediaProjectionScreenCaptureProvider.kt:149-155`.
- **Riesgo**: Medio. Presión de memoria; en dispositivos de 3-4 GB puede contribuir a OOM junto con los buffers de ML Kit.
- **Severidad**: MEDIA
- **Recomendación**: Capturar a densidad reducida (`densityDpi/2`) para OCR sin pérdida significativa; liberar en `onTrimMemory`.
- **Prioridad**: P2

---

## 7. Accessibility

### ACC-7.1 — Dos servicios de accesibilidad con recorridos redundantes y doble parseo
- **Resumen**: `SircAccessibilityService` (legacy) y `CaptureAccessibilityService` (moderno) están ambos declarados y activos; cada evento recorre el árbol dos veces.
- **Evidencia**: `feature/overlay/src/main/AndroidManifest.xml:11-35` (dos `<service>`); `SircAccessibilityService.kt:66-88` y `CaptureAccessibilityService.kt:76-98` (misma `collectTexts`). `docs/KNOWN_ISSUES.md:9-14` invita a activar ambos.
- **Riesgo**: Medio. Doble costo de CPU/batería por evento; el usuario debe habilitar dos servicios, confuso.
- **Severidad**: MEDIA
- **Recomendación**: Unificar en un único servicio que encole en `DebounceCaptureScheduler`; eliminar el coordinador legacy.
- **Prioridad**: P1

### ACC-7.2 — `AccessibilityNodeInfo` no reciclado (API < 33)
- **Resumen**: `collectTexts` obtiene hijos con `node.getChild(i)` y no llama `recycle()`, ni al root ni a los nodos.
- **Evidencia**: `CaptureAccessibilityService.kt:83-85`, `SircAccessibilityService.kt:83-85`.
- **Riesgo**: Medio. Pérdida de memoria nativa progresiva en dispositivos API 24-29 mientras el servicio corre (que es permanente).
- **Severidad**: MEDIA
- **Recomendación**: Reciclar cada nodo tras visitarlo (`finally { node.recycle() }`) cuando `SDK_INT < 33`.
- **Prioridad**: P1

### ACC-7.3 — Recorrido síncrono del árbol en el hilo principal del servicio por cada WINDOW_CONTENT_CHANGED
- **Resumen**: El fingerprinting recorre hasta 400 nodos en cada evento; `WINDOW_CONTENT_CHANGED` se dispara con frecuencia durante el scroll de la lista de viajes.
- **Evidencia**: `CaptureAccessibilityService.kt:44-70`; `SircAccessibilityService.kt:31-60`. Sin backoff/retry; `notificationTimeout=100` ms en `accessibility_service_config.xml:9`.
- **Riesgo**: Medio. Jank del servicio, eventos perdidos (el sistema deja de entregar si el servicio tarda) y batería.
- **Severidad**: MEDIA
- **Recomendación**: Procesar principalmente `TYPE_WINDOW_STATE_CHANGED` para el disparo; mover `collectTexts` fuera del hilo del evento; dedupe por ventana activa.
- **Prioridad**: P1

### ACC-7.4 — El recorrido del árbol no es exception-safe
- **Resumen**: `node.text`, `node.contentDescription` y `node.getChild(i)` sin try/catch; `AccessibilityNodeInfo` lanza `IllegalStateException`/`SecurityException` cuando la ventana cambia a mitad del recorrido.
- **Evidencia**: `CaptureAccessibilityService.kt:76-98`, `SircAccessibilityService.kt:66-88`.
- **Riesgo**: Alto. Una excepción no capturada en `onAccessibilityEvent` crashea el servicio; Android aplica backoff antes de reiniciarlo → la captura queda en silencio por un tiempo indeterminado.
- **Severidad**: ALTA
- **Recomendación**: Envolver todo el recorrido en `runCatching`/try-catch, devolver resultados parciales y loguear.
- **Prioridad**: P1

### ACC-7.5 — `hasAccessibilityPermission()` verifica solo el servicio legacy
- **Resumen**: La detección de permiso de accesibilidad comprueba únicamente `SircAccessibilityService`.
- **Evidencia**: `feature/overlay/.../PermissionManager.kt:50-57` → `info.resolveInfo.serviceInfo.name == SircAccessibilityService::class.java.name`.
- **Riesgo**: Medio. Si el usuario habilita solo el de captura (la vía productiva), el panel dice "accesibilidad desactivada" y no guía bien; o el inverso.
- **Severidad**: MEDIA
- **Recomendación**: Verificar `CaptureAccessibilityService` (o ambos durante la migración) y exponer estados por servicio.
- **Prioridad**: P1

### ACC-7.6 — Configuración read-only correcta (positivo)
- **Resumen**: `canPerformGestures="false"`, sin `canRequestFilterKeyEvents`, filtro por paquetes de las 4 plataformas, `notificationTimeout=100`.
- **Evidencia**: `feature/overlay/src/main/res/xml/accessibility_service_config.xml:3-11`.
- **Riesgo**: Ninguno; alineado a política Play.
- **Severidad**: BAJA
- **Recomendación**: N/A.
- **Prioridad**: P3

---

## 8. Foreground Services

### FGS-8.1 — Tipos FGS y propiedades correctas para Android 14/15 (positivo)
- **Resumen**: `OverlayService` usa `specialUse` y `MediaProjectionService` usa `mediaProjection`, ambos con `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`; `startForeground()` ocurre antes de `getMediaProjection()` (requisito API 34).
- **Evidencia**: `feature/overlay/src/main/AndroidManifest.xml:37-44`; `core/capture/android/src/main/AndroidManifest.xml:9-16`; `MediaProjectionService.kt:55-56`.
- **Riesgo**: Ninguno por los tipos; el riesgo está en la liberación al morir el servicio (MPR-6.1).
- **Severidad**: BAJA
- **Recomendación**: Añadir `onDestroy` en `MediaProjectionService` (ver MPR-6.1).
- **Prioridad**: P2

### FGS-8.2 — `POST_NOTIFICATIONS` solicitado automáticamente al abrir Home
- **Resumen**: La app pide el permiso en el primer `LaunchedEffect(Unit)` de Home (API 33+), sin que el usuario haya iniciado nada.
- **Evidencia**: `app/.../HomeScreen.kt:59-66`.
- **Riesgo**: Bajo. Solicitud temprana/agresiva; rechazo puede impedir ver la notificación del FGS.
- **Severidad**: BAJA
- **Recomendación**: Pedir el permiso en el flujo guiado de inicio del overlay, no al abrir la app.
- **Prioridad**: P2

### FGS-8.3 — `START_STICKY` con `isRunning` no sincronizado con el servicio real
- **Resumen**: Si el servicio se reinicia por el sistema, `OverlayController.isRunning` queda desactualizado.
- **Evidencia**: `OverlayService.kt:75` (`START_STICKY`); `OverlayController.kt:26-35` (optimista, sin callback del servicio).
- **Riesgo**: Bajo-Medio. La UI puede mostrar "overlay en ejecución" cuando el servicio murió.
- **Severidad**: BAJA
- **Recomendación**: Reportar estado real desde el servicio (`onCreate`/`onDestroy` → `StateFlow<Boolean>`) y que el controller lo refleje; recrear la ventana de forma idempotente en `onStartCommand`.
- **Prioridad**: P2

### FGS-8.4 — `startForeground` y `canDrawOverlays` sin robustez
- **Resumen**: `startForeground` no está protegido contra `ForegroundServiceStartNotAllowedException` y `canDrawOverlays` solo se verifica una vez en `onStartCommand`.
- **Evidencia**: `OverlayService.kt:68` (`startForeground`), `:69` (check), `:122` (`wm.addView` sin re-check). Los errores de `WindowManager` se tragan con `runCatching {}` sin log (`:122,141,154,167`).
- **Riesgo**: Medio. Un `BadTokenException` o permiso revocado deja al usuario con FGS corriendo y sin overlay, sin señal diagnóstica.
- **Severidad**: MEDIA
- **Recomendación**: Proteger `startForeground`; re-verificar `canDrawOverlays` antes de `addView`; loguear errores de `WindowManager` y `stopSelf()` si no se puede crear la ventana.
- **Prioridad**: P2

---

## 9. ViewModel

### VM-9.1 — Patrón Hilt correcto; sin `SavedStateHandle` ni tests de ViewModel
- **Resumen**: Todos los ViewModels usan `@HiltViewModel` + `StateFlow`/`asStateFlow`, pero ninguno usa `SavedStateHandle` y no existen tests unitarios de ViewModels.
- **Evidencia**: `HomeViewModel.kt`, `SettingsViewModel.kt`, `OnboardingViewModel.kt` (todos `@HiltViewModel`); glob `**/src/test/**` sin tests de ViewModel.
- **Riesgo**: Medio. Estado efímero (selección de historial, pasos del onboarding) no sobrevive muerte de proceso; sin red de seguridad para regresiones de estado.
- **Severidad**: MEDIA
- **Recomendación**: `SavedStateHandle` para estado transitorio (filtros, selección, step); tests con `viewModelScope` + `StandardTestDispatcher`.
- **Prioridad**: P2

### VM-9.2 — One-shot "saved" modelado en estado persistente (racy)
- **Resumen**: El aviso "Guardado ✓" se modela en `UiState.saved` y el `combine` lo resetea en la siguiente emisión (que el propio guardado dispara).
- **Evidencia**: `feature/settings/.../SettingsViewModel.kt:32,47,74`; consumido en `SettingsScreen.kt:158`.
- **Riesgo**: Bajo. Feedback de guardado poco fiable.
- **Severidad**: BAJA
- **Recomendación**: Modelar eventos one-shot con `Channel`/`SharedFlow` o snackbar (`docs/CODING_STANDARDS.md:92-93`).
- **Prioridad**: P2

### VM-9.3 — `OnboardingViewModel.save()` sin manejo de errores (puede quedar atascado)
- **Resumen**: Si la persistencia lanza, `saving` queda en `true` para siempre y el botón se deshabilita sin mensaje.
- **Evidencia**: `feature/onboarding/.../OnboardingViewModel.kt:74-80` (sin try/finally); `OnboardingScreen.kt:103,110`.
- **Riesgo**: Medio. Usuario atascado sin error visible.
- **Severidad**: MEDIA
- **Recomendación**: `try/finally` para resetear `saving`; estado `error`/`saved` explícito.
- **Prioridad**: P1

---

## 10. WorkManager

### WKM-10.1 — WorkManager ausente; trim del historial como DELETE síncrono en cada inserción
- **Resumen**: No existe `work-runtime` en el catálogo; el límite del historial se aplica con un DELETE + SELECT COUNT en cada `add()`.
- **Evidencia**: grep `work-runtime|WorkManager` en `gradle/libs.versions.toml` y `build.gradle.kts` = 0 coincidencias; `data/.../DefaultOfferHistoryRepository.kt:23-26` (`dao.insert(...); trimToLimit(overlayConfigRepository.getOverlayConfig().historyLimit)`); `OfferHistoryDao.kt:20-24`.
- **Riesgo**: Medio. Cada oferta dispara 2 consultas extra (leer config + DELETE sobre la tabla) en la ruta crítica del pipeline; la base solo se recorta al insertar, nunca en segundo plano.
- **Severidad**: MEDIA
- **Recomendación**: Añadir `androidx.work:work-runtime-ktx` y un `CoroutineWorker` periódico (diario) para `trimToLimit` + limpieza; en la inserción, recortar solo si `count() > limit`.
- **Prioridad**: P1

---

## 11. Compatibilidad Android 10–15 (API 29–35)

### CMP-11.1 — APIs versionadas correctamente (positivo)
- **Resumen**: `WindowMetrics` (API 30+) con fallback `Display.getRealMetrics` para 24-29; `getParcelableExtra` tipado en 33+ con guard; edge-to-edge activado.
- **Evidencia**: `OverlayService.kt:193-201`; `MediaProjectionService.kt:94-102`; `MainActivity.kt:14`.
- **Riesgo**: Ninguno.
- **Severidad**: BAJA
- **Recomendación**: N/A.
- **Prioridad**: P3

### CMP-11.2 — 16KB page alignment de ML Kit sin verificar
- **Resumen**: Android 15 exige librerías nativas alineadas a 16 KB; no se verifica la del ML Kit empaquetado.
- **Evidencia**: `app/build.gradle.kts:16-17` (targetSdk 35); ML Kit `text-recognition 16.0.1` (`gradle/libs.versions.toml:62`). Sin check de `zipalign -c -P 16`.
- **Riesgo**: Bajo (con ML Kit reciente). Un rechazo de Play por librería nativa no alineada sería bloqueante.
- **Severidad**: BAJA
- **Recomendación**: Verificar los `.so` del release con `zipalign -c -P 16`; confirmar compatibilidad de ML Kit 16.0.1.
- **Prioridad**: P2

### CMP-11.3 — API usadas por encima de minSdk sin guard (verificado)
- **Resumen**: Las llamadas versionadas (`Build.VERSION.SDK_INT` en `OverlayService.kt:195,204,220`, `MediaProjectionService.kt:96`, `PermissionManager.kt:60`) están correctamente guardadas.
- **Evidencia**: grep de `Build.VERSION.SDK_INT` en rutas Android.
- **Riesgo**: Ninguno.
- **Severidad**: BAJA
- **Recomendación**: Mantener la disciplina en nuevas APIs.
- **Prioridad**: P3

---

## 12. Batería / Memoria / Rendimiento (ruta overlay + captura)

### BRT-12.1 — El pipeline nunca se pausa si la captura está activa
- **Resumen**: `PipelineOverlayDataSource` y `DebounceCaptureScheduler` corren mientras el proceso viva; no hay pausa cuando la pantalla no muestra ofertas.
- **Evidencia**: `DebounceCaptureScheduler.kt:34-35`; `PipelineOverlayDataSource.kt:79-91`.
- **Riesgo**: Bajo-Medio. Consumo de CPU residual en idle durante toda la jornada del conductor.
- **Severidad**: BAJA
- **Recomendación**: Pausar colecciones cuando `OverlayService` no corre; `sharedIn` con `WhileSubscribed` al tener suscriptor del overlay.
- **Prioridad**: P2

### BRT-12.2 — PNG full-screen + hash O(n) por captura
- **Resumen**: Además de la ronda PNG (MPR-6.3), la caché de frames hashea el `ByteArray` completo multi-MB en cada solicitud.
- **Evidencia**: `InMemoryCaptureFrameCache.kt:35-36` (`"img-${bytes.contentHashCode()}"`); `MediaProjectionScreenCapture.kt:47-51`.
- **Riesgo**: Medio. CPU/memoria extra en la ruta crítica.
- **Severidad**: MEDIA
- **Recomendación**: Hash perceptual sobre el Bitmap/`Image` o key por timestamp del `ImageReader` + fingerprint de accesibilidad.
- **Prioridad**: P1

### BRT-12.3 — `MediaProjectionScreenCaptureProvider` no libera la imagen reemplazada
- **Resumen**: Cuando `trySend` reemplaza una imagen no consumida en el canal CONFLATED, la reemplazada nunca se cierra.
- **Evidencia**: `MediaProjectionScreenCaptureProvider.kt:156-162` (ver también MPR-6.2).
- **Riesgo**: Medio. Retención de buffers (~8 MB/frame) bajo proyección sostenida.
- **Severidad**: MEDIA
- **Recomendación**: Política de cierre explícita al reemplazar (parte de MPR-6.2).
- **Prioridad**: P1

---

## 13. BUENAS PRÁCTICAS CUMPLIDAS (evidencia verificada)

1. **FGS Android 14/15 correcto**: tipos `mediaProjection` y `specialUse` declarados con `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`; `startForeground()` antes de `getMediaProjection()` (FGS-8.1).
2. **Accesibilidad read-only alineada a Play**: `canPerformGestures="false"`, filtro por `packageNames`, `notificationTimeout=100`, límites duros de nodos/textos y dedupe por fingerprint (ACC-7.6).
3. **Ciclo de vida de imágenes mayormente correcto**: `close()` en `finally` en `captureFrame`, `drainFrames()` al liberar, bitmap reciclado en OCR y en `MediaProjectionScreenCapture` (salvo la imagen reemplazada del canal, BRT-12.3).
4. **Configuración de cambios correcta**: reclamp del overlay y recreación del VirtualDisplay en `onConfigurationChanged` (MPR-6.4 positivo).
5. **Compatibilidad de APIs**: `WindowMetrics` con fallback deprecado marcado; `getParcelableExtra` tipado con guard; `enableEdgeToEdge()` para API 35 (CMP-11.1).
6. **Compose**: `collectAsStateWithLifecycle` en pantallas, `key` en `LazyColumn`, `rememberSaveable` en formularios, patrón bottom-nav con `saveState/restoreState` correcto.
7. **Corrutinas**: sin `GlobalScope`, scopes cancelados en `onDestroy`/`viewModelScope`, `SupervisorJob` para aislar fallos, debounce para coalescer eventos.
8. **StateFlow/Flow**: `stateIn(WhileSubscribed)` en History/Stats/Debug, colas acotadas, `asStateFlow` para exponer estado inmutable.
9. **Persistencia**: Room con migraciones versionadas (1→2→3), esquema exportado, historial acotado por `historyLimit`.
10. **Cobertura de tests**: buenos tests unitarios en dominio, core/capture y data (migraciones, DAOs, pipeline, scheduler) y smoke test instrumentado de MediaProjection.
11. **Privacidad/seguridad**: solo lectura de pantalla, análisis 100% local (sin red), permisos mínimos declarados.

---

## 14. Plan de remediación priorizado (Android)

| Prioridad | Acción | Hallazgos |
|---|---|---|
| P0 | `onDestroy` en `MediaProjectionService` → `provider.stopProjection()` | MPR-6.1 |
| P1 | Unificar servicios de accesibilidad; recorrido exception-safe y con reciclaje de nodos | ACC-7.1, ACC-7.2, ACC-7.4, ARC-1.1 |
| P1 | Corregir `hasAccessibilityPermission()` → `CaptureAccessibilityService` | ACC-7.5 |
| P1 | Colectores del data source con try/catch; último-snapshot-pendiente en vez de descarte | FLW-4.1, FLW-4.2 |
| P1 | Overlay: no recomponer oculto; Debug scoped a su ruta | CMP-1.1, CMP-1.3 |
| P1 | `limitedParallelism` para OCR; `onDestroy` arranque legacy (eliminar coordinador) | CO-5.1, LIF-3.1 |
| P1 | Canal CONFLATED → política de cierre segura; pasar Bitmap directo a OCR; hash barato | MPR-6.2, MPR-6.3, BRT-12.2, BRT-12.3 |
| P1 | WorkManager para trim periódico; `OnboardingViewModel.save()` con try/finally | WKM-10.1, VM-9.3 |
| P2 | `SavedStateHandle` + tests de ViewModels; one-shot events; sync `isRunning` | VM-9.1, VM-9.2, FGS-8.3, LIF-3.2 |
| P2 | Robustez FGS: `startForeground` protegido + re-check `canDrawOverlays` + log WindowManager | FGS-8.4 |

---

*Fin de ANDROID_AUDIT.md. Documento derivado de la evidencia del código; sin modificaciones realizadas.*

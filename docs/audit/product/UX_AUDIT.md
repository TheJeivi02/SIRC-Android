# Auditoría de Experiencia de Usuario — SIRC

> Auditoría completa de producto. Roles: UX/UI Director · Accessibility Specialist ·
> Technical Writer.
> Solo evidencia y recomendaciones — sin modificaciones de código. Fecha: 2026-08-01.
> Método: lectura de toda la capa UI (Compose), design system (`core:ui`), servicios de
> overlay y recursos; verificación de cada hallazgo con `file:line`.

**Convención de severidad**: `CRITICA` = bloquea release / riesgos de seguridad o de
seguridad vial; `ALTA` = defecto real en la ruta productiva; `MEDIA` = riesgo
significativo o deuda mantenible; `BAJA` = mejora.
**Convención de prioridad**: `P0` = inmediato; `P1` = antes del siguiente release;
`P2` = siguiente iteración; `P3` = backlog.

---

## Resumen ejecutivo

| Dimensión | Veredicto |
|---|---|
| Material 3 | ⚠️ Tema oscuro SIRC sólido; claro incompleto; `dynamicColor` ausente |
| Overlay (experiencia en conducción) | ⚠️ Concepto excelente; legibilidad y a11y insuficientes |
| Dashboard / Home | ❌ No es un dashboard de KPIs: es un checklist de permisos |
| Historial | ✅ Bien estructurado; falta confirmación de borrado y estados vacíos de filtros |
| Onboarding | ✅ Fluido, 6 pasos con validación; sin botón de saltar |
| Configuración | ⚠️ Funcional; sin feedback de guardado robusto ni validación de moneda |
| Animaciones | ⚠️ Solo en overlay; sin respeto por "Quitar animaciones" del sistema |
| Espaciados | ❌ Tokens de diseño (`SircSpacing`) casi sin adopción; valores sueltos |
| Tipografía | ⚠️ Sistema incompleto (5/15 estilos); hardcodeos dispersos (9–20sp) |
| Jerarquía visual | ✅ Semáforo consistente; ⚠️ donut/barras dependen de color |
| Accesibilidad | ❌ Cero semántica, overlay no enfocable, objetivos táctiles < 48dp |
| Modo oscuro | ⚠️ Tema principal correcto; modo claro roto por colores hardcodeados |
| Localización | ❌ Textos hardcodeados en español; `strings.xml` solo en overlay/captura |

**Veredicto global**: el producto tiene una idea central excelente y correctamente
ejecutada en la lógica de decisión ("reconocer, no leer", semáforo consistente en toda
la app). Los riesgos materiales están en la **legibilidad y accesibilidad del overlay
durante la conducción** (texto 9–12sp, contraste ~2–3.9:1, no enfocable por TalkBack),
el **modo claro roto**, y la **capa de presentación que ignora su propio design system**
(tokens de espaciado y tipografía sin adoptar, textos sin localizar). Ningún hallazgo
requiere reescritura; todos son correcciones localizadas de alto retorno.

---

## 1. Material 3

### UX-1.1 — `lightColorScheme` incompleto: el modo claro hereda defaults de Material
- **Resumen**: El tema claro solo define `primary` y `secondary`; el resto (surface,
  onSurface, background, surfaceVariant, error) cae a defaults de Material3 (fondos
  blancos/grises) que no combinan con la paleta SIRC pensada para fondos oscuros.
- **Evidencia**: `core/ui/src/main/kotlin/com/sirc/core/ui/theme/Theme.kt:24-28`.
- **Riesgo**: Alto. En modo claro las superficies de Material conviven con colores SIRC
  oscuros hardcodeados en componentes (ver UX-3.1), produciendo contrastes 2.0–3.9:1.
- **Severidad**: ALTA
- **Recomendación**: Definir `LightColorScheme` completa (surface, onSurface,
  surfaceVariant, onSurfaceVariant, background, onBackground, error) con una paleta
  clara SIRC derivada de la marca; usar esos tokens en los componentes en vez de
  `SircColors` directos.
- **Prioridad**: P1

### UX-1.2 — Sin `dynamicColor` (Material You) ni opción manual de tema
- **Resumen**: El tema sigue únicamente a `isSystemInDarkTheme()`; no hay Material You
  (Android 12+) ni selector claro/oscuro/sistema en Ajustes.
- **Evidencia**: `Theme.kt:30-40`; `SettingsScreen.kt` no expone ninguna opción de tema.
- **Riesgo**: Medio. El producto declara compatibilidad Play 35; los usuarios de Android
  12+ esperan colores dinámicos, y los conductores no pueden fijar el tema a la luz del
  sol (uso en cabina).
- **Severidad**: MEDIA
- **Recomendación**: (a) soportar `dynamicColor` en Android 12+ manteniendo la paleta
  SIRC para el overlay (el overlay debe conservar sus colores semáforo); (b) agregar en
  Ajustes un selector de tema (Sistema / Claro / Oscuro) persistido.
- **Prioridad**: P2

### UX-1.3 — `SircTypography` solo define 5 de los 15 estilos de Material
- **Resumen**: El sistema tipográfico cubre `headlineMedium`, `titleMedium`,
  `bodyMedium`, `labelLarge`, `labelSmall`. Los demás estilos caen a defaults de
  Material que mezclan pesos/tamaños distintos a la marca.
- **Evidencia**: `core/ui/src/main/kotlin/com/sirc/core/ui/theme/Type.kt:9-47`.
- **Riesgo**: Medio. `StatsScreen.kt:114` usa `labelMedium` y `:119` `titleLarge`, y
  `DebugPanelScreen.kt:268,273` usa `bodySmall` — estilos no definidos que producen
  jerarquía inconsistente entre pantallas.
- **Severidad**: MEDIA
- **Recomendación**: Definir los 15 estilos M3 (al menos los usados: `titleLarge`,
  `labelMedium`, `bodySmall`) con la escala tipográfica SIRC; prohibir estilos
  no-token vía lint/ktlint custom.
- **Prioridad**: P2

---

## 2. Overlay y experiencia durante la conducción

### UX-2.1 — `ProfitIndicator`: contraste insuficiente en el elemento de decisión
- **Resumen**: La píldora semáforo (la pieza que el conductor debe reconocer en <3 s)
  usa texto blanco bold sobre colores de baja luminancia relativa.
- **Evidencia**: `core/ui/src/main/kotlin/com/sirc/core/ui/components/ProfitIndicator.kt:47-52`;
  colores en `SircColors.kt:7-9`.
- **Riesgo**: CRITICA para seguridad vial. Contraste aproximado blanco-sobre:
  - `Profit #1DB954` → **~2.6:1** (falla AA)
  - `Marginal #F5A623` → **~2.0:1** (falla gravemente)
  - `NotProfit #E5484D` → **~3.9:1** (falla AA para texto normal)
  La etiqueta en blanco además compite con el `label` del estado en el mismo bloque.
- **Severidad**: CRITICA
- **Recomendación**: (a) oscurecer el fondo del estado (verde `#0F7A33`, ámbar
  `#8A5B00`, rojo `#B3261E`) manteniendo texto blanco ≥ 4.5:1; o (b) texto en negro sobre
  los colores actuales. Validar con herramienta de contraste y añadir test unitario de
  contraste (ver UX-11.1).
- **Prioridad**: P0

### UX-2.2 — Overlay no enfocable por TalkBack: sin alternativa accesible
- **Resumen**: La ventana del overlay se crea con `FLAG_NOT_FOCUSABLE`, por lo que
  ningún lector de pantalla puede leer ni navegar el indicador; el único elemento con
  `contentDescription` es el botón de cerrar.
- **Evidencia**: `feature/overlay/src/main/kotlin/com/sirc/feature/overlay/OverlayService.kt:239-242`;
  `OverlayCard.kt:87`.
- **Riesgo**: Alto. Los conductores con discapacidad visual usan TalkBack para decidir;
  hoy el producto les es invisible durante el viaje.
- **Severidad**: ALTA
- **Recomendación**: (a) anunciar el veredicto vía `announceForAccessibility` /
  `AccessibilityManager` sin enfocar la ventana (mantener `FLAG_NOT_FOCUSABLE` para no
  robar el foco de la app de transporte); (b) exponer un nodo virtual
  (`AccessibilityNodeInfo` virtual) o un "modo lectura" que envíe la decisión a la
  notificación FGS; (c) documentar la estrategia de a11y del overlay.
- **Prioridad**: P1

### UX-2.3 — Texto del overlay por debajo del umbral de lectura en marcha
- **Resumen**: Los valores críticos se muestran a 9–20sp con literales fuera de la
  tipografía del tema; etiquetas y metadatos a 9–11sp; el badge de decisión a 10–12sp.
- **Evidencia**: `Metrics.kt:42,48` (9/11 y 16/20sp), `OverlayCard.kt:81` (9/11sp),
  `OverlayContent.kt:180,196,213,251` (9–14sp), `ProfitIndicator.kt:50` (10/12sp).
- **Riesgo**: ALTA para seguridad vial. A 60 km/h el ojo del conductor tiene ~1.1 s de
  desconexión de la carretera por lectura; texto de 9sp en cabina es ilegible.
- **Severidad**: ALTA
- **Recomendación**: (a) definir en `SircTypography` una escala "overlay"
  (Display/Headline) con mínimo 14sp para etiquetas y 18–24sp para valores;
  (b) usar `sp` respetando `fontScale` (aumentar tamaño en modo compacto en vez de
  reducirlo); (c) probar con conductores reales a velocidades de conducción.
- **Prioridad**: P1

### UX-2.4 — Ancho fijo 82% + opacidad mínima 20%: contraste incontrolable
- **Resumen**: El overlay ocupa por defecto el 82% del ancho de pantalla y su fondo
  puede reducirse a 20% de opacidad, dejando texto claro sobre fondos claros de las
  apps de transporte sin garantía de contraste.
- **Evidencia**: `OverlayService.kt:237` (`OVERLAY_WIDTH_RATIO = 0.82f`), `:172`;
  `OverlayCard.kt:46` (alpha clamp 0.15–1.0); slider `SettingsScreen.kt:124-131`
  (20–100%).
- **Riesgo**: Medio-Alto. El conductor puede configurar valores que vuelven ilegible el
  indicador sin advertencia.
- **Severidad**: MEDIA
- **Recomendación**: (a) restringir la opacidad mínima a 50% (o advertir con preview);
  (b) ofrecer ancho compacto (`0.55f`) además del modo compacto actual; (c) usar un
  contorno/borde opaco (`OverlayBorder`) que mantenga legibilidad sobre cualquier fondo.
- **Prioridad**: P1

### UX-2.5 — Objetivo táctil del botón de cerrar: ~28dp en conducción
- **Resumen**: El icono de cerrar del overlay tiene un área táctil estimada de 28dp
  (icono 24dp + padding 2dp), lejos de los 48dp recomendados, y es un elemento que se
  toca mientras se conduce.
- **Evidencia**: `core/ui/src/main/kotlin/com/sirc/core/ui/components/OverlayCard.kt:85-93`.
- **Riesgo**: Medio. Tocar "cerrar" accidentalmente al agarrar el teléfono detiene el
  overlay durante un viaje.
- **Severidad**: MEDIA
- **Recomendación**: `Modifier.size(48.dp).minimumInteractiveComponentSize()` en el
  contenedor del icono; o mover el cierre a un gesto/long-press con confirmación.
- **Prioridad**: P2

### UX-2.6 — Cambio de visibilidad instantáneo con animación de opacidad de 220ms
- **Resumen**: Al ocultar el overlay se cambia `FLAG_NOT_TOUCHABLE` de forma instantánea
  mientras la opacidad aún anima 220ms, causando una percepción de corte.
- **Evidencia**: `OverlayService.kt:131-142` (cambio de flags) vs
  `OverlayContent.kt:62-66` (tween 220ms).
- **Riesgo**: Bajo. Pulido visual.
- **Severidad**: BAJA
- **Recomendación**: Coordinar el flag con el fin de la animación (callback de
  `Animatable`/`LaunchedEffect`) o reducir el fade-out a <120ms al ocultar.
- **Prioridad**: P3

### UX-2.7 — Sin respeto por "Quitar animaciones" del sistema
- **Resumen**: Las animaciones del overlay (entrada/salida, crossfade, punto pulsante)
  se ejecutan siempre; no se consulta `MotionDurationScale` ni `LocalReduceMotion`.
- **Evidencia**: `OverlayContent.kt:62-66,95-98,238`.
- **Riesgo**: Medio. Para usuarios con vestibulopatía o preferencia de movimiento
  reducido, las animaciones de un elemento que flota sobre la conducción son molestas.
- **Severidad**: MEDIA
- **Recomendación**: Leer `LocalReduceMotion`/`MotionDurationScale` y usar
  `animateFloatAsState(animationSpec = if (reduceMotion) tween(0) else tween(220))`.
- **Prioridad**: P2

---

## 3. Dashboard / Home

### UX-3.1 — El Home es un checklist de permisos, no un panel de decisión
- **Resumen**: La pantalla principal muestra 5 tarjetas de estado del sistema (overlay,
  accesibilidad, batería, notificaciones, captura) y un "Cómo funciona" de 4 pasos. No
  hay ni un solo KPI del conductor (ofertas analizadas, aceptación, ganancia estimada).
- **Evidencia**: `app/src/main/kotlin/com/sirc/app/HomeScreen.kt:76-237`.
- **Riesgo**: Medio. El conductor abre la app para "configurar una vez" y luego vive en
  el overlay; un home enfocado en permisos invita a abandonar la app. Además, el Home
  duplica casi por completo a `DiagnosisScreen`.
- **Severidad**: MEDIA
- **Recomendación**: (a) mostrar en Home un resumen de hoy (oferta evaluada más reciente,
  tendencia de ganancia, contador del día) con acceso rápido a Historial/Estadísticas;
  (b) colapsar el checklist de permisos en una sola tarjeta con estado global
  "Todo listo ✅" y CTA de corrección; (c) fusionar/eliminar la duplicación con
  Diagnóstico.
- **Prioridad**: P2

### UX-3.2 — `StatusDot` con colores oscuros hardcodeados rompe el modo claro
- **Resumen**: El punto de estado "Activo/Inactivo" usa `SircColors.Profit` /
  `OnDarkMuted` sobre la superficie clara del `SectionCard`, con contraste ~2.4–2.6:1.
- **Evidencia**: `core/ui/src/main/kotlin/com/sirc/core/ui/components/StatusComponents.kt:72-79`;
  consumido en `HomeScreen.kt:90-122` y `DiagnosisScreen.kt`.
- **Riesgo**: Alto en modo claro (ver UX-1.1).
- **Severidad**: ALTA
- **Recomendación**: Usar `colorScheme.secondary`/`onSurfaceVariant` en lugar de tokens
  oscuros; añadir test de contraste.
- **Prioridad**: P1

---

## 4. Historial

### UX-4.1 — Borrado del historial sin diálogo de confirmación
- **Resumen**: El enlace "Borrar" (rojo) elimina todo el historial al primer toque; no
  hay `AlertDialog` de confirmación.
- **Evidencia**: `feature/history/src/main/kotlin/com/sirc/feature/history/HistoryScreen.kt:65-81`.
- **Riesgo**: Medio. Borrado destructivo irreversible de datos (hasta 500 registros).
- **Severidad**: MEDIA
- **Recomendación**: `AlertDialog` de confirmación ("¿Borrar todo el historial? Esta
  acción no se puede deshacer") antes de ejecutar.
- **Prioridad**: P1

### UX-4.2 — Estado vacío único para "sin datos" y "sin resultados de filtro"
- **Resumen**: Hay dos textos vacíos correctos, pero el `EmptyHistory` no sugiere
  acciones (ej. "ver cómo funciona" o "quitar filtros").
- **Evidencia**: `HistoryScreen.kt:403-420`.
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: En el estado "sin resultados", ofrecer botón "Quitar filtros"; en
  "sin datos", enlace al "Cómo funciona".
- **Prioridad**: P3

### UX-4.3 — Lista sin paginación ni rendimiento bajo 500 registros
- **Resumen**: La `LazyColumn` carga hasta `HISTORY_LIMIT=500` elementos con un `Card`
  complejo por fila y timestamp formateado; sin paginación ni índice de fecha.
- **Evidencia**: `HistoryScreen.kt:91-102,297-343`; `HistoryViewModel.kt:127`.
- **Riesgo**: Bajo hoy (500 registros), crece con uso prolongado.
- **Severidad**: BAJA
- **Recomendación**: Paginar (Paging 3) o agrupar por día con secciones; preformatear el
  timestamp una vez (no por recomposición).
- **Prioridad**: P3

---

## 5. Onboarding

### UX-5.1 — Sin botón de "Saltar"/"Ahora no"
- **Resumen**: El onboarding de 6 pasos no permite omitir; los botones se deshabilitan
  hasta completar validación, lo que obliga a completar todo antes de ver el producto.
- **Evidencia**: `feature/onboarding/src/main/kotlin/com/sirc/feature/onboarding/OnboardingScreen.kt:97-115,119-136`.
- **Riesgo**: Medio. Abandono en el paso 3 (costos) si el conductor no tiene datos a
  mano; no hay forma de "configurar después" y se pierde un usuario.
- **Severidad**: MEDIA
- **Recomendación**: Botón secundario "Configurar después" visible desde el paso 1 que
  guarda un perfil mínimo y salta a Home; los campos críticos (moneda, plataformas)
  con defaults razonables.
- **Prioridad**: P2

### UX-5.2 — Objetivo táctil del botón "✕" de costos: ~20dp
- **Resumen**: El botón para quitar un costo adicional es un `Text` con `clickable` y
  `padding(start=8.dp)` sin altura mínima.
- **Evidencia**: `feature/onboarding/src/main/kotlin/com/sirc/feature/onboarding/OnboardingSteps.kt:319-326`.
- **Riesgo**: Bajo (fuera de conducción), pero incumple a11y.
- **Severidad**: BAJA
- **Recomendación**: `IconButton` de 48dp o `Modifier.minimumInteractiveComponentSize()`.
- **Prioridad**: P2

### UX-5.3 — Sin feedback de carga al guardar
- **Resumen**: Al pulsar "Guardar y comenzar" el botón se deshabilita (`saving`) pero no
  hay indicador de progreso ni mensaje de error si la persistencia falla.
- **Evidencia**: `OnboardingViewModel.kt:74-80`; `OnboardingScreen.kt:97-115`.
- **Riesgo**: Bajo. Un fallo de Room (poco probable) dejaría al usuario en un estado
  confuso.
- **Severidad**: BAJA
- **Recomendación**: Mostrar `CircularProgressIndicator` en el botón mientras `saving` y
  `Snackbar` ante error.
- **Prioridad**: P3

---

## 6. Configuración

### UX-6.1 — Campo de moneda libre sin validación ISO 4217
- **Resumen**: Ajustes usa un `TextField` que hace `uppercase().take(3)` sin validar el
  código; el onboarding sí ofrece un selector de 8 monedas. Inconsistencia de patrón.
- **Evidencia**: `feature/settings/src/main/kotlin/com/sirc/feature/settings/SettingsScreen.kt:61-67` vs
  `OnboardingSteps.kt:330-340`.
- **Riesgo**: Medio. Un código inválido ("XYZ") se guarda y corrompe la representación
  de moneda en Historial/Stats.
- **Severidad**: MEDIA
- **Recomendación**: Reutilizar el selector de moneda del onboarding (o autocompletar
  ISO) en Ajustes; validar contra lista ISO antes de guardar.
- **Prioridad**: P1

### UX-6.2 — Sin feedback de guardado robusto (ni error, ni estado de guardando)
- **Resumen**: El botón cambia a "Guardado ✓" sin timeout, sin snackbar, y sin estado de
  error si la persistencia falla; "Guardado ✓" permanece indefinidamente.
- **Evidencia**: `SettingsScreen.kt:158`; `SettingsViewModel.kt:69-76`.
- **Riesgo**: Medio. El usuario asume éxito aunque la persistencia falle; el "✓"
  permanente confunde tras volver a la pantalla.
- **Severidad**: MEDIA
- **Recomendación**: `Snackbar` de confirmación con auto-dismiss, estado de guardando con
  spinner, y manejo de error (reintentar) vía `snackbarHostState`.
- **Prioridad**: P2

### UX-6.3 — Nota de "máx. 4 indicadores" hardcodeada en la UI
- **Resumen**: La nota "Se muestran máx. 4 indicadores..." está escrita en la pantalla
  mientras que `OverlayConfig.activeIndicatorCount` calcula el conteo real pero no se
  consume.
- **Evidencia**: `SettingsScreen.kt:133-136`; `domain/src/main/kotlin/com/sirc/domain/model/OverlayConfig.kt:22-23`.
- **Riesgo**: Bajo. La UI puede mentir si se añaden indicadores nuevos.
- **Severidad**: BAJA
- **Recomendación**: Derivar la nota de `activeIndicatorCount` y, si el usuario activa
  más de 4, advertir cuáles se priorizan.
- **Prioridad**: P3

---

## 7. Animaciones

### UX-7.1 — Las animaciones viven solo en el overlay; el resto de la app no tiene transiciones
- **Resumen**: Solo el overlay anima (entrada 220ms, crossfade 200/150ms, punto
  pulsante). El `NavHost` usa transiciones por defecto; Home, Settings, History, Stats,
  Onboarding, Diagnóstico no tienen animaciones de contenido.
- **Evidencia**: `OverlayContent.kt:62-66,95-98,238`; `SircApp.kt:92-115`.
- **Riesgo**: Bajo (rendimiento) / percepción de app estática. No es un defecto, pero la
  ausencia de `enterTransition/exitTransition` hace los cambios de pestaña bruscos.
- **Severidad**: BAJA
- **Recomendación**: Definir transiciones de destino suaves (fade+slide leve) en el
  `NavHost` y animación del `LinearProgressIndicator` del onboarding es suficiente; no
  añadir animaciones que compitan con la decisión en conducción.
- **Prioridad**: P3

### UX-7.2 — Donut y barras de Stats dependen solo de color
- **Resumen**: El donut (verde/ámbar/rojo) y las barras del gráfico diario distinguen
  rentabilidad únicamente por color; la leyenda textual ayuda al daltonismo pero no hay
  etiquetas de porcentaje sobre el donut.
- **Evidencia**: `feature/history/src/main/kotlin/com/sirc/feature/history/StatsScreen.kt:148-220`.
- **Riesgo**: Medio. 1 de cada 12 hombres tiene daltonismo rojo/verde; en el donut sin
  etiquetas la decisión visual se pierde.
- **Severidad**: MEDIA
- **Recomendación**: Añadir porcentajes a las secciones del donut y patrón de relleno
  alternativo (hachas) o `ContentDescription` por sección; etiquetar el eje de las barras.
- **Prioridad**: P2

---

## 8. Espaciados

### UX-8.1 — Tokens `SircSpacing` con adopción casi nula
- **Resumen**: Existen tokens (`XS=4, SM=8, MD=12, LG=16, XL=24, XXL=32`) pero solo se
  usan en `OverlayCard.kt:55` y `Metrics.kt:37`. Las 6 pantallas usan `.dp` sueltos
  (16/12/8/6/4/2) y `SectionCard` hardcodea `16.dp`.
- **Evidencia**: `core/ui/src/main/kotlin/com/sirc/core/ui/theme/SircSpacing.kt:13-28`;
  `Containers.kt:36,43`; `HomeScreen.kt:81-82`; `SettingsScreen.kt:42-43`; `HistoryScreen.kt:60`.
- **Riesgo**: Medio (consistencia). Cada pantalla establece su propio ritmo vertical;
  el "Cómo funciona" del Home usa 4dp entre pasos y el Historial 6dp, generando una
  retícula visual desigual.
- **Severidad**: MEDIA
- **Recomendación**: (a) migrar pantallas a `SircSpacing.*` (o un `AppSpacing` en
  `core:ui`); (b) hacer que `SectionCard` use tokens; (c) prohibir `Modifier.padding(n.dp)`
  en composables de UI vía detekt/ktlint.
- **Prioridad**: P2

### UX-8.2 — Padding interior inconsistente entre tarjetas
- **Resumen**: Las tarjetas de Historial usan padding interior 12dp y lista `spacedBy(8)`
  mientras `SectionCard` usa 16dp/12dp; el overlay usa padding variable (SM/MD) según
  compacto.
- **Evidencia**: `HistoryScreen.kt:306` (12dp) vs `Containers.kt:36` (16dp).
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: Unificar a `SircSpacing.LG` (16dp) en tarjetas de datos y
  `SircSpacing.MD` en listas densas; documentar la decisión en el design system.
- **Prioridad**: P3

---

## 9. Tipografía

### UX-9.1 — Tamaños de texto hardcodeados dispersos fuera del sistema
- **Resumen**: Los tamaños 9/10/11/12/14/16/20sp aparecen como literales en
  `Metrics.kt`, `OverlayCard.kt`, `ProfitIndicator.kt`, `OverlayContent.kt` y
  `StatusComponents.kt`, en lugar de `MaterialTheme.typography`.
- **Evidencia**: `Metrics.kt:42,48`; `OverlayCard.kt:81,109,115`; `ProfitIndicator.kt:50`;
  `StatusComponents.kt:79`; `OverlayContent.kt:180,196,213,251`.
- **Riesgo**: Medio. Impide un retoque global de tipografía (ej. subir todos los textos
  del overlay 2sp) y fragmenta la escala.
- **Severidad**: MEDIA
- **Recomendación**: Crear estilos de tema para el overlay (`displaySmall`/`labelMedium`
  ajustados) y usarlos en todos los componentes; eliminar `sp` literales.
- **Prioridad**: P2

### UX-9.2 — Sin gestión de `maxLines`/`ellipsis`: textos largos sin truncado controlado
- **Resumen**: Solo `DebugPanelScreen.kt:422` usa `maxLines = 1` (sin `overflow`). Los
  valores del overlay y las cadenas del historial pueden desbordarse o cortarse sin
  indicador.
- **Evidencia**: `OverlayContent.kt` (valores de métricas), `HistoryScreen.kt:325-334`.
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: `maxLines` + `overflow = TextOverflow.Ellipsis` en valores del
  overlay (con guarda de ancho fijo) y en tarjetas de historial.
- **Prioridad**: P3

### UX-9.3 — `SircTypography` sin `letterSpacing` ni estilos numéricos (tabulación)
- **Resumen**: No hay `letterSpacing` en ningún estilo y los valores numéricos del
  overlay usan fuente del sistema sin tabulación, causando "salto" del dígito al cambiar
  la oferta.
- **Evidencia**: `Type.kt:9-47`; `Metrics.kt:48` (valores 16/20sp).
- **Riesgo**: Bajo. Los números grandes cambian de ancho al pasar de "12" a "123" en el
  overlay.
- **Severidad**: BAJA
- **Recomendación**: Definir un estilo numérico con `fontFeatureSettings = "tnum"` (o
  `fontFamily = FontFamily.Monospace` para el overlay) y usarlo en `MetricCell`.
- **Prioridad**: P3

---

## 10. Jerarquía visual

### UX-10.1 — Semáforo consistente en toda la app ✅
- **Resumen**: Verde/ámbar/rojo con etiquetas textuales (CONVIENE/DUDOSO/NO CONVIENE;
  ACEPTAR/RECHAZAR/REVISAR) está unificado en `ProfitState`, respetando daltonismo con
  texto además de color.
- **Evidencia**: `ProfitState.kt:15-47`.
- **Riesgo**: Ninguno. Es el mayor activo de jerarquía visual del producto.
- **Severidad**: — (conservar)
- **Recomendación**: Mantener; ampliar el patrón de "color+texto" a los gráficos de Stats
  (ver UX-7.2) y al Home.
- **Prioridad**: —

### UX-10.2 — Home con dos "Cómo funciona": duplicación de contenido
- **Resumen**: El Home termina con una tarjeta "Cómo funciona" de 4 pasos y Diagnóstico
  tiene su propia sección explicativa; contenido duplicado y no anclado a datos reales.
- **Evidencia**: `HomeScreen.kt:226-237`; `DiagnosisScreen.kt:114-144`.
- **Riesgo**: Bajo.
- **Severidad**: BAJA
- **Recomendación**: Unificar en un componente `HowItWorks` reutilizable en `core:ui`.
- **Prioridad**: P3

---

## 11. Accesibilidad

### UX-11.1 — Ausencia casi total de semántica accesible
- **Resumen**: Solo 3 `contentDescription` en toda la app (nav, cerrar overlay, limpiar
  búsqueda); cero `semantics {}`, `stateDescription`, `liveRegion`, `role` o
  `announceForAccessibility`. No hay UI tests de a11y.
- **Evidencia**: `SircApp.kt:83`, `OverlayCard.kt:87`, `HistoryScreen.kt:137`;
  ausencia en `OverlayContent.kt`, `HomeScreen.kt`, `StatsScreen.kt`.
- **Riesgo**: Alto. La app es en gran medida invisible para TalkBack; los botones M3
  exponen su rol automáticamente pero los textos "Activo/Inactivo" y los valores del
  historial no se leen con contexto.
- **Severidad**: ALTA
- **Recomendación**: (a) `contentDescription`/`stateDescription` en todos los iconos y
  estados; (b) `liveRegion` en el punto de estado del overlay (o anuncio vía servicio);
  (c) añadir test de a11y con `ComposeTestRule` (semantics sin duplicados de
  contenido); (d) evaluar con TalkBack real.
- **Prioridad**: P1

### UX-11.2 — Objetivos táctiles por debajo de 48dp
- **Resumen**: Elementos interactivos con área táctil < 48dp: cerrar overlay (~28dp),
  "✕" de costos en onboarding (~20dp), "Borrar" del historial (~34dp), chips M3 (32dp).
- **Evidencia**: `OverlayCard.kt:85-93`, `OnboardingSteps.kt:319-326`,
  `HistoryScreen.kt:71-80`; chips en `OnboardingSteps.kt:135,159,267,286` y
  `HistoryScreen.kt:173-184`.
- **Riesgo**: Medio (a11y y errores de toque en conducción).
- **Severidad**: MEDIA
- **Recomendación**: `minimumInteractiveComponentSize()` (48dp) en todos los
  `clickable`/`IconButton`/chips; verificar con el guidance de Material.
- **Prioridad**: P1

### UX-11.3 — Sin gestión de `fontScale`/`TextScaler`
- **Resumen**: Los textos usan `sp` pero el overlay tiene ancho fijo (82%) y alturas
  `WRAP_CONTENT`; con fontScale alto (usuarios con baja visión) los valores pueden
  desbordarse o cortarse.
- **Evidencia**: `OverlayService.kt:172-176`; `OverlayContent.kt`.
- **Riesgo**: Medio.
- **Severidad**: MEDIA
- **Recomendación**: (a) medir `LocalDensity.fontScale` en el servicio y adaptar el
  ancho del overlay; (b) test manual con fontScale 1.5 y 2.0; (c) no fijar tamaños en
  `sp` literales que el usuario no pueda agrandar.
- **Prioridad**: P2

### UX-11.4 — `notificationTimeout="100"` agresivo para batería
- **Resumen**: El AccessibilityService procesa eventos con `notificationTimeout=100ms`,
  generando wake-ups frecuentes incluso cuando no hay oferta.
- **Evidencia**: `feature/overlay/src/main/res/xml/accessibility_service_config.xml:10`.
- **Riesgo**: Medio (batería, relevante para un conductor todo el día).
- **Severidad**: MEDIA
- **Recomendación**: Subir a 300–500ms y depender del debounce del pipeline; o filtrar
  solo `TYPE_WINDOW_CONTENT_CHANGED` de las 4 plataformas.
- **Prioridad**: P2

---

## 12. Localización y contenido

### UX-12.1 — Todos los textos de pantalla hardcodeados en español en los `.kt`
- **Resumen**: Home, Settings, History, Stats, Onboarding, Diagnosis y Debug escriben
  sus cadenas literales en Kotlin; `strings.xml` del app solo tiene `app_name`. Solo el
  overlay y la captura usan recursos.
- **Evidencia**: `HomeScreen.kt:129-133`, `SettingsScreen.kt:158`, `HistoryScreen.kt:411-415`,
  `DebugPanelScreen.kt:191-194`, `OverlayContent.kt:228-231`;
  `app/src/main/res/values/strings.xml` (solo `app_name`).
- **Riesgo**: Alto para Play (los conductores de estas plataformas hablan EN/PT-BR) y
  para tests de a11y (sin recursos, las strings no son verificables).
- **Severidad**: ALTA
- **Recomendación**: Migrar todas las cadenas a `strings.xml` por feature (o a un
  módulo `:core:strings`), con `defaultConfig` en español; preparar `values-en`/
  `values-pt` en backlog.
- **Prioridad**: P1

---

## Anexo A — Inventario de pantallas

| Pantalla | Archivo | Estado |
|---|---|---|
| Shell + nav | `app/src/main/kotlin/com/sirc/app/SircApp.kt` | ✅ |
| Home | `app/src/main/kotlin/com/sirc/app/HomeScreen.kt` | ⚠️ checklist |
| Diagnóstico | `app/src/main/kotlin/com/sirc/app/DiagnosisScreen.kt` | ⚠️ duplica Home |
| Debug | `app/src/main/kotlin/com/sirc/app/DebugPanelScreen.kt` | ✅ (dev) |
| Overlay | `feature/overlay/src/main/kotlin/com/sirc/feature/overlay/OverlayContent.kt` | ⚠️ a11y/legibilidad |
| Onboarding | `feature/onboarding/src/main/kotlin/com/sirc/feature/onboarding/OnboardingScreen.kt` | ✅ |
| Ajustes | `feature/settings/src/main/kotlin/com/sirc/feature/settings/SettingsScreen.kt` | ⚠️ feedback |
| Historial | `feature/history/src/main/kotlin/com/sirc/feature/history/HistoryScreen.kt` | ✅ |
| Estadísticas | `feature/history/src/main/kotlin/com/sirc/feature/history/StatsScreen.kt` | ⚠️ gráficos solo-color |

## Anexo B — Matriz de prioridad

| ID | Hallazgo | Sev | Prioridad |
|---|---|---|---|
| UX-2.1 | Contraste del ProfitIndicator (decisión) | CRITICA | P0 |
| UX-1.1 / UX-3.2 | Modo claro roto | ALTA | P1 |
| UX-2.2 | Overlay no enfocable por TalkBack | ALTA | P1 |
| UX-2.3 | Texto 9–12sp en conducción | ALTA | P1 |
| UX-11.1 | Sin semántica accesible | ALTA | P1 |
| UX-12.1 | Textos sin localizar | ALTA | P1 |
| UX-2.4 | Opacidad 20% + ancho 82% | MEDIA | P1 |
| UX-4.1 | Borrado sin confirmación | MEDIA | P1 |
| UX-6.1 | Moneda sin validación ISO | MEDIA | P1 |
| UX-11.2 | Objetivos táctiles < 48dp | MEDIA | P1 |
| UX-1.2 | Sin dynamicColor / toggle de tema | MEDIA | P2 |
| UX-2.5 | Cerrar overlay 28dp | MEDIA | P2 |
| UX-2.7 | Sin respeto por reducir movimiento | MEDIA | P2 |
| UX-3.1 | Home es checklist, no dashboard | MEDIA | P2 |
| UX-5.1 | Onboarding sin "saltar" | MEDIA | P2 |
| UX-6.2 | Feedback de guardado débil | MEDIA | P2 |
| UX-7.2 | Gráficos solo-color | MEDIA | P2 |
| UX-8.1 | Tokens de espaciado sin adoptar | MEDIA | P2 |
| UX-9.1 | Tipografía hardcodeada | MEDIA | P2 |
| UX-11.3 | fontScale sin gestionar | MEDIA | P2 |
| UX-11.4 | notificationTimeout 100ms | MEDIA | P2 |
| UX-1.3 | Tipografía incompleta | MEDIA | P2 |
| UX-2.6 | Fade-out y flag desacoplados | BAJA | P3 |
| UX-4.2 / 4.3 | Historial vacío/paginación | BAJA | P3 |
| UX-5.2 / 5.3 | Onboarding táctil/carga | BAJA | P3 |
| UX-6.3 | Nota indicadores hardcodeada | BAJA | P3 |
| UX-7.1 | Transiciones de navegación | BAJA | P3 |
| UX-8.2 | Padding de tarjetas | BAJA | P3 |
| UX-9.2 / 9.3 | Ellipsis / tnum | BAJA | P3 |
| UX-10.2 | "Cómo funciona" duplicado | BAJA | P3 |

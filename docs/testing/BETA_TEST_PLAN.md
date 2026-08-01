# BETA_TEST_PLAN — v1.0.0-beta

> Plan de pruebas para la **beta cerrada** de SIRC. Objetivo: validar en
> dispositivos reales la preparación del Sprint 9: sesión de captura,
> persistencia en Room, Historial, Dashboard de estadísticas, estabilidad,
> modo beta (feature flags + diagnóstico) y overlay mejorado.

## Alcance

- Versión: **v1.0.0-beta** (Sprint 9).
- Participantes: grupo cerrado de conductores (5-15).
- Duración: 2 semanas.
- Plataformas objetivo: **Uber** (parsers especializados); DiDi/Cabify/inDrive
  con extractor genérico.
- Dispositivos recomendados: Android 10+ (minSdk 24), 4 GB+ RAM, idealmente
  dos dispositivos (uno Android 14+ y otro Android 8-11).

## Perfiles de prueba

| Perfil | Dispositivos | Enfoque |
|--------|--------------|---------|
| A — Flagship | Android 14+ | MediaProjection, FGS `mediaProjection`, overlay |
| B — Legacy | Android 8-11 | Rendimiento, degradación a accesibilidad |
| C — Estrés | cualquier | Sesiones largas (>2 h), memoria, batería |

## Instalación

1. Entregar el APK `app/build/outputs/apk/debug/app-debug.apk` (o firma beta).
2. Instalar con `adb install -r` o enlace de descarga.
3. Conceder permisos: **accesibilidad** (2 servicios), **captura de pantalla**
   y **overlay** (`Mostrar sobre otras apps`).
4. Completar el onboarding (perfil, vehículo, costos, plataformas, objetivos).

## Áreas y casos de prueba

### 1. Sesión de captura (O1)

| # | Caso | Resultado esperado |
|---|------|--------------------|
| 1.1 | Iniciar sesión desde Debug | Estado pasa a `ACTIVE`; duración cuenta |
| 1.2 | Pausar y reanudar | La duración no cuenta mientras está pausada |
| 1.3 | Detener y volver a iniciar | Se acumula una sesión nueva; los contadores se reinician |
| 1.4 | Ofertas durante la sesión | `Ofertas procesadas`, `Aceptadas`, `Rechazadas` y `Errores` se incrementan |
| 1.5 | Exportar diagnóstico | El share contiene estado de sesión, promedios y flags |

### 2. Historial persistente (O2/O3)

| # | Caso | Resultado esperado |
|---|------|--------------------|
| 2.1 | Analizar 5 ofertas reales | Aparecen en **Historial** con plataforma, precio, ganancia y decisión |
| 2.2 | Reiniciar la app | El historial sigue presente (Room) |
| 2.3 | Buscar por texto | Filtra por resumen/tipo/motivo |
| 2.4 | Filtrar por plataforma / decisión / fecha | Se aplican los filtros combinados |
| 2.5 | Abrir detalle | Muestra confianza, reglas, motivos y tiempos |
| 2.6 | Borrar historial | Confirma y vacía la tabla |
| 2.7 | Límite configurable | Con `Límite de registros` = 50, al pasar de 50 se eliminan los más antiguos |

### 3. Dashboard de estadísticas (O4)

| # | Caso | Resultado esperado |
|---|------|--------------------|
| 3.1 | Entrar a **Estadísticas** | Se muestran tarjetas: aceptación, ganancia/hora, ganancia/km, procesamiento, confianza |
| 3.2 | Con historial de varios días | El gráfico de barras diarias y el donut de decisiones se dibujan |
| 3.3 | Sin historial | Se muestra estado vacío sin crashear |
| 3.4 | Rotación del dispositivo | La pantalla no pierde datos (StateFlow + stateIn) |

### 4. Estabilidad (O5/O6)

| # | Caso | Resultado esperado |
|---|------|--------------------|
| 4.1 | Rotación con overlay activo | El overlay se reubica/reclama tamaño; no parpadea |
| 4.2 | Cambio de resolución (televisor/escritorio) | El virtual display se recrea; la captura continúa |
| 4.3 | Revocar permiso de captura en vivo | Degrada a accesibilidad sin crashear |
| 4.4 | Matar y relanzar la app | Los servicios se reinician; el historial persiste |
| 4.5 | Sesión larga (2 h) | Sin fugas evidentes; consumo de batería razonable (≈5-10 %/h capturando) |
| 4.6 | Modo oscuro / split screen | El overlay y las pantallas se ven correctos |

### 5. Modo Beta y diagnóstico (O8)

| # | Caso | Resultado esperado |
|---|------|--------------------|
| 5.1 | Alternar `RULES` en Debug | Con `RULES` OFF, el overlay ya no muestra veredictos de reglas |
| 5.2 | Alternar `DETAILED_LOGS` | Con OFF, `logcat` deja de recibir logs `debug` de SIRC |
| 5.3 | Alternar `OCR` / `CAPTURE` | El pipeline degrada correctamente |
| 5.4 | Exportar diagnóstico | El texto incluye flags y estado del pipeline para soporte |

### 6. Overlay mejorado (O9)

| # | Caso | Resultado esperado |
|---|------|--------------------|
| 6.1 | Llegada de una oferta | El overlay aparece con animación (sin parpadeos ni recreación de vista) |
| 6.2 | Transición estado → evaluación | Crossfade suave entre "Analizando…" y el resultado |
| 6.3 | Ocultar automático (TTL) | El overlay se oculta solo tras el TTL configurado |
| 6.4 | Overlay no tocable | Los toques pasan a Uber Driver (nunca bloquea la app) |

## Criterios de aceptación (gate de salida)

- [ ] Los 6 objetivos del Sprint 9 funcionan en dispositivos reales
      (sesión, historial, dashboard, estabilidad, flags, overlay).
- [ ] Ningún crash en 72 h de uso continuo con captura activa.
- [ ] El overlay nunca bloquea la interacción con la plataforma.
- [ ] El historial sobrevive a reinicios de la app.
- [ ] `ktlintCheck`, tests unitarios, `lintDebug`, `assembleDebug` y
      `assembleDebugAndroidTest` en verde.
- [ ] Los bugs bloqueantes reportados quedan registrados en GitHub Issues
      antes de cerrar la beta.

## Métricas a recopilar

- Tiempo total por sesión y número de ofertas analizadas.
- Distribución de decisiones (aceptadas/rechazadas).
- Tiempos por etapa (captura/OCR/parseo/evaluación/reglas) desde el
  diagnóstico exportado.
- Reportes de batería y temperatura en sesiones largas.

## Gestión de incidentes

1. El tester exporta el **diagnóstico** (Debug → Exportar diagnóstico) y lo
   adjunta al issue con captura de pantalla.
2. Prioridad P0: crash, overlay que bloquea, fuga de datos de pantalla.
   P1: historial/dashboard incorrectos. P2: cosmético.
3. Cada fix se valida con `docs/testing/SPRINT_09_MANUAL_TEST.md` antes de
   publicar una nueva build beta.

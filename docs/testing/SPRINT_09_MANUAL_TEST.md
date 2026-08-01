# SPRINT 09 — Prueba manual (Preparación beta cerrada)

> Objetivo: validar la preparación para la **beta cerrada** (Sprint 9):
> sesión de captura, persistencia en Room, pantalla Historial, Dashboard de
> estadísticas, optimización/estabilidad, modo beta (feature flags +
> diagnóstico), overlay mejorado y las pruebas de integración nuevas.

## Requisitos

- APK de depuración (`app/build/outputs/apk/debug/app-debug.apk`) instalado.
- Dispositivo/emulador Android 7.0+ (minSdk 24). Recomendado **Android 14+**
  para validar el FGS tipo `mediaProjection`.
- Permisos: **accesibilidad** (ambos servicios SIRC), **captura de pantalla**
  y **overlay**.
- Conductor configurado (onboarding) con costos y umbrales.
- **Uber Driver instalado** (los parsers especializados son de Uber).

## 1. Configuración previa

1. Completa el onboarding si no está configurado.
2. Ajustes > Accesibilidad: activa **Análisis de ofertas SIRC** y **Captura de
   ofertas SIRC**.
3. En Home: **Permitir captura de pantalla** e inicia el overlay.

## 2. Sesión de captura (O1)

1. Abre **Debug** → sección **Sesión de captura**.
2. Pulsa **Iniciar**: el estado pasa a `ACTIVE` y la duración cuenta.
3. Pulsa **Pausar**: la duración se congela. Espera 5 s y pulsa **Reanudar**.
4. Con una oferta real en Uber Driver, verifica que `Ofertas procesadas` y
   `Aceptadas`/`Rechazadas` se incrementan.
5. Pulsa **Detener**: la sesión vuelve a `IDLE` conservando las estadísticas.
6. Pulsa **Exportar diagnóstico**: el share incluye estado de sesión,
   promedios de rendimiento, flags y memoria.

## 3. Historial persistente (O2/O3)

1. Analiza 5 ofertas reales (puedes dejar que lleguen de forma natural).
2. Abre **Historial**: las 5 aparecen con plataforma, precio, ganancia y
   decisión (badge Aceptar/Rechazar).
3. **Mata la app** (desliza en el recents) y vuelve a abrirla: el historial
   persiste (Room).
4. **Buscar**: escribe "uber" y verifica el filtro en vivo.
5. **Filtros**: prueba plataforma, decisión y presets de fecha (Hoy / 7 días /
   30 días) combinados.
6. **Detalle**: toca una entrada → alerta con precio, distancia, duración,
   ganancia, tipo de oferta, confianza, recomendación, reglas y motivo.
7. **Límite configurable**: en Ajustes pon `Límite de registros` = 3, genera 5
   ofertas nuevas y verifica que el historial queda en 3 (las más recientes).
8. **Borrar**: pulsa el icono de papelera → vacía el historial.

## 4. Dashboard de estadísticas (O4)

1. Abre **Estadísticas** (icono de gráfica en la barra inferior).
2. Verifica las tarjetas: Ofertas analizadas, Aceptación %, Ganancia total,
   $/hora, $/km, procesamiento medio y confianza media.
3. Con historial de al menos 2 días, verifica el **gráfico de barras diarias**
   y el **donut de decisiones**.
4. **Borra el historial** y vuelve a Estadísticas: se muestra estado vacío sin
   crashear.
5. Rota el dispositivo: la pantalla mantiene los datos.

## 5. Estabilidad (O5/O6)

1. Con el overlay activo, **rota** el dispositivo: el overlay se reubica y no
   parpadea.
2. **Cambia la resolución** (p. ej. conecta a un proyector/escritorio): la
   captura continúa (se recrea el virtual display).
3. **Revoca** el permiso de captura en tiempo real: la app degrada a
   accesibilidad sin crashear.
4. **Mata y relanza** la app: los servicios se reinician.
5. **Split screen** con Uber Driver: el overlay sigue mostrando la oferta
   (estado `WAITING`/`CAPTURING`/`PROCESSING`).
6. Modo **oscuro**: las pantallas y el overlay se ven correctos.

## 6. Modo Beta (O8)

1. En **Debug → Feature Flags** alterna `RULES` a OFF: en la siguiente oferta
   el overlay ya no muestra veredictos de reglas (la evaluación sigue).
2. Alterna `DETAILED_LOGS` a OFF y revisa `logcat`: los logs `debug` de SIRC
   desaparecen (los `warn`/`error` siguen).
3. Alterna `OVERLAY` a OFF: el estado degrada a `DISABLED`.
4. Vuelve a activar todo y **Exportar diagnóstico**: el texto incluye el estado
   de cada flag.

## 7. Overlay mejorado (O9)

1. Con Uber Driver en una pantalla de solicitud, verifica que el overlay
   aparece con **animación** (escala + crossfade) y sin parpadeos ni
   reconstrucción de vista.
2. La transición "Analizando…" → resultado es un crossfade suave.
3. El overlay **no intercepta toques**: puedes pulsar botones de Uber Driver a
   través de él.
4. Tras el TTL configurado, el overlay se oculta solo.

## 8. Pruebas automatizadas (O7)

Desde la raíz del proyecto (Windows / PowerShell):

```powershell
.\gradlew.bat :domain:test :feature:overlay:testDebugUnitTest --console=plain
.\gradlew.bat ktlintCheck --console=plain
```

Pruebas de integración de Room (requieren dispositivo/emulador):

```powershell
.\gradlew.bat :data:connectedDebugAndroidTest --console=plain
```

Cubre: `CaptureSessionManagerTest`, `HistoryFilterTest`,
`HistoryStatsCalculatorTest`, `PipelineOverlayDataSourceTest` (sesión +
persistencia), `OfferHistoryDaoTest` y `SircDatabaseMigrationTest` (v1→v3).

## Criterios de aceptación (verificación)

- [ ] La sesión de captura inicia/pausa/reanuda/detiene y acumula estadísticas.
- [ ] El historial persiste tras reiniciar la app, se filtra, busca y detalla;
      el límite configurable recorta los más antiguos.
- [ ] El Dashboard muestra métricas y gráficos; estado vacío sin crashear.
- [ ] Rotación, cambio de resolución, revocación de permisos y split screen
      no rompen la captura ni el overlay.
- [ ] Los feature flags `RULES`, `DETAILED_LOGS` y `OVERLAY` surten efecto;
      el diagnóstico exportable es legible y completo.
- [ ] El overlay aparece con animación, no parpadea, no bloquea toques y se
      oculta por TTL.
- [ ] `ktlintCheck`, `:domain:test`, `:feature:overlay:testDebugUnitTest`,
      `lintDebug`, `assembleDebug`, `assembleDebugAndroidTest` en verde.

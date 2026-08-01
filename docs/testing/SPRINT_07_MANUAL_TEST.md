# SPRINT 07 — Prueba manual (Evaluación en tiempo real con recomendación)

> Objetivo: validar la evaluación detallada de ofertas con el motor real y la
> recomendación accionable en el overlay (Sprint 7): captura → evaluación con
> costos derivados del `DriverConfig` → recomendación ACCEPT/REJECT/WARNING con
> motivo y % de confianza, historial en memoria de las últimas 100 ofertas y
> métricas de rendimiento por oferta (promedio de 20) en el panel de depuración.

## Requisitos

- APK de depuración (`app/build/outputs/apk/debug/app-debug.apk`) instalado.
- Dispositivo/emulador con Android 7.0+ (minSdk 24). Recomendado **Android 14+**
  para validar el FGS tipo `mediaProjection`.
- Permiso de accesibilidad otorgado a ambos servicios de SIRC:
  - **Análisis de ofertas SIRC** (`SircAccessibilityService`, overlay).
  - **Captura de ofertas SIRC** (`CaptureAccessibilityService`, pipeline).
- Permiso de **captura de pantalla** concedido desde Home (ver sección 2).
- **Conductor configurado** (onboarding completado) con costos y umbrales
  (ganancia mínima por km y por hora), porque la recomendación depende de
  `DriverConfig`.
- Tener instalada al menos una plataforma soportada (Uber, DiDi, Cabify o
  InDrive) y una oferta visible.

## 1. Configuración previa

1. Completa el onboarding (Perfil, Vehículo, Costos, Plataformas, Objetivos) si
   no está configurado.
2. Ajustes > Accesibilidad: activa **Análisis de ofertas SIRC** y **Captura de
   ofertas SIRC**.
3. Ajusta los umbrales (por km y por hora) para que conozcas el criterio de la
   decisión.

## 2. Captura y evaluación en vivo

1. Abre la app → **Inicio** → **Permitir captura de pantalla** y acepta el
   consentimiento (selecciona una app para emular, p. ej. Uber).
2. Inicia el overlay desde Inicio/Diagnóstico.
3. Abre **Uber** con una oferta visible. El overlay debe recorrer
   `Esperando… → Capturando… → Analizando…` y terminar mostrando:
   - **Recomendación** (insignia semáforo): `ACEPTAR` (verde),
     `RECHAZAR` (rojo) o `REVISAR` (ámbar).
   - **Precio**, **GANANCIA**, **POR HORA**, **POR KM**, resumen del viaje y
     **COSTO EST.**
   - **Motivo principal** y **% de confianza** (solo cuando hay evaluación).

## 3. Coherencia de la recomendación

1. Con una oferta claramente por encima de tus umbrales (ganancia/km y /hora
   altos) → debe salir `ACEPTAR`.
2. Con una oferta claramente por debajo de tus umbrales → debe salir `RECHAZAR`.
3. Con ganancia cercana a los umbrales → debe salir `REVISAR` (margen bajo).
4. La confianza crece cuanto mayor es el margen (≥ 50 %, ≤ 98 %).
5. Cambia los umbrales en Ajustes y vuelve a evaluar: la recomendación debe
   reflejar la nueva configuración **sin reiniciar la app** (los umbrales se leen
   solo de `DriverConfig`).

## 4. Panel de depuración — "Última oferta"

1. Abre **Debug** (icono de llave) con una oferta ya evaluada.
2. La sección **Última oferta** muestra: recomendación con confianza, plataforma,
   precio, distancia, duración, motivo, **texto OCR** (truncado a 200 chars),
   parser y timestamp de captura.
3. Evalúa otra oferta: la sección se actualiza con la última.

## 5. Rendimiento por oferta (promedio de 20)

1. En Debug, la sección **Rendimiento (promedio últimas 20 ofertas)** muestra
   captura/OCR/parseo/evaluación/overlay/total en ms.
2. El **Total** debe estar muy por debajo de 3000 ms (objetivo <3 s desde que
   aparece la oferta hasta que el overlay la muestra).
3. Con varias ofertas evaluadas, la fila "Última oferta (ms)" (E · O · T)
   refleja la oferta más reciente.

## 6. Historial en memoria

1. No hay borrado ni persistencia entre reinicios (es en memoria): al reiniciar
   la app, **Última oferta** vuelve a vaciarse hasta la primera captura.
2. El historial persistente de **Historial** (Room, sprints 2/4) sigue
   funcionando sin cambios.

## 7. Degradación y regresión

1. Sin conductor configurado, la evaluación usa `DriverConfig.default()` y no
   debe crashear.
2. Sin proyección, el pipeline degrada a textos de accesibilidad y la evaluación
   sigue funcionando.
3. Diagnóstico mantiene los 5 indicadores; el overlay persiste el historial de
   siempre.

## Criterios de aceptación (verificación)

- [ ] El overlay muestra recomendación ACCEPTAR/RECHAZAR/REVISAR con precio,
      ganancia, $/hora, $/km, costo estimado, motivo y % de confianza.
- [ ] La recomendación responde a los umbrales de `DriverConfig` (cambios en
      Ajustes se reflejan sin reiniciar).
- [ ] Debug muestra **Última oferta** (recomendación + texto OCR) y
      **Rendimiento** (promedio de 20) con Total << 3000 ms.
- [ ] El historial en memoria se vacía al reiniciar; el de Room (Historial) no.
- [ ] Sin configuración ni proyección el sistema no crashea.
- [ ] `ktlintCheck`, `testDebugUnitTest`, `:core:capture:test`, `:domain:test`,
      `lintDebug`, `assembleDebug` y `assembleDebugAndroidTest` en verde.

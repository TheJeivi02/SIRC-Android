# SPRINT 08 — Prueba manual (Motor de análisis de pantallas de Uber)

> Objetivo: validar el pipeline de análisis real de pantallas (Sprint 8):
> detección de pantalla → parsers especializados por tipo de oferta →
> evaluación → reglas + confianza → overlay con tipo de oferta, % de confianza
> y veredicto de reglas en el panel de depuración.

## Requisitos

- APK de depuración (`app/build/outputs/apk/debug/app-debug.apk`) instalado.
- Dispositivo/emulador con Android 7.0+ (minSdk 24). Recomendado **Android 14+**
  para validar el FGS tipo `mediaProjection`.
- Permisos: **accesibilidad** (ambos servicios SIRC), **captura de pantalla**
  y **overlay**.
- Conductor configurado (onboarding) con costos y umbrales (por km/hora).
- **Uber Driver instalado** (los parsers especializados son de Uber; otras
  plataformas usan el extractor genérico).

## 1. Configuración previa

1. Completa el onboarding si no está configurado.
2. Ajustes > Accesibilidad: activa **Análisis de ofertas SIRC** y **Captura de
   ofertas SIRC**.
3. En Home: **Permitir captura de pantalla** e inicia el overlay.

## 2. Detección y parsing por tipo de pantalla

Con Uber Driver abierto, verifica que el overlay se comporte así según la
pantalla:

| Pantalla                       | Tipo de oferta esperado | Overlay |
|--------------------------------|-------------------------|---------|
| Solicitud estándar (X/Comfort) | `UBER_REQUEST`          | Recomendación normal |
| Uber Moto                      | `UBER_MOTO`             | Recomendación normal |
| Uber XL                        | `UBER_XL`               | Recomendación normal |
| Viaje reservado/programado     | `UBER_RESERVATION`      | Recomendación normal |
| Radar (explorar el mapa)       | `UBER_RADAR`            | Recomendación normal |
| Inicio / Menú                  | (sin oferta)            | Sin overlay de oferta |
| Navegando (viaje en curso)     | (sin oferta)            | Sin overlay de oferta |
| Pantalla de error/offline      | (sin oferta)            | Sin overlay de oferta |

1. El tipo de oferta aparece en el overlay como `TIPO · Confianza X% (NIVEL)`.
2. Las pantallas sin oferta (Home, navegación, error) **no** producen oferta:
   el overlay no muestra precio ni recomendación.
3. Con una pantalla de solicitud cuyo texto esté parcialmente oculto, el overlay
   debe seguir apareciendo (heurística tolerante a OCR).

## 3. Confianza y "Información insuficiente"

1. Con una oferta completa (monto + distancia + duración) la confianza debe ser
   `HIGH`/`MEDIUM` y el overlay muestra la recomendación.
2. Si la oferta no tiene monto o no hay distancia ni duración, la confianza cae
   a `LOW` y el overlay muestra **"Información insuficiente · X% confianza"**
   en rojo, sin sugerir aceptar/rechazar.
3. El nivel de confianza y las razones aparecen en **Debug → Análisis**.

## 4. Panel de depuración — sección "Análisis"

1. Abre **Debug** (icono de llave) con una oferta ya analizada.
2. La sección **Análisis** muestra:
   - **Tipo de oferta** (p. ej. `UBER_REQUEST`).
   - **Confianza**: % y nivel.
   - **Razones** (p. ej. "Faltan datos del viaje").
   - **Reglas**: cada una de las 6 con su veredicto
     (`PASS` verde / `WARNING` ámbar / `FAIL` rojo): Ganancia, Por km, Por hora,
     Distancia máx., Recogida, Duración máx.
3. Con una oferta de distancia > 60 km, la regla **Distancia máx.** debe salir
   `FAIL`; con ganancia/km por debajo del umbral, **Por km** sale `FAIL`.

## 5. Rendimiento (tiempos por etapa)

1. En Debug → **Rendimiento (promedio últimas 20 ofertas)** ahora hay filas
   **Detección** y **Reglas** además de captura/OCR/parseo/evaluación/overlay/total.
2. El **Total** debe mantenerse muy por debajo de 3000 ms; la detección debe ser
   una fracción del parseo.

## 6. Degradación y regresión

1. Sin conductor configurado no crashea (usa `DriverConfig.default()`).
2. Sin proyección, el pipeline degrada a textos de accesibilidad.
3. DiDi/Cabify/inDrive siguen funcionando con el extractor genérico (tipo
   `GENERIC` en Debug si no matchea un parser de Uber).
4. El historial de **Historial** (Room) sigue funcionando sin cambios.

## Criterios de aceptación (verificación)

- [ ] Pantallas de solicitud de Uber se detectan como `REQUEST` y producen
      oferta; Home/navegación/error no.
- [ ] Los parsers especializados distinguen `UBER_REQUEST`, `UBER_MOTO`,
      `UBER_XL`, `UBER_RESERVATION` y `UBER_RADAR`.
- [ ] El overlay muestra tipo de oferta + % de confianza, y "Información
      insuficiente" cuando la confianza es `LOW`.
- [ ] Debug → **Análisis** muestra tipo, confianza (con razones) y el veredicto
      de las 6 reglas.
- [ ] El panel muestra tiempos de **Detección** y **Reglas**.
- [ ] Sin configuración ni proyección el sistema no crashea.
- [ ] `ktlintCheck`, `testDebugUnitTest`, `:core:capture:test`,
      `:core:platform:test`, `:domain:test`, `lintDebug`, `assembleDebug` y
      `assembleDebugAndroidTest` en verde.

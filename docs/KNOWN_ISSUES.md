# Incidencias conocidas — v1.0.0-rc1

> Estado actual y limitaciones conocidas de la Release Candidate 1.
> Este documento se actualiza con los hallazgos de las pruebas en dispositivos
> reales. Nada aquí es bloqueante para la prueba; son riesgos a vigilar.

## Accesibilidad / captura

1. **Un solo servicio de accesibilidad** (`Captura de ofertas SIRC` →
   `CaptureAccessibilityService`). Desde WP-E1-03 el servicio es único (solo
   lectura) y alimenta el pipeline completo: overlay + historial + panel de
   depuración. Si está desactivado, no hay captura ni análisis.
   - **Mitigación**: el Diagnóstico indica el estado del servicio.

2. **El overlay depende del servicio de accesibilidad.** El análisis y la
   persistencia del historial los produce el pipeline alimentado por
   `CaptureAccessibilityService` (con o sin captura de pantalla). Si el
   usuario no habilita el servicio, no hay historial ni overlay. Es la
   arquitectura única desde Sprint 7/8.
   - **Impacto**: esperado; documentado para soporte.

3. **Falsa detección de oferta en pantallas no soportadas.** El parser ignora
   pantallas que no sean `REQUEST` (Uber) o que no tengan textos extraíbles
   (resto de plataformas). Pueden darse descartes `NO_TEXTS`/`UNSUPPORTED_PLATFORM`
   frecuentes; quedan registrados en el informe de validación.
   - **No es un fallo**: es el comportamiento de deduplicación/detección.

## MediaProjection

4. **La proyección puede detenerse por el sistema** (cambio de usuario, modo
   ahorro, algunos fabricantes agresivos). El provider registra el incidente
   (`CaptureError`), libera recursos y el usuario debe volver a conceder el
   permiso.
   - **Mitigación**: mensaje claro; el pipeline degrada a textos de
     accesibilidad mientras tanto.

5. **Android 14+ requiere FGS de tipo `mediaProjection`.** Ya implementado;
   en algunos dispositivos el fabricante puede bloquear el arranque del FGS
   (tarea de limpieza de RAM). Si la captura de pantalla no arranca, revisar el
   ahorro de batería.

## Rendimiento / batería

6. **El OCR es la etapa más cara** (ML Kit sobre el frame completo). Se ejecuta
   con debounce (400 ms) y caché por hash de frame; aun así, en pantallas 2K+
   puede tardar 300–800 ms y consumir batería durante capturas prolongadas.
   - **Vigilar**: ver `docs/PERFORMANCE_REPORT.md`.

7. **Buffer del historial en memoria limitado a 50 snapshots** y el tracker de
   rendimiento a las últimas 100 ofertas (promedio de 20). Son acotados a
   propósito (consumo de memoria).

## UI / configuración

8. **Ajustes de umbrales y perfil son responsabilidad del conductor.** Con
   `DriverConfig.default()` (conductor sin configurar) las reglas y la
   recomendación usan valores por defecto que pueden no coincidir con el
   mercado local. Es esperado hasta que el usuario complete el onboarding.

## Plataformas

9. **Análisis de pantallas solo para Uber en el tipo de oferta.** El parser
   de tipo (`OfferTypeParser`/`GenericOfferTypeParser`) reporta la variante
   (Moto/XL/etc.) para Uber; DiDi, Cabify e InDrive caen al extractor genérico
   por plataforma (monto/distancia/duración). El **tipo de oferta** no se
   reporta para estas plataformas.

## Calidad

10. **Test instrumentado mínimo.** Las pruebas de Android (rotación real, split
    screen, memoria en dispositivo) no se pueden ejecutar en CI; están cubiertas
    por el plan manual (`docs/testing/`) y pendientes de validación en
    dispositivos Android 10–15.

## Backlog candidato (post-RC1)

- Umbrales de ahorro de batería y configuración de calidad de OCR.
- Datos de rendimiento medidos en dispositivo para calibrar las ventanas.
- Cobertura instrumentada para rotación y split screen.

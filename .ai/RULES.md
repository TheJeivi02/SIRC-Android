# Reglas permanentes del proyecto

> Reglas que ningún agente (humano o IA) debe violar. Si una decisión de diseño
> contradice estas reglas, la regla gana.

## Arquitectura

1. **Nunca romper la arquitectura.** El flujo de dependencias es unidireccional
   hacia adentro: `UI → ViewModel → use case → domain`. Los módulos `:domain` y
   `:core:platform` deben seguir siendo **Kotlin puro** (sin Android).
2. **Mantener Clean Architecture y MVVM.** Los contratos de repositorio se
   definen en `domain`; `data` los implementa. La UI nunca accede a `data` ni a
   `core:platform` directamente.
3. **El `ProfitEngine` es una función pura**: sin estado, sin I/O, todo por
   parámetros. No acoplarlo a Android.

## Dependencias

4. **No agregar dependencias innecesarias.** Cada librería nueva requiere
   justificación escrita en el changelog. Preferir el ecosistema AndroidX/Jetpack
   ya usado. Revisar `gradle/libs.versions.toml` antes de añadir algo.

## Producto

5. **El Overlay es la prioridad absoluta.** Cualquier cambio que pueda degradar
   la velocidad de decisión (<3 s), la estabilidad del overlay o su
   compatibilidad con Google Play se considera bloqueante.
6. **Mantener bajo consumo de batería.** Traversals limitados, deduplicación de
   frames, un único `ComposeView`, sin trabajo innecesario en el servicio de
   accesibilidad.
7. **Priorizar rendimiento.** El pipeline (lectura → parseo → evaluación →
   overlay) debe ser rápido y barato.
8. **No duplicar información mostrada por la plataforma.** SIRC solo muestra
   información derivada: ganancia, ganancia/hora, ganancia/km e insignia de
   decisión. Nunca repetir monto/distancia/tiempo como los muestra la app.

## Estrategia de producto

9b. **Prohibido cualquier automatización de interacción** (auto-aceptar,
    auto-rechazar, contra-ofertas, clics, gestos). Es el mayor riesgo de baneo
    del conductor y viola la política de Play. La ruta de producto que guía
    esto está en `docs/PRODUCT_STRATEGY.md` (matriz EVITAR/DIFERENCIAR).
9c. **Respetar las prioridades P0–P3 definidas** en
    `docs/PRODUCT_STRATEGY.md`. No implementar features fuera de roadmap sin
    aprobación explícita; P0 (robustez/cumplimiento) y P1 (ruta inmediata)
    siempre tienen prioridad sobre features aspiracionales.

## Seguridad, privacidad y Google Play

9. **Accessibility Service SOLO lectura.** Prohibido: `performAction`,
   `dispatchGesture`, key events, automatizar aceptar/rechazar viajes o
   interactuar con la interfaz de otras apps.
10. **100 % local.** Sin backend, sin telemetría, sin anuncios, sin subir datos
    de pantalla. Todo el historial vive en Room. La integridad (Play Integrity)
    y el ahorro de energía (SOC) —cuando existan— se exponen como contratos de
    `:domain` para mantener los módulos Kotlin puro.
11. **Respetar la declaración de propósito** de `accessibility_service_config.xml`
    y el subtipo `specialUse` del Foreground Service. Mantener al día
    `docs/GOOGLE_PLAY_COMPLIANCE.md`.

## Calidad y trabajo

12. **Verificar siempre antes de dar por terminado**: `ktlintCheck`,
    `lintDebug`, pruebas unitarias y `assembleDebug` en verde.
13. **Toda corrección de lógica añade o ajusta una prueba unitaria** en
    `:domain` o `:core:platform`.
14. **Documentar solo lo que existe.** No inventar funcionalidades ni APIs.
    Actualizar `docs/ARCHITECTURE.md` y `.ai/CONTEXT.md` cuando algo cambie de
    forma relevante.
15. **No subir secretos ni `local.properties`.** No incluir artefactos `build/`
    en commits.
16. **No implementar nuevas funcionalidades sin instrucción explícita.**

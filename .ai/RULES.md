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
    aprobación explícita; P0 (robustez del núcleo) y P1 (lanzamiento comercial
    seguro: suscripción + entitlement + integridad) siempre tienen prioridad
    sobre features aspiracionales.

## Modelo de negocio y seguridad (LOCAL-FIRST)

9d. **SIRC es una aplicación de pago por suscripción.** El APK debe considerarse
    **potencialmente manipulable**: nunca confiar en el cliente para decisiones
    críticas de autorización (suscripción activa, entitlement, revocación,
    identidad de cuenta). Esas decisiones viven en un backend verificado con
    Google Play APIs (`docs/SECURITY_MODEL.md`).
9e. **Principio LOCAL-FIRST** (reemplaza "100 % local"): el procesamiento de
    ofertas (OCR, parsing, evaluación, overlay, historial) permanece 100 %
    local y **ninguna oferta/dato de pantalla sale del dispositivo**; identidad,
    suscripción, entitlement, integridad y recuperación usan **servicios remotos
    mínimos**.
9f. **No implementar** Play Integrity, Play Billing, suscripciones, backend,
    RTDN ni entitlement hasta que se abra explícitamente **E1b** (etapa posterior
    a la beta del núcleo; ver `docs/SECURITY_MODEL.md` y `docs/BETA_READINESS.md`).
9g. **Backend inicial = Supabase** (Auth + RLS + Edge Functions + Postgres)
    para identidad/suscripción/entitlement; el plan pasa a **Pro** al salir a
    producción (el Free pausa proyectos por inactividad). `service_role`/
    secret keys y la service account de Play **nunca** en el APK (solo la
    publishable key + URL del proyecto). Detalle en `docs/BACKEND_ARCHITECTURE.md`.
9h. **Verificación de compra SOLO server-side** con la Google Play Developer API
    **`purchases.subscriptionsv2.get`** (la API `purchases.subscriptions.get`
    está **deprecada**); RTDN como señal (siempre re-consultar la API antes de
    mutar entitlement, dedupe por `messageId`). El cliente jamás adjudica
    entitlement.

## Colaboración multi-agente

17. **Prohibido que dos agentes modifiquen simultáneamente el mismo workspace o
    branch.** Si se usan varios agentes (OpenCode + Antigravity, etc.): worktrees
    aislados, commits separados, revisión humana e integración controlada. El
    **agente principal** (OpenCode) mantiene autoridad sobre arquitectura,
    roadmap, WPs, integración y release (ver `docs/ANTIGRAVITY_EVALUATION.md`
    si existe, y la sección "Herramientas" de `.ai/CONTEXT.md`).

## Seguridad, privacidad y Google Play

9. **Accessibility Service SOLO lectura.** Prohibido: `performAction`,
   `dispatchGesture`, key events, automatizar aceptar/rechazar viajes o
   interactuar con la interfaz de otras apps.
10. **El procesamiento de ofertas es 100 % local** (LOCAL-FIRST): sin telemetría
    de pantalla, sin subir datos de ofertas. El historial vive en Room. Cuando
    exista el backend (E1b), solo se intercambia estado comercial/de cuenta
    (cuenta, suscripción, entitlement, integridad), **nunca** contenido de
    pantalla. La integridad (Play Integrity) y el ahorro de energía (SOC) —cuando
    existan— se exponen como contratos de `:domain` para mantener los módulos
    Kotlin puro.
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

# SIRC — Android

> Contexto de arranque para agentes de IA. Léelo siempre al iniciar una sesión.
> Este archivo es el MAPA del proyecto; el ESTADO EN VIVO de la tarea actual
> está en `TASK.md`. No re-leas el proyecto completo: consulta los archivos
> puntuales que necesites.

## Qué es

App Android nativa (Kotlin, Jetpack Compose, Material 3) que ayuda a
conductores de Uber/DiDi/Cabify/InDrive a decidir en <3 segundos si una oferta
es rentable, mediante un overlay flotante que solo muestra información
derivada (ganancia, $/hora, $/km, insignia semáforo). 100 % local.

## Dónde está el contexto (NO duplicar, consultar)

- `.ai/CONTEXT.md` — qué es, flujo real, estado de sprints, stack, arquitectura.
- `.ai/RULES.md` — reglas permanentes (arquitectura, overlay prioridad, solo lectura, 100 % local).
- `.ai/DECISIONS.md` — decisiones de diseño registradas.
- `docs/ARCHITECTURE.md` — arquitectura y decisiones técnicas.
- `docs/CODING_STANDARDS.md` — estilo Kotlin y convenciones.
- `TASK.md` — **estado en vivo de la tarea actual** (siempre actualizado).

## Reglas rápidas (resumen)

- Nunca romper la arquitectura (dependencias hacia adentro; `:domain` y
  `:core:platform` son Kotlin puro).
- El Overlay es la prioridad absoluta; mantener <3 s, bajo consumo de batería.
- Accessibility Service SOLO lectura (prohibido `performAction`/gestos).
- 100 % local: sin telemetría, sin backend, sin subir datos de pantalla.
- Verificar siempre: `.\gradlew.bat ktlintCheck`, `testDebugUnitTest`,
  `lintDebug`, `assembleDebug` en verde.
- Documentar solo lo que existe; actualizar `.ai/CONTEXT.md` y `TASK.md`.

## Verificación (Windows / PowerShell)

```powershell
.\gradlew.bat ktlintCheck --console=plain
.\gradlew.bat lintDebug assembleDebug testDebugUnitTest :domain:test :core:platform:test :core:capture:test :feature:overlay:testDebugUnitTest --console=plain
```

## Cómo trabajar

1. Lee `TASK.md` primero (es el estado actual).
2. Consulta `.ai/CONTEXT.md` y `.ai/RULES.md`.
3. Antes de tocar código, confirma el objetivo y el plan con el usuario.
4. Al terminar una tarea relevante, actualiza `TASK.md` (y `.ai/CONTEXT.md`
   si cambió algo de forma relevante).

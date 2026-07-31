# SIRC — Sistema Inteligente de Rentabilidad para Conductores

Aplicación Android nativa para que conductores de plataformas de movilidad
(Uber, DiDi, Cabify, InDrive) decidan en **menos de 3 segundos** si una oferta
de viaje es rentable.

El MVP se concentra en un **Overlay flotante** rápido, estable, configurable y
compatible con Google Play.

> **Filosofía:** el conductor ya ve la información de la plataforma. SIRC NO la
> repite. Solo muestra **información derivada** para decidir: ganancia
> estimada, ganancia/hora, ganancia/km y una insignia de decisión. El conductor
> no debe leer: debe **reconocer** con colores semáforo.

---

## Estado del MVP

| Entregable | Estado |
|---|---|
| Proyecto Android multi-módulo compilable | ✅ |
| Clean Architecture + MVVM + Modularización | ✅ |
| Overlay flotante (TYPE_APPLICATION_OVERLAY) | ✅ |
| Accessibility Service (solo lectura) | ✅ |
| Motor de Rentabilidad (puro, testeado) | ✅ |
| Configuración de Overlay y costos | ✅ |
| Historial básico + persistencia Room | ✅ |
| Hilt, Coroutines, Flow | ✅ |
| CI (GitHub Actions) + ktlint + lint | ✅ |
| Cumplimiento Google Play (política de Accesibilidad) | ✅ |

## Stack

- Kotlin 2.0 · Jetpack Compose · Material Design 3
- Clean Architecture + MVVM
- Hilt (DI) · Room (persistencia) · Coroutines/Flow
- Gradle 8.11 · AGP 8.7 · Version Catalog
- ktlint · Android Lint · GitHub Actions

## Módulos

```
:app                 → Entrada, navegación, Home
:feature:overlay     → Accessibility Service + Overlay Service + UI del overlay
:feature:settings    → Configuración (costos, umbrales, indicadores)
:feature:history     → Historial de ofertas evaluadas
:core:ui             → Design system (tema + componentes)
:core:platform       → Extractores multi-plataforma (puro Kotlin)
:data                → Room, DAOs, repositorios
:domain              → Modelo, ProfitEngine, casos de uso (puro Kotlin)
```

## Requisitos

- JDK 17
- Android Studio (Ladybug+) o SDK 35
- `local.properties` con `sdk.dir=...`

## Compilar

```bash
./gradlew assembleDebug          # APK debug
./gradlew testDebugUnitTest      # pruebas unitarias
./gradlew ktlintCheck            # estilo de código
./gradlew lintDebug              # lint de Android
```

## Estructura de referencia (sin copiar)

Uber, DiDi, Cabify e InDrive se manejan como **plataformas desacopladas**:
agregar una nueva plataforma = nuevo descriptor de palabras clave en
`core:platform` (ver `docs/ARCHITECTURE.md`).

## Seguridad y Google Play

El Accessibility Service **solo lee** el contenido visible. No toca botones, no
acepta/rechaza viajes, no simula gestos. Declaración de propósito en
`feature/overlay/src/main/res/xml/accessibility_service_config.xml`. Detalle en
`docs/GOOGLE_PLAY_COMPLIANCE.md`.

## Documentación

- `docs/PROJECT.md` — objetivo, estado actual, módulos y stack.
- `docs/ARCHITECTURE.md` — arquitectura, flujo de datos del overlay, decisiones.
- `docs/CODING_STANDARDS.md` — estilo Kotlin, convenciones, SOLID, Clean + MVVM.
- `docs/GOOGLE_PLAY_COMPLIANCE.md` — política de Accesibilidad, permisos, FGS.
- `docs/CHANGELOG.md` — historial de versiones.
- `docs/ROADMAP.md` — plan de sprints.
- `.ai/CONTEXT.md` — contexto de arranque para agentes de IA.
- `.ai/RULES.md` — reglas permanentes del proyecto.
- `.ai/AGENTS.md` — roles de agentes recomendados.

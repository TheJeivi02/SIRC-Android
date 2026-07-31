# SIRC — Contexto para agentes de IA

> Resumen de arranque para cualquier sesión de IA. Léelo junto con
> `docs/PROJECT.md`, `docs/ARCHITECTURE.md` y `.ai/RULES.md` antes de tocar
> código. Actualiza este archivo cuando el proyecto cambie de forma relevante.

## Qué es el proyecto

**SIRC (Sistema Inteligente de Rentabilidad para Conductores)** es una app
Android nativa (Kotlin, Jetpack Compose, Material 3) que ayuda a conductores de
Uber, DiDi, Cabify e InDrive a decidir en **<3 segundos** si una oferta de viaje
es rentable.

**Cómo funciona (flujo real):**

> Nota SPRINT 2: el overlay aún NO consume el análisis real. Hoy se alimenta de
> `SimulatedOverlayDataSource` (ofertas simuladas por plataforma cada 20 s,
> evaluadas con el `ProfitEngine` real). El flujo de accesibilidad persiste el
> historial pero su UI no está conectada aún.

1. Un **Accessibility Service (solo lectura)** detecta la app de transporte
   visible y recolecta los textos de pantalla (límites duros: 400 nodos, 80
   textos, ≤200 chars, deduplicación por huella de texto).
2. `ExtractorRegistry` → `GenericPlatformExtractor` parsea el texto
   (`OfferTextParser`) y construye un `TripOffer?` con palabras clave por
   plataforma.
3. `OfferEventBus` (StateFlow en memoria) lleva la oferta a `OfferEvaluator`.
4. `ProfitEngine` (función pura) calcula ganancia, ganancia/hora, ganancia/km y
   emite `Decision` (`PROFITABLE` / `MARGINAL` / `NOT_PROFITABLE`).
5. El historial se persiste en Room.
6. `OverlayService` (Foreground, `TYPE_APPLICATION_OVERLAY`) dibuja un
   `ComposeView` liviano con la insignia de decisión y hasta 4 indicadores.

**Filosofía**: NO repetir la información que ya muestra la plataforma; solo
información derivada (ganancia, métricas) con colores semáforo. El Accessibility
Service **nunca** interactúa con otras apps.

## Estado del proyecto

- **SPRINT 3 completado**: Configuración Inicial del Conductor (ver
  `docs/ROADMAP.md`). Onboarding de 6 pasos (perfil, vehículo, costos,
  plataformas, objetivos, resumen) que persiste `DriverConfig` en Room
  (`driver_config` v2, migración 1→2) y se muestra solo la primera vez
  (`RootViewModel`/`SircRoot` en `:app`).
- **SPRINT 2 completado**: Design System + Overlay Foundation. El overlay se
  alimenta de `SimulatedOverlayDataSource` (ofertas simuladas cada 20 s
  evaluadas con el `ProfitEngine` real); el flujo de accesibilidad persiste
  historial pero su UI no está conectada aún.
- Design system en `:core:ui`: `SircTheme`, `SircColors`, `SircTypography`,
  `SircSpacing`, `SircElevations`, `ProfitState`, `ProfitIndicator`,
  `OverlayCard`/`OverlayCardContent`, `MetricCell`, etc. Todos con `@Preview`,
  KDoc y prueba unitaria de paleta/estados.
- MVP compilable (ver `docs/PROJECT.md`); 9 módulos Gradle (`:feature:onboarding`
  agregado); `:domain` y `:core:platform` son **Kotlin puro** (sin Android).
- Pruebas unitarias JUnit 4: `:domain`, `:core:platform`, `:core:ui` y `:data`.
- ktlint + lint + CI (GitHub Actions) en verde.
- Repositorio git en rama `main`, sincronizado con
  `origin https://github.com/TheJeivi02/SIRC-Android.git`.
- Decisiones de diseño registradas en `.ai/DECISIONS.md`.

## Stack y versiones clave

Kotlin 2.0.21 · Compose BOM 2024.12.01 · AGP 8.7.3 · Gradle 8.11.1 · Hilt 2.52 ·
Room 2.6.1 · Coroutines 1.9.0 · Navigation Compose 2.8.5 · KSP 2.0.21-1.0.27 ·
ktlint 12.1.2. compile/targetSdk 35 · minSdk 24 · Java/Kotlin 17.

## Arquitectura (resumen)

```
app ──► feature:overlay ──► core:platform ─► domain
  │          │                 │
  │          └──────► data ────┘
  ├──► feature:settings ─► data
  ├──► feature:history  ─► data
  ├──► feature:onboarding ─► data
  └──► core:ui ──────────► domain (tipos)
```

- `domain`: modelos (incluye `DriverConfig`/`DriverProfile`/`DriverVehicle`/
  `FuelType`/`AdditionalCost`), `ProfitEngine`, use cases, contratos de
  repositorio.
- `data`: Room + repositorios concretos + Hilt (`DatabaseModule` con migración
  1→2, `RepositoryModule`).
- `core:platform`: parser y extractores multi-plataforma.
- `core:ui`: design system.
- `feature:overlay`: accesibilidad + overlay + pipeline de evaluación. Estado
  del overlay vía `OverlayDataSource` (simulado); permisos y control vía
  `PermissionManager` y `OverlayManager`.
- `feature:onboarding`: flujo de configuración inicial (6 pasos) que persiste
  `DriverConfig`; gating en `app` (`RootViewModel`/`SircRoot`).
- `feature:settings` / `feature:history`: UI.
- `app`: entrada, gating de onboarding y navegación (4 destinos, incluido
  Diagnóstico).

## Comandos de verificación (Windows / PowerShell)

```powershell
$env:JAVA_HOME = "C:\Users\Jeivi\AppData\Local\Temp\opencode\jdk17\jdk-17.0.20+8"
$env:ANDROID_HOME = "C:\Users\Jeivi\AppData\Local\Temp\opencode\sdk"
.\gradlew.bat ktlintCheck --console=plain --max-workers=2
.\gradlew.bat lintDebug assembleDebug testDebugUnitTest :domain:test :core:platform:test --console=plain --max-workers=2
```

- `ktlintFormat` NO desactiva reglas: si rompe `@Inject constructor`, es que
  falta `ktlint_standard_annotation = disabled` en `.editorconfig` (ya presente).
- Gradle con `--max-workers=2` para evitar fallos de workers de ktlint.

## Qué NO se debe hacer

Ver `.ai/RULES.md` (lista completa). Lo esencial:

- No romper la arquitectura (dependencias hacia adentro).
- El overlay es la prioridad absoluta del producto.
- No agregar dependencias ni funcionalidades que no existan ("documentar solo
  lo real").
- Accessibility Service solo lectura; prohibido `performAction`/gestos.
- Mantener bajo consumo de batería.
- No duplicar información que ya muestra la plataforma.

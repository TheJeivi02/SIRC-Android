# SIRC — PROJECT.md

> Documentación derivada exclusivamente del estado actual del repositorio.
> Fecha de referencia: v0.1.0.

## Objetivo del proyecto

**SIRC (Sistema Inteligente de Rentabilidad para Conductores)** es una aplicación
Android nativa para que conductores de plataformas de movilidad (Uber, DiDi,
Cabify, InDrive) decidan en **menos de 3 segundos** si una oferta de viaje es
rentable.

La app lee —sin interactuar— el contenido visible de la plataforma mediante un
**Accessibility Service de solo lectura**, detecta la oferta en pantalla y
muestra en un **overlay flotante** la información **derivada** que permite
decidir: ganancia estimada, ganancia/hora, ganancia/km y una insignia de
decisión con colores tipo semáforo.

El MVP se concentra en: Overlay, Accessibility, Motor de Rentabilidad,
Configuración y persistencia local de un historial básico.

## Filosofía del proyecto

- **No repetir información.** El conductor ya ve el monto, la distancia y el
  tiempo en la app de la plataforma. SIRC NO la repite: solo muestra
  **información derivada** (ganancia y métricas de rentabilidad).
- **Reconocer, no leer.** Las decisiones se comunican con colores semáforo y
  pocos indicadores (máximo 4) para que la lectura sea instantánea.
- **Solo lectura.** El Accessibility Service no toca botones, no acepta/rechaza
  viajes y no simula gestos. Cumplimiento estricto de la política de Google
  Play (ver `GOOGLE_PLAY_COMPLIANCE.md`).
- **Multi-plataforma desacoplada.** Uber, DiDi, Cabify e InDrive se tratan como
  plataformas desacopladas con extractores por palabras clave, sin depender de
  APIs públicas inexistentes.
- **Batería y rendimiento.** Traversals limitados, deduplicación de frames y un
  overlay liviano: el servicio debe ser imperceptible en consumo.
- **Calidad de producción.** Clean Architecture, MVVM, pruebas unitarias,
  ktlint, lint y CI desde el inicio.

## Estado actual

El proyecto es un **MVP completo y compilable**:

| Entregable | Estado |
|---|---|
| Proyecto Android multi-módulo compilable | ✅ |
| Clean Architecture + MVVM + Modularización | ✅ |
| Overlay flotante (`TYPE_APPLICATION_OVERLAY`) | ✅ |
| Accessibility Service (solo lectura) | ✅ |
| Motor de Rentabilidad (puro, testeado) | ✅ |
| Configuración de costos, umbrales y overlay | ✅ |
| Historial básico + persistencia Room | ✅ |
| Hilt, Coroutines, Flow | ✅ |
| CI (GitHub Actions) + ktlint + lint | ✅ |
| Cumplimiento Google Play (política de Accesibilidad) | ✅ |

Pruebas unitarias existentes: 14 (6 en `:domain`, 8 en `:core:platform`).

## Módulos existentes

| Módulo | Tipo | Propósito |
|---|---|---|
| `:app` | Android application | Entrada de la app, navegación, Home. |
| `:domain` | Kotlin/JVM puro | Modelos, `ProfitEngine`, use cases, contratos de repositorio. |
| `:data` | Android library | Room (`SircDatabase`, entidades, DAOs), repositorios concretos, DI de datos. |
| `:core:platform` | Kotlin/JVM puro | `OfferTextParser` y extractores multi-plataforma. |
| `:core:ui` | Android library | Design system Compose (tema + componentes). |
| `:feature:overlay` | Android library | Accessibility Service, Overlay Service y UI del overlay. |
| `:feature:settings` | Android library | Configuración de costos, umbrales e indicadores. |
| `:feature:history` | Android library | Historial de ofertas evaluadas. |

## Tecnologías utilizadas

- **Kotlin 2.0.21** · **Jetpack Compose** (BOM 2024.12.01) · **Material Design 3**
- **Gradle 8.11.1** · **AGP 8.7.3** · **Version Catalog** (`gradle/libs.versions.toml`)
- **Hilt 2.52** (DI) · **Room 2.6.1** (persistencia) · **Coroutines/Flow 1.9.0**
- **Navigation Compose 2.8.5** · **Lifecycle 2.8.7** · **Activity Compose 1.9.3**
- **KSP 2.0.21-1.0.27**
- **ktlint 12.1.2** · **Android Lint** · **GitHub Actions** (CI)
- **SDKs:** compile/target 35 · minSdk 24 · Java/Kotlin 17

## Conventions y gobernanza

- Arquitectura y decisiones: `docs/ARCHITECTURE.md`.
- Estándares de código y reglas de arquitectura: `docs/CODING_STANDARDS.md`.
- Cumplimiento de políticas de Play: `docs/GOOGLE_PLAY_COMPLIANCE.md`.
- Historial de versiones: `docs/CHANGELOG.md`.
- Plan de sprints: `docs/ROADMAP.md`.
- Contexto para sesiones de IA: `.ai/CONTEXT.md`.

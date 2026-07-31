# Roadmap

Plan de desarrollo del proyecto. El estado de cada sprint refleja el repositorio
actual.

## Sprint 1 — Proyecto base ✅

Scaffolding Gradle multi-módulo con Version Catalog, wrapper, `.editorconfig`,
`.gitignore`, CI (GitHub Actions), ktlint y lint. Arquitectura Clean +
MVVM + modularización en 8 módulos (`:app`, `:domain`, `:data`, `:core:ui`,
`:core:platform`, `:feature:overlay`, `:feature:settings`, `:feature:history`).

- Estado: **completado**.

## Sprint 2 — Design System + Overlay Foundation ✅

Base visual reutilizable (Material 3) y el overlay flotante sobre las apps de
transporte.

**Design System (`:core:ui`):**

- `SircTheme`, `SircColors` (semáforo), `SircTypography`, `SircSpacing` y
  `SircElevations`.
- `ProfitState` + `ProfitIndicator` (insignia semáforo), `DecisionBadge`,
  `StatusDot`, `SectionCard`, `LabeledValue`, `MetricCell`/`MetricValue`,
  `OverlayCard`/`OverlayCardContent`.
- `@Preview` y KDoc en todos los componentes; pruebas unitarias de paleta y
  estados.

**Overlay:**

- `OverlayService` (Foreground Service) con `TYPE_APPLICATION_OVERLAY`.
- `OverlayContent`: insignia de decisión y hasta 4 indicadores, consumiendo los
  componentes de `:core:ui`.
- `OverlayConfig`: indicadores visibles, modo compacto, opacidad, TTL, posición.
- `OverlayDataSource`/`SimulatedOverlayDataSource`: datos simulados evaluados
  con el `ProfitEngine` real.
- `PermissionManager`, `OverlayManager` y `OverlayController` (control del
  servicio).
- `OverlayViewModel` (`@HiltViewModel`) y `OverlayModule` (`@Binds`).
- Pantalla Diagnóstico con 5 indicadores y vista previa simulada.

- Estado: **completado**.

## Sprint 3 — Accessibility

Canal de lectura del contenido visible de las plataformas.

- `SircAccessibilityService` (solo lectura, filtrado por paquete).
- Traversal con límites duros y deduplicación por huella de texto.
- `OfferEventBus` como puente hacia el evaluador.

- Estado: **implementado en el MVP**.

## Sprint 4 — Motor de Rentabilidad

Decisión de rentabilidad en menos de 3 segundos.

- `ProfitEngine` (función pura): ganancia, ganancia/hora, ganancia/km, margen.
- `DecisionThresholds` (ganancia mínima y mínima por hora) y `Decision`
  (`PROFITABLE`, `MARGINAL`, `NOT_PROFITABLE`).
- `OfferTextParser` + extractores por plataforma en `:core:platform`.
- Pruebas unitarias del motor y del parser.

- Estado: **implementado en el MVP**.

## Sprint 5 — Persistencia

Persistencia local de configuración e historial.

- Room (`SircDatabase`): `driver_config`, `overlay_config`, `offer_history`.
- Repositorios `Default*` y mappers, con valores por defecto.
- Historial de ofertas evaluadas (límite 100) en `:feature:history`.

- Estado: **implementado en el MVP**.

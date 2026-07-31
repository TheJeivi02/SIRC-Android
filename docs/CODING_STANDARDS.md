# Estándares de Código

> Reglas que todo código del proyecto debe cumplir. Verificadas por ktlint y
> Android Lint en CI; las reglas de arquitectura se revisan en code review.

## 1. Estilo Kotlin

- **Formato gobernado por ktlint** con la configuración de `.editorconfig`:
  - `indent_size = 4` espacios, `max_line_length = 120`.
  - `end_of_line = lf`, `insert_final_newline = true`,
    `trim_trailing_whitespace = true`, `charset = utf-8`.
  - Trailing commas habilitados en declaraciones y call-sites.
  - `ktlint_function_naming_ignore_when_annotated_with = Composable`
    (permite nombres `PascalCase` en composables).
  - `ktlint_standard_annotation = disabled` (permite el idiomático
    `class X @Inject constructor(...)`).
- `ktlintCheck` debe pasar en todos los módulos. Si algo no es auto-corregible
  (`ktlintFormat`), se arregla a mano y se vuelve a ejecutar hasta quedar verde.
- Identación de 4 espacios, cuerpo de clase sin línea en blanco inicial.
- Líneas de más de 120 caracteres se parten (regex largas se concatenan con `+`
  entre literales).
- Los nombres en Kotlin siguen las convenciones del lenguaje:
  `PascalCase` para clases/objetos/enums, `camelCase` para funciones y
  variables, `SCREAMING_SNAKE_CASE` para constantes.

## 2. Convenciones del proyecto

- **Idioma del código**: nombres de clases, funciones y variables en inglés;
  textos de UI (etiquetas, `DecisionBadge`, razones) en español.
- **Inyección de dependencias con Hilt**: constructores `@Inject`, `@HiltViewModel`
  para ViewModels, `@AndroidEntryPoint` para `Activity`/`Service`/`Application`,
  `@Binds` para implementaciones de repositorios. Evitar `@Provides` salvo que
  se necesite contexto o construcción manual (p. ej. Room).
- **Flows**: los ViewModels exponen `StateFlow` inmutable
  (`asStateFlow()`, `MutableStateFlow` privado). `stateIn(WhileSubscribed(...))`
  para observables que no deben correr en vacío.
- **Módulos puros (`:domain`, `:core:platform`)**: no importan nada de Android;
  solo `kotlinx-coroutines-core` y `javax.inject` (anotaciones).
- **Colores y componentes**: usar siempre el design system de `:core:ui`
  (`SircColors`, `SircTypography`, `DecisionBadge`, `StatusDot`, `SectionCard`,
  `LabeledValue`). No duplicar estilos en cada feature.
- **Nombres de archivo**: coinciden con la declaración única de nivel superior
  (regla de ktlint `filename`).

## 3. Reglas de arquitectura

- **Flujo de dependencias unidireccional hacia adentro**:
  `UI → ViewModel → use case → dominio → (repositorio)`. La UI nunca toca
  `data` ni `core:platform` directamente; los ViewModels usan use cases.
- **`domain` es el núcleo**: modelos, `ProfitEngine` (función pura) y contratos
  de repositorio viven ahí. No poner lógica Android en `domain`.
- **Los contratos de repositorio se definen en `domain`**; `data` los
  implementa. Los features dependen de `data` solo para la implementación
  concreta inyectada por Hilt.
- **`core:platform` solo parsea texto** (puro); no conoce la UI ni la
  persistencia.
- **Agregar una plataforma**: nuevo `RidePlatform` + descriptor de palabras
  clave en `core:platform` + entrada de `<queries>` y `packageNames` del
  accessibility config. No modificar el núcleo.
- **Regresión de arquitectura = error de revisión**, incluso si compila.

## 4. Principios SOLID

- **S — Responsabilidad única**: clases con un solo motivo de cambio
  (`OfferEvaluator` orquesta; `ProfitEngine` calcula; `OverlayService` dibuja).
- **O — Abierto/cerrado**: las plataformas se agregan sin modificar el
  pipeline (extractor + descriptor).
- **L — Sustitución de Liskov**: los repositorios concretos respetan los
  contratos de `domain` (incluidos los valores por defecto).
- **I — Segregación de interfaces**: contratos pequeños y específicos
  (`DriverConfigRepository`, `OverlayConfigRepository`,
  `OfferHistoryRepository`).
- **D — Inversión de dependencias**: `domain` define interfaces; `data`
  implementa; Hilt inyecta. Nunca dependencia de concreto→abstracción invertida.

## 5. Clean Architecture

- Capas: **Domain** (reglas de negocio puras), **Data** (persistencia),
  **Core** (utilidades compartidas: UI, platform), **Presentation/Features**
  (UI + ViewModels).
- El `ProfitEngine` es una función pura sin estado ni I/O: recibe todo por
  parámetros y devuelve un `ProfitEvaluation`.
- Los use cases no guardan estado; delegan en repositorios y el engine.
- `domain` y `core:platform` deben poder probarse con JUnit puro (sin
  Robolectric/emulador).

## 6. MVVM

- Cada pantalla tiene un `ViewModel` (`@HiltViewModel`) que expone
  `StateFlow<UiState>` y expone acciones como funciones.
- La UI observa el estado con `collectAsStateWithLifecycle()`.
- Los eventos de una sola vez (navegación, snackbars) se mantienen fuera del
  estado persistente o se modelan explícitamente.
- El overlay no es MVVM clásico: usa `OfferEvaluator` (Singleton) como fuente de
  verdad y `OverlayUiState` como estado.

## 7. Buenas prácticas

- **Batería y rendimiento**:
  - Traversal de accesibilidad con límites duros (`MAX_NODES`, `MAX_TEXTS`,
    `MAX_TEXT_LENGTH`).
  - Deduplicación de frames (huella de texto) para no re-evaluar.
  - Un solo `ComposeView` en el overlay, retirado de `WindowManager` cuando no
    hay oferta.
  - Evitar trabajo en `onAccessibilityEvent` que no sea necesario.
- **Seguridad y privacidad**:
  - 100 % local: sin backend, sin telemetría, sin anuncios.
  - No registrar ni transmitir contenido de pantalla.
  - El Accessibility Service **solo lee**; prohibido `performAction`,
    `dispatchGesture` y automatizar decisiones de la plataforma.
- **Persistencia**: Room con `exportSchema = true` y esquemas versionados en
  `data/schemas/` para migraciones futuras.
- **Tests**: la lógica crítica (engine, parser, extractores) siempre con
  pruebas unitarias JUnit 4. Toda corrección de lógica debe añadir o ajustar un
  test.
- **Documentación**: cualquier cambio de arquitectura o decisión relevante debe
  actualizar `docs/ARCHITECTURE.md` (sección de decisiones) y el `CHANGELOG.md`.
- **Commits**: mensajes claros y atómicos; no incluir secretos, `local.properties`
  ni artefactos `build/`.

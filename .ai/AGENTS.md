# Agentes recomendados

Roles de referencia para dividir el trabajo con agentes de IA o entre personas.
Cada agente respeta `.ai/RULES.md`, `docs/ARCHITECTURE.md` y
`docs/CODING_STANDARDS.md`.

## Android Architect

- Diseña y mantiene la arquitectura Clean + MVVM y la modularización
  (8 módulos).
- Controla el grafo de dependencias y garantiza que `:domain` y `:core:platform`
  sigan siendo Kotlin puro.
- Decide versiones de herramientas (AGP, Gradle, Kotlin, catálogo), scripting de
  build y configuración de CI.
- Valida que toda decisión técnica quede registrada en
  `docs/ARCHITECTURE.md` (sección "Decisiones técnicas").

## UI Engineer

- Construye las pantallas Compose (`:app`, `:feature:settings`,
  `:feature:history`, `:core:ui`).
- Mantiene el design system: `SircTheme`, `SircColors`, `SircTypography` y los
  componentes (`DecisionBadge`, `StatusDot`, `SectionCard`, `LabeledValue`).
- Aplica MVVM: `StateFlow` + `collectAsStateWithLifecycle()`, sin lógica de
  negocio en composables.
- Prioriza legibilidad y velocidad de lectura (colores semáforo, ≤4 indicadores).

## Accessibility Engineer

- Es dueño del `CaptureAccessibilityService` y de `accessibility_service_config.xml`.
- Garantiza que el servicio sea **solo lectura**: sin `performAction`, sin
  gestos, sin interacción con otras apps.
- Mantiene los límites de rendimiento (400 nodos, 80 textos, deduplicación por
  huella) y el cumplimiento de la política de Google Play.

## Profit Engine Engineer

- Es dueño del `ProfitEngine` y de `:domain` (modelos, use cases, contratos).
- Mantiene la función pura de evaluación y los umbrales de decisión.
- Extiende la detección de ofertas: `OfferTextParser` y extractores por
  plataforma en `:core:platform` (agregar plataforma = nuevo descriptor).
- Escribe y mantiene las pruebas unitarias del motor y del parser.

## Security Engineer

- Revisa permisos, Privacy Sandbox/Data safety y superficies de exposición.
- Verifica que todo sea 100 % local: sin telemetría, sin backend, sin fuga de
  contenido de pantalla.
- Audita el overlay (`TYPE_APPLICATION_OVERLAY`, `FLAG_NOT_FOCUSABLE`) y el
  manejo de notificaciones (FGS `specialUse`).
- Mantiene al día `docs/GOOGLE_PLAY_COMPLIANCE.md`.

## QA Engineer

- Ejecuta el ciclo de verificación completo: `ktlintCheck`, `lintDebug`,
  `testDebugUnitTest`, `:domain:test`, `:core:platform:test`, `assembleDebug`.
- Mantiene y expande las pruebas unitarias de `:domain` y `:core:platform`.
- Registra defectos y requisitos de regresión (rendimiento, batería, <3 s).
- Valida la compatibilidad (minSdk 24 → targetSdk 35) y el cumplimiento de Play.

# WP-E3-02 — Framework Genérico de Detección (spec)

> Fecha: 2026-08-06 · Estado: aprobado por el usuario · Epic: EPIC-03 (Platform Agnostic Detection)

## Objetivo

Implementar un framework genérico de detección 100 % descriptor-driven en
`:core:platform`. La detección depende únicamente de la información contenida en
cada `PlatformDescriptor`; agregar una plataforma nunca requiere nuevas clases de
detección.

No se modifican: OCR, Overlay, Capture, ProfitEngine, `PlatformDescriptor`
público ni los consumidores actuales. `PlatformDescriptorRegistry` sigue siendo
la única fuente oficial de plataformas y no adquiere lógica de negocio.

## Restricciones

- No crear detectores específicos (`UberDetector`, `DiDiDetector`, etc.).
- No usar `when(platform)` ni `if(platform)` para la detección.
- `:core:platform` permanece Kotlin puro: sin Android, sin logging, sin I/O, sin
  callbacks hacia fuera.
- API pública existente intacta (`OfferParserOrchestrator.parse(texts, ts, platform)`).

## Componentes y responsabilidades

```
PlatformDescriptorRegistry   (solo datos validados y precompilados)
        ↓  expone Collection<PlatformDescriptor> de solo lectura
PlatformDetectionEngine      (servicio independiente, estrategia completa)
        ↓  usa
DetectionMatcher             (función pura, determinista, sin estado, sin I/O)
        ↓  produce
DetectionResult              (inmutable, autocontenido)
        ↓  consume
OfferParserOrchestrator      (traduce a ParsedOffer, sin re-recorrer nada)
```

### PlatformDescriptorRegistry

- Valida descriptores durante la construcción (comportamiento ya existente).
- Precompila reglas, extractores y motores (comportamiento ya existente).
- **Cambio mínimo:** expone una vista de solo lectura
  `val descriptors: Collection<PlatformDescriptor>` (inmutable; no se exponen los
  mapas internos). Sin lógica de negocio añadida.

### PlatformDetectionEngine

- Servicio independiente que consume `PlatformDescriptorRegistry`.
- Contrato: `detect(texts: List<String>, timestampMillis: Long, packageName: String? = null): DetectionResult`.
- No conoce el origen de los textos (OCR, galería, tests, etc.). El origen lo
  declara el llamador vía `DetectionOrigin`.
- Ejecuta la estrategia completa en una sola pasada; no vuelve a recorrer los
  descriptores para recalcular pantalla ni keywords.
- No contiene lógica de parsing.

### DetectionMatcher

- Función pura, determinista y sin estado.
- `matchesPackage(packageNames: List<String>, packageName: String): Boolean` —
  normaliza (minúsculas/trim) y compara.
- `matchScore(descriptor: PlatformDescriptor, normalizedTexts: List<String>): Int` —
  por ahora el score es el número de keywords de detección del descriptor
  presentes en los textos (cuenta las keywords de **todas** las `detectionRules`
  del descriptor, sin duplicados). El nombre permite evolucionar el algoritmo sin
  romper la API.

### DetectionResult

Data class inmutable y autocontenido:

- `resolution: DetectionResolution` — forma oficial de indicar cómo se detectó
  la plataforma (`PACKAGE_MATCH`, `KEYWORD_CANDIDATE`, `AMBIGUOUS`, `NONE`).
- `origin: DetectionOrigin` — procedencia de los textos
  (`PACKAGE`, `OCR`, `GALLERY`, `TEST`, `UNKNOWN`).
- `descriptor: PlatformDescriptor?` — el ganador (`null` si AMBIGUOUS/NONE).
- `screenDetection: ScreenDetection` — ya calculado; nunca se recalcula.
- `candidates: List<DetectionCandidate>` — diagnóstico de la etapa keyword.
- `sourcePackage: String?` — paquete de origen (opcional, diagnóstico de
  PACKAGE_MATCH).
- Helper `isRecognized: Boolean` = `resolution == PACKAGE_MATCH || resolution == KEYWORD_CANDIDATE`.

### DetectionCandidate

Data class de diagnóstico: `descriptor: PlatformDescriptor`,
`screenDetection: ScreenDetection`, `matchScore: Int`.

### DetectionResolution

Enum: `PACKAGE_MATCH`, `KEYWORD_CANDIDATE`, `AMBIGUOUS`, `NONE`.

### DetectionOrigin

Enum: `PACKAGE`, `OCR`, `GALLERY`, `TEST`, `UNKNOWN`. No modifica el
comportamiento actual; deja preparado el framework para capturas desde galería,
pruebas internas y futuros laboratorios de detección.

## Estrategia del engine (determinista, sin scoring heurístico)

1. **Etapa 1 — Paquete:** si `packageName != null`, busca en una sola pasada el
   descriptor cuyo `packageNames` matchee (normalizado). Si hay match único →
   `PACKAGE_MATCH`, con `screenDetection` calculado con el `OfferDetectionEngine`
   precompilado de ese descriptor.
2. **Etapa 2 — Keywords:** si no hubo match de paquete, en la segunda pasada
   evalúa cada descriptor con su engine precompilado; candidato válido =
   `screenDetection.type != UNKNOWN`. Construye la lista de candidatos con su
   `matchScore`.
3. **Etapa 3 — Único candidato:** si hay exactamente un candidato →
   `KEYWORD_CANDIDATE`.
4. **Etapa 4 — Empate:** si varios candidatos comparten el mayor `matchScore` →
   `AMBIGUOUS` (sin elegir arbitrariamente).
5. **Etapa 5 — Ninguno:** sin candidatos → `NONE`.

Sin logging, sin I/O, sin callbacks: `:core:platform` sigue 100 % Kotlin puro.

## OfferParserOrchestrator (backward compatible)

- **Nuevo overload:** `parse(texts, timestampMillis, packageName: String): ParsedOffer`
  → invoca `PlatformDetectionEngine`; si `isRecognized && screenDetection.isRequest`
  continúa con el parseo vía un método privado compartido
  `parseWith(descriptor, screenDetection, texts, timestampMillis)`.
- **Método existente** `parse(texts, timestampMillis, platform)` se conserva
  intacto en firma y delega al mismo `parseWith` (misma lógica de variantes +
  extractor genérico actual; cero cambio funcional).
- `AMBIGUOUS`/`NONE` → `ParsedOffer.none()`.

## Tests (TDD, RED → GREEN)

- `DetectionMatcherTest` — pureza, normalización, `matchScore`.
- `PlatformDetectionEngineTest` — las 5 etapas (PACKAGE_MATCH único,
  KEYWORD_CANDIDATE único, AMBIGUOUS por empate, NONE, textos vacíos,
  packageName null, origin).
- `OfferParserOrchestratorTest` — overload por packageName que parsea;
  AMBIGUOUS → none(); NONE → none().
- `PlatformDescriptorRegistryTest` — vista de solo lectura inmutable.

## Documentación

Actualizar `docs/CHANGELOG.md`, `.ai/CONTEXT.md`, `.ai/DECISIONS.md`
(decisión arquitectónica WP-E3-02).

## Verificación

`assembleDebug`, `lintDebug`, `testDebugUnitTest`, `:core:platform:test`,
`:core:capture:test`, `:domain:test`, `ktlintCheck` (documentando solo fallos
preexistentes).

## Fuera de alcance

- Nuevas funcionalidades de producto.
- Conectar el framework al pipeline de captura real (`:core:capture` está
  prohibido de modificar en este WP).
- Telemetría/logging desde `:core:platform` (si se requiere, vía adaptadores
  fuera del motor).

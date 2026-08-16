# WP-E3-02 — Framework Genérico de Detección — Plan de Implementación

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar un framework genérico de detección 100 % descriptor-driven en `:core:platform` (`PlatformDetectionEngine` → `DetectionMatcher` → `DetectionResult`) con un overload backward-compatible en `OfferParserOrchestrator`.

**Architecture:** `PlatformDescriptorRegistry` conserva solo datos validados/precompilados y expone una vista de solo lectura de descriptores. `PlatformDetectionEngine` es un servicio independiente que recorre los descriptores (O(n)) en una sola pasada, resuelve plataforma por paquete (PACKAGE_MATCH) o por keywords (KEYWORD_CANDIDATE/AMBIGUOUS/NONE) y produce un `DetectionResult` inmutable y autocontenido. `OfferParserOrchestrator` consume `DetectionResult` para parsear sin re-recorrer nada.

**Tech Stack:** Kotlin puro (JVM), JUnit4, Gradle (Windows / PowerShell: `.\gradlew.bat`). Sin dependencias nuevas.

## Global Constraints

- `:core:platform` es **Kotlin puro**: sin Android, sin logging, sin I/O, sin callbacks.
- No crear detectores específicos (`UberDetector`, `DiDiDetector`, etc.) ni `when(platform)`/`if(platform)` para detección.
- No modificar: OCR, Overlay, Capture, ProfitEngine, `PlatformDescriptor` público, ni consumidores actuales.
- API pública existente intacta: `OfferParserOrchestrator.parse(texts, timestampMillis, platform)` no cambia de firma.
- ktlint: max line length 120; trailing comma en listas/multiline de data class; sin lambdas multiline en `data class` (patrón `OfferTypeVariant`).
- Comandos: `.\gradlew.bat :core:platform:test --console=plain` (Windows). NO usar `2>&1 | Select-Object -Last 30` (rompe el proceso con `ChildProcess.kill`).

---

### Task 1: Tipos del framework — `DetectionResolution`, `DetectionOrigin`, `DetectionCandidate`, `DetectionResult`

**Files:**
- Create: `core/platform/src/main/kotlin/com/sirc/core/platform/DetectionResolution.kt`
- Create: `core/platform/src/main/kotlin/com/sirc/core/platform/DetectionOrigin.kt`
- Create: `core/platform/src/main/kotlin/com/sirc/core/platform/DetectionCandidate.kt`
- Create: `core/platform/src/main/kotlin/com/sirc/core/platform/DetectionResult.kt`
- Test: `core/platform/src/test/kotlin/com/sirc/core/platform/DetectionResultTest.kt`

**Interfaces:**
- Consumes: `PlatformDescriptor` (`core/platform/.../PlatformDescriptor.kt`), `ScreenDetection` (`core/platform/.../ScreenDetection.kt`).
- Produces (usado por Tasks 3-5):
  - `enum DetectionResolution { PACKAGE_MATCH, KEYWORD_CANDIDATE, AMBIGUOUS, NONE }`
  - `enum DetectionOrigin { PACKAGE, OCR, GALLERY, TEST, UNKNOWN }`
  - `data class DetectionCandidate(val descriptor: PlatformDescriptor, val screenDetection: ScreenDetection, val matchScore: Int)`
  - `data class DetectionResult(resolution, origin, descriptor: PlatformDescriptor? = null, screenDetection: ScreenDetection = ScreenDetection(ScreenType.UNKNOWN), candidates: List<DetectionCandidate> = emptyList(), sourcePackage: String? = null)` con `val isRecognized: Boolean` (true si `resolution == PACKAGE_MATCH || resolution == KEYWORD_CANDIDATE`).

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.sirc.core.platform

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionResultTest {
    private fun result(resolution: DetectionResolution): DetectionResult =
        DetectionResult(
            resolution = resolution,
            origin = DetectionOrigin.PACKAGE,
        )

    @Test
    fun `PACKAGE_MATCH se reconoce como plataforma detectada`() {
        assertTrue(result(DetectionResolution.PACKAGE_MATCH).isRecognized)
    }

    @Test
    fun `KEYWORD_CANDIDATE se reconoce como plataforma detectada`() {
        assertTrue(result(DetectionResolution.KEYWORD_CANDIDATE).isRecognized)
    }

    @Test
    fun `AMBIGUOUS no se reconoce`() {
        assertFalse(result(DetectionResolution.AMBIGUOUS).isRecognized)
    }

    @Test
    fun `NONE no se reconoce`() {
        assertFalse(result(DetectionResolution.NONE).isRecognized)
    }

    @Test
    fun `valores por defecto exponen pantalla UNKNOWN y sin candidatos`() {
        val r = DetectionResult(resolution = DetectionResolution.NONE, origin = DetectionOrigin.UNKNOWN)

        assertEquals(ScreenType.UNKNOWN, r.screenDetection.type)
        assertTrue(r.candidates.isEmpty())
        assertNull(r.descriptor)
        assertNull(r.sourcePackage)
    }

    @Test
    fun `candidate expone descriptor, screenDetection y matchScore`() {
        val candidate =
            DetectionCandidate(
                descriptor = PlatformDescriptors.UBER,
                screenDetection = ScreenDetection(ScreenType.REQUEST),
                matchScore = 3,
            )

        assertEquals(RidePlatform.UBER, candidate.descriptor.platform)
        assertEquals(ScreenType.REQUEST, candidate.screenDetection.type)
        assertEquals(3, candidate.matchScore)
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :core:platform:test --console=plain`
Expected: FAIL (tipos sin resolver).

- [ ] **Step 3: Write minimal implementations**

`DetectionResolution.kt`:
```kotlin
package com.sirc.core.platform

/**
 * Forma oficial de indicar cómo se detectó la plataforma.
 */
enum class DetectionResolution {
    /** La plataforma se resolvió por coincidencia exacta de packageName. */
    PACKAGE_MATCH,

    /** La plataforma se resolvió por keywords de detección (único candidato). */
    KEYWORD_CANDIDATE,

    /** Varios descriptores empataron en prioridad; no se elige ninguno. */
    AMBIGUOUS,

    /** Ningún descriptor coincidió. */
    NONE,
}
```

`DetectionOrigin.kt`:
```kotlin
package com.sirc.core.platform

/**
 * Procedencia de los textos analizados.
 *
 * No modifica el comportamiento actual; deja preparado el framework para
 * capturas desde galería, pruebas internas y futuros laboratorios de
 * detección.
 */
enum class DetectionOrigin {
    /** Textos etiquetados con el package de origen. */
    PACKAGE,

    /** Textos provenientes de OCR en tiempo real. */
    OCR,

    /** Captura almacenada (p. ej. galería). */
    GALLERY,

    /** Textos de prueba unitaria. */
    TEST,

    /** Origen no informado. */
    UNKNOWN,
}
```

`DetectionCandidate.kt`:
```kotlin
package com.sirc.core.platform

/**
 * Candidato de la etapa de detección por keywords: el descriptor evaluado, la
 * pantalla detectada y su [matchScore] (diagnóstico).
 */
data class DetectionCandidate(
    val descriptor: PlatformDescriptor,
    val screenDetection: ScreenDetection,
    val matchScore: Int,
)
```

`DetectionResult.kt`:
```kotlin
package com.sirc.core.platform

/**
 * Resultado autocontenido de la detección genérica de plataforma.
 *
 * Encapsula toda la información necesaria para continuar el parseo
 * ([descriptor], [screenDetection]) y para diagnosticar por qué se llegó al
 * resultado ([resolution], [candidates], [sourcePackage]). No vuelve a recorrer
 * descriptores: el [OfferParserOrchestrator] consume este objeto tal cual.
 */
data class DetectionResult(
    val resolution: DetectionResolution,
    val origin: DetectionOrigin,
    val descriptor: PlatformDescriptor? = null,
    val screenDetection: ScreenDetection = ScreenDetection(ScreenType.UNKNOWN),
    val candidates: List<DetectionCandidate> = emptyList(),
    val sourcePackage: String? = null,
) {
    val isRecognized: Boolean
        get() = resolution == DetectionResolution.PACKAGE_MATCH || resolution == DetectionResolution.KEYWORD_CANDIDATE
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat :core:platform:test --console=plain`
Expected: PASS.

- [ ] **Step 5: Run ktlint on the module**

Run: `.\gradlew.bat :core:platform:ktlintCheck --console=plain`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/platform/src/main/kotlin/com/sirc/core/platform/DetectionResolution.kt core/platform/src/main/kotlin/com/sirc/core/platform/DetectionOrigin.kt core/platform/src/main/kotlin/com/sirc/core/platform/DetectionCandidate.kt core/platform/src/main/kotlin/com/sirc/core/platform/DetectionResult.kt core/platform/src/test/kotlin/com/sirc/core/platform/DetectionResultTest.kt
git commit -m "feat(platform): detection framework value types (WP-E3-02)"
```

---

### Task 2: `PlatformDescriptorRegistry` — vista de solo lectura de descriptores

**Files:**
- Modify: `core/platform/src/main/kotlin/com/sirc/core/platform/PlatformDescriptorRegistry.kt` (añadir `val descriptors` + init)
- Test: `core/platform/src/test/kotlin/com/sirc/core/platform/PlatformDescriptorRegistryTest.kt` (añadir 2 tests)

**Interfaces:**
- Consumes: nada nuevo.
- Produces (usado por Task 3): `registry.descriptors: Collection<PlatformDescriptor>` — vista de solo lectura de los descriptores validados, en el orden original.

- [ ] **Step 1: Write the failing tests**

Añadir al final de `PlatformDescriptorRegistryTest.kt` (antes de la sección `// --- Helpers ---`):

```kotlin
    @Test
    fun `expone los descriptores como coleccion de solo lectura`() {
        val registry = PlatformDescriptorRegistry(listOf(validDescriptor(), validDescriptor(RidePlatform.DIDI)))

        assertEquals(2, registry.descriptors.size)
        assertEquals(listOf(RidePlatform.UBER, RidePlatform.DIDI), registry.descriptors.map { it.platform })
    }

    @Test
    fun `la vista de descriptores es inmutable ante modificaciones externas`() {
        val descriptors = mutableListOf(validDescriptor())
        val registry = PlatformDescriptorRegistry(descriptors)

        descriptors.clear()

        assertEquals(1, registry.descriptors.size)
    }
```

Añadir import `assertEquals` (ya existe) — verificar imports: `assertNull`, `assertSame`, `assertTrue`, `fail` ya están. `assertEquals` también. OK.

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :core:platform:test --console=plain`
Expected: FAIL (referencia `registry.descriptors` sin resolver).

- [ ] **Step 3: Write minimal implementation**

En `PlatformDescriptorRegistry.kt`:
- Añadir propiedad y asignación en `init`. El parámetro del constructor se llama `descriptors`; usar `this.descriptors` para la propiedad y el parámetro sin `this.`:

```kotlin
class PlatformDescriptorRegistry(
    descriptors: List<PlatformDescriptor>,
) {
    private val descriptorsByPlatform: Map<RidePlatform, PlatformDescriptor>
    private val descriptorsByPackageName: Map<String, PlatformDescriptor>
    private val detectionEngines: Map<RidePlatform, OfferDetectionEngine>
    private val extractors: Map<RidePlatform, GenericPlatformExtractor>
    private val variantParsers: Map<RidePlatform, List<OfferTypeParser>>

    /** Vista de solo lectura de los descriptores validados y precompilados. */
    val descriptors: Collection<PlatformDescriptor>

    init {
        validate(descriptors)
        this.descriptors = descriptors.toList()
        descriptorsByPlatform = descriptors.associateBy { it.platform }
        // ... resto del init sin cambios ...
    }
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat :core:platform:test --console=plain`
Expected: PASS (todos, incluidos los 2 nuevos).

- [ ] **Step 5: Run ktlint on the module**

Run: `.\gradlew.bat :core:platform:ktlintCheck --console=plain`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/platform/src/main/kotlin/com/sirc/core/platform/PlatformDescriptorRegistry.kt core/platform/src/test/kotlin/com/sirc/core/platform/PlatformDescriptorRegistryTest.kt
git commit -m "feat(platform): expose read-only descriptors view (WP-E3-02)"
```

---

### Task 3: `DetectionMatcher` — función pura de matching

**Files:**
- Create: `core/platform/src/main/kotlin/com/sirc/core/platform/DetectionMatcher.kt`
- Test: `core/platform/src/test/kotlin/com/sirc/core/platform/DetectionMatcherTest.kt`

**Interfaces:**
- Consumes: `PlatformDescriptor`, `OfferDetectionEngine.normalize` (internal, mismo módulo), `PlatformDescriptors.UBER`.
- Produces (usado por Task 4):
  - `fun DetectionMatcher.matchesPackage(packageNames: List<String>, packageName: String): Boolean`
  - `fun DetectionMatcher.matchScore(descriptor: PlatformDescriptor, normalizedTexts: List<String>): Int` — número de keywords de detección del descriptor (todas las `detectionRules`, sin duplicados) presentes en `normalizedTexts`.

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionMatcherTest {
    @Test
    fun `matchesPackage normaliza y compara exacto`() {
        assertTrue(DetectionMatcher.matchesPackage(listOf("com.ubercab"), "  COM.UberCab "))
    }

    @Test
    fun `matchesPackage respeta los aliases del descriptor`() {
        assertTrue(DetectionMatcher.matchesPackage(listOf("com.ubercab", "com.uber"), "com.uber"))
    }

    @Test
    fun `matchesPackage no coincide con paquete distinto`() {
        assertFalse(DetectionMatcher.matchesPackage(listOf("com.ubercab"), "com.cabify.rider"))
    }

    @Test
    fun `matchScore cuenta keywords de deteccion presentes sin duplicados`() {
        val descriptor = PlatformDescriptors.UBER
        val normalized = listOf("aceptar", "rechazar", "nueva solicitud")

        val score = DetectionMatcher.matchScore(descriptor, normalized)

        assertEquals(3, score)
    }

    @Test
    fun `matchScore no cuenta keywords ausentes`() {
        val descriptor = PlatformDescriptors.UBER

        assertEquals(0, DetectionMatcher.matchScore(descriptor, listOf("hola mundo")))
    }

    @Test
    fun `matchScore con textos vacios es cero`() {
        assertEquals(0, DetectionMatcher.matchScore(PlatformDescriptors.UBER, emptyList()))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :core:platform:test --console=plain`
Expected: FAIL (`DetectionMatcher` sin resolver).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.sirc.core.platform

/**
 * Matcher puro, determinista y sin estado de la detección genérica.
 *
 * Normaliza (minúsculas/sin acentos) vía [OfferDetectionEngine.normalize] y
 * compara contra los campos de [PlatformDescriptor]. No conoce de OCR ni de
 * ninguna fuente de textos.
 */
object DetectionMatcher {
    /** True si [packageName] coincide (normalizado) con alguno de [packageNames]. */
    fun matchesPackage(
        packageNames: List<String>,
        packageName: String,
    ): Boolean {
        val normalized = OfferDetectionEngine.normalize(packageName)
        return packageNames.any { OfferDetectionEngine.normalize(it) == normalized }
    }

    /**
     * Puntúa [descriptor] por las keywords de detección presentes en
     * [normalizedTexts]. Por ahora el score es el número de keywords (de todas
     * las [DetectionRule]s del descriptor, sin duplicados); el nombre permite
     * evolucionar el algoritmo sin romper la API.
     */
    fun matchScore(
        descriptor: PlatformDescriptor,
        normalizedTexts: List<String>,
    ): Int {
        val keywords = descriptor.detectionRules.flatMap { it.keywords }.distinct()
        return keywords.count { keyword -> normalizedTexts.any { it.contains(OfferDetectionEngine.normalize(keyword)) } }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat :core:platform:test --console=plain`
Expected: PASS.

- [ ] **Step 5: Run ktlint on the module**

Run: `.\gradlew.bat :core:platform:ktlintCheck --console=plain`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/platform/src/main/kotlin/com/sirc/core/platform/DetectionMatcher.kt core/platform/src/test/kotlin/com/sirc/core/platform/DetectionMatcherTest.kt
git commit -m "feat(platform): pure detection matcher (WP-E3-02)"
```

---

### Task 4: `PlatformDetectionEngine` — estrategia de detección por etapas

**Files:**
- Create: `core/platform/src/main/kotlin/com/sirc/core/platform/PlatformDetectionEngine.kt`
- Test: `core/platform/src/test/kotlin/com/sirc/core/platform/PlatformDetectionEngineTest.kt`

**Interfaces:**
- Consumes: `PlatformDescriptorRegistry` (con `descriptors`, `detectionEngineFor`), `DetectionMatcher`, `ScreenDetection`, `ScreenType`, `DetectionResult`.
- Produces (usado por Task 5):
  - `class PlatformDetectionEngine(private val registry: PlatformDescriptorRegistry)`
  - `fun detect(texts: List<String>, timestampMillis: Long, packageName: String? = null, origin: DetectionOrigin = DetectionOrigin.UNKNOWN): DetectionResult`

- [ ] **Step 1: Write the failing tests**

```kotlin
package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlatformDetectionEngineTest {
    private fun descriptor(
        platform: RidePlatform,
        requestKeywords: List<String>,
        packageNames: List<String> = listOf(platform.packageName),
    ): PlatformDescriptor =
        PlatformDescriptor(
            platform = platform,
            packageNames = packageNames,
            detectionRules =
                listOf(
                    DetectionRule(ScreenType.REQUEST, 3f, requestKeywords),
                    DetectionRule(ScreenType.HOME, 1f, listOf("buscar")),
                ),
            offerTypes = emptyList(),
            extractorKeywords = PlatformKeywords(listOf("total"), listOf("tarifa")),
            defaultCurrency = "MXN",
        )

    private val uber = descriptor(RidePlatform.UBER, listOf("aceptar", "rechazar"))
    private val didi = descriptor(RidePlatform.DIDI, listOf("aceptar didi", "solicitud didi"))

    private fun engine(vararg descriptors: PlatformDescriptor) =
        PlatformDetectionEngine(PlatformDescriptorRegistry(descriptors.toList()))

    @Test
    fun `packageName con match unico resuelve PACKAGE_MATCH`() {
        val result = engine(uber, didi).detect(texts = listOf("Aceptar"), timestampMillis = 1000L, packageName = "com.ubercab")

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
        assertEquals(ScreenType.REQUEST, result.screenDetection.type)
        assertEquals("com.ubercab", result.sourcePackage)
    }

    @Test
    fun `packageName normalizado con alias resuelve PACKAGE_MATCH`() {
        val result =
            engine(uber, didi).detect(
                texts = listOf("Aceptar"),
                timestampMillis = 1000L,
                packageName = " COM.UberCab ",
            )

        assertEquals(DetectionResolution.PACKAGE_MATCH, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
    }

    @Test
    fun `sin packageName y un unico candidato por keywords resuelve KEYWORD_CANDIDATE`() {
        val result = engine(uber, didi).detect(texts = listOf("Aceptar", "Rechazar"), timestampMillis = 1000L)

        assertEquals(DetectionResolution.KEYWORD_CANDIDATE, result.resolution)
        assertEquals(RidePlatform.UBER, result.descriptor?.platform)
        assertEquals(ScreenType.REQUEST, result.screenDetection.type)
        assertEquals(1, result.candidates.size)
    }

    @Test
    fun `empate de candidatos por keywords resuelve AMBIGUOUS sin elegir`() {
        val result = engine(uber, didi).detect(texts = listOf("Aceptar", "Rechazar"), timestampMillis = 1000L, packageName = "desconocido.pkg")

        assertEquals(DetectionResolution.AMBIGUOUS, result.resolution)
        assertNull(result.descriptor)
        assertEquals(2, result.candidates.size)
    }

    @Test
    fun `sin candidatos resuelve NONE`() {
        val result = engine(uber, didi).detect(texts = listOf("Hola mundo"), timestampMillis = 1000L)

        assertEquals(DetectionResolution.NONE, result.resolution)
        assertNull(result.descriptor)
        assertTrue(result.candidates.isEmpty())
    }

    @Test
    fun `textos vacios resuelven NONE`() {
        val result = engine(uber).detect(texts = emptyList(), timestampMillis = 1000L)

        assertEquals(DetectionResolution.NONE, result.resolution)
    }

    @Test
    fun `propaga el origin recibido`() {
        val result = engine(uber, didi).detect(texts = listOf("Aceptar"), timestampMillis = 1000L, packageName = "com.ubercab", origin = DetectionOrigin.OCR)

        assertEquals(DetectionOrigin.OCR, result.origin)
    }
}
```

**Nota sobre `empate de candidatos`:** `uber` tiene keywords `["aceptar","rechazar"]`; `didi` tiene `["aceptar didi","solicitud didi"]`. Con textos `["Aceptar","Rechazar"]`: el texto `"aceptar"` NO contiene `"aceptar didi"` (la keyword completa no aparece), por lo que `didi` queda con score 0 y type UNKNOWN → NO es candidato. Por eso, para forzar el empate, este test pasa `packageName = "desconocido.pkg"` y usa dos descriptores con LAS MISMAS keywords. Corregir `didi` en este test con keywords idénticas:

```kotlin
    @Test
    fun `empate de candidatos por keywords resuelve AMBIGUOUS sin elegir`() {
        val sameA = descriptor(RidePlatform.UBER, listOf("aceptar", "rechazar"))
        val sameB = descriptor(RidePlatform.DIDI, listOf("aceptar", "rechazar"))
        val result = engine(sameA, sameB).detect(texts = listOf("Aceptar", "Rechazar"), timestampMillis = 1000L)

        assertEquals(DetectionResolution.AMBIGUOUS, result.resolution)
        assertNull(result.descriptor)
        assertEquals(2, result.candidates.size)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :core:platform:test --console=plain`
Expected: FAIL (`PlatformDetectionEngine` sin resolver).

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.sirc.core.platform

/**
 * Motor genérico de detección de plataforma (WP-E3-02).
 *
 * Recorre los descriptores del registry (O(n)) en una sola pasada y resuelve
 * la plataforma por paquete o por keywords. No conoce el origen de los textos
 * (OCR, galería, tests): su contrato se limita a [texts], [timestampMillis] y
 * [packageName] opcional. No contiene lógica de parsing.
 */
class PlatformDetectionEngine(
    private val registry: PlatformDescriptorRegistry,
) {
    fun detect(
        texts: List<String>,
        timestampMillis: Long,
        packageName: String? = null,
        origin: DetectionOrigin = DetectionOrigin.UNKNOWN,
    ): DetectionResult {
        val normalized = texts.map(OfferDetectionEngine::normalize)

        // Etapa 1: paquete.
        if (packageName != null) {
            for (descriptor in registry.descriptors) {
                if (DetectionMatcher.matchesPackage(descriptor.packageNames, packageName)) {
                    val engine = registry.detectionEngineFor(descriptor.platform) ?: continue
                    return DetectionResult(
                        resolution = DetectionResolution.PACKAGE_MATCH,
                        origin = origin,
                        descriptor = descriptor,
                        screenDetection = engine.detect(texts),
                        sourcePackage = packageName,
                    )
                }
            }
        }

        // Etapa 2: keywords. Candidato válido = pantalla distinta de UNKNOWN.
        val candidates = mutableListOf<DetectionCandidate>()
        for (descriptor in registry.descriptors) {
            val engine = registry.detectionEngineFor(descriptor.platform) ?: continue
            val detection = engine.detect(texts)
            if (detection.type != ScreenType.UNKNOWN) {
                candidates +=
                    DetectionCandidate(
                        descriptor = descriptor,
                        screenDetection = detection,
                        matchScore = DetectionMatcher.matchScore(descriptor, normalized),
                    )
            }
        }

        if (candidates.isEmpty()) {
            return DetectionResult(resolution = DetectionResolution.NONE, origin = origin)
        }

        // Etapas 3-4: único candidato o empate por el mayor score.
        val topScore = candidates.maxOf { it.matchScore }
        val winners = candidates.filter { it.matchScore == topScore }
        return if (winners.size == 1) {
            DetectionResult(
                resolution = DetectionResolution.KEYWORD_CANDIDATE,
                origin = origin,
                descriptor = winners.single().descriptor,
                screenDetection = winners.single().screenDetection,
                candidates = candidates,
            )
        } else {
            DetectionResult(
                resolution = DetectionResolution.AMBIGUOUS,
                origin = origin,
                candidates = candidates,
            )
        }
    }
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat :core:platform:test --console=plain`
Expected: PASS.

- [ ] **Step 5: Run ktlint on the module**

Run: `.\gradlew.bat :core:platform:ktlintCheck --console=plain`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add core/platform/src/main/kotlin/com/sirc/core/platform/PlatformDetectionEngine.kt core/platform/src/test/kotlin/com/sirc/core/platform/PlatformDetectionEngineTest.kt
git commit -m "feat(platform): generic detection engine (WP-E3-02)"
```

---

### Task 5: `OfferParserOrchestrator` — overload por `packageName` + `parseWith` compartido

**Files:**
- Modify: `core/platform/src/main/kotlin/com/sirc/core/platform/OfferParserOrchestrator.kt`
- Test: `core/platform/src/test/kotlin/com/sirc/core/platform/OfferParserOrchestratorTest.kt`

**Interfaces:**
- Consumes: `PlatformDetectionEngine`, `DetectionResult` (Tasks 1-4).
- Produces: nuevo método público `parse(texts: List<String>, timestampMillis: Long, packageName: String): ParsedOffer`; método privado `parseWith(descriptor: PlatformDescriptor, screenDetection: ScreenDetection, texts: List<String>, timestampMillis: Long, parseStartNanos: Long, detectionMillis: Double): ParsedOffer`.

- [ ] **Step 1: Write the failing tests**

Añadir al final de `OfferParserOrchestratorTest.kt`:

```kotlin
    @Test
    fun `parse por packageName resuelve y extrae la oferta`() {
        val parsed =
            orchestrator().parse(
                texts = listOf("Nueva solicitud de viaje", "Aceptar", "Total $120", "8.5 km", "25 min"),
                timestampMillis = 1000L,
                packageName = "com.ubercab",
            )

        assertEquals(OfferType.UBER_REQUEST, parsed.type)
        assertNotNull(parsed.offer)
        assertEquals(120.0, parsed.offer?.estimatedTotal ?: 0.0, 0.001)
    }

    @Test
    fun `parse por packageName de plataforma no registrada devuelve none`() {
        val parsed =
            orchestrator().parse(
                texts = listOf("Nueva solicitud", "Total $120", "8.5 km"),
                timestampMillis = 1000L,
                packageName = "com.desconocido.app",
            )

        assertNull(parsed.offer)
    }

    @Test
    fun `parse por packageName con pantalla no request devuelve none`() {
        val parsed =
            orchestrator().parse(
                texts = listOf("Dónde quieres ir?", "Buscar", "Disponible"),
                timestampMillis = 1000L,
                packageName = "com.ubercab",
            )

        assertNull(parsed.offer)
    }
```

Nota: `parse por packageName de plataforma no registrada` — con `orchestrator()` (solo UBER registrado) y `com.desconocido.app`, la etapa 1 no matchea; la etapa 2 con textos de solicitud sí genera candidato(s) con el descriptor de UBER (único registrado) → `KEYWORD_CANDIDATE`, y como es REQUEST, **SÍ parsea**. Para que el test falle en "none" hay que usar textos que NO matcheen ninguna keyword del descriptor registrado:

```kotlin
    @Test
    fun `parse por packageName de plataforma no registrada devuelve none`() {
        val parsed =
            orchestrator().parse(
                texts = listOf("Texto irrelevante sin keywords"),
                timestampMillis = 1000L,
                packageName = "com.desconocido.app",
            )

        assertNull(parsed.offer)
    }
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat :core:platform:test --console=plain`
Expected: FAIL (método `parse(texts, ts, packageName)` sin resolver).

- [ ] **Step 3: Write the implementation**

Reemplazar el cuerpo de `OfferParserOrchestrator.kt` por:

```kotlin
package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform

/**
 * Orquestador de Parsing (O2).
 *
 * Decide qué variante de oferta aplicar al texto visible: primero resuelve el
 * descriptor de la plataforma, detecta si realmente hay una pantalla de oferta
 * ([OfferDetectionEngine] con las reglas del descriptor) y, si es así, prueba
 * las variantes del descriptor en orden de especificidad. Si ninguna matchea o
 * extrae, cae al extractor genérico de la plataforma.
 *
 * Es 100 % descriptor-driven: no contiene ninguna referencia a una plataforma
 * concreta. Resuelve la plataforma por [RidePlatform] (flujo actual) o por
 * packageName vía [PlatformDetectionEngine]. Es el único punto de entrada que
 * usa el pipeline de captura.
 */
class OfferParserOrchestrator(
    private val platformRegistry: PlatformDescriptorRegistry,
) {
    private val detectionEngine = PlatformDetectionEngine(platformRegistry)

    /**
     * Clasifica [texts] y extrae la oferta (flujo por plataforma conocida).
     *
     * @param platform plataforma ya conocida por el pipeline (package name).
     * @return [ParsedOffer] con el tipo detectado y la oferta extraída, o
     *   `offer = null` si la pantalla no era una solicitud, la plataforma no
     *   está registrada o no se pudo parsear.
     */
    fun parse(
        texts: List<String>,
        timestampMillis: Long,
        platform: RidePlatform,
    ): ParsedOffer {
        val parseStart = System.nanoTime()
        val descriptor = platformRegistry.descriptorFor(platform) ?: return ParsedOffer.none()
        val engine = platformRegistry.detectionEngineFor(platform) ?: return ParsedOffer.none()

        val detectionStart = System.nanoTime()
        val detection = engine.detect(texts)
        val detectionMillis = elapsedMillis(detectionStart)
        if (!detection.isRequest) {
            return ParsedOffer.none(detectionMillis = detectionMillis)
        }

        return parseWith(
            descriptor = descriptor,
            screenDetection = detection,
            texts = texts,
            timestampMillis = timestampMillis,
            parseStartNanos = parseStart,
            detectionMillis = detectionMillis,
        )
    }

    /**
     * Clasifica [texts] y extrae la oferta (flujo por packageName).
     *
     * Usa [PlatformDetectionEngine] para resolver la plataforma y la pantalla en
     * una sola pasada. Si la detección es ambigua o no encuentra plataforma,
     * devuelve `offer = null`.
     */
    fun parse(
        texts: List<String>,
        timestampMillis: Long,
        packageName: String,
    ): ParsedOffer {
        val parseStart = System.nanoTime()
        val result = detectionEngine.detect(texts, timestampMillis, packageName)
        if (!result.isRecognized || !result.screenDetection.isRequest) {
            return ParsedOffer.none()
        }
        val descriptor = result.descriptor ?: return ParsedOffer.none()
        return parseWith(
            descriptor = descriptor,
            screenDetection = result.screenDetection,
            texts = texts,
            timestampMillis = timestampMillis,
            parseStartNanos = parseStart,
            detectionMillis = 0.0,
        )
    }

    private fun parseWith(
        descriptor: PlatformDescriptor,
        screenDetection: ScreenDetection,
        texts: List<String>,
        timestampMillis: Long,
        parseStartNanos: Long,
        detectionMillis: Double,
    ): ParsedOffer {
        val normalized = texts.map(OfferDetectionEngine::normalize)
        for (parser in platformRegistry.variantParsersFor(descriptor.platform)) {
            if (parser.matches(normalized)) {
                val offer = parser.extract(texts, timestampMillis)
                if (offer != null) {
                    return ParsedOffer(
                        type = parser.type,
                        offer = offer,
                        detectionMillis = detectionMillis,
                        parsingMillis = elapsedMillis(parseStartNanos),
                    )
                }
            }
        }

        val generic = platformRegistry.extractorFor(descriptor.platform)?.extract(texts, timestampMillis)
        return ParsedOffer(
            type = OfferType.GENERIC,
            offer = generic,
            detectionMillis = detectionMillis,
            parsingMillis = elapsedMillis(parseStartNanos),
        )
    }

    private fun elapsedMillis(startNanos: Long): Double = (System.nanoTime() - startNanos) / NANOS_PER_MILLI

    companion object {
        private const val NANOS_PER_MILLI = 1_000_000.0
    }
}
```

Nota: `screenDetection` se recibe en `parseWith` para futura instrumentación (Debug); la firma lo expone sin que se use en el cuerpo, coherente con la spec.

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat :core:platform:test --console=plain`
Expected: PASS (todos: existentes + 3 nuevos).

- [ ] **Step 5: Run ktlint on the module**

Run: `.\gradlew.bat :core:platform:ktlintCheck --console=plain`
Expected: PASS.

- [ ] **Step 6: Run full module verification**

Run: `.\gradlew.bat :core:platform:test :core:platform:ktlintCheck --console=plain`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 7: Commit**

```bash
git add core/platform/src/main/kotlin/com/sirc/core/platform/OfferParserOrchestrator.kt core/platform/src/test/kotlin/com/sirc/core/platform/OfferParserOrchestratorTest.kt
git commit -m "feat(platform): orchestrator packageName overload via detection engine (WP-E3-02)"
```

---

### Task 6: Documentación y verificación completa

**Files:**
- Modify: `docs/CHANGELOG.md` (entrada WP-E3-02)
- Modify: `.ai/CONTEXT.md` (nota SPRINT 11 WP-E3-02)
- Modify: `.ai/DECISIONS.md` (decisión D11.12)

**Interfaces:** ninguna (documentación).

- [ ] **Step 1: Documentar en CHANGELOG.md**

Añadir un bullet en la sección `## [v1.0.0-rc1] — 2026-08-01` > primer `### Añadido` (junto a WP-E3-01):

```markdown
- **Framework Genérico de Detección** (WP-E3-02): en `:core:platform`,
  `PlatformDetectionEngine` (servicio independiente que consume
  `PlatformDescriptorRegistry`) resuelve la plataforma de forma descriptor-driven:
  por coincidencia exacta de packageName (`PACKAGE_MATCH`), por keywords de
  detección (`KEYWORD_CANDIDATE`), `AMBIGUOUS` ante empate y `NONE` sin
  candidatos, en una sola pasada (O(n)). `DetectionMatcher` es una función pura
  sin estado; `DetectionResult` es inmutable, autocontenido y expone
  diagnóstico (`candidates`, `sourcePackage`, `origin` con
  `DetectionOrigin`: PACKAGE/OCR/GALLERY/TEST/UNKNOWN). El registry expone una
  vista de solo lectura de descriptores. `OfferParserOrchestrator` gana un
  overload `parse(texts, ts, packageName)` 100 % backward compatible (el método
  por `RidePlatform` se conserva intacto). Sin cambios funcionales para el
  conductor; sin plataformas nuevas.
```

- [ ] **Step 2: Documentar en .ai/CONTEXT.md**

En el bloque de notas SPRINT 11 (tras la nota WP-E3-01), añadir:

```markdown
> **WP-E3-02 (SPRINT 11)**: framework genérico de detección. `PlatformDetectionEngine`
> recorre los descriptores del registry (O(n), una sola pasada) y resuelve la
> plataforma por packageName (`PACKAGE_MATCH`) o por keywords (`KEYWORD_CANDIDATE`;
> `AMBIGUOUS` ante empate; `NONE` sin candidatos). `DetectionMatcher` es puro y sin
> estado; `DetectionResult` encapsula descriptor, `ScreenDetection`, `origin`
> (`DetectionOrigin`) y diagnóstico (`candidates`, `sourcePackage`).
> `OfferParserOrchestrator` añade `parse(texts, ts, packageName)` sin romper el
> método por `RidePlatform`. El registry solo expone una vista de solo lectura.
> Sin cambios de comportamiento; `:core:platform` sigue Kotlin puro.
```

Y añadir un bullet en `## Estado del proyecto`:

```markdown
- **SPRINT 11 WP-E3-02 completado**: Framework Genérico de Detección
  descriptor-driven en `:core:platform`. `PlatformDetectionEngine` +
  `DetectionMatcher` + `DetectionResult`; overload `parse(packageName)` en el
  orquestador; vista de solo lectura en el registry. Sin plataformas nuevas ni
  cambios de comportamiento.
```

- [ ] **Step 3: Documentar en .ai/DECISIONS.md**

Añadir `### D11.12 — Framework Genérico de Detección descriptor-driven (WP-E3-02)` tras `D11.11`:

```markdown
### D11.12 — Framework Genérico de Detección descriptor-driven (WP-E3-02)

**Contexto:** la resolución de plataforma dependía de `RidePlatform.fromPackageName`
(mapeo del enum) y la detección de pantalla vivía dentro de `OfferParserOrchestrator`;
WP-E3-02 busca consolidar el framework genérico de detección ya iniciado en WP-E3-01.

**Decisión:** `PlatformDetectionEngine` (servicio independiente que consume el
`PlatformDescriptorRegistry`) ejecuta la estrategia por etapas en una sola pasada
(O(n) descriptores): 1) `PACKAGE_MATCH` por coincidencia de packageName normalizado;
2) candidatos por keywords de detección (pantalla ≠ UNKNOWN); 3) único candidato →
`KEYWORD_CANDIDATE`; 4) empate por mayor `matchScore` → `AMBIGUOUS` (sin elegir);
5) sin candidatos → `NONE`. `DetectionMatcher` es una función pura sin estado;
`DetectionResult` es inmutable y autocontenido (descriptor, `ScreenDetection`,
`origin` con `DetectionOrigin`, diagnóstico). El registry solo expone una
`Collection<PlatformDescriptor>` de solo lectura. `OfferParserOrchestrator` añade
un overload `parse(texts, ts, packageName)` sin tocar el método por `RidePlatform`.

**Alternativas descartadas:** matchers componibles (sobreingeniería); scoring
heurístico con umbrales (rompe determinismo); callbacks de eventos dentro del
motor (rompe pureza de `:core:platform`).

**Consecuencias:** agregar una plataforma = datos en su descriptor; sin ramas por
plataforma; detección determinista y testeable; el motor no conoce el origen de
los textos (`DetectionOrigin` deja preparado el framework para galería/laboratorios).
```

- [ ] **Step 4: Verificación completa**

Run: `.\gradlew.bat :core:platform:test :core:capture:test :domain:test :feature:overlay:testDebugUnitTest testDebugUnitTest lintDebug assembleDebug --console=plain`
Expected: BUILD SUCCESSFUL.

Run: `.\gradlew.bat ktlintCheck --console=plain`
Expected: FAIL solo por violaciones preexistentes documentadas (`feature:overlay/OverlayService.kt` commit `6d62dba` y `core/capture/android/.../ProjectionLifecycleTest.kt` commit `be66f49`). Documentar este resultado; no modificar esos archivos.

- [ ] **Step 5: Commit**

```bash
git add docs/CHANGELOG.md .ai/CONTEXT.md .ai/DECISIONS.md
git commit -m "docs: WP-E3-02 generic detection framework"
```

---

### Task 7: Commit final del WP y cierre

- [ ] **Step 1: Revisar estado del repo**

Run: `git status --short`
Verificar que solo quedan cambios intencionados (los archivos de `data/` y `domain/` ajenos al WP deben permanecer sin stage).

- [ ] **Step 2: Obtener hash del commit final del framework**

Run: `git log --oneline -6`
Identificar el último commit de código del WP-E3-02 (Task 5) y anotar su hash para el informe.

- [ ] **Step 3: Actualizar TASK.md**

Marcar WP-E3-02 como completado y documentar estado final, archivos y resultado de verificación.

- [ ] **Step 4: Entregar informe**

Responder con las 11 secciones solicitadas por el WP (Resumen técnico, Impacto arquitectónico ANTES→DESPUÉS, Diagrama final, Archivos creados/modificados, Archivos eliminados, Resultado de validaciones, Deuda técnica, Respuestas, Estado, Commit, Hash).

package com.sirc.core.platform

import com.sirc.domain.model.RidePlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionResultTest {
    private fun result(resolution: DetectionResolution): DetectionResult =
        DetectionResult(
            resolution = resolution,
            origin = CaptureInputType.PACKAGE,
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
        val r = DetectionResult(resolution = DetectionResolution.NONE, origin = CaptureInputType.UNKNOWN)

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

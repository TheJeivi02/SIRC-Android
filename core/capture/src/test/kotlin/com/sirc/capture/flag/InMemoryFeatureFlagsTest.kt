package com.sirc.capture.flag

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InMemoryFeatureFlagsTest {
    private val flags = InMemoryFeatureFlags()

    @Test
    fun `todos los flags habilitados por defecto`() {
        FeatureFlag.entries.forEach { flag ->
            assertTrue(flags.isEnabled(flag))
        }
    }

    @Test
    fun `configura un flag a su valor`() {
        flags.setEnabled(FeatureFlag.CAPTURE, false)

        assertFalse(flags.isEnabled(FeatureFlag.CAPTURE))
        assertTrue(flags.isEnabled(FeatureFlag.PARSER))
    }

    @Test
    fun `restablece un flag al default`() {
        flags.setEnabled(FeatureFlag.DEBUG_PANEL, false)
        assertEquals(false, flags.isEnabled(FeatureFlag.DEBUG_PANEL))
    }
}

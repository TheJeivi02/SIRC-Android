package com.sirc.capture.android.provider

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProjectionLifecycleTest {

    @Test
    fun initialStateIsIdle() {
        val lifecycle = ProjectionLifecycle()
        assertEquals(ProjectionLifecycle.State.IDLE, lifecycle.currentState)
        assertFalse(lifecycle.isActive)
    }

    @Test
    fun beginReturnsTokenAndStateBecomesInitializing() {
        val lifecycle = ProjectionLifecycle()
        val token = lifecycle.begin()
        assertEquals(ProjectionLifecycle.State.INITIALIZING, lifecycle.currentState)
        assertFalse(lifecycle.isActive)
        assertTrue(lifecycle.isCurrent(token))
    }

    @Test
    fun activateWithCurrentTokenBecomesActive() {
        val lifecycle = ProjectionLifecycle()
        val token = lifecycle.begin()
        val activated = lifecycle.activate(token)
        assertTrue(activated)
        assertEquals(ProjectionLifecycle.State.ACTIVE, lifecycle.currentState)
        assertTrue(lifecycle.isActive)
    }

    @Test
    fun activateWithStaleTokenIsIgnored() {
        val lifecycle = ProjectionLifecycle()
        val token1 = lifecycle.begin()
        val token2 = lifecycle.begin() // supersedes token1

        val activated1 = lifecycle.activate(token1)
        assertFalse(activated1)
        assertEquals(ProjectionLifecycle.State.INITIALIZING, lifecycle.currentState)
        assertFalse(lifecycle.isActive)

        val activated2 = lifecycle.activate(token2)
        assertTrue(activated2)
        assertEquals(ProjectionLifecycle.State.ACTIVE, lifecycle.currentState)
        assertTrue(lifecycle.isActive)
    }

    @Test
    fun stopReturnsToIdleAndInvalidatesToken() {
        val lifecycle = ProjectionLifecycle()
        val token = lifecycle.begin()
        lifecycle.activate(token)

        lifecycle.stop()
        assertEquals(ProjectionLifecycle.State.IDLE, lifecycle.currentState)
        assertFalse(lifecycle.isActive)
        assertFalse(lifecycle.isCurrent(token))
    }

    @Test
    fun stopIsIdempotent() {
        val lifecycle = ProjectionLifecycle()
        lifecycle.stop()
        lifecycle.stop()
        assertEquals(ProjectionLifecycle.State.IDLE, lifecycle.currentState)
        assertFalse(lifecycle.isActive)
    }

    @Test
    fun abortReturnsToIdleFromInitializing() {
        val lifecycle = ProjectionLifecycle()
        val token = lifecycle.begin()
        lifecycle.abort(token)
        assertEquals(ProjectionLifecycle.State.IDLE, lifecycle.currentState)
        assertFalse(lifecycle.isActive)
        assertFalse(lifecycle.isCurrent(token))
    }
}

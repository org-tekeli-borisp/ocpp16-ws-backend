package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.LastSeenThrottle

class LastSeenThrottleTest {

    @Test
    fun `first call returns true`() {
        var now = 1_000L
        val throttle = LastSeenThrottle(30_000) { now }

        assertTrue(throttle.shouldTouch())
    }

    @Test
    fun `immediate second call returns false`() {
        var now = 1_000L
        val throttle = LastSeenThrottle(30_000) { now }

        assertTrue(throttle.shouldTouch())
        assertFalse(throttle.shouldTouch())
    }

    @Test
    fun `call just before interval returns false`() {
        var now = 1_000L
        val throttle = LastSeenThrottle(30_000) { now }

        assertTrue(throttle.shouldTouch())
        now += 29_999
        assertFalse(throttle.shouldTouch())
    }

    @Test
    fun `call after interval elapsed returns true again`() {
        var now = 1_000L
        val throttle = LastSeenThrottle(30_000) { now }

        assertTrue(throttle.shouldTouch())
        now += 30_000
        assertTrue(throttle.shouldTouch())
    }

    @Test
    fun `touch after interval resets the window`() {
        var now = 1_000L
        val throttle = LastSeenThrottle(30_000) { now }

        assertTrue(throttle.shouldTouch())
        now += 30_000
        assertTrue(throttle.shouldTouch())
        now += 29_999
        assertFalse(throttle.shouldTouch())
    }

    @Test
    fun `default interval is 30 seconds`() {
        var now = 0L
        val throttle = LastSeenThrottle { now }

        assertTrue(throttle.shouldTouch())
        now += 29_999
        assertFalse(throttle.shouldTouch())
        now += 1
        assertTrue(throttle.shouldTouch())
    }
}

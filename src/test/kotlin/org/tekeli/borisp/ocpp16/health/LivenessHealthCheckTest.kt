package org.tekeli.borisp.ocpp16.health

import org.eclipse.microprofile.health.HealthCheckResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class LivenessHealthCheckTest {

    @Test
    fun `call returns UP response`() {
        val check = LivenessHealthCheck()

        val response = check.call()

        assertEquals("liveness", response.name)
        assertEquals(HealthCheckResponse.Status.UP, response.status)
    }
}

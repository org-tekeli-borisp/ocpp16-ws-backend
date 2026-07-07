package org.tekeli.borisp.ocpp16.health

import io.quarkus.test.junit.QuarkusTest
import jakarta.enterprise.inject.Any
import jakarta.inject.Inject
import org.eclipse.microprofile.health.HealthCheckResponse
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class ReadinessHealthCheckTest {

    @Inject
    @Any
    lateinit var check: ReadinessHealthCheck

    @Test
    fun `call returns UP when database is reachable`() {
        val response = check.call()

        assertEquals("database", response.name)
        assertEquals(HealthCheckResponse.Status.UP, response.status)
    }
}

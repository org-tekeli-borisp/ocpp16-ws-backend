package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException

class SecurityEventNotificationHandlerBranchTest {

    private val handler = SecurityEventNotificationHandler()

    @Test
    fun `validatePayload with valid security event`() {
        val payload = mapOf(
            "type" to "LocalAccess",
            "timestamp" to "2024-01-01T00:00:00Z",
            "techInfo" to "some info"
        )

        val result = handler.validatePayload(payload)

        assertEquals("LocalAccess", result.type)
        assertEquals("some info", result.techInfo)
    }

    @Test
    fun `validatePayload with missing techInfo`() {
        val payload = mapOf(
            "type" to "Reset",
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val result = handler.validatePayload(payload)

        assertNull(result.techInfo)
    }
}

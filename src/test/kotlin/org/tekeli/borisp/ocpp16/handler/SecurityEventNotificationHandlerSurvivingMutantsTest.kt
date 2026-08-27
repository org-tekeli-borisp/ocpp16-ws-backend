package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.persistence.SecurityLog
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.time.Instant

class SecurityEventNotificationHandlerSurvivingMutantsTest {

    private val handler = SecurityEventNotificationHandler()

    private fun makeCall(id: String, payload: Map<String, Any>?) =
        OcppMessage.Call(id, "SecurityEventNotification", payload)

    private fun serverWithCapturedPersistence(captured: MutableList<String?>): OcppWebSocketServer {
        val ps = object : PersistenceService() {
            override fun createSecurityLog(cp: String, type: String, ts: Instant, info: String?): SecurityLog {
                captured.add(info)
                return SecurityLog(chargePointId = cp, type = type, timestamp = ts, techInfo = info)
            }
        }
        return OcppWebSocketServer().apply {
            chargePointId = "CP-001"
            persistenceService = ps
        }
    }

    @Test
    fun `handle persists techInfo from payload`() {
        val captured = mutableListOf<String?>()
        val server = serverWithCapturedPersistence(captured)
        val call = makeCall("se-mut-1", mapOf(
            "type" to "Tampering",
            "timestamp" to "2024-01-01T00:00:00Z",
            "techInfo" to "cable cut detected"
        ))

        handler.handle(call, server)

        assertEquals(listOf<String?>("cable cut detected"), captured)
    }

    @Test
    fun `handle completes without persistenceService`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-001"
            persistenceService = null
            metricsService = null
        }
        val call = makeCall("se-mut-2", mapOf(
            "type" to "Reset",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        var response: String? = null
        assertDoesNotThrow {
            response = handler.handle(call, server)
        }

        assertTrue(response!!.startsWith("[3,"))
        assertTrue(response!!.contains("se-mut-2"))
    }

    @Test
    fun `validatePayload accepts type at exactly max length and fails enum check instead`() {
        val exactLengthType = "A".repeat(OcppConstants.MAX_EVENT_TYPE_LENGTH)
        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(mapOf(
                "type" to exactLengthType,
                "timestamp" to "2024-01-01T00:00:00Z"
            ))
        }
        assertEquals("Invalid type: $exactLengthType", ex.message)
    }

    @Test
    fun `validatePayload rejects over-long type with length message before enum check`() {
        val longType = "A".repeat(OcppConstants.MAX_EVENT_TYPE_LENGTH + 1)
        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(mapOf(
                "type" to longType,
                "timestamp" to "2024-01-01T00:00:00Z"
            ))
        }
        assertTrue(ex.message!!.contains("type must not exceed ${OcppConstants.MAX_EVENT_TYPE_LENGTH} characters"))
    }
}

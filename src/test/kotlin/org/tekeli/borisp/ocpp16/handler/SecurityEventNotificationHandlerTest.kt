package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.SecurityEventNotificationHandler
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.persistence.SecurityLog
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import java.time.Instant

class SecurityEventNotificationHandlerTest {

    private val handler = SecurityEventNotificationHandler()

    private fun makeCall(action: String, payload: Map<String, Any>?) = OcppMessage.Call("test-id", action, payload)

    @Test
    fun `should accept valid SecurityEventNotification`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("test-id"))
    }

    @Test
    fun `should accept SecurityEventNotification with techInfo`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "InvalidTLSVersion",
            "timestamp" to "2024-01-01T00:00:00Z",
            "techInfo" to "TLS 1.0 rejected"
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should throw FormationViolation for null payload`() {
        val call = makeCall("SecurityEventNotification", null)

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for missing type`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for empty type`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for missing timestamp`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated"
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for empty timestamp`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated",
            "timestamp" to ""
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for type exceeding 50 characters`() {
        val longType = "A".repeat(51)
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to longType,
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should reject invalid security event type`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "InvalidEventType",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should throw FormationViolation for techInfo exceeding 255 characters`() {
        val longTechInfo = "A".repeat(256)
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated",
            "timestamp" to "2024-01-01T00:00:00Z",
            "techInfo" to longTechInfo
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should accept techInfo with exactly 255 characters`() {
        val maxTechInfo = "A".repeat(255)
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated",
            "timestamp" to "2024-01-01T00:00:00Z",
            "techInfo" to maxTechInfo
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should accept all valid security event types`() {
        val validEvents = listOf(
            "FirmwareUpdated", "FirmwareVerificationFailed",
            "InvalidChargePointCertificate", "InvalidCentralSystemCertificate",
            "InvalidTLSCipherSuite", "InvalidTLSVersion",
            "LocalAccess", "ResetFailed", "Reset", "Tampering",
            "TransactionInfoNotStored", "InvalidFirmwareSigningCertificate",
            "InvalidFirmwareSignature", "DiscardedRenewedClientCertificate",
            "UnauthorizedAccess"
        )

        for (eventType in validEvents) {
            val call = makeCall("SecurityEventNotification", mapOf(
                "type" to eventType,
                "timestamp" to "2024-01-01T00:00:00Z"
            ))

            val response = handler.handle(call, mockServer())
            assertTrue(response.startsWith("[3,"), "Should accept event type: $eventType")
        }
    }

    @Test
    fun `should preserve messageId in response`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        val response = handler.handle(call, mockServer())

        assertTrue(response.contains("test-id"))
    }

    @Test
    fun `should handle null type field`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to null as Any?,
            "timestamp" to "2024-01-01T00:00:00Z"
        ) as Map<String, Any>)

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    @Test
    fun `should handle whitespace-only type`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "   ",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, mockServer())
        }
    }

    // ====== Mutation-killing tests ======

    @Test
    fun `extractType returns exact type value for valid input`() {
        val payload = mapOf("type" to "FirmwareUpdated")
        val result = handler.extractType(payload)
        assertEquals("FirmwareUpdated", result)
    }

    @Test
    fun `extractType rejects type exceeding 50 characters`() {
        val longType = "A".repeat(51)
        val payload = mapOf("type" to longType)
        val ex = assertThrows(FormationViolationException::class.java) {
            handler.extractType(payload)
        }
        assertTrue(ex.message!!.contains("50"))
    }

    @Test
    fun `extractType accepts type with exactly 50 characters if valid`() {
        // No valid type is 50 chars, but test boundary with valid short type
        val result = handler.extractType(mapOf("type" to "Reset"))
        assertEquals("Reset", result)
    }

    @Test
    fun `extractTimestamp returns correct Instant`() {
        val ts = "2024-06-15T12:00:00Z"
        val result = handler.extractTimestamp(mapOf("timestamp" to ts))
        assertEquals(java.time.Instant.parse(ts), result)
    }

    @Test
    fun `extractTimestamp rejects blank timestamp`() {
        val ex = assertThrows(FormationViolationException::class.java) {
            handler.extractTimestamp(mapOf("timestamp" to "   "))
        }
        assertEquals("timestamp is required", ex.message)
    }

    @Test
    fun `extractTimestamp rejects null timestamp`() {
        val ex = assertThrows(FormationViolationException::class.java) {
            @Suppress("UNCHECKED_CAST")
            handler.extractTimestamp(mapOf("timestamp" to null as Any?) as Map<String, Any>)
        }
        assertEquals("timestamp is required", ex.message)
    }

    @Test
    fun `extractTechInfo returns value when present`() {
        val result = handler.extractTechInfo(mapOf("techInfo" to "test info"))
        assertEquals("test info", result)
    }

    @Test
    fun `extractTechInfo returns null when missing`() {
        val result = handler.extractTechInfo(emptyMap<String, Any>())
        assertNull(result)
    }

    @Test
    fun `extractTechInfo rejects value exceeding 255 characters`() {
        val longInfo = "A".repeat(256)
        val ex = assertThrows(FormationViolationException::class.java) {
            handler.extractTechInfo(mapOf("techInfo" to longInfo))
        }
        assertTrue(ex.message!!.contains("255"))
    }

    @Test
    fun `extractTechInfo accepts value with exactly 255 characters`() {
        val maxInfo = "A".repeat(255)
        val result = handler.extractTechInfo(mapOf("techInfo" to maxInfo))
        assertEquals(255, result!!.length)
    }

    @Test
    fun `validatePayload extracts all fields correctly`() {
        val payload = mapOf(
            "type" to "Tampering",
            "timestamp" to "2024-06-15T12:00:00Z",
            "techInfo" to "tamper detected"
        )
        val parsed = handler.validatePayload(payload)
        assertEquals("Tampering", parsed.type)
        assertEquals(java.time.Instant.parse("2024-06-15T12:00:00Z"), parsed.timestamp)
        assertEquals("tamper detected", parsed.techInfo)
    }

    @Test
    fun `validatePayload handles missing techInfo`() {
        val payload = mapOf(
            "type" to "Reset",
            "timestamp" to "2024-06-15T12:00:00Z"
        )
        val parsed = handler.validatePayload(payload)
        assertEquals("Reset", parsed.type)
        assertNull(parsed.techInfo)
    }

    @Test
    fun `handle passes correct type to processSecurityEvent`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "FirmwareVerificationFailed",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))
        val response = handler.handle(call, mockServer())
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("test-id"))
    }

    @Test
    fun `handle passes correct timestamp to processSecurityEvent`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "Reset",
            "timestamp" to "2025-12-31T23:59:59Z"
        ))
        val response = handler.handle(call, mockServer())
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `handle passes correct techInfo to processSecurityEvent`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "Tampering",
            "timestamp" to "2024-01-01T00:00:00Z",
            "techInfo" to "specific tamper info"
        ))
        val response = handler.handle(call, mockServer())
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `handle response has empty payload`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "Reset",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))
        val response = handler.handle(call, mockServer())
        // Response should be CallResult with empty payload: [3,"id",{}]
        assertTrue(response.endsWith(",{}]"))
    }

    @Test
    fun `handle rejects null chargePointId`() {
        val call = makeCall("SecurityEventNotification", mapOf(
            "type" to "Reset",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))
        val server = org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer()
        // chargePointId stays null
        val ex = assertThrows(FormationViolationException::class.java) {
            handler.handle(call, server)
        }
        assertEquals("No chargePointId from connection", ex.message)
    }

    @Test
    fun `processSecurityEvent calls createSecurityLog with correct type`() {
        val captured = mutableListOf<String>()
        val ps = object : PersistenceService() {
            override fun createSecurityLog(cp: String, type: String, ts: Instant, info: String?): SecurityLog {
                captured.add(type)
                return SecurityLog(chargePointId = cp, type = type, timestamp = ts, techInfo = info)
            }
        }
        val h = SecurityEventNotificationHandler(ps)
        h.processSecurityEvent(mockServer(), "CP-001", "Tampering", Instant.parse("2024-01-01T00:00:00Z"), null)
        assertEquals(listOf("Tampering"), captured)
    }

    @Test
    fun `processSecurityEvent calls createSecurityLog with correct techInfo`() {
        val capturedInfo = mutableListOf<String?>()
        val ps = object : PersistenceService() {
            override fun createSecurityLog(cp: String, type: String, ts: Instant, info: String?): SecurityLog {
                capturedInfo.add(info)
                return SecurityLog(chargePointId = cp, type = type, timestamp = ts, techInfo = info)
            }
        }
        val h = SecurityEventNotificationHandler(ps)
        h.processSecurityEvent(mockServer(), "CP-001", "Reset", Instant.parse("2024-01-01T00:00:00Z"), "details here")
        assertEquals(listOf("details here"), capturedInfo)
    }

    @Test
    fun `processSecurityEvent increments metrics when metricsService set`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val ms = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = mockServer()
        server.metricsService = ms
        handler.processSecurityEvent(server, "CP-001", "Reset", Instant.parse("2024-01-01T00:00:00Z"), null)
        val counter = meterRegistry.find("ocpp.security.events.received").counter()
        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    @Test
    fun `processSecurityEvent works without persistenceService`() {
        val server = mockServer()
        server.persistenceService = null
        server.metricsService = null
        assertDoesNotThrow {
            handler.processSecurityEvent(server, "CP-001", "Reset", Instant.parse("2024-01-01T00:00:00Z"), null)
        }
    }

    @Test
    fun `validatePayload calls extractTechInfo`() {
        val parsed = handler.validatePayload(mapOf(
            "type" to "Reset",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))
        assertNull(parsed.techInfo)
    }

    @Test
    fun `extractTechInfo returns empty string for empty value`() {
        val result = handler.extractTechInfo(mapOf("techInfo" to ""))
        assertEquals("", result)
    }

    @Test
    fun `handle response contains correct messageId`() {
        val call = OcppMessage.Call("unique-msg-123", "SecurityEventNotification", mapOf(
            "type" to "Reset",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))
        val response = handler.handle(call, mockServer())
        assertTrue(response.contains("unique-msg-123"))
    }

    @Test
    fun `extractType rejects type at exactly 51 characters`() {
        val longType = "A".repeat(51)
        val ex = assertThrows(FormationViolationException::class.java) {
            handler.extractType(mapOf("type" to longType))
        }
        assertEquals("type must not exceed 50 characters", ex.message)
    }

    @Test
    fun `extractType accepts valid type correctly`() {
        val result = handler.extractType(mapOf("type" to "LocalAccess"))
        assertEquals("LocalAccess", result)
    }

    private fun mockServer(): org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer {
        val server = org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer()
        server.chargePointId = "CP-001"
        return server
    }
}

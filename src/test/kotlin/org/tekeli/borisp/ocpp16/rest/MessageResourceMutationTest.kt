package org.tekeli.borisp.ocpp16.rest

import jakarta.ws.rs.NotFoundException
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.tekeli.borisp.ocpp16.persistence.ChargePoint
import org.tekeli.borisp.ocpp16.persistence.OcppMessageLog
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.MessageCaptureService
import org.tekeli.borisp.ocpp16.protocol.OcppMessageDto
import java.time.Instant

class MessageResourceMutationTest {

    private lateinit var captureService: MessageCaptureService
    private lateinit var persistence: PersistenceService
    private lateinit var resource: MessageResource

    @BeforeEach
    fun setup() {
        captureService = mock(MessageCaptureService::class.java)
        persistence = mock(PersistenceService::class.java)
        resource = MessageResource()
        resource.messageCaptureService = captureService
        resource.persistenceService = persistence
    }

    private fun dto(direction: String, action: String?, timestamp: String) = OcppMessageDto(
        chargePointId = "CP-001",
        direction = direction,
        messageType = "CALL",
        action = action,
        messageId = "m-$timestamp",
        payload = null,
        timestamp = timestamp
    )

    private fun log(messageId: String, action: String, timestamp: Instant, payload: String? = "{}") = OcppMessageLog(
        chargePointId = "CP-001",
        direction = "INBOUND",
        messageType = "CALL",
        action = action,
        messageId = messageId,
        payload = payload,
        timestamp = timestamp
    )

    private fun existingCp() {
        `when`(persistence.findChargePointById("CP-001")).thenReturn(mock(ChargePoint::class.java))
    }

    @Test
    fun `getMessages throws when persistenceService is not injected`() {
        val bare = MessageResource()

        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            bare.getMessages("CP-001")
        }
    }

    @Test
    fun `getMessages throws when messageCaptureService is not injected`() {
        val bare = MessageResource()
        bare.persistenceService = persistence
        `when`(persistence.findChargePointById("CP-001")).thenReturn(mock(ChargePoint::class.java))

        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            bare.getMessages("CP-001")
        }
    }

    @Test
    fun `getMessages returns all buffered messages when no filters are set`() {
        existingCp()
        val d1 = dto("INBOUND", "Heartbeat", "2024-01-01T00:00:00Z")
        val d2 = dto("OUTBOUND", "BootNotification", "2024-02-01T00:00:00Z")
        `when`(captureService.getMessages("CP-001")).thenReturn(listOf(d1, d2))

        val result = resource.getMessages("CP-001")

        assertEquals(listOf(d1, d2), result)
    }

    @Test
    fun `getMessages filters by direction`() {
        existingCp()
        val d1 = dto("INBOUND", "Heartbeat", "2024-01-01T00:00:00Z")
        val d2 = dto("OUTBOUND", "BootNotification", "2024-02-01T00:00:00Z")
        `when`(captureService.getMessages("CP-001")).thenReturn(listOf(d1, d2))

        val result = resource.getMessages("CP-001", direction = "INBOUND")

        assertEquals(listOf(d1), result)
    }

    @Test
    fun `getMessages filters by action`() {
        existingCp()
        val d1 = dto("INBOUND", "Heartbeat", "2024-01-01T00:00:00Z")
        val d2 = dto("INBOUND", "BootNotification", "2024-02-01T00:00:00Z")
        `when`(captureService.getMessages("CP-001")).thenReturn(listOf(d1, d2))

        val result = resource.getMessages("CP-001", action = "Heartbeat")

        assertEquals(listOf(d1), result)
    }

    @Test
    fun `getMessages filters by since timestamp keeping the boundary`() {
        existingCp()
        val old = dto("INBOUND", "Heartbeat", "2024-01-01T00:00:00Z")
        val boundary = dto("INBOUND", "Heartbeat", "2024-03-01T00:00:00Z")
        val recent = dto("INBOUND", "Heartbeat", "2024-06-01T00:00:00Z")
        `when`(captureService.getMessages("CP-001")).thenReturn(listOf(old, boundary, recent))

        val result = resource.getMessages("CP-001", since = "2024-03-01T00:00:00Z")

        assertEquals(listOf(boundary, recent), result)
    }

    @Test
    fun `getMessages treats a blank direction as no filter`() {
        existingCp()
        val d1 = dto("INBOUND", "Heartbeat", "2024-01-01T00:00:00Z")
        val d2 = dto("OUTBOUND", "BootNotification", "2024-02-01T00:00:00Z")
        `when`(captureService.getMessages("CP-001")).thenReturn(listOf(d1, d2))

        val result = resource.getMessages("CP-001", direction = "   ")

        assertEquals(listOf(d1, d2), result)
    }

    @Test
    fun `getMessages treats a blank action as no filter`() {
        existingCp()
        val d1 = dto("INBOUND", "Heartbeat", "2024-01-01T00:00:00Z")
        val d2 = dto("INBOUND", "BootNotification", "2024-02-01T00:00:00Z")
        `when`(captureService.getMessages("CP-001")).thenReturn(listOf(d1, d2))

        val result = resource.getMessages("CP-001", action = "   ")

        assertEquals(listOf(d1, d2), result)
    }

    @Test
    fun `getMessages keeps the last N messages when limit is positive`() {
        existingCp()
        val dtos = (1..5).map { dto("INBOUND", "Heartbeat", "2024-01-0${it}T00:00:00Z") }
        `when`(captureService.getMessages("CP-001")).thenReturn(dtos)

        val result = resource.getMessages("CP-001", limit = 3)

        assertEquals(listOf(dtos[2], dtos[3], dtos[4]), result)
    }

    @Test
    fun `getMessages returns all messages when limit is zero`() {
        existingCp()
        val dtos = (1..3).map { dto("INBOUND", "Heartbeat", "2024-01-0${it}T00:00:00Z") }
        `when`(captureService.getMessages("CP-001")).thenReturn(dtos)

        val result = resource.getMessages("CP-001", limit = 0)

        assertEquals(dtos, result)
    }

    @Test
    fun `getMessages returns 404 for an unknown charge point`() {
        `when`(persistence.findChargePointById("nope")).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            resource.getMessages("nope")
        }
    }

    @Test
    fun `getHistory returns the requested window with totals`() {
        existingCp()
        val logs = (1..5).map { log("m$it", "Heartbeat", Instant.parse("2024-01-0${it}T00:00:00Z")) }
        `when`(persistence.findMessageLogs("CP-001", null, null, 6)).thenReturn(logs)

        val result = resource.getHistory("CP-001", limit = 2, offset = 1)

        assertEquals(5, result["total"])
        assertEquals(1, result["offset"])
        assertEquals(2, result["limit"])
        val messages = result["messages"] as List<OcppMessageDto>
        assertEquals(listOf("m2", "m3"), messages.map { it.messageId })
        val first = messages[0]
        assertEquals("CP-001", first.chargePointId)
        assertEquals("INBOUND", first.direction)
        assertEquals("CALL", first.messageType)
        assertEquals("Heartbeat", first.action)
        assertEquals("m2", first.messageId)
        assertEquals("{}", first.payload)
        assertEquals("2024-01-02T00:00:00Z", first.timestamp)
    }

    @Test
    fun `getHistory fetches twice the requested window`() {
        existingCp()
        `when`(persistence.findMessageLogs("CP-001", null, null, 30)).thenReturn(emptyList())

        resource.getHistory("CP-001", limit = 10, offset = 5)

        verify(persistence).findMessageLogs("CP-001", null, null, 30)
    }

    @Test
    fun `getHistory maps a blank action to null`() {
        existingCp()
        val withAction = log("m1", "Heartbeat", Instant.parse("2024-01-01T00:00:00Z"))
        val blankAction = log("m2", "   ", Instant.parse("2024-01-02T00:00:00Z"))
        `when`(persistence.findMessageLogs("CP-001", null, null, 20)).thenReturn(listOf(withAction, blankAction))

        val result = resource.getHistory("CP-001", limit = 10, offset = 0)

        val messages = result["messages"] as List<OcppMessageDto>
        assertEquals("Heartbeat", messages[0].action)
        assertNull(messages[1].action)
    }

    @Test
    fun `getHistory returns 404 for an unknown charge point`() {
        `when`(persistence.findChargePointById("nope")).thenReturn(null)

        assertThrows(NotFoundException::class.java) {
            resource.getHistory("nope")
        }
    }
}

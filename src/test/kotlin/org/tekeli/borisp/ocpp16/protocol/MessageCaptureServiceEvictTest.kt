package org.tekeli.borisp.ocpp16.protocol

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.tekeli.borisp.ocpp16.persistence.OcppMessageLog
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

@Timeout(10)
class MessageCaptureServiceEvictTest {

    private val persistenceService = object : PersistenceService() {
        override fun createMessageLog(
            chargePointId: String,
            direction: String,
            messageType: String,
            action: String?,
            messageId: String,
            payload: String?
        ): OcppMessageLog {
            return OcppMessageLog(
                chargePointId = chargePointId,
                direction = direction,
                messageType = messageType,
                action = action ?: "",
                messageId = messageId,
                payload = payload
            )
        }

        override fun findMessageLogs(chargePointId: String, direction: String?, action: String?, limit: Int): List<OcppMessageLog> = emptyList()

        override fun purgeMessageLogsBefore(cutoff: Instant): Int = 0
    }

    private lateinit var service: MessageCaptureService

    @BeforeEach
    fun setup() {
        service = MessageCaptureService()
        service.persistenceService = persistenceService
    }

    @AfterEach
    fun tearDown() {
        service.close()
    }

    @Test
    fun `evict removes buffered messages for the charge point`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "Heartbeat", null))
        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m2", "Heartbeat", null))
        service.capture("CP-2", OcppMessageDirection.INBOUND, OcppMessage.Call("m3", "Heartbeat", null))

        assertEquals(2, service.getMessages("CP-1").size)

        service.evict("CP-1")

        assertTrue(service.getMessages("CP-1").isEmpty())
        assertEquals(1, service.getMessages("CP-2").size)
        assertEquals("m3", service.getMessages("CP-2")[0].messageId)
    }

    @Test
    fun `capture after evict starts a fresh buffer`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "Heartbeat", null))

        service.evict("CP-1")

        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m2", "Heartbeat", null))

        val messages = service.getMessages("CP-1")
        assertEquals(1, messages.size)
        assertEquals("m2", messages[0].messageId)
    }

    @Test
    fun `evict stops subscriber notifications for the charge point`() {
        val captured = CopyOnWriteArrayList<OcppMessageDto>()
        service.subscribe("CP-1") { captured.add(it) }

        service.evict("CP-1")

        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "Heartbeat", null))
        assertTrue(captured.isEmpty())
    }

    @Test
    fun `evict for unknown charge point is a no-op`() {
        assertDoesNotThrow { service.evict("UNKNOWN") }
    }
}

package org.tekeli.borisp.ocpp16.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.tekeli.borisp.ocpp16.persistence.OcppMessageLog
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList

@Timeout(10)
class MessageCaptureServiceTest {

    private val persistenceService = object : PersistenceService() {
        val persistedLogs = CopyOnWriteArrayList<OcppMessageLog>()
        override fun createMessageLog(
            chargePointId: String,
            direction: String,
            messageType: String,
            action: String?,
            messageId: String,
            payload: String?
        ): OcppMessageLog {
            val log = OcppMessageLog(
                chargePointId = chargePointId,
                direction = direction,
                messageType = messageType,
                action = action ?: "",
                messageId = messageId,
                payload = payload
            )
            synchronized(persistedLogs) { persistedLogs.add(log) }
            return log
        }
    }

    private lateinit var service: MessageCaptureService

    @BeforeEach
    fun setup() {
        service = MessageCaptureService()
        service.bufferSize = 5
        service.purgeHours = 24
        service::class.java.getDeclaredField("persistenceService").apply {
            isAccessible = true
            set(service, persistenceService)
        }
    }

    @Test
    fun `capture adds to buffer`() {
        val msg = OcppMessage.Call("msg-1", "BootNotification", mapOf("vendor" to "V"))
        service.capture("CP-1", OcppMessageDirection.INBOUND, msg)

        val messages = service.getMessages("CP-1")
        assertEquals(1, messages.size)
        assertEquals("CP-1", messages[0].chargePointId)
        assertEquals("INBOUND", messages[0].direction)
        assertEquals("CALL", messages[0].messageType)
        assertEquals("BootNotification", messages[0].action)
    }

    @Test
    fun `capture CallResult has no action`() {
        val msg = OcppMessage.CallResult("msg-2", mapOf("status" to "Accepted"))
        service.capture("CP-1", OcppMessageDirection.OUTBOUND, msg)

        val messages = service.getMessages("CP-1")
        assertEquals(1, messages.size)
        assertNull(messages[0].action)
        assertEquals("CALLRESULT", messages[0].messageType)
    }

    @Test
    fun `capture CallError includes error info`() {
        val msg = OcppMessage.CallError("msg-3", OcppErrorCode.PROTOCOL_ERROR, "desc", null)
        service.capture("CP-1", OcppMessageDirection.INBOUND, msg)

        val messages = service.getMessages("CP-1")
        assertEquals(1, messages.size)
        assertEquals("CALLERROR", messages[0].messageType)
        assertNull(messages[0].action)
    }

    @Test
    fun `buffer respects max size`() {
        repeat(10) { i ->
            val msg = OcppMessage.Call("msg-$i", "Heartbeat", null)
            service.capture("CP-1", OcppMessageDirection.INBOUND, msg)
        }

        val messages = service.getMessages("CP-1")
        assertEquals(5, messages.size)
        assertEquals("msg-5", messages[0].messageId)
        assertEquals("msg-9", messages[4].messageId)
    }

    @Test
    fun `buffer is per charge point`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "H", null))
        service.capture("CP-2", OcppMessageDirection.INBOUND, OcppMessage.Call("m2", "H", null))

        assertEquals(1, service.getMessages("CP-1").size)
        assertEquals(1, service.getMessages("CP-2").size)
        assertEquals("m1", service.getMessages("CP-1")[0].messageId)
        assertEquals("m2", service.getMessages("CP-2")[0].messageId)
    }

    @Test
    fun `getMessages for unknown charge point returns empty`() {
        assertTrue(service.getMessages("UNKNOWN").isEmpty())
    }

    @Test
    fun `subscribe notifies callback`() {
        val captured = CopyOnWriteArrayList<OcppMessageDto>()
        service.subscribe("CP-1") { captured.add(it) }

        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "H", null))

        assertEquals(1, captured.size)
        assertEquals("m1", captured[0].messageId)
    }

    @Test
    fun `persistAsync stores to persistence service`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "BootNotification", mapOf("k" to "v")))

        Thread.sleep(2000)
        assertTrue(synchronized(persistenceService.persistedLogs) { persistenceService.persistedLogs.isNotEmpty() })
        val log = synchronized(persistenceService.persistedLogs) { persistenceService.persistedLogs[0] }
        assertEquals("CP-1", log.chargePointId)
        assertEquals("INBOUND", log.direction)
        assertEquals("BootNotification", log.action)
    }

    @Test
    fun `payload contains full JSON`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND,
            OcppMessage.Call("m1", "BootNotification", mapOf("vendor" to "V", "model" to "M")))

        val messages = service.getMessages("CP-1")
        assertNotNull(messages[0].payload)
        assertTrue(messages[0].payload!!.contains("BootNotification"))
    }

    @Test
    fun `timestamp is valid ISO instant`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "H", null))
        val messages = service.getMessages("CP-1")
        Instant.parse(messages[0].timestamp)
    }
}

package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.OcppMessageLog
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.MessageCaptureService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppMessageDirection
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import java.time.Instant

class ChargePointRegistryEvictionTest {

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

    private lateinit var registry: ChargePointRegistry
    private lateinit var captureService: MessageCaptureService

    @BeforeEach
    fun setup() {
        registry = ChargePointRegistry()
        captureService = MessageCaptureService()
        captureService.persistenceService = persistenceService
        registry.messageCaptureService = captureService
    }

    @AfterEach
    fun tearDown() {
        captureService.close()
    }

    @Test
    fun `unregister evicts captured messages for the disconnected charge point`() {
        registry.register("s1", "c1", "CP-001", ResponseAwaiter())
        captureService.capture("CP-001", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "Heartbeat", null))

        assertEquals(1, captureService.getMessages("CP-001").size)

        registry.unregister("s1")

        assertTrue(captureService.getMessages("CP-001").isEmpty())
    }

    @Test
    fun `unregister does not evict messages of other charge points`() {
        registry.register("s1", "c1", "CP-001", ResponseAwaiter())
        captureService.capture("CP-001", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "Heartbeat", null))
        captureService.capture("CP-002", OcppMessageDirection.INBOUND, OcppMessage.Call("m2", "Heartbeat", null))

        registry.unregister("s1")

        assertTrue(captureService.getMessages("CP-001").isEmpty())
        assertEquals(1, captureService.getMessages("CP-002").size)
    }

    @Test
    fun `unregister without chargePointId does not throw`() {
        registry.register("s1", "c1", null, ResponseAwaiter())

        assertDoesNotThrow { registry.unregister("s1") }
    }
}

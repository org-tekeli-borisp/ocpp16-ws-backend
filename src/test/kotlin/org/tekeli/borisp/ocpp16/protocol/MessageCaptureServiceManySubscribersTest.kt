package org.tekeli.borisp.ocpp16.protocol

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.tekeli.borisp.ocpp16.persistence.OcppMessageLog
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

@Timeout(60)
class MessageCaptureServiceManySubscribersTest {

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
    fun `notifies all subscribers when there are many subscribers`() {
        val counter = AtomicInteger(0)
        repeat(50_000) { service.subscribe("CP-1") { counter.incrementAndGet() } }

        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "Heartbeat", null))

        assertEquals(50_000, counter.get())
    }
}

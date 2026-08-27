package org.tekeli.borisp.ocpp16.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import org.tekeli.borisp.ocpp16.persistence.OcppMessageLog
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import java.time.Duration
import java.time.Instant
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicInteger

@Timeout(10)
class MessageCaptureServiceTest {

    private val persistenceService = object : PersistenceService() {
        val persistedLogs = CopyOnWriteArrayList<OcppMessageLog>()
        val purgeCutoffs = CopyOnWriteArrayList<Instant>()
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

        override fun findMessageLogs(chargePointId: String, direction: String?, action: String?, limit: Int): List<OcppMessageLog> {
            synchronized(persistedLogs) {
                return persistedLogs
                    .filter { it.chargePointId == chargePointId }
                    .filter { direction.isNullOrEmpty() || it.direction == direction }
                    .filter { action.isNullOrEmpty() || it.action == action }
                    .take(limit)
            }
        }

        override fun purgeMessageLogsBefore(cutoff: Instant): Int {
            purgeCutoffs.add(cutoff)
            return 0
        }
    }

    private fun snapshotCutoffs(): List<Instant> =
        synchronized(persistenceService.purgeCutoffs) { persistenceService.purgeCutoffs.toList() }

    private lateinit var service: MessageCaptureService

    @BeforeEach
    fun setup() {
        service = MessageCaptureService()
        service.bufferSize = 5
        service.purgeHours = 24
        service.persistenceService = persistenceService
    }

    @AfterEach
    fun tearDown() {
        service.close()
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

    @Test
    fun `getMessagesFromDb returns persisted logs as DTOs`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND,
            OcppMessage.Call("m1", "BootNotification", mapOf("vendor" to "V")))

        Thread.sleep(2000)

        val dtos = service.getMessagesFromDb("CP-1", null, null, 100)
        assertEquals(1, dtos.size)
        assertEquals("CP-1", dtos[0].chargePointId)
        assertEquals("INBOUND", dtos[0].direction)
        assertEquals("BootNotification", dtos[0].action)
    }

    @Test
    fun `getMessagesFromDb filters by direction`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND,
            OcppMessage.Call("m1", "Heartbeat", null))
        service.capture("CP-1", OcppMessageDirection.OUTBOUND,
            OcppMessage.Call("m2", "Reset", mapOf("type" to "Soft")))

        Thread.sleep(2000)

        val inbound = service.getMessagesFromDb("CP-1", "INBOUND", null, 100)
        assertEquals(1, inbound.size)
        assertEquals("INBOUND", inbound[0].direction)
    }

    @Test
    fun `getMessagesFromDb filters by action`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND,
            OcppMessage.Call("m1", "Heartbeat", null))
        service.capture("CP-1", OcppMessageDirection.INBOUND,
            OcppMessage.Call("m2", "BootNotification", mapOf("vendor" to "V")))

        Thread.sleep(2000)

        val filtered = service.getMessagesFromDb("CP-1", null, "Heartbeat", 100)
        assertEquals(1, filtered.size)
        assertEquals("Heartbeat", filtered[0].action)
    }

    @Test
    fun `getMessagesFromDb with blank action returns null action`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND,
            OcppMessage.Call("m1", "BootNotification", mapOf("vendor" to "V")))

        Thread.sleep(2000)

        val log = synchronized(persistenceService.persistedLogs) { persistenceService.persistedLogs[0] }
        log.action = ""

        val dtos = service.getMessagesFromDb("CP-1", null, null, 100)
        assertEquals(1, dtos.size)
        assertNull(dtos[0].action)
    }

    @Test
    fun `purgeHours is coerced to at least 1`() {
        service.purgeHours = 0
        assertEquals(1, service.purgeHours)
        service.purgeHours = -5
        assertEquals(1, service.purgeHours)
    }

    @Test
    fun `unsubscribe stops notifications`() {
        val captured = CopyOnWriteArrayList<OcppMessageDto>()
        val callback: (OcppMessageDto) -> Unit = { captured.add(it) }
        service.subscribe("CP-1", callback)

        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "H", null))
        service.unsubscribe("CP-1", callback)
        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m2", "H", null))

        assertEquals(1, captured.size)
        assertEquals("m1", captured[0].messageId)
    }

    @Test
    fun `unsubscribe for unknown charge point is a no-op`() {
        service.unsubscribe("UNKNOWN") { }
    }

    @Test
    fun `unsubscribe during notification does not break delivery`() {
        val captured = CopyOnWriteArrayList<OcppMessageDto>()
        lateinit var callback: (OcppMessageDto) -> Unit
        callback = { dto ->
            captured.add(dto)
            service.unsubscribe("CP-1", callback)
        }
        service.subscribe("CP-1", callback)

        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "H", null))
        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m2", "H", null))

        assertEquals(1, captured.size)
        assertEquals("m1", captured[0].messageId)
    }

    @Test
    fun `getMessagesFromDb returns full dto fields`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND,
            OcppMessage.Call("m1", "BootNotification", mapOf("vendor" to "V")))

        Thread.sleep(2000)

        val dtos = service.getMessagesFromDb("CP-1", null, null, 100)
        assertEquals(1, dtos.size)
        assertEquals("CP-1", dtos[0].chargePointId)
        assertEquals("INBOUND", dtos[0].direction)
        assertEquals("CALL", dtos[0].messageType)
        assertEquals("BootNotification", dtos[0].action)
        assertEquals("m1", dtos[0].messageId)
        assertNotNull(dtos[0].payload)
        assertTrue(dtos[0].payload!!.contains("BootNotification"))
    }

    @Test
    fun `getMessagesFromDb on uninitialized service throws UninitializedPropertyAccessException`() {
        val fresh = MessageCaptureService()
        try {
            assertThrows(UninitializedPropertyAccessException::class.java) {
                fresh.getMessagesFromDb("CP-1", null, null, 100)
            }
        } finally {
            fresh.close()
        }
    }

    @Test
    fun `purge loop purges before cutoff and stops after close`() {
        val svc = MessageCaptureService()
        svc.purgeIntervalMillis = 1
        svc.persistenceService = persistenceService
        try {
            svc.startPurgeLoop()
            val deadline = System.currentTimeMillis() + 2000
            var cutoffs = snapshotCutoffs()
            while (cutoffs.size < 10 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10)
                cutoffs = snapshotCutoffs()
            }
            assertTrue(cutoffs.size >= 10, "expected purges within deadline, got ${cutoffs.size}")

            val span = Duration.between(cutoffs.first(), cutoffs[9])
            assertTrue(span.toMillis() >= 5, "purge loop must sleep between iterations, 10 purges took ${span.toNanos()} ns")

            val firstCutoff = cutoffs.first()
            val now = Instant.now()
            assertTrue(firstCutoff.isBefore(now.minus(Duration.ofHours(23))), "cutoff too recent: $firstCutoff")
            assertTrue(firstCutoff.isAfter(now.minus(Duration.ofHours(25))), "cutoff too old: $firstCutoff")

            svc.close()
            Thread.sleep(150)
            val countAfterClose = snapshotCutoffs().size
            Thread.sleep(150)
            assertEquals(countAfterClose, snapshotCutoffs().size, "purge loop must stop after close")
        } finally {
            svc.close()
        }
    }

    @Test
    fun `getMessagesFromDb uses default limit`() {
        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "Heartbeat", null))

        Thread.sleep(2000)

        val dtos = service.getMessagesFromDb("CP-1", null, null)
        assertEquals(1, dtos.size)
        assertEquals("m1", dtos[0].messageId)
    }

    @Test
    fun `failing callback does not break other subscribers`() {
        val captured = CopyOnWriteArrayList<OcppMessageDto>()
        service.subscribe("CP-1") { throw RuntimeException("boom") }
        service.subscribe("CP-1") { captured.add(it) }

        service.capture("CP-1", OcppMessageDirection.INBOUND, OcppMessage.Call("m1", "H", null))

        assertEquals(1, captured.size)
        assertEquals("m1", captured[0].messageId)
    }

    @Test
    fun `purge loop stops when thread is interrupted`() {
        val svc = MessageCaptureService()
        svc.purgeIntervalMillis = 1
        val attempts = AtomicInteger(0)
        val interruptingService = object : PersistenceService() {
            override fun purgeMessageLogsBefore(cutoff: Instant): Int {
                if (attempts.incrementAndGet() == 1) {
                    Thread.currentThread().interrupt()
                }
                return 0
            }
        }
        svc.persistenceService = interruptingService
        try {
            svc.startPurgeLoop()
            val deadline = System.currentTimeMillis() + 2000
            while (attempts.get() < 1 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10)
            }
            assertEquals(1, attempts.get())
            Thread.sleep(300)
            assertEquals(1, attempts.get(), "purge loop must stop after interruption")
        } finally {
            svc.close()
        }
    }

    @Test
    fun `purge loop survives purge failures`() {
        val svc = MessageCaptureService()
        svc.purgeIntervalMillis = 1
        val attempts = AtomicInteger(0)
        val failingService = object : PersistenceService() {
            override fun purgeMessageLogsBefore(cutoff: Instant): Int {
                attempts.incrementAndGet()
                throw RuntimeException("db down")
            }
        }
        svc.persistenceService = failingService
        try {
            svc.startPurgeLoop()
            val deadline = System.currentTimeMillis() + 2000
            while (attempts.get() < 2 && System.currentTimeMillis() < deadline) {
                Thread.sleep(10)
            }
            assertTrue(attempts.get() >= 2, "purge loop must survive failures, attempts=${attempts.get()}")
        } finally {
            svc.close()
        }
    }

    @Test
    fun `capture with failing toJson stores null payload`() {
        val msg = mock(OcppMessage.Call::class.java)
        `when`(msg.toJson()).thenThrow(RuntimeException("boom"))
        `when`(msg.messageId).thenReturn("m1")
        `when`(msg.type).thenReturn(OcppMessageType.CALL)
        `when`(msg.action).thenReturn("Heartbeat")

        service.capture("CP-1", OcppMessageDirection.INBOUND, msg)

        val messages = service.getMessages("CP-1")
        assertEquals(1, messages.size)
        assertNull(messages[0].payload)
    }
}

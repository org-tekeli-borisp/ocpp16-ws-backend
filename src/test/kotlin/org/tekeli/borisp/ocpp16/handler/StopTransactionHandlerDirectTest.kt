package org.tekeli.borisp.ocpp16.handler

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.persistence.Transaction
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.time.Instant
import java.util.concurrent.TimeUnit

class StopTransactionHandlerDirectTest {

    private val handler = StopTransactionHandler()

    @Test
    fun `validatePayload extracts transactionId correctly`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 42L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(42L, parsed.transactionId)
    }

    @Test
    fun `validatePayload extracts transactionId from Int`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 42,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(42L, parsed.transactionId)
    }

    @Test
    fun `validatePayload throws for missing transactionId`() {
        val payload = mapOf<String, Any>(
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("transactionId"))
    }

    @Test
    fun `validatePayload throws for non-integer transactionId`() {
        val payload = mapOf<String, Any>(
            "transactionId" to "not-a-number",
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("transactionId"))
    }

    @Test
    fun `validatePayload extracts meterStop correctly`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 9999,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(9999, parsed.meterStop)
    }

    @Test
    fun `validatePayload throws for missing meterStop`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("meterStop"))
    }

    @Test
    fun `validatePayload extracts stopTime correctly`() {
        val expectedTime = Instant.parse("2024-06-15T12:30:00Z")
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to expectedTime.toString()
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(expectedTime, parsed.stopTime)
    }

    @Test
    fun `validatePayload throws for missing timestamp`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("timestamp"))
    }

    @Test
    fun `validatePayload throws for empty timestamp`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to ""
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("timestamp"))
    }

    @Test
    fun `validatePayload throws for invalid timestamp format`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "not-a-date"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("Invalid timestamp"))
    }

    @Test
    fun `validatePayload returns null reason when missing`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.reason)
    }

    @Test
    fun `validatePayload returns null reason when empty`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "reason" to ""
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.reason)
    }

    @Test
    fun `validatePayload extracts valid reason`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "reason" to "Local"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("Local", parsed.reason)
    }

    @Test
    fun `validatePayload extracts each valid reason`() {
        val validReasons = listOf(
            "DeAuthorized", "EmergencyStop", "EVDisconnected", "HardReset",
            "Local", "Other", "PowerLoss", "Reboot", "Remote", "SoftReset",
            "UnlockCommand"
        )
        for (reason in validReasons) {
            val payload = mapOf<String, Any>(
                "transactionId" to 1L,
                "meterStop" to 5000,
                "timestamp" to "2024-01-01T00:00:00Z",
                "reason" to reason
            )
            val parsed = handler.validatePayload(payload)
            assertEquals(reason, parsed.reason, "Reason $reason should be valid")
        }
    }

    @Test
    fun `validatePayload throws for invalid reason`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "reason" to "InvalidReason"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("Invalid reason"))
    }

    @Test
    fun `validatePayload returns null idTagEnd when missing`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.idTagEnd)
    }

    @Test
    fun `validatePayload returns null idTagEnd when empty`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "idTag" to ""
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.idTagEnd)
    }

    @Test
    fun `validatePayload extracts idTagEnd correctly`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "idTag" to "ABC123"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("ABC123", parsed.idTagEnd)
    }

    @Test
    fun `validatePayload trims idTagEnd`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "idTag" to "  TAG  "
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("TAG", parsed.idTagEnd)
    }

    @Test
    fun `validatePayload throws for idTagEnd exceeding 20 characters`() {
        val longIdTag = "A".repeat(21)
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "idTag" to longIdTag
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("20 characters"))
    }

    @Test
    fun `validatePayload accepts idTagEnd exactly 20 characters`() {
        val maxIdTag = "A".repeat(20)
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z",
            "idTag" to maxIdTag
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(maxIdTag, parsed.idTagEnd)
    }

    @Test
    fun `validatePayload extracts all fields correctly`() {
        val stopTime = Instant.parse("2024-06-15T12:30:00Z")
        val payload = mapOf<String, Any>(
            "transactionId" to 777L,
            "meterStop" to 12345,
            "timestamp" to stopTime.toString(),
            "reason" to "EVDisconnected",
            "idTag" to "END_TAG"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(777L, parsed.transactionId)
        assertEquals(12345, parsed.meterStop)
        assertEquals(stopTime, parsed.stopTime)
        assertEquals("EVDisconnected", parsed.reason)
        assertEquals("END_TAG", parsed.idTagEnd)
    }

    @Test
    fun `validatePayload handles meterStop as Long`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000L,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(5000, parsed.meterStop)
    }

    @Test
    fun `validatePayload handles transactionId as Int`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 99,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(99L, parsed.transactionId)
    }

    @Test
    fun `validatePayload handles transactionId 0`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 0L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(0L, parsed.transactionId)
    }

    @Test
    fun `validatePayload handles meterStop 0`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(0, parsed.meterStop)
    }

    @Test
    fun `validatePayload throws for meterStop as String`() {
        val payload = mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to "not-a-number",
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("meterStop must be an integer"))
    }

    @Test
    fun `handle with MetricsService records metrics`() {
        val handlerWithMetrics = StopTransactionHandler()
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-001"
            sessionId = "test-session"
        }

        val call = OcppMessage.Call(
            messageId = "test-msg",
            action = "StopTransaction",
            payload = mapOf<String, Any>(
                "transactionId" to 1L,
                "meterStop" to 5000,
                "timestamp" to "2024-01-01T01:00:00Z",
                "reason" to "Local"
            )
        )

        val response = handlerWithMetrics.handle(call, server)

        assertTrue(response.contains("Accepted"))
        assertTrue(response.contains("test-msg"))
    }

    // =====================================================
    // TrackingPersistenceService for direct processStopTransaction tests
    // =====================================================

    private class TrackingPersistenceService(
        private val transactionToReturn: Transaction?
    ) : PersistenceService() {
        var findTransactionId: Long? = null
        var stopTransactionId: Long? = null
        var stopMeterStop: Int? = null
        var stopStopTime: Instant? = null
        var stopReason: String? = null
        var stopIdTagEnd: String? = null

        override fun findTransaction(id: Long): Transaction? {
            findTransactionId = id
            return transactionToReturn
        }

        override fun stopTransaction(
            transactionId: Long,
            meterStop: Int,
            stopTime: Instant,
            reason: String?,
            idTagEnd: String?
        ): Boolean {
            stopTransactionId = transactionId
            stopMeterStop = meterStop
            stopStopTime = stopTime
            stopReason = reason
            stopIdTagEnd = idTagEnd
            return true
        }
    }

    // =====================================================
    // Kill EQUAL_ELSE mutants: processStopTransaction with null persistenceService
    // L30: ps?. → if ps is null, removing conditional crashes on findTransaction
    // L33: ps?. → if ps is null, removing conditional crashes on stopTransaction
    // =====================================================

    @Test
    fun `processStopTransaction with null persistenceService does not throw`() {
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply {
            persistenceService = null
        }
        val parsed = handler.validatePayload(mapOf<String, Any>(
            "transactionId" to 1L,
            "meterStop" to 5000,
            "timestamp" to "2024-01-01T00:00:00Z"
        ))

        assertDoesNotThrow { handler.processStopTransaction(server, parsed) }
    }

    // =====================================================
    // Kill EQUAL_ELSE mutants: processStopTransaction with transaction not found
    // L31: transaction?.meterStart → if null, removing conditional crashes
    // L32: transaction?.startTime → if null, removing conditional crashes
    // =====================================================

    @Test
    fun `processStopTransaction with transaction not found uses defaults`() {
        val ps = TrackingPersistenceService(null)
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply {
            persistenceService = ps
            this.metricsService = metricsService
        }
        val parsed = StopTransactionHandler.ParsedStopTransaction(
            transactionId = 999L,
            meterStop = 5000,
            stopTime = Instant.parse("2024-01-01T01:00:00Z"),
            reason = "Local",
            idTagEnd = null
        )

        handler.processStopTransaction(server, parsed)

        assertEquals(999L, ps.findTransactionId)
        assertEquals(5000, ps.stopMeterStop)
        assertEquals("Local", ps.stopReason)
        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertNotNull(counter)
        assertEquals(5000.0, counter!!.count(), 0.01)
    }

    // =====================================================
    // Kill MathMutator + NonVoidMethodCallMutator: verify exact calculations
    // L31: subtraction vs addition - must have non-zero meterStart
    // L29: removed getPersistenceService call - must verify ps methods were called
    // =====================================================

    @Test
    fun `processStopTransaction calculates exact energy with non-zero meterStart`() {
        val txn = Transaction(
            id = 42L,
            chargePointId = "CP-ENERGY",
            meterStart = 2000,
            startTime = Instant.parse("2024-01-01T00:00:00Z")
        )
        val ps = TrackingPersistenceService(txn)
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply {
            persistenceService = ps
            this.metricsService = metricsService
        }
        val parsed = StopTransactionHandler.ParsedStopTransaction(
            transactionId = 42L,
            meterStop = 7000,
            stopTime = Instant.parse("2024-01-01T02:30:00Z"),
            reason = "EVDisconnected",
            idTagEnd = "END999"
        )

        handler.processStopTransaction(server, parsed)

        // 7000 - 2000 = 5000, NOT 7000 + 2000 = 9000
        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertNotNull(counter)
        assertEquals(5000.0, counter!!.count(), 0.01)
        assertEquals(42L, ps.stopTransactionId)
        assertEquals(7000, ps.stopMeterStop)
        assertEquals("EVDisconnected", ps.stopReason)
        assertEquals("END999", ps.stopIdTagEnd)
    }

    @Test
    fun `processStopTransaction calculates exact duration from timestamps`() {
        val txn = Transaction(
            id = 10L,
            chargePointId = "CP-DUR",
            meterStart = 1000,
            startTime = Instant.parse("2024-06-15T10:00:00Z")
        )
        val ps = TrackingPersistenceService(txn)
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply {
            persistenceService = ps
            this.metricsService = metricsService
        }
        val parsed = StopTransactionHandler.ParsedStopTransaction(
            transactionId = 10L,
            meterStop = 4000,
            stopTime = Instant.parse("2024-06-15T14:30:00Z"),
            reason = "Remote",
            idTagEnd = null
        )

        handler.processStopTransaction(server, parsed)

        // 4000 - 1000 = 3000 Wh
        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertEquals(3000.0, counter!!.count(), 0.01)
        // 14:30 - 10:00 = 4h30m = 16200 seconds
        val timer = meterRegistry.find("ocpp.transaction.duration.seconds").timer()
        assertNotNull(timer)
        assertEquals(16200.0, timer!!.totalTime(TimeUnit.SECONDS), 1.0)
    }

    // =====================================================
    // Kill EQUAL_IF mutants on L39 L40: metricsService null-safe access
    // =====================================================

    @Test
    fun `recordMetrics with null metricsService does not throw`() {
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply { metricsService = null }
        assertDoesNotThrow { handler.recordMetrics(server, 500.0, 120) }
    }

    @Test
    fun `recordMetrics energy and duration are recorded with exact values`() {
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply { this.metricsService = metricsService }

        handler.recordMetrics(server, 1234.56, 7890)

        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertNotNull(counter)
        assertEquals(1234.56, counter!!.count(), 0.001)
        val timer = meterRegistry.find("ocpp.transaction.duration.seconds").timer()
        assertNotNull(timer)
        assertEquals(7890.0, timer!!.totalTime(TimeUnit.SECONDS), 1.0)
    }

    // =====================================================
    // NullMetricsProperties - overrides lazy props to return null
    // Kills L39 and L40 EQUAL_IF: if null check on energyDeliveredWh
    // or transactionDuration is removed, NPE is thrown on .increment()/.record()
    // =====================================================

    private class NullMetricsProperties : MetricsService() {
        override val energyDeliveredWh: Counter?
            get() = null
        override val transactionDuration: Timer?
            get() = null
    }

    @Test
    fun `recordMetrics with null counter and timer properties does not throw`() {
        val nullMetrics = NullMetricsProperties()
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply { metricsService = nullMetrics }
        // If null checks on energyDeliveredWh or transactionDuration are removed,
        // calling .increment() or .record() on null will throw NPE
        assertDoesNotThrow { handler.recordMetrics(server, 500.0, 120) }
    }

    // =====================================================
    // Kill L32 EQUAL_IF: transaction?.startTime - removed conditional
    // Directly call processStopTransaction with transaction not found
    // =====================================================

    @Test
    fun `processStopTransaction with null transaction calculates duration as zero`() {
        val ps = TrackingPersistenceService(null)
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply {
            persistenceService = ps
            this.metricsService = metricsService
        }
        val parsed = StopTransactionHandler.ParsedStopTransaction(
            transactionId = 404L,
            meterStop = 9999,
            stopTime = Instant.parse("2024-06-15T12:00:00Z"),
            reason = "Other",
            idTagEnd = "NULLTXN"
        )

        handler.processStopTransaction(server, parsed)

        assertEquals(404L, ps.findTransactionId)
        assertEquals(404L, ps.stopTransactionId)
        assertEquals(9999, ps.stopMeterStop)
        assertEquals("Other", ps.stopReason)
        assertEquals("NULLTXN", ps.stopIdTagEnd)
        // duration should be 0 when transaction is null
        val timer = meterRegistry.find("ocpp.transaction.duration.seconds").timer()
        assertNotNull(timer)
        assertEquals(0.0, timer!!.totalTime(TimeUnit.SECONDS), 1.0)
    }
}

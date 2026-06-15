package org.tekeli.borisp.ocpp16.handler

import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.ChargePoint
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.persistence.Transaction
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.time.Instant

class StartTransactionHandlerDirectTest {

    private val handler = StartTransactionHandler()

    // -- connectorId tests --

    @Test
    fun `validatePayload extracts connectorId as 1`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(1, parsed.connectorId)
    }

    @Test
    fun `validatePayload extracts large connectorId`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 999999,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(999999, parsed.connectorId)
    }

    @Test
    fun `validatePayload extracts connectorId from Long`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 42L,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(42, parsed.connectorId)
    }

    @Test
    fun `validatePayload throws for missing connectorId`() {
        val payload = mapOf<String, Any>(
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("connectorId"))
    }

    @Test
    fun `validatePayload throws for non-integer connectorId`() {
        val payload = mapOf<String, Any>(
            "connectorId" to "not-a-number",
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("connectorId"))
    }

    @Test
    fun `validatePayload throws for zero connectorId`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 0,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("connectorId"))
    }

    @Test
    fun `validatePayload throws for negative connectorId`() {
        val payload = mapOf<String, Any>(
            "connectorId" to -5,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("connectorId"))
    }

    // -- idTag tests --

    @Test
    fun `validatePayload extracts valid idTag`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("RFID001", parsed.idTag)
    }

    @Test
    fun `validatePayload throws for missing idTag`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("idTag"))
    }

    @Test
    fun `validatePayload throws for empty idTag`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("idTag"))
    }

    @Test
    fun `validatePayload throws for idTag exceeding 20 characters`() {
        val longIdTag = "A".repeat(21)
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to longIdTag,
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("20 characters"))
    }

    @Test
    fun `validatePayload accepts idTag exactly 20 characters`() {
        val maxIdTag = "A".repeat(20)
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to maxIdTag,
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(maxIdTag, parsed.idTag)
    }

    // -- meterStart tests --

    @Test
    fun `validatePayload extracts valid meterStart`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 12345,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(12345, parsed.meterStart)
    }

    @Test
    fun `validatePayload extracts meterStart as zero`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(0, parsed.meterStart)
    }

    @Test
    fun `validatePayload extracts negative meterStart`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to -100,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(-100, parsed.meterStart)
    }

    @Test
    fun `validatePayload extracts meterStart from Long`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 99999L,
            "timestamp" to "2024-01-01T00:00:00Z"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(99999, parsed.meterStart)
    }

    @Test
    fun `validatePayload throws for missing meterStart`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("meterStart"))
    }

    @Test
    fun `validatePayload throws for non-integer meterStart`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to "not-a-number",
            "timestamp" to "2024-01-01T00:00:00Z"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("meterStart"))
    }

    // -- startTime (timestamp) tests --

    @Test
    fun `validatePayload extracts valid startTime`() {
        val expected = Instant.parse("2024-06-15T12:30:00Z")
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to expected.toString()
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(expected, parsed.startTime)
    }

    @Test
    fun `validatePayload throws for missing timestamp`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("timestamp"))
    }

    @Test
    fun `validatePayload throws for empty timestamp`() {
        val payload = mapOf<String, Any>(
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
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
            "connectorId" to 1,
            "idTag" to "RFID001",
            "meterStart" to 0,
            "timestamp" to "not-a-date"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("Invalid timestamp"))
    }

    // -- full payload tests --

    @Test
    fun `validatePayload extracts all fields correctly`() {
        val startTime = Instant.parse("2024-06-15T12:30:00Z")
        val payload = mapOf<String, Any>(
            "connectorId" to 3,
            "idTag" to "RFID999",
            "meterStart" to 5000,
            "timestamp" to startTime.toString()
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(3, parsed.connectorId)
        assertEquals("RFID999", parsed.idTag)
        assertEquals(5000, parsed.meterStart)
        assertEquals(startTime, parsed.startTime)
    }

    @Test
    fun `validatePayload extracts all fields with edge values`() {
        val startTime = Instant.parse("2024-12-31T23:59:59Z")
        val maxIdTag = "A".repeat(20)
        val payload = mapOf<String, Any>(
            "connectorId" to 100000,
            "idTag" to maxIdTag,
            "meterStart" to 999999,
            "timestamp" to startTime.toString()
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(100000, parsed.connectorId)
        assertEquals(maxIdTag, parsed.idTag)
        assertEquals(999999, parsed.meterStart)
        assertEquals(startTime, parsed.startTime)
    }
}

// -- handle() integration tests (kills SURVIVED mutants in createTransaction / handle) --

class StartTransactionHandlerHandleTest {

    private val mapper = ObjectMapper()

    // Minimal test server: no persistenceService, no registry
    private fun newServer(sessionId: String = "test-session", persistenceService: PersistenceService? = null): OcppWebSocketServer {
        val server = object : OcppWebSocketServer() {
            override fun sendText(text: String): io.smallrye.mutiny.Uni<Void> =
                io.smallrye.mutiny.Uni.createFrom().voidItem()
        }
        server.sessionId = sessionId
        server.persistenceService = persistenceService
        return server
    }

    private fun extractTransactionId(json: String): Long {
        val root = mapper.readTree(json)
        val payload = root.get(2)
        return payload.get("transactionId").asLong()
    }

    // M1: persistenceService == null  ->  transactionId == 1
    // Kills: line 34 "removed call to getPersistenceService"
    //       line 34 "removed conditional - replaced equality check with true"
    @Test
    fun `handle returns transactionId 1 when persistenceService is null`() {
        val server = newServer(persistenceService = null)
        val handler = StartTransactionHandler()
        val call = OcppMessage.Call(
            messageId = "msg-1",
            action = "StartTransaction",
            payload = mapOf(
                "connectorId" to 1,
                "idTag" to "RFID001",
                "meterStart" to 100,
                "timestamp" to "2024-01-01T00:00:00Z"
            )
        )
        val result = handler.handle(call, server)
        val txnId = extractTransactionId(result)
        assertEquals(1, txnId)
    }

    // M2: persistenceService != null, chargePoint found  ->  transactionId == 42
    // Kills: line 14 component1 (connectorId)
    //        line 14 component2 (idTag)
    //        line 14 component3 (meterStart)
    //        line 14 component4 (startTime)
    // Because we verify createTransaction receives the correct arguments.
    @Test
    fun `handle delegates correct params to createTransaction`() {
        val captured = mutableListOf<Any>()

        val ps = object : PersistenceService() {
            override fun findChargePointBySessionId(sessionId: String): ChargePoint? =
                ChargePoint(chargePointId = "cp-1", sessionId = sessionId)
            override fun createTransaction(
                chargePointId: String, connectorId: Int,
                idTag: String, meterStart: Int, startTime: Instant
            ): Transaction {
                captured += chargePointId
                captured += connectorId
                captured += idTag
                captured += meterStart
                captured += startTime
                return Transaction(id = 42L, chargePointId = chargePointId,
                    connectorId = connectorId, idTag = idTag,
                    meterStart = meterStart, startTime = startTime)
            }
        }

        val server = newServer("sess-1", ps)
        val metrics = object : MetricsService() {}
        val handler = StartTransactionHandler(metrics)

        val startTime = Instant.parse("2024-06-15T12:30:00Z")
        val call = OcppMessage.Call(
            messageId = "msg-2",
            action = "StartTransaction",
            payload = mapOf(
                "connectorId" to 7,
                "idTag" to "TAG-ABC",
                "meterStart" to 5555,
                "timestamp" to startTime.toString()
            )
        )
        val result = handler.handle(call, server)

        // transactionId comes from Transaction.id  (kills line 34 removed-call-to-getPersistenceService)
        assertEquals(42, extractTransactionId(result))

        // Verify each destructured value from validatePayload flowed through
        // (kills line 14 component1/2/3/4 mutants)
        assertEquals("cp-1", captured[0])
        assertEquals(7, captured[1])       // connectorId  (component1)
        assertEquals("TAG-ABC", captured[2])  // idTag      (component2)
        assertEquals(5555, captured[3])    // meterStart   (component3)
        assertEquals(startTime, captured[4]) // startTime    (component4)
    }

    // M3: persistenceService != null but findChargePointBySessionId returns null -> 1
    @Test
    fun `handle returns transactionId 1 when chargePoint not found`() {
        val ps = object : PersistenceService() {
            override fun findChargePointBySessionId(sessionId: String): ChargePoint? = null
        }
        val server = newServer("missing-session", ps)
        val handler = StartTransactionHandler()
        val call = OcppMessage.Call(
            messageId = "msg-3",
            action = "StartTransaction",
            payload = mapOf(
                "connectorId" to 1,
                "idTag" to "RFID001",
                "meterStart" to 0,
                "timestamp" to "2024-01-01T00:00:00Z"
            )
        )
        val result = handler.handle(call, server)
        assertEquals(1, extractTransactionId(result))
    }

    // M4: Transaction.id is null -> fallback to 1
    @Test
    fun `handle returns transactionId 1 when Transaction id is null`() {
        val ps = object : PersistenceService() {
            override fun findChargePointBySessionId(sessionId: String): ChargePoint? =
                ChargePoint(chargePointId = "cp-2", sessionId = sessionId)
            override fun createTransaction(
                chargePointId: String, connectorId: Int,
                idTag: String, meterStart: Int, startTime: Instant
            ): Transaction = Transaction(id = null)
        }
        val server = newServer("sess-2", ps)
        val handler = StartTransactionHandler()
        val call = OcppMessage.Call(
            messageId = "msg-4",
            action = "StartTransaction",
            payload = mapOf(
                "connectorId" to 1,
                "idTag" to "RFID001",
                "meterStart" to 0,
                "timestamp" to "2024-01-01T00:00:00Z"
            )
        )
        val result = handler.handle(call, server)
        assertEquals(1, extractTransactionId(result))
    }

    // M5: Verify metricsService.onTransactionStarted is called
    @Test
    fun `handle calls metricsService onTransactionStarted`() {
        var metricsCalled = false
        val metrics = object : MetricsService() {
            override fun onTransactionStarted() {
                metricsCalled = true
                super.onTransactionStarted()
            }
        }

        val ps = object : PersistenceService() {
            override fun findChargePointBySessionId(sessionId: String): ChargePoint? =
                ChargePoint(chargePointId = "cp-3", sessionId = sessionId)
            override fun createTransaction(
                chargePointId: String, connectorId: Int,
                idTag: String, meterStart: Int, startTime: Instant
            ): Transaction = Transaction(id = 99L)
        }
        val server = newServer("sess-3", ps)
        val handler = StartTransactionHandler(metrics)
        val call = OcppMessage.Call(
            messageId = "msg-5",
            action = "StartTransaction",
            payload = mapOf(
                "connectorId" to 1,
                "idTag" to "RFID001",
                "meterStart" to 0,
                "timestamp" to "2024-01-01T00:00:00Z"
            )
        )
        handler.handle(call, server)
        assertTrue(metricsCalled)
    }
}

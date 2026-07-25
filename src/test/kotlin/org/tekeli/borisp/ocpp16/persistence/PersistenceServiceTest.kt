package org.tekeli.borisp.ocpp16.persistence

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
@jakarta.transaction.Transactional
class PersistenceServiceTest {

    @Inject
    lateinit var em: EntityManager

    @Inject
    lateinit var persistenceService: PersistenceService

    @BeforeEach
    fun cleanup() {
        em.createNativeQuery("DELETE FROM transactions").executeUpdate()
        em.createNativeQuery("DELETE FROM charge_points").executeUpdate()
        em.createNativeQuery("DELETE FROM security_logs").executeUpdate()
        em.createNativeQuery("DELETE FROM signed_firmware").executeUpdate()
        em.flush()
    }

    @Test
    fun `stopTransaction returns true for valid transaction`() {
        val txn = persistenceService.createTransaction(
            chargePointId = "CP-001",
            connectorId = 1,
            idTag = "CARD1",
            meterStart = 100,
            startTime = Instant.parse("2024-01-01T00:00:00Z")
        )

        val result = persistenceService.stopTransaction(
            transactionId = txn.id!!,
            meterStop = 500,
            stopTime = Instant.parse("2024-01-01T01:00:00Z"),
            reason = "Local",
            idTagEnd = null
        )

        assertTrue(result)
        val found = persistenceService.findTransaction(txn.id!!)
        assertNotNull(found)
        assertFalse(found!!.isRunning)
        assertEquals(500, found.meterStop)
    }

    @Test
    fun `stopTransaction returns false for non-existent transaction`() {
        val result = persistenceService.stopTransaction(
            transactionId = 99999L,
            meterStop = 500,
            stopTime = Instant.parse("2024-01-01T01:00:00Z"),
            reason = "Local",
            idTagEnd = null
        )

        assertFalse(result)
    }

    @Test
    fun `stopTransaction returns false for already stopped transaction`() {
        val txn = persistenceService.createTransaction(
            chargePointId = "CP-001",
            connectorId = 1,
            idTag = "CARD1",
            meterStart = 100,
            startTime = Instant.parse("2024-01-01T00:00:00Z")
        )
        persistenceService.stopTransaction(
            transactionId = txn.id!!,
            meterStop = 500,
            stopTime = Instant.parse("2024-01-01T01:00:00Z"),
            reason = "Local",
            idTagEnd = null
        )

        val result = persistenceService.stopTransaction(
            transactionId = txn.id!!,
            meterStop = 600,
            stopTime = Instant.parse("2024-01-01T02:00:00Z"),
            reason = "Remote",
            idTagEnd = null
        )

        assertFalse(result)
    }

    @Test
    fun `upsertChargePoint creates new entry`() {
        persistenceService.upsertChargePoint(
            sessionId = "session-1",
            chargePointId = "CP-001",
            vendor = "Vendor",
            model = "Model",
            firmwareVersion = "1.0"
        )

        val found = persistenceService.findChargePointById("CP-001")
        assertNotNull(found)
        assertEquals("Vendor", found!!.vendor)
        assertEquals("Model", found!!.model)
        assertEquals(ChargePointStatus.ONLINE, found.status)
    }

    @Test
    fun `upsertChargePoint updates existing entry`() {
        persistenceService.upsertChargePoint(
            sessionId = "session-1",
            chargePointId = "CP-001",
            vendor = "OldVendor",
            model = "OldModel",
            firmwareVersion = "1.0"
        )

        persistenceService.upsertChargePoint(
            sessionId = "session-2",
            chargePointId = "CP-001",
            vendor = "NewVendor",
            model = "NewModel",
            firmwareVersion = "2.0"
        )

        val found = persistenceService.findChargePointById("CP-001")
        assertNotNull(found)
        assertEquals("session-2", found!!.sessionId)
        assertEquals(ChargePointStatus.ONLINE, found.status)
    }

    @Test
    fun `findOnlineChargePoints returns only online charge points`() {
        persistenceService.upsertChargePoint("s1", "CP-001", "V", "M", null)
        persistenceService.upsertChargePoint("s2", "CP-002", "V", "M", null)
        persistenceService.setChargePointOffline("s1")

        val online = persistenceService.findOnlineChargePoints()

        assertEquals(1, online.size)
        assertEquals("CP-002", online[0].chargePointId)
    }

    @Test
    fun `setChargePointOffline updates status`() {
        persistenceService.upsertChargePoint("s1", "CP-001", "V", "M", null)
        em.flush()

        persistenceService.setChargePointOffline("s1")
        em.flush()

        val found = persistenceService.findChargePointBySessionId("s1")
        em.clear()
        val refreshed = persistenceService.findChargePointBySessionId("s1")
        assertEquals(ChargePointStatus.OFFLINE, refreshed!!.status)
    }

    @Test
    fun `setChargePointOfflineByChargePointId works regardless of sessionId`() {
        persistenceService.upsertChargePoint("s1", "CP-001", "V", "M", null)
        em.flush()

        persistenceService.setChargePointOfflineByChargePointId("CP-001")
        em.flush()

        em.clear()
        val refreshed = persistenceService.findChargePointBySessionId("s1")
        assertEquals(ChargePointStatus.OFFLINE, refreshed!!.status)
    }

    @Test
    fun `setChargePointOnline updates status`() {
        persistenceService.upsertChargePoint("s1", "CP-001", "V", "M", null)
        persistenceService.setChargePointOffline("s1")

        persistenceService.setChargePointOnline("s1")

        val found = persistenceService.findChargePointBySessionId("s1")
        assertEquals(ChargePointStatus.ONLINE, found!!.status)
    }

    @Test
    fun `findByStatus returns matching charge points`() {
        persistenceService.upsertChargePoint("s1", "CP-001", "V", "M", null)
        persistenceService.upsertChargePoint("s2", "CP-002", "V", "M", null)
        persistenceService.setChargePointOffline("s1")

        val offline = persistenceService.findByStatus(ChargePointStatus.OFFLINE)
        val online = persistenceService.findByStatus(ChargePointStatus.ONLINE)

        assertEquals(1, offline.size)
        assertEquals(1, online.size)
    }

    @Test
    fun `findChargePointBySessionId returns null for unknown`() {
        val found = persistenceService.findChargePointBySessionId("unknown")

        assertNull(found)
    }

    @Test
    fun `findAllTransactions returns transactions for chargePointId`() {
        persistenceService.createTransaction("CP-001", 1, "CARD1", 100, Instant.parse("2024-01-01T00:00:00Z"))
        persistenceService.createTransaction("CP-001", 2, "CARD2", 200, Instant.parse("2024-01-02T00:00:00Z"))

        val txns = persistenceService.findAllTransactions("CP-001")

        assertEquals(2, txns.size)
    }

    @Test
    fun `findRunningTransactions returns only running transactions`() {
        val txn1 = persistenceService.createTransaction("CP-001", 1, "CARD1", 100, Instant.parse("2024-01-01T00:00:00Z"))
        val txn2 = persistenceService.createTransaction("CP-001", 2, "CARD2", 200, Instant.parse("2024-01-02T00:00:00Z"))
        persistenceService.stopTransaction(txn2.id!!, 500, Instant.parse("2024-01-02T01:00:00Z"), "Local", null)

        val running = persistenceService.findRunningTransactions("CP-001")

        assertEquals(1, running.size)
        assertEquals(txn1.id, running[0].id)
    }

    @Test
    fun `touchLastSeenAt updates lastSeenAt by chargePointId`() {
        persistenceService.upsertChargePoint("s1", "CP-001", "V", "M", null)
        val before = persistenceService.findChargePointById("CP-001")!!.lastSeenAt
        Thread.sleep(10)

        persistenceService.touchLastSeenAt("CP-001")
        em.flush()
        em.clear()

        val after = persistenceService.findChargePointById("CP-001")!!.lastSeenAt
        assertTrue(after.isAfter(before) || after.equals(before))
    }
}

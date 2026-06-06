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
class TransactionRepositoryTest {

    @Inject
    lateinit var em: EntityManager

    @BeforeEach
    fun cleanup() {
        em.createNativeQuery("DELETE FROM transactions").executeUpdate()
        em.flush()
    }

    private fun persistAndFlush(txn: Transaction) {
        em.persist(txn)
        em.flush()
    }

    @Test
    fun `should persist and find transaction`() {
        val txn = Transaction(
            chargePointId = "Tesla-Model3-1.0",
            connectorId = 1,
            idTag = "ABC123",
            meterStart = 1000,
            startTime = Instant.parse("2024-01-01T00:00:00Z")
        )
        persistAndFlush(txn)

        assertNotNull(txn.id)
        assertTrue(txn.isRunning)

        val found = em.find(Transaction::class.java, txn.id)
        assertNotNull(found)
        assertEquals("ABC123", found.idTag)
    }

    @Test
    fun `should stop transaction and calculate duration`() {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val stop = Instant.parse("2024-01-01T01:30:00Z")

        val txn = Transaction(
            chargePointId = "Tesla-Model3-1.0",
            connectorId = 1,
            idTag = "ABC123",
            meterStart = 1000,
            startTime = start
        )
        persistAndFlush(txn)

        txn.stop(5500, stop, "Local", null)
        em.flush()

        assertFalse(txn.isRunning)
        assertEquals(5400, txn.durationSeconds)
        assertEquals(4500, txn.energyWh)
        assertEquals("Local", txn.stopReason)
    }

    @Test
    fun `should find running transactions by chargePointId`() {
        persistAndFlush(Transaction(chargePointId = "CP-1", connectorId = 1, idTag = "TAG1", meterStart = 100, startTime = Instant.parse("2024-01-01T00:00:00Z")))

        val txn2 = Transaction(chargePointId = "CP-1", connectorId = 2, idTag = "TAG2", meterStart = 200, startTime = Instant.parse("2024-01-01T01:00:00Z"))
        persistAndFlush(txn2)
        txn2.stop(700, Instant.parse("2024-01-01T02:00:00Z"), "Remote", null)
        em.flush()

        val running = em.createQuery("SELECT t FROM Transaction t WHERE t.chargePointId = :cpId AND t.stopTime IS NULL", Transaction::class.java)
            .setParameter("cpId", "CP-1")
            .resultList

        assertEquals(1, running.size)
    }

    @Test
    fun `should return null for non-existent transaction`() {
        val found = em.find(Transaction::class.java, 99999L)
        assertNull(found)
    }
}

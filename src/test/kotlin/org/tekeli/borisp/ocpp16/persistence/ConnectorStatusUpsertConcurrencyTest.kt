package org.tekeli.borisp.ocpp16.persistence

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.UserTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@QuarkusTest
class ConnectorStatusUpsertConcurrencyTest {

    @Inject
    lateinit var em: EntityManager

    @Inject
    lateinit var persistenceService: PersistenceService

    @Inject
    lateinit var userTransaction: UserTransaction

    private val createdChargePointIds = mutableListOf<String>()

    private fun newChargePointId(prefix: String): String {
        val cpId = "$prefix-${UUID.randomUUID().toString().replace("-", "").take(16)}"
        createdChargePointIds += cpId
        return cpId
    }

    @AfterEach
    fun cleanup() {
        userTransaction.begin()
        try {
            createdChargePointIds.forEach { cpId ->
                em.createNativeQuery("DELETE FROM connector_status WHERE charge_point_id = :cpId")
                    .setParameter("cpId", cpId)
                    .executeUpdate()
            }
            userTransaction.commit()
        } catch (e: Exception) {
            userTransaction.rollback()
            throw e
        }
    }

    @Test
    fun `concurrent updateConnectorStatus for same connector keeps exactly one row without exceptions`() {
        val cpId = newChargePointId("CP-CS-CONC")
        val statuses = listOf("Available", "Charging", "Faulted", "Unavailable")

        for (connectorId in 1..3) {
            val threads = 4
            val executor = Executors.newFixedThreadPool(threads)
            val startGate = CountDownLatch(1)
            val doneGate = CountDownLatch(threads)
            val failures = AtomicInteger(0)
            val firstError = AtomicReference<Throwable>()

            repeat(threads) { i ->
                executor.submit {
                    try {
                        startGate.await()
                        persistenceService.updateConnectorStatus(cpId, connectorId, statuses[i], "NoError", null)
                    } catch (t: Throwable) {
                        failures.incrementAndGet()
                        firstError.compareAndSet(null, t)
                    } finally {
                        doneGate.countDown()
                    }
                }
            }

            startGate.countDown()
            assertTrue(doneGate.await(30, TimeUnit.SECONDS), "All threads should complete")
            executor.shutdown()

            assertEquals(0, failures.get(), "No thread should fail for connector $connectorId, but got: ${firstError.get()}")
            val count = em.createNativeQuery(
                "SELECT COUNT(*) FROM connector_status WHERE charge_point_id = :cpId AND connector_id = :connId"
            ).setParameter("cpId", cpId).setParameter("connId", connectorId).singleResult as Long
            assertEquals(1L, count, "Exactly one row must exist for ($cpId, $connectorId)")
            val row = em.createNativeQuery(
                "SELECT status FROM connector_status WHERE charge_point_id = :cpId AND connector_id = :connId"
            ).setParameter("cpId", cpId).setParameter("connId", connectorId).singleResult as String
            assertTrue(statuses.contains(row), "Status $row must be one of the submitted values")
        }
    }

    @Test
    fun `sequential updateConnectorStatus keeps one row with second call values and refreshed timestamp`() {
        val cpId = newChargePointId("CP-CS-SEQ")

        persistenceService.updateConnectorStatus(cpId, 1, "Available", "NoError", "first")
        em.clear()
        val firstTimestamp = Instant.parse(persistenceService.findConnectorStatusesByChargePointId(cpId)[0].timestamp)
        Thread.sleep(20)

        persistenceService.updateConnectorStatus(cpId, 1, "Charging", "NoError", "second")
        em.clear()

        val count = em.createNativeQuery(
            "SELECT COUNT(*) FROM connector_status WHERE charge_point_id = :cpId AND connector_id = :connId"
        ).setParameter("cpId", cpId).setParameter("connId", 1).singleResult as Long
        assertEquals(1L, count, "Exactly one row must exist for ($cpId, 1)")
        val dtos = persistenceService.findConnectorStatusesByChargePointId(cpId)
        assertEquals(1, dtos.size)
        assertEquals("Charging", dtos[0].status)
        assertEquals("NoError", dtos[0].errorCode)
        assertEquals("second", dtos[0].info)
        val secondTimestamp = Instant.parse(dtos[0].timestamp)
        assertTrue(secondTimestamp.isAfter(firstTimestamp), "timestamp must be refreshed on update")
    }
}

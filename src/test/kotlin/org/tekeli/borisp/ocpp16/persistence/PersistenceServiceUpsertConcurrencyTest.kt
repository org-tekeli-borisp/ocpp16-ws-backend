package org.tekeli.borisp.ocpp16.persistence

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.UserTransaction
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference

@QuarkusTest
class PersistenceServiceUpsertConcurrencyTest {

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
                em.createNativeQuery("DELETE FROM charge_points WHERE charge_point_id = :cpId")
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
    fun `concurrent upsertChargePoint for new charge point creates exactly one row without exceptions`() {
        val cpId = newChargePointId("CP-CONC")
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
                    persistenceService.upsertChargePoint(
                        sessionId = "session-conc-$i",
                        chargePointId = cpId,
                        vendor = "V",
                        model = "M",
                        firmwareVersion = null
                    )
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

        assertEquals(0, failures.get(), "No thread should fail, but got: ${firstError.get()}")
        val count = em.createNativeQuery(
            "SELECT COUNT(*) FROM charge_points WHERE charge_point_id = :cpId"
        ).setParameter("cpId", cpId).singleResult as Long
        assertEquals(1L, count, "Exactly one row must exist for $cpId")
        val found = persistenceService.findChargePointById(cpId)
        assertNotNull(found)
        assertEquals(ChargePointStatus.ONLINE, found!!.status)
    }

    @Test
    fun `sequential upsertChargePoint for same new charge point keeps one row with second call values`() {
        val cpId = newChargePointId("CP-SEQ")

        persistenceService.upsertChargePoint("sess-first", cpId, "VendorA", "ModelA", "1.0")
        persistenceService.upsertChargePoint("sess-second", cpId, "VendorB", "ModelB", null)

        val count = em.createNativeQuery(
            "SELECT COUNT(*) FROM charge_points WHERE charge_point_id = :cpId"
        ).setParameter("cpId", cpId).singleResult as Long
        assertEquals(1L, count, "Exactly one row must exist for $cpId")
        val found = persistenceService.findChargePointById(cpId)
        assertNotNull(found)
        assertEquals("sess-second", found!!.sessionId)
        assertEquals("VendorB", found.vendor)
        assertEquals("ModelB", found.model)
        assertNull(found.firmwareVersion)
        assertEquals(ChargePointStatus.ONLINE, found.status)
    }
}

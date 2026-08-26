package org.tekeli.borisp.ocpp16.persistence

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

@QuarkusTest
@jakarta.transaction.Transactional
class PersistenceServiceSurvivingMutantsTest {

    @Inject
    lateinit var em: EntityManager

    @Inject
    lateinit var persistenceService: PersistenceService

    @BeforeEach
    fun cleanup() {
        em.createNativeQuery("DELETE FROM ocpp_message_logs").executeUpdate()
        em.createNativeQuery("DELETE FROM connector_status").executeUpdate()
        em.createNativeQuery("DELETE FROM charge_points").executeUpdate()
        em.flush()
    }

    @Test
    fun `setChargePointOnline updates status and lastSeenAt`() {
        persistenceService.upsertChargePoint("s-mut-1", "CP-MUT-ONLINE", "V", "M", null)
        em.flush()
        persistenceService.setChargePointOffline("s-mut-1")
        em.flush()
        em.clear()

        val before = persistenceService.findChargePointBySessionId("s-mut-1")!!
        assertEquals(ChargePointStatus.OFFLINE, before.status)
        val seenBefore = before.lastSeenAt
        Thread.sleep(10)

        persistenceService.setChargePointOnline("s-mut-1")
        em.flush()
        em.clear()

        val after = persistenceService.findChargePointBySessionId("s-mut-1")!!
        assertEquals(ChargePointStatus.ONLINE, after.status)
        assertNotNull(after.lastSeenAt)
        assertTrue(after.lastSeenAt.isAfter(seenBefore))
    }

    @Test
    fun `updateConnectorStatus replaces previous row for same connector`() {
        persistenceService.updateConnectorStatus("CP-MUT-CS-REPLACE", 1, "AVAILABLE", "NoError", "info-one")
        persistenceService.updateConnectorStatus("CP-MUT-CS-REPLACE", 1, "UNAVAILABLE", "EVDisconnected", null)

        val dtos = persistenceService.findConnectorStatusesByChargePointId("CP-MUT-CS-REPLACE")

        assertEquals(1, dtos.size)
        assertEquals("UNAVAILABLE", dtos[0].status)
        assertEquals("EVDisconnected", dtos[0].errorCode)
        assertNull(dtos[0].info)
    }

    @Test
    fun `findConnectorStatusesByChargePointId maps all dto fields`() {
        persistenceService.updateConnectorStatus("CP-MUT-CS-DTO", 1, "AVAILABLE", "NoError", "info-one")
        persistenceService.updateConnectorStatus("CP-MUT-CS-DTO", 2, "FAULTED", "EVDisconnected", null)

        val dtos = persistenceService.findConnectorStatusesByChargePointId("CP-MUT-CS-DTO")

        assertEquals(2, dtos.size)
        val first = dtos[0]
        assertEquals(1, first.connectorId)
        assertEquals("AVAILABLE", first.status)
        assertEquals("NoError", first.errorCode)
        assertEquals("info-one", first.info)
        assertNotNull(Instant.parse(first.timestamp))
        val second = dtos[1]
        assertEquals(2, second.connectorId)
        assertEquals("FAULTED", second.status)
        assertEquals("EVDisconnected", second.errorCode)
        assertNull(second.info)
        assertNotNull(Instant.parse(second.timestamp))
    }

    @Test
    fun `findMessageLogs applies limit`() {
        for (i in 1..3) {
            persistenceService.createMessageLog("CP-MUT-ML-LIMIT", "CALL", "CALL", "Heartbeat", "id-$i", null)
        }
        em.flush()

        val limited = persistenceService.findMessageLogs("CP-MUT-ML-LIMIT", null, null, 1)
        assertEquals(1, limited.size)

        val all = persistenceService.findMessageLogs("CP-MUT-ML-LIMIT", null, null, 100)
        assertEquals(3, all.size)
    }

    @Test
    fun `findMessageLogs filters by direction`() {
        persistenceService.createMessageLog("CP-MUT-ML-DIR", "CALL", "CALL", "Heartbeat", "id-1", null)
        persistenceService.createMessageLog("CP-MUT-ML-DIR", "CALL", "CALL", "BootNotification", "id-2", null)
        persistenceService.createMessageLog("CP-MUT-ML-DIR", "CALLRESULT", "CALLRESULT", "Heartbeat", "id-3", null)
        em.flush()

        val result = persistenceService.findMessageLogs("CP-MUT-ML-DIR", "CALL", null, 100)

        assertEquals(2, result.size)
        assertTrue(result.all { it.direction == "CALL" })
    }

    @Test
    fun `findMessageLogs filters by action`() {
        persistenceService.createMessageLog("CP-MUT-ML-ACT", "CALL", "CALL", "Heartbeat", "id-1", null)
        persistenceService.createMessageLog("CP-MUT-ML-ACT", "CALL", "CALL", "BootNotification", "id-2", null)
        em.flush()

        val result = persistenceService.findMessageLogs("CP-MUT-ML-ACT", null, "Heartbeat", 100)

        assertEquals(1, result.size)
        assertEquals("Heartbeat", result[0].action)
    }

    @Test
    fun `findMessageLogs filters by direction and action together`() {
        persistenceService.createMessageLog("CP-MUT-ML-BOTH", "CALL", "CALL", "Heartbeat", "id-1", null)
        persistenceService.createMessageLog("CP-MUT-ML-BOTH", "CALL", "CALL", "BootNotification", "id-2", null)
        persistenceService.createMessageLog("CP-MUT-ML-BOTH", "CALLRESULT", "CALLRESULT", "Heartbeat", "id-3", null)
        em.flush()

        val result = persistenceService.findMessageLogs("CP-MUT-ML-BOTH", "CALL", "Heartbeat", 100)

        assertEquals(1, result.size)
        assertEquals("CALL", result[0].direction)
        assertEquals("Heartbeat", result[0].action)
    }

    @Test
    fun `findMessageLogs with null direction returns all directions`() {
        persistenceService.createMessageLog("CP-MUT-ML-NULL", "CALL", "CALL", "Heartbeat", "id-1", null)
        persistenceService.createMessageLog("CP-MUT-ML-NULL", "CALLRESULT", "CALLRESULT", "Heartbeat", "id-2", null)
        em.flush()

        val result = persistenceService.findMessageLogs("CP-MUT-ML-NULL", null, null, 100)

        assertEquals(2, result.size)
    }

    @Test
    fun `findMessageLogs with blank direction returns all directions`() {
        persistenceService.createMessageLog("CP-MUT-ML-BLANK", "CALL", "CALL", "Heartbeat", "id-1", null)
        persistenceService.createMessageLog("CP-MUT-ML-BLANK", "CALLRESULT", "CALLRESULT", "Heartbeat", "id-2", null)
        em.flush()

        val result = persistenceService.findMessageLogs("CP-MUT-ML-BLANK", "", null, 100)

        assertEquals(2, result.size)
    }

    @Test
    fun `purgeMessageLogsBefore deletes only logs before cutoff`() {
        em.persist(
            OcppMessageLog(
                chargePointId = "CP-MUT-PURGE",
                direction = "CALL",
                messageType = "CALL",
                action = "Heartbeat",
                messageId = "old-1",
                timestamp = Instant.parse("2020-01-01T00:00:00Z")
            )
        )
        em.persist(
            OcppMessageLog(
                chargePointId = "CP-MUT-PURGE",
                direction = "CALL",
                messageType = "CALL",
                action = "Heartbeat",
                messageId = "old-2",
                timestamp = Instant.parse("2020-06-01T00:00:00Z")
            )
        )
        em.persist(
            OcppMessageLog(
                chargePointId = "CP-MUT-PURGE",
                direction = "CALL",
                messageType = "CALL",
                action = "Heartbeat",
                messageId = "new-1",
                timestamp = Instant.parse("2025-01-01T00:00:00Z")
            )
        )
        em.persist(
            OcppMessageLog(
                chargePointId = "CP-MUT-PURGE",
                direction = "CALL",
                messageType = "CALL",
                action = "Heartbeat",
                messageId = "new-2",
                timestamp = Instant.parse("2025-06-01T00:00:00Z")
            )
        )
        em.flush()

        val deleted = persistenceService.purgeMessageLogsBefore(Instant.parse("2023-01-01T00:00:00Z"))
        em.clear()

        assertEquals(2, deleted)
        val remaining = persistenceService.findMessageLogs("CP-MUT-PURGE", null, null, 100)
        assertEquals(2, remaining.size)
        assertTrue(remaining.all { it.messageId.startsWith("new-") })
    }
}

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
class SecurityLogRepositoryTest {

    @Inject
    lateinit var em: EntityManager

    @Inject
    lateinit var persistenceService: PersistenceService

    @BeforeEach
    fun cleanup() {
        em.createNativeQuery("DELETE FROM security_logs").executeUpdate()
        em.flush()
    }

    @Test
    fun `should persist and find security log by chargePointId`() {
        val log = persistenceService.createSecurityLog(
            chargePointId = "CP-001",
            type = "FirmwareUpdated",
            timestamp = Instant.parse("2024-01-01T00:00:00Z"),
            techInfo = "Firmware v2.0 installed"
        )

        assertNotNull(log.id)
        assertEquals("CP-001", log.chargePointId)
        assertEquals("FirmwareUpdated", log.type)

        val found = persistenceService.findSecurityLogsByChargePointId("CP-001")
        assertEquals(1, found.size)
        assertEquals("FirmwareUpdated", found[0].type)
    }

    @Test
    fun `should find security logs by type`() {
        persistenceService.createSecurityLog("CP-001", "FirmwareUpdated", Instant.parse("2024-01-01T00:00:00Z"), null)
        persistenceService.createSecurityLog("CP-002", "FirmwareUpdated", Instant.parse("2024-01-01T01:00:00Z"), null)
        persistenceService.createSecurityLog("CP-001", "InvalidTLSVersion", Instant.parse("2024-01-01T02:00:00Z"), null)
        em.flush()

        val firmwareLogs = persistenceService.findSecurityLogsByType("FirmwareUpdated")
        assertEquals(2, firmwareLogs.size)

        val tlsLogs = persistenceService.findSecurityLogsByType("InvalidTLSVersion")
        assertEquals(1, tlsLogs.size)
    }

    @Test
    fun `should return empty list for non-existent chargePointId`() {
        val found = persistenceService.findSecurityLogsByChargePointId("NONEXISTENT")
        assertTrue(found.isEmpty())
    }

    @Test
    fun `should return empty list for non-existent type`() {
        val found = persistenceService.findSecurityLogsByType("NonExistentType")
        assertTrue(found.isEmpty())
    }

    @Test
    fun `should find recent security logs with limit`() {
        persistenceService.createSecurityLog("CP-001", "Type1", Instant.parse("2024-01-01T00:00:00Z"), null)
        persistenceService.createSecurityLog("CP-002", "Type2", Instant.parse("2024-01-01T01:00:00Z"), null)
        persistenceService.createSecurityLog("CP-003", "Type3", Instant.parse("2024-01-01T02:00:00Z"), null)
        em.flush()

        val recent = persistenceService.findRecentSecurityLogs(2)
        assertEquals(2, recent.size)
        assertEquals("Type3", recent[0].type)
    }

    @Test
    fun `should create security log with null techInfo`() {
        val log = persistenceService.createSecurityLog(
            chargePointId = "CP-001",
            type = "FirmwareUpdated",
            timestamp = Instant.parse("2024-01-01T00:00:00Z"),
            techInfo = null
        )

        assertNull(log.techInfo)
    }

    @Test
    fun `should order security logs by timestamp descending`() {
        persistenceService.createSecurityLog("CP-001", "Type1", Instant.parse("2024-01-01T02:00:00Z"), null)
        persistenceService.createSecurityLog("CP-001", "Type2", Instant.parse("2024-01-01T00:00:00Z"), null)
        persistenceService.createSecurityLog("CP-001", "Type3", Instant.parse("2024-01-01T01:00:00Z"), null)
        em.flush()

        val logs = persistenceService.findSecurityLogsByChargePointId("CP-001")
        assertEquals(3, logs.size)
        assertEquals("Type1", logs[0].type)
        assertEquals("Type3", logs[1].type)
        assertEquals("Type2", logs[2].type)
    }

    @Test
    fun `should handle techInfo with max length`() {
        val maxTechInfo = "A".repeat(255)
        val log = persistenceService.createSecurityLog(
            chargePointId = "CP-001",
            type = "TestEvent",
            timestamp = Instant.now(),
            techInfo = maxTechInfo
        )
        assertEquals(255, log.techInfo?.length)
    }

    @Test
    fun `should handle type with max length`() {
        val maxType = "A".repeat(50)
        val log = persistenceService.createSecurityLog(
            chargePointId = "CP-001",
            type = maxType,
            timestamp = Instant.now(),
            techInfo = null
        )
        assertEquals(50, log.type.length)
    }
}

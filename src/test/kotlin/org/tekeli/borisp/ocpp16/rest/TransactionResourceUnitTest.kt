package org.tekeli.borisp.ocpp16.rest

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.persistence.Transaction
import java.time.Instant

class TransactionResourceUnitTest {

    private lateinit var persistence: PersistenceService
    private lateinit var resource: TransactionResource

    @BeforeEach
    fun setup() {
        persistence = mock(PersistenceService::class.java)
        resource = TransactionResource()
        resource.persistenceService = persistence
    }

    private fun stoppedTransaction() = Transaction(
        id = 7L,
        chargePointId = "CP-1",
        connectorId = 1,
        idTag = "TAG-1",
        meterStart = 100,
        startTime = Instant.parse("2024-01-01T00:00:00Z"),
        stopTime = Instant.parse("2024-01-01T01:00:00Z"),
        meterStop = 350,
        stopReason = "Local"
    )

    private fun runningTransaction() = Transaction(
        id = 8L,
        chargePointId = "CP-1",
        connectorId = 2,
        idTag = "TAG-2",
        meterStart = 50,
        startTime = Instant.parse("2024-01-02T00:00:00Z")
    )

    @Test
    fun `getTransactions with default argument returns all transactions as dtos`() {
        `when`(persistence.findAllTransactions("CP-1")).thenReturn(listOf(stoppedTransaction()))

        val result = resource.getTransactions("CP-1")

        assertEquals(1, result.size)
        val dto = result[0]
        assertEquals(7L, dto.id)
        assertEquals("CP-1", dto.chargePointId)
        assertEquals(1, dto.connectorId)
        assertEquals("TAG-1", dto.idTag)
        assertEquals(100, dto.meterStart)
        assertEquals("2024-01-01T00:00:00Z", dto.startTime)
        assertEquals("2024-01-01T01:00:00Z", dto.stopTime)
        assertEquals(350, dto.meterStop)
        assertEquals("Local", dto.stopReason)
        assertEquals(3600L, dto.durationSeconds)
        assertEquals(250, dto.energyWh)
        verify(persistence).findAllTransactions("CP-1")
        verify(persistence, never()).findRunningTransactions(anyString())
    }

    @Test
    fun `getTransactions with running true returns running transactions as dtos`() {
        `when`(persistence.findRunningTransactions("CP-1")).thenReturn(listOf(runningTransaction()))

        val result = resource.getTransactions("CP-1", true)

        assertEquals(1, result.size)
        val dto = result[0]
        assertEquals(8L, dto.id)
        assertEquals("CP-1", dto.chargePointId)
        assertEquals(2, dto.connectorId)
        assertEquals("TAG-2", dto.idTag)
        assertEquals(50, dto.meterStart)
        assertEquals("2024-01-02T00:00:00Z", dto.startTime)
        assertNull(dto.stopTime)
        assertNull(dto.meterStop)
        assertNull(dto.stopReason)
        assertNull(dto.durationSeconds)
        assertNull(dto.energyWh)
        verify(persistence).findRunningTransactions("CP-1")
        verify(persistence, never()).findAllTransactions(anyString())
    }
}

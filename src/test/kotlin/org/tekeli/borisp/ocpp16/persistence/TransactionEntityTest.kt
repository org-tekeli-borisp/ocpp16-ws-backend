package org.tekeli.borisp.ocpp16.persistence

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.Instant

class TransactionEntityTest {

    @Test
    fun `isRunning is true when stopTime is null`() {
        val txn = Transaction(
            chargePointId = "CP-001",
            connectorId = 1,
            idTag = "CARD1",
            meterStart = 100,
            startTime = Instant.parse("2024-01-01T00:00:00Z")
        )

        assertTrue(txn.isRunning)
    }

    @Test
    fun `isRunning is false when stopTime is set`() {
        val txn = Transaction(
            chargePointId = "CP-001",
            connectorId = 1,
            idTag = "CARD1",
            meterStart = 100,
            startTime = Instant.parse("2024-01-01T00:00:00Z")
        )
        txn.stop(200, Instant.parse("2024-01-01T01:00:00Z"), "Local", null)

        assertFalse(txn.isRunning)
    }

    @Test
    fun `durationSeconds is null when not stopped`() {
        val txn = Transaction(
            chargePointId = "CP-001",
            connectorId = 1,
            idTag = "CARD1",
            meterStart = 100,
            startTime = Instant.parse("2024-01-01T00:00:00Z")
        )

        assertNull(txn.durationSeconds)
    }

    @Test
    fun `durationSeconds returns correct value`() {
        val start = Instant.parse("2024-01-01T00:00:00Z")
        val txn = Transaction(
            chargePointId = "CP-001",
            connectorId = 1,
            idTag = "CARD1",
            meterStart = 100,
            startTime = start
        )
        txn.stop(200, start.plusSeconds(3600), "Local", null)

        assertEquals(3600, txn.durationSeconds)
    }

    @Test
    fun `energyWh is null when not stopped`() {
        val txn = Transaction(
            chargePointId = "CP-001",
            connectorId = 1,
            idTag = "CARD1",
            meterStart = 100,
            startTime = Instant.parse("2024-01-01T00:00:00Z")
        )

        assertNull(txn.energyWh)
    }

    @Test
    fun `energyWh returns correct difference`() {
        val txn = Transaction(
            chargePointId = "CP-001",
            connectorId = 1,
            idTag = "CARD1",
            meterStart = 100,
            startTime = Instant.parse("2024-01-01T00:00:00Z")
        )
        txn.stop(500, Instant.parse("2024-01-01T01:00:00Z"), "Local", null)

        assertEquals(400, txn.energyWh)
    }

    @Test
    fun `stop sets all fields correctly`() {
        val stopTime = Instant.parse("2024-01-01T01:00:00Z")
        val txn = Transaction(
            chargePointId = "CP-001",
            connectorId = 1,
            idTag = "CARD1",
            meterStart = 100,
            startTime = Instant.parse("2024-01-01T00:00:00Z")
        )
        txn.stop(500, stopTime, "EmergencyStop", "CARD2")

        assertEquals(500, txn.meterStop)
        assertEquals(stopTime, txn.stopTime)
        assertEquals("EmergencyStop", txn.stopReason)
        assertEquals("CARD2", txn.idTagEnd)
    }

    @Test
    fun `constructor defaults`() {
        val txn = Transaction()

        assertEquals("", txn.chargePointId)
        assertEquals(0, txn.connectorId)
        assertEquals("", txn.idTag)
        assertEquals(0, txn.meterStart)
        assertEquals(Instant.EPOCH, txn.startTime)
        assertNull(txn.stopTime)
        assertNull(txn.meterStop)
    }
}

package org.tekeli.borisp.ocpp16.rest

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.Transaction
import java.time.Instant

class TransactionResourceSurvivingMutantsTest {

    @Test
    fun `toDto maps stopTime for stopped transactions`() {
        val stopTime = Instant.parse("2024-01-01T02:00:00Z")
        val txn = Transaction(
            chargePointId = "CP1",
            connectorId = 1,
            idTag = "CARD1",
            meterStart = 1000,
            startTime = Instant.parse("2024-01-01T00:00:00Z"),
            stopTime = stopTime,
            meterStop = 2000,
            stopReason = "Local"
        )
        val resource = TransactionResource()
        val method = TransactionResource::class.java.getDeclaredMethod("toDto", Transaction::class.java)
        method.isAccessible = true
        val dto = method.invoke(resource, txn) as TransactionDto
        assertNotNull(dto.stopTime)
        assertEquals(stopTime.toString(), dto.stopTime)
    }
}

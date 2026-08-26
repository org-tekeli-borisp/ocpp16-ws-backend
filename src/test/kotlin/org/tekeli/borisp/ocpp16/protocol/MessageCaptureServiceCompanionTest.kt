package org.tekeli.borisp.ocpp16.protocol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MessageCaptureServiceCompanionTest {

    @Test
    fun `default purgeIntervalMillis is 30 minutes`() {
        assertEquals(1_800_000L, MessageCaptureService.purgeIntervalMillis)
    }

    @Test
    fun `purgeIntervalMillis setter stores and getter returns the value`() {
        val original = MessageCaptureService.purgeIntervalMillis
        try {
            MessageCaptureService.purgeIntervalMillis = 42
            assertEquals(42L, MessageCaptureService.purgeIntervalMillis)
        } finally {
            MessageCaptureService.purgeIntervalMillis = original
        }
    }
}

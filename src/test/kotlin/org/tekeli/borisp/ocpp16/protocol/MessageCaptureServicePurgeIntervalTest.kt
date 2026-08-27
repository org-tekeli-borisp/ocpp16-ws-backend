package org.tekeli.borisp.ocpp16.protocol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class MessageCaptureServicePurgeIntervalTest {

    @Test
    fun `default purgeIntervalMillis is 30 minutes`() {
        val svc = MessageCaptureService()
        try {
            assertEquals(1_800_000L, svc.purgeIntervalMillis)
        } finally {
            svc.close()
        }
    }

    @Test
    fun `purgeIntervalMillis setter stores and getter returns the value`() {
        val svc = MessageCaptureService()
        try {
            svc.purgeIntervalMillis = 42
            assertEquals(42L, svc.purgeIntervalMillis)
        } finally {
            svc.close()
        }
    }

    @Test
    fun `purgeIntervalMillis is coerced to at least 1`() {
        val svc = MessageCaptureService()
        try {
            svc.purgeIntervalMillis = 0
            assertEquals(1L, svc.purgeIntervalMillis)
            svc.purgeIntervalMillis = -5
            assertEquals(1L, svc.purgeIntervalMillis)
        } finally {
            svc.close()
        }
    }
}

package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry

class HeartbeatHandlerMutationTest {

    private fun contextWith(persistenceService: PersistenceService?, sessionId: String): OcppHandlerContext {
        return object : OcppHandlerContext {
            override val chargePointId: String = "CP-HB-MUT"
            override val sessionId: String = sessionId
            override var chargePointRegistry: ChargePointRegistry? = null
            override var persistenceService: PersistenceService? = persistenceService
            override var metricsService: MetricsService? = null
            override val heartbeatIntervalSeconds: Long = 300
        }
    }

    private fun withLogCapture(block: () -> Unit): List<String> {
        val captured = mutableListOf<String>()
        val logHandler = object : java.util.logging.Handler() {
            override fun publish(record: java.util.logging.LogRecord) {
                record.message?.let { captured.add(it) }
            }
            override fun flush() {}
            override fun close() {}
        }
        val rootLogger = java.util.logging.Logger.getLogger("")
        rootLogger.addHandler(logHandler)
        try {
            block()
        } finally {
            rootLogger.removeHandler(logHandler)
        }
        return captured
    }

    @Test
    fun `handle calls setChargePointOnline with session id`() {
        val persistence = mock(PersistenceService::class.java)
        val context = contextWith(persistence, "session-hb-mut-1")
        val call = OcppMessage.Call("hb-mut-1", "Heartbeat", emptyMap())

        HeartbeatHandler().handle(call, context)

        verify(persistence).setChargePointOnline("session-hb-mut-1")
    }

    @Test
    fun `handle does not log warning when persistenceService is null`() {
        val context = contextWith(null, "session-hb-mut-2")
        val call = OcppMessage.Call("hb-mut-2", "Heartbeat", emptyMap())

        val captured = withLogCapture {
            HeartbeatHandler().handle(call, context)
        }

        assertFalse(
            captured.any { it.contains("Heartbeat persistence update failed") },
            "No warning must be logged when persistenceService is null, got: $captured"
        )
    }

    @Test
    fun `handle logs exception message when persistence update fails`() {
        val persistence = mock(PersistenceService::class.java)
        doThrow(RuntimeException("hb-persistence-failure-marker"))
            .`when`(persistence).setChargePointOnline(anyString())
        val context = contextWith(persistence, "session-hb-mut-3")
        val call = OcppMessage.Call("hb-mut-3", "Heartbeat", emptyMap())

        val captured = withLogCapture {
            val response = HeartbeatHandler().handle(call, context)
            assertTrue(response.startsWith("[3,"), "Must still return CallResult")
        }

        assertTrue(
            captured.any { it.contains("hb-persistence-failure-marker") },
            "Log must contain exception message, got: $captured"
        )
    }
}

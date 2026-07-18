package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.BootNotificationHandler
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.metrics.MetricsService

class HeartbeatHandlerBranchTest {

    @Test
    fun `handle returns result with currentTime`() {
        val handler = HeartbeatHandler()
        val call = OcppMessage.Call("msg-1", "Heartbeat", emptyMap())
        val context = object : OcppHandlerContext {
            override val chargePointId: String = "CP-001"
            override val sessionId: String = "session-1"
            override var chargePointRegistry: ChargePointRegistry? = null
            override var persistenceService: PersistenceService? = null
            override var metricsService: MetricsService? = null
            override val heartbeatIntervalSeconds: Long = 300
        }

        val response = handler.handle(call, context)

        assertTrue(response.contains("currentTime"))
        assertTrue(response.startsWith("["))
    }
}

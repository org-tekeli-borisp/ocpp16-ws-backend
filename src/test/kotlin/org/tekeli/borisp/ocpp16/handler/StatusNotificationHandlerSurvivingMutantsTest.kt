package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry

class StatusNotificationHandlerSurvivingMutantsTest {

    private fun contextWith(persistenceService: PersistenceService?): OcppHandlerContext {
        return object : OcppHandlerContext {
            override val chargePointId: String = "CP-SN-SM"
            override val sessionId: String = "session-sn-sm"
            override var chargePointRegistry: ChargePointRegistry? = null
            override var persistenceService: PersistenceService? = persistenceService
            override var metricsService: MetricsService? = null
            override val heartbeatIntervalSeconds: Long = 300
        }
    }

    @Test
    fun `persists non-int Number connectorId converted to int`() {
        val persistence = mock(PersistenceService::class.java)
        val context = contextWith(persistence)
        val call = OcppMessage.Call(
            "sn-sm-1",
            "StatusNotification",
            mapOf("connectorId" to 7L, "status" to "Available", "errorCode" to "NoError")
        )

        val response = StatusNotificationHandler().handle(call, context)

        assertTrue(response.startsWith("[3,"))
        verify(persistence).updateConnectorStatus("CP-SN-SM", 7, "Available", "NoError", null)
    }

    @Test
    fun `persists non-string info converted to string`() {
        val persistence = mock(PersistenceService::class.java)
        val context = contextWith(persistence)
        val call = OcppMessage.Call(
            "sn-sm-2",
            "StatusNotification",
            mapOf("connectorId" to 1, "status" to "Available", "errorCode" to "NoError", "info" to 42)
        )

        val response = StatusNotificationHandler().handle(call, context)

        assertTrue(response.startsWith("[3,"))
        verify(persistence).updateConnectorStatus("CP-SN-SM", 1, "Available", "NoError", "42")
    }

    @Test
    fun `persists string info value`() {
        val persistence = mock(PersistenceService::class.java)
        val context = contextWith(persistence)
        val call = OcppMessage.Call(
            "sn-sm-3",
            "StatusNotification",
            mapOf("connectorId" to 1, "status" to "Available", "errorCode" to "NoError", "info" to "some-info")
        )

        val response = StatusNotificationHandler().handle(call, context)

        assertTrue(response.startsWith("[3,"))
        verify(persistence).updateConnectorStatus("CP-SN-SM", 1, "Available", "NoError", "some-info")
    }
}

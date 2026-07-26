package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class BootNotificationHeartbeatIntervalTest {

    private val handler = BootNotificationHandler()

    @Test
    fun `BootNotification response uses heartbeatIntervalSeconds from context`() {
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = null
            override var persistenceService: PersistenceService? = null
            override var heartbeatIntervalSeconds: Long = 0
        }.apply {
            sessionId = "sess-hb-interval"
            chargePointId = "CP-HB"
            heartbeatIntervalSeconds = 600
        }

        val call = OcppMessage.Call(
            messageId = "msg-hb",
            action = "BootNotification",
            payload = mapOf(
                "chargePointVendor" to "VendorA",
                "chargePointModel" to "ModelX"
            )
        )

        val response = handler.handle(call, server)

        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("\"interval\":600"), "interval must be 600 from context")
        assertFalse(response.contains("\"interval\":300"), "interval must NOT be default 300")
    }

    @Test
    fun `BootNotification response uses default 300 when context heartbeatIntervalSeconds is 300`() {
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = null
            override var persistenceService: PersistenceService? = null
            override var heartbeatIntervalSeconds: Long = 0
        }.apply {
            sessionId = "sess-hb-default"
            chargePointId = "CP-HB-DEFAULT"
            heartbeatIntervalSeconds = 300
        }

        val call = OcppMessage.Call(
            messageId = "msg-hb-default",
            action = "BootNotification",
            payload = mapOf(
                "chargePointVendor" to "V",
                "chargePointModel" to "M"
            )
        )

        val response = handler.handle(call, server)

        assertTrue(response.contains("300"), "interval must be 300")
    }
}

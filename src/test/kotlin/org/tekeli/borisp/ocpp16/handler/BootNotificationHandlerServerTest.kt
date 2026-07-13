package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class BootNotificationHandlerServerTest {

    @Test
    fun `BootNotificationHandler throws when chargePointId is null`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = ""
            sessionId = "sess-bn-null"
        }

        val response = server.onTextMessage(
            """[2,"bn-null","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]"""
        )
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("No chargePointId from connection"))
    }

    @Test
    fun `BootNotificationHandler works with null registry`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-BN-NULLREG"
            sessionId = "sess-bn-nullreg"
            chargePointRegistry = null
            persistenceService = null
        }

        val response = server.onTextMessage(
            """[2,"bn-nr","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]"""
        )
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `BootNotificationHandler response contains valid ISO timestamp`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-BN-TS"
            sessionId = "sess-bn-ts"
        }

        val response = server.onTextMessage(
            """[2,"bn-ts","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]"""
        )
        assertTrue(response.contains("T"))
        assertTrue(response.contains("Z") || response.contains("+"))
    }

    @Test
    fun `BootNotificationHandler response contains interval`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-BN-INT"
            sessionId = "sess-bn-int"
        }

        val response = server.onTextMessage(
            """[2,"bn-int","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]"""
        )
        assertTrue(response.contains("\"interval\""))
        assertTrue(response.contains("300"))
    }
}

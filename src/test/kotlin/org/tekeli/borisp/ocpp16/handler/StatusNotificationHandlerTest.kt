package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class StatusNotificationHandlerTest {

    private fun newServer(chargePointId: String): OcppWebSocketServer {
        return OcppWebSocketServer().apply {
            this.chargePointId = chargePointId
        }
    }

    @Test
    fun `StatusNotificationHandler rejects connectorId -1`() {
        val server = newServer("CP-SN")
        val response = server.onTextMessage(
            """[2,"sn-n1","StatusNotification",{"connectorId":-1,"errorCode":"NoError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("connectorId is out of range"))
    }

    @Test
    fun `StatusNotificationHandler accepts connectorId 0`() {
        val server = newServer("CP-SN2")
        val response = server.onTextMessage(
            """[2,"sn-0","StatusNotification",{"connectorId":0,"errorCode":"NoError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `StatusNotificationHandler rejects non-integer connectorId`() {
        val server = newServer("CP-SN3")
        val response = server.onTextMessage(
            """[2,"sn-bad","StatusNotification",{"connectorId":"bad","errorCode":"NoError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[4,"))
    }

    @Test
    fun `StatusNotificationHandler rejects invalid errorCode`() {
        val server = newServer("CP-SN4")
        val response = server.onTextMessage(
            """[2,"sn-ec","StatusNotification",{"connectorId":1,"errorCode":"FakeError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[4,"))
    }

    @Test
    fun `StatusNotificationHandler rejects invalid status`() {
        val server = newServer("CP-SN5")
        val response = server.onTextMessage(
            """[2,"sn-st","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"FakeStatus"}]"""
        )
        assertTrue(response.startsWith("[4,"))
    }

    @Test
    fun `StatusNotificationHandler accepts all valid statuses`() {
        val server = newServer("CP-SN6")
        for (status in listOf("Available", "Preparing", "Charging", "SuspendedEVSE", "SuspendedEV",
            "Finishing", "Reserved", "Unavailable", "Faulted")) {
            val response = server.onTextMessage(
                """[2,"sn-${status}","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"$status"}]"""
            )
            assertTrue(response.startsWith("[3,"), "Should accept status: $status")
        }
    }

    @Test
    fun `StatusNotificationHandler accepts connectorId 1`() {
        val server = newServer("CP-SN-MUT3")
        val response = server.onTextMessage(
            """[2,"sn-mut3","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }
}

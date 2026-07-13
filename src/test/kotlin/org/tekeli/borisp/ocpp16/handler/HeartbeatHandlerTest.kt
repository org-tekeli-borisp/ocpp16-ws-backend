package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class HeartbeatHandlerTest {

    @Test
    fun `HeartbeatHandler works without persistenceService`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-HB-NOPS"
            sessionId = "sess-hb-nops"
            persistenceService = null
        }

        val response = server.onTextMessage("""[2,"hb-nops","Heartbeat",{}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("currentTime"))
    }

    @Test
    fun `HeartbeatHandler returns valid timestamp format`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-HB-TS" }

        val response = server.onTextMessage("""[2,"hb-ts","Heartbeat",{}]""")
        assertTrue(response.contains("T"))
        assertTrue(response.contains(":"))
    }
}

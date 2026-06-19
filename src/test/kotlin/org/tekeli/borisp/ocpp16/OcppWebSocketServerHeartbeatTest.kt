package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class OcppWebSocketServerHeartbeatTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    @Test
    fun `should return Accepted with currentTime for valid Heartbeat`() {
        val response = server.onTextMessage("""[2,"123","Heartbeat",{}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("123"))
        assertTrue(response.contains("currentTime"))
    }
}

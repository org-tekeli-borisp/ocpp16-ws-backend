package org.tekeli.borisp.ocpp16

import io.quarkus.test.junit.QuarkusTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

@QuarkusTest
class OcppWebSocketServerConfigTest {

    @jakarta.inject.Inject
    lateinit var server: OcppWebSocketServer

    @Test
    fun `pong timeout default is 720 seconds`() {
        assertEquals(720, server.pongTimeoutSeconds,
            "pong-timeout-seconds must default to 720 (larger than heartbeat interval)")
    }

    @Test
    fun `ping interval default is 360 seconds`() {
        assertEquals(360, server.pingIntervalSeconds,
            "ping-interval-seconds must default to 360 (heartbeat interval + delta)")
    }

    @Test
    fun `heartbeat interval default is 300 seconds`() {
        assertEquals(300, server.heartbeatIntervalSeconds,
            "heartbeat-interval-seconds must default to 300")
    }
}

package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class MeterValuesHandlerTest {

    private fun newServer(chargePointId: String): OcppWebSocketServer {
        return OcppWebSocketServer().apply {
            this.chargePointId = chargePointId
        }
    }

    @Test
    fun `MeterValuesHandler returns correct result for connectorId 0`() {
        val server = newServer("CP-MV")
        val response = server.onTextMessage(
            """[2,"mv-0","MeterValues",{"connectorId":0,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"100"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `MeterValuesHandler returns correct result for connectorId 1`() {
        val server = newServer("CP-MV2")
        val response = server.onTextMessage(
            """[2,"mv-1","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"100"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `MeterValuesHandler rejects connectorId -1`() {
        val server = newServer("CP-MV-MUT3")
        val response = server.onTextMessage(
            """[2,"mv-mut3","MeterValues",{"connectorId":-1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"100"}]}]}]"""
        )
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("connectorId is out of range"))
    }
}

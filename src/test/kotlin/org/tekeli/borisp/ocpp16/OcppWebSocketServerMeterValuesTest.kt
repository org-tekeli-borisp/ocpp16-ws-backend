package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class OcppWebSocketServerMeterValuesTest {

    private val server = OcppWebSocketServer().apply { chargePointId = "SNH764" }

    @Test
    fun `should return empty CallResult for valid MeterValues`() {
        val response = server.onTextMessage("""[2,"mv-1","MeterValues",{"connectorId":1,"transactionId":123,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000.5","measurand":"Energy.Active.Import.Register"}]}]}]""")
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("mv-1"))
    }

    @Test
    fun `should return FormationViolation for null payload`() {
        val response = server.onTextMessage("""[2,"mv-2","MeterValues",null]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("Payload is null"))
    }

    @Test
    fun `should return FormationViolation for missing connectorId`() {
        val response = server.onTextMessage("""[2,"mv-3","MeterValues",{"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000"}]}]}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("connectorId is required"))
    }

    @Test
    fun `should return FormationViolation for negative connectorId`() {
        val response = server.onTextMessage("""[2,"mv-4","MeterValues",{"connectorId":-1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000"}]}]}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("connectorId is out of range"))
    }

    @Test
    fun `should accept MeterValues with connectorId 0 for main power meter`() {
        val response = server.onTextMessage("""[2,"mv-9","MeterValues",{"connectorId":0,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"1000"}]}]}]""")
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should return FormationViolation for missing meterValue`() {
        val response = server.onTextMessage("""[2,"mv-5","MeterValues",{"connectorId":1}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("meterValue is required"))
    }

    @Test
    fun `should return FormationViolation for empty meterValue array`() {
        val response = server.onTextMessage("""[2,"mv-6","MeterValues",{"connectorId":1,"meterValue":[]}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("meterValue must contain at least 1 element"))
    }

    @Test
    fun `should return FormationViolation for string meterValue instead of array`() {
        val response = server.onTextMessage("""[2,"mv-12","MeterValues",{"connectorId":1,"meterValue":"not an array"}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("meterValue must be an array"))
    }

    @Test
    fun `should return FormationViolation for missing timestamp in meterValue`() {
        val response = server.onTextMessage("""[2,"mv-7","MeterValues",{"connectorId":1,"meterValue":[{"sampledValue":[{"value":"5000"}]}]}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("timestamp is required in meterValue"))
    }

    @Test
    fun `should return FormationViolation for missing sampledValue`() {
        val response = server.onTextMessage("""[2,"mv-8","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z"}]}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("sampledValue is required in meterValue"))
    }

    @Test
    fun `should return FormationViolation for empty sampledValue array`() {
        val response = server.onTextMessage("""[2,"mv-16","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[]}]}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("sampledValue must contain at least 1 element"))
    }

    @Test
    fun `should return FormationViolation for string sampledValue instead of array`() {
        val response = server.onTextMessage("""[2,"mv-13","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":"not an array"}]}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("sampledValue must be an array"))
    }

    @Test
    fun `should return FormationViolation for missing value in sampledValue`() {
        val response = server.onTextMessage("""[2,"mv-15","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"measurand":"Energy"}]}]}]""")
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
        assertTrue(response.contains("value is required in sampledValue"))
    }

    @Test
    fun `should accept MeterValues with multiple sampledValues`() {
        val response = server.onTextMessage("""[2,"mv-10","MeterValues",{"connectorId":1,"transactionId":456,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000","measurand":"Energy.Active.Import.Register"},{"value":"230.5","measurand":"Voltage"},{"value":"16.2","measurand":"Current.Import"}]}]}]""")
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `should accept MeterValues with optional transactionId omitted`() {
        val response = server.onTextMessage("""[2,"mv-11","MeterValues",{"connectorId":0,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"1000"}]}]}]""")
        assertTrue(response.startsWith("[3,"))
    }
}

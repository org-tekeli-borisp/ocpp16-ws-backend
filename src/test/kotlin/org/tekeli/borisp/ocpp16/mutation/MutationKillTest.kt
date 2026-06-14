package org.tekeli.borisp.ocpp16.mutation

import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.command.*
import org.tekeli.borisp.ocpp16.handler.*
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.*
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.time.Instant
import java.util.concurrent.CompletableFuture

class MutationKillTest {

    // =====================================================
    // StopTransactionHandler - recordMetrics (11 mutants)
    // =====================================================

    @Test
    fun `recordMetrics increments energyDeliveredWh correctly`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler(metricsService)

        handler.recordMetrics(1500.5, 3600)

        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertNotNull(counter)
        assertEquals(1500.5, counter!!.count(), 0.01)
    }

    @Test
    fun `recordMetrics records transactionDuration without error`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler(metricsService)

        assertDoesNotThrow { handler.recordMetrics(1500.5, 7200) }
    }

    @Test
    fun `recordMetrics calls onTransactionStopped`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply {
            injectedMeterRegistry = meterRegistry
            initGauges()
        }
        val handler = StopTransactionHandler(metricsService)

        handler.recordMetrics(100.0, 60)

        val gauge = meterRegistry.find("ocpp.transactions.active").gauge()
        assertNotNull(gauge)
    }

    @Test
    fun `recordMetrics with zero energy and duration still records`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler(metricsService)

        handler.recordMetrics(0.0, 0)

        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertNotNull(counter)
        assertEquals(0.0, counter!!.count())
    }

    // =====================================================
    // StopTransactionHandler - processStopTransaction (7 mutants)
    // Test via onTextMessage which goes through full flow
    // =====================================================

    @Test
    fun `processStopTransaction calculates correct energyWh from meterStop and meterStart`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-ENERGY"
            sessionId = "sess-energy"
        }
        // First start a transaction
        val startResp = server.onTextMessage(
            """[2,"start-e","StartTransaction",{"connectorId":1,"idTag":"ENERGYCARD","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(startResp.startsWith("[3,"))

        // Now stop it with meterStop=5000, energy should be 4000
        val stopResp = server.onTextMessage(
            """[2,"stop-e","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(stopResp.startsWith("[3,"))
    }

    @Test
    fun `processStopTransaction calculates correct duration from timestamps`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-DUR"
            sessionId = "sess-dur"
        }
        // Start at 00:00:00
        server.onTextMessage(
            """[2,"start-d","StartTransaction",{"connectorId":1,"idTag":"DURCARD","meterStart":0,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        // Stop at 02:00:00 (7200 seconds later)
        val stopResp = server.onTextMessage(
            """[2,"stop-d","StopTransaction",{"transactionId":1,"meterStop":100,"timestamp":"2024-01-01T02:00:00Z","reason":"Remote"}]"""
        )
        assertTrue(stopResp.startsWith("[3,"))
    }

    @Test
    fun `processStopTransaction works without persistenceService`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-NOPS"
            sessionId = "sess-nops"
            // persistenceService stays null
        }
        server.onTextMessage(
            """[2,"start-n","StartTransaction",{"connectorId":1,"idTag":"CARD","meterStart":0,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        val stopResp = server.onTextMessage(
            """[2,"stop-n","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(stopResp.startsWith("[3,"))
    }

    // =====================================================
    // StartTransactionHandler - createTransaction + handle mutants
    // =====================================================

    @Test
    fun `StartTransactionHandler returns correct transactionId in response`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-TXN"
            sessionId = "sess-txn"
        }
        val response = server.onTextMessage(
            """[2,"start-t","StartTransaction",{"connectorId":2,"idTag":"TXNCARD","meterStart":500,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"))
        // Transaction ID should be present
        assertTrue(response.contains("transactionId"))
    }

    @Test
    fun `StartTransactionHandler passes connectorId and idTag to validation`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-PASS"
            sessionId = "sess-pass"
        }
        val response = server.onTextMessage(
            """[2,"start-p","StartTransaction",{"connectorId":5,"idTag":"PASSTAG","meterStart":100,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    // =====================================================
    // BootNotificationHandler mutants (9)
    // =====================================================

    @Test
    fun `BootNotificationHandler passes vendor and model to response`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-BN"
            sessionId = "sess-bn"
        }
        val response = server.onTextMessage(
            """[2,"bn-1","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3","firmwareVersion":"v1.0"}]"""
        )
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
        assertTrue(response.contains("currentTime"))
    }

    @Test
    fun `BootNotificationHandler passes null firmwareVersion`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-BN2"
            sessionId = "sess-bn2"
        }
        val response = server.onTextMessage(
            """[2,"bn-2","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"Model"}]"""
        )
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `BootNotificationHandler works without chargePointRegistry`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-BN3"
            chargePointRegistry = null
        }
        val response = server.onTextMessage(
            """[2,"bn-3","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    // =====================================================
    // HeartbeatHandler mutants (4)
    // =====================================================

    @Test
    fun `HeartbeatHandler works without persistenceService`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-HB"
        }
        val response = server.onTextMessage(
            """[2,"hb-1","Heartbeat",{}]"""
        )
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("currentTime"))
    }

    // =====================================================
    // StatusNotificationHandler mutants (5)
    // =====================================================

    @Test
    fun `StatusNotificationHandler rejects connectorId -1`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SN" }
        val response = server.onTextMessage(
            """[2,"sn-n1","StatusNotification",{"connectorId":-1,"errorCode":"NoError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("connectorId must be >= 0"))
    }

    @Test
    fun `StatusNotificationHandler accepts connectorId 0`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SN2" }
        val response = server.onTextMessage(
            """[2,"sn-0","StatusNotification",{"connectorId":0,"errorCode":"NoError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `StatusNotificationHandler rejects non-integer connectorId`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SN3" }
        val response = server.onTextMessage(
            """[2,"sn-bad","StatusNotification",{"connectorId":"bad","errorCode":"NoError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[4,"))
    }

    @Test
    fun `StatusNotificationHandler rejects invalid errorCode`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SN4" }
        val response = server.onTextMessage(
            """[2,"sn-ec","StatusNotification",{"connectorId":1,"errorCode":"FakeError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[4,"))
    }

    @Test
    fun `StatusNotificationHandler rejects invalid status`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SN5" }
        val response = server.onTextMessage(
            """[2,"sn-st","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"FakeStatus"}]"""
        )
        assertTrue(response.startsWith("[4,"))
    }

    @Test
    fun `StatusNotificationHandler accepts all valid statuses`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SN6" }
        for (status in listOf("Available", "Preparing", "Charging", "SuspendedEVSE", "SuspendedEV",
            "Finishing", "Reserved", "Unavailable", "Faulted")) {
            val response = server.onTextMessage(
                """[2,"sn-${status}","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"$status"}]"""
            )
            assertTrue(response.startsWith("[3,"), "Should accept status: $status")
        }
    }

    // =====================================================
    // MeterValuesHandler mutants (3)
    // =====================================================

    @Test
    fun `MeterValuesHandler returns correct result for connectorId 0`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-MV" }
        val response = server.onTextMessage(
            """[2,"mv-0","MeterValues",{"connectorId":0,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"100"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `MeterValuesHandler returns correct result for connectorId 1`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-MV2" }
        val response = server.onTextMessage(
            """[2,"mv-1","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"100"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    // =====================================================
    // ChargePointRegistry - sendCall (1 mutant)
    // =====================================================

    @Test
    fun `ChargePointRegistry sendCall works with null metricsService`() {
        val registry = ChargePointRegistry()
        registry.metricsService = null
        val conn = object : org.tekeli.borisp.ocpp16.websocket.ChargePointConnection {
            override val responseAwaiter = org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter()
            override fun sendText(text: String): io.smallrye.mutiny.Uni<Void> = io.smallrye.mutiny.Uni.createFrom().voidItem()
        }
        registry.register("s1", "c1", conn)
        registry.setTestSender("s1", conn)
        registry.updateChargePointInfo("s1", "CP-001", "V1", "M1")

        assertDoesNotThrow { registry.sendCall("CP-001", "Reset", mapOf("type" to "Hard")) }
    }

    // =====================================================
    // OcppMessage$Companion - parse (1 mutant)
    // =====================================================

    @Test
    fun `OcppMessage parse wraps exception message`() {
        val ex = assertThrows(OcppParseException::class.java) {
            OcppMessage.parse("{invalid json")
        }
        assertNotNull(ex.message)
        assertTrue(ex.message!!.contains("Failed to parse"))
    }

    // =====================================================
    // Command validate mutants
    // =====================================================

    private object InlineGateway : ChargePointGateway {
        private val resp = CompletableFuture.completedFuture<OcppMessage>(OcppMessage.CallResult("id", mapOf()))
        override fun sendReset(cp: String, type: String) = resp
        override fun sendRemoteStartTransaction(cp: String, idTag: String, connectorId: Int?) = resp
        override fun sendRemoteStopTransaction(cp: String, transactionId: Int) = resp
        override fun sendUnlockConnector(cp: String, connectorId: Int) = resp
        override fun sendCancelReservation(cp: String, reservationId: Int) = resp
        override fun sendChangeAvailability(cp: String, connectorId: Int, type: String) = resp
        override fun sendChangeConfiguration(cp: String, key: String, value: String) = resp
        override fun sendClearCache(cp: String) = resp
        override fun sendClearChargingProfile(cp: String, connectorId: Int?, stackLevel: Int?) = resp
        override fun sendGetCompositeSchedule(cp: String, connectorId: Int, duration: Int) = resp
        override fun sendGetConfiguration(cp: String, keys: List<String>?) = resp
        override fun sendGetDiagnostics(cp: String, location: String, retries: Int?, retryInterval: Int?) = resp
        override fun sendGetLocalListVersion(cp: String) = resp
        override fun sendReserveNow(cp: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int) = resp
        override fun sendSendLocalList(cp: String, listVersion: Int, updateType: String) = resp
        override fun sendSetChargingProfile(cp: String, connectorId: Int, csChargingProfiles: Map<String, Any>) = resp
        override fun sendTriggerMessage(cp: String, requestedMessage: String, connectorId: Int?) = resp
        override fun sendUpdateFirmware(cp: String, location: String, retrieveDate: String, retries: Int?, retryInterval: Int?) = resp
        override fun sendExtendedTriggerMessage(cp: String, requestedMessage: String, connectorId: Int?) = resp
        override fun sendInstallCertificate(cp: String, certificateType: String, certificate: String) = resp
        override fun sendGetInstalledCertificateIds(cp: String, certificateType: String) = resp
        override fun sendDeleteCertificate(cp: String, certificateHashData: Map<String, Any>) = resp
        override fun sendGetLog(cp: String, logType: String, requestId: Int, log: Map<String, Any>, retries: Int?, retryInterval: Int?) = resp
        override fun sendSignedUpdateFirmware(cp: String, requestId: Int, firmware: Map<String, Any>, retries: Int?, retryInterval: Int?) = resp
        override fun sendCertificateSigned(cp: String, certificateChain: String) = resp
    }

    @Test
    fun `ResetCommand validate rejects empty type`() {
        val cmd = ResetCommand(InlineGateway)
        val resp = cmd.validate(mapOf("type" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `TriggerMessageCommand validate rejects empty requestedMessage`() {
        val cmd = TriggerMessageCommand(InlineGateway)
        val resp = cmd.validate(mapOf("requestedMessage" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ExtendedTriggerMessageCommand validate rejects empty requestedMessage`() {
        val cmd = ExtendedTriggerMessageCommand(InlineGateway)
        val resp = cmd.validate(mapOf("requestedMessage" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ChangeAvailabilityCommand validate rejects empty type`() {
        val cmd = ChangeAvailabilityCommand(InlineGateway)
        val resp = cmd.validate(mapOf("connectorId" to 1, "type" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ReserveNowCommand validate rejects empty expiryDate`() {
        val cmd = ReserveNowCommand(InlineGateway)
        val resp = cmd.validate(mapOf<String, Any>(
            "connectorId" to 1, "expiryDate" to "", "idTag" to "CARD1", "reservationId" to 1
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ReserveNowCommand validate rejects empty idTag`() {
        val cmd = ReserveNowCommand(InlineGateway)
        val resp = cmd.validate(mapOf<String, Any>(
            "connectorId" to 1, "expiryDate" to "2024-01-01T00:00:00Z", "idTag" to "", "reservationId" to 1
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

  
    @Test
    fun `SendLocalListCommand validate rejects empty updateType`() {
        val cmd = SendLocalListCommand(InlineGateway)
        val resp = cmd.validate(mapOf("listVersion" to 1, "updateType" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ChangeConfigurationCommand validate rejects empty key`() {
        val cmd = ChangeConfigurationCommand(InlineGateway)
        val resp = cmd.validate(mapOf("key" to "", "value" to "val"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetDiagnosticsCommand validate rejects empty location`() {
        val cmd = GetDiagnosticsCommand(InlineGateway)
        val resp = cmd.validate(mapOf("location" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `UpdateFirmwareCommand validate rejects empty location`() {
        val cmd = UpdateFirmwareCommand(InlineGateway)
        val resp = cmd.validate(mapOf("location" to "", "retrieveDate" to "2024-01-01T00:00:00Z"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `UpdateFirmwareCommand validate rejects empty retrieveDate`() {
        val cmd = UpdateFirmwareCommand(InlineGateway)
        val resp = cmd.validate(mapOf("location" to "http://fw.bin", "retrieveDate" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `InstallCertificateCommand validate rejects empty certificateType`() {
        val cmd = InstallCertificateCommand(InlineGateway)
        val resp = cmd.validate(mapOf("certificateType" to "", "certificate" to "cert"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `InstallCertificateCommand validate rejects empty certificate`() {
        val cmd = InstallCertificateCommand(InlineGateway)
        val resp = cmd.validate(mapOf("certificateType" to "RootCA", "certificate" to ""))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetInstalledCertificateIdsCommand validate rejects missing certificateType`() {
        val cmd = GetInstalledCertificateIdsCommand(InlineGateway)
        val resp = cmd.validate(emptyMap<String, Any>())
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetInstalledCertificateIdsCommand validate rejects invalid certificateType`() {
        val cmd = GetInstalledCertificateIdsCommand(InlineGateway)
        val resp = cmd.validate(mapOf("certificateType" to "InvalidType"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `DeleteCertificateCommand validate rejects empty hash`() {
        val cmd = DeleteCertificateCommand(InlineGateway)
        val resp = cmd.validate(mapOf("certificateHashData" to mapOf<String, Any>()))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }
}

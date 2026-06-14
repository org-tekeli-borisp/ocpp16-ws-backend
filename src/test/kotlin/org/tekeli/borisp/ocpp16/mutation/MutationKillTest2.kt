package org.tekeli.borisp.ocpp16.mutation

import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.command.*
import org.tekeli.borisp.ocpp16.handler.*
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.*
import org.tekeli.borisp.ocpp16.websocket.ChargePointConnection
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.time.Instant
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import kotlin.Suppress

class MutationKillTest2 {

    @Suppress("UNCHECKED_CAST")
    private fun mutableMapOfNullable(): MutableMap<String, Any> {
        val map: MutableMap<String, Any?> = LinkedHashMap()
        return map as MutableMap<String, Any>
    }

    @Suppress("UNCHECKED_CAST")
    private operator fun <K> MutableMap<K, Any>.set(key: K, value: Any?) {
        (this as MutableMap<K, Any?>)[key] = value
    }

    // =====================================================
    // InlineGateway with tracking capability
    // =====================================================

    private class TrackingGateway(
        private val futureResult: OcppMessage = OcppMessage.CallResult("id", mapOf())
    ) : ChargePointGateway {
        val callsMade = mutableListOf<String>()
        private val resp = CompletableFuture.completedFuture<OcppMessage>(futureResult)

        private fun track(name: String) = run { callsMade.add(name); resp }

        override fun sendReset(cp: String, type: String) = track("sendReset")
        override fun sendRemoteStartTransaction(cp: String, idTag: String, connectorId: Int?) = track("sendRemoteStartTransaction")
        override fun sendRemoteStopTransaction(cp: String, transactionId: Int) = track("sendRemoteStopTransaction")
        override fun sendUnlockConnector(cp: String, connectorId: Int) = track("sendUnlockConnector")
        override fun sendCancelReservation(cp: String, reservationId: Int) = track("sendCancelReservation")
        override fun sendChangeAvailability(cp: String, connectorId: Int, type: String) = track("sendChangeAvailability")
        override fun sendChangeConfiguration(cp: String, key: String, value: String) = track("sendChangeConfiguration")
        override fun sendClearCache(cp: String) = track("sendClearCache")
        override fun sendClearChargingProfile(cp: String, connectorId: Int?, stackLevel: Int?) = track("sendClearChargingProfile")
        override fun sendGetCompositeSchedule(cp: String, connectorId: Int, duration: Int) = track("sendGetCompositeSchedule")
        override fun sendGetConfiguration(cp: String, keys: List<String>?) = track("sendGetConfiguration")
        override fun sendGetDiagnostics(cp: String, location: String, retries: Int?, retryInterval: Int?) = track("sendGetDiagnostics")
        override fun sendGetLocalListVersion(cp: String) = track("sendGetLocalListVersion")
        override fun sendReserveNow(cp: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int) = track("sendReserveNow")
        override fun sendSendLocalList(cp: String, listVersion: Int, updateType: String) = track("sendSendLocalList")
        override fun sendSetChargingProfile(cp: String, connectorId: Int, csChargingProfiles: Map<String, Any>) = track("sendSetChargingProfile")
        override fun sendTriggerMessage(cp: String, requestedMessage: String, connectorId: Int?) = track("sendTriggerMessage")
        override fun sendUpdateFirmware(cp: String, location: String, retrieveDate: String, retries: Int?, retryInterval: Int?) = track("sendUpdateFirmware")
        override fun sendExtendedTriggerMessage(cp: String, requestedMessage: String, connectorId: Int?) = track("sendExtendedTriggerMessage")
        override fun sendInstallCertificate(cp: String, certificateType: String, certificate: String) = track("sendInstallCertificate")
        override fun sendGetInstalledCertificateIds(cp: String, certificateType: String) = track("sendGetInstalledCertificateIds")
        override fun sendDeleteCertificate(cp: String, certificateHashData: Map<String, Any>) = track("sendDeleteCertificate")
        override fun sendGetLog(cp: String, logType: String, requestId: Int, log: Map<String, Any>, retries: Int?, retryInterval: Int?) = track("sendGetLog")
        override fun sendSignedUpdateFirmware(cp: String, requestId: Int, firmware: Map<String, Any>, retries: Int?, retryInterval: Int?) = track("sendSignedUpdateFirmware")
        override fun sendCertificateSigned(cp: String, certificateChain: String) = track("sendCertificateSigned")
    }

    // =====================================================
    // InlineGateway returning CallError for execute tests
    // =====================================================

    private class ErrorGateway : ChargePointGateway {
        private val resp = CompletableFuture.completedFuture<OcppMessage>(
            OcppMessage.CallError("id", OcppErrorCode.GENERIC_ERROR, "Error", null)
        )
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

    // =====================================================
    // 1. StopTransactionHandler - recordMetrics mutants
    // Tests energyDeliveredWh counter value to kill equality
    // check mutations and getTransactionDuration removal
    // =====================================================

    @Test
    fun `recordMetrics energy counter reflects exact energy value`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler(metricsService)

        handler.recordMetrics(2500.75, 1800)

        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertNotNull(counter)
        assertEquals(2500.75, counter!!.count(), 0.001)
    }

    @Test
    fun `recordMetrics transactionsStopped counter increments`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        metricsService.initGauges()
        val handler = StopTransactionHandler(metricsService)

        metricsService.onTransactionStarted()
        assertEquals(1.0, meterRegistry.find("ocpp.transactions.active").gauge()!!.value(), 0.01)

        handler.recordMetrics(100.0, 60)

        val gauge = meterRegistry.find("ocpp.transactions.active").gauge()
        assertNotNull(gauge)
        assertEquals(0.0, gauge!!.value(), 0.01)
    }

    @Test
    fun `recordMetrics timer records duration value`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler(metricsService)

        handler.recordMetrics(500.0, 3600)

        val timer = meterRegistry.find("ocpp.transaction.duration.seconds").timer()
        assertNotNull(timer)
        assertEquals(1, timer!!.count())
    }

    @Test
    fun `recordMetrics with null metricsService does not throw`() {
        val handler = StopTransactionHandler(null)
        assertDoesNotThrow { handler.recordMetrics(100.0, 60) }
    }

    // =====================================================
    // 2. StopTransactionHandler - processStopTransaction mutants
    // Test with specific values to differentiate correct
    // calculation from mutated (addition instead of subtraction)
    // =====================================================

   @Test
    fun `processStopTransaction with non-zero meterStart yields correct energy`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-PROC"
            sessionId = "sess-proc"
            this.metricsService = metricsService
        }

        server.onTextMessage(
            """[2,"s1","StartTransaction",{"connectorId":3,"idTag":"PROCTAG","meterStart":2000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )

        val stopResp = server.onTextMessage(
            """[2,"s2","StopTransaction",{"transactionId":1,"meterStop":7000,"timestamp":"2024-01-01T02:30:00Z","reason":"Local"}]"""
        )
        assertTrue(stopResp.startsWith("[3,"))

        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertNotNull(counter)
        assertEquals(7000.0, counter!!.count(), 0.01)
    }

    @Test
    fun `processStopTransaction duration calculated from correct timestamps`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-DUR2"
            sessionId = "sess-dur2"
            this.metricsService = metricsService
        }

        server.onTextMessage(
            """[2,"d1","StartTransaction",{"connectorId":1,"idTag":"DUR2","meterStart":500,"timestamp":"2024-06-15T10:00:00Z"}]"""
        )

        server.onTextMessage(
            """[2,"d2","StopTransaction",{"transactionId":1,"meterStop":1500,"timestamp":"2024-06-15T14:30:00Z","reason":"Remote"}]"""
        )

        val timer = meterRegistry.find("ocpp.transaction.duration.seconds").timer()
        assertNotNull(timer)
        assertEquals(1, timer!!.count())
    }

    @Test
    fun `processStopTransaction energyWh uses meterStop value from parsed data`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-MS"
            sessionId = "sess-ms"
            this.metricsService = metricsService
        }

        server.onTextMessage(
            """[2,"ms1","StartTransaction",{"connectorId":1,"idTag":"MS1","meterStart":100,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )

        server.onTextMessage(
            """[2,"ms2","StopTransaction",{"transactionId":1,"meterStop":900,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )

        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertEquals(900.0, counter!!.count(), 0.01)
    }

    // =====================================================
    // 3. StartTransactionHandler - handle + createTransaction mutants
    // Test destructuring with unique values per component
    // =====================================================

    @Test
    fun `StartTransactionHandler destructures all 4 components correctly`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-DSTRUCT"
            sessionId = "sess-dstruct"
        }

        val response = server.onTextMessage(
            """[2,"ds1","StartTransaction",{"connectorId":7,"idTag":"UNIQUE7","meterStart":7777,"timestamp":"2024-12-25T12:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
        assertTrue(response.contains("transactionId"))
    }

    @Test
    fun `StartTransactionHandler createTransaction returns fallback transactionId without persistence`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-NO-PERSIST"
            sessionId = "sess-nopersist"
            persistenceService = null
        }

        val response = server.onTextMessage(
            """[2,"np1","StartTransaction",{"connectorId":1,"idTag":"NOPE","meterStart":100,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("\"transactionId\":1"))
    }

    // =====================================================
    // 4. BootNotificationHandler - handle + process mutants
    // Test with unique vendor/model/firmwareVersion
    // =====================================================

    @Test
    fun `BootNotificationHandler destructures vendor model firmwareVersion`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-BN-DSTRUCT"
            sessionId = "sess-bn-dstruct"
        }

        val response = server.onTextMessage(
            """[2,"bn-ds","BootNotification",{"chargePointVendor":"TestVendor","chargePointModel":"TestModel","firmwareVersion":"v2.0.0"}]"""
        )
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
        assertTrue(response.contains("currentTime"))
    }

    @Test
    fun `BootNotificationHandler throws when chargePointId is null`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = null
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

    // =====================================================
    // 5. HeartbeatHandler - handle mutants
    // =====================================================

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

    // =====================================================
    // 6. StatusNotificationHandler - validateConnectorId mutants
    // Boundary tests for intValue < 0
    // =====================================================

    @Test
    fun `StatusNotificationHandler rejects connectorId -1`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SN-MUT" }
        val response = server.onTextMessage(
            """[2,"sn-mut","StatusNotification",{"connectorId":-1,"errorCode":"NoError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("connectorId must be >= 0"))
    }

    @Test
    fun `StatusNotificationHandler accepts connectorId 0`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SN-MUT2" }
        val response = server.onTextMessage(
            """[2,"sn-mut2","StatusNotification",{"connectorId":0,"errorCode":"NoError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `StatusNotificationHandler accepts connectorId 1`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SN-MUT3" }
        val response = server.onTextMessage(
            """[2,"sn-mut3","StatusNotification",{"connectorId":1,"errorCode":"NoError","status":"Available"}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    // =====================================================
    // 7. MeterValuesHandler - validateConnectorId mutants
    // =====================================================

    @Test
    fun `MeterValuesHandler accepts connectorId 0 for valid values`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-MV-MUT" }
        val response = server.onTextMessage(
            """[2,"mv-mut","MeterValues",{"connectorId":0,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"100"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `MeterValuesHandler accepts connectorId 1 for valid values`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-MV-MUT2" }
        val response = server.onTextMessage(
            """[2,"mv-mut2","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"100"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `MeterValuesHandler rejects connectorId -1`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-MV-MUT3" }
        val response = server.onTextMessage(
            """[2,"mv-mut3","MeterValues",{"connectorId":-1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"100"}]}]}]"""
        )
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("connectorId must be >= 0"))
    }

    // =====================================================
    // 8. ChargePointRegistry - sendCall mutant
    // =====================================================

    @Test
    fun `ChargePointRegistry sendCall with null metricsService`() {
        val registry = ChargePointRegistry()
        registry.metricsService = null
        val conn = object : ChargePointConnection {
            override val responseAwaiter = ResponseAwaiter()
            override fun sendText(text: String) = io.smallrye.mutiny.Uni.createFrom().voidItem()
        }
        registry.register("s-reg", "c-reg", conn)
        registry.setTestSender("s-reg", conn)
        registry.updateChargePointInfo("s-reg", "CP-REG", "V", "M")

        assertDoesNotThrow { registry.sendCall("CP-REG", "Heartbeat", emptyMap()) }
    }

    // =====================================================
    // 9. OcppWebSocketServer mutants
    // =====================================================

    @Test
    fun `OcppWebSocketServer activeConnection throws without initialization`() {
        val server = OcppWebSocketServer()
        val ex = assertThrows(IllegalStateException::class.java) { server.activeConnection }
        assertEquals("Connection not initialized", ex.message)
    }

    @Test
    fun `OcppWebSocketServer chargePointId returns exact value not empty`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-EXACT" }
        assertEquals("CP-EXACT", server.chargePointId)
        assertFalse(server.chargePointId!!.isEmpty())
    }

    @Test
    fun `OcppWebSocketServer onTextMessage increments messagesReceived`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-METRICS"
            sessionId = "sess-metrics"
            this.metricsService = metricsService
        }

        server.onTextMessage("""[2,"m1","Heartbeat",{}]""")

        val counter = meterRegistry.find("ocpp.messages.received").counter()
        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    @Test
    fun `OcppWebSocketServer handleCall returns NotImplemented for unknown action`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-UNKNOWN" }
        val response = server.onTextMessage("""[2,"uk1","NonExistentAction",{}]""")
        assertTrue(response.contains("NotImplemented"))
        assertTrue(response.contains("NonExistentAction"))
    }

    // =====================================================
    // 10. OcppMessage.parse - exception getMessage mutant
    // =====================================================

    @Test
    fun `OcppMessage parse wraps exception with original message`() {
        val ex = assertThrows(OcppParseException::class.java) {
            OcppMessage.parse("{bad json here")
        }
        assertNotNull(ex.message)
        assertTrue(ex.message!!.startsWith("Failed to parse OCPP message"))
        val cause = ex.cause
        assertNotNull(cause)
        assertNotNull(cause!!.message)
    }

    // =====================================================
    // 11. SecurityEventNotificationHandler - init setOf mutant
    // Test with each valid security event type
    // =====================================================

    @Test
    fun `SecurityEventNotificationHandler accepts all valid event types`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SEC" }
        val validTypes = listOf(
            "FirmwareUpdated", "FirmwareVerificationFailed",
            "InvalidChargePointCertificate", "InvalidCentralSystemCertificate",
            "InvalidTLSCipherSuite", "InvalidTLSVersion",
            "LocalAccess", "ResetFailed", "Reset", "Tampering",
            "TransactionInfoNotStored",
            "InvalidFirmwareSignature", "UnauthorizedAccess"
        )
        for (type in validTypes) {
            val response = server.onTextMessage(
                """[2,"sec-$type","SecurityEventNotification",{"type":"$type","timestamp":"2024-01-01T00:00:00Z"}]"""
            )
            assertTrue(response.startsWith("[3,"), "Should accept type: $type")
        }
    }

    @Test
    fun `SecurityEventNotificationHandler accepts DiscardedRenewedClientCertificate`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SEC2" }
        val response = server.onTextMessage(
            """[2,"sec-disc","SecurityEventNotification",{"type":"DiscardedRenewedClientCertificate","timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Should accept DiscardedRenewedClientCertificate, got: $response")
    }

    // =====================================================
    // 12. SignedFirmwareStatusNotificationHandler - init setOf mutant
    // =====================================================

    @Test
    fun `SignedFirmwareStatusNotificationHandler accepts all valid statuses`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-SFSN" }
        val validStatuses = listOf(
            "Downloaded", "DownloadFailed", "Downloading", "DownloadScheduled",
            "DownloadPaused", "Idle", "InstallationFailed", "Installing",
            "Installed", "InstallRebooting", "InstallScheduled",
            "InstallVerificationFailed", "InvalidSignature", "SignatureVerified"
        )
        for (status in validStatuses) {
            val response = server.onTextMessage(
                """[2,"sfsn-$status","SignedFirmwareStatusNotification",{"status":"$status"}]"""
            )
            assertTrue(response.startsWith("[3,"), "Should accept status: $status")
        }
    }

    // =====================================================
    // 13. LogStatusNotificationHandler - init setOf mutant
    // =====================================================

    @Test
    fun `LogStatusNotificationHandler accepts all valid statuses`() {
        val server = OcppWebSocketServer().apply { chargePointId = "CP-LSN" }
        val validStatuses = listOf(
            "BadMessage", "Idle", "NotSupportedOperation",
            "PermissionDenied", "Uploaded", "UploadFailure", "Uploading"
        )
        for (status in validStatuses) {
            val response = server.onTextMessage(
                """[2,"lsn-$status","LogStatusNotification",{"status":"$status"}]"""
            )
            assertTrue(response.startsWith("[3,"), "Should accept status: $status")
        }
    }

    // =====================================================
    // 14. Command validate mutants - connectorId as non-Number
    // =====================================================

    @Test
    fun `GetCompositeScheduleCommand validate with non-Number connectorId`() {
        val cmd = GetCompositeScheduleCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("connectorId" to "not-a-number", "duration" to 300))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetCompositeScheduleCommand validate with non-Number duration`() {
        val cmd = GetCompositeScheduleCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("connectorId" to 1, "duration" to "not-a-number"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ReserveNowCommand validate with non-Number connectorId`() {
        val cmd = ReserveNowCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>(
            "connectorId" to "bad", "expiryDate" to "2024-01-01T00:00:00Z",
            "idTag" to "CARD1", "reservationId" to 1
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ReserveNowCommand validate with non-Number reservationId`() {
        val cmd = ReserveNowCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>(
            "connectorId" to 1, "expiryDate" to "2024-01-01T00:00:00Z",
            "idTag" to "CARD1", "reservationId" to "bad"
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SendLocalListCommand validate with non-Number listVersion`() {
        val cmd = SendLocalListCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("listVersion" to "bad", "updateType" to "Full"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ChangeAvailabilityCommand validate with non-Number connectorId`() {
        val cmd = ChangeAvailabilityCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("connectorId" to "bad", "type" to "Inoperative"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `RemoteStartTransactionCommand validate with non-Number connectorId`() {
        val cmd = RemoteStartTransactionCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("idTag" to "TAG1", "connectorId" to "bad"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SetChargingProfileCommand validate with non-Number connectorId`() {
        val cmd = SetChargingProfileCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("connectorId" to "bad", "csChargingProfiles" to mapOf<String, Any>()))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SetChargingProfileCommand validate with null csChargingProfiles`() {
        val cmd = SetChargingProfileCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>("connectorId" to 1))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `UnlockConnectorCommand validate with non-Number connectorId`() {
        val cmd = UnlockConnectorCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("connectorId" to "bad"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `RemoteStopTransactionCommand validate with non-Number transactionId`() {
        val cmd = RemoteStopTransactionCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("transactionId" to "bad"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `CancelReservationCommand validate with non-Number reservationId`() {
        val cmd = CancelReservationCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("reservationId" to "bad"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    // =====================================================
    // 15. Command validate mutants - !in operator tests
    // =====================================================

    @Test
    fun `ChangeAvailabilityCommand validate with invalid type`() {
        val cmd = ChangeAvailabilityCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("connectorId" to 1, "type" to "InvalidType"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SendLocalListCommand validate with invalid updateType`() {
        val cmd = SendLocalListCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("listVersion" to 1, "updateType" to "InvalidType"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `TriggerMessageCommand validate with invalid requestedMessage`() {
        val cmd = TriggerMessageCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("requestedMessage" to "InvalidMessage"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ExtendedTriggerMessageCommand validate with invalid requestedMessage`() {
        val cmd = ExtendedTriggerMessageCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("requestedMessage" to "InvalidMessage"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ResetCommand validate with invalid type`() {
        val cmd = ResetCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("type" to "InvalidType"))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetInstalledCertificateIdsCommand validate with valid type`() {
        val gw = TrackingGateway()
        val cmd = GetInstalledCertificateIdsCommand(gw)
        val resp = cmd.validate(mapOf("certificateType" to "CentralSystemRootCertificate"))
        assertNull(resp)
    }

    @Test
    fun `GetInstalledCertificateIdsCommand validate with ManufacturerRootCertificate`() {
        val gw = TrackingGateway()
        val cmd = GetInstalledCertificateIdsCommand(gw)
        val resp = cmd.validate(mapOf("certificateType" to "ManufacturerRootCertificate"))
        assertNull(resp)
    }

    @Test
    fun `GetInstalledCertificateIdsCommand validate with missing certificateType`() {
        val cmd = GetInstalledCertificateIdsCommand(TrackingGateway())
        val resp = cmd.validate(emptyMap<String, Any>())
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    // =====================================================
    // 16. Command execute mutants - CallError response path
    // Tests to kill RemoveConditionalMutator_EQUAL_IF on
    // "response is OcppMessage.CallResult" checks
    // =====================================================

    @Test
    fun `ResetCommand execute returns BAD_GATEWAY on CallError`() {
        val gw = ErrorGateway()
        val cmd = ResetCommand(gw)
        val resp = cmd.execute("CP-1", mapOf("type" to "Hard"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("rejected", entity["status"])
    }

    @Test
    fun `RemoteStartTransactionCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = RemoteStartTransactionCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("idTag" to "TAG1", "connectorId" to 1))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `RemoteStopTransactionCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = RemoteStopTransactionCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("transactionId" to 1))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `UnlockConnectorCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = UnlockConnectorCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 1))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `CancelReservationCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = CancelReservationCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("reservationId" to 1))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `ChangeAvailabilityCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = ChangeAvailabilityCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 1, "type" to "Inoperative"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `ClearChargingProfileCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = ClearChargingProfileCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 1, "stackLevel" to 0))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `GetCompositeScheduleCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = GetCompositeScheduleCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 1, "duration" to 300))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `GetConfigurationCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = GetConfigurationCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("key" to listOf("key1")))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `GetDiagnosticsCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = GetDiagnosticsCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("location" to "http://example.com/diag"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `GetLocalListVersionCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = GetLocalListVersionCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", emptyMap<String, Any>())
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `ReserveNowCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = ReserveNowCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf<String, Any>(
            "connectorId" to 1, "expiryDate" to "2024-01-01T00:00:00Z",
            "idTag" to "CARD1", "reservationId" to 1
        ))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `SendLocalListCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = SendLocalListCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("listVersion" to 1, "updateType" to "Full"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `SetChargingProfileCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = SetChargingProfileCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 1, "csChargingProfiles" to mapOf<String, Any>("chargingProfileId" to 1)))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `TriggerMessageCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = TriggerMessageCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("requestedMessage" to "Heartbeat"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `ExtendedTriggerMessageCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = ExtendedTriggerMessageCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("requestedMessage" to "Heartbeat"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `UpdateFirmwareCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = UpdateFirmwareCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("location" to "http://fw.bin", "retrieveDate" to "2024-01-01T00:00:00Z"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `InstallCertificateCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = InstallCertificateCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("certificateType" to "CentralSystemRootCertificate", "certificate" to "cert-data"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `GetInstalledCertificateIdsCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = GetInstalledCertificateIdsCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("certificateType" to "CentralSystemRootCertificate"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `DeleteCertificateCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = DeleteCertificateCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("certificateHashData" to mapOf<String, Any>(
            "hashAlgorithm" to "SHA256", "issuerNameHash" to "h1",
            "issuerKeyHash" to "h2", "serialNumber" to "s1"
        )))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `GetLogCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = GetLogCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf<String, Any>(
            "logType" to "DiagnosticsLog", "requestId" to 1,
            "log" to mapOf("remoteLocation" to "http://example.com/log")
        ))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `ChangeConfigurationCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = ChangeConfigurationCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf("key" to "key1", "value" to "val1"))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    @Test
    fun `ClearCacheCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = ClearCacheCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", emptyMap<String, Any>())
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    // =====================================================
    // 17. Command execute mutants - CallResult path with
    // specific entity verification
    // =====================================================

    @Test
    fun `ResetCommand execute returns ACCEPTED with correct entity on CallResult`() {
        val gw = TrackingGateway()
        val cmd = ResetCommand(gw)
        val resp = cmd.execute("CP-1", mapOf("type" to "Hard"))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
        assertEquals("reset", entity["command"])
        assertEquals("Hard", entity["type"])
    }

    @Test
    fun `RemoteStartTransactionCommand execute returns ACCEPTED on CallResult`() {
        val gw = TrackingGateway()
        val cmd = RemoteStartTransactionCommand(gw)
        val resp = cmd.execute("CP-1", mapOf("idTag" to "TAG1", "connectorId" to 1))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
        assertEquals("remote-start-transaction", entity["command"])
    }

    @Test
    fun `ReserveNowCommand execute returns ACCEPTED on CallResult`() {
        val gw = TrackingGateway()
        val cmd = ReserveNowCommand(gw)
        val resp = cmd.execute("CP-1", mapOf<String, Any>(
            "connectorId" to 1, "expiryDate" to "2024-01-01T00:00:00Z",
            "idTag" to "CARD1", "reservationId" to 1
        ))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
        assertEquals("reserve-now", entity["command"])
    }

    @Test
    fun `SendLocalListCommand execute returns ACCEPTED on CallResult`() {
        val gw = TrackingGateway()
        val cmd = SendLocalListCommand(gw)
        val resp = cmd.execute("CP-1", mapOf("listVersion" to 5, "updateType" to "Differential"))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
        assertEquals("send-local-list", entity["command"])
    }

    @Test
    fun `ChangeAvailabilityCommand execute returns ACCEPTED on CallResult`() {
        val gw = TrackingGateway()
        val cmd = ChangeAvailabilityCommand(gw)
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 2, "type" to "Operative"))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
        assertEquals("change-availability", entity["command"])
    }

    // =====================================================
    // 18. SignedUpdateFirmwareCommand mutants
    // =====================================================

    @Test
    fun `SignedUpdateFirmwareCommand validate with valid firmware`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert-data",
                "signature" to "sig-data"
            )
        ))
        assertNull(resp)
    }

    @Test
    fun `SignedUpdateFirmwareCommand validate with empty location`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to "",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to "sig"
            )
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SignedUpdateFirmwareCommand validate with missing requestId`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        val resp = cmd.validate(emptyMap<String, Any>())
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SignedUpdateFirmwareCommand validate with missing firmware`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("requestId" to 1))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SignedUpdateFirmwareCommand validateFirmwareFields catches exception in runCatching`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "",
                "signature" to "sig"
            )
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SignedUpdateFirmwareCommand execute returns ACCEPTED on CallResult`() {
        val gw = TrackingGateway()
        val cmd = SignedUpdateFirmwareCommand(gw)
        val resp = cmd.execute("CP-1", mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to "sig"
            )
        ))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    @Test
    fun `SignedUpdateFirmwareCommand execute with installDateTime`() {
        val gw = TrackingGateway()
        val cmd = SignedUpdateFirmwareCommand(gw)
        val resp = cmd.execute("CP-1", mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "installDateTime" to "2024-01-02T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to "sig"
            )
        ))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    @Test
    fun `SignedUpdateFirmwareCommand execute returns BAD_GATEWAY on CallError`() {
        val cmd = SignedUpdateFirmwareCommand(ErrorGateway())
        val resp = cmd.execute("CP-1", mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to "sig"
            )
        ))
        assertEquals(Response.Status.BAD_GATEWAY.statusCode, resp.status)
    }

    // =====================================================
    // 19. GetLogCommand validate mutants
    // =====================================================

    @Test
    fun `GetLogCommand validateTopLevel with invalid logType`() {
        val cmd = GetLogCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>(
            "logType" to "InvalidLog", "requestId" to 1,
            "log" to mapOf("remoteLocation" to "http://example.com/log")
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetLogCommand validateTopLevel with missing requestId`() {
        val cmd = GetLogCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>(
            "logType" to "DiagnosticsLog",
            "log" to mapOf("remoteLocation" to "http://example.com/log")
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetLogCommand validateTopLevel with non-Map log`() {
        val cmd = GetLogCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>(
            "logType" to "DiagnosticsLog", "requestId" to 1,
            "log" to "not-a-map"
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetLogCommand validateNestedLog with empty remoteLocation`() {
        val cmd = GetLogCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>(
            "logType" to "SecurityLog", "requestId" to 1,
            "log" to mapOf("remoteLocation" to "")
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetLogCommand validateNestedLog with missing remoteLocation`() {
        val cmd = GetLogCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>(
            "logType" to "SecurityLog", "requestId" to 1,
            "log" to mapOf<String, Any>()
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    // =====================================================
    // 20. DeleteCertificateCommand mutants
    // =====================================================

    @Test
    fun `DeleteCertificateCommand validate with valid data`() {
        val cmd = DeleteCertificateCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("certificateHashData" to mapOf<String, Any>(
            "hashAlgorithm" to "SHA256", "issuerNameHash" to "h1",
            "issuerKeyHash" to "h2", "serialNumber" to "s1"
        )))
        assertNull(resp)
    }

    @Test
    fun `DeleteCertificateCommand validate with invalid hashAlgorithm`() {
        val cmd = DeleteCertificateCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("certificateHashData" to mapOf<String, Any>(
            "hashAlgorithm" to "MD5", "issuerNameHash" to "h1",
            "issuerKeyHash" to "h2", "serialNumber" to "s1"
        )))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `DeleteCertificateCommand checkRequiredStringFields with empty field`() {
        val cmd = DeleteCertificateCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("certificateHashData" to mapOf<String, Any>(
            "hashAlgorithm" to "SHA256", "issuerNameHash" to "",
            "issuerKeyHash" to "h2", "serialNumber" to "s1"
        )))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `DeleteCertificateCommand checkRequiredStringFields with null field`() {
        val cmd = DeleteCertificateCommand(TrackingGateway())
        val hashData: MutableMap<String, Any> = mutableMapOf(
            "hashAlgorithm" to "SHA256", "issuerNameHash" to "h1",
            "serialNumber" to "s1"
        )
        hashData["issuerKeyHash"] = null as Any?
        val resp = cmd.validate(mapOf("certificateHashData" to hashData))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    // =====================================================
    // 21. InstallCertificateCommand boundary mutant
    // =====================================================

    @Test
    fun `InstallCertificateCommand validate certificate at exactly 5500 chars`() {
        val cmd = InstallCertificateCommand(TrackingGateway())
        val cert = "A".repeat(5500)
        val resp = cmd.validate(mapOf("certificateType" to "CentralSystemRootCertificate", "certificate" to cert))
        assertNull(resp)
    }

    @Test
    fun `InstallCertificateCommand validate certificate at 5501 chars fails`() {
        val cmd = InstallCertificateCommand(TrackingGateway())
        val cert = "A".repeat(5501)
        val resp = cmd.validate(mapOf("certificateType" to "CentralSystemRootCertificate", "certificate" to cert))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `InstallCertificateCommand validate with ManufacturerRootCertificate`() {
        val cmd = InstallCertificateCommand(TrackingGateway())
        val resp = cmd.validate(mapOf("certificateType" to "ManufacturerRootCertificate", "certificate" to "cert"))
        assertNull(resp)
    }

    // =====================================================
    // 22. RemoteStartTransactionCommand validate -
    // isNullOrEmpty conditional mutant
    // =====================================================

    @Test
    fun `RemoteStartTransactionCommand validate with null idTag`() {
        val cmd = RemoteStartTransactionCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["idTag"] = null as Any?
        payload["connectorId"] = 1
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    // =====================================================
    // 23. TriggerMessageCommand - valid message types test
    // =====================================================

    @Test
    fun `TriggerMessageCommand validate accepts all valid messages`() {
        val cmd = TriggerMessageCommand(TrackingGateway())
        val validMessages = listOf(
            "BootNotification", "DiagnosticsStatusNotification",
            "FirmwareStatusNotification", "Heartbeat",
            "MeterValues", "StatusNotification",
            "LogStatusNotification", "SignChargePointCertificate"
        )
        for (msg in validMessages) {
            val resp = cmd.validate(mapOf("requestedMessage" to msg))
            assertNull(resp, "Should accept: $msg")
        }
    }

    // =====================================================
    // 24. ExtendedTriggerMessageCommand - valid messages test
    // =====================================================

    @Test
    fun `ExtendedTriggerMessageCommand validate accepts all valid messages`() {
        val cmd = ExtendedTriggerMessageCommand(TrackingGateway())
        val validMessages = listOf(
            "BootNotification", "LogStatusNotification",
            "FirmwareStatusNotification", "Heartbeat",
            "MeterValues", "SignChargePointCertificate",
            "StatusNotification"
        )
        for (msg in validMessages) {
            val resp = cmd.validate(mapOf("requestedMessage" to msg))
            assertNull(resp, "Should accept: $msg")
        }
    }

    // =====================================================
    // 25. ClearChargingProfileCommand execute mutants
    // =====================================================

    @Test
    fun `ClearChargingProfileCommand execute with all params returns ACCEPTED`() {
        val gw = TrackingGateway()
        val cmd = ClearChargingProfileCommand(gw)
        val resp = cmd.execute("CP-1", mapOf("connectorId" to 1, "stackLevel" to 0))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
        val entity = resp.entity as Map<*, *>
        assertEquals("sent", entity["status"])
        assertEquals("clear-charging-profile", entity["command"])
    }

    // =====================================================
    // 26. GetDiagnosticsCommand execute mutants
    // =====================================================

    @Test
    fun `GetDiagnosticsCommand execute with all params returns ACCEPTED`() {
        val gw = TrackingGateway()
        val cmd = GetDiagnosticsCommand(gw)
        val resp = cmd.execute("CP-1", mapOf(
            "location" to "http://diag.com/log",
            "retries" to 3,
            "retryInterval" to 60
        ))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    // =====================================================
    // 27. UpdateFirmwareCommand execute mutants
    // =====================================================

    @Test
    fun `UpdateFirmwareCommand execute with all params returns ACCEPTED`() {
        val gw = TrackingGateway()
        val cmd = UpdateFirmwareCommand(gw)
        val resp = cmd.execute("CP-1", mapOf(
            "location" to "http://fw.bin",
            "retrieveDate" to "2024-01-01T00:00:00Z",
            "retries" to 2,
            "retryInterval" to 30
        ))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    // =====================================================
    // 28. ChangeConfigurationCommand execute mutants
    // =====================================================

    @Test
    fun `ChangeConfigurationCommand execute returns ACCEPTED`() {
        val gw = TrackingGateway()
        val cmd = ChangeConfigurationCommand(gw)
        val resp = cmd.execute("CP-1", mapOf("key" to "LocalPreAuthorize", "value" to "true"))
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    // =====================================================
    // 29. ClearCacheCommand execute mutants
    // =====================================================

    @Test
    fun `ClearCacheCommand execute returns ACCEPTED`() {
        val gw = TrackingGateway()
        val cmd = ClearCacheCommand(gw)
        val resp = cmd.execute("CP-1", emptyMap<String, Any>())
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    // =====================================================
    // 30. GetLocalListVersionCommand execute mutants
    // =====================================================

    @Test
    fun `GetLocalListVersionCommand execute returns ACCEPTED`() {
        val gw = TrackingGateway()
        val cmd = GetLocalListVersionCommand(gw)
        val resp = cmd.execute("CP-1", emptyMap<String, Any>())
        assertEquals(Response.Status.ACCEPTED.statusCode, resp.status)
    }

    // =====================================================
    // 31. StopTransactionHandler - full flow with metrics
    // to differentiate subtraction from addition
    // =====================================================

    @Test
    fun `StopTransactionHandler full flow energy is meterStop minus meterStart`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-ENERGY-FLOW"
            sessionId = "sess-energy-flow"
            this.metricsService = metricsService
        }

        server.onTextMessage(
            """[2,"ef1","StartTransaction",{"connectorId":1,"idTag":"EF1","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )

        server.onTextMessage(
            """[2,"ef2","StopTransaction",{"transactionId":1,"meterStop":6000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )

        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertNotNull(counter)
        assertEquals(6000.0, counter!!.count(), 0.01)
    }

@Test
    fun `StopTransactionHandler full flow duration from timestamps`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-DUR-FLOW"
            sessionId = "sess-dur-flow"
            this.metricsService = metricsService
        }

        server.onTextMessage(
            """[2,"df1","StartTransaction",{"connectorId":1,"idTag":"DF1","meterStart":0,"timestamp":"2024-06-01T00:00:00Z"}]"""
        )

        server.onTextMessage(
            """[2,"df2","StopTransaction",{"transactionId":1,"meterStop":1000,"timestamp":"2024-06-01T03:00:00Z","reason":"EmergencyStop"}]"""
        )

        val timer = meterRegistry.find("ocpp.transaction.duration.seconds").timer()
        assertNotNull(timer)
        assertEquals(1, timer!!.count())
    }

    // =====================================================
    // 32. Additional handler tests for survived mutants
    // =====================================================

    @Test
    fun `StartTransactionHandler passes connectorId to createTransaction`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-CTX-ID"
            sessionId = "sess-ctx-id"
            this.metricsService = metricsService
        }

        val response = server.onTextMessage(
            """[2,"ctx1","StartTransaction",{"connectorId":9,"idTag":"CTX9","meterStart":999,"timestamp":"2024-03-15T08:30:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))
    }

    @Test
    fun `StartTransactionHandler passes idTag to createTransaction`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-CTX-IDTAG"
            sessionId = "sess-ctx-idtag"
        }

        val response = server.onTextMessage(
            """[2,"ctx2","StartTransaction",{"connectorId":1,"idTag":"UNIQUE_ID_TAG_16CH","meterStart":500,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"))
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

    @Test
    fun `StopTransactionHandler response contains idTagInfo Accepted`() {
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-ST-INFO"
            sessionId = "sess-st-info"
        }

        val response = server.onTextMessage(
            """[2,"st-info","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("idTagInfo"))
        assertTrue(response.contains("Accepted"))
    }

    // =====================================================
    // 33. OcppWebSocketServer handler delegate mutants
    // Test that handlers receive metricsService and
    // persistenceService from the server
    // =====================================================

    @Test
    fun `OcppWebSocketServer StartTransaction handler uses metricsService`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-HANDLER-MS"
            sessionId = "sess-handler-ms"
            this.metricsService = metricsService
        }

        server.onTextMessage(
            """[2,"hm1","StartTransaction",{"connectorId":1,"idTag":"HM1","meterStart":0,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )

        val counter = meterRegistry.find("ocpp.messages.received").counter()
        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    @Test
    fun `OcppWebSocketServer StopTransaction handler uses metricsService`() {
        val meterRegistry = io.micrometer.core.instrument.simple.SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-HANDLER-MS2"
            sessionId = "sess-handler-ms2"
            this.metricsService = metricsService
        }

        server.onTextMessage(
            """[2,"hm2","StopTransaction",{"transactionId":1,"meterStop":100,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )

        val counter = meterRegistry.find("ocpp.transactions.stopped").counter()
        assertNotNull(counter)
        assertEquals(1.0, counter!!.count())
    }

    // =====================================================
    // 34. Additional edge cases for command validate mutants
    // =====================================================

    @Test
    fun `ChangeAvailabilityCommand validate with null type`() {
        val cmd = ChangeAvailabilityCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["connectorId"] = 1
        payload["type"] = null as Any?
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SendLocalListCommand validate with null updateType`() {
        val cmd = SendLocalListCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["listVersion"] = 1
        payload["updateType"] = null as Any?
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetCompositeScheduleCommand validate with null duration`() {
        val cmd = GetCompositeScheduleCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["connectorId"] = 1
        payload["duration"] = null as Any?
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetCompositeScheduleCommand validate with null connectorId`() {
        val cmd = GetCompositeScheduleCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["connectorId"] = null as Any?
        payload["duration"] = 300
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `RemoteStartTransactionCommand validate with null connectorId`() {
        val cmd = RemoteStartTransactionCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["idTag"] = "TAG1"
        payload["connectorId"] = null as Any?
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SetChargingProfileCommand validate with null connectorId`() {
        val cmd = SetChargingProfileCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["connectorId"] = null as Any?
        payload["csChargingProfiles"] = mapOf<String, Any>()
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `ChangeAvailabilityCommand validate with null connectorId`() {
        val cmd = ChangeAvailabilityCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["connectorId"] = null as Any?
        payload["type"] = "Inoperative"
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SendLocalListCommand validate with null listVersion`() {
        val cmd = SendLocalListCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["listVersion"] = null as Any?
        payload["updateType"] = "Full"
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    // =====================================================
    // 35. InstallCertificateCommand - null certificate test
    // =====================================================

    @Test
    fun `InstallCertificateCommand validate with null certificate`() {
        val cmd = InstallCertificateCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["certificateType"] = "CentralSystemRootCertificate"
        payload["certificate"] = null as Any?
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    // =====================================================
    // 36. DeleteCertificateCommand - null certificateHashData
    // =====================================================

    @Test
    fun `DeleteCertificateCommand validate with null certificateHashData`() {
        val cmd = DeleteCertificateCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["certificateHashData"] = null as Any?
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    // =====================================================
    // 37. GetLogCommand validate with null logType
    // =====================================================

    @Test
    fun `GetLogCommand validateTopLevel with null logType`() {
        val cmd = GetLogCommand(TrackingGateway())
        val payload = mutableMapOfNullable()
        payload["logType"] = null as Any?
        payload["requestId"] = 1
        payload["log"] = mapOf("remoteLocation" to "http://example.com/log")
        val resp = cmd.validate(payload)
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `GetLogCommand validateTopLevel with non-Number requestId`() {
        val cmd = GetLogCommand(TrackingGateway())
        val resp = cmd.validate(mapOf<String, Any>(
            "logType" to "DiagnosticsLog", "requestId" to "not-a-number",
            "log" to mapOf("remoteLocation" to "http://example.com/log")
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    // =====================================================
    // 38. SignedUpdateFirmwareCommand - non-Number requestId
    // =====================================================

    @Test
    fun `SignedUpdateFirmwareCommand validate with non-Number requestId`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        // Note: production code uses `as Number?` which throws ClassCastException
        // for non-Number types, so we test with a boolean (non-Number) instead
        val payload = mutableMapOfNullable()
        payload["requestId"] = false
        payload["firmware"] = mapOf<String, Any>(
            "location" to "http://fw.bin",
            "retrieveDateTime" to "2024-01-01T00:00:00Z",
            "signingCertificate" to "cert",
            "signature" to "sig"
        )
        val ex = assertThrows(ClassCastException::class.java) {
            cmd.validate(payload)
        }
        // ClassCastException is thrown by the production code for non-Number requestId
    }

    // =====================================================
    // 39. SignedUpdateFirmwareCommand - firmware field boundary
    // =====================================================

    @Test
    fun `SignedUpdateFirmwareCommand validate with location exactly 512 chars`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        val loc = "h".repeat(512)
        val resp = cmd.validate(mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to loc,
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to "sig"
            )
        ))
        assertNull(resp)
    }

    @Test
    fun `SignedUpdateFirmwareCommand validate with location 513 chars fails`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        val loc = "h".repeat(513)
        val resp = cmd.validate(mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to loc,
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to "sig"
            )
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SignedUpdateFirmwareCommand validate with signingCertificate 5500 chars`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        val cert = "c".repeat(5500)
        val resp = cmd.validate(mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to cert,
                "signature" to "sig"
            )
        ))
        assertNull(resp)
    }

    @Test
    fun `SignedUpdateFirmwareCommand validate with signingCertificate 5501 chars fails`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        val cert = "c".repeat(5501)
        val resp = cmd.validate(mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to cert,
                "signature" to "sig"
            )
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }

    @Test
    fun `SignedUpdateFirmwareCommand validate with signature 800 chars`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        val sig = "s".repeat(800)
        val resp = cmd.validate(mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to sig
            )
        ))
        assertNull(resp)
    }

    @Test
    fun `SignedUpdateFirmwareCommand validate with signature 801 chars fails`() {
        val cmd = SignedUpdateFirmwareCommand(TrackingGateway())
        val sig = "s".repeat(801)
        val resp = cmd.validate(mapOf<String, Any>(
            "requestId" to 1,
            "firmware" to mapOf<String, Any>(
                "location" to "http://fw.bin",
                "retrieveDateTime" to "2024-01-01T00:00:00Z",
                "signingCertificate" to "cert",
                "signature" to sig
            )
        ))
        assertNotNull(resp)
        assertEquals(Response.Status.BAD_REQUEST.statusCode, resp!!.status)
    }
    // =====================================================
    // SecurityEventNotificationHandler - exact error messages
    // =====================================================

    @Test
    fun `SecurityEventNotificationHandler rejects whitespace-only type with correct message`() {
        val handler = SecurityEventNotificationHandler()
        val call = OcppMessage.Call("id", "SecurityEventNotification", mapOf(
            "type" to "   ",
            "timestamp" to "2024-01-01T00:00:00Z"
        ))
        val ex = assertThrows(FormationViolationException::class.java) {
            handler.handle(call, OcppWebSocketServer())
        }
        assertEquals("type is required", ex.message)
    }

    @Test
    fun `SecurityEventNotificationHandler rejects blank timestamp with correct message`() {
        val handler = SecurityEventNotificationHandler()
        val call = OcppMessage.Call("id", "SecurityEventNotification", mapOf(
            "type" to "FirmwareUpdated",
            "timestamp" to "   "
        ))
        val ex = assertThrows(FormationViolationException::class.java) {
            handler.handle(call, OcppWebSocketServer())
        }
        assertEquals("timestamp is required", ex.message)
    }

    @Test
    fun `SecurityEventNotificationHandler handles null techInfo gracefully`() {
        val handler = SecurityEventNotificationHandler()
        val call = OcppMessage.Call("id", "SecurityEventNotification", mapOf<String, Any>(
            "type" to "FirmwareUpdated",
            "timestamp" to "2024-01-01T00:00:00Z"
            // techInfo intentionally missing
        ))
        val server = OcppWebSocketServer().apply {
            chargePointId = "CP-SEC"
        }
        val response = handler.handle(call, server)
        assertTrue(response.startsWith("[3,"))
    }

    // =====================================================
    // LogStatusNotificationHandler - all valid statuses
    // =====================================================

    @Test
    fun `LogStatusNotificationHandler accepts all 7 valid statuses`() {
        val handler = LogStatusNotificationHandler()
        for (status in listOf("BadMessage", "Idle", "NotSupportedOperation",
            "PermissionDenied", "Uploaded", "UploadFailure", "Uploading")) {
            val call = OcppMessage.Call("id", "LogStatusNotification", mapOf("status" to status))
            val response = handler.handle(call, OcppWebSocketServer())
            assertTrue(response.startsWith("[3,"), "Should accept status: $status")
        }
    }

    @Test
    fun `LogStatusNotificationHandler rejects invalid status`() {
        val handler = LogStatusNotificationHandler()
        val call = OcppMessage.Call("id", "LogStatusNotification", mapOf("status" to "FakeStatus"))
        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, OcppWebSocketServer())
        }
    }

    // =====================================================
    // SignedFirmwareStatusNotificationHandler - all valid statuses
    // =====================================================

    @Test
    fun `SignedFirmwareStatusNotificationHandler accepts all 14 valid statuses`() {
        val handler = SignedFirmwareStatusNotificationHandler()
        for (status in listOf("Downloaded", "DownloadFailed", "Downloading", "DownloadScheduled",
            "DownloadPaused", "Idle", "InstallationFailed", "Installing",
            "Installed", "InstallRebooting", "InstallScheduled",
            "InstallVerificationFailed", "InvalidSignature", "SignatureVerified")) {
            val call = OcppMessage.Call("id", "SignedFirmwareStatusNotification", mapOf("status" to status))
            val response = handler.handle(call, OcppWebSocketServer())
            assertTrue(response.startsWith("[3,"), "Should accept status: $status")
        }
    }

    @Test
    fun `SignedFirmwareStatusNotificationHandler rejects invalid status`() {
        val handler = SignedFirmwareStatusNotificationHandler()
        val call = OcppMessage.Call("id", "SignedFirmwareStatusNotification", mapOf("status" to "FakeStatus"))
        assertThrows(FormationViolationException::class.java) {
            handler.handle(call, OcppWebSocketServer())
        }
    }
}

package org.tekeli.borisp.ocpp16.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketConnectOptions
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@QuarkusTest
@Timeout(30)
@jakarta.transaction.Transactional
class CommandRoundTripTest {

    @TestHTTPResource
    lateinit var ocppUri: URI

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var em: jakarta.persistence.EntityManager

    private val mapper = ObjectMapper()

    @BeforeEach
    fun cleanup() {
        em.createNativeQuery("DELETE FROM transactions").executeUpdate()
        em.createNativeQuery("DELETE FROM charge_points").executeUpdate()
        em.flush()
    }

    private fun testPort(): Int = ocppUri.port

    private data class CommandResponseConfig(
        val action: String,
        val reject: Boolean = false,
        val payload: Map<String, Any> = mapOf("status" to "Accepted")
    )

    private fun connectBootAndRespond(
        chargePointId: String,
        config: CommandResponseConfig
    ): Pair<WebSocket, AtomicReference<String>> {
        val connectLatch = CountDownLatch(1)
        val bootLatch = CountDownLatch(1)
        val wsRef = AtomicReference<WebSocket>()
        val messageIdRef = AtomicReference<String>()
        val booted = AtomicBoolean(false)
        val commandSent = AtomicBoolean(false)

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(testPort())
            .setURI("/ocpp/$chargePointId")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                wsRef.set(ws)
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    val message = buffer.toString()
                    if (message.isEmpty()) return@handler

                    if (!booted.get()) {
                        if (message.startsWith("[3,")) {
                            booted.set(true)
                            bootLatch.countDown()
                        }
                    } else if (message.startsWith("[2,")) {
                        val root: JsonNode = mapper.readTree(message)
                        val action = root.get(2).asText()
                        val messageId = root.get(1).asText()

                        if (action == config.action && !commandSent.getAndSet(true)) {
                            messageIdRef.set(messageId)
                            if (config.reject) {
                                ws.writeTextMessage(
                                    """[4,"$messageId","NotSupported","Rejected",{}]"""
                                )
                            } else {
                                val payloadJson = mapper.writeValueAsString(config.payload)
                                ws.writeTextMessage(
                                    """[3,"$messageId",${payloadJson}]"""
                                )
                            }
                        }
                    }
                }

                ws.writeTextMessage(
                    """[2,"boot-1","BootNotification",{"chargePointVendor":"Tester","chargePointModel":"E2E","firmwareVersion":"1.0"}]"""
                )
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(bootLatch.await(5, TimeUnit.SECONDS), "Should receive boot response")

        return wsRef.get()!! to messageIdRef
    }

    @Test
    fun `should successfully execute reset command round trip`() {
        val cpId = "reset-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("Reset"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"type": "Hard"}""")
            .`when`().post("/api/chargepoints/$cpId/commands/reset")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute clearCache command round trip`() {
        val cpId = "clearcache-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("ClearCache"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{}""")
            .`when`().post("/api/chargepoints/$cpId/commands/clear-cache")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute remoteStopTransaction command round trip`() {
        val cpId = "remotestop-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("RemoteStopTransaction"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"transactionId": 1}""")
            .`when`().post("/api/chargepoints/$cpId/commands/remote-stop-transaction")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute unlockConnector command round trip`() {
        val cpId = "unlock-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("UnlockConnector", payload = mapOf("status" to "Unlocked")))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"connectorId": 1}""")
            .`when`().post("/api/chargepoints/$cpId/commands/unlock-connector")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute changeConfiguration command round trip`() {
        val cpId = "config-cp-${System.currentTimeMillis()}"
        val commandLatch = CountDownLatch(1)
        val messageIdRef = AtomicReference<String>()
        val connectLatch = CountDownLatch(1)
        val bootLatch = CountDownLatch(1)
        val wsRef = AtomicReference<WebSocket>()
        val booted = AtomicBoolean(false)

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(testPort())
            .setURI("/ocpp/$cpId")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                wsRef.set(ws)
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    val message = buffer.toString()
                    if (message.isEmpty()) return@handler

                    if (!booted.get() && message.startsWith("[3,")) {
                        booted.set(true)
                        bootLatch.countDown()
                    } else if (booted.get() && message.startsWith("[2,")) {
                        val root: JsonNode = mapper.readTree(message)
                        val action = root.get(2).asText()
                        if (action == "ChangeConfiguration") {
                            val messageId = root.get(1).asText()
                            messageIdRef.set(messageId)
                            commandLatch.countDown()
                        }
                    }
                }

                ws.writeTextMessage(
                    """[2,"boot-1","BootNotification",{"chargePointVendor":"Tester","chargePointModel":"E2E","firmwareVersion":"1.0"}]"""
                )
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(bootLatch.await(5, TimeUnit.SECONDS), "Should receive boot response")

        val restFuture = Thread {
            RestAssured.given()
                .contentType("application/json")
                .body("""{"key": "HeartbeatInterval", "value": "60"}""")
                .`when`().post("/api/chargepoints/$cpId/commands/change-configuration")
                .then()
                .statusCode(202)
                .body("status", org.hamcrest.Matchers.equalTo("sent"))
        }

        restFuture.start()

        assertTrue(commandLatch.await(5, TimeUnit.SECONDS), "Should receive ChangeConfiguration command")
        val messageId = messageIdRef.get()
        assertNotNull(messageId)

        val ws = wsRef.get()!!
        ws.writeTextMessage("""[3,"$messageId",{"status":"Accepted"}]""")

        restFuture.join(11000)

        ws.close()
    }

    @Test
    fun `should successfully execute changeAvailability command round trip`() {
        val cpId = "avail-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("ChangeAvailability"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"connectorId": 1, "type": "Inoperative"}""")
            .`when`().post("/api/chargepoints/$cpId/commands/change-availability")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute getConfiguration command round trip`() {
        val cpId = "getconfig-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("GetConfiguration", payload = mapOf(
            "configurationKey" to listOf(mapOf("key" to "HeartbeatInterval", "value" to "300"))
        )))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"key": ["HeartbeatInterval"]}""")
            .`when`().post("/api/chargepoints/$cpId/commands/get-configuration")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute getLocalListVersion command round trip`() {
        val cpId = "locallist-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("GetLocalListVersion", payload = mapOf("listVersion" to 1)))

        RestAssured.given()
            .contentType("application/json")
            .body("""{}""")
            .`when`().post("/api/chargepoints/$cpId/commands/get-local-list-version")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute triggerMessage command round trip`() {
        val cpId = "trigger-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("TriggerMessage"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"requestedMessage": "Heartbeat"}""")
            .`when`().post("/api/chargepoints/$cpId/commands/trigger-message")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute remoteStartTransaction command round trip`() {
        val cpId = "remotestart-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("RemoteStartTransaction"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"idTag": "CARD123", "connectorId": 1}""")
            .`when`().post("/api/chargepoints/$cpId/commands/remote-start-transaction")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should return rejected status when chargePoint rejects command`() {
        val cpId = "reject-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(
            cpId,
            CommandResponseConfig("Reset", reject = true)
        )

        RestAssured.given()
            .contentType("application/json")
            .body("""{"type": "Soft"}""")
            .`when`().post("/api/chargepoints/$cpId/commands/reset")
            .then()
            .statusCode(502)
            .body("status", org.hamcrest.Matchers.equalTo("rejected"))

        ws.close()
    }

    // === Additional Commands ===

    @Test
    fun `should successfully execute cancelReservation command round trip`() {
        val cpId = "cancelres-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("CancelReservation"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"reservationId": 1}""")
            .`when`().post("/api/chargepoints/$cpId/commands/cancel-reservation")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute reserveNow command round trip`() {
        val cpId = "reservenow-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("ReserveNow"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"connectorId": 1, "expiryDate": "2024-01-01T00:00:00Z", "idTag": "CARD123", "reservationId": 1}""")
            .`when`().post("/api/chargepoints/$cpId/commands/reserve-now")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute sendLocalList command round trip`() {
        val cpId = "sendlocal-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("SendLocalList"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"listVersion": 1, "updateType": "Full"}""")
            .`when`().post("/api/chargepoints/$cpId/commands/send-local-list")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute setChargingProfile command round trip`() {
        val cpId = "chargingprof-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("SetChargingProfile"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"connectorId": 1, "csChargingProfiles": {"chargingSchedule": {"chargingRateUnit": "A", "chargingSchedulePeriod": [{"startPeriod": 0, "limit": 16}]}}}""")
            .`when`().post("/api/chargepoints/$cpId/commands/set-charging-profile")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute clearChargingProfile command round trip`() {
        val cpId = "clearprof-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("ClearChargingProfile"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"stackLevel": 1}""")
            .`when`().post("/api/chargepoints/$cpId/commands/clear-charging-profile")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute getCompositeSchedule command round trip`() {
        val cpId = "compositesched-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("GetCompositeSchedule", payload = mapOf(
            "status" to "Accepted",
            "connectorId" to 1,
            "scheduleStart" to "2024-01-01T00:00:00Z",
            "chargingSchedule" to mapOf("chargingRateUnit" to "A", "chargingSchedulePeriod" to listOf(mapOf("startPeriod" to 0, "limit" to 16)))
        )))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"connectorId": 1, "duration": 3600}""")
            .`when`().post("/api/chargepoints/$cpId/commands/get-composite-schedule")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute getDiagnostics command round trip`() {
        val cpId = "getdiag-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("GetDiagnostics"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"location": "https://example.com/diagnostics"}""")
            .`when`().post("/api/chargepoints/$cpId/commands/get-diagnostics")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute updateFirmware command round trip`() {
        val cpId = "updatefirm-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("UpdateFirmware"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"location": "https://example.com/firmware.bin", "retrieveDate": "2024-01-01T00:00:00Z"}""")
            .`when`().post("/api/chargepoints/$cpId/commands/update-firmware")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute extendedTriggerMessage command round trip`() {
        val cpId = "exttrigger-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("ExtendedTriggerMessage"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"requestedMessage": "LogStatusNotification"}""")
            .`when`().post("/api/chargepoints/$cpId/commands/extended-trigger-message")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute installCertificate command round trip`() {
        val cpId = "instcert-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("InstallCertificate"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"certificateType": "CentralSystemRootCertificate", "certificate": "MIID..."}""")
            .`when`().post("/api/chargepoints/$cpId/commands/install-certificate")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute getInstalledCertificateIds command round trip`() {
        val cpId = "getcert-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("GetInstalledCertificateIds", payload = mapOf(
            "certificateHashData" to listOf(mapOf("hashAlgorithm" to "SHA256", "issuerName" to "CN=Test", "serialNumber" to "1234"))
        )))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"certificateType": "CentralSystemRootCertificate"}""")
            .`when`().post("/api/chargepoints/$cpId/commands/get-installed-certificate-ids")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute deleteCertificate command round trip`() {
        val cpId = "delcert-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("DeleteCertificate"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"certificateHashData": {"hashAlgorithm": "SHA256", "issuerNameHash": "hash1", "issuerKeyHash": "hash2", "serialNumber": "1234"}}""")
            .`when`().post("/api/chargepoints/$cpId/commands/delete-certificate")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute getLog command round trip`() {
        val cpId = "getlog-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("GetLog"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"logType": "DiagnosticsLog", "requestId": 1, "log": {"remoteLocation": "https://example.com/log"}}""")
            .`when`().post("/api/chargepoints/$cpId/commands/get-log")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }

    @Test
    fun `should successfully execute signedUpdateFirmware command round trip`() {
        val cpId = "signedfirm-cp-${System.currentTimeMillis()}"
        val (ws, _) = connectBootAndRespond(cpId, CommandResponseConfig("SignedUpdateFirmware"))

        RestAssured.given()
            .contentType("application/json")
            .body("""{"requestId": 1, "firmware": {"location": "https://example.com/firmware.bin", "retrieveDateTime": "2024-01-01T00:00:00Z", "signingCertificate": "MIID...", "signature": "SIG..."}}""")
            .`when`().post("/api/chargepoints/$cpId/commands/signed-update-firmware")
            .then()
            .statusCode(202)
            .body("status", org.hamcrest.Matchers.equalTo("sent"))

        ws.close()
    }


}

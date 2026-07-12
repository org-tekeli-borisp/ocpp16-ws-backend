package org.tekeli.borisp.ocpp16

import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.WebSocketConnectOptions
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val METER_VALUES_CALL = """[2,"mv-1","MeterValues",{"connectorId":1,"transactionId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"1000","measurand":"Energy.Active.Import.Register","unit":"Wh"}]}]}]"""

private const val METER_VALUES_NULL_PAYLOAD = """[2,"mv-2","MeterValues",null]"""

private const val METER_VALUES_MISSING_CONNECTOR = """[2,"mv-3","MeterValues",{"transactionId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"1000"}]}]}]"""

private const val SECURITY_EVENT_CALL = """[2,"se-1","SecurityEventNotification",{"type":"Tampering","timestamp":"2024-01-01T00:00:00Z","techInfo":"info"}]"""

private const val SECURITY_EVENT_NULL_PAYLOAD = """[2,"se-2","SecurityEventNotification",null]"""

private const val SECURITY_EVENT_MISSING_TYPE = """[2,"se-3","SecurityEventNotification",{"timestamp":"2024-01-01T00:00:00Z"}]"""

private const val SIGNED_FIRMWARE_STATUS_CALL = """[2,"sfs-1","SignedFirmwareStatusNotification",{"status":"Downloading"}]"""

private const val SIGNED_FIRMWARE_STATUS_NULL_PAYLOAD = """[2,"sfs-2","SignedFirmwareStatusNotification",null]"""

private const val LOG_STATUS_CALL = """[2,"ls-1","LogStatusNotification",{"status":"Uploading"}]"""

private const val LOG_STATUS_NULL_PAYLOAD = """[2,"ls-2","LogStatusNotification",null]"""

private const val SIGN_CERTIFICATE_CALL = """[2,"sc-1","SignCertificate",{"csr":"MIID"}]"""

private const val SIGN_CERTIFICATE_NULL_PAYLOAD = """[2,"sc-2","SignCertificate",null]"""

private const val SIGN_CERTIFICATE_EMPTY_CSR = """[2,"sc-3","SignCertificate",{"csr":""}]"""

private const val CERTIFICATE_SIGNED_CALL = """[2,"cs-1","CertificateSigned",{"certificateChain":"MIID"}]"""

private const val CERTIFICATE_SIGNED_NULL_PAYLOAD = """[2,"cs-2","CertificateSigned",null]"""

@QuarkusTest
class OcppWebSocketServerMissingHandlersTest {

    @TestHTTPResource
    lateinit var ocppUri: URI

    @Inject
    lateinit var vertx: Vertx

    private fun testPort(): Int = ocppUri.port

    private fun connectAndSend(message: String): String {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()
        var ws: io.vertx.core.http.WebSocket? = null

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(testPort())
            .setURI("/ocpp/handler-test-${System.currentTimeMillis()}")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    val msg = buffer.toString()
                    if (msg.isNotEmpty()) {
                        responses.add(msg)
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(message)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")

        ws?.close()

        return responses[0]
    }

    @Test
    fun `should return empty CallResult for valid MeterValues`() {
        val response = connectAndSend(METER_VALUES_CALL)
        assertTrue(response.startsWith("[3,"), "Should be CALLRESULT")
    }

    @Test
    fun `should return FormationViolation for MeterValues with null payload`() {
        val response = connectAndSend(METER_VALUES_NULL_PAYLOAD)
        assertTrue(response.startsWith("[4,"), "Should be CALLERROR")
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should return FormationViolation for MeterValues missing connectorId`() {
        val response = connectAndSend(METER_VALUES_MISSING_CONNECTOR)
        assertTrue(response.startsWith("[4,"), "Should be CALLERROR")
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should return empty CallResult for valid SecurityEventNotification`() {
        val response = connectAndSend(SECURITY_EVENT_CALL)
        assertTrue(response.startsWith("[3,"), "Should be CALLRESULT")
    }

    @Test
    fun `should return FormationViolation for SecurityEventNotification with null payload`() {
        val response = connectAndSend(SECURITY_EVENT_NULL_PAYLOAD)
        assertTrue(response.startsWith("[4,"), "Should be CALLERROR")
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should return FormationViolation for SecurityEventNotification missing type`() {
        val response = connectAndSend(SECURITY_EVENT_MISSING_TYPE)
        assertTrue(response.startsWith("[4,"), "Should be CALLERROR")
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should return empty CallResult for valid SignedFirmwareStatusNotification`() {
        val response = connectAndSend(SIGNED_FIRMWARE_STATUS_CALL)
        assertTrue(response.startsWith("[3,"), "Should be CALLRESULT")
    }

    @Test
    fun `should return FormationViolation for SignedFirmwareStatusNotification with null payload`() {
        val response = connectAndSend(SIGNED_FIRMWARE_STATUS_NULL_PAYLOAD)
        assertTrue(response.startsWith("[4,"), "Should be CALLERROR")
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should return empty CallResult for valid LogStatusNotification`() {
        val response = connectAndSend(LOG_STATUS_CALL)
        assertTrue(response.startsWith("[3,"), "Should be CALLRESULT")
    }

    @Test
    fun `should return FormationViolation for LogStatusNotification with null payload`() {
        val response = connectAndSend(LOG_STATUS_NULL_PAYLOAD)
        assertTrue(response.startsWith("[4,"), "Should be CALLERROR")
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should return empty CallResult for valid SignCertificate`() {
        val response = connectAndSend(SIGN_CERTIFICATE_CALL)
        assertTrue(response.startsWith("[3,"), "Should be CALLRESULT")
    }

    @Test
    fun `should return FormationViolation for SignCertificate with null payload`() {
        val response = connectAndSend(SIGN_CERTIFICATE_NULL_PAYLOAD)
        assertTrue(response.startsWith("[4,"), "Should be CALLERROR")
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should return FormationViolation for SignCertificate with empty CSR`() {
        val response = connectAndSend(SIGN_CERTIFICATE_EMPTY_CSR)
        assertTrue(response.startsWith("[4,"), "Should be CALLERROR")
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `should return empty CallResult for valid CertificateSigned`() {
        val response = connectAndSend(CERTIFICATE_SIGNED_CALL)
        assertTrue(response.startsWith("[3,"), "Should be CALLRESULT")
    }

    @Test
    fun `should return FormationViolation for CertificateSigned with null payload`() {
        val response = connectAndSend(CERTIFICATE_SIGNED_NULL_PAYLOAD)
        assertTrue(response.startsWith("[4,"), "Should be CALLERROR")
        assertTrue(response.contains("FormationViolation"))
    }
}

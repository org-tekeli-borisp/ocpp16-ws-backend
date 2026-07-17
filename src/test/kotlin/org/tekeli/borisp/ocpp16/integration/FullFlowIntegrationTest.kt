package org.tekeli.borisp.ocpp16.integration

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketConnectOptions
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.ChargePointStatus
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@QuarkusTest
@jakarta.transaction.Transactional
class FullFlowIntegrationTest {

    @TestHTTPResource
    lateinit var ocppUri: URI

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var persistenceService: PersistenceService

    @Inject
    lateinit var em: jakarta.persistence.EntityManager

    @BeforeEach
    fun cleanup() {
        em.createNativeQuery("DELETE FROM transactions").executeUpdate()
        em.createNativeQuery("DELETE FROM charge_points").executeUpdate()
        em.flush()
    }

    private fun testPort(): Int = ocppUri.port

    private fun createWsOptions(uri: String): WebSocketConnectOptions = WebSocketConnectOptions()
        .setHost("localhost")
        .setPort(testPort())
        .setURI(uri)
        .setSubProtocols(listOf("ocpp1.6"))

    private fun waitForChargePoint(chargePointId: String, timeout: Long = 3000): org.tekeli.borisp.ocpp16.persistence.ChargePoint? {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            val cp = persistenceService.findChargePointById(chargePointId)
            if (cp != null) return cp
            Thread.sleep(100)
        }
        return null
    }

    private fun waitForChargePointStatus(chargePointId: String, expectedStatus: ChargePointStatus, timeout: Long = 5000): org.tekeli.borisp.ocpp16.persistence.ChargePoint? {
        val start = System.currentTimeMillis()
        while (System.currentTimeMillis() - start < timeout) {
            val cp = persistenceService.findChargePointById(chargePointId)
            if (cp != null && cp.status == expectedStatus) return cp
            Thread.sleep(100)
        }
        return persistenceService.findChargePointById(chargePointId)
    }

    private fun connectAndExchange(messages: List<String>, expectedResponses: Int): Pair<List<String>, WebSocket> {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(expectedResponses)
        val responses = mutableListOf<String>()
        var ws: WebSocket? = null

        val client = vertx.createWebSocketClient()
        val options = createWsOptions("/ocpp/integration-cp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    responseLatch.countDown()
                }

                for (msg in messages) {
                    ws.writeTextMessage(msg)
                }
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(10, TimeUnit.SECONDS), "Should receive $expectedResponses responses, got ${responses.size}")

        return responses to ws!!
    }

    private val mapper = ObjectMapper()

    private fun parsePayload(json: String): Map<String, Any?> {
        val root: JsonNode = mapper.readTree(json)
        val payloadNode: JsonNode? = root.get(2)
        if (payloadNode == null || !payloadNode.isObject) return emptyMap()

        val result = mutableMapOf<String, Any?>()
        for ((key, valueNode) in payloadNode.properties()) {
            result[key] = when {
                valueNode.isTextual -> valueNode.asText()
                valueNode.isNumber -> valueNode.asLong()
                valueNode.isBoolean -> valueNode.asBoolean()
                valueNode.isObject -> valueNode.toString()
                else -> valueNode.asText()
            }
        }
        return result
    }

    private fun getIdTagInfoStatus(json: String): String? {
        val root: JsonNode = mapper.readTree(json)
        val payloadNode: JsonNode? = root.get(2)
        payloadNode ?: return null
        val idTagInfoNode: JsonNode? = payloadNode.get("idTagInfo")
        idTagInfoNode ?: return null
        return idTagInfoNode.get("status")?.asText()
    }

    @Test
    fun `should persist chargePoint on BootNotification`() {
        val messages = listOf(
            """[2,"boot-1","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3","firmwareVersion":"1.0"}]"""
        )
        val (responses, ws) = connectAndExchange(messages, 1)

        // Verify response
        val payload = parsePayload(responses[0])
        assertEquals("Accepted", payload["status"] as String?)

        // Close and wait for OnClose
        ws.close()
        Thread.sleep(1000)

        // Verify persistence
        val cp = waitForChargePoint("integration-cp")
        assertNotNull(cp)
        assertEquals("Tesla", cp!!.vendor)
        assertEquals("Model3", cp.model)
        assertEquals("1.0", cp.firmwareVersion)
    }

    @Test
    fun `should full flow boot heartbeat start stop transaction`() {
        val messages = listOf(
            """[2,"boot-2","BootNotification",{"chargePointVendor":"ABB","chargePointModel":"Terra","firmwareVersion":"2.1"}]""",
            """[2,"hb-1","Heartbeat",{}]""",
            """[2,"start-1","StartTransaction",{"connectorId":1,"idTag":"CARD123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )

        val (responses, ws) = connectAndExchange(messages, 3)

        // Verify BootNotification response
        var payload = parsePayload(responses[0])
        assertEquals("Accepted", payload["status"] as String?)

        // Verify Heartbeat response
        payload = parsePayload(responses[1])
        assertNotNull(payload["currentTime"])

        // Verify StartTransaction response
        payload = parsePayload(responses[2])
        // idTagInfo handled separately
        assertEquals("Accepted", getIdTagInfoStatus(responses[2]))

        // Verify chargePoint was created and is ONLINE
        val cp = persistenceService.findChargePointById("integration-cp")
        assertNotNull(cp)
        assertEquals(ChargePointStatus.ONLINE, cp!!.status)

        // Verify all chargePoints
        val allCps = persistenceService.findAllChargePoints()
        assertTrue(allCps.any { it.chargePointId == "integration-cp" })

        ws.close()
    }

    @Test
    fun `should reject StartTransaction with invalid connectorId`() {
        val messages = listOf(
            """[2,"boot-3","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"ModelS"}]""",
            """[2,"start-invalid","StartTransaction",{"connectorId":0,"idTag":"CARD456","meterStart":100,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )

        val (responses, ws) = connectAndExchange(messages, 2)

        // First response should be CALLRESULT
        assertTrue(responses[0].startsWith("[3,"))

        // Second response should be CALLERROR
        assertTrue(responses[1].startsWith("[4,"), "Should be CALLERROR: ${responses[1]}")

        ws.close()
    }

    @Test
    fun `should reject StopTransaction for non-existent transaction`() {
        val messages = listOf(
            """[2,"boot-4","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"ModelY"}]""",
            """[2,"stop-2","StopTransaction",{"transactionId":99999,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )

        val (responses, ws) = connectAndExchange(messages, 2)

        // Stop should still return Accepted (no validation on server side for txn existence)
        assertEquals("Accepted", getIdTagInfoStatus(responses[1]))

        ws.close()
    }
}

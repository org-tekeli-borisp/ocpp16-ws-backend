package org.tekeli.borisp.ocpp16.integration

import io.quarkus.test.common.http.TestHTTPResource
import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.WebSocket
import io.vertx.core.http.WebSocketConnectOptions
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.net.URI
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

@QuarkusTest
@Timeout(60)
@jakarta.transaction.Transactional
class CommandTimeoutTest {

    @TestHTTPResource
    lateinit var ocppUri: URI

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var em: EntityManager

    @BeforeEach
    fun cleanup() {
        em.createNativeQuery("DELETE FROM transactions").executeUpdate()
        em.createNativeQuery("DELETE FROM charge_points").executeUpdate()
        em.flush()
    }

    private fun testPort(): Int = ocppUri.port

    private fun connectAndBoot(chargePointId: String): WebSocket {
        val connectLatch = CountDownLatch(1)
        val bootLatch = CountDownLatch(1)
        val wsRef = AtomicReference<WebSocket>()
        val booted = AtomicBoolean(false)

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(testPort())
            .setURI("/ocpp/$chargePointId")
            .setSubProtocols(listOf("ocpp1.6"))

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                wsRef.set(ws)
                connectLatch.countDown()
                ws.handler { buffer: Buffer ->
                    val message = buffer.toString()
                    if (!booted.get() && message.startsWith("[3,")) {
                        booted.set(true)
                        bootLatch.countDown()
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
        return wsRef.get()!!
    }

    @Test
    fun `should return 504 when connected chargePoint never responds to command`() {
        val cpId = "timeout-cp-${System.currentTimeMillis()}"
        val ws = connectAndBoot(cpId)

        RestAssured.given()
            .contentType("application/json")
            .body("""{"type": "Hard"}""")
            .`when`().post("/api/chargepoints/$cpId/commands/reset")
            .then()
            .statusCode(504)
            .body("error", org.hamcrest.Matchers.notNullValue())

        ws.close()
    }
}

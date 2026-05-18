package org.tekeli.borisp.ocpp16

import io.quarkus.test.junit.QuarkusTest
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.WebSocketConnectOptions
import io.quarkus.websockets.next.WebSocketConnection
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val BOOT_NOTIFICATION_CALL = """[2,"12345","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3","firmwareVersion":"1.0"}]"""

private const val HEARTBEAT_CALL = """[2,"67890","Heartbeat",{}]"""

private const val AUTHORIZE_CALL = """[2,"auth-1","Authorize",{"idTag":"ABC123"}]"""

private const val AUTHORIZE_NULL_PAYLOAD = """[2,"auth-2","Authorize",null]"""

private const val AUTHORIZE_MISSING_IDTAG = """[2,"auth-3","Authorize",{}]"""

private const val AUTHORIZE_EMPTY_IDTAG = """[2,"auth-4","Authorize",{"idTag":""}]"""

private const val AUTHORIZE_LONG_IDTAG = """[2,"auth-5","Authorize",{"idTag":"AAAAAAAAAAAAAAAAAAAAA"}]"""

private const val START_TRANSACTION_CALL = """[2,"st-1","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""

private const val START_TRANSACTION_NULL_PAYLOAD = """[2,"st-2","StartTransaction",null]"""

private const val START_TRANSACTION_MISSING_IDTAG = """[2,"st-3","StartTransaction",{"connectorId":1,"meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""

private const val INVALID_MESSAGE_FORMAT = """not a json array"""

private const val CALL_WITH_WRONG_ACTION = """[2,"11111","InvalidAction",{}]"""

private const val BOOT_NOTIFICATION_WITH_MISSING_FIELDS = """[2,"22222","BootNotification",{"chargePointVendor":"Tesla"}]"""

private const val BOOT_NOTIFICATION_WITH_EMPTY_VENDOR = """[2,"33333","BootNotification",{"chargePointVendor":"","chargePointModel":"Model3"}]"""

private const val BOOT_NOTIFICATION_WITH_EMPTY_MODEL = """[2,"44444","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":""}]"""

private const val CALLRESULT_FROM_CHARGEPOINT = """[3,"55555",{}]"""

private const val CALLERROR_FROM_CHARGEPOINT = """[4,"66666","GenericError","Error",""]"""

private const val BOOT_NOTIFICATION_WITH_NULL_PAYLOAD = """[2,"77777","BootNotification",null]"""

private const val INVALID_MESSAGE_TYPE_ZERO = """[0,"88888"]"""

private const val INVALID_MESSAGE_TYPE_NEGATIVE = """[-1,"99999"]"""

@QuarkusTest
class OcppWebSocketServerTest {

    @Inject
    lateinit var vertx: Vertx

    @Inject
    lateinit var server: OcppWebSocketServer

    @Inject
    lateinit var connection: WebSocketConnection

    @Test
    fun `should access connection property`() {
        assertDoesNotThrow {
            val conn = server.activeConnection
            assertNotNull(conn)
        }
    }

    @Test
    fun `should throw when connection not initialized`() {
        val uninitializedServer = OcppWebSocketServer()
        assertThrows(IllegalStateException::class.java) {
            uninitializedServer.activeConnection
        }
    }

    @Test
    fun shouldAcceptConnectionOnOcppEndpoint() {
        val latch = CountDownLatch(1)
        
        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")
        
        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                ar.result().close()
                latch.countDown()
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }
        
        val connected = latch.await(5, TimeUnit.SECONDS)
        assertTrue(connected, "Connection should be accepted on /ocpp endpoint")
    }

    @Test
    fun shouldReturnCallErrorForInvalidJsonFormat() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(INVALID_MESSAGE_FORMAT)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.contains("[4"), "Should be CALLERROR message")
        assertTrue(callError.contains("ProtocolError"), "Should return ProtocolError for invalid JSON")
    }

    @Test
    fun shouldReturnCallErrorForUnknownAction() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(CALL_WITH_WRONG_ACTION)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.contains("[4"), "Should be CALLERROR message")
        assertTrue(callError.contains("NotImplemented"), "Should return NotImplemented for unknown action")
    }

    @Test
    fun shouldReturnCallErrorForBootNotificationWithMissingRequiredFields() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(BOOT_NOTIFICATION_WITH_MISSING_FIELDS)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.contains("[4"), "Should be CALLERROR message")
        assertTrue(callError.contains("FormationViolation"), "Should return FormationViolation for missing required fields")
    }

    @Test
    fun shouldReturnCallResultForValidBootNotification() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(BOOT_NOTIFICATION_CALL)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callResult = responses[0]
        assertTrue(callResult.contains("[3"), "Should be CALLRESULT message")
        assertTrue(callResult.contains("12345"), "Should contain original messageId")
        assertTrue(callResult.contains("currentTime"), "Should contain currentTime in response")
    }

    @Test
    fun shouldReturnCallResultForValidHeartbeat() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(HEARTBEAT_CALL)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callResult = responses[0]
        assertTrue(callResult.contains("[3"), "Should be CALLRESULT message")
        assertTrue(callResult.contains("67890"), "Should contain original messageId")
        assertTrue(callResult.contains("currentTime"), "Should contain currentTime in response")
    }

    @Test
    fun shouldReturnCallErrorForBootNotificationWithEmptyVendor() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(BOOT_NOTIFICATION_WITH_EMPTY_VENDOR)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.contains("[4"), "Should be CALLERROR message")
        assertTrue(callError.contains("FormationViolation"), "Should return FormationViolation for empty vendor")
    }

    @Test
    fun shouldReturnCallErrorForBootNotificationWithEmptyModel() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(BOOT_NOTIFICATION_WITH_EMPTY_MODEL)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.contains("[4"), "Should be CALLERROR message")
        assertTrue(callError.contains("FormationViolation"), "Should return FormationViolation for empty model")
    }

    @Test
    fun shouldReturnCallErrorForCallResultFromChargePoint() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(CALLRESULT_FROM_CHARGEPOINT)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.contains("[4"), "Should be CALLERROR message")
        assertTrue(callError.contains("ProtocolError"), "Should return ProtocolError for unexpected CALLRESULT")
    }

    @Test
    fun shouldReturnCallErrorForCallErrorFromChargePoint() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(CALLERROR_FROM_CHARGEPOINT)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.contains("[4"), "Should be CALLERROR message")
        assertTrue(callError.contains("ProtocolError"), "Should return ProtocolError for unexpected CALLERROR")
    }

    @Test
    fun shouldReturnCallErrorForBootNotificationWithNullPayload() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(BOOT_NOTIFICATION_WITH_NULL_PAYLOAD)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.contains("[4"), "Should be CALLERROR message")
        assertTrue(callError.contains("FormationViolation"), "Should return FormationViolation for null payload")
    }

    @Test
    fun shouldReturnCallErrorForInvalidMessageTypeZero() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(INVALID_MESSAGE_TYPE_ZERO)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.contains("[4"), "Should be CALLERROR message")
        assertTrue(callError.contains("ProtocolError"), "Should return ProtocolError for invalid message type")
    }

    @Test
    fun shouldReturnCallErrorForInvalidMessageTypeNegative() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(INVALID_MESSAGE_TYPE_NEGATIVE)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.contains("[4"), "Should be CALLERROR message")
        assertTrue(callError.contains("ProtocolError"), "Should return ProtocolError for invalid message type")
    }

    @Test
    fun `should return Accepted for valid Authorize request`() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(AUTHORIZE_CALL)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callResult = responses[0]
        assertTrue(callResult.startsWith("[3,"), "Should be CALLRESULT message")
        assertTrue(callResult.contains("Accepted"), "Should return Accepted status")
    }

    @Test
    fun `should return FormationViolation for Authorize with null payload`() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(AUTHORIZE_NULL_PAYLOAD)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.startsWith("[4,"), "Should be CALLERROR message")
        assertTrue(callError.contains("FormationViolation"), "Should return FormationViolation")
    }

    @Test
    fun `should return FormationViolation for Authorize with missing idTag`() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(AUTHORIZE_MISSING_IDTAG)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.startsWith("[4,"), "Should be CALLERROR message")
        assertTrue(callError.contains("FormationViolation"), "Should return FormationViolation")
    }

    @Test
    fun `should return Accepted with transactionId for valid StartTransaction`() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(START_TRANSACTION_CALL)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callResult = responses[0]
        assertTrue(callResult.startsWith("[3,"), "Should be CALLRESULT message")
        assertTrue(callResult.contains("Accepted"), "Should return Accepted status")
        assertTrue(callResult.contains("transactionId"), "Should contain transactionId")
    }

    @Test
    fun `should return FormationViolation for StartTransaction with null payload`() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(START_TRANSACTION_NULL_PAYLOAD)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.startsWith("[4,"), "Should be CALLERROR message")
        assertTrue(callError.contains("FormationViolation"), "Should return FormationViolation")
    }

    @Test
    fun `should return FormationViolation for StartTransaction with missing idTag`() {
        val connectLatch = CountDownLatch(1)
        val responseLatch = CountDownLatch(1)
        val responses = mutableListOf<String>()

        val client = vertx.createWebSocketClient()
        val options = WebSocketConnectOptions()
            .setHost("localhost")
            .setPort(8081)
            .setURI("/ocpp")

        client.connect(options).onComplete { ar ->
            if (ar.succeeded()) {
                val ws = ar.result()
                connectLatch.countDown()

                ws.handler { buffer: Buffer ->
                    responses.add(buffer.toString())
                    if (responses.size >= 1) {
                        responseLatch.countDown()
                    }
                }

                ws.writeTextMessage(START_TRANSACTION_MISSING_IDTAG)
            } else {
                throw RuntimeException("Failed to connect", ar.cause())
            }
        }

        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Should connect")
        assertTrue(responseLatch.await(5, TimeUnit.SECONDS), "Should receive response")
        val callError = responses[0]
        assertTrue(callError.startsWith("[4,"), "Should be CALLERROR message")
        assertTrue(callError.contains("FormationViolation"), "Should return FormationViolation")
    }
}

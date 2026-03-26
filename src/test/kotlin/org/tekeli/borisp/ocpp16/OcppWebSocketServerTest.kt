package org.tekeli.borisp.ocpp16

import io.quarkus.test.junit.QuarkusTest
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.WebSocketConnectOptions
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

private const val BOOT_NOTIFICATION_CALL = """[2,"12345","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3","firmwareVersion":"1.0"}]"""

private const val HEARTBEAT_CALL = """[2,"67890","Heartbeat",{}]"""

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
}

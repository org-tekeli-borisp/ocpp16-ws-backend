package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.outbound.OutboundCallDispatcher
import org.tekeli.borisp.ocpp16.outbound.TextSender
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import org.tekeli.borisp.ocpp16.protocol.OcppMessageType
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class OutboundCallDispatcherTest {

    @Test
    fun `should create and send CALL message`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        val future = dispatcher.sendCall("Reset", mapOf("type" to "Hard"))

        assertFalse(future.isDone)
        assertEquals(1, sentMessages.size)
        assertTrue(sentMessages[0].startsWith("[2,"))
        assertTrue(sentMessages[0].contains("\"Reset\""))
        assertTrue(sentMessages[0].contains("\"Hard\""))
    }

    @Test
    fun `should generate UUID messageId`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        dispatcher.sendCall("Reset", mapOf("type" to "Hard"))

        val json = sentMessages[0]
        val uuidRegex = Regex("""\[2,"[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}",""")
        assertTrue(uuidRegex.containsMatchIn(json), "messageId must be valid UUID")
    }

    @Test
    fun `should resolve future when CallResult arrives`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val latch = CountDownLatch(1)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        val future = dispatcher.sendCall("Reset", mapOf("type" to "Hard"))
        future.whenComplete { _, _ -> latch.countDown() }

        val messageId = extractMessageId(sentMessages[0])
        awaiter.resolve(messageId, OcppMessage.CallResult(messageId, mapOf("status" to "Accepted")))

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertTrue(future.isDone)
        val result = future.get() as OcppMessage.CallResult
        assertEquals("Accepted", result.payload?.get("status"))
    }

    @Test
    fun `should resolve future when CallError arrives`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val latch = CountDownLatch(1)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        val future = dispatcher.sendCall("Reset", mapOf("type" to "Hard"))
        future.whenComplete { _, _ -> latch.countDown() }

        val messageId = extractMessageId(sentMessages[0])
        awaiter.reject(messageId, OcppMessage.CallError(
            messageId = messageId,
            errorCode = OcppErrorCode.PROTOCOL_ERROR,
            errorDescription = "Invalid type",
            errorDetails = null
        ))

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        val result = future.get() as OcppMessage.CallError
        assertEquals(OcppErrorCode.PROTOCOL_ERROR, result.errorCode)
    }

    @Test
    fun `should timeout pending call`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        val future = dispatcher.sendCall("Reset", mapOf("type" to "Hard"))

        val messageId = extractMessageId(sentMessages[0])
        awaiter.timeout(messageId, TimeoutException("Connection timed out"))

        assertTrue(future.isDone)
        assertThrows(java.util.concurrent.ExecutionException::class.java) { future.get() }
    }

    @Test
    fun `should send call with null payload serialized as empty object`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        dispatcher.sendCall("ClearCache", null)

        val json = sentMessages[0]
        assertTrue(json.contains("\"ClearCache\""))
        val parsed = OcppMessage.parse(json) as OcppMessage.Call
        assertNotNull(parsed.payload)
        assertTrue(parsed.payload!!.isEmpty())
    }

    @Test
    fun `should send call with empty payload`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        dispatcher.sendCall("ClearCache", emptyMap<String, Any>())

        val json = sentMessages[0]
        assertTrue(json.contains("\"ClearCache\""))
    }

    @Test
    fun `should handle multiple concurrent outbound calls`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val latch = CountDownLatch(2)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        val future1 = dispatcher.sendCall("Reset", mapOf("type" to "Hard"))
        val future2 = dispatcher.sendCall("ClearCache", null)

        future1.whenComplete { _, _ -> latch.countDown() }
        future2.whenComplete { _, _ -> latch.countDown() }

        assertEquals(2, sentMessages.size)

        val msgId1 = extractMessageId(sentMessages[0])
        val msgId2 = extractMessageId(sentMessages[1])

        awaiter.resolve(msgId1, OcppMessage.CallResult(msgId1, mapOf("status" to "Accepted")))
        awaiter.resolve(msgId2, OcppMessage.CallResult(msgId2, mapOf("status" to "Accepted")))

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals("Accepted", (future1.get() as OcppMessage.CallResult).payload?.get("status"))
        assertEquals("Accepted", (future2.get() as OcppMessage.CallResult).payload?.get("status"))
    }

    @Test
    fun `should produce valid OCPP CALL JSON format`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        dispatcher.sendCall("Reset", mapOf("type" to "Soft"))

        val json = sentMessages[0]
        val parsed = OcppMessage.parse(json) as OcppMessage.Call

        assertEquals(OcppMessageType.CALL, parsed.type)
        assertEquals("Reset", parsed.action)
        assertEquals("Soft", parsed.payload?.get("type"))
    }

    @Test
    fun `should generate unique messageId for each call`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        dispatcher.sendCall("Reset", mapOf("type" to "Hard"))
        dispatcher.sendCall("Reset", mapOf("type" to "Hard"))
        dispatcher.sendCall("ClearCache", null)

        val ids = sentMessages.map { extractMessageId(it) }
        assertEquals(3, ids.toSet().size, "All messageIds must be unique")
    }

    @Test
    fun `should send UpdateFirmware with complex payload`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        dispatcher.sendCall("UpdateFirmware", mapOf(
            "location" to "https://example.com/firmware.bin",
            "retrieveDate" to "2024-06-01T10:00:00Z",
            "retries" to 3,
            "retryInterval" to 120
        ))

        val json = sentMessages[0]
        val parsed = OcppMessage.parse(json) as OcppMessage.Call
        assertEquals("UpdateFirmware", parsed.action)
        assertEquals("https://example.com/firmware.bin", parsed.payload?.get("location"))
        assertEquals(3, parsed.payload?.get("retries"))
    }

    @Test
    fun `should send TriggerMessage with optional connectorId`() {
        val sentMessages = mutableListOf<String>()
        val mockConn = TestWebSocketConnection(sentMessages)
        val awaiter = ResponseAwaiter()
        val dispatcher = OutboundCallDispatcher(mockConn, awaiter)

        dispatcher.sendCall("TriggerMessage", mapOf(
            "requestedMessage" to "BootNotification",
            "connectorId" to 1
        ))

        val json = sentMessages[0]
        val parsed = OcppMessage.parse(json) as OcppMessage.Call
        assertEquals("TriggerMessage", parsed.action)
        assertEquals("BootNotification", parsed.payload?.get("requestedMessage"))
        assertEquals(1, parsed.payload?.get("connectorId"))
    }

    private fun extractMessageId(json: String): String {
        val msg = OcppMessage.parse(json)
        return msg.messageId
    }

    private class TestWebSocketConnection(
        private val sentMessages: MutableList<String>
    ) : TextSender {
        override fun sendText(text: String): io.smallrye.mutiny.Uni<Void> {
            sentMessages.add(text)
            return io.smallrye.mutiny.Uni.createFrom().voidItem()
        }
    }
}

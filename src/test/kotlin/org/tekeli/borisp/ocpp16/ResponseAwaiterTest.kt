package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.concurrent.ExecutionException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ResponseAwaiterTest {

    @Test
    fun `should return pending future for new messageId`() {
        val awaiter = ResponseAwaiter()

        val future = awaiter.pending("msg-1")

        assertNotNull(future)
        assertFalse(future.isDone)
    }

    @Test
    fun `should return different futures for different messageIds`() {
        val awaiter = ResponseAwaiter()

        val future1 = awaiter.pending("msg-1")
        val future2 = awaiter.pending("msg-2")

        assertNotSame(future1, future2)
    }

    @Test
    fun `should complete future when resolved`() {
        val awaiter = ResponseAwaiter()
        val latch = CountDownLatch(1)

        val future = awaiter.pending("msg-1")
        future.whenComplete { _, _ -> latch.countDown() }

        val response = OcppMessage.CallResult(
            messageId = "msg-1",
            payload = mapOf("status" to "Accepted")
        )
        awaiter.resolve("msg-1", response)

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertTrue(future.isDone)
        assertEquals(response, future.get())
    }

    @Test
    fun `should complete with CallError when rejected`() {
        val awaiter = ResponseAwaiter()
        val latch = CountDownLatch(1)

        val future = awaiter.pending("msg-1")
        future.whenComplete { _, _ -> latch.countDown() }

        val error = OcppMessage.CallError(
            messageId = "msg-1",
            errorCode = OcppErrorCode.PROTOCOL_ERROR,
            errorDescription = "Invalid",
            errorDetails = null
        )
        awaiter.reject("msg-1", error)

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertTrue(future.isDone)
        val result = future.get() as OcppMessage.CallError
        assertEquals(OcppErrorCode.PROTOCOL_ERROR, result.errorCode)
    }

    @Test
    fun `should throw CompletionException for unknown messageId on resolve`() {
        val awaiter = ResponseAwaiter()

        val response = OcppMessage.CallResult(
            messageId = "unknown",
            payload = mapOf("status" to "Accepted")
        )

        assertThrows(IllegalStateException::class.java) {
            awaiter.resolve("unknown", response)
        }
    }

    @Test
    fun `should throw CompletionException for unknown messageId on reject`() {
        val awaiter = ResponseAwaiter()

        val error = OcppMessage.CallError(
            messageId = "unknown",
            errorCode = OcppErrorCode.PROTOCOL_ERROR,
            errorDescription = "Invalid",
            errorDetails = null
        )

        assertThrows(IllegalStateException::class.java) {
            awaiter.reject("unknown", error)
        }
    }

    @Test
    fun `should not affect other pending futures when one is resolved`() {
        val awaiter = ResponseAwaiter()

        val future1 = awaiter.pending("msg-1")
        val future2 = awaiter.pending("msg-2")

        val response = OcppMessage.CallResult(
            messageId = "msg-1",
            payload = mapOf("status" to "Accepted")
        )
        awaiter.resolve("msg-1", response)

        assertTrue(future1.isDone)
        assertFalse(future2.isDone)
    }

    @Test
    fun `should remove entry after resolution`() {
        val awaiter = ResponseAwaiter()

        val future = awaiter.pending("msg-1")
        awaiter.resolve("msg-1", OcppMessage.CallResult("msg-1", null))

        assertThrows(IllegalStateException::class.java) {
            awaiter.resolve("msg-1", OcppMessage.CallResult("msg-1", null))
        }
        assertTrue(future.isDone)
    }

    @Test
    fun `should handle multiple concurrent pending calls`() {
        val awaiter = ResponseAwaiter()
        val latch = CountDownLatch(3)

        val future1 = awaiter.pending("msg-1")
        val future2 = awaiter.pending("msg-2")
        val future3 = awaiter.pending("msg-3")

        future1.whenComplete { _, _ -> latch.countDown() }
        future2.whenComplete { _, _ -> latch.countDown() }
        future3.whenComplete { _, _ -> latch.countDown() }

        awaiter.resolve("msg-2", OcppMessage.CallResult("msg-2", mapOf("a" to 1)))
        awaiter.resolve("msg-1", OcppMessage.CallResult("msg-1", mapOf("b" to 2)))
        awaiter.reject("msg-3", OcppMessage.CallError("msg-3", OcppErrorCode.GENERIC_ERROR, "err", null))

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertEquals(mapOf("b" to 2), (future1.get() as OcppMessage.CallResult).payload)
        assertEquals(mapOf("a" to 1), (future2.get() as OcppMessage.CallResult).payload)
        assertTrue(future3.get() is OcppMessage.CallError)
    }

    @Test
    fun `should handle CallResult with null payload`() {
        val awaiter = ResponseAwaiter()

        val future = awaiter.pending("msg-1")
        awaiter.resolve("msg-1", OcppMessage.CallResult("msg-1", null))

        val result = future.get() as OcppMessage.CallResult
        assertNull(result.payload)
    }

    @Test
    fun `should handle empty pending map initially`() {
        val awaiter = ResponseAwaiter()

        assertThrows(IllegalStateException::class.java) {
            awaiter.resolve("no-pending", OcppMessage.CallResult("no-pending", null))
        }
    }

    @Test
    fun `should complete future exceptionally when resolved with exception`() {
        val awaiter = ResponseAwaiter()
        val latch = CountDownLatch(1)

        val future = awaiter.pending("msg-1")
        future.whenComplete { _, ex -> if (ex != null) latch.countDown() }

        val timeout = TimeoutException("ChargePoint timed out")
        awaiter.timeout("msg-1", timeout)

        assertTrue(latch.await(1, TimeUnit.SECONDS))
        assertTrue(future.isDone)
        val ex = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(ex.cause is TimeoutException)
    }

    @Test
    fun `should not affect other futures on timeout`() {
        val awaiter = ResponseAwaiter()

        val future1 = awaiter.pending("msg-1")
        val future2 = awaiter.pending("msg-2")

        awaiter.timeout("msg-1", TimeoutException("timeout"))

        assertTrue(future1.isDone)
        assertFalse(future2.isDone)
    }

    @Test
    fun `should throw for timeout on unknown messageId`() {
        val awaiter = ResponseAwaiter()

        assertThrows(IllegalStateException::class.java) {
            awaiter.timeout("unknown", TimeoutException())
        }
    }

    @Test
    fun `should handle resolve after pending returns same response`() {
        val awaiter = ResponseAwaiter()

        val response = OcppMessage.CallResult(
            messageId = "msg-1",
            payload = mapOf("status" to "Accepted", "data" to "test")
        )
        val future = awaiter.pending("msg-1")
        awaiter.resolve("msg-1", response)

        val result = future.get() as OcppMessage.CallResult
        assertEquals("Accepted", result.payload?.get("status"))
        assertEquals("test", result.payload?.get("data"))
    }

    @Test
    fun `should handle reject preserving all error details`() {
        val awaiter = ResponseAwaiter()

        val error = OcppMessage.CallError(
            messageId = "msg-1",
            errorCode = OcppErrorCode.FORMATION_VIOLATION,
            errorDescription = "Invalid field",
            errorDetails = mapOf("field" to "connectorId", "value" to -1)
        )
        val future = awaiter.pending("msg-1")
        awaiter.reject("msg-1", error)

        val result = future.get() as OcppMessage.CallError
        assertEquals(OcppErrorCode.FORMATION_VIOLATION, result.errorCode)
        assertEquals("Invalid field", result.errorDescription)
        assertEquals(-1, result.errorDetails?.get("value"))
    }

    @Test
    fun `should handle pending with same messageId returning different futures`() {
        val awaiter = ResponseAwaiter()

        val future1 = awaiter.pending("msg-1")
        awaiter.resolve("msg-1", OcppMessage.CallResult("msg-1", null))

        val future2 = awaiter.pending("msg-1")

        assertNotSame(future1, future2)
        assertFalse(future2.isDone)
    }
}

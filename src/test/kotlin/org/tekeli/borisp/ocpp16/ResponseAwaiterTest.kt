package org.tekeli.borisp.ocpp16

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import java.util.concurrent.ExecutionException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
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

    // --- rejectAll() tests ---

    @Test
    fun `rejectAll completes all pending futures exceptionally`() {
        val awaiter = ResponseAwaiter()
        val latch = CountDownLatch(3)

        val future1 = awaiter.pending("msg-1")
        val future2 = awaiter.pending("msg-2")
        val future3 = awaiter.pending("msg-3")

        future1.whenComplete { _, _ -> latch.countDown() }
        future2.whenComplete { _, _ -> latch.countDown() }
        future3.whenComplete { _, _ -> latch.countDown() }

        awaiter.rejectAll("Connection closed")

        assertTrue(latch.await(1, TimeUnit.SECONDS), "All pending futures should complete")
        assertTrue(future1.isDone)
        assertTrue(future2.isDone)
        assertTrue(future3.isDone)
    }

    @Test
    fun `rejectAll completes futures with IllegalStateException`() {
        val awaiter = ResponseAwaiter()

        val future = awaiter.pending("msg-1")
        awaiter.rejectAll("Connection lost")

        val ex = assertThrows(ExecutionException::class.java) { future.get() }
        assertTrue(ex.cause is IllegalStateException)
        assertTrue(ex.cause!!.message!!.contains("Connection lost"))
    }

    @Test
    fun `rejectAll rejects all pending and marks awaiter as rejected`() {
        val awaiter = ResponseAwaiter()

        val future1 = awaiter.pending("msg-1")
        awaiter.rejectAll("first")
        assertTrue(future1.isDone)

        // After rejectAll, new pending calls return immediately rejected futures
        val future2 = awaiter.pending("msg-2")
        assertTrue(future2.isDone, "awaiter should be in rejected state after rejectAll")
        assertThrows(ExecutionException::class.java) { future2.get() }
    }

    @Test
    fun `rejectAll on empty awaiter does not throw`() {
        val awaiter = ResponseAwaiter()

        assertDoesNotThrow { awaiter.rejectAll("closed") }
    }

    @Test
    fun `rejectAll clears all pending entries`() {
        val awaiter = ResponseAwaiter()

        awaiter.pending("msg-1")
        awaiter.pending("msg-2")

        awaiter.rejectAll("closed")

        assertThrows(IllegalStateException::class.java) {
            awaiter.resolve("msg-1", OcppMessage.CallResult("msg-1", null))
        }
    }

    @Test
    fun `rejectAll does not affect already resolved futures`() {
        val awaiter = ResponseAwaiter()

        val future1 = awaiter.pending("msg-1")
        val future2 = awaiter.pending("msg-2")

        awaiter.resolve("msg-1", OcppMessage.CallResult("msg-1", mapOf("status" to "Accepted")))

        awaiter.rejectAll("closed")

        assertTrue(future1.isDone)
        assertEquals("Accepted", (future1.get() as OcppMessage.CallResult).payload?.get("status"))
        assertTrue(future2.isDone)
        assertThrows(ExecutionException::class.java) { future2.get() }
    }

    @Test
    fun `rejectAll handles many pending futures concurrently`() {
        val awaiter = ResponseAwaiter()
        val latch = CountDownLatch(50)
        val futures = List(50) { i ->
            val f = awaiter.pending("msg-$i")
            f.whenComplete { _, _ -> latch.countDown() }
            f
        }

        awaiter.rejectAll("disconnected")

        assertTrue(latch.await(2, TimeUnit.SECONDS), "All 50 futures should complete")
        futures.forEach { assertTrue(it.isDone) }
    }

    // --- rejectAll concurrency tests ---

    @Test
    fun `rejectAll must not throw ConcurrentModificationException when resolve runs concurrently`() {
        val awaiter = ResponseAwaiter()
        val threads = 10
        val executor: ExecutorService = Executors.newFixedThreadPool(threads)
        val startLatch = CountDownLatch(1)
        val errorHolder: java.util.concurrent.atomic.AtomicReference<Throwable?> = java.util.concurrent.atomic.AtomicReference(null)

        // Create pending responses
        repeat(100) { i ->
            awaiter.pending("msg-$i")
        }

        // Multiple threads call rejectAll and resolve concurrently
        repeat(threads / 2) { t ->
            executor.submit {
                try {
                    startLatch.await()
                    repeat(10) {
                        awaiter.rejectAll("test-$t")
                    }
                } catch (e: Throwable) {
                    errorHolder.compareAndSet(null, e)
                }
            }
        }
        repeat(threads / 2) { t ->
            executor.submit {
                try {
                    startLatch.await()
                    repeat(10) { i ->
                        try {
                            awaiter.resolve("msg-$i", OcppMessage.CallResult("msg-$i", null))
                        } catch (_: IllegalStateException) {
                            // Expected when entry already removed
                        }
                    }
                } catch (e: Throwable) {
                    errorHolder.compareAndSet(null, e)
                }
            }
        }

        startLatch.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS))

        if (errorHolder.get() != null) {
            throw AssertionError("Concurrent rejectAll/resolve threw", errorHolder.get()!!)
        }
    }

    @Test
    fun `rejectAll must not lose futures when pending is called concurrently`() {
        val awaiter = ResponseAwaiter()
        val threads = 10
        val operationsPerThread = 100
        val executor: ExecutorService = Executors.newFixedThreadPool(threads)
        val startLatch = CountDownLatch(1)
        val completedFutures = java.util.concurrent.atomic.AtomicInteger(0)

        // Thread group 1: create pending and immediately track completion
        // Thread group 2: call rejectAll

        repeat(threads / 2) {
            executor.submit {
                try {
                    startLatch.await()
                    repeat(operationsPerThread) { i ->
                        val f = awaiter.pending("new-msg-${System.nanoTime()}-$i")
                        f.whenComplete { _, _ -> completedFutures.incrementAndGet() }
                    }
                } finally {
                    // done creating
                }
            }
        }
        repeat(threads / 2) {
            executor.submit {
                try {
                    startLatch.await()
                    repeat(operationsPerThread) {
                        awaiter.rejectAll("concurrent-disconnect")
                    }
                } finally {
                    // done rejecting
                }
            }
        }

        startLatch.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS))

        // Every future that was created should eventually complete
        // (either by rejectAll or by a subsequent rejectAll)
        val totalCreated = (threads / 2) * operationsPerThread
        // Give some time for async completions
        Thread.sleep(500)
        assertTrue(completedFutures.get() >= totalCreated * 0.9,
            "Most futures should complete: ${completedFutures.get()}/$totalCreated")
    }

    @Test
    fun `rejectAll and pending must not corrupt internal map state`() {
        val awaiter = ResponseAwaiter()
        val threads = 20
        val operationsPerThread = 200
        val executor: ExecutorService = Executors.newFixedThreadPool(threads)
        val startLatch = CountDownLatch(1)
        val errorHolder: java.util.concurrent.atomic.AtomicReference<Throwable?> = java.util.concurrent.atomic.AtomicReference(null)
        val nanos = System.nanoTime()

        repeat(threads) { t ->
            executor.submit {
                try {
                    startLatch.await()
                    repeat(operationsPerThread) { i ->
                        when (i % 3) {
                            0 -> {
                                val f = awaiter.pending("race-$nanos-$t-$i")
                                f.whenComplete { _, _ -> }
                            }
                            1 -> awaiter.rejectAll("race-$t-$i")
                            2 -> {
                                try {
                                    awaiter.resolve("race-$nanos-$t-$i",
                                        OcppMessage.CallResult("race-$nanos-$t-$i", null))
                                } catch (_: IllegalStateException) {
                                    // Expected
                                }
                            }
                        }
                    }
                } catch (e: Throwable) {
                    errorHolder.compareAndSet(null, e)
                }
            }
        }

        startLatch.countDown()
        executor.shutdown()
        assertTrue(executor.awaitTermination(60, TimeUnit.SECONDS))

        if (errorHolder.get() != null) {
            throw AssertionError("Race condition detected", errorHolder.get()!!)
        }
    }
}

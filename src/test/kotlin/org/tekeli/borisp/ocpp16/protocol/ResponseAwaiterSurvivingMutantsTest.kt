package org.tekeli.borisp.ocpp16.protocol

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mockito.doReturn
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ResponseAwaiterSurvivingMutantsTest {

    @Test
    fun `constructor with zero timeout must not throw`() {
        val executor = Executors.newSingleThreadScheduledExecutor()
        assertDoesNotThrow { ResponseAwaiter(executor, 0) }
        executor.shutdown()
    }

    @Test
    fun `pending future must not time out when timeout is zero`() {
        val executor = Executors.newSingleThreadScheduledExecutor()
        val awaiter = ResponseAwaiter(executor, 0)
        val future = awaiter.pending("m1")
        Thread.sleep(500)
        assertFalse(future.isDone)
        executor.shutdown()
    }

    @Test
    fun `cleanupTimedOut must skip already completed futures`() {
        val awaiter = ResponseAwaiter()
        val done = awaiter.pending("m1")
        val pending = awaiter.pending("m2")
        done.complete(OcppMessage.CallResult("m1", null))
        invokeCleanupTimedOut(awaiter)
        assertTrue(pending.isDone)
        val ex = assertThrows(ExecutionException::class.java) { pending.get() }
        assertTrue(ex.cause is TimeoutException)
        assertDoesNotThrow {
            awaiter.resolve("m1", OcppMessage.CallResult("m1", mapOf("status" to "Accepted")))
        }
    }

    @Test
    fun `rejectAll must cancel the periodic cleanup task`() {
        val scheduledFuture = mock(ScheduledFuture::class.java)
        val executor = mock(ScheduledExecutorService::class.java)
        doReturn(scheduledFuture)
            .`when`(executor)
            .scheduleAtFixedRate(any(Runnable::class.java), anyLong(), anyLong(), any(TimeUnit::class.java))
        val awaiter = ResponseAwaiter(executor, 1000)
        awaiter.rejectAll("closed")
        verify(scheduledFuture).cancel(false)
    }

    @Test
    fun `constructor with null executor and positive timeout must not throw`() {
        assertDoesNotThrow { ResponseAwaiter(null, 1000) }
    }

    @Test
    fun `rejectAll must short-circuit when already rejected`() {
        val scheduledFuture = mock(ScheduledFuture::class.java)
        val executor = mock(ScheduledExecutorService::class.java)
        doReturn(scheduledFuture)
            .`when`(executor)
            .scheduleAtFixedRate(any(Runnable::class.java), anyLong(), anyLong(), any(TimeUnit::class.java))
        val awaiter = ResponseAwaiter(executor, 1000)
        awaiter.rejectAll("first")
        awaiter.rejectAll("second")
        verify(scheduledFuture, times(1)).cancel(false)
    }

    private fun invokeCleanupTimedOut(awaiter: ResponseAwaiter) {
        val method = ResponseAwaiter::class.java.getDeclaredMethod("cleanupTimedOut")
        method.isAccessible = true
        method.invoke(awaiter)
    }
}

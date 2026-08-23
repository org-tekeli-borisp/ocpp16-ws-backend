package org.tekeli.borisp.ocpp16.websocket

import io.vertx.core.Vertx
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class VertxSchedulerTest {

    private lateinit var vertx: Vertx
    private lateinit var scheduler: VertxScheduler

    @BeforeEach
    fun setUp() {
        vertx = Vertx.vertx()
        scheduler = VertxScheduler(vertx)
    }

    @AfterEach
    fun tearDown() {
        try {
            vertx.close().toCompletionStage().toCompletableFuture().get(5, TimeUnit.SECONDS)
        } catch (_: Exception) {
        }
    }

    @Test
    fun `schedules task after delay`() {
        val executed = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        scheduler.schedule<Unit>({
            executed.set(true)
            latch.countDown()
        }, 50, TimeUnit.MILLISECONDS)

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Task should have executed")
        assertTrue(executed.get(), "Task runnable should have been called")
    }

    @Test
    fun `scheduled task runs on vertx event loop`() {
        val latch = CountDownLatch(1)

        scheduler.schedule<Unit>({
            assertTrue(io.vertx.core.Context.isOnEventLoopThread(), "Task must run on Vert.x event loop")
            latch.countDown()
        }, 10, TimeUnit.MILLISECONDS)

        assertTrue(latch.await(2, TimeUnit.SECONDS), "Task should have executed")
    }

    @Test
    fun `cancel prevents task from running`() {
        val executed = AtomicBoolean(false)
        val future = scheduler.schedule<Unit>({ executed.set(true) }, 100, TimeUnit.MILLISECONDS)

        assertTrue(future.cancel(false), "cancel should succeed")

        Thread.sleep(300)
        assertFalse(executed.get(), "task should not run after cancel")
    }

    @Test
    fun `cancel returns true on first call and false on second`() {
        val future = scheduler.schedule<Unit>({ }, 1000, TimeUnit.MILLISECONDS)

        assertTrue(future.cancel(false), "first cancel should return true")
        assertFalse(future.cancel(false), "second cancel should return false")
    }

    @Test
    fun `isCancelled and isDone reflect cancellation state`() {
        val future = scheduler.schedule<Unit>({ }, 1000, TimeUnit.MILLISECONDS)

        assertFalse(future.isCancelled, "not cancelled before cancel")
        assertFalse(future.isDone, "not done before cancel")

        future.cancel(false)

        assertTrue(future.isCancelled, "cancelled after cancel")
        assertTrue(future.isDone, "done after cancel")
    }

    private fun vertxTimeouts(): Map<Long, *> {
        val timeoutsField = io.vertx.core.impl.VertxImpl::class.java.getDeclaredField("timeouts")
        timeoutsField.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        return timeoutsField.get(vertx) as Map<Long, *>
    }

    @Test
    fun `cancel is idempotent and does not affect other timers`() {
        val future1 = scheduler.schedule<Unit>({ }, 100, TimeUnit.MILLISECONDS) as VertxScheduledFuture<*>
        val future2 = scheduler.schedule<Unit>({ }, 100, TimeUnit.MILLISECONDS) as VertxScheduledFuture<*>

        assertTrue(future1.cancel(false))
        assertFalse(future1.cancel(false), "second cancel must return false")

        val timeouts = vertxTimeouts()
        assertFalse(timeouts.containsKey(future1.timerId()), "cancelled timer must be removed from vertx registry")
        assertTrue(timeouts.containsKey(future2.timerId()), "unrelated timers must stay registered")
    }

    @Test
    fun `cancel removes the timer from the vertx timeout registry`() {
        val future = scheduler.schedule<Unit>({ }, 100, TimeUnit.MILLISECONDS) as VertxScheduledFuture<*>

        assertTrue(future.cancel(false))

        assertFalse(vertxTimeouts().containsKey(future.timerId()), "cancelled timer must be removed from vertx registry")
    }
}

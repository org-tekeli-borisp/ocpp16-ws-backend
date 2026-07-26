package org.tekeli.borisp.ocpp16

import io.smallrye.mutiny.Uni
import io.vertx.core.buffer.Buffer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.websocket.*
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

class OcppWebSocketServerPongTimeoutTest {

    @Test
    fun `pong timeout must close connection and set charge point offline when no pong received`() {
        var offlineCalled = false
        var offlineSessionId: String? = null
        val ps = object : PersistenceService() {
            override fun setChargePointOffline(sid: String) {
                offlineCalled = true
                offlineSessionId = sid
            }
        }

        val registry = ChargePointRegistry()
        val closed = AtomicBoolean(false)
        val closeCodeRef = AtomicReference<Int?>(null)
        val closeReasonRef = AtomicReference<String?>(null)

        val server = OcppWebSocketServer().apply {
            chargePointId = "PONG-CP"
            sessionId = "pong-timeout-session"
            chargePointRegistry = registry
            persistenceService = ps
        }

        registry.register("pong-timeout-session", "pong-timeout-session", server, "PONG-CP")

        val target = object : PingPongTarget {
            override fun sendPing(buffer: Buffer): Uni<Void> = Uni.createFrom().voidItem()
            override fun closeConnection(reason: String): Uni<Void> {
                closeCodeRef.set(1001)
                closeReasonRef.set(reason)
                closed.set(true)
                return Uni.createFrom().voidItem()
            }
            override fun setChargePointOffline(id: String) = ps.setChargePointOffline(id)
            override fun unregisterFromRegistry(id: String) = registry.unregister(id)
            override fun isConnected(id: String): Boolean = registry.isConnected(id)
            override fun rejectAwaiter(message: String) {}
            override fun executeAsync(runnable: Runnable) = runnable.run()
        }

        val scheduler = TestScheduler()
        val manager = PingPongManager(target, "pong-timeout-session", 1, 2, scheduler)
        manager.start()

        scheduler.executeNext() // Execute ping (isPinging = true, schedules pong timeout)
        scheduler.executeNext() // Execute pong timeout

        assertTrue(offlineCalled, "setChargePointOffline must be called when pong timeout fires")
        assertEquals("pong-timeout-session", offlineSessionId)
        assertTrue(closed.get(), "Connection should have been closed")
        assertEquals(1001, closeCodeRef.get())
        assertEquals("Pong timeout", closeReasonRef.get())
    }

    @Test
    fun `pong received before timeout must NOT close connection`() {
        var offlineCalled = false
        val ps = object : PersistenceService() {
            override fun setChargePointOffline(sid: String) {
                offlineCalled = true
            }
        }

        val registry = ChargePointRegistry()
        val closed = AtomicBoolean(false)

        val server = OcppWebSocketServer().apply {
            chargePointId = "PONG-OK-CP"
            sessionId = "pong-ok-session"
            chargePointRegistry = registry
            persistenceService = ps
        }

        registry.register("pong-ok-session", "pong-ok-session", server, "PONG-OK-CP")

        val target = object : PingPongTarget {
            override fun sendPing(buffer: Buffer): Uni<Void> = Uni.createFrom().voidItem()
            override fun closeConnection(reason: String): Uni<Void> {
                closed.set(true)
                return Uni.createFrom().voidItem()
            }
            override fun setChargePointOffline(id: String) = ps.setChargePointOffline(id)
            override fun unregisterFromRegistry(id: String) = registry.unregister(id)
            override fun isConnected(id: String): Boolean = registry.isConnected(id)
            override fun rejectAwaiter(message: String) {}
            override fun executeAsync(runnable: Runnable) = runnable.run()
        }

        val scheduler = TestScheduler()
        val manager = PingPongManager(target, "pong-ok-session", 1, 2, scheduler)
        manager.start()

        scheduler.executeNext() // Execute ping (isPinging = true)
        manager.pongReceived() // Simulate pong arrival

        scheduler.executeNext() // Execute rescheduled ping — pong timeout was cancelled, no-op expected

        assertFalse(offlineCalled, "setChargePointOffline must NOT be called when pong is received in time")
        assertFalse(closed.get(), "Connection must NOT be closed when pong is received in time")
    }
}

class TestScheduler : Scheduler {
    private val tasks = mutableListOf<TestScheduledTask<*>>()
    var cancelledCount = 0

    fun hasScheduledTasks(): Boolean = tasks.isNotEmpty()

    fun executeNext() {
        if (tasks.isEmpty()) return
        val task = tasks.removeAt(0)
        task.runnable.run()
    }

    @Suppress("UNCHECKED_CAST")
    override fun <V : Any?> schedule(
        runnable: Runnable,
        delay: Long,
        unit: TimeUnit
    ): java.util.concurrent.ScheduledFuture<V> {
        val task = TestScheduledTask<V>(runnable, delay, unit) { cancelledCount++ }
        tasks.add(task)
        return task
    }
}

class TestScheduledTask<V>(
    val runnable: Runnable,
    val delay: Long,
    val unit: TimeUnit,
    val onCancel: () -> Unit = {}
) : java.util.concurrent.ScheduledFuture<V> {
    private var cancelled = false

    override fun get(): V { throw UnsupportedOperationException() }
    override fun get(timeout: Long, unit: TimeUnit): V { throw UnsupportedOperationException() }
    override fun isCancelled(): Boolean = cancelled
    override fun isDone(): Boolean = cancelled
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (cancelled) return false
        cancelled = true
        onCancel()
        return true
    }
    override fun getDelay(unit: TimeUnit): Long = 0
    override fun compareTo(other: Delayed): Int = 0
}

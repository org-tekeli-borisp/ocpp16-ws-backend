package org.tekeli.borisp.ocpp16.websocket

import io.vertx.core.buffer.Buffer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.*

class PingPongManagerTest {

    private val target = MockPingPongTarget()
    private val scheduler = TestingScheduler()
    private lateinit var manager: PingPongManager

    @BeforeEach
    fun setUp() {
        manager = PingPongManager(
            target = target,
            sessionId = "test-session",
            pingInterval = 1,
            pongTimeout = 2,
            scheduler = scheduler
        )
    }

    @Test
    fun `start schedules ping and stop cleans up`() {
        manager.start()
        assertTrue(scheduler.hasScheduledTasks(), "Should have scheduled tasks after start")

        manager.stop()
        assertTrue(scheduler.hasCancelledTasks(), "Should have cancelled tasks after stop")
        assertFalse(manager.isPinging, "isPinging should be false after stop")
    }

    @Test
    fun `executing ping sends ping and schedules pong timeout`() {
        manager.start()
        scheduler.executeNext() // Execute the ping task

        assertEquals(1, target.pingSendCount, "Should have sent one ping")
        assertTrue(scheduler.hasScheduledTasks(), "Should have scheduled pong timeout")
        assertTrue(manager.isPinging, "isPinging should be true after sending ping")
    }

    @Test
    fun `executing pong timeout closes connection when no pong received`() {
        manager.start()
        scheduler.executeNext() // Execute ping
        assertTrue(manager.isPinging)

        scheduler.executeNext() // Execute pong timeout

        assertEquals(1, target.closeCount, "Should close connection on pong timeout")
        assertFalse(manager.isPinging, "isPinging should be false after timeout")
    }

    @Test
    fun `pongReceived cancels timeout and reschedules ping`() {
        manager.start()
        scheduler.executeNext() // Execute ping
        assertTrue(manager.isPinging)

        manager.pongReceived()
        assertFalse(manager.isPinging, "isPinging should be false after pong")
        assertTrue(scheduler.hasCancelledTasks(), "Should have cancelled pong timeout")
        assertTrue(scheduler.hasScheduledTasks(), "Should have rescheduled ping after pong")
    }

    @Test
    fun `messageReceived cancels timeout and reschedules ping`() {
        manager.start()
        scheduler.executeNext() // Execute ping
        assertTrue(manager.isPinging)

        manager.messageReceived()
        assertFalse(manager.isPinging, "isPinging should be false after message")
        assertTrue(scheduler.hasCancelledTasks(), "Should have cancelled pong timeout")
        assertTrue(scheduler.hasScheduledTasks(), "Should have rescheduled ping after message")
    }

    @Test
    fun `does not send duplicate ping when already pinging`() {
        manager.start()
        scheduler.executeNext() // Execute ping
        assertEquals(1, target.pingSendCount)

        scheduler.executeNext() // This is the pong timeout
        // After pong timeout, isPinging is false but no new ping is scheduled automatically
        // A new ping is only scheduled when messageReceived() or pongReceived() is called

        // Simulate receiving a message to trigger reschedule
        manager.messageReceived()
        scheduler.executeNext() // Next ping
        assertEquals(2, target.pingSendCount, "Should only have sent expected pings")
    }

    @Test
    fun `pong timeout should call unregister when charge point is connected`() {
        manager.start()
        scheduler.executeNext() // Execute ping
        scheduler.executeNext() // Execute pong timeout

        assertEquals(1, target.setOfflineCount)
        assertEquals(1, target.unregisterCount)
        assertEquals(1, target.rejectAwaiterCount)
    }

    @Test
    fun `stop cancels pong timeout future`() {
        manager.start()
        scheduler.executeNext() // Execute ping, which schedules pong timeout
        assertTrue(scheduler.hasScheduledTasks())

        manager.stop()
        assertTrue(scheduler.hasCancelledTasks())
    }

    @Test
    fun `ping failure triggers connection close`() {
        target.failNextPing = true
        manager.start()
        scheduler.executeNext() // Execute ping (will fail)

        assertEquals(1, target.closeCount, "Should close connection on ping failure")
        assertFalse(manager.isPinging, "isPinging should be false after ping failure")
    }

    @Test
    fun `stop while pinging resets state`() {
        manager.start()
        scheduler.executeNext() // Execute ping
        assertTrue(manager.isPinging)

        manager.stop()
        assertFalse(manager.isPinging, "isPinging should be false after stop")
        assertTrue(scheduler.hasCancelledTasks())
    }

    @Test
    fun `multiple start-stop cycles work correctly`() {
        manager.start()
        manager.stop()
        manager.start()
        manager.stop()

        assertTrue(scheduler.hasCancelledTasks())
        assertFalse(manager.isPinging)
    }

    @Test
    fun `pong timeout does not unregister when charge point already disconnected`() {
        target.connected = false
        manager.start()
        scheduler.executeNext()
        scheduler.executeNext()

        assertEquals(1, target.closeCount)
        assertEquals(0, target.unregisterCount, "Should NOT unregister when already disconnected")
        assertEquals(0, target.rejectAwaiterCount, "Should NOT reject awaiter when already disconnected")
    }

    @Test
    fun `pong timeout unsubscribes closeConnection properly`() {
        manager.start()
        scheduler.executeNext()
        scheduler.executeNext()

        assertEquals(1, target.closeCount)
    }

    @Test
    fun `ping success callback is invoked`() {
        manager.start()
        scheduler.executeNext()

        assertEquals(1, target.pingSuccessCallbackCount)
    }

    @Test
    fun `does not send duplicate ping when ping task fires while already pinging`() {
        manager.start()
        val pingTask = scheduler.firstTask()

        pingTask.runnable.run()
        assertEquals(1, target.pingSendCount)
        assertTrue(manager.isPinging)

        pingTask.runnable.run()
        assertEquals(1, target.pingSendCount, "should not send a duplicate ping while already pinging")
    }

    @Test
    fun `pongReceived cancels the pending pong timeout task`() {
        manager.start()
        scheduler.executeNext()
        val pongTimeoutTask = scheduler.firstPendingTask()
        assertFalse(pongTimeoutTask.isCancelled)

        manager.pongReceived()

        assertTrue(pongTimeoutTask.isCancelled, "pong timeout task should be cancelled")
    }

    @Test
    fun `pong timeout subscribes to closeConnection`() {
        manager.start()
        scheduler.executeNext()
        scheduler.executeNext()

        assertEquals(1, target.closeCount)
        assertEquals(1, target.closeSubscribedCount, "closeConnection should be subscribed on pong timeout")
    }

    @Test
    fun `pong timeout does not close connection when no longer pinging`() {
        manager.start()
        scheduler.executeNext()
        val pongTimeoutTask = scheduler.firstPendingTask()

        manager.pongReceived()
        assertFalse(manager.isPinging)

        pongTimeoutTask.runnable.run()
        assertEquals(0, target.closeCount, "should not close connection when no longer pinging")
    }

    @Test
    fun `ping task resets state when pong timeout scheduling fails`() {
        val pingRunnables = mutableListOf<Runnable>()
        val failingScheduler = object : Scheduler {
            var callCount = 0
            override fun <V : Any?> schedule(
                runnable: Runnable,
                delay: Long,
                unit: TimeUnit
            ): java.util.concurrent.ScheduledFuture<V> {
                callCount++
                if (callCount == 2) throw RuntimeException("schedule failed")
                pingRunnables.add(runnable)
                return object : java.util.concurrent.ScheduledFuture<V> {
                    override fun get(): V = throw UnsupportedOperationException()
                    override fun get(timeout: Long, unit: TimeUnit): V = throw UnsupportedOperationException()
                    override fun isCancelled(): Boolean = false
                    override fun isDone(): Boolean = false
                    override fun cancel(mayInterruptIfRunning: Boolean): Boolean = true
                    override fun getDelay(unit: TimeUnit): Long = 0
                    override fun compareTo(other: Delayed): Int = 0
                }
            }
        }
        val mgr = PingPongManager(target, "test-session", 1, 2, failingScheduler)
        mgr.start()
        pingRunnables[0].run()

        assertEquals(0, target.pingSendCount, "no ping should be sent when pong timeout scheduling fails")
        assertFalse(mgr.isPinging, "isPinging should be reset after scheduling failure")
    }

    @Test
    fun `ping failure close throwing synchronously does not propagate`() {
        target.failNextPing = true
        target.closeThrowsSynchronously = true
        manager.start()

        assertDoesNotThrow { scheduler.executeNext() }

        assertEquals(1, target.closeCount, "close should have been attempted")
        assertFalse(manager.isPinging, "isPinging should be false after ping failure")
    }

    @Test
    fun `pong timeout continues processing when closeConnection throws synchronously`() {
        target.closeThrowsSynchronously = true
        manager.start()
        scheduler.executeNext()
        scheduler.executeNext()

        assertEquals(1, target.setOfflineCount, "setChargePointOffline should still be called")
        assertEquals(1, target.unregisterCount, "unregister should still be called")
        assertEquals(1, target.rejectAwaiterCount, "rejectAwaiter should still be called")
    }

    @Test
    fun `pong timeout continues processing when setChargePointOffline throws`() {
        target.setOfflineThrows = true
        manager.start()
        scheduler.executeNext()
        scheduler.executeNext()

        assertEquals(1, target.unregisterCount, "unregister should still be called")
        assertEquals(1, target.rejectAwaiterCount, "rejectAwaiter should still be called")
    }
}

class MockPingPongTarget : PingPongTarget {
    var pingSendCount = 0
    var pingSuccessCallbackCount = 0
    var closeCount = 0
    var closeSubscribedCount = 0
    var setOfflineCount = 0
    var unregisterCount = 0
    var rejectAwaiterCount = 0
    var failNextPing = false
    var connected = true
    var closeThrows = false
    var closeThrowsSynchronously = false
    var setOfflineThrows = false

    override fun sendPing(buffer: Buffer): io.smallrye.mutiny.Uni<Void> {
        pingSendCount++
        if (failNextPing) {
            failNextPing = false
            return io.smallrye.mutiny.Uni.createFrom().failure(RuntimeException("Ping failed"))
        }
        return io.smallrye.mutiny.Uni.createFrom().voidItem()
            .onItem().invoke(Runnable { pingSuccessCallbackCount++ })
    }

    override fun closeConnection(reason: String): io.smallrye.mutiny.Uni<Void> {
        closeCount++
        if (closeThrowsSynchronously) {
            throw RuntimeException("Close failed synchronously")
        }
        if (closeThrows) {
            return io.smallrye.mutiny.Uni.createFrom().failure(RuntimeException("Close failed"))
        }
        return io.smallrye.mutiny.Uni.createFrom().voidItem()
            .onItem().invoke(Runnable { closeSubscribedCount++ })
    }

    override fun setChargePointOffline(sessionId: String) {
        setOfflineCount++
        if (setOfflineThrows) {
            throw RuntimeException("Set offline failed")
        }
    }

    override fun unregisterFromRegistry(sessionId: String) {
        unregisterCount++
        connected = false
    }

    override fun isConnected(sessionId: String): Boolean = connected

    override fun rejectAwaiter(message: String) {
        rejectAwaiterCount++
    }

    override fun executeAsync(runnable: Runnable) {
        runnable.run()
    }
}

class TestingScheduler : Scheduler {
    private val pendingTasks = mutableListOf<ScheduledTask<*>>()
    private val allTasks = mutableListOf<ScheduledTask<*>>()

    fun hasScheduledTasks(): Boolean = pendingTasks.isNotEmpty()
    fun hasCancelledTasks(): Boolean = allTasks.any { it.isCancelled }
    fun firstTask(): ScheduledTask<*> = allTasks.first()
    fun firstPendingTask(): ScheduledTask<*> = pendingTasks.first()

    fun executeNext() {
        if (pendingTasks.isEmpty()) return
        val task = pendingTasks.removeAt(0)
        task.runnable.run()
    }

    override fun <V : Any?> schedule(
        runnable: Runnable,
        delay: Long,
        unit: TimeUnit
    ): ScheduledTask<V> {
        val task = ScheduledTask<V>(runnable, delay, unit)
        pendingTasks.add(task)
        allTasks.add(task)
        return task
    }
}

class ScheduledTask<V>(
    val runnable: Runnable,
    val delay: Long,
    val unit: TimeUnit
) : java.util.concurrent.ScheduledFuture<V> {
    private var cancelled = false

    override fun get(): V { throw UnsupportedOperationException() }
    override fun get(timeout: Long, unit: TimeUnit): V { throw UnsupportedOperationException() }
    override fun isCancelled(): Boolean = cancelled
    override fun isDone(): Boolean = cancelled
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (cancelled) return false
        cancelled = true
        return true
    }
    override fun getDelay(unit: TimeUnit): Long = 0
    override fun compareTo(other: Delayed): Int = 0
}

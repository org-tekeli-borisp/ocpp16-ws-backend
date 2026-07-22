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
    fun `pongReceived cancels timeout and resets isPinging`() {
        manager.start()
        scheduler.executeNext() // Execute ping
        assertTrue(manager.isPinging)

        manager.pongReceived()
        assertFalse(manager.isPinging, "isPinging should be false after pong")
        assertTrue(scheduler.hasCancelledTasks(), "Should have cancelled pong timeout")
    }

    @Test
    fun `messageReceived cancels timeout and resets isPinging`() {
        manager.start()
        scheduler.executeNext() // Execute ping
        assertTrue(manager.isPinging)

        manager.messageReceived()
        assertFalse(manager.isPinging, "isPinging should be false after message")
        assertTrue(scheduler.hasCancelledTasks(), "Should have cancelled pong timeout")
    }

    @Test
    fun `does not send duplicate ping when already pinging`() {
        manager.start()
        scheduler.executeNext() // Execute ping
        assertEquals(1, target.pingSendCount)

        scheduler.executeNext() // This is the pong timeout, not another ping
        // After the pong timeout executes, isPinging is false again

        // The next scheduled ping should only send one ping
        scheduler.executeNext() // Next ping
        assertEquals(2, target.pingSendCount, "Should not have sent extra pings")
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
}

class MockPingPongTarget : PingPongTarget {
    var pingSendCount = 0
    var closeCount = 0
    var setOfflineCount = 0
    var unregisterCount = 0
    var rejectAwaiterCount = 0
    var failNextPing = false
    var connected = true

    override fun sendPing(buffer: Buffer): io.smallrye.mutiny.Uni<Void> {
        pingSendCount++
        if (failNextPing) {
            failNextPing = false
            return io.smallrye.mutiny.Uni.createFrom().failure(RuntimeException("Ping failed"))
        }
        return io.smallrye.mutiny.Uni.createFrom().voidItem()
    }

    override fun closeConnection(reason: String): io.smallrye.mutiny.Uni<Void> {
        closeCount++
        return io.smallrye.mutiny.Uni.createFrom().voidItem()
    }

    override fun setChargePointOffline(sessionId: String) {
        setOfflineCount++
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
    private val tasks = mutableListOf<ScheduledTask<*>>()
    private var cancelledCount = 0

    fun hasScheduledTasks(): Boolean = tasks.isNotEmpty()
    fun hasCancelledTasks(): Boolean = cancelledCount > 0

    fun executeNext() {
        if (tasks.isEmpty()) return
        val task = tasks.removeAt(0)
        task.runnable.run()
        if (task.period != null && !task.isCancelled) {
            tasks.add(task)
        }
    }

    override fun <V : Any?> schedule(
        runnable: Runnable,
        delay: Long,
        unit: TimeUnit
    ): ScheduledTask<V> {
        val task = ScheduledTask<V>(runnable, delay, unit, null) {
            cancelledCount++
        }
        tasks.add(task)
        return task
    }

    override fun <V : Any?> scheduleAtFixedRate(
        runnable: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit
    ): ScheduledTask<V> {
        val task = ScheduledTask<V>(runnable, initialDelay, unit, period) {
            cancelledCount++
        }
        tasks.add(task)
        return task
    }
}

class ScheduledTask<V>(
    val runnable: Runnable,
    val delay: Long,
    val unit: TimeUnit,
    val period: Long?,
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

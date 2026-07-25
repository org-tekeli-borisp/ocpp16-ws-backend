package org.tekeli.borisp.ocpp16.websocket

import io.vertx.core.buffer.Buffer
import java.util.concurrent.Delayed
import java.util.concurrent.TimeUnit

interface Scheduler {

    fun <V : Any?> schedule(
        runnable: Runnable,
        delay: Long,
        unit: TimeUnit
    ): java.util.concurrent.ScheduledFuture<V>

    fun <V : Any?> scheduleAtFixedRate(
        runnable: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit
    ): java.util.concurrent.ScheduledFuture<V>
}

class VertxScheduler(
    private val vertx: io.vertx.core.Vertx
) : Scheduler {

    override fun <V : Any?> schedule(
        runnable: Runnable,
        delay: Long,
        unit: TimeUnit
    ): java.util.concurrent.ScheduledFuture<V> {
        val timerId = vertx.setTimer(unit.toMillis(delay)) { runnable.run() }
        return VertxScheduledFuture<V>(timerId) { vertx.cancelTimer(timerId) }
    }

    override fun <V : Any?> scheduleAtFixedRate(
        runnable: Runnable,
        initialDelay: Long,
        period: Long,
        unit: TimeUnit
    ): java.util.concurrent.ScheduledFuture<V> {
        val timerId = vertx.setPeriodic(unit.toMillis(initialDelay), unit.toMillis(period)) { runnable.run() }
        return VertxScheduledFuture<V>(timerId) { vertx.cancelTimer(timerId) }
    }
}

private class VertxScheduledFuture<V : Any?>(
    private val timerId: Long,
    private val cancelAction: () -> Unit
) : java.util.concurrent.ScheduledFuture<V> {
    private var cancelled = false

    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        if (cancelled) return false
        cancelled = true
        cancelAction()
        return true
    }

    override fun isCancelled(): Boolean = cancelled
    override fun isDone(): Boolean = cancelled
    override fun get(): V = throw java.util.concurrent.CancellationException("VertxScheduledFuture.get() not supported")
    override fun get(timeout: Long, unit: TimeUnit?): V = throw java.util.concurrent.CancellationException("VertxScheduledFuture.get() not supported")
    override fun getDelay(unit: TimeUnit?): Long = 0
    override fun compareTo(other: Delayed?): Int = 0
}

interface PingPongTarget {

    fun sendPing(buffer: Buffer): io.smallrye.mutiny.Uni<Void>

    fun closeConnection(reason: String): io.smallrye.mutiny.Uni<Void>

    fun setChargePointOffline(sessionId: String)

    fun unregisterFromRegistry(sessionId: String)

    fun isConnected(sessionId: String): Boolean

    fun rejectAwaiter(message: String)

    fun executeAsync(runnable: Runnable)
}

class PingPongManager(
    private val target: PingPongTarget,
    private val sessionId: String,
    private val pingInterval: Long,
    private val pongTimeout: Long,
    private val scheduler: Scheduler
) {

    val isPinging: Boolean
        get() = isPingingFlag.get()

    private val isPingingFlag = java.util.concurrent.atomic.AtomicBoolean(false)
    private var pingFuture: java.util.concurrent.ScheduledFuture<*>? = null
    private var pongTimeoutFuture: java.util.concurrent.ScheduledFuture<*>? = null

    fun start() {
        stop()
        pingFuture = scheduler.scheduleAtFixedRate<Unit>({
            sendPingAndScheduleTimeout()
        }, pingInterval, pingInterval, TimeUnit.SECONDS)
    }

    fun stop() {
        pingFuture?.cancel(false)
        pingFuture = null
        cancelPongTimeout()
        isPingingFlag.set(false)
    }

    fun pongReceived() {
        cancelPongTimeout()
        isPingingFlag.set(false)
    }

    fun messageReceived() {
        cancelPongTimeout()
        isPingingFlag.set(false)
    }

    private fun sendPingAndScheduleTimeout() {
        if (!isPingingFlag.compareAndSet(false, true)) return

        try {
            schedulePongTimeout()
        } catch (e: Exception) {
            cancelPongTimeout()
            isPingingFlag.set(false)
            return
        }

        target.sendPing(Buffer.buffer())
            .subscribe()
            .with(
                { },
                {
                    target.executeAsync {
                        cancelPongTimeout()
                        isPingingFlag.set(false)
                        try {
                            target.closeConnection("Ping failed")
                        } catch (_: Exception) {}
                    }
                }
            )
    }

    private fun schedulePongTimeout() {
        pongTimeoutFuture = scheduler.schedule<Unit>({
            if (isPingingFlag.compareAndSet(true, false)) {
                handlePongTimeout()
            }
        }, pongTimeout, TimeUnit.SECONDS)
    }

    private fun cancelPongTimeout() {
        pongTimeoutFuture?.cancel(false)
        pongTimeoutFuture = null
    }

    private fun handlePongTimeout() {
        target.executeAsync {
            try {
                target.closeConnection("Pong timeout")
            } catch (_: Exception) {}
            try {
                target.setChargePointOffline(sessionId)
            } catch (_: Exception) {}
            if (target.isConnected(sessionId)) {
                target.unregisterFromRegistry(sessionId)
                target.rejectAwaiter("WebSocket connection closed: pong timeout $sessionId")
            }
        }
    }
}

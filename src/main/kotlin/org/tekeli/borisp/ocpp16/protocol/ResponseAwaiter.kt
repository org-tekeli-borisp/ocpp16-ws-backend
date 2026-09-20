package org.tekeli.borisp.ocpp16.protocol

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class ResponseAwaiter(
    executor: ScheduledExecutorService? = null,
    private val timeoutMillis: Long = 0
) {
    private class PendingEntry(val future: CompletableFuture<OcppMessage>, val deadline: Long)

    private val pendingResponses = ConcurrentHashMap<String, PendingEntry>()
    private val isRejected = AtomicBoolean(false)
    private val timeoutHandle: ScheduledFuture<*>? = if (executor != null && timeoutMillis > 0) {
        executor.scheduleAtFixedRate(
            { cleanupTimedOut() },
            timeoutMillis,
            timeoutMillis,
            TimeUnit.MILLISECONDS
        )
    } else {
        null
    }

    fun pending(messageId: String): CompletableFuture<OcppMessage> {
        if (isRejected.get()) {
            val f = CompletableFuture<OcppMessage>()
            f.completeExceptionally(IllegalStateException("ResponseAwaiter has been rejected"))
            return f
        }
        val future = CompletableFuture<OcppMessage>()
        pendingResponses[messageId] = PendingEntry(future, System.currentTimeMillis() + timeoutMillis)
        return future
    }

    fun resolve(messageId: String, response: OcppMessage.CallResult) {
        val entry = pendingResponses.remove(messageId)
            ?: throw IllegalStateException("No pending response for messageId: $messageId")
        entry.future.complete(response)
    }

    fun reject(messageId: String, error: OcppMessage.CallError) {
        val entry = pendingResponses.remove(messageId)
            ?: throw IllegalStateException("No pending response for messageId: $messageId")
        entry.future.complete(error)
    }

    fun timeout(messageId: String, cause: TimeoutException) {
        val entry = pendingResponses.remove(messageId)
            ?: throw IllegalStateException("No pending response for messageId: $messageId")
        entry.future.completeExceptionally(cause)
    }

    fun rejectAll(reason: String) {
        if (!isRejected.compareAndSet(false, true)) return
        val exception = IllegalStateException(reason)
        val entries = pendingResponses.entries.toList()
        pendingResponses.clear()
        timeoutHandle?.cancel(false)
        entries.forEach { (_, entry) ->
            entry.future.completeExceptionally(exception)
        }
    }

    private fun cleanupTimedOut() {
        val now = System.currentTimeMillis()
        val expired = pendingResponses.entries.filter { (_, entry) -> !entry.future.isDone && entry.deadline <= now }
        expired.forEach { (messageId, _) ->
            try {
                timeout(messageId, TimeoutException("Command timed out"))
            } catch (_: IllegalStateException) {
            }
        }
    }
}

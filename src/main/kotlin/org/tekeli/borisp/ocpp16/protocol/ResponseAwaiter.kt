package org.tekeli.borisp.ocpp16.protocol

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean

class ResponseAwaiter(
    executor: ScheduledExecutorService? = null,
    timeoutMillis: Long = 0
) {
    private val pendingResponses = ConcurrentHashMap<String, CompletableFuture<OcppMessage>>()
    private val isRejected = AtomicBoolean(false)
    private val timeoutHandle = if (executor != null && timeoutMillis > 0) {
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
        pendingResponses[messageId] = future
        return future
    }

    fun resolve(messageId: String, response: OcppMessage.CallResult) {
        val future = pendingResponses.remove(messageId)
            ?: throw IllegalStateException("No pending response for messageId: $messageId")
        future.complete(response)
    }

    fun reject(messageId: String, error: OcppMessage.CallError) {
        val future = pendingResponses.remove(messageId)
            ?: throw IllegalStateException("No pending response for messageId: $messageId")
        future.complete(error)
    }

    fun timeout(messageId: String, cause: TimeoutException) {
        val future = pendingResponses.remove(messageId)
            ?: throw IllegalStateException("No pending response for messageId: $messageId")
        future.completeExceptionally(cause)
    }

    fun rejectAll(reason: String) {
        if (!isRejected.compareAndSet(false, true)) return
        val exception = IllegalStateException(reason)
        val entries = pendingResponses.entries.toList()
        pendingResponses.clear()
        timeoutHandle?.cancel(false)
        entries.forEach { (_, future) ->
            future.completeExceptionally(exception)
        }
    }

    private fun cleanupTimedOut() {
        val toTimeout = mutableListOf<String>()
        for ((messageId, future) in pendingResponses) {
            if (!future.isDone) {
                toTimeout.add(messageId)
            }
        }
        toTimeout.forEach { messageId ->
            timeout(messageId, TimeoutException("Command timed out"))
        }
    }
}

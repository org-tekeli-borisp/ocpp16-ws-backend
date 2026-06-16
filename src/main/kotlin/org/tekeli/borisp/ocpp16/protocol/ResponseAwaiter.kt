package org.tekeli.borisp.ocpp16.protocol

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

class ResponseAwaiter {
    private val pendingResponses = ConcurrentHashMap<String, CompletableFuture<OcppMessage>>()
    private val isRejected = AtomicBoolean(false)

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

    fun timeout(messageId: String, cause: java.util.concurrent.TimeoutException) {
        val future = pendingResponses.remove(messageId)
            ?: throw IllegalStateException("No pending response for messageId: $messageId")
        future.completeExceptionally(cause)
    }

    fun rejectAll(reason: String) {
        if (!isRejected.compareAndSet(false, true)) return
        val exception = IllegalStateException(reason)
        val entries = pendingResponses.entries.toList()
        pendingResponses.clear()
        entries.forEach { (_, future) ->
            future.completeExceptionally(exception)
        }
    }
}

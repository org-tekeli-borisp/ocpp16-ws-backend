package org.tekeli.borisp.ocpp16

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap

class ResponseAwaiter {
    private val pendingResponses = ConcurrentHashMap<String, CompletableFuture<OcppMessage>>()

    fun pending(messageId: String): CompletableFuture<OcppMessage> {
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
}

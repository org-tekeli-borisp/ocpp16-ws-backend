package org.tekeli.borisp.ocpp16

import io.quarkus.websockets.next.OpenConnections
import io.smallrye.mutiny.Uni
import java.util.UUID
import java.util.concurrent.CompletableFuture

class WsSender(
    private val openConnections: OpenConnections,
    private val connectionId: String
) : TextSender {
    override fun sendText(text: String): Uni<Void> {
        return openConnections.findByConnectionId(connectionId)
            .orElse(null)
            ?.sendText(text)
            ?: Uni.createFrom().failure(IllegalStateException("WebSocket connection not found: $connectionId"))
    }
}

@FunctionalInterface
interface TextSender {
    fun sendText(text: String): Uni<Void>
}

class OutboundCallDispatcher(
    private val sender: TextSender,
    private val responseAwaiter: ResponseAwaiter
) {
    fun sendCall(action: String, payload: Map<String, Any>?): CompletableFuture<OcppMessage> {
        val messageId = UUID.randomUUID().toString()
        val call = OcppMessage.Call(
            messageId = messageId,
            action = action,
            payload = payload
        )
        val future = responseAwaiter.pending(messageId)

        sender.sendText(call.toJson())
            .subscribe()
            .with(
                { },
                { error -> future.completeExceptionally(error) }
            )

        return future
    }
}

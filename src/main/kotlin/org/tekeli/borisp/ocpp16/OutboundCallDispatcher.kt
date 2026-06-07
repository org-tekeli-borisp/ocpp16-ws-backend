package org.tekeli.borisp.ocpp16

import io.quarkus.websockets.next.WebSocketConnection
import io.smallrye.mutiny.Uni
import java.util.UUID
import java.util.concurrent.CompletableFuture

@JvmInline
value class WsSender(val delegate: WebSocketConnection) : TextSender {
    override fun sendText(text: String): Uni<Void> = delegate.sendText(text)
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
        return future
    }
}

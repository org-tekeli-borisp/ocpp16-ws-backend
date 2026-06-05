package org.tekeli.borisp.ocpp16

import java.util.UUID
import java.util.concurrent.CompletableFuture

class OutboundCallDispatcher(
    private val connection: WebSocketSend,
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
        connection.sendText(call.toJson())
        return future
    }
}

interface WebSocketSend {
    fun sendText(text: String)
}

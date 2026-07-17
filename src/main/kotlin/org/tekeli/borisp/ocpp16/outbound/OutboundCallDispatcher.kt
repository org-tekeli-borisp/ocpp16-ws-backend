package org.tekeli.borisp.ocpp16.outbound

import io.quarkus.websockets.next.OpenConnections
import io.smallrye.mutiny.Uni
import org.tekeli.borisp.ocpp16.protocol.MessageCaptureService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppMessageDirection
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import java.util.UUID
import java.util.concurrent.CompletableFuture

class WsSender(
    private val openConnections: OpenConnections,
    private val connectionId: String
) : TextSender {
    override fun sendText(text: String): Uni<Void> {
        val conn = openConnections.findByConnectionId(connectionId).orElse(null)
        if (conn != null) return conn.sendText(text)
        return Uni.createFrom().failure(IllegalStateException("ChargePoint not connected: $connectionId"))
    }
}

@FunctionalInterface
interface TextSender {
    fun sendText(text: String): Uni<Void>
}

class OutboundCallDispatcher(
    private val sender: TextSender,
    private val responseAwaiter: ResponseAwaiter,
    private val messageCaptureService: MessageCaptureService? = null,
) {
    fun sendCall(action: String, payload: Map<String, Any>?): CompletableFuture<OcppMessage> {
        return sendCall("", action, payload)
    }

    fun sendCall(chargePointId: String, action: String, payload: Map<String, Any>?): CompletableFuture<OcppMessage> {
        val messageId = UUID.randomUUID().toString()
        val call = OcppMessage.Call(
            messageId = messageId,
            action = action,
            payload = payload
        )

        if (chargePointId.isNotEmpty() && messageCaptureService != null) {
            messageCaptureService.capture(chargePointId, OcppMessageDirection.OUTBOUND, call)
        }

        val future = responseAwaiter.pending(messageId)

        if (chargePointId.isNotEmpty() && messageCaptureService != null) {
            future.thenAccept { response ->
                messageCaptureService.capture(chargePointId, OcppMessageDirection.INBOUND, response)
            }
        }

        sender.sendText(call.toJson())
            .onFailure()
            .invoke { error -> future.completeExceptionally(error) }
            .subscribe()
            .asCompletionStage()

        return future
    }
}

package org.tekeli.borisp.ocpp16

import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnOpen
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.WebSocketConnection
import jakarta.inject.Inject
import java.util.UUID

@WebSocket(path = "/ocpp")
class OcppWebSocketServer {

    @Inject
    var connection: WebSocketConnection? = null

    val activeConnection: WebSocketConnection
        get() = connection ?: throw IllegalStateException("Connection not initialized")

    private val sessionId = UUID.randomUUID().toString()
    private val handlers: Map<String, OcppActionHandler> = mapOf(
        "BootNotification" to BootNotificationHandler(),
        "Heartbeat" to HeartbeatHandler(),
        "Authorize" to AuthorizeHandler(),
        "StartTransaction" to StartTransactionHandler(),
        "StopTransaction" to StopTransactionHandler(),
        "StatusNotification" to StatusNotificationHandler(),
        "DataTransfer" to DataTransferHandler(),
        "FirmwareStatusNotification" to FirmwareStatusNotificationHandler(),
        "DiagnosticsStatusNotification" to DiagnosticsStatusNotificationHandler(),
        "MeterValues" to MeterValuesHandler()
    )

    @OnOpen
    fun onOpen() {
        println("WebSocket connection opened: $sessionId")
    }

    @OnTextMessage
    fun onTextMessage(message: String): String {
        val response = try {
            val ocppMessage = OcppMessage.parse(message)

            when (ocppMessage.type) {
                OcppMessageType.CALL -> handleCall(ocppMessage as OcppMessage.Call)
                OcppMessageType.CALLRESULT -> OcppMessage.CallError(
                    messageId = ocppMessage.messageId,
                    errorCode = OcppErrorCode.PROTOCOL_ERROR,
                    errorDescription = "CALLRESULT not expected from ChargePoint",
                    errorDetails = null
                ).toJson()
                OcppMessageType.CALLERROR -> OcppMessage.CallError(
                    messageId = ocppMessage.messageId,
                    errorCode = OcppErrorCode.PROTOCOL_ERROR,
                    errorDescription = "CALLERROR not expected from ChargePoint",
                    errorDetails = null
                ).toJson()
            }
        } catch (e: OcppParseException) {
            val errorMsg = e.message ?: "Parse error"
            OcppMessage.CallError(
                messageId = generateMessageId(),
                errorCode = OcppErrorCode.PROTOCOL_ERROR,
                errorDescription = errorMsg,
                errorDetails = null
            ).toJson()
        }

        return response
    }

    private fun handleCall(call: OcppMessage.Call): String {
        val handler = handlers[call.action]

        if (handler == null) {
            return OcppMessage.CallError(
                messageId = call.messageId,
                errorCode = OcppErrorCode.NOT_IMPLEMENTED,
                errorDescription = "Action '${call.action}' is not implemented",
                errorDetails = null
            ).toJson()
        }

        return try {
            handler.handle(call)
        } catch (e: FormationViolationException) {
            val errorMsg = e.message ?: "Payload validation failed"
            OcppMessage.CallError(
                messageId = call.messageId,
                errorCode = OcppErrorCode.FORMATION_VIOLATION,
                errorDescription = errorMsg,
                errorDetails = null
            ).toJson()
        }
    }

    private fun generateMessageId(): String = UUID.randomUUID().toString()

    @OnClose
    fun onClose() {
        println("WebSocket connection closed: $sessionId")
    }
}

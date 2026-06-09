package org.tekeli.borisp.ocpp16.websocket

import io.quarkus.websockets.next.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.tekeli.borisp.ocpp16.handler.AuthorizeHandler
import org.tekeli.borisp.ocpp16.handler.BootNotificationHandler
import org.tekeli.borisp.ocpp16.handler.DataTransferHandler
import org.tekeli.borisp.ocpp16.handler.DiagnosticsStatusNotificationHandler
import org.tekeli.borisp.ocpp16.handler.FirmwareStatusNotificationHandler
import org.tekeli.borisp.ocpp16.handler.HeartbeatHandler
import org.tekeli.borisp.ocpp16.handler.MeterValuesHandler
import org.tekeli.borisp.ocpp16.handler.OcppActionHandler
import org.tekeli.borisp.ocpp16.handler.StartTransactionHandler
import org.tekeli.borisp.ocpp16.handler.StatusNotificationHandler
import org.tekeli.borisp.ocpp16.handler.StopTransactionHandler
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppMessageType
import org.tekeli.borisp.ocpp16.protocol.OcppParseException
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import java.util.*

@WebSocket(path = "/ocpp/{chargePointId}")
@ApplicationScoped
class OcppWebSocketServer : ChargePointConnection {

    @Inject
    var connection: WebSocketConnection? = null

    @Inject
    var chargePointRegistry: ChargePointRegistry? = null

    @Inject
    var persistenceService: PersistenceService? = null

    @Inject
    var metricsService: MetricsService? = null

    val activeConnection: WebSocketConnection
        get() = connection ?: throw IllegalStateException("Connection not initialized")

    override val responseAwaiter = ResponseAwaiter()
    var sessionId: String = ""
    var chargePointId: String? = null
    private val handlers: Map<String, OcppActionHandler> = mapOf(
        "BootNotification" to BootNotificationHandler(),
        "Heartbeat" to HeartbeatHandler(),
        "Authorize" to AuthorizeHandler(),
        "StartTransaction" to StartTransactionHandler(metricsService),
        "StopTransaction" to StopTransactionHandler(metricsService),
        "StatusNotification" to StatusNotificationHandler(),
        "DataTransfer" to DataTransferHandler(),
        "FirmwareStatusNotification" to FirmwareStatusNotificationHandler(),
        "DiagnosticsStatusNotification" to DiagnosticsStatusNotificationHandler(),
        "MeterValues" to MeterValuesHandler()
    )

    @OnOpen
    fun onOpen() {
        chargePointId = connection?.pathParam("chargePointId")
        val connectionId = connection?.id()
            ?: throw IllegalStateException("WebSocket connection id not available")
        sessionId = connectionId
        chargePointRegistry?.register(sessionId, connectionId, this)
        println("WebSocket connection opened: $sessionId, chargePointId=$chargePointId")
    }

    @OnTextMessage
    fun onTextMessage(message: String): String {
        val response = try {
            val ocppMessage = OcppMessage.parse(message)

            when (ocppMessage.type) {
                OcppMessageType.CALL -> {
                    metricsService?.messagesReceived?.increment()
                    handleCall(ocppMessage as OcppMessage.Call)
                }
                OcppMessageType.CALLRESULT -> handleCallResult(ocppMessage as OcppMessage.CallResult)
                OcppMessageType.CALLERROR -> handleCallError(ocppMessage as OcppMessage.CallError)
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
            handler.handle(call, this)
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

    private fun handleCallResult(callResult: OcppMessage.CallResult): String {
        try {
            responseAwaiter.resolve(callResult.messageId, callResult)
            return ""
        } catch (e: IllegalStateException) {
            return OcppMessage.CallError(
                messageId = callResult.messageId,
                errorCode = OcppErrorCode.PROTOCOL_ERROR,
                errorDescription = "CALLRESULT not expected from ChargePoint",
                errorDetails = null
            ).toJson()
        }
    }

    private fun handleCallError(callError: OcppMessage.CallError): String {
        try {
            responseAwaiter.reject(callError.messageId, callError)
            return ""
        } catch (e: IllegalStateException) {
            return OcppMessage.CallError(
                messageId = callError.messageId,
                errorCode = OcppErrorCode.PROTOCOL_ERROR,
                errorDescription = "CALLERROR not expected from ChargePoint",
                errorDetails = null
            ).toJson()
        }
    }

    override fun sendText(text: String): io.smallrye.mutiny.Uni<Void> {
        return connection?.sendText(text) ?: io.smallrye.mutiny.Uni.createFrom().voidItem()
    }

    private fun generateMessageId(): String = UUID.randomUUID().toString()

    @OnClose
    fun onClose() {
        val connectionId = sessionId
        if ((chargePointRegistry?.isConnected(connectionId) ?: false).not()) {
            return
        }
        responseAwaiter.rejectAll("WebSocket connection closed: $connectionId")
        try {
            chargePointRegistry?.unregister(connectionId)
            persistenceService?.setChargePointOffline(connectionId)
        } catch (e: Exception) {
        }
        println("WebSocket connection closed: $connectionId")
    }
}

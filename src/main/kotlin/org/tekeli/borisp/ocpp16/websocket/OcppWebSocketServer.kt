package org.tekeli.borisp.ocpp16.websocket

import io.quarkus.logging.Log
import io.quarkus.websockets.next.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.tekeli.borisp.ocpp16.handler.AuthorizeHandler
import org.tekeli.borisp.ocpp16.handler.BootNotificationHandler
import org.tekeli.borisp.ocpp16.handler.CertificateSignedHandler
import org.tekeli.borisp.ocpp16.handler.DataTransferHandler
import org.tekeli.borisp.ocpp16.handler.DiagnosticsStatusNotificationHandler
import org.tekeli.borisp.ocpp16.handler.FirmwareStatusNotificationHandler
import org.tekeli.borisp.ocpp16.handler.HeartbeatHandler
import org.tekeli.borisp.ocpp16.handler.LogStatusNotificationHandler
import org.tekeli.borisp.ocpp16.handler.MeterValuesHandler
import org.tekeli.borisp.ocpp16.handler.OcppActionHandler
import org.tekeli.borisp.ocpp16.handler.OcppHandlerContext
import org.tekeli.borisp.ocpp16.handler.SecurityEventNotificationHandler
import org.tekeli.borisp.ocpp16.handler.SignCertificateHandler
import org.tekeli.borisp.ocpp16.handler.SignedFirmwareStatusNotificationHandler
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
open class OcppWebSocketServer : ChargePointConnection, OcppHandlerContext {

    @Inject
    var connection: WebSocketConnection? = null

    @Inject
    override var chargePointRegistry: ChargePointRegistry? = null

    @Inject
    override var persistenceService: PersistenceService? = null

    @Inject
    override var metricsService: MetricsService? = null

    val activeConnection: WebSocketConnection
        get() = connection ?: throw IllegalStateException("Connection not initialized")

    private val activeRegistry: ChargePointRegistry
        get() = chargePointRegistry ?: throw IllegalStateException("Registry not initialized")

    private val activePersistence: PersistenceService
        get() = persistenceService ?: throw IllegalStateException("Persistence not initialized")

    override val responseAwaiter = ResponseAwaiter()
    open override var sessionId: String = ""
    override var chargePointId: String = ""
    private val handlers: Map<String, OcppActionHandler> by lazy {
        mapOf(
            "BootNotification" to BootNotificationHandler(),
            "Heartbeat" to HeartbeatHandler(),
            "Authorize" to AuthorizeHandler(),
            "StartTransaction" to StartTransactionHandler(metricsService),
            "StopTransaction" to StopTransactionHandler(metricsService),
            "StatusNotification" to StatusNotificationHandler(),
            "DataTransfer" to DataTransferHandler(),
            "FirmwareStatusNotification" to FirmwareStatusNotificationHandler(),
            "DiagnosticsStatusNotification" to DiagnosticsStatusNotificationHandler(),
            "MeterValues" to MeterValuesHandler(),
            "SecurityEventNotification" to SecurityEventNotificationHandler(persistenceService),
            "SignedFirmwareStatusNotification" to SignedFirmwareStatusNotificationHandler(),
            "LogStatusNotification" to LogStatusNotificationHandler(),
            "SignCertificate" to SignCertificateHandler(),
            "CertificateSigned" to CertificateSignedHandler()
        )
    }

    @OnOpen
    fun onOpen() {
        chargePointId = activeConnection.pathParam("chargePointId")
        sessionId = activeConnection.id() ?: throw IllegalStateException("Connection id not available")
        activeRegistry.register(sessionId, sessionId, this)
        Log.info("WebSocket connection opened: session=$sessionId, chargePoint=$chargePointId")
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
        val conn = connection
        if (conn != null) return conn.sendText(text)
        return io.smallrye.mutiny.Uni.createFrom().voidItem()
    }

    private fun generateMessageId(): String = UUID.randomUUID().toString()

  @OnClose
    fun onClose() {
        val connectionId = sessionId
        if (activeRegistry.isConnected(connectionId)) {
            responseAwaiter.rejectAll("WebSocket connection closed: $connectionId")
            activeRegistry.unregister(connectionId)
            activePersistence.setChargePointOffline(connectionId)
            Log.info("WebSocket connection closed: session=$connectionId")
        }
    }
}

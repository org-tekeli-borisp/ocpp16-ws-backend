package org.tekeli.borisp.ocpp16

import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnOpen
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.WebSocketConnection
import jakarta.inject.Inject
import java.util.UUID
import java.util.concurrent.CompletableFuture

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

    // Outbound infrastructure
    private val responseAwaiter = ResponseAwaiter()
    private val dispatcher: OutboundCallDispatcher by lazy {
        val sender: WebSocketSend = object : WebSocketSend {
            override fun sendText(text: String) {
                connection?.sendText(text)
            }
        }
        OutboundCallDispatcher(sender, responseAwaiter)
    }

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

    // Outbound S->C methods

    fun sendReset(type: String): CompletableFuture<OcppMessage> {
        return dispatcher.sendCall("Reset", mapOf("type" to type))
    }

    fun sendClearCache(): CompletableFuture<OcppMessage> {
        return dispatcher.sendCall("ClearCache", null)
    }

    fun sendChangeConfiguration(key: String, value: String): CompletableFuture<OcppMessage> {
        return dispatcher.sendCall("ChangeConfiguration", mapOf("key" to key, "value" to value))
    }

    fun sendChangeAvailability(connectorId: Int, type: String): CompletableFuture<OcppMessage> {
        return dispatcher.sendCall("ChangeAvailability", mapOf("connectorId" to connectorId, "type" to type))
    }

    fun sendGetConfiguration(keys: List<String>? = null): CompletableFuture<OcppMessage> {
        return dispatcher.sendCall("GetConfiguration", keys?.let { mapOf("key" to it) })
    }

    fun sendGetDiagnostics(location: String, retries: Int? = null, retryInterval: Int? = null, startTime: String? = null, stopTime: String? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>()
        payload["location"] = location
        retries?.let { payload["retries"] = it }
        retryInterval?.let { payload["retryInterval"] = it }
        startTime?.let { payload["startTime"] = it }
        stopTime?.let { payload["stopTime"] = it }
        return dispatcher.sendCall("GetDiagnostics", payload)
    }

    fun sendGetLocalListVersion(): CompletableFuture<OcppMessage> {
        return dispatcher.sendCall("GetLocalListVersion", null)
    }

    fun sendGetCompositeSchedule(connectorId: Int, duration: Int, chargingRateUnit: String? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>()
        payload["connectorId"] = connectorId
        payload["duration"] = duration
        chargingRateUnit?.let { payload["chargingRateUnit"] = it }
        return dispatcher.sendCall("GetCompositeSchedule", payload)
    }

    fun sendRemoteStartTransaction(idTag: String, connectorId: Int? = null, chargingProfile: Map<String, Any>? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>()
        payload["idTag"] = idTag
        connectorId?.let { payload["connectorId"] = it }
        chargingProfile?.let { payload["chargingProfile"] = it }
        return dispatcher.sendCall("RemoteStartTransaction", payload)
    }

    fun sendRemoteStopTransaction(transactionId: Int): CompletableFuture<OcppMessage> {
        return dispatcher.sendCall("RemoteStopTransaction", mapOf("transactionId" to transactionId))
    }

    fun sendReserveNow(connectorId: Int, expiryDate: String, idTag: String, reservationId: Int, parentIdTag: String? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>()
        payload["connectorId"] = connectorId
        payload["expiryDate"] = expiryDate
        payload["idTag"] = idTag
        payload["reservationId"] = reservationId
        parentIdTag?.let { payload["parentIdTag"] = it }
        return dispatcher.sendCall("ReserveNow", payload)
    }

    fun sendCancelReservation(reservationId: Int): CompletableFuture<OcppMessage> {
        return dispatcher.sendCall("CancelReservation", mapOf("reservationId" to reservationId))
    }

    fun sendSendLocalList(listVersion: Int, updateType: String, localAuthorizationList: List<Map<String, Any>>? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>()
        payload["listVersion"] = listVersion
        payload["updateType"] = updateType
        localAuthorizationList?.let { payload["localAuthorizationList"] = it }
        return dispatcher.sendCall("SendLocalList", payload)
    }

    fun sendSetChargingProfile(connectorId: Int, csChargingProfiles: Map<String, Any>): CompletableFuture<OcppMessage> {
        return dispatcher.sendCall("SetChargingProfile", mapOf("connectorId" to connectorId, "csChargingProfiles" to csChargingProfiles))
    }

    fun sendClearChargingProfile(id: Int? = null, connectorId: Int? = null, chargingProfilePurpose: String? = null, stackLevel: Int? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>()
        id?.let { payload["id"] = it }
        connectorId?.let { payload["connectorId"] = it }
        chargingProfilePurpose?.let { payload["chargingProfilePurpose"] = it }
        stackLevel?.let { payload["stackLevel"] = it }
        return dispatcher.sendCall("ClearChargingProfile", payload)
    }

    fun sendTriggerMessage(requestedMessage: String, connectorId: Int? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>()
        payload["requestedMessage"] = requestedMessage
        connectorId?.let { payload["connectorId"] = it }
        return dispatcher.sendCall("TriggerMessage", payload)
    }

    fun sendUnlockConnector(connectorId: Int): CompletableFuture<OcppMessage> {
        return dispatcher.sendCall("UnlockConnector", mapOf("connectorId" to connectorId))
    }

    fun sendUpdateFirmware(location: String, retrieveDate: String, retries: Int? = null, retryInterval: Int? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>()
        payload["location"] = location
        payload["retrieveDate"] = retrieveDate
        retries?.let { payload["retries"] = it }
        retryInterval?.let { payload["retryInterval"] = it }
        return dispatcher.sendCall("UpdateFirmware", payload)
    }

    private fun generateMessageId(): String = UUID.randomUUID().toString()

    @OnClose
    fun onClose() {
        println("WebSocket connection closed: $sessionId")
    }
}

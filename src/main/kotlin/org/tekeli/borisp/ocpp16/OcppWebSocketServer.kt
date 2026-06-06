package org.tekeli.borisp.ocpp16

import io.quarkus.websockets.next.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.*
import java.util.concurrent.CompletableFuture

@WebSocket(path = "/ocpp/{chargeBoxIdentity}")
@ApplicationScoped
class OcppWebSocketServer : ChargePointConnection {

    @Inject
    var connection: WebSocketConnection? = null

    @Inject
    var chargePointRegistry: ChargePointRegistry? = null

    val activeConnection: WebSocketConnection
        get() = connection ?: throw IllegalStateException("Connection not initialized")

    override val responseAwaiter = ResponseAwaiter()
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

    private val dispatcher: OutboundCallDispatcher by lazy {
        OutboundCallDispatcher(this, responseAwaiter)
    }

    @OnOpen
    fun onOpen() {
        chargePointRegistry?.register(sessionId, this)
        println("WebSocket connection opened: $sessionId, ${connection?.pathParam("chargeBoxIdentity")}")
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

    // ChargePointConnection interface
    override fun sendText(text: String) {
        connection?.sendText(text)
    }

    // Outbound S->C methods
    fun sendReset(type: String): CompletableFuture<OcppMessage> = dispatcher.sendCall("Reset", mapOf("type" to type))
    fun sendClearCache(): CompletableFuture<OcppMessage> = dispatcher.sendCall("ClearCache", null)
    fun sendChangeConfiguration(key: String, value: String): CompletableFuture<OcppMessage> =
        dispatcher.sendCall("ChangeConfiguration", mapOf("key" to key, "value" to value))

    fun sendChangeAvailability(connectorId: Int, type: String): CompletableFuture<OcppMessage> =
        dispatcher.sendCall("ChangeAvailability", mapOf("connectorId" to connectorId, "type" to type))

    fun sendGetConfiguration(keys: List<String>? = null): CompletableFuture<OcppMessage> =
        dispatcher.sendCall("GetConfiguration", keys?.let { mapOf("key" to it) })

    fun sendGetDiagnostics(
        location: String,
        retries: Int? = null,
        retryInterval: Int? = null,
        startTime: String? = null,
        stopTime: String? = null
    ): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("location" to location)
        retries?.let { payload["retries"] = it }
        retryInterval?.let { payload["retryInterval"] = it }
        startTime?.let { payload["startTime"] = it }
        stopTime?.let { payload["stopTime"] = it }
        return dispatcher.sendCall("GetDiagnostics", payload)
    }

    fun sendGetLocalListVersion(): CompletableFuture<OcppMessage> = dispatcher.sendCall("GetLocalListVersion", null)
    fun sendGetCompositeSchedule(
        connectorId: Int,
        duration: Int,
        chargingRateUnit: String? = null
    ): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("connectorId" to connectorId, "duration" to duration)
        chargingRateUnit?.let { payload["chargingRateUnit"] = it }
        return dispatcher.sendCall("GetCompositeSchedule", payload)
    }

    fun sendRemoteStartTransaction(
        idTag: String,
        connectorId: Int? = null,
        chargingProfile: Map<String, Any>? = null
    ): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("idTag" to idTag)
        connectorId?.let { payload["connectorId"] = it }
        chargingProfile?.let { payload["chargingProfile"] = it }
        return dispatcher.sendCall("RemoteStartTransaction", payload)
    }

    fun sendRemoteStopTransaction(transactionId: Int): CompletableFuture<OcppMessage> =
        dispatcher.sendCall("RemoteStopTransaction", mapOf("transactionId" to transactionId))

    fun sendReserveNow(
        connectorId: Int,
        expiryDate: String,
        idTag: String,
        reservationId: Int,
        parentIdTag: String? = null
    ): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>(
            "connectorId" to connectorId,
            "expiryDate" to expiryDate,
            "idTag" to idTag,
            "reservationId" to reservationId
        )
        parentIdTag?.let { payload["parentIdTag"] = it }
        return dispatcher.sendCall("ReserveNow", payload)
    }

    fun sendCancelReservation(reservationId: Int): CompletableFuture<OcppMessage> =
        dispatcher.sendCall("CancelReservation", mapOf("reservationId" to reservationId))

    fun sendSendLocalList(
        listVersion: Int,
        updateType: String,
        localAuthorizationList: List<Map<String, Any>>? = null
    ): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("listVersion" to listVersion, "updateType" to updateType)
        localAuthorizationList?.let { payload["localAuthorizationList"] = it }
        return dispatcher.sendCall("SendLocalList", payload)
    }

    fun sendSetChargingProfile(connectorId: Int, csChargingProfiles: Map<String, Any>): CompletableFuture<OcppMessage> =
        dispatcher.sendCall(
            "SetChargingProfile",
            mapOf("connectorId" to connectorId, "csChargingProfiles" to csChargingProfiles)
        )

    fun sendClearChargingProfile(
        id: Int? = null,
        connectorId: Int? = null,
        chargingProfilePurpose: String? = null,
        stackLevel: Int? = null
    ): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>()
        id?.let { payload["id"] = it }
        connectorId?.let { payload["connectorId"] = it }
        chargingProfilePurpose?.let { payload["chargingProfilePurpose"] = it }
        stackLevel?.let { payload["stackLevel"] = it }
        return dispatcher.sendCall("ClearChargingProfile", payload)
    }

    fun sendTriggerMessage(requestedMessage: String, connectorId: Int? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("requestedMessage" to requestedMessage)
        connectorId?.let { payload["connectorId"] = it }
        return dispatcher.sendCall("TriggerMessage", payload)
    }

    fun sendUnlockConnector(connectorId: Int): CompletableFuture<OcppMessage> =
        dispatcher.sendCall("UnlockConnector", mapOf("connectorId" to connectorId))

    fun sendUpdateFirmware(
        location: String,
        retrieveDate: String,
        retries: Int? = null,
        retryInterval: Int? = null
    ): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("location" to location, "retrieveDate" to retrieveDate)
        retries?.let { payload["retries"] = it }
        retryInterval?.let { payload["retryInterval"] = it }
        return dispatcher.sendCall("UpdateFirmware", payload)
    }

    fun getSessionId(): String = sessionId

    private fun generateMessageId(): String = UUID.randomUUID().toString()

    @OnClose
    fun onClose() {
        chargePointRegistry?.unregister(sessionId)
        println("WebSocket connection closed: $sessionId")
    }
}

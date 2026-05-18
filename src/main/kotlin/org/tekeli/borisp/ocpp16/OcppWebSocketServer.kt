package org.tekeli.borisp.ocpp16

import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnOpen
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.WebSocketConnection
import jakarta.inject.Inject
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

@WebSocket(path = "/ocpp")
class OcppWebSocketServer {
    
    @Inject
    var connection: WebSocketConnection? = null

    val activeConnection: WebSocketConnection
        get() = connection ?: throw IllegalStateException("Connection not initialized")
    
    private val sessionId = UUID.randomUUID().toString()
    private val handlers = mapOf(
        "BootNotification" to ::handleBootNotification,
        "Heartbeat" to ::handleHeartbeat,
        "Authorize" to ::handleAuthorize,
        "StartTransaction" to ::handleStartTransaction,
        "StopTransaction" to ::handleStopTransaction,
        "StatusNotification" to ::handleStatusNotification
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
            handler(call)
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
    
    private fun handleBootNotification(call: OcppMessage.Call): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")
        
        val vendor = payload["chargePointVendor"]
        val model = payload["chargePointModel"]
        
        if (vendor == null || vendor.toString().isBlank()) {
            throw FormationViolationException("chargePointVendor is required")
        }
        
        if (model == null || model.toString().isBlank()) {
            throw FormationViolationException("chargePointModel is required")
        }
        
        val currentTime = ZonedDateTime.now(ZoneOffset.UTC)
            .toString()
        
        val responsePayload = mapOf(
            "currentTime" to currentTime,
            "interval" to 300,
            "status" to "Accepted"
        )
        
        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }
    
    private fun handleHeartbeat(call: OcppMessage.Call): String {
        val currentTime = ZonedDateTime.now(ZoneOffset.UTC)
            .toString()

        val responsePayload = mapOf(
            "currentTime" to currentTime
        )

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }

    private fun handleAuthorize(call: OcppMessage.Call): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val idTag = payload["idTag"]

        if (idTag == null || idTag.toString().isBlank()) {
            throw FormationViolationException("idTag is required")
        }

        if (idTag.toString().length > 20) {
            throw FormationViolationException("idTag must not exceed 20 characters")
        }

        val responsePayload = mapOf(
            "idTagInfo" to mapOf("status" to "Accepted")
        )

return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }

    private fun handleStartTransaction(call: OcppMessage.Call): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val connectorId = payload["connectorId"]
        if (connectorId == null) {
            throw FormationViolationException("connectorId is required")
        }

        val idTag = payload["idTag"]
        if (idTag == null || idTag.toString().isBlank()) {
            throw FormationViolationException("idTag is required")
        }

        if (idTag.toString().length > 20) {
            throw FormationViolationException("idTag must not exceed 20 characters")
        }

        val meterStart = payload["meterStart"]
        if (meterStart == null) {
            throw FormationViolationException("meterStart is required")
        }

        val timestamp = payload["timestamp"]
        if (timestamp == null || timestamp.toString().isBlank()) {
            throw FormationViolationException("timestamp is required")
        }

        val responsePayload = mapOf(
            "idTagInfo" to mapOf("status" to "Accepted"),
            "transactionId" to 1
        )

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }

    private val validStopReasons = setOf(
        "Emergency", "EnergyAsymmetric", "EmergencyExt", "EVDisconnected",
        "HardReset", "Local", "LossOfAsset", "LossOfPower", "Other",
        "PowerQuality", "Reboot", "SoftReset", "UnlockCommand",
        "UploadDiagnostics", "OtherExt", "OutOfCredit", "Overcurrentsign"
    )

    private fun handleStopTransaction(call: OcppMessage.Call): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val transactionId = payload["transactionId"]
        if (transactionId == null) {
            throw FormationViolationException("transactionId is required")
        }

        val meterStop = payload["meterStop"]
        if (meterStop == null) {
            throw FormationViolationException("meterStop is required")
        }

        val timestamp = payload["timestamp"]
        if (timestamp == null || timestamp.toString().isBlank()) {
            throw FormationViolationException("timestamp is required")
        }

        val reason = payload["reason"]
        if (reason == null || reason.toString().isBlank()) {
            throw FormationViolationException("reason is required")
        }

        if (!validStopReasons.contains(reason.toString())) {
            throw FormationViolationException("Invalid reason: ${reason}")
        }

        val idTag = payload["idTag"]
        if (idTag != null) {
            val idTagStr = idTag.toString().trim()
            if (idTagStr.length > 20) {
                throw FormationViolationException("idTag must not exceed 20 characters")
            }
        }

        val responsePayload = mapOf(
            "idTagInfo" to mapOf("status" to "Accepted")
        )

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }

    private val validErrorCodes = setOf(
        "Accepted", "Boot", "CableWake", "LockedIn", "LockedOut", "NoError",
        "OpenRouter", "OverCurrent", "OverVoltage", "Overheating",
        "PermissionDenied", "ReaderBlocked", "Reset", "Unavailable",
        "VoltageTooLow", "WeldedConnector", "WeldedMeter"
    )

    private val validConnectorStatuses = setOf("Available", "Unavailable", "Faulted")

    private fun handleStatusNotification(call: OcppMessage.Call): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val connectorId = payload["connectorId"]
        if (connectorId == null) {
            throw FormationViolationException("connectorId is required")
        }

        val errorCode = payload["errorCode"]
        if (errorCode == null || errorCode.toString().isBlank()) {
            throw FormationViolationException("errorCode is required")
        }

        if (!validErrorCodes.contains(errorCode.toString())) {
            throw FormationViolationException("Invalid errorCode: ${errorCode}")
        }

        val status = payload["status"]
        if (status == null || status.toString().isBlank()) {
            throw FormationViolationException("status is required")
        }

        if (!validConnectorStatuses.contains(status.toString())) {
            throw FormationViolationException("Invalid status: ${status}")
        }

        val info = payload["info"]
        if (info != null) {
            val infoStr = info.toString().trim()
            if (infoStr.length > 50) {
                throw FormationViolationException("info must not exceed 50 characters")
            }
        }

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = null
        ).toJson()
    }

    private fun generateMessageId(): String = UUID.randomUUID().toString()
    
    @OnClose
    fun onClose() {
        println("WebSocket connection closed: $sessionId")
    }
}

class FormationViolationException(message: String) : RuntimeException(message)

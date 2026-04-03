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
    lateinit var connection: WebSocketConnection
    
    private val sessionId = UUID.randomUUID().toString()
    private val handlers = mapOf(
        "BootNotification" to ::handleBootNotification,
        "Heartbeat" to ::handleHeartbeat
    )
    
    @OnOpen
    fun onOpen() {
        println("WebSocket connection opened: $sessionId")
    }
    
       @OnTextMessage
    fun onTextMessage(message: String): String {
        val response = try {
            val ocppMessage = OcppMessage.parse(message)
            
            when (val parsedMessage = ocppMessage) {
                is OcppMessage.Call -> handleCall(parsedMessage)
                is OcppMessage.CallResult -> {
                    OcppMessage.CallError(
                        messageId = parsedMessage.messageId,
                        errorCode = OcppErrorCode.PROTOCOL_ERROR,
                        errorDescription = "CALLRESULT not expected from ChargePoint",
                        errorDetails = null
                    ).toJson()
                }
                is OcppMessage.CallError -> {
                    OcppMessage.CallError(
                        messageId = parsedMessage.messageId,
                        errorCode = OcppErrorCode.PROTOCOL_ERROR,
                        errorDescription = "CALLERROR not expected from ChargePoint",
                        errorDetails = null
                    ).toJson()
                }
            }
        } catch (e: OcppParseException) {
            val errorMsg = e.message?.takeIf { it.isNotBlank() } ?: "Failed to parse message"
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
            val errorMsg = e.message?.takeIf { it.isNotBlank() } ?: "Payload validation failed"
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
    
    private fun generateMessageId(): String = UUID.randomUUID().toString()
    
    @OnClose
    fun onClose() {
        println("WebSocket connection closed: $sessionId")
    }
}

class FormationViolationException(message: String) : RuntimeException(message)

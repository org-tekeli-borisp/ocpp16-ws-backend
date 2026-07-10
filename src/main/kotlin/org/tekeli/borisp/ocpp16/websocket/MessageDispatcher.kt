package org.tekeli.borisp.ocpp16.websocket

import org.tekeli.borisp.ocpp16.handler.OcppActionHandler
import org.tekeli.borisp.ocpp16.handler.OcppHandlerContext
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.MessageCaptureService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppMessageDirection
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import org.tekeli.borisp.ocpp16.protocol.OcppMessageType
import org.tekeli.borisp.ocpp16.protocol.OcppParseException
import org.tekeli.borisp.ocpp16.protocol.callError
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import java.util.*

/**
 * Dispatches incoming OCPP messages to the appropriate handler or response awaiter.
 */
class MessageDispatcher(
    private val handlers: Map<String, OcppActionHandler>,
    private val messageCaptureService: MessageCaptureService? = null,
) {
    fun dispatch(
        message: String,
        context: OcppHandlerContext,
        responseAwaiter: ResponseAwaiter,
        metricsService: MetricsService?
    ): String {
        return try {
            val ocppMessage = OcppMessage.parse(message)
            dispatchParsed(ocppMessage, context, responseAwaiter, metricsService)
        } catch (e: OcppParseException) {
            val errorMsg = e.message ?: "Parse error"
            OcppMessage.CallError(
                messageId = generateMessageId(),
                errorCode = OcppErrorCode.PROTOCOL_ERROR,
                errorDescription = errorMsg,
                errorDetails = null
            ).toJson()
        }
    }

    private fun dispatchParsed(
        ocppMessage: OcppMessage,
        context: OcppHandlerContext,
        responseAwaiter: ResponseAwaiter,
        metricsService: MetricsService?
    ): String {
        messageCaptureService?.capture(context.chargePointId, OcppMessageDirection.INBOUND, ocppMessage)
        return when (ocppMessage.type) {
            OcppMessageType.CALL -> {
                metricsService?.messagesReceived?.increment()
                handleCall(ocppMessage as OcppMessage.Call, context)
            }
            OcppMessageType.CALLRESULT -> handleCallResult(ocppMessage as OcppMessage.CallResult, responseAwaiter)
            OcppMessageType.CALLERROR -> handleCallError(ocppMessage as OcppMessage.CallError, responseAwaiter)
        }
    }

    private fun handleCall(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val handler = handlers[call.action]
        val responseJson: String
        if (handler == null) {
            responseJson = call.callError(
                OcppErrorCode.NOT_IMPLEMENTED,
                "Action '${call.action}' is not implemented"
            )
        } else {
            responseJson = try {
                handler.handle(call, context)
            } catch (e: FormationViolationException) {
                val errorMsg = e.message ?: "Payload validation failed"
                call.callError(OcppErrorCode.FORMATION_VIOLATION, errorMsg)
            }
        }
        try {
            val responseMsg = OcppMessage.parse(responseJson)
            messageCaptureService?.capture(context.chargePointId, OcppMessageDirection.OUTBOUND, responseMsg)
        } catch (_: Throwable) { }
        return responseJson
    }

    private fun handleCallResult(callResult: OcppMessage.CallResult, responseAwaiter: ResponseAwaiter): String {
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

    private fun handleCallError(callError: OcppMessage.CallError, responseAwaiter: ResponseAwaiter): String {
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

    private fun generateMessageId(): String = UUID.randomUUID().toString()
}

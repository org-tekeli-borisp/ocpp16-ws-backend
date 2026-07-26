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
import com.fasterxml.jackson.databind.ObjectMapper
import org.tekeli.borisp.ocpp16.protocol.SchemaValidator
import java.util.*

/**
 * Dispatches incoming OCPP messages to the appropriate handler or response awaiter.
 */
class MessageDispatcher(
    private val handlers: Map<String, OcppActionHandler>,
    private val messageCaptureService: MessageCaptureService? = null,
    private val schemaValidator: SchemaValidator? = null,
) {
    private val objectMapper = ObjectMapper()
    fun dispatch(
        message: String,
        context: OcppHandlerContext,
        responseAwaiter: ResponseAwaiter,
        metricsService: MetricsService?
    ): String {
        return dispatch(message, context, responseAwaiter, metricsService, context.chargePointId)
    }

    fun dispatch(
        message: String,
        context: OcppHandlerContext,
        responseAwaiter: ResponseAwaiter,
        metricsService: MetricsService?,
        chargePointId: String
    ): String {
        return try {
            val ocppMessage = OcppMessage.parse(message)
            dispatchParsed(ocppMessage, context, responseAwaiter, metricsService, chargePointId)
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
        metricsService: MetricsService?,
        chargePointId: String
    ): String {
        return when (ocppMessage.type) {
            OcppMessageType.CALL -> {
                messageCaptureService?.capture(chargePointId, OcppMessageDirection.INBOUND, ocppMessage)
                metricsService?.messagesReceived?.increment()
                handleCall(ocppMessage as OcppMessage.Call, context, chargePointId)
            }
            OcppMessageType.CALLRESULT -> handleCallResult(ocppMessage as OcppMessage.CallResult, responseAwaiter)
            OcppMessageType.CALLERROR -> handleCallError(ocppMessage as OcppMessage.CallError, responseAwaiter)
        }
    }

    private fun handleCall(call: OcppMessage.Call, context: OcppHandlerContext, chargePointId: String): String {
        val responseJson = handlers[call.action]
            ?.let { invokeHandler(call, context, it) }
            ?: call.callError(OcppErrorCode.NOT_IMPLEMENTED, "Action '${call.action}' is not implemented")
        captureOutbound(chargePointId, responseJson)
        return responseJson
    }

    private fun invokeHandler(call: OcppMessage.Call, context: OcppHandlerContext, handler: OcppActionHandler): String {
        val schemaErrors = validateSchema(call)
        if (schemaErrors != null) {
            return schemaErrors
        }
        return try {
            handler.handle(call, context)
        } catch (e: Exception) {
            handleError(call, e)
        }
    }

    private fun validateSchema(call: OcppMessage.Call): String? {
        val schemaErrors = schemaValidator?.validate(call.action, objectMapper.writeValueAsString(call.payload ?: emptyMap<String, Any>()))
        if (schemaErrors?.isNotEmpty() == true) {
            return call.callError(OcppErrorCode.FORMATION_VIOLATION, "Schema validation failed: ${schemaErrors.joinToString("; ")}")
        }
        return null
    }

    private fun handleError(call: OcppMessage.Call, e: Exception): String {
        return when (e) {
            is FormationViolationException -> call.callError(OcppErrorCode.FORMATION_VIOLATION, e.message ?: "Payload validation failed")
            else -> call.callError(OcppErrorCode.INTERNAL_ERROR, e.message ?: "Internal error")
        }
    }

    private fun captureOutbound(chargePointId: String, responseJson: String) {
        try {
            val responseMsg = OcppMessage.parse(responseJson)
            messageCaptureService?.capture(chargePointId, OcppMessageDirection.OUTBOUND, responseMsg)
        } catch (_: Throwable) { }
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

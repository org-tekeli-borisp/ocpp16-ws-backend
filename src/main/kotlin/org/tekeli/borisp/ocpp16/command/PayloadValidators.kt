package org.tekeli.borisp.ocpp16.command

import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

object PayloadValidators {
    fun isNumber(value: Any?): Boolean = value is Number

    fun isString(value: Any?): Boolean = value is String

    fun isNonEmptyString(value: Any?): Boolean =
        value is String && value.isNotEmpty()

    fun isValidOneOf(value: Any?, validValues: Set<String>): Boolean =
        value is String && validValues.contains(value)

    fun isMap(value: Any?): Boolean = value is Map<*, *>

    fun isCallResult(response: OcppMessage): Boolean =
        response is OcppMessage.CallResult

    fun buildAcceptedResponse(command: String, extra: Map<String, Any> = emptyMap()): Response =
        Response.status(Response.Status.ACCEPTED)
            .entity(mapOf<String, Any>("status" to "sent", "command" to command) + extra)
            .build()

    fun buildRejectedResponse(): Response =
        Response.status(Response.Status.BAD_GATEWAY)
            .entity(mapOf<String, Any>("status" to "rejected", "error" to "ChargePoint rejected command"))
            .build()

    fun buildCommandResponse(response: OcppMessage, command: String, extra: Map<String, Any> = emptyMap()): Response =
        if (isCallResult(response)) buildAcceptedResponse(command, extra)
        else buildRejectedResponse()
}

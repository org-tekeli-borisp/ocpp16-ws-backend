package org.tekeli.borisp.ocpp16.command

import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import java.util.concurrent.CompletableFuture

object PayloadValidators {
    fun isNumber(value: Any?): Boolean = value is Number

    fun isString(value: Any?): Boolean = value is String

    fun isNonEmptyString(value: Any?): Boolean =
        value is String && value.isNotEmpty()

    fun isValidOneOf(value: String, validValues: Set<String>): Boolean =
        validValues.contains(value)

    fun isMap(value: Any?): Boolean = value is Map<*, *>

    fun safeMap(value: Any?): Map<String, Any> {
        val source = value as? Map<*, *>
        val result = HashMap<String, Any>()
        if (source != null) {
            for ((k, v) in source) result[k as String] = v as Any
        }
        return result
    }

    fun safeList(value: Any?): List<String> =
        (value as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

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

    fun awaitAndBuildResponse(
        future: CompletableFuture<OcppMessage>,
        command: String,
        extra: Map<String, Any> = emptyMap()
    ): Response =
        buildCommandResponse(
            future.get(OcppConstants.COMMAND_TIMEOUT_SECONDS.toLong(), java.util.concurrent.TimeUnit.SECONDS),
            command,
            extra
        )
}

package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class GetLogCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "get-log"

    private val validLogTypes = setOf("DiagnosticsLog", "SecurityLog")

    override fun validate(payload: Map<String, Any>): Response? {
        val error = validateTopLevel(payload)
            ?: validateNestedLog(payload)
        return error?.let {
            Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to it))
                .build()
        }
    }

    internal fun validateTopLevel(payload: Map<String, Any>): String? {
        val raw = payload["logType"]
        val logType = raw?.toString()
        if (logType == null || logType !in validLogTypes)
            return "logType must be one of: DiagnosticsLog, SecurityLog"
        if (payload["requestId"] !is Number)
            return "requestId is required"
        return if (payload["log"] is Map<*, *>) null else "log is required"
    }

    internal fun validateNestedLog(payload: Map<String, Any>): String? {
        val remoteLocation = (payload["log"] as Map<*, *>).get("remoteLocation") as? String
            ?: return "log.remoteLocation is required"
        return if (remoteLocation.isEmpty() || remoteLocation.length > OcppConstants.MAX_FIRMWARE_LOCATION_LENGTH)
            "log.remoteLocation must not exceed ${OcppConstants.MAX_FIRMWARE_LOCATION_LENGTH} characters" else null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val logType = payload["logType"] as String
        val requestId = (payload["requestId"] as Number).toInt()
        val log = PayloadValidators.safeMap(payload["log"])
        val retries = (payload["retries"] as? Number)?.toInt()
        val retryInterval = (payload["retryInterval"] as? Number)?.toInt()

        val result = gateway.sendGetLog(chargePointId, logType, requestId, log, retries, retryInterval)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

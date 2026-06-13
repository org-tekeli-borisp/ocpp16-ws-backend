package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

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

    private fun validateTopLevel(payload: Map<String, Any>): String? {
        val logType = payload["logType"] as? String
        if (logType == null || logType !in validLogTypes)
            return "logType must be one of: DiagnosticsLog, SecurityLog"
        if (payload["requestId"] !is Number)
            return "requestId is required"
        return if (payload["log"] is Map<*, *>) null else "log is required"
    }

    private fun validateNestedLog(payload: Map<String, Any>): String? {
        val remoteLocation = (payload["log"] as Map<*, *>).get("remoteLocation") as? String
            ?: return "log.remoteLocation is required"
        return if (remoteLocation.isEmpty() || remoteLocation.length > 512)
            "log.remoteLocation must not exceed 512 characters" else null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val logType = payload["logType"] as String
        val requestId = (payload["requestId"] as Number).toInt()
        val log = payload["log"] as Map<String, Any>
        val retries = (payload["retries"] as? Number)?.toInt()
        val retryInterval = (payload["retryInterval"] as? Number)?.toInt()

        val result = gateway.sendGetLog(chargePointId, logType, requestId, log, retries, retryInterval)
        val response = result.get(10, java.util.concurrent.TimeUnit.SECONDS)

        return if (response is OcppMessage.CallResult) {
            Response.status(Response.Status.ACCEPTED)
                .entity(mapOf<String, Any>("status" to "sent", "command" to name))
                .build()
        } else {
            Response.status(Response.Status.BAD_GATEWAY)
                .entity(mapOf<String, Any>("status" to "rejected", "error" to "ChargePoint rejected command"))
                .build()
        }
    }
}

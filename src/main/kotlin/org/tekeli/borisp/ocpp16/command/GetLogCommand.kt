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
        val logType = payload["logType"] as String?
        if (logType == null || logType !in validLogTypes) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "logType must be one of: DiagnosticsLog, SecurityLog"))
                .build()
        }

        val requestId = payload["requestId"] as Number?
        if (requestId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "requestId is required"))
                .build()
        }

        val log = payload["log"] as? Map<*, *>
        if (log == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "log is required"))
                .build()
        }

        val remoteLocation = log["remoteLocation"] as String?
        if (remoteLocation.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "log.remoteLocation is required"))
                .build()
        }

        if (remoteLocation.length > 512) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "log.remoteLocation must not exceed 512 characters"))
                .build()
        }

        return null
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

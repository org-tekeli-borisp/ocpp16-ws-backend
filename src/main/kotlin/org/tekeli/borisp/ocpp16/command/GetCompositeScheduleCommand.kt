package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class GetCompositeScheduleCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "get-composite-schedule"

    override fun validate(payload: Map<String, Any>): Response? {
        val connectorIdRaw = payload["connectorId"]
        if (connectorIdRaw is Number) {
            val durationRaw = payload["duration"]
            if (durationRaw is Number) {
                return null
            }
        }
        if (payload["connectorId"] !is Number) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "connectorId is required"))
                .build()
        }
        return Response.status(Response.Status.BAD_REQUEST)
            .entity(mapOf<String, Any>("error" to "duration is required"))
            .build()
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val connectorId = (payload["connectorId"] as Number).toInt()
        val duration = (payload["duration"] as Number).toInt()

        val result = gateway.sendGetCompositeSchedule(chargePointId, connectorId, duration)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

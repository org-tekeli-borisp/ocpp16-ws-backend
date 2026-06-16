package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class ClearChargingProfileCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "clear-charging-profile"

    override fun validate(payload: Map<String, Any>): Response? {
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val connectorId = if (payload["connectorId"] is Number) (payload["connectorId"] as Number).toInt() else null
        val stackLevel = if (payload["stackLevel"] is Number) (payload["stackLevel"] as Number).toInt() else null

        val result = gateway.sendClearChargingProfile(chargePointId, connectorId, stackLevel)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

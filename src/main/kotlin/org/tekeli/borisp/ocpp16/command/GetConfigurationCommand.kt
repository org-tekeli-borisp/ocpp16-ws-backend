package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class GetConfigurationCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "get-configuration"

    override fun validate(payload: Map<String, Any>): Response? {
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val keys = if (payload["key"] is List<*>) payload["key"] as List<String> else null

        val result = gateway.sendGetConfiguration(chargePointId, keys)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

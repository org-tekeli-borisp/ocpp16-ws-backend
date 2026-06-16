package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class ClearCacheCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "clear-cache"

    override fun validate(payload: Map<String, Any>): Response? {
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val result = gateway.sendClearCache(chargePointId)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

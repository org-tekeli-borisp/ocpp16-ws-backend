package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class GetLocalListVersionCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "get-local-list-version"

    override fun validate(payload: Map<String, Any>): Response? {
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val result = gateway.sendGetLocalListVersion(chargePointId)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

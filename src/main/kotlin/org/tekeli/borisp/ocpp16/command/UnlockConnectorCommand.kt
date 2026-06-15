package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class UnlockConnectorCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "unlock-connector"

    override fun validate(payload: Map<String, Any>): Response? {
        val connectorIdRaw = payload["connectorId"]

        if (connectorIdRaw !is Number) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "connectorId is required"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val connectorId = (payload["connectorId"] as Number).toInt()

        val result = gateway.sendUnlockConnector(chargePointId, connectorId)
        val response = result.get(10, java.util.concurrent.TimeUnit.SECONDS)

        return PayloadValidators.buildCommandResponse(response, name)
    }
}

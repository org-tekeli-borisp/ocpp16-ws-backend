package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class RemoteStartTransactionCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "remote-start-transaction"

    override fun validate(payload: Map<String, Any>): Response? {
        val idTag = payload["idTag"]
        val connectorIdRaw = payload["connectorId"]

        if (!PayloadValidators.isNonEmptyString(idTag)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "idTag is required"))
                .build()
        }
        if (!PayloadValidators.isNumber(connectorIdRaw)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "connectorId is required"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val idTag = payload["idTag"] as String
        val connectorId = (payload["connectorId"] as Number).toInt()

        val result = gateway.sendRemoteStartTransaction(chargePointId, idTag, connectorId)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

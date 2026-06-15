package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

@jakarta.enterprise.context.ApplicationScoped
class ChangeAvailabilityCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "change-availability"
    private val validTypes = setOf("Inoperative", "Operative")

    override fun validate(payload: Map<String, Any>): Response? {
        val connectorIdRaw = payload["connectorId"]
        val type = payload["type"]

        if (!PayloadValidators.isNumber(connectorIdRaw)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "connectorId is required"))
                .build()
        }
        if (!PayloadValidators.isValidOneOf(type, validTypes)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "type must be 'Inoperative' or 'Operative'"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val connectorId = (payload["connectorId"] as Number).toInt()
        val type = payload["type"] as String

        val result = gateway.sendChangeAvailability(chargePointId, connectorId, type)
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

package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class CancelReservationCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "cancel-reservation"

    override fun validate(payload: Map<String, Any>): Response? {
        val reservationIdRaw = payload["reservationId"]

        if (reservationIdRaw !is Number) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "reservationId is required"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val reservationId = (payload["reservationId"] as Number).toInt()

        val result = gateway.sendCancelReservation(chargePointId, reservationId)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

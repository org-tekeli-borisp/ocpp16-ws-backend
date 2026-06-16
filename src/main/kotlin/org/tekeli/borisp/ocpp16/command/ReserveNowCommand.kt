package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class ReserveNowCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "reserve-now"

    override fun validate(payload: Map<String, Any>): Response? {
        val error = validateRequiredIds(payload)
            ?: validateStringFields(payload)
        return error?.let { badRequest(it) }
    }

    private fun validateRequiredIds(payload: Map<String, Any>): String? {
        val connectorIdRaw = payload["connectorId"]
        if (connectorIdRaw is Number) {
            val reservationIdRaw = payload["reservationId"]
            if (reservationIdRaw is Number) {
                return null
            } else {
                return "reservationId is required"
            }
        } else {
            return "connectorId is required"
        }
    }

    private fun validateStringFields(payload: Map<String, Any>): String? {
        if ((payload["expiryDate"] as String?).isNullOrEmpty()) {
            return "expiryDate is required"
        }
        return if ((payload["idTag"] as String?).isNullOrEmpty()) "idTag is required" else null
    }

    private fun badRequest(error: String) = Response.status(Response.Status.BAD_REQUEST)
        .entity(mapOf<String, Any>("error" to error)).build()

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val connectorId = (payload["connectorId"] as Number).toInt()
        val expiryDate = payload["expiryDate"] as String
        val idTag = payload["idTag"] as String
        val reservationId = (payload["reservationId"] as Number).toInt()

        val result = gateway.sendReserveNow(chargePointId, connectorId, expiryDate, idTag, reservationId)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

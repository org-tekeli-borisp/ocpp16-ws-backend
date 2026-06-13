package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

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
        if ((payload["connectorId"] as? Number)?.toInt() == null) {
            return "connectorId is required"
        }
        return if ((payload["reservationId"] as? Number)?.toInt() != null) null
            else "reservationId is required"
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
        val connectorId = (payload["connectorId"] as? Number)?.toInt()!!
        val expiryDate = payload["expiryDate"] as String
        val idTag = payload["idTag"] as String
        val reservationId = (payload["reservationId"] as? Number)?.toInt()!!

        val result = gateway.sendReserveNow(chargePointId, connectorId, expiryDate, idTag, reservationId)
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

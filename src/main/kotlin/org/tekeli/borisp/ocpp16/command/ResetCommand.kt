package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

@jakarta.enterprise.context.ApplicationScoped
class ResetCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "reset"
    private val validTypes = setOf("Hard", "Soft")

    override fun validate(payload: Map<String, Any>): Response? {
        val type = payload["type"] as String?

        if (type == null || type !in validTypes) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "type must be 'Hard' or 'Soft'"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val type = payload["type"] as String

        val result = gateway.sendReset(chargePointId, type)
        val response = result.get(10, java.util.concurrent.TimeUnit.SECONDS)

        return if (response is OcppMessage.CallResult) {
            Response.status(Response.Status.ACCEPTED)
                .entity(mapOf<String, Any>("status" to "sent", "command" to name, "type" to type))
                .build()
        } else {
            Response.status(Response.Status.BAD_GATEWAY)
                .entity(mapOf<String, Any>("status" to "rejected", "error" to "ChargePoint rejected command"))
                .build()
        }
    }
}

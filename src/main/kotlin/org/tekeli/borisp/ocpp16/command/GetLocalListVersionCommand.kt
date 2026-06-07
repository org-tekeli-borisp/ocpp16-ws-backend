package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

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

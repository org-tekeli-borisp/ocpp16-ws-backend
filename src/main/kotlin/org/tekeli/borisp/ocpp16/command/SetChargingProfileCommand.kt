package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

@jakarta.enterprise.context.ApplicationScoped
class SetChargingProfileCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "set-charging-profile"

    override fun validate(payload: Map<String, Any>): Response? {
        val connectorId = (payload["connectorId"] as? Number)?.toInt()
        val csChargingProfiles = payload["csChargingProfiles"] as? Map<String, Any>

        if (connectorId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "connectorId is required"))
                .build()
        }
        if (csChargingProfiles == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "csChargingProfiles is required"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val connectorId = (payload["connectorId"] as? Number)?.toInt()!!
        val csChargingProfiles = payload["csChargingProfiles"] as Map<String, Any>

        val result = gateway.sendSetChargingProfile(chargePointId, connectorId, csChargingProfiles)
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

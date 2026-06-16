package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class UpdateFirmwareCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "update-firmware"

    override fun validate(payload: Map<String, Any>): Response? {
        val location = payload["location"] as String?
        val retrieveDate = payload["retrieveDate"] as String?

        if (location.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "location is required"))
                .build()
        }
        if (retrieveDate.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "retrieveDate is required"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val location = payload["location"] as String
        val retrieveDate = payload["retrieveDate"] as String

        val result = gateway.sendUpdateFirmware(chargePointId, location, retrieveDate, null, null)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class GetDiagnosticsCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "get-diagnostics"

    override fun validate(payload: Map<String, Any>): Response? {
        val location = payload["location"] as String?

        if (location.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "location is required"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val location = payload["location"] as String

        val result = gateway.sendGetDiagnostics(chargePointId, location, null, null)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

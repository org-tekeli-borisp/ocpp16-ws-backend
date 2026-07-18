package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class DataTransferCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "data-transfer"

    override fun validate(payload: Map<String, Any>): Response? {
        val vendorId = payload["vendorId"]

        if (!PayloadValidators.isNonEmptyString(vendorId)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "vendorId is required"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val vendorId = payload["vendorId"] as String
        val messageId = payload["messageId"] as? String
        val data = payload["data"] as? String

        val result = gateway.sendDataTransfer(chargePointId, vendorId, messageId, data)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

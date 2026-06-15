package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

@jakarta.enterprise.context.ApplicationScoped
class SendLocalListCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "send-local-list"
    private val validUpdateTypes = setOf("Differential", "Full")

    override fun validate(payload: Map<String, Any>): Response? {
        val listVersionRaw = payload["listVersion"]
        val updateType = payload["updateType"]

        if (!PayloadValidators.isNumber(listVersionRaw)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "listVersion is required"))
                .build()
        }
        if (!PayloadValidators.isValidOneOf(updateType, validUpdateTypes)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "updateType must be 'Differential' or 'Full'"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val listVersion = (payload["listVersion"] as Number).toInt()
        val updateType = payload["updateType"] as String

        val result = gateway.sendSendLocalList(chargePointId, listVersion, updateType)
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

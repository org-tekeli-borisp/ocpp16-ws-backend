package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class TriggerMessageCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "trigger-message"
    private val validMessages = setOf(
        "BootNotification",
        "DiagnosticsStatusNotification",
        "FirmwareStatusNotification",
        "Heartbeat",
        "MeterValues",
        "StatusNotification",
        "LogStatusNotification",
        "SignChargePointCertificate"
    )

    override fun validate(payload: Map<String, Any>): Response? {
        val requestedMessage = payload["requestedMessage"]

        if (!PayloadValidators.isValidOneOf(requestedMessage, validMessages)) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "requestedMessage must be one of: BootNotification, DiagnosticsStatusNotification, FirmwareStatusNotification, Heartbeat, MeterValues, StatusNotification"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val requestedMessage = payload["requestedMessage"] as String
        val connectorId = if (payload["connectorId"] is Number) (payload["connectorId"] as Number).toInt() else null

        val result = gateway.sendTriggerMessage(chargePointId, requestedMessage, connectorId)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

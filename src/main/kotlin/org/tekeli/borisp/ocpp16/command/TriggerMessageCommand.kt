package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

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
        val requestedMessage = payload["requestedMessage"] as String?

        if (requestedMessage == null || requestedMessage !in validMessages) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "requestedMessage must be one of: BootNotification, DiagnosticsStatusNotification, FirmwareStatusNotification, Heartbeat, MeterValues, StatusNotification"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val requestedMessage = payload["requestedMessage"] as String
        val connectorId = (payload["connectorId"] as? Number)?.toInt()

        val result = gateway.sendTriggerMessage(chargePointId, requestedMessage, connectorId)
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

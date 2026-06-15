package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@Suppress("unused")
@jakarta.enterprise.context.ApplicationScoped
class RemoteStopTransactionCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "remote-stop-transaction"

    override fun validate(payload: Map<String, Any>): Response? {
        val transactionIdRaw = payload["transactionId"]

        if (transactionIdRaw !is Number) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "transactionId is required"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val transactionId = (payload["transactionId"] as Number).toInt()

        val result = gateway.sendRemoteStopTransaction(chargePointId, transactionId)
        val response = result.get(10, java.util.concurrent.TimeUnit.SECONDS)

        return PayloadValidators.buildCommandResponse(response, name)
    }
}

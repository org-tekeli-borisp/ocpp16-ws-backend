package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class CertificateSignedCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "send-certificate-signed"

    override fun validate(payload: Map<String, Any>): Response? {
        val certificateChain = payload["certificateChain"] as String?

        if (certificateChain.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "certificateChain is required"))
                .build()
        }

        if (certificateChain.length > OcppConstants.MAX_CERTIFICATE_CHAIN_LENGTH) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "certificateChain must not exceed ${OcppConstants.MAX_CERTIFICATE_CHAIN_LENGTH} characters"))
                .build()
        }

        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val certificateChain = payload["certificateChain"] as String

        val result = gateway.sendCertificateSigned(chargePointId, certificateChain)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

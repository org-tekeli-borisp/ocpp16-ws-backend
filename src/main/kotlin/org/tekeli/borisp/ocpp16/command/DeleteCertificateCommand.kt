package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

@jakarta.enterprise.context.ApplicationScoped
class DeleteCertificateCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "delete-certificate"

    private val validHashAlgorithms = setOf("SHA256", "SHA384", "SHA512")

    override fun validate(payload: Map<String, Any>): Response? {
        val certificateHashData = payload["certificateHashData"] as? Map<*, *>
            ?: return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "certificateHashData is required"))
                .build()

        val hashAlgorithm = certificateHashData["hashAlgorithm"] as String?
        if (hashAlgorithm == null || hashAlgorithm !in validHashAlgorithms) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "hashAlgorithm must be one of: SHA256, SHA384, SHA512"))
                .build()
        }

        val issuerNameHash = certificateHashData["issuerNameHash"] as String?
        if (issuerNameHash.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "issuerNameHash is required"))
                .build()
        }

        val issuerKeyHash = certificateHashData["issuerKeyHash"] as String?
        if (issuerKeyHash.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "issuerKeyHash is required"))
                .build()
        }

        val serialNumber = certificateHashData["serialNumber"] as String?
        if (serialNumber.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "serialNumber is required"))
                .build()
        }

        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val certificateHashData = payload["certificateHashData"] as Map<String, Any>

        val result = gateway.sendDeleteCertificate(chargePointId, certificateHashData)
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

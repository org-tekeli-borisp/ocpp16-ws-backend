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
        val certData = payload["certificateHashData"] as? Map<*, *>
            ?: return badRequest("certificateHashData is required")
        val hashAlgorithm = certData["hashAlgorithm"] as String?
        if (hashAlgorithm == null || hashAlgorithm !in validHashAlgorithms) {
            return badRequest("hashAlgorithm must be one of: SHA256, SHA384, SHA512")
        }
        return checkRequiredStringFields(certData, "issuerNameHash", "issuerKeyHash", "serialNumber")
            ?.let { badRequest("$it is required") }
    }

    private fun checkRequiredStringFields(map: Map<*, *>, vararg fields: String): String? {
        for (field in fields) {
            val value = map[field] as String?
            if (value.isNullOrEmpty()) return field
        }
        return null
    }

    private fun badRequest(error: String) = Response.status(Response.Status.BAD_REQUEST)
        .entity(mapOf<String, Any>("error" to error)).build()

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

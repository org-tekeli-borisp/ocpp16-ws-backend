package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

@jakarta.enterprise.context.ApplicationScoped
class InstallCertificateCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "install-certificate"

    private val validCertificateTypes = setOf(
        "CentralSystemRootCertificate",
        "ManufacturerRootCertificate"
    )

    override fun validate(payload: Map<String, Any>): Response? {
        val certificateType = payload["certificateType"] as String?
        val certificate = payload["certificate"] as String?

        if (certificateType == null || certificateType !in validCertificateTypes) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "certificateType must be one of: CentralSystemRootCertificate, ManufacturerRootCertificate"))
                .build()
        }

        if (certificate.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "certificate is required"))
                .build()
        }

        if (certificate.length > 5500) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "certificate must not exceed 5500 characters"))
                .build()
        }

        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val certificateType = payload["certificateType"] as String
        val certificate = payload["certificate"] as String

        val result = gateway.sendInstallCertificate(chargePointId, certificateType, certificate)
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

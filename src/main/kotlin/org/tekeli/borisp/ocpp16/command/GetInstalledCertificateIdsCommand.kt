package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class GetInstalledCertificateIdsCommand @Inject constructor(
    private val gateway: ChargePointGateway
) : OcppCommand {

    override val name = "get-installed-certificate-ids"

    private val validCertificateTypes = setOf(
        "CentralSystemRootCertificate",
        "ManufacturerRootCertificate"
    )

    override fun validate(payload: Map<String, Any>): Response? {
        val certificateType = payload["certificateType"] as String?

        if (certificateType == null || certificateType !in validCertificateTypes) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "certificateType must be one of: CentralSystemRootCertificate, ManufacturerRootCertificate"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val certificateType = payload["certificateType"] as String

        val result = gateway.sendGetInstalledCertificateIds(chargePointId, certificateType)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

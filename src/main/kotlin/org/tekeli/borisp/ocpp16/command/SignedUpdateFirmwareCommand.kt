package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import java.time.Instant

 @jakarta.enterprise.context.ApplicationScoped
class SignedUpdateFirmwareCommand @Inject constructor(
    private val gateway: ChargePointGateway,
    private val persistenceService: PersistenceService? = null
) : OcppCommand {

    override val name = "signed-update-firmware"

    override fun validate(payload: Map<String, Any>): Response? {
        if (payload["requestId"] as? Number == null) {
            return badRequest("requestId is required")
        }
        val firmware = payload["firmware"] as? Map<*, *>
            ?: return badRequest("firmware is required")
        return validateFirmwareFields(firmware)
    }

    private fun validateFirmwareFields(firmware: Map<*, *>): Response? =
        runCatching {
            checkField(firmware, "location", OcppConstants.MAX_FIRMWARE_LOCATION_LENGTH, "firmware.location")
            checkField(firmware, "retrieveDateTime", Int.MAX_VALUE, "firmware.retrieveDateTime")
            checkField(firmware, "signingCertificate", OcppConstants.MAX_SIGNING_CERTIFICATE_LENGTH, "firmware.signingCertificate")
            checkField(firmware, "signature", OcppConstants.MAX_SIGNATURE_LENGTH, "firmware.signature")
        }.exceptionOrNull()?.let { badRequest(it.message ?: "Validation failed") }

    private fun checkField(map: Map<*, *>, key: String, maxLen: Int, name: String) {
        val value = map[key] as String
        if (value.isEmpty() || value.length > maxLen) {
            throw IllegalArgumentException("$name validation failed")
        }
    }

    private fun badRequest(error: String) = Response.status(Response.Status.BAD_REQUEST)
        .entity(mapOf<String, Any>("error" to error)).build()

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val requestId = (payload["requestId"] as Number).toInt()
        val firmware = PayloadValidators.safeMap(payload["firmware"])
        val retries = (payload["retries"] as? Number)?.toInt()
        val retryInterval = (payload["retryInterval"] as? Number)?.toInt()
        return sendFirmwareCommand(chargePointId, requestId, firmware, retries, retryInterval)
    }

    private fun sendFirmwareCommand(
        chargePointId: String, requestId: Int,
        firmware: Map<String, Any>, retries: Int?, retryInterval: Int?
    ): Response {
        val installDateTime = firmware["installDateTime"]?.toString()?.let { Instant.parse(it) }
        persistenceService?.createSignedFirmware(
            chargePointId = chargePointId,
            requestId = requestId,
            location = firmware["location"] as String,
            retrieveDateTime = Instant.parse(firmware["retrieveDateTime"] as String),
            installDateTime = installDateTime,
            signingCertificate = firmware["signingCertificate"] as String,
            signature = firmware["signature"] as String
        )
        val result = gateway.sendSignedUpdateFirmware(chargePointId, requestId, firmware, retries, retryInterval)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }
}

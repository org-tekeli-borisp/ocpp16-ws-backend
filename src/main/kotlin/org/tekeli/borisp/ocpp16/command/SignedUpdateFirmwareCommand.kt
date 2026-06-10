package org.tekeli.borisp.ocpp16.command

import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import java.time.Instant

 @jakarta.enterprise.context.ApplicationScoped
class SignedUpdateFirmwareCommand @Inject constructor(
    private val gateway: ChargePointGateway,
    private val persistenceService: PersistenceService? = null
) : OcppCommand {

    override val name = "signed-update-firmware"

    override fun validate(payload: Map<String, Any>): Response? {
        val requestId = payload["requestId"] as Number?
        if (requestId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "requestId is required"))
                .build()
        }

        val firmware = payload["firmware"] as? Map<*, *>
        if (firmware == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "firmware is required"))
                .build()
        }

        val location = firmware["location"] as String?
        if (location.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "firmware.location is required"))
                .build()
        }

        if (location.length > 512) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "firmware.location must not exceed 512 characters"))
                .build()
        }

        val retrieveDateTime = firmware["retrieveDateTime"] as String?
        if (retrieveDateTime.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "firmware.retrieveDateTime is required"))
                .build()
        }

        val signingCertificate = firmware["signingCertificate"] as String?
        if (signingCertificate.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "firmware.signingCertificate is required"))
                .build()
        }

        if (signingCertificate.length > 5500) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "firmware.signingCertificate must not exceed 5500 characters"))
                .build()
        }

        val signature = firmware["signature"] as String?
        if (signature.isNullOrEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "firmware.signature is required"))
                .build()
        }

        if (signature.length > 800) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "firmware.signature must not exceed 800 characters"))
                .build()
        }

        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val requestId = (payload["requestId"] as Number).toInt()
        val firmware = payload["firmware"] as Map<String, Any>
        val retries = (payload["retries"] as? Number)?.toInt()
        val retryInterval = (payload["retryInterval"] as? Number)?.toInt()

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

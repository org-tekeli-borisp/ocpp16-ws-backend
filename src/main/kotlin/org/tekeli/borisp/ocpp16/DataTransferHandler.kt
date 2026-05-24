package org.tekeli.borisp.ocpp16

class DataTransferHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val vendorId = payload["vendorId"]
        if (vendorId == null || vendorId.toString().isBlank()) {
            throw FormationViolationException("vendorId is required")
        }

        if (vendorId.toString().length > 255) {
            throw FormationViolationException("vendorId must not exceed 255 characters")
        }

        val responsePayload = mapOf(
            "status" to "Accepted"
        )

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }
}

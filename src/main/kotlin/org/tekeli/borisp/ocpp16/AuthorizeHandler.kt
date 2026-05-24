package org.tekeli.borisp.ocpp16

class AuthorizeHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val idTag = payload["idTag"]
        if (idTag == null || idTag.toString().isBlank()) {
            throw FormationViolationException("idTag is required")
        }

        if (idTag.toString().length > 20) {
            throw FormationViolationException("idTag must not exceed 20 characters")
        }

        val responsePayload = mapOf(
            "idTagInfo" to mapOf("status" to "Accepted")
        )

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }
}

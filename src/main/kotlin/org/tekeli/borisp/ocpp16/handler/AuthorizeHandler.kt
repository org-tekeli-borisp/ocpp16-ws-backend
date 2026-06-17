package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

class AuthorizeHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val idTag = payload["idTag"]
        if (idTag == null || idTag.toString().isBlank()) {
            throw FormationViolationException("idTag is required")
        }

        if (idTag.toString().length > OcppConstants.MAX_ID_TAG_LENGTH) {
            throw FormationViolationException("idTag must not exceed ${OcppConstants.MAX_ID_TAG_LENGTH} characters")
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

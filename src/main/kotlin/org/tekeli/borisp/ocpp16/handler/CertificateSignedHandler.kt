package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

class CertificateSignedHandler : OcppActionHandler {

    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val certificateChain = payload["certificateChain"]
        if (certificateChain == null || certificateChain.toString().isBlank()) {
            throw FormationViolationException("certificateChain is required")
        }

        if (certificateChain.toString().length > 10000) {
            throw FormationViolationException("certificateChain must not exceed 10000 characters")
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

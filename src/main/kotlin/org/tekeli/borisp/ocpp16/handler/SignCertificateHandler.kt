package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

class SignCertificateHandler : OcppActionHandler {

    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val csr = payload["csr"]
        if (csr == null || csr.toString().isBlank()) {
            throw FormationViolationException("csr is required")
        }

        if (csr.toString().length > OcppConstants.MAX_CSR_LENGTH) {
            throw FormationViolationException("csr must not exceed ${OcppConstants.MAX_CSR_LENGTH} characters")
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

package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class SignCertificateHandler : OcppActionHandler {

    override fun handle(call: OcppMessage.Call, server: OcppWebSocketServer): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val csr = payload["csr"]
        if (csr == null || csr.toString().isBlank()) {
            throw FormationViolationException("csr is required")
        }

        if (csr.toString().length > 5500) {
            throw FormationViolationException("csr must not exceed 5500 characters")
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

package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.callResult

class SignCertificateHandler : OcppActionHandler {

    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")
        payload.requiredString("csr", OcppConstants.MAX_CSR_LENGTH)

        return call.callResult(
            mapOf("status" to "Accepted")
        )
    }
}

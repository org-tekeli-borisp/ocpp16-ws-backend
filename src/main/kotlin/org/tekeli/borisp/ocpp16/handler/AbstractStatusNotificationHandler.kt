package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.callResult

abstract class AbstractStatusNotificationHandler(
    private val validStatuses: Set<String>
) : OcppActionHandler {

    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")
        payload.requiredStringIn("status", validStatuses)

        return call.callResult()
    }
}

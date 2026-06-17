package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

abstract class AbstractStatusNotificationHandler(
    private val validStatuses: Set<String>
) : OcppActionHandler {

    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val status = payload["status"]
        if (status == null || status.toString().isBlank()) {
            throw FormationViolationException("status is required")
        }

        if (status.toString() !in validStatuses) {
            throw FormationViolationException("Invalid status: ${status}")
        }

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = null as Map<String, Any>?
        ).toJson()
    }
}

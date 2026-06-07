package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class DiagnosticsStatusNotificationHandler : OcppActionHandler {
    private val validDiagnosticsStatuses = setOf(
        "Idle", "Uploaded", "UploadFailed", "Uploading"
    )

    override fun handle(call: OcppMessage.Call, server: OcppWebSocketServer): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val status = payload["status"]
        if (status == null) {
            throw FormationViolationException("status is required")
        }

        val statusStr = status.toString().trim()
        if (statusStr.isEmpty()) {
            throw FormationViolationException("status is required")
        }

        if (!validDiagnosticsStatuses.contains(statusStr)) {
            throw FormationViolationException("Invalid status: ${status}")
        }

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = null
        ).toJson()
    }
}

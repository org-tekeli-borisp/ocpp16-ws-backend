package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class FirmwareStatusNotificationHandler : OcppActionHandler {
    private val validFirmwareStatuses = setOf(
        "Downloaded", "DownloadFailed", "Downloading", "Idle",
        "InstallationFailed", "Installing", "Installed"
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

        if (!validFirmwareStatuses.contains(statusStr)) {
            throw FormationViolationException("Invalid status: ${status}")
        }

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = null
        ).toJson()
    }
}

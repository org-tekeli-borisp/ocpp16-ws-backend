package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class SignedFirmwareStatusNotificationHandler : OcppActionHandler {

    private val validStatuses = setOf(
        "Downloaded", "DownloadFailed", "Downloading", "DownloadScheduled",
        "DownloadPaused", "Idle", "InstallationFailed", "Installing",
        "Installed", "InstallRebooting", "InstallScheduled",
        "InstallVerificationFailed", "InvalidSignature", "SignatureVerified"
    )

    override fun handle(call: OcppMessage.Call, server: OcppWebSocketServer): String {
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

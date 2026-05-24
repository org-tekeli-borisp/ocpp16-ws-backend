package org.tekeli.borisp.ocpp16

class FirmwareStatusNotificationHandler : OcppActionHandler {
    private val validFirmwareStatuses = setOf(
        "Downloaded", "DownloadFailed", "Downloading", "Idle",
        "InstallationFailed", "Installing", "Installed"
    )

    override fun handle(call: OcppMessage.Call): String {
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

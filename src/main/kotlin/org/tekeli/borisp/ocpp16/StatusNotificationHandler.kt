package org.tekeli.borisp.ocpp16

class StatusNotificationHandler : OcppActionHandler {
    private val validErrorCodes = setOf(
        "ConnectorLockFailure", "EVCommunicationError", "GroundFailure",
        "HighTemperature", "InternalError", "LocalListConflict", "NoError",
        "OtherError", "OverCurrentFailure", "OverVoltage", "PowerMeterFailure",
        "PowerSwitchFailure", "ReaderFailure", "ResetFailure", "UnderVoltage",
        "WeakSignal"
    )

    private val validConnectorStatuses = setOf(
        "Available", "Preparing", "Charging", "SuspendedEVSE", "SuspendedEV",
        "Finishing", "Reserved", "Unavailable", "Faulted"
    )

    override fun handle(call: OcppMessage.Call): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val connectorId = payload["connectorId"]
        if (connectorId == null) {
            throw FormationViolationException("connectorId is required")
        }

        val connectorIdValue = (connectorId as? Number)?.toInt()
            ?: throw FormationViolationException("connectorId must be an integer")

        if (connectorIdValue < 0) {
            throw FormationViolationException("connectorId must be >= 0")
        }

        val errorCode = payload["errorCode"]
        if (errorCode == null || errorCode.toString().isBlank()) {
            throw FormationViolationException("errorCode is required")
        }

        if (!validErrorCodes.contains(errorCode.toString())) {
            throw FormationViolationException("Invalid errorCode: ${errorCode}")
        }

        val status = payload["status"]
        if (status == null) {
            throw FormationViolationException("status is required")
        }

        val statusStr = status.toString().trim()
        if (statusStr.isEmpty()) {
            throw FormationViolationException("status is required")
        }

        if (!validConnectorStatuses.contains(statusStr)) {
            throw FormationViolationException("Invalid status: ${status}")
        }

        val info = payload["info"]
        if (info != null) {
            val infoStr = info.toString().trim()
            if (infoStr.length > 50) {
                throw FormationViolationException("info must not exceed 50 characters")
            }
        }

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = null
        ).toJson()
    }
}

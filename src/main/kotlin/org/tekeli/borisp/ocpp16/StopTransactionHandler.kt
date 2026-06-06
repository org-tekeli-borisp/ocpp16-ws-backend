package org.tekeli.borisp.ocpp16

class StopTransactionHandler : OcppActionHandler {
    private val validStopReasons = setOf(
        "DeAuthorized", "EmergencyStop", "EVDisconnected", "HardReset",
        "Local", "Other", "PowerLoss", "Reboot", "Remote", "SoftReset",
        "UnlockCommand"
    )

    override fun handle(call: OcppMessage.Call, server: OcppWebSocketServer): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val transactionId = payload["transactionId"]
        if (transactionId == null) {
            throw FormationViolationException("transactionId is required")
        }

        val meterStop = payload["meterStop"]
        if (meterStop == null) {
            throw FormationViolationException("meterStop is required")
        }

        val timestamp = payload["timestamp"]
        if (timestamp == null || timestamp.toString().isBlank()) {
            throw FormationViolationException("timestamp is required")
        }

        val reason = payload["reason"]
        if (reason != null && reason.toString().trim().isNotEmpty()) {
            if (!validStopReasons.contains(reason.toString())) {
                throw FormationViolationException("Invalid reason: ${reason}")
            }
        }

        val idTag = payload["idTag"]
        if (idTag != null) {
            val idTagStr = idTag.toString().trim()
            if (idTagStr.length > 20) {
                throw FormationViolationException("idTag must not exceed 20 characters")
            }
        }

        val responsePayload = mapOf(
            "idTagInfo" to mapOf("status" to "Accepted")
        )

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }
}

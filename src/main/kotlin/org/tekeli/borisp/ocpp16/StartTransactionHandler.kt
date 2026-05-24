package org.tekeli.borisp.ocpp16

class StartTransactionHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val connectorId = payload["connectorId"]
        if (connectorId == null) {
            throw FormationViolationException("connectorId is required")
        }

        val connectorIdValue = (connectorId as? Number)?.toInt()
            ?: throw FormationViolationException("connectorId must be an integer")

        if (connectorIdValue <= 0) {
            throw FormationViolationException("connectorId must be > 0")
        }

        val idTag = payload["idTag"]
        if (idTag == null || idTag.toString().isBlank()) {
            throw FormationViolationException("idTag is required")
        }

        if (idTag.toString().length > 20) {
            throw FormationViolationException("idTag must not exceed 20 characters")
        }

        val meterStart = payload["meterStart"]
        if (meterStart == null) {
            throw FormationViolationException("meterStart is required")
        }

        val timestamp = payload["timestamp"]
        if (timestamp == null || timestamp.toString().isBlank()) {
            throw FormationViolationException("timestamp is required")
        }

        val responsePayload = mapOf(
            "idTagInfo" to mapOf("status" to "Accepted"),
            "transactionId" to 1
        )

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }
}

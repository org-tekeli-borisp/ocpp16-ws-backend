package org.tekeli.borisp.ocpp16

import java.time.Instant

class StartTransactionHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call, server: OcppWebSocketServer): String {
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

        val meterStartValue = (meterStart as? Number)?.toInt()
            ?: throw FormationViolationException("meterStart must be an integer")

        val timestamp = payload["timestamp"]
        if (timestamp == null || timestamp.toString().isBlank()) {
            throw FormationViolationException("timestamp is required")
        }

        val startTime = try {
            Instant.parse(timestamp.toString())
        } catch (e: Exception) {
            throw FormationViolationException("Invalid timestamp format")
        }

        val sessionId = server.getSessionId()
        val ps = server.persistenceService

        var transactionId: Long = 1
        val chargePoint = ps?.findChargePointBySessionId(sessionId)
        if (chargePoint != null && ps != null) {
            val txn = ps.createTransaction(
                chargePointId = chargePoint.chargePointId,
                connectorId = connectorIdValue,
                idTag = idTag.toString(),
                meterStart = meterStartValue,
                startTime = startTime
            )
            transactionId = txn.id ?: 1
        }

        val responsePayload = mapOf(
            "idTagInfo" to mapOf("status" to "Accepted"),
            "transactionId" to transactionId
        )

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }
}

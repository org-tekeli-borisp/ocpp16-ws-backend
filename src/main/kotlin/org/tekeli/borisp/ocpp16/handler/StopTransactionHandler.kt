package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.time.Instant

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

        val transactionIdValue = (transactionId as? Number)?.toLong()
            ?: throw FormationViolationException("transactionId must be an integer")

        val meterStop = payload["meterStop"]
        if (meterStop == null) {
            throw FormationViolationException("meterStop is required")
        }

        val meterStopValue = (meterStop as? Number)?.toInt()
            ?: throw FormationViolationException("meterStop must be an integer")

        val timestamp = payload["timestamp"]
        if (timestamp == null || timestamp.toString().isBlank()) {
            throw FormationViolationException("timestamp is required")
        }

        val stopTime = try {
            Instant.parse(timestamp.toString())
        } catch (e: Exception) {
            throw FormationViolationException("Invalid timestamp format")
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

        server.persistenceService?.stopTransaction(
            transactionId = transactionIdValue,
            meterStop = meterStopValue,
            stopTime = stopTime,
            reason = reason?.toString(),
            idTagEnd = idTag?.toString()?.trim()
        )

        val responsePayload = mapOf(
            "idTagInfo" to mapOf("status" to "Accepted")
        )

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }
}

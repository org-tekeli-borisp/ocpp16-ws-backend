package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.time.ZoneOffset
import java.time.ZonedDateTime

class BootNotificationHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call, server: OcppWebSocketServer): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val vendor = payload["chargePointVendor"]
        if (vendor == null || vendor.toString().isBlank()) {
            throw FormationViolationException("chargePointVendor is required")
        }

        if (vendor.toString().length > 20) {
            throw FormationViolationException("chargePointVendor must not exceed 20 characters")
        }

        val model = payload["chargePointModel"]
        if (model == null || model.toString().isBlank()) {
            throw FormationViolationException("chargePointModel is required")
        }

        if (model.toString().length > 20) {
            throw FormationViolationException("chargePointModel must not exceed 20 characters")
        }

        val firmwareVersion = payload["firmwareVersion"]?.toString()
        val chargePointId = server.chargePointId ?: throw FormationViolationException("No chargePointId from connection")
        server.chargePointRegistry?.updateChargePointInfo(
            server.sessionId,
            chargePointId,
            vendor.toString(),
            model.toString()
        )

        server.persistenceService?.upsertChargePoint(
            sessionId = server.sessionId,
            chargePointId = chargePointId,
            vendor = vendor.toString(),
            model = model.toString(),
            firmwareVersion = firmwareVersion,
            certFingerprint = server.clientCertService?.fingerprint(server.clientCertificate)
        )

        val currentTime = ZonedDateTime.now(ZoneOffset.UTC).toString()
        val responsePayload = mapOf(
            "currentTime" to currentTime,
            "interval" to 300,
            "status" to "Accepted"
        )

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }
}

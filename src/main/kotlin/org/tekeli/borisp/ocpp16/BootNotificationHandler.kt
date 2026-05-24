package org.tekeli.borisp.ocpp16

import java.time.ZoneOffset
import java.time.ZonedDateTime

class BootNotificationHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call): String {
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

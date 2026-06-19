package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.callResult
import java.time.ZoneOffset
import java.time.ZonedDateTime

class BootNotificationHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")
        val (vendor, model, firmwareVersion) = validatePayload(payload)
        val chargePointId = context.chargePointId
            .takeIf { it.isNotBlank() }
            ?: throw FormationViolationException("No chargePointId from connection")

        processBootNotification(context, chargePointId, vendor, model, firmwareVersion)

        return call.callResult(
            mapOf(
                "currentTime" to ZonedDateTime.now(ZoneOffset.UTC).toString(),
                "interval" to OcppConstants.DEFAULT_HEARTBEAT_INTERVAL_SECONDS,
                "status" to "Accepted"
            )
        )
    }

    internal fun processBootNotification(
        context: OcppHandlerContext,
        chargePointId: String,
        vendor: String,
        model: String,
        firmwareVersion: String?
    ) {
        context.chargePointRegistry?.updateChargePointInfo(
            context.sessionId, chargePointId, vendor, model
        )
        context.persistenceService?.upsertChargePoint(
            sessionId = context.sessionId,
            chargePointId = chargePointId,
            vendor = vendor,
            model = model,
            firmwareVersion = firmwareVersion
        )
    }

    internal data class ParsedBootNotification(
        val vendor: String,
        val model: String,
        val firmwareVersion: String?
    )

    internal fun validatePayload(payload: Map<String, Any>): ParsedBootNotification {
        val vendor = payload.requiredString("chargePointVendor", OcppConstants.MAX_VENDOR_LENGTH)
        val model = payload.requiredString("chargePointModel", OcppConstants.MAX_MODEL_LENGTH)
        val firmwareVersion = payload["firmwareVersion"]?.toString()
        return ParsedBootNotification(vendor, model, firmwareVersion)
    }
}

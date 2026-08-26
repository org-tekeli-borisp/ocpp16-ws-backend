package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.callResult
import java.time.Instant

class SecurityEventNotificationHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")
        val (type, timestamp, techInfo) = validatePayload(payload)
        val chargePointId = context.chargePointId
            .takeIf { it.isNotBlank() }
            ?: throw FormationViolationException("No chargePointId from connection")

        processSecurityEvent(context, chargePointId, type, timestamp, techInfo)

        return call.callResult()
    }

    internal fun processSecurityEvent(
        context: OcppHandlerContext,
        chargePointId: String,
        type: String,
        timestamp: Instant,
        techInfo: String?
    ) {
        context.persistenceService?.createSecurityLog(chargePointId, type, timestamp, techInfo)
        context.metricsService?.securityEventsReceived?.increment()
    }

    internal data class ParsedSecurityEvent(
        val type: String,
        val timestamp: Instant,
        val techInfo: String?
    )

    internal fun validatePayload(payload: Map<String, Any>): ParsedSecurityEvent {
        val rawType = payload["type"]?.toString().orEmpty()
        if (rawType.length > OcppConstants.MAX_EVENT_TYPE_LENGTH) {
            throw FormationViolationException("type must not exceed ${OcppConstants.MAX_EVENT_TYPE_LENGTH} characters")
        }
        val type = payload.requiredStringIn("type", OcppConstants.SECURITY_EVENTS)
        val timestamp = payload.requiredInstant("timestamp")
        val techInfo = payload.optionalString("techInfo", OcppConstants.MAX_TECH_INFO_LENGTH)
        return ParsedSecurityEvent(type, timestamp, techInfo)
    }
}

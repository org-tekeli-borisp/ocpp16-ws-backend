package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import java.time.ZoneOffset
import java.time.ZonedDateTime

class HeartbeatHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        try {
            context.persistenceService?.setChargePointOnline(context.sessionId)
        } catch (e: Exception) {
            // Ignore persistence errors on heartbeat
        }
        val currentTime = ZonedDateTime.now(ZoneOffset.UTC).toString()
        val responsePayload = mapOf("currentTime" to currentTime)

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }
}

package org.tekeli.borisp.ocpp16

import java.time.ZoneOffset
import java.time.ZonedDateTime

class HeartbeatHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call, server: OcppWebSocketServer): String {
        val currentTime = ZonedDateTime.now(ZoneOffset.UTC).toString()
        val responsePayload = mapOf("currentTime" to currentTime)

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = responsePayload
        ).toJson()
    }
}

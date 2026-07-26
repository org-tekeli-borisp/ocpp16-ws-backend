package org.tekeli.borisp.ocpp16.websocket

import java.time.Instant

data class ChargePointInfo(
    val sessionId: String,
    val connectionId: String,
    val chargePointId: String? = null,
    val vendor: String? = null,
    val model: String? = null,
    val connectedAt: Instant = Instant.now()
)

class SessionContext(
    val sessionId: String,
    val chargePointId: String,
    val responseAwaiter: org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter,
    var pingPongManager: PingPongManager? = null
)

package org.tekeli.borisp.ocpp16

import java.time.Instant

data class ChargePointInfo(
    val sessionId: String,
    val chargePointId: String? = null,
    val vendor: String? = null,
    val model: String? = null,
    val connectedAt: Instant = Instant.now()
)

package org.tekeli.borisp.ocpp16.protocol

data class OcppMessageDto(
    val chargePointId: String,
    val direction: String,
    val messageType: String,
    val action: String?,
    val messageId: String,
    val payload: String?,
    val timestamp: String
)

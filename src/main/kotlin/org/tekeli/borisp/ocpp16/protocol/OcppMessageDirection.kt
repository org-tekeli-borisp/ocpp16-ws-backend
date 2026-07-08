package org.tekeli.borisp.ocpp16.protocol

enum class OcppMessageDirection(val value: String) {
    INBOUND("INBOUND"),
    OUTBOUND("OUTBOUND");

    companion object {
        fun fromValue(value: String): OcppMessageDirection = entries.find { it.value == value } ?: INBOUND
    }
}

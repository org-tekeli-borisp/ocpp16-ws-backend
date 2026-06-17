package org.tekeli.borisp.ocpp16.outbound

internal fun Map<String, Any?>.filterNulls(): Map<String, Any> =
    entries.filter { it.value != null }
        .associate { it.key to it.value as Any }

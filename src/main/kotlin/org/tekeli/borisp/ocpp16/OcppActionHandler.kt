package org.tekeli.borisp.ocpp16

interface OcppActionHandler {
    fun handle(call: OcppMessage.Call): String
}

package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.OcppMessage

interface OcppActionHandler {
    fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String
}

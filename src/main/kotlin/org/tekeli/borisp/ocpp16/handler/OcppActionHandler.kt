package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

interface OcppActionHandler {
    fun handle(call: OcppMessage.Call, server: OcppWebSocketServer): String
}

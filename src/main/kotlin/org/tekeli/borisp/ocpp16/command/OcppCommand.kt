package org.tekeli.borisp.ocpp16.command

import jakarta.ws.rs.core.Response

interface OcppCommand {
    val name: String

    fun validate(payload: Map<String, Any>): Response?

    fun execute(chargePointId: String, payload: Map<String, Any>): Response
}

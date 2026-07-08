package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.callResult

class StatusNotificationHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")
        validatePayload(payload)

        val connectorId = (payload["connectorId"] as Number).toInt()
        val status = payload["status"] as String
        val errorCode = payload["errorCode"] as String
        val info = payload["info"]?.toString()

        context.persistenceService?.updateConnectorStatus(
            chargePointId = context.chargePointId,
            connectorId = connectorId,
            status = status,
            errorCode = errorCode,
            info = info
        )

        return call.callResult()
    }

    private fun validatePayload(payload: Map<String, Any>) {
        validateTopLevelFields(payload)
        validateInfoField(payload)
    }

    private fun validateTopLevelFields(payload: Map<String, Any>) {
        validateConnectorId(payload)
        validateErrorCode(payload)
        validateStatus(payload)
    }

    private fun validateConnectorId(payload: Map<String, Any>) {
        payload.requiredInt("connectorId", min = 0)
    }

    private fun validateErrorCode(payload: Map<String, Any>) {
        payload.requiredStringIn("errorCode", OcppConstants.ERROR_CODES)
    }

    private fun validateStatus(payload: Map<String, Any>) {
        payload.requiredStringIn("status", OcppConstants.CONNECTOR_STATUSES)
    }

    private fun validateInfoField(payload: Map<String, Any>) {
        payload.optionalString("info", OcppConstants.MAX_INFO_LENGTH)
    }
}

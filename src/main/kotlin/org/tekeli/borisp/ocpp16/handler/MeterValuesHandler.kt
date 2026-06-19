package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.callResult

class MeterValuesHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")
        validateConnectorId(payload)
        validateMeterValues(payload)

        return call.callResult()
    }

    private fun validateConnectorId(payload: Map<String, Any>) {
        payload.requiredInt("connectorId", min = 0)
    }

    private fun validateMeterValues(payload: Map<String, Any>) {
        val meterValue = payload["meterValue"]
        if (meterValue == null) throw FormationViolationException("meterValue is required")
        val meterValueList = meterValue as? List<*>
            ?: throw FormationViolationException("meterValue must be an array")
        if (meterValueList.isEmpty()) {
            throw FormationViolationException("meterValue must contain at least 1 element")
        }
        meterValueList.forEach { mv -> validateSingleMeterValue(mv) }
    }

    private fun validateSingleMeterValue(mv: Any?) {
        val mvMap = mv as? Map<*, *>
            ?: throw FormationViolationException("Each meterValue must be an object")
        val timestamp = mvMap["timestamp"]
        if (timestamp == null || timestamp.toString().trim().isEmpty()) {
            throw FormationViolationException("timestamp is required in meterValue")
        }
        val sampledValues = mvMap["sampledValue"]
        if (sampledValues == null) {
            throw FormationViolationException("sampledValue is required in meterValue")
        }
        validateSampledValues(sampledValues)
    }

    private fun validateSampledValues(sampledValues: Any?) {
        val list = sampledValues as? List<*>
            ?: throw FormationViolationException("sampledValue must be an array")
        if (list.isEmpty()) {
            throw FormationViolationException("sampledValue must contain at least 1 element")
        }
        list.forEach { validateSingleSampledValue(it) }
    }

    private fun validateSingleSampledValue(sv: Any?) {
        val svMap = sv as? Map<*, *>
            ?: throw FormationViolationException("Each sampledValue must be an object")
        if (svMap["value"] == null) {
            throw FormationViolationException("value is required in sampledValue")
        }
    }
}

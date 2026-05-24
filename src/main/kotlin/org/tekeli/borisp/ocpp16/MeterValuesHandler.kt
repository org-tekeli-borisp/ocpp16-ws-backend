package org.tekeli.borisp.ocpp16

class MeterValuesHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")

        val connectorId = payload["connectorId"]
        if (connectorId == null) {
            throw FormationViolationException("connectorId is required")
        }

        val connectorIdValue = (connectorId as? Number)?.toInt()
            ?: throw FormationViolationException("connectorId must be an integer")

        if (connectorIdValue < 0) {
            throw FormationViolationException("connectorId must be >= 0")
        }

        val meterValue = payload["meterValue"]
        if (meterValue == null) {
            throw FormationViolationException("meterValue is required")
        }

        val meterValueList = meterValue as? List<*>
            ?: throw FormationViolationException("meterValue must be an array")

        if (meterValueList.isEmpty()) {
            throw FormationViolationException("meterValue must contain at least 1 element")
        }

        for (mv in meterValueList) {
            val mvMap = mv as? Map<*, *>
                ?: throw FormationViolationException("Each meterValue must be an object")

            val timestamp = mvMap["timestamp"]
            if (timestamp == null || timestamp.toString().trim().isEmpty()) {
                throw FormationViolationException("timestamp is required in meterValue")
            }

            val sampledValue = mvMap["sampledValue"]
            if (sampledValue == null) {
                throw FormationViolationException("sampledValue is required in meterValue")
            }

            val sampledValueList = sampledValue as? List<*>
                ?: throw FormationViolationException("sampledValue must be an array")

            if (sampledValueList.isEmpty()) {
                throw FormationViolationException("sampledValue must contain at least 1 element")
            }

            for (sv in sampledValueList) {
                val svMap = sv as? Map<*, *>
                    ?: throw FormationViolationException("Each sampledValue must be an object")

                if (svMap["value"] == null) {
                    throw FormationViolationException("value is required in sampledValue")
                }
            }
        }

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = null
        ).toJson()
    }
}

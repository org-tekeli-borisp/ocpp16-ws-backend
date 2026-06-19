package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.callResult
import java.time.Instant

class StopTransactionHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")
        val parsed = validatePayload(payload)
        processStopTransaction(context, parsed)
        return call.callResult(
            mapOf("idTagInfo" to mapOf("status" to "Accepted"))
        )
    }

    internal fun processStopTransaction(context: OcppHandlerContext, parsed: ParsedStopTransaction) {
        val ps = context.persistenceService
        val transaction = ps?.findTransaction(parsed.transactionId)
        val energyWh = (parsed.meterStop - (transaction?.meterStart ?: 0)).toDouble()
        val durationSeconds = transaction?.let { parsed.stopTime.epochSecond - it.startTime.epochSecond } ?: 0
        ps?.stopTransaction(parsed.transactionId, parsed.meterStop, parsed.stopTime, parsed.reason, parsed.idTagEnd)
        recordMetrics(context, energyWh, durationSeconds)
    }

    internal fun recordMetrics(context: OcppHandlerContext, energyWh: Double, durationSeconds: Long) {
        context.metricsService?.onTransactionStopped()
        context.metricsService?.energyDeliveredWh?.increment(energyWh)
        context.metricsService?.transactionDuration?.record(durationSeconds, java.util.concurrent.TimeUnit.SECONDS)
    }

    internal data class ParsedStopTransaction(
        val transactionId: Long,
        val meterStop: Int,
        val stopTime: Instant,
        val reason: String?,
        val idTagEnd: String?
    )

    internal fun validatePayload(payload: Map<String, Any>): ParsedStopTransaction {
        val transactionId = payload.requiredLong("transactionId")
        val meterStop = payload.requiredInt("meterStop")
        val stopTime = payload.requiredInstant("timestamp")
        val reason = extractReason(payload)
        val idTagEnd = payload.optionalString("idTag", OcppConstants.MAX_ID_TAG_LENGTH)
        return ParsedStopTransaction(transactionId, meterStop, stopTime, reason, idTagEnd)
    }

    private fun extractReason(payload: Map<String, Any>): String? {
        val reason = payload["reason"] ?: return null
        val reasonStr = reason.toString().trim()
        if (reasonStr.isEmpty()) return null
        if (reasonStr !in OcppConstants.STOP_REASONS) {
            throw FormationViolationException("Invalid reason: ${reason}")
        }
        return reasonStr
    }
}

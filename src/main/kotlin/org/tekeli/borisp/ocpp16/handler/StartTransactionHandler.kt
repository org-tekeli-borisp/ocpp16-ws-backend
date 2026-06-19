package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.callResult
import java.time.Instant

class StartTransactionHandler : OcppActionHandler {
    override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
        val payload = call.payload ?: throw FormationViolationException("Payload is null")
        val (connectorId, idTag, meterStart, startTime) = validatePayload(payload)

        val transactionId = createTransaction(context, connectorId, idTag, meterStart, startTime)

        return call.callResult(
            mapOf(
                "idTagInfo" to mapOf("status" to "Accepted"),
                "transactionId" to transactionId
            )
        )
    }

    internal open fun createTransaction(
        context: OcppHandlerContext,
        connectorId: Int,
        idTag: String,
        meterStart: Int,
        startTime: Instant
    ): Long {
        val ps = context.persistenceService ?: return 1
        val chargePoint = ps.findChargePointBySessionId(context.sessionId) ?: return 1
        val txn = ps.createTransaction(
            chargePointId = chargePoint.chargePointId,
            connectorId = connectorId,
            idTag = idTag,
            meterStart = meterStart,
            startTime = startTime
        )
        context.metricsService?.onTransactionStarted()
        return txn.id ?: 1
    }

    internal data class ParsedStartTransaction(
        val connectorId: Int,
        val idTag: String,
        val meterStart: Int,
        val startTime: Instant
    )

    internal fun validatePayload(payload: Map<String, Any>): ParsedStartTransaction {
        val connectorId = payload.requiredInt("connectorId", min = 1)
        val idTag = payload.requiredString("idTag", OcppConstants.MAX_ID_TAG_LENGTH)
        val meterStart = payload.requiredInt("meterStart")
        val startTime = payload.requiredInstant("timestamp")
        return ParsedStartTransaction(connectorId, idTag, meterStart, startTime)
    }
}

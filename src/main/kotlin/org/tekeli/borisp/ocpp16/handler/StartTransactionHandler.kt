package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import java.time.Instant

class StartTransactionHandler(
     internal val metricsService: MetricsService? = null
  ) : OcppActionHandler {
     override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
         val payload = call.payload ?: throw FormationViolationException("Payload is null")
         val (connectorId, idTag, meterStart, startTime) = validatePayload(payload)

         val transactionId = createTransaction(context, connectorId, idTag, meterStart, startTime)

         return OcppMessage.CallResult(
             messageId = call.messageId,
             payload = mapOf(
                 "idTagInfo" to mapOf("status" to "Accepted"),
                 "transactionId" to transactionId
             )
         ).toJson()
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
         metricsService?.onTransactionStarted()
         return txn.id ?: 1
     }

     internal data class ParsedStartTransaction(
         val connectorId: Int,
         val idTag: String,
         val meterStart: Int,
         val startTime: Instant
     )

     internal fun validatePayload(payload: Map<String, Any>): ParsedStartTransaction {
         val connectorId = extractConnectorId(payload)
         val idTag = extractIdTag(payload)
         val meterStart = extractMeterStart(payload)
         val startTime = extractStartTime(payload)
         return ParsedStartTransaction(connectorId, idTag, meterStart, startTime)
     }

     private fun extractConnectorId(payload: Map<String, Any>): Int {
         val value = payload["connectorId"]
         if (value == null) throw FormationViolationException("connectorId is required")
         val intValue = (value as? Number)?.toInt()
             ?: throw FormationViolationException("connectorId must be an integer")
         if (intValue <= 0) throw FormationViolationException("connectorId must be > 0")
         return intValue
     }

     private fun extractIdTag(payload: Map<String, Any>): String {
         val value = payload["idTag"]
         if (value == null || value.toString().isBlank()) {
             throw FormationViolationException("idTag is required")
         }
         if (value.toString().length > OcppConstants.MAX_ID_TAG_LENGTH) {
              throw FormationViolationException("idTag must not exceed ${OcppConstants.MAX_ID_TAG_LENGTH} characters")
         }
         return value.toString()
     }

     private fun extractMeterStart(payload: Map<String, Any>): Int {
         val value = payload["meterStart"]
         if (value == null) throw FormationViolationException("meterStart is required")
         return (value as? Number)?.toInt()
             ?: throw FormationViolationException("meterStart must be an integer")
     }

     private fun extractStartTime(payload: Map<String, Any>): Instant {
         val timestamp = payload["timestamp"]
         if (timestamp == null || timestamp.toString().isBlank()) {
             throw FormationViolationException("timestamp is required")
         }
         return try {
             Instant.parse(timestamp.toString())
         } catch (e: Exception) {
             throw FormationViolationException("Invalid timestamp format")
         }
     }
 }

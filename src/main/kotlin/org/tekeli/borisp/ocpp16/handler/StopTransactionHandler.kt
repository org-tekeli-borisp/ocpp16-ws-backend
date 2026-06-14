package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.time.Instant

class StopTransactionHandler(
     private val metricsService: MetricsService? = null
 ) : OcppActionHandler {
     private val validStopReasons = setOf(
         "DeAuthorized", "EmergencyStop", "EVDisconnected", "HardReset",
         "Local", "Other", "PowerLoss", "Reboot", "Remote", "SoftReset",
         "UnlockCommand"
     )

     override fun handle(call: OcppMessage.Call, server: OcppWebSocketServer): String {
         val payload = call.payload ?: throw FormationViolationException("Payload is null")
         val (transactionId, meterStop, stopTime, reason, idTagEnd) = validatePayload(payload)

         processStopTransaction(server, call.messageId, transactionId, meterStop, stopTime, reason, idTagEnd)

         return OcppMessage.CallResult(
             messageId = call.messageId,
             payload = mapOf("idTagInfo" to mapOf("status" to "Accepted"))
         ).toJson()
     }

     private fun processStopTransaction(
         server: OcppWebSocketServer,
         messageId: String,
         transactionId: Long,
         meterStop: Int,
         stopTime: Instant,
         reason: String?,
         idTagEnd: String?
     ) {
         val ps = server.persistenceService
         val transaction = ps?.findTransaction(transactionId)
         val energyWh = (meterStop - (transaction?.meterStart ?: 0)).toDouble()
         val durationSeconds = transaction?.startTime?.let { stopTime.epochSecond - it.epochSecond } ?: 0

         ps?.stopTransaction(transactionId, meterStop, stopTime, reason, idTagEnd)
         recordMetrics(energyWh, durationSeconds)
     }

     private fun recordMetrics(energyWh: Double, durationSeconds: Long) {
         metricsService?.onTransactionStopped()
         metricsService?.energyDeliveredWh?.increment(energyWh)
         metricsService?.transactionDuration?.record(durationSeconds, java.util.concurrent.TimeUnit.SECONDS)
     }

     internal data class ParsedStopTransaction(
         val transactionId: Long,
         val meterStop: Int,
         val stopTime: Instant,
         val reason: String?,
         val idTagEnd: String?
     )

     internal fun validatePayload(payload: Map<String, Any>): ParsedStopTransaction {
         val transactionId = extractTransactionId(payload)
         val meterStop = extractMeterStop(payload)
         val stopTime = extractStopTime(payload)
         val reason = extractReason(payload)
         val idTagEnd = extractIdTagEnd(payload)
         return ParsedStopTransaction(transactionId, meterStop, stopTime, reason, idTagEnd)
     }

     private fun extractTransactionId(payload: Map<String, Any>): Long {
         val value = payload["transactionId"]
         if (value == null) throw FormationViolationException("transactionId is required")
         return (value as? Number)?.toLong()
             ?: throw FormationViolationException("transactionId must be an integer")
     }

     private fun extractMeterStop(payload: Map<String, Any>): Int {
         val value = payload["meterStop"]
         if (value == null) throw FormationViolationException("meterStop is required")
         return (value as? Number)?.toInt()
             ?: throw FormationViolationException("meterStop must be an integer")
     }

     private fun extractStopTime(payload: Map<String, Any>): Instant {
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

     private fun extractReason(payload: Map<String, Any>): String? {
         val reason = payload["reason"] ?: return null
         val reasonStr = reason.toString().trim()
         if (reasonStr.isEmpty()) return null
         if (!validStopReasons.contains(reasonStr)) {
             throw FormationViolationException("Invalid reason: ${reason}")
         }
         return reasonStr
     }

     private fun extractIdTagEnd(payload: Map<String, Any>): String? {
         val idTag = payload["idTag"] ?: return null
         val idTagStr = idTag.toString().trim()
         if (idTagStr.length > 20) {
             throw FormationViolationException("idTag must not exceed 20 characters")
         }
         return if (idTagStr.isEmpty()) null else idTagStr
     }
 }

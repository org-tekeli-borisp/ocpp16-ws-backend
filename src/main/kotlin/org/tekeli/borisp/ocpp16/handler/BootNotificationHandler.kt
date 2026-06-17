package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import java.time.ZoneOffset
import java.time.ZonedDateTime

class BootNotificationHandler : OcppActionHandler {
     override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
         val payload = call.payload ?: throw FormationViolationException("Payload is null")
         val (vendor, model, firmwareVersion) = validatePayload(payload)
         val chargePointId = context.chargePointId
             .takeIf { it.isNotBlank() }
             ?: throw FormationViolationException("No chargePointId from connection")

         processBootNotification(context, chargePointId, vendor, model, firmwareVersion)

         return OcppMessage.CallResult(
             messageId = call.messageId,
             payload = mapOf(
                 "currentTime" to ZonedDateTime.now(ZoneOffset.UTC).toString(),
                 "interval" to 300,
                 "status" to "Accepted"
             )
         ).toJson()
     }

     internal fun processBootNotification(
         context: OcppHandlerContext,
         chargePointId: String,
         vendor: String,
         model: String,
         firmwareVersion: String?
     ) {
         context.chargePointRegistry?.updateChargePointInfo(
             context.sessionId, chargePointId, vendor, model
         )
         context.persistenceService?.upsertChargePoint(
             sessionId = context.sessionId,
             chargePointId = chargePointId,
             vendor = vendor,
             model = model,
             firmwareVersion = firmwareVersion
         )
     }

     internal data class ParsedBootNotification(
         val vendor: String,
         val model: String,
         val firmwareVersion: String?
     )

     internal fun validatePayload(payload: Map<String, Any>): ParsedBootNotification {
         val vendor = extractStringField(payload, "chargePointVendor", 20)
         val model = extractStringField(payload, "chargePointModel", 20)
         val firmwareVersion = payload["firmwareVersion"]?.toString()
         return ParsedBootNotification(vendor, model, firmwareVersion)
     }

     internal fun extractStringField(payload: Map<String, Any>, fieldName: String, maxLength: Int): String {
         val value = payload[fieldName]
         if (value == null || value.toString().isBlank()) {
             throw FormationViolationException("$fieldName is required")
         }
         if (value.toString().length > maxLength) {
             throw FormationViolationException("$fieldName must not exceed $maxLength characters")
         }
         return value.toString()
     }
 }

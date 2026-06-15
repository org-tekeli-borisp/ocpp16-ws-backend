package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.time.ZoneOffset
import java.time.ZonedDateTime

class BootNotificationHandler : OcppActionHandler {
     override fun handle(call: OcppMessage.Call, server: OcppWebSocketServer): String {
         val payload = call.payload ?: throw FormationViolationException("Payload is null")
         val (vendor, model, firmwareVersion) = validatePayload(payload)
         val chargePointId = server.chargePointId
             ?: throw FormationViolationException("No chargePointId from connection")

         processBootNotification(server, chargePointId, vendor, model, firmwareVersion)

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
         server: OcppWebSocketServer,
         chargePointId: String,
         vendor: String,
         model: String,
         firmwareVersion: String?
     ) {
         server.chargePointRegistry?.updateChargePointInfo(
             server.sessionId, chargePointId, vendor, model
         )
         server.persistenceService?.upsertChargePoint(
             sessionId = server.sessionId,
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

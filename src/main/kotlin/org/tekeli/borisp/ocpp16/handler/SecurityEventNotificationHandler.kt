package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer
import java.time.Instant

class SecurityEventNotificationHandler(
     private val persistenceService: PersistenceService? = null
 ) : OcppActionHandler {

     private val validSecurityEvents = setOf(
         "FirmwareUpdated",
         "FirmwareVerificationFailed",
         "InvalidChargePointCertificate",
         "InvalidCentralSystemCertificate",
         "InvalidTLSCipherSuite",
         "InvalidTLSVersion",
         "LocalAccess",
         "ResetFailed",
         "Reset",
         "Tampering",
         "TransactionInfoNotStored",
         "InvalidFirmwareSigningCertificate",
         "InvalidFirmwareSignature",
         "DiscardedRenewedClientCertificate",
         "UnauthorizedAccess"
     )

     override fun handle(call: OcppMessage.Call, server: OcppWebSocketServer): String {
         val payload = call.payload ?: throw FormationViolationException("Payload is null")
         val (type, timestamp, techInfo) = validatePayload(payload)
         val chargePointId = server.chargePointId
             ?: throw FormationViolationException("No chargePointId from connection")

         processSecurityEvent(server, chargePointId, type, timestamp, techInfo)

         return OcppMessage.CallResult(
             messageId = call.messageId,
             payload = emptyMap<String, Any>()
         ).toJson()
     }

     private fun processSecurityEvent(
         server: OcppWebSocketServer,
         chargePointId: String,
         type: String,
         timestamp: Instant,
         techInfo: String?
     ) {
         persistenceService?.createSecurityLog(chargePointId, type, timestamp, techInfo)
         server.metricsService?.securityEventsReceived?.increment()
     }

     private data class ParsedSecurityEvent(
         val type: String,
         val timestamp: Instant,
         val techInfo: String?
     )

     private fun validatePayload(payload: Map<String, Any>): ParsedSecurityEvent {
         val type = extractType(payload)
         val timestamp = extractTimestamp(payload)
         val techInfo = extractTechInfo(payload)
         return ParsedSecurityEvent(type, timestamp, techInfo)
     }

     private fun extractType(payload: Map<String, Any>): String {
         val type = payload["type"]
         if (type == null || type.toString().isBlank()) {
             throw FormationViolationException("type is required")
         }
         if (type.toString().length > 50) {
             throw FormationViolationException("type must not exceed 50 characters")
         }
         return type.toString()
     }

     private fun extractTimestamp(payload: Map<String, Any>): Instant {
         val timestamp = payload["timestamp"]
         if (timestamp == null || timestamp.toString().isBlank()) {
             throw FormationViolationException("timestamp is required")
         }
         return Instant.parse(timestamp.toString())
     }

     private fun extractTechInfo(payload: Map<String, Any>): String? {
         val techInfo = payload["techInfo"]?.toString() ?: return null
         if (techInfo.length > 255) {
             throw FormationViolationException("techInfo must not exceed 255 characters")
         }
         return techInfo
     }
 }

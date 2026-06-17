package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
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

     override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
         val payload = call.payload ?: throw FormationViolationException("Payload is null")
         val (type, timestamp, techInfo) = validatePayload(payload)
         val chargePointId = context.chargePointId
             .takeIf { it.isNotBlank() }
             ?: throw FormationViolationException("No chargePointId from connection")

         processSecurityEvent(context, chargePointId, type, timestamp, techInfo)

         return OcppMessage.CallResult(
             messageId = call.messageId,
             payload = emptyMap<String, Any>()
         ).toJson()
     }

 internal fun processSecurityEvent(
       context: OcppHandlerContext,
       chargePointId: String,
       type: String,
       timestamp: Instant,
       techInfo: String?
   ) {
       persistenceService?.createSecurityLog(chargePointId, type, timestamp, techInfo)
       context.metricsService?.securityEventsReceived?.increment()
   }

    internal data class ParsedSecurityEvent(
        val type: String,
        val timestamp: Instant,
        val techInfo: String?
    )

    internal fun validatePayload(payload: Map<String, Any>): ParsedSecurityEvent {
        val type = extractType(payload)
        val timestamp = extractTimestamp(payload)
        val techInfo = extractTechInfo(payload)
        return ParsedSecurityEvent(type, timestamp, techInfo)
    }

    internal fun extractType(payload: Map<String, Any>): String {
        val type = payload["type"]
        if (type == null || type.toString().isBlank()) {
            throw FormationViolationException("type is required")
        }
        val typeStr = type.toString()
        if (typeStr.length > OcppConstants.MAX_EVENT_TYPE_LENGTH) {
            throw FormationViolationException("type must not exceed ${OcppConstants.MAX_EVENT_TYPE_LENGTH} characters")
        }
        if (typeStr !in validSecurityEvents) {
            throw FormationViolationException("Invalid security event type: $typeStr")
        }
        return typeStr
    }

     internal fun extractTimestamp(payload: Map<String, Any>): Instant {
         val timestamp = payload["timestamp"]
         if (timestamp == null || timestamp.toString().isBlank()) {
             throw FormationViolationException("timestamp is required")
         }
         return Instant.parse(timestamp.toString())
     }

     internal fun extractTechInfo(payload: Map<String, Any>): String? {
         val techInfo = payload["techInfo"]?.toString() ?: return null
         if (techInfo.length > OcppConstants.MAX_TECH_INFO_LENGTH) {
              throw FormationViolationException("techInfo must not exceed ${OcppConstants.MAX_TECH_INFO_LENGTH} characters")
         }
         return techInfo
     }
 }

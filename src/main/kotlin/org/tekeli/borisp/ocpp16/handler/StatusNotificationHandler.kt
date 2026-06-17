package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

class StatusNotificationHandler : OcppActionHandler {
     private val validErrorCodes = setOf(
         "ConnectorLockFailure", "EVCommunicationError", "GroundFailure",
         "HighTemperature", "InternalError", "LocalListConflict", "NoError",
         "OtherError", "OverCurrentFailure", "OverVoltage", "PowerMeterFailure",
         "PowerSwitchFailure", "ReaderFailure", "ResetFailure", "UnderVoltage",
         "WeakSignal"
     )

     private val validConnectorStatuses = setOf(
         "Available", "Preparing", "Charging", "SuspendedEVSE", "SuspendedEV",
         "Finishing", "Reserved", "Unavailable", "Faulted"
     )

     override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
         val payload = call.payload ?: throw FormationViolationException("Payload is null")
         validatePayload(payload)

         return OcppMessage.CallResult(
             messageId = call.messageId,
             payload = null
         ).toJson()
     }

     private fun validatePayload(payload: Map<String, Any>) {
         validateTopLevelFields(payload)
         validateInfoField(payload)
     }

     private fun validateTopLevelFields(payload: Map<String, Any>) {
         validateConnectorId(payload)
         validateErrorCode(payload)
         validateStatus(payload)
     }

     private fun validateConnectorId(payload: Map<String, Any>) {
         val value = payload["connectorId"]
         if (value == null) throw FormationViolationException("connectorId is required")
         val intValue = (value as? Number)?.toInt()
             ?: throw FormationViolationException("connectorId must be an integer")
         if (intValue < 0) throw FormationViolationException("connectorId must be >= 0")
     }

     private fun validateErrorCode(payload: Map<String, Any>) {
         val errorCode = payload["errorCode"]
         if (errorCode == null || errorCode.toString().isBlank()) {
             throw FormationViolationException("errorCode is required")
         }
         if (!validErrorCodes.contains(errorCode.toString())) {
             throw FormationViolationException("Invalid errorCode: ${errorCode}")
         }
     }

     private fun validateStatus(payload: Map<String, Any>) {
         val status = payload["status"]
         if (status == null) throw FormationViolationException("status is required")
         val statusStr = status.toString().trim()
         if (statusStr.isEmpty()) throw FormationViolationException("status is required")
         if (!validConnectorStatuses.contains(statusStr)) {
             throw FormationViolationException("Invalid status: ${status}")
         }
     }

     private fun validateInfoField(payload: Map<String, Any>) {
         val info = payload["info"] ?: return
         val infoStr = info.toString().trim()
         if (infoStr.length > OcppConstants.MAX_INFO_LENGTH) {
              throw FormationViolationException("info must not exceed ${OcppConstants.MAX_INFO_LENGTH} characters")
         }
     }
 }

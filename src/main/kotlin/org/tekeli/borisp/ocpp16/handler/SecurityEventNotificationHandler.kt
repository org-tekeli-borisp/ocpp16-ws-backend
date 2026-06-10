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

        val type = payload["type"]
        if (type == null || type.toString().isBlank()) {
            throw FormationViolationException("type is required")
        }

        if (type.toString().length > 50) {
            throw FormationViolationException("type must not exceed 50 characters")
        }

        val timestamp = payload["timestamp"]
        if (timestamp == null || timestamp.toString().isBlank()) {
            throw FormationViolationException("timestamp is required")
        }

        val techInfo = payload["techInfo"]?.toString()

        if (techInfo != null && techInfo.length > 255) {
            throw FormationViolationException("techInfo must not exceed 255 characters")
        }

        val chargePointId = server.chargePointId
            ?: throw FormationViolationException("No chargePointId from connection")

        persistenceService?.createSecurityLog(
            chargePointId = chargePointId,
            type = type.toString(),
            timestamp = Instant.parse(timestamp.toString()),
            techInfo = techInfo
        )

        server.metricsService?.securityEventsReceived?.increment()

        return OcppMessage.CallResult(
            messageId = call.messageId,
            payload = emptyMap<String, Any>()
        ).toJson()
    }
}

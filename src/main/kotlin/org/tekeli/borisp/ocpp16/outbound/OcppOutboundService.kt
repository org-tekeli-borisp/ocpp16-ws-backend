package org.tekeli.borisp.ocpp16.outbound

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import java.util.concurrent.CompletableFuture

@ApplicationScoped
class OcppOutboundService : ChargePointGateway {

    @Inject
    lateinit var chargePointRegistry: ChargePointRegistry

    override fun sendReset(chargePointId: String, type: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "Reset", mapOf("type" to type))

    override fun sendClearCache(chargePointId: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "ClearCache", null)

    override fun sendChangeConfiguration(chargePointId: String, key: String, value: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "ChangeConfiguration", mapOf("key" to key, "value" to value))

    override fun sendChangeAvailability(chargePointId: String, connectorId: Int, type: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "ChangeAvailability", mapOf("connectorId" to connectorId, "type" to type))

    override fun sendGetConfiguration(chargePointId: String, keys: List<String>?): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "GetConfiguration", keys?.let { mapOf("key" to it) })

    override fun sendRemoteStopTransaction(chargePointId: String, transactionId: Int): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "RemoteStopTransaction", mapOf("transactionId" to transactionId))

    override fun sendTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("requestedMessage" to requestedMessage)
        connectorId?.let { payload["connectorId"] = it }
        return chargePointRegistry.sendCall(chargePointId, "TriggerMessage", payload)
    }

    override fun sendUnlockConnector(chargePointId: String, connectorId: Int): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "UnlockConnector", mapOf("connectorId" to connectorId))

    override fun sendUpdateFirmware(chargePointId: String, location: String, retrieveDate: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("location" to location, "retrieveDate" to retrieveDate)
        retries?.let { payload["retries"] = it }
        retryInterval?.let { payload["retryInterval"] = it }
        return chargePointRegistry.sendCall(chargePointId, "UpdateFirmware", payload)
    }

    override fun sendGetDiagnostics(chargePointId: String, location: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("location" to location)
        retries?.let { payload["retries"] = it }
        retryInterval?.let { payload["retryInterval"] = it }
        return chargePointRegistry.sendCall(chargePointId, "GetDiagnostics", payload)
    }

    override fun sendGetLocalListVersion(chargePointId: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "GetLocalListVersion", null)

    override fun sendRemoteStartTransaction(chargePointId: String, idTag: String, connectorId: Int?): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("idTag" to idTag)
        connectorId?.let { payload["connectorId"] = it }
        return chargePointRegistry.sendCall(chargePointId, "RemoteStartTransaction", payload)
    }

    override fun sendReserveNow(chargePointId: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "ReserveNow", mapOf("connectorId" to connectorId, "expiryDate" to expiryDate, "idTag" to idTag, "reservationId" to reservationId))

    override fun sendCancelReservation(chargePointId: String, reservationId: Int): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "CancelReservation", mapOf("reservationId" to reservationId))

    override fun sendSendLocalList(chargePointId: String, listVersion: Int, updateType: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "SendLocalList", mapOf("listVersion" to listVersion, "updateType" to updateType))

    override fun sendSetChargingProfile(chargePointId: String, connectorId: Int, csChargingProfiles: Map<String, Any>): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "SetChargingProfile", mapOf("connectorId" to connectorId, "csChargingProfiles" to csChargingProfiles))

    override fun sendClearChargingProfile(chargePointId: String, connectorId: Int?, stackLevel: Int?): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>()
        connectorId?.let { payload["connectorId"] = it }
        stackLevel?.let { payload["stackLevel"] = it }
        return chargePointRegistry.sendCall(chargePointId, "ClearChargingProfile", payload)
    }

    override fun sendGetCompositeSchedule(chargePointId: String, connectorId: Int, duration: Int): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "GetCompositeSchedule", mapOf("connectorId" to connectorId, "duration" to duration))

    // Security messages
    override fun sendExtendedTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("requestedMessage" to requestedMessage)
        connectorId?.let { payload["connectorId"] = it }
        return chargePointRegistry.sendCall(chargePointId, "ExtendedTriggerMessage", payload)
    }

    override fun sendInstallCertificate(chargePointId: String, certificateType: String, certificate: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "InstallCertificate", mapOf("certificateType" to certificateType, "certificate" to certificate))

    override fun sendGetInstalledCertificateIds(chargePointId: String, certificateType: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "GetInstalledCertificateIds", mapOf("certificateType" to certificateType))

    override fun sendDeleteCertificate(chargePointId: String, certificateHashData: Map<String, Any>): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "DeleteCertificate", mapOf("certificateHashData" to certificateHashData))

    override fun sendGetLog(chargePointId: String, logType: String, requestId: Int, log: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>(
            "logType" to logType,
            "requestId" to requestId,
            "log" to log
        )
        retries?.let { payload["retries"] = it }
        retryInterval?.let { payload["retryInterval"] = it }
        return chargePointRegistry.sendCall(chargePointId, "GetLog", payload)
    }

    override fun sendSignedUpdateFirmware(chargePointId: String, requestId: Int, firmware: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>(
            "requestId" to requestId,
            "firmware" to firmware
        )
        retries?.let { payload["retries"] = it }
        retryInterval?.let { payload["retryInterval"] = it }
        return chargePointRegistry.sendCall(chargePointId, "SignedUpdateFirmware", payload)
    }

    override fun sendCertificateSigned(chargePointId: String, certificateChain: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "CertificateSigned", mapOf("certificateChain" to certificateChain))
}
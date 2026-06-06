package org.tekeli.borisp.ocpp16

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import java.util.concurrent.CompletableFuture

@ApplicationScoped
class OcppOutboundService {

    @Inject
    lateinit var chargePointRegistry: ChargePointRegistry

    fun sendReset(chargePointId: String, type: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "Reset", mapOf("type" to type))

    fun sendClearCache(chargePointId: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "ClearCache", null)

    fun sendChangeConfiguration(chargePointId: String, key: String, value: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "ChangeConfiguration", mapOf("key" to key, "value" to value))

    fun sendChangeAvailability(chargePointId: String, connectorId: Int, type: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "ChangeAvailability", mapOf("connectorId" to connectorId, "type" to type))

    fun sendGetConfiguration(chargePointId: String, keys: List<String>? = null): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "GetConfiguration", keys?.let { mapOf("key" to it) })

    fun sendRemoteStopTransaction(chargePointId: String, transactionId: Int): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "RemoteStopTransaction", mapOf("transactionId" to transactionId))

    fun sendTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("requestedMessage" to requestedMessage)
        connectorId?.let { payload["connectorId"] = it }
        return chargePointRegistry.sendCall(chargePointId, "TriggerMessage", payload)
    }

    fun sendUnlockConnector(chargePointId: String, connectorId: Int): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "UnlockConnector", mapOf("connectorId" to connectorId))

    fun sendUpdateFirmware(chargePointId: String, location: String, retrieveDate: String, retries: Int? = null, retryInterval: Int? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("location" to location, "retrieveDate" to retrieveDate)
        retries?.let { payload["retries"] = it }
        retryInterval?.let { payload["retryInterval"] = it }
        return chargePointRegistry.sendCall(chargePointId, "UpdateFirmware", payload)
    }

    fun sendGetDiagnostics(chargePointId: String, location: String, retries: Int? = null, retryInterval: Int? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("location" to location)
        retries?.let { payload["retries"] = it }
        retryInterval?.let { payload["retryInterval"] = it }
        return chargePointRegistry.sendCall(chargePointId, "GetDiagnostics", payload)
    }

    fun sendGetLocalListVersion(chargePointId: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "GetLocalListVersion", null)

    fun sendRemoteStartTransaction(chargePointId: String, idTag: String, connectorId: Int? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>("idTag" to idTag)
        connectorId?.let { payload["connectorId"] = it }
        return chargePointRegistry.sendCall(chargePointId, "RemoteStartTransaction", payload)
    }

    fun sendReserveNow(chargePointId: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "ReserveNow", mapOf("connectorId" to connectorId, "expiryDate" to expiryDate, "idTag" to idTag, "reservationId" to reservationId))

    fun sendCancelReservation(chargePointId: String, reservationId: Int): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "CancelReservation", mapOf("reservationId" to reservationId))

    fun sendSendLocalList(chargePointId: String, listVersion: Int, updateType: String): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "SendLocalList", mapOf("listVersion" to listVersion, "updateType" to updateType))

    fun sendSetChargingProfile(chargePointId: String, connectorId: Int, csChargingProfiles: Map<String, Any>): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "SetChargingProfile", mapOf("connectorId" to connectorId, "csChargingProfiles" to csChargingProfiles))

    fun sendClearChargingProfile(chargePointId: String, connectorId: Int? = null, stackLevel: Int? = null): CompletableFuture<OcppMessage> {
        val payload = mutableMapOf<String, Any>()
        connectorId?.let { payload["connectorId"] = it }
        stackLevel?.let { payload["stackLevel"] = it }
        return chargePointRegistry.sendCall(chargePointId, "ClearChargingProfile", payload)
    }

    fun sendGetCompositeSchedule(chargePointId: String, connectorId: Int, duration: Int): CompletableFuture<OcppMessage> =
        chargePointRegistry.sendCall(chargePointId, "GetCompositeSchedule", mapOf("connectorId" to connectorId, "duration" to duration))
}

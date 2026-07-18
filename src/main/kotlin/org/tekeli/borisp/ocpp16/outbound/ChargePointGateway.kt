package org.tekeli.borisp.ocpp16.outbound

import java.util.concurrent.CompletableFuture
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

interface ChargePointGateway {
    fun sendReset(chargePointId: String, type: String): CompletableFuture<OcppMessage>
    fun sendRemoteStartTransaction(chargePointId: String, idTag: String, connectorId: Int?): CompletableFuture<OcppMessage>
    fun sendRemoteStopTransaction(chargePointId: String, transactionId: Int): CompletableFuture<OcppMessage>
    fun sendUnlockConnector(chargePointId: String, connectorId: Int): CompletableFuture<OcppMessage>
    fun sendCancelReservation(chargePointId: String, reservationId: Int): CompletableFuture<OcppMessage>
    fun sendChangeAvailability(chargePointId: String, connectorId: Int, type: String): CompletableFuture<OcppMessage>
    fun sendChangeConfiguration(chargePointId: String, key: String, value: String): CompletableFuture<OcppMessage>
    fun sendClearCache(chargePointId: String): CompletableFuture<OcppMessage>
    fun sendClearChargingProfile(chargePointId: String, connectorId: Int?, stackLevel: Int?): CompletableFuture<OcppMessage>
    fun sendGetCompositeSchedule(chargePointId: String, connectorId: Int, duration: Int): CompletableFuture<OcppMessage>
    fun sendGetConfiguration(chargePointId: String, keys: List<String>?): CompletableFuture<OcppMessage>
    fun sendGetDiagnostics(chargePointId: String, location: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage>
    fun sendGetLocalListVersion(chargePointId: String): CompletableFuture<OcppMessage>
    fun sendReserveNow(chargePointId: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int): CompletableFuture<OcppMessage>
    fun sendSendLocalList(chargePointId: String, listVersion: Int, updateType: String): CompletableFuture<OcppMessage>
    fun sendSetChargingProfile(chargePointId: String, connectorId: Int, csChargingProfiles: Map<String, Any>): CompletableFuture<OcppMessage>
    fun sendTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage>
    fun sendUpdateFirmware(chargePointId: String, location: String, retrieveDate: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage>

    // Security messages
    fun sendExtendedTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage>
    fun sendInstallCertificate(chargePointId: String, certificateType: String, certificate: String): CompletableFuture<OcppMessage>
    fun sendGetInstalledCertificateIds(chargePointId: String, certificateType: String): CompletableFuture<OcppMessage>
    fun sendDeleteCertificate(chargePointId: String, certificateHashData: Map<String, Any>): CompletableFuture<OcppMessage>
    fun sendGetLog(chargePointId: String, logType: String, requestId: Int, log: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage>
    fun sendSignedUpdateFirmware(chargePointId: String, requestId: Int, firmware: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage>
    fun sendCertificateSigned(chargePointId: String, certificateChain: String): CompletableFuture<OcppMessage>
    fun sendDataTransfer(chargePointId: String, vendorId: String, messageId: String?, data: String?): CompletableFuture<OcppMessage>
}
package org.tekeli.borisp.ocpp16.outbound

import java.util.concurrent.CompletableFuture
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

interface ChargePointGateway {
    fun sendReset(chargePointId: String, type: String): CompletableFuture<OcppMessage>
    fun sendRemoteStartTransaction(chargePointId: String, idTag: String, connectorId: Int? = null): CompletableFuture<OcppMessage>
    fun sendRemoteStopTransaction(chargePointId: String, transactionId: Int): CompletableFuture<OcppMessage>
    fun sendUnlockConnector(chargePointId: String, connectorId: Int): CompletableFuture<OcppMessage>
}

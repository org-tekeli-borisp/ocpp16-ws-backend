package org.tekeli.borisp.ocpp16.rest

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.tekeli.borisp.ocpp16.persistence.Transaction

@Path("/api/chargepoints/{chargePointId}/transactions")
@Produces(MediaType.APPLICATION_JSON)
class TransactionResource {

    @Inject
    lateinit var persistenceService: org.tekeli.borisp.ocpp16.persistence.PersistenceService

    @GET
    fun getTransactions(
        @PathParam("chargePointId") chargePointId: String,
        @QueryParam("running") running: Boolean? = null
    ): List<TransactionDto> {
        return if (running == true) {
            persistenceService.findRunningTransactions(chargePointId)
        } else {
            persistenceService.findAllTransactions(chargePointId)
        }.map { toDto(it) }
    }

    private fun toDto(txn: Transaction): TransactionDto = TransactionDto(
        id = txn.id,
        chargePointId = txn.chargePointId,
        connectorId = txn.connectorId,
        idTag = txn.idTag,
        meterStart = txn.meterStart,
        startTime = txn.startTime.toString(),
        stopTime = txn.stopTime?.toString(),
        meterStop = txn.meterStop,
        stopReason = txn.stopReason,
        durationSeconds = txn.durationSeconds,
        energyWh = txn.energyWh
    )
}

data class TransactionDto(
    val id: Long?,
    val chargePointId: String,
    val connectorId: Int,
    val idTag: String,
    val meterStart: Int,
    val startTime: String,
    val stopTime: String?,
    val meterStop: Int?,
    val stopReason: String?,
    val durationSeconds: Long?,
    val energyWh: Int?
)

package org.tekeli.borisp.ocpp16.rest

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.tekeli.borisp.ocpp16.persistence.ChargePoint
import org.tekeli.borisp.ocpp16.persistence.ChargePointStatus
import org.tekeli.borisp.ocpp16.persistence.PersistenceService

@Path("/api/chargepoints")
@Produces(MediaType.APPLICATION_JSON)
class ChargePointResource {

    @Inject
    lateinit var persistenceService: PersistenceService

    @GET
    fun getAll(@QueryParam("status") status: String? = null): List<ChargePointDto> {
        return if (status != null) {
            try {
                persistenceService.findByStatus(ChargePointStatus.valueOf(status)).map { toDto(it) }
            } catch (e: IllegalArgumentException) {
                emptyList()
            }
        } else {
            persistenceService.findAllChargePoints().map { toDto(it) }
        }
    }

    @GET
    @Path("/{chargePointId}")
    fun getById(@PathParam("chargePointId") chargePointId: String): ChargePointDto {
        val cp = persistenceService.findChargePointById(chargePointId)
            ?: throw NotFoundException("ChargePoint not found: $chargePointId")
        return toDto(cp)
    }

    private fun toDto(cp: ChargePoint): ChargePointDto = ChargePointDto(
        id = cp.id,
        chargePointId = cp.chargePointId,
        vendor = cp.vendor,
        model = cp.model,
        firmwareVersion = cp.firmwareVersion,
        status = cp.status.name,
        sessionId = cp.sessionId,
        lastSeenAt = cp.lastSeenAt.toString(),
        createdAt = cp.createdAt.toString()
    )
}

data class ChargePointDto(
    val id: Long?,
    val chargePointId: String,
    val vendor: String?,
    val model: String?,
    val firmwareVersion: String?,
    val status: String,
    val sessionId: String,
    val lastSeenAt: String,
    val createdAt: String
)

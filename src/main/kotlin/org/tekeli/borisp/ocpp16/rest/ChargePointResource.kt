package org.tekeli.borisp.ocpp16.rest

import jakarta.inject.Inject
import jakarta.ws.rs.BadRequestException
import jakarta.ws.rs.DELETE
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.POST
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.tekeli.borisp.ocpp16.persistence.ChargePoint
import org.tekeli.borisp.ocpp16.persistence.ChargePointStatus
import org.tekeli.borisp.ocpp16.persistence.ConnectorStatusDto
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry

@Path("/api/chargepoints")
@Produces(MediaType.APPLICATION_JSON)
class ChargePointResource {

    @Inject
    lateinit var persistenceService: PersistenceService

    @Inject
    lateinit var chargePointRegistry: ChargePointRegistry

    @GET
    fun getAll(@QueryParam("status") status: String? = null): List<ChargePointDto> {
       return if (status != null) {
            val chargePointStatus = try {
                ChargePointStatus.valueOf(status)
            } catch (e: IllegalArgumentException) {
                throw BadRequestException("Invalid status: $status. Must be one of: ${ChargePointStatus.values().joinToString()}")
            }
            buildList {
                for (cp in persistenceService.findByStatus(chargePointStatus)) {
                    add(toDto(cp))
                }
            }
        } else {
            buildList {
                for (cp in persistenceService.findAllChargePoints()) {
                    add(toDto(cp))
                }
            }
        }
    }

    @GET
    @Path("/{chargePointId}")
    fun getById(@PathParam("chargePointId") chargePointId: String): ChargePointDto {
        val cp = persistenceService.findChargePointById(chargePointId)
            ?: throw NotFoundException("ChargePoint not found: $chargePointId")
        return toDto(cp)
    }

    @DELETE
    @Path("/{chargePointId}/connection")
    fun disconnect(@PathParam("chargePointId") chargePointId: String): DisconnectResponse {
        if (chargePointRegistry.getByChargePointId(chargePointId) == null) {
            throw NotFoundException("ChargePoint not connected: $chargePointId")
        }
        chargePointRegistry.disconnect(chargePointId)
        return DisconnectResponse(disconnected = true, chargePointId = chargePointId)
    }

    @POST
    @Path("/reconnect-all")
    fun reconnectAll(): ReconnectAllResponse {
        val count = chargePointRegistry.disconnectAll()
        return ReconnectAllResponse(disconnectedCount = count)
    }

    private fun toDto(cp: ChargePoint): ChargePointDto = ChargePointDto(
        id = cp.id,
        chargePointId = cp.chargePointId,
        vendor = cp.vendor,
        model = cp.model,
        firmwareVersion = cp.firmwareVersion,
        status = effectiveStatus(cp).name,
        sessionId = cp.sessionId,
        lastSeenAt = cp.lastSeenAt.toString(),
        lastConnectedAt = cp.lastConnectedAt.toString(),
        createdAt = cp.createdAt.toString(),
        connectors = persistenceService.findConnectorStatusesByChargePointId(cp.chargePointId).filter { it.connectorId > 0 }
    )

    private fun effectiveStatus(cp: ChargePoint): ChargePointStatus {
        if (cp.status != ChargePointStatus.ONLINE) {
            return cp.status
        }
        val connected = chargePointRegistry.getByChargePointId(cp.chargePointId) != null
        return if (connected) ChargePointStatus.ONLINE else ChargePointStatus.OFFLINE
    }
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
    val lastConnectedAt: String,
    val createdAt: String,
    val connectors: List<ConnectorStatusDto> = emptyList()
)

data class DisconnectResponse(
    val disconnected: Boolean,
    val chargePointId: String
)

data class ReconnectAllResponse(
    val disconnectedCount: Int
)

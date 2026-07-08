package org.tekeli.borisp.ocpp16.rest

import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.NotFoundException
import jakarta.ws.rs.Path
import jakarta.ws.rs.PathParam
import jakarta.ws.rs.Produces
import jakarta.ws.rs.QueryParam
import jakarta.ws.rs.core.MediaType
import org.tekeli.borisp.ocpp16.persistence.OcppMessageLog
import org.tekeli.borisp.ocpp16.protocol.MessageCaptureService
import org.tekeli.borisp.ocpp16.protocol.OcppMessageDto

@Path("/api/chargepoints/{chargePointId}/messages")
@Produces(MediaType.APPLICATION_JSON)
class MessageResource {

    @Inject
    lateinit var messageCaptureService: MessageCaptureService

    @Inject
    lateinit var persistenceService: org.tekeli.borisp.ocpp16.persistence.PersistenceService

    @GET
    fun getMessages(
        @PathParam("chargePointId") chargePointId: String,
        @QueryParam("direction") direction: String? = null,
        @QueryParam("action") action: String? = null,
        @QueryParam("limit") limit: Int = 200,
        @QueryParam("since") since: String? = null
    ): List<OcppMessageDto> {
        validateChargePoint(chargePointId)

        val buffered = messageCaptureService.getMessages(chargePointId)
        val filtered = buffered.filter { msg ->
            (direction == null || direction.isBlank() || msg.direction == direction) &&
            (action == null || action.isBlank() || msg.action == action) &&
            (since == null || since.isBlank() || msg.timestamp >= since)
        }
        return if (limit > 0) filtered.takeLast(limit) else filtered
    }

    @GET
    @Path("/history")
    fun getHistory(
        @PathParam("chargePointId") chargePointId: String,
        @QueryParam("direction") direction: String? = null,
        @QueryParam("action") action: String? = null,
        @QueryParam("limit") limit: Int = 200,
        @QueryParam("offset") offset: Int = 0
    ): Map<String, Any> {
        validateChargePoint(chargePointId)

        val allLogs = persistenceService.findMessageLogs(chargePointId, direction, action, (offset + limit) * 2)
        val page = allLogs.drop(offset).take(limit)
        val dtos = page.map { toDto(it) }

        return mapOf(
            "total" to allLogs.size,
            "offset" to offset,
            "limit" to limit,
            "messages" to dtos
        )
    }

    private fun validateChargePoint(chargePointId: String) {
        if (persistenceService.findChargePointById(chargePointId) == null) {
            throw NotFoundException("ChargePoint not found: $chargePointId")
        }
    }

    private fun toDto(log: OcppMessageLog): OcppMessageDto = OcppMessageDto(
        chargePointId = log.chargePointId,
        direction = log.direction,
        messageType = log.messageType,
        action = log.action.takeIf { it.isNotBlank() },
        messageId = log.messageId,
        payload = log.payload,
        timestamp = log.timestamp.toString()
    )
}

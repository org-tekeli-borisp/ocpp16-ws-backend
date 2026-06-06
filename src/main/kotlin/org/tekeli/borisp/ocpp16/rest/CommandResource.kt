package org.tekeli.borisp.ocpp16.rest

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.persistence.ChargePointStatus
import org.tekeli.borisp.ocpp16.persistence.PersistenceService

@Path("/api/chargepoints/{chargePointId}/commands")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class CommandResource {

    @Inject
    lateinit var persistenceService: PersistenceService

    @Inject
    lateinit var objectMapper: ObjectMapper

    private val validResetTypes = setOf("Hard", "Soft")

    @GET
    fun listCommands(): List<String> {
        return listOf(
            "remote-start-transaction",
            "remote-stop-transaction",
            "reset",
            "unlock-connector"
        )
    }

    @POST
    @Path("/{command}")
    fun executeCommand(
        @PathParam("chargePointId") chargePointId: String,
        @PathParam("command") command: String,
        body: String
    ): Response {
        val cp = persistenceService.findChargePointById(chargePointId)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "ChargePoint not found: $chargePointId"))
                .build()

        return when (command) {
            "remote-start-transaction" -> handleRemoteStartTransaction(chargePointId, body)
            "remote-stop-transaction" -> handleRemoteStopTransaction(chargePointId, body)
            "reset" -> handleReset(chargePointId, body)
            "unlock-connector" -> handleUnlockConnector(chargePointId, body)
            else -> Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "Unknown command: $command"))
                .build()
        }
    }

    private fun handleRemoteStartTransaction(chargePointId: String, body: String): Response {
        val payload = objectMapper.readValue(body, Map::class.java)
        val idTag = payload["idTag"] as String?
        val connectorId = payload["connectorId"] as Int?

        if (idTag.isNullOrEmpty() || connectorId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "idTag and connectorId are required"))
                .build()
        }

        return Response.status(Response.Status.ACCEPTED)
            .entity(mapOf("status" to "accepted", "command" to "remote-start-transaction"))
            .build()
    }

    private fun handleRemoteStopTransaction(chargePointId: String, body: String): Response {
        val payload = objectMapper.readValue(body, Map::class.java)
        val transactionId = (payload["transactionId"] as? Number)?.toLong()

        if (transactionId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "transactionId is required"))
                .build()
        }

        return Response.status(Response.Status.ACCEPTED)
            .entity(mapOf("status" to "accepted", "command" to "remote-stop-transaction"))
            .build()
    }

    private fun handleReset(chargePointId: String, body: String): Response {
        val payload = objectMapper.readValue(body, Map::class.java)
        val type = payload["type"] as String?

        if (type == null || type !in validResetTypes) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "type must be 'Hard' or 'Soft'"))
                .build()
        }

        return Response.status(Response.Status.ACCEPTED)
            .entity(mapOf("status" to "accepted", "command" to "reset", "type" to type))
            .build()
    }

    private fun handleUnlockConnector(chargePointId: String, body: String): Response {
        val payload = objectMapper.readValue(body, Map::class.java)
        val connectorId = payload["connectorId"] as Int?

        if (connectorId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf("error" to "connectorId is required"))
                .build()
        }

        return Response.status(Response.Status.ACCEPTED)
            .entity(mapOf("status" to "accepted", "command" to "unlock-connector"))
            .build()
    }
}

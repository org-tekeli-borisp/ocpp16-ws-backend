package org.tekeli.borisp.ocpp16.rest

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.outbound.OcppOutboundService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage

@Path("/api/chargepoints/{chargePointId}/commands")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class CommandResource {

    @Inject
    lateinit var outboundService: OcppOutboundService

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
                .entity(mapOf<String, Any>("error" to "ChargePoint not found: $chargePointId"))
                .build()

        return try {
            when (command) {
                "remote-start-transaction" -> handleRemoteStartTransaction(chargePointId, body)
                "remote-stop-transaction" -> handleRemoteStopTransaction(chargePointId, body)
                "reset" -> handleReset(chargePointId, body)
                "unlock-connector" -> handleUnlockConnector(chargePointId, body)
                else -> Response.status(Response.Status.NOT_FOUND)
                    .entity(mapOf<String, Any>("error" to "Unknown command: $command"))
                    .build()
            }
        } catch (e: IllegalStateException) {
            Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(mapOf<String, Any>("error" to (e.message ?: "ChargePoint not connected")))
                .build()
        }
    }

    private fun handleRemoteStartTransaction(chargePointId: String, body: String): Response {
        val payload = objectMapper.readValue(body, Map::class.java)
        val idTag = payload["idTag"] as String?
        val connectorId = (payload["connectorId"] as? Number)?.toInt()

        if (idTag.isNullOrEmpty() || connectorId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "idTag and connectorId are required"))
                .build()
        }

        val result = outboundService.sendRemoteStartTransaction(chargePointId, idTag, connectorId)

        val response = result.get(10, java.util.concurrent.TimeUnit.SECONDS)
        return if (response is OcppMessage.CallResult) {
            Response.status(Response.Status.ACCEPTED)
                .entity(mapOf<String, Any>("status" to "sent", "command" to "remote-start-transaction"))
                .build()
        } else {
            Response.status(Response.Status.BAD_GATEWAY)
                .entity(mapOf<String, Any>("status" to "rejected", "error" to "ChargePoint rejected command"))
                .build()
        }
    }

    private fun handleRemoteStopTransaction(chargePointId: String, body: String): Response {
        val payload = objectMapper.readValue(body, Map::class.java)
        val transactionId = (payload["transactionId"] as? Number)?.toInt()

        if (transactionId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "transactionId is required"))
                .build()
        }

        val result = outboundService.sendRemoteStopTransaction(chargePointId, transactionId)

        val response = result.get(10, java.util.concurrent.TimeUnit.SECONDS)
        return if (response is OcppMessage.CallResult) {
            Response.status(Response.Status.ACCEPTED)
                .entity(mapOf<String, Any>("status" to "sent", "command" to "remote-stop-transaction"))
                .build()
        } else {
            Response.status(Response.Status.BAD_GATEWAY)
                .entity(mapOf<String, Any>("status" to "rejected", "error" to "ChargePoint rejected command"))
                .build()
        }
    }

    private fun handleReset(chargePointId: String, body: String): Response {
        val payload = objectMapper.readValue(body, Map::class.java)
        val type = payload["type"] as String?

        if (type == null || type !in validResetTypes) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "type must be 'Hard' or 'Soft'"))
                .build()
        }

        val result = outboundService.sendReset(chargePointId, type)

        val response = result.get(10, java.util.concurrent.TimeUnit.SECONDS)
        return if (response is OcppMessage.CallResult) {
            Response.status(Response.Status.ACCEPTED)
                .entity(mapOf<String, Any>("status" to "sent", "command" to "reset", "type" to type))
                .build()
        } else {
            Response.status(Response.Status.BAD_GATEWAY)
                .entity(mapOf<String, Any>("status" to "rejected", "error" to "ChargePoint rejected command"))
                .build()
        }
    }

    private fun handleUnlockConnector(chargePointId: String, body: String): Response {
        val payload = objectMapper.readValue(body, Map::class.java)
        val connectorId = (payload["connectorId"] as? Number)?.toInt()

        if (connectorId == null) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "connectorId is required"))
                .build()
        }

        val result = outboundService.sendUnlockConnector(chargePointId, connectorId)

        val response = result.get(10, java.util.concurrent.TimeUnit.SECONDS)
        return if (response is OcppMessage.CallResult) {
            Response.status(Response.Status.ACCEPTED)
                .entity(mapOf<String, Any>("status" to "sent", "command" to "unlock-connector"))
                .build()
        } else {
            Response.status(Response.Status.BAD_GATEWAY)
                .entity(mapOf<String, Any>("status" to "rejected", "error" to "ChargePoint rejected command"))
                .build()
        }
    }
}

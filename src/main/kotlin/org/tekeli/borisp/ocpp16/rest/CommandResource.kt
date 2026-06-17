package org.tekeli.borisp.ocpp16.rest

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.command.OcppCommand
import org.tekeli.borisp.ocpp16.persistence.PersistenceService

@Path("/api/chargepoints/{chargePointId}/commands")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class CommandResource {

    @Inject
    lateinit var persistenceService: PersistenceService

    @Inject
    lateinit var objectMapper: ObjectMapper

    @Inject
    lateinit var commands: jakarta.enterprise.inject.Instance<OcppCommand>

    private val commandMap: Map<String, OcppCommand> by lazy {
        commands.iterator().asSequence().associateBy { it.name }
    }

    @GET
    fun listCommands(): List<String> {
        return commandMap.keys.sorted()
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

        val cmd = commandMap[command]
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf<String, Any>("error" to "Unknown command: $command"))
                .build()

        val payload = objectMapper.readValue(body, Map::class.java) as Map<String, Any>

        val validationError = cmd.validate(payload)
        if (validationError != null) return validationError

        return try {
            cmd.execute(chargePointId, payload)
        } catch (e: IllegalStateException) {
            Response.status(Response.Status.SERVICE_UNAVAILABLE)
                .entity(mapOf<String, Any>("error" to (e.message ?: "ChargePoint not connected")))
                .build()
        }
    }
}

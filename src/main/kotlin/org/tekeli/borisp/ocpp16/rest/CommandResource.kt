package org.tekeli.borisp.ocpp16.rest

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.command.OcppCommand
import org.tekeli.borisp.ocpp16.command.PayloadValidators
import org.tekeli.borisp.ocpp16.diagnostics.DiagnosticsUrlGenerator
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.SchemaValidator

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

    @Inject
    lateinit var schemaValidator: SchemaValidator

    @Inject
    lateinit var urlGeneratorInstance: Instance<DiagnosticsUrlGenerator>

    private val commandMap: Map<String, OcppCommand> by lazy {
        commands.iterator().asSequence().associateBy { it.name }
    }

    private fun toActionName(commandName: String): String {
        return commandName.split("-").joinToString("") { it.capitalize() }
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

        val payload = PayloadValidators.safeMap(objectMapper.readValue(body, Map::class.java))
        val (validatedPayload, validatedBody) = resolveDiagnosticsUrl(chargePointId, command, payload, body)

        val actionName = toActionName(command)
        val schemaErrors = schemaValidator.validate(actionName, validatedBody)
        if (schemaErrors.isNotEmpty()) {
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "Schema validation failed", "details" to schemaErrors))
                .build()
        }

        val validationError = cmd.validate(validatedPayload)
        if (validationError != null) return validationError

        return try {
            cmd.execute(chargePointId, validatedPayload)
        } catch (e: java.util.concurrent.ExecutionException) {
            handleExecutionException(e)
        } catch (e: IllegalStateException) {
            buildUnavailableResponse(e.message)
        }
    }

    private fun handleExecutionException(e: java.util.concurrent.ExecutionException): Response {
        return when (val cause = e.cause) {
            is IllegalStateException -> buildUnavailableResponse(cause.message)
            else -> throw e
        }
    }

    private fun buildUnavailableResponse(message: String?): Response {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
            .entity(mapOf<String, Any>("error" to (message ?: "ChargePoint not connected")))
            .build()
    }

    private fun resolveDiagnosticsUrl(
        chargePointId: String,
        command: String,
        payload: Map<String, Any>,
        body: String
    ): Pair<Map<String, Any>, String> {
        if (command != "get-diagnostics") return payload to body
        val existingLocation = payload["location"] as String?
        if (existingLocation != null && existingLocation.isNotBlank()) return payload to body

        val generator = resolveUrlGenerator()
            ?: return payload to body

        return try {
            val generatedUrl = generator.generate(chargePointId)
            val updatedPayload = payload.toMutableMap()
            updatedPayload["location"] = generatedUrl
            val updatedBody = objectMapper.writeValueAsString(updatedPayload)
            updatedPayload to updatedBody
        } catch (e: Exception) {
            payload to body
        }
    }

    private fun resolveUrlGenerator(): DiagnosticsUrlGenerator? {
        if (urlGeneratorInstance.isUnsatisfied || urlGeneratorInstance.isAmbiguous) return null
        return try { urlGeneratorInstance.get() } catch (e: Exception) { null }
    }
}

package org.tekeli.borisp.ocpp16.rest

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.command.*
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
    lateinit var remoteStartTransactionCommand: RemoteStartTransactionCommand

    @Inject
    lateinit var remoteStopTransactionCommand: RemoteStopTransactionCommand

    @Inject
    lateinit var resetCommand: ResetCommand

    @Inject
    lateinit var unlockConnectorCommand: UnlockConnectorCommand

    @Inject
    lateinit var cancelReservationCommand: CancelReservationCommand

    @Inject
    lateinit var changeAvailabilityCommand: ChangeAvailabilityCommand

    @Inject
    lateinit var changeConfigurationCommand: ChangeConfigurationCommand

    @Inject
    lateinit var clearCacheCommand: ClearCacheCommand

    @Inject
    lateinit var clearChargingProfileCommand: ClearChargingProfileCommand

    @Inject
    lateinit var getCompositeScheduleCommand: GetCompositeScheduleCommand

    @Inject
    lateinit var getConfigurationCommand: GetConfigurationCommand

    @Inject
    lateinit var getDiagnosticsCommand: GetDiagnosticsCommand

    @Inject
    lateinit var getLocalListVersionCommand: GetLocalListVersionCommand

    @Inject
    lateinit var reserveNowCommand: ReserveNowCommand

    @Inject
    lateinit var sendLocalListCommand: SendLocalListCommand

    @Inject
    lateinit var setChargingProfileCommand: SetChargingProfileCommand

    @Inject
    lateinit var triggerMessageCommand: TriggerMessageCommand

    @Inject
    lateinit var updateFirmwareCommand: UpdateFirmwareCommand

    @Inject
    lateinit var extendedTriggerMessageCommand: ExtendedTriggerMessageCommand

    @Inject
    lateinit var installCertificateCommand: InstallCertificateCommand

    @Inject
    lateinit var getInstalledCertificateIdsCommand: GetInstalledCertificateIdsCommand

    @Inject
    lateinit var deleteCertificateCommand: DeleteCertificateCommand

    @Inject
    lateinit var getLogCommand: GetLogCommand

    @Inject
    lateinit var signedUpdateFirmwareCommand: SignedUpdateFirmwareCommand

    private val commandMap: Map<String, OcppCommand> by lazy {
        mapOf(
            remoteStartTransactionCommand.name to remoteStartTransactionCommand,
            remoteStopTransactionCommand.name to remoteStopTransactionCommand,
            resetCommand.name to resetCommand,
            unlockConnectorCommand.name to unlockConnectorCommand,
            cancelReservationCommand.name to cancelReservationCommand,
            changeAvailabilityCommand.name to changeAvailabilityCommand,
            changeConfigurationCommand.name to changeConfigurationCommand,
            clearCacheCommand.name to clearCacheCommand,
            clearChargingProfileCommand.name to clearChargingProfileCommand,
            getCompositeScheduleCommand.name to getCompositeScheduleCommand,
            getConfigurationCommand.name to getConfigurationCommand,
            getDiagnosticsCommand.name to getDiagnosticsCommand,
            getLocalListVersionCommand.name to getLocalListVersionCommand,
            reserveNowCommand.name to reserveNowCommand,
            sendLocalListCommand.name to sendLocalListCommand,
            setChargingProfileCommand.name to setChargingProfileCommand,
            triggerMessageCommand.name to triggerMessageCommand,
            updateFirmwareCommand.name to updateFirmwareCommand,
            extendedTriggerMessageCommand.name to extendedTriggerMessageCommand,
            installCertificateCommand.name to installCertificateCommand,
            getInstalledCertificateIdsCommand.name to getInstalledCertificateIdsCommand,
            deleteCertificateCommand.name to deleteCertificateCommand,
            getLogCommand.name to getLogCommand,
            signedUpdateFirmwareCommand.name to signedUpdateFirmwareCommand
        )
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

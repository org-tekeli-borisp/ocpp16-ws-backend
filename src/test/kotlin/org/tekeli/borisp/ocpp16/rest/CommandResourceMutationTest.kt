package org.tekeli.borisp.ocpp16.rest

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.enterprise.inject.Instance
import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.*
import org.tekeli.borisp.ocpp16.command.OcppCommand
import org.tekeli.borisp.ocpp16.diagnostics.DiagnosticsConfig
import org.tekeli.borisp.ocpp16.diagnostics.DiagnosticsInitializer
import org.tekeli.borisp.ocpp16.diagnostics.FtpServerConfig
import org.tekeli.borisp.ocpp16.diagnostics.SftpServerConfig
import org.tekeli.borisp.ocpp16.persistence.ChargePoint
import org.tekeli.borisp.ocpp16.persistence.ChargePointStatus
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.SchemaValidator
import java.util.concurrent.ExecutionException

private const val EMPTY_BODY = "{}"
private const val SFTP_PROTOCOL_BODY = """{"protocol": "sftp"}"""
private const val BOGUS_PROTOCOL_BODY = """{"protocol": "bogus"}"""

class CommandResourceMutationTest {

    private lateinit var persistence: PersistenceService
    private lateinit var commands: Instance<OcppCommand>
    private lateinit var schemaValidator: SchemaValidator
    private lateinit var sftpConfig: SftpServerConfig
    private lateinit var ftpConfig: FtpServerConfig
    private lateinit var diagnosticsConfig: DiagnosticsConfig
    private lateinit var command: OcppCommand
    private lateinit var resource: CommandResource

    @BeforeEach
    fun setup() {
        persistence = mock(PersistenceService::class.java)
        commands = mock(Instance::class.java) as Instance<OcppCommand>
        schemaValidator = mock(SchemaValidator::class.java)
        sftpConfig = mock(SftpServerConfig::class.java)
        ftpConfig = mock(FtpServerConfig::class.java)
        diagnosticsConfig = mock(DiagnosticsConfig::class.java)
        command = mock(OcppCommand::class.java)

        resource = CommandResource()
        resource.persistenceService = persistence
        resource.objectMapper = ObjectMapper()
        resource.commands = commands
        resource.schemaValidator = schemaValidator
        resource.diagnosticsConfig = diagnosticsConfig
        resource.sftpServerConfig = sftpConfig
        resource.ftpServerConfig = ftpConfig

        `when`(persistence.findChargePointById("CP-001")).thenReturn(chargePoint("CP-001"))
        `when`(schemaValidator.validate(anyString(), anyString())).thenReturn(emptyList())
        `when`(command.validate(anyMap())).thenReturn(null)
    }

    private fun chargePoint(chargePointId: String) = ChargePoint(
        chargePointId = chargePointId,
        vendor = "Vendor-$chargePointId",
        model = "Model-$chargePointId",
        firmwareVersion = "fw-$chargePointId",
        status = ChargePointStatus.ONLINE,
        sessionId = "session-$chargePointId"
    )

    private fun wireCommand(name: String) {
        `when`(command.name).thenReturn(name)
        `when`(commands.iterator()).thenReturn(listOf(command).toMutableList().iterator())
    }

    private fun accepted() = Response.status(Response.Status.ACCEPTED).build()

    @Test
    fun `diagnosticsInitializer throws when not injected`() {
        val bare = CommandResource()

        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            bare.diagnosticsInitializer
        }
    }

    @Test
    fun `diagnosticsInitializer returns the injected instance`() {
        val initializer = mock(DiagnosticsInitializer::class.java)

        resource.diagnosticsInitializer = initializer

        assertSame(initializer, resource.diagnosticsInitializer)
    }

    @Test
    fun `executeCommand returns 503 with cause message when execute throws ExecutionException wrapping IllegalStateException`() {
        wireCommand("reset")
        `when`(command.execute("CP-001", emptyMap<String, Any>()))
            .thenAnswer { throw ExecutionException(IllegalStateException("connection dropped")) }

        val response = resource.executeCommand("CP-001", "reset", EMPTY_BODY)

        assertEquals(503, response.status)
        assertEquals("connection dropped", (response.entity as Map<*, *>)["error"])
    }

    @Test
    fun `executeCommand rethrows ExecutionException when cause is not IllegalStateException`() {
        wireCommand("reset")
        `when`(command.execute("CP-001", emptyMap<String, Any>()))
            .thenAnswer { throw ExecutionException(RuntimeException("boom")) }

        assertThrows(ExecutionException::class.java) {
            resource.executeCommand("CP-001", "reset", EMPTY_BODY)
        }
    }

    @Test
    fun `getDiagnostics with protocol generates protocol specific location`() {
        wireCommand("get-diagnostics")
        `when`(sftpConfig.enabled()).thenReturn(true)
        `when`(sftpConfig.username()).thenReturn("ocpp")
        `when`(sftpConfig.password()).thenReturn("ocpp")
        `when`(sftpConfig.port()).thenReturn(2022)
        `when`(diagnosticsConfig.publicHost()).thenReturn("127.0.0.1")
        val expectedLocation = "sftp://ocpp:ocpp@127.0.0.1:2022/CP-001"
        `when`(command.execute("CP-001", mapOf("location" to expectedLocation))).thenReturn(accepted())

        val response = resource.executeCommand("CP-001", "get-diagnostics", SFTP_PROTOCOL_BODY)

        assertEquals(202, response.status)
        verify(command).execute("CP-001", mapOf("location" to expectedLocation))
    }

    @Test
    fun `getDiagnostics with unknown protocol falls back to original payload`() {
        wireCommand("get-diagnostics")
        `when`(command.execute("CP-001", mapOf("protocol" to "bogus"))).thenReturn(accepted())

        val response = resource.executeCommand("CP-001", "get-diagnostics", BOGUS_PROTOCOL_BODY)

        assertEquals(202, response.status)
        verify(command).execute("CP-001", mapOf("protocol" to "bogus"))
        verify(schemaValidator).validate("GetDiagnostics", BOGUS_PROTOCOL_BODY)
    }
}

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
import org.tekeli.borisp.ocpp16.diagnostics.FtpServerConfig
import org.tekeli.borisp.ocpp16.diagnostics.SftpServerConfig
import org.tekeli.borisp.ocpp16.persistence.ChargePoint
import org.tekeli.borisp.ocpp16.persistence.ChargePointStatus
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.SchemaValidator

private const val BLANK_LOCATION_BODY = """{"protocol": "sftp", "location": ""}"""
private const val EXPLICIT_LOCATION_BODY = """{"protocol": "sftp", "location": "http://x/y"}"""

class CommandResourceErrorPathsTest {

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

        `when`(persistence.findChargePointById("CP-001")).thenReturn(
            ChargePoint(
                chargePointId = "CP-001",
                vendor = "Vendor",
                model = "Model",
                firmwareVersion = "fw",
                status = ChargePointStatus.ONLINE,
                sessionId = "session-1"
            )
        )
        `when`(schemaValidator.validate(anyString(), anyString())).thenReturn(emptyList())
        `when`(command.validate(anyMap())).thenReturn(null)
    }

    private fun wireCommand(name: String) {
        `when`(command.name).thenReturn(name)
        `when`(commands.iterator()).thenReturn(listOf(command).toMutableList().iterator())
    }

    private fun accepted() = Response.status(Response.Status.ACCEPTED).build()

    @Test
    fun `executeCommand returns validation error response from command validate`() {
        wireCommand("reset")
        val validationError = Response.status(Response.Status.BAD_REQUEST)
            .entity(mapOf<String, Any>("error" to "manual validation failed"))
            .build()
        `when`(command.validate(emptyMap<String, Any>())).thenReturn(validationError)

        val response = resource.executeCommand("CP-001", "reset", "{}")

        assertSame(validationError, response)
        assertEquals(400, response.status)
        verify(command, never()).execute(anyString(), anyMap())
    }

    @Test
    fun `executeCommand returns 503 with message when execute throws IllegalStateException`() {
        wireCommand("reset")
        `when`(command.execute("CP-001", emptyMap<String, Any>()))
            .thenAnswer { throw IllegalStateException("CP gone") }

        val response = resource.executeCommand("CP-001", "reset", "{}")

        assertEquals(503, response.status)
        assertEquals("CP gone", (response.entity as Map<*, *>)["error"])
    }

    @Test
    fun `executeCommand returns 503 with default message when IllegalStateException has null message`() {
        wireCommand("reset")
        `when`(command.execute("CP-001", emptyMap<String, Any>()))
            .thenAnswer { throw IllegalStateException() }

        val response = resource.executeCommand("CP-001", "reset", "{}")

        assertEquals(503, response.status)
        assertEquals("ChargePoint not connected", (response.entity as Map<*, *>)["error"])
    }

    @Test
    fun `getDiagnostics with blank location generates new location`() {
        wireCommand("get-diagnostics")
        `when`(sftpConfig.enabled()).thenReturn(true)
        `when`(sftpConfig.username()).thenReturn("ocpp")
        `when`(sftpConfig.password()).thenReturn("ocpp")
        `when`(sftpConfig.port()).thenReturn(2022)
        `when`(diagnosticsConfig.publicHost()).thenReturn("127.0.0.1")
        val expectedLocation = "sftp://ocpp:ocpp@127.0.0.1:2022/CP-001"
        `when`(command.execute("CP-001", mapOf("location" to expectedLocation))).thenReturn(accepted())

        val response = resource.executeCommand("CP-001", "get-diagnostics", BLANK_LOCATION_BODY)

        assertEquals(202, response.status)
        verify(command).execute("CP-001", mapOf("location" to expectedLocation))
    }

    @Test
    fun `getDiagnostics with explicit location returns payload with protocol stripped`() {
        wireCommand("get-diagnostics")
        `when`(command.execute("CP-001", mapOf("location" to "http://x/y"))).thenReturn(accepted())

        val response = resource.executeCommand("CP-001", "get-diagnostics", EXPLICIT_LOCATION_BODY)

        assertEquals(202, response.status)
        verify(command).execute("CP-001", mapOf("location" to "http://x/y"))
        verify(schemaValidator).validate("GetDiagnostics", """{"location":"http://x/y"}""")
    }
}

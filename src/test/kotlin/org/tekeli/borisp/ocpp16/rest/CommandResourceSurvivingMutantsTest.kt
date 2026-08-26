package org.tekeli.borisp.ocpp16.rest

import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.tekeli.borisp.ocpp16.diagnostics.DiagnosticsConfig
import org.tekeli.borisp.ocpp16.diagnostics.FtpServerConfig

class CommandResourceSurvivingMutantsTest {

    @Test
    fun `diagnosticsConfig returns the assigned instance`() {
        val config = mock(DiagnosticsConfig::class.java)
        val resource = CommandResource()
        resource.diagnosticsConfig = config

        assertSame(config, resource.diagnosticsConfig)
    }

    @Test
    fun `ftpServerConfig returns the assigned instance`() {
        val config = mock(FtpServerConfig::class.java)
        val resource = CommandResource()
        resource.ftpServerConfig = config

        assertSame(config, resource.ftpServerConfig)
    }

    @Test
    fun `diagnosticsConfig throws when not initialized`() {
        val resource = CommandResource()

        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            resource.diagnosticsConfig
        }
    }

    @Test
    fun `ftpServerConfig throws when not initialized`() {
        val resource = CommandResource()

        assertThrows(kotlin.UninitializedPropertyAccessException::class.java) {
            resource.ftpServerConfig
        }
    }
}

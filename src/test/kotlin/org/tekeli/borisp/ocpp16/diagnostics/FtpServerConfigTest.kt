package org.tekeli.borisp.ocpp16.diagnostics

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class FtpServerConfigTest {

    @Inject
    lateinit var ftpServerConfig: FtpServerConfig

    @Test
    fun `should be enabled by default`() {
        assertTrue(ftpServerConfig.enabled())
    }

    @Test
    fun `should have default port 2021`() {
        assertEquals(2021, ftpServerConfig.port())
    }

    @Test
    fun `should have default host all interfaces`() {
        assertEquals("0.0.0.0", ftpServerConfig.host())
    }

    @Test
    fun `should have default username`() {
        assertEquals("ocpp", ftpServerConfig.username())
    }

    @Test
    fun `should have default password`() {
        assertEquals("ocpp", ftpServerConfig.password())
    }
}

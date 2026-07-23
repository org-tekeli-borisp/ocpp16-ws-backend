package org.tekeli.borisp.ocpp16.diagnostics

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class SftpServerConfigTest {

    @Inject
    lateinit var sftpServerConfig: SftpServerConfig

    @Test
    fun `should be enabled by default`() {
        assertTrue(sftpServerConfig.enabled())
    }

    @Test
    fun `should have default port 2022`() {
        assertEquals(2022, sftpServerConfig.port())
    }

    @Test
    fun `should have default host all interfaces`() {
        assertEquals("0.0.0.0", sftpServerConfig.host())
    }

    @Test
    fun `should have default username`() {
        assertEquals("ocpp", sftpServerConfig.username())
    }

    @Test
    fun `should have default password`() {
        assertEquals("ocpp", sftpServerConfig.password())
    }
}

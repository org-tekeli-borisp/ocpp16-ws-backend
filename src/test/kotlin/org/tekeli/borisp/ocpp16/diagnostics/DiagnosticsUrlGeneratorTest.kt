package org.tekeli.borisp.ocpp16.diagnostics

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class DiagnosticsUrlGeneratorTest {

    private val sftpConfig = object : SftpServerConfig {
        override fun enabled() = true
        override fun port() = 2022
        override fun host() = "127.0.0.1"
        override fun username() = "ocpp"
        override fun password() = "testpass"
    }

    private val ftpConfig = object : FtpServerConfig {
        override fun enabled() = true
        override fun port() = 2021
        override fun host() = "127.0.0.1"
        override fun username() = "ocpp"
        override fun password() = "testpass"
        override fun passivePorts() = "30000-30100"
        override fun externalAddress() = ""
    }

    private val diagnosticsConfig = object : DiagnosticsConfig {
        override fun uploadDir() = "/tmp/ocpp-diagnostics"
        override fun maxFileSizeBytes() = 104857600L
        override fun retentionDays() = 30
        override fun preferredProtocol() = "sftp"
        override fun publicHost() = "127.0.0.1"
    }

    private val diagnosticsFtpConfig = object : DiagnosticsConfig {
        override fun uploadDir() = "/tmp/ocpp-diagnostics"
        override fun maxFileSizeBytes() = 104857600L
        override fun retentionDays() = 30
        override fun preferredProtocol() = "ftp"
        override fun publicHost() = "127.0.0.1"
    }

    @Test
    fun `should generate SFTP URL when protocol is sftp`() {
        val generator = DiagnosticsUrlGenerator(sftpConfig, ftpConfig, diagnosticsConfig)
        val url = generator.generate("CP-001")
        assertEquals("sftp://ocpp:testpass@127.0.0.1:2022/CP-001", url)
    }

    @Test
    fun `should generate FTP URL when protocol is ftp`() {
        val generator = DiagnosticsUrlGenerator(sftpConfig, ftpConfig, diagnosticsFtpConfig)
        val url = generator.generate("CP-001")
        assertEquals("ftp://ocpp:testpass@127.0.0.1:2021/CP-001", url)
    }

    @Test
    fun `should include username and password in URL`() {
        val generator = DiagnosticsUrlGenerator(sftpConfig, ftpConfig, diagnosticsConfig)
        val url = generator.generate("Tesla-Model3")
        assertTrue(url.contains("ocpp:testpass@"))
    }

    @Test
    fun `should include chargePointId in path`() {
        val generator = DiagnosticsUrlGenerator(sftpConfig, ftpConfig, diagnosticsConfig)
        val url = generator.generate("SNH764")
        assertTrue(url.endsWith("/SNH764"))
    }

    @Test
    fun `should use publicHost from diagnostics config`() {
        val configWithPublicHost = object : DiagnosticsConfig {
            override fun uploadDir() = "/tmp/ocpp-diagnostics"
            override fun maxFileSizeBytes() = 104857600L
            override fun retentionDays() = 30
            override fun preferredProtocol() = "sftp"
            override fun publicHost() = "cs.example.com"
        }
        val generator = DiagnosticsUrlGenerator(sftpConfig, ftpConfig, configWithPublicHost)
        val url = generator.generate("CP-001")
        assertEquals("sftp://ocpp:testpass@cs.example.com:2022/CP-001", url)
    }

    @Test
    fun `should use default publicHost localhost`() {
        val generator = DiagnosticsUrlGenerator(sftpConfig, ftpConfig, diagnosticsConfig)
        val url = generator.generate("CP-001")
        assertEquals("sftp://ocpp:testpass@127.0.0.1:2022/CP-001", url)
    }

    @Test
    fun `should throw when SFTP requested but disabled`() {
        val disabledSftp = object : SftpServerConfig {
            override fun enabled() = false
            override fun port() = 2022
            override fun host() = "127.0.0.1"
            override fun username() = "ocpp"
            override fun password() = "testpass"
        }
        val generator = DiagnosticsUrlGenerator(disabledSftp, ftpConfig, diagnosticsConfig)
        assertThrows(IllegalStateException::class.java) {
            generator.generate("CP-001")
        }
    }

    @Test
    fun `should throw when FTP requested but disabled`() {
        val disabledFtp = object : FtpServerConfig {
            override fun enabled() = false
            override fun port() = 2021
            override fun host() = "127.0.0.1"
            override fun username() = "ocpp"
            override fun password() = "ocpp"
            override fun passivePorts() = "30000-30100"
            override fun externalAddress() = ""
        }
        val generator = DiagnosticsUrlGenerator(sftpConfig, disabledFtp, diagnosticsFtpConfig)
        assertThrows(IllegalStateException::class.java) {
            generator.generate("CP-001")
        }
    }

    @Test
    fun `should throw when both servers disabled`() {
        val disabledSftp = object : SftpServerConfig {
            override fun enabled() = false
            override fun port() = 2022
            override fun host() = "127.0.0.1"
            override fun username() = "ocpp"
            override fun password() = "testpass"
        }
        val disabledFtp = object : FtpServerConfig {
            override fun enabled() = false
            override fun port() = 2021
            override fun host() = "127.0.0.1"
            override fun username() = "ocpp"
            override fun password() = "ocpp"
            override fun passivePorts() = "30000-30100"
            override fun externalAddress() = ""
        }
        val generator = DiagnosticsUrlGenerator(disabledSftp, disabledFtp, diagnosticsConfig)
        assertThrows(IllegalStateException::class.java) {
            generator.generate("CP-001")
        }
    }
}

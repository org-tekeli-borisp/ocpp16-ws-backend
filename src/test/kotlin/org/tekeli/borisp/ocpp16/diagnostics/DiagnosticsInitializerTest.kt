package org.tekeli.borisp.ocpp16.diagnostics

import io.quarkus.runtime.StartupEvent
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class DiagnosticsInitializerTest {

    @TempDir
    lateinit var tempDir: Path

    private val sftpConfig = object : SftpServerConfig {
        override fun enabled() = false
        override fun port() = 22024
        override fun host() = "127.0.0.1"
        override fun username() = "ocpp"
        override fun password() = "testpass"
    }

    private val ftpConfig = object : FtpServerConfig {
        override fun enabled() = false
        override fun port() = 22025
        override fun host() = "127.0.0.1"
        override fun username() = "ocpp"
        override fun password() = "testpass"
        override fun passivePorts() = "30000-30100"
        override fun externalAddress() = ""
    }

    private fun initializer(retentionDays: Int) = DiagnosticsInitializer().apply {
        diagnosticsConfig = object : DiagnosticsConfig {
            override fun uploadDir() = tempDir.toString()
            override fun maxFileSizeBytes() = 1024L
            override fun retentionDays() = retentionDays
            override fun preferredProtocol() = "sftp"
            override fun publicHost() = "127.0.0.1"
        }
        sftpServerConfig = sftpConfig
        ftpServerConfig = ftpConfig
    }

    @Test
    fun `produceStorage throws before initialization`() {
        assertThrows(IllegalStateException::class.java) {
            initializer(30).produceStorage()
        }
    }

    @Test
    fun `onStart initializes storage with configured base dir`() {
        val initializer = initializer(30)
        initializer.onStart(StartupEvent())
        try {
            assertEquals(tempDir.toString(), initializer.produceStorage().baseDir())
        } finally {
            initializer.stop()
        }
    }

    @Test
    fun `stop is idempotent when never started`() {
        val initializer = initializer(30)
        assertDoesNotThrow { initializer.stop() }
    }
}

package org.tekeli.borisp.ocpp16.diagnostics

import org.apache.ftpserver.FtpServer
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import java.nio.file.Path
import java.util.logging.Handler
import java.util.logging.LogRecord

class FtpServerServiceSurvivingMutantsTest {

    @TempDir
    lateinit var tempDir: Path

    @Test
    fun `stop swallows FtpServer stop failures and logs the exception message`() {
        val captured = mutableListOf<String>()
        val handler = object : Handler() {
            override fun publish(record: LogRecord) {
                record.message?.let { captured.add(it) }
            }

            override fun flush() {}
            override fun close() {}
        }
        val rootLogger = java.util.logging.Logger.getLogger("")
        rootLogger.addHandler(handler)
        try {
            val failingServer = mock(FtpServer::class.java)
            doThrow(RuntimeException("ftp-stop-boom")).`when`(failingServer).stop()

            val field = FtpServerService::class.java.getDeclaredField("server")
            field.isAccessible = true
            val config = object : FtpServerConfig {
                override fun enabled() = false
                override fun port() = 0
                override fun host() = "127.0.0.1"
                override fun username() = "ocpp"
                override fun password() = "testpass"
                override fun passivePorts() = "0-0"
                override fun externalAddress() = ""
            }
            val service = FtpServerService(config, FileSystemStorage(tempDir.toString(), 1024))
            field.set(service, failingServer)

            assertDoesNotThrow { service.stop() }
            assertTrue(
                captured.any { it.contains("ftp-stop-boom") },
                "stop error log must contain the exception message, got: $captured"
            )
        } finally {
            rootLogger.removeHandler(handler)
        }
    }
}

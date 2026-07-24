package org.tekeli.borisp.ocpp16.diagnostics

import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import org.apache.commons.net.ftp.FTP

class FtpServerServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var ftpConfig: FtpServerConfig
    private lateinit var storage: FileSystemStorage
    private lateinit var service: FtpServerService

    @BeforeEach
    fun setup() {
        ftpConfig = object : FtpServerConfig {
            override fun enabled() = true
            override fun port() = 20221
            override fun host() = "127.0.0.1"
            override fun username() = "ocpp"
            override fun password() = "testpass"
            override fun passivePorts() = "30000-30100"
            override fun externalAddress() = "127.0.0.1"
        }
        storage = FileSystemStorage(tempDir.toString(), 10 * 1024 * 1024L)
        service = FtpServerService(ftpConfig, storage)
        service.start()
        Thread.sleep(500)
    }

    @AfterEach
    fun teardown() {
        service.stop()
    }

    @Test
    fun `should start and accept FTP connections`() {
        val client = org.apache.commons.net.ftp.FTPClient()
        try {
            client.connect("127.0.0.1", 20221)
            assertTrue(client.isConnected)
            assertTrue(client.login("ocpp", "testpass"))
            assertEquals(230, client.replyCode)
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun `should reject wrong password`() {
        val client = org.apache.commons.net.ftp.FTPClient()
        try {
            client.connect("127.0.0.1", 20221)
            assertFalse(client.login("ocpp", "wrong"))
        } finally {
            client.disconnect()
        }
    }

    @Test
    fun `should upload file via FTP`() {
        storage.ensureDirectory("CP-003")
        val client = org.apache.commons.net.ftp.FTPClient()
        try {
            client.connect("127.0.0.1", 20221)
            client.login("ocpp", "testpass")
            client.enterLocalPassiveMode()
            client.setFileType(FTP.BINARY_FILE_TYPE)

            val content = "ftp diagnostic data"

            val input = java.io.ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
            assertTrue(client.storeFile("CP-003/ftp-test.log", input))
            input.close()

            val targetFile = tempDir.resolve("CP-003/ftp-test.log").toFile()
            assertTrue(targetFile.exists())
        } finally {
            client.disconnect()
        }
    }
}

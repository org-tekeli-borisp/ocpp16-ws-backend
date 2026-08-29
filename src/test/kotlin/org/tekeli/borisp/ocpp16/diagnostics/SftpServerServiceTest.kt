package org.tekeli.borisp.ocpp16.diagnostics

import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import com.jcraft.jsch.Session
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

class SftpServerServiceTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var sftpConfig: SftpServerConfig
    private lateinit var storage: FileSystemStorage
    private lateinit var service: SftpServerService
    private lateinit var session: Session

    @BeforeEach
    fun setup() {
        sftpConfig = object : SftpServerConfig {
            override fun enabled() = true
            override fun port() = 22022
            override fun host() = "127.0.0.1"
            override fun username() = "ocpp"
            override fun password() = "testpass"
        }
        storage = FileSystemStorage(tempDir.toString(), 10 * 1024 * 1024L)
        service = SftpServerService(sftpConfig, storage)
        service.start()
        Thread.sleep(500)

        val jsch = JSch()
        session = jsch.getSession("ocpp", "127.0.0.1", 22022)
        session.setPassword("testpass")
        session.setConfig("StrictHostKeyChecking", "no")
        session.connect(5000)
    }

    @AfterEach
    fun teardown() {
        try { session.disconnect() } catch (_: Exception) {}
        service.stop()
    }

    @Test
    fun `should start and accept connections`() {
        assertTrue(session.isConnected)
    }

    @Test
    fun `should use nio2 io service factory`() {
        val factoryName = service.ioServiceFactoryFactory?.javaClass?.name
        assertTrue(factoryName?.startsWith("org.apache.sshd.common.io.nio2") == true, "expected Nio2 factory but was $factoryName")
    }

    @Test
    fun `should reject wrong password`() {
        val jsch = JSch()
        val badSession = jsch.getSession("ocpp", "127.0.0.1", 22022)
        badSession.setPassword("wrong")
        badSession.setConfig("StrictHostKeyChecking", "no")
        assertThrows(Exception::class.java) {
            badSession.connect(2000)
        }
    }

    @Test
    fun `should not start server when disabled`() {
        val disabledConfig = object : SftpServerConfig {
            override fun enabled() = false
            override fun port() = 22023
            override fun host() = "127.0.0.1"
            override fun username() = "ocpp"
            override fun password() = "testpass"
        }
        val disabledService = SftpServerService(disabledConfig, storage)

        disabledService.start()

        assertTrue(disabledService.ioServiceFactoryFactory == null, "server should not be started when disabled")
    }

    @Test
    fun `should reject shell channel because no shell is provided`() {
        val channel = session.openChannel("shell")
        var connectFailed = false
        try {
            channel.connect(3000)
        } catch (_: Exception) {
            connectFailed = true
        }
        val deadline = System.currentTimeMillis() + 5000
        while (!channel.isClosed && System.currentTimeMillis() < deadline) {
            Thread.sleep(50)
        }
        assertTrue(connectFailed || channel.isClosed, "shell channel should be rejected")
        channel.disconnect()
    }

    @Test
    fun `should upload file and verify via storage API`() {
        storage.ensureDirectory("CP-001")
        val channel = session.openChannel("sftp") as ChannelSftp
        channel.connect(5000)
        try {
            val content = "diagnostic data"
            val targetPath = tempDir.resolve("CP-001/test.log").toString()
            channel.put(ByteArrayInputStream(content.toByteArray(Charsets.UTF_8)), targetPath)

            val file = tempDir.resolve("CP-001/test.log")
            assertTrue(file.exists())
            assertEquals(content, file.readText())
        } finally {
            channel.disconnect()
        }
    }
}

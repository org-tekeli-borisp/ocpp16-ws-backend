package org.tekeli.borisp.ocpp16.diagnostics

import com.jcraft.jsch.JSch
import org.apache.sshd.server.SshServer
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.Mockito.doThrow
import org.mockito.Mockito.mock
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

class SftpServerServiceSurvivingMutantsTest {

    @TempDir
    lateinit var tempDir: Path

    private val sftpPort = 22042

    private fun config() = object : SftpServerConfig {
        override fun enabled() = true
        override fun port() = sftpPort
        override fun host() = "127.0.0.1"
        override fun username() = "ocpp"
        override fun password() = "testpass"
    }

    private fun service() = SftpServerService(config(), FileSystemStorage(tempDir.toString(), 10 * 1024 * 1024L))

    private fun attemptConnect(username: String, password: String): Boolean {
        val jsch = JSch()
        val session = jsch.getSession(username, "127.0.0.1", sftpPort)
        session.setPassword(password)
        session.setConfig("StrictHostKeyChecking", "no")
        return try {
            session.connect(5000)
            session.isConnected.also { session.disconnect() }
        } catch (e: Exception) {
            false
        }
    }

    private fun waitUntilServerUp() {
        var up = false
        var attempts = 0
        while (!up && attempts < 40) {
            up = attemptConnect("ocpp", "testpass")
            if (!up) Thread.sleep(250)
            attempts++
        }
        assertTrue(up, "SFTP server did not come up on port $sftpPort")
    }

    @Test
    fun `stop should swallow sshd stop exception and log its message`() {
        val svc = service()
        val sshd = mock(SshServer::class.java)
        doThrow(RuntimeException("sftp-stop-boom")).`when`(sshd).stop(false)
        val field = SftpServerService::class.java.getDeclaredField("sshd")
        field.isAccessible = true
        field.set(svc, sshd)

        val records = CopyOnWriteArrayList<LogRecord>()
        val handler = object : Handler() {
            override fun publish(record: LogRecord) {
                records.add(record)
            }

            override fun flush() {
            }

            override fun close() {
            }
        }
        val logger = Logger.getLogger("org.tekeli.borisp.ocpp16.diagnostics.SftpServerService")
        logger.addHandler(handler)
        try {
            assertDoesNotThrow { svc.stop() }
        } finally {
            logger.removeHandler(handler)
        }
        assertTrue(
            records.any { it.message.contains("sftp-stop-boom") },
            "expected stop error message to be logged, got: ${records.map { it.message }}"
        )
    }

    @Test
    fun `should reject correct username with wrong password`() {
        val svc = service()
        svc.start()
        try {
            waitUntilServerUp()
            assertFalse(attemptConnect("ocpp", "definitely-wrong-password"), "wrong password must be rejected")
        } finally {
            svc.stop()
        }
    }

    @Test
    fun `should reject wrong username with correct password`() {
        val svc = service()
        svc.start()
        try {
            waitUntilServerUp()
            assertFalse(attemptConnect("intruder", "testpass"), "wrong username must be rejected")
        } finally {
            svc.stop()
        }
    }
}

package org.tekeli.borisp.ocpp16.diagnostics

import io.quarkus.runtime.StartupEvent
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import java.nio.file.Path
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Handler
import java.util.logging.LogRecord
import java.util.logging.Logger

class DiagnosticsInitializerSurvivingMutantsTest {

    @TempDir
    lateinit var tempDir: Path

    private val records = CopyOnWriteArrayList<LogRecord>()
    private lateinit var handler: Handler
    private lateinit var logger: Logger

    @BeforeEach
    fun captureLogs() {
        logger = Logger.getLogger("org.tekeli.borisp.ocpp16.diagnostics")
        handler = object : Handler() {
            override fun publish(record: LogRecord) {
                records.add(record)
            }

            override fun flush() {}

            override fun close() {}
        }
        logger.addHandler(handler)
    }

    @AfterEach
    fun stopCapturingLogs() {
        logger.removeHandler(handler)
    }

    private fun sftpConfig(enabled: Boolean, host: String, port: Int) = object : SftpServerConfig {
        override fun enabled() = enabled
        override fun port() = port
        override fun host() = host
        override fun username() = "ocpp"
        override fun password() = "testpass"
    }

    private fun ftpConfig(enabled: Boolean, host: String, port: Int) = object : FtpServerConfig {
        override fun enabled() = enabled
        override fun port() = port
        override fun host() = host
        override fun username() = "ocpp"
        override fun password() = "testpass"
        override fun passivePorts() = "32100-32199"
        override fun externalAddress() = ""
    }

    private fun initializer(retentionDays: Int, sftp: SftpServerConfig, ftp: FtpServerConfig) =
        DiagnosticsInitializer().apply {
            diagnosticsConfig = object : DiagnosticsConfig {
                override fun uploadDir() = tempDir.toString()
                override fun maxFileSizeBytes() = 1024L
                override fun retentionDays() = retentionDays
                override fun preferredProtocol() = "sftp"
                override fun publicHost() = "127.0.0.1"
            }
            sftpServerConfig = sftp
            ftpServerConfig = ftp
        }

    private fun awaitRecord(timeoutMs: Long, predicate: (LogRecord) -> Boolean): LogRecord? {
        val deadline = System.currentTimeMillis() + timeoutMs
        while (true) {
            records.firstOrNull { predicate(it) }?.let { return it }
            if (System.currentTimeMillis() >= deadline) return null
            Thread.sleep(20)
        }
    }

    @Test
    fun `onStart logs SFTP start failure with exception message`() {
        val initializer = initializer(
            0,
            sftpConfig(true, "unresolvable-sftp-host-xyz", 23011),
            ftpConfig(false, "127.0.0.1", 24011)
        )
        initializer.onStart(StartupEvent())
        try {
            val record = awaitRecord(5000) {
                it.message.contains("Failed to start SFTP server") &&
                    it.message.contains("unresolvable-sftp-host-xyz")
            }
            assertTrue(record != null, "expected SFTP failure log containing the exception message")
        } finally {
            initializer.stop()
        }
    }

    @Test
    fun `onStart logs FTP start failure with exception message`() {
        val initializer = initializer(
            0,
            sftpConfig(false, "127.0.0.1", 23012),
            ftpConfig(true, "unresolvable-ftp-host-xyz", 24012)
        )
        initializer.onStart(StartupEvent())
        try {
            val record = awaitRecord(5000) {
                it.message.contains("Failed to start FTP server") &&
                    it.message.contains("Unknown host")
            }
            assertTrue(record != null, "expected FTP failure log containing the exception message")
        } finally {
            initializer.stop()
        }
    }

    @Test
    fun `cleanup task failure logs exception message`() {
        val storage = mock(FileSystemStorage::class.java)
        `when`(storage.cleanupExpired(anyInt())).thenThrow(RuntimeException("cleanup-boom"))
        val storageField = DiagnosticsInitializer::class.java
            .getDeclaredField("storage").apply { isAccessible = true }
        val schedulerField = DiagnosticsInitializer::class.java
            .getDeclaredField("scheduler").apply { isAccessible = true }
        var record: LogRecord? = null
        repeat(3) {
            val initializer = initializer(
                30,
                sftpConfig(false, "127.0.0.1", 23013),
                ftpConfig(false, "127.0.0.1", 24013)
            )
            val done = AtomicBoolean(false)
            val swapThread = Thread {
                while (!done.get() && schedulerField.get(initializer) == null) Thread.onSpinWait()
                if (!done.get()) storageField.set(initializer, storage)
            }.apply { isDaemon = true }
            swapThread.start()
            initializer.onStart(StartupEvent())
            record = awaitRecord(2000) { it.message.contains("cleanup-boom") }
            (schedulerField.get(initializer) as? ScheduledExecutorService)?.shutdownNow()
            initializer.stop()
            done.set(true)
            if (record != null) return
        }
        assertTrue(record != null, "expected cleanup failure log containing 'cleanup-boom'")
    }
}

package org.tekeli.borisp.ocpp16.diagnostics

import io.quarkus.arc.Unremovable
import io.quarkus.logging.Log
import io.quarkus.runtime.StartupEvent
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.event.Observes
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ApplicationScoped
@Unremovable
class DiagnosticsInitializer {

    @Inject
    lateinit var diagnosticsConfig: DiagnosticsConfig

    @Inject
    lateinit var sftpServerConfig: SftpServerConfig

    @Inject
    lateinit var ftpServerConfig: FtpServerConfig

    private var sftpService: SftpServerService? = null
    private var ftpService: FtpServerService? = null
    private var scheduler: java.util.concurrent.ScheduledExecutorService? = null
    private var storage: FileSystemStorage? = null

    @jakarta.enterprise.context.ApplicationScoped
    @Produces
    fun produceStorage(): FileSystemStorage {
        return storage ?: throw IllegalStateException("FileSystemStorage not yet initialized")
    }

    fun onStart(@Observes startupEvent: StartupEvent) {
        Log.info("Initializing diagnostics upload service")
        val baseDir = diagnosticsConfig.uploadDir()
        val maxFileSizeBytes = diagnosticsConfig.maxFileSizeBytes()
        Files.createDirectories(Path.of(baseDir))
        storage = FileSystemStorage(baseDir, maxFileSizeBytes)

        try {
            sftpService = SftpServerService(sftpServerConfig, storage!!)
            sftpService!!.start()
        } catch (e: Exception) {
            Log.error("Failed to start SFTP server: ${e.message}", e)
        }

        try {
            ftpService = FtpServerService(ftpServerConfig, storage!!)
            ftpService!!.start()
        } catch (e: Exception) {
            Log.error("Failed to start FTP server: ${e.message}", e)
        }

        val retentionDays = diagnosticsConfig.retentionDays()
        if (retentionDays > 0) {
            scheduler = Executors.newSingleThreadScheduledExecutor { r ->
                Thread(r, "diagnostics-cleanup").apply { isDaemon = true }
            }
            scheduler!!.scheduleAtFixedRate(
                {
                    try {
                        val count = storage!!.cleanupExpired(retentionDays)
                        if (count > 0) Log.info("Cleaned up $count expired diagnostics files")
                    } catch (e: Exception) {
                        Log.warn("Cleanup failed: ${e.message}")
                    }
                },
                0,
                24,
                TimeUnit.HOURS
            )
            Log.info("Scheduled diagnostics cleanup every 24 hours (retention: $retentionDays days)")
        }
        Log.info("Diagnostics upload service initialized (upload-dir: $baseDir)")
    }

    @PreDestroy
    fun stop() {
        scheduler?.run { shutdown(); awaitTermination(5, TimeUnit.SECONDS) }
        sftpService?.stop()
        ftpService?.stop()
    }
}

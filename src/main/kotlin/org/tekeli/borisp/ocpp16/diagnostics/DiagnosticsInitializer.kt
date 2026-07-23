package org.tekeli.borisp.ocpp16.diagnostics

import io.quarkus.logging.Log
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.enterprise.inject.Produces
import jakarta.inject.Inject
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@ApplicationScoped
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

    @PostConstruct
    fun init() {
        val baseDir = diagnosticsConfig.uploadDir()
        val maxFileSizeBytes = diagnosticsConfig.maxFileSizeBytes()
        Files.createDirectories(Path.of(baseDir))
        storage = FileSystemStorage(baseDir, maxFileSizeBytes)

        // Start SFTP server
        sftpService = SftpServerService(sftpServerConfig, storage!!)
        sftpService!!.start()

        // Start FTP server
        ftpService = FtpServerService(ftpServerConfig, storage!!)
        ftpService!!.start()

        // Start scheduled cleanup
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
        }
    }

    @PreDestroy
    fun stop() {
        scheduler?.run { shutdown(); awaitTermination(5, TimeUnit.SECONDS) }
        sftpService?.stop()
        ftpService?.stop()
    }
}

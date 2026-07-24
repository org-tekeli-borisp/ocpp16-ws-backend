package org.tekeli.borisp.ocpp16.diagnostics

open class DiagnosticsUrlGenerator(
    private val sftpConfig: SftpServerConfig,
    private val ftpConfig: FtpServerConfig,
    private val diagnosticsConfig: DiagnosticsConfig
) {

    fun generate(chargePointId: String): String {
        val protocol = diagnosticsConfig.preferredProtocol().lowercase()
        return generate(chargePointId, protocol)
    }

    fun generate(chargePointId: String, protocol: String): String {
        val publicHost = diagnosticsConfig.publicHost()
        return when (protocol.lowercase()) {
            "sftp" -> {
                if (!sftpConfig.enabled()) throw IllegalStateException("SFTP server is disabled")
                "sftp://${sftpConfig.username()}:${sftpConfig.password()}@$publicHost:${sftpConfig.port()}/$chargePointId"
            }
            "ftp" -> {
                if (!ftpConfig.enabled()) throw IllegalStateException("FTP server is disabled")
                "ftp://${ftpConfig.username()}:${ftpConfig.password()}@$publicHost:${ftpConfig.port()}/$chargePointId"
            }
            else -> throw IllegalStateException("Unknown protocol: $protocol (expected sftp or ftp)")
        }
    }
}

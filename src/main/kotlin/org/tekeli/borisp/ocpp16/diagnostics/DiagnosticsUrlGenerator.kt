package org.tekeli.borisp.ocpp16.diagnostics

open class DiagnosticsUrlGenerator(
    private val sftpConfig: SftpServerConfig,
    private val ftpConfig: FtpServerConfig,
    private val preferredProtocol: String
) {

    fun generate(chargePointId: String): String {
        val protocol = preferredProtocol.lowercase()
        return when (protocol) {
            "sftp" -> {
                if (!sftpConfig.enabled()) throw IllegalStateException("SFTP server is disabled")
                "sftp://${sftpConfig.username()}:${sftpConfig.password()}@${sftpConfig.host()}:${sftpConfig.port()}/$chargePointId"
            }
            "ftp" -> {
                if (!ftpConfig.enabled()) throw IllegalStateException("FTP server is disabled")
                "ftp://${ftpConfig.username()}:${ftpConfig.password()}@${ftpConfig.host()}:${ftpConfig.port()}/$chargePointId"
            }
            else -> throw IllegalStateException("Unknown protocol: $protocol (expected sftp or ftp)")
        }
    }
}

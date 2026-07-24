package org.tekeli.borisp.ocpp16.diagnostics

import io.smallrye.config.WithDefault

@io.smallrye.config.ConfigMapping(prefix = "ocpp.diagnostics")
interface DiagnosticsConfig {

    @WithDefault("/tmp/ocpp-diagnostics")
    fun uploadDir(): String

    @WithDefault("104857600")
    fun maxFileSizeBytes(): Long

    @WithDefault("30")
    fun retentionDays(): Int

    @WithDefault("sftp")
    fun preferredProtocol(): String

    @WithDefault("127.0.0.1")
    fun publicHost(): String
}

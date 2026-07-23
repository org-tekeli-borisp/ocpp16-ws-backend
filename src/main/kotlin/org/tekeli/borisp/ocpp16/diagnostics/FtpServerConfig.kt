package org.tekeli.borisp.ocpp16.diagnostics

import io.smallrye.config.WithDefault

@io.smallrye.config.ConfigMapping(prefix = "ocpp.ftp")
interface FtpServerConfig {

    @WithDefault("true")
    fun enabled(): Boolean

    @WithDefault("2021")
    fun port(): Int

    @WithDefault("0.0.0.0")
    fun host(): String

    @WithDefault("ocpp")
    fun username(): String

    @WithDefault("ocpp")
    fun password(): String
}

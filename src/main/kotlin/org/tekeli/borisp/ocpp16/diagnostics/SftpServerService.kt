package org.tekeli.borisp.ocpp16.diagnostics

import io.quarkus.logging.Log
import org.apache.sshd.server.SshServer
import org.apache.sshd.server.auth.password.PasswordAuthenticator
import org.apache.sshd.server.auth.pubkey.RejectAllPublickeyAuthenticator
import org.apache.sshd.server.channel.ChannelSession
import org.apache.sshd.server.command.Command
import org.apache.sshd.server.keyprovider.SimpleGeneratorHostKeyProvider
import org.apache.sshd.server.session.ServerSession
import org.apache.sshd.server.shell.ShellFactory
import org.apache.sshd.sftp.server.SftpSubsystemFactory
import java.io.File

class SftpServerService(
    private val config: SftpServerConfig,
    private val storage: FileSystemStorage
) {

    private var sshd: SshServer? = null

    fun start() {
        if (!config.enabled()) {
            Log.info("SFTP server is disabled")
            return
        }
        val server = SshServer.setUpDefaultServer()
        server.port = config.port()
        server.host = config.host()

        // SFTP subsystem
        server.subsystemFactories = listOf(SftpSubsystemFactory())

        // Password authentication
        server.setPasswordAuthenticator(object : PasswordAuthenticator {
            override fun authenticate(username: String?, password: String?, session: ServerSession?): Boolean {
                return username == config.username() && password == config.password()
            }
        })

        // Disable public key auth
        server.publickeyAuthenticator = RejectAllPublickeyAuthenticator.INSTANCE

        // Ephemeral host key
        val keyFile = File.createTempFile("sshd-hostkey-", ".ser")
        keyFile.deleteOnExit()
        server.keyPairProvider = SimpleGeneratorHostKeyProvider(keyFile.toPath())

        // Disable shell — we only need SFTP
        server.shellFactory = object : ShellFactory {
            override fun createShell(session: ChannelSession): Command? {
                return null
            }
        }

        server.start()
        sshd = server
        Log.info("SFTP server started on ${config.host()}:${config.port()}")
    }

    fun stop() {
        sshd?.run {
            try { stop(false) } catch (e: Exception) { Log.warn("SFTP server stop error: ${e.message}") }
        }
        sshd = null
        Log.info("SFTP server stopped")
    }
}

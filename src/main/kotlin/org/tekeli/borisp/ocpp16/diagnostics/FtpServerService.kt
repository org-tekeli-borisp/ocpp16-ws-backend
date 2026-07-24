package org.tekeli.borisp.ocpp16.diagnostics

import io.quarkus.logging.Log
import org.apache.ftpserver.DataConnectionConfigurationFactory
import org.apache.ftpserver.FtpServer
import org.apache.ftpserver.FtpServerFactory
import org.apache.ftpserver.ConnectionConfigFactory
import org.apache.ftpserver.listener.ListenerFactory
import org.apache.ftpserver.usermanager.PropertiesUserManagerFactory
import org.apache.ftpserver.usermanager.ClearTextPasswordEncryptor
import org.apache.ftpserver.usermanager.impl.BaseUser
import org.apache.ftpserver.usermanager.impl.WritePermission
import java.io.File

class FtpServerService(
    private val config: FtpServerConfig,
    private val storage: FileSystemStorage
) {

    private var server: FtpServer? = null

    fun start() {
        if (!config.enabled()) {
            Log.info("FTP server is disabled")
            return
        }

        // Create listener
        val listenerFactory = ListenerFactory()
        listenerFactory.port = config.port()
        listenerFactory.serverAddress = config.host()

        val dataConnFactory = DataConnectionConfigurationFactory()
        dataConnFactory.passivePorts = config.passivePorts()
        if (config.externalAddress().isNotEmpty()) {
            dataConnFactory.setPassiveExternalAddress(config.externalAddress())
        }
        listenerFactory.dataConnectionConfiguration = dataConnFactory.createDataConnectionConfiguration()

        // Create user manager programmatically
        val tempDir = File.createTempFile("ftpserver-", "dir")
        tempDir.delete()
        tempDir.mkdirs()
        tempDir.deleteOnExit()

        val usersFile = File(tempDir, "users.properties")
        usersFile.createNewFile()
        File(tempDir, "pass.properties").createNewFile()

        val userManagerFactory = PropertiesUserManagerFactory()
        userManagerFactory.file = usersFile
        userManagerFactory.passwordEncryptor = ClearTextPasswordEncryptor()
        val userManager = userManagerFactory.createUserManager()

        val user = BaseUser().apply {
            name = config.username()
            password = config.password()
            homeDirectory = storage.baseDir()
            enabled = true
            maxIdleTime = 0
            authorities = listOf(WritePermission())
        }
        userManager.save(user)

        // Create connection config
        val connectionConfigFactory = ConnectionConfigFactory()
        connectionConfigFactory.setAnonymousLoginEnabled(false)

        // Create server
        val serverFactory = FtpServerFactory()
        serverFactory.addListener("default", listenerFactory.createListener())
        serverFactory.userManager = userManager
        serverFactory.connectionConfig = connectionConfigFactory.createConnectionConfig()

        server = serverFactory.createServer()
        server!!.start()
        Log.info("FTP server started on ${config.host()}:${config.port()}")
    }

    fun stop() {
        server?.run {
            try { stop() } catch (e: Exception) { Log.warn("FTP server stop error: ${e.message}") }
        }
        server = null
        Log.info("FTP server stopped")
    }
}

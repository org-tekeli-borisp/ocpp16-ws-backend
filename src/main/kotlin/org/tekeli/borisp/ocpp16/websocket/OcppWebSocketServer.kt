package org.tekeli.borisp.ocpp16.websocket

import io.quarkus.logging.Log
import io.quarkus.websockets.next.*
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.tekeli.borisp.ocpp16.handler.*
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import java.util.*

@WebSocket(path = "/ocpp/{chargePointId}")
@ApplicationScoped
open class OcppWebSocketServer : ChargePointConnection, OcppHandlerContext {

    @Inject
    var connection: WebSocketConnection? = null

    @Inject
    override var chargePointRegistry: ChargePointRegistry? = null

    @Inject
    override var persistenceService: PersistenceService? = null

    @Inject
    override var metricsService: MetricsService? = null

    val activeConnection: WebSocketConnection
        get() = connection ?: throw IllegalStateException("Connection not initialized")

    private val activeRegistry: ChargePointRegistry
        get() = chargePointRegistry ?: throw IllegalStateException("Registry not initialized")

    private val activePersistence: PersistenceService
        get() = persistenceService ?: throw IllegalStateException("Persistence not initialized")

    override var responseAwaiter: ResponseAwaiter = ResponseAwaiter()
    open override var sessionId: String = ""
    override var chargePointId: String = ""

    private val dispatcher: MessageDispatcher by lazy {
        MessageDispatcher(createHandlers())
    }

    open fun createHandlers(): Map<String, OcppActionHandler> = mapOf(
        "BootNotification" to BootNotificationHandler(),
        "Heartbeat" to HeartbeatHandler(),
        "Authorize" to AuthorizeHandler(),
        "StartTransaction" to StartTransactionHandler(),
        "StopTransaction" to StopTransactionHandler(),
        "StatusNotification" to StatusNotificationHandler(),
        "DataTransfer" to DataTransferHandler(),
        "FirmwareStatusNotification" to FirmwareStatusNotificationHandler(),
        "DiagnosticsStatusNotification" to DiagnosticsStatusNotificationHandler(),
        "MeterValues" to MeterValuesHandler(),
        "SecurityEventNotification" to SecurityEventNotificationHandler(),
        "SignedFirmwareStatusNotification" to SignedFirmwareStatusNotificationHandler(),
        "LogStatusNotification" to LogStatusNotificationHandler(),
        "SignCertificate" to SignCertificateHandler(),
        "CertificateSigned" to CertificateSignedHandler()
    )

    @OnOpen
    fun onOpen() {
        chargePointId = activeConnection.pathParam("chargePointId")
        sessionId = activeConnection.id() ?: throw IllegalStateException("Connection id not available")
        responseAwaiter = ResponseAwaiter()
        activeRegistry.register(sessionId, sessionId, this, chargePointId)
        activePersistence.setChargePointOnlineById(chargePointId, sessionId)
        Log.info("WebSocket connection opened: session=$sessionId, chargePoint=$chargePointId")
    }

    @OnTextMessage
    fun onTextMessage(message: String): String {
        return dispatcher.dispatch(
            message,
            this,
            responseAwaiter,
            metricsService
        )
    }

    override fun sendText(text: String): io.smallrye.mutiny.Uni<Void> {
        val conn = connection
        if (conn != null) return conn.sendText(text)
        return io.smallrye.mutiny.Uni.createFrom().voidItem()
    }

    private fun generateMessageId(): String = UUID.randomUUID().toString()

    @OnClose
    fun onClose() {
        val connectionId = sessionId
        if (activeRegistry.isConnected(connectionId)) {
            activeRegistry.unregister(connectionId)
            responseAwaiter.rejectAll("WebSocket connection closed: $connectionId")
            activePersistence.setChargePointOffline(connectionId)
            Log.info("WebSocket connection closed: session=$connectionId")
        }
    }
}

package org.tekeli.borisp.ocpp16.websocket

import io.quarkus.logging.Log
import io.quarkus.websockets.next.*
import io.smallrye.mutiny.Uni
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.tekeli.borisp.ocpp16.handler.*
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.protocol.MessageCaptureService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.protocol.SchemaValidator
import java.util.*

@WebSocket(path = "/ocpp/{chargePointId}")
@ApplicationScoped
open class OcppWebSocketServer : ChargePointConnection, OcppHandlerContext {

    companion object {
        private const val DEFAULT_HEARTBEAT_INTERVAL_SECONDS = 300
        private const val DEFAULT_PING_INTERVAL_SECONDS = 360
        private const val DEFAULT_PONG_TIMEOUT_SECONDS = 720
    }

    @ConfigProperty(name = "ocpp.websocket.ping-interval-seconds", defaultValue = "${DEFAULT_PING_INTERVAL_SECONDS}")
    var pingIntervalSeconds: Long = DEFAULT_PING_INTERVAL_SECONDS.toLong()

    @ConfigProperty(name = "ocpp.websocket.pong-timeout-seconds", defaultValue = "${DEFAULT_PONG_TIMEOUT_SECONDS}")
    var pongTimeoutSeconds: Long = DEFAULT_PONG_TIMEOUT_SECONDS.toLong()

    @Inject
    var openConnections: OpenConnections? = null

    @Inject
    override var chargePointRegistry: ChargePointRegistry? = null

    @Inject
    override var persistenceService: PersistenceService? = null

    @Inject
    override var metricsService: MetricsService? = null

    @Inject
    var messageCaptureService: MessageCaptureService? = null

    @Inject
    open var schemaValidator: SchemaValidator? = null

    @Inject
    var vertx: Vertx? = null

    open var currentConnection: WebSocketConnection? = null

    val activeConnection: WebSocketConnection
        get() = currentConnection ?: throw IllegalStateException("Connection not initialized")

    private val activeRegistry: ChargePointRegistry
        get() = chargePointRegistry ?: throw IllegalStateException("Registry not initialized")

    private val activePersistence: PersistenceService
        get() = persistenceService ?: throw IllegalStateException("Persistence not initialized")

    override var responseAwaiter: ResponseAwaiter = ResponseAwaiter()
    open override var sessionId: String = ""
    override var chargePointId: String = ""

    @ConfigProperty(name = "ocpp.heartbeat.interval-seconds", defaultValue = "${DEFAULT_HEARTBEAT_INTERVAL_SECONDS}")
    override var heartbeatIntervalSeconds: Long = DEFAULT_HEARTBEAT_INTERVAL_SECONDS.toLong()

    private val dispatcher: MessageDispatcher by lazy {
        MessageDispatcher(createHandlers(), messageCaptureService, schemaValidator)
    }

    private val scheduler: Scheduler
        get() = VertxScheduler(vertx!!)
    private var pingPongManager: PingPongManager? = null

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
    fun onOpen(conn: WebSocketConnection) {
        currentConnection = conn
        val subprotocol = conn.subprotocol()
        if (subprotocol != "ocpp1.6") {
            Log.warn("Rejecting WebSocket connection: unsupported subprotocol '$subprotocol' (expected 'ocpp1.6')")
            activeConnection.closeAndAwait(io.quarkus.websockets.next.CloseReason(1003, "Subprotocol ocpp1.6 required"))
            return
        }
        initializeConnection()
    }

    open fun initializeConnection() {
        chargePointId = activeConnection.pathParam("chargePointId")
        sessionId = activeConnection.id() ?: throw IllegalStateException("Connection id not available")
        responseAwaiter = ResponseAwaiter()
        registerAndOnline()
        val remoteAddress = activeConnection.handshakeRequest().remoteAddress()
        Log.info("WebSocket connection opened: session=$sessionId, chargePoint=$chargePointId, remote=$remoteAddress")
    }

    private fun registerAndOnline() {
        activeRegistry.register(sessionId, sessionId, this, chargePointId)
        activePersistence.setChargePointOnlineById(chargePointId, sessionId)
        pingPongManager = PingPongManager(
            target = WebSocketPingPongTarget(
                connection = { activeConnection },
                registry = activeRegistry,
                persistence = activePersistence,
                sessionId = sessionId
            ) { responseAwaiter.rejectAll(it) },
            sessionId = sessionId,
            pingInterval = pingIntervalSeconds,
            pongTimeout = pongTimeoutSeconds,
            scheduler = scheduler
        ).also { it.start() }
    }

    @OnTextMessage
    fun onTextMessage(message: String): String {
        pingPongManager?.messageReceived()
        try {
            activePersistence.touchLastSeenAt(chargePointId)
        } catch (e: Exception) {
            Log.warn("touchLastSeenAt failed: ${e.message}")
        }
        return dispatcher.dispatch(
            message,
            this,
            responseAwaiter,
            metricsService
        )
    }

    @OnPingMessage
    fun onPingMessage(buffer: Buffer): Uni<Void> {
        return activeConnection.sendPong(buffer)
    }

    @OnPongMessage
    fun onPongMessage(buffer: Buffer) {
        pingPongManager?.pongReceived()
    }

    override fun sendText(text: String): Uni<Void> {
        val conn = currentConnection
        if (conn != null) return conn.sendText(text)
        return Uni.createFrom().voidItem()
    }

    private fun generateMessageId(): String = UUID.randomUUID().toString()

    @OnClose
    fun onClose() {
        pingPongManager?.stop()
        pingPongManager = null
        val connectionId = activeConnection.id() ?: return
        if (activeRegistry.isConnected(connectionId)) {
            activeRegistry.unregister(connectionId)
            responseAwaiter.rejectAll("WebSocket connection closed: $connectionId")
            try {
                activePersistence.setChargePointOfflineByChargePointId(chargePointId)
            } catch (_: Exception) {
            }
            Log.info("WebSocket connection closed: session=$connectionId, chargePoint=$chargePointId")
        }
    }
}

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

    private var _chargePointId: String = ""
    private var _sessionId: String = ""
    private var _responseAwaiter: ResponseAwaiter = ResponseAwaiter()

    private val activeSessionContext: SessionContext?
        get() {
            val connId = currentConnection?.id() ?: return null
            return activeRegistry.getContext(connId)
        }

    override var responseAwaiter: ResponseAwaiter
        get() = activeSessionContext?.responseAwaiter ?: _responseAwaiter
        set(value) { _responseAwaiter = value }

    override var chargePointId: String
        get() = activeSessionContext?.chargePointId ?: _chargePointId
        set(value) { _chargePointId = value }

    override var sessionId: String
        get() = activeSessionContext?.sessionId ?: _sessionId
        set(value) { _sessionId = value }

    private val activePingPongManager: PingPongManager?
        get() {
            val ctx = activeSessionContext
            return ctx?.let { activeRegistry.getPingPongManager(ctx.sessionId) }
        }

    @ConfigProperty(name = "ocpp.heartbeat.interval-seconds", defaultValue = "${DEFAULT_HEARTBEAT_INTERVAL_SECONDS}")
    override var heartbeatIntervalSeconds: Long = DEFAULT_HEARTBEAT_INTERVAL_SECONDS.toLong()

    private val dispatcher: MessageDispatcher by lazy {
        MessageDispatcher(createHandlers(), messageCaptureService, schemaValidator)
    }

    private val scheduler: Scheduler
        get() = VertxScheduler(vertx!!)

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
            conn.closeAndAwait(io.quarkus.websockets.next.CloseReason(1003, "Subprotocol ocpp1.6 required"))
            return
        }
        initializeConnection(conn)
    }

    open fun initializeConnection(conn: WebSocketConnection) {
        val chargePointId = conn.pathParam("chargePointId")
        val sessionId = conn.id() ?: throw IllegalStateException("Connection id not available")
        val responseAwaiter = ResponseAwaiter()
        registerAndOnline(sessionId, chargePointId, responseAwaiter)
        val remoteAddress = conn.handshakeRequest().remoteAddress()
        Log.info("WebSocket connection opened: session=$sessionId, chargePoint=$chargePointId, remote=$remoteAddress")
    }

    private fun registerAndOnline(sessionId: String, chargePointId: String, responseAwaiter: ResponseAwaiter) {
        activeRegistry.register(sessionId, sessionId, chargePointId, responseAwaiter)
        activePersistence.setChargePointOnlineById(chargePointId, sessionId)
        val conn = activeConnection
        val manager = PingPongManager(
            target = WebSocketPingPongTarget(
                connection = { conn },
                registry = activeRegistry,
                persistence = activePersistence,
                sessionId = sessionId
            ) { responseAwaiter.rejectAll(it) },
            sessionId = sessionId,
            pingInterval = pingIntervalSeconds,
            pongTimeout = pongTimeoutSeconds,
            scheduler = scheduler
        )
        manager.start()
        activeRegistry.setPingPongManager(sessionId, manager)
    }

    @OnTextMessage
    fun onTextMessage(message: String, conn: WebSocketConnection): String {
        val sessionCtx = activeRegistry.getContext(conn.id()) ?: return "[4,\"${UUID.randomUUID()}\",\"ProtocolError\",\"No session context\"]"
        val pingPongMgr = activeRegistry.getPingPongManager(sessionCtx.sessionId)
        pingPongMgr?.messageReceived()
        try {
            activePersistence.touchLastSeenAt(sessionCtx.chargePointId)
        } catch (e: Exception) {
            Log.warn("touchLastSeenAt failed: ${e.message}")
        }
        return dispatcher.dispatch(
            message,
            this,
            sessionCtx.responseAwaiter,
            metricsService,
            sessionCtx.chargePointId
        )
    }

    fun onTextMessage(message: String): String {
        return dispatcher.dispatch(message, this, responseAwaiter, metricsService)
    }

    @OnPingMessage
    fun onPingMessage(buffer: Buffer, conn: WebSocketConnection): Uni<Void> {
        return conn.sendPong(buffer)
    }

    @OnPongMessage
    fun onPongMessage(buffer: Buffer, conn: WebSocketConnection) {
        val sessionCtx = activeRegistry.getContext(conn.id()) ?: return
        activeRegistry.getPingPongManager(sessionCtx.sessionId)?.pongReceived()
    }

    override fun sendText(text: String): Uni<Void> {
        val conn = currentConnection
        return if (conn != null) conn.sendText(text) else Uni.createFrom().voidItem()
    }

    @OnClose
    fun onClose(conn: WebSocketConnection) {
        val sessionId = conn.id()
        activeRegistry.getPingPongManager(sessionId)?.stop()
        val ctx = activeRegistry.getContext(sessionId) ?: return
        if (activeRegistry.isConnected(sessionId)) {
            activeRegistry.unregister(sessionId)
            ctx.responseAwaiter.rejectAll("WebSocket connection closed: $sessionId")
            try {
                activePersistence.setChargePointOfflineByChargePointId(ctx.chargePointId)
            } catch (_: Exception) {
            }
            Log.info("WebSocket connection closed: session=$sessionId, chargePoint=${ctx.chargePointId}")
        }
    }

    fun onClose() {
        val connectionId = activeConnection.id()
        activePingPongManager?.stop()
        if (activeRegistry.isConnected(sessionId)) {
            activeRegistry.unregister(sessionId)
            responseAwaiter.rejectAll("WebSocket connection closed: $connectionId")
            try {
                activePersistence.setChargePointOfflineByChargePointId(chargePointId)
            } catch (_: Exception) {
            }
            Log.info("WebSocket connection closed: session=$connectionId, chargePoint=$chargePointId")
        }
    }
}

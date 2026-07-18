package org.tekeli.borisp.ocpp16.websocket

import io.quarkus.logging.Log
import io.quarkus.websockets.next.*
import io.smallrye.mutiny.Uni
import io.vertx.core.buffer.Buffer
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.tekeli.borisp.ocpp16.handler.*
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.protocol.MessageCaptureService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.protocol.SchemaValidator
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@WebSocket(path = "/ocpp/{chargePointId}")
@ApplicationScoped
open class OcppWebSocketServer : ChargePointConnection, OcppHandlerContext {

    companion object {
        private const val DEFAULT_PING_INTERVAL_SECONDS = 30
        private const val DEFAULT_PONG_TIMEOUT_SECONDS = 60
    }

    // Overridable for testing
    open val pingIntervalSeconds: Long = DEFAULT_PING_INTERVAL_SECONDS.toLong()
    open val pongTimeoutSeconds: Long = DEFAULT_PONG_TIMEOUT_SECONDS.toLong()

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

    private val dispatcher: MessageDispatcher by lazy {
        MessageDispatcher(createHandlers(), messageCaptureService, schemaValidator)
    }

    private val pingExecutor = Executors.newScheduledThreadPool(1) { r ->
        Thread(r, "ocpp-ping-pinger").apply { isDaemon = true }
    }

    private val isPinging = AtomicBoolean(false)
    private var pingFuture: java.util.concurrent.ScheduledFuture<*>? = null
    private var pongTimeoutFuture: java.util.concurrent.ScheduledFuture<*>? = null

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
            activeConnection.closeAndAwait(io.quarkus.websockets.next.CloseReason(4004, "Subprotocol ocpp1.6 required"))
            return
        }
        initializeConnection()
    }

    open fun initializeConnection() {
        chargePointId = activeConnection.pathParam("chargePointId")
        sessionId = activeConnection.id() ?: throw IllegalStateException("Connection id not available")
        responseAwaiter = ResponseAwaiter()
        registerAndOnline()
        Log.info("WebSocket connection opened: session=$sessionId, chargePoint=$chargePointId")
    }

    private fun registerAndOnline() {
        activeRegistry.register(sessionId, sessionId, this, chargePointId)
        activePersistence.setChargePointOnlineById(chargePointId, sessionId)
        startPingScheduler()
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

    @OnPingMessage
    fun onPingMessage(buffer: Buffer): Uni<Void> {
        return activeConnection.sendPong(buffer)
    }

    @OnPongMessage
    fun onPongMessage(buffer: Buffer) {
        cancelPongTimeout()
        isPinging.set(false)
    }

    open fun triggerPingAndPongTimeout() {
        sendPingAndScheduleTimeout()
    }

    open fun executePongTimeout() {
        if (isPinging.compareAndSet(true, false)) {
            handlePongTimeout()
        }
    }

    private fun startPingScheduler() {
        stopPingScheduler()
        pingFuture = pingExecutor.scheduleAtFixedRate({
            sendPingAndScheduleTimeout()
        }, pingIntervalSeconds, pingIntervalSeconds, TimeUnit.SECONDS)
    }

    private fun sendPingAndScheduleTimeout() {
        if (!isPinging.compareAndSet(false, true)) return
        try {
            activeConnection.sendPing(Buffer.buffer())
                .onFailure()
                .invoke { e: Throwable ->
                    Log.warn("Ping failed for session $sessionId: ${e.message}")
                    cancelPongTimeout()
                    try {
                        activeConnection.closeAndAwait(CloseReason(1001, "Ping failed"))
                    } catch (_: Exception) {}
                }
                .subscribe()
        } catch (e: Exception) {
            Log.warn("Failed to send ping for session $sessionId: ${e.message}")
            cancelPongTimeout()
            isPinging.set(false)
            return
        }
        schedulePongTimeout()
    }

    private fun schedulePongTimeout() {
        pongTimeoutFuture = pingExecutor.schedule({
            if (isPinging.compareAndSet(true, false)) {
                Log.warn("Pong timeout for session $sessionId: no pong received within ${pongTimeoutSeconds}s")
                handlePongTimeout()
            }
        }, pongTimeoutSeconds, TimeUnit.SECONDS)
    }

    private fun cancelPongTimeout() {
        pongTimeoutFuture?.cancel(false)
        pongTimeoutFuture = null
    }

    private fun handlePongTimeout() {
        try {
            activeConnection.closeAndAwait(CloseReason(1001, "Pong timeout"))
        } catch (_: Exception) {}
        try {
            activePersistence.setChargePointOffline(sessionId)
        } catch (_: Exception) {}
        if (activeRegistry.isConnected(sessionId)) {
            activeRegistry.unregister(sessionId)
            responseAwaiter.rejectAll("WebSocket connection closed: pong timeout $sessionId")
        }
        Log.info("WebSocket connection closed due to pong timeout: session=$sessionId")
    }

    private fun stopPingScheduler() {
        pingFuture?.cancel(false)
        pingFuture = null
        cancelPongTimeout()
        isPinging.set(false)
    }

    override fun sendText(text: String): io.smallrye.mutiny.Uni<Void> {
        val conn = currentConnection
        if (conn != null) return conn.sendText(text)
        return io.smallrye.mutiny.Uni.createFrom().voidItem()
    }

    private fun generateMessageId(): String = UUID.randomUUID().toString()

    @OnClose
    fun onClose() {
        stopPingScheduler()
        val connectionId = activeConnection.id() ?: return
        if (activeRegistry.isConnected(connectionId)) {
            activeRegistry.unregister(connectionId)
            responseAwaiter.rejectAll("WebSocket connection closed: $connectionId")
            try {
                activePersistence.setChargePointOffline(connectionId)
            } catch (_: Exception) {
            }
            Log.info("WebSocket connection closed: session=$connectionId")
        }
    }
}

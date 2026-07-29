package org.tekeli.borisp.ocpp16.websocket

import io.quarkus.logging.Log
import io.quarkus.websockets.next.CloseReason
import io.quarkus.websockets.next.OpenConnections
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.outbound.OutboundCallDispatcher
import org.tekeli.borisp.ocpp16.outbound.TextSender
import org.tekeli.borisp.ocpp16.outbound.WsSender
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.MessageCaptureService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

interface ChargePointConnection : TextSender {
    val responseAwaiter: ResponseAwaiter
}

@ApplicationScoped
class ChargePointRegistry {

    @Inject
    lateinit var openConnections: OpenConnections

    @Inject
    var metricsService: MetricsService? = null

    @Inject
    var messageCaptureService: MessageCaptureService? = null

    @Inject
    var persistenceService: PersistenceService? = null

    private val sessionInfos = ConcurrentHashMap<String, ChargePointInfo>()
    private val sessionContexts = ConcurrentHashMap<String, SessionContext>()
    private val chargePointIdIndex = ConcurrentHashMap<String, String>()
    private val testSenders = ConcurrentHashMap<String, TextSender>()

    val connectionCount: Int get() = sessionInfos.size
    val connectedSessionIds: Set<String> get() = Collections.unmodifiableSet(sessionInfos.keys)
    val connectedChargePointIds: Set<String> get() = Collections.unmodifiableSet(chargePointIdIndex.keys)

    fun register(sessionId: String, connectionId: String, chargePointId: String?, responseAwaiter: ResponseAwaiter) {
        val oldInfo = chargePointId?.let { getByChargePointId(it) }
        if (oldInfo != null && oldInfo.sessionId != sessionId) {
            disconnectSession(oldInfo.sessionId, oldInfo.connectionId)
        }
        sessionInfos[sessionId] = ChargePointInfo(
            sessionId = sessionId,
            connectionId = connectionId
        )
        sessionContexts[sessionId] = SessionContext(
            sessionId = sessionId,
            chargePointId = chargePointId ?: "",
            responseAwaiter = responseAwaiter
        )
        if (chargePointId != null) {
            chargePointIdIndex[chargePointId] = sessionId
        }
        metricsService?.onChargePointConnected()
    }

    fun register(sessionId: String, connectionId: String, connection: ChargePointConnection, chargePointId: String? = null) {
        register(sessionId, connectionId, chargePointId, connection.responseAwaiter)
    }

    fun setTestSender(sessionId: String, sender: TextSender) {
        testSenders[sessionId] = sender
    }

    fun unregister(sessionId: String) {
        sessionInfos.remove(sessionId)
        sessionContexts.remove(sessionId)
        testSenders.remove(sessionId)
        chargePointIdIndex.entries.removeAll { it.value == sessionId }
        metricsService?.onChargePointDisconnected()
    }

    fun disconnect(chargePointId: String) {
        val info = getByChargePointId(chargePointId)
            ?: throw IllegalStateException("ChargePoint not connected: $chargePointId")
        disconnectSession(info.sessionId, info.connectionId)
    }

    fun disconnectAll(): Int {
        val sessionInfosCopy = sessionInfos.values.toSet()
        var count = 0
        for (info in sessionInfosCopy) {
            disconnectSession(info.sessionId, info.connectionId)
            count++
        }
        return count
    }

    private fun disconnectSession(sessionId: String, connectionId: String) {
        val context = sessionContexts[sessionId] ?: return
        val pingPongMgr = getPingPongManager(sessionId)
        val chargePointId = context.chargePointId

        Log.info("Disconnecting session=$sessionId, chargePoint=$chargePointId")

        try {
            pingPongMgr?.stop()
        } catch (_: Exception) {
        }

        try {
            unregister(sessionId)
        } catch (_: Exception) {
        }

        context.responseAwaiter.rejectAll("Disconnected via REST API")

        try {
            persistenceService?.setChargePointOfflineByChargePointId(chargePointId)
        } catch (_: Exception) {
        }

        try {
            val conn = openConnections.findByConnectionId(connectionId).orElse(null)
            if (conn != null) {
                conn.closeAndAwait(CloseReason(1001, "Disconnected via REST API"))
                Log.info("WebSocket connection closed by REST API: session=$sessionId, chargePoint=$chargePointId")
            }
        } catch (_: Exception) {
        }
    }

    fun getContext(sessionId: String): SessionContext? = sessionContexts[sessionId]

    fun setPingPongManager(sessionId: String, manager: PingPongManager) {
        sessionContexts[sessionId]?.pingPongManager = manager
    }

    fun getPingPongManager(sessionId: String): PingPongManager? =
        sessionContexts[sessionId]?.pingPongManager

    fun getResponseAwaiter(sessionId: String): ResponseAwaiter? =
        sessionContexts[sessionId]?.responseAwaiter

    fun getChargePointId(sessionId: String): String? =
        sessionContexts[sessionId]?.chargePointId

    fun updateChargePointInfo(sessionId: String, chargePointId: String, vendor: String, model: String) {
        val existing = sessionInfos[sessionId]
            ?: throw IllegalStateException("Session not found: $sessionId")
        sessionInfos[sessionId] = existing.copy(
            chargePointId = chargePointId,
            vendor = vendor,
            model = model
        )
        chargePointIdIndex[chargePointId] = sessionId
    }

    fun getInfo(sessionId: String): ChargePointInfo? = sessionInfos[sessionId]

    fun getByChargePointId(chargePointId: String): ChargePointInfo? {
        val sessionId = chargePointIdIndex[chargePointId]
        if (sessionId == null) return null
        return sessionInfos[sessionId]
    }

    fun isConnected(sessionId: String): Boolean = sessionInfos.containsKey(sessionId)

    fun getConnection(sessionId: String): ChargePointConnection? {
        val context = sessionContexts[sessionId] ?: return null
        return object : ChargePointConnection {
            override val responseAwaiter = context.responseAwaiter
            override fun sendText(text: String) = io.smallrye.mutiny.Uni.createFrom().voidItem()
        }
    }

    fun sendCall(chargePointId: String, action: String, payload: Map<String, Any>?): java.util.concurrent.CompletableFuture<OcppMessage> {
        val info = getByChargePointId(chargePointId)
            ?: throw IllegalStateException("ChargePoint not connected: $chargePointId")
        val context = sessionContexts[info.sessionId]
            ?: throw IllegalStateException("ChargePoint context not available for session: ${info.sessionId}")
        val sender = testSenders[info.sessionId] ?: WsSender(openConnections, info.connectionId)
        val dispatcher = OutboundCallDispatcher(sender, context.responseAwaiter, messageCaptureService)
        metricsService?.messagesSent?.increment()
        return dispatcher.sendCall(chargePointId, action, payload)
    }
}

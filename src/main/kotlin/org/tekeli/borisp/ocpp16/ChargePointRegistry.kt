package org.tekeli.borisp.ocpp16

import io.quarkus.websockets.next.WebSocketConnection
import io.smallrye.mutiny.Uni
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.ConcurrentHashMap

interface ChargePointConnection : TextSender {
    val responseAwaiter: ResponseAwaiter
}

@ApplicationScoped
class ChargePointRegistry {
    private val sessionInfos = ConcurrentHashMap<String, ChargePointInfo>()
    private val sessionConnections = ConcurrentHashMap<String, ChargePointConnection>()
    private val sessionSenders = ConcurrentHashMap<String, TextSender>()
    private val chargePointIdIndex = ConcurrentHashMap<String, String>()

    val connectionCount: Int get() = sessionInfos.size
    val connectedSessionIds: Set<String> get() = sessionInfos.keys.toSet()
    val connectedChargePointIds: Set<String> get() = chargePointIdIndex.keys.toSet()

    fun register(sessionId: String, connection: ChargePointConnection) {
        sessionInfos[sessionId] = ChargePointInfo(sessionId = sessionId)
        sessionConnections[sessionId] = connection
    }

    fun setSender(sessionId: String, sender: TextSender) {
        sessionSenders[sessionId] = sender
    }

    fun unregister(sessionId: String) {
        sessionInfos.remove(sessionId)
        sessionConnections.remove(sessionId)
        sessionSenders.remove(sessionId)
        chargePointIdIndex.entries.removeAll { it.value == sessionId }
    }

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

    fun getConnection(sessionId: String): ChargePointConnection? = sessionConnections[sessionId]

    fun isConnected(sessionId: String): Boolean = sessionInfos.containsKey(sessionId)

    fun sendCall(chargePointId: String, action: String, payload: Map<String, Any>?): java.util.concurrent.CompletableFuture<OcppMessage> {
        val info = getByChargePointId(chargePointId)
            ?: throw IllegalStateException("ChargePoint not connected: $chargePointId")
        val sender = sessionSenders[info.sessionId]
            ?: throw IllegalStateException("WebSocket sender not available for session: ${info.sessionId}")
        val connection = sessionConnections[info.sessionId]
            ?: throw IllegalStateException("ChargePoint connection not available for session: ${info.sessionId}")
        val dispatcher = OutboundCallDispatcher(sender, connection.responseAwaiter)
        return dispatcher.sendCall(action, payload)
    }
}

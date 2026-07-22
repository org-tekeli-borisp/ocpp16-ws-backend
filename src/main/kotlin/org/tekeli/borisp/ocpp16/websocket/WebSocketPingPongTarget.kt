package org.tekeli.borisp.ocpp16.websocket

import io.quarkus.logging.Log
import io.quarkus.websockets.next.CloseReason
import io.quarkus.websockets.next.WebSocketConnection
import io.smallrye.mutiny.Uni
import io.vertx.core.buffer.Buffer
import org.tekeli.borisp.ocpp16.persistence.PersistenceService

class WebSocketPingPongTarget(
    private val connection: () -> WebSocketConnection,
    private val registry: ChargePointRegistry,
    private val persistence: PersistenceService,
    private val sessionId: String,
    private val rejectAwaiterFn: (String) -> Unit
) : PingPongTarget {

    override fun sendPing(buffer: Buffer): Uni<Void> =
        connection().sendPing(buffer)

    override fun closeConnection(reason: String): Uni<Void> {
        Log.warn("Closing session $sessionId: $reason")
        return connection().close(CloseReason(1001, reason))
    }

    override fun setChargePointOffline(id: String) =
        persistence.setChargePointOffline(id)

    override fun unregisterFromRegistry(id: String) =
        registry.unregister(id)

    override fun isConnected(id: String): Boolean =
        registry.isConnected(id)

    override fun rejectAwaiter(message: String) =
        rejectAwaiterFn(message)

    override fun executeAsync(runnable: Runnable) =
        runnable.run()
}

package org.tekeli.borisp.ocpp16.rest

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.ws.rs.GET
import jakarta.ws.rs.Path
import jakarta.ws.rs.Produces
import jakarta.ws.rs.core.MediaType
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import java.time.Instant
import java.time.temporal.ChronoUnit

@Path("")
@Produces(MediaType.APPLICATION_JSON)
class HealthResource {

    @Inject
    lateinit var chargePointRegistry: ChargePointRegistry

    @Inject
    lateinit var persistenceService: PersistenceService

    private val startTime = Instant.now()

    @GET
    @Path("/health")
    fun health(): Map<String, Any> {
        return mapOf(
            "status" to "UP",
            "uptime" to uptimeString(),
            "timestamp" to Instant.now().toString(),
            "websockets" to mapOf(
                "connected" to chargePointRegistry.connectionCount
            )
        )
    }

    @GET
    @Path("/api/status")
    fun status(): Map<String, Any> {
        val chargePoints = persistenceService.findAllChargePoints()
        val online = chargePoints.count { it.status.name == "ONLINE" }
        val offline = chargePoints.size - online

        return mapOf(
            "uptime" to uptimeString(),
            "timestamp" to Instant.now().toString(),
            "websockets" to mapOf(
                "connected" to chargePointRegistry.connectionCount,
                "sessions" to chargePointRegistry.connectedSessionIds.toList()
            ),
            "chargePoints" to mapOf(
                "registered" to chargePoints.size,
                "online" to online,
                "offline" to offline,
                "list" to chargePoints.map {
                    mapOf<String, Any?>(
                        "chargePointId" to it.chargePointId,
                        "vendor" to it.vendor,
                        "model" to it.model,
                        "firmwareVersion" to it.firmwareVersion,
                        "status" to it.status.name,
                        "lastSeenAt" to it.lastSeenAt.toString()
                    )
                }
            )
        )
    }

    private fun uptimeString(): String {
        val start = startTime
        val now = Instant.now()
        val seconds = ChronoUnit.SECONDS.between(start, now)
        return when {
            seconds >= 3600 -> {
                val hours = seconds / 3600
                val mins = (seconds % 3600) / 60
                "${hours}h ${mins}m"
            }
            seconds >= 60 -> {
                val mins = seconds / 60
                val secs = seconds % 60
                "${mins}m ${secs}s"
            }
            else -> "${seconds}s"
            }
    }
}

package org.tekeli.borisp.ocpp16.health

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness
import org.tekeli.borisp.ocpp16.persistence.PersistenceService

@Readiness
@ApplicationScoped
class ReadinessHealthCheck : HealthCheck {

    @Inject
    lateinit var persistenceService: PersistenceService

    override fun call(): HealthCheckResponse {
        return try {
            persistenceService.findAllChargePoints()
            HealthCheckResponse.named("database").up().build()
        } catch (e: Exception) {
            HealthCheckResponse.named("database")
                .withData("error", e.message ?: "Database not reachable")
                .down()
                .build()
        }
    }
}

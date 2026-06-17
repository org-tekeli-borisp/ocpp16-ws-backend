package org.tekeli.borisp.ocpp16.health

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.eclipse.microprofile.health.HealthCheck
import org.eclipse.microprofile.health.HealthCheckResponse
import org.eclipse.microprofile.health.Readiness

@Readiness
@ApplicationScoped
class ReadinessHealthCheck : HealthCheck {

    @Inject
    lateinit var em: EntityManager

    override fun call(): HealthCheckResponse {
        return try {
            em.createNativeQuery("SELECT 1").singleResult
            HealthCheckResponse.named("database").up().build()
        } catch (e: Exception) {
            HealthCheckResponse.named("database")
                .withData("error", e.message ?: "Database not reachable")
                .down()
                .build()
        }
    }
}

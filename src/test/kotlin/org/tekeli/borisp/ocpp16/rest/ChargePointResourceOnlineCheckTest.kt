package org.tekeli.borisp.ocpp16.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.ChargePoint
import org.tekeli.borisp.ocpp16.persistence.ChargePointStatus
import jakarta.inject.Inject
import jakarta.persistence.EntityManager

@QuarkusTest
@jakarta.transaction.Transactional
class ChargePointResourceOnlineCheckTest {

    @Inject
    lateinit var em: EntityManager

    @BeforeEach
    fun setup() {
        em.createNativeQuery("DELETE FROM transactions").executeUpdate()
        em.createNativeQuery("DELETE FROM charge_points").executeUpdate()
        em.flush()

        // Simulate stale ONLINE state: DB says ONLINE but no WebSocket connection exists
        em.persist(ChargePoint(
            chargePointId = "STALE-CP-001",
            vendor = "TestVendor",
            model = "TestModel",
            firmwareVersion = "1.0",
            status = ChargePointStatus.ONLINE,
            sessionId = "non-existent-session"
        ))
        em.persist(ChargePoint(
            chargePointId = "TRULY-OFFLINE-CP",
            vendor = "TestVendor",
            model = "TestModel",
            firmwareVersion = "1.0",
            status = ChargePointStatus.OFFLINE,
            sessionId = ""
        ))
        em.flush()
    }

    @Test
    fun `charge point with ONLINE status but no WebSocket connection must report as OFFLINE`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints/STALE-CP-001")
            .then()
            .statusCode(200)
            .body("status", org.hamcrest.Matchers.equalTo("OFFLINE"))
    }

    @Test
    fun `getAll must not report stale ONLINE charge points as online`() {
        val statuses = RestAssured.given()
            .`when`().get("/api/chargepoints")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("status", String::class.java)

        assertTrue(statuses.all { it == "OFFLINE" }, "All charge points without active WebSocket must be OFFLINE, got: $statuses")
    }
}

package org.tekeli.borisp.ocpp16.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.ChargePoint
import org.tekeli.borisp.ocpp16.persistence.ChargePointStatus
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.websocket.ChargePointConnection
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import jakarta.inject.Inject
import jakarta.persistence.EntityManager

@QuarkusTest
@jakarta.transaction.Transactional
class ChargePointResourceTest {

    @Inject
    lateinit var em: EntityManager

    @Inject
    lateinit var chargePointRegistry: ChargePointRegistry

    private val dummyConnection = object : ChargePointConnection {
        override val responseAwaiter = ResponseAwaiter()
        override fun sendText(text: String) = io.smallrye.mutiny.Uni.createFrom().voidItem()
    }

    @BeforeEach
    fun setup() {
        em.createNativeQuery("DELETE FROM transactions").executeUpdate()
        em.createNativeQuery("DELETE FROM charge_points").executeUpdate()
        em.flush()

        chargePointRegistry.connectedSessionIds.forEach { chargePointRegistry.unregister(it) }

        em.persist(ChargePoint(chargePointId = "Tesla-Model3-1.0", vendor = "Tesla", model = "Model3", firmwareVersion = "1.0", status = ChargePointStatus.ONLINE, sessionId = "session-1"))
        em.persist(ChargePoint(chargePointId = "ABB-Terra-2.1", vendor = "ABB", model = "Terra", firmwareVersion = "2.1", status = ChargePointStatus.OFFLINE, sessionId = "session-2"))
        em.persist(ChargePoint(chargePointId = "Siemens-Veritar-3.0", vendor = "Siemens", model = "Veritar", firmwareVersion = "3.0", status = ChargePointStatus.ONLINE, sessionId = "session-3"))
        em.flush()

        chargePointRegistry.register("session-1", "session-1", dummyConnection, "Tesla-Model3-1.0")
        chargePointRegistry.register("session-3", "session-3", dummyConnection, "Siemens-Veritar-3.0")
    }

    @Test
    fun `should return all chargePoints`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints")
            .then()
            .statusCode(200)
            .body("size()", org.hamcrest.Matchers.equalTo(3))
    }

    @Test
    fun `should return chargePoint details`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints")
            .then()
            .statusCode(200)
            .body("find { it.chargePointId == 'Tesla-Model3-1.0' }.vendor", org.hamcrest.Matchers.equalTo("Tesla"))
            .body("find { it.chargePointId == 'Tesla-Model3-1.0' }.status", org.hamcrest.Matchers.equalTo("ONLINE"))
    }

    @Test
    fun `should return single chargePoint by id`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints/Tesla-Model3-1.0")
            .then()
            .statusCode(200)
            .body("chargePointId", org.hamcrest.Matchers.equalTo("Tesla-Model3-1.0"))
            .body("vendor", org.hamcrest.Matchers.equalTo("Tesla"))
            .body("model", org.hamcrest.Matchers.equalTo("Model3"))
            .body("firmwareVersion", org.hamcrest.Matchers.equalTo("1.0"))
            .body("status", org.hamcrest.Matchers.equalTo("ONLINE"))
    }

    @Test
    fun `should return 404 for non-existent chargePoint`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints/NONEXISTENT")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should return only online chargePoints`() {
        val statuses = RestAssured.given()
            .queryParam("status", "ONLINE")
            .`when`().get("/api/chargepoints")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()
            .getList("status", String::class.java)

        assertEquals(2, statuses.size)
        assertTrue(statuses.all { it == "ONLINE" })
    }

   @Test
    fun `should return 400 for invalid status`() {
        RestAssured.given()
            .queryParam("status", "NONEXISTENT")
            .`when`().get("/api/chargepoints")
            .then()
            .statusCode(400)
    }
}

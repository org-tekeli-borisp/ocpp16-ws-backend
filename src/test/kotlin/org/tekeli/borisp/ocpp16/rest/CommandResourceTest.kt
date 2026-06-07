package org.tekeli.borisp.ocpp16.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.ChargePoint
import org.tekeli.borisp.ocpp16.persistence.ChargePointStatus
import java.time.Instant

@QuarkusTest
@jakarta.transaction.Transactional
class CommandResourceTest {

    @Inject
    lateinit var em: EntityManager

    private lateinit var chargePointId: String

    @BeforeEach
    fun setup() {
        em.createNativeQuery("DELETE FROM transactions").executeUpdate()
        em.createNativeQuery("DELETE FROM charge_points").executeUpdate()
        em.flush()

        val cp = ChargePoint(chargePointId = "Tesla-Model3-1.0", vendor = "Tesla", model = "Model3", firmwareVersion = "1.0", status = ChargePointStatus.ONLINE, sessionId = "ws-session-1")
        em.persist(cp)
        em.flush()
        chargePointId = cp.chargePointId
    }

    @Test
    fun `should return 404 for command on non-existent chargePoint`() {
        RestAssured.given()
            .contentType("application/json")
            .body("""{"idTag": "CARD123", "connectorId": 1}""")
            .`when`().post("/api/chargepoints/NONEXISTENT/commands/remote-start-transaction")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should return 404 for non-existent command type`() {
        RestAssured.given()
            .contentType("application/json")
            .body("""{}""")
            .`when`().post("/api/chargepoints/$chargePointId/commands/nonexistent-command")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should return 503 for remote-start-transaction when chargePoint not connected`() {
        RestAssured.given()
            .contentType("application/json")
            .body("""{"idTag": "CARD123", "connectorId": 1}""")
            .`when`().post("/api/chargepoints/$chargePointId/commands/remote-start-transaction")
            .then()
            .statusCode(503)
            .body("error", org.hamcrest.Matchers.containsString("not connected"))
    }

    @Test
    fun `should return 503 for remote-stop-transaction when chargePoint not connected`() {
        RestAssured.given()
            .contentType("application/json")
            .body("""{"transactionId": 1}""")
            .`when`().post("/api/chargepoints/$chargePointId/commands/remote-stop-transaction")
            .then()
            .statusCode(503)
            .body("error", org.hamcrest.Matchers.containsString("not connected"))
    }

    @Test
    fun `should return 503 for reset when chargePoint not connected`() {
        RestAssured.given()
            .contentType("application/json")
            .body("""{"type": "Hard"}""")
            .`when`().post("/api/chargepoints/$chargePointId/commands/reset")
            .then()
            .statusCode(503)
            .body("error", org.hamcrest.Matchers.containsString("not connected"))
    }

    @Test
    fun `should reject reset with invalid type`() {
        RestAssured.given()
            .contentType("application/json")
            .body("""{"type": "InvalidType"}""")
            .`when`().post("/api/chargepoints/$chargePointId/commands/reset")
            .then()
            .statusCode(400)
    }

    @Test
    fun `should return 503 for unlock-connector when chargePoint not connected`() {
        RestAssured.given()
            .contentType("application/json")
            .body("""{"connectorId": 1}""")
            .`when`().post("/api/chargepoints/$chargePointId/commands/unlock-connector")
            .then()
            .statusCode(503)
            .body("error", org.hamcrest.Matchers.containsString("not connected"))
    }

    @Test
    fun `should reject command with missing required fields`() {
        RestAssured.given()
            .contentType("application/json")
            .body("""{}""")
            .`when`().post("/api/chargepoints/$chargePointId/commands/remote-start-transaction")
            .then()
            .statusCode(400)
    }

    @Test
    fun `should return list of available commands`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints/$chargePointId/commands")
            .then()
            .statusCode(200)
            .body("contains('remote-start-transaction')", org.hamcrest.Matchers.equalTo(true))
            .body("contains('remote-stop-transaction')", org.hamcrest.Matchers.equalTo(true))
            .body("contains('reset')", org.hamcrest.Matchers.equalTo(true))
            .body("contains('unlock-connector')", org.hamcrest.Matchers.equalTo(true))
    }
}

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
import org.tekeli.borisp.ocpp16.persistence.Transaction
import java.time.Instant

@QuarkusTest
@jakarta.transaction.Transactional
class TransactionResourceTest {

    @Inject
    lateinit var em: EntityManager

    @BeforeEach
    fun setup() {
        em.createNativeQuery("DELETE FROM transactions").executeUpdate()
        em.createNativeQuery("DELETE FROM charge_points").executeUpdate()
        em.flush()

        em.persist(ChargePoint(chargePointId = "Tesla-Model3-1.0", vendor = "Tesla", model = "Model3", status = ChargePointStatus.ONLINE, sessionId = "s1"))
        em.flush()

        val txn1 = Transaction(chargePointId = "Tesla-Model3-1.0", connectorId = 1, idTag = "CARD1", meterStart = 1000, startTime = Instant.parse("2024-01-01T00:00:00Z"))
        em.persist(txn1)
        em.flush()

        val txn2 = Transaction(chargePointId = "Tesla-Model3-1.0", connectorId = 2, idTag = "CARD2", meterStart = 500, startTime = Instant.parse("2024-01-01T01:00:00Z"), stopTime = Instant.parse("2024-01-01T02:00:00Z"), meterStop = 2000, stopReason = "Local")
        em.persist(txn2)
        em.flush()
    }

    @Test
    fun `should return all transactions for chargePoint`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints/Tesla-Model3-1.0/transactions")
            .then()
            .statusCode(200)
            .body("size()", org.hamcrest.Matchers.greaterThan(1))
    }

    @Test
    fun `should return only running transactions`() {
        val result = RestAssured.given()
            .queryParam("running", true)
            .`when`().get("/api/chargepoints/Tesla-Model3-1.0/transactions")
            .then()
            .statusCode(200)
            .extract()
            .body()
            .asString()

        val ids = io.restassured.path.json.JsonPath(result).getList("id", Long::class.java)
        assertEquals(1, ids.size)
    }

    @Test
    fun `should return transaction details with correct fields`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints/Tesla-Model3-1.0/transactions")
            .then()
            .statusCode(200)
            .body("find { it.idTag == 'CARD1' }.chargePointId", org.hamcrest.Matchers.equalTo("Tesla-Model3-1.0"))
            .body("find { it.idTag == 'CARD1' }.connectorId", org.hamcrest.Matchers.equalTo(1))
            .body("find { it.idTag == 'CARD1' }.meterStart", org.hamcrest.Matchers.equalTo(1000))
    }

    @Test
    fun `should return stopped transaction with stop details`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints/Tesla-Model3-1.0/transactions")
            .then()
            .statusCode(200)
            .body("find { it.idTag == 'CARD2' }.stopReason", org.hamcrest.Matchers.equalTo("Local"))
            .body("find { it.idTag == 'CARD2' }.meterStop", org.hamcrest.Matchers.equalTo(2000))
    }

    @Test
    fun `should return empty list for chargePoint with no transactions`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints/NONEXISTENT/transactions")
            .then()
            .statusCode(200)
            .body("size()", org.hamcrest.Matchers.equalTo(0))
    }
}

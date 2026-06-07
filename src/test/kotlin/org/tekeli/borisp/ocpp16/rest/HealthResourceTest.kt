package org.tekeli.borisp.ocpp16.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.ChargePoint
import org.tekeli.borisp.ocpp16.persistence.ChargePointStatus

@QuarkusTest
@jakarta.transaction.Transactional
class HealthResourceTest {

    @Inject
    lateinit var em: EntityManager

    @Test
    fun `health endpoint returns 200 with ok status`() {
        RestAssured.given()
            .`when`().get("/health")
            .then()
            .statusCode(200)
            .body("status", org.hamcrest.Matchers.equalTo("UP"))
    }

    @Test
    fun `health endpoint contains uptime`() {
        RestAssured.given()
            .`when`().get("/health")
            .then()
            .statusCode(200)
            .body("uptime", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString()))
    }

    @Test
    fun `health endpoint contains timestamp`() {
        RestAssured.given()
            .`when`().get("/health")
            .then()
            .statusCode(200)
            .body("timestamp", org.hamcrest.Matchers.not(org.hamcrest.Matchers.emptyString()))
    }

    @Test
    fun `health endpoint returns connected count`() {
        RestAssured.given()
            .`when`().get("/health")
            .then()
            .statusCode(200)
            .body("websockets.connected", org.hamcrest.Matchers.greaterThanOrEqualTo(0))
    }

    @Test
    fun `status endpoint returns chargePoints registered in DB`() {
        RestAssured.given()
            .`when`().get("/api/status")
            .then()
            .statusCode(200)
            .body("chargePoints.registered", org.hamcrest.Matchers.greaterThanOrEqualTo(0))
            .body("chargePoints.list.size()", org.hamcrest.Matchers.greaterThanOrEqualTo(0))
    }

    @Test
    fun `status endpoint returns connected count`() {
        RestAssured.given()
            .`when`().get("/api/status")
            .then()
            .statusCode(200)
            .body("websockets.connected", org.hamcrest.Matchers.greaterThanOrEqualTo(0))
    }

    @Test
    fun `status endpoint returns online and offline counts`() {
        RestAssured.given()
            .`when`().get("/api/status")
            .then()
            .statusCode(200)
            .body("chargePoints.online", org.hamcrest.Matchers.greaterThanOrEqualTo(0))
            .body("chargePoints.offline", org.hamcrest.Matchers.greaterThanOrEqualTo(0))
    }

    @Test
    fun `health endpoint returns application json content type`() {
        RestAssured.given()
            .`when`().get("/health")
            .then()
            .header("Content-Type", org.hamcrest.Matchers.startsWith("application/json"))
    }

    @Test
    fun `status endpoint returns application json content type`() {
        RestAssured.given()
            .`when`().get("/api/status")
            .then()
            .header("Content-Type", org.hamcrest.Matchers.startsWith("application/json"))
    }

    @Test
    fun `status endpoint returns online and offline breakdown`() {
        val response = RestAssured.given()
            .`when`().get("/api/status")
            .then()
            .statusCode(200)
            .extract()
            .path<Map<String, Any>>("chargePoints")

        val registered = response["registered"] as Int
        val online = response["online"] as Int
        val offline = response["offline"] as Int

        assert(online + offline == registered) {
            "online ($online) + offline ($offline) should equal registered ($registered)"
        }
    }
}

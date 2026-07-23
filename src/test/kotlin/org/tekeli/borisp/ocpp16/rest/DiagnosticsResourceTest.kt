package org.tekeli.borisp.ocpp16.rest

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class DiagnosticsResourceTest {

    @Test
    fun `should return empty list for CP with no diagnostics`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints/CP-001/diagnostics")
            .then()
            .statusCode(200)
            .body("size()", equalTo(0))
    }

    @Test
    fun `should return 200 with list endpoint accessible`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints/CP-999/diagnostics")
            .then()
            .statusCode(200)
    }

    @Test
    fun `should return 404 for non-existent file`() {
        RestAssured.given()
            .`when`().get("/api/chargepoints/CP-001/diagnostics/nonexistent.log")
            .then()
            .statusCode(404)
    }

    @Test
    fun `should return 404 when deleting non-existent file`() {
        RestAssured.given()
            .`when`().delete("/api/chargepoints/CP-001/diagnostics/nonexistent.log")
            .then()
            .statusCode(404)
    }
}

package org.tekeli.borisp.ocpp16.rest

import io.restassured.RestAssured
import io.restassured.http.ContentType
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.MessageCaptureService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppMessageDirection

@Timeout(30)
@QuarkusTest
class MessageResourceTest {

    @Inject
    lateinit var persistenceService: PersistenceService

    @Inject
    lateinit var messageCaptureService: MessageCaptureService

    private val testCpId = "test-msg-cp-${System.currentTimeMillis()}"

    private fun createTestCp() {
        persistenceService.upsertChargePoint("sess-${testCpId}", testCpId, "V", "M", null)
    }

    @Test
    fun `getMessages returns empty for new charge point`() {
        createTestCp()

        RestAssured.given()
            .contentType(ContentType.JSON)
            .get("/api/chargepoints/$testCpId/messages")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .body("size()", org.hamcrest.Matchers.`is`(0))
    }

    @Test
    fun `getMessages returns buffered messages`() {
        createTestCp()

        messageCaptureService.capture(testCpId, OcppMessageDirection.INBOUND,
            OcppMessage.Call("m1", "BootNotification", mapOf("vendor" to "V")))

        RestAssured.given()
            .contentType(ContentType.JSON)
            .get("/api/chargepoints/$testCpId/messages")
            .then()
            .statusCode(200)
            .body("size()", org.hamcrest.Matchers.greaterThan(0))
            .body("[0].messageType", org.hamcrest.Matchers.`is`("CALL"))
            .body("[0].direction", org.hamcrest.Matchers.`is`("INBOUND"))
    }

    @Test
    fun `getMessages returns 404 for unknown charge point`() {
        RestAssured.given()
            .contentType(ContentType.JSON)
            .get("/api/chargepoints/nonexistent/messages")
            .then()
            .statusCode(404)
    }

    @Test
    fun `getMessages with direction filter`() {
        createTestCp()

        messageCaptureService.capture(testCpId, OcppMessageDirection.INBOUND,
            OcppMessage.Call("m1", "Heartbeat", null))
        messageCaptureService.capture(testCpId, OcppMessageDirection.OUTBOUND,
            OcppMessage.Call("m2", "Reset", mapOf("type" to "Soft")))

        val inbound = RestAssured.given()
            .contentType(ContentType.JSON)
            .queryParam("direction", "INBOUND")
            .get("/api/chargepoints/$testCpId/messages")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()

        assertEquals("INBOUND", inbound.getList("", Map::class.java)[0]["direction"])
    }

    @Test
    fun `getMessages with action filter`() {
        createTestCp()

        messageCaptureService.capture(testCpId, OcppMessageDirection.INBOUND,
            OcppMessage.Call("m1", "Heartbeat", null))
        messageCaptureService.capture(testCpId, OcppMessageDirection.INBOUND,
            OcppMessage.Call("m2", "BootNotification", mapOf("vendor" to "V")))

        val filtered = RestAssured.given()
            .contentType(ContentType.JSON)
            .queryParam("action", "Heartbeat")
            .get("/api/chargepoints/$testCpId/messages")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()

        assertEquals(1, filtered.getList("", Map::class.java).size)
        assertEquals("Heartbeat", filtered.getList("", Map::class.java)[0]["action"])
    }

    @Test
    fun `getHistory returns paginated results`() {
        createTestCp()

        messageCaptureService.capture(testCpId, OcppMessageDirection.INBOUND,
            OcppMessage.Call("m1", "Heartbeat", null))

        Thread.sleep(2000)

        val response = RestAssured.given()
            .contentType(ContentType.JSON)
            .queryParam("limit", 100)
            .get("/api/chargepoints/$testCpId/messages/history")
            .then()
            .statusCode(200)
            .contentType(ContentType.JSON)
            .extract()
            .body()
            .`as`(Map::class.java)

        assertTrue(response.containsKey("total"))
        assertTrue(response.containsKey("messages"))
        assertTrue(response.containsKey("offset"))
        assertTrue(response.containsKey("limit"))
    }

    @Test
    fun `getMessages respects limit`() {
        createTestCp()

        repeat(5) { i ->
            messageCaptureService.capture(testCpId, OcppMessageDirection.INBOUND,
                OcppMessage.Call("m$i", "Heartbeat", null))
        }

        val limited = RestAssured.given()
            .contentType(ContentType.JSON)
            .queryParam("limit", 3)
            .get("/api/chargepoints/$testCpId/messages")
            .then()
            .statusCode(200)
            .extract()
            .jsonPath()

        assertEquals(3, limited.getList("", Map::class.java).size)
    }
}

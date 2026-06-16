package org.tekeli.borisp.ocpp16.command

import jakarta.ws.rs.core.Response
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class PayloadValidatorsTest {

    // ---- isNumber ----

    @Test
    fun `isNumber returns true for Int`() {
        assertTrue(PayloadValidators.isNumber(42))
    }

    @Test
    fun `isNumber returns true for Long`() {
        assertTrue(PayloadValidators.isNumber(42L))
    }

    @Test
    fun `isNumber returns true for Double`() {
        assertTrue(PayloadValidators.isNumber(3.14))
    }

    @Test
    fun `isNumber returns false for String`() {
        assertFalse(PayloadValidators.isNumber("42"))
    }

    @Test
    fun `isNumber returns false for null`() {
        assertFalse(PayloadValidators.isNumber(null))
    }

    // ---- isString ----

    @Test
    fun `isString returns true for String`() {
        assertTrue(PayloadValidators.isString("hello"))
    }

    @Test
    fun `isString returns false for Int`() {
        assertFalse(PayloadValidators.isString(42))
    }

    @Test
    fun `isString returns false for null`() {
        assertFalse(PayloadValidators.isString(null))
    }

    // ---- isNonEmptyString ----

    @Test
    fun `isNonEmptyString returns true for non-empty string`() {
        assertTrue(PayloadValidators.isNonEmptyString("hello"))
    }

    @Test
    fun `isNonEmptyString returns false for empty string`() {
        assertFalse(PayloadValidators.isNonEmptyString(""))
    }

    @Test
    fun `isNonEmptyString returns false for null`() {
        assertFalse(PayloadValidators.isNonEmptyString(null))
    }

    // ---- isValidOneOf ----

    @Test
    fun `isValidOneOf returns true when value is in set`() {
        assertTrue(PayloadValidators.isValidOneOf("Hard", setOf("Hard", "Soft")))
    }

    @Test
    fun `isValidOneOf returns false when value not in set`() {
        assertFalse(PayloadValidators.isValidOneOf("Invalid", setOf("Hard", "Soft")))
    }

    @Test
    fun `isValidOneOf returns false for null`() {
        assertFalse(PayloadValidators.isValidOneOf(null, setOf("Hard", "Soft")))
    }

    // ---- isMap ----

    @Test
    fun `isMap returns true for Map`() {
        assertTrue(PayloadValidators.isMap(mapOf("a" to 1)))
    }

    @Test
    fun `isMap returns false for String`() {
        assertFalse(PayloadValidators.isMap("not a map"))
    }

    @Test
    fun `isMap returns false for null`() {
        assertFalse(PayloadValidators.isMap(null))
    }

    // ---- isCallResult ----

    @Test
    fun `isCallResult returns true for CallResult`() {
        val msg = OcppMessage.CallResult("id", mapOf())
        assertTrue(PayloadValidators.isCallResult(msg))
    }

    @Test
    fun `isCallResult returns false for CallError`() {
        val msg = OcppMessage.CallError("id", OcppErrorCode.PROTOCOL_ERROR, "err", null)
        assertFalse(PayloadValidators.isCallResult(msg))
    }

    @Test
    fun `isCallResult returns false for Call`() {
        val msg = OcppMessage.Call("id", "action", mapOf())
        assertFalse(PayloadValidators.isCallResult(msg))
    }

    // ---- buildAcceptedResponse ----

    @Test
    fun `buildAcceptedResponse returns ACCEPTED status`() {
        val response = PayloadValidators.buildAcceptedResponse("test-command")

        assertEquals(Response.Status.ACCEPTED.statusCode, response.status)
        val entity = response.entity as Map<String, Any>
        assertEquals("sent", entity["status"])
        assertEquals("test-command", entity["command"])
    }

    @Test
    fun `buildAcceptedResponse includes extra fields`() {
        val response = PayloadValidators.buildAcceptedResponse("test-command", mapOf("type" to "Hard"))

        assertEquals(Response.Status.ACCEPTED.statusCode, response.status)
        val entity = response.entity as Map<String, Any>
        assertEquals("sent", entity["status"])
        assertEquals("test-command", entity["command"])
        assertEquals("Hard", entity["type"])
    }

    // ---- buildRejectedResponse ----

    @Test
    fun `buildRejectedResponse returns BAD_GATEWAY status`() {
        val response = PayloadValidators.buildRejectedResponse()

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, response.status)
        val entity = response.entity as Map<String, Any>
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    // ---- buildCommandResponse ----

    @Test
    fun `buildCommandResponse delegates to buildAcceptedResponse for CallResult`() {
        val msg = OcppMessage.CallResult("id", mapOf())
        val response = PayloadValidators.buildCommandResponse(msg, "test-command")

        assertEquals(Response.Status.ACCEPTED.statusCode, response.status)
        val entity = response.entity as Map<String, Any>
        assertEquals("sent", entity["status"])
        assertEquals("test-command", entity["command"])
    }

    @Test
    fun `buildCommandResponse delegates to buildRejectedResponse for CallError`() {
        val msg = OcppMessage.CallError("id", OcppErrorCode.PROTOCOL_ERROR, "err", null)
        val response = PayloadValidators.buildCommandResponse(msg, "test-command")

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, response.status)
        val entity = response.entity as Map<String, Any>
        assertEquals("rejected", entity["status"])
    }

    @Test
    fun `buildCommandResponse delegates to buildRejectedResponse for Call`() {
        val msg = OcppMessage.Call("id", "action", mapOf())
        val response = PayloadValidators.buildCommandResponse(msg, "test-command")

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, response.status)
    }

    @Test
    fun `buildCommandResponse includes extra fields`() {
        val msg = OcppMessage.CallResult("id", mapOf())
        val response = PayloadValidators.buildCommandResponse(msg, "reset", mapOf("type" to "Hard"))

        assertEquals(Response.Status.ACCEPTED.statusCode, response.status)
        val entity = response.entity as Map<String, Any>
        assertEquals("sent", entity["status"])
        assertEquals("reset", entity["command"])
        assertEquals("Hard", entity["type"])
    }

    // ---- awaitAndBuildResponse ----

    @Test
    fun `awaitAndBuildResponse returns ACCEPTED for completed CallResult`() {
        val future: CompletableFuture<OcppMessage> = CompletableFuture.completedFuture(
            OcppMessage.CallResult("id", mapOf())
        )
        val response = PayloadValidators.awaitAndBuildResponse(future, "test-command")

        assertEquals(Response.Status.ACCEPTED.statusCode, response.status)
        val entity = response.entity as Map<String, Any>
        assertEquals("sent", entity["status"])
        assertEquals("test-command", entity["command"])
    }

    @Test
    fun `awaitAndBuildResponse returns BAD_GATEWAY for completed CallError`() {
        val future: CompletableFuture<OcppMessage> = CompletableFuture.completedFuture(
            OcppMessage.CallError("id", OcppErrorCode.PROTOCOL_ERROR, "err", null)
        )
        val response = PayloadValidators.awaitAndBuildResponse(future, "test-command")

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, response.status)
        val entity = response.entity as Map<String, Any>
        assertEquals("rejected", entity["status"])
        assertEquals("ChargePoint rejected command", entity["error"])
    }

    @Test
    fun `awaitAndBuildResponse includes extra fields on success`() {
        val future: CompletableFuture<OcppMessage> = CompletableFuture.completedFuture(
            OcppMessage.CallResult("id", mapOf())
        )
        val response = PayloadValidators.awaitAndBuildResponse(future, "reset", mapOf("type" to "Hard"))

        assertEquals(Response.Status.ACCEPTED.statusCode, response.status)
        val entity = response.entity as Map<String, Any>
        assertEquals("sent", entity["status"])
        assertEquals("reset", entity["command"])
        assertEquals("Hard", entity["type"])
    }

    @Test
    fun `awaitAndBuildResponse returns BAD_GATEWAY for completed Call`() {
        val future: CompletableFuture<OcppMessage> = CompletableFuture.completedFuture(
            OcppMessage.Call("id", "action", mapOf())
        )
        val response = PayloadValidators.awaitAndBuildResponse(future, "test-command")

        assertEquals(Response.Status.BAD_GATEWAY.statusCode, response.status)
        val entity = response.entity as Map<String, Any>
        assertEquals("rejected", entity["status"])
    }

    @Test
    fun `awaitAndBuildResponse propagates ExecutionException`() {
        val future = CompletableFuture<OcppMessage>()
        future.completeExceptionally(RuntimeException("simulated failure"))

        val ex = assertThrows(ExecutionException::class.java) {
            PayloadValidators.awaitAndBuildResponse(future, "test-command")
        }
        assertEquals("simulated failure", ex.cause?.message)
    }

    @Test
    fun `awaitAndBuildResponse waits for async completion`() {
        val deferred = CompletableFuture<OcppMessage>()
        val result = object : Thread() {
            lateinit var response: jakarta.ws.rs.core.Response
            override fun run() {
                response = PayloadValidators.awaitAndBuildResponse(deferred, "test-command")
            }
        }.also { it.start() }

        Thread.sleep(50)
        deferred.complete(OcppMessage.CallResult("id", mapOf()))
        result.join(2000)

        assertEquals(Response.Status.ACCEPTED.statusCode, result.response.status)
    }
}

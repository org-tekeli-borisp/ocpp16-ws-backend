package org.tekeli.borisp.ocpp16.websocket

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.OcppActionHandler
import org.tekeli.borisp.ocpp16.handler.OcppHandlerContext
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.protocol.SchemaValidator

class MessageDispatcherCoverageTest {

    private fun createTestContext(): OcppHandlerContext = object : OcppHandlerContext {
        override val sessionId: String = "test-session"
        override val chargePointId: String = "test-cp"
        override var chargePointRegistry: ChargePointRegistry? = null
        override var persistenceService: org.tekeli.borisp.ocpp16.persistence.PersistenceService? = null
        override var metricsService: MetricsService? = null
    }

    @Test
    fun `handleCall with schema validator returning empty errors delegates to handler`() {
        val testHandler = object : OcppActionHandler {
            override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
                return """["3","${call.messageId}",{"status":"Accepted"}]"""
            }
        }
        val mockSchemaValidator = object : SchemaValidator() {
            override fun validate(actionName: String, payloadJson: String): List<String> {
                return emptyList()
            }
        }
        val dispatcher = MessageDispatcher(
            mapOf("TestAction" to testHandler),
            null,
            mockSchemaValidator
        )
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-1","TestAction",{"key":"value"}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        val parsed = OcppMessage.parse(response) as OcppMessage.CallResult
        assertEquals("msg-1", parsed.messageId)
        assertTrue((parsed.payload as Map<*, *>)["status"] == "Accepted")
    }

    @Test
    fun `handleCall catches Throwable when response JSON is invalid`() {
        val invalidJsonHandler = object : OcppActionHandler {
            override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
                return "not-valid-json-{bad"
            }
        }
        val dispatcher = MessageDispatcher(
            mapOf("InvalidAction" to invalidJsonHandler),
            null,
            null
        )
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-1","InvalidAction",{}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        assertEquals("not-valid-json-{bad", response)
    }
}

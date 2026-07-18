package org.tekeli.borisp.ocpp16.websocket

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.OcppActionHandler
import org.tekeli.borisp.ocpp16.handler.OcppHandlerContext
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter

class MessageDispatcherErrorCodesTest {

    private fun createTestContext(): OcppHandlerContext = object : OcppHandlerContext {
        override val sessionId: String = "test-session"
        override val chargePointId: String = "test-cp"
        override var chargePointRegistry: ChargePointRegistry? = null
        override var persistenceService: org.tekeli.borisp.ocpp16.persistence.PersistenceService? = null
        override var metricsService: MetricsService? = null
        override val heartbeatIntervalSeconds: Long = 300
    }

    @Test
    fun `should return InternalError when handler throws unexpected exception`() {
        val exceptionHandler = object : OcppActionHandler {
            override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
                throw RuntimeException("Unexpected failure")
            }
        }
        val dispatcher = MessageDispatcher(mapOf("FailingAction" to exceptionHandler))
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-1","FailingAction",{"key":"value"}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        val parsed = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals(OcppErrorCode.INTERNAL_ERROR, parsed.errorCode)
        assertEquals("msg-1", parsed.messageId)
        assertTrue(parsed.errorDescription.contains("Unexpected failure"))
    }

    @Test
    fun `should return InternalError for NullPointerException in handler`() {
        val exceptionHandler = object : OcppActionHandler {
            override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
                throw NullPointerException("Null reference")
            }
        }
        val dispatcher = MessageDispatcher(mapOf("NullAction" to exceptionHandler))
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-2","NullAction",{}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        val parsed = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals(OcppErrorCode.INTERNAL_ERROR, parsed.errorCode)
    }

    @Test
    fun `should return InternalError for IllegalStateException in handler`() {
        val exceptionHandler = object : OcppActionHandler {
            override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
                throw IllegalStateException("Invalid state")
            }
        }
        val dispatcher = MessageDispatcher(mapOf("StateAction" to exceptionHandler))
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-3","StateAction",{}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        val parsed = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals(OcppErrorCode.INTERNAL_ERROR, parsed.errorCode)
    }

    @Test
    fun `should preserve original messageId in InternalError response`() {
        val exceptionHandler = object : OcppActionHandler {
            override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
                throw RuntimeException("Error")
            }
        }
        val dispatcher = MessageDispatcher(mapOf("ErrorAction" to exceptionHandler))
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","custom-message-id-123","ErrorAction",{}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        val parsed = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals("custom-message-id-123", parsed.messageId)
    }

    @Test
    fun `should return valid CALLERROR structure for InternalError`() {
        val exceptionHandler = object : OcppActionHandler {
            override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
                throw RuntimeException("Error")
            }
        }
        val dispatcher = MessageDispatcher(mapOf("ErrorAction" to exceptionHandler))
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-4","ErrorAction",{}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        assertTrue(response.startsWith("[4,"))
        assertTrue(response.endsWith("]"))
        assertTrue(response.contains("InternalError"))
    }
}

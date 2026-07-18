package org.tekeli.borisp.ocpp16.websocket

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.OcppActionHandler
import org.tekeli.borisp.ocpp16.handler.OcppHandlerContext
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.protocol.MessageCaptureService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppMessageDirection
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import java.util.concurrent.CompletableFuture

class MessageDispatcherTest {

    private val testHandler = TestHandler()
    private val handlers = mapOf("TestAction" to testHandler)
    private var dispatchedContext: OcppHandlerContext? = null

    @Test
    fun `dispatch CALL to registered handler`() {
        val dispatcher = MessageDispatcher(handlers)
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-1","TestAction",{"key":"value"}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        assertEquals("msg-1", testHandler.lastCall?.messageId)
        assertTrue(response.startsWith("["))
    }

    @Test
    fun `dispatch CALL to unregistered handler returns NOT_IMPLEMENTED`() {
        val dispatcher = MessageDispatcher(handlers)
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-1","UnknownAction",{}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        val parsed = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals(OcppErrorCode.NOT_IMPLEMENTED, parsed.errorCode)
    }

    @Test
    fun `dispatch CALLRESULT resolves awaiter`() {
        val dispatcher = MessageDispatcher(emptyMap())
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val future = awaiter.pending("msg-1")
        future.complete(OcppMessage.CallResult("msg-1", mapOf()))

        val message = """["3","msg-1",{"status":"Accepted"}]"""
        val response = dispatcher.dispatch(message, context, awaiter, null)

        assertEquals("", response)
    }

    @Test
    fun `dispatch CALLRESULT for unknown messageId returns protocol error`() {
        val dispatcher = MessageDispatcher(emptyMap())
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["3","unknown-id",{"status":"Accepted"}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        val parsed = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals(OcppErrorCode.PROTOCOL_ERROR, parsed.errorCode)
        assertTrue(parsed.errorDescription.contains("CALLRESULT"))
    }

    @Test
    fun `dispatch CALLERROR resolves awaiter rejection`() {
        val dispatcher = MessageDispatcher(emptyMap())
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val future = awaiter.pending("msg-1")
        future.complete(OcppMessage.CallError("msg-1", OcppErrorCode.PROTOCOL_ERROR, "err", null))

        val message = """["4","msg-1","ProtocolError","err",null]"""
        val response = dispatcher.dispatch(message, context, awaiter, null)

        assertEquals("", response)
    }

    @Test
    fun `dispatch CALLERROR for unknown messageId returns protocol error`() {
        val dispatcher = MessageDispatcher(emptyMap())
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["4","unknown-id","ProtocolError","err",null]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        val parsed = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals(OcppErrorCode.PROTOCOL_ERROR, parsed.errorCode)
        assertTrue(parsed.errorDescription.contains("CALLERROR"))
    }

    @Test
    fun `dispatch parse error returns protocol error`() {
        val dispatcher = MessageDispatcher(emptyMap())
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """invalid json"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        val parsed = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals(OcppErrorCode.PROTOCOL_ERROR, parsed.errorCode)
    }

    @Test
    fun `handleCall captures outbound CALLRESULT via MessageCaptureService`() {
        val capturedMessages = mutableListOf<Pair<OcppMessageDirection, OcppMessage>>()
        val mockCaptureService = object : MessageCaptureService() {
            override fun capture(chargePointId: String, direction: OcppMessageDirection, ocppMessage: OcppMessage) {
                super.capture(chargePointId, direction, ocppMessage)
                capturedMessages.add(Pair(direction, ocppMessage))
            }
        }
        val dispatcher = MessageDispatcher(handlers, mockCaptureService)
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-1","TestAction",{"key":"value"}]"""

        dispatcher.dispatch(message, context, awaiter, null)

        assertEquals(2, capturedMessages.size)
        assertEquals(OcppMessageDirection.INBOUND, capturedMessages[0].first)
        assertTrue(capturedMessages[0].second is OcppMessage.Call)
        assertEquals(OcppMessageDirection.OUTBOUND, capturedMessages[1].first)
        assertTrue(capturedMessages[1].second is OcppMessage.CallResult)
    }

    @Test
    fun `handleCall captures outbound CALLERROR for NOT_IMPLEMENTED via MessageCaptureService`() {
        val capturedMessages = mutableListOf<Pair<OcppMessageDirection, OcppMessage>>()
        val mockCaptureService = object : MessageCaptureService() {
            override fun capture(chargePointId: String, direction: OcppMessageDirection, ocppMessage: OcppMessage) {
                super.capture(chargePointId, direction, ocppMessage)
                capturedMessages.add(Pair(direction, ocppMessage))
            }
        }
        val dispatcher = MessageDispatcher(handlers, mockCaptureService)
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-1","UnknownAction",{}]"""

        dispatcher.dispatch(message, context, awaiter, null)

        assertEquals(2, capturedMessages.size)
        assertEquals(OcppMessageDirection.INBOUND, capturedMessages[0].first)
        assertEquals(OcppMessageDirection.OUTBOUND, capturedMessages[1].first)
        val callError = capturedMessages[1].second as OcppMessage.CallError
        assertEquals(OcppErrorCode.NOT_IMPLEMENTED, callError.errorCode)
    }

    @Test
    fun `handleCall captures outbound CALLERROR for FORMATION_VIOLATION via MessageCaptureService`() {
        val violationHandler = object : OcppActionHandler {
            override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
                throw org.tekeli.borisp.ocpp16.protocol.FormationViolationException("Invalid payload")
            }
        }
        val capturedMessages = mutableListOf<Pair<OcppMessageDirection, OcppMessage>>()
        val mockCaptureService = object : MessageCaptureService() {
            override fun capture(chargePointId: String, direction: OcppMessageDirection, ocppMessage: OcppMessage) {
                super.capture(chargePointId, direction, ocppMessage)
                capturedMessages.add(Pair(direction, ocppMessage))
            }
        }
        val dispatcher = MessageDispatcher(mapOf("ViolationAction" to violationHandler), mockCaptureService)
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-1","ViolationAction",{}]"""

        dispatcher.dispatch(message, context, awaiter, null)

        assertEquals(2, capturedMessages.size)
        assertEquals(OcppMessageDirection.INBOUND, capturedMessages[0].first)
        assertEquals(OcppMessageDirection.OUTBOUND, capturedMessages[1].first)
        val callError = capturedMessages[1].second as OcppMessage.CallError
        assertEquals(OcppErrorCode.FORMATION_VIOLATION, callError.errorCode)
    }

    private fun createTestContext(): OcppHandlerContext = object : OcppHandlerContext {
        override val sessionId: String = "test-session"
        override val chargePointId: String = "test-cp"
        override var chargePointRegistry: ChargePointRegistry? = null
        override var persistenceService: org.tekeli.borisp.ocpp16.persistence.PersistenceService? = null
        override var metricsService: MetricsService? = null
        override val heartbeatIntervalSeconds: Long = 300
    }

    private class TestHandler : OcppActionHandler {
        var lastCall: OcppMessage.Call? = null
        override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
            lastCall = call
            return """["3","${call.messageId}",{"status":"Accepted"}]"""
        }
    }
}

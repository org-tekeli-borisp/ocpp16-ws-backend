package org.tekeli.borisp.ocpp16.websocket

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.objenesis.ObjenesisStd
import org.tekeli.borisp.ocpp16.handler.OcppActionHandler
import org.tekeli.borisp.ocpp16.handler.OcppHandlerContext
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.MessageCaptureService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppMessageDirection
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import org.tekeli.borisp.ocpp16.protocol.OcppParseException
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter

class MessageDispatcherNullMessageTest {

    private fun createTestContext(): OcppHandlerContext = object : OcppHandlerContext {
        override val sessionId: String = "test-session"
        override val chargePointId: String = "test-cp"
        override var chargePointRegistry: ChargePointRegistry? = null
        override var persistenceService: PersistenceService? = null
        override var metricsService: MetricsService? = null
        override val heartbeatIntervalSeconds: Long = 300
    }

    @Test
    fun `dispatch uses default parse error description when parse exception has null message`() {
        val nullMessageException = ObjenesisStd().newInstance(OcppParseException::class.java)
        val throwingCaptureService = object : MessageCaptureService() {
            override fun capture(chargePointId: String, direction: OcppMessageDirection, ocppMessage: OcppMessage) {
                throw nullMessageException
            }
        }
        val dispatcher = MessageDispatcher(emptyMap(), throwingCaptureService, null)
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-null","Heartbeat",{}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        val parsed = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals(OcppErrorCode.PROTOCOL_ERROR, parsed.errorCode)
        assertEquals("Parse error", parsed.errorDescription)
    }

    @Test
    fun `handleError uses default formation violation description when exception has null message`() {
        val nullMessageViolation = ObjenesisStd().newInstance(FormationViolationException::class.java)
        val throwingHandler = object : OcppActionHandler {
            override fun handle(call: OcppMessage.Call, context: OcppHandlerContext): String {
                throw nullMessageViolation
            }
        }
        val dispatcher = MessageDispatcher(mapOf("NullViolation" to throwingHandler), null, null)
        val context = createTestContext()
        val awaiter = ResponseAwaiter()
        val message = """["2","msg-null-2","NullViolation",{}]"""

        val response = dispatcher.dispatch(message, context, awaiter, null)

        val parsed = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals(OcppErrorCode.FORMATION_VIOLATION, parsed.errorCode)
        assertEquals("Payload validation failed", parsed.errorDescription)
    }
}

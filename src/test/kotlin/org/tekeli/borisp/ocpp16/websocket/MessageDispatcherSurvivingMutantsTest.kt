package org.tekeli.borisp.ocpp16.websocket

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.BootNotificationHandler
import org.tekeli.borisp.ocpp16.handler.OcppActionHandler
import org.tekeli.borisp.ocpp16.handler.OcppHandlerContext
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.OcppErrorCode
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.protocol.SchemaValidator

private const val INVALID_MESSAGE_TYPE = """[99,"msg-1"]"""
private const val BOOT_NOTIFICATION_MISSING_REQUIRED = """[2,"msg-1","BootNotification",{}]"""

class MessageDispatcherSurvivingMutantsTest {

    private val handlers = mapOf<String, OcppActionHandler>(
        "BootNotification" to BootNotificationHandler()
    )

    private val context = object : OcppHandlerContext {
        override val chargePointId: String = "CP-TEST"
        override val sessionId: String = "sess-1"
        override val chargePointRegistry: Nothing? = null
        override val persistenceService: Nothing? = null
        override val metricsService: Nothing? = null
        override val heartbeatIntervalSeconds: Long = 300
    }

    @Test
    fun `dispatch parse error preserves specific exception message`() {
        val dispatcher = MessageDispatcher(handlers)

        val response = dispatcher.dispatch(INVALID_MESSAGE_TYPE, context, ResponseAwaiter(), null)

        val callError = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals(OcppErrorCode.PROTOCOL_ERROR, callError.errorCode)
        assertNotEquals("Parse error", callError.errorDescription)
        assertTrue(callError.errorDescription.contains("Invalid message type: 99"))
    }

    @Test
    fun `dispatch schema validation error joins multiple errors with separator`() {
        val dispatcher = MessageDispatcher(handlers, null, SchemaValidator())

        val response = dispatcher.dispatch(BOOT_NOTIFICATION_MISSING_REQUIRED, context, ResponseAwaiter(), null)

        val callError = OcppMessage.parse(response) as OcppMessage.CallError
        assertEquals(OcppErrorCode.FORMATION_VIOLATION, callError.errorCode)
        val detail = callError.errorDescription.removePrefix("Schema validation failed: ")
        assertTrue(detail.contains("chargePointVendor"))
        assertTrue(detail.contains("chargePointModel"))
        assertTrue(detail.contains("; "))
    }
}

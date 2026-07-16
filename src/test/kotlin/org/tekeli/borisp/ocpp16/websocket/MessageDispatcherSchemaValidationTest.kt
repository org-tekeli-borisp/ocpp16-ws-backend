package org.tekeli.borisp.ocpp16.websocket

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.handler.BootNotificationHandler
import org.tekeli.borisp.ocpp16.handler.OcppActionHandler
import org.tekeli.borisp.ocpp16.handler.OcppHandlerContext
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.protocol.SchemaValidator

class MessageDispatcherSchemaValidationTest {

    private val mockContext = object : OcppHandlerContext {
        override val chargePointId: String = "CP-TEST"
        override val sessionId: String = "sess-1"
        override val chargePointRegistry: Nothing? = null
        override val persistenceService: Nothing? = null
        override val metricsService: Nothing? = null
    }

    private val handlers = mapOf<String, OcppActionHandler>(
        "BootNotification" to BootNotificationHandler(),
        "Heartbeat" to org.tekeli.borisp.ocpp16.handler.HeartbeatHandler()
    )

    private fun createDispatcher(schemaValidator: SchemaValidator?): MessageDispatcher {
        return MessageDispatcher(handlers, null, schemaValidator)
    }

    @Test
    fun `dispatcher without schema validator delegates to handler`() {
        val dispatcher = createDispatcher(null)
        val response = dispatcher.dispatch(
            """[2,"1","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]""",
            mockContext,
            ResponseAwaiter(),
            null
        )
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `dispatcher with schema validator rejects additional properties`() {
        val dispatcher = createDispatcher(SchemaValidator())
        val response = dispatcher.dispatch(
            """[2,"1","BootNotification",{"chargePointVendor":"V","chargePointModel":"M","extraField":true}]""",
            mockContext,
            ResponseAwaiter(),
            null
        )
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `dispatcher with schema validator rejects missing required field`() {
        val dispatcher = createDispatcher(SchemaValidator())
        val response = dispatcher.dispatch(
            """[2,"1","BootNotification",{"chargePointVendor":"V"}]""",
            mockContext,
            ResponseAwaiter(),
            null
        )
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `dispatcher with schema validator accepts valid payload`() {
        val dispatcher = createDispatcher(SchemaValidator())
        val response = dispatcher.dispatch(
            """[2,"1","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]""",
            mockContext,
            ResponseAwaiter(),
            null
        )
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `dispatcher with schema validator rejects maxLength violation`() {
        val dispatcher = createDispatcher(SchemaValidator())
        val longVendor = "A".repeat(21)
        val response = dispatcher.dispatch(
            """[2,"1","BootNotification",{"chargePointVendor":"$longVendor","chargePointModel":"M"}]""",
            mockContext,
            ResponseAwaiter(),
            null
        )
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
    }

    @Test
    fun `dispatcher without schema validator passes additional properties to handler`() {
        val dispatcher = createDispatcher(null)
        val response = dispatcher.dispatch(
            """[2,"1","BootNotification",{"chargePointVendor":"V","chargePointModel":"M","extra":1}]""",
            mockContext,
            ResponseAwaiter(),
            null
        )
        assertTrue(response.startsWith("[3,"))
    }

    @Test
    fun `dispatcher with schema validator rejects unknown action silently`() {
        val dispatcher = createDispatcher(SchemaValidator())
        val response = dispatcher.dispatch(
            """[2,"1","UnknownAction",{"extra":"value"}]""",
            mockContext,
            ResponseAwaiter(),
            null
        )
        assertTrue(response.contains("NotImplemented"))
    }

    @Test
    fun `dispatcher with schema validator still delegates handler errors`() {
        val dispatcher = createDispatcher(SchemaValidator())
        val response = dispatcher.dispatch(
            """[2,"1","BootNotification",{"chargePointVendor":"","chargePointModel":"M"}]""",
            mockContext,
            ResponseAwaiter(),
            null
        )
        assertTrue(response.startsWith("[4,"))
        assertTrue(response.contains("FormationViolation"))
    }
}

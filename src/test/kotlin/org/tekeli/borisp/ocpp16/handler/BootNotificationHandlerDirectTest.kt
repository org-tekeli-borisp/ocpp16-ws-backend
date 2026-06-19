package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import org.tekeli.borisp.ocpp16.protocol.ResponseAwaiter
import org.tekeli.borisp.ocpp16.websocket.ChargePointConnection
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class BootNotificationHandlerDirectTest {

    private val handler = BootNotificationHandler()

    // -- vendor (chargePointVendor) tests --

    @Test
    fun `validatePayload extracts valid vendor`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("VendorA", parsed.vendor)
    }

    @Test
    fun `validatePayload throws for missing vendor`() {
        val payload = mapOf<String, Any>(
            "chargePointModel" to "ModelX"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointVendor"))
    }

    @Test
    fun `validatePayload throws for empty vendor`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "",
            "chargePointModel" to "ModelX"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointVendor"))
    }

    @Test
    fun `validatePayload throws for whitespace vendor`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "   ",
            "chargePointModel" to "ModelX"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointVendor"))
    }

    @Test
    fun `validatePayload throws for vendor exceeding 20 chars`() {
        val longVendor = "A".repeat(21)
        val payload = mapOf<String, Any>(
            "chargePointVendor" to longVendor,
            "chargePointModel" to "ModelX"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("20 characters"))
    }

    @Test
    fun `validatePayload accepts vendor exactly 20 chars`() {
        val maxVendor = "A".repeat(20)
        val payload = mapOf<String, Any>(
            "chargePointVendor" to maxVendor,
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(maxVendor, parsed.vendor)
    }

    // -- model (chargePointModel) tests --

    @Test
    fun `validatePayload extracts valid model`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("ModelX", parsed.model)
    }

    @Test
    fun `validatePayload throws for missing model`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA"
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointModel"))
    }

    @Test
    fun `validatePayload throws for empty model`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to ""
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointModel"))
    }

    @Test
    fun `validatePayload throws for whitespace model`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "   "
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("chargePointModel"))
    }

    @Test
    fun `validatePayload throws for model exceeding 20 chars`() {
        val longModel = "B".repeat(21)
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to longModel
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            handler.validatePayload(payload)
        }
        assertTrue(ex.message!!.contains("20 characters"))
    }

    @Test
    fun `validatePayload accepts model exactly 20 chars`() {
        val maxModel = "B".repeat(20)
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to maxModel
        )
        val parsed = handler.validatePayload(payload)

        assertEquals(maxModel, parsed.model)
    }

    // -- firmwareVersion tests --

    @Test
    fun `validatePayload extracts present firmwareVersion`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX",
            "firmwareVersion" to "1.2.3"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("1.2.3", parsed.firmwareVersion)
    }

    @Test
    fun `validatePayload returns null firmwareVersion when missing`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.firmwareVersion)
    }

    @Test
    fun `validatePayload treats absent firmwareVersion as null`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertNull(parsed.firmwareVersion)
        assertNull(payload["firmwareVersion"])
    }

    // -- requiredString extension direct tests --

    @Test
    fun `requiredString returns valid value`() {
        val payload = mapOf<String, Any>(
            "fieldName" to "someValue"
        )

        val result = payload.requiredString("fieldName", 50)

        assertEquals("someValue", result)
    }

    @Test
    fun `requiredString throws for missing field`() {
        val payload = mapOf<String, Any>()

        val ex = assertThrows(FormationViolationException::class.java) {
            payload.requiredString("fieldName", 50)
        }
        assertTrue(ex.message!!.contains("fieldName"))
    }

    @Test
    fun `requiredString throws for empty value`() {
        val payload = mapOf<String, Any>(
            "fieldName" to ""
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            payload.requiredString("fieldName", 50)
        }
        assertTrue(ex.message!!.contains("fieldName"))
    }

    @Test
    fun `requiredString throws for whitespace value`() {
        val payload = mapOf<String, Any>(
            "fieldName" to "  "
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            payload.requiredString("fieldName", 50)
        }
        assertTrue(ex.message!!.contains("fieldName"))
    }

    @Test
    fun `requiredString throws when value exceeds maxLength`() {
        val payload = mapOf<String, Any>(
            "fieldName" to "A".repeat(11)
        )

        val ex = assertThrows(FormationViolationException::class.java) {
            payload.requiredString("fieldName", 10)
        }
        assertTrue(ex.message!!.contains("10 characters"))
    }

    @Test
    fun `requiredString accepts value exactly at maxLength`() {
        val payload = mapOf<String, Any>(
            "fieldName" to "A".repeat(10)
        )

        val result = payload.requiredString("fieldName", 10)

        assertEquals("A".repeat(10), result)
    }

    // -- full validation tests --

    @Test
    fun `validatePayload extracts all fields correctly`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX",
            "firmwareVersion" to "2.0.0"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("VendorA", parsed.vendor)
        assertEquals("ModelX", parsed.model)
        assertEquals("2.0.0", parsed.firmwareVersion)
    }

    @Test
    fun `validatePayload extracts all fields with optional firmwareVersion missing`() {
        val payload = mapOf<String, Any>(
            "chargePointVendor" to "VendorA",
            "chargePointModel" to "ModelX"
        )
        val parsed = handler.validatePayload(payload)

        assertEquals("VendorA", parsed.vendor)
        assertEquals("ModelX", parsed.model)
        assertNull(parsed.firmwareVersion)
    }

    // =====================================================
    // Tracking inline stubs for processBootNotification tests
    // =====================================================

    private class TrackingRegistry : ChargePointRegistry() {
        var updateCalled = false
        var updateSessionId: String? = null
        var updateChargePointId: String? = null
        var updateVendor: String? = null
        var updateModel: String? = null

        override fun updateChargePointInfo(
            sessionId: String,
            chargePointId: String,
            vendor: String,
            model: String
        ) {
            updateCalled = true
            updateSessionId = sessionId
            updateChargePointId = chargePointId
            updateVendor = vendor
            updateModel = model
        }

        // Prevent session-not-found errors from real parent class
        init {
            // Register a default session so parent's updateChargePointInfo doesn't throw
            // We override it anyway, but defensive
        }
    }

    private class TrackingPersistence : PersistenceService() {
        var upsertCalled = false
        var upsertSessionId: String? = null
        var upsertChargePointId: String? = null
        var upsertVendor: String? = null
        var upsertModel: String? = null
        var upsertFirmwareVersion: String? = null

        override fun upsertChargePoint(
            sessionId: String,
            chargePointId: String,
            vendor: String,
            model: String,
            firmwareVersion: String?
        ) {
            upsertCalled = true
            upsertSessionId = sessionId
            upsertChargePointId = chargePointId
            upsertVendor = vendor
            upsertModel = model
            upsertFirmwareVersion = firmwareVersion
        }
    }

    // =====================================================
    // processBootNotification direct tests - kills mutants 4-7
    // M4: removed call to getChargePointRegistry
    // M5: removed call to getPersistenceService
    // M6: chargePointRegistry != null -> false (skips registry call)
    // M7: persistenceService != null -> false (skips persistence call)
    // =====================================================

    @Test
    fun `processBootNotification calls registry with exact vendor`() {
        val registry = TrackingRegistry()
        val persistence = TrackingPersistence()
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = registry
            override var persistenceService: PersistenceService? = persistence
        }.apply {
            sessionId = "sess-pbn-vendor"
        }

        handler.processBootNotification(
            context = server,
            chargePointId = "CP-VENDOR",
            vendor = "ExactVendor",
            model = "ModelX",
            firmwareVersion = "1.0"
        )

        assertTrue(registry.updateCalled, "updateChargePointInfo must be called")
        assertEquals("ExactVendor", registry.updateVendor, "vendor must match exactly")
    }

    @Test
    fun `processBootNotification calls registry with exact model`() {
        val registry = TrackingRegistry()
        val persistence = TrackingPersistence()
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = registry
            override var persistenceService: PersistenceService? = persistence
        }.apply {
            sessionId = "sess-pbn-model"
        }

        handler.processBootNotification(
            context = server,
            chargePointId = "CP-MODEL",
            vendor = "VendorA",
            model = "ExactModel",
            firmwareVersion = "2.0"
        )

        assertTrue(registry.updateCalled, "updateChargePointInfo must be called")
        assertEquals("ExactModel", registry.updateModel, "model must match exactly")
    }

    @Test
    fun `processBootNotification calls persistence with exact firmwareVersion`() {
        val registry = TrackingRegistry()
        val persistence = TrackingPersistence()
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = registry
            override var persistenceService: PersistenceService? = persistence
        }.apply {
            sessionId = "sess-pbn-fw"
        }

        handler.processBootNotification(
            context = server,
            chargePointId = "CP-FW",
            vendor = "VendorA",
            model = "ModelX",
            firmwareVersion = "ExactFW3.0"
        )

        assertTrue(persistence.upsertCalled, "upsertChargePoint must be called")
        assertEquals("ExactFW3.0", persistence.upsertFirmwareVersion, "firmwareVersion must match")
    }

    @Test
    fun `processBootNotification calls both registry and persistence when services present`() {
        val registry = TrackingRegistry()
        val persistence = TrackingPersistence()
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = registry
            override var persistenceService: PersistenceService? = persistence
        }.apply {
            sessionId = "sess-pbn-both"
        }

        handler.processBootNotification(
            context = server,
            chargePointId = "CP-BOTH",
            vendor = "VendorA",
            model = "ModelX",
            firmwareVersion = "1.0"
        )

        assertTrue(registry.updateCalled, "registry must be called when non-null")
        assertTrue(persistence.upsertCalled, "persistence must be called when non-null")
        assertEquals("CP-BOTH", registry.updateChargePointId)
        assertEquals("VendorA", registry.updateVendor)
        assertEquals("ModelX", registry.updateModel)
        assertEquals("CP-BOTH", persistence.upsertChargePointId)
        assertEquals("VendorA", persistence.upsertVendor)
        assertEquals("ModelX", persistence.upsertModel)
        assertEquals("1.0", persistence.upsertFirmwareVersion)
    }

    @Test
    fun `processBootNotification passes sessionId correctly to registry`() {
        val registry = TrackingRegistry()
        val persistence = TrackingPersistence()
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = registry
            override var persistenceService: PersistenceService? = persistence
        }.apply {
            sessionId = "unique-session-id-42"
        }

        handler.processBootNotification(
            context = server,
            chargePointId = "CP-SESSION",
            vendor = "V",
            model = "M",
            firmwareVersion = null
        )

        assertEquals("unique-session-id-42", registry.updateSessionId)
        assertEquals("unique-session-id-42", persistence.upsertSessionId)
    }

    @Test
    fun `processBootNotification passes null firmwareVersion correctly`() {
        val registry = TrackingRegistry()
        val persistence = TrackingPersistence()
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = registry
            override var persistenceService: PersistenceService? = persistence
        }.apply {
            sessionId = "sess-pbn-null-fw"
        }

        handler.processBootNotification(
            context = server,
            chargePointId = "CP-NULL-FW",
            vendor = "VendorA",
            model = "ModelX",
            firmwareVersion = null
        )

        assertTrue(persistence.upsertCalled)
        assertNull(persistence.upsertFirmwareVersion, "null firmware must be passed as null")
    }

    // =====================================================
    // handle() integration tests - kills mutants 1-3 (componentN)
    // M1: removed call to component1 (vendor)
    // M2: removed call to component2 (model)
    // M3: removed call to component3 (firmwareVersion)
    // These mutants cause vendor/model/firmware to get wrong
    // default values, which are then passed to processBootNotification.
    // =====================================================

    @Test
    fun `handle passes vendor from payload through to registry`() {
        val registry = TrackingRegistry()
        val persistence = TrackingPersistence()
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = registry
            override var persistenceService: PersistenceService? = persistence
        }.apply {
            sessionId = "sess-handle-vendor"
            chargePointId = "CP-HANDLE-V"
        }

        val call = org.tekeli.borisp.ocpp16.protocol.OcppMessage.Call(
            messageId = "msg-vendor",
            action = "BootNotification",
            payload = mapOf<String, Any>(
                "chargePointVendor" to "TestVendorX",
                "chargePointModel" to "ModelX",
                "firmwareVersion" to "1.0"
            )
        )

        val response = handler.handle(call, server)

        assertTrue(response.startsWith("[3,"))
        assertTrue(registry.updateCalled)
        assertEquals("TestVendorX", registry.updateVendor, "vendor must flow from payload to registry")
    }

    @Test
    fun `handle passes model from payload through to registry`() {
        val registry = TrackingRegistry()
        val persistence = TrackingPersistence()
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = registry
            override var persistenceService: PersistenceService? = persistence
        }.apply {
            sessionId = "sess-handle-model"
            chargePointId = "CP-HANDLE-M"
        }

        val call = org.tekeli.borisp.ocpp16.protocol.OcppMessage.Call(
            messageId = "msg-model",
            action = "BootNotification",
            payload = mapOf<String, Any>(
                "chargePointVendor" to "VendorA",
                "chargePointModel" to "TestModelY",
                "firmwareVersion" to "1.0"
            )
        )

        val response = handler.handle(call, server)

        assertTrue(response.startsWith("[3,"))
        assertTrue(registry.updateCalled)
        assertEquals("TestModelY", registry.updateModel, "model must flow from payload to registry")
    }

    @Test
    fun `handle passes firmwareVersion from payload through to persistence`() {
        val registry = TrackingRegistry()
        val persistence = TrackingPersistence()
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = registry
            override var persistenceService: PersistenceService? = persistence
        }.apply {
            sessionId = "sess-handle-fw"
            chargePointId = "CP-HANDLE-FW"
        }

        val call = org.tekeli.borisp.ocpp16.protocol.OcppMessage.Call(
            messageId = "msg-fw",
            action = "BootNotification",
            payload = mapOf<String, Any>(
                "chargePointVendor" to "VendorA",
                "chargePointModel" to "ModelX",
                "firmwareVersion" to "TestFW5.0"
            )
        )

        val response = handler.handle(call, server)

        assertTrue(response.startsWith("[3,"))
        assertTrue(persistence.upsertCalled)
        assertEquals("TestFW5.0", persistence.upsertFirmwareVersion, "firmwareVersion must flow from payload to persistence")
    }

    @Test
    fun `handle passes all three fields correctly through full flow`() {
        val registry = TrackingRegistry()
        val persistence = TrackingPersistence()
        val server = object : OcppWebSocketServer() {
            override var chargePointRegistry: ChargePointRegistry? = registry
            override var persistenceService: PersistenceService? = persistence
        }.apply {
            sessionId = "sess-handle-all"
            chargePointId = "CP-HANDLE-ALL"
        }

        val call = org.tekeli.borisp.ocpp16.protocol.OcppMessage.Call(
            messageId = "msg-all",
            action = "BootNotification",
            payload = mapOf<String, Any>(
                "chargePointVendor" to "UniqueVendor",
                "chargePointModel" to "UniqueModel",
                "firmwareVersion" to "UniqueFW"
            )
        )

        val response = handler.handle(call, server)

        assertTrue(response.startsWith("[3,"))
        assertTrue(response.contains("Accepted"))

        // Verify all 3 components destructured correctly
        assertEquals("UniqueVendor", registry.updateVendor)
        assertEquals("UniqueModel", registry.updateModel)
        assertEquals("UniqueVendor", persistence.upsertVendor)
        assertEquals("UniqueModel", persistence.upsertModel)
        assertEquals("UniqueFW", persistence.upsertFirmwareVersion)
    }
}

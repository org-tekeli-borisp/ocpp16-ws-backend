package org.tekeli.borisp.ocpp16.protocol

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SchemaValidatorTest {

    private val validator = SchemaValidator()

    @Test
    fun `validates BootNotification payload passes`() {
        val payload = """{"chargePointVendor":"Tesla","chargePointModel":"Model3"}"""

        val errors = validator.validate("BootNotification", payload)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validates BootNotification missing required field fails`() {
        val payload = """{"chargePointVendor":"Tesla"}"""

        val errors = validator.validate("BootNotification", payload)
        assertFalse(errors.isEmpty())
    }

    @Test
    fun `validates BootNotification wrong type fails`() {
        val payload = """{"chargePointVendor":123,"chargePointModel":"Model3"}"""

        val errors = validator.validate("BootNotification", payload)
        assertFalse(errors.isEmpty())
    }

    @Test
    fun `validates BootNotification additionalProperties fails`() {
        val payload = """{"chargePointVendor":"Tesla","chargePointModel":"Model3","unknownField":true}"""

        val errors = validator.validate("BootNotification", payload)
        assertFalse(errors.isEmpty())
    }

    @Test
    fun `validates BootNotification missing field error message contains field name`() {
        val payload = """{"chargePointVendor":"Tesla"}"""

        val errors = validator.validate("BootNotification", payload)

        assertTrue(errors.any { it.contains("chargePointModel") })
    }

    @Test
    fun `validates unknown action returns no errors`() {
        val payload = """{"anything":true}"""

        val errors = validator.validate("NonExistentAction", payload)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validates Heartbeat empty payload passes`() {
        val payload = """{}"""

        val errors = validator.validate("Heartbeat", payload)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validates StatusNotification payload passes`() {
        val payload = """{"connectorId":1,"errorCode":"NoError","status":"Available"}"""

        val errors = validator.validate("StatusNotification", payload)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validates StatusNotification missing required fields fails`() {
        val payload = """{}"""

        val errors = validator.validate("StatusNotification", payload)
        assertFalse(errors.isEmpty())
    }

    @Test
    fun `validates Reset payload passes`() {
        val payload = """{"type":"Hard"}"""

        val errors = validator.validate("Reset", payload)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validates Reset invalid enum value fails`() {
        val payload = """{"type":"InvalidType"}"""

        val errors = validator.validate("Reset", payload)
        assertFalse(errors.isEmpty())
    }

    @Test
    fun `validates StartTransaction payload passes`() {
        val payload = """{"connectorId":1,"idTag":"CARD123","meterStart":0,"timestamp":"2024-01-01T00:00:00Z"}"""

        val errors = validator.validate("StartTransaction", payload)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validates StopTransaction payload passes`() {
        val payload = """{"meterStop":0,"timestamp":"2024-01-01T00:00:00Z","transactionId":1}"""

        val errors = validator.validate("StopTransaction", payload)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validates StopTransaction missing transactionId fails`() {
        val payload = """{"meterStop":0,"timestamp":"2024-01-01T00:00:00Z"}"""

        val errors = validator.validate("StopTransaction", payload)
        assertFalse(errors.isEmpty())
    }

    @Test
    fun `validates security SecurityEventNotification payload passes`() {
        val payload = """{"type":"SigningCertificateExpired","timestamp":"2024-01-01T00:00:00Z"}"""

        val errors = validator.validate("SecurityEventNotification", payload)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validates security SecurityEventNotification missing required fails`() {
        val payload = """{}"""

        val errors = validator.validate("SecurityEventNotification", payload)
        assertFalse(errors.isEmpty())
    }

    @Test
    fun `validates ChangeConfiguration payload passes`() {
        val payload = """{"key":"Key","value":"Value"}"""

        val errors = validator.validate("ChangeConfiguration", payload)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validates ChangeConfiguration missing key fails`() {
        val payload = """{"attributeType":"Active","value":"Value"}"""

        val errors = validator.validate("ChangeConfiguration", payload)
        assertFalse(errors.isEmpty())
    }

    @Test
    fun `validates SecurityEventNotification additionalProperties fails`() {
        val payload = """{"type":"SigningCertificateExpired","timestamp":"2024-01-01T00:00:00Z","invalid":"x"}"""

        val errors = validator.validate("SecurityEventNotification", payload)
        assertFalse(errors.isEmpty())
    }

    @Test
    fun `validates BootNotification maxLength violation fails`() {
        val payload = """{"chargePointVendor":"this-is-a-very-long-vendor-name-that-exceeds-20-chars","chargePointModel":"M"}"""

        val errors = validator.validate("BootNotification", payload)
        assertFalse(errors.isEmpty())
    }

    @Test
    fun `validates SignedUpdateFirmware payload passes`() {
        val firmwareJson = """
            {
                "requestId": 1,
                "retries": 3,
                "firmware": {
                    "location": "https://example.com/firmware.bin",
                    "retrieveDateTime": "2024-01-01T00:00:00Z",
                    "signingCertificate": "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0example==",
                    "signature": "dGVzdA=="
                }
            }
        """.trimIndent()

        val errors = validator.validate("SignedUpdateFirmware", firmwareJson)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validates SignedUpdateFirmware missing firmware fails`() {
        val payload = """
            {
                "requestId": 1,
                "retries": 3,
                "retrieveDate": "2024-01-01T00:00:00Z"
            }
        """.trimIndent()

        val errors = validator.validate("SignedUpdateFirmware", payload)
        assertFalse(errors.isEmpty())
    }

    @Test
    fun `validates InstallCertificate payload passes`() {
        val payload = """{"certificateType":"CentralSystemRootCertificate","certificate":"MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA0example=="}"""

        val errors = validator.validate("InstallCertificate", payload)
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validates InstallCertificate missing certificate fails`() {
        val payload = """{"certificateType":"CentralSystemRootCertificate"}"""

        val errors = validator.validate("InstallCertificate", payload)
        assertFalse(errors.isEmpty())
    }
}

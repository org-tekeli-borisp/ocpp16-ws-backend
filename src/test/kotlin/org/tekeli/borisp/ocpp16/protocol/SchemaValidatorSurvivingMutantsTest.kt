package org.tekeli.borisp.ocpp16.protocol

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow

class SchemaValidatorSurvivingMutantsTest {

    private val validator = SchemaValidator()

    @Test
    fun `validate unknown action returns empty list without throwing`() {
        val errors = assertDoesNotThrow {
            validator.validate("UnknownAction", "{}")
        }
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `validate unknown action returns exactly empty list`() {
        val errors = validator.validate("UnknownAction", "{}")
        assertEquals(0, errors.size)
    }

    @Test
    fun `validate known action with invalid payload returns non-empty errors`() {
        val errors = validator.validate("ChangeConfiguration", """{"value":"Value"}""")
        assertTrue(errors.isNotEmpty())
    }
}

package org.tekeli.borisp.ocpp16.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.Schema
import com.networknt.schema.SchemaContext
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.Specification
import com.networknt.schema.SpecificationVersion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class SchemaValidatorSurvivingMutantsTest {

    private val validator = SchemaValidator().apply {
        objectMapper = ObjectMapper()
        initSchemas()
    }

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

    @Test
    fun `loadSchema with missing resource throws RuntimeException and adds nothing`() {
        val dialect = Specification.getDialect(SpecificationVersion.DRAFT_4)
        val context = SchemaContext(dialect, SchemaRegistry.withDefaultDialect(dialect))
        val schemas = mutableMapOf<String, Schema>()

        val ex = assertThrows<RuntimeException> {
            validator.loadSchema(context, "schemas/json/DoesNotExist.json", "DoesNotExist", schemas)
        }
        assertTrue(ex.message!!.contains("schemas/json/DoesNotExist.json"))
        assertTrue(ex.message!!.contains("DoesNotExist"))
        assertTrue(schemas.isEmpty())
    }

    @Test
    fun `loadSchema with malformed json throws RuntimeException with action and path`() {
        val dialect = Specification.getDialect(SpecificationVersion.DRAFT_4)
        val context = SchemaContext(dialect, SchemaRegistry.withDefaultDialect(dialect))
        val schemas = mutableMapOf<String, Schema>()

        val ex = assertThrows<RuntimeException> {
            validator.loadSchema(context, "invalid/InvalidSchema.json", "InvalidSchema", schemas)
        }
        assertTrue(ex.message!!.contains("InvalidSchema"))
        assertTrue(ex.message!!.contains("invalid/InvalidSchema.json"))
        assertTrue(schemas.isEmpty())
    }
}

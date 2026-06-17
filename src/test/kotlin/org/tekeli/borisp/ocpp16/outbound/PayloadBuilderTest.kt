package org.tekeli.borisp.ocpp16.outbound

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PayloadBuilderTest {

    @Test
    fun `filterNulls removes null entries`() {
        val map: Map<String, Any?> = mapOf("a" to 1, "b" to null, "c" to "text")

        val result = map.filterNulls()

        assertEquals(mapOf("a" to 1, "c" to "text"), result)
    }

    @Test
    fun `filterNulls returns empty map when all values are null`() {
        val map: Map<String, Any?> = mapOf("a" to null, "b" to null)

        val result = map.filterNulls()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterNulls returns all entries when none are null`() {
        val map: Map<String, Any?> = mapOf("a" to 1, "b" to "text", "c" to 3.14)

        val result = map.filterNulls()

        assertEquals(mapOf("a" to 1, "b" to "text", "c" to 3.14), result)
    }

    @Test
    fun `filterNulls returns typed Map without unchecked cast`() {
        val map: Map<String, Any?> = mapOf("key" to 42)

        val result: Map<String, Any> = map.filterNulls()

        assertEquals(42, result["key"])
    }

    @Test
    fun `filterNulls handles empty map`() {
        val map: Map<String, Any?> = emptyMap()

        val result = map.filterNulls()

        assertTrue(result.isEmpty())
    }

    @Test
    fun `filterNulls preserves value types`() {
        val map: Map<String, Any?> = mapOf(
            "int" to 42,
            "long" to 42L,
            "double" to 3.14,
            "string" to "hello",
            "bool" to true,
            "list" to listOf(1, 2, 3)
        )

        val result = map.filterNulls()

        assertEquals(42, result["int"])
        assertEquals(42L, result["long"])
        assertEquals(3.14, result["double"])
        assertEquals("hello", result["string"])
        assertEquals(true, result["bool"])
        assertEquals(listOf(1, 2, 3), result["list"])
    }

    @Test
    fun `filterNulls handles mixed null and non-null`() {
        val map: Map<String, Any?> = mapOf(
            "keep1" to 1,
            "drop1" to null,
            "keep2" to "text",
            "drop2" to null,
            "keep3" to 3.14
        )

        val result = map.filterNulls()

        assertEquals(3, result.size)
        assertTrue(result.containsKey("keep1"))
        assertTrue(result.containsKey("keep2"))
        assertTrue(result.containsKey("keep3"))
        assertFalse(result.containsKey("drop1"))
        assertFalse(result.containsKey("drop2"))
    }
}

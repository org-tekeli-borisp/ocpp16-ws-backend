package org.tekeli.borisp.ocpp16.health

import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.eclipse.microprofile.health.HealthCheckResponse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock

class ReadinessHealthCheckMutationTest {

    private fun checkWith(em: EntityManager): ReadinessHealthCheck {
        val check = ReadinessHealthCheck()
        check.em = em
        return check
    }

    @Test
    fun `call returns UP response named database when query succeeds`() {
        val query = mock(Query::class.java)
        val em = mock(EntityManager::class.java)
        `when`(em.createNativeQuery("SELECT 1")).thenReturn(query)

        val response = checkWith(em).call()

        assertEquals("database", response.name)
        assertEquals(HealthCheckResponse.Status.UP, response.status)
    }

    @Test
    fun `em getter throws UninitializedPropertyAccessException before injection`() {
        val check = ReadinessHealthCheck()

        assertThrows(UninitializedPropertyAccessException::class.java) { check.em }
    }

    @Test
    fun `em getter returns the injected entity manager`() {
        val em = mock(EntityManager::class.java)

        assertSame(em, checkWith(em).em)
    }

    @Test
    fun `call returns DOWN with exception message when query fails`() {
        val query = mock(Query::class.java)
        val em = mock(EntityManager::class.java)
        `when`(em.createNativeQuery("SELECT 1")).thenReturn(query)
        `when`(query.getSingleResult()).thenThrow(RuntimeException("connection refused"))

        val response = checkWith(em).call()

        assertEquals("database", response.name)
        assertEquals(HealthCheckResponse.Status.DOWN, response.status)
        assertEquals("connection refused", response.data.get()["error"])
    }

    @Test
    fun `call returns DOWN with fallback message when exception has no message`() {
        val em = mock(EntityManager::class.java)
        `when`(em.createNativeQuery("SELECT 1")).thenThrow(IllegalStateException())

        val response = checkWith(em).call()

        assertEquals("database", response.name)
        assertEquals(HealthCheckResponse.Status.DOWN, response.status)
        assertEquals("Database not reachable", response.data.get()["error"])
    }
}

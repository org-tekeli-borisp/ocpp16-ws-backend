package org.tekeli.borisp.ocpp16.persistence
import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@QuarkusTest
@jakarta.transaction.Transactional
class ChargePointRepositoryTest {

    @Inject
    lateinit var em: EntityManager

    @BeforeEach
    fun cleanup() {
        em.createNativeQuery("DELETE FROM charge_points").executeUpdate()
        em.flush()
    }

    private fun persistAndFlush(cp: ChargePoint) {
        em.persist(cp)
        em.flush()
    }

    @Test
    fun `should persist and find chargePoint by id`() {
        val cp = ChargePoint(
            chargePointId = "Tesla-Model3-1.0",
            vendor = "Tesla",
            model = "Model3",
            firmwareVersion = "1.0",
            status = ChargePointStatus.ONLINE,
            sessionId = "session-1"
        )
        persistAndFlush(cp)

        val found = em.createQuery("SELECT c FROM ChargePoint c WHERE c.chargePointId = :id", ChargePoint::class.java)
            .setParameter("id", "Tesla-Model3-1.0")
            .singleResult

        assertNotNull(found)
        assertEquals("Tesla", found.vendor)
        assertEquals("Model3", found.model)
        assertEquals(ChargePointStatus.ONLINE, found.status)
    }

    @Test
    fun `should update chargePoint status`() {
        val cp = ChargePoint(
            chargePointId = "ABB-Terra-2.1",
            vendor = "ABB",
            model = "Terra",
            status = ChargePointStatus.ONLINE,
            sessionId = "session-2"
        )
        persistAndFlush(cp)

        cp.status = ChargePointStatus.OFFLINE
        cp.touch()
        em.flush()

        em.detach(cp)
        val updated = em.find(ChargePoint::class.java, cp.id)
        assertEquals(ChargePointStatus.OFFLINE, updated?.status)
    }

    @Test
    fun `should find all online chargePoints`() {
        persistAndFlush(ChargePoint(chargePointId = "CP-1", vendor = "V1", model = "M1", status = ChargePointStatus.ONLINE, sessionId = "s1"))
        persistAndFlush(ChargePoint(chargePointId = "CP-2", vendor = "V2", model = "M2", status = ChargePointStatus.OFFLINE, sessionId = "s2"))
        persistAndFlush(ChargePoint(chargePointId = "CP-3", vendor = "V3", model = "M3", status = ChargePointStatus.ONLINE, sessionId = "s3"))

        val online = em.createQuery("SELECT c FROM ChargePoint c WHERE c.status = :status", ChargePoint::class.java)
            .setParameter("status", ChargePointStatus.ONLINE)
            .resultList

        assertEquals(2, online.size)
    }

    @Test
    fun `should return null for no matching chargePoints`() {
        val query = em.createQuery("SELECT c FROM ChargePoint c WHERE c.chargePointId = :id", ChargePoint::class.java)
            .setParameter("id", "NONEXISTENT")
        val results = query.resultList
        assertTrue(results.isEmpty())
    }

    @Test
    fun `should find by sessionId`() {
        persistAndFlush(ChargePoint(chargePointId = "CP-1", vendor = "V1", model = "M1", sessionId = "my-session"))

        val found = em.createQuery("SELECT c FROM ChargePoint c WHERE c.sessionId = :sid", ChargePoint::class.java)
            .setParameter("sid", "my-session").resultList.firstOrNull()

        assertNotNull(found)
        assertEquals("CP-1", found!!.chargePointId)
    }
}

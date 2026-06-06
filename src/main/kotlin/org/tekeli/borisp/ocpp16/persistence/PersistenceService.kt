package org.tekeli.borisp.ocpp16.persistence

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import java.time.Instant

@ApplicationScoped
class PersistenceService {

    @Inject
    lateinit var em: EntityManager

    @Transactional
    fun upsertChargePoint(sessionId: String, chargePointId: String, vendor: String, model: String, firmwareVersion: String?) {
        val existing = em.createQuery(
            "SELECT c FROM ChargePoint c WHERE c.chargePointId = :cpId", ChargePoint::class.java
        ).setParameter("cpId", chargePointId).resultList

        if (existing.isNotEmpty()) {
            val cp = existing[0] as ChargePoint
            cp.status = ChargePointStatus.ONLINE
            cp.sessionId = sessionId
            cp.touch()
            em.flush()
        } else {
            em.persist(ChargePoint(
                chargePointId = chargePointId,
                vendor = vendor,
                model = model,
                firmwareVersion = firmwareVersion,
                status = ChargePointStatus.ONLINE,
                sessionId = sessionId
            ))
            em.flush()
        }
    }

    @Transactional
    fun createTransaction(chargePointId: String, connectorId: Int, idTag: String, meterStart: Int, startTime: Instant): Transaction {
        val txn = Transaction(
            chargePointId = chargePointId,
            connectorId = connectorId,
            idTag = idTag,
            meterStart = meterStart,
            startTime = startTime
        )
        em.persist(txn)
        em.flush()
        return txn
    }

    @Transactional
    fun stopTransaction(transactionId: Long, meterStop: Int, stopTime: Instant, reason: String?, idTagEnd: String?): Boolean {
        val txn = em.find(Transaction::class.java, transactionId)
        if (txn == null || txn.stopTime != null) {
            return false
        }
        txn.stop(meterStop, stopTime, reason, idTagEnd)
        em.flush()
        return true
    }

    @Transactional
    fun setChargePointOffline(sessionId: String) {
        em.createQuery(
            "UPDATE ChargePoint c SET c.status = :status WHERE c.sessionId = :sid", ChargePoint::class.java
        ).setParameter("status", ChargePointStatus.OFFLINE).setParameter("sid", sessionId).executeUpdate()
    }

    @Transactional
    fun setChargePointOnline(sessionId: String) {
        em.createQuery(
            "UPDATE ChargePoint c SET c.status = :status, c.lastSeenAt = :now WHERE c.sessionId = :sid", ChargePoint::class.java
        ).setParameter("status", ChargePointStatus.ONLINE).setParameter("now", Instant.now()).setParameter("sid", sessionId).executeUpdate()
    }

    fun findChargePointBySessionId(sessionId: String): ChargePoint? =
        em.createQuery("SELECT c FROM ChargePoint c WHERE c.sessionId = :sid", ChargePoint::class.java)
            .setParameter("sid", sessionId).resultList.firstOrNull() as ChargePoint?

    fun findChargePointById(chargePointId: String): ChargePoint? =
        em.createQuery("SELECT c FROM ChargePoint c WHERE c.chargePointId = :cpId", ChargePoint::class.java)
            .setParameter("cpId", chargePointId).resultList.firstOrNull() as ChargePoint?

    fun findTransaction(id: Long): Transaction? =
        em.find(Transaction::class.java, id)

    fun findRunningTransactions(chargePointId: String): List<Transaction> =
        em.createQuery("SELECT t FROM Transaction t WHERE t.chargePointId = :cpId AND t.stopTime IS NULL", Transaction::class.java)
            .setParameter("cpId", chargePointId).resultList as List<Transaction>

    fun findAllChargePoints(): List<ChargePoint> =
        em.createQuery("SELECT c FROM ChargePoint c ORDER BY c.lastSeenAt DESC", ChargePoint::class.java)
            .resultList as List<ChargePoint>

    fun findByStatus(status: ChargePointStatus): List<ChargePoint> =
        em.createQuery("SELECT c FROM ChargePoint c WHERE c.status = :status", ChargePoint::class.java)
            .setParameter("status", status).resultList as List<ChargePoint>

    fun findOnlineChargePoints(): List<ChargePoint> =
        em.createQuery("SELECT c FROM ChargePoint c WHERE c.status = :status", ChargePoint::class.java)
            .setParameter("status", ChargePointStatus.ONLINE).resultList as List<ChargePoint>

    fun findAllTransactions(chargePointId: String): List<Transaction> =
        em.createQuery("SELECT t FROM Transaction t WHERE t.chargePointId = :cpId ORDER BY t.startTime DESC", Transaction::class.java)
            .setParameter("cpId", chargePointId).resultList as List<Transaction>
}

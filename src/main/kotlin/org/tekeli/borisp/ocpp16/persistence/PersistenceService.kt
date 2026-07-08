package org.tekeli.borisp.ocpp16.persistence

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import java.time.Instant

data class ConnectorStatusDto(
    val connectorId: Int,
    val status: String,
    val errorCode: String,
    val info: String?,
    val timestamp: String
)

@ApplicationScoped
open class PersistenceService {

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
        } else {
            em.persist(ChargePoint(
                chargePointId = chargePointId,
                vendor = vendor,
                model = model,
                firmwareVersion = firmwareVersion,
                status = ChargePointStatus.ONLINE,
                sessionId = sessionId
            ))
        }
        em.flush()
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
    open fun stopTransaction(transactionId: Long, meterStop: Int, stopTime: Instant, reason: String?, idTagEnd: String?): Boolean {
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
            "UPDATE ChargePoint c SET c.status = :status WHERE c.sessionId = :sid"
        ).setParameter("status", ChargePointStatus.OFFLINE).setParameter("sid", sessionId).executeUpdate()
    }

    @Transactional
    fun setChargePointOnline(sessionId: String) {
        em.createQuery(
            "UPDATE ChargePoint c SET c.status = :status, c.lastSeenAt = :now WHERE c.sessionId = :sid"
        ).setParameter("status", ChargePointStatus.ONLINE).setParameter("now", Instant.now()).setParameter("sid", sessionId).executeUpdate()
    }

    @Transactional
    fun setChargePointOnlineById(chargePointId: String, sessionId: String) {
        em.createQuery(
            "UPDATE ChargePoint c SET c.status = :status, c.sessionId = :sid, c.lastSeenAt = :now WHERE c.chargePointId = :cpId"
        ).setParameter("status", ChargePointStatus.ONLINE)
         .setParameter("sid", sessionId)
         .setParameter("now", Instant.now())
         .setParameter("cpId", chargePointId)
         .executeUpdate()
    }

    fun findChargePointBySessionId(sessionId: String): ChargePoint? {
        val result = em.createQuery("SELECT c FROM ChargePoint c WHERE c.sessionId = :sid", ChargePoint::class.java)
            .setParameter("sid", sessionId).resultList
        return if (result.isEmpty()) null else result[0] as ChargePoint
    }

    fun findChargePointById(chargePointId: String): ChargePoint? {
        val result = em.createQuery("SELECT c FROM ChargePoint c WHERE c.chargePointId = :cpId", ChargePoint::class.java)
            .setParameter("cpId", chargePointId).resultList
        return if (result.isEmpty()) null else result[0] as ChargePoint
    }

    open fun findTransaction(id: Long): Transaction? =
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

    // Security: SecurityLog
    @Transactional
    fun createSecurityLog(chargePointId: String, type: String, timestamp: Instant, techInfo: String?): SecurityLog {
        val log = SecurityLog(
            chargePointId = chargePointId,
            type = type,
            timestamp = timestamp,
            techInfo = techInfo
        )
        em.persist(log)
        em.flush()
        return log
    }

    fun findSecurityLogsByChargePointId(chargePointId: String): List<SecurityLog> =
        em.createQuery("SELECT l FROM SecurityLog l WHERE l.chargePointId = :cpId ORDER BY l.timestamp DESC", SecurityLog::class.java)
            .setParameter("cpId", chargePointId).resultList as List<SecurityLog>

    fun findSecurityLogsByType(type: String): List<SecurityLog> =
        em.createQuery("SELECT l FROM SecurityLog l WHERE l.type = :type ORDER BY l.timestamp DESC", SecurityLog::class.java)
            .setParameter("type", type).resultList as List<SecurityLog>

    fun findRecentSecurityLogs(limit: Int): List<SecurityLog> {
        val query = em.createQuery("SELECT l FROM SecurityLog l ORDER BY l.timestamp DESC", SecurityLog::class.java)
        query.maxResults = limit
        return query.resultList as List<SecurityLog>
    }

    // Security: SignedFirmware
    @Transactional
    fun createSignedFirmware(
        chargePointId: String,
        requestId: Int,
        location: String,
        retrieveDateTime: Instant,
        installDateTime: Instant?,
        signingCertificate: String,
        signature: String
    ): SignedFirmware {
        val firmware = SignedFirmware(
            chargePointId = chargePointId,
            requestId = requestId,
            location = location,
            retrieveDateTime = retrieveDateTime,
            installDateTime = installDateTime,
            signingCertificate = signingCertificate,
            signature = signature,
            status = "Accepted"
        )
        em.persist(firmware)
        em.flush()
        return firmware
    }

    fun findSignedFirmwareByRequestId(chargePointId: String, requestId: Int): SignedFirmware? {
        val result = em.createQuery(
            "SELECT f FROM SignedFirmware f WHERE f.chargePointId = :cpId AND f.requestId = :reqId", SignedFirmware::class.java
        ).setParameter("cpId", chargePointId).setParameter("reqId", requestId).resultList
        return if (result.isEmpty()) null else result[0] as SignedFirmware
    }

    fun findSignedFirmwareById(id: Long): SignedFirmware? =
        em.find(SignedFirmware::class.java, id)

    fun findAllSignedFirmware(chargePointId: String): List<SignedFirmware> =
        em.createQuery("SELECT f FROM SignedFirmware f WHERE f.chargePointId = :cpId ORDER BY f.createdAt DESC", SignedFirmware::class.java)
            .setParameter("cpId", chargePointId).resultList as List<SignedFirmware>

    @Transactional
    fun updateSignedFirmwareStatus(chargePointId: String, requestId: Int, status: String): Boolean {
        val firmware = findSignedFirmwareByRequestId(chargePointId, requestId) ?: return false
        firmware.status = status
        em.flush()
        return true
    }

    // ConnectorStatus
    @Transactional
    open fun updateConnectorStatus(
        chargePointId: String,
        connectorId: Int,
        status: String,
        errorCode: String,
        info: String?
    ) {
        em.createQuery("DELETE FROM ConnectorStatus cs WHERE cs.chargePointId = :cpId AND cs.connectorId = :connId")
            .setParameter("cpId", chargePointId)
            .setParameter("connId", connectorId)
            .executeUpdate()
        em.persist(ConnectorStatus(
            chargePointId = chargePointId,
            connectorId = connectorId,
            status = status,
            errorCode = errorCode,
            info = info
        ))
        em.flush()
    }

    fun findConnectorStatusesByChargePointId(chargePointId: String): List<ConnectorStatusDto> {
        val result = em.createQuery(
            "SELECT cs FROM ConnectorStatus cs WHERE cs.chargePointId = :cpId ORDER BY cs.connectorId",
            ConnectorStatus::class.java
        ).setParameter("cpId", chargePointId).resultList as List<ConnectorStatus>
        return result.map { cs ->
            ConnectorStatusDto(
                connectorId = cs.connectorId,
                status = cs.status,
                errorCode = cs.errorCode,
                info = cs.info,
                timestamp = cs.timestamp.toString()
            )
        }
    }

    @Transactional
    fun createMessageLog(
        chargePointId: String,
        direction: String,
        messageType: String,
        action: String?,
        messageId: String,
        payload: String?
    ): OcppMessageLog {
        val log = OcppMessageLog(
            chargePointId = chargePointId,
            direction = direction,
            messageType = messageType,
            action = action ?: "",
            messageId = messageId,
            payload = payload
        )
        em.persist(log)
        return log
    }

    fun findMessageLogs(chargePointId: String, direction: String?, action: String?, limit: Int): List<OcppMessageLog> {
        var qp = "SELECT m FROM OcppMessageLog m WHERE m.chargePointId = :cpId"
        val conditions = mutableListOf<String>()
        if (direction != null && direction.isNotBlank()) {
            conditions += "m.direction = :direction"
        }
        if (action != null && action.isNotBlank()) {
            conditions += "m.action = :action"
        }
        if (conditions.isNotEmpty()) {
            qp += " AND " + conditions.joinToString(" AND ")
        }
        qp += " ORDER BY m.timestamp DESC"
        var query = em.createQuery(qp, OcppMessageLog::class.java)
            .setParameter("cpId", chargePointId)
        if (direction != null && direction.isNotBlank()) {
            query = query.setParameter("direction", direction)
        }
        if (action != null && action.isNotBlank()) {
            query = query.setParameter("action", action)
        }
        query.maxResults = limit
        return query.resultList as List<OcppMessageLog>
    }

    @Transactional
    fun purgeMessageLogsBefore(cutoff: Instant): Int {
        return em.createQuery(
            "DELETE FROM OcppMessageLog m WHERE m.timestamp < :cutoff"
        ).setParameter("cutoff", cutoff).executeUpdate()
    }
}

package org.tekeli.borisp.ocpp16.persistence

import io.quarkus.hibernate.orm.panache.PanacheEntityBase
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "transactions", indexes = [
    Index(name = "idx_txn_chargepoint", columnList = "chargePointId"),
    Index(name = "idx_txn_idtag", columnList = "idTag"),
    Index(name = "idx_txn_started", columnList = "startTime")
])
class Transaction(
    @Id
    @GeneratedValue
    val id: Long? = null,

    @Column(length = 255, nullable = false)
    val chargePointId: String,

    @Column(nullable = false)
    val connectorId: Int,

    @Column(length = 20, nullable = false)
    val idTag: String,

    @Column(nullable = false)
    val meterStart: Int,

    @Column(nullable = false)
    val startTime: Instant,

    @Column(nullable = false)
    var stopTime: Instant? = null,

    var meterStop: Int? = null,

    @Column(length = 20)
    var stopReason: String? = null,

    @Column(length = 20)
    var idTagEnd: String? = null
) : PanacheEntityBase() {

    val isRunning: Boolean get() = stopTime == null

    val durationSeconds: Long? get() = stopTime?.let { java.time.Duration.between(startTime, it).seconds }

    val energyWh: Int? get() = meterStart.let { start ->
        meterStop?.let { it - start }
    }

    fun stop(meterStop: Int, stopTime: Instant, reason: String?, idTagEnd: String?) {
        this.meterStop = meterStop
        this.stopTime = stopTime
        this.stopReason = reason
        this.idTagEnd = idTagEnd
    }
}

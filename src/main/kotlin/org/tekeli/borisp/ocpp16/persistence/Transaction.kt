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
    Index(name = "idx_txn_chargepoint", columnList = "charge_point_id"),
    Index(name = "idx_txn_idtag", columnList = "id_tag"),
    Index(name = "idx_txn_started", columnList = "start_time")
])
class Transaction @JvmOverloads constructor(
    @Id
    @GeneratedValue
    val id: Long? = null,

    @Column(name = "charge_point_id", length = 255)
    var chargePointId: String = "",

    @Column(name = "connector_id")
    var connectorId: Int = 0,

    @Column(name = "id_tag", length = 20)
    var idTag: String = "",

    @Column(name = "meter_start")
    var meterStart: Int = 0,

    @Column(name = "start_time")
    var startTime: Instant = Instant.EPOCH,

    @Column(name = "stop_time")
    var stopTime: Instant? = null,

    @Column(name = "meter_stop")
    var meterStop: Int? = null,

    @Column(name = "stop_reason", length = 20)
    var stopReason: String? = null,

    @Column(name = "id_tag_end", length = 20)
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

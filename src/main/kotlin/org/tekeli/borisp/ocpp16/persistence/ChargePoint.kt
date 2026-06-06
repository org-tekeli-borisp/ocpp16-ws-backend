package org.tekeli.borisp.ocpp16.persistence

import io.quarkus.hibernate.orm.panache.PanacheEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.Instant

@Entity
@Table(name = "charge_points", indexes = [
    Index(name = "idx_chargepoint_id", columnList = "chargePointId"),
    Index(name = "idx_status", columnList = "status")
])
class ChargePoint @JvmOverloads constructor(
    @Column(length = 255)
    var chargePointId: String = "",

    @Column(length = 20)
    var vendor: String? = null,

    @Column(length = 20)
    var model: String? = null,

    @Column(length = 50)
    var firmwareVersion: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var status: ChargePointStatus = ChargePointStatus.OFFLINE,

    @Column
    var sessionId: String = "",

    var lastSeenAt: Instant = Instant.now(),

    var createdAt: Instant = Instant.now()
) : PanacheEntity() {

    fun touch() {
        lastSeenAt = Instant.now()
    }
}

enum class ChargePointStatus {
    ONLINE,
    OFFLINE
}

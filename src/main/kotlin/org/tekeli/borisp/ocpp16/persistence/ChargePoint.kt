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
    Index(name = "idx_chargepoint_id", columnList = "charge_point_id"),
    Index(name = "idx_status", columnList = "status")
])
class ChargePoint @JvmOverloads constructor(
    @Column(name = "charge_point_id", length = 255)
    var chargePointId: String = "",

    @Column(name = "vendor", length = 20)
    var vendor: String? = null,

    @Column(name = "model", length = 20)
    var model: String? = null,

    @Column(name = "firmware_version", length = 50)
    var firmwareVersion: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    var status: ChargePointStatus = ChargePointStatus.OFFLINE,

    @Column(name = "session_id")
    var sessionId: String = "",

    @Column(name = "last_seen_at")
    var lastSeenAt: Instant = Instant.now(),

    @Column(name = "last_connected_at")
    var lastConnectedAt: Instant = Instant.now(),

    @Column(name = "created_at")
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

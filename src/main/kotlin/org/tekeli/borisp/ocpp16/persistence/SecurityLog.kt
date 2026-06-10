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
@Table(name = "security_logs", indexes = [
    Index(name = "idx_security_logs_chargepoint", columnList = "charge_point_id"),
    Index(name = "idx_security_logs_type", columnList = "type"),
    Index(name = "idx_security_logs_timestamp", columnList = "timestamp")
])
class SecurityLog @JvmOverloads constructor(
    @Id
    @GeneratedValue
    val id: Long? = null,

    @Column(name = "charge_point_id", length = 255)
    var chargePointId: String = "",

    @Column(name = "type", length = 50)
    var type: String = "",

    @Column(name = "timestamp")
    var timestamp: Instant = Instant.now(),

    @Column(name = "tech_info", length = 255)
    var techInfo: String? = null,

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()
) : PanacheEntityBase()

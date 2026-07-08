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
@Table(name = "connector_status", indexes = [
    Index(name = "idx_connector_status_cp", columnList = "charge_point_id")
])
class ConnectorStatus @JvmOverloads constructor(
    @Id
    @GeneratedValue
    val id: Long? = null,

    @Column(name = "charge_point_id", length = 255)
    var chargePointId: String = "",

    @Column(name = "connector_id")
    var connectorId: Int = 0,

    @Column(name = "status", length = 20)
    var status: String = "",

    @Column(name = "error_code", length = 20)
    var errorCode: String = "",

    @Column(name = "info", length = 50)
    var info: String? = null,

    @Column(name = "timestamp")
    var timestamp: Instant = Instant.now()
) : PanacheEntityBase()

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
@Table(name = "ocpp_message_logs", indexes = [
    Index(name = "idx_msg_log_cp_ts", columnList = "charge_point_id, timestamp DESC")
])
class OcppMessageLog @JvmOverloads constructor(
    @Id
    @GeneratedValue
    val id: Long? = null,

    @Column(name = "charge_point_id", length = 255)
    var chargePointId: String = "",

    @Column(name = "direction", length = 10)
    var direction: String = "INBOUND",

    @Column(name = "message_type", length = 15)
    var messageType: String = "CALL",

    @Column(name = "action", length = 100)
    var action: String = "",

    @Column(name = "message_id", length = 255)
    var messageId: String = "",

    @Column(name = "payload", columnDefinition = "TEXT")
    var payload: String? = null,

    @Column(name = "timestamp")
    var timestamp: Instant = Instant.now()
) : PanacheEntityBase()

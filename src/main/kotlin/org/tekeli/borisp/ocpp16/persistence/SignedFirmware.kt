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
@Table(name = "signed_firmware", indexes = [
    Index(name = "idx_signed_firmware_chargepoint", columnList = "charge_point_id"),
    Index(name = "idx_signed_firmware_request_id", columnList = "request_id"),
    Index(name = "idx_signed_firmware_status", columnList = "status")
])
class SignedFirmware @JvmOverloads constructor(
    @Id
    @GeneratedValue
    val id: Long? = null,

    @Column(name = "charge_point_id", length = 255)
    var chargePointId: String = "",

    @Column(name = "request_id")
    var requestId: Int = 0,

    @Column(name = "location", length = 512)
    var location: String = "",

    @Column(name = "retrieve_date_time")
    var retrieveDateTime: Instant = Instant.EPOCH,

    @Column(name = "install_date_time")
    var installDateTime: Instant? = null,

    @Column(name = "signing_certificate", length = 5500)
    var signingCertificate: String = "",

    @Column(name = "signature", length = 800)
    var signature: String = "",

    @Column(name = "status", length = 30)
    var status: String = "Idle",

    @Column(name = "created_at")
    var createdAt: Instant = Instant.now()
) : PanacheEntityBase()

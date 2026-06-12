package org.tekeli.borisp.ocpp16.tls

import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import java.security.MessageDigest
import java.security.cert.X509Certificate

/**
 * Validates client certificates presented during the TLS handshake (mTLS).
 *
 * When QUARKUS_HTTP_SSL_CLIENT_CERT=REQUEST or REQUIRED, Quarkus's elytron
 * x500 extension automatically extracts the client certificate and populates
 * the [SecurityIdentity].  This service:
 * 1. Retrieves the peer certificate from the security context.
 * 2. Stores the SHA-256 fingerprint on the ChargePoint record.
 * 3. Optionally rejects connections from unknown charge points.
 *
 * The HTTP-level mTLS (REQUIRED / REQUEST) already rejects connections without
 * a valid certificate signed by a trusted CA.  The _application_ validation
 * here checks whether the specific charge point is known to this Central System.
 */
@ApplicationScoped
class ClientCertificateService {

    @Inject
    var persistenceService: PersistenceService? = null

    @Inject
    var metricsService: MetricsService? = null

    /**
     * When true, the server rejects WebSocket connections from charge points
     * that are not yet registered in the database.  Set to false to allow
     * ad-hoc registration.
     */
    var enforceKnownChargePoints: Boolean = false

    // ---- Public API used by OcppWebSocketServer ----

    /**
     * Returns the SHA-256 fingerprint of the client certificate, or null
     * when no certificate was presented.
     */
    fun fingerprint(cert: X509Certificate?): String? {
        if (cert == null) return null
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest(cert.encoded)
        return bytes.joinToString(":") { "%02x".format(it) }
    }

    /**
     * Returns the subject DN of the client certificate (e.g. CN=CP-001, O=MyOrg).
     */
    fun subjectDn(cert: X509Certificate?): String? = cert?.subjectX500Principal?.name

    /**
     * Validate the client certificate against the expected chargePointId.
     *
     * Strategy:
     * 1. Look up the charge point by the `chargePointId` from the WebSocket path.
     * 2. If the charge point already exists, verify the fingerprint matches
     *    (prevents MITM with a different certificate).
     * 3. If the charge point does not yet exist and [enforceKnownChargePoints]
     *    is false, allow the connection (the certificate will be stored on
     *    BootNotification).
     *
     * Returns true when the connection should be accepted, false otherwise.
     */
    fun validate(chargePointId: String, cert: X509Certificate?): Boolean {
        val fp = fingerprint(cert)

        if (cert == null) {
            println("[mTLS] No client certificate for $chargePointId – allowing (TLS may be disabled)")
            return true
        }

        val existing = persistenceService?.findChargePointById(chargePointId)

        if (existing != null) {
            if (existing.certFingerprint != null && existing.certFingerprint != fp) {
                metricsService?.securityEventsReceived?.increment()
                println("[mTLS] REJECT: $chargePointId fingerprint mismatch " +
                        "(expected ${existing.certFingerprint}, got $fp)")
                return false
            }
            println("[mTLS] ACCEPT: $chargePointId fingerprint verified ($fp)")
            return true
        }

        if (enforceKnownChargePoints) {
            println("[mTLS] REJECT: unknown charge point $chargePointId (cert: ${subjectDn(cert)})")
            return false
        }

        println("[mTLS] ACCEPT (new): $chargePointId will be registered on BootNotification (cert: $fp)")
        return true
    }

    /**
     * Store the certificate fingerprint for the charge point.
     */
    fun storeFingerprint(chargePointId: String, cert: X509Certificate?) {
        val fp = fingerprint(cert) ?: return
        persistenceService?.updateChargePointCertFingerprint(chargePointId, fp)
    }
}

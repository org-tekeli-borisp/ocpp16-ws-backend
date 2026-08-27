package org.tekeli.borisp.ocpp16.command

import jakarta.enterprise.inject.Instance
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.Mockito
import org.tekeli.borisp.ocpp16.diagnostics.DiagnosticsConfig
import org.tekeli.borisp.ocpp16.diagnostics.DiagnosticsUrlGenerator
import org.tekeli.borisp.ocpp16.diagnostics.FileSystemStorage
import org.tekeli.borisp.ocpp16.diagnostics.FtpServerConfig
import org.tekeli.borisp.ocpp16.diagnostics.SftpServerConfig
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway
import org.tekeli.borisp.ocpp16.protocol.OcppMessage
import java.util.concurrent.CompletableFuture

class GetDiagnosticsCommandMutationTest {

    private val gateway = TestDiagnosticsGateway()

    private fun sftpConfig(enabled: Boolean) = object : SftpServerConfig {
        override fun enabled() = enabled
        override fun port() = 2022
        override fun host() = "127.0.0.1"
        override fun username() = "ocpp"
        override fun password() = "testpass"
    }

    private fun ftpConfig(enabled: Boolean) = object : FtpServerConfig {
        override fun enabled() = enabled
        override fun port() = 2021
        override fun host() = "127.0.0.1"
        override fun username() = "ocpp"
        override fun password() = "testpass"
        override fun passivePorts() = "30000-30100"
        override fun externalAddress() = ""
    }

    private fun diagnosticsConfig() = object : DiagnosticsConfig {
        override fun uploadDir() = "/tmp/ocpp-diagnostics"
        override fun maxFileSizeBytes() = 104857600L
        override fun retentionDays() = 30
        override fun preferredProtocol() = "sftp"
        override fun publicHost() = "127.0.0.1"
    }

    private fun createGeneratorInstance(
        generator: DiagnosticsUrlGenerator?,
        unsatisfied: Boolean? = null,
        ambiguous: Boolean? = null
    ): Instance<DiagnosticsUrlGenerator> {
        val mock = Mockito.mock(Instance::class.java) as Instance<DiagnosticsUrlGenerator>
        Mockito.`when`(mock.isUnsatisfied).thenReturn(unsatisfied ?: (generator == null))
        Mockito.`when`(mock.isAmbiguous).thenReturn(ambiguous ?: false)
        if (generator != null) {
            Mockito.`when`(mock.get()).thenReturn(generator)
        }
        return mock
    }

    private fun createStorageInstance(
        storage: FileSystemStorage?,
        unsatisfied: Boolean? = null,
        ambiguous: Boolean? = null
    ): Instance<FileSystemStorage> {
        val mock = Mockito.mock(Instance::class.java) as Instance<FileSystemStorage>
        Mockito.`when`(mock.isUnsatisfied).thenReturn(unsatisfied ?: (storage == null))
        Mockito.`when`(mock.isAmbiguous).thenReturn(ambiguous ?: false)
        if (storage != null) {
            Mockito.`when`(mock.get()).thenReturn(storage)
        }
        return mock
    }

    private fun mockGenerator(): DiagnosticsUrlGenerator {
        val generator = Mockito.mock(DiagnosticsUrlGenerator::class.java)
        Mockito.`when`(generator.generate("CP-1", "ftp")).thenReturn("gen://ftp/CP-1")
        Mockito.`when`(generator.generate("CP-1")).thenReturn("gen://default/CP-1")
        return generator
    }

    // Kill L29 SURVIVED: removed call to IllegalStateException::getMessage and removed call to mapOf
    @Test
    fun `validate - error message includes generator exception message`() {
        val throwingGenerator = DiagnosticsUrlGenerator(sftpConfig(false), ftpConfig(false), diagnosticsConfig())
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(throwingGenerator), createStorageInstance(null))

        val response = cmd.validate(emptyMap<String, Any>())

        assertNotNull(response)
        assertEquals(400, response!!.status)
        val entity = PayloadValidators.safeMap(response.entity)
        assertTrue(entity["error"].toString().contains("SFTP server is disabled"))
    }

    // Kill L42 NO_COVERAGE: removed call to String::toLowerCase
    // Kill L42 TIMED_OUT: protocol forced null (removed conditional, removed call to Map::get)
    // Kill L45 NO_COVERAGE: removed call to generate(chargePointId, protocol)
    // Kill L45 SURVIVED: removed conditional - always single-arg generate
    @Test
    fun `execute - lowercases protocol before generating URL`() {
        val generator = mockGenerator()
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(generator), createStorageInstance(null))

        val response = cmd.execute("CP-1", mapOf("protocol" to "FTP"))

        assertEquals(202, response.status)
        assertEquals("gen://ftp/CP-1", gateway.lastDiagnosticsLocation)
        Mockito.verify(generator).generate("CP-1", "ftp")
    }

    // Kill L43 SURVIVED: removed conditional - always generate even when location present
    @Test
    fun `execute - explicit location takes precedence over generated URL`() {
        val generator = mockGenerator()
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(generator), createStorageInstance(null))

        val response = cmd.execute("CP-1", mapOf("location" to "http://explicit.example.com/diag", "protocol" to "FTP"))

        assertEquals(202, response.status)
        assertEquals("http://explicit.example.com/diag", gateway.lastDiagnosticsLocation)
    }

    // Kill L43 SURVIVED: removed conditional - never throw when location resolves to null
    // Kill L44 SURVIVED: removed conditional - let block always executes even with null generator
    @Test
    fun `execute - throws IllegalStateException when location missing and no generator`() {
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(null), createStorageInstance(null))

        assertThrows<IllegalStateException> {
            cmd.execute("CP-1", emptyMap<String, Any>())
        }
    }

    // Kill L51 TIMED_OUT: removed call to Map::get for retries
    // Kill L52 TIMED_OUT: removed call to Map::get for retryInterval
    @Test
    fun `execute - passes retries and retryInterval to gateway`() {
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(null), createStorageInstance(null))

        val response = cmd.execute("CP-1", mapOf(
            "location" to "http://diag.example.com",
            "retries" to 3,
            "retryInterval" to 15
        ))

        assertEquals(202, response.status)
        assertEquals(3, gateway.lastDiagnosticsRetries)
        assertEquals(15, gateway.lastDiagnosticsRetryInterval)
    }

    // Kill L42 TIMED_OUT: removed conditional - toLowerCase always invoked (NPE on null protocol)
    // Kill L42 TIMED_OUT: negated conditional - protocol forced null when present
    @Test
    fun `execute - generates default URL when protocol missing`() {
        val generator = mockGenerator()
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(generator), createStorageInstance(null))

        val response = cmd.execute("CP-1", emptyMap<String, Any>())

        assertEquals(202, response.status)
        assertEquals("gen://default/CP-1", gateway.lastDiagnosticsLocation)
        Mockito.verify(generator).generate("CP-1")
    }

    // Kill L61 TIMED_OUT: isUnsatisfied forced true, isAmbiguous forced true, negated conditionals
    // Kill L63 TIMED_OUT: removed call to Instance::get, removed call to FileSystemStorage::ensureDirectory
    @Test
    fun `execute - calls storage ensureDirectory when storage available`() {
        val storage = Mockito.mock(FileSystemStorage::class.java)
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(null), createStorageInstance(storage))

        val response = cmd.execute("CP-1", mapOf("location" to "http://diag.example.com"))

        assertEquals(202, response.status)
        Mockito.verify(storage).ensureDirectory("CP-1")
    }

    // Kill L61 TIMED_OUT: isUnsatisfied forced false, removed call to isUnsatisfied, negated conditional
    @Test
    fun `execute - skips storage when instance unsatisfied`() {
        val storage = Mockito.mock(FileSystemStorage::class.java)
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(null), createStorageInstance(storage, unsatisfied = true))

        val response = cmd.execute("CP-1", mapOf("location" to "http://diag.example.com"))

        assertEquals(202, response.status)
        Mockito.verify(storage, Mockito.never()).ensureDirectory("CP-1")
    }

    // Kill L61 TIMED_OUT: isAmbiguous forced false, removed call to isAmbiguous, negated conditional
    @Test
    fun `execute - skips storage when instance ambiguous`() {
        val storage = Mockito.mock(FileSystemStorage::class.java)
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(null), createStorageInstance(storage, ambiguous = true))

        val response = cmd.execute("CP-1", mapOf("location" to "http://diag.example.com"))

        assertEquals(202, response.status)
        Mockito.verify(storage, Mockito.never()).ensureDirectory("CP-1")
    }

    // Cover L64: ensureDirectory failure is swallowed
    @Test
    fun `execute - swallows storage ensureDirectory failure`() {
        val storage = Mockito.mock(FileSystemStorage::class.java)
        Mockito.doThrow(RuntimeException("disk error")).`when`(storage).ensureDirectory("CP-1")
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(null), createStorageInstance(storage))

        val response = cmd.execute("CP-1", mapOf("location" to "http://diag.example.com"))

        assertEquals(202, response.status)
        Mockito.verify(storage).ensureDirectory("CP-1")
    }

    // Kill L69 SURVIVED: isUnsatisfied check forced false, removed call to isUnsatisfied
    @Test
    fun `validate - returns 400 when generator instance unsatisfied`() {
        val generator = DiagnosticsUrlGenerator(sftpConfig(true), ftpConfig(true), diagnosticsConfig())
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(generator, unsatisfied = true), createStorageInstance(null))

        val response = cmd.validate(emptyMap<String, Any>())

        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    // Kill L69 SURVIVED: isAmbiguous check forced false, removed call to isAmbiguous
    @Test
    fun `validate - returns 400 when generator instance ambiguous`() {
        val generator = DiagnosticsUrlGenerator(sftpConfig(true), ftpConfig(true), diagnosticsConfig())
        val cmd = GetDiagnosticsCommand(gateway, createGeneratorInstance(generator, ambiguous = true), createStorageInstance(null))

        val response = cmd.validate(emptyMap<String, Any>())

        assertNotNull(response)
        assertEquals(400, response!!.status)
    }

    private class TestDiagnosticsGateway : ChargePointGateway {
        var lastDiagnosticsLocation: String? = null
        var lastDiagnosticsRetries: Int? = null
        var lastDiagnosticsRetryInterval: Int? = null

        override fun sendGetDiagnostics(chargePointId: String, location: String, retries: Int?, retryInterval: Int?, startTime: String?, stopTime: String?): CompletableFuture<OcppMessage> {
            lastDiagnosticsLocation = location
            lastDiagnosticsRetries = retries
            lastDiagnosticsRetryInterval = retryInterval
            return CompletableFuture.completedFuture(OcppMessage.CallResult("id", mapOf()))
        }

        override fun sendReset(chargePointId: String, type: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendRemoteStartTransaction(chargePointId: String, idTag: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendRemoteStopTransaction(chargePointId: String, transactionId: Int): CompletableFuture<OcppMessage> = TODO()
        override fun sendUnlockConnector(chargePointId: String, connectorId: Int): CompletableFuture<OcppMessage> = TODO()
        override fun sendCancelReservation(chargePointId: String, reservationId: Int): CompletableFuture<OcppMessage> = TODO()
        override fun sendChangeAvailability(chargePointId: String, connectorId: Int, type: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendChangeConfiguration(chargePointId: String, key: String, value: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendClearCache(chargePointId: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendClearChargingProfile(chargePointId: String, connectorId: Int?, stackLevel: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetCompositeSchedule(chargePointId: String, connectorId: Int, duration: Int): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetConfiguration(chargePointId: String, keys: List<String>?): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetLocalListVersion(chargePointId: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendReserveNow(chargePointId: String, connectorId: Int, expiryDate: String, idTag: String, reservationId: Int): CompletableFuture<OcppMessage> = TODO()
        override fun sendSendLocalList(chargePointId: String, listVersion: Int, updateType: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendSetChargingProfile(chargePointId: String, connectorId: Int, csChargingProfiles: Map<String, Any>): CompletableFuture<OcppMessage> = TODO()
        override fun sendTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendUpdateFirmware(chargePointId: String, location: String, retrieveDate: String, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendExtendedTriggerMessage(chargePointId: String, requestedMessage: String, connectorId: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendInstallCertificate(chargePointId: String, certificateType: String, certificate: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetInstalledCertificateIds(chargePointId: String, certificateType: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendDeleteCertificate(chargePointId: String, certificateHashData: Map<String, Any>): CompletableFuture<OcppMessage> = TODO()
        override fun sendGetLog(chargePointId: String, logType: String, requestId: Int, log: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendSignedUpdateFirmware(chargePointId: String, requestId: Int, firmware: Map<String, Any>, retries: Int?, retryInterval: Int?): CompletableFuture<OcppMessage> = TODO()
        override fun sendCertificateSigned(chargePointId: String, certificateChain: String): CompletableFuture<OcppMessage> = TODO()
        override fun sendDataTransfer(chargePointId: String, vendorId: String, messageId: String?, data: String?): CompletableFuture<OcppMessage> = TODO()
    }
}

package org.tekeli.borisp.ocpp16.command

import jakarta.enterprise.inject.Instance
import jakarta.inject.Inject
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.diagnostics.DiagnosticsUrlGenerator
import org.tekeli.borisp.ocpp16.diagnostics.FileSystemStorage
import org.tekeli.borisp.ocpp16.outbound.ChargePointGateway

@jakarta.enterprise.context.ApplicationScoped
class GetDiagnosticsCommand @Inject constructor(
    private val gateway: ChargePointGateway,
    private val urlGeneratorInstance: Instance<DiagnosticsUrlGenerator>,
    private val storageInstance: Instance<FileSystemStorage>
) : OcppCommand {

    override val name = "get-diagnostics"

    override fun validate(payload: Map<String, Any>): Response? {
        val location = payload["location"] as String?

        if (location.isNullOrEmpty()) {
            val gen = resolveGenerator()
            if (gen != null) {
                try {
                    gen.generate("test")
                } catch (e: IllegalStateException) {
                    return Response.status(Response.Status.BAD_REQUEST)
                        .entity(mapOf<String, Any>("error" to "location is required and no auto-generation available: ${e.message}"))
                        .build()
                }
                return null
            }
            return Response.status(Response.Status.BAD_REQUEST)
                .entity(mapOf<String, Any>("error" to "location is required"))
                .build()
        }
        return null
    }

    override fun execute(chargePointId: String, payload: Map<String, Any>): Response {
        val location = (payload["location"] as String?)
            ?: resolveGenerator()?.generate(chargePointId)
            ?: throw IllegalStateException("location is required and no auto-generation available")

        ensureDirectory(chargePointId)

        val retries = payload["retries"] as Int?
        val retryInterval = payload["retryInterval"] as Int?
        val startTime = payload["startTime"] as String?
        val stopTime = payload["stopTime"] as String?

        val result = gateway.sendGetDiagnostics(chargePointId, location, retries, retryInterval, startTime, stopTime)
        return PayloadValidators.awaitAndBuildResponse(result, name)
    }

    private fun ensureDirectory(chargePointId: String) {
        if (storageInstance.isUnsatisfied || storageInstance.isAmbiguous) return
        try {
            storageInstance.get().ensureDirectory(chargePointId)
        } catch (e: Exception) {
        }
    }

    private fun resolveGenerator(): DiagnosticsUrlGenerator? {
        if (urlGeneratorInstance.isUnsatisfied || urlGeneratorInstance.isAmbiguous) return null
        return try { urlGeneratorInstance.get() } catch (e: Exception) { null }
    }
}

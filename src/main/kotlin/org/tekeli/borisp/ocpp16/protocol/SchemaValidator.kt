package org.tekeli.borisp.ocpp16.protocol

import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.InputFormat
import com.networknt.schema.Schema
import com.networknt.schema.SchemaContext
import com.networknt.schema.SchemaLocation
import com.networknt.schema.SchemaRegistry
import com.networknt.schema.Specification
import com.networknt.schema.SpecificationVersion
import jakarta.enterprise.context.ApplicationScoped
import java.io.InputStream

@ApplicationScoped
class SchemaValidator {

    private val mapper = ObjectMapper()
    private val schemas = loadSchemas()

    fun validate(actionName: String, payloadJson: String): List<String> =
        schemas[actionName]?.let { schema ->
            schema.validate(payloadJson, InputFormat.JSON).map { it.toString() }
        } ?: emptyList()

    private fun loadSchemas(): Map<String, Schema> {
        val v4 = schemaContext(SpecificationVersion.DRAFT_4)
        val v6 = schemaContext(SpecificationVersion.DRAFT_6)
        val schemas = mutableMapOf<String, Schema>()

        loadStandardSchemas(v4, schemas)
        loadSecuritySchemas(v6, schemas)

        return schemas
    }

    private fun schemaContext(version: SpecificationVersion): SchemaContext {
        val dialect = Specification.getDialect(version)
        return SchemaContext(dialect, SchemaRegistry.withDefaultDialect(dialect))
    }

    private fun loadStandardSchemas(context: SchemaContext, schemas: MutableMap<String, Schema>) {
        val actions = listOf(
            "Authorize", "BootNotification", "Heartbeat", "StatusNotification",
            "StartTransaction", "StopTransaction", "MeterValues",
            "DiagnosticsStatusNotification", "FirmwareStatusNotification",
            "DataTransfer", "Reset", "ChangeAvailability", "ChangeConfiguration",
            "ClearCache", "RemoteStartTransaction", "RemoteStopTransaction",
            "CancelReservation", "UnlockConnector", "SetChargingProfile",
            "ClearChargingProfile", "UpdateFirmware", "SendLocalList",
            "ReserveNow", "TriggerMessage", "GetDiagnostics",
            "GetConfiguration", "GetLocalListVersion", "GetCompositeSchedule"
        )
        actions.forEach { action ->
            loadSchema(context, "schemas/json/$action.json", action, schemas)
        }
    }

    private fun loadSecuritySchemas(context: SchemaContext, schemas: MutableMap<String, Schema>) {
        val actions = listOf(
            "SecurityEventNotification", "SignedFirmwareStatusNotification",
            "LogStatusNotification", "SignCertificate", "CertificateSigned",
            "ExtendedTriggerMessage", "InstallCertificate",
            "GetInstalledCertificateIds", "DeleteCertificate", "GetLog",
            "SignedUpdateFirmware"
        )
        actions.forEach { action ->
            loadSchema(context, "schemas/security/$action.json", action, schemas)
        }
    }

    internal fun loadSchema(
        context: SchemaContext,
        resourcePath: String,
        actionName: String,
        schemas: MutableMap<String, Schema>
    ) {
        val inputStream: InputStream? = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream != null) {
            try {
                val schemaNode = mapper.readTree(inputStream)
                schemas[actionName] = context.newSchema(SchemaLocation.DOCUMENT, schemaNode, null)
            } catch (e: Exception) {
                throw RuntimeException("Failed to load schema $actionName from $resourcePath", e)
            }
        }
    }
}

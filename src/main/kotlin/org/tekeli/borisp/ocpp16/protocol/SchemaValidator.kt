package org.tekeli.borisp.ocpp16.protocol

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.networknt.schema.JsonSchemaFactory
import com.networknt.schema.SpecVersion
import java.io.InputStream

class SchemaValidator {

    private val mapper = ObjectMapper()
    private val schemas = loadSchemas()

    fun validate(actionName: String, payloadJson: String): List<String> {
        val schema = schemas[actionName] ?: return emptyList()
        val rootNode: JsonNode = mapper.readTree(payloadJson)
        val result = schema.validate(rootNode)
        return result.map { it.toString() }
    }

    private fun loadSchemas(): Map<String, com.networknt.schema.JsonSchema> {
        val factoryV4 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V4)
        val factoryV6 = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V6)
        val schemas = mutableMapOf<String, com.networknt.schema.JsonSchema>()

        loadStandardSchemas(factoryV4, schemas)
        loadSecuritySchemas(factoryV6, schemas)

        return schemas
    }

    private fun loadStandardSchemas(factory: JsonSchemaFactory, schemas: MutableMap<String, com.networknt.schema.JsonSchema>) {
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
            loadSchema(factory, "schemas/json/$action.json", action, schemas)
        }
    }

    private fun loadSecuritySchemas(factory: JsonSchemaFactory, schemas: MutableMap<String, com.networknt.schema.JsonSchema>) {
        val actions = listOf(
            "SecurityEventNotification", "SignedFirmwareStatusNotification",
            "LogStatusNotification", "SignCertificate", "CertificateSigned",
            "ExtendedTriggerMessage", "InstallCertificate",
            "GetInstalledCertificateIds", "DeleteCertificate", "GetLog",
            "SignedUpdateFirmware"
        )
        actions.forEach { action ->
            loadSchema(factory, "schemas/security/$action.json", action, schemas)
        }
    }

    private fun loadSchema(
        factory: JsonSchemaFactory,
        resourcePath: String,
        actionName: String,
        schemas: MutableMap<String, com.networknt.schema.JsonSchema>
    ) {
        val inputStream: InputStream? = javaClass.classLoader.getResourceAsStream(resourcePath)
        if (inputStream != null) {
            try {
                schemas[actionName] = factory.getSchema(inputStream)
            } catch (e: Exception) {
                throw RuntimeException("Failed to load schema $actionName from $resourcePath", e)
            }
        }
    }
}

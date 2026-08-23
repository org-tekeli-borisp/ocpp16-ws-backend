package org.tekeli.borisp.ocpp16.rest

import jakarta.inject.Inject
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import org.tekeli.borisp.ocpp16.diagnostics.DiagnosticsConfig
import org.tekeli.borisp.ocpp16.diagnostics.FileSystemStorage
import java.io.FileInputStream
import java.nio.file.Files

@Path("/api/chargepoints/{chargePointId}/diagnostics")
@Produces(MediaType.APPLICATION_JSON)
class DiagnosticsResource {

    @Inject
    lateinit var storage: FileSystemStorage

    @GET
    fun list(@PathParam("chargePointId") chargePointId: String): List<Map<String, Any>> {
        val files = storage.listFiles(chargePointId)
        return buildList {
            for (info in files) {
                add(
                    mapOf(
                        "storedName" to info.storedName,
                        "originalName" to info.originalName,
                        "sizeBytes" to info.sizeBytes,
                        "uploadedAt" to info.uploadedAt.toString()
                    )
                )
            }
        }
    }

    @GET
    @Path("{fileName}")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    fun download(
        @PathParam("chargePointId") chargePointId: String,
        @PathParam("fileName") fileName: String
    ): Response {
        val filePath = storage.getFile(chargePointId, fileName)
            ?: return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "File not found: $fileName"))
                .build()

        val file = filePath.toFile()
        return Response.ok(
            FileInputStream(file),
            MediaType.APPLICATION_OCTET_STREAM
        )
            .header("Content-Disposition", "attachment; filename=\"${filePath.fileName}\"")
            .header("Content-Length", file.length())
            .build()
    }

    @DELETE
    @Path("{fileName}")
    fun delete(
        @PathParam("chargePointId") chargePointId: String,
        @PathParam("fileName") fileName: String
    ): Response {
        val deleted = storage.deleteFile(chargePointId, fileName)
        if (!deleted) {
            return Response.status(Response.Status.NOT_FOUND)
                .entity(mapOf("error" to "File not found: $fileName"))
                .build()
        }
        return Response.noContent().build()
    }
}

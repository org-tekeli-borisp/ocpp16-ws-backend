package org.tekeli.borisp.ocpp16.handler

class DiagnosticsStatusNotificationHandler : AbstractStatusNotificationHandler(
    setOf("Idle", "Uploaded", "UploadFailed", "Uploading")
)

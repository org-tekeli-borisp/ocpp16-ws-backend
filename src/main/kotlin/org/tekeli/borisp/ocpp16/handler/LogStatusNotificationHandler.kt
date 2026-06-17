package org.tekeli.borisp.ocpp16.handler

class LogStatusNotificationHandler : AbstractStatusNotificationHandler(
    setOf(
        "BadMessage", "Idle", "NotSupportedOperation",
        "PermissionDenied", "Uploaded", "UploadFailure", "Uploading"
    )
)

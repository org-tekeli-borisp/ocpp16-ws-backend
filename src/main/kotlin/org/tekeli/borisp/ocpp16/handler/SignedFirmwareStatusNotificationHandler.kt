package org.tekeli.borisp.ocpp16.handler

class SignedFirmwareStatusNotificationHandler : AbstractStatusNotificationHandler(
    setOf(
        "Downloaded", "DownloadFailed", "Downloading", "DownloadScheduled",
        "DownloadPaused", "Idle", "InstallationFailed", "Installing",
        "Installed", "InstallRebooting", "InstallScheduled",
        "InstallVerificationFailed", "InvalidSignature", "SignatureVerified"
    )
)

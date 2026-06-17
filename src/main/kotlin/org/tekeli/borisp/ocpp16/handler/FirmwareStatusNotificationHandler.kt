package org.tekeli.borisp.ocpp16.handler

class FirmwareStatusNotificationHandler : AbstractStatusNotificationHandler(
    setOf(
        "Downloaded", "DownloadFailed", "Downloading", "Idle",
        "InstallationFailed", "Installing", "Installed"
    )
)

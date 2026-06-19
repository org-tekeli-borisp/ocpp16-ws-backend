package org.tekeli.borisp.ocpp16

/**
 * OCPP 1.6J protocol constants as defined in the specification.
 */
object OcppConstants {

    // Message format limits
    const val MAX_MESSAGE_ID_LENGTH = 36

    // Identifier limits
    const val MAX_ID_TAG_LENGTH = 20
    const val MAX_VENDOR_LENGTH = 20
    const val MAX_MODEL_LENGTH = 20

    // DataTransfer limits
    const val MAX_VENDOR_ID_LENGTH = 255

    // StatusNotification limits
    const val MAX_INFO_LENGTH = 50

    // SecurityEvent limits
    const val MAX_EVENT_TYPE_LENGTH = 50
    const val MAX_TECH_INFO_LENGTH = 255

    // Certificate limits
    const val MAX_CSR_LENGTH = 5500
    const val MAX_CERTIFICATE_CHAIN_LENGTH = 10000
    const val MAX_CERTIFICATE_LENGTH = 5500

    // Firmware limits
    const val MAX_FIRMWARE_LOCATION_LENGTH = 512
    const val MAX_SIGNING_CERTIFICATE_LENGTH = 5500
    const val MAX_SIGNATURE_LENGTH = 800

    // BootNotification defaults
    const val DEFAULT_HEARTBEAT_INTERVAL_SECONDS = 300

    // Command timeout
    const val COMMAND_TIMEOUT_SECONDS = 10

    // Firmware status notification valid values
    val FIRMWARE_STATUSES = setOf(
        "Downloaded", "DownloadFailed", "Downloading", "Idle",
        "InstallationFailed", "Installing", "Installed"
    )

    // Diagnostics status notification valid values
    val DIAGNOSTICS_STATUSES = setOf("Idle", "Uploaded", "UploadFailed", "Uploading")

    // Log status notification valid values
    val LOG_STATUSES = setOf(
        "BadMessage", "Idle", "NotSupportedOperation",
        "PermissionDenied", "Uploaded", "UploadFailure", "Uploading"
    )

    // Signed firmware status notification valid values
    val SIGNED_FIRMWARE_STATUSES = setOf(
        "Downloaded", "DownloadFailed", "Downloading", "DownloadScheduled",
        "DownloadPaused", "Idle", "InstallationFailed", "Installing",
        "Installed", "InstallRebooting", "InstallScheduled",
        "InstallVerificationFailed", "InvalidSignature", "SignatureVerified"
    )

    // StatusNotification connector status valid values
    val CONNECTOR_STATUSES = setOf(
        "Available", "Preparing", "Charging", "SuspendedEVSE", "SuspendedEV",
        "Finishing", "Reserved", "Unavailable", "Faulted"
    )

    // StatusNotification error code valid values
    val ERROR_CODES = setOf(
        "ConnectorLockFailure", "EVCommunicationError", "GroundFailure",
        "HighTemperature", "InternalError", "LocalListConflict", "NoError",
        "OtherError", "OverCurrentFailure", "OverVoltage", "PowerMeterFailure",
        "PowerSwitchFailure", "ReaderFailure", "ResetFailure", "UnderVoltage",
        "WeakSignal"
    )

    // StopTransaction reason valid values
    val STOP_REASONS = setOf(
        "DeAuthorized", "EmergencyStop", "EVDisconnected", "HardReset",
        "Local", "Other", "PowerLoss", "Reboot", "Remote", "SoftReset",
        "UnlockCommand"
    )

    // SecurityEventNotification type valid values
    val SECURITY_EVENTS = setOf(
        "FirmwareUpdated",
        "FirmwareVerificationFailed",
        "InvalidChargePointCertificate",
        "InvalidCentralSystemCertificate",
        "InvalidTLSCipherSuite",
        "InvalidTLSVersion",
        "LocalAccess",
        "ResetFailed",
        "Reset",
        "Tampering",
        "TransactionInfoNotStored",
        "InvalidFirmwareSigningCertificate",
        "InvalidFirmwareSignature",
        "DiscardedRenewedClientCertificate",
        "UnauthorizedAccess"
    )
}

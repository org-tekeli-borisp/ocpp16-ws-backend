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
}

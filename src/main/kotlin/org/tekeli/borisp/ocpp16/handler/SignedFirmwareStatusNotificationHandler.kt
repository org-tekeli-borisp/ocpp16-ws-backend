package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants

class SignedFirmwareStatusNotificationHandler : AbstractStatusNotificationHandler(
    OcppConstants.SIGNED_FIRMWARE_STATUSES
)

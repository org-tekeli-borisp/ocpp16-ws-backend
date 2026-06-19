package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants

class FirmwareStatusNotificationHandler : AbstractStatusNotificationHandler(
    OcppConstants.FIRMWARE_STATUSES
)

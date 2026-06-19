package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants

class LogStatusNotificationHandler : AbstractStatusNotificationHandler(
    OcppConstants.LOG_STATUSES
)

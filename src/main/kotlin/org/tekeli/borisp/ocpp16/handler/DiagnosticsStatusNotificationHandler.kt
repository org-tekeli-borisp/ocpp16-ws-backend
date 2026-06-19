package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.OcppConstants

class DiagnosticsStatusNotificationHandler : AbstractStatusNotificationHandler(
    OcppConstants.DIAGNOSTICS_STATUSES
)

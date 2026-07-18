package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.persistence.PersistenceService
import org.tekeli.borisp.ocpp16.websocket.ChargePointRegistry

interface OcppHandlerContext {
    val chargePointId: String
    val sessionId: String
    val chargePointRegistry: ChargePointRegistry?
    val persistenceService: PersistenceService?
    val metricsService: MetricsService?
    val heartbeatIntervalSeconds: Long
}

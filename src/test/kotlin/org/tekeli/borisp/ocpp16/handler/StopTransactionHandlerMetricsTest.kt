package org.tekeli.borisp.ocpp16.handler

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.metrics.MetricsService
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class StopTransactionHandlerMetricsTest {

    @Test
    fun `recordMetrics increments energyDeliveredWh correctly`() {
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply { this.metricsService = metricsService }

        handler.recordMetrics(server, 1500.5, 3600)

        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertNotNull(counter)
        assertEquals(1500.5, counter!!.count(), 0.01)
    }

    @Test
    fun `recordMetrics records transactionDuration without error`() {
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply { this.metricsService = metricsService }

        assertDoesNotThrow { handler.recordMetrics(server, 1500.5, 7200) }
    }

    @Test
    fun `recordMetrics calls onTransactionStopped`() {
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply {
            injectedMeterRegistry = meterRegistry
            initGauges()
        }
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply { this.metricsService = metricsService }

        handler.recordMetrics(server, 100.0, 60)

        val gauge = meterRegistry.find("ocpp.transactions.active").gauge()
        assertNotNull(gauge)
    }

    @Test
    fun `recordMetrics with zero energy and duration still records`() {
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply { this.metricsService = metricsService }

        handler.recordMetrics(server, 0.0, 0)

        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertNotNull(counter)
        assertEquals(0.0, counter!!.count())
    }

    @Test
    fun `recordMetrics energy counter reflects exact energy value`() {
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply { this.metricsService = metricsService }

        handler.recordMetrics(server, 2500.75, 1800)

        val counter = meterRegistry.find("ocpp.energy.delivered.wh").counter()
        assertNotNull(counter)
        assertEquals(2500.75, counter!!.count(), 0.001)
    }

    @Test
    fun `recordMetrics transactionsStopped counter increments`() {
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        metricsService.initGauges()
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply { this.metricsService = metricsService }

        metricsService.onTransactionStarted()
        assertEquals(1.0, meterRegistry.find("ocpp.transactions.active").gauge()!!.value(), 0.01)

        handler.recordMetrics(server, 100.0, 60)

        val gauge = meterRegistry.find("ocpp.transactions.active").gauge()
        assertNotNull(gauge)
        assertEquals(0.0, gauge!!.value(), 0.01)
    }

    @Test
    fun `recordMetrics timer records duration value`() {
        val meterRegistry = SimpleMeterRegistry()
        val metricsService = MetricsService().apply { injectedMeterRegistry = meterRegistry }
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply { this.metricsService = metricsService }

        handler.recordMetrics(server, 500.0, 3600)

        val timer = meterRegistry.find("ocpp.transaction.duration.seconds").timer()
        assertNotNull(timer)
        assertEquals(1, timer!!.count())
    }

    @Test
    fun `recordMetrics with null metricsService does not throw`() {
        val handler = StopTransactionHandler()
        val server = OcppWebSocketServer().apply { metricsService = null }
        assertDoesNotThrow { handler.recordMetrics(server, 100.0, 60) }
    }
}

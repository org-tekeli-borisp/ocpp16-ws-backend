package org.tekeli.borisp.ocpp16.metrics

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import jakarta.annotation.PostConstruct
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject

@ApplicationScoped
class MetricsService {

    @Inject
    var injectedMeterRegistry: MeterRegistry? = null

    private val meterRegistry: MeterRegistry
        get() = injectedMeterRegistry ?: SimpleMeterRegistry()

    private var connectionCount = 0
    private var activeTransactionCount = 0

    val transactionsStarted: Counter by lazy {
        Counter.builder("ocpp.transactions.started")
            .description("Total number of charging transactions started")
            .tag("version", "1.6")
            .register(meterRegistry)
    }

    val transactionsStopped: Counter by lazy {
        Counter.builder("ocpp.transactions.stopped")
            .description("Total number of charging transactions stopped")
            .tag("version", "1.6")
            .register(meterRegistry)
    }

    val energyDeliveredWh: Counter by lazy {
        Counter.builder("ocpp.energy.delivered.wh")
            .description("Total energy delivered in watt-hours")
            .tag("version", "1.6")
            .register(meterRegistry)
    }

    val messagesReceived: Counter by lazy {
        Counter.builder("ocpp.messages.received")
            .description("Total OCPP messages received from charge points")
            .tag("version", "1.6")
            .register(meterRegistry)
    }

    val messagesSent: Counter by lazy {
        Counter.builder("ocpp.messages.sent")
            .description("Total OCPP messages sent to charge points")
            .tag("version", "1.6")
            .register(meterRegistry)
    }

    val transactionDuration: Timer by lazy {
        Timer.builder("ocpp.transaction.duration.seconds")
            .description("Duration of charging transactions")
            .tag("version", "1.6")
            .register(meterRegistry)
    }

    @PostConstruct
    fun initGauges() {
        Gauge.builder("ocpp.charge.points.connected", ::getConnectionCount)
            .description("Number of currently connected charge points")
            .register(meterRegistry)

        Gauge.builder("ocpp.transactions.active", ::getActiveTransactionCount)
            .description("Number of currently active charging transactions")
            .register(meterRegistry)
    }

    fun onChargePointConnected() {
        connectionCount++
    }

    fun onChargePointDisconnected() {
        connectionCount--
    }

    fun onTransactionStarted() {
        activeTransactionCount++
        transactionsStarted.increment()
    }

    fun onTransactionStopped() {
        activeTransactionCount--
        transactionsStopped.increment()
    }

    private fun getConnectionCount(): Double = connectionCount.toDouble()
    private fun getActiveTransactionCount(): Double = activeTransactionCount.toDouble()
}

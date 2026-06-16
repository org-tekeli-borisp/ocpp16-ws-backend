package org.tekeli.borisp.ocpp16.metrics

import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MetricsServiceTest {

    private fun createService(): MetricsService {
        val service = MetricsService()
        service.injectedMeterRegistry = SimpleMeterRegistry()
        service.initGauges()
        return service
    }

    @Test
    fun `should start with zero connection count`() {
        val service = createService()

        assertEquals(0, service.getConnectionCount())
    }

    @Test
    fun `should start with zero active transaction count`() {
        val service = createService()

        assertEquals(0, service.getActiveTransactionCount())
    }

    @Test
    fun `should increment connection count on connect`() {
        val service = createService()

        service.onChargePointConnected()

        assertEquals(1, service.getConnectionCount())
    }

    @Test
    fun `should decrement connection count on disconnect`() {
        val service = createService()
        service.onChargePointConnected()

        service.onChargePointDisconnected()

        assertEquals(0, service.getConnectionCount())
    }

    @Test
    fun `should track multiple connections`() {
        val service = createService()

        service.onChargePointConnected()
        service.onChargePointConnected()
        service.onChargePointConnected()

        assertEquals(3, service.getConnectionCount())
    }

    @Test
    fun `should increment transaction count on start`() {
        val service = createService()

        service.onTransactionStarted()

        assertEquals(1, service.getActiveTransactionCount())
    }

    @Test
    fun `should decrement transaction count on stop`() {
        val service = createService()
        service.onTransactionStarted()

        service.onTransactionStopped()

        assertEquals(0, service.getActiveTransactionCount())
    }

    @Test
    fun `should decrement below zero for connection count`() {
        val service = createService()

        service.onChargePointDisconnected()

        assertEquals(-1, service.getConnectionCount())
    }

    @Test
    fun `should decrement below zero for transaction count`() {
        val service = createService()

        service.onTransactionStopped()

        assertEquals(-1, service.getActiveTransactionCount())
    }

    @RepeatedTest(5)
    fun `connection count must be accurate under concurrent access`() {
        val service = createService()
        val threads = 10
        val operationsPerThread = 1000
        val executor: ExecutorService = Executors.newFixedThreadPool(threads)
        val latch = CountDownLatch(threads)

        repeat(threads) {
            executor.submit {
                try {
                    repeat(operationsPerThread) {
                        service.onChargePointConnected()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete")
        executor.shutdown()

        assertEquals(threads * operationsPerThread.toLong(), service.getConnectionCount(),
            "Connection count must be exact after concurrent increments")
    }

    @RepeatedTest(5)
    fun `transaction count must be accurate under concurrent start and stop`() {
        val service = createService()
        val threads = 10
        val operationsPerThread = 1000
        val executor: ExecutorService = Executors.newFixedThreadPool(threads)
        val latch = CountDownLatch(threads)

        // Start half the threads incrementing, half decrementing
        repeat(threads / 2) {
            executor.submit {
                try {
                    repeat(operationsPerThread) {
                        service.onTransactionStarted()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }
        repeat(threads / 2) {
            executor.submit {
                try {
                    repeat(operationsPerThread) {
                        service.onTransactionStarted() // Pre-increment to have something to stop
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete")
        executor.shutdown()

        assertEquals(threads * operationsPerThread.toLong(), service.getActiveTransactionCount(),
            "Transaction count must be exact after concurrent increments")
    }

    @RepeatedTest(5)
    fun `connection count must be accurate under mixed connect and disconnect`() {
        val service = createService()
        val threads = 10
        val operationsPerThread = 1000
        val executor: ExecutorService = Executors.newFixedThreadPool(threads)
        val latch = CountDownLatch(threads)

        // Each thread: connect N times, disconnect N times -> net 0
        repeat(threads) {
            executor.submit {
                try {
                    repeat(operationsPerThread) {
                        service.onChargePointConnected()
                        service.onChargePointDisconnected()
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete")
        executor.shutdown()

        assertEquals(0, service.getConnectionCount(),
            "Connection count should be 0 after equal connect/disconnect operations")
    }

    @RepeatedTest(5)
    fun `counts must be accurate under full concurrent load`() {
        val service = createService()
        val threads = 20
        val operationsPerThread = 500
        val executor: ExecutorService = Executors.newFixedThreadPool(threads)
        val latch = CountDownLatch(threads)

        var expectedConnections = 0L
        var expectedTransactions = 0L

        repeat(threads) { i ->
            executor.submit {
                try {
                    repeat(operationsPerThread) {
                        if (i % 2 == 0) {
                            service.onChargePointConnected()
                            synchronized(service) { expectedConnections++ }
                        } else {
                            service.onTransactionStarted()
                            synchronized(service) { expectedTransactions++ }
                        }
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete")
        executor.shutdown()

        assertEquals(expectedConnections, service.getConnectionCount(),
            "Connection count must match expected under concurrent load")
        assertEquals(expectedTransactions, service.getActiveTransactionCount(),
            "Transaction count must match expected under concurrent load")
    }
}

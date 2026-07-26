package org.tekeli.borisp.ocpp16.websocket

import io.quarkus.test.junit.QuarkusTest
import io.vertx.core.Vertx
import jakarta.inject.Inject
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@QuarkusTest
class VertxSchedulerTest {

    @Inject
    lateinit var vertx: Vertx

    @Test
    fun `schedules task after delay`() {
        val scheduler = VertxScheduler(vertx)
        val executed = AtomicBoolean(false)
        val latch = CountDownLatch(1)

        scheduler.schedule<Unit>({
            executed.set(true)
            latch.countDown()
        }, 50, TimeUnit.MILLISECONDS)

        assertTrue(latch.await(500, TimeUnit.MILLISECONDS), "Task should have executed")
        assertTrue(executed.get(), "Task runnable should have been called")
    }

    @Test
    fun `scheduled task runs on vertx event loop`() {
        val scheduler = VertxScheduler(vertx)

        scheduler.schedule<Unit>({
            assertTrue(
                io.vertx.core.Context.isOnEventLoopThread(),
                "Task must run on Vert.x event loop, not custom thread"
            )
        }, 10, TimeUnit.MILLISECONDS)

        Thread.sleep(100)
    }
}

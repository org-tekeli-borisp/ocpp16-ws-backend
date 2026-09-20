package org.tekeli.borisp.ocpp16.protocol

import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService

@ApplicationScoped
class AwaiterTimeoutScheduler {

    val executor: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor { runnable ->
        Thread(runnable, "ocpp-response-timeout").apply { isDaemon = true }
    }

    @PreDestroy
    fun shutdown() {
        executor.shutdown()
    }
}

package org.tekeli.borisp.ocpp16.protocol

import io.quarkus.logging.Log
import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import jakarta.enterprise.context.ApplicationScoped
import jakarta.inject.Inject
import org.tekeli.borisp.ocpp16.persistence.OcppMessageLog
import java.time.Duration
import java.time.Instant
import java.util.ArrayList
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

@ApplicationScoped
class MessageCaptureService {

    @Inject
    lateinit var persistenceService: org.tekeli.borisp.ocpp16.persistence.PersistenceService

    var bufferSize: Int = 200
        set(value) { field = value.coerceAtLeast(1) }

    var purgeHours: Long = 24
        set(value) { field = value.coerceAtLeast(1) }

    var purgeIntervalMillis: Long = 1_800_000
        set(value) { field = value.coerceAtLeast(1) }

    private val buffers: ConcurrentHashMap<String, LinkedBlockingDeque<OcppMessageDto>> = ConcurrentHashMap()
    private val perCpSubscribers: ConcurrentHashMap<String, MutableList<(OcppMessageDto) -> Unit>> = ConcurrentHashMap()
    private val stopPurger = AtomicBoolean(false)
    private val persistExecutor = Executors.newVirtualThreadPerTaskExecutor()

    @PostConstruct
    fun startPurgeLoop() {
        persistExecutor.submit { purgeLoop() }
    }

    @PreDestroy
    fun close() {
        stopPurger.set(true)
        persistExecutor.shutdown()
    }

    fun capture(chargePointId: String, direction: OcppMessageDirection, ocppMessage: OcppMessage) {
        val dto = toDto(chargePointId, direction, ocppMessage)
        addToBuffer(chargePointId, dto)
        notifySubscribers(chargePointId, dto)
        persistAsync(dto)
    }

    fun getMessages(chargePointId: String): List<OcppMessageDto> {
        return buffers[chargePointId]?.toList().orEmpty()
    }

    fun evict(chargePointId: String) {
        buffers.remove(chargePointId)
        perCpSubscribers.remove(chargePointId)
    }

    fun getMessagesFromDb(chargePointId: String, direction: String?, action: String?, limit: Int = 200): List<OcppMessageDto> {
        val logs = persistenceService.findMessageLogs(chargePointId, direction, action, limit)
        val dtos = mutableListOf<OcppMessageDto>()
        for (log in logs) {
            dtos.add(toDto(log))
        }
        return dtos
    }

    fun subscribe(chargePointId: String, callback: (OcppMessageDto) -> Unit) {
        val list = perCpSubscribers.computeIfAbsent(chargePointId) { ArrayList() }
        synchronized(list) { list.add(callback) }
    }

    fun unsubscribe(chargePointId: String, callback: (OcppMessageDto) -> Unit) {
        val list = perCpSubscribers[chargePointId] ?: return
        synchronized(list) { list.remove(callback) }
    }

    private fun addToBuffer(chargePointId: String, dto: OcppMessageDto) {
        val deque = buffers.computeIfAbsent(chargePointId) { LinkedBlockingDeque(bufferSize) }
        if (deque.size >= bufferSize) {
            deque.pollFirst()
        }
        deque.addLast(dto)
    }

    private fun notifySubscribers(chargePointId: String, dto: OcppMessageDto) {
        val list = perCpSubscribers[chargePointId] ?: return
        val snapshot: List<(OcppMessageDto) -> Unit>
        synchronized(list) { snapshot = ArrayList(list) }
        for (callback in snapshot) {
            try {
                callback(dto)
            } catch (_: Throwable) { }
        }
    }

    private fun persistAsync(dto: OcppMessageDto) {
        persistExecutor.submit {
            try {
                persistenceService.createMessageLog(
                    chargePointId = dto.chargePointId,
                    direction = dto.direction,
                    messageType = dto.messageType,
                    action = dto.action,
                    messageId = dto.messageId,
                    payload = dto.payload
                )
            } catch (e: Throwable) {
                Log.warn("Failed to persist message log for chargePoint=${dto.chargePointId}, messageId=${dto.messageId}: ${e.message}")
            }
        }
    }

    private fun purgeLoop() {
        while (!stopPurger.get()) {
            try {
                TimeUnit.MILLISECONDS.sleep(purgeIntervalMillis)
                purgeOnce()
            } catch (_: InterruptedException) {
                break
            } catch (e: Throwable) {
                Log.warn("Message log purge failed: ${e.message}")
            }
        }
    }

    private fun purgeOnce() {
        val cutoff = Instant.now().minus(Duration.ofHours(purgeHours))
        persistenceService.purgeMessageLogsBefore(cutoff)
    }

    private fun toDto(chargePointId: String, direction: OcppMessageDirection, msg: OcppMessage): OcppMessageDto {
        val payloadJson = try { msg.toJson() } catch (_: Throwable) { null }
        val action = if (msg is OcppMessage.Call) msg.action else null
        return OcppMessageDto(
            chargePointId = chargePointId,
            direction = direction.value,
            messageType = msg.type.name,
            action = action,
            messageId = msg.messageId,
            payload = payloadJson,
            timestamp = Instant.now().toString()
        )
    }

    private fun toDto(log: OcppMessageLog): OcppMessageDto = OcppMessageDto(
        chargePointId = log.chargePointId,
        direction = log.direction,
        messageType = log.messageType,
        action = log.action.takeIf { it.isNotBlank() },
        messageId = log.messageId,
        payload = log.payload,
        timestamp = log.timestamp.toString()
    )
}

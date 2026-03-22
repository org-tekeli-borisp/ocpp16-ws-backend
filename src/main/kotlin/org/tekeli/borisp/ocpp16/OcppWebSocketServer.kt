package org.tekeli.borisp.ocpp16

import io.quarkus.websockets.next.OnClose
import io.quarkus.websockets.next.OnOpen
import io.quarkus.websockets.next.OnTextMessage
import io.quarkus.websockets.next.WebSocket
import io.quarkus.websockets.next.WebSocketConnection
import jakarta.inject.Inject
import java.util.UUID

@WebSocket(path = "/ocpp")
class OcppWebSocketServer {
    
    @Inject
    lateinit var connection: WebSocketConnection
    
    private val sessionId = UUID.randomUUID().toString()
    
    @OnOpen
    fun onOpen() {
        println("WebSocket connection opened: $sessionId")
        
        // Send welcome message
        val welcome = """{"messageId":"${UUID.randomUUID()}","action":"SupportedActions","result":{"actions":["BootNotification","Heartbeat","Authorize"]}}"""
        connection.sendTextAndAwait(welcome)
    }
    
    @OnTextMessage
    fun onTextMessage(message: String): String {
        println("Received message: $message")
        
        // Echo message back for now
        val response = """{"messageId":"${UUID.randomUUID()}","action":"Echo","result":{"echo":"$message"}}"""
        return response
    }
    
    @OnClose
    fun onClose() {
        println("WebSocket connection closed: $sessionId")
    }
}

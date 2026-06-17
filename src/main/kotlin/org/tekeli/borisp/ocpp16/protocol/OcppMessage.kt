package org.tekeli.borisp.ocpp16.protocol

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.tekeli.borisp.ocpp16.OcppConstants

enum class OcppMessageType(val value: Int) {
    CALL(2),
    CALLRESULT(3),
    CALLERROR(4);

    companion object {
        fun fromValue(value: Int): OcppMessageType? = entries.find { it.value == value }
    }
}

enum class OcppErrorCode(val value: String) {
    NOT_IMPLEMENTED("NotImplemented"),
    NOT_SUPPORTED("NotSupported"),
    INTERNAL_ERROR("InternalError"),
    PROTOCOL_ERROR("ProtocolError"),
    SECURITY_ERROR("SecurityError"),
    FORMATION_VIOLATION("FormationViolation"),
    PROPERTY_CONSTRAINT_VIOLATION("PropertyConstraintViolation"),
    OCCURENCE_CONSTRAINT_VIOLATION("OccurenceConstraintViolation"),
    TYPE_CONSTRAINT_VIOLATION("TypeConstraintViolation"),
    GENERIC_ERROR("GenericError");

    companion object {
        fun fromValue(value: String): OcppErrorCode? = entries.find { it.value == value }
    }
}

class OcppParseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

sealed class OcppMessage {
    abstract val type: OcppMessageType
    abstract val messageId: String
    abstract fun toJson(): String

    companion object {
        private val objectMapper = ObjectMapper()

        fun parse(json: String): OcppMessage {
            return try {
                parseMessageInternal(json)
            } catch (e: OcppParseException) {
                throw e
            } catch (e: Exception) {
                throw OcppParseException("Failed to parse OCPP message: ${e.message}", e)
            }
        }

        private fun parseMessageInternal(json: String): OcppMessage {
            val nodes = objectMapper.readValue(json, Array<JsonNode>::class.java)
                .also {
                    if (it.size < 2) {
                        throw OcppParseException("Invalid OCPP message: must have at least 2 elements")
                    }
                }

            val messageTypeId = nodes[0].asInt()
            val messageId = nodes[1].asText()
                .also {
                    if (it.length > OcppConstants.MAX_MESSAGE_ID_LENGTH) {
                        throw OcppParseException("messageId exceeds ${OcppConstants.MAX_MESSAGE_ID_LENGTH} characters")
                    }
                }

            val messageType = OcppMessageType.fromValue(messageTypeId)
                ?: throw OcppParseException("Invalid message type: $messageTypeId")

            return messageType.parseMessage(nodes, messageId)
        }

        @Suppress("UNCHECKED_CAST")
        private fun parseJsonNode(node: JsonNode): Map<String, Any>? {
            return objectMapper.convertValue(node, Map::class.java) as? Map<String, Any>
        }

        private fun OcppMessageType.parseMessage(
            nodes: Array<JsonNode>,
            messageId: String
        ): OcppMessage = when (this) {
            OcppMessageType.CALL -> parseCall(nodes, messageId)
            OcppMessageType.CALLRESULT -> parseCallResult(nodes, messageId)
            OcppMessageType.CALLERROR -> parseCallError(nodes, messageId)
        }

        private fun parseCall(nodes: Array<JsonNode>, messageId: String): Call {
            if (nodes.size != 4) {
                throw OcppParseException("CALL message must have exactly 4 elements")
            }
            return Call(
                messageId = messageId,
                action = nodes[2].asText(),
                payload = parseJsonNode(nodes[3])
            )
        }

        private fun parseCallResult(nodes: Array<JsonNode>, messageId: String): CallResult {
            if (nodes.size != 3) {
                throw OcppParseException("CALLRESULT message must have exactly 3 elements")
            }
            return CallResult(
                messageId = messageId,
                payload = parseJsonNode(nodes[2])
            )
        }

        private fun parseCallError(nodes: Array<JsonNode>, messageId: String): CallError {
            if (nodes.size != 5) {
                throw OcppParseException("CALLERROR message must have exactly 5 elements")
            }
            val errorCodeStr = nodes[2].asText()
            val errorCode = OcppErrorCode.fromValue(errorCodeStr)
                ?: throw OcppParseException("Invalid error code: $errorCodeStr")
            return CallError(
                messageId = messageId,
                errorCode = errorCode,
                errorDescription = nodes[3].asText(),
                errorDetails = parseJsonNode(nodes[4])
            )
        }
    }

    data class Call(
        override val messageId: String,
        val action: String,
        val payload: Map<String, Any>?
    ) : OcppMessage() {
        override val type: OcppMessageType = OcppMessageType.CALL

        override fun toJson(): String {
            val payloadValue: Any = payload ?: emptyMap<String, Any>()
            return objectMapper.writeValueAsString(arrayOf<Any?>(type.value, messageId, action, payloadValue))
        }
    }

    data class CallResult(
        override val messageId: String,
        val payload: Map<String, Any>?
    ) : OcppMessage() {
        override val type: OcppMessageType = OcppMessageType.CALLRESULT

        override fun toJson(): String {
            val payloadValue: Any = payload ?: emptyMap<String, Any>()
            return objectMapper.writeValueAsString(arrayOf<Any?>(type.value, messageId, payloadValue))
        }
    }

    data class CallError(
        override val messageId: String,
        val errorCode: OcppErrorCode,
        val errorDescription: String,
        val errorDetails: Map<String, Any>?
    ) : OcppMessage() {
        override val type: OcppMessageType = OcppMessageType.CALLERROR

        override fun toJson(): String {
            val detailsValue: Any = errorDetails ?: emptyMap<String, Any>()
            return objectMapper.writeValueAsString(
                arrayOf<Any?>(type.value, messageId, errorCode.value, errorDescription, detailsValue)
            )
        }
    }
}

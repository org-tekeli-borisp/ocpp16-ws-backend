package org.tekeli.borisp.ocpp16.handler

import org.tekeli.borisp.ocpp16.protocol.FormationViolationException
import java.time.Instant

/**
 * Shared validation extension functions for OCPP payload maps.
 */

fun Map<String, Any>.requiredString(key: String, maxLength: Int): String {
    val value = get(key)
    if (value == null || value.toString().isBlank()) {
        throw FormationViolationException("$key is required")
    }
    if (value.toString().length > maxLength) {
        throw FormationViolationException("$key must not exceed $maxLength characters")
    }
    return value.toString()
}

fun Map<String, Any>.optionalString(key: String, maxLength: Int): String? {
    val value = get(key) ?: return null
    val str = value.toString().trim()
    if (str.isEmpty()) return null
    if (str.length > maxLength) {
        throw FormationViolationException("$key must not exceed $maxLength characters")
    }
    return str
}

fun Map<String, Any>.requiredInt(key: String, min: Int = Int.MIN_VALUE, max: Int = Int.MAX_VALUE): Int {
    val value = get(key)
    if (value == null) throw FormationViolationException("$key is required")
    val intValue = (value as? Number)?.toInt()
        ?: throw FormationViolationException("$key must be an integer")
    if (intValue < min || intValue > max) {
        val constraint = when {
            min != Int.MIN_VALUE && max != Int.MAX_VALUE -> "between $min and $max"
            min != Int.MIN_VALUE -> ">= $min"
            max != Int.MAX_VALUE -> "<= $max"
            else -> "valid integer"
        }
        throw FormationViolationException("$key must be $constraint")
    }
    return intValue
}

fun Map<String, Any>.requiredLong(key: String): Long {
    val value = get(key)
    if (value == null) throw FormationViolationException("$key is required")
    return (value as? Number)?.toLong()
        ?: throw FormationViolationException("$key must be an integer")
}

fun Map<String, Any>.requiredInstant(key: String): Instant {
    val timestamp = get(key)
    if (timestamp == null || timestamp.toString().isBlank()) {
        throw FormationViolationException("$key is required")
    }
    return try {
        Instant.parse(timestamp.toString())
    } catch (e: Exception) {
        throw FormationViolationException("Invalid $key format")
    }
}

fun Map<String, Any>.requiredStringIn(key: String, validValues: Set<String>): String {
    val value = get(key)
    if (value == null || value.toString().isBlank()) {
        throw FormationViolationException("$key is required")
    }
    val str = value.toString()
    if (str !in validValues) {
        throw FormationViolationException("Invalid $key: $str")
    }
    return str
}

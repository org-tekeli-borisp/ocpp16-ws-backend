package org.tekeli.borisp.ocpp16.protocol

/**
 * Extension functions to construct OCPP call results from a Call message.
 */

fun OcppMessage.Call.callResult(payload: Map<String, Any>? = null): String =
    OcppMessage.CallResult(
        messageId = messageId,
        payload = payload
    ).toJson()

fun OcppMessage.Call.callError(errorCode: OcppErrorCode, errorDescription: String): String =
    OcppMessage.CallError(
        messageId = messageId,
        errorCode = errorCode,
        errorDescription = errorDescription,
        errorDetails = null
    ).toJson()

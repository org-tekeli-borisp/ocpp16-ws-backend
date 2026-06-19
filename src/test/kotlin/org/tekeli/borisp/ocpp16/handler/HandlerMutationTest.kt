package org.tekeli.borisp.ocpp16.handler

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.tekeli.borisp.ocpp16.websocket.OcppWebSocketServer

class HandlerMutationTest {

    private fun newServer(): OcppWebSocketServer {
        return OcppWebSocketServer().apply {
            chargePointId = "SNH764"
            sessionId = "test-session"
        }
    }

    // ========================
    // StopTransactionHandler
    // ========================

    @Test
    fun `StopTransaction all valid reason codes - DeAuthorized`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-DeAuth","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"DeAuthorized"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction all valid reason codes - EmergencyStop`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-Emergency","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"EmergencyStop"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction all valid reason codes - EVDisconnected`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-EVDisc","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"EVDisconnected"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction all valid reason codes - HardReset`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-HardReset","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"HardReset"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction all valid reason codes - Local`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-Local","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction all valid reason codes - Other`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-Other","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Other"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction all valid reason codes - PowerLoss`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-PowerLoss","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"PowerLoss"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction all valid reason codes - Reboot`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-Reboot","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Reboot"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction all valid reason codes - Remote`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-Remote","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Remote"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction all valid reason codes - SoftReset`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-SoftReset","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"SoftReset"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction all valid reason codes - UnlockCommand`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-Unlock","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"UnlockCommand"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction idTag exactly 20 chars must succeed`() {
        val server = newServer()
        val maxIdTag = "T".repeat(20)
        val response = server.onTextMessage(
            """[2,"stop-idtag20","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":"$maxIdTag"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult for idTag=20 chars")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction idTag exactly 21 chars must fail`() {
        val server = newServer()
        val longIdTag = "T".repeat(21)
        val response = server.onTextMessage(
            """[2,"stop-idtag21","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":"$longIdTag"}]"""
        )
        assertTrue(response.startsWith("[4,"), "Must return CallError for idTag=21 chars")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
        assertTrue(response.contains("idTag must not exceed 20 characters"), "Error must mention 20 char limit")
    }

    @Test
    fun `StopTransaction empty idTag must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-emptyidtag","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":""}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult for empty idTag")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction missing idTag must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-noidtag","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult when idTag omitted")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction null idTag must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-nullidtag","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local","idTag":null}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult for null idTag")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction response contains idTagInfo with Accepted status`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-structure","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("\"idTagInfo\""), "Must contain idTagInfo field")
        assertTrue(response.contains("\"status\""), "Must contain status field")
        assertTrue(response.contains("\"Accepted\""), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction meterStop value 0 must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-meter0","StopTransaction",{"transactionId":1,"meterStop":0,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"), "meterStop=0 must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction meterStop negative value must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-meterneg","StopTransaction",{"transactionId":1,"meterStop":-100,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Negative meterStop must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction meterStop large value must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-meterlarge","StopTransaction",{"transactionId":1,"meterStop":2147483647,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Large meterStop must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction transactionId as large long must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-largetxn","StopTransaction",{"transactionId":99999999999,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Large transactionId must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction transactionId as small positive int must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-smalltxn","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Small transactionId must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction transactionId zero must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-zero","StopTransaction",{"transactionId":0,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"), "transactionId=0 must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction transactionId as float must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-float","StopTransaction",{"transactionId":1.0,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Float transactionId must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction invalid reason must fail with FormationViolation`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-bad-reason","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"MadeUpReason"}]"""
        )
        assertTrue(response.startsWith("[4,"), "Must return CallError for invalid reason")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
        assertTrue(response.contains("Invalid reason"), "Error must mention Invalid reason")
    }

    @Test
    fun `StopTransaction whitespace reason must succeed as empty`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-ws-reason","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z","reason":"   "}]"""
        )
        assertTrue(response.startsWith("[3,"), "Whitespace reason treated as empty -> success")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction meterStop as float must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-meter-float","StopTransaction",{"transactionId":1,"meterStop":5000.9,"timestamp":"2024-01-01T01:00:00Z","reason":"Local"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Float meterStop must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StopTransaction response preserves messageId`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"unique-msg-id-stop","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"2024-01-01T01:00:00Z"}]"""
        )
        assertTrue(response.contains("unique-msg-id-stop"), "Response must preserve original messageId")
    }

    @Test
    fun `StopTransaction invalid timestamp format must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"stop-bad-ts","StopTransaction",{"transactionId":1,"meterStop":5000,"timestamp":"not-a-timestamp"}]"""
        )
        assertTrue(response.startsWith("[4,"), "Must return CallError for bad timestamp")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
        assertTrue(response.contains("Invalid timestamp"), "Error must mention Invalid timestamp")
    }

    // ========================
    // StartTransactionHandler
    // ========================

    @Test
    fun `StartTransaction connectorId 0 must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-c0","StartTransaction",{"connectorId":0,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[4,"), "connectorId=0 must return CallError")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
        assertTrue(response.contains("connectorId must be >= 1"), "Error must mention >= 1")
    }

    @Test
    fun `StartTransaction connectorId negative must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-cneg","StartTransaction",{"connectorId":-1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[4,"), "Negative connectorId must return CallError")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
        assertTrue(response.contains("connectorId must be >= 1"), "Error must mention >= 1")
    }

    @Test
    fun `StartTransaction connectorId 1 must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-c1","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"), "connectorId=1 must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StartTransaction response contains transactionId greater than 0`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-txnid","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("\"transactionId\""), "Must contain transactionId field")
        assertTrue(response.contains("\"transactionId\":1"), "transactionId must be >= 1")
    }

    @Test
    fun `StartTransaction idTag exactly 20 chars must succeed`() {
        val server = newServer()
        val maxIdTag = "A".repeat(20)
        val response = server.onTextMessage(
            """[2,"st-idtag20","StartTransaction",{"connectorId":1,"idTag":"$maxIdTag","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"), "idTag=20 chars must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StartTransaction idTag 21 chars must fail`() {
        val server = newServer()
        val longIdTag = "A".repeat(21)
        val response = server.onTextMessage(
            """[2,"st-idtag21","StartTransaction",{"connectorId":1,"idTag":"$longIdTag","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[4,"), "idTag=21 chars must return CallError")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
        assertTrue(response.contains("idTag must not exceed 20 characters"), "Error must mention 20 char limit")
    }

    @Test
    fun `StartTransaction empty idTag must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-emptyidtag","StartTransaction",{"connectorId":1,"idTag":"","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[4,"), "Empty idTag must return CallError")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
        assertTrue(response.contains("idTag is required"), "Error must mention idTag required")
    }

    @Test
    fun `StartTransaction response contains both idTagInfo and transactionId`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-bothfields","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("\"idTagInfo\""), "Must contain idTagInfo")
        assertTrue(response.contains("\"transactionId\""), "Must contain transactionId")
        assertTrue(response.contains("\"Accepted\""), "idTagInfo status must be Accepted")
    }

    @Test
    fun `StartTransaction timestamp with timezone offset must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-tz","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00+05:30"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Timestamp with offset must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StartTransaction timestamp epoch must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-epoch","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"1970-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Epoch timestamp must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StartTransaction invalid timestamp must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-bad-ts","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024/01/01"}]"""
        )
        assertTrue(response.startsWith("[4,"), "Invalid timestamp must return CallError")
        assertTrue(response.contains("Invalid timestamp"), "Error must mention Invalid timestamp")
    }

    @Test
    fun `StartTransaction meterStart 0 must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-ms0","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":0,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"), "meterStart=0 must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StartTransaction meterStart large must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-ms-large","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":2147483647,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Large meterStart must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StartTransaction connectorId large must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-clarge","StartTransaction",{"connectorId":999,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Large connectorId must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `StartTransaction response preserves messageId`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"unique-start-id","StartTransaction",{"connectorId":1,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.contains("unique-start-id"), "Response must preserve messageId")
    }

    @Test
    fun `StartTransaction whitespace idTag must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-ws-idtag","StartTransaction",{"connectorId":1,"idTag":"   ","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[4,"), "Whitespace idTag must return CallError")
        assertTrue(response.contains("idTag is required"), "Error must mention idTag required")
    }

    @Test
    fun `StartTransaction connectorId as float must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"st-cid-float","StartTransaction",{"connectorId":1.0,"idTag":"ABC123","meterStart":1000,"timestamp":"2024-01-01T00:00:00Z"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Float connectorId must return CallResult")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    // ========================
    // BootNotificationHandler
    // ========================

    @Test
    fun `BootNotification response contains currentTime field`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"bn-time","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("\"currentTime\""), "Must contain currentTime field")
        assertTrue(response.contains("T"), "currentTime must contain ISO date separator")
    }

    @Test
    fun `BootNotification response contains interval field`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"bn-interval","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("\"interval\""), "Must contain interval field")
        assertTrue(response.contains("300"), "interval must be 300")
    }

    @Test
    fun `BootNotification response contains status field`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"bn-status","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("\"status\""), "Must contain status field")
        assertTrue(response.contains("\"Accepted\""), "Status must be Accepted")
    }

    @Test
    fun `BootNotification response contains all required fields`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"bn-allfields","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"Model"}]"""
        )
        assertTrue(response.contains("\"currentTime\""), "Must contain currentTime")
        assertTrue(response.contains("\"interval\""), "Must contain interval")
        assertTrue(response.contains("\"status\""), "Must contain status")
        assertTrue(response.contains("\"Accepted\""), "Status must be Accepted")
    }

    @Test
    fun `BootNotification firmwareVersion optional - with firmwareVersion`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"bn-with-fw","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"Model","firmwareVersion":"v1.0.0"}]"""
        )
        assertTrue(response.startsWith("[3,"), "With firmwareVersion must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `BootNotification firmwareVersion optional - without firmwareVersion`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"bn-no-fw","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"Model"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Without firmwareVersion must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `BootNotification firmwareVersion null must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"bn-null-fw","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"Model","firmwareVersion":null}]"""
        )
        assertTrue(response.startsWith("[3,"), "Null firmwareVersion must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `BootNotification vendor exactly 20 chars must succeed`() {
        val server = newServer()
        val maxVendor = "V".repeat(20)
        val response = server.onTextMessage(
            """[2,"bn-v20","BootNotification",{"chargePointVendor":"$maxVendor","chargePointModel":"Model"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Vendor=20 chars must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `BootNotification vendor 21 chars must fail`() {
        val server = newServer()
        val longVendor = "V".repeat(21)
        val response = server.onTextMessage(
            """[2,"bn-v21","BootNotification",{"chargePointVendor":"$longVendor","chargePointModel":"Model"}]"""
        )
        assertTrue(response.startsWith("[4,"), "Vendor=21 chars must fail")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
        assertTrue(response.contains("chargePointVendor must not exceed 20 characters"), "Error must mention limit")
    }

    @Test
    fun `BootNotification model exactly 20 chars must succeed`() {
        val server = newServer()
        val maxModel = "M".repeat(20)
        val response = server.onTextMessage(
            """[2,"bn-m20","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"$maxModel"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Model=20 chars must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `BootNotification model 21 chars must fail`() {
        val server = newServer()
        val longModel = "M".repeat(21)
        val response = server.onTextMessage(
            """[2,"bn-m21","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"$longModel"}]"""
        )
        assertTrue(response.startsWith("[4,"), "Model=21 chars must fail")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
        assertTrue(response.contains("chargePointModel must not exceed 20 characters"), "Error must mention limit")
    }

    @Test
    fun `BootNotification response preserves messageId`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"unique-bn-id","BootNotification",{"chargePointVendor":"Vendor","chargePointModel":"Model"}]"""
        )
        assertTrue(response.contains("unique-bn-id"), "Response must preserve messageId")
    }

    @Test
    fun `BootNotification vendor with special characters must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"bn-special","BootNotification",{"chargePointVendor":"Vendor-X","chargePointModel":"Model 3"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Special chars in vendor must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    @Test
    fun `BootNotification single char vendor and model must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"bn-single","BootNotification",{"chargePointVendor":"V","chargePointModel":"M"}]"""
        )
        assertTrue(response.startsWith("[3,"), "Single char fields must succeed")
        assertTrue(response.contains("Accepted"), "Status must be Accepted")
    }

    // ========================
    // HeartbeatHandler
    // ========================

    @Test
    fun `Heartbeat response contains currentTime field`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"hb-time","Heartbeat",{}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("\"currentTime\""), "Must contain currentTime field")
        assertTrue(response.contains("T"), "currentTime must be ISO datetime")
    }

    @Test
    fun `Heartbeat response structure is correct`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"hb-struct","Heartbeat",{}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("hb-struct"), "Must preserve messageId")
        assertTrue(response.contains("\"currentTime\""), "Must contain currentTime")
    }

    @Test
    fun `Heartbeat response is not an error`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"hb-noterror","Heartbeat",{}]"""
        )
        assertFalse(response.startsWith("[4,"), "Must NOT return CallError")
        assertFalse(response.contains("FormationViolation"), "Must NOT contain FormationViolation")
        assertFalse(response.contains("ProtocolError"), "Must NOT contain ProtocolError")
    }

    @Test
    fun `Heartbeat response with null payload`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"hb-null","Heartbeat",null]"""
        )
        assertTrue(response.startsWith("[3,"), "Null payload heartbeat must return CallResult")
        assertTrue(response.contains("currentTime"), "Must contain currentTime")
    }

    // ========================
    // MeterValuesHandler
    // ========================

    @Test
    fun `MeterValues connectorId 0 valid main power meter`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-c0","MeterValues",{"connectorId":0,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"1000"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"), "connectorId=0 must return CallResult")
        assertTrue(response.contains("mv-c0"), "Must preserve messageId")
    }

    @Test
    fun `MeterValues connectorId -1 invalid`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-cn1","MeterValues",{"connectorId":-1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"1000"}]}]}]"""
        )
        assertTrue(response.startsWith("[4,"), "connectorId=-1 must return CallError")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
        assertTrue(response.contains("connectorId must be >= 0"), "Error must mention >= 0")
    }

    @Test
    fun `MeterValues response is CallResult with null payload`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-nullpayload","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"), "Must return CallResult")
        assertTrue(response.contains("mv-nullpayload"), "Must preserve messageId")
    }

    @Test
    fun `MeterValues multiple meterValue entries`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-multi","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000"}]},{"timestamp":"2024-01-01T00:05:00Z","sampledValue":[{"value":"5100"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"), "Multiple meterValue entries must succeed")
    }

    @Test
    fun `MeterValues multiple sampledValue entries`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-multisv","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000"},{"value":"230.5"},{"value":"16.2"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"), "Multiple sampledValue entries must succeed")
    }

    @Test
    fun `MeterValues sampledValue with string value`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-str","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"Start"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"), "String value in sampledValue must succeed")
    }

    @Test
    fun `MeterValues sampledValue with numeric value`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-num","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":5000.5}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"), "Numeric value in sampledValue must succeed")
    }

    @Test
    fun `MeterValues sampledValue with boolean value`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-bool","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":true}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"), "Boolean value in sampledValue must succeed")
    }

    @Test
    fun `MeterValues sampledValue with additional fields must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-addl","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000","measurand":"Energy.Active.Import.Register","unit":"Wh"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"), "sampledValue with additional fields must succeed")
    }

    @Test
    fun `MeterValues empty meterValue array must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-empty","MeterValues",{"connectorId":1,"meterValue":[]}]"""
        )
        assertTrue(response.startsWith("[4,"), "Empty meterValue must fail")
        assertTrue(response.contains("must contain at least 1 element"), "Error must mention at least 1 element")
    }

    @Test
    fun `MeterValues empty sampledValue array must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-empty-sv","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[]}]}]"""
        )
        assertTrue(response.startsWith("[4,"), "Empty sampledValue must fail")
        assertTrue(response.contains("must contain at least 1 element"), "Error must mention at least 1 element")
    }

    @Test
    fun `MeterValues missing value in sampledValue must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-novalue","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"measurand":"Energy"}]}]}]"""
        )
        assertTrue(response.startsWith("[4,"), "Missing value must fail")
        assertTrue(response.contains("value is required"), "Error must mention value required")
    }

    @Test
    fun `MeterValues missing timestamp in meterValue must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-nots","MeterValues",{"connectorId":1,"meterValue":[{"sampledValue":[{"value":"5000"}]}]}]"""
        )
        assertTrue(response.startsWith("[4,"), "Missing timestamp must fail")
        assertTrue(response.contains("timestamp is required"), "Error must mention timestamp required")
    }

    @Test
    fun `MeterValues empty timestamp in meterValue must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-empty-ts","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"","sampledValue":[{"value":"5000"}]}]}]"""
        )
        assertTrue(response.startsWith("[4,"), "Empty timestamp must fail")
        assertTrue(response.contains("timestamp is required"), "Error must mention timestamp required")
    }

    @Test
    fun `MeterValues whitespace timestamp in meterValue must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-ws-ts","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"   ","sampledValue":[{"value":"5000"}]}]}]"""
        )
        assertTrue(response.startsWith("[4,"), "Whitespace timestamp must fail")
        assertTrue(response.contains("timestamp is required"), "Error must mention timestamp required")
    }

    @Test
    fun `MeterValues meterValue as string must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-str-mv","MeterValues",{"connectorId":1,"meterValue":"not an array"}]"""
        )
        assertTrue(response.startsWith("[4,"), "String meterValue must fail")
        assertTrue(response.contains("meterValue must be an array"), "Error must mention array")
    }

    @Test
    fun `MeterValues sampledValue as string must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-str-sv","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":"not an array"}]}]"""
        )
        assertTrue(response.startsWith("[4,"), "String sampledValue must fail")
        assertTrue(response.contains("sampledValue must be an array"), "Error must mention array")
    }

    @Test
    fun `MeterValues meterValue entry as number must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-mv-num","MeterValues",{"connectorId":1,"meterValue":[42]}]"""
        )
        assertTrue(response.startsWith("[4,"), "Numeric meterValue entry must return CallError")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
    }

    @Test
    fun `MeterValues sampledValue entry as number must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-sv-num","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[42]}]}]"""
        )
        assertTrue(response.startsWith("[4,"), "Numeric sampledValue entry must return CallError")
        assertTrue(response.contains("FormationViolation"), "Error must be FormationViolation")
    }

    @Test
    fun `MeterValues connectorId negative 2 must fail`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-cn2","MeterValues",{"connectorId":-2,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"1000"}]}]}]"""
        )
        assertTrue(response.startsWith("[4,"), "connectorId=-2 must fail")
        assertTrue(response.contains("connectorId must be >= 0"), "Error must mention >= 0")
    }

    @Test
    fun `MeterValues connectorId large positive must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-clarge","MeterValues",{"connectorId":999,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"1000"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"), "Large connectorId must succeed")
    }

    @Test
    fun `MeterValues response preserves messageId`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"unique-mv-id","MeterValues",{"connectorId":1,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"1000"}]}]}]"""
        )
        assertTrue(response.contains("unique-mv-id"), "Response must preserve messageId")
    }

    @Test
    fun `MeterValues with transactionId field must succeed`() {
        val server = newServer()
        val response = server.onTextMessage(
            """[2,"mv-txn","MeterValues",{"connectorId":1,"transactionId":42,"meterValue":[{"timestamp":"2024-01-01T00:00:00Z","sampledValue":[{"value":"5000"}]}]}]"""
        )
        assertTrue(response.startsWith("[3,"), "MeterValues with transactionId must succeed")
    }
}

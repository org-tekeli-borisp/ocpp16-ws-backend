# OCPP 1.6J Specification Gap Analysis

Generated: 2026-07-22

**Coverage Reports**: [JaCoCo](https://org-tekeli-borisp.github.io/ocpp16-ws-backend/jacoco/index.html) | [PITest Mutation](https://org-tekeli-borisp.github.io/ocpp16-ws-backend/mutation/index.html)

## Current State

| Metric | Count |
|--------|-------|
| Command implementations | 26 (19 standard + 7 security) |
| Handler implementations | 15 (10 standard + 5 security) |
| Persistence entities | 6 |
| Test files | 77 |
| Test methods | 1369 |
| JSON schemas | 78 (66 standard + 22 security) |
| Liquibase migrations | 5 |
| REST resources | 4 |
| Prometheus metrics | 8 |
| Health checks | 2 (Liveness, Readiness) |
| Mutation coverage | 95% (PITest) |
| Line coverage | 97% (JaCoCo) |

## References

| Document | Location | Purpose |
|----------|----------|---------|
| `ocpp-j-1.6-specification.pdf` | `docs/spec/` | OCPP-J transport, RPC, connection |
| `ocpp-1.6-edition2.pdf` | `docs/spec/` | Main OCPP 1.6 message definitions |
| `ocpp-1.6-security-e4.pdf` | `docs/spec/` | Security Edition 4 (certificates, signed firmware) |
| JSON schemas (66 files) | `docs/spec/schemas/json/` | Standard message validation |
| Security schemas (22 files) | `docs/spec/schemas/security/` | Security message validation |

---

## Connection & Transport (ocpp-j-1.6-specification.pdf)

| Section | Requirement | Status | Notes |
|---------|-------------|--------|-------|
| 3.1.1 | Connection URL `/ocpp/{chargePointId}` | ✅ Implemented | WebSocket route |
| 3.1.2 | WebSocket subprotocol `ocpp1.6` | ✅ Implemented | Enforced in `@OnOpen`, rejects connections without or with wrong subprotocol |
| 4.1.1 | Synchronicity: no overlapping CALL messages | ⚠️ Partial | `ResponseAwaiter` but no queue/enforcement |
| 4.1.2 | UTF-8 encoding | ✅ Implemented | Jackson default |
| 4.1.3 | Message types: CALL(2), CALLRESULT(3), CALLERROR(4) | ✅ Implemented | `OcppMessage` sealed class |
| 4.1.4 | Unique messageId | ✅ Implemented | UUID generation |
| 4.2.3 | CallError codes (NotImplemented, ProtocolError, etc.) | ✅ Implemented | NotImplemented, ProtocolError, FormationViolation, InternalError all used |
| 5.3 | **WebSocket Ping/Pong** | ✅ Implemented | Server sends periodic ping (30s interval), connection closed on failure |
| 5.4 | Reconnecting: no duplicate BootNotification | ⚠️ Not enforced | Server accepts repeated BootNotification |
| 6.2.1 | TLS support | ❌ Missing | No TLS/WSS implemented |
| 6.2.2 | HTTP Basic Auth for charge points | ❌ Missing | No authorization key validation |
| 7 | **WebSocketPingInterval** config key | ❌ Missing | Required by spec Table 8 |

---

## Standard Messages (ocpp-1.6-edition2.pdf)

| Section | Message Type | Direction | Status | Notes |
|---------|-------------|-----------|--------|-------|
| 4.1 | BootNotification | CP→CS | ✅ Implemented | Handler + persistence |
| 4.2 | Heartbeat | CP→CS | ✅ Implemented | Updates lastSeenAt |
| 4.3 | StatusNotification | CP→CS | ✅ Implemented | Connector status tracking |
| 4.4 | StartTransaction | CP→CS | ✅ Implemented | Transaction persistence |
| 4.4 | StopTransaction | CP→CS | ✅ Implemented | Transaction persistence |
| 4.5 | MeterValues | CP→CS | ✅ Implemented | Energy tracking |
| 4.7 | DiagnosticsStatusNotification | CP→CS | ✅ Implemented | Legacy diagnostics |
| 4.8 | FirmwareStatusNotification | CP→CS | ✅ Implemented | Firmware update status |
| 4.9 | DataTransfer | CP→CS | ✅ Implemented | Vendor-specific |
| 4.10 | Authorize | CP→CS | ✅ Implemented | ID tag validation |
| 5.1 | Reset | CS→CP | ✅ Implemented | Hard/Soft |
| 5.2 | ChangeAvailability | CS→CP | ✅ Implemented | Operative/Inoperative |
| 5.3 | ChangeConfiguration | CS→CP | ⚠️ Partial | **Missing WebSocketPingInterval** |
| 5.4 | ClearCache | CS→CP | ✅ Implemented | |
| 5.5 | RemoteStartTransaction | CS→CP | ✅ Implemented | |
| 5.6 | RemoteStopTransaction | CS→CP | ✅ Implemented | |
| 5.7 | CancelReservation | CS→CP | ✅ Implemented | |
| 5.8 | UnlockConnector | CS→CP | ✅ Implemented | |
| 5.9 | SetChargingProfile | CS→CP | ✅ Implemented | |
| 5.10 | ClearChargingProfile | CS→CP | ✅ Implemented | |
| 5.11 | UpdateFirmware | CS→CP | ✅ Implemented | |
| 5.12 | SendLocalList | CS→CP | ✅ Implemented | |
| 5.13 | ReserveNow | CS→CP | ✅ Implemented | |
| 5.14 | TriggerMessage | CS→CP | ✅ Implemented | All 7 message types |
| 5.15 | GetDiagnostics | CS→CP | ✅ Implemented | Legacy |
| 5.16 | GetConfiguration | CS→CP | ✅ Implemented | |
| 5.17 | DataTransfer | CS→CP | ✅ Implemented | `DataTransferCommand` |
| 5.18 | GetLocalListVersion | CS→CP | ✅ Implemented | |
| 5.19 | GetCompositeSchedule | CS→CP | ✅ Implemented | |
| 9.1.10 | HeartbeatInterval config key | ⚠️ Not tracked | **Stored but not used for stale detection** |

---

## Security Edition 4 (ocpp-1.6-security-e4.pdf)

| Section | Message Type | Direction | Status | Notes |
|---------|-------------|-----------|--------|-------|
| 7.2 | SecurityEventNotification | CP→CS | ✅ Implemented | 15 event types |
| 7.3 | SignedFirmwareStatusNotification | CP→CS | ✅ Implemented | 14 status values |
| 7.4 | LogStatusNotification | CP→CS | ✅ Implemented | |
| 7.5 | SignCertificate | CP→CS | ✅ Implemented | CSR handling |
| 7.6 | CertificateSigned | CP→CS | ✅ Implemented | Signed cert installation |
| 7.6 | CertificateSigned | CS→CP | ⚠️ Partial | `sendCertificateSigned` in `ChargePointGateway` but no `CertificateSignedCommand` |
| 7.7 | ExtendedTriggerMessage | CS→CP | ✅ Implemented | SignChargePointCertificate, LogStatusNotification |
| 7.8 | InstallCertificate | CS→CP | ✅ Implemented | CA cert installation |
| 7.9 | GetInstalledCertificateIds | CS→CP | ✅ Implemented | |
| 7.10 | DeleteCertificate | CS→CP | ✅ Implemented | By hash |
| 7.11 | GetLog | CS→CP | ✅ Implemented | Diagnostics/Security log |
| 7.12 | SignedUpdateFirmware | CS→CP | ✅ Implemented | With signature + certificate |

---

## Critical Gaps

### P0 — Must Fix

| # | Gap | Impact | Effort |
|---|-----|--------|--------|
| 3 | **Missing WebSocketPingInterval** (Table 8) | ChangeConfiguration rejects valid config key | Low |

### P1 — Should Fix

| # | Gap | Impact | Effort |
|---|-----|--------|--------|
| 5 | **HeartbeatInterval not tracked** (9.1.10) | Cannot detect stale connections via heartbeat timeout | Medium |

### Closed

| # | Gap | Resolution |
|---|-----|------------|
| 1 | **No WebSocket Ping/Pong** (5.3) | ✅ Server sends periodic ping (30s interval), connection closed on failure |
| 2 | **Stale Connection Detection** | ✅ Partially addressed: `last_connected_at` column added (migration 005), `lastSeenAt` updated on every message |
| 3 | **Missing WebSocketPingInterval** (Table 8) | ✅ CONFIG_KEYS whitelist (29 keys) added to `ChangeConfigurationCommand` |
| 4 | **Incomplete CallError codes** (4.2.3) | ✅ `InternalError` added for unexpected handler exceptions in `MessageDispatcher` |
| 5 | **HeartbeatInterval not tracked** (9.1.10) | ✅ Closed: Ping/Pong mechanism (360s timeout) provides sufficient stale detection |
| 6 | **WebSocket subprotocol not enforced** (3.1.2) | ✅ Enforced in `OcppWebSocketServer.onOpen()` |
| 11 | **Missing CertificateSignedCommand** | ✅ `CertificateSignedCommand` added with 6 tests |

### P2 — Nice to Have

| # | Gap | Impact | Effort |
|---|-----|--------|--------|
| 7 | **Synchronicity enforcement** (4.1.1) | Prevents message overlap issues | High |
| 8 | **TLS/WSS support** (6.2.1) | Security (required for production) | High |
| 9 | **HTTP Basic Auth** (6.2.2) | Charge point authentication | High |
| 10 | **Reconnect deduplication** (5.4) | Prevents duplicate BootNotification | Medium |

---

## JSON Schema Coverage

| Category | Schemas Available | Schemas Used in Validation |
|----------|------------------|---------------------------|
| Standard (66 files, 33 Call + 33 Response) | ✅ All in `docs/spec/schemas/json/` | ✅ Used — `SchemaValidator` in `MessageDispatcher` + `CommandResource` |
| Security (22 files, 11 Call + 11 Response) | ✅ All in `docs/spec/schemas/security/` | ✅ Used — `SchemaValidator` in `MessageDispatcher` + `CommandResource` |

**Implementation:** `SchemaValidator` loads 78 JSON schemas (66 standard draft-04 + 22 security draft-06) at startup, cached in a `Map<String, JsonSchema>`. Two-layer validation: schema validation runs first (structural checks: required fields, types, additionalProperties, maxLength, enums), followed by manual validation (business logic: empty strings, connectorId ranges, etc.).

### Error Codes (OcppErrorCode)

10 values implemented: `NotImplemented`, `NotSupported`, `InternalError`, `ProtocolError`, `SecurityError`, `FormationViolation`, `PropertyConstraintViolation`, `OccurenceConstraintViolation`, `TypeConstraintViolation`, `GenericError`.

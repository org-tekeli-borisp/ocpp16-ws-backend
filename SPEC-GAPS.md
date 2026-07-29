# SPEC-GAPS — OCPP 1.6J Specification Compliance

Tracks implemented vs. missing features relative to the OCPP 1.6J spec and OCPP 1.6 Security Edition 4.

## Fully Implemented ✅

### Core OCPP 1.6J

| Feature | Status | Notes |
|---------|--------|-------|
| WebSocket transport (`ws://host/ocpp/{id}`) | ✅ | Quarkus WebSocket Next |
| JSON Message Protocol (Call, CallResult, CallError) | ✅ | Types 2, 3, 4 |
| 10 Client→Server messages | ✅ | All handlers implemented |
| 19 Server→Client commands | ✅ | All commands implemented |
| JSON Schema validation (draft-04) | ✅ | 56 standard schemas |
| Connector status tracking | ✅ | Per-connector state persistence |
| Transaction lifecycle | ✅ | Start/Stop with meter values |
| Heartbeat + Keep-alive | ✅ | Ping/Pong + configurable interval |
| Authorization list management | ✅ | SendLocalList, GetLocalListVersion |
| Charging profile management | ✅ | SetChargingProfile, GetCompositeSchedule, ClearChargingProfile |
| Diagnostics upload (legacy) | ✅ | GetDiagnostics + FTP/SFTP servers |
| Firmware update (legacy) | ✅ | UpdateFirmware |
| Reservation management | ✅ | ReserveNow, CancelReservation |
| DataTransfer | ✅ | Vendor-specific data |
| Connection management | ✅ | `DELETE /{id}/connection` + `POST /reconnect-all` (non-standard, operational) |

### OCPP 1.6 Security Edition 4

| Feature | Status | Notes |
|---------|--------|-------|
| 5 Security Client→Server messages | ✅ | SecurityEvent, SignedFirmwareStatus, LogStatus, SignCertificate, CertificateSigned |
| 7 Security Server→Client commands | ✅ | InstallCertificate, GetInstalledCertificateIds, DeleteCertificate, GetLog, ExtendedTriggerMessage, SignedUpdateFirmware, SendCertificateSigned |
| JSON Schema validation (draft-06) | ✅ | 22 security schemas |
| Certificate management | ✅ | Install, query, delete, sign workflow |
| Security event logging | ✅ | 15 event types, persistent storage |
| Signed firmware updates | ✅ | 14 status values |
| Log upload (GetLog) | ✅ | DiagnosticsLog + SecurityLog |

## Partially Implemented ⚠️

| Feature | Status | Gap |
|---------|--------|-----|
| Diagnostics upload (FTP/SFTP) | ⚠️ | Servers work, but URL generation via `GetDiagnostics` is legacy; modern approach uses `GetLog` |
| WebSocket over TLS (WSS) | ⚠️ | Not configured in code (handled by reverse proxy in production) |
| OCSP / CRL validation | ❌ | Not implemented — certificates are stored but not actively validated |
| Message retry with exponential backoff | ⚠️ | Basic retry supported, but not configurable per-message |
| Graceful disconnect handling | ⚠️ | `onClose` handler exists but doesn't persist disconnect reason |

## Not Implemented ❌

| Feature | Status | Notes |
|---------|--------|-------|
| Local Auth List caching | ❌ | ChargePoint-side ACL not mirrored by CS |
| Smart Charging profiles (complex) | ⚠️ | Basic `SetChargingProfile` supported, but complex schedule validation is minimal |
| OCPP-J Message Queue | ❌ | No offline message queue for disconnected charge points |
| Multiple concurrent connections | ❌ | Only one routing entry per chargePointId; new registration actively disconnects the old session (PingPongManager stopped, WebSocket closed, awaiters rejected). |
| Rate limiting | ❌ | No request rate limiting on REST or WebSocket |
| Audit logging | ❌ | No CS-side audit trail for admin actions |
| Role-based access control | ❌ | REST API has no authentication/authorization |
| `Authorize` with LocalAuthList | ❌ | Always returns `Accepted` — no external authorization service |
| Transaction pre-validation | ❌ | No validation of `StopTransaction` against active transaction state |
| MeterValues sampling configuration | ❌ | Doesn't respond to `ChangeConfiguration` for `MeterValueSampleInterval` |
| Diagnostics file retention policy | ⚠️ | Configurable in `application.properties` but not exposed via REST API |

## TriggerMessage Options

### `TriggerMessage` (OCPP 1.6J Standard — 6 options)

Per `schemas/json/TriggerMessage.json`:

| Option | Spec | Implemented |
|--------|------|-------------|
| `BootNotification` | ✅ | ✅ |
| `DiagnosticsStatusNotification` | ✅ | ✅ |
| `FirmwareStatusNotification` | ✅ | ✅ |
| `Heartbeat` | ✅ | ✅ |
| `MeterValues` | ✅ | ✅ |
| `StatusNotification` | ✅ | ✅ |

### `ExtendedTriggerMessage` (Security Edition 4 — 7 options)

Per `schemas/security/ExtendedTriggerMessage.json`:

| Option | Spec | Implemented |
|--------|------|-------------|
| `BootNotification` | ✅ | ✅ |
| `LogStatusNotification` | ✅ | ✅ |
| `FirmwareStatusNotification` | ✅ | ✅ |
| `Heartbeat` | ✅ | ✅ |
| `MeterValues` | ✅ | ✅ |
| `SignChargePointCertificate` | ✅ | ✅ |
| `StatusNotification` | ✅ | ✅ |

## Known Limitations

1. **Single WebSocket per chargePointId** — If a charge point reconnects, the old connection is silently orphaned (not actively closed). It remains alive until timeout or natural close.
2. **No message persistence for offline charge points** — Commands sent to disconnected charge points fail immediately.
3. **No authentication on REST API** — Any client with network access can execute commands.
4. **FTP passive ports require system-level availability** — Ports 40000-40100 must be free for test execution.
5. **No WebSocket TLS in code** — WSS is expected to be handled by a reverse proxy (nginx, Caddy, etc.).
6. **`Authorize` always accepts** — No integration with external authorization systems.
7. **No OCPP 2.0.x support** — This implementation is strictly OCPP 1.6J.

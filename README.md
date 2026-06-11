# OCPP 1.6J WebSocket Backend

OCPP 1.6J (JSON over WebSocket) Charge Point Central System implemented in Kotlin with Quarkus.

## Features

- **OCPP 1.6J compliant** – all Client→Server and Server→Call messages
- **OCPP 1.6 Security Edition 4** – 11 Security Messages (Certificate Management, Security Events, Signed Firmware)
- **WebSocket transport** – `ws://localhost:8080/ocpp/{chargePointId}`
- **24 Remote Commands** – 18 OCPP 1.6J + 6 Security Commands via REST API
- **Database migrations** – Liquibase with PostgreSQL (Dev Services for dev/test)
- **REST API** – charge points, transactions, commands, health & status
- **Mutation Testing** – PITest integration (72% mutation score)
- **554 Unit & Integration Tests**
- **Docker Compose** – ready for production deployment with Prometheus + Grafana monitoring

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  REST API (ChargePointResource, CommandResource,            │
│   TransactionResource, HealthResource)                      │
├─────────────────────────────────────────────────────────────┤
│  Command Pattern (24 OcppCommand implementations)           │
│  ├─ 18 standard OCPP 1.6J commands                          │
│  └─ 6 security commands (SignedUpdateFirmware, GetLog,      │
│     ExtendedTriggerMessage, InstallCertificate,              │
│     GetInstalledCertificateIds, DeleteCertificate)           │
├─────────────────────────────────────────────────────────────┤
│  OcppOutboundService → ChargePointRegistry                  │
│  → OpenConnections → WebSocket                              │
├─────────────────────────────────────────────────────────────┤
│  OcppWebSocketServer → Handlers (15 total)                  │
│  ├─ 10 standard OCPP 1.6J handlers                          │
│  └─ 5 security handlers (SecurityEventNotification,         │
│     SignedFirmwareStatusNotification, LogStatusNotification,│
│     SignCertificate, CertificateSigned)                      │
├─────────────────────────────────────────────────────────────┤
│  Persistence (ChargePoint, Transaction, SecurityLog,        │
│   SignedFirmware → PostgreSQL)                               │
│  Liquibase migrations in db/changelog/                      │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

### Prerequisites

- JDK 21+
- Maven 3.8+
- Docker (for PostgreSQL via Quarkus Dev Services)

### Run in Dev Mode

```bash
mvn quarkus:dev
```

Quarkus Dev Services automatically starts a PostgreSQL container. The server starts on `http://localhost:8080`.

### Connect a Charge Point

```
ws://localhost:8080/ocpp/{chargePointId}
```

Example BootNotification:

```json
[2,"1","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3","firmwareVersion":"1.0"}]
```

## Production Deployment

### Docker Compose

#### JVM Mode (local build, Dev/Test)

```bash
# Optional: configure database credentials
cp .env.example .env

# Start the stack (builds JVM image + PostgreSQL 18)
docker compose up -d

# Check health
curl http://localhost:8080/health

# Stop
docker compose down
```

#### With Prometheus + Grafana Monitoring

```bash
# Start full stack (App + PostgreSQL + Prometheus + Grafana)
docker compose -f docker-compose.monitoring.yml up -d --build

# Services available:
# - App:       http://localhost:8080
# - Prometheus: http://localhost:9090
# - Grafana:   http://localhost:3000 (admin/admin)

# Stop
docker compose -f docker-compose.monitoring.yml down
```

#### Native Mode (pre-built image from GHCR, Production)

```bash
# Start with native image (pulled from GHCR)
docker compose --env-file .env.native up -d

# Stop
docker compose down
```

#### Custom GHCR Image

```bash
# Use specific image tag
APP_IMAGE=ghcr.io/org-tekeli-borisp/ocpp16-ws-backend:sha-abcdef docker compose up -d
```

The stack includes:
- **PostgreSQL 18** on port `5432` with persistent volume
- **Application** on port `8080` with Liquibase auto-migration
- **Prometheus** on port `9090` for metrics collection (monitoring stack)
- **Grafana** on port `3000` for visualization (monitoring stack)
- **Health checks** for all services

### Build & Run Standalone

```bash
mvn package
java -jar target/quarkus-app/quarkus-run.jar \
  -Dquarkus.datasource.jdbc.url=jdbc:postgresql://localhost:5432/ocpp \
  -Dquarkus.datasource.username=postgres \
  -Dquarkus.datasource.password=postgres
```

## REST API

### Health Checks (SmallRye Health)

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Overall health (liveness + readiness) |
| `GET` | `/health/live` | Liveness probe (always UP if running) |
| `GET` | `/health/ready` | Readiness probe (DB connectivity) |

### Prometheus Metrics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/metrics` | Prometheus metrics |

### Charge Points

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/chargepoints` | List all charge points |
| `GET` | `/api/chargepoints/{chargePointId}` | Get charge point details |

### Transactions

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/chargepoints/{chargePointId}/transactions` | List transactions |

### Remote Commands

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/chargepoints/{id}/commands` | List available commands |
| `POST` | `/api/chargepoints/{id}/commands/{command}` | Execute remote command |

**Available Commands (24):**

| Command | Payload | Description |
|---------|---------|-------------|
| `cancel-reservation` | `{"reservationId": 1}` | Cancel a reservation |
| `change-availability` | `{"connectorId": 1, "type": "Inoperative"}` | Set connector availability |
| `change-configuration` | `{"key": "Key", "value": "Value"}` | Change configuration |
| `clear-cache` | `{}` | Clear local cache |
| `clear-charging-profile` | `{"connectorId": 1}` | Clear charging profiles |
| `get-composite-schedule` | `{"connectorId": 1, "duration": 3600}` | Get charging schedule |
| `get-configuration` | `{"key": ["Key1", "Key2"]}` | Get configuration |
| `get-diagnostics` | `{"location": "https://..."}` | Request diagnostics |
| `get-local-list-version` | `{}` | Get ACL version |
| `remote-start-transaction` | `{"idTag": "CARD", "connectorId": 1}` | Start charging remotely |
| `remote-stop-transaction` | `{"transactionId": 1}` | Stop charging remotely |
| `reserve-now` | `{"connectorId": 1, "expiryDate": "...", "idTag": "ID", "reservationId": 1}` | Reserve connector |
| `reset` | `{"type": "Hard"}` | Reset charge point |
| `send-local-list` | `{"listVersion": 1, "updateType": "Full"}` | Send ACL |
| `set-charging-profile` | `{"connectorId": 1, "csChargingProfiles": {...}}` | Set charging profile |
| `trigger-message` | `{"requestedMessage": "Heartbeat"}` | Trigger notification |
| `unlock-connector` | `{"connectorId": 1}` | Unlock connector |
| `update-firmware` | `{"location": "https://...", "retrieveDate": "..."}` | Update firmware |

**Security Commands (OCPP 1.6 Security Edition 4):**

| Command | Payload | Description |
|---------|---------|-------------|
| `extended-trigger-message` | `{"requestedMessage": "SignChargePointCertificate"}` | Extended trigger (Security) |
| `install-certificate` | `{"certificateType": "CentralSystemRootCertificate", "certificate": "..."}` | Install CA certificate |
| `get-installed-certificate-ids` | `{"certificateType": "CentralSystemRootCertificate"}` | List installed certificates |
| `delete-certificate` | `{"certificateHashData": {...}}` | Delete certificate by hash |
| `get-log` | `{"logType": "SecurityLog", "requestId": 1, "log": {...}}` | Request log upload |
| `signed-update-firmware` | `{"requestId": 1, "firmware": {...}}` | Signed firmware update |

**Example – Reset:**

```bash
curl -X POST http://localhost:8080/api/chargepoints/CP-001/commands/reset \
  -H "Content-Type: application/json" \
  -d '{"type": "Soft"}'
```

## OCPP 1.6J Messages

### Client → Server (standard, 10 messages)

| Action | Description |
|--------|-------------|
| `Authorize` | Authorize an ID tag |
| `BootNotification` | Charge point registration |
| `DataTransfer` | Vendor-specific data |
| `DiagnosticsStatusNotification` | Diagnostics upload status |
| `FirmwareStatusNotification` | Firmware update status |
| `Heartbeat` | Keep-alive |
| `MeterValues` | Energy meter readings |
| `StartTransaction` | Charging session start |
| `StatusNotification` | Connector status change |
| `StopTransaction` | Charging session end |

### Client → Server (Security Edition 4, 5 messages)

| Action | Description |
|--------|-------------|
| `SecurityEventNotification` | Critical security events |
| `SignedFirmwareStatusNotification` | Signed firmware update progress |
| `LogStatusNotification` | Log upload status |
| `SignCertificate` | CSR for ChargePoint certificate |
| `CertificateSigned` | Signed certificate response |

### Server → Client (24 commands)

18 standard OCPP 1.6J remote calls + 6 Security Commands (see tables above).

## OCPP 1.6 Security (Edition 4)

Implemented according to OCA White Paper "Improved security for OCPP 1.6-J" Edition 4.

**Certificate Management:**

| Message | Description |
|---------|-------------|
| `InstallCertificate` | Install CA certificate on ChargePoint |
| `GetInstalledCertificateIds` | Query installed certificates |
| `DeleteCertificate` | Delete certificate by hash |
| `SignCertificate` → `CertificateSigned` | Renew ChargePoint certificate |

**Secure Firmware Updates:**

| Message | Description |
|---------|-------------|
| `SignedUpdateFirmware` | Firmware with signature + certificate |
| `SignedFirmwareStatusNotification` | Status updates (14 status values) |

**Security Events & Logging:**

| Message | Description |
|---------|-------------|
| `SecurityEventNotification` | 15 security event types (e.g. Tampering, InvalidTLSVersion) |
| `GetLog` → `LogStatusNotification` | Diagnostics/Security log upload |
| `ExtendedTriggerMessage` | Extended triggers (SignChargePointCertificate, LogStatusNotification) |

## Database Migrations

Liquibase manages the database schema via SQL changelogs in `src/main/resources/db/changelog/`.

```
db/changelog/
├── changelog-master.yaml   # Master changelog
├── 001-init.sql            # Initial schema (charge_points, transactions)
└── 002-security.sql        # Security schema (security_logs, signed_firmware)
```

New migrations: add `00X-name.sql` to `db/changelog/` and include it in `changelog-master.yaml`.

## Project Structure

```
src/main/kotlin/org/tekeli/borisp/ocpp16/
├── command/            # 24 OcppCommand implementations (18 standard + 6 security)
├── handler/            # 15 C→P message handlers (10 standard + 5 security)
├── outbound/           # S→C service layer
├── persistence/        # Entities (ChargePoint, Transaction, SecurityLog, SignedFirmware)
├── protocol/           # OCPP message types, ResponseAwaiter
├── rest/               # REST API resources
└── websocket/          # WebSocket server & registry
```

## Testing

```bash
# Run all tests (Dev Services provides PostgreSQL automatically)
mvn test

# Mutation testing (PITest)
mvn org.pitest:pitest-maven:mutationCoverage
```

**Test Coverage:**

| Category | Tests | Description |
|---------|-------|-------------|
| OCPP Messages | 49 | Parse, Serialize, Error Handling |
| WebSocket Server | 141 | Integration, Handler Dispatch, Error Paths |
| Commands | 179 | 18 Standard + 6 Security Commands |
| Security Handlers | 49 | 5 Handler × ~10 Tests each |
| Persistence | 30 | ChargePoint, Transaction, SecurityLog, SignedFirmware |
| REST API | 19 | ChargePoints, Commands, Transactions |
| Other | 87 | Registry, Dispatcher, Awaiter, Message Types |
| **Total** | **554** | |

## CI/CD Pipeline

GitHub Actions triggered automatically on every push and pull request:

| Job | Trigger | Description |
|-----|---------|-------------|
| **Test** | push + PR | `mvn verify` with PostgreSQL (Dev Services) |
| **Mutation Test** | push | PITest + HTML report as artifact |
| **Docker JVM** | push | JVM Image → GHCR (`latest`, `sha-xxx`) |
| **Docker Native** | push | GraalVM Native Image → GHCR (`latest-native`, `sha-xxx-native`) |

### Docker Images von GHCR

```bash
# JVM Image (recommended for Dev/Test)
docker pull ghcr.io/org-tekeli-borisp/ocpp16-ws-backend:latest

# Native Image (minimal footprint, fastest startup)
docker pull ghcr.io/org-tekeli-borisp/ocpp16-ws-backend:latest-native
```

## Prometheus Metrics

Quarkus Micrometer with Prometheus registry. Metrics available at `/metrics`:

```bash
curl http://localhost:8080/metrics
```

### OCPP-specific metrics:

| Metric | Type | Description |
|--------|-----|-------------|
| `ocpp_transactions_started_total` | Counter | Started charging transactions |
| `ocpp_transactions_stopped_total` | Counter | Stopped transactions |
| `ocpp_energy_delivered_wh` | Counter | Delivered energy (Wh) |
| `ocpp_messages_received_total` | Counter | C→S messages |
| `ocpp_messages_sent_total` | Counter | S→C commands |
| `ocpp_security_events_received_total` | Counter | Security events from charge points |
| `ocpp_charge_points_connected` | Gauge | Active WebSocket connections |
| `ocpp_transactions_active` | Gauge | Running transactions |
| `ocpp_transaction_duration_seconds` | Timer | Transaction duration |

## Configuration

Key properties in `application.properties`:

| Property | Default  | Description |
|----------|----------|-------------|
| `quarkus.http.port` | `8080`   | HTTP/WS port |
| `quarkus.datasource.db-kind` | `postgresql` | Database type |
| `quarkus.liquibase.migrate-at-start` | `true` | Run migrations on startup |
| `quarkus.liquibase.change-log` | `db/changelog/changelog-master.yaml` | Master changelog |

> **Dev & Test:** Quarkus Dev Services automatically provisions a PostgreSQL container – no manual configuration needed.

## Tech Stack

- **Kotlin** 2.3 – primary language
- **Quarkus** 3.36 – application framework
- **PostgreSQL** 18 – production database (Dev Services for dev/test)
- **Liquibase** – database migrations
- **WebSocket Next** – server-sent WebSocket API
- **Hibernate ORM + Panache** – persistence
- **Jackson** – JSON serialization
- **PITest** – mutation testing
- **Docker Compose** – production deployment
- **GitHub Actions** – CI/CD Pipeline
- **GHCR** – Container Registry

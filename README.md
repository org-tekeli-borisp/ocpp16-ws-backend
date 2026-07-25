# OCPP 1.6J WebSocket Backend

OCPP 1.6J (JSON over WebSocket) Charge Point Central System implemented in Kotlin with Quarkus.

## Features

- **OCPP 1.6J compliant** – all Client→Server and Server→Client messages
- **OCPP 1.6 Security Edition 4** – 11 Security Messages (Certificate Management, Security Events, Signed Firmware)
- **JSON Schema Validation** – automatic payload validation using 78 JSON schemas (66 standard + 22 security, draft-04 + draft-06)
- **WebSocket transport** – `ws://localhost:8080/ocpp/{chargePointId}`
- **26 Remote Commands** – 19 OCPP 1.6J + 7 Security Commands via REST API
- **WebUI** – Svelte 5 single-page application (DE/EN/FR) with station overview, remote commands, message log, transactions, and diagnostics
- **Connector Status Tracking** – real-time per-connector state (Available, Charging, Faulted, etc.)
- **Database migrations** – Liquibase with PostgreSQL (Dev Services for dev/test)
- **REST API** – charge points, transactions, commands, health & status
- **Mutation Testing** – PITest integration (95% mutation coverage, 97% line coverage)
- **1433 Unit & Integration Tests** (86 test files)
- **Coverage Reports** – [JaCoCo](https://org-tekeli-borisp.github.io/ocpp16-ws-backend/jacoco/index.html) | [PITest Mutation](https://org-tekeli-borisp.github.io/ocpp16-ws-backend/mutation/index.html)
- **Diagnostics Upload** – FTP (2021) + SFTP (2022) servers for receiving firmware/diagnostic files from charge points
- **Docker Compose** – ready for production deployment with Prometheus + Grafana monitoring

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  REST API (ChargePointResource, CommandResource,            │
│   TransactionResource, MessageResource)                     │
├─────────────────────────────────────────────────────────────┤
│  Command Pattern (26 OcppCommand implementations)           │
│  ├─ 19 standard OCPP 1.6J commands                          │
│  └─ 7 security commands                                     │
├─────────────────────────────────────────────────────────────┤
│  SchemaValidator → 78 JSON Schemas (66 std + 22 sec)        │
│  └─ Two-layer: schema validation + manual business rules    │
├─────────────────────────────────────────────────────────────┤
│  OcppOutboundService → ChargePointRegistry → WebSocket      │
├─────────────────────────────────────────────────────────────┤
│  OcppWebSocketServer → 15 Handlers                          │
│  ├─ 10 standard OCPP 1.6J handlers                          │
│  └─ 5 security handlers                                     │
├─────────────────────────────────────────────────────────────┤
│  Persistence (6 entities → PostgreSQL)                      │
│  Liquibase migrations in db/changelog/                      │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

### Prerequisites

- JDK 25+
- Maven 3.8+
- Docker (for PostgreSQL via Quarkus Dev Services)

### Run in Dev Mode

```bash
mvn quarkus:dev
```

Quarkus Dev Services automatically starts a PostgreSQL container. The server starts on `http://localhost:8080`.

### WebUI

Open `http://localhost:8080/` for the Svelte 5 single-page application with station overview, remote commands, and message log – all in one interface with DE/EN/FR language switching.

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

### OpenAPI / Swagger UI

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/q/swagger-ui/` | Interactive API documentation |
| `GET` | `/q/openapi` | OpenAPI 3.0 specification (JSON) |

### Charge Points

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/chargepoints` | List all charge points |
| `GET` | `/api/chargepoints/{chargePointId}` | Get charge point details with connectors |

### Transactions

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/chargepoints/{chargePointId}/transactions` | List transactions |

### Messages

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/chargepoints/{id}/messages` | List OCPP messages for a charge point |
| `GET` | `/api/chargepoints/{id}/messages/history` | Message history with pagination |

### Diagnostics

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/chargepoints/{id}/diagnostics` | List uploaded diagnostics files |
| `GET` | `/api/chargepoints/{id}/diagnostics/{fileName}` | Download diagnostics file |
| `DELETE` | `/api/chargepoints/{id}/diagnostics/{fileName}` | Delete diagnostics file |

### Remote Commands

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/api/chargepoints/{id}/commands` | List available commands |
| `POST` | `/api/chargepoints/{id}/commands/{command}` | Execute remote command |

**Available Commands (19 standard):**

| Command | Payload | Description |
|---------|---------|-------------|
| `cancel-reservation` | `{"reservationId": 1}` | Cancel a reservation |
| `change-availability` | `{"connectorId": 1, "type": "Inoperative"}` | Set connector availability |
| `change-configuration` | `{"key": "Key", "value": "Value"}` | Change configuration |
| `clear-cache` | `{}` | Clear local cache |
| `clear-charging-profile` | `{"connectorId": 1}` | Clear charging profiles |
| `data-transfer` | `{"vendorId": "VENDOR", "messageId": "M", "data": "D"}` | Vendor-specific data |
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

**Security Commands (OCPP 1.6 Security Edition 4, 7 commands):**

| Command | Payload | Description |
|---------|---------|-------------|
| `send-certificate-signed` | `{"certificateChain": "..."}` | Send signed certificate to ChargePoint |
| `delete-certificate` | `{"certificateHashData": {...}}` | Delete certificate by hash |
| `extended-trigger-message` | `{"requestedMessage": "SignChargePointCertificate"}` | Extended trigger (Security) |
| `get-installed-certificate-ids` | `{"certificateType": "CentralSystemRootCertificate"}` | List installed certificates |
| `get-log` | `{"logType": "SecurityLog", "requestId": 1, "log": {...}}` | Request log upload |
| `install-certificate` | `{"certificateType": "CentralSystemRootCertificate", "certificate": "..."}` | Install CA certificate |
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

### Server → Client (26 commands)

19 standard OCPP 1.6J remote calls + 7 Security Commands (see tables above).

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
├── changelog-master.yaml           # Master changelog
├── 001-init.sql                    # Initial schema (charge_points, transactions)
├── 002-security.sql                # Security schema (security_logs, signed_firmware)
├── 003-connector-status.sql        # Connector status tracking
├── 004-message-log.sql             # OCPP message capture log
└── 005-last-connected-at.sql       # last_connected_at column on charge_points
```

New migrations: add `00X-name.sql` to `db/changelog/` and include it in `changelog-master.yaml`.

## WebUI

Svelte 5 single-page application built with Vite. Source code lives in `webui/` and is compiled during the Maven build via `frontend-maven-plugin` (Node.js v24.11.0, npm 11.6.0). The built output is placed in `src/main/resources/META-INF/resources/` and served by Quarkus.

| Page | URL | Description |
|------|-----|-------------|
| **WebUI** | `/` | Svelte 5 SPA with 5 tabs: station overview, remote commands (26), OCPP message log, transactions, and diagnostics |

### i18n (Internationalization)

The SPA supports three languages with client-side translation, auto-detected from browser locale, selectable via dropdown, and persisted in `localStorage`:

| Language | Code |
|----------|------|
| Deutsch | `de` |
| English | `en` |
| Français | `fr` |

## Project Structure

```
src/main/kotlin/org/tekeli/borisp/ocpp16/
├── command/            # 26 OcppCommand implementations (19 standard + 7 security)
├── diagnostics/        # FTP/SFTP servers, FileSystemStorage, DiagnosticsUrlGenerator
├── handler/            # 15 C→P message handlers (10 standard + 5 security)
├── health/             # Liveness + Readiness health checks
├── metrics/            # Prometheus metrics service
├── outbound/           # S→C service layer
├── persistence/        # Entities (ChargePoint, Transaction, SecurityLog, SignedFirmware, ConnectorStatus, MessageLog)
├── protocol/           # OCPP message types, ResponseAwaiter, MessageCaptureService
├── rest/               # REST API resources (ChargePoint, Command, Transaction, Message, Diagnostics)
└── websocket/          # WebSocket server, registry, ChargePointInfo

src/main/resources/META-INF/resources/
├── index.html          # Svelte 5 SPA entry point
├── assets/             # Bundled CSS
├── js/                 # Bundled JS (Svelte 5)
└── src/                # Source assets
```

## Testing

```bash
# Run all tests (Dev Services provides PostgreSQL automatically)
mvn test

# Mutation testing (PITest)
mvn org.pitest:pitest-maven:mutationCoverage
```

**Test Coverage:**

| Category | Files | Description |
|---------|-------|-------------|
| WebSocket & MessageDispatcher | 21 | Handler Dispatch, Protocol, Error Codes, Infrastructure, Ping/Pong |
| Commands | 4 | Standard + Security + Mutation Tests + Validators |
| Handlers (unit) | 15 | BootNotification, Start/StopTransaction, Heartbeat, Security, Certificates |
| Persistence | 8 | Repositories, Entities, PersistenceService |
| REST API | 5 | ChargePoints, Commands, Transactions, Messages |
| Outbound | 2 | OcppOutboundService, PayloadBuilder |
| Protocol | 3 | SchemaValidator, MessageCaptureService, OcppMessageDirection |
| Health | 2 | Liveness + Readiness probes |
| Metrics | 1 | Prometheus metrics service |
| Integration | 2 | CommandRoundTrip, FullFlowIntegration |
| Root-level | 14 | WebSocket Server, OcppMessage, Registry, ResponseAwaiter, Dispatcher, etc. |
| **Total** | **1433 Tests** | 86 test files |

## CI/CD Pipeline

GitHub Actions triggered automatically on every push and pull request:

| Job | Trigger | Description |
|-----|---------|-------------|
| **Test** | push + PR | `mvn verify` with PostgreSQL (Dev Services) |
| **E2E Tests** | push + PR | Playwright end-to-end tests against Quarkus dev server |
| **CRAP Analysis** | push + PR | Code complexity + coverage metrics (`mvn verify -Pcrap`) |
| **Mutation Test** | push | PITest + HTML report as artifact |
| **Docker JVM** | push | JVM Image → GHCR (`latest`, `sha-xxx`) |
| **Docker Native** | push | GraalVM Native Image → GHCR (`latest-native`, `sha-xxx-native`) |
| **Deploy Pages** | main push | JaCoCo + mutation reports → GitHub Pages |

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
| `ocpp.transactions.started` | Counter | Started charging transactions |
| `ocpp.transactions.stopped` | Counter | Stopped transactions |
| `ocpp.energy.delivered.wh` | Counter | Delivered energy (Wh) |
| `ocpp.messages.received` | Counter | C→S messages |
| `ocpp.messages.sent` | Counter | S→C commands |
| `ocpp.security.events.received` | Counter | Security events from charge points |
| `ocpp.charge.points.connected` | Gauge | Active WebSocket connections |
| `ocpp.transactions.active` | Gauge | Running transactions |
| `ocpp.transaction.duration.seconds` | Timer | Transaction duration |

## Configuration

Key properties in `application.properties`:

| Property | Default  | Description |
|----------|----------|-------------|
| `quarkus.http.port` | `8080`   | HTTP/WS port |
| `quarkus.datasource.db-kind` | `postgresql` | Database type |
| `quarkus.liquibase.migrate-at-start` | `true` | Run migrations on startup |
| `quarkus.liquibase.change-log` | `db/changelog/changelog-master.yaml` | Master changelog |
| `ocpp.websocket.ping-interval-seconds` | `30` | WebSocket ping interval (seconds) |
| `ocpp.websocket.pong-timeout-seconds` | `360` | Pong timeout (seconds) |
| `ocpp.heartbeat.interval-seconds` | `300` | Default heartbeat interval (seconds) |
| `quarkus.smallrye-health.root-path` | `/health` | Health check root path |
| `quarkus.micrometer.export.prometheus.path` | `/metrics` | Prometheus metrics path |

> **Dev & Test:** Quarkus Dev Services automatically provisions a PostgreSQL container – no manual configuration needed.

## Tech Stack

| Layer | Technology |
|-------|------------|
| Language | Kotlin 2.3.21 (JVM target 25) |
| Framework | Quarkus 3.36.2 |
| Database | PostgreSQL 18 |
| Migrations | Liquibase (5 migrations) |
| WebSocket | Quarkus WebSocket Next |
| Persistence | Hibernate ORM + Panache |
| JSON | Jackson |
| Schema Validation | networknt/json-schema-validator 1.5.2 (draft-04 + draft-06) |
| Testing | JUnit 5, RestAssured, Mockito 5.18.0, PITest 1.23.0, Playwright, Vitest |
| Metrics | Micrometer + Prometheus |
| Deployment | Docker Compose, GitHub Actions, GHCR |
| WebUI | Svelte 5 + Vite 6 + TypeScript 5.7 (built via frontend-maven-plugin) |

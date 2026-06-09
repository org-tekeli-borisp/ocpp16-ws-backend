# OCPP 1.6J WebSocket Backend

OCPP 1.6J (JSON over WebSocket) Charge Point Central System implemented in Kotlin with Quarkus.

## Features

- **OCPP 1.6J compliant** – all Client→Server and Server→Call messages
- **WebSocket transport** – `ws://localhost:8080/ocpp/{chargePointId}`
- **18 Remote Commands** – full Server→Client control via REST API
- **Database migrations** – Liquibase with PostgreSQL (Dev Services for dev/test)
- **REST API** – charge points, transactions, commands, health & status
- **Mutation Testing** – PITest integration (79%+ mutation score)
- **430+ Unit & Integration Tests**
- **Docker Compose** – ready for production deployment

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│  REST API (ChargePointResource, CommandResource,            │
│   TransactionResource, HealthResource)                      │
├─────────────────────────────────────────────────────────────┤
│  Command Pattern (18 OcppCommand implementations)           │
├─────────────────────────────────────────────────────────────┤
│  OcppOutboundService → ChargePointRegistry                  │
│  → OpenConnections → WebSocket                              │
├─────────────────────────────────────────────────────────────┤
│  OcppWebSocketServer → Handlers (BootNotification,          │
│   StartTransaction, StopTransaction, Authorize, etc.)       │
├─────────────────────────────────────────────────────────────┤
│  Persistence (ChargePoint, Transaction → PostgreSQL)        │
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

#### JVM Mode (lokales Bauen, Dev/Test)

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

#### Native Mode (fertiges Image von GHCR, Production)

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
- **Health checks** for both services

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

**Available Commands (18):**

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

**Example – Reset:**

```bash
curl -X POST http://localhost:8080/api/chargepoints/CP-001/commands/reset \
  -H "Content-Type: application/json" \
  -d '{"type": "Soft"}'
```

## OCPP 1.6J Messages

### Client → Server (implemented)

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

### Server → Client (18 commands)

All 18 OCPP 1.6J remote calls implemented (see table above).

## Database Migrations

Liquibase manages the database schema via SQL changelogs in `src/main/resources/db/changelog/`.

```
db/changelog/
├── changelog-master.yaml   # Master changelog
└── 001-init.sql            # Initial schema (tables, sequences, indexes)
```

New migrations: add `00X-name.sql` to `db/changelog/` and include it in `changelog-master.yaml`.

## Project Structure

```
src/main/kotlin/org/tekeli/borisp/ocpp16/
├── command/            # 18 OcppCommand implementations
├── handler/            # C→P message handlers (10)
├── outbound/           # S→C service layer
├── persistence/        # Entities & repository
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

## CI/CD Pipeline

GitHub Actions automatisch bei jedem Push und Pull Request:

| Job | Trigger | Beschreibung |
|-----|---------|-------------|
| **Test** | push + PR | `mvn verify` mit PostgreSQL (Dev Services) |
| **Mutation Test** | push | PITest + HTML Report als Artifact |
| **Docker JVM** | push | JVM Image → GHCR (`latest`, `sha-xxx`) |
| **Docker Native** | push | GraalVM Native Image → GHCR (`latest-native`, `sha-xxx-native`) |

### Docker Images von GHCR

```bash
# JVM Image (empfohlen für Dev/Test)
docker pull ghcr.io/org-tekeli-borisp/ocpp16-ws-backend:latest

# Native Image (minimaler Footprint, schnellster Start)
docker pull ghcr.io/org-tekeli-borisp/ocpp16-ws-backend:latest-native
```

## Prometheus Metrics

Quarkus Micrometer mit Prometheus Registry. Metriken verfügbar unter `/metrics`:

```bash
curl http://localhost:8080/metrics
```

### OCP-spezifische Metriken:

| Metrik | Typ | Beschreibung |
|--------|-----|-------------|
| `ocpp_transactions_started_total` | Counter | Gestartete Ladetransaktionen |
| `ocpp_transactions_stopped_total` | Counter | Beendete Transaktionen |
| `ocpp_energy_delivered_wh` | Counter | Gelieferte Energie (Wh) |
| `ocpp_messages_received_total` | Counter | C→S Messages |
| `ocpp_messages_sent_total` | Counter | S→C Commands |
| `ocpp_charge_points_connected` | Gauge | Aktive WS-Verbindungen |
| `ocpp_transactions_active` | Gauge | Laufende Transaktionen |
| `ocpp_transaction_duration_seconds` | Timer | Dauer der Transaktionen |

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

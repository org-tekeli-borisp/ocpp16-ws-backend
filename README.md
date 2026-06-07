# OCPP 1.6J WebSocket Backend

OCPP 1.6J (JSON over WebSocket) Charge Point Central System implemented in Kotlin with Quarkus.

## Features

- **OCPP 1.6J compliant** – all Client→Server and Server→Call messages
- **WebSocket transport** – `ws://localhost:8181/ocpp/{chargePointId}`
- **18 Remote Commands** – full Server→Client control via REST API
- **Persistent storage** – H2 database (configurable for PostgreSQL, MySQL, etc.)
- **REST API** – charge points, transactions, commands, health & status
- **Mutation Testing** – PITest integration (79%+ mutation score)
- **430+ Unit & Integration Tests**

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
│  Persistence (ChargePoint, Transaction → H2/SQL)            │
└─────────────────────────────────────────────────────────────┘
```

## Quick Start

### Prerequisites

- JDK 21+
- Maven 3.8+

### Run in Dev Mode

```bash
./mvnw quarkus:dev
```

The server starts on `http://localhost:8181`.

### Connect a Charge Point

```
ws://localhost:8181/ocpp/{chargePointId}
```

Example BootNotification:

```json
[2,"1","BootNotification",{"chargePointVendor":"Tesla","chargePointModel":"Model3","firmwareVersion":"1.0"}]
```

### Package & Run

```bash
./mvnw package
java -jar target/quarkus-app/quarkus-run.jar
```

## REST API

### Health & Status

| Method | Endpoint | Description |
|--------|----------|-------------|
| `GET` | `/health` | Health check (status, uptime, connected) |
| `GET` | `/api/status` | Detailed system status |

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
curl -X POST http://localhost:8181/api/chargepoints/CP-001/commands/reset \
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
# Run all tests
./mvnw verify

# Mutation testing (PITest)
./mvnw org.pitest:pitest-maven:mutationCoverage
```

## Configuration

Key properties in `application.properties`:

| Property | Default | Description |
|----------|---------|-------------|
| `quarkus.http.port` | `8181` | HTTP/WS port |
| `quarkus.datasource.db-kind` | `h2` | Database type |
| `quarkus.hibernate-orm.database.generation` | `update` | Schema generation |

## Tech Stack

- **Kotlin** 2.3 – primary language
- **Quarkus** 3.36 – application framework
- **WebSocket Next** – server-sent WebSocket API
- **Hibernate ORM + Panache** – persistence
- **H2** – embedded database
- **Jackson** – JSON serialization
- **PITest** – mutation testing

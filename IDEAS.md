# Ideas & Roadmap

Kanban board for features, improvements, and long-term plans.

## 🔴 Backlog

| Issue | Description |
|-------|-------------|
| #001 | **Real authorization logic** — AuthorizeHandler always returns `Accepted`; add `IdToken` entity, whitelist/blacklist, validation |
| #002 | **WSS/TLS support** — add secure WebSocket for Security Edition 4 compliance |
| #003 | **Persist MeterValues** — currently acknowledged but lost; add entity with configurable retention |
| #004 | **Webhook/event system** — notifications for transactions, faults, offline charge points |
| #005 | **Local authorization list** — SendLocalList has no backing store; add entity + REST endpoints |
| #006 | **Smart charging engine** — load balancing across charge points, grid capacity management |
| #007 | **Firmware upload endpoint** — `POST /api/chargepoints/{id}/firmware` with validation |
| #008 | **Security logs API** — expose existing `SecurityLog` entity via REST |
| #009 | **Reservation persistence** — ReserveNow creates no persistent record |
| #010 | **Multi-instance support** — shared `ChargePointRegistry` via Redis |
| #011 | **Offline message queue** — commands delivered when charge point reconnects |
| #012 | **OpenTelemetry tracing** — correlation IDs per charge point |
| #013 | **Fleet-wide bulk commands** — `POST /api/commands/bulk` for multiple charge points |
| #014 | **Configuration reconciliation** — validate applied config via GetConfiguration audits |
| #015 | **Manual WebSocket disconnect** — `DELETE /api/chargepoints/{id}/connection` (single), `POST /api/chargepoints/reconnect-all` (bulk); simple force disconnect |

## 🟡 Prioritized

| Issue | Description |
|-------|-------------|

## 🟢 In Progress

| Issue | Description |
|-------|-------------|

## 🔵 Review

| Issue | Description |
|-------|-------------|

## ✅ Done

| Issue | Description |
|-------|-------------|

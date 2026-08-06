# Ideas & Roadmap

Kanban board for features, improvements, and long-term plans.

## 🔴 Critical (Sicherheit)

| Issue | Description |
|-------|-------------|
| #001 | **Real authorization logic** — `AuthorizeHandler` always returns `Accepted`; add `IdToken` entity, whitelist/blacklist, per-charge-point ACL |
| #016 | **REST API Authentication** — kein Auth/Authorization auf REST-Endpoints; jeder mit Netzwerkkontakt kann Commands ausführen |
| #017 | **Rate Limiting** — kein Schutz gegen Missbrauch auf REST und WebSocket |

## 🟠 High (Kernfunktionalität)

| Issue | Description |
|-------|-------------|
| #003 | **Persist MeterValues** — aktuell acknowledged but lost; `MeterValue` entity mit konfigurierbarer Retention hinzufügen |
| #011 | **Offline message queue** — Commands an disconnectete Charge Points schlagen sofort fehl; Queue für nachgelieferte delivery |
| #008 | **Security logs REST + WebUI** — `SecurityLog` entity existiert, ist aber über REST und UI nicht zugreifbar; Panel + Filter hinzufügen |
| #007 | **Firmware upload endpoint** — `POST /api/chargepoints/{id}/firmware/upload` mit Binary-Upload und Validierung; `FirmwareArtifact` entity |
| #018 | **IdToken REST + WebUI** — CRUD-Endpoints für ID-Tokens + Management-Panel in der WebUI |

## 🟡 Medium (Operative Reife)

| Issue | Description |
|-------|-------------|
| #004 | **Webhook/event system** — notifications für Transaktionen, Fehler, offline Charge Points; konfigurierbare Endpoints |
| #019 | **Audit Logging** — kein Trail für Admin-Aktionen (wer, welche Command, welcher ChargePoint, wann) |
| #013 | **Fleet-wide bulk commands** — `POST /api/commands/bulk` für mehrere Charge Points gleichzeitig |
| #009 | **Reservation persistence** — `ReserveNow` erstellt keinen persistenten Eintrag; `Reservation` entity + expiry monitoring |
| #020 | **Connector Status History** — nur letzter Status gespeichert; History-Tabelle für Zeitverlauf der Connector-Zustände |
| #021 | **StopTransaction validation** — nicht gegen aktiven Transaktionszustand geprüft; `transactionId` muss zu laufender Transaktion passen |
| #012 | **OpenTelemetry tracing** — correlation IDs pro Charge Point für Request-Tracing |
| #014 | **Configuration reconciliation** — validierte, angewendete Konfiguration via `GetConfiguration` audits |
| #005 | **Local authorization list** — `SendLocalList` hat keine backing store; `LocalAuthListEntry` entity + REST endpoints |
| #022 | **Fleet Dashboard** — Gesamtübersicht aller Ladepunkte mit aggregierten Charts (online count, active transactions, total energy) |
| #023 | **Certificate Management WebUI** — Panel für Install, Query, Delete von Zertifikaten über die UI |
| #024 | **Analytics & Reports** — Energieverbrauch-Charts, Transaktionsberichte, CSV/Excel Export |
| #025 | **Scheduled Operations** — cron-ähnliche Tasks für Firmware-Updates, Diagnostics Collection, Configuration Changes |

## 🟢 Nice-to-Have

| Issue | Description |
|-------|-------------|
| #006 | **Smart charging engine** — load balancing über Charge Points, grid capacity management |
| #002 | **WSS/TLS support** — secure WebSocket für Security Edition 4 Compliance (in Prod via reverse proxy) |
| #010 | **Multi-instance support** — shared `ChargePointRegistry` via Redis für horizontales Scaling |
| #026 | **OCSP/CRL Validation** — Zertifikate werden gespeichert, aber nicht aktiv validiert |
| #027 | **Dark Mode** — UI theme toggle |
| #028 | **Mobile Responsive Design** — aktuelles Layout ist desktop-fokussiert |

## 🔵 Review

| Issue | Description |
|-------|-------------|

## ✅ Done

| Issue | Description |
|-------|-------------|
| #015 | **Manual WebSocket disconnect** — `DELETE /api/chargepoints/{id}/connection` (single), `POST /api/chargepoints/reconnect-all` (bulk) + WebUI disconnect buttons |

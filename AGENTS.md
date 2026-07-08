# AGENTS.md — OCPP 1.6J WebSocket Backend

## ⚠️ TDD-ZWANG (vor JEDER Code-Änderung abarbeiten)

Dieses Projekt folgt **striktem Test-Driven Development**. Jeder Agent (einschließlich mir) muss vor dem ersten Production-Code die folgende Checkliste abgearbeitet haben:

### Pre-Task Checklist (vor Production-Code zwingend ausführen)

1. **Lies diese Datei (AGENTS.md) komplett** – insbesondere den TDD-Abschnitt
2. **Existiert bereits ein Test, der das gewünschte Verhalten abdeckt?**
   - JA → Test läuft grün? → Weiter mit Production-Code
   - JA → Test läuft rot? → Production-Code schreiben, bis Test grün
   - NEIN → **Test schreiben (RED)**, der das neue Verhalten fordert
3. **Test muss fehlschlagen** – `mvn test -Dtest={TestName}` zeigt Fehler
4. **Erst dann** Production-Code schreiben (GREEN)
5. **`mvn test`** – alle Tests grün
6. **Refaktorieren** nur wenn alle Tests grün sind

### Sanktion bei Verstoß

Wird Production-Code ohne vorherigen roten Test committet, muss der Agent den Code **komplett zurückrollen** und die Checkliste von vorne durchlaufen. Keine Ausnahmen.

---

## Project Overview

OCPP 1.6J Charge Point Central System in **Kotlin** with **Quarkus**. 1126 tests, 24 remote commands, 15 message handlers, full OCPP 1.6 Security Edition 4 support.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3 (JVM target 25) |
| Framework | Quarkus 3.36 |
| DB | PostgreSQL 18 + Liquibase |
| WebSocket | Quarkus WebSocket Next |
| Persistence | Hibernate ORM + Panache |
| JSON | Jackson |
| Testing | JUnit 5, RestAssured, MockK, PITest |
| Metrics | Micrometer + Prometheus |

## Key Commands

```bash
# Dev mode (auto-starts PostgreSQL via Dev Services)
mvn quarkus:dev

# Run all tests
mvn test

# Verify (tests + packaging)
mvn verify

# CRAP score check (complexity + coverage threshold)
mvn verify -Pcrap

# Mutation testing (PITest)
mvn org.pitest:pitest-maven:mutationCoverage

# Build JVM image
mvn package

# Build native image
mvn package -Pnative
```

## Project Structure

```
src/main/kotlin/org/tekeli/borisp/ocpp16/
├── command/            # 24 OcppCommand impls (18 standard + 6 security)
├── handler/            # 15 OcppActionHandler impls (10 standard + 5 security)
├── outbound/           # Server→ChargePoint service layer
├── persistence/        # Entities + repositories
├── protocol/           # OCPP message types, ResponseAwaiter
├── rest/               # REST API resources
├── websocket/          # WebSocket server, registry, dispatcher
├── health/             # Liveness + readiness checks
└── metrics/            # Prometheus metrics service
```

## Architecture

```
REST API → OcppCommand → OutboundCallDispatcher → ChargePointRegistry → WebSocket
                                                                         ↑
WebSocket ← OcppResponse ← ResponseAwaiter ← Handler ← OcppWebSocketServer
```

### Key Interfaces

- **`OcppCommand`** (`command/OcppCommand.kt`): `name`, `validate(payload)`, `execute(chargePointId, payload)`
- **`OcppActionHandler`** (`handler/OcppActionHandler.kt`): `handle(call, context)` → returns JSON response string
- **`OcppHandlerContext`**: shared context for handlers (persistence, metrics, outbound)

### Message Flow (Client→Server)

1. `OcppWebSocketServer.onMessage()` receives raw JSON
2. `MessageDispatcher` routes to the correct `OcppActionHandler` by action name
3. Handler parses payload via `PayloadParser`, validates, processes, persists
4. Handler returns JSON response string sent back over WebSocket

### Message Flow (Server→Client)

1. REST API calls `CommandResource.execute()`
2. `OcppCommand.validate()` checks payload
3. `OcppCommand.execute()` calls `OcppOutboundService`
4. `OutboundCallDispatcher` sends OCPP Call via WebSocket, waits for response
5. `ResponseAwaiter` resolves when matching response arrives

## Code Conventions

- **No comments** unless explicitly requested
- **Kotlin idiomatic**: data classes, extension functions, null safety
- **Package structure**: `org.tekeli.borisp.ocpp16.<layer>`
- **Naming**: `*Command` for server→client, `*Handler` for client→server
- **Constants**: Centralized in `OcppConstants.kt`
- **Error responses**: Use `FormationViolationException` for protocol violations
- **Validation**: Centralized in `PayloadValidators.kt` for shared validators

## Testing Conventions

- **Unit tests**: Mock external dependencies with MockK
- **Integration tests**: `@QuarkusTest` with Dev Services PostgreSQL
- **WebSocket tests**: Vertx WebSocket client against running server
- **REST tests**: RestAssured against running server
- **Test naming**: `{Subject}{Scenario}Test.kt` — focused, single-responsibility files
- **Constants**: Raw OCPP JSON messages as `private const val` at file top

### Strict TDD Workflow

All new functionality must strictly follow the Red-Green-Refactor cycle:

1. **RED** — Write the failing test FIRST, before any production code exists
   - Test defines the expected behavior
   - Test must compile (interfaces/stub implementations are allowed)
   - Test must fail with an assertion error (not just a compilation error)

2. **GREEN** — Minimal implementation to make the test pass
   - Only as much code as necessary, nothing speculative
   - No refactoring in this phase
   - Do not write code ahead for the next test

3. **REFACTOR** — Only when test is green
   - Improve code quality without changing behavior
   - Remove duplicates, improve naming
   - Tests must still pass

**Rules:**
- Never write production code without a preceding failing test
- Never write multiple tests in a row without implementing in between
- Always validate the current test with `mvn test -Dtest={TestClassName}`
- For new features: Test → Implementation → Test → Implementation (alternating)
- For bug fixes: Reproducing test first, then fix

### Test File Pattern

```
OcppWebSocketServer{Action}Test.kt     — per-handler tests
{Subject}CommandTest.kt                — command tests
{Subject}HandlerTest.kt                — handler unit tests
{Subject}RepositoryTest.kt             — persistence tests
{Subject}ResourceTest.kt               — REST endpoint tests
```

## Adding a New Handler

1. Create `src/main/kotlin/.../handler/{Action}Handler.kt` implementing `OcppActionHandler`
2. Register in `OcppWebSocketServer` handler map
3. Add tests in `src/test/kotlin/.../OcppWebSocketServer{Action}Test.kt`
4. Update `OcppConstants.kt` if new action name constant needed

## Adding a New Command

1. Create `src/main/kotlin/.../command/{Name}Command.kt` implementing `OcppCommand`
2. Register in `OutboundCallDispatcher` command map
3. Add endpoint in `CommandResource.kt`
4. Add tests in `src/test/kotlin/.../command/OcppCommandTest.kt`

## Database

- Migrations in `src/main/resources/db/changelog/`
- New migration: add `00X-name.sql`, include in `changelog-master.yaml`
- Dev/Test: Quarkus Dev Services auto-provisions PostgreSQL

## CI/CD

- All pushes/PRs: `mvn verify`
- Pushes only: PITest mutation, Docker JVM/Native → GHCR

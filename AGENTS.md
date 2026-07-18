# AGENTS.md — OCPP 1.6J WebSocket Backend

## ⚠️ TDD ENFORCEMENT (mandatory before ANY code change)

This project follows **strict Test-Driven Development**. Every agent (including me) must complete the following checklist before writing any production code:

### Pre-Task Checklist (MUST run before production code)

1. **Read this file (AGENTS.md) completely** – especially the TDD section
2. **Does a test already exist that covers the desired behavior?**
   - YES → Test passes (green)? → Proceed to production code
   - YES → Test fails (red)? → Write production code until test passes
   - NO → **Write a test (RED)** that demands the new behavior
3. **Test MUST fail** – `mvn test -Dtest={TestName}` shows failure
4. **Only then** write production code (GREEN)
5. **`mvn test`** – all tests green
6. **Refactor** only when all tests are green

### Penalty for Violation

If production code is committed without a preceding failing test, the agent MUST **roll back the code completely** and re-run the checklist from the start. No exceptions.

---

## Project Overview

OCPP 1.6J Charge Point Central System in **Kotlin** with **Quarkus**. 1323 tests (74 files), 24 remote commands, 15 message handlers, full OCPP 1.6 Security Edition 4 support, 78 JSON schemas.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Kotlin 2.3 (JVM target 25) |
| Framework | Quarkus 3.36 |
| DB | PostgreSQL 18 + Liquibase |
| WebSocket | Quarkus WebSocket Next |
| Persistence | Hibernate ORM + Panache |
| JSON | Jackson |
| Testing | JUnit 5, RestAssured, MockK, PITest, Playwright |
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
├── protocol/           # OCPP message types, ResponseAwaiter, MessageCaptureService
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
- **`SchemaValidator`** (`protocol/SchemaValidator.kt`): loads 78 JSON schemas (66 standard + 22 security, draft-04 + draft-06), `validate(actionName, payloadJson)` → `List<String>`

### Two-Layer Validation

All OCPP payloads go through two validation layers:

1. **Schema Validation** (`SchemaValidator`): structural checks (required fields, types, additionalProperties, maxLength, enums) — runs in `MessageDispatcher` (WebSocket) and `CommandResource` (REST)
2. **Manual Validation**: business logic (empty strings, connectorId ranges, custom constraints) — runs in each handler/command

When adding new validation logic, determine which layer it belongs to:
- Schema-level: update the JSON schema file
- Business-level: add to handler/command validation

### Message Flow (Client→Server)

1. `OcppWebSocketServer.onMessage()` receives raw JSON
2. `MessageDispatcher` routes to the correct `OcppActionHandler` by action name
3. Handler parses payload via `PayloadParser`, validates, processes, persists
4. Handler returns JSON response string sent back over WebSocket

### Message Flow (Server→Client)

1. REST API calls `CommandResource.execute()`
2. `SchemaValidator` validates payload against JSON schema
3. `OcppCommand.validate()` checks business logic
4. `OcppCommand.execute()` calls `OcppOutboundService`
5. `OutboundCallDispatcher` sends OCPP Call via WebSocket, waits for response
6. `ResponseAwaiter` resolves when matching response arrives

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
OcppWebSocketServer{Action}Test.kt     — per-handler WebSocket tests
{Subject}CommandTest.kt                — command tests
{Subject}HandlerTest.kt                — handler unit tests
{Subject}RepositoryTest.kt             — repository persistence tests
{Subject}EntityTest.kt                 — entity unit tests
{Subject}ServiceTest.kt                — service tests
{Subject}ResourceTest.kt               — REST endpoint tests
{Subject}MutationTest.kt               — targeted PITest mutation tests
{Subject}IntegrationTest.kt            — full-flow integration tests
{Subject}HealthCheckTest.kt            — health probe tests
{Subject}DispatcherTest.kt             — dispatcher tests
{Subject}RegistryTest.kt               — registry tests
{Subject}AwaiterTest.kt                — response awaiter tests
{Subject}Test.kt                       — generic component tests (Metrics, Protocol, etc.)
```

## Adding a New Handler

1. Create `src/main/kotlin/.../handler/{Action}Handler.kt` implementing `OcppActionHandler`
2. Register in `OcppWebSocketServer` handler map
3. Add tests in `src/test/kotlin/.../OcppWebSocketServer{Action}Test.kt`
4. Update `OcppConstants.kt` if new action name constant needed

## Adding a New Command

1. Create `src/main/kotlin/.../command/{Name}Command.kt` implementing `OcppCommand`
2. CDI discovers commands automatically via `Instance<OcppCommand>` — no manual registration needed
3. Add tests in `src/test/kotlin/.../command/OcppCommandTest.kt`

## Database

- Migrations in `src/main/resources/db/changelog/`
- New migration: add `00X-name.sql`, include in `changelog-master.yaml`
- Dev/Test: Quarkus Dev Services auto-provisions PostgreSQL

## CI/CD

- All pushes/PRs: `mvn verify`, E2E tests (Playwright), CRAP analysis
- Pushes only: PITest mutation, Docker JVM/Native → GHCR
- Main pushes only: JaCoCo + mutation reports → GitHub Pages
- **Reports**: [JaCoCo](https://org-tekeli-borisp.github.io/ocpp16-ws-backend/jacoco/index.html) | [PITest Mutation](https://org-tekeli-borisp.github.io/ocpp16-ws-backend/mutation/index.html)

# Draughts

A web draughts (English draughts / American checkers) game: play the computer or another person in real time.
See [SPEC.md](SPEC.md) for the full specification.

| Part | Stack |
|---|---|
| `backend/` | Java 25, Spring Boot 4.1 (Web MVC, WebSocket/STOMP, JDBC, Flyway, Actuator), PostgreSQL |
| `frontend/` | React 19, TypeScript, Vite, TanStack Query, Zustand, react-i18next, STOMP.js |

## Quick start

Requirements: JDK 25, Node 24, Docker (for PostgreSQL and the integration tests).

```bash
docker compose up -d db                 # PostgreSQL on localhost:5432

cd backend && ./mvnw spring-boot:run    # API on http://localhost:8080
cd frontend && npm install && npm run dev   # app on http://localhost:5173
```

Or run everything in one container: `docker compose up --build`, then open http://localhost:8080.

API docs (Swagger UI): http://localhost:8080/api/docs, OpenAPI JSON at `/api/openapi`.

## Deploy

GitHub Pages cannot host this (it needs a Java server, PostgreSQL and WebSockets). `render.yaml` is a
[Render](https://render.com) Blueprint for one Docker web service plus a PostgreSQL database:

1. Sign in to Render, choose **New → Blueprint**, and select this repository.
2. Apply it. Render builds the `Dockerfile`, creates the database, and generates the identity secret.
3. Open the `*.onrender.com` URL.

On Render's free plan the service sleeps after inactivity (the first request takes ~1 minute) and the free
database expires after 30 days; use a paid plan for anything lasting. The same image runs on Fly.io,
Railway, Koyeb or any Docker host: set `DATABASE_URL` (or `DB_*`), `DRAUGHTS_IDENTITY_SECRET` and
`DRAUGHTS_SECURE_COOKIE=true`.

## Tests

```bash
cd backend && ./mvnw verify      # unit, property, architecture and integration tests
cd frontend && npm run lint && npm run typecheck && npm test
```

Backend integration tests start PostgreSQL with Testcontainers. To use an existing database instead, set
`DRAUGHTS_TEST_DATABASE_URL` (and optionally `DRAUGHTS_TEST_DATABASE_USERNAME` / `_PASSWORD`).

## Architecture

The backend follows **hexagonal architecture** (ports and adapters). Dependencies point inwards only;
`ArchitectureTest` enforces this on every build.

```
backend/src/main/java/com/draughts/
  domain/                      Framework-free core (plain Java + Lombok)
    engine/                    Rules: bitboards, move generation, draw rules, FEN, Zobrist
    game/                      Game aggregate: join, play, resign, draw offers, invariants, versioning
    player/                    Player
  application/
    port/in/                   Use cases the outside world calls (CreateGameUseCase, PlayMoveUseCase, ...)
    port/out/                  What the core needs (GameRepository, PlayerRepository, GameEventPublisher,
                               ComputerPlayer)
    service/                   Thin use-case implementations: load, call the aggregate, save, publish
  adapter/
    in/web/                    REST + STOMP controllers, DTOs, view mapping, guest identity cookie
    in/scheduling/             Triggers computer moves after commits and on a recovery sweep
    out/persistence/           JdbcClient + PostgreSQL implementations of the repositories
    out/ai/                    Alpha-beta search implementing ComputerPlayer
    out/event/                 GameEventPublisher on Spring application events
  config/                      Composition root: registers rule sets, clock, random source
backend/src/main/resources/db/migration/   Flyway migrations
```

Design notes:

- **Rich domain model.** Business rules (whose turn, who may join, draw offers, game end) live on the
  immutable `Game` aggregate, so use-case services stay a few lines long.
- **Single responsibility / interface segregation.** One small inbound port per use case; controllers
  depend on those ports, never on services. Commands and queries use separate controllers.
- **Dependency inversion.** The core defines `ComputerPlayer`, `GameRepository`, etc.; adapters implement
  them. The AI can be swapped without touching the domain.
- **Open/closed.** A new variant is a new `RuleSet` bean; `Variants` picks it up.
- **Lombok** removes constructors (`@RequiredArgsConstructor`), loggers (`@Slf4j`), null checks
  (`@NonNull`), copy-and-modify code (`@Builder(toBuilder = true)`, `@With`) and utility-class boilerplate.

```
frontend/src/
  api/          Typed REST client, TanStack Query hooks, STOMP subscription
  features/     game (board, selection logic, controls), lobby, history
  components/   App layout
  store/        Persisted UI settings (theme, language, board orientation)
  locales/      English and Portuguese
```

## How it works

- **Server-authoritative.** The browser never decides legality: every `GameView` includes the legal moves,
  and the UI only lets you pick among them. Multi-jumps are entered one landing square at a time.
- **Move log as the source of truth.** `game_move` is append-only; the position is rebuilt by replaying it.
  `game.board_snapshot` (JSONB) is a derived copy for quick reads.
- **Optimistic concurrency.** Each game has a `version`; moves may send `expectedVersion` and get `409` if
  the game moved on.
- **Real time.** After each committed change the server publishes the game to `/topic/games/{id}`.
  The UI submits moves over REST (simpler error handling) and listens over STOMP; moves can also be sent to
  `/app/games/{id}/move`.
- **Computer opponent.** Runs on a small dedicated thread pool after each human move; a periodic sweep
  resumes games after a restart. Difficulty sets depth, time budget and randomness.
- **Guest identity.** Each browser gets a signed, HttpOnly, SameSite=Lax cookie holding its player id.

## Configuration

| Variable | Default | Purpose |
|---|---|---|
| `DATABASE_URL` | built from `DB_HOST`/`DB_PORT`/`DB_NAME` (`localhost:5432/draughts`) | JDBC URL; `DB_USER` / `DB_PASSWORD` also work |
| `PORT` | `8080` | HTTP port |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `draughts` / `draughts` | Database credentials |
| `DRAUGHTS_IDENTITY_SECRET` | dev-only value | HMAC key for the identity cookie (min. 32 chars). **Set in every deployment.** |
| `DRAUGHTS_SECURE_COOKIE` | `false` | Mark the cookie `Secure` (enable behind HTTPS) |
| `DRAUGHTS_ALLOWED_ORIGINS` | `http://localhost:5173` | Extra origins allowed to open WebSocket connections |

## Not built yet

Compared with SPEC.md, these are deliberately left for later milestones:

- Spring Security / OAuth login (guests only, via the signed cookie), rate limiting.
- Generated TypeScript client from OpenAPI (types in `frontend/src/api/types.ts` are hand-written).
- Redis for multi-instance WebSocket fan-out; the in-memory STOMP broker assumes a single instance.
- Clocks, ratings, replay viewer, other variants, Playwright E2E tests in CI.

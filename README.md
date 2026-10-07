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

## Tests

```bash
cd backend && ./mvnw verify      # unit, property, architecture and integration tests
cd frontend && npm run lint && npm run typecheck && npm test
```

Backend integration tests start PostgreSQL with Testcontainers. To use an existing database instead, set
`DRAUGHTS_TEST_DATABASE_URL` (and optionally `DRAUGHTS_TEST_DATABASE_USERNAME` / `_PASSWORD`).

## Layout

```
backend/src/main/java/com/draughts/
  engine/       Pure rules engine: bitboards, move generation, draw rules, FEN, Zobrist. No dependencies.
  ai/           Negamax + alpha-beta, iterative deepening, transposition table. Depends on engine only.
  game/         Game aggregate, GameService (create/join/move/resign/draw), AI scheduler, repository port.
  player/       Guest players.
  persistence/  JDBC adapters (JdbcClient) for the game and player ports.
  web/          REST controllers, STOMP handlers, DTOs, problem details, guest identity cookie.
backend/src/main/resources/db/migration/   Flyway migrations.

frontend/src/
  api/          Typed REST client, TanStack Query hooks, STOMP subscription.
  features/     game (board, selection logic, controls), lobby, history.
  components/   App layout.
  store/        Persisted UI settings (theme, language, board orientation).
  locales/      English and Portuguese.
```

The `engine` → `ai` → `game` → adapters dependency rule is enforced by `ArchitectureTest`.

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
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/draughts` | JDBC URL |
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

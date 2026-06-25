## Reactive Rumble: Distributed Multiplayer Snake

A real-time multiplayer Snake game built with **Spring Boot WebFlux**, **Reactive Redis**, and a **React** frontend. Players register an account, log in, and compete on a shared game board with persistent leaderboard scores.

### Architecture

Originally built with local in-memory state and a Vanilla JS frontend, the project has evolved through two major phases:

- **Phase 2:** Refactored to a stateless backend using Redis as the single source of truth, enabling horizontal scaling.
- **Phase 3:** Added a React + Tailwind CSS frontend and a full authentication system (JWT + BCrypt).

### Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 21, Spring Boot 4, Spring WebFlux, Spring Security |
| Auth | JWT (JJWT 0.12.6, HS256), BCrypt password hashing |
| Database | Redis (Reactive Stack) |
| Frontend | React 19, Vite, Tailwind CSS |
| Realtime | Server-Sent Events (SSE) |

### Key Features

**Authentication**
- **Register** — creates a new account with a unique username, password (BCrypt-hashed), and snake color. Stored in Redis.
- **Login** — verifies credentials and returns a signed JWT valid for 24 hours.
- **`JwtAuthFilter`** — a `WebFilter` that runs on every request, validates the JWT signature, and populates Spring's `ReactiveSecurityContextHolder` with the user ID. Controllers read the identity from the security context rather than accepting an `?id=` parameter.
- `/game/stream` (SSE) is permit-all because the browser `EventSource` API cannot set custom headers.

**Game Engine**
- **Stateless Game Logic:** All active game entities (snakes and food) are stored in Redis. State survives server restarts.
- **Reactive Event Loop:** Non-blocking 10 Hz heartbeat (`Flux.interval` + `flatMap`) coordinates async Redis reads and writes every 100 ms.
- **Collision Detection:** Wall, self, and player-vs-player collisions resolved each tick across all active entities from Redis.

**Distributed State (Redis Key Design)**

| Key | Type | Purpose |
|---|---|---|
| `user:{uuid}` | String (JSON) | Full user account record |
| `user:username:{name}` | String | Username → UUID index for login lookup |
| `player:profile:{id}` | Hash | Persistent snake color, loaded on join |
| `game:active_players` | Hash | Live player states (body, direction, color) |
| `game:leaderboard` | Sorted Set | High scores, ranked by snake length |
| `game:food` | String (JSON) | Current food position |

### Getting Started

**Prerequisites**
- Java 21+
- Node.js 18+
- Redis running on port 6379

**Run backend and frontend separately (development)**
```bash
# Terminal 1 — Spring Boot
./gradlew bootRun

# Terminal 2 — React dev server (proxies /auth and /game to localhost:8080)
cd src/frontend
npm install
npm run dev
```

Open `http://localhost:5173`, register an account, and open multiple tabs to see distributed sync in action.

**Run as a single artifact (production)**
```bash
./gradlew build   # builds frontend and copies it into src/main/resources/static
java -jar build/libs/reactive-rumble-*.jar
```

Open `http://localhost:8080`.

**Environment variables**

| Variable | Default | Description |
|---|---|---|
| `JWT_SECRET` | dev key (base64) | HS256 signing secret — **override in production** |
| `spring.data.redis.host` | `localhost` | Redis host |
| `spring.data.redis.port` | `6379` | Redis port |

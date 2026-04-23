## Reactive Rumble (Phase 1)

**Reactive Rumble** is a real-time, multiplayer Snake game built with a **Reactive Systems** architecture. It leverages non-blocking I/O to maintain a high-concurrency "game heartbeat" where game state is synchronized across all connected clients with minimal latency.

### Architecture Overview (Phase 1)

* **Reactive Heartbeat:** Uses **Project Reactor** (`Flux.interval`) to drive a server-side authoritative game loop at 10Hz (100ms ticks).
* **Uni-directional Data Flow:** **Downstream:** Server-Sent Events (**SSE**) stream full `GameState` updates to clients in real-time.
    * **Upstream:** RESTful API endpoints handle player joining and asynchronous movement inputs.
* **State Management:** Utilizes **AtomicReferences** and **Concurrent Maps** to ensure thread-safe state transitions and consistency across asynchronous user events.
* **Authoritative Logic:** All collision detection (walls, self, and opponents), food spawning, and snake growth are calculated server-side to prevent client-side cheating.

### Tech Stack
* **Backend:** Java 25, Spring Boot 3 (WebFlux), Project Reactor, Lombok.
* **Frontend:** HTML5 Canvas, Vanilla JavaScript (EventSource API).
* **Build Tool:** Gradle.
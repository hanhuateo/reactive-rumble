## Snake-Redis: Distributed Reactive Game Engine

A high-performance, real-time multiplayer Snake game built with **Spring Boot WebFlux** and **Reactive Redis**. 

### Architecture (Phase 2 Evolution)
Originally built with local in-memory state, this project has been refactored into a **stateless architecture**. The game engine can now be scaled horizontally across multiple server instances while maintaining a single global "Source of Truth" for game state.

* **Backend:** Java 21, Spring Boot 4, Project Reactor (WebFlux).
* **Database:** Redis (Reactive Stack).
* **Frontend:** Vanilla JS with Server-Sent Events (SSE) for real-time updates.

### Key Technical Features
* **Stateless Game Logic:** All active game entities (Snakes and Food) are stored and managed in Redis. If the server restarts, the game state persists.
* **Reactive Event Loop:** Utilizes a non-blocking 10Hz heartbeat (100ms ticks) powered by `Flux.interval` and `flatMap` to coordinate asynchronous Redis operations.
* **Distributed State Management:**
    * **Player Profiles:** Stored as Redis Hashes for fast identity lookup.
    * **Active Players:** Managed via Redis Hashes to enable multi-server synchronization.
    * **Global Leaderboard:** Implemented using Redis **Sorted Sets (ZSETs)** for O(log(N)) ranking and real-time score updates.
    * **Shared Food State:** Synchronized across all instances via Redis String keys.
* **Non-Blocking Collision Detection:** Real-time spatial calculations performed across all active entities retrieved from the global Redis state.

### Getting Started
1. **Prerequisites:**
   * Java 21+
   * Redis Stack (running on port 6379)
2. **Run the App:**
   ```bash
   ./gradlew bootRun
   ```
3. **Join the Game:**
   Open `localhost:8080` in multiple tabs to see the distributed synchronization in action!

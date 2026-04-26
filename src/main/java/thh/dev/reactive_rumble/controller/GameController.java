package thh.dev.reactive_rumble.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import thh.dev.reactive_rumble.engine.GameEngine;
import thh.dev.reactive_rumble.model.Direction;
import thh.dev.reactive_rumble.model.GameState;
import thh.dev.reactive_rumble.service.LeaderboardService;
import thh.dev.reactive_rumble.service.PlayerService;

@RestController
@RequestMapping("/game")
public class GameController {
    private final GameEngine engine;
    private final PlayerService playerService;
    private final LeaderboardService leaderboardService;

    public GameController(GameEngine engine, PlayerService playerService, LeaderboardService leaderboardService) {
        this.engine = engine;
        this.playerService = playerService;
        this.leaderboardService = leaderboardService;
    }

    // Stream the game state to the frontend
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<GameState> streamGame() {
        return this.engine.getGameStream()
                .onBackpressureDrop(); // If client is slow, drop frames
    }

    // Handle player movement input
    @PostMapping("/move")
    public Mono<Void> move(@RequestParam String id, @RequestParam Direction dir) {
        this.playerService.updateDirection(id, dir);
        return Mono.empty();
    }

    @PostMapping("/join")
    public Mono<String> join(@RequestParam String id) {
        this.playerService.addPlayer(id);
        return Mono.just("Joined as " + id);
    }

    @GetMapping("/leaderboard")
    public Flux<Map<String, Object>> getLeaderboard() {
        return this.leaderboardService.getTopScores()
                .map(tuple -> Map.of(
                        "playerId", tuple.getValue(),
                        "score", tuple.getScore().intValue()));
    }
}

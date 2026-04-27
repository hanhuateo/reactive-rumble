package thh.dev.reactive_rumble.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import thh.dev.reactive_rumble.engine.GameEngine;
import thh.dev.reactive_rumble.model.Direction;
import thh.dev.reactive_rumble.model.GameState;
import thh.dev.reactive_rumble.service.LeaderboardService;
import thh.dev.reactive_rumble.service.PlayerService;
import thh.dev.reactive_rumble.service.ProfileService;

@Slf4j
@RestController
@RequestMapping("/game")
public class GameController {
    private final GameEngine engine;
    private final PlayerService playerService;
    private final LeaderboardService leaderboardService;
    private final ProfileService profileService;

    public GameController(GameEngine engine, PlayerService playerService, LeaderboardService leaderboardService,
            ProfileService profileService) {
        this.engine = engine;
        this.playerService = playerService;
        this.leaderboardService = leaderboardService;
        this.profileService = profileService;
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

    @PostMapping("/join/{id}")
    public Mono<String> join(@PathVariable String id) {
        return this.playerService.addPlayer(id).then(Mono.just("Joined as " + id));
    }

    @GetMapping("/leaderboard")
    public Flux<Map<String, Object>> getLeaderboard() {
        return this.leaderboardService.getTopScores()
                .map(tuple -> Map.of(
                        "playerId", tuple.getValue(),
                        "score", tuple.getScore().intValue()));
    }

    @PostMapping("/profile/{id}")
    public Mono<Void> saveProfile(@PathVariable String id, @RequestBody Map<String, String> profileData) {
        String username = profileData.getOrDefault("username", "Snakey");
        String color = profileData.getOrDefault("color", "#00ff00");
        return this.profileService.saveProfile(id, username, color);
    }
}

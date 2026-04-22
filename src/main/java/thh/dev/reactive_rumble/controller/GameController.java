package thh.dev.reactive_rumble.controller;

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
import thh.dev.reactive_rumble.service.PlayerService;

@RestController
@RequestMapping("/game")
public class GameController {
    private final GameEngine engine;
    private final PlayerService playerService;

    public GameController(GameEngine engine, PlayerService playerService) {
        this.engine = engine;
        this.playerService = playerService;
    }

    // Stream the game state to the frontend
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<GameState> streamGame() {
        return engine.getGameStream()
                .onBackpressureDrop(); // If client is slow, drop frames
    }

    // Handle player movement input
    @PostMapping("/move")
    public Mono<Void> move(@RequestParam String id, @RequestParam Direction dir) {
        playerService.updateDirection(id, dir);
        return Mono.empty();
    }

    @PostMapping("/join")
    public Mono<String> join(@RequestParam String id) {
        playerService.addPlayer(id);
        return Mono.just("Joined as " + id);
    }
}

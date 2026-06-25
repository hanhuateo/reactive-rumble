package thh.dev.reactive_rumble.controller;

import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
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
import thh.dev.reactive_rumble.service.UserService;

@Slf4j
@RestController
@RequestMapping("/game")
public class GameController {
    private final GameEngine engine;
    private final PlayerService playerService;
    private final LeaderboardService leaderboardService;
    private final ProfileService profileService;
    private final UserService userService;

    public GameController(GameEngine engine, PlayerService playerService, LeaderboardService leaderboardService,
            ProfileService profileService, UserService userService) {
        this.engine = engine;
        this.playerService = playerService;
        this.leaderboardService = leaderboardService;
        this.profileService = profileService;
        this.userService = userService;
    }

    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<GameState> streamGame() {
        return this.engine.getGameStream()
                .onBackpressureDrop();
    }

    @PostMapping("/move")
    public Mono<Void> move(@RequestParam Direction dir) {
        return userId().flatMap(id -> this.playerService.updateDirection(id, dir));
    }

    @PostMapping("/join")
    public Mono<String> join() {
        return userId().flatMap(id -> this.playerService.addPlayer(id).then(Mono.just("Joined as " + id)));
    }

    @GetMapping("/leaderboard")
    public Flux<Map<String, Object>> getLeaderboard() {
        return this.leaderboardService.getTopScores()
                .map(tuple -> Map.of(
                        "username", tuple.getValue(),
                        "score", tuple.getScore().intValue()));
    }

    @PostMapping("/profile")
    public Mono<Void> saveProfile(@RequestBody Map<String, String> profileData) {
        String newColor = profileData.get("color");
        return userId().flatMap(id -> this.userService.getUserById(id)
                .flatMap(user -> {
                    String color = newColor != null ? newColor : user.color();
                    return this.profileService.saveProfile(id, user.username(), color);
                }));
    }

    private Mono<String> userId() {
        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> (String) ctx.getAuthentication().getPrincipal());
    }
}

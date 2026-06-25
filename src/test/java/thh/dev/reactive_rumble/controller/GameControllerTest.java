package thh.dev.reactive_rumble.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import thh.dev.reactive_rumble.engine.GameEngine;
import thh.dev.reactive_rumble.model.Direction;
import thh.dev.reactive_rumble.model.GameState;
import thh.dev.reactive_rumble.model.Point;
import thh.dev.reactive_rumble.model.User;
import thh.dev.reactive_rumble.service.LeaderboardService;
import thh.dev.reactive_rumble.service.PlayerService;
import thh.dev.reactive_rumble.service.ProfileService;
import thh.dev.reactive_rumble.service.UserService;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GameControllerTest {

    @Mock GameEngine gameEngine;
    @Mock PlayerService playerService;
    @Mock LeaderboardService leaderboardService;
    @Mock ProfileService profileService;
    @Mock UserService userService;
    @InjectMocks GameController gameController;

    // Populates ReactiveSecurityContextHolder with userId "user-123" for the given Mono
    private <T> Mono<T> withAuth(Mono<T> mono) {
        var auth = new UsernamePasswordAuthenticationToken(
                "user-123", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
        return mono.contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
    }

    @Test
    void join_callsAddPlayer_withUserIdFromToken() {
        when(playerService.addPlayer("user-123")).thenReturn(Mono.empty());

        StepVerifier.create(withAuth(gameController.join()))
                .expectNextMatches(msg -> msg.contains("user-123"))
                .verifyComplete();
    }

    @Test
    void move_updatesDirectionForAuthenticatedUser() {
        when(playerService.updateDirection("user-123", Direction.UP)).thenReturn(Mono.empty());

        StepVerifier.create(withAuth(gameController.move(Direction.UP)))
                .verifyComplete();
    }

    @Test
    void leaderboard_returnsMappedScores() {
        ZSetOperations.TypedTuple<String> entry = ZSetOperations.TypedTuple.of("alice", 10.0);
        when(leaderboardService.getTopScores()).thenReturn(Flux.just(entry));

        StepVerifier.create(gameController.getLeaderboard())
                .expectNextMatches(map -> "alice".equals(map.get("username")) && Integer.valueOf(10).equals(map.get("score")))
                .verifyComplete();
    }

    @Test
    void saveProfile_updatesColorForAuthenticatedUser() {
        User alice = new User("user-123", "alice", "hashed", "#ff0000");
        when(userService.getUserById("user-123")).thenReturn(Mono.just(alice));
        when(profileService.saveProfile("user-123", "alice", "#00ff00")).thenReturn(Mono.empty());

        StepVerifier.create(withAuth(gameController.saveProfile(Map.of("color", "#00ff00"))))
                .verifyComplete();
    }

    @Test
    void streamGame_returnsSinkFlux() {
        when(gameEngine.getGameStream()).thenReturn(Flux.just(new GameState(Map.of(), new Point(5, 5))));

        StepVerifier.create(gameController.streamGame().take(1))
                .expectNextCount(1)
                .verifyComplete();
    }
}

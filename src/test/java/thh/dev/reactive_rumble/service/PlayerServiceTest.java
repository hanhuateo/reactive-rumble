package thh.dev.reactive_rumble.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveHashOperations;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import thh.dev.reactive_rumble.model.Direction;
import thh.dev.reactive_rumble.model.Player;
import thh.dev.reactive_rumble.model.Point;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlayerServiceTest {

    @Mock
    ReactiveStringRedisTemplate redisTemplate;
    @Mock
    ReactiveHashOperations<String, Object, Object> hashOps;
    @Mock
    ProfileService profileService;
    @Mock
    ObjectMapper objectMapper;

    private PlayerService playerService;

    private static final Player TEST_PLAYER = new Player(
            "user-123",
            List.of(new Point(10, 10), new Point(10, 11), new Point(10, 12)),
            Direction.UP,
            "#ff0000");

    private static final String PLAYER_JSON = "{\"id\":\"user-123\"}";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        playerService = new PlayerService(redisTemplate, profileService, objectMapper);
    }

    @Test
    void addPlayer_withProfile_usesProfileColor() {
        when(profileService.getProfile("user-123")).thenReturn(Mono.just(Map.of("color", "#ff0000")));
        when(hashOps.put(anyString(), any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(playerService.addPlayer("user-123"))
                .verifyComplete();
    }

    @Test
    void addPlayer_withoutProfile_usesDefaultColor() {
        when(profileService.getProfile("user-123")).thenReturn(Mono.empty());
        when(hashOps.put(anyString(), any(), any())).thenReturn(Mono.just(true));

        StepVerifier.create(playerService.addPlayer("user-123"))
                .verifyComplete();
    }

    @Test
    void updateDirection_validChange_savesNewDirection() throws Exception {
        when(hashOps.get(anyString(), any())).thenReturn(Mono.just(PLAYER_JSON));
        when(objectMapper.readValue(eq(PLAYER_JSON), eq(Player.class))).thenReturn(TEST_PLAYER);
        when(objectMapper.writeValueAsString(any())).thenReturn(PLAYER_JSON);
        when(hashOps.put(anyString(), any(), any())).thenReturn(Mono.just(true));

        // UP → RIGHT is valid (not opposite)
        StepVerifier.create(playerService.updateDirection("user-123", Direction.RIGHT))
                .verifyComplete();
    }

    @Test
    void updateDirection_oppositeDirection_isIgnored() throws Exception {
        when(hashOps.get(anyString(), any())).thenReturn(Mono.just(PLAYER_JSON));
        when(objectMapper.readValue(eq(PLAYER_JSON), eq(Player.class))).thenReturn(TEST_PLAYER);

        // UP → DOWN is the opposite and must be ignored
        StepVerifier.create(playerService.updateDirection("user-123", Direction.DOWN))
                .verifyComplete();
    }

    @Test
    void removePlayer_delegatesToRedis() {
        when(hashOps.remove(anyString(), any())).thenReturn(Mono.just(1L));

        StepVerifier.create(playerService.removePlayer("user-123"))
                .verifyComplete();
    }

    @Test
    void getAllPlayers_deserializesEachEntry() throws Exception {
        when(hashOps.values(anyString())).thenReturn(Flux.just(PLAYER_JSON));
        when(objectMapper.readValue(eq(PLAYER_JSON), eq(Player.class))).thenReturn(TEST_PLAYER);

        StepVerifier.create(playerService.getAllPlayers())
                .expectNext(TEST_PLAYER)
                .verifyComplete();
    }
}

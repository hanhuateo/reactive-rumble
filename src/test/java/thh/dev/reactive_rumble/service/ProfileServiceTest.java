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

import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    ReactiveStringRedisTemplate redisTemplate;
    @Mock
    ReactiveHashOperations<String, Object, Object> hashOps;

    private ProfileService profileService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForHash()).thenReturn(hashOps);
        profileService = new ProfileService(redisTemplate);
    }

    @Test
    void saveProfile_writesColorAndUsernameToHash() {
        when(hashOps.putAll(eq("player:profile:user-123"), anyMap())).thenReturn(Mono.just(true));

        StepVerifier.create(profileService.saveProfile("user-123", "alice", "#ff0000"))
                .verifyComplete();
    }

    @Test
    void getProfile_returnsMapFromHash() {
        when(hashOps.entries("player:profile:user-123"))
                .thenReturn(Flux.just(Map.entry("color", "#ff0000"), Map.entry("username", "alice")));

        StepVerifier.create(profileService.getProfile("user-123"))
                .expectNextMatches(map -> "#ff0000".equals(map.get("color")) && "alice".equals(map.get("username")))
                .verifyComplete();
    }

    @Test
    void getProfile_returnsEmptyMapWhenNoProfile() {
        when(hashOps.entries("player:profile:unknown")).thenReturn(Flux.empty());

        StepVerifier.create(profileService.getProfile("unknown"))
                .expectNextMatches(Map::isEmpty)
                .verifyComplete();
    }
}

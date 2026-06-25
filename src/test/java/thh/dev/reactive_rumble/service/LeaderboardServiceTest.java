package thh.dev.reactive_rumble.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveZSetOperations;
import org.springframework.data.redis.core.ZSetOperations;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class LeaderboardServiceTest {

    @Mock ReactiveStringRedisTemplate redisTemplate;
    @Mock ReactiveZSetOperations<String, String> zSetOps;

    private LeaderboardService leaderboardService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForZSet()).thenReturn(zSetOps);
        leaderboardService = new LeaderboardService(redisTemplate);
    }

    @Test
    void updateScore_storesScoreInZSet() {
        when(zSetOps.add(anyString(), eq("alice"), eq(5.0))).thenReturn(Mono.just(true));

        StepVerifier.create(leaderboardService.updateScore("alice", 5))
                .expectNext(5.0)
                .verifyComplete();
    }

    @Test
    void removeScore_delegatesToZSetRemove() {
        when(zSetOps.remove(anyString(), eq("alice"))).thenReturn(Mono.just(1L));

        StepVerifier.create(leaderboardService.removeScore("alice"))
                .verifyComplete();
    }

    @Test
    void getTopScores_returnsRankedEntries() {
        ZSetOperations.TypedTuple<String> tuple = ZSetOperations.TypedTuple.of("alice", 10.0);
        when(zSetOps.reverseRangeWithScores(anyString(), any(Range.class)))
                .thenReturn(Flux.just(tuple));

        StepVerifier.create(leaderboardService.getTopScores())
                .expectNextMatches(t -> t.getValue().equals("alice") && t.getScore() == 10.0)
                .verifyComplete();
    }
}

package thh.dev.reactive_rumble.service;

import org.springframework.data.domain.Range;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private static final String LEADERBOARD_KEY = "game:leaderboard";

    // Adds/Updates a player's score
    public Mono<Double> updateScore(String playerId, int score) {
        return this.redisTemplate.opsForZSet()
                .add(LEADERBOARD_KEY, playerId, score)
                .thenReturn((double) score);
    }

    public Mono<Void> removeScore(String playerId) {
        return this.redisTemplate.opsForZSet().remove(LEADERBOARD_KEY, playerId).then();
    }

    // Gets Top 10 Players
    public Flux<ZSetOperations.TypedTuple<String>> getTopScores() {
        return this.redisTemplate.opsForZSet()
                .reverseRangeWithScores(LEADERBOARD_KEY, Range.closed(0L, 9L));
    }
}

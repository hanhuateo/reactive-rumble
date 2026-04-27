package thh.dev.reactive_rumble.service;

import java.util.Map;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ReactiveStringRedisTemplate redisTemplate;
    private static final String PROFILE_KEY_PREFIX = "player:profile:";

    public Mono<Void> saveProfile(String id, String username, String color) {
        String key = PROFILE_KEY_PREFIX + id;
        return this.redisTemplate.opsForHash()
                .putAll(key, Map.of("username", username, "color", color))
                .then();
    }

    public Mono<Map<Object, Object>> getProfile(String id) {
        return this.redisTemplate.opsForHash().entries(PROFILE_KEY_PREFIX + id).collectMap(Map.Entry::getKey,
                Map.Entry::getValue);
    }
}
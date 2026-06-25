package thh.dev.reactive_rumble.service;

import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import thh.dev.reactive_rumble.model.Direction;
import thh.dev.reactive_rumble.model.Player;
import thh.dev.reactive_rumble.model.Point;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    private static final String PLAYERS_KEY = "game:active_players";

    public Mono<Void> addPlayer(String id) {
        return this.profileService.getProfile(id)
                .defaultIfEmpty(Map.of("color", "#00ff00"))
                .flatMap(profile -> {
                    Player newPlayer = new Player(
                            id,
                            List.of(new Point(10, 10), new Point(10, 11), new Point(10, 12)),
                            Direction.UP,
                            (String) profile.get("color"));
                    return this.saveToRedis(newPlayer);
                });
    }

    public Mono<Void> saveToRedis(Player player) {
        try {
            String json = this.objectMapper.writeValueAsString(player);
            return this.redisTemplate.opsForHash().put(PLAYERS_KEY, player.id(), json).then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public Mono<Void> updateDirection(String id, Direction newDirection) {
        return this.redisTemplate.opsForHash().get(PLAYERS_KEY, id)
                .flatMap(json -> {
                    try {
                        // 1. Read & Deserialize
                        Player player = objectMapper.readValue((String) json, Player.class);
                        Direction currentDir = player.direction();

                        boolean isOpposite = (currentDir == Direction.UP && newDirection == Direction.DOWN) ||
                                (currentDir == Direction.DOWN && newDirection == Direction.UP) ||
                                (currentDir == Direction.LEFT && newDirection == Direction.RIGHT) ||
                                (currentDir == Direction.RIGHT && newDirection == Direction.LEFT);

                        if (!isOpposite) {
                            // 2. Modify (Create a new record with the updated direction)
                            Player updatedPlayer = new Player(
                                    player.id(),
                                    player.body(),
                                    newDirection,
                                    player.color());

                            // 3. Write back to Redis
                            return this.saveToRedis(updatedPlayer);
                        }

                        return Mono.empty();
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    public Flux<Player> getAllPlayers() {
        return this.redisTemplate.opsForHash().values(PLAYERS_KEY)
                .map(obj -> {
                    try {
                        return this.objectMapper.readValue((String) obj, Player.class);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    public Mono<Void> removePlayer(String id) {
        return this.redisTemplate.opsForHash().remove(PLAYERS_KEY, id).then();
    }
}

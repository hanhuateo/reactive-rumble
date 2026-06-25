package thh.dev.reactive_rumble.engine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import thh.dev.reactive_rumble.model.GameState;
import thh.dev.reactive_rumble.model.Player;
import thh.dev.reactive_rumble.model.Point;
import thh.dev.reactive_rumble.service.LeaderboardService;
import thh.dev.reactive_rumble.service.PlayerService;
import thh.dev.reactive_rumble.service.UserService;
import tools.jackson.databind.ObjectMapper;

@Service
@Slf4j
public class GameEngine {
    private final PlayerService playerService;
    private final LeaderboardService leaderboardService;
    private final UserService userService;
    private final Sinks.Many<GameState> gameSink = Sinks.many().replay().latest();
    private final AtomicReference<GameState> state;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private static final String FOOD_KEY = "game:food";

    public GameEngine(PlayerService playerService, LeaderboardService leaderboardService,
            UserService userService, ReactiveStringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.playerService = playerService;
        this.leaderboardService = leaderboardService;
        this.userService = userService;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.state = new AtomicReference<>(new GameState(Map.of(), new Point(5, 5)));

        Flux.interval(Duration.ofMillis(100))
                .flatMap(tick -> moveSnakes()) // Move every snake on every tick
                .doOnNext(newState -> {
                    this.state.set(newState);
                    this.gameSink.tryEmitNext(newState);
                })
                .subscribe();
    }

    private Mono<GameState> moveSnakes() {
        return this.getFoodFromRedis()
                .flatMap(currentFood -> this.playerService.getAllPlayers().collectList().flatMap(players -> {
                    boolean foodEaten = false;
                    List<Player> updatedPlayers = new ArrayList<>();

                    for (Player p : players) {
                        Player moved = this.calculateNextPosition(p);
                        Point head = moved.body().get(0);
                        List<Point> body = moved.body();
                        if (head.equals(currentFood)) {
                            foodEaten = true;
                            // Also update high score in background
                            this.userService.getUserById(p.id())
                                    .flatMap(user -> this.leaderboardService.updateScore(user.username(), moved.body().size()))
                                    .subscribe();
                        } else {
                            body.remove(body.size() - 1);
                        }
                        updatedPlayers.add(moved);
                    }
                    List<Player> survivors = this.getSurvivors(updatedPlayers);

                    // Prepare Redis tasks
                    List<Mono<Void>> tasks = new ArrayList<>();
                    survivors.forEach(p -> tasks.add(this.playerService.saveToRedis(p)));

                    // If food was eaten, add the "Spawn" task to the list
                    if (foodEaten) {
                        tasks.add(this.spawnNewFood());
                    }

                    return Mono.when(tasks).then(this.getFoodFromRedis().map(latestFood -> new GameState(
                            survivors.stream().collect(Collectors.toMap(Player::id, p -> p)), latestFood)));
                }));
    }

    private Player calculateNextPosition(Player player) {
        String id = player.id();
        List<Point> body = new ArrayList<>(player.body());
        Point head = body.get(0);
        Point newHead = switch (player.direction()) {
            case UP -> new Point(head.x(), head.y() - 1);
            case DOWN -> new Point(head.x(), head.y() + 1);
            case LEFT -> new Point(head.x() - 1, head.y());
            case RIGHT -> new Point(head.x() + 1, head.y());
        };
        body.addFirst(newHead);
        return new Player(id, body, player.direction(), player.color());
    }

    private List<Player> getSurvivors(List<Player> movedPlayers) {
        return movedPlayers.stream()
                .filter(player -> {
                    Point head = player.body().get(0);

                    // 1. Wall Collision (40x40 grid)
                    if (head.x() < 0 || head.x() >= 40 || head.y() < 0 || head.y() >= 40) {
                        this.playerService.removePlayer(player.id()).subscribe();
                        return false;
                    }

                    // 2. Self Collision (Head hitting any part of its own tail)
                    boolean hitSelf = player.body().stream()
                            .skip(1)
                            .anyMatch(segment -> segment.equals(head));
                    if (hitSelf) {
                        log.info("Player {} hit themselves.", player.id());
                        this.playerService.removePlayer(player.id()).subscribe();
                        return false;
                    }

                    // 3. Collision with Others (Head hitting any segment of any other snake)
                    boolean hitOthers = movedPlayers.stream()
                            .filter(other -> !other.id().equals(player.id())) // Don't check against self
                            .anyMatch(other -> other.body().contains(head));

                    if (hitOthers) {
                        log.info("Player {} collided with another player.", player.id());
                        this.playerService.removePlayer(player.id()).subscribe();
                        return false;
                    }

                    return true;
                })
                .toList();
    }

    public Flux<GameState> getGameStream() {
        return this.gameSink.asFlux();
    }

    private Mono<Point> getFoodFromRedis() {
        return this.redisTemplate.opsForValue().get(FOOD_KEY)
                .map(json -> {
                    try {
                        return this.objectMapper.readValue(json, Point.class);
                    } catch (Exception e) {
                        return new Point(5, 5); // Fallback
                    }
                })
                .defaultIfEmpty(new Point(5, 5)); // Initial food if Redis is empty
    }

    private Mono<Void> spawnNewFood() {
        Point newFood = new Point(new Random().nextInt(20), new Random().nextInt(20));
        try {
            String json = this.objectMapper.writeValueAsString(newFood);
            return this.redisTemplate.opsForValue().set(FOOD_KEY, json).then();
        } catch (Exception e) {
            return Mono.error(e);
        }
    }
}
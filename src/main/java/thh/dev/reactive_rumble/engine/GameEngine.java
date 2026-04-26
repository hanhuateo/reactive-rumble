package thh.dev.reactive_rumble.engine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import thh.dev.reactive_rumble.model.GameState;
import thh.dev.reactive_rumble.model.Player;
import thh.dev.reactive_rumble.model.Point;
import thh.dev.reactive_rumble.service.LeaderboardService;
import thh.dev.reactive_rumble.service.PlayerService;

@Service
@Slf4j
public class GameEngine {
    private final PlayerService playerService;
    private final LeaderboardService leaderboardService;
    private final Sinks.Many<GameState> gameSink = Sinks.many().replay().latest();
    private final AtomicReference<GameState> state;

    public GameEngine(PlayerService playerService, LeaderboardService leaderboardService) {
        this.playerService = playerService;
        this.leaderboardService = leaderboardService;
        this.state = new AtomicReference<>(new GameState(Map.of(), new Point(5, 5)));

        Flux.interval(Duration.ofMillis(100))
                .map(tick -> moveSnakes()) // Move every snake on every tick
                .doOnNext(newState -> {
                    this.state.set(newState);
                    this.gameSink.tryEmitNext(newState);
                })
                .subscribe();
    }

    private GameState moveSnakes() {
        Map<String, Player> currentPlayers = this.playerService.getActivePlayers();
        Map<String, Player> movedPlayers = new ConcurrentHashMap<>();
        Point food = this.state.get().food();
        boolean foodEaten = false;

        // Define your grid size (matches the 400x400 canvas / 10px blocks)
        int gridSize = 40;

        // currentPlayers.forEach((id, player) -> {
        for (Map.Entry<String, Player> entry : currentPlayers.entrySet()) {
            Player player = entry.getValue();
            String id = entry.getKey();

            List<Point> body = new ArrayList<>(player.body());
            Point head = body.get(0);

            Point newHead = switch (player.direction()) {
                case UP -> new Point(head.x(), head.y() - 1);
                case DOWN -> new Point(head.x(), head.y() + 1);
                case LEFT -> new Point(head.x() - 1, head.y());
                case RIGHT -> new Point(head.x() + 1, head.y());
            };

            // --- COLLISION DETECTION ---

            // 1. Wall Collision
            boolean hitWall = newHead.x() < 0 || newHead.x() >= gridSize ||
                    newHead.y() < 0 || newHead.y() >= gridSize;

            // 2. Self or Other Player Collision
            // We check if the newHead exists in ANY player's body
            boolean hitSnake = currentPlayers.values().stream()
                    .flatMap(p -> p.body().stream())
                    .anyMatch(segment -> segment.equals(newHead));

            if (hitWall || hitSnake) {
                log.info("Collision! Player {} is out.", id);
                this.playerService.removePlayer(id);
                this.leaderboardService.removeScore(id).subscribe();
            } else {
                // No collision? Move as normal
                body.add(0, newHead);
                // --- FOOD LOGIC ---
                if (newHead.equals(food)) {
                    foodEaten = true;
                    // 1. Calculate new score (body length)
                    int newScore = body.size() + 1;

                    // 2. Fire and forget the update to Redis
                    this.leaderboardService.updateScore(id, newScore).subscribe();

                    log.info("Score updated in Redis for {}", id);
                } else {
                    // NORMAL MOVE: Remove tail
                    body.remove(body.size() - 1);
                }

                Player movedPlayer = new Player(id, body, player.direction());
                this.playerService.updatePlayer(movedPlayer);
                movedPlayers.put(id, movedPlayer);
            }
        }
        ;

        // If food was eaten by ANYONE, spawn new food
        Point nextFood = foodEaten ? spawnFood(movedPlayers) : food;

        return new GameState(movedPlayers, nextFood);
    }

    public Flux<GameState> getGameStream() {
        return this.gameSink.asFlux();
    }

    private Point spawnFood(Map<String, Player> players) {
        int gridSize = 40;
        Random random = new Random();
        Point newFood;

        // Create a Set of all occupied coordinates for O(1) lookup
        Set<Point> occupied = players.values().stream()
                .flatMap(p -> p.body().stream())
                .collect(Collectors.toSet());

        do {
            newFood = new Point(random.nextInt(gridSize), random.nextInt(gridSize));
        } while (occupied.contains(newFood));

        return newFood;
    }
}
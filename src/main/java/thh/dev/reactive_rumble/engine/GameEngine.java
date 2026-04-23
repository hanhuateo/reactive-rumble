package thh.dev.reactive_rumble.engine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import thh.dev.reactive_rumble.model.GameState;
import thh.dev.reactive_rumble.model.Player;
import thh.dev.reactive_rumble.model.Point;
import thh.dev.reactive_rumble.service.PlayerService;

@Service
@Slf4j
public class GameEngine {
    private final PlayerService playerService;
    private final Sinks.Many<GameState> gameSink = Sinks.many().replay().latest();
    private final AtomicReference<GameState> state;

    public GameEngine(PlayerService playerService) {
        this.playerService = playerService;
        this.state = new AtomicReference<>(new GameState(Map.of(), new Point(5, 5)));

        Flux.interval(Duration.ofMillis(100))
                .map(tick -> moveSnakes()) // Move every snake on every tick
                .doOnNext(newState -> {
                    state.set(newState);
                    gameSink.tryEmitNext(newState);
                })
                .subscribe();
    }

    private GameState moveSnakes() {
        Map<String, Player> currentPlayers = playerService.getActivePlayers();
        Map<String, Player> movedPlayers = new ConcurrentHashMap<>();

        // Define your grid size (matches the 400x400 canvas / 10px blocks)
        int gridSize = 40;

        currentPlayers.forEach((id, player) -> {
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
                playerService.removePlayer(id); // We'll add this method next
            } else {
                // No collision? Move as normal
                body.add(0, newHead);
                body.remove(body.size() - 1);

                Player movedPlayer = new Player(id, body, player.direction());
                playerService.updatePlayer(movedPlayer);
                movedPlayers.put(id, movedPlayer);
            }
        });

        return new GameState(movedPlayers, state.get().food());
    }

    public Flux<GameState> getGameStream() {
        return gameSink.asFlux();
    }
}
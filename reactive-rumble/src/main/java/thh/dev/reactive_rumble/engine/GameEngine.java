package thh.dev.reactive_rumble.engine;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import thh.dev.reactive_rumble.model.GameState;
import thh.dev.reactive_rumble.model.Player;
import thh.dev.reactive_rumble.model.Point;
import thh.dev.reactive_rumble.service.PlayerService;

@Service
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

        currentPlayers.forEach((id, player) -> {
            List<Point> body = new ArrayList<>(player.body());
            Point head = body.get(0);

            // Calculate new head position
            Point newHead = switch (player.direction()) {
                case UP -> new Point(head.x(), head.y() - 1);
                case DOWN -> new Point(head.x(), head.y() + 1);
                case LEFT -> new Point(head.x() - 1, head.y());
                case RIGHT -> new Point(head.x() + 1, head.y());
            };

            // Movement logic: Add new head, remove old tail
            body.add(0, newHead);
            body.remove(body.size() - 1);

            movedPlayers.put(id, new Player(id, body, player.direction()));
        });

        return new GameState(movedPlayers, state.get().food());
    }

    public Flux<GameState> getGameStream() {
        return gameSink.asFlux();
    }
}
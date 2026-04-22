package thh.dev.reactive_rumble.engine;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Service;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;
import thh.dev.reactive_rumble.model.GameState;
import thh.dev.reactive_rumble.model.Point;

@Service
public class GameEngine {
    // The "Sink" acts as a broadcaster. .replay().latest() ensures new players get
    // the current state.
    private final Sinks.Many<GameState> gameSink = Sinks.many().replay().latest();
    private final AtomicReference<GameState> state = new AtomicReference<>(
            new GameState(new ConcurrentHashMap<>(), new Point(5, 5)));

    public GameEngine() {
        // The Game Loop: Ticks every 100ms
        Flux.interval(Duration.ofMillis(100))
                .map(tick -> updateState(state.get()))
                .doOnNext(newState -> {
                    state.set(newState);
                    gameSink.tryEmitNext(newState);
                })
                .subscribe();
    }

    private GameState updateState(GameState current) {
        // TODO: Logic to move snakes and check collisions
        // For now, it just returns the current state
        return current;
    }

    public Flux<GameState> getGameStream() {
        return gameSink.asFlux();
    }
}
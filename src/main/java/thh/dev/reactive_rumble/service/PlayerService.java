package thh.dev.reactive_rumble.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import thh.dev.reactive_rumble.model.Direction;
import thh.dev.reactive_rumble.model.Player;
import thh.dev.reactive_rumble.model.Point;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlayerService {
    // A thread-safe map of current players
    private final Map<String, Player> players = new ConcurrentHashMap<>();
    private final ProfileService profileService;

    public Mono<Void> addPlayer(String id) {
        return this.profileService.getProfile(id)
                .defaultIfEmpty(Map.of("username", "Guest_" + id, "color", "#00ff00"))
                .map(profile -> {
                    String username = (String) profile.getOrDefault("username", "Guest");
                    String color = (String) profile.getOrDefault("color", "#00ff00");
                    // Initial snake at (10,10)
                    List<Point> initialBody = List.of(new Point(10, 10), new Point(10, 11), new Point(10, 12));
                    Player newPlayer = new Player(id, username, initialBody, Direction.UP, color);

                    this.players.put(id, newPlayer);
                    return newPlayer;
                }).then();
    }

    public void updateDirection(String id, Direction newDir) {
        Player p = this.players.get(id);
        if (p == null)
            return;

        Direction currentDir = p.direction();

        // Prevent 180-degree turns
        boolean isOpposite = (currentDir == Direction.UP && newDir == Direction.DOWN) ||
                (currentDir == Direction.DOWN && newDir == Direction.UP) ||
                (currentDir == Direction.LEFT && newDir == Direction.RIGHT) ||
                (currentDir == Direction.RIGHT && newDir == Direction.LEFT);

        if (!isOpposite) {
            this.players.put(id, new Player(id, p.username(), p.body(), newDir, p.color()));
        }
    }

    public void updatePlayer(Player player) {
        this.players.put(player.id(), player);
    }

    public Map<String, Player> getActivePlayers() {
        return this.players;
    }

    public void removePlayer(String id) {
        this.players.remove(id);
    }
}

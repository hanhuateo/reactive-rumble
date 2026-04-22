package thh.dev.reactive_rumble.service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import thh.dev.reactive_rumble.model.Direction;
import thh.dev.reactive_rumble.model.Player;
import thh.dev.reactive_rumble.model.Point;

@Service
public class PlayerService {
    // A thread-safe map of current players
    private final Map<String, Player> players = new ConcurrentHashMap<>();

    public void addPlayer(String id) {
        // Start the snake with 3 segments at a random-ish spot
        List<Point> initialBody = List.of(
                new Point(10, 10),
                new Point(10, 11),
                new Point(10, 12));
        players.put(id, new Player(id, initialBody, Direction.UP));
    }

    public void updateDirection(String id, Direction direction) {
        Player player = players.get(id);
        if (player != null) {
            // In a real game, you'd check to make sure they aren't
            // trying to turn 180 degrees into themselves
            players.put(id, new Player(id, player.body(), direction));
        }
    }

    public Map<String, Player> getActivePlayers() {
        return players;
    }
}

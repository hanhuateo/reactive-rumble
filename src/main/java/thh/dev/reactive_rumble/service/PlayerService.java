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

    public void updateDirection(String id, Direction newDir) {
        Player p = players.get(id);
        if (p == null)
            return;

        Direction currentDir = p.direction();

        // Prevent 180-degree turns
        boolean isOpposite = (currentDir == Direction.UP && newDir == Direction.DOWN) ||
                (currentDir == Direction.DOWN && newDir == Direction.UP) ||
                (currentDir == Direction.LEFT && newDir == Direction.RIGHT) ||
                (currentDir == Direction.RIGHT && newDir == Direction.LEFT);

        if (!isOpposite) {
            players.put(id, new Player(id, p.body(), newDir));
        }
    }

    public void updatePlayer(Player player) {
        players.put(player.id(), player);
    }

    public Map<String, Player> getActivePlayers() {
        return players;
    }

    public void removePlayer(String id) {
        players.remove(id);
    }
}

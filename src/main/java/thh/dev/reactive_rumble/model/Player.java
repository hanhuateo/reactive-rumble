package thh.dev.reactive_rumble.model;

import java.util.List;

public record Player(String id, String username, List<Point> body, Direction direction, String color) {
}

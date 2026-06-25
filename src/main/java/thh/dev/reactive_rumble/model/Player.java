package thh.dev.reactive_rumble.model;

import java.util.List;

public record Player(String id, List<Point> body, Direction direction, String color) {
}

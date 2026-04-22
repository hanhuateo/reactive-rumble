package thh.dev.reactive_rumble.model;

import java.util.Map;

public record GameState(Map<String, Player> players, Point food) {}

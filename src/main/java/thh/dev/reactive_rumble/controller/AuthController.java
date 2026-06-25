package thh.dev.reactive_rumble.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import thh.dev.reactive_rumble.service.JwtService;
import thh.dev.reactive_rumble.service.UserService;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final JwtService jwtService;

    @PostMapping("/register")
    public Mono<Map<String, String>> register(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");
        String color = body.getOrDefault("color", "#00ff00");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password required"));
        }

        return userService.register(username, password, color)
                .map(user -> Map.of(
                        "token", jwtService.generateToken(user),
                        "id", user.id(),
                        "username", user.username(),
                        "color", user.color()))
                .onErrorMap(IllegalArgumentException.class,
                        e -> new ResponseStatusException(HttpStatus.CONFLICT, e.getMessage()));
    }

    @PostMapping("/login")
    public Mono<Map<String, String>> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null || username.isBlank() || password.isBlank()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Username and password required"));
        }

        return userService.login(username, password)
                .map(user -> Map.of(
                        "token", jwtService.generateToken(user),
                        "id", user.id(),
                        "username", user.username(),
                        "color", user.color()))
                .onErrorMap(IllegalArgumentException.class,
                        e -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage()));
    }
}

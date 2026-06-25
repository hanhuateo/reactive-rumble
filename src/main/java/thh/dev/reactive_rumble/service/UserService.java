package thh.dev.reactive_rumble.service;

import java.util.UUID;

import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;
import thh.dev.reactive_rumble.model.User;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class UserService {

    private final ReactiveStringRedisTemplate redisTemplate;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;
    private final ProfileService profileService;

    private static final String USER_KEY = "user:";
    private static final String USERNAME_INDEX_KEY = "user:username:";

    public Mono<User> register(String username, String password, String color) {
        String usernameKey = USERNAME_INDEX_KEY + username.toLowerCase();

        return redisTemplate.hasKey(usernameKey)
                .flatMap(exists -> {
                    if (exists) {
                        return Mono.error(new IllegalArgumentException("Username already taken"));
                    }
                    String id = UUID.randomUUID().toString();
                    String hash = passwordEncoder.encode(password);
                    User user = new User(id, username, hash, color);

                    try {
                        String json = objectMapper.writeValueAsString(user);
                        return redisTemplate.opsForValue().set(USER_KEY + id, json)
                                .then(redisTemplate.opsForValue().set(usernameKey, id))
                                .then(profileService.saveProfile(id, username, color))
                                .thenReturn(user);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    public Mono<User> login(String username, String password) {
        String usernameKey = USERNAME_INDEX_KEY + username.toLowerCase();

        return redisTemplate.opsForValue().get(usernameKey)
                .switchIfEmpty(Mono.error(new IllegalArgumentException("Invalid username or password")))
                .flatMap(id -> redisTemplate.opsForValue().get(USER_KEY + id))
                .flatMap(json -> {
                    try {
                        User user = objectMapper.readValue(json, User.class);
                        if (!passwordEncoder.matches(password, user.passwordHash())) {
                            return Mono.error(new IllegalArgumentException("Invalid username or password"));
                        }
                        return Mono.just(user);
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }

    public Mono<User> getUserById(String id) {
        return redisTemplate.opsForValue().get(USER_KEY + id)
                .flatMap(json -> {
                    try {
                        return Mono.just(objectMapper.readValue(json, User.class));
                    } catch (Exception e) {
                        return Mono.error(e);
                    }
                });
    }
}

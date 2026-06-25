package thh.dev.reactive_rumble.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import thh.dev.reactive_rumble.model.User;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock ReactiveStringRedisTemplate redisTemplate;
    @Mock ReactiveValueOperations<String, String> valueOps;
    @Mock PasswordEncoder passwordEncoder;
    @Mock ObjectMapper objectMapper;
    @Mock ProfileService profileService;

    private UserService userService;

    private static final User TEST_USER = new User("user-123", "alice", "hashed", "#ff0000");
    private static final String USER_JSON = "{\"id\":\"user-123\",\"username\":\"alice\",\"passwordHash\":\"hashed\",\"color\":\"#ff0000\"}";

    @BeforeEach
    void setUp() throws Exception {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOps);
        userService = new UserService(redisTemplate, passwordEncoder, objectMapper, profileService);
    }

    @Test
    void register_success_returnsUser() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(false));
        when(passwordEncoder.encode("secret")).thenReturn("hashed");
        when(objectMapper.writeValueAsString(any())).thenReturn(USER_JSON);
        when(valueOps.set(anyString(), anyString())).thenReturn(Mono.just(true));
        when(profileService.saveProfile(anyString(), eq("alice"), eq("#ff0000"))).thenReturn(Mono.empty());

        StepVerifier.create(userService.register("alice", "secret", "#ff0000"))
                .expectNextMatches(u -> u.username().equals("alice") && u.color().equals("#ff0000"))
                .verifyComplete();
    }

    @Test
    void register_duplicateUsername_returnsError() {
        when(redisTemplate.hasKey(anyString())).thenReturn(Mono.just(true));

        StepVerifier.create(userService.register("alice", "secret", "#ff0000"))
                .expectErrorMessage("Username already taken")
                .verify();
    }

    @Test
    void login_success_returnsUser() throws Exception {
        when(valueOps.get("user:username:alice")).thenReturn(Mono.just("user-123"));
        when(valueOps.get("user:user-123")).thenReturn(Mono.just(USER_JSON));
        when(objectMapper.readValue(eq(USER_JSON), eq(User.class))).thenReturn(TEST_USER);
        when(passwordEncoder.matches("secret", "hashed")).thenReturn(true);

        StepVerifier.create(userService.login("alice", "secret"))
                .expectNext(TEST_USER)
                .verifyComplete();
    }

    @Test
    void login_wrongPassword_returnsError() throws Exception {
        when(valueOps.get("user:username:alice")).thenReturn(Mono.just("user-123"));
        when(valueOps.get("user:user-123")).thenReturn(Mono.just(USER_JSON));
        when(objectMapper.readValue(eq(USER_JSON), eq(User.class))).thenReturn(TEST_USER);
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);

        StepVerifier.create(userService.login("alice", "wrong"))
                .expectErrorMessage("Invalid username or password")
                .verify();
    }

    @Test
    void login_unknownUsername_returnsError() {
        when(valueOps.get(anyString())).thenReturn(Mono.empty());

        StepVerifier.create(userService.login("ghost", "secret"))
                .expectErrorMessage("Invalid username or password")
                .verify();
    }

    @Test
    void getUserById_found_returnsUser() throws Exception {
        when(valueOps.get("user:user-123")).thenReturn(Mono.just(USER_JSON));
        when(objectMapper.readValue(eq(USER_JSON), eq(User.class))).thenReturn(TEST_USER);

        StepVerifier.create(userService.getUserById("user-123"))
                .expectNext(TEST_USER)
                .verifyComplete();
    }

    @Test
    void getUserById_notFound_returnsEmpty() {
        when(valueOps.get("user:user-999")).thenReturn(Mono.empty());

        StepVerifier.create(userService.getUserById("user-999"))
                .verifyComplete();
    }
}

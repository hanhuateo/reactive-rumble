package thh.dev.reactive_rumble;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;

import java.util.Map;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthIntegrationTest {

    // Single Redis container shared across all tests in this class — closed in @AfterAll
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @BeforeAll
    static void startRedis() {
        redis.start();
    }

    @AfterAll
    static void stopRedis() {
        redis.stop();
    }

    // Tells Spring to connect to the container instead of localhost:6379
    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @LocalServerPort
    int port;

    @Autowired
    ReactiveStringRedisTemplate redisTemplate;

    WebTestClient client;

    @BeforeEach
    void setUp() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();

        // Clear Redis before each test so state doesn't bleed between tests
        redisTemplate.execute(conn -> conn.serverCommands().flushAll()).blockLast();
    }

    @Test
    void register_success_storesUserAndReturnsToken() {
        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "alice", "password", "secret", "color", "#ff0000"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty()
                .jsonPath("$.username").isEqualTo("alice")
                .jsonPath("$.color").isEqualTo("#ff0000");
    }

    @Test
    void register_duplicateUsername_returns409() {
        Map<String, String> body = Map.of("username", "alice", "password", "secret", "color", "#ff0000");

        client.post().uri("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body).exchange().expectStatus().isOk();

        client.post().uri("/auth/register").contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body).exchange().expectStatus().isEqualTo(409);
    }

    @Test
    void login_afterRegister_returnsToken() {
        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "alice", "password", "secret", "color", "#ff0000"))
                .exchange().expectStatus().isOk();

        client.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "alice", "password", "secret"))
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.token").isNotEmpty();
    }

    @Test
    void login_wrongPassword_returns401() {
        client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "alice", "password", "secret", "color", "#ff0000"))
                .exchange().expectStatus().isOk();

        client.post().uri("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "alice", "password", "wrong"))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void joinGame_withValidToken_returns200() {
        String token = client.post().uri("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of("username", "alice", "password", "secret", "color", "#ff0000"))
                .exchange()
                .returnResult(Map.class)
                .getResponseBody()
                .blockFirst()
                .get("token")
                .toString();

        client.post().uri("/game/join")
                .header("Authorization", "Bearer " + token)
                .exchange()
                .expectStatus().isOk();
    }

    @Test
    void joinGame_withoutToken_returns401() {
        client.post().uri("/game/join")
                .exchange()
                .expectStatus().isUnauthorized();
    }
}

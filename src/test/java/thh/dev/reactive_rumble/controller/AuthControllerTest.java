package thh.dev.reactive_rumble.controller;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import thh.dev.reactive_rumble.model.User;
import thh.dev.reactive_rumble.service.JwtService;
import thh.dev.reactive_rumble.service.UserService;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

        @Mock
        UserService userService;
        @Mock
        JwtService jwtService;
        @InjectMocks
        AuthController authController;

        private static final User ALICE = new User("user-123", "alice", "hashed", "#ff0000");

        @Test
        void register_success_returnsTokenAndProfile() {
                when(userService.register("alice", "secret", "#ff0000")).thenReturn(Mono.just(ALICE));
                when(jwtService.generateToken(ALICE)).thenReturn("test.jwt");

                StepVerifier.create(authController
                                .register(Map.of("username", "alice", "password", "secret", "color", "#ff0000")))
                                .expectNextMatches(resp -> "test.jwt".equals(resp.get("token")) &&
                                                "alice".equals(resp.get("username")) &&
                                                "user-123".equals(resp.get("id")))
                                .verifyComplete();
        }

        @Test
        void register_duplicateUsername_returns409() {
                when(userService.register(any(), any(), any()))
                                .thenReturn(Mono.error(new IllegalArgumentException("Username already taken")));

                StepVerifier.create(authController
                                .register(Map.of("username", "alice", "password", "secret", "color", "#ff0000")))
                                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse &&
                                                rse.getStatusCode() == HttpStatus.CONFLICT)
                                .verify();
        }

        @Test
        void register_missingUsername_returns400() {
                StepVerifier.create(authController.register(Map.of("password", "secret")))
                                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse &&
                                                rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                                .verify();
        }

        @Test
        void login_success_returnsToken() {
                when(userService.login("alice", "secret")).thenReturn(Mono.just(ALICE));
                when(jwtService.generateToken(ALICE)).thenReturn("test.jwt");

                StepVerifier.create(authController.login(Map.of("username", "alice", "password", "secret")))
                                .expectNextMatches(resp -> "test.jwt".equals(resp.get("token")))
                                .verifyComplete();
        }

        @Test
        void login_wrongPassword_returns401() {
                when(userService.login(any(), any()))
                                .thenReturn(Mono.error(new IllegalArgumentException("Invalid username or password")));

                StepVerifier.create(authController.login(Map.of("username", "alice", "password", "wrong")))
                                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse &&
                                                rse.getStatusCode() == HttpStatus.UNAUTHORIZED)
                                .verify();
        }

        @Test
        void register_missingPassword_returns400() {
                StepVerifier.create(authController.register(Map.of("username", "alice")))
                                .expectErrorMatches(ex -> ex instanceof ResponseStatusException rse &&
                                                rse.getStatusCode() == HttpStatus.BAD_REQUEST)
                                .verify();
        }
}

package thh.dev.reactive_rumble.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import thh.dev.reactive_rumble.model.User;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private JwtService jwtService;

    private static final String SECRET = "VGhpc0lzQVNlY3JldEtleUZvckpXVFRoYXRJc0xvbmdFbm91Z2g=";
    private static final User USER = new User("user-123", "alice", "hashed", "#ff0000");

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secret", SECRET);
        ReflectionTestUtils.setField(jwtService, "expiration", 86400000L);
    }

    @Test
    void generateToken_embedsUserIdAsSubject() {
        String token = jwtService.generateToken(USER);
        assertThat(jwtService.extractUserId(token)).isEqualTo("user-123");
    }

    @Test
    void isValid_returnsTrueForFreshToken() {
        String token = jwtService.generateToken(USER);
        assertThat(jwtService.isValid(token)).isTrue();
    }

    @Test
    void isValid_returnsFalseForTamperedSignature() {
        String token = jwtService.generateToken(USER);
        String tampered = token.substring(0, token.lastIndexOf('.') + 1) + "invalidsignature";
        assertThat(jwtService.isValid(tampered)).isFalse();
    }

    @Test
    void isValid_returnsFalseForExpiredToken() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L);
        String token = jwtService.generateToken(USER);
        assertThat(jwtService.isValid(token)).isFalse();
    }

    @Test
    void isValid_returnsFalseForGarbage() {
        assertThat(jwtService.isValid("not.a.token")).isFalse();
    }
}

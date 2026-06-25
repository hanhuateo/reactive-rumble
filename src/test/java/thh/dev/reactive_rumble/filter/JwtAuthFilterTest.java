package thh.dev.reactive_rumble.filter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import thh.dev.reactive_rumble.service.JwtService;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JwtAuthFilterTest {

    @Mock JwtService jwtService;
    @Mock WebFilterChain chain;
    @InjectMocks JwtAuthFilter filter;

    @Test
    void noAuthHeader_passesRequestThroughUnchanged() {
        var request = MockServerHttpRequest.get("/game/join").build();
        var exchange = MockServerWebExchange.from(request);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtService, never()).isValid(any());
    }

    @Test
    void invalidToken_passesRequestThroughWithoutAuth() {
        var request = MockServerHttpRequest.get("/game/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer bad.token")
                .build();
        var exchange = MockServerWebExchange.from(request);
        when(jwtService.isValid("bad.token")).thenReturn(false);
        when(chain.filter(exchange)).thenReturn(Mono.empty());

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();

        verify(jwtService, never()).extractUserId(any());
    }

    @Test
    void validToken_populatesSecurityContextWithUserId() {
        var request = MockServerHttpRequest.get("/game/join")
                .header(HttpHeaders.AUTHORIZATION, "Bearer valid.token")
                .build();
        var exchange = MockServerWebExchange.from(request);
        when(jwtService.isValid("valid.token")).thenReturn(true);
        when(jwtService.extractUserId("valid.token")).thenReturn("user-123");
        when(chain.filter(exchange)).thenReturn(
                ReactiveSecurityContextHolder.getContext()
                        .flatMap(ctx -> {
                            assert ctx.getAuthentication().getPrincipal().equals("user-123");
                            return Mono.empty();
                        }));

        StepVerifier.create(filter.filter(exchange, chain))
                .verifyComplete();
    }
}

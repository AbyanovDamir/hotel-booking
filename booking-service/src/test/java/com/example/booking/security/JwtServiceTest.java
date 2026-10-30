package com.example.booking.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import javax.crypto.SecretKey;
import com.example.booking.entity.Role;
import com.example.booking.entity.UserAccount;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.*;

@DisplayName("JWT service")
class JwtServiceTest {
    private static final String SECRET = "test-secret-key-with-at-least-32-bytes";

    @Test
    @DisplayName("токен содержит пользователя, роль и ожидаемый срок жизни")
    void issuesSignedTokenWithClaims() {
        JwtService service = new JwtService(SECRET, Duration.ofMinutes(30));
        UserAccount user = UserAccount.builder().username("alice").role(Role.ADMIN).build();

        String token = service.issue(user);
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        var claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(claims.get("role", String.class)).isEqualTo("ADMIN");
        assertThat(claims.getExpiration().getTime() - claims.getIssuedAt().getTime())
                .isEqualTo(Duration.ofMinutes(30).toMillis());
        assertThat(service.expiresInSeconds()).isEqualTo(1800L);
    }
}

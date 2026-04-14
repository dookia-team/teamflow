package com.dookia.teamflow.auth.service;

import com.dookia.teamflow.auth.config.JwtProperties;
import com.dookia.teamflow.auth.exception.AuthErrorCode;
import com.dookia.teamflow.auth.exception.AuthException;
import com.dookia.teamflow.user.entity.User;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-must-be-at-least-256-bits-long-aaaaaaaaaa";
    private static final long ACCESS_TTL = 900;
    private static final long REFRESH_TTL = 604800;

    private final JwtService jwtService = new JwtService(
        new JwtProperties(SECRET, ACCESS_TTL, REFRESH_TTL)
    );

    @Test
    @DisplayName("access token 생성 후 subject 파싱 시 user.no 반환")
    void issueAccessToken_parseUserNo_success() {
        User user = userWithNo(100L, "a@b.com", "홍길동");

        String token = jwtService.issueAccessToken(user);
        Long parsed = jwtService.parseUserNo(token);

        assertThat(token).isNotBlank();
        assertThat(parsed).isEqualTo(user.getNo());
    }

    @Test
    @DisplayName("만료된 토큰은 AUTH_TOKEN_EXPIRED 예외")
    void parseUserNo_expired_throwsExpired() {
        User user = userWithNo(101L, "a@b.com", "홍길동");
        SecretKey key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String expired = Jwts.builder()
            .subject(user.getNo().toString())
            .issuedAt(Date.from(Instant.now().minus(Duration.ofHours(2))))
            .expiration(Date.from(Instant.now().minus(Duration.ofHours(1))))
            .signWith(key)
            .compact();

        assertThatThrownBy(() -> jwtService.parseUserNo(expired))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.AUTH_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("변조된 토큰은 AUTH_TOKEN_INVALID 예외")
    void parseUserNo_tampered_throwsInvalid() {
        User user = userWithNo(102L, "a@b.com", "홍길동");
        String token = jwtService.issueAccessToken(user);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThatThrownBy(() -> jwtService.parseUserNo(tampered))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    @DisplayName("다른 시크릿으로 서명된 토큰은 AUTH_TOKEN_INVALID 예외")
    void parseUserNo_wrongSecret_throwsInvalid() {
        SecretKey otherKey = Keys.hmacShaKeyFor(
            "different-secret-key-also-at-least-256-bits-long-bbbbbbbb".getBytes(StandardCharsets.UTF_8)
        );
        String foreign = Jwts.builder()
            .subject("999")
            .issuedAt(new Date())
            .expiration(Date.from(Instant.now().plus(Duration.ofMinutes(10))))
            .signWith(otherKey)
            .compact();

        assertThatThrownBy(() -> jwtService.parseUserNo(foreign))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.AUTH_TOKEN_INVALID);
    }

    private static User userWithNo(long no, String email, String name) {
        User user = User.createFromGoogle("google-sub-" + no, email, name, null);
        try {
            Field noField = User.class.getDeclaredField("no");
            noField.setAccessible(true);
            noField.set(user, no);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return user;
    }
}

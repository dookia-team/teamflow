package com.dookia.teamflow.auth.service;

import com.dookia.teamflow.auth.config.JwtProperties;
import com.dookia.teamflow.exception.AuthErrorCode;
import com.dookia.teamflow.exception.AuthException;
import com.dookia.teamflow.user.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

/**
 * JWT Access Token 발급/검증 서비스. auth-design.md §2.1 을 따른다.
 *  - 알고리즘: HS256
 *  - payload: sub=user.no, email, name, iat, exp
 */
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties properties;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(properties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(Duration.ofSeconds(properties.accessTokenTtlSeconds()));

        return Jwts.builder()
            .subject(user.getNo().toString())
            .claim("email", user.getEmail())
            .claim("name", user.getName())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiresAt))
            .signWith(key())
            .compact();
    }

    /**
     * 토큰 검증 후 subject(user.no)를 반환한다.
     *
     * @throws AuthException AUTH_TOKEN_EXPIRED / AUTH_TOKEN_INVALID
     */
    public Long parseUserNo(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return Long.parseLong(claims.getSubject());
        } catch (ExpiredJwtException e) {
            throw new AuthException(AuthErrorCode.AUTH_TOKEN_EXPIRED, e);
        } catch (JwtException | IllegalArgumentException e) {
            throw new AuthException(AuthErrorCode.AUTH_TOKEN_INVALID, e);
        }
    }
}

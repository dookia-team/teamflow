package com.dookia.teamflow.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * JWT 관련 설정값 바인딩. application.yml의 app.jwt.* 와 매핑된다.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
    String secret,
    long accessTokenTtlSeconds,
    long refreshTokenTtlSeconds,
    String cookieSameSite,
    boolean cookieSecure
) {
}

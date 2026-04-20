package com.dookia.teamflow.auth.controller;

import com.dookia.teamflow.auth.config.JwtProperties;
import com.dookia.teamflow.auth.dto.AuthDto;
import com.dookia.teamflow.auth.service.AuthService;
import com.dookia.teamflow.user.entity.UserProvider;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 인증 엔드포인트. OpenAPI 문서 계약은 {@link AuthApi} 참조.
 *  - POST /api/auth/oauth/{provider}  로그인/회원가입 (google, naver, kakao …)
 *  - POST /api/auth/refresh           토큰 갱신 (Rotation)
 *  - POST /api/auth/logout            로그아웃
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController implements AuthApi {

    public static final String REFRESH_COOKIE_NAME = "teamflow_rt";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @Override
    @PostMapping("/oauth/{provider}")
    public ResponseEntity<AuthDto.AuthResponse> oauthLogin(@PathVariable UserProvider provider, @Valid @RequestBody AuthDto.OAuthLoginRequest request, HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ipAddress = resolveClientIp(httpRequest);

        AuthService.LoginResult result = authService.oauthLogin(provider, request, userAgent, ipAddress);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshTokenPlain()).toString())
            .body(AuthDto.AuthResponse.of(result.accessToken(), result.user(), result.isNewUser()));
    }

    @Override
    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.TokenRefreshResponse> refresh(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken, HttpServletRequest httpRequest) {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ipAddress = resolveClientIp(httpRequest);

        AuthService.RefreshResult result = authService.refresh(refreshToken, userAgent, ipAddress);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshTokenPlain()).toString())
            .body(AuthDto.TokenRefreshResponse.of(result.accessToken(), result.user()));
    }

    @Override
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(refreshToken);

        ResponseCookie expired = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(jwtProperties.cookieSecure())
            .sameSite(jwtProperties.cookieSameSite())
            .path(REFRESH_COOKIE_PATH)
            .maxAge(0)
            .build();

        return ResponseEntity.noContent()
            .header(HttpHeaders.SET_COOKIE, expired.toString())
            .build();
    }

    // --- helpers ------------------------------------------------------------

    private ResponseCookie buildRefreshCookie(String token) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, token)
            .httpOnly(true)
            .secure(jwtProperties.cookieSecure())
            .sameSite(jwtProperties.cookieSameSite())
            .path(REFRESH_COOKIE_PATH)
            .maxAge(Duration.ofSeconds(jwtProperties.refreshTokenTtlSeconds()))
            .build();
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}

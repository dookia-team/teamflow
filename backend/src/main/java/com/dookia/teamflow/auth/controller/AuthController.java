package com.dookia.teamflow.auth.controller;

import com.dookia.teamflow.auth.config.JwtProperties;
import com.dookia.teamflow.auth.dto.AuthDto;
import com.dookia.teamflow.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * 인증 엔드포인트. auth-design.md §4 를 따른다.
 *  - POST /api/auth/google    로그인/회원가입
 *  - POST /api/auth/refresh   토큰 갱신 (Rotation)
 *  - POST /api/auth/logout    로그아웃
 */
@Tag(name = "Auth", description = "Google OAuth 로그인 · 토큰 갱신 · 로그아웃")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    public static final String REFRESH_COOKIE_NAME = "teamflow_rt";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;
    private final JwtProperties jwtProperties;

    @Operation(
        summary = "Google OAuth 로그인/회원가입",
        description = "Google Authorization Code Flow: 프론트가 전달한 code 로 Google token endpoint 를 "
            + "호출해 id_token 을 받고, 사용자 정보로 로그인 또는 신규 가입을 처리한다. "
            + "Access Token 은 응답 JSON 으로, Refresh Token 은 httpOnly Cookie(teamflow_rt) 로 전달된다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "로그인 성공"),
        @ApiResponse(responseCode = "400", description = "AUTH_MISSING_CODE / AUTH_INVALID_CODE"),
        @ApiResponse(responseCode = "502", description = "AUTH_GOOGLE_ERROR — Google 서버 통신 실패")
    })
    @SecurityRequirements // 인증 없이 호출하는 엔드포인트
    @PostMapping("/google")
    public ResponseEntity<AuthDto.AuthResponse> googleLogin(
        @Valid @RequestBody AuthDto.GoogleLoginRequest request,
        HttpServletRequest httpRequest
    ) {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ipAddress = resolveClientIp(httpRequest);

        var result = authService.login(request, userAgent, ipAddress);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshTokenPlain()).toString())
            .body(AuthDto.AuthResponse.of(result.accessToken(), result.user(), result.isNewUser()));
    }

    @Operation(
        summary = "Access Token 갱신 (Token Rotation)",
        description = "Cookie 의 teamflow_rt 를 검증해 새 Access Token 과 새 Refresh Token 을 발급한다. "
            + "이전 Refresh Token 은 used=true 로 마킹되며, 재사용 감지 시 family 전체가 무효화된다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "갱신 성공"),
        @ApiResponse(responseCode = "401",
            description = "AUTH_TOKEN_MISSING / AUTH_TOKEN_EXPIRED / AUTH_TOKEN_INVALID / AUTH_TOKEN_REUSED")
    })
    @SecurityRequirements // Cookie 인증이므로 Bearer 요구하지 않음
    @PostMapping("/refresh")
    public ResponseEntity<AuthDto.TokenRefreshResponse> refresh(
        @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
        HttpServletRequest httpRequest
    ) {
        String userAgent = httpRequest.getHeader(HttpHeaders.USER_AGENT);
        String ipAddress = resolveClientIp(httpRequest);

        var result = authService.refresh(refreshToken, userAgent, ipAddress);

        return ResponseEntity.ok()
            .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(result.refreshTokenPlain()).toString())
            .body(new AuthDto.TokenRefreshResponse(result.accessToken()));
    }

    @Operation(
        summary = "로그아웃",
        description = "Cookie 의 teamflow_rt 에 해당하는 Refresh Token 을 DB 에서 삭제하고, "
            + "Set-Cookie 로 Cookie 를 즉시 만료시킨다."
    )
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "로그아웃 처리 완료")
    })
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
        @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken
    ) {
        authService.logout(refreshToken);

        ResponseCookie expired = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(true)
            .sameSite("Strict")
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
            .secure(true)
            .sameSite("Strict")
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

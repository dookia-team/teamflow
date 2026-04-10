package com.dookia.teamflow.auth.controller;

import com.dookia.teamflow.auth.config.JwtProperties;
import com.dookia.teamflow.auth.dto.AuthDto;
import com.dookia.teamflow.auth.exception.AuthErrorCode;
import com.dookia.teamflow.auth.exception.AuthException;
import com.dookia.teamflow.auth.service.AuthService;
import com.dookia.teamflow.auth.service.JwtService;
import com.dookia.teamflow.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Field;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(AuthControllerTest.TestConfig.class)
class AuthControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean AuthService authService;
    // JwtAuthenticationFilter(@Component)가 @WebMvcTest에 의해 자동 스캔되어
    // JwtService 주입을 요구하므로 Mock Bean으로 충족시킨다. (addFilters=false 이므로 실제 호출은 되지 않음)
    @MockBean JwtService jwtService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtProperties jwtProperties() {
            return new JwtProperties(
                "test-secret-key-must-be-at-least-256-bits-long-aaaaaaaaaa",
                900,
                604800
            );
        }
    }

    @Test
    @DisplayName("POST /api/auth/google 성공 → 200 + Set-Cookie(teamflow_rt) + AuthResponse 본문")
    void googleLogin_success() throws Exception {
        UUID userId = UUID.randomUUID();
        User user = userWithId(userId, "g-1", "a@b.com", "홍길동");
        given(authService.login(any(AuthDto.GoogleLoginRequest.class), any(), any()))
            .willReturn(new AuthService.LoginResult(
                "access.jwt.token",
                "plain-refresh-token",
                user,
                true,
                OffsetDateTime.now().plusDays(7)
            ));

        var body = new AuthDto.GoogleLoginRequest("auth-code", "http://localhost:5173/auth/callback");

        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", equalTo("access.jwt.token")))
            .andExpect(jsonPath("$.isNewUser", equalTo(true)))
            .andExpect(jsonPath("$.user.id", equalTo(userId.toString())))
            .andExpect(jsonPath("$.user.email", equalTo("a@b.com")))
            .andExpect(header().string("Set-Cookie", containsString("teamflow_rt=plain-refresh-token")))
            .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
            .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")));
    }

    @Test
    @DisplayName("POST /api/auth/google 요청 body 누락 → 400")
    void googleLogin_missingCode_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"redirectUri\":\"http://localhost\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/refresh: 서비스가 AUTH_TOKEN_REUSED 던지면 401 + 표준 에러 포맷")
    void refresh_reused_returns401WithErrorPayload() throws Exception {
        willThrow(new AuthException(AuthErrorCode.AUTH_TOKEN_REUSED))
            .given(authService).refresh(any(), any(), any());

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("teamflow_rt", "reused-token")))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.error.code", equalTo("AUTH_TOKEN_REUSED")))
            .andExpect(jsonPath("$.error.message", containsString("재사용")))
            .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("POST /api/auth/logout → 204 + 만료 Set-Cookie")
    void logout_returns204AndExpiresCookie() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                .cookie(new jakarta.servlet.http.Cookie("teamflow_rt", "any")))
            .andExpect(status().isNoContent())
            .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    private static User userWithId(UUID id, String googleId, String email, String name) {
        User user = User.createFromGoogle(googleId, email, name, null);
        try {
            Field idField = User.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(user, id);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return user;
    }
}

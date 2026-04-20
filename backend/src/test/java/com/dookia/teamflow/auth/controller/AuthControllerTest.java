package com.dookia.teamflow.auth.controller;

import com.dookia.teamflow.auth.config.JwtProperties;
import com.dookia.teamflow.auth.dto.AuthDto;
import com.dookia.teamflow.auth.exception.AuthErrorCode;
import com.dookia.teamflow.auth.exception.AuthException;
import com.dookia.teamflow.auth.service.AuthService;
import com.dookia.teamflow.auth.service.JwtService;
import com.dookia.teamflow.user.entity.User;
import com.dookia.teamflow.user.entity.UserProvider;
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
import java.time.LocalDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @MockBean JwtService jwtService;

    @TestConfiguration
    static class TestConfig {
        @Bean
        JwtProperties jwtProperties() {
            return new JwtProperties(
                "test-secret-key-must-be-at-least-256-bits-long-aaaaaaaaaa",
                900,
                604800,
                "Lax",
                false
            );
        }
    }

    @Test
    @DisplayName("POST /api/auth/oauth/google 성공 → 200 + Set-Cookie(teamflow_rt) + AuthResponse 본문")
    void oauthLogin_google_success() throws Exception {
        long userNo = 123L;
        User user = userWithNo(userNo, "g-1", "a@b.com", "홍길동");
        given(authService.oauthLogin(eq(UserProvider.GOOGLE), any(AuthDto.OAuthLoginRequest.class), any(), any()))
            .willReturn(new AuthService.LoginResult(
                "access.jwt.token",
                "plain-refresh-token",
                user,
                true,
                LocalDateTime.now().plusDays(7)
            ));

        AuthDto.OAuthLoginRequest body = new AuthDto.OAuthLoginRequest("auth-code", "http://localhost:5173/auth/callback");

        mockMvc.perform(post("/api/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", equalTo("access.jwt.token")))
            .andExpect(jsonPath("$.isNewUser", equalTo(true)))
            .andExpect(jsonPath("$.user.no", equalTo((int) userNo)))
            .andExpect(jsonPath("$.user.email", equalTo("a@b.com")))
            .andExpect(jsonPath("$.user.provider", equalTo(UserProvider.GOOGLE.name())))
            .andExpect(header().string("Set-Cookie", containsString("teamflow_rt=plain-refresh-token")))
            .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
            .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")));
    }

    @Test
    @DisplayName("POST /api/auth/oauth/google 요청 body 누락 → 400")
    void oauthLogin_missingCode_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/oauth/google")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"redirectUri\":\"http://localhost\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/auth/refresh 성공 → 200 + accessToken + user + Set-Cookie")
    void refresh_success_returnsTokenAndUser() throws Exception {
        long userNo = 77L;
        User user = userWithNo(userNo, "g-2", "me@b.com", "김팀플");
        given(authService.refresh(any(), any(), any()))
            .willReturn(new AuthService.RefreshResult(
                "new.access.token",
                "new-refresh-token",
                user,
                LocalDateTime.now().plusDays(7)
            ));

        mockMvc.perform(post("/api/auth/refresh")
                .cookie(new jakarta.servlet.http.Cookie("teamflow_rt", "old-refresh-token")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken", equalTo("new.access.token")))
            .andExpect(jsonPath("$.user.no", equalTo((int) userNo)))
            .andExpect(jsonPath("$.user.email", equalTo("me@b.com")))
            .andExpect(jsonPath("$.user.name", equalTo("김팀플")))
            .andExpect(jsonPath("$.user.provider", equalTo(UserProvider.GOOGLE.name())))
            .andExpect(header().string("Set-Cookie", containsString("teamflow_rt=new-refresh-token")))
            .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
            .andExpect(header().string("Set-Cookie", containsString("Path=/api/auth")));
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

    private static User userWithNo(long no, String googleSub, String email, String name) {
        User user = User.createFromOAuth(UserProvider.GOOGLE, googleSub, email, name, null);
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

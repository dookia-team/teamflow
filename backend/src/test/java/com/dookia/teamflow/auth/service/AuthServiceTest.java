package com.dookia.teamflow.auth.service;

import com.dookia.teamflow.auth.config.JwtProperties;
import com.dookia.teamflow.auth.dto.AuthDto;
import com.dookia.teamflow.auth.entity.RefreshToken;
import com.dookia.teamflow.auth.exception.AuthErrorCode;
import com.dookia.teamflow.auth.exception.AuthException;
import com.dookia.teamflow.auth.oauth.OAuthProvider;
import com.dookia.teamflow.auth.oauth.OAuthProviderRegistry;
import com.dookia.teamflow.auth.oauth.OAuthUserInfo;
import com.dookia.teamflow.auth.repository.RefreshTokenRepository;
import com.dookia.teamflow.user.entity.User;
import com.dookia.teamflow.user.entity.UserProvider;
import com.dookia.teamflow.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private OAuthProvider googleProvider;
    @Mock private JwtService jwtService;

    private JwtProperties jwtProperties;
    private OAuthProviderRegistry oauthProviderRegistry;
    private AuthService authService;
    private final AtomicLong idSeq = new AtomicLong(1);

    @BeforeEach
    void setUp() {
        given(googleProvider.provider()).willReturn(UserProvider.GOOGLE);
        oauthProviderRegistry = new OAuthProviderRegistry(List.of(googleProvider));

        jwtProperties = new JwtProperties(
            "test-secret-key-must-be-at-least-256-bits-long-aaaaaaaaaa",
            900,
            604800,
            "Lax",
            false
        );
        authService = new AuthService(
            userRepository, refreshTokenRepository, oauthProviderRegistry, jwtService, jwtProperties
        );
    }

    @Test
    @DisplayName("신규 사용자 OAuth(Google) 로그인 → user 저장 + isNewUser=true")
    void oauthLogin_newUser_createsUserAndReturnsIsNewUserTrue() {
        OAuthUserInfo oauthUser = new OAuthUserInfo("g-123", "new@x.com", "신규", "http://pic");
        given(googleProvider.exchangeCodeForUser("code", "uri")).willReturn(oauthUser);
        given(userRepository.findByProviderAndProviderId(UserProvider.GOOGLE, "g-123")).willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(inv -> {
            User u = inv.getArgument(0);
            injectNo(u, idSeq.getAndIncrement());
            return u;
        });
        given(jwtService.issueAccessToken(any(User.class))).willReturn("access.jwt.token");

        AuthService.LoginResult result = authService.oauthLogin(
            UserProvider.GOOGLE,
            new AuthDto.OAuthLoginRequest("code", "uri"),
            "UA", "1.2.3.4"
        );

        assertThat(result.isNewUser()).isTrue();
        assertThat(result.accessToken()).isEqualTo("access.jwt.token");
        assertThat(result.refreshTokenPlain()).isNotBlank();
        verify(userRepository, times(1)).save(any(User.class));
        verify(refreshTokenRepository, times(1)).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("기존 사용자 OAuth(Google) 로그인 → 프로필 갱신 + isNewUser=false")
    void oauthLogin_existingUser_updatesProfile() {
        long userNo = 42L;
        User existing = userWithNo(userNo, "g-999", "old@x.com", "기존");
        OAuthUserInfo oauthUser = new OAuthUserInfo("g-999", "old@x.com", "기존 갱신", "http://pic2");
        given(googleProvider.exchangeCodeForUser("code", "uri")).willReturn(oauthUser);
        given(userRepository.findByProviderAndProviderId(UserProvider.GOOGLE, "g-999")).willReturn(Optional.of(existing));
        given(jwtService.issueAccessToken(existing)).willReturn("access.jwt.token");

        AuthService.LoginResult result = authService.oauthLogin(
            UserProvider.GOOGLE,
            new AuthDto.OAuthLoginRequest("code", "uri"),
            "UA", "1.2.3.4"
        );

        assertThat(result.isNewUser()).isFalse();
        assertThat(existing.getName()).isEqualTo("기존 갱신");
        assertThat(existing.getPicture()).isEqualTo("http://pic2");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("유효한 refresh token → Rotation 수행, 기존 토큰 used=true, 새 토큰 저장")
    void refresh_validToken_rotates() {
        long userNo = 10L;
        String familyId = UUID.randomUUID().toString();
        User user = userWithNo(userNo, "g-1", "a@b.com", "A");

        String plain = "plain-refresh-token-xyz";
        String hash = sha256Hex(plain);
        RefreshToken stored = RefreshToken.builder()
            .userNo(userNo)
            .tokenHash(hash)
            .familyId(familyId)
            .used(false)
            .userAgent("UA")
            .expireDate(LocalDateTime.now().plus(Duration.ofDays(3)))
            .build();

        given(refreshTokenRepository.findByTokenHash(hash)).willReturn(Optional.of(stored));
        given(userRepository.findById(userNo)).willReturn(Optional.of(user));
        given(jwtService.issueAccessToken(user)).willReturn("new.access.token");

        AuthService.RefreshResult result = authService.refresh(plain, "UA", "1.2.3.4");

        assertThat(stored.isUsed()).isTrue();
        assertThat(result.accessToken()).isEqualTo("new.access.token");
        assertThat(result.refreshTokenPlain()).isNotEqualTo(plain);
        assertThat(result.user()).isSameAs(user);

        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(captor.capture());
        assertThat(captor.getValue().getFamilyId()).isEqualTo(familyId);
        assertThat(captor.getValue().isUsed()).isFalse();
    }

    @Test
    @DisplayName("이미 used=true 인 refresh token → Replay 감지 + family 전체 삭제 + AUTH_TOKEN_REUSED")
    void refresh_reusedToken_triggersReplayDetection() {
        String familyId = UUID.randomUUID().toString();
        String plain = "plain-refresh-token";
        String hash = sha256Hex(plain);
        RefreshToken stored = RefreshToken.builder()
            .userNo(7L)
            .tokenHash(hash)
            .familyId(familyId)
            .used(true)
            .userAgent("UA")
            .expireDate(LocalDateTime.now().plus(Duration.ofDays(1)))
            .build();

        given(refreshTokenRepository.findByTokenHash(hash)).willReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(plain, "UA", "1.2.3.4"))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.AUTH_TOKEN_REUSED);

        verify(refreshTokenRepository).deleteByFamilyId(familyId);
    }

    @Test
    @DisplayName("만료된 refresh token → AUTH_TOKEN_EXPIRED")
    void refresh_expiredToken_throwsExpired() {
        String plain = "plain-refresh-token";
        String hash = sha256Hex(plain);
        RefreshToken stored = RefreshToken.builder()
            .userNo(5L)
            .tokenHash(hash)
            .familyId(UUID.randomUUID().toString())
            .used(false)
            .userAgent("UA")
            .expireDate(LocalDateTime.now().minus(Duration.ofDays(1)))
            .build();

        given(refreshTokenRepository.findByTokenHash(hash)).willReturn(Optional.of(stored));

        assertThatThrownBy(() -> authService.refresh(plain, "UA", "1.2.3.4"))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.AUTH_TOKEN_EXPIRED);
    }

    @Test
    @DisplayName("쿠키 없이 refresh 호출 → AUTH_TOKEN_MISSING")
    void refresh_nullToken_throwsMissing() {
        assertThatThrownBy(() -> authService.refresh(null, "UA", "1.2.3.4"))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.AUTH_TOKEN_MISSING);
    }

    @Test
    @DisplayName("DB에 존재하지 않는 refresh token → AUTH_TOKEN_INVALID")
    void refresh_unknownToken_throwsInvalid() {
        given(refreshTokenRepository.findByTokenHash(any())).willReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refresh("ghost-token", "UA", "1.2.3.4"))
            .isInstanceOf(AuthException.class)
            .extracting("errorCode")
            .isEqualTo(AuthErrorCode.AUTH_TOKEN_INVALID);
    }

    @Test
    @DisplayName("유효한 refresh token 로그아웃 → DB에서 해당 토큰 삭제")
    void logout_existingToken_deletes() {
        String plain = "plain-refresh-token";
        String hash = sha256Hex(plain);
        RefreshToken stored = RefreshToken.builder()
            .userNo(9L)
            .tokenHash(hash)
            .familyId(UUID.randomUUID().toString())
            .used(false)
            .userAgent("UA")
            .expireDate(LocalDateTime.now().plus(Duration.ofDays(1)))
            .build();
        given(refreshTokenRepository.findByTokenHash(hash)).willReturn(Optional.of(stored));

        authService.logout(plain);

        verify(refreshTokenRepository).delete(stored);
    }

    @Test
    @DisplayName("쿠키 없이 로그아웃 → 예외 없이 조용히 통과")
    void logout_nullToken_noop() {
        authService.logout(null);
        verify(refreshTokenRepository, never()).delete(any());
    }

    private static String sha256Hex(String input) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static User userWithNo(long no, String googleSub, String email, String name) {
        User user = User.createFromOAuth(UserProvider.GOOGLE, googleSub, email, name, null);
        injectNo(user, no);
        return user;
    }

    private static void injectNo(User user, long no) {
        try {
            Field noField = User.class.getDeclaredField("no");
            noField.setAccessible(true);
            noField.set(user, no);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}

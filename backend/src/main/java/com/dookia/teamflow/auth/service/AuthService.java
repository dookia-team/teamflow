package com.dookia.teamflow.auth.service;

import com.dookia.teamflow.auth.config.JwtProperties;
import com.dookia.teamflow.auth.dto.AuthDto;
import com.dookia.teamflow.auth.exception.AuthErrorCode;
import com.dookia.teamflow.auth.exception.AuthException;
import com.dookia.teamflow.token.entity.RefreshToken;
import com.dookia.teamflow.token.repository.RefreshTokenRepository;
import com.dookia.teamflow.user.entity.User;
import com.dookia.teamflow.user.entity.UserProvider;
import com.dookia.teamflow.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

/**
 * 인증 핵심 비즈니스 로직. auth-design.md §1~§2, ERD v0.1 §1 을 구현한다.
 *
 * <ul>
 *   <li>{@link #login} — Google OAuth 로그인/회원가입</li>
 *   <li>{@link #refresh} — Token Rotation + Replay Detection</li>
 *   <li>{@link #logout} — Refresh Token 폐기</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int REFRESH_TOKEN_RAW_BYTES = 48;

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final GoogleOAuthService googleOAuthService;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;

    public LoginResult login(AuthDto.GoogleLoginRequest request, String userAgent, String ipAddress) {
        var googleUser = googleOAuthService.exchangeCodeForUser(request.code(), request.redirectUri());

        Optional<User> existing = userRepository.findByProviderAndProviderId(UserProvider.GOOGLE, googleUser.sub());
        boolean isNewUser = existing.isEmpty();

        User user = existing.orElseGet(() -> userRepository.save(
            User.createFromGoogle(
                googleUser.sub(),
                googleUser.email(),
                googleUser.name(),
                googleUser.picture()
            )
        ));

        if (!isNewUser) {
            user.updateProfile(googleUser.name(), googleUser.picture());
        }

        String accessToken = jwtService.issueAccessToken(user);
        IssuedRefreshToken refresh = issueRefreshToken(user, UUID.randomUUID().toString(), userAgent, ipAddress);

        return new LoginResult(accessToken, refresh.plainToken(), user, isNewUser, refresh.expiresAt());
    }

    public RefreshResult refresh(String plainRefreshToken, String userAgent, String ipAddress) {
        if (plainRefreshToken == null || plainRefreshToken.isBlank()) {
            throw new AuthException(AuthErrorCode.AUTH_TOKEN_MISSING);
        }

        String hash = sha256Hex(plainRefreshToken);
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hash)
            .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_TOKEN_INVALID));

        if (stored.isExpired()) {
            throw new AuthException(AuthErrorCode.AUTH_TOKEN_EXPIRED);
        }

        if (stored.isUsed()) {
            log.warn("Refresh token replay detected. familyId={}, userNo={}", stored.getFamilyId(), stored.getUserNo());
            refreshTokenRepository.deleteByFamilyId(stored.getFamilyId());
            throw new AuthException(AuthErrorCode.AUTH_TOKEN_REUSED);
        }

        User user = userRepository.findById(stored.getUserNo())
            .orElseThrow(() -> new AuthException(AuthErrorCode.AUTH_TOKEN_INVALID));

        stored.markUsed();

        String accessToken = jwtService.issueAccessToken(user);
        IssuedRefreshToken newRefresh = issueRefreshToken(user, stored.getFamilyId(), userAgent, ipAddress);

        return new RefreshResult(accessToken, newRefresh.plainToken(), newRefresh.expiresAt());
    }

    public void logout(String plainRefreshToken) {
        if (plainRefreshToken == null || plainRefreshToken.isBlank()) {
            return;
        }
        String hash = sha256Hex(plainRefreshToken);
        refreshTokenRepository.findByTokenHash(hash)
            .ifPresent(refreshTokenRepository::delete);
    }

    // --- internal helpers ---------------------------------------------------

    private IssuedRefreshToken issueRefreshToken(User user, String familyId, String userAgent, String ipAddress) {
        byte[] randomBytes = new byte[REFRESH_TOKEN_RAW_BYTES];
        RANDOM.nextBytes(randomBytes);
        String plain = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String hash = sha256Hex(plain);

        LocalDateTime expiresAt = LocalDateTime.now()
            .plus(Duration.ofSeconds(jwtProperties.refreshTokenTtlSeconds()));

        RefreshToken entity = RefreshToken.builder()
            .userNo(user.getNo())
            .tokenHash(hash)
            .familyId(familyId)
            .used(false)
            .userAgent(userAgent != null ? userAgent : "unknown")
            .ipAddress(ipAddress)
            .expireDate(expiresAt)
            .build();

        refreshTokenRepository.save(entity);
        return new IssuedRefreshToken(plain, expiresAt);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    // --- result records -----------------------------------------------------

    public record LoginResult(
        String accessToken,
        String refreshTokenPlain,
        User user,
        boolean isNewUser,
        LocalDateTime refreshTokenExpiresAt
    ) {}

    public record RefreshResult(
        String accessToken,
        String refreshTokenPlain,
        LocalDateTime refreshTokenExpiresAt
    ) {}

    private record IssuedRefreshToken(String plainToken, LocalDateTime expiresAt) {}
}

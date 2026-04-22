package com.dookia.teamflow.auth.oauth;

import com.dookia.teamflow.auth.config.GoogleOAuthProperties;
import com.dookia.teamflow.exception.AuthErrorCode;
import com.dookia.teamflow.exception.AuthException;
import com.dookia.teamflow.user.entity.UserProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Base64;
import java.util.Map;

/**
 * Google OAuth 2.0 Authorization Code Flow 구현체.
 *  1. code -> Google token endpoint 교환
 *  2. id_token(JWT) payload 디코딩 -> 사용자 식별 정보 추출
 *
 * <p><b>NOTE</b>: id_token 서명 검증은 본 구현에서 생략한다. token endpoint 응답은
 * HTTPS로 Google 에서 직접 받은 값이므로 payload 디코딩만 수행하며, 운영 환경에서는
 * GoogleIdTokenVerifier 또는 RS256 공개키 검증 도입이 권장된다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleOAuthService implements OAuthProvider {

    private final GoogleOAuthProperties properties;
    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Override
    public UserProvider provider() {
        return UserProvider.GOOGLE;
    }

    @Override
    public OAuthUserInfo exchangeCodeForUser(String code, String redirectUri) {
        GoogleTokenResponse tokenResponse = requestToken(code, redirectUri);
        return decodeIdToken(tokenResponse.idToken());
    }

    private GoogleTokenResponse requestToken(String code, String redirectUri) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", properties.clientId());
        form.add("client_secret", properties.clientSecret());
        form.add("redirect_uri", redirectUri);
        form.add("grant_type", "authorization_code");

        try {
            GoogleTokenResponse response = restClient.post()
                .uri(properties.tokenUri())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED_VALUE)
                .body(form)
                .retrieve()
                .body(GoogleTokenResponse.class);

            if (response == null || response.idToken() == null || response.idToken().isBlank()) {
                throw new AuthException(AuthErrorCode.AUTH_INVALID_CODE);
            }
            return response;
        } catch (RestClientException e) {
            log.warn("Google token endpoint 호출 실패: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.AUTH_GOOGLE_ERROR, e);
        }
    }

    @SuppressWarnings("unchecked")
    private OAuthUserInfo decodeIdToken(String idToken) {
        try {
            String[] parts = idToken.split("\\.");
            if (parts.length < 2) {
                throw new AuthException(AuthErrorCode.AUTH_GOOGLE_ERROR);
            }
            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[1]);
            Map<String, Object> payload = objectMapper.readValue(payloadBytes, Map.class);
            return new OAuthUserInfo(
                (String) payload.get("sub"),
                (String) payload.get("email"),
                (String) payload.get("name"),
                (String) payload.get("picture")
            );
        } catch (AuthException e) {
            throw e;
        } catch (Exception e) {
            log.warn("id_token payload 디코딩 실패: {}", e.getMessage());
            throw new AuthException(AuthErrorCode.AUTH_GOOGLE_ERROR, e);
        }
    }

    private record GoogleTokenResponse(
        @JsonProperty("access_token") String accessToken,
        @JsonProperty("id_token") String idToken,
        @JsonProperty("refresh_token") String refreshToken,
        @JsonProperty("expires_in") Integer expiresIn,
        @JsonProperty("token_type") String tokenType
    ) {
    }
}

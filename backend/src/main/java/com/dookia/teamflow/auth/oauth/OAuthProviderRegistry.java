package com.dookia.teamflow.auth.oauth;

import com.dookia.teamflow.exception.AuthErrorCode;
import com.dookia.teamflow.exception.AuthException;
import com.dookia.teamflow.user.entity.UserProvider;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 등록된 OAuthProvider 구현체들을 UserProvider 키로 조회한다.
 * Spring 이 모든 OAuthProvider Bean 을 List 로 주입하면 Map 으로 정규화한다.
 */
@Component
public class OAuthProviderRegistry {

    private final Map<UserProvider, OAuthProvider> providers;

    public OAuthProviderRegistry(List<OAuthProvider> providers) {
        this.providers = providers.stream()
            .collect(Collectors.toUnmodifiableMap(OAuthProvider::provider, Function.identity()));
    }

    public OAuthProvider get(UserProvider provider) {
        OAuthProvider impl = providers.get(provider);
        if (impl == null) {
            throw new AuthException(AuthErrorCode.AUTH_UNSUPPORTED_PROVIDER);
        }
        return impl;
    }
}

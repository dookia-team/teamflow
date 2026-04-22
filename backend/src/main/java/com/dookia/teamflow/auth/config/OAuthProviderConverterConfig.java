package com.dookia.teamflow.auth.config;

import com.dookia.teamflow.exception.AuthErrorCode;
import com.dookia.teamflow.exception.AuthException;
import com.dookia.teamflow.user.entity.UserProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * URL path variable 의 OAuth provider 문자열을 대소문자 무관하게 UserProvider enum 으로 변환한다.
 * 예: "/api/auth/oauth/google" → UserProvider.GOOGLE
 */
@Configuration
public class OAuthProviderConverterConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(String.class, UserProvider.class, source -> {
            try {
                return UserProvider.valueOf(source.toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new AuthException(AuthErrorCode.AUTH_UNSUPPORTED_PROVIDER);
            }
        });
    }
}

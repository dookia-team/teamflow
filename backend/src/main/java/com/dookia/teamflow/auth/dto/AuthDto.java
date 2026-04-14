package com.dookia.teamflow.auth.dto;

import com.dookia.teamflow.user.entity.User;
import com.dookia.teamflow.user.entity.UserProvider;
import jakarta.validation.constraints.NotBlank;

/**
 * 인증 도메인 요청/응답 DTO. backend-conventions.md 규칙에 따라 {Domain}Dto.java 한 파일에 inner record로 선언한다.
 * 필드 네이밍은 ERD v0.1 기준 (USER PK = no).
 */
public class AuthDto {

    private AuthDto() {
    }

    public record GoogleLoginRequest(
        @NotBlank(message = "code는 필수입니다.") String code,
        @NotBlank(message = "redirectUri는 필수입니다.") String redirectUri
    ) {}

    public record AuthResponse(
        String accessToken,
        UserInfo user,
        boolean isNewUser
    ) {
        public static AuthResponse of(String accessToken, User user, boolean isNewUser) {
            return new AuthResponse(accessToken, UserInfo.from(user), isNewUser);
        }
    }

    public record TokenRefreshResponse(String accessToken) {
    }

    public record UserInfo(
        Long no,
        String email,
        String name,
        String picture,
        UserProvider provider
    ) {
        public static UserInfo from(User user) {
            return new UserInfo(
                user.getNo(),
                user.getEmail(),
                user.getName(),
                user.getPicture(),
                user.getProvider()
            );
        }
    }
}

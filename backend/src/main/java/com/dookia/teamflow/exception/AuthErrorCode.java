package com.dookia.teamflow.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * auth-design.md §4에 정의된 인증 에러 코드 목록.
 * {@link ErrorCode} 공통 인터페이스를 구현해 핸들러/필터/ApiResponse 가 CommonErrorCode 와 동일하게 처리 가능.
 */
@Getter
public enum AuthErrorCode implements ErrorCode {

    AUTH_MISSING_CODE(HttpStatus.BAD_REQUEST, "code 파라미터가 필요합니다."),
    AUTH_INVALID_CODE(HttpStatus.BAD_REQUEST, "Authorization code가 유효하지 않습니다."),
    AUTH_UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 OAuth 제공자입니다."),
    AUTH_GOOGLE_ERROR(HttpStatus.BAD_GATEWAY, "Google 서버 통신에 실패했습니다."),

    AUTH_TOKEN_MISSING(HttpStatus.UNAUTHORIZED, "인증 토큰이 없습니다."),
    AUTH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다."),
    AUTH_TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "토큰이 유효하지 않습니다."),
    AUTH_TOKEN_REUSED(HttpStatus.UNAUTHORIZED, "사용된 토큰이 재사용되었습니다. 모든 세션이 무효화되었습니다.");

    private final HttpStatus status;
    private final String message;

    AuthErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    @Override
    public String code() {
        return this.name();
    }
}

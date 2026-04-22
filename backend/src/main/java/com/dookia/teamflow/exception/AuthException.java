package com.dookia.teamflow.exception;

import lombok.Getter;

/**
 * 인증 관련 비즈니스 예외. GlobalExceptionHandler에서 표준 에러 응답으로 변환된다.
 */
@Getter
public class AuthException extends RuntimeException {

    private final AuthErrorCode errorCode;

    public AuthException(AuthErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AuthException(AuthErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}

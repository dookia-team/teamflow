package com.dookia.teamflow.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 공통(비도메인) 에러 코드. HTTP 상태와 기계 판독용 code 를 한 곳에서 관리한다.
 * 도메인 고유 코드는 별도 enum(예: {@code AuthErrorCode})으로 유지한다.
 */
@Getter
public enum CommonErrorCode implements ErrorCode {

    ENTITY_NOT_FOUND(HttpStatus.NOT_FOUND),
    FORBIDDEN(HttpStatus.FORBIDDEN),
    CONFLICT(HttpStatus.CONFLICT),
    BAD_REQUEST(HttpStatus.BAD_REQUEST),
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST),
    INVALID_REQUEST_BODY(HttpStatus.BAD_REQUEST),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR);

    private final HttpStatus status;

    CommonErrorCode(HttpStatus status) {
        this.status = status;
    }

    @Override
    public String code() {
        return this.name();
    }
}

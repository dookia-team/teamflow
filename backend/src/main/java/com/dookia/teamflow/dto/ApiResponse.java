package com.dookia.teamflow.dto;

import com.dookia.teamflow.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 공통 API 응답 envelope.
 * - 성공: {@code {success:true, data, timestamp}}
 * - 에러: {@code {success:false, code, message, timestamp}} — code 는 프론트 분기 키 (AUTH_TOKEN_EXPIRED 등)
 * null 필드는 직렬화에서 제외한다.
 */
@Getter
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String code;
    private final String message;
    private final String timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true)
            .data(data)
            .timestamp(LocalDateTime.now().toString())
            .build();
    }

    /**
     * 구조화된 에러 응답. code 는 프론트 로직 분기에 사용되는 기계 읽기용 키,
     * message 는 사람이 읽는 한글 메시지.
     */
    public static <T> ApiResponse<T> error(String code, String message) {
        return ApiResponse.<T>builder()
            .success(false)
            .code(code)
            .message(message)
            .timestamp(LocalDateTime.now().toString())
            .build();
    }

    /**
     * {@link ErrorCode} 을 구현한 enum(AuthErrorCode/CommonErrorCode 등)을 그대로 받는 편의 오버로드.
     * 호출부에서 {@code code.code()} 반복 호출을 제거한다.
     */
    public static <T> ApiResponse<T> error(ErrorCode code, String message) {
        return error(code.code(), message);
    }
}

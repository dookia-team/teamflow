package com.dookia.teamflow.exception;

import org.springframework.http.HttpStatus;

/**
 * 모든 에러 코드 enum이 구현해야 하는 공통 인터페이스.
 * 프론트 분기용 기계 판독 코드(String)와 HTTP 상태를 한 쌍으로 묶어
 * GlobalExceptionHandler/Filter/ApiResponse 가 도메인에 관계없이 동일하게 처리할 수 있게 한다.
 *
 * 구현체:
 *  - {@link CommonErrorCode} — 도메인 비종속 공통 에러
 *  - {@link com.dookia.teamflow.exception.AuthErrorCode} — 인증 도메인 고유 에러
 */
public interface ErrorCode {

    HttpStatus getStatus();

    /** 기계 판독용 안정 키. 기본적으로 enum 이름을 그대로 노출한다. */
    String code();
}

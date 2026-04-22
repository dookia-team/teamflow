package com.dookia.teamflow.exception;

import com.dookia.teamflow.exception.AuthException;
import com.dookia.teamflow.dto.ApiResponse;
import com.dookia.teamflow.exception.WorkspaceAccessDeniedException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 전역 예외 처리. 모든 응답은 {@link ApiResponse} envelope 으로 통일된다.
 * code 필드는 프론트가 기계 판독으로 분기할 수 있는 안정 키로,
 * {@link ErrorCode} 인터페이스를 구현한 enum(AuthErrorCode/CommonErrorCode)에서 가져온다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(AuthException.class)
    public ResponseEntity<ApiResponse<Void>> handleAuth(AuthException e) {
        return build(e.getErrorCode(), e.getErrorCode().getMessage());
    }

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(EntityNotFoundException e) {
        return build(CommonErrorCode.ENTITY_NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(WorkspaceAccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleWorkspaceAccessDenied(WorkspaceAccessDeniedException e) {
        return build(CommonErrorCode.FORBIDDEN, e.getMessage());
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalState(IllegalStateException e) {
        return build(CommonErrorCode.CONFLICT, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException e) {
        return build(CommonErrorCode.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getDefaultMessage())
            .findFirst()
            .orElse("입력값 오류");
        return build(CommonErrorCode.VALIDATION_ERROR, message);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return build(CommonErrorCode.INVALID_REQUEST_BODY, "요청 본문이 올바르지 않습니다.");
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException e) {
        return build(CommonErrorCode.INTERNAL_ERROR, e.getMessage());
    }

    /**
     * 도메인 구분 없이 {@link ErrorCode} 어느 구현체든 동일하게 응답을 조립한다.
     */
    private ResponseEntity<ApiResponse<Void>> build(ErrorCode code, String message) {
        return ResponseEntity.status(code.getStatus())
            .body(ApiResponse.error(code, message));
    }
}

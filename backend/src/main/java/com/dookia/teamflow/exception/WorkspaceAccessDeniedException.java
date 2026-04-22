package com.dookia.teamflow.exception;

/**
 * 워크스페이스 권한 부족. GlobalExceptionHandler 에서 403 으로 매핑한다.
 */
public class WorkspaceAccessDeniedException extends RuntimeException {

    public WorkspaceAccessDeniedException(String message) {
        super(message);
    }
}

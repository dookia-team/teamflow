package com.dookia.teamflow.common.exception;

/**
 * 공통 엔티티 미존재 예외. backend-conventions.md 규칙에 따라 엔티티별 예외 클래스를 따로 두지 않고
 * 엔티티명 + 식별자를 파라미터로 받아 공용 예외로 처리한다.
 */
public class EntityNotFoundException extends RuntimeException {

    public EntityNotFoundException(String entity, Object id) {
        super("%s(no=%s) 를 찾을 수 없습니다.".formatted(entity, id));
    }
}

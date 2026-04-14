package com.dookia.teamflow.workspace.entity;

/**
 * WORKSPACE_MEMBER.role / WORKSPACE_INVITATION.role 값. ERD v0.1 §2.
 * 초대 시에는 OWNER 를 지정할 수 없으며, 서비스 레이어에서 검증한다.
 */
public enum WorkspaceMemberRole {
    OWNER,
    MEMBER,
    GUEST
}

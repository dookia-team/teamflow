package com.dookia.teamflow.workspace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WORKSPACE_INVITATION 엔티티. ERD v0.1 §2.
 * 초대 role 은 MEMBER/GUEST 만 허용 (OWNER 초대 불가) — 서비스 레이어에서 검증.
 */
@Entity
@Table(name = "workspace_invitation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class WorkspaceInvitation {

    public static final long DEFAULT_TTL_DAYS = 7L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no")
    private Long no;

    @Column(name = "workspace_no", nullable = false)
    private Long workspaceNo;

    @Column(name = "invitee_user_no", nullable = false)
    private Long inviteeUserNo;

    @Column(name = "inviter_user_no", nullable = false)
    private Long inviterUserNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WorkspaceMemberRole role;

    @Column(nullable = false, unique = true, length = 128)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WorkspaceInvitationStatus status;

    @Column(name = "expire_date", nullable = false)
    private LocalDateTime expireDate;

    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @PrePersist
    void onCreate() {
        if (createDate == null) {
            createDate = LocalDateTime.now();
        }
    }

    public void accept() {
        this.status = WorkspaceInvitationStatus.ACCEPTED;
    }

    public void revoke() {
        this.status = WorkspaceInvitationStatus.REVOKED;
    }

    public void expire() {
        this.status = WorkspaceInvitationStatus.EXPIRED;
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expireDate);
    }

    public boolean isPending() {
        return status == WorkspaceInvitationStatus.PENDING;
    }

    public static WorkspaceInvitation issue(Long workspaceNo, Long inviteeUserNo, Long inviterUserNo,
                                            WorkspaceMemberRole role, String token) {
        return WorkspaceInvitation.builder()
            .workspaceNo(workspaceNo)
            .inviteeUserNo(inviteeUserNo)
            .inviterUserNo(inviterUserNo)
            .role(role)
            .token(token)
            .status(WorkspaceInvitationStatus.PENDING)
            .expireDate(LocalDateTime.now().plusDays(DEFAULT_TTL_DAYS))
            .build();
    }
}

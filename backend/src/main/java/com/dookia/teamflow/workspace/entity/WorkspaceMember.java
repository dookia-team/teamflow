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
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * WORKSPACE_MEMBER 엔티티. ERD v0.1 §2.
 */
@Entity
@Table(
    name = "workspace_member",
    uniqueConstraints = @UniqueConstraint(name = "uk_wm", columnNames = {"workspace_no", "user_no"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class WorkspaceMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no")
    private Long no;

    @Column(name = "workspace_no", nullable = false)
    private Long workspaceNo;

    @Column(name = "user_no", nullable = false)
    private Long userNo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private WorkspaceMemberRole role;

    @Column(name = "join_date", nullable = false, updatable = false)
    private LocalDateTime joinDate;

    @PrePersist
    void onCreate() {
        if (joinDate == null) {
            joinDate = LocalDateTime.now();
        }
    }

    public void changeRole(WorkspaceMemberRole role) {
        this.role = role;
    }

    public static WorkspaceMember of(Long workspaceNo, Long userNo, WorkspaceMemberRole role) {
        return WorkspaceMember.builder()
            .workspaceNo(workspaceNo)
            .userNo(userNo)
            .role(role)
            .build();
    }
}

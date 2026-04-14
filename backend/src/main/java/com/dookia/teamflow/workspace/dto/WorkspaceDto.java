package com.dookia.teamflow.workspace.dto;

import com.dookia.teamflow.workspace.entity.Workspace;
import com.dookia.teamflow.workspace.entity.WorkspaceInvitation;
import com.dookia.teamflow.workspace.entity.WorkspaceInvitationStatus;
import com.dookia.teamflow.workspace.entity.WorkspaceMember;
import com.dookia.teamflow.workspace.entity.WorkspaceMemberRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 워크스페이스 도메인 요청/응답 DTO. backend-conventions.md 규칙에 따라 {Domain}Dto.java 하나에 inner record 로 선언.
 * 식별자는 ERD v0.1 에 따라 `no` (Long).
 */
public class WorkspaceDto {

    private WorkspaceDto() {
    }

    public record CreateRequest(
        @NotBlank(message = "이름은 필수입니다.")
        @Size(min = 2, max = 100, message = "이름은 2~100자여야 합니다.")
        String name
    ) {}

    public record Response(
        Long no,
        String name,
        String slug,
        WorkspaceMemberRole role,
        long memberCount
    ) {
        public static Response of(Workspace ws, WorkspaceMemberRole role, long memberCount) {
            return new Response(ws.getNo(), ws.getName(), ws.getSlug(), role, memberCount);
        }
    }

    public record SummaryResponse(
        Long no,
        String name,
        String slug,
        long memberCount
    ) {
        public static SummaryResponse of(Workspace ws, long memberCount) {
            return new SummaryResponse(ws.getNo(), ws.getName(), ws.getSlug(), memberCount);
        }
    }

    public record DetailResponse(
        Long no,
        String name,
        String slug,
        List<MemberEntry> members
    ) {
        public static DetailResponse of(Workspace ws, List<MemberEntry> members) {
            return new DetailResponse(ws.getNo(), ws.getName(), ws.getSlug(), members);
        }
    }

    public record MemberEntry(
        Long no,
        Long userNo,
        WorkspaceMemberRole role,
        LocalDateTime joinDate
    ) {
        public static MemberEntry from(WorkspaceMember m) {
            return new MemberEntry(m.getNo(), m.getUserNo(), m.getRole(), m.getJoinDate());
        }
    }

    public record InviteRequest(
        @NotNull(message = "inviteeUserNo 는 필수입니다.") Long inviteeUserNo,
        @NotNull(message = "role 은 필수입니다.") WorkspaceMemberRole role
    ) {}

    public record InvitationResponse(
        Long no,
        Long workspaceNo,
        Long inviteeUserNo,
        WorkspaceMemberRole role,
        WorkspaceInvitationStatus status,
        String token,
        LocalDateTime expireDate
    ) {
        public static InvitationResponse from(WorkspaceInvitation inv) {
            return new InvitationResponse(
                inv.getNo(),
                inv.getWorkspaceNo(),
                inv.getInviteeUserNo(),
                inv.getRole(),
                inv.getStatus(),
                inv.getToken(),
                inv.getExpireDate()
            );
        }
    }
}

package com.dookia.teamflow.workspace.service;

import com.dookia.teamflow.exception.EntityNotFoundException;
import com.dookia.teamflow.user.repository.UserRepository;
import com.dookia.teamflow.workspace.dto.WorkspaceDto;
import com.dookia.teamflow.workspace.entity.Workspace;
import com.dookia.teamflow.workspace.entity.WorkspaceInvitation;
import com.dookia.teamflow.workspace.entity.WorkspaceMember;
import com.dookia.teamflow.workspace.entity.WorkspaceMemberRole;
import com.dookia.teamflow.workspace.exception.WorkspaceAccessDeniedException;
import com.dookia.teamflow.workspace.repository.WorkspaceInvitationRepository;
import com.dookia.teamflow.workspace.repository.WorkspaceMemberRepository;
import com.dookia.teamflow.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;

/**
 * WORKSPACE 도메인 비즈니스 로직. ERD v0.1 §2 + HANDOFF.md §2 를 구현한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class WorkspaceService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int INVITE_TOKEN_BYTES = 48;

    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;
    private final WorkspaceInvitationRepository workspaceInvitationRepository;
    private final UserRepository userRepository;

    public WorkspaceDto.Response create(Long ownerUserNo, WorkspaceDto.CreateRequest request) {
        Workspace ws = workspaceRepository.save(Workspace.create(request.name()));
        workspaceMemberRepository.save(WorkspaceMember.of(ws.getNo(), ownerUserNo, WorkspaceMemberRole.OWNER));
        return WorkspaceDto.Response.of(ws, WorkspaceMemberRole.OWNER, 1L);
    }

    @Transactional(readOnly = true)
    public List<WorkspaceDto.SummaryResponse> listForUser(Long userNo) {
        List<WorkspaceMember> memberships = workspaceMemberRepository.findAllByUserNo(userNo);
        return memberships.stream()
            .sorted(Comparator.comparing(WorkspaceMember::getJoinDate))
            .map(m -> {
                Workspace ws = workspaceRepository.findById(m.getWorkspaceNo())
                    .orElseThrow(() -> new EntityNotFoundException("Workspace", m.getWorkspaceNo()));
                long count = workspaceMemberRepository.countByWorkspaceNo(ws.getNo());
                return WorkspaceDto.SummaryResponse.of(ws, count);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public WorkspaceDto.DetailResponse getDetail(Long workspaceNo, Long userNo) {
        Workspace ws = workspaceRepository.findById(workspaceNo)
            .orElseThrow(() -> new EntityNotFoundException("Workspace", workspaceNo));
        requireMember(workspaceNo, userNo);

        List<WorkspaceDto.MemberEntry> members = workspaceMemberRepository.findAllByWorkspaceNo(workspaceNo)
            .stream()
            .sorted(Comparator.comparing(WorkspaceMember::getJoinDate))
            .map(WorkspaceDto.MemberEntry::from)
            .toList();
        return WorkspaceDto.DetailResponse.of(ws, members);
    }

    public WorkspaceDto.InvitationResponse invite(Long workspaceNo, Long inviterUserNo, WorkspaceDto.InviteRequest request) {
        if (request.role() == WorkspaceMemberRole.OWNER) {
            throw new IllegalArgumentException("OWNER 로는 초대할 수 없습니다.");
        }
        WorkspaceMember inviter = requireMember(workspaceNo, inviterUserNo);
        if (inviter.getRole() != WorkspaceMemberRole.OWNER) {
            throw new WorkspaceAccessDeniedException("초대는 OWNER 만 가능합니다.");
        }
        if (!userRepository.existsById(request.inviteeUserNo())) {
            throw new EntityNotFoundException("User", request.inviteeUserNo());
        }
        if (workspaceMemberRepository.existsByWorkspaceNoAndUserNo(workspaceNo, request.inviteeUserNo())) {
            throw new IllegalStateException("이미 워크스페이스 멤버입니다.");
        }

        String token = generateInviteToken();
        WorkspaceInvitation inv = workspaceInvitationRepository.save(
            WorkspaceInvitation.issue(workspaceNo, request.inviteeUserNo(), inviterUserNo, request.role(), token)
        );
        return WorkspaceDto.InvitationResponse.from(inv);
    }

    // --- helpers ------------------------------------------------------------

    private WorkspaceMember requireMember(Long workspaceNo, Long userNo) {
        return workspaceMemberRepository.findByWorkspaceNoAndUserNo(workspaceNo, userNo)
            .orElseThrow(() -> new WorkspaceAccessDeniedException("워크스페이스 멤버가 아닙니다."));
    }

    private static String generateInviteToken() {
        byte[] bytes = new byte[INVITE_TOKEN_BYTES];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}

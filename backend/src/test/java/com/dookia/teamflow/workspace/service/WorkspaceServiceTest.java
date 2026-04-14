package com.dookia.teamflow.workspace.service;

import com.dookia.teamflow.common.exception.EntityNotFoundException;
import com.dookia.teamflow.user.repository.UserRepository;
import com.dookia.teamflow.workspace.dto.WorkspaceDto;
import com.dookia.teamflow.workspace.entity.Workspace;
import com.dookia.teamflow.workspace.entity.WorkspaceInvitation;
import com.dookia.teamflow.workspace.entity.WorkspaceInvitationStatus;
import com.dookia.teamflow.workspace.entity.WorkspaceMember;
import com.dookia.teamflow.workspace.entity.WorkspaceMemberRole;
import com.dookia.teamflow.workspace.exception.WorkspaceAccessDeniedException;
import com.dookia.teamflow.workspace.repository.WorkspaceInvitationRepository;
import com.dookia.teamflow.workspace.repository.WorkspaceMemberRepository;
import com.dookia.teamflow.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WorkspaceServiceTest {

    @Mock WorkspaceRepository workspaceRepository;
    @Mock WorkspaceMemberRepository workspaceMemberRepository;
    @Mock WorkspaceInvitationRepository workspaceInvitationRepository;
    @Mock UserRepository userRepository;

    @InjectMocks WorkspaceService workspaceService;

    @Test
    @DisplayName("create → 워크스페이스 + OWNER 멤버 저장 후 role=OWNER 응답")
    void create_savesOwnerMember() {
        given(workspaceRepository.save(any(Workspace.class)))
            .willAnswer(inv -> injectNo(inv.getArgument(0, Workspace.class), 10L));

        WorkspaceDto.Response res = workspaceService.create(1L, new WorkspaceDto.CreateRequest("Alpha"));

        assertThat(res.no()).isEqualTo(10L);
        assertThat(res.role()).isEqualTo(WorkspaceMemberRole.OWNER);
        assertThat(res.memberCount()).isEqualTo(1L);

        ArgumentCaptor<WorkspaceMember> captor = ArgumentCaptor.forClass(WorkspaceMember.class);
        verify(workspaceMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getUserNo()).isEqualTo(1L);
        assertThat(captor.getValue().getRole()).isEqualTo(WorkspaceMemberRole.OWNER);
    }

    @Test
    @DisplayName("listForUser → 가입한 모든 워크스페이스를 요약 응답으로 반환")
    void listForUser_returnsAllMemberships() {
        Workspace ws = injectNo(Workspace.create("W"), 5L);
        WorkspaceMember m = WorkspaceMember.of(5L, 1L, WorkspaceMemberRole.OWNER);
        given(workspaceMemberRepository.findAllByUserNo(1L)).willReturn(List.of(m));
        given(workspaceRepository.findById(5L)).willReturn(Optional.of(ws));
        given(workspaceMemberRepository.countByWorkspaceNo(5L)).willReturn(3L);

        var list = workspaceService.listForUser(1L);

        assertThat(list).hasSize(1);
        assertThat(list.get(0).no()).isEqualTo(5L);
        assertThat(list.get(0).memberCount()).isEqualTo(3L);
    }

    @Test
    @DisplayName("getDetail → 비멤버는 WorkspaceAccessDeniedException")
    void getDetail_nonMember_denied() {
        Workspace ws = injectNo(Workspace.create("W"), 7L);
        given(workspaceRepository.findById(7L)).willReturn(Optional.of(ws));
        given(workspaceMemberRepository.findByWorkspaceNoAndUserNo(7L, 99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> workspaceService.getDetail(7L, 99L))
            .isInstanceOf(WorkspaceAccessDeniedException.class);
    }

    @Test
    @DisplayName("invite → OWNER가 아니면 거부")
    void invite_nonOwner_denied() {
        given(workspaceMemberRepository.findByWorkspaceNoAndUserNo(1L, 2L))
            .willReturn(Optional.of(WorkspaceMember.of(1L, 2L, WorkspaceMemberRole.MEMBER)));

        assertThatThrownBy(() -> workspaceService.invite(1L, 2L,
            new WorkspaceDto.InviteRequest(3L, WorkspaceMemberRole.MEMBER)))
            .isInstanceOf(WorkspaceAccessDeniedException.class);
    }

    @Test
    @DisplayName("invite → OWNER role 초대 시도는 IllegalArgumentException")
    void invite_ownerRole_rejected() {
        assertThatThrownBy(() -> workspaceService.invite(1L, 2L,
            new WorkspaceDto.InviteRequest(3L, WorkspaceMemberRole.OWNER)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("invite → 대상 USER 없으면 EntityNotFoundException")
    void invite_inviteeMissing_throws() {
        given(workspaceMemberRepository.findByWorkspaceNoAndUserNo(1L, 2L))
            .willReturn(Optional.of(WorkspaceMember.of(1L, 2L, WorkspaceMemberRole.OWNER)));
        given(userRepository.existsById(99L)).willReturn(false);

        assertThatThrownBy(() -> workspaceService.invite(1L, 2L,
            new WorkspaceDto.InviteRequest(99L, WorkspaceMemberRole.MEMBER)))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("invite → 이미 멤버인 사용자 초대 시 IllegalStateException")
    void invite_alreadyMember_throws() {
        given(workspaceMemberRepository.findByWorkspaceNoAndUserNo(1L, 2L))
            .willReturn(Optional.of(WorkspaceMember.of(1L, 2L, WorkspaceMemberRole.OWNER)));
        given(userRepository.existsById(3L)).willReturn(true);
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(1L, 3L)).willReturn(true);

        assertThatThrownBy(() -> workspaceService.invite(1L, 2L,
            new WorkspaceDto.InviteRequest(3L, WorkspaceMemberRole.MEMBER)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("invite → OWNER 정상 초대 시 PENDING 초대장 저장 + 토큰 발급")
    void invite_success_createsPendingInvitation() {
        given(workspaceMemberRepository.findByWorkspaceNoAndUserNo(1L, 2L))
            .willReturn(Optional.of(WorkspaceMember.of(1L, 2L, WorkspaceMemberRole.OWNER)));
        given(userRepository.existsById(3L)).willReturn(true);
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(1L, 3L)).willReturn(false);
        given(workspaceInvitationRepository.save(any(WorkspaceInvitation.class)))
            .willAnswer(inv -> inv.getArgument(0));

        WorkspaceDto.InvitationResponse res = workspaceService.invite(1L, 2L,
            new WorkspaceDto.InviteRequest(3L, WorkspaceMemberRole.MEMBER));

        assertThat(res.status()).isEqualTo(WorkspaceInvitationStatus.PENDING);
        assertThat(res.inviteeUserNo()).isEqualTo(3L);
        assertThat(res.role()).isEqualTo(WorkspaceMemberRole.MEMBER);
        assertThat(res.token()).isNotBlank();
    }

    // ---------- helpers ----------

    private static Workspace injectNo(Workspace ws, long no) {
        try {
            Field f = Workspace.class.getDeclaredField("no");
            f.setAccessible(true);
            f.set(ws, no);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return ws;
    }
}

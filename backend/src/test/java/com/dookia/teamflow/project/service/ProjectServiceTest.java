package com.dookia.teamflow.project.service;

import com.dookia.teamflow.exception.EntityNotFoundException;
import com.dookia.teamflow.project.dto.ProjectDto;
import com.dookia.teamflow.project.entity.Project;
import com.dookia.teamflow.project.entity.ProjectMember;
import com.dookia.teamflow.project.entity.ProjectMemberRole;
import com.dookia.teamflow.project.entity.ProjectStatus;
import com.dookia.teamflow.project.entity.ProjectVisibility;
import com.dookia.teamflow.project.repository.ProjectMemberRepository;
import com.dookia.teamflow.project.repository.ProjectRepository;
import com.dookia.teamflow.exception.WorkspaceAccessDeniedException;
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
class ProjectServiceTest {

    @Mock ProjectRepository projectRepository;
    @Mock ProjectMemberRepository projectMemberRepository;
    @Mock WorkspaceRepository workspaceRepository;
    @Mock WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks ProjectService projectService;

    @Test
    @DisplayName("create → 정상 흐름: 프로젝트 저장 + OWNER 멤버 등록")
    void create_success() {
        given(workspaceRepository.existsById(1L)).willReturn(true);
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(1L, 2L)).willReturn(true);
        given(projectRepository.existsByWorkspaceNoAndKey(1L, "TF")).willReturn(false);
        given(projectRepository.save(any(Project.class)))
            .willAnswer(inv -> injectNo(inv.getArgument(0, Project.class), 50L));

        ProjectDto.Response res = projectService.create(1L, 2L,
            new ProjectDto.CreateRequest("TeamFlow", "TF", "desc", ProjectVisibility.PRIVATE));

        assertThat(res.no()).isEqualTo(50L);
        assertThat(res.key()).isEqualTo("TF");
        assertThat(res.status()).isEqualTo(ProjectStatus.ACTIVE);

        ArgumentCaptor<ProjectMember> captor = ArgumentCaptor.forClass(ProjectMember.class);
        verify(projectMemberRepository).save(captor.capture());
        assertThat(captor.getValue().getUserNo()).isEqualTo(2L);
        assertThat(captor.getValue().getRole()).isEqualTo(ProjectMemberRole.OWNER);
    }

    @Test
    @DisplayName("create → 존재하지 않는 워크스페이스면 EntityNotFoundException")
    void create_missingWorkspace_throws() {
        given(workspaceRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> projectService.create(1L, 2L,
            new ProjectDto.CreateRequest("P", "PK", null, null)))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("create → 비멤버면 WorkspaceAccessDeniedException")
    void create_nonMember_denied() {
        given(workspaceRepository.existsById(1L)).willReturn(true);
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(1L, 99L)).willReturn(false);

        assertThatThrownBy(() -> projectService.create(1L, 99L,
            new ProjectDto.CreateRequest("P", "PK", null, null)))
            .isInstanceOf(WorkspaceAccessDeniedException.class);
    }

    @Test
    @DisplayName("create → workspace 내 key 중복이면 IllegalStateException")
    void create_duplicateKey_throws() {
        given(workspaceRepository.existsById(1L)).willReturn(true);
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(1L, 2L)).willReturn(true);
        given(projectRepository.existsByWorkspaceNoAndKey(1L, "TF")).willReturn(true);

        assertThatThrownBy(() -> projectService.create(1L, 2L,
            new ProjectDto.CreateRequest("P", "TF", null, null)))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("listInWorkspace → 멤버에게 프로젝트 + memberCount 반환")
    void listInWorkspace_success() {
        Project p1 = injectNo(Project.create(1L, "A", "AA", null, null), 10L);
        Project p2 = injectNo(Project.create(1L, "B", "BB", null, null), 11L);
        given(workspaceRepository.existsById(1L)).willReturn(true);
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(1L, 2L)).willReturn(true);
        given(projectRepository.findAllByWorkspaceNoOrderByCreateDateDesc(1L)).willReturn(List.of(p1, p2));
        given(projectMemberRepository.countByProjectNo(10L)).willReturn(1L);
        given(projectMemberRepository.countByProjectNo(11L)).willReturn(2L);

        List<ProjectDto.SummaryResponse> list = projectService.listInWorkspace(1L, 2L);

        assertThat(list).hasSize(2);
        assertThat(list.get(0).memberCount()).isEqualTo(1L);
        assertThat(list.get(1).memberCount()).isEqualTo(2L);
    }

    @Test
    @DisplayName("getDetail → 프로젝트 없으면 EntityNotFoundException")
    void getDetail_missing_throws() {
        given(projectRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> projectService.getDetail(99L, 2L))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("getDetail → 워크스페이스 비멤버는 WorkspaceAccessDeniedException")
    void getDetail_nonMember_denied() {
        Project p = injectNo(Project.create(1L, "A", "AA", null, null), 10L);
        given(projectRepository.findById(10L)).willReturn(Optional.of(p));
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(1L, 99L)).willReturn(false);

        assertThatThrownBy(() -> projectService.getDetail(10L, 99L))
            .isInstanceOf(WorkspaceAccessDeniedException.class);
    }

    // ---------- helpers ----------

    private static Project injectNo(Project p, long no) {
        try {
            Field f = Project.class.getDeclaredField("no");
            f.setAccessible(true);
            f.set(p, no);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return p;
    }
}

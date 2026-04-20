package com.dookia.teamflow.issue.service;

import com.dookia.teamflow.exception.EntityNotFoundException;
import com.dookia.teamflow.issue.dto.IssueDto;
import com.dookia.teamflow.issue.entity.Issue;
import com.dookia.teamflow.issue.entity.IssuePriority;
import com.dookia.teamflow.issue.entity.IssueStatus;
import com.dookia.teamflow.issue.repository.IssueRepository;
import com.dookia.teamflow.project.entity.Project;
import com.dookia.teamflow.project.repository.ProjectRepository;
import com.dookia.teamflow.workspace.exception.WorkspaceAccessDeniedException;
import com.dookia.teamflow.workspace.repository.WorkspaceMemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IssueServiceTest {

    @Mock IssueRepository issueRepository;
    @Mock ProjectRepository projectRepository;
    @Mock WorkspaceMemberRepository workspaceMemberRepository;

    @InjectMocks IssueService issueService;

    @Test
    @DisplayName("create → ticket_counter 증가 + issueKey 조립(TF-1) + Issue 저장")
    void create_success_assemblesIssueKey() {
        Project project = injectProjectNo(
            Project.create(10L, "TeamFlow", "TF", null, null), 50L);
        given(projectRepository.findById(50L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(10L, 2L)).willReturn(true);
        given(issueRepository.save(any(Issue.class)))
            .willAnswer(inv -> injectIssueNo(inv.getArgument(0, Issue.class), 100L));

        IssueDto.Response res = issueService.create(50L, 2L, new IssueDto.CreateRequest(
            "로그인 화면 구현", "desc", null, IssuePriority.HIGH, null, LocalDate.of(2026, 4, 25)));

        assertThat(res.no()).isEqualTo(100L);
        assertThat(res.issueKey()).isEqualTo("TF-1");
        assertThat(res.status()).isEqualTo(IssueStatus.BACKLOG);
        assertThat(res.priority()).isEqualTo(IssuePriority.HIGH);
        assertThat(project.getTicketCounter()).isEqualTo(1);

        ArgumentCaptor<Issue> captor = ArgumentCaptor.forClass(Issue.class);
        verify(issueRepository).save(captor.capture());
        assertThat(captor.getValue().getIssueKey()).isEqualTo("TF-1");
        assertThat(captor.getValue().getProjectNo()).isEqualTo(50L);
    }

    @Test
    @DisplayName("create → 연속 생성 시 TF-1, TF-2 순차 발급")
    void create_sequentialIssueKey() {
        Project project = injectProjectNo(
            Project.create(10L, "TeamFlow", "TF", null, null), 50L);
        given(projectRepository.findById(50L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(10L, 2L)).willReturn(true);
        given(issueRepository.save(any(Issue.class)))
            .willAnswer(inv -> inv.getArgument(0, Issue.class));

        IssueDto.Response first = issueService.create(50L, 2L,
            new IssueDto.CreateRequest("A", null, null, null, null, null));
        IssueDto.Response second = issueService.create(50L, 2L,
            new IssueDto.CreateRequest("B", null, null, null, null, null));

        assertThat(first.issueKey()).isEqualTo("TF-1");
        assertThat(second.issueKey()).isEqualTo("TF-2");
    }

    @Test
    @DisplayName("create → 프로젝트 없으면 EntityNotFoundException")
    void create_missingProject_throws() {
        given(projectRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.create(99L, 2L,
            new IssueDto.CreateRequest("A", null, null, null, null, null)))
            .isInstanceOf(EntityNotFoundException.class);

        verify(issueRepository, never()).save(any());
    }

    @Test
    @DisplayName("create → 워크스페이스 비멤버는 WorkspaceAccessDeniedException")
    void create_nonMember_denied() {
        Project project = injectProjectNo(Project.create(10L, "P", "TF", null, null), 50L);
        given(projectRepository.findById(50L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(10L, 99L)).willReturn(false);

        assertThatThrownBy(() -> issueService.create(50L, 99L,
            new IssueDto.CreateRequest("A", null, null, null, null, null)))
            .isInstanceOf(WorkspaceAccessDeniedException.class);

        verify(issueRepository, never()).save(any());
    }

    @Test
    @DisplayName("listByProject → position 오름차순으로 활성 이슈만 반환")
    void listByProject_returnsActiveOnly() {
        Project project = injectProjectNo(Project.create(10L, "P", "TF", null, null), 50L);
        Issue a = injectIssueNo(Issue.create(50L, "TF-1", "A", null, null, null, null, null, 0), 101L);
        Issue b = injectIssueNo(Issue.create(50L, "TF-2", "B", null, null, null, null, null, 1), 102L);
        given(projectRepository.findById(50L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(10L, 2L)).willReturn(true);
        given(issueRepository.findAllByProjectNoAndDeleteDateIsNullOrderByPositionAsc(50L))
            .willReturn(List.of(a, b));

        List<IssueDto.Response> list = issueService.listByProject(50L, 2L);

        assertThat(list).extracting(IssueDto.Response::issueKey).containsExactly("TF-1", "TF-2");
    }

    @Test
    @DisplayName("get → soft delete 된 이슈는 EntityNotFoundException")
    void get_softDeleted_throws() {
        given(issueRepository.findByNoAndDeleteDateIsNull(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.get(99L, 2L))
            .isInstanceOf(EntityNotFoundException.class);
    }

    @Test
    @DisplayName("update → 부분 수정: null 필드는 그대로, 지정된 필드만 반영")
    void update_partialFields() {
        Project project = injectProjectNo(Project.create(10L, "P", "TF", null, null), 50L);
        Issue issue = injectIssueNo(Issue.create(
            50L, "TF-1", "원래제목", "원래설명",
            IssueStatus.BACKLOG, IssuePriority.MEDIUM, null, null, 0), 101L);
        given(issueRepository.findByNoAndDeleteDateIsNull(101L)).willReturn(Optional.of(issue));
        given(projectRepository.findById(50L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(10L, 2L)).willReturn(true);

        IssueDto.Response res = issueService.update(101L, 2L, new IssueDto.UpdateRequest(
            null, null, IssueStatus.IN_PROGRESS, IssuePriority.HIGH, 7L, null));

        assertThat(res.status()).isEqualTo(IssueStatus.IN_PROGRESS);
        assertThat(res.priority()).isEqualTo(IssuePriority.HIGH);
        assertThat(res.assigneeNo()).isEqualTo(7L);
        assertThat(res.title()).isEqualTo("원래제목");
    }

    @Test
    @DisplayName("delete → softDelete 호출 + deleteDate 세팅")
    void delete_setsSoftDeleteMarker() {
        Project project = injectProjectNo(Project.create(10L, "P", "TF", null, null), 50L);
        Issue issue = injectIssueNo(Issue.create(
            50L, "TF-1", "A", null, null, null, null, null, 0), 101L);
        given(issueRepository.findByNoAndDeleteDateIsNull(101L)).willReturn(Optional.of(issue));
        given(projectRepository.findById(50L)).willReturn(Optional.of(project));
        given(workspaceMemberRepository.existsByWorkspaceNoAndUserNo(10L, 2L)).willReturn(true);

        issueService.delete(101L, 2L);

        assertThat(issue.isDeleted()).isTrue();
        assertThat(issue.getDeleteDate()).isNotNull();
    }

    @Test
    @DisplayName("delete → 이슈 없으면 EntityNotFoundException")
    void delete_missing_throws() {
        given(issueRepository.findByNoAndDeleteDateIsNull(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> issueService.delete(99L, 2L))
            .isInstanceOf(EntityNotFoundException.class);
    }

    // ---------- helpers ----------

    private static Project injectProjectNo(Project p, long no) {
        try {
            Field f = Project.class.getDeclaredField("no");
            f.setAccessible(true);
            f.set(p, no);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return p;
    }

    private static Issue injectIssueNo(Issue i, long no) {
        try {
            Field f = Issue.class.getDeclaredField("no");
            f.setAccessible(true);
            f.set(i, no);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return i;
    }
}

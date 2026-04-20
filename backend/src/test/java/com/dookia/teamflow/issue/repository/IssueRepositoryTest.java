package com.dookia.teamflow.issue.repository;

import com.dookia.teamflow.issue.entity.Issue;
import com.dookia.teamflow.issue.entity.IssuePriority;
import com.dookia.teamflow.issue.entity.IssueStatus;
import com.dookia.teamflow.project.entity.Project;
import com.dookia.teamflow.project.repository.ProjectRepository;
import com.dookia.teamflow.user.entity.User;
import com.dookia.teamflow.user.entity.UserProvider;
import com.dookia.teamflow.user.repository.UserRepository;
import com.dookia.teamflow.workspace.entity.Workspace;
import com.dookia.teamflow.workspace.repository.WorkspaceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
class IssueRepositoryTest {

    @Autowired IssueRepository issueRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired WorkspaceRepository workspaceRepository;
    @Autowired UserRepository userRepository;
    @Autowired TestEntityManager em;

    private Project persistProject(String key) {
        Workspace ws = workspaceRepository.save(Workspace.create("W-" + key));
        return projectRepository.save(Project.create(ws.getNo(), "P-" + key, key, null, null));
    }

    @Test
    @DisplayName("Issue.create — status=BACKLOG, priority=MEDIUM, position=0, delete_date=NULL 기본값")
    void save_defaults() {
        Project project = persistProject("TF");

        Issue saved = issueRepository.save(
            Issue.create(project.getNo(), "TF-1", "로그인 화면 구현", null, null, null, null, null, 0));
        em.flush();
        em.clear();

        Issue found = issueRepository.findById(saved.getNo()).orElseThrow();
        assertThat(found.getProjectNo()).isEqualTo(project.getNo());
        assertThat(found.getIssueKey()).isEqualTo("TF-1");
        assertThat(found.getTitle()).isEqualTo("로그인 화면 구현");
        assertThat(found.getStatus()).isEqualTo(IssueStatus.BACKLOG);
        assertThat(found.getPriority()).isEqualTo(IssuePriority.MEDIUM);
        assertThat(found.getPosition()).isZero();
        assertThat(found.getAssigneeNo()).isNull();
        assertThat(found.getDueDate()).isNull();
        assertThat(found.getDeleteDate()).isNull();
        assertThat(found.getCreateDate()).isNotNull();
        assertThat(found.getUpdateDate()).isNotNull();
    }

    @Test
    @DisplayName("Issue.create — 담당자/기한/우선순위/상태 지정 시 그대로 저장된다")
    void save_withAllFields() {
        Project project = persistProject("AA");
        User assignee = userRepository.save(
            User.createFromOAuth(UserProvider.GOOGLE, "sub-1", "a@x.com", "Alice", null));

        Issue saved = issueRepository.save(Issue.create(
            project.getNo(), "AA-1", "긴급 핫픽스", "서버 500 에러",
            IssueStatus.IN_PROGRESS, IssuePriority.CRITICAL, assignee.getNo(),
            LocalDate.of(2026, 4, 25), 3));
        em.flush();
        em.clear();

        Issue found = issueRepository.findById(saved.getNo()).orElseThrow();
        assertThat(found.getDescription()).isEqualTo("서버 500 에러");
        assertThat(found.getStatus()).isEqualTo(IssueStatus.IN_PROGRESS);
        assertThat(found.getPriority()).isEqualTo(IssuePriority.CRITICAL);
        assertThat(found.getAssigneeNo()).isEqualTo(assignee.getNo());
        assertThat(found.getDueDate()).isEqualTo(LocalDate.of(2026, 4, 25));
        assertThat(found.getPosition()).isEqualTo(3);
    }

    @Test
    @DisplayName("UNIQUE(project_no, issue_key) — 같은 프로젝트 내 key 중복 저장 불가")
    void unique_projectNoAndIssueKey() {
        Project project = persistProject("TF");
        issueRepository.save(Issue.create(project.getNo(), "TF-1", "A", null, null, null, null, null, 0));
        em.flush();

        assertThatThrownBy(() -> {
            issueRepository.save(Issue.create(project.getNo(), "TF-1", "B", null, null, null, null, null, 0));
            em.flush();
        }).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("findAllByProjectNoAndDeleteDateIsNullOrderByPositionAsc — 활성 이슈만 position 오름차순")
    void findActiveInProject_orderedByPosition() {
        Project project = persistProject("TF");

        Issue second = issueRepository.save(
            Issue.create(project.getNo(), "TF-1", "두번째", null, null, null, null, null, 2));
        Issue first = issueRepository.save(
            Issue.create(project.getNo(), "TF-2", "첫번째", null, null, null, null, null, 0));
        Issue deleted = issueRepository.save(
            Issue.create(project.getNo(), "TF-3", "지울거", null, null, null, null, null, 1));
        deleted.softDelete();
        em.flush();
        em.clear();

        List<Issue> list = issueRepository.findAllByProjectNoAndDeleteDateIsNullOrderByPositionAsc(project.getNo());
        assertThat(list)
            .extracting(Issue::getIssueKey)
            .containsExactly(first.getIssueKey(), second.getIssueKey());
    }

    @Test
    @DisplayName("findByNoAndDeleteDateIsNull — soft delete 된 이슈는 조회되지 않는다")
    void findActiveById_excludesSoftDeleted() {
        Project project = persistProject("TF");
        Issue issue = issueRepository.save(
            Issue.create(project.getNo(), "TF-1", "삭제될 이슈", null, null, null, null, null, 0));
        em.flush();

        assertThat(issueRepository.findByNoAndDeleteDateIsNull(issue.getNo())).isPresent();

        issue.softDelete();
        em.flush();
        em.clear();

        assertThat(issueRepository.findByNoAndDeleteDateIsNull(issue.getNo())).isEmpty();
        assertThat(issueRepository.findById(issue.getNo())).isPresent(); // 하드 조회는 남아 있음
    }

    @Test
    @DisplayName("existsByProjectNoAndIssueKey — 프로젝트 내 issueKey 사용 여부")
    void existsByProjectNoAndIssueKey() {
        Project project = persistProject("TF");
        issueRepository.save(Issue.create(project.getNo(), "TF-1", "A", null, null, null, null, null, 0));
        em.flush();
        em.clear();

        assertThat(issueRepository.existsByProjectNoAndIssueKey(project.getNo(), "TF-1")).isTrue();
        assertThat(issueRepository.existsByProjectNoAndIssueKey(project.getNo(), "TF-2")).isFalse();
    }
}

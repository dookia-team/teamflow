package com.dookia.teamflow.issue.service;

import com.dookia.teamflow.exception.EntityNotFoundException;
import com.dookia.teamflow.issue.dto.IssueDto;
import com.dookia.teamflow.issue.entity.Issue;
import com.dookia.teamflow.issue.repository.IssueRepository;
import com.dookia.teamflow.project.entity.Project;
import com.dookia.teamflow.project.repository.ProjectRepository;
import com.dookia.teamflow.workspace.exception.WorkspaceAccessDeniedException;
import com.dookia.teamflow.workspace.repository.WorkspaceMemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 이슈 도메인 비즈니스 로직. Sprint 2 §2.2 + HANDOFF.md §2 를 구현한다.
 * 삭제는 delete_date 를 채우는 soft delete (RISK-IMPACT 2026-04-20 결정).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class IssueService {

    private final IssueRepository issueRepository;
    private final ProjectRepository projectRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public IssueDto.Response create(Long projectNo, Long userNo, IssueDto.CreateRequest request) {
        Project project = projectRepository.findById(projectNo)
            .orElseThrow(() -> new EntityNotFoundException("Project", projectNo));
        requireWorkspaceMember(project.getWorkspaceNo(), userNo);

        int ticketNumber = project.nextTicketNumber();
        String issueKey = project.getKey() + "-" + ticketNumber;

        Issue saved = issueRepository.save(Issue.create(
            projectNo,
            issueKey,
            request.title(),
            request.description(),
            request.status(),
            request.priority(),
            request.assigneeNo(),
            request.dueDate(),
            0
        ));
        return IssueDto.Response.from(saved);
    }

    @Transactional(readOnly = true)
    public List<IssueDto.Response> listByProject(Long projectNo, Long userNo) {
        Project project = projectRepository.findById(projectNo)
            .orElseThrow(() -> new EntityNotFoundException("Project", projectNo));
        requireWorkspaceMember(project.getWorkspaceNo(), userNo);

        return issueRepository.findAllByProjectNoAndDeleteDateIsNullOrderByPositionAsc(projectNo).stream()
            .map(IssueDto.Response::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public IssueDto.Response get(Long issueNo, Long userNo) {
        Issue issue = loadActive(issueNo);
        requireProjectMember(issue.getProjectNo(), userNo);
        return IssueDto.Response.from(issue);
    }

    public IssueDto.Response update(Long issueNo, Long userNo, IssueDto.UpdateRequest request) {
        Issue issue = loadActive(issueNo);
        requireProjectMember(issue.getProjectNo(), userNo);

        if (request.title() != null || request.description() != null || request.dueDate() != null) {
            issue.updateDetails(request.title(), request.description(), request.dueDate());
        }
        if (request.status() != null) {
            issue.changeStatus(request.status());
        }
        if (request.priority() != null) {
            issue.changePriority(request.priority());
        }
        if (request.assigneeNo() != null) {
            issue.assignTo(request.assigneeNo());
        }
        return IssueDto.Response.from(issue);
    }

    public void delete(Long issueNo, Long userNo) {
        Issue issue = loadActive(issueNo);
        requireProjectMember(issue.getProjectNo(), userNo);
        issue.softDelete();
    }

    // --- helpers ------------------------------------------------------------

    private Issue loadActive(Long issueNo) {
        return issueRepository.findByNoAndDeleteDateIsNull(issueNo)
            .orElseThrow(() -> new EntityNotFoundException("Issue", issueNo));
    }

    private void requireProjectMember(Long projectNo, Long userNo) {
        Project project = projectRepository.findById(projectNo)
            .orElseThrow(() -> new EntityNotFoundException("Project", projectNo));
        requireWorkspaceMember(project.getWorkspaceNo(), userNo);
    }

    private void requireWorkspaceMember(Long workspaceNo, Long userNo) {
        if (!workspaceMemberRepository.existsByWorkspaceNoAndUserNo(workspaceNo, userNo)) {
            throw new WorkspaceAccessDeniedException("워크스페이스 멤버가 아닙니다.");
        }
    }
}

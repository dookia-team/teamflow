package com.dookia.teamflow.project.service;

import com.dookia.teamflow.exception.EntityNotFoundException;
import com.dookia.teamflow.project.dto.ProjectDto;
import com.dookia.teamflow.project.entity.Project;
import com.dookia.teamflow.project.entity.ProjectMember;
import com.dookia.teamflow.project.entity.ProjectMemberRole;
import com.dookia.teamflow.project.repository.ProjectMemberRepository;
import com.dookia.teamflow.project.repository.ProjectRepository;
import com.dookia.teamflow.workspace.exception.WorkspaceAccessDeniedException;
import com.dookia.teamflow.workspace.repository.WorkspaceMemberRepository;
import com.dookia.teamflow.workspace.repository.WorkspaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * PROJECT 도메인 비즈니스 로직. ERD v0.1 §3 + HANDOFF.md §2 를 구현한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final WorkspaceRepository workspaceRepository;
    private final WorkspaceMemberRepository workspaceMemberRepository;

    public ProjectDto.Response create(Long workspaceNo, Long creatorUserNo, ProjectDto.CreateRequest request) {
        if (!workspaceRepository.existsById(workspaceNo)) {
            throw new EntityNotFoundException("Workspace", workspaceNo);
        }
        requireWorkspaceMember(workspaceNo, creatorUserNo);
        if (projectRepository.existsByWorkspaceNoAndKey(workspaceNo, request.key())) {
            throw new IllegalStateException("이미 사용 중인 프로젝트 key 입니다: " + request.key());
        }

        Project saved = projectRepository.save(Project.create(
            workspaceNo, request.name(), request.key(), request.description(), request.visibility()
        ));
        projectMemberRepository.save(ProjectMember.of(saved.getNo(), creatorUserNo, ProjectMemberRole.OWNER));
        return ProjectDto.Response.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ProjectDto.SummaryResponse> listInWorkspace(Long workspaceNo, Long userNo) {
        if (!workspaceRepository.existsById(workspaceNo)) {
            throw new EntityNotFoundException("Workspace", workspaceNo);
        }
        requireWorkspaceMember(workspaceNo, userNo);

        return projectRepository.findAllByWorkspaceNoOrderByCreateDateDesc(workspaceNo).stream()
            .map(p -> ProjectDto.SummaryResponse.of(p, projectMemberRepository.countByProjectNo(p.getNo())))
            .toList();
    }

    @Transactional(readOnly = true)
    public ProjectDto.Response getDetail(Long projectNo, Long userNo) {
        Project project = projectRepository.findById(projectNo)
            .orElseThrow(() -> new EntityNotFoundException("Project", projectNo));
        requireWorkspaceMember(project.getWorkspaceNo(), userNo);
        return ProjectDto.Response.from(project);
    }

    // --- helpers ------------------------------------------------------------

    private void requireWorkspaceMember(Long workspaceNo, Long userNo) {
        if (!workspaceMemberRepository.existsByWorkspaceNoAndUserNo(workspaceNo, userNo)) {
            throw new WorkspaceAccessDeniedException("워크스페이스 멤버가 아닙니다.");
        }
    }
}

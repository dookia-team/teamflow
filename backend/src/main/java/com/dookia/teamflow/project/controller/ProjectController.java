package com.dookia.teamflow.project.controller;

import com.dookia.teamflow.dto.ApiResponse;
import com.dookia.teamflow.project.dto.ProjectDto;
import com.dookia.teamflow.project.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 프로젝트 엔드포인트. HANDOFF.md §2 를 따른다.
 *  - POST /api/workspaces/{wsNo}/projects
 *  - GET  /api/workspaces/{wsNo}/projects
 *  - GET  /api/projects/{no}
 */
@Tag(name = "Project", description = "프로젝트 CRUD")
@RestController
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 생성")
    @PostMapping("/api/workspaces/{workspaceNo}/projects")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ProjectDto.Response> create(@AuthenticationPrincipal Long userNo, @PathVariable Long workspaceNo, @Valid @RequestBody ProjectDto.CreateRequest request) {
        return ApiResponse.success(projectService.create(workspaceNo, userNo, request));
    }

    @Operation(summary = "워크스페이스 하위 프로젝트 목록")
    @GetMapping("/api/workspaces/{workspaceNo}/projects")
    public ApiResponse<List<ProjectDto.SummaryResponse>> list(@AuthenticationPrincipal Long userNo, @PathVariable Long workspaceNo) {
        return ApiResponse.success(projectService.listInWorkspace(workspaceNo, userNo));
    }

    @Operation(summary = "프로젝트 상세")
    @GetMapping("/api/projects/{projectNo}")
    public ApiResponse<ProjectDto.Response> getDetail(@AuthenticationPrincipal Long userNo, @PathVariable Long projectNo) {
        return ApiResponse.success(projectService.getDetail(projectNo, userNo));
    }
}

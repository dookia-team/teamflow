package com.dookia.teamflow.issue.controller;

import com.dookia.teamflow.dto.ApiResponse;
import com.dookia.teamflow.issue.dto.IssueDto;
import com.dookia.teamflow.issue.service.IssueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 이슈 엔드포인트. Sprint 2 HANDOFF.md §2 를 따른다.
 *  - POST   /api/projects/{projectNo}/issues
 *  - GET    /api/projects/{projectNo}/issues
 *  - GET    /api/issues/{issueNo}
 *  - PATCH  /api/issues/{issueNo}
 *  - DELETE /api/issues/{issueNo}
 */
@Tag(name = "Issue", description = "이슈 CRUD (칸반 보드)")
@RestController
@RequiredArgsConstructor
public class IssueController {

    private final IssueService issueService;

    @Operation(summary = "이슈 생성")
    @PostMapping("/api/projects/{projectNo}/issues")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<IssueDto.Response> create(@AuthenticationPrincipal Long userNo, @PathVariable Long projectNo, @Valid @RequestBody IssueDto.CreateRequest request) {
        return ApiResponse.success(issueService.create(projectNo, userNo, request));
    }

    @Operation(summary = "프로젝트 내 이슈 목록 (활성 이슈만, position 오름차순)")
    @GetMapping("/api/projects/{projectNo}/issues")
    public ApiResponse<List<IssueDto.Response>> list(@AuthenticationPrincipal Long userNo, @PathVariable Long projectNo) {
        return ApiResponse.success(issueService.listByProject(projectNo, userNo));
    }

    @Operation(summary = "이슈 상세")
    @GetMapping("/api/issues/{issueNo}")
    public ApiResponse<IssueDto.Response> getDetail(@AuthenticationPrincipal Long userNo, @PathVariable Long issueNo) {
        return ApiResponse.success(issueService.get(issueNo, userNo));
    }

    @Operation(summary = "이슈 수정 (부분)")
    @PatchMapping("/api/issues/{issueNo}")
    public ApiResponse<IssueDto.Response> update(@AuthenticationPrincipal Long userNo, @PathVariable Long issueNo, @Valid @RequestBody IssueDto.UpdateRequest request) {
        return ApiResponse.success(issueService.update(issueNo, userNo, request));
    }

    @Operation(summary = "이슈 삭제 (soft delete)")
    @DeleteMapping("/api/issues/{issueNo}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Long userNo, @PathVariable Long issueNo) {
        issueService.delete(issueNo, userNo);
    }

    @Operation(summary = "이슈 상태 변경 (드래그 앤 드롭: 컬럼 간 이동)")
    @PatchMapping("/api/issues/{issueNo}/status")
    public ApiResponse<IssueDto.StatusResponse> changeStatus(@AuthenticationPrincipal Long userNo, @PathVariable Long issueNo, @Valid @RequestBody IssueDto.StatusChangeRequest request) {
        return ApiResponse.success(issueService.changeStatus(issueNo, userNo, request.status()));
    }

    @Operation(summary = "이슈 순서 변경 (같은 컬럼 내)")
    @PatchMapping("/api/issues/{issueNo}/position")
    public ApiResponse<IssueDto.PositionResponse> changePosition(@AuthenticationPrincipal Long userNo, @PathVariable Long issueNo, @Valid @RequestBody IssueDto.PositionChangeRequest request) {
        return ApiResponse.success(issueService.changePosition(issueNo, userNo, request.position()));
    }
}

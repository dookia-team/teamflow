package com.dookia.teamflow.workspace.controller;

import com.dookia.teamflow.dto.ApiResponse;
import com.dookia.teamflow.workspace.dto.WorkspaceDto;
import com.dookia.teamflow.workspace.service.WorkspaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 워크스페이스 엔드포인트. HANDOFF.md §2 를 따른다.
 */
@Tag(name = "Workspace", description = "워크스페이스 CRUD · 멤버 초대")
@RestController
@RequestMapping("/api/workspaces")
@RequiredArgsConstructor
public class WorkspaceController {

    private final WorkspaceService workspaceService;

    @Operation(summary = "워크스페이스 생성")
    @PostMapping
    public ResponseEntity<ApiResponse<WorkspaceDto.Response>> create(
        @AuthenticationPrincipal Long userNo,
        @Valid @RequestBody WorkspaceDto.CreateRequest request
    ) {
        WorkspaceDto.Response created = workspaceService.create(userNo, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(created));
    }

    @Operation(summary = "내 워크스페이스 목록")
    @GetMapping
    public ResponseEntity<ApiResponse<List<WorkspaceDto.SummaryResponse>>> list(
        @AuthenticationPrincipal Long userNo
    ) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.listForUser(userNo)));
    }

    @Operation(summary = "워크스페이스 상세 (멤버 포함)")
    @GetMapping("/{workspaceNo}")
    public ResponseEntity<ApiResponse<WorkspaceDto.DetailResponse>> getDetail(
        @AuthenticationPrincipal Long userNo,
        @PathVariable Long workspaceNo
    ) {
        return ResponseEntity.ok(ApiResponse.success(workspaceService.getDetail(workspaceNo, userNo)));
    }

    @Operation(summary = "워크스페이스 멤버 초대")
    @PostMapping("/{workspaceNo}/invite")
    public ResponseEntity<ApiResponse<WorkspaceDto.InvitationResponse>> invite(
        @AuthenticationPrincipal Long userNo,
        @PathVariable Long workspaceNo,
        @Valid @RequestBody WorkspaceDto.InviteRequest request
    ) {
        WorkspaceDto.InvitationResponse invitation = workspaceService.invite(workspaceNo, userNo, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(invitation));
    }
}

package com.dookia.teamflow.issue.controller;

import com.dookia.teamflow.auth.service.JwtService;
import com.dookia.teamflow.exception.EntityNotFoundException;
import com.dookia.teamflow.issue.dto.IssueDto;
import com.dookia.teamflow.issue.entity.IssuePriority;
import com.dookia.teamflow.issue.entity.IssueStatus;
import com.dookia.teamflow.issue.service.IssueService;
import com.dookia.teamflow.workspace.exception.WorkspaceAccessDeniedException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = IssueController.class)
@AutoConfigureMockMvc(addFilters = false)
class IssueControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean IssueService issueService;
    @MockBean JwtService jwtService;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void authenticatedAs(long userNo) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(userNo, null, Collections.emptyList()));
    }

    private IssueDto.Response sampleResponse(Long no, String key) {
        return new IssueDto.Response(
            no, 50L, key, "로그인 화면 구현", "desc",
            IssueStatus.BACKLOG, IssuePriority.HIGH, null, 0, LocalDate.of(2026, 4, 25));
    }

    @Test
    @DisplayName("POST /api/projects/{projectNo}/issues → 201 + Response")
    void create_returns201() throws Exception {
        authenticatedAs(2L);
        given(issueService.create(eq(50L), eq(2L), any(IssueDto.CreateRequest.class)))
            .willReturn(sampleResponse(101L, "TF-1"));

        mockMvc.perform(post("/api/projects/50/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"로그인 화면 구현\",\"description\":\"desc\",\"priority\":\"HIGH\",\"dueDate\":\"2026-04-25\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.no", equalTo(101)))
            .andExpect(jsonPath("$.data.issueKey", equalTo("TF-1")))
            .andExpect(jsonPath("$.data.status", equalTo("BACKLOG")));
    }

    @Test
    @DisplayName("POST create → 제목 1자는 400 (Size 검증)")
    void create_invalidTitle_returns400() throws Exception {
        authenticatedAs(2L);
        mockMvc.perform(post("/api/projects/50/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"A\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST create → 제목 누락은 400 (NotBlank)")
    void create_blankTitle_returns400() throws Exception {
        authenticatedAs(2L);
        mockMvc.perform(post("/api/projects/50/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST create → 프로젝트 없음(404)")
    void create_projectNotFound_returns404() throws Exception {
        authenticatedAs(2L);
        given(issueService.create(eq(99L), eq(2L), any(IssueDto.CreateRequest.class)))
            .willThrow(new EntityNotFoundException("Project", 99L));

        mockMvc.perform(post("/api/projects/99/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"제목있음\"}"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST create → 비멤버(403)")
    void create_forbidden_returns403() throws Exception {
        authenticatedAs(99L);
        given(issueService.create(eq(50L), eq(99L), any(IssueDto.CreateRequest.class)))
            .willThrow(new WorkspaceAccessDeniedException("워크스페이스 멤버가 아닙니다."));

        mockMvc.perform(post("/api/projects/50/issues")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"제목있음\"}"))
            .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /api/projects/{projectNo}/issues → 200 + 활성 이슈 목록")
    void list_returns200() throws Exception {
        authenticatedAs(2L);
        given(issueService.listByProject(50L, 2L))
            .willReturn(List.of(sampleResponse(101L, "TF-1"), sampleResponse(102L, "TF-2")));

        mockMvc.perform(get("/api/projects/50/issues"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].issueKey", equalTo("TF-1")))
            .andExpect(jsonPath("$.data[1].issueKey", equalTo("TF-2")));
    }

    @Test
    @DisplayName("GET /api/issues/{issueNo} → 200 + 상세")
    void get_returns200() throws Exception {
        authenticatedAs(2L);
        given(issueService.get(101L, 2L)).willReturn(sampleResponse(101L, "TF-1"));

        mockMvc.perform(get("/api/issues/101"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.no", equalTo(101)))
            .andExpect(jsonPath("$.data.priority", equalTo("HIGH")));
    }

    @Test
    @DisplayName("GET /api/issues/{issueNo} → 없음 404")
    void get_notFound_returns404() throws Exception {
        authenticatedAs(2L);
        given(issueService.get(99L, 2L)).willThrow(new EntityNotFoundException("Issue", 99L));

        mockMvc.perform(get("/api/issues/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("PATCH /api/issues/{issueNo} → 200 + 부분 수정 Response")
    void update_returns200() throws Exception {
        authenticatedAs(2L);
        IssueDto.Response updated = new IssueDto.Response(
            101L, 50L, "TF-1", "로그인 화면 구현", "desc",
            IssueStatus.IN_PROGRESS, IssuePriority.CRITICAL, 7L, 0, null);
        given(issueService.update(eq(101L), eq(2L), any(IssueDto.UpdateRequest.class)))
            .willReturn(updated);

        mockMvc.perform(patch("/api/issues/101")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\":\"IN_PROGRESS\",\"priority\":\"CRITICAL\",\"assigneeNo\":7}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.status", equalTo("IN_PROGRESS")))
            .andExpect(jsonPath("$.data.assigneeNo", equalTo(7)));
    }

    @Test
    @DisplayName("DELETE /api/issues/{issueNo} → 204")
    void delete_returns204() throws Exception {
        authenticatedAs(2L);

        mockMvc.perform(delete("/api/issues/101"))
            .andExpect(status().isNoContent());

        verify(issueService).delete(101L, 2L);
    }

    @Test
    @DisplayName("DELETE /api/issues/{issueNo} → 없음 404")
    void delete_notFound_returns404() throws Exception {
        authenticatedAs(2L);
        willThrow(new EntityNotFoundException("Issue", 99L))
            .given(issueService).delete(99L, 2L);

        mockMvc.perform(delete("/api/issues/99"))
            .andExpect(status().isNotFound());
    }
}

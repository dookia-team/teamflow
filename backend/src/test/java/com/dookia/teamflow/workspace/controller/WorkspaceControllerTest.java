package com.dookia.teamflow.workspace.controller;

import com.dookia.teamflow.auth.service.JwtService;
import com.dookia.teamflow.workspace.dto.WorkspaceDto;
import com.dookia.teamflow.workspace.entity.WorkspaceInvitationStatus;
import com.dookia.teamflow.workspace.entity.WorkspaceMemberRole;
import com.dookia.teamflow.workspace.service.WorkspaceService;
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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WorkspaceController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkspaceControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;

    @MockBean WorkspaceService workspaceService;
    @MockBean JwtService jwtService;

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void authenticatedAs(long userNo) {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(userNo, null, Collections.emptyList()));
    }

    @Test
    @DisplayName("POST /api/workspaces → 201 + Response(role=OWNER)")
    void create_returns201() throws Exception {
        authenticatedAs(1L);
        given(workspaceService.create(eq(1L), any(WorkspaceDto.CreateRequest.class)))
            .willReturn(new WorkspaceDto.Response(10L, "Alpha", "alpha-xxxx", WorkspaceMemberRole.OWNER, 1L));

        mockMvc.perform(post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Alpha\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.success", equalTo(true)))
            .andExpect(jsonPath("$.data.no", equalTo(10)))
            .andExpect(jsonPath("$.data.role", equalTo("OWNER")));
    }

    @Test
    @DisplayName("POST /api/workspaces with blank name → 400")
    void create_blankName_returns400() throws Exception {
        authenticatedAs(1L);
        mockMvc.perform(post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/workspaces → 200 + 리스트")
    void list_returns200() throws Exception {
        authenticatedAs(1L);
        given(workspaceService.listForUser(1L))
            .willReturn(List.of(new WorkspaceDto.SummaryResponse(10L, "Alpha", "alpha-xxxx", 2L)));

        mockMvc.perform(get("/api/workspaces"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].no", equalTo(10)))
            .andExpect(jsonPath("$.data[0].memberCount", equalTo(2)));
    }

    @Test
    @DisplayName("GET /api/workspaces/{no} → 200 + detail")
    void getDetail_returns200() throws Exception {
        authenticatedAs(1L);
        given(workspaceService.getDetail(10L, 1L)).willReturn(
            WorkspaceDto.DetailResponse.of(workspaceStub(), List.of()));

        mockMvc.perform(get("/api/workspaces/10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.no", equalTo(10)));
    }

    @Test
    @DisplayName("POST /api/workspaces/{no}/invite → 201 + PENDING")
    void invite_returns201() throws Exception {
        authenticatedAs(1L);
        given(workspaceService.invite(eq(10L), eq(1L), any()))
            .willReturn(new WorkspaceDto.InvitationResponse(
                100L, 10L, 3L, WorkspaceMemberRole.MEMBER,
                WorkspaceInvitationStatus.PENDING, "token-xyz",
                LocalDateTime.now().plusDays(7)));

        mockMvc.perform(post("/api/workspaces/10/invite")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"inviteeUserNo\":3,\"role\":\"MEMBER\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.status", equalTo("PENDING")))
            .andExpect(jsonPath("$.data.token", equalTo("token-xyz")));
    }

    private static com.dookia.teamflow.workspace.entity.Workspace workspaceStub() {
        var ws = com.dookia.teamflow.workspace.entity.Workspace.create("Alpha");
        try {
            var f = com.dookia.teamflow.workspace.entity.Workspace.class.getDeclaredField("no");
            f.setAccessible(true);
            f.set(ws, 10L);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
        return ws;
    }
}

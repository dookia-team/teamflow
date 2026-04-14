package com.dookia.teamflow.workspace.repository;

import com.dookia.teamflow.workspace.entity.WorkspaceInvitation;
import com.dookia.teamflow.workspace.entity.WorkspaceInvitationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkspaceInvitationRepository extends JpaRepository<WorkspaceInvitation, Long> {

    Optional<WorkspaceInvitation> findByToken(String token);

    boolean existsByWorkspaceNoAndInviteeUserNoAndStatus(
        Long workspaceNo, Long inviteeUserNo, WorkspaceInvitationStatus status);
}

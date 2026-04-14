package com.dookia.teamflow.workspace.repository;

import com.dookia.teamflow.workspace.entity.WorkspaceMember;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    List<WorkspaceMember> findAllByUserNo(Long userNo);

    List<WorkspaceMember> findAllByWorkspaceNo(Long workspaceNo);

    Optional<WorkspaceMember> findByWorkspaceNoAndUserNo(Long workspaceNo, Long userNo);

    long countByWorkspaceNo(Long workspaceNo);

    boolean existsByWorkspaceNoAndUserNo(Long workspaceNo, Long userNo);
}

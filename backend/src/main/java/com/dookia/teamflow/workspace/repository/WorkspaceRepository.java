package com.dookia.teamflow.workspace.repository;

import com.dookia.teamflow.workspace.entity.Workspace;
import org.springframework.data.jpa.repository.JpaRepository;

public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {
    boolean existsBySlug(String slug);
}

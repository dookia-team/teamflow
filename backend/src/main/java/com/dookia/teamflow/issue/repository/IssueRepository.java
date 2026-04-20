package com.dookia.teamflow.issue.repository;

import com.dookia.teamflow.issue.entity.Issue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IssueRepository extends JpaRepository<Issue, Long> {

    List<Issue> findAllByProjectNoAndDeleteDateIsNullOrderByPositionAsc(Long projectNo);

    Optional<Issue> findByNoAndDeleteDateIsNull(Long no);

    boolean existsByProjectNoAndIssueKey(Long projectNo, String issueKey);
}

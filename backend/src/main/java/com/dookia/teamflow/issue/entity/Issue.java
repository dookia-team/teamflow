package com.dookia.teamflow.issue.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ISSUE 엔티티. Sprint 2 §2.2. 칸반 보드 이슈의 단일 레코드.
 * 삭제는 delete_date 를 채우는 soft delete 로 수행한다 (RISK-IMPACT 결정 2026-04-20).
 */
@Entity
@Table(
    name = "issue",
    uniqueConstraints = @UniqueConstraint(name = "uk_issue_key", columnNames = {"project_no", "issue_key"})
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Issue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "no")
    private Long no;

    @Column(name = "project_no", nullable = false)
    private Long projectNo;

    @Column(name = "issue_key", nullable = false, length = 20)
    private String issueKey;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private IssueStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private IssuePriority priority;

    @Column(name = "assignee_no")
    private Long assigneeNo;

    @Column(nullable = false)
    private int position;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "create_date", nullable = false, updatable = false)
    private LocalDateTime createDate;

    @Column(name = "update_date", nullable = false)
    private LocalDateTime updateDate;

    @Column(name = "delete_date")
    private LocalDateTime deleteDate;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createDate == null) {
            createDate = now;
        }
        if (updateDate == null) {
            updateDate = now;
        }
        if (status == null) {
            status = IssueStatus.BACKLOG;
        }
        if (priority == null) {
            priority = IssuePriority.MEDIUM;
        }
    }

    @PreUpdate
    void onUpdate() {
        updateDate = LocalDateTime.now();
    }

    public void updateDetails(String title, String description, LocalDate dueDate) {
        if (title != null) {
            this.title = title;
        }
        this.description = description;
        this.dueDate = dueDate;
    }

    public void changeStatus(IssueStatus status) {
        this.status = status;
    }

    public void changePriority(IssuePriority priority) {
        this.priority = priority;
    }

    public void assignTo(Long assigneeNo) {
        this.assigneeNo = assigneeNo;
    }

    public void moveTo(int position) {
        this.position = position;
    }

    public void softDelete() {
        this.deleteDate = LocalDateTime.now();
    }

    public boolean isDeleted() {
        return this.deleteDate != null;
    }

    public static Issue create(
        Long projectNo,
        String issueKey,
        String title,
        String description,
        IssueStatus status,
        IssuePriority priority,
        Long assigneeNo,
        LocalDate dueDate,
        int position
    ) {
        return Issue.builder()
            .projectNo(projectNo)
            .issueKey(issueKey)
            .title(title)
            .description(description)
            .status(status != null ? status : IssueStatus.BACKLOG)
            .priority(priority != null ? priority : IssuePriority.MEDIUM)
            .assigneeNo(assigneeNo)
            .dueDate(dueDate)
            .position(position)
            .build();
    }
}

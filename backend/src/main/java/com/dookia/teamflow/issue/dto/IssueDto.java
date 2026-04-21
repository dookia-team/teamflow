package com.dookia.teamflow.issue.dto;

import com.dookia.teamflow.issue.entity.Issue;
import com.dookia.teamflow.issue.entity.IssuePriority;
import com.dookia.teamflow.issue.entity.IssueStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 이슈 도메인 요청/응답 DTO. backend-conventions.md 규칙에 따라 {Domain}Dto.java 하나에 inner record 로 선언.
 */
public class IssueDto {

    private IssueDto() {
    }

    public record CreateRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Size(min = 2, max = 200, message = "제목은 2~200자여야 합니다.")
        String title,

        String description,
        IssueStatus status,
        IssuePriority priority,
        Long assigneeNo,
        LocalDate dueDate
    ) {}

    public record UpdateRequest(
        @Size(min = 2, max = 200, message = "제목은 2~200자여야 합니다.")
        String title,

        String description,
        IssueStatus status,
        IssuePriority priority,
        Long assigneeNo,
        LocalDate dueDate
    ) {}

    public record Response(
        Long no,
        Long projectNo,
        String issueKey,
        String title,
        String description,
        IssueStatus status,
        IssuePriority priority,
        Long assigneeNo,
        int position,
        LocalDate dueDate
    ) {
        public static Response from(Issue issue) {
            return new Response(
                issue.getNo(),
                issue.getProjectNo(),
                issue.getIssueKey(),
                issue.getTitle(),
                issue.getDescription(),
                issue.getStatus(),
                issue.getPriority(),
                issue.getAssigneeNo(),
                issue.getPosition(),
                issue.getDueDate()
            );
        }
    }

    public record StatusChangeRequest(
        @NotNull(message = "status 는 필수입니다.") IssueStatus status
    ) {}

    public record PositionChangeRequest(
        @NotNull(message = "position 은 필수입니다.")
        @PositiveOrZero(message = "position 은 0 이상이어야 합니다.")
        Integer position
    ) {}

    public record StatusResponse(Long no, IssueStatus status) {
        public static StatusResponse from(Issue issue) {
            return new StatusResponse(issue.getNo(), issue.getStatus());
        }
    }

    public record PositionResponse(Long no, int position) {
        public static PositionResponse from(Issue issue) {
            return new PositionResponse(issue.getNo(), issue.getPosition());
        }
    }
}

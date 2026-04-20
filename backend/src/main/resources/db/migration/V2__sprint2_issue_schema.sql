-- ============================================================================
-- V2: Sprint 2 — 이슈 관리 (칸반 보드)
--   [1] issue — 프로젝트 내 이슈 레코드
--
-- PK      = no BIGINT AUTO_INCREMENT
-- FK      = {table}_no BIGINT
-- 타임스탬프 = create_date / update_date / delete_date (TIMESTAMPTZ)
-- soft delete = delete_date IS NOT NULL 인 레코드는 조회에서 제외 (RISK-IMPACT 2026-04-20 결정)
-- 테이블명은 V1 컨벤션(단수형 user/project/workspace) 을 따른다.
-- ============================================================================

-- [1] issue ------------------------------------------------------------------
CREATE TABLE issue (
    no              BIGSERIAL    PRIMARY KEY,
    project_no      BIGINT       NOT NULL REFERENCES project(no) ON DELETE CASCADE,
    issue_key       VARCHAR(20)  NOT NULL,                 -- "TF-1", "TF-2" 형태
    title           VARCHAR(200) NOT NULL,
    description     TEXT             NULL,
    status          VARCHAR(15)  NOT NULL DEFAULT 'BACKLOG',  -- BACKLOG|TODO|IN_PROGRESS|DONE
    priority        VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',   -- LOW|MEDIUM|HIGH|CRITICAL
    assignee_no     BIGINT           NULL REFERENCES "user"(no) ON DELETE SET NULL,
    position        INT          NOT NULL DEFAULT 0,       -- 같은 status 컬럼 내 오름차순 정렬 키
    due_date        DATE             NULL,
    create_date     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    update_date     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    delete_date     TIMESTAMPTZ      NULL,                 -- soft delete 마커
    CONSTRAINT uk_issue_key UNIQUE (project_no, issue_key)
);

CREATE INDEX idx_issue_project_status ON issue(project_no, status);
CREATE INDEX idx_issue_assignee       ON issue(assignee_no);
CREATE INDEX idx_issue_project_active ON issue(project_no) WHERE delete_date IS NULL;

-- ============================================================================
-- V1: Sprint 1 도메인 초기 스키마 (ERD v0.1)
--   [1] user / refresh_token            — 인증
--   [2] workspace / workspace_member / workspace_invitation
--   [3] project / project_member
--
-- PK      = no BIGINT AUTO_INCREMENT
-- FK      = {table}_no BIGINT
-- 타임스탬프 = create_date / update_date / delete_date (TIMESTAMPTZ)
-- ============================================================================

-- [1] user -------------------------------------------------------------------
CREATE TABLE "user" (
    no              BIGSERIAL PRIMARY KEY,
    user_id         VARCHAR(100)     NULL UNIQUE,           -- 자체 회원가입 시. OAuth 전용은 NULL
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    picture         VARCHAR(500)     NULL,
    provider        VARCHAR(20)      NULL,                  -- GOOGLE 등 (자체 회원가입은 NULL)
    provider_id     VARCHAR(255)     NULL,                  -- OAuth sub
    password_hash   VARCHAR(255)     NULL,                  -- 자체 회원가입 시
    create_date     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    update_date     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    delete_date     TIMESTAMPTZ      NULL,
    CONSTRAINT uk_user_provider UNIQUE (provider, provider_id)
);

-- [1] refresh_token ----------------------------------------------------------
CREATE TABLE refresh_token (
    no              BIGSERIAL PRIMARY KEY,
    user_no         BIGINT       NOT NULL REFERENCES "user"(no) ON DELETE CASCADE,
    token_hash      VARCHAR(128) NOT NULL UNIQUE,
    family_id       VARCHAR(64)  NOT NULL,                  -- UUID 문자열
    used            BOOLEAN      NOT NULL DEFAULT FALSE,
    user_agent      VARCHAR(500) NOT NULL,
    ip_address      VARCHAR(45)      NULL,
    expire_date     TIMESTAMPTZ  NOT NULL,
    create_date     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_rt_user       ON refresh_token(user_no);
CREATE INDEX idx_rt_family     ON refresh_token(family_id);
CREATE INDEX idx_rt_expire     ON refresh_token(expire_date);

-- [2] workspace --------------------------------------------------------------
CREATE TABLE workspace (
    no              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(140) NOT NULL UNIQUE,           -- slugify(name)+'-'+UUID
    create_date     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    update_date     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE TABLE workspace_member (
    no              BIGSERIAL PRIMARY KEY,
    workspace_no    BIGINT       NOT NULL REFERENCES workspace(no) ON DELETE CASCADE,
    user_no         BIGINT       NOT NULL REFERENCES "user"(no)   ON DELETE CASCADE,
    role            VARCHAR(10)  NOT NULL,                  -- OWNER|MEMBER|GUEST
    join_date       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_wm UNIQUE (workspace_no, user_no)
);

CREATE TABLE workspace_invitation (
    no                 BIGSERIAL PRIMARY KEY,
    workspace_no       BIGINT       NOT NULL REFERENCES workspace(no) ON DELETE CASCADE,
    invitee_user_no    BIGINT       NOT NULL REFERENCES "user"(no),
    inviter_user_no    BIGINT       NOT NULL REFERENCES "user"(no),
    role               VARCHAR(10)  NOT NULL,               -- MEMBER|GUEST
    token              VARCHAR(128) NOT NULL UNIQUE,
    status             VARCHAR(10)  NOT NULL,               -- PENDING|ACCEPTED|EXPIRED|REVOKED
    expire_date        TIMESTAMPTZ  NOT NULL,               -- 기본 TTL 7일
    create_date        TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_wi_workspace ON workspace_invitation(workspace_no);
CREATE INDEX idx_wi_invitee   ON workspace_invitation(invitee_user_no);

-- [3] project ----------------------------------------------------------------
CREATE TABLE project (
    no              BIGSERIAL PRIMARY KEY,
    workspace_no    BIGINT       NOT NULL REFERENCES workspace(no) ON DELETE CASCADE,
    name            VARCHAR(100) NOT NULL,
    key             VARCHAR(10)  NOT NULL,                  -- workspace 내 unique
    description     VARCHAR(500)     NULL,
    icon            VARCHAR(100)     NULL,
    color           VARCHAR(20)      NULL,
    visibility      VARCHAR(10)  NOT NULL DEFAULT 'PRIVATE',-- PUBLIC|PRIVATE
    status          VARCHAR(15)  NOT NULL DEFAULT 'ACTIVE', -- ACTIVE|COMPLETED
    ticket_counter  INT          NOT NULL DEFAULT 0,
    create_date     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    update_date     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_project_key UNIQUE (workspace_no, key)
);

CREATE TABLE project_member (
    no              BIGSERIAL PRIMARY KEY,
    project_no      BIGINT       NOT NULL REFERENCES project(no) ON DELETE CASCADE,
    user_no         BIGINT       NOT NULL REFERENCES "user"(no) ON DELETE CASCADE,
    role            VARCHAR(10)  NOT NULL,                  -- OWNER|MEMBER|VIEWER
    join_date       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_pm UNIQUE (project_no, user_no)
);

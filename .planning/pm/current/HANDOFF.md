# Sprint 1 — 엔지니어링 핸드오프

> ⚠️ **ERD v0.1 정렬 (2026-04-14)** — DB 스키마와 API 계약을 `.planning/architecture/ERD.md` v0.1에 맞춰 전면 재작성. PK는 `no` (bigint AUTO_INCREMENT), FK는 `{참조테이블}_no`, 타임스탬프는 `create_date` / `update_date` / `delete_date`.

---

## 1. 기술 제약사항

### Backend (Spring Boot)
- **ERD v0.1** 및 `auth-design.md` 를 기준으로 구현
- PK: `bigint no` (IDENTITY), FK: `{x}_no`
- 타임스탬프: `create_date` / `update_date` / `delete_date` (soft delete)
- JWT 서명: HS256, 환경변수로 시크릿 키 관리
- Refresh Token: SHA-256 해시로 저장, Token Rotation + Replay Detection 필수
  - `family_id` 는 세션 계열 식별용 문자열 (UUID)
  - ERD 주석상 "실제 저장소는 Redis" 이나 Sprint 1 범위에서는 RDB 구현 유지
- Google OAuth: Authorization Code Flow, `id_token` 에서 사용자 정보 추출
- USER 식별은 `provider + provider_id` 조합 (예: `GOOGLE` + Google `sub`)
- Rate Limiting: Redis 기반 (`POST /api/auth/google` IP당 10회/분)
- DB: PostgreSQL, Flyway 또는 직접 마이그레이션

### Frontend (React + FSD)
- Access Token: Zustand store(메모리)에만 저장 — localStorage/sessionStorage 금지
- Refresh Token: httpOnly Cookie 브라우저 자동 관리
- API 클라이언트: axios 1.14.0, interceptor 자동 갱신
- 라우팅: react-router-dom, ProtectedRoute/PublicRoute 패턴
- 상태: zustand (인증), @tanstack/react-query (서버 상태)
- **응답 식별자는 `no` (bigint)** — JSON 직렬화 시 JS `number` 상한(2^53-1) 고려. 현재 규모에서는 `number`로 안전, 장기적으로는 `string` 직렬화 옵션 검토.

---

## 2. API 계약 (Backend ↔ Frontend)

### 인증 API

```
POST   /api/auth/google
  Req:  { code: string, redirectUri: string }
  Res:  { accessToken: string,
          user: { no, email, name, picture, provider },
          isNewUser: boolean }
  Set-Cookie: refreshToken (httpOnly)

POST   /api/auth/refresh
  Req:  (Cookie 자동 전송)
  Res:  { accessToken: string }
  Set-Cookie: refreshToken (회전됨)

POST   /api/auth/logout
  Res:  204 No Content
  Cookie 삭제
```

### 워크스페이스 API

```
POST   /api/workspaces
  Req:  { name: string }
  Res:  { no, name, slug, role: "OWNER" }

GET    /api/workspaces
  Res:  [{ no, name, slug, memberCount }]

GET    /api/workspaces/{no}
  Res:  { no, name, slug, members: [{ no, user: {...}, role }] }

POST   /api/workspaces/{no}/invite
  Req:  { inviteeUserNo: number, role: "MEMBER"|"GUEST" }
  Res:  { no, status: "PENDING", token, expireDate }
```

### 프로젝트 API

```
POST   /api/workspaces/{wsNo}/projects
  Req:  { name, key, description?, visibility: "PUBLIC"|"PRIVATE" }
  Res:  { no, workspaceNo, name, key, description, visibility, status: "ACTIVE" }

GET    /api/workspaces/{wsNo}/projects
  Res:  [{ no, name, key, description, memberCount }]

GET    /api/projects/{no}
  Res:  { no, workspaceNo, name, key, description, icon, color, visibility, status }
```

### 에러 응답 형식

```json
{
  "success": false,
  "error": {
    "code": "AUTH_TOKEN_EXPIRED",
    "message": "설명"
  },
  "timestamp": "2026-04-14T12:00:00Z"
}
```

---

## 3. DB 스키마 (Sprint 1 범위, ERD v0.1)

> 모든 PK = `no BIGINT AUTO_INCREMENT PRIMARY KEY`, FK = `{table}_no BIGINT`, 타임스탬프 = `create_date` / `update_date` / `delete_date`.

```sql
-- ============================================================
-- [1] USER — 사용자 계정
-- ============================================================
CREATE TABLE `user` (
    no              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         VARCHAR(100)     NULL UNIQUE,            -- 자체 회원가입 시. OAuth 전용은 NULL
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    picture         VARCHAR(500)     NULL,
    provider        VARCHAR(20)      NULL,                   -- GOOGLE 등 (자체 회원가입은 NULL)
    provider_id     VARCHAR(255)     NULL,                   -- OAuth sub
    password_hash   VARCHAR(255)     NULL,                   -- 자체 회원가입 시
    create_date     DATETIME(6)  NOT NULL,
    update_date     DATETIME(6)  NOT NULL,
    delete_date     DATETIME(6)      NULL,                   -- soft delete
    UNIQUE KEY uk_user_provider (provider, provider_id)
);

-- ============================================================
-- [1] REFRESH_TOKEN — 세션 토큰 (Rotation + Replay Detection)
-- ============================================================
CREATE TABLE refresh_token (
    no              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_no         BIGINT       NOT NULL,
    token_hash      VARCHAR(128) NOT NULL UNIQUE,            -- SHA-256
    family_id       VARCHAR(64)  NOT NULL,                   -- 세션 계열 (UUID 문자열)
    used            BOOLEAN      NOT NULL DEFAULT FALSE,
    user_agent      VARCHAR(500) NOT NULL,
    ip_address      VARCHAR(45)      NULL,
    expire_date     DATETIME(6)  NOT NULL,
    create_date     DATETIME(6)  NOT NULL,
    CONSTRAINT fk_rt_user FOREIGN KEY (user_no) REFERENCES `user`(no) ON DELETE CASCADE,
    KEY idx_rt_family (family_id)
);

-- ============================================================
-- [2] WORKSPACE — 워크스페이스 + 멤버 + 초대
-- ============================================================
CREATE TABLE workspace (
    no              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(100) NOT NULL,
    slug            VARCHAR(140) NOT NULL UNIQUE,            -- slugify(name) + '-' + UUID
    create_date     DATETIME(6)  NOT NULL,
    update_date     DATETIME(6)  NOT NULL
);

CREATE TABLE workspace_member (
    no              BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_no    BIGINT       NOT NULL,
    user_no         BIGINT       NOT NULL,
    role            VARCHAR(10)  NOT NULL,                   -- OWNER|MEMBER|GUEST
    join_date       DATETIME(6)  NOT NULL,
    CONSTRAINT fk_wm_workspace FOREIGN KEY (workspace_no) REFERENCES workspace(no) ON DELETE CASCADE,
    CONSTRAINT fk_wm_user      FOREIGN KEY (user_no)      REFERENCES `user`(no)   ON DELETE CASCADE,
    UNIQUE KEY uk_wm (workspace_no, user_no)
);

CREATE TABLE workspace_invitation (
    no                 BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_no       BIGINT       NOT NULL,
    invitee_user_no    BIGINT       NOT NULL,
    inviter_user_no    BIGINT       NOT NULL,
    role               VARCHAR(10)  NOT NULL,                -- MEMBER|GUEST
    token              VARCHAR(128) NOT NULL UNIQUE,
    status             VARCHAR(10)  NOT NULL,                -- PENDING|ACCEPTED|EXPIRED|REVOKED
    expire_date        DATETIME(6)  NOT NULL,                -- 기본 TTL 7일
    create_date        DATETIME(6)  NOT NULL,
    CONSTRAINT fk_wi_workspace FOREIGN KEY (workspace_no)    REFERENCES workspace(no) ON DELETE CASCADE,
    CONSTRAINT fk_wi_invitee   FOREIGN KEY (invitee_user_no) REFERENCES `user`(no),
    CONSTRAINT fk_wi_inviter   FOREIGN KEY (inviter_user_no) REFERENCES `user`(no)
);

-- ============================================================
-- [3] PROJECT — 프로젝트 + 멤버
-- ============================================================
CREATE TABLE project (
    no              BIGINT AUTO_INCREMENT PRIMARY KEY,
    workspace_no    BIGINT       NOT NULL,
    name            VARCHAR(100) NOT NULL,
    `key`           VARCHAR(10)  NOT NULL,                   -- workspace 내 unique, 티켓 접두사
    description     VARCHAR(500)     NULL,
    icon            VARCHAR(100)     NULL,
    color           VARCHAR(20)      NULL,
    visibility      VARCHAR(10)  NOT NULL DEFAULT 'PRIVATE', -- PUBLIC|PRIVATE
    status          VARCHAR(15)  NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE|COMPLETED
    ticket_counter  INT          NOT NULL DEFAULT 0,         -- 티켓 시퀀스 (원자적 +1)
    create_date     DATETIME(6)  NOT NULL,
    update_date     DATETIME(6)  NOT NULL,
    CONSTRAINT fk_project_workspace FOREIGN KEY (workspace_no) REFERENCES workspace(no) ON DELETE CASCADE,
    UNIQUE KEY uk_project_key (workspace_no, `key`)
);

CREATE TABLE project_member (
    no              BIGINT AUTO_INCREMENT PRIMARY KEY,
    project_no      BIGINT       NOT NULL,
    user_no         BIGINT       NOT NULL,
    role            VARCHAR(10)  NOT NULL,                   -- OWNER|MEMBER|VIEWER
    join_date       DATETIME(6)  NOT NULL,
    CONSTRAINT fk_pm_project FOREIGN KEY (project_no) REFERENCES project(no) ON DELETE CASCADE,
    CONSTRAINT fk_pm_user    FOREIGN KEY (user_no)    REFERENCES `user`(no) ON DELETE CASCADE,
    UNIQUE KEY uk_pm (project_no, user_no)
);
```

---

## 4. 테스트 전략

| 영역 | 방식 | 범위 |
|------|------|------|
| Backend Service | `@ExtendWith(MockitoExtension.class)` | AuthService, WorkspaceService, ProjectService |
| Backend Controller | `@WebMvcTest` | AuthController, WorkspaceController, ProjectController |
| Backend Repository | `@DataJpaTest` | 커스텀 쿼리가 있는 경우만 |
| Frontend | 수동 E2E | 로그인 → 허브 → 프로젝트 진입 플로우 |

---

## 5. 환경 변수

```env
# Backend
GOOGLE_CLIENT_ID=
GOOGLE_CLIENT_SECRET=
JWT_SECRET=
JWT_EXPIRATION=900000
REFRESH_TOKEN_EXPIRATION=604800000
DATABASE_URL=jdbc:postgresql://localhost:5432/teamflow
REDIS_HOST=localhost
REDIS_PORT=6379

# Frontend
VITE_GOOGLE_CLIENT_ID=
VITE_API_BASE_URL=http://localhost:8080
VITE_GOOGLE_REDIRECT_URI=http://localhost:5173/auth/callback
```

---

## 6. 변경 이력

- **2026-04-14** — ERD v0.1 정렬: PK `UUID id` → `BIGINT no`, `google_id/avatar_url` 제거 후 `picture/provider/provider_id`로 정규화, 타임스탬프 `*_at` → `*_date`, `workspace_invites` 재설계(초대 대상을 이메일에서 USER.no 참조로 변경). Figma Sprint 01 Description 도 동일 정렬.

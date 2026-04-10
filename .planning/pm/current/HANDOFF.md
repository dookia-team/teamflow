# Sprint 1 — 엔지니어링 핸드오프

---

## 1. 기술 제약사항

### Backend (Spring Boot)
- **auth-design.md** 문서를 기준으로 구현
- JWT 서명: HS256, 환경변수로 시크릿 키 관리
- Refresh Token: SHA-256 해시로 DB 저장, Token Rotation + Replay Detection 필수
- Google OAuth: Authorization Code Flow, id_token에서 사용자 정보 추출
- Rate Limiting: Redis 기반 (POST /api/auth/google IP당 10회/분)
- DB: PostgreSQL, Flyway 또는 직접 마이그레이션

### Frontend (React + FSD)
- Access Token: Zustand store(메모리)에만 저장 — localStorage/sessionStorage 절대 금지
- Refresh Token: httpOnly Cookie로 브라우저가 자동 관리
- API 클라이언트: axios 1.14.0 (고정), interceptor로 자동 갱신
- 라우팅: react-router-dom, ProtectedRoute/PublicRoute 패턴
- 상태: zustand (인증), @tanstack/react-query (서버 상태)

---

## 2. API 계약 (Backend ↔ Frontend)

### 인증 API

```
POST   /api/auth/google     → { accessToken, user, isNewUser } + Set-Cookie
POST   /api/auth/refresh    → { accessToken } + Set-Cookie (Cookie 자동 전송)
DELETE /api/auth/logout      → 204 No Content + Cookie 삭제
```

### 워크스페이스 API

```
POST   /api/workspaces                → { id, name, slug, role }
GET    /api/workspaces                → [{ id, name, slug, memberCount }]
GET    /api/workspaces/:id            → { id, name, slug, members[] }
POST   /api/workspaces/:id/invite     → { inviteLink } 또는 { memberId }
```

### 프로젝트 API

```
POST   /api/workspaces/:wsId/projects          → { id, name, key, description }
GET    /api/workspaces/:wsId/projects          → [{ id, name, key, memberCount }]
GET    /api/projects/:id                        → { id, name, key, description }
```

### 에러 응답 형식

```json
{
  "error": {
    "code": "AUTH_TOKEN_EXPIRED",
    "message": "설명"
  },
  "timestamp": "2026-04-10T12:00:00Z"
}
```

---

## 3. DB 스키마 (Sprint 1 범위)

```sql
-- users (auth-design.md 기준)
-- refresh_tokens (auth-design.md 기준)

-- 추가 테이블
CREATE TABLE workspaces (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    owner_id    UUID NOT NULL REFERENCES users(id),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE workspace_members (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    user_id       UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role          VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    joined_at     TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(workspace_id, user_id)
);

CREATE TABLE workspace_invites (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    email         VARCHAR(255),
    invite_token  VARCHAR(128) NOT NULL UNIQUE,
    expires_at    TIMESTAMPTZ NOT NULL,
    used          BOOLEAN NOT NULL DEFAULT FALSE,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE projects (
    id            UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    workspace_id  UUID NOT NULL REFERENCES workspaces(id) ON DELETE CASCADE,
    name          VARCHAR(100) NOT NULL,
    key           VARCHAR(10) NOT NULL,
    description   TEXT,
    created_by    UUID NOT NULL REFERENCES users(id),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(workspace_id, key)
);
```

---

## 4. 테스트 전략

| 영역 | 방식 | 범위 |
|------|------|------|
| Backend Service | @ExtendWith(MockitoExtension.class) | AuthService, WorkspaceService, ProjectService |
| Backend Controller | @WebMvcTest | AuthController, WorkspaceController, ProjectController |
| Backend Repository | @DataJpaTest | 커스텀 쿼리가 있는 경우만 |
| Frontend | 수동 E2E 확인 | 로그인 → 허브 → 프로젝트 진입 플로우 |

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

# TeamFlow API — Sprint 01

> **Base URL**: `http://localhost:8082`
> **버전**: v1
> **출처**: `/v3/api-docs` (OpenAPI 3.0.1) + 실제 DTO 코드 보정
> **범위**: Sprint 1 (인증 + 워크스페이스/프로젝트 허브)

---

## 1. 인증

### 1.1 인증 방식

- **Access Token**: JWT Bearer — `Authorization: Bearer {accessToken}` 헤더
  - 알고리즘: HS256
  - TTL: 900초 (15분)
  - payload: `sub=user.no`, `email`, `name`, `iat`, `exp`
- **Refresh Token**: httpOnly Cookie
  - 이름: `teamflow_rt`
  - TTL: 604800초 (7일)
  - `Secure`, `HttpOnly`, `SameSite=Strict`, `Path=/api/auth`
  - DB에는 SHA-256 해시만 저장 (Token Rotation + Replay Detection)

### 1.2 인증 불필요 엔드포인트

- `POST /api/auth/google`
- `POST /api/auth/refresh` (Cookie 기반)
- `POST /api/auth/logout`

그 외 모든 엔드포인트는 **Bearer 토큰 필수**.

---

## 2. 공통 응답 포맷

### 2.1 성공 응답 (`ApiResponse<T>`)

```json
{
  "success": true,
  "data": { /* T */ },
  "message": null,
  "timestamp": "2026-04-14T12:00:00.000"
}
```

### 2.2 인증 도메인 에러 (`ErrorResponse`)

```json
{
  "error": {
    "code": "AUTH_TOKEN_EXPIRED",
    "message": "Access Token 이 만료되었습니다."
  },
  "timestamp": "2026-04-14T12:00:00.000"
}
```

### 2.3 일반 에러 (`ApiResponse` 실패)

```json
{
  "success": false,
  "data": null,
  "message": "워크스페이스 멤버가 아닙니다.",
  "timestamp": "2026-04-14T12:00:00.000"
}
```

### 2.4 HTTP 상태 매핑

| 예외 종류 | HTTP | 포맷 |
|----------|------|------|
| `AuthException` | 400/401/502 (code별) | `ErrorResponse` |
| `EntityNotFoundException` | 404 | `ApiResponse` |
| `WorkspaceAccessDeniedException` | 403 | `ApiResponse` |
| `IllegalStateException` | 409 | `ApiResponse` |
| `IllegalArgumentException` | 400 | `ApiResponse` |
| Bean Validation 실패 | 400 | `ApiResponse` |
| 기타 `RuntimeException` | 500 | `ApiResponse` |

---

## 3. 인증 도메인 (`/api/auth`)

### 3.1 `POST /api/auth/google` — Google OAuth 로그인/회원가입

**요청 Body**

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| `code` | string | ✓ | Google Authorization Code |
| `redirectUri` | string | ✓ | 프론트의 리다이렉트 URI (Google 콘솔 등록값) |

```json
{
  "code": "4/0AY0e-...",
  "redirectUri": "http://localhost:5173/auth/callback"
}
```

**응답 200 — 로그인 성공**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
  "user": {
    "no": 1,
    "email": "alice@example.com",
    "name": "Alice",
    "picture": "https://.../photo.jpg",
    "provider": "GOOGLE"
  },
  "isNewUser": true
}
```

- `Set-Cookie: teamflow_rt={refreshToken}; Path=/api/auth; HttpOnly; Secure; SameSite=Strict; Max-Age=604800`
- `isNewUser: true` 인 경우 프론트는 워크스페이스 생성 모달로 분기 (Sprint 1 §4 참고)

**에러 응답**

| HTTP | code | 상황 |
|------|------|------|
| 400 | `AUTH_MISSING_CODE` | `code` 누락 |
| 400 | `AUTH_INVALID_CODE` | Google 토큰 교환 실패 (잘못된 code) |
| 502 | `AUTH_GOOGLE_ERROR` | Google 서버 통신 실패 |

---

### 3.2 `POST /api/auth/refresh` — Access Token 갱신 (Token Rotation)

**요청**: Cookie `teamflow_rt` 만 필요. Body 없음.

**응답 200**

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

- 새 Refresh Token 이 `Set-Cookie` 로 교체됨 (같은 family 유지)
- 기존 Refresh Token 은 DB에서 `used=true` 마킹

**에러 응답 (401)**

| code | 상황 |
|------|------|
| `AUTH_TOKEN_MISSING` | Cookie 없음 |
| `AUTH_TOKEN_EXPIRED` | Refresh Token 만료 |
| `AUTH_TOKEN_INVALID` | DB에 없거나 변조 |
| `AUTH_TOKEN_REUSED` | 이미 used=true 인 토큰 재사용 → **family 전체 무효화** |

> `AUTH_TOKEN_REUSED` 수신 시 프론트는 사용자를 **즉시 로그아웃 + 로그인 화면으로 이동** 시키세요. 토큰 도난 시나리오로 간주됩니다.

---

### 3.3 `POST /api/auth/logout` — 로그아웃

**요청**: Cookie `teamflow_rt`. Body 없음.

**응답 204 No Content**

- `Set-Cookie: teamflow_rt=; Max-Age=0` (쿠키 즉시 만료)
- 해당 Refresh Token DB 레코드 삭제

---

## 4. 워크스페이스 도메인 (`/api/workspaces`)

모든 엔드포인트는 **Bearer 토큰 필수**. 이하 `{userNo}` 는 Access Token의 `sub` 에서 추출됩니다.

### 4.1 `POST /api/workspaces` — 워크스페이스 생성

**요청 Body**

| 필드 | 타입 | 제약 |
|------|------|------|
| `name` | string | 2~100자 (필수) |

```json
{ "name": "TeamFlow" }
```

**응답 201 Created**

```json
{
  "success": true,
  "data": {
    "no": 10,
    "name": "TeamFlow",
    "slug": "teamflow-3f2b1c8a",
    "role": "OWNER",
    "memberCount": 1
  },
  "timestamp": "..."
}
```

- 생성자는 자동으로 `OWNER` 멤버로 등록됨.
- `slug` = `slugify(name) + '-' + UUID 앞 8자` (전역 유일).

**에러**: 400 Validation (이름 길이)

---

### 4.2 `GET /api/workspaces` — 내 워크스페이스 목록

**응답 200**

```json
{
  "success": true,
  "data": [
    { "no": 10, "name": "TeamFlow", "slug": "teamflow-3f2b1c8a", "memberCount": 3 },
    { "no": 11, "name": "Design",   "slug": "design-b7e92a01",   "memberCount": 2 }
  ]
}
```

- 가입된 워크스페이스만 반환 (join_date 오름차순)

---

### 4.3 `GET /api/workspaces/{workspaceNo}` — 워크스페이스 상세

**Path**: `workspaceNo: long`

**응답 200**

```json
{
  "success": true,
  "data": {
    "no": 10,
    "name": "TeamFlow",
    "slug": "teamflow-3f2b1c8a",
    "members": [
      { "no": 100, "userNo": 1, "role": "OWNER",  "joinDate": "2026-04-14T12:00:00" },
      { "no": 101, "userNo": 2, "role": "MEMBER", "joinDate": "2026-04-14T12:05:00" }
    ]
  }
}
```

**에러**

| HTTP | 상황 |
|------|------|
| 403 | 호출자가 워크스페이스 멤버가 아님 |
| 404 | 워크스페이스 없음 |

---

### 4.4 `POST /api/workspaces/{workspaceNo}/invite` — 멤버 초대

**요청 Body**

| 필드 | 타입 | 제약 |
|------|------|------|
| `inviteeUserNo` | long | 필수, 기존 USER.no |
| `role` | enum | `MEMBER` 또는 `GUEST` — `OWNER` 지정 불가 |

```json
{ "inviteeUserNo": 42, "role": "MEMBER" }
```

**응답 201 Created**

```json
{
  "success": true,
  "data": {
    "no": 500,
    "workspaceNo": 10,
    "inviteeUserNo": 42,
    "role": "MEMBER",
    "status": "PENDING",
    "token": "vQZ9L...base64url",
    "expireDate": "2026-04-21T12:00:00"
  }
}
```

- 토큰 TTL: 7일 (기본)
- 프론트가 `token` 을 초대 수락 링크에 포함시켜 초대 대상자에게 전달

**에러**

| HTTP | 상황 |
|------|------|
| 400 | `role=OWNER` 로 초대 시도 |
| 403 | 호출자가 `OWNER` 가 아님 |
| 404 | 초대 대상 USER 또는 워크스페이스 없음 |
| 409 | 이미 해당 워크스페이스의 멤버 |

---

## 5. 프로젝트 도메인

### 5.1 `POST /api/workspaces/{workspaceNo}/projects` — 프로젝트 생성

**Path**: `workspaceNo: long`

**요청 Body**

| 필드 | 타입 | 제약 |
|------|------|------|
| `name` | string | 2~100자 (필수) |
| `key` | string | `^[A-Z][A-Z0-9]{1,9}$` 2~10자 대문자/숫자, 첫 글자 대문자 (필수) |
| `description` | string | 최대 500자 (선택) |
| `visibility` | enum | `PUBLIC` 또는 `PRIVATE` (선택, 기본 `PRIVATE`) |

```json
{
  "name": "TeamFlow",
  "key": "TF",
  "description": "프로젝트 관리 도구 MVP",
  "visibility": "PRIVATE"
}
```

**응답 201 Created**

```json
{
  "success": true,
  "data": {
    "no": 50,
    "workspaceNo": 10,
    "name": "TeamFlow",
    "key": "TF",
    "description": "프로젝트 관리 도구 MVP",
    "icon": null,
    "color": null,
    "visibility": "PRIVATE",
    "status": "ACTIVE"
  }
}
```

- 생성자는 자동으로 `OWNER` 프로젝트 멤버로 등록.
- `key` 는 해당 워크스페이스 내에서 unique (티켓 key 접두사, 예: `TF-1`).

**에러**

| HTTP | 상황 |
|------|------|
| 400 | Validation (name/key 패턴, visibility enum) |
| 403 | 호출자가 워크스페이스 멤버가 아님 |
| 404 | 워크스페이스 없음 |
| 409 | 같은 워크스페이스에 동일 `key` 가 이미 존재 |

---

### 5.2 `GET /api/workspaces/{workspaceNo}/projects` — 프로젝트 목록

**응답 200**

```json
{
  "success": true,
  "data": [
    {
      "no": 50,
      "workspaceNo": 10,
      "name": "TeamFlow",
      "key": "TF",
      "description": "프로젝트 관리 도구 MVP",
      "memberCount": 3
    },
    {
      "no": 51,
      "workspaceNo": 10,
      "name": "Design System",
      "key": "DS",
      "description": null,
      "memberCount": 2
    }
  ]
}
```

- `createDate DESC` 정렬 (최신순)

**에러**

| HTTP | 상황 |
|------|------|
| 403 | 워크스페이스 비멤버 |
| 404 | 워크스페이스 없음 |

---

### 5.3 `GET /api/projects/{projectNo}` — 프로젝트 상세

**Path**: `projectNo: long`

**응답 200**: 5.1 응답과 동일 구조

**에러**

| HTTP | 상황 |
|------|------|
| 403 | 프로젝트가 속한 워크스페이스의 비멤버 |
| 404 | 프로젝트 없음 |

---

## 6. Enum 레퍼런스

### 6.1 `UserProvider`

| 값 | 의미 |
|----|------|
| `GOOGLE` | Google OAuth 로그인 사용자 |

> 자체 회원가입 사용자는 `provider` 필드가 `null` 입니다.

### 6.2 `WorkspaceMemberRole`

| 값 | 권한 |
|----|------|
| `OWNER` | 워크스페이스 전권 (멤버 초대·설정 변경) |
| `MEMBER` | 편집자 (기본) |
| `GUEST` | 제한 접근 |

### 6.3 `WorkspaceInvitationStatus`

| 값 | 의미 |
|----|------|
| `PENDING` | 초대 발송됨, 대기 중 |
| `ACCEPTED` | 초대 수락 완료 → `WorkspaceMember` 에 추가됨 |
| `EXPIRED` | TTL 경과로 자동 만료 |
| `REVOKED` | 초대자가 취소 |

### 6.4 `ProjectVisibility`

| 값 | 의미 |
|----|------|
| `PUBLIC` | 워크스페이스 멤버 전체 공개 |
| `PRIVATE` | `ProjectMember` 만 접근 가능 (기본) |

### 6.5 `ProjectStatus`

| 값 | 의미 |
|----|------|
| `ACTIVE` | 진행 중, 편집 가능 (기본) |
| `COMPLETED` | 완료, 읽기 전용 |

### 6.6 `ProjectMemberRole`

| 값 | 권한 |
|----|------|
| `OWNER` | 프로젝트 전권 |
| `MEMBER` | 편집자 |
| `VIEWER` | 읽기 전용 |

---

## 7. 인증 에러 코드 전체 목록

| `code` | HTTP | 상황 |
|--------|------|------|
| `AUTH_MISSING_CODE` | 400 | Google OAuth `code` 누락 |
| `AUTH_INVALID_CODE` | 400 | `code` 로 Google 토큰 교환 실패 |
| `AUTH_GOOGLE_ERROR` | 502 | Google 서버 통신 오류 |
| `AUTH_TOKEN_MISSING` | 401 | 토큰 누락 |
| `AUTH_TOKEN_EXPIRED` | 401 | 토큰 만료 |
| `AUTH_TOKEN_INVALID` | 401 | 토큰 변조/DB에 없음 |
| `AUTH_TOKEN_REUSED` | 401 | Refresh Token 재사용 감지 — family 전체 폐기 |

---

## 8. 프론트엔드 연동 체크리스트

- [ ] `axios` interceptor 에서 401 + `AUTH_TOKEN_EXPIRED` 수신 시 `POST /api/auth/refresh` 자동 호출 후 원 요청 재시도
- [ ] `AUTH_TOKEN_REUSED` 수신 시 Zustand `authStore.clear()` + `/login` 이동
- [ ] Access Token 은 **Zustand (메모리)** 에만 저장 — `localStorage` 금지
- [ ] Refresh Token 은 브라우저가 자동 관리 — JS에서 접근 불가 (httpOnly)
- [ ] 로그인 성공 후 `isNewUser=true` → `/workspace/new` (워크스페이스 생성 모달) 분기
- [ ] API 요청은 모두 `credentials: 'include'` 또는 `axios.defaults.withCredentials = true` (Cookie 전송용)
- [ ] 응답의 `data` 필드가 `null` 이면 에러 플로우 (4.x/5.x)
- [ ] `no` 필드는 JS `number` — 현재 규모에서는 안전, 2^53 넘을 우려 생기면 백엔드와 string 직렬화 합의 필요

---

## 9. 참고 문서

- ERD: `.planning/architecture/ERD.md` (v0.1)
- PM 핸드오프: `.planning/pm/current/HANDOFF.md`
- 스프린트 태스크: `.planning/pm/current/SPRINT.md`
- 백엔드 컨벤션: `.claude/rules/backend-conventions.md`
- 인증 설계 상세: `docs/auth-design.md` (해당 파일이 있다면)

Swagger UI (로컬): http://localhost:8082/swagger-ui/index.html
OpenAPI JSON (로컬): http://localhost:8082/v3/api-docs

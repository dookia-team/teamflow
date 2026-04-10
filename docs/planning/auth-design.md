# TeamFlow 인증(Auth) 기술 설계 문서

> **Version:** 1.0  
> **Date:** 2026-04-10  
> **Status:** Draft

---

## 1. 인증 플로우

### 1.1 Google OAuth 2.0 Authorization Code Flow

```
[사용자]          [Frontend]           [Backend]            [Google]           [DB]
  │                  │                    │                    │                 │
  │  1. 로그인 클릭   │                    │                    │                 │
  │─────────────────>│                    │                    │                 │
  │                  │ 2. Google OAuth    │                    │                 │
  │                  │    페이지 리다이렉트 │                    │                 │
  │                  │───────────────────────────────────────>│                 │
  │                  │                    │                    │                 │
  │  3. Google 로그인 및 동의              │                    │                 │
  │──────────────────────────────────────────────────────────>│                 │
  │                  │                    │                    │                 │
  │                  │ 4. Authorization Code 수신 (redirect)  │                 │
  │                  │<──────────────────────────────────────│                 │
  │                  │                    │                    │                 │
  │                  │ 5. POST /api/auth/google               │                 │
  │                  │    { code }        │                    │                 │
  │                  │──────────────────>│                    │                 │
  │                  │                    │ 6. Code → Token   │                 │
  │                  │                    │    교환 요청        │                 │
  │                  │                    │──────────────────>│                 │
  │                  │                    │                    │                 │
  │                  │                    │ 7. id_token +      │                 │
  │                  │                    │    access_token    │                 │
  │                  │                    │<──────────────────│                 │
  │                  │                    │                    │                 │
  │                  │                    │ 8. id_token에서 사용자 정보 추출       │
  │                  │                    │    (email, name, picture, sub)       │
  │                  │                    │                    │                 │
  │                  │                    │ 9. google_id로 사용자 조회/생성       │
  │                  │                    │──────────────────────────────────> │
  │                  │                    │                    │                 │
  │                  │                    │ 10. JWT Access Token 생성           │
  │                  │                    │     Refresh Token 생성 및 DB 저장    │
  │                  │                    │──────────────────────────────────> │
  │                  │                    │                    │                 │
  │                  │ 11. Response:      │                    │                 │
  │                  │     { accessToken, │                    │                 │
  │                  │       user }       │                    │                 │
  │                  │     + Set-Cookie:  │                    │                 │
  │                  │       refreshToken │                    │                 │
  │                  │<──────────────────│                    │                 │
  │                  │                    │                    │                 │
  │  12. 인증 완료    │                    │                    │                 │
  │     프로젝트 허브  │                    │                    │                 │
  │<─────────────────│                    │                    │                 │
```

### 1.2 신규 사용자 vs 기존 사용자

- **기존 사용자**: `google_id`로 조회 성공 → `last_login_at` 갱신 → 토큰 발급
- **신규 사용자**: `google_id` 조회 실패 → Google id_token 정보로 `users` 레코드 자동 생성 → 토큰 발급

### 1.3 프론트엔드 OAuth 흐름

```
1. "Google로 로그인" 버튼 클릭
2. window.location.href = Google OAuth Authorization URL
   - client_id, redirect_uri, response_type=code, scope=openid email profile
   - state: CSRF 방지용 랜덤 문자열 (sessionStorage 저장)
3. Google 로그인/동의 후 redirect_uri로 code 전달
4. /auth/callback 페이지에서 code 추출 + state 검증
5. POST /api/auth/google { code, redirectUri } 호출
6. accessToken → 메모리(Zustand) 저장
7. refreshToken → httpOnly Cookie 자동 저장
8. /projects (프로젝트 허브)로 리다이렉트
```

---

## 2. 토큰 전략

### 2.1 Access Token (JWT)

| 항목 | 값 |
|------|-----|
| 형식 | JWT (HS256) |
| 만료 시간 | 15분 |
| 저장 위치 | 프론트엔드 메모리 (Zustand store) |
| 전송 방식 | `Authorization: Bearer {token}` 헤더 |

**Payload:**

```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "name": "사용자 이름",
  "iat": 1712700000,
  "exp": 1712700900
}
```

> 메모리 저장은 XSS 토큰 탈취 위험을 최소화한다. 15분 만료로 탈취 시 피해 범위를 제한한다.

### 2.2 Refresh Token

| 항목 | 값 |
|------|-----|
| 형식 | opaque 랜덤 문자열 (64자) |
| 만료 시간 | 7일 |
| 클라이언트 저장 | httpOnly + Secure + SameSite=Strict Cookie |
| 서버 저장 | `refresh_tokens` 테이블 (SHA-256 해시) |
| Cookie 이름 | `teamflow_rt` |

**Cookie 속성:**

```
Set-Cookie: teamflow_rt={token};
  HttpOnly;
  Secure;
  SameSite=Strict;
  Path=/api/auth;
  Max-Age=604800
```

### 2.3 Token Rotation + Replay Detection

Refresh Token 갱신 시마다 새 토큰을 발급하고 이전 토큰을 `used=true`로 표시한다.

```
1. 클라이언트: POST /api/auth/refresh (Cookie: RT_1)
2. 서버: RT_1 조회 → 유효성 검증 → used=true로 갱신
3. 서버: 새 RT_2 생성 (같은 family_id) → DB 저장
4. 서버: 새 Access Token + Set-Cookie: RT_2 응답
```

**Replay Detection:** 이미 `used=true`인 토큰으로 갱신 요청 시 → 토큰 탈취로 간주 → 해당 `family_id`의 모든 토큰 무효화 → 재로그인 필요

---

## 3. DB 스키마

### 3.1 users 테이블

```sql
CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    google_id       VARCHAR(255) NOT NULL UNIQUE,
    email           VARCHAR(255) NOT NULL UNIQUE,
    name            VARCHAR(100) NOT NULL,
    avatar_url      VARCHAR(500),
    status_message  VARCHAR(200),
    provider        VARCHAR(20)  NOT NULL DEFAULT 'google',
    last_login_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);
```

기획서 대비 추가 컬럼:
- `google_id`: Google OAuth sub 식별자 (이메일 변경에도 안정적 식별)
- `status_message`: AUTH-02 프로필 관리 대응
- `provider`: 향후 GitHub 등 OAuth 확장 대비
- `last_login_at`: 보안 감사 및 비활성 계정 관리

### 3.2 refresh_tokens 테이블

```sql
CREATE TABLE refresh_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(128) NOT NULL UNIQUE,
    family_id   UUID NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    expires_at  TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    user_agent  VARCHAR(500),
    ip_address  INET
);
```

- `token_hash`: 원문이 아닌 SHA-256 해시 저장 (DB 유출 시 보호)
- `family_id`: Token Rotation에서 같은 세션 계열 추적
- `used`: Replay Detection용

### 3.3 인덱스

```sql
CREATE UNIQUE INDEX idx_users_google_id ON users(google_id);
CREATE UNIQUE INDEX idx_users_email ON users(email);
CREATE UNIQUE INDEX idx_refresh_tokens_token_hash ON refresh_tokens(token_hash);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens(family_id);
CREATE INDEX idx_refresh_tokens_expires_at ON refresh_tokens(expires_at);
```

### 3.4 만료 토큰 정리

Spring Boot `@Scheduled`로 매일 1회 실행:

```sql
DELETE FROM refresh_tokens WHERE expires_at < NOW();
```

---

## 4. API 상세 설계

### 4.1 POST /api/auth/google — 로그인/회원가입

**Request:**
```json
{
  "code": "4/0AX4XfWh...",
  "redirectUri": "https://app.teamflow.com/auth/callback"
}
```

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@gmail.com",
    "name": "홍길동",
    "avatarUrl": "https://lh3.googleusercontent.com/...",
    "statusMessage": null
  },
  "isNewUser": true
}
```

+ `Set-Cookie: teamflow_rt={refreshToken}; HttpOnly; Secure; SameSite=Strict; Path=/api/auth; Max-Age=604800`

**에러:**

| Status | 코드 | 설명 |
|--------|------|------|
| 400 | `AUTH_INVALID_CODE` | Authorization code가 유효하지 않음 |
| 400 | `AUTH_MISSING_CODE` | code 파라미터 누락 |
| 502 | `AUTH_GOOGLE_ERROR` | Google 서버 통신 실패 |

### 4.2 POST /api/auth/refresh — 토큰 갱신

**Request:** Body 없음. Cookie에서 Refresh Token 자동 전송.

**Response (200):**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIs..."
}
```

+ 새 Refresh Token Cookie 설정

**에러:**

| Status | 코드 | 설명 |
|--------|------|------|
| 401 | `AUTH_TOKEN_MISSING` | Cookie 없음 |
| 401 | `AUTH_TOKEN_EXPIRED` | 만료됨 |
| 401 | `AUTH_TOKEN_INVALID` | DB에 존재하지 않음 |
| 401 | `AUTH_TOKEN_REUSED` | Replay 감지 (family 전체 무효화) |

### 4.3 POST /api/auth/logout — 로그아웃

**Request:** `Authorization: Bearer {accessToken}` + Cookie

**Response:** 204 No Content

+ `Set-Cookie: teamflow_rt=; Max-Age=0` (Cookie 삭제)

동작: Cookie의 Refresh Token으로 DB에서 해당 토큰 삭제

### 4.4 공통 에러 응답 형식

```json
{
  "error": {
    "code": "AUTH_TOKEN_EXPIRED",
    "message": "Refresh token has expired. Please login again."
  },
  "timestamp": "2026-04-10T12:00:00Z"
}
```

---

## 5. 백엔드 구조 (Spring Boot)

### 5.1 패키지 구조

```
src/main/java/com/dookia/teamflow/
├── config/
│   ├── SecurityConfig.java              # Spring Security 설정
│   └── JwtConfig.java                  # JWT 설정 값
├── auth/
│   ├── controller/AuthController.java   # /api/auth/** 엔드포인트
│   ├── service/
│   │   ├── AuthService.java             # 인증 비즈니스 로직
│   │   ├── GoogleOAuthService.java      # Google API 통신
│   │   └── JwtService.java             # JWT 생성/검증
│   ├── dto/
│   │   ├── GoogleAuthRequest.java
│   │   ├── AuthResponse.java
│   │   └── TokenRefreshResponse.java
│   ├── filter/JwtAuthenticationFilter.java
│   └── exception/AuthErrorCode.java
├── user/
│   ├── entity/User.java
│   ├── repository/UserRepository.java
│   └── service/UserService.java
└── token/
    ├── entity/RefreshToken.java
    └── repository/RefreshTokenRepository.java
```

### 5.2 JWT 인증 필터

```
HTTP Request
    │
    ▼
┌─────────────────────────┐
│  JwtAuthenticationFilter │
│                         │
│  1. Authorization 헤더   │
│     에서 Bearer 토큰 추출 │
│  2. JWT 서명 검증        │
│  3. 만료 여부 확인        │
│  4. SecurityContext에    │
│     Authentication 설정  │
└────────────┬────────────┘
             │
             ▼
  허용 경로 (permitAll):
  - POST /api/auth/google
  - POST /api/auth/refresh
  
  인증 필요: 그 외 /api/** 전체
```

---

## 6. 프론트엔드 구조 (FSD)

### 6.1 디렉토리

```
src/
├── app/
│   ├── providers/AuthProvider.tsx       # 앱 초기 인증 상태 복원
│   └── routes/
│       ├── ProtectedRoute.tsx          # 인증 필요 라우트 가드
│       └── PublicRoute.tsx             # 로그인 시 접근 불가
├── entities/user/
│   ├── model/types.ts                  # User 타입
│   └── api/userApi.ts
├── features/auth/
│   ├── model/authStore.ts              # Zustand 인증 스토어
│   ├── api/authApi.ts                  # 인증 API 호출
│   ├── lib/apiClient.ts               # API 클라이언트 + interceptor
│   └── ui/
│       ├── GoogleLoginButton.tsx
│       └── LogoutButton.tsx
├── pages/
│   ├── auth/callback/index.tsx         # OAuth 콜백 처리
│   ├── login/index.tsx
│   └── projects/index.tsx              # 프로젝트 허브
└── shared/config/env.ts
```

### 6.2 Zustand 인증 스토어

```typescript
interface AuthState {
  accessToken: string | null;
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  setAuth: (token: string, user: User) => void;
  clearAuth: () => void;
  setLoading: (loading: boolean) => void;
}
```

### 6.3 AuthProvider — 새로고침 시 인증 복원

```
앱 마운트 → POST /api/auth/refresh (httpOnly cookie 자동 전송)
  ├── 성공 → accessToken + user를 store에 저장 → 자식 렌더링
  └── 실패 → 미인증 상태 → 로그인 페이지로 유도
```

### 6.4 API Interceptor — 자동 토큰 갱신

```
API 요청 → Authorization: Bearer 헤더 추가 → 서버 응답
  ├── 200~299 → 정상
  └── 401 → refresh 진행 중? → 큐에 대기
             └── POST /api/auth/refresh
                 ├── 성공 → 새 토큰 저장 → 원래 요청 + 큐 재시도
                 └── 실패 → clearAuth → /login 리다이렉트
```

### 6.5 라우트 구조

| 경로 | 타입 | 설명 |
|------|------|------|
| `/` | PublicRoute | 랜딩 페이지 |
| `/login` | PublicRoute | 로그인 (인증 시 /projects로) |
| `/auth/callback` | 공개 | OAuth 콜백 처리 |
| `/projects` | ProtectedRoute | 프로젝트 허브 |
| `/projects/:id/*` | ProtectedRoute | 워크스페이스 |

---

## 7. 보안

### 7.1 XSS 방지

| 대상 | 전략 |
|------|------|
| Access Token | 메모리 저장 (localStorage/sessionStorage 금지) |
| Refresh Token | httpOnly Cookie (JavaScript 접근 불가) |
| 사용자 입력 | React 기본 이스케이핑. `dangerouslySetInnerHTML` 금지 |

### 7.2 CSRF 방지

- Cookie: `SameSite=Strict` → 크로스 사이트 요청에서 전송 안 됨
- API: `Authorization: Bearer` 헤더 기반 → Cookie CSRF에 면역
- OAuth: `state` 파라미터로 CSRF 방지

### 7.3 Rate Limiting

| 엔드포인트 | 제한 |
|-----------|------|
| POST /api/auth/google | IP당 10회/분 |
| POST /api/auth/refresh | IP당 30회/분 |
| POST /api/auth/logout | 사용자당 10회/분 |

Redis 기반 bucket4j 또는 resilience4j로 구현.

### 7.4 기타

- HTTPS 필수 (NFR-05)
- JWT 서명 키: 환경변수 또는 시크릿 매니저에서 로드
- Refresh Token: DB에 SHA-256 해시로 저장
- SQL Injection: JPA 파라미터 바인딩 사용 (문자열 결합 쿼리 금지)
- 응답 헤더: `X-Content-Type-Options: nosniff`, `X-Frame-Options: DENY`, `Strict-Transport-Security`

---

## 8. 구현 순서

| 순서 | 작업 | 담당 | 의존성 |
|------|------|------|--------|
| 1 | DB 마이그레이션 (users, refresh_tokens) | Backend | - |
| 2 | User Entity, Repository | Backend | 1 |
| 3 | RefreshToken Entity, Repository | Backend | 1 |
| 4 | JwtService (토큰 생성/검증) | Backend | - |
| 5 | GoogleOAuthService (Google API 통신) | Backend | - |
| 6 | AuthService (로그인/갱신/로그아웃) | Backend | 2,3,4,5 |
| 7 | JwtAuthenticationFilter | Backend | 4 |
| 8 | SecurityConfig | Backend | 7 |
| 9 | AuthController | Backend | 6,8 |
| 10 | Zustand authStore | Frontend | - |
| 11 | API 클라이언트 + interceptor | Frontend | 10 |
| 12 | AuthProvider | Frontend | 11 |
| 13 | OAuth 콜백 페이지 | Frontend | 11 |
| 14 | GoogleLoginButton + 로그인 페이지 | Frontend | 13 |
| 15 | ProtectedRoute / PublicRoute | Frontend | 12 |
| 16 | Rate Limiting | Backend | 9 |
| 17 | 만료 토큰 정리 스케줄러 | Backend | 3 |

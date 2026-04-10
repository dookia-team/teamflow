# Sprint 1 — 진입 플로우 (인증 + 랜딩/허브)

> **기간:** 2주 (Week 1~2)  
> **목표:** Google 로그인 → 워크스페이스 생성/초대 → 프로젝트 허브 진입 전체 플로우 완성

---

## Phase 1: 기반 구축 (Week 1 전반)

| 태스크 | 요구사항 ID | 담당 | TDD |
|--------|------------|------|-----|
| DB 스키마 설계 + 마이그레이션 (users, refresh_tokens, workspaces, workspace_members, projects) | REQ-AUTH-001~006, REQ-WS-001~003, REQ-LAND-003 | Backend | - |
| Spring Boot 프로젝트 초기 설정 (Security, JPA, Redis) | 전체 | Backend | - |
| User Entity + Repository | REQ-AUTH-006 | Backend | true |
| RefreshToken Entity + Repository | REQ-AUTH-003 | Backend | true |
| React 프로젝트 FSD 디렉토리 구조 잡기 | 전체 | Frontend | - |

## Phase 2: 인증 구현 (Week 1 후반)

| 태스크 | 요구사항 ID | 담당 | TDD |
|--------|------------|------|-----|
| JwtService (토큰 생성/검증) | REQ-AUTH-002, 004 | Backend | true |
| GoogleOAuthService (Google API 통신) | REQ-AUTH-001 | Backend | true |
| AuthService (로그인/갱신/로그아웃) | REQ-AUTH-001~006 | Backend | true |
| JwtAuthenticationFilter + SecurityConfig | REQ-AUTH-002 | Backend | true |
| AuthController (3개 엔드포인트) | REQ-AUTH-001~005 | Backend | true |
| Zustand authStore + API 클라이언트 (interceptor) | REQ-AUTH-002, 004 | Frontend | - |
| AuthProvider (새로고침 시 인증 복원) | REQ-AUTH-004 | Frontend | - |
| OAuth 콜백 페이지 | REQ-AUTH-001 | Frontend | - |
| GoogleLoginButton + 로그인 페이지 | REQ-AUTH-001, REQ-LAND-001 | Frontend | - |
| ProtectedRoute / PublicRoute | REQ-AUTH-002 | Frontend | - |

## Phase 3: 워크스페이스 + 프로젝트 허브 (Week 2)

| 태스크 | 요구사항 ID | 담당 | TDD |
|--------|------------|------|-----|
| Workspace Entity + Repository | REQ-WS-001 | Backend | true |
| WorkspaceMember Entity + Repository | REQ-WS-001~003 | Backend | true |
| Project Entity + Repository | REQ-LAND-003 | Backend | true |
| WorkspaceService (생성, 초대, 멤버 관리) | REQ-WS-001~003 | Backend | true |
| ProjectService (CRUD) | REQ-LAND-003, 004 | Backend | true |
| WorkspaceController + ProjectController | REQ-WS-001~003, REQ-LAND-003~004 | Backend | true |
| 랜딩 페이지 UI | REQ-LAND-001 | Frontend | - |
| 프로젝트 허브 UI (카드 목록 + 생성) | REQ-LAND-002, 003 | Frontend | - |
| 프로젝트 선택 → 워크스페이스 진입 | REQ-LAND-004 | Frontend | - |
| 워크스페이스 생성 모달 + 초대 UI | REQ-WS-001~003 | Frontend | - |

## Phase 4: 통합 + 검증 (Week 2 후반)

| 태스크 | 요구사항 ID | 담당 | TDD |
|--------|------------|------|-----|
| 프론트-백 통합 테스트 (로그인 → 허브 E2E) | 전체 | 공용 | - |
| Rate Limiting 설정 (Redis) | REQ-AUTH-001~004 | Backend | - |
| 만료 토큰 정리 스케줄러 | REQ-AUTH-003 | Backend | - |
| 버그 수정 + 코드 리뷰 | 전체 | 공용 | - |

---

## 병렬 작업 가능 영역

```
Backend (친구)              Frontend (나)
─────────────────           ─────────────────
Week 1: DB + Spring 셋업    Week 1: FSD 구조 + Auth UI
        Auth API 구현                Zustand + Interceptor
                                     로그인/콜백 페이지

Week 2: Workspace/Project   Week 2: 허브 UI + 워크스페이스 UI
        API 구현                     프론트-백 통합
```

# TeamFlow Sprint 1 — PRD (제품 요구사항 문서)

> **Version:** 1.0  
> **Date:** 2026-04-10  
> **Sprint:** 1 (Week 1~2)  
> **Status:** Draft

---

## 1. 개요

### 1.1 목적
TeamFlow의 **진입 플로우**를 구축한다. 사용자가 Google 계정으로 로그인하고, 워크스페이스를 생성/초대하며, 프로젝트 허브까지 진입할 수 있는 전체 경로를 완성한다.

### 1.2 대상 사용자
- 소규모 팀(2~15명) 리더 또는 멤버
- 사이드 프로젝트 참여자

### 1.3 성공 기준
- Google OAuth 로그인 → 프로젝트 허브 진입까지 전체 플로우 동작
- 워크스페이스 생성 및 멤버 초대(이메일/링크) 정상 동작
- 프로젝트 생성 후 카드 클릭으로 워크스페이스 진입 가능

### 1.4 비목표 (Non-Goals)
- 이슈 관리(칸반 보드), 문서 편집기, 채팅 기능 — Sprint 2~3 범위
- 프로필 관리(AUTH-02), 역할 관리(AUTH-05) — Sprint 4+ (P1)
- 프로젝트 설정(LAND-05) — Sprint 4+ (P1)
- 배포/인프라 구축
- 다크모드, 반응형 디자인

---

## 2. 요구사항

### 2.1 인증 (Auth)

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| REQ-AUTH-001 | Google OAuth 로그인 | P0 | Google OAuth 2.0 Authorization Code Flow로 회원가입/로그인 통합 처리 |
| REQ-AUTH-002 | JWT Access Token 발급 | P0 | 로그인 성공 시 JWT(15분) 발급, 프론트 메모리 저장 |
| REQ-AUTH-003 | Refresh Token 관리 | P0 | httpOnly Cookie로 Refresh Token(7일) 발급, Token Rotation 적용 |
| REQ-AUTH-004 | 토큰 갱신 | P0 | Access Token 만료 시 Refresh Token으로 자동 갱신 |
| REQ-AUTH-005 | 로그아웃 | P0 | Refresh Token 삭제 + Cookie 제거 |
| REQ-AUTH-006 | 신규 사용자 자동 가입 | P0 | google_id 미존재 시 users 레코드 자동 생성 |

### 2.2 워크스페이스 (Workspace)

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| REQ-WS-001 | 워크스페이스 생성 | P0 | 이름, slug 설정 후 워크스페이스 생성. 생성자 = Admin |
| REQ-WS-002 | 멤버 초대 (이메일) | P0 | 이메일로 초대 → 가입된 사용자는 즉시 추가, 미가입은 가입 후 자동 추가 |
| REQ-WS-003 | 멤버 초대 (링크) | P0 | 초대 링크 생성 → 링크 클릭 시 워크스페이스에 자동 합류 |

### 2.3 랜딩 / 프로젝트 허브 (Landing & Hub)

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| REQ-LAND-001 | 랜딩 페이지 | P0 | 서비스 소개 + "Google로 시작하기" CTA 버튼 |
| REQ-LAND-002 | 프로젝트 허브 | P0 | 로그인 후 참여 중인 워크스페이스/프로젝트 카드 목록 표시 |
| REQ-LAND-003 | 프로젝트 생성 | P0 | 워크스페이스 내 프로젝트 생성 (이름, 설명, 키) |
| REQ-LAND-004 | 프로젝트 선택 | P0 | 카드 클릭 → 해당 프로젝트 워크스페이스로 진입 |

---

## 3. 기술 제약사항

- Frontend: React 19 + TypeScript + Tailwind CSS 4 (FSD 아키텍처)
- Backend: Spring Boot (Java) — 레이어드 아키텍처
- Database: PostgreSQL (users, refresh_tokens, workspaces, workspace_members, projects)
- Cache: Redis (세션, Rate Limiting)
- 인증 상세: `docs/planning/auth-design.md` 참조

---

## 4. 의존성

| 의존 항목 | 상태 | 비고 |
|-----------|------|------|
| Google Cloud Console OAuth 클라이언트 설정 | 미완료 | client_id, client_secret 발급 필요 |
| PostgreSQL 인스턴스 | 미완료 | 로컬 Docker 또는 클라우드 |
| Redis 인스턴스 | 미완료 | 로컬 Docker |

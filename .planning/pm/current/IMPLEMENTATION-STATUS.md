# Sprint 1 — 구현 현황

> **기준 문서:** HANDOFF.md, PRD.md, STORIES.md
> **최종 갱신:** 2026-04-14
> **상태 범례:** ✅ 완료 · ⚠️ 부분 구현 · ❌ 미구현 · 🔄 변경됨

---

## 1. 요구사항별 구현 상태

### 인증 (REQ-AUTH)

| REQ-ID | 요구사항 | Backend | Frontend | 비고 |
|--------|---------|---------|----------|------|
| REQ-AUTH-001 | Google OAuth 로그인 | ✅ | ✅ | Authorization Code Flow 완전 구현 |
| REQ-AUTH-002 | JWT Access Token (15분) | ✅ | ✅ | HS256, 메모리 저장 (Zustand) |
| REQ-AUTH-003 | Refresh Token (7일, httpOnly Cookie) | ✅ | ✅ | Token Rotation + Replay Detection |
| REQ-AUTH-004 | 토큰 자동 갱신 | ✅ | ✅ | axios interceptor + 401 retry |
| REQ-AUTH-005 | 로그아웃 | ✅ | ✅ | POST /api/auth/logout + Cookie 삭제 |
| REQ-AUTH-006 | 신규 사용자 자동 가입 | ✅ | ✅ | isNewUser 플래그 → 워크스페이스 모달 트리거 |

### 워크스페이스 (REQ-WS)

| REQ-ID | 요구사항 | Backend | Frontend | 비고 |
|--------|---------|---------|----------|------|
| REQ-WS-001 | 워크스페이스 생성 | ✅ | ✅ | WorkspaceModal 구현 완료 |
| REQ-WS-002 | 멤버 초대 (이메일) | ⚠️ | ⚠️ | 아래 변경점 #3 참조 |
| REQ-WS-003 | 초대 링크 합류 | ⚠️ | ❌ | 아래 변경점 #2 참조 |

### 랜딩/허브 (REQ-LAND)

| REQ-ID | 요구사항 | Backend | Frontend | 비고 |
|--------|---------|---------|----------|------|
| REQ-LAND-001 | 랜딩 페이지 | — | ✅ | 2컬럼 + 앱 프리뷰 + CTA |
| REQ-LAND-002 | 프로젝트 허브 (목록) | ✅ | ✅ | 3열 카드 그리드 |
| REQ-LAND-003 | 프로젝트 생성 | ✅ | ⚠️ | API 완료, 프론트 모달 미구현 (워크스페이스 모달만 있음) |
| REQ-LAND-004 | 프로젝트 선택 → 진입 | ✅ | ⚠️ | API 완료, 프론트 라우팅 미연결 |

---

## 2. API 계약 — HANDOFF 대비 변경점

### 변경점 #1: GET /api/workspaces/{no} 응답 구조

```
HANDOFF 계약:
  members: [{ no, user: { no, email, name, picture }, role }]

실제 구현:
  members: [{ no, userNo, role, joinDate }]
```

**사유:** 중첩 user 객체 대신 userNo 참조 방식 채택. N+1 방지 및 응답 경량화.
**프론트 영향:** 멤버 목록에서 유저 상세 정보가 필요한 경우 별도 API 호출 또는 batch fetch 필요.
**결정:** Sprint 1에서는 userNo만 사용. 유저 상세가 필요한 UI는 Sprint 2에서 대응.

---

### 변경점 #2: 초대 수락 엔드포인트 미구현

```
HANDOFF 계약:
  초대 링크 클릭 → 워크스페이스 자동 합류

실제 구현:
  POST /api/workspaces/{no}/invite → 토큰 생성만
  수락 엔드포인트 (POST /api/workspaces/{no}/invitations/{token}/accept) 없음
```

**사유:** Sprint 1 범위 외. PRD 기준 Sprint 1은 "워크스페이스 생성 + 초대 발송"까지. 수락/합류 플로우는 Sprint 2 범위.
**프론트 영향:** 초대 수락 UI/플로우는 Sprint 2에서 구현.
**결정:** Sprint 2 구현 확정. REQ-WS-003은 Sprint 1에서는 토큰 생성(Backend ⚠️)까지만.

---

### 변경점 #3: 이메일 기반 초대 → userNo 기반 초대

```
HANDOFF 계약:
  POST /api/workspaces/{no}/invite
  Request: { email: "team@example.com" }

실제 구현:
  Request: { inviteeUserNo: 123, role: "MEMBER" }
```

**사유:** Sprint 1에서는 이메일 서비스 인프라가 없으므로 가입된 유저를 userNo로 직접 초대하는 것이 정상 구현. 이메일 기반 초대(미가입 유저 포함)는 이메일 발송 인프라 구축 후 Sprint 2에서 확장.
**프론트 영향:** WorkspaceModal의 이메일 입력은 Sprint 2에서 이메일 검색 API와 연결. Sprint 1에서는 초대 기능 UI만 존재.
**결정:** 현재 userNo 기반 구현이 Sprint 1 정상 범위. 이메일 초대 확장은 Sprint 2.

---

## 3. DB 스키마 — HANDOFF 대비 상태

| 테이블 | HANDOFF 대비 | 비고 |
|--------|-------------|------|
| users | ✅ 일치 | PK: bigint no, provider enum 추가 |
| refresh_tokens | ✅ 일치 | RDB 구현 (Sprint 1 범위) |
| workspaces | ✅ 일치 | |
| workspace_members | ✅ 일치 | |
| workspace_invitations | ✅ 일치 | inviter_user_no 컬럼 추가 (ERD v0.1 반영) |
| projects | ✅ 일치 | icon, color, visibility, ticket_counter 포함 |
| project_members | ✅ 일치 | OWNER/MEMBER/VIEWER 역할 |

**PK 전략:** UUID → bigint no (AUTO_INCREMENT) 변경 — ERD v0.1에서 확정.
**시간 컬럼:** create_date / update_date / delete_date 네이밍 통일.

---

## 4. 추가 구현 (HANDOFF에 없던 것)

| 항목 | 설명 | 파일 |
|------|------|------|
| EntityNotFoundException | 공통 예외 클래스 | common/exception/ |
| GlobalExceptionHandler | 전역 에러 핸들링 | exception/ |
| WorkspaceAccessDeniedException | 워크스페이스 접근 권한 예외 | workspace/exception/ |
| ProjectVisibility enum | PUBLIC/PRIVATE | project/entity/ |
| ProjectStatus enum | ACTIVE/COMPLETED | project/entity/ |
| UserProvider enum | GOOGLE (확장 가능) | user/entity/ |
| ticket_counter (Project) | 티켓 자동 번호 매김 | project/entity/ |

---

## 5. Sprint 2 이관 항목

| 항목 | 요구사항 | 사유 |
|------|---------|------|
| 초대 수락 엔드포인트 | REQ-WS-003 | 초대 플로우 전체 구현 필요 |
| 이메일 기반 초대 | REQ-WS-002 | 이메일 검색/알림 인프라 필요 |
| 프로젝트 생성 모달 (프론트) | REQ-LAND-003 | 워크스페이스 모달만 Sprint 1 범위 |
| 프로젝트 진입 라우팅 (프론트) | REQ-LAND-004 | 워크스페이스 내 프로젝트 뷰 미구현 |
| GET /workspaces/{no} 유저 상세 | 변경점 #1 | 멤버 상세 UI Sprint 2 |

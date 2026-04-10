# TeamFlow - 요구사항 명세서 (SRS)

> **Version:** 1.1  
> **Date:** 2026-04-10  
> **Status:** Draft - 피드백 반영 중

---

## 1. 프로젝트 개요

### 1.1 목적
Notion(문서) + Jira(이슈 관리) + Slack(실시간 소통)의 핵심 기능을 **하나의 웹 애플리케이션**으로 통합하여, 팀 협업 시 발생하는 컨텍스트 스위칭을 최소화한다.

### 1.2 대상 사용자
- 소규모 팀 (2~15명)
- 사이드 프로젝트 / 스타트업 초기 팀
- 별도 도구 구독 비용을 줄이고 싶은 팀

### 1.3 핵심 가치
| 가치 | 설명 |
|------|------|
| **통합** | 문서, 이슈, 채팅을 한 화면에서 접근 |
| **실시간** | 모든 변경사항이 실시간으로 동기화 |
| **단순함** | 복잡한 설정 없이 바로 사용 가능 |

---

## 2. 시스템 아키텍처 (안)

```
┌─────────────┐     ┌─────────────────────────────────┐
│   Client     │     │           Backend               │
│  (React +    │◄───►│  REST API (Spring Boot)         │
│   TypeScript)│     │  WebSocket (STOMP)              │
└─────────────┘     │  Auth (OAuth 2.0)               │
                     └──────────┬──────────────────────┘
                                │
                     ┌──────────▼──────────────────────┐
                     │        Database / Storage        │
                     │  PostgreSQL  │  Redis  │  S3     │
                     └─────────────────────────────────┘
```

### 2.1 기술 스택 (안)

| 영역 | 기술 | 비고 |
|------|------|------|
| Frontend | React + TypeScript + Tailwind CSS | SPA |
| State | Zustand 또는 Redux Toolkit | 전역 상태 관리 |
| Backend | Spring Boot (Java) | REST API |
| Realtime | WebSocket (Spring WebSocket + STOMP) | 채팅 + 실시간 동기화 |
| Database | PostgreSQL | 메인 DB |
| Cache | Redis | 세션, 실시간 Presence |
| Storage | AWS S3 또는 Cloudflare R2 | 파일 업로드 |
| Auth | OAuth 2.0 (Google) + JWT | 인증/인가 |
| Deploy | AWS ECS 또는 Vercel + Railway / Docker | 협의 필요 |

---

## 3. 유저 플로우

### 3.1 전체 흐름

```
[메인 페이지 (랜딩)]
        │
        ▼
[로그인 / 회원가입]  ←── Google OAuth
        │
        ▼
[프로젝트 허브]  ←── 프로젝트 생성 or 기존 프로젝트 선택
        │
        ▼
[프로젝트 워크스페이스]
  ┌─────────────────────────────────────────┐
  │  Sidebar        Main Content            │
  │  ┌───────┐  ┌─────────────────────┐     │
  │  │Notion │  │                     │     │
  │  │Jira   │  │  선택한 탭의 콘텐츠   │     │
  │  │Slack  │  │                     │     │
  │  └───────┘  └─────────────────────┘     │
  └─────────────────────────────────────────┘
```

### 3.2 화면별 상세

| 순서 | 화면 | 설명 |
|------|------|------|
| 1 | **랜딩 페이지** | 서비스 소개 + 로그인/회원가입 CTA 버튼 |
| 2 | **로그인** | Google OAuth 소셜 로그인 (추후 GitHub 등 확장 가능) |
| 3 | **프로젝트 허브** | 참여 중인 프로젝트 목록 카드 + "새 프로젝트 만들기" 버튼 |
| 4 | **프로젝트 워크스페이스** | 좌측 사이드바에서 Notion / Jira / Slack 탭 전환, 메인 영역에 해당 콘텐츠 표시 |

### 3.3 사이드바 탭 구조

| 탭 | 아이콘 | 메인 콘텐츠 |
|----|--------|-------------|
| **Docs** (Notion) | 문서 아이콘 | 블록 에디터, 문서 트리 |
| **Board** (Jira) | 보드 아이콘 | 칸반 보드, 이슈 관리 |
| **Chat** (Slack) | 채팅 아이콘 | 채널 목록, 실시간 메시지 |

---

## 4. 기능 요구사항

### 4.1 랜딩 / 프로젝트 허브

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| LAND-01 | 랜딩 페이지 | P0 | 서비스 소개, 로그인/회원가입 CTA |
| LAND-02 | 프로젝트 허브 | P0 | 로그인 후 진입, 참여 중인 프로젝트 카드 목록 표시 |
| LAND-03 | 프로젝트 생성 | P0 | 프로젝트 이름, 설명, 아이콘 설정 후 생성 |
| LAND-04 | 프로젝트 선택 | P0 | 카드 클릭 시 해당 프로젝트 워크스페이스로 진입 |
| LAND-05 | 프로젝트 설정 | P1 | 프로젝트 이름/설명 수정, 멤버 관리, 삭제 |

### 4.2 인증 / 사용자 관리

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| AUTH-01 | 소셜 로그인 | P0 | Google OAuth로 회원가입/로그인 |
| AUTH-02 | 프로필 관리 | P1 | 이름, 아바타, 상태 메시지 설정 |
| AUTH-03 | 워크스페이스 생성 | P0 | 팀 단위 워크스페이스 생성 및 초대 |
| AUTH-04 | 멤버 초대 | P0 | 이메일 또는 링크로 멤버 초대 |
| AUTH-05 | 역할 관리 | P1 | Admin / Member 역할 구분 |

### 4.3 이슈 관리 (Jira 기능)

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| ISS-01 | 프로젝트 생성 | P0 | 워크스페이스 내 프로젝트 생성 |
| ISS-02 | 이슈 CRUD | P0 | 이슈 생성/조회/수정/삭제 |
| ISS-03 | 칸반 보드 | P0 | 드래그 앤 드롭으로 상태 변경 (Backlog → To Do → In Progress → Done) |
| ISS-04 | 이슈 상세 | P0 | 제목, 설명, 담당자, 우선순위, 라벨, 기한 |
| ISS-05 | 이슈 댓글 | P1 | 이슈 내 댓글 작성 및 멘션 |
| ISS-06 | 라벨/태그 | P1 | 기능, 버그, 개선 등 커스텀 라벨 |
| ISS-07 | 필터 & 검색 | P1 | 담당자, 상태, 라벨 기준 필터링 |
| ISS-08 | 리스트 뷰 | P1 | 테이블 형태의 이슈 목록 |
| ISS-09 | 타임라인 뷰 | P2 | 간트 차트 형태의 일정 시각화 |
| ISS-10 | 스프린트 관리 | P2 | 스프린트 생성, 이슈 할당, 번다운 차트 |

### 4.4 문서 관리 (Notion 기능)

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| DOC-01 | 문서 CRUD | P0 | 문서 생성/조회/수정/삭제 |
| DOC-02 | 블록 에디터 | P0 | 텍스트, 제목(H1-H3), 리스트, 체크리스트, 콜아웃, 코드 블록 |
| DOC-03 | 페이지 트리 | P0 | 사이드바에서 문서 계층 구조 탐색 |
| DOC-04 | 실시간 공동 편집 | P1 | 여러 사용자가 동시에 문서 편집 (CRDT 또는 OT 기반) |
| DOC-05 | 문서 내 멘션 | P1 | @사용자, #이슈 멘션으로 연결 |
| DOC-06 | 이미지/파일 첨부 | P1 | 문서 내 이미지 업로드 및 파일 첨부 |
| DOC-07 | 문서 히스토리 | P2 | 변경 이력 조회 및 롤백 |
| DOC-08 | 템플릿 | P2 | 회의록, 기획서 등 문서 템플릿 |
| DOC-09 | 문서 공유/권한 | P1 | 문서별 읽기/쓰기 권한 설정 |

### 4.5 채팅 / 메시징 (Slack 기능)

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| CHAT-01 | 채널 | P0 | 공개/비공개 채널 생성 및 관리 |
| CHAT-02 | 실시간 메시지 | P0 | WebSocket 기반 실시간 메시지 전송/수신 |
| CHAT-03 | DM (다이렉트 메시지) | P0 | 1:1 및 그룹 DM |
| CHAT-04 | 쓰레드 | P1 | 메시지에 대한 답글 쓰레드 |
| CHAT-05 | 리액션 | P1 | 이모지 리액션 |
| CHAT-06 | 멘션 | P1 | @사용자, @here, @channel 멘션 |
| CHAT-07 | 파일 공유 | P1 | 채팅 내 파일/이미지 첨부 |
| CHAT-08 | 메시지 검색 | P2 | 키워드로 과거 메시지 검색 |
| CHAT-09 | Presence | P1 | 온라인/자리비움/오프라인 상태 표시 |
| CHAT-10 | 채팅 패널 | P0 | 메인 컨텐츠 옆에 접었다 펼 수 있는 사이드 패널 |

### 4.6 통합 기능 (Cross-feature)

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| INT-01 | 이슈 ↔ 문서 링크 | P1 | 문서에서 이슈 참조, 이슈에서 관련 문서 연결 |
| INT-02 | 이슈 ↔ 채팅 연동 | P1 | 이슈 상태 변경 시 채팅 채널에 자동 알림 |
| INT-03 | 통합 알림 | P1 | 멘션, 이슈 할당, 댓글 등 통합 알림 센터 |
| INT-04 | 통합 검색 | P2 | 문서 + 이슈 + 채팅 메시지 통합 검색 |
| INT-05 | AI Summary | P2 | 일일 요약, 스프린트 리포트 자동 생성 |

### 4.7 대시보드

| ID | 기능 | 우선순위 | 설명 |
|----|------|----------|------|
| DASH-01 | 프로젝트 현황 | P1 | 전체 이슈 수, 완료율, 활성 멤버 수 |
| DASH-02 | 스프린트 진행률 | P1 | 현재 스프린트 프로그레스 바 |
| DASH-03 | 활동 로그 | P1 | 최근 팀 활동 타임라인 |
| DASH-04 | 벨로시티 차트 | P2 | 스프린트별 처리량 추이 |

---

## 5. 비기능 요구사항

### 5.1 성능

| ID | 요구사항 | 기준 |
|----|----------|------|
| NFR-01 | 페이지 초기 로딩 | 3초 이내 (LCP) |
| NFR-02 | API 응답 시간 | 200ms 이내 (p95) |
| NFR-03 | 채팅 메시지 전달 | 500ms 이내 (실시간) |
| NFR-04 | 동시 접속자 | MVP 기준 50명 이상 |

### 5.2 보안

| ID | 요구사항 |
|----|----------|
| NFR-05 | HTTPS 필수 적용 |
| NFR-06 | JWT 토큰 기반 인증, Refresh Token 적용 |
| NFR-07 | 워크스페이스 간 데이터 완전 격리 |
| NFR-08 | XSS / CSRF / SQL Injection 방지 |
| NFR-09 | 파일 업로드 시 타입 및 크기 제한 |

### 5.3 사용성

| ID | 요구사항 |
|----|----------|
| NFR-10 | 반응형 디자인 (데스크탑 우선, 태블릿 대응) |
| NFR-11 | 다크모드 지원 |
| NFR-12 | 키보드 단축키 지원 (이슈 생성, 검색 등) |

---

## 6. 우선순위 정의

| 등급 | 의미 | 목표 시기 |
|------|------|-----------|
| **P0** | MVP 필수 | Sprint 1~3 |
| **P1** | 핵심 사용성 | Sprint 4~6 |
| **P2** | Nice to have | Sprint 7+ |

---

## 7. 화면 구성 (와이어프레임 매핑)

### 화면 1: 랜딩 페이지
```
┌─────────────────────────────────────────┐
│  Logo              [로그인] [회원가입]    │
├─────────────────────────────────────────┤
│                                         │
│     TeamFlow                            │
│     문서 + 이슈 + 채팅, 하나로 끝.        │
│                                         │
│         [ 무료로 시작하기 ]               │
│                                         │
│     ┌─────────────────────────┐         │
│     │    서비스 스크린샷/미리보기│         │
│     └─────────────────────────┘         │
└─────────────────────────────────────────┘
```

### 화면 2: 프로젝트 허브 (로그인 후)
```
┌─────────────────────────────────────────┐
│  Logo    검색...          [프로필] [설정] │
├─────────────────────────────────────────┤
│                                         │
│  내 프로젝트                              │
│                                         │
│  ┌──────────┐ ┌──────────┐ ┌──────────┐ │
│  │ 📦       │ │ 🎨       │ │ ＋       │ │
│  │ 웹앱 MVP │ │ 디자인    │ │ 새 프로젝트│ │
│  │ 멤버 3명  │ │ 멤버 2명  │ │ 만들기    │ │
│  │ 이슈 12개 │ │ 이슈 5개  │ │          │ │
│  └──────────┘ └──────────┘ └──────────┘ │
│                                         │
│  최근 활동                                │
│  - HS가 "실시간 채팅 구현" 진행 중 (10분전)│
│  - JK가 문서 "API 문서" 수정 (1시간전)    │
└─────────────────────────────────────────┘
```

### 화면 3: 프로젝트 워크스페이스 (프로젝트 선택 후)
```
┌──────────┬──────────────────────────────┐
│          │                              │
│ Sidebar  │       Main Content           │
│          │                              │
│ [프로젝트명]│                             │
│          │                              │
│ 📄 Docs  │  선택한 탭(Docs/Board/Chat)의 │
│ ▦ Board  │  콘텐츠가 여기에 표시          │
│ 💬 Chat  │                              │
│          │                              │
│ ──────── │                              │
│ 설정      │                              │
│ 멤버      │                              │
│ [프로필]  │                              │
└──────────┴──────────────────────────────┘
```

### 탭별 메인 콘텐츠

| 탭 | 메인 영역 | 사이드바 하위 메뉴 |
|----|-----------|-------------------|
| **Docs** (Notion) | 블록 에디터 | 문서 트리, 개인 메모 |
| **Board** (Jira) | 칸반 보드 (4컬럼) | 뷰 전환, 필터, 스프린트 |
| **Chat** (Slack) | 채팅 메시지 영역 | 채널 목록, DM 목록 |

---

## 8. 데이터 모델 (초안)

### 주요 엔티티

```
Workspace
├── Members (User ↔ Workspace, role)
├── Projects
│   ├── Issues
│   │   ├── Comments
│   │   ├── Labels
│   │   └── Assignees
│   └── Sprints
├── Documents
│   ├── Blocks
│   └── Permissions
├── Channels
│   ├── Messages
│   │   ├── Reactions
│   │   └── Threads
│   └── Members
└── Notifications
```

### 핵심 테이블 요약

| 테이블 | 주요 컬럼 |
|--------|-----------|
| `users` | id, email, name, avatar_url, created_at |
| `workspaces` | id, name, slug, owner_id, created_at |
| `workspace_members` | workspace_id, user_id, role |
| `projects` | id, workspace_id, name, key (e.g. "WEB"), created_at |
| `issues` | id, project_id, title, description, status, priority, assignee_id, sprint_id, due_date |
| `documents` | id, workspace_id, parent_id, title, created_by, updated_at |
| `doc_blocks` | id, document_id, type, content, position |
| `channels` | id, workspace_id, name, is_private |
| `messages` | id, channel_id, user_id, content, thread_parent_id, created_at |
| `notifications` | id, user_id, type, reference_id, is_read, created_at |

---

## 9. API 엔드포인트 (초안)

### Auth
```
POST   /api/auth/google          # Google OAuth 콜백
POST   /api/auth/refresh          # 토큰 갱신
POST   /api/auth/logout           # 로그아웃
```

### Workspace
```
POST   /api/workspaces            # 워크스페이스 생성
GET    /api/workspaces/:id        # 워크스페이스 조회
POST   /api/workspaces/:id/invite # 멤버 초대
```

### Issues
```
GET    /api/projects/:id/issues          # 이슈 목록
POST   /api/projects/:id/issues          # 이슈 생성
PATCH  /api/issues/:id                   # 이슈 수정
DELETE /api/issues/:id                   # 이슈 삭제
PATCH  /api/issues/:id/status            # 상태 변경 (드래그 앤 드롭)
POST   /api/issues/:id/comments          # 댓글 작성
```

### Documents
```
GET    /api/workspaces/:id/documents     # 문서 목록 (트리)
POST   /api/documents                    # 문서 생성
GET    /api/documents/:id                # 문서 조회 (블록 포함)
PATCH  /api/documents/:id                # 문서 수정
DELETE /api/documents/:id                # 문서 삭제
```

### Chat
```
GET    /api/workspaces/:id/channels      # 채널 목록
POST   /api/channels                     # 채널 생성
GET    /api/channels/:id/messages        # 메시지 목록 (페이지네이션)
```

### WebSocket Events
```
chat:message        # 메시지 전송/수신
chat:typing         # 타이핑 표시
chat:presence       # 온라인 상태
issue:updated       # 이슈 변경 실시간 반영
doc:updated         # 문서 실시간 동기화
```

---

## 10. MVP 개발 로드맵

### Sprint 1 (Week 1~2): 기반 구축
- [ ] 프로젝트 초기 세팅 (모노레포 or 프론트/백 분리)
- [ ] DB 스키마 설계 및 마이그레이션
- [ ] Google OAuth 인증 구현
- [ ] 워크스페이스 생성/초대 기능

### Sprint 2 (Week 3~4): 이슈 관리
- [ ] 이슈 CRUD API + UI
- [ ] 칸반 보드 (드래그 앤 드롭)
- [ ] 라벨, 담당자, 우선순위 설정

### Sprint 3 (Week 5~6): 문서 + 채팅
- [ ] 블록 기반 문서 에디터
- [ ] 실시간 채팅 (채널 + DM)
- [ ] WebSocket 연결 및 메시지 전송

### Sprint 4 (Week 7~8): 통합 + 대시보드
- [ ] 이슈 ↔ 문서 ↔ 채팅 연동
- [ ] 알림 시스템
- [ ] 대시보드 (프로젝트 현황)
- [ ] QA 및 버그 수정

---

## 11. 역할 분담 (예시)

| 영역 | 담당 | 비고 |
|------|------|------|
| Frontend 전체 | 협의 필요 | React + Tailwind |
| Backend API | 협의 필요 | Spring Boot + PostgreSQL |
| 실시간 (WebSocket) | 협의 필요 | Spring WebSocket + STOMP |
| 인프라/배포 | 협의 필요 | AWS or Vercel |
| 디자인/UI | 협의 필요 | Figma |

---

## 12. 오픈 이슈 / 논의 사항

- [ ] 모노레포(Turborepo) vs 프론트/백 별도 레포?
- [ ] 실시간 공동 편집 라이브러리 선택 (Yjs vs Automerge?)
- [ ] 모바일 대응 범위 (반응형 웹 vs 네이티브 앱?)
- [ ] 파일 스토리지 (AWS S3 vs Cloudflare R2?)
- [ ] 배포 환경 (AWS ECS vs Vercel+Railway?)
- [ ] AI 기능 범위 (MVP에 포함할지?)

---

> **다음 단계:** 이 문서를 기반으로 피드백 반영 후, 상세 설계 및 개발 착수

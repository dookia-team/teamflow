# Sprint 2 — 엔지니어링 핸드오프

---

## 1. 기술 제약사항

### Backend (Spring Boot)
- ERD v0.1 네이밍 컨벤션 유지 (PK: `no`, FK: `{테이블}_no`, 시간: `create_date`)
- Issue 엔티티에 `position` 필드 → 칸반 보드 내 순서 관리
- `ticket_counter`는 Project 엔티티에 이미 존재 → 이슈 생성 시 atomic increment
- 이슈 상태 enum: `BACKLOG`, `TODO`, `IN_PROGRESS`, `DONE`
- 이슈 우선순위 enum: `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`

### Frontend (React + FSD)
- 드래그 앤 드롭: `@hello-pangea/dnd` 또는 `dnd-kit` 사용
- 워크스페이스 레이아웃: 사이드바(240px 고정) + 메인 콘텐츠(flex-1)
- 이슈 상태 관리: `@tanstack/react-query` (서버 상태 캐시 + 낙관적 업데이트)
- 이슈 상세: 사이드 패널 (오른쪽에서 슬라이드인)

---

## 2. API 계약 (Backend ↔ Frontend)

### 이슈 API

```
POST   /api/projects/{projectNo}/issues          → { no, issueKey, title, ... }
GET    /api/projects/{projectNo}/issues          → [{ no, issueKey, title, status, priority, assigneeNo, dueDate, position }]
GET    /api/issues/{issueNo}                      → { no, issueKey, title, description, status, priority, assigneeNo, dueDate, position }
PATCH  /api/issues/{issueNo}                      → { no, ... } (부분 수정)
DELETE /api/issues/{issueNo}                      → 204 No Content
PATCH  /api/issues/{issueNo}/status               → { no, status } (상태 변경)
PATCH  /api/issues/{issueNo}/position             → { no, position } (순서 변경)
```

### 이슈 생성 요청/응답

```json
// POST /api/projects/{projectNo}/issues
// Request
{
  "title": "로그인 화면 구현",
  "description": "Google OAuth 로그인 화면을 디자인 시스템에 맞게 구현",
  "status": "BACKLOG",
  "priority": "HIGH",
  "assigneeNo": 1,
  "dueDate": "2026-04-25"
}

// Response 201
{
  "success": true,
  "data": {
    "no": 1,
    "issueKey": "TF-1",
    "title": "로그인 화면 구현",
    "description": "...",
    "status": "BACKLOG",
    "priority": "HIGH",
    "assigneeNo": 1,
    "dueDate": "2026-04-25",
    "position": 0
  }
}
```

### 이슈 상태 변경 (드래그 앤 드롭)

```json
// PATCH /api/issues/{issueNo}/status
// Request
{ "status": "IN_PROGRESS" }

// Response 200
{ "success": true, "data": { "no": 1, "status": "IN_PROGRESS" } }
```

### 에러 응답

| HTTP | 상황 |
|------|------|
| 400 | Validation (제목 길이, 잘못된 status/priority enum) |
| 403 | 프로젝트 멤버가 아님 |
| 404 | 이슈 또는 프로젝트 없음 |

---

## 3. DB 스키마 (Sprint 2 추가 — V2 마이그레이션)

> 테이블명은 V1 컨벤션(단수형: `user`, `project`, `workspace`)에 맞춰 `issue` 로 통일했다.
> 이슈 삭제는 soft delete 로 확정(RISK-IMPACT 2026-04-20) — `delete_date IS NULL` 필터링.

```sql
-- backend/src/main/resources/db/migration/V2__sprint2_issue_schema.sql
CREATE TABLE issue (
    no              BIGSERIAL    PRIMARY KEY,
    project_no      BIGINT       NOT NULL REFERENCES project(no) ON DELETE CASCADE,
    issue_key       VARCHAR(20)  NOT NULL,                     -- "TF-1", "TF-2"
    title           VARCHAR(200) NOT NULL,
    description     TEXT             NULL,
    status          VARCHAR(15)  NOT NULL DEFAULT 'BACKLOG',   -- BACKLOG|TODO|IN_PROGRESS|DONE
    priority        VARCHAR(10)  NOT NULL DEFAULT 'MEDIUM',    -- LOW|MEDIUM|HIGH|CRITICAL
    assignee_no     BIGINT           NULL REFERENCES "user"(no) ON DELETE SET NULL,
    position        INT          NOT NULL DEFAULT 0,           -- 같은 status 컬럼 내 오름차순 정렬
    due_date        DATE             NULL,
    create_date     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    update_date     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    delete_date     TIMESTAMPTZ      NULL,                     -- soft delete 마커
    CONSTRAINT uk_issue_key UNIQUE (project_no, issue_key)
);

CREATE INDEX idx_issue_project_status ON issue(project_no, status);
CREATE INDEX idx_issue_assignee       ON issue(assignee_no);
CREATE INDEX idx_issue_project_active ON issue(project_no) WHERE delete_date IS NULL;
```

---

## 4. 테스트 전략

| 영역 | 방식 | 범위 |
|------|------|------|
| Backend Service | @ExtendWith(MockitoExtension.class) | IssueService |
| Backend Controller | @WebMvcTest | IssueController |
| Backend Repository | @DataJpaTest | 커스텀 쿼리 (상태별 조회 등) |
| Frontend | Vitest + RTL | 이슈 카드, 칸반 보드 컴포넌트 |
| Frontend | 수동 E2E | 이슈 생성 → 보드 확인 → DnD → 상세 편집 |

---

## 5. Enum 정의

### IssueStatus

| 값 | 의미 | 칸반 컬럼 |
|----|------|----------|
| `BACKLOG` | 백로그 (미착수) | 1번째 |
| `TODO` | 할 일 (예정) | 2번째 |
| `IN_PROGRESS` | 진행 중 | 3번째 |
| `DONE` | 완료 | 4번째 |

### IssuePriority

| 값 | 의미 | 아이콘 |
|----|------|--------|
| `LOW` | 낮음 | 🟢 |
| `MEDIUM` | 보통 | 🟡 |
| `HIGH` | 높음 | 🟠 |
| `CRITICAL` | 긴급 | 🔴 |

---

## 6. 백엔드 구현 정렬 노트 (Sprint 2)

> 계약 문서와 실제 구현 간 결정 사항을 기록한다. 차이가 있는 항목은 실제 구현이 기준.

### 6.1 테이블/엔티티 명명
- 본 문서 초안의 `issues` / `projects` / `users` 복수형 → 실제 구현은 V1 컨벤션과 일치하는 **단수형** 사용 (`issue`, `project`, `"user"`).
- FK: `project_no`, `assignee_no` → `"user"(no)` — `user` 는 PostgreSQL 예약어이므로 따옴표 필수.

### 6.2 Soft delete 확정
- RISK-IMPACT 2026-04-20 결정. `delete_date TIMESTAMPTZ NULL` 컬럼으로 구현.
- Repository: `findByNoAndDeleteDateIsNull`, `findAllByProjectNoAndDeleteDateIsNullOrderByPositionAsc`.
- `DELETE /api/issues/{issueNo}` 는 204 + soft delete (`issue.softDelete()`).

### 6.3 Controller 응답 계약
- CRUD 엔드포인트 4개(POST/GET list/GET detail/PATCH) → `ApiResponse<IssueDto.Response>` 전체 반환.
- 칸반 전용 2개:
  - `PATCH /api/issues/{issueNo}/status` → `ApiResponse<IssueDto.StatusResponse>` (`{ no, status }` 슬림)
  - `PATCH /api/issues/{issueNo}/position` → `ApiResponse<IssueDto.PositionResponse>` (`{ no, position }` 슬림)
- DELETE 만 `@ResponseStatus(HttpStatus.NO_CONTENT)` + `void`.

### 6.4 권한 모델
- 프로젝트 멤버십이 아닌 **워크스페이스 멤버십**으로 일원화 (`requireWorkspaceMember` 헬퍼).
- 이슈 → `project.workspaceNo` → `workspace_member.user_no` 체인으로 판정.
- 비멤버는 `WorkspaceAccessDeniedException` → 403.

### 6.5 이슈 키 발급
- `Project.nextTicketNumber()` 가 `ticket_counter` 를 원자 증가 → `{project.key}-{counter}` 로 조립.
- 동시성 높은 환경에서는 DB `uk_issue_key` UNIQUE 제약으로 최후 방어. 대량 동시 생성 시나리오에 한해 PESSIMISTIC_WRITE 또는 DB 시퀀스 전환 검토 (향후 스프린트).

### 6.6 테스트 커버리지
| 레이어 | 케이스 |
|--------|--------|
| `IssueRepositoryTest` (@DataJpaTest) | 6 |
| `IssueServiceTest` (@ExtendWith MockitoExtension) | 14 |
| `IssueControllerTest` (@WebMvcTest) | 18 |

### 6.7 Sprint 2 Phase 3+ 이월
- 프론트엔드 통합(React + FSD, dnd-kit / @hello-pangea/dnd 중 택1), 워크스페이스 레이아웃, 이슈 상세 패널은 Sprint.md Phase 3~4 그대로 진행.
- 다중 이슈 position 재배치(rebalancing), 동시 드래그 정렬 보정은 본 스프린트 범위 외.

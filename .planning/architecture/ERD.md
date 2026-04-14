# TeamFlow ERD v0.1

> **목적:** Notion(문서) + Slack(채팅·Git 알림) + Jira(티켓·스프린트) 통합 워크플로우
> **대상 독자:** Backend / Frontend 공통
> **범위:** Sprint 01 이후 전체 도메인 큰 그림 (Sprint 01 구현 범위는 §9 참조)

---

## 1. 도메인 레이어 개요

```
Identity ─── Workspace ─── Project ─┬── Page (Notion) ── Block (블록 단위 콘텐츠 엔티티)
                                     ├── Channel + Message (Slack)
                                     │     └── GitIntegration (알림 소스)
                                     └── Sprint + Ticket (Jira)
                                           └── Workflow / Board / Component / Version
                                           └── CustomField / Link / Watcher / Vote / Worklog
Dashboard / SavedFilter / Notification (cross-cutting)
```

---

## 2. Mermaid ERD

> **명명 규칙**
> - 모든 테이블 PK는 `no` (bigint, auto increment)
> - FK는 참조 대상 테이블명 + `_no` (예: `user_no`, `workspace_no`)
> - 같은 테이블 내에서 USER를 여러 역할로 참조하는 경우 역할 + `_user_no` 형태로 명시 (예: `author_user_no`, `assignee_user_no`, `reporter_user_no`, `inviter_user_no`, `installed_user_no`)
> - `USER` 테이블은 `no`(PK)와 별개로 `user_id`(로그인 식별자)를 보유
>
> **NULL 정책 (필수/선택 표기)**
> - **기본값은 NOT NULL.** 주석에 `nullable` 이 명시된 컬럼만 NULL 허용.
> - PK / FK (단, 주석에 `nullable` 표시된 관계 제외) / UK / 생성·수정 타임스탬프는 원칙적으로 NOT NULL.
> - `nullable` 표기가 있는 경우 그 옆에 "어떤 상황에 NULL인지" 간단히 부연함.
>
> **티켓 키 자동 증가**
> - `PROJECT.ticket_counter`가 프로젝트별 티켓 시퀀스 저장. 티켓 생성 시 원자적 `UPDATE ... SET ticket_counter = ticket_counter + 1 RETURNING` 으로 +1 후, `TICKET.ticket_key = PROJECT.key + '-' + ticket_counter` 형태로 조립 (예: `TF-1`, `TF-2`, ...).
> - 프로젝트 간 번호는 독립. 삭제된 티켓 번호는 재사용하지 않음.
>
> **`position` 컬럼 정책 (확장성 확보)**
> - `position` 값 자체를 **API 요청/응답에 노출하지 않는다.** 클라이언트는 `{ beforeNo, afterNo }` 또는 `{ targetNo, direction }` 형태로만 재정렬을 요청.
> - 서버(서비스 레이어)가 해당 컬럼 타입에 맞게 실제 값을 계산해 저장 — 클라이언트는 값 생성 로직을 모른다.
> - 현재 컬럼 타입:
>   - `BLOCK.position`: **string (lexorank)** — 드래그가 매우 빈번한 문서 블록
>   - 나머지(`PAGE.position`, `TICKET.position`, `BOARD_COLUMN.position`, `CUSTOM_FIELD.position`, `CUSTOM_FIELD_OPTION.position`, `WORKFLOW_STATUS.position`, `DASHBOARD_GADGET.position`, `DASHBOARD_GADGET.column_index`): **int (sparse, 100 간격 권장)**
> - 병목 발생 시 해당 테이블만 `position_v2 string` 추가 → 백그라운드 backfill → 애플리케이션 cutover → 구 컬럼 drop 순으로 lexorank 이관. API 계약이 불변이라 클라이언트 변경 불필요.
>
> **도메인 그룹 순서** (§1 개요와 동일)
> `[1] Identity → [2] Workspace → [3] Project → [4] Page/Block → [5] Channel/Message → [6] Git Integration → [7] Sprint/Ticket → [8] Workflow → [9] Board → [10] Component → [11] Version → [12] Custom Field → [13] Ticket Link/Watcher/Vote/Worklog → [14] Dashboard → [15] Saved Filter → [16] Notification`
>
> 각 그룹은 "관계 → 엔티티 정의" 순서로 함께 배치됩니다.

```mermaid
erDiagram

    %% ============================================================
    %% [1] IDENTITY — 사용자 계정·세션·토큰
    %% ============================================================

    USER ||--o{ REFRESH_TOKEN : "owns"

    USER {
        bigint no PK
        string user_id UK "nullable — 자체 회원가입 사용자만 보유 (OAuth 전용 계정은 null)"
        string email UK "알림·비밀번호 재설정·인증 메일용 (OAuth/자체 모두 필수)"
        string name
        string picture "nullable"
        string provider "GOOGLE 등, nullable — 자체 회원가입 도입 확장성 확보"
        string provider_id "nullable — OAuth 공급자 고유 ID"
        string password_hash "nullable — 자체 회원가입 시 사용"
        datetime create_date
        datetime update_date
        datetime delete_date "nullable — soft delete"
    }

    REFRESH_TOKEN {
        bigint no PK "※ 실제 저장소는 Redis (RDB 아님). 본 스키마는 구조 명세용"
        bigint user_no FK
        string token_hash UK "해시된 리프레시 토큰 값"
        string family_id "토큰 회전 체인 식별자 — 동일 로그인 세션에서 발급된 토큰 그룹. 재사용(도난) 탐지 시 family 전체를 일괄 무효화하기 위함"
        boolean used "회전되어 소비된 토큰 여부 — 이미 used=true인 토큰이 재사용되면 탈취로 간주하고 family 전체를 폐기"
        string user_agent "발급 당시 클라이언트 UA — 디바이스/세션 구분, 이상 로그인 탐지, 세션 목록 UI 표시용"
        string ip_address "nullable — 프록시·네트워크 환경에 따라 누락 가능"
        datetime expire_date
        datetime create_date
    }

    %% ============================================================
    %% [2] WORKSPACE — 워크스페이스·멤버·초대
    %% ============================================================

    WORKSPACE ||--o{ WORKSPACE_MEMBER : "has"
    WORKSPACE ||--o{ WORKSPACE_INVITATION : "issues"
    WORKSPACE ||--o{ PROJECT : "contains"
    USER ||--o{ WORKSPACE_MEMBER : "joins"
    USER ||--o{ WORKSPACE_INVITATION : "invites"
    USER ||--o{ WORKSPACE_INVITATION : "is_invited"

    WORKSPACE {
        bigint no PK
        string name
        string slug UK "형식: slugify(name) + '-' + UUID — 이름 중복 허용 + 전역 유일성 보장"
        datetime create_date
        datetime update_date
    }

    WORKSPACE_MEMBER {
        bigint no PK
        bigint workspace_no FK
        bigint user_no FK
        string role "OWNER|MEMBER|GUEST"
        datetime join_date
    }

    WORKSPACE_INVITATION {
        bigint no PK
        bigint workspace_no FK
        bigint invitee_user_no FK "USER.no 참조 (초대 대상자) — 가입된 사용자만 초대 가능. 실제 알림은 해당 USER.email로 발송"
        bigint inviter_user_no FK "USER.no 참조 (초대자)"
        string role "MEMBER|GUEST"
        string token UK "초대 수락 링크용 단발성 토큰 — 상세 관리 규칙은 §3.1 참조"
        string status "PENDING|ACCEPTED|EXPIRED|REVOKED"
        datetime expire_date "기본 TTL 7일, 만료 시 status=EXPIRED 전환"
        datetime create_date
    }

    %% ============================================================
    %% [3] PROJECT — 프로젝트·멤버
    %% ============================================================

    PROJECT ||--o{ PROJECT_MEMBER : "has"
    USER ||--o{ PROJECT_MEMBER : "joins"

    PROJECT {
        bigint no PK
        bigint workspace_no FK
        string name
        string key "짧은 대문자 식별자 (2~10자), workspace 내 unique. 티켓 키 접두사로 사용 (예: TF → TF-1, TF-2)"
        string description "nullable"
        string icon "nullable"
        string color "nullable"
        string visibility "PUBLIC|PRIVATE — PUBLIC은 해당 워크스페이스 멤버 전체 공개, PRIVATE은 PROJECT_MEMBER만 접근"
        string status "ACTIVE|COMPLETED — ACTIVE는 진행 중 (편집 가능), COMPLETED는 완료 (읽기 전용)"
        int ticket_counter "기본 0. 티켓 생성 시 원자적 +1 후 TICKET.ticket_key 조립에 사용"
        datetime create_date
        datetime update_date
    }

    PROJECT_MEMBER {
        bigint no PK
        bigint project_no FK
        bigint user_no FK
        string role "OWNER|MEMBER|VIEWER — 소유자/관리자는 OWNER로 통일. MEMBER 단은 동일 권한"
        datetime join_date
    }

    %% ============================================================
    %% [4] PAGE (Notion) — 문서·블록
    %% ============================================================

    PROJECT ||--o{ PAGE : "contains"
    PAGE ||--o{ PAGE : "has_children"
    PAGE ||--o{ BLOCK : "has_blocks"
    BLOCK ||--o{ BLOCK : "has_children"
    BLOCK ||--o{ BLOCK : "synced_from"
    PAGE ||--o{ BLOCK : "embedded_as_sub_page"

    PAGE {
        bigint no PK "페이지 고유 번호"
        bigint project_no FK "소속 프로젝트. 페이지는 프로젝트 스코프 내에서만 존재"
        bigint parent_page_no FK "nullable — 사이드바 트리 구조의 상위 페이지. 루트 페이지면 null"
        string title "페이지 제목 (사이드바·브레드크럼에 표시)"
        string icon "nullable — 이모지 또는 이미지 URL. 사이드바/탭 아이콘으로 표시"
        string cover_url "nullable — 페이지 상단 배너 이미지 URL"
        int position "같은 parent_page_no 하위에서의 정렬 순서 (사이드바 드래그앤드롭 시 업데이트)"
        datetime create_date "페이지 최초 생성 시각"
        datetime update_date "페이지 메타데이터(제목/아이콘/커버) 최종 수정 시각. 블록 수정은 BLOCK.update_date 참조"
        datetime delete_date "nullable — 휴지통으로 이동된 페이지 (soft delete). 휴지통 화면에서 복원/영구삭제"
    }

    BLOCK {
        bigint no PK "블록 고유 번호"
        bigint page_no FK "소속 페이지. 페이지 삭제(soft delete)와 독립적으로 블록은 자체 delete_date으로 관리 가능"
        bigint parent_block_no FK "nullable — 블록 간 중첩 구조(토글 하위, 리스트 항목 하위 등). 페이지 최상위 블록이면 null"
        string type "블록 타입 enum: PARAGRAPH|HEADING|CODE|QUOTE|BULLET_LIST_ITEM|ORDERED_LIST_ITEM|TODO|TOGGLE|TABLE|TABLE_ROW|TABLE_CELL|IMAGE|VIDEO|FILE|EMBED|DIVIDER|CALLOUT|MATH|SUB_PAGE|DB_VIEW|SYNCED_BLOCK"
        jsonb attrs "nullable — 타입별 부가 속성. 예) HEADING={level:2} / CODE={language:'typescript', wrap:true} / TODO={checked:true} / TOGGLE={open:false} / CALLOUT={emoji:'💡'} / DB_VIEW={filterNo:12, layout:'table'}"
        jsonb content "nullable — 블록 내부 인라인 콘텐츠(텍스트 + 굵게/기울임/링크/멘션 같은 marks). Tiptap JSONContent[] 배열 형식. 컨테이너 전용 블록(TOGGLE/TABLE 루트 등)은 null 가능"
        bigint ref_block_no FK "nullable — SYNCED_BLOCK 원본 블록 참조. null이면 일반 독립 블록. 원본 수정 시 복제 블록에도 내용 동기화"
        bigint ref_page_no FK "nullable — SUB_PAGE 또는 페이지 링크 블록이 가리키는 PAGE.no. 본문에 하위 페이지를 인라인으로 임베드할 때 사용"
        string position "lexorank 문자열 (Jira/Figma/Notion 방식). 같은 parent_block_no 하위 형제 정렬용. 중간 삽입 시 전후 row 재번호 없이 신규 키만 생성 (예: 'a0'과 'a1' 사이에 'a0V' 삽입). `(parent_block_no, position)` 복합 인덱스 권장"
        datetime create_date "블록 최초 생성 시각"
        datetime update_date "블록 내용/속성 최종 수정 시각. 페이지 단위 '최근 편집일'은 MAX(BLOCK.update_date)로 집계"
        datetime delete_date "nullable — 블록 단위 soft delete. 페이지 휴지통과는 독립적으로 특정 블록만 되돌리기/영구삭제 가능"
    }

    %% ============================================================
    %% [5] CHANNEL / MESSAGE (Slack) — 채널·메시지
    %% ============================================================

    PROJECT ||--o{ CHANNEL : "contains"
    CHANNEL ||--o{ CHANNEL_MEMBER : "has"
    CHANNEL ||--o{ MESSAGE : "has"
    MESSAGE ||--o{ MESSAGE : "threads"
    MESSAGE ||--o{ MESSAGE_REACTION : "has"
    MESSAGE ||--o{ MESSAGE_ATTACHMENT : "has"
    USER ||--o{ CHANNEL_MEMBER : "joins"
    USER ||--o{ MESSAGE : "authors"
    USER ||--o{ MESSAGE_REACTION : "reacts"

    CHANNEL {
        bigint no PK
        bigint project_no FK
        string name "프로젝트 내 unique"
        string description "nullable"
        string type "PUBLIC|PRIVATE|DIRECT"
        boolean is_default
        datetime create_date
    }

    CHANNEL_MEMBER {
        bigint no PK
        bigint channel_no FK
        bigint user_no FK
        datetime last_read_date "nullable — 이 사용자가 해당 채널에서 마지막으로 읽은 메시지 시각. ▶ 서버: 사용자가 채널 열람(포커스)하거나 '읽음 처리' API 호출 시 `NOW()`로 갱신. ▶ 프론트 활용: (1) 안 읽은 개수 = MESSAGE.create_date > last_read_date 인 메시지 수 → 채널 목록 배지 표시 (2) 채널 진입 시 last_read_date 이후 첫 메시지 위치에 '여기부터 안 읽음' 구분선 렌더 (3) last_read_date < 최신 MESSAGE.create_date 이면 채널명 굵게 강조. ▶ null 의미: 채널 가입 후 한 번도 열람한 적 없음 → 모든 메시지가 안 읽음 상태"
        datetime join_date
    }

    MESSAGE {
        bigint no PK
        bigint channel_no FK
        bigint author_user_no FK "nullable (BOT 메시지는 null), USER.no 참조 (작성자)"
        string type "TEXT|SYSTEM|GIT_EVENT"
        text content
        bigint thread_root_no FK "nullable — 자기참조 (스레드 루트이면 null)"
        boolean is_edited
        datetime edit_date "nullable — 편집 전에는 null"
        datetime delete_date "nullable — soft delete"
        datetime create_date
    }

    MESSAGE_REACTION {
        bigint no PK
        bigint message_no FK
        bigint user_no FK
        string emoji
        datetime create_date
    }

    MESSAGE_ATTACHMENT {
        bigint no PK
        bigint message_no FK
        string file_url
        string file_name
        string mime_type
        int size_bytes
    }

    %% ============================================================
    %% [6] GIT INTEGRATION — 저장소 연동·이벤트
    %% ============================================================

    PROJECT ||--o{ GIT_INTEGRATION : "connects"
    GIT_INTEGRATION ||--o{ CHANNEL_GIT_SUBSCRIPTION : "fans_out"
    GIT_INTEGRATION ||--o{ GIT_EVENT : "receives"
    CHANNEL ||--o{ CHANNEL_GIT_SUBSCRIPTION : "subscribes"

    GIT_INTEGRATION {
        bigint no PK
        bigint project_no FK
        string provider "GITHUB|GITLAB"
        string repo_full_name "owner/repo"
        string repo_url
        string access_token_encrypted
        string webhook_secret
        bigint installed_user_no FK "USER.no 참조 (설치자) — OAuth 토큰 소유자. 토큰 만료/해제 시 이 사용자에게 재인증 요청"
        boolean active
        datetime create_date
    }

    CHANNEL_GIT_SUBSCRIPTION {
        bigint no PK
        bigint channel_no FK
        bigint integration_no FK
        jsonb event_filters "push/pr_opened/pr_merged/issue/release"
        datetime create_date
    }

    GIT_EVENT {
        bigint no PK
        bigint integration_no FK
        string event_type
        string delivery_id UK
        jsonb payload
        datetime receive_date
    }

    %% ============================================================
    %% [7] SPRINT / TICKET (Jira) — 스프린트·티켓 본체
    %% ============================================================

    PROJECT ||--o{ SPRINT : "contains"
    PROJECT ||--o{ TICKET : "contains"
    PROJECT ||--o{ TICKET_LABEL : "defines"
    SPRINT ||--o{ TICKET : "contains"
    TICKET ||--o{ TICKET : "has_subtasks"
    TICKET ||--o{ TICKET_COMMENT : "has"
    TICKET ||--o{ TICKET_ATTACHMENT : "has"
    TICKET ||--o{ TICKET_ACTIVITY : "logs"
    TICKET }o--o{ TICKET_LABEL : "tagged_with"
    USER ||--o{ TICKET : "reports"
    USER ||--o{ TICKET : "is_assigned"
    USER ||--o{ TICKET_COMMENT : "authors"
    USER ||--o{ TICKET_ACTIVITY : "acts"

    SPRINT {
        bigint no PK
        bigint project_no FK
        string name
        string goal "nullable"
        string status "PLANNED|ACTIVE|COMPLETED"
        date start_date "nullable — PLANNED 단계에서는 미지정 가능"
        date end_date "nullable — PLANNED 단계에서는 미지정 가능"
        datetime create_date
    }

    TICKET {
        bigint no PK
        bigint project_no FK
        bigint sprint_no FK "nullable — 백로그 상태면 null"
        string ticket_key UK "예: TF-123. PROJECT.key + '-' + PROJECT.ticket_counter 로 생성 시점에 자동 조립. 삭제된 번호 재사용 안 함"
        string type "EPIC|STORY|TASK|BUG|SUBTASK"
        string title
        text description "nullable — jsonb 또는 markdown"
        string status "TODO|IN_PROGRESS|IN_REVIEW|DONE|CANCELLED"
        string priority "LOW|MEDIUM|HIGH|URGENT"
        bigint assignee_user_no FK "nullable — 미배정 가능, USER.no 참조 (담당자)"
        bigint reporter_user_no FK "USER.no 참조 (보고자)"
        bigint parent_ticket_no FK "nullable — Epic의 하위 Story/Subtask 관계"
        int story_points "nullable"
        date due_date "nullable"
        int position "보드 내 순서"
        datetime create_date
        datetime update_date
        datetime delete_date "nullable — soft delete"
    }

    TICKET_LABEL {
        bigint no PK
        bigint project_no FK
        string name
        string color
    }

    TICKET_COMMENT {
        bigint no PK
        bigint ticket_no FK
        bigint author_user_no FK "USER.no 참조 (작성자)"
        text content
        datetime create_date
        datetime update_date
    }

    TICKET_ATTACHMENT {
        bigint no PK
        bigint ticket_no FK
        string file_url
        string file_name
        int size_bytes
        datetime create_date
    }

    TICKET_ACTIVITY {
        bigint no PK
        bigint ticket_no FK
        bigint actor_user_no FK "USER.no 참조 (행위자)"
        string action "CREATED|STATUS_CHANGED|ASSIGNED|COMMENTED|..."
        jsonb changes
        datetime create_date
    }

    %% ============================================================
    %% [8] WORKFLOW — 커스텀 워크플로우
    %% ============================================================

    PROJECT ||--o{ WORKFLOW : "defines"
    WORKFLOW ||--o{ WORKFLOW_STATUS : "has"
    WORKFLOW ||--o{ WORKFLOW_TRANSITION : "has"
    WORKFLOW_STATUS ||--o{ WORKFLOW_TRANSITION : "from"
    WORKFLOW_STATUS ||--o{ WORKFLOW_TRANSITION : "to"
    TICKET }o--|| WORKFLOW_STATUS : "current_status"

    WORKFLOW {
        bigint no PK
        bigint project_no FK
        string name
        boolean is_default
        datetime create_date
        datetime update_date
    }

    WORKFLOW_STATUS {
        bigint no PK
        bigint workflow_no FK
        string name "예: To Do, Code Review, QA"
        string color "nullable"
        string category "TODO|IN_PROGRESS|DONE|CANCELLED"
        int position "보드 내 순서"
        datetime create_date
    }

    WORKFLOW_TRANSITION {
        bigint no PK
        bigint workflow_no FK
        bigint from_status_no FK "WORKFLOW_STATUS.no 참조 (출발 상태)"
        bigint to_status_no FK "WORKFLOW_STATUS.no 참조 (도착 상태)"
        string name "예: Start Work, Submit Review"
        jsonb conditions "nullable — 전환 조건 (담당자 필수 등)"
        datetime create_date
    }

    %% ============================================================
    %% [9] BOARD — 스크럼/칸반 보드 설정
    %% ============================================================

    PROJECT ||--o{ BOARD : "has"
    BOARD ||--o{ BOARD_COLUMN : "has"
    BOARD_COLUMN ||--o{ BOARD_COLUMN_STATUS_MAPPING : "maps"
    WORKFLOW_STATUS ||--o{ BOARD_COLUMN_STATUS_MAPPING : "mapped_by"

    BOARD {
        bigint no PK
        bigint project_no FK
        string name
        string type "SCRUM|KANBAN"
        jsonb filter_config "nullable — 보드 필터 설정"
        string swimlane_by "NONE|ASSIGNEE|PRIORITY|EPIC, default NONE"
        datetime create_date
        datetime update_date
    }

    BOARD_COLUMN {
        bigint no PK
        bigint board_no FK
        string name
        int position
        int wip_limit "nullable, 0이면 무제한"
        datetime create_date
    }

    BOARD_COLUMN_STATUS_MAPPING {
        bigint no PK
        bigint board_column_no FK
        bigint workflow_status_no FK
    }

    %% ============================================================
    %% [10] COMPONENT — 프로젝트 내 컴포넌트 태깅
    %% ============================================================

    PROJECT ||--o{ COMPONENT : "defines"
    TICKET ||--o{ TICKET_COMPONENT : "tagged_with"
    COMPONENT ||--o{ TICKET_COMPONENT : "tagged_to"

    COMPONENT {
        bigint no PK
        bigint project_no FK
        string name
        string description "nullable"
        bigint owner_user_no FK "nullable — USER.no 참조 (컴포넌트 소유자/관리자)"
        bigint default_assignee_user_no FK "nullable — USER.no 참조 (자동 배정 대상)"
        datetime create_date
    }

    TICKET_COMPONENT {
        bigint no PK
        bigint ticket_no FK
        bigint component_no FK
    }

    %% ============================================================
    %% [11] VERSION — 릴리즈 버전 관리
    %% ============================================================

    PROJECT ||--o{ VERSION : "has"
    TICKET ||--o{ TICKET_FIX_VERSION : "fixed_in"
    TICKET ||--o{ TICKET_AFFECT_VERSION : "affected_in"
    VERSION ||--o{ TICKET_FIX_VERSION : "fixes"
    VERSION ||--o{ TICKET_AFFECT_VERSION : "affects"

    VERSION {
        bigint no PK
        bigint project_no FK
        string name "예: v2.1.0"
        string description "nullable"
        string status "UNRELEASED|RELEASED|ARCHIVED"
        date start_date "nullable"
        date release_date "nullable — RELEASED 전에는 null"
        bigint released_user_no FK "nullable — USER.no 참조 (릴리즈 담당자)"
        datetime create_date
        datetime update_date
    }

    TICKET_FIX_VERSION {
        bigint no PK
        bigint ticket_no FK
        bigint version_no FK
    }

    TICKET_AFFECT_VERSION {
        bigint no PK
        bigint ticket_no FK
        bigint version_no FK
    }

    %% ============================================================
    %% [12] CUSTOM FIELD — 프로젝트별 커스텀 필드
    %% ============================================================

    PROJECT ||--o{ CUSTOM_FIELD : "defines"
    CUSTOM_FIELD ||--o{ CUSTOM_FIELD_OPTION : "has_options"
    TICKET ||--o{ TICKET_CUSTOM_FIELD_VALUE : "has"
    CUSTOM_FIELD ||--o{ TICKET_CUSTOM_FIELD_VALUE : "valued_by"

    CUSTOM_FIELD {
        bigint no PK
        bigint project_no FK
        string name "예: 환경, QA 담당자"
        string field_type "TEXT|NUMBER|DATE|SELECT|MULTI_SELECT|USER|CHECKBOX|URL"
        boolean is_required "티켓 저장 시 해당 필드 값 필수 여부"
        string description "nullable"
        int position "필드 표시 순서"
        jsonb default_value "nullable"
        datetime create_date
        datetime update_date
    }

    CUSTOM_FIELD_OPTION {
        bigint no PK
        bigint custom_field_no FK
        string label "예: Production, Staging, Dev"
        string color "nullable"
        int position
        boolean is_active
    }

    TICKET_CUSTOM_FIELD_VALUE {
        bigint no PK
        bigint ticket_no FK
        bigint custom_field_no FK
        string value_text "nullable"
        decimal value_number "nullable"
        date value_date "nullable"
        bigint value_option_no FK "nullable, CUSTOM_FIELD_OPTION.no 참조 (SELECT용)"
        bigint value_user_no FK "nullable, USER.no 참조 (USER 타입용)"
        jsonb value_multi "nullable, MULTI_SELECT용"
    }

    %% ============================================================
    %% [13] TICKET LINK / WATCHER / VOTE / WORKLOG — 티켓 부가 관계
    %% ============================================================

    TICKET ||--o{ TICKET_LINK : "source"
    TICKET ||--o{ TICKET_LINK : "target"
    TICKET ||--o{ TICKET_WATCHER : "watched_by"
    TICKET ||--o{ TICKET_VOTE : "voted_by"
    TICKET ||--o{ TICKET_WORKLOG : "has"
    USER ||--o{ TICKET_WATCHER : "watches"
    USER ||--o{ TICKET_VOTE : "votes"
    USER ||--o{ TICKET_WORKLOG : "logs"

    TICKET_LINK {
        bigint no PK
        bigint source_ticket_no FK
        bigint target_ticket_no FK
        string link_type "BLOCKS|IS_BLOCKED_BY|RELATES_TO|DUPLICATES|IS_DUPLICATED_BY|CLONES"
        datetime create_date
    }

    TICKET_WATCHER {
        bigint no PK
        bigint ticket_no FK
        bigint user_no FK
        datetime create_date
    }

    TICKET_VOTE {
        bigint no PK
        bigint ticket_no FK
        bigint user_no FK
        datetime create_date
    }

    TICKET_WORKLOG {
        bigint no PK
        bigint ticket_no FK
        bigint user_no FK "작업자"
        int time_spent_minutes
        string description "nullable"
        datetime start_date "실제 작업 시작 시각"
        datetime create_date
    }

    %% ============================================================
    %% [14] DASHBOARD — 개인/공유 대시보드 (cross-cutting)
    %% ============================================================

    WORKSPACE ||--o{ DASHBOARD : "scoped_to"
    DASHBOARD ||--o{ DASHBOARD_GADGET : "has"
    USER ||--o{ DASHBOARD : "owns_dashboard"

    DASHBOARD {
        bigint no PK
        bigint workspace_no FK
        bigint user_no FK "소유자"
        string name
        string layout "SINGLE|DOUBLE|TRIPLE"
        boolean is_shared
        datetime create_date
        datetime update_date
    }

    DASHBOARD_GADGET {
        bigint no PK
        bigint dashboard_no FK
        string gadget_type "BURNDOWN|VELOCITY|PIE_CHART|FILTER_RESULTS|SPRINT_HEALTH|CREATED_VS_RESOLVED|WORKLOG_SUMMARY"
        jsonb config "필터NO, 프로젝트NO 등 가젯 설정"
        int column_index "레이아웃 내 열 위치"
        int position "열 내 순서"
        datetime create_date
    }

    %% ============================================================
    %% [15] SAVED FILTER — 저장된 필터/쿼리 (cross-cutting)
    %% ============================================================

    PROJECT ||--o{ SAVED_FILTER : "scoped_to"
    USER ||--o{ SAVED_FILTER : "owns_filter"

    SAVED_FILTER {
        bigint no PK
        bigint project_no FK "nullable — null이면 워크스페이스 전체 스코프"
        bigint user_no FK "소유자"
        string name
        string description "nullable"
        jsonb filter_config "타입/상태/담당자/라벨/우선순위 등 조합"
        jsonb sort_config "nullable — 정렬 조건"
        boolean is_shared "다른 멤버에게 공유 여부"
        boolean is_favorite
        datetime create_date
        datetime update_date
    }

    %% ============================================================
    %% [16] NOTIFICATION — 알림 (cross-cutting)
    %% ============================================================

    USER ||--o{ NOTIFICATION : "receives"

    NOTIFICATION {
        bigint no PK
        bigint user_no FK
        string type "MENTION|TICKET_ASSIGNED|PR_MERGED|..."
        string source_type "MESSAGE|TICKET|PAGE|GIT_EVENT"
        bigint source_no "source_type에 따른 polymorphic 참조 (FK 제약 없음)"
        jsonb payload "nullable"
        datetime read_date "nullable — 읽지 않은 알림은 null"
        datetime create_date
    }

```

---

## 3. 토큰 관리 정책

### 3.1 WORKSPACE_INVITATION.token — 초대 수락 토큰

| 항목 | 정책 |
|------|------|
| **생성** | `SecureRandom` 기반 URL-safe 32바이트(= 256비트) 난수. 충돌 검사 후 재생성. |
| **저장 형태** | **해시 저장** (SHA-256). DB에 평문 저장 금지. UK 인덱스는 해시값 기준. |
| **전달** | 평문 토큰은 생성 직후 1회에 한해 초대 메일 URL(`/invite/accept?token=<plain>`)에 포함해 발송. 서버 로그/감사 로그에도 평문 기록 금지. |
| **검증** | 수락 요청 시 수신 토큰을 해시하여 `token_hash`와 비교. 타이밍 공격 방지를 위해 constant-time 비교. |
| **만료** | `expire_date` 기본 TTL **7일**. 만료 시 배치 또는 조회 시점에 `status = EXPIRED`로 전환. |
| **1회성** | 수락 성공 시 `status = ACCEPTED`로 전환 후 이후 사용 불가. 재초대가 필요하면 새 invitation row 생성. |
| **철회** | 초대자/워크스페이스 관리자가 `status = REVOKED`로 수동 무효화 가능. |
| **상태 전이** | `PENDING → ACCEPTED` (수락) / `PENDING → EXPIRED` (만료) / `PENDING → REVOKED` (철회). 종료 상태에서 재전이 불가. |
| **중복 방지** | `(workspace_no, invitee_user_no, status = PENDING)` 조합에 unique 인덱스 — 같은 대상에 대한 동시 다수 PENDING 금지. |
| **재발송** | 같은 초대를 다시 보내야 하면 기존 PENDING을 REVOKED 처리 후 신규 row 발급. |
| **감사** | 발급·수락·철회·만료 모두 `TICKET_ACTIVITY`와 유사한 수준의 감사 로그 필요 (actor, timestamp, result). |

### 3.2 REFRESH_TOKEN — 재사용 탐지 정책 (요약)

| 항목 | 정책 |
|------|------|
| **저장소** | Redis (본 ERD는 구조 명세용). |
| **token_hash** | SHA-256 해시 저장. 평문 금지. |
| **family_id** | 최초 로그인 시 생성한 UUID. 회전 시 동일 family 내에서 `used=true`로 마킹 후 새 토큰 발급. |
| **재사용 탐지** | 이미 `used=true`인 토큰이 재제출되면 도난으로 간주하고 해당 `family_id` 전체를 즉시 폐기 + 해당 USER 재로그인 강제. |
| **만료** | `expire_date` 기본 TTL **14일**. 만료 시 Redis TTL에 의해 자동 삭제. |
| **디바이스 구분** | `user_agent`, `ip_address`는 이상 로그인 탐지와 세션 관리 UI에서 활용. |

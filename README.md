# TeamFlow

> 문서(Notion) + 이슈 관리(Jira) + 채팅(Slack)을 하나로 통합한 팀 협업 플랫폼

## 프로젝트 구조

```
teamflow/
├── frontend/              # 프론트엔드 (React + TypeScript + Tailwind CSS)
├── backend/               # 백엔드 (Spring Boot + Java + PostgreSQL)
├── docs/
│   └── planning/          # 기획 문서 (요구사항 명세서, 와이어프레임)
├── .claude/               # Claude Code 설정 (공용)
└── CLAUDE.md              # Claude Code 메인 설정 파일
```

## 기술 스택

| 영역 | 기술 |
|------|------|
| Frontend | React + TypeScript + Tailwind CSS |
| Backend | Spring Boot (Java) |
| Database | PostgreSQL + Redis |
| Realtime | Socket.io |
| Auth | Google OAuth 2.0 + JWT |

---

## Claude Code 가이드

이 프로젝트는 [superpower-agent](https://github.com/nv-minh/superpower-agent)를 기반으로 Claude Code 워크플로우가 구성되어 있습니다.

### 디렉토리 구조

```
.claude/
├── commands/              # 슬래시 커맨드 (/커맨드명으로 실행)
│   ├── front/             #   프론트 전용 커맨드 (/front:커맨드명)
│   ├── back/              #   백엔드 전용 커맨드 (/back:커맨드명)
│   ├── fad/               #   공용 파이프라인 (/fad:커맨드명)
│   ├── gsd/               #   공용 파이프라인 (/gsd:커맨드명)
│   └── *.md               #   공용 커맨드 (/커맨드명)
├── conventions/           # 코드 컨벤션
│   ├── front/             #   프론트엔드 (FSD, React, 라이브러리)
│   └── back/              #   백엔드 (Spring — 추가 예정)
├── skills/                # 스킬 (상세 워크플로우 가이드)
│   ├── shared/            #   공용 스킬
│   ├── front/             #   프론트 전용
│   └── back/              #   백엔드 전용
├── rules/                 # 코드 규칙 (자동 적용)
├── hooks/                 # 런타임 훅
├── scripts/               # 유틸리티 스크립트
├── pm/                    # PM(제품 관리) 워크플로우
├── config/                # 설정 파일
├── instructions/          # 오케스트레이션 지침
├── memory/                # 루프 상태/의사결정 기록
└── state/                 # 세션 안전 상태
```

### 커맨드 카탈로그

Claude Code에서 `/커맨드명`으로 실행할 수 있습니다.

#### 핵심 파이프라인

| 커맨드 | 설명 |
|--------|------|
| `/fad:pipeline` | **메인 파이프라인** — 브레인스토밍 → 계획 → 구현 → 리뷰 → 최적화 → 완료까지 통합 실행 |
| `/fad:optimize` | 리뷰 후 필수 최적화 단계 |
| `/fad:quality-gate` | 완료/배포 전 엄격한 품질 게이트 (통과/불통과 판정) |
| `/fad:ship` | 배포 준비 및 실행 |
| `/fad:map-codebase` | 코드베이스 구조 분석 및 매핑 |
| `/fad:pr-branch` | PR용 브랜치 생성 및 관리 |

#### 프론트엔드 전용

| 커맨드 | 설명 |
|--------|------|
| `/front:new-feature` | FSD 아키텍처 기반 새 기능 개발 (7단계 워크플로우) |
| `/front:discovery-ui-handoff` | 요구사항 → UI 컨셉 → UI 계약 → 핸드오프 |
| `/front:qc-verify-ui` | 브라우저 자동화로 기능/디자인 검증 |

#### 백엔드 전용

| 커맨드 | 설명 |
|--------|------|
| `/back:*` | 백엔드 전용 커맨드 (Spring 기반 — @beanteacher가 추가 예정) |

#### 개발 워크플로우 (공용)

| 커맨드 | 설명 |
|--------|------|
| `/feature-swarm` | 병렬 워크스트림으로 기능 구현 |
| `/fix-issue` | 이슈 분석 + 타겟 수정 + 검증 |
| `/autoplan` | 자동 계획-리뷰 파이프라인 (PM + 아키텍처 + 설계 + 테스트) |
| `/autopilot-loop` | 자율 딜리버리 루프 (메모리 업데이트 + 블로커 감지 시 중단) |

#### 코드 리뷰 & PR

| 커맨드 | 설명 |
|--------|------|
| `/code-review` | 현재 PR 또는 브랜치 변경사항 코드 리뷰 |
| `/review` | 심각도 기반 코드 리뷰 (병렬 분석) |
| `/pr` | GitHub Pull Request 생성 |
| `/pr-feedback-loop` | PR 댓글 수집 → 수정 → 품질 게이트 → QC 재검증 |

#### 품질 & 테스트

| 커맨드 | 설명 |
|--------|------|
| `/code-quality-gate` | lint + 타입체크(TS) + 테스트 실행 |
| `/qa-only` | 브라우저 기반 QA 리포트 (코드 변경 없음) |

#### PM (제품 관리)

| 커맨드 | 설명 |
|--------|------|
| `/pm-intake` | 요구사항 논의 → PM 핸드오프 팩 생성 (PRD, 스프린트, 스토리) |
| `/pm-to-build` | PM 산출물 → FAD 계획/실행으로 전환 |
| `/pm-discover` | PM 디스커버리 워크플로우 |
| `/pm-write-prd` | PRD(제품 요구사항 문서) 생성 |
| `/pm-story` | 유저 스토리 생성 및 분할 |
| `/pm-prioritize` | 우선순위 결정 워크플로우 |
| `/pm-plan-roadmap` | 로드맵 및 스프린트 시퀀싱 |
| `/pm-strategy` | 제품 전략 워크플로우 |
| `/pm-delivery-loop` | `fad:pipeline` 호환 래퍼 |

#### 보안 & 운영

| 커맨드 | 설명 |
|--------|------|
| `/security-scan` | 보안 스캔 (의존성 + 시크릿 + SAST) |
| `/dependency-audit` | 의존성 취약점 검사 |
| `/secrets-scan` | 시크릿/자격증명 노출 감지 |
| `/deploy` | 프로덕션 배포 (병렬 준비 검사 + 롤백 계획) |
| `/health-check` | 서비스 상태 진단 |
| `/incident-response` | 인시던트 분류/격리/복구 워크플로우 |
| `/rollback` | 롤백 준비 및 실행 |
| `/setup-monitoring` | 모니터링 구성 (알림, 대시보드) |

#### 환경 설정 & 유틸리티

| 커맨드 | 설명 |
|--------|------|
| `/setup-doctor` | CLI, MCP, 인증 설정 상태 점검 |
| `/install-browser-skills` | 브라우저 테스트 스킬 설치 (Playwright) |
| `/brownfield-map-style` | 기존 코드베이스 패턴 분석 및 승인/안티 패턴 분류 |
| `/gen-doc-sheet` | PM/빌드/QC 산출물을 스프레드시트로 내보내기 |

#### 안전 제어

| 커맨드 | 설명 |
|--------|------|
| `/careful` | 파괴적 명령 경고 모드 활성화 |
| `/freeze` | 특정 디렉토리로 편집 범위 제한 |
| `/guard` | careful + freeze 동시 활성화 |
| `/unfreeze` | freeze 해제 |
| `/unguard` | careful + freeze 모두 해제 |

### 규칙 (Rules)

`.claude/rules/`에 있는 규칙들은 자동으로 적용됩니다:

| 규칙 | 설명 |
|------|------|
| `code-style` | 포매터/린터 설정 우선, 작은 함수 선호, 브라운필드에서 diff 최소화 |
| `api-conventions` | 버전 네임스페이스, 입력 검증, 안정적 응답 형태 |
| `database` | 추가적(additive) 스키마 변경 선호, 마이그레이션 분리 |
| `error-handling` | 경계에서 빠른 실패, 부작용은 안전하게, 에러 삼키지 않기 |
| `git-workflow` | 하나의 작업 = 하나의 커밋, 요구사항 ID 연결 |
| `testing` | 도메인/API 로직은 TDD 필수, 수정된 버그는 회귀 테스트 필수 |
| `security` | 시크릿 하드코딩 금지, 입력 검증, 최소 권한 원칙 |
| `project-structure` | 기존 모듈 경계 존중, 최상위 폴더 함부로 만들지 않기 |
| `search-before-build` | 만들기 전에 기존 솔루션 먼저 검색 |
| `completeness-principle` | 80-90% 완료는 미완성, 할 수 있으면 끝까지 |
| `agent-docs` | 에이전트 우선 문서화 (입력, 출력, 제약조건 명시) |

### 컨벤션

| 경로 | 내용 |
|------|------|
| `conventions/front/react-nextjs.md` | React/Next.js 코드 컨벤션 (FSD 구조, 네이밍, 패턴) |
| `conventions/front/fsd-architecture.md` | Feature-Sliced Design 아키텍처 가이드 |
| `conventions/front/library-stack.md` | 프론트엔드 라이브러리 스택 가이드 |
| `conventions/back/` | 백엔드 컨벤션 (Spring — @beanteacher가 추가 예정) |

### 스킬

| 경로 | 설명 |
|------|------|
| `skills/shared/code-reviews/` | 코드 리뷰 가이드 (체크리스트, 점수, 피드백 규칙) |
| `skills/shared/git-workflow/` | Git 워크플로우 및 브랜치 전략 |
| `skills/shared/playwright/` | Playwright 브라우저 자동화 테스트 |
| `skills/shared/fad-*/` | FAD 파이프라인 관련 스킬 (계획, 실행, 검증, 최적화) |

---

## 시작하기

```bash
# 클론
git clone git@github.com:HyunSeungBeom/teamflow.git
cd teamflow

# Git 로컬 설정 (개인 계정)
git config user.name "YourGitHubUsername"
git config user.email "your@email.com"
```

## 팀

| 역할 | GitHub |
|------|--------|
| Frontend | [@HyunSeungBeom](https://github.com/HyunSeungBeom) |
| Backend | [@beanteacher](https://github.com/beanteacher) |

---
name: "Git Workflow"
description: "브랜치 관리, Jira 연동, 작업 시작/완료 체크리스트를 통한 체계적인 Git 워크플로우"
---

# Git Workflow Skill

## 목적
Git 브랜치 관리와 Jira 티켓 연동을 통한 체계적인 작업 워크플로우를 지원합니다.

---

## 브랜치 네이밍 규칙

### 형식
```
<type>/<TICKET-NUMBER>
```

### 타입

| 타입 | 용도 | 예시 |
|------|------|------|
| `feat/` | 새로운 기능 | `feat/PROJ-123` |
| `fix/` | 버그 수정 | `fix/PROJ-456` |
| `refactor/` | 리팩토링 | `refactor/PROJ-789` |
| `hotfix/` | 긴급 수정 | `hotfix/PROJ-100` |
| `chore/` | 설정/환경 변경 | `chore/PROJ-200` |
| `docs/` | 문서 | `docs/PROJ-300` |
| `test/` | 테스트 | `test/PROJ-400` |

### 규칙
```bash
# 올바른 예시
git checkout -b feat/PROJ-123
git checkout -b fix/PROJ-456

# 잘못된 예시 (너무 상세함 — 내용은 Jira에)
git checkout -b feat/PROJ-123-add-user-auth-with-jwt-and-refresh-token
```

**원칙**: 브랜치 이름은 티켓 번호만으로 충분합니다. 상세 내용은 Jira 티켓에 기록합니다.

Jira 연동이 없는 개인 프로젝트의 경우:
```
<type>/<간결한-설명>
```
예: `feat/user-auth`, `fix/login-redirect`

---

## 브랜치 전략

```
main                        # 프로덕션
├── develop                 # 개발 통합
│   ├── feat/PROJ-123       # 기능 개발 → develop
│   ├── fix/PROJ-456        # 버그 수정 → develop
│   └── refactor/PROJ-789   # 리팩토링 → develop
├── hotfix/PROJ-100         # 긴급 수정 → main
└── release/v1.2.0          # 릴리즈 → main
```

| 브랜치 | 머지 대상 | 용도 |
|--------|-----------|------|
| `feat/*`, `fix/*`, `refactor/*` | `develop` | 일반 개발 |
| `hotfix/*` | `main` | 프로덕션 긴급 수정 |
| `release/*` | `main` | 릴리즈 배포 |

---

## 작업 시작 체크리스트

### 1단계: 티켓 확인

Jira MCP가 설정된 경우:
```
"PROJ-123 티켓 내용 확인해줘"
→ mcp__atlassian__getJiraIssue
```

확인 사항:
- [ ] Summary (작업 제목)
- [ ] Description (상세 요구사항)
- [ ] Status (현재 상태)
- [ ] Parent/Epic (전체 컨텍스트)

### 2단계: 브랜치 생성

```bash
# 1. 최신 develop으로 이동
git checkout develop
git pull origin develop

# 2. 새 브랜치 생성
git checkout -b feat/PROJ-123

# 3. 원격에 푸시 (선택)
git push -u origin feat/PROJ-123
```

### 3단계: Jira 상태 업데이트 (MCP 사용 시)

```
"PROJ-123을 In Progress로 변경해줘"
→ mcp__atlassian__transitionJiraIssue

"작업 시작 코멘트 추가해줘"
→ mcp__atlassian__addCommentToJiraIssue
```

### 4단계: 작업 환경 확인

- [ ] 관련 문서/디자인 확인
- [ ] 기존 코드 파악
- [ ] 테스트 환경 준비

---

## 작업 완료 체크리스트

### 1단계: 코드 완성

- [ ] 기능 구현 완료
- [ ] TypeScript 에러 없음
- [ ] 린트 통과 (`npm run lint`)
- [ ] 빌드 성공 (`npm run build`)
- [ ] 테스트 통과 (`npm run test`)

### 2단계: 커밋

```bash
git add .
git commit -m "feat: PROJ-123 사용자 인증 페이지 구현

- LoginForm 컴포넌트 생성
- useAuth 훅 구현
- 로그인 API 연동
"
```

### 3단계: PR 생성

```bash
git push -u origin feat/PROJ-123
gh pr create --base develop --title "feat: PROJ-123 사용자 인증 페이지 구현"
```

### 4단계: Jira 업데이트 (MCP 사용 시)

```
"PROJ-123에 PR 링크 코멘트 달아줘"
→ mcp__atlassian__addCommentToJiraIssue

"PROJ-123을 In Review로 변경해줘"
→ mcp__atlassian__transitionJiraIssue
```

---

## 커밋 메시지 규칙

### 형식

```
<type>: <subject>

<body>
```

Jira 연동 시:
```
<type>: <TICKET-NUMBER> <subject>

<body>
```

### 예시

```
feat: PROJ-123 사용자 인증 페이지 구현

- LoginForm 컴포넌트 생성 (features/auth)
- useAuth 커스텀 훅 구현
- Zod 스키마 기반 폼 검증 추가
- 로그인/로그아웃 API 연동
```

### 타입

| 타입 | 설명 |
|------|------|
| `feat` | 새 기능 |
| `fix` | 버그 수정 |
| `refactor` | 리팩토링 (기능 변경 없음) |
| `style` | UI/CSS 변경 |
| `chore` | 빌드, 설정 변경 |
| `docs` | 문서 변경 |
| `test` | 테스트 추가/수정 |
| `perf` | 성능 개선 |

### 규칙
- 제목 50자 이내
- 본문 72자 줄바꿈
- 한글로 작성 (영어 기술 용어는 영어 유지)
- 명령형으로 작성 (예: "추가", "수정", "제거")

---

## Jira 연동 워크플로우 (MCP Atlassian 사용 시)

### 전체 플로우

```
1. 티켓 확인        → mcp__atlassian__getJiraIssue
2. 브랜치 생성      → git checkout -b feat/PROJ-123
3. 상태 변경        → mcp__atlassian__transitionJiraIssue (In Progress)
4. 작업 시작 코멘트 → mcp__atlassian__addCommentToJiraIssue
5. 코드 작성        → (개발)
6. PR 생성          → gh pr create
7. PR 링크 코멘트   → mcp__atlassian__addCommentToJiraIssue
8. 상태 변경        → mcp__atlassian__transitionJiraIssue (In Review)
9. 리뷰 후 머지     → gh pr merge
10. 상태 변경       → mcp__atlassian__transitionJiraIssue (Done)
```

### Jira 티켓 Description 작성 (마크다운)

```markdown
## 작업 개요
작업 설명

## 작업 목표
- 목표 1
- 목표 2

## 작업 내용
### 주요 변경사항
- 변경 1
- 변경 2

## 체크리스트
- [ ] 작업 1
- [ ] 작업 2
- [ ] 테스트 작성

## 관련 문서
- [디자인](Figma 링크)
- [PR](GitHub PR 링크)
```

### 진행 상황 코멘트

```markdown
## 진행 상황 업데이트

### 완료
- LoginForm 컴포넌트 구현
- API 연동 완료

### 진행 중
- 테스트 코드 작성 중

### 다음 작업
- E2E 테스트 추가
```

---

## 주의사항

1. **브랜치 이름**: 간결하게, 한글/특수문자 사용 금지
2. **커밋 전**: `npm run build && npm run lint` 확인
3. **PR 생성**: 반드시 올바른 base 브랜치로
4. **Jira 상태**: 실제 작업 상태와 항상 동기화
5. **force push**: develop/main에는 절대 force push 금지

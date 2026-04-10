# Git 워크플로우

- 하나의 작업, 하나의 명확한 커밋 의도.
- 커밋 메시지에 타입과 (해당되는 경우) 범위를 표시한다.
- 계획 산출물과 구현 커밋을 실용적으로 분리한다.
- 명시적으로 요청하지 않는 한 히스토리를 재작성하지 않는다.
- 추적성을 위해 커밋/작업 노트에 요구사항 ID를 연결한다.

## 커밋 메시지 컨벤션

### 포맷
```
type(scope): subject          # scope가 있는 경우
type: subject                 # scope가 없는 경우

[optional body]

[optional footer]
```

- `type`은 **필수**. `scope`는 **조건부 필수** (아래 "스코프 사용 규칙" 참조).
- `subject`는 한글 또는 영문 모두 허용. 마침표로 끝내지 않는다.
- 본문은 "무엇을 바꿨는가"보다 "왜 바꿨는가"를 설명한다.

### 허용 타입

| 타입 | 용도 |
|------|------|
| `feat` | 새 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 동작 변경 없는 구조/가독성 개선 |
| `perf` | 성능 개선 |
| `docs` | 문서·주석·README 변경 |
| `test` | 테스트 추가/수정 |
| `chore` | 빌드·설정·의존성·초기 셋업 등 잡무 |
| `style` | 포매팅·공백·세미콜론 등 (로직 변경 없음) |
| `ci` | CI/CD 파이프라인 변경 |
| `build` | 빌드 스크립트·툴체인 변경 |

### 스코프 사용 규칙

**스코프는 `backend` 또는 `frontend` 두 가지만 허용한다.** 그리고 커밋의 **주 영향 도메인**이 해당 영역에 확실하게 귀속될 때에만 붙인다.

- `type(backend):` — 변경이 백엔드 도메인에 귀속 (예: `backend/` 하위 코드, `.claude/commands/back/`, `.claude/rules/backend-*` 등 백엔드 전용 자산)
- `type(frontend):` — 변경이 프론트엔드 도메인에 귀속 (예: `frontend/` 하위 코드, `.claude/commands/front/`, `.claude/rules/frontend-*` 등 프론트엔드 전용 자산)
- **스코프 생략** — 아래 중 하나라도 해당되면 `type:` 단독 사용
  - 루트 설정·메타 파일 (`README.md`, `.gitignore`, `CLAUDE.md` 등)
  - 양쪽 도메인에 동시에 해당하거나 어느 쪽에도 특정되지 않는 범용 규칙·도구·문서 (예: `git-workflow.md` 같은 공통 워크플로우 규칙)
  - `backend/`와 `frontend/` 파일이 한 커밋에 섞인 경우 — 원칙적으로 **커밋을 분리**하고, 불가능하면 스코프 생략

`docs`, `chore`, `config`, `claude`, `infra` 같은 임의 스코프는 **사용하지 않는다**. 스코프는 오직 `backend`/`frontend` 두 값만 유효하다.

### 예시

**스코프 사용 (backend/frontend 도메인에 귀속):**
```
feat(backend): 로그인 엔드포인트에 JWT 발급 로직 추가 (REQ-AUTH-001)
fix(frontend): 로그인 폼 엔터키 제출 핸들러 누락 수정
refactor(backend): UserService 중복 조회 로직 통합
chore(backend): teamflow-api Spring Boot 프로젝트 초기 셋업
test(frontend): 로그인 페이지 상호작용 테스트 추가
docs(backend): 백엔드 커맨드·컨벤션·옵션 레퍼런스 추가
```

**스코프 생략 (공통/루트/범용):**
```
docs: 커밋 메시지 컨벤션을 git-workflow 규칙에 추가
chore: 루트 .gitignore에 OMC 런타임 상태 제외 규칙 추가
refactor: 레포 루트 구조 정리 및 커맨드 재배치
docs: README의 기술 스택 섹션 갱신
```

### 요구사항 ID 연결

- 구현 커밋 subject 또는 body에 관련 `REQ-<DOMAIN>-<NNN>`을 표기한다.
- 여러 요구사항이 묶이면 body에 bullet 형태로 나열한다.

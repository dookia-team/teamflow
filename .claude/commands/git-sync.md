# /git-sync 커맨드

원격 동기화 → 변경사항 커밋 → 푸시 전체 사이클을 하나의 결정론적 흐름으로 실행한다.

> **전제:** 커밋 메시지 컨벤션은 `.claude/rules/git-workflow.md` 를 따른다. `type(scope):` 포맷이며 스코프는 `backend` / `frontend` 두 값만 허용한다.

---

## 입력

```
$ARGUMENTS: [옵션]
```

| 옵션 | 의미 |
|------|------|
| (비어있음) | 전체 사이클 실행 (fetch → pull → commit → push) |
| `--dry-run` | 커밋/푸시 없이 상태만 리포트 |
| `--no-push` | 커밋까지만, 푸시 생략 |
| `--message "..."` | 커밋 메시지를 사용자가 직접 지정 (컨벤션 검증은 수행) |
| `--amend` | 마지막 커밋을 amend (사용자가 명시적으로 요청한 경우만) |

---

## 실행 단계

### 1단계: 선행 안전 점검

병렬로 실행한다.

```bash
git rev-parse --is-inside-work-tree
git branch --show-current
git status --porcelain=v1
git log --oneline -n 5
git remote -v
```

- 워크트리 아님 → 즉시 중단하고 사용자에게 리포트.
- 현재 브랜치가 `main` / `master` → **사용자 확인 받을 때까지 차단**. 직접 푸시를 허용하지 않는다. 사용자가 명시적으로 확인하면 계속 진행.
- 업스트림이 설정되어 있지 않으면 7단계에서 `--set-upstream` 옵션을 붙인다.

### 2단계: 원격 fetch

```bash
git fetch --all --prune
```

- 실패하면 네트워크/인증 원인을 리포트하고 중단.

### 3단계: pull (rebase 우선)

```bash
git pull --rebase --autostash
```

- 머지 충돌 발생 시 작업을 **중단**하고 충돌 파일 목록과 다음 액션(해결 → `git rebase --continue`)을 사용자에게 제시한다. 임의로 `git rebase --abort` / `git reset --hard` 를 실행하지 않는다.
- `--autostash` 로 인해 stash 된 변경이 있었으면 복원 성공 여부를 확인한다.

### 4단계: 변경사항 유무 판정

```bash
git status --porcelain=v1
```

| 결과 | 분기 |
|------|------|
| 변경 없음 | 5~7단계 건너뛰고 "동기화만 수행" 으로 리포트하고 종료. |
| 변경 있음 | 5단계로 진행. |
| `--no-push` 이면서 변경 없음 | 종료. |

### 5단계: 커밋 계획 작성

1. `git diff --stat` + `git diff`(요약) 로 변경 파일/영역을 파악.
2. **스코프 결정** — `.claude/rules/git-workflow.md` 의 스코프 사용 규칙에 따른다.
   - 변경이 `backend/` 하위에만 귀속 → `type(backend):`
   - 변경이 `frontend/` 하위에만 귀속 → `type(frontend):`
   - 루트 설정, 공통 규칙, 양쪽 도메인 혼합 → **스코프 생략**
   - `backend/` 와 `frontend/` 가 한 커밋에 섞였고 논리적으로 분리 가능 → **커밋을 나눈다**. 분리 불가능하면 스코프 생략.
3. **타입 결정** — `feat` / `fix` / `refactor` / `perf` / `docs` / `test` / `chore` / `style` / `ci` / `build` 중 하나. `docs` / `chore` / `config` / `claude` / `infra` 같은 임의 스코프는 금지.
4. **메시지 작성** — subject 는 한 줄, 마침표 없이, "왜" 를 본문에 기재. `.planning/pm/current/PRD.md` 에 요구사항 ID 가 있으면 subject 또는 body 에 표기.
5. 금지 대상 체크 — `.env`, `*credentials*`, `*secret*`, `.key`, `.pem` 등이 staging 에 포함되면 **즉시 중단**하고 사용자에게 경고.

사용자가 `--message "..."` 를 줬으면 그 메시지가 `type(scope)?: subject` 정규식 (`^(feat|fix|refactor|perf|docs|test|chore|style|ci|build)(\\((backend|frontend)\\))?:\\s.+`) 에 맞는지 검증만 수행한다. 검증 실패 시 수정 제안 후 중단.

### 6단계: 스테이징 + 커밋

```bash
# 구체 파일명으로 staging (git add -A 금지)
git add <file1> <file2> ...
git status

git commit -m "$(cat <<'EOF'
<subject>

<body (선택)>
EOF
)"
```

- `-A` / `.` 전부 staging 은 시크릿 유출 위험이 있어 금지. 변경 파일을 열거하여 staging.
- 훅 실패 시 `--no-verify` 를 붙이지 않는다. 원인을 해결하고 **새 커밋**을 만든다 (amend 금지).
- `--amend` 플래그를 사용자가 준 경우에만 amend 수행.

### 7단계: push

```bash
# 업스트림 있을 때
git push

# 업스트림 없을 때 (1단계에서 판정)
git push --set-upstream origin <current-branch>
```

- `--force` / `--force-with-lease` 는 **절대 자동으로 붙이지 않는다**. 필요하면 사용자에게 확인을 받는다.
- 리모트에서 거부(non-fast-forward) → fetch/pull 을 다시 돌리라고 안내하되, 자동으로 force push 하지 않는다.
- `--no-push` 플래그가 있으면 이 단계는 건너뛴다.

### 8단계: 최종 리포트

아래 정보를 한 번에 요약한다.

```
✅ 동기화 완료
  브랜치: <name> → origin/<name>
  fetch:  <new refs 개수>
  pull:   <ahead/behind 요약>
  커밋:   <sha short> <subject>
  push:   <성공 | 스킵 | 실패 사유>
  변경 파일: N개 (<파일 리스트 5개까지 + 이상 생략>)
```

아무 변경도 없었으면 "원격 동기화만 수행 (커밋/푸시 없음)" 으로 리포트한다.

---

## 금지 사항 (하드 스톱)

다음 경우는 작업을 **중단하고 사용자 확인을 받는다**. 자동으로 진행하지 않는다.

- 현재 브랜치가 `main` 또는 `master` 이고 커밋/푸시를 시도하는 경우
- `--force` / `--force-with-lease` 요청
- `git reset --hard`, `git checkout -- .`, `git clean -fd` 같은 파괴적 복구 필요한 상황
- 시크릿 의심 파일 staging 감지 (`.env`, `credentials*`, `*secret*`, `*.key`, `*.pem`, `id_rsa*`)
- rebase 머지 충돌

---

## 실행 예시

| 입력 | 동작 |
|------|------|
| `/git-sync` | fetch → pull → 자동 커밋 메시지 생성 → push |
| `/git-sync --dry-run` | 상태만 출력 (커밋/푸시 없음) |
| `/git-sync --no-push` | 커밋까지만 |
| `/git-sync --message "fix(backend): Issue soft delete 쿼리 누락 보정"` | 메시지 검증 후 커밋 + push |
| `/git-sync --amend` | 마지막 커밋 amend (사용자 명시 요청) |

---

## 참조

- `.claude/rules/git-workflow.md` — 커밋 메시지 컨벤션 (type/scope/subject 규칙)
- `.claude/rules/security.md` — 시크릿 하드코딩 금지

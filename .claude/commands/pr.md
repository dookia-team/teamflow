# /pr 커맨드

GitHub Pull Request를 생성합니다.

## 입력
$ARGUMENTS: PR 제목 또는 추가 옵션 (선택사항)

## 실행 단계

### 1단계: 현재 브랜치 및 베이스 브랜치 확인

```bash
# 현재 브랜치
CURRENT_BRANCH=$(git branch --show-current)

# 베이스 브랜치 자동 감지
case "$CURRENT_BRANCH" in
  hotfix/*) BASE_BRANCH="main" ;;
  release/*) BASE_BRANCH="main" ;;
  *) BASE_BRANCH="develop" ;;
esac
```

### 2단계: 변경사항 확인

```bash
# 베이스 브랜치 최신화
git fetch origin $BASE_BRANCH

# 커밋 목록
git log origin/$BASE_BRANCH..HEAD --oneline

# 변경 파일 통계
git diff origin/$BASE_BRANCH...HEAD --stat

# 상세 변경 내용
git diff origin/$BASE_BRANCH...HEAD
```

### 3단계: 커밋 안 된 변경사항 확인

```bash
git status
# 커밋되지 않은 변경사항 있으면 경고
```

### 4단계: 원격 브랜치 푸시

```bash
git push -u origin $CURRENT_BRANCH
```

### 5단계: PR 생성

```bash
gh pr create --base $BASE_BRANCH --title "<type>: <subject>" --body "$(cat <<'EOF'
## Summary
- 변경사항 요약

## Changes
| 파일 | 변경 내용 |
|------|-----------|
| `파일명` | 변경 설명 |

## Test plan
- [ ] 테스트 항목

🤖 Generated with [Claude Code](https://claude.com/claude-code)
EOF
)"
```

## 타겟 브랜치 규칙

| 브랜치 타입 | 타겟 브랜치 | 설명 |
|-------------|-------------|------|
| `feat/*` | `develop` | 새 기능 |
| `fix/*` | `develop` | 버그 수정 |
| `hotfix/*` | `main` | 긴급 수정 |
| `refactor/*` | `develop` | 리팩토링 |
| `chore/*` | `develop` | 설정/환경 변경 |
| `release/*` | `main` | 릴리즈 |

## PR 제목 규칙

```
<type>: <subject>
```

예시:
- `feat: 사용자 인증 페이지 구현`
- `fix: 로그인 시 토큰 갱신 버그 수정`
- `hotfix: 결제 금액 표시 오류 긴급 수정`
- `refactor: API 클라이언트 모듈 분리`

## 타입별 PR 본문 템플릿

### feat (기능 개발)
```markdown
## Summary
- 새로운 기능에 대한 설명

## Changes
| 파일 | 변경 내용 |
|------|-----------|
| `파일명` | 변경 설명 |

## Test plan
- [ ] 기능 테스트
- [ ] 반응형 확인

## Screenshots
UI 변경 시 스크린샷 첨부
```

### fix (버그 수정)
```markdown
## Summary
- 수정된 버그 설명

## Root Cause
버그 원인 설명

## Solution
해결 방법 설명

## Test plan
- [ ] 버그 재현 확인
- [ ] 수정 확인
```

### hotfix (긴급 수정)
```markdown
## Summary
- 긴급 수정 사항

## Problem
- 에러 내용: `에러 메시지`
- 영향 범위: 영향받는 기능/사용자

## Solution
해결 방법

## Rollback Plan
문제 발생 시 롤백 방법
```

## PR 생성 전 체크리스트

- [ ] 빌드 성공 (`npm run build`)
- [ ] 린트 통과 (`npm run lint`)
- [ ] 테스트 통과 (`npm run test`)
- [ ] 불필요한 파일 커밋 안 함 (.env, node_modules 등)
- [ ] PR 제목에 타입 포함
- [ ] 관련 이슈 번호 포함 (있는 경우)

## PR 생성 후

```bash
# PR URL 확인
gh pr view --web

# PR 상세 보기
gh pr view
```

**주의**: PR 승인(`--approve`)은 반드시 사용자 확인 후 진행

# /code-review 커맨드

열려있는 PR 또는 현재 브랜치의 변경 사항을 코드 리뷰합니다.

## 참조
- 리뷰 스킬: `skills/code-reviews/SKILL.md`

## 입력
$ARGUMENTS: PR 번호 또는 옵션

**사용법:**
- `/code-review` — 현재 브랜치의 변경 사항 리뷰
- `/code-review 123` — PR #123 리뷰
- `/code-review all` — 열려있는 모든 PR 순차 리뷰

## 실행 단계

### 1단계: 리뷰 대상 결정

```bash
# PR 번호가 있으면 해당 PR만
# "all"이면 열려있는 모든 PR
# 비어있으면 현재 브랜치의 diff
gh pr list --state open --json number,title,baseRefName,headRefName,author
```

### 2단계: PR 정보 수집

```bash
# PR 상세 정보
gh pr view {PR_NUMBER} --json number,title,author,baseRefName,headRefName,additions,deletions,files

# PR 작성자 (멘션용)
gh pr view {PR_NUMBER} --json author --jq '.author.login'

# 변경 파일 목록
gh pr diff {PR_NUMBER} --name-only
```

### 3단계: 코드 분석

```bash
# 전체 diff
git diff {BASE_BRANCH}...HEAD

# 파일별 통계
git diff {BASE_BRANCH}...HEAD --stat

# 커밋 목록
git log {BASE_BRANCH}..HEAD --oneline
```

### 4단계: 심층 검토 수행

`skills/code-reviews/SKILL.md`의 체크리스트에 따라 검토 수행

### 5단계: 리뷰 결과 작성

리뷰 템플릿(`skills/code-reviews/SKILL.md`의 템플릿)에 따라 작성

### 6단계: PR에 코멘트 게시

```bash
# PR에 리뷰 코멘트 포스팅
gh pr comment {PR_NUMBER} --body "$(cat review.md)"

# 또는 승인/변경요청과 함께
gh pr review {PR_NUMBER} --approve --body "리뷰 내용"
gh pr review {PR_NUMBER} --request-changes --body "리뷰 내용"
```

**주의**: PR 승인(`--approve`)은 반드시 사용자 확인 후 진행

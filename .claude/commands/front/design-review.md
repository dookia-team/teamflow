---
name: design-review
description: Figma 디자인과 구현된 UI를 비교하여 디자인 QC 리포트를 생성한다.
argument-hint: "<Figma URL 또는 페이지명>"
allowed-tools:
  - Read
  - Write
  - Edit
  - Bash
  - Glob
  - Grep
  - AskUserQuestion
  - mcp__figma__*
---

<objective>
Figma 원본 디자인과 구현된 프론트엔드 UI를 비교하여 디자인 시스템 준수 여부를 검증한다.
</objective>

<context>
입력: $ARGUMENTS

참조:
- @.claude/conventions/front/design-system.md
- @.claude/skills/front/design-system/SKILL.md

출력:
- `.planning/design/reviews/{page-name}-review.md` QC 리포트
</context>

<process>
1. 입력에서 대상 페이지/컴포넌트 식별.
2. Figma에서 `get_screenshot`으로 디자인 스크린샷 캡처.
3. `get_design_context`로 디자인 상세 (토큰, 컴포넌트, 스페이싱) 추출.
4. 구현된 코드 읽기 — 해당 페이지/컴포넌트 파일.
5. 비교 항목 체크:
   - [ ] 컬러 토큰 일치
   - [ ] 타이포그래피 (크기, 무게, 줄간격)
   - [ ] 스페이싱 (패딩, 마진, 갭)
   - [ ] 라운딩 (border-radius)
   - [ ] 컴포넌트 사용 일관성 (shared/ui/ 컴포넌트 재사용 여부)
   - [ ] 레이아웃 구조 (flex/grid)
   - [ ] 반응형 대응 (있는 경우)
6. 불일치 항목 severity 분류:
   - **critical**: 레이아웃 깨짐, 기능 영향
   - **major**: 토큰 불일치 (다른 컬러, 사이즈)
   - **minor**: 미세 스페이싱 차이 (1~2px)
7. `.planning/design/reviews/{page-name}-review.md`에 리포트 작성:
   - 전체 점수 (critical 0개 = pass)
   - 불일치 항목 목록 + 수정 제안
   - 스크린샷 참조
8. critical/major 항목이 있으면 수정 코드 제안.
</process>

---
name: design-tokens
description: Figma Variables에서 디자인 토큰을 추출하여 Tailwind/CSS와 동기화한다.
argument-hint: "<Figma 파일 URL (선택)>"
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
Figma의 디자인 토큰(Variables)을 프론트엔드 Tailwind/CSS 설정과 동기화한다.
변경된 토큰을 감지하고 코드에 반영한다.
</objective>

<context>
입력: $ARGUMENTS (Figma URL, 없으면 디자인 시스템 파일 사용)

참조:
- @.claude/conventions/front/design-system.md
- @.claude/skills/front/design-system/SKILL.md
- @frontend/src/index.css

출력:
- 업데이트된 Tailwind/CSS 토큰 설정
- 토큰 diff 리포트
</context>

<process>
1. Figma 파일에서 `get_variable_defs`로 현재 Variables 추출.
2. 추출된 토큰을 카테고리별 분류:
   - color/ → 컬러 토큰
   - typography/ → 타이포 토큰
   - spacing/ → 스페이싱 토큰
   - radius/ → 라운딩 토큰
3. 현재 프론트엔드 토큰 설정 읽기 (index.css 또는 tailwind config).
4. Diff 생성:
   - 추가된 토큰
   - 변경된 값
   - 삭제된 토큰
5. 사용자에게 diff 리포트 보여주고 적용 확인.
6. 승인 시 토큰 설정 업데이트.
7. 영향 받는 컴포넌트 목록 출력.
8. 빌드 확인.
</process>

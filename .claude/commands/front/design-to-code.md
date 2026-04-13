---
name: design-to-code
description: Figma 디자인을 FSD 구조에 맞는 React 컴포넌트 코드로 변환한다.
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
Figma 디자인을 읽어서 TeamFlow 프론트엔드의 FSD 아키텍처에 맞는 React + Tailwind 코드로 변환한다.
</objective>

<context>
입력: $ARGUMENTS

참조:
- @.claude/conventions/front/design-system.md
- @.claude/conventions/front/fsd-architecture.md
- @.claude/conventions/front/react-nextjs.md
- @.claude/skills/front/design-system/SKILL.md

출력:
- FSD 구조에 맞는 React 컴포넌트 파일들
</context>

<process>
1. 입력에서 Figma fileKey, nodeId 추출:
   - URL이면 파싱 (node-id의 `-` → `:` 변환)
   - 페이지명이면 `.planning/design/pages/{name}.md`에서 Figma 링크 조회
2. `get_design_context`로 디자인 컨텍스트 수신 (코드 + 스크린샷 + 힌트).
3. `get_code_connect_map`으로 기존 코드 매핑 확인.
4. 코드 힌트 분석:
   - Code Connect 스니펫 → 기존 shared/ui/ 컴포넌트 재사용
   - 디자인 토큰 → Tailwind 클래스 매핑
   - 레이아웃 → flex/grid 구조 결정
5. FSD 레이어 배치 결정:
   - 범용 UI → `shared/ui/`
   - 데이터 표시 → `entities/{name}/ui/`
   - 사용자 인터랙션 → `features/{name}/ui/`
   - 페이지 조합 → `pages/{name}/`
6. React + TypeScript + Tailwind 코드 생성:
   - 디자인 시스템 컨벤션 준수
   - Props 타입 정의
   - 접근성(a11y) 기본 속성 포함
7. 빌드 확인 (`tsc --noEmit`).
8. 새 컴포넌트에 대해 `add_code_connect_map`으로 Figma 매핑 등록.
</process>

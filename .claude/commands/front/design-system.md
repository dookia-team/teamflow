---
name: design-system
description: 디자인 시스템 초기화 — Figma 파일 생성, 토큰 정의, 기본 컴포넌트 셋 생성
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
TeamFlow 디자인 시스템을 초기화한다. Figma에 디자인 시스템 파일을 생성하고,
프론트엔드에 기본 컴포넌트 셋과 토큰 설정을 구축한다.
</objective>

<context>
참조:
- @.claude/conventions/front/design-system.md
- @.claude/skills/front/design-system/SKILL.md
- @.claude/conventions/front/react-nextjs.md

출력:
- Figma 디자인 시스템 파일 (Figma MCP)
- `frontend/src/shared/ui/` 기본 컴포넌트
- Tailwind 커스텀 설정
- `.planning/design/DESIGN-SYSTEM.md` 결정 기록
</context>

<process>
1. 사용자에게 프로젝트 브랜딩 방향 확인 (컬러, 톤앤매너).
2. Figma MCP `create_new_file`로 "TeamFlow Design System" 파일 생성.
3. `use_figma`로 Figma에 다음 구성요소 생성:
   - 컬러 팔레트 프레임 (Primary, Neutral, Semantic)
   - 타이포그래피 스케일 프레임
   - 스페이싱/라운딩 참조 프레임
   - 기본 컴포넌트 (Button, Input, Card, Modal, Badge, Avatar, Spinner)
4. `get_variable_defs`로 생성된 Figma Variables 확인.
5. `frontend/src/shared/ui/`에 React 컴포넌트 코드 생성:
   - `conventions/front/design-system.md`의 variant/size 규칙 준수
   - forwardRef + HTML 속성 확장
   - tailwind-merge 적용
6. Tailwind CSS에 커스텀 토큰 반영 (index.css 또는 tailwind config).
7. `add_code_connect_map`으로 Figma 컴포넌트 ↔ 코드 매핑.
8. `.planning/design/DESIGN-SYSTEM.md`에 결정 사항 기록.
9. 빌드 확인 (`npm run build`).
</process>

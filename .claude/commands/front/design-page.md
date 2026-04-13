---
name: design-page
description: 페이지 디자인을 Figma에 생성하고 디자인 결정을 기록한다.
argument-hint: "<페이지명 — 예: 랜딩 페이지, 로그인 페이지, 프로젝트 허브>"
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
지정된 페이지의 UI 디자인을 Figma에 생성한다.
와이어프레임 기획서와 디자인 시스템 토큰/컴포넌트를 기반으로 제작한다.
</objective>

<context>
페이지: $ARGUMENTS

참조:
- @docs/planning/requirements.md (와이어프레임 섹션)
- @.claude/conventions/front/design-system.md
- @.claude/skills/front/design-system/SKILL.md
- @.planning/design/DESIGN-SYSTEM.md (디자인 시스템 결정 사항)

출력:
- Figma 페이지 디자인
- `.planning/design/pages/{page-name}.md` 디자인 결정 기록
</context>

<process>
1. 와이어프레임 기획서에서 해당 페이지 레이아웃/요구사항 추출.
2. 기존 디자인 시스템 Figma 파일에서 `search_design_system`으로 사용 가능한 컴포넌트 확인.
3. 사용자에게 디자인 방향 확인 (레이아웃 선호, 특별한 요구 등).
4. `use_figma`로 Figma에 페이지 디자인 생성:
   - 디자인 시스템 토큰 준수 (컬러, 타이포, 스페이싱)
   - 기존 컴포넌트 재사용
   - 데스크탑 기준 (1440px 또는 1280px)
5. `get_screenshot`으로 결과 캡처.
6. `.planning/design/pages/{page-name}.md`에 기록:
   - 사용된 컴포넌트 목록
   - 디자인 결정 사항 (레이아웃, 컬러, 특이사항)
   - Figma 파일/노드 링크
7. 사용자에게 결과 공유 및 피드백 수렴.
</process>

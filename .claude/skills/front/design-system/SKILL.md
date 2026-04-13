# 디자인 시스템 스킬

TeamFlow 프론트엔드의 디자인 시스템을 관리하는 워크플로우 가이드.

---

## 역할

이 스킬은 다음을 담당한다:
1. **디자인 토큰 관리** — 컬러, 타이포, 스페이싱 토큰의 정의와 동기화
2. **컴포넌트 라이브러리** — shared/ui/ 컴포넌트의 생성과 유지보수
3. **Figma 연동** — Figma MCP를 통한 디자인 생성, 추출, 검증
4. **디자인-코드 일관성** — Figma 디자인과 구현 간 일관성 보장

---

## Figma MCP 활용 가이드

### 사용 가능한 도구

| 도구 | 용도 | 언제 사용 |
|------|------|-----------|
| `mcp__figma__create_new_file` | 새 Figma 파일 생성 | 디자인 시스템 초기화 시 |
| `mcp__figma__use_figma` | Figma Plugin API로 디자인 생성/수정 | 페이지 디자인 생성 시 |
| `mcp__figma__generate_figma_design` | 웹 페이지 → Figma 캡처 | 구현된 UI를 Figma에 기록 |
| `mcp__figma__get_design_context` | Figma 노드 → 코드 + 스크린샷 | 디자인→코드 변환 시 |
| `mcp__figma__get_screenshot` | Figma 노드 스크린샷 | 디자인 QC 비교 시 |
| `mcp__figma__get_variable_defs` | 디자인 토큰/변수 추출 | 토큰 동기화 시 |
| `mcp__figma__search_design_system` | 디자인 시스템 자산 검색 | 기존 컴포넌트 확인 |
| `mcp__figma__get_code_connect_map` | Code Connect 매핑 조회 | 컴포넌트-코드 매핑 확인 |
| `mcp__figma__add_code_connect_map` | Code Connect 매핑 추가 | 새 컴포넌트 매핑 시 |

### Figma URL 파싱

```
figma.com/design/:fileKey/:fileName?node-id=:nodeId
→ fileKey, nodeId(- → : 변환) 추출

figma.com/design/:fileKey/branch/:branchKey/:fileName
→ branchKey를 fileKey로 사용
```

---

## 워크플로우

### 1. 디자인 시스템 초기화 (/front:design-system)
```
1. Figma에 "TeamFlow Design System" 파일 생성
2. 컬러/타이포/스페이싱 토큰을 Figma Variables로 정의
3. 기본 컴포넌트(Button, Input, Card, Modal) Figma에 생성
4. frontend/src/shared/ui/에 React 컴포넌트 코드 생성
5. Tailwind 설정 업데이트 (커스텀 토큰)
6. Code Connect 매핑 등록
```

### 2. 페이지 디자인 생성 (/front:design-page)
```
1. 와이어프레임 기획서(docs/planning/requirements.md) 참조
2. 디자인 시스템 토큰/컴포넌트 기반으로 페이지 구성
3. use_figma로 Figma에 디자인 생성
4. get_screenshot으로 결과 캡처
5. .planning/design/에 디자인 결정 기록
```

### 3. 디자인 → 코드 변환 (/front:design-to-code)
```
1. Figma URL에서 fileKey, nodeId 추출
2. get_design_context로 코드 + 스크린샷 + 힌트 수신
3. FSD 구조에 맞게 코드 배치 결정:
   - 공통 UI → shared/ui/
   - 엔티티 표시 → entities/{name}/ui/
   - 인터랙션 → features/{name}/ui/
   - 페이지 조합 → pages/{name}/
4. 디자인 토큰을 Tailwind 클래스로 매핑
5. Code Connect에 기존 컴포넌트 매핑이 있으면 재사용
```

### 4. 토큰 동기화 (/front:design-tokens)
```
1. get_variable_defs로 Figma Variables 추출
2. 현재 Tailwind 설정과 비교
3. 변경된 토큰 식별 → diff 리포트 생성
4. Tailwind/CSS 업데이트 적용
5. 영향 받는 컴포넌트 목록 출력
```

### 5. 디자인 QC (/front:design-review)
```
1. Figma 디자인 스크린샷 (get_screenshot)
2. 구현된 UI와 비교 포인트:
   - 컬러 토큰 일치 여부
   - 스페이싱/사이즈 일치
   - 타이포그래피 매칭
   - 컴포넌트 사용 일관성
3. 불일치 항목을 severity 분류:
   - critical: 레이아웃/기능 깨짐
   - major: 토큰 불일치
   - minor: 미세 스페이싱 차이
4. 리포트를 .planning/design/에 기록
```

---

## 컴포넌트 생성 규칙

### variant 스타일 맵 패턴
```typescript
const variants = {
  primary: 'bg-primary-500 text-white hover:bg-primary-600',
  secondary: 'bg-gray-100 text-gray-700 hover:bg-gray-200',
  ghost: 'bg-transparent text-gray-700 hover:bg-gray-100',
  danger: 'bg-error text-white hover:bg-red-600',
} as const

const sizes = {
  sm: 'px-3 py-1.5 text-sm',
  md: 'px-4 py-2 text-base',
  lg: 'px-6 py-3 text-lg',
} as const
```

### 필수 export
```typescript
// shared/ui/index.ts — 배럴 파일
export { Button } from './Button'
export { Input } from './Input'
// ... 모든 컴포넌트
```

---

## 참조 문서
- 디자인 토큰/컴포넌트 컨벤션: `conventions/front/design-system.md`
- FSD 아키텍처: `conventions/front/fsd-architecture.md`
- React/Next.js 컨벤션: `conventions/front/react-nextjs.md`

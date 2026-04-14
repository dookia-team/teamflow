# Figma 디자인 컨벤션

> Figma MCP로 디자인 작업 시 따르는 규칙.
> Atomic Design 기반, Starter 플랜 환경에 맞춘 실용적 접근.

---

## 1. Atomic Design 계층

모든 디자인 요소는 아래 5계층 중 하나에 속한다.

| 계층 | 정의 | 예시 |
|------|------|------|
| **Atom** | 더 쪼갤 수 없는 최소 단위 | Button, Input, Badge, Avatar, Spinner, Icon |
| **Molecule** | Atom 2~3개 조합 | Google Login Button (Icon + Text), Toast (Bar + Text) |
| **Organism** | Molecule + Atom의 복합 구조 | Top Nav (Logo + Avatar + Dropdown), Login Card (Logo + Text + Button) |
| **Template** | Organism으로 구성된 페이지 뼈대 | Project Hub Layout (Nav + Header + Card Grid) |
| **Page** | 실제 데이터가 들어간 최종 화면 | 01. Landing Page, 04. Project Hub |

### 판단 기준
- 단독으로 의미가 있는가 → **Atom**
- 2~3개 Atom이 합쳐져야 의미가 되는가 → **Molecule**
- 페이지의 한 영역을 담당하는가 → **Organism**
- 반복 사용되는 레이아웃 구조인가 → **Template**

---

## 2. 네이밍

### 컴포넌트 레이어
```
{계층}/{Component}/{Variant}/{Size}

예시:
  Atom/Button/Primary/lg
  Atom/Input/Focus
  Atom/Badge/success
  Molecule/GoogleLoginButton/light
  Molecule/Toast/error
  Organism/TopNav
  Organism/LoginCard
```
- `/`로 계층 구분 (Figma에서 자동 그룹핑됨)
- Variant가 없으면 생략
- Size가 없으면 생략

### 페이지 프레임
```
{번호}. {페이지명}         → 01. Landing Page
{번호} Description        → 01 Description
```

### Breadcrumb (카드 헤더)
```
{카테고리}  ›  {섹션명}
  Foundation  ›  Colors
  Components  ›  Atoms (Button, Input, Badge)
```

---

## 3. 파일 구조

### 페이지 구성 (Starter 3페이지 제한)
| 페이지 | 용도 | 고정 여부 |
|--------|------|----------|
| 🎨 Foundation | 디자인 토큰 | 고정 |
| 🧩 Components | 재사용 컴포넌트 | 고정 |
| 📱 Sprint NN | 스프린트별 페이지 디자인 | 교체 |

### Foundation 섹션 분류
| 카드 | 포함 내용 | 분류 근거 |
|------|----------|----------|
| Colors | Primary, Neutral, Semantic, Background | 색상 토큰 전체 |
| Typography | Display ~ Overline | 하나의 타입 스케일 |
| Spacing & Grid | 4px 기반 간격 체계 | 레이아웃 수치 |
| Border Radius | none ~ full | 모서리 수치 |
| Shadows | xs ~ xl | 깊이 수치 |
| Icons | Outline / Solid | 아이콘 전체 |

### Components 섹션 분류
| 카드 | Atomic 계층 | 포함 내용 | 분류 근거 |
|------|------------|----------|----------|
| Atoms | Atom | Button, Input, Badge, Avatar, Spinner | 최소 단위 |
| Molecules | Molecule | Toast, Google Login Button | Atom 조합 |
| Organisms | Organism | Modal, Card (콘텐츠 포함) | 복합 구조 |

---

## 4. 섹션 나눔 기준

### 카드를 나누는 경우
- **도메인이 다르다** — Colors ≠ Typography
- **Atomic 계층이 다르다** — Atom ≠ Molecule ≠ Organism
- **독립적으로 참조된다** — 개별적으로 찾아볼 필요가 있는 단위

### 카드를 합치는 경우
- **같은 계층 + 같은 도메인** — Button과 Input은 둘 다 Form Atom
- **5개 이하 관련 요소** — 카드 하나에 수용 가능

### 카드가 너무 커지면
- 가로로 2컬럼 분할 (예: Icons의 Outline | Solid)
- 또는 서브 카드로 분리

---

## 5. 카드 프레임 규칙

### 원칙
1. 모든 콘텐츠는 **카드 프레임 안에** 존재한다
2. 배경 프레임을 따로 깔지 **않는다**
3. 카드를 이동하면 콘텐츠도 같이 움직인다 (**reparent 필수**)

### 카드 스타일
| 속성 | 값 | Tailwind 대응 |
|------|-----|--------------|
| 배경 | #FFFFFF | bg-white |
| 모서리 | 16px | rounded-2xl |
| 테두리 | #E5E7EB, 1px | border border-gray-200 |
| 그림자 | blur 8, #000 4%, y:2 | shadow-sm |

### 카드 내부 구조
```
┌─────────────────────────────────────────┐
│  Header Bar                             │
│  h: 44px                                │
│  bg: #F9FAFB (gray-50)                  │
│  border-bottom: #E5E7EB                 │
│  padding-left: 24px                     │
│  content: Breadcrumb (13px Medium)      │
├─────────────────────────────────────────┤
│  ┌───────────────────────────────────┐  │
│  │                                   │  │
│  │         콘텐츠 영역                │  │
│  │                                   │  │
│  └───────────────────────────────────┘  │
│                                         │
│  padding-top: 20px (헤더 아래부터)       │
│  padding-left: 30px                     │
│  padding-right: 30px                    │
│  padding-bottom: 30px                   │
└─────────────────────────────────────────┘
```

### 카드 크기 규칙 (잘림 방지)
1. 카드 프레임은 **반드시** 내부 콘텐츠 전체를 포함하는 크기여야 한다
2. 콘텐츠 생성/이동 후 **카드 크기를 재확인**한다
3. 카드 높이 = 콘텐츠 최하단 y + 콘텐츠 높이 + padding-bottom(30px)
4. 콘텐츠가 잘려 보이면 **즉시 카드를 키운다** — 잘린 상태로 두지 않는다
5. 의심스러우면 `get_screenshot`으로 확인한다

### 카드 배치
| 규칙 | 값 |
|------|-----|
| 방향 | 가로 우선 |
| 카드 간 가로 간격 | 60px |
| 행 간 세로 간격 | 60px |
| 너비 | 콘텐츠에 맞춤 (최소 600px) |
| 높이 | 콘텐츠에 맞춤 (잘리지 않도록) |

---

## 6. Sprint 페이지 규칙

### 페이지 프레임
| 속성 | 값 |
|------|-----|
| 크기 | 1440 × 900 (Desktop) |
| 배치 | 가로, 간격 60px |

### Page Description (필수)

모든 페이지 프레임 위에 Description을 **반드시** 배치한다.
페이지와 40px 간격. 2컬럼 레이아웃으로 공간을 효율적으로 사용한다.

```
┌──────────────────────────────────────────────┐
│  Page Description              (20px Bold)   │
│                                              │
│  좌측 (x:32)              우측 (x:740)       │
│                                              │
│  ① 페이지명 (REQ-ID)     ⑤ 사용자 동작       │
│  ② Route / 한줄 설명         클릭 → 이동      │
│  ③ 필요 데이터               입력 → 검증      │
│     • user: { id, .. }                       │
│     • projects: [..]    ⑥ API 호출           │
│  ④ 컴포넌트 구성             POST /api/...   │
│     • 01-1 GNB              Req/Res 형식     │
│     • 01-2 Hero                              │
│     • 01-3 CTA           ⑦ 상태 관리         │
│                              zustand: ...    │
│                              react-query: .. │
└──────────────────────────────────────────────┘
                    ↕ 40px
┌──────────────────────────────────────────────┐
│            페이지 디자인 (1440×900)            │
└──────────────────────────────────────────────┘
```

### 좌측 컬럼 (① ~ ④)
1. **페이지명 + 요구사항 ID** — `01. Landing Page (REQ-LAND-001)`
2. **Route + 접근 권한** — `/login (PublicRoute)`
3. **필요 데이터** — API에서 받는 데이터 구조 (타입 포함)
4. **컴포넌트 구성** — 번호별 UI 요소, Atomic 계층, variant/size 명시

### 우측 컬럼 (⑤ ~ ⑦)
5. **사용자 동작** — 트리거 → 결과 형식으로 작성
6. **API 호출** — 엔드포인트, Method, Request/Response body
7. **상태 관리** — 어떤 store/hook을 사용하는지

---

## 7. 디자인 토큰

### 버튼 (Atom/Button)
| Size | Height | Padding(좌우) | Font | Radius |
|------|--------|-------------|------|--------|
| sm | 32px | 12px | 13px Semi Bold | 10px |
| md | 40px | 16px | 14px Semi Bold | 10px |
| lg | 48px | 24px | 16px Semi Bold | 10px |

| Variant | Background | Text | Border | Shadow |
|---------|-----------|------|--------|--------|
| Primary | #4F46E5 | #FFFFFF | - | indigo glow |
| Secondary | #FFFFFF | #374151 | #E5E7EB | - |
| Ghost | transparent | #6B7280 | - | - |
| Danger | #EF4444 | #FFFFFF | - | - |

### 인풋 (Atom/Input)
| 속성 | Default | Focus | Error | Disabled |
|------|---------|-------|-------|----------|
| Height | 44px | 44px | 44px | 44px |
| Padding | 14px | 14px | 14px | 14px |
| Radius | 10px | 10px | 10px | 10px |
| Border | #E5E7EB | #6366F1 (2px) | #EF4444 | #E5E7EB |
| Background | #FFFFFF | #FFFFFF | #FFFFFF | #F9FAFB |
| Text | #111827 | #111827 | #111827 | #9CA3AF |

### 카드 (Organism/Card)
| 속성 | sm | md | lg |
|------|-----|-----|-----|
| Padding | 16px | 24px | 32px |
| Radius | 24px | 24px | 24px |
| Border | #E5E7EB | #E5E7EB | #E5E7EB |
| Shadow | shadow-sm | shadow-sm | shadow-sm |

### 모달 (Organism/Modal)
| 속성 | 값 |
|------|-----|
| Padding | 32~36px |
| Radius | 24px |
| Shadow | shadow-xl |
| Backdrop | rgba(0,0,0,0.5) |
| 닫기 | ESC, 배경 클릭, X 버튼 |

### 텍스트 스타일
| 용도 | Size | Weight | Color |
|------|------|--------|-------|
| Display | 48px | Bold | #111827 |
| H1 | 36px | Bold | #111827 |
| H2 | 30px | Bold | #111827 |
| H3 | 24px | Semi Bold | #1A1A1A |
| H4 | 20px | Semi Bold | #1A1A1A |
| H5 | 18px | Semi Bold | #1A1A1A |
| Body LG | 18px | Regular | #374151 |
| Body MD | 16px | Regular | #374151 |
| Body SM | 14px | Regular | #374151 |
| Caption | 12px | Medium | #6B7280 |
| Overline | 11px | Semi Bold | #6B7280 |

---

## 8. Tailwind 코딩 규칙

### @theme 토큰은 반드시 축약형으로 사용한다

Tailwind v4에서 `@theme`에 정의한 CSS 변수는 자동으로 유틸리티 클래스가 된다.
`var()` 문법을 직접 쓰지 않는다.

```
✗ 금지: px-[var(--spacing-fluid-page-x)]
✓ 사용: px-fluid-page-x

✗ 금지: text-[length:var(--text-fluid-display)]
✓ 사용: text-fluid-display

✗ 금지: rounded-[var(--radius-button)]
✓ 사용: rounded-button

✗ 금지: shadow-[var(--shadow-primary-glow)]
✓ 사용: shadow-primary-glow
```

### 규칙
- `@theme`에 `--spacing-*`, `--text-*`, `--radius-*`, `--shadow-*` 등을 정의하면 Tailwind가 자동으로 `px-*`, `text-*`, `rounded-*`, `shadow-*` 클래스를 생성한다
- **항상 축약형(canonical class)을 사용한다** — 긴 `var()` 문법은 코드 리뷰에서 반려한다
- 임의 값(`[...]`)은 토큰에 없는 일회성 값에만 사용한다

---

## 9. 반응형 규칙

### Breakpoint (Tailwind 기준)

| 이름 | 너비 | 용도 |
|------|------|------|
| Desktop | ≥ 1024px (`lg:`) | 기본 디자인 기준. Figma 1440px 프레임 |
| Tablet | 768px ~ 1023px (`md:`) | 2컬럼 → 1컬럼, 패딩 축소 |
| Mobile | < 768px | Sprint 2 이후 대응 예정 |

### 반응형 적용 원칙

1. **Mobile-first 코딩** — Tailwind 기본값이 모바일, `md:` `lg:`로 확장
2. **Fluid Scaling (clamp) 필수** — 타이포그래피, 간격, 이미지는 화면 크기에 따라 연속적으로 변화
   - 하드코딩된 `text-[40px] lg:text-[56px]` 대신 `text-[var(--text-fluid-display)]` 사용
   - 화면이 커지면 글자/간격/이미지도 자연스럽게 커지고, 작아지면 작아짐
3. **레이아웃 변경은 Breakpoint 사용**
   - Desktop: 2~3컬럼 그리드
   - Tablet: 1~2컬럼 그리드
   - 레이아웃 구조 변경(flex-row ↔ flex-col)만 breakpoint, 크기 변화는 clamp()
4. **Fluid 토큰 (index.css @theme에 정의)**
   - `--text-fluid-display`: 36px → 56px (히어로 타이틀)
   - `--text-fluid-h1`: 28px → 36px (페이지 제목)
   - `--text-fluid-h2`: 24px → 30px (섹션 제목)
   - `--text-fluid-h3`: 20px → 24px (서브 제목)
   - `--spacing-fluid-section`: 32px → 80px (섹션 간 간격)
   - `--spacing-fluid-page-x`: 32px → 80px (페이지 좌우 패딩)
5. **컴포넌트 반응형**
   - Button/Input: 크기 변경 없음 (동일 스펙)
   - Card: 그리드 열 수만 변경 (3col → 2col → 1col)
   - Modal: Desktop max-w-[520px] → Tablet w-full mx-4
   - 이미지/일러스트: max-w-full + 부모 컨테이너 flex-1로 자연 축소
6. **Figma에서는 Desktop만 디자인** — Fluid 대응은 코드에서 처리

### Sprint 페이지 Description에 반응형 명시

Description 우측 컬럼에 반응형 변경사항을 추가로 기재한다:
```
⑧ 반응형 (Tablet)
  • 2컬럼 → 1컬럼
  • 패딩 px-20 → px-8
  • 히어로 텍스트 축소
```

---

## 9. 향후 개선 (MCP 한도 풀리면)

공식 Figma MCP `use_figma`가 사용 가능해지면 아래를 적용한다:

1. **Figma Variables** — 색상/간격 토큰을 Variable Collection으로 등록
2. **Auto Layout 전환** — 수동 좌표 → Auto Layout + padding/gap
3. **Component Instance** — Sprint 페이지에서 Components의 Instance 사용
4. **Variant 등록** — 컴포넌트별 Property(variant, size, state) 설정

# Feature-Sliced Design (FSD) 아키텍처 가이드

## Next.js App Router + FSD 구조

Next.js의 `app/` 라우팅과 FSD를 결합한 구조입니다.

```
src/
├── app/                          # Next.js App Router (라우팅 전용)
│   ├── layout.tsx                #   루트 레이아웃
│   ├── page.tsx                  #   홈 페이지
│   ├── providers.tsx             #   글로벌 프로바이더 조합
│   ├── (auth)/
│   │   ├── login/page.tsx
│   │   └── register/page.tsx
│   ├── dashboard/
│   │   ├── layout.tsx
│   │   └── page.tsx
│   └── api/                      #   Route Handlers
│       └── [...]/route.ts
│
├── widgets/                      # 독립적인 UI 블록 (조합 레이어)
│   ├── header/
│   │   ├── ui/
│   │   │   └── Header.tsx
│   │   └── index.ts
│   ├── sidebar/
│   └── footer/
│
├── features/                     # 사용자 인터랙션 (행위 단위)
│   ├── auth/
│   │   ├── api/
│   │   │   └── login.ts
│   │   ├── model/
│   │   │   ├── useAuth.ts
│   │   │   └── authStore.ts
│   │   ├── ui/
│   │   │   ├── LoginForm.tsx
│   │   │   └── LogoutButton.tsx
│   │   └── index.ts
│   ├── create-post/
│   └── search/
│
├── entities/                     # 비즈니스 엔티티 (데이터 중심)
│   ├── user/
│   │   ├── api/
│   │   │   └── userApi.ts
│   │   ├── model/
│   │   │   ├── types.ts
│   │   │   └── userStore.ts
│   │   ├── ui/
│   │   │   ├── UserCard.tsx
│   │   │   └── UserAvatar.tsx
│   │   └── index.ts
│   ├── post/
│   └── product/
│
└── shared/                       # 공통 모듈 (프로젝트 무관)
    ├── api/
    │   ├── client.ts             #   API 클라이언트 (fetch 래퍼)
    │   └── types.ts              #   공통 API 타입
    ├── ui/
    │   ├── Button.tsx
    │   ├── Input.tsx
    │   ├── Modal.tsx
    │   └── Skeleton.tsx
    ├── lib/
    │   ├── cn.ts                 #   clsx + twMerge 유틸
    │   └── formatDate.ts
    ├── hooks/
    │   ├── useDebounce.ts
    │   └── useMediaQuery.ts
    ├── constants/
    │   └── routes.ts
    └── types/
        └── common.ts
```

---

## 레이어 규칙

### 의존성 방향 (핵심 규칙)

```
app → widgets → features → entities → shared
 ↓       ↓         ↓          ↓         ✕
 OK      OK        OK         OK     (아무것도 import 불가)
```

| 레이어 | import 가능 | import 불가 |
|--------|------------|------------|
| `app/` | widgets, features, entities, shared | - |
| `widgets/` | features, entities, shared | app |
| `features/` | entities, shared | app, widgets |
| `entities/` | shared | app, widgets, features |
| `shared/` | 없음 (외부 라이브러리만) | app, widgets, features, entities |

**같은 레이어 간 import는 금지**
- `features/auth` → `features/search` 불가
- 필요하면 상위 레이어(widgets)에서 조합

---

### 각 레이어 역할

#### `app/` — 앱 진입점
- Next.js 라우팅, 레이아웃, 프로바이더
- **비즈니스 로직 없음** — widgets/features를 조합만 함
- 페이지 컴포넌트는 최대한 얇게 유지

```tsx
// app/dashboard/page.tsx
import { DashboardHeader } from '@/widgets/dashboard-header';
import { RecentPosts } from '@/features/recent-posts';
import { UserStats } from '@/entities/user';

export default function DashboardPage() {
  return (
    <div>
      <DashboardHeader />
      <UserStats />
      <RecentPosts />
    </div>
  );
}
```

#### `widgets/` — 독립적인 UI 블록
- 여러 features/entities를 조합하는 큰 UI 블록
- Header, Sidebar, Footer, ProductList 등
- 자체 비즈니스 로직은 최소화

#### `features/` — 사용자 행위
- 하나의 유즈케이스를 담당
- 예: 로그인, 댓글 작성, 검색, 좋아요
- API 호출 + 상태 관리 + UI를 포함

#### `entities/` — 비즈니스 엔티티
- 도메인 모델 (User, Post, Product, Order)
- 엔티티의 타입, API, 표시용 컴포넌트
- 행위(action)는 포함하지 않음 — 그건 features에서

#### `shared/` — 공통 유틸리티
- 프로젝트 독립적인 코드
- UI 키트, 유틸 함수, 공통 훅, API 클라이언트
- 어떤 프로젝트에 가져다 놔도 동작해야 함

---

### Slice 내부 구조 (Segment)

각 slice(기능 단위 폴더)는 동일한 segment 구조를 따릅니다:

```
features/auth/
├── api/          # API 호출 함수
├── model/        # 상태 관리, 훅, 비즈니스 로직
├── ui/           # 컴포넌트
├── lib/          # slice 전용 유틸리티 (선택)
├── config/       # slice 전용 상수/설정 (선택)
└── index.ts      # Public API (배럴 파일)
```

### Public API (index.ts)

각 slice는 반드시 `index.ts`를 통해서만 외부에 노출합니다:

```tsx
// features/auth/index.ts
export { LoginForm } from './ui/LoginForm';
export { LogoutButton } from './ui/LogoutButton';
export { useAuth } from './model/useAuth';
export type { User } from './model/types';
```

```tsx
// 올바른 import
import { LoginForm, useAuth } from '@/features/auth';

// 잘못된 import (내부 경로 직접 접근 금지)
import { LoginForm } from '@/features/auth/ui/LoginForm';
```

---

## tsconfig.json 경로 별칭

```json
{
  "compilerOptions": {
    "baseUrl": ".",
    "paths": {
      "@/*": ["./src/*"],
      "@/widgets/*": ["./src/widgets/*"],
      "@/features/*": ["./src/features/*"],
      "@/entities/*": ["./src/entities/*"],
      "@/shared/*": ["./src/shared/*"]
    }
  }
}
```

---

## 새 기능 추가 시 판단 기준

```
"이 코드는 어디에 넣어야 하지?"

1. 특정 비즈니스 도메인과 무관한가?
   → shared/

2. 데이터 모델/엔티티를 표현하는가? (User, Product)
   → entities/

3. 사용자 액션/인터랙션인가? (로그인, 검색, 좋아요)
   → features/

4. 여러 feature/entity를 조합한 큰 UI 블록인가?
   → widgets/

5. 라우팅/레이아웃인가?
   → app/
```

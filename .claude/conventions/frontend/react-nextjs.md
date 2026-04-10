# React / Next.js 코드 컨벤션

## 프로젝트 구조

**FSD (Feature-Sliced Design) 아키텍처를 사용합니다.**
상세 구조는 `fsd-architecture.md`, 라이브러리 가이드는 `library-stack.md`를 참고하세요.

```
src/
├── app/          # Next.js App Router (라우팅 전용)
├── widgets/      # 독립적인 UI 블록 (Header, Sidebar)
├── features/     # 사용자 인터랙션 (로그인, 검색)
├── entities/     # 비즈니스 엔티티 (User, Product)
└── shared/       # 공통 모듈 (UI킷, 유틸, API 클라이언트)
```

## 네이밍 규칙

### 파일명
- 컴포넌트: `PascalCase.tsx` (예: `UserProfile.tsx`)
- 훅: `camelCase.ts` (예: `useAuth.ts`)
- 유틸리티: `camelCase.ts` (예: `formatDate.ts`)
- 타입: `camelCase.ts` (예: `user.ts`)
- 상수: `camelCase.ts` 또는 `SCREAMING_SNAKE_CASE.ts`

### 변수/함수
- 컴포넌트: `PascalCase` (예: `UserProfile`)
- 함수/변수: `camelCase` (예: `getUserData`)
- 상수: `SCREAMING_SNAKE_CASE` (예: `MAX_RETRY_COUNT`)
- 타입/인터페이스: `PascalCase` (예: `UserResponse`)
- enum: `PascalCase` (예: `UserRole`)

### 디렉토리
- 소문자 kebab-case (예: `user-profile/`, `auth-guard/`)

## 컴포넌트 작성 규칙

### 기본 패턴
```tsx
interface Props {
  title: string;
  onSubmit: (data: FormData) => void;
}

export default function ComponentName({ title, onSubmit }: Props) {
  // 1. hooks
  // 2. 상태/변수
  // 3. 핸들러
  // 4. effects
  // 5. render

  return <div>{title}</div>;
}
```

### 규칙
- 함수 컴포넌트 + `export default function` 사용
- Props 인터페이스는 컴포넌트 바로 위에 정의
- `React.FC` 사용하지 않음
- 구조 분해 할당으로 props 받기
- 조건부 렌더링 시 early return 패턴 사용

### Server vs Client 컴포넌트
```tsx
// 기본은 Server Component (별도 선언 불필요)
export default function ServerComponent() { ... }

// Client Component는 명시적으로 선언
'use client';
export default function ClientComponent() { ... }
```

- 기본적으로 Server Component 사용
- `useState`, `useEffect`, 이벤트 핸들러 사용 시에만 `'use client'`
- Client Component는 가능한 트리 하단에 배치

## 상태 관리

### 우선순위
1. **Server Component** — 가능하면 서버에서 데이터 처리
2. **URL 상태** — `useSearchParams`, `usePathname` (필터, 페이지네이션 등)
3. **React 기본** — `useState`, `useReducer` (로컬 상태)
4. **Context** — 테마, 인증 등 전역이 필요한 경우
5. **외부 라이브러리** — zustand, jotai 등 (복잡한 클라이언트 상태)

### 서버 상태
- `fetch` + React Server Component 우선
- 클라이언트에서 필요시 `@tanstack/react-query` 사용

## API 호출

### Server Component
```tsx
// app/users/page.tsx
export default async function UsersPage() {
  const users = await fetch(`${API_URL}/users`, {
    next: { revalidate: 60 },
  }).then(res => res.json());

  return <UserList users={users} />;
}
```

### Route Handler
```tsx
// app/api/users/route.ts
import { NextResponse } from 'next/server';

export async function GET(request: Request) {
  const data = await fetchUsers();
  return NextResponse.json(data);
}
```

## 스타일링

### 우선순위
1. **Tailwind CSS** — 기본 스타일링 도구
2. **CSS Modules** — Tailwind로 해결 어려운 경우
3. **styled-components** — 레거시 프로젝트 유지보수 시

### Tailwind 규칙
- 클래스 순서: layout → sizing → spacing → typography → visual → interactive
- 긴 클래스는 `cn()` 또는 `clsx()` 유틸리티 사용
- 반복되는 스타일은 `@apply`보다 컴포넌트 추출 선호

## 에러 처리

### Error Boundary
```tsx
// app/error.tsx
'use client';

export default function Error({ error, reset }: {
  error: Error & { digest?: string };
  reset: () => void;
}) {
  return (
    <div>
      <h2>문제가 발생했습니다</h2>
      <button onClick={reset}>다시 시도</button>
    </div>
  );
}
```

### Loading
```tsx
// app/loading.tsx
export default function Loading() {
  return <Skeleton />;
}
```

## Import 순서

```tsx
// 1. React/Next
import { useState } from 'react';
import Link from 'next/link';

// 2. 외부 라이브러리
import { clsx } from 'clsx';

// 3. 내부 모듈 (절대 경로)
import { Button } from '@/components/ui/Button';
import { useAuth } from '@/hooks/useAuth';
import { formatDate } from '@/lib/formatDate';
import type { User } from '@/types/user';

// 4. 상대 경로
import { SubComponent } from './SubComponent';

// 5. 스타일
import styles from './Component.module.css';
```

## TypeScript 규칙

- `any` 사용 금지 — `unknown` 사용 후 타입 좁히기
- API 응답 타입은 반드시 정의
- `as` 타입 단언 최소화 — 타입 가드 사용
- `interface` 우선 사용 (`type`은 유니온/인터섹션에)
- 제네릭 남용 금지 — 필요한 경우에만

## 커밋 메시지

```
feat: 사용자 프로필 페이지 추가
fix: 로그인 토큰 만료 처리 수정
refactor: API 클라이언트 모듈화
style: 헤더 반응형 레이아웃 적용
chore: eslint 설정 업데이트
docs: API 엔드포인트 문서 추가
test: 회원가입 폼 유효성 검사 테스트 추가
```

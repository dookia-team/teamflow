# React / Next.js 라이브러리 스택 가이드

## 추천 스택 요약

| 카테고리 | 라이브러리 | 선택 이유 |
|----------|-----------|----------|
| 프레임워크 | Next.js (App Router) | RSC, 라우팅, SSR/SSG 통합 |
| 언어 | TypeScript (strict) | 타입 안전성 필수 |
| 스타일링 | Tailwind CSS | 유틸리티 퍼스트, 빠른 개발 |
| UI 컴포넌트 | shadcn/ui | 복사 방식, 커스터마이징 자유 |
| 서버 상태 | @tanstack/react-query | 캐싱, 동기화, 에러 핸들링 |
| 클라이언트 상태 | zustand | 경량, 보일러플레이트 최소 |
| 폼 | react-hook-form + zod | 성능 좋고, 스키마 기반 검증 |
| HTTP 클라이언트 | ky | fetch 기반, 경량, 인터셉터 지원 |
| 날짜 | date-fns | 트리쉐이킹, 함수형 |
| 애니메이션 | framer-motion | 선언적 API, React 궁합 최고 |
| 아이콘 | lucide-react | 경량, shadcn/ui 기본 아이콘 |
| 테스트 | Vitest + Testing Library | 빠름, Vite 호환 |
| 린트/포맷 | ESLint + Prettier | 표준 |
| 패키지 매니저 | pnpm | 빠름, 디스크 효율적 |

---

## 카테고리별 상세 가이드

### 1. 스타일링 — Tailwind CSS + shadcn/ui

#### 왜 이 조합인가
- Tailwind: 유틸리티 클래스로 빠른 개발, 번들 사이즈 최적화
- shadcn/ui: 라이브러리가 아닌 **복사 방식** → `node_modules`에 의존 안 함, 완전한 커스터마이징

#### 설치
```bash
# Tailwind
pnpm add -D tailwindcss @tailwindcss/postcss postcss

# shadcn/ui 초기화
pnpm dlx shadcn@latest init

# 필요한 컴포넌트만 추가
pnpm dlx shadcn@latest add button input dialog
```

#### 유틸리티 함수
```tsx
// shared/lib/cn.ts
import { clsx, type ClassValue } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}
```

#### 사용 규칙
- shadcn/ui 컴포넌트는 `shared/ui/`에 배치
- Tailwind 클래스 순서: layout → sizing → spacing → typography → visual → interactive
- 조건부 스타일은 `cn()` 사용
- `@apply`보다 컴포넌트 추출 선호

---

### 2. 서버 상태 — @tanstack/react-query

#### 왜 사용하는가
- Server Component의 `fetch`로 부족한 경우 (실시간 동기화, 낙관적 업데이트, 무한스크롤)
- 캐싱, 자동 리페치, 에러/로딩 상태 관리

#### 설치
```bash
pnpm add @tanstack/react-query
```

#### 프로바이더 설정
```tsx
// app/providers.tsx
'use client';

import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useState } from 'react';

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(
    () =>
      new QueryClient({
        defaultOptions: {
          queries: {
            staleTime: 60 * 1000,       // 1분
            gcTime: 5 * 60 * 1000,      // 5분
            retry: 1,
            refetchOnWindowFocus: false,
          },
        },
      }),
  );

  return (
    <QueryClientProvider client={queryClient}>
      {children}
    </QueryClientProvider>
  );
}
```

#### FSD 배치 규칙
```tsx
// entities/user/api/userApi.ts
import { api } from '@/shared/api/client';
import type { User } from '../model/types';

export async function getUser(id: string): Promise<User> {
  return api.get(`users/${id}`).json();
}

export async function getUsers(): Promise<User[]> {
  return api.get('users').json();
}
```

```tsx
// entities/user/model/useUser.ts
import { useQuery } from '@tanstack/react-query';
import { getUser } from '../api/userApi';

export function useUser(id: string) {
  return useQuery({
    queryKey: ['user', id],
    queryFn: () => getUser(id),
  });
}
```

#### queryKey 컨벤션
```tsx
// 계층적으로 구성
['user']                    // 모든 유저
['user', userId]            // 특정 유저
['user', userId, 'posts']   // 특정 유저의 글
['posts', { status, page }] // 필터가 있는 목록
```

#### Server Component vs React Query 판단
```
데이터가 정적이거나 SEO 필요?
  → Server Component + fetch

실시간 동기화, 낙관적 업데이트, 무한스크롤 필요?
  → Client Component + React Query

둘 다 필요?
  → Server Component에서 초기 데이터 fetch
  → React Query의 initialData로 전달 (hydration)
```

---

### 3. 클라이언트 상태 — zustand

#### 왜 zustand인가
- Redux 대비 보일러플레이트 90% 감소
- 번들 사이즈 ~1KB
- React 외부에서도 사용 가능
- TypeScript 지원 우수

#### 설치
```bash
pnpm add zustand
```

#### FSD 배치 규칙
```tsx
// features/auth/model/authStore.ts
import { create } from 'zustand';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  login: (user: User) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  login: (user) => set({ user, isAuthenticated: true }),
  logout: () => set({ user: null, isAuthenticated: false }),
}));
```

#### 상태 관리 선택 기준
```
URL에 반영되어야 하는 상태? (필터, 페이지, 탭)
  → useSearchParams (URL 상태)

서버 데이터?
  → React Query

컴포넌트 내부에서만 쓰는 상태?
  → useState / useReducer

여러 컴포넌트에서 공유하는 클라이언트 상태?
  → zustand

테마, 인증 등 앱 전체 설정?
  → zustand 또는 Context
```

---

### 4. 폼 — react-hook-form + zod

#### 왜 이 조합인가
- react-hook-form: 비제어 컴포넌트 기반 → 리렌더링 최소화
- zod: 스키마 기반 검증 → API 응답 타입과 공유 가능

#### 설치
```bash
pnpm add react-hook-form zod @hookform/resolvers
```

#### 사용 패턴
```tsx
// features/auth/model/loginSchema.ts
import { z } from 'zod';

export const loginSchema = z.object({
  email: z.string().email('올바른 이메일을 입력하세요'),
  password: z.string().min(8, '8자 이상 입력하세요'),
});

export type LoginFormData = z.infer<typeof loginSchema>;
```

```tsx
// features/auth/ui/LoginForm.tsx
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { loginSchema, type LoginFormData } from '../model/loginSchema';

export function LoginForm() {
  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<LoginFormData>({
    resolver: zodResolver(loginSchema),
  });

  const onSubmit = async (data: LoginFormData) => {
    // API 호출
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      <input {...register('email')} />
      {errors.email && <p>{errors.email.message}</p>}

      <input type="password" {...register('password')} />
      {errors.password && <p>{errors.password.message}</p>}

      <button type="submit" disabled={isSubmitting}>
        로그인
      </button>
    </form>
  );
}
```

---

### 5. HTTP 클라이언트 — ky

#### 왜 ky인가
- fetch 기반 (네이티브 호환)
- axios보다 경량 (~3KB vs ~13KB)
- 인터셉터, 리트라이, 타임아웃 내장
- JSON 자동 파싱

#### 설치
```bash
pnpm add ky
```

#### API 클라이언트 설정
```tsx
// shared/api/client.ts
import ky from 'ky';

export const api = ky.create({
  prefixUrl: process.env.NEXT_PUBLIC_API_URL,
  timeout: 10000,
  hooks: {
    beforeRequest: [
      (request) => {
        const token = typeof window !== 'undefined'
          ? localStorage.getItem('token')
          : null;
        if (token) {
          request.headers.set('Authorization', `Bearer ${token}`);
        }
      },
    ],
    afterResponse: [
      async (_request, _options, response) => {
        if (response.status === 401) {
          // 토큰 만료 처리
        }
      },
    ],
  },
});
```

---

### 6. 날짜 — date-fns

#### 왜 date-fns인가
- 트리쉐이킹 지원 (사용한 함수만 번들링)
- 불변성 (원본 Date 변경 안 함)
- 함수형 API

#### 설치
```bash
pnpm add date-fns
```

#### 사용 예시
```tsx
// shared/lib/formatDate.ts
import { format, formatDistanceToNow } from 'date-fns';
import { ko } from 'date-fns/locale';

export function formatDate(date: Date | string) {
  return format(new Date(date), 'yyyy년 M월 d일', { locale: ko });
}

export function formatRelative(date: Date | string) {
  return formatDistanceToNow(new Date(date), {
    addSuffix: true,
    locale: ko,
  });
}
```

---

### 7. 애니메이션 — framer-motion

#### 설치
```bash
pnpm add framer-motion
```

#### 사용 규칙
- `'use client'` 필수 (Client Component에서만)
- 복잡한 애니메이션 설정은 `shared/lib/animations.ts`에 정의
- 간단한 전환은 인라인으로 사용

---

### 8. 테스트 — Vitest + Testing Library

#### 설치
```bash
pnpm add -D vitest @testing-library/react @testing-library/jest-dom @vitejs/plugin-react jsdom
```

#### FSD 배치 규칙
테스트 파일은 테스트 대상 옆에 배치:
```
features/auth/
├── ui/
│   ├── LoginForm.tsx
│   └── LoginForm.test.tsx     # 컴포넌트 테스트
├── model/
│   ├── useAuth.ts
│   └── useAuth.test.ts        # 훅 테스트
└── api/
    ├── authApi.ts
    └── authApi.test.ts         # API 테스트
```

---

### 9. 린트 & 포맷 — ESLint + Prettier

#### 설치
```bash
pnpm add -D eslint prettier eslint-config-prettier eslint-plugin-import
```

#### FSD Import 규칙 강제 (ESLint)
```js
// .eslintrc.js — FSD 레이어 의존성 규칙
module.exports = {
  rules: {
    'import/no-restricted-paths': [
      'error',
      {
        zones: [
          // shared는 다른 레이어 import 불가
          { target: './src/shared', from: './src/entities' },
          { target: './src/shared', from: './src/features' },
          { target: './src/shared', from: './src/widgets' },
          { target: './src/shared', from: './src/app' },
          // entities는 features, widgets import 불가
          { target: './src/entities', from: './src/features' },
          { target: './src/entities', from: './src/widgets' },
          // features는 widgets import 불가
          { target: './src/features', from: './src/widgets' },
        ],
      },
    ],
  },
};
```

---

## 새 프로젝트 초기화 체크리스트

```bash
# 1. Next.js 프로젝트 생성
pnpm create next-app@latest my-app --typescript --tailwind --eslint --app --src-dir

# 2. 핵심 라이브러리 설치
pnpm add zustand @tanstack/react-query ky zod react-hook-form @hookform/resolvers date-fns framer-motion lucide-react

# 3. 개발 의존성
pnpm add -D vitest @testing-library/react @testing-library/jest-dom prettier eslint-config-prettier

# 4. shadcn/ui 초기화
pnpm dlx shadcn@latest init

# 5. FSD 디렉토리 생성
mkdir -p src/{widgets,features,entities,shared/{api,ui,lib,hooks,constants,types}}

# 6. dev-assistant 연결
bash ~/dev-assistant/scripts/setup.sh .
```

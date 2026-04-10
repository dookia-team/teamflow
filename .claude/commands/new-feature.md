# /new-feature 커맨드

새로운 기능을 생성합니다. FSD 아키텍처에 맞춰 구조를 잡고 단계별로 구현합니다.

## 참조
- 아키텍처: `conventions/frontend/fsd-architecture.md`
- 라이브러리: `conventions/frontend/library-stack.md`
- Git 워크플로우: `skills/git-workflow/SKILL.md`

## 입력
$ARGUMENTS: 기능명 또는 설명 (예: "사용자 인증", "상품 검색", "결제 프로세스")

---

## 전체 워크플로우

```
1. 요구사항 분석     → 기능 범위, 데이터 모델 정의
2. FSD 배치 결정     → 어떤 레이어에 무엇을 넣을지
3. 타입 정의         → entities/{name}/model/types.ts
4. API 레이어        → entities/{name}/api/
5. 상태 관리         → features/{name}/model/
6. UI 컴포넌트       → features/{name}/ui/ + entities/{name}/ui/
7. 페이지 조합       → app/{route}/page.tsx
8. 테스트            → 각 파일 옆에 .test.ts(x)
```

---

## Phase 1: 분석 및 설계

### 1-1. 요구사항 분석

```markdown
## 기능 분석

### 목적
- 이 기능이 해결하는 문제는?
- 누가 사용하는가?

### 데이터 모델
- 필요한 타입/인터페이스
- API 엔드포인트
- 상태 관리 필요 여부

### UI 구성
- 필요한 페이지/라우트
- 컴포넌트 목록
- Server vs Client 컴포넌트 구분
```

### 1-2. FSD 레이어 배치 결정

```
"이 코드는 어디에?"

데이터 모델 (User, Product 등)     → entities/
사용자 행위 (로그인, 검색, 좋아요)  → features/
큰 UI 블록 (Header, ProductList)   → widgets/
공통 유틸/UI킷                     → shared/
라우팅/레이아웃                    → app/
```

---

## Phase 2: 타입 정의

### entities 타입

```tsx
// entities/{name}/model/types.ts
export interface {Entity} {
  id: string;
  // ... 필드 정의
  createdAt: string;
  updatedAt: string;
}

// API 요청/응답 타입
export interface {Entity}ListParams {
  page?: number;
  limit?: number;
  search?: string;
}

export interface {Entity}ListResponse {
  items: {Entity}[];
  total: number;
  page: number;
}

export interface Create{Entity}Request {
  // 생성 요청 필드
}
```

### Zod 스키마 (폼 검증용)

```tsx
// features/{name}/model/schema.ts
import { z } from 'zod';

export const create{Entity}Schema = z.object({
  field1: z.string().min(1, '필수 항목입니다'),
  field2: z.string().email('올바른 이메일을 입력하세요'),
});

export type Create{Entity}FormData = z.infer<typeof create{Entity}Schema>;
```

---

## Phase 3: API 레이어

```tsx
// entities/{name}/api/{name}Api.ts
import { api } from '@/shared/api/client';
import type { {Entity}, {Entity}ListParams, {Entity}ListResponse } from '../model/types';

export async function get{Entity}(id: string): Promise<{Entity}> {
  return api.get(`{endpoint}/${id}`).json();
}

export async function get{Entity}List(params: {Entity}ListParams): Promise<{Entity}ListResponse> {
  return api.get('{endpoint}', { searchParams: params }).json();
}

export async function create{Entity}(data: Create{Entity}Request): Promise<{Entity}> {
  return api.post('{endpoint}', { json: data }).json();
}
```

---

## Phase 4: 상태 관리

### React Query 훅 (서버 상태)

```tsx
// entities/{name}/model/use{Entity}.ts
import { useQuery } from '@tanstack/react-query';
import { get{Entity}, get{Entity}List } from '../api/{name}Api';

export function use{Entity}(id: string) {
  return useQuery({
    queryKey: ['{name}', id],
    queryFn: () => get{Entity}(id),
  });
}

export function use{Entity}List(params: {Entity}ListParams) {
  return useQuery({
    queryKey: ['{name}', 'list', params],
    queryFn: () => get{Entity}List(params),
  });
}
```

### Zustand 스토어 (클라이언트 상태, 필요시)

```tsx
// features/{name}/model/{name}Store.ts
import { create } from 'zustand';

interface {Name}State {
  // 상태 정의
  // 액션 정의
}

export const use{Name}Store = create<{Name}State>((set) => ({
  // 초기 상태 및 액션 구현
}));
```

---

## Phase 5: UI 컴포넌트

### Entity UI (데이터 표시용)

```tsx
// entities/{name}/ui/{Entity}Card.tsx
import type { {Entity} } from '../model/types';

interface Props {
  data: {Entity};
}

export function {Entity}Card({ data }: Props) {
  return (
    <div>
      {/* 엔티티 표시 */}
    </div>
  );
}
```

### Feature UI (사용자 인터랙션)

```tsx
// features/{name}/ui/{Action}Form.tsx
'use client';

import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { schema, type FormData } from '../model/schema';

export function {Action}Form() {
  const { register, handleSubmit, formState: { errors, isSubmitting } } = useForm<FormData>({
    resolver: zodResolver(schema),
  });

  const onSubmit = async (data: FormData) => {
    // API 호출
  };

  return (
    <form onSubmit={handleSubmit(onSubmit)}>
      {/* 폼 필드 */}
    </form>
  );
}
```

### Public API (배럴 파일)

```tsx
// entities/{name}/index.ts
export { {Entity}Card } from './ui/{Entity}Card';
export { use{Entity}, use{Entity}List } from './model/use{Entity}';
export type { {Entity} } from './model/types';

// features/{name}/index.ts
export { {Action}Form } from './ui/{Action}Form';
export { use{Name}Store } from './model/{name}Store';
```

---

## Phase 6: 페이지 조합

```tsx
// app/{route}/page.tsx
import { {Entity}Card } from '@/entities/{name}';
import { {Action}Form } from '@/features/{name}';

export default async function {Name}Page() {
  // Server Component에서 초기 데이터 fetch
  const data = await fetch(`${API_URL}/{endpoint}`).then(res => res.json());

  return (
    <div>
      <h1>페이지 제목</h1>
      <{Action}Form />
      {data.items.map(item => (
        <{Entity}Card key={item.id} data={item} />
      ))}
    </div>
  );
}
```

---

## Phase 7: 테스트

```tsx
// features/{name}/ui/{Action}Form.test.tsx
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { {Action}Form } from './{Action}Form';

describe('{Action}Form', () => {
  it('폼이 렌더링된다', () => {
    render(<{Action}Form />);
    expect(screen.getByRole('form')).toBeInTheDocument();
  });

  it('유효성 검사가 동작한다', async () => {
    render(<{Action}Form />);
    await userEvent.click(screen.getByRole('button', { name: /제출/ }));
    expect(screen.getByText(/필수 항목/)).toBeInTheDocument();
  });
});
```

---

## 생성되는 파일 구조

```
src/
├── entities/{name}/
│   ├── api/
│   │   └── {name}Api.ts           # API 호출 함수
│   ├── model/
│   │   ├── types.ts               # 타입 정의
│   │   └── use{Entity}.ts         # React Query 훅
│   ├── ui/
│   │   └── {Entity}Card.tsx       # 표시 컴포넌트
│   └── index.ts                   # Public API
├── features/{name}/
│   ├── model/
│   │   ├── schema.ts              # Zod 스키마
│   │   └── {name}Store.ts         # Zustand (필요시)
│   ├── ui/
│   │   ├── {Action}Form.tsx       # 인터랙션 컴포넌트
│   │   └── {Action}Form.test.tsx  # 테스트
│   └── index.ts                   # Public API
└── app/{route}/
    └── page.tsx                   # 페이지 조합
```

## 체크리스트

### 설계
- [ ] 요구사항 분석 완료
- [ ] FSD 레이어 배치 결정
- [ ] Server vs Client 컴포넌트 구분

### 구현
- [ ] 타입 정의
- [ ] API 레이어
- [ ] 상태 관리 (React Query / Zustand)
- [ ] UI 컴포넌트
- [ ] 페이지 조합
- [ ] index.ts (Public API)

### 검증
- [ ] TypeScript 에러 없음
- [ ] 빌드 성공 (`npm run build`)
- [ ] 린트 통과 (`npm run lint`)
- [ ] 테스트 작성 및 통과
- [ ] FSD 레이어 의존성 규칙 준수 확인

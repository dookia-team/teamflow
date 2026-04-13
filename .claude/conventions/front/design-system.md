# TeamFlow 디자인 시스템 컨벤션

---

## 1. 컬러 팔레트

### Primary (브랜드)
| 토큰 | 값 | 용도 |
|------|-----|------|
| `primary-50` | `#EEF2FF` | 배경 하이라이트 |
| `primary-100` | `#E0E7FF` | 호버 배경 |
| `primary-200` | `#C7D2FE` | 비활성 보더 |
| `primary-400` | `#818CF8` | 보조 액센트 |
| `primary-500` | `#6366F1` | **기본 브랜드 컬러** |
| `primary-600` | `#4F46E5` | 호버 상태 |
| `primary-700` | `#4338CA` | 액티브/프레스 |
| `primary-900` | `#312E81` | 다크 텍스트 |

### Neutral (그레이스케일)
| 토큰 | 값 | 용도 |
|------|-----|------|
| `gray-50` | `#F9FAFB` | 페이지 배경 |
| `gray-100` | `#F3F4F6` | 카드 배경 |
| `gray-200` | `#E5E7EB` | 보더, 구분선 |
| `gray-300` | `#D1D5DB` | 비활성 보더 |
| `gray-400` | `#9CA3AF` | 플레이스홀더 |
| `gray-500` | `#6B7280` | 보조 텍스트 |
| `gray-700` | `#374151` | 본문 텍스트 |
| `gray-900` | `#111827` | 제목 텍스트 |

### Semantic (의미)
| 토큰 | 값 | 용도 |
|------|-----|------|
| `success` | `#10B981` | 성공, 완료 |
| `warning` | `#F59E0B` | 경고, 주의 |
| `error` | `#EF4444` | 에러, 삭제 |
| `info` | `#3B82F6` | 정보, 링크 |

---

## 2. 타이포그래피

| 토큰 | 크기 | 줄간격 | 무게 | 용도 |
|------|------|--------|------|------|
| `text-xs` | 12px | 16px | 400 | 캡션, 뱃지 |
| `text-sm` | 14px | 20px | 400 | 보조 텍스트, 라벨 |
| `text-base` | 16px | 24px | 400 | 본문 |
| `text-lg` | 18px | 28px | 500 | 강조 본문 |
| `text-xl` | 20px | 28px | 600 | 서브 제목 |
| `text-2xl` | 24px | 32px | 700 | 섹션 제목 |
| `text-3xl` | 30px | 36px | 700 | 페이지 제목 |
| `text-4xl` | 36px | 40px | 800 | 히어로 제목 |
| `text-5xl` | 48px | 1 | 900 | 랜딩 히어로 |

폰트 패밀리: `system-ui, -apple-system, sans-serif` (기본 시스템 폰트)

---

## 3. 스페이싱 (4px 기반)

| 토큰 | 값 | 용도 |
|------|-----|------|
| `spacing-1` | 4px | 아이콘 내부 패딩 |
| `spacing-2` | 8px | 인라인 요소 간격 |
| `spacing-3` | 12px | 컴팩트 패딩 |
| `spacing-4` | 16px | 기본 패딩 |
| `spacing-5` | 20px | 카드 패딩 |
| `spacing-6` | 24px | 섹션 간격 |
| `spacing-8` | 32px | 큰 섹션 간격 |
| `spacing-10` | 40px | 페이지 여백 |
| `spacing-12` | 48px | 히어로 여백 |
| `spacing-16` | 64px | 섹션 구분 |

---

## 4. 라운딩 (Border Radius)

| 토큰 | 값 | 용도 |
|------|-----|------|
| `rounded-sm` | 4px | 뱃지, 태그 |
| `rounded` | 8px | 인풋, 작은 카드 |
| `rounded-lg` | 12px | 버튼, 드롭다운 |
| `rounded-xl` | 16px | 카드 |
| `rounded-2xl` | 20px | 모달, 큰 카드 |
| `rounded-full` | 9999px | 아바타, 원형 버튼 |

---

## 5. 그림자 (Shadow)

| 토큰 | 용도 |
|------|------|
| `shadow-sm` | 인풋 포커스 |
| `shadow` | 드롭다운, 팝오버 |
| `shadow-md` | 카드 |
| `shadow-lg` | 모달 |
| `shadow-xl` | 토스트/알림 |

---

## 6. 컴포넌트 규칙

### Props 네이밍
```
size: 'sm' | 'md' | 'lg'           — 크기
variant: 'primary' | 'secondary' | 'ghost' | 'danger'  — 스타일 변형
disabled: boolean                    — 비활성화
className: string                    — 추가 스타일 (Tailwind merge)
```

### 파일 구조
```
shared/ui/
├── Button.tsx
├── Input.tsx
├── Card.tsx
├── Modal.tsx
├── Badge.tsx
├── Avatar.tsx
├── Spinner.tsx
└── index.ts          ← 배럴 파일
```

### Tailwind Merge 규칙
- 외부에서 전달된 `className`은 항상 컴포넌트 기본 스타일을 오버라이드할 수 있어야 한다.
- `tailwind-merge` 사용하여 클래스 충돌 해결.

### 컴포넌트 작성 원칙
- forwardRef 사용하여 ref 전달 가능하게
- 기본 HTML 속성 확장 (ButtonHTMLAttributes 등)
- variant/size별 스타일은 객체 맵으로 관리
- 기본값: `size="md"`, `variant="primary"`

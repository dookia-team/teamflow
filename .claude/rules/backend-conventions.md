# 백엔드 코드 컨벤션 (규칙 전용)

> **이 파일은 Spring Boot 백엔드의 범용 규칙·컨벤션만 포함합니다.**
> 코드 예시는 `backend-java-patterns.md`, 디자인 패턴은 `backend-design-patterns.md`를 참고하세요.

---

## 레이어별 책임 원칙

| 레이어 | 책임 | 금지 사항 |
|--------|------|-----------|
| **Controller** | 요청 수신 + 응답 반환 | 비즈니스 로직 금지 |
| **Service** | 비즈니스 로직 | DB 직접 접근 금지 (Repository 경유) |
| **Repository** | 쿼리 실행 | 비즈니스 로직 금지 |
| **Domain (Entity)** | 상태 보유 + 도메인 메서드 캡슐화 | 요청/응답 어노테이션 금지 |

- Controller → Service → Repository 단방향 의존. 순환 의존 금지.
- Entity를 API 응답으로 직접 반환 금지. 반드시 DTO로 변환.

---

## 명명 규칙

| 대상 | 규칙 | 예시 |
|------|------|------|
| 클래스명 | PascalCase | `ProductService` |
| 메서드/변수명 | camelCase | `getById`, `productList` |
| 상수 | UPPER_SNAKE_CASE | `MAX_PAGE_SIZE` |
| 패키지명 | 소문자, 점 구분 | `com.company.service.controller` |

---

## DTO Record 규칙

```
규칙 1. dto/request/, dto/response/ 하위 폴더 구조 사용 금지.

규칙 2. 기능 패키지 내 dto/{Domain}Dto.java 파일 하나에 inner record를 모두 선언.
        → product/dto/ProductDto.java    (CreateRequest, Response 등)
        → order/dto/OrderDto.java        (CreateRequest, Response 등)

규칙 3. Controller / Service에 직접 record 선언 금지.
        항상 dto/{Domain}Dto.java를 참조한다.

규칙 4. record 유효성 검증은 파라미터에 직접 어노테이션 적용.
        public record CreateRequest(@NotBlank String name, @Positive BigDecimal price) {}

규칙 5. Entity → Record 변환은 record 내부 static 팩토리 메서드로 처리.
        public static Response from(Product product) { return new Response(...); }

규칙 6. record는 불변(immutable) — Setter, Lombok @Builder 사용 금지.
```

---

## API 설계 컨벤션

### REST API 규칙

```
GET    /api/{resource}          목록 조회
GET    /api/{resource}/{id}     단건 조회
POST   /api/{resource}          생성
PUT    /api/{resource}/{id}     전체 수정
PATCH  /api/{resource}/{id}     부분 수정
DELETE /api/{resource}/{id}     삭제
```

### 응답 포맷

- 성공: `{ success, data, message, timestamp }`
- 실패: `{ success: false, message, timestamp }`
- 모든 응답은 `ApiResponse<T>`로 래핑

---

## Service 구조 판단 기준

```
아래 조건 중 하나라도 해당하면 → 인터페이스 + 구현체
모두 해당하지 않으면         → 구체 클래스 (기본)

[인터페이스 + 구현체 조건]
1. 동일 역할의 구현이 2개 이상 존재하거나 예정된 경우
2. 런타임에 구현체를 교체하거나 선택해야 하는 경우
3. 외부 시스템 연동을 추상화하여 테스트 대체(Mock)가 필요한 경우

[구체 클래스 조건 — 위 조건에 해당하지 않는 모든 경우]
  → 일반 CRUD, 단일 비즈니스 로직 등
  → "혹시 나중에 바뀔 수도 있으니까" 같은 추측성 분리 금지
```

---

## Spring 컨벤션

- `@Transactional` — 쓰기 메서드에 적용. 클래스 레벨 `@Transactional` + 읽기 메서드에 `@Transactional(readOnly = true)`.
- 생성자 주입 — `@RequiredArgsConstructor` 사용. `@Autowired` 필드 주입 금지.
- `Optional` — 반환 타입에만 사용. 필드, 파라미터에 사용 금지.

---

## 코드 포매팅 규칙

- **메서드 시그니처 개행 금지** — 파라미터가 과도하게 긴 경우(라인 120자 초과 등)가 아니면 시그니처를 한 줄로 유지한다.
파라미터가 2~3개인 평범한 메서드를 굳이 여러 줄로 쪼개지 않는다.
- 메서드 호출도 동일 — 특별히 길거나 가독성이 떨어지는 경우에만 개행한다.

```java
// 권장
public ResponseEntity<ApiResponse<UsageDto.Response>> getHistory(int page, int size) { ... }

// 지양 (불필요한 개행)
public ResponseEntity<ApiResponse<UsageDto.Response>> getHistory(
    int page,
    int size
) { ... }
```

---

## 예외 처리 규칙

- 엔티티마다 별도 예외 클래스 생성 금지. 공통 `EntityNotFoundException` 하나를 두고 엔티티명을 파라미터로 전달.
- 모든 예외는 `GlobalExceptionHandler`에서 일괄 처리. 각 Controller에 예외 처리 산재 금지.

---

## 테스트 규칙

### FIRST 원칙

| 원칙 | 핵심 |
|------|------|
| **F**ast | 외부 의존성 최소화, Mock/인메모리 활용 |
| **I**solated | 테스트 간 상태 공유 금지 |
| **R**epeatable | 환경 무관하게 동일 결과 |
| **S**elf-validating | assert로 명확한 pass/fail |
| **T**imely | 기능 구현과 동시에 작성 |

### BE 테스트 도구

| 어노테이션 | 용도 |
|------------|------|
| `@ExtendWith(MockitoExtension.class)` | Service 단위 테스트 |
| `@WebMvcTest` | Controller 통합 테스트 |
| `@DataJpaTest` | Repository 슬라이스 테스트 |
| `@SpringBootTest` | 풀 통합 테스트 |

- 테스트 없는 기능 구현 금지.

---

## 디자인 패턴 적용 규칙

> 레이어별 패턴 목록, 적용 시점, 판단 기준: `.claude/rules/backend-design-patterns.md`

- 패턴 없이 절차적으로 작성된 코드는 리뷰에서 반려한다.
- 조건 미해당 시 단순 구현 유지. 과도한 패턴 적용 금지.

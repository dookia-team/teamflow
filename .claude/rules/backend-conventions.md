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

### 컨트롤러 반환 타입

**`ResponseEntity<ApiResponse<T>>` 이중 래핑 금지. `ApiResponse<T>` 직접 반환을 기본으로 한다.**

```java
// 권장
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public ApiResponse<WorkspaceDto.Response> create(...) {
    return ApiResponse.success(workspaceService.create(...));
}

@GetMapping("/{no}")
public ApiResponse<WorkspaceDto.Response> get(@PathVariable Long no) {
    return ApiResponse.success(workspaceService.get(no));
}

// 금지 (이중 래핑)
public ResponseEntity<ApiResponse<WorkspaceDto.Response>> create(...) {
    return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(...));
}
```

**판단 기준**
- 상태코드는 `@ResponseStatus`로 선언. 기본 200 외에는 `@ResponseStatus(HttpStatus.CREATED)` 등 명시.
- 에러 응답은 `GlobalExceptionHandler`가 일괄 처리. 컨트롤러에서 직접 에러 분기 금지.

**`ResponseEntity<T>` 가 정당한 예외 케이스 (이때만 사용 허용)**
1. `Set-Cookie` 등 응답 헤더를 동적으로 추가해야 할 때 (예: `AuthController` 의 RT 쿠키 발급)
2. 동일 메서드에서 200/204/302 등 **런타임 분기**가 필요할 때
3. 파일 다운로드 등 `Content-Disposition` 헤더를 메서드 안에서 결정해야 할 때

위 경우에도 본문 envelope 은 `ApiResponse<T>` 그대로 사용한다 → `ResponseEntity<ApiResponse<T>>` 형태 유지.

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
- 생성자 주입 — **`@RequiredArgsConstructor` 사용을 기본**으로 한다. 모든 필드는 `private final` 로 선언하고, 명시적 생성자는 작성하지 않는다. **production 코드(`src/main`) 에서는 `@Autowired` 필드 주입 금지.**
  - 예외: 생성자 본문에 **변환 로직** (예: `List → Map` 정규화) 이 필요한 경우에만 명시적 생성자 작성 허용.
  - 예외: 부모 클래스 생성자 호출 (`super(...)`) 만 필요한 사용자 정의 예외 클래스.
  - **테스트 코드 예외**: `@WebMvcTest`, `@DataJpaTest`, `@SpringBootTest` 등 Spring 슬라이스/통합 테스트 클래스는 JUnit 이 인스턴스를 생성하므로 `@Autowired` 필드 주입을 허용한다 (관용적 패턴). 단위 테스트(`@ExtendWith(MockitoExtension.class)`) 에서는 `@InjectMocks` 사용.
- `Optional` — 반환 타입에만 사용. 필드, 파라미터에 사용 금지.
- `var` 사용 금지 — 지역변수 타입은 항상 명시적으로 선언한다. 코드 리뷰·diff·GitHub UI·콘솔 로그 등 IDE 외 환경에서도 타입을 즉시 파악할 수 있어야 한다.

```java
// 권장
LoginResult result = authService.login(request, userAgent, ipAddress);
List<ProjectDto.SummaryResponse> list = projectService.listInWorkspace(1L, 2L);

// 금지
var result = authService.login(request, userAgent, ipAddress);
var list = projectService.listInWorkspace(1L, 2L);
```

---

## 공유 인스턴스 (Spring Bean 싱글톤) 규칙

> 상세 예시·판단 흐름·금지/권장 패턴은 **`backend-bean-reuse.md`** 참조.

**Thread-safe 하게 재사용 가능한 컴포넌트는 절대 서비스 내부에서 직접 생성하지 않는다. 반드시 빈으로 등록하고 생성자 주입으로 받아 사용한다.**

| 컴포넌트 | 처리 방식 | 위치 |
|----------|----------|------|
| `ObjectMapper` | Spring Boot 자동 설정 빈 주입 | (별도 정의 불필요) |
| `RestClient` / `RestTemplate` / `WebClient` | Builder 로 빌드한 단일 빈 주입 | `config/HttpClientConfig` |
| `CorsConfigurationSource` | `CorsProperties` 와 함께 빈 등록 | `config/CorsConfig` |
| `PasswordEncoder`, `Clock`, `JwtParser` 등 | 단일 빈으로 정의 후 주입 | `config/` 또는 도메인 `Config` |

환경별로 달라지는 값은 `@ConfigurationProperties` record + `@EnableConfigurationProperties` 로 묶어 빈 생성 시점에 주입한다.

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

## 객체 변환 규칙 (MapStruct vs 수동 매핑)

> 상세 절차·예시·Gradle 설정은 **`backend-mapping.md`** 참조.

객체 ↔ 객체 변환은 우선순위 순서로 다음 **세 가지 경로** 중 하나로 처리한다:

| # | 경로 | 적용 조건 |
|---|------|-----------|
| 1 | **도메인 팩토리** — `Entity.createFromXxx(...)` | 비즈니스 의미가 담긴 생성 (기본 상태 부여, 외부 식별자 변환 등) |
| 2 | **record 내부 정적 팩토리** — `Response.of(entity, ...)` | Entity + 외부 파라미터(count, list, role 등) 결합 |
| 3 | **MapStruct** — `{domain}/mapper/{Domain}Mapper` | 비즈니스 로직 0 + 외부 파라미터 0 + 순수 1:1 매핑일 때만 |

판단 흐름:

```
1) 비즈니스 로직 있나? → YES → 도메인 팩토리 (1번)
2) 외부 파라미터 결합? → YES → record `of()` (2번)
3) 순수 1:1 매핑?     → YES → MapStruct (3번)
```

핵심 금기 (어기면 리뷰 반려):
- 도메인 팩토리를 분기별로 분리 (`createFromGoogle` + `createFromKakao`) — enum 파라미터로 일반화한다.
- MapStruct 매퍼에 분기·계산·검증 로직 넣기 — 변환 책임만 진다.
- 동일 변환을 record `from()` 과 MapStruct 양쪽에 두기 — 진입점 단일화.

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

### TDD 순서 철칙 (domain/API 구현 시 반드시 준수)

```
RepositoryTest → Repository(+Entity) → ServiceTest → Service → ControllerTest → Controller(+DTO)
```

- 각 단계는 반드시 **테스트 먼저 작성 → RED → 최소 구현 → GREEN → 리팩터** 순서.
- 프로덕션 코드는 실패하는 테스트 없이 작성하지 않는다.
- 위반 시 즉시 중단 후 삭제 또는 보충 테스트로 복구. 세부 절차는 `.claude/rules/testing.md` 참조.

---

## 디자인 패턴 적용 규칙

> 레이어별 패턴 목록, 적용 시점, 판단 기준: `.claude/rules/backend-design-patterns.md`

- 패턴 없이 절차적으로 작성된 코드는 리뷰에서 반려한다.
- 조건 미해당 시 단순 구현 유지. 과도한 패턴 적용 금지.

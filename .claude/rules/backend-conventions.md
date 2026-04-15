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
- 생성자 주입 — `@RequiredArgsConstructor` 사용. `@Autowired` 필드 주입 금지.
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

객체 ↔ 객체 변환은 **세 가지 경로** 중 하나로 처리한다. 우선순위 순서:

### 1) 도메인 팩토리 — `Entity.createFromXxx(...)` (최우선)

**항상 도메인 엔티티 안에 둔다. MapStruct로 옮기지 않는다.**

```
조건:
- 비즈니스 의미가 담긴 생성 (기본 상태 부여, 식별자 변환 등)
- 외부 식별자(OAuth sub, 외부 시스템 ID)로부터 도메인 객체를 만들 때

예시: User.createFromOAuth(provider, providerId, email, name, picture)
```

**팩토리 메서드 일반화 원칙** — 같은 종류의 생성이 여러 분기로 나뉘면 메서드를 분리하지 말고
enum 파라미터로 일반화한다. (`createFromGoogle` + `createFromKakao` ❌ → `createFromOAuth(provider, ...)` ⭕)

### 2) 명시적 팩토리 — record 내부 `of(entity, ...추가데이터)` (현 컨벤션 유지)

**Entity + 외부 파라미터(count, list, role 등)를 결합해 만드는 응답 DTO. MapStruct 부적합.**

```
조건:
- Repository 집계 결과(memberCount 등)를 함께 담는 SummaryResponse
- 사전 변환된 컬렉션을 주입받는 DetailResponse
- 권한 컨텍스트(role) 같은 호출자 정보를 결합

예시: WorkspaceDto.SummaryResponse.of(ws, memberCount)
      ProjectDto.SummaryResponse.of(p, memberCount)
```

### 3) MapStruct — `{domain}/mapper/{Domain}Mapper` (조건부 사용)

**모든 조건을 만족할 때만 사용한다. 하나라도 어긋나면 record 내부 `from()` 으로 처리.**

```
조건 (모두 충족):
- Entity → DTO 또는 외부 응답 DTO → 내부 DTO 의 순수 1:1 매핑
- 변환 로직에 비즈니스 분기·계산·조건이 전혀 없음
- 외부 파라미터 결합이 없음 (오직 source 객체 하나만 입력)

판단 흐름:
  1) 비즈니스 로직 있나? → YES → 도메인 팩토리 (1번)
  2) 외부 파라미터 결합? → YES → record `of()` (2번)
  3) 순수 1:1 매핑? → YES → MapStruct (3번)
```

#### MapStruct 사용 시 구조

```
{domain}/mapper/{Domain}Mapper.java
```

```java
package com.{company}.{app}.user.mapper;

import com.{company}.{app}.user.entity.User;
import com.{company}.{app}.auth.dto.AuthDto;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    AuthDto.UserInfo toUserInfo(User user);
}
```

```java
// Service 에서 주입받아 호출
private final UserMapper userMapper;
...
return userMapper.toUserInfo(user);
```

- `componentModel = "spring"` 필수 — Spring Bean 으로 등록되어야 주입 가능.
- record 안에 있던 `from()` 정적 팩토리는 **삭제**한다 (변환 진입점이 두 곳이 되면 일관성 깨짐).
- 매퍼는 변환 책임만 진다. 비즈니스 로직(검증, 계산, 분기)은 매퍼에 절대 두지 않는다.

#### Gradle 설정 (도입 시 한 번)

```groovy
dependencies {
    implementation 'org.mapstruct:mapstruct:1.5.+'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.+'
    // Lombok 와 함께 쓸 경우 lombok-mapstruct-binding 추가
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.+'
}
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

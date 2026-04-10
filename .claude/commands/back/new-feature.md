# /back-new-feature 커맨드

새로운 백엔드 기능을 생성합니다. 레이어드 아키텍처에 맞춰 단계별로 구현합니다.

## 참조
- 컨벤션: `.claude/rules/backend-conventions.md`
- 코드 패턴: `.claude/rules/backend-java-patterns.md`
- 디자인 패턴: `.claude/rules/backend-design-patterns.md`

## 입력
$ARGUMENTS: 기능명 + 옵션 키워드 (선택)

**사용법:**
- `/back-new-feature 상품 CRUD` — 기본 CRUD 기능
- `/back-new-feature 주문 처리 kafka` — Kafka 이벤트 발행 포함
- `/back-new-feature 사용자 인증 security` — Spring Security 연동
- `/back-new-feature 상품 목록 redis` — Redis 캐시 적용
- `/back-new-feature 결제 처리 kafka redis` — 조합 가능

**옵션 키워드:**
| 키워드 | 설명 |
|--------|------|
| (없음) | 기본 CRUD (Entity → DTO → Repository → Service → Controller) |
| `kafka` | Kafka 이벤트 발행/소비 코드 추가 |
| `redis` | Redis 캐시 어노테이션 적용 |
| `security` | 인증/인가 관련 로직 포함 |

## 프로젝트 구조 감지

옵션 파일 중 구조별 분기가 있는 경우, 아래 순서로 현재 프로젝트 구조를 판단한다.

```
1. settings.gradle에 eureka-server 모듈이 포함 → MSA
2. settings.gradle 존재 + 서브 모듈 2개 이상    → multi-module
3. 그 외 (settings.gradle 없거나 단일 모듈)     → single
```

> 옵션 파일 내에 `## single / multi-module` 또는 `## msa` 섹션이 있으면, 위 판단 결과에 해당하는 섹션만 적용한다.

## 옵션 적용

$ARGUMENTS에 포함된 키워드에 해당하는 파일을 Read로 참조한 뒤 내용을 추가 적용합니다.
해당 키워드가 없으면 참조하지 않습니다.

| 키워드 | 참조 파일 |
|--------|-----------|
| `kafka` | `.claude/rules/backend-options/feature-kafka.md` |
| `redis` | `.claude/rules/backend-options/feature-redis.md` |
| `security` | `.claude/rules/backend-options/feature-security.md` |

---

## 전체 워크플로우

```
1. 요구사항 분석     → 기능 범위, 도메인 모델, API 스펙 정의
2. 대상 위치 결정    → 어떤 패키지(또는 모듈)에 구현할지
3. DB 스키마 설계    → Entity, Enum, 테이블
4. DTO 정의         → dto/{Domain}Dto.java (inner record)
5. Repository       → JPA Repository + 커스텀 쿼리
6. Service          → 기본: 구체 클래스 / 다형성 필요 시: 인터페이스 + 구현체
7. Controller       → REST 엔드포인트
8. 옵션 적용        → 해당 옵션 파일 참조 후 적용
9. 테스트           → Service 단위 + Controller 통합
```

---

## Phase 1: 분석 및 설계

### 1-1. 요구사항 분석

```markdown
## 기능 분석

### 목적
- 이 기능이 해결하는 문제는?
- 어떤 사용자 시나리오에서 호출되는가?

### 도메인 모델
- 필요한 Entity / VO / Enum
- 다른 도메인과의 의존 관계

### API 스펙
- 엔드포인트 (Method + Path)
- 요청/응답 구조
- 인증 필요 여부

### 비기능 요구사항
- 트랜잭션 범위
- 캐시 필요 여부 (redis 옵션)
- 비동기 처리 필요 여부 (kafka 옵션)
```

### 1-2. 배치 결정

```
"이 코드는 어디에?"

단일 모듈이면 → 해당 패키지 하위에 레이어별 배치
멀티 모듈이면 → 모듈별로 분산 배치 (아래 표 참고)
MSA이면       → 대상 서비스 모듈 결정 후 동일 구조

레이어 규칙:
  controller/  → 요청 수신 + 응답 반환만. 비즈니스 로직 금지
  service/     → 비즈니스 로직. DB 직접 접근 금지 (Repository 경유)
  repository/  → 쿼리만. 비즈니스 로직 금지
  domain/      → JPA Entity, Enum, VO
  dto/         → inner record (요청/응답/파라미터)
  exception/   → 커스텀 예외
  event/       → (kafka 옵션 시) 이벤트 클래스 + Producer/Consumer
  config/      → (옵션 적용 시) 설정 클래스

멀티 모듈(multi-module) 시 배치:
  common/  → 공통 예외, 공유 DTO (ApiResponse 등)
  domain/  → Entity, Enum, VO, Repository
  api/     → Controller, Service, 기능별 DTO, Config
```

---

## Phase 2: Entity + Enum

```java
// domain/{Entity}.java
package com.{company}.{app}.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "{table_name}")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder @AllArgsConstructor
public class {Entity} {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private {Entity}Status status;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 도메인 메서드 — 상태 변경은 Entity 내부에서
    public void update(String name) {
        if (name != null) this.name = name;
    }

    public void activate() {
        this.status = {Entity}Status.ACTIVE;
    }
}
```

```java
// domain/{Entity}Status.java
public enum {Entity}Status {
    DRAFT, ACTIVE, INACTIVE
}
```

---

## Phase 3: DTO (inner record)

```java
// dto/{Entity}Dto.java
package com.{company}.{app}.dto;

import com.{company}.{app}.domain.{Entity};
import jakarta.validation.constraints.*;

public class {Entity}Dto {

    public record CreateRequest(
        @NotBlank(message = "이름은 필수입니다") @Size(max = 255) String name
        // 기능에 맞게 필드 추가
    ) {}

    public record UpdateRequest(
        @Size(max = 255) String name
    ) {}

    public record ListParams(
        String keyword,
        int page,
        int size
    ) {
        public ListParams {
            if (page < 0) page = 0;
            if (size <= 0 || size > 100) size = 20;
        }
    }

    public record Response(
        Long id,
        String name,
        String status
    ) {
        public static Response from({Entity} entity) {
            return new Response(
                entity.getId(),
                entity.getName(),
                entity.getStatus().name()
            );
        }
    }
}
```

---

## Phase 4: Repository

```java
// repository/{Entity}Repository.java
package com.{company}.{app}.repository;

import com.{company}.{app}.domain.{Entity};
import com.{company}.{app}.domain.{Entity}Status;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface {Entity}Repository extends JpaRepository<{Entity}, Long> {

    Page<{Entity}> findByStatusOrderByCreatedAtDesc({Entity}Status status, Pageable pageable);

    Page<{Entity}> findByNameContainingAndStatus(String keyword, {Entity}Status status, Pageable pageable);
}
```

---

## Phase 5: Service

### 판단 기준 — 구체 클래스 vs 인터페이스 + 구현체

```
아래 조건 중 하나라도 해당하면 → 인터페이스 + 구현체
모두 해당하지 않으면         → 구체 클래스 (기본)

[인터페이스 + 구현체 조건]
1. 동일 역할의 구현이 2개 이상 존재하거나 예정된 경우
   → 예: 알림 발송 — SMS / Slack / Email
   → 예: 결제 처리 — 카드 / 카카오페이 / 네이버페이
   → 예: 파일 저장 — S3 / 로컬 파일시스템

2. 런타임에 구현체를 교체하거나 선택해야 하는 경우
   → 예: 사용자 설정에 따라 알림 채널 분기
   → 예: 환경(로컬/운영)에 따라 저장소 분기

3. 외부 시스템 연동을 추상화하여 테스트 대체(Mock)가 필요한 경우
   → 예: 외부 API 호출을 인터페이스로 격리

[구체 클래스 조건 — 위 조건에 해당하지 않는 모든 경우]
  → 일반 CRUD (상품, 주문, 사용자 등)
  → 단일 비즈니스 로직 (정산 계산, 통계 집계 등)
  → "혹시 나중에 바뀔 수도 있으니까" 같은 추측성 분리 금지
```

### A. 구체 클래스 (기본)

```java
// service/{Entity}Service.java
@Service
@RequiredArgsConstructor
@Transactional
public class {Entity}Service {

    private final {Entity}Repository {entity}Repository;

    @Transactional(readOnly = true)
    public {Entity}Dto.Response getById(Long id) {
        {Entity} entity = {entity}Repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("{Entity}", id));
        return {Entity}Dto.Response.from(entity);
    }

    @Transactional(readOnly = true)
    public Page<{Entity}Dto.Response> getList({Entity}Dto.ListParams params) {
        Pageable pageable = PageRequest.of(params.page(), params.size(),
            Sort.by("createdAt").descending());

        Page<{Entity}> page;
        if (params.keyword() != null && !params.keyword().isBlank()) {
            page = {entity}Repository.findByNameContainingAndStatus(
                params.keyword(), {Entity}Status.ACTIVE, pageable);
        } else {
            page = {entity}Repository.findByStatusOrderByCreatedAtDesc(
                {Entity}Status.ACTIVE, pageable);
        }
        return page.map({Entity}Dto.Response::from);
    }

    public {Entity}Dto.Response create({Entity}Dto.CreateRequest request) {
        {Entity} entity = {Entity}.builder()
            .name(request.name())
            .status({Entity}Status.DRAFT)
            .build();
        return {Entity}Dto.Response.from({entity}Repository.save(entity));
    }

    public {Entity}Dto.Response update(Long id, {Entity}Dto.UpdateRequest request) {
        {Entity} entity = {entity}Repository.findById(id)
            .orElseThrow(() -> new EntityNotFoundException("{Entity}", id));
        entity.update(request.name());
        return {Entity}Dto.Response.from(entity);
    }

    public void delete(Long id) {
        if (!{entity}Repository.existsById(id)) {
            throw new EntityNotFoundException("{Entity}", id);
        }
        {entity}Repository.deleteById(id);
    }
}
```

### B. 인터페이스 + 구현체 (다형성 필요 시)

```java
// service/{Entity}Service.java — 인터페이스
public interface {Entity}Service {
    void send({Entity}Dto.SendRequest request);
}
```

```java
// service/impl/Sms{Entity}ServiceImpl.java
@Service("sms{Entity}Service")
@RequiredArgsConstructor
public class Sms{Entity}ServiceImpl implements {Entity}Service {

    @Override
    public void send({Entity}Dto.SendRequest request) {
        // SMS 발송 로직
    }
}
```

```java
// service/impl/Slack{Entity}ServiceImpl.java
@Service("slack{Entity}Service")
@RequiredArgsConstructor
public class Slack{Entity}ServiceImpl implements {Entity}Service {

    @Override
    public void send({Entity}Dto.SendRequest request) {
        // Slack 발송 로직
    }
}
```

---

## Phase 6: Controller

```java
// controller/{Entity}Controller.java
@RestController
@RequestMapping("/api/v1/{endpoint}")
@RequiredArgsConstructor
public class {Entity}Controller {

    private final {Entity}Service {entity}Service;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<{Entity}Dto.Response>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success({entity}Service.getById(id)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<{Entity}Dto.Response>>> getList(
        @ModelAttribute {Entity}Dto.ListParams params
    ) {
        return ResponseEntity.ok(ApiResponse.success({entity}Service.getList(params)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<{Entity}Dto.Response>> create(
        @RequestBody @Valid {Entity}Dto.CreateRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success({entity}Service.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<{Entity}Dto.Response>> update(
        @PathVariable Long id,
        @RequestBody @Valid {Entity}Dto.UpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success({entity}Service.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        {entity}Service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## Phase 7: 커스텀 예외

> 엔티티마다 별도 예외 클래스를 생성하지 않는다. 공통 예외 클래스 하나를 두고 엔티티명을 파라미터로 전달한다.
> `EntityNotFoundException`은 `init-project` 시 `exception/` 패키지에 한 번만 생성하며, 이후 기능 추가 시 재사용한다.

```java
// exception/EntityNotFoundException.java (프로젝트 공통 — 한 번만 생성)
public class EntityNotFoundException extends RuntimeException {

    private final String entityName;
    private final Long id;

    public EntityNotFoundException(String displayName, Long id) {
        super(displayName + "을(를) 찾을 수 없습니다. (ID: " + id + ")");
        this.entityName = displayName;
        this.id = id;
    }

    public String getEntityName() { return entityName; }
    public Long getId() { return id; }
}
```

```java
// Service에서 사용 예시 — displayName에 사용자가 이해할 수 있는 한글명 전달
throw new EntityNotFoundException("상품", id);
throw new EntityNotFoundException("주문", id);
throw new EntityNotFoundException("사용자", id);
```

---

## Phase 8: 테스트

> 테스트 코드 템플릿과 옵션별 테스트는 `/back-test` 커맨드를 참조한다.
> 기본: Service 단위 테스트 (`@ExtendWith(MockitoExtension.class)`) + Controller 통합 테스트 (`@WebMvcTest`)

---

## 생성되는 파일 구조 (기본 — 단일 모듈)

```
src/main/java/com/{company}/{app}/
├── controller/
│   └── {Entity}Controller.java
├── service/
│   ├── {Entity}Service.java              # 기본: 구체 클래스 (@Service)
│   └── impl/                             # 다형성 필요 시에만 생성
│       └── {Impl}{Entity}ServiceImpl.java # 인터페이스 + 구현체 구조일 때만
├── repository/
│   └── {Entity}Repository.java
├── domain/
│   ├── {Entity}.java                      # JPA Entity
│   └── {Entity}Status.java               # Enum
├── dto/
│   └── {Entity}Dto.java                  # inner record
└── exception/
    └── EntityNotFoundException.java     # 프로젝트 공통 (init-project 시 1회 생성)

src/test/java/com/{company}/{app}/
├── service/
│   └── {Entity}ServiceTest.java
└── controller/
    └── {Entity}ControllerTest.java
```

## 체크리스트

### 설계
- [ ] 요구사항 분석 완료
- [ ] 대상 패키지(또는 모듈) 결정
- [ ] API 스펙 정의

### 구현
- [ ] JPA Entity + Enum
- [ ] DTO inner record
- [ ] Repository
- [ ] Service (구체 클래스 또는 인터페이스+구현체 — 판단 기준 확인)
- [ ] Controller
- [ ] 공통 예외 (`EntityNotFoundException`) 존재 확인 — 없으면 생성
- [ ] (옵션 파일 참조 후) 해당 옵션 적용

### 검증
- [ ] Gradle 빌드 성공 (`./gradlew build`)
- [ ] Service 단위 테스트 통과
- [ ] Controller 통합 테스트 통과
- [ ] API 수동 테스트 (Postman / curl)
- [ ] 레이어 의존성 규칙 준수 (Controller → Service → Repository)

---
globs: ["*.java", "*.kt"]
description: "백엔드 디자인 패턴 (Controller/Service/Domain/Repository 레이어별 패턴) 참조"
---

# 백엔드 디자인 패턴 참고 자료 (참고 전용)

> **이 파일은 레이어별 디자인 패턴 가이드입니다. 규칙·컨벤션은 `backend-conventions.md`를 참고하세요.**
> `/backend:design-patterns` 스킬로 호출 가능합니다.

---

## Controller Layer

| 패턴 | 적용 시점 | 예시 |
|------|-----------|------|
| **Facade** | Controller 자체가 Facade 역할 — 여러 Service를 조합하여 단일 엔드포인트로 노출 | `OrderController`가 `OrderService` + `PaymentService` + `StockService` 조합 |

---

## Service Layer

| 패턴 | 적용 시점 | 예시 |
|------|-----------|------|
| **Strategy** | 동일 인터페이스, 다른 비즈니스 로직 분기 | `PaymentStrategy` → `CardPayment`, `KakaoPayPayment`, `NaverPayPayment` |
| **Template Method** | 공통 비즈니스 흐름에서 일부 단계만 다를 때 | `AbstractOrderProcessor.process()` → `validate()` → `execute()` → `notify()` |
| **Command** | CQRS Command 처리, 작업 단위 캡슐화 | `CreateOrderCommand` → `OrderCommandHandler.handle()` |
| **Observer / Event** | 서비스 간 느슨한 결합, 비동기 후처리 | `OrderCreatedEvent` → Kafka 발행 → 재고 차감, 알림 발송 |

---

## Domain Layer (Entity / VO)

| 패턴 | 적용 시점 | 예시 |
|------|-----------|------|
| **Factory Method** | Entity/VO 생성 로직이 복잡하거나 검증이 필요할 때 | `Order.create(dto)` — 생성자 대신 static 팩토리 |
| **Value Object** | 불변 값, 동등성 비교가 필요한 도메인 개념 | `Money(amount, currency)`, `Address(city, street, zip)` |
| **Builder** | 필드가 많은 Entity 조립 시 | `Order.builder().buyer(...).items(...).build()` (Entity에서만 허용, DTO Record는 금지) |
| **도메인 이벤트** | 엔티티 상태 변경 시 부수효과를 분리 | `order.complete()` 내부에서 `registerEvent(new OrderCompletedEvent(...))` |

---

## Repository / 인프라 Layer

| 패턴 | 적용 시점 | 예시 |
|------|-----------|------|
| **Repository** | Spring Data JPA 기본 | `ProductRepository extends JpaRepository` |
| **Specification** | 동적 쿼리 조건 조합 시 | `ProductSpec.hasCategory(cat).and(ProductSpec.priceBetween(min, max))` |
| **Adapter** | 외부 시스템 연동을 내부 인터페이스로 변환 | `KakaoPayAdapter implements PaymentGateway` |

---

## Cross-Cutting (공통)

| 패턴 | 적용 시점 | 예시 |
|------|-----------|------|
| **Decorator (AOP)** | 로깅, 트랜잭션, 캐시, 인증 등 횡단 관심사 | `@Transactional`, `@Cacheable`, `@LogExecutionTime` (커스텀 AOP) |
| **Singleton** | Spring Bean 기본 스코프 | Service, Repository 등 모든 Bean은 기본 Singleton |

---

## 패턴 적용 판단 기준

```
1. if 분기가 3개 이상이고 같은 인터페이스 → Strategy 패턴
2. 처리 흐름은 같고 일부 단계만 다름 → Template Method 패턴
3. Entity 생성 시 검증/변환 로직 포함 → Factory Method 패턴
4. 상태 변경 후 다른 모듈에 알려야 함 → Observer/Event 패턴
5. 외부 API 호출 → Adapter 패턴으로 격리
6. 동적 조건 조합 쿼리 → Specification 패턴
7. 위 조건에 해당 없으면 → 단순 구현 (과도한 패턴 적용 금지)
```

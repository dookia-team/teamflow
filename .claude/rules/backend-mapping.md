# 백엔드 객체 변환 규칙 (MapStruct vs 수동 매핑)

> 본 규칙은 `backend-conventions.md` 의 "객체 변환 규칙" 항목에서 분리된 상세 가이드다.
> 컨벤션 본문에서는 요약과 판단 흐름만 두고, 세부 절차/예시는 이 파일을 참조한다.

객체 ↔ 객체 변환은 **세 가지 경로** 중 하나로 처리한다. 우선순위 순서:

---

## 1) 도메인 팩토리 — `Entity.createFromXxx(...)` (최우선)

**항상 도메인 엔티티 안에 둔다. MapStruct로 옮기지 않는다.**

```
조건:
- 비즈니스 의미가 담긴 생성 (기본 상태 부여, 식별자 변환 등)
- 외부 식별자(OAuth sub, 외부 시스템 ID)로부터 도메인 객체를 만들 때

예시: User.createFromOAuth(provider, providerId, email, name, picture)
```

**팩토리 메서드 일반화 원칙** — 같은 종류의 생성이 여러 분기로 나뉘면 메서드를 분리하지 말고
enum 파라미터로 일반화한다. (`createFromGoogle` + `createFromKakao` ❌ → `createFromOAuth(provider, ...)` ⭕)

---

## 2) 명시적 팩토리 — record 내부 `of(entity, ...추가데이터)` (현 컨벤션 유지)

**Entity + 외부 파라미터(count, list, role 등)를 결합해 만드는 응답 DTO. MapStruct 부적합.**

```
조건:
- Repository 집계 결과(memberCount 등)를 함께 담는 SummaryResponse
- 사전 변환된 컬렉션을 주입받는 DetailResponse
- 권한 컨텍스트(role) 같은 호출자 정보를 결합

예시: WorkspaceDto.SummaryResponse.of(ws, memberCount)
      ProjectDto.SummaryResponse.of(p, memberCount)
```

---

## 3) MapStruct — `{domain}/mapper/{Domain}Mapper` (조건부 사용)

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

### MapStruct 사용 시 구조

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

### Gradle 설정 (도입 시 한 번)

```groovy
dependencies {
    implementation 'org.mapstruct:mapstruct:1.5.+'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.5.+'
    // Lombok 와 함께 쓸 경우 lombok-mapstruct-binding 추가
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.+'
}
```

# /back-test 커맨드

특정 도메인/레이어에 대한 테스트를 생성하거나 기존 테스트를 실행합니다.

## 참조
- 컨벤션: `.claude/rules/backend-conventions.md`
- 코드 패턴: `.claude/rules/backend-java-patterns.md`

## 입력
$ARGUMENTS: 테스트 대상 + 옵션 키워드 (선택)

**사용법:**
- `/back-test service/product` — ProductService 단위 테스트 생성
- `/back-test controller/product` — ProductController 통합 테스트 생성
- `/back-test product` — Service + Controller 테스트 모두 생성
- `/back-test run` — 전체 테스트 실행
- `/back-test run product` — 특정 도메인 테스트만 실행
- `/back-test product integration` — 실제 DB 통합 테스트 생성
- `/back-test product kafka` — Kafka 이벤트 테스트 포함

**옵션 키워드:**
| 키워드 | 설명 |
|--------|------|
| (없음) | 기본: Service 단위 + Controller MockMvc 테스트 |
| `integration` | @SpringBootTest 기반 통합 테스트 (실제 DB) |
| `kafka` | Kafka Producer/Consumer 테스트 포함 |
| `security` | Spring Security 인증/인가 테스트 포함 |
| `repository` | Repository 슬라이스 테스트 (@DataJpaTest) |

## 옵션 적용

$ARGUMENTS에 포함된 키워드에 해당하는 파일을 Read로 참조한 뒤 내용을 추가 적용합니다.
해당 키워드가 없으면 참조하지 않습니다.

| 키워드 | 참조 파일 |
|--------|-----------|
| `repository` | `.claude/rules/backend-options/test-repository.md` |
| `integration` | `.claude/rules/backend-options/test-integration.md` |
| `kafka` | `.claude/rules/backend-options/test-kafka.md` |
| `security` | `.claude/rules/backend-options/test-security.md` |

---

## 분기: "run"이면 테스트 실행

```bash
# 전체 실행
./gradlew test

# 특정 도메인 테스트만
./gradlew test --tests "com.{company}.{app}.*.{Domain}*"

# 특정 클래스만
./gradlew test --tests "com.{company}.{app}.service.{Entity}ServiceTest"

# 실패한 테스트만 재실행
./gradlew test --rerun

# 테스트 + 커버리지 (JaCoCo 설정 시)
./gradlew test jacocoTestReport

# 멀티 모듈인 경우 — 모듈별 실행
# ./gradlew :api:test                                        # api 모듈 테스트만
# ./gradlew :domain:test                                     # domain 모듈 테스트만
# ./gradlew :api:test --tests "com.{company}.api.*.{Entity}*"  # api 내 특정 도메인
```

---

## 분기: 경로 지정이면 테스트 생성

### 1단계: 대상 파일 분석

```bash
# 테스트 대상 파일 확인 (단일 모듈)
find src/main/java -path "*/$ARGUMENTS*" -name "*.java" | grep -v "test"

# 멀티 모듈인 경우
# find */src/main/java -path "*/$ARGUMENTS*" -name "*.java" | grep -v "test"
```

각 파일의 public 메서드를 분석하여 테스트 대상을 파악합니다.

### 2단계: 테스트 파일 배치

```
src/test/java/com/{company}/{app}/
├── controller/
│   └── {Entity}ControllerTest.java     ← @WebMvcTest
├── service/
│   └── {Entity}ServiceTest.java        ← @ExtendWith(MockitoExtension)
├── repository/                          ← (repository 옵션 시)
│   └── {Entity}RepositoryTest.java     ← @DataJpaTest
├── integration/                         ← (integration 옵션 시)
│   └── {Entity}IntegrationTest.java    ← @SpringBootTest
└── event/                               ← (kafka 옵션 시)
    └── {Entity}EventTest.java          ← @EmbeddedKafka
```

---

## 기본 테스트: Service 단위 테스트

```java
// test/.../service/{Entity}ServiceTest.java
package com.{company}.{app}.service;

import com.{company}.{app}.domain.{Entity};
import com.{company}.{app}.domain.{Entity}Status;
import com.{company}.{app}.dto.{Entity}Dto;
import com.{company}.{app}.exception.{Entity}NotFoundException;
import com.{company}.{app}.repository.{Entity}Repository;
import com.{company}.{app}.service.impl.{Entity}ServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class {Entity}ServiceTest {

    @Mock        private {Entity}Repository {entity}Repository;
    @InjectMocks private {Entity}ServiceImpl {entity}Service;

    // ── 조회 ──

    @Test
    @DisplayName("존재하는 ID 조회 → Response 반환")
    void getById_existing_returnsResponse() {
        // given
        {Entity} entity = {Entity}.builder()
            .id(1L).name("테스트").status({Entity}Status.ACTIVE).build();
        given({entity}Repository.findById(1L)).willReturn(Optional.of(entity));

        // when
        var result = {entity}Service.getById(1L);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.name()).isEqualTo("테스트");
    }

    @Test
    @DisplayName("존재하지 않는 ID → NotFoundException")
    void getById_notFound_throwsException() {
        given({entity}Repository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> {entity}Service.getById(999L))
            .isInstanceOf({Entity}NotFoundException.class)
            .hasMessageContaining("999");
    }

    // ── 생성 ──

    @Test
    @DisplayName("유효한 요청 → 저장 후 Response 반환")
    void create_validRequest_returnsResponse() {
        // given
        var request = new {Entity}Dto.CreateRequest("테스트");
        {Entity} saved = {Entity}.builder()
            .id(1L).name("테스트").status({Entity}Status.DRAFT).build();
        given({entity}Repository.save(any())).willReturn(saved);

        // when
        var result = {entity}Service.create(request);

        // then
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.status()).isEqualTo("DRAFT");
        verify({entity}Repository).save(any());
    }

    // ── 수정 ──

    @Test
    @DisplayName("존재하는 ID 수정 → 업데이트된 Response 반환")
    void update_existing_returnsUpdated() {
        // given
        {Entity} entity = {Entity}.builder()
            .id(1L).name("기존").status({Entity}Status.ACTIVE).build();
        given({entity}Repository.findById(1L)).willReturn(Optional.of(entity));
        var request = new {Entity}Dto.UpdateRequest("수정됨");

        // when
        var result = {entity}Service.update(1L, request);

        // then
        assertThat(result.name()).isEqualTo("수정됨");
    }

    // ── 삭제 ──

    @Test
    @DisplayName("존재하는 ID 삭제 → 정상 처리")
    void delete_existing_success() {
        given({entity}Repository.existsById(1L)).willReturn(true);

        {entity}Service.delete(1L);

        verify({entity}Repository).deleteById(1L);
    }

    @Test
    @DisplayName("존재하지 않는 ID 삭제 → NotFoundException")
    void delete_notFound_throwsException() {
        given({entity}Repository.existsById(999L)).willReturn(false);

        assertThatThrownBy(() -> {entity}Service.delete(999L))
            .isInstanceOf({Entity}NotFoundException.class);
    }
}
```

---

## 기본 테스트: Controller 통합 테스트

```java
// test/.../controller/{Entity}ControllerTest.java
package com.{company}.{app}.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.{company}.{app}.dto.{Entity}Dto;
import com.{company}.{app}.exception.{Entity}NotFoundException;
import com.{company}.{app}.service.{Entity}Service;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.bean.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({Entity}Controller.class)
class {Entity}ControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean  private {Entity}Service {entity}Service;
    @Autowired private ObjectMapper objectMapper;

    // ── GET ──

    @Test
    @DisplayName("GET /api/v1/{endpoint}/{id} → 200 OK")
    void getById_success_returns200() throws Exception {
        var response = new {Entity}Dto.Response(1L, "테스트", "ACTIVE");
        given({entity}Service.getById(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/{endpoint}/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.name").value("테스트"));
    }

    @Test
    @DisplayName("GET /api/v1/{endpoint}/999 → 404 Not Found")
    void getById_notFound_returns404() throws Exception {
        given({entity}Service.getById(999L))
            .willThrow(new {Entity}NotFoundException(999L));

        mockMvc.perform(get("/api/v1/{endpoint}/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false));
    }

    // ── POST ──

    @Test
    @DisplayName("POST /api/v1/{endpoint} → 201 Created")
    void create_validRequest_returns201() throws Exception {
        var request  = new {Entity}Dto.CreateRequest("테스트");
        var response = new {Entity}Dto.Response(1L, "테스트", "DRAFT");
        given({entity}Service.create(any())).willReturn(response);

        mockMvc.perform(post("/api/v1/{endpoint}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.data.id").value(1));
    }

    @Test
    @DisplayName("POST /api/v1/{endpoint} 필수값 누락 → 400 Bad Request")
    void create_invalidRequest_returns400() throws Exception {
        var request = new {Entity}Dto.CreateRequest("");  // @NotBlank 위반

        mockMvc.perform(post("/api/v1/{endpoint}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    // ── PUT ──

    @Test
    @DisplayName("PUT /api/v1/{endpoint}/{id} → 200 OK")
    void update_validRequest_returns200() throws Exception {
        var request  = new {Entity}Dto.UpdateRequest("수정됨");
        var response = new {Entity}Dto.Response(1L, "수정됨", "ACTIVE");
        given({entity}Service.update(any(), any())).willReturn(response);

        mockMvc.perform(put("/api/v1/{endpoint}/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("수정됨"));
    }

    // ── DELETE ──

    @Test
    @DisplayName("DELETE /api/v1/{endpoint}/{id} → 204 No Content")
    void delete_success_returns204() throws Exception {
        mockMvc.perform(delete("/api/v1/{endpoint}/1"))
            .andExpect(status().isNoContent());
    }
}
```

---

## 테스트 실행 및 결과 보고

### 실행

```bash
# 생성된 테스트 실행 (단일 모듈)
./gradlew test --tests "com.{company}.{app}.*{Entity}*"

# 멀티 모듈인 경우
# ./gradlew :api:test --tests "com.{company}.api.*{Entity}*"
# ./gradlew :domain:test --tests "com.{company}.domain.*{Entity}*"

# 커버리지 확인 (JaCoCo 설정 시)
./gradlew test jacocoTestReport
# 리포트: build/reports/jacoco/test/html/index.html
# 멀티 모듈: api/build/reports/jacoco/test/html/index.html
```

### 결과 보고

```markdown
## 테스트 생성 결과

### 생성된 파일
| 파일 | 테스트 유형 | 테스트 수 |
|------|------------|----------|
| {Entity}ServiceTest.java | 단위 (Mockito) | N개 |
| {Entity}ControllerTest.java | MockMvc | N개 |

### 실행 결과
- 전체: N개
- 성공: N개
- 실패: N개
- 커버리지: N% (JaCoCo 기준)
```

## 체크리스트

- [ ] 대상 파일 분석 완료
- [ ] Service 단위 테스트 생성
- [ ] Controller 통합 테스트 생성
- [ ] (옵션 파일 참조 후) 해당 옵션 테스트 추가
- [ ] 전체 테스트 통과 (`./gradlew test`)
- [ ] 빌드 성공 확인 (`./gradlew build`)

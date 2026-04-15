# 백엔드 공유 인스턴스 (Spring Bean 싱글톤) 규칙

> 본 규칙은 `backend-conventions.md` 의 "공유 인스턴스" 항목에서 분리된 상세 가이드다.
> 컨벤션 본문에서는 적용 대상 표와 한 줄 원칙만 두고, 세부 예시·판단 흐름은 이 파일을 참조한다.

---

## 핵심 원칙

**Thread-safe 하게 재사용 가능한 컴포넌트는 절대 서비스 내부에서 직접 생성하지 않는다.**
반드시 `@Configuration` 에 빈으로 등록하고 생성자 주입(`@RequiredArgsConstructor`) 으로 받아 사용한다.

---

## 적용 대상 (대표 예시)

| 컴포넌트 | 처리 방식 | 위치 |
|----------|----------|------|
| `ObjectMapper` | Spring Boot 자동 설정 빈을 그대로 주입 | (별도 빈 정의 불필요) |
| `RestClient` | `RestClient.Builder` 로 빌드한 단일 빈 주입 | `config/HttpClientConfig` |
| `RestTemplate` | (사용 시) 단일 빈으로 정의 후 주입 | `config/HttpClientConfig` |
| `WebClient` | `WebClient.Builder` 로 빌드한 단일 빈 주입 | `config/HttpClientConfig` |
| `PasswordEncoder`, `Clock`, `JwtParser` 등 | 단일 빈으로 정의 후 주입 | `config/` 또는 도메인별 `Config` |
| `CorsConfigurationSource` | `CorsProperties` 와 함께 `config/CorsConfig` | `config/CorsConfig` |

---

## 금지

```java
// ❌ 서비스마다 신규 인스턴스 생성 — 메모리 낭비 + 설정 일관성 깨짐
private final ObjectMapper objectMapper = new ObjectMapper();
private final RestClient restClient = RestClient.create();

public MyService() {
    this.restClient = RestClient.create();
}
```

## 권장

```java
// ✅ 공용 빈 주입 — @RequiredArgsConstructor 와 자연스럽게 결합
@Service
@RequiredArgsConstructor
public class GoogleOAuthService {

    private final GoogleOAuthProperties properties;
    private final RestClient restClient;       // HttpClientConfig 의 빈
    private final ObjectMapper objectMapper;   // Spring Boot 자동 설정 빈
    ...
}
```

```java
// ✅ 공용 빈 등록
@Configuration
public class HttpClientConfig {

    @Bean
    public RestClient restClient(RestClient.Builder builder) {
        return builder.build();
    }
}
```

---

## 판단 기준

```
어떤 컴포넌트를 서비스 안에서 new ... 로 만들고 싶은가?

1. thread-safe 한가? (Spring 빈은 기본 싱글톤 — 멀티 스레드 동시 접근 가능해야 함)
   - YES → 다음 단계
   - NO  → 빈으로 만들지 말고, 호출 시점마다 지역 변수로 생성

2. 무거운 초기화·연결·설정이 들어가는가? (HTTP 클라이언트, 직렬화기, 암호화기, 외부 SDK 등)
   - YES → 빈으로 등록하고 주입 (재사용)
   - NO  → 일회성 값/유틸이면 그냥 지역 생성

3. 전역적으로 동일한 설정을 공유해야 하는가?
   - YES → 빈으로 등록 (설정 분산 방지)
   - NO  → 빈 등록 가능하지만 필수는 아님
```

`ObjectMapper`, `RestClient`, `WebClient`, `RestTemplate`, `PasswordEncoder`, `CorsConfigurationSource` 는 위 모든 항목에 해당하므로 **항상 빈으로 사용**한다.

---

## 외부에서 주입받아야 하는 설정값

값이 환경별로 달라지는 빈(예: 허용 origin, API key, 외부 endpoint URL)은 **`@ConfigurationProperties` record 로 정의**하고 `@EnableConfigurationProperties` 로 등록한다 (`AuthConfig`, `CorsConfig` 사례 참고). 빈 내부에서 `@Value` 를 직접 주입하는 방식은 단발성 값 외에는 지양한다.

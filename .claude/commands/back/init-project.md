# /back-init-project 커맨드

새로운 Spring Boot 프로젝트를 초기화합니다. Java 21+ 기반.

## 참조
- 컨벤션: `.claude/rules/backend-conventions.md`
- 코드 패턴: `.claude/rules/backend-java-patterns.md`
- 디자인 패턴: `.claude/rules/backend-design-patterns.md`

## 입력
$ARGUMENTS: 프로젝트명 + 옵션 키워드 (선택)

**사용법:**
- `/back-init-project my-app` — 단일 모듈 Spring Boot (기본)
- `/back-init-project my-app multi-module` — Gradle 멀티 모듈 (공통 모듈 분리)
- `/back-init-project my-app msa` — MSA 멀티 모듈 + Eureka + Gateway
- `/back-init-project my-app kafka` — Kafka 이벤트 통신 포함
- `/back-init-project my-app multi-module kafka redis` — 조합 가능

**옵션 키워드:**
| 키워드 | 설명 |
|--------|------|
| (없음) | 단일 모듈 Spring Boot 프로젝트 |
| `multi-module` | Gradle 멀티 모듈 (common + api + domain 등 레이어 분리) |
| `msa` | MSA 멀티 모듈 + Eureka + Gateway (multi-module 포함) |
| `kafka` | Spring Kafka 이벤트 통신 설정 |
| `redis` | Spring Data Redis 캐시/세션 설정 |
| `security` | Spring Security + JWT 인증 설정 |

## 모듈 구조 선택 (택 1 — 필수 참조)

$ARGUMENTS의 키워드에 따라 아래 파일 중 **하나를 반드시 Read**하여 build.gradle과 디렉토리 구조를 적용합니다.

| 조건 | 참조 파일 |
|------|-----------|
| `multi-module`, `msa` 키워드 **없음** (기본) | `.claude/rules/backend-options/init-single.md` |
| `multi-module` 키워드 포함 | `.claude/rules/backend-options/init-multi-module.md` |
| `msa` 키워드 포함 | `.claude/rules/backend-options/init-msa.md` |

## 추가 옵션 (해당 키워드 있을 때만 참조)

$ARGUMENTS에 포함된 키워드에 해당하는 파일을 Read로 참조한 뒤 의존성/설정을 추가합니다.
멀티 모듈인 경우 해당 모듈의 build.gradle에 의존성을 배치합니다.

| 키워드 | 참조 파일 |
|--------|-----------|
| `kafka` | `.claude/rules/backend-options/init-kafka.md` |
| `redis` | `.claude/rules/backend-options/init-redis.md` |
| `security` | `.claude/rules/backend-options/init-security.md` |

---

## 공통 구조 (모듈 구조와 무관하게 항상 적용)

### 1단계: 프로젝트 생성

```bash
mkdir -p $PROJECT_NAME && cd $PROJECT_NAME
```

### 2단계: Application 클래스

```java
// src/main/java/com/{company}/{app}/Application.java
package com.{company}.{app};

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

### 3단계: application.yml

```yaml
server:
  port: 8080

spring:
  application:
    name: ${PROJECT_NAME}
  datasource:
    url: jdbc:mysql://localhost:3306/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USER:root}
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        format_sql: true
    open-in-view: false

management:
  endpoints:
    web:
      exposure:
        include: health, info, prometheus

---
# 로컬 개발 프로파일
spring:
  config:
    activate:
      on-profile: local
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
```

### 4단계: 공통 클래스

#### `ApiResponse.java`
```java
package com.{company}.{app}.dto;

import lombok.Builder;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter @Builder
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;
    private final String timestamp;

    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
            .success(true).data(data)
            .timestamp(LocalDateTime.now().toString())
            .build();
    }

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder()
            .success(false).message(message)
            .timestamp(LocalDateTime.now().toString())
            .build();
    }
}
```

#### `GlobalExceptionHandler.java`
```java
package com.{company}.{app}.exception;

import com.{company}.{app}.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
            .map(fe -> fe.getDefaultMessage()).findFirst().orElse("입력값 오류");
        return ResponseEntity.badRequest().body(ApiResponse.error(message));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntime(RuntimeException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(e.getMessage()));
    }
}
```

### 5단계: Docker Compose (로컬 개발)

```yaml
# docker-compose.yml
version: '3.8'
services:
  mysql:
    image: mysql:8.0
    ports:
      - "3306:3306"
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: ${DB_NAME}
    volumes:
      - mysql-data:/var/lib/mysql

volumes:
  mysql-data:
```

### 6단계: .gitignore

```
.gradle/
build/
!gradle/wrapper/gradle-wrapper.jar
.idea/
*.iml
.vscode/
*.class
*.jar
.env
*.local
.DS_Store
Thumbs.db
```

---

## 최종 커밋

```bash
git add .
git commit -m "chore: Spring Boot 프로젝트 초기 셋업

- Java 21+ / Spring Boot 3.x
- 레이어드 아키텍처 디렉토리 구조
- 공통 ApiResponse / GlobalExceptionHandler
- Docker Compose 로컬 환경
- [옵션 적용 내역 나열]"
```

## 체크리스트

- [ ] 모듈 구조 참조 파일 Read 완료 (single / multi-module / msa)
- [ ] build.gradle + 디렉토리 구조 생성
- [ ] Application 클래스 + application.yml
- [ ] 공통 ApiResponse / GlobalExceptionHandler
- [ ] Docker Compose (MySQL + 옵션별 추가)
- [ ] .gitignore
- [ ] (추가 옵션 참조 후) kafka / redis / security 설정 적용
- [ ] Gradle 빌드 확인 (`./gradlew build`)
- [ ] 초기 커밋

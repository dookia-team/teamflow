# init-project: multimodule 옵션 — Gradle 멀티 모듈 (레이어 분리)

> MSA 인프라(Eureka, Gateway) 없이 순수 모듈 분리만 수행.

**변경 사항:**
- 루트 `build.gradle`을 멀티 모듈용으로 교체
- `settings.gradle` 생성
- 공통/도메인/API 모듈 분리
- 라이브러리 모듈(common, domain)은 `java-library` 플러그인 + `api` 스코프 사용

#### `settings.gradle`
```groovy
rootProject.name = '$PROJECT_NAME'

include 'common'        // 공통 DTO, 유틸, 예외 처리
include 'domain'        // JPA Entity, Repository
include 'api'           // Controller, Service (메인 애플리케이션)
// 필요 시 모듈 추가
// include 'batch'      // 배치 작업
// include 'infra'      // 외부 연동 (S3, 메일 등)
```

#### 루트 `build.gradle` (멀티 모듈)
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.+' apply false
    id 'io.spring.dependency-management' version '1.1.+' apply false
}

allprojects {
    group = 'com.{company}'
    version = '0.0.1-SNAPSHOT'
    repositories { mavenCentral() }
}

subprojects {
    apply plugin: 'java'
    apply plugin: 'io.spring.dependency-management'

    java {
        toolchain { languageVersion = JavaLanguageVersion.of(21) }
    }

    dependencies {
        compileOnly 'org.projectlombok:lombok'
        annotationProcessor 'org.projectlombok:lombok'
        testImplementation 'org.springframework.boot:spring-boot-starter-test'
    }

    test { useJUnitPlatform() }
}
```

#### 의존성 스코프 규칙 (`api` vs `implementation`)

```
라이브러리 모듈 (common, domain)
  → java-library 플러그인 적용
  → 소비자 모듈에 노출해야 하는 의존성은 api 스코프
  → 내부에서만 쓰는 의존성은 implementation 스코프

애플리케이션 모듈 (api)
  → 최종 실행 모듈이므로 전부 implementation 스코프
```

#### common 모듈 — 공통 DTO, 예외, 유틸
```groovy
// common/build.gradle
apply plugin: 'java-library'    // api 스코프 사용

dependencies {
    // 소비자(domain, api)가 validation 어노테이션을 직접 사용하므로 api
    api 'org.springframework.boot:spring-boot-starter-validation'
    // 소비자가 Spring Web 타입(ResponseEntity 등)을 사용하면 api, 아니면 implementation
    api 'org.springframework.boot:spring-boot-starter-web'
}

bootJar.enabled = false
jar.enabled = true
```

```
common/src/main/java/com/{company}/common/
├── dto/
│   └── ApiResponse.java
├── exception/
│   └── GlobalExceptionHandler.java
└── util/
```

#### domain 모듈 — Entity, Repository
```groovy
// domain/build.gradle
apply plugin: 'java-library'    // api 스코프 사용

dependencies {
    // 소비자(api)가 common의 DTO/예외를 직접 사용하므로 api
    api project(':common')
    // 소비자(api)가 Entity, Repository, JPA 어노테이션을 직접 사용하므로 api
    api 'org.springframework.boot:spring-boot-starter-data-jpa'
    // DB 드라이버는 런타임에만 필요 — 소비자에 노출 불필요
    runtimeOnly 'com.mysql:mysql-connector-j'
    testRuntimeOnly 'com.h2database:h2'
}

bootJar.enabled = false
jar.enabled = true
```

```
domain/src/main/java/com/{company}/domain/
├── {feature}/
│   ├── {Entity}.java
│   ├── {Entity}Status.java
│   └── {Entity}Repository.java
└── ...
```

#### api 모듈 — Controller, Service (메인 애플리케이션)
```groovy
// api/build.gradle
apply plugin: 'org.springframework.boot'

// 최종 실행 모듈 — 전부 implementation (전이 노출 불필요)
dependencies {
    implementation project(':common')
    implementation project(':domain')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
}
```

```
api/src/main/java/com/{company}/api/
├── Application.java                # @SpringBootApplication
├── config/
├── {feature}/
│   ├── controller/
│   │   └── {Entity}Controller.java
│   ├── service/
│   │   ├── {Entity}Service.java
│   │   └── impl/
│   │       └── {Entity}ServiceImpl.java
│   └── dto/
│       └── {Entity}Dto.java
└── ...

api/src/main/resources/
└── application.yml
```

#### 모듈 간 의존 방향
```
api → domain → common
          ↗
api ──────
```

- `common`: 어디서든 참조 가능 (의존 없음) — `java-library` + `api` 스코프
- `domain`: common만 참조 — `java-library` + `api` 스코프
- `api`: common + domain 참조 (최종 모듈) — `implementation` 스코프
- 순환 의존 절대 금지

### 적용 시 생성되는 구조
```
$PROJECT_NAME/
├── build.gradle           ← 멀티 모듈 루트
├── settings.gradle
├── docker-compose.yml
├── common/                ← 공통 DTO, 예외, 유틸 (java-library)
│   ├── build.gradle
│   └── src/main/java/.../common/
├── domain/                ← Entity, Repository (java-library)
│   ├── build.gradle
│   └── src/main/java/.../domain/
└── api/                   ← Controller, Service (메인 앱)
    ├── build.gradle
    └── src/
        ├── main/java/.../api/
        └── main/resources/application.yml
```

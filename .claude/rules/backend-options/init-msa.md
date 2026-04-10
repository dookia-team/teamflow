# init-project: msa 옵션 — MSA 멀티 모듈 구조

**변경 사항:**
- 루트 `build.gradle`을 멀티 모듈용으로 교체
- `settings.gradle` 생성 (모듈 include)
- 인프라 모듈 추가: `eureka-server`, `api-gateway`
- 비즈니스 서비스를 독립 모듈로 분리
- 공유 라이브러리 모듈이 있으면 `java-library` + `api` 스코프 적용

#### `settings.gradle`
```groovy
rootProject.name = '$PROJECT_NAME'

include 'eureka-server'
include 'api-gateway'
include 'common'            // 서비스 간 공유 모듈 (DTO, 이벤트, 유틸)
// 비즈니스 모듈은 필요 시 추가
// include 'user-service'
// include 'product-service'
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

    ext {
        set('springCloudVersion', '2023.0.+')
    }

    dependencyManagement {
        imports {
            mavenBom "org.springframework.cloud:spring-cloud-dependencies:${springCloudVersion}"
        }
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
공유 라이브러리 모듈 (common)
  → java-library 플러그인 적용
  → 소비자 서비스에 노출해야 하는 의존성은 api 스코프
  → 내부에서만 쓰는 의존성은 implementation 스코프

애플리케이션 모듈 (각 서비스, gateway, eureka)
  → 최종 실행 모듈이므로 전부 implementation 스코프
```

#### common 모듈 — 서비스 간 공유 (DTO, 이벤트, 유틸)
```groovy
// common/build.gradle
apply plugin: 'java-library'    // api 스코프 사용

dependencies {
    // 소비자 서비스가 validation 어노테이션을 직접 사용하므로 api
    api 'org.springframework.boot:spring-boot-starter-validation'
    // 소비자 서비스가 Spring Web 타입을 사용하면 api
    api 'org.springframework.boot:spring-boot-starter-web'
}

bootJar.enabled = false
jar.enabled = true
```

#### eureka-server 모듈
```groovy
// eureka-server/build.gradle
apply plugin: 'org.springframework.boot'

dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-server'
}
```

```java
@SpringBootApplication
@EnableEurekaServer
public class EurekaServerApplication { ... }
```

```yaml
# eureka-server/src/main/resources/application.yml
server:
  port: 8761
eureka:
  server:
    enable-self-preservation: false
  client:
    register-with-eureka: false
    fetch-registry: false
```

#### api-gateway 모듈
```groovy
// api-gateway/build.gradle
apply plugin: 'org.springframework.boot'

dependencies {
    implementation 'org.springframework.cloud:spring-cloud-starter-gateway'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
}
```

#### 각 비즈니스 서비스 모듈
```groovy
// {service}/build.gradle
apply plugin: 'org.springframework.boot'

// 최종 실행 모듈 — 전부 implementation
dependencies {
    implementation project(':common')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'io.micrometer:micrometer-registry-prometheus'
    implementation 'org.springframework.cloud:spring-cloud-starter-netflix-eureka-client'
    runtimeOnly 'com.mysql:mysql-connector-j'
    testRuntimeOnly 'com.h2database:h2'
}
```

```yaml
# 각 서비스 application.yml에 추가
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
```

### 적용 시 생성되는 구조
```
$PROJECT_NAME/
├── build.gradle           ← 멀티 모듈 루트
├── settings.gradle
├── docker-compose.yml
├── common/                ← 공유 라이브러리 (java-library)
│   └── build.gradle
├── eureka-server/         ← 인프라 (implementation)
│   └── build.gradle
├── api-gateway/           ← 인프라 (implementation)
│   └── build.gradle
└── {service-name}/        ← 각 비즈니스 서비스 (implementation)
    └── build.gradle
```

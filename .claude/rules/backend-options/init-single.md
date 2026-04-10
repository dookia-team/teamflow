# init-project: 단일 모듈 (기본) — build.gradle + 디렉토리 구조

> `multimodule`, `msa` 키워드가 없을 때 적용되는 기본 구조.

#### `build.gradle`
```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.2.+'
    id 'io.spring.dependency-management' version '1.1.+'
}

group = 'com.{company}'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    // Core
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'

    // Monitoring
    implementation 'io.micrometer:micrometer-registry-prometheus'

    // DB
    runtimeOnly 'com.mysql:mysql-connector-j'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // Test
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testRuntimeOnly 'com.h2database:h2'
}

test {
    useJUnitPlatform()
}
```

#### 디렉토리 구조 생성

```bash
mkdir -p src/main/java/com/{company}/{app}/{config,controller,service/impl,repository,domain,dto,exception}
mkdir -p src/main/resources
mkdir -p src/test/java/com/{company}/{app}/{controller,service}
```

#### 생성되는 구조

```
$PROJECT_NAME/
├── build.gradle
├── docker-compose.yml
├── .gitignore
└── src/
    ├── main/
    │   ├── java/com/{company}/{app}/
    │   │   ├── Application.java
    │   │   ├── config/
    │   │   ├── controller/
    │   │   ├── service/
    │   │   │   └── impl/
    │   │   ├── repository/
    │   │   ├── domain/
    │   │   ├── dto/
    │   │   │   └── ApiResponse.java
    │   │   └── exception/
    │   │       └── GlobalExceptionHandler.java
    │   └── resources/
    │       └── application.yml
    └── test/java/com/{company}/{app}/
        ├── controller/
        └── service/
```

# init-project: security 옵션 — JWT 인증

**추가 의존성:**
```groovy
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'io.jsonwebtoken:jjwt-api:0.12.+'
runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.+'
runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.+'
testImplementation 'org.springframework.security:spring-security-test'
```

**SecurityConfig.java 생성:**
```java
package com.{company}.{app}.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/auth/**", "/actuator/**").permitAll()
                .anyRequest().authenticated()
            )
            // .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

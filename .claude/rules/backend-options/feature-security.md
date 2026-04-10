# new-feature: security 옵션 — 인증/인가

### Controller에 권한 어노테이션 추가

```java
@PostMapping
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<{Entity}Dto.Response>> create(...) { ... }

@PutMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ApiResponse<{Entity}Dto.Response>> update(...) { ... }

@DeleteMapping("/{id}")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<Void> delete(...) { ... }
```

### 현재 사용자 정보 접근 (필요 시)

```java
// Service에서 현재 인증 사용자 조회
SecurityContextHolder.getContext().getAuthentication().getName();
```

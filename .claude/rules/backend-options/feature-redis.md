# new-feature: redis 옵션 — 캐시 적용

### ServiceImpl에 캐시 어노테이션 추가

```java
@Override
@Transactional(readOnly = true)
@Cacheable(value = "{entity}", key = "#id")
public {Entity}Dto.Response getById(Long id) { ... }

@Override
@CacheEvict(value = "{entity}", key = "#id")
public {Entity}Dto.Response update(Long id, {Entity}Dto.UpdateRequest request) { ... }

@Override
@CacheEvict(value = "{entity}", key = "#id")
public void delete(Long id) { ... }
```

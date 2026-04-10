# test: repository 옵션 — Repository 슬라이스 테스트

```java
// test/.../repository/{Entity}RepositoryTest.java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)  // H2 사용
class {Entity}RepositoryTest {

    @Autowired private {Entity}Repository {entity}Repository;
    @Autowired private TestEntityManager entityManager;

    @Test
    @DisplayName("키워드 검색 → 이름 포함 결과 반환")
    void findByNameContaining_returnsMatching() {
        // given
        {Entity} entity = {Entity}.builder()
            .name("테스트 상품").status({Entity}Status.ACTIVE).build();
        entityManager.persistAndFlush(entity);

        // when
        var result = {entity}Repository.findByNameContainingAndStatus(
            "테스트", {Entity}Status.ACTIVE, PageRequest.of(0, 10));

        // then
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).contains("테스트");
    }

    @Test
    @DisplayName("상태 필터 → 해당 상태만 반환")
    void findByStatus_returnsFiltered() {
        entityManager.persistAndFlush(
            {Entity}.builder().name("활성").status({Entity}Status.ACTIVE).build());
        entityManager.persistAndFlush(
            {Entity}.builder().name("비활성").status({Entity}Status.INACTIVE).build());

        var result = {entity}Repository.findByStatusOrderByCreatedAtDesc(
            {Entity}Status.ACTIVE, PageRequest.of(0, 10));

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("활성");
    }
}
```

### 테스트 파일 위치

```
src/test/java/com/{company}/{app}/repository/
└── {Entity}RepositoryTest.java     ← @DataJpaTest
```

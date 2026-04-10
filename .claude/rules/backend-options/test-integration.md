# test: integration 옵션 — 풀 통합 테스트

```java
// test/.../integration/{Entity}IntegrationTest.java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Transactional  // 각 테스트 후 롤백
class {Entity}IntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private {Entity}Repository {entity}Repository;
    @Autowired private ObjectMapper objectMapper;

    @Test
    @DisplayName("생성 → 조회 전체 플로우")
    void createAndGet_fullFlow() throws Exception {
        // 생성
        var request = new {Entity}Dto.CreateRequest("통합테스트");

        var createResult = mockMvc.perform(post("/api/v1/{endpoint}")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andReturn();

        Long id = objectMapper.readTree(createResult.getResponse().getContentAsString())
            .path("data").path("id").asLong();

        // 조회
        mockMvc.perform(get("/api/v1/{endpoint}/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("통합테스트"));

        // DB 직접 확인
        assertThat({entity}Repository.findById(id)).isPresent();
    }
}
```

### 테스트 파일 위치

```
src/test/java/com/{company}/{app}/integration/
└── {Entity}IntegrationTest.java    ← @SpringBootTest
```

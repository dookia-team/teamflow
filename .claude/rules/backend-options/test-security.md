# test: security 옵션 — 인증/인가 테스트

### ControllerTest에 Security 테스트 케이스 추가

```java
@Test
@DisplayName("인증 없이 POST → 401 Unauthorized")
void create_noAuth_returns401() throws Exception {
    var request = new {Entity}Dto.CreateRequest("테스트");

    mockMvc.perform(post("/api/v1/{endpoint}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized());
}

@Test
@WithMockUser(roles = "USER")
@DisplayName("USER 권한으로 POST → 403 Forbidden")
void create_userRole_returns403() throws Exception {
    var request = new {Entity}Dto.CreateRequest("테스트");

    mockMvc.perform(post("/api/v1/{endpoint}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden());
}

@Test
@WithMockUser(roles = "ADMIN")
@DisplayName("ADMIN 권한으로 POST → 201 Created")
void create_adminRole_returns201() throws Exception {
    var request  = new {Entity}Dto.CreateRequest("테스트");
    var response = new {Entity}Dto.Response(1L, "테스트", "DRAFT");
    given({entity}Service.create(any())).willReturn(response);

    mockMvc.perform(post("/api/v1/{endpoint}")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated());
}
```

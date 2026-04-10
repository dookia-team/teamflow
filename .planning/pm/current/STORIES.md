# Sprint 1 — 유저 스토리

---

## US-001: Google 로그인 (REQ-AUTH-001, REQ-AUTH-006)

**As a** 신규/기존 사용자  
**I want to** Google 계정으로 로그인/회원가입하고 싶다  
**So that** 별도 비밀번호 없이 빠르게 서비스에 진입할 수 있다

### 인수 기준
- [ ] 랜딩 페이지에서 "Google로 시작하기" 버튼 클릭 → Google OAuth 화면으로 이동
- [ ] Google 인증 완료 → 프로젝트 허브로 리다이렉트
- [ ] 최초 로그인 시 users 테이블에 자동 생성 (google_id, email, name, avatar_url)
- [ ] 기존 사용자 재로그인 시 last_login_at 갱신
- [ ] Access Token(JWT 15분) 발급 → 프론트 메모리 저장
- [ ] Refresh Token(7일) 발급 → httpOnly Cookie 저장 + DB 해시 저장

---

## US-002: 토큰 자동 갱신 (REQ-AUTH-002, REQ-AUTH-003, REQ-AUTH-004)

**As a** 로그인된 사용자  
**I want to** 세션이 자동으로 유지되길 원한다  
**So that** 15분마다 재로그인하지 않아도 된다

### 인수 기준
- [ ] Access Token 만료 시 Refresh Token으로 자동 갱신
- [ ] 갱신 시 Token Rotation 적용 (이전 RT → used=true, 새 RT 발급)
- [ ] 동시 여러 API 요청이 401 받으면 refresh는 1회만 실행, 나머지는 큐에 대기
- [ ] 페이지 새로고침 시 AuthProvider가 refresh 호출하여 인증 복원
- [ ] Refresh Token 만료/무효 시 로그인 페이지로 리다이렉트

---

## US-003: 로그아웃 (REQ-AUTH-005)

**As a** 로그인된 사용자  
**I want to** 로그아웃할 수 있다  
**So that** 다른 사람이 내 계정에 접근할 수 없다

### 인수 기준
- [ ] 로그아웃 버튼 클릭 → POST /api/auth/logout 호출
- [ ] DB에서 해당 Refresh Token 삭제
- [ ] Cookie에서 teamflow_rt 제거
- [ ] 프론트 authStore 초기화 → 로그인 페이지로 이동

---

## US-004: 워크스페이스 생성 (REQ-WS-001)

**As a** 로그인된 사용자  
**I want to** 팀 워크스페이스를 생성하고 싶다  
**So that** 팀원들과 함께 프로젝트를 관리할 수 있다

### 인수 기준
- [ ] 프로젝트 허브에서 "새 워크스페이스" 버튼 클릭 → 생성 모달
- [ ] 워크스페이스 이름, slug 입력 → POST /api/workspaces
- [ ] 생성자는 자동으로 Admin 역할 부여
- [ ] 생성 완료 후 프로젝트 허브에 카드 표시

---

## US-005: 멤버 초대 (REQ-WS-002, REQ-WS-003)

**As a** 워크스페이스 Admin  
**I want to** 팀원을 이메일 또는 링크로 초대하고 싶다  
**So that** 팀원들이 워크스페이스에 합류할 수 있다

### 인수 기준
- [ ] 이메일 초대: 가입된 사용자 → 즉시 멤버 추가 (Member 역할)
- [ ] 이메일 초대: 미가입 사용자 → 가입 후 자동 추가 (pending invite)
- [ ] 링크 초대: 초대 링크 생성 → 링크 접속 시 자동 합류
- [ ] 초대 링크는 만료 기한 설정 가능 (기본 7일)

---

## US-006: 랜딩 페이지 (REQ-LAND-001)

**As a** 미인증 방문자  
**I want to** TeamFlow가 무엇인지 알고 싶다  
**So that** 서비스를 시작할지 판단할 수 있다

### 인수 기준
- [ ] 서비스 소개 텍스트 + 핵심 가치 3가지 (통합, 실시간, 단순함)
- [ ] "Google로 시작하기" CTA 버튼 → OAuth 플로우 시작
- [ ] 이미 로그인된 사용자 접근 시 프로젝트 허브로 자동 이동

---

## US-007: 프로젝트 허브 (REQ-LAND-002, REQ-LAND-003, REQ-LAND-004)

**As a** 로그인된 사용자  
**I want to** 내가 참여 중인 프로젝트들을 한눈에 보고 싶다  
**So that** 원하는 프로젝트에 빠르게 진입할 수 있다

### 인수 기준
- [ ] 참여 중인 워크스페이스별 프로젝트 카드 목록 표시
- [ ] 각 카드에 프로젝트명, 멤버 수 표시
- [ ] "새 프로젝트 만들기" 카드 → 프로젝트 생성 모달 (이름, 설명, 키)
- [ ] 프로젝트 카드 클릭 → 해당 프로젝트 워크스페이스로 진입
- [ ] 워크스페이스 진입 시 빈 워크스페이스 셸(사이드바 + 빈 메인) 표시

---

## 스토리 매핑

| 스토리 | 요구사항 ID | Phase |
|--------|------------|-------|
| US-001 | REQ-AUTH-001, 006 | Phase 2 |
| US-002 | REQ-AUTH-002, 003, 004 | Phase 2 |
| US-003 | REQ-AUTH-005 | Phase 2 |
| US-004 | REQ-WS-001 | Phase 3 |
| US-005 | REQ-WS-002, 003 | Phase 3 |
| US-006 | REQ-LAND-001 | Phase 2~3 |
| US-007 | REQ-LAND-002, 003, 004 | Phase 3 |

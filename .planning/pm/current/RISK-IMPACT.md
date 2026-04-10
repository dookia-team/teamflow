# Sprint 1 — 리스크 레지스터

---

## 리스크 목록

| ID | 리스크 | 심각도 | 영향 | 완화 방안 | 상태 |
|----|--------|--------|------|-----------|------|
| RISK-001 | Google Cloud OAuth 클라이언트 미설정 | **high** | 인증 전체 기능 블로킹 | Sprint 시작 전 Google Cloud Console에서 OAuth 클라이언트 생성 | **해결됨** — 직접 발급 예정 |
| RISK-002 | PostgreSQL/Redis 로컬 환경 미구축 | medium | 백엔드 개발 블로킹 | docker-compose.yml로 로컬 DB/Redis 구성 | 미해결 |
| RISK-003 | 프론트-백 CORS 설정 | low | 통합 시 API 호출 실패 | SecurityConfig에 CORS 허용 origin 설정 | 미해결 |
| RISK-004 | 초대 이메일 발송 인프라 | medium | REQ-WS-002 이메일 초대 기능 | MVP에서 이메일 초대도 구현 (SendGrid 등 활용) | **해결됨** — 이메일 초대 구현 |
| RISK-005 | 프론트-백 동시 개발 시 API 계약 불일치 | medium | 통합 시 대량 수정 | HANDOFF.md의 API 계약을 기준으로 삼고, 변경 시 상호 고지 | 완화됨 |

---

## 사용자 결정 완료 항목

### RISK-001: Google OAuth 클라이언트 → **직접 발급** (2026-04-10 결정)
### RISK-004: 이메일 초대 → **MVP에서 구현** (2026-04-10 결정)

---

## 브라운필드 영향 맵

Sprint 1은 그린필드(신규 프로젝트)이므로 기존 코드 영향 없음.
단, `frontend/` 디렉토리에 이미 Vite + React 셋업이 완료되어 있으므로 FSD 구조를 해당 셋업 위에 구축한다.

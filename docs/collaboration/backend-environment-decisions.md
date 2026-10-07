# Issue #1 백엔드 실행환경 결정 기록

## 2026-10-07 선택 갱신

[MVP 결정 변경 기록](../specs/Discushion_MVP_결정변경_2026-10-07.md)에 따라 서비스 선택을 반영했다. #74는 변경 계약/문서, #75는 별도 시연 계정 준비다. 아래 과거 실행 검증 기록을 새로운 외부 서비스 검증으로 재사용하지 않는다.

개정일: 2026-10-07 (Asia/Seoul), 제품 기준 v10.2. 서비스 결정 기록이며 실행 검증일/대상은 아래 이력에서 별도로 확인한다. 작업 역할은 BE2이며, 작업 브랜치는 `back/feature/1-env`, PR base는 `back/develop`이다. 사용자 답변으로 정해진 선택과 팀 협의가 남은 항목을 구분한다. 이 기록은 API 정본과 제품 정책을 대체하지 않는다.

## 확인된 선택

| 항목 | 선택 | 근거·상태 |
| --- | --- | --- |
| Frontend | Next.js, TypeScript, Tailwind CSS | 사용자 제공. 버전·App Router 여부는 실제 package.json에서 확인한다. |
| Backend | Java, Spring Boot, Gradle | 사용자 답변으로 선택. |
| DB | Supabase PostgreSQL | 사용자 답변으로 선택. 프로젝트 접속정보와 연결 검증은 아직 없다. |
| 배포 도구·서비스 | Vercel 플러그인 | 사용자 답변으로 선택. 실제 프로젝트·리전·배포는 Issue #30에서 확인한다. |

Frontend의 TanStack Query, React Hook Form, Zod, Zustand, Axios는 선택 후보다. Backend 초기화의 선행조건으로 설치하지 않는다. 지도 provider와 실시간 투표 방식도 확정하지 않는다.

## 구현 전에 확인할 항목

| 항목 | 현재 상태 | 담당·후속 작업 |
| --- | --- | --- |
| JDK·Spring Boot·Gradle 버전 | 기존 문서의 확정 버전은 미정. 실행 골격은 Java 17, Spring Boot 4.0.8, 공식 Initializr의 Gradle 9.7.1로 구성 | BE1·BE2가 초기화 버전 검토, Issue #1 |
| Health Check | API 초안 4.1절: 인증 없는 `GET /health`, HTTP 200, `{"data":{"status":"UP"}}`를 구현. 계약 합의 완료로 표시하지 않음 | BE1 계약 정합성 확인, BE2 구현 |
| 파일 저장 서비스 | Supabase Storage, 게시물 사진 공개·파일 삭제·미완료 업로드 24시간 정리 | BE2·FE, #13·#16·#30 / 계약 #74 |
| 인증 제공자 | Privy 이메일 OTP 로그인·인증. 자체 메일 코드 발급 대체; 별도 일반 메일 provider 선택 아님 | BE1·FE, 환경 BE2 / #6·#7·#8·#74 |
| AI 연동 서비스 | Google Gemini 3.5 Flash-Lite 선택. API 모델 ID·키/요금·실제 호출 확인 필요 | BE2, #20·#30 |
| 서버 배포 환경 | Vercel Container Images(베타)를 실행 후보로 문서화. 실제 프로젝트·설정 미정 | BE2, Issue #30 |
| Supabase 연결 방식 | JDBC transaction pooler 예시, TLS·prepared statement 비활성화·기본 pool 1. 실제 주소·계정은 프로젝트의 Connect 화면에서 확인 | BE2 실행환경, BE1 DB 모델 |

Supabase PostgreSQL과 Storage를 선택했다. 인증은 Privy이며 Supabase Auth를 추가 선택한 것으로 간주하지 않는다. 실제 비밀번호·키·접속정보는 저장소와 이 문서에 기록하지 않는다.

## Issue #1 범위와 검증

- 확정된 스택의 애플리케이션, 실행·테스트·build 설정, 환경변수 예시와 Health Check를 준비한다.
- Schema/Migration·도메인 Entity는 Issue #3, 공통 인증·권한은 Issue #4에서 각 담당자가 구현한다.
- 실제 테스트·build 결과는 아래 검증 기록에 남긴다. Supabase DB 연결·FE/BE 실제 연동·실제 배포는 미검증이다.

버전 선택 근거: 기존 애플리케이션 의존성이 없고 로컬에는 JDK 17.0.19가 있다. Java 17과 호환되는 Spring Boot 4.0.8 안정 버전으로 초기화했고, Gradle 9.7.1은 공식 Spring Initializr에서 생성한 Wrapper 버전이다. Snapshot/Milestone은 사용하지 않았다. Wrapper JAR와 배포 ZIP의 공식 SHA-256을 확인했다. 이는 이번 골격의 기술 선택 기록이며 외부 서비스의 미정 상태나 API 초안의 상태를 바꾸지 않는다.

당시 검증 시점에 Supabase 플러그인은 설치·활성화 상태였지만 조회 도구가 제공되지 않아 실제 프로젝트를 조회하지 못했다. Vercel 플러그인으로 공식 컨테이너 지원 문서와 이름에 discushion이 포함된 프로젝트 목록을 조회했으며 검색 결과는 없었다. 프로젝트·배포·DB·인증 설정은 변경하지 않았다.

외부 서비스 선택은 사용자 결정으로 갱신했다. 초기화 버전·Health 계약의 팀 확인과 FE/BE 실제 연동은 아래 사용자 지시에 따라 #30의 연결 단계로 이관했다. 초기화 코드 통과만으로 팀 승인이나 실제 연동 완료를 표시하지 않는다.

## 검증 기록

검증일: 2026-10-07 (Asia/Seoul), JAR 확인 완료 02:23. 확인자: Codex. 대상은 기준 SHA `d1114f4`에서 작성한 `back/feature/1-env`의 미커밋 작업 트리이며, 새 변경을 이미 커밋한 SHA로 기록하지 않는다.

| 검사 | 실제 결과 |
| --- | --- |
| `backend/`에서 `gradlew.bat --no-daemon test build --console=plain` | 성공. 테스트 4개, 실패·오류 0개. JAR 생성 완료 |
| 생성 JAR 직접 실행 + `GET /health` | 임시 포트의 실제 HTTP 서버에서 200 및 계약 JSON 확인, 실행 프로세스 종료 |
| Supabase 설정 테스트 | 가짜 접속값으로 DataSource와 lazy pool 설정 확인. 실제 원격 DB 연결 검증 아님 |
| Gradle Wrapper | JAR SHA-256 일치, 배포 ZIP checksum을 Wrapper 설정에 기록 |
| 실제 Docker build/run | 미실행. Docker Linux Engine이 실행되지 않아 `docker info`가 연결 실패 |
| 실제 Supabase DB 연결 | 미실행. 플러그인 조회 도구·실제 연결 설정 미확보 |
| FE/BE 실제 연동·Vercel 배포 | 미실행. FE 실행환경·배포 프로젝트 미준비 |

Vercel Functions의 요청 본문 제한은 4.5MB다. 제품 명세의 게시물 사진 합계 10MB와 기관 증빙 한도를 그대로 유지하려면 Issue #13·#12·#30에서 파일 저장소 직접 업로드 등 전송 경로를 FE/BE와 합의해야 한다. 요청 제한에 맞추려고 제품의 파일 한도를 임의로 줄이지 않는다. 파일 저장 서비스는 Supabase Storage로 선택했다. 직접 업로드/삭제·24시간 정리의 세부 계약과 실제 구성은 #74/#13/#30에서 확인한다. 기관 증빙 업로드는 이번 MVP에서 제외한다. [Vercel 요청 제한](https://vercel.com/docs/functions/limitations#request-body-size)

## 참고

- [Issue #1](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/issues/1)
- [Backend 협업전략](Discushion_백엔드_Git_GitHub_협업전략_2026-10-06.md)
- [API 계약 초안](../api/Discushion_API_SPEC_v2.md)
- [Supabase PostgreSQL 연결 안내](https://supabase.com/docs/guides/database/connecting-to-postgres)
- [Spring Boot 4.0 시스템 요구사항](https://docs.spring.io/spring-boot/4.0/system-requirements.html)
- [Vercel Container Images](https://vercel.com/docs/functions/container-images)

## 추가 환경 준비 (#30/#75)

- Privy 환경별 앱 설정·허용 도메인·토큰 검증/회원 연결, Supabase Storage 업로드/삭제 권한, Gemini 서버 키·사용 제한을 실제 프로젝트에서 확인한다.
- 가비아 구매 도메인의 사용할 호스트·DNS 권한·Vercel 실제 Java 실행 방식/프로젝트·HTTPS·CORS를 확인한다.
- 파일 정리의 실행 위치/간격·오류/재시도/관측과 24시간 기준은 #74/#13과 맞춘다. 무료 요금제·여유 용량·비용 0원은 확인 전 보장하지 않는다.
- #75의 시연 계정 생성/설정·재실행/초기화·인증정보 보관을 확정 담당자가 수행한다. 실제 키와 계정 인증정보는 저장소에 기록하지 않는다.

## #1 후속 정리: 환경변수와 확인 요청

2026-10-07, BE2 작업 범위. 기존 파일명과 경로를 유지해 이 기록·Backend README·환경변수 예시를 갱신한다. 아래 변수 이름은 프로젝트의 환경 설정 규약이며 실제 값이나 외부 서비스 구성을 확정한 기록이 아니다.

| 변수 | 용도·예시 | 비밀 여부·현재 적용 상태 |
| --- | --- | --- |
| `SPRING_PROFILES_ACTIVE` | `local` / `supabase` | 현재 코드 사용. local은 외부 서비스 없는 실행 확인용 |
| `PORT` | `8080` | 현재 코드 사용 |
| `DB_URL` | `jdbc:postgresql://<pooler-host>:6543/postgres?sslmode=verify-full&sslrootcert=<root-certificate-path>&prepareThreshold=0&connectTimeout=10&socketTimeout=30` | 현재 코드 사용. #3의 실제 JDBC 검증을 반영한 예시. 공식 CA 경로와 실제 접속 주소는 외부 관리 |
| `DB_USERNAME`, `DB_PASSWORD` | `<database-user>`, `<database-password>` | 현재 코드 사용. 비밀번호는 서버 비밀 값 |
| `DB_POOL_SIZE` | `1` | 현재 코드 사용. 실제 한도 확인 후 조정 |
| `PRIVY_APP_ID` | `<privy-app-id>` | 공개 앱 식별자. 후속 인증 구현용, 현재 코드 미사용 |
| `PRIVY_APP_SECRET` | `<server-only-privy-app-secret>` | 서버 API 호출 시 필요한 비밀 값. 토큰 검증만으로 필수라고 단정하지 않음; #74/#4에서 필요 여부 확인. 현재 코드 미사용 |
| `SUPABASE_URL` | `https://<project-ref>.supabase.co` | Storage 프로젝트 주소. 현재 코드 미사용 |
| `SUPABASE_SECRET_KEY` | `<server-only-supabase-secret-key>` | 서버 전용 권한 키. 브라우저·FE 변수에 노출 금지. 현재 코드 미사용 |
| `SUPABASE_STORAGE_BUCKET` | `<post-photo-bucket>` | 실제 버킷 이름은 #13/#30에서 확인. 현재 코드 미사용 |
| `GEMINI_API_KEY` | `<server-only-gemini-api-key>` | 서버 전용 비밀 값. 현재 코드 미사용 |
| `GEMINI_MODEL` | `<confirmed-api-model-id>` | 서비스 표시명과 API ID를 구분. #20/#30에서 사용 가능한 ID 확인 후 입력. 현재 코드 미사용 |

FE의 Privy 앱 ID 환경변수 이름은 FE 담당자가 확인한다. 사진 URL 공개 열람에 서버 secret key를 전달하지 않는다. FE 직접 업로드 여부·토큰 검증 키/JWKS 설정·회원 연결은 #74에서 합의한 뒤 필요한 변수와 구현을 갱신한다. 자체 SMTP·메일 발송 변수나 Supabase Auth 설정은 이번 선택만으로 추가하지 않는다.

비밀 값은 로컬 `backend/.env` 또는 실제 배포 환경의 비밀 변수에 설정한다. `.env.example`에는 자리표시자만 보관한다. 앱 ID 같은 공개 식별자와 비밀 키를 구분하고 서버 secret에 `NEXT_PUBLIC_` 접두사를 사용하지 않는다.

### BE1·FE 확인할 내용

| 확인 대상 | 제시하는 기준 | 확인 상태 |
| --- | --- | --- |
| 초기화 버전 | Java 17, Spring Boot 4.0.8, Gradle Wrapper 9.7.1. 현재 build.gradle/Wrapper 설정과 대조 | BE1·FE 확인 대기; 사용자 스택 선택과 팀 확인을 구분 |
| Health 계약 | 인증 없는 `GET /health`, HTTP 200, `application/json`, `{"data":{"status":"UP"}}` | 코드·API 정본 초안과 일치. BE1·FE 확인 대기 |
| Health 의미 | 프로세스가 HTTP 요청을 처리하는지 확인. DB·Privy·Storage·Gemini readiness를 보장하지 않음 | BE1·FE 확인 대기 |
| 선택된 서비스 | Supabase PostgreSQL/Storage, Privy 이메일 OTP, Gemini 3.5 Flash-Lite, Vercel 도구 | 사용자 선택 기록 있음. 팀 확인 대기 |

확인자는 실제 GitHub 계정/역할, 확인 날짜, 대상 commit 또는 PR, 동의 항목/이견을 #1 댓글 또는 리뷰에 남긴다. Codex의 코드·문서 대조 결과는 팀원의 확인을 대신하지 않는다. 현재 #1 댓글과 API 계약 검토표에는 BE1·FE의 위 확인 완료 근거가 없어 대기로 유지한다. 외부 팀원에게 메시지를 전송한 기록은 없다.

남은 범위: #1은 서비스 선택·환경변수 예시·현재 실행 골격 검증 기록을 PR로 반영한다. #74는 변경된 API/DB 상세 계약, #30은 초기화 버전·서비스 선택·Health 계약의 팀 확인과 실제 구성·연결·배포, #13/#20은 사진·AI 기능 구현이다. 이 후속 문서 변경만으로 #1을 닫거나 합의/실제 연동 완료 체크를 하지 않는다.

### 사용자 지시에 따른 #30 이관

2026-10-07 사용자 지시: 팀 승인과 실제 FE/BE 연동 완료 확인은 나중에 연결할 때 해결한다. 두 항목을 #1 초기화 단계의 진행을 막는 조건에서 제외하고 #30 연결 단계의 미완료 작업으로 옮긴다. #1의 초기화·환경변수 예시·검증 기록 변경은 PR로 반영하며, 이번 이관만으로 Issue를 닫지 않는다.

- #30에서 BE1·FE와 초기화 버전·서비스 선택·Health 계약/의미를 확인하고 확인자·날짜·대상 SHA/PR을 기록한다.
- #30에서 실제 FE 실행/배포 환경의 API 주소·CORS를 설정하고 Backend 호출·응답을 검증한다. Mock이나 Backend 직접 호출만으로 FE/BE 연동 완료를 표시하지 않는다.
- 전체 검증 #31 전에 두 항목을 해소한다. #74의 토큰·회원 연결·사진 등 상세 API/DB 계약 선행 조건은 유지한다.
- 읽기 전용 조회 기준 `front/develop`의 `4f4f4d5`에 Backend 버전과 Health 응답 참고 기록이 있으며 [FE PR #81](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/pull/81)이 병합돼 있다. 이는 팀 승인·실제 연동 완료 기록을 대신하지 않는다.

### 이번 후속 작업 검증

2026-10-07 16:44 (Asia/Seoul), 확인자 Codex. 기준 SHA `ea9005c7979e0a463644b43fe263ce0d91903411`에서 시작한 `back/feature/1-env-followup`의 미커밋 변경 기준이다. 변경은 `backend/.env.example`, `backend/README.md`, 이 기록의 3개 파일이며 코드·API 정본·DB Schema·FE 구현은 변경하지 않았다.

| 검사 | 실제 결과 |
| --- | --- |
| Backend `gradlew.bat --no-daemon test build --rerun-tasks --console=plain` | 성공. 작업 6개 재실행, JAR 생성. 기존 실제 HTTP Health/오류 응답 테스트와 DB 없는 설정 테스트 수행 |
| `git diff --check` | 통과 |
| 변경 문서의 상대 링크 대상 검사 | 통과 |
| `backend/.env` Git 제외 규칙 | 확인. 예시는 자리표시자이며 실제 비밀 값 추가 없음 |
| 실제 서비스 연결·Docker·배포·FE/BE 사용자 흐름 | 미실행. #30/관련 기능 Issue에서 실제 환경 검증 필요 |
| BE1·FE 계약 확인 | 확인 완료 근거 없음. 대기 유지 |

공식 자료: [Supabase 키](https://supabase.com/docs/guides/getting-started/api-keys), [Privy 토큰 확인](https://docs.privy.io/authentication/user-authentication/access-tokens), [Privy 서버 설정](https://docs.privy.io/basics/nodeJS-node/setup), [Gemini API 키](https://ai.google.dev/gemini-api/docs/api-key). 2026-10-07 조회. Supabase changelog Markdown 조회는 도구의 content-type 오류와 실행환경 DNS 제한으로 실패했으며 변경 로그 확인 완료로 기록하지 않는다. 이 작업은 실제 Supabase 프로젝트·Schema·Storage 권한을 변경하지 않는다.

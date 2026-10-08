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
| `PRIVY_APP_ID` | `<privy-app-id>` | 현재 인증 코드가 access-token audience와 공식 앱 JWKS 조회에 사용. FE와 같은 앱의 공개 식별자 |
| `PRIVY_APP_SECRET` | `<server-only-privy-app-secret>` | 현재 #6/#7 Privy 사용자 조회·검증된 이메일 확인에 필요. JWKS 토큰 검증만에는 불필요. 서버 전용 비밀 값 |
| `SUPABASE_URL` | `https://<project-ref>.supabase.co` | 사진 활성화 시 Storage adapter가 사용하는 프로젝트 주소 |
| `SUPABASE_SECRET_KEY` | `<server-only-supabase-secret-key>` | 사진 활성화 시 Storage adapter가 사용하는 서버 권한 키. 브라우저·FE 변수에 노출 금지 |
| `SUPABASE_STORAGE_BUCKET` | `<post-photo-bucket>` | 실제 공개 버킷 discushion-post-photos 준비됨. 사진 활성화 시 사용 |
| `GEMINI_API_KEY` | `<server-only-gemini-api-key>` | 서버 전용 비밀 값. 현재 코드 미사용 |
| `GEMINI_MODEL` | `<confirmed-api-model-id>` | 서비스 표시명과 API ID를 구분. #20/#30에서 사용 가능한 ID 확인 후 입력. 현재 코드 미사용 |

FE의 Privy 앱 ID 환경변수 이름은 FE 담당자가 확인한다. 사진 URL 공개 열람에 서버 secret key를 전달하지 않는다. 현재 인증은 Privy access token 직접 검증·공식 앱 JWKS·Privy subject 기반 로컬 회원 연결이며 새 사진은 API §13.9의 앱 서버 중계를 따른다. 실제 Privy 앱 구성/로그인과 배포·FE 연결은 별도 검증한다. 자체 SMTP·메일 발송 변수나 Supabase Auth 설정은 추가하지 않는다.

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

## #30 Render 무료 배포 진행 기록 (2026-10-08)

PR #178은 `back/develop`에 병합됐다(4fffb1e). 현재 배포 대상은 Vercel FE와 별도 Render Spring API이며 이전 Vercel Backend 예시를 대체한다. Render 설정은 저장소 루트 `render.yaml`과 Backend README의 배포 환경 절을 따른다. 무료 플랜·Singapore·자동 배포 꺼짐·사진 플래그 비활성 상태를 유지한다.

- Render GitHub 로그인 권한과 신규 가입 약관 동의는 사용자가 확인했고 이메일 인증도 완료했다. Discushion Workspace에 공개 저장소 URL로 Blueprint `discushion-api`를 생성했다. 서비스는 Docker·Free·Singapore이며 `back/develop`의 `4fffb1e`를 배포했다. 자동 배포와 PR Preview는 꺼져 있다.
- [Render 서비스](https://dashboard.render.com/web/srv-db3mtql9fdbs73efdhng)의 API 주소는 `https://discushion-api.onrender.com`이다. 승인된 서버 계정의 DB_URL·DB_USERNAME·DB_PASSWORD를 비밀 환경변수에 등록했고 관리자 감사 계정은 등록하지 않았다. CA는 Secret File `prod-ca-2021.crt`로 등록해 `/etc/secrets/prod-ca-2021.crt`를 참조한다. TLS `verify-full`과 기존 pooler 연결 설정을 유지했다. 실제 비밀 값은 이 문서와 Git에 저장하지 않는다.
- 최신 병합 코드에서 `DISCUSHION_VERIFY_SUPABASE=true`로 `gradlew.bat --no-daemon test --tests com.discushion.SupabaseJdbcSmokeTests --rerun-tasks --offline --max-workers=2 --console=plain`을 실행했다. 관리자 감사 3개 통과, 실패/오류/skip 0이다. Schema/적용 이력·서버 RLS·공개 API 역할 접근 차단을 SELECT-only로 확인했다.
- 로컬 `.env`의 실제 서버 계정으로 별도 JDBC SELECT-only 연결을 확인했다. TLS `verify-full`, 비-SUPERUSER·비-BYPASSRLS, `media_files` SELECT 허용·물리 DELETE 거부·Schema CREATE 거부, `regions` 조회가 통과했다. DB 데이터·역할·권한은 변경하지 않았다. 이는 Render 호스트에서의 연결 검증을 대신하지 않는다.
- CA 저장 후 최신 commit을 수동 재배포했다. [최종 배포](https://dashboard.render.com/web/srv-db3mtql9fdbs73efdhng/deploys/dep-db3mv8mgekts73fefing)는 2026-10-08 19:27 (Asia/Seoul)에 Live로 전환됐다. 첫 배포에서 Docker/JAR build가 통과했고 재배포는 같은 이미지의 캐시를 사용했다. Java 17·Spring Boot 4.0.8·supabase profile·포트 10000 서버 시작 로그를 확인했다.
- 배포 후 실제 HTTPS 호출: `GET /health` → HTTP 200, `{"data":{"status":"UP"}}`; `GET /api/v1/regions` → HTTP 200, `{"data":[],"meta":{"nextCursor":null,"hasNext":false}}`. 지역 데이터는 아직 0건이다. DB 의존 API 성공으로 Render 호스트의 서버 계정·CA/TLS 연결과 지역 조회 허용을 확인했다. Render에서 모든 테이블의 쓰기/거부 권한을 검증한 것은 아니며 로컬 감사 결과와 구분한다. 이 검증은 Backend API 검증이고 FE/BE 실제 연동 완료를 뜻하지 않는다.
- 무료 서비스 휴면 중에는 정리 worker가 계속 실행되지 않는다. 다음 #13 검증에서 재기동 후 만료 후보 정리 재개를 확인하며, 24시간 기준을 항상 켜진 worker의 정확한 실행 시각 보장으로 표현하지 않는다.
- 공유 DB의 사진 서버 중계 Migration 적용·최소 권한 확인·사진 활성화·배포 경계 10 MB/삭제/정리·FE 실제 연결은 #13/#30 잔여 조건이다. 로컬 Privy 설정이 미준비된 상태를 실제 로그인/가입 연동 성공으로 표시하지 않는다.

### #13 사진 DB 적용·Storage 비밀 설정 준비 (2026-10-08)

기준 back/develop dd5cb52에서 지정 공유 DB에 통합된 사진 중계 Migration1개를 적용하고 185컬럼·7개 이력·제약/trigger/함수 권한·사진 최소 권한/RLS·공개 역할 차단을 확인했다. 실제 관리자 SELECT-only 감사3개와 build도 통과했다. 앞선 사진 Migration 미적용 조건은 해소됐으며 상세 이력 대응과 참여 권한4개 rollout 대기는 [DB 연결 결정 기록](backend-db-connection-decisions.md)의 마지막 절을 따른다.

사용자가 기존 SUPABASE_SECRET_KEY의 Render 비밀 환경변수 등록을 승인했다. discushion-api에 SUPABASE_URL·SUPABASE_STORAGE_BUCKET·SUPABASE_SECRET_KEY를 Save only로 저장하고 저장 완료 화면을 확인했다. 기존 DB/TLS 설정과 사진 플래그 false는 유지했다. 설정 저장 후 재배포하지 않았으므로 Live4fffb1e의 새 설정 소비/Storage 연결 완료로 기록하지 않는다.

Privy 앱은 아직 준비되지 않았다는 사용자 답변에 따라 실제 Privy 앱·OTP·가입 회원 연결을 대기한다. 배포 사진 API 활성화와 실제10MB 전송/조회/삭제/24시간 후보 정리·휴면 후 재개, FE 주소/CORS와 실제 사용자 흐름 검증은 #13/#30/#31에서 해소한다. 로컬 합성 JWT·격리 DB의 실제 Storage 성공을 이 검증의 대체로 기록하지 않는다.

### Privy 앱 준비와 FE 전달 기준 (2026-10-08)

사용자가 앱 생성·서버 키 저장을 승인해 Discushion 앱을 무료 개발 모드로 생성했다. 공개 App ID `cmuzh7iga01ao0cjv2jqayvxa`를 ignored backend/.env와 Render discushion-api의 PRIVY_APP_ID에 저장했다. 이메일 OTP는 활성화, 다른 로그인 방법과 자동 지갑 생성은 비활성화 상태다. 현재 공급자 계정 로그인은 제품 시연 회원 로그인이나 로컬 회원 가입과 구분한다.

현재 코드가 조회하는 공식 api.privy.io 앱 JWKS에서 EC/P-256 공개키2개를 실제 확인했다. 자동 생성 Secret의 전체 값 재조회가 불가능해 추가 발급을 대기했으나, 사용자가 서버용 Secret 추가 발급·저장을 명시적으로 허용해 새 키1개를 발급했다. 기존 키는 삭제·교체하지 않았다. 새 원본 값을 ignored backend/.env의 PRIVY_APP_SECRET과 Render discushion-api의 같은 비밀 변수에 저장했다. 로컬 키로 공식 GET /v1/users?limit=1을 호출해 HTTP200·사용자0건을 확인했으며 App ID/Secret 서버 인증은 성공했다. 특정 회원의 검증된 이메일 조회·실제 OTP/가입 성공을 대신하지 않는다. Render는 Save only로 저장했으며 재배포·새 환경값 소비는 미검증이다. 앱은 개발 모드이며 화면의 제한은150명이다.

- 앱 이름은 Discushion, 사용 목적은 이메일 OTP 인증이다. FE와 BE가 동일한 앱 ID를 사용한다. 지갑·블록체인 기능이나 별도 로그인 방식을 이번 준비의 선행조건으로 추가하지 않는다.
- backend/.env의 PRIVY_APP_ID는 JWT audience 및 공식 앱 JWKS 조회에, PRIVY_APP_SECRET은 현재 PrivyVerifiedEmailSource의 서버 사용자 조회/검증된 이메일 확인에 사용한다. App Secret을 FE/NEXT_PUBLIC_ 변수·Git에 저장하지 않는다. JWT 공개키 검증만에는 Secret이 필요하지 않지만 현재 가입 흐름에는 필요하다.
- 사용자가 가비아 구매 도메인 galds.shop을 알려줬다. 예정 FE 로그인 origin은 https://galds.shop이다. 실제 FE 배포/DNS/HTTPS 연결을 확인한 뒤 해당 origin을 Privy에 등록한다. localhost 포트·Vercel 배포 주소·www 사용 여부는 별도로 확인하며 임의로 추가하지 않는다. Render API URL을 FE 로그인 origin으로 대신하거나 와일드카드 전체 허용을 적용하지 않는다.
- 조회한 origin/front/develop package.json에는 Privy SDK가 없고 frontend/.env.example에도 Privy 앱 변수가 없다. AuthSessionAdapter는 source='privy' 계약을 제공하지만 실제 SDK/OTP/token adapter 연결은 확인되지 않았다. FE 파일은 변경하지 않았다.
- FE 후속은 이메일 OTP 성공 → 최신 Privy access token 획득 → 공통 앱 API Bearer 전달 → 기존 가입/회원 API와 오류 계약에 따라 로컬 가입 상태 반영 → 최초 가입 후 상태 재조회다. identity token이나 클라이언트 userId/email을 서버 인증 근거로 사용하지 않는다. 로그아웃·토큰 갱신·실패 복구는 기존 계약을 따른다.
- 준비 완료 판정은 앱 구성/환경값 저장뿐 아니라 실제 공식 JWKS·서버 사용자 조회와 Privy 로그인/가입 흐름 검증을 포함한다. 실제 App Secret의 Render 저장은 전달 대상과 비밀 값을 명시한 사용자 승인 후 진행한다. 사진 플래그는 해당 실제 배포 검증 전까지 false를 유지한다.

공식 설정 근거: [Privy 서버 설정](https://docs.privy.io/basics/nodeJS-node/setup), [Privy 사용자 조회 API](https://docs.privy.io/api-reference/users/get-all). 프로젝트의 실제 소비 방식은 IdentityConfiguration·PrivyJwksVerificationKeySource·PrivyVerifiedEmailSource와 API 정본을 대조했다.

2026-10-08 공개 DNS 확인: galds.shop의 NS는 가비아 ns.gabia.co.kr/ns1.gabia.co.kr/ns.gabia.net이며, 조회 시점의 apex A/AAAA 답변에는 IP 주소가 없었다. FE 사이트 배포와 해당 도메인의 DNS/HTTPS 연결은 미확인·후속이다. 이번 준비에서 가비아 DNS나 Vercel 설정을 변경하지 않았다.

### Privy 설정 적용 재배포·실제 OTP 검증 준비 (2026-10-08)

저장한 Privy/Storage 설정을 소비하도록 기존 Live commit `4fffb1ebbbfcad2a674776ad24b788e80d257d78`을 수동 재배포했다. [배포 기록](https://dashboard.render.com/web/srv-db3mtql9fdbs73efdhng/deploys/dep-db3ohhk9v7es73dsrm7g)은 21:12:54 KST에 시작해 Live로 전환됐으며 소요 시간은 1분28초다. Java17·Spring Boot4.0.8·supabase profile 시작을 확인했다. 재배포 후 HTTPS `/health`는 200/UP, `/api/v1/regions?q=월계1동`은 200/지역1건으로 실제 DB 조회에 성공했다. 새 Privy 설정의 특정 회원 인증·가입 성공은 별도로 확인한다.

조회 시점의 최신 back/develop은 `26cb5e8`이며 북마크/기관 목록과 별도 권한 Migration 변경이 포함돼 있다. 이번 배포는 기존 사진·인증 코드 검증으로 한정해 최신 develop 전체 배포나 추가 권한 Migration 적용을 수행하지 않았다. 배포 SHA와 최신 develop을 혼동하지 않는다.

사용자가 지정한 활동 지역을 위해 공유 DB의 빈 regions에 `서울특별시 노원구 월계1동` 1건(ID1)을 준비했다. 확인되지 않은 external_code/map_feature_key는 NULL로 유지하고 이웃/기관 자격을 부여하지 않았다. 이메일 OTP와 가입은 프로젝트 밖의 임시 Privy SDK 검증 화면에서 실제 Render API에 연결해 확인했다. frontend 브랜치/파일은 수정하지 않았으며 이 시험을 프로젝트 FE/BE 사용자 흐름 완료로 표시하지 않는다.

21:40 KST 공식 Privy 로그인 모달에서 실제 OTP 인증이 성공했다. 실제 access token으로 `/api/v1/users/me`는 가입 전403을 반환했다. 사용자가 필수 이용약관/개인정보 수집 동의값 true·마케팅 false와 시연 회원 생성을 승인한 뒤 `/api/v1/auth/sign-up` 신규 요청201·재요청200, 본인 프로필 조회200을 확인했다. 공유 DB의 Privy 연결/가입 완료 회원1명과 동의3행의 값도 대조했다. 이메일 일치로 연결하거나 합성 토큰을 사용하지 않았다.

### Render 실제 사진 흐름 검증 (2026-10-08)

사용자가 폐기용 합성 사진/만료 예약 생성·삭제와 검증용 사진 활성화를 승인했다. `PHOTO_UPLOADS_ENABLED`·`PHOTO_STORAGE_WIRE_VERIFIED`·`PHOTO_CLEANUP_ENABLED`를 true로 저장하고 기존 `4fffb1e`를 [재배포](https://dashboard.render.com/web/srv-db3mtql9fdbs73efdhng/deploys/dep-db3p1l49v7es73dufs2g)했다. 21:47:16 KST 시작, 1분29초 후 Live이며 worker 주기는60000ms를 유지한다. 검증 후에도 이 개발 서버에서 활성화 상태를 유지하며 자동 배포는 꺼져 있다. 실패 시에는 업로드를 비활성화하고 RUNNING/UNKNOWN 증거를 보존한다.

| 실제 검증 | 결과 |
| --- | --- |
| 실제 회원 사진 예약·24시간 기준 | HTTP201. createdAt부터 cleanupEligibleAt까지86400000ms |
| 비로그인 PUT | HTTP401/UNAUTHORIZED |
| 정확히10,000,000 bytes PNG 전송 | Render PUT200. Supabase 쓰기 ACKNOWLEDGED |
| complete·익명 공개 조회 | complete200/UNLINKED/canAttach, 공개 조회200/원본10MB 내용 일치 |
| complete 재요청 | HTTP200. uploadedAt 불변 |
| 10,000,001 bytes 예약 | HTTP413/PHOTO_SIZE_EXCEEDED |
| 삭제 예약·늦은 PUT | DELETE202/DELETE_PENDING, 이후 PUT409/PHOTO_DELETION_PENDING |
| 실제 정리 worker 최종 삭제 | GET200/DELETED/deletionCompleted, 공개 URL400(파일 없음), Storage object0개 |
| 만료 임시 예약 | 생성 시각이25시간 전인 폐기용 SERVER_RELAY 예약을 생성. 실제 worker가1회 처리해 DELETED, 오류 없음 |

초기 검증 파일2행의 최종 DELETED 이력은 보존했으며 실제 Storage 파일은 남지 않았다. 만료 예약은 시험용으로 처음부터 과거 생성 시각을 지정한 새 행이며 기존 행/trigger/시각 제약을 수정하지 않았다. 실제24시간을 기다린 시험으로 기록하지 않는다. 운영 코드·기존 Migration·FE 파일·사용자 자격을 변경하지 않았다.

22:06:30 KST에 마지막 API 요청21:51:30 이후15분의 실제 자연 휴면·graceful shutdown/DB pool 종료를 로그에서 확인했다. 수동 재시작/배포로 대체하지 않았다. 휴면 중에 승인된 별도 만료 예약1행(ID3)을 생성하고 UPLOADING·delete_requested_at NULL·정리 시도0회가 유지됨을 확인했다. HTTPS Health 요청으로 깨운 뒤76.29초에 UP 응답을 받았으며 새 인스턴스 x7wht가22:10:44에 시작됐다. worker는22:10:56에 후보1개를 처리하고 해당 예약을 DELETED·시도1회·오류 없음으로 전환했다.

최종 검증 이력은 DELETED3행이며 Storage object0개다. 이 결과로 실제 휴면 후 정리 재개 대기는 해소됐다. 무료 플랜은 휴면 동안 정리가 지연되므로24시간은 정리 대상의 기준이며 정확한 시각의 실행 보장은 아니다([공식 무료 서비스 안내](https://render.com/docs/free#spinning-down-on-idle)). 프로젝트 FE origin/CORS·실제 SDK/adapter/화면 연결과 게시물 사진 연결은 해당 후속 #14/#30/#31에서 검증하며 #13/#30을 임의로 닫지 않는다.

# Discushion Backend

Issue #1의 실행 골격이다. Java 17, Spring Boot 4.0.8, Gradle Wrapper 9.7.1을 사용한다. 사용자 선택은 Supabase PostgreSQL·Storage, Privy 이메일 OTP, Google Gemini 3.5 Flash-Lite다. 버전·Health 계약의 BE1·FE 확인과 실제 서비스 구성/연동은 별도로 기록한다. [결정 기록](../docs/collaboration/backend-environment-decisions.md)을 함께 확인한다.

## 로컬 실행

JDK 17을 설치하고 `JAVA_HOME`을 해당 JDK 디렉터리로 설정한다. 시스템 Gradle 설치는 필요하지 않다. 아래 명령은 `backend/` 안에서 실행한다.

```powershell
# Windows PowerShell: 경로는 실제 설치 위치에 맞춘다.
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17.0.19'
.\gradlew.bat bootRun
```

```bash
# macOS / Linux: JAVA_HOME은 설치한 JDK 17 경로로 설정한다.
sh ./gradlew bootRun
```

기본 `local` 프로필은 DB나 외부 서비스 없이 시작한다. 기본 포트는 8080이며 `PORT` 또는 Spring의 `SERVER_PORT` 환경변수로 변경할 수 있다. `backend/.env`가 있으면 Spring이 properties 형식으로 읽는다. 값 앞에 `export`를 붙이지 않는다.

```powershell
Invoke-RestMethod -Uri http://localhost:8080/health
```

`GET /health`는 인증 없이 HTTP 200과 `{"data":{"status":"UP"}}`를 반환한다. API 초안 4.1절을 구현한 서버 생존 확인이며, DB·파일 저장·이메일·AI 연결 성공을 뜻하지 않는다. BE1의 계약 정합성 확인은 아직 필요하다. `local`은 향후 DB 의존 기능의 검증 환경으로 사용하지 않는다.

## Supabase 연결

1. `.env.example`을 `backend/.env`로 복사한다.
2. `SPRING_PROFILES_ACTIVE=supabase`로 바꾼다.
3. Supabase 프로젝트의 **Connect**에서 실제 호스트와 계정명을 확인하고 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 로컬 파일 또는 배포 환경에 설정한다. DB URL에는 비밀번호를 넣지 않는다.
4. `bootRun` 또는 아래 JAR 명령으로 시작한다.

서버리스 실행 후보와 IPv4 환경을 고려해 transaction pooler(6543)를 사용한다. `prepareThreshold=0`, `sslmode=verify-full`, 공식 Supabase 루트 CA 파일을 지정하는 `sslrootcert`를 연결 예시에 설정했다. 초기 Java 기본 신뢰 저장소 방식은 실제 인증서 체인을 신뢰하지 못해 실패했으므로 프로젝트 연결 전용 CA 파일로 보완했다. 시스템/Java 전역 인증서는 바꾸지 않고 인증서·호스트 검증을 유지한다. 연결 10초/소켓 30초는 초기 기술 기준이다. direct/session으로 바꾸면 실제 URL과 이유를 기록한다. [Supabase 연결 안내](https://supabase.com/docs/guides/database/connecting-to-postgres), [결정 기록](../docs/collaboration/backend-db-connection-decisions.md)

`DB_POOL_SIZE` 기본값은 인스턴스당 1이다. 트래픽과 Supabase 연결 한도를 확인한 뒤 조정한다. SQL 초기화는 비활성화돼 있으며 Schema/Migration은 BE1의 Issue #3에서 관리한다. ORM·도메인 Entity·테이블 생성은 포함하지 않는다. Storage는 별도 사용자 결정으로 선택됐으며 인증은 Privy를 사용한다. Supabase Auth를 추가 채택하지 않는다. 2026-10-07 사용자 지정 개발 DB에 실제 Spring JDBC SELECT-only 검증 3개가 통과했다. 초기 검증용 postgres 계정이며 최종 앱 최소 권한 역할·권한표·계정 검증은 [DB 연결 결정 기록](../docs/collaboration/backend-db-connection-decisions.md)의 #4/#30 후속 조건을 따른다. DB 연결 검증은 Storage·Privy·Gemini의 실제 연결이나 FE/BE 연동 완료를 뜻하지 않는다.

원격 연결 회귀 검사는 `backend/`에서 `DISCUSHION_VERIFY_SUPABASE=true`를 설정하고 `./gradlew.bat test --tests com.discushion.SupabaseJdbcSmokeTests --rerun-tasks`로 명시적으로 실행한다. 지정 개발 프로젝트만 검사하며 비밀값은 `.env`에서 읽고 쿼리는 SELECT만 수행한다. 기본 실행에서는 이 3개 검사가 skip된다. 현재 PC의 CA 파일은 Git 제외 `.local-db/supabase-prod-ca-2021.crt`다. 다른 PC/배포 환경에서는 공식 대시보드의 SSL Certificate를 다운로드하고 실제 파일 경로를 지정한다. 이 원격 검사는 fixture를 사용하는 localhost 전용 Schema 시험과 구분한다.

## 외부 서비스 환경변수 준비

`.env.example`의 기존 실행/DB 변수는 현재 코드에서 사용한다. 추가한 `PRIVY_*`, `SUPABASE_*`, `GEMINI_*`는 후속 구현을 위한 프로젝트 변수 이름과 자리표시자이며 현재 코드에서 읽지 않는다. 예시를 채우는 것만으로 외부 서비스가 연결되지 않는다. 변수별 용도·비밀 여부·후속 Issue는 [환경 결정 기록](../docs/collaboration/backend-environment-decisions.md)의 환경변수 표를 따른다.

Privy 앱 ID는 FE에도 필요한 공개 식별자이며 FE 변수 이름은 해당 담당자가 합의한다. Privy 앱 secret, Supabase secret key, DB password, Gemini API key는 Backend의 로컬 `.env` 또는 배포 환경의 비밀 변수로 관리한다. `NEXT_PUBLIC_` 접두사를 붙이거나 FE에 전달하지 않는다. 현재 FE 파일은 변경하지 않는다. Supabase 공개 사진 열람은 URL로 가능하지만 업로드·삭제 권한은 #74/#13에서 합의하고 서버에서 확인한다.

## 테스트와 build

```powershell
.\gradlew.bat test
.\gradlew.bat build
# 실패 후 재검증은 기존 결과를 재사용하지 않는다.
.\gradlew.bat test --rerun-tasks
java -jar build/libs/discushion.jar
```

```bash
sh ./gradlew test
sh ./gradlew build
sh ./gradlew test --rerun-tasks
java -jar build/libs/discushion.jar
```

테스트는 실제 임시 HTTP 서버에서 익명 Health 응답, 지원하지 않는 HTTP Method, 없는 경로를 확인한다. 별도 설정 테스트는 `supabase` 프로필을 가짜 접속값으로 확인하며 DB에 연결하지 않는다. 테스트는 `backend/.env`를 읽지 않는다. 결과는 `build/reports/tests/test/index.html`, JAR는 `build/libs/discushion.jar`에 생성된다.

## Vercel 배포 준비

사용자가 선택한 배포 도구는 Vercel 플러그인이다. Spring Boot는 `Dockerfile.vercel`로 컨테이너 빌드하도록 준비했다. Vercel Container Images는 베타이며, 서버는 `$PORT`에 HTTP 요청을 받는다. [Vercel 공식 문서](https://vercel.com/docs/functions/container-images)

```powershell
# Docker가 실행 중인 backend/에서 검증한다.
docker build -f Dockerfile.vercel -t discushion-backend:local .
docker run --rm -p 8080:8080 -e SPRING_PROFILES_ACTIVE=local discushion-backend:local
```

실제 배포는 Issue #30에서 프로젝트·요금제·리전·이미지 실행·DB 연결과 FE 연동을 검증한다. Backend를 별도 Vercel 프로젝트로 배포한다면 Root Directory는 `backend/`, `PORT=8080`을 프로젝트 환경변수로 설정한다. FE와 같은 프로젝트에 배포할 경우에는 Services와 `/health`, `/api/v1/*` 라우팅을 FE 담당자와 합의한다. 현재 루트 `vercel.json`, 외부 프로젝트 생성, 배포는 포함하지 않는다.

Vercel의 요청 본문 제한(4.5MB)과 게시물 사진 합계 10MB를 함께 만족할 전송 경로는 #74/#13/#30에서 합의한다. 저장 서비스는 Supabase Storage이며 JPG/PNG·최대 10장·합계 10MB, 저장 파일 삭제, 미완료 업로드 24시간 정리 정책을 따른다. 기관·이웃 증빙 제출은 이번 MVP에서 제외한다. 제품 한도를 임의로 줄이거나 업로드 계약을 만들지 않는다. [Vercel 요청 제한](https://vercel.com/docs/functions/limitations#request-body-size)

FE/BE 실제 연동은 실제 FE가 실행 중인 Backend를 호출해야 한다. Health API 직접 호출이나 Backend 테스트만으로 FE/BE 연동을 완료 처리하지 않는다. API 오류·인증·권한 계약은 BE1의 Issue #2·#4와 맞춘다.

## 협업

일반 Backend Issue는 `back/feature/<번호>-<기능>`에서 작업하며 PR base는 `back/develop`이다. BE1은 A·C·D, BE2는 B·실행환경을 담당한다. 각 담당자는 자기 API/DB 변경을 작성하고 API 정본·DB 전역 정합성은 BE1이 확인한다. `back/develop`과 `main`에는 직접 push하지 않는다. 실제 비밀번호·키·토큰·증빙 자료는 저장소에 넣지 않는다.

## 독립 개발용 테스트 기반

`com.discushion.contracts.identity`와 `.post`의 인터페이스/record는 공통 내부 계약이다. 실제 인증·회원 adapter는 BE1, 게시물 adapter는 BE2가 구현한다. 현재 인터페이스만 제공하며 운영 구현이나 인증 허용 bean은 없다.

`src/test/java/com/discushion/support/ContractFixtures.java`에서 고정 Clock, guest/검증된 주체/가입 미완료, 지역·기관 자격, 삭제 게시물, 종료 경계 투표와 선택지를 준비한다. 테스트별 새 `Members`/`Posts` 인스턴스에 필요한 데이터를 등록한다. `member(false, Set.of(), List.of())`는 가입 미완료, 타지역 Set은 자격 불일치, `institution(true)`는 평가 시각에 만료된 기관을 뜻한다. 최종 권한 결정은 소비 기능에서 구현·검증한다.

### #4 실제 identity 기반 사용

`com.discushion.identity`가 기존 identity port를 구현한다. /api/v1 Bearer를 검증한 subject로만 회원을 조회하고 요청별 servlet attribute에 주체를 보관했다가 제거한다. 무효 토큰을 익명으로 바꾸지 않는다. `CurrentActorProvider.current()`의 빈 값/미가입/미완료와 기능별 공개/회원/공유 게스트 접근은 각 기능이 판정한다.

실제 데이터 프로필에서 `JdbcMemberStore`는 `MemberQualificationReader`·`MemberWriteGuard` bean이다. `MemberAuthorization.lockCurrentCompletedMember()`는 **호출자의 같은 DataSource 쓰기 transaction**에서 users를 잠그고 최신 가입 상태를 조회한다. 이후 기능에서 실제 posts → polls → media 순서의 잠금·최종 지역/기관 유효기간/작성자·유형/상태를 확인한다. `requireActiveInstitution`은 호출 시 Clock으로 만료를 재평가한다. `isOwner`가 false이면 기능의 기존 수정/삭제 오류로 거부한다. 인증 시점 스냅샷이나 클라이언트 userId를 쓰기 권한 근거로 사용하지 않는다.

앱 ID는 기존 `PRIVY_APP_ID`에서 읽고, 실제 앱의 신뢰한 공개키는 BE2 #30과 공급/회전 방식을 확인한 `VerificationKeySource` bean으로 제공한다. 공개 SPKI PEM에는 `PemVerificationKeySource`를 사용할 수 있다. token의 jku/x5u로 외부 키를 찾지 않는다. 앱/키 부재는503이며 기본 허용·합성 키 bean을 등록하지 않는다. local Health는 DB/키 없이 기존대로 동작한다. 실제 Privy 설정·키 호환/회전·서버 계정/TLS는 아직 확인 대기다.

미가입과 가입 미완료는 사용자 결정에 따라 모두403 USER_REGISTRATION_REQUIRED로 회원가입 화면을 안내한다. 내부 상태 구분은 유지한다. 키/provider 장애503 AUTH_PROVIDER_UNAVAILABLE, DB/서버 내부 오류500 INTERNAL_ERROR는 기존 `{code,message,details,traceId}`를 유지하고 원문·token·subject를 숨긴다. 실제 소비 계약 확인은 FE·BE2와 별도로 기록한다.

`IdentityHttpIntegrationTests`는 합성 키와 localhost 실제 DB를 사용한 **테스트 전용 route/profile**다. 운영 JAR·API 목록에 포함하지 않는다. 기존 localhost JDBC 환경변수가 설정되면 신규 실제 DB/HTTP 시험이 실행되며 원격Supabase3개는 기존 opt-in을 유지한다. 실제 Privy/FE 연결·실제 서버 역할·#75 계정 검증과 구분한다. 실행 결과/남은 조건은 [계약 검토표 §12.5](../docs/api/Discushion_API_CONTRACT_검토표_2026-10-07.md), 기본 권한표는 [DB 연결 결정 기록](../docs/collaboration/backend-db-connection-decisions.md)을 따른다.

```java
var reader = new ContractFixtures.Posts()
        .add(ContractFixtures.post(1, PostStatus.PUBLISHED));
// reader를 서비스 생성자에 주입해 상대 adapter 없이 개별 조회 로직을 검증한다.
```

읽기 대체 구현의 `findForUpdate`는 예외를 발생시킨다. 쓰기 서비스의 단위 시험에는 해당 테스트에서 명시한 guard 대체 구현을 사용할 수 있지만 실제 DB 잠금 성공으로 보고하지 않는다. 실제 adapter 이후 같은 트랜잭션·자격 변경·게시물 삭제·투표 종료 경합을 JDBC 통합 시험으로 확인한다. 테스트 지원 파일은 JAR에 포함하지 않는다.

## #9 지역 후보 조회

- `GET /api/v1/regions?q=...&size=20&cursor=...`: 가입 전에도 Bearer 없이 조회한다. Bearer가 있으면 기존 공통 검증을 통과해야 한다. 조회가 가입 완료·이웃/기관 자격을 만들지는 않는다.
- 실제 `discushion.regions`만 SELECT한다. 지역명 부분 검색, NFC 이름/ID 순서의 cursor 페이지, 기본20/최대100, 기존 `data/meta`·숫자 ID·`mapFeatureKey` null을 사용한다. q/size/cursor 입력 오류는 기존400 VALIDATION_ERROR다. 공개 anon/authenticated DB 권한을 추가하지 않는다.
- 실제 설정된 `supabase` 프로필 DataSource를 사용한다. DB 없는 기본 `local` 프로필은 기존 Health 실행을 유지하며 지역 요청은500 INTERNAL_ERROR로 표시한다. 빈 운영 DB를 임의/test 데이터로 채우거나 DB 미설정을 정상 빈 목록으로 숨기지 않는다.
- `RegionQueryTests`는 입력/cursor, `JdbcRegionCatalogIntegrationTests`는 실제 localhost DB 검색·페이지·공유 FK, `RegionHttpIntegrationTests`는 **운영 Region route**와 실제 JDBC/공통 인증을 검증한다. HTTP 시험의 데이터·앱/서명키는 test-only다. 기존 CI의 localhost 환경에서 함께 실행하고 실제 Supabase3개는 기존 opt-in을 유지한다.
- 계약·실행 결과와 공동 확인 대기는 [API 정본 §4.6](../docs/api/Discushion_API_SPEC_v2.md#46-지역-후보), [계약 검토표 §14](../docs/api/Discushion_API_CONTRACT_검토표_2026-10-07.md#14-9-지역-후보-조회-계약과-검증-2026-10-08)를 따른다. 실제 지역 원천/지도 대응·FE 사람 확인/연동·실제 서버 역할 검증 전 전체 #9 완료로 표시하지 않는다. 지도 조회 구현은 BE2 #19다.
- 최신 검증(2026-10-08 01:16 KST): 사용자 요청으로 사진 보완 Migration을 지정 Supabase 개발 DB에 적용했다. 원격과 localhost 모두27테이블·180컬럼·이력5개다. 원격 검사3개를 포함한 전체69개 통과/실패0/오류0/skip0, build 성공. SupabaseJdbcSmokeTests의 기대 이력/컬럼과 사진 추적4컬럼·검증된6제약·유효2인덱스 검사를 갱신했다. 실제 서버 역할·Storage worker/Privy/FE 연결 완료와 구분하며 검토표 §14.5와 DB 연결 결정 기록의 최신 적용 절을 따른다. 이전 §14.4의176컬럼/4이력은 적용 전 역사 기록이다.

## GitHub CI와 CD 상태

추가 batch port는 `ParticipationSnapshotReader`(BE1), `PostSummaryReader`(BE2)다. `SharedReadFixtures`에서 공개 집계/본인 상태 분리와 공개/삭제 요약을 시험한다. `PostDeletionParticipant`는 기존 transaction에서 북마크를 해제하는 BE1 adapter의 규약이며 아직 구현은 없다. 정확한 batch 누락·오류·접근·삭제/보존 의미는 계약 검토표 §11.9를 따른다. guest와 타회원의 개인 상태가 섞이지 않는지, 삭제 요약에 display가 없는지를 회귀 검증한다.

루트 `.github/workflows/backend-ci.yml`은 `back/develop`/`main` 대상 PR, Backend 브랜치 push, 수동 실행에서 다음을 수행한다. 파일 경로 필터를 두지 않아 필수 검사 대기 문제를 피한다.

1. Linux 임시 Docker PostgreSQL 17.11에 현재 Migration을 순서대로 적용한다.
2. 기존 `schema_integrity.sql`, `api-role-isolation.sql`을 각각 빈 시험 DB에서 실행한다. 컨테이너는 host network/127.0.0.1:55432를 사용해 기존 안전 가드를 유지한다.
3. Java 17로 `test build --rerun-tasks`를 실행한다. localhost JDBC 4개는 실행 대상으로, 실제 Supabase 검증 3개는 명시적으로 비활성화한다.
4. 테스트 결과와 성공한 JAR를 artifact로 남기고 시험 컨테이너를 제거한다.

CI 비밀번호는 격리된 컨테이너용 합성값이며 운영 비밀값이 아니다. Schema 시험은 실제 PostgreSQL 제약 검증이지만 모의 역할 검사는 실제 Supabase 서버 역할/GRANT 검증을 대신하지 않는다. SQL 직접 적용 CI는 기존 Windows `run-schema-tests.ps1`의 Supabase CLI Migration history/재실행 검사를 대신하지 않는다. 코드 소유권/리뷰 규칙은 AGENTS.md를 따르며 GitHub 관리자가 `Backend tests and build`를 필수 검사로 설정하고 리뷰 강제 여부를 확인해야 한다.

워크플로 파일은 PR 통합/원격 반영 후 GitHub에서 실행된다. CD는 #30에서 실제 Vercel 대상·배포 방식·리전·환경별 비밀 변수·DB 연결과 health/실패 복구를 확인한 뒤 연결한다. 현재 자동 배포 워크플로는 제공하지 않으며 CI JAR 생성은 배포 성공을 뜻하지 않는다.

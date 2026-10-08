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

`.env.example`의 실행/DB 변수와 `PRIVY_APP_ID`·`PRIVY_APP_SECRET`은 현재 코드에서 사용한다. 사진 활성화 시 `SUPABASE_*`를 Storage adapter가 사용하며 `GEMINI_*`는 아직 후속 구현용이다. 예시를 채우는 것만으로 외부 서비스가 연결되지 않는다. 변수별 용도·비밀 여부·후속 Issue는 [환경 결정 기록](../docs/collaboration/backend-environment-decisions.md)의 환경변수 표를 따른다.

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

## 배포 환경

Vercel은 프론트엔드 프로젝트로 유지하고 Spring Boot API는 별도 Render Web Service에서 실행한다. 저장소 루트의 [`render.yaml`](../render.yaml)은 `back/develop`을 기준으로 `backend/Dockerfile.vercel`을 빌드하며, 싱가포르 리전·`/health` 확인·자동 배포 꺼짐을 설정한다. Render가 `PORT`를 주입하며 Spring 설정은 이를 사용한다. Render는 현재 Web Service 지역으로 싱가포르를 제공한다. [Render Blueprint](https://render.com/docs/blueprint-spec), [Render 리전](https://render.com/docs/regions)

사용자 결정으로 `plan: free`를 사용한다. 무료 Web Service는 15분 동안 요청이 없으면 휴면에 들어가고 다음 요청 때 다시 시작하며, 재시작에 약 1분이 걸릴 수 있다. 시연 전에 `/health` 응답을 확인하고 첫 요청 지연을 고려한다. 무료 인스턴스 시간은 워크스페이스당 월 750시간이며 공유 사용량·전송량·빌드 한도를 대시보드에서 확인한다. 설정 파일 준비만으로 실제 서비스 생성이나 배포를 완료 처리하지 않는다. [Render 무료 플랜 제한](https://render.com/docs/free)

휴면 중에는 Spring의 사진 정리 스케줄도 실행되지 않는다. 미완료 업로드의 24시간 만료 기준과 삭제 안전성 정책은 유지하며, 휴면으로 늦어진 후보를 재시작 후 처리하는지 #30에서 검증한다. 정시 실행이 필요한 별도 스케줄 방식은 활성화 전에 확인하며, 무료 Web Service만으로 24시간 정리 작업의 상시 실행을 보장했다고 기록하지 않는다. 사진과 DB 데이터는 기존 Supabase에 보관하고 Render의 임시 로컬 파일에 영구 저장하지 않는다.

Render Blueprint의 `sync: false` 항목은 비밀값을 YAML/Git에 넣지 않도록 대시보드에서 별도로 입력하게 한다. 실제 서버 계정이 준비·승인된 뒤 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 Render 대시보드에 설정한다. Supabase CA는 `prod-ca-2021.crt` 이름의 Secret File로 업로드하고, `DB_URL`의 JDBC URL에는 `sslmode=verify-full`, `sslrootcert=/etc/secrets/prod-ca-2021.crt`, `prepareThreshold=0`, `connectTimeout=10`, `socketTimeout=30`을 사용한다. 비밀번호나 인증서 본문을 저장소에 커밋하지 않는다. [Render 비밀 변수와 파일](https://render.com/docs/configure-environment-variables)

사진 API는 `PHOTO_UPLOADS_ENABLED=false`, `PHOTO_STORAGE_WIRE_VERIFIED=false`, `PHOTO_CLEANUP_ENABLED=false`로 유지한다. #13의 공유 DB Migration·서버 권한·실제 Storage lifecycle·배포 요청 한도를 검증한 뒤에만 활성화한다. 게시물 사진 규격은 JPG/PNG, 최대 10장, 합계 10,000,000 bytes다. FE가 별도 API 호스트를 직접 호출하도록 연결하며, 실제 Render 배포 후 10 MB 경계 업로드를 검증해야 한다. Vercel Function을 통해 사진 요청을 중계하면 4.5 MB 요청 제한이 다시 적용된다. [Vercel 요청 제한](https://vercel.com/docs/functions/limitations#request-body-size)

이 문서는 Render 서비스 생성, 도메인 연결, 비밀값 등록, 배포 또는 FE 환경 변수 변경을 수행했다는 뜻이 아니다. 실제 API 주소/도메인, CORS 허용 origin, FE의 API base URL 환경 변수 이름은 FE 담당자와 확인한 뒤 연결한다. 첫 배포 후 `/health`, 실제 Supabase 서버 계정의 TLS/권한, 10 MB 업로드와 삭제 작업을 검증한다. Docker 이미지는 로컬 Docker daemon을 사용할 수 있을 때 별도 검증한다.

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

워크플로 파일은 PR 통합/원격 반영 후 GitHub에서 실행된다. CD는 #30에서 Vercel FE와 별도 Render API의 실제 대상·배포 방식·리전·환경별 비밀 변수·DB 연결과 health/실패 복구를 확인한 뒤 연결한다. 현재 Render 자동 배포는 꺼져 있고 CI JAR 생성은 배포 성공을 뜻하지 않는다.

## #13 사진 처리 구현 준비 (2026-10-08)

`com.discushion.photos`에 예약 POST, 완료 POST, 본인 상태 GET, 취소 DELETE를 구현했다. 가입 완료는 실제 identity adapter로 매 요청 재검증하며 users→media 순서로 잠근다. 예약/key·잠재 권한 만료 상한은 외부 발급 전에 commit하고, URL/token을 DB에 저장하지 않는다. 실제 JPG/PNG 내용을 제한된 크기로 읽어 검사하고 최초 uploadedAt·24시간 만료를 반복 완료로 연장하지 않는다.

`PhotoAttachments.replace`는 #14/#16의 같은 쓰기 transaction에서 호출하는 연결 도우미다. users→posts→polls→media_files ID 오름차순으로 잠그고 최종 참조·10장/10,000,000 bytes·소유권·지역·종료 투표를 확인한다. null은 유지, []는 전부 제거이며 기존 photoId를 보존해 재정렬한다. 제거된 본인 fileId 목록을 반환해 향후 PATCH meta.photoDeletion에 사용한다. rollback 전에 Storage를 삭제하지 않는다. 게시물 API와 상세 DTO 조립은 #14~16의 후속이며 아직 제공하지 않는다.

`PhotoCleanup`은 UPLOADING createdAt+24h / UNLINKED uploadedAt+24h 후보와 삭제 재시도를 처리한다. 실제 참조와 파일 잠금을 재확인하고 token/lease로 stale worker 결과를 차단한다. 2분 lease, 기본 60초 간격/20개 batch, 60초부터 최대 1시간의 재시도 지연을 사용한다. 외부 Storage 호출은 transaction 밖에서 수행한다. lease가 만료되면 결과를 기록하지 않고 다음 작업자가 회수한다.

### 활성화와 검증 경계

- 기본 PHOTO_UPLOADS_ENABLED=false이므로 현재 local Health 서버에 사진 API/worker를 노출하지 않는다.
- 실제 DB 프로필과 #4 Privy 공개키·가입 회원 기반이 필요하다. 테스트용 인증/Storage는 src/test에만 있으며 운영 빈에 등록하지 않는다.
- 새 사진은 아래 2026-10-08 서버 중계 계약을 따른다. `PHOTO_VERIFIED_ISSUANCE_ALLOWANCE_SECONDS`는 새 예약에 사용하지 않는다. 새 Migration·공유 DB 권한·배포 요청 한도와 실제 연결을 검증하기 전에는 PHOTO_STORAGE_WIRE_VERIFIED와 PHOTO_UPLOADS_ENABLED를 활성화하지 않는다. client timeout을 provider 쓰기 종료의 증거로 사용하지 않는다.
- 실제 provider의 진행 중 업로드 종료/재생성 방어는 아직 입증되지 않았다. 현재 Supabase adapter의 uploadsDrained는 항상 false이며, 단순 권한 만료·DELETE 성공·부재 조회만으로 DELETED를 기록하지 않는다. 파일을 제거해도 DELETE_PENDING과 quota 슬롯을 유지한다. #30에서 실제 증거를 갖춘 종료 확인 구현이 필요하며 시연 중 예약 누적을 관측해야 한다.
- PHOTO_CLEANUP_ENABLED=true는 위 환경을 검증한 뒤 선택한다. batch 결과와 정제된 실패 분류를 로그로 보고, media_files의 deletion_attempts/next_delete_attempt_at/last_delete_error_code/claim을 확인한다. signed URL·secret·raw provider 오류를 로그에 남기지 않는다.
- 초기 구현 시 Supabase 플러그인의 조회 가능한 프로젝트는0개였고 원격 DB/Storage/계정/버킷을 변경하지 않았다. 이후 실제 Storage 준비 결과는 아래 02:20 KST 기록을 따른다. 로컬 합성 Storage의 성공은 실제 원격 연결이나 FE 사용자 흐름 검증이 아니다. #13 완료와 Issue 종료는 대기다.

로컬 시험은 DISCUSHION_TEST_JDBC_URL=jdbc:postgresql://127.0.0.1:55432/discushion_migration_test 및 저장소 밖의 DISCUSHION_TEST_DB_PASSWORD를 사용해 `gradlew.bat --no-daemon test build --rerun-tasks --console=plain`로 실행한다. 임시 PostgreSQL 17.11의 새 시험 DB에 기존 Migration5개를 적용하며 실제 DB 잠금/권한/rollback/정리와 HTTP 호출을 검증한다. 원격 Supabase 시험은 명시적으로 제외한다. 2026-10-08 01:30 KST 최종 test/build 새 실행 성공: JUnit70개 중67통과/원격Supabase3제외/실패0/오류0. 신규25개는 이미지4·실제JDBC14·실제HTTP4·Supabase HTTP 요청 대체 검증3이며 실제 Storage 시험은 아니다. 기존 Schema 무결성128개 통과, 운영 JAR의 test fixture0개, 시험 후 사진/합성 회원 잔여0개를 확인했다.

2026-10-08 01:49 KST 기준 갱신 후 재검증: #13 미커밋·미추적 파일을 stash로 보존하고 `back/develop`을 `git pull --ff-only origin back/develop`으로 PR #121 병합 기준 `91fa30d`까지 갱신했다. `back/feature/13-photos`를 같은 기준으로 fast-forward한 뒤 변경을 복원했다. 충돌0, README의 지역/사진 문단 모두 유지, 사진 코드·기존 변경 내용은 Git 줄바꿈 정규화 후 동일하다. 위 test/build를 실제 localhost DB로 새로 실행해 지역 조회24개를 포함한 **94개 중91통과/원격Supabase3제외/실패0/오류0, build 성공**을 확인했다. 원격 Storage·Privy·FE 연결은 이번 검증 범위가 아니며 활성화/완료 대기 조건을 유지한다. 백업 stash는 보존했고 commit/push/PR은 수행하지 않았다.

### 실제 Supabase Storage 준비·직접 전송 검증 (2026-10-08 02:20 KST)

사용자의 연결 요청과 로컬 키 저장 후 지정 프로젝트 `pmhmgqpyvrbbseqelpze`에서 Storage REST API로 `discushion-post-photos` 공개 버킷을 생성했다. 파일당 10,000,000 bytes, image/jpeg·image/png만 허용한다. 게시물 전체10장/합계10,000,000 bytes 검증은 별도 서버 로직이며 버킷 설정만으로 보장하지 않는다. 기존 Schema/Migration·DB 역할/RLS·타 버킷/파일은 변경하지 않았다. ignored backend/.env에 실제 URL·버킷을 준비했고 비밀 키·서명 URL은 출력/문서/commit에 포함하지 않았다.

- 작은 합성 PNG의 서버 서명 발급 → 인증 헤더 없는 PUT/RAW200 → 인증 없는 공개 GET200 및 원본 bytes 일치 → 서버 DELETE200 → authenticated GET 부재400을 실제 서비스에서 확인했다. 동일 key의 중복 PUT은400이다.
- text/plain과10,000,001 bytes 파일은 각각400으로 거부됐다. localhost:3000 Origin의 PUT preflight는200, allow-origin=* 및 content-type/x-upsert 허용을 확인했다. 이는 직접 REST 검증이며 실제 FE 브라우저 또는 Spring PhotoService/인증/DB와의 전체 연동 시험은 아니다.
- **유효한 서명 URL로 삭제된 key를 다시 PUT하면200으로 파일이 재생성됐다.** 기존 uploadsDrained=false와 DELETE_PENDING 보존을 유지한다. TTL 만료만으로 진행 중 전송 종료를 입증하거나 PHOTO_STORAGE_WIRE_VERIFIED/PHOTO_UPLOADS_ENABLED/worker를 활성화하지 않는다. 발급 지연·시각 오차 상한, 진행 중 전송 종료/재생성 방어, 실제 서버 DB 계정/RLS·Privy·FE 흐름은 #13/#30에서 남은 조건이다.
- connection-check/ 아래 이번 시험 파일은 재생성 시험 후에도 Storage API로 정리했고 실제 storage.objects 잔여0개를 확인했다. 보안 advisor WARN/ERROR0, 기존 private Schema27개의 RLS 정책 없음 INFO는 #30 서버 역할 작업으로 유지한다. build/Java 테스트 결과는 앞선01:49 실행이며 이번에는 REST 검증과 문서만 갱신했다. commit/push/PR은 수행하지 않았다.


### PR #125 리뷰 보완·최신 기준 재검증 (2026-10-08 04:10 KST)

BE1의 재현 문제2건을 보완했다. PhotoAttachments는 기존 잠금 순서(users→posts→polls→media)를 유지하고 모든 파일 잠금/검사 이후 첫 DB 변경 직전에 서버 Clock으로 투표 종료를 다시 검사한다. 실제 JDBC 파일 잠금 대기 중 Clock을 ends_at으로 이동하는 시험에서 변경이 거부되고 rollback됐다.

SupabasePhotoStorage는 헤더부터 응답 본문 전체에30초 deadline을 적용하며 초과/중단/인터럽트 시 구독과 요청을 취소한다. 성공 object는 수신 중10,000,000 bytes 한도와 기존 PHOTO_SIZE_EXCEEDED, metadata/오류 응답은65,536 bytes 한도와 PHOTO_STORAGE_UNAVAILABLE을 사용한다. 완료 object만 메모리 stream으로 전달하며 PhotoContent의 이미지 검사는 네트워크 수신에서 남은 같은30초 예산을 사용한다. 시간 초과 시 Future 취소·input close를 수행한다. 검사 worker는 최대2개·대기 큐 없음으로 제한해 decoder가 인터럽트를 따르지 않아도 task/thread를 무제한 생성하지 않으며 포화는 기술 실패다. 상품 사진 한도나 공개 API/DTO·Migration 변경은 없다.

최신 back/develop d93cbf5(PR #127)를 충돌 없이 반영한56b4cae에서 Java17 `gradlew.bat --no-daemon test build --rerun-tasks --console=plain`을 실제 localhost PostgreSQL로 새로 실행했다. **133개 중130통과/실패0/오류0/원격Supabase3skip, build 성공**. 사진33개 모두 통과: 기존25개에 종료 잠금1개·실제 HTTP5개·검사 deadline2개 추가. 기본30초 그대로인 중단 본문·오류/metadata 중단·크기 초과·정상 PNG·검사 취소/close를 확인했다. 최초 시험의 데이터 정리/Mockito 설정 오류2건은 수정·재검증했고 잔여 데이터도 정리했다. 시험 사진 회원 잔여0개·운영 JAR 테스트 지원 클래스0개·diff 공백 오류0·이번에 시작한 DB 정상 종료.

실제 원격 Storage/Privy/FE 전체 연결을 이번 시험으로 완료 처리하지 않는다. 업로드 종료 증거·최소 권한 서버 계정·실제 연결 후속과 PHOTO_* 비활성/Draft/#13 미완료 조건은 유지한다. 수정된 최종 PR은 BE1 재리뷰·승인 대상이며 작성자 검증을 BE1 승인으로 사용하지 않는다. CI와 실제 승인 상태는 PR에서 확인한다.

## #30 서버 DB 실행 계정 준비 (2026-10-08)

`discushion_server` NOLOGIN 역할과 현재 가입·권한·지역·사진 코드용 11개 테이블/26개 RLS 정책을 후속 Migration으로 준비했다. 비밀번호는 Migration에 없고 실제 LOGIN은 BE1 승인·병합/공용 DB 적용 후 별도로 설정한다. 현재 `.env`와 원격 계정을 변경하지 않았으며 #30 전체 완료는 아니다. 권한표·승인 후 적용/교체 순서는 [DB 연결 결정 기록](../docs/collaboration/backend-db-connection-decisions.md)의 마지막 절을 따른다.

`ServerRuntimePermissionsIntegrationTests`는 격리 localhost DB에서만 실제 비밀번호 LOGIN, 전체 27개 테이블 작업 권한, 최초 가입/재요청, 사진 예약/연결/참조 제거, DDL/TRUNCATE/불필요한 물리 삭제/legacy·자격 쓰기/권한 상승 거부를 시험한다. provider는 테스트용 대체 구현이며 실제 Privy/Storage/FE 연결 시험이 아니다. 테스트 끝에 LOGIN/비밀번호를 제거한다. CI는 새 Migration을 포함한 격리 DB를 준비하고 이 시험을 실행하며 원격 secret은 사용하지 않는다.

기존 Supabase 원격 smoke 3개는 SELECT-only 관리자 감사다. 실제 런타임을 서버 계정으로 교체한 뒤에도 감사 테스트에는 로컬 secret의 `DB_AUDIT_USERNAME`/`DB_AUDIT_PASSWORD`를 사용할 수 있다. 이 변수는 테스트 전용이며 서버 권한을 넓히기 위한 용도가 아니다. 미설정 시 기존 DB_USERNAME/DB_PASSWORD를 사용하고, 기존 관리자 계정 기대값은 유지한다. 실제 원격 권한 Migration 적용 전후의 카탈로그/이력 차이를 확인하고 기대값을 갱신한다.

## #13 실제 Storage 검증 후속 (2026-10-08)

PR #125의 사진 구현은 이미 병합돼 있으므로 재구현하지 않는다. `PhotoStorageLiveIntegrationTests`는 명시적으로 선택할 때만 지정 개발 프로젝트의 실제 Storage와 운영 사진 HTTP/JWT 검증·회원/사진 JDBC adapter를 연결한다. 로컬 회원·서명키와 `discushion_server` 비밀번호 LOGIN은 격리 테스트 DB에서만 준비한다. 실제 Privy OTP, 공유 DB의 회원 등록/사진 변경 또는 FE 브라우저 연동 시험이 아니다.

실행에는 기존 localhost PostgreSQL의 최신 Migration·서버 RLS, `DISCUSHION_TEST_JDBC_URL`/`DISCUSHION_TEST_DB_PASSWORD`, ignored `.env`의 Supabase URL·버킷·관리 키와 `DISCUSHION_VERIFY_PHOTO_STORAGE=true`가 필요하다. 지정 프로젝트/버킷과 localhost DB를 검사한 뒤 합성 PNG의 예약·PUT/RAW·익명 공개 조회·완료/상태 조회·타 회원 거부·취소·삭제 후 동일 URL 재업로드·재삭제를 검증한다. 시계를 이동해 UPLOADING의 예약+24시간과 UNLINKED의 최초 완료+24시간 경계를 확인하며 반복 완료로 기한이 연장되지 않아야 한다. 이는 실제로 24시간을 기다리거나 provider의 URL을 만료시키는 시험이 아니다.

원격 전체 정리 배치를 호출하지 않고 이번 시험의 fileId만 처리한다. 종료 시 이번 예약의 Storage object 부재를 확인한 뒤 로컬 DB fixture를 제거하고 테스트 LOGIN/비밀번호를 해제한다. 원격 삭제가 실패하면 추적 행을 보존하고 테스트를 실패시킨다. 비밀 키와 서명 URL은 출력/문서/Git에 남기지 않는다. CI에는 외부 secret을 추가하지 않고 이 opt-in 시험을 제외한다.

테스트의 1분 발급 여유는 테스트 입력이며 운영의 발급 지연·시각 오차 상한으로 확정하지 않는다. `uploadsDrained=false`와 `DELETE_PENDING`/`deletionCompleted=false`를 유지한다. Storage DELETE 성공이나 테스트 시계의 24시간 경과는 진행 중 전송 종료의 증거가 아니다. provider 종료 보장 또는 재생성 방어 계약을 확인하기 전에는 실제 `PHOTO_UPLOADS_ENABLED`/`PHOTO_STORAGE_WIRE_VERIFIED`/`PHOTO_CLEANUP_ENABLED`를 활성화하거나 #13 전체 완료로 표시하지 않는다.

검증 기록(2026-10-08 15:00 KST): 기준 back/develop `3c4b6aa`와 이 후속 테스트의 미커밋 변경에서 Java17 `gradlew.bat --no-daemon test build --console=plain --rerun-tasks --max-workers=2 --offline`을 실행했다. **226개 중223통과/실패0/오류0/관리자 감사3skip, build 성공**. 실제 Storage 시험2개 모두 통과했다. 예약→PUT/RAW→익명 공개 GET/bytes 일치→완료/본인 조회→취소202→실제 object 삭제→동일 유효 URL로 재생성→재삭제와 DELETE_PENDING 보존을 확인했다. 24시간 정리 경계·최초 완료시각 보존도 실제 object와 로컬 DB/HTTP로 검증했다. 두 시험이 만든 합성 object3개는 종료 시 부재를 확인했고 로컬 사진 회원/파일 잔여0개·테스트 역할 NOLOGIN/비밀번호 제거·운영 JAR 테스트 클래스0개를 확인한 뒤 이번에 시작한 로컬 DB를 종료했다.

최초 기준 검증의 로컬 포트 오류와 오래된 테스트 DB의 institutions RLS 누락은 격리 환경에서 바로잡고 재검증했다. 새 테스트의 잘못된 익명 요청 입력과 PostgreSQL보다 세밀한 Clock 정밀도도 수정했다. 공용 DB Schema/권한이나 제품 코드는 변경하지 않았다. 현재 ignored `.env`의 실제 서버 계정으로 Supabase TLS verify-full 연결·사진 SELECT/INSERT/UPDATE 허용·물리 DELETE 거부·사진 RLS3개를 별도 읽기 전용으로 확인했다.

관리자 감사3개는 이번 최초 실행에서 실제로 실행했지만 서버 계정이 기존 관리자 감사 username 기대값과 달라 실패했다. `DB_AUDIT_USERNAME`/`DB_AUDIT_PASSWORD`가 없으므로 최종 전체 실행에서는 opt-in을 끄고3skip으로 기록했다. 실제 서버 계정 검증을 이 감사3개 통과로 대체하지 않는다. 아래 후속 실행에서 감사 자격 증명 준비 및 재검증 결과를 기록한다. 사진 Storage 검증 통과와 별도로 운영 발급 상한/전송 종료·재생성 방어, 실제 Privy OTP·FE 연결, #14~16 게시물 연결과 #13 전체 완료 조건은 남아 있다.

### 발급 응답 안전성 보완과 관리자 감사 재실행 (2026-10-08)

로컬 `.env`에 감사 변수가 준비된 것을 확인했다. 첫 재실행은 감사 username 오입력으로 DB 접속 전 실패했으며, 지정 프로젝트 관리자 계정명으로 수정한 뒤 15:52 KST `SupabaseJdbcSmokeTests` 3개가 실패0/오류0/skip0으로 통과했다. TLS 인증서/hostname 검증, 실제 Schema·Migration 내용·서버 RLS 및 공개 API 역할의 private Schema 접근 거부를 SELECT-only로 확인했다. 비밀번호 재설정이나 원격 DB 역할/권한 변경은 수행하지 않았다.

`PhotoService.reserve`는 관측한 실제 만료시각을 먼저 영속 기록하고, 외부 발급 전 기록한 상한을 넘는 응답 또는 만료된/잘못된 전송 응답을 FE에 전달하지 않는다. 아직 UPLOADING이면 즉시 DELETE_PENDING으로 예약해 24시간 동안 방치하지 않는다. 기존 성공/오류 DTO·PUT/RAW·DB Schema는 유지한다. 상한 초과와 만료 응답의 회귀 테스트2개를 추가했다. 알려진 만료시각을 보존하는 보완이며, 응답 유실 시 provider 발급 종료시각이 입증됐다는 의미는 아니다.

[공식 signed upload URL 안내](https://supabase.com/docs/reference/javascript/storage-from-createsigneduploadurl)는 2시간 유효성을 명시한다. 확인한 공식 문서에서는 개별 URL 취소·발급 처리 지연의 최대치·진행 중 전송 종료 장벽을 입증할 수 없었다. HTTP timeout과 테스트용 여유값을 이 보장으로 대체하지 않는다. 현재 adapter의 uploadsDrained=false, DELETE_PENDING 추적과 운영 비활성 플래그를 유지하며, #13 최종 삭제 및 운영 발급 상한 조건은 미완료다. 공급자 보장 확인 또는 직접 업로드/삭제 계약 변경안의 BE1·FE 공동 확인이 필요하다. 검증 없이 상한을 임의 확정하거나 전송 방식을 변경하지 않는다.

2026-10-08 16:01 KST 최종 검증: 최신 back/develop `04d60fa`를 현재 Feature에 fast-forward하고 위 보완 및 기존 미커밋 후속 테스트를 포함해 Java17 test/build를 새로 실행했다. **245개 모두 통과/실패0/오류0/skip0, build 성공(7분21초)**. `DISCUSHION_VERIFY_SUPABASE=true`와 `DISCUSHION_VERIFY_PHOTO_STORAGE=true`를 함께 설정해 관리자 감사3개와 실제 Storage2개를 제외하지 않았다. 사진 JDBC17개에 신규 회귀2개를 포함한다. 시험용 Storage object는 테스트 종료 시 삭제·부재 확인했고 운영 JAR의 테스트 클래스0개·임시 DB 서버 역할 NOLOGIN/비밀번호 제거를 확인했다. 실제 Privy OTP/FE 흐름, 운영 발급 지연 상한 및 진행 중 업로드 종료 증명은 이번 성공에 포함되지 않는다. git diff --check 통과, 기존 문서 이름 유지, commit/push/PR은 아직 수행하지 않았다.

### #13 합의된 서버 중계 구현 (2026-10-08)

사용자가 팀 합의를 확인해 새 예약의 전송을 FE → Spring → Supabase로 변경했다. 기존 직접 전송 관련 기록은 과거 검증이다. 현재 계약은 API 정본 §13.9다. 새 PUT `/api/v1/photo-uploads/{fileId}/content`는 앱 API의 Privy Bearer·가입 완료·소유권을 검사하며 RAW bytes를 받고, DB 시간 기준2시간 안에 단 한 번 외부 쓰기를 시작한다. 새 adapter는 signed URL 발급을 거부하고 공급자 발급 여유 설정을 사용하지 않는다. HTTP 수신/검사30초·Storage 응답30초·동시2건으로 제한한다.

`20261008071616_track_server_photo_uploads.sql`의5컬럼·제약·trigger를 **로컬 테스트 DB에만** 적용했다. 공유 Supabase의27테이블/180컬럼·기존 Migration은 유지하며 새 배포 코드 활성화 전에 통합된 Migration 적용과 최소 권한/RLS 검증이 필요하다. 운영 PHOTO_* 플래그와 .env의 활성화 상태는 변경하지 않는다.

RUNNING은 외부 호출 전에 commit하고 성공 종료는 ACKNOWLEDGED로 저장한다. UNKNOWN 또는 종료 commit 유실은 영속 보존해 최종 삭제를 막는다. 미전송/정상 종료 파일은 삭제와 부재 확인 후 DELETED가 가능하며, 기존 직접 업로드 행은 DIRECT_UNCONFIRMED로 유지한다. 외부 쓰기 결과 불명확 파일을 시간 경과로 강제 종료하지 않는다.

새 PhotoRelayJdbcIntegrationTests는 경합/취소·응답 유실·장애 추적을 격리 DB에서 검증한다. PhotoRelayLiveIntegrationTests는 실제 Supabase와 로컬 Spring/합성 인증·서버 LOGIN으로 정상 파일과10,000,000 bytes 파일을 검증한다. 기존 PhotoStorageLiveIntegrationTests는 직접 업로드의 재생성 위험을 검증하는 회귀 시험이다. 실제 Privy OTP/FE 화면·배포 연동과 구분한다. 최종 실행 결과는 아래 검증 기록을 따른다.

Storage 성공 종료 판정은 [공식 Storage POST 구현](https://github.com/supabase/storage/blob/master/src/http/routes/object/createObject.ts)이 uploadFromRequest 완료 후 Key를 반환하는 흐름과 실제 서비스 응답을 대조했다. 2xx만으로 판정하지 않고 기대한 bucket/key가 일치하는 전체 응답을 확인한다. 이 근거는 실제 서비스 내부의 모든 장애/비동기 복구 경로가 유한 시간에 종료된다는 보장으로 확장하지 않는다.

2026-10-08 16:48 KST 최종 검증: 기준 back/develop `04d60fa`와 현재 `back/feature/13-storage-verification`의 미커밋 서버 중계 구현으로 Java17 test/build를 새로 실행했다. **전체256개 통과·실패0·오류0·skip0·build 성공(7분6초)**. 관리자 원격 감사3개, 기존 직접 Storage 회귀2개, 새 서버 중계 실제 Storage2개와 JDBC 안전성9개를 포함한다. 새 실제 시험은 합성 인증·격리된 최소 권한 서버 LOGIN으로 Spring PUT→실제 Storage 쓰기/익명 공개 조회→complete→취소→최종 DELETED→늦은 PUT 거부를 확인했으며 정확히10,000,000 bytes PNG도 같은 흐름을 통과했다. JPG 내용 검사 등 기존 검증은 유지한다. 실제 Privy OTP·FE 화면·배포 환경의10MB 수용 시험을 대신하지 않는다.

로컬 PostgreSQL 제약133개, 공개 역할 차단9개, 전체7 Migration의 적용/재실행 불변성, ERD27테이블/185컬럼 일치를 검증했다. 시험 Storage object 삭제·부재, 로컬 사진 회원/파일0개, 임시 서버 역할 NOLOGIN/비밀번호 제거, 운영 JAR의 테스트 fixture0개를 확인했다. 문서 상대 링크와 diff 공백 검사를 통과했다. 공유 Supabase에는 새 Migration을 적용하지 않았으며 PHOTO_* 활성화·FE 연결·GitHub 상대 승인·commit/push/PR은 별도다. 결과 불명확 전송과 기존 직접 전송 파일은 삭제 대기를 유지하고 시간 경과로 최종 삭제하지 않는다.

## #13 공유 DB 적용·Render 사진 설정 준비 (2026-10-08)

기준 back/develop dd5cb52의 `20261008071616_track_server_photo_uploads.sql`을 지정 공유 개발 DB에 실제 적용했다. 앞선 로컬 적용/공유 DB 미적용 기록은 당시 결과다. 원격 버전은 Supabase가 생성한20261008111216이며 로컬 파일명을 유지하고 SQL 내용까지 대조한다. 현재 원격27테이블/185컬럼/FK55/RLS27·Migration7개, 전송 추적5컬럼·신규 제약4개·guard trigger/함수 최소 권한 및 공개 역할 접근 차단을 확인했다. 상세와 참여 권한 Migration4개의 별도 rollout 대기는 DB 연결 결정 기록의 새 절을 따른다.

사용자 승인 후 Render discushion-api에 SUPABASE_URL·SUPABASE_STORAGE_BUCKET·SUPABASE_SECRET_KEY를 Save only로 저장하고 값이 가려진 저장 화면에서 확인했다. 키는 백엔드 서비스에만 등록했고 Git·FE에 전달하지 않았다. 저장만 수행했으므로 기존 Live 배포4fffb1e에 새 설정이 적용됐다고 표시하지 않는다. PHOTO_UPLOADS_ENABLED·PHOTO_STORAGE_WIRE_VERIFIED·PHOTO_CLEANUP_ENABLED는 false를 유지하며 자동 배포도 켜지 않았다.

사용자는 Privy 앱이 아직 없다고 확인했다. 다음 연결에는 FE와 같은 Privy 앱의 실제 App ID/서버 App Secret, 이메일 OTP·허용 origin 설정, 실제 로그인 및 공유 DB의 가입 완료 회원이 필요하다. 비밀 값은 ignored .env와 승인된 배포 secret에서 관리한다. 테스트용 인증을 Render에 등록하지 않는다. 실제 배포의 10,000,000 bytes 업로드·익명 조회·완료·삭제·늦은 PUT 거부·24시간 후보 정리 및 휴면 후 재개, FE/BE 사용자 흐름은 대기이며 #13 완료로 기록하지 않는다.

20:19 KST 실제 관리자 원격 감사3개/실패0/오류0/skip0·build 성공(1분6초). TLS/hostname, 정확한 적용 이력·SQL digest/사진 제약/trigger/함수 권한, private Schema 공개 역할 차단을 SELECT-only로 확인했다. 제품 사진 코드·적용된 Migration은 수정하지 않았다.

2026-10-08 20:22 KST 최종 후속 검증: 위 원격 감사3개, PhotoRelayLiveIntegrationTests2개, 기존 PhotoStorageLiveIntegrationTests2개, PhotoRelayJdbcIntegrationTests9개를 실제 실행해 **16개 통과/실패0/오류0/skip0·build 성공(1분22초)**을 확인했다. 실제 Storage와 로컬 Spring/합성 JWT/격리된 최소 권한 LOGIN을 연결해 정상 사진 및 정확히10,000,000 bytes PNG의 전송·익명 공개 조회·완료·취소·DELETED·늦은 PUT 거부를 재검증했다. 기존 직접 전송의 삭제 후 재생성/재삭제·24시간 정리 경계 및 RUNNING/UNKNOWN 추적·경합은 각각 기존 실제 Storage/JDBC 시험으로 확인했다. 실제24시간 대기·Privy OTP·Render 요청 수용/worker·FE 사용자 흐름 검증은 아니다.

종료 후 지정 버킷 object0개·공유 DB 사진/참조0개, 로컬 사진 회원/파일0개·시험 서버 역할 NOLOGIN/password null을 확인하고 이번에 시작한 로컬 DB를 정상 종료했다. 실제 제품 코드와 Migration 파일은 유지했다. 이전 전체307개 통과 기준41ef5bb와 현재 기준dd5cb52의 Git tree가 동일(ceabef9c6f10b4d2aef8b129425bbc1ee2eed65f)함을 확인했으며, 이번 변경에 맞는16개만 재실행했다. 전체307개를 이번에 재실행한 것으로 기록하지 않는다. 변경 문서4개/감사 테스트1개는 현재 Feature에서 미커밋이며 commit/push/PR/병합은 수행하지 않았다.

### Privy 설정 소비 재배포와 실제 OTP 준비 (2026-10-08)

앞선 Privy 미준비/Save only 기록 이후 실제 앱을 만들고 사용자 승인된 서버 Secret을 ignored `.env`와 Render에 저장했다. 기존 Live `4fffb1e`를 [수동 재배포](https://dashboard.render.com/web/srv-db3mtql9fdbs73efdhng/deploys/dep-db3ohhk9v7es73dsrm7g)해 Live 전환·HTTPS Health200/UP·실제 월계1동 조회200을 확인했다. 최신 develop `26cb5e8`의 추가 기능/권한 Migration을 이번 배포에 포함하지 않았다.

공유 DB의 빈 지역 목록에 가입 검증용 `서울특별시 노원구 월계1동`(ID1)을 등록했고 확인되지 않은 공적 코드/지도 key는 NULL로 유지했다. 지역/기관 인증 자격은 부여하지 않았다. 실제 Privy SDK 화면은 프로젝트 밖의 임시 검증 도구이며 frontend 파일을 수정하지 않았다. 공식 Privy 로그인 모달 OTP 인증이 성공했고 실제 Render 회원 조회는 가입 전403이었다. 사용자 승인된 필수 동의값 true·마케팅 false로 가입201·재요청200·본인 프로필200을 확인했으며 DB의 가입 완료 Privy 회원1명과 동의3행을 대조했다.

Storage 버킷의 public=true·파일 상한10,000,000 bytes·JPEG/PNG 허용을 실제 조회했다. 사용자 승인 후 Render의 PHOTO_UPLOADS_ENABLED·PHOTO_STORAGE_WIRE_VERIFIED·PHOTO_CLEANUP_ENABLED를 true로 저장하고 같은 `4fffb1e`를 재배포했다. [사진 활성화 배포](https://dashboard.render.com/web/srv-db3mtql9fdbs73efdhng/deploys/dep-db3p1l49v7es73dufs2g)는21:47:16 KST에 시작해1분29초 후 Live다. 자동 배포는 꺼져 있으며 worker 주기는60000ms다.

실제 Privy 회원으로 예약201·정확히10,000,000 bytes RAW PUT200·complete200/UNLINKED·익명 공개200/원본 내용 일치·complete 재요청 시각 불변을 확인했다. 비로그인 PUT401, 10,000,001 bytes 예약413, DELETE202/DELETE_PENDING, 늦은 PUT409, worker 처리 후 GET200/DELETED/deletionCompleted와 공개 파일 부재400을 확인했다. 25시간 전 생성된 폐기용 미완료 예약도 실제 worker가1회 처리해 DELETED다. 이 시험은 새 합성 행을 사용했고 기존 행·guard/시간 제약을 수정하지 않았으며 실제24시간 대기 시험이 아니다.

Storage object0개·초기 최종 DELETED 검증 이력2행·시연 회원1명·활동 지역1건을 확인했다. 삭제 이력과 사용자 승인된 회원은 보존한다. 이 배포 검증 시점에는 제품 코드/Schema/Migration 변경이나 commit/push/PR/병합을 수행하지 않았다. 이후 문서·감사 테스트 변경은 별도 PR로 통합한다.

22:06:30 KST 실제15분 무요청 후 graceful shutdown을 확인했다. 휴면 중 생성한 별도25시간 전 만료 예약은 UPLOADING·정리 시도0회로 유지됐다. Health 요청으로 서버를 깨운 뒤76.29초에 UP 응답을 확인했고22:10:56에 새 worker가 후보1개를 DELETED·시도1회·오류 없음으로 처리했다. 수동 재배포/재시작으로 자연 휴면 검증을 대신하지 않았다. 최종 DELETED 이력3행·Storage object0개이며 이 결과로 휴면 후 재개 검증 대기를 해소한다. 실제24시간 대기와 프로젝트 FE SDK/adapter·배포 origin/CORS·화면 및 게시물 사진 연결은 이 시험과 구분한다. 무료 플랜의 정리는 휴면 동안 지연될 수 있다.

### 배포 검증 기록 PR의 최신 기준 재검증 (2026-10-08)

최신 `origin/back/develop 26cb5e8`을 Feature에 반영한 `1d6e6fa`에서 Java17 전체 `test build --rerun-tasks --max-workers=2 --offline`을 실행했다. 22:31 KST 종료, 소요8분19초, **328개 통과·실패0·오류0·skip0·build 성공**이다. 실제 Supabase SELECT-only 감사3개와 직접/서버 중계 Storage 시험4개를 모두 실행했다. 격리 PostgreSQL에는 최신 북마크/기관 조회 권한 Migration2개를 추가 적용했고 제약133개·공개 역할 차단9개를 확인했다. 공유 DB의 추가 기능 권한은 이 검증으로 적용하지 않았다.

종료 후 로컬 시험 회원/사진0개·임시 서버 역할 NOLOGIN/password null·운영 JAR의 테스트 클래스/fixture0개를 확인하고 시험 DB를 정상 종료했다. 기존6파일만 PR에 포함하고 frontend·제품 코드·적용된 Migration 파일은 변경하지 않는다. 실제 프로젝트 FE 및 게시물 사진 연결이 남아 있으므로 PR은 #13/#30을 참조하며 이슈를 자동 종료하지 않는다. 병합 후에도 Render 자동 배포는 꺼져 있고 현재 Live `4fffb1e`와 개발 브랜치 최신 코드를 구분한다.

### #18 지역 메인 구현·검증 (2026-10-08)

back/feature/18-home, 기준 a9ad5c9에서 사용자 확정 최신 공개6개/진행 투표3개 계약을 구현했다. 최신 프로필 기본 지역과 임시 지역을 구분하고 회원 상태·지역·공개 원본·활동 상태·사진 첫 참조·반응·댓글/답글·투표 옵션/득표를 읽기 snapshot으로 조회한다. 입력 오류/401/403/404와 Controller200·no-store를 검증했다. 새 Migration은 activity_post_details 서버 SELECT/RLS만 추가하며 공유 DB에는 미적용이다.

전체336개를 새로 실행해 권한 기대값1곳이 실패했다(335개 통과). activity SELECT 허용표를 보완한 후 home·서버 최소 권한·실제 Supabase 감사3개·Storage4개를 다시 실행해24개 통과/실패0/오류0/skip0·build 성공(23:32 KST)을 확인했다. 이후 검사 범위를 전체336개 재통과로 표시하지 않는다. Schema 제약133개와 공개 API 역할 차단9개도 통과했다. 현재 원격 back/develop d1f8bcf의 #14 생성 구현은 작업 도중 병합됐으며 commit/PR 전 최신 base를 반영해 권한 기대값/CI를 재확인해야 한다. 실제 프로젝트 FE·게시물 생성부터 탐색까지의 사용자 흐름 및 배포는 #30/#31 대기다. commit/push/PR/병합은 수행하지 않았다.
메인 추가 사진 회귀: 실제 로컬 서버 LOGIN에서 사진2개를 생성 ID와 다른 첨부 순서로 연결해 첫 사진·공개 URL의 공백 인코딩을 확인했다. home 전체 관련10개 재실행·실패0/오류0/skip0·build 성공(1분28초). 이 추가 검증은 앞선 원격 감사/Storage24개와 구분한다. 새 사진/참조 fixture는 정리했다.

### #19 지도 대표 조회 구현·검증 (2026-10-08)

back/feature/19-map, 기준 a9ad5c9. 사용자 채택 regionIds CSV·선택 centerRegionId로 실제 catalog를 읽고 동별 공개 안건/투표의 반응 합계·created_at·id 순서 대표를 조회한다. 입력 순서·빈 동 null·임시 중심 지역의 프로필 불변·재조회 대표 변경·삭제/활동/다른 지역 제외·회원 상태·잘못된 query를 검증했다. Batch 지역/대표 조회이며 임의 경계/좌표/GPS/캐시를 추가하지 않는다. endpoint는 no-store다.

23:41 KST 전체 Java17 test build --rerun-tasks --max-workers=2 --offline,336개 통과/실패0/오류0/skip0·build 성공(8분1초). 실제 Supabase SELECT-only 감사3개·Storage4개를 포함했다. 로컬 Schema 제약133개·공개 API 역할 차단9개 통과, 시험 데이터 정리·서버 역할 NOLOGIN 복원을 확인했다. 공유 DB에는 Migration을 적용하지 않았으며 실제 geometry/프로젝트 FE adapter 연결·배포는 #30/#31 대기다. front/develop ad0700c의 지도 인터페이스를 읽기 전용으로 확인하고 통합 지침에 후속 매핑을 기록했다. 현재 back/develop d1f8bcf의 #14는 작업 도중 병합됐으므로 commit/PR 전 최신 기준 갱신·권한/CI 재검증이 필요하다. 아직 commit/push/PR/병합하지 않았다.
지도 추가 사진 회귀(2026-10-09): 사진2개를 생성 ID와 다른 첨부 순서로 연결해 첫 사진·공개 URL 공백 인코딩을 확인했다. map 관련9개 재실행·실패0/오류0/skip0·build 성공(1분16초). 이 추가 시험은 앞선 전체336개와 구분하며 데이터/사진 참조를 정리했다.

### #20 원문 버전별 AI 요약 구현·검증 (2026-10-08)

back/feature/20-summary, 기준 a9ad5c9. 사용자 채택 최초 요청 생성·원문 revision별 DB 저장/재사용·수정 후 새 버전 생성·실패 자동 재시도 없음·정보 부족 SOURCE_TOO_SHORT를 구현했다. 회원 또는 검증된 공유 범위·공개 LOCAL_AGENDA를 생성 전/저장·응답 전에 재검사하며 DB 잠금을 외부 호출 중 유지하지 않는다. PENDING/요청시각/revision 조건으로 중복/늦은 결과를 차단하고 expired PENDING은 FAILED로 처리한다. 키 미설정/비활성은 영구 캐시 실패를 남기지 않는 원문 fallback이다. 서버 S/I/U·RLS Migration만 새로 추가했으며 공유 DB에는 미적용이다.

23:50 KST Java17 전체 test build --rerun-tasks --max-workers=2 --offline,345개 통과/실패0/오류0/skip0·build 성공(7분33초). 실제 Supabase SELECT-only 감사3개·Storage4개·Gemini2개를 포함했다. 격리된 Migration/Schema 제약133개·공개 역할 차단9개 및 AI 서버 S/I/U·DELETE 차단도 통과했다. 테스트 준비 순서를 명시한 후 AI DB/Controller9개와 실제 Gemini2개를 재실행해11개 통과·build 성공(1분21초)을 확인했다. 정상 합성 출력은 원문의 보행 공간 부족·표지 개선 의견·기관 전달 제안만 3문장 한 문단으로 담았고 정보 부족 원문은 SOURCE_TOO_SHORT다. 새 key는 ignored .env에서만 읽었으며 테스트 출력/문서/Git에 포함하지 않았다.

실제 원본 PostContextReader는 작업 도중 병합된 back/develop d1f8bcf의 #14 PostJdbcRepository에서 준비됐다. commit/PR 전 최신 base와 새 권한/테스트를 반영하고 운영 SharedPostAccess에 연결해 서명 게스트 경로를 검증한다. 다양한 실제 원문 품질/쿼터·배포 환경 key/활성화·FE PENDING 표시/HTTP adapter는 #20/#30/#31에서 확인한다. front/develop ad0700c는 읽기 전용으로 확인했고 변경하지 않았다. 아직 commit/push/PR/배포/병합하지 않았다.

### #14 게시물 생성과 서버 권한 (2026-10-08)

최신 `back/develop a9ad5c9`에서 `back/feature/14-createpost`를 준비했다. Java17 `gradlew.bat --no-daemon test build --max-workers=2`는 **341개 중334통과·실패0·오류0·7개 건너뜀, build 성공**. 생략 항목은 선택적인 실제 Supabase/Storage 감사이며 이 시험에서는 opt-in을 끄고 실행하지 않았다. 신규 #14 입력 단위·실제 HTTP/JDBC 통합 테스트 13개는 모두 통과했다.

격리 localhost PostgreSQL에서 전체 Migration 적용, Schema 제약 133개, 공개 API 역할의 접근 차단9개, ERD 대조(27테이블·185컬럼)를 확인했다. LOGIN으로 임시 활성화한 실제 `discushion_server` 역할에서 세 유형 생성·postId 응답·지역 자격 거부·사진 최대10,000,000 bytes 연결·초과/만료/타인 파일 rollback·동일 파일 동시 연결 경합·DDL/물리 삭제 거부를 HTTP로 확인했다. 테스트 이후 합성 회원·지역0건, 서버 역할 NOLOGIN을 확인했다. 이는 공유 Supabase 적용, 실제 Storage 전송 또는 프로젝트 FE 연결을 뜻하지 않는다.

### #15 공통 상세·실제 참여/댓글 원본 연결 (2026-10-08)

기준 `origin/back/develop d1f8bcf`에서 `back/feature/15-detail`을 준비했다. 사용자는 기존 상세 응답, 초기 댓글 LIKES/부모20개·전체 답글·동일 커서, 회원 본인 상태·게스트 생략, 작성자 전용 fileId와 #25 동일 득표율 반올림을 채택했다. 활동 문의 이메일의 회원·유효 공유 게스트 공개도 명시 승인했다. 기존 #29 미커밋 작업과 #16 Draft PR은 별도 작업 공간에 보존했다.

`GET /api/v1/posts/{postId}`가 실제 게시물·사진·활동/투표·기관 채택과 `JdbcParticipationSnapshotReader`/`JdbcPostSummaryReader`, 기존 #22 댓글 초기 조회를 연결한다. 조회는 posts→polls SHARE 잠금과 같은 snapshot을 사용하고 #16용 PostDetailLookup은 호출자의 쓰기 transaction에 참여한다. 프로필 사진은 #10 미완료 범위를 유지해 null이다. 활동 상세 SELECT 누락을 실제 서버 역할로 재현하고 추가 Migration `20261008144530_allow_activity_detail_reads.sql`로 SELECT/RLS만 보완했다. 기존 Migration은 수정하지 않는다.

Java17 `gradlew.bat --no-daemon test build --max-workers=2 --console=plain`은 2026-10-08 23:54 KST에 **352개 중345통과·실패0·오류0·선택형 원격7개 미실행, build 성공**으로 종료했다. #15 신규 실제 HTTP/JDBC11개는 통과했고 같은 DB 역할에서 세 유형 조회·게스트/타회원 개인정보 격리·사진 작성자 fileId·댓글 페이지/전체 답글·기관 배지/채택·종료/삭제 차단·원본 오류의 안전한500·SHARE/삭제 경합·기존 쓰기 transaction 결과/rollback을 확인했다. 기본 Schema 제약133개·공개 역할 차단9개, 추가 SELECT/RLS 검사, ERD27테이블/185컬럼/FK55 대조도 통과했다. 최종 SQL 정리와 북마크/기관 목록 실제 adapter 소비를 포함한 같은11개 대상 재실행도 실패/오류/skip0·build 성공이다. 종료 후 Schema 검사를 다시 통과했으며 합성 회원/지역0개, 서버 역할 NOLOGIN·정책49개·활동 수정/삭제 권한 없음·운영 JAR 테스트 fixture0개를 확인하고 로컬 시험 DB를 정상 종료했다.

FE 기준 `origin/front/develop ad0700c`의 post model/service와 대조했다. 화면용 string ID·시간 표시·활동 UPCOMING/ONGOING/CANCELLED·투표 ENDED는 Backend 안전 정수/절대시간·SCHEDULED/IN_PROGRESS/CANCELED·CLOSED에서 변환해야 한다. 사진은 photoId와 작성자 전용 fileId를 구분하고 본인 상태 생략을 회원의 미선택과 혼동하지 않는다. author model의 id는 현재 공개 DTO에 없으므로 임의 회원 ID를 만들지 않고 capabilities로 소유자 행동을 소비하도록 FE adapter에서 조정해야 한다. 이 문서 대조는 FE 담당자의 실제 확인이나 실제 FE/Privy 사용자 흐름 검증을 대신하지 않는다.

공유 Supabase의 신규 권한 Migration 적용·배포 및 프로젝트 FE 실제 연결은 #30/#31에서 해소한다. 원격/Storage7개 선택형 검사는 이번 localhost 시험에서 opt-in을 끄고 실행하지 않았으며 통과로 표시하지 않는다. #16의 실제 수정/삭제 API 재연결·권한/rollback 검증은 해당 Issue에서 진행한다. 구현과 로컬 검증은 commit/push/PR·병합 또는 공유 DB 적용을 뜻하지 않는다.

2026-10-09 최신 기준 반영: #14·#15의 통합 원본과 활동 SELECT 권한을 재사용한다. 아직 적용하지 않은 #18 중복 활동 SELECT Migration은 제외하고, #15 Migration은 보존했다. 게시물 생성 INSERT 권한과 SELECT-only 탐색 권한을 구분해 시험 기대값을 통합했다. 공유 DB 적용·배포·실제 FE 연결은 #30/#31에 남긴다.

### #18 PR 전 최신 기준 검증 (2026-10-09)
최신 back/develop 7e0d73f(#14·#15) 반영 코드83a4d51에서 전체362개 통과·실패0·오류0·skip0, build 성공(00:28 KST, 9분19초). 실제 Supabase 감사3개·Storage4개 포함. Schema 제약133개·공개 역할 차단9개 통과. 기존 #15 활동 SELECT Migration을 재사용하며 미적용 중복 #18 Migration은 제외했다. frontend 변경0·secret 유출0. 실제 공유 DB 권한 rollout·Render 배포·프로젝트 FE 연동은 #30/#31에 남아 있으므로 이 구현 PR으로 #18을 자동 종료하지 않는다.

### #16 실제 상세 연결·게시물 수정/삭제 검증 (2026-10-09)

기존 Draft PR #186의 Feature에 최신 back/develop `7e0d73f`(#14/#15)를 병합했다. 계약 검토표 충돌은 #14 입력 결정과 #16 삭제/보존 결정을 모두 보존해 해결했다. 공통 PostContextReader는 #14 실제 Bean을 유지하고 #16의 중복 등록을 제거했다. PATCH가 #15 실제 상세를 같은 쓰기 transaction에서 반환하도록 연결했으며 DELETE는 기존 북마크·사진 adapter와 연결한다.

누락된 활동 UPDATE를 추가 Migration `20261008150737_allow_activity_post_updates.sql`에서 이 권한과 서버 전용 UPDATE 정책으로만 보완한다. DELETE·DDL·시퀀스·공개 역할 권한은 추가하지 않고 기존 적용 Migration은 수정하지 않는다. #14 동일 URL/미래 종료시각 규칙을 재검증하고 존재하지 않는 지역 및 반올림될 수 있는 소수 ID를 거부한다.

2026-10-09 00:21 KST 전체 Java17 `test build --max-workers=2 --console=plain`은 **367개 중360통과·실패0·오류0·선택형 원격7개 미실행, build 성공**이다. 서버 역할 실제 HTTP8개와 서비스3개·입력4개를 포함했다. PATCH 최신 상세/생략 필드 보존·동시 부분 수정, 활동 수정/URL, 투표 수정 제한/종료, 작성자/지역 거부, 활성 채택의 지역 변경 차단, 기존 photoId 보존/사진 교체·실패 rollback, 삭제 DB 실패의 북마크/사진/게시물 rollback과 정상 삭제의 이력 보존, 삭제 예약·Storage 실패 재시도 상태를 확인했다. Storage는 시험용 mock 경계이며 실제 외부 버킷 삭제 완료로 기록하지 않는다.

최종 관련11개(HTTP8/서비스3) 재실행도 실패/오류/skip0·build 성공이다. 삭제 후 유효 공유 링크·댓글·반응·북마크·투표 API 차단 및 삭제 투표 내부 summary의 display 비노출을 포함했다. 최종 Schema133개·공개 역할 격리9개와 활동 UPDATE/RLS·ERD27테이블/185컬럼/FK55 검사를 통과했다. 합성 회원/지역0개·서버 역할 NOLOGIN·정책50개·실패 주입 함수0개·운영 JAR 테스트 클래스0개를 확인하고 localhost 시험 DB를 정상 종료했다.

실제 공유 Supabase의 #14/#15/#16 권한 적용·배포·프로젝트 FE/Privy 사용자 흐름은 #30/#31에서 해소한다. #29 코드는 별도 작업 공간에 보존하며 이번 PR에 포함하지 않는다. 최종 PR의 공통 API/DB/삭제/보존 영향은 필수 승인0명 규칙으로 작성자·Codex가 재검토하며 실제 연동과 로컬 시험을 구분한다.

### #29 기관별 채택·취소와 실제 게시물 연결 (2026-10-09)

보존했던 `back/feature/29-adoption` 변경을 최신 `origin/back/develop 72969f0`(#14→#15→#16) 위에 복원했으며 충돌은 없었다. 실제 회원 자격·PostContextReader JDBC adapter와 같은 transaction에서 회원→게시물→채택 관계 순서로 잠근다. 공개 지역 안건·현재 유효 기관·담당 지역을 확인하고 최초 채택201/현재 관계 재요청200, 본인 기관 취소·동일 취소 재요청204를 반환한다. 재채택은 새 관계이며 취소 담당자·시각과 이전 관계를 보존한다. 게시물 상태/내용을 변경하지 않고 공개 상세에는 기관명·채택시각만 제공한다.

추가 Migration `20261008120014_allow_institution_agenda_adoption_writes.sql`은 기존 SELECT에 INSERT/UPDATE와 서버 역할 전용 정책2개를 더한다. 물리 삭제·DDL·공개 역할 접근을 허용하지 않으며 기존 적용 Migration은 수정하지 않는다. 실행 역할 권한 회귀와 API 역할 격리 시험에 이 파일을 연결했다. 첫 HTTP 시험에서는 로컬 실행 DB에 이 Migration이 빠져 실패했으며 적용한 뒤 전체 시험을 다시 실행했다.

2026-10-09 00:45 KST Java17 `gradlew.bat --no-daemon test build --max-workers=2 --console=plain`은 **380개 중373통과·실패0·오류0·선택형 원격7개 미실행, build 성공**이다. 신규13개(서비스5/ID1/실제 HTTP·JDBC7)는 모두 통과했다. 실제 `discushion_server` 역할로 기관별 독립 관계·동일 기관 동시 채택201/200·동시 취소204·타 기관 취소 거부·만료/지역/유형/삭제 거부·클라이언트 기관 입력 거부·DB 실패 rollback·공개 개인정보 제한을 검증했다. #16 실제 지역 변경/삭제와 채택의 경합 및 #15 공개 상세/#28 ADOPTED 소비를 연결해 검증했으며 삭제는 채택 감사 관계를 보존하고 기관 취소 사건을 만들지 않는다.

최종 Schema133개·공개 역할 격리9개 및 채택 권한/RLS·ERD27테이블/185컬럼/FK55 검사를 통과했다. 서버 정책52개·시험 회원/지역0개·실패 주입 함수0개·서버 역할 NOLOGIN/password null을 확인하고 localhost DB를 종료했다. 실제 공유 Supabase 권한 적용·배포·FE/Privy 사용자 흐름은 #30/#31에서 해소하며 선택형 원격7개 미실행을 실제 원격 성공으로 기록하지 않는다. 최종 PR의 공통 API/DB 영향은 승인0명 규칙에 따라 작성자·Codex가 재검토하고 필수 CI와 최신 base 조건을 확인한다.

#16 추가 병합 후 재검증: 최신72969f0을 반영한 db28278에서 메인·게시물 생성/상세/수정삭제·서버 권한·실제 감사3/Storage4 관련64개 통과·실패0·오류0·skip0, build 성공(00:43 KST, 4분). Schema133/역할 차단9 통과. 활동 UPDATE는 #16에 필요한 권한이며 메인의 금지 시험은 물리 DELETE 거부로 조정했다. 앞선 전체362개와 이 후속64개를 구분한다.

### #19 PR 준비 검증 (2026-10-09)
#14·#15 기준6aa69a6에서 전체361개 통과·실패0·오류0·skip0·build 성공(00:39 KST, 9분3초). 실제 Supabase 감사3개·Storage4개 포함. Schema133개·공개 역할 차단9개 통과. 이후 병합된 #16은 같은 원본/권한과 기준 문서에 반영했고 영향받는 검증·최신 CI는 추가 확인한다. 공유 DB rollout·실제 지도/FE/배포는 후속이며 #19를 자동 종료하지 않는다.

#18/#16 최종 통합 후 재검증: 기준305a793을 반영한1613900에서 메인·지도·서버 권한·실제 감사3/Storage4 관련34개 통과·실패0·오류0·skip0·build 성공(00:52 KST, 1분58초). Schema133/공개 역할 차단9 통과. 앞선 전체361개와 구분하며 최종 원본/권한·빈 동·썸네일·profile 불변을 재검토했다.

### #20 최신 게시물 기준 검증 (2026-10-09)
#16 포함 기준72969f0을 반영한74a2bcb에서 summary·게시물·서버 권한·실제 감사3/Storage4/Gemini2 관련71개 통과·실패0·오류0·skip0·build 성공(00:49 KST, 4분24초). Schema133/공개 역할 차단9 통과. 이전 전체345개와 이 후속 검증을 구분한다. 합성 안건 정상3문장·정보 부족 처리의 실제 Gemini 호출 성공. 공유 DB summary 권한 적용·Render 비밀 설정/활성화·프로젝트 FE 연동은 대기다.

#18 메인 통합 후 재검증:70c3bf3에서 요약·메인·서버 권한·실제 감사3/Storage4/Gemini2 관련42개 통과·실패0·오류0·skip0·build 성공(00:59 KST, 2분56초). 이후 #19 통합 코드/계약을 보존해 반영하며 지도와 최종 서버 구성 영향은 별도 확인한다.

### #20 PR 최종 통합 검증 (2026-10-09)
최신 back/develop85cb888(#18·#19 및 BE1 #14~#16)을 반영한3cc1829에서 지도·서버 권한·실제 감사3/Storage4 관련24개 통과·실패0·오류0·skip0·build 성공(01:02 KST, 1분48초). Schema133/공개 역할 차단9 통과. 앞선 요약/게시물71개 및 메인/요약42개(실제 Gemini2개 포함)와 구분한다. 최종 code/API/DB/권한 검토에서 frontend 변경0·secret 유출0·차단 지적 없음. 실제 공유 DB 적용·Render 키/활성화·프로젝트 FE/공유 사용자 흐름은 #30/#31에 유지한다.

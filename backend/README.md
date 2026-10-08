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

## #13 사진 처리 구현 준비 (2026-10-08)

`com.discushion.photos`에 예약 POST, 완료 POST, 본인 상태 GET, 취소 DELETE를 구현했다. 가입 완료는 실제 identity adapter로 매 요청 재검증하며 users→media 순서로 잠근다. 예약/key·잠재 권한 만료 상한은 외부 발급 전에 commit하고, URL/token을 DB에 저장하지 않는다. 실제 JPG/PNG 내용을 제한된 크기로 읽어 검사하고 최초 uploadedAt·24시간 만료를 반복 완료로 연장하지 않는다.

`PhotoAttachments.replace`는 #14/#16의 같은 쓰기 transaction에서 호출하는 연결 도우미다. users→posts→polls→media_files ID 오름차순으로 잠그고 최종 참조·10장/10,000,000 bytes·소유권·지역·종료 투표를 확인한다. null은 유지, []는 전부 제거이며 기존 photoId를 보존해 재정렬한다. 제거된 본인 fileId 목록을 반환해 향후 PATCH meta.photoDeletion에 사용한다. rollback 전에 Storage를 삭제하지 않는다. 게시물 API와 상세 DTO 조립은 #14~16의 후속이며 아직 제공하지 않는다.

`PhotoCleanup`은 UPLOADING createdAt+24h / UNLINKED uploadedAt+24h 후보와 삭제 재시도를 처리한다. 실제 참조와 파일 잠금을 재확인하고 token/lease로 stale worker 결과를 차단한다. 2분 lease, 기본 60초 간격/20개 batch, 60초부터 최대 1시간의 재시도 지연을 사용한다. 외부 Storage 호출은 transaction 밖에서 수행한다. lease가 만료되면 결과를 기록하지 않고 다음 작업자가 회수한다.

### 활성화와 검증 경계

- 기본 PHOTO_UPLOADS_ENABLED=false이므로 현재 local Health 서버에 사진 API/worker를 노출하지 않는다.
- 실제 DB 프로필과 #4 Privy 공개키·가입 회원 기반이 필요하다. 테스트용 인증/Storage는 src/test에만 있으며 운영 빈에 등록하지 않는다.
- SupabasePhotoStorage는 공식 REST 요청 형식의 준비 코드다. 실제 public bucket·PUT/RAW·CORS·MIME/10MB·토큰 TTL/발급 지연/시각 오차 상한을 #13/#30에서 검증한 뒤에만 PHOTO_STORAGE_WIRE_VERIFIED=true 및 검증된 PHOTO_VERIFIED_ISSUANCE_ALLOWANCE_SECONDS를 설정한다. client timeout을 provider 발급 종료의 증거로 사용하지 않는다.
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

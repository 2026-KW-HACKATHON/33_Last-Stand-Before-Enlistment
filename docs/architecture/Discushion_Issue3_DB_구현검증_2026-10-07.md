# Issue #3 — DB Schema 구현·로컬 검증 결과

> 날짜/확인자: 2026-10-07 (Asia/Seoul) / Codex
> 기준 SHA: `aedc647952dadbe503fa2f2e91cbf52e300405f7` + 현재 미커밋 작업 트리
> 브랜치: `back/feature/3-schema`, PR base 예정: `back/develop`
> 최신 MVP 후속: `dad35c0`까지 pull했고 파일 충돌 없음. 아래 이전 결과는 v10.1 물리 구조 검증 이력이다. [사용자 승인된 v10.2 DB 반영](Discushion_Issue3_MVP_v10.2_반영.md)을 우선 확인한다. 원격 이력 4개, legacy 포함 27테이블·176컬럼, 로컬 SQL 113개·API 역할 모의 9개·Java 11개/build를 검증했다. BE2 검토·최소 권한 서버 계정과 실제 Privy/Storage/API/FE 기능 연동은 미완료다.
> 상태: 로컬 검증 및 사용자 지정 Supabase Schema 적용·실제 Spring Java JDBC 연결/verify-full TLS 확인 완료. BE2 공동 검토·최소 권한 앱 역할 대기, #3 종료/병합 완료 아님. 기존 절의 원격 미실행 기록은 아래 §9~10 후속 결과 이전 상태다.

## 1. 승인과 진행 상태

사용자가 Supabase PostgreSQL + Supabase CLI, 소프트 삭제·북마크 제거·개인 투표/활동/채택 이력 보존을 승인했다. Java 17 설치와 단계 1~5 진행을 요청했고 Docker 실패 후 별도 로컬 PostgreSQL 사용을 승인했다.

| 단계 | 실제 상태 |
| --- | --- |
| Java 17 | Temurin 17.0.20.101 설치, 실행 버전 17.0.20.1+1. JDK 25 보존. 설치 프로그램이 시스템 JAVA_HOME을 17로 등록한 사실 확인 |
| 1. 최신 기준 | #3에서 pull, Already up to date, 기준 aedc647. 기존 미커밋 변경 보존 |
| 2. 로컬 DB | native PostgreSQL 17.11 독립 실행, BIOS/Windows 서비스/WSL 기능 변경 없음 |
| 3. Migration | [CLI 생성 파일](../../supabase/migrations/20261006182228_mvp_schema.sql)에 27핵심 테이블·166컬럼·키/제약·인덱스·접근 제한 구현 |
| 4. 검증 | 실제 DB·CLI 적용/재실행/실패 복구·Java JDBC 포함 test/build 수행 |
| 5. 검토·PR | BE2 체크리스트와 결과 준비. 실제 팀 확인·사용자 검토·commit·push·PR은 아직 없음 |

API 경로/DTO와 실제 서버 DB 설정은 변경하지 않았다. 선택 공유·미정 세션 테이블, 실제 정본 seed, 증빙 보관 기간·물리 정리, 비-MVP 기능도 추가하지 않았다.

## 2. 로컬 DB와 팀 공유

로컬 DB는 실 사용자 데이터와 분리된 시험 공간이다. 잘못된 FK/중복/삭제/적용 실패를 시험해도 공용 DB에 영향을 주지 않는다. 이번 데이터는 example.invalid 이메일과 합성 지역·기관·회원만 사용했다.

이 PC DB를 외부에 열지 않는 것이 팀 공용 Supabase의 접근을 막는다는 뜻은 아니다. 팀은 Git의 Migration·검증 파일을 공유하고, 팀원별 DB 또는 합의된 공용 Supabase 개발 DB에 같은 변경을 적용한다. 실제 Supabase에는 link/query/push하지 않았다.

## 3. 환경과 설치

| 항목 | 값/경계 |
| --- | --- |
| PostgreSQL | 17.11 / x86_64-windows / EDB 바이너리 |
| CLI | Supabase CLI 2.120.0, npm exec 정확한 버전 지정 |
| Backend | Java 17.0.20.1+1 / 기존 Spring Boot 4.0.8·Gradle 9.7.1 유지 |
| 접속·인증 | 127.0.0.1:55432, SCRAM-SHA-256. 실제 listen_addresses 조회도 127.0.0.1. 외부 개방 없음 |
| 실행 파일 | C:/Users/PC/AppData/Local/Temp/discushion-issue3-pg17/runtime/pgsql/bin |
| 데이터 | C:/Users/PC/AppData/Local/Temp/discushion-issue3-pg17/data — 합성 시험 전용, 재부팅 자동 시작 서비스 아님 |
| DB | discushion_schema_draft, discushion_migration_test, discushion_roles_test, discushion_failure_test |
| 계정·정보 | 로컬 시험 postgres. 운영 앱 역할 선택을 뜻하지 않음. 시험 자격정보는 Git 제외 .local-db에 보관 |

설치 출처: [PostgreSQL 공식 Windows 안내](https://www.postgresql.org/download/windows/)의 [EDB 바이너리](https://www.enterprisedb.com/download-postgresql-binaries). 다운로드 파일 `postgresql-17.11-5-windows-x64-binaries.zip`의 SHA-256 계산값은 `80379B2C04D51C30225532E0AE04509899141E9957ED096FE749D7FD9DF8F82F`. 별도 공개된 checksum과 일치 검증했다고 주장하지 않는다.

한글 경로의 initdb 인코딩 오류를 피하려고 영문 임시 경로를 사용했다. 실행/종료는 위 설치의 pg_ctl과 정확한 데이터 경로로 수행한다. 다른 PostgreSQL 서비스/데이터는 건드리지 않는다.

## 4. 구현 내용과 서비스 경계

- [ERD](../specs/Discushion_MVP_ERD_상세명세.md)와 [준비 명세](Discushion_MVP_DB_SCHEMA_준비명세_2026-10-07.md) 기준. 선택 공유를 제외한 27테이블이다.
- 숫자형 API ID 유지: BIGINT identity 상한 9,007,199,254,740,991과 양수/안전 정수 CHECK. 공유/복합 PK는 부모 키 사용.
- 시각 timestamptz, 닉네임 10자·소개 50자, 종류 TEXT+CHECK. 미정 입력 길이·TTL·MB 환산을 임의 설정하지 않음.
- 복합 FK 8개: 신청/회원/지역, 기관 신청/credential, 투표/option, 댓글/post, 이벤트 대상, 채택 credential 귀속. 단일 FK 47개도 참조 대상까지 실제 카탈로그와 대조.
- 현재 채택만 부분 UNIQUE. 타 기관 관계·취소 후 재채택 회차 보존. 현재 관계와 누적 이벤트 분리.
- 안정 부모에 CASCADE 삭제 없음. Post 삭제 상태/시각 CHECK. 북마크 제거·공개 차단은 #16 서비스 트랜잭션에서 구현해야 하며 자동 API/트리거가 구현됐다는 뜻 아님.
- 비공개 discushion Schema, 모든 테이블 RLS. PUBLIC 및 존재하는 anon/authenticated/service_role의 Schema/테이블/시퀀스 권한 제거. Supabase Auth UUID 정책은 추가하지 않음.
- FK 자식/조회 인덱스, 현재 채택 부분 UNIQUE. 실제 API 쿼리의 성능 계획은 후속 구현에서 확인.

스킬 영향: Supabase 스킬의 접근 제한/RLS·실제 검증과 PostgreSQL 스킬의 FK 인덱스·부분 유일성 지침을 적용했다. CLI 버전·명령 help를 실제 확인했고 auth.uid()를 BIGINT 계정에 임의 연결하지 않았다.

## 5. 실제 실행 결과

대상은 현재 미커밋 작업 트리다. 없는 commit SHA를 기록하지 않는다. 기존 build 03:10 KST, native 준비/Schema 시험 03:25 이후, JDBC build 03:39 KST, 최종 SQL/복구 확인 03:45 이후 수행.

| 검사 | 실제 결과 |
| --- | --- |
| 초안 원자 적용 | draft DB에서 psql -1 -v ON_ERROR_STOP=1 -f Migration 성공. 개발 중 Migration 이력 기록 없음 |
| 전체 카탈로그 대조 | [검사 스크립트](../../supabase/tests/check-erd-schema.mjs): 27테이블·166컬럼 타입/NULL/PK/UNIQUE/FK, 단일 참조 47개·복합 8개 일치, 오류 0 |
| 무결성·기본 권한 | [SQL 시험](../../supabase/tests/schema_integrity.sql) 최종 73개 통과. 오류 SQLSTATE와 주요 UNIQUE 제약 이름 확인 |
| API 역할 모의 | [격리 SQL](../../supabase/tests/api-role-isolation.sql): 역할 이름만 일시 생성, 3역할×Schema/테이블/시퀀스 거부 9개 확인, 역할/Schema ROLLBACK |
| CLI 최초 적용 | 빈 migration_test에 migration up --db-url localhost 성공. 이력 20261006182228 1개 |
| CLI 재실행 | [재현 스크립트](../../supabase/tests/run-schema-tests.ps1) 성공. applied=[] 및 Schema dump/이력/합성 기존 데이터 불변. 만든 표식만 정리, regions 행 수 0 확인 |
| 원격 방지 | database.example.invalid 입력 시 연결 전에 거부 확인 |
| 실패 재시도 | 격리 failure_test에 의도된 42P01 실패: 부분 테이블 없음·성공 이력 0. 미적용 시험 파일 수정 후 성공: 테이블 존재·이력 1. 실제 공유/적용 파일을 고친 시험 아님 |
| Backend test/build | Java 17 + 실제 localhost JDBC 변수, gradlew.bat --no-daemon test build --console=plain --rerun-tasks 성공. JDBC 4+기존 4=8, skip/실패/오류 0, JAR 생성 |
| 한글·시각·ID | 실제 JDBC로 한글 문자열, 절대 시각, 안전 정수 최댓값 long 읽기 확인 |
| Supabase advisor | native의 anon 역할 부재로 실패. Supabase advisor 통과로 기록하지 않음 |
| 실제 Supabase·FE/BE | 미실행. TLS/pooler·Data API·제품 API 사용자 흐름 통합 시험을 대신하지 않음 |

SQL 삭제/이력 시험은 명시적 UPDATE/DELETE가 저장 구조와 호환됨을 확인한 것이며, 아직 없는 API의 자동 차단 기능 시험은 아니다. fixture ROLLBACK에도 sequence 상태는 바뀔 수 있어 전용 빈 시험 DB에만 실행한다.

도구 제한: CLI 2.120.0 db query --file의 복수 문장 prepared statement 오류는 psql로 대체. CLI의 Connecting to remote database는 db-url 모드 일반 출력이며 사용 URI는 전부 127.0.0.1이었다. 실제 원격으로 우회하지 않았다.

Migration 파일 SHA-256: `7E1B8153D477291A91E1A41671EEE26AC076660D79EC108E81E8691AC5AAAECA`. CLI 이력 관리가 자동 checksum 변경 감지를 보장한다고 가정하지 않으며 공유/적용 파일은 후속 Migration으로 변경한다.

## 6. BE2 검토 체크리스트 — 미확인

1. A~D 원본·현재 관계·누적 이력, B/C API의 컬럼/키·인덱스를 검토한다.
2. 실제 Supabase의 PostgreSQL 버전·기존 Schema/데이터·이력·적용 역할을 읽기 전용 확인한다. 빈 DB로 추측해 push하지 않는다.
3. 비공개 discushion Schema, Schema-qualified JDBC 쿼리, 서버 DB 역할·최소 권한/RLS 방식을 확인한다. 공개 역할에 직접 권한을 주지 않는다.
4. 실제 Supabase에서 advisor·TLS/pooler·권한을 확인한다. 역할 모의 시험을 실제 Auth/Data API 통합으로 취급하지 않는다.
5. 서비스의 잠금/상태 전이/이벤트 원자성을 공유한다. 완료 지역 최대 3개, 유형별 확장/옵션 개수, 종료/유효 인증, 같은 thread 답글, 파일 용도/합계는 후속 서비스 시험 대상이다.
6. ERD 표현 PR #35와 Schema 통합 순서를 확인한다. 증빙 보관 기간·세션·공유 저장·기관 동시 인증 정책은 미정 유지.

BE2에게 메시지를 보냈거나 확인받은 기록은 없다. 사람이 새 파일/diff를 검토한 뒤 요청하면 commit·push·PR을 진행한다. 확인 전 #3 자동 종료/완료 처리를 하지 않는다.

## 7. 남은 검증

MG01 최초 적용·MG02 재실행·MG04 실패 재시도와 제약 검사는 native에서 수행했다. 실제 이전 서비스 버전의 후속 변경(MG03), checksum 통제(MG05), 백업/복원(MG06), 실제 정본 seed(MG07), 운영 앱 역할/배포(MG08)는 미검증/합의 대기다. TX01~16의 실제 API·동시성 시험은 후속 기능 이슈에서 수행한다.

현재는 #3 기반 구현의 검토 가능 상태이며 모든 정책·팀 확인·원격 통합까지 끝났다는 뜻은 아니다.

## 8. 위임 후 공백 보완·연결 결정

2026-10-07 후속 전체 검증에서 탭/줄바꿈-only 입력 저장을 발견했다. 사용자 위임 후 기존 파일을 바꾸지 않고 `20261007011459_harden_nonblank_inputs.sql`을 추가했다. Unicode White_Space/BOM-only 거부와 실제 내용 허용 회귀 12개를 추가해 DB 시험이 85개가 됐고 재실행 불변/ERD 대조/모의 권한 9개/Java 8개 test/build를 재확인했다.

이력 2개의 native 출력이 줄 배열인 데 따른 비교 스크립트의 거짓 실패도 문자열 정규화로 수정했다. 실제 데이터 손상은 아니었다. 원격 연결은 transaction pooler·인증서/호스트 검증·pool 1·연결/소켓 timeout을 선택해 예시에 반영했다. [선택 이유·CLI 로그인 상태](../collaboration/backend-db-connection-decisions.md)를 참조한다. CLI 로그인은 성공했지만 로그인 후 프로젝트 목록이 `[]`이었다. 대상 프로젝트 접근 확인과 실제 Supabase JDBC 연결은 아직 완료되지 않았다. 원격 DB는 변경하지 않았다.

## 9. 사용자 생성 Supabase에 적용·검증

사용자가 직접 생성한 `pmhmgqpyvrbbseqelpze`를 지정해 사용하도록 요청했다. 실제 접근/신규 상태를 확인한 뒤 CLI link와 두 Migration의 dry-run·push를 수행했다. 원격 PostgreSQL 17.11, 도쿄 리전, 27테이블·166컬럼·55외래키·RLS 27개·공개 API 역할 접근 차단을 실제 조회했다. 원격 이력 두 개와 후속 dry-run의 적용 대상 없음도 확인했다. 실제 advisor는 WARN/ERROR 기준 문제가 없었다.

이 작업은 Supabase 스킬의 실제 검증·RLS/접근 제한 지침에 따라 수행했다. 플러그인 DB 실행 도구가 노출되지 않아 CLI를 사용했다. 관리 API의 SELECT 1/SSL 확인은 성공했지만 Java JDBC의 원격 연결·verify-full TLS 확인은 DB 비밀번호 입력 전이므로 아직 미검증이다. 원격 합성 fixture나 역할 모의 테스트, seed/별도 역할/Vault 변경은 하지 않았다. 기존 로컬 85/9/8개 결과를 원격 테스트 결과로 바꿔 기록하지 않는다.

상세 실행 결과와 비밀값 제외 로컬 연결 준비는 [결정 기록](../collaboration/backend-db-connection-decisions.md)의 후속 결과를 참조한다. 두 Migration은 원격에도 적용됐으므로 수정이 필요하면 새 Migration을 추가한다. BE2 검토·최소 권한 앱 역할·실제 API/Java 연결 확인·commit/PR 및 #3 종료는 미완료다.

## 10. 실제 Java 원격 연결 검증

사용자의 로컬 비밀번호 입력 후 `SupabaseJdbcSmokeTests`로 실제 Spring supabase 프로필과 Hikari/pgJDBC를 사용했다. 초기 기본 Java 신뢰 저장소 방식은 CA 신뢰 체인 오류로 실패했고, 공식 대시보드 배포 루트 CA를 연결 전용 sslrootcert로 지정한 뒤 2026-10-07 10:51 KST 3개가 모두 통과했다. 인증서·호스트 검증(verify-full)을 유지했고 시스템 전역 인증서 설정은 바꾸지 않았다.

실제 6543 transaction pooler의 SELECT 1/PostgreSQL 17, Schema 27/166/55/RLS 27·이력 두 개, 공개 역할 접근 차단을 읽기 전용으로 확인했다. 원격 fixture나 쓰기/DDL은 실행하지 않았으며 비밀번호를 출력/커밋하지 않았다. 로컬 실행 프로필은 supabase로 전환했다. 자세한 인증서 출처·지문·파일 경로·재현은 [결정 기록](../collaboration/backend-db-connection-decisions.md)을 참조한다. 실제 API/동시성/FE 통합과 서버 최소 권한 역할/BE2 검토까지 완료한 것은 아니다.

10:52 KST 전체 test/build 재실행 성공: Health 3 + 로컬 JDBC 4 + 설정 1 + 원격 JDBC 3 = 총 11개, skip/실패/오류 0. 실제 로컬 85개 SQL/9개 역할 시험과는 별도 Java 결과다. JAR 생성과 비밀 설정 Git 제외도 확인했다.

## 11. 현재 Feature 재검증 — 2026-10-07 13:46~13:48 KST

사용자 요청으로 현재 프로젝트의 Issue #3 변경을 다시 검증했다. 대상 SHA는 `2d3f87a912dc290c20e62f05ac20e3991c5969eb`, 브랜치는 `back/feature/3-schema`다. GitHub PR #78의 head SHA와 일치하며 PR은 open/Draft/미병합이다. 이전 절의 commit/PR 미생성 표현은 당시 기록이다.

| 실행한 검사 | 현재 실행 결과 |
| --- | --- |
| `supabase/tests/run-schema-tests.ps1` | DB 무결성 113개 통과. Migration 재실행 전후 Schema·이력·합성 표식 데이터 불변 |
| `node supabase/tests/check-erd-schema.mjs` | 27테이블·176컬럼·단일 FK 47·복합 FK 8·Privy UNIQUE 대조, 오류 0 |
| `psql ... -d discushion_roles_test -f supabase/tests/api-role-isolation.sql` | 공개 API 역할 이름 3개 × Schema/테이블/시퀀스 접근 차단 9개 통과. 트랜잭션 ROLLBACK |
| Java 17 `gradlew.bat --no-daemon test build --console=plain --rerun-tasks` | BUILD SUCCESSFUL. Health 3 + 로컬 JDBC 4 + 설정 1 + 실제 Supabase JDBC 3 = 총 11개, 실패/오류/skip 모두 0 |
| 실제 원격 SELECT-only smoke 검사 | 지정 개발 Supabase의 PostgreSQL 17·verify-full 연결, 27테이블·176컬럼·FK 55·RLS 27·정확한 Migration 이력 4개, 공개 API 역할 접근 차단 확인 |
| 변경/비밀 파일 검사 | `git diff --check`, `git diff --cached --check` 오류 없음. `.env`·시험 비밀번호·CA 파일의 Git 제외 확인 |

Java 결과 XML의 이번 실행 시각은 13:47:12~13:47:20 KST이며 JAR도 이번 실행으로 생성했다. 기존 결과를 재사용하지 않았다. 보고서는 `backend/build/reports/tests/test/index.html`, JAR는 `backend/build/libs/discushion.jar`다.

실행 환경: Temurin `jdk-17.0.20.101-hotspot`, 기존 native PostgreSQL 17.11 시험 cluster, 기존 Supabase CLI 2.120.0 캐시. 로컬 DB가 꺼져 있어 `pg_ctl`로 기존 시험 cluster를 `-h 127.0.0.1 -p 55432`로 시작하고 모든 검사 뒤 정상 종료했다. 로컬 첫 접속은 CLI의 TLS 기본값 때문에 실패했고, localhost 시험 URI에 `sslmode=disable`을 명시한 후 통과했다. 원격 JDBC는 기존 `sslmode=verify-full`을 유지했다. 비밀번호는 Git 제외 파일에서 환경변수로 읽고 출력하지 않았다.

이번 Migration up 두 실행은 이미 적용된 로컬 이력에 대한 재실행으로 `applied: []`였다. 빈 DB에 대한 최초 CLI 적용·실패 복구·백업/복원은 이번에 다시 실행하지 않았다. 역할 모의 시험은 localhost 전용 DB에서 네 Migration의 DDL을 트랜잭션 내 실행하고 ROLLBACK했다. 원격에는 SELECT-only smoke 검사만 실행했으며 Migration push·fixture·seed·역할 변경·advisor 재실행은 하지 않았다.

기존 미추적 `docs/specs/Discushion_MVP_ERD.mmd`는 보존했다. 카탈로그 대조는 저장소에 구성된 검사의 입력인 `Discushion_MVP_ERD_상세명세.md`와 승인된 v10.2 overlay를 대상으로 했다. 구현 파일과 Migration 변경은 없으며 이번 변경은 이 검증 기록뿐이다.

판정: 현재 로컬 Feature와 지정 원격 DB의 Schema 기반 검사는 통과했다. Issue #3 전체 완료는 아니다. PR의 BE2 공동 검토, 최소 권한 서버 계정/RLS 접근 모델 협의, #74 토큰/파일 API·FE 경계 확인, PR #35와 문서 병합 순서, 필수 CI/리뷰는 별도 확인이 필요하다. 실제 Privy OTP·Storage worker·제품 API·FE/BE 사용자 흐름은 후속 기능 이슈의 검증 대상이며 이번 검사로 완료 처리하지 않는다.

## 12. 사용자 RLS 결정과 후속 이관

§11 검증 이후 사용자가 Spring 사용자 권한 검사 + 서버 전용 역할의 제한된 DB 접근 방식을 채택했다. RLS 유지, 비소유자·BYPASSRLS 없는 서버 역할과 필요한 테이블/작업에 한정된 허용 정책, 공개 역할 차단을 원칙으로 정했다. §11의 접근 모델 협의 대기는 사용자 결정으로 갱신하되 BE2 실제 검토 기록과 구분한다.

1번 권한표는 BE1/#4에서 실제 권한 부여 전에, 3번 계정·연결은 BE2/#30과 #4 협업으로 서버 역할 기능 검증 전에 준비한다. 4번은 실제 서버 계정의 DB 권한 시험과 각 API 권한 거부 시험으로 수행하고 #31 전에 해소한다. 상세 담당·시점·완료 조건은 [DB 연결 결정 기록](../collaboration/backend-db-connection-decisions.md)의 마지막 절에 명시했다. 실제 계정/RLS 정책 구성은 아직 미완료이며 위 113/9/11개 검사는 새 서버 역할 검증 결과가 아니다.

이번 후속 변경은 결정·검증 문서만 수정하며 코드·Migration·DB 권한은 변경하지 않는다. §11의 실제 test/build 결과는 코드 SHA `2d3f87a912dc290c20e62f05ac20e3991c5969eb`에 대한 결과로 유지한다. BE2 공동 검토·후속 배정 확인과 필수 리뷰·CI 후에만 병합 여부를 판단한다.

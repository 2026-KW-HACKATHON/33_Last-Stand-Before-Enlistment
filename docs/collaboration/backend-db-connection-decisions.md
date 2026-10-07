# Issue #3 — DB 연결·입력 검증 결정 기록

기준일: 2026-10-07 (Asia/Seoul). 사용자(BE1)의 “다 정해주고, 어떻게 정했는지 보고해. CLI도 로그인 해.” 요청으로 접속 방식과 발견된 공백 검증을 결정했다. 다른 이슈의 세션·메일·보관 기간을 임의 확정하는 기록은 아니다. BE2의 실제 환경 확인과 구분한다.

## 선택과 이유

| 항목 | 선택 | 이유·경계 |
| --- | --- | --- |
| DB/Migration | Supabase PostgreSQL + CLI 2.120.0 | 기존 승인 유지. 도구 중복 관리 없음 |
| 시험 DB | native PostgreSQL 17.11 / localhost:55432 / SCRAM | Docker 문제를 피하고 실 데이터와 분리 |
| 원격 접속 | transaction pooler 6543, prepareThreshold=0 | IPv4·서버리스 실행 후보와 pooler prepared statement 제한 대응 |
| TLS | verify-full + sslrootcert 공식 Supabase CA | 초기 DefaultJavaSSLFactory는 실제 검증에서 신뢰 체인 실패. 연결별 CA로 보완, 인증서·호스트 검증 유지, 전역 trust store 변경 없음 |
| 한도/시간 | pool 1, 연결 10초·소켓 30초 | 초기 연결 한도·빠른 실패 기준. 실제 환경에 따라 BE2와 조정 |
| Schema·접근 | private discushion + RLS + 공개 API 역할 차단 | 계정 해시·증빙/담당자 정보 직접 노출 방지. JDBC는 Schema-qualified 쿼리. 실제 서버 역할/최소 권한은 대상 DB 확인 후 구성 |
| 비밀값 | backend/.env 또는 배포 secret, Git 제외 | DB 비밀번호·관리 토큰을 커밋/채팅에 넣지 않음. 실제 host/user/password를 추측하지 않음 |
| 공백 검사 | Unicode White_Space와 BOM-only 거부 | 탭·줄바꿈·전각 공백·NBSP 우회 방지. 실제 문자가 있는 다중행 허용, 자동 trim/기존 행 변환 없음 |
| 이력 | 기존 적용 파일 유지 + 후속 Migration | 이력을 덮어 고치지 않음. 기존 공백-only 데이터가 있으면 적용 전 확인하며 임의 삭제하지 않음 |

[backend/.env.example](../../backend/.env.example)에 연결 기술 기준을 반영했다. 실제 .env는 접속 정보 확인 전 허위 값으로 만들지 않았다. 근거: [Supabase 연결 안내](https://supabase.com/docs/guides/database/connecting-to-postgres), [pgJDBC TLS](https://jdbc.postgresql.org/documentation/ssl/).

## 보완·검증

기존 `20261006182228_mvp_schema.sql` 해시를 보존하고 CLI가 생성한 [후속 Migration](../../supabase/migrations/20261007011459_harden_nonblank_inputs.sql)으로 닉네임·댓글 CHECK를 교체했다. 컬럼·타입·DTO는 변경하지 않았다.

회귀 검사 12개를 추가해 DB 시험 85개가 통과했다. Migration 이력이 2개가 되면서 PowerShell 줄 배열 비교가 거짓 실패하는 문제도 고쳐 출력을 단일 문자열로 정규화했다. Schema/데이터 손상으로 해석하지 않는다. 실제 재실행 불변·ERD 일치·API 역할 모의 9개·Java JDBC 포함 8개 test/build도 재확인했다. 대상은 aedc647 + 미커밋 변경, 적용은 localhost뿐이다.

## CLI 로그인과 실제 연결 — 로그인 성공, 프로젝트 확인 필요

- 공식 login help 확인 후 실행. 자동 JSON 프롬프트 오류는 `--agent no --output-format text --no-browser`로 해결했고 사용자 브라우저 인증 절차가 시작됐다.
- 브라우저에 로그인 링크를 열고 계정 로그인/승인과 이번 verification code를 요청했다. 장기 access token/DB 비밀번호는 채팅에 요청하지 않았다.
- 사용자 verification code 입력 후 CLI가 `You are now logged in`을 반환하고 정상 종료했다. 인증 코드·관리 토큰은 문서에 저장하지 않았다.
- 로그인 후 CLI 2.120.0의 `projects list --output json`을 실행했다. 정상 종료했지만 프로젝트 목록은 `[]`이었다. 현재 로그인 계정에 조회 가능한 프로젝트가 없으며, Discushion 대상 프로젝트는 확인하지 못했다. 프로젝트 연결 여부 안내는 별개이며 빈 목록을 실제 DB 연결 성공으로 취급하지 않는다.
- CLI 관리 API 로그인과 실제 Spring JDBC의 DB 비밀번호·접속 성공은 다르다.
- 인증 후 기존 Discushion 프로젝트를 읽기 전용 확인한다. 후보가 없거나 여러 개면 대상을 확인한다. 새 유료 프로젝트 생성·DB 비밀번호 재설정·원격 Migration 적용은 하지 않는다.
- 실제 접속 정보가 안전하게 준비되면 read-only SELECT 1, DB 버전, TLS, 역할/Schema를 확인한다. 원격 DDL·데이터 변경은 하지 않는다.

계정 인증은 완료됐다. 실제 프로젝트와 DB 비밀번호는 기술 선택으로 대신할 수 없다. 대상 프로젝트 접근과 실제 JDBC 검증 전에는 Supabase 연결 완료·#3 종료를 선언하지 않는다.

## 사용자 생성 프로젝트 연결·원격 적용 — 2026-10-07 후속 결과

위의 빈 목록과 원격 미변경 기록은 프로젝트 생성 전 상태다. 사용자가 직접 생성하고 지정한 프로젝트 `pmhmgqpyvrbbseqelpze`를 사용하도록 요청한 후 다음을 수행했다. 플러그인은 설치되어 있으나 이번 대화에 Supabase 실행 도구가 노출되지 않아 로그인된 CLI 2.120.0을 사용했다.

- 프로젝트 상태 `ACTIVE_HEALTHY`, 리전 `ap-northeast-1`(도쿄), 실제 SQL 버전 PostgreSQL 17.11 확인. 신규 생성·요금제 변경·비밀번호 재설정은 하지 않았다.
- 적용 전 public 앱 테이블 0개, discushion Schema 없음, Migration 이력 테이블 없음을 실제 조회했다.
- 지정 ref로 CLI link 성공. 임시 연결 메타데이터는 Git 제외 `supabase/.temp`에만 기록된다.
- dry-run에서 기존 Migration 두 개만 대상임을 확인하고 `db push --linked --project-ref pmhmgqpyvrbbseqelpze --skip-vault --yes`로 적용했다. 별도 역할·seed·Vault 변경은 하지 않았다. 두 파일은 이제 원격 적용 이력이므로 덮어 수정하지 않는다.
- 실제 SELECT 1 성공. 27테이블·166컬럼·55외래키(단일 47 + 복합 8), RLS 활성화 27개, anon/authenticated/service_role의 Schema·테이블 접근 차단 확인.
- 이력 `20261006182228`, `20261007011459` 확인. 후속 dry-run은 `upToDate=true`, 대상 `[]`였다.
- `db advisors --type all --level warn --fail-on error`는 정상 종료하고 `No issues found`를 반환했다. 경고/오류 없음이며 INFO까지 없다는 주장이나 제품/API 통합 검증은 아니다.
- 관리 API SQL 세션의 SSL 활성화도 확인했다. 이것은 Spring Java JDBC의 TLS 인증서 검증 성공을 의미하지 않는다.

실제 CLI 연결 메타데이터에서 pooler 호스트 `aws-0-ap-northeast-1.pooler.supabase.com`와 초기 확인용 사용자 `postgres.pmhmgqpyvrbbseqelpze`를 확인했다. CLI 메타데이터의 5432는 session pooler다. 기존 선택인 transaction pooler 6543을 로컬 `backend/.env`에 준비했지만 실제 6543 Java 연결은 아직 시험하지 않았다. DB_PASSWORD는 비워 두고 비밀번호 입력 전 기존 local 프로필을 유지한다. `.env`는 Git 제외를 확인했으며 실제 비밀번호를 저장/출력하지 않았다.

남은 단계: 사용자가 로컬 `.env`의 DB_PASSWORD를 안전하게 입력 → Java JDBC의 SELECT 1·verify-full TLS·Schema 읽기 검증. 실제 서버 최소 권한 역할/RLS 구성은 BE2/#4와 확인하며 postgres를 최종 앱 계정으로 확정하지 않는다. 원격 fixture/역할 모의 시험을 실행하지 않았고, #3 종료·commit·PR도 수행하지 않았다.

## 실제 Spring JDBC 연결 확인 — 비밀번호 입력 후

사용자가 로컬 `.env`에 비밀번호를 입력한 후, `SupabaseJdbcSmokeTests`를 추가해 Java 17 + 실제 Spring `supabase` 프로필/Hikari DataSource로 SELECT-only 검사를 수행했다. 비밀번호는 출력하거나 커밋하지 않았다.

첫 실행은 3개 모두 SSLHandshakeException / SunCertPathBuilderException으로 실패했다. 비밀번호 인증 이전 인증서 신뢰 체인 오류이며 비밀번호 오류로 판정하지 않았다. 공식 [대시보드 인증서 URL 설정](https://github.com/supabase/supabase/blob/master/apps/studio/hooks/custom-content/custom-content.json)과 [SSL 안내](https://supabase.com/docs/guides/platform/ssl-enforcement)를 확인해 공식 배포 CA를 Git 제외 `.local-db/supabase-prod-ca-2021.crt`에 다운로드했다.

- 출처: `https://supabase-downloads.s3-ap-southeast-1.amazonaws.com/prod/ssl/prod-ca-2021.crt`
- 발급자/Subject: Supabase Root 2021 CA / Supabase Inc. CA=true, 인증서 서명용 키 용도, 2021-04-28~2031-04-26 유효기간 확인.
- 다운로드 파일 SHA-256: `700723581420DD1AC98FD7E9AC529F0EF210EADCAF87FC868A3AD7D114C2F3B7`. 인증서 DER SHA-256: `807025AD50D4ED219D2C9C7D299C004F824EB00CF7F65AFEF607D07B72E6CAFA`. 둘은 서로 다른 바이트 형식의 해시다.
- `DefaultJavaSSLFactory`를 제거하고 pgJDBC 기본 LibPQFactory + `sslrootcert=../.local-db/supabase-prod-ca-2021.crt`를 사용했다. 시스템/Java 전역 신뢰 저장소는 변경하지 않았으며 verify-full을 유지했다. 서버에서 제시한 인증서를 임의로 신뢰한 방식이 아니다.

2026-10-07 10:51 KST 재실행은 3개 모두 통과했다: (1) transaction pooler 6543 실제 인증/SELECT 1/PostgreSQL 17 + 인증서·호스트 검증, (2) 27테이블·166컬럼·55외래키·RLS 27개·Migration 이력 두 개, (3) 공개 API 3역할의 Schema/테이블 접근 차단. 쓰기 쿼리/fixture/DDL 없이 수행했다. 로컬 `.env`를 supabase 프로필로 전환하고 예시·README에 CA 파일 요구사항을 반영했다.

이 검사는 명시적인 `DISCUSHION_VERIFY_SUPABASE=true`에서만 실행하며 기본 CI에서는 skip된다. 상대 CA 경로는 `backend/` 작업 디렉터리 기준이며 다른 PC/배포에서는 공식 CA를 별도로 준비하고 실제 경로를 설정해야 한다. postgres는 초기 연결 검증 계정이지 최종 서버 최소 권한 역할이 아니다. BE2 확인·#4 접근 모델·제품 API/FE 통합·commit/PR 및 #3 종료는 여전히 미완료다.

2026-10-07 10:52 KST 전체 Java 17 `test build --rerun-tasks`도 성공했다. XML 집계: Health 3 + 로컬 JDBC 4 + 설정 1 + 실제 원격 JDBC 3 = 총 11개, skip/실패/오류 모두 0. JAR 생성 성공. `git diff --check` 오류 없고 `.env`와 CA 파일의 Git 제외를 재확인했다. Supabase 스킬의 실제 검증/비밀값 보호 원칙이 연결 검사와 CA 처리에 반영됐다.

## 최신 develop·MVP v10.2 후속 — 승인 후 결과

최신 `back/develop`의 PR #76 / `dad35c0`을 fast-forward pull했고 파일 충돌은 없었다. 제품 변경으로 초기 v10.1 회원/증빙/사진 모델의 의미 차이를 발견해 commit/PR을 보류했다. 이후 사용자(BE1)가 [v10.2 DB 설계](../architecture/Discushion_Issue3_MVP_v10.2_반영.md)를 승인했다.

기존 두 적용 파일을 그대로 보존하고 CLI 생성 후속 두 개를 추가했다. `20261007021128`은 비밀번호/기관 신청 필수 의존 해제·legacy 보존, `20261007023149`는 Privy 1:1·가입 완료·사진 lifecycle/정리 후보/삭제 재시도 컬럼과 제약·부분 인덱스다. 로컬 SQL 113개·권한 모의 9개·카탈로그 27/176/단일 FK47/복합8 대조가 통과했다. 실제 Storage worker·Privy 로그인/API 구현은 후속 이슈이며 DB 상태와 외부 작업을 구분한다.

원격 적용 전 회원/파일/기관 자격은 각각 0행이었다. dry-run의 두 파일 대상 확인 후 지정 Supabase에 적용했으며 이후에도 모두 0행, RLS 27개·이력 4개·새 제약/NULL 변경을 실제 조회했다. 원격 advisor WARN/ERROR 없음, 후속 dry-run은 적용 대상 없음이다. 11:35 KST Java 17 test/build 11개(skip/실패/오류 0)도 현재 원격 176컬럼/이력 4개와 verify-full TLS를 실제 확인했다.

최종 fetch에서도 HEAD와 origin/back/develop의 분기 0/0(`dad35c0`)이었다. 본문 이전 절의 미정/원격 미적용 결과는 당시 기록이며 최신 상태는 이 절과 v10.2 상세 계약을 따른다. 서버 최소 권한 로그인 역할은 아직 없고 초기 postgres 사용은 미완료 경계로 유지한다. BE2 공동 검토 대기이므로 요청된 PR은 Draft로 준비하고 병합/Issue 종료는 하지 않는다.

## MVP 서버 접근·RLS 방식 결정 — 2026-10-07

사용자가 추천안을 채택하고 Issue #3 PR에 반영하도록 요청했다. 아래는 사용자 결정이며 BE2/FE가 실제 검토·승인했다고 기록하지 않는다. 이 절은 앞 절의 RLS 접근 방식 미정 상태를 대체한다. 역할/권한/RLS 정책의 실제 구성은 후속 작업이다.

| 경계 | 결정 |
| --- | --- |
| Spring 사용자 권한 | 검증된 Privy 인증 결과와 로컬 회원 연결·가입 완료 상태를 확인한다. 작성자 소유권, 완료 지역, 기관 자격 유효기간·담당 지역, 공유 게스트 권한은 서버에서 제품 명세대로 판정한다. 클라이언트 userId·역할·배지를 권한 근거로 사용하지 않는다. |
| DB 실행 역할 | Migration 역할과 분리된 서버 전용 로그인 역할을 사용한다. 테이블 비소유자이며 SUPERUSER·BYPASSRLS·관리자 역할로의 권한 상승을 허용하지 않는다. 필요한 Schema 사용·테이블 작업·시퀀스 권한만 부여한다. 역할명과 테이블별 권한은 후속 권한표에서 정한다. |
| RLS | 기존 테이블의 RLS를 유지한다. 필요한 테이블과 작업에만 서버 전용 역할을 지정한 정책을 추가한다. 해당 서버 정책은 서버가 처리해야 하는 행을 허용하며 사용자별 행 접근의 최종 판정은 Spring에서 수행한다. PUBLIC·anon·authenticated·service_role의 discushion 접근 차단을 유지한다. |
| 무결성/삭제 | DB CHECK·FK·UNIQUE를 유지한다. 게시물 소프트 삭제와 투표·활동·기관 채택 이력 보존을 권한표에 반영한다. 삭제가 필요한 관계와 기록의 물리 삭제 권한을 개별 검토한다. |

서버용 허용 정책은 테이블 권한을 대신하지 않는다. 두 조건을 모두 충족해야 접근할 수 있다. `USING (true)` / `WITH CHECK (true)` 형태가 필요한 경우에도 지정 서버 역할·해당 작업에 한정하며 전체 테이블/공개 역할에 일괄 허용하지 않는다. 사용자별 DB 세션 컨텍스트나 Supabase Auth의 auth.uid()에 의존하는 접근 모델은 이번 결정에 포함하지 않는다. Privy 토큰 직접 검증/세션 교환의 상세 계약은 #74의 기존 선행 조건을 따른다.

**검증 경계:** 이 모델의 서버용 RLS는 허용된 작업 안에서 회원별 행을 격리하지 않는다. Spring 권한 검사나 조회 조건 누락으로 생기는 타인·타지역 접근은 RLS가 대신 차단한다고 주장하지 않는다. 객체별 조회 조건과 쓰기 시점의 자격·소유권 재검증을 구현하고 실제 권한 거부 시험을 수행한다. 기존 postgres smoke 시험은 관리자 연결 확인이며 최종 서버 역할의 최소 권한 검증이 아니다.

### 1·3·4번 후속 처리와 완료 시점

아래는 사용자 요청으로 #4·#30·#31의 완료 조건에 이관하는 후속 명세다. BE2의 후속 배정·실행 계약 확인은 #4의 권한표 공동 검토와 #30의 계정 구성 준비에서 기록한다. #3에서는 접근 방식과 이관 조건을 문서화하고 실행 완료로 표시하지 않는다.

| 항목 | 담당·연결 Issue | 완료 조건·완료 시점 |
| --- | --- | --- |
| 1. 테이블별 권한 정리 | BE1, BE2 공동 검토 / #4; 각 기능 Issue에서 필요한 변경 제안 | 테이블별 SELECT·INSERT·UPDATE·DELETE, Schema USAGE·시퀀스 권한, 서버 역할용 RLS 정책 및 legacy/이력/소프트 삭제의 제한을 문서화한다. 기본 권한표는 #4 완료/병합 전에 BE1·BE2 검토를 마쳐 확정한다. #30에서 서버 역할을 생성·GRANT하는 작업의 선행 조건이며, #30의 계정 구성 착수가 더 빠르면 그 전에 확정한다. 후속 기능별 권한 변경은 해당 기능 Issue 완료/병합 전에 검토·갱신한다. |
| 3. 계정·서버 연결 구성 | BE2, DB 권한은 BE1 검토 / #30, #4와 협업 | 확정 권한표에 따라 별도 서버 계정·RLS 정책을 적용하고 Migration 계정과 분리한다. 비밀값은 저장소 외부에서 관리하며 JDBC/pooler·verify-full TLS 연결을 검증한다. 서버 역할로 DB 의존 기능을 검증하기 전에 개발 계정을 준비하고 배포 환경 연결은 #31 착수 전에 완료한다. #30 전체 완료를 기다려 개발 계정 준비를 지연하지 않는다. |
| 4. 새 계정 실제 검증 | BE1·BE2 / #4·#30, 각 기능 Issue, #31 | 허용된 정상 조회/저장 성공, 금지된 DDL·TRUNCATE·불필요한 물리 삭제·관리자 권한 상승 거부, 공개 역할 차단을 실제 서버 계정으로 확인한다. 타인·타지역·유효/만료 기관·게스트 범위는 실제 API에서 거부를 검증한다. 계정 준비 직후 DB 권한 시험을 수행하고 각 API 완료 전 해당 기능 권한 시험, #31 전체 인수 전에 누락을 해소한다. |

PR #78 병합 전에는 현재 구현된 Schema의 BE2 공동 검토와 필수 리뷰·CI를 확인한다. PR #35 문서 통합 순서는 해소됐다. 후속 권한표·계정·검증·파일 실행 계약 확인은 아래 해당 이슈에서 수행하며, 이관을 구현 완료나 BE2 검토 완료로 표시하지 않는다. #3 종료/병합, #4 구현 착수, #74 상세 계약 완료, 실제 계정 구성·제품 연동 완료를 뜻하지 않는다.

### GitHub 이슈로 이관하는 추적 항목 — 2026-10-07

사용자가 PR #78의 미완료 후속 작업을 필요한 이슈에 이관하도록 요청했다. PR에만 적힌 할 일을 해당 GitHub 이슈 본문의 미완료 체크리스트로 연결한다.

| 이관 대상 | 이관 범위 | 담당·기한 |
| --- | --- | --- |
| #4 | 테이블별 권한표·서버 역할용 RLS 정책 설계, 사용자 권한 검사 및 실제 권한 거부 시험 | BE1, BE2 공동 검토. 기본 권한표는 #4 완료/병합 전 및 #30 계정 생성/GRANT 전 중 더 이른 시점. API 권한 시험은 해당 기능 완료 전 |
| #30 | 서버/Migration 계정 분리, 실제 RLS/권한 적용·JDBC/TLS 연결·DB 권한 성공/거부 검증, BE2 후속 배정·실행 계약 확인 | BE2, BE1 DB 권한 검토. 개발 계정은 서버 역할로 DB 의존 기능을 검증하기 전, 배포 연결과 검증은 #31 착수 전 |
| #31 | #4/#30 권한표·계정·검증 증거의 누락 확인과 실제 FE/BE 권한 인수 | BE1·BE2. #31 완료 및 develop→main 최종 PR 전 |
| #74 | #3의 회원 연결/가입 완료·사진 lifecycle Schema와 토큰/파일 상세 계약의 정합성 및 BE1·BE2 검토 기록 | BE1·BE2, 영향 있는 FE 계약 확인. 각 의존 기능 코드 구현 전 back/develop에 반영 |

#30의 개발 계정 준비는 #4와 협업 가능한 독립 환경 준비로 먼저 수행한다. #30 전체 완료를 기다려 #4 또는 각 기능의 실제 서버 역할 검증을 지연하지 않는다. 후속 작업의 팀 확인은 해당 이슈에서 기록하며, 현재 #3 구현에 대한 공동 Schema 리뷰와 PR 자체의 필수 리뷰·CI는 PR #78에 남긴다.

## #4 기본 서버 권한표 준비 — BE1, BE2 공동 검토 대기 (2026-10-07)

기준: back/develop `6a84ffc`의 현재27테이블/180컬럼, 적용 이력을 보존한 Migration5개. #4 인증 adapter는 users·neighbor_verified_regions·institution_credentials를 사용하며 users의 SELECT FOR UPDATE에는 SELECT 외 UPDATE 권한도 필요하다. 아래는 기능별 서버 작업을 위한 **검토안**이며 실제 서버 역할/GRANT/공유 DB 적용 완료가 아니다. 기본 표는 #4 완료/병합 전과 #30 계정 생성/GRANT 전 중 더 이른 시점에 BE1·BE2가 확정한다. 기능의 필수 API가 미정이면 해당 기능 권한도 구현 대조 후 확정한다.

S/I/U/D는 SELECT/INSERT/UPDATE/DELETE다. `—`는 부여하지 않음을 뜻한다. 단일 서버 계정의 표이므로 표의 S 허용은 다른 회원/지역/기관 조회 허용을 뜻하지 않는다. 모든 제품 권한·조회 조건은 Spring에서 판정한다.

| 테이블 | S | I | U | D | 근거·확인 Issue |
| --- | --- | --- | --- | --- | --- |
| regions | S | — | — | — | #9 공통 원본 조회. 원본/seed 준비는 별도 지정 계정 |
| institutions | S | — | — | — | #12/#29 기관 원본 조회. 원본/seed 준비는 별도 지정 계정 |
| users | S | I | U | — | #4 회원 연결/공통 잠금, #7 가입. 탈퇴/계정 물리 삭제 제외 |
| profiles | S | I | U | — | #7/#10 동의된 프로필 입력/변경 |
| profile_attributes | S | I | — | D | #7/#10 본인 프로필 속성 관계 교체. 회원/활동 이력 삭제와 구분 |
| user_agreements | S | I | U | — | #7 동의 저장. 정책 버전·기록 보존 |
| neighbor_verified_regions | S | — | — | — | #4/#11 완료 지역만 조회. #75 시연 자격는 지정 준비 계정으로 생성 |
| institution_credentials | S | — | — | — | #4/#12/#29 저장된 유효기간/담당 지역 조회. #75 준비 계정 분리 |
| email_verifications | — | — | — | — | legacy. 자체 OTP 미구현 |
| neighbor_verification_requests | — | — | — | — | legacy. 신청/접수 제외 |
| neighbor_verification_evidences | — | — | — | — | legacy. 증빙 제출 제외 |
| institution_verification_requests | — | — | — | — | legacy. 신청/접수 제외 |
| institution_verification_evidences | — | — | — | — | legacy. 증빙 제출 제외 |
| media_files | S | I | U | — | BE2 #13/#16 lifecycle·잠금·삭제 예약/재시도. 상태 이력은 보존. 프로필 용도 #7/#10은 별도 파일 계약 확인 |
| posts | S | I | U | — | BE2 #14/#16 생성·수정·소프트 삭제·잠금 |
| activity_post_details | S | I | U | — | BE2 #14/#16 활동 상세 원본. 게시물 소프트 삭제 정책 유지 |
| polls | S | I | U | — | BE2 투표 원본/공통 잠금. 변경 가능 필드는 #16/#25 계약대로 제한 |
| poll_options | S | I | — | — | BE2 투표 생성. 진행/종료 투표 선택지 변경·물리 삭제 제외 |
| post_photos | S | I | U | D | BE2 #13/#14/#16 순서·참조 제거. 동일 transaction에서 파일 삭제 예약을 기록 |
| comments | S | I | — | — | BE1 #22 댓글/답글 작성·조회. 게시물 삭제로 참여 이력 물리 삭제하지 않음 |
| post_reactions | S | I | — | D | BE1 #23 본인 반응 등록/취소. 활동 이벤트는 보존 |
| comment_evaluations | S | I | U | D | BE1 #24 본인 평가 등록/전환/취소. 실제 저장 방식에서 필요한 작업만 확정 |
| vote_selections | S | I | U | — | BE1 #25 선택 제출/변경. 표·종료·개인 투표 이력 보존 |
| bookmarks | S | I | — | D | BE1 #26 본인 해제·게시물 삭제 시 관계 자동 해제 |
| activity_events | S | I | — | — | BE1 #5 누적 등록 행동. 취소/전환/삭제로 이력 수정·삭제하지 않음 |
| institution_agenda_adoptions | S | I | U | — | BE1 #29 채택·취소시각/행위자 기록. 감사 관계 보존 |
| ai_agenda_summaries | S | I | U | — | BE2 #20 생성·재생성·상태 갱신. 삭제 원문 비노출 계약 유지 |

### Schema·시퀀스·RLS·계정 경계

- 서버에는 discushion Schema USAGE만 허용하고 CREATE/테이블 소유권·DDL·TRUNCATE·TRIGGER·권한 위임·관리자 역할·SUPERUSER/BYPASSRLS를 부여하지 않는다. Schema/테이블 적용 계정은 서버와 분리한다. 상속/PUBLIC에서 받은 유효 권한도 #30 실제 역할로 확인한다.
- 현재 PK는 GENERATED ALWAYS AS IDENTITY이며 서버는 기본값으로 INSERT/RETURNING한다. 그 내부 자동 생성은 serial/직접 nextval와 구분해 실제 PostgreSQL 역할 시험으로 확인한다. 직접 nextval/currval/setval·재시작/번호 덮어쓰기는 서버 요구 범위에 없으므로 ALL SEQUENCES 일괄 권한을 부여하지 않는다. 직접 nextval가 필요한 SQL을 도입할 경우 해당 기능에서 실제 시퀀스와 USAGE 필요성을 다시 검토하고, SELECT/UPDATE/setval 권한을 자동 확대하지 않는다.
- 식별자 생성 대상은 users/institutions/media_files/각 legacy 신청·OTP/posts/polls/poll_options/post_photos/comments/activity_events/institution_agenda_adoptions다. 위 표에서 서버 INSERT가 없는 원본/legacy에는 ID 생성 권한도 없다. 복합/기존 FK PK 테이블은 별도 시퀀스가 없다. #30은 pg_get_serial_sequence와 카탈로그로 실제 이름·권한을 대조한다.
- RLS는 현재 모든 테이블에서 유지한다. 위 허용 작업만 실제 서버 역할 TO 정책으로 제공한다. SELECT/DELETE는 USING, INSERT는 WITH CHECK, UPDATE는 USING+WITH CHECK가 필요하며 UPDATE/잠금 경로는 SELECT 정책도 함께 확인한다. legacy와 미허용 작업에는 서버 허용 정책을 만들지 않는다. 서버 역할의 작업 허용 정책이 사용자별 행 격리를 대신하지 않으며 PUBLIC/anon/authenticated 역할 차단을 유지한다.
- BE2의 B 영역 권한, 공통 파일/트랜잭션 제약, 실제 계정 이름·JDBC/TLS·pooler는 공동 검토 대기다. #30이 실제 역할을 구성한 뒤 허용 조회/저장·금지 DDL/삭제·RLS를 시험한다. localhost의 합성 NOLOGIN 역할/RLS 시험을 실제 Supabase 서버 로그인 계정 검증으로 표시하지 않는다.

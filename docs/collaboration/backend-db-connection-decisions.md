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

아래는 사용자가 요청한 PR 후속 명세다. 각 담당자의 기존 Issue 범위에 연결하며 BE2의 수락/검토 기록은 PR에서 확인한다. #3에서는 접근 방식과 이관 조건을 문서화하고 실행 완료로 표시하지 않는다.

| 항목 | 담당·연결 Issue | 완료 조건·완료 시점 |
| --- | --- | --- |
| 1. 테이블별 권한 정리 | BE1, BE2 공동 검토 / #4; 각 기능 Issue에서 필요한 변경 제안 | 테이블별 SELECT·INSERT·UPDATE·DELETE, Schema USAGE·시퀀스 권한, 서버 역할용 RLS 정책 및 legacy/이력/소프트 삭제의 제한을 문서화한다. 실제 서버 역할 생성·GRANT 전에 기본 권한표를 확정하고 기능 추가 시 갱신한다. |
| 3. 계정·서버 연결 구성 | BE2, DB 권한은 BE1 검토 / #30, #4와 협업 | 확정 권한표에 따라 별도 서버 계정·RLS 정책을 적용하고 Migration 계정과 분리한다. 비밀값은 저장소 외부에서 관리하며 JDBC/pooler·verify-full TLS 연결을 검증한다. 서버 역할로 DB 의존 기능을 검증하기 전에 개발 계정을 준비하고 배포 환경 연결은 #31 착수 전에 완료한다. #30 전체 완료를 기다려 개발 계정 준비를 지연하지 않는다. |
| 4. 새 계정 실제 검증 | BE1·BE2 / #4·#30, 각 기능 Issue, #31 | 허용된 정상 조회/저장 성공, 금지된 DDL·TRUNCATE·불필요한 물리 삭제·관리자 권한 상승 거부, 공개 역할 차단을 실제 서버 계정으로 확인한다. 타인·타지역·유효/만료 기관·게스트 범위는 실제 API에서 거부를 검증한다. 계정 준비 직후 DB 권한 시험을 수행하고 각 API 완료 전 해당 기능 권한 시험, #31 전체 인수 전에 누락을 해소한다. |

PR #78 병합 전에는 BE2 공동 Schema 검토와 위 후속 배정 확인, PR #35 문서 통합 순서, 필수 리뷰·CI를 확인한다. 이 결정은 #3 종료/병합, #4 구현 착수, #74 상세 계약 완료, 실제 계정 구성·제품 연동 완료를 뜻하지 않는다.

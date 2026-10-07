# Discushion DB Schema / Issue #3

## #74 사진 저장 구조 검토 후속

`20261007104543_support_photo_cleanup_leases.sql`을 추가해 미완료/실패 파일의 삭제 예약, 업로드 권한 만료와 삭제 선점/복구 저장 구조, 파일당 현재 게시물 참조 UNIQUE를 준비했다. 로컬 카탈로그는 27테이블·180컬럼, DB 시험 128개·권한 모의9개·재실행 불변을 검증했다. 원격은 기존 이력4개/176컬럼이며 새 Migration은 미적용이다. [검토표 §10.10](../docs/api/Discushion_API_CONTRACT_검토표_2026-10-07.md)와 [API 정본 §13의 FE/BE2 확인 대기 계약](../docs/api/Discushion_API_SPEC_v2.md)을 먼저 확인한다. 아래 #3의 113개/176컬럼/이력4개 수치는 당시 검증 기록이다. 실제 Storage worker·최소 권한 서버 계정·FE 연동 완료를 뜻하지 않는다.

> **최신 기준 dad35c0 / MVP v10.2:** [승인된 MVP DB 반영](../docs/architecture/Discushion_Issue3_MVP_v10.2_반영.md)을 우선 확인한다. Migration 4개로 legacy 포함 27테이블·176컬럼을 구성했다. 기존 비밀번호·증빙 필수 의존 해제, Privy 회원 1:1/가입 완료, 사진 lifecycle·24시간 정리 후보/삭제 재시도 저장 구조를 사용자 승인 후 로컬 검증하고 지정 Supabase에 적용했다. 실제 Privy API/Storage worker나 BE2 검토·최소 권한 서버 역할 완료를 뜻하지 않는다.

Migration 도구는 사용자 승인에 따라 **Supabase CLI**를 사용한다. 검증한 CLI 버전은 **2.120.0**이며 다음처럼 버전을 고정해서 실행한다. 다른 Migration 도구와 중복 관리하지 않는다.

```powershell
npm exec --yes --package=supabase@2.120.0 -- supabase --version
npm exec --yes --package=supabase@2.120.0 -- supabase --help
```

`config.toml`은 공식 init으로 생성한 후 채택하지 않은 Auth/Storage/Data API 등의 로컬 서비스를 껐다. 사용자 생성 프로젝트 `pmhmgqpyvrbbseqelpze`에 link하고 두 Migration을 적용했다. 실제 원격 PostgreSQL 17.11과 Schema/접근 제한을 확인했다. Java 원격 JDBC와 서버 앱 최소 권한 역할은 미검증/협의 대기다. [후속 결과](../docs/collaboration/backend-db-connection-decisions.md)를 참조한다.

## 현재 상태와 팀 공유

Docker/WSL2 시작 실패 후 사용자 승인으로 **native PostgreSQL 17.11**을 준비해 실제 Schema·Migration을 검증했다. BIOS/Windows 기능은 변경하지 않았다. 로컬 DB는 SCRAM 인증과 `127.0.0.1:55432`만 사용하고 외부에 포트/방화벽을 열지 않는다.

팀이 공유하는 것은 PC의 DB 주소가 아니라 **Migration·검증 파일**이다. 팀원은 같은 파일로 자신의 시험 DB를 만들거나, BE2와 확인한 공용 Supabase 개발 DB에 적용한다. 실 서비스 데이터와 분리해 잘못된 FK/중복/삭제/실패·재실행을 시험하는 공간이 로컬 DB다.

- [구현·검증 결과 및 BE2 체크리스트](../docs/architecture/Discushion_Issue3_DB_구현검증_2026-10-07.md)
- [27개 핵심 테이블 Migration](migrations/20261006182228_mvp_schema.sql)

일반 PostgreSQL의 테이블·키·RLS·Migration을 검증한 것이며 Supabase Auth/Storage/Data API·실제 원격 TLS/pooler·FE/BE 통합 시험은 아니다. Supabase Auth를 자동 채택하지 않는다.

## 로컬 시험 재현

빈 native PostgreSQL 전용 DB `discushion_migration_test`를 `127.0.0.1:55432`에 준비한다. 실제 비밀번호는 외부 환경변수로 전달하며 저장소에 기록하지 않는다.

```powershell
# 저장소 루트. 두 환경변수를 실제 로컬 시험 값으로 먼저 설정한다.
# DISCUSHION_TEST_DB_URL: postgres URI (127.0.0.1:55432/discushion_migration_test)
# DISCUSHION_TEST_PSQL: PostgreSQL 설치의 psql.exe 절대 경로
& ./supabase/tests/run-schema-tests.ps1 `
  -DbUrl $env:DISCUSHION_TEST_DB_URL -PsqlPath $env:DISCUSHION_TEST_PSQL
node ./supabase/tests/check-erd-schema.mjs
```

첫 스크립트는 CLI Migration 적용 → DB 시험 113개 → 동일 Migration 재실행의 Schema/이력/기존 데이터 불변을 확인한다. 외부 주소·다른 포트/DB는 연결 전에 거부하고 reset/drop을 하지 않는다. 합성 fixture는 ROLLBACK으로 제거하지만 sequence 발급 상태는 바뀔 수 있으므로 실 데이터 DB에 실행하지 않는다.

공백 보완은 [후속 Migration](migrations/20261007011459_harden_nonblank_inputs.sql)으로 적용하고 원본 이력은 보존했다. 연결·공백 처리의 선택 이유와 CLI 로그인 상태는 [결정 기록](../docs/collaboration/backend-db-connection-decisions.md)에 남겼다.

카탈로그 검사는 변경 전 ERD를 보존하면서 승인된 v10.2 NULL 변경/신규 컬럼을 함께 반영해 27테이블·176컬럼 타입/NULL/PK/UNIQUE/FK, 단일 참조 47개·복합 FK 8개·Privy UNIQUE를 대조한다. API 역할 모의 검사는 별도 빈 `discushion_roles_test`와 [SQL 파일](tests/api-role-isolation.sql)을 사용한다. 기존 anon/authenticated/service_role이 있는 환경에는 실행하지 않는다.

BE2에게 비공개 `discushion` Schema, Schema-qualified JDBC 쿼리, 실제 DB 버전·기존 상태·실행 역할과 원격 advisor 검토를 요청한다. 실제 팀 확인·사용자 변경 검토 후 commit/PR을 진행하고, 확인 전 #3을 종료하지 않는다.

비밀번호·API key·실제 증빙을 저장소에 넣지 않는다. Data API 노출 스키마의 테이블은 RLS/권한을 함께 검토하고, Spring JDBC 방식이라는 이유로 브라우저 직접 접근을 허용하지 않는다. 후속 사용자 요청으로 지정한 개발 프로젝트에만 remote `db push`를 수행했다. destructive reset은 하지 않는다.

## Java 17 검증

Java 17은 Temurin 17.0.20.1+1을 추가 설치했고 JDK 25는 보존했다. 설치 프로그램이 시스템 JAVA_HOME을 17로 등록한 사실을 확인했다. 현재 Codex 프로세스의 이전 환경과 혼동하지 않도록 검증 명령에서도 경로를 명시했다.

```powershell
# 저장소의 backend 디렉터리에서 실행. 다른 PC에서는 실제 설치 경로로 바꾼다.
$env:JAVA_HOME = 'C:\Program Files\Eclipse Adoptium\jdk-17.0.20.101-hotspot'
$env:DISCUSHION_TEST_JDBC_URL = 'jdbc:postgresql://127.0.0.1:55432/discushion_migration_test?sslmode=disable'
# DISCUSHION_TEST_DB_PASSWORD는 외부 환경변수로 공급
.\gradlew.bat --no-daemon test build --console=plain --rerun-tasks
```

2026-10-07 03:39 KST: 실제 JDBC 시험 4개 + 기존 Health/설정 시험 4개, 총 8개·skip/실패/오류 0 및 build/JAR 성공. JDBC 시험 환경변수가 없으면 해당 4개는 skip이며 통과로 기록하지 않는다. 환경변수 변경 후 기존 캐시 결과를 쓰지 않도록 rerun-tasks를 사용한다.

비공개 Schema의 모든 테이블은 RLS 활성화, PUBLIC/API 역할 접근 차단 상태다. 서버 앱 역할/RLS 방식은 BE2/#4와 확인한다. 이미 적용/공유된 Migration은 덮어 고치지 않으며 CLI 이력 관리와 checksum 변경 감지를 같은 기능으로 가정하지 않는다.

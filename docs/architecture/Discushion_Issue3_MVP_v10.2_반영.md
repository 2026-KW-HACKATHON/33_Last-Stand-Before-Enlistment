# Issue #3 — 승인된 MVP v10.2 DB 반영

기준: 최신 back/develop `dad35c0` / PR #76 / PRD·기능명세서 v10.2. 사용자 요청: 최신 MVP를 #3에 반영. 변경 전 Migration 두 개는 이미 원격에 적용되어 수정하지 않는다.

## 1. 확정 범위 반영

- 새 CLI 생성 Migration `20261007021128_align_mvp_auth_and_demo_prerequisites.sql`: users.password_hash, institution_credentials.request_id의 NOT NULL만 해제했다. 관련 PK/FK/UNIQUE/유효기간과 private Schema/RLS는 유지한다.
- 자체 비밀번호·이메일 코드 발급이나 증빙 신청 기능은 새로 구현하지 않는다. 기존 해시/신청/증빙 데이터와 참조를 임의 삭제하지 않는다. 해당 테이블은 legacy-only로 DB 주석을 달았다.
- 이웃 완료 지역은 기존 nullable source_request_id로 신청 없이 설정 가능하다. 기관 자격도 이제 신청 없이 저장할 수 있다. 실제 시연 계정/자격 생성은 #75이며 공개 사용자 승인 API는 제공하지 않는다.
- 기존 85개 SQL 시험에 신규 8개를 추가해 localhost에서 93개 통과. 비밀번호 없는 회원, 신청 없는 기관 자격 저장, 회원/기관/지역/기간 거부, 기존 신청 자격 보존을 검증한다. 재실행 시 Schema/이력/기존 표식 데이터 불변도 확인했다.
- 기존 ERD 변경 전 Mermaid + 두 컬럼 NULL 변경을 명시적으로 overlay해 대조한다. 검사가 통과해도 Privy/사진 상세 계약까지 완료됐다고 해석하지 않는다.

이 절의 최초 호환 보완 시점은 legacy 5개 포함 27테이블·166컬럼이었다. 이후 사용자 승인과 §2의 Migration으로 27테이블·176컬럼이 됐고 지정 원격에도 적용했다. 구 ERD의 27개를 신규 MVP의 필수 테이블 수로 선언하지 않는다.

2026-10-07 11:14 KST 후속 검증: 카탈로그 27테이블·166컬럼·단일 FK 47/복합 8 대조 오류 0, 격리 API 역할 검사 9개 통과. Java 17의 기존 local JDBC/Health/설정 및 기존 원격 SELECT-only 시험을 포함한 test/build 성공: 11개, skip/실패/오류 0. 원격 읽기 시험은 이전 두 Migration 상태의 연결 유지 확인이지 새 Migration 원격 적용 검사가 아니다. 기존 Migration 두 개는 해시가 바뀌지 않았고 git diff --check 오류 없음.

## 2. 사용자 승인과 구현

2026-10-07 사용자(BE1)가 “승인할께”라고 답해 아래 DB 설계안을 승인했다. 이 승인은 BE2/FE의 실제 검토, API 경로/DTO/토큰·SDK·파일 전송 계약 합의나 #4/#13/#30 구현 완료를 의미하지 않는다. CLI 생성 `20261007023149_add_privy_registration_and_media_lifecycle.sql`로 구현했으며 두 후속 Migration 모두 로컬 검증 후 사용자 지정 Supabase에 적용했다.

| 주제 | 제안 | 구현 전 확인/경계 |
| --- | --- | --- |
| Privy 회원 연결 | provider 식별자와 로컬 회원 1:1 유일 연결 | 식별자는 검증된 토큰/Privy 서버 응답에서만 얻음. 이메일만 같다고 기존 회원 자동 연결하지 않음. 토큰 원문·자체 OTP 저장 없음 |
| 가입 상태 | 별도 가입 완료 시각으로 미완료/완료 구분 | 동의·프로필·활동 지역이 모두 원자 확정된 뒤 완료. 실제 token 직접 검증/세션 교환은 #4/#74 |
| 사진 미완료 | 업로드 완료 시각 + 24시간, 미연결이면 정리 대상 | 이 시작시각은 제안이지 기존 확정 정책이 아님. 진행 중 업로드/연결 경합·공개 시점·정리 실행은 #13/#30 |
| 사진 lifecycle | 연결 완료 파일 보호, 삭제 대기·재시도 상태 | DB 참조 처리/Storage 삭제의 원자성·실패 보상 협의 필요. 상태 컬럼만으로 Storage 삭제가 구현된 것이 아님 |

기존 데이터의 Privy/가입 완료/업로드 완료를 추측해 backfill하지 않는다. 실 API·FE 계약이나 Storage 정리 작업은 이 #3에서 구현하지 않는다. 아래는 승인된 DB 저장 계약이다.

### 회원 컬럼

| 컬럼 | 타입·NULL | 의미·제약 |
| --- | --- | --- |
| users.privy_user_id | TEXT NULL, UNIQUE | 검증된 Privy subject와 로컬 회원 1:1. 빈/공백-only 거부. NULL은 legacy 미연결. 동일 이메일만으로 자동 연결 금지 |
| users.registration_completed_at | TIMESTAMPTZ NULL | 가입 미완료/legacy는 NULL. 완료 시 Privy ID 필수, created_at 이후. 동의·프로필·활동 지역 확정은 #7 서비스 트랜잭션 |

기존 users.email UNIQUE/NOT NULL과 프로필·지역·동의 관계를 유지한다. 가입 중 users 행만 존재할 수 있으며 프로필이 있다는 이유만으로 완료 시각을 자동 기록하지 않는다. 새 회원은 검증된 provider 식별자를 저장해야 하며, legacy NULL 허용이 인증 없는 가입 API를 허용하는 뜻은 아니다. JWT/OTP/refresh 원문이나 Privy 비밀번호를 저장하지 않는다.

### 파일 컬럼

| 컬럼 | 타입·NULL | 의미·제약 |
| --- | --- | --- |
| lifecycle_status | TEXT NOT NULL, 기본 LEGACY | LEGACY·UPLOADING·UNLINKED·LINKED·DELETE_PENDING·DELETED |
| uploaded_at | TIMESTAMPTZ NULL | 서버가 확인한 업로드 완료 시각, created_at 이후. 미연결 POST_PHOTO의 24시간 기산점 |
| linked_at | TIMESTAMPTZ NULL | 연결 완료 시각, uploaded_at 이후 |
| delete_requested_at | TIMESTAMPTZ NULL | 삭제 예약 시각, 업로드/연결 시각 이후 |
| deleted_at | TIMESTAMPTZ NULL | Storage 삭제 성공 확인 시각, 삭제 예약 이후 |
| deletion_attempts | INTEGER NOT NULL, 기본 0 | 삭제 시도 횟수 0 이상. 삭제 대기/완료만 비제로 허용 |
| next_delete_attempt_at | TIMESTAMPTZ NULL | 삭제 대기에서만 예약 가능, 요청 시각 이후 |
| last_delete_error_code | TEXT NULL | 삭제 대기에서만 비밀값 제거한 오류 분류 저장. 빈/공백-only 거부 |

| 상태 | 필요한 시각 | 정리/실행 경계 |
| --- | --- | --- |
| LEGACY | lifecycle 시각 없음 | 기존 파일 시각을 모름. 자동 정리 제외, 안전한 별도 이관 확인 전 보존 |
| UPLOADING | created_at만 | 업로드 미완료. 이번 승인 기준인 완료 후 24시간 정리에서 제외. 미완료 전송 자체의 별도 TTL은 확정하지 않음 |
| UNLINKED | uploaded_at, linked_at 없음 | POST_PHOTO이고 uploaded_at <= now() - interval '24 hours'면 후보 |
| LINKED | uploaded_at + linked_at | 24시간 만료 후보에서 제외. 실제 참조 존재·소유권은 서비스에서 재검증 |
| DELETE_PENDING | uploaded_at + delete_requested_at | 시도 횟수·재시도 시각·비밀 제거 오류 코드 저장. 이전 linked_at 보존 가능 |
| DELETED | uploaded_at + delete_requested_at + deleted_at | 성공 후 재시도 예약/오류 제거. 이전 linked_at/시도 횟수 보존 가능 |

UNLINKED 만료와 DELETE_PENDING 재시도는 POST_PHOTO 한정 부분 인덱스로 지원한다. 프로필/legacy 증빙의 보관·공개 정책을 사진 결정에서 유추해 적용하지 않는다. worker가 DB 상태만 보고 외부 Storage 파일을 삭제하게 만들어서는 안 된다.

### #13/#16/#30 실행 계약

1. 최종 연결과 정리 예약은 같은 media_files 행을 잠그고 상태·소유권·실제 참조를 다시 확인한다. 시간 경과만으로 LINKED 파일을 지우지 않는다. 여러 행은 일관된 ID 순서로 잠근다.
2. 파일/게시물 연결과 상태 갱신은 같은 DB 트랜잭션이다. 사진 제거·교체/게시물 삭제는 참조 처리와 DELETE_PENDING 예약을 원자 기록한다. active 참조가 있으면 정리하지 않는다. 삭제 투표/활동/채택 이력은 사진 파일과 별개로 유지한다.
3. 외부 Storage 삭제는 DB와 원자 트랜잭션이 아니므로 예약 이후 실행한다. 실패/이미 없음/재시도 응답 처리는 #13에서 검증하고 성공이 확인될 때만 DELETED로 기록한다. exact backoff/실행 간격/동시 worker claim은 #13/#30에서 정한다.
4. 임시 파일 공개 시점, bytes 환산·형식 검증·업로드 API/DTO·최종 사진 개수와 총량·Privy 토큰 검증/세션 방식은 아직 상세 계약 대기다. 새 상태 컬럼이 이것들을 대신 구현하지 않는다.

위 잠금/참조 보호는 후속 서비스 작업 계약이며 trigger/worker 구현 완료를 주장하지 않는다. DB CHECK는 각 행의 상태/시각/카운터 형태만 강제한다.

## 3. 실행 결과

- 로컬 DB 시험 113개(기존 93 + Privy 7/파일 13) 통과. Migration 재실행 Schema/이력/기존 표식 불변.
- 카탈로그 27테이블·176컬럼·단일 FK 47/복합 FK 8 + Privy UNIQUE 일치, 오류 0. API 역할 모의 9개 통과.
- 2026-10-07 11:35 KST Java 17 test/build 성공. 로컬 JDBC와 실제 Supabase Spring JDBC 읽기 검증 포함. 원격은 새로운 176컬럼·이력 4개를 검사한다.
- 원격 적용 전 users/media/credentials 행 수 각각 0, 이후에도 0. 기존 데이터 삭제·seed·역할·Vault 변경 없음. 원격 RLS 27개 유지, password/request nullable 및 새 제약 확인.
- 실제 Privy OTP 로그인/토큰 검증, Supabase Storage 업로드·삭제/worker 실행과 FE 연동은 미실행. #4/#6~8/#13/#16/#30/#31에서 검증한다.

## 4. 다음 단계와 PR 조건

승인된 DB 설계는 #74 계약과 ERD에 연결해 기록하고 후속 Migration으로 검증한다. 기존 적용 파일을 수정하지 않는다. 서버 최소 권한 로그인 역할은 아직 없고 postgres는 초기 연결 검증 계정이며 RLS를 우회한다. 계정/권한 구성과 BE2 검토는 별도 확인해야 한다.

사용자의 commit/PR 요청에 따라 back/feature/3-schema에서 back/develop 대상 검토용 Draft PR을 준비한다. BE2 공동 검토와 최소 권한 서버 계정은 미완료로 명시하며 #3 전체 완료나 자동 병합으로 처리하지 않는다. 별도 기존 ERD 표현 PR #35는 변경하지 않고 중복/병합 순서를 PR에 안내한다. #4 구현이나 #75 시연 seed는 아직 시작하지 않는다.

Supabase 스킬의 적용 이력 보존·실제 DB 검증·RLS/비밀값 보호와 PostgreSQL 지침을 따랐다. 로컬 시험 결과를 원격/Privy/Storage 실제 연동 결과로 대체하지 않는다.

-- v10.2의 확정 범위만 반영. Privy 회원 연결/사진 lifecycle 상세는 #74 합의 전 추가하지 않는다.
-- 이미 적용한 Migration과 기존 데이터/증빙 관계는 보존한다. DROP TABLE/COLUMN/데이터 삭제 없음.
-- MVP 인증은 Privy OTP다. 기존 해시는 이력 호환용이며 새 회원의 필수 입력이 아니다.
alter table discushion.users alter column password_hash drop not null;
comment on column discushion.users.password_hash is
  'Legacy-only: v10.2 MVP uses Privy OTP. Not a required registration field or an enabled password login contract.';

-- 시연 기관 자격은 증빙 신청 없이 준비할 수 있다.
-- 요청을 제공한 기존 행은 단일/복합 FK로 회원·기관·지역 일치를 계속 강제한다.
alter table discushion.institution_credentials alter column request_id drop not null;
comment on column discushion.institution_credentials.request_id is
  'Optional legacy application reference. v10.2 demo credentials do not require a submitted application.';

-- 제외된 신청/이메일 코드 테이블을 지워 기존 감사/참조 데이터를 유실하지 않는다.
-- 유지된 테이블은 현재 MVP 구현 대상이 아니며 공개 API 권한은 계속 차단된다.
comment on table discushion.email_verifications is 'Legacy v10.1 schema: own email code issuance is excluded from v10.2 MVP.';
comment on table discushion.neighbor_verification_requests is 'Legacy v10.1 schema: evidence applications are excluded from v10.2 MVP.';
comment on table discushion.neighbor_verification_evidences is 'Legacy v10.1 schema: evidence collection is excluded from v10.2 MVP.';
comment on table discushion.institution_verification_requests is 'Legacy v10.1 schema: evidence applications are excluded from v10.2 MVP.';
comment on table discushion.institution_verification_evidences is 'Legacy v10.1 schema: evidence collection is excluded from v10.2 MVP.';

-- 사용자(BE1) 승인: Privy 회원 1:1, 가입 완료 시각, 업로드 완료 +24h 미연결 정리,
-- 연결 파일 보호, 삭제 대기/재시도. 기존 행의 가입/업로드 완료를 추측해 backfill하지 않는다.
alter table discushion.users
  add column privy_user_id text,
  add column registration_completed_at timestamptz,
  add constraint users_privy_user_id_key unique(privy_user_id),
  add constraint users_privy_id_nonblank check(privy_user_id is null or privy_user_id ~ '[^[:space:]]'),
  add constraint users_registration_completion check(registration_completed_at is null or
    (privy_user_id is not null and registration_completed_at >= created_at));
comment on column discushion.users.privy_user_id is
  'Verified Privy subject only; unique 1:1 local member mapping. NULL preserves legacy rows. Never auto-link by email.';
comment on column discushion.users.registration_completed_at is
  'NULL means registration incomplete or unmigrated legacy. Set only after agreements/profile/activity region commit atomically.';

alter table discushion.media_files
  add column lifecycle_status text not null default 'LEGACY',
  add column uploaded_at timestamptz,
  add column linked_at timestamptz,
  add column delete_requested_at timestamptz,
  add column deleted_at timestamptz,
  add column deletion_attempts integer not null default 0,
  add column next_delete_attempt_at timestamptz,
  add column last_delete_error_code text,
  add constraint media_lifecycle_status check(lifecycle_status in
    ('LEGACY','UPLOADING','UNLINKED','LINKED','DELETE_PENDING','DELETED')),
  add constraint media_deletion_attempts check(deletion_attempts >= 0),
  add constraint media_delete_error_nonblank check(last_delete_error_code is null or
    last_delete_error_code ~ '[^[:space:]]'),
  add constraint media_lifecycle_shape check(case lifecycle_status
    when 'LEGACY' then uploaded_at is null and linked_at is null and delete_requested_at is null and deleted_at is null
    when 'UPLOADING' then uploaded_at is null and linked_at is null and delete_requested_at is null and deleted_at is null
    when 'UNLINKED' then uploaded_at is not null and linked_at is null and delete_requested_at is null and deleted_at is null
    when 'LINKED' then uploaded_at is not null and linked_at is not null and delete_requested_at is null and deleted_at is null
    when 'DELETE_PENDING' then uploaded_at is not null and delete_requested_at is not null and deleted_at is null
    when 'DELETED' then uploaded_at is not null and delete_requested_at is not null and deleted_at is not null
    else false end),
  add constraint media_retry_shape check(
    (lifecycle_status='DELETE_PENDING' or (next_delete_attempt_at is null and last_delete_error_code is null))
    and (lifecycle_status in ('DELETE_PENDING','DELETED') or deletion_attempts=0)),
  add constraint media_lifecycle_times check(
    (uploaded_at is null or uploaded_at >= created_at)
    and (linked_at is null or (uploaded_at is not null and linked_at >= uploaded_at))
    and (delete_requested_at is null or (uploaded_at is not null and delete_requested_at >= uploaded_at
      and (linked_at is null or delete_requested_at >= linked_at)))
    and (deleted_at is null or (delete_requested_at is not null and deleted_at >= delete_requested_at))
    and (next_delete_attempt_at is null or (delete_requested_at is not null and next_delete_attempt_at >= delete_requested_at)));

-- 실제 연결/Storage 삭제 실행은 #13/#16/#30. DB 상태만으로 외부 object를 삭제하지 않는다.
-- LEGACY는 실제 업로드 시각 불명으로 24시간 자동 정리 대상에서 제외한다.
create index ix_media_unlinked_expiry on discushion.media_files(uploaded_at,id)
  where purpose='POST_PHOTO' and lifecycle_status='UNLINKED';
create index ix_media_delete_retry on discushion.media_files(next_delete_attempt_at,id)
  where purpose='POST_PHOTO' and lifecycle_status='DELETE_PENDING';
comment on column discushion.media_files.uploaded_at is
  'Server-confirmed upload completion. POST_PHOTO UNLINKED cleanup eligible after 24 elapsed hours; unknown LEGACY never auto-expires.';
comment on column discushion.media_files.lifecycle_status is
  'LEGACY/UPLOADING/UNLINKED/LINKED/DELETE_PENDING/DELETED. Link/cleanup transactions lock the same file row and recheck references; LINKED is not an expiry candidate.';
comment on column discushion.media_files.last_delete_error_code is
  'Sanitized retry classification only; never persist access tokens, signed URLs, passwords or raw secret-bearing errors.';

-- 기존 private schema/RLS 유지. 새 컬럼 때문에 API 역할의 접근을 허용하지 않는다.

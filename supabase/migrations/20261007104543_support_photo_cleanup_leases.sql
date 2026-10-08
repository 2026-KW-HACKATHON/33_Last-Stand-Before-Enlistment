-- #74 BE1 review: preserve applied migrations and legacy data.
-- This stores cleanup intent/leases; it does not implement Storage deletion.
do $preflight$
begin
  if exists(select 1 from discushion.post_photos group by file_id having count(*) > 1) then
    raise exception 'Resolve existing multiple-post file references with BE1 before migration; no automatic deletion';
  end if;
end $preflight$;

alter table discushion.media_files
  add column upload_authorization_expires_at timestamptz,
  add column deletion_claim_token uuid,
  add column deletion_claimed_at timestamptz,
  add column deletion_claim_expires_at timestamptz,
  drop constraint media_lifecycle_shape,
  drop constraint media_lifecycle_times;

alter table discushion.media_files
  add constraint media_lifecycle_shape check(case lifecycle_status
    when 'LEGACY' then uploaded_at is null and linked_at is null and delete_requested_at is null and deleted_at is null
    when 'UPLOADING' then uploaded_at is null and linked_at is null and delete_requested_at is null and deleted_at is null
    when 'UNLINKED' then uploaded_at is not null and linked_at is null and delete_requested_at is null and deleted_at is null
    when 'LINKED' then uploaded_at is not null and linked_at is not null and delete_requested_at is null and deleted_at is null
    when 'DELETE_PENDING' then delete_requested_at is not null and deleted_at is null
    when 'DELETED' then delete_requested_at is not null and deleted_at is not null
    else false end),
  add constraint media_lifecycle_times check(
    (uploaded_at is null or uploaded_at >= created_at)
    and (linked_at is null or (uploaded_at is not null and linked_at >= uploaded_at))
    and (delete_requested_at is null or (delete_requested_at >= created_at
      and (uploaded_at is null or delete_requested_at >= uploaded_at)
      and (linked_at is null or delete_requested_at >= linked_at)))
    and (deleted_at is null or (delete_requested_at is not null and deleted_at >= delete_requested_at))
    and (next_delete_attempt_at is null or (delete_requested_at is not null and next_delete_attempt_at >= delete_requested_at))),
  add constraint media_upload_authorization_time check(upload_authorization_expires_at is null
    or upload_authorization_expires_at >= created_at),
  add constraint media_deletion_claim_shape check(
    (deletion_claim_token is null and deletion_claimed_at is null and deletion_claim_expires_at is null)
    or (deletion_claim_token is not null and deletion_claimed_at is not null and deletion_claim_expires_at is not null
      and lifecycle_status='DELETE_PENDING' and deletion_claimed_at >= delete_requested_at
      and deletion_claim_expires_at > deletion_claimed_at)),
  add constraint media_deleted_after_upload_authorization check(lifecycle_status <> 'DELETED'
    or upload_authorization_expires_at is null or deleted_at >= upload_authorization_expires_at);

alter table discushion.post_photos
  add constraint post_photos_file_id_key unique(file_id);

create index ix_media_uploading_expiry on discushion.media_files(created_at,id)
  where purpose='POST_PHOTO' and lifecycle_status='UPLOADING';
create index ix_media_delete_claim_expiry on discushion.media_files(deletion_claim_expires_at,id)
  where purpose='POST_PHOTO' and lifecycle_status='DELETE_PENDING' and deletion_claim_token is not null;

comment on column discushion.media_files.upload_authorization_expires_at is
  'Maximum expiration of every issued upload capability. Persist before returning it; never shorten on retry. No token or signed URL stored. NULL preserves legacy/never-issued reservations.';
comment on column discushion.media_files.deletion_claim_token is
  'Opaque worker lease/fencing token. Claim atomically under the common file row lock; every result update must match the current token. Never a Storage credential.';
comment on column discushion.media_files.deletion_claim_expires_at is
  'Expired worker claims can be atomically reclaimed with a new token. Clear all three claim fields on completion/retry release. Lease duration is configured by #13/#30.';
comment on column discushion.media_files.deleted_at is
  'Final absence verification after all upload capabilities expire AND in-flight upload risk is resolved. A single Storage DELETE response is insufficient; #13/#30 verify late recreation.';
comment on column discushion.media_files.uploaded_at is
  'First successful server content verification; never reset by repeated completion/grant calls. UNLINKED expiry uses this time +24h; UPLOADING without it uses original created_at +24h.';
comment on constraint post_photos_file_id_key on discushion.post_photos is
  'One file may have at most one current post reference across all posts. Ownership/purpose/state checks and common media row locking remain required in #13/#14/#16.';

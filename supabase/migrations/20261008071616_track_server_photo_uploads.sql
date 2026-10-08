-- #13 agreed relay contract. Preserve every existing direct/legacy reservation.
alter table discushion.media_files
  add column upload_transport text not null default 'DIRECT_UNCONFIRMED',
  add column upload_attempt_id uuid,
  add column upload_attempt_status text,
  add column upload_attempt_started_at timestamptz,
  add column upload_attempt_finished_at timestamptz,
  drop constraint media_deleted_after_upload_authorization;

alter table discushion.media_files
  add constraint media_upload_transport check(upload_transport in ('DIRECT_UNCONFIRMED','SERVER_RELAY')),
  add constraint media_relay_shape check(upload_transport <> 'SERVER_RELAY'
    or (purpose='POST_PHOTO' and upload_authorization_expires_at is not null)),
  add constraint media_upload_attempt_shape check(
    (upload_attempt_id is null and upload_attempt_status is null
      and upload_attempt_started_at is null and upload_attempt_finished_at is null)
    or (upload_transport='SERVER_RELAY' and upload_attempt_id is not null
      and upload_attempt_started_at is not null and upload_attempt_started_at >= created_at
      and upload_attempt_status is not null and (
        (upload_attempt_status in ('RUNNING','UNKNOWN') and upload_attempt_finished_at is null)
        or (upload_attempt_status='ACKNOWLEDGED' and upload_attempt_finished_at is not null
          and upload_attempt_finished_at >= upload_attempt_started_at)))),
  add constraint media_deleted_after_upload_authorization check(lifecycle_status <> 'DELETED'
    or upload_transport='SERVER_RELAY' or upload_authorization_expires_at is null
    or deleted_at >= upload_authorization_expires_at),
  add constraint media_relay_deleted_safe check(upload_transport <> 'SERVER_RELAY' or lifecycle_status <> 'DELETED'
    or upload_attempt_status is null or upload_attempt_status='ACKNOWLEDGED');

-- Never erase an uncertain attempt or relabel an old signed-URL file as relay-safe.
create function discushion.guard_photo_upload_attempt() returns trigger
language plpgsql security invoker set search_path=pg_catalog as $guard$
begin
  if TG_OP='INSERT' then
    if NEW.upload_attempt_id is not null then
      raise exception 'An upload attempt must start after reservation commit' using errcode='23514';
    end if;
  else
    if NEW.upload_transport is distinct from OLD.upload_transport then
      raise exception 'Upload transport is immutable' using errcode='23514';
    end if;
    if OLD.upload_attempt_id is not null then
      if NEW.upload_attempt_id is distinct from OLD.upload_attempt_id
        or NEW.upload_attempt_started_at is distinct from OLD.upload_attempt_started_at
        or (OLD.upload_attempt_status in ('UNKNOWN','ACKNOWLEDGED')
          and (NEW.upload_attempt_status is distinct from OLD.upload_attempt_status
            or NEW.upload_attempt_finished_at is distinct from OLD.upload_attempt_finished_at)) then
        raise exception 'Upload attempt evidence is immutable' using errcode='23514';
      end if;
    elsif NEW.upload_attempt_id is not null then
      if OLD.lifecycle_status <> 'UPLOADING' or NEW.lifecycle_status <> 'UPLOADING'
        or NEW.upload_transport <> 'SERVER_RELAY' or NEW.upload_attempt_status <> 'RUNNING'
        or NEW.upload_attempt_started_at >= OLD.upload_authorization_expires_at then
        raise exception 'Upload attempt cannot start in this state' using errcode='23514';
      end if;
    end if;
  end if;
  return NEW;
end $guard$;
revoke all on function discushion.guard_photo_upload_attempt() from public;
grant execute on function discushion.guard_photo_upload_attempt() to discushion_server;
create trigger guard_photo_upload_attempt before insert or update on discushion.media_files
for each row execute function discushion.guard_photo_upload_attempt();

comment on column discushion.media_files.upload_transport is
  '#13 immutable transport. Existing DIRECT_UNCONFIRMED rows retain signed upload risk; only new SERVER_RELAY rows may use the durable server-write barrier.';
comment on column discushion.media_files.upload_attempt_status is
  'At most one external write per key. RUNNING commits before dispatch; ACKNOWLEDGED records confirmed successful completion; UNKNOWN never expires by time/lease and blocks final deletion.';
comment on column discushion.media_files.upload_authorization_expires_at is
  'DIRECT: conservative maximum issued capability expiry. SERVER_RELAY: DB-clock admission deadline, checked under the file lock; cancellation blocks new writes immediately.';

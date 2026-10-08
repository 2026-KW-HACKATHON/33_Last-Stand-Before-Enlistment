-- #29: the Spring runtime may create and cancel adoption relations.
-- Institution, member, region and post-state checks remain in Spring.
do $adoption_runtime_permissions$
begin
  if not exists(select 1 from pg_roles where rolname='discushion_server' and not (rolsuper or rolbypassrls)) then
    raise exception 'Safe runtime role must be provisioned first';
  end if;
  if not exists(select 1 from pg_class
      where oid='discushion.institution_agenda_adoptions'::regclass and relrowsecurity) then
    raise exception 'RLS must already be enabled: institution_agenda_adoptions';
  end if;

  if not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='institution_agenda_adoptions' and policyname='server_select'
      and roles=array['discushion_server']::name[] and cmd='SELECT'
      and permissive='PERMISSIVE' and qual='true' and with_check is null) then
    raise exception 'Expected existing institution_agenda_adoptions server SELECT policy';
  end if;

  if exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='institution_agenda_adoptions' and policyname='server_insert') then
    if not exists(select 1 from pg_policies where schemaname='discushion'
        and tablename='institution_agenda_adoptions' and policyname='server_insert'
        and roles=array['discushion_server']::name[] and cmd='INSERT'
        and permissive='PERMISSIVE' and qual is null and with_check='true') then
      raise exception 'Unexpected existing institution_agenda_adoptions server_insert policy';
    end if;
  else
    create policy server_insert on discushion.institution_agenda_adoptions
      for insert to discushion_server with check (true);
  end if;

  if exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='institution_agenda_adoptions' and policyname='server_update') then
    if not exists(select 1 from pg_policies where schemaname='discushion'
        and tablename='institution_agenda_adoptions' and policyname='server_update'
        and roles=array['discushion_server']::name[] and cmd='UPDATE'
        and permissive='PERMISSIVE' and qual='true' and with_check='true') then
      raise exception 'Unexpected existing institution_agenda_adoptions server_update policy';
    end if;
  else
    create policy server_update on discushion.institution_agenda_adoptions
      for update to discushion_server using (true) with check (true);
  end if;
end $adoption_runtime_permissions$;

grant select,insert,update on table discushion.institution_agenda_adoptions to discushion_server;
revoke delete,truncate,references,trigger on table discushion.institution_agenda_adoptions from discushion_server;

comment on policy server_insert on discushion.institution_agenda_adoptions is
  'Application-level member, institution, region and public-post checks are enforced by Spring.';
comment on policy server_update on discushion.institution_agenda_adoptions is
  'Spring may preserve cancellation history by setting canceled_at and canceled_by_user_id; physical deletion is not granted.';

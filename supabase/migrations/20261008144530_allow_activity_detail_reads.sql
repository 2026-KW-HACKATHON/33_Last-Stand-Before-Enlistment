-- #15: read activity source fields in the canonical post detail; no new write privileges.
do $activity_detail_permissions$
begin
  if not exists(select 1 from pg_roles where rolname='discushion_server' and not (rolsuper or rolbypassrls)) then
    raise exception 'Safe runtime role must be provisioned first';
  end if;
  if not exists(select 1 from pg_class where oid='discushion.activity_post_details'::regclass and relrowsecurity) then
    raise exception 'Activity RLS must already be enabled';
  end if;
  grant select on table discushion.activity_post_details to discushion_server;
  if exists(select 1 from pg_policies where schemaname='discushion' and tablename='activity_post_details' and policyname='server_select') then
    if not exists(select 1 from pg_policies where schemaname='discushion' and tablename='activity_post_details'
        and policyname='server_select' and roles=array['discushion_server']::name[] and cmd='SELECT'
        and permissive='PERMISSIVE' and qual='true' and with_check is null) then
      raise exception 'Unexpected existing activity server SELECT policy';
    end if;
  else
    create policy server_select on discushion.activity_post_details for select to discushion_server using (true);
  end if;
end $activity_detail_permissions$;
comment on policy server_select on discushion.activity_post_details is
  'Server reads activity details after Spring validates member or post-scoped guest access.';

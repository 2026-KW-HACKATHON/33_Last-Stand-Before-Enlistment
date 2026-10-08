-- #16: server activity updates after Spring author/region validation; no DELETE or client access.
do $activity_update_permissions$
begin
  if not exists(select 1 from pg_roles where rolname='discushion_server' and not (rolsuper or rolbypassrls))
      or not exists(select 1 from pg_class where oid='discushion.activity_post_details'::regclass and relrowsecurity) then
    raise exception 'Safe server role and enabled activity RLS required';
  end if;
  grant update on table discushion.activity_post_details to discushion_server;
  if exists(select 1 from pg_policies where schemaname='discushion' and tablename='activity_post_details' and policyname='server_update') then
    if not exists(select 1 from pg_policies where schemaname='discushion' and tablename='activity_post_details'
        and policyname='server_update' and cmd='UPDATE' and roles=array['discushion_server']::name[]
        and permissive='PERMISSIVE' and qual='true' and with_check='true') then
      raise exception 'Unexpected activity server UPDATE policy';
    end if;
  else
    create policy server_update on discushion.activity_post_details for update to discushion_server using(true) with check(true);
  end if;
end $activity_update_permissions$;

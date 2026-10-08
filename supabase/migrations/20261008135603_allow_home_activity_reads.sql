-- #18: activity state is read from the existing post source. No new write permission.
do $home$
begin
  if not exists(select 1 from pg_roles where rolname='discushion_server' and not (rolsuper or rolbypassrls))
    or not exists(select 1 from pg_class where oid='discushion.activity_post_details'::regclass and relrowsecurity) then
    raise exception 'Safe runtime role and activity RLS required';
  end if;
  if exists(select 1 from pg_policies where schemaname='discushion' and tablename='activity_post_details' and policyname='server_select') then
    if not exists(select 1 from pg_policies where schemaname='discushion' and tablename='activity_post_details' and policyname='server_select'
      and roles=array['discushion_server']::name[] and cmd='SELECT' and permissive='PERMISSIVE' and qual='true' and with_check is null) then
      raise exception 'Unexpected activity read policy';
    end if;
  else
    create policy server_select on discushion.activity_post_details for select to discushion_server using (true);
  end if;
end $home$;
grant select on table discushion.activity_post_details to discushion_server;

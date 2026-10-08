-- #28: the runtime may read current institution-to-agenda relations for the
-- authenticated officer's own institution. Spring still enforces membership,
-- credential validity, region and public-post rules.
do $officer_agenda_permissions$
begin
  if not exists(select 1 from pg_roles where rolname='discushion_server' and not (rolsuper or rolbypassrls)) then
    raise exception 'Safe runtime role must be provisioned first';
  end if;
  if not exists(select 1 from pg_class
      where oid='discushion.institution_agenda_adoptions'::regclass and relrowsecurity) then
    raise exception 'RLS must already be enabled: institution_agenda_adoptions';
  end if;
  if exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='institution_agenda_adoptions' and policyname='server_select') then
    if not exists(select 1 from pg_policies where schemaname='discushion'
        and tablename='institution_agenda_adoptions' and policyname='server_select'
        and roles=array['discushion_server']::name[] and cmd='SELECT'
        and permissive='PERMISSIVE' and qual='true' and with_check is null) then
      raise exception 'Unexpected existing institution_agenda_adoptions server_select policy';
    end if;
  else
    create policy server_select on discushion.institution_agenda_adoptions
      for select to discushion_server using (true);
  end if;
end $officer_agenda_permissions$;

grant select on table discushion.institution_agenda_adoptions to discushion_server;

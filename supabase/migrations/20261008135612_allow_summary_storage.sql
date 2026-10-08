-- #20: revision-fenced summary persistence; never grants delete or public access.
do $summary$
declare operation text;
begin
  if not exists(select 1 from pg_roles where rolname='discushion_server' and not (rolsuper or rolbypassrls))
    or not exists(select 1 from pg_class where oid='discushion.ai_agenda_summaries'::regclass and relrowsecurity) then
    raise exception 'Safe runtime role and summary RLS required';
  end if;
  foreach operation in array array['SELECT','INSERT','UPDATE'] loop
    if exists(select 1 from pg_policies where schemaname='discushion' and tablename='ai_agenda_summaries' and policyname='server_'||lower(operation)) then
      if not exists(select 1 from pg_policies where schemaname='discushion' and tablename='ai_agenda_summaries' and policyname='server_'||lower(operation)
        and roles=array['discushion_server']::name[] and cmd=operation and permissive='PERMISSIVE'
        and qual is not distinct from (case when operation in ('SELECT','UPDATE') then 'true' end)
        and with_check is not distinct from (case when operation in ('INSERT','UPDATE') then 'true' end)) then
        raise exception 'Unexpected summary policy %',operation;
      end if;
    elsif operation='SELECT' then
      create policy server_select on discushion.ai_agenda_summaries for select to discushion_server using(true);
    elsif operation='INSERT' then
      create policy server_insert on discushion.ai_agenda_summaries for insert to discushion_server with check(true);
    else
      create policy server_update on discushion.ai_agenda_summaries for update to discushion_server using(true) with check(true);
    end if;
  end loop;
end $summary$;
grant select,insert,update on table discushion.ai_agenda_summaries to discushion_server;

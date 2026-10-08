-- #25: the Spring server records current votes and reads public aggregates.
-- No delete, DDL, sequence or client-role access is added.
do $vote_permissions$
declare operation text; policy_name text; clause text;
begin
  if not exists(select 1 from pg_roles where rolname='discushion_server') then
    raise exception 'discushion_server role must be created by #30 permissions migration first';
  end if;
  if not exists(select 1 from pg_class where oid='discushion.vote_selections'::regclass and relrowsecurity) then
    raise exception 'RLS must already be enabled on discushion.vote_selections';
  end if;
  if not exists(select 1 from pg_class where oid='discushion.poll_options'::regclass and relrowsecurity) then
    raise exception 'RLS must already be enabled on discushion.poll_options';
  end if;
  foreach operation in array array['SELECT','INSERT','UPDATE'] loop
    execute format('grant %s on table discushion.vote_selections to discushion_server',operation);
    policy_name='server_'||lower(operation);
    clause=case operation
      when 'SELECT' then 'using (true)'
      when 'INSERT' then 'with check (true)'
      when 'UPDATE' then 'using (true) with check (true)' end;
    if exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='vote_selections' and policyname=policy_name) then
      if not exists(select 1 from pg_policies where schemaname='discushion'
        and tablename='vote_selections' and policyname=policy_name
        and roles=array['discushion_server']::name[] and cmd=operation
        and permissive='PERMISSIVE'
        and (qual is not distinct from case when operation<>'INSERT' then 'true' end)
        and (with_check is not distinct from case when operation in ('INSERT','UPDATE') then 'true' end)) then
        raise exception 'Unexpected existing vote policy: %',policy_name;
      end if;
    else
      execute format('create policy %I on discushion.vote_selections for %s to discushion_server %s',
        policy_name,operation,clause);
    end if;
  end loop;
  grant select on table discushion.poll_options to discushion_server;
  if exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='poll_options' and policyname='server_select') then
    if not exists(select 1 from pg_policies where schemaname='discushion'
        and tablename='poll_options' and policyname='server_select'
        and roles=array['discushion_server']::name[] and cmd='SELECT'
        and permissive='PERMISSIVE' and qual='true' and with_check is null) then
      raise exception 'Unexpected existing poll_options server_select policy';
    end if;
  else
    execute 'create policy server_select on discushion.poll_options for select to discushion_server using (true)';
  end if;
end $vote_permissions$;

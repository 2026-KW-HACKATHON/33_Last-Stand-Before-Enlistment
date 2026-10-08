-- #24: additive runtime permissions only. Actual shared DB rollout is BE2-coordinated.
-- Spring still enforces author/region/share access. Evaluation INSERT/UPDATE/DELETE only; existing SELECT and comment immutability remain.
do $evaluation_permissions$
declare
  entry record;
  operation text;
  policy_name text;
  clause text;
begin
  if not exists(select 1 from pg_roles where rolname='discushion_server' and not (rolsuper or rolbypassrls)) then
    raise exception 'Safe runtime role must be provisioned first';
  end if;
  for entry in select * from (values
    ('comment_evaluations',array['INSERT','UPDATE','DELETE'])
  ) as allowed(table_name,operations) loop
    if not exists(select 1 from pg_class where oid=format('discushion.%I',entry.table_name)::regclass and relrowsecurity) then
      raise exception 'RLS must be enabled: %',entry.table_name;
    end if;
    foreach operation in array entry.operations loop
      execute format('grant %s on table discushion.%I to discushion_server',operation,entry.table_name);
      policy_name='server_'||lower(operation);
      clause=case when operation='DELETE' then 'using (true)' when operation='UPDATE' then 'using (true) with check (true)' else 'with check (true)' end;
      if exists(select 1 from pg_policies where schemaname='discushion' and tablename=entry.table_name and policyname=policy_name) then
        if not exists(select 1 from pg_policies where schemaname='discushion' and tablename=entry.table_name
          and policyname=policy_name and roles=array['discushion_server']::name[] and cmd=operation and permissive='PERMISSIVE'
          and (qual is not distinct from case when operation in ('UPDATE','DELETE') then 'true' end)
          and (with_check is not distinct from case when operation in ('INSERT','UPDATE') then 'true' end)) then
          raise exception 'Unexpected existing evaluation policy: %',policy_name;
        end if;
      else
        execute format('create policy %I on discushion.%I for %s to discushion_server %s',policy_name,entry.table_name,operation,clause);
      end if;
    end loop;
  end loop;
end $evaluation_permissions$;

-- #30 phase 1: permissions for code merged through back/develop c7aee0b.
-- NOLOGIN and no password: activate only after BE1 review and shared DB approval.
-- User/region/ownership checks remain in Spring, not these server-wide policies.
do $role$
begin
  if not exists (select 1 from pg_roles where rolname='discushion_server') then
    create role discushion_server nologin nosuperuser nocreatedb nocreaterole
      noinherit noreplication nobypassrls;
  end if;
  if exists (select 1 from pg_roles where rolname='discushion_server'
      and (rolsuper or rolcreatedb or rolcreaterole or rolinherit or rolreplication or rolbypassrls))
    or exists (select 1 from pg_auth_members
      where member=(select oid from pg_roles where rolname='discushion_server'))
    or exists (select 1 from pg_namespace
      where nspowner=(select oid from pg_roles where rolname='discushion_server'))
    or exists (select 1 from pg_class
      where relowner=(select oid from pg_roles where rolname='discushion_server')) then
    raise exception 'Unsafe existing discushion_server role; reconcile before migration';
  end if;
  if has_schema_privilege('discushion_server','discushion','CREATE')
    or has_schema_privilege('discushion_server','public','CREATE') then
    raise exception 'Effective schema CREATE permission must be removed before provisioning';
  end if;
end $role$;

grant usage on schema discushion to discushion_server;
alter role discushion_server set search_path = pg_catalog;

-- Explicit per-table operations only. No default privileges, sequence grants,
-- future feature writes, evidence, seed/qualification writes or history deletion.
do $permissions$
declare
  entry record;
  operation text;
  policy_name text;
  clause text;
begin
  for entry in select * from (values
    ('regions', array['SELECT']),
    ('neighbor_verified_regions', array['SELECT']),
    ('institution_credentials', array['SELECT']),
    ('users', array['SELECT','INSERT','UPDATE']),
    ('profiles', array['SELECT','INSERT','UPDATE']),
    ('profile_attributes', array['SELECT','INSERT','DELETE']),
    ('user_agreements', array['SELECT','INSERT','UPDATE']),
    ('media_files', array['SELECT','INSERT','UPDATE']),
    -- PostgreSQL requires UPDATE permission for SELECT FOR UPDATE in #13.
    ('posts', array['SELECT','UPDATE']),
    ('polls', array['SELECT','UPDATE']),
    ('post_photos', array['SELECT','INSERT','UPDATE','DELETE'])
  ) as allowed(table_name,operations) loop
    if not exists(select 1 from pg_class
      where oid=format('discushion.%I',entry.table_name)::regclass and relrowsecurity) then
      raise exception 'RLS must already be enabled: %',entry.table_name;
    end if;
    foreach operation in array entry.operations loop
      execute format('grant %s on table discushion.%I to discushion_server',operation,entry.table_name);
      policy_name='server_'||lower(operation);
      clause=case operation
        when 'SELECT' then 'using (true)'
        when 'INSERT' then 'with check (true)'
        when 'UPDATE' then 'using (true) with check (true)'
        when 'DELETE' then 'using (true)' end;
      -- Existing policies are never silently replaced during a re-run.
      if exists(select 1 from pg_policies where schemaname='discushion'
        and tablename=entry.table_name and policyname=policy_name) then
        if not exists(select 1 from pg_policies where schemaname='discushion'
          and tablename=entry.table_name and policyname=policy_name
          and roles=array['discushion_server']::name[] and cmd=operation
          and permissive='PERMISSIVE'
          and (qual is not distinct from case when operation<>'INSERT' then 'true' end)
          and (with_check is not distinct from case when operation in ('INSERT','UPDATE') then 'true' end)) then
          raise exception 'Unexpected existing policy on %: %',entry.table_name,policy_name;
        end if;
      else
        execute format('create policy %I on discushion.%I for %s to discushion_server %s',
          policy_name,entry.table_name,operation,clause);
      end if;
    end loop;
  end loop;
end $permissions$;

comment on role discushion_server is
  '#30 phase-1 Spring runtime role. Separate from migration/seed admin. Activate LOGIN/password outside Git after approval. No member-level RLS isolation.';

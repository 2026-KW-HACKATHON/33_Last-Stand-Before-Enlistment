-- #14: allow the server to insert shared posts and their type-specific source rows.
-- Keep user/region eligibility in Spring; no DELETE, sequence or client-role access is added.
do $post_creation_permissions$
declare entry record; policy_name text;
begin
  if not exists(select 1 from pg_roles where rolname='discushion_server' and not (rolsuper or rolbypassrls)) then
    raise exception 'Safe runtime role must be provisioned first';
  end if;
  for entry in select * from (values
    ('posts'),('activity_post_details'),('polls'),('poll_options')
  ) as targets(table_name) loop
    if not exists(select 1 from pg_class where oid=format('discushion.%I',entry.table_name)::regclass and relrowsecurity) then
      raise exception 'RLS must already be enabled: %',entry.table_name;
    end if;
    execute format('grant insert on table discushion.%I to discushion_server',entry.table_name);
    policy_name='server_insert';
    if exists(select 1 from pg_policies where schemaname='discushion'
        and tablename=entry.table_name and policyname=policy_name) then
      if not exists(select 1 from pg_policies where schemaname='discushion'
          and tablename=entry.table_name and policyname=policy_name
          and roles=array['discushion_server']::name[] and cmd='INSERT'
          and permissive='PERMISSIVE' and qual is null and with_check='true') then
        raise exception 'Unexpected existing server INSERT policy on %',entry.table_name;
      end if;
    else
      execute format('create policy server_insert on discushion.%I for insert to discushion_server with check (true)',entry.table_name);
    end if;
  end loop;
end $post_creation_permissions$;

comment on policy server_insert on discushion.posts is
  'Server may create shared post rows; Spring checks member and verified-region eligibility.';
comment on policy server_insert on discushion.polls is
  'Server may create type-specific vote rows inside the post transaction.';
comment on policy server_insert on discushion.poll_options is
  'Server may create poll options inside the post transaction.';
comment on policy server_insert on discushion.activity_post_details is
  'Server may create type-specific activity rows inside the post transaction.';

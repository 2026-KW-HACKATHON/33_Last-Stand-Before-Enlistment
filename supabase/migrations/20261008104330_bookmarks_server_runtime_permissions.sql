-- #26: the Spring runtime may read and mutate current bookmark relations only.
-- User ownership and published-post checks remain in Spring; no public role access.
do $bookmark_permissions$
begin
  if not exists(select 1 from pg_class
      where oid='discushion.bookmarks'::regclass and relrowsecurity) then
    raise exception 'RLS must already be enabled: bookmarks';
  end if;
  if exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='bookmarks' and policyname in ('server_select','server_insert','server_delete')) then
    raise exception 'Unexpected pre-existing bookmarks server policy';
  end if;
end $bookmark_permissions$;

grant select,insert,delete on table discushion.bookmarks to discushion_server;
create policy server_select on discushion.bookmarks for select to discushion_server using (true);
create policy server_insert on discushion.bookmarks for insert to discushion_server with check (true);
create policy server_delete on discushion.bookmarks for delete to discushion_server using (true);

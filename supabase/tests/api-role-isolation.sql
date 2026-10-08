-- 일반 PostgreSQL에서 Supabase 역할 이름만 모의해 접근 차단 분기를 시험한다.
-- 실제 Supabase Auth/Data API 통합 시험은 아니다. 역할/Schema는 전부 ROLLBACK.
begin;
do $guard$
begin
  if inet_server_addr()<>'127.0.0.1'::inet or current_database()<>'discushion_roles_test' then
    raise exception '시험 전용 localhost roles DB에서만 허용';
  end if;
  if exists(select 1 from pg_roles where rolname in ('anon','authenticated','service_role')) then
    raise exception '기존 역할을 변경하지 않습니다. 별도 빈 native cluster에서 실행하세요.';
  end if;
end $guard$;
create role anon nologin;
create role authenticated nologin;
create role service_role nologin bypassrls;
\ir ../migrations/20261006182228_mvp_schema.sql
\ir ../migrations/20261007011459_harden_nonblank_inputs.sql
\ir ../migrations/20261007021128_align_mvp_auth_and_demo_prerequisites.sql
\ir ../migrations/20261007023149_add_privy_registration_and_media_lifecycle.sql
\ir ../migrations/20261007104543_support_photo_cleanup_leases.sql
\ir ../migrations/20261007202633_configure_server_runtime_permissions.sql
\ir ../migrations/20261008070000_allow_comment_reads_and_creation.sql
\ir ../migrations/20261008071616_track_server_photo_uploads.sql
\ir ../migrations/20261008080000_allow_reaction_reads_and_transitions.sql
\ir ../migrations/20261008090000_allow_comment_evaluation_transitions.sql
\ir ../migrations/20261008100000_allow_vote_selection_writes.sql
\ir ../migrations/20261008104330_bookmarks_server_runtime_permissions.sql
\ir ../migrations/20261008113000_allow_officer_agenda_reads.sql
\ir ../migrations/20261008130000_allow_post_creation.sql
\ir ../migrations/20261008144530_allow_activity_detail_reads.sql
do $checks$
declare r text;
begin
  foreach r in array array['anon','authenticated','service_role'] loop
    if has_schema_privilege(r,'discushion','USAGE') then raise exception 'Schema 접근 허용: %',r; end if;
    if exists(select 1 from pg_class where relnamespace='discushion'::regnamespace and relkind='r'
      and has_table_privilege(r,oid,'SELECT,INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')) then
      raise exception '테이블 접근 허용: %',r;
    end if;
    if exists(select 1 from pg_class where relnamespace='discushion'::regnamespace
      and case when relkind='S' then has_sequence_privilege(r,oid,'USAGE,SELECT,UPDATE') else false end)
      then raise exception '시퀀스 접근 허용: %',r; end if;
  end loop;
end $checks$;
do $server_checks$
begin
  if not has_table_privilege('discushion_server','discushion.activity_post_details','SELECT')
    or not has_table_privilege('discushion_server','discushion.activity_post_details','INSERT')
    or has_table_privilege('discushion_server','discushion.activity_post_details','UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')
    or not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='activity_post_details' and policyname='server_insert'
      and roles=array['discushion_server']::name[] and cmd='INSERT' and with_check='true') then
    raise exception 'Unexpected activity creation permissions';
  end if;
  if not exists(select 1 from pg_policies where schemaname='discushion' and tablename='activity_post_details'
      and policyname='server_select' and roles=array['discushion_server']::name[] and cmd='SELECT' and qual='true') then
    raise exception 'Missing activity detail server SELECT policy';
  end if;
  if not has_table_privilege('discushion_server','discushion.posts','INSERT')
    or has_table_privilege('discushion_server','discushion.posts','DELETE,TRUNCATE,REFERENCES,TRIGGER')
    or not has_table_privilege('discushion_server','discushion.polls','INSERT')
    or has_table_privilege('discushion_server','discushion.polls','DELETE,TRUNCATE,REFERENCES,TRIGGER')
    or not has_table_privilege('discushion_server','discushion.poll_options','INSERT')
    or has_table_privilege('discushion_server','discushion.poll_options','UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER') then
    raise exception 'Unexpected post creation grants for discushion_server';
  end if;
  if not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='posts' and policyname='server_insert'
      and roles=array['discushion_server']::name[] and cmd='INSERT' and with_check='true')
    or not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='polls' and policyname='server_insert'
      and roles=array['discushion_server']::name[] and cmd='INSERT' and with_check='true')
    or not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='poll_options' and policyname='server_insert'
      and roles=array['discushion_server']::name[] and cmd='INSERT' and with_check='true') then
    raise exception 'Missing or unexpected post creation server RLS policies';
  end if;
  if has_table_privilege('anon','discushion.posts','INSERT')
    or has_table_privilege('authenticated','discushion.posts','INSERT')
    or has_table_privilege('service_role','discushion.posts','INSERT')
    or has_table_privilege('anon','discushion.polls','INSERT')
    or has_table_privilege('authenticated','discushion.polls','INSERT')
    or has_table_privilege('service_role','discushion.polls','INSERT')
    or has_table_privilege('anon','discushion.poll_options','INSERT')
    or has_table_privilege('authenticated','discushion.poll_options','INSERT')
    or has_table_privilege('service_role','discushion.poll_options','INSERT') then
    raise exception 'API roles must not create posts or poll rows';
  end if;
  if not has_table_privilege('discushion_server','discushion.vote_selections','SELECT')
    or not has_table_privilege('discushion_server','discushion.vote_selections','INSERT')
    or not has_table_privilege('discushion_server','discushion.vote_selections','UPDATE')
    or has_table_privilege('discushion_server','discushion.vote_selections','DELETE')
    or has_table_privilege('discushion_server','discushion.vote_selections','TRUNCATE')
    or has_table_privilege('discushion_server','discushion.vote_selections','REFERENCES')
    or has_table_privilege('discushion_server','discushion.vote_selections','TRIGGER') then
    raise exception 'Unexpected vote_selections privileges for discushion_server';
  end if;
  if not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='vote_selections' and policyname='server_select'
      and roles=array['discushion_server']::name[] and cmd='SELECT' and qual='true')
    or not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='vote_selections' and policyname='server_insert'
      and roles=array['discushion_server']::name[] and cmd='INSERT' and with_check='true')
    or not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='vote_selections' and policyname='server_update'
      and roles=array['discushion_server']::name[] and cmd='UPDATE' and qual='true' and with_check='true')
    then raise exception 'Missing or unexpected vote_selections server RLS policies';
  end if;
  if has_table_privilege('anon','discushion.vote_selections','SELECT,INSERT,UPDATE,DELETE')
    or has_table_privilege('authenticated','discushion.vote_selections','SELECT,INSERT,UPDATE,DELETE')
    or has_table_privilege('service_role','discushion.vote_selections','SELECT,INSERT,UPDATE,DELETE')
    or has_table_privilege('anon','discushion.poll_options','SELECT')
    or has_table_privilege('authenticated','discushion.poll_options','SELECT')
    or has_table_privilege('service_role','discushion.poll_options','SELECT') then
    raise exception 'Client/API role must not access vote tables/options';
  end if;
  if not has_table_privilege('discushion_server','discushion.poll_options','SELECT')
    or not has_table_privilege('discushion_server','discushion.poll_options','INSERT')
    or has_table_privilege('discushion_server','discushion.poll_options','UPDATE')
    or has_table_privilege('discushion_server','discushion.poll_options','DELETE')
    or not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='poll_options' and policyname='server_select'
      and roles=array['discushion_server']::name[] and cmd='SELECT' and qual='true') then
    raise exception 'Unexpected poll_options server access';
  end if;
  if not has_table_privilege('discushion_server','discushion.bookmarks','SELECT')
    or not has_table_privilege('discushion_server','discushion.bookmarks','INSERT')
    or not has_table_privilege('discushion_server','discushion.bookmarks','DELETE')
    or has_table_privilege('discushion_server','discushion.bookmarks','UPDATE,TRUNCATE,REFERENCES,TRIGGER')
    or has_table_privilege('anon','discushion.bookmarks','SELECT,INSERT,UPDATE,DELETE')
    or has_table_privilege('authenticated','discushion.bookmarks','SELECT,INSERT,UPDATE,DELETE')
    or has_table_privilege('service_role','discushion.bookmarks','SELECT,INSERT,UPDATE,DELETE') then
    raise exception 'Unexpected bookmarks access for server or API roles';
  end if;
  if not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='bookmarks' and policyname='server_select'
      and roles=array['discushion_server']::name[] and cmd='SELECT' and qual='true')
    or not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='bookmarks' and policyname='server_insert'
      and roles=array['discushion_server']::name[] and cmd='INSERT' and with_check='true')
    or not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='bookmarks' and policyname='server_delete'
      and roles=array['discushion_server']::name[] and cmd='DELETE' and qual='true') then
    raise exception 'Missing or unexpected bookmarks server RLS policies';
  end if;
  if not has_table_privilege('discushion_server','discushion.institution_agenda_adoptions','SELECT')
    or has_table_privilege('discushion_server','discushion.institution_agenda_adoptions','INSERT,UPDATE,DELETE,TRUNCATE,REFERENCES,TRIGGER')
    or has_table_privilege('anon','discushion.institution_agenda_adoptions','SELECT,INSERT,UPDATE,DELETE')
    or has_table_privilege('authenticated','discushion.institution_agenda_adoptions','SELECT,INSERT,UPDATE,DELETE')
    or has_table_privilege('service_role','discushion.institution_agenda_adoptions','SELECT,INSERT,UPDATE,DELETE') then
    raise exception 'Unexpected institution_agenda_adoptions access for server or API roles';
  end if;
  if not exists(select 1 from pg_policies where schemaname='discushion'
      and tablename='institution_agenda_adoptions' and policyname='server_select'
      and roles=array['discushion_server']::name[] and cmd='SELECT' and qual='true') then
    raise exception 'Missing institution_agenda_adoptions server SELECT policy';
  end if;
end $server_checks$;
select 'PASS: 3 API role names x schema/table/sequence denial = 9 checks' as result;
rollback;

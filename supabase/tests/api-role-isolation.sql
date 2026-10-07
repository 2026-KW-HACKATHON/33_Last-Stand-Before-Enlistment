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
select 'PASS: 3 API role names x schema/table/sequence denial = 9 checks' as result;
rollback;

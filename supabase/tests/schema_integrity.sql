-- PostgreSQL 실제 DB 제약 시험. 합성 데이터와 임시 helper는 마지막 ROLLBACK으로 제거.
-- 서버/DB 대상은 run-schema-tests.ps1에서 localhost로 제한한다.
begin;
set local search_path = discushion, pg_catalog;
do $guard$
declare t record; n bigint;
begin
  if inet_server_addr() <> '127.0.0.1'::inet or current_database() not in
    ('discushion_schema_draft','discushion_migration_test','discushion_roles_test') then
    raise exception '시험 전용 localhost DB에서만 실행할 수 있습니다';
  end if;
  for t in select tablename from pg_tables where schemaname='discushion' loop
    execute format('select count(*) from discushion.%I',t.tablename) into n;
    if n<>0 then raise exception '시험 전에 비어 있어야 하는 테이블: %',t.tablename; end if;
  end loop;
end $guard$;
create temporary table schema_test_results(case_id text primary key) on commit drop;
create function pg_temp.assert_ok(label text, condition boolean) returns void language plpgsql as $$
begin
  if condition is distinct from true then raise exception 'FAIL: %',label; end if;
  insert into schema_test_results values(label);
end $$;
create function pg_temp.assert_error(label text, statement text, expected_state text,
  expected_constraint text default null) returns void language plpgsql as $$
declare actual_constraint text;
begin
  begin
    execute statement;
  exception when others then
    get stacked diagnostics actual_constraint = constraint_name;
    if sqlstate <> expected_state then raise exception 'FAIL: %, expected %, got %: %',label,expected_state,sqlstate,sqlerrm; end if;
    if expected_constraint is not null and actual_constraint<>expected_constraint then
      raise exception 'FAIL: %, expected constraint %, got %',label,expected_constraint,actual_constraint;
    end if;
    insert into schema_test_results values(label);
    return;
  end;
  raise exception 'FAIL: %, statement unexpectedly succeeded',label;
end $$;

insert into regions(id,name) overriding system value values(1,'합성 지역'),(2,'합성 지역'),(3,'지역3'),(4,'지역4');
insert into users(id,email,password_hash,email_verified_at,created_at,updated_at) overriding system value
 values(1,'one@example.invalid','not-a-real-hash',now(),now(),now()),
       (2,'two@example.invalid','not-a-real-hash',now(),now(),now()),
       (3,'three@example.invalid','not-a-real-hash',now(),now(),now());
insert into profiles(user_id,nickname,activity_region_id,updated_at) values(1,'합성회원1',1,now()),(2,'합성회원2',2,now());
insert into institutions(id,name,created_at) overriding system value values(1,'합성 기관',now()),(2,'합성 기관',now());
insert into media_files(id,owner_user_id,storage_key,original_name,mime_type,size_bytes,purpose,created_at) overriding system value
 values(1,1,'synthetic/photo','photo.png','image/png',100,'POST_PHOTO',now()),
       (2,1,'synthetic/evidence','evidence.pdf','application/pdf',100,'INSTITUTION_EVIDENCE',now());
insert into neighbor_verification_requests(id,user_id,region_id,status,submitted_at,completed_at) overriding system value
 values(1,1,1,'COMPLETED',now(),now()),(2,2,2,'COMPLETED',now(),now()),(3,1,1,'RECEIVED',now(),null);
insert into neighbor_verified_regions values(1,1,1,now());
insert into institution_verification_requests(id,user_id,institution_id,submitted_institution_name,department_name,
 position_name,applicant_name,work_email,phone_number,responsible_region_id,status,submitted_at,completed_at) overriding system value
 values(1,1,1,'합성','합성','합성','테스트1','one@example.invalid','000',1,'COMPLETED',now(),now()),
       (2,2,2,'합성','합성','합성','테스트2','two@example.invalid','000',1,'COMPLETED',now(),now());
insert into institution_credentials(id,request_id,user_id,institution_id,responsible_region_id,completed_at,valid_until) overriding system value
 values(1,1,1,1,1,now(),now()+interval '1 year'),(2,2,2,2,1,now(),now()+interval '1 year');
insert into posts(id,author_user_id,region_id,type,topic,title,content,status,content_revision,created_at,updated_at) overriding system value
 values(1,1,1,'LOCAL_AGENDA','OTHER','합성 안건','합성 본문','PUBLISHED',1,now(),now()),
       (2,1,1,'VOTE','OTHER','합성 투표1','합성 본문','PUBLISHED',1,now(),now()),
       (3,2,2,'VOTE','OTHER','합성 투표2','합성 본문','PUBLISHED',1,now(),now()),
       (4,1,1,'LOCAL_ACTIVITY','OTHER','합성 활동','합성 본문','PUBLISHED',1,now(),now()),
       (5,2,2,'LOCAL_AGENDA','OTHER','합성 안건2','합성 본문','PUBLISHED',1,now(),now());
insert into activity_post_details values(4,'합성 출처','합성 일정','합성 장소','SCHEDULED',null);
insert into polls(id,post_id,question,ends_at) overriding system value values(1,2,'질문1',now()+interval '1 day'),(2,3,'질문2',now()+interval '1 day');
insert into poll_options(id,poll_id,content,sort_order) overriding system value values(1,1,'선택1',0),(2,1,'선택2',1),(3,2,'선택1',0),(4,2,'선택2',1);
insert into comments(id,post_id,parent_comment_id,author_user_id,author_kind,content,created_at) overriding system value
 values(1,1,null,1,'MEMBER','합성 댓글',now()),(2,1,1,2,'MEMBER','합성 답글',now()),(3,5,null,null,'GUEST','합성 게스트',now());

-- 명시적 fixture ID 뒤 자동 발급이 PK 충돌을 일으켜 다른 제약 시험을 거짓 통과시키지 않도록 조정.
do $sequences$
declare t record; next_id bigint;
begin
  for t in select table_name,column_name from information_schema.columns
    where table_schema='discushion' and is_identity='YES' loop
    execute format('select coalesce(max(%I),0)+1 from discushion.%I',t.column_name,t.table_name) into next_id;
    perform setval(pg_get_serial_sequence('discushion.'||t.table_name,t.column_name),next_id,false);
  end loop;
end $sequences$;

select pg_temp.assert_ok('CAT01_core_tables',(select count(*)=27 from pg_tables where schemaname='discushion'));
select pg_temp.assert_ok('CAT02_all_rls',(select bool_and(relrowsecurity) from pg_class where relnamespace='discushion'::regnamespace and relkind='r'));
select pg_temp.assert_ok('CAT03_no_cascade',(select count(*)=0 from pg_constraint where connamespace='discushion'::regnamespace and contype='f' and confdeltype='c'));
select pg_temp.assert_ok('CAT04_composite_fks',(select count(*)=8 from pg_constraint where connamespace='discushion'::regnamespace and contype='f' and cardinality(conkey)>1));
select pg_temp.assert_ok('CAT05_timestamptz',(select count(*)=0 from information_schema.columns where table_schema='discushion' and data_type='timestamp without time zone'));
select pg_temp.assert_ok('CAT06_unresolved_share_omitted',to_regclass('discushion.post_share_links') is null);
select pg_temp.assert_ok('DB03_names_not_identity',(select count(*)=2 from regions where name='합성 지역') and (select count(*)=2 from institutions where name='합성 기관'));
select pg_temp.assert_error('DB01_email_unique',$q$insert into users(email,password_hash,email_verified_at,created_at,updated_at) values('one@example.invalid','x',now(),now(),now())$q$,'23505','users_email_key');
select pg_temp.assert_error('DB01_nickname_unique',$q$insert into profiles(user_id,nickname,activity_region_id,updated_at) values(3,'합성회원1',1,now())$q$,'23505');
select pg_temp.assert_error('DB02_region_fk',$q$update profiles set activity_region_id=999 where user_id=1$q$,'23503');
select pg_temp.assert_error('DB02_required_null',$q$update profiles set activity_region_id=null where user_id=1$q$,'23502');
select pg_temp.assert_error('DB05_wrong_user_request',$q$insert into neighbor_verified_regions values(3,1,3,now())$q$,'23503');
select pg_temp.assert_error('DB05_wrong_region_request',$q$insert into neighbor_verified_regions values(1,2,3,now())$q$,'23503');
select pg_temp.assert_error('DB06_duplicate_completed',$q$insert into neighbor_verified_regions values(1,1,null,now())$q$,'23505');
insert into neighbor_verified_regions values(2,3,null,now()),(3,4,null,now());
select pg_temp.assert_ok('DB04_nullable_unique',(select count(*)=2 from neighbor_verified_regions where source_request_id is null));
select pg_temp.assert_ok('DB06_repeat_request_allowed',(select count(*)=2 from neighbor_verification_requests where user_id=1 and region_id=1));
select pg_temp.assert_error('DB07_credential_wrong_user',$q$update institution_credentials set user_id=3 where id=1$q$,'23503');
select pg_temp.assert_error('DB07_credential_wrong_region',$q$update institution_credentials set responsible_region_id=2 where id=1$q$,'23503');
select pg_temp.assert_error('DB07_credential_wrong_institution',$q$update institution_credentials set institution_id=2 where id=1$q$,'23503');
select pg_temp.assert_error('DB07_request_unique',$q$insert into institution_credentials(request_id,user_id,institution_id,responsible_region_id,completed_at,valid_until) values(1,1,1,1,now(),now()+interval '1 year')$q$,'23505','institution_credentials_request_id_key');
-- v10.2 호환: 자체 비밀번호와 증빙 신청 없이 저장 가능하며 기존 FK는 약화하지 않는다.
update users set password_hash=null where id=3;
select pg_temp.assert_ok('MVP01_password_not_required',(select password_hash is null from users where id=3));
select pg_temp.assert_error('MVP02_email_still_required',$q$update users set email=null where id=3$q$,'23502');
insert into institution_credentials(request_id,user_id,institution_id,responsible_region_id,completed_at,valid_until)
 values(null,3,1,2,now(),now()+interval '1 year');
select pg_temp.assert_ok('MVP03_requestless_credential',(select count(*)=1 from institution_credentials where user_id=3 and request_id is null));
select pg_temp.assert_error('MVP04_demo_user_fk',$q$insert into institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until) values(999,1,2,now(),now()+interval '1 year')$q$,'23503');
select pg_temp.assert_error('MVP05_demo_institution_fk',$q$insert into institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until) values(3,999,2,now(),now()+interval '1 year')$q$,'23503');
select pg_temp.assert_error('MVP06_demo_region_fk',$q$insert into institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until) values(3,1,999,now(),now()+interval '1 year')$q$,'23503');
select pg_temp.assert_error('MVP07_demo_valid_period',$q$insert into institution_credentials(user_id,institution_id,responsible_region_id,completed_at,valid_until) values(3,1,2,now(),now())$q$,'23514');
select pg_temp.assert_error('DB08_foreign_poll_option',$q$insert into vote_selections values(1,1,3,now(),now())$q$,'23503');
insert into vote_selections values(1,1,1,now(),now());
select pg_temp.assert_error('DB08_one_current_vote',$q$insert into vote_selections values(1,1,2,now(),now())$q$,'23505');
select pg_temp.assert_error('DB09_foreign_post_parent',$q$update comments set parent_comment_id=3 where id=2$q$,'23503');
select pg_temp.assert_error('DB09_foreign_post_target',$q$update comments set reply_to_comment_id=3,reply_to_display_name='게스트' where id=2$q$,'23503');
select pg_temp.assert_error('DB10_member_null_author',$q$update comments set author_user_id=null where id=1$q$,'23514');
select pg_temp.assert_error('DB10_guest_with_account',$q$update comments set author_user_id=1 where id=3$q$,'23514');
select pg_temp.assert_error('DB10_self_parent',$q$update comments set parent_comment_id=2 where id=2$q$,'23514');
select pg_temp.assert_error('DB10_self_target',$q$update comments set reply_to_comment_id=2,reply_to_display_name='자기' where id=2$q$,'23514');
select pg_temp.assert_error('DB10_unpaired_target',$q$update comments set reply_to_comment_id=1 where id=2$q$,'23514');
insert into post_reactions values(1,1,'EMPATHY',now()),(1,1,'NEEDED',now()),(1,1,'CURIOUS',now());
select pg_temp.assert_ok('DB11_independent_reactions',(select count(*)=3 from post_reactions where post_id=1 and user_id=1));
select pg_temp.assert_error('DB11_reaction_unique',$q$insert into post_reactions values(1,1,'EMPATHY',now())$q$,'23505');
insert into comment_evaluations values(1,1,'LIKE',now(),now());
select pg_temp.assert_error('DB12_mutually_exclusive',$q$insert into comment_evaluations values(1,1,'DISLIKE',now(),now())$q$,'23505');
update comment_evaluations set evaluation_type='DISLIKE',updated_at=now() where comment_id=1 and user_id=1;
select pg_temp.assert_ok('DB12_switch_allowed',(select evaluation_type='DISLIKE' from comment_evaluations where comment_id=1 and user_id=1));
insert into bookmarks values(1,1,now());
select pg_temp.assert_error('DB13_bookmark_unique',$q$insert into bookmarks values(1,1,now())$q$,'23505');
insert into post_photos(post_id,file_id,sort_order) values(1,1,0);
select pg_temp.assert_error('DB13_photo_order_unique',$q$insert into post_photos(post_id,file_id,sort_order) values(1,2,0)$q$,'23505');
select pg_temp.assert_error('DB13_photo_file_unique',$q$insert into post_photos(post_id,file_id,sort_order) values(1,1,1)$q$,'23505');
insert into institution_verification_evidences values(1,2,0);
select pg_temp.assert_error('DB13_evidence_order_unique',$q$insert into institution_verification_evidences values(1,1,0)$q$,'23505');
select pg_temp.assert_error('DB14_negative_file_size',$q$update media_files set size_bytes=0 where id=1$q$,'23514');
select pg_temp.assert_error('DB14_negative_order',$q$update poll_options set sort_order=-1 where id=1$q$,'23514');
select pg_temp.assert_error('DB14_unknown_code',$q$update post_reactions set reaction_type='UNKNOWN' where post_id=1$q$,'23514');
select pg_temp.assert_error('DB14_missing_activity_state',$q$update activity_post_details set activity_status=null where post_id=4$q$,'23502');
select pg_temp.assert_error('DB14_long_nickname',$q$update profiles set nickname='12345678901' where user_id=1$q$,'22001');
select pg_temp.assert_error('DB14_empty_nickname',$q$update profiles set nickname=' ' where user_id=1$q$,'23514');
select pg_temp.assert_error('DB14_empty_comment',$q$update comments set content=' ' where id=1$q$,'23514');
do $whitespace$
declare s text; n integer := 0;
begin
  foreach s in array array[E'\t',E'\n',U&'\3000',U&'\00A0',U&'\FEFF'] loop
    n := n+1;
    perform pg_temp.assert_error('WS_nickname_'||n,
      format('update discushion.profiles set nickname=%L where user_id=1',s),
      '23514','profiles_nickname_nonblank');
    perform pg_temp.assert_error('WS_comment_'||n,
      format('update discushion.comments set content=%L where id=1',s),
      '23514','comments_content_nonblank');
  end loop;
end $whitespace$;
update profiles set nickname=E'\tvalid\t' where user_id=1;
select pg_temp.assert_ok('WS_meaningful_nickname_allowed',(select nickname=E'\tvalid\t' from profiles where user_id=1));
update comments set content=E'\nmeaningful text\n' where id=1;
select pg_temp.assert_ok('WS_meaningful_multiline_allowed',(select content=E'\nmeaningful text\n' from comments where id=1));
select pg_temp.assert_error('DB14_unsafe_id',$q$insert into regions(id,name) overriding system value values(9007199254740992,'범위 밖')$q$,'23514');
select pg_temp.assert_error('DB14_zero_revision',$q$update posts set content_revision=0 where id=1$q$,'23514');
select pg_temp.assert_error('DB15_sent_requires_fields',$q$insert into email_verifications(email,purpose,delivery_status,requested_at) values('test@example.invalid','SIGN_UP','SENT',now())$q$,'23514');
select pg_temp.assert_error('DB14_attempt_limit',$q$insert into email_verifications(email,purpose,delivery_status,requested_at,attempt_count) values('test@example.invalid','SIGN_UP','PENDING',now(),6)$q$,'23514');
select pg_temp.assert_error('DB15_completion_requires_time',$q$update neighbor_verification_requests set completed_at=null where id=1$q$,'23514');
select pg_temp.assert_error('DB15_completion_requires_institution',$q$update institution_verification_requests set institution_id=null where id=1$q$,'23514');
select pg_temp.assert_error('DB15_credential_period',$q$update institution_credentials set valid_until=completed_at where id=1$q$,'23514');
select pg_temp.assert_error('DB15_required_agreement',$q$insert into user_agreements values(1,'TERMS_OF_SERVICE',false,'synthetic-v1',now())$q$,'23514');
insert into user_agreements values(1,'MARKETING',false,'synthetic-v1',now());
select pg_temp.assert_ok('DB15_optional_agreement_false',(select not agreed from user_agreements where user_id=1 and agreement_type='MARKETING'));
select pg_temp.assert_error('DB16_foreign_comment_event',$q$insert into activity_events(transition_key,user_id,post_id,comment_id,event_type,occurred_at) values('wrong-comment',1,1,3,'COMMENT_CREATED',now())$q$,'23503');
select pg_temp.assert_error('DB16_foreign_poll_event',$q$insert into activity_events(transition_key,user_id,post_id,poll_id,event_type,occurred_at) values('wrong-poll',1,1,1,'VOTE_PARTICIPATED',now())$q$,'23503');
select pg_temp.assert_error('DB16_event_shape',$q$insert into activity_events(transition_key,user_id,post_id,event_type,occurred_at) values('missing-target',1,1,'COMMENT_CREATED',now())$q$,'23514');
insert into activity_events(transition_key,user_id,post_id,event_type,occurred_at) values('bookmark-first',1,1,'BOOKMARK_REGISTERED',now());
select pg_temp.assert_error('DB16_transition_unique',$q$insert into activity_events(transition_key,user_id,post_id,event_type,occurred_at) values('bookmark-first',1,1,'BOOKMARK_REGISTERED',now())$q$,'23505');
delete from bookmarks where post_id=1 and user_id=1;
insert into bookmarks values(1,1,now());
insert into activity_events(transition_key,user_id,post_id,event_type,occurred_at) values('bookmark-again',1,1,'BOOKMARK_REGISTERED',now());
select pg_temp.assert_ok('REL01_current_vs_cumulative',(select count(*)=1 from bookmarks) and (select count(*)=2 from activity_events));
insert into institution_agenda_adoptions(id,post_id,institution_id,adopted_by_user_id,credential_id,adopted_at) overriding system value values(1,1,1,1,1,now());
select setval(pg_get_serial_sequence('discushion.institution_agenda_adoptions','id'),1000,false);
select pg_temp.assert_error('DB17_current_adoption_unique',$q$insert into institution_agenda_adoptions(post_id,institution_id,adopted_by_user_id,credential_id,adopted_at) values(1,1,1,1,now())$q$,'23505','uq_current_institution_agenda_adoption');
insert into institution_agenda_adoptions(id,post_id,institution_id,adopted_by_user_id,credential_id,adopted_at) overriding system value values(2,1,2,2,2,now());
select pg_temp.assert_error('DB18_wrong_credential',$q$insert into institution_agenda_adoptions(post_id,institution_id,adopted_by_user_id,credential_id,adopted_at) values(5,1,2,1,now())$q$,'23503');
select pg_temp.assert_error('DB18_cancel_pair',$q$update institution_agenda_adoptions set canceled_at=now() where id=1$q$,'23514');
select pg_temp.assert_error('DB18_cancel_time',$q$update institution_agenda_adoptions set canceled_at=adopted_at-interval '1 second',canceled_by_user_id=1 where id=1$q$,'23514');
update institution_agenda_adoptions set canceled_at=now(),canceled_by_user_id=1 where id=1;
insert into institution_agenda_adoptions(post_id,institution_id,adopted_by_user_id,credential_id,adopted_at) values(1,1,1,1,now());
select pg_temp.assert_ok('DB17_independent_history',(select count(*)=3 from institution_agenda_adoptions) and
 (select count(*)=2 from institution_agenda_adoptions where canceled_at is null));
select pg_temp.assert_error('DB19_success_requires_result',$q$insert into ai_agenda_summaries(post_id,source_revision,status,requested_at,updated_at) values(1,1,'SUCCEEDED',now(),now())$q$,'23514');
select pg_temp.assert_error('DB19_failure_no_result',$q$insert into ai_agenda_summaries(post_id,source_revision,status,summary,requested_at,updated_at) values(1,1,'FAILED','성공처럼 남은 내용',now(),now())$q$,'23514');
select pg_temp.assert_error('DB20_stable_parent',$q$delete from posts where id=1$q$,'23503');
select pg_temp.assert_error('DB20_deletion_state_pair',$q$update posts set status='DELETED' where id=1$q$,'23514');
-- 후속 #16 서비스가 수행할 트랜잭션의 저장 호환성 시험이지 API/자동 삭제 기능 시험이 아님.
insert into bookmarks values(2,1,now());
update posts set status='DELETED',deleted_at=now(),updated_at=now() where id=2;
delete from bookmarks where post_id=2;
select pg_temp.assert_ok('REL04_deleted_bookmark_removed',not exists(select 1 from bookmarks where post_id=2)
  and (select count(*)=1 from bookmarks where post_id=1));
select pg_temp.assert_ok('REL02_deleted_vote_retained',(select count(*)=1 from vote_selections where poll_id=1));
select pg_temp.assert_ok('REL03_events_preserved',(select count(*)=2 from activity_events));

create role discushion_schema_test_reader nologin;
select pg_temp.assert_ok('SEC01_schema_private',not has_schema_privilege('discushion_schema_test_reader','discushion','USAGE'));
select pg_temp.assert_error('SEC02_direct_access_denied',$q$set local role discushion_schema_test_reader; select * from discushion.users$q$,'42501');
grant usage on schema discushion to discushion_schema_test_reader;
grant select on all tables in schema discushion to discushion_schema_test_reader;
do $rls$
declare n bigint;
begin
  set local role discushion_schema_test_reader;
  select count(*) into n from discushion.users;
  reset role;
  perform pg_temp.assert_ok('SEC03_rls_default_deny',n=0);
end $rls$;
select pg_temp.assert_ok('MVP08_legacy_credentials_preserved',(select count(*)=2 from institution_credentials where request_id is not null));
-- 승인된 v10.2 Privy 연결/가입 상태 및 사진 lifecycle 저장 제약.
update users set privy_user_id='synthetic-privy-subject-3' where id=3;
select pg_temp.assert_ok('PRIVY01_mapping',(select privy_user_id='synthetic-privy-subject-3' from users where id=3));
select pg_temp.assert_error('PRIVY02_unique_subject',$q$update users set privy_user_id='synthetic-privy-subject-3' where id=2$q$,'23505','users_privy_user_id_key');
select pg_temp.assert_error('PRIVY03_blank_subject',$q$update users set privy_user_id=E'\t\n ' where id=3$q$,'23514');
select pg_temp.assert_error('PRIVY04_completion_needs_subject',$q$update users set registration_completed_at=now() where id=2$q$,'23514');
select pg_temp.assert_error('PRIVY05_completion_time',$q$update users set registration_completed_at=created_at-interval '1 second' where id=3$q$,'23514');
update users set registration_completed_at=now() where id=3;
select pg_temp.assert_ok('PRIVY06_completion',(select registration_completed_at is not null from users where id=3));
select pg_temp.assert_ok('PRIVY07_no_legacy_auto_link',(select privy_user_id is null and registration_completed_at is null from users where id=1));
select pg_temp.assert_ok('MEDIA01_legacy_not_expired',(select count(*)=2 from media_files where lifecycle_status='LEGACY' and uploaded_at is null));
insert into media_files(owner_user_id,storage_key,original_name,mime_type,size_bytes,purpose,created_at,lifecycle_status)
 values(3,'synthetic/lifecycle','synthetic.png','image/png',100,'POST_PHOTO',now()-interval '25 hours','UPLOADING');
select pg_temp.assert_ok('MEDIA02_uploading',(select uploaded_at is null from media_files where storage_key='synthetic/lifecycle'));
select pg_temp.assert_error('MEDIA03_unlinked_needs_upload',$q$update media_files set lifecycle_status='UNLINKED' where storage_key='synthetic/lifecycle'$q$,'23514');
update media_files set lifecycle_status='UNLINKED',uploaded_at=created_at+interval '1 hour' where storage_key='synthetic/lifecycle';
select pg_temp.assert_ok('MEDIA04_expiry_24h',(select uploaded_at <= now()-interval '24 hours' from media_files where storage_key='synthetic/lifecycle' and lifecycle_status='UNLINKED'));
update media_files set lifecycle_status='LINKED',linked_at=now() where storage_key='synthetic/lifecycle';
select pg_temp.assert_ok('MEDIA05_linked_protected',not exists(select 1 from media_files where storage_key='synthetic/lifecycle' and lifecycle_status='UNLINKED' and uploaded_at <= now()-interval '24 hours'));
update media_files set lifecycle_status='DELETE_PENDING',delete_requested_at=now(),deletion_attempts=1,
 next_delete_attempt_at=now()+interval '5 minutes',last_delete_error_code='SYNTHETIC_TIMEOUT' where storage_key='synthetic/lifecycle';
select pg_temp.assert_ok('MEDIA06_retry',(select deletion_attempts=1 and next_delete_attempt_at is not null from media_files where storage_key='synthetic/lifecycle'));
select pg_temp.assert_error('MEDIA07_negative_attempts',$q$update media_files set deletion_attempts=-1 where storage_key='synthetic/lifecycle'$q$,'23514');
select pg_temp.assert_error('MEDIA08_backdated_retry',$q$update media_files set next_delete_attempt_at=delete_requested_at-interval '1 second' where storage_key='synthetic/lifecycle'$q$,'23514');
select pg_temp.assert_error('MEDIA09_deleted_needs_time',$q$update media_files set lifecycle_status='DELETED' where storage_key='synthetic/lifecycle'$q$,'23514');
update media_files set lifecycle_status='DELETED',deleted_at=now(),next_delete_attempt_at=null,last_delete_error_code=null where storage_key='synthetic/lifecycle';
select pg_temp.assert_ok('MEDIA10_deleted',(select deleted_at is not null and next_delete_attempt_at is null from media_files where storage_key='synthetic/lifecycle'));
select pg_temp.assert_error('MEDIA11_unknown_state',$q$update media_files set lifecycle_status='UNKNOWN' where storage_key='synthetic/lifecycle'$q$,'23514');
select pg_temp.assert_error('MEDIA12_backdated_upload',$q$update media_files set uploaded_at=created_at-interval '1 second' where storage_key='synthetic/lifecycle'$q$,'23514');
select pg_temp.assert_error('MEDIA13_deleted_no_retry',$q$update media_files set next_delete_attempt_at=now()+interval '1 hour' where storage_key='synthetic/lifecycle'$q$,'23514');
select count(*) as passed_assertions from schema_test_results;
select case_id from schema_test_results order by case_id;
rollback;

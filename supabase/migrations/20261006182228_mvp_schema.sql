-- Issue #3: ERD의 27개 핵심 테이블. 공유 저장형/세션 방식은 미정이므로 제외.
-- Spring JDBC용 비공개 스키마. 서비스 트랜잭션/권한 규칙은 후속 구현과 구분한다.
create schema discushion;
set search_path = discushion, pg_catalog;

create table regions (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  name text not null, external_code text unique, map_feature_key text unique,
  check (id between 1 and 9007199254740991)
);
create table users (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  email text not null unique, password_hash text not null,
  email_verified_at timestamptz not null,
  created_at timestamptz not null, updated_at timestamptz not null,
  check (id between 1 and 9007199254740991)
);
create table institutions (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  name text not null, external_code text unique, created_at timestamptz not null,
  check (id between 1 and 9007199254740991)
);
create table media_files (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  owner_user_id bigint not null references users(id), storage_key text not null unique,
  original_name text not null, mime_type text not null, size_bytes bigint not null check(size_bytes > 0),
  purpose text not null check(purpose in ('PROFILE_IMAGE','POST_PHOTO','NEIGHBOR_EVIDENCE','INSTITUTION_EVIDENCE')),
  created_at timestamptz not null, check (id between 1 and 9007199254740991)
);
create table profiles (
  user_id bigint primary key references users(id),
  nickname varchar(10) not null unique check(length(btrim(nickname)) > 0),
  bio varchar(50), activity_region_id bigint not null references regions(id),
  profile_image_file_id bigint references media_files(id), updated_at timestamptz not null
);
create table profile_attributes (
  user_id bigint not null references profiles(user_id),
  attribute text not null check(attribute in ('RESIDENT','STUDENT','WORKER','MERCHANT')),
  primary key(user_id,attribute)
);
create table user_agreements (
  user_id bigint not null references users(id),
  agreement_type text not null check(agreement_type in ('TERMS_OF_SERVICE','PRIVACY_COLLECTION','MARKETING')),
  agreed boolean not null, policy_version text not null, recorded_at timestamptz not null,
  primary key(user_id,agreement_type), check(agreement_type='MARKETING' or agreed)
);
create table email_verifications (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  email text not null, purpose text not null check(purpose='SIGN_UP'), code_digest text,
  delivery_status text not null check(delivery_status in ('PENDING','SENT','FAILED')),
  requested_at timestamptz not null, sent_at timestamptz, expires_at timestamptz,
  resend_available_at timestamptz, attempt_count integer not null default 0 check(attempt_count between 0 and 5),
  verified_at timestamptz, invalidated_at timestamptz, proof_token_digest text unique,
  proof_expires_at timestamptz, consumed_at timestamptz, registered_user_id bigint references users(id),
  check (id between 1 and 9007199254740991),
  check(delivery_status<>'SENT' or (code_digest is not null and sent_at is not null
    and expires_at is not null and resend_available_at is not null))
);
create table neighbor_verification_requests (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  user_id bigint not null references users(id), region_id bigint not null references regions(id),
  status text not null check(status in ('RECEIVED','COMPLETED')),
  submitted_at timestamptz not null, completed_at timestamptz,
  unique(id,user_id,region_id), check (id between 1 and 9007199254740991),
  check(status<>'COMPLETED' or completed_at is not null)
);
create table neighbor_verification_evidences (
  request_id bigint not null references neighbor_verification_requests(id),
  file_id bigint not null references media_files(id), sort_order integer not null check(sort_order>=0),
  primary key(request_id,file_id), unique(request_id,sort_order)
);
create table neighbor_verified_regions (
  user_id bigint not null references users(id), region_id bigint not null references regions(id),
  source_request_id bigint unique, verified_at timestamptz not null,
  primary key(user_id,region_id),
  foreign key(source_request_id,user_id,region_id) references neighbor_verification_requests(id,user_id,region_id)
);
create table institution_verification_requests (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  user_id bigint not null references users(id), institution_id bigint references institutions(id),
  submitted_institution_name text not null, department_name text not null, position_name text not null,
  applicant_name text not null, work_email text not null, phone_number text not null,
  responsible_region_id bigint not null references regions(id),
  status text not null check(status in ('RECEIVED','COMPLETED')),
  submitted_at timestamptz not null, completed_at timestamptz,
  unique(id,user_id,institution_id,responsible_region_id), check (id between 1 and 9007199254740991),
  check(status<>'COMPLETED' or (completed_at is not null and institution_id is not null))
);
create table institution_verification_evidences (
  request_id bigint not null references institution_verification_requests(id),
  file_id bigint not null references media_files(id), sort_order integer not null check(sort_order>=0),
  primary key(request_id,file_id), unique(request_id,sort_order)
);
create table institution_credentials (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  request_id bigint not null unique references institution_verification_requests(id),
  user_id bigint not null references users(id), institution_id bigint not null references institutions(id),
  responsible_region_id bigint not null references regions(id), completed_at timestamptz not null,
  valid_until timestamptz not null check(valid_until>completed_at),
  unique(id,user_id,institution_id), check (id between 1 and 9007199254740991),
  foreign key(request_id,user_id,institution_id,responsible_region_id)
    references institution_verification_requests(id,user_id,institution_id,responsible_region_id)
);
create table posts (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  author_user_id bigint not null references users(id), region_id bigint not null references regions(id),
  type text not null check(type in ('LOCAL_AGENDA','LOCAL_ACTIVITY','VOTE')),
  topic text not null check(topic in ('TRANSPORTATION','HOUSING','SAFETY','WELFARE','LIVING_INFORMATION','ENVIRONMENT','OTHER')),
  title text not null, content text not null,
  status text not null check(status in ('PUBLISHED','DELETED')),
  content_revision bigint not null check(content_revision>=1),
  created_at timestamptz not null, updated_at timestamptz not null, deleted_at timestamptz,
  check (id between 1 and 9007199254740991),
  check((status='PUBLISHED' and deleted_at is null) or (status='DELETED' and deleted_at is not null))
);
create table activity_post_details (
  post_id bigint primary key references posts(id), source text not null, schedule text not null,
  place text not null, activity_status text not null check(activity_status in ('SCHEDULED','IN_PROGRESS','ENDED','CANCELED')),
  external_participation_url text
);
create table polls (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  post_id bigint not null unique references posts(id), question text not null, ends_at timestamptz not null,
  unique(post_id,id), check (id between 1 and 9007199254740991)
);
create table poll_options (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  poll_id bigint not null references polls(id), content text not null,
  sort_order integer not null check(sort_order>=0), unique(poll_id,sort_order), unique(poll_id,id),
  check (id between 1 and 9007199254740991)
);
create table vote_selections (
  poll_id bigint not null references polls(id), user_id bigint not null references users(id),
  option_id bigint not null, first_submitted_at timestamptz not null, updated_at timestamptz not null,
  primary key(poll_id,user_id), foreign key(poll_id,option_id) references poll_options(poll_id,id)
);
create table post_photos (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  post_id bigint not null references posts(id), file_id bigint not null references media_files(id),
  sort_order integer not null check(sort_order>=0),
  unique(post_id,sort_order) deferrable initially immediate, unique(post_id,file_id),
  check (id between 1 and 9007199254740991)
);
create table comments (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  post_id bigint not null references posts(id), parent_comment_id bigint, reply_to_comment_id bigint,
  author_user_id bigint references users(id), author_kind text not null check(author_kind in ('MEMBER','GUEST')),
  content text not null check(length(btrim(content))>0), reply_to_display_name text, created_at timestamptz not null,
  unique(post_id,id), check (id between 1 and 9007199254740991),
  foreign key(post_id,parent_comment_id) references comments(post_id,id),
  foreign key(post_id,reply_to_comment_id) references comments(post_id,id),
  check((author_kind='MEMBER' and author_user_id is not null) or (author_kind='GUEST' and author_user_id is null)),
  check(parent_comment_id is null or parent_comment_id<>id),
  check(reply_to_comment_id is null or reply_to_comment_id<>id),
  check(parent_comment_id is not null or (reply_to_comment_id is null and reply_to_display_name is null)),
  check((reply_to_comment_id is null)=(reply_to_display_name is null))
);
create table post_reactions (
  post_id bigint not null references posts(id), user_id bigint not null references users(id),
  reaction_type text not null check(reaction_type in ('EMPATHY','NEEDED','CURIOUS')),
  created_at timestamptz not null, primary key(post_id,user_id,reaction_type)
);
create table comment_evaluations (
  comment_id bigint not null references comments(id), user_id bigint not null references users(id),
  evaluation_type text not null check(evaluation_type in ('LIKE','DISLIKE')),
  created_at timestamptz not null, updated_at timestamptz not null, primary key(comment_id,user_id)
);
create table bookmarks (
  post_id bigint not null references posts(id), user_id bigint not null references users(id),
  created_at timestamptz not null, primary key(post_id,user_id)
);
create table activity_events (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  transition_key text not null unique, user_id bigint not null references users(id),
  post_id bigint not null references posts(id), comment_id bigint, poll_id bigint,
  event_type text not null check(event_type in ('POST_CREATED','BOOKMARK_REGISTERED','EMPATHY_REGISTERED',
    'NEEDED_REGISTERED','CURIOUS_REGISTERED','COMMENT_CREATED','REPLY_CREATED','COMMENT_LIKE_REGISTERED',
    'COMMENT_DISLIKE_REGISTERED','REPLY_LIKE_REGISTERED','REPLY_DISLIKE_REGISTERED','VOTE_PARTICIPATED')),
  occurred_at timestamptz not null, check (id between 1 and 9007199254740991),
  foreign key(post_id,comment_id) references comments(post_id,id),
  foreign key(post_id,poll_id) references polls(post_id,id),
  check(case when event_type in ('COMMENT_CREATED','REPLY_CREATED','COMMENT_LIKE_REGISTERED',
    'COMMENT_DISLIKE_REGISTERED','REPLY_LIKE_REGISTERED','REPLY_DISLIKE_REGISTERED')
    then comment_id is not null and poll_id is null
    when event_type='VOTE_PARTICIPATED' then poll_id is not null and comment_id is null
    else comment_id is null and poll_id is null end)
);
create table institution_agenda_adoptions (
  id bigint generated always as identity (maxvalue 9007199254740991) primary key,
  post_id bigint not null references posts(id), institution_id bigint not null references institutions(id),
  adopted_by_user_id bigint not null references users(id), credential_id bigint not null references institution_credentials(id),
  adopted_at timestamptz not null, canceled_at timestamptz, canceled_by_user_id bigint references users(id),
  check (id between 1 and 9007199254740991),
  foreign key(credential_id,adopted_by_user_id,institution_id) references institution_credentials(id,user_id,institution_id),
  check((canceled_at is null)=(canceled_by_user_id is null)), check(canceled_at is null or canceled_at>=adopted_at)
);
create unique index uq_current_institution_agenda_adoption on institution_agenda_adoptions(post_id,institution_id)
  where canceled_at is null;
create table ai_agenda_summaries (
  post_id bigint primary key references posts(id), source_revision bigint not null check(source_revision>=1),
  status text not null check(status in ('PENDING','SUCCEEDED','FAILED','SOURCE_TOO_SHORT')),
  summary text, requested_at timestamptz not null, generated_at timestamptz, updated_at timestamptz not null,
  check((status='SUCCEEDED' and summary is not null and generated_at is not null)
    or (status<>'SUCCEEDED' and summary is null and generated_at is null))
);

-- PK/UNIQUE의 선두 컬럼은 중복 인덱스를 만들지 않는다. 실제 쿼리 계획은 후속 API 구현 시 검증.
create index ix_profiles_region on profiles(activity_region_id);
create index ix_profiles_image on profiles(profile_image_file_id);
create index ix_media_owner on media_files(owner_user_id,purpose,created_at);
create index ix_email_recent on email_verifications(email,purpose,sent_at desc,id desc);
create index ix_email_user on email_verifications(registered_user_id);
create index ix_neighbor_request_user on neighbor_verification_requests(user_id,submitted_at desc,id desc);
create index ix_neighbor_request_region on neighbor_verification_requests(region_id);
create index ix_neighbor_evidence_file on neighbor_verification_evidences(file_id);
create index ix_verified_region on neighbor_verified_regions(region_id,user_id);
create index ix_institution_request_user on institution_verification_requests(user_id,submitted_at desc,id desc);
create index ix_institution_request_institution on institution_verification_requests(institution_id);
create index ix_institution_request_region on institution_verification_requests(responsible_region_id);
create index ix_institution_evidence_file on institution_verification_evidences(file_id);
create index ix_credential_user on institution_credentials(user_id,valid_until,completed_at);
create index ix_credential_institution on institution_credentials(institution_id);
create index ix_credential_region on institution_credentials(responsible_region_id);
create index ix_posts_filter on posts(region_id,status,type,topic,created_at desc,id desc);
create index ix_posts_region on posts(region_id,status,created_at desc,id desc);
create index ix_posts_author on posts(author_user_id,status,created_at desc,id desc);
create index ix_polls_end on polls(ends_at,post_id);
create index ix_votes_user on vote_selections(user_id,first_submitted_at desc,poll_id);
create index ix_votes_option on vote_selections(poll_id,option_id);
create index ix_photos_file on post_photos(file_id);
create index ix_comments_parent on comments(post_id,parent_comment_id,created_at desc,id desc);
create index ix_comments_reply_to on comments(post_id,reply_to_comment_id);
create index ix_comments_author on comments(author_user_id,post_id);
create index ix_reactions_user on post_reactions(user_id,post_id,reaction_type);
create index ix_reactions_counts on post_reactions(post_id,reaction_type);
create index ix_evaluations_user on comment_evaluations(user_id,comment_id);
create index ix_evaluations_counts on comment_evaluations(comment_id,evaluation_type);
create index ix_bookmarks_user on bookmarks(user_id,created_at desc,post_id);
create index ix_events_user on activity_events(user_id,event_type,occurred_at);
create index ix_events_comment on activity_events(post_id,comment_id);
create index ix_events_poll on activity_events(post_id,poll_id);
create index ix_adoptions_post on institution_agenda_adoptions(post_id);
create index ix_adoptions_institution on institution_agenda_adoptions(institution_id,canceled_at,adopted_at desc,id desc);
create index ix_adoptions_credential on institution_agenda_adoptions(credential_id,adopted_by_user_id,institution_id);
create index ix_adoptions_author on institution_agenda_adoptions(adopted_by_user_id);
create index ix_adoptions_cancel_user on institution_agenda_adoptions(canceled_by_user_id);

-- Data API/Supabase Auth는 채택하지 않았다. PUBLIC/API 역할은 이 내부 스키마에 접근하지 못한다.
revoke all on schema discushion from public;
revoke all on all tables in schema discushion from public;
revoke all on all sequences in schema discushion from public;
alter default privileges in schema discushion revoke all on tables from public;
alter default privileges in schema discushion revoke all on sequences from public;
do $security$
declare t record; r text;
begin
  for t in select tablename from pg_catalog.pg_tables where schemaname='discushion' loop
    execute format('alter table discushion.%I enable row level security',t.tablename);
  end loop;
  foreach r in array array['anon','authenticated','service_role'] loop
    if exists(select 1 from pg_catalog.pg_roles where rolname=r) then
      execute format('revoke all on schema discushion from %I',r);
      execute format('revoke all on all tables in schema discushion from %I',r);
      execute format('revoke all on all sequences in schema discushion from %I',r);
      execute format('alter default privileges in schema discushion revoke all on tables from %I',r);
      execute format('alter default privileges in schema discushion revoke all on sequences from %I',r);
    end if;
  end loop;
end $security$;
reset search_path;

# Discushion MVP DB Schema 준비 명세 — Issue #3

> **최신 우선 기준:** `dad35c0`의 MVP v10.2는 자체 비밀번호/OTP와 증빙 신청을 제외했다. 아래 v10.1 준비 내용은 이력이며 현재 구현 대상으로 그대로 사용하지 않는다. [사용자 승인된 v10.2 DB 반영](Discushion_Issue3_MVP_v10.2_반영.md)을 우선 확인한다. 후속 Migration은 비밀번호·기관 신청의 NOT NULL 해제, Privy/가입 완료·사진 lifecycle을 구현했고 기존 파일 두 개는 보존했다. 현재 legacy 포함 27테이블·176컬럼이다. API/FE와 실행 worker·최소 권한 서버 역할은 아직 미완료다.

> 작성일: 2026-10-07 (Asia/Seoul)
> 담당: BE1 / 공통 데이터 모델 / 통합 지침서 §3, API §9
> 최초 기준: `back/develop`에서 pull한 `ad8f987fe75d05b5c23d405e023b71e1803dfa96`
> 현재 기준: 2026-10-07 재개 시 pull한 `aedc647952dadbe503fa2f2e91cbf52e300405f7`
> 작업 브랜치: `back/feature/3-schema`
> 현재 상태: Migration과 별도 native PostgreSQL의 로컬 검증까지 수행. 최신 구현/검증 결과는 §15와 별도 결과 문서 참조. 아래 최초 준비·실패 기록은 당시 상태로 보존.

## 1. 이번 작업과 근거

이 문서는 기존 ERD의 컬럼 정의를 대체하지 않는다. 그 구조를 PostgreSQL Schema로 옮기기 위한 생성 순서, 제약조건, 트랜잭션 경계, 결정 대기 항목을 정리한다. 별도 승인 기록이 없는 물리 설계 선택은 BE1·BE2 검토 전의 **설계 제안**이며 제품 정책과 구분한다. 소프트 삭제·개인 투표/누적 활동/채택 이력 보존 구조는 §13의 사용자 승인 범위에서 구현 기준으로 채택한다.

| 기준 | 용도 |
| --- | --- |
| [Issue #3](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/issues/3) | A~D 원본·관계 검토, Migration 적용·재실행, 공개 차단·기록 보존 합의의 완료 조건 |
| [PRD v10.1](../specs/Discushion_PRD_2026-10-06_MVP반영_정리본_v10.1.md), [기능명세서 v10.1](../specs/Discushion_기능명세서_2026-10-06_MVP반영_정리본_v10.1.md) | MVP 범위와 확정 제품 동작 |
| [상세 ERD](../specs/Discushion_MVP_ERD_상세명세.md) | §4 FK 연결, §5 전체 컬럼·NULL, §6 제약·인덱스, §9 삭제·동시성 |
| [통합 지침서](../specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md) | §3 원본·현재 관계·누적 활동 분리 |
| [API v1.1.1](../api/Discushion_API_SPEC_v2.md), [계약 검토표](../api/Discushion_API_CONTRACT_검토표_2026-10-07.md) | 기존 API·숫자형 ID 유지, C01~C16 및 D01~D17의 미정 사항 추적 |
| [#1 환경 결정 기록](../collaboration/backend-environment-decisions.md), [Backend 안내](../../backend/README.md) | Java·Spring Boot·Gradle / Supabase PostgreSQL 선택과 미검증 환경 |
| [DB 검증 계획](Discushion_MVP_DB_검증계획_2026-10-07.md) | 실제 구현 후 실행할 무결성·동시성·Migration 시험 |

작업 시작 시 #1은 열려 있지만 실행 골격은 위 기준 SHA에 반영돼 있다. JDBC 의존성은 있고 ORM·Migration 도구는 구성되지 않았다. 실제 Supabase 연결도 미검증이다. #2는 GitHub에서 닫혔지만 검토표에 기록되지 않은 팀 합의를 완료한 것으로 간주하지 않는다.

ERD 표현 오류 수정 [PR #35](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/pull/35)는 작업 시작 시 미병합이다. 현재 기준의 ERD는 v1.0이고 편집용 `.mmd`는 없다. 본 문서는 PR #35를 중복 수정하지 않으며, 증빙 파일의 식별 관계 표기·완료 지역 근거 신청의 UNIQUE·API 상대 경로 수정은 해당 PR 통합 후 재대조한다. 아래 설계에는 ERD 본문에 이미 명시된 `source_request_id` 유일성을 포함한다.

최초 준비 단계에서 하지 않은 작업: DB 접속정보 변경, Supabase 접속·DDL 실행, ORM/도구 설치, 실행환경 변경, 실제 seed 삽입, API/DTO 변경, commit·push·PR 생성·Issue 종료. 이후 사용자 승인에 따라 Java 17 설치와 로컬 CLI 초기화는 §14에서 별도로 진행했다. 실제 DB 적용은 여전히 미실행이다.

## 2. 타입과 공통 규칙 — PostgreSQL 전환 제안

| ERD 항목 | 물리 타입·처리 제안 | 확정/대기 경계 |
| --- | --- | --- |
| 단독 `id` PK | `bigint` + identity 발급 | 자동 발급 방식은 검토 필요. 공유 PK·복합 PK에는 새 identity를 만들지 않음 |
| 참조 ID | 부모와 같은 `bigint` | JSON ID는 기존 API의 `number` 유지. 발급·직렬화 모두 1~9,007,199,254,740,991의 안전 정수 범위 보장 필요 |
| 모든 절대 시각 | `timestamptz` | ERD의 `timestamp`를 무조건 `timestamp without time zone`으로 옮기지 않음. API ISO 8601, 화면 Asia/Seoul |
| 닉네임·소개 | `varchar(10)`, nullable `varchar(50)` | 제품 길이는 확정. 이메일·닉네임 정규화/대소문자 비교는 미정 |
| 본문·제목·전화·코드 검증값 | `text` | 미정 최대 길이를 임의 설정하지 않음. 전화와 인증 코드 원문은 숫자 타입으로 처리하지 않음 |
| 상태·종류 코드 | `text` + 허용값 `CHECK` | 코드 목록은 기존 API/ERD 사용. PostgreSQL ENUM 채택은 확정하지 않음 |
| 순서·시도 횟수 | `integer` | `sort_order >= 0`, 이메일 시도 `0 <= attempt_count <= 5` |
| 파일 바이트·원문 버전 | `bigint` | 파일 크기 `> 0`, 원문 버전 `>= 1`. MB 환산은 D06 대기 |
| 동의 여부 | `boolean NOT NULL` | 필수 약관 두 종류는 true, 선택 동의 false 허용 |
| NULL·기본값 | ERD §5의 NULL만 허용 | `CHECK`만으로 필수 입력을 강제하지 않음. 활동 상태에 기본값 금지 |

`bigint`는 JavaScript 안전 정수보다 넓으므로 DB 타입만 선택해도 API 문제는 해결되지 않는다. identity를 채택하면 발급 상한과 명시적 ID 입력 검증을 함께 설계하고, 문자열 ID로 바꾸려면 별도 API 합의가 필요하다. [PostgreSQL 숫자 타입](https://www.postgresql.org/docs/current/datatype-numeric.html)

`timestamptz`는 절대 시각을 저장하고 출력 시 세션 시간대가 적용된다. 입력에 오프셋을 명시하고 UTC 기반 비교·Asia/Seoul 표시를 구분한다. [PostgreSQL 시각 타입](https://www.postgresql.org/docs/current/datatype-datetime.html)

행 내 조건에는 `NOT NULL`과 `CHECK`를 함께 사용한다. 기본 nullable UNIQUE는 여러 NULL을 허용하며 PK·UNIQUE가 만드는 인덱스를 중복 생성하지 않는다. 다른 행의 개수·소유권·시간에 따라 변하는 권한은 아래 §5에서 별도로 처리한다. [PostgreSQL 제약조건](https://www.postgresql.org/docs/current/ddl-constraints.html)

## 3. 생성 순서와 테이블별 핵심 키

27개 핵심 테이블과 1개 선택 테이블이다. 순서는 모든 부모를 먼저 만드는 기준이며 Migration 파일 개수나 버전 번호를 확정하는 표가 아니다. 부모 컬럼과 전체 FK는 ERD §4를 사용하고, 복합 연결은 §4의 보강을 함께 적용한다.

| 순서 | 테이블 | PK | 선행 참조 테이블 | 추가 유일성·주요 역할 |
| --- | --- | --- | --- | --- |
| 01 | `regions` | `id` | 없음 | nullable `external_code`, `map_feature_key` 각각 UNIQUE. 같은 동 이름 허용 |
| 02 | `users` | `id` | 없음 | `email` UNIQUE. 고정 기관/이웃 역할 플래그 저장 안 함 |
| 03 | `institutions` | `id` | 없음 | nullable `external_code` UNIQUE. 기관명만으로 정본 병합 금지 |
| 04 | `media_files` | `id` | `users` | `storage_key` UNIQUE. 파일 원문 대신 메타데이터·비공개 내부 경로 |
| 05 | `profiles` | `user_id` | `users`, `regions`, `media_files` | `nickname` UNIQUE. 기본 활동 지역은 필수, 사진은 선택 |
| 06 | `profile_attributes` | `(user_id, attribute)` | `profiles` | 복수 속성의 중복 방지. 자격·배지 근거 아님 |
| 07 | `user_agreements` | `(user_id, agreement_type)` | `users` | 종류별 동의 기록. 필수 행 존재는 가입 트랜잭션 |
| 08 | `email_verifications` | `id` | `users` | nullable `proof_token_digest` UNIQUE. 가입 전 `registered_user_id` NULL 허용 |
| 09 | `neighbor_verification_requests` | `id` | `users`, `regions` | §4 참조용 UNIQUE. 회원·지역 신청 회차 수 UNIQUE 금지 |
| 10 | `neighbor_verification_evidences` | `(request_id, file_id)` | `neighbor_verification_requests`, `media_files` | `(request_id, sort_order)` UNIQUE |
| 11 | `neighbor_verified_regions` | `(user_id, region_id)` | `users`, `regions`, `neighbor_verification_requests` | nullable `source_request_id` UNIQUE. 완료 최대 3개는 트랜잭션 |
| 12 | `institution_verification_requests` | `id` | `users`, `institutions`, `regions` | §4 참조용 UNIQUE. 접수 시 정본 기관 ID는 NULL 가능 |
| 13 | `institution_verification_evidences` | `(request_id, file_id)` | `institution_verification_requests`, `media_files` | `(request_id, sort_order)` UNIQUE |
| 14 | `institution_credentials` | `id` | `institution_verification_requests`, `users`, `institutions`, `regions` | `request_id` UNIQUE + §4 참조용 UNIQUE. `user_id` 단독 UNIQUE 금지 |
| 15 | `posts` | `id` | `users`, `regions` | 세 유형 공통 원본. 제목/본문·유형·주제·지역 각 하나 |
| 16 | `activity_post_details` | `post_id` | `posts` | 활동 전용 공유 PK. 상태는 필수 선택이며 자동 진행 안 함 |
| 17 | `polls` | `id` | `posts` | `post_id` UNIQUE + `(post_id, id)` 참조용 UNIQUE. 종료 상태는 계산 |
| 18 | `poll_options` | `id` | `polls` | `(poll_id, sort_order)` UNIQUE + `(poll_id, id)` 참조용 UNIQUE |
| 19 | `vote_selections` | `(poll_id, user_id)` | `polls`, `users`, `poll_options` | 동일 투표 선택지 복합 FK. 최초 참여 시각 유지 |
| 20 | `post_photos` | `id` | `posts`, `media_files` | `(post_id, sort_order)`, `(post_id, file_id)` 각각 UNIQUE |
| 21 | `comments` | `id` | `posts`, `users`, `comments` 자체 | `(post_id, id)` 참조용 UNIQUE. 자기 FK는 테이블 생성 후 선언 가능 |
| 22 | `post_reactions` | `(post_id, user_id, reaction_type)` | `posts`, `users` | 세 반응 유형을 독립 저장 |
| 23 | `comment_evaluations` | `(comment_id, user_id)` | `comments`, `users` | LIKE/DISLIKE 상호배타. 종류를 PK에 넣지 않음 |
| 24 | `bookmarks` | `(post_id, user_id)` | `posts`, `users` | 현재 저장 관계. 참여 목록의 원본 아님 |
| 25 | `activity_events` | `id` | `users`, `posts`, `comments`, `polls` | `transition_key` UNIQUE. 누적 +1만 저장; 현재 관계 FK 없음 |
| 26 | `institution_agenda_adoptions` | `id` | `posts`, `institutions`, `users`, `institution_credentials` | 현재 행의 `(post_id, institution_id)`만 부분 UNIQUE |
| 27 | `ai_agenda_summaries` | `post_id` | `posts` | 안건 전용 최신 슬롯 제안. 원문 버전 비교 |
| 28 선택 | `post_share_links` | `post_id` | `posts`, `users` | `token_digest` UNIQUE. 저장형 공유를 채택할 때만 생성 |

선택 공유 테이블·로그인 세션 테이블은 미정이다. Supabase 선택만으로 `auth.users` 연동, Supabase Auth/Storage, 브라우저의 직접 DB 쓰기를 추가하지 않는다. 알림·신고·탈퇴·운영자 심사 테이블도 MVP에 새로 추가하지 않는다.

## 4. 잘못된 귀속을 막는 복합 FK

단일 ID 참조만 있으면 존재하는 타인의 신청·타 투표 옵션·타 게시물 댓글이 잘못 연결될 수 있다. 다음 부모 UNIQUE 6개와 복합 FK 8개가 필요하다. 이는 ERD §4의 PostgreSQL 전환 목록이며 실행 DDL은 아니다.

| 부모 테이블 | 참조용 UNIQUE |
| --- | --- |
| `neighbor_verification_requests` | `(id, user_id, region_id)` |
| `institution_verification_requests` | `(id, user_id, institution_id, responsible_region_id)` |
| `institution_credentials` | `(id, user_id, institution_id)` |
| `poll_options` | `(poll_id, id)` |
| `comments` | `(post_id, id)` |
| `polls` | `(post_id, id)` |

| 자식 테이블·컬럼 | 부모 테이블·컬럼 | DB가 차단하는 오류 |
| --- | --- | --- |
| `neighbor_verified_regions.(source_request_id, user_id, region_id)` | `neighbor_verification_requests.(id, user_id, region_id)` | 타 회원/타 지역 신청을 완료 근거로 사용 |
| `institution_credentials.(request_id, user_id, institution_id, responsible_region_id)` | `institution_verification_requests.(id, user_id, institution_id, responsible_region_id)` | 신청자·기관·담당 지역을 바꿔 인증 생성 |
| `vote_selections.(poll_id, option_id)` | `poll_options.(poll_id, id)` | 타 투표 선택지를 제출 |
| `comments.(post_id, parent_comment_id)` | `comments.(post_id, id)` | 타 게시물 댓글을 계층 부모로 사용 |
| `comments.(post_id, reply_to_comment_id)` | `comments.(post_id, id)` | 타 게시물 댓글을 지목 |
| `activity_events.(post_id, comment_id)` | `comments.(post_id, id)` | 타 게시물 댓글의 이벤트 생성 |
| `activity_events.(post_id, poll_id)` | `polls.(post_id, id)` | 타 게시물 투표의 이벤트 생성 |
| `institution_agenda_adoptions.(credential_id, adopted_by_user_id, institution_id)` | `institution_credentials.(id, user_id, institution_id)` | 타 회원/타 기관 인증을 채택 감사 근거로 사용 |

nullable 연결은 기본 `MATCH SIMPLE` 전제다. 선택 ID가 NULL이면 복합 FK 검사를 건너뛰므로 필수 `user_id`, `region_id`, `post_id` 등의 단독 FK와 NOT NULL을 유지한다. 완료 상태·인증 시각·댓글 깊이·파일 소유권은 복합 FK만으로 검증되지 않는다.

## 5. DB 행 내 검사와 서비스 트랜잭션 분리

### 5.1 행 내 CHECK 목록

| 대상 | 검사 계획 |
| --- | --- |
| 모든 종류/상태 컬럼 | ERD §5의 허용 코드만 수용. 전체 필터·게스트 접근 컨텍스트·만료 파생 상태를 정본 코드에 섞지 않음 |
| `profiles` | 닉네임 공백만 입력 거부, 최대 10자; 소개 최대 50자. Unicode 길이 판단은 FE/BE 계약과 대조 |
| `user_agreements` | 필수 두 종류면 `agreed=true`. 세 동의 행의 존재는 CHECK로 보장 못 함 |
| `email_verifications` | 시도 0~5. SENT이면 코드 검증값·성공 발송·만료·재발송 가능 시각 필수. 가입 증명 TTL·소비 정책은 D03 대기 |
| `media_files` / 순서 컬럼 | `size_bytes > 0`; 증빙·사진·선택지 `sort_order >= 0` |
| 인증 신청 | COMPLETED이면 `completed_at` 필수. 기관 신청 COMPLETED이면 `institution_id`도 필수 |
| `institution_credentials` | `valid_until > completed_at`. 정확한 달력 1년·윤년 경계 계산은 합의 대기 |
| `posts` | `content_revision >= 1`. 사용자 승인된 소프트 삭제 구조에 맞춰 공개/삭제 상태와 `deleted_at` 일치 검사 |
| `comments` | MEMBER이면 작성자 NN, GUEST이면 NULL. 부모/지목의 자기 ID 금지. 원 댓글이면 지목 ID/이름 NULL, 지목 ID/이름의 NULL 쌍 일치 |
| `activity_events` | 댓글 관련 6종은 comment만 NN, 최초 투표는 poll만 NN, 나머지 5종은 둘 다 NULL. 총 12종 +1 이벤트 |
| `institution_agenda_adoptions` | 취소 시각/취소자 둘 다 NULL 또는 둘 다 NN. 취소 시각은 채택 시각 이상 |
| `ai_agenda_summaries` | 최신 슬롯 채택 시 SUCCEEDED에만 summary/generated_at NN, 나머지 NULL. 최신 원문 일치는 별도 비교 |

### 5.2 다른 행·동시 요청·시간에 의존하는 규칙

아래는 후속 서비스 구현의 경계다. COUNT나 현재 시각에 의존하는 권한을 행 내 CHECK로 해결했다고 기록하지 않는다. 잠금·충돌 처리의 구체 방식은 #5 및 각 구현 이슈에서 BE2와 맞춘다.

| 쓰기 단위 | 같은 트랜잭션에서 해야 할 일 | 관련 이슈 |
| --- | --- | --- |
| 가입 | 유효 이메일 증명 검증/소비, 계정·프로필·활동 지역·속성·동의 원자 저장. 성공 세션 이후 이웃 신청 진입 | #6·#7·#11 |
| 인증 완료 시연 처리 | 사용자 행 잠금 후 완료 지역 중복/최대 3개 검사, 신청 COMPLETED와 완료 관계 함께 기록. 접수만으로 권한 부여 금지 | #11 |
| 기관 완료 시연 처리 | 정본 기관·신청·인증의 회원/지역/완료 시각 일치. 현재 인증 개수 제한은 정책 확정 후 구현 | #12 |
| 게시물 생성 | 유형에 맞는 확장 존재/부재 검증, 투표 2~10개 옵션, 사진 0~10장/합계 제한, 소유자·파일 용도 확인, 생성 이벤트 | #13·#14 |
| 댓글·답글 | 같은 게시물의 원 부모(parent=NULL) 확인, 지목 대상이 같은 thread인지 확인. 회원만 활동 이벤트, 게스트 회원 이관 금지 | #22 |
| 반응·평가·북마크 | 현재 행의 실제 변화와 +1 이벤트 함께 저장. 중복 최종 상태 재시도 +0, 평가 직접 전환 +0, 취소 후 재등록 +1 | #5·#23·#24·#26 |
| 투표 | Post → Poll → 현재 선택 등 일관된 잠금 순서. 잠금 후 공개 상태·종료 시각 재확인, 동일 투표 옵션·변경 확인, 최초 시각/이벤트 유지 | #25 |
| 게시 삭제 | 유형·소유권 검증, 공개 차단과 현재 북마크 제거 원자 처리. 같은 Post 잠금에 참여 쓰기를 맞춰 삭제 이후 새 관계 방지 | #16·#26·#27 |
| 채택·취소 | 매번 현재 유효 기관/담당 지역·공개 안건·본인 기관 재확인. 활성 UNIQUE 충돌은 기존 관계 처리, 타 기관 관계 유지 | #28·#29 |
| 요약 결과 저장 | 현재 공개 상태·원문 버전 비교 후 갱신. 수정 전 작업이 늦게 끝나도 최신 결과를 덮지 않음 | #20 |

빈 테이블의 행을 잠그는 것만으로 최초 INSERT 경합은 막히지 않는다. 동일 부모 행 잠금·UNIQUE·INSERT 결과 판정 등 하나의 전략을 정하고, 실제 INSERT 성공에만 이벤트를 만든다. 제약 위반으로 트랜잭션이 중단되면 그 트랜잭션에서 무조건 이어서 이벤트를 쓰지 않는다.

`transition_key`의 UNIQUE는 같은 전이 이벤트의 중복만 막는다. 매번 새 키를 만든다고 같은 행동 중복이 해결되지는 않는다. `UNIQUE(user_id, post_id, event_type)`는 합법적인 취소 후 재등록을 막으므로 넣지 않는다. 생성 POST의 요청 멱등 키 범위·TTL·payload 비교는 D14/#5에서 따로 정한다.

## 6. 삭제와 조회 원본 — 확정 결과와 미정 저장 방식

업데이트: 2026-10-07 사용자(BE1)가 아래 소프트 삭제·북마크 제거·개인 투표/활동/채택 이력 보존 구조를 승인했다. 기존 ERD의 제안 표기는 원래 문서의 상태를 설명하며, 이번 구현에서는 §13의 승인 기록을 따른다. 증빙 파일 보관 기간·물리 정리와 다른 미정 정책까지 승인된 것은 아니다.

| 상황 | 제품 확정 결과 | ERD의 저장 제안 / 남은 선택 |
| --- | --- | --- |
| 게시물 삭제 | 모든 공개 경로 콘텐츠 차단·북마크 제거·개인 투표 기록 유지 | 사용자 승인: `status=DELETED` + `deleted_at`, 안정 원본을 보존해 투표/활동/채택 이력 유지. 세부 FK 선언은 검증 필요, 물리 보존 기간 미정 |
| 반응/평가/북마크 취소 | 현재 관계 제거, 누적 활동 차감 없음 | 관계 DELETE, 누적 이벤트 유지 제안. 이벤트를 삭제되는 관계의 자식으로 연결하지 않음 |
| 삭제 투표의 개인 기록 | UNAVAILABLE, `post=null`, `vote=null`; 본문·선택지·결과·이미지 재노출 금지 | 원문과 표의 물리 보존 전략 및 OPEN/CLOSED 필터는 D09 대기 |
| 기관 채택 취소 | 본인 기관 표시 제거·타 기관 유지·내부 취소 이력 보존 | 회차 UPDATE + 재채택 새 INSERT 제안. 현재 행만 부분 UNIQUE |
| 기관 인증 만료 | 권한·과거 게시물 기관 배지 즉시 제거 | 현재 시각으로 판정. 과거 credential 감사 근거 유지 제안, 자동 채택 취소 안 함 |
| 파일 연결 제거 | 화면에서 파일 제외 | object 폐기·증빙 보관 기간은 미정. 무기한 보관으로 해석 금지 |

소프트 삭제의 UPDATE에는 FK `ON DELETE CASCADE`가 실행되지 않는다. 따라서 북마크 제거와 모든 공개 조회 차단은 별도의 명시적 처리여야 한다. 물리 삭제/탈퇴/보관기간 결정 없이 일반 CASCADE 또는 자동 정리 작업을 추가하지 않는다.

현재 채택 유일성은 `(post_id, institution_id) WHERE canceled_at IS NULL`의 부분 유일 인덱스 제안이다. 취소 이력을 포함하는 전체 `(post_id, institution_id)` UNIQUE는 재채택을 막으며, `(post_id, institution_id, canceled_at)` UNIQUE만으로 활성 NULL 중복을 막을 수 없다.

조회 연결은 ERD §7~8을 그대로 사용한다. 현재 참여 목록은 본인 반응·회원 댓글·평가·표의 Post 귀속을 UNION하고 postId 중복을 제거한다. 북마크·누적 이벤트만으로 참여 카드를 만들지 않는다. 반응/평가/득표·댓글 수는 현재 관계를 집계하며 별도 필수 카운터 테이블을 추가하지 않는다.

## 7. 조회 인덱스 준비

아래는 ERD §6.3의 후보다. PK·UNIQUE와 중복 생성하지 않고 실제 API 쿼리, 데이터 규모, `EXPLAIN` 확인 후 채택한다. API의 cursor·정렬은 D10 대기이며 최신순 ID 보조 정렬을 제품 확정으로 취급하지 않는다.

| 테이블 | 후보 컬럼 순서 | 용도 |
| --- | --- | --- |
| `posts` | `(region_id, status, type, topic, created_at DESC, id DESC)`; `(region_id, status, created_at DESC, id DESC)`; `(author_user_id, status, created_at DESC, id DESC)` | 지역 필터·전체 유형·내 작성 목록 |
| `profiles` | `(activity_region_id)` | 기본 지역 연결 |
| `neighbor_verification_requests` | `(user_id, submitted_at DESC, id DESC)` | 신청 이력 |
| `neighbor_verified_regions` | `(region_id, user_id)` | 완료 지역 역방향 조회 |
| `institution_verification_requests` | `(user_id, submitted_at DESC, id DESC)` | 기관 신청 이력 |
| `institution_credentials` | `(user_id, valid_until, completed_at)` | 현재 인증 |
| `media_files` | `(owner_user_id, purpose, created_at)` | 파일 소유/용도 |
| `email_verifications` | `(email, purpose, sent_at DESC, id DESC)` | 최신 코드·발송 제한 |
| `comments` | `(post_id, parent_comment_id, created_at DESC, id DESC)`; `(author_user_id, post_id)` | 댓글 목록·본인 참여 |
| `post_reactions` | `(user_id, post_id, reaction_type)`; `(post_id, reaction_type)` | 본인 참여·유형별 집계 |
| `comment_evaluations` | `(user_id, comment_id)`; `(comment_id, evaluation_type)` | 본인 평가·대상 집계 |
| `vote_selections` | `(user_id, first_submitted_at DESC, poll_id)`; `(poll_id, option_id)` | 개인 투표·선택지 집계 |
| `polls` | `(ends_at, post_id)` | 진행/종료 필터 |
| `bookmarks` | `(user_id, created_at DESC, post_id)` | 현재 저장 목록 |
| `activity_events` | `(user_id, event_type, occurred_at)` | 누적 활동 |
| `institution_agenda_adoptions` | `(institution_id, canceled_at, adopted_at DESC, id DESC)` | 기관 현재 채택 |

좋아요순·기관 반응순·지도 대표는 관계 집계 정렬이다. 위 timestamp 인덱스만으로 해결됐다고 판단하지 않는다. FK 자식 인덱스는 조회·부모 변경 계획과 함께 별도 점검한다.

## 8. 시연/검증 데이터 준비 원칙

- 실제 지역·기관 정본 원천과 코드가 미정이므로 이번 작업에서 현실 기관/행정 데이터 seed를 만들지 않는다.
- 격리된 테스트 DB에만 서로 다른 지역 4개, 회원 2명 이상, 기관 2개, 게시물 세 유형, 각 상태의 인증·관계를 합성한다. 실제 계정·증빙·메일·전화·비밀값 사용 금지.
- 완료 상태는 시연용으로만 준비하고 운영자 심사 API/UI는 추가하지 않는다. 신청 없는 완료 지역의 `source_request_id=NULL`은 보호된 테스트 fixture 제안이지 사용자 쓰기 API가 아니다.
- 기관 fixture는 유효·만료와 담당 지역을 구분하고 실제 업무 담당자 정책을 유지한다. 복수 동시 기관 인증 허용 여부는 정책 결정 전 고정하지 않는다.
- 시연 seed와 Schema Migration을 분리한다. 재실행 시 PK/코드 충돌과 기존 관계 변경이 없도록 식별 전략을 정하고, 명시적 identity 값을 넣으면 후속 발급 충돌을 별도 확인한다.
- fixture 정리는 지정된 테스트 DB/Schema만 대상으로 하고 운영 데이터 초기화·범용 삭제 스크립트는 만들지 않는다.

## 9. Migration 착수 전 결정 목록

| 항목 | 상태 | 다음 확인 / 영향 |
| --- | --- | --- |
| DBMS·프레임워크 | 사용자 선택 기록 있음: Supabase PostgreSQL / Java·Spring Boot·Gradle | #1에서 실제 PostgreSQL 버전·대상 DB·실행 경로 확인. 원격 접속 성공은 아직 없음 |
| Migration 도구·권한·실행 시점 | Supabase CLI 2.120.0 + 사용자 승인된 native PostgreSQL 17.11로 로컬 검증 | 실제 Supabase 버전·기존 DB·적용/서버 역할·원격 연결은 BE2 확인 필요 |
| 보존·삭제 방식 | D09 일부 사용자 승인, §13 | 소프트 삭제·북마크 제거·개인 투표/활동/채택 이력 보존 채택. 증빙/개인정보 보관 기간·물리 폐기·삭제 투표 필터는 별도 미정 |
| 정본 원천·기관 인증 개수 | D11 미정 | 지역/기관 원천, 동일성, 동시 유효 기관 인증 정책 합의. 이름 UNIQUE·회원 인증 UNIQUE 금지 |
| PK 발급·비교 규칙 | ERD 제안 / 일부 API 기존안 | 안전 정수 발급 전략, 이메일/닉네임 정규화·비교 방식 검토 |
| 가입 증명·세션 | D02·D03 미정 | TTL·소비·폐기·세션 저장 필요성 결정. refresh/session 테이블 임의 추가 금지 |
| 파일·일정·입력 형식 | D04~D06·D08 미정 | 크기/종류·bytes 환산·활동 schedule 구조·최대 길이 확정 후 해당 제약 작성 |
| 공유·요약 저장 방식 | D12·D13 미정 | 선택 테이블·최신 슬롯 채택 여부 결정. 단방향 해시에서 공유 원문 복구 가정 금지 |
| 이벤트·멱등 | D14/#5 대기 | 실제 상태 전이·원자성·키 범위·재등록 처리 합의 |
| 안건 지역 변경·취소 재시도 | D16 미정 | 채택 이후 수정 영향·동일 취소 응답 계약 결정 |
| ERD 수정 통합·BE2 검토 | PR #35 미병합 / 검토 기록 없음 | 같은 #3 변경의 통합 순서 협의 후 ERD·Schema 대조. 별도 PR로 #3을 조기 종료하지 않음 |

전부 결정될 때까지 모든 작업을 멈춰야 한다는 뜻은 아니다. 영향 없는 정본·키 구조부터 검토할 수 있지만, 미정 정책에 의존하는 Migration을 최종본으로 적용하지 않는다.

## 10. #1 준비 후 실제 적용 순서

1. 최신 `back/develop`과 PR #35 통합 상태를 확인하고 이 준비안을 BE2와 검토한다. 기존 테이블/데이터가 있는지 읽기 전용 조사한 뒤 신규 생성인지 기존 변경인지 구분한다.
2. 필요한 저장 정책·타입·키·실행 도구를 합의하고 API/ERD와 결정 기록에 근거를 남긴다. `public` 노출 여부·DB 역할·접근 권한도 실행환경 담당자와 확인한다.
3. §3 순서로 테이블/키/CHECK, §4 복합 FK, 필요한 인덱스를 버전 관리된 Migration으로 작성한다. 시연 seed는 분리한다. 이미 적용된 파일을 덮어 고치지 않는다.
4. 별도 테스트 DB에 최초 적용하고 카탈로그에서 타입·NULL·PK·UNIQUE·FK·인덱스가 설계와 일치하는지 대조한다. 문서 개수 검사만으로 DB 무결성 검증을 대체하지 않는다.
5. 동일 Migration 재실행에서 이력상 중복 실행이 없고 데이터가 그대로인지 확인한다. 모든 DDL에 `IF NOT EXISTS`를 붙여 스키마 차이를 숨기지 않는다.
6. 후속 Migration, 실패 후 재시도, checksum 변경 감지, 백업/복구를 시험한다. 되돌릴 수 없는 변경은 무조건 down SQL을 실행하는 대신 승인된 복구 계획을 쓴다.
7. [검증 계획](Discushion_MVP_DB_검증계획_2026-10-07.md)의 DB 시험을 실행하고, 후속 기능에 의존하는 항목은 해당 이슈로 연결한다. 실제 test/build와 실행 환경·결과를 기록한다.
8. 사람이 변경을 검토한 뒤 요청하면 commit·push·PR을 준비한다. #3의 실제 완료 조건을 만족하기 전에는 자동 종료 문구나 Issue 종료를 사용하지 않는다.

## 11. 현재 진행 상태

| #3 완료 조건 | 이번 상태 |
| --- | --- |
| A~D 원본·관계·유일성·조회 요구 검토 | 준비 명세 작성. BE2 공동 검토는 아직 없음 |
| 공통 원본·인증 지역·기관 채택·현재 관계·활동 일관성 | DB 제약/원본 관계 구현 및 실제 73개 시험. 권한·개수·깊이·동시성 등 서비스 규칙은 후속 구현 시험 필요 |
| Migration 적용·재실행 검증 | native DB에서 최초 적용·불변 재실행·실패 재시도 확인. 실제 Supabase 확인은 대기 |
| 공개 차단·기록 보존 합의 | 사용자 승인된 소프트 삭제·이력 보존 구조 채택. 실제 구현·팀 검토와 보관 기간·물리 폐기는 남음 |
| FE 계약/영향·실제 연동 | 이번 준비 문서로 API/DTO 변경 없음. 후속 구현 시 영향 확인 필요 |

현재 #3은 부분 진행이며 완료가 아니다. 정적 문서 검사와 실제 애플리케이션 test/build 결과는 함께 제공하는 검증 계획의 실행 기록에 구분한다.

## 12. 최신 기준 반영 후 구현 착수 확인

2026-10-07 작업 재개 시 기존 `back/feature/3-schema`에서 `git pull --ff-only origin back/develop`을 수행했다. 기존 준비 문서 두 개를 보존한 채 `ad8f987`에서 `aedc647`로 fast-forward했다. 이번 원격 변경은 PR #72의 AGENTS.md 작업 순서 추가이며 DB 코드·도구·연결 결정의 추가는 없다.

- GitHub 재조회: #1 열림, #2 닫힘, #3 열림, ERD PR #35 미병합.
- 실제 build 설정: JDBC/PostgreSQL 드라이버는 있으나 Migration 도구·ORM은 아직 없다.
- 현재 PC: `JAVA_HOME`은 Java 25를 가리키며 프로젝트의 Java 17 요구와 다르다. 팀 결정 기록의 다른 실행 결과를 현재 PC 검증 결과로 대체하지 않는다.
- 실제 Supabase 조회 도구와 대상 테스트 DB 설정을 확보하지 못했으며, 운영 DB 접속·변경은 하지 않았다.

재개 시 Migration 도구 선택과 소프트 삭제·이력 보존 제안 채택 여부를 요청했다. 이후 소프트 삭제 구조는 §13에서 사용자 승인됐으며 Migration 실행 도구 선택은 아직 확인 중이다. 증빙 물리 보관 기간은 별도 미정으로 남긴다. 도구와 필요한 DB 실행 환경이 확인되면 §3~5를 실제 Migration과 DB 검증으로 전환한다.

## 13. 사용자 확인 기록 — 삭제 구조 승인

2026-10-07 사용자(BE1)는 “게시물은 삭제 상태로 숨기고, 북마크는 제거하되 투표·활동·채택 이력은 보존하는 구조로 진행해도 될까?”라는 질문에 “그렇게 해”라고 답했다.

- 구현 기준 채택: Post 소프트 삭제, 현재 북마크 제거, 개인 투표·누적 활동·채택 이력 보존. 삭제된 콘텐츠는 모든 공개 경로에서 차단한다.
- 별도 미정 유지: 증빙/파일·개인정보의 구체 보관 기간과 물리 폐기, 삭제 투표의 OPEN/CLOSED 필터 처리. 이 승인을 무기한 보관 또는 DB 실행 승인으로 확대하지 않는다.
- Migration 답변: 사용자는 “supadata를 사용하기로 했는데 이게 Migration 도구맞아?”라고 질문했다. 기존 #1 기록은 Supabase PostgreSQL 선택이며 새 이름의 의미는 확인 필요하다. Supabase 선택과 Supabase CLI의 Migration 실행 방식 선택을 구분하고, 아직 도구 설치·원격 적용은 하지 않는다.
- 공식 안내: [Supabase CLI Migration](https://supabase.com/docs/guides/deployment/database-migrations). Supabase의 PostgreSQL DB를 쓰면서 CLI로 SQL Migration을 관리할 수 있으나, 이번 팀의 CLI 사용 합의를 이미 완료한 것으로 기록하지 않는다.

## 14. Java 설치와 순차 실행 기록

사용자가 Supabase가 맞다고 확인하고 Supabase CLI 방식으로 진행하도록 승인했다. 이어 “먼저 Java 17 설치”와 단계 1~5의 순차 진행을 요청했다.

2026-10-07: 공식 winget 패키지 `EclipseAdoptium.Temurin.17.JDK` 17.0.20.101 추가 설치 완료. 설치 관리자 해시 확인 후 Temurin 17.0.20.1+1 확인. JDK 25는 제거하지 않았다. 검증 프로세스에서 17 경로를 명시했으며, 이후 설치 프로그램이 시스템 JAVA_HOME을 17로 등록한 사실도 확인했다. 초기의 “프로세스 설정만 변경” 설명은 이 설치 효과를 누락한 것이므로 정정한다.

| 순서 | 실제 상태 |
| --- | --- |
| Java 17 설치 | 완료. 기존 Backend 테스트 4개·build 성공 |
| 1. 최신 back/develop 반영 | 완료. #3 브랜치에서 pull, 기준 `aedc647`, Already up to date |
| 2. CLI·로컬 DB 준비 | 일부 완료. CLI 2.120.0 실제 실행, 공식 init 생성. WSL2/Docker 엔진 시작 실패로 DB 준비 중단 |
| 3. Migration 작성 | 미진행. 순차 요청에 따라 DB 환경 해결 후 작성. 빈 CLI 생성 파일은 제거 |
| 4. 실제 적용·재실행·무결성 | 미실행. 문서 검사·Health 테스트로 대체하지 않음 |
| 5. BE2 확인·commit·PR | 미진행. 실제 팀 확인 없이 완료 선언·Issue 종료하지 않음 |

현재 PC에서 펌웨어 가상화 false 및 WSL2 지원 조건 오류, Docker Linux Engine pipe 부재를 확인했다. BIOS 변경·Windows 기능 변경·재부팅은 별도 사용자 조치/확인 없이 하지 않는다. 가상화를 복구할지, Docker 없이 별도 로컬 PostgreSQL로 시험할지 방향을 요청한다. 원격 DB로 우회하지 않았다. 로컬 도구 안내는 [supabase/README](../../supabase/README.md)에 정리했다.

## 15. 별도 로컬 DB 승인 후 구현 결과

사용자가 별도 로컬 PostgreSQL 진행을 승인했다. 공식 Windows 17.11 바이너리로 loopback/SCRAM 전용 native 시험 DB를 준비했고, [Migration](../../supabase/migrations/20261006182228_mvp_schema.sql)에 핵심 27테이블·166컬럼·FK/UNIQUE/CHECK·인덱스·비공개 Schema/RLS를 구현했다. 미정 공유/세션 테이블은 제외했다.

실제 카탈로그의 전체 컬럼/키·단일 참조 대상 47개·복합 FK 8개를 대조했다. DB 시험 73개, API 역할 모의 차단 9개, CLI 최초 적용·재실행 불변·실패 재시도, Java 실제 JDBC 포함 8개와 build가 통과했다. 아직 제품 API·동시성·실제 Supabase platform 검증을 완료한 것이 아니다.

[최신 구현·검증 결과 및 BE2 체크리스트](Discushion_Issue3_DB_구현검증_2026-10-07.md)를 함께 검토한다. private `discushion` Schema/앱 역할과 실제 Supabase 상태는 BE2 확인이 남아 있으며 commit·push·PR·Issue 종료는 하지 않았다.

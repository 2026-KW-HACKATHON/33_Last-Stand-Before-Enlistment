# Discushion API Specification v1.1.2 — 해커톤 MVP

> **#3 DB 설계 후속 승인:** 사용자(BE1)가 Privy subject↔회원 1:1·가입 완료 시각, 사진 업로드 완료 후 24시간 미연결 정리·연결 보호·삭제 대기/재시도 구조를 승인했고 [DB 상세 계약](../architecture/Discushion_Issue3_MVP_v10.2_반영.md)에 기록했다. DB 저장 계약만 승인된 것이며 아래 Privy SDK/토큰 직접 검증·세션·Endpoint/DTO·파일 전송/공개 시점·bytes 환산의 #74 합의 대기는 그대로다. 해당 API나 실제 Storage worker를 구현 완료한 것으로 해석하지 않는다.

## 2026-10-07 결정 반영

사용자가 확정한 아래 변경이 같은 주제의 변경 전 v10.1 본문·예시·MVP 표보다 우선한다. 상세 근거와 남은 계약은 [MVP 결정 변경 기록](../specs/Discushion_MVP_결정변경_2026-10-07.md)을 확인한다. 제품 범위·서비스 선택과 팀 계약 합의·실제 구현/연동 완료를 구분한다. 기능 ID는 유지하며 PRD·기능명세서의 현재 파일명과 참조는 v10.2로 갱신했다.

- **계정:** Privy 이메일 OTP로 인증·로그인한다. 최초 사용자는 필수/선택 동의·프로필·활동 지역을 완료해 로컬 회원으로 가입한다. 자체 비밀번호 설정·확인·로그인과 자체 가입 코드 발급은 대체한다. OTP 세부 제한과 토큰/회원 연결 계약은 #74에서 합의한다. `returnTo`를 전체 과정에서 보존하고 복귀만으로 참여·북마크를 자동 실행하지 않는다.
- **이웃 자격:** 증빙 선택·첨부·신청 제출·접수·실제 인증 처리는 이번 MVP에서 제외한다. 별도 시연용 계정의 완료 지역을 서버에 준비하고 상태 조회·최대 3지역·해당 지역 참여 자격은 유지한다. 일반 가입·Privy 로그인·활동 지역 설정·기관 자격만으로 이웃 자격을 부여하지 않는다. 계정 준비는 #75, 조회 계약은 #74/#11을 따른다.
- **기관 자격:** 기관 증빙 입력·첨부·자료 제출·접수·실제 심사는 이번 MVP에서 제외한다. 별도 시연용 계정의 기관 정본·담당 지역·유효기간/완료 상태를 준비하고 현재 유효 상태 기반 역할·배지·업무 권한은 유지한다. 기관 자격이 주민 참여의 이웃 자격을 대신하지 않는다. 계정 준비는 #75, 조회 계약은 #74/#12를 따른다.
- **사진:** 게시물 사진은 Supabase Storage에 저장하며 회원·게스트·사진 URL을 아는 앱 외부 사람 모두 열람할 수 있다. JPG/PNG·최대 10장·게시물 합계 10MB를 유지한다. 사진 제거·교체 및 게시물 전체 삭제 시 해당 저장 파일을 지운다. 미완료 업로드는 임시 보관 후 24시간이 지나면 정리한다. 업로드 권한·최종 연결·bytes 환산·삭제 보상·24시간 기준시각/정리 간격/경합·임시 파일 공개 시점은 #74/#13/#30에서 합의한다. 이는 게시물 임시저장 기능을 추가한다는 뜻이 아니다.
- **AI:** Google Gemini 3.5 Flash-Lite 선택. 공개 지역 안건 원문 기반 3문장 한 문단·원문 fallback은 유지한다. 실제 API 모델 ID·사용 가능 여부·키/요금·생성/저장/재생성/재시도 기준은 #20/#30에서 확인·합의한다.
- **진행:** 완료된 #2의 변경 후속은 #74, 별도 시연용 계정은 #75. 지역·기관·소유권·게스트 공유 범위는 유지한다. 비밀번호 복구·계정 변경/탈퇴 등 비-MVP 기능은 추가하지 않는다. 증빙 첨부 기능 ID `S-JRMYIV`는 변경 이력으로 유지하되 현재 MVP에서 제외한다.

> **문서 버전** v1.1.2 (2026-10-07 사용자 결정 반영; 변경 경로/DTO 합의 대기, 파일명 유지)\
> **갱신 기준일** 2026-10-07 (Asia/Seoul)\
> **상태** 최신 제품 명세 기반 API 계약 초안. 실제 구현·연동 완료를 뜻하지 않음.  
> **대상** Frontend / Backend / QA  
> **#2 공통 계약 검토** [공통 API 계약 검토표](./Discushion_API_CONTRACT_검토표_2026-10-07.md). 기존안을 정리한 자료이며 FE/BE 합의 완료를 뜻하지 않음.\
> **제품 정본** [PRD v10.2](../specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md), [기능명세서 v10.2](../specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md)\
> **통합 기준** [MVP 통합 지침서](../specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md)\
> **협업 기준** 2026-10-06 통합/프론트엔드/백엔드 Git·GitHub 협업전략

## 0. 목적·근거·변경 이력

이 문서는 가입 → 지역 탐색 → 게시·참여 → 개인 기록 → 기관 채택을 같은 원본 데이터로 구현하기 위한 HTTP API 계약안이다. 기존 명세의 v9 기준과 전체 제품 API를 v10.1의 MVP 범위를 바탕으로 v10.2 결정 변경을 반영했다. 제공 자료에는 실제 Controller/DTO/DB/Frontend 구현이 없으므로 구현이 존재한다고 가정하지 않는다.

### 0.1 표기와 우선순위

- **[확정]**: 제품 문서에서 확정한 기능·권한·제약·MVP 범위.
- **[설계 제안]**: 경로, HTTP 상태, JSON 필드, Enum 코드, multipart 구조, 커서, 공유 컨텍스트 전달 등 구현 계약안. FE/BE가 합의해 고정할 기술 선택이다.
- **[확인 필요]**: 원문에 없는 제품·운영 세부 또는 실제 구현과 대조가 필요한 사항. 제품 정책으로 단정하지 않는다.
- **[비-MVP]**: 최종 제품 정책은 보존하되 이번 필수 UI/API/배치·인수 대상에서 제외한다.

제품 해석은 두 정본의 `확정 정책 보완`, `범위 대조 및 확인 필요`, 최신 MVP 표기 → 기능 ID별 본문 → PRD 사용자 흐름 순으로 적용한다. 기능명세의 일부 포함 항목에 남아 있는 비-MVP 최종 정책을 구현 요구로 확대하지 않는다. 통합 지침과 협업전략의 예시 경로는 확정 경로가 아니며, 본 명세는 가능한 한 기존 v1.0 경로를 유지한다.

### 0.2 변경 이력: v1.0 → v1.1 (당시 범위)

| 항목 | 갱신 내용 |
| --- | --- |
| 근거 | 2026-10-05 v9 → 2026-10-06 v10.1 |
| 범위 | 추천·관심 정보·알림/투표 예약·신고·계정 복구/설정/탈퇴·임시저장 API를 비-MVP로 분리 |
| 게시물 | 참고 자료 링크·익명·알림 예약 필드를 MVP 요청/응답에서 제거 |
| 계정 | 이메일 인증 목적은 MVP에서 SIGN_UP만 사용; 로그아웃은 이번 필수 범위에서 제외 |
| 인증 | 접수와 완료 분리, 이웃 완료 지역·기관 담당 지역/유효기간·파생 배지·권한 응답 보완 |
| 기관 증빙 | 다중 첨부, PDF/JPG/PNG, 파일당 10MB·신청 전체 50MB, 업로드와 최종 제출 구분 |
| AI | 공개 지역 안건 한정, 생성 상태·실패/짧은 원문 fallback·원문/출처 연결 |
| 개인 기록 | 누적 행동 횟수 API 및 +1/+0/재등록 규칙 추가, 현재 유효 참여와 분리 |
| 탐색 | 별도 인기글 선정/14일 노출 운영·자유 게시물 검색·GPS를 MVP 필수에서 제외 |
| 연동 | Part A~D·기능 ID·공통 DTO·검증 및 계약 변경 절차 연결 |

### 0.3 범위

| 영역 | MVP 구현 | 비-MVP / 정책만 유지 |
| --- | --- | --- |
| 계정·지역 | Privy OTP 인증/로그인, 로컬 동의·프로필·활동 지역 | 자체 비밀번호·코드 발급, 복구/계정 변경/탈퇴·다크 모드 |
| 탐색·게시 | 메인·통합 목록·지도·공통 상세, 세 유형 작성/수정/삭제, 선택 사진 | 자유 게시물 검색, 별도 인기글 선정·누적 14일 노출, 임시저장, 참고 자료 링크, 익명 작성, AI 이미지 |
| 참여 | 세 반응, 댓글/답글, 좋아요/싫어요, 정렬, 실제 투표, 북마크 | 신고 전체, 알림·푸시·투표 예약 |
| 개인 기록 | 내가 만든/참여한 게시물, 참여 투표, 북마크, 행동 횟수 | 알림 목록에 의존하는 활동 조회 |
| 인증·기관 | 시연 완료 지역·기관 유효 상태·담당 지역·권한/배지·기관 채택 | 증빙 첨부·신청/제출·접수·실제 심사·보완/증빙 운영 |
| AI·공유 | 공개 안건 3문장 요약, 특정 공개 상세 공유·게스트 댓글 | AI 추천·개인 맞춤·관심 지역/키워드, 게스트 전화번호 인증·문자 알림 |

`정책만 유지`는 API·테이블·예약 작업을 만들라는 뜻이 아니다. 활동 출처와 외부 참여 링크는 MVP이며 공통 참고 자료 링크와 다르다. 기관 인증 1년 유효기간과 만료 시 접근/배지 제거는 MVP 권한 판정에 적용한다.

### 0.4 변경 이력: v1.1 → v1.1.1 (이번 결정 반영 전)

- #2 검토표에 기존 공통 계약안·39개 API/담당 이슈·미정 사항과 확인 역할을 정리했다. FE·BE2 합의 완료를 뜻하지 않는다.
- 사용자(BE1)가 확인한 가입 완료 후 이웃 인증 진입 순서를 §4.3·4.7에 반영했다. 인증 취소/접수 후 복귀 화면의 세부는 FE 확인이 필요하다.
- 제품/통합 근거 링크를 저장소의 실제 docs/specs 경로로 정정했다.
- 기존 Method+Path·숫자형 ID·Bearer 전달·Request/Response·MVP 범위를 유지한다. DB·세션 TTL·provider 등 원본 미정 사항은 여전히 미정이다.

### 0.5 v1.1.1 → v1.1.2 변경 (2026-10-07)

- 제품 정본은 PRD·기능명세서 v10.2다. Privy OTP·시연 자격·사진 공개/파일 삭제/24시간 정리·Gemini 선택을 반영했다.
- 자체 코드/비밀번호 로그인·증빙 POST 예시는 변경 전 이력으로 구분했다.
- 변경된 Method/Endpoint/DTO·토큰·업로드 연결은 #74 합의 대기다. 버전 증가는 문서 개정이며 실제 API 배포/연동 완료를 뜻하지 않는다.

## 1. 공통 규칙

### 1.1 URL·형식·시간 [설계 제안]

| 항목 | 계약안 / 확인 사항 |
| --- | --- |
| Base path | `/api/v1`; 실제 배포 주소는 환경변수로 제공 |
| Content-Type | 기본 application/json. 게시물 사진 직접 업로드/참조 계약은 #74 합의 대기, 증빙 제외. 기존 multipart 예시는 현재 전송 계약이 아님 |
| ID | 본문은 v1.0의 JSON number 예시 유지. PK 타입은 미확정; UUID/string/BIGINT 전환 시 모든 DTO·mock을 함께 변경. JS 안전 정수 초과 ID를 number로 전달하지 않음 |
| 날짜·시각 | 날짜 YYYY-MM-DD, 절대 시각 ISO 8601 offset 포함, 서비스 표시 Asia/Seoul |
| 페이지 | 불투명 cursor + size; nextCursor/hasNext 반환 |
| 페이지 크기·기본 정렬 | 실제 프로젝트에서 합의. 기관/댓글의 확정 정렬 외 수치·기본값을 제품 정책으로 고정하지 않음 |
| null·생략 | 선택 입력은 nullable 명시. 게스트 본인 데이터는 필드 자체를 생략 |

성공 응답:

```json
{ "data": {} }
```

목록 응답:

```json
{ "data": [], "meta": { "nextCursor": null, "hasNext": false } }
```

오류 응답:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "요청값을 확인해 주세요.",
  "details": [{ "field": "title", "reason": "필수 입력값입니다." }],
  "traceId": "server-generated-id"
}
```

공개 DTO는 비밀번호/해시·다른 회원 이메일·기관 증빙 원본·담당자 개인정보·저장 절대 경로·금칙어 목록·AI 원시 응답·stack trace·DB 오류를 반환하지 않는다. 예외적으로 활동 게시물의 문의 이메일은 확정 정책대로 공개한다. 인증 API의 자기 토큰과 자기 신청정보는 해당 본인에게 필요한 범위에서만 반환한다.

### 1.2 HTTP 상태 [설계 제안]

| 상태 | 사용 |
| --- | --- |
| 200 | 조회·수정·desired state 등록/해제 |
| 201 | 가입·게시물·댓글/답글·인증 신청·채택 신규 생성 |
| 204 | 게시물 삭제·채택 취소 등 본문 없는 성공 |
| 400 | 형식/필수값/지원하지 않는 필드·Enum |
| 401 | 미로그인/무효 세션, 게스트의 회원 전용 접근 |
| 403 | 이웃 인증 지역·기관 유효 상태·담당 지역·소유권 부족, 공유 범위 위반 |
| 404 | 없는/삭제된 게시물 또는 조회할 수 없는 대상; 콘텐츠 미반환 |
| 409 | 투표 변경 확인 필요, 종료/현재 상태 충돌, 중복 이메일/닉네임 |
| 413 / 415 | 용량/개수 한도 초과 / 지원하지 않는 파일 형식 |
| 422 | 도메인 입력조건 위반·금칙어 포함 등 |
| 429 | 이메일 발송/입력 한도 초과 |
| 500 / 503 | 내부 오류 / 외부 서비스 장애; 내부 사유 비공개 |

AI 생성 실패는 가능한 한 200 응답의 실패 상태와 원문 fallback으로 전달한다. 통신 자체의 실패만 5xx로 처리하고 FE는 그 경우에도 이미 읽은 원문을 유지한다.

### 1.3 세션·지역·기관 권한

2026-10-07 공통 기반 합의로 **Privy access token의 Bearer 전달과 Spring 직접 검증**을 채택했다. 자체 세션 교환은 채택하지 않는다. 아래 인증 잔여 계약 절은 검증·회원 상태·FE wire의 미정 세부만 구분한다.

```http
Authorization: Bearer <access-token>
```

- **[확정]** 사용자/작성자/소유자는 서버 인증 주체에서 판별한다. Request userId·authorId·역할·배지 플래그를 권한 근거로 쓰지 않는다.
- Privy OTP를 가입/로그인의 인증 제공자로 사용한다. 검증된 Privy subject로 로컬 회원을 조회하고 미가입·가입 미완료·완료를 구분한다. 이메일만 같다고 자동 연결하지 않는다. HTTP 오류/가입 응답의 미정 세부는 아래 #74 인증 잔여 계약에서 제안 상태로 추적한다.
- 게스트는 계정 역할이 아니라 비로그인 공유 상세 컨텍스트다. 유효 공유 컨텍스트가 있어도 회원 전용 권한은 생기지 않는다.
- 지역 게시·댓글/답글·반응·댓글 평가·투표는 대상 지역 이웃 인증 완료 회원만 가능하다. 공유 게스트의 댓글/답글만 예외다.
- 기본 활동 지역 설정, 기관 인증, 프로필의 거주자/학생/직장인/상인 속성은 이웃 인증을 대체하지 않는다.
- 게시물 수정/삭제는 작성자 소유권·대상 지역 자격·유형별 제한을 요청 시점에 확인한다. 기관 채택은 별도 기관 권한을 적용한다.
- 북마크는 로그인 회원이면 가능하며 해당 지역 이웃 인증을 요구하지 않는다.
- 기관 전체 안건 조회는 현재 유효 기관 인증, 채택/취소는 추가로 담당 지역·공개 지역 안건·본인 기관 관계가 필요하다.
- 토큰 검증은 서명·앱 대상/발급자·유효시간을 확인하며 수명은 실제 token의 exp를 따른다. FE는 Privy SDK로 토큰을 얻고 갱신하며 자체 refresh token API를 추가하지 않는다. SDK 저장 설정·앱 검증키/회전·시간 허용오차의 실제 값은 #4/#6~8/#30에서 확인한다. 서버 로그아웃/기기 세션 모델을 MVP 필수로 추가하지 않는다.

### 1.4 공유 컨텍스트·로그인 복귀 [설계 제안]

공유 링크는 공개 게시물 원본에 연결한다. 유효 컨텍스트를 서버가 확인할 전달 방식은 코드와 대조해 합의한다. 이 문서의 제안은 회원의 `GET /posts/{postId}/share-link`가 게시물에 귀속된 shareToken 포함 링크를 반환하고, 공유 화면이 이를 다음 header로 전달하는 방식이다.

```http
X-Post-Share-Token: <server-issued-post-bound-token>
```

서버는 토큰의 게시물과 대상 postId(댓글/답글이면 그 소속 게시물)를 비교하고 현재 공개 상태를 확인한다. 무토큰/다른 게시물 토큰은 공유 접근을 허용하지 않는다. 토큰 유효기간·재발급·서명/저장 모델은 미확정이다. 공개 링크의 전달 가능성은 공유 기능의 성질이며 특정 수신자 신원 인증을 새로 요구하지 않는다.

| 요청 | 회원 | 유효 공유 게스트 |
| --- | --- | --- |
| home/posts 목록/map/개인 기록 | 가능, 개인 기록은 본인 | 401 |
| 대상 게시물 상세·요약·댓글 조회 | 공개 대상 가능 | 공유 대상에 한정 |
| 대상 댓글/답글 생성 | 해당 지역 이웃 완료 | 공유 대상만, 공개명 게스트 |
| 반응/평가/투표/북마크 | 각 회원 권한 조건 | 401, 로그인 안내 |

유효하지 않은 회원 토큰을 보낸 요청을 자동으로 게스트 쓰기로 전환하지 않는다. 로그인한 미인증 회원이 공유 토큰을 보내도 회원 지역 권한 검증을 우회하지 않는다.

`returnTo`는 FE가 가입·프로필·지역 설정까지 유지하는 내부 상세 복귀 정보다. 서버가 받는 구현에서는 허용된 내부 경로만 검증한다. 로그인/가입 성공은 원 상세 또는 메인으로 복귀하는 것까지이며 반응·평가·투표·북마크·게스트 댓글 이관을 자동 실행하지 않는다. 메인/지도/개인 목록의 지역·유형·주제·커서·정렬·스크롤 맥락도 FE가 보존한다.

## 2. Enum·오류·공통 DTO

### 2.1 제품 의미가 확정된 Enum (코드명은 설계 제안)

| Enum | 값 |
| --- | --- |
| PostType | LOCAL_AGENDA / LOCAL_ACTIVITY / VOTE |
| PostTopic | TRANSPORTATION / HOUSING / SAFETY / WELFARE / LIVING_INFORMATION / ENVIRONMENT / OTHER |
| PostStatus | PUBLISHED / DELETED |
| ActivityStatus | SCHEDULED / IN_PROGRESS / ENDED / CANCELED |
| VoteStatus | OPEN / CLOSED (서버 시각과 endsAt으로 파생) |
| PostReactionType | EMPATHY / NEEDED / CURIOUS |
| CommentSort | LIKES / LATEST |
| CommentEvaluationType | LIKE / DISLIKE |
| ResidentAttribute | RESIDENT / STUDENT / WORKER / MERCHANT (복수 속성, 역할 아님) |
| EmailVerificationPurpose | SIGN_UP (MVP만) |

`ALL`은 저장 유형/주제가 아니다. 전체 필터는 type/topic 생략으로 전달한다. `scope=ALL|ADOPTED`와 투표 목록 `status=ALL|OPEN|CLOSED`는 조회 전용 값이다.

### 2.2 상태 코드 제안과 미확정 경계

| 대상 | 계약 제안 | 의미 / 주의 |
| --- | --- | --- |
| 이웃 신청 status | RECEIVED / COMPLETED | 접수 / 완료. 신청 전은 requests 빈 목록; 완료 지역은 별도 verifiedRegions |
| 기관 상태 status | NOT_SUBMITTED / RECEIVED / COMPLETED / EXPIRED | 미신청 / 접수 / 완료 / 만료. isActive는 현재 상태·유효기간으로 파생 |
| AI status | PENDING / SUCCEEDED / FAILED / SOURCE_TOO_SHORT | 생성 중 / 완료 / 실패 / 짧은 원문; 후자의 3개 외 상태 의미는 합의 필요 |
| 공개 접근 availability | AVAILABLE / UNAVAILABLE | 삭제 투표 기록 등의 콘텐츠 노출 가능 여부 |
| 게스트 context | SHARED_GUEST | 계정 역할 Enum으로 저장하지 않음 |

이 상태의 영문 문자열은 제품이 지정한 정본이 아니다. 기존 코드가 APPROVED를 완료 상태로 쓰면 동등 의미를 매핑해 FE/BE가 함께 고정한다. MVP에 실제 반려·보완·심사 workflow를 추가하지 않는다. AI 저장/재생성/폴링 간격·짧은 원문 판정 기준은 미확정이며 임의 수치로 고정하지 않는다.

### 2.3 MVP 오류 코드 [설계 제안]

| 그룹 | 코드 |
| --- | --- |
| 공통/Auth | VALIDATION_ERROR, UNAUTHORIZED, LOGIN_FAILED, EMAIL_ALREADY_IN_USE, EMAIL_VERIFICATION_INVALID, EMAIL_VERIFICATION_EXPIRED, EMAIL_VERIFICATION_LIMIT_EXCEEDED, EMAIL_DELIVERY_FAILED, PASSWORD_POLICY_VIOLATION, REQUIRED_AGREEMENT_MISSING |
| User/Region | USER_NOT_FOUND, NICKNAME_ALREADY_IN_USE, REGION_NOT_FOUND, NEIGHBOR_VERIFICATION_REQUIRED, NEIGHBOR_VERIFICATION_LIMIT_EXCEEDED |
| 공유 | SHARE_CONTEXT_REQUIRED, SHARE_CONTEXT_INVALID, SHARE_SCOPE_MISMATCH |
| Post/Media | POST_NOT_FOUND, POST_DELETED, POST_NOT_EDITABLE, POST_NOT_DELETABLE, POST_TYPE_INVALID, POST_TOPIC_INVALID, ACTIVITY_INFO_REQUIRED, ACTIVITY_STATUS_REQUIRED, VOTE_OPTIONS_INVALID, MEDIA_LIMIT_EXCEEDED, UNSUPPORTED_MEDIA_TYPE |
| 참여 | COMMENT_NOT_FOUND, COMMENT_FORBIDDEN_WORD, COMMENT_DEPTH_EXCEEDED, REACTION_TYPE_INVALID, COMMENT_EVALUATION_TYPE_INVALID, VOTE_OPTION_INVALID, VOTE_ENDED, VOTE_CHANGE_CONFIRMATION_REQUIRED |
| 기관 | INSTITUTION_VERIFICATION_NOT_ACTIVE, ADOPTION_NOT_ALLOWED, ADOPTION_NOT_FOUND |
| AI | AI_SUMMARY_NOT_APPLICABLE, AI_SUMMARY_UNAVAILABLE |

UI에서 확정 문구를 우선 사용한다: 로그인 `로그인에 실패했습니다`, 게스트 회원 기능 `로그인이 필요한 기능입니다.`, 코드 불일치 `인증번호가 일치하지 않습니다.`, 만료 `인증번호가 만료되었습니다. 다시 발급받아 주세요.`. 세부 오류를 로그인 실패 원인 구분에 사용하지 않는다.

### 2.4 공통 PostCard [설계 제안]

```json
{
  "id": 101,
  "type": "LOCAL_AGENDA",
  "topic": "TRANSPORTATION",
  "title": "광운대역 주변 보행 환경 개선",
  "excerpt": "게시물 본문 미리보기",
  "region": { "id": 15, "name": "월계1동" },
  "author": { "displayName": "동네주민", "profileImageUrl": null, "institutionVerified": false },
  "thumbnailUrl": null,
  "reactionCounts": { "EMPATHY": 3, "NEEDED": 2, "CURIOUS": 1, "total": 6 },
  "commentCount": 4,
  "createdAt": "2026-10-06T13:20:00+09:00",
  "updatedAt": "2026-10-06T13:20:00+09:00"
}
```

타입별 카드에는 activityStatus 또는 vote status/endsAt/participantCount를 같은 원본에서 더한다. 사진이 없으면 thumbnailUrl=null, 상세 images=[]이며 UI는 이미지 영역을 생략한다. excerpt는 일반 본문 미리보기이며 모든 유형을 AI 요약 대상으로 만들지 않는다. 작성자 표시/배지는 조회 시 최신 공개 프로필과 현재 유효 기관 인증을 참조한다.

## 3. MVP API 목록

아래 모든 경로는 Base path 뒤에 붙이며 **[설계 제안]**이다. 표의 `기능 ID`는 최신 기능명세를 추적한다. 변경 전 묶음은 인프라 포함 39개였다. 자체 이메일 코드·비밀번호 로그인·증빙 POST는 현재 MVP에서 제외한다. Privy 연결·가입·사진 직접 업로드의 변경 Method/Path/DTO와 최종 개수는 #74 합의 후 확정한다. 아래 변경 대상 행을 확정 계약으로 사용하지 않는다.

| Part / Domain | Method | Endpoint | 목적 | 기능 ID |
| --- | --- | --- | --- | --- |
| 공통 Infra | GET | `/health` | 기본 상태 | 인프라 |
| A Auth | 합의 대기 | #74에서 확정 | Privy 인증 기반 로컬 최초 가입/회원 연결 | F-KZRSXU, F-RBVFZX |
| A Auth | 합의 대기 | #74에서 확정 | Privy 인증/로컬 회원 세션 연결 | F-TSOXGG |
| A User | GET/PATCH | `/users/me` | 프로필·지역·권한 조회/수정 | F-RBVFZX, F-QQKYLC |
| A Region | GET | `/regions` | 활동/인증 지역 선택 후보 | F-QQKYLC, F-ATWJDJ |
| A Verification | GET (기존 제안) | `/users/me/neighbor-verifications` | 본인 완료 지역/자격 조회; DTO #74 대조 | F-ATWJDJ |
| A Institution | GET (기존 제안) | `/institution-verifications` | 본인 기관 유효 상태/담당 지역 조회; DTO #74 대조 | F-OPNIXL, S-YLSPHQ |
| B Home | GET | `/home` | 현재 탐색 지역 메인 | F-UPRLMN |
| B Map | GET | `/map/dongs` | 동별 대표 게시물 | F-QIGKAK |
| B Post | GET/POST | `/posts` | 통합 목록/세 유형 게시 | F-EAJPVC, F-FTLHCX, S-TBFIHO |
| B Post | GET/PATCH/DELETE | `/posts/{postId}` | 상세/수정/삭제 | F-PUDHYO, F-UCDVNA, F-FTLHCX |
| B AI | GET | `/posts/{postId}/summary` | 공개 안건 요약·원문 | F-WSCKDN |
| C Share | GET | `/posts/{postId}/share-link` | 공유 대상 연결 링크 | F-OWFYWE, S-NYUECP |
| C Comment | GET/POST | `/posts/{postId}/comments` | 부모+답글 조회/댓글 등록 | F-EDNVWZ, S-JCEZAP, S-OXTKEP |
| C Comment | POST | `/comments/{commentId}/replies` | 원 부모 아래 답글 | S-YYDGUS |
| C Reaction | PUT/DELETE | `/posts/{postId}/reactions/{reactionType}` | 독립 반응 등록/취소 | F-GOMLGG, S-HNVDPO |
| C Evaluation | PUT/DELETE | `/comments/{commentId}/evaluation` | 좋아요/싫어요 등록/전환/취소 | F-CDIBRF |
| C Vote | PUT | `/posts/{postId}/vote` | 실제 선택 제출/교체 | F-FCPVIS, S-CMGJIG |
| D Bookmark | PUT/DELETE | `/posts/{postId}/bookmark` | 회원 저장/해제 | F-FYQJPT |
| D Adoption | GET | `/officer/agendas` | 전체 안건/현재 기관 채택 목록 | F-CNNPYL, S-PCCNUU |
| D Adoption | POST | `/posts/{postId}/adoptions` | 담당 지역 안건 채택 | F-TUGMEP, S-AQOBIE |
| D Adoption | DELETE | `/posts/{postId}/adoptions/{adoptionId}` | 본인 기관 채택 취소 | F-TUGMEP |
| D Activity | GET | `/users/me/posts` | 내가 만든 게시물 | F-SSHXAA |
| D Activity | GET | `/users/me/participations` | 게시물별 현재 유효 내 행동 | F-NZTUYE |
| D Activity | GET | `/users/me/votes` | 실제 참여 투표/선택/결과 | F-QPGNCF |
| D Activity | GET | `/users/me/bookmarks` | 북마크 유형/주제 목록 | F-FYQJPT |
| D Activity | GET | `/users/me/activity` | 누적 개인 행동 횟수 | F-WYMXXP |

변경 전 39개/38개 경로 수는 이력이다. 현재 API 수는 Privy 연결·가입·직접 업로드 및 증빙 POST 제외의 최종 계약이 합의된 뒤 다시 계산한다.

## 4. Auth·프로필·지역·인증

### 4.1 Health

`GET /health`는 인증 불필요, 200 `{ "data": { "status": "UP" } }`. AI 호출/무거운 집계는 실행하지 않는다.

### 4.2 이메일 인증 발송·확인

**2026-10-07 적용 기준**

**현재 범위: Privy 이메일 OTP 인증.**

자체 코드 발송/확인 API·verificationToken·고정 OTP 수치는 현재 구현 계약에서 대체한다. Privy SDK/API 연결과 검증 방법·재시도/오류·provider 실제 제한은 #74/#6에서 확인·합의한다. 아래 변경 전 경로/DTO는 구현하지 않는다.

<details>
<summary>변경 전 v10.1 / API 초안 — 이번 MVP에 적용하지 않음</summary>

아래 내용은 변경 이력 보존용이다. 현재 동작·완료 조건은 위의 2026-10-07 기준을 적용한다.

```http
POST /api/v1/auth/email-verifications
POST /api/v1/auth/email-verifications/confirm
```

발송 request/response:

```json
{ "email": "user@example.com", "purpose": "SIGN_UP" }
```

```json
{
  "data": {
    "expiresAt": "2026-10-06T13:25:00+09:00",
    "resendAvailableAt": "2026-10-06T13:21:00+09:00"
  }
}
```

확인 request/response:

```json
{ "email": "user@example.com", "purpose": "SIGN_UP", "code": "123456" }
```

```json
{ "data": { "verificationToken": "server-issued-sign-up-proof", "verifiedAt": "2026-10-06T13:21:12+09:00" } }
```

- 인증번호는 선행 0을 유지하는 6자리 문자열. 유효 5분, 재발송 대기 60초, 수신 이메일 기준 30분 최대 5회, 코드당 입력 최대 5회 **[확정]**.
- 새 코드 재발급 성공 시 이전 코드를 즉시 무효화하고 FE 인증 성공 상태도 다시 확인하도록 갱신한다. 발송 실패를 발송/인증 성공으로 처리하지 않는다.
- 추가 계정 잠금시간을 임의로 만들지 않는다. 코드·발송 한도를 서버에서 검증한다.
- verificationToken은 해당 이메일/SIGN_UP에 묶인 인증 증명이며 로그인 토큰이 아니다. TTL/형식·일회 소비와 재발급 시 증명 폐기는 **[설계 제안/구현 합의 필요]**.
- EMAIL_CHANGE/PASSWORD_CHANGE/PASSWORD_RESET 목적은 비-MVP이며 가입 요청에 우회 사용하지 않는다.

</details>

### 4.3 가입

**2026-10-07 적용 기준**

Privy 이메일 OTP로 인증·로그인한다. 최초 사용자는 필수/선택 동의·프로필·활동 지역을 완료해 로컬 회원으로 가입한다. 자체 비밀번호 설정·확인·로그인과 자체 가입 코드 발급은 대체한다. OTP 세부 제한과 토큰/회원 연결 계약은 #74에서 합의한다. `returnTo`를 전체 과정에서 보존하고 복귀만으로 참여·북마크를 자동 실행하지 않는다.

검증된 Privy 식별자와 로컬 회원의 관계·중복 처리·약관·프로필·활동 지역 저장·가입 미완료 상태 및 Request/Response/세션 방식은 #74/#7에서 정한다. 기존 password/passwordConfirmation/emailVerificationToken은 현재 필수 입력으로 사용하지 않는다. 닉네임/소개 등 변경하지 않은 프로필 제품 제약은 유지한다.

공통 기반의 직접 Bearer 검증·subject 1:1 연결·가입 상태 구분은 이미 채택됐다. 남은 가입 wire/이메일 출처·오류 매핑과 검증 기준은 문서 끝의 **#74 인증 잔여 계약 정리**를 따른다. 아래 변경 전 세션/accessToken 응답은 현재 구현 계약이 아니다.

<details>
<summary>변경 전 v10.1 / API 초안 — 이번 MVP에 적용하지 않음</summary>

아래 내용은 변경 이력 보존용이다. 현재 동작·완료 조건은 위의 2026-10-07 기준을 적용한다.

`POST /auth/sign-up`, 201. 사진 없는 기본 요청은 JSON이며 아래 객체를 사용한다.

```json
{
  "email": "user@example.com",
  "emailVerificationToken": "...",
  "password": "abc12345",
  "passwordConfirmation": "abc12345",
  "agreements": { "termsOfService": true, "privacyCollection": true, "marketing": false },
  "profile": {
    "nickname": "동네주민",
    "bio": "우리 동네를 더 좋게",
    "residentAttributes": ["RESIDENT", "STUDENT"],
    "activityRegionId": 15
  }
}
```

프로필 사진이 있으면 **[설계 제안]** multipart의 `payload` JSON part에 같은 객체, `profileImage`에 파일을 보내는 대체 형식을 사용한다. Base64/임의 원격 URL을 저장하지 않는다. 프로필 사진의 허용 형식·개수·용량·교체/삭제 보관 규칙은 **[확인 필요]**이며 게시물 사진 규칙을 자동 전용하지 않는다.

가입은 이메일 인증 → 비밀번호/확인 → 동의 → 프로필 → 활동 지역 완료 순서다. 서버는 필수 이용약관·개인정보 수집/이용 동의, 비밀번호 8~64자·영문+숫자·공백 불가·확인 일치, 닉네임 중복 불가/최대 10자, 소개 최대 50자, 선택 지역 존재를 검증한다. 특수문자 필수 아님. 가입·프로필·기본 지역 저장은 실패 시 부분 완료를 남기지 않는 방식으로 처리한다 **[설계 제안]**.

```json
{ "data": { "user": { "id": 23, "nickname": "동네주민", "activityRegion": { "id": 15, "name": "월계1동" } }, "accessToken": "..." } }
```

세션 제공 방식은 로그인과 통일한다. 일반 가입 완료 후 returnTo 원 상세/없으면 메인으로 FE가 이동한다. 가입했다고 이웃/기관 인증을 자동 완료하거나 과거 게스트 의견을 회원에게 이관하지 않는다.

가입 프로필 단계에서 이웃 인증하기를 선택한 경우에는 가입 입력·기본 활동 지역·returnTo·인증 진입 의도를 유지하고, 가입·활동 지역 설정을 완료해 회원과 세션이 생성된 다음 이웃 인증 신청 화면으로 연결한다. 가입 전에 인증 신청이나 증빙을 회원 소유 데이터로 저장하지 않는다. 가입 완료 후 인증 진입 순서는 **[사용자(BE1) 확인: 2026-10-07]**이며 FE·BE2의 실제 구현 확인은 별도다. 인증 취소/접수 후 어느 화면으로 복귀할지는 FE와 고정하고 이미 완료한 가입 및 원 게시물 복귀 정보를 보존한다.

</details>

### 4.4 로그인

**2026-10-07 적용 기준**

Privy 이메일 OTP로 인증·로그인한다. 최초 사용자는 필수/선택 동의·프로필·활동 지역을 완료해 로컬 회원으로 가입한다. 자체 비밀번호 설정·확인·로그인과 자체 가입 코드 발급은 대체한다. OTP 세부 제한과 토큰/회원 연결 계약은 #74에서 합의한다. `returnTo`를 전체 과정에서 보존하고 복귀만으로 참여·북마크를 자동 실행하지 않는다.

기존 회원과 로컬 가입 미완료를 구분한다. 세션 연결·만료/갱신·오류·Method/Path는 #74/#8에서 합의하고 검증된 인증 주체만 사용한다. 비밀번호 로그인 Request는 현재 계약이 아니다.

자체 세션 연결 방식의 선택은 종료됐으며 Privy Bearer 직접 검증을 사용한다. 앱 로그인 endpoint의 입력/회원 상태 응답은 아래 #74 인증 잔여 계약의 검토안으로 구분한다. 기존 비밀번호 입력·자체 accessToken 발급 응답은 변경 전 이력이다.

<details>
<summary>변경 전 v10.1 / API 초안 — 이번 MVP에 적용하지 않음</summary>

아래 내용은 변경 이력 보존용이다. 현재 동작·완료 조건은 위의 2026-10-07 기준을 적용한다.

`POST /auth/login`, 200. Request `{ "email": "user@example.com", "password": "abc12345" }`, 성공 응답은 가입과 같은 세션/회원 구조를 사용한다. 실패는 401 LOGIN_FAILED와 `로그인에 실패했습니다`로 통일하고 이메일 존재/비밀번호 오류 원인을 구분하지 않는다. returnTo 복귀 직후 원 행동을 실행하지 않는다.

</details>

### 4.5 내 프로필 조회·수정

```http
GET /api/v1/users/me
PATCH /api/v1/users/me
```

본인 회원만, 200. 응답 제안:

```json
{
  "data": {
    "id": 23,
    "email": "user@example.com",
    "profile": {
      "nickname": "동네주민",
      "bio": "우리 동네를 더 좋게",
      "profileImageUrl": null,
      "residentAttributes": ["RESIDENT", "WORKER"],
      "activityRegion": { "id": 15, "name": "월계1동" }
    },
    "neighborVerifiedRegions": [{ "id": 15, "name": "월계1동" }],
    "institutionVerification": {
      "status": "COMPLETED",
      "institutionName": "노원구청",
      "responsibleRegion": { "id": 15, "name": "월계1동" },
      "completedAt": "2026-10-06T13:00:00+09:00",
      "validUntil": "2027-10-06T13:00:00+09:00",
      "isActive": true
    },
    "institutionVerified": true
  }
}
```

PATCH의 허용 필드는 nickname/bio/residentAttributes/activityRegionId. 사진 변경은 가입과 같은 multipart `payload`+`profileImage` 제안이며 사진 제거는 payload `removeProfileImage=true`; 교체와 제거 동시 요청은 400 **[설계 제안]**. 이메일/비밀번호/interestKeywords/기관 역할·배지·완료 지역은 여기서 수정하지 않는다. 성공 시 최신 프로필을 반환하고 비익명 작성자 표시는 같은 프로필을 재조회한다. 취소/실패면 저장 전 값 유지.

### 4.6 지역 후보

`GET /regions?cursor=...&size=...`, 인증 예외로 가입 초기 지역 선택도 허용 **[설계 제안]**. 등록된 지역 ID/name과 동 단위 지도 연계 정보만 제공하며 게시물 목록 탐색 권한을 부여하지 않는다. 필요 시 지역명 `q`를 후보 조회 보조 필터로 합의할 수 있지만 게시물 자유 검색·latitude/longitude·현재 위치 권한 수집은 MVP 필수가 아니다. 저장 지역은 후보에서 선택한 ID를 가입 또는 PATCH /users/me에 전달한다. Region 계층/경계·지도 좌표 원천은 실제 데이터 준비 시 합의한다.

### 4.7 이웃 인증

**2026-10-07 적용 기준**

증빙 선택·첨부·신청 제출·접수·실제 인증 처리는 이번 MVP에서 제외한다. 별도 시연용 계정의 완료 지역을 서버에 준비하고 상태 조회·최대 3지역·해당 지역 참여 자격은 유지한다. 일반 가입·Privy 로그인·활동 지역 설정·기관 자격만으로 이웃 자격을 부여하지 않는다. 계정 준비는 #75, 조회 계약은 #74/#11을 따른다.

기존 GET 경로는 조회 제안으로 대조한다. 신청/접수/증빙 목록 중심 DTO는 그대로 유지하지 않으며 완료 지역/현재 자격 조회 계약은 #74에서 고정한다. 시연 데이터 설정은 일반 사용자 공개 API로 노출하지 않는다.

<details>
<summary>변경 전 v10.1 / API 초안 — 이번 MVP에 적용하지 않음</summary>

아래 내용은 변경 이력 보존용이다. 현재 동작·완료 조건은 위의 2026-10-07 기준을 적용한다.

```http
GET /api/v1/users/me/neighbor-verifications
POST /api/v1/users/me/neighbor-verifications
```

본인 회원만. 가입 프로필에서 선택한 인증 진입도 가입 완료로 생성된 회원 세션 이후에 신청한다(§4.3). 접수는 인증 완료나 가입 권한 추가를 뜻하지 않는다. POST multipart: `regionId` + `evidenceFiles` 파일 part **[설계 제안]**. 사용자가 증빙을 선택/첨부하는 동안 FE 제출 큐에만 보관하며 `신청 제출` 때 파일과 함께 요청한다. 201 접수 응답 제안:

```json
{ "data": { "id": 501, "region": { "id": 15, "name": "월계1동" }, "status": "RECEIVED", "submittedAt": "2026-10-06T14:00:00+09:00" } }
```

GET 200 응답은 `{ "data": { "requests": [], "verifiedRegions": [] } }` 형태로 본인 신청지역/접수·완료 상태/제출 시각/증빙 파일명·크기와 완료 지역을 분리해 반환한다. 상태·완료 지역을 영속 저장하고 재조회해 유지한다. 상세 증빙 참조는 비공개이며 본인 외 조회 불가.

완료 지역은 최대 3개 **[확정]**. 접수는 완료가 아니고 참여 권한을 주지 않는다. 한도는 신청 수 제한으로 바꾸지 않고 완료 지역 등록 시 서버/시연 설정에서도 검증한다. 개발자 시연용 완료 설정은 별도 안전한 seed/script 등으로 가능하며 사용자용 승인 API를 만들지 않는다.

거주 증빙 인정 종류·파일 형식/개수/용량·반려 후 재신청·보관은 **[확인 필요]**. 상태/완료 지역 저장·지역 권한 구현을 제외하는 근거로 사용하지 않는다. 기관 증빙의 확정 제한을 이웃 증빙에 그대로 적용하지 않는다.

</details>

### 4.8 기관 인증

**2026-10-07 적용 기준**

기관 증빙 입력·첨부·자료 제출·접수·실제 심사는 이번 MVP에서 제외한다. 별도 시연용 계정의 기관 정본·담당 지역·유효기간/완료 상태를 준비하고 현재 유효 상태 기반 역할·배지·업무 권한은 유지한다. 기관 자격이 주민 참여의 이웃 자격을 대신하지 않는다. 계정 준비는 #75, 조회 계약은 #74/#12를 따른다.

기존 GET 경로는 조회 제안으로 대조한다. 본인 현재 유효 상태·담당 지역·유효기간·기관 표시만 필요한 계약을 #74에서 고정한다. 증빙 POST/파일 DTO는 현재 MVP에서 제외한다.

<details>
<summary>변경 전 v10.1 / API 초안 — 이번 MVP에 적용하지 않음</summary>

아래 내용은 변경 이력 보존용이다. 현재 동작·완료 조건은 위의 2026-10-07 기준을 적용한다.

```http
GET /api/v1/institution-verifications
POST /api/v1/institution-verifications
```

본인 회원만. 로그인 화면의 기관 인증 진입과 마이페이지는 같은 요청/결과를 사용한다. 비로그인 진입은 로그인/가입 후 같은 인증 흐름으로 연결하며 게스트 신청을 허용하지 않는다.

POST multipart Form:

```text
institutionName, departmentName, positionName, applicantName,
workEmail, phoneNumber, responsibleRegionId,
employmentCertificates (반복 파일 part)
```

- 기관명·부서·직책·담당자 이름·업무 이메일·전화번호·담당 지역·재직증명서 필수 **[확정]**.
- PDF/JPG/PNG, 파일 개수 제한 없음, 파일 1개 최대 10MB, 신청 전체 첨부 합계 최대 50MB **[확정]**. 요청 본문 overhead를 파일 합산에 혼동하지 않는다.
- 파일명·크기·제거를 FE 큐에서 관리한다. 첨부/업로드와 최종 제출을 구분하고 `자료 제출` 클릭 때 같은 신청의 모든 선택 파일을 제출한다. 별도 최종 확인 화면 없음. 선업로드를 도입하면 소유자별 비공개 참조·최종 제출 연결 계약을 먼저 추가해야 하며, 업로드만으로 접수/완료 처리하지 않는다.
- 업무 이메일은 심사용 입력이며 별도 6자리 인증 없음. 계정 로그인 이메일과 구분.
- 단순 기관 소속 학생/직원이 아닌 실제 민원 업무 담당자 대상이라는 정책 유지. 시연용 완료 설정도 이 의미를 따른다.

201 응답 제안:

```json
{
  "data": {
    "id": 601,
    "status": "RECEIVED",
    "responsibleRegion": { "id": 15, "name": "월계1동" },
    "submittedAt": "2026-10-06T14:00:00+09:00",
    "files": [{ "id": 701, "name": "재직증명서.pdf", "sizeBytes": 800000 }],
    "isActive": false,
    "institutionVerified": false
  }
}
```

GET은 본인 제출 정보·파일명/크기·신청상태와 현재 기관 인증 completedAt/validUntil/담당 지역을 반환한다. 접수·첨부만으로 역할/파란 배지 부여 금지. 유효 완료 상태에서만 역할·배지·기관 접근을 파생한다. 승인일부터 1년, 만료 시 현재 및 과거 작성물의 배지·기관 접근 제거, 일반 회원 계정/게시물 유지. 실제 심사/승인/반려 UI·보완 메일·만료 증빙 삭제/반려 후 30일 삭제 배치는 비-MVP다.

</details>

## 5. Home·Map·Post·AI·Share

### 5.1 지역 메인

`GET /home?regionId=15`, 로그인 회원, 200. 미지정 시 프로필 기본 활동 지역을 사용하며 지정 regionId는 임시 탐색 기준으로만 적용한다.

```json
{
  "data": {
    "region": { "id": 15, "name": "월계1동" },
    "posts": [],
    "openVotes": [],
    "boardCounts": { "LOCAL_AGENDA": 0, "LOCAL_ACTIVITY": 0, "VOTE": 0 }
  }
}
```

posts/openVotes는 공통 카드와 같은 원본 ID/집계를 사용한다. 빈 상태에서도 섹션·게시판 CTA를 유지하고 `아직 등록된 게시물이 없습니다.`를 표시한다. 메인의 노출 순서/건수는 FE/BE 기술 계약으로 합의한다. `trendingPost` 전용 API·인기도=반응+댓글+답글·누적 노출 14일 운영은 보조 정책만 보존하며 이번 완료 조건으로 요구하지 않는다. 추천 카드·알림 데이터 의존 없음.

### 5.2 이슈 지도

`GET /map/dongs?centerRegionId=15`, 로그인 회원, 200. 최초 미지정 시 기본 활동 지역 중심. 표시할 동 집합/경계·좌표 데이터 원천은 **[확인 필요]**.

```json
{
  "data": {
    "centerRegion": { "id": 15, "name": "월계1동" },
    "dongs": [
      {
        "region": { "id": 15, "name": "월계1동" },
        "representativePost": { "id": 101, "type": "LOCAL_AGENDA", "title": "보행 환경 개선", "reactionCount": 6, "thumbnailUrl": null }
      },
      { "region": { "id": 18, "name": "월계2동" }, "representativePost": null }
    ]
  }
}
```

대표는 각 동의 공개 지역 안건/투표 중 반응 합계 최대 1건, 동률 최신 게시물이다. 활동 정보·댓글 수·득표 수를 선정에 사용하지 않는다. 사진 있으면 첨부 순서 첫 사진만 thumbnailUrl, 없으면 null/이미지 영역 생략. 후보 없는 동도 유지하고 선택 시 `등록된 게시물이 없습니다` 표시. 반응 변경 후 재조회하면 최신 대표를 반환한다. GPS·실시간 위치 추적·지도 표시용 위치 권한 불필요. 확대/축소와 오류/재시도는 FE 구현 책임.

### 5.3 통합 목록·상세

```http
GET /api/v1/posts?regionId=15&type=LOCAL_AGENDA&topic=TRANSPORTATION&cursor=...&size=...
GET /api/v1/posts/{postId}
```

목록은 회원 전용이며 지역 → 유형 → 주제 조건을 적용하고 목록 응답은 PostCard 배열이다. 전체 유형/주제는 query 생략. regionId 미지정 기본은 프로필 활동 지역 **[설계 제안]**. q 자유 게시물 검색은 MVP 계약에 포함하지 않는다. 상세는 회원 또는 유효 공유 게스트에게만 공개한다.

상세 응답 제안(기관 채택은 지역 안건에서만 표시):

```json
{
  "data": {
    "id": 101,
    "type": "LOCAL_AGENDA",
    "topic": "TRANSPORTATION",
    "title": "광운대역 주변 보행 환경 개선",
    "content": "게시물 공개 원문",
    "status": "PUBLISHED",
    "region": { "id": 15, "name": "월계1동" },
    "author": { "displayName": "동네주민", "profileImageUrl": null, "institutionVerified": false },
    "images": [],
    "reactionCounts": { "EMPATHY": 3, "NEEDED": 2, "CURIOUS": 1, "total": 6 },
    "commentCount": 0,
    "comments": [],
    "commentsMeta": { "sort": "LATEST", "nextCursor": null, "hasNext": false },
    "adoptions": [{ "institutionName": "노원구청", "adoptedAt": "2026-10-06T14:00:00+09:00" }],
    "myState": { "reactions": ["EMPATHY"], "isBookmarked": true },
    "capabilities": {
      "canComment": true,
      "canReact": true,
      "canEvaluateComment": true,
      "canVote": false,
      "canBookmark": true,
      "canEdit": false,
      "canDelete": false,
      "canAdopt": false,
      "canCancelAdoption": false
    },
    "createdAt": "2026-10-06T13:20:00+09:00",
    "updatedAt": "2026-10-06T13:20:00+09:00"
  }
}
```

comments 초기 페이지와 후속 GET comments는 동일 구조/정렬을 사용한다. 초기 기본 정렬/건수는 합의해야 한다. 게스트는 myState·vote.myOptionId·댓글 myEvaluation 필드를 생략한다. capabilities는 공유 대상 댓글/답글만 true, 회원 행동은 false로 반환한다. UI 가드는 서버의 매 요청 재검증을 대체하지 않는다.

LOCAL_ACTIVITY는 activity를 추가:

```json
{
  "source": "노원구청",
  "schedule": "2026-10-12 14:00",
  "place": "월계동 주민센터",
  "status": "SCHEDULED",
  "externalParticipationUrl": null,
  "externalParticipationEnabled": false,
  "organizerEmail": "user@example.com"
}
```

schedule 저장 형식은 **[설계 제안]**인 문자열이며 상세 일정 구조/길이는 실제 계약에서 합의한다. 문의 이메일은 작성자 현재 등록 로그인 이메일에서 조회하고 회원·공유 게스트에게 표시한다. 없으면 null과 `주최자 문의 정보를 확인할 수 없습니다.` 표시. 외부 링크가 없거나 유효하지 않은 경우와 문의 이메일 없음은 별개다. 종료/취소 상태면 링크를 비활성화하고 예정/진행으로 되돌리면 유효한 링크가 있을 때 재활성화한다. 링크 허용 protocol·유효성 검사는 **[설계 제안]**, 내부 신청/결제 API는 없음.

VOTE는 vote를 추가:

```json
{
  "question": "설치에 동의하시나요?",
  "status": "OPEN",
  "endsAt": "2026-10-10T18:00:00+09:00",
  "participantCount": 11,
  "myOptionId": 1,
  "options": [
    { "id": 1, "content": "찬성", "voteCount": 8, "votePercentage": 72.73 },
    { "id": 2, "content": "반대", "voteCount": 3, "votePercentage": 27.27 }
  ]
}
```

회원 myOptionId는 실제 본인 선택이며 미참여면 null, 게스트는 필드 생략. 다른 참여자의 개인 선택/이름/식별자별 선택은 반환하지 않는다. 득표율 소수 자릿수·반올림은 **[설계 제안]**이며 합의해 통일한다. 참여 0명일 때 각 득표율 0을 반환하는 안을 사용한다. 지역 안건에는 활동/투표 진행 상태를 만들지 않고 기관 채택도 PostStatus에 넣지 않는다.

### 5.4 게시물 생성·사진

#74 사진 JSON/API의 최신 BE1 검토안은 §13을 참조한다. FE/BE2 wire 확인 전 구현 계약으로 고정하지 않으며 아래 multipart는 변경 전 예시다.

> **전송 계약 변경 대기:** 게시물 사진은 Supabase Storage에 저장하며 회원·게스트·사진 URL을 아는 앱 외부 사람 모두 열람할 수 있다. JPG/PNG·최대 10장·게시물 합계 10MB를 유지한다. 사진 제거·교체 및 게시물 전체 삭제 시 해당 저장 파일을 지운다. 미완료 업로드는 임시 보관 후 24시간이 지나면 정리한다. 업로드 권한·최종 연결·bytes 환산·삭제 보상·24시간 기준시각/정리 간격/경합·임시 파일 공개 시점은 #74/#13/#30에서 합의한다. 이는 게시물 임시저장 기능을 추가한다는 뜻이 아니다. 아래 multipart·newImageIndex 등의 전송 예시는 변경 전 제안이다. 직접 업로드의 파일 참조/Endpoint/DTO는 #74 합의 후 고정하며 게시물 제품 입력·유형 제약은 유지한다.

`POST /posts`, 해당 지역 이웃 인증 완료 회원, multipart, 201 최신 상세 DTO. 서버는 사용자/지역 자격·공통 및 유형 입력·사진을 검증하고 원본과 하위 데이터를 저장한다. 성공한 postId의 상세로 FE가 이동하며 목록/마이/지도도 같은 원본을 사용한다.

multipart part 제안:

```text
payload: application/json
images: 반복 파일 part (선택, 첨부 순서 유지)
```

지역 안건 payload:

```json
{ "type": "LOCAL_AGENDA", "topic": "TRANSPORTATION", "regionId": 15, "title": "보행 환경 개선", "content": "원문" }
```

활동 payload:

```json
{
  "type": "LOCAL_ACTIVITY", "topic": "LIVING_INFORMATION", "regionId": 15,
  "title": "주민 간담회", "content": "활동 설명",
  "details": {
    "source": "노원구청", "schedule": "2026-10-12 14:00", "place": "월계동 주민센터",
    "activityStatus": "SCHEDULED", "externalParticipationUrl": "https://example.org/event"
  }
}
```

투표 payload:

```json
{
  "type": "VOTE", "topic": "SAFETY", "regionId": 15,
  "title": "횡단보도 조명 설치 의견", "content": "투표 설명",
  "details": { "question": "설치에 동의하시나요?", "options": ["찬성", "반대"], "endsAt": "2026-10-10T18:00:00+09:00" }
}
```

| 입력 | 검증 |
| --- | --- |
| 공통 | 유형·주제·지역·제목·본문 필수, 저장 ALL 불가 |
| 지역 안건 | 활동 필드/투표 확장 없음 |
| 지역 활동 정보 | 출처·일정·장소·활동 상태 필수, 상태 기본값 없음, 외부 링크 선택, 문의 이메일 별도 입력 없음 |
| 투표 | 질문·선택지 2~10개·종료 시각 필수; 최소 2개는 찬반 강제 아님 |
| 사진 | JPG/PNG·10장·합계10MB, 직접 업로드 계약·최종 한도/소유권·URL 공개·저장 파일 삭제·미완료 24시간 정리·경합/실패 보상 | F-GSMCLD |
| 비-MVP 필드 | referenceLink, isAnonymous, reminderAt, draft 입력은 MVP 허용 필드에서 제외 |

사진 한도는 최종 연결된 전체 사진에 적용하며 FE 사전 안내와 서버 확정 검증을 함께 수행한다. MB를 bytes로 환산하는 기준은 FE/BE 합의 후 고정 **[확인 필요]**. 제목/본문/질문/선택지 길이, 중복 선택지 허용 여부, 신규 종료시각의 과거값 처리 등 미명시 입력 세부는 제품 정책으로 임의 고정하지 않는다.

사진 선택·카메라/갤러리·미리보기·교체·제거는 제출 전 FE 파일 큐에서 지원한다. 화면 왕복/선택 취소/저장 실패 시 공통/유형별 입력과 기존 사진 유지. 성공 때만 연결 완료로 표시한다. 파일 부분 실패 시 게시물 생성 완료로 표시하지 않는 원자 저장/보상 처리는 **[설계 제안]**.

### 5.5 게시물 수정·삭제

#74의 기존 photoId/신규 fileId 최종 순서 배열과 삭제 예약/최종 완료 응답 구분은 §13의 BE1 검토안에 정리했다. FE/BE2 확인 전 구현 계약으로 고정하지 않는다.

> **전송 계약 변경 대기:** 게시물 사진은 Supabase Storage에 저장하며 회원·게스트·사진 URL을 아는 앱 외부 사람 모두 열람할 수 있다. JPG/PNG·최대 10장·게시물 합계 10MB를 유지한다. 사진 제거·교체 및 게시물 전체 삭제 시 해당 저장 파일을 지운다. 미완료 업로드는 임시 보관 후 24시간이 지나면 정리한다. 업로드 권한·최종 연결·bytes 환산·삭제 보상·24시간 기준시각/정리 간격/경합·임시 파일 공개 시점은 #74/#13/#30에서 합의한다. 이는 게시물 임시저장 기능을 추가한다는 뜻이 아니다. 아래 multipart·newImageIndex 등의 전송 예시는 변경 전 제안이다. 직접 업로드의 파일 참조/Endpoint/DTO는 #74 합의 후 고정하며 게시물 제품 입력·유형 제약은 유지한다.

```http
PATCH /api/v1/posts/{postId}
DELETE /api/v1/posts/{postId}
```

PATCH multipart `payload` JSON + 선택 `images`. 사진 유지/교체/삭제 순서를 명확히 하기 위한 제안:

```json
{
  "title": "수정 제목",
  "photoOrder": [{ "photoId": 801 }, { "newImageIndex": 0 }]
}
```

photoOrder 생략은 기존 사진 유지, []는 전체 제거, 지정 시 최종 순서와 연결할 기존 photoId/newImageIndex를 전부 나열한다. newImageIndex는 요청 images의 0-based 순서. 같은 참조 중복·다른 게시물 photoId·범위 밖 index 거부. 제거된 사진의 저장 파일은 삭제한다. 참조 처리·Storage 삭제 실패 재시도/보상은 별도 합의한다. 최종 사진 수/합산 용량 재검증 **[설계 제안]**.

| 유형 | 수정/삭제 정책 |
| --- | --- |
| 안건/활동 | 본인 원본 수정/삭제 가능. 공통·활동 필드의 세부 변경 허용 목록은 합의; 게시물 유형 전환 API는 임의 제공하지 않음 |
| 활동 상태 | 작성자가 네 상태 사이에서 자유 변경. 시간 경과로 자동 전환하지 않음 |
| 진행 중 투표 | 제목·본문·사진·종료 시각만 수정. 질문·선택지·지역·주제 수정 불가, 삭제 가능 |
| 종료 투표 | 수정·삭제 불가. 서버 시각으로 최종 재검증 |

POST/PATCH에 알림 생성/예약은 없다. PATCH 200은 최신 상세, DELETE 204. 삭제 즉시 공개 상세/공유/댓글/요약 접근 및 추가 참여를 차단하고 북마크 관계 자동 해제·목록 제거. 참여 투표 기록은 유지하되 삭제 본문/선택지를 다시 노출하지 않는다. 사진은 저장 파일까지 삭제한다. 댓글·반응·표·행동/채택 기록의 물리 보존/삭제 방식은 **[확인 필요]**다.

### 5.6 AI 안건 요약

`GET /posts/{postId}/summary`, 회원 또는 해당 공유 게스트, 200 상태 응답 **[설계 제안]**. 공개 LOCAL_AGENDA만, 다른 유형은 422 AI_SUMMARY_NOT_APPLICABLE. 삭제/비공개는 콘텐츠 없이 404.

```json
{
  "data": {
    "postId": 101,
    "status": "SUCCEEDED",
    "summary": "원문의 핵심을 설명하는 첫 문장입니다. 핵심 논점을 설명하는 두 번째 문장입니다. 원문에 근거한 마지막 문장입니다.",
    "generatedAt": "2026-10-06T14:00:00+09:00",
    "source": {
      "postId": 101,
      "title": "광운대역 주변 보행 환경 개선",
      "content": "게시물 공개 원문",
      "authorDisplayName": "동네주민",
      "updatedAt": "2026-10-06T13:20:00+09:00",
      "originalPath": "/posts/101"
    },
    "fallbackToSource": false
  }
}
```

source는 실제 원 게시물의 출처 정보/원문 연결이며 안건 작성자에게 비-MVP referenceLink나 활동 source 입력을 요구하지 않는다. guest originalPath 이동도 같은 공유 컨텍스트를 유지한다. AI 3문장은 자연스러운 한 문단, 불릿 3개 아님, 원문 외 사실 추가 금지 **[확정]**.

PENDING은 summary/generatedAt=null과 source 반환, FAILED/SOURCE_TOO_SHORT는 summary=null·fallbackToSource=true와 원문/상태 안내 반환. 실패를 전체 상세 열람 실패로 처리하지 않는다. 조회 GET은 저장된 생성 상태를 읽는 제안이며 생성 착수·동기/비동기/캐시/수정 후 재생성·짧은 원문 임계값·재시도는 구현 전에 합의한다. 원문 갱신 시 이전 버전 요약을 최신 요약으로 표시하지 않는 source version 연결은 권장한다. 별도 사용자 Job API·추천 API·정해진 폴링 간격을 필수로 만들지 않는다.

### 5.7 공개 공유 링크

`GET /posts/{postId}/share-link`, 회원, 200. 공개 대상 여부 확인 후 `{ "data": { "postId": 101, "shareUrl": "https://<서비스도메인>/shared/posts/101?token=<공유토큰>" } }` **[설계 제안]**. 실제 서비스 domain은 환경 설정, 위 URL은 예시. 클라이언트는 복사/공유하고 링크 열기는 로그인 없이 특정 상세로 직행한다. 조회로 개인 활동 +1 없음. 게스트도 받은 링크를 FE에서 복사할 수 있으며 새 링크 발급 API는 호출하지 않는다.

서버 링크 발급 API/토큰을 사용하지 않는 동등 방식도 가능하지만 공유 대상과 서버 권한 검증 계약을 먼저 합의해야 한다. 공유 링크 만료·재발급은 제품 미정이며 추가 요구로 단정하지 않는다. 삭제된 원본을 링크/토큰으로 복구해 노출하지 않는다.

## 6. 댓글·반응·평가·투표·북마크

### 6.1 댓글과 1단계 답글

```http
GET /api/v1/posts/{postId}/comments?sort=LIKES&cursor=...&size=...
POST /api/v1/posts/{postId}/comments
POST /api/v1/comments/{commentId}/replies
```

GET은 회원/해당 공유 게스트, POST는 해당 지역 이웃 완료 회원 또는 공유 게스트. 댓글 Request `{ "content": "좋은 의견입니다." }`, 답글 Request `{ "content": "저도 동의합니다.", "replyToCommentId": 302 }`. path commentId는 부모 또는 답글로 해석 가능하도록 설계하되 서버는 항상 원 부모를 찾아 연결한다. replyToCommentId를 지정하면 같은 게시물·같은 부모 하위인지 검증한다 **[설계 제안]**.

생성 201, 댓글 DTO 제안:

```json
{
  "data": {
    "id": 301,
    "postId": 101,
    "parentCommentId": null,
    "content": "좋은 의견입니다.",
    "author": { "displayName": "게스트", "isGuest": true, "institutionVerified": false },
    "replyTo": null,
    "likeCount": 0,
    "dislikeCount": 0,
    "createdAt": "2026-10-06T14:00:00+09:00",
    "replies": []
  }
}
```

회원 컨텍스트에는 myEvaluation=LIKE/DISLIKE/null 추가. 답글 parentCommentId는 원 부모 ID, replyTo는 대상 commentId/displayName. 게스트 공개명은 정확히 `게스트`, 기관 배지는 false. 회원 이름/배지는 최신 공개 프로필을 참조한다.

- 빈 내용 거부, 댓글/답글과 회원/게스트 모두 동일 사전 정의 문자열 **포함** 검사를 적용. 통과 시 즉시 공개. 실제 금칙어 목록은 **[확인 필요]**, 임의로 만들어 확정하지 않는다. AI 유해 문맥 판정/운영자 관리 UI 없음.
- 답글의 답글도 원 부모 아래 1단계 저장하며 대상명을 표시한다. 2단계 계층을 만들지 않는다.
- LIKES는 부모 좋아요 내림차순·동률 최신 부모, LATEST는 부모 작성시각 내림차순. 답글 좋아요를 부모 정렬에 합산하지 않고 답글은 부모 아래 유지한다.
- 커서 페이지는 부모 단위이며 각 부모 replies를 함께 반환하는 안. 답글이 큰 경우 별도 페이지 계약을 합의해야 하며 답글을 누락한 채 전체로 표시하지 않는다. 최종 동률의 ID 보조 정렬은 기술 설계로 합의.
- 성공 회원 댓글/답글은 개인 행동 +1, 현재 참여 카드에 `댓글 작성` 표시. 게스트 의견은 가입 후 자동 회원 이관/개인 활동 집계 없음.
- 댓글/답글 수정·삭제 API는 원문 미명시이므로 임의 제공하지 않는다.

### 6.2 게시물 반응

```http
PUT /api/v1/posts/{postId}/reactions/{reactionType}
DELETE /api/v1/posts/{postId}/reactions/{reactionType}
```

이웃 완료 회원, 공개 대상, 200. reactionType=EMPATHY/NEEDED/CURIOUS. PUT은 그 관계 존재, DELETE는 그 관계 부재라는 최종 상태를 요청한다. FE의 같은 버튼 재클릭은 DELETE. 세 반응은 독립이고 동시에 모두 선택 가능.

```json
{ "data": { "postId": 101, "reactionCounts": { "EMPATHY": 3, "NEEDED": 2, "CURIOUS": 1, "total": 6 }, "myReactions": ["EMPATHY", "NEEDED"] } }
```

동일 PUT 재시도는 관계/횟수 추가 없음, DELETE 부재 상태 재시도도 유지 **[설계 제안]**. 실제 없는→있는 등록은 유형별 +1, 취소 +0, 취소 뒤 재등록 +1. 집계 total은 3유형 합계이며 댓글/투표 수와 분리.

### 6.3 댓글/답글 좋아요·싫어요

```http
PUT /api/v1/comments/{commentId}/evaluation
DELETE /api/v1/comments/{commentId}/evaluation
```

대상 소속 지역 이웃 완료 회원. PUT body `{ "type": "LIKE" }` 또는 DISLIKE, DELETE body 없음. 응답 200 `{ "data": { "commentId": 301, "myEvaluation": "LIKE", "likeCount": 4, "dislikeCount": 1 } }`. 취소면 myEvaluation=null.

평가는 상호배타. 반대 버튼은 PUT으로 전환, 같은 버튼은 DELETE로 취소. 최초 평가 +1, 직접 전환/취소/같은 desired state 재시도 +0, 취소 후 새 등록 +1. 댓글/답글 모두 적용하고 참여한 게시물에 `댓글 좋아요·싫어요` 표시. 게스트 평가 401, 답글 like는 부모 정렬에 합산하지 않음.

### 6.4 실제 투표 제출·변경

`PUT /posts/{postId}/vote`, 해당 지역 이웃 완료 회원, 200 최신 vote DTO. Request:

```json
{ "optionId": 1, "confirmChange": false }
```

서버 순서: 인증 → 대상 공개 VOTE → 지역 자격 → 진행 여부(서버 시각) → 옵션 소속 → 현재 선택 잠금/확인 → 생성/동일 유지/확인 후 교체 → 최신 집계.

- UI 선택만으로 요청/저장하지 않고 `투표 제출` 클릭 때 요청 **[확정]**.
- 다른 기존 선택은 `선택을 변경하시겠습니까?` 확인. 취소 시 요청하지 않고 기존 표 유지. 확인 후 confirmChange=true.
- false인 다른 선택은 409 VOTE_CHANGE_CONFIRMATION_REQUIRED **[설계 제안]**. FE에서 선확인했어도 서버가 요청 시 기존 선택을 다시 확인한다.
- 동일 옵션 재요청 200 유지는 **[설계 제안]**, 참여/집계/+1 중복 없음. 종료 후에는 동일 옵션 요청을 포함한 신규 제출·변경 요청을 거부하는 안.
- 최초 표 +1, 선택 변경 +0, 사용자당 현재 한 표. 변경/종료 경합은 원자 갱신과 서버 종료시각 우선.
- 응답은 실제 본인 선택, 옵션별 수/율, 전체 참여자 수, 진행/종료·endsAt. 타인 개인 선택 비공개.
- 결과 최신화는 제출 성공 후 조회·상세 재진입/재조회로 충족하도록 설계. SSE/WebSocket/폴링 주기는 제품에서 미확정이며 기술 합의 전 필수 방식으로 고정하지 않는다. 알림 예약·발송 없음.

### 6.5 북마크

```http
PUT /api/v1/posts/{postId}/bookmark
DELETE /api/v1/posts/{postId}/bookmark
```

로그인 회원, 지역 이웃 인증 불필요, 공개 게시물만. 200 `{ "data": { "postId": 101, "isBookmarked": true } }`, 해제 false. desired state 재시도 멱등 **[설계 제안]**.

상세 버튼 하나에서 회원 저장 성공 시 `저장되었습니다` 팝업, 현재 상세 유지. 게스트는 저장 API/성공 팝업 없이 로그인 안내·returnTo 보존. 가입/로그인 뒤 복귀만 하며 재클릭 전 자동 저장 금지. 목록 카드에 등록/해제 버튼 추가 없음.

실제 등록 +1, 해제 +0, 해제 후 재등록 +1. 북마크만 한 글은 참여 게시물 목록에서 제외. 게시물 삭제 시 관계 자동 해제·북마크 목록 제거, 삭제에 따른 해제도 +0.

## 7. 기관 안건 조회·채택

### 7.1 담당자 안건 목록

`GET /officer/agendas?regionId=15&scope=ALL&cursor=...&size=...`, 현재 유효 기관 인증 완료 사용자, 200 목록. 전체 공개 LOCAL_AGENDA만, 반응 합계 내림차순. regionId는 조회 편의 필터이며 담당 지역 밖도 안건·공개 댓글/답글 내용을 열람할 수 있다. 반응 동률의 보조 정렬은 최신/ID 등 기술 계약으로 합의하며 제품이 확정한 값으로 단정하지 않는다.

item은 공통 PostCard의 제목·지역·반응 수·commentCount·게시시각에 `myInstitutionAdoption`을 추가한다 **[설계 제안]**:

```json
{
  "postId": 101,
  "myInstitutionAdoption": { "id": 70, "adoptedAt": "2026-10-06T14:00:00+09:00" },
  "capabilities": { "canAdopt": false, "canCancelAdoption": true }
}
```

scope=ADOPTED는 현재 인증된 **본인 기관**의 유효 채택만 제공한다. 동일 기관 내 개별 채택 담당자의 userId와 기관 관계를 혼동하지 않는다. 식별 기관 모델은 **[설계 제안/합의 필요]**, 기관명 문자열만으로 같다고 추정하지 않는다. 취소된 관계는 현재 목록에서 제외한다. 목록의 의견 숫자만 보이고 상세 댓글 내용이 누락되는 방식은 인수하지 않는다.

### 7.2 채택·취소

```http
POST /api/v1/posts/{postId}/adoptions
DELETE /api/v1/posts/{postId}/adoptions/{adoptionId}
```

POST body 없음, 인증 주체에서 기관/담당 지역/채택 사용자를 판별. 유효 기관 인증 → 공개 LOCAL_AGENDA → 담당 지역 일치 → 본인 기관 관계 생성. 201 `{ "data": { "id": 70, "postId": 101, "institutionName": "노원구청", "adoptedAt": "2026-10-06T14:00:00+09:00" } }`. 같은 현재 관계 재요청은 200 기존 관계 반환 권장 **[설계 제안]**.

DELETE는 유효 기관·대상 담당 지역·adoptionId의 postId/본인 기관 귀속 확인 후 204. 다른 기관 관계를 취소할 수 없다. 동일 취소 재시도 성공 유지 또는 404 처리는 실제 계약에서 통일한다. 취소 시 내부 채택 사용자/시각·취소 이력을 보존하고 현재 표시/목록에서 제거한다. 복수 기관 채택 독립, 한 기관 취소는 타 기관에 영향 없음.

일반 상세의 공개 정보는 기관명·채택 시각만이며 내부 채택 담당자 이름/업무 이메일/전화번호/증빙은 공개하지 않는다. 일반 DTO에 불필요한 adoptionId는 넣지 않고 기관용 본인 관계에만 취소용 ID를 제공한다 **[설계 제안]**. 채택은 게시물 상태 변경이 아니며 투표/활동 상태와도 분리한다. 채택/취소 알림·검토 중/처리 중/완료·행정 연동 없이 정상 동작한다. 기관 채택/취소는 정의된 주민 개인 활동 +1 항목에 추가하지 않는다.

## 8. 개인 목록·활동 횟수

모든 API는 로그인 본인만 조회하며 userId query로 다른 회원 기록을 선택하지 않는다. 목록은 같은 원본 PostCard/관계를 사용한다.

### 8.1 목록 계약

| API | 필터 / 반환 |
| --- | --- |
| GET /users/me/posts | type, cursor, size. 내가 작성한 세 유형 원본, 유형/주제 표시, 상태 필터 없음. 유형별 canEdit/canDelete |
| GET /users/me/participations | type, cursor, size. 현재 유효 내 반응·댓글/답글·평가·표를 postId별 한 카드에 합침 |
| GET /users/me/votes | status=ALL/OPEN/CLOSED, cursor, size. 실제 참여한 투표만, 본인 실제 선택·최신/최종 결과·종료시각 |
| GET /users/me/bookmarks | type, topic, cursor, size. 본인 현재 저장 원본, 삭제물은 제거 |

type/topic 전체는 생략한다. 내가 만든/참여한 게시물은 유형 필터를 사용하며 북마크의 주제 필터를 모든 개인 목록에 일괄 추가하지 않는다. 투표 카드의 남은 기간은 endsAt와 서버 시각/상태를 기준으로 FE가 표시하고 최다 득표를 내 선택으로 추정하지 않는다.

참여 목록 item은 공통 카드에 다음 `myParticipation` 제안을 추가한다.

```json
{
  "reactions": ["EMPATHY", "CURIOUS"],
  "hasCommentOrReply": true,
  "hasCommentOrReplyEvaluation": true,
  "hasVote": false
}
```

댓글/답글은 `댓글 작성`, 댓글/답글 평가 종류는 `댓글 좋아요·싫어요`로 묶되 원본 관계는 유지한다. 현재 유효한 모든 표시를 반환하고 취소 시 그 표시만 제거한다. 다른 참여가 있으면 카드 유지. 북마크만/게시물 작성만/타인이 내 글에 한 행동은 참여 카드 근거가 아니다. 게스트 의견을 회원 기록으로 자동 이관하지 않는다. 빈 참여 목록은 `아직 참여한 게시물이 없습니다`와 게시판 CTA.

### 8.2 삭제·접근 불가 투표 기록

개인 참여 기록은 남기되 일반 공개 콘텐츠를 되살리지 않는다. 응답 제안:

```json
{
  "postId": 102,
  "availability": "UNAVAILABLE",
  "participatedAt": "2026-10-06T15:00:00+09:00",
  "post": null,
  "vote": null
}
```

사용자에게 접근 불가 안내만 제공한다. 삭제된 본문/선택지·본문 excerpt·이미지·선택 텍스트를 별도 사본에서 반환하지 않는다. status=OPEN/CLOSED 필터와 삭제 기록의 관계·내부 보존 모델은 **[확인 필요]**이며 전체 목록에는 기록 유지가 보장되어야 한다. 북마크 삭제 정책(카드 제거)과 혼동하지 않는다.

### 8.3 활동 횟수

`GET /users/me/activity`, 본인 회원, 200. 행동 원본/누적 이벤트에서 반환하는 제안:

```json
{
  "data": {
    "totalCount": 7,
    "counts": {
      "POST_CREATED": 1,
      "BOOKMARK_REGISTERED": 2,
      "EMPATHY_REGISTERED": 1,
      "NEEDED_REGISTERED": 0,
      "CURIOUS_REGISTERED": 0,
      "COMMENT_CREATED": 1,
      "REPLY_CREATED": 0,
      "COMMENT_LIKE_REGISTERED": 1,
      "COMMENT_DISLIKE_REGISTERED": 0,
      "REPLY_LIKE_REGISTERED": 0,
      "REPLY_DISLIKE_REGISTERED": 0,
      "VOTE_PARTICIPATED": 1
    }
  }
}
```

| 사건 | 누적 활동 | 현재 유효 참여 표시 |
| --- | --- | --- |
| 게시물 최초 게시 | +1 | 작성 목록; 작성 자체로 참여 목록 추가 없음 |
| 북마크 최초 등록 | +1 | 북마크 목록; 참여 목록 근거 아님 |
| 각 반응 등록 | 각각 +1 | 해당 반응 표시 |
| 댓글/답글 작성 | 각각 +1 | 댓글 작성 표시 |
| 댓글/답글 LIKE/DISLIKE 최초 등록 | 각각 +1 | 댓글 좋아요·싫어요 표시 |
| 투표 최초 참여 | +1 | 투표 참여 표시 |
| 반응·평가·북마크 취소 | +0, 누적 횟수 차감 없음 | 해당 현재 표시 제거 |
| 평가 직접 LIKE↔DISLIKE 전환 | +0 | 평가/집계 갱신, 참여 유지 |
| 투표 선택 변경 | +0, 최초 1회 유지 | 실제 본인 선택 갱신 |
| 취소 후 반응/평가/북마크 재등록 | +1 | 해당 현재 표시 복원 |
| 조회·수정·삭제·프로필/지역 설정 | +0 | 각 원본 상태만 갱신 |
| 동일 desired state 재시도 | +0 | 기존 관계 유지 |

totalCount는 위 누적 행동 횟수의 합이고 현재 북마크 수/반응 수/중복 제거 게시물 수와 다르다. 직접 평가 전환은 새로운 +1 이벤트로 기록하지 않는다. 활동 카테고리 세부 코드는 **[설계 제안]**이며 +1 규칙은 **[확정]**. 원본 변경과 이벤트 기록을 원자 처리하거나 신뢰할 동등 방식으로 연계하고 알림 생성 여부에 의존하지 않는다.

## 9. 데이터 정본·동시성·재시도

| 값 | 정본 / 계산 |
| --- | --- |
| 게시물·사진·작성자 | 같은 Post 원본, 정렬된 사진, 최신 공개 프로필 |
| 반응 총수 | 현재 세 유형 관계 수의 합 |
| 댓글 수 | 부모 댓글 + 답글 수 |
| 댓글 정렬 | 부모 평가 수/부모 작성시각, 답글 like 합산 없음 |
| 지도 대표 | 동별 공개 안건/투표 중 반응 최대·동률 최신 |
| 투표 선택·결과 | 회원별 현재 한 표와 옵션 집계 |
| 참여 게시물 | 본인의 현재 유효 참여 관계, postId 중복 제거 |
| 누적 활동 | 실제 +1 등록/작성 이벤트, 취소/전환/변경 +0 |
| 기관 배지·권한 | 유효한 기관 인증 완료 상태·유효기간·담당 지역 |
| 채택 표시 | 기관↔안건의 현재 관계; 취소 이력 내부 보존 |
| 기본 활동 지역 | User 프로필 저장값 |
| 탐색 지역 | 임시 조회 parameter, 프로필 덮어쓰기 없음 |

권장 제약 **[설계 제안]**:

```text
PostReaction       UNIQUE(post_id, user_id, reaction_type)
CommentEvaluation  UNIQUE(comment_id, user_id)
VoteSelection      UNIQUE(poll_id, user_id)
Bookmark           UNIQUE(post_id, user_id)
VerifiedRegion     UNIQUE(user_id, region_id) + 사용자별 최대 3개 원자 검증
Adoption           UNIQUE(post_id, institution_id) for current relation
```

v1.0의 institution_verification_id만 유일 키로 쓰면 동일 기관의 다른 인증 담당자가 중복 채택 관계를 만들 수 있다. 관계 단위는 확정 기관↔안건이며 기관 식별 정본/인증 요청 연결 구조를 합의한다. 채택 사용자와 현재 인증 ID는 내부 감사 정보로 별도 저장한다.

반응/평가/투표/북마크는 원자 갱신·유일 제약으로 중복 집계를 막는다. 원본과 활동 이벤트도 함께 확정한다. 댓글/게시/인증 신청 등 POST 생성 재시도는 클라이언트 요청 ID 또는 Idempotency-Key 지원을 권장한다. 지원하기로 합의하면 키 scope·보관기간·같은 키 다른 payload 오류를 문서에 고정하고 같은 요청의 리소스/+1 이벤트를 한 번만 생성한다. 모든 API에 아직 구현되지 않은 전역 키를 필수로 요구하지 않는다.

권한/상태 파생 응답은 조회 시점 정보이며 서버는 각 쓰기에 재검증한다. 네트워크 오류로 결과가 불명확한 경우 FE는 성공을 가정하지 않고 최신 상태를 조회해 복구한다. 실패 시 낙관 갱신 롤백·입력/첨부 큐 유지. 401/403/409를 구분해 로그인·지역 자격·변경 확인/종료 안내를 제공한다.

## 10. 비-MVP API·최종 정책 보존

아래는 **이번 구현 대상 목록에 포함하지 않는다**. v1.0에서 제안한 경로는 후순위 추적용이며 제품 정책 제거를 뜻하지 않는다. 후순위 개발 시 당시 정본과 계약을 다시 대조한다.

| 후순위 경로/필드 | 보존 정책 / 기능 ID |
| --- | --- |
| POST /auth/logout | 현재 기기 세션만 종료·시작 화면 복귀. 설정/로그아웃 UI는 이번 필수 범위 제외 (S-HDQTIR) |
| POST /auth/password-resets | 이메일 인증→새 비밀번호, 존재 여부 구분 없는 안내, 성공 후 로그인 화면·자동 로그인 없음, returnTo 유지 (F-TZCLRG) |
| PUT /users/me/email | 기존 비밀번호+새 이메일 인증, 중복 이메일 불가, 활동 문의 이메일 갱신 (F-JRAFSD, S-YXAQIT) |
| PUT /users/me/password | 등록 이메일 인증 후 변경, 공통 비밀번호 정책 (S-ZTMHOQ) |
| POST /users/me/withdrawal | 주의사항→현재 비밀번호→최종 확인, 콘텐츠/집계 유지·회원 식별 연결/북마크/설정 제거·문의 이메일 제거·탈퇴 사용자 표시 (S-FIUQIE) |
| GET/PUT /users/me/regions/interests | 관심 지역 무제한·0개 허용, 탐색/추천만, 참여 권한·알림 근거 아님 (F-MBJNPG) |
| PATCH /users/me의 interestKeywords | 7개 주제 중 최대 4개, 자유 입력 없음, 추천만, 실패 시 기존 선택 유지 (F-OAPPBV) |
| GET/PATCH /users/me/settings | 다크 모드 계정 저장, push 단일 ON/OFF. 앱 내 알림 기록 유지 (F-SAOWVT, S-EJZFTB) |
| GET/POST /posts/drafts | 지역 안건 임시 저장. 식별자/발행/갱신 세부 미정 (S-NVXXYQ의 비-MVP 부분) |
| referenceLink / isAnonymous | 참고 자료는 활동 출처/외부 링크와 별개. 허용 유형의 실제 작성자 연결 유지; 활동/기관 인증 사용자는 익명 불가 (F-FTLHCX) |
| GET /agenda-recommendations | 안건만 추천, 활동 지역→관심 지역→관심 키워드, 행동 이력 없음, 현재 카드만 제외·후보 없으면 현재 유지 (F-QZITNN) |
| GET /notifications, PATCH /notifications/{id}/read | 본인 알림·읽음, 자기 행동 제외·같은 이벤트/수신자 중복 방지, 일반 상세/신고 삭제 안내 연결 (R-ZZFXGA) |
| reminderAt·투표 예약 데이터/실행 | 종료 전 선택 시점, 작성자/참여자 중복 없이 발송. 종료/삭제/종료 일시 변경 시 기존 예약 취소, 새 시점은 작성자가 다시 지정 (S-ASBCKL) |
| POST /posts/{postId}/reports | 자유 입력 사유, 동일 회원/게시물 재신고 불가, 접수만으로 삭제 없음, 조치 없음/경고/삭제 운영 심사·경고 누적 (F-VXHBWJ) |
| 인증 승인/반려·보완·보관 운영 | 1주일 내 실제 운영자 정성 심사, 기관 보완은 이메일 회신, 기관 만료 증빙 삭제·반려 증빙 30일 후 삭제 운영 (F-ATWJDJ, F-OPNIXL) |

후순위 알림은 푸시 OFF여도 앱 내 생성/보관, ON이면 1회 시도·실패해도 기록 유지·자동 재시도 없음. 채택/취소 알림 실패는 관계 저장을 되돌리지 않는다. 이는 알림을 이번 MVP에 만들어야 한다는 요구가 아니다. 신고 삭제 시 콘텐츠 미노출·북마크 해제 정책은 일반 삭제의 MVP 처리와 일관되게 유지한다.

서비스 내부 활동 신청·결제, 게스트 전화번호 인증/문자 알림, AI 이미지, 기관 채택 후 처리 상태/외부 행정 자동 연동도 MVP에 추가하지 않는다.

## 11. 구현 전 확인과 계약 고정

### 11.1 MVP에서 합의할 항목

| 구분 | 항목 | 범위 / 해석 |
| --- | --- | --- |
| 제품 세부 | 실제 금칙어 사전 | 단순 포함 필터 완료에 필요, 임의 목록 확정 금지 |
| 제품/첨부 세부 | 프로필 사진 제한·Privy OTP 정책 확인. 이웃/기관 증빙 제출은 이번 MVP 제외 | 게시물 사진 한도를 프로필로 전용하지 않음 |
| 제품/입력 세부 | 필드 길이·활동 일정 구조·URL 검증·과거 종료 시각·선택지 중복 | 원문 미명시 규칙을 확정 정책으로 단정하지 않음 |
| 삭제 보존 | 사진 파일 삭제 확정. 다른 댓글/표/활동/채택의 물리 보존은 미정 | 공개 차단·북마크 제거·개인 투표 기록 유지 보장 |
| 기술 공통 | 실제 URL, PK/ID, 인증 전달·TTL·갱신, DTO/nullable/error | 기존 구현이 제공되면 일괄 대조; 본 경로/Enum은 초안 |
| 기술 목록 | cursor/size·기본 정렬·동률, 초기 댓글 페이지·답글 | 기관/댓글 확정 정렬 유지, 미정 수치 합의 |
| 기술 지역·기관 | Region/지도 원천, 기관 ID와 인증 요청 연결 | 동일 원본·기관별 독립 관계 보장 |
| 기술 파일 | Supabase 직접 업로드·파일 참조·24시간 기준/정리·삭제 보상·MB bytes | #74 합의 후 #13/#30 구현, 증빙 제외 |
| 기술 공유 | 공유 전달 컨텍스트·토큰·수명·returnTo | 특정 postId 귀속·공개 재검증·자동 행동 금지 |
| 기술 AI | 생성 시점·provider·source version·저장/재생성·재시도·짧은 원문 기준 | 상태/원문 fallback은 MVP; 추천 구현 불필요 |
| 기술 집계 | 활동 이벤트·멱등 키·투표 득표율/갱신 방식 | +1/+0·한 표·한 현재 관계 보장 |

### 11.2 후순위 운영 질문

반려 후 재신청·인증 보완 메일 수신 주소/회신 연결·전화번호 별도 인증·증빙 보관 구조·신고 심사/경고 임계치/제재·알림 수신자 세부·draft 발행 모델 등은 후순위다. 이를 이유로 MVP 상태·지역 저장/권한·참여·개인 활동을 누락하지 않는다.

### 11.3 협업전략 적용

FE/BE 같은 Part끼리 Method+Path, Request 필수/선택, Response 타입/nullable, 정상/오류·HTTP 상태, 회원/게스트 전달 방식, 권한, mock과 실 API 차이를 합의해 작업 카드에 기록한다. mock 확인은 연동 완료가 아니다.

- A는 계정·프로필·지역·이웃/기관 인증·공통 권한. B는 공통 Post·사진·탐색·지도·요약. C는 참여·공유 범위. D는 북마크·개인 기록·활동·기관 채택.
- 공통 상세는 B 소유이며 C의 참여와 D의 북마크/채택 응답을 같은 postId로 조립한다. A가 인증/권한 기반을 제공하고 각 쓰기 소유 Part가 재검증한다.
- 투표 원본/작성 제약은 B, 선택/집계 C, 개인 조회 D. 기관 상태 A, 기관 안건/채택 D.
- 활동 +1 생성은 각 행동 소유 Part, 조회/집계 D. 동일 원본과 이벤트 계약을 공유한다.
- API 변경은 명세·협업 문서에 변경 전/후·영향 Part·적용 순서·commit·상대 확인/연동 결과를 기록한다. 실제 담당자 공지는 사람이 수행하며 문서 갱신만으로 자동 통보된 것으로 표시하지 않는다.
- 검증된 Part 변경을 develop을 통해 전달한다. 일반 기능 PR/Issue 필수 절차를 별도 추가하지 않고 제공 협업전략을 따른다. 실제 폴더·test/build 명령은 저장소에서 확인 후 기록한다.

## 12. MVP 인수 검증

구현이 제공되지 않았으므로 아래는 실행 결과가 아닌 **구현 후 검증 목록**이다. 인증 접수 계정·이웃 완료 계정·유효/만료 기관 계정·다른 담당 지역 계정을 준비한다.

| 여정 | 인수 기준 | 기능 ID |
| --- | --- | --- |
| 가입·로그인 | Privy OTP 실제 인증·오류/만료·갱신, 로컬 미가입/가입 완료, 필수 동의·프로필·지역·returnTo; 자체 비밀번호/고정 OTP 제한 제외 | F-KZRSXU, F-TSOXGG |
| 복귀 | 가입·프로필·지역까지 returnTo 보존, 원 상세/없으면 메인, 자동 저장/표 제출/게스트 의견 이관 없음 | F-TSOXGG, S-NYUECP |
| 프로필·지역 | 본인만 변경, 최신 작성자 표시, 임시 탐색으로 기본 지역 변경 없음, 기관/이웃 속성으로 참여 권한 생성 없음 | F-RBVFZX, F-QQKYLC |
| 이웃 자격 | #75 시연 완료 지역·최대 3지역·타 지역/무자격 제한. 증빙 신청/접수 제외 | F-ATWJDJ |
| 게시 원본 | 세 유형 게시→해당 상세, 목록·메인·마이·지도 같은 원본/집계, 필수 활동 4항목·투표 2~10옵션 경계 | F-FTLHCX, S-TBFIHO |
| 사진 | JPG/PNG·10장·합계10MB, 직접 업로드 계약·최종 한도/소유권·URL 공개·저장 파일 삭제·미완료 24시간 정리·경합/실패 보상 | F-GSMCLD |
| 수정·삭제 | 본인/지역/상태 검증, 투표 허용4항목만 수정, 종료 후 수정/삭제 거부, 삭제 상세/공유 차단·북마크 해제 | F-FTLHCX, F-FYQJPT |
| 메인·지도 | 빈 상태 CTA 유지, 지도 활동 후보 제외·최고반응/동률최신·첫사진, 빈 동 유지, 반응 변경 후 대표 갱신, GPS 불필요 | F-UPRLMN, F-QIGKAK |
| 요약 | 공개 안건만 3문장 한 문단·원문/출처 연결, 생성중/실패/짧은 원문 상태·원문 fallback, 공유 대상 한정 | F-WSCKDN |
| 게스트 | 특정 공유 상세/공개집계/댓글·답글 가능, 메인/목록/지도/개인기록 불가, 다른 postId 토큰 접근·평가/표/반응/북마크 거부 | F-OWFYWE, S-NYUECP |
| 댓글 | 동일 금칙어 포함 검사, 빈 내용 거부·통과 즉시 게시, 답글의 답글도 동일 원부모/대상명, 부모 좋아요/최신 정렬·답글like 합산 없음 | F-EDNVWZ, S-YYDGUS, S-OXTKEP |
| 반응·평가 | 세 반응 동시 선택/개별취소, 평가 상호배타 전환/취소, 재시도 중복 관계/집계/+1 없음 | F-GOMLGG, F-CDIBRF |
| 투표 | 선택만 저장 없음, 변경 확인 취소 기존표 유지/확인 한표 교체, 종료 경합 서버 우선, 본인 선택 실제값·타인 선택 비공개 | S-CMGJIG |
| 북마크 | 미인증 지역 회원 저장 가능, 상세 단일버튼·성공 팝업/현재상세 유지, 게스트 복귀 뒤 재클릭, 목록 유형/주제/삭제 제거 | F-FYQJPT |
| 개인 기록 | 참여 postId 한카드·현재 모든 표시·다른 행동 있으면 유지, 북마크만/타인행동 제외, 삭제 투표 기록 유지·콘텐츠 미노출 | F-NZTUYE, F-QPGNCF |
| 활동 횟수 | 등록/작성+1, 취소/직접전환/표변경+0, 재등록+1, 현재관계수/카드수와 별개, 알림 없이 조회 | F-WYMXXP |
| 기관 자격 | #75 유효/만료 기관·담당 지역·파생 배지/역할·기관 업무 접근. 증빙 입력/제출/접수 제외 | F-OPNIXL, S-YLSPHQ, F-MUBDJD |
| 기관 채택 | 전체 안건·주민 의견 내용 열람, 담당지역 공개안건만 채택, 기관별 독립/현재관계만표시/취소이력 내부보존/담당자정보 비공개 | F-CNNPYL, F-TUGMEP |
| 범위 독립 | 추천·관심·알림/예약·신고·계정복구·운영심사 없이 모든 MVP 여정 저장/조회 가능 | v10.1 A~AA |

## 13. 구현 순서·Endpoint 정합성

1. **공통 계약/A 기반**: 실제 ID·세션·Region·응답/오류 → 가입/로그인/프로필 → 이웃/기관 신청 상태·지역과 시연 완료 권한.
2. **B 읽기/쓰기**: 공통 Post/사진/활동/투표 원본 → 목록/상세/메인/지도 → 세 유형 게시/수정/삭제.
3. **C 참여·공유**: 공유 상세 검증 → 댓글/답글/정렬 → 반응/평가 → 한 표 제출/확인 변경/종료.
4. **D 개인·기관**: 북마크·개인 목록·누적 활동 → 기관 전체 안건/담당 지역 채택/취소. 활동 이벤트 계약은 각 쓰기 구현 전에 합의한다.
5. **B 요약·전체 연동**: 공개 원문/출처/생성상태/fallback → 권한/동시성·삭제/복귀 경합 인수 검증. 요약은 추천/관심 정보 없이 독립 완료한다.

순서는 의존성 기준이며 단일 Part 전체 일괄 구현 요청이 아니다. 각 기능 ID에 세부 작업·허용 범위·API·검증을 연결해 순차 구현하고 선행 검증된 공통 변경을 develop에서 받는다.

아래는 3절과 같은 39개 Method+Path 계약안이다. 비-MVP 경로를 섞지 않는다.

```text
GET    /health
POST   /auth/email-verifications
POST   /auth/email-verifications/confirm
POST   /auth/sign-up
POST   /auth/login
GET    /users/me
PATCH  /users/me
GET    /regions
GET    /users/me/neighbor-verifications
POST   /users/me/neighbor-verifications
GET    /institution-verifications
POST   /institution-verifications
GET    /home
GET    /map/dongs
GET    /posts
POST   /posts
GET    /posts/{postId}
PATCH  /posts/{postId}
DELETE /posts/{postId}
GET    /posts/{postId}/summary
GET    /posts/{postId}/share-link
GET    /posts/{postId}/comments
POST   /posts/{postId}/comments
POST   /comments/{commentId}/replies
PUT    /posts/{postId}/reactions/{reactionType}
DELETE /posts/{postId}/reactions/{reactionType}
PUT    /comments/{commentId}/evaluation
DELETE /comments/{commentId}/evaluation
PUT    /posts/{postId}/vote
PUT    /posts/{postId}/bookmark
DELETE /posts/{postId}/bookmark
GET    /officer/agendas
POST   /posts/{postId}/adoptions
DELETE /posts/{postId}/adoptions/{adoptionId}
GET    /users/me/posts
GET    /users/me/participations
GET    /users/me/votes
GET    /users/me/bookmarks
GET    /users/me/activity
```

제품 범위·정책은 최신 정본으로 정렬했으며, 기술 초안의 경로/필드/Enum은 FE/BE 합의·실제 구현 대조 전 확정 완료로 표시하지 않는다.

## 13. #74 사진 API BE1 검토안 — FE/BE2 wire 확인 대기

2026-10-07 사용자 요청으로 BE2 원문 `back/feature/74-photo-contract`의 `4f7bb25` / 검토표 §10.8~10.9를 대조했다. 실행 방향 1~4는 사용자 채택이며 아래 경로·DTO·응답 코드·bytes는 **BE1 검토안**이다. FE/BE2 확인자·날짜·대상 SHA/PR이 기록되기 전 확정 계약이나 #13 구현 완료로 취급하지 않는다. 아래 안이 확인되면 §5.4~5.5의 변경 전 multipart/newImageIndex 예시를 대체한다. 인증 토큰/header/cookie 방식은 #74 인증 계약의 선행 조건을 유지한다.

### 13.1 전송·ID·용량

- 모든 앱 경로의 prefix는 `/api/v1`. 가입 완료 회원의 검증된 로컬 회원 ID를 owner로 사용한다. 업로드 준비/완료/조회/취소에 `regionId`는 허용하지 않는다. 최종 게시물 작성·변경에서 대상 지역 자격/소유권/게시물 상태를 재검증한다.
- `fileId`, `photoId`, `postId`는 기존 전역 계약의 양의 JSON number, 최대 9007199254740991. fileId는 media_files.id, photoId는 post_photos.id이며 서로 대체하지 않는다.
- **BE1/FE 확인 대기인 환산안: 10MB = 10,000,000 bytes.** 최종 전체 사진 최대 10장, 서버가 검증한 실제 크기 합계로 계산한다. 신고 sizeBytes는 양의 정수이며 개별 파일도 게시물 전체 한도를 초과할 수 없다. 확장자/신고 MIME만으로 통과시키지 않고 실제 JPG/PNG 내용을 검증한다.
- 최초 예약 createdAt와 최초 검증 완료 uploadedAt을 유지한다. UPLOADING의 cleanupEligibleAt은 createdAt+24h, UNLINKED는 uploadedAt+24h. 업로드 권한의 upload.expiresAt와 연결 마감 linkExpiresAt은 별개다. LINKED/LEGACY는 자동 만료 대상이 아니다.

### 13.2 API와 공통 상태 DTO

| Method·경로 | 요청 | 성공 응답·멱등 의미 |
| --- | --- | --- |
| POST /photo-uploads | JSON `{originalName, contentType, sizeBytes}`. contentType은 image/jpeg 또는 image/png 신고값 | 201 `{data:{fileId,status:"UPLOADING",createdAt,cleanupEligibleAt,upload:{url,method,headers,expiresAt}}}`. method/headers는 실제 Storage adapter가 검증한 전송 정보이며 비밀 관리 키를 포함하지 않는다. 권한 발급 성공 전 만료 추적을 영속 기록한다. |
| POST /photo-uploads/{fileId}/complete | 빈 JSON 객체. 바이너리/실제 크기/owner/region 값을 받지 않음 | 200 `{data:PhotoUploadView}`. 실제 object를 검증한 뒤 최초 uploadedAt 설정·MIME/bytes 갱신·UNLINKED 전환. 반복 완료는 시간을 연장하지 않으며 이미 LINKED면 기존 검증 결과와 canAttach=false 반환. |
| GET /photo-uploads/{fileId} | 본인 fileId, body 없음 | 200 `{data:PhotoUploadView}`. DELETE_PENDING/DELETED/만료 상태도 본인에게 조회 가능. 조회 자체가 완료 검증·권한 재발급을 수행하지 않는다. |
| DELETE /photo-uploads/{fileId} | 본인 미연결 POST_PHOTO, body 없음 | 삭제 예약 확정/이미 대기면 202 `{data:PhotoUploadView}`. 이미 최종 DELETED면 200 같은 상태. LINKED는 409로 거부하고 게시물 변경 API를 거친다. 존재하지 않거나 타인 파일은 모두 404. |

PhotoUploadView 필드:

| 필드 | 타입·의미 |
| --- | --- |
| fileId, status | number, LEGACY/UPLOADING/UNLINKED/LINKED/DELETE_PENDING/DELETED |
| contentType, sizeBytes, url | 검증 완료 파일만 MIME/number/공개 URL, 미검증이면 null. 삭제 대기/완료의 url은 null이어도 이미 알려진 URL 접근 차단을 보장하는 뜻은 아님 |
| createdAt, uploadedAt | 전역 시각 문자열; uploadedAt은 미검증일 때 null |
| uploadAuthorizationExpiresAt | 마지막으로 발급한 모든 업로드 권한 중 최댓값의 만료시각, 발급 전/legacy는 null. signed URL/token 원문 반환 없음 |
| cleanupEligibleAt, linkExpiresAt | 정리 후보 시각 / UNLINKED 연결 마감시각. 자동 정리 제외 상태에는 null. canAttach는 요청 시점의 실제 참조·상태·소유권·시간을 재검사한 boolean |
| canAttach, deletionCompleted | boolean. deletionCompleted는 최종 DELETED만 true. 단순 예약/한 번의 Storage 삭제 성공은 false |
| deleteRequestedAt, deletedAt | nullable 시각. 최초 예약/최종 확인 시각이며 반복 취소로 예약 시각을 갱신하지 않음 |

검증 실패는 파일을 연결 가능 상태로 승격하지 않고 DELETE_PENDING 예약을 영속 저장한 뒤 오류를 반환한다. Storage 일시 장애는 검증 성공으로 처리하지 않는다. signed 권한 재발급은 최초 시각을 유지하고 상태/소유권을 검사하며 기존과 새 권한의 최대 만료를 보존한다. 발급 실패/프로세스 장애 때문에 이미 발급된 권한 추적을 잃지 않도록 #13에서 검증한다. 미완료 예약 수/용량·발급 빈도 제한 수치는 #13 전에 별도 합의하며 이 안은 임의 값을 추가하지 않는다.

### 13.3 게시물 JSON 사진 참조

- POST /posts는 JSON 요청의 기존 유형별 필드에 `photoFileIds:[101,102]`를 추가한다. 생략/빈 배열은 사진 없음, null·중복 ID는 입력 오류. 배열 순서가 최종 sort_order 0부터의 연속 순서다.
- PATCH /posts/{postId}는 JSON `photoOrder:[{"photoId":801},{"fileId":102}]`를 사용한다. 생략=기존 유지, []=모두 제거, 지정=최종 전체 사진 구성. 각 항목은 photoId 또는 fileId 하나만 허용하며 null·두 참조 동시 지정·같은 실제 파일의 중복·타 게시물 photoId를 거부한다.
- 기존 photoId는 해당 게시물의 사진만 유지/재정렬한다. 신규 fileId는 본인 POST_PHOTO·서버 검증 완료·미연결·미만료 파일만 허용한다. 같은 파일의 다중 게시물 연결은 서비스 검사와 DB UNIQUE(file_id)로 차단한다.
- 게시물/파일 권한 재검증, 모든 관련 media_files 행의 ID 오름차순 잠금, 참조·LINKED 갱신 및 제거된 파일의 DELETE_PENDING 예약은 같은 DB 트랜잭션이다. Storage 삭제는 커밋 이후 수행한다. rollback 시 기존 object를 먼저 삭제하지 않는다.
- POST/PATCH는 201/200 상세 DTO를 유지한다. PATCH에서 삭제 예약이 있으면 `meta.photoDeletion:{status:"PENDING",fileIds:[제거된 본인 fileId]}`를 함께 반환하고 본인 파일 상태 GET으로 최종 확인한다. 최종 삭제 확인 후에는 같은 GET이 deletionCompleted=true를 반환한다.
- DELETE /posts/{postId}의 기존 204는 게시물 비노출·관계 처리·삭제 예약 커밋을 뜻하며 Storage 최종 삭제 완료를 뜻하지 않는다. FE는 기존 상세에서 본인 fileId를 보존해 상태를 조회한다. 이 204 의미와 PATCH meta는 FE 확인 항목이며 실제 저장 파일 삭제는 #13/#16 인수 조건으로 남긴다.

### 13.4 오류안과 확인 조건

전역 `{code,message,details,traceId}` 오류 envelope를 유지하고 아래 code/status는 FE/BE2 확인 후 고정한다. 토큰·signed URL·worker claim·DB 원문 오류를 반환하지 않는다.

| HTTP | code 제안 | 의미 |
| --- | --- | --- |
| 401 / 403 | 기존 인증 오류 / REGISTRATION_INCOMPLETE 또는 기존 권한 오류 | 인증 실패 / 미가입 완료 및 최종 게시물 권한 거부. 정확한 인증 오류명은 #74 인증 계약을 따름 |
| 404 | PHOTO_UPLOAD_NOT_FOUND | 없음·타인·다른 용도 파일을 동일하게 처리 |
| 409 | PHOTO_UPLOAD_NOT_READY / PHOTO_UPLOAD_EXPIRED / PHOTO_ALREADY_LINKED / PHOTO_DELETION_PENDING | object 미확인·만료·중복 연결·취소/정리 경합. 재시도 전 GET 상태 확인 |
| 400 | VALIDATION_ERROR | 참조 중복/배열 구조/regionId 등 허용하지 않은 요청 필드 |
| 413 / 415 | PHOTO_SIZE_EXCEEDED / PHOTO_FORMAT_UNSUPPORTED | 검증된 용량 초과 / 실제 JPG·PNG 외 형식. 완료 실패 파일은 삭제 예약 |
| 503 | PHOTO_STORAGE_UNAVAILABLE | Storage 확인/권한 발급 일시 장애. 성공/삭제 완료로 표시하지 않음 |

FE/BE2 확인 대상은 숫자 ID, endpoint/요청·응답, Storage 전송 method/headers, JSON 최종 순서, 10,000,000 bytes, 오류와 재시도, 202 삭제 대기/200 최종 삭제/게시물 204 의미, 상태 polling 동작이다. #74에 실제 확인자·날짜·문서 SHA/PR·이견을 기록하고 계약과 필요한 DB 보완이 back/develop에 반영된 뒤 #13/#14/#16 의존 코드를 구현한다. FE 확인을 받은 증거가 아직 없으므로 최종 확정/구현 완료 체크를 하지 않는다.

## 내부 독립 개발 규약 (2026-10-07)

Backend 내부 호출은 [계약 검토표 §11](Discushion_API_CONTRACT_검토표_2026-10-07.md#11-독립-개발용-내부-인터페이스-상세안-2026-10-07)을 따른다. CurrentActor/MemberQualification/MemberWriteGuard/PostContext와 참여 batch 집계·게시물 batch 요약·삭제 시 북마크 해제 port를 준비했다. 이는 HTTP Request/Response 확정이나 실제 adapter 구현을 뜻하지 않는다.

BE1은 A·C·D/참여 집계, BE2는 B/게시물 요약·사진·환경을 맡으며 각자 자기 API/Migration을 작성한다. 삭제 시 공개/공유/추가 참여를 차단하고 북마크 자동 해제·사진 삭제 예약은 같은 DB transaction으로 처리한다. 참여/활동/기관 감사 관계는 보존하되 삭제 콘텐츠·선택지·결과는 비노출한다. 삭제 투표의 개인 이력은 기존 UNAVAILABLE 계약을 유지한다. 진행 중 투표의 질문·선택지·지역·주제 변경 금지, 종료 후 수정/삭제/신규·변경 제출 금지는 유지한다.

## #74 인증 잔여 계약 정리 — BE1

기준: `back/develop d9fe885`, AGENTS.md의 공통 기반 합의, 계약 검토표 §11과 GitHub #74/#4의 최신 완료 조건. 2026-10-07 사용자 요청으로 남은 인증 세부만 정리한다. 아래는 **기존 합의**, **provider 확인 사실**, **BE1 검토안/공동 확인 필요**, **실제 환경 확인 필요**를 구분한다. 공통 port/DTO·오류 envelope·권한/잠금·사진/게시물/집계·삭제 보존·담당 분담·CI/리뷰 조건은 변경하지 않는다. #4 adapter 구현·Dependency·Migration·환경 설정·실제 token 검증은 이번 범위가 아니다.

### 유지하는 합의와 인증 처리 경계

- FE는 Privy access token을 `Authorization: Bearer ...`로 전달하고 Spring 공통 계층이 직접 검증한다. refresh/identity token을 API Bearer로 대체하지 않고 자체 세션·accessToken/refreshToken 발급을 추가하지 않는다.
- CurrentActorProvider/VerifiedActor/LocalMember 및 MemberQualificationReader/MemberWriteGuard는 검토표 §11의 서명/의미 그대로 사용한다. 토큰 없음은 current 비어 있음, 잘못된 토큰은 인증 실패, 검증된 subject에 회원이 없으면 member 비어 있음이다. 요청 종료 후 컨텍스트를 비우며 다른 스레드/요청에 주체를 재사용하지 않는다.
- users.privy_user_id UNIQUE로 subject 1:1 연결을 유지한다. NULL legacy 회원을 이메일로 자동 연결하지 않고 신규 Privy 인증만으로 로컬 회원/가입 완료/지역·기관 자격을 생성하지 않는다. 가입 미완료는 registration_completed_at=NULL, 완료는 동의·프로필·활동 지역 저장이 원자 커밋된 이후다.
- 중요한 쓰기는 기존 MemberWriteGuard와 같은 트랜잭션에서 최신 회원·자격을 재조회한다. 기능별 최종 소유권/지역/기관/대상 상태 판정과 기존 잠금 순서는 바꾸지 않는다.

### provider 검증 사실과 #4 구현 수용 기준

공식 [access token 설명](https://docs.privy.io/authentication/user-authentication/access-tokens)과 [token 설명](https://docs.privy.io/authentication/user-authentication/tokens)에 따른 확인 사실: access token은 JWT/ES256, iss는 privy.io, aud는 해당 Privy app ID, sub는 Privy DID, iat/exp는 발급/만료 시각이다. 실제 exp를 검사하며 문서의 기본 수명을 서비스 고정 TTL로 복사하지 않는다. sid는 provider 세션 정보이며 로컬 회원 ID/권한이 아니다.

검토 기준은 신뢰한 앱 검증키로 서명을 검증하고 허용 알고리즘·iss/aud·비어 있지 않은 subject·필수 시각 및 유효기간을 검사하는 것이다. 검증 전 JWT decode/클라이언트 userId/email/역할로 주체를 만들지 않는다. 토큰의 jku/x5u 등을 신뢰해 임의 URL에서 키를 가져오지 않는다. 서명/클레임 실패와 검증에 필요한 키·provider 확인 불가 장애를 구분하며 실패 시 게스트로 강등하지 않는다.

**실제 키/검증기 확인 필요:** official access-token 페이지는 ES256 설명과 Ed25519 공개키 문구를 함께 포함한다. [RFC 7518 §3.4](https://datatracker.ietf.org/doc/html/rfc7518#section-3.4)의 ES256 키/알고리즘 규약과 일치하는 앱 검증키·공식 지원 검증 방법을 확인해야 한다. 확인 전 특정 Java SDK/키 타입/JWKS URL을 추측해 고정하지 않는다. 앱 ID·키 공급/회전·허용 clock skew·캐시/외부 호출 timeout의 실제 설정과 정상/위조 token 증거는 #4에서 BE1, 실행값은 BE2/#30과 확인한다. 이미 검증 가능한 신뢰 키가 있으면 매 요청별 provider user API 호출을 인증 필수로 만들지 않으며, 검증 불가 장애를 성공으로 우회하지 않는다.

### 로컬 회원 연결·가입 완료의 남은 세부

| 대상 | 유지/검토 기준 | 확인 상태·영향 |
| --- | --- | --- |
| 검증된 subject 조회 | Privy subject로만 로컬 회원 조회. 이메일/Request ID로 다른 회원 선택 금지 | 기존 합의, #4 identity adapter |
| 이메일 원본 | access token에 이메일이 있다고 가정하지 않는다. BE1안: 최초 가입 시 검증된 subject의 provider 서버 사용자 정보에서 검증된 email 계정을 확인하고 기존 users.email에 저장 | [provider user 조회](https://docs.privy.io/api-reference/users/get) 확인. 실제 email 계정 선택/검증시각·서버 credential/조회 방법은 #6/#7, 실행환경 BE2와 확인. FE 신고 이메일만 신뢰하지 않음 |
| 이메일 중복 | 다른 subject 또는 미연결 legacy의 기존 이메일과 충돌하면 기존 EMAIL_ALREADY_IN_USE 409로 거부. 기존 회원 자동 병합/연결·이메일 일괄 변경 없음 | 기존 UNIQUE/자동 연결 금지 유지. 대소문자/정규화 정책을 새로 추가하지 않음 |
| 미가입 | 유효 토큰이나 local member 없음. 가입 가능 상태와 일반 회원 API 접근을 구분하며 조회/로그인 상태 확인이 DB 회원을 자동 생성하지 않음 | §11 의미 유지. 아래 wire 제안은 FE 확인 필요 |
| 가입 미완료 | local member 있음/registration_completed_at 없음. 가입 재개와 일반 회원 기능을 구분. 프로필/활동 지역 존재만으로 자동 완료하지 않음 | §11 의미 유지. 새 API 권한 근거 아님 |
| 가입 저장 | provider 확인을 DB 잠금 중 외부 호출로 수행하지 않는다. 기존 subject/이메일 중복을 다시 확인하고 회원·동의·프로필·활동 지역·완료시각을 같은 트랜잭션에 저장. 기존 미완료 회원 갱신은 §11 users 행 잠금 사용 | #7 실제 DB/경합/rollback 검증 필요. UNIQUE 경합 결과를 성공/자동 계정 연결로 숨기지 않음 |

### FE 데이터 계약 검토안 — 합의된 공통 규약을 변경하지 않음

다음은 아직 비어 있는 Auth wire를 채우기 위한 제안이다. 기존 endpoint 후보를 재사용하며 구현·FE/BE2 확인 완료로 선언하지 않는다. 일반 프로필/자격/사진 DTO를 변경하지 않는다.

1. `POST /api/v1/auth/login`: Bearer 필요, JSON `{}`. 자체 비밀번호·이메일·token body는 받지 않는다. 새 세션을 발급하는 endpoint가 아니라 검증된 현재 주체의 로컬 가입 상태 조회로 정리하는 안이다. 200 성공 envelope에 registrationStatus와 nullable member를 반환한다. NOT_REGISTERED에서는 member=null, INCOMPLETE에서는 완료시각 null, COMPLETED에서는 완료시각이 존재한다. 반환 토큰·refresh token·Privy subject·역할/배지는 추가하지 않는다.

```json
{ "data": { "registrationStatus": "NOT_REGISTERED", "member": null } }
```

```json
{ "data": { "registrationStatus": "INCOMPLETE", "member": { "id": 23, "registrationCompletedAt": null } } }
```

```json
{ "data": { "registrationStatus": "COMPLETED", "member": { "id": 23, "registrationCompletedAt": "2026-10-07T12:00:00+09:00" } } }
```

2. `POST /api/v1/auth/sign-up`: Bearer 필요, JSON의 기존 agreements/profile 입력을 유지하고 자체 emailVerificationToken/password/passwordConfirmation 및 client subject/회원 ID를 제외하는 안이다. 이메일은 위 서버 확인 기준을 따른다. 약관/프로필/지역의 기존 제품 검증은 그대로다. 프로필 사진의 미정 계약을 게시물 사진 규칙으로 채우지 않는다. 신규 원자 저장 201은 위 COMPLETED 상태와 member를 반환하고 토큰을 발급하지 않는다. 기존 완료 회원의 재가입/유실 응답 재시도는 login 상태 조회로 먼저 확인하는 안이며 정확한 중복 POST 응답은 #7/FE 검토 후 고정한다.

3. login 상태 조회의 200은 지역/기관 기능 허용을 뜻하지 않는다. 일반 회원 endpoint는 현재 가입 상태와 기존 제품 자격을 검사한다. returnTo는 FE에서 보존·검증하고 기존 복귀 규칙을 유지한다. 인증/가입 후 복귀만으로 원래 참여·북마크 요청을 자동 실행하지 않는다.

### 공통 오류 등록 제안 — 기존 표/구현은 변경하지 않음

기존 오류 envelope `{code,message,details,traceId}`와 HTTP 상태표는 유지한다. 토큰 없음/위조/만료·앱/발급자 불일치는 기존 401 UNAUTHORIZED로 매핑하고 내부 이유·token/provider 원문은 노출하지 않는다. 키/provider 확인 장애는 인증 실패와 구분해 기존 503 외부 서비스 장애 범주로 처리하되 code 등록은 아래 공동 검토 대상이다. DB 장애는 로컬 회원 없음/미가입으로 숨기지 않는다.

| 제안 | 이유 | 영향·확인 상태 |
| --- | --- | --- |
| 403 USER_REGISTRATION_REQUIRED | token은 유효하지만 로컬 회원 없음. 401로 보내 FE의 불필요한 재인증 루프를 만들지 않고 가입 진입을 구분 | 회원 endpoint/FE API client 영향. A 담당 BE1의 새 Auth 매핑 제안이며 공통 오류표에 아직 등록하지 않음 |
| 403 REGISTRATION_INCOMPLETE | 로컬 회원은 있으나 가입 미완료. 지역/기관 자격 오류와 가입 재개를 구분 | 회원 endpoint/FE 가입 재개 영향. 공통 오류표 등록 전 BE1·BE2/FE 검토 필요 |
| 503 AUTH_PROVIDER_UNAVAILABLE | 검증키/provider 사용자 정보 확인 불가 장애를 토큰 무효·미가입과 구분 | 공통 error adapter/FE 재시도 영향. 등록 전 공동 검토 필요, 실제 재시도값은 미정 |

§11.6의 내부 결과를 변경하거나 error DTO/공통 Java port에 필드를 추가하지 않는다. 위 세 code를 필요하다고 판단하면 이 표의 이유·영향으로 공동 검토한 뒤 별도 승인된 변경으로 등록한다. 승인 전 새 문자열을 임의 구현하지 않는다. 다른 영역의 기존 오류 매핑은 수정하지 않는다.

### 토큰 취득·보관·갱신·OTP·로그아웃 범위

- FE는 Privy SDK의 현재 access token 취득/갱신 기능을 사용하고 SDK 관리 refresh token을 앱이 읽거나 자체 Backend로 전달/저장하지 않는다. token은 URL/query·로그·분석·응답 echo에 넣지 않는다. 앱의 별도 토큰 복사 저장을 만들지 않으며 실제 FE SDK 저장 설정은 담당 FE/#30에서 확인한다. 서버는 요청 범위에서만 token을 처리하고 DB/session 저장 구조를 추가하지 않는다.
- 401에 대한 FE 갱신/한정 재시도는 SDK 결과와 endpoint 의미를 대조해 FE 데이터 계약으로 확인한다. 403 가입/자격 오류와 503 장애를 같은 재로그인 흐름으로 처리하지 않는다. 무제한 재시도·응답 유실된 쓰기의 자동 중복 제출·login 복귀 직후 원 동작 자동 실행은 허용 계약으로 추가하지 않는다. 요청 중복/재시도 횟수의 공통 규칙은 이 문서가 새로 정하지 않는다.
- 자체 OTP endpoint/고정 코드 길이·만료·재전송 횟수는 변경 전 이력이다. 실제 Privy SDK/앱의 이메일 인증 오류/제한과 FE 대응은 #6에서 확인하고 이번 문서에 추측한 수치를 채우지 않는다.
- 로그아웃은 기존 MVP 필수 제외를 유지한다. 별도 서버 logout/refresh/전 기기 종료/세션 blacklist를 추가하지 않는다. FE가 provider SDK logout을 사용하는 경우 [공식 동작](https://docs.privy.io/authentication/user-authentication/logout)을 확인해 UI·진행 요청을 정리한다. 직접 JWT 서명 검증만으로 이미 복사된 token의 즉시 서버 거부가 증명되지는 않으며, 그 보장이 필요해지면 공통 규약/범위 변경으로 먼저 제안한다. provider revoke를 검증했다고 기록하지 않는다.

### 착수·완료 검증과 남은 확인

문서/서명/fixture 준비와 실제 adapter 완료를 구분한다. #4 구현은 필요한 인증 계약·공통 port·필수 Schema가 back/develop에 반영된 뒤 별도 Issue/Feature에서 시작한다. 실제 앱 ID/지원 검증키·검증기와 위 wire/오류안의 확정 여부를 작업 시작 시 확인한다. 기존 test-only fixture는 독립 개발에 사용할 수 있으나 운영 bean/allowAll로 등록하지 않는다.

실제 #4/#6~8 검증은 정상/위조·다른 alg·iss/aud·만료·키 장애, 없음/미가입/미완료/완료, 이메일 충돌·subject 중복·가입 rollback/경합, 요청 컨텍스트 격리, 기존 guard의 실 JDBC 잠금/권한과 adapter 연결을 포함한다. 테스트 대체 구현만으로 Privy·DB·FE 연동 완료를 표시하지 않는다. #30/#31은 실제 FE 취득/갱신·상태 분기·환경 연결을 확인한다. CI와 리뷰 1명 조건을 유지하고 CODEOWNERS를 추가하지 않는다.

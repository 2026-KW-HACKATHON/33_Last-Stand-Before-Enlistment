# Discushion #2 — 공통 API 계약 검토표

## 2026-10-07 합의: 공통 인증·조회와 영역별 작성

사용자가 공통 기반 1~5번대로 팀 합의를 확인했다. 인증은 FE의 Privy Bearer access token을 Spring이 직접 검증하며 자체 세션 교환은 채택하지 않는다. 검증된 Privy 식별자로 로컬 회원을 연결하고 인증 성공·가입 완료를 구분한다. 토큰 보관/갱신·로그아웃 범위·미가입 처리·오류 등 정확한 FE/API 세부는 #74/#4에서 확인한다. 같은 주제의 인증 방식 선택 대기 표현보다 이 합의를 우선하되 구현·FE 검토·실제 연동 완료로 표시하지 않는다.

BE1은 A·C·D와 공통 인증/자격 조회, BE2는 B와 최소 게시물 내부 조회·실행환경을 담당한다. 각 담당자가 자기 API 정본 해당 절·ERD·Migration을 작성하고 BE1은 전체 정합성을 검토한다. 공통/타 영역·기존 합의 계약 변경은 사전 공동 검토하며 이미 적용된 Migration은 보존한다. 공유 DB 적용은 통합된 Migration을 BE2 조율 아래 지정 담당자가 수행한다.

게시물 기본 조회는 ID·유형·지역·작성자·공개/삭제 상태와 투표 선택지/종료 정보를 제공하는 내부 규약으로 먼저 준비한다. 공통 데이터/오류는 기존 API 정본의 규칙을 대조해 고정하고 새 형식을 임의 만들지 않는다. 정확한 내부 DTO/인터페이스는 구현 전에 합의한다. 필수 계약/Schema가 back/develop에 준비되면 합의된 대체 구현으로 독립 개발할 수 있지만 실제 상대 기능 연결과 권한/데이터 검증은 완료 조건으로 유지한다.

전체 규칙은 [AGENTS.md](../../AGENTS.md)의 “2026-10-07 합의: Backend 공통 기반과 독립 개발” 절을 따른다. 이전 BE1 전담 Migration 작성 요청은 담당 영역 작성+BE1 정합성 검토로 읽는다. 사진 공통 제약과 타 용도 영향은 공동 검토하며 사진 정책·한도는 유지한다. 실제 코드·DDL·환경·GitHub 이슈 담당/선행 갱신은 별도 작업이다.

> **#74/#3 DB 부분 후속:** 사용자(BE1)가 Privy subject↔회원 1:1, 가입 완료 시각, 업로드 완료 후 24시간 미연결 POST_PHOTO 정리·연결 보호·삭제 대기/재시도 저장 구조를 승인했다. [상세·검증·후속 경계](../architecture/Discushion_Issue3_MVP_v10.2_반영.md)를 참조한다. 이 승인은 BE2/FE 실제 검토나 토큰·SDK·Endpoint/DTO·파일 전송 계약 합의가 아니다.

## 2026-10-07 결정 반영

사용자가 확정한 아래 변경이 같은 주제의 변경 전 v10.1 본문·예시·MVP 표보다 우선한다. 상세 근거와 남은 계약은 [MVP 결정 변경 기록](../specs/Discushion_MVP_결정변경_2026-10-07.md)을 확인한다. 제품 범위·서비스 선택과 팀 계약 합의·실제 구현/연동 완료를 구분한다. 기능 ID는 유지하며 PRD·기능명세서의 현재 파일명과 참조는 v10.2로 갱신했다.

- **계정:** Privy 이메일 OTP로 인증·로그인한다. 최초 사용자는 필수/선택 동의·프로필·활동 지역을 완료해 로컬 회원으로 가입한다. 자체 비밀번호 설정·확인·로그인과 자체 가입 코드 발급은 대체한다. OTP 세부 제한과 토큰/회원 연결 계약은 #74에서 합의한다. `returnTo`를 전체 과정에서 보존하고 복귀만으로 참여·북마크를 자동 실행하지 않는다.
- **이웃 자격:** 증빙 선택·첨부·신청 제출·접수·실제 인증 처리는 이번 MVP에서 제외한다. 별도 시연용 계정의 완료 지역을 서버에 준비하고 상태 조회·최대 3지역·해당 지역 참여 자격은 유지한다. 일반 가입·Privy 로그인·활동 지역 설정·기관 자격만으로 이웃 자격을 부여하지 않는다. 계정 준비는 #75, 조회 계약은 #74/#11을 따른다.
- **기관 자격:** 기관 증빙 입력·첨부·자료 제출·접수·실제 심사는 이번 MVP에서 제외한다. 별도 시연용 계정의 기관 정본·담당 지역·유효기간/완료 상태를 준비하고 현재 유효 상태 기반 역할·배지·업무 권한은 유지한다. 기관 자격이 주민 참여의 이웃 자격을 대신하지 않는다. 계정 준비는 #75, 조회 계약은 #74/#12를 따른다.
- **사진:** 게시물 사진은 Supabase Storage에 저장하며 회원·게스트·사진 URL을 아는 앱 외부 사람 모두 열람할 수 있다. JPG/PNG·최대 10장·게시물 합계 10MB를 유지한다. 사진 제거·교체 및 게시물 전체 삭제 시 해당 저장 파일을 지운다. 미완료 업로드는 임시 보관 후 24시간이 지나면 정리한다. 업로드 권한·최종 연결·bytes 환산·삭제 보상·24시간 기준시각/정리 간격/경합·임시 파일 공개 시점은 #74/#13/#30에서 합의한다. 이는 게시물 임시저장 기능을 추가한다는 뜻이 아니다.
- **AI:** Google Gemini 3.5 Flash-Lite 선택. 공개 지역 안건 원문 기반 3문장 한 문단·원문 fallback은 유지한다. 실제 API 모델 ID·사용 가능 여부·키/요금·생성/저장/재생성/재시도 기준은 #20/#30에서 확인·합의한다.
- **진행:** 완료된 #2의 변경 후속은 #74, 별도 시연용 계정은 #75. 지역·기관·소유권·게스트 공유 범위는 유지한다. 비밀번호 복구·계정 변경/탈퇴 등 비-MVP 기능은 추가하지 않는다.

> 작성일: 2026-10-07 (Asia/Seoul)
> 관련 이슈: [#2 MVP 공통 API 계약과 미확정 항목 합의](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/issues/2)
> 작업 브랜치: back/feature/2-contract / 통합 대상: back/develop
> 상태: #2 기존 검토 기록 + #74 사용자 결정 반영. #2는 완료 상태이며 새 계약 확인과 실제 구현/연동 완료는 별도다.

## 1. 이번 작업에서 무엇을 정리했는가

사용자는 BE1이며 기존 소스의 API 명세를 참고해 진행하도록 요청했다. 따라서 기존 경로·숫자형 ID·Bearer 전달·DTO 구조를 변경하지 않고 공통 작업 기준을 추렸다. 원본에서 미정인 TTL·DB·파일 제한·삭제 보존·AI 세부를 확정됐다고 기록하지 않는다.

| 문서 | 역할 |
| --- | --- |
| [API 명세 v1.1.2](./Discushion_API_SPEC_v2.md) | v1.1.2: v10.2 사용자 결정을 반영하고 변경 전 인증·파일 계약을 구분한 초안. 새 경로/DTO는 #74 합의 대기 |
| [PRD v10.2](../specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md) | 제품 확정 정책과 MVP 범위 |
| [기능명세 v10.2](../specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md) | 기능 ID별 조건·인수 기준 |
| [통합 지침서](../specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md) | 데이터 원본·FE/BE 책임 |
| [ERD 상세 명세](../specs/Discushion_MVP_ERD_상세명세.md) | 논리 데이터 모델과 설계 제안. DB 생성 결과 아님 |

**변경 전 #2 작성 시점의 관찰:** 당시 #1·#2에는 합의 댓글이 없고 backend/에는 README만 있었다. 코드에서 이미 정한 프레임워크·DB·세션 설정은 확인되지 않았다. ERD 정정 PR #35는 확인 시점에 미병합이므로 이 브랜치에는 해당 PR 변경을 임의로 섞지 않았다.

표시를 구분한다.

- **제품 확정:** PRD·기능명세의 확정 정책. 이 검토표에서 재정의하지 않는다.
- **기존안 유지:** API v1.1의 제안을 그대로 사용하는 BE1 작업 기준. FE·BE2 확인 전 팀 합의 완료로 표시하지 않는다.
- **결정 대기:** 원본이 비워 둔 항목. §6에 결정 역할·관련 이슈·필요 시점을 기록했다.
- **신규 제안:** 기존 API에 없는 해결 방식. 사용자·FE/BE 검토 후에만 계약으로 반영한다.

## 2. 공통 계약 — 기존 API안을 유지

아래 C01~C16은 검토 ID이며 기능명세 ID를 대체하지 않는다.

| ID | 항목 | 유지할 계약 | 근거 | 상태 |
| --- | --- | --- | --- | --- |
| C01 | 경로·버전 | Base /api/v1은 기존 제안. Privy 가입/로그인·사진 업로드 변경 경로는 #74 합의 대기, 증빙 POST 제외 | API §3·4 | 변경 계약 대기 |
| C02 | 식별자 | JSON number 유지. 정수이며 JS 안전 정수 범위를 초과한 ID를 number로 보내지 않음. UUID/string 전환 시 모든 DTO/mock 함께 변경 | API §1.1 | 기존안 유지 |
| C03 | 회원 인증 전달 | Privy 검증 주체와 로컬 회원 연결. 직접 토큰/세션 교환·전달/보관·갱신은 #74에서 합의 | API §4.2~4.4 | provider 확정 / 계약 대기 |
| C04 | 시간 | 날짜 YYYY-MM-DD, 절대 시각 ISO 8601 offset 포함, 표시 Asia/Seoul. 투표 종료·기관 만료는 서버 시각 판정 | API §1.1·6.4·4.8 | 기존안 유지 / 종료·만료 제품 확정 |
| C05 | 성공 응답 | 객체는 data, 목록은 data 배열 + meta.nextCursor/hasNext. 204는 본문 없음 | API §1.1·1.2 | 기존안 유지 |
| C06 | 오류 응답 | code/message/details/traceId 형식. 401 미로그인·무효 세션, 403 자격 부족, 404 대상 없음/삭제, 409 상태 충돌 구분 | API §1.1~1.2·2.3 | 기존안 유지 |
| C07 | null·생략 | 계약상 nullable은 null. 게스트의 본인 데이터는 필드 생략. 빈 목록·사진 없음은 배열 [] | API §1.1·5.3 | 기존안 유지 |
| C08 | PATCH 의미 | API에 명시된 허용 필드만 갱신. 사진 photoOrder 생략=유지, []=전체 제거. profileImage 교체와 removeProfileImage 동시 요청 거부 | API §4.5·5.5 | 기존안 유지 |
| C09 | 목록·필터 | 불투명 cursor + size. 다음 페이지 없으면 nextCursor=null/hasNext=false. type/topic 전체는 생략, ALL 저장 금지 | API §1.1·2.1·8.1 | 기존안 유지 / ALL 저장 금지 제품 확정 |
| C10 | Enum | API §2.1·2.2의 대문자 코드명 유지. 새 심사/행정 처리 상태 임의 추가 없음 | API §2 | 코드명 기존안 유지 / 의미 제품 확정 |
| C11 | 파일 요청 | Supabase Storage 게시물 사진; 최종 파일 참조·형식·합계·소유권 검증. 직접 업로드 DTO 대기, 증빙 제외 | #74·#13 | 저장/제품 정책 확정 / 전송 계약 대기 |
| C12 | 공유 게스트 | 특정 postId에 귀속된 공유 컨텍스트. 기존 X-Post-Share-Token 전달안을 유지. 일반 목록·지도·개인 기록 권한 없음 | API §1.4·5.7 | 전달 방식 기존안 유지 / 범위 제품 확정 |
| C13 | 로그인 복귀 | FE가 returnTo를 가입·프로필·지역까지 유지. 서버가 받는 구현은 허용 내부 경로 검증. 복귀만으로 행동 자동 실행 없음 | API §1.4·4.3~4.4·6.5 | 기존안 유지 / 자동 행동 금지 제품 확정 |
| C14 | 현재 관계 쓰기 | 반응·평가·북마크는 PUT=원하는 관계 상태, DELETE=관계 제거. 동일 최종 상태 재시도는 중복 관계·활동 증가 없음 | API §6.2~6.5·9 | 기존안 유지 |
| C15 | 원본·집계 | 모든 화면은 같은 Post·관계 원본. 현재 참여와 누적 활동을 분리. 게스트/타인 행동·북마크만으로 본인 참여 카드 생성 금지 | API §8~9 | 제품 확정 |
| C16 | 공개 데이터 | 비밀번호·타인 개인 표·기관 담당자 개인정보·증빙·내부 경로 미노출. 활동 문의만 작성자 현재 등록 이메일 참조 | API §1.1·5.3·7.2 | 제품 확정 |

숫자형 ID 유지가 DB의 PK 타입 확정을 뜻하지 않는다. bigint를 채택할 때도 C02의 안전 정수 경계를 만족하도록 발급·직렬화 계약을 맞춘다. count, percentage, size는 ID와 구분한다. 변경 전 자체 6자리 코드 규칙은 현재 Privy 계약을 확정하지 않는다. OTP 입력 형식은 확인된 provider 정책으로 맞춘다.

## 3. FE와 대조할 공통 예시

아래는 기존 DTO 형식 확인용이며 새 필드·타입을 추가하지 않는다.

성공 객체:

```json
{ "data": { "postId": 101, "isBookmarked": true } }
```

빈 목록:

```json
{ "data": [], "meta": { "nextCursor": null, "hasNext": false } }
```

필드 오류:

```json
{
  "code": "VALIDATION_ERROR",
  "message": "요청값을 확인해 주세요.",
  "details": [{ "field": "title", "reason": "필수 입력값입니다." }],
  "traceId": "server-generated-id"
}
```

nullable과 게스트 필드 생략의 차이:

| 경우 | 예시·의미 |
| --- | --- |
| 회원 미평가 | myEvaluation=null. 회원 본인 선택 정보는 존재하지만 값 없음 |
| 게스트 | myEvaluation/myState/vote.myOptionId 자체를 생략. 회원 개인 상태로 오해하지 않음 |
| 사진 없음 | thumbnailUrl=null, images=[] |
| 삭제 투표 개인 기록 | availability=UNAVAILABLE, post=null, vote=null. 제목·본문·옵션·이미지·결과 재노출 금지 |

기관 DTO NOT_SUBMITTED는 미신청, EXPIRED는 만료를 뜻한다. 접수 RECEIVED와 완료 COMPLETED를 같게 처리하지 않는다. API에 명시되지 않은 다른 PATCH 필드의 null=삭제 규칙은 일괄 가정하지 않고 기능별 계약에서 지정한다.

## 4. 권한·상태 계약

| 요청 범위 | 조건 |
| --- | --- |
| 가입 이메일 코드·가입·로그인 | 비로그인 진입 가능. 이메일 코드 인증은 로그인 세션과 구분 |
| 지역 후보 | 가입 초기 지역 선택을 위한 인증 예외는 API §4.6의 기존안 |
| 메인·게시물 목록·지도 | 로그인 회원. 공유 게스트는 거부 |
| 공개 특정 상세·댓글 조회·안건 요약 | 회원 또는 해당 postId의 유효 공유 게스트. 요약은 공개 지역 안건만 |
| 게시·회원 댓글/답글·반응·평가·투표 | 해당 지역 이웃 인증 완료 회원. 기본 활동 지역·복수 속성·기관 인증으로 대체 불가 |
| 공유 게스트 댓글/답글 | 해당 공유 게시물만. 공개명 게스트, 회원 활동 이벤트·회원 이관 없음 |
| 북마크 | 로그인 회원, 이웃 인증 불필요 |
| 개인 목록·활동 | 로그인 본인. userId query로 다른 사람 선택 금지 |
| 기관 전체 안건 목록 | 현재 유효 기관 완료 인증. 담당 지역 밖도 열람 가능 |
| 기관 채택·취소 | 현재 유효 기관 인증 + 담당 지역 공개 지역 안건 + 본인 기관 관계 |

동적 조건은 조회 capabilities만 믿지 않고 서버가 매 쓰기에서 재검증한다. 무효 회원 토큰을 자동으로 게스트 쓰기로 전환하지 않는다. 로그인 미인증 회원이 공유 토큰을 제출해 회원 지역 자격을 우회할 수 없다.

반응 합계는 3종 관계 수의 합, 댓글 수는 부모+답글, 표는 회원당 현재 한 표다. 부모 댓글 좋아요순에 답글 좋아요를 합산하지 않는다. 기관 채택은 Post 상태를 바꾸지 않는다.

## 5. 가입 중 이웃 인증 진입 — 검토가 필요한 한 가지 흐름

Privy 이메일 OTP로 인증·로그인한다. 최초 사용자는 필수/선택 동의·프로필·활동 지역을 완료해 로컬 회원으로 가입한다. 자체 비밀번호 설정·확인·로그인과 자체 가입 코드 발급은 대체한다. OTP 세부 제한과 토큰/회원 연결 계약은 #74에서 합의한다. `returnTo`를 전체 과정에서 보존하고 복귀만으로 참여·북마크를 자동 실행하지 않는다.

증빙 선택·첨부·신청 제출·접수·실제 인증 처리는 이번 MVP에서 제외한다. 별도 시연용 계정의 완료 지역을 서버에 준비하고 상태 조회·최대 3지역·해당 지역 참여 자격은 유지한다. 일반 가입·Privy 로그인·활동 지역 설정·기관 자격만으로 이웃 자격을 부여하지 않는다. 계정 준비는 #75, 조회 계약은 #74/#11을 따른다.

변경 전 가입 후 증빙 신청 진입 확인은 이번 결정으로 대체한다. FE는 미구현 신청으로 이동시키지 않으며 현재 자격 조회와 제한 안내를 계약에 맞춘다.

<details>
<summary>변경 전 v10.1 / API 초안 — 이번 MVP에 적용하지 않음</summary>

아래 내용은 변경 이력 보존용이다. 현재 동작·완료 조건은 위의 2026-10-07 기준을 적용한다.

기능명세 F-KZRSXU/F-RBVFZX는 가입 프로필·활동 지역을 완료해 회원 상태로 진입하도록 정의한다. 현재 API §4.3은 프로필·기본 지역을 포함한 sign-up 성공 시 세션을 제공하고 §4.7은 로그인 본인만 인증 신청하도록 한다. 최종디자인 A08은 그 전에 이웃 인증으로 이동하는 연결을 갖는다.

**사용자 확인된 진행 방식:** A08에서 이웃 인증 의도를 선택하면 입력값·returnTo·인증 진입 의도를 FE가 보존하고, 가입·활동 지역 설정을 완료한 뒤 생성된 세션으로 이웃 인증 신청에 진입한다. 가입 취소/실패 시 입력과 진입 의도를 유지한다. 인증 취소/접수 후의 최종 화면 복귀 경로는 FE와 정하되, 이미 완료한 가입을 되돌리거나 원 게시물 returnTo를 잃지 않는다. 가입 전 게스트 인증 신청 API나 임시 계정을 추가하지 않는다.

현재 상태: 사용자(BE1)가 2026-10-07 채팅에서 가입 완료 후 인증으로 연결하도록 확인했다. API v1.1.1의 §4.3·4.7에 이 순서를 반영했다. FE·BE2 구현 확인과 인증 후 복귀 화면의 세부는 미기록이다. 영향 이슈는 #7·#10·#11이며 회원·세션 생성 이후에만 인증 신청을 저장한다.

</details>

## 6. 원본에 남은 미정 사항과 결정 역할

역할은 제안하는 확인 주체이며 실제 담당자 계정 배정·연락 완료를 뜻하지 않는다. 제품 입력/보존 정책은 BE1 혼자 제품 확정으로 선언하지 않는다.

| ID | 남은 결정 | 확인 주체 | 관련 이슈·결정 시점 |
| --- | --- | --- | --- |
| D01 | 서비스 선택 완료: Java/Spring/Gradle·Supabase PostgreSQL/Storage·Privy·Gemini. 초기화 버전/Health 팀 확인·실제 구성은 남음 | BE2·BE1·FE | #1·#74·#30 |
| D02 | Privy 토큰 검증/세션 교환·식별자/로컬 회원 연결·전달·보관·만료/갱신·오류 | BE1·BE2·FE | #74·#4·#8, 인증 구현 전 |
| D03 | 자체 verificationToken은 대체. Privy OTP 제한·오류·최초 가입 완료 상태와 회원 연결 합의 | BE1·FE, 환경 BE2 | #74·#6·#7 |
| D04 | 프로필 사진 형식·용량·제거/교체와 실패 보상 | 제품 확인 + BE1·FE, 파일 환경 BE2 | #7·#10, 사진 처리 구현 전 |
| D05 | 이웃 증빙 제출/접수는 MVP 제외. 완료 지역 조회·시연 계정 상태 준비는 유지 | BE1·BE2·FE | #11·#75 |
| D06 | 게시물 10MB bytes 환산·직접 업로드·참조·공개 시점·24시간 기준/정리 간격·삭제 보상 | BE1·BE2·FE | #74·#13·#30 |
| D07 | #22 사용자 위임 초기 금칙어4개(씨발·개새끼·병신·좆같), 동일 literal 포함 검사 | BE1 작성·BE2 최신 PR 정합성 리뷰 | API §6.1. 변형 탐지/AI/관리 UI 제외, 실제 FE 연결 후속 |
| D08 | #14: 길이 제한 추가 없음, 선택 URL은 절대 http/https, 투표 종료는 DB 현재 시각 이후, 선택지는 앞뒤 공백 정리 후 중복 거부, 활동 schedule은 비어 있지 않은 원문 문자열로 저장. #16/#25 등은 각 이슈 입력 계약에서 별도 확인 | #14 사용자 결정 (2026-10-08); 나머지는 제품 확인 + BE1·BE2·FE | #14 결정 반영 완료; #16·#25 등 해당 입력 구현 전 |
| D09 | 사진 저장 파일 삭제 확정. 다른 관계의 물리 보존·소프트 삭제·투표 상태 필터는 미정 | BE1·BE2·FE | #3·#13·#16·#26·#27 |
| D10 | cursor 형식·size 기본/상한·기본 정렬·ID 동률 정렬·초기 댓글 수/답글 페이지. #28 담당자 안건 목록의 size/정렬/scope/cursor는 API §7.1 결정 반영; #9의 사용자 채택·구현/FE 확인 경계는 §14, 그 밖의 목록 계약은 미정 유지 | BE1·BE2·FE | #9·#15·#17·#22·#27, 각 목록 구현 전 |
| D11 | Region 계층·지도 원천·기관 정본 식별/seed는 확인 필요. #12 동시 유효 기관 자격 1개·달력 1년/윤년 계산은 §15 사용자 채택 기준 | BE1·BE2, 남은 원천/seed 확인 | #3·#9·#12·#19·#29, Schema·권한 구현 전 |
| D12 | #21 사용자 채택: DB 미저장 전용 서명·7일·기간 내 재사용·기존 링크 유지. 세부 wire/port/실제 연결 경계는 §16 | BE1 작성, BE2 소비/환경·FE 연결 확인 | #21, 실제 adapter·환경·FE 연결은 기능 완료 전 |
| D13 | Gemini 3.5 Flash-Lite 선택. 실제 API 모델 ID·생성/저장/재생성·재시도·짧은 원문은 확인/합의 필요 | BE2·BE1·FE | #74·#20·#30 |
| D14 | 이벤트 저장·원자성 범위와 생성 POST 멱등 키의 범위/TTL/payload 비교. #14는 MVP 멱등 키를 추가하지 않으며 재전송은 새 게시물 생성으로 처리한다고 사용자 결정 | BE1·BE2; #14 예외 결정은 사용자 | #5 및 각 생성 이슈, 쓰기 구현 전 (#14 결정 반영 완료) |
| D15 | 득표율 소수 자릿수·반올림·결과 재조회 방식·서버 시각 전달 세부 | BE2·BE1·FE | #25·#27, 투표·개인 결과 구현 전 |
| D16 | 동일 채택 취소 재시도 204/404 및 채택 후 안건 지역 변경 | BE1·BE2·FE, 지역 변경 정책 제품 확인 | #16·#29, 수정·채택 구현 전 |
| D17 | 가입 후 증빙 인증 신청 진입은 이번 MVP에서 대체. Privy·동의/프로필/지역·returnTo와 자격 제한 안내 확인 | BE1·FE | #74·#7·#41~#45 |

API §11.1의 모든 미정 분류를 위 표에 대응했다. 실제 금칙어·파일 정책·TTL 등에 임의 수치를 채우지 않았다. provider 선택과 배포는 #1/#30 범위이고 이 문서가 그 작업을 대신 완료하지 않는다.

## 7. 변경 전 #2 경로/담당 추적 (변경 API는 #74에서 대조) — API·담당·이슈 대응 — 기존 39개 유지

모든 경로 앞에 /api/v1을 붙인다. 이 표는 구현 순서가 아니라 계약의 소유·누락 점검용이다. 기존 Method+Path를 추가·삭제하지 않았다.

| Part·담당 | Method | Endpoint | 구현 이슈 |
| --- | --- | --- | --- |
| 공통·BE2 | GET | /health | #1 |
| A·BE1 | POST | /auth/email-verifications | #6 |
| A·BE1 | POST | /auth/email-verifications/confirm | #6 |
| A·BE1 | POST | /auth/sign-up | #7 |
| A·BE1 | POST | /auth/login | #8 |
| A·BE1 | GET | /users/me | #10 |
| A·BE1 | PATCH | /users/me | #10 |
| A·BE1 | GET | /regions | #9 |
| A·BE1 | GET | /users/me/neighbor-verifications | #11 |
| A·BE1 | POST | /users/me/neighbor-verifications | #11 |
| A·BE1 | GET | /institution-verifications | #12 |
| B·BE2 | GET | /home | #18 |
| B·BE2 | GET | /map/dongs | #19 |
| B·BE2 | GET | /posts | #17 |
| B·BE2 | POST | /posts | #14 |
| B·BE2 | GET | /posts/{postId} | #15 |
| B·BE2 | PATCH | /posts/{postId} | #16 |
| B·BE2 | DELETE | /posts/{postId} | #16 |
| B·BE2 | GET | /posts/{postId}/summary | #20 |
| C·BE1 | GET | /posts/{postId}/share-link | #21 |
| C·BE1 | GET | /posts/{postId}/comments | #22 |
| C·BE1 | POST | /posts/{postId}/comments | #22 |
| C·BE1 | POST | /comments/{commentId}/replies | #22 |
| C·BE1 | PUT | /posts/{postId}/reactions/{reactionType} | #23 |
| C·BE1 | DELETE | /posts/{postId}/reactions/{reactionType} | #23 |
| C·BE1 | PUT | /comments/{commentId}/evaluation | #24 |
| C·BE1 | DELETE | /comments/{commentId}/evaluation | #24 |
| C·BE2 | PUT | /posts/{postId}/vote | #25 |
| D·BE1 | PUT | /posts/{postId}/bookmark | #26 |
| D·BE1 | DELETE | /posts/{postId}/bookmark | #26 |
| D·BE1 | GET | /officer/agendas | #28 |
| D·BE1 | POST | /posts/{postId}/adoptions | #29 |
| D·BE1 | DELETE | /posts/{postId}/adoptions/{adoptionId} | #29 |
| D·BE1 | GET | /users/me/posts | #27 |
| D·BE1 | GET | /users/me/participations | #27 |
| D·BE1 | GET | /users/me/votes | #27 |
| D·BE1 | GET | /users/me/bookmarks | #26 |
| D·BE1 | GET | /users/me/activity | #5 |

## 8. 합의 기록

| 대상 | 상태 | 확인자·날짜·근거 |
| --- | --- | --- |
| BE1 작업 기준 | 기존 소스 API를 참고해 진행 | 사용자 2026-10-07 채팅 지시. 팀 전체 승인과 구분 |
| FE 공통 계약 C01~C16 | 확인 대기 | 미기록 |
| BE2 공통 계약 C01~C16 | 확인 대기 | 미기록 |
| 가입 완료 후 이웃 인증 진입 | 사용자(BE1) 확인 완료, FE·BE2 확인 대기 | 2026-10-07 채팅: 가입 완료 후 인증으로 연결 |
| D01~D17 결정 역할·후속 추적 | 검토안 작성 | 실제 담당자/결정·확인 미기록 |
| API/코드/DB 실제 연동 | 미실행 | 구현 코드 미확인 |
| 문서 정합성 검사 | 통과 | 2026-10-07: 기존 API 39개·이슈 대응 39개·원본 JSON DTO 예시 30개 유지, 상대 링크 11개 정상, 공통 계약 16개·결정/확인 항목 17개 및 변경 형식 검사 확인 |
| GitHub 공지·Issue 완료 | 미수행 | 외부 댓글/완료 처리 없음 |

합의 시 결정 내용·확인 역할·날짜·영향 이슈를 기록하고 API 정본에 합의된 내용만 반영한다. 이 검토표가 생겼다는 이유로 #2를 닫지 않는다. 원본의 설계 제안을 일괄 제품 확정으로 바꾸지 않는다.

## 9. #2의 다음 확인과 완료 기준

1. FE·BE2가 C01~C16의 공통 작업 기준을 확인하고 ID·세션·응답·nullable·시간·목록·파일·공유의 실제 계약을 기록한다.
2. #74에서 Privy·로컬 가입/returnTo·완료 지역 조회·신청 UI 제외의 계약을 맞추고 #6~#8/#11 및 FE #41~#44에 연결한다.
3. D01~D17의 미정 사항은 확인 역할과 관련 이슈에 연결하고, 각 기능 구현 전 결정할 항목을 구분한다.
4. 실제 합의된 계약을 API 정본에 반영하고 FE/BE의 DTO/mock 영향과 적용 순서를 검토한다.
5. 합의 증거와 문서 검사 결과를 #2 작업 기록으로 남긴 후 리뷰한다. 구현·실제 연동은 후속 이슈에서 별도로 검증한다.

BE1이 바로 할 수 있는 준비(기존안 정리·미정 목록·담당/이슈 연결)는 작성했다. 아직 다른 담당자의 확인을 받지 않았으므로 #2의 팀 합의 완료 조건은 남아 있다.

## 2026-10-07 변경 후 진행 상태

#2는 완료 상태를 유지한다. 새 사용자 결정의 계약/문서 후속은 #74이며 시연 계정은 #75다. 위 과거 #2 확인/미확인 표는 당시 기록이며 현재 합의·구현·연동 완료의 증거가 아니다. 이번 문서 반영은 provider/제품 범위 결정만 기록하며 토큰·DTO·Schema와 FE/BE 상세 합의는 남아 있다.

## 10. #74 BE2 사진 계약 검토안 (2026-10-07)

**상태: 1~4번 실행 방향은 사용자 채택, API wire 계약·BE1 Schema 검토는 대기.** 사용자 요청으로 BE2가 준비할 수 있는 사진 처리 계약을 구체화했다. 실제 endpoint·DTO·Enum·DDL을 확정하거나 #13을 구현한 기록이 아니다. API 정본은 BE1 확인 후 갱신하고, FE가 사용하는 요청/응답은 해당 기능 구현 전에 맞춘다. FE 문서 전체 최종 승인과 실제 연결 검증은 사용자 지시에 따라 #30으로 이관한다. 기능 ID는 `F-GSMCLD`, 영향 Issue는 #13/#14/#16/#30/#74다.

### 10.1 확정 정책과 검토 범위

- Supabase Storage 사용, JPG/PNG·최대 10장·게시물 합계 10MB, 사진 URL을 아는 누구나 열람 가능, 제거/교체/게시물 삭제 때 저장 파일 삭제, 미완료 파일 24시간 후 정리는 사용자 확정이다.
- 공개 사진 열람은 일반 게시물 탐색·공유 API나 지역 참여 권한을 확장하지 않는다. 업로드에는 가입 완료 회원, 파일 취소에는 소유권을 확인한다. 게시물 연결/변경/삭제에는 명세의 회원·소유권·지역 자격·유형/상태 제약을 적용하며, 파일을 업로드 지역에 묶지 않는다.
- 미완료 임시 사진도 업로드 직후 URL을 아는 사람에게 공개 열람을 허용한다(2026-10-07 사용자 답변). 24시간 기준·정리·소유자 연결·삭제 보상의 실행 방향은 §10.9의 사용자 채택 결정을 따른다. 바이트 환산·API 경로/DTO·Schema/Migration 반영·BE1/FE 데이터 계약 확인은 남아 있다. 증빙 제출이나 게시물 임시저장 기능은 추가하지 않는다.

### 10.2 전송 흐름 제안

```text
FE → BE: 가입 완료 회원을 확인하고 본인 파일별 업로드 권한 요청 (지역은 최종 게시 때 재검사)
BE → FE: 서버가 정한 fileId와 해당 object 전용 업로드 정보 반환
FE → Storage: 파일 바이너리 직접 전송 (BE를 경유하지 않음)
FE → BE: 업로드 완료 확인 요청
BE → Storage: object 존재·실제 크기·이미지 형식 확인
BE → FE: 게시물에 연결 가능한 파일 참조 반환
FE → BE: 게시물 생성/수정 JSON에 최종 파일 참조와 순서 전달
BE: 최종 권한·소유권·개수/합계·유효 상태 재검증 후 DB 연결
```

시연 규모의 우선 검토안은 signed standard upload다. Supabase는 signed upload URL에 추가 사용자 인증이 필요 없고 유효기간은 2시간이라고 안내한다. 이 권한은 서버가 가입 완료 회원·파일 소유권·유효 상태를 확인한 뒤 특정 object에만 발급하고, 서버 secret은 FE에 전달하지 않는다. URL/토큰은 로그·분석 도구에 남기지 않는다. Privy 사용자가 Supabase Auth 사용자라고 가정하지 않는다.

Standard upload는 6MB 초과 파일에서 TUS가 권장되므로, 6~10MB 단일 파일도 허용하는 현재 정책에서 실제 성공·실패/재시도 검증이 필요하다. #13에서 시연 네트워크로 검증한 뒤 TUS 필요 여부를 결정한다. 제품 한도를 6MB로 줄이거나 FE SDK 설치를 선행조건으로 만들지 않는다.

### 10.3 앱 API 후보와 DTO 의미

Base path `/api/v1` 뒤에 붙일 아래 경로는 **후보**다. 인증 header/cookie는 #74 인증 계약을 따른다. ID의 wire 타입은 기존 전역 ID 계약과 BE1 확인 후 맞추며 임의로 새로운 전역 타입을 정하지 않는다.

| 후보 API | 입력 | 출력·동작 제안 |
| --- | --- | --- |
| `POST /photo-uploads` | `originalName`, `contentType`, `sizeBytes` (regionId 제외 방향 채택) | 201. `data`에 `fileId`, `upload` 반환. upload에는 URL·실제 전송 method·필요 header·권한 만료시각 제공. 업로드 권한 만료와 게시 연결 만료는 구분하며, 완료 전 연결 만료시각 필드는 계약 확정 전 보류. 정확한 method/header는 #13에서 Storage REST/adapter와 대조 |
| `POST /photo-uploads/{fileId}/complete` | 파일 바이너리 없음 | 200. 서버가 실제 object를 확인한 뒤 `fileId`, 검증된 `sizeBytes`·`contentType`, 연결 가능 상태 반환. 같은 파일의 반복 완료는 같은 결과; 클라이언트 성공 통지만 믿지 않음 |
| `GET /photo-uploads/{fileId}` | 본인 파일 참조 | 200. 상태·만료·연결 가능 여부 조회. 업로드 응답 유실 후 확인용. 삭제/만료 참조는 아래 오류 계약으로 처리 |
| `DELETE /photo-uploads/{fileId}` | 본인 미연결 참조 | 응답 코드/DTO 미정. 취소와 연결 금지·삭제 예약을 영속 기록하며 물리 삭제 완료와 구분. 반복 취소는 같은 결과. 연결된 파일은 이 API로 삭제하지 않고 게시물 수정/삭제 권한을 거침 |

`sizeBytes`·`contentType`은 업로드 전 안내/예약에 쓰는 신고값이며 저장 후 서버 검증값과 구분한다. `originalName`은 표시용이고 저장 경로를 결정하지 않는다. object key는 서버 난수로 만들며 충돌 시 덮어쓰지 않는다. 일반 게시물 DTO는 파일 참조와 URL만 제공하고 Storage 관리 키를 받지 않는다. 업로드 URL 자체에 object 경로가 포함될 수 있으므로 ERD의 storage_key 비노출 문구는 BE1과 이 범위를 구분한다.

#3의 media_files는 MIME/size_bytes가 NOT NULL이고 size_bytes>0을 요구한다. UPLOADING 생성 때는 신고값을 임시 저장하고 서버 확인 후 실제 값으로 바꾸는 실행안을 검토하며, 신고값 상태를 최종 합계 검사에 사용하지 않는다. 현재 컬럼만으로 신고값·검증 결과를 분리할 수 있는지 BE1이 확인한다. 특히 취소 API의 응답 코드·멱등성은 후보이며 UPLOADING 취소를 현재 CHECK로 구현할 수 있다고 해석하지 않는다(§10.8).

`upload` 권한 재발급이 필요하면 소유권·연결 가능 상태를 재확인하고, 이미 기록된 최초 `uploaded_at`을 바꾸지 않는 방식을 합의한다. 업로드 권한 만료시각과 `uploaded_at+24h` 연결 만료 후보를 하나의 `expiresAt`으로 혼용하지 않는다. 중복 권한 요청으로 무제한 파일을 예약할 수 없도록 사용자별 미완료 용량/개수·발급 빈도 제한은 #13 구현 전에 별도 값으로 정한다. 현재 DB에 권한 발급 이력이 없으므로 재발급·지연 업로드 방어의 저장 방법은 §10.8에서 검토한다.

### 10.4 게시물 연결·정렬·오류 제안

- 생성 요청은 기존 유형별 payload를 유지하면서 별도 바이너리 대신 순서가 있는 `photoFileIds` 후보 필드를 사용한다. 빈 배열은 사진 없음이다. 기존 multipart는 변경 전 계약으로 남기고 정본은 합의 후 바꾼다.
- 수정은 기존 `photoId`와 새 업로드 `fileId`를 구분한 최종 순서 배열을 제안한다. 생략=기존 유지, 빈 배열=전체 제거, 지정=최종 전체 구성. 두 참조를 동시에 가진 항목·중복·타 게시물 사진·타인/미검증/만료 파일을 거부한다. 기존 `newImageIndex`는 새 계약 확정 후 대체한다.
- 생성/수정 트랜잭션에서 게시물 소유권·작성/참여 자격을 재확인하고 최대 10장·전체 합계를 검증한다. **10MB=10,000,000 bytes**를 제안하되 BE1·FE 확인 전 구현하지 않는다. 기존 사진+추가 사진의 실제 저장 크기를 합산한다.
- 파일 형식은 확장자·신고 MIME만으로 허용하지 않고 실제 이미지 내용과 크기를 확인한다. 확인 중 Storage 장애는 업로드 성공으로 처리하지 않는다. 구체 검사 방식/자원 상한은 #13에서 정한다.
- 파일 연결은 한 게시물에 한 번만 가능하게 제안한다. 게시물 요청 실패/rollback이면 파일은 미연결 상태로 남아 24시간 정리 대상이다. 같은 파일의 동시 생성 요청에서 이중 연결을 막는다.
- 오류 후보: 미인증 401, 자격/게시물 권한 없음 403, 접근 불가 파일 404(타인 참조 포함), 만료·이미 연결·정리 중·상태 경합 409, 실제 합계 초과 413, JPG/PNG 외 415, Storage 일시 장애 503. 필드 오류/중복 입력은 전역 오류 계약을 따른다. 구체 wire code·retryable·오류 body는 BE1 확인 전 고정하지 않는다.

### 10.5 상태·24시간 정리·삭제 보상 제안

2026-10-07 PR #78 병합 후 실제 DB 상태명을 사용해 제안을 정리했다. 이전 설명용 RESERVED/UPLOADED/ATTACHED는 각각 UPLOADING/UNLINKED/LINKED에 대응하며 새 DB Enum을 추가하지 않는다. 아래는 실행 흐름 제안이고 API wire 상태값의 확정은 아니다.

```text
UPLOADING → UNLINKED → LINKED
UNLINKED → DELETE_PENDING → DELETED
LINKED → DELETE_PENDING → DELETED (사진 제거·교체·게시물 삭제)
UPLOADING 취소/실패 정리: 현재 CHECK로 DELETE_PENDING 전환 불가, 계약·저장 방법 확인 대기
LEGACY: 자동 정리 제외
```

| 항목 | 제안 |
| --- | --- |
| 24시간 기준 | #3 저장 구조와 맞춰 서버가 확인한 최초 업로드 완료시각 `uploaded_at+24h`를 실행 계약 후보로 제안. 이전 `createdAt+24h` 제안은 대체한다. 사용자 확정은 24시간 정리 정책이며 기산점의 최종 API/FE 합의는 #74에 남는다. 완료 재호출·재조회·재발급으로 uploaded_at을 갱신하지 않음. UTC 저장, wire 시간은 전역 계약 사용 |
| 미완료 판정 | POST_PHOTO·UNLINKED이며 실제 게시물 참조가 없는 파일. LINKED/LEGACY는 24시간 자동 정리 제외. UPLOADING은 현재 완료 후 24시간 인덱스의 대상이 아니므로 응답 유실·전송 포기 파일을 방치하지 않도록 별도 판정/TTL·저장 방법을 #74에서 먼저 합의 (§10.8) |
| 정리 간격 | 1시간마다 실행을 제안. 24시간 이상 경과한 대상만 처리하며 첫 예정 실행에서 정리; 만료 즉시 밀리초 단위 삭제를 보장하지 않음. 간격·재시도 값은 #30에서 확정 |
| 완료/정리 경합 | #3 실행 계약대로 게시물 연결과 정리 예약은 같은 media_files 행을 잠그고 소유권·상태·실제 참조를 다시 검사. 여러 파일은 ID 오름차순 잠금. 연결 성공 파일은 선점에서 제외하고, 만료/선점 후 연결은 409 후보. Storage 삭제 전 DB 연결 금지 상태를 먼저 확정. 상태만 보고 참조 파일을 삭제하지 않음 |
| 연결된 사진 삭제 | 게시물 DB 연결 제거/비공개 처리와 삭제 작업 기록을 같은 DB 트랜잭션으로 저장. Storage 파일 삭제를 즉시 시도하고 실패하면 durable 작업을 재시도. 최종 물리 삭제 전까지 삭제 성공을 과장하지 않고 pending 의미를 계약에 반영 |
| 삭제 재시도 | object가 이미 없으면 성공. DB rollback 시 기존 게시물 사진을 먼저 삭제하지 않음. DB와 Storage의 단일 원자적 트랜잭션을 가정하지 않음 |
| 지연 업로드·재생성 | 취소/삭제가 signed 권한을 즉시 폐기한다고 가정하지 않음. 미만료 권한으로 object가 늦게 생기거나 삭제 후 다시 생기는 경우를 방어. 경로 재사용 금지, 마지막 발급 권한 만료까지 tombstone/재확인·삭제 재시도 유지, 만료 후 진행 중 업로드까지 끝난 뒤 최종 정리 확인 |
| 공개 URL/cache | 원본 파일 삭제와 이미 다운로드한 복사본·CDN cache 제거는 구분. 새 파일은 새 경로 사용. 삭제 후 공개 URL 재조회 및 cache 동작은 #13/#30 검증에 포함 |

BE1 검토용 데이터 요구사항은 파일 owner·용도·검증된 MIME/bytes·object 경로, 상태, 최초 예약/만료/연결 시각, 마지막 발급 권한 만료시각, 삭제 이유/작업·재시도 정보다. 기존 `media_files`/`post_photos`를 재사용할 수 있는지 확인하고 BE1이 Schema/Migration과 유니크/FK/잠금 전략을 관리한다. 이 검토안은 DDL·새 테이블을 생성하지 않는다.

### 10.6 미완료 파일의 공개 시점

**사용자 확정: 임시 사진도 URL로 공개 열람 허용.** 2026-10-07 채팅 답변으로 미완료 파일의 공개 시점 질문을 해소했다. 공개 버킷에 직접 업로드하는 방향으로 준비하며 비공개 임시 버킷에서 공개 버킷으로 이동하는 절차는 추가하지 않는다.

미연결 사진도 업로드 직후 URL을 알면 열람할 수 있다. 예측 어려운 경로는 접근 권한의 대체가 아니며 이 선택은 게시물 API 탐색·지역 참여·업로드/변경/삭제 권한을 확장하지 않는다. 파일 검증 후 완료 확인 응답에 공개 `url`을 제공하는 DTO를 제안하고, 게시 전 URL이 없다고 접근이 차단되는 것으로 안내하지 않는다. 검증 실패/취소/만료 파일의 저장 파일 삭제와 지연 업로드 재생성 방어는 그대로 필요하다.

### 10.7 다음 확인과 검증 기준

| 담당 | 구현 전에 필요한 확인 |
| --- | --- |
| 사용자 | 미완료 업로드의 공개 열람 허용 확인 완료(2026-10-07 채팅). 해당 선택의 추가 질문 없음 |
| BE1 | 후보 endpoint/DTO·ID wire 타입·오류·MB 환산·파일 상태와 Schema·트랜잭션/삭제 보상 정합성 |
| BE2 | Storage REST/서명 업로드 adapter·큰 사진 업로드 검증·내용 검사 자원 제한·권한 만료/지연 업로드 정리·실행 스케줄 |
| FE | 업로드 descriptor 소비·완료/상태 재조회·취소·최종 참조/정렬·413/415/409 처리의 데이터 계약. 화면 문구·실제 연결 검증은 #30으로 이관 |

#13/#16 검증 시 정상 업로드·선언 MIME/크기 위조·10장/합계 한계·타인/타 게시물/중복 참조·완료 재호출·취소 후 업로드·만료 전후·동시 연결/정리·DB rollback·Storage 장애/삭제 재시도·지연 object 재생성·연결 완료 파일 보호·공개 URL 삭제 동작을 확인한다. FE/BE 실제 사용자 흐름은 #30 및 #31 전에 확인한다.

이번 작업은 문서만 수정했다. Backend 코드·Schema·Storage 프로젝트·FE 브랜치는 변경하지 않았고 #74/#13 완료로 표시하지 않는다. 2026-10-07 공식 문서의 signed URL 만료·업로드 방식·공개 열람·삭제 동작을 대조했다. changelog HTML의 Storage 관련 항목도 확인하며 실제 프로젝트 설정 검증은 #30으로 남긴다.

자료: [signed upload](https://supabase.com/docs/reference/javascript/storage-from-createsigneduploadurl), [standard upload와 TUS 권장](https://supabase.com/docs/guides/storage/uploads/standard-uploads), [공개/비공개 파일 열람](https://supabase.com/docs/guides/storage/serving/downloads), [파일 삭제](https://supabase.com/docs/reference/javascript/storage-from-remove), [changelog](https://supabase.com/changelog).

검증 기록: 2026-10-07 17:55 KST, Codex, 기준 SHA `db50de3`의 `back/feature/74-photo-contract` 미커밋 작업 트리. 기존 Backend `gradlew.bat --no-daemon test build --rerun-tasks --console=plain` 성공(테스트 4개, 실패/오류 0, 작업 6개 재실행). 변경 3개 문서의 상대 링크 및 `git diff --check` 통과. Backend 소스 변경은 없으며 이 테스트는 제안된 사진 API·Storage 동작의 검증이 아니다. 실제 업로드·권한·정리/삭제·FE/BE 연동 검증은 미실행이다.

### 10.8 PR #78 DB 저장 구조 대조 (2026-10-07)

확인자: Codex(BE2 작업 준비). 팀의 승인자와 구분한다. PR #78은 merged=true이며 fetch 후 origin/back/develop은 `452debdfd4971a8a5756c9d3783d5d5ffff1545b`다. 같은 리비전의 실제 Migration과 #3 설계·접근 결정 문서를 대조했다. 로컬 Feature는 `db50de3` 기준을 유지하며 미커밋 변경을 보존했고 develop의 신규 코드는 병합하지 않았다. 이 비교를 BE1/FE 계약 합의·#74 완료·실제 서비스 검증으로 표시하지 않는다.

| 대상 | 실제 #3 저장 구조 | 계약안 정합성·남은 확인 |
| --- | --- | --- |
| 파일 ID·소유권 | media_files.id는 BIGINT, 상한 9007199254740991. owner_user_id·storage_key UNIQUE·purpose·MIME/bytes·created_at 존재 | fileId는 기존 ID 참조. wire 타입은 BE1 정본 확인 필요. POST_PHOTO만 게시물 사진 처리 대상으로 삼고 owner는 검증된 로컬 회원으로 설정 |
| 상태 | LEGACY/UPLOADING/UNLINKED/LINKED/DELETE_PENDING/DELETED | §10.5를 실제 DB 상태명에 맞췄다. CHECK는 행의 상태/시각 형태만 검사하며 권한·참조·전이를 구현하지 않음 |
| 24시간 | uploaded_at 기산, POST_PHOTO/UNLINKED 부분 인덱스 | created_at 기산 제안을 대체하고 uploaded_at+24h를 실행 계약 후보로 제안. 최종 API/FE 합의 대기. 완료 응답 유실·재호출로 시간을 연장하지 않도록 최초 서버 확인/재조사 기준 합의 필요 |
| 미완료 전송 취소 | UPLOADING은 uploaded_at=NULL. DELETE_PENDING/DELETED는 uploaded_at 필수 | 현재 CHECK로 UPLOADING→DELETE_PENDING 불가. 미전송·잘못된 MIME/크기·완료 통지 없는 object의 취소/정리 추적이 부족함. 미검증 파일을 UNLINKED로 승격하거나 완료시각을 꾸미거나 행을 지워 추적을 잃는 우회 금지. BE1이 추가 Migration 또는 안전한 별도 영속 추적 방식을 검토 |
| 발급 권한·지연 업로드 | 마지막 signed 권한 만료·발급 이력·worker claim 전용 정보 없음 | 재발급·만료 전 취소 후 object 재생성을 추적할 저장 방법 합의 필요. DELETED는 Storage 삭제 확인이지만 유효 업로드 권한으로 재생성될 수 있으면 최종 정리 완료라고 볼 수 없음. token/URL 원문은 저장하지 않음 |
| 대상 지역 | media_files에 region_id 없음 | regionId를 받아 다른 지역 파일을 거부하는 제안에는 영속적인 지역 연결 또는 지역 구속 없이 최종 게시 때 권한 재검사하는 계약 변경 필요. 나중의 회원 활동 지역으로 업로드 당시 지역을 추측하지 않음 |
| 게시물 참조·중복 연결 | post_photos는 UNIQUE(post_id,file_id), UNIQUE(post_id,sort_order). file_id 단독 UNIQUE 없음 | FK/UNIQUE만으로 한 파일 한 게시물 보장 불가. 공통 media_files 행 잠금 안에서 전체 post_photos 참조를 검사하고 연결·상태를 같은 트랜잭션에서 변경. 추가 DB 제약 필요 여부는 BE1 판단 |
| 정렬 | sort_order>=0, 게시물 내 UNIQUE는 DEFERRABLE INITIALLY IMMEDIATE | 최종 배열을 0부터 연속 순서로 변환하는 실행안. 교체 중 UNIQUE 충돌을 피할 트랜잭션 내 제약 지연/갱신 순서를 #16에서 검증. 최대 개수·합계 bytes는 실제 값을 서비스에서 검사 |
| 삭제 재시도 | delete_requested_at/deleted_at/deletion_attempts/next_delete_attempt_at/last_delete_error_code 존재 | 별도 삭제 작업 테이블을 필수로 추가하지 않고 기존 행의 영속 예약 재사용을 우선 검토. DELETE_PENDING 저장 후 외부 삭제, 성공/이미 없음을 확인해 DELETED. 실패는 비밀 제거 오류 코드·재시도 시각 기록. 복수 worker 선점/복구·재생성 검사는 미구현 |
| 자격·접근 | Spring 사용자/지역/기관/소유권 판정, DB는 서버 전용 역할의 접근 제한 | Privy를 Supabase Auth로 가정하지 않음. 공개 버킷 읽기·Storage 관리 권한과 private discushion의 GRANT/RLS는 별개. #4 권한표에서 media_files 조회/삽입/갱신, post_photos 조회/삽입/정렬/필요 삭제, ID 시퀀스 권한을 BE1과 검토. 실제 구성·연결은 #30 |

#### 구현 전 확인 목록

- [ ] BE1: uploaded_at+24h의 최종 계약 채택 여부, UPLOADING의 별도 TTL, 완료 통지 유실 시 재조사, 불량 object·미전송 취소의 저장 방법 확인.
- [ ] BE1/BE2: 미검증·미완료 파일 삭제 추적, 마지막 signed 권한 만료/재발급·재생성 방어, 지역 연결을 합의. 이미 적용된 Migration은 수정하지 않으며 필요하면 BE1이 추가 Migration을 준비해 back/develop에 반영.
- [ ] BE1: endpoint/DTO·wire ID·상태/오류·10MB bytes 환산·기존/신규 사진 최종 배열을 정본에 반영. BE2/FE는 구현 전에 해당 데이터 계약 확인.
- [ ] BE2: #13에서 공통 행 잠금·참조 보호·내용/합계 검사·취소/삭제 재시도·권한 재발급/지연 업로드를 검증. #16의 사진 변경·게시물 삭제 트랜잭션과 연결.
- [ ] BE1/BE2: #4 기본 권한표를 #30 서버 역할 생성/GRANT 전에 확정. #30은 서버 역할로 DB 의존 기능을 검증하기 전에 개발 계정을 준비.
- [ ] 실제 FE 호출·공개 URL/Storage/worker/권한 검증은 해당 기능/#30/#31에서 수행. 이번 비교와 기존 Health test를 실제 사진 처리 성공 근거로 사용하지 않음.

대조 후 검증: 2026-10-07 18:14 KST, Codex. 로컬 `db50de3` 기준 Feature의 문서 변경 상태에서 Backend `gradlew.bat --no-daemon test build --rerun-tasks --console=plain` 성공, 6개 작업 재실행. 테스트 XML은 Health 3개·설정 1개, 총 4개 실패/오류/skip 0. 첫 실행은 sandbox의 Gradle 캐시 잠금 파일 접근 거부로 시작되지 않았으며 캐시 접근 허용 후 재실행했다. 이 결과는 현재 Feature의 기존 코드 검증이며 PR #78의 11개 테스트 재실행·사진 기능 검증이 아니다. 상대 링크와 diff 검사 통과. 실제 DB/Storage/FE 호출은 수행하지 않았고 코드·Migration·원격 설정은 변경하지 않았다.

참조: [#3 설계](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/blob/452debdfd4971a8a5756c9d3783d5d5ffff1545b/docs/architecture/Discushion_Issue3_MVP_v10.2_반영.md), [lifecycle Migration](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/blob/452debdfd4971a8a5756c9d3783d5d5ffff1545b/supabase/migrations/20261007023149_add_privy_registration_and_media_lifecycle.sql), [초기 Schema](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/blob/452debdfd4971a8a5756c9d3783d5d5ffff1545b/supabase/migrations/20261006182228_mvp_schema.sql), [서버 접근 결정](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/blob/452debdfd4971a8a5756c9d3783d5d5ffff1545b/docs/collaboration/backend-db-connection-decisions.md).
### 10.9 사용자 채택: 사진 처리 1~4번 실행 방향 (2026-10-07)

근거: 사용자가 이 채팅에서 “1~4는 너가 추천하는대로 처리해줘”라고 요청했다. 아래 실행 방향을 채택한 것으로 기록한다. §10.3~10.8의 같은 주제에 대한 제안/선택 대기 표현보다 이 절을 우선한다. 실제 BE1/FE 검토·API 정본 갱신·DB 보완·구현 완료를 대신하지 않는다. 5번 API 경로/DTO·응답 코드·오류·bytes 환산·인증 전달 계약은 계속 대기한다.

#### 1. 업로드 완료와 중단을 구분해 정리

- 완료 확인 API에서 BE가 실제 object의 존재·이미지 내용·크기를 검증한 최초 시각을 uploaded_at으로 기록한다. 검증 전에는 게시물 연결 불가. 완료 재호출/권한 재발급은 최초 시각을 갱신하지 않는다.
- 검증 완료·미연결 파일은 uploaded_at+24시간, 검증 완료시각이 없는 UPLOADING은 최초 예약 created_at+24시간부터 정리 대상이다. 예약 재발급/재조회로 기한을 연장하지 않는다. 두 기한은 업로드 권한 자체의 만료와 별개다.
- 정리 대상 조사는 1시간마다 실행한다. 기한 이후 다음 조사에서 삭제 예약하고 실패는 재시도한다. LINKED와 LEGACY는 자동 만료에서 제외하고 실제 활성 참조가 있으면 삭제하지 않는다.
- 완료 통지가 유실된 UPLOADING은 무조건 완료 상태로 승격하지 않는다. 소유자가 기한 내 완료 확인을 재요청할 수 있고, 기한 이후에는 검증 여부와 무관하게 미연결 object 정리 대상으로 처리한다. 잘못된 형식/크기·명시적 취소는 24시간을 기다리지 않고 삭제 예약한다.
- 저장 방향: 기존 media_files에 미검증/미전송 파일의 삭제 예약·재시도를 추적한다. uploaded_at=NULL인 삭제 상태를 안전하게 표현할 수 있도록 BE1에 CHECK/저장 구조 보완을 요청한다. 실제 컬럼/제약과 추가 Migration은 BE1 담당이며, 업로드 완료시각을 꾸며 기존 제약을 우회하지 않는다.

#### 2. 취소 후 지연 업로드와 재생성 방어

- 파일별 object key는 서버가 생성하고 재사용·덮어쓰기를 허용하지 않는다. 발급된 권한의 마지막 만료시각을 영속 기록하며, 권한 원문/서명 URL은 저장·로그에 남기지 않는다.
- 재발급은 본인 소유의 기한 내 UPLOADING에만 허용한다. 취소·정리 예약·연결·만료 후에는 재발급하지 않는다. 재발급으로 파일 보관 기한을 늘리지 않는다. 사용자별 발급 제한 값은 #13 구현 전 계약으로 남긴다.
- 취소/정리 예약 후에는 완료 처리·게시물 연결·재발급을 모두 차단한다. 파일 행과 key·권한 만료 추적은 보존하고 외부 삭제를 반복 확인한다.
- 최초 물리 삭제 성공 뒤에도 유효 권한/진행 중 전송에 의한 재생성을 확인한다. 단순 object 부재만으로 최종 정리 성공을 확정하지 않는다. 마지막 권한 만료와 진행 중 전송의 종료/안전한 재확인 기준을 충족한 뒤 DELETED로 확정한다. 정확한 진행 중 전송 판별/대기 조건은 #13 adapter 검증과 BE1 저장 계약에서 정하며, 확인 불가하면 삭제 추적을 유지한다.
- BE1 보완 대상은 마지막 권한 만료와 삭제 처리 선점/복구에 필요한 영속 정보다. worker 처리 중 장애 후에도 재개 가능해야 하며 기존 적용 Migration은 보존한다.

#### 3. 파일은 소유자에게 연결, 지역은 최종 게시 때 검사

- 사진 파일 자체는 업로드 지역에 묶지 않는다. 업로드 후보 요청에서 regionId를 제외하며 파일에 region_id를 추가하는 요구를 철회한다.
- 가입 완료 회원이 본인 파일을 업로드한다. 미연결 파일 취소는 소유자가 할 수 있으며 이후 지역 자격 변경만으로 취소를 막지 않는다.
- 실제 게시물 생성/수정/삭제 때 대상 게시물의 지역 자격·소유권·투표 상태 등 명세의 권한을 다시 검사한다. 다른 지역의 자격을 대신 부여하거나 타인 파일을 사용할 수 있다는 뜻이 아니다.

#### 4. 연결과 삭제 성공 의미

- 파일 연결·post_photos 참조·LINKED 상태 갱신은 같은 DB 트랜잭션에서 처리한다. 같은 media_files 행을 잠그고 실제 참조·소유권·검증/만료 상태를 다시 확인한다. 여러 파일은 ID 오름차순 잠금으로 처리한다.
- 신규 파일은 한 게시물에 한 번만 연결한다. 같은 게시물의 기존 사진 유지/재정렬은 새 연결로 취급하지 않는다. 정리 예약과 연결은 공통 잠금으로 직렬화해 이중 연결·연결 중 삭제를 막는다.
- 제거/교체/게시물 삭제에서는 명세상 참조 제거·비노출과 DELETE_PENDING 기록을 같은 DB 트랜잭션에 저장한다. 커밋 이후 Storage 삭제를 즉시 시도하며 실패하면 영속 기록을 통해 재시도한다. DB rollback 시 기존 object를 먼저 삭제하지 않는다.
- 사용자 요청의 성공은 사진 관계 제거/취소와 삭제 예약의 확정을 뜻한다. 실제 저장 파일 삭제 완료와 구분해 FE에 전달할 수 있어야 한다. 삭제 대기는 상태 조회 대상이며 DELETED는 위 재생성 방어까지 충족한 최종 확인 상태로 사용한다. HTTP 202/204 등 코드와 상태 필드명은 5번 API 계약에서 BE1/FE가 정한다.
- 저장 파일 삭제는 계속 완료 조건이다. 삭제 대기 응답만으로 #13/#16 인수를 완료하지 않으며, 실패·재시도·늦은 전송·경합을 실제 Storage에서 검증한다.

다음 검토: BE1은 미검증 파일의 삭제 상태 제약·발급 만료/선점 추적을 추가 Migration으로 검토하고 API 정본을 정리한다. BE2는 #13/#16 실행을 준비하되 DB 보완과 필수 데이터 계약이 back/develop에 반영되기 전 의존 코드를 구현하지 않는다. FE는 요청/응답·삭제 대기 의미를 구현 전에 확인하고 실제 사용자 흐름은 #30/#31에서 검증한다.

### 10.10 BE1 DB·API 대조와 보완안 (2026-10-07)

검토 입력은 BE2 `4f7bb25`의 §10.8~10.9, 구현 기준은 병합된 back/develop `452debd`다. 확인자 Codex(BE1 작업 준비), 사용자 요청 근거는 BE2 검토 요청 전달 및 진행 지시다. BE2/FE의 최종 확인을 대신하지 않는다.

- CLI 생성 추가 Migration `20261007104543_support_photo_cleanup_leases.sql`로 미검증 파일도 uploaded_at=NULL을 유지한 채 DELETE_PENDING/DELETED로 추적한다. UNLINKED/LINKED의 검증 완료 요구는 유지하며 삭제 예약시각은 created_at 및 존재하는 uploaded_at/linked_at 이후다.
- media_files에 upload_authorization_expires_at, deletion_claim_token(UUID), deletion_claimed_at, deletion_claim_expires_at을 추가한다. 서명 URL/토큰 원문은 저장하지 않는다. 신규 발급의 만료 최댓값을 응답 전에 보존하며 최초 created_at/uploaded_at을 덮어쓰지 않는다. 기존 행에 임의 만료/선점 값을 backfill하지 않는다.
- 선점 세 필드는 함께 NULL 또는 DELETE_PENDING에서 함께 존재해야 하고 선점시각은 예약 이후, 만료는 선점 이후다. 실제 worker는 FOR UPDATE SKIP LOCKED로 원자 선점하고 결과 갱신에 현재 token을 조건으로 사용한다. 만료 claim은 새 token으로 회수한다. lease/backoff/정리 간격은 #13/#30 설정이며 임의 수치를 Migration에 넣지 않는다.
- DELETED는 마지막 발급 권한이 만료되기 전 기록할 수 없게 제한한다. 단, 이 CHECK만으로 진행 중 업로드 종료·재생성 방어를 증명할 수 없다. #13/#30은 signed 권한 만료 후 늦은 전송까지 정리됐음을 실제 Storage에서 확인해야 한다. 그 전에는 DELETE_PENDING을 유지한다.
- UPLOADING created_at 정리 후보와 DELETE_PENDING claim 만료용 부분 인덱스를 추가한다. 검증된 미연결 파일은 uploaded_at+24h, 완료시각 없는 파일은 created_at+24h이며 LEGACY/LINKED는 후보가 아니다. 공통 파일 잠금·실제 참조 재검사·연결/예약 원자성은 서비스 책임이다.
- post_photos에 UNIQUE(file_id)를 추가해 서로 다른 게시물의 중복 연결도 차단한다. 기존 중복이 있으면 적용 전 중단하며 데이터 자동 삭제·보정은 하지 않는다. 본인 파일·purpose·검증 상태·참조·만료는 서비스에서도 검사한다. region_id는 추가하지 않는다.
- 현재 mime_type/size_bytes는 UPLOADING 때 신고값, 최초 서버 검증 성공 후 실제 값이다. uploaded_at 없는 값은 게시물 합계/연결 판단에 사용하지 않는다. 별도 신고값 감사 이력은 이번 범위에 추가하지 않는다.
- API 정본 §13에 준비/완료/상태/취소 경로·DTO·숫자 ID·사진 최종 배열·오류·삭제 예약/최종 완료를 구체화했다. **BE1 검토안이며 FE/BE2 wire 확인 대기**다. 10MB=10,000,000 bytes도 제안 상태를 유지하고 구현에서 확정값으로 쓰지 않는다. §5.4~5.5의 multipart/newImageIndex는 변경 전 예시다.

검증 및 확인 기록은 이 절 아래에 실행 후 추가한다. 새 Migration은 localhost 시험에만 적용하며 지정 원격 DB에는 적용하지 않는다. 기본 권한표·서버 역할 구성은 #4/#30 후속 조건을 유지한다. 실제 worker·Storage·FE 호출은 #13/#16/#30/#31에서 검증한다.

#### BE1 실행 결과·확인 상태

2026-10-07 19:49~19:50 KST, Codex(BE1 검토 준비). 기준 `452debd` + 이번 미커밋 변경, 입력 문서 `4f7bb25`. 사용자 채택된 1~4 실행 방향의 DB 보완을 구현했으나 BE2/FE wire 승인·원격 적용·#74 전체 완료는 아니다.

| 검사/확인 | 실제 결과 |
| --- | --- |
| localhost CLI Migration 적용 및 재실행 | 신규 1개 적용 후 재실행 대상 없음. Schema/이력/표식 데이터 불변 |
| DB 무결성 | 128개 통과(기존 113 + 신규 15). 미완료 삭제·권한 만료 이전 최종 완료 거부·claim 형태/시각·다른 게시물 중복 연결 등을 확인 |
| 카탈로그/ERD | 27테이블·180컬럼·단일 FK47/복합FK8·Privy UNIQUE·post_photos.file_id UNIQUE 대조 오류 0 |
| API 역할 이름 모의 | 기존 격리 DB에서 9개 접근 차단 통과. 실제 서버 최소 권한 시험은 아님 |
| Java 17 test/build | 실제 작업 6개 재실행 성공. Health3/local JDBC4/설정1/원격 JDBC3 = 11, 실패/오류/skip0, JAR 생성 |
| 원격 Schema | 기존 27테이블·176컬럼·이력4개의 SELECT-only 회귀 확인. 새 Migration은 미적용이며 180컬럼 원격 검증이라고 기록하지 않음 |
| worker·경합·Storage·FE | 미실행. claim token CAS/장애 회수/진행 중 업로드 종료·재생성 방어 및 실제 공개 URL/삭제는 #13/#16/#30에서 검증 |
| wire 계약 최종 확인 | API 정본 §13의 endpoint/DTO/숫자 ID/10,000,000 bytes/순서/202·200·204/오류를 FE·BE2가 확인한 기록 없음. 확인자·날짜·PR/SHA·이견을 #74에 기록한 후 고정 |

현재 원격 smoke 검사는 #3의 4개 Migration 상태를 고정 확인한다. 새 Migration 원격 적용 시에는 해당 smoke의 기대 컬럼/이력도 이 rollout에 맞춰 갱신·검증해야 하며 기존 검사를 새 Schema 검증으로 대체하지 않는다. 이미 적용된 Migration 파일은 변경하지 않았다. Supabase changelog Markdown은 웹 도구 content-type 오류로 읽지 못했으며 확인 완료로 기록하지 않는다. CLI는 캐시된 2.120.0 help를 확인해 새 파일을 생성했다.

기술 근거: [signed upload 권한](https://supabase.com/docs/reference/javascript/file-buckets-createsigneduploadurl), [standard upload와 대용량 전송 권장](https://supabase.com/docs/guides/storage/uploads/standard-uploads), [PostgreSQL 행 잠금/SKIP LOCKED](https://www.postgresql.org/docs/17/sql-select.html). provider 권한은 2시간 유효하며 전송 중 종료·늦은 재생성 안전성을 이 수치만으로 보장하지 않는다. 일반 업로드의 6MB 초과 TUS 권장은 제품 10MB 한도 변경이 아니다.

### 10.11 BE2 PR #85 검토와 최신 기준 동기화 (2026-10-07)

사용자 진행 지시로 Codex(BE2 검토)가 PR #85의 0e75e94와 최신 back/develop 95dddd8을 대조했다. 동일 위치의 추가 문단 충돌은 사진 검토 §10.10/API §13과 공통 내부 규약 §11을 모두 보존해 해결했다. 기존 적용 Migration 4개·공통 Java port·FE 파일·외부 프로젝트 설정은 수정하지 않았다.

DB 검토: uploaded_at 없는 삭제 상태 추적, 발급 만료 최댓값 저장, 삭제 claim의 완전한 형태/시각 제약, 권한 만료 이전 최종 완료 거부, post_photos UNIQUE(file_id), 후보/claim 부분 인덱스는 채택된 실행 방향과 일치한다. 기존 중복 참조는 자동 보정하지 않고 preflight에서 중단한다. 이 구조만으로 실제 Storage 삭제·늦은 업로드·worker fencing/경합이 완료된 것은 아니며 #13/#16/#30에서 검증한다.

남은 구현 선행 계약:

- API §13은 계속 검토안이다. 최신 front/develop의 API 문서는 직접 업로드 합의 대기를 유지하며 FE가 숫자 ID·bytes·JSON 최종 배열·202/200/204 의미를 확인한 근거가 없다. BE2 코드 검토를 FE 승인으로 대신하지 않는다.
- UPLOADING 권한 재발급의 소유권/기한/최대 만료 보존 규칙은 있으나 재발급 endpoint·요청/응답 또는 재발급 없음 중 결정이 필요하다. 새 예약 POST를 기존 예약 재발급으로 임의 해석하지 않는다.
- 미완료 예약 수/용량·권한 발급 빈도 제한은 §13에서 #13 전 합의 대상으로 남아 있다. 시연 규모라도 무제한을 확정한 것으로 해석하지 않는다.
- 발급 요청 전에 잠재 권한의 만료 상한을 기록하고 외부 요청 중 장애/응답 유실에도 추적하는 단계, 실제 signed/TUS method·headers와 진행 중 전송 종료/안전 재확인 조건을 #13 adapter 계약에서 구체화한다. 외부 호출을 DB 잠금 transaction 안에 넣지 않는다. 확인 불가 파일은 DELETE_PENDING을 유지한다.
- 실제 서버 역할/기본 권한표는 #4/#30에서 준비하고 원격 Migration 적용/새 Schema smoke 검증은 별도 rollout이다. 이번 검토는 원격 DB 변경을 허용하거나 수행한 것이 아니다.

최신 기준의 Backend test/build 및 격리 PostgreSQL CI 결과는 PR #85와 #74에 실행 후 기록한다. 기존 시험 결과는 그대로 보존하며 새 CI로 검증한 범위와 실제 FE/Storage 미실행을 구분한다. 필수 리뷰·wire/필수 계약 확인 전에는 PR 병합이나 #13 의존 구현 완료로 기록하지 않는다.

### 10.12 사진 잔여 계약 구체화 — BE2 실행 기준 (2026-10-07)

사용자가 PR #85 병합 후 다음 작업인 사진 잔여 계약 마무리를 요청했다. API 정본 §13.5~13.7에 다음을 구체화했다. 같은 주제의 재발급 허용·예약 제한 수치 미정 표현은 이 기준으로 대체하며 제품 사진 한도/삭제/24시간 정리와 공통 규약은 유지한다.

- 같은 예약의 권한 재발급 endpoint는 만들지 않는다. 유효한 최초 URL의 전송 실패는 같은 key로 재시도하되 덮어쓰기는 금지한다. 성공 여부가 불분명하면 complete로 확인하고, fileId를 아는 권한 만료/전송 정보 유실은 이전 예약 취소 후 새 fileId/key로 예약한다. 최초 응답 전체 유실로 fileId를 모르면 §10.13의 별도 서버 추적/정리·명시적 재시도 흐름을 따른다.
- 초기 서버 예약 보호 기준: 회원당 UPLOADING/UNLINKED/DELETE_PENDING 20개·신고 합계100,000,000 bytes, 최근60초 새 예약20개. 삭제 예약은 실제 최종 정리 전 슬롯 반환으로 취급하지 않는다. users 잠금과 같은 DB transaction에서 제한 조회/예약을 원자 처리한다. 실 Storage 사용량 상한으로 주장하지 않는다.
- 게시물 10MB는 BE2 실행 기준 10,000,000 bytes로 정리한다. 영역 오류안409 PHOTO_UPLOAD_QUOTA_EXCEEDED/429 PHOTO_UPLOAD_RATE_LIMITED·Retry-After와 FE 재시도/삭제 상태 확인을 명시했다. 공통 오류 envelope나 인증 코드 registry는 변경하지 않았다.
- provider 호출 전 잠재 권한 만료 상한을 기록하고 commit 후 외부 호출한다. provider TTL/시각/timeout·응답 유실·프로세스 장애·상한 확장 저장·취소 재검사와 최종 삭제 시 진행 중 전송 종료/재생성 방어는 #13 실제 adapter 검증 조건이다. 확인 불가하면 DELETE_PENDING 유지이며 임의 시간이 안전성 보장이 아니다.
- FE가 확인할 endpoint/숫자ID/bytes/최종배열/Storage 직접 전송·오류/재시도·202/200/204 표를 API §13.7과 통합 지침서에 준비했다. 최신 FE 문서 대조는 합의 대기를 확인한 것이며 FE 승인·외부 연락·실제 연동 완료가 아니다.

이번 변경은 기존 문서 내용만 갱신한다. Java 기능·추가 Migration·FE 브랜치·Storage 프로젝트·비밀 변수는 변경하지 않는다. FE wire 확인과 계약 PR 통합 전 #13 의존 endpoint를 임의 구현하지 않는다. #74의 인증 잔여 계약/실제 환경 검증은 별도 유지한다.

### 10.13 PR #107 Codex 리뷰 보완안 (2026-10-07)

2026-10-08 FE 계약 확인 기록: FE 담당 sungjin0616은 [PR #107 코멘트](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/pull/107#issuecomment-6041114660)에서 상세 사진 DTO·PUT/RAW 전송·응답 유실 재시도·meta 보존을 구현 가능한 계약으로 확인했다. 사용자는 같은 날 FE에게 계약 자체에 문제가 없고 구현 가능하다는 확인을 전달받았다고 명시했다. 아래 과거 FE 확인 대기 표시는 이 기록으로 갱신한다. FE 코드 구현·실제 연동 완료나 GitHub Approve를 뜻하지 않는다. 최신 사진 계약에 대한 BE1 승인 1명과 back/develop 통합은 아직 필요하며, 실제 Storage/서버 계정 검증과 사용자 흐름은 #13/#30/#31에서 수행한다.

사용자가 리뷰에서 발견한 세 누락의 보완안 작성을 요청했다. API §13.8이 이번 세부 계약의 정본이며 이전의 본문 형식 미정·fileId를 모르는 응답 유실도 취소 가능하다는 설명을 보완한다.

- 상세 data.images 항목은 photoId/url/contentType/sizeBytes이며 첨부 순서 배열. fileId는 검증된 게시물 작성자의 상세/생성/수정 응답에만 포함하고 다른 회원/공유 게스트에는 생략한다. 삭제 전 ID 보존·PATCH meta.photoDeletion용 domain decoder를 명시한다.
- Storage는 PUT·bodyMode=RAW, File/Blob bytes 직접 전송. 반환 Content-Type/x-upsert=false와 signed URL을 사용하며 앱 ApiClient/prepareRequest와 분리하고 credentials=omit·redirect=error. SDK File→FormData 자동 변환을 전용하지 않는다. 실제 adapter와 CORS 검증 후 endpoint를 제공한다.
- POST 예약 응답 전체 유실로 fileId를 모르면 즉시 GET/DELETE 불가. 폼/파일 선택을 유지하고 명시적 재시도로 새 예약을 만들며 이전 예약은 서버가 영속 추적·24시간 후보 정리한다. 확인된 발급 실패는 즉시 DELETE_PENDING. 예약 POST는 비멱등이고 미식별 예약도 quota에 포함한다. 409/429에서 무한 재시도하지 않는다. 새 복구 endpoint/Idempotency-Key·DDL은 추가하지 않는다.
- 원 검토는 PR #107 d6d481c, FE front/develop 67783ab, BE back/develop c768080의 코드·문서·Migration을 대조했다. 검토 결과: https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/pull/107#pullrequestreview-5443833790

이번 변경은 계약 보완안이며 FE 실 담당자 확인/BE1 승인/실 Storage·DB·worker 구현·FE 연동 완료를 뜻하지 않는다. 사용자 요청에 따라 Codex가 작성했으며 필요한 상대 승인 1명과 FE wire 확인, back/develop 통합 조건을 유지한다. #13은 raw 전송과 최초 예약 응답 유실/실패·quota·정리 경합을, #14~16은 사진 DTO·연결·수정/삭제 흐름을 실제 검증한다.

## 11. 독립 개발용 내부 인터페이스 상세안 (2026-10-07)

사용자가 “너가 알아서 생각해서 해줘”라고 요청해 합의된 공통 기반을 아래 메서드/반환 타입/실행 책임으로 구체화했다. 내부 규약의 준비 결과이며 BE1의 실제 리뷰나 코드 구현·back/develop 반영·연동 완료를 뜻하지 않는다. 새로운 HTTP endpoint·테이블·인증 provider를 추가하지 않는다. #74에서 공통 규약을 검토·반영하고 BE1 #4/#11/#12, BE2 #14/#15에서 실제 adapter를 구현한다. 기존 사용자 결정은 유지하며 코드 의존 작업은 필수 계약/Schema 통합 후 시작한다.

### 11.1 코드 배치와 공통 타입

단일 Spring Boot/JDBC 구조를 유지한다. 최소 공통 port/불변 반환 타입은 `com.discushion.contracts.identity`, `com.discushion.contracts.post` 패키지에 둔다. 실제 인증/회원 adapter는 BE1, 게시물 adapter는 BE2 영역에 두고 소비자는 상대 Controller/Entity/Repository 대신 port만 사용한다. 공통 타입 변경은 양쪽 공동 검토한다. 아래 Java 선언에 대응하는 source file과 테스트 전용 ContractFixtures를 준비했다. 실제 adapter/Spring bean은 없으며 PR 리뷰·back/develop 통합과 실제 연동 완료를 뜻하지 않는다.

- 내부 ID는 기존 BIGINT에 대응하는 Java long, 유효 범위 1~9007199254740991. 이 결정은 HTTP JSON ID 계약 변경을 뜻하지 않는다.
- 시간은 Java Instant, UTC 기준으로 비교. 기관 유효 종료시각/투표 종료시각과 같으면 만료/종료다. 서버 Clock을 사용하며 클라이언트 시각을 받지 않는다.
- 컬렉션은 null 대신 빈 불변 Set/List, 단일 선택 값은 Optional. DB 실패·시간초과는 빈 데이터/권한 없음으로 바꾸지 않고 기술 예외로 전파한다.
- provider token·이메일·배지·증빙·사진 URL은 이 최소 반환값에 포함하지 않는다. 화면 표시용 프로필/집계는 필요한 기능에서 별도 내부 규약으로 추가한다.

### 11.2 현재 인증 주체 — BE1 제공

```java
interface CurrentActorProvider {
    Optional<VerifiedActor> current();
}
record VerifiedActor(String privySubject, Optional<LocalMember> member) {}
record LocalMember(long userId, Optional<Instant> registrationCompletedAt) {}
```

| 경우 | 반환/처리 |
| --- | --- |
| 토큰 없음 | Optional.empty(). 해당 endpoint가 비회원 접근을 허용하는지는 호출 기능이 판정 |
| 토큰 있음·검증 실패/만료 | 인증 실패. 비회원으로 자동 강등하지 않음. provider 확인 불가 장애는 기술 실패로 구분 |
| 검증 성공·로컬 회원 없음 | VerifiedActor 존재, member 비어 있음. 최초 가입 흐름만 허용하며 일반 회원 권한을 만들지 않음 |
| 로컬 회원 존재·가입 미완료 | member 존재, registrationCompletedAt 비어 있음. 기능이 가입 완료 필요 여부를 판정 |
| 가입 완료 | member와 registrationCompletedAt 존재. 지역/기관 자격을 자동 부여하지 않음 |

인증 계층이 검증한 요청 컨텍스트에서만 주체를 만든다. 인자나 Request userId로 현재 회원을 바꾸지 않는다. 요청 종료 후 컨텍스트를 비우고 임의 비동기 스레드에서 그대로 사용하지 않는다. BE2는 회원 기능에서 주체/가입 완료를 검사하되 JWT를 다시 검증하지 않는다. LocalMember는 최초 인증 결과의 조회 스냅샷이며 중요한 쓰기에서 최신 회원/자격을 재조회한다.

### 11.3 회원 자격 — BE1 제공

```java
interface MemberQualificationReader {
    Optional<MemberQualification> find(long userId);
}
record MemberQualification(
    long userId,
    Optional<Instant> registrationCompletedAt,
    Set<Long> verifiedRegionIds,
    List<InstitutionGrant> institutionGrants,
    Instant evaluatedAt
) {}
record InstitutionGrant(
    long credentialId, long institutionId, long responsibleRegionId,
    Instant completedAt, Instant validUntil
) {}
```

- userId는 내부 인증 주체의 로컬 회원 또는 서버가 조회하는 대상 회원에서 얻는다. 외부 요청 userId/역할을 권한 근거로 전달하지 않는다. 회원이 없으면 Optional.empty(), 자격이 없으면 빈 컬렉션이다.
- verifiedRegionIds는 neighbor_verified_regions 원본만 사용한다. 기본 활동 지역·프로필 속성·기관 자격은 포함하지 않는다. 신청/접수 테이블은 자격 근거로 조회하지 않는다.
- 기관 자격은 institution_credentials의 저장 사실을 반환한다. 현재 유효성은 `completedAt <= 평가시각 < validUntil`로 판단하고 담당 지역은 responsibleRegionId로 확인한다. evaluatedAt은 조회 시 서버 Clock 값이며 소비자는 시간 경과 뒤 이 결과를 장기 캐시해 권한으로 사용하지 않는다.
- 기관 배지/전체 기관 안건 조회와 지역 안건 채택을 구분한다. 기관 자격은 주민 참여의 이웃 완료 지역을 대신하지 않는다. 최종 행동 권한·작성자 소유권·유형/상태 검사는 기능 담당자 책임이다.
- 초기 규약은 읽기 스냅샷이다. 동시 자격 변경이 가능한 쓰기는 아래 §11.5의 공통 회원 잠금과 재조회를 사용한다. 자격 adapter에 임의 allowAll 또는 테스트 계정을 운영 기본값으로 넣지 않는다.

### 11.4 게시물 기본 조회 — BE2 제공

```java
interface PostContextReader {
    Optional<PostContext> find(long postId);
    Optional<PostContext> findForUpdate(long postId);
}
record PostContext(
    long postId, PostType type, long regionId, long authorUserId,
    PostStatus status, Optional<PollContext> poll
) {}
record PollContext(long pollId, Instant endsAt, List<Long> optionIds) {}
enum PostType { LOCAL_AGENDA, LOCAL_ACTIVITY, VOTE }
enum PostStatus { PUBLISHED, DELETED }
```

- 값은 현재 Migration의 posts/polls/poll_options와 대조했다. 일반 조회는 스냅샷이고 쓰기 권한의 최종 근거로 재사용하지 않는다.
- 존재하지 않으면 Optional.empty(). 삭제된 행은 DELETED로 반환해 내부에서 구분하되 본문·선택지 등 공개 비노출은 호출 기능이 적용한다. 공개 endpoint의 없는/삭제 응답은 기존 API 계약을 따른다.
- VOTE만 poll 존재, LOCAL_AGENDA/LOCAL_ACTIVITY는 비어 있음. VOTE인데 poll이 없으면 무결성/기술 실패이며 일반 게시물로 간주하지 않는다. 삭제된 투표 정보는 서버 내부 검사에만 사용하고 공개 응답으로 직렬화하지 않는다.
- optionIds는 실제 해당 poll의 sort_order 순서, 종료 판단은 저장 단계의 서버 Clock으로 `now >= endsAt`. 선택지가 해당 투표에 속하는지 BE1 참여 서비스가 검사한다.
- findForUpdate는 호출자가 이미 시작한 같은 JDBC 트랜잭션에서 posts 행을 잠그고 VOTE면 polls 행도 잠근 뒤 반환한다. 트랜잭션이 없으면 실행을 거부한다. 자기 트랜잭션을 새로 열거나 반환 직후 커밋하지 않는다. 이 read port가 작성자/지역 참여 권한을 대신 승인하지 않는다.

### 11.5 공통 쓰기 트랜잭션과 재검사

외부 호출이나 HTTP를 내부 port 경계에 넣지 않는다. Storage/AI 호출은 이미 합의한 별도 실행 방식에 따른다. 내부 adapter는 Spring의 동일 DataSource/트랜잭션을 사용하고 REQUIRES_NEW로 잠금을 분리하지 않는다.

1. 권한 있는 회원 쓰기에서는 같은 transaction 안에서 대상 users 행을 먼저 잠그고 가입 상태·자격을 재조회한다. BE1이 `MemberWriteGuard.lockAndRead(long userId)`라는 내부 규약을 제공하며 transaction이 없으면 거부하고 반환은 위 MemberQualification과 동일한 Optional이다. 이는 별도 네 번째 제품 기능이 아니라 자격 port의 쓰기 보호용 보완이다.
2. 해당 회원의 자격/가입 상태를 바꾸는 BE1 코드도 같은 users 행을 먼저 잠근다. 여러 회원 행이 필요하면 userId 오름차순으로 잠근다. 자격 데이터만 단독 갱신하는 우회를 허용하지 않는다.
3. 이후 관련 posts → polls 순으로 findForUpdate를 사용하고 공개/삭제·지역·소유권·선택지·종료를 재검사한 뒤 참여/관계를 저장한다. 여러 게시물은 postId 오름차순. 사진 처리의 media_files 잠금은 필요한 posts/polls 이후 fileId 순으로 취득한다. 실제 신규 생성 등 존재하지 않는 게시물 행에는 존재하는 관련 행만 적용한다.
4. BE2 게시물 삭제/투표 종료시각 변경/수정 코드도 같은 posts → polls 잠금 규칙을 따른다. 공유 게스트 댓글처럼 회원이 없는 흐름은 합의된 공유 컨텍스트 검사 후 posts 잠금부터 시작한다.
5. 투표 저장 직전 서버 Clock을 다시 읽어 종료시각을 검사한다. 명시적 종료 상태 컬럼을 새로 만들지 않는다. #25는 제출·선택 변경을 실제 원본에 기록하고 종료 후 쓰기를 거부한다.
6. 실제 코드가 모두 이 규칙을 지키는지 #4/#14/#16/#22~25에서 통합 검증한다. FK만으로 제품 권한·시간 경합을 보장한다고 주장하지 않는다. 기관 만료 등 시각 조건도 최종 저장 단계에서 재평가하며 장시간 transaction을 피한다.

회원 guard의 코드/DB 잠금 구현은 BE1, posts/polls guard 구현은 BE2다. 메서드 명세만으로 공통 잠금이 구현된 것이 아니며 공통 트랜잭션/권한표·RLS는 양쪽 검토 후 반영한다. 잠금 순서 변경이나 기존 경로의 누락을 발견하면 공동 계약을 먼저 갱신한다.

### 11.6 내부 오류와 외부 매핑

| 내부 결과 | 호출 기능의 처리 책임 |
| --- | --- |
| current 비어 있음 | 비회원 허용 또는 인증 필요 처리; 공유 컨텍스트 검사 생략 금지 |
| 토큰 검증 실패 | 인증 실패로 처리. 토큰/원시 provider 오류를 로그·응답에 노출하지 않음 |
| actor.member 비어 있음 / 가입 완료시각 없음 | 가입 흐름과 일반 기능 구분. 새 HTTP 오류 코드를 임의로 만들지 않고 #74/#4에서 정본 매핑 |
| 회원/게시물 조회 Optional.empty | 대상 없음으로 처리. 내부 빈 반환을 권한 허용으로 해석하지 않음 |
| DELETED / 투표 종료 / 대상 선택지 아님 | 기존 POST_DELETED / VOTE_ENDED / VOTE_OPTION_INVALID 등의 정본 의미에 매핑. 정확한 status/body는 기존 HTTP 계약 검토 유지 |
| 자격/소유권 부족 | 기능별 권한 거부. 다른 영역 정보를 불필요하게 노출하지 않음 |
| DB 실패·무결성 실패·transaction 없음 | 기술 실패. 비회원/빈 집계/자격 없음으로 숨기지 않음 |

### 11.7 구현 순서와 검증 기준

- BE1: CurrentActorProvider·MemberQualificationReader·MemberWriteGuard 실제 인증/회원 adapter와 요청 컨텍스트 처리. BE2: PostContextReader 실제 JDBC adapter. 공통 port/record는 계약 반영 PR에서 코드로 준비하고 양쪽 리뷰 후 back/develop에 통합한다.
- 테스트 대체 구현은 src/test 범위에서만 제공하며 임의 기본 Spring bean/운영 allowAll은 만들지 않는다. 없는 대상·가입 미완료·타지역·유효/만료 기관·공개/삭제 게시물·투표 종류/선택지/종료를 재현한다.
- 실제 JDBC 검증은 같은 transaction 잠금 유지, 삭제와 댓글/반응 동시 실행, 투표 종료시각 변경과 제출, 자격 변경과 게시, 동일 파일 연결/정리 잠금 순서 등을 포함한다. 필수 서버 역할/GRANT/RLS 준비도 선행한다.
- 이 문서 상세안만으로 상대 코드/DB 구현이 준비된 것은 아니다. port·필수 Schema·테스트 기반의 통합 후 개별 개발하고 실제 adapter 연결 전에는 연동 대기로 기록한다. FE 연결은 이 내부 검증과 구분해 #30/#31에서 수행한다.

### 11.8 테스트 기반·담당 규칙·CI 준비

사용자 요청으로 §11의 공통 port/record, 테스트 전용 ContractFixtures/회귀 검사와 Backend CI 설정을 준비했다. 테스트 데이터는 합성값·고정 Clock을 사용하고 instance별 읽기 대체 구현을 주입한다. 잠금 조회 대체 구현은 지원하지 않으며 실제 트랜잭션 검증으로 오인하지 않는다. 실제 authentication/qualification/JDBC adapter와 HTTP 오류 매핑은 각 담당자의 후속 작업이다.

BE1은 identity adapter와 A·C·D, BE2는 post adapter와 B·환경/CI를 담당한다. 공통 contracts/support/오류/트랜잭션 변경은 양쪽 리뷰 대상이다. 파일 담당 및 검증 절차는 AGENTS.md와 Backend README를 따른다. 사람/팀 GitHub 핸들이 미확정이므로 CODEOWNERS와 강제 리뷰는 자동 설정하지 않았다.

CI는 격리 PostgreSQL Migration·Schema 시험과 Java 테스트/build를 구성한다. 실제 Supabase/Privy/Storage·FE 연결과 DB 잠금 경합은 별도 검증이며 자동 배포는 #30 대상 확정 후 연결한다. 원격 CI 실행 전에는 CI 통과를 기록하지 않는다.

### 11.9 참여 집계·게시물 요약·삭제 접점 확정 (2026-10-07)

사용자가 남은 BE1·BE2 공동 준비를 본인이 수행하도록 위임했다. 아래 내부 규약을 작업 기준으로 채택한다. 특정 팀원의 실제 리뷰·FE wire 승인·운영 adapter 구현으로 기록하지 않는다. 새로운 HTTP endpoint나 테이블은 추가하지 않는다.

#### 참여 집계 — BE1 제공

`ParticipationSnapshotReader.findAll(Set<Long> postIds, OptionalLong verifiedViewerUserId)`는 `Map<Long, ParticipationSnapshot>`을 반환한다. 호출자가 먼저 공개/회원/특정 공유 컨텍스트의 접근을 검증한다. 본인 ID는 검증된 현재 주체에서만 받으며 임의 Request userId를 사용하지 않는다. guest에는 OptionalLong.empty를 전달하고 viewer 필드를 비운다. 회원은 본인 reactions/bookmarked/selectedOptionId만 조회하고 타인의 선택·회원별 행동을 반환하지 않는다.

- snapshot은 부모 댓글 수·답글 수, EMPATHY/NEEDED/CURIOUS 각 수, 선택적 poll 결과, 선택적 본인 상태, 서버 평가시각을 담는다. 댓글 합계는 부모+답글이며 답글 좋아요를 부모 정렬에 합산하지 않는다. 댓글 평가 수/정렬은 댓글 기능 조회에서 처리한다.
- 투표는 pollId·participantCount·정렬된 optionId/count 목록을 반환한다. 참가자 수는 실제 현재 표의 수이며 반응과 합산하지 않는다. 본인 선택은 저장된 표로만 판단한다. 득표율의 표시 반올림은 기존 HTTP 계약에서 정하고 내부 port는 원본 정수 count만 제공한다.
- 이미 접근 확인된 공개 게시물에 참여가 0건이면 0/빈 상태 snapshot을 반환한다. 없는/삭제 게시물은 map에서 제외한다. VOTE인데 원본 poll이 없거나 조회 오류면 기술 실패로 처리하며 0으로 숨기지 않는다. 조회 사이 삭제 경합은 같은 읽기 트랜잭션의 게시물 상태 재확인으로 막는다.
- batch는 요청 ID만 반환하고 빈 요청은 빈 map. 입력 Set은 중복을 제거하며 순서는 보장하지 않는다. 리스트 소비자는 자신의 기존 페이지 순서로 조합한다. 페이지 ID 집합을 한 번에 조회하고 SQL을 게시물별 반복 실행하지 않는다. 결과/본인 상태를 사용자 간 캐시로 공유하지 않는다.

#### 게시물 요약 — 게시물 구현 제공

`PostSummaryReader.findAll(Set<Long> postIds)`는 `Map<Long, PostSummary>`을 반환한다. 최소 값은 ID·유형·지역·작성자·공개/삭제 상태·생성시각이다. 공개 행의 display에는 title/topic만 담고 삭제 행의 display는 비운다. 삭제 요약에 본문·사진·질문·선택지·득표 결과를 넣지 않는다. 없는 ID는 map에 없고 DB 실패는 예외다. 이 내부 조회 자체가 공개 권한을 부여하지 않는다.

기관 안건/일반 탐색/내가 만든·참여한 게시물 목록은 공개 행만 사용한다. 참여한 투표의 기존 삭제 이력은 UNAVAILABLE 표시용 최소 메타데이터만 사용하며 제목/본문/선택/결과를 공개 DTO로 다시 옮기지 않는다. 종료 상태는 poll.endsAt과 서버 Clock으로 평가하고 필요한 투표 메타데이터는 기존 PostContextReader 규약을 함께 사용한다. 사진·기관 배지 등 화면별 추가 정보는 소유 영역에서 별도 조합한다.

#### 삭제·투표 변경과 보존

| 대상 | 이번 내부 처리 기준 |
| --- | --- |
| posts/polls/options | posts를 DELETED로 갱신, 원본 행은 FK/이력 보존을 위해 유지. 공개 조회·공유·추가 쓰기 차단 |
| bookmarks | BE1 PostDeletionParticipant.removeBookmarks(postId)를 같은 호출자 transaction에서 실행해 자동 해제. 실패하면 게시물 삭제도 rollback |
| post_photos/media | BE2가 참조 제거·DELETE_PENDING을 같은 transaction에 기록, commit 후 실제 Storage 삭제/재시도. 활성 참조 보호·늦은 전송 방어는 기존 사진 계약 유지 |
| comments/reactions/evaluations/votes | DB 관계는 유지하고 삭제 원본의 콘텐츠/결과·추가 참여를 차단. 참여 투표의 UNAVAILABLE 이력 유지 |
| activity_events | 누적 행동 이력은 유지. 삭제/표 변경으로 새 활동을 추가하거나 기존 누적 횟수를 빼지 않음 |
| adoptions | 감사 관계를 보존하고 공개/현재 기관 목록에서는 삭제 원본 제외. 삭제가 기관 취소 행동을 자동 생성하지 않음 |
| AI summary | 삭제 원본의 결과 비노출. revision/삭제 상태를 검사해 늦은 생성 결과가 다시 노출되지 않게 함 |

DB 보존은 원본/감사 이력 유지를 위한 기술 기준이며 삭제된 내용을 앱에서 계속 열람할 권한이 아니다. 보존기간·계정 탈퇴·운영 purge는 이번 MVP에서 새로 구현하지 않는다. 실제 Migration 변경 필요 시 기존 파일을 수정하지 않고 추가 Migration으로 검증한다.

삭제 호출 순서는 회원 guard → posts → polls → bookmark 처리 → media ID순 잠금/사진 처리이며 모든 DB 변경은 같은 Spring transaction이다. 외부 호출을 transaction에 넣지 않는다. 참여 쓰기도 같은 posts/polls 잠금을 사용하고 status·권한·종료를 재검사한다. PostDeletionParticipant는 인터페이스만 준비했으며 실제 삭제 adapter/transaction 검증은 #16/#26에서 수행한다.

진행 중 투표는 제목·본문·사진·종료시각만 수정 가능하고 질문·선택지·지역·주제는 변경하지 않는다. 기존 표/first_submitted_at은 그대로 보존하고 표 변경은 한 현재 표의 원자 교체다. 종료 후에는 게시물 수정·삭제와 신규/변경 투표를 모두 거부한다. 종료시각 수정과 제출은 같은 posts→polls 잠금으로 직렬화해 최종 저장 전 Clock을 다시 평가한다. 비-MVP 알림/예약 처리는 추가하지 않는다.

#### 준비와 실제 완료 기준

2026-10-08 #15 진행: 사용자 결정에 따라 역할별 작업 분리 없이 #14→#15→#16→#29 순으로 진행한다. 실제 `JdbcParticipationSnapshotReader`와 `JdbcPostSummaryReader`를 추가하고 상세·기존 참여/댓글/북마크/기관 목록이 같은 원본을 소비하도록 연결한다. batch 조회는 게시물별 SQL 반복 없이 공개 원본 ID 집합의 댓글·반응·투표·본인 상태를 읽는다. 삭제 snapshot은 map에서 제외하고 삭제 summary의 display는 비우며 원본 누락/DB 오류를 0으로 숨기지 않는다. 기본 port의 기존 의미와 삭제/보존 규칙은 유지한다.

#15 사용자는 API §5.3 상세 응답, 초기 댓글 LIKES/부모20개·전체 답글·기존 커서, 회원 본인 상태와 게스트 생략, 작성자 전용 fileId, #25 동일 득표율 반올림을 채택했다. F-UCDVNA의 활동 문의 등록 이메일을 로그인 회원·유효 공유 게스트에게 공개하는 범위도 별도로 명시 승인했다. 공개 기관 관계는 기관명·채택시각만 제공한다. `PostCommentPageReader.initialPage`는 기존 #22 조회를 같은 transaction에서 재사용하는 내부 연결점이고 `PostDetailLookup.read`는 #16의 검증된 작성자/기존 쓰기 transaction 결과 조회용이다. 상세 외 별도 HTTP endpoint나 DB 테이블은 추가하지 않는다. 서버 역할 시험에서 누락된 활동 상세 SELECT/RLS만 `20261008144530_allow_activity_detail_reads.sql`로 보완하고 기존 적용 Migration은 유지한다. 실제 FE adapter·Privy 사용자 흐름·공유 Supabase 적용/배포 확인은 #30/#31에서 해소하며 테스트의 합성 서명/로컬 DB 성공을 그 완료로 기록하지 않는다.

인터페이스·test-only batch 대체 구현 및 guest/타회원 정보 격리·삭제 display 차단 회귀 검사를 준비했다. #15/#17/#18/#19는 집계 adapter 전체 완성을 기다리지 않고 규약/Schema 통합 후 본문·목록을 구현할 수 있다. #21~#29도 PostContext/요약 규약을 사용해 개별 검증한다. 실제 연결·DB 경합·서비스 실패 전파는 해당 기능 완료 조건이고 FE 사용자 흐름은 #30/#31에서 확인한다. CI·CODEOWNERS/필수 검사 설정은 GitHub 실제 실행/관리자 설정 완료와 구분한다.

## 12. #74 남은 인증 계약 — BE1 검토 (2026-10-07)

작업 전 기준 확인: origin/back/develop `d9fe885`, AGENTS.md와 Backend 협업전략, API 정본 §1.3/§4.2~4.4, 이 검토표 §11, 최신 GitHub #74/#4 완료 조건. 사진 검토 브랜치의 로컬 `f3a17cf`(원격 미push 병합 포함)는 보존하고 별도 `back/feature/74-auth-contract`를 최신 기준에서 시작했다. 미커밋 변경은 없었다. 문서는 기존 파일명/경로를 유지한다.

### 12.1 변경 전 범위·검증 기준

| 구분 | 이번 작업 |
| --- | --- |
| 유지할 합의 | Privy Bearer 직접 검증/자체 세션 교환 제외, subject 1:1, 미가입/미완료/완료, 기존 identity/qualification/write guard, 권한·잠금/내부 규약 |
| 남은 인증 문서 | 공식 검증 사실/실제 키 확인, 이메일 원본·가입 원자성, Auth wire와 오류 매핑 제안, FE token 취득/보관/갱신 및 기존 로그아웃 제외 범위 |
| 공동 변경 제안 | 403 미가입/가입 미완료 code와 503 provider 장애 code, 기존 로그인 후보의 상태 확인 응답. 이유/영향을 API 정본에 먼저 제안하고 공통 오류표/Java 규약에 바로 등록하지 않음 |
| 범위 밖 | #4 adapter/Controller/Dependency/Migration/실행환경, 사진·게시물·참여 집계·삭제 보존·역할 분담·공통 port/fixture/CI/리뷰 정책 |
| 문서 검사 | 상대 링크·JSON 예시, 최신 기준과 diff, 기존 §11·공통 envelope/오류표·범위 밖 절/파일 보존, 이미 합의한 항목을 다시 선택 대기로 돌리지 않았는지 확인 |
| 구현/연동 검사 | 이번에는 코드·token·DB·FE 구현/연동 검사를 수행하지 않음. 문서 검사 통과를 실제 인증/adapter 완료로 표시하지 않음 |

### 12.2 확인된 내용과 미정 항목

| 항목 | 상태 | 남은 확인·연결 |
| --- | --- | --- |
| 인증 방식 | 합의됨: access token Bearer 직접 검증 | 자체 세션 비교/선택 재진행 없음 |
| 현재 주체/회원 상태 | §11.2 확정 규약 유지 | HTTP wire/오류 매핑은 아래 제안 검토, 운영 adapter는 #4 |
| 서명·iss/aud/sub/exp | 공식 provider 자료 대조 | 실제 앱 ID·공식 지원키·Java 검증기·시간 설정은 #4/실행값 #30 |
| verification key 설명 | official 자료에 ES256/Ed25519 문구 불일치 확인 | 추측한 JWKS URL/키 타입으로 구현하지 않음. 실제 앱 키·지원 검증 경로 대조 후 #4 수용 시험 |
| 이메일 출처/중복 | 이메일 자동 연결 금지/기존 UNIQUE 유지. 최초 가입 이메일은 검증된 subject의 서버 provider 정보 사용 제안 | 실제 검증 email 계정 선택/서버 credential·API 사용 확인은 #6/#7·BE2, identity token Bearer 대체는 추가하지 않음 |
| 가입 원자 저장 | 가입 완료는 동의·프로필·활동 지역·완료시각 원자 커밋 | #7 DB 경합/rollback 실검증. local row 자동 생성/legacy backfill 없음 |
| Auth wire | 기존 POST /auth/login과 /auth/sign-up 후보를 사용한 회원 상태 응답/입력 검토안 | FE/BE2 데이터 계약 확인 전 확정/구현 완료 아님. 새로운 공통 port/일반 프로필·자격 DTO 변경 없음 |
| HTTP 오류 | 401 UNAUTHORIZED·이메일 충돌409는 기존 규약 유지. 미가입/미완료403·키/provider 장애503 code는 공동 검토 제안 | 이유·영향은 API 정본에 기록. 기존 registry/envelope는 변경하지 않았고 승인 전 임의 구현 금지 |
| 갱신·보관 | SDK access token 취득/갱신, 자체 refresh 발급/저장 없음 | 실제 FE SDK 저장 설정/재시도 UI·실제 앱 TTL은 #6~8/#30. OTP 수치를 추측하지 않음 |
| 로그아웃 | 기존 MVP 서버 기능 제외 유지 | FE provider logout 사용 시 실제 동작 확인. 오프라인 JWT 검사만으로 즉시 revoke 완료 주장 금지 |

상세 검토안과 근거는 [API 정본의 #74 인증 잔여 계약 정리](Discushion_API_SPEC_v2.md#74-인증-잔여-계약-정리--be1)를 따른다. 공식 근거: [Privy tokens](https://docs.privy.io/authentication/user-authentication/tokens), [access token](https://docs.privy.io/authentication/user-authentication/access-tokens), [user 조회](https://docs.privy.io/api-reference/users/get), [logout](https://docs.privy.io/authentication/user-authentication/logout), [이메일 인증](https://docs.privy.io/authentication/user-authentication/login-methods/email). 2026-10-07 문서 조회이며 실제 provider 호출/앱 설정 확인은 아니다.

### 12.3 공동 검토·후속 인계

- [ ] BE1·BE2가 제안된 Auth code의 의미·status·기술 장애 구분과 공통 error adapter 영향을 검토한다. 공통 registry/§11 변경이 필요하면 이유/영향을 별도 승인 후 반영한다.
- [ ] FE 데이터 담당자가 Bearer/SDK 취득·갱신, login 상태 응답·미가입/미완료 분기, sign-up 입력·이메일 서버 확인·중복 응답, 오류/재시도·returnTo를 확인하고 실제 확인자/날짜/PR·SHA/이견을 #74에 기록한다. 문서 전체 최종 UI/연동은 기존 #30/#31 이관을 유지한다.
- [ ] BE1/#4는 공식 앱 검증키·검증 경로와 Clock/장애 수용 기준을 확인하고 필요한 계약이 back/develop에 반영된 뒤 별도 Feature로 구현한다. 각 기능은 §11 port/test-only 대체 구현으로 독립 개발하되 실제 adapter/권한/데이터/transaction 검증을 완료 조건으로 남긴다.
- [ ] 원격 통합 전에 최신 back/develop과 문서 diff/충돌을 다시 확인한다. 필수 CI·리뷰1명 유지, CODEOWNERS 생략. develop/main 직접 push·PR 병합·배포 없음.

작성/대조: Codex(BE1 계약 준비), 사용자 이번 역할/범위 지시. 실제 FE/BE2 리뷰나 코드/DB/Privy 연동 완료를 대신하지 않는다. #74 전체 완료/Issue 종료로 표시하지 않는다. 문서 검사 결과와 Feature PR 링크는 실제 실행/준비 후 연결한다.

문서 검증(2026-10-07): 상대 파일 링크 16개와 새 JSON 예시 3개 검사 통과, 기존 검토표 전체(§11 포함) 보존 확인, API 공통 형식/상태·공유·사진/수정·내부 독립 개발 규약 보존 확인, diff 공백 검사 통과. 변경 파일은 API 정본과 이 검토표 2개뿐이며 코드·Dependency·Migration·공통 Java 규약·담당 분담·CI/리뷰 정책 변경은 없다. Backend test/build·실제 Privy token/DB/FE 검증은 이번에 재실행하지 않았다. PR의 자동 CI 결과는 별도로 확인하며 문서 검사 결과로 대체하지 않는다.

### 12.4 PR #104 최신 기준·구현 여부 재검증 (2026-10-07)

사용자 요청으로 Codex(BE1)가 인증 Feature `back/feature/74-auth-contract`의 `17a0a41`과 최신 `origin/back/develop 1366fabba218cd345be4d23376823cf2ea1998f0`을 대조했다. 기준에는 업무표 PR #105와 사진 DB 검토 PR #85가 병합되어 있다. 작업 시작 시 미커밋 변경은 없었으며 최신 기준을 `git merge --no-commit --no-ff`로 로컬 반영했다. 자동 병합 성공, 미해결 충돌 없음. 기준에서 가져온 사진·Migration·담당 규칙은 유지하고 이번 인증 변경은 API 정본과 이 검토표 두 파일로 제한한다. 커밋·push·PR 병합·배포는 수행하지 않았다.

| 검증 | 실제 결과·범위 |
| --- | --- |
| 소스·PR 범위 | 원격 PR 변경은 API 문서 두 개뿐. 실제 소스에는 Health Controller와 공통 port/record가 있으며 인증 Filter/Interceptor·JWT 검증기·Auth Controller·identity 실제 adapter는 없다. test-only fixture를 실제 구현으로 세지 않는다. |
| Java 17 test/build | 2026-10-07 22:15 KST, 최신 기준을 포함한 로컬 작업 트리에서 `gradlew.bat --no-daemon test build --console=plain --rerun-tasks` 성공. 총16개 중13통과/실패0/오류0/원격3skip. Health3·localhost JDBC4·설정1·공통 fixture5 통과. |
| 실제 로컬 HTTP | 생성한 JAR를 DB 연결이 없는 local profile로 실행. GET /health=200, POST /api/v1/auth/login은 토큰 없음/합성 무효 토큰 모두404, POST /api/v1/auth/sign-up=404. 후보 인증 경로가 아직 제공되지 않는 것을 확인했으며 토큰 거부·가입 성공 시험 통과를 뜻하지 않는다. 시험 서버와 이번에 시작한 localhost DB는 종료했다. |
| 문서·범위 보존 | 최신 기준의 공통/사진/내부 규약 등 API9개 절과 인증 추가 전 검토표 전체(§11 포함) 보존, JSON 예시3개·상대 파일 링크16개 확인. 최신 기준 대비 인증 변경은 문서2개뿐이며 working/staged diff 공백 검사·미해결 충돌 검사 통과. |
| 원격 CI | 기존 원격 head `17a0a41`의 Backend tests and build 성공 기록은 확인했다. 이번 로컬 병합/문서 갱신은 아직 push하지 않았으므로 새 조합의 GitHub CI 통과로 표시하지 않는다. |
| 실제 인증·연동 | Privy OTP/실제 토큰·검증키·회원 연결/가입 API·요청 컨텍스트 격리·실제 서버 역할·FE 연결은 미검증이며 기능 미구현. #4/#6~8/#30/#31의 완료 조건을 유지한다. |

결론: 충돌 없는 로컬 통합과 기존 기반의 회귀 검증을 완료했다. #104는 남은 인증 계약 문서 PR이며 인증 기능 구현 완료가 아니다. Auth wire/오류 code 공동 검토·실제 앱 검증 경로 확인·필수 리뷰1명은 계속 미완료다. 필요한 계약을 back/develop에 반영한 뒤 #4를 별도 Feature에서 구현한다. §12.3의 미완료 항목을 근거 없이 완료로 표시하지 않는다.

### 12.5 #4 인증·자격 기반 구현과 사용자 오류 결정 (2026-10-07)

착수 기준: Backend 협업전략 §6.1의 BE1 두 번째 업무, 열린 #4(F-TSOXGG/F-ATWJDJ/F-OPNIXL), 최신 back/develop `6a84ffc`의 #104/#85/#101 통합. 미커밋 변경 없는 상태에서 `back/feature/4-auth`를 새로 만들었다. #74 전체 종료나 상대 기능 전체 구현을 일괄 착수 조건으로 두지 않는다. 확정된 identity port·회원 Schema를 구현하며 일반 가입/로그인 endpoint wire는 #7/#8의 미확정 계약을 임의 확정하지 않는다.

**사용자(BE1) 결정:** 403 USER_REGISTRATION_REQUIRED 추가, 가입 미완료도 같은 회원가입 화면 안내/같은 code 사용, 503 AUTH_PROVIDER_UNAVAILABLE 추가. 후속 질문에 500 INTERNAL_ERROR의 공통 형식/일반 안내·원문 비노출도 승인했다. 내부 미가입/미완료 상태는 구분하고 REGISTRATION_INCOMPLETE는 별도 HTTP code로 등록하지 않는다. 실제 FE·BE2 확인과 구분한다.

구현은 `com.discushion.identity`에 둔다. 기존 contracts/support 인터페이스/fixture·Migration·사진/게시물/집계/삭제 보존·실행환경/CI 파일은 수정하지 않는다.

| 구성 | 실행 책임·경계 |
| --- | --- |
| PrivyAccessTokenVerifier / PemVerificationKeySource | 신뢰한 앱 공개 SPKI P-256 키로 ES256/JWT 서명·iss/aud/sub/sid/iat/exp/nbf를 검사. Clock 사용, 기본 허용오차0. 토큰의 jku/x5u를 키 원천으로 쓰지 않고 linked_accounts가 있는 identity payload를 Bearer access token으로 받지 않는다. 서명/키 파싱은 실제 암호 연산이며 실제 Privy 발급 검증은 별도. |
| VerificationKeySource / IdentityConfiguration | 실제 키는 BE2와 확인한 앱 설정에서 서버 bean으로 공급. 앱 ID는 기존 PRIVY_APP_ID를 읽는다. 현재 실제 앱 ID/키 설정이 없으며 임의 JWKS URL·키를 만들지 않는다. key source 부재/키 오류는503, 허용으로 우회하지 않음. 실제 키 공급/회전/Clock 설정·외부 앱 대조는 #30과 협의 후 연결. |
| BearerIdentityFilter / RequestActorContext | /api/v1의 요청별 Bearer 검증 후 서버 subject로 회원 조회. 토큰 없음은 내부 Optional.empty이며 endpoint가 공개/회원/게스트 접근을 판정. 무효 토큰은401로 종료하고 익명으로 강등하지 않음. 중복/빈/다른 scheme 헤더 거부, OPTIONS/Health 유지. servlet attribute를 요청 종료·오류 때 제거하고 임의 비동기 스레드로 상속하지 않음. |
| JdbcMemberStore | subject로만 로컬 회원 연결 조회. 이메일 연결/회원 자동 생성 없음. 자격 원본은 neighbor_verified_regions와 institution_credentials. 만료 포함 저장 사실을 제공하고 쓰기 전에 현재 Clock으로 판정. users FOR UPDATE는 호출자의 동일 DataSource·쓰기 transaction에서만 유지. 임의 REQUIRES_NEW 없음. |
| MemberAuthorization | 현재 회원을 공통 guard로 잠그고 최신 가입 상태·이웃 완료 지역·기관 유효기간/담당 지역을 검사. 소유권 비교는 실제 대상 작성자 ID를 사용하고 기능 담당자가 기존 소유권 오류로 거부. 게시물/투표/파일 잠금 순서·최종 유형/상태·저장 직전 재검사는 기존 §11.5와 기능 담당자의 완료 조건 유지. |
| IdentityErrorResponse / IdentityExceptionHandler | 승인된 Auth/내부 오류와 기존 권한 오류의 envelope·HTTP 매핑. DB/기술 장애를 미가입·빈 결과로 처리하지 않고500. token·subject·provider/DB 원문 미반환, traceId는 서버 생성. 일반 Framework 4xx의 HTTP 상태는 유지. |

공식 자료 대조: [Privy 공식 Node SDK 배포 소스 auth.mjs, 0.20.0](https://unpkg.com/@privy-io/node@0.20.0/lib/auth.mjs)에서 ES256·typ JWT·issuer/app 검증과 importSPKI/필수 sid 반환을 확인했다. 기존 문서의 ES256/Ed25519 설명 혼재는 역사로 보존하되 ES256 시험에는 P-256 공개키를 사용한다. 이를 실제 앱 키 호환/회전 검증으로 표시하지 않는다. Java 라이브러리는 Spring Boot가 관리하는 spring-security-oauth2-jose/Nimbus를 사용한다.

테스트는 생성한 합성 서명키·test-only endpoint와 localhost 전용 JDBC를 사용한다. 실제 Privy·FE·공유 개발 DB·최소 권한 서버 역할의 검증은 아니며, #75 실제 시연 계정 검증도 별도로 유지한다. 기본 DB 권한표는 [DB 연결 결정 기록의 #4 절](../collaboration/backend-db-connection-decisions.md)에 준비하고 BE2 공동 검토/#30 실제 적용 전 확정한다. 최종 테스트 결과는 아래에 실행 후 기록한다.

#### #4 로컬 실행 결과와 미완료 조건

2026-10-07 23:09 KST, Codex(BE1), `back/feature/4-auth`의 `6a84ffc` 기준 미커밋 작업 트리. Java17 `gradlew.bat --no-daemon test build --console=plain --rerun-tasks` 성공. JUnit45개 중42통과/실패0/오류0/원격Supabase3skip. 기존13개 통과에 신규29개(ES2566·권한5·filter7·실제JDBC6·실제HTTP+JDBC5)가 추가됐다. 시험 서버와 이번 실행에서 시작한 PostgreSQL은 정상 종료했다.

- JWT: 실제 합성 P-256 서명 성공, 타 서명키/HS256/잘못된 iss·aud/만료 경계/누락 exp/미래 iat·nbf/identity payload/없는 subject·sid 거부, 키 공급 장애 구분. 실제 Privy 발급 token 시험은 아님.
- 실제 JDBC: subject 연결과 미가입/미완료/완료, 완료 지역 원본과 프로필 활동 지역 분리, 만료 기관 저장 사실, 트랜잭션/동일 DataSource/쓰기 조건, 없는 회원/rollback, 별도 연결의 UPDATE lock_timeout(55P03) 및 호출자 rollback 후 잠금 해제 확인.
- 합성 NOLOGIN 역할: localhost transaction 안에 권한·RLS 정책을 준비했다가 모두 rollback. SUPERUSER/BYPASSRLS 없이 users의 IDENTITY INSERT/RETURNING과 회원 guard 동작, 시퀀스 USAGE 없음·users DELETE 없음 확인. 실제 Supabase 서버 로그인 계정 시험과 구분한다.
- 실제 HTTP: test-only 프로필의 route에서 실제 filter/ES256/PEM·JDBC·users 잠금·오류 handler 연결. 무토큰 회원 요청401, 무효 token401, 미가입/미완료403 USER_REGISTRATION_REQUIRED, 실제 합성 회원200·클라이언트 userId 무시, 다음 무토큰 요청에 주체 유출 없음, 내부 오류500/원문 비노출 확인. test-only Controller/키는 운영 JAR에 넣지 않는다. /auth/login·/auth/sign-up wire를 임의 구현하지 않음.
- 기본 권한표27테이블 작성, 기존 Migration5개와 공통 contracts/support·BE2 담당 사진/게시물 문단·환경/CI 구성 보존. Schema 보완이 필요하지 않아 추가 Migration 없음. 원격 DB·역할·Privy 앱 설정·FE 변경 없음.
- 최종 문서 검사: 변경 문서의 상대 링크26개, API 정본 JSON33개 파싱, 기본 권한표와 실제27테이블 이름 대조 통과. 기존 §11과 BE2 사진 API 문단 보존, diff 공백 검사 통과. 운영 JAR의 identity class14개/테스트 fixture·Controller0개 확인. 실제 운영 JAR localhost GET /health=200, 무효 Bearer POST /api/v1/auth/login=401; 무토큰 login/signup 후보는404로 #7/#8 미구현 경계를 확인. 추가 시험 서버도 종료.

현재 상태는 **구현 및 로컬 개별 검증 완료 / 실제 연결·공동 검토 대기**다. 다음을 #4 완료 조건으로 유지한다.

- [ ] BE2 #30: 실제 Privy 앱 ID·공개 검증키/공급·회전·Clock 설정을 준비. BE1 #4가 신뢰한 VerificationKeySource bean에 연결하고 실제 provider 정상/위조/만료·환경 장애를 검증. #30 전체 종료를 일괄 선행으로 두지 않음.
- [ ] BE1·BE2: 기본 권한표·RLS 작업 정책 공동 확인, 실제 서버/Migration 계정 분리 및 서버 역할의 정상/금지 DB 작업·TLS 검증. 관리자/local 합성 역할 결과로 대신하지 않음.
- [ ] 영향 있는 FE·BE2: 승인한 가입 화면 안내/오류 code 소비 동작, 남은 login/signup wire 확인. 실제 FE 인증/권한·#75 시연 계정은 각 기능/#30/#31에서 확인.
- [ ] 기능 담당자: 실제 posts/polls/media adapter와 같은 transaction/잠금 순서·최종 소유권/지역/유형·시간 경합 검증. test-only 상대 adapter 성공을 실제 연동 완료로 기록하지 않음.
- [ ] Feature diff 검토·최신 기준/필수 CI·리뷰1명 확인 후 별도 사용자 요청으로 commit·push·PR. #4 종료/PR 병합·배포는 수행하지 않음.

### 12.6 최신 back/develop pull·조건부 PR 병합 준비 (2026-10-07)

사용자가 back/develop pull·AGENTS.md 확인·PR 생성과 조건 충족 시 병합을 요청했다. #4 미커밋 변경을 untracked 포함 stash로 보존한 뒤 로컬 back/develop을 `git pull --ff-only origin back/develop`으로 `c768080`까지 갱신했다. 기준 상태를 실제 localhost JDBC/Java17 test/build로 먼저 검증했고16개 중13통과/원격3skip, 실패0/오류0/build 성공이다. #4 Feature를 최신 기준으로 fast-forward한 뒤 원래 변경을 복원했으며 충돌 없음. 복원된 최종 Feature도 2026-10-07 23:29 KST에45개 중42통과/원격3skip, 실패0/오류0/build 성공을 확인했다. 기존 §12.5 검증 범위와 실제 연동 미완료 조건은 유지한다.

최신 AGENTS.md의 Backend PR 리뷰 기준과 Backend 협업전략 §5.1을 적용한다. **리뷰 분류: 상대 리뷰 필요.** 인증/가입 상태·제품 권한·공통 오류/API 계약과 기본 DB 권한표를 포함하므로 일반 PR의 상대 승인 생략 대상이 아니다. BE2의 최신 변경 리뷰·승인, 필수 CI, 최신 base·충돌/미해결 지적 없음과 PR 생성 후 최종 diff 재검토를 확인한다. GitHub의 일괄 승인 수0이나 Codex 검증을 BE2 승인으로 대신하지 않는다. 실제 권한표 공동 확정·Privy/서버 역할/FE 검증 전 #4 전체 완료·Issue 종료로 표시하지 않으며 PR에는 Refs #4와 남은 조건을 명시한다. 조건을 만족하지 않으면 Draft/병합 대기로 보고한다.

## 14. #9 지역 후보 조회 계약과 검증 (2026-10-08)

### 14.1 착수·사용자 결정·범위

- 역할 BE1, Issue #9 (F-QQKYLC·F-ATWJDJ), `back/feature/9-region`. 작업 트리 clean을 확인하고 실제 `git pull --ff-only origin back/develop`으로 최신 `cc148e1`을 확인했다. Feature 생성 전 Java17/localhost PostgreSQL17.11 기준 test/build는45개 중42통과/원격Supabase3skip/실패0/오류0/build 성공이다. #4 PR #113은 원격에서 병합됐지만 실제 Privy/서버 역할/FE 확인의 #4 잔여 완료 조건은 유지한다.
- 사용자(BE1)가 가입 전 조회, 지역 이름 검색, 가나다순, 기본20·최대100/요청, DB 등록 지역만 반환을 채택했다. 이름/ID keyset cursor와 기존 envelope·숫자 ID를 사용한다. 다른 목록/API/공통 오류·사진·게시물·집계·삭제/보존·담당 분담은 변경하지 않는다.
- 구현 범위: 실제 GET /api/v1/regions, 입력 검사, SELECT-only JDBC, 같은 이름의 별도 ID와 기존 nullable map_feature_key, A영역의 findById 조회. Schema/기존 Migration·운영 seed·서버 역할/환경 설정 변경 없음.
- 검증 기준: q NFC/공백·입력 범위·문자 그대로의 부분 검색, 가나다순/동률ID, 기본20·상한100·전체100초과 연속 조회, cursor/검색 조건 불일치, 실제 JDBC 공유 FK/비존재 ID, 실제 HTTP 익명/미가입/미완료·오류 envelope, 기존 test/build와 최종 diff.

### 14.2 FE 계약 대조와 지도 연결 확인 항목

확인자 **Codex(BE1 구현 준비)**, 검토 대상은 back/develop `cc148e1`의 API 정본 §4.6/5.2·검토표 §11/DB 명세 §5.6 및 origin/front/develop `b9d2acc`의 기존 FE 상세지침서(지역 선택 §608·신규 가입 §1354·탐색 지역 선택 §1420·wire 표 §2510)와 auth/contracts.ts다. 숫자는 해당 SHA의 줄 번호다. FE wire는 id/name·기존 data/meta·cursor/size를 요구하며 지역명 q는 합의 대기였다. FE auth service 타입은 Backend DTO가 아니므로 이를 그대로 서버 wire로 복제하지 않았다. 후보 선택과 가입/프로필 저장, 임시 탐색 지역을 구분한다.

이 기록은 문서/코드 대조이며 **FE 담당자·BE2의 실제 확인/승인 기록이나 FE/BE 연결 결과가 아니다.** 사용자 결정과 검토안을 이번 Feature의 API 정본 §4.6에 포함하며 다음을 공동 확인한다.

최종 fetch에서 origin/front/develop이 `4bb2052`로 갱신된 것도 확인했다. 위 FE 기준 문서와 auth/contracts.ts는 바뀌지 않았으며 신규 explore/model.ts·service.ts·mock.ts를 추가 대조했다. ExploreRegion의 string ID·neighborVerified는 명시적으로 FE display/mock 계약이고 실제 /regions HTTP adapter는 없다. 이를 서버 JSON number ID나 공개 후보의 자격 응답으로 변경하지 않는다. FE 실연결 시 기존 숫자 ID를 display 모델로 변환하고 이웃 자격은 별도 회원 상태 원본에서 조합하는 소비 계약을 확인해야 한다. FE mock의 실명 지역/문자열 ID는 DB 정본 seed로 사용하지 않는다.

| 확인 주체·시점 | 구체적인 남은 항목 |
| --- | --- |
| BE1·BE2, #9 완료 전/실제 지역 적재 전 | 시연 지역 범위·행정동/법정동 기준·정본 데이터의 출처/버전·external_code 대응. 임의 지역·코드·계층/geometry를 추가하지 않음 |
| BE1·BE2, #9 연결 합의 및 #19 지도 구현 전 | 기존 regions.id ↔ 정적 지도 feature의 map_feature_key 대응을 검토. 같은 이름을 키로 쓰지 않고, 누락은 null. 실제 지도 원천/경계·좌표는 미확정이며 연결 성공으로 표시하지 않음 |
| FE 담당자·BE2, 소비 계약 확인/연동 전 | 익명 GET, q·size·cursor/400 field 상세, id/name/mapFeatureKey nullable와 data/meta 소비 확인. q 변경 시 cursor 초기화, 선택 ID로 가입/프로필 저장·임시 탐색 구분 |
| BE2 #30와 BE1 #9, 실제 서버 역할 검증 시 | 기존 권한표의 regions SELECT·Schema USAGE 및 서버 전용 SELECT RLS 정책으로 실조회/TLS 확인. 공개 anon/authenticated DB 역할을 열지 않음. Spring의 공개 조회 예외와 DB 직접 공개는 다름 |
| FE·BE, #30/#31 연결 이관 해소 | 합의 환경에서 가입 전 후보 조회→선택→가입 저장, 프로필 변경 저장/재조회 사용자 흐름과 오류/재시도. 직접 HTTP 검사를 실제 FE 연결로 기록하지 않음 |

계약/Schema가 준비된 #9 조회 개발은 #19 지도 전체 완료를 기다리지 않는다. 지도 대표 조회 자체는 BE2 #19, 화면 표시/선택은 FE 범위다. #9의 공동 연결 합의·실제 데이터 원본 검증 조건은 별개로 유지한다. 실제 지역 데이터가 없는 환경에서는 빈 목록을 반환한다.

### 14.3 실행 결과와 완료 상태

2026-10-08 00:34 KST, Codex(BE1), 기준 `cc148e1` + #9 미커밋 작업 트리. Java17 `gradlew.bat --no-daemon test build --console=plain --rerun-tasks` 성공. 총69개 중66통과/실패0/오류0/원격Supabase3skip. 신규24개(입력/cursor8·실제JDBC7·실제HTTP7·DB없는HTTP2)는 모두 통과했다.

| 검사 | 실제 결과·한계 |
| --- | --- |
| 실제 JDBC 검색·페이지 | 한글 가나다순/동일 이름 ID 정렬, NFC 이름/cursor 일치, `%_!` 문자 검색·SQL 주입 문자열 분리, 103행을100+3으로 중복/누락 없이 조회, 미존재 ID/검색 결과 없음 확인 |
| 공통 Region 원본 | 합성 fixture의 profiles·neighbor_verified_regions·institution_credentials·posts가 같은 regions.id에 연결되고 미존재 ID 저장은 FK 거부. 조회로 자격/회원 완료 상태를 만들지 않음. 각 후속 기능 전체 API/운영 시연 원본 연결은 아님 |
| 실제 HTTP | 실제 운영 Region Controller·Configuration·JDBC·공통 ES256/PEM filter 연결. 익명/미가입/가입 미완료 조회200, 기본20·후속3, 숫자ID·mapFeatureKey/null·external_code 비노출, 입력400 field/traceId, 검색 조건 바꾼 cursor400, 잘못된 Bearer401·후속 익명200 확인. 앱/서명키·행 데이터만 합성 test-only |
| SELECT 전용 모의 역할 | localhost transaction 안에서 NOLOGIN/NOSUPERUSER/NOBYPASSRLS 역할·SELECT RLS 정책을 만들고 rollback. Schema USAGE·regions SELECT만으로 조회/ID 확인 성공, INSERT/UPDATE/DELETE·시퀀스 USAGE 없음, 실제 INSERT는42501 거부. 실제 Supabase 서버 LOGIN 계정 시험 아님 |
| DB 미설정·JAR | DB 없는 local Health200 유지, 정상 지역 입력은500 INTERNAL_ERROR/원문 비노출, 잘못된 입력400. 운영 JAR 별도 localhost 실행에서도 Health200·지역500·입력400·잘못된 Bearer401 확인. Region 운영 class11개, 테스트 fixture0개 |
| 실제 ERD/카탈로그 | 기존27테이블·180컬럼·단일FK47/복합FK8, 대조 errors0. Migration 수정/추가 없음. #9 합성 지역/임시 역할 잔존0, 이번 시험 Java/DB 서버 종료 |
| 문서·경계 | API §4.6 외 모든 절(공통·사진·게시물 포함)과 검토표 D10/§14 외 모든 내용 보존. JSON 예시34개/상대 파일 링크26개 통과. 기존 contracts/support·인증 코드·환경/CI·BE2 코드 변경 없음 |
| 최신 기준·병행 PR | 최종 fetch의 origin/back/develop은 `cc148e1`로 변경 없음. PR #107 사진 head `33379cb`와 두 API 문서를 merge-base 기준으로 temp 파일에서 git merge-file 검사해 충돌0. 실제 branch 병합/PR CI 결과를 뜻하지 않음. working/staged diff 공백·미해결 conflict0 |

**상태: #9 조회 구현/로컬 실제 DB·HTTP 검증 완료, 공동 데이터·소비 계약 확인/운영·FE 연결 대기.** §14.2의 미완료 조건은 유지한다. #9 Issue 종료·commit/push/PR 생성·병합은 수행하지 않았다. API 계약을 포함하므로 향후 PR은 상대 Backend 리뷰 대상이다. 원격 Supabase3개는 opt-in 검사로 이번 localhost 실행에서 제외됐으며 이 로컬 결과를 원격 연결 검증으로 표시하지 않는다.

### 14.4 제외된 원격 Supabase3개 별도 실행 (2026-10-08)

사용자가 반복된 skip의 이유를 확인해 달라고 요청해 #9 구현·최종 diff 검토 후 검사 코드를 확인했다. SupabaseJdbcSmokeTests는 `DISCUSHION_VERIFY_SUPABASE=true`일 때만 실행하며 기존 localhost 검증 스크립트/CI는 false를 사용한다. 실제 공유 DB의 지정 환경·비밀 설정·TLS CA를 쓰는 검사이므로 localhost 시험과 분리한 것이며 skip은 통과/원격 연동 완료가 아니다.

기존 ignored backend/.env의 대상이 시험에 고정된 개발 프로젝트와 일치하고 TLS verify-full/인증서 설정이 있음을 확인한 뒤 2026-10-08 00:41 KST, Codex(BE1)가 `gradlew.bat --no-daemon test --tests com.discushion.SupabaseJdbcSmokeTests --console=plain --rerun-tasks`를 true 환경변수로 별도 실행했다. **3개 모두 통과/실패0/오류0/skip0**. 지정 개발 DB의 SELECT만 수행했고 DB 변경/Migration·GRANT·계정/환경 설정 변경 없음. 테스트 pool은 종료됐다. 전체 로컬66개와 별도 원격3개가 통과한 것이며 같은 실행에서69개가 모두 원격으로 검증됐다는 뜻이 아니다. build 성공은 §14.3의 전체 로컬 실행 결과다.

| 원격 검사 | 실제 확인 결과 |
| --- | --- |
| TLS JDBC 연결 | 인증서·호스트명 검증 포함 접속, SELECT1, postgres DB/PostgreSQL17 확인 |
| 원격 Schema/Migration 이력 | 27테이블·176컬럼·FK55·RLS27·#3의 Migration4개 확인 |
| 공개 API 역할 격리 | anon/authenticated/service_role3개가 private discushion Schema USAGE/테이블 데이터 권한을 갖지 않음 |

**원격·로컬 차이 유지:** 원격은4개 Migration/176컬럼, 로컬 최신은 사진 보완 Migration 포함5개/180컬럼이다. 원격 검사 통과는 기존 #3 적용 상태에 대한 것이며 PR #85의 사진 보완 원격 적용 성공을 뜻하지 않는다. BE2가 #13/#30 실제 실행 전에 통합된 사진 보완을 조율해 적용하고 해당 rollout에서 smoke 기대 이력/컬럼을 함께 갱신·검증해야 한다. #9는 기존 regions 컬럼만 사용한다. 실제 최소 권한 서버 LOGIN 계정·지역 원본/지도 대응·Privy/FE 연결은 이 기존 postgres 계정의 SELECT 검사로 대신하지 않는다.

### 14.5 최신 기준 pull·사진 보완 원격 적용 (2026-10-08)

사용자가 최신 pull 및 사진 보완 Migration1개 미적용 문제 해결을 명시적으로 요청했다. 실제 병합은 사진 계약 PR #107이며 #13 기능 구현 Issue는 아직 열려 있음을 확인했다. #9 변경18파일(untracked 포함)을 stash로 보존하고 로컬 back/develop을 `git pull --ff-only origin back/develop`으로 `42dfb14`까지 갱신했다. 기준 test/build45개 중42통과/원격3skip를 확인한 뒤 #9 Feature를 fast-forward하고 stash를 복원했다. 문서 자동 병합 충돌0, 보존한 Region 소스/시험13파일의 내용 동일(Windows 줄바꿈 전환만 존재), 백업 stash도 유지한다. #107 사진 문단과 확정 공통 규약을 보존한다.

이전 §14.4의 원격176컬럼/이력4개는 **적용 전 기록**이다. 이번에는 통합된 `20261007104543_support_photo_cleanup_leases.sql`을 그대로 지정 개발 프로젝트 `pmhmgqpyvrbbseqelpze`에 적용했다. 사용자 지시는 기존 원격 적용 보류의 후속 실행 요청이며 새 계약·Migration 내용을 임의로 결정하지 않았다. CLI2.120.0 help와 공식 changelog를 확인했다. dry-run에서 승인된1파일만 대상임을 확인하고 `db push --linked --project-ref pmhmgqpyvrbbseqelpze --skip-vault --yes`를 실행했다. 기존 Migration5개는 수정하지 않았고 역할/seed/Vault/Storage·비밀 설정도 변경하지 않았다.

| 검증 | 실제 결과 |
| --- | --- |
| 원격 적용 전 | 27테이블·176컬럼·이력4개, media_files/post_photos 각각0행, 중복 파일 참조0, lifecycle shape 위반0 |
| 원격 적용 후 | 27테이블·180컬럼·FK55·RLS27·이력5개. 추적4컬럼, 신규4제약 및 교체2제약 검증됨, cleanup 인덱스2개 유효/ready. 기존 사진/파일 행0 유지 |
| 재실행/Advisor | 후속 dry-run migrations=[]/upToDate=true. security WARN/ERROR0(INFO1), performance WARN/ERROR0(INFO2). 정보성 안내를 권한 확대/운영 연동 완료로 취급하지 않음 |
| 원격 smoke 갱신 | SupabaseJdbcSmokeTests의 기대180컬럼/이력5개 및 추가4컬럼·6제약·2유효인덱스 검사를 갱신. TLS verify-full 접속과 공개 API3역할 접근 차단은 유지 |
| 전체 Java17 test/build | 2026-10-08 01:16 KST, `42dfb14` + #9/이번 rollout 미커밋 변경, `gradlew.bat --no-daemon test build --console=plain --rerun-tasks`. localhost 시험과 원격 opt-in3개를 함께 활성화해 **69통과/실패0/오류0/skip0/build 성공**. 실제 원격 검사는SELECT-only, 쓰기 fixture/잠금 시험은localhost만 사용. test pool/HTTP 서버와 이번에 시작한 PostgreSQL 종료 |

**미적용 사진 Migration 문제는 해결됨.** 실제 최소 권한 서버 계정/RLS 작업 정책·Privy·Storage 전송/삭제/worker·FE 연결, #9 지역 원천/지도 대응 공동 확인은 별도 완료 조건으로 유지한다. 소스·검증 기대값·기존 문서 갱신은 현재 Feature 작업 트리에 있으며 commit/push/PR·GitHub 병합·배포는 수행하지 않았다. DB 실행 결과는 [DB 연결 결정 기록](../collaboration/backend-db-connection-decisions.md)의 최신 절을 함께 따른다.

### 14.6 #9 Feature PR·조건부 병합 준비 (2026-10-08)

사용자가 #9 commit/push/PR 생성과 조건 충족 시 병합까지 요청했다. 최신 origin/back/develop은 `42dfb14`이며 Feature의 기준과 분기0/0을 확인했다. 이 PR은 Region 구현9파일·신규 시험4파일·실제 원격 smoke1파일 및 기존 문서7파일을 포함한다. 사진 보완 원격 적용의 사용자 요청에 따른 smoke 기대값/실행 기록도 함께 공유하며 새 Migration/Schema·역할/환경 코드 변경은 없다. §14.5의69개 모두 통과/skip0/build 성공 결과와 최종 commit을 대조하고 PR 생성 후 원격 최종 diff를 다시 검토해 결과를 기록한다.

**리뷰 분류: 상대 리뷰 필요.** 새 공개 지역 API 계약과 소비 DTO/인증 예외, 원격 Schema 검증 기대값을 포함하므로 최신 변경에 대한 BE2 승인1명이 필요하다. AGENTS.md Backend PR 리뷰 기준과 협업전략 §5.1에 따라 필수 CI·최신 base·충돌/미해결 지적 없음·작성자/Codex 최종 diff 검토를 함께 확인한다. GitHub의 일괄 승인 수0이나 합성 시험/다른 PR의 승인을 이번 상대 승인으로 대신하지 않는다.

PR은 Refs #9를 사용한다. 후보 조회·공통 FK의 로컬 실제 구현 검증은 끝났으나 §14.2의 실제 지역 원천/지도 대응·소비 계약 공동 확인과 서버 역할·FE 연결 완료 조건은 남아 있어 Issue를 자동 종료하지 않는다. BE2 리뷰 요청에는 현재 조회 계약의 정합성과 남은 지도/기관 지역 연결 기준 확인을 명시한다. FE 실제 사용자 흐름은 기존 #30/#31 이관 조건을 유지한다. 별도 사용자 요청이 없었던 main/develop 직접 push·배포는 수행하지 않는다. 조건 충족 여부와 실제 PR 링크·CI/리뷰·병합 결과는 PR 및 Issue에 기록한다.

## 15. #12 기관 조회 wire와 FE 표시 port 변환 (2026-10-08)

### 15.1 결정과 근거

PR [#154](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/pull/154)의 BE2 리뷰 요청에 따라 API 정본 §4.8을 현재 구현의 인증·200 JSON·오류·상태 의미로 갱신했다. 사용자가 #12에서 채택한 사항은 대표 상태 하나 조회, 같은 기간에 유효 자격 1개, 한국 시간 달력 1년(윤년 2월29일 → 다음해2월28일 같은 시각)이다. 대표 선택은 기존 #10의 유효 이력 우선·최근 완료/ID 순서를 유지한다. 증빙/신청·배지 플래그 수동 부여·이웃 자격 확대를 추가하지 않는다.

Backend wire는 API 정본 §4.8을 사용한다. FE InstitutionQualification은 표시 port이며 wire DTO가 아니다. 대조한 FE 기준은 front/develop c473af32bdc84e7eef67072631fd7b6541dbb949의 [model.ts](https://github.com/2026-KW-HACKATHON/33_Last-Stand-Before-Enlistment/blob/c473af32bdc84e7eef67072631fd7b6541dbb949/frontend/src/features/institution/model.ts), provider.tsx, README.md다. 코드의 배열/문자열 표현 차이는 아래 adapter로 연결할 수 있으며 Backend의 공통 ID 타입이나 FE 표시 port를 변경할 근거가 아니다.

### 15.2 adapter 변환 계약

| Backend wire | FE InstitutionQualification 표시 값 | 조건 |
| --- | --- | --- |
| data.id | subjectId | 응답 ID는 로컬 회원 ID. adapter가 검증된 현재 회원의 localMemberId와 일치하는지 확인한 뒤, 그 회원에 이미 결합된 Session subjectKey를 subjectId로 사용 |
| institutionVerification.institutionId / institutionName | institution {id, name} | ID를 양의 JSON 안전 정수로 검증한 뒤 10진 문자열로 변환. 기관명으로 ID를 만들거나 정본을 병합하지 않음 |
| responsibleRegion {id, name} | responsibleRegions: [{id, name}] | 같은 ID 검증/문자열 변환 후 길이1 배열로 감쌈. 이 배열은 현재 단일 담당 지역을 나타내며 복수 자격·지역 정책을 새로 허용하지 않음 |
| status NOT_SUBMITTED | status none | 기관 정보/시각 null, isActive/institutionVerified false인 정상200에서만 변환. 이력 없음과 조회 오류를 혼동하지 않음 |
| status COMPLETED | status completed | 필수 기관/지역/시각을 검증. 미래 completedAt의 completed 상태도 현재 자격을 뜻하지 않으며 기간 비교로 표시 |
| status EXPIRED | status expired | 원본 기관/지역/완료·만료시각을 보존하고 배지/업무 허용은 false |
| completedAt / validUntil | 같은 ISO-8601 문자열 | 유효 날짜 및 completedAt < validUntil 확인. FE에서 승인일/만료일을 생성하지 않음 |
| isActive / institutionVerified | 표시 port의 시간 기반 배지 판정과 대조하는 서버 스냅샷 | 두 값의 일치 확인. 현재 자격은 completedAt ≤ 평가시각 < validUntil일 때만 true. FE timer/focus/visibility 갱신은 화면 표시용이며 실제 업무는 서버가 다시 검사 |

Session subjectKey의 전역 형식을 바꾸지 않는다. 구현 adapter는 인증/회원 adapter에서 신뢰할 수 있게 얻은 localMemberId ↔ subjectKey 결합을 사용해야 한다. 현재 회원 GET /users/me의 id 등 검증된 서버 응답을 통해 이 결합을 준비할 수 있다. Privy subject, 로컬 회원 ID, FE subjectKey가 같은 값이라고 가정하거나 이메일·기관명·요청 userId로 연결하지 않는다. 결합이 없거나 응답 회원이 다르거나 요청 도중 Session이 바뀌면 결과를 표시 상태에 반영하지 않는다. 이 검사는 FE 표시 주체의 정합성 검증이며 Backend 권한 근거는 계속 검증된 Bearer다.

adapter의 실행 순서는 현재 검증된 회원 결합과 Session 세대를 고정 → Bearer GET(AbortSignal 전달) → 같은 Session 유지/회원 ID 일치 확인 → ID·필수 필드·Enum·시간·서버 플래그 검증 → 위 변환 → FE validateQualification(value, subjectKey)다. 취소/이전 Session의 늦은 결과는 폐기한다.
HTTP401은 인증 안내, 403 USER_REGISTRATION_REQUIRED는 가입 안내, 503 AUTH_PROVIDER_UNAVAILABLE와500 INTERNAL_ERROR는 기존 오류/재시도 처리로 연결한다. 오류를 none/completed로 성공 변환하거나 배지·Session 권한을 임의 부여하지 않는다. 미지 Enum/잘못된 필드는 조회 오류로 처리하며 기존 FE unknown/error 차단 동작을 사용한다.

### 15.3 검토·구현·실제 연동 경계

- 확인된 근거: 사용자 제품/조회 방식 채택, Backend wire의 기존 ID·시간·envelope 유지, 현재 FE 표시 port 소스와 위 필드별 변환 가능성 대조.
- 이번 수정은 문서/계약 기록이다. FE adapter·Session 회원 결합·화면 코드를 이 Backend Feature에 추가하지 않는다.
- FE 담당자의 실제 합의/승인 기록은 아직 없다. 이 절을 FE 승인으로 표시하지 않는다. BE2 최신 정합성 재검토는 PR #154에서 요청한다.
- 실제 FE adapter를 연결할 때 검증된 회원 결합, 정상/미등록/만료/미래·타 회원 응답·잘못된 ID/필드·오류·요청 취소/Session 전환과 반환 시점의 기간 경계를 확인한다. 실제 Privy/FE 사용자 흐름은 #30/#31, FE #65/#68의 완료 전 검증 조건으로 유지한다.
- 기관 상태 GET은 본인 전용이다. 다른 작성자의 배지를 본인의 응답으로 채우지 않는다. 작성자 표시/기관 안건 업무의 실제 조회·최종 권한 연결은 각 관련 Issue에서 처리한다.
- #75는 지정된 관리자 등록 도구와 공통 회원 잠금으로 상태를 준비한다. 기간 겹침1개 제한은 이 작성 경로에서 보장하며 임의 관리자 SQL까지 막는 DB exclusion 제약 추가/실제 시연 데이터 준비 완료를 뜻하지 않는다.
- PR #154의 3731c1b 검증: Java17 전체224개/실패0/오류0/skip0·build 성공, 신규 JDBC10+실제 localhost 서버 LOGIN HTTP5, 실제 원격 SELECT-only 감사3. 이는 실제 provider/FE 연결 완료와 구분한다. 문서 보완 commit의 diff·검증·CI·BE2 리뷰는 PR에서 갱신한다.

## 16. #21 공유 발급·게스트 source port 계약 (2026-10-08)

### 16.1 사용자 결정과 wire

사용자가 GET /api/v1/posts/{postId}/share-link 및 /shared/posts/{postId}?token=... 주소, X-Post-Share-Token 헤더, DB 미저장 서버 서명, 발급 후7일·기간 내 재사용·새 발급 뒤 기존 링크 유지 추천을 채택했다. 기본200 data는 postId/shareUrl이며 회원 토큰이나 개인정보를 담지 않는다. 범위/JSON/오류의 정본은 API §1.4·§5.7이다. 경로는 가입 완료 회원의 공개 게시물 링크 발급이며 소유권/이웃 자격을 추가 발급 조건으로 만들지 않는다.

HMAC-SHA256 전용 토큰은 v1.postId.issuedAt.expiresAt.nonce.signature 형식이다. ID는 JSON 안전 정수, 시각은 Unix 초, expiresAt=issuedAt+604800, nonce는16bytes의 Base64url이다. 서명은 고정 domain prefix와 payload 전체에 적용하며 Privy token/다른 서명 protocol과 혼용하지 않는다. 형식/길이/정규 Base64url·서명·미래 발급·만료를 검증하며 서명 비교는 Java17 MessageDigest.isEqual을 사용한다. 만료시각 이상에서는 거부한다. 원문을 로그/DB에 저장하거나 임의 fallback key를 만들지 않는다.

무토큰·변조/만료·미지원 guest 행동은401 UNAUTHORIZED, 유효한 다른 게시물 토큰은403 SHARE_SCOPE_MISMATCH, 없는/삭제 원본은404 POST_NOT_FOUND다. 질문 초안의 일반 FORBIDDEN 이름은 기존 공유 코드로 정정했고 새 공통 오류 코드를 만들지 않는다. 무효 Bearer는 기존 인증 filter의401로 처리하며 공유 토큰으로 자동 게스트 전환하지 않는다.

### 16.2 공통 source port와 소비 규약

공통 인터페이스는 backend/src/main/java/com/discushion/contracts/share/SharedPostAccess.java다.

| port | 호출자의 조건 | 결과·의미 |
| --- | --- | --- |
| guestForRead(resolvedPostId, action) | 동일 실제 JDBC transaction, 서버가 결정한 action 및 원본 postId | guest context는 게시물 ID/만료시각만 제공. 실제 공개 상태를 매 호출 조회 |
| guestForWrite(resolvedPostId, action) | 동일 writable JDBC transaction, CREATE_COMMENT/CREATE_REPLY | PostContextReader.findForUpdate로 게시물/투표 잠금 후 현재 공개 상태와 만료 재검사 |
| 인증된 회원 요청의 Optional.empty() | 호출자가 회원 인증·가입/지역/소유권 경로로 처리 | 회원 권한 승인이라는 뜻이 아니며 미가입/미완료/무자격을 guest 예외로 바꾸지 않음 |

게스트는 대상 상세·요약·공개 집계·댓글 조회·댓글/답글 생성 범위만 허용한다. 메인/목록/지도/개인 기록·반응/평가/투표/북마크·재발급은 허용하지 않는다. 헤더1개에서 토큰을 검증하고 실제 대상과 비교하며 요청 userId/역할은 받지 않는다.
댓글/답글은 실제 원본으로 게시물 귀속을 조회하고 게시물→투표→댓글 등의 기존 잠금/보존 규칙을 유지한다. 허용 context를 장기 캐시하거나 다른 transaction/원본에 재사용하지 않는다. 댓글 실제 저장 직전의 대상/부모·제약·권한 재검사는 #22의 완료 조건이다.
공개 참여 조회는 ParticipationSnapshotReader의 viewer를 비우고 개인 선택/반응/북마크를 반환하지 않는다. 이 port는 상세/집계 DTO를 대신 구현하지 않는다. 실제 게시물 adapter는 BE2, 실제 참여/댓글 adapter는 각 담당자 책임이다. 테스트용 source를 운영 bean으로 등록하지 않는다.
PostContextReader 실제 bean이 준비되지 않으면 발급/검증은 안전한 INTERNAL_ERROR로 실패한다. 운영 테스트 fallback으로 우회하지 않는다. 공통 port의 최신 소비/transaction 정합성은 BE2 리뷰 후 back/develop에 통합하고 다른 Issue는 통합된 규약으로 사용한다.

### 16.3 FE·환경과 실제 확인

FE front/develop df7b4003454f385d62d9d953f38561ade5a0f599의 frontend/src/features/share/service.ts는 copy(postId:string):Promise<void> 표시 port이며 아직 wire adapter가 아니다. 최신 fetch 후 기존 대조 기준과 공유 port 변경이 없음을 확인했다. 회원 adapter는 양의 안전 정수 postId로 발급 GET을 호출하고 응답 postId를 같은 대상으로 대조한 뒤 shareUrl을 복사한다. 수신 화면은 기존 공유 경로의 token을 헤더로 전달한다. 게스트의 받은 URL 복사는 API 발급과 구분한다. 오류를 유효 guest/member 상태로 변환하지 않으며 returnTo/원 화면 복귀 규칙과 행동 자동 실행 금지는 유지한다.
실제 FE 담당자의 승인·adapter 연결·브라우저 사용자 흐름은 미확인이다. 이번 문서 대조/사용자 결정은 FE 확인자 승인이나 실제 연동 성공을 뜻하지 않는다. #30/#31 및 FE 연동 Issue에서 이 경계를 해소한다.

BE2 준비값은 SHARE_TOKEN_SIGNING_KEY(32bytes 이상 전용 무작위 키의 Base64 secret), PUBLIC_WEB_BASE_URL(프론트 주소)다. 실제 secret·.env·배포 설정 변경은 이번 구현 범위에 없다. 같은 키를 재시작/배포에도 보존하고 회전/복구는 기존 링크의 남은7일 검증과 함께 조율한다. DB Migration/GRANT/RLS 변경은 없다.
2026-10-08 14:43 KST 검증: 기준 back/develop 3c4b6aaf85920b646105849f5386172067927760 + back/feature/21-share 미커밋 구현에서 Java17 `gradlew.bat --no-daemon test build --console=plain --rerun-tasks` 실행. **전체241개/실패0/오류0/skip0·build 성공**이며 신규 공유17개와 실제 원격 Supabase SELECT-only 감사3개를 포함한다. 공유17개는 정책5·토큰6·실제 localhost 서버 LOGIN HTTP6으로, HMAC 변조/만료/재사용, 공통 Bearer filter, 실제 JDBC 게시물 읽기/잠금, 잠금 대기 중 만료·삭제, 회원 무권한 우회 거부 및 게스트 회원 기능 차단을 확인했다.
게시물 JDBC source와 소비 endpoint는 src/test 전용 fixture이며 실제 BE2 adapter·댓글 저장 구현·FE 연결 완료를 뜻하지 않는다. 운영 JAR에 공유 test fixture 클래스가 없음을 확인했다. 원격 감사는 TLS·Schema/Migration·공개 역할 접근 차단의 SELECT-only 검사이며 실제 원격 서버 LOGIN 사용자 흐름 검증을 대신하지 않는다. 최신 origin/back/develop은 위 기준과 동일하고 diff 공백 검사를 통과했다. API/권한·공통 port 변경이므로 최종 PR에는 상대 리뷰 필요로 분류하고 BE2의 최신 정합성 승인을 받는다. commit·push·PR 및 실제 연결은 별도 진행 조건으로 남긴다.

## 17. #22 댓글·답글 구현 계약과 권한 보완 (2026-10-08)

### 17.1 사용자 결정과 범위

사용자가 기존 댓글 GET/POST·답글 POST 및 DTO/201, 기본 좋아요순·부모20개/최대100·동률 시각/ID, 답글 전체 오래된 순과 페이지 커서를 채택했다. 권한 보완도 이번 Feature에 포함하도록 허용했으며 실제 Supabase 적용은 BE2와 조율한다. 금칙어는 사용자가 과도하지 않은 초기 목록 선정을 위임해 씨발·개새끼·병신·좆같4개로 최소화했다. 모두 literal 포함 검사이며 초성/띄어쓰기 변형·AI·관리 UI는 추가하지 않는다. 최신 코드·wire의 정본은 API §6.1이다.

회원은 검증된 현재 회원의 가입 완료와 이웃 완료 지역으로 작성한다. 기관 자격·기본 활동 지역만으로 작성 자격을 만들지 않는다. 공유 게스트는 #21의 특정 게시물·현재 공개/삭제 상태·서명/7일·동일 transaction·posts/polls 잠금을 사용하며 마지막 저장 단계에서 만료를 다시 검사한다. 회원 쓰기는 users→posts→polls 순으로 잠그고 새 자격을 조회한다. 답글은 실제 path 댓글의 postId/원 부모를 읽고 같은 대화의 원 부모 또는 답글만 지목한다. 댓글 수정/삭제가 없으므로 행 갱신 권한·추가 댓글 행 잠금은 제공하지 않으며, 보존/원본 수정 경로도 같은 게시물 잠금 규약을 따라야 한다.

생략 가능한 replyToCommentId를 보내면 양의 JSON 안전 정수여야 하고 null/문자열/소수는400이다. 생략하면 path의 실제 대상을 지목한다. 작성자/배지/userId/parentCommentId/postId 등 추가 필드는 거부한다. 공개 작성자 이름·현재 기관 유효 배지는 실제 profiles/institution_credentials에서 조회하며, replyTo.displayName은 작성 시 서버가 조회한 지목 대상 이름을 저장한 표시값이다. 게스트는 정확히 게스트/배지false, 회원별 myEvaluation을 생략한다.

읽기는 같은 read-only REPEATABLE_READ transaction의 공개 원본·댓글/평가/작성자 스냅샷을 사용한다. LIKES는 부모 평가의 LIKE 관계 수만 반영하고 답글 좋아요를 합산하지 않는다. parent 쿼리는 size+1로 제한하고 선택된 부모들의 답글/작성자/평가를 별도 batch 쿼리로 읽어 댓글별 N+1 요청을 만들지 않는다. cursor는 해당 postId/sort/마지막 부모 위치에 결합하고 다음 요청에서 재검증한다. 인가 근거가 아니며 페이지 전체 snapshot·좋아요 변경 중 완전한 중복/누락 방지를 보장하지 않는다. 새로고침 시 처음부터 조회한다. 모든 답글 반환의 데이터 규모 한계는 실제 데이터 연결 때 확인하고 임의로 답글을 잘라 전체라고 표시하지 않는다.

사용자 #5 건너뜀 지시를 유지해 activity_events/+1은 구현하지 않았다. 기존 제품 규칙을 삭제하지 않고 해당 미완료를 유지한다. 회원 댓글 관계 자체는 참여 기록 원본이며 게스트 자동 회원 이관은 없다. 별도 POST 멱등 키 계약이 없으므로 성공 POST마다 새 원본이 생성된다. FE 전송 중 중복 클릭 차단·응답 유실 후 목록 확인/명시적 재시도는 실제 연결 검증 대상이다.

### 17.2 서버 권한과 Migration

신규 20261008070000_allow_comment_reads_and_creation.sql은 기존 적용6파일을 유지하고 comments SELECT/INSERT 및 comment_evaluations SELECT만 추가한다. RLS 활성화/서버 역할 전용 정책3개, public/anon/authenticated/service_role의 접근 차단과 DDL/평가 쓰기·댓글 UPDATE/DELETE 거부를 유지한다. 기존 identity 댓글 ID 생성은 테이블 INSERT로 시험하며 직접 nextval/setval 시퀀스 권한은 확대하지 않는다. 서버 권한 회귀 기대값은 로컬14테이블·정책30개로 갱신하고 Supabase 감사의 원격12테이블·27정책/이력6개 기대값은 그대로 유지한다. 신규 권한은 원격 미적용이다. 실제 Schema 컬럼/FK·보존 구조 변경은 없다.

DB 권한은 서버 기능의 작업 범위이며 회원별 권한을 대신하지 않는다. 이번 변경은 사용자 승인 범위 내 공통 권한/API 영향이므로 PR에서 BE2 최신 정합성 승인을 받아야 한다. 로컬 검증만으로 공유 DB rollout/실제 서버 환경 완료로 기록하지 않는다.

### 17.3 FE 대조와 후속

대조 기준 front/develop 5fc0bc755ae2205540ec4d1cc619d983b0f47eca의 frontend/src/features/comment/model.ts·service.ts·CommentPanel.tsx. 최초 대조 e98a20a 이후 최신 fetch에서도 해당 댓글 파일 변경이 없음을 확인했다. CommentDisplay는 문자열 ID·작성시각 label/order·평면 authorName을 사용하는 표시 port이며 Backend wire DTO가 아니다. adapter는 JSON 안전 정수 id/postId/parentCommentId/replyTo.commentId를 검증 후 문자열로 변환하고 createdAt을 표시 label/order로 변환하며 author.displayName/replyTo.displayName을 연결한다. Backend data의 각 부모/replies를 CommentThread로 분리한다. myEvaluation/LIKE·DISLIKE 수와 기관 배지 표시는 서버 응답을 사용하고 FE 로컬 성공 상태만으로 DB 저장/자격을 승인하지 않는다.

현재 FE ReplyTarget/CommentReplyInput은 parentCommentId와 targetAuthorName만 보존해 기존 답글을 고유하게 지목할 수 없다. 통합 시 FE가 실제 targetCommentId를 보존하도록 자기 port/화면을 보완하고 그 ID를 API path 또는 replyToCommentId로 전달해야 한다. 이름으로 대상 ID를 추정하거나 targetAuthorName을 서버 권한 근거/요청 필드로 보내지 않는다. 서버는 실제 원 부모와 이름을 결정한다. FE list port도 정렬·커서/다음 페이지 상태를 연결해야 한다. 이번 Feature에서 FE 코드를 변경하거나 FE 확인자 승인을 대신하지 않는다.

BE2는 실제 PostContextReader의 동일 DataSource/transaction·posts→polls 잠금 및 삭제/수정 경합 규약을 연결한다. #22는 테스트 전용 JDBC post source로 독립 검증하며 실제 BE2 adapter/Privy/FE 연결 완료를 뜻하지 않는다. FE 실제 사용자 흐름은 사용자 결정대로 통합 #30/#31에서 해소한다. 게시물 초기 댓글 소비도 이 부모/replies·meta 계약으로 연결하되 BE2 상세 구현을 임의 변경하지 않는다. 실제 Supabase 권한 적용은 BE2 조율/승인 후 시행하고 원격 감사 기대값은 적용 이력과 함께 갱신한다.

### 17.4 독립 검증 결과와 현재 상태

기준 back/develop 04d60fa693c5fb1c0dbbe0d70f9826df351c938a(PR #162 병합), Feature back/feature/22-comments의 미커밋 변경을 검증했다.

- 2026-10-08 15:55 KST Java17 `gradlew.bat --no-daemon test build --console=plain --rerun-tasks`: 전체261개/실패0/오류0/skip0·build 성공. 신규 댓글20개(입력3·커서/사전4·실제 localhost 서버 LOGIN HTTP13), 기존 권한 회귀8개 및 실제 원격 Supabase SELECT-only 감사3개 포함.
- 최종 커서 anchor(실제 같은 게시물의 원 부모·작성시각) 검증 및 NUL 입력 거부 보완 후 15:58 KST 관련28개(댓글20+권한8)/실패0/오류0/skip0·build 재통과. 해당 최종 수정 뒤 전체261개를 다시 실행한 것으로 표시하지 않는다.
- 실제 production 댓글 Controller/JDBC store와 공통 인증·HMAC 공유 guard를 native PostgreSQL의 discushion_server LOGIN으로 실행했다. 회원/게스트 생성·조회, 미가입/미완료/타지역·무효 Bearer·다른 공유 원본 거부, 답글의 답글 평탄화/다른 대화 지목 거부, 부모 정렬·모든 답글·커서, 최신 이름/기관 만료 배지, 응답 실패 rollback, 실제 posts 잠금 대기 중 삭제/만료 및 users 잠금 대기 중 지역 자격 철회 거부를 확인했다. PostContextReader만 test-only JDBC source이며 실제 BE2 adapter 연결 완료는 아니다.
- 댓글 identity ID 생성 성공과 직접 시퀀스 접근·댓글 UPDATE/DELETE·평가 INSERT·DDL/TRUNCATE 거부를 확인했다. 신규 Migration의 로컬 재실행과 Schema SQL128개, API 역할 이름 모의 격리9개(3역할×Schema/테이블/시퀀스) 통과. 역할 모의는 rollback하며 실제 Supabase 역할 시험으로 표시하지 않는다. 원격 감사3개는 별도 실제 TLS/Schema/Migration/공개 역할 SELECT-only 확인이다.
- 초기 테스트 fixture의 verified_at 컬럼 및 로컬 anon 역할 이름 부재 오류를 수정한 뒤 통과했다. 테스트 결과를 위해 실제 Schema·실제 원격 계정 검사 기준을 완화하지 않았다.
- 운영 JAR의 댓글 test fixture0, diff 공백 검사 통과. 로컬 새 Migration 적용 외 원격 DB/배포/비밀 설정/FE 변경은 없다. 실제 기본6개 원격 Migration과 로컬 추가1개 차이는 승인 전 rollout 대기로 명시하며 원격 이력 기대값을 미리 바꾸지 않았다.

변경은 Feature 작업 트리에 보존한다. 이번 #22 commit/push/PR·병합은 아직 수행하지 않았으며 이후 요청 시 최종 diff·최신 base·필수 CI·BE2 상대 리뷰 조건을 확인한다. 실제 BE2 adapter·권한 rollout·FE/Privy 연동과 사용자 지시로 미구현인 #5 활동 횟수는 후속으로 유지하고 #22 전체 완료로 기록하지 않는다.

## 18. #23 독립 반응 등록·취소·집계 계약 (2026-10-08)

사용자가 기존 PUT/DELETE와 재시도 멱등·3종 독립 선택·200 count/본인 선택 응답 및 필요한 최소 DB 권한 추가를 채택했다. 정본은 API §6.2다. #5 활동 횟수 건너뜀 지시를 유지하며 원본 반응 관계는 실제 DB에 기록하지만 activity_events/+1은 구현하지 않는다. 제품 규칙 자체를 삭제하거나 미구현을 완료로 표시하지 않는다.

초기 독립 착수 기준은 back/develop 04d60fa693c5fb1c0dbbe0d70f9826df351c938a이고 당시 #166 댓글 PR은 미병합이었다. back/feature/23-reactions는 이 기준에서 별도로 시작해 #22 로컬 DB를 보존하고 별도 PostgreSQL cluster에 기준6개 Migration만 적용한 기본241개/실패0/오류0/skip0·build 및 원격 SELECT-only3개 통과를 확인했다. 현재는 #166이 병합된 bf98ecd11a73b6b3790e5e2707182e9b4b406e98을 반영했으며 최신 통합 상태는 §18.4를 따른다.

### 18.1 실행·권한 규약

현재 가입 완료·이웃 완료 지역만 회원 guard로 검사한다. 공유 게스트는 반응을 쓰지 못하고 유효 공유 토큰도 회원 자격을 대체하지 않는다. 같은 transaction에서 users→posts→polls 잠금을 사용하고 현재 공개/삭제 상태·지역을 검증한다. 본인 ID는 검증된 서버 주체만 사용한다. PUT의 기존 복합 PK와 ON CONFLICT DO NOTHING은 중복 등록/최초 created_at 덮어쓰기를 막는다. DELETE는 본인의 해당 유형만 취소하며 다른 유형·다른 회원을 건드리지 않는다. 집계/본인 상태는 같은 post_reactions 원본을 한 SQL로 조회한다.

신규 20261008080000_allow_reaction_reads_and_transitions.sql은 post_reactions SELECT/INSERT/DELETE·역할 전용 정책3개만 추가한다. UPDATE·직접 시퀀스·활동/댓글/다른 테이블 권한은 추가하지 않는다. 초기 독립 Feature 로컬은13테이블/30정책이었고, #166 반영 후 로컬 합계는15테이블/33정책이다. 원격은 적용된 기존12테이블/27정책·이력6개를 유지하며 원격 미적용을 숨기거나 감사 기대값을 미리 바꾸지 않는다. BE2 최신 리뷰 및 실제 적용 조율이 필요하다.

#166이 먼저 병합되면 #23 최종 PR 전에 최신 base를 반영하고 댓글 권한2테이블/3정책과 반응 권한1테이블/3정책을 모두 유지해야 한다(공통 합계15테이블/33정책). 반대 순서도 같은 원칙이다. ServerRuntimePermissionsIntegrationTests·API 역할 SQL·API/검토표 문서의 겹치는 변경은 한쪽으로 버리지 않고 실제 합계를 검증한다. 실제 공유 DB 적용된 파일은 수정하지 않는다.

### 18.2 FE·공통 adapter와 미완료 경계

현재 FE frontend/src/features/reaction/model.ts·service.ts·ReactionControls.tsx를 대조했다. ReactionSnapshot은 counts/selected/total 표시 port다. Backend의 data.postId를 요청 대상과 대조한 뒤 JSON 안전 정수 ID를 FE 문자열 ID와 연결하고 reactionCounts.EMPATHY/NEEDED/CURIOUS 및 total·myReactions를 변환한다. 본인 선택을 타 회원에게 캐시/표시하지 않는다. set은 selected를 PUT/DELETE로 변환하고 body 없이 보내며 오류를 성공 선택 상태로 바꾸지 않는다.

초기 get은 기존 상세 조회의 공개 집계/회원 myState와 연결해야 한다. 새 GET 반응 API를 임의로 만들지 않는다. 실제 ParticipationSnapshotReader batch adapter의 반응 부분이 같은 post_reactions 원본을 사용하도록 연결·검증해야 하며, 아직 없는 댓글/표/북마크 값을0으로 채우는 운영 fallback bean은 만들지 않는다. 이 adapter 전체 완료를 현재 반응 저장/집계 테스트 통과로 대신하지 않는다. 실제 PostContextReader/게시물 삭제·수정 경합의 통합은 BE2와 확인한다. FE/Privy 연결은 사용자 지시에 따라 통합 #30/#31에서 확인하며 문서 대조는 FE 담당자 승인이나 실제 연동 성공을 의미하지 않는다.

현재 변경은 BE1 API·반응 구현·필요 권한 Migration과 검증 준비다. 실제 원격 권한 적용·adapter/FE 연결·#5 활동은 미완료로 유지하고 #23 전체 완료로 기록하지 않는다. commit/push/PR은 이번 Issue의 별도 요청 때 수행한다.

### 18.3 #23 독립 검증 결과 (2026-10-08)

기준 back/develop 04d60fa693c5fb1c0dbbe0d70f9826df351c938a,Feature back/feature/23-reactions의 미커밋 변경을 검증했다. FE 대조 기준은 front/develop 5fc0bc755ae2205540ec4d1cc619d983b0f47eca다.

- 16:27 KST Java17 gradlew.bat --no-daemon test build --console=plain --rerun-tasks: 전체250개/실패0/오류0/skip0·build 성공. 당시 신규 입력2/실제 서버 LOGIN HTTP7 및 기존 권한8, 실제 원격 Supabase SELECT-only 감사3개 포함.
- DB 집계 읽기 실패 rollback·users 잠금 중 자격 철회 검사2개를 추가한 뒤 16:28 KST 관련19개(입력2/실제 HTTP9/권한8) 전부 통과·실패0/오류0/skip0·build 재통과. 최종 추가 테스트를 포함한 전체252개를 재실행한 것으로 표시하지 않는다. 운영 코드에는 이 두 테스트 추가 시 변경이 없다.
- production 반응 Controller/store·공통 서명 filter·실제 localhost discushion_server LOGIN을 사용했다. PostContextReader만 test-only JDBC source다. 세 반응 독립성·중복 PUT 최초 시각 유지·중복 DELETE/재등록·다른 사용자/유형 보존·전체 수와 본인 선택 분리, 동시12개 등록, 삭제 원본/게스트/미가입/미완료/타지역/무효 Bearer 거부를 확인했다.
- 실제 posts 잠금 대기 중 삭제와 users 잠금 중 지역 자격 철회는 저장 전에 거부했다. 실제 SQL 집계 실패의500·DB 원문 비노출·새 관계 rollback, UPDATE/TRUNCATE/DDL 및 미병합 댓글 읽기 거부를 확인했다. 테스트용 실패 정책/함수는 localhost fixture에서만 생성하고 제거하며 운영 Migration에 넣지 않는다.
- 추가 Migration의 재실행, Schema SQL128개, API 역할 이름 모의 격리9개 통과. 운영 JAR의 반응 테스트 fixture0, diff 공백 검사 통과. 실제 원격 감사3개는 기존6개 Migration/12테이블 권한·27정책 및 TLS/공개 역할 SELECT-only 감사다. 신규 반응 권한 적용·원격 runtime 사용자 흐름 완료를 뜻하지 않는다.
- 최신 origin/back/develop은 동일하다. #166 댓글 변경/데이터/브랜치를 보존했고 이 Feature에 합치지 않았다. 별도 테스트 cluster의 역할은 NOLOGIN/password null로 복구하고 이번에 시작한 PostgreSQL을 종료했다. 원격 DB/배포/실제 secret·FE 코드를 변경하지 않았다.

실제 PostContextReader/ParticipationSnapshotReader 소비 연결·공유 DB 권한 rollout·FE/Privy·#5 활동 기록은 후속이다. 변경은 작업 트리에 보존하고 이번 Issue commit/push/PR·병합은 아직 수행하지 않았다. PR 분류는 API/권한·Migration 영향에 따른 상대 리뷰 필요이며 최신 BE2 승인/필수 CI/최신 base·충돌/미해결 리뷰 없음 조건을 적용한다.

### 18.4 #166 병합 기준 반영·충돌 해결 (2026-10-08)

사용자가 최신 develop pull 및 #23 반영/수정을 요청했다. #166은 BE2 최신 승인 후 병합되어 origin/back/develop bf98ecd11a73b6b3790e5e2707182e9b4b406e98에 통합된 상태였다. 작업 트리 clean을 확인한 뒤 로컬 back/develop을 git pull --ff-only로 갱신하고 #23 Feature에 origin/back/develop을 merge했다. 직접 develop/main push·force/rebase·원격 DB 적용은 수행하지 않는다.

충돌3파일은 각 변경의 의미를 대조해 다음과 같이 해결했다.
- ServerRuntimePermissionsIntegrationTests: 기존 댓글 SELECT/INSERT·평가 SELECT를 보존하고 반응 SELECT/INSERT/DELETE를 더해15테이블·33정책으로 검증한다. 동일하게30으로 자동 합쳐지는 정책 수를 그대로 두지 않았다.
- api-role-isolation.sql: 두 추가 Migration을 시간 순서로 모두 포함하고 공개 API 역할 접근 차단 검사 유지.
- 계약 검토표: #166 댓글 §17을 보존하고 반응을 §18로 옮겼다. 댓글/반응 API 작성자는 모두 BE1로 표시하고 기존 정책·실제 연결/원격 적용/FE·#5 후속을 유지했다.

반응 HTTP 권한 회귀의 이전 '미병합 댓글 SELECT 거부' 기대는 새 기준과 충돌하므로 댓글 SELECT 허용을 확인하고, 여전히 미허용인 activity_events SELECT 거부로 갱신했다. 반응 UPDATE/TRUNCATE/DDL 거부는 그대로 유지한다. #166 댓글 운영 코드·테스트·Migration과 #23 반응 운영 코드·Migration은 변경하지 않았다. 최신 develop과 댓글 파일 차이가 없음을 확인했다.

검증은 같은 격리 #23 로컬 DB에 통합된 댓글 권한을 추가해8개 Migration·15테이블/33정책으로 실행한다. 원격은 적용된 기존6개/12테이블·27정책의 SELECT-only 감사를 유지하고 댓글/반응의 실제 공유 DB 적용은 BE2 조율 후로 남긴다. 통합 전체 test/build와 최종 PR diff·CI·BE2 최신 재검토 결과는 이어 기록한다. 기존 독립 검증 기록을 통합 전체 검증으로 대신하지 않는다.

통합 검증 완료 — 2026-10-08 17:19 KST: 기준 bf98ecd + 이번 #23 병합 해결 작업 트리에서 Java17 gradlew.bat --no-daemon test build --console=plain --rerun-tasks 실행. 전체272개/실패0/오류0/skip0·build 성공. 댓글20개·반응11개·통합 최소 권한 회귀8개·실제 Supabase SELECT-only 감사3개 포함. Schema SQL128개·모의 API 역할 격리9개 통과, 두 추가 Migration의 로컬 적용/재실행과 runtime 권한15테이블·33정책을 확인했다. 댓글·반응 test fixture의 운영 JAR 포함0, 미해결 충돌0·공백 검사 통과. 새 통합 commit/push는 기존 PR #167에 반영하고 최종 head에 대한 BE2 재검토/필수 CI를 확인한다. #166 승인이나 기존 #167 CI를 새 head의 승인/CI로 대신하지 않는다. 공유 DB 적용·실제 adapter/FE·#5 후속은 유지한다.

## 19. #13 서버 중계 사진 업로드·안전 삭제 결정과 검증 (2026-10-08)

**상태: 사용자 합의에 따라 구현했고 최신 back/develop 통합 검증을 마쳤다.** 이 절은 최초 검토안을 보존한 작업 결정 기록이다. 현재 정본은 API 명세 §13.9와 Migration `20261008071616_track_server_photo_uploads.sql`이다. 새 예약은 앱 API 상대 경로 PUT/RAW, 앱 origin에만 Privy Bearer 전달, DB 시계 기준 2시간 허용 기한, 파일별 단일 외부 쓰기와 영속 종료 추적을 사용한다. 기존 직접 업로드 행은 보수적으로 유지한다. 구현 완료는 GitHub 상대 승인·공유 DB 적용·배포·FE 실제 연동 완료를 뜻하지 않는다.

### 19.1 직접 업로드에서 확인하지 못한 보장

[공식 서명 업로드 안내](https://supabase.com/docs/reference/javascript/storage-from-createsigneduploadurl)는 URL의 2시간 유효성을 명시한다. 이는 발급 요청이 timeout된 뒤 공급자가 처리를 언제 마치는지, 만료 전에 시작한 전송이 언제 종료되는지를 보장하지 않는다. 이번 실제 시험은 삭제한 key가 같은 유효 URL의 PUT으로 다시 생성됨을 확인했다. 두 번 삭제·시간 경과·lease 만료를 업로드 종료 증거로 사용할 수 없다. 현 방식의 uploadsDrained=false·DELETE_PENDING 유지가 필요한 이유다.

### 19.2 합의된 구현 방식: FE → Spring → Supabase

Supabase 공개 사진 저장과 관리 키의 서버 보관을 유지하되 새 예약에서는 Supabase signed upload URL을 발급하지 않는다. FE는 사진 bytes를 Spring으로 보내고, Spring만 예약 key에 Storage 쓰기를 수행한다. 서버 중계만으로 모든 장애를 해결했다고 취급하지 않는다. 전송 종료의 확인이 없는 쓰기 시도는 영속 추적하고 최종 삭제를 보류한다.

| 접점 | 제안 | 기존 계약 대비 영향 |
| --- | --- | --- |
| 예약 | 기존 POST /api/v1/photo-uploads와 fileId·수량/용량·빈도 제한 유지. upload.url은 앱 API origin의 PUT /api/v1/photo-uploads/{fileId}/content, method=PUT·bodyMode=RAW | 업로드 대상은 Storage에서 앱 서버로 변경했고 신규 endpoint를 구현했다. 정본은 API 명세 §13.9 |
| 업로드 인증 | 공통 Privy Bearer 검증과 현재 가입 완료 회원·소유권 검사. 반환 headers에는 관리 키·Privy 토큰을 넣지 않음. FE가 앱 origin에만 현재 access token을 전달 | 기존 Storage 전송의 Bearer 금지/별도 client 규칙을 앱 서버 전송 규칙과 다시 합의해야 함. Storage 주소에 Bearer를 보내지 않음 |
| 권한 만료 | 외부 URL 발급 없이 예약 DB에 서버 업로드 허용 종료시각을 기록한다. 유효 시간은 DB 시계 기준 2시간이다. 발급·PUT 허용 판정은 같은 DB 시간 기준을 사용 | 공급자 발급 지연 여유 설정을 제거했다. 취소/24시간 정리/회원 자격은 TTL과 별도로 최종 재검사 |
| 전송 입력 | JPG/PNG bytes를 최대10,000,000 bytes로 제한해 읽고 내용 검사. 명세상 게시물 합계·10장 제한과 최초 24시간 기준 유지 | FE raw body 전송 지원 및 실제 배포의 요청 크기·메모리·동시 전송 한도를 #30에서 확인. 10MB 수용을 가정하지 않음 |
| 완료·조회·취소 | 기존 complete/GET/DELETE와 photoFileIds·202 삭제 대기·deletionCompleted 유지. 취소 이후 새 쓰기 시도는 거부 | 삭제 완료의 의미를 완화하지 않음. 완료 호출/검증 재시도로 최초 보관 기한을 늘리지 않음 |

### 19.3 영속 쓰기 추적과 삭제 장벽

내부 상태는 Migration `20261008071616_track_server_photo_uploads.sql`에 추가했다. 적용된 Migration 파일은 수정하지 않았다. 현재 Migration은 Feature PR에 포함하며, 공유 DB 적용은 상대 검토·승인과 BE2 조율 이후 진행한다.

- 파일별 전송 방식 구분이 필요하다. 기존 행은 직접 업로드 또는 안전성 미확인으로 보존하고 서버 중계 완료 행으로 임의 backfill하지 않는다. 새 서버 중계 예약만 외부 업로드 권한이 발급되지 않았다는 사실을 보장한다.
- 각 Storage 쓰기에 재사용하지 않는 시도 ID·파일/key·시작시각·종료 확인·불명확 결과를 영속 기록한다. users→media_files 잠금 아래 가입/소유권·허용기한·UPLOADING·미완료 시도 여부를 확인하고, 쓰기 시작 기록을 commit한 뒤에만 외부 호출한다. 동일 key 동시 전송·중복 외부 쓰기는 허용하지 않는다.
- 외부 Storage 호출은 DB transaction 밖에서 수행한다. 잠금/lease가 만료돼도 살아 있던 작업자의 외부 쓰기는 늦게 진행될 수 있으므로, 시도 기록을 자동 종료하거나 새 쓰기로 대체하지 않는다. 예약 당시의 실제 key는 재사용하지 않는다.
- 의미가 확인된 공급자 응답으로 쓰기 종료가 확인되면 해당 시도의 종료를 commit한다. 성공 응답 유실·timeout·프로세스 장애·종료 commit 유실은 UNKNOWN 또는 미종료로 보존한다. 단순 object 존재/부재 조회나 HTTP client 취소로 종료를 확정하지 않는다. 모르는 시도의 자동 재전송도 금지한다.
- 취소/기한 만료/관계 제거는 현재처럼 같은 파일 잠금으로 DELETE_PENDING을 기록한다. 이후 새 쓰기 시작을 차단한다. 이미 시작한 작업자는 취소 뒤 파일을 쓸 수 있으므로 해당 시도가 끝나기 전 최종 삭제를 기록하지 않는다.
- 최종 삭제는 새 쓰기 차단·모든 쓰기 시도 종료 확인·활성 참조 없음이 충족된 파일만 가능하다. 이어 Storage 삭제와 실제 부재를 확인하고 최신 파일 잠금/삭제 claim 아래 같은 조건을 다시 검사해 DELETED를 저장한다. 늦은 작업자 결과는 새 쓰기 시작을 허용하지 않는다.
- 정상 전송의 종료가 확인된 새 서버 중계 파일은 위 절차로 최종 삭제할 수 있다. 결과 불명확 시도 및 기존 직접 업로드 파일은 종료 증거를 확보할 때까지 DELETE_PENDING을 유지한다. **장애가 있어도 반드시 유한 시간 안에 DELETED가 된다는 보장은 이 안에 없다.** 공급자 종료 확인 기능이 없는 경우 운영 조치/추가 계약이 필요하며, 삭제 대기만으로 #13 인수를 완료하지 않는다.

### 19.4 합의 및 검증 기준

BE1 확인: 새 시도 추적 Schema/권한/RLS, 기존 직접 업로드와의 구분, 잠금·종료 기록·삭제 장벽·장애 복구. FE 확인: 앱 origin 업로드 경로·Bearer 전달·RAW client·complete 호출·202/오류 처리. BE2 확인: 실제 배포의 10MB 요청 수용과 메모리/동시 전송 제한, 관리 키 서버 보관, 실제 Storage 성공/실패 의미.

합의 뒤 구현 검증은 정상 업로드→공개 조회→완료→취소→DELETED, 취소 후 PUT 차단, 업로드 중 취소 및 마지막 쓰기 종료 후 삭제, timeout/서버 재시작/종료 commit 유실의 삭제 대기 보존, lease 만료 뒤 늦은 작업자, 중복 PUT·타인 소유·회원 상태 변경, 최초24시간 경계, 기존 직접 업로드 행의 보수적 처리를 포함한다. 실제 Supabase와 격리 DB 검증을 실제 Privy OTP/FE 사용자 흐름과 구분해 기록한다. 공급자 응답 의미나 배포 제약이 확인되지 않으면 해당 완료 판정을 보류한다.

2026-10-08 KST 최신 통합 검증: 최신 `origin/back/develop` `ff922f1`을 Feature에 병합하고 #166 댓글·#167 반응 변경 및 세 권한 Migration과 함께 검증했다. Java17 `gradlew.bat --no-daemon test build --console=plain --rerun-tasks --max-workers=2 --offline` 결과 **287개 통과·실패0·오류0·skip0, build 성공(6분20초)**. 실제 Supabase SELECT-only 감사3개, 실제 Storage direct 회귀2개, 서버 중계 Storage2개, 서버 중계 JDBC 안전성9개가 포함됐다. 실제 사진 시험은 합성 회원과 격리된 서버 LOGIN으로 Spring PUT→Storage 저장→익명 공개 조회→완료·취소·DELETED 및 늦은 PUT 차단을 확인했고 10,000,000 bytes PNG도 통과했다. 실제 Privy OTP·FE 화면·배포 환경의10MB 수용 검증은 별도다.

깨끗한 localhost PostgreSQL에서 전체 Migration을 적용한 뒤 스키마 무결성133개, API 역할 격리9개, ERD 대조 27테이블/185컬럼·단일 FK47·복합 FK8을 통과했다. 시험용 댓글·반응 Migration은 별도 localhost 통합 DB에만 적용했으며 공유 Supabase에는 새 Migration을 적용하지 않았다. 테스트 Storage 파일 정리와 운영 JAR의 test fixture 비포함을 확인했다. 사용자 자격·공유 DB 적용·PHOTO 활성화·BE1 상대 승인·GitHub 필수 CI·FE 실제 연결은 남아 있다. 결과 불명확한 쓰기와 기존 직접 업로드 행은 종료 증거 전까지 삭제 대기로 유지한다.

## 20. #24 댓글·답글 평가 전환·취소 계약 (2026-10-08)

### 20.1 사용자 결정·원본·잠금

사용자가 PUT LIKE/DISLIKE·DELETE 취소 및 같은 desired state의 재시도 유지,200 commentId/myEvaluation/likeCount/dislikeCount, NONE↔null FE 변환과 필요한 평가 INSERT/UPDATE/DELETE 권한 보완을 채택했다. 정본은 API §6.3이다. #5 건너뜀을 유지해 activity_events/+1은 미구현이며 기존 제품 규칙을 삭제한 것으로 표시하지 않는다.

기준은 #166/#167이 병합된 back/develop ff922f18713689116fb1c0e933b882eeff77848f다. 이 트리는 이전 통합272개/skip0·build/원격3 검증 대상363e4d3과 동일함을 확인한 뒤 Feature back/feature/24-evaluation을 시작했다. 미커밋 변경은 없었고 별도 생성·전환으로 기존 Feature를 보존했다.

평가 쓰기는 실제 댓글의 post_id를 DB에서 읽고 현재 회원 users→posts/polls 순의 공통 잠금 규약을 사용한다. 가입 완료·이웃 완료 지역·공개/삭제·댓글 귀속을 저장 시 다시 검사한다. 해당 댓글 작성자가 게스트여도 자격 있는 회원은 평가할 수 있지만 요청자 게스트는 유효 공유 토큰이 있어도 평가할 수 없다. 기관 자격/기본 활동 지역·클라이언트 userId/postId/역할은 권한 근거가 아니다. 댓글 본문은 기존 SELECT로 확인하고 수정/삭제 권한·API를 만들지 않는다.

(comment_id,user_id) PK의 실제 현재 관계를 저장하며 다른 회원·다른 댓글의 평가는 보존한다. 동일 선택은 created_at/updated_at을 그대로 유지하고 반대 평가로 전환하면 기존 created_at을 보존한 채 updated_at을 갱신한다. DELETE는 그 회원의 대상 평가만 제거한다. 취소 후 재등록은 새 관계의 시각을 가지며 평가와 반응·투표를 혼합하지 않는다. 원본 SELECT 집계·본인 선택과 저장은 같은 transaction에서 처리하고 읽기/응답 실패도 rollback한다. 취소 뒤 참여 표시 제거와 다른 참여가 있을 때 게시물 카드 유지 등 실제 참여 목록 연결은 #27의 완료 조건이다.

댓글/답글 평가 수는 기존 댓글 GET의 같은 원본 집계로 제공한다. 답글의 LIKE는 부모 점수에 합산하지 않는다. 빈 본인 선택은 null, 회원 GET만 myEvaluation을 포함하며 타인의 선택과 게스트의 누락 필드를 본인 NONE으로 혼동하지 않는다.

### 20.2 Migration·권한·후속

신규20261008090000_allow_comment_evaluation_transitions.sql은 기존 comment_evaluations SELECT를 보존하고 INSERT/UPDATE/DELETE와 서버 전용 정책3개만 추가한다. 기존8개 파일/댓글 본문/다른 테이블/시퀀스/DDL 권한은 수정하지 않는다. 로컬 총15테이블·36정책, 원격 적용된 상태는 기존6개·12테이블/27정책이며 실제 Supabase 적용은 BE2 조율 후다. 원격 감사 기대값을 미리 바꾸거나 관리자 SELECT 감사를 실제 runtime CRUD·제품 권한 시험으로 표시하지 않는다.

기존 댓글 HTTP 테스트의 '평가 INSERT 거부'는 이번 기능에서 허용되므로 허용 확인으로 갱신하고 댓글 본문 UPDATE/DELETE·시퀀스·DDL 거부는 유지한다. 권한 회귀는 평가4작업과 전체36정책을 대조한다. API 역할 SQL은9번째 추가 Migration까지 포함해 공개 역할 접근 차단을 확인한다. 공통 권한/API 영향을 포함하므로 Feature PR에서 최신 BE2 정합성 승인·필수 CI·최신 base·충돌/미해결 리뷰 없음 조건을 확인한다.

### 20.3 FE 대조·실제 연결 경계

현재 front/develop의 frontend/src/features/comment/evaluation.ts·evaluation-service.ts 및 CommentPanel.tsx를 대조했다. FE EvaluationType은 NONE/LIKE/DISLIKE이고 실제 wire DTO와 별개다. set의 LIKE/DISLIKE를 PUT, NONE을 body 없는 DELETE로 변환하고 응답의 JSON 안전 정수 commentId를 문자열 표시 ID와 대조한다. myEvaluation=null은 NONE으로 변환하고 서버 count를 그대로 반영한다. FE의 낙관적 nextEvaluation 계산은 실제 저장/권한 완료를 보장하지 않으며 실패 복구·Session 전환·늦은 응답을 통합 때 검증한다.

EvaluationService.list는 기존 댓글 GET의 부모/replies에 있는 본인 평가/수로 연결해야 하며 새 GET 평가 API를 추가하지 않는다. 게스트는 회원 평가 list/set으로 우회하지 않고 공유 컨텍스트 댓글 GET의 공개 수만 사용한다. 실제 FE 확인자 승인·adapter/브라우저·Privy 흐름은 미확인이고 사용자 결정에 따라 #30/#31 통합에서 해소한다.

운영 코드는 실제 댓글/평가 JDBC 원본·공통 회원 guard를 사용하며 PostContextReader만 BE2 actual adapter 연결 대기다. test-only JDBC post source를 운영 bean으로 등록하지 않는다. 실제3유형 게시물/동일 DataSource·transaction/posts→polls 잠금과 삭제·수정 경합 연결, 공통 ParticipationSnapshotReader·개인 참여 기록의 현재 평가 관계 연결은 각 완료 조건으로 유지한다. 테스트 대체 구현 통과를 실제 source/FE 연결 또는 #24 전체 완료로 기록하지 않는다.

### 20.4 검증 결과와 현재 상태

2026-10-08 17:52 KST, 기준 ff922f18713689116fb1c0e933b882eeff77848f + back/feature/24-evaluation 미커밋 구현에서 Java17 gradlew.bat --no-daemon test build --console=plain --rerun-tasks 실행. 전체285개/실패0/오류0/skip0·build 성공이며 신규 평가13개(입력3/실제 localhost runtime LOGIN HTTP10), 기존 댓글20/반응11/서버 권한8 및 실제 원격 Supabase SELECT-only 감사3개를 포함한다. 앞선 평가·댓글·권한40개/build 검증 뒤 유효 공유 토큰·지역 자격 철회 검사를 추가하고 최종 전체를 다시 실행했다.

실제 production 평가 Controller/JDBC store·공통 인증/회원 guard, 기존 댓글 GET 및 실제 발급한 HMAC 공유 토큰을 사용했다. LIKE↔DISLIKE 상호배타·같은 PUT 시각 유지·중복 DELETE/재등록·다른 사용자/답글 보존·본인 평가/수 분리, 동시12요청의 단일 평가, 게스트/무효 Bearer/미가입·미완료/타지역·유효 공유 토큰 우회 거부를 확인했다. 답글 LIKE가 부모 정렬에 합산되지 않는 실제 댓글 조회도 검증했다.

실제 posts 잠금 대기 중 삭제와 users 잠금 대기 중 지역 자격 철회 거부, 집계 SQL 실패의 rollback/안전한500·DB 원문 비노출, runtime 평가 CRUD 허용 및 댓글 본문 UPDATE/DELETE·DDL/TRUNCATE 거부를 확인했다. PostContextReader는 src/test 전용 JDBC source이며 실제 BE2 adapter/3유형 통합 완료를 뜻하지 않는다. 실패 주입 policy/function은 native fixture에서만 만들고 제거한다.

신규 권한 Migration의 로컬 적용/재실행,15테이블·36정책 및 전체 Schema SQL128개/API 역할 이름 모의 격리9개 통과. 원격 감사3개는 기존6개 이력/180컬럼·55FK·27RLS/12테이블·27정책 및 TLS verify-full·공개 역할 차단을 SELECT-only로 확인했다. 로컬은9개 Migration이며 원격6개와의 차이는 승인 전 공유 DB rollout 대기다. 기대값을 완화하거나 실제 원격 DB를 변경하지 않았다. 운영 JAR 평가 test fixture0·diff 공백 오류0, 최신 origin/back/develop과 기준 동일을 확인했다. 로컬 임시 서버 역할은 NOLOGIN/password null로 복구하고 이번 PostgreSQL을 종료했다.

변경은 Feature 작업 트리에 보존한다. 이번 #24 commit/push/PR·병합은 아직 수행하지 않았다. 실제 BE2 source/공통 참여 기록·권한 rollout·FE/Privy·#5 활동은 후속으로 유지하며 #24 전체 완료로 표시하지 않는다. 이후 요청 시 API/권한 Migration 영향에 대한 최신 BE2 승인·필수 CI·최신 base·충돌/미해결 리뷰 없음 조건으로 PR 통합을 진행한다.

## #19 지도 계약 확정 (2026-10-08)

사용자가 FE viewport의 실제 지역 ID 집합을 서버에 전달하는 방식을 채택했다. API 정본 §5.2: regionIds 필수 CSV·centerRegionId 선택, 요청 순서 보존·빈 동 null·미등록 지역404. 반응 합계/created_at/id 내림차순으로 공개 안건/투표 대표 한 건을 계산한다. FE 경계/좌표/SDK 및 실제 지도 연동은 대기다. 사용자의 계약 채택을 실제 FE 확인으로 대신하지 않는다.

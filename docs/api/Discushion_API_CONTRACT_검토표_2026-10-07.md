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
| D07 | 실제 금칙어 사전 | 제품 확인 + BE2·BE1 | #22, 댓글 필터 구현 전 |
| D08 | 제목/본문/질문/옵션 길이·URL 검증·과거 투표 종료시각·중복 선택지·활동 일정 구조 | 제품 확인 + BE1·BE2·FE | #14·#16·#25, 각 입력 구현 전 |
| D09 | 사진 저장 파일 삭제 확정. 다른 관계의 물리 보존·소프트 삭제·투표 상태 필터는 미정 | BE1·BE2·FE | #3·#13·#16·#26·#27 |
| D10 | cursor 형식·size 기본/상한·기본 정렬·ID 동률 정렬·초기 댓글 수/답글 페이지 | BE1·BE2·FE | #9·#15·#17·#22·#27·#28, 목록 구현 전 |
| D11 | Region 계층·지도 원천·기관 정본 식별/seed·동시 유효 기관 인증 개수 | BE1·BE2, 기관 정책 제품 확인 | #3·#9·#12·#19·#29, Schema·권한 구현 전 |
| D12 | 공유 토큰 저장/서명·TTL·재발급·링크 원문 재사용 | BE2·BE1·FE | #21, 공유 구현 전 |
| D13 | Gemini 3.5 Flash-Lite 선택. 실제 API 모델 ID·생성/저장/재생성·재시도·짧은 원문은 확인/합의 필요 | BE2·BE1·FE | #74·#20·#30 |
| D14 | 이벤트 저장·원자성·생성 POST 멱등 키의 범위/TTL/payload 비교 | BE1·BE2 | #5 및 각 생성 이슈, 쓰기 구현 전 |
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
| A·BE1 | POST | /institution-verifications | #12 |
| B·BE2 | GET | /home | #18 |
| B·BE2 | GET | /map/dongs | #19 |
| B·BE2 | GET | /posts | #17 |
| B·BE2 | POST | /posts | #14 |
| B·BE2 | GET | /posts/{postId} | #15 |
| B·BE2 | PATCH | /posts/{postId} | #16 |
| B·BE2 | DELETE | /posts/{postId} | #16 |
| B·BE2 | GET | /posts/{postId}/summary | #20 |
| C·BE2 | GET | /posts/{postId}/share-link | #21 |
| C·BE2 | GET | /posts/{postId}/comments | #22 |
| C·BE2 | POST | /posts/{postId}/comments | #22 |
| C·BE2 | POST | /comments/{commentId}/replies | #22 |
| C·BE2 | PUT | /posts/{postId}/reactions/{reactionType} | #23 |
| C·BE2 | DELETE | /posts/{postId}/reactions/{reactionType} | #23 |
| C·BE2 | PUT | /comments/{commentId}/evaluation | #24 |
| C·BE2 | DELETE | /comments/{commentId}/evaluation | #24 |
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

- 같은 예약의 권한 재발급 endpoint는 만들지 않는다. 유효한 최초 URL의 전송 실패는 같은 key로 재시도하되 덮어쓰기는 금지한다. 성공 여부가 불분명하면 complete로 확인하고, 권한 만료/응답 유실은 이전 예약 취소 후 새 fileId/key로 예약한다.
- 초기 서버 예약 보호 기준: 회원당 UPLOADING/UNLINKED/DELETE_PENDING 20개·신고 합계100,000,000 bytes, 최근60초 새 예약20개. 삭제 예약은 실제 최종 정리 전 슬롯 반환으로 취급하지 않는다. users 잠금과 같은 DB transaction에서 제한 조회/예약을 원자 처리한다. 실 Storage 사용량 상한으로 주장하지 않는다.
- 게시물 10MB는 BE2 실행 기준 10,000,000 bytes로 정리한다. 영역 오류안409 PHOTO_UPLOAD_QUOTA_EXCEEDED/429 PHOTO_UPLOAD_RATE_LIMITED·Retry-After와 FE 재시도/삭제 상태 확인을 명시했다. 공통 오류 envelope나 인증 코드 registry는 변경하지 않았다.
- provider 호출 전 잠재 권한 만료 상한을 기록하고 commit 후 외부 호출한다. provider TTL/시각/timeout·응답 유실·프로세스 장애·상한 확장 저장·취소 재검사와 최종 삭제 시 진행 중 전송 종료/재생성 방어는 #13 실제 adapter 검증 조건이다. 확인 불가하면 DELETE_PENDING 유지이며 임의 시간이 안전성 보장이 아니다.
- FE가 확인할 endpoint/숫자ID/bytes/최종배열/Storage 직접 전송·오류/재시도·202/200/204 표를 API §13.7과 통합 지침서에 준비했다. 최신 FE 문서 대조는 합의 대기를 확인한 것이며 FE 승인·외부 연락·실제 연동 완료가 아니다.

이번 변경은 기존 문서 내용만 갱신한다. Java 기능·추가 Migration·FE 브랜치·Storage 프로젝트·비밀 변수는 변경하지 않는다. FE wire 확인과 계약 PR 통합 전 #13 의존 endpoint를 임의 구현하지 않는다. #74의 인증 잔여 계약/실제 환경 검증은 별도 유지한다.

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

#### 게시물 요약 — BE2 제공

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

인터페이스·test-only batch 대체 구현 및 guest/타회원 정보 격리·삭제 display 차단 회귀 검사를 준비했다. #15/#17/#18/#19는 집계 adapter 전체 완성을 기다리지 않고 규약/Schema 통합 후 본문·목록을 구현할 수 있다. #21~#29도 PostContext/요약 규약을 사용해 개별 검증한다. 실제 연결·DB 경합·서비스 실패 전파는 해당 기능 완료 조건이고 FE 사용자 흐름은 #30/#31에서 확인한다. CI·CODEOWNERS/필수 검사 설정은 GitHub 실제 실행/관리자 설정 완료와 구분한다.

# Discushion 프론트엔드 2인 개발 시작 전 합의사항 — 병렬 개발

> 적용 기준: 2026-10-07 (Asia/Seoul) · FE1 / FE2 공통 개발 합의
> 기존 파일의 내용을 갱신한다. 화면별 상세명세·새 담당 배분·실제 구현 완료 보고를 대신하지 않는다.

이 문서는 두 Frontend 개발자가 같은 Contract·공통 UI·디자인·상태·완료 정의를 사용하면서 서로 다른 화면군을 독립 개발하기 위한 규칙이다. 화면별 Task는 **프론트엔드 개발 상세지침서**, 실제 화면 담당은 **담당분배 문서**에서 확인한다. 이 문서에서 상세 Task를 복제하거나 FE1/FE2의 담당 화면을 새로 배정하지 않는다.

| 판단 대상 | 기준 문서·자료 |
| --- | --- |
| 제품 동작·권한·MVP·기능 ID | [PRD v10.2](../specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md), [기능명세서 v10.2](../specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md)의 최신 확정 정책·범위 대조·ID별 명세 |
| 기존 NON-MVP에서 복구된 FE 범위 | 위 제품 문서의 복구 대조표 및 최신 통합·FE 상세지침서의 17항목 + 검색/현재 위치 보조 2항목. 오래된 제외 표보다 최신 복구 기준 우선 |
| 사용자 이동·취소·복귀 | [최신 유저플로우](../specs/Discushion_유저플로우_와이어프레임기반_2026-10-06.md)의 현재 정책 본문. 접힌 과거 Prototype 기록은 실행 요구가 아님 |
| FE/BE 책임·통합 상태 | [MVP 백엔드·프론트엔드 통합지침서](../specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md) |
| FE 구현 범위·화면별 Task·상태·QA | [MVP 프론트엔드 개발 상세지침서](../frontend/Discushion_MVP_프론트엔드_개발_상세지침서.md), 특히 12·16·51~53·58절 |
| 시각적 외형·공통 규격 | [Figma KW 해커톤 디자인 / 최종디자인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1136-6698), [디자인기준 v2](../design/Discushion_최종디자인_디자인기준%20v2.md) |
| 상세 담당·기존 Page/공유 파일 Owner | [프론트엔드 2인 담당분배·병렬개발순서](./Discushion_프론트엔드_2인_담당분배_병렬개발순서.md)와 해당 Issue의 확인된 파일 Owner |
| Git·Issue·Branch·Commit·PR·Merge | [통합 Git/GitHub 협업전략](./Discushion_Git_GitHub_통합협업전략_2026-10-06.md) 우선, [FE Git/GitHub 협업전략](./Discushion_프론트엔드_Git_GitHub_협업전략_2026-10-06_v2.md) 및 [AGENTS.md](../../AGENTS.md) 함께 적용 |
| API·타입·실행 구조 | [API SPEC v2](../api/Discushion_API_SPEC_v2.md)의 현행/변경 전 계약 구분 + 실제 FE/BE 합의 + 현재 코드. 문서 존재는 Backend 구현 증거가 아님 |

판단 종류에 맞는 정본을 따른다. 제품 정책이 디자인의 과거 동작과 다르면 정책대로 UI를 수정한다. 현재 `front/develop`은 **현재 구현 상태의 기준**이며 최종 화면이나 MVP 범위의 상한이 아니다. 해결되지 않는 정책 충돌은 영향받는 부분만 확인 대기로 남기고 독립 가능한 작업을 계속한다. 기술 권장안·미확정 Route/DTO/Endpoint를 확정 정책으로 취급하지 않는다.

2026-10-07 확인: 최신 로컬 제품·Flow·통합·FE 상세지침서, 디자인기준 v2, 협업 문서와 AGENTS를 대조했다. `git fetch origin front/develop` 후 원격 FE `4f4f4d5f7b51991c0eef3fa13a33d09d7d77a06c`의 공통 Header·4개 하단 메뉴·API 도구·package scripts를 정적으로 확인했다. Figma 하단바 `1255:6597` metadata의 5개 메뉴를 직접 확인했으며 전 화면 시각 QA나 실행 검증은 수행하지 않았다. 독립 FE 복구목록 파일은 현재 저장소에 없어 제품·통합·FE 문서의 복구 대조를 사용했다. GitHub 보호 설정·Issue 현재 상태·실 API 동작을 확인했다는 뜻이 아니다.

## 1. 병렬 개발 원칙

**Figma 최종디자인에 존재하고 최신 FE 상세지침서에서 구현 대상으로 정의한 화면·상태·UI는 Backend 구현 여부와 무관하게 FE 구현 대상이다. BE 미구현은 FE Blocker가 아니다.** 명시적으로 제거한 자체 비밀번호·자체 가입 코드·증빙 제출 흐름은 제외한다.

`BE API 없음 → Mock/Contract 기반 FE 구현 → 실 API 연동 대기 → BE 준비 후 Integration`

`BE API 없음 → FE 작업 대기`로 해석하지 않는다. FE1과 FE2 모두 Route·Component·입력·클릭·상태 변경·실패/재시도·취소·뒤로가기·상태 보존까지 먼저 구현한다.

1. Issue별 범위·기능 ID·Figma Frame·Owner·완료 조건과 기존 코드부터 확인한다.
2. 공통 Component interface, Mock data shape, Route/복귀 맥락, Shared Type, Service interface를 먼저 합의한다. 상대 구현이나 PR 병합 자체를 시작 조건으로 삼지 않는다.
3. 합의 Contract는 같은 타입·호출·오류를 사용한다. 계약 미확정 부분은 실제 데이터 연결만 대기한다. 확정된 UI·Client State의 Mock 가정은 기록하고 서버 DTO로 승격하지 않는다.
4. 미구현 공통 UI는 합의 props·슬롯·콜백·단일 토큰을 쓰는 개발용 stub으로 검증할 수 있다. 기존 구현이 있으면 우선 재사용하며 별도 제품용 Header/Nav/Theme를 만들지 않는다.
5. 같은 Page/Screen과 공유 파일은 한 Issue 담당자만 수정한다. 공통 파일의 변경·병합 순서는 해당 파일만 조율하며 전체 Track 대기로 확대하지 않는다.
6. 각자 Mock 정상/실패 상태와 관련 검증을 통과한 작은 PR로 통합한다. 준비된 접점부터 Integration Issue로 연결하며 전체 Track 완료를 기다리지 않는다.

공통 Contract 합의 → 독립 Feature 개발 → 실제 Page에서 Mock 검증 → Feature별 PR → `front/develop` 통합 → 작은 Integration Issue → 동일 UI의 실제 Adapter/provider 연결 → 실제 흐름 검증.

## 2. 담당 기준 참조와 공통 경계

기존 큰 방향인 **FE1: Part A·D 중심(Account / Authority / Personal / Institution), FE2: Part B·C 중심(Content / Post / Participation / Share)**은 유지한다. Part는 제품 기능 분류이며 장기 브랜치나 디렉터리가 아니다.

누가 어떤 화면·복구 기능을 맡는지는 담당분배 문서와 실제 Issue에서 결정한다. 이번 합의사항으로 알림·검색·추천·설정 등 복구 화면의 담당자를 임의 배정하거나 기존 화면 담당을 바꾸지 않는다. 담당분배 문서에 남은 과거 비밀번호·증빙·기능 제외 문구는 최신 제품 정책·FE 범위를 덮지 못한다. 해당 문서는 이번에 수정하지 않으며, 새 범위의 상세 배분은 그 문서와 Issue에서 별도로 맞춘다.

여러 화면에 걸치는 기능은 **Page 조립 책임 / 데이터·Service 계약 책임 / 공유 파일 편집 책임**을 구분한다. 같은 기능 ID를 양쪽이 추적해도 같은 파일을 동시에 수정하지 않는다.

## 3. Page Owner·Route와 조립 경계

각 담당자는 배정된 Route/Page를 기준으로 구현한다. 논리 화면명이나 Figma WF 코드를 실제 URL·파일명으로 단정하지 않는다. 기존 Route·폴더 구조를 확인하고 목적지·active 상태·returnTo·복귀 맥락을 공통 계약으로 기록한다.

| 변경 상황 | 공통 처리 |
| --- | --- |
| 자기 담당 Page/Screen | 해당 Page Owner가 화면·연결·상태·복귀까지 조립 |
| 상대 Domain 기능을 자기 Page에 삽입 | 합의 props/콜백·Shared Type·Service/provider를 소비. 상대 파일 직접 수정 없이 개발용 stub/Mock 사용 가능 |
| 상대 담당 Route의 문제 발견 | 임의 대규모 수정 금지. 담당자에게 영향 범위를 남기고 별도 Issue/공통 Task 또는 합의한 최소 수정으로 분리 |
| 공통 Route/Layout 변경 | 영향 범위 확인 → 최소 수정 → 상대 기능 호환 유지 → 필요 시 작은 공통 Issue/PR로 선통합 |
| 하나의 Issue에 양쪽 연결 필요 | 실제 수정 담당자 한 명·파일 범위·리뷰어 지정. 분리 가능하면 별도 Issue/PR |
| Integration | 기존 Page Owner 유지. 같은 화면을 다시 만들지 않고 Adapter/provider와 연결부 교체 |

기존 북마크 접점처럼 FE1의 데이터·개인 목록 계약을 FE2 상세가 소비하는 경우, 상세의 버튼 조립과 목록의 데이터 책임을 섞지 않는다. 기관 채택 실행과 공개 상세 표시도 기존 담당 경계를 유지한다. 실제 Route·Page별 상세 배정은 담당분배 문서를 참조한다.

## 4. 공통 코드·충돌 가능 파일 Owner

아래는 기존 공유 영역의 조율 기준을 유지한 것이다. 상세 화면 배분을 새로 확정하는 표가 아니다. 실제 Issue에서 현재 파일 Owner·편집자·리뷰어를 확인한다.

Owner는 인터페이스·변경 영향·파일 충돌·재사용 구조를 조율하는 1차 담당자다. 모든 소비 화면을 혼자 구현하는 사람은 아니다. 공동 검토 대상도 실제 파일 수정 Owner는 반드시 한 명으로 지정한다.

| 충돌 가능 영역 | Owner | 소비/리뷰 | 파일 변경 기준 |
| --- | --- | --- | --- |
| Design Token / Theme / Typography | FE 1 | 양쪽 소비, FE 2 리뷰 | 한 정의, 실제 Theme에 매핑 |
| Header / BottomNavigation / NavItem / 공통 레이아웃 | FE 1 | 양쪽 소비, FE 2 리뷰 | props/슬롯 Contract로 사용, 별도 구현 금지 |
| Input / TextArea / Profile / Verification / Attachment | FE 1 | FE 2 소비/리뷰 | 폼 규격/가변 높이 합의; Attachment는 허용된 사진 UI이며 폐기된 증빙 제출 제외 |
| Auth/Session / Capabilities / returnTo / Region 의미 | FE 1 | FE 2 소비/리뷰 | provider와 계약 조율 |
| Post Type 표시 계층 / PostCard / Photo / PostDetail | FE 2 | FE 1 소비/리뷰 | 동일 원본 카드, 상세 조립 FE 2 |
| CommentItem / ReactionBar / VoteCard / VoteParticipation / Share UI | FE 2 | FE 1 리뷰/목록 소비 | 투표 표시·참여 모두 FE 2 |
| BookmarkState·서비스 / InstitutionState / Adoption 서비스 | FE 1 | FE 2 소비/리뷰 | 상세 버튼/공개 표시 파일은 FE 2 |
| Button / Chip / Modal / BottomSheet / EmptyState / Notice / Toast / Skeleton | 저장소 확인 후 단일 Owner 지정 | 공동 검토 | 새 중복 컴포넌트 없이 개발 stub 주입, 확인된 파일 Owner 우선 |
| API Client / 공통 DTO·Shared Type의 중앙 파일 | 저장소 확인 후 단일 Owner 지정 | 기능 FE/BE + BE1 | 의미별 Owner와 중앙 파일 편집자를 구분 |
| App Router / layout / 전역 Route 등록 | 저장소 확인 후 단일 Owner 지정 | FE 1 복귀, FE 2 상세 계약 리뷰 | 각자 Page 구현, 중앙 등록만 별도 Issue |
| lockfile / 의존성 / 전역 설정·빌드/CI | 저장소 확인 후 단일 Owner 지정 | 공동 검토, 환경 영향 BE2 | 한 Issue 담당자만 변경 |

미지정 공유 영역은 착수 시 해당 Contract/공유 파일 Issue에서 실제 경로·편집자·리뷰어를 기록한다. 이는 양쪽 화면 개발의 선행 구현이 아니다. 중앙 파일 등록이 아직 없어도 합의된 인터페이스로 독립 검증하고 실제 등록은 Integration Issue로 진행한다. 실제 저장소 구조상 파일이 겹치면 더 잘게 나누거나 그 파일 수정만 한 명에게 배정한다.

동일한 `layout`, `globals.css`, 디자인 토큰, Header, BottomNavigation, Provider, 공통 types, API Client를 양쪽 Feature가 동시에 편집하지 않는다. **소비자 추가 요청 → Owner 영향 검토 → 한 명이 작은 공통 PR → 리뷰/CI 후 front/develop 통합 → 소비 Feature 최신 반영**으로 처리한다. 다른 사람의 변경을 덮거나 상대 Feature를 직접 merge하지 않는다. 공통 변경이 진행 중이어도 각자 Page는 합의 인터페이스와 Mock/stub으로 계속 개발한다.

## 5. 선행 Contract와 변경 절차

Contract는 구현체를 기다리기 위한 목록이 아니라 양쪽이 같은 의미로 개발하기 위한 합의다. 아래 명칭은 논리적 계약명이다. 기존 구현이 있으면 기존 함수·파일 구조를 우선하고 아직 없는 경로/DTO를 확정하지 않는다.

| Contract | Owner | Consumer | Mock 필요 여부 | 실제 연동 대상 |
| --- | --- | --- | --- | --- |
| UserSession | FE 1 | 양쪽 | 필요 | 로그인/가입·현재 사용자·인증 전달 |
| UserProfile | FE 1 | 양쪽 | 필요 | 프로필·활동 지역·최신 작성자 표시 |
| Capabilities | FE 1 | 양쪽 | 필요 | 서버 권한·게시물 지역·인증 유효 상태 |
| Region | FE 1 | 양쪽 | 필요 | 지역 후보·기본 활동/탐색/완료/담당 지역 |
| Post / PostCard 표시 모델 / PostDetail | FE 2 | 양쪽 | 필요 | 게시물 원본·카드·상세·개인/기관 목록 |
| Comment / ReactionState | FE 2 | FE 2, FE 1 참여 목록 | 필요 | 댓글·부모 관계·독립 반응·집계 |
| Vote / VoteOption / VoteState | FE 2 | 양쪽 | 필요 | 투표 원본·결과·본인 선택·종료 상태 |
| BookmarkState / Bookmark service | FE 1 | 양쪽 | 필요 | 동일 상세 북마크·개인 목록 |
| InstitutionState / AdoptionState·공개 표시 | FE 1 | 양쪽 | 필요 | 유효 인증·담당 지역·현재 채택 관계 |
| PersonalLists / ActivitySummary | FE 1 | FE 1, FE 2 갱신 영향 | 필요 | 본인 목록·누적 활동·원본 ID |
| returnTo / NavigationContext | FE 1 | 양쪽 | 필요 | 로그인/가입 복귀·목록/지도/상세 이동 |
| Button·Header·Nav·PostCard 등의 props/슬롯/콜백 | 위 공통 영역의 Owner | 양쪽 | 미구현 시 개발 stub | 단일 공통 UI·Theme |
| 검색/추천·관심·Notification·설정/Privy 계정 및 작성 보조 | 상세 담당분배/Issue에서 단일 조율자 확인 | 관련 소비자 | 필요 | 확정 UI·Client State와 미합의 실제 저장/조회 구분 |
| HTTP 응답/오류·공통 DTO 중앙 관리 | 저장소 확인 후 단일 Owner 지정 | 양쪽·기능 BE | 필요 | 기존 API Client·FE/BE 합의 |

각 계약에 의미, 최소 타입, ID 형식, required/nullable, 요청/응답, 오류, 권한, pending, 콜백/갱신 범위, 버전과 합의 Issue를 기록한다. 권한은 단일 isVerified로 축약하지 않는다. 세션 미조회 Loading과 미로그인을 구분하고 이웃/기관 상태 및 게시물 지역을 함께 다룬다. 공통 User/Post/Vote/Comment/Notification/Region 타입이 이미 있으면 재사용한다. 같은 개념을 양쪽에서 다른 이름·다른 구조의 정본으로 만들지 않고 공유 계약은 기존 공유 영역에 둔다. Mock 전용 DTO 정본도 만들지 않는다. 아직 없는 도메인 타입의 존재를 가정하지 않으며 미확정 필드·nullable·Enum은 실제 BE 계약으로 확정하지 않는다.

호출 역할 예시: `getCurrentUser()`, `getPosts()`, `getPost()`, `getComments()`, `createComment()`, `toggleReaction()`, `submitVote()`, `toggleBookmark()`, `getMyPosts()`, `getParticipatedPosts()`, `getParticipatedVotes()`, `getInstitutionAgendas()`, `adoptAgenda()`, `cancelAdoption()`. 함수명은 예시다. toggle 호출도 실제 API의 합의된 등록/취소와 상태 규칙으로 매핑한다.

Contract 변경 절차: 변경 Issue 생성 → 의미별 Owner/소비자 영향 확인 → API는 기능 BE와 협의 및 BE1 정합성 확인 → 타입·Mock·클라이언트·오류·UI props의 동시 변경 범위 합의 → 담당 Feature 검증/PR → 리뷰/CI → front/develop 통합 → 소비 Feature 최신 반영 → 필요한 Integration 재검증. 호환되지 않는 변경은 소비자 이행 범위와 버전을 먼저 기록한다. 합의 전에는 해당 데이터 연결만 보류하며 양쪽 Track 전체를 멈추지 않는다.

실 Endpoint가 없는 기능은 필요한 입출력·오류·권한·상태 계약을 정리하고 Mock으로 구현한다. FE 개발자가 문서 근거 없이 실제 Method/Endpoint를 확정하거나 API SPEC의 이전 경로를 현행 구현으로 복사하지 않는다. 미합의 값은 확인 항목으로 남기고 BE1 및 기능 BE와 계약을 맞춘 뒤 Real Adapter에 반영한다.

## 6. Mock 기반 독립 개발

**동일 Page / Feature → Service interface → Mock Adapter 또는 Real API Adapter**를 사용한다. 현재 저장소의 `frontend/src/lib/api`에 있는 `createApiClient`, `ApiError`, `decodeApiResponse`/`decodeNoContent`, `createMockApiClient`, `ApiClientProvider`/`useApiClient`를 우선 재사용한다. Mock과 Provider는 각각 기존 모듈에서 사용하며 현재 루트 Provider 주입·도메인 Service·기능별 fixture까지 완료된 것은 아니다. 새 Service는 합의한 경계로 추가하고 기존 클라이언트를 복제하지 않는다.

UI 안에서 직접 `fetch()`를 여러 곳에 흩뿌리지 않는다. Adapter는 조립 지점에서 선택하고 정상·실패 응답을 같은 Service 인터페이스로 전달한다. BE 준비 후 실제 UI를 다시 작성하거나 별도 데모 화면을 만드는 대신 연결 Adapter/provider를 교체한다. 인증 전달·오류 해석·decoder는 기존 공통 계층을 사용하며 실제 합의가 없는 자동 인증/refresh를 가정하지 않는다.

| Mock 검증 범위 | 공통 시나리오 |
| --- | --- |
| 데이터 조회 | 정상·빈 데이터·Loading·Error·Retry·삭제/접근 불가 |
| 입력·mutation | 입력 검증·Pending·Disabled·성공·실패·중복 요청 차단·실패 시 입력/기존 저장 상태 보존 |
| 세션·권한 | 미조회·미로그인·로컬 가입 미완료·회원·공유 게스트·이웃 완료/미완료/타 지역·기관 유효/만료/미확인·담당 지역 |
| 화면 이동 | 진입·상세·취소·뒤로가기·저장 후 복귀·returnTo·필터/탭/선택/스크롤 복원 |
| 참여·개인 기록 | 등록/취소·평가 전환·표 변경 확인/취소·같은 원본 ID·목록/상세 갱신 |
| 복구 FE 기능 | 검색 결과/없음/오류, 추천 이유/새 추천/대안 없음, 관심 저장/실패, 알림 필터/읽음/관련 이동, 설정·작성 보조·Privy 계정 상태 |
| 미구현 공통 UI | 단일 props/토큰을 따르는 개발용 stub. 기존 Button/Header/Nav 등을 대체 제작하지 않음 |

화면 내부에 임의 배열·fixture를 하드코딩해 서로 다른 Mock 구조를 만들지 않는다. 기존 Mock/Provider 경계에서 기능 fixture와 시나리오를 모으고 같은 postId/regionId/commentId/voteOptionId 등 합의 ID를 참조한다. 성공 응답만 반환하는 Mock으로 인수를 대신하지 않는다. 기능별 적용되지 않는 상태는 Issue에 이유와 함께 N/A로 기록한다.

권한 Mock은 테스트 상태일 뿐 실제 권한을 생성하지 않는다. Mock 동작 완료는 실제 인증·저장·업로드·알림 발송·접수·탈퇴 완료의 증거가 아니다. 개발/검증 기록에 Data Source를 표시하고 일반 사용자 흐름에는 불필요한 기술 설명을 넣지 않는다. 기존 `createMockApiClient`의 production 생성 금지를 유지하며 Mock/개발 stub을 제품 배포 경로에 노출하지 않는다.

## 7. 완료 상태와 인수 기록

두 개발자는 다음 네 용어를 Issue·PR에서 동일하게 사용한다. UI/Mock 결과와 남은 연결을 각각 기록하며 단독으로 ‘완료’라고 쓰지 않는다.

| 상태·공통 명칭 | 인정 조건 | 의미의 한계 |
| --- | --- | --- |
| **FE UI 구현 완료 (화면 구현 완료)** | 대상 Figma 기준 UI·Route·Component·기본 Navigation 구현 및 확인 | 실제 저장/조회·상호작용 전체나 Backend 완료를 뜻하지 않음 |
| **FE Mock 동작 완료 (FE 기능 완료: Mock 기준)** | 같은 Page에서 입력·클릭·상태 변경·Loading/Empty/Error/Success/Failure/Retry·Pending/Disabled·권한 제한·취소·뒤로가기·상태 보존을 합의 Mock 기준 검증 | 전체 제품 기능 완료 또는 실제 API 동작 완료가 아님 |
| **실 API 연동 대기 (Integration 대기)** | FE UI/Mock을 완료했으나 BE API·실 Contract·Provider 또는 실제 연결 검증이 남음 | FE 완료를 지우지 않음. FE 자체 작업도 남았으면 그 미완료 상태를 함께 표시하며 ‘실 API만 남음’으로 쓰지 않음 |
| **Integration 완료** | 실제 API·Backend 및 실제 provider로 인증/권한·저장/재조회·오류·복귀·연관 화면 갱신을 검증 | Mock 성공이나 PR 병합만으로 올릴 수 없음 |

`Mock 사용 중`을 `FE 미구현`으로 표현하지 않는다. 공통 Mock 도구가 존재한다는 사실만으로 해당 화면의 Mock 동작 완료를 주장하지도 않는다. UI/Mock 완료와 실 API 대기는 함께 기록할 수 있다. API 일부 연결만 끝난 경우 연결·미검증 범위를 별도 근거로 적고 Integration 완료는 전체 해당 Issue 인수 후 표시한다.

Mock 범위 Issue는 정의된 Mock 인수를 충족하면 종료할 수 있다. 실제 연결 후속 Issue·남은 Contract·검증 범위는 링크해 제품 인수 상태를 남긴다. 인수 기록 양식은 12절을 사용한다.

## 8. 디자인 토큰·공통 UI·레이아웃 합의

### 역할별 정본과 구현 순서

| 판단 대상 | 우선 기준 |
| --- | --- |
| 기능 동작·권한·MVP 포함 여부 | 최신 PRD·기능명세서의 확정 정책/MVP 범위·기능 ID별 본문 |
| FE/BE 책임·실제 연동·인수 | 최신 통합지침서 |
| 화면별 Task·상태·동작 | 최신 FE 상세지침서 |
| 사용자 이동·복귀 | 최신 유저플로우 |
| API 데이터·요청/응답·오류·인증 전달 | `Discushion_API_SPEC_v2.md` 계약안과 실제 FE/BE 합의; 설계 제안은 미확정 |
| Git·Issue·Branch·PR | 통합 Git/GitHub 협업전략 최우선 |
| 시각적 화면 외형 | Figma `최종디자인` |
| 색상·폰트·크기·간격·정렬·Radius·Shadow·공통 UI 규격 | 디자인기준 v2 + 해당 최종디자인 Frame |

구현 재사용 순서는 **디자인 토큰 → 공통 UI → 도메인 컴포넌트 → Screen → Page/Route**다. 공통값은 토큰, 반복 UI는 공통 컴포넌트, 화면별 내용은 props/data, 화면별 예외만 개별 스타일로 표현한다.

디자인 문서의 `color/*`, `type/*`, `size/*` 등은 Figma 변수 또는 문서 기준명이다. 실제 코드 토큰명·파일명·Tailwind 설정이 이미 존재한다는 뜻이 아니다. 실제 저장소의 기존 Theme/스타일/공통 컴포넌트를 우선 확인하고 대응표를 남긴다. 코드 이름은 기존 프로젝트 방식에 맞춰 연결하되 값과 역할은 아래 기준을 유지한다.

- 같은 역할의 UI를 화면마다 복제하거나 임의의 `font-size`, `font-weight`, Tailwind 색상·간격 값을 다시 입력하지 않는다. 공통 토큰/Theme에 연결된 표현을 재사용한다.
- 저장소에 Tailwind가 있더라도 버전·설정 방식을 확인하기 전 특정 설정 파일이나 문법을 강제하지 않는다. 신규 패키지 설치를 전제하지 않는다.
- 디자인 문서에 없는 수치를 추정해 새 토큰으로 확정하지 않는다. 필요한 미정 규격은 사용 화면·기존 구현을 확인하고 PO/디자인 합의 결과를 Issue에 남긴다.
- Figma 원본이 없더라도 MVP 기능은 유지한다. 필요한 공통 UI를 개발 대상으로 남기고 먼저 정의한다. 반대로 Figma에 있어도 비-MVP 기능은 추가하지 않는다.

### 모바일 화면·레이아웃

| 항목 | 디자인 기준 | 실제 구현 적용 |
| --- | --- | --- |
| 기본 모바일 화면 | 396 × 852px | 비교 기준 뷰포트. 전체 콘텐츠 높이를 852px로 고정하지 않음 |
| 화면 좌우 / `layout/pad` | 16px / 16px | 기준 너비에서 콘텐츠 364px. 실제 화면 너비에 맞춰 레이아웃 처리 |
| 콘텐츠 top / bottom | `spacing/contentTop` 20px / `spacing/contentBottom` 32px | 스크롤 영역의 위/아래 패딩; 헤더·하단바와 중복 계산하지 않음 |
| 블록 간격 | `layout/sectionGap` 12px | 화면 섹션·카드·텍스트 블록 사이 |
| 내부 간격 | `layout/gap` 8px | 카드/입력 내부, 가로 필터·버튼 사이 |
| 기본 컴포넌트 패딩 | 상하 12px / 좌우 16px | 해당 컴포넌트 규격에 적용; 모든 UI에 일괄 적용하지 않음 |
| 네비게이션 라벨 간격 | `spacing/navigationLabel` 4px | 아이콘과 라벨 사이 |
| 기준 콘텐츠 시작 | y=102px | 상태바 44 + 앱 Header 58인 Figma 기준 좌표 |
| 하단바 있는 뷰포트 | 396 × 674px, y=102~776 | Figma 비교값. 브라우저 높이·고정 영역에 맞춰 실제 가시영역 검증 |
| 하단바 없는 뷰포트 | 396 × 716px, y=102~818, 아래 34px 여유 | Figma 비교값. 34px를 미확인 safe-area 토큰으로 일반화하지 않음 |

웹에서 Figma의 시스템 상태바를 무조건 별도 UI로 복제하거나 y 좌표로 본문을 고정하지 않는다. 실제 실행 환경의 헤더·하단바·스크롤·safe-area 처리를 확인하고, 본문/버튼이 가려지지 않게 한다. 다른 뷰포트용 breakpoint·최대 너비 등 미정 수치를 이 문서에서 새로 지정하지 않는다.

### Typography 토큰

기본 UI 폰트는 **Pretendard**다. font-size·line-height·font-weight·letter-spacing을 역할별 묶음으로 적용한다. 기존 `frontend/public/fonts/PretendardVariable.woff2`와 `src/app/globals.css`의 공통 폰트 설정을 재사용한다. 화면별 inline font-family나 임의 개별 폰트 설정을 만들지 않는다. size/weight/line-height도 공통 역할 토큰을 사용한다.

| 토큰 / 역할 | font-size | line-height | font-weight | letter-spacing |
| --- | ---: | ---: | ---: | --- |
| `type/body` 본문·입력값 | 15px | 23px | 400 | 0% |
| `type/button` 버튼 | 15px | 23px | 600 | 0% |
| `type/caption` 캡션·라벨·필터·메타 | 12px | 18px | 500 | 0% |
| `type/title` 카드 제목·투표 질문 | 17px | 26px | 600 | 0% |
| `type/header` 일반 헤더 | 18px | 27px | 600 | 0% |
| `type/section` 섹션 제목 | 18px | 27px | 700 | 0% |
| `type/navigation` 하단 라벨 | 11px | 17px | 500 | 0% |
| `type/brand` 메인 서비스명 | 20px | 27px | 700 | 0% |

`font/family=Pretendard`, `font/weight/regular=400`, `medium=500`, `semibold=600`, `bold=700`, `font/letterSpacing=0%`를 대응시킨다. Figma 재사용 스타일의 `Discushion/heading`은 24px / 34px / 700이고 실제 모바일 Header는 18px / 27px / 600이다. heading을 Header에 대체 적용하지 않는다. `Discushion/label`은 15px / 23px / 500이며 입력 라벨의 caption과 구분한다.

별도 실제 값: 뒤로가기 문자 `‹`는 Pretendard 26px / 39px / 500, 브랜드 부가 문구는 9px / 약 12.15px / 400이다. 시스템 상태바 시간은 Pretendard 15px / AUTO / 500, 자간 -1.58%이며 일반 UI에 적용하지 않는다. **A01 개별 시작 슬로건은 Inter Bold 16px / AUTO**로 남은 디자인 예외다. 기본 UI를 Inter로 바꾸는 근거가 아니며 해당 문구만 원본과 확인한다. Toggle 원본 라벨 13px / 20px / 400과 L08 인스턴스 15px / 23px를 구분한다.

### Color 토큰

| 용도 / 토큰 | 실제 HEX | 사용 기준 |
| --- | --- | --- |
| Primary / `color/primary` | #0F766E | 주요 버튼·액션·선택 네비게이션 |
| Text / `color/text` | #1F2937 | 본문·제목 |
| Secondary text / `color/secondary` | #667085 | 보조 문구·메타·비선택 네비게이션 |
| Background / `color/background` | #F8FAFC | 화면 배경 |
| Surface / `color/surface` | #FFFFFF | 카드·입력·Header·하단바 |
| Border / `color/border` | #D5DDE3 | 기본 테두리 |
| Soft background / `color/soft` | #E6F4F1 | 안내·AI 요약·투표 선택지 |
| Disabled / `color/disabled` | #E9EEF1 | 비활성 버튼 배경 |
| Muted / `color/muted` | #94A3B8 | 비활성 텍스트·Toggle OFF |
| Error / `color/error` | #D92D20 | 오류 텍스트 |
| Error soft / `color/errorSoft` | #FFF1F0 | 오류 안내 배경 |
| Warning / `color/warning` | #B7791F | 주의 문구 |
| Warning soft / `color/warningSoft` | #FFF7E6 | 주의 안내 배경 |
| Success / `color/success` | #0F766E | 성공 상태 |
| 선택 Chip / `chip/selectedBackground`, `chip/selectedText` | #0F7662 / #FFFFFF | 선택 배경 / 글자 |
| 비선택 Chip / `chip/defaultBackground`, `chip/defaultText` | #F3F4F6 / #4B5563 | 비선택 배경 / 글자 |
| 글쓰기 버튼 / `navigation/writeBackground` | #0F7662 | 하단 중앙 원형 버튼 |

**#0F766E와 #0F7662를 하나의 primary 값으로 합치지 않는다.** Figma 변수 모음 `Discushion / Final UI`의 `primitive/* → color/*` 관계와 문서 기준명 Chip/글쓰기 역할을 구분해 코드에 대응시킨다.

화면별 실제 예외는 전역 토큰으로 일반화하지 않는다: 설정·알림 아이콘 벡터 #212121, 일부 단일 메뉴/버튼 #F0F0F0, 지도 컨테이너 #F2F2F2, 시스템 상태바/시작 슬로건 #000000. E02·E03·E04 게시하기 버튼은 #0F7662 배경 + #2E2E2E 테두리다. 그 화면의 MVP 게시하기 버튼에 대응할 때만 적용하며 기본 Primary를 덮어쓰지 않는다.

### Spacing·Radius·Size·Border·Shadow

| 분류 | 토큰 / 대상 | 값 |
| --- | --- | --- |
| Spacing | `layout/pad` / `layout/gap` / `layout/sectionGap` | 16px / 8px / 12px |
| Spacing | `spacing/contentTop` / `spacing/contentBottom` / `spacing/navigationLabel` | 20px / 32px / 4px |
| Radius | `layout/radius` 버튼·카드·메뉴 | 14px |
| Radius | `layout/inputRadius` 입력·댓글·Notice·투표 선택지 | 10px |
| Radius | `layout/pillRadius` Chip | 999px |
| Border | `layout/stroke` | 1px, 기본 #D5DDE3, Figma Inside |
| Size | `size/buttonHeight` | 52px |
| Size | `size/headerHeight` / `size/statusBarHeight` | 58px / 44px |
| Size | `size/bottomNavigationHeight` | 76px |
| Size | `size/navigationIcon` / `size/headerActionIcon` | 24px / 32px |
| Size | `size/writeButton` | 40px |
| Shadow | `shadow/card` | `0px 3px 10px 0px rgba(15, 23, 41, 0.04)` |
| Shadow | `shadow/bottomNavigation` | `0px -2px 8px 0px rgba(15, 23, 41, 0.05)` |

Figma Inside stroke와 CSS의 border/box model 대응을 확인한다. 테두리 있는 노드의 실제 자식 x=17과 패딩 16을 혼동해 별도 17px 패딩을 만들지 않는다. 카드 Shadow는 게시물·투표·AI 요약·지도 말풍선에 적용하고 입력·Button·Menu·Chip에 일괄 추가하지 않는다. 원본 카드 RGB 0.06 / 0.09 / 0.16, alpha 0.04의 CSS 대응은 위 반올림값이다.

### 공통 컴포넌트 제작 상태와 코드 재사용

2026-10-07 원격 FE 확인 기준으로 다음 공통 구현이 존재한다. **존재/재사용 가능**과 모든 화면·상태·Figma 대응 완료는 구분한다. 각 Issue 시작 때 최신 코드와 기능을 다시 확인한다.

| 공통 영역 | 확인된 코드·재사용/수정 기준 |
| --- | --- |
| Button / Input / TextArea / Notice | `frontend/src/components/ui/`에 존재. native props·disabled·aria 등을 재사용하고 필요한 Pending/오류 문구/성공 피드백 확장은 단일 Owner와 합의. 현재 Notice에 success tone이 있다고 가정하지 않음 |
| Header / BottomNavigation / MobileLayout | `frontend/src/components/layout/`에 존재. Header의 title/onBack/rightAction, 하단바 activeItem/destinations/onNavigate, MobileLayout의 Header/Nav slot·스크롤·safe-area를 우선 재사용. 메인 Header·5메뉴는 부분 구현의 보완 대상 |
| API Client / Error / Response decoder / Mock / Provider | `frontend/src/lib/api/`의 공통 구현 재사용. 루트 주입·도메인 Service 연결·실 인증/응답 검증은 별도 작업 |
| 디자인 토큰 / 폰트 / 아이콘 | `globals.css`의 기존 토큰, Pretendard 폰트, `public/icons`의 home/map/my/write 자산 재사용. 알림 등 부족한 자산은 Figma 기준으로 공통 변경 |
| 반복 도메인 UI 후보 | PostCard, VoteCard, CommentItem, CategoryChip, FilterChip, ProfileRow, SettingRow, NotificationItem |
| 반복 상태·피드백 UI 후보 | EmptyState, ErrorState, LoadingState, Modal, BottomSheet, Toast, ConfirmDialog |

후보 목록은 코드 존재나 새 개발 완료를 뜻하지 않는다. **기존 컴포넌트로 해결 가능한지 먼저 확인하고 실제로 여러 화면에서 반복될 때 공통화**한다. 한 화면만 쓰는 UI를 무조건 공통 영역으로 올리지 않는다. props/variant/slot으로 차이를 표현하며 같은 목적의 베이스를 FE1/FE2가 각각 제작하지 않는다.

미구현 공통 UI는 합의된 props·단일 토큰의 개발 stub으로 소비 화면을 먼저 검증할 수 있다. 제품용 대체 UI를 만들지 않고 공통 PR 통합 뒤 실제 컴포넌트로 연결한다. Figma 원본은 실제 코드·모든 상태 Variant 완료의 증거가 아니다. Toggle은 복구된 푸시 설정 등 명세상 사용처에 적용하며 다크 모드를 추가하지 않는다.

### 컴포넌트별 상세 구현 규격

| 요소 | 기본 규격 | 적용·변형 원칙 |
| --- | --- | --- |
| Button | 기준 364 × 52px, Radius 14, 패딩 상하 12/좌우 16, `type/button`, 중앙 정렬 | 배치에 따라 너비만 변경. 2열 178+8+178, 3열 116×3+8×2. 박스 내부 332, 지도 확대/축소 56, 일부 게스트 행동 108은 해당 화면 참조값 |
| Primary | 배경/테두리 #0F766E, 글자 #FFFFFF, 테두리 1px | 선택 Chip 색과 합치지 않음 |
| Secondary | 배경 #FFFFFF, 글자 #0F766E, 테두리 1px #D5DDE3 | 기본 보조 행동 |
| Disabled | 배경 #E9EEF1, 글자 #94A3B8, 테두리 1px #D5DDE3 | 비활성 사유/실제 권한은 제품·서버 기준; pending 상태와 의미 구분 |
| Input | 전체 364 × 75px, Radius 10, Surface/Border, 패딩 상하 12/좌우 16, gap 8 | 75는 라벨+값 전체 높이. label `type/caption`, 값/placeholder `type/body`; input 태그 자체를 75로 단정하지 않음 |
| TextArea | 공통 원본 364 × 132px, Radius 10 | 화면별 75px, E09 본문 121px는 개별 변형; 장문·오류 안내가 가려지지 않게 확인 |
| Chip | 116 × 34px, Radius 999, 패딩 상하 8/좌우 16, `type/caption`, 중앙 정렬 | 가로 gap 8, 다음 줄 block gap 12. 긴 `댓글 좋아요·싫어요`는 높이 52; 필터 내용은 PRD의 유형/주제/참여 의미 우선 |
| PostCard / PostCardPhoto | 기준 364 × 104 / 364 × 212px, Radius 14, gap 8, 패딩 상하 12/좌우 16 | Surface·1px Border·카드 Shadow. 제목 `type/title`, 메타/반응 `type/caption`. 장문은 높이 확장. 사진 카드 내부 332 × 100, Radius 10 |
| CommentItem | 기준 364 × 99px, Radius 10, gap 8, 패딩 상하 12/좌우 16 | 가변 본문 높이. 답글 들여쓰기 22는 외부 목록 컨테이너에 적용하며 답글 1단계 정책 유지 |
| VoteCard / 선택지 | 메인 대표 364 × 220px, Radius 14, 카드 패딩/gap 12 | 질문 `type/title`, 카드 Shadow. 선택지는 soft 배경·Radius 10·패딩 8. 원본 미제작이므로 진행/제출 후/종료/제한 상태 공통 정의가 선행 |
| Notice | 기준 364 × 42px, Radius 10, gap 8, 패딩 상하 12/좌우 16, 기본 soft 배경 | 2줄 60, 3줄 78 높이 참조. 기본 테두리 없음. 역할별 errorSoft/warningSoft 사용; 장문을 42에 고정하지 않음 |
| Attachment / Photo | Attachment 364 × 80px, Photo 원본 364 × 150px, Radius 14 | Photo 1px Border, 상세 사진 대표 364 × 180 변형. 이미지 비율/crop은 에셋/원본 확인; 사진 없으면 영역 생략 |
| Profile 표시 | 사진 영역 364 × 72 / 364 × 80px 변형 | 이 값은 전체 사진 영역이지 원형 avatar 크기가 아님. 닉네임·유효 기관 배지 표시를 공통화 |
| AI 요약 | soft 배경, Radius 14, 1px Border, 카드 Shadow, gap 12 | 실제 패딩 위 38/우 16/아래 36/좌 16. 내용에 따라 높이 증가, 3문장 한 문단·원문 fallback 유지 |
| 지도 말풍선 | 대표 152 × 90 / 152 × 80px, Surface·Radius 14·Border·카드 Shadow | 패딩 상하 6/좌우 8, gap 4. 첫 사진 없으면 썸네일 생략; 메인 지도 미리보기 대표 364 × 152px |

표의 기본 크기는 원본 비교값이며 모든 문구·상태에 고정 높이를 강제하지 않는다. 34px Chip, 52px Button 같은 지정 정렬 규격과 가변 본문/카드 높이를 구분한다. 문서의 패딩·행간·고정 높이 조합은 실제 box model에서 확인하고, 잘림이 있으면 원본·Owner와 확인한다. 확인 없이 새로운 높이로 전역 변경하지 않는다.

Toggle을 MVP 화면에서 실제 사용할 경우 전체 108 × 44, 트랙 44 × 26/Radius 13, 손잡이 20 × 20/#FFFFFF, 라벨 영역 40/gap 8을 따른다. ON 트랙 #0F766E, 손잡이 x=20/y=3; OFF #94A3B8, x=4/y=3은 원본 비교값이다.

### Header·BottomNavigation·NavItem

**일반 Header:** 높이 58px, Surface, 좌우 패딩 16px, gap 8px, 세로 중앙 정렬. 제목은 `type/header`로 뒤로가기 오른쪽에 왼쪽 정렬한다. 396px 기준 뒤로가기 영역 24 × 39(x=16/y=9.5), 제목 영역 260 × 27(x=48/y=15.5), 설정 액션 영역 56 × 58(x=316/y=0)이다. `‹`는 문자(26/39/500)이며 24 × 24 SVG라고 기록하지 않는다. 문자 내부 y≈-2.01 보정은 원본 참조값이지 모든 브라우저에 무조건 적용할 CSS offset이 아니다. 톱니바퀴는 32 × 32, 액션 영역 내 x=24/y≈11.44이며 작은 `설정` 글자와 혼용하지 않는다.

**메인 Header:** Figma `Header/Main` 외형은 기존 공통 Header 계열의 props/variant/합성으로 확장한다. 화면마다 새 Header를 만들지 않는다. 높이 58, 좌 24/우 14 패딩, gap 12. 브랜드 영역 270 × 58(x=24), 브랜드 이미지 35 × 32, 서비스명 `type/brand`, 부가 문구 9/약12.15/400. 설정/알림의 터치 영역 각각 32 × 58(x=306/350), 아이콘 32 × 32다. 설정은 L01, 알림은 H01의 FE 대상 화면으로 연결한다. 실제 Route 문자열은 공통 Route 계약과 맞추며 API 부재로 액션을 누락하지 않는다.

**BottomNavigation 원본:** 396 × 76, 기준 y=776, Surface, Radius 0, 1px Border(Inside), `shadow/bottomNavigation`. 상하 8/좌우 16 패딩, 슬롯 gap 0. Figma 슬롯은 **메인 / 지도 / 글쓰기 / 알림 / 마이** 5개, 기준 슬롯 너비 72.8이다. 일반 NavItem 높이 53, 상하 4/좌우 2 패딩, 아이콘 정렬 영역 24 × 24, 라벨 `type/navigation` 높이 17, 아이콘-라벨 gap 4, 중앙 정렬. 선택 메뉴 Primary, 비선택 Secondary다.

**중앙 글쓰기:** 40 × 40 원형, #0F7662, 흰 `+`, 일반 라벨 없음. 원본의 슬롯 프레임 높이 32 안에서 위아래로 걸친 배치와 일반 NavItem을 구분하고 clipping을 확인한다. 아이콘 bounds(지도 도형 약 24 × 22.77), 정렬 영역(24 × 24), 터치 컨테이너를 혼동하지 않는다. 아이콘 선 굵기를 카드 Border 1px로 통일하지 않는다.

**공통 5메뉴 확정:** 메인 / 지도 / 글쓰기 / 알림 / 마이의 순서·목적지·active 상태·아이콘 크기·label·간격·safe-area·선택/비선택 스타일을 양쪽이 동일하게 사용한다. 기존 공통 BottomNavigation을 수정·재사용하며 현재 4개 구현은 `FE 부분 구현 / 수정 필요`다. 이를 제품 기준으로 유지하거나 화면별 Nav를 새로 만들지 않는다. 알림은 H01로 독립 진입하고 헤더 알림도 같은 화면을 사용한다. 396px의 5슬롯 비교값을 적용하되 실제 너비에 맞춰 대응한다. 글쓰기는 시각 라벨을 숨기는 원본 표현과 접근 가능한 이름을 구분한다.

Header 역시 뒤로가기·제목·로고·설정·알림·우측 action·수평/수직 정렬·padding·높이·아이콘을 기존 계열에서 통일한다. Main과 일반 Header의 디자인 차이를 props/variant로 표현하고 화면별 복제나 임의 정렬을 금지한다. 공유 게스트에는 일반 BottomNavigation을 제공하지 않으며 다른 화면의 노출 여부는 해당 Figma와 유저플로우를 따른다.

### 미정 UI 규격·화면 예외·디자인 전달

Modal/BottomSheet/EmptyState와 Toast/Skeleton/Profile의 미확정 세부는 필요한 사용 화면·상태·역할을 먼저 기록한다. 확정 토큰과 Button/Input 등은 재사용하되 Modal 너비, overlay opacity, Toast 지속시간, Skeleton 속도 등을 추정해 디자인 정본 값처럼 쓰지 않는다. BottomSheet는 MVP에서 쓰는 화면이 확인될 때 구현 대상으로 삼으며 공통 목록에 있다는 이유만으로 새 흐름을 만들지 않는다.

다음 기록값은 일반화하지 않는다: A01 캔버스 Radius 20(일반 목업 28), 시작 이미지 336 × 224(x=29/y=169), C01-새추천성공 스크롤 너비 380, B07-투표결과-하계2동-이웃미인증 뷰포트 높이 1354, TextArea/Chip/Toggle의 개별 높이·라벨 변형. 추천 화면은 최신 FE 대상이며 그 화면의 예외 수치를 전역 규격으로 승격하지 않는다. 캔버스 Radius 28은 웹페이지·카드의 전역 Radius가 아니다.

공통 UI 없는 MVP 화면은 기능을 삭제하지 않고 **규격 확인 → 필요한 공통 정의 Issue/PR → front/develop 통합 → 기능 Issue 재사용**으로 진행한다. 공통 시스템 전체를 매 기능 Issue에서 재구축하지 않는다. 이미 통합된 컴포넌트는 props/data/합의된 변형만 사용한다. 여러 화면에 영향을 주는 새 변형은 Owner와 범위를 맞춰 별도 선행 공통 Issue로 통합한다.

대응 기록에는 Figma 원본/화면, 디자인 토큰, 실제 코드 컴포넌트·경로, Owner/Review, 상태별 구현 범위, 미정 규격, 선행 Issue/PR을 적는다. 미확인 코드 경로는 `저장소 확인 필요`로 둔다. 현재 존재하는 공통 Header를 우선 사용하고 부족한 변형만 Owner와 공통 변경으로 확장한다.

Figma 참조: [최종디자인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1136-6698), [공통 UI](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1155-950), [Header/Back](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1155-1005), [Header/Action](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1155-1010), [Header/Main](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1255-6584), [BottomNavigation](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1255-6597), [NavItem](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1155-981), [ChipSelected](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1255-6582). 이 문서는 제공된 디자인 기준 전체를 반영한 개발 합의이며 새 Figma 컴포넌트나 실제 코드를 제작했다는 보고가 아니다.

---

## 9. 기술·상태·실패/복귀 기준

### 프론트엔드 기술 선택 및 상태 관리 기준

현재 `frontend/package.json`에서 Next.js 16.3.8·React 19.3.0·TypeScript 5.9.3·Tailwind CSS 4.3.3을 확인했다. 실행 환경과 최신 버전은 작업 시작 때 다시 확인한다.

- Next.js
- TypeScript
- Tailwind CSS

현재 TanStack Query·React Hook Form·Zod·Zustand·Axios·Privy SDK는 package에 없다. 문서의 권장안을 이미 설치된 것으로 가정하지 않는다. 추가 라이브러리는 `package.json`과 기존 구현을 확인하고 필요성·공유 영향 합의 후 별도 범위로 다룬다.

이미 프로젝트에 동일 목적의 라이브러리나 공통 구조가 존재한다면 새 라이브러리를 추가하지 않고 기존 방식을 우선 사용한다.

기존 구현이 없을 경우 다음 조합은 기존 문서의 조건부 권장안으로만 유지한다. 설치·채택 완료를 뜻하지 않으며 실제 도입은 별도 필요성·저장소 확인 후 합의한다.

| 목적 | 기본 권장안 | 사용 원칙 |
|---|---|---|
| 서버 상태 | TanStack Query | 게시물·댓글·반응·투표·북마크·사용자 정보·인증 상태 |
| 폼 상태 | React Hook Form | Privy 확인 상태·로컬 가입·프로필·게시물 작성/수정 폼 |
| 폼/DTO 검증 | Zod | 이미 합의된 값만 검증하며 미정 길이·정책을 임의로 추가하지 않음 |
| Client 전역 상태 | Zustand | 필요한 경우에만 최소 사용 |
| HTTP 요청 | 기존 API Client·decoder·Provider | Service/Adapter 경계로 사용. UI별 직접 fetch나 새 Axios 도입을 기본으로 삼지 않음 |

### 상태 종류별 사용 기준

프론트 전체 상태를 하나의 전역 상태로 몰아넣지 않는다.

| 상태 종류 | 관리 기준 | 예시 |
|---|---|---|
| 서버 상태 | 기존 서버 상태 계층 우선; 도입 합의 시 TanStack Query | 게시물, 댓글, 반응 수, 투표 결과, 북마크, 사용자 정보, 인증 상태 |
| 전역 Client 상태 | 기존 공통 상태 계층 최소 사용; 도입 합의 시 Zustand | 회원가입 단계 유지값, Route 왕복이 필요한 작성 draft, 현재 탐색 지역 등 |
| 폼 상태 | 기존 폼 방식 우선; 도입 합의 시 React Hook Form | Privy 확인·로컬 가입, 프로필, 게시물 작성/수정 |
| 화면 로컬 상태 | 컴포넌트 로컬 state | Modal 열림, 탭 선택, 임시 선택값, 버튼 pending |

게시물·반응·댓글·투표 결과를 여러 화면에서 서로 다른 원본처럼 관리하지 않는다.

같은 서버 데이터는 가능한 한 같은 서버 원본과 캐시를 기준으로 사용한다.

---

### 공통 UI 상태 및 사용자 피드백 기준

기능이 정상일 때의 UI만 구현하고 끝내지 않는다.

두 사람 모두 다음 상태를 공통 기준으로 구현한다.

| 상태 | 공통 처리 |
|---|---|
| Loading | 최초 데이터 조회는 Skeleton 또는 진행 안내 |
| Pending | 저장·수정·삭제·투표 등 요청 단위 pending·중복 차단, 무관한 탐색을 일괄 잠그지 않음 |
| Disabled | 미입력/처리 중/정책 제한 사유를 구분하고 공통 스타일 적용 |
| Success / Failure | 성공 후 명세상 피드백·이동/재조회, 실패 시 입력·기존 저장 상태 유지와 복구 |
| Retry | 실패 원인·재시도 가능 여부에 맞는 재호출. 처리 결과 불명이면 재조회 후 판단 |
| Empty | 정상 응답이지만 데이터가 없으면 공통 `EmptyState` |
| Error | 조회/통신 실패는 Notice + 필요 시 Retry |
| Forbidden | 로그인/이웃 인증/기관 권한 부족 사유에 맞는 안내 |
| Deleted/Unavailable | 삭제됐거나 접근할 수 없는 원본을 Empty로 위장하지 않음 |

### 상태 외형과 디자인 토큰 연결

정상 상태만 디자인 확인하고 끝내지 않는다. Loading/Empty/Error/권한 제한/Pending에서도 디자인 기준 절의 Typography·Surface·간격·레이아웃을 재사용한다. 조회 실패는 `color/error`/`color/errorSoft`, 주의는 `color/warning`/`color/warningSoft`, 입력 오류 텍스트는 `color/error`로 역할을 구분한다. Notice는 Radius 10과 가변 높이 기준, EmptyState·Toast·Skeleton·Modal은 미정 상세 규격을 확인한 공통 정의를 사용한다.

Pending 중 버튼의 높이 52·라벨 정렬을 유지하고 중복 요청을 막는다. pending 외형이 디자인 문서에 제작 완료로 기록된 것은 아니므로 신규 spinner 크기·색·시간을 임의 정본으로 확정하지 않는다. 해당 영역 Loading이 Header/하단 메뉴를 화면별로 다른 스타일로 바꾸거나 본문을 가리지 않도록 확인한다.

### Error 표시 방식

오류는 목적에 따라 다음처럼 구분한다.

| 상황 | 표시 방식 |
|---|---|
| 사용자가 입력값을 수정해야 함 | Input 아래 Inline Error |
| 화면/목록 조회에 실패함 | Notice + Retry |
| 짧은 성공/실패 결과 안내 | Toast |
| 사용자가 확인 후 진행해야 함 | Modal |

예:

- 이메일 형식 오류 → Inline Error
- 게시물 목록 조회 실패 → Notice + Retry
- 링크 복사 완료 → Toast
- 게시물 삭제 → 확인 Modal
- 투표 선택 변경 → 확인 Modal

### Modal 사용 기준

Modal은 삭제·투표 변경처럼 사용자의 명시적 확인이 필요한 경우에만 사용한다.

단순 성공 안내나 짧은 오류 알림을 Modal로 남발하지 않는다. 다만 북마크 등록 성공의 `저장되었습니다` 팝업 등 기능명세에서 지정한 피드백은 해당 명세를 우선한다.

---


### mutation·상태·복귀 기준

기본은 API 요청→성공 응답→서버 정본으로 갱신이며 Optimistic Update를 기본 사용하지 않는다. 선택 도입 시 rollback을 보장한다. 로컬 가입/Privy 확인/댓글 생성/투표/삭제/채택은 실제 연동에서 서버·Provider 성공 전에 성공으로 확정하지 않는다. Mock에서는 합의 시나리오 상태만 전환하고 실제 성공의 증거로 기록하지 않는다.

요청 단위 Pending·중복 클릭 차단을 적용한다. 실패 시 가입·작성/수정·댓글 입력·정상 첨부·필터·정렬·탭·스크롤을 유지한다. 투표 변경 취소는 기존 표를 유지한다. 401/403/404/409 등은 실제 합의 오류 코드와 의미에 맞추며 409를 모든 투표 변경의 필수 응답으로 단정하지 않는다. 서버 데이터는 공통 캐시/조회 경계를 공유하고 FE 1 목록과 FE 2 상세에 별도 정본을 두지 않는다.

### 화면 이동·상태 보존

뒤로가기·취소·저장 후 복귀·로그인 후 returnTo·상세 → 목록·검색 결과 → 상세 → 검색 결과는 최신 유저플로우를 따른다. 화면마다 임의 목적지로 보내지 않는다. 입력값·검색 조건·지역/유형/주제 필터·정렬·탭·선택 상태·원 진입 맥락·스크롤을 필요한 범위에서 복구한다. 사진 선택/미리보기·지역 선택·부가 화면 왕복과 실패/취소에서도 정상 입력·선택 파일을 유지한다. 회원 기능 로그인 후에는 원 화면으로 복귀만 하고 행동은 자동 실행하지 않는다.

### 폴더·Naming 공통 기준

현재 `frontend/src/app`, `components/ui`, `components/layout`, `lib/api` 등 실제 구조와 FE 상세지침서를 우선한다. 동일 개념을 한쪽은 components, 다른 쪽은 features/widgets로 임의 분산시키지 않는다. 필요한 새 도메인 구조는 기존 코드와 합의한 뒤 사용한다. Component는 `PascalCase`, 함수/변수는 `camelCase`, 상수·파일명·폴더명은 기존 프로젝트 스타일을 유지한다. 상대 Part 리팩터링·전역 포맷팅·불필요한 패키지 추가를 함께 하지 않는다.

### 개발 시작 전 기록

- [ ] 실제 FE/BE 이름·리뷰 상대, 저장소/브랜치 상태, 프레임워크·버전·패키지 매니저·lockfile 확인
- [ ] 공유 파일 실제 경로·단일 Owner·수정 범위 기록
- [ ] 각 Issue의 Contract 합의 링크/버전·Mock/provider·UI props·실제 연결 대상 확인
- [ ] API URL/인증·ID/DTO/nullable/error·실행/CORS·테스트/build 명령 확인
- [ ] 5개 하단 메뉴·브랜드 Header의 설정/알림 연결·미정 공통 UI 규격 확인
- [ ] 네 상태 인수·후속 Integration Issue·수정 담당자 기록

개발 착수용 Contract 합의는 최소 계약 기준이다. 실제 코드가 PR로 준비되었다는 뜻이 아니다. 미합의 항목의 연결만 보류하고 확정 영역의 작업을 계속한다.

## 10. 병렬 Track 운영

이 절은 상세 화면 담당표가 아니라 **상대 구현 없이 진행하는 방식**을 정한다. 실제 화면군·복구 기능 배분은 담당분배 문서/Issue에서 확정한다. 아래 관계는 단계별 Gate가 아니며 어느 행도 전체 Track 종료를 다음 작업의 시작 조건으로 삼지 않는다.

| 작업 관계 | 구현 전 합의 | 서로 기다리지 않고 진행할 것 | 준비 후 연결 |
| --- | --- | --- | --- |
| 세션/권한 소비 화면 ↔ 계정·자격 데이터 | Session·Capabilities·Region·returnTo interface | 조회 중/미로그인/가입 미완료/지역·기관 상태 Mock으로 각 Page와 제한 UI 개발 | 실제 Privy/provider·서버 권한 |
| 개인/기관 목록 ↔ 콘텐츠 카드·상세 | 표시 모델·postId·props·클릭/복귀 콜백 | 목록은 카드 stub/Mock, 콘텐츠는 같은 모델의 카드/상세 개발 | 단일 공통 카드·원 상세·캐시 갱신 |
| 상세 버튼/공개 표시 ↔ 북마크/기관 관계 | 상태·요청 결과·피드백·갱신 범위 | Page Owner는 버튼/표시, 데이터 담당은 같은 Service Mock을 독립 검증 | 실제 관계·목록/상세 동기화 |
| 화면군 ↔ 공통 UI/Router | props/variant/slot·논리 목적지·active·복귀 | 기존 공통 UI 재사용, 부족한 부분만 개발 stub으로 독립 검증 | 작은 공통 PR과 실제 Route 등록 |
| 복구 기능 ↔ 미준비 BE | 확정 UI/Client State·공유 모델·미합의 필드 기록 | 알림·검색·추천·관심·설정/Privy 계정·작성 보조 사용자 흐름 Mock 구현 | 합의 실제 Adapter·저장/조회/발송 검증 |

독립 Issue를 동시에 진행하면 작업 디렉터리/브랜치와 수정 파일을 분리하고 공유 파일은 한 명만 편집한다. 제품 데이터의 의미는 공유하되 상대 작업을 복사·merge해 시작 조건을 만들지 않는다. 공통 변경의 통합 순서만 조율하고 나머지 Page 개발은 계속한다.

## 11. Integration Issue 운영

실 API/실 Provider가 준비된 접점부터 작은 Issue로 연결한다. 전체 Track 완료는 진입 조건이 아니다. 미준비 기능은 `실 API 연동 대기`로 남기고 다른 FE 개발·Mock 검증은 계속한다. 실제 Method/Endpoint·DTO·인증 전달·오류·CORS를 API 정본 및 FE/BE 합의와 대조하며 **이 문서에서 API 경로를 새로 확정하지 않는다.**

| 연동 접점 | 합의/확인할 계약 | 실제 검증 근거 |
| --- | --- | --- |
| Privy·로컬 가입 ↔ 세션/returnTo | #74 Provider·회원 연결·인증 전달·취소/오류 | 실제 인증과 로컬 가입 완료 구분·원 상세 복귀·자동 행동 없음 |
| 자격 상태 ↔ 주민 참여·기관 업무 | #75 시연 계정과 #74 자격 조회·서버 검증 | 완료 이웃 지역·기관 유효/만료·담당 지역·서버 거부; 증빙 제출 흐름 없음 |
| 원 상세 ↔ 북마크/개인·기관 목록 | 같은 원본 ID·관계·캐시 갱신 | 실제 저장/취소·현재 표/행동·삭제 원본·공개 채택 표시 |
| 작성·사진 ↔ 실제 저장/Storage | 업로드/연결·권한·삭제·미완료 정리 계약 | 원본 게시·사진 공개 열람·제거/교체/게시물 삭제·서버 24시간 정리 |
| 검색/추천·관심·알림·설정/계정·작성 보조 | 해당 기능의 실제 조회/저장/Provider·이벤트 계약 | 실제 결과·저장/재조회·읽음·수신 설정·푸시/예약·계정/세션 등 해당 Issue 흐름 |
| 공통 UI/Route ↔ 각 Page | 기존 Owner의 props·단일 토큰·목적지/active | stub 제거·5메뉴·Header 액션·뒤로가기·레이아웃/스크롤 |
| 실제 실행 환경 ↔ 전체 사용자 흐름 | #30 API 주소·인증·Storage·CORS, #31 여정 인수 범위 | 실제 화면 → API → 서버 처리 → 재조회. health 응답만으로 제품 연동 완료 처리하지 않음 |

각 Issue는 수정 담당자 한 명·기존 Page Owner·실제 파일·Contract 버전·교체 Mock/stub·완료 조건·정상/실패/권한/복귀·실행 결과를 기록한다. 큰 접점은 독립 검증 가능한 기능으로 나누고 **1 Issue / 1 Feature / 1 PR**을 지킨다. 서버·Provider가 없어 검증하지 못한 범위는 숨기지 않는다.

Integration PR은 동일 UI의 연결 계층을 실제 Adapter/provider로 바꾸고 원본·상태·오류·캐시/화면 갱신·회귀를 확인한다. 개발 fixture는 검증용으로 유지할 수 있으나 제품 경로에 남기지 않는다. 실제 API 일부 연결이나 공통 UI 연결만으로 해당 기능의 Backend Integration 완료를 주장하지 않는다. BE API/업무 로직/계약 상태는 FE 완료와 따로 기록하며 DB Schema 파일 존재도 API·업무 처리 완료의 근거가 아니다.

## 12. Git·Issue·PR 운영

**1 Issue → 1 Feature → 1 PR → front/develop**. Mock·Contract·Integration Issue 모두 같은 규칙을 사용한다.

GitHub Issue → 최신 front/develop → front/feature/<issue번호>-<기능명> → 구현 → 관련 test/build → push → PR(base=front/develop) → 상대 리뷰·필수 CI → GitHub PR 병합.

```bash
git status
git fetch origin
git switch front/develop
git pull --ff-only origin front/develop
# <실제번호>와 <기능명>을 GitHub Issue 값으로 교체
git switch -c front/feature/<실제번호>-<기능명>
```

미커밋 변경은 먼저 보존한다. 이미 만든 Feature를 계속할 때 새 브랜치를 다시 만들지 않는다. 최신 통합 반영은 자신의 Feature에서 `git fetch origin` 후 `git merge origin/front/develop`으로 진행한다. 상대 Feature 직접 merge/cherry-pick 전달을 하지 않는다. 아직 없는 구현을 기다리는 대신 합의 Contract/Mock으로 진행한다.

commit은 `<type>(<scope>): <내용>`: type=feat/fix/refactor/test/docs/chore, scope=fe/be/common. 공유 commit amend/rebase·force push·이력을 덮는 reset 금지. 수정은 후속 commit이다. 관련 test/build 명령은 실제 README/package 설정에서 확인하며 미확인 명령을 만들지 않는다.

PR에는 Issue 연결·기능명세 ID·변경 범위·Contract/API/공통 UI 영향·네 완료 상태·검증 근거·후속 Integration Issue를 기록한다. 필수 CI와 아래 PR 병합 규칙을 충족한 뒤 GitHub PR로 병합하고 Issue 종료·Feature 삭제를 확인한다. 충돌은 담당 Feature에서 요구사항을 보존하며 해결·재검증한다. 전부 ours/theirs 선택이나 타인 변경 삭제는 금지다.

### PR 단위·검증·병합 규칙

- 공통 Layout, Auth, Home, Map, Create, Notification, My, Settings 등 독립 검증 가능한 담당 화면군/기능군으로 Issue·PR을 나눈다. 한 PR에 무관한 여러 화면이나 다른 Part 기능을 섞지 않는다. 이는 분할 예시이며 화면 담당 배정이 아니다.
- PR 전 Route 접근, Figma 대응, 버튼/입력 동작, 뒤로가기/취소, 상태 보존, Loading/Empty/Error, Mock 정상/실패, 권한 제한과 관련 상태를 확인한다. 상세 QA는 FE 상세지침서 58절을 사용한다.
- 현재 `frontend/package.json`에는 `lint`, `typecheck`, `build`가 있다. `frontend`에서 `npm run lint`, `npm run typecheck`, `npm run build`를 관련 작업에 실행하고 명령·결과를 남긴다. 현재 `test` script는 없으므로 `npm test`가 있다고 가정하지 않는다. 기존 테스트는 README/CI의 실제 실행 방법을 확인한 경우에만 해당 명령으로 수행한다.
- 병합 전 소비 Feature는 최신 `origin/front/develop`을 자기 Feature에 반영하고 공유 파일/API 영향·충돌·관련 검증을 다시 확인한다. 보호 브랜치에서 로컬 병합 후 직접 push하지 않는다.
- 통합/FE Git 전략에 따라 상대 FE 리뷰와 필수 CI를 통과한 뒤 GitHub PR에서 병합한다. 작성자 단독 승인·병합이나 임의의 리뷰 생략 예외를 이 문서에서 만들지 않는다. 실제 보호 설정은 별도 확인한다.
- CI 실패·미검증·미해결 충돌·정책/디자인/계약 충돌이 있으면 병합하지 않는다. PR base는 `front/develop`, compare는 해당 Feature다.
- `front/develop` 대상 PR의 Issue 자동 종료를 전제하지 않는다. 병합·Issue 종료·후속 Integration 연결·Feature 정리를 확인한다.

금지: front/develop/main/back/develop 직접 push·로컬 병합 반영, 상대 Feature 직접 merge, 공유 이력 강제 덮어쓰기, 무관한 리팩터링/전역 포맷팅, 무통보 계약 변경, secret commit.

FE/BE 코드를 Git merge로 연결하지 않는다. 기존 제품 Part별 FE/BE 창구를 사용하며 API 정본/DTO 정합성은 BE1, 실행환경·환경변수·CORS는 BE2와 확인한다. 최종 main 반영은 검증한 각 develop의 PR이며 두 번째 PR은 최신 main 기준으로 재확인한다. 이는 별도 통합 절차이며 일반 기능 PR에 포함하지 않는다.

Codex는 Issue 하나의 허용 범위에서 변경안·검증 결과를 준비한다. **사용자가 명시적으로 요청하지 않는 한 브랜치 생성/전환·commit·push·PR 생성·PR 병합·develop/main 반영을 수행하지 않는다.** 작업 후 사람이 status/diff/staged diff와 새 파일을 검토할 수 있게 보고한다.

### Issue/PR 공통 기록 양식

```markdown
- 목표/담당자/리뷰어:
- Domain/Part/기능명세 ID:
- 실제 Page Owner/수정 파일/공유 파일 Owner:
- Contract: 합의 Issue·버전 또는 미합의 항목
- Data Source: Mock / 실제 API (기능별)
- Mock/stub 주입 및 실제 교체 대상:
- 대상 Figma Frame/정책 차이·Route/복귀·상태 보존:
- FE UI 구현 완료: 예/아니오 · 확인 근거
- FE Mock 동작 완료: 예/아니오 · 정상/실패·상태/복귀 근거
- 실 API 연동 대기: 예/아니오 · 남은 API/Contract/Provider·후속 Issue
- Integration 완료: 예/아니오 · 실 API/Backend 검증 근거
- 실제 연결된 API/미검증 범위: 부분 연결도 명시
- BE API/업무 로직/계약 상태: FE 완료와 별도 기록
- 실제 실행한 lint/typecheck/test/build 명령·결과 및 미실행 사유:
- 공통 변경/API 영향 및 후속 Integration Issue:
```

## 13. 제품 정책 경계·최종 점검

다음은 양쪽이 공유하는 경계다. 화면별 입력·이동·Task·상태·QA는 제품 정본/유저플로우/FE 상세지침서를 참조하며 여기서 다시 상세 명세하거나 Backend 범위를 확대하지 않는다.

| 영역 | 동일하게 지킬 공통 기준 |
| --- | --- |
| Privy 인증·가입 | 이메일 OTP·성공/실패/취소 UI. 최초 사용자는 로컬 약관·프로필·활동 지역과 returnTo를 완료. Provider 인증 성공과 로컬 회원 가입 완료 구분. 자체 비밀번호 로그인/설정/확인/찾기/재설정, 자체 OTP 서버·코드 저장/발급 금지. 기존 자체 코드 길이/타이머를 Privy 정책으로 복사하지 않음 |
| 이메일 변경·탈퇴·로그아웃 | 복구된 FE 화면/Mock 대상. 이메일 변경·탈퇴 확인은 Privy, 실제 지원/로컬 회원·세션 계약은 확인 대기. 실패/취소와 기존 상태를 유지. 중복 이메일을 계정 복구로 전환하지 않음. 로그아웃 성공은 A01 시작, 탈퇴 성공 L12 다음은 A02 로그인. 자체 비밀번호 확인이나 임의 Provider 전체 계정 삭제로 대체하지 않음 |
| 권한 | FE는 표시/진입 제한을 구현하고 최종 판단은 Backend가 요청 시 재검증. 로그인·활동 지역·기관 자격을 이웃 자격으로 대체하지 않음. 완료 이웃 지역 최대 3개, 기관 유효기간/담당 지역·채택 권한 구분. Mock 권한을 실제 권한처럼 저장/자동 부여하지 않음 |
| 폐기된 인증 Flow | 이웃·기관 증빙 입력/첨부/신청/제출/접수·실제 심사 화면 제외. #75 시연용 완료 자격 및 #74 상태 조회·유효/만료·배지/권한 UI는 유지. 미확인 기관 배지는 숨김. 상태 조회 실패를 신청 CTA로 바꾸지 않음 |
| 사진 역할 | FE: 선택·미리보기·JPG/PNG/최대 10장/게시물 합계 10MB 검증·업로드 상태·제거/오류·왕복 입력 보존. BE/Storage: 실제 권한·저장/연결·파일 삭제·미완료 정리. 사진 없으면 이미지 영역 생략. 24시간 정리는 서버 미완료 파일 정책이며 FE 타이머·초안 보관기간으로 구현하지 않음 |
| 알림 | NON-MVP로 제외하지 않음. 하단바/헤더 알림·알림/활동 목록·전체/알림/활동 필터·읽음/미읽음·Empty/Loading/Error·관련 상세/삭제 안내를 Mock으로 구현. 단일 푸시 수신 설정 UI 포함. 실제 이벤트 생성·읽음 저장·예약/푸시/전송은 Integration에서 검증. 푸시 OFF와 앱 내 기록 유지 구분 |
| 검색·추천 | API 부재로 제외하지 않음. 검색 입력/지역·유형·주제/결과/결과 없음/Error와 원 검색 맥락 복귀, 추천 카드/이유/새 추천/Empty/Error/Retry를 Mock 구현. 실제 검색·추천 산출은 계약 후 연결. 추천 대안 없으면 기존 카드 유지. 미정 검색 제안·이력 가중치를 임의 추가하지 않음 |
| 관심·지역 선택 보조 | 관심 지역/키워드 및 현재 위치 버튼의 후보 선택·권한/실패·수동 검색 복귀 UI는 FE 대상. 관심 설정·탐색·기본 활동·완료 이웃·기관 담당 지역을 권한으로 혼합하지 않음. 현재 위치 후보만으로 이웃 자격 자동 부여 금지 |
| 작성 보조·신고·설정 | 참고 링크·허용 범위 익명·임시 저장/성공/실패·선택적 투표 종료 전 알림·신고 자유 입력/제출 상태·설정/개인 목록 재진입은 최신 FE 대상. 세 작성 폼 왕복 입력 보존, 초안 저장과 공개 게시 분리. 실제 서버 보관/예약/발송/접수는 별도 계약과 Integration. 원 행동 성공을 알림 실패 때문에 취소하지 않음 |
| 참여·원본·개인 기록 | 최신 상세지침의 세 반응·1단계 댓글/답글·상호배타 평가·투표 제출/변경 확인·단일 상세 북마크·기관 채택 관계를 같은 원본/Service로 연결. I04는 현재 유형 필터이며 과거 반응 종류별 필터를 되살리지 않음. 투표 처리 결과 불명은 재조회 후 재시도 판단. 목록/상세/개인 기록에 별도 데이터 정본 금지 |

AI 이미지·다크 모드·운영 심사/백오피스·외부 행정 연동·채택 이후 처리 등 명시적 제품 제외는 추가하지 않는다. 복구된 추천·관심·알림·신고·설정/계정·작성 보조를 과거 NON-MVP 표기나 BE 부재로 다시 제외하지 않는다. API SPEC의 `비-MVP / 후순위`는 해당 API·서버 업무의 구현 범위와 우선순위로 해석하며, Frontend 화면·입력·상태·Mock 사용자 흐름의 제외를 뜻하지 않는다. 협업/담당 문서에서도 API 범위 표기를 FE 제외로 옮기지 않고 최신 제품·FE 범위를 적용한다. FE 복구를 이유로 API SPEC을 변경하거나 Backend 구현 범위를 확대하지 않으며 실제 계약의 미합의·불일치는 해당 연결만 확인 대기로 남긴다.

### 문서 자체 점검 결과

아래는 **이번 공통 합의 문서의 정합성 점검**이며 UI·Mock·실 API·GitHub CI를 실행한 체크리스트가 아니다.

| 점검 | 문서 반영 결과 |
| --- | --- |
| 1. BE 미구현이 FE Blocker가 아님 | 1·6·10·11절, FE 구현 후 실 API 대기 |
| 2. Mock/Contract 선행 구현 | 1·5·6절, 동일 UI·Service·Adapter |
| 3. BottomNavigation 5개 | 8절, 메인/지도/글쓰기/알림/마이·공통 구현 수정 |
| 4. 알림 FE 대상 | 8·13절, 진입·목록·상태·관련 이동과 실제 발송 분리 |
| 5. 검색/추천 FE 대상 | 6·10·13절, Mock 사용자 흐름 선행 |
| 6. 공통 컴포넌트 중복 방지 | 1·4·8절, 기존 재사용·반복 UI만 공통화 |
| 7. 기존 API Client/Mock 구조 재사용 | 6·8절, decoder/Error/Provider 포함 |
| 8. 실제 Endpoint 임의 생성 금지 | 5·11절, 미합의 계약은 실제 연결만 대기 |
| 9. UI/Mock/실 API 대기/Integration 구분 | 7·12절, 같은 정의·Issue/PR 기록 |
| 10. Loading/Empty/Error 등 상태 책임 | 6·9·12절, Success/Failure/Retry/Pending/Disabled/권한 포함 |
| 11. Figma·디자인 토큰·폰트 통일 | 8절, 기존 Pretendard·토큰·공통 UI 재사용 |
| 12. Privy 정책 | 13절, OTP·로컬 가입·계정 변경/탈퇴 확인 |
| 13. 자체 비밀번호 Flow 제외 유지 | 2·13절, 실제 지원 대기를 자체 인증으로 대체하지 않음 |
| 14. 증빙 제출 Flow 복구 금지 | 6·11·13절, 완료 자격 조회만 유지 |
| 15. 상대 구현 없는 병렬 개발 | 1·3·6·10절, 합의 interface와 Mock/stub |
| 16. 공유 파일 충돌 최소화 | 3·4·12절, 단일 편집 Owner·작은 공통 PR·최신 develop 반영 |
| 17. 상세 담당 배분 임의 변경 없음 | 2·3·10절, 기존 큰 방향/공유 경계 유지·실제 배분은 담당 문서/Issue |
| 18. 수정 파일 하나 | 이 합의사항 기존 경로만 갱신, 다른 파일의 기존 변경 보존 |

문서 검증은 정본과의 내용 대조·Markdown 구조·로컬 링크·diff 검사다. 실제 UI/Mock/Backend 실행·build·API·Storage·CORS·Integration 검증은 이번 문서 수정에서 수행하지 않았으며 해당 기능 구현 Issue에서 결과를 별도로 기록한다.

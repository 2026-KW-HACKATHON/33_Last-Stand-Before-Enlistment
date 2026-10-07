# Discushion 프론트엔드 2인 개발 시작 전 합의사항 — 병렬 개발

> 작성일: 2026-10-07 (Asia/Seoul) · 병렬 개발 정본  
> 적용 대상: FE 1 / FE 2 · 실제 저장소와 GitHub 설정은 미확인

이 문서는 첨부한 합의사항(6)·담당분배(7)를 재작성한 별도 최종본이다. 담당 배정은 **FE 1=A+D 중심, FE 2=B+C 중심**으로 적용한다. Part A~D는 제품 Domain·기능명세 추적용 분류이며 사람별 장기 브랜치가 아니다. 북마크처럼 여러 화면에 걸치는 기능은 Page 조립 책임과 데이터 계약 책임을 구분한다.

| 판단 대상 | 기준 문서 |
| --- | --- |
| 화면 흐름 참고 | `Discushion_유저플로우_와이어프레임기반_2026-10-06.md` |
| 제품 정책·MVP·기능 ID | `Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md`, `Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md` (내부 v10.2) |
| FE/BE 책임·인수 | `Discushion_MVP_백엔드_프론트엔드_통합_지침서.md` |
| API 계약안 | `Discushion_API_SPEC_v2.md` (내부 v1.1): 경로·DTO·Enum·인증 전달은 설계 제안 |
| Git 운영 정본 | `Discushion_Git_GitHub_통합협업전략_2026-10-06.md` |
| FE 협업 | `Discushion_프론트엔드_Git_GitHub_협업전략_2026-10-06_v2.md`: Git 규칙 유지, 본 문서의 병렬 접점 배분 적용 |
| 디자인 정본 | `Discushion_최종디자인_디자인기준 v2.md` 전체: 값·정렬·공통 UI·개별 예외 유지 |

제품은 확정 정책 보완·최신 MVP 범위와 기능 ID별 본문을 따른다. Git은 통합 협업전략이 우선한다. API 설계안과 Figma 공통 원본은 실제 코드/API 구현의 증거가 아니다. 실제 경로·파일·패키지·검증 명령을 저장소 확인 없이 확정하지 않는다.

## 1. 병렬 개발 원칙

**제품 데이터 의존성은 유지하되 구현 선행조건은 제거한다.** 상대 FE Feature·PR 병합 또는 Backend 구현은 개발 시작 조건이 아니다.

1. 0단계에서 저장소·환경·디자인·API 계약안을 확인하고 필요한 Contract를 합의한다.
2. 합의한 Contract의 같은 타입·호출·오류 구조를 사용해 각자 Mock으로 개발한다.
3. Contract 미합의 부분은 데이터 연동만 보류한다. 확정된 외형·레이아웃·다른 기능은 진행한다. 임의 DTO로 우회하지 않는다.
4. 공통 UI가 아직 구현되지 않아도 합의된 props·슬롯·콜백과 단일 디자인 토큰 명세를 바탕으로 개발용 stub을 주입한다. 별도 제품용 Header/Nav/Theme를 만들지 않는다.
5. 공통 컴포넌트 stub은 개발/검증 전용이며 제품 배포 경로로 노출하지 않는다. Mock PR도 build와 필수 CI를 통과해야 한다.
6. 준비된 기능부터 작은 Integration Issue로 실제 API/상대 기능을 연결한다. 전체 Track이 끝날 때까지 연동을 미루지 않는다.
7. 하나의 Page/Screen과 공유 파일은 한 Issue 담당자만 수정한다. 계약만 사용하는 소비자는 상대 파일을 직접 수정하지 않는다.
8. 공통 파일의 변경·병합 순서는 충돌 방지용 국소 조율이다. 이를 전체 Track 개발 대기로 확대하지 않는다.

공통 Contract 확인 → Issue 생성 → FE 1 / FE 2 동시 개발 → Mock 기반 독립 검증 → Feature별 PR → front/develop 통합 → 준비된 기능부터 Integration Issue → 실제 API/상대 기능 연결 → Integration 검증 완료.

## 2. 최종 담당·Domain 분배

| 담당자 | Domain | 관련 Part | 담당 기능 | 주요 Page | 기능명세 ID |
| --- | --- | --- | --- | --- | --- |
| FE 1 | Account / Authority | A | 시작·로그인·가입·이메일 6자리 인증·비밀번호 설정·약관·프로필·활동 지역·이웃 인증·권한/capabilities·returnTo | 시작, 로그인, 가입, 프로필, 이웃 인증 | F-WLXSSC, F-TSOXGG, F-KZRSXU, F-RBVFZX, F-QQKYLC, F-ATWJDJ |
| FE 1 | Institution verification | A | 기관 신청·증빙·접수·유효 상태·권한·파란 배지의 의미/계약 | 기관 인증, 인증 상태 | F-OPNIXL, S-YLSPHQ, S-JRMYIV, F-GDASNA, F-MUBDJD |
| FE 1 | Personal | D | 마이페이지·개인 활동 횟수·내가 만든/참여한 게시물·참여한 투표·북마크 목록·북마크 상태/서비스 계약 조율 | 마이페이지, 개인 목록 | F-WYMXXP, F-SSHXAA, F-NZTUYE, F-QPGNCF, F-FYQJPT |
| FE 1 | Institution adoption | D | 전체 안건 목록·지역/채택 필터·기관 채택/취소 실행·관계 계약 | 기관 안건 목록, 기관 업무 화면 | F-CNNPYL, S-PCCNUU, F-TUGMEP, S-AQOBIE |
| FE 2 | Content / Post | B | 메인·통합 게시판·지역/유형/주제 필터·지도·PostCard·공통 상세·세 유형 작성/수정/삭제·사진·AI 요약 | 메인, 게시판, 지도, 작성/수정, 상세 | F-UPRLMN, F-EAJPVC, F-QIGKAK, F-PUDHYO, F-UCDVNA, F-FTLHCX, S-NVXXYQ, S-TBFIHO, F-GSMCLD, F-WSCKDN |
| FE 2 | Participation / Share | C | 세 반응·댓글/답글·평가/정렬·실제 투표/선택 변경·공유 링크·공유 게스트 상세/댓글 | 공통 상세, 공유 게스트 상세 | F-GOMLGG, S-HNVDPO, F-EDNVWZ, S-JCEZAP, S-YYDGUS, F-CDIBRF, S-OXTKEP, F-FCPVIS, S-CMGJIG, F-OWFYWE, S-NYUECP |
| FE 2 | Detail 접점 | D 소비 | 상세의 BookmarkButton 삽입·클릭/피드백 UI, 현재 기관 채택 정보 표시 | 공통 상세, 공유 게스트 상세 | F-FYQJPT, F-TUGMEP, F-PUDHYO |

S-NVXXYQ는 게시 부분만 MVP이며 임시 저장은 제외한다. 같은 ID를 둘이 추적해도 수정 파일/화면은 겹치지 않는다. FE 1은 북마크 데이터 계약·개인 목록, FE 2는 상세 버튼 조립과 UI를 담당한다. 기관 채택 실행은 FE 1의 기관 업무 화면, 공개 상세의 기관명/시각 표시만 FE 2다. 기관 목록에서 열람하는 게시물은 같은 FE 2 상세를 사용한다.

## 3. Page Owner와 조립 경계

Page 이름은 역할 식별용이며 실제 파일명·Route가 존재한다는 뜻이 아니다.

| Page/Screen | Owner | 연결되는 타 Domain 기능 | 연결 방식 | 타 개발자 직접 수정 여부 |
| --- | --- | --- | --- | --- |
| 시작 | FE 1 | 메인 목적지 | Session/Navigation Contract | 없음 |
| 로그인 | FE 1 | 공유 상세 복귀 | returnTo/Session Contract | 없음 |
| 회원가입·이메일 인증·비밀번호·약관 | FE 1 | 공유 상세 복귀 | 가입 단계 보존/returnTo | 없음 |
| 프로필·활동 지역 | FE 1 | 탐색·작성자 표시 | UserProfile/Region Contract | 없음 |
| 이웃 인증·인증 상태 | FE 1 | 지역 참여 | Capabilities/완료 지역 Contract | 없음 |
| 기관 인증·접수/유효 상태 | FE 1 | 상세 파란 배지 | InstitutionState Contract | 없음 |
| 메인 | FE 2 | 사용자/기본 활동 지역 | Session/Profile Mock→실제 provider | 없음 |
| 통합 게시판 | FE 2 | 지역·권한 | Region/Capabilities Contract | 없음 |
| 지도 | FE 2 | 기본 활동 지역·상세 진입 | Region/Navigation Contract | 없음 |
| 게시물 작성·수정 | FE 2 | 로그인·이웃 인증·지역 | Capabilities/Region Contract | 없음 |
| PostDetailPage 공통 상세 | FE 2 | 북마크·기관 채택 표시·권한 | BookmarkState/AdoptionDisplay/Capabilities 주입 | 없음 |
| 공유 게스트 상세 | FE 2 | 로그인/가입 복귀 | 공통 상세 변형+returnTo 콜백 | 없음 |
| 마이페이지 | FE 1 | 콘텐츠 집계 | Activity/개인 목록 Contract | 없음 |
| 내가 만든/참여한 게시물·참여 투표·북마크 목록 | FE 1 | PostCard·원 상세 | PostCard props/탐색 복귀 Contract, 개발용 stub | 없음 |
| 기관 안건 목록·채택 실행 화면 | FE 1 | 안건·의견·공통 상세 | Agenda/Adoption Contract, 동일 postId | 없음 |

PostDetailPage의 PostContent·Photo·AiSummary·ReactionBar·VoteParticipation·CommentSection·ShareButton·BookmarkButton·AdoptionDisplay는 FE 2가 조립한다. FE 1은 계약/provider 또는 자신의 화면 파일만 변경한다. Integration Issue에도 조립 Page Owner를 바꾸지 않는다. 하나의 Issue 안에서 양쪽 작업이 필요하면 실제 수정 담당자 한 명과 파일 범위를 지정하고 상대는 리뷰/계약 확인을 맡는다. 분리 가능한 변경은 별도 Issue/PR로 나눈다.

## 4. 공통 코드·충돌 가능 파일 Owner

Owner는 인터페이스·변경 영향·파일 충돌·재사용 구조를 조율하는 1차 담당자다. 모든 소비 화면을 혼자 구현하는 사람은 아니다. 공동 검토 대상도 실제 파일 수정 Owner는 반드시 한 명으로 지정한다.

| 충돌 가능 영역 | Owner | 소비/리뷰 | 파일 변경 기준 |
| --- | --- | --- | --- |
| Design Token / Theme / Typography | FE 1 | 양쪽 소비, FE 2 리뷰 | 한 정의, 실제 Theme에 매핑 |
| Header / BottomNavigation / NavItem / 공통 레이아웃 | FE 1 | 양쪽 소비, FE 2 리뷰 | props/슬롯 Contract로 사용, 별도 구현 금지 |
| Input / TextArea / Profile / Verification / Attachment | FE 1 | FE 2 소비/리뷰 | 폼 규격/가변 높이 합의 |
| Auth/Session / Capabilities / returnTo / Region 의미 | FE 1 | FE 2 소비/리뷰 | provider와 계약 조율 |
| Post Type 표시 계층 / PostCard / Photo / PostDetail | FE 2 | FE 1 소비/리뷰 | 동일 원본 카드, 상세 조립 FE 2 |
| CommentItem / ReactionBar / VoteCard / VoteParticipation / Share UI | FE 2 | FE 1 리뷰/목록 소비 | 투표 표시·참여 모두 FE 2 |
| BookmarkState·서비스 / InstitutionState / Adoption 서비스 | FE 1 | FE 2 소비/리뷰 | 상세 버튼/공개 표시 파일은 FE 2 |
| Button / Chip / Modal / BottomSheet / EmptyState / Notice / Toast / Skeleton | 저장소 확인 후 단일 Owner 지정 | 공동 검토 | 새 중복 컴포넌트 없이 개발 stub 주입, 확인된 파일 Owner 우선 |
| API Client / 공통 DTO·Shared Type의 중앙 파일 | 저장소 확인 후 단일 Owner 지정 | 기능 FE/BE + BE1 | 의미별 Owner와 중앙 파일 편집자를 구분 |
| App Router / 전역 Route 등록 | 저장소 확인 후 단일 Owner 지정 | FE 1 복귀, FE 2 상세 계약 리뷰 | 각자 Page 구현, 중앙 등록만 별도 Issue |
| lockfile / 의존성 / 전역 설정·빌드/CI | 저장소 확인 후 단일 Owner 지정 | 공동 검토, 환경 영향 BE2 | 한 Issue 담당자만 변경 |

미지정 공유 영역은 0단계의 해당 Contract/공유 파일 Issue에서 실제 경로·편집자·리뷰어를 기록한다. 이는 양쪽 화면 개발의 선행 구현이 아니다. 중앙 파일 등록이 아직 없어도 합의된 인터페이스로 독립 검증하고 실제 등록은 Integration Issue로 진행한다. 실제 저장소 구조상 파일이 겹치면 더 잘게 나누거나 그 파일 수정만 한 명에게 배정한다.

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
| HTTP 응답/오류·공통 DTO 중앙 관리 | 저장소 확인 후 단일 Owner 지정 | 양쪽·기능 BE | 필요 | 기존 API Client·FE/BE 합의 |

각 계약에 의미, 최소 타입, ID 형식, required/nullable, 요청/응답, 오류, 권한, pending, 콜백/갱신 범위, 버전과 합의 Issue를 기록한다. 권한은 단일 isVerified로 축약하지 않는다. 세션 미조회 Loading과 미로그인을 구분하고 이웃/기관 상태 및 게시물 지역을 함께 다룬다. DTO 필드는 FE/BE 합의 전 임의로 선언하지 않는다.

호출 역할 예시: `getCurrentUser()`, `getPosts()`, `getPost()`, `getComments()`, `createComment()`, `toggleReaction()`, `submitVote()`, `toggleBookmark()`, `getMyPosts()`, `getParticipatedPosts()`, `getParticipatedVotes()`, `getInstitutionAgendas()`, `adoptAgenda()`, `cancelAdoption()`. 함수명은 예시다. toggle 호출도 실제 API의 합의된 등록/취소와 상태 규칙으로 매핑한다.

Contract 변경 절차: 변경 Issue 생성 → 의미별 Owner/소비자 영향 확인 → API는 기능 BE와 협의 및 BE1 정합성 확인 → 타입·Mock·클라이언트·오류·UI props의 동시 변경 범위 합의 → 담당 Feature 검증/PR → 리뷰/CI → front/develop 통합 → 소비 Feature 최신 반영 → 필요한 Integration 재검증. 호환되지 않는 변경은 소비자 이행 범위와 버전을 먼저 기록한다. 합의 전에는 해당 데이터 연결만 보류하며 양쪽 Track 전체를 멈추지 않는다.

## 6. Mock 기반 독립 개발

Mock과 실제 구현은 같은 합의 타입·호출 인터페이스를 사용한다. 화면에 fixture를 직접 박아 넣는 대신 저장소 기존 방식의 서비스/provider 경계에서 주입한다. 특정 Mock 라이브러리 설치를 전제하지 않는다. 개발 stub 역시 합의 props와 토큰을 소비하며 제품용 공통 UI의 두 번째 구현이 아니다.

| 기능 | Mock 데이터 | 검증 상태 | 실제 연결 시 교체 대상 |
| --- | --- | --- | --- |
| FE 1 계정/프로필/지역 | mockUserSession, mockUserProfile, mockRegion | 로그인·미로그인·조회 Loading/Error·가입 오류 | Auth/User/Region 서비스·provider |
| FE 1 인증/권한 | mockCapabilities, mockInstitutionState, mockVerificationState | 이웃 완료/미완료·타 지역·기관 완료/미완료/만료·접수 | 인증 상태/완료 지역·서버 권한 |
| FE 1 개인 목록 | mockMyPosts, mockParticipatedPosts, mockParticipatedVotes, mockBookmarks | 본인 기록·Empty/Error·삭제 투표·유형/상태 필터 | 개인 조회 서비스·PostCard stub |
| FE 1 기관 목록/채택 | mockInstitutionAgendas, mockAdoptionState | 담당 지역/외 지역·채택/취소·권한 없음·Pending | 기관 조회/채택 서비스 |
| FE 2 탐색/작성/상세 | mockPost, mockPostDetail, mockRegion | 세 유형·사진 없음·필터·삭제·권한·Error | Home/Map/Post/Media/AI 서비스 |
| FE 2 참여 | mockComments, mockReactionState, mockVoteState, mockCapabilities | 답글·정렬·평가 전환/취소·세 반응·표 변경 확인/취소·종료 | 참여 서비스·실제 권한 provider |
| FE 2 상세 접점 | mockBookmarkState, mockInstitutionState, mockAdoptionState | 저장 피드백·현재 채택 표시·게스트 로그인 유도 | FE 1 계약의 실제 서비스/provider |
| FE 2 공유/게스트 | mockPostDetail, mockUserSession, mockNavigationContext | 특정 공유 상세·게스트 댓글/답글·returnTo | 공유 컨텍스트·Session/Navigation |
| 양쪽 공통 UI 소비 | 합의 props의 개발 stub | 배치·문구·권한·Loading/Empty/Error/Pending | 단일 Header/Nav/Button/PostCard 등 |

공통 상태 집합: 로그인, 미로그인, 공유 게스트, 이웃 인증 완료/미완료, 타 지역, 기관 인증 완료/미완료, 권한 있음/없음, Loading, Empty, Error, Pending. 기관 접수·만료 및 Deleted/Unavailable은 추가 필수 사례다. 한 기능에 적용되지 않는 사례는 Issue에 이유와 함께 N/A로 적는다.

공유 fixture는 같은 postId/regionId·부모 commentId·voteOptionId를 참조한다. mutation Mock은 등록/취소·전환·표 대체·오류 시 상태 유지·중복 요청 규칙까지 검증한다. 단순 성공 JSON만 반환하는 Mock으로 인수를 대신하지 않는다. 권한 Mock은 FE 화면 검증용이며 BE 접근 제어 검증의 증거가 아니다.

**Mock 검증을 실 API 연동 완료로 기록하지 않는다.** 제품 실행에서는 실제 provider만 사용하고 Mock/개발 stub의 활성화는 개발 검증 범위로 제한한다. 교체 범위·남은 fixture·실제 데이터 원천을 Integration PR에 기록한다.

## 7. 완료 상태와 인수 기록

| 상태 | 인정 조건 | 인정하지 않는 것 |
| --- | --- | --- |
| UI 완료 | 합의 디자인·공통 props로 화면·상태·이동 표현 검증 | 실제 서버 저장·조회 증거 |
| Mock 연동 완료 | 합의 Contract의 Mock으로 요청·응답·권한/오류/상태 검증 | Backend/API 또는 상대 Feature 준비 |
| 실 API 연동 완료 | 합의 실제 API의 인증·요청·응답·조회/저장 확인 | 여러 Domain의 전체 흐름 인수 |
| Integration 검증 완료 | 실제 API와 실제 상대 provider/공통 UI를 연결해 원본·권한·복귀·회귀 검증 | UI/Mock PR 병합만 한 상태 |

Issue/PR에는 네 상태를 각각 기록한다. 단독으로 ‘완료’라고 쓰지 않는다. Mock 범위 Issue는 Mock 인수를 만족해 닫을 수 있지만 실 API 연결/통합 후속 Issue를 연결하고 제품 인수 상태는 남긴다.

```text
Contract: 합의됨 / 합의 Issue·버전
Data Source: Mock / 실제 API (혼합이면 기능별 표기)
UI 완료: 예/아니오
Mock 연동 완료: 예/아니오
실 API 연동 완료: 예/아니오
Integration 검증 완료: 예/아니오
API Integration: 미연동 / 관련 검증 근거
Integration Test: 미검증 / 관련 검증 근거
남은 연동: Integration Issue 링크
```

## 8. 디자인 토큰·공통 UI·레이아웃 합의

### 역할별 정본과 구현 순서

| 판단 대상 | 우선 기준 |
| --- | --- |
| 기능 동작·권한·MVP 포함 여부 | 최신 PRD·기능명세서의 확정 정책/MVP 범위·기능 ID별 본문 |
| FE/BE 책임·실제 연동·인수 | 통합 지침서 `(1)` |
| API 데이터·요청/응답·오류·인증 전달 | `Discushion_API_SPEC_v2.md` 계약안과 실제 FE/BE 합의; 설계 제안은 미확정 |
| Git·Issue·Branch·PR | 통합 Git/GitHub 협업전략 최우선 |
| 색상·폰트·크기·간격·정렬·Radius·Shadow·공통 UI 외형 | `Discushion_최종디자인_디자인기준 v2.md` |

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

기본 UI 폰트는 **Pretendard**다. font-size·line-height·font-weight·letter-spacing을 역할별 묶음으로 적용한다. 폰트 로딩 경로·방식은 실제 저장소와 에셋을 확인한다.

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

아래의 **Figma 원본 있음**은 디자인 공통 원본이 있다는 뜻이다. 실제 코드 구현 완료, 모든 화면 인스턴스 연결, 모든 Hover/Pressed/Loading Variant 완성을 뜻하지 않는다. 모든 코드 컴포넌트의 존재 여부는 실제 저장소 확인 대상이다.

| 요소 | Figma 상태 / 실제 코드 구분 | 구현 책임 |
| --- | --- | --- |
| Header / BottomNavigation / NavItem | 공통 원본 있음, 실제 코드 미확인 | FE 1, 공통 props/정렬 기준으로 양쪽 소비 |
| Input / TextArea / Profile / Attachment | 원본 있음, 실제 코드 미확인 | FE 1, 입력/증빙/프로필 규격 |
| PostCard / PostCardPhoto / Photo / PostDetail | 카드/사진 원본 있음, 실제 코드 미확인 | FE 2, 사진 유무 변형·공통 상세 |
| CommentItem / ReactionBar / Share UI | 댓글 원본 있음, 실제 코드 미확인 | FE 2, 부모/답글·참여·공유 UI |
| VoteCard / VoteParticipation | VoteCard 규격 있음, 공통 원본 제작 필요 | FE 2, 표시/제출/변경·결과를 공통화 |
| Button / Chip / Notice | 공통 원본 있음, 모든 상태 Variant/코드 완료 의미 아님 | 저장소 확인 후 단일 Owner 지정 |
| Modal / BottomSheet / EmptyState | 공통 원본 제작·규격 확정 필요 | 저장소 확인 후 단일 Owner 지정, 사용 화면 기준 |
| Toast / Skeleton / Loading | 독립 원본·상세 규격 미제시 | 저장소 확인 후 단일 Owner 지정, 미정 수치 임의 확정 금지 |

미구현 공통 UI는 합의된 props·단일 토큰 명세의 개발 stub으로 소비 화면을 검증한다. 제품용 대체 Header/Nav를 만들거나 공통 UI PR을 기다려 화면 개발을 시작하지 않는다. 실제 공통 구현 연결과 stub 제거는 Integration Issue로 진행한다.

보조 원본 `Discushion/Menu`(364 × 80px, Radius 14), `Discushion/Toggle`(108 × 44px, ON/OFF Variant)도 기존 코드와 대조한다. 존재하는 Toggle 원본 때문에 비-MVP 알림 설정·다크 모드 화면을 추가하지 않는다.

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

**메인 Header:** `Header/Main`을 사용해 일반 Header와 분리한다. 높이 58, 좌 24/우 14 패딩, gap 12. 브랜드 영역 270 × 58(x=24), 브랜드 이미지 35 × 32, 서비스명 `type/brand`, 부가 문구 9/약12.15/400. 설정/알림의 터치 영역 각각 32 × 58(x=306/350), 아이콘 32 × 32다. 아이콘 외형이 존재해도 비-MVP 설정/알림 Route·API는 만들지 않는다. MVP에서 그 액션을 어떻게 표시할지는 PO/디자인 확인 항목으로 기록한다.

**BottomNavigation 원본:** 396 × 76, 기준 y=776, Surface, Radius 0, 1px Border(Inside), `shadow/bottomNavigation`. 상하 8/좌우 16 패딩, 슬롯 gap 0. Figma 슬롯은 **메인 / 지도 / 글쓰기 / 알림 / 마이** 5개, 기준 슬롯 너비 72.8이다. 일반 NavItem 높이 53, 상하 4/좌우 2 패딩, 아이콘 정렬 영역 24 × 24, 라벨 `type/navigation` 높이 17, 아이콘-라벨 gap 4, 중앙 정렬. 선택 메뉴 Primary, 비선택 Secondary다.

**중앙 글쓰기:** 40 × 40 원형, #0F7662, 흰 `+`, 일반 라벨 없음. 원본의 슬롯 프레임 높이 32 안에서 위아래로 걸친 배치와 일반 NavItem을 구분하고 clipping을 확인한다. 아이콘 bounds(지도 도형 약 24 × 22.77), 정렬 영역(24 × 24), 터치 컨테이너를 혼동하지 않는다. 아이콘 선 굵기를 카드 Border 1px로 통일하지 않는다.

**MVP 구분:** 제품은 메인·지도·글쓰기·마이 연결을 포함하고 알림은 필수 메뉴에서 제외한다. 5슬롯 원본이 알림 기능 구현 근거가 될 수 없다. 알림 제외 후 슬롯 배열·중앙 글쓰기 위치와 Header의 비-MVP 액션 표시를 공통 기반 Issue에서 PO/디자인과 확인한다. 미합의 4슬롯 너비·대체 메뉴·예약 Route를 임의로 만들지 않는다. 확인 전 원본 72.8을 MVP의 확정 슬롯 너비로 표기하지 않고 해당 외형만 `디자인 확인 필요`로 남긴다. 다른 확정 공통 규격은 먼저 진행할 수 있다. 공유 게스트에는 일반 하단 Navigation을 제공하지 않는다. 다른 화면의 하단바 사용/미사용도 화면·MVP 기준을 확인해 Issue에 기록한다.

### 미정 UI 규격·화면 예외·디자인 전달

Modal/BottomSheet/EmptyState와 Toast/Skeleton/Profile의 미확정 세부는 필요한 사용 화면·상태·역할을 먼저 기록한다. 확정 토큰과 Button/Input 등은 재사용하되 Modal 너비, overlay opacity, Toast 지속시간, Skeleton 속도 등을 추정해 디자인 정본 값처럼 쓰지 않는다. BottomSheet는 MVP에서 쓰는 화면이 확인될 때 구현 대상으로 삼으며 공통 목록에 있다는 이유만으로 새 흐름을 만들지 않는다.

다음 기록값은 일반화하지 않는다: A01 캔버스 Radius 20(일반 목업 28), 시작 이미지 336 × 224(x=29/y=169), C01-새추천성공 스크롤 너비 380, B07-투표결과-하계2동-이웃미인증 뷰포트 높이 1354, TextArea/Chip/Toggle의 개별 높이·라벨 변형. 특히 추천 화면은 비-MVP이므로 그 예외로 MVP를 확장하지 않는다. 캔버스 Radius 28은 웹페이지·카드의 전역 Radius가 아니다.

공통 UI 없는 MVP 화면은 기능을 삭제하지 않고 **규격 확인 → 필요한 공통 정의 Issue/PR → front/develop 통합 → 기능 Issue 재사용**으로 진행한다. 공통 시스템 전체를 매 기능 Issue에서 재구축하지 않는다. 이미 통합된 컴포넌트는 props/data/합의된 변형만 사용한다. 여러 화면에 영향을 주는 새 변형은 Owner와 범위를 맞춰 별도 선행 공통 Issue로 통합한다.

대응 기록에는 Figma 원본/화면, 디자인 토큰, 실제 코드 컴포넌트·경로, Owner/Review, 상태별 구현 범위, 미정 규격, 선행 Issue/PR을 적는다. 미확인 코드 경로는 `저장소 확인 필요`로 둔다. “실제 저장소의 기존 공통 Header를 우선 사용하고, 없다면 공통 기반 Issue에서 하나의 Header를 정의한다”는 방식으로 작업한다.

Figma 참조: [최종디자인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1136-6698), [공통 UI](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1155-950), [Header/Back](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1155-1005), [Header/Action](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1155-1010), [Header/Main](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1255-6584), [BottomNavigation](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1255-6597), [NavItem](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1155-981), [ChipSelected](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1255-6582). 이 문서는 제공된 디자인 기준 전체를 반영한 개발 합의이며 새 Figma 컴포넌트나 실제 코드를 제작했다는 보고가 아니다.

---

## 9. 기술·상태·실패/복귀 기준

### 프론트엔드 기술 선택 및 상태 관리 기준

기존 개발 문서의 기술 기준은 다음과 같다. 실제 저장소 적용 여부·버전·설정은 아직 확인되지 않았다.

- Next.js
- TypeScript
- Tailwind CSS

추가 라이브러리는 실제 저장소의 `package.json`과 기존 구현을 먼저 확인한다.

이미 프로젝트에 동일 목적의 라이브러리나 공통 구조가 존재한다면 새 라이브러리를 추가하지 않고 기존 방식을 우선 사용한다.

기존 구현이 없을 경우 다음 조합은 기존 문서의 조건부 권장안으로만 유지한다. 설치·채택 완료를 뜻하지 않으며 실제 도입은 별도 필요성·저장소 확인 후 합의한다.

| 목적 | 기본 권장안 | 사용 원칙 |
|---|---|---|
| 서버 상태 | TanStack Query | 게시물·댓글·반응·투표·북마크·사용자 정보·인증 상태 |
| 폼 상태 | React Hook Form | 로그인·회원가입·프로필·게시물 작성/수정·인증 폼 |
| 폼/DTO 검증 | Zod | 이미 합의된 값만 검증하며 미정 길이·정책을 임의로 추가하지 않음 |
| Client 전역 상태 | Zustand | 필요한 경우에만 최소 사용 |
| HTTP 요청 | 기존 API Client 우선, 없으면 `fetch` | Axios를 새로 설치하는 것을 기본 전제로 하지 않음 |

### 상태 종류별 사용 기준

프론트 전체 상태를 하나의 전역 상태로 몰아넣지 않는다.

| 상태 종류 | 관리 기준 | 예시 |
|---|---|---|
| 서버 상태 | 기존 서버 상태 계층 우선; 도입 합의 시 TanStack Query | 게시물, 댓글, 반응 수, 투표 결과, 북마크, 사용자 정보, 인증 상태 |
| 전역 Client 상태 | 기존 공통 상태 계층 최소 사용; 도입 합의 시 Zustand | 회원가입 단계 유지값, Route 왕복이 필요한 작성 draft, 현재 탐색 지역 등 |
| 폼 상태 | 기존 폼 방식 우선; 도입 합의 시 React Hook Form | 로그인, 회원가입, 프로필, 게시물 작성/수정, 인증 신청 |
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
| Pending | 저장·수정·삭제·투표 등 mutation은 해당 버튼만 pending |
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

기본은 API 요청→성공 응답→서버 정본으로 갱신이며 Optimistic Update를 기본 사용하지 않는다. 선택 도입 시 rollback을 보장한다. 회원가입/인증 제출/댓글 생성/투표/삭제/채택은 서버 성공 전 성공으로 확정하지 않는다.

요청 단위 Pending·중복 클릭 차단을 적용한다. 실패 시 가입·작성/수정·댓글 입력·정상 첨부·필터·정렬·탭·스크롤을 유지한다. 투표 변경 취소는 기존 표를 유지한다. 401/403/404/409 등은 실제 합의 오류 코드와 의미에 맞추며 409를 모든 투표 변경의 필수 응답으로 단정하지 않는다. 서버 데이터는 공통 캐시/조회 경계를 공유하고 FE 1 목록과 FE 2 상세에 별도 정본을 두지 않는다.

### 개발 시작 전 기록

- [ ] 실제 FE/BE 이름·리뷰 상대, 저장소/브랜치 상태, 프레임워크·버전·패키지 매니저·lockfile 확인
- [ ] 공유 파일 실제 경로·단일 Owner·수정 범위 기록
- [ ] 각 Issue의 Contract 합의 링크/버전·Mock/provider·UI props·실제 연결 대상 확인
- [ ] API URL/인증·ID/DTO/nullable/error·실행/CORS·테스트/build 명령 확인
- [ ] MVP 하단 메뉴/브랜드 Header 비-MVP 액션 처리·미정 공통 UI 규격 확인
- [ ] 네 상태 인수·후속 Integration Issue·수정 담당자 기록

개발 착수용 Contract 합의는 최소 계약 기준이다. 실제 코드가 PR로 준비되었다는 뜻이 아니다. 미합의 항목의 연결만 보류하고 확정 영역의 작업을 계속한다.

## 10. 병렬 Track 운영

| FE 1 작업 | FE 2 작업 | 동시에 시작 가능 여부 | 상대 구현 필요 여부 |
| --- | --- | --- | --- |
| 시작·Auth·가입·프로필 | 메인·게시판·필터·PostCard | 해당 Contract 합의 후 가능 | 없음 |
| 이웃/기관 인증·상태·capabilities | 작성·수정·삭제·사진·AI 요약 | 해당 Contract 합의 후 가능 | 없음 |
| 마이페이지·개인 활동·개인 목록 | 상세·반응·댓글/답글·평가·정렬 | 해당 Contract 합의 후 가능 | 없음 |
| 북마크 목록·상태/서비스 계약 | 상세 BookmarkButton·공유·returnTo 소비 | 해당 Contract 합의 후 가능 | 없음 |
| 기관 안건 목록·채택/취소 | 현재 채택 정보 표시·지도·투표 참여 | 해당 Contract 합의 후 가능 | 없음 |

각 행은 작업 묶음의 병렬 관계이며 위에서 아래로 통과해야 하는 Gate가 아니다. 각 담당자 내부에서도 독립 Issue는 병렬 가능하다. 여러 Issue를 동시에 열면 작업 디렉터리/브랜치·파일 범위를 분리하고 동일 공유 파일은 한 명이 수정한다. 기능 개발에는 상대 구현이 필요 없지만 실제 Integration 검증에는 해당 실제 API와 provider가 준비되어야 한다.

### FE 1 Track

Account / Personal / Institution Track은 아래 독립 Issue 후보로 운영한다. 실제 Issue 번호는 GitHub 생성 결과를 사용한다.

| Issue 후보 | Contract 입력 | Mock 인수 범위 | 실제 연동 후 추가 확인 |
| --- | --- | --- | --- |
| 시작·로그인·returnTo | UserSession, NavigationContext | 실패 공통 안내·복귀·일반 메인 진입 | 실제 세션/가입 경유 복귀 |
| 가입·이메일 인증·약관 | UserSession, UserProfile, Region | 6자리·재발송·만료·발송 실패·필수 동의·단계 입력 보존 | 실제 인증/가입 저장 |
| 프로필·활동 지역 | UserProfile, Region | 닉네임/소개·저장 실패·기본/탐색 지역 분리 | 작성자 표시/기본 지역 갱신 |
| 이웃 인증 신청/상태 | Verification, Capabilities, Region | 첨부·제출·접수·최대 3개 완료 지역·접수 권한 없음 | 실제 상태·지역 권한 |
| 기관 인증 신청/상태 | InstitutionState | 정보·증빙·자료 제출·접수·유효/만료 배지 | 실제 권한·담당 지역 |
| 마이페이지·활동 횟수 | UserProfile, ActivitySummary | 본인 정보·누적 횟수·Empty/Error | 행동 이벤트 +1/+0 규칙 |
| 내가 만든/참여한 게시물 | PersonalLists, PostCard props | 유형 필터·유효 행동·postId별 한 카드·stub 카드 | 같은 원본 상세/최신 집계 |
| 참여한 투표 | PersonalLists, Vote | 본인 실제 선택·진행/종료·삭제 기록 안내 | 실제 표 변경·종료·원본 접근 차단 |
| 북마크 목록·서비스 경계 | BookmarkState, PersonalLists | 유형/주제·목록 버튼 없음·삭제 원본 제거 | 상세 저장/해제와 목록 동기화 |
| 기관 목록·채택/취소 | InstitutionState, AdoptionState, Post | 전체 안건·반응순·지역 필터·담당 지역 채택 | 실제 관계·취소 이력·공개 표시 |

PostCard와 공통 상세의 FE 2 구현을 기다리지 않는다. 개인/기관 목록은 합의 표시 모델·콜백과 개발 stub으로 검증하고 실제 PostCard/상세 연결은 작은 Integration Issue로 교체한다. 기관 채택 실행은 FE 1 업무 화면에 둔다.

### FE 2 Track

Content / Post / Participation / Share Track은 아래 독립 Issue 후보로 운영한다.

| Issue 후보 | Contract 입력 | Mock 인수 범위 | 실제 연동 후 추가 확인 |
| --- | --- | --- | --- |
| 메인·게시판·필터 | Region, PostCard, UserProfile | 지역·유형→주제·빈 섹션/CTA·기본/탐색 지역 분리 | 실제 목록/새 게시물 반영 |
| 지도 | Region, PostCard, NavigationContext | 동별 대표 안건/투표·최고 반응·동률 최신·사진 없음 | 실제 지도 원천·재조회 |
| PostCard·공통 상세 | PostDetail, Capabilities, BookmarkState, AdoptionState | 세 유형·공개 정보·사진·현재 채택·버튼 | 실제 원본/공통 UI·권한 provider |
| 작성·수정·삭제·사진 | Post, Vote, Region, Capabilities | 유형별 입력·사진 한도·투표 수정/삭제 제한·실패 보존 | 실제 저장/삭제/업로드 |
| AI 안건 요약 | PostDetail, Summary | 공개 원문 3문장·생성·실패/짧은 원문 fallback | 실제 요약/원문 버전 |
| 세 반응 | ReactionState, Capabilities | 독립 복수 선택·각 등록/취소·합계·Pending | 실제 서버 집계/개인 활동 |
| 댓글·답글 | Comment, Capabilities | 게스트 예외·1단계·대상명·금칙어 실패 입력 보존 | 실제 부모/게스트 컨텍스트 |
| 댓글 평가·정렬 | Comment, Capabilities | 상호배타 전환/취소·부모 좋아요순/최신순 | 실제 목록 집계/개인 기록 |
| 투표 참여·변경 | Vote, VoteOption, Capabilities | 선택 후 제출·변경 확인/취소·종료 제한 | 실제 한 표/본인 선택/공개 집계 |
| 공유·게스트·상세 북마크 | UserSession, returnTo, BookmarkState | 특정 상세·회원 기능 로그인 유도·복귀 후 재클릭 | 실제 세션·동일 게시물/북마크 |

Auth/권한/Header/Nav의 FE 1 구현을 기다리지 않는다. 합의된 세션·권한 Mock과 공통 UI 개발 stub으로 시작한다. FE 2가 상세의 모든 도메인 UI를 조립하며 FE 1 파일을 직접 편집하지 않는다.

## 11. Integration Issue 운영

실제 API 또는 공통 provider가 준비된 접점부터 연동한다. 전체 병렬 Track 종료는 진입 조건이 아니다. 관련 기능이 미준비이면 해당 Integration 검증만 보류하고 다른 기능은 Mock 개발/검증을 계속한다.

아래 API는 API SPEC의 **설계 제안**이며 실제 FE/BE 합의 후 사용한다. Base URL·인증 전달·DTO를 확인하지 않고 호출 경로를 확정하지 않는다.

| 연동 항목 | FE 1 | FE 2 | 관련 API | 검증 항목 |
| --- | --- | --- | --- | --- |
| [FE Integration] 로그인 상태 ↔ 게시물 권한 UI | Session/Capabilities 실제 provider | 상세/작성 조립 | POST /auth/login, /auth/sign-up; GET /users/me; GET /posts/{postId} | 미조회/미로그인 구분·본인·401/403 |
| [FE Integration] 이웃 인증 ↔ 댓글/반응/투표 | 완료 지역 상태 | 참여 UI/서비스 | GET/POST /users/me/neighbor-verifications; GET/POST /posts/{postId}/comments; PUT/DELETE /posts/{postId}/reactions/{reactionType}; PUT /posts/{postId}/vote | 접수 권한 없음·타 지역·기관 인증 대체 금지 |
| [FE Integration] PostDetail ↔ 북마크 | 상태/서비스·개인 목록 | 상세 버튼 조립 | PUT/DELETE /posts/{postId}/bookmark; GET /users/me/bookmarks | 저장 팝업·현재 상세 유지·해제/삭제·목록 버튼 없음 |
| [FE Integration] returnTo ↔ 공유 로그인 복귀 | 로그인/가입·복귀 provider | 공유 컨텍스트·진입 콜백 | GET /posts/{postId}/share-link; POST /auth/login, /auth/sign-up | 원 상세·가입 경유·자동 행동 금지·게스트 범위 |
| [FE Integration] 기관 인증 ↔ 채택/표시 | 인증·목록·채택 실행 | 파란 배지/공개 채택 표시 | GET/POST /institution-verifications; GET /officer/agendas; POST /posts/{postId}/adoptions; DELETE /posts/{postId}/adoptions/{adoptionId} | 유효/만료·담당 지역·별도 관계·기관명/시각·개인정보 비공개 |
| [FE Integration] 개인 목록 ↔ 실제 Post 원본 | 개인 화면 Owner | PostCard/원 상세 계약 | GET /users/me/posts, /users/me/participations, /users/me/votes, /users/me/activity; GET /posts/{postId} | postId별 한 카드·현재 행동/누적 횟수 분리·삭제 투표 접근 제한 |
| [FE Integration] 공통 UI ↔ Page/Route | Header/Nav/Theme·복귀 계약 | PostCard·상세/메인 조립 | API 없음, 실제 Router/공통 UI 계약 | stub 제거·단일 토큰·MVP 메뉴·레이아웃/뒤로가기 |

표의 넓은 항목은 접점 목록이다. 한 PR이 과대해지면 로그인/가입, 댓글/반응/투표, 기관 표시/채택 등 독립 검증 가능한 작은 Issue로 나누고 각 Issue는 1 Feature/1 PR을 유지한다. 각 Integration Issue에 수정 담당자 한 명·관련 Page Owner·실제 파일·Contract 버전·교체할 Mock/stub·실 API 인수 사례를 지정한다. API 서비스 교체가 가능해도 실제 상대 provider가 없으면 Integration 검증 완료로 올리지 않는다.

Integration PR은 해당 경계의 Mock/stub을 실제 서비스로 교체하고 실제 상대 기능 연결, 권한·오류·상태·캐시 갱신·회귀를 확인한다. 개발용 fixture는 검증 용도로 남길 수 있으나 제품 경로에서 사용하지 않는다. FE 1 목록과 FE 2 상세는 postId를 공유하고 화면별 사본 데이터를 정본으로 만들지 않는다.

## 12. Git·Issue·PR 운영

**1 Issue → 1 Feature → 1 PR → front/develop**. Mock·Contract·Integration Issue 모두 같은 규칙을 사용한다.

GitHub Issue → 최신 front/develop → front/feature/<issue번호>-<기능명> → 구현 → 관련 test/build → push → PR(base=front/develop) → 상대 리뷰/필수 CI → merge.

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

PR에는 Issue 연결·기능명세 ID·변경 범위·Contract/API/공통 UI 영향·네 완료 상태·검증 근거·후속 Integration Issue를 기록한다. 작성자가 자기 PR을 단독 승인/병합하지 않는다. 리뷰/필수 CI 통과 후 GitHub PR로 병합하고 Issue 종료·Feature 삭제를 확인한다. 충돌은 담당 Feature에서 요구사항을 보존하며 해결·재검증한다. 전부 ours/theirs 선택이나 타인 변경 삭제는 금지다.

금지: front/develop/main/back/develop 직접 push 또는 로컬 merge 결과 직접 반영, Part/개인 장기 브랜치·release·단일 develop, Issue 없는 개발, 상대 Feature 직접 merge, 미검증/미승인 PR 병합, 무관한 전역 수정, 무통보 계약 변경, secret commit.

FE/BE 코드는 서로의 Feature/develop을 merge해서 연결하지 않는다. 기능 API는 FE 1↔BE1(A/D), FE 2↔BE2(B/C)와 협의하고 API 정본/DTO 정합성은 BE1, 실행환경·환경변수·CORS는 BE2와 확인한다. 최종 main 반영은 검증한 front/develop/back/develop 각각의 PR이다. 두 번째 PR은 첫 병합 후 최신 main을 다시 확인하며 필요한 main→develop 동기화도 PR로 처리한다.

Codex 요청은 Issue 하나의 범위로 제한한다. Issue·담당·현재 Feature·base·허용/금지 파일·Contract·Data Source·인수 상태를 지정한다. Codex가 보호 브랜치에 push하거나 PR을 병합하지 않는다. 작업 후 사람이 status/diff/staged diff를 검토한다.

### Issue/PR 공통 기록 양식

```markdown
- 목표/담당자/리뷰어:
- Domain/Part/기능명세 ID:
- 실제 Page Owner/수정 파일/공유 파일 Owner:
- Contract: 합의 Issue·버전 또는 미합의 항목
- Data Source: Mock / 실제 API (기능별)
- Mock/stub 주입 및 실제 교체 대상:
- UI 완료: 예/아니오 · 근거
- Mock 연동 완료: 예/아니오 · 근거
- 실 API 연동 완료: 예/아니오 · 근거
- Integration 검증 완료: 예/아니오 · 근거
- 관련 test/build·권한/오류/복귀 검증:
- 공통 변경/API 영향 및 후속 Integration Issue:
```

## 13. 제품 정책·통합 검증·최종 점검

통합 검증은 실제 응답·원본 ID·권한/오류·mutation 이후 재조회·화면 이동을 근거로 남긴다. Mock 결과와 별도 기록한다. 관련 test/build와 필수 CI를 실행하고 실행하지 못한 검증은 미검증으로 적는다.

| 상태 | 인정 조건 | 인정하지 않는 것 |
| --- | --- | --- |
| UI 완료 | 합의 디자인·공통 props로 화면·상태·이동 표현 검증 | 실제 서버 저장·조회 증거 |
| Mock 연동 완료 | 합의 Contract의 Mock으로 요청·응답·권한/오류/상태 검증 | Backend/API 또는 상대 Feature 준비 |
| 실 API 연동 완료 | 합의 실제 API의 인증·요청·응답·조회/저장 확인 | 여러 Domain의 전체 흐름 인수 |
| Integration 검증 완료 | 실제 API와 실제 상대 provider/공통 UI를 연결해 원본·권한·복귀·회귀 검증 | UI/Mock PR 병합만 한 상태 |

Issue/PR에는 네 상태를 각각 기록한다. 단독으로 ‘완료’라고 쓰지 않는다. Mock 범위 Issue는 Mock 인수를 만족해 닫을 수 있지만 실 API 연결/통합 후속 Issue를 연결하고 제품 인수 상태는 남긴다.

```text
Contract: 합의됨 / 합의 Issue·버전
Data Source: Mock / 실제 API (혼합이면 기능별 표기)
UI 완료: 예/아니오
Mock 연동 완료: 예/아니오
실 API 연동 완료: 예/아니오
Integration 검증 완료: 예/아니오
API Integration: 미연동 / 관련 검증 근거
Integration Test: 미검증 / 관련 검증 근거
남은 연동: Integration Issue 링크
```

| 영역 | 양쪽에 동일하게 적용할 제품 정책/통합 인수 |
| --- | --- |
| 계정 | 이메일/비밀번호 로그인(인증번호 없음), 가입은 이메일 6자리→비밀번호 확인→필수/선택 동의→프로필→활동 지역. 로그인 실패는 `로그인에 실패했습니다`. 닉네임 중복 불가·최대 10자, 소개 최대 50자 |
| 권한 | 지역 게시/댓글/답글/반응/평가/투표는 해당 지역 이웃 인증 완료 회원. 기관 인증·활동 지역은 이를 대체하지 않음. 완료 지역 최대 3개. FE 표시와 BE 거부를 모두 검증 |
| 인증 | 신청·증빙·제출·접수까지 MVP. 접수/첨부만으로 완료 금지. 시연 완료 상태는 개발자가 설정 가능하나 실제 서버 상태/지역에 연결. 운영자 심사 비-MVP |
| 기관 | 실제 민원 담당자 인증, 기관명/부서/직책/담당자/업무 이메일/전화/담당 지역/재직증명서. PDF/JPG/PNG 개수 제한 없음, 파일당 10MB·요청 전체 50MB. 업무 이메일 별도 6자리 인증 없음. 1년 유효기간·만료 시 역할/권한/파란 배지 제거 |
| 탐색/지도 | 기본 활동 지역과 임시 탐색 지역 분리. 지역→유형→주제, ‘전체’는 조회 필터. 주제 교통/주거/안전/복지/생활정보/환경/기타. 지도 동별 안건/투표 최고 반응 1개·동률 최신, 활동 정보 제외·첫 첨부 사진·재조회 갱신 |
| 게시물 | 지역 안건/지역 활동 정보/투표 3유형 공통 원본. 게시 후 원 상세. 안건 진행 상태 없음. 활동 출처/일정/장소/상태 필수, 상태 기본값 없이 예정/진행/종료/취소 수동 변경, 종료/취소 외부 참여 비활성. 문의는 현재 등록 로그인 이메일이며 없으면 전용 안내 |
| 사진 | 선택 JPG/PNG 최대 10장·게시물 전체 합산 10MB. 첨부/교체/제거/취소 시 입력 보존. 사진 없으면 영역 생략, 대체 이미지 없음 |
| AI | 공개 안건 원문 기반 3문장 한 문단, 원문 없는 사실 금지·생성 상태·실패/짧은 원문 fallback·원문 열람. 추천 없이 동작 |
| 반응 | 공감해요/필요해요/궁금해요 각각 독립 등록/취소·복수 선택. 총 반응은 합계, 댓글/득표와 별도 |
| 댓글/답글 | 원 부모 아래 1단계, 답글에 답해도 동일 부모+대상명. 평가 좋아요/싫어요 상호배타·반대 전환·재선택 취소. 좋아요순은 부모 수 내림차순/동률 최신, 답글 평가 합산 금지. 최신순 부모 시각. 사전 정의 문자열 포함 금칙어 필터, 실제 단어 목록은 합의 |
| 투표 | 독립 게시물·선택지 2~10개·종료 시각. 선택 후 제출, 다른 선택 제출 시 변경 확인/취소. 진행 중 제목/본문/사진/종료 시각만 수정, 질문/선택지/지역/주제 수정 금지. 진행 중 작성자 삭제 가능, 종료 후 수정/삭제·표 제출/변경 금지. 공개 집계와 로그인 본인 선택 분리, 타인 개인 선택 비공개 |
| 게스트/returnTo | 특정 공유 상세의 본문/사진/댓글/반응 수/투표 결과 열람 및 `게스트` 댓글/답글만 예외 허용. 일반 메인/게시판/전체 지도·평가/반응/투표/북마크 불가. 회원 기능은 `로그인이 필요한 기능입니다.` 안내→로그인. 가입 중도 returnTo 보존, 복귀만 하고 행동 자동 실행 금지·게스트 댓글 자동 이관 없음 |
| 북마크 | 상세 동일 버튼의 회원 등록/해제, 성공 `저장되었습니다` 팝업·현재 상세 유지. 게스트 로그인 복귀 후 재클릭. 목록 유형/주제 필터·카드 버튼 없음. 게시물 삭제 시 관계/목록 제거 |
| 개인 기록 | 내가 만든/참여한 게시물은 유형 필터, 참여 투표 진행/종료·실제 본인 선택. 참여 목록은 반응/댓글/답글/평가/투표 현재 유효 행동을 postId별 한 카드로 통합; 북마크/작성만·타인 행동 제외. 삭제 투표 기록 유지하되 본문/선택지 노출 금지 |
| 활동 횟수 | 게시/북마크 등록/각 반응 등록/댓글·답글 작성/평가 최초 등록/투표 최초 참여 +1. 취소/평가 전환/표 변경/조회/수정/삭제 +0, 취소 후 재등록 +1. 누적 횟수와 현재 관계 수·목록 카드 수 분리, 재시도 중복 집계 금지 |
| 채택 | 유효 기관 인증자는 전체 공개 안건/주민 의견 열람, 반응 합계 내림차순·지역 필터. 담당 지역 공개 지역 안건만 채택. 기관↔안건 별도 관계·기관별 독립 채택/취소·취소 이력 내부 유지. 현재 기관명/채택 시각만 공개, 담당자 개인정보 비공개·게시물 상태 변경 금지 |
| 동일 원본/복귀 | 게시물/사진/댓글/반응/표/북마크/채택/개인 기록 동일 원본. 등록/취소/표 변경/삭제 후 관련 캐시 재조회. 목록→상세→뒤로가기 지역/유형/주제/정렬/탭/스크롤 맥락 유지 |

후순위는 이번 병렬화로 추가하지 않는다: AI 추천/이미지 생성, 관심 지역/키워드, 알림/예약/설정, 신고/운영 심사, 비밀번호 찾기·이메일/비밀번호 변경·탈퇴·다크 모드, 안건 임시 저장·참고자료·익명 작성, 행정 후속 처리/실제 해결. 알림 없이 참여·투표·채택·개인 기록이 동작해야 한다.

이웃 증빙 종류/파일 제한·프로필 사진 제한·미명시 필드 길이/URL/일정 구조·실제 금칙어·삭제 보존 세부·인증 전달·지도/AI 원천은 해당 FE/BE/PO 합의 대상이다. 기관 증빙/게시 사진의 확정 제한을 이웃 증빙에 전용하지 않는다.

### 문서 자체 점검 결과

| 점검 | 결과 |
| --- | --- |
| FE 1/FE 2가 상대 Feature를 기다려야 하는 개발 시작 조건 | 없음, 미합의 Contract의 데이터 연결만 보류 |
| 동일 Page 두 사람 조립 | 없음, 단일 Page Owner |
| Contract 합의 후 동시 개발·BE 없는 Mock 개발 | 가능, 서비스/provider·공통 UI stub 경계 명시 |
| UI/Mock/실 API/Integration 상태 분리 | 네 용어·Issue/PR 필드 일치 |
| 작은 Integration Issue·단계별 실제 연결 | 준비된 접점부터 진행, 전체 Track 종료 Gate 없음 |
| Git 정본 유지 | Issue/Feature/PR·보호 브랜치·리뷰/CI·공유 이력 규칙 유지 |
| 제품/MVP·디자인 유지 | 정본 정책·토큰·외형/미정/예외 유지 |
| 두 문서 담당·Owner·Contract·Mock·Integration | 동일 표와 정의 적용 |

이 점검은 문서 설계의 일관성 확인이다. 실제 저장소·GitHub 보호 규칙·API 구현·제품 Integration 검증을 수행했다는 뜻이 아니다.

# Discushion 프론트엔드 2인 담당 분배·병렬 개발 순서

> 적용 기준: 2026-10-07 (Asia/Seoul) · 실행용 담당분배 · 기존 파일명/경로 유지
> FE1 = Part A·D 중심 / FE2 = Part B·C 중심. 제품 정책·상세 동작은 새로 정하지 않는다.

최신 FE 범위를 기능 Owner·Page Owner·공유 파일 편집 Owner에 연결한다. **FE 대상 기능에는 반드시 한 명의 Owner가 있고, 그 담당자가 UI + Mock 사용자 흐름 + 디자인 품질을 책임진다.** Backend 부재로 담당을 제거하지 않으며 실제 API가 준비되면 같은 기능 담당자가 Integration을 이어간다.

| 판단 대상 | 참조 정본 |
| --- | --- |
| 제품·권한·MVP·기능 ID | [PRD v10.2](../specs/Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md), [기능명세서 v10.2](../specs/Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md)의 최신 확정 정책·범위 대조·ID별 본문 |
| FE 복구 범위 | 최신 제품·통합·FE 상세지침서의 17개 복구 + 검색/현재 위치 보조 2항목. 오래된 NON-MVP 표로 되돌리지 않음 |
| 이동·취소·복귀·상태 보존 | [최신 유저플로우](../specs/Discushion_유저플로우_와이어프레임기반_2026-10-06.md)의 현재 정책 본문 |
| FE/BE 책임·통합 상태 | [MVP 통합지침서](../specs/Discushion_MVP_백엔드_프론트엔드_통합_지침서.md) |
| 화면별 구현·Task·QA | [FE 개발 상세지침서](../frontend/Discushion_MVP_프론트엔드_개발_상세지침서.md) 6절의 157 Frame 매핑, 12·16절 공통 기반, 51~53절 Task 및 FE-R01~19, 58절 QA |
| 공통 개발 규칙 | [개발시작 전 합의사항](./Discushion_프론트엔드_2인_개발시작전_합의사항_병렬개발.md) |
| 디자인 | [Figma KW 해커톤 디자인 / 최종디자인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1136-6698), [디자인기준 v2](../design/Discushion_최종디자인_디자인기준%20v2.md) |
| API·Git·저장소 작업 | [API SPEC v2](../api/Discushion_API_SPEC_v2.md)의 현행 계약과 실제 합의, [통합 Git 전략](./Discushion_Git_GitHub_통합협업전략_2026-10-06.md), [FE Git 전략](./Discushion_프론트엔드_Git_GitHub_협업전략_2026-10-06_v2.md), [AGENTS.md](../../AGENTS.md) |

제품·권한은 PRD/기능명세, 화면 상세는 FE 상세지침서, 이동은 유저플로우, 외형은 Figma/디자인기준, 공통 작업 방식은 합의사항을 따른다. 현재 코드나 과거 협업 문서의 제외 문구로 최신 FE 범위를 축소하지 않는다. 독립 FE 복구목록 파일은 현재 작업공간에 없어 위 정본의 복구 대조표로 확인했으며 새 파일을 만들지 않았다.

### 현재 구현과 목표의 구분

2026-10-07 `git fetch origin front/develop back/develop` 후 FE **4f4f4d5f7b51991c0eef3fa13a33d09d7d77a06c**, BE **452debdfd4971a8a5756c9d3783d5d5ffff1545b**를 정적으로 확인했다. 최종디자인 metadata의 157개 Frame ID·이름은 최신 FE 상세지침서와 일치한다. 일반 FE 108 + Privy 대체 27 = **FE 대상 135개**, 제품 예외 22개다. Frame/상태 수는 URL이나 독립 Issue 수가 아니다. 이번 문서 수정은 UI/Mock/실 API 구현 완료나 전 화면 시각 QA 보고가 아니다.

| 현재 코드 근거 | 배분 시 판정 |
| --- | --- |
| `frontend/src/app/page.tsx`는 `return null`; `layout.tsx`는 html/body·globals | 제품 Route/Page 신규 구현 필요. 아래 화면 Task를 기존 완료로 취급하지 않음 |
| `components/ui/{Button,Input,TextArea,Notice}.tsx`, `components/layout/{Header,BottomNavigation,MobileLayout}.tsx` | 공통 구현 재사용. 두 명에게 신규 베이스 제작을 중복 배정하지 않음 |
| Header는 title/onBack/rightAction, BottomNavigation은 메인/지도/작성/마이 4개 | Header 브랜드·설정/알림과 5메뉴는 부분 구현/수정 필요. 공통 수정 Owner FE1 |
| `lib/api` Client·ApiError·decoder·Mock·Provider, `globals.css` 토큰·Pretendard·기존 4개 아이콘 | 재사용 기반. 루트 주입·기능별 Service/fixture/실 인증 연결 완료는 아님 |
| BE main Java는 Application·HealthController, 별도 SQL Schema/테스트 산출물 존재 | 제품 API 미구현. 업무 로직·계약은 별도 확인. Schema/health 존재를 기능 완료로 해석하지 않음 |

## 1. Shared Initial — 동시에 시작할 최소 계약

**기존 역할을 유지한다: FE1은 A·D, FE2는 B·C 중심이다.** 새 Part 체계를 만들지 않는다. `A-01`/`D-01`/`B-01`/`C-01`/`SH-01`/`INT-01`은 이 문서 내부 작업 식별자이며 기능명세 ID나 GitHub Issue 번호가 아니다.

`BE API 없음 → 기존 기능 담당 지정 유지 → UI/Client State·Mock/Contract 구현 → 실 API 연동 대기 → 같은 담당자가 Integration`

Contract 미확정이어도 FE 담당은 유지한다. 확정 UI·Client State·개발용 Mock·임시 Service interface와 요구 계약은 준비하며 미확정 Method/Endpoint·Backend DTO·Provider 지원을 실제 계약으로 확정하지 않는다. 정책 충돌은 해당 항목만 확인하고 독립 화면 개발을 계속한다.

| 착수 시 최소 합의 | 조율 Owner | 상대가 바로 할 수 있는 것 |
| --- | --- | --- |
| 세션·로컬 가입·Capabilities·Region·returnTo 의미 | FE1 | FE2는 합의 타입·상태 Mock으로 자신의 Page/권한 UI 구현 |
| Post 표시 모델·PostCard props·상세 진입/복귀 콜백 | FE2 | FE1은 동일 표시 모델과 카드 stub으로 개인/기관 목록 구현 |
| 공통 Header/Nav/Layout props·논리 목적지·active 규칙 | FE1 | 두 사람 모두 기존 UI/합의 stub을 소비해 각자 Route/Page 구현 |
| Service interface·오류/nullable·Mock data shape | 각 기능 Owner, 중앙 파일 편집 FE1 | 각자 기능 fixture/상태를 구현. 실제 데이터 연결만 Contract 확인 대기 |

이 합의는 짧은 인터페이스 확인이며 공통 코드 전체 완성을 기다리는 단계가 아니다. Day 1부터 FE1은 A-01/A-02 또는 개인 목록, FE2는 B-01/B-02 또는 상세를 각각 Mock으로 시작할 수 있다. 미준비 컴포넌트는 개발용 stub으로 주입하고 제품용 복제 Header/Nav/Card를 만들지 않는다.

## 2. FE1 담당 — Account / Authority / Personal / Institution

| 기능군 | 담당 범위 | 대표 작업 |
| --- | --- | --- |
| Account·Onboarding·Profile·Region | 시작·Privy 로그인/OTP·약관/로컬 가입·프로필·활동 지역·현재 위치 후보·returnTo·계정 정보/이메일 변경/로그아웃/탈퇴 | A-01~08 |
| Authority | 이웃 완료 지역·기관 유효/만료·담당 지역·배지/제한 UI·세션/권한 Contract. 증빙 입력/첨부/신청/접수/심사 제외 | A-04 |
| Personal | 마이/누적 활동·북마크 Service/목록·내가 만든/참여한 게시물·참여 투표·관심 지역/키워드·알림/활동·수신 설정·설정/개인 목록 재진입 | D-01~05, D-08~12 |
| Institution | 기관 전체 안건·담당 지역·채택 검토/실행/취소·채택 기록. 공개 상세 표시에는 같은 관계 Contract 제공 | D-06~07 |
| Shared/Common | 기존 레이아웃/토큰/Header/5메뉴·기본 입력·상태/피드백 UI·API/Mock 기반의 수정/중앙 등록 Owner | 3절 Shared 표 |

관심·알림·설정·계정 기능을 FE1 흐름 안에 모아 상태/복귀 정책을 일관되게 구현한다. 북마크 데이터/개인 목록과 기관 관계/업무 실행은 FE1, 상세 버튼·공개 표시는 기존대로 FE2가 조립한다.

## 3. FE2 담당 및 Shared/Common 단일 Owner

FE2는 기존 **Content / Post / Participation / Share, Part B·C 중심**을 유지한다.

| 기능군 | 담당 범위 | 대표 작업 |
| --- | --- | --- |
| Content·탐색 | 메인·게시판·탐색 지역·자유 검색/결과·AI 의제/개인 맞춤/새 추천·지도·공통 상세·AI 요약/원문 | B-01~05, B-09~10 |
| Post | 세 유형 글쓰기·사진·수정/삭제·참고 링크·허용 익명·임시 저장·투표 종료 전 알림·입력 보존 | B-06~08, B-11~14 |
| Participation·Share | 세 반응·댓글/답글·댓글 평가/정렬·투표 참여/변경·신고·공유/게스트·returnTo 소비 | C-01~05, C-07 |
| 상세 접점 | 상세 북마크 버튼·공개 기관 채택 표시. FE1의 데이터/관계 Service를 소비 | C-06 |
| Shared/Common | PostCard/Photo/PostDetail·VoteCard·CommentItem/참여·Share 도메인 컴포넌트와 계약 Owner | 아래 Shared 표 |

### 공통 작업·충돌 가능 파일 Owner

아래 Owner는 실제 편집 조율자 한 명이다. 기존 확정 Owner는 유지하고 미지정 중앙 영역은 FE1로 명확히 배정한다. 소비자는 필요한 변경을 Owner에게 전달하거나 작은 별도 Task로 분리한다. 현재 없는 후보 컴포넌트는 반복 사용이 확인될 때만 공통화하며 한 화면 UI를 무조건 공통 영역에 올리지 않는다.

| 공통 작업 | Owner | Consumer/리뷰 | 현재 상태·수정 범위 | 완료 기준 |
| --- | --- | --- | --- | --- |
| SH-01 root layout·MobileLayout·전역 Navigation/Route 등록 | FE1 | 양쪽 / FE2 리뷰 | 기존 재사용, root Provider 주입/목적지 등록은 필요 시 수정. 각자 Page 파일은 각 Page Owner | 합의 slot·스크롤·safe-area·논리 목적지 연결, 상대 Route 호환 |
| SH-02 globals.css·Theme/Design Token·Typography·폰트 | FE1 | 양쪽 / FE2 리뷰 | 기존 토큰·Pretendard 재사용. 부족한 역할/화면 차이만 공통 수정 | Figma/디자인기준 v2 매핑·중복 토큰/임의 폰트 없음 |
| SH-03 공통 Header 계열 | FE1 | 양쪽 / FE2 리뷰 | 일반 Header 재사용·Main 브랜드/설정/알림 variant·action/정렬 보완 | 뒤로가기/제목/로고/우측 action 통일, 설정 L01·알림 H01 연결 |
| SH-04 BottomNavigation·NavItem·공통 아이콘 | FE1 | 양쪽 / FE2 리뷰 | 현재 4개를 메인/지도/글쓰기/알림/마이 5개로 수정·알림 자산 보완 | 5열·Route/active·label·spacing·선택 스타일·safe-area·게스트 비노출 |
| SH-05 Button/Input/TextArea/Notice·ProfileRow·SettingRow·Attachment 기본 UI | FE1 | 양쪽 / FE2 리뷰 | 기존 베이스 재사용·필요한 상태/props 확장. Attachment는 허용 사진 선택 UI이며 증빙 제출 아님 | native/aria·disabled/pending·오류/피드백·동일 props/토큰 |
| SH-06 CategoryChip/FilterChip·공통 Modal/BottomSheet/ConfirmDialog | FE1 | 양쪽 / FE2 리뷰 | 반복 후보 신규/합의 variant; 필터 의미·선택 상태는 각 화면 Owner | 단일 프레젠테이션 API·확인/취소·접근/복귀·단일 토큰 |
| SH-07 EmptyState/ErrorState/LoadingState·Toast/Skeleton | FE1 | 양쪽 / FE2 리뷰 | 반복 후보. 미정 overlay/시간/속도 임의 확정 금지 | 정상과 같은 레이아웃·Retry/피드백 인터페이스·중복 베이스 없음 |
| SH-08 Auth/Session·Capabilities·Region·returnTo 공통 provider | FE1 | FE2 소비/리뷰 | 합의 타입·개발 상태/실 Provider 주입 연결, 현재 세션 구현 완료로 취급하지 않음 | 초기 미조회/미로그인/가입 미완료 구분·실 권한은 서버 판단 |
| SH-09 API Client·ApiError·response decoder·공통 types 중앙 파일 | FE1 | 양쪽·기능 BE / FE2 리뷰 | 기존 lib/api 재사용. 도메인 DTO 의미는 각 기능 Owner·BE1 합의, 중앙 편집만 FE1 | 동일 오류/decoder·실제 인증 합의·기존 클라이언트 복제 없음 |
| SH-10 Mock Infrastructure·ApiClientProvider·조립 지점 | FE1 | 양쪽 / FE2 리뷰 | 기존 createMockApiClient·Provider 재사용·공통 선택/주입 연결. 모든 도메인 Mock을 FE1에 몰지 않음 | 같은 Service의 Mock/Real 교체·production Mock 금지·시나리오 주입 |
| SH-11 Post 표시 모델·PostCard·Photo·PostDetail | FE2 | FE1 소비/리뷰 | 제품 도메인 UI 신규. 사진 선택 베이스 SH-05를 소비하고 게시물 사진 로직은 B-07 | 카드/상세 같은 원본·사진 유무·목록/기관 stub 교체 |
| SH-12 VoteCard/VoteParticipation·CommentItem·ReactionBar·Share UI | FE2 | FE1 소비/리뷰 | 제품 도메인 UI 신규·참여/게스트 상태와 props 공유 | 동일 투표/댓글/반응 모델·복수 소비 화면에서 중복 없음 |
| SH-13 NotificationItem·북마크/개인 목록·기관 관계 Service | FE1 | FE2 소비/리뷰 | 신규 도메인 UI/Service·Contract. FE2 상세 버튼·공개 표시 파일은 C-06 | 알림/개인/기관 상태와 원 상세 연결·동일 관계 |
| SH-14 lockfile·전역 dependency/config·빌드/CI 수정 창구 | FE1 | FE2 리뷰, 환경 영향 BE2 확인 | 기존 설정 재사용. 기능 편의로 변경하지 않고 꼭 필요할 때 별도 공통 Task | 합의 범위·영향·실제 검증 기록; 새 CI/패키지 도입 확정 아님 |

Shared 표의 A/D/B/C Service·도메인 UI 책임은 아래 기능 작업의 동일 구현을 가리키며 두 번째 구현/Issue를 만들라는 뜻이 아니다. SH-08/13과 A-04·D-02/07/10이 겹치는 계약은 같은 파일/타입을 재사용한다. 별도 공유 파일 변경만 작은 SH 작업으로 분리한다.

SH 작업은 공유 파일에 대한 작은 PR로 먼저 통합한다. 소비 Feature는 합의 interface로 먼저 개발하고 통합 후 최신 `front/develop`을 받는다. **같은 root layout/globals/Header/Nav/provider/types/API 파일을 양쪽이 동시에 크게 편집하지 않는다.** 도메인별 Service/fixture는 각 담당 파일에 두고 중앙 등록만 FE1이 편집한다.

## 4. Page Owner와 135개 FE Frame 추적

Page 조립·하위 Component·Service Owner를 구분한다. 아래 Frame 그룹은 FE 상세지침서 6절의 실제 node ID/화면명을 기준으로 분할하며 **표에 적힌 WF 코드의 FE 대상 모든 variant**를 포함한다. 같은 코드의 중복명 A02도 node ID별로 포함한다. 그룹의 여러 Task는 같은 Page에 조립되는 기능이며 중복 Page 배정이 아니다.

| 화면군 | 포함 WF 코드 (대상 variant 전체) | Frame 수 | Page Owner | 연결 작업 |
| --- | --- | ---: | --- | --- |
| 시작·Privy 로그인 | A01, A02, G02 | 4 | FE1 | A-01 |
| 최초 가입·약관·프로필/지역 단계 | A06, A07, A08, A09 | 7 | FE1 | A-02/A-03 |
| 메인·탐색 지역·게시판/추천 | B01, B02, B03, B04, B05, C01 | 7 | FE2 | B-01/B-02/B-04 |
| 검색·결과 상태 | C02, B12, B13, B14 | 6 | FE2 | B-03 |
| 지도·오류 | D01, D03, B08 | 3 | FE2 | B-05 |
| 유형 선택·세 작성 폼 | E01, E02, E03, E04 | 4 | FE2 | B-06/B-11~14 |
| 사진 선택/미리보기 | E05 | 1 | FE2 | B-07 |
| 수정·삭제 확인 | E06, E08, E09 | 3 | FE2 | B-08/B-14 |
| 회원·공개 상세/결과·본인 상세 | F01, F02, F03, F04, F09, F11, F12, F16, F17, F18, F19, F24, F25, B06, B07 | 15 | FE2 | B-09/B-04/B-08/C-01~04/C-06 |
| 참여 제한·회원 기능 안내 | F20, B09, B11 | 3 | FE2 | B-09/C-05 |
| 상세 북마크 저장 피드백 | F07 | 1 | FE2 | C-06 (D-02 Service 소비) |
| 신고 입력 | F08 | 1 | FE2 | C-07 |
| 공유·게스트/복귀 상세·링크/답글 | G01, G04, G05, G07, G08, G09 | 6 | FE2 | C-02/C-04/C-05 |
| AI 요약·원문 | B10, F21, F22, F23, G06 | 5 | FE2 | B-10 |
| 알림·활동·빈/삭제 안내 | H01, H02, H03 | 5 | FE1 | D-10 |
| 마이 | I01 | 1 | FE1 | D-01/D-12 |
| 북마크 목록·유형/주제 | I02 | 11 | FE1 | D-02 |
| 내가 만든/참여한 게시물 | I03, I04 | 12 | FE1 | D-03/D-04 |
| 참여 투표·진행/종료 | I05 | 3 | FE1 | D-05 |
| 프로필·기본 활동 지역 | I06, I07 | 3 | FE1 | A-03 |
| 관심 지역 | I08 | 3 | FE1 | D-08 |
| 기관 안건 목록·필터 | K01 | 3 | FE1 | D-06 |
| 기관 채택·타 지역 열람·기록 | K02, K03, K04 | 3 | FE1 | D-07 |
| 설정 | L01 | 1 | FE1 | D-12/A-07 |
| 계정 정보·변경 완료/마이 진입 | L02 | 4 | FE1 | A-05/A-06 |
| Privy 이메일 변경·확인/실패 | L03, L04 | 11 | FE1 | A-06 |
| 관심 키워드·저장 실패 | L07 | 3 | FE1 | D-09 |
| 푸시 수신 설정 | L08 | 1 | FE1 | D-11 |
| Privy 탈퇴 확인·완료 | L09, L10, L11, L12 | 5 | FE1 | A-08 |
| **합계** | **157개 중 FE 대상만 포함** | **135** | **FE1 80 / FE2 55** | 미배정 0·중복 배정 0 |

제품 예외 **22개**는 작업으로 배분하지 않는다: A03~A05 및 L05/L06의 자체 비밀번호 17개, J01~J05 증빙 5개. 반대로 L03/L04/L10의 과거 비밀번호 확인 표현은 Privy 확인·실패/취소 UI로 대체하여 FE1 대상에 포함한다. I04의 과거 반응 종류별 variant도 화면/참여 표시의 FE1 책임으로 유지하되 현재 유형 필터로 수정한다.

독립 Frame이 없는 자격 상태·Loading/Error/Pending·로그아웃·초안/업로드 결과 등은 아래 작업의 상태로 담당을 지정한다. Shared Header/Nav는 135개 화면의 별도 중복 Page로 세지 않고 SH-03/04의 FE1 책임으로 연결한다.

### 조립 경계와 FE1 ↔ FE2 사용자 여정

실제 URL/parameter 이름은 기존 코드·합의 Route 계약을 확인한 뒤 정한다. 아래는 전달해야 할 **논리 맥락**이며 서버 필드명/DTO 확정이 아니다.

| 이동/접점 | 조립 Owner·연결 | 전달/복귀 계약 |
| --- | --- | --- |
| A01/OTP/가입 FE1 → B01 메인 FE2 | FE1 세션/가입 완료, FE2 메인 소비 | 세션/로컬 가입 완료·기본 지역·원 returnTo; 공유 진입 목적지 우선 |
| 공유 상세 FE2 → G02 Privy FE1 → G05/G09 상세 FE2 | 로그인 Page FE1, 공유 진입/복귀 상세 FE2 | 원 게시물 ID·공유 맥락·returnTo·입력/진입 보존; 복귀만 하고 원 행동 자동 실행 금지 |
| I02~I05 FE1 → F 상세 FE2 → 원 개인 목록 | FE1 목록, FE2 단일 원 상세 | postId·목록 종류·유형/주제·탭/정렬·스크롤·실제 본인 표/삭제 접근 정책 |
| H01 알림 FE1 → F 상세 FE2 / K04 FE1 | 알림과 삭제 안내 FE1, 관련 원 상세 FE2 | 관련 원본·읽음 상태·원 필터/탭·진입 맥락; 삭제 원본은 H03 안내 |
| K01~K04 FE1 ↔ 콘텐츠/공개 채택 표시 FE2 | 업무 Page/채택 실행 FE1, 공통 콘텐츠 뷰/공개 표시 FE2 | postId·유효 기관/담당 지역·현재 기관 관계·조회 갱신. FE1은 FE2 콘텐츠 props/stub을 소비하며 파일 직접 편집 금지 |
| FE2 탐색/작성/상세 ↔ FE1 Region/Capabilities | FE2 Page 조립·FE1 공통 상태 계약 | 탐색·기본 활동·완료 이웃·기관 담당 지역 분리, 요청 시 권한 최종 판단 Backend |
| 관심/키워드 FE1 → 추천 FE2 | FE1 설정 Service, FE2 추천 소비 | 같은 합의 지역/키워드 모델·저장/실패·추천 갱신; 세션/관심 상태 Mock으로 독립 개발 |
| 공통 Header/Nav FE1 → 두 사람의 Route | SH-03/04는 공통 구현, 각 Page는 자신의 목적지/active 전달 | 메인·지도·글쓰기·알림·마이 5개, 설정 L01/알림 H01·safe-area·회원/게스트 노출 일치 |

같은 기능 ID라도 북마크 Service/목록 FE1 ↔ 상세 버튼 FE2, 기관 채택 실행 FE1 ↔ 공개 표시 FE2처럼 수정 파일과 조립 책임이 다르면 의도된 접점이다. 한 Page를 두 사람이 조립하거나 동일 Service 정본을 복제하지 않는다. 상대 영역 문제는 해당 Owner와 최소 변경/작은 공통 Task로 분리한다.

## 5. Contract와 복구 기능 담당 대조

각 Contract에는 의미·최소 타입·required/nullable·ID·오류·권한·Pending·콜백/갱신 범위·합의 Issue/버전을 기록한다. 기존 Shared Type을 먼저 재사용하고 Mock 전용 정본 타입을 만들지 않는다. 미확정 DTO/Endpoint는 `Contract 확인 필요 / FE Mock 가능`으로 남긴다.

| 계약/중앙 편집 경계 | 의미 Owner | 소비자 |
| --- | --- | --- |
| Session/Profile·완료 자격·Region·returnTo | FE1 | 양쪽 |
| BookmarkState/Service·개인 목록/활동·Institution/Adoption·Notification·관심/설정/계정 | FE1 | FE1 화면, FE2 상세/추천 등 소비 |
| Post/카드·Comment/Reaction·Vote/Option·검색/추천·Summary·Media·작성 보조/신고/공유 | FE2 | FE2 화면, FE1 목록/기관 콘텐츠 등 소비 |
| Header/Nav/Layout·공통 기본/상태 UI props | FE1 | 양쪽 |
| PostCard/VoteCard/CommentItem 등 도메인 props | FE2 | 양쪽 |
| 공통 API/Error/decoder/types·Mock/Provider 중앙 파일 | FE1 편집, 도메인 의미는 위 Owner와 BE1 합의 | 양쪽·기능 BE |

Contract 변경은 기능 Owner·소비자 영향 확인 → API는 기능 BE와 협의/BE1 정합성 확인 → 타입·Mock·Adapter·UI의 변경 범위/버전 합의 → 작은 PR 통합 → 소비 Feature 최신 반영 → 관련 검증 순서다. 실행환경/CORS/환경변수는 BE2와 확인한다. FE 개발자가 API/Schema를 독자 확정하지 않는다.

### 복구 17항목 + 보조 2항목의 단일 책임

| 상세지침서 Task | 복구 기능 | 기능 Owner / 작업 | 공통 접점 |
| --- | --- | --- | --- |
| FE-R01 | AI 의제 추천·개인 맞춤 추천·새 추천 | FE2 / B-04 | FE1 관심/키워드 Service 소비 |
| FE-R02 | 관심 지역 관리 | FE1 / D-08 | 추천/탐색 입력·권한 아님 |
| FE-R03 | 관심 키워드 관리 | FE1 / D-09 | 추천 소비 |
| FE-R04 | BottomNavigation 알림 진입·Header 알림 진입 | FE1 / SH-03/04 | 양쪽은 기존 공통 UI와 H01 목적지 사용 |
| FE-R05 | 알림/활동 목록·필터·읽음/미읽음·관련 이동 | FE1 / D-10 | 상세 FE2·기관 기록 FE1로 이동 |
| FE-R06 | 알림 수신 설정 | FE1 / D-11 | 실제 Push/설정 저장 별도 |
| FE-R07 | 투표 종료 전 알림 설정 | FE2 / B-14 | 작성/수정에 통합, 실제 예약/발송 별도 |
| FE-R08 | 신고 | FE2 / C-07 | 상세와 동일 기능군 |
| FE-R09 | 참고 자료 링크 | FE2 / B-11 | 작성·상세·허용 수정 |
| FE-R10 | 익명 선택 | FE2 / B-12 | 허용 회원/지역 안건 범위만 |
| FE-R11 | 임시 저장 | FE2 / B-13 | 세 작성 폼의 입력/결과, 서버 보관은 별도 |
| FE-R12 | 설정 | FE1 / D-12 | Header/My의 L01 진입 |
| FE-R13 | 설정에서 기존 개인 목록 재진입 | FE1 / D-12 | I03/I04/I02·원 진입 재사용 |
| FE-R14 | 계정 정보 | FE1 / A-05 | L02·My/설정 진입 |
| FE-R15 | 로그아웃 | FE1 / A-07 | 현재 기기·실패 상태/성공 A01 |
| FE-R16 | Privy 기반 이메일 변경 | FE1 / A-06 | 기존 이메일/중복/로컬 동기화 |
| FE-R17 | Privy 기반 회원 탈퇴 | FE1 / A-08 | 본인/최종 확인·성공 다음 A02 |
| FE-R18 | 자유 검색·검색 결과 있음/없음 | FE2 / B-03 | 게시판/상세/검색 복귀 |
| FE-R19 | 현재 위치 기반 지역 후보 | FE1 / A-03 | 가입·프로필 기본 지역에서 후보 사용자 선택 |

BE 미구현은 이 표의 담당 제거·후순위 제외 사유가 아니다. 실제 AI 호출·서버 저장·읽음/푸시/예약·신고 접수·초안 복구·Provider 계정 처리 등은 각 기능의 **별도 Integration**으로 연결하고 FE 작업에서 서버 구현을 떠맡지 않는다.

## 6. 기능 Owner의 UI + Mock 책임·완료 상태

모든 FE1/FE2 작업은 대상 Route/Page/Component·Figma 적용·입력/버튼/Validation·Navigation·Loading/Empty/Error/Success/Failure/Retry/Pending/Disabled·권한 제한·필요한 Modal/BottomSheet·취소/뒤로가기·입력/필터/탭/선택/원 진입/스크롤 보존·Mock 데이터 변화까지 포함한다. 적용되지 않는 상태는 이유와 함께 N/A로 기록한다. 사진 선택·부가 화면 왕복도 입력을 보존한다.

**동일 Page/Feature → Service interface → Mock Adapter 또는 Real API Adapter**를 사용한다. 현재 API Client/ApiError/response decoder/createMockApiClient/ApiClientProvider를 재사용한다. 기능별 fixture/Mock 시나리오는 해당 기능 Owner가 관리하고 SH-10 Owner는 공통 주입 기반만 관리한다. `FE1=화면, FE2=모든 Mock` 또는 `한 명=기능, 다른 한 명=모든 스타일`로 나누지 않는다.

화면 내부 임의 배열·다른 Mock DTO를 흩뿌리지 않는다. 같은 원본 ID와 오류/타입/Service interface로 정상·Empty·Loading·Error·권한 없음·성공·실패·재시도·취소/복귀를 검증한다. 미합의 계약은 Client State Mock의 가정을 명시하고 실제 계약으로 승격하지 않는다. 직접 fetch를 UI마다 흩뿌리지 않으며 준비 후 동일 UI의 Adapter/provider만 교체한다. production Mock 생성 금지와 개발 stub 비노출을 유지한다.

| 상태 | 공통 완료 정의 |
| --- | --- |
| FE UI 구현 완료 | Figma 기준 UI·Route·Component·기본 Navigation 구현/확인 |
| FE Mock 동작 완료 | 동일 실제 Page에서 사용자 Interaction·데이터 변화·위 상태/실패/복귀를 합의 Mock 기준 검증. 전체 기능 완료가 아님 |
| 실 API 연동 대기 | FE UI/Mock 완료 이후 실제 BE API/Contract/Provider·연결 검증이 남음. FE 작업도 남았으면 미완료를 함께 표시 |
| Integration 완료 | 실제 API/Backend/Provider의 인증·권한·저장/재조회·오류·복귀·연관 화면 갱신까지 검증 |

Mock 도구 존재만으로 기능별 Mock 완료를 주장하지 않고 Mock 사용을 FE 미구현으로 표현하지 않는다. Mock 성공은 실제 인증·저장·업로드·접수·푸시·탈퇴의 완료 근거가 아니다. 아래 담당표의 현재 상태는 정적 조사 기준이며 미래 작업을 이미 완료로 표시하지 않는다.

## 7. FE1 Track — 독립 구현 작업 단위

각 행은 GitHub Issue로 전환할 작업 후보다. 실제 번호를 만들지 않았으며 공통 완료 기준은 6절, 상세 동작/QA는 FE 상세지침서의 대응 Task를 따른다. FE1이 기능별 Mock과 후속 Integration을 이어서 담당한다.

| 작업 ID·제목 | 담당 | 근거 Frame/상세 Task·기능 ID | 현재 FE 상태 | Mock 범위 | BE 상태 / Integration | 필요한 선행조건 |
| --- | --- | --- | --- | --- | --- | --- |
| A-01 시작·Privy OTP 로그인 + Mock | FE1 | A01/A02/G02; F-WLXSSC, F-TSOXGG | FE 신규 구현 필요 (기존 공통 기반 재사용) | 시작 CTA·OTP 성공/실패/취소·미조회/미로그인·returnTo; 6절 공통 상태 포함 | 시작 API 비의존 / 로그인 BE API 미구현·계약 확인 필요; 시작은 API 비의존 / 로그인은 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| A-02 최초 가입·약관·프로필/지역 단계 + Mock | FE1 | A06~A09; F-KZRSXU | FE 신규 구현 필요 (기존 공통 기반 재사용) | Provider 인증과 로컬 가입 완료 분리·필수 동의·입력 보존·중도 복귀; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| A-03 프로필·활동 지역·현재 위치 후보 + Mock | FE1 | I06/I07, A08/A09; FE-R19; F-RBVFZX, F-QQKYLC | FE 신규 구현 필요 (기존 공통 기반 재사용) | 프로필 저장/실패·기본/탐색/완료 지역 구분·위치 권한/거부/실패→수동 검색·후보 사용자 선택; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API/위치 연동 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| A-04 이웃·기관 완료 자격/권한 상태 + Mock | FE1 | 독립 상태 UI; F-ATWJDJ, F-OPNIXL, S-YLSPHQ, F-GDASNA, F-MUBDJD | FE 신규 구현 필요 (기존 공통 기반 재사용) | 이웃 최대 3완료 지역·미완료/타 지역·기관 유효/만료/미확인·담당 지역·배지·서버 제한 UI; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 자격 조회/권한 검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-01 마이·누적 활동 횟수 + Mock | FE1 | I01; F-WYMXXP | FE 신규 구현 필요 (기존 공통 기반 재사용) | 프로필·기관 메뉴 분기·개인 목록/설정 진입·활동 +1/+0와 현재 관계 수 구분; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-02 북마크 Service 계약·목록 + Mock | FE1 | I02; F-FYQJPT | FE 신규 구현 필요 (기존 공통 기반 재사용) | 상태/등록/해제 공통 Service·유형/주제 필터·삭제 원본 제거·목록 버튼 없음; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-03 내가 만든 게시물 목록 + Mock | FE1 | I03; F-SSHXAA | FE 신규 구현 필요 (기존 공통 기반 재사용) | 유형별 본인 원본·Empty/Error/Retry·상세 진입/복귀; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-04 참여 게시물 목록 + Mock | FE1 | I04; F-NZTUYE | FE 신규 구현 필요 (기존 공통 기반 재사용) | 현재 유형 필터·postId별 한 카드·현재 유효 참여·북마크/작성만 제외; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-05 참여한 투표 목록 + Mock | FE1 | I05; F-QPGNCF | FE 신규 구현 필요 (기존 공통 기반 재사용) | 진행/종료·실제 본인 선택·삭제 기록 유지/내용 차단·동일 원 상세; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-06 기관 담당 전체 안건·필터 + Mock | FE1 | K01; F-CNNPYL, S-PCCNUU | FE 신규 구현 필요 (기존 공통 기반 재사용) | 전체 공개 안건/주민 의견·담당 지역·반응순·채택 목록·권한/조회 오류; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-07 기관 채택 검토·취소·기록 + Mock | FE1 | K02~K04; F-TUGMEP, S-AQOBIE | FE 신규 구현 필요 (기존 공통 기반 재사용) | 담당 지역만 채택·타 지역 열람·본 기관 취소·다른 기관 관계 유지·실패/복귀; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-08 관심 지역 관리 + Mock | FE1 | I08; FE-R02; F-MBJNPG | FE 신규 구현 필요 (기존 공통 기반 재사용) | 추가/삭제·0개 허용·개수 제한 없음·저장 실패·권한과 분리; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-09 관심 키워드 관리 + Mock | FE1 | L07; FE-R03; F-OAPPBV | FE 신규 구현 필요 (기존 공통 기반 재사용) | 7개 중 최대 4개·선택/저장 실패·기존 저장값 유지·추천 소비; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-10 알림·활동 목록/읽음/관련 이동 + Mock | FE1 | H01~H03; FE-R05; F-ITKTQN, S-KIPYBM | FE 신규 구현 필요 (기존 공통 기반 재사용) | 전체/알림/활동·읽음/미읽음·Empty/Error/Retry·관련 상세/채택/삭제 안내; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API·이벤트/읽음/푸시 연동 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-11 푸시 수신 설정 + Mock | FE1 | L08; FE-R06; S-EJZFTB | FE 신규 구현 필요 (기존 공통 기반 재사용) | 단일 ON/OFF·저장/실패 복구·OFF에도 앱 내 알림 유지; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API·실 Push 검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| D-12 설정·기존 개인 목록 재진입 + Mock | FE1 | L01/I01; FE-R12/13; F-SAOWVT | FE 신규 구현 필요 (기존 공통 기반 재사용) | 메뉴/진입·I03/I04/I02 재사용·원 진입/탭/필터/스크롤 복원·새 목록 복제 금지; 6절 공통 상태 포함 | 메뉴 API 비의존 / 데이터 BE API 미구현·계약 확인 필요; 정적 메뉴 API 비의존 / 각 데이터 연동 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| A-05 계정 정보 조회 + Mock | FE1 | L02; FE-R14; F-JRAFSD | FE 신규 구현 필요 (기존 공통 기반 재사용) | 등록 이메일·Loading/Error·변경 진입·L01/I01 원 진입 복귀; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API/Provider 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| A-06 Privy 이메일 변경/확인 + Mock | FE1 | L03/L04/L02 결과; FE-R16; S-YXAQIT | FE 신규 구현 필요 (기존 공통 기반 재사용) | 새 이메일·Provider 성공/실패/만료/취소·중복 거부·실패 시 기존 이메일 유지; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; Privy 지원·로컬 동기화 계약/연동 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| A-07 현재 기기 로그아웃 + Mock | FE1 | L01 action; FE-R15; S-HDQTIR | FE 신규 구현 필요 (기존 공통 기반 재사용) | Pending·실패/Retry 시 로그인 유지·성공 A01 시작; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 Provider/세션 검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| A-08 Privy 본인 확인·탈퇴 + Mock | FE1 | L09~L12; FE-R17; S-FIUQIE | FE 신규 구현 필요 (기존 공통 기반 재사용) | 주의→Privy 확인→최종 확인→성공·취소 L01·실패/Retry·성공 다음 A02 로그인; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 Provider·로컬 회원/데이터/세션 정리 검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |

A-02는 가입 단계 조립과 동의를, A-03은 재사용 프로필 편집/지역 선택 UI·Service를 담당한다. A08/A09에 필요한 편집/선택 컴포넌트를 별도 복제하지 않고 같은 계약/stub으로 합성한다.

FE1은 PostCard/상세의 FE2 구현을 기다리지 않는다. D-02~07은 합의 표시 모델·콜백과 개발용 카드/콘텐츠 stub으로 검증한다. A-04는 실제 권한을 FE에서 만들지 않으며 완료 자격/담당 지역의 Mock UI만 제공한다. 자체 비밀번호·증빙 제출/접수/심사·승인/반려 Flow는 작업으로 배분하지 않는다.

## 8. FE2 Track — 독립 구현 작업 단위

FE2는 Auth/Capabilities/Header/Nav의 FE1 구현 완료를 기다리지 않는다. 기존 공통 UI와 합의 세션/권한 Mock, 필요한 개발 stub으로 아래 화면/도메인 기능을 시작한다. 각 기능의 Mock·스타일·오류/복귀와 후속 Integration도 FE2 책임이다.

| 작업 ID·제목 | 담당 | 근거 Frame/상세 Task·기능 ID | 현재 FE 상태 | Mock 범위 | BE 상태 / Integration | 필요한 선행조건 |
| --- | --- | --- | --- | --- | --- | --- |
| B-01 메인·지도/투표 미리보기 + Mock | FE2 | B01/B03; F-UPRLMN | FE 신규 구현 필요 (기존 공통 기반 재사용) | 기본/탐색 지역·카드/CTA·미인증 분기·목록/지도/상세 진입; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-02 통합 게시판·탐색 지역·필터 + Mock | FE2 | C01/B02/B04/B05; F-EAJPVC | FE 신규 구현 필요 (기존 공통 기반 재사용) | 지역→유형→주제·목록/Empty/Error·정렬/필터·상세 왕복; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-03 자유 검색·결과 있음/없음 + Mock | FE2 | C02/B12~B14; FE-R18; F-EAJPVC | FE 신규 구현 필요 (기존 공통 기반 재사용) | 검색어·지역/유형/주제·결과/없음/Error/Retry·검색→상세→검색 조건/스크롤 복원; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 검색 API 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-04 AI 의제·개인 맞춤·새 추천 + Mock | FE2 | C01/F19; FE-R01; F-QZITNN | FE 신규 구현 필요 (기존 공통 기반 재사용) | 추천 카드/이유·새 추천·Loading/Empty/Error/Retry·대안 없으면 기존 유지·원 상세/원문; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 추천 산출/조회 연동 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-05 전체 지도·대표 말풍선·오류 + Mock | FE2 | D01/D03/B08; F-QIGKAK | FE 신규 구현 필요 (기존 공통 기반 재사용) | 동 선택/대표 안건·투표·사진 없음·지도 상태/Retry·상세 복귀; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 지도 데이터/renderer 검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-06 글쓰기 선택·세 유형 폼/게시 + Mock | FE2 | E01~E04; F-FTLHCX, S-NVXXYQ, S-TBFIHO | FE 신규 구현 필요 (기존 공통 기반 재사용) | 공통 폼/Validation·안건/활동/투표 정책·진행/성공/실패·게시 후 원 상세; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 저장/권한 연동 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-07 사진 선택·미리보기·업로드 상태 + Mock | FE2 | E05/작성·수정; F-GSMCLD | FE 신규 구현 필요 (기존 공통 기반 재사용) | JPG/PNG·최대 10장/게시물 합계 10MB·제거/교체·진행/실패/Retry·왕복 입력 유지; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 Storage/권한·삭제/24시간 정리 검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-08 게시물/진행 투표 수정·삭제 + Mock | FE2 | E06/E08/E09/F24/F25; F-FTLHCX, S-TBFIHO | FE 신규 구현 필요 (기존 공통 기반 재사용) | 작성자·종료 제한·변경 가능 필드·삭제 확인/취소·실패 시 입력 유지; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 저장/삭제/서버 재검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-09 공통 상세·사진/활동/투표·제한 상태 + Mock | FE2 | F/B06/B07/B09/B11, 단 별도 행 기능 제외; F-PUDHYO, F-UCDVNA | FE 신규 구현 필요 (기존 공통 기반 재사용) | 같은 원본·공개/회원/공유 변형·미확인/권한 제한·삭제·하위 컴포넌트 조립; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 API/실 Capabilities 연결 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-10 AI 요약·원문·fallback + Mock | FE2 | B10/F21~F23/G06 및 상세; F-WSCKDN | FE 신규 구현 필요 (기존 공통 기반 재사용) | 공개 안건 원문 3문장 한 문단·Loading/실패/짧은 원문 fallback·출처/원문 이동; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 Gemini 호출/저장 계약·연동 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-11 참고 자료 링크 입력/상세 + Mock | FE2 | E02~E04/상세·허용 수정; FE-R09 | FE 신규 구현 필요 (기존 공통 기반 재사용) | 선택 링크·입력/유지·상세 이동·사진 왕복 유지·활동 출처와 구분; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 저장/조회 계약·연동 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-12 허용 범위 익명 선택/표시 + Mock | FE2 | E02/지역 안건 상세; FE-R10 | FE 신규 구현 필요 (기존 공통 기반 재사용) | 일반 회원 지역 안건만·선택/해제·실제 작성자 관계·기관/활동 제외; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 저장/표시 계약·연동 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-13 세 작성 양식 임시 저장 상태 + Mock | FE2 | E02~E04; FE-R11; S-NVXXYQ | FE 신규 구현 필요 (기존 공통 기반 재사용) | 저장 버튼·Pending·완료/실패/Retry·입력 유지·공개 게시와 구분; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 초안 보관/복구 계약·연동 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| B-14 투표 종료 전 알림 입력/수정 + Mock | FE2 | E04/E06; FE-R07; S-ASBCKL | FE 신규 구현 필요 (기존 공통 기반 재사용) | 선택 입력·미선택 게시 가능·시점·종료 변경 시 기존 취소/직접 재설정·삭제/종료 안내; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 예약 취소/실행/발송 계약·연동 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| C-01 세 반응·등록/취소 + Mock | FE2 | 상세 상태; F-GOMLGG, S-HNVDPO | FE 신규 구현 필요 (기존 공통 기반 재사용) | 독립 복수 선택·개별 취소·합계·Pending·실패 복구·자격 제한; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 관계/집계/권한 검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| C-02 댓글·1단계 답글·게스트 작성 + Mock | FE2 | 상세/G07; F-EDNVWZ, S-JCEZAP, S-YYDGUS | FE 신규 구현 필요 (기존 공통 기반 재사용) | 부모/대상명·입력·빈 값/단순 금칙어/중복 차단·실패 입력 유지·게스트 예외; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 저장/금칙어·권한 검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| C-03 댓글 평가·부모 정렬 + Mock | FE2 | 상세 상태; F-CDIBRF, S-OXTKEP | FE 신규 구현 필요 (기존 공통 기반 재사용) | 좋아요/싫어요 상호배타 전환/취소·부모 좋아요순/최신순·실패/목록 갱신; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 집계/개인 기록 검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| C-04 투표 참여·변경 확인/취소 + Mock | FE2 | F04/F09/F11/B07/G08/G09; F-FCPVIS, S-CMGJIG | FE 신규 구현 필요 (기존 공통 기반 재사용) | 임시 선택≠제출·실제 본인 표·변경 확인/취소·종료·결과 불명 재조회 후 Retry; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 한 표/서버 종료·동시성 검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| C-05 공유·게스트 상세·로그인 복귀 소비 + Mock | FE2 | G01/G04~G09, G02 로그인 제외; F-OWFYWE, S-NYUECP | FE 신규 구현 필요 (기존 공통 기반 재사용) | 링크 복사·특정 공개 상세·게스트 댓글·로그인 유도/returnTo 소비·복귀 후 재클릭; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 공유 컨텍스트·세션/권한 검증 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| C-06 상세 북마크 버튼·공개 채택 표시 + Mock | FE2 | F07/상세; F-FYQJPT, F-TUGMEP | FE 신규 구현 필요 (기존 공통 기반 재사용) | FE1 Service/관계 계약 소비·단일 버튼·저장 피드백/해제·공개 현재 기관명/시각; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 Service/목록·상세 동기화 대기 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |
| C-07 신고 입력·제출 상태 + Mock | FE2 | F08; FE-R08; F-VXHBWJ | FE 신규 구현 필요 (기존 공통 기반 재사용) | 회원 자유 입력·중복 제한·Pending·성공/실패/Retry·게스트 로그인 복귀; 6절 공통 상태 포함 | BE API 미구현·업무 로직/계약 확인 필요; 실 접수 계약·연동 대기; 운영 심사 UI 제외 | 최소 interface/Route 합의만, 상대 구현 대기 없음 |

B-06은 유형 선택/공통 작성 상태와 안건·활동·투표 폼을 독립 완료 조건으로 분할할 수 있다. B-08은 허용 수정과 삭제 확인/처리로, C-02는 댓글과 1단계 답글/게스트 상태로 나눌 수 있다. 같은 Page 파일은 한 Feature가 편집하고 다른 Feature는 합의된 하위 Component/Service 범위로 제한한다. B-11~14는 B-06 완료를 기다리지 않고 같은 Form State interface·개발 harness로 구현 후 실제 폼에 합성한다. harness는 별도 제품 화면이 아니다.

### Day 1 병렬 시작과 제거한 선행조건

| FE1 Track | FE2 Track | 최소 공유 계약 | 기다리지 않는 것 |
| --- | --- | --- | --- |
| A-01/A-02 시작·Privy/가입 Mock | B-01/B-02 메인·게시판 Mock | Session/Region 상태·논리 Route | FE1 로그인 완료 후 FE2 시작 관계 없음 |
| D-02~05 개인 목록 Mock | B-09/C-01~04 상세/참여 Mock | Post 표시 모델·props·원본/복귀 | FE2 PostCard/상세 완료 후 FE1 목록 시작 관계 없음 |
| A-04 자격 상태 Mock·D-06/07 기관 | B-06~08 작성/사진·수정 | Capabilities/Region·공통 UI props | 실제 자격 API나 기관 Track 종료 대기 없음 |
| D-08/09 관심/키워드·D-10/11 알림 | B-03/04 검색/추천·B-14 종료 알림 | 저장 상태/입력·Navigation interface | 관심 실제 저장·알림 실제 발송을 FE 구현 시작 조건으로 삼지 않음 |
| SH-01~10 필요한 작은 공통 수정 | B/C 도메인 UI·자기 Service/fixture | 기존 UI 재사용·필요한 props/stub | 공통 시스템 전체 완료·BE 전체 완료를 기다리지 않음 |

공통 파일 PR의 병합 순서는 해당 파일에만 적용한다. 각 담당자 내부 작업도 전체 번호 순서가 아닌 독립 가능한 Issue부터 진행한다. 실제 연결에 필요한 Provider/API 준비만 Integration의 선행조건으로 남긴다.

### 각 화면의 디자인·구조 책임

각 담당자는 자기 화면을 Figma 최종디자인/디자인기준 v2와 396 × 852px 기준으로 비교한다. 웹 전체 높이를 852px로 고정하지 않으며 공통 Header/Nav·스크롤·safe-area를 재사용한다. 기존 Pretendard·Typography·색/간격/Radius/Border/Shadow/아이콘 토큰을 사용하고 화면별 임의 폰트/중복 베이스를 만들지 않는다. 세부 수치·예외는 공통 합의사항/디자인 문서를 참조하여 이 문서에 다시 복제하지 않는다.

구현 순서는 `Design Token → Common UI → Domain Component → Screen → Page/Route`를 따른다. 현재 app/components/ui/components/layout/lib/api 구조와 기존 Naming을 우선하며 상대 Part 리팩터링·전역 포맷팅·독자 구조/패키지 도입을 섞지 않는다. Figma를 직접 확인하지 못한 구현은 비교 완료로 기록하지 않는다.

## 9. Shared Integration — 준비된 기능부터 실제 연결

Shared는 공동 편집을 뜻하지 않는다. 각 INT 작업도 실행 Owner 한 명을 지정한다. 아래 INT 행은 연동 접점 분류이며 하나의 거대 Issue를 뜻하지 않는다. 실제 Issue는 원 기능 작업(A-06, D-10, B-07 등)에 대응하는 작은 Adapter/검증 단위로 나눈다. **실 API 준비 후 기존 기능 담당자가 Adapter 교체/실제 검증을 이어간다.** 두 사람의 교차 여정은 Page별 Owner가 자기 파일을 수정하고 한 PR에 여러 편집자를 섞는 대신 연관된 작은 PR로 분리한다.

| Integration 후보 | 실행 Owner | 상대 확인/소비 | 준비 조건·실제 확인 |
| --- | --- | --- | --- |
| INT-01 Privy·로컬 가입·프로필/지역·계정/세션 | FE1 | FE2 세션 소비 | #74 실제 Provider·인증 전달·로컬 회원/계정 변경/탈퇴 계약, 실 저장/조회·실패·취소·returnTo |
| INT-02 완료 자격·기관 관계·개인 목록/북마크·알림/관심/설정 | FE1 | FE2 상세/추천 소비 | 해당 실제 조회/저장·#75 시연 상태·서버 권한·원본/관계/집계·읽음/수신·관련 이동 |
| INT-03 탐색·검색/추천·지도·AI 요약 | FE2 | FE1 지역/관심 계약 | 실제 데이터/지도 원천·AI/검색/추천 계약·원문/fallback·에러·목록/상세 일관성 |
| INT-04 작성·수정/삭제·사진·링크/익명/초안·종료 알림 | FE2 | FE1 세션/권한 계약 | 실제 저장/Storage 권한·사진 공개 열람/삭제/서버 미완료 24시간 정리·초안 복구/예약 계약 |
| INT-05 반응·댓글/평가·투표·신고·공유·상세 접점 | FE2 | FE1 북마크/기관/개인 기록 | 실제 권한/저장·표/집계·중복/종료·실패 복구·게스트 범위·관계 동기화 |
| INT-06 공통 UI/Route/provider·FE1 Page 간 연결 | FE1 | FE2 props/목적지 리뷰 | 실제 공통 UI/루트 주입·stub 제거·5메뉴/헤더·FE1 화면 복귀. UI 연결만으로 BE Integration 완료 아님 |
| INT-07 FE2 Page의 공통 UI 소비·원 상세 연결 | FE2 | FE1 공통 props 리뷰 | 자기 Page 연결·stub 제거·active/복귀·게스트/회원 경계 |
| INT-08 교차 여정 QA·#30/#31 연동 증거 취합 | FE1 조율 | FE2는 B/C 시나리오 검증/자기 파일 수정 | #30 실제 API 주소·인증·Storage·CORS, #31 사용자 여정·SHA/환경·실행 근거. health 200만으로 완료 아님 |

API Method/Endpoint·DTO·오류·인증·권한은 실제 합의와 API 정본으로 확인한다. 계약이 없으면 화면 Mock은 계속하고 해당 실제 연결만 대기한다. 실제 Backend/Provider가 없는 범위를 Integration 완료로 올리지 않는다. Mock/stub은 개발 검증용으로 유지할 수 있으나 제품 경로에 남기지 않는다.

## 10. GitHub Issue 전환과 최소 Git 규칙

각 작업 행은 담당자 한 명·독립 사용자 결과·허용 파일·UI/Mock 인수·실 Integration 후속으로 Issue화한다. 코드가 없는 화면은 신규, 기존 공통 UI는 재사용/수정으로 기록한다. 무관한 여러 화면을 하나의 거대 Issue로 묶거나 작은 스타일 값만 독립 기능 Issue로 쪼개지 않는다. 큰 그룹은 8절 예시처럼 나눠도 Page Owner와 Contract를 유지한다.

**1 Issue → 1 Feature → 1 PR → front/develop**. 최신 develop 반영·충돌 해결·commit/push·리뷰/CI·병합은 기존 통합/FE Git 전략을 따른다. 상대 Feature 직접 merge·보호 브랜치 직접 push·공유 이력 덮어쓰기를 하지 않는다. 이 문서에서 Git 전략이나 리뷰 예외를 새로 만들지 않는다.

PR 전 담당자는 Route·Figma 대응·입력/버튼·뒤로가기·Loading/Empty/Error·정상/실패 Mock·권한/상태 보존을 확인하고 현재 scripts에 맞는 검증을 실행한다. 현재 frontend에는 `npm run lint`, `npm run typecheck`, `npm run build`가 있고 test script는 없다. 테스트는 실제 README/CI 실행 방법을 확인한 경우에만 해당 명령으로 기록한다. 실행하지 않은 검증은 통과로 쓰지 않는다.

```markdown
- 작업 ID/제목·담당자·리뷰어:
- 제품 기능 ID·Figma node/화면군·FE 상세 Task:
- 포함/제외·현재 FE 상태·예상 수정 파일/Page Owner:
- 공통 파일 Owner/영향·합의 interface·변경 순서:
- Mock 정상/Empty/Loading/Error/권한·성공/실패/Retry·복귀/입력 보존:
- Contract: 합의 근거/미합의 필드·실제 API 연결 대기:
- FE UI 구현 완료: 예/아니오·근거
- FE Mock 동작 완료: 예/아니오·근거
- 실 API 연동 대기: 예/아니오·남은 API/Provider/Contract
- Integration 완료: 예/아니오·실제 Backend/저장/권한/오류 근거
- 실제 검증 명령/환경/결과·미실행 사유:
- 기능 Owner가 이어갈 Integration 후속 작업:
```

이번 개정에서는 **문서만 갱신**한다. GitHub Issue 생성/수정·브랜치 생성/전환·commit·push·PR 생성/병합은 수행하지 않는다. Issue 생성은 이 배분을 바탕으로 별도 요청 시 진행한다.

## 11. 작업량·담당 누락/중복 검증

### 역할을 유지한 작업량 재검토

| 항목 | FE1 | FE2 |
| --- | --- | --- |
| Frame 담당 | 80개. 계정 오류/재발송·목록 필터 variant 다수, 같은 Route/Component의 상태로 처리 | 55개. Frame 밖의 반응/댓글/평가/투표·쓰기 상태가 많아 수보다 복잡도가 큼 |
| 독립 기능 작업군 | A 8개 + D 12개 = 20개 | B 14개 + C 7개 = 21개 |
| 큰 복잡도 | Privy/로컬 가입·계정/탈퇴 상태, 지역/권한, 기관 채택, 개인 기록/알림·설정 갱신 | 세 유형 폼/사진/수정, 상세/게스트, 댓글·투표 상호작용/동시성, 검색/지도/AI |
| Shared 책임 | 레이아웃/토큰/Header/5메뉴·기본/상태 UI·공통 API/Mock 등록. 기존 기반 재사용 중심 | 카드/상세·사진·댓글/투표/반응/공유 도메인 UI. 화면/모델과 같은 Owner 유지 |
| Mock 책임 | 모든 A/D 기능의 정상/실패·권한·복귀·데이터 변화 | 모든 B/C 기능의 정상/실패·권한·복귀·데이터 변화 |
| Integration 대기 | Privy/로컬 회원·자격·개인/기관·관심/알림·계정·위치 후보 실제 연결 | 콘텐츠/검색/추천/지도·사진/AI·참여/신고/공유·작성 보조 실제 연결 |

화면 수만 맞추기 위해 역할을 뒤집지 않았다. FE2의 상세·댓글·투표/폼 복잡도와 API/Service 연결 수를 고려해 FE1에 공통 상태/피드백 UI·Chip·중앙 API/Mock 등록을 배정하고, 설정/관심/알림/계정을 한 흐름에 모았다. 기존 Header/Nav/Token 및 PostCard/상세의 확정 Owner는 유지한다. 이 비교는 일정/공수 확정치가 아니며 상태 수·권한 분기·폼/Navigation·Mock 시나리오·공통 의존·실 연결 수를 Issue 추정 때 함께 확인한다. 불균형이 확인되면 큰 Part 구조를 유지한 작은 Shared Task 조정으로 해소하고 동시 편집을 만들지 않는다.

### 주요 기능군 누락 검증

| 기능군 | 책임 Owner / 작업 | 결과 |
| --- | --- | --- |
| Auth / Onboarding / Account | FE1 A-01/02/05~08 | 배정 |
| Profile / Region / Qualification / returnTo | FE1 A-03/04·SH-08, FE2는 자기 Page 소비 | 배정·경계 분리 |
| Home / Board / Search / Recommendation | FE2 B-01~04 | 배정 |
| Map | FE2 B-05 | 배정 |
| Create / Photo / Edit / Draft / Link / Anonymous / Notice input | FE2 B-06~08/11~14 | 배정 |
| Detail / Reaction / Comment / Reply / Evaluation / Vote | FE2 B-09·C-01~04 | 배정 |
| Bookmark | FE1 D-02 Service/목록, FE2 C-06 상세 버튼 | 의도된 접점·파일 중복 없음 |
| Report / Share / Guest | FE2 C-05/07·C-02 게스트 댓글 | 배정 |
| Notification / Push setting | FE1 D-10/11·SH-03/04 공통 진입 | 배정 |
| My / Personal activity / Created / Participated / My votes | FE1 D-01/03~05 | 배정 |
| Settings / Personal list reentry / Interest region / Keyword | FE1 D-08/09/12 | 배정 |
| Officer / Adoption / Institution badge | FE1 A-04·D-06/07, FE2 C-06 공개 표시 | 의도된 접점·파일 중복 없음 |
| AI summary / Original text | FE2 B-10 | 배정 |
| Header / Navigation / Token / Modal / FilterChip / API / Mock Provider | 3절 SH 표 단일 Owner | 중복 제작 없음 |

135 Frame의 Page Owner는 **FE1 80 / FE2 55 / 미배정 0 / 중복 0**, 복구 **FE-R01~19 전부** 기능 또는 Shared Owner에 연결했다. 공통 컴포넌트·데이터 Service를 소비하는 것은 두 명의 중복 구현 배정이 아니다. 자체 비밀번호·증빙 제출/심사 22 Frame은 제외하고, 완료 기관/이웃 상태·기관 업무 자체는 유지했다.

### 자체 점검 (이번 문서 정합성)

| 점검 | 결과 |
| --- | --- |
| 1~2 기존 FE1/FE2·Part 구조 | FE1 A/D·FE2 B/C 유지, 새 Part 없음 |
| 3 최신 FE 135 Frame 매핑 | 4절 그룹·node 기준 전수 대조, 135/135 |
| 4~5 BE 미구현·복구 기능 담당 | 5·7·8절, 미구현을 이유로 제외하지 않음 |
| 6~9 Nav/Header/공통 컴포넌트/Mock Infra Owner | SH-03/04/05~07/10 단일 FE1, 도메인 SH-11/12 FE2 |
| 10~13 UI+Mock 책임·Integration 분리·독립 시작 | 1·6~9절, 최소 interface만 공유·불필요한 구현 선행 제거 |
| 14~21 핵심 화면/기능군 누락 | 위 기능군 검증표 전부 배정 |
| 22 각 화면의 디자인 책임 | 8절, 기능 Owner가 자신의 디자인/상태까지 담당 |
| 23 자체 비밀번호/증빙 Flow 재배분 없음 | 4·7절, 명시적 제품 예외 유지 |
| 24~25 미배정/의도하지 않은 중복 없음 | Frame 135/135·Task/Shared 단일 Owner·Page/Service 접점 구분 |
| 26 Issue 전환 가능한 단위 | A/D 20·B/C 21 작업군, 큰 그룹 분할·공통/연동 후보·인수 양식 |
| 27 대상 문서만 수정 | 기존 경로 갱신, 다른 파일의 미커밋 변경 보존 |

문서 내용·Frame 매핑·Owner/Task 참조·Markdown/링크·diff를 검증한다. 실제 UI·Mock·Backend 실행·build·API·Storage·CORS·GitHub 보호 규칙/Issue 상태·Integration은 이번 문서 수정에서 검증한 것으로 기록하지 않는다. 실제 구현 시 FE 상세지침서 QA와 공통 합의사항의 검증 절차를 적용한다.

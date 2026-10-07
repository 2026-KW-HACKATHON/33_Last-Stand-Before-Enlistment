# Discushion MVP 프론트엔드 개발 상세지침서

> 작성 기준: 2026-10-06 (Asia/Seoul) · 문서 전용 산출물 · Next.js / TypeScript / Tailwind CSS
> **실제 코드는 수정하지 않았다.** 제품 정책은 최신 통합지침서를 따른다. Route·폴더·타입·HTTP 계약은 실제 저장소에 대조하기 전까지 구현 제안이다.
> **API SPEC 기준 계약안 / 실제 구현 확인 필요**: 이 표기는 본문의 모든 API·DTO·영문 Enum·에러 코드·공유 헤더에 적용한다.

## 1. 문서 목적과 사용 순서

이 문서는 시작 → 가입 → 탐색 → 작성·참여 → 개인 기록 → 기관 채택을 실제 화면, 상태, 사용자 행동과 구현 작업으로 변환한다. 화면을 먼저 만들고 해당 기능에 필요한 API를 연결한다. 아래 순서로 작업한다.

1. 2~6절로 근거·범위·Figma 화면을 확인한다.
2. 7~16절로 이동, 상태, 권한, 공통 컴포넌트를 구축한다.
3. 17~45절의 기능 계약과 51~53절의 Task·화면 명세를 함께 구현한다.
4. 54~55절로 API·타입 정합성을 맞춘다.
5. 56~58절로 제외 범위·미결정·인수 기준을 확인한다.

`[확정]`은 제품 정책, `[권장안]`은 구현 선택, `[확인 필요]`는 아직 결정할 수 없는 값이다. 체크박스는 앞으로 수행할 개발 작업이며 구현 완료 표시가 아니다.

## 2. Source of Truth와 확인 결과

| 자료 | 직접 확인 범위 | 적용 |
| --- | --- | --- |
| Discushion_MVP_백엔드_프론트엔드_통합_지침서.md | 전체 | MVP·역할·지역 자격·FE/BE 책임 최우선 |
| Discushion_기능명세서_2026-10-07_MVP반영_정리본_v10.2.md | 제공 원문 전체, 보완 정책·기능 ID·MVP | 동작·예외·인수 기준 |
| Discushion_PRD_2026-10-07_MVP반영_정리본_v10.2.md | 제공 원문 전체, 보완 정책·MVP | 제품 목적·여정 |
| Figma KW 해커톤 디자인 / 최종디자인 | 페이지 1136:6698, 모바일 프레임 157개 식별자/이름; 핵심 화면 텍스트·메인/투표 상세 렌더 확인 | MVP 화면의 시각적 기준 |
| Discushion_최종디자인_디자인기준 v2.md | 전체 | 실제 색·폰트·정렬·규격 |
| Discushion_API_SPEC_v2.md | 전체 | 연동 계약 참고 |

유저플로우 참조: `Discushion_유저플로우_와이어프레임기반_2026-10-06.md`.

[Figma 최종디자인](https://www.figma.com/design/Wobmrjd8xSUNKQISplV5aR?node-id=1136-6698).

기능·권한: 통합지침서 → 기능명세 → PRD. 시각: 최종디자인만. 토큰: 디자인기준 v2. API: SPEC v2. 기능명세 A~AA와 Figma WF-A~L은 독립된 식별 체계다. PRD·기능명세서의 참조 기준은 v10.2이며 API SPEC v2의 내부 제목은 v1.1이다. 이름 차이만으로 다른 계약으로 간주하지 않는다.

제공 작업 공간에는 package.json, Next 설정, src/app, Controller/DTO 등 실제 FE/BE 저장소가 없다. 기존 구현 여부는 **확인 불가**이며 아래 구현 항목을 이미 구현됨/미구현으로 단정하지 않는다. 향후 실제 저장소를 받으면 기존 Route/라이브러리/컴포넌트/명령을 먼저 확인한다. Git/GitHub 협업전략 문서는 이번 기능 지침의 근거로 사용하지 않았다.

### 발견한 디자인·정책 차이

| 디자인 관찰 | 구현 지침 |
| --- | --- |
| B01/C01 검색·지금 뜨는 이야기/추천 | 자유 검색·추천·별도 인기 선정은 제외. MVP 게시물·진행 투표·지역/유형/주제 목록으로 구성 |
| D01에 지역 활동 대표 | 지도 대표에서 활동 제외, 공개 안건/투표만 |
| E 작성에 참고 링크·익명·임시 저장·투표 알림 | 요청 필드와 UI 모두 제외 |
| F 상세 신고·참고 자료 | 제외. 활동의 필수 출처·외부 참여 링크는 유지 |
| I04 '반응한 게시물' | 참여한 게시물로 안내하고 반응 외 댓글/답글·평가·투표도 반영 |
| H 활동 탭에도 댓글·투표·기관 알림 표시 | H 알림 피드는 제외. I01의 누적 행동 숫자를 실제 activity API로 구현 |
| L 설정·계정·관심·탈퇴 | MVP Route/메뉴 미제공 |
| 하단 5슬롯에 알림 | **[확인 필요]** PO/디자인 결정 전 4슬롯/빈 슬롯/비활성 알림 어느 것도 확정하지 않음 |
| A06 가입 화면에 비밀번호·기관 진입 혼재 | 이메일 인증→비밀번호/약관→프로필→활동 지역 완료를 단계로 구현. 기관 신청은 로그인 후 |
| A09 현재 위치 버튼 | GPS·위치 권한 수집은 필수 아님. 등록 지역 선택으로 구현 |

## 3. 기술 스택과 선택적 라이브러리

확정: Next.js, TypeScript, Tailwind CSS. 버전·App Router 여부는 실제 package.json에서 확인하며 임의 최신 버전을 고정하지 않는다. 이하 Route 예시는 App Router 채택 시 권장안이다.

| 권장 후보 | 목적/사용 기능 | 필수 여부·대안 |
| --- | --- | --- |
| TanStack Query | 목록 캐시, 중복 조회, 무효화, mutation rollback | 선택. fetch + 프로젝트의 서버 상태 계층으로 가능 |
| React Hook Form | 작성/가입/기관 입력·필드 오류 | 선택. 제어 입력·reducer 가능 |
| Zod | 합의된 DTO와 폼 검증 | 선택. 직접 검증 가능; 미정 길이를 임의 schema에 넣지 않음 |
| Zustand | 작성 큐/가입 단계 등 Route 왕복 상태 | 선택. Context/reducer로 가능 |
| Axios | 기존 인터셉터/클라이언트와 일관성 | 선택. 기본 fetch로 충분 |

먼저 기존 의존성을 확인한다. 이 후보 전체 설치를 작업 선행조건으로 삼지 않는다. 지도 SDK/provider와 실시간 투표 방식도 미정이다.

## 4. MVP 구현 범위

서비스 목표는 지역 변화의 정보를 쉽게 확인하고 하나의 게시물에서 의견과 투표를 나누는 것이다. 게스트는 공유받은 상세에 한정되고, 지역 참여는 이웃 인증 완료 지역에 한정된다. 기관은 공개 지역 안건을 검토하고 담당 지역 안건을 독립 관계로 채택한다.

## 5. FULL / DEMO / NON-MVP

| 분류 | 전체 구현 범위 | 완료 의미 |
| --- | --- | --- |
| FULL | 시작·로그인·가입·이메일 인증·프로필·활동 지역·메인·통합 게시판·이슈 지도·세 유형 상세/작성/수정/삭제·사진·반응·댓글/1단계 답글·평가·정렬·실제 투표·공유·게스트·returnTo·북마크·마이·작성/참여/투표 기록·행동 횟수·기관 담당자/채택·AI 안건 요약 | 실제 UI + 실 API 저장/조회. Mock만 성공은 완료 아님 |
| DEMO / LIMITED BACKEND | 이웃/기관 인증 신청 입력·증빙 큐·최종 제출·접수 저장·상태 조회·시연 완료 상태·완료 지역/담당 지역/기관 유효기간·배지/권한 반영 | 실 신청/상태 연동. 실제 운영 심사 제외; 완료 seed를 서버가 제공 |
| NON-MVP | AI 추천·관심 지역/키워드·알림/Push/설정·신고·계정 복구/이메일·비밀번호 변경/탈퇴/다크 모드·임시저장·참고자료 링크·익명·AI 이미지·운영자 심사·행정 연동·채택 후 처리 상태 | UI·API·예약/운영 Task를 추가하지 않음 |

사진 없음, 삭제 안내, 오류/빈 상태도 FULL의 완료 조건이다. 인증 접수는 참여 권한 또는 기관 역할 획득을 의미하지 않는다.

## 6. Figma ↔ MVP 전체 화면 매핑

157개를 모두 별도 페이지로 개발하지 않는다. 같은 화면의 필터·오류·완료는 동일 Route의 상태로 구현한다. 아래 NON-MVP는 해당 기능 전용 프레임을 제외한다는 의미다. FULL 프레임에서도 비-MVP 요소를 제거한다. '관련 API'는 계약안이다. 상세 동작은 17~45절, 구현 단위는 53절에서 확인한다.

| Figma ID (실제 node) | 화면명 | MVP 여부 | 구현 분류 | 관련 기능 ID | 관련 API | 비고 |
| --- | --- | --- | --- | --- | --- | --- |
| 1157:905 | WF-A01-시작 | O | FULL | F-WLXSSC | — | 시작 이미지 사용 |
| 1157:962 | WF-A02-로그인 | O | FULL | F-TSOXGG | POST /auth/login | 중복 프레임 동일 Route |
| 1157:1044 | WF-G02-로그인-원게시물복귀 | O | FULL | F-TSOXGG | POST /auth/login | 중복 프레임 동일 Route |
| 1157:1130 | WF-A06-회원가입-계정정보·약관 | O | FULL | F-KZRSXU / F-RBVFZX / F-QQKYLC | POST /auth/email-verifications; /confirm; /auth/sign-up; GET /regions | 가입 단계/상태 |
| 1157:1262 | WF-A07-회원가입-이메일인증-발송 | O | FULL | F-KZRSXU / F-RBVFZX / F-QQKYLC | POST /auth/email-verifications; /confirm; /auth/sign-up; GET /regions | 가입 단계/상태 |
| 1157:1410 | WF-A07-회원가입-이메일인증-재발송 | O | FULL | F-KZRSXU / F-RBVFZX / F-QQKYLC | POST /auth/email-verifications; /confirm; /auth/sign-up; GET /regions | 가입 단계/상태 |
| 1157:1558 | WF-A07-회원가입-이메일인증-완료 | O | FULL | F-KZRSXU / F-RBVFZX / F-QQKYLC | POST /auth/email-verifications; /confirm; /auth/sign-up; GET /regions | 가입 단계/상태 |
| 1157:1703 | WF-A08-회원가입-프로필설정 | O | FULL | F-KZRSXU / F-RBVFZX / F-QQKYLC | POST /auth/email-verifications; /confirm; /auth/sign-up; GET /regions | 가입 단계/상태 |
| 1157:1801 | WF-A08-회원가입-프로필설정-활동지역설정완료 | O | FULL | F-KZRSXU / F-RBVFZX / F-QQKYLC | POST /auth/email-verifications; /confirm; /auth/sign-up; GET /regions | 가입 단계/상태 |
| 1157:1910 | WF-A09-회원가입-활동지역설정 | O | FULL | F-KZRSXU / F-RBVFZX / F-QQKYLC | POST /auth/email-verifications; /confirm; /auth/sign-up; GET /regions | 가입 단계/상태 |
| 1157:1999 | WF-A03-비밀번호찾기 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1157:2063 | WF-A04-비밀번호복구-본인확인 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1159:1586 | WF-A04-비밀번호복구-본인확인-재발송 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1159:1643 | WF-A05-비밀번호재설정 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1159:1694 | WF-B01-메인 | O | FULL | F-UPRLMN | GET /home | 검색/추천/인기 전용 선정 제외 |
| 1159:1887 | WF-B03-메인-하계2동-이웃미인증 | O | FULL | F-UPRLMN | GET /home | 검색/추천/인기 전용 선정 제외 |
| 1159:2083 | WF-B02-탐색지역선택 | O | FULL | F-UPRLMN | GET /regions; GET /home | 탐색 지역만 변경 |
| 1159:2244 | WF-C01-통합게시판 | O | FULL | F-EAJPVC | GET /posts | 추천/검색 UI 제외 |
| 1159:2497 | WF-C01-새추천성공 | X | NON-MVP | 추천 제외 | — | 추천 진입 제외; 상세/요약 공통 UI는 다른 FULL 프레임 사용 |
| 1159:2751 | WF-C02-검색-필터 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1159:2933 | WF-C02-검색결과있음 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1159:3129 | WF-C02-검색결과없음 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1159:3319 | WF-D01-전체지도 | O | FULL | F-QIGKAK | GET /map/dongs | 안건/투표 대표만 |
| 1159:3480 | WF-D03-지도-로딩실패 | O | FULL | F-QIGKAK | GET /map/dongs | 안건/투표 대표만 |
| 1159:3606 | WF-E01-글쓰기-유형선택 | O | FULL | F-FTLHCX / F-GSMCLD | POST/PATCH/DELETE /posts/{postId} | 생성은 POST /posts; 비-MVP 입력 제거 |
| 1159:3688 | WF-E02-글쓰기-지역안건 | O | FULL | F-FTLHCX / F-GSMCLD | POST/PATCH/DELETE /posts/{postId} | 생성은 POST /posts; 비-MVP 입력 제거 |
| 1159:3823 | WF-E03-글쓰기-지역활동정보 | O | FULL | F-FTLHCX / F-GSMCLD | POST/PATCH/DELETE /posts/{postId} | 생성은 POST /posts; 비-MVP 입력 제거 |
| 1159:4002 | WF-E04-글쓰기-투표 | O | FULL | F-FTLHCX / F-GSMCLD | POST/PATCH/DELETE /posts/{postId} | 생성은 POST /posts; 비-MVP 입력 제거 |
| 1159:4185 | WF-E05-사진-첨부미리보기 | O | FULL | F-GSMCLD | POST/PATCH /posts (multipart) | 클라이언트 파일 큐 |
| 1159:4271 | WF-E06-진행중투표-수정 | O | FULL | F-FTLHCX / F-GSMCLD | POST/PATCH/DELETE /posts/{postId} | 생성은 POST /posts; 비-MVP 입력 제거 |
| 1160:2463 | WF-F01-지역안건상세-사진있음 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1160:2636 | WF-F02-지역안건상세-사진없음 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1160:2806 | WF-F03-지역활동정보상세 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1160:2997 | WF-F04-투표상세-진행중 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1160:3177 | WF-F07-북마크-저장성공 | O | FULL | F-FYQJPT | PUT/DELETE /posts/{postId}/bookmark | 성공 안내 상태 |
| 1160:3218 | WF-F08-신고-입력 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:3483 | WF-F09-투표-종료결과 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1160:3650 | WF-G01-공유-게스트상세 | O | FULL | F-OWFYWE / F-EDNVWZ / F-TSOXGG | GET /posts/{postId}; GET/POST comments; POST replies | 공유 대상 범위; 복귀 회원 권한 재조회 |
| 1160:3759 | WF-G04-공유-링크복사 | O | FULL | F-OWFYWE | GET /posts/{postId}/share-link | 게스트 기존 링크 복사 |
| 1160:3811 | WF-G05-로그인복귀-미인증회원상세 | O | FULL | F-OWFYWE / F-EDNVWZ / F-TSOXGG | GET /posts/{postId}; GET/POST comments; POST replies | 공유 대상 범위; 복귀 회원 권한 재조회 |
| 1160:3940 | WF-H01-알림및활동 | X | NON-MVP | 알림 피드 제외 | — | 활동 숫자는 I01 + activity로 구현 |
| 1160:4069 | WF-H02-알림및활동-빈상태 | X | NON-MVP | 알림 피드 제외 | — | 활동 숫자는 I01 + activity로 구현 |
| 1160:4427 | WF-H03-삭제된게시물안내 | O | FULL | F-PUDHYO | GET /posts/{postId} (최신 상태) | Modal/접근 제한 상태 |
| 1160:4471 | WF-I01-마이페이지 | O | FULL | F-WYMXXP | GET /users/me; GET /users/me/activity | 비-MVP 메뉴 제외 |
| 1160:4653 | WF-I02-북마크목록 | O | FULL | F-FYQJPT | GET /users/me/bookmarks | 필터 상태 |
| 1160:4807 | WF-I03-내가만든게시물 | O | FULL | F-SSHXAA | GET /users/me/posts | 유형 필터 |
| 1160:4930 | WF-I04-반응한게시물 | O | FULL | F-NZTUYE | GET /users/me/participations | 참여한 게시물로 구현; 반응 하위 필터 계약 미정 |
| 1160:5075 | WF-I05-참여한투표 | O | FULL | F-QPGNCF | GET /users/me/votes | 실제 본인 선택 |
| 1160:5372 | WF-I06-프로필수정 | O | FULL | F-RBVFZX / F-QQKYLC | GET/PATCH /users/me; GET /regions | 기본 활동 지역 |
| 1160:5452 | WF-I07-기본활동지역 | O | FULL | F-RBVFZX / F-QQKYLC | GET/PATCH /users/me; GET /regions | 기본 활동 지역 |
| 1160:5516 | WF-I08-관심지역 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:5577 | WF-J01-이웃인증-제출 | O | DEMO / LIMITED BACKEND | F-ATWJDJ | GET/POST /users/me/neighbor-verifications | 접수/완료 분리 |
| 1160:5631 | WF-J02-이웃인증-접수 | O | DEMO / LIMITED BACKEND | F-ATWJDJ | GET/POST /users/me/neighbor-verifications | 접수/완료 분리 |
| 1160:5676 | WF-J03-기관인증-정보 | O | DEMO / LIMITED BACKEND | F-OPNIXL | GET/POST /institution-verifications | 접수/완료 분리 |
| 1160:5900 | WF-J04-기관인증-증빙 | O | DEMO / LIMITED BACKEND | F-OPNIXL | GET/POST /institution-verifications | 접수/완료 분리 |
| 1160:5953 | WF-J05-기관인증-접수 | O | DEMO / LIMITED BACKEND | F-OPNIXL | GET/POST /institution-verifications | 접수/완료 분리 |
| 1160:5995 | WF-K01-담당자-전체안건 | O | FULL | F-CNNPYL | GET /officer/agendas | 유효 기관만 |
| 1160:6063 | WF-K02-담당지역-채택상세 | O | FULL | F-TUGMEP / F-PUDHYO | GET /posts/{postId}; POST/DELETE /posts/{postId}/adoptions | 타지역 열람; 주민 의견 내용 유지 |
| 1160:6134 | WF-K03-타지역-열람전용 | O | FULL | F-TUGMEP / F-PUDHYO | GET /posts/{postId}; POST/DELETE /posts/{postId}/adoptions | 타지역 열람; 주민 의견 내용 유지 |
| 1160:6189 | WF-K04-지역안건-채택기록 | O | FULL | F-TUGMEP / F-PUDHYO | GET /posts/{postId}; POST/DELETE /posts/{postId}/adoptions | 타지역 열람; 주민 의견 내용 유지 |
| 1160:6401 | WF-L01-설정 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:6480 | WF-L02-계정관리 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:6527 | WF-L03-이메일변경 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:6577 | WF-L04-이메일변경-인증 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:6633 | WF-L05-비밀번호변경-인증 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:6687 | WF-L06-비밀번호변경 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:6874 | WF-L07-관심키워드 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:6943 | WF-L08-알림설정 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:6991 | WF-L09-탈퇴-주의사항 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:7035 | WF-L10-탈퇴-본인확인 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:7084 | WF-L11-탈퇴-최종확인 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:7128 | WF-L12-탈퇴완료-회원이용종료 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1160:7364 | WF-F11-투표-제출후 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1160:7476 | WF-F12-지역활동정보상세-종료 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1160:7599 | WF-G06-게스트-AI요약 | O | FULL | F-WSCKDN | GET /posts/{postId}/summary | 공개 안건만 |
| 1160:7644 | WF-G07-게스트-답글작성 | O | FULL | F-OWFYWE / F-EDNVWZ / F-TSOXGG | GET /posts/{postId}; GET/POST comments; POST replies | 공유 대상 범위; 복귀 회원 권한 재조회 |
| 1160:7698 | WF-G08-게스트-공유투표결과 | O | FULL | F-OWFYWE / F-EDNVWZ / F-TSOXGG | GET /posts/{postId}; GET/POST comments; POST replies | 공유 대상 범위; 복귀 회원 권한 재조회 |
| 1160:7788 | WF-G09-로그인복귀-미인증회원투표 | O | FULL | F-OWFYWE / F-EDNVWZ / F-TSOXGG | GET /posts/{postId}; GET/POST comments; POST replies | 공유 대상 범위; 복귀 회원 권한 재조회 |
| 1162:4296 | WF-B04-탐색지역선택-하계2동 | O | FULL | F-UPRLMN | GET /regions; GET /home | 탐색 지역만 변경 |
| 1162:4417 | WF-B05-통합게시판-하계2동-열람전용 | O | FULL | F-EAJPVC | GET /posts | 추천/검색 UI 제외 |
| 1162:4606 | WF-B06-지역안건상세-하계2동-열람전용 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1162:4777 | WF-B07-투표결과-하계2동-이웃미인증 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1162:4954 | WF-B08-이슈지도-하계2동 | O | FULL | F-QIGKAK | GET /map/dongs | 안건/투표 대표만 |
| 1162:5101 | WF-B09-하계2동-지역참여제한안내 | O | FULL | F-PUDHYO | GET /posts/{postId} (최신 상태) | Modal/접근 제한 상태 |
| 1162:5417 | WF-B10-AI요약-하계2동안건 | O | FULL | F-WSCKDN | GET /posts/{postId}/summary | 공개 안건만 |
| 1162:5462 | WF-B11-하계2동-일반회원기능안내 | O | FULL | F-PUDHYO | GET /posts/{postId} (최신 상태) | Modal/접근 제한 상태 |
| 1162:5558 | WF-B12-검색-하계2동 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1162:5697 | WF-B13-검색결과있음-하계2동 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1162:5846 | WF-B14-검색결과없음-하계2동 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1162:5991 | WF-F20-탐색지역-참여제한안내 | O | FULL | F-PUDHYO | GET /posts/{postId} (최신 상태) | Modal/접근 제한 상태 |
| 1162:6355 | WF-F16-월계2동안건-열람전용 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1162:6526 | WF-F21-AI요약-월계2동-지도안건 | O | FULL | F-WSCKDN | GET /posts/{postId}/summary | 공개 안건만 |
| 1162:6571 | WF-F17-월계3동안건-열람전용 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1162:6742 | WF-F22-AI요약-월계3동-지도안건 | O | FULL | F-WSCKDN | GET /posts/{postId}/summary | 공개 안건만 |
| 1162:6787 | WF-F18-하계1동-독서모임-열람전용 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1162:6978 | WF-F19-지역안건-새추천상세 | X | NON-MVP | 추천 제외 | — | 추천 진입 제외; 상세/요약 공통 UI는 다른 FULL 프레임 사용 |
| 1162:7372 | WF-F23-AI요약-월계1동-새추천 | X | NON-MVP | 추천 제외 | — | 추천 진입 제외; 상세/요약 공통 UI는 다른 FULL 프레임 사용 |
| 1162:7417 | WF-E08-본인투표-삭제확인 | O | FULL | F-FTLHCX / F-GSMCLD | POST/PATCH/DELETE /posts/{postId} | 생성은 POST /posts; 비-MVP 입력 제거 |
| 1162:7519 | WF-A02-로그인 | O | FULL | F-TSOXGG | POST /auth/login | 중복 프레임 동일 Route |
| 1162:7575 | WF-F24-내게시물상세-지역안건 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1162:7750 | WF-F25-내게시물상세-진행중투표 | O | FULL | F-PUDHYO / F-UCDVNA / F-FCPVIS | GET /posts/{postId}; 참여 API | 공통 상세/유형·권한 상태 |
| 1162:7932 | WF-E09-내지역안건-수정 | O | FULL | F-FTLHCX / F-GSMCLD | POST/PATCH/DELETE /posts/{postId} | 생성은 POST /posts; 비-MVP 입력 제거 |
| 1164:5414 | WF-L02-계정관리-이메일변경완료 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:5461 | WF-L03-이메일변경-발송실패 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:5514 | WF-L04-이메일변경-인증-불일치 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:5570 | WF-L04-이메일변경-인증-만료 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:5626 | WF-L04-이메일변경-인증-재발송 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:5682 | WF-L05-비밀번호변경-인증-발송완료 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:5895 | WF-L05-비밀번호변경-발송실패 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:5952 | WF-L05-비밀번호변경-인증-불일치 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:6006 | WF-L05-비밀번호변경-인증-만료 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:6060 | WF-L05-비밀번호변경-인증-재발송 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:6114 | WF-L07-관심키워드-저장실패 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:6182 | WF-K01-담당자-채택게시물 | O | FULL | F-CNNPYL | GET /officer/agendas | 유효 기관만 |
| 1164:6520 | WF-K01-담당자-월계1동 | O | FULL | F-CNNPYL | GET /officer/agendas | 유효 기관만 |
| 1164:6588 | WF-I02-북마크-유형-지역안건 | O | FULL | F-FYQJPT | GET /users/me/bookmarks | 필터 상태 |
| 1164:6742 | WF-I02-북마크-유형-지역활동정보 | O | FULL | F-FYQJPT | GET /users/me/bookmarks | 필터 상태 |
| 1164:6896 | WF-I02-북마크-유형-투표 | O | FULL | F-FYQJPT | GET /users/me/bookmarks | 필터 상태 |
| 1164:7050 | WF-I02-북마크-주제-교통 | O | FULL | F-FYQJPT | GET /users/me/bookmarks | 필터 상태 |
| 1164:7204 | WF-I02-북마크-주제-주거 | O | FULL | F-FYQJPT | GET /users/me/bookmarks | 필터 상태 |
| 1164:7637 | WF-I02-북마크-주제-안전 | O | FULL | F-FYQJPT | GET /users/me/bookmarks | 필터 상태 |
| 1164:7791 | WF-I02-북마크-주제-복지 | O | FULL | F-FYQJPT | GET /users/me/bookmarks | 필터 상태 |
| 1164:7945 | WF-I02-북마크-주제-생활정보 | O | FULL | F-FYQJPT | GET /users/me/bookmarks | 필터 상태 |
| 1164:8099 | WF-I02-북마크-주제-환경 | O | FULL | F-FYQJPT | GET /users/me/bookmarks | 필터 상태 |
| 1164:8253 | WF-I02-북마크-주제-기타 | O | FULL | F-FYQJPT | GET /users/me/bookmarks | 필터 상태 |
| 1164:8407 | WF-I03-내가만든게시물-지역안건 | O | FULL | F-SSHXAA | GET /users/me/posts | 유형 필터 |
| 1164:8708 | WF-I03-내가만든게시물-지역활동정보 | O | FULL | F-SSHXAA | GET /users/me/posts | 유형 필터 |
| 1164:8831 | WF-I03-내가만든게시물-투표 | O | FULL | F-SSHXAA | GET /users/me/posts | 유형 필터 |
| 1164:8954 | WF-L03-이메일변경-현재비밀번호불일치 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:9007 | WF-L03-이메일변경-이미사용중인이메일 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:9060 | WF-L06-비밀번호변경-형식오류 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:9113 | WF-L06-비밀번호변경-확인불일치 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:9352 | WF-L10-탈퇴-본인확인-비밀번호불일치 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:9400 | WF-L07-관심키워드-4개선택 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:9468 | WF-I04-반응한게시물-지역안건 | O | FULL | F-NZTUYE | GET /users/me/participations | 참여한 게시물로 구현; 반응 하위 필터 계약 미정 |
| 1164:9610 | WF-L02-계정관리-마이페이지진입 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:9657 | WF-I04-반응한게시물-지역활동정보 | O | FULL | F-NZTUYE | GET /users/me/participations | 참여한 게시물로 구현; 반응 하위 필터 계약 미정 |
| 1164:9799 | WF-L03-이메일변경-마이페이지진입 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:10057 | WF-I04-반응한게시물-투표 | O | FULL | F-NZTUYE | GET /users/me/participations | 참여한 게시물로 구현; 반응 하위 필터 계약 미정 |
| 1164:10199 | WF-L04-이메일변경-인증-마이페이지진입 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:10255 | WF-I04-반응한게시물-공감해요 | O | FULL | F-NZTUYE | GET /users/me/participations | 참여한 게시물로 구현; 반응 하위 필터 계약 미정 |
| 1164:10397 | WF-L05-비밀번호변경-인증-마이페이지진입 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:10451 | WF-I04-반응한게시물-필요해요 | O | FULL | F-NZTUYE | GET /users/me/participations | 참여한 게시물로 구현; 반응 하위 필터 계약 미정 |
| 1164:10593 | WF-L06-비밀번호변경-마이페이지진입 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:10849 | WF-I04-반응한게시물-댓글작성 | O | FULL | F-NZTUYE | GET /users/me/participations | 참여한 게시물로 구현; 반응 하위 필터 계약 미정 |
| 1164:10991 | WF-L02-계정관리-이메일변경완료-마이페이지진입 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:11038 | WF-I04-반응한게시물-댓글좋아요싫어요 | O | FULL | F-NZTUYE | GET /users/me/participations | 참여한 게시물로 구현; 반응 하위 필터 계약 미정 |
| 1164:11180 | WF-L04-이메일변경-인증-재발송-마이페이지진입 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:11236 | WF-L05-비밀번호변경-인증-발송완료-마이페이지진입 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:11290 | WF-I05-참여한투표-진행중 | O | FULL | F-QPGNCF | GET /users/me/votes | 실제 본인 선택 |
| 1164:11601 | WF-I05-참여한투표-종료 | O | FULL | F-QPGNCF | GET /users/me/votes | 실제 본인 선택 |
| 1164:11734 | WF-L05-비밀번호변경-인증-재발송-마이페이지진입 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:11788 | WF-H01-알림및활동-알림 | X | NON-MVP | 알림 피드 제외 | — | 활동 숫자는 I01 + activity로 구현 |
| 1164:11917 | WF-I07-기본활동지역-지역선택 | O | FULL | F-RBVFZX / F-QQKYLC | GET/PATCH /users/me; GET /regions | 기본 활동 지역 |
| 1164:11981 | WF-I08-관심지역-추가됨 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1164:12042 | WF-I08-관심지역-제거됨 | X | NON-MVP | 구현 제외 | — | 비-MVP 전용 화면 |
| 1170:7447 | WF-H01-알림및활동-활동 | X | NON-MVP | 알림 피드 제외 | — | 활동 숫자는 I01 + activity로 구현 |

### 매핑 보완

WF-C01-새추천성공은 추천 성공 전용이므로 제외하되 기본 게시판 WF-C01은 FULL이다. WF-F19/WF-F23의 추천 진입도 제외하고 안건 상세·AI 요약 자체는 공통 FULL에서 제공한다. WF-F18 독서모임 상세는 활동 상세로 재사용할 수 있지만 지도 대표로 연결하지 않는다. WF-I04의 공감/필요/댓글/평가 변형은 **참여 표시의 시각 참고**이며 API에 없는 행동별 필터를 추가하지 않는다. WF-H03 삭제 안내는 알림 없이도 삭제 상세 처리에 재사용한다. WF-L 메뉴를 '비활성 버튼'으로 전부 남기지 않는다.

## 7. 전체 사용자 Flow

| 사용자 여정 | 화면/행동 순서 | 성공·실패·복귀 |
| --- | --- | --- |
| 신규 | A01 → A06/A07 이메일 인증 → 비밀번호/약관 → A08 프로필 → A09 지역 → 가입 완료 | 인증 proof를 유지해 마지막 가입 요청. 원 상세가 있으면 복귀, 없으면 메인 |
| 기존 | A01 → A02 로그인 → 메인 | returnTo 우선; 실패는 원인 비구분 |
| 탐색 | B01 → B02 탐색 지역 → C01 게시판 → F 상세 | 목록 조건/스크롤 복구. 기본 활동 지역 유지 |
| 지도 | B01 미리보기 → D01 지도 → 동 말풍선 → 대표 상세 | 대표 없으면 안내, 활동 상세로 연결하지 않음 |
| 작성 | E01 유형 → E02/E03/E04 작성 → E05 사진 → 저장 → F 상세 | 실패 입력/파일 유지; 미인증 대상 지역은 제출 불가 |
| 일반 참여 | F 상세 → 반응/댓글/답글/평가 | 서버 성공으로 집계·기록 갱신 |
| 투표 | F04 선택 → 제출 → F11 결과 / 다른 선택 → 확인 → 변경 | 취소 시 기존 표 유지; 종료면 F09 결과-only |
| 공유 | 공유 복사 → G01 특정 상세 → 게스트 댓글/답글/AI·공개 결과 | 일반 탐색 없음. 로그인 요구 행동은 안내만 |
| 공유→회원 | G 상세 → G02 로그인 또는 가입 → 원 상세 | 원 행동 자동 실행 없음; G05/G09처럼 미인증이면 참여 제한 유지 |
| 이웃 | I01 → J01 지역/증빙 → 제출 → J02 접수 → 상태 조회 | 시연 완료 지역 반영 후만 지역 참여 |
| 기관 | I01/가입 후 기관 진입 → J03 정보 → J04 증빙 → 제출 → J05 접수 | 유효 완료 재조회 후만 파란 배지/담당자 메뉴 |
| 채택 | 유효 기관 → K01 전체 안건 → K02 담당 지역 → 채택 / K04 취소 | K03 타지역은 댓글·답글 열람만, 기관별 독립 |
| 개인 | I01 → 북마크/작성/참여/투표 → 상세 → 원 목록 | 각 필터와 위치 보존 |

가입 초기에는 로그인 세션이 없으므로 이웃 인증 '신청 제출'을 가입 전에 호출하지 않는다. A08의 이웃 인증 CTA는 가입 완료 후 신청 진입으로 연결하는 권장안이며 정확한 가입 후 CTA 배치는 PO/디자인 확인 대상이다.

## 8. Routing / Navigation [권장안]

아래 경로는 제품 확정값이 아니다. Figma 상태 변형별로 Route를 만들지 않는다. 모든 보호 Route는 세션 확인 중 콘텐츠를 미리 노출하지 않는다.

| Route | 화면 | MVP / 로그인 / 게스트 | 진입 조건·API | 뒤로가기·returnTo·상태 |
| --- | --- | --- | --- | --- |
| / | 시작 A01 | FULL / 불필요 / 공유게스트 일반 탐색용 아님 | 진입; API 없음 | 인증 회원 처리 기존 앱 확인 |
| /login | A02/G02 | FULL / 불필요 | auth/login | 공유 진입은 해당 상세, 일반은 시작. returnTo 유지 |
| /signup | A06~A09 | FULL / 불필요 | 이메일 발송/확인·regions·sign-up | 단계 back, 가입 draft 유지, returnTo 유지 |
| /home | B01/B03 | FULL / 필요 / 불가 | users/me·home | 루트; 탐색 지역 유지 |
| /explore/region | B02/B04 | FULL / 필요 / 불가 | regions | 호출 화면, 임시 지역만 |
| /board | C01/B05 | FULL / 필요 / 불가 | posts | home; region/type/topic URL 및 scroll 유지 |
| /map | D01/B08 | FULL / 필요 / 불가 | map/dongs | 출발 화면; 중심/선택 동/zoom 보존 |
| /posts/[postId] | F/B 상세 | FULL / 필요 / 공유는 별도 진입 | posts/detail·comments·summary | 목록/지도/개인 출처, 직접진입 fallback home |
| /posts/new | E01~E05 | FULL / 필요 / 불가 | 작성 지역 완료; posts | 출발 화면; 폼은 메모리 큐 |
| /posts/[postId]/edit | E06/E09 | FULL / 필요 / 불가 | canEdit; detail·PATCH | 원 상세; 서버 저장 전 draft 별도 |
| /shared/posts/[postId] | G01/G06~G08 | FULL / 불필요 / 특정 대상만 | 공유 컨텍스트 검증; detail·comments·summary | 일반 BottomNavigation 없음; 로그인 유도 returnTo는 같은 게시물 |
| /me | I01 | FULL / 필요 / 불가 | users/me·activity | home; 목록 origin 유지 |
| /me/profile | I06 | FULL / 필요 / 불가 | GET/PATCH users/me | me; 취소는 저장 전 값 |
| /me/region | I07 | FULL / 필요 / 불가 | regions·PATCH users/me | profile/me; 탐색 변경과 분리 |
| /me/bookmarks | I02 | FULL / 필요 / 불가 | bookmarks | me; type/topic 유지 |
| /me/posts | I03 | FULL / 필요 / 불가 | users/me/posts | me; type 유지 |
| /me/participations | I04 | FULL / 필요 / 불가 | participations | me; type 유지 |
| /me/votes | I05 | FULL / 필요 / 불가 | votes | me; ALL/OPEN/CLOSED 유지 |
| /verifications/neighbor | J01/J02 | DEMO / 필요 / 불가 | neighbor-verifications | me/이전 상세; 입력·첨부 큐/접수 조회 |
| /verifications/institution | J03~J05 | DEMO / 필요 / 불가 | institution-verifications | me/가입 후 진입점; 단계·파일 큐 |
| /officer/agendas | K01 | FULL / 필요 / 불가 | 유효 기관; officer/agendas | me; region/scope 유지 |
| /officer/agendas/[postId] | K02~K04 | FULL / 필요 / 불가 | 유효 기관; detail·comments·adoptions | 담당자 목록; 필터/scroll 유지 |

사진 미리보기·AI 요약·삭제 확인·투표 변경·저장 성공은 Route 아래 Local overlay로 구현 가능하다. shareToken을 보통 목록 URL에 붙이지 않는다. 공유→로그인 성공 후 회원 상세로 정규화하고 회원 권한을 조회한다. 원 게시물이 삭제되었다면 삭제 안내와 메인 이동을 제공한다. 인증 후 원 상세가 없어도 임의 다른 상세로 보내지 않는다.

## 9. Frontend Architecture [권장안]

디자인 토큰 → 공통 UI → 도메인 컴포넌트 → Screen → Page 순으로 조립한다. Route는 데이터 진입·가드를 연결하고 폼/투표 로직을 여러 Page에 복제하지 않는다.

| 계층/폴더 예시 | 책임 |
| --- | --- |
| app | Route, 화면 layout, 오류 경계; 실제 저장소 App Router 확인 후 적용 |
| components/ui | Button/Input/Header/Modal/Notice/Chip 등 제품 도메인 독립 UI |
| features/auth, profile, regions, verifications | 가입 reducer·세션·기본 지역·인증 상태 |
| features/posts | PostCard/PostDetail/PostForm/PhotoQueue, 유형별 확장 |
| features/participation | ReactionBar/CommentSection/VotePanel; postId 정본 공유 |
| features/personal, officer, summary | 개인 기록/기관 채택/AI 원문 연결 |
| lib/api | fetch·인증 전달·응답 parsing·ApiError·multipart |
| types/contracts, styles/tokens | 계약 DTO/코드값 매핑, CSS 변수 |

브라우저 입력/파일/선택 UI는 Client 경계, 서버용 비밀키는 서버 경계에서만 다룬다. 실제 토큰 보관/쿠키 방식은 합의 전 확정하지 않는다. 인증 개인 데이터와 공유게스트 캐시를 분리한다. 새 폴더 구조를 기존 구조에 강제하지 않는다.

## 10. Design Token

CSS 변수 또는 기존 Tailwind 토큰 계층으로 한 곳에서 관리한다. Tailwind 버전에 맞는 연결 방식은 실제 설정을 확인한다.

| 토큰 | 값 | 적용 |
| --- | --- | --- |
| primary / selected | #0F766E / #0F7662 | 일반 액션 / 선택 칩·글쓰기; 서로 합치지 않음 |
| background / surface | #F8FAFC / #FFFFFF | 화면 / 카드·입력·헤더 |
| text / secondary | #1F2937 / #667085 | 제목·본문 / 메타 |
| soft / border | #E6F4F1 / #D5DDE3 | 안내·AI·선택 / 1px 테두리 |
| disabled / muted | #E9EEF1 / #94A3B8 | 비활성 배경 / 글자 |
| error / errorSoft | #D92D20 / #FFF1F0 | 오류 |
| warning / warningSoft | #B7791F / #FFF7E6 | 주의 |
| chip default bg/text | #F3F4F6 / #4B5563 | 비선택 필터 |
| layout | 396×852 기준; 좌우16, 콘텐츠364 | 작은 기기에서 width:100%, 고정852 높이 금지 |
| spacing | 콘텐츠 위20/아래32, section12, internal8 | 일반 레이아웃 |
| size | Header58, BottomNavigation76, Button52 | 안전영역 추가와 중복 padding 점검 |
| radius | Button/Card/Menu14, Input/Notice/Comment10, Chip999 | 역할별 구분 |
| shadow/card | 0 3px 10px 0 rgba(15,23,41,.04) | 실제 카드 |
| shadow/nav | 0 -2px 8px 0 rgba(15,23,41,.05) | 하단바 |

상태바44는 Figma 시안 값이다. 웹에서 9:41/시스템 아이콘을 앱 UI로 고정 복제하지 않는다. 실제 브라우저 safe-area와 sticky/fixed 레이아웃을 반영한다. 그림자는 존재하는 역할에만 적용한다.

## 11. Typography

| 역할 | Pretendard size / line-height / weight |
| --- | --- |
| Header | 18 / 27 / 600 |
| Section | 18 / 27 / 700 |
| Card title·질문 | 17 / 26 / 600 |
| Body·Input | 15 / 23 / 400 |
| Button | 15 / 23 / 600 |
| Caption·라벨·Chip | 12 / 18 / 500 |
| Navigation | 11 / 17 / 500 |
| Brand / 부가 문구 | 20 / 27 / 700; 9 / 약12.15 / 400 |

자간 기본0. 등록 heading24/34/700을 모바일 Header18로 혼동하지 않는다. A01 슬로건은 디자인기준에 Inter Bold16 예외가 남아 있다. 기본 UI는 Pretendard이며 시작 예외를 일괄 변경하지 않고 디자인에 확인한다. 긴 제목·한글 줄바꿈·폰트 로딩 후 높이·키보드 표시를 확인한다.

## 12. Common Components

Figma 공통 원본이 있다는 사실은 FE 컴포넌트 구현 완료를 뜻하지 않는다. 디자인기준 v2의 규격을 가져오고 아래 props/state는 권장 인터페이스다.

| 컴포넌트 | 역할·Props | Variant / State | 사용 화면·API 관계 |
| --- | --- | --- | --- |
| Button | children,type,onClick,disabled,pending | primary/secondary/danger; idle/pending | 제출 중 중복 방지; 성공 후만 이동 |
| Input | id,label,value,onChange,type,error,description | 기본/invalid/disabled | 가입/프로필/작성; field error 매핑 |
| TextArea | label,value,onChange,error,rows | 기본/invalid; 내용 높이 확장 | 본문·댓글·소개; 실패 내용 유지 |
| Header | title,backTarget,onBack,action,variant | back/action/main, 높이58 | 일반24 back 정렬·8 gap, Main32 action; 미구현 설정 액션 제거 |
| BottomNavigation | items,activeItem,onNavigate | 선택/비선택; 슬롯 **미정** | 회원 탐색만; 게스트 없음; 알림 결정을 props 데이터로 교체 가능 |
| PostCard | post,onOpen,myParticipation | 사진/무사진/본인 투표/접근불가 기록 | B/C/I/K; 공통 postId 원본, 카드에 북마크 토글 없음 |
| CommentItem | comment,onReply,onEvaluate,capabilities | 부모/답글22 들여쓰기/게스트 | 내용·평가·대상명, replies는 부모 컨테이너 |
| Menu | label,value,href,disabled | 기본/선택 | 마이·지역; 비-MVP 메뉴 제외 |
| Notice | tone,title,children,action | info/warning/error/success | 높이42/60/78 참조, 장문 auto; API 사유 안내 |
| Chip / ChipSelected | label,selected,onSelect,disabled | 하나의 selected prop; 높이34/장문52 | type/topic/속성; ALL은 조회 시 생략 |
| Photo | src,alt,onOpen | 목록/상세/미리보기 | 원본364×150 참조; 없음은 영역 생략 |
| Attachment | name,sizeBytes,onRemove,status | queued/pending/failed/submitted | J/E; 파일명/크기, 공개 증빙 URL 없음 |
| Toggle | checked,onChange,label,disabled | ON/OFF; 트랙44×26 | 기반 재사용 후보; 비-MVP 알림/다크 설정을 만들지 않음 |
| VoteCard | poll,myOptionId,temporaryOptionId,canVote,onSelect,onSubmit | open/unvoted/selected/submitting/voted/closed/restricted | B/F/I/G; 본인 선택은 집계최대에서 추정 금지 |
| Modal | open,title,description,onConfirm,onCancel,pending | confirm/destructive/info | 투표 변경/삭제/로그인 안내; focus trap·복원 |
| BottomSheet | open,title,onClose,children | 선택/행동 목록 | 지역/행동 패널이 필요할 때; 규격 미확정은 별도 디자인 승인 |
| EmptyState | title,description,action | 목록/기록 없음 | 데이터0 정상 상태; 오류·삭제 안내와 분리 |

추가 도메인: PostForm/PostDetail/CommentSection/ReactionBar/VerificationForm/AdoptionPanel/SummaryPanel. Button→VoteCard→PostDetail→Page로 조립한다. 표준 form label, keyboard, aria-pressed, radio semantics, focus-visible, dialog 취소를 구현한다. Loading은 pending prop과 연동하고 성공/오류 안내는 스크린리더에도 전달한다.

## 13. 사용자 상태 모델

| 상태 축 | 값·정본 | UI 영향 |
| --- | --- | --- |
| session | guest/member + 초기 확인 중 unknown | unknown에서 보호 콘텐츠/참여 버튼 미노출 |
| profile | nickname,bio,attributes,photo,activityRegion | attrs는 권한 아님 |
| neighbor | 미신청/접수/완료; 완료 지역 목록 최대3 | 완료 목록에 대상 post.regionId 있어야 참여 |
| institution | 미신청/접수/완료/만료, responsibleRegion,validUntil,isActive | 유효 완료에서만 기관 메뉴/배지/채택 |
| access context | member / post-bound shared guest | 공유 대상 댓글만 예외 허용 |

로그인 ≠ 지역 참여. 기본 활동 지역 설정 ≠ 이웃 인증. 기관 인증 ≠ 이웃 인증. 완료 지역/역할/배지를 form 값 또는 localStorage flag로 생성하지 않는다.

## 14. 권한 / capabilities

| 작업 | 회원 미인증/타지역 | 해당 지역 완료 | 공유 게스트 | 유효 기관 |
| --- | --- | --- | --- | --- |
| 일반 탐색 | O | O | X | O |
| 공유 대상 공개 상세 | O | O | O | O |
| 게시/댓글/답글/반응/평가/표 | X | O | 댓글/답글만 O | 이웃 자격 별도 필요 |
| 북마크 | O | O | X·로그인 안내 | O |
| 본인 수정/삭제 | X | 소유권·유형 조건 | X | 이웃/소유권 별도 |
| 기관 목록/채택 | X | X | X | 목록 전체, 채택 담당지역 공개안건만 |

capabilities의 canComment/canReact/canEvaluateComment/canVote/canBookmark/canEdit/canDelete/canAdopt/canCancelAdoption을 렌더 조건으로 사용한다. 답글은 canComment와 동일 자격이나 DTO 별도 필드는 서버와 합의한다. 버튼 비활성 이유는 세션/기관상태/대상 지역 등으로 설명하되 권한 전체를 Role로 독자 재계산하지 않는다. capability 없는 경우 권한을 허용으로 추정하지 않고 계약 오류/재조회로 처리한다.

게스트의 회원 기능은 저장 버튼을 활성 쓰기로 만들지 않으면서 로그인 안내 진입을 제공한다. 로그인한 미인증 회원에게는 이웃 인증 필요를 안내한다. 유효하지 않은 회원 세션 실패를 게스트 댓글 제출로 자동 재시도하지 않는다. 403/409 뒤 detail/users/me를 재조회한다. 서버 요청시점 검증이 최종이다.

## 15. 전역 상태 / Local State

| 값 | 권장 위치 | 보존·정리 |
| --- | --- | --- |
| session/user/profile/verifiedRegions/institution | 서버 상태 캐시 + 세션 provider | 인증 주체/완료/만료 변경 후 갱신; 회원 전환 시 캐시 격리 |
| activityRegion | user.profile의 저장값 | PATCH 성공 후만 정본 변경 |
| currentExploreRegion | 탐색 Context + 목록 URL regionId | 메인 임시 선택으로 PATCH 호출 금지 |
| responsibleRegion | 기관 상태 서버값 | 탐색 지역으로 덮어쓰기 금지 |
| returnTo | 검증된 내부 복귀 Context | 성공 복귀/취소 후 정리; action payload 저장 없음 |
| filters/sort/cursor/scroll | URL + 해당 목록 복귀 cache | 조건 변경 때 cursor 초기화, 상세 왕복 때 복원 |
| signup/post/verification draft + files | 흐름별 메모리 reducer/Context | Route 왕복/실패 유지. 새로고침 영속 저장은 MVP 임시저장 아님/보장하지 않음 |
| temporaryOptionId/modal/pending/summary | 화면 Local 또는 query 상태 | 서버 표와 분리; 완료/종료/취소 때 명시적으로 정리 |

File 객체/비밀번호/증빙을 localStorage에 저장하지 않는다. 가입 중 필요한 입력은 메모리에 유지하고 복귀 경로만 안전하게 보존한다. 서버 상태를 전역 store에 복제해 두 개의 정본으로 만들지 않는다.

## 16. API Client 기본 원칙

- BaseURL `/api/v1`는 계약안, 실제 주소는 환경 설정으로 주입한다. public 환경에는 지도 공개 설정 등 공개값만, AI/server 비밀키는 넣지 않는다.
- `request<T>()`는 정상 JSON data/meta, 204 무본문, 오류 code/message/details/traceId, 네트워크 취소를 구분한다. 오류 때 HTTP 성공으로 감싸지 않는다.
- 인증 전달은 Bearer 제안과 실제 BE 대조. 공유 헤더 `X-Post-Share-Token`은 해당 공유 postId 요청에만 사용한다. 게시판/마이로 전파하지 않는다.
- 401은 세션과 유효 공유 컨텍스트를 구분해 로그인 안내. 로그인 실패는 통일 문구. 403 자격 안내, 404 콘텐츠 미노출, 409 충돌/종료/변경 확인, 413/415 파일 오류, 422 field/금칙어, 429 서버 허용시각 표시.
- GET 실패 Retry는 화면에서 명시 가능. 게시·댓글·인증 POST를 네트워크 오류만으로 자동 반복하지 않는다. 합의된 멱등 키가 있을 때만 같은 요청으로 재시도한다.
- 늦게 도착한 이전 지역/필터 응답은 새 화면을 덮어쓰지 않도록 query key/request identity·AbortSignal을 사용한다.
- 각 기능의 성공은 서버 응답 확인 후 확정한다. Mock adapter와 실 API adapter를 구분하고 release에서 mock 권한/seed를 자동 켜지 않는다.

## 17. Start / Login / Signup

시작은 최종디자인 A01의 로고/이미지·슬로건과 로그인/가입 CTA를 사용한다. 이미지 추출이 아직 제공되지 않았으므로 실제 구현 때 해당 노드 자산을 가져온다. 다른 페이지의 시작 이미지를 대신 쓰지 않는다. 로그인 A02와 공유 복귀 G02는 같은 폼에 안내문/returnTo만 다르게 준다. 비밀번호 찾기 링크는 제외한다.

가입은 이메일 인증 → 비밀번호/확인·약관 → 프로필 → 활동 지역 완료다. 화면 단계별 입력을 하나의 SignupDraft로 유지하고 마지막 성공 요청으로 계정/프로필/지역을 확정한다. API 제안은 마지막 sign-up에 profile을 포함하므로 가입 전에 PATCH /users/me를 호출하지 않는다.

| 입력·상태 | 규칙 |
| --- | --- |
| code | 6자리 문자열, 선행0 유지; 유효5분·재발송60초·이메일별30분 최대5회·코드별 입력5회 |
| code timer | expiresAt/resendAvailableAt 서버값으로 남은 시간 계산; 클라이언트 카운트가 서버 제한을 대체하지 않음 |
| resend | 성공한 새 발급만 이전 code/proof 무효로 교체. 실패를 발송 성공 상태로 만들지 않음 |
| email 변경 | 기존 이메일 proof 재사용 금지; 인증 완료 상태 제거 |
| password | 8~64자, 영문+숫자, 공백 불가, 확인 일치. 특수문자 필수 아님 |
| agreements | 필수 이용약관/개인정보 동의 각각 필요. marketing 선택; 전체 동의는 개별 상태 집계 |
| pending | 발송/확인/가입/로그인 각각 분리; 동일 요청 중복 클릭 차단 |

필요 API: POST /auth/email-verifications `{email,purpose:SIGN_UP}` → expiresAt/resendAvailableAt; POST /auth/email-verifications/confirm `{email,purpose,code}` → verificationToken/verifiedAt; POST /auth/sign-up `{email,emailVerificationToken,password,passwordConfirmation,agreements,profile}` → user/accessToken; POST /auth/login `{email,password}` → user/accessToken.

불일치 `인증번호가 일치하지 않습니다.`; 만료 `인증번호가 만료되었습니다. 다시 발급받아 주세요.`; 로그인은 `로그인에 실패했습니다`로 원인 비구분. 전달 실패/429/필수동의/중복이메일/비밀번호정책 오류는 각 필드 또는 Notice. 성공 후 세션 확정·users/me 확인·복귀 처리. 회원가입/로그인 실패는 이메일·필요한 draft/returnTo 유지, 비밀번호는 메모리에만 유지한다.

## 18. Profile / Activity Region

프로필은 사진, 최대10자·중복불가 닉네임, 최대50자 소개, 거주자/학생/직장인/상인 복수 속성, 기본 활동 지역을 제공한다. 속성은 UI 설명이며 참여 자격이 아니다. 사진 제한은 확인 필요다.

지역 후보 선택에서 저장 전 local selectedRegionId와 저장된 activityRegion을 분리한다. 가입에서는 SignupDraft에 반영하고 마지막 sign-up으로 저장; 기존 회원은 PATCH 성공 후 profile에 반영한다. 메인 탐색 지역 변경은 이 API를 호출하지 않는다. 지역명 보조 검색은 합의된 regions q 계약만 사용하며 자유 게시물 검색과 구분한다.

필요 API: GET /regions → id/name; GET /users/me → profile/인증상태; PATCH /users/me `{nickname,bio,residentAttributes,activityRegionId}` → 최신 프로필. 사진은 payload+profileImage, 제거 removeProfileImage 제안. 교체·제거 동시 요청 금지. NICKNAME_ALREADY_IN_USE/REGION_NOT_FOUND/파일 오류는 입력 유지. 저장 후 기존 게시물 작성자 표시를 같은 공개 프로필에서 재조회한다.

## 19. Home

B01을 Main Header·탐색 지역·지역 변경·통합 게시판 CTA·지도 미리보기·진행중 투표·지역 게시물·하단바로 분해한다. B03의 미인증 안내는 선택 탐색 지역 기준으로 보여준다. 사진 없는 PostCard는 이미지 공간을 제거한다. 검색/추천/전용 인기 운영/설정·알림 버튼은 범위에서 제외한다. 하단 슬롯은 미결정 상태로 기록한다.

필요 API: GET /home?regionId → region/posts/openVotes/boardCounts; 지도 미리보기에 GET /map/dongs를 공통 조회로 연결. API에 없는 지도 데이터를 home에 있다고 가정하지 않는다. 최초 기본 활동 지역, 이후 currentExploreRegion 사용. 이웃 미완료 Notice는 읽기 가능/참여 제한을 설명하고 북마크는 로그인 자격으로 허용한다. 빈 경우 섹션/게시판 CTA 유지와 `아직 등록된 게시물이 없습니다.`. home 또는 지도만 실패하면 해당 영역 Retry, 이미 성공한 영역 유지. 투표 CTA는 같은 postId 상세로 이동하며 홈에서 자동 투표하지 않는다.

## 20. Board

C01은 Header·탐색 지역·유형(전체/안건/활동/투표)·주제(전체/7개)·PostCard 목록·후속 페이지로 구성한다. AI 추천/새추천/검색 영역은 제외한다. 지역→유형→주제 조건을 적용한다.

필요 API: GET /posts `{regionId,type?,topic?,cursor?,size?}` → 카드 배열/meta. ALL은 query 생략. 최초 skeleton, data, empty, error를 구분한다. 필터 변경 시 cursor와 해당 목록 위치 초기화. 상세 왕복에서는 동일 조건/목록 캐시/scroll 복원. 페이지 실패는 기존 카드 유지하고 후속 조회 Retry만 제공한다. 게스트 일반 탐색은 차단한다.

## 21. Map

D01/B08은 기본 활동 지역 중심 지도, 확대/축소, 동 선택 말풍선, 대표 PostCard, 상세 진입으로 구성한다. GPS·실시간 위치 추적·위치 권한 없음. 이후 탐색 지역을 지도 중심으로 연결하는 전달은 centerRegionId 권장안이며 기본 지역 저장을 바꾸지 않는다.

필요 API: GET /map/dongs?centerRegionId → centerRegion/dongs[].region/representativePost. 대표는 동별 공개 안건/투표 중 반응3종 합계 최대, 동률 최신. 활동 정보·댓글·득표 수 제외. 첫 사진 썸네일만 사용하고 없으면 영역 제거. representativePost=null인 동도 유지하고 `등록된 게시물이 없습니다` 안내. API/지도 renderer 실패를 구분하여 D03 Retry 제공. 지도 provider·경계/좌표 원천은 미정이며 좌표를 임의 생성하지 않는다. 반응 변경 후 지도 재조회로 대표 갱신한다.

## 22. Post 공통 구조

Post = id/type/region/topic/title/content/author/images/createdAt/updatedAt/status. LOCAL_AGENDA는 공통원본, LOCAL_ACTIVITY는 activity 확장, VOTE는 vote 확장. 카드·상세·작성폼은 공통 구조에서 해당 확장만 추가한다. content/body 이름을 화면별로 혼용하지 말고 DTO adapter에서 통일한다. 상세/메인/지도/개인기록이 같은 postId와 집계를 참조한다.

## 23. Local Agenda

지역·주제·제목·본문 필수, 사진 선택. activity/details·임시저장·참고 링크·익명·AI 이미지 없음. 게시 지역은 완료 이웃 지역 자격을 확인한다. AI 요약은 안건 상세에 별도 읽기 영역이며 작성자의 생성 버튼/프롬프트 입력을 새 요구로 만들지 않는다.

필요 API: POST /posts multipart payload `{type:LOCAL_AGENDA,regionId,topic,title,content}` + images → 최신 PostDetail. VALIDATION_ERROR/NEIGHBOR_VERIFICATION_REQUIRED/MEDIA_LIMIT_EXCEEDED면 폼/사진 유지. 성공 시 해당 상세로 이동하고 home/board/myPosts/map 갱신.

## 24. Local Activity

공통 Post + 출처·일정·장소·활동 상태 필수, 외부 참여 링크 선택. 예정/진행/종료/취소는 작성자가 선택하고 **기본값 없음**. 시간 경과로 자동 전환하지 않는다. 종료/취소면 외부 참여 CTA 비활성, 예정/진행으로 돌아오면 유효 링크일 때 재활성. 내부 신청/결제 없음.

필요 API: POST/PATCH /posts의 details `{source,schedule,place,activityStatus,externalParticipationUrl?}`. 상세 응답 activity의 `status`와 작성의 `activityStatus`는 adapter로 매핑한다. 문의는 작성자의 현재 로그인 이메일 organizerEmail로 표시하고 별도 입력받지 않는다. null이면 `주최자 문의 정보를 확인할 수 없습니다.`. 출처 필드와 제외된 referenceLink는 다르다. 문의 이메일·외부 링크·상태 각각 오류/누락을 분리한다.

## 25. Vote Post 작성

공통 Post + 질문·선택지2~10·종료시각 필수. 처음 최소2개 입력, 안정적인 local key로 추가/제거, 2개에서는 제거 제한·10개에서는 추가 비활성. 특정 찬반 내용 강제 없음. 종료 시각의 시간대는 절대 ISO offset으로 계약하고 표시 Asia/Seoul. 과거 시각 허용/중복 선택지/최대문자 수는 미정.

필요 API: POST /posts payload type=VOTE, details `{question,options:string[],endsAt}` + images. 작성 options local key와 저장 후 optionId를 혼동하지 않는다. VOTE_OPTIONS_INVALID/validation 오류면 draft 유지. 투표 알림/예약 필드 없음. 작성 성공은 투표 참여가 아니며 최초 표를 자동 생성하지 않는다.

## 26. Post Create / Edit / Delete

| 동작 | 화면·권한·요청 | 성공/실패 |
| --- | --- | --- |
| create | 유형 E01→공통/확장→사진→POST /posts; 인증지역 | 성공 최신 상세 이동; 실패 모든 draft 유지 |
| edit | canEdit 확인→GET detail→서버값과 editDraft 분리→PATCH /posts/{id} | 성공 상세/목록/원문 요약 갱신; 실패 editDraft 유지 |
| delete | canDelete→확인 Modal→DELETE /posts/{id} | 204 후 목록으로 이동·전체 관련 캐시 제거/무효화; 실패 상세 유지 |

진행 투표 수정은 제목·본문·사진·종료시각만, 지역·주제·질문·선택지는 readonly이며 PATCH에서 제외한다. 종료 투표는 수정·삭제 불가. 서버 상태 종료 경합이면 409 안내 후 재조회한다. 안건/활동의 세부 수정 허용 목록은 계약 합의 필요, 유형 전환 기능 임의 추가 없음.

사진 PATCH의 photoOrder 제안은 생략=기존 유지, []=전부 제거, 지정=최종 전체순서. 기존 `{photoId}`와 신규 `{newImageIndex}` 혼합, images 순서와 index 매핑을 검증한다. 최종 사진 전체 제한을 기존+신규에서 계산한다.

삭제 즉시 공개 상세/공유/댓글/요약/추가 참여 차단, 북마크 자동 해제·목록 제거. 참여 투표 기록은 접근불가 카드로 유지하고 과거 본문/선택지 캐시를 표시하지 않는다. 물리 데이터 보존은 미정이다.

## 27. Post Detail

공통 Header→유형/지역/주제→최신 작성자/기관 배지/시각→제목/원문→있을 때만 사진→유형 확장→반응→북마크/공유→댓글/답글/평가/정렬로 조립한다. 안건은 AI 요약/채택기관 기록, 활동은 출처/일정/장소/상태/문의/외부 링크, 투표는 VotePanel 확장. 본인 메뉴는 capability 기반 수정/삭제만.

필요 API: GET /posts/{id} → 공통 필드, activity/vote, reactionCounts/commentCount, myState/capabilities, adoptions, comments/commentsMeta. 초기 댓글을 포함하면 후속 comments와 같은 정렬/key를 사용해 중복하지 않는다. 초기 댓글 생략 가능 계약이면 CommentSection이 별도 조회한다.

404/삭제에는 제목/사진/본문 캐시를 지우고 H03식 안내를 제공한다. 재조회 실패로 공개 상태가 불명확한 것과 확정 삭제는 구분한다. 게스트에 myState/본인 선택 생략을 false/null로 임의 위장하지 않는다. 일반 회원 미인증 상세는 본문·결과·의견 열람, 참여 제한 Notice, 북마크 가능.

## 28. Reaction

공감해요/필요해요/궁금해요는 각각 독립 선택이다. 이미 선택한 유형 재클릭은 해당 유형만 취소한다. 세 개 동시에 선택 가능. 반응 총수는 세 관계 수의 합계다.

필요 API: PUT /posts/{id}/reactions/{EMPATHY|NEEDED|CURIOUS}, DELETE 동일 경로 → reactionCounts/myReactions. 게스트는 로그인 안내만, 회원 미완료는 지역 인증 안내. 유형별 pending + 한 게시물 응답 정합성을 관리해 늦은 응답이 다른 유형을 덮어쓰지 않게 한다. 권장 초기는 서버응답 기반으로 구현하고 안정화 뒤 제한적 optimistic 적용.

## 29. Comment / Reply

댓글 작성은 content만, 답글은 원부모/답변 대상 표시를 분리한다. 답글에 답해도 원부모 아래 한 단계로 붙이고 @대상명을 표시한다. member 해당지역 완료 또는 shared guest만 작성 가능, 게스트 공개명 정확히 `게스트`. 전화/이메일 인증 없음.

필요 API: GET /posts/{id}/comments?sort&cursor&size; POST /posts/{id}/comments `{content}`; POST /comments/{commentId}/replies `{content,replyToCommentId?}` → Comment. 서버는 path/대상의 같은 게시물·원부모 귀속을 검사한다. replyToCommentId 매핑은 계약안이다.

빈 내용·금칙어 포함 거부, 동일 사전 문자열 포함 검사를 회원/게스트/댓글/답글에 적용, 통과하면 즉시 공개. 실제 금칙어 목록 미정이며 FE에서 임의 목록을 확정하지 않는다. 실패는 내용/대상/정렬 유지, 성공 확인 후만 입력 비우고 부모/댓글수 갱신한다. 댓글/답글 수정·삭제 버튼/API는 미정이므로 제공하지 않는다.

## 30. Comment Evaluation / Sort

좋아요↔싫어요 상호배타, 반대 클릭 PUT 전환, 동일 클릭 DELETE 취소. 댓글과 답글 동일. API PUT /comments/{id}/evaluation `{type:LIKE|DISLIKE}` / DELETE → myEvaluation/likeCount/dislikeCount. 게스트는 로그인 안내, 회원 이웃 완료 필요. 각 항목 pending 분리.

LIKES = 부모 likeCount 내림차순, 동률 최신 부모. LATEST = 부모 createdAt 내림차순. 답글 likeCount를 부모에 합산하지 않는다. sort 변경→GET comments 새 cursor로 조회, 답글을 독립 root로 정렬하지 않는다. 정렬 변경 중 이전 데이터에는 갱신 중 표시, 오류 때 이전 정렬 데이터 유지. 답글 순서·초기 페이지·동률 추가 키는 BE 합의 필요.

## 31. Vote Participation

| 상태 | 화면 행동 | 요청/전이 |
| --- | --- | --- |
| OPEN + unvoted | 결과/선택지, 제출 disabled(선택 전) | 선택→temporarySelected, API 없음 |
| temporarySelected | 임시 radio와 제출 | 제출→submitting, PUT vote confirmChange=false |
| voted | 실제 myOptionId + 집계 표시 | 다른 선택은 temporary만 변경 |
| changeConfirm | `선택을 변경하시겠습니까?` | 취소→temporary를 기존 표로 복원, 확인→confirmChange=true 요청 |
| submitting | 중복 클릭/선택 변경 차단 | 응답 실제선택/수/율/participantCount로 확정 |
| CLOSED | 결과·실제 본인표(회원)만 | 제출/변경 UI 없음 |
| restricted / guest | 공개 결과 열람 | 회원 인증 안내 / 게스트 로그인 안내 |
| error | 입력/기존표 보존 | 최신 서버 조회로 결과불명/종료 복구 |

필요 API: PUT /posts/{id}/vote `{optionId,confirmChange}` → 최신 vote. 서버 기존 myOptionId와 temporaryOptionId를 별도 관리한다. 같은 선택이면 변경으로 처리하지 않는다. 409 VOTE_CHANGE_CONFIRMATION_REQUIRED를 받으면 최신표 재조회·사용자 확인 후만 요청, VOTE_ENDED는 종료 결과로 갱신. 제출 성공 전에 본인표/participantCount를 증가시키지 않는다. 변경은 전체 한 표를 교체한다. 최다 득표 옵션과 본인 선택 강조는 별도다. 0표와 동률·소수율 표시 기준은 계약 확인. WebSocket/SSE/고정 polling은 확정 요구가 아니다.

## 32. Share

공개 상세의 공유 버튼→GET /posts/{id}/share-link→shareUrl 복사/플랫폼 공유. 복사 Promise 성공 후만 '복사 완료', 권한 거부/브라우저 미지원이면 읽을 수 있는 링크와 수동 복사 fallback을 제공한다. 게스트는 받은 링크를 복사하고 회원 전용 링크발급 API를 호출하지 않는다. shareUrl을 FE가 서버 발급 없이 조합하는 방식은 별도 합의 전 적용하지 않는다.

## 33. Guest

공유 특정 postId의 본문/사진/공개반응수/댓글·답글/투표결과/안건요약/게스트댓글·답글만. BottomNavigation·메인/게시판/지도/마이/글쓰기 링크 없음. 공유 토큰은 대상에 귀속되어 다른 postId·다른 댓글·요약에서 재사용 불가. 로그인한 미인증 회원이 공유토큰으로 게스트 작성 자격을 우회하지 않는다. 탈락/만료/삭제 컨텍스트는 콘텐츠 없이 안내, 게스트 의견은 가입 후 회원 기록에 자동 이관하지 않는다.

## 34. returnTo

1. 로그인 필요 행동 클릭 시 내부 상세 식별자/정규화 경로만 보존한다. 액션을 실행할 command나 pending POST는 저장하지 않는다.
2. 안내→login/signup 단계·profile/region 동안 유지한다.
3. 성공 후 해당 상세의 현재 존재/권한을 재조회하고 회원 컨텍스트로 복귀한다.
4. 원상세 없으면 삭제 안내/메인 fallback, returnTo 없으면 메인.
5. 반응/평가/투표/북마크는 사용자가 다시 클릭해야 실행된다.

외부 URL·protocol-relative·허용하지 않는 경로는 복귀에 쓰지 않는다. 로그인 후 이웃 미완료 상태면 읽기만 가능하며 북마크만 회원 자격으로 가능. 복귀 Context와 일반 목록 origin은 다른 값이다. 기관 인증을 위해 로그인한 뒤 신청 화면으로 돌아오는 auth continuation은 상세 returnTo와 별도 용도/허용경로로 구분해 합의한다.

## 35. Bookmark

상세 단일 버튼만. 회원은 지역 미인증이어도 가능. PUT /posts/{id}/bookmark / DELETE → isBookmarked. 등록 성공 `저장되었습니다` 팝업 후 현재 상세 유지. 게스트는 안내·복귀만, 성공팝업/저장 요청 없음. 카드별 북마크 버튼을 추가하지 않는다. 등록/해제 뒤 북마크 목록/activity 갱신, 북마크만으로 participations에 넣지 않는다.

## 36. MyPage

I01의 최신 프로필/소개/속성/기본 지역·실제 행동 횟수·북마크·작성/참여게시물·참여투표·이웃/기관 인증·활동지역을 구성한다. 계정/관심/알림/설정/탈퇴 메뉴 제외. 유효 기관만 담당자 메뉴 노출. GET /users/me와 GET /users/me/activity를 독립 상태로 조립, 한 API 실패로 나머지 성공영역을 지우지 않는다.

## 37. My Posts

GET /users/me/posts?type&cursor&size, 내가 쓴 원본만. 전체/3유형 필터, 상세 이동, capability 수정/삭제. 종료투표 메뉴 없음. 본인 작성 자체는 참여 게시물의 근거가 아니다. list origin/type/scroll 보존. 필터별 빈 상태 제공.

## 38. Participated Posts

GET /users/me/participations?type&cursor&size. 현재 유효한 내 반응/댓글·답글/평가/표를 postId당 한 카드로 모으고 myParticipation의 모든 유효 표시를 같이 보여준다. 북마크만/작성만/타인이 내 글에 참여한 것은 제외. 반응 취소 때 다른 참여가 남으면 카드 유지. I04의 일부 반응 필터 시안은 확정 API 필터가 아니므로 type 이외를 임의 추가하지 않는다. empty `아직 참여한 게시물이 없습니다` + 게시판 CTA.

## 39. Participated Votes

GET /users/me/votes?status=ALL|OPEN|CLOSED&cursor&size. 제출한 투표만, 실제 본인 선택/현집계/남은 시간·최종결과. 작성만/선택만은 포함하지 않는다. availability=UNAVAILABLE면 기록·participatedAt만 남기고 post/vote null을 처리, 삭제 제목/선택텍스트/이미지를 캐시로 복원하지 않는다. 삭제기록과 OPEN/CLOSED 필터 관계는 미정이며 전체에서 기록 유지.

## 40. Activity

GET /users/me/activity → totalCount/counts. I01 숫자를 현재 카드 수/집계로 재계산하지 않는다.

| 행동 | 누적 횟수 |
| --- | --- |
| 게시물 작성·북마크 등록·각 반응 등록·댓글/답글·댓글/답글 평가 최초등록·투표 최초참여 | 각각 +1 |
| 조회·수정·삭제·설정·취소·평가 직접전환·투표변경·동일 state 재시도 | +0, 기존 횟수 차감 없음 |
| 취소 뒤 재등록 | 다시 +1 |

기관 채택은 이 주민 활동 항목에 임의 추가하지 않는다. 활동은 알림 API 없이 조회된다. 성공 쓰기 뒤 activity를 무효화하되 FE 임시 카운트가 정본이 되지 않는다.

## 41. Neighbor Verification

지역 선택→증빙 선택→파일명/크기/제거목록→최종 제출→접수. GET /users/me/neighbor-verifications → requests/verifiedRegions; POST 동일 multipart `{regionId,evidenceFiles}` → id/region/status/submittedAt. 업로드 큐는 제출 전 local, 선택만으로 접수 아님. 실패 입력/정상 파일 모두 유지. 접수는 Notice/재조회 상태이며 참여 자격 없음.

미신청/접수/완료는 requests와 완료지역을 함께 표현, 완료지역 최대3. 이를 신청건수 최대3으로 바꾸지 않는다. 서버 시연 seed/script의 완료지역을 재조회→users/me/detail capability 갱신. 프론트에 '승인' 버튼/체크 후 완료 전환 기능 없음. 이웃 증빙 인정종류/형식/개수/용량은 미정, 게시사진/기관 제한 전용 금지.

## 42. Institution Verification

J03의 기관명/부서/직책/담당자이름/업무이메일/전화/담당지역 필수→J04 재직증명→자료제출→J05 접수. 추가 최종확인 화면 없음. 업무이메일 6자리 인증 없음.

GET /institution-verifications → 본인정보/파일/상태/완료·만료·담당지역; POST multipart `institutionName,departmentName,positionName,applicantName,workEmail,phoneNumber,responsibleRegionId,employmentCertificates` → 접수상태/files/isActive/institutionVerified. PDF/JPG/PNG, 개별10MB, 전체50MB, 개수 제한 없음. 선택과 최종 제출과 완료를 분리한다. 접수만은 배지·메뉴·채택 모두 X. 완료일부터1년, 만료면 기관 접근·과거글 배지도 제거; 일반 계정 유지. 실제 민원 담당자 대상이며 단순 소속자에게 기관 권한을 부여하지 않는다. 운영심사/반려관리/보완메일 제외. J02/J05의 최종제품 1주일 심사 문구는 MVP에서 실제 승인 일정 보장으로 표시하지 않는다. 접수 저장 및 시연 완료 상태를 구분해 설명한다.

## 43. Institution Officer

K01 전체지역/지역필터·전체공개안건/본인기관채택 목록·반응수 내림차순. GET /officer/agendas?regionId&scope=ALL|ADOPTED&cursor&size. 담당 지역 밖도 원문/공개반응/주민 댓글·답글 **내용** 열람, 숫자만 보여주는 상세는 미완료. 반응 동률 보조정렬 미정. 유효 기관 아니면 메뉴/Route 접근 차단하고 인증상태를 갱신한다.

## 44. Adoption

채택은 Institution↔LocalAgenda 관계다. 담당지역·공개안건·유효기관 capability일 때만 채택, canCancelAdoption에서 본인기관 취소. POST /posts/{id}/adoptions body없음→id/institutionName/adoptedAt; DELETE /posts/{id}/adoptions/{adoptionId}→204. 권한/기관은 클라이언트 기관명 문자열로 판별하지 않는다.

서버 성공 후 detail.adoptions/기관 본인관계/ADOPTED 목록 갱신. 여러 기관이 각각 채택, 취소는 해당관계만 제거하며 타기관 표시 유지. 공개는 기관명/채택시각만. 담당자이름/전화/업무이메일/증빙 비공개. 채택/취소 pending 중 중복 차단, 실패 원관계 유지·최신조회. PostStatus·검토중/처리중/완료·행정연동·알림을 만들지 않는다.

## 45. AI Summary

공개 지역 안건만. SummaryPanel은 AI 요약·원문·출처(원제목/작성자/갱신시각)·원문확인으로 구성. 3문장 자연스러운 한 문단, 원문 외 사실 없음. GET /posts/{id}/summary → status/summary/source/generatedAt/fallbackToSource.

PENDING 로딩 안내 + 원문, SUCCEEDED 문단, FAILED/SOURCE_TOO_SHORT 안내 + 원문 우선. 네트워크 5xx에도 이미 읽은 원문 유지. 활동/투표에 호출하지 않는다. source.originalPath는 게스트에서 공유컨텍스트를 유지하는 같은 상세로 연결한다. source.updatedAt와 현재 원문 버전 차이를 확인하여 구요약을 최신으로 표시하지 않는다. 생성착수/재생성/재시도/폴링/짧은원문 기준은 미정, 임의 새 job API나 생성버튼/추천을 추가하지 않는다.

## 46. Loading / Empty / Error

| 영역 | Loading | Empty | Error / Retry |
| --- | --- | --- | --- |
| Home/Board/개인/기관목록 | 최초 skeleton, 추가페이지는 끝 pending | 정상0개와 CTA | 첫오류 안내·Retry; 후속오류 기존목록 유지 |
| Map | 지도/대표 로딩 분리 | 동 유지+대표없음 | API/렌더실패 Notice·Retry |
| Detail | 본문/댓글 분리 | 댓글0은 작성 가능조건 유지 | 삭제404는 콘텐츠 제거; 통신실패와 분리 |
| Form/인증 | 필드/버튼 pending | 기본폼 | details를 field에, 공통은 Notice; 입력/파일 보존 |
| Vote | submitting | 0표 결과 | 종료/확인/권한 사유별 처리 |
| AI | PENDING | 원문부족은 SOURCE_TOO_SHORT | failed/통신실패도 원문 유지 |

Notice는 오류 상태를 단순 빈 상태로 위장하지 않는다. 모든 Retry는 같은 조건을 유지하며 새로운 저장 요청으로 자동 전환하지 않는다.

## 47. State Preservation

작성/수정 실패→draft·정상파일 유지, 사진선택 취소→기존 큐 유지, 댓글 실패→내용/대상 유지, 가입 실패→필요단계·returnTo 유지, 인증실패→정보/첨부 유지. 상세왕복→필터/정렬/scroll 유지. 로그인복귀→상세만, action 자동실행 없음. 메모리 큐의 Route 왕복 유지와 새로고침/브라우저 종료의 영속 저장을 구분한다. 임시저장 서버/기기 자동저장 기능은 제외한다.

## 48. Optimistic Update / Rollback

반응·평가·북마크는 선택적 권장안. 적용 시 현재 cache snapshot→UI 임시변경→API→성공 응답 정본교체→실패 snapshot rollback→오류 안내/재조회. 요청 key별 pending·세션/postId 변경 시 rollback 대상을 검증한다. 여러 유형 반응 응답의 오래된 snapshot이 새 성공결과를 지우지 않게 직렬화/최신조회한다. 초기 구현은 서버응답 우선도 허용한다. 투표/삭제/채택/가입/인증/댓글 생성은 서버 성공 전 확정하지 않는다. 활동횟수는 optimistic으로 합산하지 않는다.

## 49. Pagination

불투명 cursor/size, meta.nextCursor/hasNext는 계약안. size 수치 미정. region/type/topic/sort/scope/투표status/인증context를 query key에 포함. 조건변경은 cursor 초기화, 기존 요청취소/늦은응답 무시. 후속 중복클릭 방지, postId/commentId 중복제거, hasNext=false 종료. 부모댓글 단위 페이지이며 replies를 별도 부모로 추가하지 않는다. 답글 전체/후속계약이 미정이면 누락된 답글을 전부라고 표시하지 않는다.

## 50. File / Multipart

게시사진 JPG/PNG·10장·합계10MB, 기관증빙 PDF/JPG/PNG·개별10MB·전체50MB·개수무제한, 이웃/프로필 미정. 각 정책 validator를 분리한다. 파일명/bytes/MIME/preview/local key/status/기존photoId를 큐에서 관리한다. 파일선택 accept는 최종검증 아님. 서버가 형식·용량·소유권 확정검증.

FormData의 boundary는 브라우저가 지정, Content-Type을 JSON이나 수동 multipart 문자열로 덮어쓰지 않는다. posts payload JSON part + 반복 images; profile payload+profileImage; neighbor regionId+evidenceFiles; institution 각정보+employmentCertificates. 실제 parser/part 이름은 계약고정 전 대조한다. preview object URL은 교체/제거/흐름종료 때 해제하고 아직 사용중인 정상파일 URL을 조기 revoke하지 않는다. 사진전용 선업로드 API는 SPEC에 없으므로 새 API를 임의 요구하지 않는다. 증빙 원문은 공개 URL/카드에 노출하지 않는다. 업로드부분실패/응답유실은 성공으로 가정하지 않고 결과확인·입력보존 처리한다.

## 51. 개발 Phase와 의존성

인증 상태·시연 완료 자격을 쓰기 기능보다 먼저 준비한다. 공유/returnTo도 공통 상세와 함께 먼저 연결해 모든 참여 UI가 같은 가드를 재사용한다. 에러/빈 상태는 각 Phase에서 구현하고 마지막에는 통합 검증만 수행한다. 아래 번호는 사용자 예시 순서를 의존성에 맞춰 조정한 권장안이다.

| Phase | 선행 | 산출/완료 기준 |
| --- | --- | --- |
| 0 저장소·계약 확인 | 없음 | 지도원천·하단슬롯 등 미정항목 분리 포함, 아래 세부 Task 검증 |
| 1 토큰·레이아웃 | 0 | 모바일 긴문구·폰트로드·키보드 가림 확인 포함, 아래 세부 Task 검증 |
| 2 공통 UI | 1 | PostCard 사진유무·CommentItem 부모답글·VoteCard 상태 뼈대 포함, 아래 세부 Task 검증 |
| 3 API·상태·권한 기반 | 0,2 | 활동 +1/+0 응답과 무효화 계약 연결 포함, 아래 세부 Task 검증 |
| 4 계정·프로필·기본지역 | 3 | 모든 가입단계 returnTo 보존 포함, 아래 세부 Task 검증 |
| 5 인증 상태·신청 기반 | 4 | 사용자용 승인화면/API 없음 포함, 아래 세부 Task 검증 |
| 6 Home·Board·PostCard | 4,5 | 게스트 접근차단·검색/추천 제거 포함, 아래 세부 Task 검증 |
| 7 공통 상세·게스트·복귀 | 6 | 복귀후 자동행동 없음·회원 미인증 유지 포함, 아래 세부 Task 검증 |
| 8 작성·수정·삭제·사진 | 5,7 | 삭제 공유차단/북마크제거/기록접근불가 확인 포함, 아래 세부 Task 검증 |
| 9 Map | 6,7 | GPS/위치권한 코드 없음 포함, 아래 세부 Task 검증 |
| 10 Reaction·Comments·Evaluation | 7 | 참여/activity/map/기관목록 무효화 포함, 아래 세부 Task 검증 |
| 11 Vote | 8,10 | 종료결과only·타인선택 미노출 포함, 아래 세부 Task 검증 |
| 12 Bookmark·My·Records | 7,10,11 | 활동 +1/+0/재등록 검증·알림의존없음 포함, 아래 세부 Task 검증 |
| 13 Officer·Adoption | 5,10,12 | PostStatus/후속처리/알림 없음 포함, 아래 세부 Task 검증 |
| 14 AI Summary | 7 | 추천API/활동/투표요약 호출 없음 포함, 아래 세부 Task 검증 |
| 15 통합 QA·완료 | 8~14 | 실제 저장소 build/typecheck/관련검증 명령 실행결과 기록 포함, 아래 세부 Task 검증 |

## 52. Phase별 상세 Task

각 Task는 해당 기능의 완료 조건·QA를 만족해야 체크한다. UI만 연결한 것은 FULL 완료가 아니다.

### Phase 0 저장소·계약 확인

선행: 없음

- [ ] 실제 FE/BE 저장소 제공 여부 기록
- [ ] package.json·Next버전·Router·TS·Tailwind·환경변수 이름 확인
- [ ] 기존 app/components/features/types/api 구조 및 재사용 후보 조사
- [ ] 현재 화면·컴포넌트를 이미구현/수정필요/신규/확인불가로 표기
- [ ] 실제 DTO/권한/에러와 SPEC 차이 기록
- [ ] 지도원천·하단슬롯 등 미정항목 분리

### Phase 1 토큰·레이아웃

선행: 0

- [ ] Pretendard 소스/라이선스·로딩 연결
- [ ] semantic 색상/typography/spacing/radius/shadow 토큰 작성
- [ ] primary와 selected 색상 분리
- [ ] 396 기준 responsive shell·safe-area·scroll 적용
- [ ] Header58·Button52·본문15 규격 비교
- [ ] 모바일 긴문구·폰트로드·키보드 가림 확인

### Phase 2 공통 UI

선행: 1

- [ ] Button 주요/보조/disabled/pending
- [ ] Input·TextArea label/error/description
- [ ] Header back/action/main
- [ ] BottomNavigation items 추상화 및 슬롯 미결정 표시
- [ ] Chip 선택/비선택
- [ ] Notice·Menu·Attachment·Photo
- [ ] Modal focus/취소/복원
- [ ] EmptyState·BottomSheet 사용처/미정규격 기록
- [ ] PostCard 사진유무·CommentItem 부모답글·VoteCard 상태 뼈대

### Phase 3 API·상태·권한 기반

선행: 0,2

- [ ] ApiResponse/ApiError/페이지 wrapper
- [ ] 204 무본문 처리
- [ ] Bearer/공유컨텍스트 실제계약 대조
- [ ] 서버캐시를 세션/context별 격리
- [ ] GET 취소·늦은응답 방지
- [ ] capabilities 읽기·권한없음 기본처리
- [ ] 401/403/404/409/422/429 안내 연결
- [ ] POST 결과불명 자동중복재시도 방지
- [ ] 활동 +1/+0 응답과 무효화 계약 연결

### Phase 4 계정·프로필·기본지역

선행: 3

- [ ] A01 시작자산 확인·CTA
- [ ] A02 로그인·실패통일·pending
- [ ] A06/A07 이메일발송/확인/재발송 상태
- [ ] 6자리문자열·5분·60초·30분5회·입력5회 오류
- [ ] 이메일변경 proof 초기화
- [ ] 비밀번호/확인·필수/선택약관
- [ ] 닉네임10·소개50·복수속성
- [ ] 활동지역 후보/선택·draft 반영
- [ ] 최종sign-up 요청·세션확정
- [ ] 프로필PATCH 저장/취소/실패
- [ ] 기본지역과 탐색지역 분리
- [ ] 모든 가입단계 returnTo 보존

### Phase 5 인증 상태·신청 기반

선행: 4

- [ ] 이웃 지역선택·증빙큐·제거
- [ ] 이웃 파일규칙 미정 설정 분리
- [ ] 최종제출 POST·접수화면
- [ ] requests/verifiedRegions 상태 재조회
- [ ] 접수계정 참여차단
- [ ] 시연완료계정 users/me/capabilities 갱신
- [ ] 완료지역3 한도 서버검증 확인
- [ ] 기관정보7항목/필수지역
- [ ] 기관증빙 PDF/JPG/PNG·개별10·전체50
- [ ] 증빙목록·제거·정보화면 왕복유지
- [ ] 자료제출→접수; 추가확인화면 없음
- [ ] 유효완료/만료 배지·기관메뉴 제어
- [ ] 사용자용 승인화면/API 없음

### Phase 6 Home·Board·PostCard

선행: 4,5

- [ ] home Route·MainHeader
- [ ] 현재탐색지역과 임시변경
- [ ] home posts/openVotes/boardCounts 연결
- [ ] 지도미리보기 별도 map 조회
- [ ] 게시판 Route·지역·유형Chip·주제Chip
- [ ] ALL query 생략
- [ ] 카드사진 있음/없음
- [ ] initial loading·empty·error·retry
- [ ] cursor 추가조회·실패 기존목록유지
- [ ] 상세이동·origin 보존
- [ ] 복귀 filters/scroll 복원
- [ ] 게스트 접근차단·검색/추천 제거

### Phase 7 공통 상세·게스트·복귀

선행: 6

- [ ] PostDetail 공통정본 렌더
- [ ] activity/vote 확장슬롯
- [ ] 작성자 최신프로필/유효배지
- [ ] 사진없음 영역생략
- [ ] capabilities/myState 반영
- [ ] 초기댓글·후속댓글 중복방지
- [ ] 삭제404 미노출·안내
- [ ] 공유대상 context·범위검증
- [ ] 게스트 BottomNavigation 제거
- [ ] 공유링크발급/복사성공·실패 fallback
- [ ] guest 기존링크복사
- [ ] 로그인유도·내부 returnTo 검증
- [ ] 가입/로그인 성공 원상세복귀
- [ ] 복귀후 자동행동 없음·회원 미인증 유지

### Phase 8 작성·수정·삭제·사진

선행: 5,7

- [ ] 유형선택Route·공통폼
- [ ] 지역안건5입력
- [ ] 활동출처/일정/장소/상태 기본값없음
- [ ] 활동외부링크 선택·문의 별도입력없음
- [ ] 투표질문·선택지2~10·안정key·종료시간
- [ ] 사진 JPG/PNG·10장·합계10MB 검증
- [ ] 사진preview/순서/교체/제거·objectURL 정리
- [ ] multipart payload/images
- [ ] submitting 중복차단·실패 draft/파일 유지
- [ ] 성공상세이동·목록/마이/지도갱신
- [ ] 본인 editDraft·photoOrder 매핑
- [ ] 진행투표 허용4필드·종료편집차단
- [ ] 삭제확인·취소·204처리
- [ ] 삭제 공유차단/북마크제거/기록접근불가 확인

### Phase 9 Map

선행: 6,7

- [ ] 지도Route·렌더 provider 계약확인
- [ ] 기본활동지역 중심
- [ ] 지도 확대/축소·동선택·말풍선
- [ ] dongs 응답연결
- [ ] 지역안건/투표 대표만
- [ ] 반응최대·동률최신 서버값표시
- [ ] 첫사진/사진없음
- [ ] 후보없는동 유지·안내
- [ ] 로딩/API/렌더실패·retry
- [ ] 대표상세이동·중심/zoom 복원
- [ ] GPS/위치권한 코드 없음

### Phase 10 Reaction·Comments·Evaluation

선행: 7

- [ ] ReactionBar 3독립상태·개별취소
- [ ] 응답집계/myReactions·pending
- [ ] CommentSection 조회·댓글0
- [ ] 부모작성·답글대상/context
- [ ] 답글의답글 원부모1단계
- [ ] guest 이름/배지 고정
- [ ] 금칙어/빈내용 서버오류·입력유지
- [ ] 성공후 내용만비우기·댓글수재조회
- [ ] LIKE/DISLIKE 전환·동일취소
- [ ] 부모 LIKES/LATEST 정렬·동률최신
- [ ] 답글like 부모합산금지
- [ ] 부모cursor·답글누락 표시주의
- [ ] 권한오류/삭제경합 처리
- [ ] 참여/activity/map/기관목록 무효화

### Phase 11 Vote

선행: 8,10

- [ ] OPEN/CLOSED 상태분리
- [ ] 서버 myOptionId 저장
- [ ] temporaryOptionId 별도
- [ ] 옵션선택 API없음
- [ ] 선택전 제출비활성
- [ ] 최초표 confirmChange=false
- [ ] 다른기존표 변경Modal
- [ ] Modal취소 기존표복원·요청없음
- [ ] 확인 confirmChange=true
- [ ] 중복제출차단
- [ ] 성공응답 선택/수/율/참여자수 갱신
- [ ] VOTE_ENDED 종료결과
- [ ] VOTE_CHANGE_CONFIRMATION_REQUIRED 재확인
- [ ] 실패 기존표/임시값 보존·상태불명 재조회
- [ ] 최다득표와 본인표 별도강조
- [ ] 종료결과only·타인선택 미노출

### Phase 12 Bookmark·My·Records

선행: 7,10,11

- [ ] 상세북마크 단일버튼
- [ ] 미인증회원 허용·guest 로그인만
- [ ] 저장성공팝업·상세유지
- [ ] 해제/삭제 목록갱신
- [ ] I01 프로필/누적활동 숫자
- [ ] 작성목록type·본인수정/삭제
- [ ] 참여목록postId 중복제거·모든유효표시
- [ ] 북마크/작성/타인행동만 제외
- [ ] 다른참여남은 카드유지
- [ ] 참여투표 ALL/OPEN/CLOSED·실제선택
- [ ] 삭제투표 기록유지·내용미노출
- [ ] 북마크type/topic·카드토글없음
- [ ] 목록loading/empty/error·복귀
- [ ] 활동 +1/+0/재등록 검증·알림의존없음

### Phase 13 Officer·Adoption

선행: 5,10,12

- [ ] 기관유효세션 Route가드
- [ ] 전체공개안건/본인기관채택 scope
- [ ] 지역필터·반응수내림차순
- [ ] 담당밖 안건/주민의견내용 열람
- [ ] 담당지역안건 canAdopt
- [ ] 채택pending·서버관계응답
- [ ] 본인기관 adoptionId 취소
- [ ] 다른기관관계 독립유지
- [ ] 공개기관명/시각만
- [ ] 만료/삭제/403 충돌 재조회
- [ ] 목록/상세/채택기록 동기화
- [ ] PostStatus/후속처리/알림 없음

### Phase 14 AI Summary

선행: 7

- [ ] 공개지역안건만 query
- [ ] AI요약/원문/출처/원문확인
- [ ] PENDING 원문유지
- [ ] SUCCEEDED 3문장한문단
- [ ] FAILED/SOURCE_TOO_SHORT 원문fallback
- [ ] 통신실패 본문유지
- [ ] 원문버전과 구요약 연결확인
- [ ] 게스트 original 이동context 유지
- [ ] 생성/재생성/폴링 미정표기
- [ ] 추천API/활동/투표요약 호출 없음

### Phase 15 통합 QA·완료

선행: 8~14

- [ ] 각기능 loading/empty/error/pending 실제응답 검증
- [ ] 인증접수/완료/만료 및 타지역 권한 시나리오
- [ ] 응답유실·동시투표·삭제경합
- [ ] 새로고침 서버정본·완료상태 유지
- [ ] 파일/입력 실패보존·URL정리
- [ ] 헤더/하단/폰트/사진유무 시각확인
- [ ] 게스트 타postToken/회원권한우회 차단
- [ ] 실 API 연결과 mock 차이 기록
- [ ] NON-MVP UI/API 없음
- [ ] 미정 PO/BE 결정 목록별 완료영향 기록
- [ ] 실제 저장소 build/typecheck/관련검증 명령 실행결과 기록

## 53. 화면별 상세 구현 명세

같은 Route의 필터·권한·완료 변형을 한 구현 단위로 묶는다. 아래 Figma ID는 전체 매핑 중 대표이며 변형은 6절을 모두 따른다. 화면마다 다음 공통 조항도 적용한다.

| 공통 항목 | 의무 |
| --- | --- |
| Loading | 초기 데이터는 skeleton/진행안내, mutation은 해당버튼 pending. 권한확인 중 쓰기 허용 없음 |
| Empty | 조회0은 정상빈상태·CTA, 폼은 Empty 해당없음; 삭제는 Empty로 위장하지 않음 |
| Error | 16/46절 사유별처리; 성공하지 않은 생성/변경을 확정표시하지 않음 |
| 성공처리 | 쓰기는 응답정본으로 교체·관련목록 무효화; 아래 화면별 목적지 적용 |
| 실패처리·입력보존 | 폼/파일/댓글/임시표·필터를 유지, 저장정본은 그대로; 세션/삭제오류는 재조회 |
| returnTo | 공유로그인 상세복귀만, action 자동실행 없음; 일반목록은 origin과 구분 |
| 완료조건 | 정상+권한+로딩+빈/오류+실패보존+실API 저장/조회 검증. 미정값은 확인필요로 유지 |
| QA | 허용/거부계정·네트워크실패·왕복·중복클릭을 각 화면에서 검증 |

아래 표의 Common 참조는 위 공통 의무를 생략하는 뜻이 아니다. 문서의 화면별 항목과 공통 조항을 함께 구현한다.

### WF-A01 시작

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1157:905 WF-A01-시작 (나머지 변형은 6절) |
| 목적 | 브랜드 이미지·슬로건·로그인/가입 CTA를 제공하고 login/signup 이동를 수행 |
| 관련 기능 ID | F-WLXSSC |
| 진입 조건·경로 / Route | 비로그인 시작; `/` |
| 표시 데이터 | 브랜드 이미지·슬로건·로그인/가입 CTA |
| 사용자 행동 | login/signup 이동 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 비로그인 시작 |
| Client State | 없음 |
| 필요한 API | 없음 — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | 없음 |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 이미지 로딩/작은화면 상황도 처리 |
| 성공 처리 | A01 자산·CTA 이동 일치; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | 없음의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 시작 root |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Header,Button |
| 의존 기능 | 토큰/이미지자산 |
| 완료 조건 | A01 자산·CTA 이동 일치; 실API·권한·오류·복귀 검증 |
| QA 항목 | 이미지 로딩/작은화면; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 시작 Inter 예외·자산 추출 |

### WF-A02/G02 로그인

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1157:962 WF-A02-로그인; 1157:1044 WF-G02-로그인-원게시물복귀; 1162:7519 WF-A02-로그인 (나머지 변형은 6절) |
| 목적 | 등록이메일/비밀번호·복귀 안내를 제공하고 이메일/비밀번호 입력→제출를 수행 |
| 관련 기능 ID | F-TSOXGG |
| 진입 조건·경로 / Route | 시작 또는 게스트 회원행동; `/login` |
| 표시 데이터 | 등록이메일/비밀번호·복귀 안내 |
| 사용자 행동 | 이메일/비밀번호 입력→제출 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 시작 또는 게스트 회원행동 |
| Client State | email,password,pending,returnTo |
| 필요한 API | POST /auth/login — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | user,accessToken |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 오류원인 비구분·게스트북마크복귀 상황도 처리 |
| 성공 처리 | 실패 통일·성공 복귀·자동행동 없음; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | email,password,pending,returnTo의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 시작/원 공유상세 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Header,Input,Button,Notice |
| 의존 기능 | 세션/API |
| 완료 조건 | 실패 통일·성공 복귀·자동행동 없음; 실API·권한·오류·복귀 검증 |
| QA 항목 | 오류원인 비구분·게스트북마크복귀; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | Token 저장/TTL |

### WF-A06/A07 가입 계정·인증

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1157:1130 WF-A06-회원가입-계정정보·약관; 1157:1262 WF-A07-회원가입-이메일인증-발송; 1157:1410 WF-A07-회원가입-이메일인증-재발송; 1157:1558 WF-A07-회원가입-이메일인증-완료 (나머지 변형은 6절) |
| 목적 | email/code/timer/인증완료·비밀번호/약관를 제공하고 발송/확인/재발송·다음를 수행 |
| 관련 기능 ID | F-KZRSXU |
| 진입 조건·경로 / Route | 시작/로그인에서 가입; `/signup` |
| 표시 데이터 | email/code/timer/인증완료·비밀번호/약관 |
| 사용자 행동 | 발송/확인/재발송·다음 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 시작/로그인에서 가입 |
| Client State | SignupDraft,proof,pending별 |
| 필요한 API | POST /auth/email-verifications; POST /auth/email-verifications/confirm — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | expiresAt,resendAvailableAt,verificationToken |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 0시작코드·만료/재발급/발송실패 상황도 처리 |
| 성공 처리 | 5분·60초·한도·필수동의 적용; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | SignupDraft,proof,pending별의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 이전단계/시작 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Input,Button,Notice |
| 의존 기능 | 공통오류/가입draft |
| 완료 조건 | 5분·60초·한도·필수동의 적용; 실API·권한·오류·복귀 검증 |
| QA 항목 | 0시작코드·만료/재발급/발송실패; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | proof TTL |

### WF-A08/A09 가입 프로필·지역

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1157:1703 WF-A08-회원가입-프로필설정; 1157:1801 WF-A08-회원가입-프로필설정-활동지역설정완료; 1157:1910 WF-A09-회원가입-활동지역설정 (나머지 변형은 6절) |
| 목적 | nickname/bio/attributes/선택지역를 제공하고 프로필입력·지역선택·최종가입를 수행 |
| 관련 기능 ID | F-RBVFZX/F-QQKYLC/F-KZRSXU |
| 진입 조건·경로 / Route | 이메일인증 및 약관단계 완료; `/signup` |
| 표시 데이터 | nickname/bio/attributes/선택지역 |
| 사용자 행동 | 프로필입력·지역선택·최종가입 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 이메일인증 및 약관단계 완료 |
| Client State | SignupDraft,selectedRegionId,files |
| 필요한 API | GET /regions; POST /auth/sign-up — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | region.id/name,user,accessToken |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 닉네임중복·미선택·파일실패·지역권한생기지않음 상황도 처리 |
| 성공 처리 | 마지막가입성공 후 메인/원상세; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | SignupDraft,selectedRegionId,files의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 이전가입단계·draft유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Input,Menu,Chip,Button |
| 의존 기능 | 이메일proof·지역후보 |
| 완료 조건 | 마지막가입성공 후 메인/원상세; 실API·권한·오류·복귀 검증 |
| QA 항목 | 닉네임중복·미선택·파일실패·지역권한생기지않음; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 프로필사진제한·가입후 이웃CTA |

### WF-B01/B03 메인

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1159:1694 WF-B01-메인; 1159:1887 WF-B03-메인-하계2동-이웃미인증 (나머지 변형은 6절) |
| 목적 | 지역·지도미리보기·진행투표·게시물를 제공하고 지역변경/게시판/지도/상세를 수행 |
| 관련 기능 ID | F-UPRLMN |
| 진입 조건·경로 / Route | 로그인 완료; `/home` |
| 표시 데이터 | 지역·지도미리보기·진행투표·게시물 |
| 사용자 행동 | 지역변경/게시판/지도/상세 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 로그인 완료 |
| Client State | currentExploreRegion,home/map query |
| 필요한 API | GET /home; GET /map/dongs — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | region,posts,openVotes,boardCounts,dongs |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 타지역미인증·한영역실패 상황도 처리 |
| 성공 처리 | 빈 섹션CTA·기본지역불변; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | currentExploreRegion,home/map query의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | root |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Header/Main,PostCard,VoteCard,EmptyState,BottomNavigation |
| 의존 기능 | profile/지역/공통카드 |
| 완료 조건 | 빈 섹션CTA·기본지역불변; 실API·권한·오류·복귀 검증 |
| QA 항목 | 타지역미인증·한영역실패; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 노출건수·하단슬롯 |

### WF-B02/B04 탐색지역

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1159:2083 WF-B02-탐색지역선택; 1162:4296 WF-B04-탐색지역선택-하계2동 (나머지 변형은 6절) |
| 목적 | 지역후보·현재탐색선택를 제공하고 선택→임시탐색변경를 수행 |
| 관련 기능 ID | F-UPRLMN |
| 진입 조건·경로 / Route | 메인/게시판 지역CTA; `/explore/region` |
| 표시 데이터 | 지역후보·현재탐색선택 |
| 사용자 행동 | 선택→임시탐색변경 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 메인/게시판 지역CTA |
| Client State | selectedExploreRegion |
| 필요한 API | GET /regions — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | id,name |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 취소·왕복·지연응답 상황도 처리 |
| 성공 처리 | profile PATCH 없이 홈/목록전환; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | selectedExploreRegion의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 호출메인/목록 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Header,Menu,Button |
| 의존 기능 | 지역후보 |
| 완료 조건 | profile PATCH 없이 홈/목록전환; 실API·권한·오류·복귀 검증 |
| QA 항목 | 취소·왕복·지연응답; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 시연지역목록 |

### WF-C01/B05 통합게시판

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1159:2244 WF-C01-통합게시판; 1159:2497 WF-C01-새추천성공; 1162:4417 WF-B05-통합게시판-하계2동-열람전용 (나머지 변형은 6절) |
| 목적 | 지역/type/topic/카드/페이지를 제공하고 필터→목록→상세를 수행 |
| 관련 기능 ID | F-EAJPVC |
| 진입 조건·경로 / Route | 회원 메인CTA; `/board` |
| 표시 데이터 | 지역/type/topic/카드/페이지 |
| 사용자 행동 | 필터→목록→상세 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 회원 메인CTA |
| Client State | URL filters,cursor,scroll |
| 필요한 API | GET /posts — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | PostCard[],meta |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. ALL생략·필터변경cursor·사진없음 상황도 처리 |
| 성공 처리 | 3유형·7주제·빈/후속오류; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | URL filters,cursor,scroll의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | home·목록상태 유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Header,Chip,PostCard,EmptyState |
| 의존 기능 | 탐색지역/상세 |
| 완료 조건 | 3유형·7주제·빈/후속오류; 실API·권한·오류·복귀 검증 |
| QA 항목 | ALL생략·필터변경cursor·사진없음; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | size·기본정렬 |

### WF-D01/D03/B08 지도

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1159:3319 WF-D01-전체지도; 1159:3480 WF-D03-지도-로딩실패; 1162:4954 WF-B08-이슈지도-하계2동 (나머지 변형은 6절) |
| 목적 | 동·대표제목/첫사진를 제공하고 확대축소/동선택/대표상세를 수행 |
| 관련 기능 ID | F-QIGKAK |
| 진입 조건·경로 / Route | 회원 메인/하단지도; `/map` |
| 표시 데이터 | 동·대표제목/첫사진 |
| 사용자 행동 | 확대축소/동선택/대표상세 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 회원 메인/하단지도 |
| Client State | center,zoom,selectedDong |
| 필요한 API | GET /map/dongs — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | centerRegion,dongs,representativePost |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 동률/반응갱신·MapError 상황도 처리 |
| 성공 처리 | 활동제외·후보없는동 유지; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | center,zoom,selectedDong의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 출발메인·지도중심 복원 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Header,PostCard,Notice |
| 의존 기능 | 지역원천/지도renderer |
| 완료 조건 | 활동제외·후보없는동 유지; 실API·권한·오류·복귀 검증 |
| QA 항목 | 동률/반응갱신·MapError; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | provider/좌표/경계 |

### WF-E01 유형선택

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1159:3606 WF-E01-글쓰기-유형선택 (나머지 변형은 6절) |
| 목적 | 3유형·지역참여 안내를 제공하고 유형선택→공통폼를 수행 |
| 관련 기능 ID | F-FTLHCX |
| 진입 조건·경로 / Route | 로그인 글쓰기; `/posts/new` |
| 표시 데이터 | 3유형·지역참여 안내 |
| 사용자 행동 | 유형선택→공통폼 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 로그인 글쓰기 |
| Client State | postType,작성흐름origin |
| 필요한 API | GET /users/me — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | neighborVerifiedRegions |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 미인증지역 제출금지 상황도 처리 |
| 성공 처리 | 3유형 공통폼 진입; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | postType,작성흐름origin의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 출발화면 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Header,Menu,Notice |
| 의존 기능 | 세션/완료지역 |
| 완료 조건 | 3유형 공통폼 진입; 실API·권한·오류·복귀 검증 |
| QA 항목 | 미인증지역 제출금지; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 없음 |

### WF-E02/E03/E04 작성

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1159:3688 WF-E02-글쓰기-지역안건; 1159:3823 WF-E03-글쓰기-지역활동정보; 1159:4002 WF-E04-글쓰기-투표 (나머지 변형은 6절) |
| 목적 | 공통5필드+유형별확장를 제공하고 입력/사진선택/게시를 수행 |
| 관련 기능 ID | F-FTLHCX/F-GSMCLD |
| 진입 조건·경로 / Route | 유형선택·해당지역완료; `/posts/new` |
| 표시 데이터 | 공통5필드+유형별확장 |
| 사용자 행동 | 입력/사진선택/게시 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 유형선택·해당지역완료 |
| Client State | PostFormDraft,PhotoQueue,pending |
| 필요한 API | POST /posts — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | 최신PostDetail.id/확장 |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 활동상태무기본·투표2/10·10MB 상황도 처리 |
| 성공 처리 | 성공 상세·실패draft유지; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | PostFormDraft,PhotoQueue,pending의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 유형선택·draft유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Input,TextArea,Chip,Photo,Button |
| 의존 기능 | 인증지역/공통폼 |
| 완료 조건 | 성공 상세·실패draft유지; 실API·권한·오류·복귀 검증 |
| QA 항목 | 활동상태무기본·투표2/10·10MB; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 길이·schedule·중복옵션·과거시각 |

### WF-E05 사진

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1159:4185 WF-E05-사진-첨부미리보기 (나머지 변형은 6절) |
| 목적 | 파일목록·순서·preview·용량를 제공하고 선택/교체/제거/돌아가기를 수행 |
| 관련 기능 ID | F-GSMCLD |
| 진입 조건·경로 / Route | 작성/수정 사진추가; `작성 Route overlay` |
| 표시 데이터 | 파일목록·순서·preview·용량 |
| 사용자 행동 | 선택/교체/제거/돌아가기 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 작성/수정 사진추가 |
| Client State | PhotoQueue·objectUrls |
| 필요한 API | 저장 시 POST/PATCH /posts — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | images/photoOrder 대응 |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 취소·중간실패·기존+신규 상황도 처리 |
| 성공 처리 | 선택만으로 게시없음·최종한도; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | PhotoQueue·objectUrls의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 작성폼·정상사진 유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Photo,Attachment,Button |
| 의존 기능 | 작성메모리큐 |
| 완료 조건 | 선택만으로 게시없음·최종한도; 실API·권한·오류·복귀 검증 |
| QA 항목 | 취소·중간실패·기존+신규; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | MB bytes |

### WF-E06/E09 수정·E08 삭제

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1159:4271 WF-E06-진행중투표-수정; 1162:7417 WF-E08-본인투표-삭제확인; 1162:7932 WF-E09-내지역안건-수정 (나머지 변형은 6절) |
| 목적 | 서버원본·editDraft·삭제안내를 제공하고 수정저장/삭제확인/취소를 수행 |
| 관련 기능 ID | F-FTLHCX |
| 진입 조건·경로 / Route | canEdit/canDelete·본인·지역자격; `/posts/[id]/edit; 상세 Modal` |
| 표시 데이터 | 서버원본·editDraft·삭제안내 |
| 사용자 행동 | 수정저장/삭제확인/취소 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; canEdit/canDelete·본인·지역자격 |
| Client State | editDraft,photoOrder,pending |
| 필요한 API | GET/PATCH/DELETE /posts/{id} — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | 최신상세 또는204 |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 변경중종료·삭제북마크제거 상황도 처리 |
| 성공 처리 | 진행표4항목/종료차단; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | editDraft,photoOrder,pending의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 원상세·삭제성공 origin목록 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Input,Photo,Modal,Button |
| 의존 기능 | 상세·사진/투표상태 |
| 완료 조건 | 진행표4항목/종료차단; 실API·권한·오류·복귀 검증 |
| QA 항목 | 변경중종료·삭제북마크제거; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 안건/활동수정허용목록 |

### WF-F01~F25 공통상세

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:2463 WF-F01-지역안건상세-사진있음; 1160:2636 WF-F02-지역안건상세-사진없음; 1160:2806 WF-F03-지역활동정보상세; 1160:2997 WF-F04-투표상세-진행중 (나머지 변형은 6절) |
| 목적 | 본문/사진/유형확장/댓글/집계/권한를 제공하고 참여/저장/공유/본인메뉴를 수행 |
| 관련 기능 ID | F-PUDHYO/F-UCDVNA/F-FCPVIS |
| 진입 조건·경로 / Route | 회원 목록/지도/개인기록; `/posts/[id]` |
| 표시 데이터 | 본문/사진/유형확장/댓글/집계/권한 |
| 사용자 행동 | 참여/저장/공유/본인메뉴 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 회원 목록/지도/개인기록 |
| Client State | detail query,sort,temporaryOption,overlays |
| 필요한 API | GET /posts/{id}; comments/participation각 API — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | myState,capabilities,activity,vote,adoptions |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 적은득표 본인선택·활동종료/문의null 상황도 처리 |
| 성공 처리 | 유형/사진/회원자격별 UI·삭제미노출; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | detail query,sort,temporaryOption,overlays의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | origin목록/지도; 없음home |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | PostDetail,CommentSection,VoteCard |
| 의존 기능 | 공통Post·권한 |
| 완료 조건 | 유형/사진/회원자격별 UI·삭제미노출; 실API·권한·오류·복귀 검증 |
| QA 항목 | 적은득표 본인선택·활동종료/문의null; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 초기댓글·실제권한DTO |

### WF-G01/G06~G09 공유/복귀

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:3650 WF-G01-공유-게스트상세; 1160:3811 WF-G05-로그인복귀-미인증회원상세; 1160:7599 WF-G06-게스트-AI요약; 1160:7644 WF-G07-게스트-답글작성 (나머지 변형은 6절) |
| 목적 | 공개내용/결과·게스트명를 제공하고 댓글/답글·AI·로그인안내를 수행 |
| 관련 기능 ID | F-OWFYWE/F-EDNVWZ/F-TSOXGG |
| 진입 조건·경로 / Route | 유효 공유링크/로그인복귀; `/shared/posts/[id]` |
| 표시 데이터 | 공개내용/결과·게스트명 |
| 사용자 행동 | 댓글/답글·AI·로그인안내 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 유효 공유링크/로그인복귀 |
| Client State | shareContext,content,replyTarget,returnTo |
| 필요한 API | GET detail/comments/summary; POST comments/replies — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | 게스트DTO·회원복귀 최신capabilities |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 다른post token·자동북마크금지 상황도 처리 |
| 성공 처리 | 일반nav없음·회원행동안내; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | shareContext,content,replyTarget,returnTo의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 공유상세; 일반탐색 없음 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | PostDetail,Notice,Modal |
| 의존 기능 | 서버공유범위검증 |
| 완료 조건 | 일반nav없음·회원행동안내; 실API·권한·오류·복귀 검증 |
| QA 항목 | 다른post token·자동북마크금지; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | share TTL/전달방식 |

### WF-F07 북마크·G04 공유

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:3177 WF-F07-북마크-저장성공; 1160:3759 WF-G04-공유-링크복사 (나머지 변형은 6절) |
| 목적 | 저장/복사성공안내를 제공하고 저장/해제/복사를 수행 |
| 관련 기능 ID | F-FYQJPT/F-OWFYWE |
| 진입 조건·경로 / Route | 공개상세 버튼; `상세 overlay` |
| 표시 데이터 | 저장/복사성공안내 |
| 사용자 행동 | 저장/해제/복사 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 공개상세 버튼 |
| Client State | pending,notice |
| 필요한 API | PUT/DELETE bookmark; GET share-link — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | isBookmarked/shareUrl |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. guest로그인만·미인증북마크가능 상황도 처리 |
| 성공 처리 | 응답/복사성공 후만 성공안내; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | pending,notice의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 현재상세 유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Button,Notice,Modal |
| 의존 기능 | 세션/상세 |
| 완료 조건 | 응답/복사성공 후만 성공안내; 실API·권한·오류·복귀 검증 |
| QA 항목 | guest로그인만·미인증북마크가능; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | clipboard fallback |

### WF-I01/I06/I07 마이·프로필

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:4471 WF-I01-마이페이지; 1160:5372 WF-I06-프로필수정; 1160:5452 WF-I07-기본활동지역; 1164:11917 WF-I07-기본활동지역-지역선택 (나머지 변형은 6절) |
| 목적 | 프로필/기본지역/누적횟수/포함메뉴를 제공하고 편집·저장·지역·개인/인증진입를 수행 |
| 관련 기능 ID | F-WYMXXP/F-RBVFZX/F-QQKYLC |
| 진입 조건·경로 / Route | 본인로그인; `/me; /me/profile; /me/region` |
| 표시 데이터 | 프로필/기본지역/누적횟수/포함메뉴 |
| 사용자 행동 | 편집·저장·지역·개인/인증진입 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 본인로그인 |
| Client State | profileDraft,selectedRegion,activity query |
| 필요한 API | GET/PATCH /users/me; GET regions/activity — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | profile,counts,totalCount,기관/이웃상태 |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 저장실패/취소·기관만료 상황도 처리 |
| 성공 처리 | 번호는누적행동·제외메뉴없음; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | profileDraft,selectedRegion,activity query의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | me/home; 저장전값 유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Header,Input,TextArea,Menu |
| 의존 기능 | 프로필/활동정본 |
| 완료 조건 | 번호는누적행동·제외메뉴없음; 실API·권한·오류·복귀 검증 |
| QA 항목 | 저장실패/취소·기관만료; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 사진제한 |

### WF-I02 북마크목록

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:4653 WF-I02-북마크목록; 1164:6588 WF-I02-북마크-유형-지역안건; 1164:6742 WF-I02-북마크-유형-지역활동정보; 1164:6896 WF-I02-북마크-유형-투표 (나머지 변형은 6절) |
| 목적 | 저장PostCard·type/topic를 제공하고 필터/상세를 수행 |
| 관련 기능 ID | F-FYQJPT |
| 진입 조건·경로 / Route | 본인로그인; `/me/bookmarks` |
| 표시 데이터 | 저장PostCard·type/topic |
| 사용자 행동 | 필터/상세 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 본인로그인 |
| Client State | type,topic,cursor,scroll |
| 필요한 API | GET /users/me/bookmarks — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | PostCard[],meta |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 사진없음/비어있음 상황도 처리 |
| 성공 처리 | 삭제원본목록제거; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | type,topic,cursor,scroll의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | me; 상세왕복상태유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Chip,PostCard,EmptyState |
| 의존 기능 | 북마크정본 |
| 완료 조건 | 삭제원본목록제거; 실API·권한·오류·복귀 검증 |
| QA 항목 | 사진없음/비어있음; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | size |

### WF-I03 작성목록

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:4807 WF-I03-내가만든게시물; 1164:8407 WF-I03-내가만든게시물-지역안건; 1164:8708 WF-I03-내가만든게시물-지역활동정보; 1164:8831 WF-I03-내가만든게시물-투표 (나머지 변형은 6절) |
| 목적 | 작성PostCard·유형·편집권한를 제공하고 type/상세/수정삭제를 수행 |
| 관련 기능 ID | F-SSHXAA |
| 진입 조건·경로 / Route | 본인로그인; `/me/posts` |
| 표시 데이터 | 작성PostCard·유형·편집권한 |
| 사용자 행동 | type/상세/수정삭제 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 본인로그인 |
| Client State | type,cursor,scroll |
| 필요한 API | GET /users/me/posts — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | cards/capabilities |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 타인행동 작성수 영향없음 상황도 처리 |
| 성공 처리 | 작성원본만·종료표메뉴없음; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | type,cursor,scroll의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | me; type유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Chip,PostCard |
| 의존 기능 | 공통Post/수정권한 |
| 완료 조건 | 작성원본만·종료표메뉴없음; 실API·권한·오류·복귀 검증 |
| QA 항목 | 타인행동 작성수 영향없음; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 기본정렬 |

### WF-I04 참여목록

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:4930 WF-I04-반응한게시물; 1164:9468 WF-I04-반응한게시물-지역안건; 1164:9657 WF-I04-반응한게시물-지역활동정보; 1164:10057 WF-I04-반응한게시물-투표 (나머지 변형은 6절) |
| 목적 | 게시물별 모든 현재참여표시를 제공하고 type/상세를 수행 |
| 관련 기능 ID | F-NZTUYE |
| 진입 조건·경로 / Route | 본인로그인; `/me/participations` |
| 표시 데이터 | 게시물별 모든 현재참여표시 |
| 사용자 행동 | type/상세 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 본인로그인 |
| Client State | type,cursor,scroll |
| 필요한 API | GET /users/me/participations — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | myParticipation/cards/meta |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 반응취소후댓글남음·게스트이관없음 상황도 처리 |
| 성공 처리 | 한post 한카드·북마크만제외; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | type,cursor,scroll의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | me; 상태유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Chip,PostCard,EmptyState |
| 의존 기능 | 참여정본 |
| 완료 조건 | 한post 한카드·북마크만제외; 실API·권한·오류·복귀 검증 |
| QA 항목 | 반응취소후댓글남음·게스트이관없음; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 행동별필터 추가금지 |

### WF-I05 참여투표

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:5075 WF-I05-참여한투표; 1164:11290 WF-I05-참여한투표-진행중; 1164:11601 WF-I05-참여한투표-종료 (나머지 변형은 6절) |
| 목적 | 진행/종료·실제내표·삭제접근불가를 제공하고 상태필터/상세를 수행 |
| 관련 기능 ID | F-QPGNCF |
| 진입 조건·경로 / Route | 본인실제투표; `/me/votes` |
| 표시 데이터 | 진행/종료·실제내표·삭제접근불가 |
| 사용자 행동 | 상태필터/상세 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 본인실제투표 |
| Client State | status,cursor,scroll |
| 필요한 API | GET /users/me/votes — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | availability,post,vote,participatedAt |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 최대득표와내표차이·0표 상황도 처리 |
| 성공 처리 | 삭제기록존재·내용없음; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | status,cursor,scroll의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | me; 필터유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | VoteCard,Notice,EmptyState |
| 의존 기능 | 표정본/삭제정책 |
| 완료 조건 | 삭제기록존재·내용없음; 실API·권한·오류·복귀 검증 |
| QA 항목 | 최대득표와내표차이·0표; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 삭제기록상태필터 |

### WF-J01/J02 이웃인증

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | DEMO / LIMITED BACKEND; 1160:5577 WF-J01-이웃인증-제출; 1160:5631 WF-J02-이웃인증-접수 (나머지 변형은 6절) |
| 목적 | 지역/증빙/접수·완료목록를 제공하고 선택/제거/최종제출/상태조회를 수행 |
| 관련 기능 ID | F-ATWJDJ |
| 진입 조건·경로 / Route | 회원·마이/참여안내; `/verifications/neighbor` |
| 표시 데이터 | 지역/증빙/접수·완료목록 |
| 사용자 행동 | 선택/제거/최종제출/상태조회 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 회원·마이/참여안내 |
| Client State | region,evidenceQueue,pending |
| 필요한 API | GET/POST /users/me/neighbor-verifications — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | requests,verifiedRegions,status |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 왕복/재조회·4번째완료거부 상황도 처리 |
| 성공 처리 | 접수권한없음·완료지역만참여; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | region,evidenceQueue,pending의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 진입점; 실패첨부보존 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Attachment,Menu,Notice |
| 의존 기능 | 세션/지역/시연상태 |
| 완료 조건 | 접수권한없음·완료지역만참여; 실API·권한·오류·복귀 검증 |
| QA 항목 | 왕복/재조회·4번째완료거부; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 증빙종류/파일제한 |

### WF-J03/J04/J05 기관인증

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | DEMO / LIMITED BACKEND; 1160:5676 WF-J03-기관인증-정보; 1160:5900 WF-J04-기관인증-증빙; 1160:5953 WF-J05-기관인증-접수 (나머지 변형은 6절) |
| 목적 | 기관7항목/지역/증빙/접수상태를 제공하고 정보→증빙→자료제출를 수행 |
| 관련 기능 ID | F-OPNIXL/F-MUBDJD |
| 진입 조건·경로 / Route | 회원·동일신청흐름; `/verifications/institution` |
| 표시 데이터 | 기관7항목/지역/증빙/접수상태 |
| 사용자 행동 | 정보→증빙→자료제출 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 회원·동일신청흐름 |
| Client State | institutionDraft,files,step |
| 필요한 API | GET/POST /institution-verifications — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | status,files,responsibleRegion,validUntil,isActive |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 개별10/전체50·만료·무제한개수 상황도 처리 |
| 성공 처리 | 접수는배지없음·유효완료만권한; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | institutionDraft,files,step의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 정보단계/진입점·파일유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Input,Attachment,Button,Notice |
| 의존 기능 | 지역/시연유효완료 |
| 완료 조건 | 접수는배지없음·유효완료만권한; 실API·권한·오류·복귀 검증 |
| QA 항목 | 개별10/전체50·만료·무제한개수; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | MB bytes/입력길이 |

### WF-K01 기관목록

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:5995 WF-K01-담당자-전체안건; 1164:6182 WF-K01-담당자-채택게시물; 1164:6520 WF-K01-담당자-월계1동 (나머지 변형은 6절) |
| 목적 | 전체/채택·지역·반응수를 제공하고 필터/안건상세를 수행 |
| 관련 기능 ID | F-CNNPYL |
| 진입 조건·경로 / Route | 유효기관인증; `/officer/agendas` |
| 표시 데이터 | 전체/채택·지역·반응수 |
| 사용자 행동 | 필터/안건상세 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 유효기관인증 |
| Client State | scope,region,cursor,scroll |
| 필요한 API | GET /officer/agendas — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | cards,myInstitutionAdoption,capabilities |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 담당밖열람·만료거부 상황도 처리 |
| 성공 처리 | 전체지역·반응내림차순; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | scope,region,cursor,scroll의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | me; origin유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Header,Chip,PostCard |
| 의존 기능 | 기관상태/공개안건 |
| 완료 조건 | 전체지역·반응내림차순; 실API·권한·오류·복귀 검증 |
| QA 항목 | 담당밖열람·만료거부; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 동률정렬/기관ID |

### WF-K02/K03/K04 채택검토

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:6063 WF-K02-담당지역-채택상세; 1160:6134 WF-K03-타지역-열람전용; 1160:6189 WF-K04-지역안건-채택기록 (나머지 변형은 6절) |
| 목적 | 원문·주민의견·기관명/채택시각를 제공하고 담당내 채택/본인기관취소·담당밖열람를 수행 |
| 관련 기능 ID | F-TUGMEP/F-PUDHYO |
| 진입 조건·경로 / Route | 유효기관·공개안건; `/officer/agendas/[id]` |
| 표시 데이터 | 원문·주민의견·기관명/채택시각 |
| 사용자 행동 | 담당내 채택/본인기관취소·담당밖열람 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 유효기관·공개안건 |
| Client State | detail,relation,pending |
| 필요한 API | GET detail/comments; POST/DELETE adoptions — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | adoptions,myInstitutionAdoption,capabilities |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 타기관취소불가·한기관취소독립 상황도 처리 |
| 성공 처리 | 의견내용·독립관계·담당밖불가; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | detail,relation,pending의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 기관목록필터복원 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | PostDetail,CommentSection,Button,Notice |
| 의존 기능 | 기관/공통상세/댓글 |
| 완료 조건 | 의견내용·독립관계·담당밖불가; 실API·권한·오류·복귀 검증 |
| QA 항목 | 타기관취소불가·한기관취소독립; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 기관관계DTO/취소재시도 |

### WF-B/F/G AI요약

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:7599 WF-G06-게스트-AI요약; 1162:5417 WF-B10-AI요약-하계2동안건; 1162:6526 WF-F21-AI요약-월계2동-지도안건; 1162:6742 WF-F22-AI요약-월계3동-지도안건 (나머지 변형은 6절) |
| 목적 | 요약/원문/출처를 제공하고 열람·원문확인를 수행 |
| 관련 기능 ID | F-WSCKDN |
| 진입 조건·경로 / Route | 공개안건·회원/유효공유; `안건상세 overlay` |
| 표시 데이터 | 요약/원문/출처 |
| 사용자 행동 | 열람·원문확인 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 공개안건·회원/유효공유 |
| Client State | summaryStatus/query |
| 필요한 API | GET /posts/{id}/summary — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | status,summary,source,fallbackToSource |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. PENDING/FAILED/짧음·원문버전 상황도 처리 |
| 성공 처리 | 3문장문단·실패원문우선; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | summaryStatus/query의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | 같은상세·공유context유지 |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Notice,Button,SummaryPanel |
| 의존 기능 | 공개안건 |
| 완료 조건 | 3문장문단·실패원문우선; 실API·권한·오류·복귀 검증 |
| QA 항목 | PENDING/FAILED/짧음·원문버전; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 생성/재생성/임계값 |

### WF-H03/F20/B09 삭제·권한안내

| 항목 | 화면별 명세 |
| --- | --- |
| MVP 분류 / Figma | FULL; 1160:4427 WF-H03-삭제된게시물안내; 1162:5101 WF-B09-하계2동-지역참여제한안내; 1162:5991 WF-F20-탐색지역-참여제한안내 (나머지 변형은 6절) |
| 목적 | 안내만; 삭제본문없음를 제공하고 닫기/목록/인증진입를 수행 |
| 관련 기능 ID | F-PUDHYO |
| 진입 조건·경로 / Route | 없는원본/부족한권한; `상세 Error/Notice` |
| 표시 데이터 | 안내만; 삭제본문없음 |
| 사용자 행동 | 닫기/목록/인증진입 |
| 권한 | 14절 capabilities·대상지역/공유범위 적용; 없는원본/부족한권한 |
| Client State | error/context |
| 필요한 API | 최신GET detail/users/me — 계약안 / 실제 구현 확인 필요 |
| Response 사용 필드 | HTTP404/403/capabilities |
| Loading / Empty / Error | 53절 공통조항 + 46절 해당영역. 삭제링크·잘못된공유·접수계정 상황도 처리 |
| 성공 처리 | 삭제캐시부활없음·권한안내구분; 관련 원본/목록 캐시 갱신 |
| 실패 처리·입력/상태 보존 | error/context의 사용자 입력·목록조건 유지; 정본 변경하지 않고 오류 안내/재조회 |
| 뒤로가기 | origin 또는 home |
| returnTo | 34절; 공유기원 인증에서 원 상세 보존, 복귀만 하고 자동행동 없음 |
| 공통 컴포넌트 | Notice,Modal,Button |
| 의존 기능 | 공통오류 |
| 완료 조건 | 삭제캐시부활없음·권한안내구분; 실API·권한·오류·복귀 검증 |
| QA 항목 | 삭제링크·잘못된공유·접수계정; 실패·중복클릭·back 후 동일상태 |
| 확인 필요 | 삭제물물리보존 |

## 54. API 연결 매핑표 (연동 부록)

모든 경로는 API SPEC 기준 계약안 / 실제 구현 확인 필요. Base `/api/v1`, Request·Response 이름/nullable·상태/코드도 설계 제안이다. 아래 표는 화면부터 개발한 후 FE/BE 대조용으로 쓴다. 서버가 제공되지 않았으므로 실행 검증 결과가 아니다.

| 기능/화면 | Method | Endpoint | 주요 Request | 주요 Response | 주요 Error |
| --- | --- | --- | --- | --- | --- |
| 연결 확인/Phase0 | GET | /health | 없음 | data.status | 503 |
| 가입 A06/07 | POST | /auth/email-verifications | email,purpose | expiresAt,resendAvailableAt | EMAIL_DELIVERY_FAILED,EMAIL_VERIFICATION_LIMIT_EXCEEDED |
| 인증확인 A07 | POST | /auth/email-verifications/confirm | email,purpose,code | verificationToken,verifiedAt | EMAIL_VERIFICATION_INVALID,EMAIL_VERIFICATION_EXPIRED |
| 가입 A08/09 | POST | /auth/sign-up | email,emailVerificationToken,password,passwordConfirmation,agreements,profile | user,accessToken | EMAIL_ALREADY_IN_USE,NICKNAME_ALREADY_IN_USE,REQUIRED_AGREEMENT_MISSING |
| 로그인 A02/G02 | POST | /auth/login | email,password | user,accessToken | LOGIN_FAILED |
| 마이/권한 I01 | GET | /users/me | 세션 | profile,neighborVerifiedRegions,institutionVerification | UNAUTHORIZED |
| 프로필 I06/07 | PATCH | /users/me | nickname,bio,residentAttributes,activityRegionId; 사진 multipart | 최신 profile | NICKNAME_ALREADY_IN_USE,REGION_NOT_FOUND |
| 지역 A09/B02/I07/J | GET | /regions | cursor,size; 지역명q는 합의필요 | Region[],meta | VALIDATION_ERROR |
| 이웃상태 J01/02 | GET | /users/me/neighbor-verifications | 세션 | requests,verifiedRegions | UNAUTHORIZED |
| 이웃제출 J01 | POST | /users/me/neighbor-verifications | regionId,evidenceFiles | id,region,status,submittedAt | VALIDATION_ERROR,NEIGHBOR_VERIFICATION_LIMIT_EXCEEDED (완료한도 판정) |
| 기관상태 J03~05 | GET | /institution-verifications | 세션 | 본인정보,files,status,validUntil,responsibleRegion | UNAUTHORIZED |
| 기관제출 J04 | POST | /institution-verifications | 기관정보7필드,employmentCertificates | status,files,isActive | MEDIA_LIMIT_EXCEEDED,UNSUPPORTED_MEDIA_TYPE |
| 메인 B01/B03 | GET | /home | regionId | region,posts,openVotes,boardCounts | REGION_NOT_FOUND,UNAUTHORIZED |
| 지도 D01/B08 | GET | /map/dongs | centerRegionId | centerRegion,dongs/representativePost | REGION_NOT_FOUND,503 |
| 게시판 C01/B05 | GET | /posts | regionId,type,topic,cursor,size | PostCard[],meta | VALIDATION_ERROR,UNAUTHORIZED |
| 작성 E02~04 | POST | /posts | payload,images | PostDetail | NEIGHBOR_VERIFICATION_REQUIRED,ACTIVITY_INFO_REQUIRED,VOTE_OPTIONS_INVALID,MEDIA_LIMIT_EXCEEDED |
| 상세 F/G/K | GET | /posts/{postId} | 세션/공유컨텍스트 | PostDetail,myState,capabilities | POST_NOT_FOUND,POST_DELETED,SHARE_CONTEXT_INVALID |
| 수정 E06/E09 | PATCH | /posts/{postId} | payload,photoOrder,images | 최신 PostDetail | POST_NOT_EDITABLE,NEIGHBOR_VERIFICATION_REQUIRED |
| 삭제 E08/본인상세 | DELETE | /posts/{postId} | 세션 | 204 무본문 | POST_NOT_DELETABLE,POST_DELETED |
| 안건요약 B/F/G | GET | /posts/{postId}/summary | 세션/공유컨텍스트 | status,summary,source,fallbackToSource | AI_SUMMARY_NOT_APPLICABLE,AI_SUMMARY_UNAVAILABLE |
| 공유 G04 | GET | /posts/{postId}/share-link | 회원세션 | shareUrl,postId | POST_DELETED,UNAUTHORIZED |
| 댓글 조회 F/G/K | GET | /posts/{postId}/comments | sort,cursor,size | 부모/답글/meta | SHARE_SCOPE_MISMATCH,POST_DELETED |
| 댓글 등록 F/G | POST | /posts/{postId}/comments | content | Comment | COMMENT_FORBIDDEN_WORD,NEIGHBOR_VERIFICATION_REQUIRED |
| 답글 F/G | POST | /comments/{commentId}/replies | content,replyToCommentId | Comment.parentCommentId/replyTo | COMMENT_NOT_FOUND,COMMENT_DEPTH_EXCEEDED |
| 반응 F | PUT | /posts/{postId}/reactions/{reactionType} | desired state 존재 | reactionCounts,myReactions | REACTION_TYPE_INVALID,NEIGHBOR_VERIFICATION_REQUIRED |
| 반응 취소 F | DELETE | /posts/{postId}/reactions/{reactionType} | 없음 | reactionCounts,myReactions | UNAUTHORIZED,POST_DELETED |
| 평가 F | PUT | /comments/{commentId}/evaluation | type=LIKE/DISLIKE | myEvaluation,likeCount,dislikeCount | COMMENT_EVALUATION_TYPE_INVALID,NEIGHBOR_VERIFICATION_REQUIRED |
| 평가 취소 F | DELETE | /comments/{commentId}/evaluation | 없음 | myEvaluation=null/집계 | UNAUTHORIZED,COMMENT_NOT_FOUND |
| 실제투표 F04/F11 | PUT | /posts/{postId}/vote | optionId,confirmChange | 최신vote | VOTE_ENDED,VOTE_CHANGE_CONFIRMATION_REQUIRED,VOTE_OPTION_INVALID |
| 북마크 F07 | PUT | /posts/{postId}/bookmark | 없음 | isBookmarked=true | UNAUTHORIZED,POST_DELETED |
| 북마크 해제 F | DELETE | /posts/{postId}/bookmark | 없음 | isBookmarked=false | UNAUTHORIZED,POST_DELETED |
| 기관목록 K01 | GET | /officer/agendas | regionId,scope,cursor,size | cards,myInstitutionAdoption | INSTITUTION_VERIFICATION_NOT_ACTIVE |
| 채택 K02 | POST | /posts/{postId}/adoptions | 없음 | id,institutionName,adoptedAt | ADOPTION_NOT_ALLOWED,INSTITUTION_VERIFICATION_NOT_ACTIVE |
| 채택취소 K04 | DELETE | /posts/{postId}/adoptions/{adoptionId} | 없음 | 204 무본문 | ADOPTION_NOT_FOUND,ADOPTION_NOT_ALLOWED |
| 작성기록 I03 | GET | /users/me/posts | type,cursor,size | cards,capabilities | UNAUTHORIZED |
| 참여기록 I04 | GET | /users/me/participations | type,cursor,size | cards,myParticipation | UNAUTHORIZED |
| 투표기록 I05 | GET | /users/me/votes | status,cursor,size | availability,post,vote,participatedAt | UNAUTHORIZED |
| 저장기록 I02 | GET | /users/me/bookmarks | type,topic,cursor,size | cards,meta | UNAUTHORIZED |
| 행동수 I01 | GET | /users/me/activity | 세션 | totalCount,counts | UNAUTHORIZED |

### 캐시/화면 갱신 영향

| mutation | 성공 후 재조회/무효화 |
| --- | --- |
| 프로필/기본지역 | users/me, 작성자 공개표시; 기본지역 의존 초기화는 탐색값과 별도 |
| 게시/수정/삭제 | detail,home,board,map,myPosts,개인기록; 삭제는 공유/요약 캐시도 차단 |
| 반응 | detail,home,board,map,officer,participations,activity |
| 댓글/답글/평가 | comments,detail.commentCount,카드집계,participations,activity; 부모정렬 |
| 투표 | detail.vote,home.openVotes,myVotes,participations,activity |
| 북마크 | detail.myState,bookmarks,activity; 북마크만 참여목록 추가 없음 |
| 인증 완료/만료 재조회 | users/me,기관/이웃상태,detail capabilities,배지/담당자 접근 |
| 채택/취소 | detail.adoptions,기관관계,officer scope 목록 |

## 55. TypeScript 타입 정리 [권장안]

아래는 계약 정리 예시이며 실제 서버 구현 타입이 아니다. ID는 원문 number 예시와 실제 PK를 대조하고 안전정수 초과는 문자열로 계약한다. nullable/생략은 DTO별로 고정한다. DTO와 화면 draft는 분리하고 게스트 필드 생략을 optional로 표현한다.

```ts
// 실제 PK 합의 전의 문서상 입력 허용 예시. 실제 코드에서는 한 형태로 고정.
type Id = string | number;
type PostType = 'LOCAL_AGENDA' | 'LOCAL_ACTIVITY' | 'VOTE';
type PostTopic = 'TRANSPORTATION' | 'HOUSING' | 'SAFETY' | 'WELFARE'
  | 'LIVING_INFORMATION' | 'ENVIRONMENT' | 'OTHER';
type ReactionType = 'EMPATHY' | 'NEEDED' | 'CURIOUS';
type Evaluation = 'LIKE' | 'DISLIKE';
type Region = { id: Id; name: string };
type ResidentAttribute = 'RESIDENT' | 'STUDENT' | 'WORKER' | 'MERCHANT';
interface Profile {
  nickname: string; bio: string | null; profileImageUrl: string | null;
  residentAttributes: ResidentAttribute[]; activityRegion: Region;
}
interface InstitutionVerification {
  status: 'NOT_SUBMITTED' | 'RECEIVED' | 'COMPLETED' | 'EXPIRED';
  responsibleRegion: Region | null; institutionName: string | null;
  completedAt: string | null; validUntil: string | null; isActive: boolean;
}
interface User {
  id: Id; email: string; profile: Profile;
  neighborVerifiedRegions: Region[];
  institutionVerification: InstitutionVerification;
}
interface Capabilities {
  canComment: boolean; canReact: boolean; canEvaluateComment: boolean;
  canVote: boolean; canBookmark: boolean; canEdit: boolean; canDelete: boolean;
  canAdopt: boolean; canCancelAdoption: boolean;
}
interface MyState { reactions: ReactionType[]; isBookmarked: boolean }
interface ReactionCounts { EMPATHY: number; NEEDED: number; CURIOUS: number; total: number }
interface PublicAuthor {
  displayName: string; profileImageUrl: string | null;
  institutionVerified: boolean; isGuest?: boolean;
}
interface Photo { id: Id; url: string; order: number }
interface ActivityDetail {
  source: string; schedule: string; place: string;
  status: 'SCHEDULED' | 'IN_PROGRESS' | 'ENDED' | 'CANCELED';
  externalParticipationUrl: string | null; externalParticipationEnabled: boolean;
  organizerEmail: string | null;
}
interface PollOption { id: Id; content: string; voteCount: number; votePercentage: number }
interface Poll {
  question: string; status: 'OPEN' | 'CLOSED'; endsAt: string;
  participantCount: number; options: PollOption[];
  myOptionId?: Id | null; // member null=미참여, shared guest=필드 생략
}
interface PostBase {
  id: Id; region: Region; topic: PostTopic; title: string; content: string;
  author: PublicAuthor; images: Photo[];
  status: 'PUBLISHED' | 'DELETED'; createdAt: string; updatedAt: string;
}
type Post =
  | (PostBase & { type: 'LOCAL_AGENDA' })
  | (PostBase & { type: 'LOCAL_ACTIVITY'; activity: ActivityDetail })
  | (PostBase & { type: 'VOTE'; vote: Poll });
interface Comment {
  id: Id; postId: Id; parentCommentId: Id | null; content: string;
  author: PublicAuthor; replyTo: { commentId: Id; displayName: string } | null;
  likeCount: number; dislikeCount: number; myEvaluation?: Evaluation | null;
  createdAt: string; replies: Comment[]; // 화면은 원부모 아래 한 단계만 렌더
}
interface Adoption { institutionName: string; adoptedAt: string }
type PostDetail = Post & {
  reactionCounts: ReactionCounts; commentCount: number; comments: Comment[];
  adoptions?: Adoption[]; capabilities: Capabilities; myState?: MyState;
};
interface NeighborVerification {
  id: Id; region: Region; status: 'RECEIVED' | 'COMPLETED'; submittedAt: string;
}
interface PersonalParticipation {
  reactions: ReactionType[]; hasCommentOrReply: boolean;
  hasCommentOrReplyEvaluation: boolean; hasVote: boolean;
}
interface ApiResponse<T> { data: T }
interface ApiPage<T> { data: T[]; meta: { nextCursor: string | null; hasNext: boolean } }
interface ApiError {
  code: string; message: string;
  details?: { field: string; reason: string }[]; traceId?: string;
}
```

PostCard DTO는 본문 전체/기관개인정보 없이 id/type/topic/region/title/author/thumbnailUrl/집계/시각과 목록별 myParticipation/myInstitutionAdoption을 필요한 만큼 포함한다. 상세 DTO를 그대로 모든 목록에 복사하지 않는다. `Photo` id/url/order 필드 이름과 User의 nullable 구조는 예시로 실제 계약에 맞춘다. 삭제 투표 기록은 `AVAILABLE`의 post/vote 존재와 `UNAVAILABLE`의 post/vote=null을 discriminated union으로 분리한다. SignupDraft/PostFormDraft/FileQueue/TemporaryVoteState는 API DTO가 아니라 클라이언트 타입이다.

## 56. NON-MVP / DO NOT IMPLEMENT

- Figma 화면 존재·과거 문구·일반 SNS 관행으로 범위를 확대하지 않는다.
- AI 추천/새추천/개인맞춤, 관심지역/키워드, 별도 인기 선정 운영, 자유 게시물 검색을 추가하지 않는다.
- 알림/Push/설정/신고/계정복구/이메일·비밀번호변경/탈퇴/다크모드 메뉴·Route·API를 만들지 않는다.
- 임시저장/참고자료 링크/익명/AI 이미지/투표 알림예약을 요청 payload에 넣지 않는다.
- 운영자 인증 심사/승인/반려/백오피스/보완메일/보관운영을 만들지 않는다. 서버 시연완료 seed는 사용자 승인 기능과 구분한다.
- 기관 채택 후 처리단계/행정연동/해결보장, 내부 활동신청·결제 없음.
- 게스트 일반탐색/전화인증/반응/평가/투표/북마크 저장 없음.
- 로그인 뒤 원행동 자동실행·게스트 의견 회원이관 없음.
- 기관인증/활동지역/속성만으로 이웃 자격을 만들지 않는다.
- 사진없음 기본/AI/placeholder 이미지를 강제하지 않는다.
- 댓글/답글 수정·삭제와 서버 API를 임의 추가하지 않는다.

## 57. 확인 필요 사항과 구현 영향

미정은 이 문서에서 제품값으로 확정하지 않았다. 해당 영역을 제외하거나 mock을 연동완료로 표기하는 이유도 아니다. 명시된 값을 받아 validator/DTO/config에 연결하고 나머지 확정영역은 계속 구현한다.

| 우선 | 결정 사항 | 결정 주체 / 영향 |
| --- | --- | --- |
| 높음 | BottomNavigation 알림슬롯: 4슬롯/5슬롯 빈자리/비활성 어느 것인가 | PO/디자인; 최종 nav QA 전 결정, 임의선택 금지 |
| 높음 | 실제 금칙어 문자열 목록 | PO/BE; 댓글포함필터 완료 전 |
| 높음 | 이웃 증빙 인정종류·파일형식/개수/용량 | PO/BE; 이웃 첨부 validator |
| 높음 | 실제 API URL/ID/Request/Response/error/null·capability 계약 | BE/FE; 실연동 완료 전 |
| 높음 | 인증 token 전달/보관/TTL/갱신·proof TTL | BE/FE; 세션과 가입복귀 |
| 높음 | 공유 token 방식/TTL/재발급·post 귀속·전달 | BE/FE; 공유 접근검증 |
| 높음 | 지도 region/경계/좌표 데이터 원천·provider | PO/BE/FE; 지도 렌더 |
| 중간 | 프로필 사진 형식/용량/교체제거 규칙 | PO/BE; 프로필 validator |
| 중간 | 제목/본문/질문/선택지/기관입력 최대길이 | PO/BE; 임의수치 없음 |
| 중간 | 활동 일정 구조·URL protocol/검증 | PO/BE/FE; 일정·링크 DTO |
| 중간 | 신규 과거 종료시각·중복선택지 | PO/BE; 투표 입력 |
| 중간 | 게시물 삭제 데이터 물리보존·댓글/반응/사진/표/활동/채택 | BE/PO; 공개차단/북마크제거/개인투표기록유지는 이미 확정 |
| 중간 | 댓글/답글 수정·삭제 범위 | PO; 결정 전 제공하지 않음 |
| 중간 | size·cursor·초기 댓글수/답글순서·답글 큰목록 계약 | BE/FE; 목록/답글 누락 방지 |
| 중간 | 일반목록 기본정렬·기관동률 보조정렬 | BE/PO; 확정 댓글/기관기준 유지 |
| 중간 | MB→bytes 10/50 계산 기준 | BE/FE; 모든 용량경계 동일하게 |
| 중간 | AI 생성착수/저장/재생성/버전/재시도·짧은원문 임계 | BE/PO; 상태/fallback은 구현 |
| 중간 | 득표율 소수/반올림·결과갱신 방식 | BE/FE; 과반·본인표 표시와 별도 |
| 중간 | 기관ID/인증요청/본인기관 채택관계 DTO | BE; 담당자 userId와 기관을 혼동하지 않음 |
| 중간 | 생성 POST 멱등키 지원·scope/유효기간 | BE/FE; 결과불명 재시도 |
| 중간 | 삭제 투표 기록 OPEN/CLOSED 필터와 관계 | PO/BE; 전체기록 유지 우선 |
| 중간 | 가입 마지막 commit·가입후 이웃CTA·기관인증 auth continuation | PO/BE/FE; 가입전 회원API 호출금지 |
| 낮음 | Modal/BottomSheet/EmptyState 추가 원본 규격·시작폰트예외 | 디자인; 확인 전 임의 Figma 완료표시 없음 |

운영심사/보완메일/증빙보관/행정연동은 후순위 질문이며 MVP로 끌어오지 않는다.

## 58. QA / Acceptance Checklist

실제 테스트를 실행한 결과가 아니라 구현 후 인수 목록이다. 계정 준비: 미인증회원, 해당지역완료회원, 타지역완료회원, 접수기관, 유효기관, 만료기관, 담당밖기관, 비로그인공유게스트. 데이터: 사진0/10장, 안건/활동4상태/진행·종료표, 삭제표 기록, 부모/답글/복수기관 채택, 빈목록.

- [ ] QA-01 **신규회원가입**: 가입단계순서·필수동의·최종저장 후 세션/프로필/지역 존재.

- [ ] QA-02 **이메일인증**: 6자리선행0·5분·60초·30분5회·입력5회·재발급 이전코드 무효·발송실패 성공금지.

- [ ] QA-03 **로그인**: 이메일/비밀번호 성공; 실패원인비구분·중복클릭차단.

- [ ] QA-04 **returnTo**: 로그인/가입/프로필/지역 후 원상세복귀·행동자동실행없음.

- [ ] QA-05 **프로필**: 닉네임10/중복·소개50·복수속성·최신작성자표시·실패유지.

- [ ] QA-06 **기본활동지역**: 후보ID 저장·이웃자격 자동부여없음.

- [ ] QA-07 **타지역탐색**: 기본활동지역 월계1동→탐색 하계2동 이후 프로필 월계1동 유지.

- [ ] QA-08 **미인증지역참여제한**: 게시/댓글/답글/반응/평가/투표 모두 거부, 열람/북마크 가능.

- [ ] QA-09 **이웃완료지역참여**: 완료지역에서 같은 쓰기 허용, 기관인증만으로 우회 불가.

- [ ] QA-10 **지역안건작성**: 5입력·사진선택·성공 같은postId 상세/목록.

- [ ] QA-11 **지역활동작성**: 출처/일정/장소/상태필수·기본값없음·상태자동전환없음.

- [ ] QA-12 **투표작성**: 질문/선택2~10/종료필수·알림없음·작성은참여아님.

- [ ] QA-13 **게시물수정**: 본인/지역/capability·진행표4항목만·종료불가.

- [ ] QA-14 **게시물삭제**: 확인/취소·서버204 뒤공개차단·종료표삭제불가.

- [ ] QA-15 **사진제한**: JPG/PNG·10장·합계10MB 경계; 사진없음영역없음·최종기존+신규 검증.

- [ ] QA-16 **반응복수선택**: 3개동시·개별취소·동시응답/재시도집계정확.

- [ ] QA-17 **댓글**: 내용/금칙어·등록성공후만비우기·회원지역자격.

- [ ] QA-18 **답글**: 부모귀속·대상표시·댓글수 포함.

- [ ] QA-19 **답글의답글**: 원부모1단계·@대상명·2단계중첩 없음.

- [ ] QA-20 **댓글평가**: LIKE/DISLIKE 전환·동일취소·게스트불가·최초+1/전환+0.

- [ ] QA-21 **댓글정렬**: 부모like내림/동률최신·최신순·답글like미합산.

- [ ] QA-22 **최초투표**: 선택만API없음→제출성공 실제내표·한표.

- [ ] QA-23 **투표변경**: 확인취소기존유지·확인1표교체·409재확인.

- [ ] QA-24 **종료투표**: 제출/변경/수정/삭제불가·종료직전경합 서버우선.

- [ ] QA-25 **공유**: 링크대상원본·복사성공확인·브라우저실패fallback.

- [ ] QA-26 **게스트열람**: 특정상세/공개결과/요약만·일반nav없음.

- [ ] QA-27 **게스트댓글**: 공개명게스트·같은금칙어·전화인증없음.

- [ ] QA-28 **게스트회원행동복귀**: 반응/평가/표/북마크→안내→로그인→복귀·재클릭전저장없음.

- [ ] QA-29 **북마크**: 회원미인증가능·상세한버튼·저장팝업·현재상세유지.

- [ ] QA-30 **내가만든게시물**: 본인원본3유형·종료표메뉴없음.

- [ ] QA-31 **참여게시물**: postId당1카드·모든유효표시·북마크만/타인행동제외.

- [ ] QA-32 **참여투표**: 실제제출만·내표와최다표별도·진행/종료필터.

- [ ] QA-33 **지도**: 안건/투표 반응합최대·동률최신·첫사진·빈동·활동제외·GPS없음.

- [ ] QA-34 **이웃인증상태**: 첨부≠제출≠완료·접수재조회·완료최대3·4번째거부.

- [ ] QA-35 **기관인증상태**: 필수7정보+지역·PDF/JPG/PNG·개별10/전체50·접수권한없음·유효완료배지.

- [ ] QA-36 **기관담당자목록**: 유효기관·전체공개안건·반응수내림·본인기관채택scope.

- [ ] QA-37 **담당지역채택**: 공개안건만·기관관계·공개명/시각만.

- [ ] QA-38 **담당지역밖열람**: 원문+주민의견내용확인·채택불가.

- [ ] QA-39 **채택취소**: 본인기관관계만·타기관보존·PostStatus 불변.

- [ ] QA-40 **AI성공**: 공개안건3문장한문단·출처/원문연결·타유형호출없음.

- [ ] QA-41 **AI실패**: FAILED/짧은원문/5xx 원문유지·구버전오표시없음.

- [ ] QA-42 **Empty**: 목록0·댓글0·후보없는동·개인기록0 정상CTA.

- [ ] QA-43 **Error**: 401/403/404/409/413/415/422/429·첫/후속오류 구분.

- [ ] QA-44 **Loading**: 초기skeleton·영역별pending·중복submit 차단.

- [ ] QA-45 **네트워크실패입력보존**: 작성/사진/댓글/가입/인증 파일큐 유지·결과불명 성공가정없음.

- [ ] QA-46 **삭제게시물접근**: 공유/상세/요약미노출·북마크제거·참여표기록유지본문없음.

- [ ] QA-47 **NON-MVP미노출**: 추천/관심/알림/신고/복구/탈퇴/임시저장/참고/익명/AI이미지/심사 없음.

- [ ] QA-48 **누적행동횟수**: 최초등록+1·취소/전환/표변경+0·재등록+1·카드수와별도.

- [ ] QA-49 **기관만료**: 기관메뉴/Route/배지 제거·일반회원유지·이웃자격별도.

- [ ] QA-50 **공유범위공격**: 다른postId/댓글token거부·회원실패guest자동전환없음.

- [ ] QA-51 **목록복귀**: type/topic/region/sort/scroll 유지·조건변경cursor초기화.

- [ ] QA-52 **반응변경일관성**: detail/home/map/officer/participation/activity 재조회 같은원본.

- [ ] QA-53 **기관참여분리**: 기관담당지역과다른 이웃완료지역 일반참여가능·기관채택은담당내.

- [ ] QA-54 **첨부/게시결과불명**: 자동POST중복없음·멱등계약/서버조회로확인.

- [ ] QA-55 **미정결정추적**: 하단슬롯/파일제한/실API차이 미결정은완료표시하지않음.

### 최종 문서 검토 결과

- 실제 화면·사용자흐름·상태·권한·Task가 본문이고 API는 각기능 아래와 후반 부록에 배치했다.
- Figma 157개 프레임은 모두 매핑했으며 비-MVP와 중복상태를 분리했다. 화면별 핵심 구현 단위의 완료조건·QA가 있다.
- 지역탐색/프로필·이웃/기관·게스트/회원·임시표/서버표·참여카드/누적횟수를 각각 구분했다.
- API 경로·타입·추가라이브러리·Router는 제안으로 표기했으며 실제 서버/저장소 구현완료를 주장하지 않았다.
- 하단슬롯 등 미정값을 임의확정하지 않았다. 코드·Figma를 변경하지 않았다.

문서상 Task를 시작할 첫 작업은 **Phase 0의 실제 저장소/계약 확인**, 다음은 **Phase 1 토큰·폰트·레이아웃**이다.


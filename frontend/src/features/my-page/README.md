# #46 마이페이지·개인 활동 횟수

F-WYMXXP / Figma 최종디자인 WF-I01 1160:4471 직접 확인. 모바일396 기준 프로필 사진364×72, section18/27, Notice,6개 Chip(3열), 메뉴의14px radius/16px padding/8px gap·하단5개를 적용합니다. 기존 profile-preview.png는 동일 놀이터 사진이므로 Mock profile의 동적 photoUrl로 재사용하며 제품에 기본 사진으로 등록하지 않습니다.

기존 ProfileProvider.saved를 조회/소비하고 #43 프로필·활동 지역 수정으로 origin:me를 전달합니다. #44 NeighborStatus, #45 InstitutionStatus/InstitutionBadge를 같은 마이페이지의 상태 패널에서 소비합니다. 신청/증빙 Route 없음. 기관 업무는 institutionAccess의 현재 자격+Capabilities 확인 뒤 진입 콜백만 제공하며 #50 업무 본체를 만들지 않습니다.

ActivityService는 조회 전용 FE 표시 port이며 실제 #5/#66 adapter가 확정 wire 범주를6개 UI 카테고리로 묶습니다. totalCount와 누적counts 합·음수/정수 오류를 검증하며 currentRelations는 별도입니다. 현재 카드/관계 수에서 누적을 계산하지 않습니다. null 집계와0회는 구별합니다. 실 API mutation 성공 뒤 Integration에서 store.load()로 재조회하며 메뉴 이동 자체는 횟수를 증가시키지 않습니다.

등록/작성/최초 참여+1, 취소/삭제/설정/직접 평가 전환/표변경/동일state재시도+0, 취소 후 재등록+1은 개발용 simulate로만 검증합니다. 실제 참여 mutation/event 저장을 구현하지 않습니다. Mock 성공은 실제 집계/영속 저장 완료가 아닙니다.

menus/menuEntry는 기존 논리 Route와 origin:me를 사용합니다. 미구현 Page나 URL은 추측하지 않습니다. onMyMenu는 담당 기능 Owner가 진입/복귀를 연결하는 콜백입니다. 현재 구현된 profile/activityRegion은 기본 navigation으로 이동하고, 그 외 콜백이 없으면 현재 마이에서 담당 Issue 연결 대기 안내를 표시합니다. 관심#87/키워드#88/알림#89/설정#91/계정#92/탈퇴#95를 제품 제외로 취급하지 않습니다. 상대 Page/Service는 복제하지 않습니다.

MyPageProvider는 기존 subjectKey로 계정별 activity/scroll을 격리합니다. 마이의 실제 main scroll을 Provider memory에 보관하고 메뉴/상태 패널 왕복 때 복원합니다. 기존 Navigation origin/returnTo를 소비하며 로그인/가입 복귀만으로 행동을 재실행하지 않습니다. 게스트/가입미완료/loading/error는 AccessGuard에서 제한합니다.

/dev/my-page-preview는 개발에서만 제공: 일반/이웃/기관/만료 및 메뉴stub→원마이, 프로필 수정, 활동 조회 정상/null/0/loading/error→retry, 등록/취소/재등록/직접전환/재시도. production Mock/역할 스위치 없음. 상태 패널은 기존 #44/#45 UI를 재사용합니다.

실 API 연동 대기:#5/#10/#27/#26, #66/#62/#65/#68. 실제 계정별 집계·누적 원자성·자격/기간·Page 콜백·returnTo/scroll·프로필 재조회는 Integration에서 검증합니다. 증빙Validation/쓰기Pending/발송/탈퇴 실행은 이 Issue에 N/A입니다.

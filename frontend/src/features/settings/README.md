# #91 설정·개인 목록 재진입

F-SAOWVT / D-12 / FE-R12·13. Figma 최종디자인 WF-L01 1160:6401 직접 확인: 일반 Header 58px, 콘텐츠 top20/좌우16/간격12, 364×80 Menu(14px radius, 16/12px padding, label/value gap8), 계정/커뮤니티/앱 설정/기타 순서. L01에는 BottomNavigation이 없으며 원 화면의 5개 메뉴를 변경하지 않습니다. OS 상태바는 웹에서 합성하지 않습니다. 기존 MyPage 메뉴를 공통 SettingRow로 추출해 동일 UI를 사용합니다.

설정 URL은 아직 논리 계약이므로 임의 /settings를 등록하지 않습니다. NavigationProvider.onIntent는 URL 미확정 목적지의 의도를 소비하는 선택적 port입니다. SettingsNavigation은 settings만 소비해 L01을 표시하고 원 화면을 mounted/hidden으로 유지합니다. FE2 Explore Header의 기존 navigation 호출을 그대로 소비하며 FE2 파일을 수정하지 않습니다. MyPage의 settings 진입만 중앙 Navigation으로 연결합니다.

SettingsMenuHandler는 해당 Owner로 넘기는 entry(origin:settings), 복귀 callback, AbortSignal을 받습니다. 로그아웃은 null entry의 action intent이며 #94 실행 본체를 복제하지 않습니다. 계정 #92, 목록 #47/#49, 키워드 #88, 수신 #90, 탈퇴 #95도 동일 원칙입니다. handler가 없으면 L01에서 연결 대기를 표시하고 존재하지 않는 Page로 보내지 않습니다. pending 동안 중복 선택 차단, 연결 실패의 명시적 Retry/취소, stale 성공/복귀 무시. 성공은 Owner 화면 연결 성공이며 실제 저장/로그아웃/탈퇴 성공이 아닙니다.

I03/I04/I02는 기존 myPosts/participations/bookmarks 목적지이며 댓글 전용 목록이 없습니다. 기존 Navigation의 list snapshot ref를 전달하며 값/Service는 Owner가 소유합니다. 현재 develop에 목록 본체/Service가 없으므로 개발용 port stub으로 왕복을 검증하고 실 필터/탭/스크롤 인수는 #47/#49/#66/#67에서 대기합니다. 설정 자체 스크롤은 메모리에 보존하고 새 원 진입 때 초기화합니다. 설정 뒤로는 원 진입, 목록 뒤로는 L01, I01 목록 진입은 I01 유지. 계정 변경 때 설정 패널/상태를 격리하고 Navigation returnTo는 유지합니다.

/dev/settings-preview는 NODE_ENV=development에서만 제공: home/me 진입, 8개 Owner stub 왕복, handoff success/error-retry/loading, 인증 loading/error/guest/signup-incomplete/member. 데이터 없는 설정 메뉴는 정적이므로 read API Loading/Empty, 입력 Validation, 저장 mutation은 N/A. Loading/Failure/Retry는 실제 menu handoff/권한 확인에만 적용합니다. 실 API 연동 대기: 각 데이터/계정 Owner의 API/Provider, #66/#67/#68 실제 연결 확인. API SPEC의 /users/me/settings 표기를 구현 완료로 취급하지 않으며 다크 모드는 구현하지 않습니다.

실행 검증 결과는 작업 보고에 기록합니다. Figma 직접 확인과 실제 브라우저 시각/스크롤 비교 완료는 별개입니다.

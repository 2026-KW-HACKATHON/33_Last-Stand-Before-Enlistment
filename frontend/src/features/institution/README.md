# #45 기관 유효 상태·담당 지역·배지

F-OPNIXL / S-YLSPHQ / F-GDASNA / F-MUBDJD. S-JRMYIV 증빙 첨부는 제외 이력입니다. 신청/증빙/제출/접수/심사/승인 UI나 API를 추가하지 않습니다.

InstitutionService는 FE 조회 전용 표시 port이며 미합의 endpoint/DTO/enum을 wire 정본으로 확정하지 않습니다. 실제 #12/#74 adapter가 subjectId, 기관 정본, 담당 지역, 완료일·validUntil을 반환하고 승인일부터1년 정책을 검증해야 합니다. FE는 임의 승인일이나 만료일을 생성하지 않습니다. subjectKey는 Session과 연결된 안정적인 FE 회원 ID이며 이메일·기관명·역할로 대체하지 않습니다. 응답 주체 불일치는 오류입니다.

현재 완료 상태 + 완료일≤현재시각<validUntil에서만 배지를 표시합니다. 기간 경계 timer, focus/visibility 복귀로 만료 표시를 갱신합니다. 업무는 기존 SessionProvider의 institution grant, 대상 업무는 responsible-region grant와 담당 지역 일치를 추가 확인합니다. 미확인/실패/loading은 배지/업무를 허용하지 않으며 일반 회원/이웃 자격·게시물은 변경하지 않습니다. 실제 요청 시 서버 재검증이 필요합니다.

InstitutionBadge는 작성자 subjectId와 조회 결과를 대조합니다. withInstitutionBadge는 기존 FE2 AuthorDisplay의 badge prop만 파생하며 다른 작성자/만료/미확인/오류에서는 기존 badge도 제거합니다. FE2 PostCard/PostDetail/Page는 수정하지 않습니다. 작성자별 조회 결과와 일반 사용자 Session을 섞지 않습니다. 공개 작성자 상태 조회 계약·게시물 adapter에서의 재조회는 #65/#68 대기입니다.

AppProviders의 선택적 institutionService와 기존 subjectKey로 연결합니다. 제품 Mock/역할 스위치/localStorage 권한 등록 없음. Page Owner는 InstitutionStatus에 onBack/onAuthenticate/onEnterWork를 주입하며 현재 Navigation의 origin/returnTo/snapshot을 보존합니다. 콜백은 원 행동을 자동 실행하지 않습니다. #46 마이페이지 및 #50 업무 Page를 선행 구현하지 않습니다.

Figma 최종디자인 WF-I01(1160:4471), 기관 메뉴(1160:4504)를 직접 확인했습니다. 기존 공통 Header/BottomNavigation/Button/Notice·색상/간격/카드 토큰을 소비합니다. 독립 자격 Frame은 배정되지 않아 상태 상세는 Issue의 최소 표시 계약을 합성합니다. 파란 배지 도형/색상 정밀 규격은 해당 Frame에 없으며 작은 텍스트 배지에 기존 Tailwind blue-600만 국소 적용합니다. 새 전역 토큰/아이콘은 만들지 않습니다. 관련 없는 I01 프로필 이미지·활동 메뉴는 #46 범위라 복제하지 않습니다.

/dev/institution-preview는 development에서만 제공하며 유효/만료/무자격/미확인/타 지역/loading/error→retry, 업무 진입 콜백과 원 화면 필터 보존·인증 취소 복귀를 재현합니다. 실제 브라우저·실 API 인수는 별도입니다. 조회 전용이라 증빙 validation·쓰기 Pending/Success/Failure는 N/A이며 조회 성공/실패/loading/retry·잘못된 응답은 검증합니다.

실 API 연동 대기: #12/#4/#9/#74/#75. #65/#68에서 실제 기간·자격 갱신, 프로필/과거 게시물 배지 제거, 서버의 담당/타 지역 제한, 원 화면/returnTo/스크롤 보존, #50/#53 소비를 검증합니다.
